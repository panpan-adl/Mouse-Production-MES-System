"""Clone an existing configured MES database; clear ONLY the newly created acceptance copy."""
import argparse
import datetime
import json
import subprocess
from pathlib import Path
from stage1_database import ROOT, sql, config, env

def prepare(source):
    import re
    import socket
    try:
        connection=socket.create_connection(('127.0.0.1',8093),timeout=1)
    except OSError:
        connection=None
    if connection is not None:
        connection.close()
        raise RuntimeError('Stop the owned four-tier demo on port 8093 before preparing a new database')
    if not re.fullmatch(r'[A-Za-z0-9_]+',source):
        raise ValueError('Invalid source database')
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    database='mouse_mes_four_tier_'+stamp
    out=ROOT/'.runtime/four-tier-20261030'
    out.mkdir(parents=True,exist_ok=True)
    backup=ROOT/'.runtime/backups'/f'four_tier_source_{stamp}.sql'
    backup.parent.mkdir(parents=True,exist_ok=True)
    with backup.open('wb') as output:
        result=subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'),'-h127.0.0.1','-uroot',
            '--single-transaction','--default-character-set=utf8mb4',source],env=env,stdout=output,stderr=subprocess.PIPE)
    if result.returncode or backup.stat().st_size<1000:
        raise RuntimeError('Source backup failed')
    sql('CREATE DATABASE `'+database+'` CHARACTER SET utf8mb4')
    sql(backup.read_text(encoding='utf-8'),database)
    assert re.fullmatch(r'mouse_mes_four_tier_[0-9_]+',database)
    exact={'t_order','t_production_plan','t_production_task','t_production_report','t_inventory','t_inventory_transaction',
        't_stock_in','t_stock_in_item','t_stock_out','t_stock_out_item','t_lot','t_purchase_order','t_purchase_order_item',
        't_maintenance','t_material_request','mm_schedule','mm_task_context','mm_task_slot','mm_inspection','mm_nc','mm_rework',
        'mm_stock_posting','mm_check_record','mm_check_due','mm_report','mm_report_line','mm_report_draft','mm_report_approval',
        'mm_qc_todo','mm_holiday','mm_material_origin','mm_qc_comparison','mm_inventory_anchor','mm_inventory_baseline',
        'mm_agent_run','mm_ai_session','mm_query_audit','mm_ai_budget','mm_replenishment'}
    tables=sql('SHOW TABLES',database).splitlines()[1:]
    targets=[t for t in tables if t in exact or t.startswith(('mm_work_','mm_line_','mm_document','mm_package','mm_auto_','mm_rotation'))]
    sql('SET FOREIGN_KEY_CHECKS=0;'+''.join('DELETE FROM `'+t+'`;' for t in targets)
        +"DELETE FROM t_user WHERE user_code='103999' AND username='mcworker2';UPDATE t_material SET current_stock=0;SET FOREIGN_KEY_CHECKS=1;",database)
    sql('DELETE p FROM mm_product_material p JOIN t_material m ON m.id=p.material_id WHERE m.material_type=2',database)
    # Capture a NEW current-time baseline after resetting the disposable database. No historical backdating.
    for name in ['rectification.sql','stage34_intelligence.sql','four_tier_acceptance.sql']:
        sql((ROOT/'04-database'/name).read_text(encoding='utf-8'),database)
    sql((ROOT/'04-database/four_tier_acceptance.sql').read_text(encoding='utf-8'),database)
    state=dict(database=database,source_database=source,backup=str(backup),port=8093,source_business_writes=False)
    (out/'state.json').write_text(json.dumps(state,indent=2),encoding='utf-8')
    (out/'suite').mkdir(exist_ok=True)
    for name in ['verify_mes_completion.py','verify_mes_completion_extra.py','verify_mes_completion_final.py']:
        text=(ROOT/'07-tools-barcodes'/name).read_text(encoding='utf-8')
        text=text.replace('.runtime/mes-completion-database.json','.runtime/four-tier-20261030/state.json')
        text=text.replace('mouse_mes_completion_test_','mouse_mes_four_tier_').replace('127.0.0.1:8085','127.0.0.1:8093')
        text=text.replace('.runtime/mes-completion-acceptance.json','.runtime/four-tier-20261030/business-regression.json')
        prefix="import sys\nfrom pathlib import Path\nsys.path.append(str(Path(__file__).resolve().parents[3]/'07-tools-barcodes'))\n"
        (out/'suite'/name).write_text(prefix+text,encoding='utf-8')
    return state

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--source-database',required=True,help='Existing fully configured database, read only')
    print(json.dumps(prepare(parser.parse_args().source_database),ensure_ascii=False))

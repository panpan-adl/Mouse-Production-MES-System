"""Add repeatable quality-task fixtures to the running local demo database."""
from stage1_database import ROOT, sql, config, env
from pathlib import Path
import datetime, subprocess, json

DB = 'mouse_mes_four_tier_20261030_171925_883824'
assert DB.startswith('mouse_mes_four_tier_')
assert DB in sql('SELECT DISTINCT db FROM information_schema.processlist WHERE db IS NOT NULL')
stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
backup = ROOT / f'.runtime/backups/quality_demo_before_{stamp}.sql'
backup.parent.mkdir(parents=True, exist_ok=True)
with backup.open('wb') as out:
    result = subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'), '-h127.0.0.1', '-uroot', '--single-transaction', '--default-character-set=utf8mb4', DB], env=env, stdout=out, stderr=subprocess.PIPE)
assert result.returncode == 0 and backup.stat().st_size > 1000, 'Backup failed'

def quote(value):
    return "'" + str(value).replace("'", "''") + "'"

statements = ['START TRANSACTION;', "SET @worker=(SELECT id FROM t_user WHERE role_id=3 ORDER BY id LIMIT 1);", "SET @inspector=(SELECT id FROM t_user WHERE role_id=4 ORDER BY id LIMIT 1);", "SET @material=(SELECT id FROM t_material ORDER BY id LIMIT 1);"]
scenarios = [None, None, None, None, 'REWORK', 'UNQUALIFIED', 'SCRAP', 'QUALIFIED', 'CONCESSION_ACCEPT']
reasons = {'REWORK': ('滚轮转动异响，需重新装配', '返工后复检'), 'UNQUALIFIED': ('按键回弹不良，隔离待处理', '隔离不合格品，安排复检'), 'SCRAP': ('外壳开裂，无法修复', '登记报废并隔离'), 'QUALIFIED': ('', '检验合格，允许入库'), 'CONCESSION_ACCEPT': ('外观轻微色差，不影响功能', '评审后让步接收')}
for i, result in enumerate(scenarios, 1):
    no = f'DEMO-QTASK-{i:03}'
    qty = [120, 200, 160, 100, 150, 180, 80, 240, 100][i-1]
    statements.append(f"""INSERT INTO t_production_task (task_no,plan_id,task_type,product_id,product_name,quantity,assigned_user_id,assigned_user_name,task_status,start_time,end_time,remark)
SELECT {quote(no)},p.id,{[1,2,3,1][(i-1)%4]},p.product_id,pr.product_name,{qty},u.id,u.real_name,3,DATE_SUB(NOW(),INTERVAL {i+3} HOUR),DATE_SUB(NOW(),INTERVAL {i} HOUR),'模拟数据：质检任务场景'
FROM t_production_plan p JOIN t_product pr ON pr.id=p.product_id JOIN t_user u ON u.id=@worker
WHERE NOT EXISTS (SELECT 1 FROM t_production_task WHERE task_no={quote(no)}) ORDER BY p.id LIMIT 1;""")
    if result:
        reason, handle = reasons[result]
        bad = {'REWORK':8,'UNQUALIFIED':12,'SCRAP':5,'QUALIFIED':0,'CONCESSION_ACCEPT':2}[result]
        inspection = f'DEMO-QC-{i:03}'
        statements.append(f"""INSERT INTO t_qc_inspection (inspection_no,inspection_type,source_order_id,source_order_no,sample_quantity,qualified_quantity,defective_quantity,qualified_rate,result,defect_reason,handle_method,business_status,remark,created_by,is_deleted)
SELECT {quote(inspection)},'PRODUCTION',t.id,t.task_no,50,{50-bad},{bad},{(50-bad)*2},{quote(result)},{quote(reason)},{quote(handle)},'DEMO','模拟数据：生产质检',@inspector,0
FROM t_production_task t WHERE t.task_no={quote(no)} AND NOT EXISTS (SELECT 1 FROM t_qc_inspection WHERE inspection_no={quote(inspection)});""")
        statements.append(f"""INSERT INTO t_qc_inspection_item (inspection_id,material_id,inspection_item,inspection_standard,actual_value,sample_quantity,qualified_quantity,defective_quantity,item_result,defect_reason,handle_method,remark)
SELECT q.id,@material,'外观与功能检查','外壳无损伤，按键回弹正常，滚轮转动顺畅',{quote(reason or '抽检50件，外观与功能均正常')},50,{50-bad},{bad},{quote('QUALIFIED' if bad==0 else 'UNQUALIFIED')},{quote(reason)},{quote(handle)},'模拟数据'
FROM t_qc_inspection q WHERE inspection_no={quote(inspection)} AND NOT EXISTS (SELECT 1 FROM t_qc_inspection_item WHERE inspection_id=q.id);""")
statements.append('COMMIT;')
sql('\n'.join(statements), DB)
verification = sql("""SELECT 'tasks' AS kind,COUNT(*) AS total FROM t_production_task WHERE task_no LIKE 'DEMO-QTASK-%'
UNION ALL SELECT 'inspections',COUNT(*) FROM t_qc_inspection WHERE inspection_no LIKE 'DEMO-QC-%';
SELECT result,COUNT(*) AS total FROM t_qc_inspection WHERE inspection_no LIKE 'DEMO-QC-%' GROUP BY result;
SELECT COUNT(*) AS pending FROM t_production_task t WHERE task_no LIKE 'DEMO-QTASK-%' AND task_status=3 AND NOT EXISTS(SELECT 1 FROM t_qc_inspection q WHERE q.source_order_no=t.task_no AND q.is_deleted=0 AND q.result IN ('QUALIFIED','CONCESSION_ACCEPT'));""", DB)
(ROOT/'.runtime/quality-demo-data.json').write_text(json.dumps({'database':DB,'backup':str(backup),'verification':verification},ensure_ascii=False,indent=2),encoding='utf-8')
print(verification)

"""Back up a named existing database before applying the idempotent four-tier migration."""
import argparse
import datetime
import json
import re
import subprocess
from pathlib import Path
from stage1_database import ROOT, sql, config, env

def migrate(database):
    if not re.fullmatch(r'[A-Za-z0-9_]+', database):
        raise ValueError('Invalid database name')
    required = {'mm_location', 'mm_area', 't_location', 't_inventory_transaction', 'mm_auto_event', 'mm_qc_todo'}
    tables = set(sql('SHOW TABLES', database).splitlines()[1:])
    if not required.issubset(tables):
        raise RuntimeError('Apply the foundation, fulfillment, intelligence and completion migrations first')
    stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    backup = ROOT/'.runtime/backups'/f'{database}_four_tier_{stamp}.sql'
    backup.parent.mkdir(parents=True, exist_ok=True)
    with backup.open('wb') as output:
        result = subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'), '-h127.0.0.1', '-uroot',
            '--single-transaction', '--routines', '--triggers', '--default-character-set=utf8mb4', database],
            env=env, stdout=output, stderr=subprocess.PIPE)
    if result.returncode or backup.stat().st_size < 1000:
        raise RuntimeError('Backup failed; migration not attempted')
    business = ['t_inventory','t_inventory_transaction','mm_document','mm_work_order','t_order','t_production_report']
    counts = lambda: {t: int(sql('SELECT COUNT(*) FROM '+t, database).strip().splitlines()[-1]) for t in business}
    before = counts()
    sql((ROOT/'04-database/four_tier_acceptance.sql').read_text(encoding='utf-8'), database)
    sql((ROOT/'04-database/four_tier_acceptance.sql').read_text(encoding='utf-8'), database)
    sql((ROOT/'04-database/purchase_request_flow.sql').read_text(encoding='utf-8'), database)
    assert counts() == before, 'Business record counts changed unexpectedly'
    state = dict(database=database, backup=str(backup), applied_twice=True, preserved_counts=before)
    (ROOT/'.runtime'/f'four-tier-migration-{database}.json').write_text(json.dumps(state,indent=2),encoding='utf-8')
    return state

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--database', required=True, help='Existing database to upgrade; business records are preserved')
    print(json.dumps(migrate(parser.parse_args().database), ensure_ascii=False))

"""Back up before additive migration. --test clones production without modifying it."""
from stage1_database import ROOT, config, env, sql
from pathlib import Path
import datetime, subprocess, sys, json

stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
backup = ROOT/f'.runtime/backups/mouse_mes_completion_{stamp}.sql'
backup.parent.mkdir(parents=True, exist_ok=True)
with backup.open('wb') as output:
    run = subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'),'-h127.0.0.1','-uroot','--single-transaction','--routines','--triggers','--default-character-set=utf8mb4','mouse_mes'],env=env,stdout=output,stderr=subprocess.PIPE)
if run.returncode or backup.stat().st_size<1000:
    raise RuntimeError('Database backup did not complete')
database = 'mouse_mes_completion_test_'+stamp if '--test' in sys.argv else 'mouse_mes'
if '--test' in sys.argv:
    sql(f'CREATE DATABASE `{database}` CHARACTER SET utf8mb4')
    sql(backup.read_text(encoding='utf-8'), database)
tables = ['t_order','t_production_plan','t_production_task','t_production_report','t_inventory','t_inventory_transaction','mm_work_order']
before = {t:sql('SELECT COUNT(*) FROM '+t, database).splitlines()[-1] for t in tables}
for _ in range(2):
    sql((ROOT/'04-database/mes_completion.sql').read_text(encoding='utf-8'), database)
after = {t:sql('SELECT COUNT(*) FROM '+t, database).splitlines()[-1] for t in tables}
assert before==after, 'Migration changed existing business counts'
state = dict(database=database, backup=str(backup), applied_twice=True,preserved_counts=after)
(ROOT/('.runtime/mes-completion-database.json' if '--test' in sys.argv else '.runtime/mes-completion-production-database.json')).write_text(json.dumps(state,indent=2),encoding='utf-8')
print(json.dumps(state))

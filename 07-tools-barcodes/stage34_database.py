"""Back up and apply only additive tables; --test clones the original before migration."""
from stage1_database import ROOT,sql,args,env,config
from pathlib import Path
import subprocess,datetime,json,sys
stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
backup=ROOT/f'.runtime/backups/mouse_mes_stage34_{stamp}.sql';backup.parent.mkdir(parents=True,exist_ok=True)
with backup.open('wb') as out:
 result=subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'),'-h127.0.0.1','-uroot','--single-transaction','--routines','--triggers','--default-character-set=utf8mb4','mouse_mes'],env=env,stdout=out,stderr=subprocess.PIPE)
assert result.returncode==0 and backup.stat().st_size>1000,'Backup failed'
database='mouse_mes_stage34_test_'+stamp if '--test' in sys.argv else 'mouse_mes'
if '--test' in sys.argv:
 sql(f'CREATE DATABASE `{database}` CHARACTER SET utf8mb4;');sql(backup.read_text(encoding='utf-8'),database)
tables=['t_order','t_production_plan','t_production_task','t_production_report','t_inventory','t_inventory_transaction','mm_work_order']
before={t:sql('SELECT COUNT(*) FROM '+t,database).strip().splitlines()[-1] for t in tables}
for _ in range(2):sql((ROOT/'04-database/stage34_intelligence.sql').read_text(encoding='utf-8'),database)
after={t:sql('SELECT COUNT(*) FROM '+t,database).strip().splitlines()[-1] for t in tables}
assert before==after,'Business rows changed during migration'
state={'database':database,'backup':str(backup),'preserved_counts':after,'applied_twice':True}
(ROOT/('.runtime/stage34-test-database.json' if '--test' in sys.argv else '.runtime/stage34-database.json')).write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(state,ensure_ascii=False))

"""Back up before applying the additive migration; optionally clone for isolated acceptance."""
from pathlib import Path
import subprocess,os,re,datetime,json,sys
ROOT=Path(__file__).resolve().parents[1]
config=json.loads((ROOT/'08-environment/config.local.json').read_text(encoding='utf-8-sig'))
env=os.environ.copy()
env['MYSQL_PWD']=os.environ.get('SPRING_DATASOURCE_PASSWORD') or config.get('databasePassword','')
mysql=str(Path(config['mysqlHome'])/'bin/mysql.exe')
args=[mysql,'-h127.0.0.1','-uroot','--default-character-set=utf8mb4']
def sql(query,database=None):
 result=subprocess.run(args+([database] if database else []),input=query.encode(),env=env,capture_output=True)
 if result.returncode: raise RuntimeError(result.stderr.decode(errors='replace'))
 return result.stdout.decode('utf-8')
if __name__=='__main__':
 stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
 backup=ROOT/f'.runtime/backups/mouse_mes_stage1_{stamp}.sql';backup.parent.mkdir(exist_ok=True,parents=True)
 with backup.open('wb') as out:
  result=subprocess.run([str(Path(config['mysqlHome'])/'bin/mysqldump.exe'),'-h127.0.0.1','-uroot','--single-transaction','--routines','--triggers','--default-character-set=utf8mb4','mouse_mes'],env=env,stdout=out,stderr=subprocess.PIPE)
 if result.returncode or backup.stat().st_size<1000: raise RuntimeError('Backup failed: '+result.stderr.decode(errors='replace'))
 database='mouse_mes_stage1_test_'+stamp if '--test' in sys.argv else 'mouse_mes'
 if '--test' in sys.argv:
  sql(f'CREATE DATABASE `{database}` CHARACTER SET utf8mb4;')
  sql(backup.read_text(encoding='utf-8'),database)
 preserved_tables=['t_order','t_production_plan','t_production_task','t_production_report','t_inventory','t_stock_in','t_stock_out']
 before={t:int(sql('SELECT COUNT(*) FROM '+t,database).strip().splitlines()[-1]) for t in preserved_tables}
 sql((ROOT/'04-database/stage1_foundation.sql').read_text(encoding='utf-8'),database)
 sql((ROOT/'04-database/stage1_foundation.sql').read_text(encoding='utf-8'),database)
 after={t:int(sql('SELECT COUNT(*) FROM '+t,database).strip().splitlines()[-1]) for t in preserved_tables}
 assert before==after,'Existing business row counts changed unexpectedly'
 state=dict(database=database,backup=str(backup),applied_twice=True,preserved_counts=after)
 (ROOT/'.runtime/stage1-database.json').write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
 print(json.dumps(state,ensure_ascii=False))

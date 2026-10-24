"""Check the actual eligibility SQL using connection-local temporary fixtures only."""
from pathlib import Path
import json,re
from stage1_database import ROOT,sql
state=json.loads((ROOT/'.runtime/rectification-test.json').read_text(encoding='utf-8'));database=state['database']
assert re.fullmatch(r'mouse_mes_rectification_test_[0-9_]+',database)
source=(ROOT/'01-backend/src/main/java/com/mouse/mes/service/AssignableStaffService.java').read_text(encoding='utf-8')
query=re.search(r'return db.queryForList\("([^"]+)"',source).group(1).replace('=?','=8')
for real,temp in [('t_user','staff_fixture_user'),('t_role','staff_fixture_role'),('mm_role_profile','staff_fixture_profile')]:query=query.replace(real,temp)
fixtures="""CREATE TEMPORARY TABLE staff_fixture_role(id BIGINT,status INT);
CREATE TEMPORARY TABLE staff_fixture_profile(role_id BIGINT,base_role_id BIGINT);
CREATE TEMPORARY TABLE staff_fixture_user(id BIGINT,user_code VARCHAR(20),real_name VARCHAR(30),role_id BIGINT,status INT,deleted INT);
INSERT INTO staff_fixture_role VALUES(8,1),(3,1),(12,1),(13,0),(14,1);
INSERT INTO staff_fixture_profile VALUES(12,8),(13,8),(14,3);
INSERT INTO staff_fixture_user VALUES(1,'1','maintainer',8,1,0),(2,'2','worker',3,1,0),(3,'3','inactive',8,0,0),(4,'4','left',8,1,1),(5,'5','custom-maintainer',12,1,0),(6,'6','disabled-role',13,1,0),(7,'7','custom-worker',14,1,0);
"""
rows=sql(fixtures+query+';',database).splitlines()[1:]
assert {int(row.split('\t')[0]) for row in rows if row.strip()}=={1,5},rows
print('PASS MySQL eligibility: active matching staff and matching custom roles only; inactive, left, disabled and unrelated roles excluded')

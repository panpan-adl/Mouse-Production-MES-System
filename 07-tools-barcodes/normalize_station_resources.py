"""Apply realistic mouse-production resource assignments to the current demo."""
import datetime
import json
from stage1_database import ROOT, sql

DB = 'mouse_mes_four_tier_20261030_171925_883824'
tables = ['mm_station', 'mm_station_resource', 't_equipment']
backup = {}
for table in tables:
    columns = [line.split('\t')[0] for line in sql('SHOW COLUMNS FROM ' + table, DB).splitlines()[1:] if line.strip()]
    expression = ','.join("'%s',`%s`" % (column, column) for column in columns)
    backup[table] = [json.loads(line) for line in sql(f'SELECT JSON_OBJECT({expression}) FROM {table}', DB).splitlines()[1:] if line.strip()]
resources = backup['mm_station_resource']
assert len(resources) == 10 and all(row['resource_type'] == 'EQUIPMENT' for row in resources)
stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
path = ROOT / '.runtime/backups' / f'station_resources_{stamp}.json'
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(backup, ensure_ascii=False, indent=2), encoding='utf-8')
equipment = {
    1: '鼠标综合功能测试台', 2: '自动螺丝锁付机', 3: '产品激光打标机',
    4: '鼠标按键寿命测试台', 5: '光学传感器定位测试台', 6: '彩盒自动包装机',
    7: '鼠标壳体超声波焊接机', 8: 'PCBA回流焊炉', 9: '结构件精密点胶机', 10: '鼠标外观视觉检测台',
}
# Station IDs remain stable so work orders, operations and schedules retain references.
stations = {
    1: ('电子装配站', 8), 2: ('结构装配站', 7), 3: ('总装测试站', 1),
    4: ('贴标包装站', 6), 5: ('光学定位检测站', 5), 6: ('按键寿命测试站', 4),
    7: ('产品激光打标站', 3), 8: ('结构件点胶站', 9), 9: ('成品外观检测站', 10),
    10: ('鼠标螺丝锁付站', 2),
}
updates = ['START TRANSACTION;']
for id, name in equipment.items():
    updates.append(f"UPDATE t_equipment SET equipment_name='{name}' WHERE id={id};")
for id, (name, device) in stations.items():
    updates.append(f"UPDATE mm_station SET name='{name}',equipment_id={device} WHERE id={id};")
for row in resources:
    device = stations[row['station_id']][1]
    updates.append(f"UPDATE mm_station_resource SET equipment_id={device},name='{equipment[device]}（01号）' WHERE id={row['id']};")
updates.append('COMMIT;')
sql('\n'.join(updates), DB)
assert sql('SELECT COUNT(*) FROM mm_station_resource', DB).strip().splitlines()[-1] == '10'
assert sql('SELECT COUNT(*) FROM mm_station_resource r JOIN mm_station s ON s.id=r.station_id WHERE r.equipment_id<>s.equipment_id', DB).strip().splitlines()[-1] == '0'
print('Backup:', path)
print(sql('SELECT r.id,s.name AS station,e.equipment_name,r.name AS resource FROM mm_station_resource r JOIN mm_station s ON s.id=r.station_id JOIN t_equipment e ON e.id=r.equipment_id ORDER BY r.id', DB))

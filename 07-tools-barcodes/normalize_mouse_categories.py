"""Replace placeholder categories with mouse-factory simulation data, preserving IDs."""
import argparse
import datetime
import json
import re
from stage1_database import ROOT, sql


def run(database):
    if not re.fullmatch(r'mouse_mes(?:_[A-Za-z0-9_]+)?', database):
        raise ValueError('Unexpected database name')
    query = "SELECT JSON_OBJECT('id',id,'code',code,'name',name,'parent_id',parent_id,'kind',kind) FROM mm_category ORDER BY id"
    before = [json.loads(line) for line in sql(query, database).splitlines()[1:] if line.strip()]
    expected = {1: '鼠标产品', 2: '办公鼠标', **{i: '演示分类' + str(i) for i in range(3, 11)}, 11: '鼠标产品', 12: '办公鼠标'}
    plan = {
        1: ('鼠标成品', None, 'PRODUCT'), 2: ('有线办公鼠标', 1, 'PRODUCT'),
        3: ('鼠标原材料', None, 'MATERIAL'), 4: ('无线办公鼠标', 1, 'PRODUCT'),
        5: ('塑胶结构件', 3, 'MATERIAL'), 6: ('游戏鼠标', 1, 'PRODUCT'),
        7: ('电子元器件', 3, 'MATERIAL'), 8: ('鼠标半成品', None, 'PRODUCT'),
        9: ('包装与辅料', 3, 'MATERIAL'), 10: ('PCBA电子组件', 8, 'PRODUCT'),
        11: ('结构装配组件', 8, 'PRODUCT'), 12: ('蓝牙双模鼠标', 1, 'PRODUCT'),
    }
    for row in before:
        if row['id'] not in plan or row['name'] not in (expected[row['id']], plan[row['id']][0]):
            raise ValueError('Unrecognized category; review manually before modifying: ' + str(row['id']))
    stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    backup = ROOT / '.runtime/backups' / f'{database}_categories_{stamp}.json'
    backup.parent.mkdir(parents=True, exist_ok=True)
    backup.write_text(json.dumps(before, ensure_ascii=False, indent=2), encoding='utf-8')
    updates = ['START TRANSACTION;']
    for row in before:
        name, parent, kind = plan[row['id']]
        updates.append(f"UPDATE mm_category SET name='{name}',parent_id={parent or 'NULL'},kind='{kind}' WHERE id={row['id']};")
    updates.append('COMMIT;')
    sql('\n'.join(updates), database)
    after = [json.loads(line) for line in sql(query, database).splitlines()[1:] if line.strip()]
    assert len(before) == len(after)
    assert [(r['id'], r['code']) for r in before] == [(r['id'], r['code']) for r in after]
    for row in after:
        assert (row['name'], row['parent_id'], row['kind']) == plan[row['id']]
    print(json.dumps({'database': database, 'backup': str(backup), 'categories': after}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--database', required=True)
    run(parser.parse_args().database)

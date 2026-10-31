"""Fill missing email addresses for the ten seeded demo customers."""
import argparse
import json
from datetime import datetime
from stage1_database import ROOT, sql


def fill_customer_emails(database):
    if not database.startswith(('mouse_mes_completion_test_', 'mouse_mes_four_tier_')):
        raise ValueError('An isolated demo database is required')
    names = ['huadong', 'yunfan', 'qiming', 'xinghe', 'ruijie',
             'kechuang', 'yuanhang', 'lanhai', 'lianchuang', 'haichen']
    condition = "is_deleted=0 AND customer_code BETWEEN 'CUS-000001' AND 'CUS-000010' AND (email IS NULL OR TRIM(email) IN ('', '-')) AND phone LIKE '1380000%'"
    before = sql(f'SELECT id,customer_code,email FROM t_customer WHERE {condition}', database)
    backup = ROOT / '.runtime' / 'backups' / f'customer_emails_{database}_{datetime.now():%Y%m%d_%H%M%S}.tsv'
    backup.parent.mkdir(parents=True, exist_ok=True)
    backup.write_text(before, encoding='utf-8')
    cases = ' '.join(f"WHEN 'CUS-{i:06}' THEN '{name}@example.com'" for i, name in enumerate(names, 1))
    result = sql(f'UPDATE t_customer SET email=CASE customer_code {cases} ELSE email END WHERE {condition}; SELECT ROW_COUNT() AS updated;', database)
    return dict(database=database, result=result.strip(), backup=str(backup))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('database')
    print(json.dumps(fill_customer_emails(parser.parse_args().database)))

"""Add repeatable, explicitly labeled order examples to an isolated demo database."""
import argparse
import datetime as dt
import json
import re

from stage1_database import ROOT, sql


def seed(database):
    if not re.fullmatch(r'mouse_mes_four_tier_\d{8}_\d{6}_\d+', database):
        raise ValueError('An isolated four-tier demo database is required')
    before = sql('SELECT * FROM t_order', database)
    backup = ROOT / '.runtime/backups' / f'order_list_{database}_{dt.datetime.now():%Y%m%d_%H%M%S_%f}.tsv'
    backup.parent.mkdir(parents=True, exist_ok=True)
    backup.write_text(before, encoding='utf-8')

    # Keep the original linked orders intact; only supply their missing promised dates.
    statements = ["UPDATE t_order SET delivery_date=DATE_ADD(order_date, INTERVAL 1 DAY) "
                  "WHERE deleted=0 AND delivery_date IS NULL AND order_no IN "
                  "('MC-172057480565','MC-MULTI-172057480565')"]
    statuses = [0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 4]
    for i, status in enumerate(statuses, 1):
        quantity = [120, 300, 80, 500, 200, 150, 240, 600, 320, 180, 100, 60][i - 1]
        # Completed examples use historical dates; open examples have upcoming delivery dates.
        order_day = dt.date(2026, 10, 31) - dt.timedelta(days=(i % 5) + 1)
        delivery = dt.date(2026, 11, 2) + dt.timedelta(days=i % 8)
        if status in (3, 4):
            order_day = dt.date(2026, 10, 20)
            delivery = dt.date(2026, 10, 29)
        statements.append(f"""
            INSERT INTO t_order
                (order_no,customer_name,customer_phone,product_name,quantity,unit_price,
                 total_amount,order_date,delivery_date,order_status,priority,remark,create_by)
            SELECT 'DEMO-SO-20261031-{i:03}',c.customer_name,c.phone,p.product_name,
                   {quantity},p.selling_price,{quantity}*p.selling_price,
                   '{order_day}','{delivery}',{status},{1 if i in (4, 8) else 2},
                   '模拟订单：用于订单列表、状态筛选与交付日期展示；不代表实际生产或出库',8
            FROM (SELECT customer_name,phone FROM t_customer WHERE is_deleted=0
                  ORDER BY id LIMIT 1 OFFSET {(i - 1) % 9}) c
            CROSS JOIN t_product p
            WHERE p.id={1 + (i - 1) % 6}
              AND NOT EXISTS (SELECT 1 FROM t_order WHERE order_no='DEMO-SO-20261031-{i:03}')
        """)
    sql('START TRANSACTION;\n' + ';\n'.join(statements) + ';\nCOMMIT;', database)
    checks = sql("""SELECT COUNT(*) AS total,
        SUM(delivery_date IS NULL) AS missing_delivery,
        SUM(delivery_date < order_date) AS invalid_delivery,
        SUM(total_amount <> quantity*unit_price) AS invalid_amount
        FROM t_order WHERE deleted=0;
        SELECT order_status,COUNT(*) AS total FROM t_order WHERE deleted=0 GROUP BY order_status;
    """, database)
    return dict(database=database, backup=str(backup), checks=checks)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('database')
    print(json.dumps(seed(parser.parse_args().database), ensure_ascii=False))

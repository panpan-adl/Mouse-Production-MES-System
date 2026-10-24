ALTER TABLE t_purchase_order ADD COLUMN IF NOT EXISTS sales_order_id BIGINT COMMENT '关联的销售订单ID';

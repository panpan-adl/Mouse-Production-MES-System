"""Enrich the active isolated mouse factory dataset; preserve quantities and workflow state.

All companies, contacts and commercial details are fictional training fixtures.
Historical audit/authentication records are deliberately excluded.
"""
import datetime as dt
import json
import re
import subprocess
from collections import Counter
from pathlib import Path

from stage1_database import ROOT, sql, config, env


def quote(value):
    if value is None:
        return 'NULL'
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace('\\', '\\\\').replace("'", "''") + "'"


def main():
    database = json.loads((ROOT / '.runtime/four-tier-20261030/state.json').read_text())['database']
    assert re.fullmatch(r'mouse_mes_four_tier_\d{8}_\d{6}_\d+', database)
    stamp = dt.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    backup = ROOT / '.runtime/backups' / f'realistic_business_{stamp}.sql'
    backup.parent.mkdir(parents=True, exist_ok=True)
    with backup.open('wb') as out:
        result = subprocess.run([str(Path(config['mysqlHome']) / 'bin/mysqldump.exe'),
                                 '-h127.0.0.1', '-uroot', '--single-transaction',
                                 '--default-character-set=utf8mb4', database],
                                env=env, stdout=out, stderr=subprocess.PIPE)
    assert result.returncode == 0 and backup.stat().st_size > 1000, 'Database backup failed'
    schema = {}
    for line in sql("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,ORDINAL_POSITION", database).splitlines()[1:]:
        if line.strip():
            table, col, kind = line.split('\t')
            schema.setdefault(table, {})[col] = kind

    def read(table, columns=None):
        columns = columns or list(schema[table])
        query = 'SELECT JSON_OBJECT(' + ','.join(quote(c) + ',`' + c + '`' for c in columns) + ') FROM `' + table + '`'
        return [json.loads(line) for line in sql(query, database).splitlines()[1:] if line.strip()]

    excluded = {'mm_audit', 'mm_query_audit', 'mm_agent_run', 'mm_ai_session',
                'mm_job_run', 'mm_auto_event', 't_operation_log', 't_login_log',
                'mm_account_alert', 'mm_menu', 'mm_parameter', 'mm_code_rule',
                'mm_auto_rule', 'mm_job_config', 't_role', 'mm_legacy_link'}
    records = {t: read(t) for t in schema if t not in excluded and t != 't_user' and 'id' in schema[t]}
    records['t_user'] = read('t_user', ['id', 'real_name', 'phone', 'email', 'entry_date'])
    changes = {}
    code_map = {}

    def update(table, row_id, **values):
        existing = next(r for r in records[table] if r['id'] == row_id)
        target = changes.setdefault((table, row_id), {})
        for key, value in values.items():
            if existing.get(key) != value:
                target[key] = value

    # Standard business identifiers replace regression-test/UUID identifiers.
    prefixes = {'t_order': ('order_no', 'SO'), 't_purchase_order': ('po_no', 'PO'),
                't_product': ('product_code', 'PRD'), 't_material': ('material_code', 'MAT'),
                't_supplier': ('supplier_code', 'SUP'), 't_equipment': ('equipment_code', 'EQ'),
                't_warehouse': ('warehouse_code', 'WH'), 't_quality_check': ('check_no', 'QC'),
                't_lot': ('lot_no', 'LOT'), 't_production_order': ('po_no', 'PPO'),
                't_production_plan': ('plan_no', 'PLAN'), 't_production_task': ('task_no', 'TASK'),
                't_material_request': ('request_no', 'MR'),
                't_qc_inspection': ('inspection_no', 'IQC'), 't_uom': ('uom_code', 'UOM')}
    config_prefix = {'mm_area': 'AREA', 'mm_location': 'LOC', 'mm_workshop': 'WS',
                     'mm_station': 'ST', 'mm_route': 'RT', 'mm_operation': 'OP',
                     'mm_bom': 'BOM', 'mm_team': 'TEAM', 'mm_shift': 'SHIFT',
                     'mm_check_plan': 'CHK', 'mm_check_subject': 'CS', 'mm_qc_item': 'QCI',
                     'mm_qc_template': 'QCT', 'mm_position': 'POS', 'mm_sop': 'SOP',
                     'mm_rotation': 'ROT', 'mm_category': 'CAT'}
    prefixes.update({t: ('code', p) for t, p in config_prefix.items()})
    for table, (column, prefix) in prefixes.items():
        for row in records.get(table, []):
            old = row.get(column) or ''
            if re.search(r'(?i)^(MC[-_]|DEMO[-_]|VERIFY[-_]|FOUR[-_]|AUDIT[-_]|TEST[-_])|^[a-f0-9]{20,}$', old):
                new = f'{prefix}-202610-{row["id"]:05}'
                code_map[old] = new
                update(table, row['id'], **{column: new})

    # Geographic addresses match company names. Phone numbers are synthetic switchboards.
    customer_cities = ['苏州', '苏州', '杭州', '深圳', '南京', '合肥', '成都', '武汉', '长沙', '厦门']
    districts = ['工业园区智造产业园', '高新区电子产业园', '滨江区数字产业园', '宝安区创智园',
                 '雨花台区软件产业园', '高新区科创园', '高新区天府智造园', '东湖高新区光谷产业园',
                 '岳麓区科技产业园', '集美区软件产业园']
    area_codes = ['0512', '0512', '0571', '0755', '025', '0551', '028', '027', '0731', '0592']
    customer_contacts = ['许知远', '沈佳宁', '陆晨曦', '程子谦', '顾思妍', '邵文博', '唐若涵', '宋嘉禾', '袁景行', '何雨桐']
    for i, row in enumerate(records['t_customer']):
        n = i % 10
        update('t_customer', row['id'], contact_person=customer_contacts[n], address=f'{customer_cities[n]}市{districts[n]}{n+1}号楼{201+n*16}室',
               phone=f'{area_codes[n]}-5550{2101+n*17:04}',
               email=f'purchase.{row["customer_code"].lower()}@example.com')
    supplier_cities = ['苏州', '昆山', '东莞', '深圳', '常州', '无锡', '苏州', '宁波', '常州', '昆山', '苏州']
    supplier_names = ['沈建华', '许文静', '蒋志强', '何佳宁', '陆明杰', '唐雅雯', '徐浩然', '邵文斌', '宋佳慧', '顾承志', '严志诚']
    supplier_area_codes = ['0512', '0512', '0769', '0755', '0519', '0510', '0512', '0574', '0519', '0512', '0512']
    for i, row in enumerate(records['t_supplier']):
        update('t_supplier', row['id'], contact_person=supplier_names[i % 11],
               contact_phone=f'{supplier_area_codes[i%11]}-5551{3101+i*23:04}',
               address=f'{supplier_cities[i%11]}市先进制造产业园{12+i*6}号{1+i%3}号厂房',
               remark=['塑胶结构件与模具配套，按图纸版本交货', 'PCBA与电子元器件供应，来料需附批次检验报告',
                       '鼠标装配辅料与包装配套，按采购订单分批交付'][i % 3])
    # A specification is tied to a component, rather than random generic text.
    specs = [('上盖', 'ABS阻燃塑胶，哑黑色，109×62mm，图纸A100-H01 Rev.C'),
             ('下盖', 'ABS塑胶，螺柱孔径2.0mm，图纸A100-H02 Rev.C'),
             ('PCB', '双面FR-4板，1.0mm厚，USB光学方案，PCBA-A100 Rev.B'),
             ('微动', '三脚直插，额定寿命500万次，触发力65±15gf'),
             ('编码器', '11mm机械编码器，24格/圈，带定位反馈'),
             ('USB', 'USB-A接口，四芯屏蔽线，长度1.5m，黑色PVC外被'),
             ('连接线', 'USB-A接口，四芯屏蔽线，长度1.5m，黑色PVC外被'),
             ('滚轮', 'TPE包胶滚轮，外径22mm，轴径2mm'),
             ('透镜', '光学级PMMA，配套1200DPI传感器，定位柱无毛刺'),
             ('螺钉', 'M2×6十字盘头自攻螺钉，碳钢镀镍，扭矩0.18±0.02N·m'),
             ('脚贴', 'PTFE脚贴，0.6mm厚，背胶离型纸，四片/套'),
             ('铭牌', '哑银PET，30×18mm，序列号及产品编码清晰可读'),
             ('包装盒', '250g白卡彩盒，125×78×45mm，内含纸质内托'),
             ('蓝牙芯片', '低功耗蓝牙5.0 SoC，QFN封装，3.3V工作电压'),
             ('接收器', '2.4GHz USB-A微型接收器，配套W200固件'),
             ('充电接口', 'USB Type-C母座，16Pin，贴片安装'),
             ('锂电池', '3.7V 500mAh聚合物电池，带保护板及JST插头'),
             ('侧键', 'ABS侧键支架及按键帽，左右一组'),
             ('内托', '可回收纸浆内托，适配125×78×45mm彩盒'),
             ('防静电袋', '防静电屏蔽袋，120×180mm，自封口'),
             ('说明书', 'A6折页，80g双胶纸，中英双语，Rev.B'),
             ('标签', '40×25mm热敏标签，含型号、条码与批次信息')]
    for row in records['t_material']:
        name = row['material_name']
        spec = next((v for k, v in specs if k in name), None)
        if not spec:
            spec = '按产品BOM及对应图纸组装，逐件功能检验，防静电周转'
        values = dict(specification=spec, remark='采购与检验按当前图纸版本执行，收发料须记录批次及库位')
        if int(row['material_type']) == 1:
            values.update(safety_stock=400 if '螺钉' in name else (200 if '微动' in name else 100), max_stock=5000)
        update('t_material', row['id'], **values)
    product_specs = {
        1: ('M100', 'USB有线，1000DPI，3键，1.5m线长，约85g'),
        2: ('W200', '2.4GHz/蓝牙5.0双模，800/1200/1600DPI，5键，500mAh'),
        3: ('G500', 'USB有线，800至6400DPI可调，6键，RGB灯效，1.8m编织线'),
        4: ('O200', '静音微动，2.4GHz无线，1200DPI，3键，单节AA供电'),
        5: ('U100', 'USB-A即插即用，1000DPI，3键，1.5m线长'),
        6: ('A100', 'USB有线，1200DPI，3键，哑黑ABS壳体，1.5m线长'),
        7: ('A100-H01', 'A100上壳与左右键预装，卡扣无白化，按键回弹正常'),
        8: ('A100-PCBA', 'A100控制板焊接组件，含传感器及开关，通过通断检验'),
        9: ('B200', '2.4GHz无线，1600DPI，3键，配微型接收器'),
        10: ('M10', '2.4GHz无线办公，800/1200/1600DPI，4键，单节AA供电'),
        11: ('A100-H02', 'A100下壳与透镜预装，定位柱完整，透镜洁净'),
        12: ('A100-PCBAT', 'A100 PCBA通过USB枚举、按键响应及光学定位测试'),
        13: ('B200-D', '蓝牙5.0/2.4GHz双模，三档DPI，Type-C充电')}
    for row in records['t_product']:
        model, spec = product_specs.get(row['id'], ('MOUSE', '按产品工艺图纸制造'))
        if '箱' in row['unit']:
            spec += '；10件/周转箱'
        update('t_product', row['id'], model=model, specification=spec,
               description=f'{row["product_name"]}，按已维护BOM和工艺路线组织生产，包装前核对型号、外观及功能。')
        if row['id'] in (7, 8, 9, 10, 11, 12, 13):
            cost, price = {7:(6.8,10.2), 8:(14.5,21.8), 9:(32,69), 10:(45,99),
                           11:(5.6,8.4), 12:(16.2,24.3), 13:(38,89)}[row['id']]
            factor = 10 if '箱' in row['unit'] else 1
            update('t_product', row['id'], cost_price=round(cost*factor,2), selling_price=round(price*factor,2))
    equipment_models = ['FT-1200', 'AS-220', 'LM-20F', 'KL-6', 'OT-1600', 'PK-35', 'UW-1520', 'RF-8Z', 'DG-300', 'VI-500', 'FT-2400', 'PS-180', 'CW-30']
    equipment_types = ['功能测试', '自动锁付', '激光标识', '寿命测试', '光学检测', '自动包装', '超声波焊接', '回流焊接', '精密点胶', '视觉检测', '功能测试', '气动锁付', '称重复核']
    for i, row in enumerate(records['t_equipment']):
        update('t_equipment', row['id'], model=equipment_models[i % 13], equipment_type=equipment_types[i % 13],
               manufacturer=['苏州恒研自动化设备有限公司', '无锡精衡测控设备有限公司', '常州启维智能装备有限公司'][i % 3],
               location=f'一号厂房{1+i%2}层·{equipment_types[i%13]}区{i+1:02}号机位',
               responsible_id=9, responsible_name='黄文博',
               purchase_date=f'2025-{1+i%12:02}-{8+i%17:02}', warranty_period=24,
               purchase_price=[18500, 32600, 21800, 12900, 24500, 66800, 17600, 128000, 28600, 39800, 22800, 8600, 6800][i % 13],
               remark='建立设备履历，开机前点检；计量与保养按计划执行')
    for row in records['t_warehouse']:
        name = '原辅材料综合仓' if row['warehouse_name'] == '默认仓库' else row['warehouse_name']
        update('t_warehouse', row['id'], warehouse_name=name,
               location=f'{"二号" if row["id"]%2 else "一号"}厂房一层·{name}存储区',
               manager_id=6, manager_name='吴俊杰', remark='按物料批次、库区和库位管理，收发料遵循先进先出')
    for row in records['mm_area']:
        w = next((w for w in records['t_warehouse'] if w['id'] == row['warehouse_id']), None)
        label = (w['warehouse_name'].replace('默认仓库', '原辅材料综合仓') if w else '物料仓')
        update('mm_area', row['id'], name=f'{label}·{["来料暂存区","合格品货架区","周转备料区"][row["id"]%3]}{row["id"]:02}')
    for row in records['mm_location']:
        update('mm_location', row['id'], name=f'R{row["area_id"]:02}-{(row["id"]%6)+1:02}-01货架位')
    for row in records['mm_operation']:
        labels = {1: 'PCBA焊接与电子装配', 2: '上壳及滚轮结构装配', 3: '整机锁付与功能测试', 4: '铭牌粘贴与彩盒包装',
                  5: '上壳按键组件预装', 6: 'PCBA元件焊接与通断检查', 11: '下壳透镜组件预装', 12: 'PCBA通电功能检查'}
        if row['id'] in labels:
            update('mm_operation', row['id'], name=labels[row['id']])
    for row in records.get('mm_sop', []):
        op = next((r for r in records['mm_operation'] if r['id'] == row['operation_id']), None)
        name = changes.get(('mm_operation', op['id']), {}).get('name', op['name']) if op else '整机装配'
        update('mm_sop', row['id'], name=f'A100·{name}作业指导书 {row["id"]:02}')
    standards = ['保护接地导通，电源线无破损，接线端子紧固', '急停触发后设备立即停机，复位后不得自动重启',
                 '导轨及丝杆润滑充分，无干摩擦和可见渗漏', '气源压力0.50至0.65MPa，接头无可听泄漏',
                 '工装及防护罩紧固件齐全，防松标记无错位', '传感器响应正常，测试信号连续稳定',
                 '定位销无磨损松动，工件放置后无翘起', '空载运行无异常振动、碰擦和尖锐噪声',
                 '散热风扇运行正常，滤网无积尘堵塞', '台面无散落螺钉及碎屑，防静电接地有效']
    for i, row in enumerate(records['mm_check_subject']):
        update('mm_check_subject', row['id'], standard=standards[i % 10])
    for row in records['mm_check_plan']:
        eq = next((e for e in records['t_equipment'] if e['id'] == row['equipment_id']), None)
        update('mm_check_plan', row['id'], name=f'{eq["equipment_name"] if eq else "产线设备"}·开机点检{row["id"]:02}',
               standard='逐项确认电源接地、安全防护、工装定位和功能测试；异常先停机报修，不得带故障投产')
    for row in records['mm_maintenance_plan']:
        eq = next(e for e in records['t_equipment'] if e['id'] == row['equipment_id'])
        kind = eq['equipment_name']
        action = ('清理光路与镜片，校验标准样件，复核定位精度' if any(k in kind for k in ['测试', '视觉', '打标']) else
                  '清洁传动机构，润滑导轨，检查紧固件，复核工装定位与安全联锁')
        update('mm_maintenance_plan', row['id'], content=f'{kind}：{action}；试运行20分钟，记录异常与更换件')
    qc_standards = {1: '外壳无裂纹、缺料及明显划伤；装配间隙均匀，卡扣锁止完整',
                    2: 'USB接入后3秒内识别为HID设备，连续插拔10次均可正常识别',
                    3: '左右键各点击20次，滚轮正反各10格，无漏触发和重复触发',
                    4: '在标准测试垫完成水平、垂直及圆形轨迹，无跳点或断线',
                    6: '自然光下距产品30cm观察，无明显色差、流痕及可见划伤',
                    7: '连续按压20次，触发一致、回弹顺畅，无卡滞及异响',
                    8: '正反向各滚动20格，每格反馈清晰，中键按压10次无漏报',
                    9: '连接标准主机连续运行30分钟，无异常断连及供电重启',
                    10: '整机、接收器（无线型号）、说明书及保修卡齐全，标签型号一致'}
    for row in records['mm_qc_item']:
        if row['id'] in (5, 11):
            update('mm_qc_item', row['id'], name=f'装配间隙偏差{ "复核" if row["id"]==11 else "检测" }',
                   standard='记录相对基准间隙的绝对偏差，单位0.01mm；允许0至10，即偏差不超过0.10mm')
        elif row['id'] in qc_standards:
            update('mm_qc_item', row['id'], standard=qc_standards[row['id']])
    for row in records['mm_qc_template']:
        if '验收' in row['name']:
            update('mm_qc_template', row['id'], name=f'A100产线首件检验规范 {row["id"]:02}')
    for i, row in enumerate(records['t_quality_standard']):
        standard_id = [6, 7, 8, 9, 10][i % 5]
        update('t_quality_standard', row['id'], standard_content=qc_standards[standard_id])
    notices = [
        ('11月首周生产排程确认', 'A100产线按已审批工单组织生产。班组长在开工前核对BOM版本、工艺路线和人员配置；缺料或设备异常应先登记，调整排程后再开工。'),
        ('PCBA防静电作业要求', '电子装配岗位进入工位前检测防静电腕带。PCBA使用防静电周转盘，禁止徒手接触焊盘；离岗时按批次封存并交接数量。'),
        ('无线接收器配对确认', 'W200及B200无线产品包装前逐套确认接收器配对。测试完成后整机与接收器保持一一对应，换箱时核对产品标签。'),
        ('月末库存盘点安排', '仓储部按仓库、库区、库位盘点原料及成品。盘点期间暂停对应库位收发，差异由仓管与复核人员共同登记确认。'),
        ('安全生产与急停检查', '班前检查设备防护罩、接地和急停开关。处理卡料须先断电卸压，严禁跨越防护区域或短接安全联锁。'),
        ('客户交付与包装核对', '销售与仓储按订单交期安排发货。装箱前核对客户、型号、箱数和批次，待出货检验合格后办理出库。'),
        ('A100物料齐套确认', '计划员在工单下达前核对上盖、下盖、PCBA、微动开关、线材及包材。缺料需求由采购确认到货日期并反馈计划员。'),
        ('新员工装配岗位培训', '新增操作人员须完成防静电、螺丝扭矩、首件检验及异常报告培训。培训后由班组长安排带教并确认独立操作资格。'),
        ('生产班组交接要求', '交接时核对在制品、物料批次、工单进度及设备异常。未完成的质量处置须说明责任人和下一步处理事项。'),
        ('鼠标彩盒包装规范', '彩盒不得压伤和破损，内托固定可靠。整机、说明书与配件按装箱清单核对，外箱标签与产品型号、数量及批次一致。'),
        ('工装夹具保养安排', '设备部对A100装配定位夹具进行清洁和防松检查，复核定位销与锁付扭矩，保养完成后使用标准样件确认。'),
        ('来料检验批次管理', '采购收货按供应商送货批次登记，未经检验合格不得转入可用库存。不合格批次按隔离标识存放，禁止混批领用。'),
        ('质量异常闭环要求', '过程检验发现异常后暂停对应工序流转，品质人员确认处置方式。返工产品重新检验合格后，方可继续入库或发货。')]
    for i, row in enumerate(records['mm_notice']):
        title, content = notices[i % len(notices)]
        update('mm_notice', row['id'], title=title, content=content)
    doc_labels = {'PURCHASE_RECEIPT': ('采购收货', '按采购订单核对物料、数量与供应商批次，检验后办理入库'),
                  'PURCHASE_RETURN': ('采购退货', '按质量处置结论退回供应商，核对退货批次与数量'),
                  'LINE_ISSUE': ('生产领料', '按工单BOM核对齐套数量，记录领用批次和目标工序'),
                  'LINE_RETURN': ('生产退料', '清点产线余料，按原领料批次复核后退库'),
                  'PRODUCTION_RECEIPT': ('成品入库', '按工单完成数量办理入库，复核成品批次及质量放行状态'),
                  'SALES_SHIPMENT': ('销售发货', '按客户订单核对型号、数量与包装标签，检验合格后发货'),
                  'SALES_RETURN': ('销售退货', '核对原发货来源和退回数量，按检验结论分类存放')}
    for row in records['mm_document']:
        title, reason = doc_labels.get(row['kind'], ('仓储作业', '按业务来源核对物料与批次'))
        update('mm_document', row['id'], name=f'{title}·{row["code"]}', reason=reason)
    order_notes = ['客户年度办公设备配套，统一黑色机身及中性包装', '渠道补货订单，外箱按型号分别标识',
                   '企业办公批量采购，随箱附使用说明与保修卡', '电商渠道备货，包装条码须与订单型号一致',
                   '区域经销商补货，按约定批次分批交付', '园区集中采购，整箱交货并附装箱清单']
    for i, row in enumerate(records['t_order']):
        update('t_order', row['id'], remark=order_notes[i % 6])
    for row in records['t_purchase_order']:
        values = {'remark': '按当前物料规格与图纸版本供货，送货单须注明采购单号及批次'}
        day = (row.get('created_at') or '2026-10-30')[:10]
        if not row.get('order_date'):
            values['order_date'] = day
        if not row.get('expected_date'):
            values['expected_date'] = (dt.date.fromisoformat(day) + dt.timedelta(days=5)).isoformat()
        update('t_purchase_order', row['id'], **values)
    for row in records['t_user']:
        update('t_user', row['id'], phone=f'0512-5552{4000+row["id"]:04}',
               email=f'staff.{row["id"]:03}@example.com',
               entry_date=row['entry_date'] or f'2025-{1+row["id"]%12:02}-08')
    for row in records.get('t_maintenance', []):
        update('t_maintenance', row['id'], fault_desc='USB测试通道接触不稳定，标准样件重复检测时偶发掉线',
               maintenance_content='清洁测试探针并更换USB转接线，紧固接口，使用标准样件完成连续30次复测')
    dict_types = ['packing_method', 'transport_method', 'delivery_method', 'payment_method',
                  'product_color', 'surface_finish', 'mouse_interface', 'quality_grade', 'storage_zone', 'aftersales_type']
    dict_labels = ['中性彩盒', '公路零担', '仓库交付', '月结30天', '哑光黑', '细纹喷涂', 'USB-A', '常规抽检', '合格品区', '质量退换']
    for i, row in enumerate(records.get('t_dict', [])):
        if (row.get('dict_type') or '').startswith('demo_business_'):
            code_map[row['dict_type']] = dict_types[i % 10]
            update('t_dict', row['id'], dict_type=dict_types[i % 10], remark='业务基础选项，供订单及生产资料维护参考')
            for item in records.get('t_dict_item', []):
                if item['dict_type'] == row['dict_type']:
                    update('t_dict_item', item['id'], dict_type=dict_types[i % 10],
                           item_label=dict_labels[i % 10], item_value=dict_types[i % 10]+'_standard', remark='常用业务选项')
    # Replace exact identifiers throughout business snapshots (never audit history).
    replacements = sorted(code_map.items(), key=lambda pair: len(pair[0]), reverse=True)
    for table, rows in records.items():
        for row in rows:
            for col, kind in schema[table].items():
                if kind not in ('varchar', 'text', 'longtext', 'mediumtext') or col not in row:
                    continue
                value = changes.get((table, row['id']), {}).get(col, row[col])
                if not isinstance(value, str):
                    continue
                new = value
                for old, replacement in replacements:
                    new = new.replace(old, replacement)
                if col in ('name', 'standard_name', 'title', 'remark', 'description', 'specification', 'fault_desc', 'maintenance_content'):
                    new = re.sub(r'审计固定库区[a-f0-9]+', '待检物料隔离区', new)
                    new = re.sub(r'四档启停验证\d+', '来料复核暂存位', new)
                    new = new.replace('临时流程验证', '客户追加采购，交期由销售与计划共同确认')
                    new = new.replace('演示', '').replace('答辩', '').replace('验收', '首件确认')
                if new != value:
                    update(table, row['id'], **{col: new})

    # Synchronize denormalized names from the canonical master records.
    statements = []
    for (table, row_id), values in changes.items():
        if values:
            statements.append('UPDATE `' + table + '` SET ' + ','.join('`'+k+'`='+quote(v) for k,v in values.items()) + ' WHERE id=' + str(row_id))
    statements += [
        'UPDATE t_location l JOIN mm_location m ON m.code=l.location_code SET l.location_name=m.name',
        'UPDATE mm_work_material w JOIN t_material m ON m.id=w.material_id SET w.material_name=m.material_name,w.material_code=m.material_code',
        'UPDATE t_material_request r JOIN t_material m ON m.id=r.material_id SET r.material_name=m.material_name,r.material_code=m.material_code',
        'UPDATE t_order o JOIN t_customer c ON c.customer_name=o.customer_name SET o.customer_phone=c.phone',
    ]
    invariants_query = """SELECT JSON_OBJECT(
        'inventory', (SELECT COALESCE(SUM(quantity),0) FROM t_inventory),
        'inventory_rows', (SELECT COUNT(*) FROM t_inventory),
        'ledger_rows', (SELECT COUNT(*) FROM t_inventory_transaction),
        'reported', (SELECT COALESCE(SUM(report_quantity),0) FROM t_production_report),
        'work_orders', (SELECT COUNT(*) FROM mm_work_order),
        'documents', (SELECT COUNT(*) FROM mm_document),
        'bom_lines', (SELECT COUNT(*) FROM mm_bom_line)) AS snapshot"""
    def invariant():
        return json.loads(next(l for l in sql(invariants_query, database).splitlines()[1:] if l.strip()))
    before = invariant()
    script = 'START TRANSACTION;\n' + ';\n'.join(statements) + ';\nCOMMIT;'
    path = ROOT / '.runtime' / f'realistic_business_{stamp}.sql'
    path.write_text(script, encoding='utf8')
    sql(script, database)
    after = invariant()
    assert before == after, 'Business quantity/count invariants changed'
    report = dict(database=database, simulated=True, backup=str(backup), migration=str(path),
                  changed_rows_by_table=dict(Counter(t for (t, _), v in changes.items() if v)),
                  preserved=after, identifier_changes=len(code_map))
    history_path = ROOT / '.runtime/realistic-business-history.json'
    history = json.loads(history_path.read_text(encoding='utf8')) if history_path.exists() else []
    history.append(report)
    history_path.write_text(json.dumps(history, ensure_ascii=False, indent=2), encoding='utf8')
    (ROOT / '.runtime/realistic-business-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf8')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()

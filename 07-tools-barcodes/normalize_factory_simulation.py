"""Normalize presentation data in the authorized local mouse-factory simulation.

Keeps IDs, document numbers, enum values, stock quantities, money, dates and audit
history unchanged. A full database backup is required before a transaction runs.
"""
import datetime
import json
import re
import subprocess
from pathlib import Path
from stage1_database import ROOT, sql, config, env

DB = 'mouse_mes_four_tier_20261030_171925_883824'
stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
out = ROOT / '.runtime/backups' / ('factory_simulation_' + stamp)
out.mkdir(parents=True)
with (out / 'database.sql').open('wb') as stream:
    proc = subprocess.run([str(Path(config['mysqlHome']) / 'bin/mysqldump.exe'),
        '-h127.0.0.1', '-uroot', '--single-transaction', '--routines', '--triggers',
        '--default-character-set=utf8mb4', DB], env=env, stdout=stream, stderr=subprocess.PIPE)
if proc.returncode or (out / 'database.sql').stat().st_size < 1000:
    raise RuntimeError('Full backup failed; no data changed')

def rows(table):
    fields = [line.split('\t')[0] for line in sql('SHOW COLUMNS FROM `' + table + '`', DB).splitlines()[1:] if line.strip()]
    pairs = ','.join("'%s',`%s`" % (field, field) for field in fields)
    return [json.loads(line) for line in sql(f'SELECT JSON_OBJECT({pairs}) FROM `{table}`', DB).splitlines()[1:] if line.strip()]

schema = {}
for line in sql("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,ORDINAL_POSITION", DB).splitlines()[1:]:
    if line.strip():
        table, column, dtype = line.split('\t'); schema.setdefault(table, {})[column] = dtype
data = {table: rows(table) for table in schema if table.startswith(('t_', 'mm_'))
    and not any(word in table for word in ['log', 'audit', 'event', 'agent', 'chat', 'message', 'history', 'snapshot', 'query'])
    and 'id' in schema[table]}
original = json.loads(json.dumps(data))
rename = {}
def name(table, column, id, value):
    for row in data.get(table, []):
        if row['id'] == id:
            old = row.get(column)
            if isinstance(old, str) and old != value: rename[old] = value
            row[column] = value

people = ['陈志远','李明轩','王建国','赵宇航','周雅雯','吴俊杰','郑思远','陈嘉诚','黄文博','钱静怡','林浩然','刘泽宇','孙启航','何承宇','许景程','蒋瑞安']
for row, person in zip(data['t_user'], people): name('t_user','real_name',row['id'],person)
for row in data['t_customer']:
    i=row['id']-1
    row['contact_person']=['林晓雯','王俊峰','陈思雨','李泽宇','周明远','赵雅琪','徐浩然','孙嘉宁','郑文轩','吴雨桐'][i%10]
    row['address']=['苏州市工业园区星港街88号','杭州市滨江区江虹路168号','南京市雨花台区软件大道66号','合肥市高新区创新大道120号'][i%4]
suppliers=['苏州恒信精密塑胶有限公司','昆山瑞芯电路有限公司','东莞启程电子开关有限公司','深圳锐视光电有限公司','常州联畅线缆有限公司','无锡科源电子有限公司','苏州嘉和包装有限公司','宁波精固五金有限公司','常州润彩新材料有限公司','昆山匠成模具有限公司','苏州诚品电子制造有限公司']
for i,row in enumerate(data['t_supplier']):
    name('t_supplier','supplier_name',row['id'],suppliers[i%len(suppliers)])
    row['contact_person']=['沈建华','许文静','蒋志强','何佳宁','陆明杰','唐雅雯'][i%6]
    row['address']=['苏州市吴中区兴南路36号','昆山市开发区同丰路128号','东莞市长安镇振安路56号'][i%3]
product_names={1:'M100基础有线鼠标',2:'W200无线办公鼠标',3:'G500电竞鼠标',4:'O200静音办公鼠标',5:'U100 USB有线鼠标',7:'A100上壳预装组件',8:'A100主板焊接组件',9:'B200无线鼠标',10:'M10无线办公鼠标',11:'A100下壳预装组件',12:'A100主板测试组件',13:'B200双模鼠标'}
for id,value in product_names.items():name('t_product','product_name',id,value)
material_names={1:'M2×6十字盘头螺钉',17:'B200无线主板组件（外购）',18:'B200无线鼠标整机',49:'B200双模主板组件（外购）',50:'B200双模鼠标整机'}
for id,value in material_names.items():name('t_material','material_name',id,value)
for row in data['t_material']:
    old=row['material_name']
    if old.startswith('验收'): name('t_material','material_name',row['id'],'A100 '+old[2:]+'（量产型）')
    elif old.startswith('演示备料·'): name('t_material','material_name',row['id'],'W200 '+old.split('·',1)[1])
    elif '演示M10' in old: name('t_material','material_name',row['id'],old.replace('演示M10','M10'))
unit_names={8:'盘',9:'料盒（20盘）',10:'周转箱（10个）',11:'托',12:'托箱（20托）',13:'周转箱（10套）'}
for id,value in unit_names.items():name('t_uom','uom_name',id,value)
workshops={2:'光学检测车间',3:'整机组装车间',4:'产品测试车间',5:'成品包装车间',6:'售后返修车间',7:'线束加工车间',8:'表面处理车间',9:'自动化装配车间',10:'新品试制车间',11:'A100柔性装配车间',13:'模具维护车间',15:'工装调试车间'}
for id,value in workshops.items():name('mm_workshop','name',id,value)
for row in data['mm_station']:
    if any(s in row['name'] for s in ['演示','隔离']): name('mm_station','name',row['id'],f"新品试制装配{row['id']-10:02d}站" if row['id']!=20 else 'A100柔性总装站')
for row in data['mm_team']:
    if any(s in row['name'] for s in ['演示','验收']):name('mm_team','name',row['id'],['电子装配','结构装配','整机总装','功能检测','贴标包装'][(row['id']-1)%5]+f"{(row['id']-1)//5+1}班")
for row in data['mm_shift']:
    if any(s in row['name'] for s in ['演示','验收']):name('mm_shift','name',row['id'],f"生产班次 {str(row['start_time'])[:5]}—{str(row['end_time'])[:5]}（{row['id']:02d}）")
positions=['生产操作员','电子装配员','结构装配员','品质检验员','仓库管理员','采购专员','设备维护员','工艺技术员','生产计划员','包装操作员','产线巡检员']
for i,row in enumerate(data['mm_position']):name('mm_position','name',row['id'],positions[i%len(positions)])
for row in data['t_warehouse']:
    old=row['warehouse_name']
    if '演示' in old:name('t_warehouse','warehouse_name',row['id'],old.replace('演示',''))
    elif '审计库位测试仓' in old:name('t_warehouse','warehouse_name',row['id'],'工装备件周转仓' if row['id']==12 else '线束材料周转仓')
for row in data['mm_area']:
    if '演示' in row['name']:name('mm_area','name',row['id'],f"{row['warehouse_id']:02d}仓收储区")
for table,column in [('mm_location','name'),('t_location','location_name')]:
    for row in data[table]:
        if '演示' in row[column]:name(table,column,row['id'],f"A-{row['id']:02d}-01货架位")
for row in data['t_equipment']:
    if any(s in row['equipment_name'] for s in ['隔离','四档状态验证']):name('t_equipment','equipment_name',row['id'],{11:'A100产线综合测试台',13:'备用气动锁付机',15:'包装称重复核台'}.get(row['id'],f"鼠标装配辅助设备{row['id']:02d}"))
    if row.get('model')=='TEST':row['model']='AS-200'
    if row.get('location') and any(s in row['location'] for s in ['隔离','演示']):row['location']='鼠标制造园区一号厂房'
products={r['id']:r['product_name'] for r in data['t_product']}
devices={r['id']:r['equipment_name'] for r in data['t_equipment']}
stations={r['id']:r['name'] for r in data['mm_station']}
for table,suffix in [('mm_bom','物料清单'),('mm_route','生产工艺路线'),('mm_sop','装配作业指导书')]:
    for row in data[table]:
        if any(s in row['name'] for s in ['演示','隔离']):name(table,'name',row['id'],f"{products.get(row.get('product_id'),'A100鼠标')}·{suffix}（{row['id']:02d}）")
        if str(row.get('version','')).startswith('DEMO-'):row['version']='V1.'+str(row['id'])
for row in data['mm_operation']:
    if '演示' in row['name']:name('mm_operation','name',row['id'],stations.get(row['station_id'],'整机装配')+'作业')
for table,suffix in [('mm_check_plan','日常点检'),('mm_maintenance_plan','月度保养')]:
    for row in data[table]:
        if any(s in row['name'] for s in ['演示','隔离']):name(table,'name',row['id'],devices.get(row['equipment_id'],'装配设备')+f"·{suffix}{row['id']:02d}")
subjects=['设备供电与接地','安全防护与急停','传动机构润滑','气源压力与泄漏','紧固件防松','传感器与信号','工装定位精度','运动部件异响','控制柜散热','作业区域清洁']
for i,row in enumerate(data['mm_check_subject']):
    if '演示' in row['name']:name('mm_check_subject','name',row['id'],subjects[i%10])
qc=['外观色差与表面质量','按键触发与回弹','滚轮步进与中键响应','USB连接稳定性','包装配件完整性']
for row in data['mm_qc_item']:
    if '演示' in row['name']:name('mm_qc_item','name',row['id'],qc[(row['id']-6)%5])
for row in data['t_quality_standard']:
    if '演示' in row['standard_name']:row['standard_name']=qc[(row['id']-1)%5]+f"检验规范 V{(row['id']-1)//5+1}.0"
for row in data.get('mm_notice',[]):
    title=re.sub(r'^(演示公告\d+：|隔离公告\d+)','',row['title'])
    row['title']=title or 'A100产线生产与质量协同通知'
    if row.get('content') and any(s in row['content'] for s in ['演示','真实持久化']):row['content']='请生产班组按已确认排程领料开工；设备维护员完成开机点检，品质人员执行首件检验，仓储人员核对物料批次与库位。异常情况请登记并通知班组长处理。'
for row in data.get('mm_menu',[]):
    if row['name']=='运营中心验收':row['name']='MES运营中心'
for row in data.get('t_order',[]):
    if row.get('customer_name')=='隔离客户':row['customer_name']='苏州恒悦办公设备有限公司'
    if row.get('customer_name')=='隔离多工单客户':row['customer_name']='杭州星辰数码有限公司'
for row in data.get('t_dict',[]):
    if '演示业务字典' in row['dict_name']:row['dict_name']=['包装方式','运输方式','交付方式','付款方式','产品颜色','表面工艺','鼠标接口','质检等级','仓储分区','售后类型'][(row['id']-1)%10]
for row in data.get('t_dict_item',[]):
    if '演示选项' in row['item_label']:row['item_label']=['彩盒包装','公路运输','仓库交货','月结30天','曜石黑','细纹磨砂','USB-A','常规抽检','常温存储','返厂维修'][(row['id']-1)%10]

# Update denormalized display names using the owning foreign key where possible.
for table, table_rows in data.items():
    for row in table_rows:
        for column, source, fk, label in [('product_name','t_product','product_id','product_name'),('material_name','t_material','material_id','material_name'),('equipment_name','t_equipment','equipment_id','equipment_name')]:
            if column in row and fk in row and table!=source:
                master=next((r for r in data[source] if r['id']==row[fk]),None)
                if master:row[column]=master[label]
        for column,fk in [('user_name','user_id'),('assigned_user_name','assigned_user_id'),('checker_name','checker_id'),('responsible_name','responsible_id'),('maintenance_user_name','maintenance_user_id'),('manager_name','manager_id')]:
            if column in row and row.get(fk):
                user=next((r for r in data['t_user'] if r['id']==row[fk]),None)
                if user:row[column]=user['real_name']

# Only human-readable descriptions are normalized; identifiers and enums stay stable.
descriptive={'name','material_name','product_name','equipment_name','warehouse_name','location_name','customer_name','supplier_name','uom_name','real_name','title','content','remark','reason','decision_note','approval_note','fault_desc','maintenance_content','actual_value','standard_name','standard_content','defect_desc','handle_result','address','location','specification','description','unit','result'}
replacements=sorted(rename.items(),key=lambda pair:len(pair[0]),reverse=True)
for table,table_rows in data.items():
    for row in table_rows:
        for column,value in list(row.items()):
            if column not in descriptive or not isinstance(value,str):continue
            if value.startswith(('{','[')):continue
            # Match whole archive labels, not common words inside a new model name.
            # Narrative descriptions may contain an old label within a sentence.
            if column in {'remark','reason','content','description','fault_desc','maintenance_content'}:
                pattern='|'.join(re.escape(old) for old,new in replacements)
                if pattern:value=re.sub(pattern,lambda match:rename[match.group()],value)
            elif table!='mm_category' and value==original[table][next(i for i,r in enumerate(original[table]) if r['id']==row['id'])].get(column):
                value=rename.get(value,value)
            value=value.replace('答辩演示独立抽检样单','独立抽检样单').replace('演示记录，非正式生产批次放行依据','独立抽检记录，非生产批次放行依据')
            value=value.replace('答辩演示完整生产链','生产全过程记录').replace('答辩演示历史考勤','生产班组历史考勤').replace('答辩演示抽检记录','产线抽检记录')
            value=value.replace('隔离验收异常，生成报修','设备运行异常，已登记报修').replace('隔离验收维修完成','设备维修完成，功能复核正常')
            value=value.replace('隔离验收：','质量处置：').replace('隔离验收','批次质量核查').replace('验收驳回，不应扣料','报工信息不完整，驳回后补充；未扣减物料')
            value=re.sub(r'演示生产日历(\d+)',r'生产工作日安排\1',value)
            value=value.replace('隔离调休上班','调休生产工作日').replace('隔离两班倒','装配产线两班轮转')
            value=value.replace('演示保养：','设备保养：').replace('演示点检：','设备点检：').replace('（演示）','').replace('答辩演示','业务参考').replace('演示','').replace('隔离','')
            row[column]=value

# Populate usable category links for material and product archives.
for row in data['t_product']:
    n=row['product_name'];row['category_id']=10 if any(w in n for w in ['电子','主板']) else 11 if any(w in n for w in ['结构','壳','组件']) else 12 if '双模' in n else 6 if any(w in n for w in ['游戏','电竞']) else 4 if '无线' in n else 2
for row in data['t_material']:
    n=row['material_name']
    row['category_id']=12 if '双模' in n and '整机' in n else 4 if any(w in n for w in ['整机','成品']) and '无线' in n else 2 if any(w in n for w in ['整机','成品']) else 10 if any(w in n for w in ['主板组件','电子组件']) else 11 if '结构组件' in n else 9 if any(w in n for w in ['包装','彩盒','内托','袋','说明书','标签','铭牌','脚贴']) else 5 if any(w in n for w in ['盖','壳','滚轮组件','侧键','透镜','螺钉']) else 7

def literal(value):
    if value is None:return 'NULL'
    if isinstance(value,(int,float)):return str(value)
    return "CONVERT(0x"+str(value).encode('utf-8').hex()+" USING utf8mb4)" if value else "''"
updates=[];counts={}
for table,table_rows in data.items():
    before={r['id']:r for r in original[table]}
    for row in table_rows:
        changed={k:v for k,v in row.items() if v!=before[row['id']].get(k)}
        if not changed:continue
        assignments=','.join(f'`{k}`={literal(v)}' for k,v in changed.items())
        # Avoid automatic timestamp rewrites on presentation-only updates.
        for col in ['update_time','updated_at']:
            if col in row and col not in changed:assignments+=f',`{col}`=`{col}`'
        updates.append(f"UPDATE `{table}` SET {assignments} WHERE id={literal(row['id'])};")
        counts[table]=counts.get(table,0)+1
(out/'changes.sql').write_text('START TRANSACTION;\n'+'\n'.join(updates)+'\nCOMMIT;',encoding='utf-8')
sql((out/'changes.sql').read_text(encoding='utf-8'),DB)
for table in counts:
    after=rows(table);old={r['id']:r for r in original[table]}
    assert len(after)==len(old),table+' row count changed'
    for row in after:
        for col,dtype in schema[table].items():
            if dtype in ('decimal','int','bigint','tinyint','date','datetime','timestamp') and col not in ('category_id',):
                assert row[col]==old[row['id']][col],f'{table}.{col} changed unexpectedly'
remaining=[]
for table in counts:
    for row in rows(table):
        for col,value in row.items():
            if col in descriptive and isinstance(value,str) and any(w in value for w in ['演示','隔离','测试物料','测试鼠标']):remaining.append({'table':table,'id':row['id'],'column':col,'value':value})
report={'database':DB,'backup':str(out/'database.sql'),'changed_rows':counts,'remaining_placeholders':remaining,'preserved':'IDs, business codes, enum states, numbers, amounts, stock, dates and immutable history'}
(out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))

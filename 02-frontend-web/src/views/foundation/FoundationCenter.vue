<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import CategoryManager from '@/components/CategoryManager.vue'
import { Search, Plus, RefreshCw, ArrowUpRight } from 'lucide-vue-next'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { foundationGroups, foundationDestinations } from '@/utils/foundationNavigation'

interface Field { name: string; label: string; type: string; ref?: string; required: boolean; options?: Record<string,string> }
interface Definition { key: string; label: string; fields: Field[]; roles: number[] }
type Row = Record<string, any>
const auth = useAuthStore(), route = useRoute(), router = useRouter()
const catalog = ref<Definition[]>([]), records = ref<Row[]>([]), lookups = reactive<Record<string,Row[]>>({})
const loading = ref(false), saving = ref(false), message = ref(''), error = ref(''), search = ref('')
const modal = ref(false), editing = ref<number>(), form = reactive<Row>({}), expansion = ref<Row[]>([]), bomQuantity = ref(100)
const key = computed(()=>String(route.params.key || 'units'))
const currentGroup = computed(()=>foundationGroups.find(group=>group.keys.includes(key.value))||foundationGroups[0])
const isSystemSetup = computed(()=>currentGroup.value.module==='系统管理')
const isWarehouseSetup = computed(()=>currentGroup.value.module==='仓库管理')
const isProductionSetup=computed(()=>['产品与工艺','班组与排班配置'].includes(currentGroup.value.title))
const isEquipmentSetup = computed(()=>currentGroup.value.module==='设备管理')
const navigationGroups = computed(()=>[currentGroup.value])
const definition = computed(()=>catalog.value.find(x=>x.key===key.value))
const canEdit = computed(()=>auth.effectiveRoleId===9 || !!definition.value?.roles.includes(Number(auth.effectiveRoleId)))
const internalCode=(f:Field)=>!f.ref&&(f.name==='code'||f.name.endsWith('_code'))
const visibleFields=computed(()=>definition.value?.fields.filter(f=>!internalCode(f))||[])
const filtered = computed(()=>records.value.filter(r=>Object.values(r).join(' ').toLowerCase().includes(search.value.toLowerCase())))
const warehouseId=computed(()=>String(route.query.warehouse||''))
const areaId=computed(()=>String(route.query.area||''))
const storageAreas=computed(()=>(lookups.areas||[]).filter(r=>!warehouseId.value||Number(r.warehouse_id)===Number(warehouseId.value)))
function selectStorage(warehouse:string,area=''){router.replace({path:route.path,query:{...route.query,warehouse:warehouse||undefined,area:area||undefined}})}
function openStorage(next:string,row:Row){router.push({path:`/foundation/${next}`,query:next==='areas'?{warehouse:row.id}:{warehouse:row.warehouse_id||warehouseId.value||undefined,area:row.id}})}
const categoryId=ref('')
const categoryOptions=computed(()=>{
  const all=lookups.categories||[],out:Row[]=[]
  const walk=(parent:number,depth:number,seen:Set<number>)=>{for(const c of all.filter(r=>Number(r.parent_id||0)===parent)){if(seen.has(c.id))continue;out.push({...c,treeLabel:'　'.repeat(depth)+c.name});walk(c.id,depth+1,new Set([...seen,c.id]))}}
  walk(0,0,new Set());return out
})
const visibleRecords=computed(()=>{
  if(isWarehouseSetup.value)return filtered.value.filter(r=>{
    if(key.value==='warehouses')return !warehouseId.value||Number(r.id)===Number(warehouseId.value)
    const area=key.value==='locations'?(lookups.areas||[]).find(a=>Number(a.id)===Number(r.area_id)):r
    return (!warehouseId.value||Number(area?.warehouse_id)===Number(warehouseId.value))&&(!areaId.value||key.value!=='locations'||Number(r.area_id)===Number(areaId.value))
  })
  if(!categoryId.value)return filtered.value
  const ids=new Set<number>([Number(categoryId.value)]);let changed=true
  while(changed){changed=false;for(const c of lookups.categories||[])if(ids.has(Number(c.parent_id))&&!ids.has(Number(c.id))){ids.add(Number(c.id));changed=true}}
  return filtered.value.filter(r=>ids.has(Number(r.category_id)))
})
const productPage=ref(1),productPageSize=ref(5)
const productPageCount=computed(()=>Math.max(1,Math.ceil(visibleRecords.value.length/productPageSize.value)))
const pagedProducts=computed(()=>visibleRecords.value.slice((productPage.value-1)*productPageSize.value,productPage.value*productPageSize.value))
watch([search,categoryId,productPageSize,key],()=>{productPage.value=1})
watch(productPageCount,count=>{productPage.value=Math.min(productPage.value,count)})
const maintenancePeople=ref<Row[]>([])
const storagePage = ref(1)
const storagePageSize = ref(10)
const storagePageCount = computed(()=>Math.max(1,Math.ceil(visibleRecords.value.length/storagePageSize.value)))
const tableRecords = computed(()=>isWarehouseSetup.value?visibleRecords.value.slice((storagePage.value-1)*storagePageSize.value,storagePage.value*storagePageSize.value):isProductionSetup.value?pagedProducts.value:visibleRecords.value)
watch([key,search,warehouseId,areaId,storagePageSize],()=>{storagePage.value=1})
watch(storagePageCount,count=>{storagePage.value=Math.min(storagePage.value,count)})

function categoryDescendants(id:number) {
  const ids=new Set<number>([id]);let changed=true
  while(changed){changed=false;for(const row of records.value)if(ids.has(Number(row.parent_id))&&!ids.has(Number(row.id))){ids.add(Number(row.id));changed=true}}
  return ids
}
function fieldChoices(f:Field):Row[]{
  if(key.value==='categories'&&f.name==='parent_id'){
    const excluded=editing.value?categoryDescendants(Number(editing.value)):new Set<number>()
    return categoryOptions.value.filter(row=>!excluded.has(Number(row.id))&&(!form.kind||row.kind===form.kind)).map(row=>({...row,name:row.treeLabel}))
  }
  if(isWarehouseSetup.value&&f.ref==='areas'&&warehouseId.value)return storageAreas.value
  return f.ref==='users'&&['maintenance-plans','checks'].includes(key.value)?maintenancePeople.value:lookups[f.ref||'']||[]
}
function addChildCategory(row:Row){edit();form.parent_id=row.id;form.kind=row.kind}
const rowName = (r:Row) => String(r.name || r.material_name || r.product_name || r.customer_name || r.supplier_name || r.warehouse_name || r.equipment_name || r.uom_name || r.code || r.id)
function display(f:Field, value:any) {
  if(value===null || value===undefined || value==='') return '—'
  if(f.options) return f.options[String(value)] || value
  if(f.ref) return rowName((lookups[f.ref] || []).find(x=>Number(x.id)===Number(value)) || {name:`#${value}`})
  return String(value)
}
async function load() {
  if(foundationDestinations[key.value]){await router.replace(foundationDestinations[key.value]);return}
  loading.value=true; error.value=''; expansion.value=[]
  try {
    if(!catalog.value.length) catalog.value=await request.get<Definition[]>('/foundation/catalog')
    if(!definition.value) throw new Error('配置类型不存在')
    records.value=await request.get<Row[]>(`/foundation/${key.value}`)
    if(['maintenance-plans','checks'].includes(key.value))maintenancePeople.value=await request.get<Row[]>('/mes/assignable-people',{params:{role:8}})
    await Promise.all([...new Set(definition.value.fields.map(f=>f.ref).filter(Boolean))].map(async r=>{lookups[r!]=await request.get<Row[]>(`/foundation/${r}`)}))
      if(isWarehouseSetup.value)await Promise.all(['warehouses','areas'].map(async r=>{lookups[r]=await request.get<Row[]>(`/foundation/${r}`)}))
  } catch(e:any) { error.value=e.message || '加载失败'; records.value=[] }
  finally {loading.value=false}
}
function edit(row?:Row) {
  editing.value=row?.id; Object.keys(form).forEach(k=>delete form[k]); error.value=''
  definition.value?.fields.forEach(f=>{
    let value=row?.[f.name] ?? ''
    if(!row&&value===''){if(key.value==='maintenance-plans'&&['frequency','remind_days'].includes(f.name))value=1;if(f.name==='conversion_rate')value=1;if(f.name==='data_type')value='TEXT';if(f.name==='phase')value='FINAL';if(f.name==='is_key')value='0';if(f.name==='enabled'||key.value==='locations'&&f.name==='status')value='1';if(f.name==='plan_type')value='CHECK'}
    if(f.type==='datetime-local' && typeof value==='string')value=value.replace(' ','T').slice(0,16)
    if(key.value==='maintenance-plans'&&['frequency','remind_days'].includes(f.name))value=Math.max(1,Number(value)||1)
    form[f.name]=value
  }); if(!row&&isWarehouseSetup.value){if(key.value==='areas'&&warehouseId.value)form.warehouse_id=Number(warehouseId.value);if(key.value==='locations'&&areaId.value)form.area_id=Number(areaId.value)}; modal.value=true
}
async function save() {
  saving.value=true;error.value=''
  try {
    if(key.value==='categories'){
      const parent=records.value.find(row=>Number(row.id)===Number(form.parent_id))
      if(editing.value&&categoryDescendants(Number(editing.value)).has(Number(form.parent_id)))throw new Error('父分类不能选择自身或下级分类')
      if(parent&&parent.kind!==form.kind)throw new Error('子分类与父分类的类型必须一致，请重新选择父分类')
      if(editing.value&&records.value.some(row=>Number(row.parent_id)===Number(editing.value)&&row.kind!==form.kind))throw new Error('该分类有其他类型的子分类，请先调整子分类再修改类型')
    }
    const data=Object.fromEntries(Object.entries(form).map(([k,v])=>[k,v===''?null:v]))
    if(editing.value) await request.put(`/foundation/${key.value}/${editing.value}`,data)
    else await request.post(`/foundation/${key.value}`,data)
    if(key.value==='role-menus'||key.value==='menus')await auth.refreshMenuAccess()
    completeDrafts();modal.value=false;message.value='保存成功';await load()
  } catch(e:any) {error.value=e.message || '保存失败'} finally{saving.value=false}
}
async function remove(row:Row) {
  if(!window.confirm(`确认删除“${rowName(row)}”？已被引用的记录会被阻止删除。`))return
  try {await request.delete(`/foundation/${key.value}/${row.id}`);message.value='已删除';await load()}catch(e:any){error.value=e.message}
}
async function expand(row:Row) {
  try{ expansion.value=await request.get<Row[]>(`/foundation/bom/${row.id}/expand`,{params:{quantity:bomQuantity.value}});error.value='' }catch(e:any){error.value=e.message}
}
async function uploadSop(event:Event){
  const file=(event.target as HTMLInputElement).files?.[0];if(!file)return
  const body=new FormData();body.append('file',file)
  try{form.image_path=await request.post<string>('/mes/sop-images',body)}catch(e:any){error.value=e.message}
}

watch(key,()=>{modal.value=false;search.value='';message.value='';load()})
onMounted(load)

const completeDrafts=useFormDrafts({modal:{open:modal,fields:{form}, record:()=>(form as Record<string,any>).id || editing.value}})
</script>

<template>
  <MainLayout :full-width="isProductionSetup" :edge-to-edge="isProductionSetup">
    <div :class="{'equipment-configuration':isEquipmentSetup,'system-configuration':isSystemSetup,'product-configuration':isProductionSetup}">
    <div v-if="currentGroup.module === '质量管理'" class="mb-4">
      <button type="button" class="btn-secondary" @click="router.push('/quality/tasks')">← 返回质检任务</button>
    </div>
    <div v-if="!isEquipmentSetup && !isSystemSetup" class="mb-6 flex items-center justify-between gap-4">
      <div><p class="text-sm text-blue-600 mb-1">{{ currentGroup.module }} / {{ currentGroup.title }}</p><h1 class="text-2xl font-bold text-gray-900">{{ definition?.label || currentGroup.title }}</h1><p class="text-sm text-gray-500 mt-2">{{ isWarehouseSetup?'按仓库、库区、库位三级维护存储位置，供入库、出库和库存查询使用。':key==='categories'?'建立清晰的分类层级，让原材料、半成品和成品各有归属。':`维护${currentGroup.title}，供${currentGroup.module}相关业务使用。` }}</p></div>
      <button v-if="currentGroup.module!=='质量管理'&&currentGroup.returnPath&&auth.hasMenu(currentGroup.returnPath)" class="rounded-lg border px-4 py-2 bg-white" @click="router.push(currentGroup.returnPath)">{{ currentGroup.returnLabel }} →</button>
    </div>
    <div :class="isWarehouseSetup || isEquipmentSetup || isSystemSetup ? 'space-y-5' : 'grid grid-cols-[200px_minmax(0,1fr)] gap-5'">
      <aside v-if="!isWarehouseSetup && !isSystemSetup" class="foundation-menu bg-white border rounded-xl p-3 self-start" :aria-label="currentGroup.title" tabindex="0">
        <div v-for="group in navigationGroups" :key="group.title" class="mb-4"><p class="px-2 text-xs text-gray-400 mb-1">{{ group.title }}</p>
          <button v-for="item in group.keys.map(k=>catalog.find(x=>x.key===k)).filter((x): x is Definition=>!!x&&auth.hasMenu(`/foundation/${x.key}`))" :key="item.key" class="block w-full text-left px-3 py-2 rounded-lg text-sm" :class="key===item.key?'bg-blue-50 text-blue-700 font-medium':'text-gray-600 hover:bg-gray-50'" :aria-current="key===item.key?'page':undefined" @click="router.push(`/foundation/${item.key}`)">{{ item.label }}</button>
        </div>
      </aside>
      <section class="min-w-0">
        <p v-if="message" role="status" class="mb-3 rounded-lg bg-green-50 p-3 text-green-700">{{ message }}</p>
        <p v-if="error" role="alert" class="mb-3 rounded-lg bg-red-50 p-3 text-red-700">{{ error }}</p>
        <div v-if="isSystemSetup && key!=='categories'" class="system-list-heading"><div><h2>{{ definition?.label || '基础资料' }}<span>{{ records.length }} 项</span></h2><p>{{ key==='units'?'维护计量单位与换算关系，统一采购、生产和库存的数量口径。':key==='categories'?'按层级整理物料和产品分类，便于查找与统一维护。':'集中维护'+(definition?.label || '配置资料')+'，供相关业务使用。' }}</p></div></div>
        <div v-if="isProductionSetup" class="product-summary"><div><span>{{definition?.label||'档案总数'}}</span><strong>{{records.length}}<small>条记录</small></strong></div><div><span>{{key==='products'?'已归类':'当前页'}}</span><strong>{{key==='products'?records.filter(r=>r.category_id).length:productPage}}<small>{{key==='products'?'款':'/ '+productPageCount+' 页'}}</small></strong></div><div><span>当前结果</span><strong>{{visibleRecords.length}}<small>条记录</small></strong></div></div>
        <CategoryManager v-if="key==='categories'" :records="records" :loading="loading" :can-edit="canEdit" @edit="edit" @child="addChildCategory" @remove="remove" @refresh="load" />
        <div v-else class="foundation-records bg-white border rounded-xl overflow-hidden">
          <div v-if="isEquipmentSetup" class="equipment-list-heading"><div><h2>{{definition?.label||'点检配置'}}<span>{{visibleRecords.length}} 项</span></h2><p>维护设备检查标准，让日常点检有据可依。</p></div><button v-if="auth.hasMenu('/equipment/list')" @click="router.push('/equipment/list')">设备台账<ArrowUpRight :size="14" /></button></div>
          <div class="foundation-toolbar p-4 flex flex-wrap gap-3 items-center border-b">
            <select v-if="['materials','products'].includes(key)" v-model="categoryId" class="border rounded-lg px-3 py-2" aria-label="按分类及子分类筛选"><option value="">全部分类</option><option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{c.treeLabel}}</option></select>
            <template v-if="isWarehouseSetup"><select :value="warehouseId" @change="selectStorage(($event.target as HTMLSelectElement).value)" class="border rounded-lg px-3 py-2" aria-label="筛选仓库"><option value="">全部仓库</option><option v-for="w in lookups.warehouses||[]" :key="w.id" :value="w.id">{{rowName(w)}}</option></select><select v-if="key==='locations'" :value="areaId" @change="selectStorage(warehouseId,($event.target as HTMLSelectElement).value)" class="border rounded-lg px-3 py-2" aria-label="筛选库区"><option value="">全部库区</option><option v-for="a in storageAreas" :key="a.id" :value="a.id">{{rowName(a)}}</option></select></template>
            <label class="foundation-search flex-1 min-w-32"><Search v-if="isEquipmentSetup || isSystemSetup || isProductionSetup" :size="16" aria-hidden="true"/><input v-model="search" aria-label="搜索记录" :placeholder="isEquipmentSetup?'搜索名称、类型或执行标准…':isSystemSetup?'搜索'+(definition?.label || '记录')+'…':key==='products'?'搜索产品名称、型号或规格…':isProductionSetup?'搜索'+(definition?.label||'资料')+'…':'搜索当前列表'" class="border rounded-lg px-3 py-2 w-full" /></label>
            <button class="foundation-refresh border rounded-lg px-4 py-2" :disabled="loading" @click="load"><RefreshCw v-if="isEquipmentSetup || isSystemSetup || isProductionSetup" :size="15" :class="{'animate-spin':loading}"/>刷新</button>
            <button v-if="canEdit&&!['code-rules','parameters','jobs','menus'].includes(key)" class="foundation-add bg-blue-600 text-white rounded-lg px-4 py-2" @click="edit()"><Plus v-if="isEquipmentSetup || isSystemSetup || isProductionSetup" :size="16"/>新增{{ definition?.label }}</button>
            <span v-else class="text-sm text-gray-500">当前岗位只读</span>
          </div>
          <div v-if="key==='bom-versions'" class="px-4 py-3 bg-blue-50 text-sm">展开生产数量：<input v-model.number="bomQuantity" type="number" min="1" class="border rounded px-2 py-1 w-24" aria-label="BOM展开数量" />，点击对应BOM的“展开用料需求”。</div>
          <div v-if="key==='products'" class="product-table-wrap"><table class="product-table"><thead><tr><th>产品信息</th><th>规格与单位</th><th>产品分类</th><th class="product-price">成本价</th><th class="product-price">销售价</th><th>操作</th></tr></thead><tbody><tr v-for="row in pagedProducts" :key="row.id"><td><div class="product-name-cell"><span class="product-mark" aria-hidden="true">{{String(row.product_name||row.name||'P').slice(0,1)}}</span><div><strong>{{row.product_name||row.name||'未命名产品'}}</strong><small>{{row.product_code||row.code||'—'}}<span v-if="row.model"> · {{row.model}}</span></small></div></div></td><td><p>{{row.specification||row.spec||'未填写规格'}}</p><small>{{lookups.units?.find(u=>Number(u.id)===Number(row.unit_id))?.name||row.unit||'—'}}</small></td><td><span class="product-category">{{lookups.categories?.find(c=>Number(c.id)===Number(row.category_id))?.name||'未分类'}}</span></td><td class="product-price">¥ {{Number(row.cost_price||0).toFixed(2)}}</td><td class="product-price product-sale">¥ {{Number(row.selling_price??row.sale_price??0).toFixed(2)}}</td><td><div v-if="canEdit" class="product-actions"><button @click="edit(row)">编辑</button><button class="delete" @click="remove(row)">删除</button></div><span v-else class="text-gray-400">只读</span></td></tr><tr v-if="!visibleRecords.length"><td colspan="6" class="product-empty">{{loading?'正在加载产品…':'暂无匹配产品，请调整筛选条件或新增产品。'}}</td></tr></tbody></table><nav class="product-list-footer product-pagination" aria-label="产品列表分页"><span aria-live="polite">共 {{visibleRecords.length}} 款产品<span v-if="visibleRecords.length"> · 当前 {{(productPage-1)*productPageSize+1}}–{{Math.min(productPage*productPageSize,visibleRecords.length)}} 条</span></span><div><label>每页 <select v-model.number="productPageSize" aria-label="产品每页条数"><option :value="5">5 条</option><option :value="10">10 条</option><option :value="20">20 条</option></select></label><button class="btn-secondary" :disabled="loading||productPage===1" @click="productPage--">上一页</button><span>{{productPage}} / {{productPageCount}}</span><button class="btn-secondary" :disabled="loading||productPage>=productPageCount" @click="productPage++">下一页</button></div></nav></div>
          <div v-else class="overflow-x-auto foundation-table-scroll"><table class="w-full text-sm text-left"><thead class="bg-gray-50 text-gray-600"><tr><th v-for="f in visibleFields" :key="f.name" class="px-4 py-3 whitespace-nowrap">{{f.label}}</th><th class="px-4 py-3">操作</th></tr></thead>
            <tbody class="divide-y"><tr v-for="row in tableRecords" :key="row.id"><td v-for="f in visibleFields" :key="f.name" class="px-4 py-3 max-w-64 break-words"><div v-if="isProductionSetup&&f.name===visibleFields[0]?.name" class="product-name-cell"><span class="product-mark" aria-hidden="true">{{String(display(f,row[f.name])).slice(0,1)}}</span><div><strong>{{display(f,row[f.name])}}</strong><small v-if="row.code||row.product_code">{{row.code||row.product_code}}</small></div></div><span v-else>{{display(f,row[f.name])}}</span></td><td class="px-4 py-3 whitespace-nowrap"><button v-if="key==='warehouses'&&auth.hasMenu('/foundation/areas')" class="text-blue-600 mr-3" @click="openStorage('areas',row)">查看库区</button><button v-if="key==='areas'&&auth.hasMenu('/foundation/locations')" class="text-blue-600 mr-3" @click="openStorage('locations',row)">查看库位</button><button v-if="key==='bom-versions'" class="text-blue-600 mr-3" @click="expand(row)">展开用料需求</button><template v-if="canEdit && key!=='check-records'"><button class="text-blue-600 mr-3" @click="edit(row)">编辑</button><button class="text-red-600" @click="remove(row)">删除</button></template><span v-else>—</span></td></tr>
            <tr v-if="!visibleRecords.length"><td :colspan="(definition?.fields.length || 0)+2" class="p-12 text-center text-gray-400">{{ loading?'正在加载…':'暂无记录，可按业务需要新增。' }}</td></tr></tbody>
          </table></div>
          <nav v-if="isProductionSetup&&key!=='products'" class="product-list-footer product-pagination" aria-label="工艺资料列表分页"><span aria-live="polite">共 {{visibleRecords.length}} 条记录<span v-if="visibleRecords.length"> · 当前 {{(productPage-1)*productPageSize+1}}–{{Math.min(productPage*productPageSize,visibleRecords.length)}} 条</span></span><div><label>每页 <select v-model.number="productPageSize" aria-label="资料每页条数"><option :value="5">5 条</option><option :value="10">10 条</option><option :value="20">20 条</option></select></label><button class="btn-secondary" :disabled="loading||productPage===1" @click="productPage--">上一页</button><span>{{productPage}} / {{productPageCount}}</span><button class="btn-secondary" :disabled="loading||productPage>=productPageCount" @click="productPage++">下一页</button></div></nav>
          <nav v-if="isWarehouseSetup" class="flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 p-4 text-sm text-gray-500" aria-label="仓储基础分页">
            <span aria-live="polite">共 {{visibleRecords.length}} 条<span v-if="visibleRecords.length">，当前 {{(storagePage-1)*storagePageSize+1}}–{{Math.min(storagePage*storagePageSize,visibleRecords.length)}} 条</span></span>
            <div class="flex flex-wrap items-center gap-3">
              <label class="flex items-center gap-2">每页<select v-model.number="storagePageSize" :disabled="loading" class="rounded-lg border px-2 py-2" aria-label="仓储基础每页条数"><option :value="10">10 条</option><option :value="20">20 条</option><option :value="50">50 条</option></select></label>
              <button type="button" class="btn-secondary" :disabled="loading||storagePage===1" @click="storagePage--">上一页</button>
              <span>{{storagePage}} / {{storagePageCount}} 页</span>
              <button type="button" class="btn-secondary" :disabled="loading||storagePage>=storagePageCount" @click="storagePage++">下一页</button>
            </div>
          </nav>
        </div>
        <div v-if="expansion.length" class="mt-5 bg-white border rounded-xl p-5"><h2 class="font-semibold mb-3">用料展开结果 · {{bomQuantity}}只</h2><p class="text-sm text-gray-500 mb-4">共{{expansion.length}}种叶子物料；已合并跨半成品的同一物料，根据供应方式保留外购半成品；自制半成品继续向下展开。</p><table class="w-full text-sm"><thead><tr class="text-left border-b"><th class="p-2">物料编码</th><th>名称</th><th>需求量</th><th>单位</th></tr></thead><tbody><tr v-for="row in expansion" :key="row.id" class="border-b"><td class="p-2">{{row.material_code}}</td><td>{{row.material_name}}</td><td>{{row.required_quantity}}</td><td>{{row.unit}}</td></tr></tbody></table></div>
      </section>
    </div>
    <div v-if="modal" class="modal-mask fixed inset-0 bg-black/40 z-[80] flex items-center justify-center p-6" data-business-form @click.self="modal=false">
      <form class="modal-card bg-white rounded-xl shadow-xl p-6 w-full max-w-xl max-h-[85vh] overflow-y-auto" @submit.prevent="save"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
        <h2 class="font-bold text-xl mb-5">{{ key==='categories'?(editing?'编辑分类':form.parent_id?'新增子分类':'新增一级分类'):`${editing?'编辑':'新增'}${definition?.label}` }}</h2>
        <p v-if="key==='categories'" class="text-sm text-gray-500 mb-4">父分类留空即为一级分类；子分类与父分类使用相同类型。</p>
        <p v-if="error" role="alert" class="bg-red-50 text-red-700 p-3 rounded mb-4">{{error}}</p>
        <div v-for="f in visibleFields" :key="f.name" class="mb-4"><label :for="`field-${f.name}`" class="block text-sm font-medium text-gray-700 mb-1">{{f.label}}<span v-if="f.required" class="text-red-500"> *</span></label>
          <select v-if="f.ref" :id="`field-${f.name}`" v-model="form[f.name]" :required="f.required" class="w-full rounded-lg border px-3 py-2"><option value="">请选择</option><option v-for="r in fieldChoices(f)" :key="r.id" :value="r.id">{{rowName(r)}}</option></select>
          <select v-else-if="f.options" :id="`field-${f.name}`" v-model="form[f.name]" :required="f.required" class="w-full rounded-lg border px-3 py-2"><option value="">请选择</option><option v-for="(label,value) in f.options" :key="value" :value="value">{{label}}</option></select>
          <template v-else-if="f.type==='image'"><input type="file" accept="image/png,image/jpeg" @change="uploadSop" /><p class="text-xs text-gray-500 mt-1">{{form[f.name]||'上传图片后自动保存地址'}}</p></template><input v-else :id="`field-${f.name}`" v-model="form[f.name]" :required="f.required" :type="['number','positive','decimal'].includes(f.type)?'number':f.type" :min="key==='maintenance-plans'&&['frequency','remind_days'].includes(f.name)?1:f.type==='positive'?0.0001:f.type==='number'?0:undefined" :step="key==='maintenance-plans'&&['frequency','remind_days'].includes(f.name)?'1':['number','positive','decimal'].includes(f.type)?'0.0001':undefined" maxlength="500" class="w-full rounded-lg border px-3 py-2" />
        </div>
        <p v-if="['maintenance-plans','checks'].includes(key)&&!maintenancePeople.length" class="text-amber-700 text-sm">暂无可负责此任务的在职设备维护员，请先配置员工岗位。</p>
        <p class="text-sm text-gray-500 mt-4">内部编号由系统自动生成，关联资料按名称选择。</p><div class="flex justify-end gap-3 mt-6"><button type="button" class="border rounded-lg px-4 py-2" @click="modal=false">取消</button><button :disabled="saving" class="bg-blue-600 text-white rounded-lg px-5 py-2">{{saving?'保存中…':'保存'}}</button></div>
      </form>
    </div>
    </div>
  </MainLayout>
</template>

<style scoped>
.equipment-configuration{--equipment-line:#e9e5dd;--equipment-ink:#514a40}
.equipment-configuration .foundation-menu{position:static;max-height:none;overflow:visible;padding:0;border:0;border-radius:0;background:transparent}
.equipment-configuration .foundation-menu>div{display:flex;gap:6px;flex-wrap:wrap;margin:0;padding-bottom:14px;border-bottom:1px solid var(--equipment-line)}
.equipment-configuration .foundation-menu p{display:none}
.equipment-configuration .foundation-menu button{width:auto;padding:9px 16px;font-size:13px;color:#8b8172;border:1px solid transparent;background:transparent;transition:background .15s,color .15s}
.equipment-configuration .foundation-menu button:hover{background:#f2eee6;color:#65543d}
.equipment-configuration .foundation-menu button[aria-current="page"]{background:#eee6d8;border-color:#e4d8c3;color:#755b35;font-weight:600}
.equipment-configuration .foundation-records{border-color:var(--equipment-line);border-radius:12px;box-shadow:0 3px 16px #514a4004}
.equipment-list-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:22px 24px 18px}
.equipment-list-heading h2{display:flex;align-items:center;gap:10px;font-size:17px;font-weight:600;color:var(--equipment-ink)}
.equipment-list-heading h2 span{font-size:11px;font-weight:400;color:#8b8172;background:#f3f0e9;padding:3px 8px;border-radius:5px}
.equipment-list-heading p{font-size:12px;color:#a09789;margin-top:6px}
.equipment-list-heading>button{display:flex;align-items:center;gap:5px;color:#8b795f;font-size:12px;white-space:nowrap}
.equipment-configuration .foundation-toolbar{padding:0 24px 18px;gap:10px;border-color:var(--equipment-line)}
.equipment-configuration .foundation-search{position:relative;max-width:420px;margin-right:auto}
.equipment-configuration .foundation-search svg{position:absolute;left:12px;top:12px;color:#a69b8b}
.equipment-configuration .foundation-search input{padding:9px 12px 9px 36px;height:40px;font-size:12px;background:#fbfaf7;border-color:var(--equipment-line)}
.equipment-configuration .foundation-search input:focus{outline:2px solid #ded1ba;outline-offset:1px}
.equipment-configuration .foundation-refresh,.equipment-configuration .foundation-add{display:inline-flex;align-items:center;justify-content:center;gap:7px;height:40px;font-size:12px;white-space:nowrap}
.equipment-configuration .foundation-add{background:#7d6749;border-color:#7d6749}
.equipment-configuration .foundation-add:hover{background:#695538}
.equipment-configuration table{font-size:13px}
.equipment-configuration thead{background:#f8f6f1;color:#9b9182}
.equipment-configuration th{font-size:12px;font-weight:500;padding:13px 24px}
.equipment-configuration td{padding:16px 24px;color:#756c5f;border-color:#f0ece5}
.equipment-configuration td:first-child{font-weight:500;color:var(--equipment-ink)}
.equipment-configuration th:last-child,.equipment-configuration td:last-child{width:140px;text-align:right}
.equipment-configuration tbody tr:hover{background:#fcfaf6}
.equipment-configuration td button{font-size:12px;border-radius:4px;padding:4px 6px}
.equipment-configuration td button:hover{background:#f3eee5}
.equipment-configuration td button.text-red-600{color:#b57970}
@media(max-width:700px){.equipment-list-heading{padding:18px 16px}.equipment-configuration .foundation-toolbar{padding:0 16px 16px}.equipment-configuration .foundation-search{max-width:none;flex-basis:100%}.equipment-configuration th,.equipment-configuration td{padding:12px 16px}.equipment-configuration .foundation-menu button{padding:8px 10px}}
.foundation-menu {
  position: sticky;
  top: 96px;
  max-height: calc(100dvh - 260px);
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior-y: contain;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: #c5c2ba transparent;
}

.foundation-menu::-webkit-scrollbar {
  width: 6px;
}

.foundation-menu::-webkit-scrollbar-thumb {
  background: #c5c2ba;
  border-radius: 3px;
}

.foundation-menu::-webkit-scrollbar-thumb:hover {
  background: #a6a299;
}

.system-list-heading{padding:2px 0 18px}
.system-list-heading h2{display:flex;align-items:center;gap:10px;font-size:17px;font-weight:600;color:#4b4236}
.system-list-heading h2 span{font-size:11px;font-weight:400;padding:3px 9px;border-radius:20px;background:#eee9e0;color:#8a7b67}
.system-list-heading p{margin-top:7px;font-size:12px;color:#9b9285;line-height:1.6}
.system-configuration .foundation-records{border-radius:10px;border-color:#e8e3da}
.system-configuration .foundation-toolbar{padding:16px 20px;gap:10px;border-color:#eee9e0}
.system-configuration .foundation-search{position:relative;max-width:360px;margin-right:auto}
.system-configuration .foundation-search svg{position:absolute;left:12px;top:11px;color:#a49886}
.system-configuration .foundation-search input{height:38px;padding:8px 12px 8px 36px;font-size:12px}
.system-configuration .foundation-refresh,.system-configuration .foundation-add{display:inline-flex;align-items:center;justify-content:center;gap:7px;height:38px;font-size:12px;white-space:nowrap}
.system-configuration .foundation-table-scroll{max-height:560px;overflow:auto;scrollbar-width:thin;scrollbar-color:#d8d0c3 transparent}
.system-configuration table{min-width:600px}
.system-configuration table th{position:sticky;top:0;z-index:1;padding:12px 20px;background:#f7f5f0;color:#8c8070;font-size:12px;letter-spacing:0}
.system-configuration table td{padding:11px 20px;font-size:13px;line-height:22px;border-color:#f0ece5}
.system-configuration td:first-child{font-weight:500;color:#534737}
.system-configuration th:last-child,.system-configuration td:last-child{width:120px;text-align:right;white-space:nowrap}
.system-configuration td button{padding:3px 7px;margin-right:2px;border-radius:4px;font-size:12px}
.system-configuration td button:hover{background:#f1ede6}
@media(max-width:700px){.system-configuration .foundation-toolbar{padding:14px}.system-configuration .foundation-search{max-width:none;flex-basis:100%}.system-configuration table td,.system-configuration table th{padding-left:14px;padding-right:14px}}
.product-configuration .foundation-menu{align-self:stretch;position:static;max-height:none;overflow:visible;scrollbar-gutter:auto}
@media(max-width:850px){.product-configuration .foundation-menu{align-self:start}}
.product-pagination{display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap}.product-pagination>div{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.product-pagination label{display:flex;align-items:center;gap:6px}.product-pagination select{padding:5px 8px;min-height:30px!important;font-size:11px!important}.product-pagination .btn-secondary{padding:6px 10px;font-size:11px}.product-pagination button:disabled{opacity:.4;cursor:not-allowed}
.product-configuration .foundation-table-scroll table{min-width:650px}.product-configuration .foundation-table-scroll td{vertical-align:middle;padding:16px!important}.product-configuration .foundation-table-scroll th{background:#f7f3eb;color:#9b8a71}.product-configuration .foundation-table-scroll tbody tr:hover{background:#fcfaf5}.product-configuration .foundation-records{border-color:#e8dfd0}.product-configuration .foundation-table-scroll .product-name-cell small{display:block;color:#a79a86;font-size:11px;margin-top:5px}
</style>

<style scoped>
.product-summary{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:16px}.product-summary>div{background:#f3eee4;border:1px solid #e8dfd0;border-radius:10px;padding:14px 18px}.product-summary span{font-size:11px;color:#a18d70}.product-summary strong{display:block;font-size:25px;color:#7b6040;margin-top:5px;font-weight:600}.product-summary small{font-size:11px;font-weight:400;margin-left:8px;color:#a18d70}.product-table-wrap{overflow:auto}.product-table{width:100%;min-width:760px}.product-table td{padding:16px!important;vertical-align:middle}.product-name-cell{display:flex;align-items:center;gap:12px}.product-mark{display:grid;place-items:center;width:38px;height:38px;flex-shrink:0;background:#f1e9da;color:#a58960;border:1px solid #e7dbc5;border-radius:10px;font-size:16px}.product-name-cell strong{display:block;font-size:13px;font-weight:600;color:#665239}.product-table small{display:block;color:#a79a86;font-size:11px;margin-top:5px}.product-table td>p{font-size:12px;color:#807461;max-width:240px;line-height:1.7}.product-category{display:inline-block;font-size:11px;padding:4px 9px;border:1px solid #e6dfd2;border-radius:5px;background:#f8f5ee;color:#9a8362}.product-table .product-price{text-align:right;white-space:nowrap;font-variant-numeric:tabular-nums}.product-table .product-sale{color:#8c6b41;font-weight:600}.product-actions{display:flex;gap:12px;font-size:12px;color:#93754e}.product-actions .delete{color:#b78072}.product-actions button:hover{text-decoration:underline}.product-list-footer{border-top:1px solid #eee7dc;padding:12px 16px;font-size:11px;color:#a79a86;background:#fcfaf5}.product-empty{text-align:center;padding:40px!important;color:#a79a86}
.product-configuration .foundation-menu{padding:12px;background:#f7f4ed;border-color:#e7dfd2}.product-configuration .foundation-menu button{font-size:12px;padding:10px 12px;margin:2px 0}.product-configuration .foundation-menu button[aria-current=page]{background:#eae0cd;color:#80613a}.product-configuration .foundation-toolbar{gap:10px;padding:16px}.product-configuration .foundation-search{position:relative}.product-configuration .foundation-search svg{position:absolute;left:12px;top:50%;transform:translateY(-50%);color:#b3a58e}.product-configuration .foundation-search input{padding-left:36px}.product-configuration .foundation-refresh,.product-configuration .foundation-add{display:inline-flex;align-items:center;gap:7px}
@media(max-width:850px){.product-configuration>div.grid{grid-template-columns:1fr}.product-configuration .foundation-menu>div{display:flex;flex-wrap:wrap;gap:4px;margin-bottom:0}.product-configuration .foundation-menu p{width:100%}.product-configuration .foundation-menu button{width:auto}.product-summary>div{padding:12px}.product-summary strong{font-size:22px}}
.product-pagination{display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap}.product-pagination>div{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.product-pagination label{display:flex;align-items:center;gap:6px}.product-pagination select{padding:5px 8px;min-height:30px!important;font-size:11px!important}.product-pagination .btn-secondary{padding:6px 10px;font-size:11px}.product-pagination button:disabled{opacity:.4;cursor:not-allowed}
.product-configuration .foundation-table-scroll table{min-width:650px}.product-configuration .foundation-table-scroll td{vertical-align:middle;padding:16px!important}.product-configuration .foundation-table-scroll th{background:#f7f3eb;color:#9b8a71}.product-configuration .foundation-table-scroll tbody tr:hover{background:#fcfaf5}.product-configuration .foundation-records{border-color:#e8dfd0}.product-configuration .foundation-table-scroll .product-name-cell small{display:block;color:#a79a86;font-size:11px;margin-top:5px}
</style>

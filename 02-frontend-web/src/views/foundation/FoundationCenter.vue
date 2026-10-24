<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

interface Field { name: string; label: string; type: string; ref?: string; required: boolean; options?: Record<string,string> }
interface Definition { key: string; label: string; fields: Field[]; roles: number[] }
type Row = Record<string, any>
const auth = useAuthStore(), route = useRoute(), router = useRouter()
const catalog = ref<Definition[]>([]), records = ref<Row[]>([]), lookups = reactive<Record<string,Row[]>>({})
const loading = ref(false), saving = ref(false), message = ref(''), error = ref(''), search = ref('')
const modal = ref(false), editing = ref<number>(), form = reactive<Row>({}), expansion = ref<Row[]>([]), bomQuantity = ref(100)
const groups = [
  { title:'主数据', keys:['units','categories','materials','products','customers','suppliers','workshops','stations','station-resources','mappings'] },
  { title:'仓储位置', keys:['warehouses','areas','locations'] },
  { title:'产品与工艺', keys:['bom-versions','bom-lines','routes','operations','consumption','sops'] },
  { title:'班组排班', keys:['teams','members','shifts','schedules','holidays','rotations','rotation-teams','rotation-shifts'] },
  { title:'质量配置', keys:['defects','qc-items','qc-templates','qc-lines'] },
  { title:'设备点检', keys:['equipment','check-subjects','checks','check-plan-subjects','check-records'] },
  { title:'系统与组织', keys:['positions','user-positions','code-rules','menus','role-menus','parameters','jobs'] },
]
const key = computed(()=>String(route.params.key || 'workshops'))
const definition = computed(()=>catalog.value.find(x=>x.key===key.value))
const canEdit = computed(()=>auth.currentUser?.roleId===9 || !!definition.value?.roles.includes(Number(auth.currentUser?.roleId)))
const filtered = computed(()=>records.value.filter(r=>Object.values(r).join(' ').toLowerCase().includes(search.value.toLowerCase())))
const categoryId=ref('')
const categoryOptions=computed(()=>{
  const all=lookups.categories||[],out:Row[]=[]
  const walk=(parent:number,depth:number,seen:Set<number>)=>{for(const c of all.filter(r=>Number(r.parent_id||0)===parent)){if(seen.has(c.id))continue;out.push({...c,treeLabel:'　'.repeat(depth)+c.name});walk(c.id,depth+1,new Set([...seen,c.id]))}}
  walk(0,0,new Set());return out
})
const visibleRecords=computed(()=>{
  if(!categoryId.value)return filtered.value
  const ids=new Set<number>([Number(categoryId.value)]);let changed=true
  while(changed){changed=false;for(const c of lookups.categories||[])if(ids.has(Number(c.parent_id))&&!ids.has(Number(c.id))){ids.add(Number(c.id));changed=true}}
  return filtered.value.filter(r=>ids.has(Number(r.category_id)))
})
const rowName = (r:Row) => String(r.name || r.material_name || r.product_name || r.customer_name || r.supplier_name || r.warehouse_name || r.equipment_name || r.uom_name || r.code || r.id)
function display(f:Field, value:any) {
  if(value===null || value===undefined || value==='') return '—'
  if(f.options) return f.options[String(value)] || value
  if(f.ref) return rowName((lookups[f.ref] || []).find(x=>Number(x.id)===Number(value)) || {name:`#${value}`})
  return String(value)
}
async function load() {
  loading.value=true; error.value=''; expansion.value=[]
  try {
    if(!catalog.value.length) catalog.value=await request.get<Definition[]>('/foundation/catalog')
    if(!definition.value) throw new Error('配置类型不存在')
    records.value=await request.get<Row[]>(`/foundation/${key.value}`)
    await Promise.all([...new Set(definition.value.fields.map(f=>f.ref).filter(Boolean))].map(async r=>{lookups[r!]=await request.get<Row[]>(`/foundation/${r}`)}))
  } catch(e:any) { error.value=e.message || '加载失败'; records.value=[] }
  finally {loading.value=false}
}
function edit(row?:Row) {
  editing.value=row?.id; Object.keys(form).forEach(k=>delete form[k]); error.value=''
  definition.value?.fields.forEach(f=>{
    let value=row?.[f.name] ?? ''
    if(!row&&value===''){if(f.name==='conversion_rate')value=1;if(f.name==='data_type')value='TEXT';if(f.name==='phase')value='FINAL';if(f.name==='is_key')value='0';if(f.name==='enabled')value='1';if(f.name==='plan_type')value='CHECK'}
    if(f.type==='datetime-local' && typeof value==='string')value=value.replace(' ','T').slice(0,16)
    form[f.name]=value
  }); modal.value=true
}
async function save() {
  saving.value=true;error.value=''
  try {
    const data=Object.fromEntries(Object.entries(form).map(([k,v])=>[k,v===''?null:v]))
    if(editing.value) await request.put(`/foundation/${key.value}/${editing.value}`,data)
    else await request.post(`/foundation/${key.value}`,data)
    if(key.value==='role-menus'||key.value==='menus')await auth.refreshMenuAccess()
    modal.value=false;message.value='保存成功';await load()
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
async function generateCode(){
  const rules:Record<string,string>={units:'UOM_CODE',products:'PRODUCT_CODE',operations:'PROCESS_CODE',routes:'ROUTE_CODE',checks:'CHECKPLAN_CODE',equipment:'MACHINERY_CODE','qc-templates':'QC_TEMPLATR_CODE','qc-items':'QC_ITEM_CODE',defects:'DEFECT_CODE','bom-versions':'BOM_CODE',teams:'TEAM_CODE',rotations:'CAL_PLAN_CODE',sops:'SOP_CODE',materials:'ITEM_CODE',suppliers:'VENDOR_CODE',customers:'CLIENT_CODE',workshops:'WORKSHOP_CODE',stations:'WORKSTATION_CODE',warehouses:'WAREHOUSE_CODE',areas:'AREA_CODE',locations:'LOCATION_CODE'}
  const rule=rules[key.value];if(!rule)return
  try{const codeField=definition.value?.fields.find(f=>f.name==='code'||f.name.endsWith('_code'));if(codeField)form[codeField.name]=await request.post(`/mes/codes/${rule}`)}catch(e:any){error.value=e.message}
}
watch(key,()=>{modal.value=false;search.value='';message.value='';load()})
onMounted(load)
</script>

<template>
  <MainLayout>
    <div class="mb-6 flex items-center justify-between gap-4">
      <div><p class="text-sm text-blue-600 mb-1">生产基础配置</p><h1 class="text-2xl font-bold text-gray-900">{{ definition?.label || '基础配置中心' }}</h1><p class="text-sm text-gray-500 mt-2">统一维护生产、仓储、质量和班组使用的档案；关联数据可通过名称选择。</p></div>
      <button class="rounded-lg border px-4 py-2 bg-white" @click="router.push('/manufacturing')">制造工单与检验 →</button>
    </div>
    <div class="grid grid-cols-[200px_minmax(0,1fr)] gap-5">
      <aside class="bg-white border rounded-xl p-3 self-start">
        <div v-for="group in groups" :key="group.title" class="mb-4"><p class="px-2 text-xs text-gray-400 mb-1">{{ group.title }}</p>
          <button v-for="item in catalog.filter(x=>group.keys.includes(x.key))" :key="item.key" class="block w-full text-left px-3 py-2 rounded-lg text-sm" :class="key===item.key?'bg-blue-50 text-blue-700 font-medium':'text-gray-600 hover:bg-gray-50'" @click="router.push(`/foundation/${item.key}`)">{{ item.label }}</button>
        </div>
      </aside>
      <section class="min-w-0">
        <p v-if="message" role="status" class="mb-3 rounded-lg bg-green-50 p-3 text-green-700">{{ message }}</p>
        <p v-if="error" role="alert" class="mb-3 rounded-lg bg-red-50 p-3 text-red-700">{{ error }}</p>
        <div class="bg-white border rounded-xl overflow-hidden">
          <div class="p-4 flex flex-wrap gap-3 items-center border-b">
            <select v-if="['materials','products'].includes(key)" v-model="categoryId" class="border rounded-lg px-3 py-2" aria-label="按分类及子分类筛选"><option value="">全部分类</option><option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{c.treeLabel}}</option></select>
            <input v-model="search" aria-label="搜索记录" placeholder="搜索当前列表" class="border rounded-lg px-3 py-2 flex-1 min-w-32" />
            <button class="border rounded-lg px-4 py-2" :disabled="loading" @click="load">刷新</button>
            <button v-if="canEdit" class="bg-blue-600 text-white rounded-lg px-4 py-2" @click="edit()">新增{{ definition?.label }}</button>
            <span v-else class="text-sm text-gray-500">当前岗位只读</span>
          </div>
          <div v-if="key==='bom-versions'" class="px-4 py-3 bg-blue-50 text-sm">展开生产数量：<input v-model.number="bomQuantity" type="number" min="1" class="border rounded px-2 py-1 w-24" aria-label="BOM展开数量" />，点击对应BOM的“展开用料需求”。</div>
          <div class="overflow-x-auto"><table class="w-full text-sm text-left"><thead class="bg-gray-50 text-gray-600"><tr><th class="px-4 py-3">编号</th><th v-for="f in definition?.fields" :key="f.name" class="px-4 py-3 whitespace-nowrap">{{f.label}}</th><th class="px-4 py-3">操作</th></tr></thead>
            <tbody class="divide-y"><tr v-for="row in visibleRecords" :key="row.id"><td class="px-4 py-3 text-gray-400">{{row.id}}</td><td v-for="f in definition?.fields" :key="f.name" class="px-4 py-3 max-w-64 break-words">{{display(f,row[f.name])}}</td><td class="px-4 py-3 whitespace-nowrap"><button v-if="key==='bom-versions'" class="text-blue-600 mr-3" @click="expand(row)">展开用料需求</button><template v-if="canEdit && key!=='check-records'"><button class="text-blue-600 mr-3" @click="edit(row)">编辑</button><button v-if="key!=='locations'" class="text-red-600" @click="remove(row)">删除</button></template><span v-else>—</span></td></tr>
            <tr v-if="!visibleRecords.length"><td :colspan="(definition?.fields.length || 0)+2" class="p-12 text-center text-gray-400">{{ loading?'正在加载…':'暂无记录，可按业务需要新增。' }}</td></tr></tbody>
          </table></div>
        </div>
        <div v-if="expansion.length" class="mt-5 bg-white border rounded-xl p-5"><h2 class="font-semibold mb-3">用料展开结果 · {{bomQuantity}}只</h2><p class="text-sm text-gray-500 mb-4">共{{expansion.length}}种叶子物料；已合并跨半成品的同一物料，根据供应方式保留外购半成品；自制半成品继续向下展开。</p><table class="w-full text-sm"><thead><tr class="text-left border-b"><th class="p-2">物料编码</th><th>名称</th><th>需求量</th><th>单位</th></tr></thead><tbody><tr v-for="row in expansion" :key="row.id" class="border-b"><td class="p-2">{{row.material_code}}</td><td>{{row.material_name}}</td><td>{{row.required_quantity}}</td><td>{{row.unit}}</td></tr></tbody></table></div>
      </section>
    </div>
    <div v-if="modal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center p-6" @click.self="modal=false">
      <form class="bg-white rounded-xl shadow-xl p-6 w-full max-w-xl max-h-[85vh] overflow-y-auto" @submit.prevent="save">
        <h2 class="font-bold text-xl mb-5">{{editing?'编辑':'新增'}}{{definition?.label}}</h2>
        <p v-if="error" role="alert" class="bg-red-50 text-red-700 p-3 rounded mb-4">{{error}}</p>
        <div v-for="f in definition?.fields" :key="f.name" class="mb-4"><label :for="`field-${f.name}`" class="block text-sm font-medium text-gray-700 mb-1">{{f.label}}<span v-if="f.required" class="text-red-500"> *</span></label>
          <select v-if="f.ref" :id="`field-${f.name}`" v-model="form[f.name]" :required="f.required" class="w-full rounded-lg border px-3 py-2"><option value="">请选择</option><option v-for="r in lookups[f.ref] || []" :key="r.id" :value="r.id">{{rowName(r)}} (#{{r.id}})</option></select>
          <select v-else-if="f.options" :id="`field-${f.name}`" v-model="form[f.name]" :required="f.required" class="w-full rounded-lg border px-3 py-2"><option value="">请选择</option><option v-for="(label,value) in f.options" :key="value" :value="value">{{label}}</option></select>
          <template v-else-if="f.type==='image'"><input type="file" accept="image/png,image/jpeg" @change="uploadSop" /><p class="text-xs text-gray-500 mt-1">{{form[f.name]||'上传图片后自动保存地址'}}</p></template><input v-else :id="`field-${f.name}`" v-model="form[f.name]" :required="f.required" :type="['number','positive','decimal'].includes(f.type)?'number':f.type" :min="f.type==='positive'?0.0001:f.type==='number'?0:undefined" :step="['number','positive','decimal'].includes(f.type)?'0.0001':undefined" maxlength="500" class="w-full rounded-lg border px-3 py-2" />
        </div>
        <button v-if="!editing" type="button" class="text-blue-600 mb-3" @click="generateCode">按编码规则自动生成编号</button><div class="flex justify-end gap-3 mt-6"><button type="button" class="border rounded-lg px-4 py-2" @click="modal=false">取消</button><button :disabled="saving" class="bg-blue-600 text-white rounded-lg px-5 py-2">{{saving?'保存中…':'保存'}}</button></div>
      </form>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { useFormDrafts } from '@/composables/useFormDrafts'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import CategoryManager from '@/components/CategoryManager.vue'
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
const isWarehouseSetup = computed(()=>currentGroup.value.module==='仓库管理')
const navigationGroups = computed(()=>[currentGroup.value])
const definition = computed(()=>catalog.value.find(x=>x.key===key.value))
const canEdit = computed(()=>auth.effectiveRoleId===9 || !!definition.value?.roles.includes(Number(auth.effectiveRoleId)))
const internalCode=(f:Field)=>!f.ref&&(f.name==='code'||f.name.endsWith('_code'))
const visibleFields=computed(()=>definition.value?.fields.filter(f=>!internalCode(f))||[])
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
const maintenancePeople=ref<Row[]>([])
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
  }); modal.value=true
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
  <MainLayout>
    <div class="mb-6 flex items-center justify-between gap-4">
      <div><p class="text-sm text-blue-600 mb-1">{{ currentGroup.module }} / {{ currentGroup.title }}</p><h1 class="text-2xl font-bold text-gray-900">{{ definition?.label || currentGroup.title }}</h1><p class="text-sm text-gray-500 mt-2">{{ isWarehouseSetup?'按仓库、库区、库位三级维护存储位置，供入库、出库和库存查询使用。':key==='categories'?'建立清晰的分类层级，让原材料、半成品和成品各有归属。':`维护${currentGroup.title}，供${currentGroup.module}相关业务使用。` }}</p></div>
      <button v-if="currentGroup.returnPath&&auth.hasMenu(currentGroup.returnPath)" class="rounded-lg border px-4 py-2 bg-white" @click="router.push(currentGroup.returnPath)">{{ currentGroup.returnLabel }} →</button>
    </div>
    <div class="grid grid-cols-[200px_minmax(0,1fr)] gap-5">
      <aside class="foundation-menu bg-white border rounded-xl p-3 self-start" :aria-label="currentGroup.title" tabindex="0">
        <div v-for="group in navigationGroups" :key="group.title" class="mb-4"><p class="px-2 text-xs text-gray-400 mb-1">{{ group.title }}</p>
          <button v-for="item in catalog.filter(x=>group.keys.includes(x.key)&&auth.hasMenu(`/foundation/${x.key}`))" :key="item.key" class="block w-full text-left px-3 py-2 rounded-lg text-sm" :class="key===item.key?'bg-blue-50 text-blue-700 font-medium':'text-gray-600 hover:bg-gray-50'" @click="router.push(`/foundation/${item.key}`)">{{ item.label }}</button>
        </div>
      </aside>
      <section class="min-w-0">
        <p v-if="message" role="status" class="mb-3 rounded-lg bg-green-50 p-3 text-green-700">{{ message }}</p>
        <p v-if="error" role="alert" class="mb-3 rounded-lg bg-red-50 p-3 text-red-700">{{ error }}</p>
        <CategoryManager v-if="key==='categories'" :records="records" :loading="loading" :can-edit="canEdit" @edit="edit" @child="addChildCategory" @remove="remove" @refresh="load" />
        <div v-else class="bg-white border rounded-xl overflow-hidden">
          <div class="p-4 flex flex-wrap gap-3 items-center border-b">
            <select v-if="['materials','products'].includes(key)" v-model="categoryId" class="border rounded-lg px-3 py-2" aria-label="按分类及子分类筛选"><option value="">全部分类</option><option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{c.treeLabel}}</option></select>
            <input v-model="search" aria-label="搜索记录" placeholder="搜索当前列表" class="border rounded-lg px-3 py-2 flex-1 min-w-32" />
            <button class="border rounded-lg px-4 py-2" :disabled="loading" @click="load">刷新</button>
            <button v-if="canEdit&&!['code-rules','parameters','jobs','menus'].includes(key)" class="bg-blue-600 text-white rounded-lg px-4 py-2" @click="edit()">新增{{ definition?.label }}</button>
            <span v-else class="text-sm text-gray-500">当前岗位只读</span>
          </div>
          <div v-if="key==='bom-versions'" class="px-4 py-3 bg-blue-50 text-sm">展开生产数量：<input v-model.number="bomQuantity" type="number" min="1" class="border rounded px-2 py-1 w-24" aria-label="BOM展开数量" />，点击对应BOM的“展开用料需求”。</div>
          <div class="overflow-x-auto"><table class="w-full text-sm text-left"><thead class="bg-gray-50 text-gray-600"><tr><th v-for="f in visibleFields" :key="f.name" class="px-4 py-3 whitespace-nowrap">{{f.label}}</th><th class="px-4 py-3">操作</th></tr></thead>
            <tbody class="divide-y"><tr v-for="row in visibleRecords" :key="row.id"><td v-for="f in visibleFields" :key="f.name" class="px-4 py-3 max-w-64 break-words">{{display(f,row[f.name])}}</td><td class="px-4 py-3 whitespace-nowrap"><button v-if="key==='bom-versions'" class="text-blue-600 mr-3" @click="expand(row)">展开用料需求</button><template v-if="canEdit && key!=='check-records'"><button class="text-blue-600 mr-3" @click="edit(row)">编辑</button><button class="text-red-600" @click="remove(row)">删除</button></template><span v-else>—</span></td></tr>
            <tr v-if="!visibleRecords.length"><td :colspan="(definition?.fields.length || 0)+2" class="p-12 text-center text-gray-400">{{ loading?'正在加载…':'暂无记录，可按业务需要新增。' }}</td></tr></tbody>
          </table></div>
        </div>
        <div v-if="expansion.length" class="mt-5 bg-white border rounded-xl p-5"><h2 class="font-semibold mb-3">用料展开结果 · {{bomQuantity}}只</h2><p class="text-sm text-gray-500 mb-4">共{{expansion.length}}种叶子物料；已合并跨半成品的同一物料，根据供应方式保留外购半成品；自制半成品继续向下展开。</p><table class="w-full text-sm"><thead><tr class="text-left border-b"><th class="p-2">物料编码</th><th>名称</th><th>需求量</th><th>单位</th></tr></thead><tbody><tr v-for="row in expansion" :key="row.id" class="border-b"><td class="p-2">{{row.material_code}}</td><td>{{row.material_name}}</td><td>{{row.required_quantity}}</td><td>{{row.unit}}</td></tr></tbody></table></div>
      </section>
    </div>
    <div v-if="modal" class="fixed inset-0 bg-black/40 z-[80] flex items-center justify-center p-6" data-business-form @click.self="modal=false">
      <form class="bg-white rounded-xl shadow-xl p-6 w-full max-w-xl max-h-[85vh] overflow-y-auto" @submit.prevent="save"><p class="text-xs text-gray-500 mb-3" role="note">退出填写会保留草稿；保存成功后自动关闭。</p>
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
  </MainLayout>
</template>

<style scoped>
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
</style>

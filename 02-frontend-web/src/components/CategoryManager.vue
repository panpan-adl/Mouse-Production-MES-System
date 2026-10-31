<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChevronRight, Folder, FolderOpen, Plus, Search } from 'lucide-vue-next'

type Category = Record<string, any>
const props = defineProps<{ records: Category[]; loading: boolean; canEdit: boolean }>()
const emit = defineEmits<{ edit: [row?: Category]; child: [row: Category]; remove: [row: Category]; refresh: [] }>()
const search = ref(''), kind = ref(''), collapsed = ref(new Set<number>())
const matches = computed(() => {
  const ids = new Set<number>()
  const byId = new Map(props.records.map(row => [Number(row.id), row]))
  for (const row of props.records) {
    if (kind.value && row.kind !== kind.value) continue
    if (!`${row.name} ${row.code || ''}`.toLowerCase().includes(search.value.trim().toLowerCase())) continue
    let current: Category | undefined = row
    const seen = new Set<number>()
    while (current && !seen.has(Number(current.id))) {
      const id = Number(current.id)
      seen.add(id); ids.add(id); current = byId.get(Number(current.parent_id))
    }
  }
  return ids
})
const rows = computed(() => {
  const result: { row: Category; depth: number; children: number }[] = []
  const seen = new Set<number>()
  const walk = (row: Category, depth: number) => {
    const id = Number(row.id)
    if (seen.has(id) || !matches.value.has(id)) return
    seen.add(id)
    const children = props.records.filter(c => Number(c.parent_id) === id)
    result.push({ row, depth, children: children.length })
    if (!search.value && !kind.value && collapsed.value.has(id)) return
    children.forEach(child => walk(child, depth + 1))
  }
  props.records.filter(row => !props.records.some(p => Number(p.id) === Number(row.parent_id))).forEach(row => walk(row, 0))
  // Keep malformed cyclic records accessible so they can be corrected.
  props.records.filter(row => !seen.has(Number(row.id)) && matches.value.has(Number(row.id))).forEach(row => {
    if (search.value || kind.value || !props.records.some(p => Number(p.id) === Number(row.parent_id))) walk(row, 0)
  })
  return result
})
function toggle(id: number) {
  const next = new Set(collapsed.value)
  next.has(id) ? next.delete(id) : next.add(id)
  collapsed.value = next
}
</script>

<template>
  <div class="category-manager">
    <div class="category-intro">
      <div><h2>分类目录 <span>{{ records.length }} 个分类</span></h2><p>按层级整理物料和产品。在档案中选择分类后，即可按分类及其下级快速筛选。</p></div>
      <button v-if="canEdit" class="category-primary" @click="emit('edit')"><Plus :size="16" />新增一级分类</button>
    </div>
    <div class="category-toolbar">
      <label class="category-search"><Search :size="16" /><input v-model="search" aria-label="搜索分类名称或编码" placeholder="搜索分类名称或编码" /></label>
      <select v-model="kind" aria-label="筛选分类类型"><option value="">全部类型</option><option value="MATERIAL">物料</option><option value="PRODUCT">产品</option></select>
      <button @click="collapsed = new Set()">全部展开</button>
      <button @click="collapsed = new Set(records.map(row => Number(row.id)))">全部收起</button>
      <button :disabled="loading" @click="emit('refresh')">{{ loading ? '加载中…' : '刷新' }}</button>
    </div>
    <div class="category-table-wrap">
      <table class="category-table">
        <thead><tr><th>分类名称 / 层级</th><th>类型</th><th>直接子分类</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="entry in rows" :key="entry.row.id">
            <td><div class="category-name" :style="{ paddingLeft: `${entry.depth * 24}px` }">
              <button v-if="entry.children" class="category-toggle" :aria-label="`${collapsed.has(Number(entry.row.id)) ? '展开' : '收起'}${entry.row.name}`" :aria-expanded="!!search || !!kind || !collapsed.has(Number(entry.row.id))" @click="toggle(Number(entry.row.id))"><ChevronRight :size="16" :class="{ expanded: !!search || !!kind || !collapsed.has(Number(entry.row.id)) }" /></button>
              <span v-else class="category-toggle-placeholder"></span>
              <component :is="entry.children ? FolderOpen : Folder" :size="18" class="category-folder" />
              <div><strong>{{ entry.row.name }}</strong></div>
            </div></td>
            <td><span class="category-badge" :class="{ product: entry.row.kind === 'PRODUCT' }">{{ entry.row.kind === 'PRODUCT' ? '产品' : '物料' }}</span></td>
            <td class="category-count">{{ entry.children ? `${entry.children} 个` : '—' }}</td>
            <td><div v-if="canEdit" class="category-actions"><button @click="emit('child', entry.row)">新增子分类</button><button @click="emit('edit', entry.row)">编辑</button><button class="category-delete" @click="emit('remove', entry.row)">删除</button></div><span v-else>只读</span></td>
          </tr>
          <tr v-if="!rows.length"><td colspan="4" class="category-empty">{{ loading ? '正在加载分类…' : records.length ? '没有匹配的分类，请调整搜索或筛选条件。' : '还没有分类，可以先新建“原材料”或“成品”等一级分类。' }}</td></tr>
        </tbody>
      </table>
    </div>
    <p class="category-note">物料：通常为外购且本厂不生产的物资。产品：本厂生产的半成品或成品。分类用于整理档案，用料组成请在 BOM 中配置。</p>
  </div>
</template>

<style scoped>
.category-manager{border:1px solid #e9e7e2;border-radius:12px;background:white;overflow:hidden}
.category-intro{display:flex;justify-content:space-between;align-items:center;gap:20px;padding:24px}
.category-intro h2{font-size:17px;font-weight:600;color:#37352f}.category-intro h2 span{font-size:12px;font-weight:400;color:#929087;margin-left:10px}
.category-intro p,.category-note{font-size:12px;line-height:1.8;color:#88867f;margin-top:8px}
.category-primary{display:flex;align-items:center;gap:6px;background:#494740;color:white;padding:10px 14px;border-radius:7px;white-space:nowrap;font-size:13px}
.category-toolbar{display:flex;align-items:center;gap:10px;padding:0 24px 20px;flex-wrap:wrap}
.category-search{display:flex;align-items:center;gap:8px;flex:1;min-width:180px;border:1px solid #e9e7e2;border-radius:7px;padding:9px 12px;color:#aaa69d}
.category-search input{width:100%;outline:none;font-size:13px;background:transparent}
.category-toolbar select,.category-toolbar>button{font-size:12px;padding:9px 10px;border:1px solid #e9e7e2;border-radius:7px;background:white;color:#66635c}
.category-table-wrap{overflow:auto;max-height:65vh}.category-table{width:100%;text-align:left;font-size:13px;white-space:nowrap}
.category-table th{position:sticky;top:0;background:#f7f6f3;color:#8c8981;font-weight:500;padding:13px 20px;z-index:1}
.category-table td{padding:14px 20px;border-top:1px solid #f0eeea}.category-table tbody tr:hover{background:#fcfbf8}
.category-name{display:flex;align-items:center;gap:8px}.category-name strong{font-weight:500;color:#494740}
.category-toggle,.category-toggle-placeholder{width:20px;flex-shrink:0}.category-toggle svg{transition:transform .15s}.category-toggle .expanded{transform:rotate(90deg)}.category-folder{color:#b39b70;flex-shrink:0}
.category-badge{display:inline-block;padding:3px 9px;border-radius:5px;background:#f1eee7;color:#8c7755;font-size:11px}.category-badge.product{background:#edf2f6;color:#617c92}.category-count{color:#969187}
.category-actions{display:flex;gap:16px;color:#667f93;font-size:12px}.category-actions .category-delete{color:#b97878}.category-actions button:hover{text-decoration:underline}
.category-empty{text-align:center;padding:48px 20px!important;color:#969187}.category-note{margin:0;padding:14px 24px;border-top:1px solid #e9e7e2;background:#fcfbf8}
@media(max-width:900px){.category-intro{align-items:flex-start;flex-direction:column}.category-intro,.category-toolbar{padding:16px}.category-note{padding:14px 16px}}
</style>

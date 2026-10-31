<script setup lang="ts">
import { computed } from 'vue'
import { Mouse, Cpu, Package, Pencil, Trash2, SearchX } from 'lucide-vue-next'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
type Row = Record<string, any>
const props = defineProps<{ rows: Row[]; categories: Row[]; loading: boolean; canEdit: boolean }>()
const emit = defineEmits<{ edit: [row: Row]; remove: [row: Row] }>()
const rows = computed(() => props.rows)
const { page, pageSize, total, items } = usePagination(rows)
const category = (row: Row) => props.categories.find(c => Number(c.id) === Number(row.category_id))?.name || '未分类'
const icon = (row: Row) => /组件|主板|预装/.test(row.product_name) ? Cpu : /鼠标/.test(row.product_name) ? Mouse : Package
const money = (value: unknown) => value === null || value === undefined || value === '' ? '—' : Number(value).toLocaleString('zh-CN', { style: 'currency', currency: 'CNY' })
</script>

<template>
  <div class="product-table-wrap" :aria-busy="loading">
    <div class="product-list-caption"><span>产品明细 <b>{{ total }}</b></span><span>统一维护产品规格与定价</span></div>
    <div class="product-scroll">
      <table class="product-table">
        <thead><tr><th>产品信息</th><th>分类</th><th>规格 / 型号</th><th>计量单位</th><th class="numeric">成本价</th><th class="numeric">销售价</th><th class="actions">操作</th></tr></thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="empty">正在加载产品档案…</td></tr>
          <tr v-for="row in loading ? [] : items" :key="row.id">
            <td><div class="product-identity"><span class="product-icon" :class="{ component: icon(row) === Cpu }"><component :is="icon(row)" :size="21" :stroke-width="1.6" /></span><div><strong>{{ row.product_name }}</strong><span class="product-code">{{ row.product_code || '产品档案' }}</span></div></div></td>
            <td><span class="category-pill" :class="{ unassigned: !row.category_id }">{{ category(row) }}</span></td>
            <td><div class="specification">{{ row.specification || '暂无规格说明' }}</div><span class="model">型号 · {{ row.model || '未填写' }}</span></td>
            <td><span class="unit">{{ row.unit || '—' }}</span></td>
            <td class="numeric cost">{{ money(row.cost_price) }}</td>
            <td class="numeric price">{{ money(row.selling_price) }}</td>
            <td class="actions"><div v-if="canEdit" class="action-buttons"><button type="button" class="edit" @click="emit('edit', row)"><Pencil :size="14" />编辑</button><button type="button" class="remove" :aria-label="`删除${row.product_name}`" @click="emit('remove', row)"><Trash2 :size="14" /><span>删除</span></button></div><span v-else class="model">只读</span></td>
          </tr>
          <tr v-if="!loading && !total"><td colspan="7" class="empty"><SearchX :size="30" /><strong>暂无匹配的产品</strong><span>调整搜索条件，或新增产品档案。</span></td></tr>
        </tbody>
      </table>
    </div>
    <div class="pagination"><TablePagination v-model:page="page" v-model:page-size="pageSize" :total="total" label="产品档案" /></div>
  </div>
</template>

<style scoped>
.product-list-caption{display:flex;justify-content:space-between;align-items:center;padding:18px 24px 14px;font-size:12px;color:#9a9287}.product-list-caption>span:first-child{font-size:13px;font-weight:600;color:#5c554b}.product-list-caption b{margin-left:7px;font-size:11px;font-weight:500;background:#f0ece5;padding:2px 7px;border-radius:5px}.product-scroll{overflow-x:auto}.product-table{width:100%;min-width:950px;border-collapse:collapse;text-align:left;font-size:13px}.product-table th{padding:13px 20px;background:#f7f6f2;border-block:1px solid #eeebe4;color:#948a7c;font-size:11px;font-weight:500;white-space:nowrap}.product-table td{padding:17px 20px;border-bottom:1px solid #f0ede7;color:#70685d;vertical-align:middle}.product-table tbody tr:hover{background:#fcfbf8}.product-identity{display:flex;align-items:center;gap:12px;min-width:190px}.product-icon{display:grid;place-items:center;flex-shrink:0;width:43px;height:43px;border-radius:12px;background:#edf2ef;color:#6b897a}.product-icon.component{background:#edf0f6;color:#7a88a0}.product-identity strong{display:block;color:#443e34;font-size:13px;font-weight:600;line-height:1.6}.product-code{display:block;margin-top:4px;color:#a29a8e;font-size:10px;letter-spacing:.4px}.category-pill{display:inline-block;background:#f2eee6;color:#8c7759;padding:5px 9px;border:1px solid #e9e2d6;border-radius:6px;font-size:11px;white-space:nowrap}.category-pill.unassigned{background:#f6f6f4;border-color:#eae8e2;color:#a7a095}.specification{max-width:220px;line-height:1.6;color:#746c60}.model{display:block;font-size:11px;color:#a49b8e;margin-top:5px}.unit{font-size:12px;white-space:nowrap}.numeric{text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap}.product-table .cost{color:#9b9285}.product-table .price{font-weight:600;color:#534536}.actions{text-align:right;white-space:nowrap}.action-buttons{display:flex;justify-content:flex-end;gap:4px}.action-buttons button{display:inline-flex;align-items:center;gap:5px;padding:7px 9px;border-radius:6px;font-size:11px}.edit{color:#7b8b91}.edit:hover{background:#edf2f3}.remove{color:#bc9188}.remove:hover{background:#faf0ed}.empty{height:220px;text-align:center!important;color:#a49a8a!important}.empty svg{margin:0 auto 12px}.empty strong,.empty span{display:block;margin-top:7px;font-weight:400}.empty span{font-size:12px}.pagination{padding:0 22px 18px}.pagination :deep(.table-pagination){margin-top:16px}@media(max-width:700px){.product-list-caption{padding:16px}.product-list-caption>span:last-child{display:none}.pagination{padding-inline:14px}.product-table td,.product-table th{padding-inline:15px}}
</style>

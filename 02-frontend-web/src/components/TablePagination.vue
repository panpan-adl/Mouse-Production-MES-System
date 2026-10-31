<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ page: number; pageSize: number; total: number; label: string }>()
const emit = defineEmits<{ 'update:page': [value: number]; 'update:pageSize': [value: number] }>()
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
</script>
<template>
  <nav class="table-pagination" :aria-label="`${label}分页`">
    <span>共 {{ total }} 条</span>
    <label>每页 <select :value="pageSize" :aria-label="`${label}每页条数`" @change="emit('update:pageSize', Number(($event.target as HTMLSelectElement).value))"><option :value="10">10条</option><option :value="20">20条</option><option :value="50">50条</option></select></label>
    <span>{{ total ? (page - 1) * pageSize + 1 : 0 }}–{{ Math.min(page * pageSize, total) }} 条</span>
    <button type="button" :disabled="page <= 1" @click="emit('update:page', page - 1)">上一页</button>
    <span>第 {{ page }} / {{ pages }} 页</span>
    <button type="button" :disabled="page >= pages" @click="emit('update:page', page + 1)">下一页</button>
  </nav>
</template>
<style scoped>
.table-pagination{display:flex;align-items:center;justify-content:flex-end;flex-wrap:wrap;gap:12px;margin-top:18px;color:#807565;font-size:12px}
select,button{border:1px solid #e5dfd4;border-radius:6px;background:white;padding:6px 10px;color:#665239}
button:disabled{opacity:.4;cursor:not-allowed}label{display:flex;align-items:center;gap:6px}
</style>

import { computed, ref, watch, type ComputedRef } from 'vue'

export function usePagination<T>(rows: ComputedRef<T[]>) {
  const page = ref(1), pageSize = ref(10)
  const total = computed(() => rows.value.length)
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  const items = computed(() => rows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
  watch([rows, pageSize], () => { page.value = 1 }, { flush: 'sync' })
  return { page, pageSize, total, pageCount, items }
}

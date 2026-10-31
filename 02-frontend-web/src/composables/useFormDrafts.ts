import { isRef, nextTick, onBeforeUnmount, unref, watch, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

type State = Record<string, any>
interface DraftDialog { open: Ref<boolean | string>; fields: State; record?: () => unknown }
const prefix = 'mousemes:form-draft:v1:'
// Credentials and access tokens never belong in a local business-form draft.
function snapshot(value: unknown): any {
  return JSON.parse(JSON.stringify(value, (key, item) => /password|token|secret/i.test(key) ? undefined : item))
}
export function useFormDrafts(dialogs: Record<string, DraftDialog>, draftPath?: string) {
  const route = useRoute(), auth = useAuthStore()
  const active = new Map<string, string>()
  let restoring = false
  const storage = {
    read(key: string) { try { return JSON.parse(localStorage.getItem(key) || 'null') } catch { return null } },
    write(key: string, value: unknown) { try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* A full/disabled browser store must not break saving. */ } },
    remove(key: string) { try { localStorage.removeItem(key) } catch { /* optional local persistence */ } },
  }
  const values = (dialog: DraftDialog) => Object.fromEntries(Object.entries(dialog.fields).map(([name, value]) => [name, unref(value)]))
  function persist(name: string) {
    const key = active.get(name), dialog = dialogs[name]
    if (key && !restoring) storage.write(key, { values: snapshot(values(dialog)), updatedAt: Date.now() })
  }
  for (const [name, dialog] of Object.entries(dialogs)) {
    watch(dialog.open, async (open) => {
      if (!open) { persist(name); active.delete(name); return }
      const key = prefix + [auth.currentUser?.id || 'anonymous', draftPath || route.path, name, typeof open === 'string' ? open : '', dialog.record?.() || 'new'].join(':')
      active.set(name, key)
      const draft = storage.read(key)
      if (!draft?.values) return
      restoring = true
      await nextTick() // Allow the form's existing open/reset logic to finish first.
      const apply = () => {
        if (active.get(name) !== key || !dialog.open.value) return
        for (const [field, value] of Object.entries(draft.values)) {
          const target = dialog.fields[field]
          if (target === undefined) continue
          if (isRef(target)) target.value = value
          else if (target && typeof target === 'object' && value && typeof value === 'object') Object.assign(target, value)
        }
      }
      apply()
      await nextTick()
      apply() // Keep the saved choice after dependent-select watchers apply their defaults.
      await nextTick()
      restoring = false
    }, { flush: 'sync' })
    watch(() => values(dialog), () => { if (dialog.open.value) persist(name) }, { deep: true, flush: 'sync' })
  }
  const flush = () => { for (const name of active.keys()) persist(name) }
  window.addEventListener('pagehide', flush)
  onBeforeUnmount(() => { flush(); window.removeEventListener('pagehide', flush) })
  // Called only after successful persistence, never from validation or error handlers.
  function completeDrafts() {
    for (const [name, key] of active) {
      storage.remove(key)
      active.delete(name)
      const open = dialogs[name].open
      open.value = typeof open.value === 'string' ? '' : false
    }
  }
  return completeDrafts
}

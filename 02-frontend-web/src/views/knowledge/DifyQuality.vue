<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

type Work = {
  code: string
  product: string
  quantity: number
  status: string
  operations: { taskNo: string; sequence: number; name: string }[]
}
const auth = useAuthStore()
const canQuery = computed(() => [1, 2, 4, 5, 9].includes(Number(auth.currentUser?.roleId)))
const code = ref('')
const orders = ref<{ code: string; product: string }[] | null>(null)
const loadingOrders = ref(false)
async function loadOrders() {
  loadingOrders.value = true
  lookupError.value = ''
  code.value = ''
  clearProduction()
  work.value = null
  orders.value = null
  try {
    orders.value = await request.get<{ code: string; product: string }[]>('/dify/op30/work-orders')
  } catch (e) {
    lookupError.value = e instanceof Error ? e.message : '无法加载生产工单。'
  } finally {
    loadingOrders.value = false
  }
}
const work = ref<Work | null>(null)
const lookupError = ref('')
const lookingUp = ref(false)
const statusNames: Record<string, string> = {
  IN_PROGRESS: '生产中', PRODUCTION_COMPLETED: '已完工', COMPLETED: '已完工',
  TERMINATED: '已终止', CANCELLED: '已终止', SCRAP_HOLD: '已终止',
}
async function lookup() {
  if (lookingUp.value) return
  lookingUp.value = true
  clearProduction()
  work.value = null
  lookupError.value = ''
  if (!code.value) {
    lookingUp.value = false
    return
  }
  try {
    work.value = await request.get<Work>('/dify/op30/work-order', { params: { code: code.value } })
  } catch (e) {
    lookupError.value = e instanceof Error ? e.message : '查询失败。'
  } finally {
    lookingUp.value = false
  }
}

type Result = { status: string; report: string; runId: string }
const workOrder = ref('DEMO-M72-041')
const torque = ref<number | string>('')
const configured = ref(false)
const productionConfigured = ref(false)
const productionBusy = ref(false)
const actual = ref<number | string>('')
const lower = ref<number | string>('')
const upper = ref<number | string>('')
const productionError = ref('')
const productionResult = ref<Result | null>(null)
const hasOp30 = computed(() => work.value?.operations.some(operation => operation.sequence === 30))
function clearProduction() {
  actual.value = lower.value = upper.value = ''
  productionResult.value = null
  productionError.value = ''
}
async function runProduction() {
  if (!work.value || !productionConfigured.value || !hasOp30.value || productionBusy.value) return
  if (typeof actual.value !== 'number' || typeof lower.value !== 'number' || typeof upper.value !== 'number'
      || lower.value >= upper.value) {
    productionError.value = '请填写实测值和工艺上下限，上限必须大于下限。'
    return
  }
  productionBusy.value = true
  productionError.value = ''
  productionResult.value = null
  try {
    productionResult.value = await request.post<Result>('/dify/op30/production-run', {
      code: work.value.code, torque: actual.value, lower: lower.value, upper: upper.value,
    }, { timeout: 30000 })
  } catch (e) {
    productionError.value = e instanceof Error ? e.message : 'Dify 运行失败。'
  } finally {
    productionBusy.value = false
  }
}
const checking = ref(true)
const busy = ref(false)
const error = ref('')
const result = ref<Result | null>(null)

onMounted(async () => {
  try {
    const status = await request.get<{ configured: boolean; productionConfigured: boolean }>('/dify/op30/status')
    configured.value = status.configured
    productionConfigured.value = status.productionConfigured
  }
  catch { error.value = '无法检查 Dify 配置。请确认 MouseMES 后端已更新并启动。' }
  finally { checking.value = false }
})

async function run() {
  if (!configured.value || busy.value || typeof torque.value !== 'number') return
  busy.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await request.post<Result>('/dify/op30/run', {
      workOrder: workOrder.value, torque: torque.value,
    }, { timeout: 30000 })
  } catch (e) { error.value = e instanceof Error ? e.message : 'Dify 运行失败。' }
  finally { busy.value = false }
}
</script>

<template>
  <MainLayout>
    <section class="quality">
      <p class="tag">DIFY WORKFLOW · 演示</p>
      <h1>OP30 扭矩质检</h1>
      <p>提交演示工单和实测扭矩，查看 Dify 返回的质检判定、依据和处置建议。</p>
      <p v-if="checking" role="status">正在检查配置…</p>
      <p v-else-if="!configured && !error" role="status">Dify 尚未配置。请先在 Dify 工作室发布工作流，并设置后端应用密钥。</p>
      <h2>Dify 扭矩质检演示</h2>
      <form @submit.prevent="run">
        <label>演示工单号<input v-model.trim="workOrder" required maxlength="40" pattern="DEMO-[A-Za-z0-9\-]{1,35}" :disabled="busy"></label>
        <label>实测扭矩（N·m）<input v-model.number="torque" required type="number" min="0" max="10" step="0.0001" placeholder="例如 0.26" :disabled="busy"></label>
        <button type="submit" :disabled="!configured || busy">{{ busy ? '正在运行…' : '运行 Dify 工作流' }}</button>
      </form>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <article v-if="result" aria-live="polite">
        <h2>{{ result.status }}</h2>
        <p>{{ result.report }}</p>
        <small>Dify 运行编号：{{ result.runId }}</small>
      </article>
      <p class="note">扭矩是旋紧螺丝时的转动力矩，单位是 N·m（牛·米）。演示采用星舟 M72 的示例标准。</p>
      <section v-if="canQuery">
        <h2>生产工单查询</h2>
        <div class="lookup">
          <button type="button" :disabled="loadingOrders || lookingUp || productionBusy" @click="loadOrders">
            {{ loadingOrders ? '正在加载工单…' : orders === null ? '接入真实工单' : '刷新工单列表' }}
          </button>
          <label v-if="orders?.length">选择生产工单
            <select v-model="code" :disabled="lookingUp || productionBusy" @change="lookup">
              <option value="">请选择工单</option>
              <option v-for="order in orders" :key="order.code" :value="order.code">
                {{ order.code }} · {{ order.product }}
              </option>
            </select>
          </label>
          <p v-else-if="orders?.length === 0" role="status">当前没有生产工单。</p>
        </div>
        <p v-if="lookingUp" role="status">正在查询工单详情…</p>
        <p v-if="lookupError" class="error" role="alert">{{ lookupError }}</p>
        <article v-if="work" aria-live="polite">
          <h3>{{ work.code }} · {{ work.product }}</h3>
          <p>计划数量 {{ work.quantity }} · {{ statusNames[work.status] || work.status }}</p>
          <ul v-if="work.operations.length">
            <li v-for="operation in work.operations" :key="operation.taskNo">
              {{ operation.taskNo }} · {{ operation.sequence }} {{ operation.name }}
            </li>
          </ul>
          <p v-if="!hasOp30">该工单没有第 30 道工序。</p>
          <template v-else>
            <p>请确认第 30 道工序适用扭矩检验，并按该产品的工艺文件填写上下限。结果供人工复核。</p>
            <p v-if="!productionConfigured">请先配置真实工单的 Dify 应用密钥。</p>
            <form @submit.prevent="runProduction" @input="productionResult = null; productionError = ''">
              <fieldset :disabled="productionBusy">
                <label>扭矩下限（N·m）<input v-model.number="lower" type="number" min="0" step="0.000001" required></label>
                <label>扭矩上限（N·m）<input v-model.number="upper" type="number" min="0" step="0.000001" required></label>
                <label>实测扭矩（N·m）<input v-model.number="actual" type="number" min="0" step="0.000001" required></label>
                <button type="submit" :disabled="!productionConfigured">{{ productionBusy ? '正在运行…' : '运行真实工单 Dify 工作流' }}</button>
              </fieldset>
            </form>
            <p v-if="productionError" class="error" role="alert">{{ productionError }}</p>
            <div v-if="productionResult" aria-live="polite">
              <h4>{{ productionResult.status }}</h4>
              <p>{{ productionResult.report }}</p>
              <small>Dify 运行编号：{{ productionResult.runId }}</small>
            </div>
          </template>
        </article>
      </section>

    </section>
  </MainLayout>
</template>

<style scoped>
.quality{max-width:920px;margin:30px auto;padding:32px;background:#fff;border:1px solid #ddd8cd;border-radius:16px;color:#253b38}
.tag,.note,small{color:#526d67}
h1{margin:8px 0;font-size:30px}
form{display:flex;flex-wrap:wrap;gap:16px;align-items:end;margin:24px 0}
fieldset{border:0;padding:0;margin:0;display:flex;gap:12px;flex-wrap:wrap;align-items:end}
label{display:grid;gap:6px}
.lookup{display:flex;flex-wrap:wrap;gap:16px;align-items:end;margin:16px 0}
input,select,button{border-radius:8px;padding:10px 14px;border:1px solid #a9c0b8;font:inherit}
button{background:#116d64;color:white;cursor:pointer}
button:disabled{opacity:.55;cursor:default}
article{background:#eaf6f1;border-radius:10px;padding:16px;overflow-wrap:anywhere}
.error{color:#a13424}
</style>

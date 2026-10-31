<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/components/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { canVisit } from '@/utils/permissions'
import {
  ArrowUpRight,
  Boxes,
  ClipboardCheck,
  RefreshCw,
  ShoppingCart,
  TrendingUp,
  Wallet,
  Wrench,
  FileText,
  AlertTriangle,
} from 'lucide-vue-next'
import { financeApi, type FinanceGroupStat, type FinanceInventoryValueStat, type FinanceQualityLossStat, type FinanceOverview, type FinanceTrendStat } from '@/api/mes'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const message = ref('')
const month = ref(new Date().toISOString().slice(0, 7))
const scope=ref('全部业务')
const scopeLabel=computed(()=>scope.value==='全部业务'?'全部业务':`${scope.value} 月度`)
const metrics=computed(()=>[
  {label:'销售收入',value:overview.value.salesIncome,icon:TrendingUp,note:'已登记销售业务',tone:'olive'},
  {label:'采购支出',value:overview.value.purchaseExpense,icon:ShoppingCart,note:'采购业务汇总',tone:'amber'},
  {label:'维修支出',value:overview.value.maintenanceExpense,icon:Wrench,note:'设备维护成本',tone:'clay'},
  {label:'当前库存价值',value:inventoryValue.value.currentAmount,icon:Boxes,note:'不随月份筛选变化',tone:'sand'},
])
const expenses=computed(()=>Number(overview.value.purchaseExpense||0)+Number(overview.value.maintenanceExpense||0))
const purchaseShare=computed(()=>expenses.value?Number(overview.value.purchaseExpense||0)/expenses.value*100:0)
const chartMonths=computed(()=>visibleTrend.value.slice(-12))
const hasTrendAmount=computed(()=>chartMonths.value.some(row=>Number(row.salesIncome)||Number(row.purchaseExpense)||Number(row.maintenanceExpense)))
const trendX=(i:number)=>70+i*(600/Math.max(chartMonths.value.length,1))
const trendHeight=(value:number)=>Math.max(0,Number(value||0))/maxTrendValue.value*150
const visibleEntries=computed(()=>navEntries.filter(entry=>router.resolve(entry.path).matched.length&&canVisit(Number(authStore.effectiveRoleId),entry.path)&&authStore.hasMenu(entry.path)))

const overview = ref<FinanceOverview>({ salesIncome: 0, purchaseExpense: 0, maintenanceExpense: 0, inventoryAmount: 0, grossProfit: 0 })
const inventoryValue = ref<FinanceInventoryValueStat>({ currentQuantity: 0, currentAmount: 0, lowStockCount: 0 })
const qualityLoss = ref<FinanceQualityLossStat>({ defectiveQuantity: 0, scrapQuantity: 0, reworkQuantity: 0 })
const trend = ref<FinanceTrendStat[]>([])
const purchaseSupplier = ref<FinanceGroupStat[]>([])
const purchaseMaterial = ref<FinanceGroupStat[]>([])

const currentMonth = new Date().toISOString().slice(0, 7)

const visibleTrend = computed(() =>
  trend.value
    .filter((item) => /^\d{4}-\d{2}$/.test(item.month) && item.month <= currentMonth)
    .sort((a, b) => a.month.localeCompare(b.month))
)

const hiddenTrendCount = computed(() => trend.value.length - visibleTrend.value.length)

const maxTrendValue = computed(() => {
  const values = visibleTrend.value.flatMap((item) => [Number(item.salesIncome || 0), Number(item.purchaseExpense || 0), Number(item.maintenanceExpense || 0)])
  return Math.max(1, ...values)
})

// ── 可导航入口 ──
const navEntries = [
  { label: '销售订单', icon: FileText, path: '/order/list' },
  { label: '采购订单', icon: ShoppingCart, path: '/purchase/order' },
  { label: '库存与批次', icon: Boxes, path: '/inventory/trace' },
  { label: '质量记录', icon: ClipboardCheck, path: '/quality/check' },
  { label: '维修费用单', icon: Wrench, path: '/equipment/bills' },
]

function goTo(path: string) {
  router.push(path)
}

onMounted(loadFinance)

async function loadFinance() {
  loading.value = true
  message.value = ''
  try {
    const [overviewData, inventoryData, qualityData, trendData, supplierData, materialData] = await Promise.all([
      financeApi.overview(),
      financeApi.inventoryValue(),
      financeApi.qualityLoss(),
      financeApi.trendMonthly(),
      financeApi.purchaseSupplier(),
      financeApi.purchaseMaterial(),
    ])
    overview.value = overviewData
    scope.value='全部业务'
    inventoryValue.value = inventoryData
    qualityLoss.value = qualityData
    trend.value = trendData
    purchaseSupplier.value = supplierData
    purchaseMaterial.value = materialData
  } catch (error: any) {
    message.value = error.message || '财务数据加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMonthOverview() {
  if (!month.value) return
  loading.value = true
  try {
    overview.value = await financeApi.overviewByMonth(month.value)
    scope.value=month.value
    message.value = `已切换到 ${month.value} 月度总览`
  } catch (error: any) {
    message.value = error.message || '月度总览加载失败'
  } finally {
    loading.value = false
  }
}

function money(value?: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function percent(part?: number, total?: number) {
  if (!total) return '0.0%'
  return `${((Number(part || 0) / Number(total)) * 100).toFixed(1)}%`
}
</script>

<template>
  <MainLayout>
    <div class="finance-page" :aria-busy="loading">
      <header class="finance-header">
        <div><p class="eyebrow">MOUSE MES / FINANCIAL OVERVIEW</p><h1>财务看板<span>经营有数，成本有据。</span></h1><p class="header-note">销售、采购与设备成本，汇聚为清晰的经营视角。</p></div>
        <div class="finance-controls"><input v-model="month" type="month" aria-label="选择财务月份" :max="currentMonth"/><button :disabled="loading||!month" @click="loadMonthOverview">查看月度</button><button class="refresh-button" :disabled="loading" @click="loadFinance"><RefreshCw :size="14" :class="{spinning:loading}"/>{{loading?'加载中':'全部业务'}}</button></div>
      </header>
      <p v-if="message" role="status" class="finance-message">{{message}}</p>

      <section class="finance-top">
        <article class="balance-card">
          <div class="balance-top"><span><Wallet :size="16"/>经营收支差额</span><small>{{scopeLabel}}</small></div>
          <strong class="balance-number" :class="{negative:overview.grossProfit<0}">{{money(overview.grossProfit)}}</strong>
          <p>销售收入 − 采购支出 − 维修支出</p>
          <div class="balance-footer"><span>{{overview.grossProfit<0?'本期支出高于收入':'本期收入覆盖支出'}}</span><span>人民币 CNY</span></div>
          <svg class="balance-decoration" viewBox="0 0 240 150" aria-hidden="true"><circle cx="190" cy="75" r="65"/><circle cx="190" cy="75" r="45"/><path d="M0 145 L55 110 L95 120 L130 75 L170 87 L225 20"/></svg>
        </article>
        <div class="finance-metrics"><article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="metric.tone"><div><span>{{metric.label}}</span><component :is="metric.icon" :size="17"/></div><strong>{{money(metric.value)}}</strong><small>{{metric.note}}</small></article></div>
      </section>

      <div class="finance-main-grid">
        <section class="finance-panel trend-panel"><div class="panel-heading"><div><h2>月度收支趋势</h2><p>最近 {{chartMonths.length}} 个业务月份 · 不随上方月度筛选变化</p></div><span class="panel-tag">收入 / 支出</span></div>
          <div class="trend-legend"><span><i class="income"></i>销售收入</span><span><i class="purchase"></i>采购支出</span><span><i class="maintenance"></i>维修支出</span></div>
          <div v-if="!chartMonths.length||!hasTrendAmount" class="finance-empty trend-empty"><TrendingUp :size="28"/><strong>{{chartMonths.length?'当前月度趋势金额均为零':'暂无月度业务记录'}}</strong><span>总览按全部业务汇总，趋势按业务月份归集；未归入月份的金额不计入图表。</span></div>
          <svg v-else class="finance-trend-chart" viewBox="0 0 720 240" role="img" aria-label="月度销售收入、采购支出和维修支出对比柱状图">
            <g v-for="i in 4" :key="i"><path :d="`M45 ${195-(i-1)*50} H700`" stroke="#eee7dc" stroke-dasharray="4 5"/><text x="35" :y="199-(i-1)*50" text-anchor="end" class="axis-label">{{Math.round(maxTrendValue*(i-1)/3).toLocaleString()}}</text></g>
            <g v-for="(item,index) in chartMonths" :key="item.month" :transform="`translate(${trendX(index)} 0)`">
              <rect x="0" :y="195-trendHeight(item.salesIncome)" :width="Math.min(26,400/chartMonths.length/3)" :height="trendHeight(item.salesIncome)" rx="3" fill="#95a383"><title>{{item.month}} 销售收入 {{money(item.salesIncome)}}</title></rect>
              <rect :x="Math.min(30,400/chartMonths.length/3+3)" :y="195-trendHeight(item.purchaseExpense)" :width="Math.min(26,400/chartMonths.length/3)" :height="trendHeight(item.purchaseExpense)" rx="3" fill="#bd9967"><title>{{item.month}} 采购支出 {{money(item.purchaseExpense)}}</title></rect>
              <rect :x="Math.min(60,(400/chartMonths.length/3+3)*2)" :y="195-trendHeight(item.maintenanceExpense)" :width="Math.min(26,400/chartMonths.length/3)" :height="trendHeight(item.maintenanceExpense)" rx="3" fill="#c1927e"><title>{{item.month}} 维修支出 {{money(item.maintenanceExpense)}}</title></rect>
              <text x="28" y="219" text-anchor="middle" class="axis-label">{{item.month}}</text>
            </g>
          </svg>
          <div class="trend-footer"><span>金额单位：元</span><span v-if="hiddenTrendCount">已排除 {{hiddenTrendCount}} 条未来或异常月份记录</span><span v-else>依据已登记业务汇总</span></div>
        </section>
        <section class="finance-panel expense-panel"><div class="panel-heading"><div><h2>支出构成</h2><p>{{scopeLabel}} · 采购与维修</p></div><ShoppingCart :size="17"/></div>
          <div class="expense-body"><div class="expense-ring" :style="{background:expenses?`conic-gradient(#b99562 0% ${purchaseShare}%,#c49480 ${purchaseShare}% 100%)`:'#ece5d9'}"><div><small>支出合计</small><strong>{{money(expenses)}}</strong></div></div></div>
          <div class="expense-item"><span><i></i>采购支出</span><strong>{{money(overview.purchaseExpense)}}</strong></div><div class="expense-item maintenance-item"><span><i></i>维修支出</span><strong>{{money(overview.maintenanceExpense)}}</strong></div>
          <p v-if="!expenses" class="subtle-note">当前范围暂无采购或维修支出</p>
        </section>
      </div>

      <section class="finance-health"><div class="health-heading"><span>运营观察</span><small>库存为当前余额 · 质量为累计业务数量</small></div><div class="health-items">
        <article><Boxes :size="19"/><div><span>库存数量</span><strong>{{Number(inventoryValue.currentQuantity).toLocaleString()}}</strong><small>库存价值 {{money(inventoryValue.currentAmount)}}</small></div></article>
        <article><AlertTriangle :size="19"/><div><span>低库存物料</span><strong>{{inventoryValue.lowStockCount}}<small> 项</small></strong><small>关注补料与采购计划</small></div></article>
        <article><ClipboardCheck :size="19"/><div><span>质检异常数量</span><strong>{{qualityLoss.defectiveQuantity}}</strong><small>报废 {{qualityLoss.scrapQuantity}} · 返工 {{qualityLoss.reworkQuantity}}</small></div></article>
      </div></section>

      <div class="finance-ranking-grid"><section v-for="(group,index) in [{title:'供应商采购分布',subtitle:'按供应商汇总 · 全部业务',rows:purchaseSupplier},{title:'物料采购分布',subtitle:'按物料汇总 · 全部业务',rows:purchaseMaterial}]" :key="group.title" class="finance-panel ranking-panel"><div class="panel-heading"><div><h2>{{group.title}}</h2><p>{{group.subtitle}}</p></div><span class="panel-tag">TOP {{Math.min(6,group.rows.length)}}</span></div><div v-if="!group.rows.length" class="finance-empty">暂无采购统计</div><div class="ranking-list"><div v-for="(item,rank) in group.rows.slice(0,6)" :key="item.groupKey" class="ranking-row"><span class="rank-no">{{String(rank+1).padStart(2,'0')}}</span><div class="ranking-content"><div><span :title="item.groupName">{{item.groupName}}</span><strong>{{money(item.amount)}}</strong></div><div class="ranking-track"><i :class="{secondary:index===1}" :style="{width:percent(item.amount,Math.max(1,...group.rows.map(row=>Number(row.amount))))}"></i></div></div></div></div></section></div>

      <nav v-if="visibleEntries.length" class="finance-links" aria-label="财务业务入口"><span>业务明细</span><button v-for="entry in visibleEntries" :key="entry.path" @click="goTo(entry.path)"><component :is="entry.icon" :size="14"/>{{entry.label}}<ArrowUpRight :size="12"/></button></nav>
      <p class="finance-footnote">收支差额为销售收入减采购及维修支出，不等同于会计毛利润；库存余额、采购分布及质量统计不随月度总览筛选变化。</p>
    </div>
  </MainLayout>
</template>

<style scoped>
.finance-page{--ink:#66513a;--muted:#ab987d;--line:#e8dfd1;--paper:#fffdf9;display:flex;flex-direction:column;gap:22px;color:var(--ink);max-width:1480px;margin:auto}.finance-header{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:8px 0 13px;border-bottom:1px solid var(--line)}.eyebrow{font-size:9px;letter-spacing:.2em;color:#b29773;margin-bottom:12px}h1{font-size:29px;font-weight:600;color:#58452f;letter-spacing:.02em}h1 span{font-size:12px;font-weight:400;color:#b09b7e;margin-left:17px}.header-note{font-size:12px;color:#aa977d;margin-top:10px}.finance-controls{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.finance-controls input,.finance-controls button{font-size:12px;padding:10px 13px;border:1px solid #e5dac7;border-radius:7px;background:#fffdf8;color:#91734e;white-space:nowrap}.finance-controls>button:first-of-type{background:#9f7e51;color:#fffaf1;border-color:#9f7e51}.finance-controls button:disabled{opacity:.5;cursor:wait}.refresh-button{display:flex;align-items:center;gap:7px}.finance-message{font-size:12px;color:#98764a;padding:12px 15px;border-radius:7px;background:#f5eddd}
.finance-top{display:grid;grid-template-columns:1.05fr 2fr;gap:18px}.balance-card{position:relative;background:linear-gradient(120deg,#eee0c8,#f5ebda);border:1px solid #e3d1b3;border-radius:15px;overflow:hidden;padding:25px 27px;display:flex;flex-direction:column;justify-content:center;min-height:245px}.balance-top{display:flex;align-items:center;justify-content:space-between;position:relative;z-index:1;gap:10px}.balance-top>span{display:flex;align-items:center;gap:8px;font-size:12px;color:#8e7048}.balance-top small{font-size:10px;color:#ad9470;border:1px solid #d8c39f;padding:4px 8px;border-radius:4px}.balance-number{font-size:clamp(25px,2.5vw,40px);color:#79603e;font-weight:500;letter-spacing:-.04em;position:relative;z-index:1;margin-top:25px}.balance-number.negative{color:#a4775d}.balance-card>p{font-size:10px;color:#ac9371;margin-top:10px;position:relative;z-index:1}.balance-footer{display:flex;justify-content:space-between;margin-top:26px;font-size:9px;color:#a48b66;gap:10px;position:relative;z-index:1}.balance-decoration{position:absolute;right:-35px;bottom:15px;width:200px;height:130px;fill:none;stroke:#d3bd95;stroke-width:1;opacity:.4}.balance-decoration path{stroke-width:2}.finance-metrics{display:grid;grid-template-columns:1fr 1fr;gap:14px}.metric-card{background:var(--paper);border:1px solid var(--line);border-radius:12px;padding:19px 22px;min-width:0}.metric-card>div{display:flex;justify-content:space-between;align-items:center;gap:10px;color:#b39b7a;font-size:11px}.metric-card strong{display:block;font-size:24px;font-weight:500;color:#917449;letter-spacing:-.025em;margin-top:12px;overflow-wrap:anywhere}.metric-card.olive strong{color:#829171}.metric-card.clay strong{color:#af846e}.metric-card small{font-size:9px;color:#b7a58d;display:block;margin-top:8px}
.finance-main-grid{display:grid;grid-template-columns:1.8fr 1fr;gap:18px}.finance-panel{border:1px solid var(--line);border-radius:14px;background:var(--paper);padding:22px;min-width:0}.panel-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.panel-heading h2{font-size:14px;font-weight:600;color:#7d6546}.panel-heading p{font-size:10px;color:#b6a38a;margin-top:7px}.panel-heading>svg{color:#bba582}.panel-tag{font-size:9px;color:#b9a17e;padding:5px 8px;border-radius:4px;background:#f6f0e5;white-space:nowrap}.trend-legend{display:flex;gap:20px;flex-wrap:wrap;margin-top:25px;font-size:10px;color:#a79377}.trend-legend>span{display:flex;align-items:center;gap:6px}.trend-legend i{width:7px;height:7px;border-radius:2px}.income{background:#95a383}.purchase{background:#bd9967}.maintenance{background:#c1927e}.finance-trend-chart{width:100%;height:240px;display:block;margin-top:8px}.axis-label{font-size:10px;fill:#b29d7e}.trend-footer{display:flex;justify-content:space-between;gap:10px;font-size:9px;color:#b5a28a;border-top:1px solid #f0e8db;padding-top:13px}.expense-body{display:flex;justify-content:center;align-items:center;padding:22px 0}.expense-ring{width:175px;height:175px;border-radius:50%;display:grid;place-items:center;position:relative}.expense-ring>div{width:145px;height:145px;background:var(--paper);border-radius:50%;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:10px}.expense-ring small{font-size:10px;color:#b19d80}.expense-ring strong{font-size:21px;font-weight:500;color:#8e7048;letter-spacing:-.04em}.expense-item{display:flex;align-items:center;justify-content:space-between;font-size:11px;padding:12px 0;border-top:1px solid #f0e9dd}.expense-item span{display:flex;align-items:center;gap:8px;color:#a38b69}.expense-item i{width:6px;height:6px;border-radius:50%;background:#b99562}.maintenance-item i{background:#c49480}.expense-item strong{font-weight:500;color:#8e7451}.subtle-note{font-size:10px;color:#b7a58c;text-align:center;margin-top:10px}
.finance-health{border:1px solid var(--line);border-radius:13px;padding:20px 23px;background:#f6f1e8}.health-heading{display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:20px}.health-heading>span{font-size:12px;font-weight:500;color:#927955}.health-heading>small{font-size:9px;color:#b7a38a}.health-items{display:grid;grid-template-columns:repeat(3,1fr);gap:20px}.health-items article{display:flex;gap:15px;align-items:flex-start;border-right:1px solid #e5dac7}.health-items article:last-child{border:0}.health-items article>svg{color:#b6a07d;margin-top:3px}.health-items article>div{display:flex;flex-direction:column;gap:8px}.health-items span{font-size:10px;color:#a08a69}.health-items strong{font-size:25px;font-weight:500;color:#927149}.health-items small{font-size:9px;color:#b09c80;font-weight:400}
.finance-ranking-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.ranking-list{margin-top:24px;max-height:340px;overflow:auto}.ranking-row{display:flex;align-items:center;gap:14px;margin:0 0 23px}.rank-no{color:#ccbda6;font-size:11px;min-width:17px}.ranking-content{flex:1;min-width:0}.ranking-content>div:first-child{display:flex;justify-content:space-between;gap:12px;font-size:11px}.ranking-content span{color:#998264;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.ranking-content strong{font-weight:500;color:#8f7047;white-space:nowrap}.ranking-track{height:4px;border-radius:4px;background:#f1eadf;margin-top:11px;overflow:hidden}.ranking-track i{display:block;height:100%;background:#c0a071;border-radius:4px}.ranking-track i.secondary{background:#a5aa90}.finance-empty{padding:60px 15px;text-align:center;color:#baa88e;font-size:12px}.finance-links{display:flex;align-items:center;gap:10px;flex-wrap:wrap;padding:5px 0}.finance-links>span{font-size:10px;color:#b7a48a;margin-right:6px}.finance-links button{display:flex;align-items:center;gap:8px;padding:10px 13px;background:#fffdf9;border:1px solid var(--line);border-radius:7px;font-size:11px;color:#a1855e;transition:background .15s}.finance-links button:hover{background:#f2e8d6}.finance-footnote{font-size:10px;color:#b4a28b;border-top:1px solid var(--line);padding-top:16px;line-height:1.8}.spinning{animation:spin 1s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:1150px){.finance-header{flex-wrap:wrap}.finance-top{grid-template-columns:1fr 1.3fr}.balance-card{padding:22px}.metric-card{padding:16px}.metric-card strong{font-size:20px}h1 span{display:none}.finance-main-grid{grid-template-columns:1.5fr 1fr}}
@media(max-width:750px){.finance-top,.finance-main-grid,.finance-ranking-grid{grid-template-columns:1fr}.balance-card{min-height:220px}.finance-header{gap:18px}h1{font-size:25px}.finance-controls{width:100%}.finance-controls input{flex:1;min-width:100px}.finance-controls button{padding:10px}.health-items{grid-template-columns:1fr;gap:17px}.health-items article{border-right:0;border-bottom:1px solid #e8ddcb;padding-bottom:17px}.health-items article:last-child{padding:0}.health-heading{align-items:start;flex-direction:column;gap:6px}.finance-panel{padding:18px}.finance-page{gap:17px}.balance-number{font-size:34px}}
@media(prefers-reduced-motion:reduce){.spinning{animation:none}}
.trend-empty{min-height:240px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;background:linear-gradient(#fffdf9,#faf6ee);margin:15px 0 0;border-radius:9px}.trend-empty svg{color:#c8b596}.trend-empty strong{font-size:12px;font-weight:500;color:#ad9472}.trend-empty span{font-size:10px;line-height:1.8;max-width:350px}.finance-ranking-grid{align-items:start}
</style>

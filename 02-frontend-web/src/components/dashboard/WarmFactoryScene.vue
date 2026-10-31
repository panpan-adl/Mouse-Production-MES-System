<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RotateCcw, ZoomIn, ZoomOut } from 'lucide-vue-next'
import type { DashboardData } from '@/mock/dashboard'
const props = defineProps<{ data: DashboardData; activeNode: string }>()
const emit = defineEmits<{ select: [key: string] }>()
const zoom = ref(1)
const zones = [
  { id: 'raw', name: '原料仓储', subtitle: '物料准备', x: 60, y: 60, w: 145, d: 105, color: '#bc9367', node: 'bom', type: 'warehouse' },
  { id: 'electronic', name: '电子装配', subtitle: 'PCBA / 元件装配', x: 245, y: 45, w: 185, d: 110, color: '#c98d5a', node: 'work', type: 'line' },
  { id: 'assembly', name: '结构总装', subtitle: '壳体 / 按键 / 滚轮', x: 465, y: 45, w: 180, d: 110, color: '#b27a54', node: 'work', type: 'line' },
  { id: 'quality', name: '质量检测', subtitle: '功能与外观检验', x: 245, y: 210, w: 185, d: 110, color: '#8c9b7b', node: 'qc', type: 'inspection' },
  { id: 'pack', name: '贴标包装', subtitle: '包装 / 赋码', x: 465, y: 210, w: 180, d: 110, color: '#c2a363', node: 'stock', type: 'line' },
  { id: 'finished', name: '成品仓储', subtitle: '入库与交付', x: 685, y: 145, w: 120, d: 140, color: '#a08f72', node: 'stock', type: 'warehouse' },
]
const selectedZone = ref('')
watch(()=>props.activeNode,key=>{if(!key)selectedZone.value=''})
const selected = computed(()=>zones.find(zone=>zone.id===selectedZone.value))
const nodeInfo = computed(()=>props.data.flowNodes.find(node=>node.key===selected.value?.node))
const project = (x:number,y:number,z=0)=>`${450+(x-y)*.72},${100+(x+y)*.36-z}`
const face = (x:number,y:number,w:number,d:number,z=0)=>[project(x,y,z),project(x+w,y,z),project(x+w,y+d,z),project(x,y+d,z)].join(' ')
function selectZone(zone:typeof zones[number]){
  const next=selectedZone.value===zone.id?'':zone.id
  selectedZone.value=next;emit('select',next?zone.node:'')
}
const flowPaths = [
  [205,110,245,100], [430,100,465,100], [555,155,555,183],
  [335,155,335,210], [430,265,465,265], [645,265,685,225],
]
</script>

<template>
  <section class="factory-scene">
    <header><div><span class="scene-kicker">MOUSE MES · FACTORY OVERVIEW</span><h2>鼠标智造 · 车间全景</h2></div><span class="scene-mode"><i></i>业务数据联动</span></header>
    <div class="scene-canvas">
      <div class="scene-watermark">MOUSE<br /><span>SMART FACTORY</span></div>
      <svg viewBox="180 75 900 480" role="img" aria-label="鼠标工厂三维示意，展示原料仓、电子装配、结构总装、质量检测、包装与成品仓。点击区域筛选业务阶段。">
        <defs>
          <linearGradient id="factory-floor" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#f9f6ed"/><stop offset="1" stop-color="#e9dfcb"/></linearGradient>
          <filter id="factory-shadow" x="-30%" y="-30%" width="170%" height="170%"><feDropShadow dx="0" dy="14" stdDeviation="13" flood-color="#8d7351" flood-opacity=".17"/></filter>
          <marker id="factory-arrow" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto"><path d="M0 0 L6 3 L0 6" fill="none" stroke="#b78d50" stroke-width="1.2"/></marker>
        </defs>
        <g :transform="`translate(630 300) scale(${zoom}) translate(-630 -300)`">
          <g filter="url(#factory-shadow)">
            <polygon :points="face(10,0,840,375,-16)" fill="#d0bea0" />
            <polygon :points="[project(10,375),project(850,375),project(850,375,-16),project(10,375,-16)].join(' ')" fill="#c5af8b" />
            <polygon :points="[project(850,0),project(850,375),project(850,375,-16),project(850,0,-16)].join(' ')" fill="#bca481" />
            <polygon :points="face(10,0,840,375)" fill="url(#factory-floor)" stroke="#fffaf0" stroke-width="2" />
          </g>
          <g opacity=".35" stroke="#d1c4ae" stroke-width=".6"><path v-for="i in 16" :key="`x${i}`" :d="`M${project(i*50,10)} L${project(i*50,365)}`"/><path v-for="i in 7" :key="`y${i}`" :d="`M${project(20,i*50)} L${project(840,i*50)}`"/></g>
          <path :d="`M${project(210,180)} L${project(820,180)}`" stroke="#dac9a5" stroke-width="16" opacity=".65" />
          <path :d="`M${project(210,180)} L${project(820,180)}`" stroke="#fff9e9" stroke-width="1" stroke-dasharray="9 8" />
          <path v-for="(p,i) in flowPaths" :key="i" :d="`M${project(p[0],p[1],3)} L${project(p[2],p[3],3)}`" class="material-route" marker-end="url(#factory-arrow)" />
          <g v-for="(zone,index) in zones" :key="zone.id" role="button" tabindex="0" :aria-label="`${zone.name}，点击筛选相关工单`" :aria-pressed="selectedZone===zone.id" class="factory-zone" :class="{selected:selectedZone===zone.id}" @click="selectZone(zone)" @keydown.enter.prevent="selectZone(zone)" @keydown.space.prevent="selectZone(zone)">
            <polygon :points="face(zone.x-7,zone.y-7,zone.w+14,zone.d+14,1)" :fill="zone.color" fill-opacity=".13" :stroke="zone.color" stroke-width="1" stroke-dasharray="4 3" class="zone-outline" />
            <polygon :points="face(zone.x,zone.y,zone.w,zone.d,4)" fill="#faf7ee" stroke="#d4c5a9" />
            <template v-if="zone.type==='warehouse'">
              <g v-for="r in 3" :key="r">
                <polygon :points="face(zone.x+12,zone.y+12+(r-1)*30,zone.w-25,18,30)" :fill="zone.color" />
                <polygon :points="[project(zone.x+12,zone.y+30+(r-1)*30,30),project(zone.x+zone.w-13,zone.y+30+(r-1)*30,30),project(zone.x+zone.w-13,zone.y+30+(r-1)*30,6),project(zone.x+12,zone.y+30+(r-1)*30,6)].join(' ')" fill="#b59973" />
                <path v-for="p in 4" :key="p" :d="`M${project(zone.x+12+p*(zone.w-25)/5,zone.y+30+(r-1)*30,30)} L${project(zone.x+12+p*(zone.w-25)/5,zone.y+30+(r-1)*30,6)}`" stroke="#f1dfbe" stroke-width="2" />
              </g>
            </template>
            <template v-else>
              <polygon :points="face(zone.x+18,zone.y+42,zone.w-36,27,14)" fill="#b9b4a7" stroke="#99988b" />
              <g v-for="m in 3" :key="m">
                <polygon :points="face(zone.x+20+(m-1)*48,zone.y+15,34,35,39)" fill="#fffdf7" stroke="#c9c1af" />
                <polygon :points="[project(zone.x+20+(m-1)*48,zone.y+50,39),project(zone.x+54+(m-1)*48,zone.y+50,39),project(zone.x+54+(m-1)*48,zone.y+50,14),project(zone.x+20+(m-1)*48,zone.y+50,14)].join(' ')" fill="#ded9cb" stroke="#bfb7a5" />
                <polygon :points="[project(zone.x+54+(m-1)*48,zone.y+15,39),project(zone.x+54+(m-1)*48,zone.y+50,39),project(zone.x+54+(m-1)*48,zone.y+50,14),project(zone.x+54+(m-1)*48,zone.y+15,14)].join(' ')" fill="#c9c2b2" />
                <polygon :points="face(zone.x+26+(m-1)*48,zone.y+24,21,18,40)" :fill="zone.type==='inspection'?'#a0b7a1':zone.color" opacity=".8" />
                <circle :cx="450+(zone.x+37+(m-1)*48-zone.y-37)*.72" :cy="100+(zone.x+37+(m-1)*48+zone.y+37)*.36-44" r="2.5" fill="#8caa85" />
                <polygon :points="face(zone.x+30+(m-1)*48,zone.y+49,14,14,17)" fill="#aa8d65" stroke="#f6ead7" />
              </g>
              <path :d="`M${project(zone.x+20,zone.y+84,5)} L${project(zone.x+zone.w-20,zone.y+84,5)}`" :stroke="zone.color" stroke-width="3" stroke-dasharray="6 5" />
            </template>
            <g :transform="`translate(${project(zone.x+zone.w/2,zone.y+zone.d+8,0).replace(',', ' ')})`">
              <rect x="-49" y="0" width="98" height="34" rx="5" fill="#fffdf7" stroke="#e3d8c5" />
              <text text-anchor="middle" y="14" class="zone-label">{{zone.name}}</text><text text-anchor="middle" y="27" class="zone-number">{{String(index+1).padStart(2,'0')}} · {{zone.subtitle}}</text>
            </g>
          </g>
          <g :transform="`translate(${project(55,310,0).replace(',', ' ')})`"><path d="M0 0 v-34 M-5 -28 l5 -6 5 6" stroke="#aa916d" fill="none"/><text x="-4" y="14" font-size="10" fill="#aa916d">N</text></g>
        </g>
      </svg>
      <div class="scene-controls"><button aria-label="缩小车间" :disabled="zoom<=.8" @click="zoom=Math.max(.8,zoom-.1)"><ZoomOut :size="16"/></button><button aria-label="重置车间视图" @click="zoom=1"><RotateCcw :size="15"/></button><button aria-label="放大车间" :disabled="zoom>=1.4" @click="zoom=Math.min(1.4,zoom+.1)"><ZoomIn :size="16"/></button></div>
      <div class="scene-legend"><i></i>装配区域 <i class="olive"></i>检验区域 <i class="sand"></i>仓储区域</div>
    </div>
    <footer>
      <div v-if="selected"><strong>{{selected.name}}</strong><span>{{nodeInfo?`${nodeInfo.name} · ${nodeInfo.count} 单 · 完成率 ${nodeInfo.completionRate}%`:'暂无对应阶段统计'}}</span><button @click="selectedZone='';emit('select','')">清除筛选 ×</button></div>
      <div v-else><strong>全流程协同</strong><span>原料 → 装配 → 检验 → 包装 → 交付</span></div>
      <small>车间布局示意 · 点击区域查看阶段工单</small>
    </footer>
  </section>
</template>

<style scoped>
.factory-scene{background:linear-gradient(145deg,#fffefb,#f6f0e4);border:1px solid #e5dac8;border-radius:16px;overflow:hidden;min-width:0;display:flex;flex-direction:column}
header{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:23px 24px 0;z-index:1}.scene-kicker{font-size:9px;letter-spacing:.18em;color:#b39b7d}h2{font-size:19px;letter-spacing:.025em;font-weight:600;color:#554735;margin-top:7px}.scene-mode{font-size:10px;color:#8e866f;display:flex;align-items:center;gap:6px;white-space:nowrap}.scene-mode i{width:5px;height:5px;background:#8b9b79;border-radius:50%}
.scene-canvas{position:relative;flex:1;min-height:320px;display:flex;align-items:center;overflow:hidden}.scene-canvas>svg{width:100%;position:relative;z-index:1;min-height:300px}.scene-watermark{position:absolute;top:20px;left:24px;color:#e9dfcc;font-size:50px;line-height:.8;font-weight:700;letter-spacing:-.05em}.scene-watermark span{font-size:10px;letter-spacing:.3em;font-weight:400}
.factory-zone{cursor:pointer;outline:none}.factory-zone:hover .zone-outline,.factory-zone:focus-visible .zone-outline,.factory-zone.selected .zone-outline{stroke:#a96a35;stroke-width:2.5;fill-opacity:.28;stroke-dasharray:none}.zone-label{font-size:11px;fill:#65533c;font-weight:600}.zone-number{font-size:6.5px;fill:#a19077}.material-route{fill:none;stroke:#b78d50;stroke-width:2;stroke-dasharray:4 4;animation:material-flow 3s linear infinite}.scene-controls{position:absolute;right:16px;bottom:16px;display:flex;gap:1px;border:1px solid #e6ddcd;border-radius:7px;background:#fffdf8;z-index:2}.scene-controls button{padding:8px;color:#9c886b}.scene-controls button:hover{background:#f2e7d4}.scene-controls button:disabled{opacity:.3}.scene-legend{position:absolute;bottom:20px;left:24px;font-size:9px;color:#9b8e78;display:flex;gap:7px;align-items:center;z-index:2}.scene-legend i{width:7px;height:7px;background:#c38e61;border-radius:2px}.scene-legend .olive{background:#8c9b7b}.scene-legend .sand{background:#bca885}
footer{border-top:1px solid #e7ddcb;padding:15px 22px;display:flex;flex-wrap:wrap;justify-content:space-between;gap:8px;background:#fffdf780;font-size:10px;color:#9d8f78}footer>div{display:flex;gap:12px;flex-wrap:wrap;align-items:center}footer strong{color:#7c6548;font-weight:600}footer button{color:#ad7846}footer small{font-size:9px}
@keyframes material-flow{to{stroke-dashoffset:-24}}@media(prefers-reduced-motion:reduce){.material-route{animation:none}}@media(max-width:700px){header{padding:18px 16px 0}.scene-mode{display:none}.scene-legend{left:12px;font-size:8px;gap:4px}.scene-canvas{min-height:260px}.scene-canvas>svg{min-height:260px}footer{padding:14px}}
</style>

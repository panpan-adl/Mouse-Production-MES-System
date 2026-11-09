<script setup lang="ts">
import {computed} from 'vue'
interface Fact {source:string;target:string;relation:string;quote:string;chunkId:string}
const props=defineProps<{facts:Fact[];highlighted:Fact[];selected:string;zoom:number;removed?:Fact[];entityTypes?:Record<string,string>}>()
const emit=defineEmits<{node:[name:string];edge:[fact:Fact]}>()
const marker='kg-'+crypto.randomUUID()
const key=(f:Fact)=>JSON.stringify([f.source,f.relation,f.target,f.chunkId])
const removed=computed(()=>new Set((props.removed||[]).map(key)))
const typeNames:Record<string,string>={EQUIPMENT:'设备',PROCESS:'工序',PRODUCT:'产品',PERSON:'人员',INSPECTION:'检验',OTHER:'其他'}
const colors:Record<string,string>={EQUIPMENT:'#dff1ff',PROCESS:'#e2f4e7',PRODUCT:'#f0e4fa',PERSON:'#fff1d4',INSPECTION:'#fce4e6',OTHER:'#e3f2f0'}
const hot=computed(()=>new Set(props.highlighted.map(key)))
const hotNodes=computed(()=>new Set(props.highlighted.flatMap(f=>[f.source,f.target])))
const points=computed(()=>{
  const names=[...new Set(props.facts.flatMap(f=>[f.source,f.target]))]
  const nodes=names.map((name,i)=>({name,x:450+240*Math.cos(i*2*Math.PI/names.length),y:300+190*Math.sin(i*2*Math.PI/names.length)}))
  const map=new Map(nodes.map(n=>[n.name,n]))
  // Deterministic spacing: no animation or random movement while reading evidence.
  for(let step=0;step<90;step++){
    const movement=nodes.map(()=>({x:0,y:0}))
    for(let i=0;i<nodes.length;i++)for(let j=i+1;j<nodes.length;j++){
      const dx=nodes[i].x-nodes[j].x,dy=nodes[i].y-nodes[j].y,d=Math.max(1,Math.hypot(dx,dy)),force=Math.min(8,1800/(d*d))
      movement[i].x+=dx/d*force;movement[i].y+=dy/d*force;movement[j].x-=dx/d*force;movement[j].y-=dy/d*force
    }
    for(const f of props.facts){const a=map.get(f.source)!,b=map.get(f.target)!,dx=b.x-a.x,dy=b.y-a.y,d=Math.max(1,Math.hypot(dx,dy)),force=(d-150)*.012
      const ai=names.indexOf(a.name),bi=names.indexOf(b.name);movement[ai].x+=dx/d*force;movement[ai].y+=dy/d*force;movement[bi].x-=dx/d*force;movement[bi].y-=dy/d*force}
    nodes.forEach((n,i)=>{n.x=Math.max(70,Math.min(830,n.x+movement[i].x));n.y=Math.max(60,Math.min(520,n.y+movement[i].y))})
  }
  return map
})
const edges=computed(()=>{
  const groups=new Map<string,Fact[]>()
  for(const f of props.facts){const pair=JSON.stringify([f.source,f.target].sort());groups.set(pair,[...(groups.get(pair)||[]),f])}
  return props.facts.map((f,i)=>{
    const a=points.value.get(f.source)!,b=points.value.get(f.target)!,group=groups.get(JSON.stringify([f.source,f.target].sort()))!,lane=group.indexOf(f)-(group.length-1)/2
    const dx=b.x-a.x,dy=b.y-a.y,d=Math.max(1,Math.hypot(dx,dy)),sign=f.source<f.target?1:-1,bend=lane*48*sign
    const cx=(a.x+b.x)/2-dy/d*bend,cy=(a.y+b.y)/2+dx/d*bend
    const path=f.source===f.target?`M ${a.x-14} ${a.y-20} C ${a.x-90} ${a.y-120} ${a.x+90} ${a.y-120} ${a.x+14} ${a.y-20}`:`M ${a.x+dx/d*25} ${a.y+dy/d*25} Q ${cx} ${cy} ${b.x-dx/d*30} ${b.y-dy/d*30}`
    return {fact:f,id:i,path,x:f.source===f.target?a.x:(a.x+2*cx+b.x)/4,y:f.source===f.target?a.y-85:(a.y+2*cy+b.y)/4,hot:hot.value.has(key(f))}
  })
})
</script>
<template>
<div class="graph-canvas"><svg viewBox="0 0 900 600" role="img" aria-label="私有资料知识图谱">
  <defs><marker :id="marker" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="context-stroke" /></marker></defs>
  <g :transform="`translate(450 300) scale(${zoom}) translate(-450 -300)`">
    <g v-for="e in edges" :key="e.id" class="graph-edge" :class="{highlighted:e.hot,removed:removed.has(key(e.fact))}" role="button" tabindex="0" :aria-label="`查看关系：${e.fact.source} ${e.fact.relation} ${e.fact.target}`" @click="emit('edge',e.fact)" @keydown.enter="emit('edge',e.fact)" @keydown.space.prevent="emit('edge',e.fact)">
      <path :d="e.path" class="edge-hit" /><path :d="e.path" class="edge-line" :marker-end="`url(#${marker})`" />
      <rect :x="e.x-Math.min(130,e.fact.relation.length*12+16)/2" :y="e.y-10" :width="Math.min(130,e.fact.relation.length*12+16)" height="20" rx="5" />
      <text :x="e.x" :y="e.y+4">{{e.fact.relation.length>10?e.fact.relation.slice(0,10)+'…':e.fact.relation}}</text><title>{{e.fact.source}} → {{e.fact.relation}} → {{e.fact.target}}</title>
    </g>
    <g v-for="n in [...points.values()]" :key="n.name" class="graph-node" :class="{chosen:selected===n.name,highlighted:hotNodes.has(n.name)}" role="button" tabindex="0" :aria-label="`查看${n.name}的关系`" @click="emit('node',n.name)" @keydown.enter="emit('node',n.name)" @keydown.space.prevent="emit('node',n.name)">
      <circle :cx="n.x" :cy="n.y" r="24" :style="!hotNodes.has(n.name)&&entityTypes?.[n.name]?{fill:colors[entityTypes[n.name]]}:{}" /><text :x="n.x" :y="n.y+42">{{n.name.length>14?n.name.slice(0,14)+'…':n.name}}</text><title>{{n.name}} · {{typeNames[entityTypes?.[n.name]||'OTHER']}}</title>
    </g>
  </g>
</svg></div>
</template>
<style scoped>
.graph-canvas{overflow:hidden;background:#fafdfd;border-block:1px solid #edf2f4}.graph-canvas svg{display:block;width:100%;min-height:300px}.graph-edge,.graph-node{cursor:pointer;outline:none}.graph-edge.removed .edge-line{stroke:#c04e43;stroke-dasharray:6 4;stroke-width:2.5}.graph-edge.removed rect{fill:#ffe5df;stroke:#c04e43}.edge-line{fill:none;stroke:#9eb8c4;stroke-width:1.6}.edge-hit{fill:none;stroke:transparent;stroke-width:18}.graph-edge rect{fill:#fff;stroke:#e0eaee}.graph-edge text{font-size:11px;fill:#527380;text-anchor:middle}.graph-node circle{fill:#e3f2f0;stroke:#62a6a0;stroke-width:1.6}.graph-node text{font-size:12px;fill:#365d68;text-anchor:middle;paint-order:stroke;stroke:#fff;stroke-width:3px}.graph-node.chosen circle,.graph-node:focus circle{stroke:#e69b32;stroke-width:3}.highlighted .graph-edge.removed .edge-line{stroke:#c04e43;stroke-dasharray:6 4;stroke-width:2.5}.graph-edge.removed rect{fill:#ffe5df;stroke:#c04e43}.edge-line{stroke:#d88b22;stroke-width:3}.graph-edge.highlighted rect{fill:#fff3d8;stroke:#d8a246}.graph-node.highlighted circle{fill:#fff0cc;stroke:#d88b22}.graph-edge:hover .edge-line,.graph-edge:focus .graph-edge.removed .edge-line{stroke:#c04e43;stroke-dasharray:6 4;stroke-width:2.5}.graph-edge.removed rect{fill:#ffe5df;stroke:#c04e43}.edge-line{stroke:#0b716d;stroke-width:3}@media(max-width:720px){.graph-canvas svg{min-height:260px}}
</style>

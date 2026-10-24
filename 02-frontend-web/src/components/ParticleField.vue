<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
const props=withDefaults(defineProps<{active?:boolean;density?:number}>(),{active:true,density:95})
const canvas=ref<HTMLCanvasElement|null>(null)
let ctx:CanvasRenderingContext2D|null=null,observer:ResizeObserver|undefined,frame=0,last=0,width=0,height=0,reduced:MediaQueryList
let pointer={x:-1000,y:-1000}
type Dot={x:number;y:number;vx:number;vy:number;r:number;phase:number;violet:boolean}
let dots:Dot[]=[]
function resize(){
  const el=canvas.value;if(!el)return
  const box=el.getBoundingClientRect();width=box.width;height=box.height
  const ratio=Math.min(devicePixelRatio||1,1.5);el.width=Math.round(width*ratio);el.height=Math.round(height*ratio)
  ctx=el.getContext('2d');ctx?.setTransform(ratio,0,0,ratio,0,0)
  const count=Math.min(props.density,width<650?38:160,Math.round(width*height/10500))
  dots=Array.from({length:Math.max(15,count)},()=>({x:Math.random()*width,y:Math.random()*height,vx:(Math.random()-.5)*.3,vy:-.15-Math.random()*.25,r:Math.random()*1.6+.4,phase:Math.random()*6.28,violet:Math.random()>.72}))
  render(performance.now(),false)
}
function render(time:number,move:boolean){
  if(!ctx||!width)return
  ctx.clearRect(0,0,width,height)
  for(let i=0;i<dots.length;i++){
    const p=dots[i]
    if(move){p.x+=p.vx;p.y+=p.vy;if(p.y<0)p.y=height;if(p.x<0)p.x=width;if(p.x>width)p.x=0}
    const alpha=.28+(Math.sin(time*.0007+p.phase)+1)*.2
    ctx.fillStyle=p.violet?`rgba(117,123,209,${alpha})`:`rgba(38,137,200,${alpha})`
    ctx.beginPath();ctx.arc(p.x,p.y,p.r,0,Math.PI*2);ctx.fill()
    for(let j=i+1;j<dots.length;j++){
      const q=dots[j],distance=(p.x-q.x)**2+(p.y-q.y)**2
      if(distance<125**2){ctx.strokeStyle=`rgba(49,135,200,${(1-distance/15625)*.2})`;ctx.lineWidth=.65;ctx.beginPath();ctx.moveTo(p.x,p.y);ctx.lineTo(q.x,q.y);ctx.stroke()}
    }
    const distance=(p.x-pointer.x)**2+(p.y-pointer.y)**2
    if(distance<180**2){ctx.strokeStyle=`rgba(37,150,211,${(1-distance/32400)*.32})`;ctx.beginPath();ctx.moveTo(p.x,p.y);ctx.lineTo(pointer.x,pointer.y);ctx.stroke()}
  }
}
function tick(time:number){if(time-last>32){render(time,true);last=time}frame=requestAnimationFrame(tick)}
function sync(){cancelAnimationFrame(frame);frame=0;if(props.active&&!document.hidden&&!reduced?.matches)frame=requestAnimationFrame(tick);else render(performance.now(),false)}
function track(e:PointerEvent){const box=canvas.value?.getBoundingClientRect();if(box)pointer={x:e.clientX-box.left,y:e.clientY-box.top}}
function leave(){pointer={x:-1000,y:-1000}}
onMounted(()=>{reduced=matchMedia('(prefers-reduced-motion: reduce)');observer=new ResizeObserver(resize);if(canvas.value)observer.observe(canvas.value);document.addEventListener('visibilitychange',sync);window.addEventListener('pointermove',track,{passive:true});window.addEventListener('pointerout',leave,{passive:true});reduced.addEventListener('change',sync);resize();sync()})
watch(()=>props.active,sync)
onBeforeUnmount(()=>{cancelAnimationFrame(frame);observer?.disconnect();document.removeEventListener('visibilitychange',sync);window.removeEventListener('pointermove',track);window.removeEventListener('pointerout',leave);reduced?.removeEventListener('change',sync)})
</script>
<template><canvas ref="canvas" class="particle-field" aria-hidden="true"></canvas></template>
<style scoped>.particle-field{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}</style>

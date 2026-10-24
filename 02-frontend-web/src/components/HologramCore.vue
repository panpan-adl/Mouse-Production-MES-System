<script setup lang="ts">
import {Cpu,Workflow,ScanLine,Boxes} from 'lucide-vue-next'
</script>
<template>
  <div class="holo-scene" aria-hidden="true">
    <div class="holo-grid"></div><div class="holo-aura"></div>
    <div class="holo-orbit orbit-one"></div><div class="holo-orbit orbit-two"></div><div class="holo-orbit orbit-three"></div>
    <svg class="holo-radar" viewBox="0 0 500 500" fill="none"><circle cx="250" cy="250" r="207" stroke="currentColor" stroke-dasharray="1 10" stroke-width="3"/><circle cx="250" cy="250" r="186" stroke="currentColor" stroke-opacity=".15"/><path d="M250 15V55M250 445V485M15 250H55M445 250H485" stroke="currentColor" stroke-opacity=".7"/><path d="M88 88L106 106M394 394L412 412M88 412L106 394M394 106L412 88" stroke="currentColor"/><circle cx="250" cy="250" r="139" stroke="currentColor" stroke-dasharray="80 13 4 13" stroke-opacity=".5"/><path d="M250 76L400 162V337L250 424L100 337V162L250 76Z" stroke="currentColor" stroke-opacity=".13"/></svg>
    <div class="holo-core"><span class="core-cross cross-top"></span><Cpu :size="44"/><strong>MES</strong><small>DIGITAL CORE</small><span class="core-cross cross-bottom"></span></div>
    <div class="holo-satellite satellite-plan"><Workflow :size="20"/><div><b>智能排产</b><small>PLAN & SCHEDULE</small></div><span></span></div>
    <div class="holo-satellite satellite-quality"><ScanLine :size="20"/><div><b>质量追溯</b><small>QUALITY & TRACE</small></div><span></span></div>
    <div class="holo-satellite satellite-stock"><Boxes :size="20"/><div><b>物料协同</b><small>MATERIAL FLOW</small></div><span></span></div>
    <div class="holo-coordinates coord-a">X / PLAN<br/>Y / EXECUTE<br/>Z / TRACE</div><div class="holo-coordinates coord-b">MOUSE INDUSTRIAL<br/><span>CONNECTED MANUFACTURING</span></div>
    <div class="holo-legend"><i></i><span>数字化制造 · 全流程协同</span><div><b></b><b></b><b></b><b></b><b></b></div></div>
  </div>
</template>

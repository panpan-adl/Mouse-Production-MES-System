<script setup lang="ts">
defineProps<{thinking?:boolean}>()
const uid='mesbot-'+Math.random().toString(36).slice(2,10)
</script>
<template><svg viewBox="0 0 160 180" class="mes-robot" :class="{thinking}" fill="none" aria-hidden="true">
 <defs><linearGradient :id="uid" x1="36" y1="20" x2="132" y2="158" gradientUnits="userSpaceOnUse"><stop stop-color="#f1f7fa"/><stop offset=".4" stop-color="#cbdde5"/><stop offset="1" stop-color="#91b2c5"/></linearGradient></defs>
 <ellipse class="robot-shadow" cx="81" cy="168" rx="35" ry="6" fill="#829ba9" opacity=".16"/>
 <g class="robot-float"><path d="M80 37V24" stroke="#93afbf" stroke-width="4" stroke-linecap="round"/><circle cx="80" cy="20" r="6" fill="#d5e6ed" stroke="#819eaf" stroke-width="2"/><circle cx="78" cy="18" r="2" fill="white"/>
 <circle cx="47" cy="48" r="21" :fill="'url(#'+uid+')'" stroke="#9ab5c5" stroke-width="2"/><circle cx="114" cy="48" r="21" :fill="'url(#'+uid+')'" stroke="#9ab5c5" stroke-width="2"/><circle cx="47" cy="47" r="11" fill="#c7dce6"/><circle cx="114" cy="47" r="11" fill="#c7dce6"/>
 <g class="robot-arm"><path d="M111 127C127 123 134 113 132 106" stroke="#a0bdcd" stroke-width="13" stroke-linecap="round"/><path d="M130 107L132 101M134 107L138 103" stroke="#eaf3f7" stroke-width="4" stroke-linecap="round"/></g><path d="M48 129C40 137 42 144 48 145" stroke="#a0bdcd" stroke-width="12" stroke-linecap="round"/>
 <rect x="58" y="139" width="17" height="23" rx="8" fill="#94b3c6" stroke="#7d9cad" stroke-width="1.5"/><rect x="87" y="139" width="17" height="23" rx="8" fill="#94b3c6" stroke="#7d9cad" stroke-width="1.5"/>
 <rect x="51" y="112" width="61" height="40" rx="18" :fill="'url(#'+uid+')'" stroke="#93afbf" stroke-width="2"/><path d="M70 129L75 134L70 139M81 139H92" stroke="#f5fafc" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
 <rect x="28" y="41" width="105" height="80" rx="34" :fill="'url(#'+uid+')'" stroke="#8eacbd" stroke-width="2"/><path d="M44 56C52 48 66 46 81 47" stroke="white" stroke-opacity=".8" stroke-width="4" stroke-linecap="round"/>
 <rect x="39" y="65" width="83" height="46" rx="19" fill="#344d5d" stroke="#aec8d6" stroke-width="2"/><path d="M46 78C49 72 57 70 67 70" stroke="#e1eef4" stroke-opacity=".16" stroke-width="2" stroke-linecap="round"/>
 <g class="robot-eyes"><rect x="53" y="81" width="11" height="15" rx="5.5" fill="#d9f0f5"/><rect x="96" y="81" width="11" height="15" rx="5.5" fill="#d9f0f5"/></g><path d="M75 98Q80 102 85 98" stroke="#e0f1f6" stroke-width="2.5" stroke-linecap="round"/><circle cx="49" cy="100" r="3" fill="#9bbacb" opacity=".5"/><circle cx="112" cy="100" r="3" fill="#9bbacb" opacity=".5"/>
 <path class="robot-spark" d="M144 56V66M139 61H149M18 91V97M15 94H21" stroke="#bdd4df" stroke-width="2" stroke-linecap="round"/></g>
</svg></template>
<style scoped>
.mes-robot{display:block;width:100%;height:100%;overflow:visible;filter:drop-shadow(0 5px 5px #417eb71a)}.robot-float{animation:robot-hover 4s ease-in-out infinite;transform-origin:80px 100px}.robot-eyes{animation:robot-blink 6s infinite;transform-origin:80px 88px}.robot-arm{transform-origin:113px 128px;animation:robot-wave 8s ease-in-out infinite}.robot-shadow{animation:robot-shadow 4s ease-in-out infinite;transform-origin:80px 168px}.robot-spark{animation:robot-shine 3s ease-in-out infinite}.thinking .robot-eyes{animation:robot-think .8s ease-in-out infinite alternate}.thinking .robot-float{animation-duration:1.8s}@keyframes robot-hover{50%{transform:translateY(-7px) rotate(2deg)}}@keyframes robot-blink{0%,43%,47%,100%{transform:scaleY(1)}45%{transform:scaleY(.12)}}@keyframes robot-wave{0%,12%,24%,100%{transform:rotate(0)}15%,21%{transform:rotate(-15deg)}18%{transform:rotate(7deg)}}@keyframes robot-shadow{50%{transform:scaleX(.8);opacity:.1}}@keyframes robot-shine{50%{opacity:.25}}@keyframes robot-think{to{transform:translateY(-3px);opacity:.5}}@media(prefers-reduced-motion:reduce){.mes-robot *{animation:none!important}}
</style>

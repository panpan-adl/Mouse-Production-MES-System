<script setup lang="ts">
defineProps<{thinking?:boolean}>()
const uid='mesbot-'+Math.random().toString(36).slice(2,10)
</script>
<template><svg viewBox="0 0 160 180" class="mes-robot" :class="{thinking}" fill="none" aria-hidden="true">
 <defs><linearGradient :id="uid" x1="36" y1="20" x2="132" y2="158" gradientUnits="userSpaceOnUse"><stop stop-color="#eefcff"/><stop offset=".4" stop-color="#a9e4ff"/><stop offset="1" stop-color="#5497ee"/></linearGradient></defs>
 <ellipse class="robot-shadow" cx="81" cy="168" rx="35" ry="6" fill="#4b89bc" opacity=".16"/>
 <g class="robot-float"><path d="M80 37V24" stroke="#69afd9" stroke-width="4" stroke-linecap="round"/><circle cx="80" cy="20" r="6" fill="#9deaf7" stroke="#519bce" stroke-width="2"/><circle cx="78" cy="18" r="2" fill="white"/>
 <circle cx="47" cy="48" r="21" :fill="'url(#'+uid+')'" stroke="#6cace0" stroke-width="2"/><circle cx="114" cy="48" r="21" :fill="'url(#'+uid+')'" stroke="#6cace0" stroke-width="2"/><circle cx="47" cy="47" r="11" fill="#a4d4f5"/><circle cx="114" cy="47" r="11" fill="#a4d4f5"/>
 <g class="robot-arm"><path d="M111 127C127 123 134 113 132 106" stroke="#6baee8" stroke-width="13" stroke-linecap="round"/><path d="M130 107L132 101M134 107L138 103" stroke="#c6efff" stroke-width="4" stroke-linecap="round"/></g><path d="M48 129C40 137 42 144 48 145" stroke="#6baee8" stroke-width="12" stroke-linecap="round"/>
 <rect x="58" y="139" width="17" height="23" rx="8" fill="#6ba9e8" stroke="#548fc7" stroke-width="1.5"/><rect x="87" y="139" width="17" height="23" rx="8" fill="#6ba9e8" stroke="#548fc7" stroke-width="1.5"/>
 <rect x="51" y="112" width="61" height="40" rx="18" :fill="'url(#'+uid+')'" stroke="#6da9dc" stroke-width="2"/><path d="M70 129L75 134L70 139M81 139H92" stroke="#f4ffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
 <rect x="28" y="41" width="105" height="80" rx="34" :fill="'url(#'+uid+')'" stroke="#68a8db" stroke-width="2"/><path d="M44 56C52 48 66 46 81 47" stroke="white" stroke-opacity=".8" stroke-width="4" stroke-linecap="round"/>
 <rect x="39" y="65" width="83" height="46" rx="19" fill="#163951" stroke="#73bfe8" stroke-width="2"/><path d="M46 78C49 72 57 70 67 70" stroke="#b7f0ff" stroke-opacity=".16" stroke-width="2" stroke-linecap="round"/>
 <g class="robot-eyes"><rect x="53" y="81" width="11" height="15" rx="5.5" fill="#7aeeff"/><rect x="96" y="81" width="11" height="15" rx="5.5" fill="#7aeeff"/></g><path d="M75 98Q80 102 85 98" stroke="#88edff" stroke-width="2.5" stroke-linecap="round"/><circle cx="49" cy="100" r="3" fill="#529bce" opacity=".5"/><circle cx="112" cy="100" r="3" fill="#529bce" opacity=".5"/>
 <path class="robot-spark" d="M144 56V66M139 61H149M18 91V97M15 94H21" stroke="#76c6e7" stroke-width="2" stroke-linecap="round"/></g>
</svg></template>
<style scoped>
.mes-robot{display:block;width:100%;height:100%;overflow:visible;filter:drop-shadow(0 5px 5px #417eb71a)}.robot-float{animation:robot-hover 4s ease-in-out infinite;transform-origin:80px 100px}.robot-eyes{animation:robot-blink 6s infinite;transform-origin:80px 88px}.robot-arm{transform-origin:113px 128px;animation:robot-wave 8s ease-in-out infinite}.robot-shadow{animation:robot-shadow 4s ease-in-out infinite;transform-origin:80px 168px}.robot-spark{animation:robot-shine 3s ease-in-out infinite}.thinking .robot-eyes{animation:robot-think .8s ease-in-out infinite alternate}.thinking .robot-float{animation-duration:1.8s}@keyframes robot-hover{50%{transform:translateY(-7px) rotate(2deg)}}@keyframes robot-blink{0%,43%,47%,100%{transform:scaleY(1)}45%{transform:scaleY(.12)}}@keyframes robot-wave{0%,12%,24%,100%{transform:rotate(0)}15%,21%{transform:rotate(-15deg)}18%{transform:rotate(7deg)}}@keyframes robot-shadow{50%{transform:scaleX(.8);opacity:.1}}@keyframes robot-shine{50%{opacity:.25}}@keyframes robot-think{to{transform:translateY(-3px);opacity:.5}}@media(prefers-reduced-motion:reduce){.mes-robot *{animation:none!important}}
</style>

<script setup lang="ts">
import {useId} from 'vue'
const id=useId().replace(/:/g,'')
</script>
<template>
<svg class="blue-robot" viewBox="0 0 120 140" fill="none" aria-hidden="true">
 <defs><linearGradient :id="id+'shell'" x1="26" y1="20" x2="89" y2="115" gradientUnits="userSpaceOnUse"><stop stop-color="#e7faff"/><stop offset=".45" stop-color="#9ddcfa"/><stop offset="1" stop-color="#50a8e6"/></linearGradient><linearGradient :id="id+'body'" x1="43" y1="92" x2="78" y2="129" gradientUnits="userSpaceOnUse"><stop stop-color="#99deff"/><stop offset="1" stop-color="#4d9ddd"/></linearGradient></defs>
 <ellipse cx="60" cy="133" rx="23" ry="4" fill="#5aaddb" opacity=".16"/>
 <g class="robot-float">
 <path d="M60 22V13" stroke="#73b9df" stroke-width="3"/><circle cx="60" cy="9" r="5" fill="#a8e9ff" stroke="#72b9df" stroke-width="2"/>
 <circle cx="31" cy="30" r="15" :fill="`url(#${id}shell)`" stroke="#7dbde4" stroke-width="2"/><circle cx="89" cy="30" r="15" :fill="`url(#${id}shell)`" stroke="#7dbde4" stroke-width="2"/>
 <path d="M38 91C25 90 20 101 25 110C29 116 36 113 40 105" :fill="`url(#${id}body)`"/><g class="robot-wave"><path d="M83 96C99 100 105 88 100 80C97 76 92 80 91 86L81 88" :fill="`url(#${id}body)`"/><path d="M99 79L98 74" stroke="#79c5ef" stroke-width="4" stroke-linecap="round"/></g>
 <path d="M39 91H81L85 110C85 116 80 119 75 119L74 125C72 131 63 131 62 123L60 117L56 124C53 131 44 129 44 122L44 116C35 115 35 107 39 91Z" :fill="`url(#${id}body)`"/>
 <path d="M51 102L56 106L51 110M62 110H68" stroke="#e8fbff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>
 <rect x="17" y="27" width="86" height="70" rx="29" :fill="`url(#${id}shell)`" stroke="#75bde7" stroke-width="2"/>
 <path d="M28 43C32 33 47 32 61 32" stroke="white" stroke-opacity=".65" stroke-width="4" stroke-linecap="round"/>
 <rect x="25" y="43" width="70" height="43" rx="17" fill="#206087" stroke="#c2edff" stroke-width="3"/><rect x="29" y="46" width="62" height="35" rx="14" fill="#103d59"/>
 <g class="robot-eyes"><rect x="39" y="54" width="8" height="14" rx="4" fill="#7deaff"/><rect x="73" y="54" width="8" height="14" rx="4" fill="#7deaff"/></g>
 <path d="M55 68Q60 72 65 68" stroke="#81e5fa" stroke-width="2.4" stroke-linecap="round"/><circle cx="36" cy="72" r="3" fill="#5bbcdc" opacity=".45"/><circle cx="84" cy="72" r="3" fill="#5bbcdc" opacity=".45"/>
 <path d="M108 43V51M104 47H112M12 83V89M9 86H15" stroke="#a0daf4" stroke-width="2" stroke-linecap="round"/>
 </g>
</svg>
</template>
<style scoped>
.blue-robot{width:100%;height:100%;overflow:visible;filter:drop-shadow(0 5px 7px #3f96cb26)}.robot-float{animation:robot-hover 3.3s ease-in-out infinite;transform-origin:center}.robot-wave{transform-origin:84px 95px;animation:robot-wave 5s ease-in-out infinite}.robot-eyes{transform-origin:60px 61px;animation:robot-blink 5.6s infinite}@keyframes robot-hover{50%{transform:translateY(-4px)}}@keyframes robot-wave{0%,70%,100%{transform:rotate(0)}80%,90%{transform:rotate(-13deg)}85%,95%{transform:rotate(5deg)}}@keyframes robot-blink{0%,43%,47%,100%{transform:scaleY(1)}45%{transform:scaleY(.1)}}@media(prefers-reduced-motion:reduce){.robot-float,.robot-wave,.robot-eyes{animation:none}}
</style>

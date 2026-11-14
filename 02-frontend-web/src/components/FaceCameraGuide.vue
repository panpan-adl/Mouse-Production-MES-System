<template>
  <figure class="guide">
    <div class="frame">
      <slot />
      <div class="scan" aria-hidden="true"><i /></div>
    </div>
    <figcaption>请正对镜头，将整个面部放在扫描区内</figcaption>
  </figure>
</template>

<style scoped>
.guide { margin: 12px 0; }
.frame { position: relative; aspect-ratio: 4 / 3; overflow: hidden; border-radius: 16px; background: white; border: 1px solid #e5e9e7; }
.frame :slotted(video), .scan {
  position: absolute; inset: 0; width: 100%; height: 100%;
  clip-path: ellipse(24% 40% at 50% 50%);
  clip-path: shape(from 50% 10%, curve to 74% 42% with 65% 10% / 74% 23%, curve to 68% 70% with 74% 53% / 72% 63%, curve to 50% 89% with 64% 81% / 58% 89%, curve to 32% 70% with 42% 89% / 36% 81%, curve to 26% 42% with 28% 63% / 26% 53%, curve to 50% 10% with 26% 23% / 35% 10%, close);
}
.frame :slotted(video) { display: block; object-fit: contain; transform: scaleX(-1); }
.scan { pointer-events: none; }
.scan i { display: block; position: absolute; width: 100%; height: 2px; top: 10%; background: #72f8ca; box-shadow: 0 0 15px 4px #72f8ca; animation: sweep 2s ease-in-out infinite alternate; }
@keyframes sweep { to { top: 89%; } }
@media (prefers-reduced-motion: reduce) { .scan i { animation: none; top: 50%; } }
figcaption { margin: 10px 0; text-align: center; font-size: 14px; font-weight: 600; }
</style>

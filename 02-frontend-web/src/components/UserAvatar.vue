<script setup lang="ts">
import {ref,watch} from 'vue'
import {User} from 'lucide-vue-next'
const props=defineProps<{src?:string|null}>()
const failed=ref(false)
watch(()=>props.src,()=>failed.value=false)
</script>
<template><span class="user-avatar"><img v-if="src&&!failed" :src="src" alt="用户头像" @error="failed=true"/><User v-else aria-hidden="true"/></span></template>
<style scoped>.user-avatar{display:inline-flex;align-items:center;justify-content:center;border-radius:50%;background:#eaf0f8;color:#4c7ac6;overflow:hidden}.user-avatar img{width:100%;height:100%;object-fit:cover}.user-avatar svg{width:55%;height:55%}</style>

import { ref, watch } from 'vue'
export const sidebarCollapsed = ref(false)
export const motionEnabled = ref(localStorage.getItem('mes-motion') !== 'off')
watch(motionEnabled,value=>localStorage.setItem('mes-motion',value?'on':'off'))

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import './style.css'
import './immersive.css'
import './sky-theme.css'
import './notion-theme.css'
import './business-theme.css'
import App from './App.vue'
import { installBusinessFormValidation } from '@/utils/businessFormValidation'

const app = createApp(App)

app.use(createPinia())
app.use(router)

installBusinessFormValidation()
app.mount('#app')

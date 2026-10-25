import axios from 'axios'
import { showBusinessFormError } from './businessFormValidation'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const baseURL = '/api'

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000,
})

// Ignore expired responses from an earlier session after logout or a new login.
function expireSession(config?: AxiosRequestConfig) {
  const auth = useAuthStore()
  if (!auth.currentUser) return
  const sent = config?.headers?.Authorization
  if (sent && String(sent) !== `Bearer ${auth.currentUser.token || ''}`) return
  auth.logout()
  if (router.currentRoute.value.path !== '/login') router.replace('/login')
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    if (authStore.currentUser) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${authStore.currentUser.token || ''}`
    }
    return config
  },
  (error) => {
    if (error.response?.status === 401) {
      expireSession(error.config)
    }
    const message=error.response?.data?.msg||error.message||'请求失败'
    if(error.config?.method&&error.config.method!=='get')showBusinessFormError(message)
    return Promise.reject(new Error(message))
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res && typeof res.code === 'undefined') {
      return res
    }
    if (res.code !== 200) {
      if (res.code === 401) {
        expireSession(response.config)
      }
      if(response.config.method!=='get')showBusinessFormError(res.msg||'保存失败，请检查填写内容')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      expireSession(error.config)
    }
    const message=error.response?.data?.msg||error.message||'请求失败'
    if(error.config?.method&&error.config.method!=='get')showBusinessFormError(message)
    return Promise.reject(new Error(message))
  }
)

const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, config) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config) as unknown as Promise<T>
  },
}

export default request

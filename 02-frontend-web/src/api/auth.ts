import request from '@/utils/request'

export interface LoginParams {
  userCode: string
  password: string
}

export interface LoginResult {
  id: number
  userCode: string
  username: string
  realName: string
  phone: string
  email: string
  gender: number
  roleId: number
  roleName: string
  deptId: number
  deptName: string
  status: number
  token: string
}

export function login(params: LoginParams) {
  return request.post<LoginResult>('/auth/login', params)
}

export function logout() {
  return request.post('/auth/logout')
}
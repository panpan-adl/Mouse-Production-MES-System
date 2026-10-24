import request from '@/utils/request'

export interface User {
  id: number
  userCode: string
  username: string
  password: string
  realName: string
  phone: string
  email: string
  gender: number
  avatar: string
  roleId: number
  deptId: number
  status: number
  entryDate: string
  leaveDate: string
  lastLoginTime: string
  lastLoginIp: string
  createTime: string
  updateTime: string
  deleted: number
}

export interface CreateUserParams {
  username: string
  password: string
  realName: string
  phone?: string
  email?: string
  gender?: number
  roleId: number
  deptId: number
  status?: number
}

export interface UpdateUserParams {
  realName?: string
  phone?: string
  email?: string
  gender?: number
  roleId?: number
  deptId?: number
  status?: number
}

export function getUserList() {
  return request.get<User[]>('/users')
}

export function getUserById(id: number) {
  return request.get<User>(`/users/${id}`)
}

export function getUserByUserCode(userCode: string) {
  return request.get<User>(`/users/usercode/${userCode}`)
}

export function getUsersByRoleId(roleId: number) {
  return request.get<User[]>(`/users/role/${roleId}`)
}

export function getUsersByDeptId(deptId: number) {
  return request.get<User[]>(`/users/dept/${deptId}`)
}

export function getAllUsersIncludingDeleted() {
  return request.get<User[]>('/users/all')
}

export function getDeletedUsers() {
  return request.get<User[]>('/users/deleted')
}

export function createUser(params: CreateUserParams) {
  return request.post<User>('/users', params)
}

export function updateUser(id: number, params: UpdateUserParams) {
  return request.put<User>(`/users/${id}`, params)
}

export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

export function employeeLeave(id: number) {
  return request.post(`/users/${id}/leave`)
}

export function restoreUser(id: number) {
  return request.post<User>(`/users/${id}/restore`)
}

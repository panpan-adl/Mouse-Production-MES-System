import request from '@/utils/request'

/** 部门资料：对应后端 t_department 表。 */
export interface Department {
  id?: number
  parentId: number
  deptName: string
  deptCode: string
  leaderId?: number | null
  leaderName?: string
  sort?: number
  status?: number
  createTime?: string
  updateTime?: string
}

/** 部门管理接口：连接后端 /api/departments。 */
export const departmentApi = {
  list: () => request.get<Department[]>('/departments'),
  get: (id: number) => request.get<Department>(`/departments/${id}`),
  byParent: (parentId: number) => request.get<Department[]>(`/departments/parent/${parentId}`),
  create: (data: Department) => request.post<Department>('/departments', data),
  update: (id: number, data: Department) => request.put<Department>(`/departments/${id}`, data),
  remove: (id: number) => request.delete(`/departments/${id}`),
}

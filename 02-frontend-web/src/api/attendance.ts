import request from '@/utils/request'

/** 考勤记录：来自数据库 t_attendance，一条记录对应某员工某一天的考勤情况。 */
export interface Attendance {
  id?: number
  userId: number
  userName: string
  attendanceDate: string
  checkInTime?: string | null
  checkOutTime?: string | null
  workHours?: number
  status: number
  remark?: string
  createTime?: string
}

/** 考勤接口：只负责和后端 /api/attendances 通信。 */
export const attendanceApi = {
  list: () => request.get<Attendance[]>('/attendances'),
  get: (id: number) => request.get<Attendance>(`/attendances/${id}`),
  byUser: (userId: number) => request.get<Attendance[]>(`/attendances/user/${userId}`),
  byDate: (attendanceDate: string) => request.get<Attendance[]>(`/attendances/date/${attendanceDate}`),
  create: (data: Attendance) => request.post<Attendance>('/attendances', data),
  update: (id: number, data: Attendance) => request.put<Attendance>(`/attendances/${id}`, data),
}

import request from '@/utils/request'
import type { User } from '@/api/user'
/** Only active employees with an enabled, matching business role are returned. */
export const assignablePeople = (role: 3 | 4 | 8) => request.get<User[]>('/mes/assignable-people', { params: { role } })

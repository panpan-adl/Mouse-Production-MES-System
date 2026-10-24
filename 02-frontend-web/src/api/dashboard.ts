import request from '@/utils/request'
import type { DashboardData, DashboardFilters } from '@/mock/dashboard'

/** Empty lists are real results; failed requests are surfaced instead of displaying invented figures. */
export function getProductionDashboard(filters: DashboardFilters): Promise<DashboardData> {
  return request.get<DashboardData>('/dashboard/production', { params: filters })
}

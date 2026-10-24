/** Page access follows the same job boundaries as the navigation. Server write checks remain authoritative. */
export function canVisit(role: number, path: string): boolean {
  if (role === 9) return true
  if (!role || role < 1 || role > 11) return false
  if (['/', '/dashboard', '/notifications', '/user/profile', '/attendance'].includes(path)) return true
  if(path==='/mes')return true
  if(path==='/purchase/returns')return [1,2,4,5,6,7,10].includes(role)
  if(['/inventory/production-issue','/inventory/production-return','/inventory/product-receipts'].includes(path))return [1,2,4,5,6,7,10].includes(role)
  if(path==='/inventory/packages')return [1,2,5,7].includes(role)
  if(path==='/production/work-orders')return [1,2,3,4,5].includes(role)

  if(path === '/intelligence')return [1,2,4,5,6,7,10].includes(role)
  if(path === '/automation')return [1,2,4,6].includes(role)
  if (path.startsWith('/foundation')) return true
  if (path === '/manufacturing') return [1, 2, 3, 4].includes(role)
  if (path === '/fulfillment') return [1, 2, 3, 4, 5, 6, 7, 8, 10].includes(role)
  if (role === 3 && path.startsWith('/production/')) return ['/production/task', '/production/equipment-status'].includes(path)
  const domains: Record<string, number[]> = {
    production: [1, 2], quality: [1, 4, 10], inventory: [1, 5, 6, 10],
    purchase: [5, 6, 10], order: [7], equipment: [1, 2, 8, 10], system: [11],
  }
  const domain = path.split('/')[1]
  if (domains[domain]) return domains[domain].includes(role)
  const homes: Record<string, number> = {
    'prod-manager': 1, 'workshop-manager': 2, worker: 3, 'qc-manager': 4,
    'warehouse-manager': 5, 'purchase-manager': 6, 'sales-manager': 7, 'equipment-manager': 8, cfo: 10,
  }
  return homes[domain] === role
}

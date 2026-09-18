import request from '@/utils/request'

// 后台首页看板统计。口径由后端定义（WashOrderStatsService），前端只展示不计算
export function dashboardStats() {
  return request({
    url: '/admin-api/wash/dashboard/stats',
    method: 'get'
  })
}

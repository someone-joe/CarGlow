import request from '@/utils/request'

// 后台订单查询。注意：后台接口前缀是 /admin-api，与小程序端的 /api/v1 分开
export function listOrder(query) {
  return request({
    url: '/admin-api/wash/order/list',
    method: 'get',
    params: query
  })
}

// 状态下拉选项由后端返回（来源是 OrderStatus 枚举），前端不得自己写一份状态名单
export function statusOptions() {
  return request({
    url: '/admin-api/wash/order/status-options',
    method: 'get'
  })
}

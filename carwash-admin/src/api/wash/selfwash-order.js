import request from '@/utils/request'

// 自助洗车订单监控（后台）

export function listSelfwashOrders(params) {
  return request({
    url: '/admin-api/wash/selfwash/order/list',
    method: 'get',
    params
  })
}

export function getSelfwashOrder(orderNo) {
  return request({
    url: `/admin-api/wash/selfwash/order/${orderNo}`,
    method: 'get'
  })
}

export function finishSelfwashOrder(orderNo) {
  return request({
    url: `/admin-api/wash/selfwash/order/${orderNo}/finish`,
    method: 'post'
  })
}

export function selfwashStatusOptions() {
  return request({
    url: '/admin-api/wash/selfwash/order/status-options',
    method: 'get'
  })
}

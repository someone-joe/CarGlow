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

// 订单详情（含 8 节点时间轴）
export function getOrder(orderNo) {
  return request({
    url: '/admin-api/wash/order/' + orderNo,
    method: 'get'
  })
}

// 当前状态下允许触发的事件（由状态机规则表算出，避免客服误操作）
export function eventOptions(orderNo) {
  return request({
    url: '/admin-api/wash/order/' + orderNo + '/event-options',
    method: 'get'
  })
}

// 人工推进状态（异常补救，会写流转日志留痕）
export function advanceOrder(orderNo, event, reason) {
  return request({
    url: '/admin-api/wash/order/' + orderNo + '/advance',
    method: 'post',
    data: { event, reason }
  })
}

// 后台取消（不受"客户只能取消车未动"限制）
export function cancelOrder(orderNo, reason) {
  return request({
    url: '/admin-api/wash/order/' + orderNo + '/cancel',
    method: 'post',
    data: { reason }
  })
}

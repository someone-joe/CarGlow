import request from '@/utils/request'

// 后台格口管理。接口前缀 /admin-api
export function listSlot(query) {
  return request({
    url: '/admin-api/wash/slot/list',
    method: 'get',
    params: query
  })
}

export function getSlot(slotId) {
  return request({
    url: '/admin-api/wash/slot/' + slotId,
    method: 'get'
  })
}

export function addSlot(data) {
  return request({
    url: '/admin-api/wash/slot',
    method: 'post',
    data
  })
}

export function updateSlot(data) {
  return request({
    url: '/admin-api/wash/slot',
    method: 'put',
    data
  })
}

export function delSlot(slotId) {
  return request({
    url: '/admin-api/wash/slot/' + slotId,
    method: 'delete'
  })
}

// 格口下拉：按机柜过滤
export function slotOptions(cabinetId) {
  return request({
    url: '/admin-api/wash/slot/options',
    method: 'get',
    params: { cabinetId }
  })
}

// 开箱留痕审计
export function openLog(query) {
  return request({
    url: '/admin-api/wash/slot/open-log',
    method: 'get',
    params: query
  })
}

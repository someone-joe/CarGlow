import request from '@/utils/request'

// 后台钥匙柜（机柜）管理。接口前缀 /admin-api
export function listCabinet(query) {
  return request({
    url: '/admin-api/wash/cabinet/list',
    method: 'get',
    params: query
  })
}

export function getCabinet(cabinetId) {
  return request({
    url: '/admin-api/wash/cabinet/' + cabinetId,
    method: 'get'
  })
}

export function addCabinet(data) {
  return request({
    url: '/admin-api/wash/cabinet',
    method: 'post',
    data
  })
}

export function updateCabinet(data) {
  return request({
    url: '/admin-api/wash/cabinet',
    method: 'put',
    data
  })
}

export function delCabinet(cabinetId) {
  return request({
    url: '/admin-api/wash/cabinet/' + cabinetId,
    method: 'delete'
  })
}

// 机柜下拉：给格口管理等页面用
export function cabinetOptions() {
  return request({
    url: '/admin-api/wash/cabinet/options',
    method: 'get'
  })
}

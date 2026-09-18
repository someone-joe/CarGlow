import request from '@/utils/request'

// 后台会员列表（手机号、openid 后端已脱敏）
export function listMember(query) {
  return request({
    url: '/admin-api/wash/member/list',
    method: 'get',
    params: query
  })
}

// 后台车辆列表（可按车牌模糊查、按会员过滤）
export function listVehicle(query) {
  return request({
    url: '/admin-api/wash/vehicle/list',
    method: 'get',
    params: query
  })
}

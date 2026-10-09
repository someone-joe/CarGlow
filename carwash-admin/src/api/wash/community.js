import request from '@/utils/request'

// 后台小区管理。接口前缀 /admin-api
export function listCommunity(query) {
  return request({
    url: '/admin-api/wash/community/list',
    method: 'get',
    params: query
  })
}

export function getCommunity(communityId) {
  return request({
    url: '/admin-api/wash/community/' + communityId,
    method: 'get'
  })
}

export function addCommunity(data) {
  return request({
    url: '/admin-api/wash/community',
    method: 'post',
    data
  })
}

export function updateCommunity(data) {
  return request({
    url: '/admin-api/wash/community',
    method: 'put',
    data
  })
}

export function delCommunity(communityId) {
  return request({
    url: '/admin-api/wash/community/' + communityId,
    method: 'delete'
  })
}

// 小区下拉：可选按站点过滤，柜机表单用
export function communityOptions(siteId) {
  return request({
    url: '/admin-api/wash/community/options',
    method: 'get',
    params: { siteId }
  })
}

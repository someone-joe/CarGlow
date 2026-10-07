import request from '@/utils/request'

// 后台站点（中央站）管理。接口前缀 /admin-api，与小程序端 /api/v1 分开
export function listSite(query) {
  return request({
    url: '/admin-api/wash/site/list',
    method: 'get',
    params: query
  })
}

export function getSite(siteId) {
  return request({
    url: '/admin-api/wash/site/' + siteId,
    method: 'get'
  })
}

export function addSite(data) {
  return request({
    url: '/admin-api/wash/site',
    method: 'post',
    data
  })
}

export function updateSite(data) {
  return request({
    url: '/admin-api/wash/site',
    method: 'put',
    data
  })
}

export function delSite(siteId) {
  return request({
    url: '/admin-api/wash/site/' + siteId,
    method: 'delete'
  })
}

// 站点下拉：来源 wash_site 表（del_flag=0）
export function siteOptions() {
  return request({
    url: '/admin-api/wash/site/options',
    method: 'get'
  })
}

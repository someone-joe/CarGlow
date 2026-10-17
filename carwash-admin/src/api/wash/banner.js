import request from '@/utils/request'

/** 契约：openapi.yaml /admin-api/wash/banner/**（首页轮播） */
export function listBanner(query) {
  return request({
    url: '/admin-api/wash/banner/list',
    method: 'get',
    params: query
  })
}

export function getBanner(bannerId) {
  return request({
    url: '/admin-api/wash/banner/' + bannerId,
    method: 'get'
  })
}

export function addBanner(data) {
  return request({
    url: '/admin-api/wash/banner',
    method: 'post',
    data: data
  })
}

export function updateBanner(data) {
  return request({
    url: '/admin-api/wash/banner',
    method: 'put',
    data: data
  })
}

export function delBanner(bannerId) {
  return request({
    url: '/admin-api/wash/banner/' + bannerId,
    method: 'delete'
  })
}

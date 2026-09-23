import request from '@/utils/request'

// 后台服务项管理。注意：后台接口前缀是 /admin-api，与小程序端的 /api/v1 分开
export function listService(query) {
  return request({
    url: '/admin-api/wash/service/list',
    method: 'get',
    params: query
  })
}

export function getService(serviceId) {
  return request({
    url: '/admin-api/wash/service/' + serviceId,
    method: 'get'
  })
}

// 价格以「分」提交：表单里填的是元，转换在页面层做
export function addService(data) {
  return request({
    url: '/admin-api/wash/service',
    method: 'post',
    data
  })
}

export function updateService(data) {
  return request({
    url: '/admin-api/wash/service',
    method: 'put',
    data
  })
}

export function delService(serviceId) {
  return request({
    url: '/admin-api/wash/service/' + serviceId,
    method: 'delete'
  })
}

// 分类下拉：由后端返回（来源 wash_service_category 表），前端不得自己写一份分类名单
export function serviceCategoryOptions() {
  return request({
    url: '/admin-api/wash/service/category-options',
    method: 'get'
  })
}

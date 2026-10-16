import request from '@/utils/request'

// 自助洗车工位管理（后台）

export function listWorkstations(params) {
  return request({
    url: '/admin-api/wash/selfwash/workstation/list',
    method: 'get',
    params
  })
}

export function addWorkstation(data) {
  return request({
    url: '/admin-api/wash/selfwash/workstation',
    method: 'post',
    data
  })
}

export function updateWorkstation(id, data) {
  return request({
    url: `/admin-api/wash/selfwash/workstation/${id}`,
    method: 'put',
    data
  })
}

export function delWorkstation(id) {
  return request({
    url: `/admin-api/wash/selfwash/workstation/${id}`,
    method: 'delete'
  })
}

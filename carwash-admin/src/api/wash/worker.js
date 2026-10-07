import request from '@/utils/request'

// 后台师傅（工作人员）管理。后台接口前缀是 /admin-api，与小程序端的 /api/v1 分开
export function listWorker(query) {
  return request({
    url: '/admin-api/wash/worker/list',
    method: 'get',
    params: query
  })
}

export function getWorker(workerId) {
  return request({
    url: '/admin-api/wash/worker/' + workerId,
    method: 'get'
  })
}

export function addWorker(data) {
  return request({
    url: '/admin-api/wash/worker',
    method: 'post',
    data
  })
}

// 修改资料：不接受 password，改密码走 resetWorkerPassword
export function updateWorker(data) {
  return request({
    url: '/admin-api/wash/worker',
    method: 'put',
    data
  })
}

export function delWorker(workerId) {
  return request({
    url: '/admin-api/wash/worker/' + workerId,
    method: 'delete'
  })
}

export function resetWorkerPassword(workerId, password) {
  return request({
    url: '/admin-api/wash/worker/' + workerId + '/reset-password',
    method: 'post',
    data: { password }
  })
}

// 状态下拉：由后端 WorkerStatus 枚举返回，前端不得自己写一份状态名单
export function workerStatusOptions() {
  return request({
    url: '/admin-api/wash/worker/status-options',
    method: 'get'
  })
}

// 站点下拉：由后端返回（来源 wash_site 表）
export function workerSiteOptions() {
  return request({
    url: '/admin-api/wash/worker/site-options',
    method: 'get'
  })
}

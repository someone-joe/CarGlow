import request from '@/utils/request'

// 小程序客服 tab 后台配置：双电话 / 夜间提示 / 企微码 / 常见问题（您可能会遇到的问题）
export function getCsConfig() {
  return request({
    url: '/admin-api/wash/cs-config',
    method: 'get'
  })
}

export function saveCsConfig(data) {
  return request({
    url: '/admin-api/wash/cs-config',
    method: 'post',
    data
  })
}

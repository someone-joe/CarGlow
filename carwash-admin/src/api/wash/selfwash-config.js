import request from '@/utils/request'

/** 契约：openapi.yaml /admin-api/wash/selfwash/pricing（自助洗车计时计价） */
export function getSelfwashPricing() {
  return request({
    url: '/admin-api/wash/selfwash/pricing',
    method: 'get'
  })
}

/** 金额单位：分；时长单位：分钟 */
export function saveSelfwashPricing(data) {
  return request({
    url: '/admin-api/wash/selfwash/pricing',
    method: 'put',
    data: data
  })
}

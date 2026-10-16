import request from '@/utils/request'

// 储值钱包后台配置：充值赠送规则 + 充值流水查询

export function listGiftRules() {
  return request({
    url: '/admin-api/wash/wallet/gift-rules',
    method: 'get'
  })
}

/** 整体覆盖赠送规则；rows: [{ rechargeFen, giftFen }]（单位：分） */
export function saveGiftRules(rows) {
  return request({
    url: '/admin-api/wash/wallet/gift-rules',
    method: 'put',
    data: rows
  })
}

export function listRecharges(params) {
  return request({
    url: '/admin-api/wash/wallet/recharges',
    method: 'get',
    params
  })
}

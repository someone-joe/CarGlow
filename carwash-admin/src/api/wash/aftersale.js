import request from '@/utils/request'

// 后台售后工单。接口前缀 /admin-api。
// 注意：处置同意只表示「已受理」，钱是否退回去要看退款单页（以退款单状态为准）。

export function listAfterSale(query) {
  return request({
    url: '/admin-api/wash/after-sale/list',
    method: 'get',
    params: query
  })
}

// accept=true 同意（REFUND 类会触发退款）；accept=false 驳回，remark 必填
export function handleAfterSale(afterSaleNo, accept, remark) {
  return request({
    url: '/admin-api/wash/after-sale/' + afterSaleNo + '/handle',
    method: 'put',
    data: { accept, remark }
  })
}

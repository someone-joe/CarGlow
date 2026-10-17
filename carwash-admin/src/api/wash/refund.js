import request from '@/utils/request'

// 后台退款单。接口前缀 /admin-api。
// 这里是判断「钱到底退没退」的地方：以退款单 status 为准，订单状态 REFUNDING 只是处理中。

export function listRefund(query) {
  return request({
    url: '/admin-api/wash/refund/list',
    method: 'get',
    params: query
  })
}

// 人工重试：超过自动重试上限或运营催办时用。已成功的单后端会拒绝（重退等于重复打款）
export function retryRefund(refundNo) {
  return request({
    url: '/admin-api/wash/refund/' + refundNo + '/retry',
    method: 'post'
  })
}

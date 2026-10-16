import request from '@/utils/request'

// 发票申请管理（后台）

export function listInvoices(params) {
  return request({
    url: '/admin-api/wash/invoice/list',
    method: 'get',
    params
  })
}

export function getInvoice(invoiceId) {
  return request({
    url: `/admin-api/wash/invoice/${invoiceId}`,
    method: 'get'
  })
}

export function issueInvoice(invoiceId, invoiceNo) {
  return request({
    url: `/admin-api/wash/invoice/${invoiceId}/issue`,
    method: 'post',
    data: { invoiceNo }
  })
}

export function rejectInvoice(invoiceId, reason) {
  return request({
    url: `/admin-api/wash/invoice/${invoiceId}/reject`,
    method: 'post',
    data: { reason }
  })
}

export function invoiceStatusOptions() {
  return request({
    url: '/admin-api/wash/invoice/status-options',
    method: 'get'
  })
}

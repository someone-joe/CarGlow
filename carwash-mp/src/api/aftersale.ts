import type { components } from '@/api/schema'
import { request } from '@/utils/request'

export type AfterSaleVO = components['schemas']['AfterSaleVO']
export type RefundVO = components['schemas']['RefundVO']

/**
 * 我的售后工单。契约：openapi.yaml GET /api/v1/after-sales。
 *
 * <p>状态中文名走后端 statusLabel，前端只展示不翻译（与订单 statusLabel 同一口径）。
 * <p>查询参数走 data（本项目 request 的 GET 约定，见 fetchOrderList）。
 */
export function fetchMyAfterSales(status?: string): Promise<AfterSaleVO[]> {
  return request<AfterSaleVO[]>({
    url: '/api/v1/after-sales',
    method: 'GET',
    data: status ? { status } : {},
  })
}

/**
 * 我的退款单。契约：openapi.yaml GET /api/v1/refunds。
 *
 * <p>钱是否真的退回去，以这里的 status 为准（SUCCESS 才是已到账），
 * 订单显示「退款中」只代表在处理中。
 */
export function fetchMyRefunds(orderNo?: string): Promise<RefundVO[]> {
  return request<RefundVO[]>({
    url: '/api/v1/refunds',
    method: 'GET',
    data: orderNo ? { orderNo } : {},
  })
}

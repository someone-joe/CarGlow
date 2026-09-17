import type { components } from '@/api/schema'
import { request } from '@/utils/request'

export type OrderListItemVO = components['schemas']['OrderListItemVO']

/** 契约：openapi.yaml /api/v1/orders 响应 data */
export interface OrderPageVO {
  list: OrderListItemVO[]
  total: number
  pageNum: number
  pageSize: number
}

/** Tab 取值由契约 enum 限定，前端不得新增取值 */
export type OrderTab = 'ONGOING' | 'WAIT_REVIEW' | 'ALL'

export interface OrderListParams {
  tab?: OrderTab
  pageNum?: number
  pageSize?: number
}

/** 契约：openapi.yaml POST /api/v1/orders 请求体 */
export interface CreateOrderRequest {
  serviceId: number
  vehicleId: number
  cabinetId: number
  appointDate: string
  appointTime: string
  pickupRequired?: boolean
  remark?: string
  parkPhotoFileIds?: string[]
  agreed: boolean
}

export type CreateOrderVO = components['schemas']['CreateOrderVO']

/**
 * 提交订单。契约要求幂等键 Idempotency-Key（客户端生成 UUID，重复提交返回首次结果）。
 * 幂等键由此处统一生成，页面不用关心。
 */
export function createOrder(data: CreateOrderRequest): Promise<CreateOrderVO> {
  return request<CreateOrderVO>({
    url: '/api/v1/orders',
    method: 'POST',
    data,
    idempotent: true,
  })
}

/** 契约：openapi.yaml POST /api/v1/payments/{orderNo}/mock-pay（x-dev-only，生产由支付回调替代） */
export function payOrder(orderNo: string): Promise<components['schemas']['PayResultVO']> {
  return request<components['schemas']['PayResultVO']>({
    url: `/api/v1/payments/${orderNo}/mock-pay`,
    method: 'POST',
  })
}

export function fetchOrderList(params: OrderListParams = {}): Promise<OrderPageVO> {
  return request<OrderPageVO>({
    url: '/api/v1/orders',
    method: 'GET',
    data: {
      tab: params.tab ?? 'ONGOING',
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
    },
  })
}

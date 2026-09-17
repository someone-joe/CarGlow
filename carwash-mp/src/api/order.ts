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

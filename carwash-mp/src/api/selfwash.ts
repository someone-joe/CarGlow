import { request } from '@/utils/request'

/** 契约：openapi.yaml /api/v1/selfwash/** */
export interface WorkstationVO {
  workstationId: number
  siteId: number
  code: string
  name?: string
  status: 'IDLE' | 'SERVING' | 'DISABLED'
  enabled: string
  remainingSeconds: number
  queueCount: number
}

export interface SelfwashOrderVO {
  orderNo: string
  siteId: number
  workstationId: number
  serviceName?: string
  serviceMinutes: number
  payAmount: number
  payChannel?: string
  status: 'WAIT_PAY' | 'WAIT_USE' | 'QUEUING' | 'SERVING' | 'PAUSED' | 'DONE' | 'CANCELED'
  statusLabel: string
  queueNo?: number
  startedAt?: number
  expireAt?: number
  addedSeconds: number
  payExpireAt?: number
  cancelReason?: string
  createTime: string
  remainingSeconds: number
}

export function fetchWorkstations(siteId: number): Promise<WorkstationVO[]> {
  return request<WorkstationVO[]>({ url: '/api/v1/selfwash/workstations', method: 'GET', data: { siteId } })
}

export function createSelfwashOrder(data: { workstationId: number; serviceId: number }): Promise<string> {
  return request<string>({ url: '/api/v1/selfwash/orders', method: 'POST', data })
}

export function fetchSelfwashOrders(tab?: string): Promise<SelfwashOrderVO[]> {
  return request<SelfwashOrderVO[]>({ url: '/api/v1/selfwash/orders', method: 'GET', data: tab ? { tab } : {} })
}

export function fetchSelfwashOrder(orderNo: string): Promise<SelfwashOrderVO> {
  return request<SelfwashOrderVO>({ url: `/api/v1/selfwash/orders/${orderNo}`, method: 'GET' })
}

export function paySelfwashBalance(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/selfwash/orders/${orderNo}/pay-balance`, method: 'POST' })
}

export function paySelfwashMock(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/selfwash/orders/${orderNo}/mock-pay`, method: 'POST' })
}

export function cancelSelfwash(orderNo: string, reason: string): Promise<void> {
  return request<void>({ url: `/api/v1/selfwash/orders/${orderNo}/cancel`, method: 'POST', data: { reason } })
}

export function activateSelfwash(orderNo: string): Promise<SelfwashOrderVO> {
  return request<SelfwashOrderVO>({ url: `/api/v1/selfwash/orders/${orderNo}/activate`, method: 'POST' })
}

export function addSelfwashTime(orderNo: string, minutes: number): Promise<SelfwashOrderVO> {
  return request<SelfwashOrderVO>({ url: `/api/v1/selfwash/orders/${orderNo}/add-time`, method: 'POST', data: { minutes } })
}

export function finishSelfwash(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/selfwash/orders/${orderNo}/finish`, method: 'POST' })
}

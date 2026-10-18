import { request } from '@/utils/request'

/** 契约：openapi.yaml /api/v1/selfwash/** */
export interface WorkstationQueueItemVO {
  /** 脱敏车牌，如 京A***12；无车牌显示 *** */
  plateNo?: string
  serviceName?: string
  status: 'SERVING' | 'QUEUING'
  /** 服务中=剩余分钟；排队中=总时长（含加时），向上取整 */
  estimatedMinutes?: number
}

export interface WorkstationVO {
  workstationId: number
  siteId: number
  code: string
  name?: string
  status: 'IDLE' | 'SERVING' | 'DISABLED'
  enabled: string
  remainingSeconds: number
  queueCount: number
  /** 服务中剩余 + 排队单总时长（含加时），向上取整 */
  estimatedWaitMinutes?: number
  /** 服务中+排队中订单明细（服务中在前），车牌脱敏，排队进度弹窗用 */
  queueItems?: WorkstationQueueItemVO[]
}

export interface SelfwashOrderVO {
  orderNo: string
  siteId: number
  workstationId: number
  serviceName?: string
  plateNo?: string
  vehicleType?: string
  vehicleTypeLabel?: string
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
  couponUserId?: number
  couponDiscount?: number
  createTime: string
  remainingSeconds: number
}

/** 契约：openapi.yaml GET /api/v1/selfwash/pricing（计时计价参数，单位分/分钟） */
export interface SelfwashPricingVO {
  baseAmount: number
  baseMinutes: number
  overtimeAmount: number
  overtimeMinutes: number
}

export function fetchSelfwashPricing(): Promise<SelfwashPricingVO> {
  return request<SelfwashPricingVO>({ url: '/api/v1/selfwash/pricing', method: 'GET' })
}

export function fetchWorkstations(siteId: number): Promise<WorkstationVO[]> {
  return request<WorkstationVO[]>({ url: '/api/v1/selfwash/workstations', method: 'GET', data: { siteId } })
}

/** 车型选项（UI 选择用）。SSOT 在后端 VehicleType 枚举 + openapi.yaml enum，后端会校验非法值。 */
export const VEHICLE_TYPE_OPTIONS = [
  { value: 'SEDAN', label: '小轿车' },
  { value: 'SUV', label: 'SUV' },
  { value: 'MPV', label: '商务车' },
  { value: 'OTHER', label: '其他' },
] as const

export function createSelfwashOrder(data: { workstationId: number; serviceId: number; couponUserId?: number; plateNo: string; vehicleType: string }): Promise<string> {
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

import type { components } from '@/api/schema'
import { request } from '@/utils/request'

export type CouponTemplateVO = components['schemas']['CouponTemplateVO']
export type CouponUserVO = components['schemas']['CouponUserVO']
export type CouponBestVO = components['schemas']['CouponBestVO']
export type InsuranceProductVO = components['schemas']['InsuranceProductVO']
export type InsurancePolicyVO = components['schemas']['InsurancePolicyVO']

/** 券状态由契约 enum 限定，前端不得新增取值 */
export type CouponStatus = 'UNUSED' | 'USED' | 'EXPIRED'

/** 契约：openapi.yaml GET /api/v1/coupons/center（领券中心，含已领数与剩余库存） */
export function fetchCouponCenter(): Promise<CouponTemplateVO[]> {
  return request<CouponTemplateVO[]>({ url: '/api/v1/coupons/center', method: 'GET' })
}

/** 契约：openapi.yaml POST /api/v1/coupons/templates/{templateId}/receive */
export function receiveCoupon(templateId: number): Promise<CouponUserVO> {
  return request<CouponUserVO>({ url: `/api/v1/coupons/templates/${templateId}/receive`, method: 'POST' })
}

/** 契约：openapi.yaml GET /api/v1/coupons（status 不传查全部） */
export function fetchMyCoupons(status?: CouponStatus): Promise<CouponUserVO[]> {
  return request<CouponUserVO[]>({ url: '/api/v1/coupons', method: 'GET', data: status ? { status } : {} })
}

/** 契约：openapi.yaml GET /api/v1/coupons/best（无可用的券时 data 为 null） */
export function fetchBestCoupon(amount: number): Promise<CouponBestVO | null> {
  return request<CouponBestVO | null>({ url: '/api/v1/coupons/best', method: 'GET', data: { amount } })
}

/** 契约：openapi.yaml GET /api/v1/insurances/products（可投保产品） */
export function fetchInsuranceProducts(): Promise<InsuranceProductVO[]> {
  return request<InsuranceProductVO[]>({ url: '/api/v1/insurances/products', method: 'GET' })
}

/** 契约：openapi.yaml GET /api/v1/insurances（我的保单） */
export function fetchMyPolicies(): Promise<InsurancePolicyVO[]> {
  return request<InsurancePolicyVO[]>({ url: '/api/v1/insurances', method: 'GET' })
}

/** 契约：openapi.yaml POST /api/v1/insurances 请求体（productId / vehicleId 必填） */
export interface BuyInsuranceRequest {
  productId: number
  vehicleId: number
  orderNo?: string | null
}

/** 契约：openapi.yaml POST /api/v1/insurances（dev 模式直接生效为 ACTIVE） */
export function buyInsurance(data: BuyInsuranceRequest): Promise<InsurancePolicyVO> {
  return request<InsurancePolicyVO>({ url: '/api/v1/insurances', method: 'POST', data })
}

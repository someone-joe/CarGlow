import type { components } from '@/api/schema'
import { request } from '@/utils/request'

export type ServiceVO = components['schemas']['ServiceVO']
export type VehicleVO = components['schemas']['VehicleVO']
export type CabinetVO = components['schemas']['CabinetVO']

/** 契约：openapi.yaml GET /api/v1/services（只出上架服务项） */
export function fetchServices(): Promise<ServiceVO[]> {
  return request<ServiceVO[]>({
    url: '/api/v1/services',
    method: 'GET'
  })
}

/** 契约：openapi.yaml GET /api/v1/vehicles（只返回当前登录会员的车辆，后端不收 memberId） */
export function fetchVehicles(): Promise<VehicleVO[]> {
  return request<VehicleVO[]>({
    url: '/api/v1/vehicles',
    method: 'GET'
  })
}

/**
 * 契约：openapi.yaml GET /api/v1/cabinets。
 * communityId 可省略；distance 目前后端恒为 null（定位未开工），前端不要展示"距您 xx 米"。
 */
export function fetchCabinets(communityId?: number): Promise<CabinetVO[]> {
  return request<CabinetVO[]>({
    url: '/api/v1/cabinets',
    method: 'GET',
    data: communityId ? { communityId } : {}
  })
}

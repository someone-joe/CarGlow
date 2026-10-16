import { request } from '@/utils/request'

/** 契约：openapi.yaml /api/v1/park-locations */
export interface ParkLocationVO {
  locationId: number
  communityId: number
  communityName?: string
  building?: string
  parkingNo?: string
  detail?: string
  isDefault?: string
}

export interface ParkLocationRequest {
  communityId: number
  communityName?: string
  building?: string
  parkingNo?: string
  detail?: string
  isDefault?: string
}

export function fetchParkLocations(): Promise<ParkLocationVO[]> {
  return request<ParkLocationVO[]>({ url: '/api/v1/park-locations', method: 'GET' })
}

export function createParkLocation(data: ParkLocationRequest): Promise<number> {
  return request<number>({ url: '/api/v1/park-locations', method: 'POST', data })
}

export function updateParkLocation(locationId: number, data: ParkLocationRequest): Promise<void> {
  return request<void>({ url: `/api/v1/park-locations/${locationId}`, method: 'PUT', data })
}

export function deleteParkLocation(locationId: number): Promise<void> {
  return request<void>({ url: `/api/v1/park-locations/${locationId}`, method: 'DELETE' })
}

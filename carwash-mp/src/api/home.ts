import type { components } from '@/api/schema'
import { request } from '@/utils/request'

export type SiteVO = components['schemas']['SiteVO']
export type CommunityVO = components['schemas']['CommunityVO']
export type BannerVO = components['schemas']['BannerVO']
export type CapacityVO = components['schemas']['CapacityVO']
export type ServiceCategoryVO = components['schemas']['ServiceCategoryVO']
export type ServiceDetailVO = components['schemas']['ServiceDetailVO']

/** 契约：openapi.yaml GET /api/v1/site/current（P1 顶部站点栏） */
export function fetchSiteCurrent(): Promise<SiteVO> {
  return request<SiteVO>({ url: '/api/v1/site/current', method: 'GET' })
}

/** 契约：openapi.yaml GET /api/v1/communities（已开通小区列表） */
export function fetchCommunities(): Promise<CommunityVO[]> {
  return request<CommunityVO[]>({ url: '/api/v1/communities', method: 'GET' })
}

/** 契约：openapi.yaml POST /api/v1/community-applies（申请开通我的小区） */
export function applyCommunity(data: { communityName: string; contactPhone?: string }): Promise<void> {
  return request<void>({ url: '/api/v1/community-applies', method: 'POST', data })
}

/** 契约：openapi.yaml GET /api/v1/banners（P1 Banner 轮播） */
export function fetchBanners(): Promise<BannerVO[]> {
  return request<BannerVO[]>({ url: '/api/v1/banners', method: 'GET' })
}

/** 契约：openapi.yaml GET /api/v1/capacity（P1 产能条：今晚剩余名额 / 最晚存钥匙时间） */
export function fetchCapacity(): Promise<CapacityVO> {
  return request<CapacityVO>({ url: '/api/v1/capacity', method: 'GET' })
}

/** 契约：openapi.yaml GET /api/v1/service-categories（P2 分类 Tab） */
export function fetchServiceCategories(): Promise<ServiceCategoryVO[]> {
  return request<ServiceCategoryVO[]>({ url: '/api/v1/service-categories', method: 'GET' })
}

/** 契约：openapi.yaml GET /api/v1/services/{serviceId}（P3 服务详情） */
export function fetchServiceDetail(serviceId: number): Promise<ServiceDetailVO> {
  return request<ServiceDetailVO>({ url: `/api/v1/services/${serviceId}`, method: 'GET' })
}

import { request } from '@/utils/request'

/**
 * 客服配置。契约：openapi.yaml GET /api/v1/config/customer-service。
 *
 * <p>该接口在契约里是内联 schema（没有具名 CustomerServiceVO），故此处按契约逐字段声明，
 * 不用 components['schemas'] 引用。字段含义见契约：企微二维码、平台电话、站点电话、夜间提示。
 */
export interface CustomerServiceVO {
  wecomQrcodeUrl?: string
  platformPhone?: string
  stationPhone?: string
  nightTip?: string
}

/**
 * 电话号码与夜间提示一律由后端配置下发，前端不得硬编码。
 */
export function fetchCustomerService(): Promise<CustomerServiceVO> {
  return request<CustomerServiceVO>({
    url: '/api/v1/config/customer-service',
    method: 'GET'
  })
}

import { reactive } from 'vue'

/**
 * 跨页下单草稿：服务列表/详情「去下单」时写入，下单页（首页）onShow 读取并预选。
 * 模块级单例，避免 tab 页无法携带参数跳转的问题。
 */
export interface OrderDraft {
  serviceId?: number
  cabinetId?: number
  vehicleId?: number
  /** 从券「去使用」带入的券 ID */
  couponUserId?: number
}

export const orderDraft = reactive<OrderDraft>({})

export function setOrderDraft(draft: OrderDraft): void {
  Object.assign(orderDraft, draft)
}

export function clearOrderDraft(): void {
  orderDraft.serviceId = undefined
  orderDraft.cabinetId = undefined
  orderDraft.vehicleId = undefined
  orderDraft.couponUserId = undefined
}

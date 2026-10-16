import { request } from '@/utils/request'

/** 契约：openapi.yaml /api/v1/wallet 响应 data。金额单位：分。 */
export interface WalletVO {
  balance: number
  giftBalance: number
  totalRecharge: number
}

/** 契约：/api/v1/wallet/recharge-rules */
export interface GiftRuleVO {
  ruleId: number
  rechargeFen: number
  giftFen: number
}

/** 契约：/api/v1/wallet/flows */
export interface WalletFlowVO {
  flowNo: string
  bizType: string
  changeAmount: number
  giftChange: number
  balanceAfter: number
  giftAfter: number
  bizNo: string
  remark: string
  createTime: string
}

export interface WalletFlowPage {
  list: WalletFlowVO[]
  total: number
  pageNum: number
  pageSize: number
}

export interface RechargeResult {
  rechargeNo: string
  payAmount: number
  giftAmount: number
  expireAt: number
}

/** 我的钱包（余额） */
export function fetchWallet(): Promise<WalletVO> {
  return request<WalletVO>({ url: '/api/v1/wallet', method: 'GET' })
}

/** 充值赠送规则（充值更优惠弹窗/充值页档位） */
export function fetchRechargeRules(): Promise<GiftRuleVO[]> {
  return request<GiftRuleVO[]>({ url: '/api/v1/wallet/recharge-rules', method: 'GET' })
}

/** 钱包流水分页 */
export function fetchWalletFlows(pageNum = 1, pageSize = 20): Promise<WalletFlowPage> {
  return request<WalletFlowPage>({ url: '/api/v1/wallet/flows', method: 'GET', data: { pageNum, pageSize } })
}

/** 发起充值（ruleId 选档 或 amountFen 自定义，二选一） */
export function recharge(data: { ruleId?: number; amountFen?: number }): Promise<RechargeResult> {
  return request<RechargeResult>({ url: '/api/v1/wallet/recharge', method: 'POST', data })
}

/** 模拟充值到账（仅本地开发；生产由微信支付回调替代） */
export function mockPayRecharge(rechargeNo: string): Promise<WalletVO> {
  return request<WalletVO>({ url: `/api/v1/wallet/recharge/${rechargeNo}/mock-pay`, method: 'POST' })
}

/** 储值余额全额支付订单 */
export function payByBalance(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/payments/${orderNo}/balance`, method: 'POST' })
}

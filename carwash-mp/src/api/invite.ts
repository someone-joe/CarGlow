import { request } from '@/utils/request'

/** 契约：openapi.yaml /api/v1/invite/** */
export interface InviteVO {
  inviteId: number
  inviteeMemberId: number
  status: 'BOUND' | 'REWARDED'
  statusLabel: string
  firstOrderNo?: string
  createTime: string
}

export interface InviteListVO {
  list: InviteVO[]
  invitedCount: number
  rewardedCount: number
}

/** 我的邀请码（首次调用生成） */
export function fetchInviteCode(): Promise<string> {
  return request<string>({ url: '/api/v1/invite/code', method: 'GET' })
}

/** 绑定邀请码 */
export function bindInvite(code: string): Promise<void> {
  return request<void>({ url: '/api/v1/invite/bind', method: 'POST', data: { code } })
}

/** 我邀请的人 + 统计 */
export function fetchInviteList(): Promise<InviteListVO> {
  return request<InviteListVO>({ url: '/api/v1/invite/list', method: 'GET' })
}

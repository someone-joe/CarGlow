import type { components } from '@/api/schema'
import { request, setToken } from '@/utils/request'

export type LoginVO = components['schemas']['LoginVO']

/** 微信 code 换 token。契约：openapi.yaml /api/v1/auth/login */
export function login(code: string): Promise<LoginVO> {
  return request<LoginVO>({
    url: '/api/v1/auth/login',
    method: 'POST',
    data: { code },
    auth: false,
  })
}

/** 静默登录：wx.login → 登录接口 → 存 token。并发调用共享同一次登录，避免重复请求 */
let pending: Promise<string> | null = null

export function ensureLogin(): Promise<string> {
  if (pending) {
    return pending
  }
  pending = new Promise<string>((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (res) => {
        // res.code 类型上可能为空，缺 code 直接失败，避免静默带上 undefined 换取错误 openid
        if (!res.code) {
          reject(new Error('wx.login 未返回 code'))
          return
        }
        login(res.code)
          .then((data) => {
            setToken(data.token ?? '')
            resolve(data.token ?? '')
          })
          .catch(reject)
      },
      fail: (err) => reject(err),
    })
  }).finally(() => {
    pending = null
  })
  return pending
}

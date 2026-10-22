import type { components } from '@/api/schema'
import { request, setToken } from '@/utils/request'

export type LoginVO = components['schemas']['LoginVO']

/**
 * 微信 code 换 token。契约：openapi.yaml /api/v1/auth/login。
 *
 * <p>phoneCode 是 `<button open-type="getPhoneNumber">` 授权回调拿到的 code，
 * 由后端换手机号并落库；用户未授权时不传（后端不会覆盖已有手机号）。
 */
export function login(code: string, phoneCode?: string): Promise<LoginVO> {
  return request<LoginVO>({
    url: '/api/v1/auth/login',
    method: 'POST',
    data: phoneCode ? { code, phoneCode } : { code },
    auth: false,
  })
}

/**
 * 主动退出登录后抑制静默登录：直到用户再次显式授权（getPhoneNumber）才恢复。
 *
 * <p>为什么需要它：微信静默登录拿的是 openid 对应的会员，「退出」若只清 token，
 * 下次任何接口的 10002 续登会立刻把会话恢复，用户感知不到退出。抑制后，
 * 未显式授权前 ensureLogin 直接失败，请求层不会再自动续登。
 */
let autoLoginSuppressed = false

export function suppressAutoLogin(): void {
  autoLoginSuppressed = true
}

export function allowAutoLogin(): void {
  autoLoginSuppressed = false
}

/** 静默登录：wx.login → 登录接口 → 存 token。并发调用共享同一次登录，避免重复请求 */
let pending: Promise<string> | null = null

export function ensureLogin(): Promise<string> {
  if (autoLoginSuppressed) {
    return Promise.reject(new Error('已退出登录，等待重新授权'))
  }
  if (pending) {
    return pending
  }
  const p = new Promise<string>((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (res) => {
        // res.code 类型上可能为空，缺 code 直接失败，避免静默带上 undefined 换取错误 openid
        if (!res.code) {
          pending = null
          reject(new Error('wx.login 未返回 code'))
          return
        }
        login(res.code)
          .then((data) => {
            setToken(data.token ?? '')
            pending = null
            resolve(data.token ?? '')
          })
          .catch((err) => {
            pending = null
            reject(err)
          })
      },
      fail: (err) => {
        pending = null
        reject(err)
      },
    })
  })
  pending = p
  return p
}

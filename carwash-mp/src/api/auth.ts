import type { components } from '@/api/schema'
import { request, setToken } from '@/utils/request'

// 小程序运行时的全局 wx 对象；mp-weixin 下 ensureLogin 直连它做静默登录。TypeScript 未内置类型，宽松声明。
declare const wx: any

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

/** 登录整体超时：wx.login/网络偶发卡住时，置空 pending 让后续调用可重试，避免永久毒化 */
const LOGIN_TIMEOUT = 10000

export function ensureLogin(force = false): Promise<string> {
  if (autoLoginSuppressed) {
    return Promise.reject(new Error('已退出登录，等待重新授权'))
  }
  if (pending && !force) {
    return pending
  }
  if (force) {
    // 强制重登：丢弃可能已失败的共享 promise，开启一次全新登录
    pending = null
  }
  const p = new Promise<string>((resolve, reject) => {
    const timer = setTimeout(() => {
      pending = null
      reject(new Error('登录超时'))
    }, LOGIN_TIMEOUT)
    // 直连 wx.login：本应用仅编译到 mp-weixin，uni.login 与其等价；
    // 但开发者工具自动化运行时 uni.login 回调不触发，会导致续登永久挂起，故用 wx.login。
    wx.login({
      success: (res: any) => {
        // res.code 类型上可能为空，缺 code 直接失败，避免静默带上 undefined 换取错误 openid
        if (!res.code) {
          clearTimeout(timer)
          pending = null
          reject(new Error('登录未返回 code'))
          return
        }
        login(res.code)
          .then((data) => {
            clearTimeout(timer)
            setToken(data.token ?? '')
            pending = null
            resolve(data.token ?? '')
          })
          .catch((err) => {
            clearTimeout(timer)
            pending = null
            reject(err)
          })
      },
      fail: (err: any) => {
        clearTimeout(timer)
        pending = null
        reject(err)
      },
    })
  })
  pending = p
  return p
}

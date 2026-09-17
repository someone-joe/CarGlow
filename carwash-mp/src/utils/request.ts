/**
 * 统一请求封装 —— 全项目唯一发请求的地方，禁止页面里直接 uni.request。
 *
 * 职责：拼接基址、自动带 token、拆 {code,msg,data} 信封、统一报错、未登录可重试一次。
 * 契约：openapi.yaml 的 Result schema（code = 0 表示成功）。
 */
import type { components } from '@/api/schema'

/**
 * 后端基址：从 carwash-mp/.env 的 VITE_API_BASE 读取。
 * - 开发者工具模拟器：localhost 即本机
 * - 真机：必须写电脑局域网 IP（.env 里有说明），换 WiFi 后要改
 */
export const BASE_URL = (import.meta.env.VITE_API_BASE as string) || 'http://localhost:8080'

export const TOKEN_KEY = 'carglow_token'

/** 与后端 ApiResult 逐字段一致 */
export interface ApiResult<T> {
  code: number
  msg: string
  data: T
  traceId?: string | null
}

export class ApiError extends Error {
  code: number

  constructor(code: number, msg: string) {
    super(msg)
    this.code = code
  }
}

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
  /** 是否带 token，默认 true；登录等匿名接口传 false */
  auth?: boolean
  /** 为 true 时不弹错误 toast，由调用方自行处理 */
  silent?: boolean
  /** 需要幂等键的接口（下单等），自动生成 UUID 放 Idempotency-Key 头 */
  idempotent?: boolean
}

export function getToken(): string {
  return (uni.getStorageSync(TOKEN_KEY) as string) || ''
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken(): void {
  uni.removeStorageSync(TOKEN_KEY)
}

export function request<T>(options: RequestOptions): Promise<T> {
  return doRequest<T>(options, false)
}

/** 简易 UUID（幂等键用，不需要密码学强度） */
function uuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

function doRequest<T>(options: RequestOptions, retried: boolean): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = { 'Content-Type': 'application/json' }
    if (options.auth !== false) {
      const token = getToken()
      if (token) {
        header.Authorization = `Bearer ${token}`
      }
    }
    // 幂等键：同一键重复提交服务端返回首次结果，防止用户狂点生成多单
    const idempotencyKey = options.idempotent ? uuid() : ''
    if (idempotencyKey) {
      header['Idempotency-Key'] = idempotencyKey
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: (options.data ?? {}) as never,
      header,
      success: (res) => {
        const body = res.data as ApiResult<T>
        if (!body || typeof body.code !== 'number') {
          if (!options.silent) {
            uni.showToast({ title: '服务返回异常', icon: 'none' })
          }
          reject(new ApiError(-1, '响应格式异常'))
          return
        }
        if (body.code === 0) {
          resolve(body.data)
          return
        }
        // 未登录：静默续登后重试一次，页面无感知
        if (body.code === 10002 && !retried) {
          clearToken()
          import('@/api/auth')
            .then(({ ensureLogin }) => {
              ensureLogin()
                .then(() => doRequest<T>(options, true).then(resolve, reject))
                .catch(reject)
            })
            .catch(reject)
          return
        }
        if (!options.silent) {
          uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
        }
        reject(new ApiError(body.code, body.msg))
      },
      fail: (err) => {
        if (!options.silent) {
          uni.showToast({ title: '网络异常，请检查网络', icon: 'none' })
        }
        reject(err)
      },
    })
  })
}

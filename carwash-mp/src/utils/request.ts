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

/**
 * 师傅端 token 与 C 端 token 分开存：同一小程序两套登录态（顾客 / 师傅），
 * 共用一个 key 会互相覆盖——师傅登录后顾客身份丢失，反之亦然。
 */
export const WORKER_TOKEN_KEY = 'carglow_worker_token'
export const WORKER_LOGIN_PAGE = '/pages/worker-login/worker-login'

/** 师傅端接口前缀：这些接口必须带师傅 token，不是会员 token */
export function isWorkerApi(url: string): boolean {
  return url.startsWith('/api/v1/worker')
    || url.startsWith('/api/v1/pick')
    || url.startsWith('/api/v1/station')
}

export function getWorkerToken(): string {
  return (uni.getStorageSync(WORKER_TOKEN_KEY) as string) || ''
}

export function setWorkerToken(token: string): void {
  uni.setStorageSync(WORKER_TOKEN_KEY, token)
}

export function clearWorkerToken(): void {
  uni.removeStorageSync(WORKER_TOKEN_KEY)
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
    const workerApi = isWorkerApi(options.url)
    if (options.auth !== false) {
      const token = workerApi ? getWorkerToken() : getToken()
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
        // 师傅端登录态失效：工号密码无法静默获取，只能跳登录页，不做自动续登
        if (body.code === 10002 && workerApi) {
          clearWorkerToken()
          uni.navigateTo({ url: WORKER_LOGIN_PAGE })
          reject(new ApiError(body.code, body.msg))
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

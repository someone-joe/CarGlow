import { reactive } from 'vue'
import { clearWorkerToken, getWorkerToken, setWorkerToken } from '@/utils/request'
import type { WorkerLoginVO } from '@/api/worker'

/**
 * 师傅（工作人员）登录态。模块级单例，与 store/orderDraft.ts 同一套路，不引入 Pinia。
 *
 * <p>同一小程序两套身份：顾客（微信静默登录）与师傅（工号登录）。
 * token 分开存（见 utils/request 的 WORKER_TOKEN_KEY），这里只放展示用的档案。
 */
export interface WorkerState {
  logged: boolean
  workerId: number
  workerNo: string
  name: string
  siteId: number
  siteName: string
}

const PROFILE_KEY = 'carglow_worker_profile'

export const workerState = reactive<WorkerState>({
  logged: false,
  workerId: 0,
  workerNo: '',
  name: '',
  siteId: 0,
  siteName: '',
})

/** 登录成功：存 token + 档案，并写入内存态 */
export function saveWorkerLogin(vo: WorkerLoginVO): void {
  setWorkerToken(vo.token ?? '')
  uni.setStorageSync(PROFILE_KEY, JSON.stringify(vo))
  workerState.logged = true
  workerState.workerId = vo.workerId ?? 0
  workerState.workerNo = vo.workerNo ?? ''
  workerState.name = vo.name ?? ''
  workerState.siteId = vo.siteId ?? 0
  workerState.siteName = vo.siteName ?? ''
}

/**
 * 恢复登录态（冷启动 / 页面 onShow 调用）。
 * token 有效即视为已登录；token 失效由接口返回 10002 后跳登录页，这里不做校验请求。
 */
export function restoreWorker(): boolean {
  if (!getWorkerToken()) {
    clearWorkerState()
    return false
  }
  const raw = uni.getStorageSync(PROFILE_KEY) as string
  if (raw) {
    try {
      const vo = JSON.parse(raw) as WorkerLoginVO
      workerState.logged = true
      workerState.workerId = vo.workerId ?? 0
      workerState.workerNo = vo.workerNo ?? ''
      workerState.name = vo.name ?? ''
      workerState.siteId = vo.siteId ?? 0
      workerState.siteName = vo.siteName ?? ''
    } catch {
      // 档案解析失败不影响登录态，token 仍在即可用
    }
  } else {
    workerState.logged = true
  }
  return true
}

/** 退出师傅端：只清师傅身份，不动 C 端会员 token */
export function logoutWorker(): void {
  clearWorkerToken()
  uni.removeStorageSync(PROFILE_KEY)
  clearWorkerState()
}

function clearWorkerState(): void {
  workerState.logged = false
  workerState.workerId = 0
  workerState.workerNo = ''
  workerState.name = ''
  workerState.siteId = 0
  workerState.siteName = ''
}

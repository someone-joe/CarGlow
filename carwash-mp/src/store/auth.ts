import { reactive } from 'vue'
import { login } from '@/api/auth'

/**
 * C 端手机号授权门控（按需触发）。模块级单例，同 store/orderDraft 套路，不引入 Pinia。
 *
 * 用法：在强用户相关动作（下单/充值/查订单）前调用 requirePhone(() => { ...原动作... })。
 * 已绑手机号 → 立即放行；未绑 → 弹全局授权浮层，用户授权成功后自动继续原动作。
 *
 * 手机号只在「我的」页或本浮层里通过微信 getPhoneNumber 授权，前端不碰明文（见 api/auth）。
 */
export interface AuthGateState {
  /** 是否已绑定手机号；App 启动与授权成功后刷新 */
  phoneBound: boolean
  /** 授权浮层是否可见 */
  visible: boolean
}

const state = reactive<AuthGateState>({
  phoneBound: false,
  visible: false,
})

/** 授权成功后要继续执行的动作；为空表示无待续操作 */
let pending: (() => void) | null = null

/** 静默登录刷新 phoneBound（不弹窗；App 启动时调用） */
export function refreshPhoneBound(): void {
  uni.login({
    provider: 'weixin',
    success: (res) => {
      if (!res.code) return
      login(res.code)
        .then((data) => {
          state.phoneBound = data.phoneBound === true
        })
        .catch(() => {})
    },
  })
}

/** 需要手机号才能继续的动作。已绑 → 立即执行；未绑 → 记下动作并弹授权浮层 */
export function requirePhone(action: () => void): void {
  if (state.phoneBound) {
    action()
    return
  }
  pending = action
  state.visible = true
}

/** 浮层里点「允许获取手机号」后的回调：拿 phoneCode 重新登录，由后端换号落库并继续原动作 */
export function submitPhoneCode(phoneCode: string): void {
  uni.login({
    provider: 'weixin',
    success: (res) => {
      if (!res.code) return
      login(res.code, phoneCode)
        .then((data) => {
          state.phoneBound = data.phoneBound === true
          if (state.phoneBound) {
            state.visible = false
            const act = pending
            pending = null
            act?.()
          } else {
            // dev 模式（无 secret）后端不落号，或微信侧失败：提示重试，保持浮层
            uni.showToast({ title: '绑定未生效，请重试', icon: 'none' })
          }
        })
        .catch(() => {})
    },
  })
}

/** 关闭授权浮层并放弃待续动作 */
export function cancelPhoneAuth(): void {
  state.visible = false
  pending = null
}

export const authGate = state

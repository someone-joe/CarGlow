import { reactive } from 'vue'

/**
 * 用户本地选择的小区。
 *
 * <p>V1.0 后端没有「切换小区」接口（/site/current 只返回默认站点），
 * 所以用户的选择先落本地存储：界面按选择回显，格口查询按契约的 communityId
 * 参数过滤（后端已支持）。等切换接口上线后，这里改为服务端真值即可，
 * 调用方（SiteBar / 各页 fetchCabinets）不需要再动。
 */
export interface SelectedCommunity {
  communityId?: number
  name?: string
}

const STORAGE_KEY = 'carglow_selected_community'

export const selectedCommunity = reactive<SelectedCommunity>({})

let hydrated = false

/** 首次读取时把本地存储 hydrate 进状态；各页 load 时调用，保证读取顺序无关 */
export function hydrateSelectedCommunity(): void {
  if (hydrated) return
  hydrated = true
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw) as SelectedCommunity
    selectedCommunity.communityId = parsed.communityId
    selectedCommunity.name = parsed.name
  } catch {
    // 存储损坏时按「未选择」处理，不影响默认站点
  }
}

/** 当前所选小区 ID（未选择返回 undefined）；内部会先 hydrate */
export function selectedCommunityId(): number | undefined {
  hydrateSelectedCommunity()
  return selectedCommunity.communityId ?? undefined
}

/** 记住用户选择；存储失败时当次会话仍生效，不阻断切换 */
export function setSelectedCommunity(c: SelectedCommunity): void {
  selectedCommunity.communityId = c.communityId
  selectedCommunity.name = c.name
  try {
    uni.setStorageSync(STORAGE_KEY, JSON.stringify({ communityId: c.communityId, name: c.name }))
  } catch {
    // 忽略：本地偏好写不进去不影响本次回显
  }
}

/** 恢复默认站点（清除本地选择） */
export function clearSelectedCommunity(): void {
  selectedCommunity.communityId = undefined
  selectedCommunity.name = undefined
  try {
    uni.removeStorageSync(STORAGE_KEY)
  } catch {
    // 忽略
  }
}

<template>
  <!-- 新版（首页 / 下单页）：白底大卡片 -->
  <view v-if="variant === 'card'" class="site-card" @click="onTap">
    <view class="site-main">
      <text class="site-name">{{ displayName || '加载中…' }}</text>
      <text class="site-switch">切换 ›</text>
    </view>
    <view v-if="statusNotice" class="site-card-notice">
      <view class="site-dot" :class="noticeLevel" />
      <text class="site-card-notice-text">{{ statusNotice }}</text>
    </view>
  </view>
  <!-- 旧版：其余未改版页面保持原样 -->
  <view v-else class="site-bar" @click="onTap">
    <view class="site-main">
      <text class="site-name">{{ displayName || '加载中…' }}</text>
      <text class="site-switch">切换 ▾</text>
    </view>
    <text v-if="statusNotice" class="site-notice">{{ statusNotice }}</text>
  </view>
</template>

<script setup lang="ts">
import { onShow, onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { fetchCommunities, fetchSiteCurrent, type CommunityVO, type SiteVO } from '@/api/home'

/** variant=card 为新版视觉（首页/下单页）；默认 bar 保持旧版，未改版页面不受影响 */
defineProps<{
  variant?: 'bar' | 'card'
}>()

const site = ref<SiteVO | null>(null)
const communities = ref<CommunityVO[]>([])
const displayName = ref('')
const statusNotice = ref('')

/** 提示级别 → 圆点颜色：停业=红、雨天=橙；由后端 serviceStatus 决定，前端不写死文案 */
const noticeLevel = computed(() => {
  if (site.value?.serviceStatus === 'CLOSED') return 'error'
  if (site.value?.serviceStatus === 'RAINY') return 'warning'
  return ''
})

/** 站点状态 → 顶部提示文案，停止业/雨天模式由后端下发，前端不写死 */
function noticeOf(s: SiteVO): string {
  if (s.serviceStatus === 'CLOSED') {
    return s.closedNotice || '本服务站临时停业'
  }
  if (s.serviceStatus === 'RAINY') {
    return '雨天模式：洗车送车内雾化消毒'
  }
  return ''
}

async function load(): Promise<void> {
  try {
    const [s, list] = await Promise.all([fetchSiteCurrent(), fetchCommunities()])
    site.value = s
    communities.value = list || []
    displayName.value = s.displayName || [s.communityName, s.siteName].filter(Boolean).join(' · ')
    statusNotice.value = noticeOf(s)
  } catch {
    displayName.value = 'CarGlow'
  }
}

function onTap(): void {
  const items = (communities.value || []).map((c) => c.name || `小区${c.communityId}`)
  if (items.length === 0) {
    uni.showToast({ title: '暂无可切换的小区', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: items,
    success: (res) => {
      const chosen = communities.value[res.tapIndex]
      uni.showToast({ title: `已选择 ${chosen.name || ''}`, icon: 'none' })
      // V1.0 后端未提供切换接口，仅本地提示；数据仍以 site/current 为准
    },
  })
}

onLoad(load)
onShow(load)
</script>

<style>
.site-bar {
  background: #1a73e8;
  color: #fff;
  padding: 20rpx 32rpx;
  box-sizing: border-box;
}

.site-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.site-name {
  font-size: 30rpx;
  font-weight: 600;
}

.site-switch {
  font-size: 24rpx;
  opacity: 0.85;
}

.site-notice {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 8rpx;
  padding: 8rpx 16rpx;
}

/* ---- 卡片变体（Apple HIG 新版） ---- */
.site-card {
  box-sizing: border-box;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.site-card .site-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.site-card .site-name {
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
}

.site-card .site-switch {
  font-size: var(--cg-font-caption);
  color: var(--cg-primary);
}

.site-card-notice {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
}

.site-dot {
  flex: none;
  width: 12rpx;
  height: 12rpx;
  margin-right: 16rpx;
  border-radius: 50%;
  background: var(--cg-warning);
}

.site-dot.error {
  background: var(--cg-error);
}

.site-card-notice-text {
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}
</style>

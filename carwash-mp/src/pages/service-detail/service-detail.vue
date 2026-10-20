<template>
  <view class="cg-page service-detail">
    <view v-if="loading" class="cg-empty">加载中…</view>

    <block v-else-if="detail">
      <!-- 封面 -->
      <image v-if="detail.coverUrl" class="cover" :src="detail.coverUrl" mode="aspectFill" />
      <image v-else class="cover cover-empty" src="/static/logo.png" mode="aspectFit" />

      <!-- 头部信息 -->
      <view class="head">
        <view class="head-top">
          <text class="name">{{ detail.name }}</text>
          <text class="price">¥{{ fen2yuan(detail.displayPrice) }}</text>
        </view>
        <text v-if="detail.subtitle" class="subtitle">{{ detail.subtitle }}</text>
        <view class="meta-row">
          <text v-if="detail.originPrice && detail.originPrice !== detail.displayPrice" class="origin">
            ¥{{ fen2yuan(detail.originPrice) }}
          </text>
          <text class="tag">预计工时 {{ detail.workMinutes || '—' }} 分钟</text>
          <text class="tag">¥0 取送车</text>
        </view>
      </view>

      <view class="panel">
        <text class="panel-title">服务内容</text>
        <view v-for="(it, i) in detail.items || []" :key="i" class="li">· {{ it }}</view>
        <view v-if="!detail.items?.length" class="li muted">暂无明细</view>
      </view>

      <view v-if="detail.applicableModels" class="panel">
        <text class="panel-title">适用车型</text>
        <text class="text">{{ detail.applicableModels }}</text>
      </view>

      <view v-if="detail.notIncluded?.length" class="panel">
        <text class="panel-title">不包含项</text>
        <view v-for="(it, i) in detail.notIncluded" :key="i" class="li">· {{ it }}</view>
      </view>

      <view v-if="detail.notices?.length" class="panel">
        <text class="panel-title">注意事项</text>
        <view v-for="(it, i) in detail.notices" :key="i" class="li">· {{ it }}</view>
      </view>

      <view v-if="detail.sampleImages?.length" class="panel">
        <text class="panel-title">前后对比样图</text>
        <view class="gallery">
          <image v-for="(img, i) in detail.sampleImages" :key="i" class="sample" :src="img" mode="aspectFill" @click="preview(img)" />
        </view>
      </view>

      <!-- 底部下单条 -->
      <view class="bottom-bar">
        <view class="bb-left">
          <text class="bb-label">服务价格</text>
          <text class="bb-price">¥{{ fen2yuan(detail.displayPrice) }}</text>
        </view>
        <text class="bottom-btn" @click="book">立即下单</text>
      </view>
    </block>

    <view v-else class="fallback">
      <text class="fallback-title">服务不存在或已下架</text>
      <text class="fallback-sub">链接可能已过期，去看看其他服务吧</text>
      <view class="fallback-btn" @click="goBack">返 回</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchServiceDetail, type ServiceDetailVO } from '@/api/home'
import { setOrderDraft } from '@/store/orderDraft'

const detail = ref<ServiceDetailVO | null>(null)
const loading = ref(true)
const serviceId = ref<number>(0)

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function preview(url: string): void {
  uni.previewImage({ urls: detail.value?.sampleImages || [url], current: url })
}

/** 兜底页返回：栈空（如直接扫码进入）时回首页 */
function goBack(): void {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/home/home' }) })
}

function book(): void {
  setOrderDraft({ serviceId: serviceId.value })
  // 下单页不是 tabBar 页面（tabBar 首页是 home），必须用 navigateTo，switchTab 会失败
  uni.navigateTo({ url: '/pages/index/index' })
}

onLoad((opts) => {
  serviceId.value = Number((opts as { serviceId?: string }).serviceId) || 0
  if (!serviceId.value) {
    loading.value = false
    return
  }
  fetchServiceDetail(serviceId.value)
    .then((d) => {
      detail.value = d
    })
    .catch(() => {
      detail.value = null
    })
    .finally(() => {
      loading.value = false
    })
})
</script>

<style>
/* 底部下单条占位 */
.service-detail {
  padding-bottom: 220rpx;
}

/* ---- 兜底态 ---- */
.fallback {
  padding: 160rpx 0;
  text-align: center;
}

.fallback-title {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-text-sub);
}

.fallback-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.fallback-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin: 48rpx auto 0;
  padding: 0 48rpx;
  background: var(--cg-card);
  border: 1rpx solid var(--cg-border);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: var(--cg-title);
}

/* ---- 封面：通栏大圆角 ---- */
.cover {
  display: block;
  width: 100%;
  height: 420rpx;
  margin-top: 32rpx;
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.cover-empty {
  background: var(--cg-fill);
}

/* ---- 头部信息 ---- */
.head {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.head-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.name {
  flex: 1;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.price {
  margin-left: 32rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 24rpx;
}

.origin {
  margin-right: 24rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  text-decoration: line-through;
}

.tag {
  margin: 0 24rpx 16rpx 0;
  padding: 8rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 内容面板 ---- */
.panel {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.li {
  padding: 16rpx 0;
  font-size: var(--cg-font-body);
  color: var(--cg-text);
  line-height: 1.6;
}

.li.muted {
  color: var(--cg-text-aux);
}

.text {
  font-size: var(--cg-font-body);
  color: var(--cg-text);
  line-height: 1.6;
}

/* ---- 样图 ---- */
.gallery {
  display: flex;
  flex-wrap: wrap;
}

.sample {
  width: 200rpx;
  height: 200rpx;
  margin: 0 16rpx 16rpx 0;
  border-radius: var(--cg-radius-md);
}

/* ---- 底部下单条：轻毛玻璃 ---- */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  padding: 16rpx 40rpx calc(16rpx + env(safe-area-inset-bottom));
  background: var(--cg-glass-bg);
  -webkit-backdrop-filter: blur(var(--cg-glass-blur));
  backdrop-filter: blur(var(--cg-glass-blur));
  border-top: var(--cg-glass-border);
  box-shadow: 0 -4rpx 40rpx rgba(0, 0, 0, 0.06);
}

.bb-label {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.bb-price {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.bottom-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 280rpx;
  height: 104rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}
</style>

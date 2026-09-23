<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

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
.page {
  padding-bottom: 160rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 80rpx 0;
}

/* ---- 空态兜底页 ---- */
.fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.fallback-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.fallback-sub {
  margin-top: 14rpx;
  font-size: 26rpx;
  color: #8a9a98;
}

.fallback-btn {
  margin-top: 60rpx;
  background: #14342f;
  color: #fff;
  font-size: 30rpx;
  padding: 22rpx 80rpx;
  border-radius: 44rpx;
}

.cover {
  width: 100%;
  height: 380rpx;
  background: #eef3f2;
}

.cover-empty {
  padding: 40rpx 0;
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
}

.head {
  background: #fff;
  border-radius: 20rpx;
  margin: -40rpx 24rpx 20rpx;
  padding: 30rpx;
  position: relative;
}

.head-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.name {
  font-size: 36rpx;
  font-weight: 700;
  color: #1b2b2a;
}

.price {
  font-size: 40rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #8a9a98;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 18rpx;
}

.origin {
  font-size: 24rpx;
  color: #b6c2c0;
  text-decoration: line-through;
}

.tag {
  font-size: 22rpx;
  color: #00aeb5;
  background: #e6f7f7;
  border-radius: 20rpx;
  padding: 6rpx 16rpx;
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin: 0 24rpx 20rpx;
}

.panel-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 18rpx;
}

.li {
  font-size: 26rpx;
  color: #4a5a58;
  line-height: 1.9;
}

.li.muted {
  color: #b6c2c0;
}

.text {
  font-size: 26rpx;
  color: #4a5a58;
  line-height: 1.8;
}

.gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.sample {
  width: 200rpx;
  height: 200rpx;
  border-radius: 14rpx;
  background: #eef3f2;
}

/* ---- 底部下单条 ---- */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.bb-left {
  display: flex;
  flex-direction: column;
}

.bb-label {
  font-size: 22rpx;
  color: #8a9a98;
}

.bb-price {
  font-size: 38rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.bottom-btn {
  background: #14342f;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  padding: 24rpx 60rpx;
  border-radius: 40rpx;
}
</style>

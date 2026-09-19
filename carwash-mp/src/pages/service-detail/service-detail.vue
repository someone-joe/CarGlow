<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="detail">
      <image v-if="detail.coverUrl" class="cover" :src="detail.coverUrl" mode="aspectFill" />
      <image v-else class="cover cover-empty" src="/static/logo.png" mode="aspectFit" />

      <view class="head">
        <text class="name">{{ detail.name }}</text>
        <text v-if="detail.subtitle" class="subtitle">{{ detail.subtitle }}</text>
        <view class="price-row">
          <text class="price">¥{{ fen2yuan(detail.displayPrice) }}</text>
          <text v-if="detail.originPrice && detail.originPrice !== detail.displayPrice" class="origin">
            ¥{{ fen2yuan(detail.originPrice) }}
          </text>
          <text class="pickup">¥0 取送车</text>
        </view>
        <text class="work">预计工时 {{ detail.workMinutes || '—' }} 分钟</text>
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

      <view class="bottom-bar">
        <text class="bottom-price">¥{{ fen2yuan(detail.displayPrice) }}</text>
        <text class="bottom-btn" @click="book">立即下单</text>
      </view>
    </block>

    <view v-else class="tip">服务不存在</view>
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

function book(): void {
  setOrderDraft({ serviceId: serviceId.value })
  uni.switchTab({ url: '/pages/index/index' })
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
  padding-bottom: 140rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #999;
  padding: 80rpx 0;
}

.cover {
  width: 100%;
  height: 360rpx;
  background: #eef1f5;
}

.cover-empty {
  opacity: 0.3;
}

.head {
  background: #fff;
  padding: 28rpx;
}

.name {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: #222;
}

.subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #888;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-top: 16rpx;
}

.price {
  font-size: 42rpx;
  color: #e8700a;
  font-weight: 600;
}

.origin {
  font-size: 26rpx;
  color: #aaa;
  text-decoration: line-through;
}

.pickup {
  font-size: 24rpx;
  color: #1a73e8;
}

.work {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #999;
}

.panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin: 24rpx;
}

.panel-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 16rpx;
}

.li {
  font-size: 28rpx;
  color: #555;
  line-height: 1.8;
}

.li.muted {
  color: #bbb;
}

.text {
  font-size: 28rpx;
  color: #555;
}

.gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.sample {
  width: 200rpx;
  height: 200rpx;
  border-radius: 12rpx;
  background: #eef1f5;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eee;
}

.bottom-price {
  font-size: 40rpx;
  color: #e8700a;
  font-weight: 600;
}

.bottom-btn {
  background: #1a73e8;
  color: #fff;
  font-size: 30rpx;
  padding: 20rpx 60rpx;
  border-radius: 12rpx;
}
</style>

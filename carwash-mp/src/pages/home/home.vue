<template>
  <view class="page">
    <!-- 品牌头图 -->
    <view class="hero">
      <text class="hero-title">夜间代客洗车</text>
      <text class="hero-sub">今晚下单 · 明早干净上路</text>
    </view>

    <SiteBar />

    <!-- 卖点条 -->
    <view class="features">
      <view class="feature">
        <text class="f-ico">🚗</text>
        <text class="f-text">上门取送车</text>
      </view>
      <view class="feature">
        <text class="f-ico">🌙</text>
        <text class="f-text">夜间随心约</text>
      </view>
      <view class="feature">
        <text class="f-ico">🛡️</text>
        <text class="f-text">全程影像留痕</text>
      </view>
    </view>

    <!-- 服务入口 -->
    <view class="entry" @click="goServices">
      <view class="entry-main">
        <text class="entry-title">洗车服务</text>
        <text class="entry-sub">中央站集中洗 · 师傅代取送</text>
      </view>
      <text class="entry-arrow">›</text>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <view class="block-title">本月爆品</view>
      <view v-for="item in services" :key="item.serviceId" class="hot" @click="goCreate(item.serviceId as number)">
        <view class="hot-main">
          <text class="hot-name">{{ item.name }}</text>
          <text class="hot-sub">约 {{ item.workMinutes }} 分钟</text>
        </view>
        <view class="hot-right">
          <view class="hot-price">
            <text class="price-now">¥{{ fen2yuan(item.displayPrice) }}</text>
            <text v-if="item.originPrice && item.originPrice !== item.displayPrice" class="price-old">
              ¥{{ fen2yuan(item.originPrice) }}
            </text>
          </view>
          <view class="hot-btn">立即下单</view>
        </view>
      </view>
      <view v-if="!services.length" class="empty">暂无可预约的服务</view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchServices, type ServiceVO } from '@/api/catalog'
import SiteBar from '@/components/SiteBar.vue'
import { setOrderDraft } from '@/store/orderDraft'

const services = ref<ServiceVO[]>([])
const loading = ref(true)

/** 金额一律以「分」传给后端，展示时才转元 */
function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function goServices(): void {
  uni.navigateTo({ url: '/pages/services/services' })
}

/** 点爆品直接进入下单页并预选该服务 */
function goCreate(serviceId: number): void {
  setOrderDraft({ serviceId })
  uni.navigateTo({ url: '/pages/index/index' })
}

onShow(() => {
  loading.value = true
  fetchServices()
    .then((list) => (services.value = list || []))
    .catch(() => (services.value = []))
    .finally(() => (loading.value = false))
})
</script>

<style>
.page {
  padding: 0 24rpx 60rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.hero {
  margin: 0 -24rpx 20rpx;
  padding: 48rpx 32rpx 40rpx;
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
}

.hero-title {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
}

.hero-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.features {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx 0;
  margin-bottom: 20rpx;
}

.feature {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.f-ico {
  font-size: 44rpx;
}

.f-text {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6b7b79;
}

.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx 28rpx;
  margin-bottom: 28rpx;
}

.entry-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.entry-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.entry-arrow {
  font-size: 36rpx;
  color: #b6c2c0;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.block-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding: 8rpx 4rpx 20rpx;
}

.hot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

/* 左侧文本区：flex:1 + min-width:0 才能被压缩换行，不会把右侧按钮挤变形 */
.hot-main {
  flex: 1;
  min-width: 0;
  margin-right: 20rpx;
}

.hot-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1b2b2a;
  /* 服务名最长限 2 行，超出省略（bug092402） */
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.hot-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.hot-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.hot-price {
  text-align: right;
  margin-right: 24rpx;
}

.price-now {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.price-old {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #b6c2c0;
  text-decoration: line-through;
}

.hot-btn {
  background: #14342f;
  color: #fff;
  font-size: 25rpx;
  padding: 14rpx 28rpx;
  border-radius: 32rpx;
  /* 固定大小不随标题长度变化（bug092401） */
  flex-shrink: 0;
  white-space: nowrap;
}

.empty {
  color: #8a9a98;
  font-size: 26rpx;
  padding: 30rpx 0;
}
</style>

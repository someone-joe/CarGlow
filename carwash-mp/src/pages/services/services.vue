<template>
  <view class="page">
    <SiteBar />

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 分类 Tab：来自后台 service-categories，支持空态 -->
      <view class="tabs">
        <view
          v-for="c in categories"
          :key="c.categoryId"
          :class="['tab', activeCat === c.categoryId && 'tab-active']"
          @click="activeCat = c.categoryId"
        >
          {{ c.name }}
        </view>
        <view v-if="!categories.length" class="tab tab-active">洗车服务</view>
      </view>

      <view v-if="!currentList.length" class="empty">
        <text class="empty-title">该分类暂无服务</text>
        <text class="empty-sub">去其他分类看看吧</text>
      </view>

      <!-- 服务卡片：价格/工时/取送费全部后端下发，前端不写死任何价格 -->
      <view v-for="item in currentList" :key="item.serviceId" class="card" :class="{ disabled: item.soldOut }">
        <image v-if="item.coverUrl" class="cover" :src="item.coverUrl" mode="aspectFill" />
        <view class="card-body">
          <view class="card-top">
            <text class="name">{{ item.name }}</text>
            <text v-if="item.soldOut" class="badge">今夜已约满</text>
          </view>
          <text v-if="item.subtitle" class="subtitle">{{ item.subtitle }}</text>

          <view class="price-row">
            <text class="price">¥{{ fen2yuan(item.displayPrice) }}</text>
            <text v-if="item.originPrice && item.originPrice !== item.displayPrice" class="origin">
              ¥{{ fen2yuan(item.originPrice) }}
            </text>
          </view>

          <view class="meta">
            <text class="work">约 {{ item.workMinutes || '—' }} 分钟</text>
            <text class="pickup">¥0 取送车 · 免代驾费</text>
          </view>

          <view class="card-actions">
            <text class="detail" @click="goDetail(item.serviceId)">详情 ›</text>
            <text v-if="!item.soldOut" class="book" @click="book(item.serviceId)">确定下单</text>
            <text v-else class="book book-disabled">明晚可约</text>
          </view>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SiteBar from '@/components/SiteBar.vue'
import { fetchServiceCategories, type ServiceCategoryVO } from '@/api/home'
import { fetchServices as fetchServiceList, type ServiceVO } from '@/api/catalog'
import { setOrderDraft } from '@/store/orderDraft'

const categories = ref<ServiceCategoryVO[]>([])
const services = ref<ServiceVO[]>([])
const activeCat = ref<number | undefined>(undefined)
const loading = ref(true)

const currentList = computed(() =>
  services.value.filter((s) => activeCat.value === undefined || s.categoryId === activeCat.value)
)

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [cats, list] = await Promise.all([fetchServiceCategories(), fetchServiceList()])
    categories.value = cats || []
    services.value = list || []
    activeCat.value = categories.value[0]?.categoryId ?? services.value[0]?.categoryId
  } catch {
    services.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(serviceId?: number): void {
  if (!serviceId) return
  uni.navigateTo({ url: `/pages/service-detail/service-detail?serviceId=${serviceId}` })
}

/**
 * 下单页不是 tabBar 页面（tabBar 首页是 home，下单页是普通页面），
 * 必须用 navigateTo 而不是 switchTab，否则跳转会失败。
 */
function book(serviceId?: number): void {
  if (!serviceId) return
  setOrderDraft({ serviceId })
  uni.navigateTo({ url: '/pages/index/index' })
}

onShow(load)
</script>

<style>
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 8rpx;
  margin-bottom: 24rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  font-size: 28rpx;
  color: #6b7b79;
  border-radius: 14rpx;
}

.tab-active {
  background: #00aeb5;
  color: #fff;
  font-weight: 600;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-title {
  font-size: 30rpx;
  color: #1b2b2a;
}

.empty-sub {
  font-size: 26rpx;
  color: #8a9a98;
  margin-top: 12rpx;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}

.card.disabled {
  opacity: 0.6;
}

.cover {
  width: 100%;
  height: 240rpx;
  background: #eef3f2;
}

.card-body {
  padding: 26rpx;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.name {
  font-size: 32rpx;
  color: #1b2b2a;
  font-weight: 600;
}

.badge {
  font-size: 22rpx;
  color: #ff5b4a;
  background: #ffeeeb;
  border-radius: 20rpx;
  padding: 6rpx 16rpx;
}

.subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  color: #8a9a98;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 14rpx;
  margin-top: 18rpx;
}

.price {
  font-size: 40rpx;
  color: #ff5b4a;
  font-weight: 700;
}

.origin {
  font-size: 24rpx;
  color: #b6c2c0;
  text-decoration: line-through;
}

.meta {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.pickup {
  color: #00aeb5;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 22rpx;
}

.detail {
  font-size: 27rpx;
  color: #8a9a98;
}

.book {
  background: #14342f;
  color: #fff;
  font-size: 27rpx;
  padding: 14rpx 38rpx;
  border-radius: 34rpx;
}

.book-disabled {
  background: #cfe0de;
  color: #fff;
}
</style>

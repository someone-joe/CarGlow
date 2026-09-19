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
            <text class="pickup">¥0 取送车 · 免代驾费</text>
          </view>

          <view class="meta">
            <text>预计工时 {{ item.workMinutes || '—' }} 分钟</text>
            <text class="quality">为保障质量，时长视车况而定</text>
          </view>

          <view class="card-actions">
            <text class="detail" @click="goDetail(item.serviceId)">详情 ›</text>
            <text v-if="!item.soldOut" class="book" @click="book(item.serviceId)">确定下单</text>
            <text v-else class="book book-disabled">明晚可约</text>
          </view>
        </view>
      </view>
    </block>

    <!-- 底部悬浮下单条：随选中服务出现（这里简化为点击卡片直接下单） -->
  </view>
</template>

<script setup lang="ts">
import { computed, onShow, ref } from 'vue'
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

function goDetail(serviceId: number): void {
  uni.navigateTo({ url: `/pages/service-detail/service-detail?serviceId=${serviceId}` })
}

function book(serviceId: number): void {
  setOrderDraft({ serviceId })
  uni.switchTab({ url: '/pages/index/index' })
}

onShow(load)
</script>

<style>
.page {
  padding: 24rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #999;
  padding: 60rpx 0;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 24rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 28rpx;
  color: #666;
  border-radius: 12rpx;
}

.tab-active {
  background: #1a73e8;
  color: #fff;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-title {
  font-size: 30rpx;
  color: #333;
}

.empty-sub {
  font-size: 26rpx;
  color: #999;
  margin-top: 12rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
}

.card.disabled {
  opacity: 0.6;
}

.cover {
  width: 100%;
  height: 260rpx;
  background: #eef1f5;
}

.card-body {
  padding: 24rpx;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.name {
  font-size: 32rpx;
  color: #222;
  font-weight: 600;
}

.badge {
  font-size: 22rpx;
  color: #d93025;
  background: #fdecec;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
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
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.price {
  font-size: 38rpx;
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

.meta {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #999;
}

.quality {
  display: block;
  margin-top: 6rpx;
  color: #bbb;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.detail {
  font-size: 28rpx;
  color: #666;
}

.book {
  background: #1a73e8;
  color: #fff;
  font-size: 28rpx;
  padding: 14rpx 36rpx;
  border-radius: 12rpx;
}

.book-disabled {
  background: #b8c6da;
}
</style>

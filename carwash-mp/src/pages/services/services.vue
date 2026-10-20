<template>
  <view class="cg-page services">
    <SiteBar variant="card" />

    <view v-if="loading" class="cg-empty">加载中…</view>

    <block v-else>
      <!-- 分类 Tab：来自后台 service-categories，支持空态 -->
      <view class="cg-tabs">
        <view
          v-for="c in categories"
          :key="c.categoryId"
          class="cg-tab"
          :class="{ 'is-active': activeCat === c.categoryId }"
          @click="activeCat = c.categoryId"
        >
          {{ c.name }}
        </view>
        <view v-if="!categories.length" class="cg-tab is-active">洗车服务</view>
      </view>

      <view v-if="!currentList.length" class="cg-empty">
        <text class="empty-title">该分类暂无服务</text>
        <text class="empty-sub">去其他分类看看吧</text>
      </view>

      <!-- 服务卡片：价格/工时/取送费全部后端下发，前端不写死任何价格 -->
      <view v-for="item in currentList" :key="item.serviceId" class="cg-card card" :class="{ disabled: item.soldOut }">
        <image v-if="item.coverUrl" class="cover" :src="item.coverUrl" mode="aspectFill" />
        <view class="card-body">
          <view class="card-top">
            <text class="name">{{ item.name }}</text>
            <text v-if="item.soldOut" class="cg-status is-busy badge">今夜已约满</text>
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
            <view class="cg-btn-text detail" @click="goDetail(item.serviceId)">详情</view>
            <view v-if="!item.soldOut" class="cg-btn cg-btn-primary book" @click="book(item.serviceId)">确定下单</view>
            <view v-else class="cg-btn cg-btn-secondary book book-disabled">明晚可约</view>
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
    const [cats, list] = await Promise.all([fetchServiceCategories(), fetchServiceList('UNSENSITIVE')])
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
/* tabBar 页：底部留白需避开原生 tabBar */
.services {
  padding-bottom: calc(200rpx + env(safe-area-inset-bottom));
}

.empty-title {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-text-sub);
}

.empty-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

/* ---- 服务卡片 ---- */
.card {
  margin-bottom: 32rpx;
  padding: 0;
  overflow: hidden;
}

.card.disabled {
  opacity: 0.6;
}

.cover {
  display: block;
  width: 100%;
  height: 240rpx;
}

.card-body {
  padding: 32rpx 40rpx 40rpx;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.name {
  flex: 1;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.badge {
  flex: none;
  margin-left: 24rpx;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.price-row {
  display: flex;
  align-items: baseline;
  margin-top: 24rpx;
}

.price {
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.origin {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  text-decoration: line-through;
}

.meta {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
}

.work {
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.pickup {
  margin-left: 24rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-success);
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
}

.detail {
  min-height: 104rpx;
}

.book {
  width: 240rpx;
}

.book-disabled {
  opacity: 0.5;
}
</style>

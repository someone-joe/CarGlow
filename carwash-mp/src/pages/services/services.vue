<template>
  <view class="cg-page services">
    <SiteBar />

    <!-- 页头：一句话定位 -->
    <view class="svc-head">
      <text class="svc-head-title">洗车服务</text>
      <text class="svc-head-sub">夜间中央站精洗 · 师傅上门取送 · 全程影像留痕</text>
    </view>

    <view v-if="loading" class="cg-empty">加载中…</view>

    <block v-else>
      <!-- 分类：横向滚动 pill，商品分类可无限扩展不挤爆布局 -->
      <scroll-view class="cats" scroll-x enhanced show-scrollbar="false">
        <view
          v-for="c in categories"
          :key="c.categoryId"
          class="cg-pill cat"
          :class="{ 'is-active': activeCat === c.categoryId }"
          @click="activeCat = c.categoryId"
        >
          {{ c.name }}
        </view>
        <view v-if="!categories.length" class="cg-pill cat is-active">洗车服务</view>
      </scroll-view>

      <view v-if="!currentList.length" class="cg-empty">该分类暂无服务</view>

      <!-- 商品网格：两列；点卡进详情，主按钮直接下单 -->
      <view class="products">
        <view
          v-for="item in currentList"
          :key="item.serviceId"
          class="cg-card product"
          :class="{ disabled: item.soldOut }"
          @click="goDetail(item.serviceId)"
        >
          <view class="product-media">
            <image v-if="item.coverUrl" class="product-cover" :src="item.coverUrl" mode="aspectFill" />
            <view v-else class="product-cover product-cover-ph">
              <view class="ph-art">
                <view class="cg-art-car is-sm">
                  <view class="art-cabin" />
                  <view class="art-body" />
                  <view class="art-wheel w-back" />
                  <view class="art-wheel w-front" />
                </view>
              </view>
            </view>
            <text v-if="item.soldOut" class="cg-status is-busy product-badge">今夜已约满</text>
          </view>
          <view class="product-body">
            <text class="product-name">{{ item.name }}</text>
            <text v-if="item.subtitle" class="product-sub">{{ item.subtitle }}</text>
            <view class="product-tags">
              <text class="product-tag">约 {{ item.workMinutes || '—' }} 分钟</text>
              <text class="product-tag">¥0 取送</text>
            </view>
            <view class="product-price">
              <text class="price-now">¥{{ fen2yuan(item.displayPrice) }}</text>
              <text v-if="item.originPrice && item.originPrice !== item.displayPrice" class="price-old">
                ¥{{ fen2yuan(item.originPrice) }}
              </text>
            </view>
            <view
              class="cg-btn cg-btn-primary product-btn"
              :class="{ 'is-disabled': item.soldOut }"
              @click.stop="book(item.serviceId)"
            >
              {{ item.soldOut ? '明晚可约' : '立即下单' }}
            </view>
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

/* ---- 页头 ---- */
.svc-head {
  margin: 32rpx 8rpx 8rpx;
}

.svc-head-title {
  display: block;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.svc-head-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 分类：横向滚动 pill ---- */
.cats {
  width: 100%;
  white-space: nowrap;
}

.cat {
  display: inline-flex;
  margin: 24rpx 16rpx 24rpx 0;
}

.cat.is-active {
  background: var(--cg-primary);
  color: #ffffff;
}

/* ---- 两列商品网格 ---- */
.products {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -8rpx;
}

.product {
  box-sizing: border-box;
  width: calc(50% - 16rpx);
  margin: 16rpx 8rpx 0;
  padding: 0;
  overflow: hidden;
}

.product.disabled {
  opacity: 0.6;
}

/* ---- 商品媒体区 ---- */
.product-media {
  position: relative;
  height: 200rpx;
  background: var(--cg-fill);
}

.product-cover {
  display: block;
  width: 100%;
  height: 200rpx;
}

/* 无封面图：低饱和渐变 + 洗车插图占位，不出现空白卡 */
.product-cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #e8f1fd 0%, #f5f5f7 62%);
}

.ph-art {
  position: relative;
  width: 96rpx;
  height: 48rpx;
}

.product-badge {
  position: absolute;
  top: 16rpx;
  left: 16rpx;
}

/* ---- 商品信息 ---- */
.product-body {
  padding: 24rpx 24rpx 32rpx;
}

.product-name {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 96rpx;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
  line-height: 1.4;
}

.product-sub {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  line-height: 1.5;
}

.product-tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 16rpx;
}

.product-tag {
  margin: 0 16rpx 16rpx 0;
  padding: 8rpx 16rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.product-price {
  display: flex;
  align-items: baseline;
  margin-top: 24rpx;
}

.price-now {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.price-old {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  text-decoration: line-through;
}

/* ---- 下单按钮：96rpx = 48px 最小触控区 ---- */
.product-btn {
  width: 100%;
  height: 96rpx;
  margin-top: 24rpx;
  font-size: var(--cg-font-caption);
}
</style>

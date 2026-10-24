<template>
  <view class="cg-page services">
    <!-- 站点详情 Hero：标题/副标题/头图/文案全部后台可配，未配置走前端回退 -->
    <view class="hero">
      <view class="hero-media">
        <app-image v-if="site?.detailImageUrl" class="hero-image" :src="site.detailImageUrl" mode="aspectFill" />
        <template v-else>
          <view class="hero-glow" />
          <view class="art-spotlight" />
          <view class="cg-art-car">
            <view class="art-cabin" />
            <view class="art-body" />
            <view class="art-wheel w-back" />
            <view class="art-wheel w-front" />
          </view>
          <view class="cg-art-drop hero-drop-1" />
          <view class="cg-art-drop hero-drop-2" />
        </template>
        <view v-if="site?.detailImageUrl" class="hero-scrim" />
        <view v-if="site?.detailImageUrl" class="hero-text is-on-image">
          <text class="hero-title">{{ heroTitle }}</text>
          <text class="hero-sub">{{ heroSub }}</text>
        </view>
      </view>
      <view v-if="!site?.detailImageUrl" class="hero-text">
        <text class="hero-title">{{ heroTitle }}</text>
        <text class="hero-sub">{{ heroSub }}</text>
      </view>
      <text v-if="heroDesc" class="hero-desc">{{ heroDesc }}</text>
      <!-- 停业提示：站点状态由后端下发；雨天等运营提示在首页站点栏展示，此处不重复 -->
      <view v-if="siteClosed" class="hero-notice">
        <view class="hero-notice-dot" />
        <text class="hero-notice-text">{{ site?.closedNotice || '本服务站临时停业' }}</text>
      </view>
    </view>

    <view v-if="loading" class="cg-empty">加载中…</view>

    <!-- 分类分区：每区一个标题 + 横版商品卡 -->
    <block v-for="g in grouped" :key="g.categoryId ?? 'all'">
      <view class="section-head">
        <view class="section-art">
          <view class="cg-art-car is-xs">
            <view class="art-cabin" />
            <view class="art-body" />
            <view class="art-wheel w-back" />
            <view class="art-wheel w-front" />
          </view>
        </view>
        <text class="section-title">{{ g.name }}</text>
      </view>

      <!-- 点卡直接进下单页；「详情」进服务详情页 -->
      <view
        v-for="item in g.list"
        :key="item.serviceId"
        class="cg-card svc"
        :class="{ disabled: item.soldOut }"
        @click="onCard(item)"
      >
        <view class="svc-media">
          <app-image v-if="item.coverUrl" class="svc-cover" :src="item.coverUrl" mode="aspectFill" />
          <view v-else class="svc-cover svc-cover-ph">
            <view class="ph-art">
              <view class="cg-art-car is-sm">
                <view class="art-cabin" />
                <view class="art-body" />
                <view class="art-wheel w-back" />
                <view class="art-wheel w-front" />
              </view>
            </view>
          </view>
          <text v-if="item.soldOut" class="cg-status is-busy svc-badge">已约满</text>
        </view>
        <view class="svc-body">
          <view class="svc-top">
            <text class="svc-name">{{ item.name }}</text>
            <text class="svc-detail-link" @click.stop="goDetail(item.serviceId)">详情 ›</text>
          </view>
          <view class="svc-time-pill">预计工时 {{ item.workMinutes || '—' }} 分钟</view>
          <text v-if="item.subtitle" class="svc-desc">{{ item.subtitle }}</text>
          <view class="svc-price-row">
            <text class="svc-price-now">¥{{ fen2yuan(item.displayPrice) }}</text>
            <text v-if="item.originPrice && item.originPrice !== item.displayPrice" class="svc-price-old">
              ¥{{ fen2yuan(item.originPrice) }}
            </text>
            <text class="svc-price-label">服务费</text>
          </view>
          <view class="svc-price-row">
            <text class="svc-price-zero">¥{{ fen2yuan(item.pickupFee ?? 0) }}</text>
            <text class="svc-price-label">取送车 可选</text>
          </view>
        </view>
      </view>
    </block>

    <view v-if="!loading && !grouped.length" class="cg-empty">暂无可预约的服务</view>

    <!-- 悬浮客服：平台热线（号码后台配置，未配置则不展示） -->
    <view v-if="csPhone" class="cs-fab cg-tap" hover-class="cg-hover" @click="callService">
      <view class="cs-fab-avatar" />
      <view class="cs-fab-main">
        <text class="cs-fab-title">洗车客服热线</text>
        <text class="cs-fab-phone">{{ csPhone }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchServiceCategories, fetchSiteCurrent, type ServiceCategoryVO, type SiteVO } from '@/api/home'
import { fetchServices as fetchServiceList, type ServiceVO } from '@/api/catalog'
import { fetchCustomerService } from '@/api/config'
import { setOrderDraft } from '@/store/orderDraft'

const categories = ref<ServiceCategoryVO[]>([])
const services = ref<ServiceVO[]>([])
const loading = ref(true)
/** 悬浮客服电话：平台号优先，未配置则隐藏入口（号码一律后台下发，不硬编码） */
const csPhone = ref('')
/** 当前站点：Hero 展示站点详情（后台可配），停业状态也由它下发 */
const site = ref<SiteVO | null>(null)

/** 站点详情标题：后台未配置时回退站点名，再回退默认卖点 */
const heroTitle = computed(() => site.value?.detailTitle || site.value?.siteName || '夜间代客洗车')
const heroSub = computed(() => site.value?.detailSubtitle || '中央站集中精洗 · 标准化 SOP · 全程影像留痕')
const heroDesc = computed(() => site.value?.detailDesc || '')
/** 停业才在 Hero 提示（CLOSED）；雨天等运营提示不在此重复展示 */
const siteClosed = computed(() => site.value?.serviceStatus === 'CLOSED')

interface SvcGroup {
  categoryId?: number
  name: string
  list: ServiceVO[]
}

/**
 * 按分类分区展示（参考稿形态）：后台配了分类就按分类顺序分组，
 * 未匹配到分类的服务归入默认组——有商品就必须可见，不能被分组逻辑吞掉。
 */
const grouped = computed<SvcGroup[]>(() => {
  const list = services.value
  if (!list.length) return []
  if (!categories.value.length) return [{ categoryId: undefined, name: '洗车服务', list }]
  const groups: SvcGroup[] = []
  for (const c of categories.value) {
    const items = list.filter((s) => s.categoryId === c.categoryId)
    if (items.length) groups.push({ categoryId: c.categoryId, name: c.name || '洗车服务', list: items })
  }
  const rest = list.filter((s) => !categories.value.some((c) => c.categoryId === s.categoryId))
  if (rest.length) groups.push({ categoryId: undefined, name: '洗车服务', list: rest })
  return groups
})

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [cats, list, cur] = await Promise.all([
      fetchServiceCategories(),
      fetchServiceList('UNSENSITIVE'),
      fetchSiteCurrent(),
    ])
    categories.value = cats || []
    services.value = list || []
    site.value = cur || null
  } catch {
    services.value = []
  } finally {
    loading.value = false
  }
  // 客服配置失败不影响服务列表，静默降级为隐藏悬浮入口
  fetchCustomerService()
    .then((cfg) => {
      csPhone.value = cfg.platformPhone || cfg.stationPhone || ''
    })
    .catch(() => {})
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

/** 点卡直接进下单页并预选该服务；约满只提示不跳转 */
function onCard(item: ServiceVO): void {
  if (item.soldOut) {
    uni.showToast({ title: '今夜已约满，明晚可约', icon: 'none' })
    return
  }
  book(item.serviceId)
}

/** 悬浮客服：直拨平台热线（号码由后台配置下发） */
function callService(): void {
  if (!csPhone.value) return
  uni.makePhoneCall({ phoneNumber: csPhone.value })
}

onShow(load)
</script>

<style>
/* tabBar 页：底部留白需避开原生 tabBar 与悬浮客服 */
.services {
  padding-bottom: calc(240rpx + env(safe-area-inset-bottom));
}

/* ---- 站点详情 Hero ---- */
.hero {
  position: relative;
  overflow: hidden;
  margin-top: 32rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

/* 媒体区：后台配了头图用实拍图，未配则渐变 + 洗车插图占位 */
.hero-media {
  position: relative;
  height: 360rpx;
  overflow: hidden;
}

.hero-image {
  display: block;
  width: 100%;
  height: 360rpx;
}

/* 图片片底部暗 scrim：保证叠加文字可读的功能性渐变 */
.hero-scrim {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.05) 0%, rgba(0, 0, 0, 0.45) 100%);
}

.hero-glow {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 320rpx;
  background: radial-gradient(130% 100% at 50% 0%, rgba(0, 122, 255, 0.10), rgba(0, 122, 255, 0.04) 45%, rgba(0, 122, 255, 0) 75%);
  pointer-events: none;
}

.art-spotlight {
  position: absolute;
  left: 50%;
  top: 42%;
  transform: translate(-50%, -50%);
  width: 420rpx;
  height: 420rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 122, 255, 0.12) 0%, rgba(0, 122, 255, 0.05) 45%, rgba(0, 122, 255, 0) 70%);
}

.hero-media .cg-art-car {
  left: 50%;
  transform: translateX(-50%);
}

.hero-drop-1 {
  left: 96rpx;
  top: 48rpx;
}

.hero-drop-2 {
  left: 150rpx;
  top: 16rpx;
}

/* 文字：无头图时在媒体区下方；有头图时叠加在图上（白字） */
.hero-text {
  position: relative;
  padding: 0 40rpx 40rpx;
}

.hero-text.is-on-image {
  position: absolute;
  left: 40rpx;
  right: 40rpx;
  bottom: 32rpx;
  padding: 0;
}

.hero-title {
  display: block;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.hero-text.is-on-image .hero-title {
  color: #ffffff;
}

.hero-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.hero-text.is-on-image .hero-sub {
  color: rgba(255, 255, 255, 0.85);
}

/* 站点详情补充文案（后台可选配置） */
.hero-desc {
  display: block;
  padding: 32rpx 40rpx 0;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  line-height: 1.6;
}

/* 停业提示 */
.hero-notice {
  display: flex;
  align-items: center;
  margin: 32rpx 40rpx 40rpx;
  padding: 16rpx 24rpx;
  background: rgba(255, 59, 48, 0.08);
  border-radius: var(--cg-radius-md);
}

.hero-notice-dot {
  flex: none;
  width: 12rpx;
  height: 12rpx;
  margin-right: 16rpx;
  border-radius: 50%;
  background: var(--cg-error);
}

.hero-notice-text {
  font-size: var(--cg-font-note);
  color: var(--cg-error);
}

/* ---- 分类分区头 ---- */
.section-head {
  display: flex;
  align-items: center;
  margin: 64rpx 8rpx 24rpx;
}

.section-art {
  position: relative;
  width: 44rpx;
  height: 24rpx;
  margin-right: 16rpx;
}

.section-title {
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

/* ---- 横版商品卡：左图右文 ---- */
.svc {
  display: flex;
  margin-bottom: 32rpx;
  padding: 24rpx;
}

.svc.disabled {
  opacity: 0.6;
}

.svc-media {
  position: relative;
  flex: none;
  width: 200rpx;
  height: 200rpx;
  margin-right: 24rpx;
}

.svc-cover {
  display: block;
  width: 200rpx;
  height: 200rpx;
  border-radius: var(--cg-radius-md);
}

/* 无封面：低饱和渐变 + 洗车插图占位 */
.svc-cover-ph {
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

.svc-badge {
  position: absolute;
  left: 16rpx;
  bottom: 16rpx;
}

.svc-body {
  flex: 1;
}

.svc-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.svc-name {
  flex: 1;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
  line-height: 1.4;
}

.svc-detail-link {
  flex: none;
  min-height: 96rpx;
  margin-left: 16rpx;
  padding: 0 8rpx;
  color: var(--cg-primary);
  font-size: var(--cg-font-note);
  line-height: 96rpx;
}

/* 蓝色工时 pill（参考稿核心识别元素） */
.svc-time-pill {
  display: inline-block;
  margin-top: 16rpx;
  padding: 8rpx 20rpx;
  background: var(--cg-tint);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
}

.svc-desc {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  line-height: 1.5;
}

.svc-price-row {
  display: flex;
  align-items: baseline;
  margin-top: 16rpx;
}

.svc-price-now {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.svc-price-old {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  text-decoration: line-through;
}

.svc-price-zero {
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.svc-price-label {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 悬浮客服（参考稿右下角浮条） ---- */
.cs-fab {
  position: fixed;
  right: 40rpx;
  bottom: calc(220rpx + env(safe-area-inset-bottom));
  z-index: 20;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx 16rpx 16rpx;
  background: var(--cg-glass-bg);
  -webkit-backdrop-filter: blur(var(--cg-glass-blur));
  backdrop-filter: blur(var(--cg-glass-blur));
  border: var(--cg-glass-border);
  border-radius: var(--cg-radius-pill);
  box-shadow: var(--cg-shadow-raised);
}

.cs-fab-avatar {
  flex: none;
  position: relative;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 30%, #b8e6c8 0%, #8fd4a8 100%);
}

/* 客服头像：极简人形（白线条） */
.cs-fab-avatar::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 18rpx;
  transform: translateX(-50%);
  width: 20rpx;
  height: 20rpx;
  border: 3rpx solid #ffffff;
  border-radius: 50%;
}

.cs-fab-avatar::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 8rpx;
  transform: translateX(-50%);
  width: 32rpx;
  height: 16rpx;
  border: 3rpx solid #ffffff;
  border-bottom: none;
  border-radius: 16rpx 16rpx 0 0;
}

.cs-fab-main {
  margin-left: 16rpx;
}

.cs-fab-title {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.cs-fab-phone {
  display: block;
  margin-top: 8rpx;
  font-size: var(--cg-font-caption);
  font-weight: 500;
  color: var(--cg-title);
}
</style>

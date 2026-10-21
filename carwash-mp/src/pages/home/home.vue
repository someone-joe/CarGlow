<template>
  <view class="cg-page home" :style="{ paddingTop: navHeight + 'px' }">
    <!-- 毛玻璃导航栏：居中标题 + 右上角个人中心入口；滚动时平缓增强 -->
    <view class="cg-glass-nav home-nav" :class="{ 'is-scrolled': navScrolled }" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="home-nav-inner">
        <view class="home-nav-side" />
        <text class="home-nav-title">CarGlow</text>
        <view class="home-nav-entry cg-tap" hover-class="cg-hover" @click="goMine">
          <view class="cg-icon-person" />
        </view>
      </view>
    </view>

    <!-- 首屏 Banner：独立组件（拖动时仅在组件内重渲染，避免整页 setData 卡顿） -->
    <HomeBanner :slides="slides" :active="pageActive" @tap="onBanner" />

    <!-- 当前服务站点 / 小区（停业、雨天模式由后端下发） -->
    <view class="home-site">
      <SiteBar />
    </view>

    <!-- 服务入口：仅「无感洗车」「自助洗车」两个 -->
    <view class="home-entries">
      <view class="cg-card home-entry cg-tap" hover-class="cg-hover" @click="goServices">
        <view class="home-entry-art">
          <view class="cg-art-car is-sm">
            <view class="art-cabin" />
            <view class="art-body" />
            <view class="art-wheel w-back" />
            <view class="art-wheel w-front" />
          </view>
        </view>
        <text class="home-entry-title">无感洗车</text>
        <text class="home-entry-sub">师傅上门取送 · 睡醒即净</text>
        <text class="cg-chevron home-entry-chevron">›</text>
      </view>
      <view class="cg-card home-entry cg-tap" hover-class="cg-hover" @click="goSelfWash">
        <view class="home-entry-art">
          <view class="cg-art-drop is-sm home-entry-drop" />
          <view class="cg-art-drop is-sm home-entry-drop-2" />
        </view>
        <text class="home-entry-title">自助洗车</text>
        <text class="home-entry-sub">到店自助 · 计时计费</text>
        <text class="cg-chevron home-entry-chevron">›</text>
      </view>
    </view>

    <!-- 附近钥匙柜：对应规范的「附近工位卡片列表」，数据全部来自 /api/v1/cabinets -->
    <view class="home-section">
      <view class="home-section-head">
        <text class="home-section-title">附近钥匙柜</text>
        <view class="cg-btn-text home-section-more" @click="goCabinetPage">查看全部 ›</view>
      </view>
      <view v-for="c in cabinets" :key="c.cabinetId" class="cg-card home-cabinet cg-tap" hover-class="cg-hover" @click="goCabinetPage">
        <view class="home-cabinet-preview">
          <view v-for="(cell, i) in slotCells(c)" :key="i" class="home-cabinet-cell" :class="cell" />
        </view>
        <view class="home-cabinet-main">
          <text class="home-cabinet-name">{{ c.name }}</text>
          <text class="home-cabinet-sub">空闲格口 {{ c.slotFree ?? 0 }} / {{ c.slotTotal ?? 0 }}</text>
        </view>
        <view class="cg-status home-cabinet-pill" :class="cabinetStatus(c).cls">{{ cabinetStatus(c).label }}</view>
      </view>
      <view v-if="!cabinets.length" class="home-empty">附近暂无钥匙柜</view>
    </view>

    <!-- 精选套餐：横向滑动卡片组 -->
    <view class="home-section">
      <view class="home-section-head">
        <text class="home-section-title">精选套餐</text>
      </view>
      <scroll-view class="home-packages" scroll-x enhanced show-scrollbar="false">
        <view v-for="item in services" :key="item.serviceId" class="cg-card home-package cg-tap" hover-class="cg-hover" @click="goCreate(item.serviceId as number)">
          <image v-if="item.coverUrl" class="home-package-cover" :src="item.coverUrl" mode="aspectFill" />
          <text class="home-package-name">{{ item.name }}</text>
          <view class="home-package-meta">
            <text class="home-package-minutes">约 {{ item.workMinutes }} 分钟</text>
          </view>
          <view class="home-package-price">
            <text class="home-package-now">¥{{ fen2yuan(item.displayPrice) }}</text>
            <text v-if="item.originPrice && item.originPrice !== item.displayPrice" class="home-package-origin">
              ¥{{ fen2yuan(item.originPrice) }}
            </text>
          </view>
        </view>
      </scroll-view>
      <view v-if="loading" class="home-loading">加载中…</view>
      <view v-else-if="!services.length" class="home-empty">暂无可预约的服务</view>
    </view>

    <!-- 优惠入口 -->
    <view class="cg-card cg-card-tint home-promo">
      <view class="home-promo-row cg-tap" hover-class="cg-hover" @click="goRecharge">
        <text class="home-promo-label">储值充值</text>
        <text class="home-promo-sub">充多送多 · 支付更优惠</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="home-promo-divider" />
      <view class="home-promo-row cg-tap" hover-class="cg-hover" @click="goCoupons">
        <text class="home-promo-label">优惠券</text>
        <text class="home-promo-sub">领券下单更省钱</text>
        <text class="cg-chevron">›</text>
      </view>
    </view>

    <!-- 服务流程 -->
    <view class="cg-card cg-card-tint home-flow">
      <text class="home-section-title">服务流程</text>
      <view class="home-flow-item">
        <text class="home-flow-name">无感洗车</text>
        <text class="home-flow-steps">下单 → 存钥匙 → 站内精洗 → 归位还钥匙</text>
      </view>
      <view class="home-flow-item">
        <text class="home-flow-name">自助洗车</text>
        <text class="home-flow-steps">选工位 → 到店激活 → 计时服务 → 断电结算</text>
      </view>
      <view class="home-flow-item">
        <text class="home-flow-name">监管留痕</text>
        <text class="home-flow-steps">取/洗/还三时段拍照 · 钥匙柜开柜记录 · 全程可查</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onHide, onLoad, onPageScroll, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { fetchBanners, type BannerVO } from '@/api/home'
import { fetchCabinets, fetchServices, type CabinetVO, type ServiceVO } from '@/api/catalog'
import HomeBanner, { type Slide } from '@/components/HomeBanner.vue'
import SiteBar from '@/components/SiteBar.vue'
import { selectedCommunityId } from '@/store/community'
import { setOrderDraft } from '@/store/orderDraft'

const services = ref<ServiceVO[]>([])
const banners = ref<BannerVO[]>([])
const cabinets = ref<CabinetVO[]>([])
const loading = ref(true)

/** 自定义导航栏避让状态栏（iPhone 刘海）；取不到时兜底 20px（常见非刘海屏值） */
const statusBarHeight = ref(20)

/** 毛玻璃导航总高（状态栏 + 44px 内容区），用于页面顶部留白 */
const navHeight = computed(() => statusBarHeight.value + 44)

/** 滚动超过 8px 时毛玻璃平缓增强（规范：安静过渡，不弹跳） */
const navScrolled = ref(false)

/** 页面是否可见：传给 Banner 控制轮播启停（跳转/切后台时暂停，避免空转） */
const pageActive = ref(true)

/** 后台无 banner 配置时播放服务理念（产品要求：无感/安全/专业洁净） */
const philosophy: Slide[] = [
  { title: '今晚下单，明早干净上路', desc: '夜间代客洗车 · 睡醒即净' },
  { title: '全程影像留痕', desc: '取/洗/还三时段拍照 · 钥匙柜开柜记录可查' },
  { title: '标准化 SOP 中央站精洗', desc: '专业洁净 · 品质稳定' },
]

/** 轮播片：后台 banner 优先，无配置时降级服务理念 */
const slides = computed<Slide[]>(() => (banners.value.length ? banners.value : philosophy))

/** 柜口预览格：按占用比例取 8 格（已占用在前）；无格口数据时置灰 */
function slotCells(c: CabinetVO): string[] {
  const total = Math.max(0, Math.min(8, c.slotTotal ?? 0))
  const free = Math.max(0, Math.min(total, c.slotFree ?? 0))
  const cells: string[] = []
  for (let i = 0; i < 8; i++) {
    cells.push(total === 0 ? 'is-off' : i < total - free ? 'is-busy' : 'is-free')
  }
  return cells
}

/** 柜口状态 pill：离线 > 已约满 > 有空位；取值全部来自后端字段，前端不推断 */
function cabinetStatus(c: CabinetVO): { cls: string; label: string } {
  if (!c.online) return { cls: 'is-off', label: '离线' }
  if (c.full) return { cls: 'is-busy', label: '已约满' }
  return { cls: 'is-free', label: '有空位' }
}

/** 金额一律以「分」传给后端，展示时才转元 */
function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function goServices(): void {
  // services 是 tabBar 页，必须用 switchTab；navigateTo 会报 "can not navigateTo a tabbar page"
  uni.switchTab({ url: '/pages/services/services' })
}

function goSelfWash(): void {
  uni.navigateTo({ url: '/pages/self-wash/self-wash' })
}

function goMine(): void {
  // mine 是 tabBar 页，必须 switchTab；navigateTo 会报 "can not navigateTo a tabbar page"
  uni.switchTab({ url: '/pages/mine/mine' })
}

/** 查看全部钥匙柜：柜机选择页（完整九宫格） */
function goCabinetPage(): void {
  uni.navigateTo({ url: '/pages/cabinet/cabinet' })
}

function goRecharge(): void {
  uni.navigateTo({ url: '/pages/recharge/recharge' })
}

function goCoupons(): void {
  uni.navigateTo({ url: '/pages/coupons/coupons' })
}

/** banner 跳转：按契约 linkType 分流，前端不猜链接 */
function onBanner(s: Slide): void {
  if (s.linkType === 'RECHARGE') {
    goRecharge()
  } else if (s.linkType === 'SERVICE') {
    if (s.linkValue && /^\d+$/.test(s.linkValue)) {
      uni.navigateTo({ url: `/pages/service-detail/service-detail?serviceId=${s.linkValue}` })
    } else {
      goServices()
    }
  } else if (s.linkType === 'ACTIVITY') {
    uni.showToast({ title: '活动详情敬请期待', icon: 'none' })
  }
}

/** 点爆品直接进入下单页并预选该服务 */
function goCreate(serviceId: number): void {
  setOrderDraft({ serviceId })
  uni.navigateTo({ url: '/pages/index/index' })
}

onLoad(() => {
  try {
    statusBarHeight.value = uni.getWindowInfo().statusBarHeight || 20
  } catch {
    statusBarHeight.value = 20
  }
})

onPageScroll((e) => {
  navScrolled.value = (e.scrollTop ?? 0) > 8
})

onShow(() => {
  loading.value = true
  fetchServices()
    .then((list) => (services.value = list || []))
    .catch(() => (services.value = []))
    .finally(() => (loading.value = false))
  fetchBanners()
    // 后端 banner 的 subtitle 映射为文字片的 desc；无配置时降级为「服务理念」片
    .then((b) => (banners.value = (b || []).map((x) => ({ ...x, desc: x.subtitle }))))
    .catch(() => (banners.value = []))
  // 附近钥匙柜：按用户所选小区过滤（后端契约 communityId 已支持）；首页只展示前 3 个，完整列表在柜机选择页
  fetchCabinets(selectedCommunityId())
    .then((list) => (cabinets.value = (list || []).slice(0, 3)))
    .catch(() => (cabinets.value = []))
  pageActive.value = true
  maybeShowFirstOrderPopup()
})

// 页面隐藏（跳转/切后台）时通知 Banner 停轮播；组件内部负责定时器清理
onHide(() => {
  pageActive.value = false
})

/** 新用户首单弹窗（需求 1.4）：只弹一次；不写死价格，引导去领券中心（新人券） */
const FIRST_POPUP_KEY = 'carglow_firstorder_popup_shown'
function maybeShowFirstOrderPopup(): void {
  try {
    if (uni.getStorageSync(FIRST_POPUP_KEY)) return
    uni.setStorageSync(FIRST_POPUP_KEY, '1')
  } catch {
    // 存储异常不阻塞首页
  }
  uni.showModal({
    title: '新人专享 · 首单立减',
    content: '新用户首单可领优惠券，自助洗车与无感洗车不重复享受。',
    confirmText: '去领券',
    cancelText: '以后再说',
    success: (res) => {
      if (res.confirm) uni.navigateTo({ url: '/pages/coupons/coupons' })
    },
  })
}
</script>

<style>
/* ---- 毛玻璃导航栏 ---- */
.home-nav-inner {
  display: flex;
  align-items: center;
  height: 88rpx;
  padding: 0 40rpx;
}

.home-nav-side {
  width: 64rpx;
}

.home-nav-title {
  flex: 1;
  text-align: center;
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
}

.home-nav-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
}

/* ---- 服务入口：仅两个 ---- */
.home-site {
  margin-top: 32rpx;
}

.home-entries {
  display: flex;
  margin-top: 32rpx;
}

.cg-card.home-entry {
  position: relative;
  flex: 1;
  min-height: 216rpx;
  padding: 32rpx;
}

.home-entry + .home-entry {
  margin-left: 32rpx;
}

.home-entry-art {
  position: relative;
  width: 96rpx;
  height: 48rpx;
  margin-bottom: 24rpx;
}

.home-entry-art .cg-art-car.is-sm {
  left: 0;
  bottom: 0;
}

.home-entry-drop {
  left: 12rpx;
  top: 8rpx;
}

.home-entry-drop-2 {
  left: 48rpx;
  top: 24rpx;
}

.home-entry-title {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.home-entry-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.home-entry-chevron {
  position: absolute;
  top: 32rpx;
  right: 24rpx;
}

/* ---- 区块 ---- */
.home-section {
  margin-top: 64rpx;
}

.home-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.home-section-title {
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.home-section-more {
  min-height: 64rpx;
  font-size: var(--cg-font-note);
}

/* ---- 附近钥匙柜卡片 ---- */
.cg-card.home-cabinet {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  padding: 32rpx;
}

.home-cabinet-preview {
  display: flex;
  flex-wrap: wrap;
  flex: none;
  width: 88rpx;
  height: 44rpx;
  margin-right: 32rpx;
}

.home-cabinet-cell {
  width: 16rpx;
  height: 16rpx;
  margin: 3rpx;
  border-radius: 6rpx;
}

.home-cabinet-cell.is-free {
  background: rgba(52, 199, 89, 0.30);
}

.home-cabinet-cell.is-busy {
  background: #d2d2d7;
}

.home-cabinet-cell.is-off {
  background: #e8e8ed;
}

.home-cabinet-main {
  flex: 1;
}

.home-cabinet-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.home-cabinet-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.home-cabinet-pill {
  flex: none;
  margin-left: 24rpx;
}

/* ---- 精选套餐：横向滑动 ---- */
.home-packages {
  width: 100%;
  white-space: nowrap;
}

.cg-card.home-package {
  display: inline-block;
  width: 260rpx;
  margin-right: 24rpx;
  padding: 32rpx;
  white-space: normal;
  vertical-align: top;
}

.home-package-cover {
  display: block;
  width: 100%;
  height: 140rpx;
  margin-bottom: 24rpx;
  border-radius: var(--cg-radius-md);
}

.home-package-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.home-package-meta {
  margin-top: 16rpx;
}

.home-package-minutes {
  display: inline-block;
  padding: 8rpx 20rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.home-package-price {
  margin-top: 24rpx;
}

.home-package-now {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.home-package-origin {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  text-decoration: line-through;
}

/* ---- 优惠入口 ---- */
.cg-card.home-promo {
  margin-top: 32rpx;
  padding: 8rpx 40rpx;
}

.home-promo-row {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
}

.home-promo-label {
  width: 160rpx;
  font-size: var(--cg-font-body);
  color: var(--cg-text);
}

.home-promo-sub {
  flex: 1;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.home-promo-divider {
  height: 1rpx;
  background: var(--cg-line);
}

/* ---- 服务流程 ---- */
.cg-card.home-flow {
  margin-top: 32rpx;
}

.home-flow-item {
  padding: 24rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.home-flow-item:first-of-type {
  border-top: none;
}

.home-flow-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-text);
}

.home-flow-steps {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  line-height: 1.6;
}

.home-empty {
  padding: 64rpx 0;
  text-align: center;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-aux);
}

.home-loading {
  padding: 64rpx 0;
  text-align: center;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}
</style>

<template>
  <view class="page">
    <!-- 品牌头图 -->
    <view class="hero">
      <text class="hero-title">夜间代客洗车</text>
      <text class="hero-sub">今晚下单 · 明早干净上路</text>
    </view>

    <SiteBar />

    <!-- 滚动广告位：后台 banner 优先；无配置时按产品要求播放服务理念（无感/安全/专业洁净） -->
    <swiper class="banner" indicator-dots autoplay circular :interval="4000">
      <swiper-item v-for="(s, i) in slides" :key="s.bannerId ?? i" @click="onBanner(s)">
        <image v-if="s.imageUrl" :src="s.imageUrl" class="slide-img" mode="aspectFill" />
        <view v-else class="slide-philosophy">
          <text class="sp-title">{{ s.title }}</text>
          <text class="sp-desc">{{ s.desc }}</text>
        </view>
      </swiper-item>
    </swiper>

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

    <!-- 服务入口：无感洗车 / 自助洗车 -->
    <view class="entries">
      <view class="entry-card" @click="goServices">
        <text class="ec-ico">🌙</text>
        <text class="ec-title">无感洗车</text>
        <text class="ec-sub">师傅上门取送 · 睡醒即净</text>
      </view>
      <view class="entry-card" @click="goSelfWash">
        <text class="ec-ico">🚿</text>
        <text class="ec-title">自助洗车</text>
        <text class="ec-sub">到店自助 · 计时计费</text>
      </view>
    </view>

    <!-- 优惠推荐窗口：储值充值 + 领券 -->
    <view class="promo">
      <view class="promo-card recharge" @click="goRecharge">
        <text class="promo-title">储值充值</text>
        <text class="promo-sub">充多送多 · 支付更优惠</text>
        <text class="promo-cta">去充值 ›</text>
      </view>
      <view class="promo-card coupon" @click="goCoupons">
        <text class="promo-title">优惠券</text>
        <text class="promo-sub">领券下单更省钱</text>
        <text class="promo-cta">领中心 ›</text>
      </view>
    </view>

    <!-- 服务流程简介：自助 / 无感 / 监管 -->
    <view class="panel">
      <view class="panel-title">服务流程</view>
      <view class="flow">
        <text class="flow-name">自助洗车</text>
        <text class="flow-steps">选工位 → 到店激活 → 计时服务 → 断电结算</text>
      </view>
      <view class="flow">
        <text class="flow-name">无感洗车</text>
        <text class="flow-steps">下单 → 存钥匙 → 站内精洗 → 归位还钥匙</text>
      </view>
      <view class="flow">
        <text class="flow-name">监管留痕</text>
        <text class="flow-steps">取/洗/还三时段拍照 · 钥匙柜开柜记录 · 全程可查</text>
      </view>
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
import { computed, ref } from 'vue'
import { fetchBanners, type BannerVO } from '@/api/home'
import { fetchServices, type ServiceVO } from '@/api/catalog'
import SiteBar from '@/components/SiteBar.vue'
import { setOrderDraft } from '@/store/orderDraft'

const services = ref<ServiceVO[]>([])
const banners = ref<BannerVO[]>([])
const loading = ref(true)

/** 轮播片：后台 banner 优先，无配置时用服务理念占位（产品要求：前期播放无感/安全/专业洁净） */
interface Slide {
  bannerId?: number
  imageUrl?: string
  title?: string
  desc?: string
  linkType?: string
  linkValue?: string
}

const philosophy: Slide[] = [
  { title: '无感', desc: '到店即走，无需排队等待' },
  { title: '安全', desc: '取洗还全程影像留痕' },
  { title: '专业洁净', desc: '标准化SOP，中央站精洗' },
]

const slides = computed<Slide[]>(() => (banners.value.length ? banners.value : philosophy))

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

onShow(() => {
  loading.value = true
  fetchServices()
    .then((list) => (services.value = list || []))
    .catch(() => (services.value = []))
    .finally(() => (loading.value = false))
  fetchBanners()
    // 后端 banner 的 subtitle 映射为文字图的 desc；无配置时前端降级为「服务理念」
    .then((b) => (banners.value = (b || []).map((x) => ({ ...x, desc: x.subtitle }))))
    .catch(() => (banners.value = []))
  maybeShowFirstOrderPopup()
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

/* ---- 轮播 ---- */
.banner {
  margin-bottom: 20rpx;
  height: 260rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.slide-img {
  width: 100%;
  height: 260rpx;
}

.slide-philosophy {
  width: 100%;
  height: 260rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0e8f94, #14342f);
  color: #fff;
}

.sp-title {
  font-size: 56rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
}

.sp-desc {
  margin-top: 14rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

/* ---- 卖点条 ---- */
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

/* ---- 两大服务入口 ---- */
.entries {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.entry-card {
  flex: 1;
  background: #fff;
  border-radius: 20rpx;
  padding: 34rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.ec-ico {
  font-size: 56rpx;
}

.ec-title {
  margin-top: 14rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #1b2b2a;
}

.ec-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

/* ---- 优惠推荐 ---- */
.promo {
  display: flex;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.promo-card {
  flex: 1;
  border-radius: 20rpx;
  padding: 26rpx 24rpx;
  color: #fff;
}

.promo-card.recharge {
  background: linear-gradient(135deg, #ff8a5b, #ff5b4a);
}

.promo-card.coupon {
  background: linear-gradient(135deg, #7b6cf6, #5a48e0);
}

.promo-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
}

.promo-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
}

.promo-cta {
  display: inline-block;
  margin-top: 16rpx;
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 30rpx;
  padding: 6rpx 20rpx;
}

/* ---- 流程简介 ---- */
.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.panel-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 12rpx;
}

.flow {
  padding: 16rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.flow:last-child {
  border-bottom: none;
}

.flow-name {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #00aeb5;
}

.flow-steps {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #6b7b79;
  line-height: 36rpx;
}

/* ---- 爆品 ---- */
.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 40rpx 0;
}

.block-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1b2b2a;
  margin: 8rpx 4rpx 18rpx;
}

.hot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx;
  margin-bottom: 18rpx;
}

.hot-main {
  flex: 1;
}

.hot-name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.hot-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.hot-right {
  display: flex;
  align-items: center;
}

.hot-price {
  text-align: right;
  margin-right: 18rpx;
}

.price-now {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.price-old {
  display: block;
  font-size: 22rpx;
  color: #b6c2c0;
  text-decoration: line-through;
}

.hot-btn {
  background: #14342f;
  color: #fff;
  font-size: 26rpx;
  padding: 14rpx 28rpx;
  border-radius: 30rpx;
}

.empty {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}
</style>

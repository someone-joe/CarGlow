<template>
  <view class="page">
    <!-- 顶部切换：领券中心 / 我的优惠券（激活码与卡包无后端支撑，按约定不展示） -->
    <view class="tabs">
      <view class="tab" :class="{ active: tab === 'center' }" @click="tab = 'center'">领券中心</view>
      <view class="tab" :class="{ active: tab === 'mine' }" @click="switchToMine">我的优惠券</view>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <!-- 领券中心 -->
    <block v-else-if="tab === 'center'">
      <view v-if="!center.length" class="empty">暂无可领取的优惠券</view>
      <view v-for="t in center" :key="t.templateId" class="coupon">
        <view class="coupon-left">
          <text class="amount">{{ discountText(t.discountAmount) }}</text>
          <text class="cond">{{ couponDesc(t.thresholdAmount, t.discountAmount) }}</text>
        </view>
        <view class="coupon-right">
          <view class="coupon-main">
            <text class="name">{{ t.name }}</text>
            <text v-if="t.type === 'NEWBIE'" class="tag">新人专享</text>
            <text class="meta">剩余 {{ t.remain }} · 已领 {{ t.claimed }}</text>
          </view>
          <view v-if="canReceive(t)" class="btn" @click="onReceive(t)">领取</view>
          <view v-else class="btn btn-disabled">{{ receiveLabel(t) }}</view>
        </view>
      </view>
    </block>

    <!-- 我的优惠券 -->
    <block v-else>
      <view class="sub-tabs">
        <view v-for="s in statusTabs" :key="s.key" class="sub-tab"
              :class="{ active: mineStatus === s.key }" @click="onMineStatus(s.key)">
          {{ s.label }}
        </view>
      </view>

      <view v-if="!mine.length" class="empty">
        {{ mineStatus === '' ? '还没有优惠券，去领券中心看看吧' : '该分类下暂无优惠券' }}
      </view>

      <view v-for="c in mine" :key="c.couponUserId" class="coupon" :class="{ invalid: c.status !== 'UNUSED' }">
        <view class="coupon-left">
          <text class="amount">{{ discountText(c.discountAmount) }}</text>
          <text class="cond">{{ couponDesc(c.thresholdAmount, c.discountAmount) }}</text>
        </view>
        <view class="coupon-right">
          <view class="coupon-main">
            <text class="name">{{ c.name }}</text>
            <text class="meta">{{ couponStatusLabel(c.status) }}</text>
          </view>
          <view v-if="c.status === 'UNUSED'" class="btn" @click="goUse(c)">去使用</view>
          <text v-else class="status-text">{{ couponStatusLabel(c.status) }}</text>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  fetchCouponCenter,
  fetchMyCoupons,
  receiveCoupon,
  type CouponStatus,
  type CouponTemplateVO,
  type CouponUserVO,
} from '@/api/promo'

const tab = ref<'center' | 'mine'>('center')
const loading = ref(true)
const center = ref<CouponTemplateVO[]>([])
const mine = ref<CouponUserVO[]>([])
const mineStatus = ref<CouponStatus | ''>('')

const statusTabs: { key: CouponStatus | ''; label: string }[] = [
  { key: '', label: '全部' },
  { key: 'UNUSED', label: '未使用' },
  { key: 'USED', label: '已使用' },
  { key: 'EXPIRED', label: '已过期' },
]

/** 金额一律以「分」为单位，展示时才转元 */
function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

/** 券面额展示：取整更醒目（分转元后去掉小数尾 0） */
function discountText(discount?: number): string {
  const yuan = (discount ?? 0) / 100
  return yuan % 1 === 0 ? `${yuan}` : yuan.toFixed(2)
}

/** 券文案：无门槛券显示「无门槛减 X.00」，满减券显示「满 X 减 Y」 */
function couponDesc(threshold?: number, discount?: number): string {
  const d = fen2yuan(discount)
  if (!threshold || threshold <= 0) return `无门槛减 ${d}`
  return `满 ${fen2yuan(threshold)} 减 ${d}`
}

function couponStatusLabel(status?: string): string {
  return status === 'UNUSED' ? '未使用' : status === 'USED' ? '已使用' : status === 'EXPIRED' ? '已过期' : '未知'
}

/** 是否可领取：未达每人限领且仍有库存 */
function canReceive(t: CouponTemplateVO): boolean {
  const claimed = t.claimed ?? 0
  const perLimit = t.perLimit ?? 0
  const remain = t.remain ?? 0
  return claimed < perLimit && remain > 0
}

function receiveLabel(t: CouponTemplateVO): string {
  if ((t.remain ?? 0) <= 0) return '已领完'
  return '已领取'
}

function switchToMine(): void {
  tab.value = 'mine'
  loadMine()
}

function onMineStatus(key: CouponStatus | ''): void {
  mineStatus.value = key
  loadMine()
}

async function loadCenter(): Promise<void> {
  center.value = (await fetchCouponCenter()) || []
}

async function loadMine(): Promise<void> {
  mine.value = (await fetchMyCoupons(mineStatus.value || undefined)) || []
}

async function onReceive(t: CouponTemplateVO): Promise<void> {
  if (t.templateId == null) return
  try {
    await receiveCoupon(t.templateId)
    uni.showToast({ title: '领取成功', icon: 'none' })
    // 领券中心与我的券都刷新
    await Promise.all([loadCenter(), loadMine()])
  } catch {
    // request 层已统一提示
  }
}

/** 未使用券点击「去使用」：回下单页并带上券 ID */
function goUse(c: CouponUserVO): void {
  if (c.couponUserId == null) return
  uni.navigateTo({ url: `/pages/index/index?couponUserId=${c.couponUserId}` })
}

function loadAll(): void {
  loading.value = true
  loadCenter()
    .then(() => (tab.value === 'mine' ? loadMine() : Promise.resolve()))
    .finally(() => {
      loading.value = false
    })
}

onShow(loadAll)
</script>

<style>
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
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

.tab.active {
  background: #00aeb5;
  color: #fff;
  font-weight: 600;
}

.sub-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.sub-tab {
  padding: 12rpx 28rpx;
  border-radius: 30rpx;
  background: #fff;
  font-size: 25rpx;
  color: #6b7b79;
}

.sub-tab.active {
  background: #14342f;
  color: #fff;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.empty {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 80rpx 0;
}

/* ---- 券卡 ---- */
.coupon {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}

.coupon.invalid {
  opacity: 0.55;
}

.coupon-left {
  width: 220rpx;
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30rpx 10rpx;
}

.amount {
  font-size: 52rpx;
  font-weight: 700;
}

.amount::before {
  content: '¥';
  font-size: 28rpx;
  margin-right: 4rpx;
}

.cond {
  margin-top: 8rpx;
  font-size: 22rpx;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 24rpx;
}

.coupon-main {
  flex: 1;
}

.name {
  font-size: 29rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.tag {
  margin-left: 10rpx;
  font-size: 21rpx;
  color: #ff5b4a;
  background: #ffeeeb;
  border-radius: 18rpx;
  padding: 3rpx 12rpx;
}

.meta {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.btn {
  background: #14342f;
  color: #fff;
  font-size: 25rpx;
  padding: 12rpx 30rpx;
  border-radius: 32rpx;
}

.btn-disabled {
  background: #cfe0de;
  color: #fff;
}

.status-text {
  font-size: 24rpx;
  color: #8a9a98;
}
</style>

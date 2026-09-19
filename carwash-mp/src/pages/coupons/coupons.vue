<template>
  <view class="page">
    <!-- 顶部切换：领券中心 / 我的优惠券 -->
    <view class="tabs">
      <view class="tab" :class="{ active: tab === 'center' }" @click="tab = 'center'">领券中心</view>
      <view class="tab" :class="{ active: tab === 'mine' }" @click="switchToMine">我的优惠券</view>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <!-- 领券中心 -->
    <block v-else-if="tab === 'center'">
      <view v-if="!center.length" class="empty">暂无可领取的优惠券</view>
      <view v-for="t in center" :key="t.templateId" class="card">
        <view class="card-main">
          <text class="card-name">{{ t.name }}</text>
          <text class="card-sub">
            {{ couponDesc(t.thresholdAmount, t.discountAmount) }}
            <text v-if="t.type === 'NEWBIE'" class="tag">新人</text>
          </text>
          <text class="card-meta">剩余 {{ t.remain }} · 已领 {{ t.claimed }}</text>
        </view>
        <view class="card-action">
          <view v-if="canReceive(t)" class="mini-btn" @click="onReceive(t)">领取</view>
          <view v-else class="mini-btn disabled">{{ receiveLabel(t) }}</view>
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
      <view v-for="c in mine" :key="c.couponUserId" class="card">
        <view class="card-main">
          <text class="card-name">{{ c.name }}</text>
          <text class="card-sub">{{ couponDesc(c.thresholdAmount, c.discountAmount) }}</text>
          <text class="card-meta">{{ couponStatusLabel(c.status) }}</text>
        </view>
        <view class="card-action">
          <view v-if="c.status === 'UNUSED'" class="mini-btn" @click="goUse(c)">去使用</view>
          <text v-else class="status-text" :class="statusClass(c.status)">{{ couponStatusLabel(c.status) }}</text>
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

/** 券文案：无门槛券显示「无门槛减 X.00」，满减券显示「满 X 减 Y」 */
function couponDesc(threshold?: number, discount?: number): string {
  const d = fen2yuan(discount)
  if (!threshold || threshold <= 0) return `无门槛减 ${d}`
  return `满 ${fen2yuan(threshold)} 减 ${d}`
}

function couponStatusLabel(status?: string): string {
  return status === 'UNUSED' ? '未使用' : status === 'USED' ? '已使用' : status === 'EXPIRED' ? '已过期' : '未知'
}

function statusClass(status?: string): string {
  return status === 'UNUSED' ? 'st-unused' : status === 'USED' ? 'st-used' : 'st-expired'
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

/** 未使用券点击「去使用」：回到首页，由下单页选择服务/车辆/机柜 */
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

// 从其他页返回时刷新（领券后状态会变）
onShow(loadAll)
</script>

<style>
.page {
  padding: 24rpx 32rpx 60rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  background: #fff;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #666;
}

.tab.active {
  color: #1a73e8;
  font-weight: 600;
  background: #f2f7ff;
}

.sub-tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.sub-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  background: #fff;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.sub-tab.active {
  color: #1a73e8;
  background: #f2f7ff;
}

.tip,
.empty {
  padding: 60rpx 0;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.empty {
  background: #fff;
  border-radius: 16rpx;
}

.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-name {
  display: block;
  font-size: 30rpx;
  color: #222;
}

.card-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #e8700a;
}

.card-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #999;
}

.tag {
  display: inline-block;
  margin-left: 12rpx;
  padding: 2rpx 10rpx;
  background: #fff3e0;
  color: #e8700a;
  border-radius: 8rpx;
  font-size: 20rpx;
}

.card-action {
  margin-left: 16rpx;
}

.mini-btn {
  padding: 14rpx 32rpx;
  background: #1a73e8;
  color: #fff;
  border-radius: 30rpx;
  font-size: 26rpx;
}

.mini-btn.disabled {
  background: #c8c8c8;
}

.status-text {
  font-size: 26rpx;
}

.st-unused {
  color: #1a73e8;
}

.st-used {
  color: #999;
}

.st-expired {
  color: #d93025;
}
</style>

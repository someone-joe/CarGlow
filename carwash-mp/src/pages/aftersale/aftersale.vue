<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        :class="['tab', activeTab === t.key && 'tab-active']"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <view v-else-if="list.length === 0" class="empty">
      <text class="empty-title">{{ activeTab === 'sale' ? '暂无售后工单' : '暂无退款记录' }}</text>
      <text class="empty-sub">
        {{ activeTab === 'sale' ? '洗不干净或有异议时，可在订单详情里发起售后' : '取消订单或售后同意的退款会出现在这里' }}
      </text>
    </view>

    <!-- 售后工单 -->
    <view v-for="item in saleList" :key="item.afterSaleNo ?? ''" class="card" @click="toggle(item.afterSaleNo)">
      <view class="card-top">
        <text class="no">{{ item.afterSaleNo }}</text>
        <text :class="['badge', 'badge-' + item.status]">{{ item.statusLabel || item.status }}</text>
      </view>
      <view class="meta">
        <text class="meta-line">关联订单 {{ item.orderNo }}</text>
        <text class="meta-line">类型 {{ typeLabel(item.type) }}</text>
        <text class="meta-line">涉及金额 ¥{{ formatAmount(item.amount) }}</text>
        <text v-if="item.reason" class="meta-line">申请原因 {{ item.reason }}</text>
      </view>

      <view v-if="openNo === item.afterSaleNo" class="detail">
        <view v-if="item.handleRemark" class="detail-row">
          <text class="detail-label">处置说明</text>
          <text class="detail-value">{{ item.handleRemark }}</text>
        </view>
        <view v-if="item.handlerBy" class="detail-row">
          <text class="detail-label">处置人</text>
          <text class="detail-value">{{ item.handlerBy }}</text>
        </view>
        <view v-if="item.handleTime" class="detail-row">
          <text class="detail-label">处置时间</text>
          <text class="detail-value">{{ formatTime(item.handleTime) }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">提交时间</text>
          <text class="detail-value">{{ formatTime(item.createTime) }}</text>
        </view>
        <!-- 退款类工单：把退款进度一并展示，客户不用自己猜钱到哪了 -->
        <view v-if="item.type === 'REFUND'" class="refund-box">
          <text class="refund-title">退款进度</text>
          <view v-if="refundLoading" class="refund-empty">查询中…</view>
          <view v-else-if="relatedRefunds(item.orderNo).length === 0" class="refund-empty">
            尚未生成退款单（运营同意后会由系统自动发起）
          </view>
          <view v-for="r in relatedRefunds(item.orderNo)" :key="r.refundNo ?? ''" class="refund-item">
            <text class="refund-no">{{ r.refundNo }}</text>
            <text :class="['badge', 'badge-' + r.status]">{{ r.statusLabel || r.status }}</text>
            <text v-if="r.failReason" class="refund-fail">失败原因：{{ r.failReason }}</text>
          </view>
        </view>
      </view>

      <text class="more">{{ openNo === (item.afterSaleNo ?? null) ? '收起 ▲' : '详情 ▼' }}</text>
    </view>

    <!-- 退款记录 -->
    <view v-for="item in refundList" :key="item.refundNo ?? ''" class="card">
      <view class="card-top">
        <text class="no">{{ item.refundNo }}</text>
        <text :class="['badge', 'badge-' + item.status]">{{ item.statusLabel || item.status }}</text>
      </view>
      <view class="meta">
        <text class="meta-line">关联订单 {{ item.orderNo }}</text>
        <text class="meta-line amount-line">退款金额 ¥{{ formatAmount(item.amount) }}</text>
        <text v-if="item.reason" class="meta-line">原因 {{ item.reason }}</text>
        <text v-if="item.failReason" class="meta-line fail-line">失败原因 {{ item.failReason }}</text>
        <text class="meta-line">发起时间 {{ formatTime(item.createTime) }}</text>
        <text v-if="item.updateTime" class="meta-line">最近更新 {{ formatTime(item.updateTime) }}</text>
      </view>
      <view v-if="item.status === 'FAILED'" class="notice">
        退款失败后系统会自动重试；若长时间未到账，可联系客服协助处理。
      </view>
      <view v-else-if="item.status === 'SUCCESS'" class="notice notice-ok">
        退款已成功，款项将按原支付渠道退回（微信零钱 / 银行卡一般 1-3 个工作日到账）。
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { fetchMyAfterSales, fetchMyRefunds, type AfterSaleVO, type RefundVO } from '@/api/aftersale'
import { BASE_URL } from '@/utils/request'

type TabKey = 'sale' | 'refund'

const tabs: { key: TabKey; label: string }[] = [
  { key: 'sale', label: '售后工单' },
  { key: 'refund', label: '退款记录' },
]

const activeTab = ref<TabKey>('sale')
const saleList = ref<AfterSaleVO[]>([])
const refundList = ref<RefundVO[]>([])
const loading = ref(false)
const refundLoading = ref(false)
const openNo = ref<string | null>(null)

const list = computed<unknown[]>(() => (activeTab.value === 'sale' ? saleList.value : refundList.value))

/** 类型中文名：这是售后域自己的类型，不是订单状态机枚举，前端展示用 */
function typeLabel(t?: string): string {
  return { REWASH: '重洗', REFUND: '退款', CLAIM: '理赔' }[t ?? ''] || t || '-'
}

function formatAmount(fen?: number): string {
  if (fen == null) return '0.00'
  return (fen / 100).toFixed(2)
}

function formatTime(ms?: number): string {
  if (!ms) return '-'
  const d = new Date(ms)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 退款类工单展开时，把该订单的退款单一起显示 */
function relatedRefunds(orderNo?: string): RefundVO[] {
  if (!orderNo) return []
  return refundList.value.filter((r) => r.orderNo === orderNo)
}

function toggle(no?: string) {
  openNo.value = openNo.value === no ? null : (no ?? null)
}

async function load() {
  loading.value = true
  try {
    if (activeTab.value === 'sale') {
      saleList.value = await fetchMyAfterSales()
    } else {
      refundList.value = await fetchMyRefunds()
    }
  } finally {
    loading.value = false
  }
}

/** 售后 tab 需要退款数据才能展示「退款进度」，两个 tab 都加载一次 */
async function loadAll() {
  loading.value = true
  try {
    const [sales, refunds] = await Promise.all([fetchMyAfterSales(), fetchMyRefunds()])
    saleList.value = sales
    refundList.value = refunds
  } finally {
    loading.value = false
  }
}

function switchTab(key: TabKey) {
  activeTab.value = key
  openNo.value = null
  load()
}

onShow(() => {
  loadAll()
})
</script>

<style scoped>
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 10rpx;
  margin-bottom: 24rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  font-size: 28rpx;
  color: #6b7d7a;
  border-radius: 14rpx;
}

.tab-active {
  background: #00aeb5;
  color: #fff;
  font-weight: 600;
}

.tip,
.empty {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  color: #14342f;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.empty-sub {
  display: block;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.no {
  font-size: 26rpx;
  color: #14342f;
  font-weight: 600;
}

.badge {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  background: #eef2f1;
  color: #6b7d7a;
}

.badge-INIT {
  background: #fff4e6;
  color: #e67e22;
}
.badge-PROCESSING {
  background: #e6f4ff;
  color: #1677ff;
}
.badge-DONE,
.badge-SUCCESS {
  background: #e6f7ee;
  color: #00a86b;
}
.badge-REJECTED,
.badge-FAILED {
  background: #fdecec;
  color: #e04b4b;
}

.meta-line {
  display: block;
  font-size: 25rpx;
  color: #6b7d7a;
  line-height: 1.9;
}

.amount-line {
  color: #ff5b4a;
  font-weight: 600;
}

.fail-line {
  color: #e04b4b;
}

.more {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #00aeb5;
  margin-top: 12rpx;
}

.detail {
  border-top: 1rpx solid #eef2f1;
  margin-top: 16rpx;
  padding-top: 16rpx;
}

.detail-row {
  display: flex;
  font-size: 25rpx;
  line-height: 1.9;
}

.detail-label {
  width: 140rpx;
  color: #8a9a98;
  flex-shrink: 0;
}

.detail-value {
  color: #14342f;
  flex: 1;
}

.refund-box {
  margin-top: 16rpx;
  background: #f7fbfb;
  border-radius: 14rpx;
  padding: 18rpx;
}

.refund-title {
  display: block;
  font-size: 25rpx;
  color: #14342f;
  font-weight: 600;
  margin-bottom: 10rpx;
}

.refund-empty {
  font-size: 24rpx;
  color: #8a9a98;
}

.refund-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 8rpx;
}

.refund-no {
  font-size: 24rpx;
  color: #14342f;
}

.refund-fail {
  font-size: 23rpx;
  color: #e04b4b;
  width: 100%;
}

.notice {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #b06a00;
  background: #fff8e6;
  border-radius: 12rpx;
  padding: 14rpx 18rpx;
  line-height: 1.7;
}

.notice-ok {
  color: #00875a;
  background: #e9f8f1;
}
</style>

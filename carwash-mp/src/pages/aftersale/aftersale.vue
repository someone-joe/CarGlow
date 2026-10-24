<template>
  <view class="cg-page aftersale">
    <view class="cg-tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="cg-tab"
        :class="[{ 'is-active': activeTab === t.key }, 'tab-' + t.key]"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
    </view>

    <view v-if="loading" class="cg-empty">加载中…</view>

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

/* ---- 工单卡 ---- */
.card {
  margin-bottom: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.no {
  flex: 1;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

/* 状态徽标：处理后绿、进行中蓝、驳回/失败红 */
.badge {
  flex: none;
  margin-left: 24rpx;
  padding: 8rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.badge-DONE,
.badge-SUCCESS {
  background: rgba(52, 199, 89, 0.12);
  color: var(--cg-success);
}

.badge-INIT,
.badge-PROCESSING {
  background: var(--cg-tint);
  color: var(--cg-primary);
}

.badge-REJECTED,
.badge-FAILED {
  background: rgba(255, 59, 48, 0.10);
  color: var(--cg-error);
}

.meta {
  margin-top: 24rpx;
}

.meta-line {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.amount-line {
  color: var(--cg-title);
}

.fail-line {
  color: var(--cg-error);
}

.more {
  display: block;
  margin-top: 24rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
}

/* ---- 展开详情 ---- */
.detail {
  margin-top: 24rpx;
  padding: 24rpx 32rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
}

.detail-label {
  flex: none;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.detail-value {
  flex: 1;
  margin-left: 32rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text);
  text-align: right;
}

/* ---- 退款进度 ---- */
.refund-box {
  margin-top: 24rpx;
  padding: 24rpx 32rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-md);
}

.refund-title {
  display: block;
  font-size: var(--cg-font-caption);
  font-weight: 500;
  color: var(--cg-title);
}

.refund-empty {
  padding: 24rpx 0;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.refund-item {
  padding: 24rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.refund-no {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text);
}

.refund-fail {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-error);
}

.notice {
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  line-height: 1.6;
}

.notice-ok {
  color: var(--cg-success);
}
</style>

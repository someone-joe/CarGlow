<template>
  <view class="cg-page selfwash-orders">
    <view class="cg-tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="cg-tab"
        :class="{ 'is-active': tab === t.key }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
    </view>

    <view v-if="loading" class="cg-empty">加载中…</view>
    <view v-else-if="!list.length" class="empty">暂无相关订单</view>

    <view v-for="o in list" :key="o.orderNo" class="card" @click="goDetail(o.orderNo)">
      <view class="card-top">
        <text class="svc">{{ o.serviceName || '自助洗车' }}</text>
        <text class="status" :class="statusClass(o.status)">{{ o.statusLabel }}</text>
      </view>
      <view class="card-sub">
        <text>¥{{ fen2yuan(o.payAmount) }} · {{ o.serviceMinutes }} 分钟</text>
        <text v-if="o.status === 'SERVING'">剩余 {{ fmtRemain(o.remainingSeconds) }}</text>
        <text v-else-if="o.status === 'QUEUING'">排队第 {{ o.queueNo }} 位</text>
      </view>
      <view class="card-sub muted">单号 {{ o.orderNo }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchSelfwashOrders, type SelfwashOrderVO } from '@/api/selfwash'

const tabs = [
  { key: 'WAIT_PAY', label: '待支付' },
  { key: 'WAIT_USE', label: '待使用' },
  { key: 'ONGOING', label: '进行中' },
  { key: 'DONE', label: '已完成' },
  { key: 'CANCELED', label: '已取消' },
]
const tab = ref('WAIT_PAY')
const list = ref<SelfwashOrderVO[]>([])
const loading = ref(true)

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function fmtRemain(sec?: number): string {
  const s = sec ?? 0
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

function statusClass(s: string): string {
  return s === 'SERVING' ? 'busy' : s === 'QUEUING' ? 'queue' : s === 'DONE' ? 'done' : s === 'CANCELED' ? 'cancel' : ''
}

async function load(): Promise<void> {
  loading.value = true
  try {
    list.value = (await fetchSelfwashOrders(tab.value)) || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function switchTab(key: string): void {
  tab.value = key
  load()
}

function goDetail(orderNo: string): void {
  uni.navigateTo({ url: `/pages/selfwash-detail/selfwash-detail?orderNo=${orderNo}` })
}

onShow(load)
</script>

<style>
/* ---- 订单卡 ---- */
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

.svc {
  flex: 1;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.status {
  flex: none;
  margin-left: 24rpx;
  padding: 8rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.status.busy {
  background: rgba(255, 59, 48, 0.10);
  color: var(--cg-error);
}

.status.queue {
  background: rgba(255, 149, 0, 0.12);
  color: var(--cg-warning);
}

.status.done {
  background: rgba(52, 199, 89, 0.12);
  color: var(--cg-success);
}

.status.cancel {
  background: rgba(134, 134, 139, 0.14);
  color: var(--cg-text-sub);
}

.card-sub {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.card-sub.muted {
  color: var(--cg-text-aux);
}
</style>

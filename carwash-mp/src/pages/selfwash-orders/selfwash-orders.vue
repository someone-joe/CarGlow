<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{ active: tab === t.key }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
    </view>

    <view v-if="loading" class="tip">加载中…</view>
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
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  font-size: 26rpx;
  color: #6b7b79;
  border-radius: 12rpx;
}

.tab.active {
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

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.svc {
  font-size: 29rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.status {
  font-size: 25rpx;
  font-weight: 600;
  color: #00aeb5;
}

.status.busy {
  color: #ff8a3d;
}

.status.queue {
  color: #7b6cf6;
}

.status.done {
  color: #8a9a98;
}

.status.cancel {
  color: #ff5b4a;
}

.card-sub {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #6b7b79;
}

.card-sub.muted {
  color: #b6c2c0;
  font-size: 22rpx;
}
</style>

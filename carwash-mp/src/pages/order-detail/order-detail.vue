<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="detail">
      <view class="summary">
        <text class="status">{{ detail.statusLabel }}</text>
        <text class="service">{{ detail.serviceName }}</text>
        <text class="plate">{{ detail.plateNo || '未填车牌' }}</text>
      </view>

      <view class="panel">
        <view class="panel-title">服务进度</view>
        <view v-for="(node, index) in timeline" :key="node.node" class="node">
          <view class="node-line">
            <view :class="['dot', node.reached && 'dot-reached', node.current && 'dot-current']" />
            <view v-if="index < timeline.length - 1" :class="['line', node.reached && 'line-reached']" />
          </view>
          <view class="node-body">
            <text :class="['node-title', !node.reached && 'node-title-todo']">{{ node.title }}</text>
            <text class="node-time">{{ node.time ? formatTime(node.time) : '待完成' }}</text>
          </view>
        </view>
      </view>

      <view class="panel">
        <view class="row"><text class="label">订单号</text><text class="value">{{ detail.orderNo }}</text></view>
        <view class="row"><text class="label">实付金额</text><text class="value">¥{{ formatAmount(detail.payAmount) }}</text></view>
        <view v-if="detail.remark" class="row"><text class="label">备注</text><text class="value">{{ detail.remark }}</text></view>
      </view>
    </block>

    <view v-else class="tip">订单不存在或无权查看</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchOrderDetail, type OrderDetailVO, type TimelineNodeVO } from '@/api/order'

const detail = ref<OrderDetailVO | null>(null)
const timeline = ref<TimelineNodeVO[]>([])
const loading = ref(true)

/** 金额：后端存分，只在展示层转元 */
function formatAmount(amount?: number | null): string {
  return ((amount ?? 0) / 100).toFixed(2)
}

function formatTime(ms: number): string {
  const d = new Date(ms)
  const pad = (n: number) => `${n}`.padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onLoad((options) => {
  const orderNo = (options as { orderNo?: string }).orderNo ?? ''
  if (!orderNo) {
    loading.value = false
    return
  }
  fetchOrderDetail(orderNo)
    .then((data) => {
      detail.value = data
      timeline.value = data.timeline ?? []
    })
    .catch(() => {
      detail.value = null
    })
    .finally(() => {
      loading.value = false
    })
})
</script>

<style>
.page {
  padding: 24rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #999;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.summary {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.status {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  color: #1a73e8;
}

.service {
  display: block;
  margin-top: 16rpx;
  font-size: 30rpx;
  color: #222;
}

.plate {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #888;
}

.panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.panel-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 24rpx;
  display: block;
}

.node {
  display: flex;
}

.node-line {
  width: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 20rpx;
}

.dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #d8d8d8;
  margin-top: 8rpx;
}

.dot-reached {
  background: #1a73e8;
}

.dot-current {
  background: #fff;
  border: 6rpx solid #1a73e8;
  width: 28rpx;
  height: 28rpx;
}

.line {
  flex: 1;
  width: 4rpx;
  background: #e6e6e6;
  margin: 8rpx 0;
}

.line-reached {
  background: #a8c7f0;
}

.node-body {
  padding-bottom: 36rpx;
}

.node-title {
  display: block;
  font-size: 30rpx;
  color: #222;
}

.node-title-todo {
  color: #aaa;
}

.node-time {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.label {
  font-size: 28rpx;
  color: #888;
}

.value {
  font-size: 28rpx;
  color: #222;
}
</style>

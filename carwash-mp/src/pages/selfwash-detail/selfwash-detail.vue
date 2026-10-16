<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="order">
      <!-- 状态头 -->
      <view class="status-bar">
        <text class="status">{{ order.statusLabel }}</text>
        <view v-if="order.status === 'SERVING'" class="countdown">
          <text class="cd-label">服务倒计时</text>
          <text class="cd-time">{{ countdown }}</text>
        </view>
        <text v-else-if="order.status === 'QUEUING'" class="queue-tip">排队第 {{ order.queueNo }} 位，前方订单结束后自动开始</text>
      </view>

      <view class="panel">
        <view class="panel-title">订单信息</view>
        <view class="row"><text class="label">服务</text><text class="value">{{ order.serviceName || '自助洗车' }}</text></view>
        <view v-if="order.plateNo" class="row"><text class="label">车牌</text><text class="value">{{ order.plateNo }}</text></view>
        <view class="row"><text class="label">时长</text><text class="value">{{ order.serviceMinutes }} 分钟<text v-if="order.addedSeconds">（已加时 {{ Math.floor(order.addedSeconds / 60) }} 分钟）</text></text></view>
        <view class="row"><text class="label">实付</text><text class="value price">¥{{ fen2yuan(order.payAmount) }}</text></view>
        <view v-if="order.couponDiscount" class="row"><text class="label">优惠抵扣</text><text class="value discount">-¥{{ fen2yuan(order.couponDiscount) }}</text></view>
        <view class="row"><text class="label">支付方式</text><text class="value">{{ order.payChannel === 'BALANCE' ? '储值余额' : '微信支付' }}</text></view>
        <view class="row"><text class="label">单号</text><text class="value">{{ order.orderNo }}</text></view>
      </view>

      <!-- 操作 -->
      <view class="ops">
        <template v-if="order.status === 'WAIT_PAY'">
          <view class="op primary" @click="goPay">去支付</view>
          <view class="op danger" @click="doCancel">取消订单</view>
        </template>
        <view v-if="order.status === 'WAIT_USE'" class="op primary" @click="doActivate">开始服务</view>
        <template v-if="order.status === 'SERVING'">
          <view class="op" @click="doAddTime">加时 10 分钟</view>
          <view class="op plain" @click="doFinish">结束服务</view>
        </template>
        <view v-if="order.status === 'WAIT_USE'" class="op danger" @click="doCancel">取消订单</view>
      </view>
    </block>

    <view v-else class="tip">订单不存在</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { ref } from 'vue'
import {
  activateSelfwash,
  addSelfwashTime,
  cancelSelfwash,
  fetchSelfwashOrder,
  finishSelfwash,
  type SelfwashOrderVO,
} from '@/api/selfwash'

const orderNo = ref('')
const order = ref<SelfwashOrderVO | null>(null)
const loading = ref(true)
const remain = ref(0)
const countdown = ref('00:00')
let timer: ReturnType<typeof setInterval> | null = null

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function tick(): void {
  const s = Math.max(0, remain.value)
  const m = Math.floor(s / 60)
  const ss = s % 60
  countdown.value = `${String(m).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
  if (s <= 0 && timer) {
    clearInterval(timer)
    timer = null
    load()
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const d = await fetchSelfwashOrder(orderNo.value)
    order.value = d
    if (d.status === 'SERVING') {
      remain.value = d.remainingSeconds || 0
      tick()
      if (timer) clearInterval(timer)
      timer = setInterval(() => {
        remain.value = Math.max(0, remain.value - 1)
        tick()
      }, 1000)
    } else if (timer) {
      clearInterval(timer)
      timer = null
    }
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

function goPay(): void {
  uni.redirectTo({ url: `/pages/selfwash-pay/selfwash-pay?orderNo=${orderNo.value}` })
}

async function doActivate(): Promise<void> {
  try {
    const d = await activateSelfwash(orderNo.value)
    order.value = d
    uni.showToast({ title: d.status === 'QUEUING' ? '已排队' : '已开始，设备通电', icon: 'none' })
    load()
  } catch {
    // 已提示
  }
}

/** 加时档位（分钟）。费用按本单实付/时长折算，与后端 Same 公式，仅用于展示预估 */
const ADD_MINUTES = 10

function addTimeCost(): number {
  const o = order.value
  if (!o || !o.payAmount || !o.serviceMinutes || o.serviceMinutes <= 0) return 0
  const unit = Math.max(1, Math.round(o.payAmount / o.serviceMinutes))
  return unit * ADD_MINUTES
}

async function doAddTime(): Promise<void> {
  const cost = addTimeCost()
  uni.showModal({
    title: '加时',
    content: cost > 0
      ? `加时 ${ADD_MINUTES} 分钟，将从储值余额扣除 ¥${fen2yuan(cost)}，倒计时相应延长。`
      : `加时 ${ADD_MINUTES} 分钟，倒计时相应延长。`,
    confirmText: cost > 0 ? `确认支付 ¥${fen2yuan(cost)}` : '确认',
    cancelText: '再想想',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const d = await addSelfwashTime(orderNo.value, ADD_MINUTES)
        order.value = d
        remain.value = d.remainingSeconds || 0
        tick()
        uni.showToast({ title: '已加时', icon: 'none' })
      } catch {
        // 已提示（余额不足等由 request 层统一提示）
      }
    },
  })
}

async function doFinish(): Promise<void> {
  uni.showModal({
    title: '结束服务',
    content: '结束后设备断电，订单完成，确定结束？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await finishSelfwash(orderNo.value)
        uni.showToast({ title: '已结束', icon: 'none' })
        load()
      } catch {
        // 已提示
      }
    },
  })
}

function doCancel(): void {
  uni.showModal({
    title: '取消订单',
    content: '取消后不可恢复，已支付将原路退回',
    editable: true,
    placeholderText: '请填写取消原因',
    success: async (res) => {
      if (!res.confirm) return
      const reason = (res.content ?? '').trim()
      if (!reason) {
        uni.showToast({ title: '请填写取消原因', icon: 'none' })
        return
      }
      try {
        await cancelSelfwash(orderNo.value, reason)
        uni.showToast({ title: '已取消', icon: 'none' })
        load()
      } catch {
        // 已提示
      }
    },
  })
}

onLoad((opts) => {
  orderNo.value = (opts as { orderNo?: string }).orderNo ?? ''
  if (orderNo.value) load()
  else loading.value = false
})

onUnload(() => {
  if (timer) clearInterval(timer)
})
</script>

<style>
.page {
  padding: 24rpx 24rpx 60rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 80rpx 0;
}

.status-bar {
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 36rpx 30rpx;
  margin-bottom: 20rpx;
  color: #fff;
}

.status {
  font-size: 36rpx;
  font-weight: 700;
}

.countdown {
  margin-top: 16rpx;
}

.cd-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}

.cd-time {
  display: block;
  margin-top: 6rpx;
  font-size: 56rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}

.queue-tip {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.95);
}

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

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 0;
}

.label {
  font-size: 26rpx;
  color: #8a9a98;
}

.value {
  font-size: 26rpx;
  color: #1b2b2a;
}

.value.price {
  color: #ff5b4a;
  font-weight: 700;
}

.value.discount {
  color: #ff5b4a;
}

.ops {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.op {
  flex: 1;
  min-width: 200rpx;
  text-align: center;
  background: #14342f;
  color: #fff;
  font-size: 29rpx;
  font-weight: 600;
  padding: 24rpx 0;
  border-radius: 40rpx;
}

.op.primary {
  background: #00aeb5;
}

.op.plain {
  background: #eef3f2;
  color: #6b7b79;
}

.op.danger {
  background: #ffeeeb;
  color: #ff5b4a;
}
</style>

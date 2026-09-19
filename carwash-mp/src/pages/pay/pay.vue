<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="detail">
      <view class="countdown">
        <text class="cd-label">剩余支付时间</text>
        <text class="cd-time">{{ countdown }}</text>
      </view>

      <view class="panel">
        <view class="row"><text class="label">服务项</text><text class="value">{{ detail.serviceName || '—' }}</text></view>
        <view class="row"><text class="label">预计工时</text><text class="value">{{ detail.workMinutes ?? '—' }} 分钟</text></view>
        <view class="row"><text class="label">钥匙柜</text><text class="value">{{ detail.cabinetName || '—' }}</text></view>
        <view class="row"><text class="label">车辆</text><text class="value">{{ detail.plateNo || '—' }}</text></view>
      </view>

      <view class="panel">
        <view class="row"><text class="label">服务费</text><text class="value">¥{{ fen2yuan(detail.originAmount) }}</text></view>
        <view v-if="detail.discountAmount" class="row">
          <text class="label">已优惠</text><text class="value discount">-¥{{ fen2yuan(detail.discountAmount) }}</text>
        </view>
        <view class="row total"><text class="label">应付金额</text><text class="value total-val">¥{{ fen2yuan(detail.payAmount) }}</text></view>
      </view>

      <view class="pay-type">
        <text class="pt-name">微信支付</text>
        <text class="pt-check">✓</text>
      </view>

      <view class="bottom">
        <text class="cancel" @click="cancel">取消订单</text>
        <text class="pay-btn" :class="{ disabled: paying }" @click="pay">{{ paying ? '支付中…' : '立即支付' }}</text>
      </view>
    </block>

    <view v-else class="tip">订单不存在或已支付</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { cancelOrder, fetchOrderDetail, fetchPrepay, payOrder, type OrderDetailVO } from '@/api/order'

const orderNo = ref('')
const detail = ref<OrderDetailVO | null>(null)
const loading = ref(true)
const paying = ref(false)
const countdown = ref('--:--')
let expireAt = 0
let timer: ReturnType<typeof setInterval> | null = null

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function tick(): void {
  if (!expireAt) {
    countdown.value = '--:--'
    return
  }
  const left = Math.max(0, expireAt - Date.now())
  const m = Math.floor(left / 60000)
  const s = Math.floor((left % 60000) / 1000)
  countdown.value = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  if (left <= 0 && timer) {
    clearInterval(timer)
    timer = null
    uni.showToast({ title: '支付超时，订单已取消', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1200)
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [d, prepay] = await Promise.all([fetchOrderDetail(orderNo.value), fetchPrepay(orderNo.value)])
    detail.value = d
    expireAt = prepay.expireAt ?? 0
    tick()
    if (expireAt) {
      timer = setInterval(tick, 1000)
    }
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function pay(): Promise<void> {
  if (paying.value) return
  paying.value = true
  try {
    await payOrder(orderNo.value)
    uni.showToast({ title: '支付成功', icon: 'none' })
    uni.redirectTo({ url: `/pages/order-detail/order-detail?orderNo=${orderNo.value}` })
  } catch {
    // request 层已提示
  } finally {
    paying.value = false
  }
}

function cancel(): void {
  uni.showModal({
    title: '取消订单',
    content: '取消后不可恢复，已支付将全额退款',
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
        await cancelOrder(orderNo.value, reason)
        uni.showToast({ title: '已取消', icon: 'none' })
        uni.navigateBack()
      } catch {
        // request 层已提示
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
  padding: 24rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 160rpx;
}

.tip {
  text-align: center;
  color: #999;
  padding: 80rpx 0;
}

.countdown {
  background: #1a73e8;
  color: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  text-align: center;
  margin-bottom: 24rpx;
}

.cd-label {
  display: block;
  font-size: 26rpx;
  opacity: 0.9;
}

.cd-time {
  display: block;
  font-size: 56rpx;
  font-weight: 600;
  margin-top: 8rpx;
  letter-spacing: 2rpx;
}

.panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
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

.value.discount {
  color: #e8700a;
}

.row.total {
  border-top: 1rpx solid #eee;
  margin-top: 12rpx;
  padding-top: 24rpx;
}

.total-val {
  font-size: 36rpx;
  color: #e8700a;
  font-weight: 600;
}

.pay-type {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.pt-name {
  font-size: 30rpx;
  color: #222;
}

.pt-check {
  color: #1a73e8;
  font-size: 32rpx;
}

.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  background: #fff;
  padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eee;
}

.cancel {
  font-size: 28rpx;
  color: #666;
  padding: 20rpx 32rpx;
}

.pay-btn {
  flex: 1;
  margin-left: 24rpx;
  background: #1a73e8;
  color: #fff;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 12rpx;
  font-size: 32rpx;
}

.pay-btn.disabled {
  background: #b8c6da;
}
</style>

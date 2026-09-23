<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="detail">
      <!-- 状态头：待支付 + 倒计时 -->
      <view class="status-bar">
        <text class="status">订单待支付</text>
        <view class="countdown">
          <text class="cd-label">剩余支付时间</text>
          <text class="cd-time">{{ countdown }}</text>
        </view>
      </view>

      <!-- 服务卡 -->
      <view class="panel">
        <view class="svc-row">
          <view class="svc-main">
            <text class="svc-name">{{ detail.serviceName || '—' }}</text>
            <text class="svc-sub">{{ detail.plateNo || '未填车牌' }}</text>
          </view>
          <text class="svc-price">¥{{ fen2yuan(detail.payAmount) }}</text>
        </view>
      </view>

      <!-- 费用明细 -->
      <view class="panel">
        <view class="panel-title">费用明细</view>
        <view class="row">
          <text class="label">服务费</text>
          <text class="value">¥{{ fen2yuan(detail.originAmount) }}</text>
        </view>
        <view v-if="detail.discountAmount" class="row">
          <text class="label">已优惠</text>
          <text class="value discount">-¥{{ fen2yuan(detail.discountAmount) }}</text>
        </view>
        <view class="row total">
          <text class="label">应付金额</text>
          <text class="value price">¥{{ fen2yuan(detail.payAmount) }}</text>
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="panel">
        <view class="panel-title">订单信息</view>
        <view class="row"><text class="label">订单号</text><text class="value">{{ detail.orderNo }}</text></view>
        <view v-if="detail.siteName" class="row">
          <text class="label">洗车站点</text><text class="value">{{ detail.siteName }}</text>
        </view>
        <view class="row"><text class="label">钥匙柜</text><text class="value">{{ detail.cabinetName || '—' }}</text></view>
        <view class="row"><text class="label">车辆</text><text class="value">{{ detail.plateNo || '—' }}</text></view>
      </view>

      <!-- 支付方式 -->
      <view class="panel pay-type">
        <view class="pt-main">
          <text class="pt-ico">💚</text>
          <text class="pt-name">微信支付</text>
        </view>
        <text class="pt-check">✓</text>
      </view>
    </block>

    <view v-else class="tip">订单不存在或已支付</view>

    <!-- 底部支付条 -->
    <view v-if="detail" class="bottom">
      <view class="bottom-left">
        <text class="bl-label">待支付</text>
        <text class="bl-price">¥{{ fen2yuan(detail.payAmount) }}</text>
      </view>
      <text class="cancel" @click="cancel">取消订单</text>
      <text class="pay-btn" :class="{ disabled: paying }" @click="pay">
        {{ paying ? '支付中…' : '立即支付' }}
      </text>
    </view>
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
  padding: 24rpx 24rpx 200rpx;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 32rpx 30rpx;
  margin-bottom: 20rpx;
}

.status {
  font-size: 34rpx;
  font-weight: 700;
  color: #fff;
}

.countdown {
  text-align: right;
}

.cd-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}

.cd-time {
  display: block;
  margin-top: 4rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2rpx;
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
  margin-bottom: 18rpx;
}

.svc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.svc-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.svc-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.svc-price {
  font-size: 40rpx;
  font-weight: 700;
  color: #ff5b4a;
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

.discount {
  color: #ff5b4a;
}

.total {
  border-top: 2rpx solid #eaf1f0;
  margin-top: 10rpx;
  padding-top: 20rpx;
}

.price {
  font-size: 34rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.pay-type {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pt-main {
  display: flex;
  align-items: center;
}

.pt-ico {
  font-size: 40rpx;
  margin-right: 14rpx;
}

.pt-name {
  font-size: 29rpx;
  color: #1b2b2a;
}

.pt-check {
  color: #00aeb5;
  font-size: 34rpx;
}

/* ---- 底部支付条 ---- */
.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  background: #fff;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.bottom-left {
  margin-right: auto;
}

.bl-label {
  display: block;
  font-size: 22rpx;
  color: #8a9a98;
}

.bl-price {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.cancel {
  font-size: 26rpx;
  color: #8a9a98;
  padding: 0 24rpx;
}

.pay-btn {
  background: #14342f;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  padding: 24rpx 46rpx;
  border-radius: 40rpx;
}

.pay-btn.disabled {
  background: #cfe0de;
}
</style>

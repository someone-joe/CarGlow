<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="order">
      <view class="amount-card">
        <text class="ac-label">应付金额</text>
        <text class="ac-value">¥{{ fen2yuan(order.payAmount) }}</text>
        <text class="ac-sub">{{ order.serviceName || '自助洗车' }} · 约 {{ order.serviceMinutes }} 分钟</text>
      </view>

      <!-- 支付方式 -->
      <view class="panel">
        <view class="panel-title">支付方式</view>
        <view class="pt-row" :class="{ on: channel === 'balance' }" @click="pickBalance">
          <view class="pt-main">
            <text class="pt-ico">💰</text>
            <view class="pt-text">
              <text class="pt-name">储值余额抵扣</text>
              <text class="pt-hint">可用 ¥{{ fen2yuan(walletAvailable) }}{{ balanceNotEnough ? '（余额不足，去充值）' : '' }}</text>
            </view>
          </view>
          <text v-if="channel === 'balance'" class="pt-check">✓</text>
        </view>
        <view class="pt-row" :class="{ on: channel === 'wechat' }" @click="channel = 'wechat'">
          <view class="pt-main">
            <text class="pt-ico">💚</text>
            <view class="pt-text">
              <text class="pt-name">微信支付</text>
              <text class="pt-hint link" @click.stop="goRecharge">储值充值 · 支付更优惠 ›</text>
            </view>
          </view>
          <text v-if="channel === 'wechat'" class="pt-check">✓</text>
        </view>
      </view>
    </block>

    <view v-else class="tip">订单不存在或已支付</view>

    <view v-if="order" class="bottom">
      <text class="cancel" @click="cancel">取消订单</text>
      <text class="pay-btn" :class="{ disabled: paying }" @click="pay">
        {{ paying ? '支付中…' : '立即支付' }}
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  cancelSelfwash,
  fetchSelfwashOrder,
  paySelfwashBalance,
  paySelfwashMock,
  type SelfwashOrderVO,
} from '@/api/selfwash'
import { fetchWallet } from '@/api/wallet'

const orderNo = ref('')
const order = ref<SelfwashOrderVO | null>(null)
const loading = ref(true)
const paying = ref(false)
const walletAvailable = ref(0)
const channel = ref<'wechat' | 'balance'>('wechat')

const balanceNotEnough = computed(() => walletAvailable.value < (order.value?.payAmount ?? 0))

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function pickBalance(): void {
  if (balanceNotEnough.value) {
    goRecharge()
    return
  }
  channel.value = 'balance'
}

function goRecharge(): void {
  uni.showModal({
    title: '储值充值更优惠',
    content: '充值不仅有赠送，还能用余额直接抵扣本单，是否前往充值？',
    confirmText: '去充值',
    cancelText: '再想想',
    success: (r) => {
      if (r.confirm) uni.navigateTo({ url: '/pages/recharge/recharge' })
    },
  })
}

async function load(): Promise<void> {
  loading.value = true
  try {
    order.value = await fetchSelfwashOrder(orderNo.value)
    fetchWallet()
      .then((w) => {
        walletAvailable.value = (w.balance ?? 0) + (w.giftBalance ?? 0)
      })
      .catch(() => {})
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function pay(): Promise<void> {
  if (paying.value) return
  if (channel.value === 'balance' && balanceNotEnough.value) {
    goRecharge()
    return
  }
  paying.value = true
  try {
    if (channel.value === 'balance') {
      await paySelfwashBalance(orderNo.value)
    } else {
      await paySelfwashMock(orderNo.value)
    }
    uni.showToast({ title: '支付成功', icon: 'none' })
    uni.redirectTo({ url: `/pages/selfwash-detail/selfwash-detail?orderNo=${orderNo.value}` })
  } catch {
    // request 层已提示
  } finally {
    paying.value = false
  }
}

function cancel(): void {
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

.amount-card {
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
  margin-bottom: 20rpx;
  color: #fff;
  text-align: center;
}

.ac-label {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.ac-value {
  display: block;
  margin-top: 10rpx;
  font-size: 60rpx;
  font-weight: 700;
}

.ac-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
}

.panel-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 8rpx;
}

.pt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.pt-row:last-child {
  border-bottom: none;
}

.pt-main {
  display: flex;
  align-items: center;
}

.pt-ico {
  font-size: 44rpx;
  margin-right: 16rpx;
}

.pt-name {
  display: block;
  font-size: 29rpx;
  color: #1b2b2a;
}

.pt-hint {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.pt-hint.link {
  color: #00aeb5;
}

.pt-check {
  color: #00aeb5;
  font-size: 34rpx;
}

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

.cancel {
  font-size: 26rpx;
  color: #8a9a98;
  padding: 0 24rpx;
}

.pay-btn {
  margin-left: auto;
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

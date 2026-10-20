<template>
  <view class="cg-page selfwash-pay">
    <view v-if="loading" class="cg-empty">加载中…</view>

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
            <view class="pt-ico pt-ico-wallet" />
            <view class="pt-text">
              <text class="pt-name">储值余额抵扣</text>
              <text class="pt-hint">可用 ¥{{ fen2yuan(walletAvailable) }}{{ balanceNotEnough ? '（余额不足，去充值）' : '' }}</text>
            </view>
          </view>
          <text v-if="channel === 'balance'" class="pt-check">✓</text>
        </view>
        <view class="pt-row" :class="{ on: channel === 'wechat' }" @click="channel = 'wechat'">
          <view class="pt-main">
            <view class="pt-ico pt-ico-wechat" />
            <view class="pt-text">
              <text class="pt-name">微信支付</text>
              <text class="pt-hint link" @click.stop="goRecharge">储值充值 · 支付更优惠 ›</text>
            </view>
          </view>
          <text v-if="channel === 'wechat'" class="pt-check">✓</text>
        </view>
      </view>
    </block>

    <view v-else class="cg-empty">订单不存在或已支付</view>

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
/* 底部支付条占位 */
.selfwash-pay {
  padding-bottom: 220rpx;
}

/* ---- 金额卡 ---- */
.amount-card {
  margin-top: 32rpx;
  padding: 56rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
  text-align: center;
}

.ac-label {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.ac-value {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.ac-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

/* ---- 支付方式 ---- */
.panel {
  margin-top: 32rpx;
  padding: 32rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.pt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  margin-top: 24rpx;
  padding: 24rpx;
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out;
}

.pt-row.on {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
}

.pt-main {
  display: flex;
  align-items: center;
  flex: 1;
}

/* 支付图标：低饱和扁平圆形，替代 emoji */
.pt-ico {
  flex: none;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
}

.pt-ico-wechat {
  background: radial-gradient(circle at 50% 50%, #b8e6c8 0%, #8fd4a8 100%);
}

.pt-ico-wallet {
  background: radial-gradient(circle at 50% 50%, #ffe3b8 0%, #ffcf8f 100%);
}

.pt-text {
  flex: 1;
  margin-left: 24rpx;
}

.pt-name {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-title);
}

.pt-hint {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.pt-hint.link {
  color: var(--cg-primary);
}

.pt-check {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: var(--cg-primary);
  color: #ffffff;
  font-size: var(--cg-font-note);
}

/* ---- 底部支付条：轻毛玻璃 ---- */
.bottom {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  padding: 16rpx 40rpx calc(16rpx + env(safe-area-inset-bottom));
  background: var(--cg-glass-bg);
  -webkit-backdrop-filter: blur(var(--cg-glass-blur));
  backdrop-filter: blur(var(--cg-glass-blur));
  border-top: var(--cg-glass-border);
  box-shadow: 0 -4rpx 40rpx rgba(0, 0, 0, 0.06);
}

.cancel {
  flex: 1;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.pay-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 240rpx;
  height: 104rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.pay-btn.disabled {
  opacity: 0.4;
}
</style>

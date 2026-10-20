<template>
  <view class="cg-page pay">
    <view v-if="loading" class="cg-empty">加载中…</view>

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
      <view class="panel">
        <view class="panel-title">支付方式</view>
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
        <view
          class="pt-row"
          :class="{ on: channel === 'balance', insufficient: balanceNotEnough }"
          @click="pickBalance"
        >
          <view class="pt-main">
            <view class="pt-ico pt-ico-wallet" />
            <view class="pt-text">
              <text class="pt-name">储值余额抵扣</text>
              <text class="pt-hint">
                可用 ¥{{ fen2yuan(walletAvailable) }}{{ balanceNotEnough ? '（余额不足，去充值）' : '' }}
              </text>
            </view>
          </view>
          <text v-if="channel === 'balance'" class="pt-check">✓</text>
        </view>
      </view>
    </block>

    <view v-else class="cg-empty">订单不存在或已支付</view>

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
import { computed, ref } from 'vue'
import { cancelOrder, fetchOrderDetail, fetchPrepay, payOrder, type OrderDetailVO } from '@/api/order'
import { fetchWallet, payByBalance } from '@/api/wallet'

const orderNo = ref('')
const detail = ref<OrderDetailVO | null>(null)
const loading = ref(true)
const paying = ref(false)
const countdown = ref('--:--')
let expireAt = 0
let timer: ReturnType<typeof setInterval> | null = null

// 支付渠道：微信支付 / 储值余额抵扣
type PayChannel = 'wechat' | 'balance'
const channel = ref<PayChannel>('wechat')
const walletAvailable = ref(0)
const balanceNotEnough = computed(() => walletAvailable.value < (detail.value?.payAmount ?? 0))

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
    success: (res) => {
      if (res.confirm) uni.navigateTo({ url: '/pages/recharge/recharge' })
    },
  })
}

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
    // 余额非关键路径，单独拉取，失败不影响下单支付
    fetchWallet()
      .then((w) => {
        walletAvailable.value = (w.balance ?? 0) + (w.giftBalance ?? 0)
      })
      .catch(() => {})
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
  if (channel.value === 'balance' && balanceNotEnough.value) {
    goRecharge()
    return
  }
  paying.value = true
  try {
    if (channel.value === 'balance') {
      await payByBalance(orderNo.value)
    } else {
      await payOrder(orderNo.value)
    }
    uni.showToast({ title: '支付成功', icon: 'none' })
    uni.redirectTo({ url: `/pages/order-detail/order-detail?orderNo=${orderNo.value}` })
  } catch {
    // request 层已提示（余额不足 B3004 等）
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
/* 底部支付条占位 */
.pay {
  padding-bottom: 220rpx;
}

/* ---- 状态头：待支付 + 倒计时 ---- */
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.status {
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.countdown {
  text-align: right;
}

.cd-label {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.cd-time {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-error);
}

/* ---- 面板 ---- */
.panel {
  margin-top: 32rpx;
  padding: 40rpx;
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

.svc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.svc-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.svc-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.svc-price {
  margin-left: 32rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72rpx;
}

.label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.value {
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.discount {
  color: var(--cg-error);
}

.total .label {
  color: var(--cg-title);
}

.total .price {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

/* ---- 支付方式 ---- */
.pt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out;
}

.pt-row + .pt-row {
  margin-top: 24rpx;
}

.pt-row.on {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
}

.pt-row.insufficient {
  opacity: 0.6;
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

.bl-label {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.bl-price {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.cancel {
  flex: 1;
  margin-left: 32rpx;
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

<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 余额卡 -->
      <view class="balance-card">
        <view class="bc-total">
          <text class="bc-total-label">账户总余额（元）</text>
          <text class="bc-total-value">{{ yuan(total) }}</text>
        </view>
        <view class="bc-split">
          <view class="bc-item">
            <text class="bc-item-label">充值本金</text>
            <text class="bc-item-value">{{ yuan(wallet.balance) }}</text>
          </view>
          <view class="bc-item">
            <text class="bc-item-label">赠送余额</text>
            <text class="bc-item-value">{{ yuan(wallet.giftBalance) }}</text>
          </view>
        </view>
        <view class="bc-recharge" @click="goRecharge">充值</view>
      </view>

      <!-- 流水 -->
      <view class="panel">
        <view class="panel-title">收支明细</view>
        <view v-if="!flows.length" class="empty">暂无流水</view>
        <view v-for="f in flows" :key="f.flowNo" class="flow">
          <view class="flow-main">
            <text class="flow-type">{{ typeLabel(f.bizType) }}</text>
            <text class="flow-time">{{ fmtTime(f.createTime) }}</text>
          </view>
          <view class="flow-amount">
            <text v-if="f.changeAmount" :class="['flow-num', f.changeAmount > 0 ? 'in' : 'out']">
              {{ f.changeAmount > 0 ? '+' : '-' }}{{ yuan(Math.abs(f.changeAmount)) }}
            </text>
            <text v-if="f.giftChange" :class="['flow-gift', f.giftChange > 0 ? 'in' : 'out']">
              赠{{ f.giftChange > 0 ? '+' : '-' }}{{ yuan(Math.abs(f.giftChange)) }}
            </text>
          </view>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchWallet, fetchWalletFlows, type WalletFlowVO, type WalletVO } from '@/api/wallet'

const loading = ref(true)
const wallet = ref<WalletVO>({ balance: 0, giftBalance: 0, totalRecharge: 0 })
const flows = ref<WalletFlowVO[]>([])

const total = ref(0)

function yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function typeLabel(t: string): string {
  const map: Record<string, string> = {
    RECHARGE: '储值充值',
    GIFT: '充值赠送',
    PAY: '余额支付',
    REFUND: '退款返还',
  }
  return map[t] ?? t
}

function fmtTime(t: string): string {
  if (!t) return ''
  // 后端返回 yyyy-MM-ddTHH:mm:ss，截成友好格式
  return t.replace('T', ' ').slice(0, 16)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [w, fp] = await Promise.all([fetchWallet(), fetchWalletFlows(1, 30)])
    wallet.value = w
    total.value = (w.balance ?? 0) + (w.giftBalance ?? 0)
    flows.value = fp.list ?? []
  } catch {
    // request 层已提示
  } finally {
    loading.value = false
  }
}

function goRecharge(): void {
  uni.navigateTo({ url: '/pages/recharge/recharge' })
}

onShow(() => {
  load()
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

.balance-card {
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  margin-bottom: 20rpx;
  color: #fff;
  position: relative;
}

.bc-total-label {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.bc-total-value {
  display: block;
  margin-top: 10rpx;
  font-size: 64rpx;
  font-weight: 700;
}

.bc-split {
  display: flex;
  margin-top: 30rpx;
  padding-top: 26rpx;
  border-top: 2rpx solid rgba(255, 255, 255, 0.25);
}

.bc-item {
  flex: 1;
}

.bc-item-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}

.bc-item-value {
  display: block;
  margin-top: 6rpx;
  font-size: 30rpx;
  font-weight: 600;
}

.bc-recharge {
  position: absolute;
  right: 32rpx;
  top: 40rpx;
  background: #fff;
  color: #0e8f94;
  font-size: 26rpx;
  font-weight: 600;
  padding: 12rpx 32rpx;
  border-radius: 40rpx;
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

.empty {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.flow:last-child {
  border-bottom: none;
}

.flow-type {
  display: block;
  font-size: 28rpx;
  color: #1b2b2a;
}

.flow-time {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8a9a98;
}

.flow-amount {
  text-align: right;
}

.flow-num {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
}

.flow-gift {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
}

.in {
  color: #ff5b4a;
}

.out {
  color: #1b2b2a;
}
</style>

<template>
  <view class="cg-page wallet">
    <view v-if="loading" class="cg-empty">加载中…</view>

    <block v-else>
      <!-- 余额卡 -->
      <view class="balance-card">
        <view class="bc-head">
          <view class="bc-total">
            <text class="bc-total-label">账户总余额（元）</text>
            <text class="bc-total-value">{{ yuan(total) }}</text>
          </view>
          <view class="bc-recharge" @click="goRecharge">充值</view>
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
/* ---- 余额总卡 ---- */
.balance-card {
  margin-top: 32rpx;
  padding: 48rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

/* 余额行：左金额、右充值按钮，按钮与金额块垂直居中 */
.bc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bc-total {
  flex: 1;
}

.bc-total-label {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.bc-total-value {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.bc-split {
  display: flex;
  margin-top: 32rpx;
}

.bc-item {
  flex: 1;
}

.bc-item-label {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.bc-item-value {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-text);
}

.bc-recharge {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 放到总余额右侧后收一档尺寸，避免压过大号金额 */
  width: 160rpx;
  height: 72rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: #ffffff;
}

/* ---- 流水面板 ---- */
.panel {
  margin-top: 32rpx;
  padding: 8rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  padding: 32rpx 0 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.empty {
  padding: 64rpx 0;
  text-align: center;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-aux);
}

.flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.flow-type {
  flex: 1;
  font-size: var(--cg-font-body);
  color: var(--cg-text);
}

.flow-time {
  flex: none;
  margin: 0 24rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.flow-amount {
  flex: none;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.flow-num {
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.flow-gift {
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.in .flow-amount {
  color: var(--cg-success);
}
</style>

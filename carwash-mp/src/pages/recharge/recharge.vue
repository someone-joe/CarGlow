<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <view class="current">
        <text class="cur-label">当前余额</text>
        <text class="cur-value">¥{{ yuan(total) }}</text>
      </view>

      <!-- 赠送档位 -->
      <view class="panel">
        <view class="panel-title">选择充值金额</view>
        <view class="tiers">
          <view
            v-for="r in rules"
            :key="r.ruleId"
            class="tier"
            :class="{ on: selectedRuleId === r.ruleId }"
            @click="pickRule(r)"
          >
            <text class="tier-amount">充{{ yuan(r.rechargeFen) }}</text>
            <text v-if="r.giftFen > 0" class="tier-gift">送{{ yuan(r.giftFen) }}</text>
            <text v-else class="tier-gift none">无赠送</text>
          </view>
        </view>

        <!-- 自定义金额 -->
        <view class="custom">
          <text class="custom-label">自定义金额</text>
          <input
            v-model="customAmount"
            class="custom-input"
            type="digit"
            placeholder="输入金额，自定义不赠送"
            @input="onCustomInput"
          />
        </view>
      </view>

      <view class="tip-inline">储值金额仅可消费，不可提现。本地开发使用模拟支付到账。</view>

      <view class="submit" :class="{ disabled: submitting }" @click="onSubmitClick">
        {{ submitting ? '充值中…' : '立即充值' }}
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchRechargeRules, fetchWallet, mockPayRecharge, recharge, type GiftRuleVO } from '@/api/wallet'
import { requirePhone } from '@/store/auth'

const loading = ref(true)
const submitting = ref(false)
const rules = ref<GiftRuleVO[]>([])
const total = ref(0)
const selectedRuleId = ref<number | null>(null)
const customAmount = ref('')

function yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function pickRule(r: GiftRuleVO): void {
  selectedRuleId.value = r.ruleId
  customAmount.value = ''
}

function onCustomInput(): void {
  if (customAmount.value) selectedRuleId.value = null
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [rs, w] = await Promise.all([fetchRechargeRules(), fetchWallet()])
    rules.value = rs ?? []
    total.value = (w.balance ?? 0) + (w.giftBalance ?? 0)
  } catch {
    // request 层已提示
  } finally {
    loading.value = false
  }
}

/** 充值入口：先过手机号授权门控——已绑直接充值，未绑弹授权、登后自动继续 */
function onSubmitClick(): void {
  requirePhone(() => submit())
}

async function submit(): Promise<void> {
  if (submitting.value) return
  const customFen = customAmount.value ? Math.round(Number(customAmount.value) * 100) : 0
  if (!selectedRuleId.value && !(customFen > 0)) {
    uni.showToast({ title: '请选择金额或输入自定义金额', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const res = await recharge(
      selectedRuleId.value ? { ruleId: selectedRuleId.value } : { amountFen: customFen }
    )
    // 本地开发：直接模拟支付到账（微信渠道后补）
    await mockPayRecharge(res.rechargeNo)
    uni.showToast({ title: '充值成功', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch {
    // request 层已提示
  } finally {
    submitting.value = false
  }
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

.current {
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  color: #fff;
}

.cur-label {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.cur-value {
  display: block;
  margin-top: 8rpx;
  font-size: 48rpx;
  font-weight: 700;
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
  margin-bottom: 20rpx;
}

.tiers {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.tier {
  width: calc(33.333% - 14rpx);
  box-sizing: border-box;
  border: 2rpx solid #e3ecea;
  border-radius: 16rpx;
  padding: 26rpx 0;
  text-align: center;
}

.tier.on {
  border-color: #00aeb5;
  background: #eaf8f8;
}

.tier-amount {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1b2b2a;
}

.tier-gift {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #ff5b4a;
}

.tier-gift.none {
  color: #b6c2c0;
}

.custom {
  display: flex;
  align-items: center;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid #f2f7f7;
}

.custom-label {
  font-size: 28rpx;
  color: #1b2b2a;
  margin-right: 20rpx;
}

.custom-input {
  flex: 1;
  background: #f2f7f7;
  border-radius: 12rpx;
  padding: 18rpx 20rpx;
  font-size: 28rpx;
}

.tip-inline {
  font-size: 22rpx;
  color: #8a9a98;
  line-height: 34rpx;
  padding: 0 8rpx 24rpx;
}

.submit {
  background: #14342f;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  text-align: center;
  padding: 26rpx 0;
  border-radius: 44rpx;
}

.submit.disabled {
  background: #cfe0de;
}
</style>

<template>
  <view class="cg-page recharge">
    <view v-if="loading" class="cg-empty">加载中…</view>

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

/** 自定义充值金额下限（分）：产品要求最低 ¥1，与后端 CUSTOM_MIN_FEN、契约 amountFen.minimum 一致 */
const CUSTOM_MIN_FEN = 100

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
  if (!selectedRuleId.value && customFen < CUSTOM_MIN_FEN) {
    uni.showToast({ title: '自定义金额最低 ¥1', icon: 'none' })
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
/* 底部提交条占位 */
.recharge {
  padding-bottom: 220rpx;
}

/* ---- 当前余额 ---- */
.current {
  margin-top: 32rpx;
  padding: 48rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.cur-label {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.cur-value {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

/* ---- 档位面板 ---- */
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

.tiers {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -8rpx;
}

/* 档位卡：选中 = 2px 蓝边 + 6% 淡蓝底 + 提升阴影，不放大 */
.tier {
  box-sizing: border-box;
  width: calc(33.333% - 16rpx);
  margin: 8rpx;
  padding: 32rpx 16rpx;
  background: var(--cg-card);
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  text-align: center;
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.tier.on {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
  box-shadow: var(--cg-shadow-raised);
}

.tier-amount {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
}

.tier-gift {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-error);
}

.tier-gift.none {
  color: var(--cg-text-aux);
}

/* ---- 自定义金额 ---- */
.custom {
  margin-top: 32rpx;
}

.custom-label {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.custom-input {
  box-sizing: border-box;
  width: 100%;
  /* 小程序原生 input 不定高会坍缩，故定高（与页面 104rpx 按钮协调）。
     上下 padding 留 0：原生 input 自身垂直居中，加了上下 padding 会把文字顶偏上去。 */
  height: 96rpx;
  margin-top: 16rpx;
  padding: 0 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.tip-inline {
  display: block;
  margin-top: 32rpx;
  padding: 0 8rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  line-height: 1.6;
}

/* ---- 底部提交条 ---- */
.submit {
  position: fixed;
  left: 40rpx;
  right: 40rpx;
  bottom: calc(32rpx + env(safe-area-inset-bottom));
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.submit.disabled {
  opacity: 0.4;
}
</style>

<template>
  <view v-if="gate.visible" class="pa-mask">
    <view class="pa-dialog">
      <text class="pa-title">绑定手机号</text>
      <text class="pa-desc">下单、充值、查询订单需要手机号，方便我们联系您并保障账户安全。授权后即可继续。</text>
      <button class="pa-btn" open-type="getPhoneNumber" @getphonenumber="onGetPhone">授权手机号并继续</button>
      <text class="pa-later" @click="cancel">暂不</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { authGate, submitPhoneCode, cancelPhoneAuth } from '@/store/auth'

const gate = authGate

/** 微信「获取手机号」回调：用户点允许才带 code；拒绝则 code 为空 */
function onGetPhone(e: { detail?: { code?: string } }): void {
  const code = e.detail?.code
  if (!code) {
    uni.showToast({ title: '已取消授权', icon: 'none' })
    return
  }
  submitPhoneCode(code)
}

function cancel(): void {
  cancelPhoneAuth()
}
</script>

<style scoped>
.pa-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 99990;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pa-fade 0.25s ease;
}

@keyframes pa-fade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.pa-dialog {
  box-sizing: border-box;
  width: 560rpx;
  padding: 48rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-raised);
  text-align: center;
  animation: pa-rise 0.25s ease;
}

@keyframes pa-rise {
  from {
    transform: translateY(40rpx);
    opacity: 0.5;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.pa-title {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.pa-desc {
  display: block;
  margin-top: 24rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
  line-height: 1.6;
}

.pa-btn {
  height: 104rpx;
  margin-top: 48rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
  line-height: 104rpx;
}

.pa-btn::after {
  border: none;
}

.pa-later {
  display: inline-block;
  min-height: 96rpx;
  margin-top: 16rpx;
  padding: 0 32rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
  line-height: 96rpx;
}
</style>

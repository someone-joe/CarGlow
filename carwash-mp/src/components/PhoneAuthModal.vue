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
  background: rgba(0, 0, 0, 0.5);
  z-index: 99990;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pa-dialog {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.pa-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #14342f;
}
.pa-desc {
  margin-top: 24rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: #6b7b79;
  text-align: center;
}
.pa-btn {
  margin-top: 40rpx;
  width: 100%;
  background: #00aeb5;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
  padding: 22rpx 0;
}
.pa-later {
  margin-top: 24rpx;
  font-size: 26rpx;
  color: #9aa8a6;
}
</style>

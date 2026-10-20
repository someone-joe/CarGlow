<template>
  <view class="cg-page settings">
    <!-- 消息通知 -->
    <view class="panel">
      <text class="panel-title">消息通知</text>
      <view class="row">
        <view class="row-main">
          <text class="row-label">订单状态提醒</text>
          <text class="row-sub">下单、存钥匙、洗好、完成等节点通知</text>
        </view>
        <view class="switch-box" :class="{ on: orderNotify }" @click="toggle('orderNotify')">
          <view class="switch-dot" />
        </view>
      </view>
      <view class="row">
        <view class="row-main">
          <text class="row-label">优惠活动推送</text>
          <text class="row-sub">优惠券到期、储值活动等（每月不超过 4 条）</text>
        </view>
        <view class="switch-box" :class="{ on: promoNotify }" @click="toggle('promoNotify')">
          <view class="switch-dot" />
        </view>
      </view>
    </view>

    <!-- 地址管理 -->
    <view class="panel">
      <text class="panel-title">地址管理</text>
      <view class="link-row" @click="go('/pages/park-location/park-location')">
        <text class="row-label">常用停车位置</text>
        <text class="arrow">›</text>
      </view>
      <view class="link-row" @click="go('/pages/vehicle/vehicle')">
        <text class="row-label">我的车辆</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 隐私与协议 -->
    <view class="panel">
      <text class="panel-title">隐私与协议</text>
      <view class="link-row" @click="doc('服务协议')">
        <text class="row-label">代客洗车服务协议</text>
        <text class="arrow">›</text>
      </view>
      <view class="link-row" @click="doc('隐私政策')">
        <text class="row-label">隐私政策</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="note">通知开关为本地偏好；微信服务通知的订阅/退订在收到对应消息时管理。</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

/** 通知开关：本地偏好（存储驱动）。后续接微信订阅消息（T11）时再与后端对齐。 */
const orderNotify = ref(read('orderNotify', true))
const promoNotify = ref(read('promoNotify', false))

function read(key: string, def: boolean): boolean {
  try {
    const v = uni.getStorageSync('carglow_notify_' + key)
    return v === '' ? def : v === '1'
  } catch {
    return def
  }
}

function write(key: string, val: boolean): void {
  try {
    uni.setStorageSync('carglow_notify_' + key, val ? '1' : '0')
  } catch {
    // ignore
  }
}

function toggle(key: 'orderNotify' | 'promoNotify'): void {
  if (key === 'orderNotify') {
    orderNotify.value = !orderNotify.value
    write('orderNotify', orderNotify.value)
  } else {
    promoNotify.value = !promoNotify.value
    write('promoNotify', promoNotify.value)
  }
}

function go(url: string): void {
  uni.navigateTo({ url })
}

function doc(name: string): void {
  uni.showToast({ title: `${name}（后台可编辑富文本，暂未接入）`, icon: 'none' })
}
</script>

<style>
/* ---- 面板 ---- */
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
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

/* ---- 开关行 ---- */
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 128rpx;
  padding: 24rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.row-main {
  flex: 1;
  margin-right: 32rpx;
}

.row-label {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-title);
}

.row-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- iOS 开关 ---- */
.switch-box {
  flex: none;
  box-sizing: border-box;
  width: 96rpx;
  height: 56rpx;
  padding: 6rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  transition: background-color 0.2s ease;
}

.switch-box.on {
  background: var(--cg-primary);
}

.switch-dot {
  width: 44rpx;
  height: 44rpx;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
}

.switch-box.on .switch-dot {
  transform: translateX(40rpx);
}

/* ---- 链接行 ---- */
.link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 108rpx;
  border-top: 1rpx solid var(--cg-line);
}

.arrow {
  color: var(--cg-text-aux);
  font-size: var(--cg-font-caption);
}
</style>

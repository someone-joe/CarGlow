<template>
  <view class="page">
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
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.panel-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 8rpx;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.row:last-child {
  border-bottom: none;
}

.row-main {
  flex: 1;
}

.row-label {
  display: block;
  font-size: 28rpx;
  color: #1b2b2a;
}

.row-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.switch-box {
  width: 88rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background: #dfe6e5;
  padding: 6rpx;
  box-sizing: border-box;
  flex-shrink: 0;
}

.switch-box.on {
  background: #00aeb5;
}

.switch-dot {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.15s;
}

.switch-box.on .switch-dot {
  transform: translateX(40rpx);
}

.link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.link-row:last-child {
  border-bottom: none;
}

.arrow {
  color: #b6c2c0;
  font-size: 32rpx;
}

.note {
  text-align: center;
  font-size: 22rpx;
  color: #8a9a98;
  line-height: 34rpx;
  padding: 8rpx 24rpx;
}
</style>

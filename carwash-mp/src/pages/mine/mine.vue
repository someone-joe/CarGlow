<template>
  <view class="page">
    <SiteBar />

    <!-- 用户卡（余额/充值无后端支撑，按约定隐藏） -->
    <view class="profile">
      <text class="avatar">🚗</text>
      <view class="profile-main">
        <text class="nick">{{ phoneBound ? '已绑定手机号' : '微信用户' }}</text>
        <text class="sub">夜间代客洗车 · 睡一觉车就干净了</text>
      </view>
    </view>

    <!-- 常用入口：卡券 / 订单 / 车辆 -->
    <view class="quick">
      <view class="quick-item" @click="go('/pages/coupons/coupons')">
        <text class="quick-ico">🎟️</text>
        <text class="quick-text">我的卡券</text>
      </view>
      <view class="quick-item" @click="go('/pages/orders/orders')">
        <text class="quick-ico">📋</text>
        <text class="quick-text">全部订单</text>
      </view>
      <view class="quick-item" @click="go('/pages/vehicle/vehicle')">
        <text class="quick-ico">🚙</text>
        <text class="quick-text">我的车辆</text>
      </view>
    </view>

    <!-- 常用功能 -->
    <view class="list">
      <view class="list-item" @click="switchTo('/pages/customer-service/customer-service')">
        <text>客服中心</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="toast('服务条款')">
        <text>服务条款</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="toast('隐私政策')">
        <text>隐私政策</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="about">
        <text>关于 CarGlow</text><text class="arrow">›</text>
      </view>
    </view>

    <!-- 师傅端入口（入口 A） -->
    <view class="list">
      <view class="list-item worker" @click="go('/pages/worker-login/worker-login')">
        <text>我是洗车师傅</text><text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SiteBar from '@/components/SiteBar.vue'

const phoneBound = ref(false)

function go(url: string): void {
  uni.navigateTo({ url })
}

function switchTo(url: string): void {
  uni.switchTab({ url })
}

function toast(name: string): void {
  uni.showToast({ title: `${name}（后台可编辑富文本，V1.0 暂未接入）`, icon: 'none' })
}

function about(): void {
  uni.showModal({
    title: '关于 CarGlow',
    content: '夜间代客洗车平台\n中央站集中洗 + 社区智能钥匙柜',
    showCancel: false,
  })
}
</script>

<style>
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.profile {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
}

.avatar {
  font-size: 76rpx;
  margin-right: 24rpx;
}

.profile-main {
  flex: 1;
}

.nick {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.quick {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx 0;
  margin-bottom: 20rpx;
}

.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.quick-ico {
  font-size: 48rpx;
}

.quick-text {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #6b7b79;
}

.list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 28rpx;
  font-size: 29rpx;
  color: #1b2b2a;
  border-bottom: 2rpx solid #f2f7f7;
}

.list-item:last-child {
  border-bottom: none;
}

.list-item.worker {
  color: #00aeb5;
}

.arrow {
  color: #b6c2c0;
  font-size: 32rpx;
}
</style>

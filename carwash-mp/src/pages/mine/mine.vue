<template>
  <view class="page">
    <SiteBar />

    <view class="profile">
      <text class="avatar">🚗</text>
      <view class="profile-main">
        <text class="nick">{{ phoneBound ? '已绑定手机号' : '微信用户' }}</text>
        <text class="sub">夜间代客洗车 · 睡一觉车就干净了</text>
      </view>
    </view>

    <view class="grid">
      <view class="grid-item" @click="go('/pages/orders/orders')">
        <text class="grid-ico">📋</text><text class="grid-text">我的订单</text>
      </view>
      <view class="grid-item" @click="go('/pages/vehicle/vehicle')">
        <text class="grid-ico">🚙</text><text class="grid-text">我的车辆</text>
      </view>
      <view class="grid-item" @click="go('/pages/coupons/coupons')">
        <text class="grid-ico">🎟️</text><text class="grid-text">我的卡券</text>
      </view>
      <view class="grid-item" @click="go('/pages/insurance/insurance')">
        <text class="grid-ico">🛡️</text><text class="grid-text">我的保单</text>
      </view>
    </view>

    <view class="list">
      <view class="list-item" @click="switchTo('/pages/customer-service/customer-service')">
        <text>联系客服</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="toast('服务条款')">
        <text>服务条款</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="toast('隐私政策')">
        <text>隐私政策</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="toast('保险与理赔说明')">
        <text>保险与理赔说明</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="about">
        <text>关于 CarGlow</text><text class="arrow">›</text>
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
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.profile {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.avatar {
  font-size: 72rpx;
  margin-right: 24rpx;
}

.profile-main {
  flex: 1;
}

.nick {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #222;
}

.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.grid {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 0;
  margin-bottom: 24rpx;
}

.grid-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.grid-ico {
  font-size: 48rpx;
}

.grid-text {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #555;
}

.list {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 28rpx;
  font-size: 30rpx;
  color: #222;
  border-bottom: 1rpx solid #f2f2f2;
}

.arrow {
  color: #ccc;
  font-size: 32rpx;
}
</style>

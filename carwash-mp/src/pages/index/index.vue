<template>
  <view class="page">
    <view class="hero">
      <text class="title">夜间代客洗车</text>
      <text class="subtitle">今晚下单，明早干净上路</text>
    </view>

    <view class="entry" @click="goOrders">
      <text class="entry-title">我的订单</text>
      <text class="entry-desc">查看进行中 / 待评价 / 全部订单</text>
    </view>
  </view>
</template>

<script setup lang="ts">
function goOrders(): void {
  uni.navigateTo({ url: '/pages/orders/orders' })
}
</script>

<style>
.page {
  padding: 32rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.hero {
  padding: 48rpx 0;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
  color: #222;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #888;
}

.entry {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}

.entry-title {
  display: block;
  font-size: 32rpx;
  color: #222;
}

.entry-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #999;
}
</style>

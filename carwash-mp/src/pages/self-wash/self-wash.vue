<template>
  <view class="page">
    <view class="hero">
      <text class="hero-ico">🚿</text>
      <text class="hero-title">自助洗车</text>
      <text class="hero-sub">到店自助 · 计时计费 · 随到随洗</text>
    </view>

    <view class="panel">
      <view class="panel-title">怎么洗</view>
      <view class="step">
        <text class="step-no">1</text>
        <view class="step-main">
          <text class="step-name">选工位下单</text>
          <text class="step-desc">查看实时工位状态（空闲/服务中）与排队人数</text>
        </view>
      </view>
      <view class="step">
        <text class="step-no">2</text>
        <view class="step-main">
          <text class="step-name">到店激活</text>
          <text class="step-desc">订单页点击「开始服务」，设备通电启动</text>
        </view>
      </view>
      <view class="step">
        <text class="step-no">3</text>
        <view class="step-main">
          <text class="step-name">计时服务</text>
          <text class="step-desc">高压水枪 / 泡沫 / 吸尘，可随时加时</text>
        </view>
      </view>
      <view class="step">
        <text class="step-no">4</text>
        <view class="step-main">
          <text class="step-name">断电结算</text>
          <text class="step-desc">时间到自动断电，订单自动完成</text>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-title">计费说明</view>
      <view class="row"><text class="label">计费方式</text><text class="value">按服务时长计费</text></view>
      <view class="row"><text class="label">支付方式</text><text class="value">微信支付 / 储值余额 / 优惠券</text></view>
      <view class="row"><text class="label">前方有单</text><text class="value">需等前方订单结束后方可开始</text></view>
    </view>

    <view class="cta" @click="start">开始自助洗车</view>
    <view class="note">工位选择与实时排队功能开发中，上线后即可在此选位下单。</view>
  </view>
</template>

<script setup lang="ts">
/** 自助洗车介绍页。工位选择/排队前端（需求 3.1 / 任务 T9）尚未开发，CTA 先给出提示，后续替换为工位页跳转。 */
function start(): void {
  uni.showModal({
    title: '即将上线',
    content: '自助洗车工位选择与实时排队功能开发中，上线后即可在此选位下单。',
    confirmText: '知道了',
    showCancel: false,
  })
}
</script>

<style>
.page {
  padding: 0 24rpx 60rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.hero {
  margin: 0 -24rpx 24rpx;
  padding: 56rpx 32rpx 44rpx;
  background: linear-gradient(135deg, #0e8f94, #14342f);
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
}

.hero-ico {
  font-size: 72rpx;
}

.hero-title {
  margin-top: 12rpx;
  font-size: 40rpx;
  font-weight: 700;
}

.hero-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
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
  margin-bottom: 12rpx;
}

.step {
  display: flex;
  align-items: flex-start;
  padding: 18rpx 0;
}

.step-no {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #00aeb5;
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  text-align: center;
  line-height: 44rpx;
  margin-right: 18rpx;
  flex-shrink: 0;
}

.step-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.step-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
}

.label {
  font-size: 26rpx;
  color: #8a9a98;
}

.value {
  font-size: 26rpx;
  color: #1b2b2a;
}

.cta {
  background: #14342f;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  text-align: center;
  padding: 26rpx 0;
  border-radius: 44rpx;
  margin-top: 8rpx;
}

.note {
  text-align: center;
  font-size: 22rpx;
  color: #8a9a98;
  margin-top: 18rpx;
}
</style>

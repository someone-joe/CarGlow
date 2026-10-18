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
      <!-- 手机号授权入口：未绑定时才出现。用微信原生 getPhoneNumber，不自己造表单 -->
      <button
        v-if="!phoneBound"
        class="bind-btn"
        open-type="getPhoneNumber"
        @getphonenumber="onGetPhoneNumber"
      >
        绑定手机号
      </button>
    </view>

    <!-- 储值余额 -->
    <view class="wallet-bar" @click="go('/pages/wallet/wallet')">
      <view class="wb-left">
        <text class="wb-label">储值余额</text>
        <text class="wb-value">¥{{ yuan(balance) }}</text>
      </view>
      <view class="wb-right">
        <text class="wb-recharge" @click.stop="go('/pages/recharge/recharge')">充值</text>
        <text class="arrow">›</text>
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
      <view class="list-item" @click="go('/pages/park-location/park-location')">
        <text>常用停车位置</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="go('/pages/settings/settings')">
        <text>设置</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="go('/pages/selfwash-orders/selfwash-orders')">
        <text>我的自助洗车</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="go('/pages/invoice/invoice')">
        <text>发票申请</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="go('/pages/invite/invite')">
        <text>邀请好友</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="go('/pages/aftersale/aftersale')">
        <text>售后与退款进度</text><text class="arrow">›</text>
      </view>
      <view class="list-item" @click="go('/pages/insurance/insurance')">
        <text>车损保障</text><text class="arrow">›</text>
      </view>
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
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import SiteBar from '@/components/SiteBar.vue'
import { fetchWallet } from '@/api/wallet'
import { ensureLogin, login } from '@/api/auth'

const phoneBound = ref(false)
const balance = ref(0)

function yuan(fen: number): string {
  return (fen / 100).toFixed(2)
}

onShow(() => {
  fetchWallet()
    .then((w) => {
      balance.value = (w.balance ?? 0) + (w.giftBalance ?? 0)
    })
    .catch(() => {})
  refreshPhoneBound()
})

/**
 * 是否已绑定手机号：读登录接口返回的 phoneBound。
 * 不单独查会员详情——登录态里已经带了，少一次请求。
 */
function refreshPhoneBound(): void {
  ensureLogin()
    .then(() => loginWithCurrentCode())
    .catch(() => {})
}

/** 重新走一次登录以刷新 phoneBound（静默，用户无感） */
function loginWithCurrentCode(): void {
  uni.login({
    provider: 'weixin',
    success: (res) => {
      if (!res.code) return
      login(res.code)
        .then((data) => {
          phoneBound.value = data.phoneBound === true
        })
        .catch(() => {})
    },
  })
}

/**
 * 手机号授权回调。用户点「允许」才会带 code 上来；拒绝则 detail.code 为空，什么都不做。
 *
 * 把拿到的 phoneCode 重新登录一次，由后端换手机号并落库——前端不碰手机号本身
 * （微信新版不允许前端直接拿到明文，只能拿 code）。
 */
function onGetPhoneNumber(e: { detail?: { code?: string } }): void {
  const phoneCode = e.detail?.code
  if (!phoneCode) {
    uni.showToast({ title: '已取消授权', icon: 'none' })
    return
  }
  uni.login({
    provider: 'weixin',
    success: (res) => {
      if (!res.code) return
      login(res.code, phoneCode)
        .then((data) => {
          phoneBound.value = data.phoneBound === true
          uni.showToast({
            title: data.phoneBound ? '绑定成功' : '绑定未生效，请重试',
            icon: 'none',
          })
        })
        .catch(() => {})
    },
  })
}

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

/* 手机号授权按钮：微信原生 open-type，样式要去掉按钮默认边框 */
.bind-btn {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #00aeb5;
  background: #eaf7f7;
  border: 1rpx solid #00aeb5;
  border-radius: 30rpx;
  padding: 10rpx 24rpx;
  line-height: 1.4;
  margin: 0;
}

.bind-btn::after {
  border: none;
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

.wallet-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 28rpx 30rpx;
  margin-bottom: 20rpx;
  color: #fff;
}

.wb-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}

.wb-value {
  display: block;
  margin-top: 6rpx;
  font-size: 40rpx;
  font-weight: 700;
}

.wb-right {
  display: flex;
  align-items: center;
}

.wb-recharge {
  background: #fff;
  color: #0e8f94;
  font-size: 24rpx;
  font-weight: 600;
  padding: 8rpx 26rpx;
  border-radius: 30rpx;
  margin-right: 16rpx;
}

.wallet-bar .arrow {
  color: rgba(255, 255, 255, 0.9);
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

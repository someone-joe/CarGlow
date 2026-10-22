<template>
  <view class="cg-page mine">
    <!-- 身份区：渐变底 + 头像 + 登录/身份入口 + 三项数据 -->
    <view class="hero">
      <!--
        顶部整块即入口：
        - 未绑手机号：整块是登录入口（微信原生 getPhoneNumber，不自造表单）
        - 已绑定：整块进「个人资料」页
      -->
      <button
        v-if="!phoneBound"
        class="hero-top"
        open-type="getPhoneNumber"
        @getphonenumber="onGetPhoneNumber"
      >
        <view class="avatar">
          <view class="cg-icon-person" />
        </view>
        <view class="hero-main">
          <text class="hero-name">登录/注册</text>
          <text class="hero-sub">欢迎使用 CarGlow</text>
        </view>
        <text class="hero-arrow">›</text>
      </button>
      <view v-else class="hero-top" @click="goProfile">
        <view class="avatar">
          <view class="cg-icon-person" />
        </view>
        <view class="hero-main">
          <text class="hero-name">微信用户</text>
          <text class="hero-sub">已绑定手机号</text>
        </view>
        <text class="hero-arrow">›</text>
      </view>

      <view class="hero-stats">
        <view class="stat" @click="go('/pages/coupons/coupons')">
          <view class="stat-num">
            <text class="stat-val">{{ couponCount }}</text>
            <text class="stat-unit">张</text>
          </view>
          <text class="stat-label">优惠券</text>
        </view>
        <view class="stat" @click="go('/pages/vehicle/vehicle')">
          <view class="stat-num">
            <text class="stat-val">{{ vehicleCount }}</text>
            <text class="stat-unit">辆</text>
          </view>
          <text class="stat-label">我的车辆</text>
        </view>
        <view class="stat" @click="go('/pages/wallet/wallet')">
          <view class="stat-num">
            <text class="stat-val">{{ yuan(balance) }}</text>
            <text class="stat-unit">元</text>
          </view>
          <text class="stat-label">余额</text>
        </view>
      </view>
    </view>

    <!-- 常用功能：iOS 风格分组列表（卡券/车辆/余额入口已在顶部数据行，此处不重复） -->
    <view class="cg-list">
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/park-location/park-location')">
        <text class="cg-list-label">常用停车位置</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/selfwash-orders/selfwash-orders')">
        <text class="cg-list-label">我的自助洗车</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/invoice/invoice')">
        <text class="cg-list-label">发票申请</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/invite/invite')">
        <text class="cg-list-label">邀请好友</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/aftersale/aftersale')">
        <text class="cg-list-label">售后与退款进度</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/insurance/insurance')">
        <text class="cg-list-label">车损保障</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/customer-service/customer-service')">
        <text class="cg-list-label">客服中心</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/settings/settings')">
        <text class="cg-list-label">设置</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="toast('服务条款')">
        <text class="cg-list-label">服务条款</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="toast('隐私政策')">
        <text class="cg-list-label">隐私政策</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="about">
        <text class="cg-list-label">关于 CarGlow</text>
        <text class="cg-chevron">›</text>
      </view>
    </view>

    <!-- 师傅端入口（入口 A） -->
    <view class="cg-list">
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/worker-login/worker-login')">
        <text class="cg-list-label">我是洗车师傅</text>
        <text class="cg-chevron">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchWallet } from '@/api/wallet'
import { fetchMyCoupons } from '@/api/promo'
import { fetchVehicles } from '@/api/catalog'
import { allowAutoLogin, ensureLogin, login } from '@/api/auth'

const phoneBound = ref(false)
const balance = ref(0)
const couponCount = ref(0)
const vehicleCount = ref(0)

function yuan(fen: number): string {
  return (fen / 100).toFixed(2)
}

onShow(() => {
  fetchWallet()
    .then((w) => {
      balance.value = (w.balance ?? 0) + (w.giftBalance ?? 0)
    })
    .catch(() => {})
  // 顶部数据行：券按「未使用」计，车辆按当前会员车辆数；失败只回落到 0，不打扰用户
  fetchMyCoupons('UNUSED')
    .then((list) => {
      couponCount.value = list?.length ?? 0
    })
    .catch(() => {})
  fetchVehicles()
    .then((list) => {
      vehicleCount.value = list?.length ?? 0
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
          if (phoneBound.value) allowAutoLogin() // 显式授权成功，恢复静默登录
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

/** 已登录：进个人资料页 */
function goProfile(): void {
  uni.navigateTo({ url: '/pages/profile/profile' })
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
/* tabBar 页：底部留白需避开原生 tabBar */
.mine {
  padding-bottom: calc(200rpx + env(safe-area-inset-bottom));
}

/* ---- 身份区：负 margin 撑满屏幕宽，顶部渐变到页面底色 ---- */
.hero {
  margin: 0 -40rpx;
  padding: 40rpx 40rpx 44rpx;
  background: linear-gradient(180deg, var(--cg-tint) 0%, var(--cg-bg) 100%);
}

.hero-top {
  display: flex;
  align-items: center;
  width: 100%;
}

/* 未登录时整块是 button：清掉原生外观，做成通栏可点的身份入口 */
button.hero-top {
  margin: 0;
  padding: 0;
  background: transparent;
  line-height: normal;
  text-align: left;
}

button.hero-top::after {
  border: none;
}

.avatar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: var(--cg-card);
  box-shadow: var(--cg-shadow-card);
}

.hero-main {
  flex: 1;
  margin-left: 28rpx;
}

.hero-name {
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
}

.hero-arrow {
  margin-left: 8rpx;
  font-size: var(--cg-font-body);
  color: var(--cg-text-sub);
}

.hero-sub {
  display: block;
  margin-top: 10rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 数据行：券 / 车辆 / 余额 ---- */
.hero-stats {
  display: flex;
  margin-top: 36rpx;
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  display: flex;
  align-items: baseline;
}

.stat-val {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  line-height: 1.1;
  color: var(--cg-title);
}

.stat-unit {
  margin-left: 4rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-title);
}

.stat-label {
  margin-top: 8rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}
</style>

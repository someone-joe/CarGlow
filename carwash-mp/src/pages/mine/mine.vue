<template>
  <view class="cg-page mine">
    <SiteBar />

    <!-- 个人资料卡：白底 + 柔和阴影 + 顶部极淡渐变；头像尺寸适中，不做巨大头像 -->
    <view class="cg-card cg-card-tint profile">
      <view class="avatar">
        <view class="cg-icon-person" />
      </view>
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
    <view class="cg-card wallet-bar" @click="go('/pages/wallet/wallet')">
      <view class="wb-left">
        <text class="wb-label">储值余额</text>
        <text class="wb-value">¥{{ yuan(balance) }}</text>
      </view>
      <view class="wb-right">
        <text class="wb-recharge" @click.stop="go('/pages/recharge/recharge')">充值</text>
        <text class="cg-chevron">›</text>
      </view>
    </view>

    <!-- 常用功能：iOS 风格分组列表 -->
    <view class="cg-list">
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/coupons/coupons')">
        <text class="cg-list-label">我的卡券</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="go('/pages/vehicle/vehicle')">
        <text class="cg-list-label">我的车辆</text>
        <text class="cg-chevron">›</text>
      </view>
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

/* ---- 个人资料卡 ---- */
.profile {
  display: flex;
  align-items: center;
  margin-top: 32rpx;
}

.avatar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: var(--cg-fill);
}

.profile-main {
  flex: 1;
  margin-left: 32rpx;
}

.nick {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
}

.sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.bind-btn {
  flex: none;
  height: 72rpx;
  margin: 0;
  padding: 0 32rpx;
  background: var(--cg-primary);
  color: #ffffff;
  font-size: var(--cg-font-note);
  line-height: 72rpx;
  border-radius: var(--cg-radius-md);
}

.bind-btn::after {
  border: none;
}

/* ---- 储值余额 ---- */
.wallet-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
}

.wb-label {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.wb-value {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.wb-right {
  display: flex;
  align-items: center;
}

.wb-recharge {
  color: var(--cg-primary);
  font-size: var(--cg-font-caption);
}
</style>

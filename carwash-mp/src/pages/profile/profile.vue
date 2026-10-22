<template>
  <view class="cg-page profile">
    <!-- 身份概览：头像 + 用户名 + 绑定状态 -->
    <view class="cg-card cg-card-tint profile-head">
      <view class="avatar">
        <view class="cg-icon-person" />
      </view>
      <view class="profile-main">
        <text class="profile-name">微信用户</text>
        <text class="profile-sub">{{ phoneBound ? '已绑定手机号' : '未绑定手机号' }}</text>
      </view>
    </view>

    <!-- 账号 -->
    <text class="cg-section-title">账号</text>
    <view class="cg-list">
      <view class="cg-list-row">
        <text class="cg-list-label">用户名</text>
        <text class="cg-list-value">微信用户</text>
      </view>
      <!-- 未绑定：整行是「去绑定」入口（走全局授权门控）；已绑定：仅展示状态，号码不落前端 -->
      <view v-if="!phoneBound" class="cg-list-row cg-tap" hover-class="cg-hover" @click="bindPhone">
        <text class="cg-list-label">关联手机</text>
        <text class="cg-list-value is-link">未绑定 · 去绑定</text>
        <text class="cg-chevron">›</text>
      </view>
      <view v-else class="cg-list-row">
        <text class="cg-list-label">关联手机</text>
        <text class="cg-list-value">已绑定</text>
      </view>
    </view>

    <!-- 协议与说明 -->
    <text class="cg-section-title">协议与说明</text>
    <view class="cg-list">
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="toast('服务条款')">
        <text class="cg-list-label">服务条款</text>
        <text class="cg-chevron">›</text>
      </view>
      <view class="cg-list-row cg-tap" hover-class="cg-hover" @click="toast('隐私政策')">
        <text class="cg-list-label">隐私政策</text>
        <text class="cg-chevron">›</text>
      </view>
    </view>

    <view class="logout cg-tap" hover-class="cg-hover" @click="onLogout">退出登录</view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed } from 'vue'
import { suppressAutoLogin } from '@/api/auth'
import { authGate, refreshPhoneBound, requirePhone } from '@/store/auth'
import { clearToken } from '@/utils/request'

/** 绑定态统一读授权门控单例，不在页面里维护第二份状态（与「我的」页授权浮层共用） */
const phoneBound = computed(() => authGate.phoneBound)

onShow(() => {
  refreshPhoneBound()
})

/** 未绑定时去绑定：走全局授权浮层（微信原生 getPhoneNumber，前端不碰手机号明文） */
function bindPhone(): void {
  requirePhone(() => {})
}

function toast(name: string): void {
  uni.showToast({ title: `${name}（后台可编辑富文本，V1.0 暂未接入）`, icon: 'none' })
}

/**
 * 退出登录：清本地 token + 抑制静默登录。
 *
 * <p>V1.0 契约没有登出接口（后端无 token 黑名单），若只清 token，
 * 下一次接口 10002 续登会立刻恢复会话，用户感知不到退出。抑制后，
 * 在用户再次显式授权（getPhoneNumber）前不会自动登录；请求层随之返回未登录。
 */
function onLogout(): void {
  uni.showModal({
    title: '退出登录',
    content: '退出后需重新授权手机号才能使用完整功能',
    success: (res) => {
      if (!res.confirm) return
      clearToken()
      suppressAutoLogin()
      authGate.phoneBound = false
      uni.showToast({ title: '已退出登录', icon: 'none' })
      uni.navigateBack()
    },
  })
}
</script>

<style scoped>
.profile {
  padding-top: 32rpx;
}

/* ---- 身份概览 ---- */
.profile-head {
  display: flex;
  align-items: center;
}

.avatar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: var(--cg-fill);
}

.profile-main {
  flex: 1;
  margin-left: 32rpx;
}

.profile-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
}

.profile-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.cg-list-value.is-link {
  color: var(--cg-primary);
}

/* ---- 退出登录 ---- */
.logout {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 64rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
  font-size: var(--cg-font-body);
  color: var(--cg-error);
}
</style>

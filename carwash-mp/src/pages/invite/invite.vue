<template>
  <view class="page">
    <!-- 我的邀请码 -->
    <view class="code-card">
      <text class="cc-label">我的邀请码</text>
      <text class="cc-code">{{ code || '生成中…' }}</text>
      <view class="cc-actions">
        <text class="cc-copy" @click="copyCode">复制邀请码</text>
        <text class="cc-tip">好友下单填你的码，你得 10 元券</text>
      </view>
    </view>

    <!-- 填写好友邀请码 -->
    <view class="panel">
      <text class="panel-title">填写好友邀请码</text>
      <input v-model="inputCode" class="input" placeholder="输入好友的邀请码（如 CGAB12XY）" maxlength="16" />
      <view class="bind-btn" :class="{ disabled: !inputCode.trim() || binding }" @click="doBind">
        {{ binding ? '绑定中…' : '绑定并领首单券' }}
      </view>
    </view>

    <!-- 我邀请的人 -->
    <view class="panel">
      <text class="panel-title">我邀请的人（{{ invitedCount }}）</text>
      <view v-if="loading" class="tip">加载中…</view>
      <view v-else-if="!list.length" class="tip">还没有邀请到好友，快把邀请码分享出去吧</view>
      <view v-for="i in list" :key="i.inviteId" class="inv">
        <text class="inv-id">会员 #{{ i.inviteeMemberId }}</text>
        <text class="inv-status" :class="{ done: i.status === 'REWARDED' }">{{ i.statusLabel }}</text>
      </view>
      <view v-if="rewardedCount" class="inv-sum">已发放奖励券 {{ rewardedCount }} 张</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { bindInvite, fetchInviteCode, fetchInviteList, type InviteVO } from '@/api/invite'

const loading = ref(true)
const binding = ref(false)
const code = ref('')
const inputCode = ref('')
const list = ref<InviteVO[]>([])
const invitedCount = ref(0)
const rewardedCount = ref(0)

function load(): void {
  loading.value = true
  fetchInviteCode()
    .then((c) => (code.value = c))
    .catch(() => {})
  fetchInviteList()
    .then((d) => {
      list.value = d.list || []
      invitedCount.value = d.invitedCount || 0
      rewardedCount.value = d.rewardedCount || 0
    })
    .catch(() => {})
    .finally(() => (loading.value = false))
}

function copyCode(): void {
  if (!code.value) return
  uni.setClipboardData({ data: code.value })
}

async function doBind(): Promise<void> {
  const c = inputCode.value.trim()
  if (!c || binding.value) return
  binding.value = true
  try {
    await bindInvite(c)
    uni.showToast({ title: '绑定成功，首单券已到账', icon: 'none' })
    inputCode.value = ''
    load()
  } catch {
    // request 层已提示
  } finally {
    binding.value = false
  }
}

onLoad(load)

onShareAppMessage(() => ({
  title: `你的好友送你一张洗车券，邀请码 ${code.value}`,
  path: '/pages/invite/invite',
}))
</script>

<style>
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.code-card {
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 36rpx 30rpx;
  margin-bottom: 20rpx;
  color: #fff;
  text-align: center;
}

.cc-label {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.cc-code {
  display: block;
  margin-top: 12rpx;
  font-size: 56rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
}

.cc-actions {
  margin-top: 20rpx;
}

.cc-copy {
  display: inline-block;
  background: #fff;
  color: #0e8f94;
  font-size: 26rpx;
  font-weight: 600;
  padding: 12rpx 40rpx;
  border-radius: 34rpx;
}

.cc-tip {
  display: block;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
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
  margin-bottom: 12rpx;
}

.input {
  background: #f2f7f7;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 27rpx;
  margin-bottom: 16rpx;
}

.bind-btn {
  background: #14342f;
  color: #fff;
  text-align: center;
  padding: 22rpx 0;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.bind-btn.disabled {
  background: #cfe0de;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 40rpx 0;
}

.inv {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.inv:last-child {
  border-bottom: none;
}

.inv-id {
  font-size: 27rpx;
  color: #1b2b2a;
}

.inv-status {
  font-size: 24rpx;
  color: #8a9a98;
}

.inv-status.done {
  color: #00aeb5;
}

.inv-sum {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #00aeb5;
}
</style>

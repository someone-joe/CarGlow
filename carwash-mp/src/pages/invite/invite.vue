<template>
  <view class="cg-page invite">
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
      <view v-if="loading" class="cg-empty">加载中…</view>
      <view v-else-if="!list.length" class="cg-empty">还没有邀请到好友，快把邀请码分享出去吧</view>
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
/* ---- 邀请码卡 ---- */
.code-card {
  margin-top: 32rpx;
  padding: 48rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
  text-align: center;
}

.cc-label {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.cc-code {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
  letter-spacing: 8rpx;
}

.cc-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 40rpx;
}

.cc-copy {
  min-height: 96rpx;
  padding: 0 40rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: #ffffff;
  line-height: 96rpx;
}

.cc-tip {
  flex: 1;
  margin-left: 32rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  text-align: left;
}

/* ---- 面板 ---- */
.panel {
  margin-top: 32rpx;
  padding: 8rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  padding: 32rpx 0 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.input {
  box-sizing: border-box;
  width: 100%;
  margin: 16rpx 0 32rpx;
  padding: 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.bind-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-bottom: 40rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.bind-btn.disabled {
  opacity: 0.4;
}

/* ---- 邀请列表 ---- */
.inv {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.inv-id {
  font-size: var(--cg-font-body);
  color: var(--cg-text);
}

.inv-status {
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.inv-status.done {
  color: var(--cg-success);
}

.inv-sum {
  padding: 32rpx 0;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  border-top: 1rpx solid var(--cg-line);
}
</style>

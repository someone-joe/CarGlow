<template>
  <view class="cg-page worker-login">
    <view class="title">师傅登录</view>
    <view class="sub">工号登录，与顾客微信登录相互隔离</view>

    <input class="input" v-model="workerNo" placeholder="工号" />
    <input class="input" v-model="password" placeholder="密码" password="true" />

    <button class="btn" @click="submit">登录</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { workerLogin } from '@/api/worker'
import { saveWorkerLogin } from '@/store/worker'

const workerNo = ref('')
const password = ref('')

async function submit(): Promise<void> {
  if (!workerNo.value || !password.value) {
    uni.showToast({ title: '请输入工号与密码', icon: 'none' })
    return
  }
  try {
    const vo = await workerLogin(workerNo.value, password.value)
    saveWorkerLogin(vo)
    uni.showToast({ title: `欢迎，${vo.name ?? ''}`, icon: 'none' })
    uni.redirectTo({ url: '/pages/worker-tasks/worker-tasks' })
  } catch (e) {
    // 错误码由后端给出（D4001 工号不存在 / D4002 密码错误 / D4003 已禁用）
    const msg = (e as { msg?: string }).msg || '登录失败'
    uni.showToast({ title: msg, icon: 'none' })
  }
}
</script>

<style>
.worker-login {
  padding-top: 160rpx;
}

.title {
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.sub {
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.input {
  box-sizing: border-box;
  width: 100%;
  margin-top: 32rpx;
  padding: 24rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-md);
  box-shadow: var(--cg-shadow-card);
  font-size: var(--cg-font-body);
  color: var(--cg-text);
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 48rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.btn::after {
  border: none;
}
</style>

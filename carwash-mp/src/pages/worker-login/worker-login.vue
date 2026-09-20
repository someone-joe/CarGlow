<template>
  <view class="page">
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
.page {
  padding: 60rpx 40rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.title {
  font-size: 44rpx;
  font-weight: 700;
  color: #222;
}

.sub {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #999;
}

.input {
  margin-top: 40rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 28rpx;
  font-size: 30rpx;
}

.btn {
  margin-top: 48rpx;
  background: #1a73e8;
  color: #fff;
  border-radius: 12rpx;
  font-size: 32rpx;
}
</style>

<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="code">
      <view class="hero">
        <text class="hero-title">{{ mode === 'take' ? '取钥匙开箱码' : '存钥匙开箱码' }}</text>
        <text class="hero-sub">{{ cabinetText }}</text>
      </view>

      <view class="code-card">
        <text v-if="code.qrcodeUrl" class="qr-tip">请到柜机扫码，或输入下方开箱码</text>
        <image v-if="code.qrcodeUrl" class="qr" :src="code.qrcodeUrl" mode="aspectFit" />
        <view class="code-box">
          <text class="code-label">开箱码</text>
          <text class="code-value">{{ code.code }}</text>
        </view>
        <text class="expire">10 分钟内有效，仅可使用一次（剩余 {{ countdown }}）</text>
        <text class="copy" @click="copy">复制开箱码</text>
      </view>

      <view class="warn">
        <text>柜门 60 秒未关闭自动关合并提示；开箱失败可联系客服远程协助或应急机械钥匙。</text>
      </view>
    </block>

    <view v-else class="tip">暂无可用的开箱码</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchOpenCode, type OpenCodeVO } from '@/api/order'

const orderNo = ref('')
const mode = ref<'deposit' | 'take'>('deposit')
const code = ref<OpenCodeVO | null>(null)
const loading = ref(true)
const countdown = ref('10:00')
let expireAt = 0
let timer: ReturnType<typeof setInterval> | null = null

const cabinetText = ref('')

function tick(): void {
  if (!expireAt) return
  const left = Math.max(0, expireAt - Date.now())
  const m = Math.floor(left / 60000)
  const s = Math.floor((left % 60000) / 1000)
  countdown.value = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  if (left <= 0 && timer) {
    clearInterval(timer)
    timer = null
    countdown.value = '00:00'
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const data = await fetchOpenCode(orderNo.value)
    code.value = data
    cabinetText.value = [data.cabinetName, data.slotNo ? data.slotNo + ' 格口' : ''].filter(Boolean).join(' · ')
    expireAt = data.expireAt ?? 0
    tick()
    if (expireAt) timer = setInterval(tick, 1000)
  } catch {
    code.value = null
  } finally {
    loading.value = false
  }
}

function copy(): void {
  if (!code.value?.code) return
  uni.setClipboardData({ data: code.value.code })
}

onLoad((opts) => {
  const o = opts as { orderNo?: string; mode?: string }
  orderNo.value = o.orderNo ?? ''
  mode.value = o.mode === 'take' ? 'take' : 'deposit'
  if (orderNo.value) load()
  else loading.value = false
})

onUnload(() => {
  if (timer) clearInterval(timer)
})
</script>

<style>
.page {
  padding: 32rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #999;
  padding: 80rpx 0;
}

.hero {
  text-align: center;
  padding: 24rpx 0 40rpx;
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
  color: #222;
}

.hero-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #888;
}

.code-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx;
  text-align: center;
}

.qr-tip {
  display: block;
  font-size: 26rpx;
  color: #888;
  margin-bottom: 20rpx;
}

.qr {
  width: 360rpx;
  height: 360rpx;
  margin: 0 auto 32rpx;
  background: #f2f2f2;
}

.code-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.code-label {
  font-size: 26rpx;
  color: #999;
}

.code-value {
  font-size: 72rpx;
  font-weight: 700;
  color: #1a73e8;
  letter-spacing: 12rpx;
  margin-top: 8rpx;
}

.expire {
  display: block;
  margin-top: 24rpx;
  font-size: 24rpx;
  color: #999;
}

.copy {
  display: inline-block;
  margin-top: 24rpx;
  font-size: 28rpx;
  color: #1a73e8;
  border: 2rpx solid #1a73e8;
  border-radius: 12rpx;
  padding: 12rpx 48rpx;
}

.warn {
  margin-top: 32rpx;
  background: #fff7e6;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 24rpx;
  color: #9a6b00;
  line-height: 1.7;
}
</style>

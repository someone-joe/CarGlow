<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="code">
      <!-- 状态头 -->
      <view class="status-bar">
        <text class="status">{{ mode === 'take' ? '取钥匙开箱码' : '存钥匙开箱码' }}</text>
        <text class="cabinet">{{ cabinetText }}</text>
      </view>

      <view class="code-card">
        <text v-if="code.qrcodeUrl" class="qr-tip">请到柜机扫码，或输入下方开箱码</text>
        <image v-if="code.qrcodeUrl" class="qr" :src="code.qrcodeUrl" mode="aspectFit" />
        <view class="code-box">
          <text class="code-label">开箱码</text>
          <text class="code-value">{{ code.code }}</text>
        </view>
        <view class="expire-row">
          <text class="expire">10 分钟内有效，仅可使用一次</text>
          <text class="countdown">剩余 {{ countdown }}</text>
        </view>
        <view class="copy" @click="copy">复制开箱码</view>
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
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 80rpx 0;
}

.status-bar {
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 34rpx 30rpx;
  margin-bottom: 20rpx;
}

.status {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
}

.cabinet {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  color: rgba(255, 255, 255, 0.85);
}

.code-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
  text-align: center;
}

.qr-tip {
  display: block;
  font-size: 26rpx;
  color: #8a9a98;
  margin-bottom: 20rpx;
}

.qr {
  width: 340rpx;
  height: 340rpx;
  margin: 0 auto 32rpx;
  background: #eef3f2;
}

.code-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.code-label {
  font-size: 25rpx;
  color: #8a9a98;
}

.code-value {
  font-size: 72rpx;
  font-weight: 700;
  color: #14342f;
  letter-spacing: 10rpx;
  margin-top: 8rpx;
}

.expire-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  margin-top: 24rpx;
}

.expire {
  font-size: 24rpx;
  color: #8a9a98;
}

.countdown {
  font-size: 24rpx;
  color: #ff5b4a;
  font-weight: 600;
}

.copy {
  display: inline-block;
  margin-top: 28rpx;
  font-size: 28rpx;
  color: #00aeb5;
  border: 2rpx solid #00aeb5;
  border-radius: 36rpx;
  padding: 14rpx 52rpx;
}

.warn {
  margin-top: 24rpx;
  background: #fff7e6;
  border-radius: 20rpx;
  padding: 26rpx;
  font-size: 24rpx;
  color: #9a6b00;
  line-height: 1.7;
}
</style>

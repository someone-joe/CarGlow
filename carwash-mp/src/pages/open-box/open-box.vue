<template>
  <view class="cg-page open-box">
    <view v-if="loading" class="cg-empty">加载中…</view>

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
        <!-- 取钥匙模式：客户开柜取走钥匙后手动确认（MVP 无柜机回调），后端释放格口并推进状态（bug092410） -->
        <view v-if="mode === 'take'" class="taken-btn" @click="confirmTaken">我已取出钥匙</view>
      </view>

      <view class="warn">
        <text>柜门 60 秒未关闭自动关合并提示；开箱失败可联系客服远程协助或应急机械钥匙。</text>
      </view>
    </block>

    <view v-else class="cg-empty">暂不可用的开箱码</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchOpenCode, takeKeyBack, type OpenCodeVO } from '@/api/order'

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

/** 客户确认已取走钥匙：后端释放格口 + RETURNED → WAIT_REVIEW */
async function confirmTaken(): Promise<void> {
  try {
    await takeKeyBack(orderNo.value)
    uni.showToast({ title: '已确认取回钥匙', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch {
    // request 层已提示（如状态不符）
  }
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
.open-box {
  padding-top: 32rpx;
}

/* ---- 状态头 ---- */
.status-bar {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.status {
  display: block;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.cabinet {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

/* ---- 开箱码卡 ---- */
.code-card {
  margin-top: 32rpx;
  padding: 48rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
  text-align: center;
}

.qr-tip {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.qr {
  display: block;
  width: 320rpx;
  height: 320rpx;
  margin: 32rpx auto 0;
}

.code-box {
  margin-top: 40rpx;
}

.code-label {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.code-value {
  display: block;
  margin-top: 16rpx;
  font-size: 72rpx;
  font-weight: 600;
  color: var(--cg-title);
  letter-spacing: 16rpx;
}

.expire-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 40rpx;
}

.expire {
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.countdown {
  font-size: var(--cg-font-note);
  color: var(--cg-error);
}

.copy {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 40rpx;
  padding: 0 48rpx;
  background: var(--cg-card);
  border: 1rpx solid var(--cg-border);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: var(--cg-title);
}

.taken-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 32rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.warn {
  margin-top: 32rpx;
  padding: 32rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  line-height: 1.6;
}
</style>

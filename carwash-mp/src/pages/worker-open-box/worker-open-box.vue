<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="code">
      <view class="status-bar">
        <text class="status">取钥匙开箱码</text>
        <text class="cabinet">{{ cabinetText }}</text>
      </view>

      <view class="card">
        <view class="code-box">
          <text class="code-label">开箱码</text>
          <text class="code-value">{{ code.code }}</text>
        </view>
        <view class="expire-row">
          <text class="expire">10 分钟内有效，仅可使用一次</text>
          <text class="countdown">剩余 {{ countdown }}</text>
        </view>
        <view class="link" @click="copy">复制开箱码</view>

        <!-- 扫码为主：扫柜机格口二维码，服务端校验与本单格口一致才放行（一物一码，防开错柜） -->
        <view :class="['scan-btn', authorized && 'scan-btn-done']" @click="scan">
          {{ authorized ? '格口已校验，请取走钥匙' : '扫柜机格口二维码开箱' }}
        </view>
        <!-- 码兜底：扫不出来时到柜机键盘输码（PRD 6.2 三级兜底里的备用取件码） -->
        <text class="scan-tip">扫不出来时，可直接在柜机键盘输入上方开箱码</text>

        <view class="taken-btn" @click="confirmTaken">我已取出钥匙</view>
      </view>

      <view class="warn">
        <text>柜门 60 秒未关闭会自动关合；开箱失败联系站长或客服远程协助。</text>
      </view>
    </block>

    <view v-else class="tip">暂无可用的开箱码</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchWorkerOpenCode, takeKey, type OpenCodeVO } from '@/api/worker'

const orderNo = ref('')
const code = ref<OpenCodeVO | null>(null)
const cabinetText = ref('')
const loading = ref(true)
const countdown = ref('10:00')
/** 是否已通过扫码校验格口（未校验也允许走码兜底，仅影响按钮文案） */
const authorized = ref(false)
let expireAt = 0
let timer: ReturnType<typeof setInterval> | null = null

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
    const data = await fetchWorkerOpenCode(orderNo.value)
    code.value = data
    cabinetText.value = [data.cabinetName, data.slotNo ? data.slotNo + ' 格口' : ''].filter(Boolean).join(' · ')
    expireAt = data.expireAt ?? 0
    tick()
    if (expireAt && !timer) timer = setInterval(tick, 1000)
  } catch {
    code.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 柜机二维码内容格式待硬件确定，MVP 约定为格口号（A02）或「柜号-格口号」（CAB301-A02）。
 * 真机接入时只改这里，后端校验逻辑不用动。
 */
function parseSlotNo(raw: string): string {
  const text = (raw || '').trim()
  const segs = text.split(/[-_#,]/)
  return segs.length > 1 ? segs[segs.length - 1].trim() : text
}

async function scan(): Promise<void> {
  const res = await new Promise<UniApp.ScanCodeSuccessRes>((resolve, reject) => {
    uni.scanCode({ onlyFromCamera: false, scanType: ['qrCode'], success: resolve, fail: reject })
  }).catch(() => null)
  if (!res?.result) return
  try {
    // 格口匹配由服务端判定：不匹配的请求会被 B2005 拒绝，request 层已把原因弹出来
    const data = await fetchWorkerOpenCode(orderNo.value, parseSlotNo(res.result))
    code.value = data
    authorized.value = true
  } catch {
    authorized.value = false
  }
}

function copy(): void {
  if (!code.value?.code) return
  uni.setClipboardData({ data: code.value.code })
}

/** 确认已取出钥匙：后端校验开箱码后释放格口 + 推进 KEY_IN → PICKING */
async function confirmTaken(): Promise<void> {
  if (!code.value?.code) return
  try {
    await takeKey(orderNo.value, code.value.code)
    uni.showToast({ title: '已取钥匙', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch {
    // request 层已提示（如开箱码失效 B2004、状态不符 B1002）
  }
}

onLoad((opts) => {
  const o = opts as { orderNo?: string }
  orderNo.value = o.orderNo ?? ''
  if (orderNo.value) load()
  else loading.value = false
})

onUnload(() => {
  if (timer) clearInterval(timer)
})
</script>

<style>
.page {
  padding: 24rpx 24rpx 60rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}
.tip {
  text-align: center;
  color: #aaa;
  font-size: 26rpx;
  padding: 80rpx 0;
}
.status-bar {
  background: linear-gradient(135deg, #1a73e8, #1456b0);
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
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
  text-align: center;
}
.code-label {
  font-size: 25rpx;
  color: #888;
}
.code-value {
  display: block;
  font-size: 72rpx;
  font-weight: 700;
  color: #16324a;
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
  color: #888;
}
.countdown {
  font-size: 24rpx;
  color: #ff5b4a;
  font-weight: 600;
}
.link {
  display: inline-block;
  margin-top: 28rpx;
  font-size: 28rpx;
  color: #1a73e8;
  border: 2rpx solid #1a73e8;
  border-radius: 36rpx;
  padding: 14rpx 52rpx;
}
.scan-btn {
  display: block;
  margin: 32rpx auto 0;
  background: #1a73e8;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  width: fit-content;
  padding: 22rpx 60rpx;
  border-radius: 44rpx;
}
.scan-btn-done {
  background: #18a058;
}
.scan-tip {
  display: block;
  margin-top: 16rpx;
  font-size: 23rpx;
  color: #999;
}
.taken-btn {
  display: block;
  margin: 32rpx auto 0;
  background: #16324a;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  width: fit-content;
  padding: 22rpx 70rpx;
  border-radius: 44rpx;
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

<template>
  <view class="page">
    <view class="intro">到店自助洗车 · 选工位 → 下单 → 到店激活即用</view>

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 工位选择 -->
      <view class="panel">
        <view class="panel-title">选择工位</view>
        <view v-if="!workstations.length" class="empty">当前站点暂无工位</view>
        <view
          v-for="w in workstations"
          :key="w.workstationId"
          class="ws"
          :class="{ active: workstationId === w.workstationId, disabled: w.status === 'DISABLED' }"
          @click="pickWs(w)"
        >
          <view class="ws-main">
            <text class="ws-code">{{ w.code }}</text>
            <text v-if="w.name" class="ws-name">{{ w.name }}</text>
          </view>
          <view class="ws-right">
            <text class="ws-status" :class="statusClass(w.status)">{{ statusText(w.status) }}</text>
            <text v-if="w.status === 'SERVING'" class="ws-sub">剩余 {{ fmtRemain(w.remainingSeconds) }} · 排队 {{ w.queueCount }}</text>
            <text v-else-if="w.status === 'IDLE' && w.queueCount" class="ws-sub">排队 {{ w.queueCount }}</text>
          </view>
        </view>
      </view>

      <!-- 服务选择 -->
      <view class="panel">
        <view class="panel-title">选择服务</view>
        <view v-if="!services.length" class="empty">暂无可预约的服务</view>
        <view
          v-for="s in services"
          :key="s.serviceId"
          class="svc"
          :class="{ active: serviceId === s.serviceId, disabled: s.soldOut }"
          @click="serviceId = s.serviceId"
        >
          <view class="svc-main">
            <text class="svc-name">{{ s.name }}</text>
            <text class="svc-sub">约 {{ s.workMinutes || '—' }} 分钟</text>
          </view>
          <text class="svc-price">¥{{ fen2yuan(s.displayPrice) }}</text>
        </view>
      </view>
    </block>

    <view class="submit" :class="{ disabled: !canSubmit }" @click="submit">
      {{ submitting ? '下单中…' : '确认下单' }}
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchServices, type ServiceVO } from '@/api/catalog'
import { fetchSiteCurrent } from '@/api/home'
import { createSelfwashOrder, fetchWorkstations, type WorkstationVO } from '@/api/selfwash'

const loading = ref(true)
const submitting = ref(false)
const workstations = ref<WorkstationVO[]>([])
const services = ref<ServiceVO[]>([])
const workstationId = ref<number | undefined>(undefined)
const serviceId = ref<number | undefined>(undefined)

const canSubmit = computed(() => !!workstationId.value && !!serviceId.value && !submitting.value)

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function statusText(s: string): string {
  return s === 'SERVING' ? '服务中' : s === 'DISABLED' ? '停用' : '空闲'
}

function statusClass(s: string): string {
  return s === 'SERVING' ? 'busy' : s === 'DISABLED' ? 'off' : 'idle'
}

function fmtRemain(sec?: number): string {
  const s = sec ?? 0
  const m = Math.floor(s / 60)
  const ss = s % 60
  return `${String(m).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
}

function pickWs(w: WorkstationVO): void {
  if (w.status === 'DISABLED') return
  workstationId.value = w.workstationId
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const site = await fetchSiteCurrent().catch(() => null)
    const siteId = site?.siteId
    const [ws, svc] = await Promise.all([
      siteId ? fetchWorkstations(siteId) : Promise.resolve([]),
      fetchServices(),
    ])
    workstations.value = ws || []
    services.value = svc || []
  } catch {
    workstations.value = []
    services.value = []
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const orderNo = await createSelfwashOrder({
      workstationId: workstationId.value as number,
      serviceId: serviceId.value as number,
    })
    uni.redirectTo({ url: `/pages/selfwash-pay/selfwash-pay?orderNo=${orderNo}` })
  } catch {
    // request 层已提示
  } finally {
    submitting.value = false
  }
}

onShow(load)
</script>

<style>
.page {
  padding: 24rpx 24rpx 160rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.intro {
  font-size: 24rpx;
  color: #8a9a98;
  padding: 8rpx 4rpx 20rpx;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.panel-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 12rpx;
}

.empty {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 40rpx 0;
}

.ws {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid #eaf1f0;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.ws.active {
  border-color: #00aeb5;
  background: #f3fbfb;
}

.ws.disabled {
  opacity: 0.5;
}

.ws-code {
  font-size: 34rpx;
  font-weight: 700;
  color: #1b2b2a;
}

.ws-name {
  display: block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.ws-right {
  text-align: right;
}

.ws-status {
  font-size: 26rpx;
  font-weight: 600;
}

.ws-status.idle {
  color: #00aeb5;
}

.ws-status.busy {
  color: #ff8a3d;
}

.ws-status.off {
  color: #b6c2c0;
}

.ws-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8a9a98;
}

.svc {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid #eaf1f0;
  border-radius: 16rpx;
  padding: 22rpx 24rpx;
  margin-bottom: 14rpx;
}

.svc.active {
  border-color: #00aeb5;
  background: #f3fbfb;
}

.svc.disabled {
  opacity: 0.5;
}

.svc-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.svc-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #8a9a98;
}

.svc-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.submit {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 40rpx;
  background: #14342f;
  color: #fff;
  text-align: center;
  padding: 28rpx 0;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.submit.disabled {
  background: #cfe0de;
}
</style>

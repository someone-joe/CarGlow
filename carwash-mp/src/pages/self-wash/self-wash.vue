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
            <!-- 有人占用：服务中则突出排队提示，点击弹排队进度 -->
            <view v-if="w.status === 'SERVING'" class="queue-line" @click="openQueue(w)">
              <text class="queue-clock">🕓</text>
              <text class="queue-text">前面有<text class="queue-num">{{ aheadCount(w) }}人</text> 约等待<text class="queue-num">{{ w.estimatedWaitMinutes || 0 }}分钟</text></text>
              <text class="queue-link">进度 ›</text>
            </view>
            <text v-else-if="w.queueCount" class="ws-sub">前方排队 {{ w.queueCount }} 人</text>
          </view>
        </view>
      </view>

      <!-- 计时计价说明：自助按时间收费，价格来自后台配置 -->
      <view v-if="pricing" class="panel pricing">
        <view class="panel-title">计时收费</view>
        <view class="pricing-row">
          <text class="pricing-main">¥{{ fen2yuan(pricing.baseAmount) }} / {{ pricing.baseMinutes }} 分钟</text>
          <text class="pricing-sub">超时 ¥{{ fen2yuan(pricing.overtimeAmount) }} / {{ pricing.overtimeMinutes }} 分钟（不足一档按一档）</text>
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
            <text class="svc-sub">按时长计费，不限项目</text>
          </view>
          <text v-if="s.scene === 'SELFWASH'" class="svc-price">计时</text>
          <text v-else class="svc-price">¥{{ fen2yuan(s.displayPrice) }}</text>
        </view>

        <view class="coupon-row" @click="pickCoupon">
          <text class="coupon-label">优惠券</text>
          <text class="coupon-value" :class="{ placeholder: !couponUserId }">{{ couponText }} ›</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel-title">选择车辆</view>
        <VehiclePicker :vehicles="vehicles" v-model="vehicleId" @navigate="openSelect" @manage="onVehicleManage" />
      </view>
    </block>

    <QueueProgressPopup :show="queueWs != null" :ws="queueWs ?? undefined" @close="queueWs = undefined" />

    <view class="submit" :class="{ disabled: !canSubmit }" @click="submit">
      {{ submitting ? '下单中…' : '确认下单' }}
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchServices, fetchVehicles, type ServiceVO, type VehicleVO } from '@/api/catalog'
import { fetchSiteCurrent } from '@/api/home'
import { fetchMyCoupons, type CouponUserVO } from '@/api/promo'
import { createSelfwashOrder, fetchSelfwashPricing, fetchWorkstations, type SelfwashPricingVO, type WorkstationVO } from '@/api/selfwash'
import VehiclePicker from '@/components/VehiclePicker.vue'
import QueueProgressPopup from '@/components/QueueProgressPopup.vue'
import { consumePickedVehicle } from '@/store/vehicleSelect'

const loading = ref(true)
const submitting = ref(false)
const workstations = ref<WorkstationVO[]>([])
const services = ref<ServiceVO[]>([])
/** 计时计价参数（后台可配）：自助按时间收费 */
const pricing = ref<SelfwashPricingVO | null>(null)
const workstationId = ref<number | undefined>(undefined)
const serviceId = ref<number | undefined>(undefined)
const coupons = ref<CouponUserVO[]>([])
const couponUserId = ref<number | undefined>(undefined)
const couponText = ref('未使用优惠券')
const vehicles = ref<VehicleVO[]>([])
const vehicleId = ref<number | undefined>(undefined)
/** 排队进度弹窗对应的工位；有值即弹窗打开 */
const queueWs = ref<WorkstationVO | undefined>(undefined)

const canSubmit = computed(
  () => !!workstationId.value && !!serviceId.value && !!vehicleId.value && !submitting.value,
)

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function statusText(s: string): string {
  return s === 'SERVING' ? '服务中' : s === 'DISABLED' ? '停用' : '空闲'
}

function statusClass(s: string): string {
  return s === 'SERVING' ? 'busy' : s === 'DISABLED' ? 'off' : 'idle'
}

/** 排队人数口径：服务中 1 单 + 排队中单数（queueCount 仅统计排队中） */
function aheadCount(w: WorkstationVO): number {
  return (w.status === 'SERVING' ? 1 : 0) + (w.queueCount ?? 0)
}

function openQueue(w: WorkstationVO): void {
  queueWs.value = w
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
    const [ws, svc, vh] = await Promise.all([
      siteId ? fetchWorkstations(siteId) : Promise.resolve([]),
      fetchServices('SELFWASH'),
      fetchVehicles(),
    ])
    workstations.value = ws || []
    services.value = svc || []
    vehicles.value = vh || []
    if (vehicleId.value == null) vehicleId.value = vehicles.value[0]?.vehicleId
    // 计时计价参数：单独拉，失败不阻塞工位/服务展示
    fetchSelfwashPricing()
      .then((p) => (pricing.value = p))
      .catch(() => {})
    fetchMyCoupons('UNUSED')
      .then((c) => (coupons.value = c || []))
      .catch(() => {})
  } catch {
    workstations.value = []
    services.value = []
  } finally {
    loading.value = false
  }
}

function pickCoupon(): void {
  if (!coupons.value.length) {
    uni.showToast({ title: '暂无可用优惠券', icon: 'none' })
    return
  }
  const items = ['不使用优惠券', ...coupons.value.map((c) => `${c.name || '优惠券'} 减¥${fen2yuan(c.discountAmount)}`)]
  uni.showActionSheet({
    itemList: items,
    success: (res) => {
      if (res.tapIndex === 0) {
        couponUserId.value = undefined
        couponText.value = '未使用优惠券'
      } else {
        const c = coupons.value[res.tapIndex - 1]
        couponUserId.value = c.couponUserId
        couponText.value = `${c.name || '优惠券'} -¥${fen2yuan(c.discountAmount)}`
      }
    },
  })
}

async function submit(): Promise<void> {
  if (!canSubmit.value) return
  const v = vehicles.value.find((x) => x.vehicleId === vehicleId.value)
  if (!v) {
    uni.showToast({ title: '请先选择车辆', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const orderNo = await createSelfwashOrder({
      workstationId: workstationId.value as number,
      serviceId: serviceId.value as number,
      couponUserId: couponUserId.value,
      plateNo: v.plateNo || '',
      vehicleType: v.vehicleType || 'OTHER',
    })
    uni.redirectTo({ url: `/pages/selfwash-pay/selfwash-pay?orderNo=${orderNo}` })
  } catch {
    // request 层已提示
  } finally {
    submitting.value = false
  }
}

function openSelect(): void {
  uni.navigateTo({ url: `/pages/vehicle/select?source=selfwash&selected=${vehicleId.value ?? ''}` })
}

function onVehicleManage(): void {
  uni.navigateTo({ url: '/pages/vehicle/vehicle' })
}

onShow(async () => {
  const picked = consumePickedVehicle('selfwash')
  await load()
  if (picked != null) vehicleId.value = picked
})
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

.pricing {
  background: linear-gradient(135deg, #e8f6f6, #f2fbfb);
}

.pricing-row {
  display: flex;
  flex-direction: column;
}

.pricing-main {
  font-size: 34rpx;
  font-weight: 700;
  color: #0e8f94;
}

.pricing-sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6d8580;
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

/* ---- 服务中工位的排队提示行（可点开进度弹窗） ---- */
.queue-line {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 8rpx;
  background: #fff4ec;
  border-radius: 12rpx;
  padding: 10rpx 16rpx;
}

.queue-clock {
  font-size: 24rpx;
}

.queue-text {
  font-size: 24rpx;
  color: #6b5a4d;
}

.queue-num {
  color: #ff5b4a;
  font-weight: 600;
  margin: 0 4rpx;
}

.queue-link {
  font-size: 24rpx;
  color: #ff8a3d;
  font-weight: 600;
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

.coupon-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 20rpx;
  border-top: 2rpx solid #f2f7f7;
}

.coupon-label {
  font-size: 27rpx;
  color: #1b2b2a;
}

.coupon-value {
  font-size: 26rpx;
  color: #ff5b4a;
}

.coupon-value.placeholder {
  color: #8a9a98;
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

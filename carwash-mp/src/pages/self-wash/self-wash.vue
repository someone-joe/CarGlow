<template>
  <view class="cg-page self-wash">
    <view class="intro">到店自助洗车 · 选工位 → 下单 → 到店激活即用</view>

    <view v-if="loading" class="cg-empty">加载中…</view>

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
              <view class="queue-clock" />
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

    <view class="submit" :class="{ disabled: !canSubmit }" @click="onSubmitClick">
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
import { requirePhone } from '@/store/auth'

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

/** 下单入口：先过手机号授权门控——已绑直接提交，未绑弹授权、登后自动继续 */
function onSubmitClick(): void {
  requirePhone(() => submit())
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
/* 底部提交条占位 */
.self-wash {
  padding-bottom: 220rpx;
}

/* ---- 引导语 ---- */
.intro {
  margin: 32rpx 8rpx 24rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

/* ---- 面板 ---- */
.panel {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.empty {
  padding: 32rpx 0;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-aux);
}

/* ---- 工位卡：选中 = 2px 蓝边 + 6% 淡蓝底 + 提升阴影 ---- */
.ws {
  box-sizing: border-box;
  margin-top: 24rpx;
  padding: 24rpx;
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.ws.active {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
  box-shadow: var(--cg-shadow-raised);
}

.ws.disabled {
  opacity: 0.55;
}

.ws-main {
  display: flex;
  align-items: baseline;
}

.ws-code {
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
}

.ws-name {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.ws-right {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}

.ws-status {
  padding: 8rpx 24rpx;
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
}

.ws-status.idle {
  background: rgba(52, 199, 89, 0.12);
  color: var(--cg-success);
}

.ws-status.busy {
  background: rgba(255, 59, 48, 0.10);
  color: var(--cg-error);
}

.ws-status.off {
  background: rgba(134, 134, 139, 0.14);
  color: var(--cg-text-sub);
}

.ws-sub {
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 排队提示 ---- */
.queue-line {
  display: flex;
  align-items: center;
}

.queue-clock {
  flex: none;
  width: 20rpx;
  height: 20rpx;
  margin-right: 8rpx;
  border: 2rpx solid var(--cg-warning);
  border-radius: 50%;
}

.queue-text {
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.queue-num {
  color: var(--cg-warning);
}

.queue-link {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
}

/* ---- 计价说明 ---- */
.pricing-row {
  padding: 16rpx 0;
}

.pricing-main {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.pricing-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 服务选择 ---- */
.svc {
  box-sizing: border-box;
  margin-top: 24rpx;
  padding: 24rpx;
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.svc.active {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
  box-shadow: var(--cg-shadow-raised);
}

.svc.disabled {
  opacity: 0.55;
}

.svc-main {
  flex: 1;
}

.svc-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.svc-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.svc-price {
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 优惠券行 ---- */
.coupon-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 96rpx;
  margin-top: 32rpx;
}

.coupon-label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.coupon-value {
  font-size: var(--cg-font-caption);
  color: var(--cg-primary);
}

.coupon-value.placeholder {
  color: var(--cg-text-aux);
}

/* ---- 底部提交 ---- */
.submit {
  position: fixed;
  left: 40rpx;
  right: 40rpx;
  bottom: calc(32rpx + env(safe-area-inset-bottom));
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.submit.disabled {
  opacity: 0.4;
}
</style>

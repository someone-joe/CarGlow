<template>
  <view class="page">
    <view class="hero">
      <text class="title">夜间代客洗车</text>
      <text class="subtitle">今晚下单，明早干净上路</text>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 服务项：价格与时长全部由后端返回，前端不写死任何价格 -->
      <view class="section">
        <text class="section-title">选择服务</text>
        <view v-for="item in services" :key="item.serviceId" class="option"
              :class="{ active: serviceId === item.serviceId }" @click="serviceId = item.serviceId">
          <view class="option-main">
            <text class="option-name">{{ item.name }}</text>
            <text class="option-sub">约 {{ item.workMinutes }} 分钟 · 取送费 ¥{{ fen2yuan(item.pickupFee) }}</text>
          </view>
          <text class="option-price">¥{{ fen2yuan(item.displayPrice) }}</text>
        </view>
        <view v-if="!services.length" class="empty">暂无可预约的服务</view>
      </view>

      <view class="section">
        <text class="section-title">选择车辆</text>
        <view v-for="item in vehicles" :key="item.vehicleId" class="option"
              :class="{ active: vehicleId === item.vehicleId }" @click="vehicleId = item.vehicleId">
          <view class="option-main">
            <text class="option-name">{{ item.plateNo }}</text>
            <text class="option-sub">{{ item.brand || '未填品牌' }}<text v-if="item.color"> · {{ item.color }}</text></text>
          </view>
        </view>
        <view v-if="!vehicles.length" class="empty">还没有车辆，请先添加车辆</view>
      </view>

      <!-- 钥匙柜：已满的柜子不允许选，避免下单时才报"无空闲格口" -->
      <view class="section">
        <text class="section-title">选择钥匙柜</text>
        <view v-for="item in cabinets" :key="item.cabinetId" class="option"
              :class="{ active: cabinetId === item.cabinetId, disabled: item.full }" @click="selectCabinet(item)">
          <view class="option-main">
            <text class="option-name">{{ item.name }}</text>
            <text class="option-sub">空闲格口 {{ item.slotFree }} / {{ item.slotTotal }}</text>
          </view>
          <text class="option-tag" :class="{ 'tag-full': item.full }">{{ item.full ? '已满' : '可预约' }}</text>
        </view>
        <view v-if="!cabinets.length" class="empty">暂无可用钥匙柜</view>
      </view>

      <view class="section">
        <text class="section-title">预约取车时间</text>
        <view class="row">
          <picker mode="date" :value="appointDate" :start="todayStr" @change="onDateChange">
            <view class="picker">{{ appointDate }}</view>
          </picker>
          <picker mode="time" :value="appointTime" @change="onTimeChange">
            <view class="picker">{{ appointTime }}</view>
          </picker>
        </view>
      </view>

      <view class="section">
        <text class="section-title">停车备注</text>
        <input class="input" v-model="remark" placeholder="如：停在地库 A 区 23 号" />
      </view>

      <view class="agree" @click="agreed = !agreed">
        <text class="checkbox">{{ agreed ? '☑' : '☐' }}</text>
        <text class="agree-text">我已阅读并同意《代客洗车服务协议》</text>
      </view>

      <view class="primary-btn" :class="{ disabled: !canSubmit }" @click="submitOrder">
        {{ submitting ? '提交中…' : '立即下单' }}
      </view>
    </block>

    <view class="entry" @click="goOrders">
      <text class="entry-title">我的订单</text>
      <text class="entry-desc">查看进行中 / 待评价 / 全部订单</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createOrder } from '@/api/order'
import { fetchCabinets, fetchServices, fetchVehicles } from '@/api/catalog'
import type { CabinetVO, ServiceVO, VehicleVO } from '@/api/catalog'

const services = ref<ServiceVO[]>([])
const vehicles = ref<VehicleVO[]>([])
const cabinets = ref<CabinetVO[]>([])

const serviceId = ref<number | undefined>(undefined)
const vehicleId = ref<number | undefined>(undefined)
const cabinetId = ref<number | undefined>(undefined)

const appointDate = ref(tomorrow())
const appointTime = ref('19:00')
const remark = ref('')
const agreed = ref(false)
const submitting = ref(false)
const loading = ref(true)

const todayStr = today()
const canSubmit = computed(
  () => !!serviceId.value && !!vehicleId.value && !!cabinetId.value && agreed.value && !submitting.value
)

/** 金额一律以「分」传给后端，展示时才转元 */
function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function pad(n: number): string {
  return `${n}`.padStart(2, '0')
}

function today(): string {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 预约日期默认明天，格式与契约一致：yyyy-MM-dd */
function tomorrow(): string {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function selectCabinet(item: CabinetVO): void {
  if (item.full) {
    uni.showToast({ title: '该柜格口已满，换一个吧', icon: 'none' })
    return
  }
  cabinetId.value = item.cabinetId
}

function onDateChange(e: { detail: { value: string } }): void {
  appointDate.value = e.detail.value
}

function onTimeChange(e: { detail: { value: string } }): void {
  appointTime.value = e.detail.value
}

/** 三个列表一次性拉齐，失败由 request 层统一提示；缺任一选项就不允许提交 */
function loadOptions(): void {
  loading.value = true
  Promise.all([fetchServices(), fetchVehicles(), fetchCabinets()])
    .then(([serviceList, vehicleList, cabinetList]) => {
      services.value = serviceList || []
      vehicles.value = vehicleList || []
      cabinets.value = cabinetList || []

      // 默认选中：服务项与车辆取第一项，机柜取第一个"未满"的
      serviceId.value = services.value[0]?.serviceId
      vehicleId.value = vehicles.value[0]?.vehicleId
      const usable = cabinets.value.find((item) => !item.full)
      cabinetId.value = usable?.cabinetId ?? cabinets.value[0]?.cabinetId
    })
    .finally(() => {
      loading.value = false
    })
}

async function submitOrder(): Promise<void> {
  if (!canSubmit.value) {
    if (!agreed.value) {
      uni.showToast({ title: '请先勾选同意服务协议', icon: 'none' })
    }
    return
  }
  submitting.value = true
  try {
    const result = await createOrder({
      serviceId: serviceId.value as number,
      vehicleId: vehicleId.value as number,
      cabinetId: cabinetId.value as number,
      appointDate: appointDate.value,
      appointTime: appointTime.value,
      pickupRequired: true,
      remark: remark.value || undefined,
      agreed: true
    })
    uni.showToast({ title: '下单成功，待支付', icon: 'none' })
    uni.navigateTo({ url: `/pages/orders/orders?orderNo=${result.orderNo}` })
  } catch {
    // request 层已统一提示，这里只恢复按钮状态
  } finally {
    submitting.value = false
  }
}

function goOrders(): void {
  uni.navigateTo({ url: '/pages/orders/orders' })
}

onMounted(loadOptions)
</script>

<style>
.page {
  padding: 32rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.hero {
  padding: 32rpx 0 40rpx;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
  color: #222;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #888;
}

.tip {
  padding: 60rpx 0;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.section {
  margin-bottom: 32rpx;
}

.section-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  color: #666;
}

.option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.option.active {
  border-color: #1a73e8;
  background: #f2f7ff;
}

.option.disabled {
  opacity: 0.5;
}

.option-main {
  flex: 1;
  min-width: 0;
}

.option-name {
  display: block;
  font-size: 30rpx;
  color: #222;
}

.option-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #999;
}

.option-price {
  font-size: 32rpx;
  color: #e8700a;
  margin-left: 16rpx;
}

.option-tag {
  font-size: 24rpx;
  color: #1a73e8;
  margin-left: 16rpx;
}

.tag-full {
  color: #d93025;
}

.empty {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  font-size: 26rpx;
  color: #999;
}

.row {
  display: flex;
  gap: 16rpx;
}

.picker {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 30rpx;
  color: #222;
  text-align: center;
}

.input {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 28rpx;
}

.agree {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
}

.checkbox {
  font-size: 32rpx;
  color: #1a73e8;
}

.agree-text {
  margin-left: 12rpx;
  font-size: 26rpx;
  color: #666;
}

.primary-btn {
  background: #1a73e8;
  color: #fff;
  text-align: center;
  padding: 28rpx 0;
  border-radius: 16rpx;
  font-size: 32rpx;
  margin-bottom: 32rpx;
}

.primary-btn.disabled {
  background: #b8c6da;
}

.entry {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}

.entry-title {
  display: block;
  font-size: 32rpx;
  color: #222;
}

.entry-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #999;
}
</style>

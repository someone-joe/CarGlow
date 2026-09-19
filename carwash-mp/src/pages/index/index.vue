<template>
  <view class="page">
    <SiteBar />
    <view class="hero">
      <text class="title">夜间代客洗车</text>
      <text class="subtitle">今晚下单，明早干净上路</text>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 从优惠券页「去使用」带过来的券（暂只做展示，后端下单接口接入 couponUserId 后再真正抵扣） -->
      <view v-if="selectedCouponId" class="coupon-banner">
        <text class="coupon-banner-text">已选优惠券 #{{ selectedCouponId }}，提交订单时将尝试抵扣</text>
        <text class="coupon-banner-clear" @click="clearCoupon">清除</text>
      </view>

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

      <!-- P7 停放照片：选填但强引导，最多 3 张；fileId 待上传接口（openapi 未定义）接入后再随单上报 -->
      <view class="section">
        <text class="section-title">车辆停放照片（选填）</text>
        <text class="section-hint">拍一张停放照片，工作人员更容易找到您的车</text>
        <view class="photos">
          <view v-for="(p, i) in photos" :key="i" class="photo">
            <image class="photo-img" :src="p" mode="aspectFill" />
            <text class="photo-del" @click="removePhoto(i)">×</text>
          </view>
          <view v-if="photos.length < 3" class="photo photo-add" @click="choosePhoto">
            <text class="photo-plus">＋</text>
          </view>
        </view>
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
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { createOrder } from '@/api/order'
import { fetchCabinets, fetchServices, fetchVehicles } from '@/api/catalog'
import SiteBar from '@/components/SiteBar.vue'
import { orderDraft } from '@/store/orderDraft'
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
const photos = ref<string[]>([])
const agreed = ref(false)
const submitting = ref(false)
const loading = ref(true)
const selectedCouponId = ref<number | undefined>(undefined)

const todayStr = today()
const canSubmit = computed(
  () => !!serviceId.value && !!vehicleId.value && !!cabinetId.value && agreed.value && !submitting.value
)

function clearCoupon(): void {
  selectedCouponId.value = undefined
}

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

/** 选停放照片，最多 3 张；本地临时路径预览，真正上报待上传接口接入 */
function choosePhoto(): void {
  uni.chooseImage({
    count: 3 - photos.value.length,
    success: (res) => {
      photos.value.push(...res.tempFilePaths)
    },
  })
}

function removePhoto(i: number): void {
  photos.value.splice(i, 1)
}

/** 三个列表一次性拉齐，失败由 request 层统一提示；缺任一选项就不允许提交 */
function loadOptions(): void {
  loading.value = true
  Promise.all([fetchServices(), fetchVehicles(), fetchCabinets()])
    .then(([serviceList, vehicleList, cabinetList]) => {
      services.value = serviceList || []
      vehicles.value = vehicleList || []
      cabinets.value = cabinetList || []

      // 默认选中：优先用服务列表/详情带入的预选服务，否则取第一项
      serviceId.value = orderDraft.serviceId ?? services.value[0]?.serviceId
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
      agreed: true,
      couponUserId: selectedCouponId.value
    })
    uni.showToast({ title: '下单成功，待支付', icon: 'none' })
    // 用券后清空选择，避免返回重复带参
    selectedCouponId.value = undefined
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

// 从优惠券页「去使用」带入的券 ID（示例：pages/coupons/coupons.vue 跳转时携带）
onLoad((opts) => {
  const id = opts?.couponUserId ? Number(opts.couponUserId) : undefined
  selectedCouponId.value = id && !isNaN(id) ? id : orderDraft.couponUserId
})

// 用 onShow 而非 onMounted：从车辆页新增车辆返回后，这里的车辆列表要立刻看到新车
onShow(loadOptions)
</script>

<style>
.page {
  padding: calc(var(--status-bar-height, 0px) + 24rpx) 32rpx 32rpx;
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

.section-hint {
  display: block;
  margin: 8rpx 0 16rpx;
  font-size: 24rpx;
  color: #999;
}

.photos {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.photo {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background: #fff;
  overflow: hidden;
}

.photo-img {
  width: 100%;
  height: 100%;
}

.photo-del {
  position: absolute;
  top: 4rpx;
  right: 8rpx;
  width: 36rpx;
  height: 36rpx;
  line-height: 32rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  font-size: 28rpx;
}

.photo-add {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #ccc;
}

.photo-plus {
  font-size: 56rpx;
  color: #bbb;
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

.coupon-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff3e0;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
}

.coupon-banner-text {
  font-size: 26rpx;
  color: #e8700a;
}

.coupon-banner-clear {
  font-size: 26rpx;
  color: #1a73e8;
  margin-left: 16rpx;
}
</style>

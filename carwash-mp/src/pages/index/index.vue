<template>
  <view class="page">
    <SiteBar />
    <view class="hero">
      <text class="title">夜间代客洗车</text>
      <text class="subtitle">今晚下单，明早干净上路</text>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <view v-if="selectedCouponId" class="coupon-banner">
        <text class="coupon-banner-text">已选优惠券 #{{ selectedCouponId }}，提交订单时将尝试抵扣</text>
        <text class="coupon-banner-clear" @click="clearCoupon">清除</text>
      </view>

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

      <!-- P4 钥匙柜选择：独立页面，点击进入列表选柜 -->
      <view class="section">
        <text class="section-title">选择钥匙柜</text>
        <view class="cabinet-entry" @click="goCabinet">
          <view class="option-main">
            <text class="option-name">{{ selectedCabinetName || '请选择就近钥匙柜' }}</text>
            <text class="option-sub">{{ cabinetSub }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
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

      <view class="section">
        <text class="section-title">车辆停放照片（选填）</text>
        <text class="section-hint">拍一张停放照片，工作人员更容易找到您的车</text>
        <view class="photos">
          <view v-for="(p, i) in photos" :key="i" class="photo">
            <image class="photo-img" :src="p" mode="aspectFill" />
            <text class="photo-del" @click="removePhoto(i)">×</text>
          </view>
          <view v-if="photos.length < 6" class="photo photo-add" @click="choosePhoto">
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
import {
  fetchCabinets,
  fetchServices,
  fetchVehicles,
  type CabinetVO,
  type ServiceVO,
  type VehicleVO,
} from '@/api/catalog'
import { uploadMedia, MAX_UPLOAD_MB } from '@/api/media'
import SiteBar from '@/components/SiteBar.vue'
import { orderDraft } from '@/store/orderDraft'

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

const selectedCabinetName = computed(
  () => cabinets.value.find((c) => c.cabinetId === cabinetId.value)?.name || ''
)
const cabinetSub = computed(() => {
  const c = cabinets.value.find((x) => x.cabinetId === cabinetId.value)
  if (!c) return '点击选择就近的自助钥匙柜'
  return `空闲格口 ${c.slotFree} / ${c.slotTotal}`
})

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

function onDateChange(e: { detail: { value: string } }): void {
  appointDate.value = e.detail.value
}

function onTimeChange(e: { detail: { value: string } }): void {
  appointTime.value = e.detail.value
}

/** 停放照片上限：与契约 parkPhotoFileIds maxItems、后端校验同值 */
const MAX_PHOTOS = 6

/**
 * 选停放照片；本地临时路径预览。
 * count 必须先拦非正数：微信 chooseImage 对 0/负数不限张数（bug092204 的根因）。
 */
function choosePhoto(): void {
  const remain = MAX_PHOTOS - photos.value.length
  if (remain <= 0) {
    uni.showToast({ title: `最多上传 ${MAX_PHOTOS} 张`, icon: 'none' })
    return
  }
  uni.chooseImage({
    count: remain,
    success: (res) => {
      // 大小预检：超限图直接跳过并提示，不等上传后被拒（后端限 10MB）
      const files = (Array.isArray(res.tempFiles) ? res.tempFiles : [res.tempFiles]) as {
        path: string
        size?: number
      }[]
      const ok = files.filter((f) => {
        if ((f.size ?? 0) > MAX_UPLOAD_MB * 1024 * 1024) {
          uni.showToast({ title: `图片不能超过 ${MAX_UPLOAD_MB}MB，已跳过`, icon: 'none' })
          return false
        }
        return true
      })
      photos.value.push(...ok.map((f) => f.path))
    },
  })
}

function removePhoto(i: number): void {
  photos.value.splice(i, 1)
}

function goCabinet(): void {
  uni.navigateTo({ url: '/pages/cabinet/cabinet' })
}

function goOrders(): void {
  uni.navigateTo({ url: '/pages/orders/orders' })
}

/** 三个列表一次性拉齐，失败由 request 层统一提示；缺任一选项就不允许提交 */
function loadOptions(): void {
  loading.value = true
  Promise.all([fetchServices(), fetchVehicles(), fetchCabinets()])
    .then(([serviceList, vehicleList, cabinetList]) => {
      services.value = serviceList || []
      vehicles.value = vehicleList || []
      cabinets.value = cabinetList || []

      // 默认选中：优先用服务/详情/选柜页带入的预选，否则取第一项
      serviceId.value = orderDraft.serviceId ?? services.value[0]?.serviceId
      vehicleId.value = vehicles.value[0]?.vehicleId
      const usable = cabinets.value.find((item) => !item.full)
      cabinetId.value = orderDraft.cabinetId ?? usable?.cabinetId ?? cabinets.value[0]?.cabinetId
    })
    .finally(() => {
      loading.value = false
    })
}

/** P7 停放照片：逐张上传（bizType=PARK），拿到 fileId 后随单上报 */
async function uploadParkPhotos(): Promise<string[]> {
  const ids: string[] = []
  for (const p of photos.value) {
    try {
      const m = await uploadMedia(p, 'PARK')
      if (m.fileId) ids.push(m.fileId)
    } catch (e) {
      // 把后端拒绝原因亮出来（如"文件超过 10MB 上限"），不说含糊的"失败"
      uni.showToast({ title: (e as { msg?: string }).msg || '照片上传失败，请重试', icon: 'none' })
      throw new Error('upload failed')
    }
  }
  return ids
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
    const parkPhotoFileIds = await uploadParkPhotos()
    const result = await createOrder({
      serviceId: serviceId.value as number,
      vehicleId: vehicleId.value as number,
      cabinetId: cabinetId.value as number,
      appointDate: appointDate.value,
      appointTime: appointTime.value,
      pickupRequired: true,
      remark: remark.value || undefined,
      parkPhotoFileIds,
      agreed: true,
      couponUserId: selectedCouponId.value,
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

// 从优惠券页「去使用」带入的券 ID（示例：pages/coupons/coupons.vue 跳转时携带）
onLoad((opts) => {
  const id = opts?.couponUserId ? Number(opts.couponUserId) : undefined
  selectedCouponId.value = id && !isNaN(id) ? id : orderDraft.couponUserId
})

// 用 onShow 而非 onMounted：从车辆页/选柜页返回后，列表与预选要立刻生效
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

.arrow {
  font-size: 40rpx;
  color: #ccc;
  margin-left: 16rpx;
}

.cabinet-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  padding: 28rpx;
}

.cabinet-entry:active {
  background: #f2f7ff;
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

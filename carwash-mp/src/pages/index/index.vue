<template>
  <view class="page">
    <SiteBar />

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <view v-if="selectedCouponId" class="coupon-banner">
        <text class="coupon-banner-text">已选优惠券 #{{ selectedCouponId }}，提交订单时将尝试抵扣</text>
        <text class="coupon-banner-clear" @click="clearCoupon">清除</text>
      </view>

      <!-- STEP 1 选择服务时间 -->
      <view class="step">
        <view class="step-head">
          <text class="step-no">STEP 1</text>
          <text class="step-title">选择服务时间</text>
        </view>
        <view class="step-body" @click="openTimeSheet">
          <text class="step-value">{{ appointDate }} {{ appointTime }}</text>
          <text class="step-action">选择 ›</text>
        </view>
      </view>

      <!-- STEP 2 选择服务方式（取送车 + 钥匙柜） -->
      <view class="step">
        <view class="step-head">
          <text class="step-no">STEP 2</text>
          <text class="step-title">选择服务方式</text>
        </view>
        <view class="switch-row">
          <view>
            <text class="switch-label">是否需要取送车服务</text>
            <text class="switch-sub">师傅上门取车，洗完送回</text>
          </view>
          <switch :checked="pickupRequired" @change="onPickupChange" color="#00aeb5" />
        </view>
        <view class="step-body" @click="goCabinet">
          <view class="step-body-main">
            <text class="step-value" :class="{ placeholder: !selectedCabinetName }">
              {{ selectedCabinetName || '请选择就近钥匙柜' }}
            </text>
            <text class="step-hint">{{ cabinetSub }}</text>
          </view>
          <text class="step-action">选择 ›</text>
        </view>
      </view>

      <!-- STEP 3 选择服务 -->
      <view class="step">
        <view class="step-head">
          <text class="step-no">STEP 3</text>
          <text class="step-title">选择服务</text>
        </view>
        <view v-for="item in services" :key="item.serviceId" class="svc"
              :class="{ active: serviceId === item.serviceId }" @click="serviceId = item.serviceId">
          <view class="svc-main">
            <text class="svc-name">{{ item.name }}</text>
            <text class="svc-sub">约 {{ item.workMinutes }} 分钟</text>
          </view>
          <view class="svc-price">
            <text class="price-now">¥{{ fen2yuan(item.displayPrice) }}</text>
            <text v-if="item.originPrice && item.originPrice !== item.displayPrice" class="price-old">
              ¥{{ fen2yuan(item.originPrice) }}
            </text>
          </view>
        </view>
        <view v-if="!services.length" class="empty">暂无可预约的服务</view>
      </view>

      <!-- STEP 4 填写车辆信息 -->
      <view class="step">
        <view class="step-head">
          <text class="step-no">STEP 4</text>
          <text class="step-title">填写车辆信息</text>
        </view>
        <!-- 直接在本页选车：车辆页是管理页（新增/编辑），没有"选中"能力 -->
        <view v-for="item in vehicles" :key="item.vehicleId" class="svc"
              :class="{ active: vehicleId === item.vehicleId }" @click="vehicleId = item.vehicleId">
          <view class="svc-main">
            <text class="svc-name">{{ item.plateNo }}</text>
            <text class="svc-sub">
              {{ item.brand || '未填品牌' }}<text v-if="item.color"> · {{ item.color }}</text>
            </text>
          </view>
          <text v-if="vehicleId === item.vehicleId" class="svc-check">✓</text>
        </view>
        <view v-if="!vehicles.length" class="empty">还没有车辆，请先添加车辆</view>
        <view class="manage" @click="goVehicles">管理车辆（添加 / 编辑）›</view>
        <input class="input" v-model="remark" placeholder="车辆停放位置，如：地库 A 区 23 号" />
      </view>

      <!-- STEP 5 拍照留言 -->
      <view class="step">
        <view class="step-head">
          <text class="step-no">STEP 5</text>
          <text class="step-title">拍照留言</text>
        </view>
        <view class="photos">
          <view v-for="(p, i) in photos" :key="i" class="photo">
            <image class="photo-img" :src="p" mode="aspectFill" />
            <text class="photo-del" @click="removePhoto(i)">×</text>
          </view>
          <view v-if="photos.length < MAX_PHOTOS" class="photo photo-add" @click="choosePhoto">
            <text class="photo-plus">＋</text>
          </view>
        </view>
        <text class="step-hint">拍一张停放照片，师傅更容易找到您的车（最多 {{ MAX_PHOTOS }} 张）</text>
      </view>

      <view class="agree" @click="agreed = !agreed">
        <text class="checkbox">{{ agreed ? '☑' : '☐' }}</text>
        <text class="agree-text">我已阅读并同意《代客洗车服务协议》</text>
      </view>

      <view class="submit" :class="{ disabled: !canSubmit }" @click="submitOrder">
        {{ submitting ? '提交中…' : '确定下单' }}
      </view>
    </block>

    <!-- 时间浮层 -->
    <view v-if="showTimeSheet" class="sheet-mask" @click="showTimeSheet = false">
      <view class="sheet" @click.stop>
        <view class="sheet-head">
          <text class="sheet-title">选择服务时间</text>
          <text class="sheet-close" @click="showTimeSheet = false">×</text>
        </view>

        <scroll-view class="days" scroll-x>
          <view v-for="d in dayTabs" :key="d.date" class="day" :class="{ active: appointDate === d.date }"
                @click="pickDay(d.date)">
            <text class="day-label">{{ d.label }}</text>
            <text class="day-date">{{ d.md }}</text>
          </view>
        </scroll-view>

        <view class="slots">
          <view v-for="t in timeSlots" :key="t" class="slot" :class="{ active: appointTime === t }" @click="pickTime(t)">
            {{ t }}
          </view>
        </view>

        <view v-if="capacity" class="sheet-tip">
          最晚存钥匙 {{ capacity.depositDeadline || '—' }} · 承诺还车 {{ capacity.promiseReturnTime || '—' }}
        </view>

        <view class="sheet-confirm" :class="{ disabled: !appointTime }" @click="confirmTime">确定</view>
      </view>
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
import { fetchCapacity, type CapacityVO } from '@/api/home'
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
const pickupRequired = ref(true)

/** 时间浮层 */
const showTimeSheet = ref(false)
const capacity = ref<CapacityVO | null>(null)

/**
 * 是否需要在 onShow 时刷新列表。
 * 不能无条件 onShow 刷新：从系统相册返回也会触发 onShow，
 * 重载会让 loading 切换导致整块重建（页面滚到顶部），还会覆盖已选的服务/车辆/柜子。
 */
const needRefresh = ref(false)

/**
 * 可选时间段（夜间服务：19:00-23:00 整点）。
 * TODO：营业时间应来自站点配置，当前后端未下发，先用固定时段并在浮层提示最晚存钥匙时间。
 */
const timeSlots = ['19:00', '20:00', '21:00', '22:00', '23:00']

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
const selectedPlate = computed(() => vehicles.value.find((v) => v.vehicleId === vehicleId.value)?.plateNo || '')

/** 近 7 天，横向日期 tab */
const dayTabs = computed(() => {
  const list: { date: string; label: string; md: string }[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
    list.push({
      date,
      label: i === 0 ? '今天' : i === 1 ? '明天' : i === 2 ? '后天' : `${d.getMonth() + 1}/${d.getDate()}`,
      md: `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
    })
  }
  return list
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

/** switch 的 change 在 vue-tsc 下推断为 Event，取值需要按 uni 的结构断言 */
function onPickupChange(e: Event): void {
  const detail = (e as unknown as { detail?: { value?: boolean } }).detail
  pickupRequired.value = detail?.value ?? false
}

function openTimeSheet(): void {
  showTimeSheet.value = true
  if (!capacity.value) {
    fetchCapacity()
      .then((c) => (capacity.value = c))
      .catch(() => null)
  }
}

function pickDay(date: string): void {
  appointDate.value = date
}

function pickTime(t: string): void {
  appointTime.value = t
}

function confirmTime(): void {
  if (!appointTime.value) return
  showTimeSheet.value = false
}

function goCabinet(): void {
  needRefresh.value = true
  uni.navigateTo({ url: '/pages/cabinet/cabinet' })
}

function goVehicles(): void {
  needRefresh.value = true
  uni.navigateTo({ url: '/pages/vehicle/vehicle' })
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

/** 三个列表一次性拉齐，失败由 request 层统一提示；缺任一选项就不允许提交 */
function loadOptions(): void {
  loading.value = true
  Promise.all([fetchServices(), fetchVehicles(), fetchCabinets()])
    .then(([serviceList, vehicleList, cabinetList]) => {
      services.value = serviceList || []
      vehicles.value = vehicleList || []
      cabinets.value = cabinetList || []

      // 只补全「未选」的项：不能覆盖用户已选，否则刷新一次选择就丢了
      if (serviceId.value == null) {
        serviceId.value = orderDraft.serviceId ?? services.value[0]?.serviceId
      }
      if (vehicleId.value == null) {
        vehicleId.value = vehicles.value[0]?.vehicleId
      }
      if (cabinetId.value == null) {
        const usable = cabinets.value.find((item) => !item.full)
        cabinetId.value = orderDraft.cabinetId ?? usable?.cabinetId ?? cabinets.value[0]?.cabinetId
      }
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
      pickupRequired: pickupRequired.value,
      remark: remark.value || undefined,
      parkPhotoFileIds,
      agreed: true,
      couponUserId: selectedCouponId.value,
    })
    uni.showToast({ title: '下单成功，待支付', icon: 'none' })
    // 用券后清空选择，避免返回重复带参
    selectedCouponId.value = undefined
    // redirectTo 替换掉下单页：返回键回到进入下单页前的页面（如「我的」），
    // 不会再回到已提交过的下单页造成重复提交（bug092403）
    uni.redirectTo({ url: `/pages/orders/orders?orderNo=${result.orderNo}` })
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
  loadOptions()
})

// 只在从车辆页/选柜页返回时刷新：避免选照片后页面回到顶部
onShow(() => {
  if (needRefresh.value) {
    needRefresh.value = false
    loadOptions()
  }
})
</script>

<style>
.page {
  padding: 24rpx 24rpx 60rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 60rpx 0;
}

.coupon-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff6e5;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  font-size: 25rpx;
  color: #a06a1b;
}

.coupon-banner-clear {
  color: #ff5b4a;
}

/* ---- STEP 卡片 ---- */
.step {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.step-head {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.step-no {
  font-size: 22rpx;
  color: #00aeb5;
  background: #e6f7f7;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
  margin-right: 12rpx;
}

.step-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.step-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f6faf9;
  border-radius: 16rpx;
  padding: 26rpx 24rpx;
}

.step-body-main {
  flex: 1;
}

.step-value {
  font-size: 30rpx;
  color: #1b2b2a;
  font-weight: 500;
}

.step-value.placeholder {
  color: #8a9a98;
}

.step-hint {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.step-action {
  font-size: 26rpx;
  color: #00aeb5;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 0 24rpx;
}

.switch-label {
  font-size: 28rpx;
  color: #1b2b2a;
}

.switch-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

/* ---- 服务列表 ---- */
.svc {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid #eaf1f0;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.svc.active {
  border-color: #00aeb5;
  background: #f3fbfb;
}

.svc-name {
  font-size: 29rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.svc-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.svc-check {
  font-size: 32rpx;
  color: #00aeb5;
}

.manage {
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #00aeb5;
  padding: 12rpx 0;
}

.svc-price {
  text-align: right;
}

.price-now {
  font-size: 34rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.price-old {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #b6c2c0;
  text-decoration: line-through;
}

.empty {
  color: #8a9a98;
  font-size: 26rpx;
  padding: 20rpx 0;
}

/* ---- 输入 ---- */
.input {
  margin-top: 16rpx;
  background: #f6faf9;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #1b2b2a;
}

/* ---- 照片 ---- */
.photos {
  display: flex;
  flex-wrap: wrap;
}

.photo {
  position: relative;
  width: 150rpx;
  height: 150rpx;
  margin: 0 16rpx 16rpx 0;
  border-radius: 14rpx;
  overflow: hidden;
}

.photo-img {
  width: 100%;
  height: 100%;
}

.photo-del {
  position: absolute;
  top: 0;
  right: 0;
  width: 40rpx;
  height: 40rpx;
  line-height: 36rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 28rpx;
  border-bottom-left-radius: 12rpx;
}

.photo-add {
  background: #f6faf9;
  border: 2rpx dashed #cfe0de;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-plus {
  font-size: 56rpx;
  color: #00aeb5;
}

/* ---- 协议与提交 ---- */
.agree {
  display: flex;
  align-items: center;
  padding: 16rpx 8rpx 32rpx;
}

.checkbox {
  font-size: 32rpx;
  color: #00aeb5;
  margin-right: 12rpx;
}

.agree-text {
  font-size: 25rpx;
  color: #6b7b79;
}

.submit {
  background: #00aeb5;
  color: #fff;
  text-align: center;
  padding: 30rpx 0;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.submit.disabled {
  background: #cfe0de;
}

/* ---- 时间浮层 ---- */
.sheet-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 20;
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  padding: 28rpx 28rpx 40rpx;
  box-sizing: border-box;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.sheet-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.sheet-close {
  font-size: 40rpx;
  color: #8a9a98;
}

.days {
  white-space: nowrap;
  margin-bottom: 20rpx;
}

.day {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 26rpx;
  margin-right: 16rpx;
  border-radius: 16rpx;
  background: #f2f7f7;
}

.day.active {
  background: #00aeb5;
}

.day-label {
  font-size: 24rpx;
  color: #6b7b79;
}

.day-date {
  margin-top: 4rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.day.active .day-label,
.day.active .day-date {
  color: #fff;
}

.slots {
  display: flex;
  flex-wrap: wrap;
}

.slot {
  width: 22%;
  margin: 0 3% 18rpx 0;
  text-align: center;
  padding: 22rpx 0;
  border-radius: 14rpx;
  background: #f2f7f7;
  font-size: 27rpx;
  color: #1b2b2a;
}

.slot.active {
  background: #14342f;
  color: #fff;
}

.sheet-tip {
  font-size: 24rpx;
  color: #8a9a98;
  padding: 8rpx 0 24rpx;
}

.sheet-confirm {
  background: #14342f;
  color: #fff;
  text-align: center;
  padding: 28rpx 0;
  border-radius: 44rpx;
  font-size: 31rpx;
  font-weight: 600;
}

.sheet-confirm.disabled {
  background: #cfe0de;
}
</style>

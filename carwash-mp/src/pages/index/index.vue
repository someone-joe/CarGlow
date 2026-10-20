<template>
  <view class="cg-page order">
    <SiteBar />

    <view v-if="loading" class="order-loading">加载中…</view>

    <block v-else>
      <view v-if="selectedCouponId" class="cg-card order-coupon">
        <view class="order-coupon-dot" />
        <text class="order-coupon-text">已选优惠券 #{{ selectedCouponId }}，提交订单时将尝试抵扣</text>
        <view class="cg-btn-text order-coupon-clear" @click="clearCoupon">清除</view>
      </view>

      <!-- STEP 1 选择服务时间 -->
      <view class="cg-card cg-card-tint order-step">
        <text class="order-step-no">STEP 1</text>
        <text class="order-step-title">选择服务时间</text>
        <view class="cg-row order-row cg-tap" hover-class="cg-hover" @click="openTimeSheet">
          <text class="cg-row-value">{{ appointDate }} {{ appointTime }}</text>
          <view class="cg-btn-text">选择</view>
        </view>
      </view>

      <!-- STEP 2 选择服务方式（取送车 + 钥匙柜） -->
      <view class="cg-card cg-card-tint order-step">
        <text class="order-step-no">STEP 2</text>
        <text class="order-step-title">选择服务方式</text>
        <view class="order-switch-row">
          <view>
            <text class="order-switch-label">是否需要取送车服务</text>
            <text class="order-switch-sub">师傅上门取车，洗完送回</text>
          </view>
          <switch :checked="pickupRequired" @change="onPickupChange" color="#007AFF" />
        </view>
        <view class="cg-row order-row cg-tap" hover-class="cg-hover" @click="goCabinet">
          <view>
            <text class="cg-row-value" :class="{ 'is-placeholder': !selectedCabinetName }">
              {{ selectedCabinetName || '请选择就近钥匙柜' }}
            </text>
            <text class="order-row-hint">{{ cabinetSub }}</text>
          </view>
          <view class="cg-btn-text">选择</view>
        </view>
      </view>

      <!-- STEP 3 选择服务 -->
      <view class="cg-card cg-card-tint order-step">
        <text class="order-step-no">STEP 3</text>
        <text class="order-step-title">选择服务</text>
        <view v-for="item in services" :key="item.serviceId" class="order-svc cg-tap"
              :class="{ active: serviceId === item.serviceId }" hover-class="cg-hover" @click="serviceId = item.serviceId">
          <view>
            <text class="order-svc-name">{{ item.name }}</text>
            <text class="order-svc-sub">约 {{ item.workMinutes }} 分钟</text>
          </view>
          <view class="order-svc-price">
            <text class="order-svc-now">¥{{ fen2yuan(item.displayPrice) }}</text>
            <text v-if="item.originPrice && item.originPrice !== item.displayPrice" class="order-svc-origin">
              ¥{{ fen2yuan(item.originPrice) }}
            </text>
          </view>
        </view>
        <view v-if="!services.length" class="order-empty">暂无可预约的服务</view>
      </view>

      <!-- STEP 4 填写车辆信息 -->
      <view class="cg-card cg-card-tint order-step">
        <text class="order-step-no">STEP 4</text>
        <text class="order-step-title">填写车辆信息</text>
        <VehiclePicker :vehicles="vehicles" v-model="vehicleId" @navigate="openSelect" @manage="onVehicleManage" />

        <!-- 常用停车位置：选一个直接填入，免手输（T6 数据） -->
        <view class="order-park">
          <view class="order-park-head">
            <text class="order-park-label">常用停车位置</text>
            <view class="cg-btn-text order-park-manage" @click="goPark">管理</view>
          </view>
          <view v-if="parkLocations.length" class="order-park-chips">
            <text
              v-for="p in parkLocations"
              :key="p.locationId"
              class="cg-pill cg-tap"
              :class="{ 'is-active': selectedParkId === p.locationId }"
              hover-class="cg-hover"
              @click="pickPark(p)"
            >{{ parkLabel(p) }}</text>
          </view>
          <view v-else class="cg-btn-text order-park-empty" @click="goPark">还没有常用位置，去添加</view>
          <input v-model="remark" class="cg-input order-park-input" placeholder="车辆停放位置，如：地库 A 区 23 号" />
        </view>
      </view>

      <!-- STEP 5 拍照留言 -->
      <view class="cg-card cg-card-tint order-step">
        <text class="order-step-no">STEP 5</text>
        <text class="order-step-title">拍照留言</text>
        <view class="order-photos">
          <view v-for="(p, i) in photos" :key="i" class="order-photo">
            <image class="order-photo-img" :src="p" mode="aspectFill" />
            <view class="order-photo-del cg-tap" hover-class="cg-hover" @click="removePhoto(i)">×</view>
          </view>
          <view v-if="photos.length < MAX_PHOTOS" class="order-photo order-photo-add cg-tap" hover-class="cg-hover" @click="choosePhoto">
            <text class="order-photo-plus">+</text>
          </view>
        </view>
        <text class="order-step-hint">拍一张停放照片，师傅更容易找到您的车（最多 {{ MAX_PHOTOS }} 张）</text>
      </view>

      <view class="order-agree cg-tap" hover-class="cg-hover" @click="agreed = !agreed">
        <view class="cg-check" :class="{ 'is-on': agreed }" />
        <text class="order-agree-text">我已阅读并同意《代客洗车服务协议》</text>
      </view>
    </block>

    <!-- 底部固定结算栏：选中服务 + 金额 + 确认下单 -->
    <view v-if="!loading" class="cg-bottom-bar">
      <view class="order-bar-main">
        <text class="order-bar-name">{{ selectedService?.name || '请选择服务' }}</text>
        <text class="order-bar-price">¥{{ fen2yuan(selectedService?.displayPrice) }}</text>
      </view>
      <view class="cg-btn cg-btn-primary cg-tap order-bar-btn" :class="{ 'is-disabled': !canSubmit }" hover-class="cg-hover" @click="onSubmitClick">
        {{ submitting ? '提交中…' : '确认下单' }}
      </view>
    </view>

    <!-- 时间浮层 -->
    <view v-if="showTimeSheet" class="cg-mask" @click="showTimeSheet = false">
      <view class="cg-sheet" @click.stop>
        <view class="order-sheet-head">
          <text class="order-sheet-title">选择服务时间</text>
          <view class="cg-btn-text order-sheet-close" @click="showTimeSheet = false">×</view>
        </view>

        <scroll-view class="order-days" scroll-x>
          <view v-for="d in dayTabs" :key="d.date" class="cg-pill cg-tap order-day" :class="{ 'is-active': appointDate === d.date }"
                hover-class="cg-hover" @click="pickDay(d.date)">
            <text class="order-day-label">{{ d.label }}</text>
            <text class="order-day-date">{{ d.md }}</text>
          </view>
        </scroll-view>

        <view class="order-slots">
          <view v-for="t in timeSlots" :key="t" class="order-slot cg-tap" :class="{ active: appointTime === t }" hover-class="cg-hover" @click="pickTime(t)">
            {{ t }}
          </view>
        </view>

        <view v-if="capacity" class="order-sheet-tip">
          最晚存钥匙 {{ capacity.depositDeadline || '—' }} · 承诺还车 {{ capacity.promiseReturnTime || '—' }}
        </view>

        <view class="cg-btn cg-btn-primary cg-tap" :class="{ 'is-disabled': !appointTime }" hover-class="cg-hover" @click="confirmTime">确定</view>
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
import { fetchParkLocations, type ParkLocationVO } from '@/api/park'
import { uploadMedia, MAX_UPLOAD_MB } from '@/api/media'
import { fetchCapacity, type CapacityVO } from '@/api/home'
import SiteBar from '@/components/SiteBar.vue'
import VehiclePicker from '@/components/VehiclePicker.vue'
import { consumePickedVehicle } from '@/store/vehicleSelect'
import { orderDraft } from '@/store/orderDraft'
import { requirePhone } from '@/store/auth'

const services = ref<ServiceVO[]>([])
const vehicles = ref<VehicleVO[]>([])
const cabinets = ref<CabinetVO[]>([])
const parkLocations = ref<ParkLocationVO[]>([])

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
const selectedParkId = ref<number | undefined>(undefined)

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
 * 可选时间段：由站点营业时间生成整点（capacity 下发的 businessStart/businessEnd）。
 * 后端未下发或数据异常时兜底夜间时段 19:00-23:00，不再写死。
 */
const timeSlots = computed<string[]>(() => {
  const start = capacity.value?.businessStart || '19:00'
  const end = capacity.value?.businessEnd || '23:00'
  const s = Number(start.split(':')[0])
  const e = Number(end.split(':')[0])
  if (!Number.isFinite(s) || !Number.isFinite(e) || e < s || e > 23) {
    return ['19:00', '20:00', '21:00', '22:00', '23:00']
  }
  const out: string[] = []
  for (let h = s; h <= e; h++) {
    out.push(`${String(h).padStart(2, '0')}:00`)
  }
  return out
})

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

/** 底部结算栏：当前所选服务（未选时展示占位） */
const selectedService = computed(() => services.value.find((s) => s.serviceId === serviceId.value))

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

function openSelect(): void {
  needRefresh.value = true
  uni.navigateTo({ url: `/pages/vehicle/select?source=valet&selected=${vehicleId.value ?? ''}` })
}

function onVehicleManage(): void {
  needRefresh.value = true
  uni.navigateTo({ url: '/pages/vehicle/vehicle' })
}

function goPark(): void {
  needRefresh.value = true
  uni.navigateTo({ url: '/pages/park-location/park-location' })
}

function parkLabel(p: ParkLocationVO): string {
  return [p.communityName, p.building, p.parkingNo].filter(Boolean).join(' ') || '未命名位置'
}

/** 选一个常用停车位置：格式化填入停车位置输入，免手输 */
function pickPark(p: ParkLocationVO): void {
  selectedParkId.value = p.locationId
  remark.value = [p.communityName, p.building, p.parkingNo, p.detail].filter(Boolean).join('，')
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
  Promise.all([fetchServices(), fetchVehicles(), fetchCabinets(), fetchParkLocations()])
    .then(([serviceList, vehicleList, cabinetList, parkList]) => {
      services.value = serviceList || []
      vehicles.value = vehicleList || []
      cabinets.value = cabinetList || []
      parkLocations.value = parkList || []

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

/** 下单入口：先过手机号授权门控——已绑直接提交，未绑弹授权、登后自动继续 */
function onSubmitClick(): void {
  requirePhone(() => submitOrder())
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
    // 订单列表已是底部 tab：用 switchTab 切过去。
    // 注意 tab 页不能带 query 参数（微信限制），而原 orderNo 参数 orders 页从未读取，
    // 去掉不改变行为；switchTab 同样会关掉下单页，返回键不会回到已提交的页面。
    uni.switchTab({ url: '/pages/orders/orders' })
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
  const picked = consumePickedVehicle('valet')
  if (picked != null) vehicleId.value = picked
  if (needRefresh.value) {
    needRefresh.value = false
    loadOptions()
  }
})
</script>

<style>
/* 下单页：底部固定结算栏占位，页面底部留白需避开 */
.cg-page.order {
  padding-bottom: 220rpx;
}

.order-loading {
  padding: 96rpx 0;
  text-align: center;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

/* ---- 优惠券提示 ---- */
.cg-card.order-coupon {
  display: flex;
  align-items: center;
  margin-top: 32rpx;
  padding: 16rpx 32rpx;
}

.order-coupon-dot {
  flex: none;
  width: 12rpx;
  height: 12rpx;
  margin-right: 16rpx;
  border-radius: 50%;
  background: var(--cg-warning);
}

.order-coupon-text {
  flex: 1;
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.order-coupon-clear {
  color: var(--cg-text-sub);
}

/* ---- STEP 卡片 ---- */
.order-step {
  margin-top: 32rpx;
}

.order-step-no {
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
  letter-spacing: 2rpx;
}

.order-step-title {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.order-row {
  border-radius: var(--cg-radius-md);
}

.order-row-hint {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 取送车开关行 ---- */
.order-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
}

.order-switch-label {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-text);
}

.order-switch-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 服务选择：细蓝边框 + 6% 淡蓝底 + 提升阴影，不放大不发光 ---- */
.order-svc {
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  margin-top: 24rpx;
  padding: 24rpx;
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.order-svc.active {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
  box-shadow: var(--cg-shadow-raised);
}

.order-svc-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.order-svc-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.order-svc-price {
  text-align: right;
}

.order-svc-now {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.order-svc-origin {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
  text-decoration: line-through;
}

.order-empty {
  padding: 32rpx 0;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-aux);
}

/* ---- 常用停车位置 ---- */
.order-park {
  margin-top: 32rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid var(--cg-line);
}

.order-park-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-park-label {
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.order-park-chips {
  display: flex;
  flex-wrap: wrap;
  margin: 24rpx 0;
}

.order-park-chips .cg-pill {
  margin: 0 16rpx 16rpx 0;
}

.order-park-empty {
  margin: 24rpx 0;
}

.order-park-input {
  margin-top: 24rpx;
}

/* ---- 照片 ---- */
.order-photos {
  display: flex;
  flex-wrap: wrap;
  margin-top: 24rpx;
}

.order-photo {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  margin: 0 16rpx 16rpx 0;
  border-radius: var(--cg-radius-md);
  overflow: hidden;
}

.order-photo-img {
  width: 100%;
  height: 100%;
}

.order-photo-del {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #ffffff;
  font-size: var(--cg-font-caption);
  line-height: 40rpx;
  text-align: center;
}

.order-photo-add {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed var(--cg-border);
}

.order-photo-plus {
  font-size: var(--cg-font-subtitle);
  font-weight: 300;
  color: var(--cg-primary);
}

.order-step-hint {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  line-height: 1.6;
}

/* ---- 协议 ---- */
.order-agree {
  display: flex;
  align-items: center;
  padding: 32rpx 8rpx;
}

.order-agree-text {
  margin-left: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 底部结算栏 ---- */
.order-bar-main {
  flex: 1;
}

.order-bar-name {
  display: block;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.order-bar-price {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.order-bar-btn {
  width: 280rpx;
  margin-left: 32rpx;
}

/* ---- 时间浮层 ---- */
.order-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-sheet-title {
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.cg-btn-text.order-sheet-close {
  color: var(--cg-text-aux);
  font-size: var(--cg-font-subtitle);
}

.order-days {
  white-space: nowrap;
  margin: 32rpx 0;
}

.order-day {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  margin-right: 16rpx;
}

.order-day-label {
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.order-day.is-active .order-day-label {
  color: rgba(255, 255, 255, 0.85);
}

.order-day-date {
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  font-weight: 500;
  color: var(--cg-title);
}

.order-day.is-active .order-day-date {
  color: #ffffff;
}

.order-slots {
  display: flex;
  flex-wrap: wrap;
}

.order-slot {
  box-sizing: border-box;
  width: 22%;
  margin: 0 3% 16rpx 0;
  padding: 24rpx 0;
  text-align: center;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.order-slot.active {
  background: var(--cg-primary);
  color: #ffffff;
}

.order-sheet-tip {
  padding: 16rpx 0 32rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}
</style>

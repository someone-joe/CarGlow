<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="order">
      <!-- 状态头 -->
      <view class="status-bar">
        <text class="status">{{ order.statusLabel }}</text>
        <view v-if="order.status === 'SERVING'" class="countdown">
          <text class="cd-label">服务倒计时</text>
          <text class="cd-time">{{ countdown }}</text>
        </view>
        <text v-else-if="order.status === 'QUEUING'" class="queue-tip">排队第 {{ order.queueNo }} 位，前方订单结束后自动开始</text>
      </view>

      <view class="panel">
        <view class="panel-title">订单信息</view>
        <view class="row"><text class="label">服务</text><text class="value">{{ order.serviceName || '自助洗车' }}</text></view>
        <view v-if="order.plateNo" class="row"><text class="label">车牌</text><text class="value">{{ order.plateNo }}</text></view>
        <view v-if="order.vehicleTypeLabel" class="row"><text class="label">车型</text><text class="value">{{ order.vehicleTypeLabel }}</text></view>
        <view class="row"><text class="label">时长</text><text class="value">{{ order.serviceMinutes }} 分钟<text v-if="order.addedSeconds">（已加时 {{ Math.floor(order.addedSeconds / 60) }} 分钟）</text></text></view>
        <view class="row"><text class="label">实付</text><text class="value price">¥{{ fen2yuan(order.payAmount) }}</text></view>
        <view v-if="order.couponDiscount" class="row"><text class="label">优惠抵扣</text><text class="value discount">-¥{{ fen2yuan(order.couponDiscount) }}</text></view>
        <view class="row"><text class="label">支付方式</text><text class="value">{{ order.payChannel === 'BALANCE' ? '储值余额' : '微信支付' }}</text></view>
        <view class="row"><text class="label">单号</text><text class="value">{{ order.orderNo }}</text></view>
      </view>

      <!-- 操作 -->
      <view class="ops">
        <template v-if="order.status === 'WAIT_PAY'">
          <view class="op primary" @click="goPay">去支付</view>
          <view class="op danger" @click="doCancel">取消订单</view>
        </template>
        <view v-if="order.status === 'WAIT_USE'" class="op primary" @click="doActivate">开始服务</view>
        <template v-if="order.status === 'SERVING'">
          <view class="op" @click="doAddTime">加时 10 分钟</view>
          <view class="op plain" @click="doFinish">结束服务</view>
        </template>
        <view v-if="order.status === 'WAIT_USE'" class="op danger" @click="doCancel">取消订单</view>
      </view>
    </block>

    <view v-else class="tip">订单不存在</view>

    <!-- 加时确认：用页面内弹层，不用 uni.showModal（该 API 在运行环境偶发不弹窗，点击会毫无反应） -->
    <view v-if="addTimeDlg">
      <view class="mask" @click="addTimeDlg = false" />
      <view class="dialog">
        <text class="dlg-title">加时</text>
        <text class="dlg-desc">{{ addTimeDesc }}</text>
        <view class="dlg-btns">
          <view class="dlg-btn plain" @click="addTimeDlg = false">再想想</view>
          <view class="dlg-btn" :class="{ disabled: submitting }" @click="confirmAddTime">
            {{ submitting ? '处理中…' : addTimeBtn }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import {
  activateSelfwash,
  addSelfwashTime,
  cancelSelfwash,
  fetchSelfwashOrder,
  fetchSelfwashPricing,
  finishSelfwash,
  type SelfwashOrderVO,
  type SelfwashPricingVO,
} from '@/api/selfwash'

const orderNo = ref('')
const order = ref<SelfwashOrderVO | null>(null)
const loading = ref(true)
const remain = ref(0)
const countdown = ref('00:00')
/** 计时计价参数（后台配置），加时预估费用必须与后端同公式，不能另算 */
const pricing = ref<SelfwashPricingVO | null>(null)
/** 加时确认弹层。刻意不用 uni.showModal：该 API 在运行环境偶发不弹窗，导致点击毫无反应 */
const addTimeDlg = ref(false)
/** 加时提交中：防连点，扣款前先挡一层（后端幂等键是第二道防线） */
const submitting = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

let expireWatch: ReturnType<typeof setInterval> | null = null
let expireWatchTicks = 0

/**
 * 倒计时归零后降频回拉：5 秒一次、最多 6 次（约 30 秒，覆盖后端定时任务周期）。
 * 过去归零即立刻 load()，而 SERVING 且剩余仍为 0 时会再次触发归零 → 变成每秒一次请求。
 */
function startExpireWatch(): void {
  if (expireWatch) return
  expireWatchTicks = 0
  expireWatch = setInterval(() => {
    expireWatchTicks++
    if (expireWatchTicks > EXPIRE_WATCH_MAX) {
      stopExpireWatch()
      return
    }
    load()
  }, EXPIRE_WATCH_MS)
}

function stopExpireWatch(): void {
  if (expireWatch) {
    clearInterval(expireWatch)
    expireWatch = null
  }
}

function tick(): void {
  const s = Math.max(0, remain.value)
  const m = Math.floor(s / 60)
  const ss = s % 60
  countdown.value = `${String(m).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
  if (s <= 0 && timer) {
    clearInterval(timer)
    timer = null
    startExpireWatch()
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const d = await fetchSelfwashOrder(orderNo.value)
    order.value = d
    if (d.status === 'SERVING') {
      remain.value = d.remainingSeconds || 0
      // 仍有剩余（含刚加时成功）说明未到期，撤掉到期轮询
      if (remain.value > 0) stopExpireWatch()
      if (timer) clearInterval(timer)
      timer = setInterval(() => {
        remain.value = Math.max(0, remain.value - 1)
        tick()
      }, 1000)
      tick()
    } else if (timer) {
      clearInterval(timer)
      timer = null
      stopExpireWatch()
    }
  } catch {
    order.value = null
  } finally {
    loading.value = false
  }
}

function goPay(): void {
  uni.redirectTo({ url: `/pages/selfwash-pay/selfwash-pay?orderNo=${orderNo.value}` })
}

async function doActivate(): Promise<void> {
  try {
    const d = await activateSelfwash(orderNo.value)
    order.value = d
    uni.showToast({ title: d.status === 'QUEUING' ? '已排队' : '已开始，设备通电', icon: 'none' })
    load()
  } catch {
    // 已提示
  }
}

/** 加时档位（分钟） */
const ADD_MINUTES = 10

/** 归零后回拉间隔与最大次数：合计约 30 秒，覆盖后端定时任务周期 */
const EXPIRE_WATCH_MS = 5000
const EXPIRE_WATCH_MAX = 6

/**
 * 加时费用（分）：与后端 overtimeCost 同公式——按后台配置的「超时价 / 超时时长」整档计，不足一档按一档。
 * 过去按「本单实付 / 时长」折算，与后端口径不同，出现过预估 ¥6.70 实扣 ¥8.00。
 */
function addTimeCost(): number {
  const p = pricing.value
  if (!p || !p.overtimeMinutes || p.overtimeMinutes <= 0) return 0
  const blocks = Math.ceil(ADD_MINUTES / p.overtimeMinutes)
  return blocks * (p.overtimeAmount ?? 0)
}

const cost = computed(addTimeCost)
const addTimeDesc = computed(() =>
  cost.value > 0
    ? `加时 ${ADD_MINUTES} 分钟，将从储值余额扣除 ¥${fen2yuan(cost.value)}，倒计时相应延长。`
    : `加时 ${ADD_MINUTES} 分钟，倒计时相应延长。`,
)
const addTimeBtn = computed(() => (cost.value > 0 ? `确认支付 ¥${fen2yuan(cost.value)}` : '确认'))

function doAddTime(): void {
  addTimeDlg.value = true
}

async function confirmAddTime(): Promise<void> {
  if (submitting.value) return
  submitting.value = true
  try {
    const d = await addSelfwashTime(orderNo.value, ADD_MINUTES)
    order.value = d
    remain.value = d.remainingSeconds || 0
    addTimeDlg.value = false
    tick()
    uni.showToast({ title: '已加时', icon: 'none' })
  } catch {
    // 已提示（余额不足等由 request 层统一提示）
  } finally {
    submitting.value = false
  }
}

async function doFinish(): Promise<void> {
  uni.showModal({
    title: '结束服务',
    content: '结束后设备断电，订单完成，确定结束？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await finishSelfwash(orderNo.value)
        uni.showToast({ title: '已结束', icon: 'none' })
        load()
      } catch {
        // 已提示
      }
    },
  })
}

function doCancel(): void {
  uni.showModal({
    title: '取消订单',
    content: '取消后不可恢复，已支付将原路退回',
    editable: true,
    placeholderText: '请填写取消原因',
    success: async (res) => {
      if (!res.confirm) return
      const reason = (res.content ?? '').trim()
      if (!reason) {
        uni.showToast({ title: '请填写取消原因', icon: 'none' })
        return
      }
      try {
        await cancelSelfwash(orderNo.value, reason)
        uni.showToast({ title: '已取消', icon: 'none' })
        load()
      } catch {
        // 已提示
      }
    },
  })
}

onLoad((opts) => {
  orderNo.value = (opts as { orderNo?: string }).orderNo ?? ''
  if (orderNo.value) load()
  else loading.value = false
  // 计价参数拉不到不影响下单/加时，只是预估金额退化成不展示
  fetchSelfwashPricing()
    .then((p) => {
      pricing.value = p
    })
    .catch(() => {})
})

onUnload(() => {
  if (timer) clearInterval(timer)
  stopExpireWatch()
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
  padding: 80rpx 0;
}

.status-bar {
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 36rpx 30rpx;
  margin-bottom: 20rpx;
  color: #fff;
}

.status {
  font-size: 36rpx;
  font-weight: 700;
}

.countdown {
  margin-top: 16rpx;
}

.cd-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}

.cd-time {
  display: block;
  margin-top: 6rpx;
  font-size: 56rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}

.queue-tip {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.95);
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

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 0;
}

.label {
  font-size: 26rpx;
  color: #8a9a98;
}

.value {
  font-size: 26rpx;
  color: #1b2b2a;
}

.value.price {
  color: #ff5b4a;
  font-weight: 700;
}

.value.discount {
  color: #ff5b4a;
}

.ops {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.op {
  flex: 1;
  min-width: 200rpx;
  text-align: center;
  background: #14342f;
  color: #fff;
  font-size: 29rpx;
  font-weight: 600;
  padding: 24rpx 0;
  border-radius: 40rpx;
}

.op.primary {
  background: #00aeb5;
}

.op.plain {
  background: #eef3f2;
  color: #6b7b79;
}

.op.danger {
  background: #ffeeeb;
  color: #ff5b4a;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9998;
}

.dialog {
  position: fixed;
  left: 70rpx;
  right: 70rpx;
  top: 50%;
  transform: translateY(-50%);
  z-index: 9999;
  background: #fff;
  border-radius: 24rpx;
  padding: 36rpx 32rpx;
}

.dlg-title {
  display: block;
  text-align: center;
  font-size: 32rpx;
  font-weight: 700;
  color: #1b2b2a;
  margin-bottom: 20rpx;
}

.dlg-desc {
  display: block;
  font-size: 26rpx;
  color: #6b7b79;
  line-height: 42rpx;
}

.dlg-btns {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}

.dlg-btn {
  flex: 1;
  background: #14342f;
  color: #fff;
  text-align: center;
  border-radius: 40rpx;
  padding: 22rpx 0;
  font-size: 29rpx;
  font-weight: 600;
}

.dlg-btn.plain {
  background: #eef3f2;
  color: #6b7b79;
}

.dlg-btn.disabled {
  opacity: 0.6;
}
</style>

<template>
  <view class="page">
    <view class="head">
      <view>
        <text class="who">{{ queue?.stationName || '中央站' }}</text>
        <text class="site">在洗 {{ queue?.washingCount ?? 0 }} · 待质检 {{ queue?.qcCount ?? 0 }}</text>
      </view>
      <text class="logout" @click="doLogout">退出</text>
    </view>

    <view v-if="!items.length" class="empty">当前没有作业中的车辆</view>

    <view v-for="it in items" :key="it.orderNo" class="card">
      <view class="row">
        <text class="plate">{{ it.orderNo }}</text>
        <text class="status">{{ stageLabel(it.status) }}</text>
      </view>
      <view class="line sub">{{ it.serviceName || '洗车服务' }}</view>

      <view class="ops">
        <text v-if="it.status === 'TO_STATION'" class="op" @click="doArrive(it.orderNo)">入场打卡</text>
        <text v-if="it.status === 'WASHING'" class="op" @click="doSopDone(it.orderNo)">SOP 完成</text>
        <template v-if="it.status === 'QC'">
          <text class="op" @click="doQcPass(it.orderNo)">质检通过</text>
          <text class="op danger" @click="doQcFail(it.orderNo)">不合格</text>
        </template>
        <text v-if="it.status === 'WAIT_RETURN'" class="op" @click="doLeave(it.orderNo)">驶离站点</text>
      </view>
    </view>

    <WorkerTabBar current="station" />
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import WorkerTabBar from '@/components/WorkerTabBar.vue'
import { uploadMedia, MAX_UPLOAD_MB } from '@/api/media'
import {
  arriveStation,
  fetchStationQueue,
  leaveStation,
  qcFail,
  qcPass,
  sopDone,
  type StationQueueVO,
} from '@/api/worker'
import { logoutWorker, restoreWorker } from '@/store/worker'

interface QueueItem {
  orderNo: string
  serviceName?: string
  status?: string
}

const queue = ref<StationQueueVO | null>(null)

onShow(() => {
  if (!restoreWorker()) {
    uni.redirectTo({ url: '/pages/worker-login/worker-login' })
    return
  }
  load()
})

/** bays（在洗）+ waiting（待入场 / 待质检 / 待还车）合成一个列表，按 status 给动作 */
const items = computed<QueueItem[]>(() => {
  if (!queue.value) return []
  const bays = (queue.value.bays ?? []).map((b) => ({
    orderNo: b.orderNo ?? '',
    serviceName: undefined,
    status: b.status,
  }))
  const waiting = (queue.value.waiting ?? []).map((w) => ({
    orderNo: w.orderNo ?? '',
    serviceName: w.serviceName,
    status: w.status,
  }))
  return [...bays, ...waiting] as QueueItem[]
})

async function load(): Promise<void> {
  try {
    queue.value = await fetchStationQueue()
  } catch {
    // 失败已有统一 toast
  }
}

function stageLabel(status?: string): string {
  const map: Record<string, string> = {
    TO_STATION: '待入场',
    WASHING: '清洗中',
    QC: '待质检',
    WAIT_RETURN: '待还车',
  }
  return status ? map[status] ?? status : '待处理'
}

async function doArrive(orderNo: string): Promise<void> {
  await guard(async () => {
    await arriveStation(orderNo)
    toast('已入场')
  })
}

async function doSopDone(orderNo: string): Promise<void> {
  await guard(async () => {
    await sopDone(orderNo)
    toast('SOP 已完成')
  })
}

async function doQcPass(orderNo: string): Promise<void> {
  await guard(async () => {
    const res = await new Promise<UniApp.ChooseMediaSuccessCallbackResult>((resolve, reject) => {
      uni.chooseMedia({ count: 2, mediaType: ['image'], sizeType: ['compressed'], success: resolve, fail: reject })
    }).catch(() => null)
    if (!res || !res.tempFiles?.length) return
    const ids: string[] = []
    for (const f of res.tempFiles) {
      if ((f.size ?? 0) > MAX_UPLOAD_MB * 1024 * 1024) {
        uni.showToast({ title: `图片不能超过 ${MAX_UPLOAD_MB}MB，已跳过`, icon: 'none' })
        continue
      }
      try {
        const media = await uploadMedia(f.tempFilePath, 'WASHED')
        if (media.fileId) ids.push(media.fileId)
      } catch (e) {
        uni.showToast({ title: (e as { msg?: string }).msg || '上传失败', icon: 'none' })
      }
    }
    if (!ids.length) return
    await qcPass(orderNo, ids)
    toast('质检通过')
  })
}

async function doQcFail(orderNo: string): Promise<void> {
  await guard(async () => {
    const reason = await prompt('质检不合格', '请填写不合格原因')
    if (!reason) return
    await qcFail(orderNo, reason)
    toast('已打回返工')
  })
}

async function doLeave(orderNo: string): Promise<void> {
  await guard(async () => {
    await leaveStation(orderNo)
    toast('已驶离站点')
  })
}

function prompt(title: string, placeholder: string): Promise<string | null> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      placeholderText: placeholder,
      editable: true,
      success: (r) => resolve(r.confirm && r.content ? r.content.trim() : null),
      fail: () => resolve(null),
    })
  })
}

function toast(title: string): void {
  uni.showToast({ title, icon: 'none' })
  load()
}

async function guard(fn: () => Promise<void>): Promise<void> {
  try {
    await fn()
  } catch {
    // 业务错误码已由 request 统一 toast
  }
}

function doLogout(): void {
  logoutWorker()
  uni.redirectTo({ url: '/pages/worker-login/worker-login' })
}
</script>

<style>
.page {
  padding: 24rpx 24rpx 140rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.who {
  font-size: 34rpx;
  font-weight: 700;
  color: #222;
}

.site {
  margin-left: 12rpx;
  font-size: 24rpx;
  color: #888;
}

.logout {
  font-size: 26rpx;
  color: #d9534f;
}

.empty {
  text-align: center;
  color: #aaa;
  padding: 80rpx 0;
  font-size: 28rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx;
  margin-bottom: 20rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plate {
  font-size: 30rpx;
  font-weight: 700;
  color: #222;
}

.status {
  font-size: 24rpx;
  color: #1a73e8;
}

.line.sub {
  margin-top: 10rpx;
  color: #999;
  font-size: 24rpx;
}

.ops {
  margin-top: 20rpx;
  display: flex;
}

.op {
  background: #1a73e8;
  color: #fff;
  padding: 14rpx 32rpx;
  border-radius: 10rpx;
  font-size: 28rpx;
  margin-right: 16rpx;
}

.op.danger {
  background: #d9534f;
}
</style>

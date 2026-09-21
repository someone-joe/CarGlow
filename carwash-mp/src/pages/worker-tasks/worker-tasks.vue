<template>
  <view class="page">
    <view class="head">
      <view>
        <text class="who">{{ workerState.name || '师傅' }}</text>
        <text class="site">{{ workerState.siteName || '未绑定站点' }}</text>
      </view>
      <text class="logout" @click="doLogout">退出</text>
    </view>

    <view class="tabs">
      <text :class="['tab', stage === 'PICKUP' && 'on']" @click="switchStage('PICKUP')">待取车</text>
      <text :class="['tab', stage === 'RETURN' && 'on']" @click="switchStage('RETURN')">待送回</text>
      <text :class="['tab', stage === 'ALL' && 'on']" @click="switchStage('ALL')">全部</text>
    </view>

    <view v-if="!tasks.length" class="empty">暂无任务</view>

    <view v-for="t in tasks" :key="t.orderNo" class="card">
      <view class="row">
        <text class="plate">{{ t.vehiclePlate || '—' }}</text>
        <text class="status">{{ t.statusLabel || t.status }}</text>
      </view>
      <view class="line">{{ t.serviceName || '洗车服务' }} · 预约 {{ t.appointmentDate || '—' }}</view>
      <view class="line sub">单号 {{ t.orderNo }}</view>
      <view class="line sub" v-if="t.requiredPhotoCount">取车需拍 ≥{{ t.requiredPhotoCount }} 张</view>

      <view v-if="photoList(t).length" class="photos">
        <text class="photos-title">用户停车照（找车用）</text>
        <view class="photos-row">
          <image
            v-for="(p, i) in photoList(t)"
            :key="p.fileId"
            class="photo"
            :src="photoUrl(p.url)"
            mode="aspectFill"
            @click.stop="preview(photoList(t), i)"
          />
        </view>
      </view>

      <view class="ops">
        <text v-if="t.status === 'KEY_IN'" class="op" @click="doTakeKey(t.orderNo)">取钥匙</text>
        <text v-if="t.status === 'PICKING'" class="op" @click="doPickCarDone(t.orderNo, t.requiredPhotoCount ?? 6)">取车拍照</text>
        <text v-if="t.status === 'RETURNING'" class="op" @click="doReturnDone(t.orderNo)">送回归柜</text>
      </view>
    </view>

    <WorkerTabBar current="tasks" />
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import WorkerTabBar from '@/components/WorkerTabBar.vue'
import { uploadMedia, type MediaVO } from '@/api/media'
import { fetchPickTasks, pickCarDone, returnDone, takeKey, type PickStage, type PickTaskVO } from '@/api/worker'
import { logoutWorker, restoreWorker, workerState } from '@/store/worker'
import { BASE_URL } from '@/utils/request'

const stage = ref<PickStage>('ALL')
const tasks = ref<PickTaskVO[]>([])

onShow(() => {
  if (!restoreWorker()) {
    uni.redirectTo({ url: '/pages/worker-login/worker-login' })
    return
  }
  load()
})

function switchStage(next: PickStage): void {
  stage.value = next
  load()
}

async function load(): Promise<void> {
  try {
    tasks.value = await fetchPickTasks(stage.value)
  } catch {
    // 失败已有统一 toast（10002 会自动跳登录页）
  }
}

async function doTakeKey(orderNo?: string): Promise<void> {
  if (!orderNo) return
  await guard(async () => {
    await takeKey(orderNo)
    uni.showToast({ title: '已取钥匙', icon: 'none' })
    await load()
  })
}

async function doPickCarDone(orderNo?: string, min: number = 6): Promise<void> {
  if (!orderNo) return
  await guard(async () => {
    const fileIds = await choosePhotos(min)
    if (!fileIds) return
    await pickCarDone(orderNo, fileIds)
    uni.showToast({ title: '取车拍照已提交', icon: 'none' })
    await load()
  })
}

async function doReturnDone(orderNo?: string): Promise<void> {
  if (!orderNo) return
  await guard(async () => {
    const parkingNo = await prompt('车位号', '请填写停放车位号')
    if (!parkingNo) return
    const fileIds = await choosePhotos(1)
    if (!fileIds) return
    await returnDone(orderNo, fileIds, parkingNo)
    uni.showToast({ title: '已送回归柜', icon: 'none' })
    await load()
  })
}

/** 用户停车照（后端已按 PARK 过滤好），无照片返回空数组，模板据此不渲染 */
function photoList(task: PickTaskVO): MediaVO[] {
  return task.parkPhotos ?? []
}

/** 影像 url 是相对路径，需拼后端基址；已是绝对地址则不重复拼 */
function photoUrl(url?: string): string {
  if (!url) return ''
  return url.startsWith('http') ? url : BASE_URL + url
}

/** 点图放大预览：师傅在车位现场看停车照找车 */
function preview(photos: MediaVO[], index: number): void {
  uni.previewImage({ urls: photos.map((p) => photoUrl(p.url)), current: index })
}

/** 选图并上传，返回 fileId 列表；用户取消返回 null */
async function choosePhotos(count: number): Promise<string[] | null> {
  const res = await new Promise<UniApp.ChooseMediaSuccessCallbackResult>((resolve, reject) => {
    uni.chooseMedia({ count, mediaType: ['image'], sizeType: ['compressed'], success: resolve, fail: reject })
  }).catch(() => null)
  if (!res || !res.tempFiles?.length) {
    return null
  }
  const ids: string[] = []
  for (const f of res.tempFiles) {
    const media = await uploadMedia(f.tempFilePath, 'PICK')
    if (media.fileId) ids.push(media.fileId)
  }
  return ids
}

/** 弹窗输入（车位号 / 原因） */
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

/** 统一兜底：业务错误码已由 request 弹 toast，这里只吞掉异常避免 unhandledrejection */
async function guard(fn: () => Promise<void>): Promise<void> {
  try {
    await fn()
  } catch {
    // noop
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

.tabs {
  display: flex;
  background: #fff;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  font-size: 28rpx;
  color: #666;
}

.tab.on {
  color: #1a73e8;
  font-weight: 600;
  border-bottom: 4rpx solid #1a73e8;
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
  font-size: 34rpx;
  font-weight: 700;
  color: #222;
}

.status {
  font-size: 24rpx;
  color: #1a73e8;
}

.line {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #555;
}

.line.sub {
  color: #999;
  font-size: 24rpx;
}

.photos {
  margin-top: 18rpx;
}

.photos-title {
  font-size: 24rpx;
  color: #888;
}

.photos-row {
  display: flex;
  margin-top: 12rpx;
}

.photo {
  width: 140rpx;
  height: 140rpx;
  margin-right: 12rpx;
  border-radius: 10rpx;
  background: #f0f0f0;
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
}
</style>

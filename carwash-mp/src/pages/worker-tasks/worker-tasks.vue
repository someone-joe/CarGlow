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
      <!-- 按阶段给提示：取车阶段才谈「≥6 张」，送回阶段说的是归柜停车照（bug092407） -->
      <view class="line sub" v-if="t.status === 'PICKING' && t.requiredPhotoCount">取车需拍 ≥{{ t.requiredPhotoCount }} 张</view>
      <view class="line sub" v-else-if="t.status === 'RETURNING'">给车主拍张停车照便于找车</view>

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

      <!-- 已拍取车照：分批上传后可预览、可长按删除 -->
      <view v-if="t.status === 'PICKING'" class="photos">
        <text class="photos-title">
          已拍取车照 {{ draftList(t.orderNo).length }}/{{ t.requiredPhotoCount ?? 6 }}（点开看大图，长按删除）
        </text>
        <view class="photos-row">
          <image
            v-for="(p, i) in draftList(t.orderNo)"
            :key="p.fileId"
            class="photo"
            :src="photoUrl(p.url)"
            mode="aspectFill"
            @click.stop="preview(draftList(t.orderNo), i)"
            @longpress.stop="removeDraft(t.orderNo, i)"
          />
        </view>
      </view>

      <view class="ops">
        <text v-if="t.status === 'KEY_IN'" class="op" @click="doTakeKey(t.orderNo)">取钥匙</text>
        <template v-if="t.status === 'PICKING'">
          <text
            :class="['op', draftList(t.orderNo).length >= (t.requiredPhotoCount ?? 6) && 'op-disabled']"
            @click="addPhotos(t.orderNo, t.requiredPhotoCount ?? 6)"
          >拍照/选图</text>
          <text class="op" @click="submitPickCarDone(t.orderNo, t.requiredPhotoCount ?? 6)">完成取车</text>
        </template>
        <text v-if="t.status === 'RETURNING'" class="op" @click="doReturnDone(t.orderNo)">拍照归柜</text>
      </view>
    </view>

    <WorkerTabBar current="tasks" />
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import WorkerTabBar from '@/components/WorkerTabBar.vue'
import { uploadMedia, type MediaVO, MAX_UPLOAD_MB } from '@/api/media'
import { fetchPickTasks, pickCarDone, returnDone, takeKey, type PickStage, type PickTaskVO } from '@/api/worker'
import { logoutWorker, restoreWorker, workerState } from '@/store/worker'
import { BASE_URL, getToken } from '@/utils/request'

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

/**
 * 已拍摄并上传成功、但还未提交的取车照（按订单分组）。
 *
 * 为什么要分批：契约只要求「提交时 ≥6 张」（minItems: 6，是业务红线），
 * 上传接口本身没有数量限制。若强制一次选满 6 张，师傅现场拍到一半退出就前功尽弃，
 * 也拿不到预览。故改为「随时拍随时传，凑够再提交」。
 */
const drafts = ref<Record<string, MediaVO[]>>({})

function draftList(orderNo?: string): MediaVO[] {
  return orderNo ? drafts.value[orderNo] ?? [] : []
}

/**
 * 选图（可只选 1 张）并立即上传；上传后即可在卡片里预览。
 * 数量闸门（bug092202）：count 设为剩余可拍数，从源头选不了超量；
 * 选中数超剩余时拦截提示；已拍满时入口置灰（见模板）。
 */
async function addPhotos(orderNo?: string, required: number = 6): Promise<void> {
  if (!orderNo) return
  await guard(async () => {
    const existing = draftList(orderNo)
    const remain = required - existing.length
    if (remain <= 0) {
      uni.showToast({ title: `已拍满 ${required} 张，长按照片可删除重拍`, icon: 'none' })
      return
    }
    const res = await new Promise<UniApp.ChooseMediaSuccessCallbackResult>((resolve, reject) => {
      uni.chooseMedia({ count: remain, mediaType: ['image'], sizeType: ['compressed'], success: resolve, fail: reject })
    }).catch(() => null)
    if (!res || !res.tempFiles?.length) return
    if (existing.length + res.tempFiles.length > required) {
      uni.showToast({ title: `最多还能拍 ${remain} 张`, icon: 'none' })
      return
    }
    const next = existing.slice()
    for (const f of res.tempFiles) {
      if ((f.size ?? 0) > MAX_UPLOAD_MB * 1024 * 1024) {
        uni.showToast({ title: `图片不能超过 ${MAX_UPLOAD_MB}MB，已跳过`, icon: 'none' })
        continue
      }
      try {
        const media = await uploadMedia(f.tempFilePath, 'PICK', orderNo)
        if (media.fileId) next.push(media)
      } catch (e) {
        // 被后端拒绝（如超限）必须把原因亮出来，否则用户以为"没反应"
        uni.showToast({ title: (e as { msg?: string }).msg || '上传失败', icon: 'none' })
      }
    }
    drafts.value = { ...drafts.value, [orderNo]: next }
  })
}

function removeDraft(orderNo?: string, index: number = 0): void {
  if (!orderNo) return
  const list = draftList(orderNo).slice()
  list.splice(index, 1)
  drafts.value = { ...drafts.value, [orderNo]: list }
}

/** 提交取车完成：不足时前端先拦并提示还差几张，不让用户白跑一趟后端 */
async function submitPickCarDone(orderNo?: string, required: number = 6): Promise<void> {
  if (!orderNo) return
  await guard(async () => {
    const ids = draftList(orderNo)
      .map((p) => p.fileId)
      .filter((id): id is string => !!id)
    if (ids.length < required) {
      uni.showToast({ title: `还需 ${required - ids.length} 张照片`, icon: 'none' })
      return
    }
    await pickCarDone(orderNo, ids)
    const next = { ...drafts.value }
    delete next[orderNo]
    drafts.value = next
    uni.showToast({ title: '取车拍照已提交', icon: 'none' })
    await load()
  })
}

async function doReturnDone(orderNo?: string): Promise<void> {
  if (!orderNo) return
  await guard(async () => {
    // 拍照归柜（bug092407）：先弹拍照/相册选最多 3 张，再登记车位号
    const fileIds = await choosePhotos(3)
    if (!fileIds) return
    const parkingNo = await prompt('车位号', '请填写停放车位号')
    if (!parkingNo) return
    await returnDone(orderNo, fileIds, parkingNo)
    uni.showToast({ title: '已拍照归柜', icon: 'none' })
    await load()
  })
}

/** 用户停车照（后端已按 PARK 过滤好），无照片返回空数组，模板据此不渲染 */
function photoList(task: PickTaskVO): MediaVO[] {
  return task.parkPhotos ?? []
}

/**
 * 影像 url 是相对路径，需拼后端基址；已是绝对地址则不重复拼。
 * image / previewImage 组件带不了 Authorization 头，鉴权走 query token
 * （后端 MediaAuthInterceptor 支持，bug092201）。
 */
function photoUrl(url?: string): string {
  if (!url) return ''
  const full = url.startsWith('http') ? url : BASE_URL + url
  const token = getToken()
  return token ? `${full}${full.includes('?') ? '&' : '?'}token=${token}` : full
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

.op-disabled {
  opacity: 0.5;
}
</style>

<template>
  <view class="page">
    <view class="head">
      <view>
        <text class="who">{{ workerState.name || '师傅' }}</text>
        <text class="site">{{ workerState.siteName || '未绑定站点' }}</text>
      </view>
      <text class="logout" @click="doLogout">退出</text>
    </view>

    <!-- 打卡上下班 -->
    <view class="card">
      <view class="card-title">上班状态</view>
      <view class="status-row">
        <text class="status">{{ workLabel }}</text>
        <text class="hint">仅 ONLINE / BUSY 才会被派单</text>
      </view>
      <view class="clock-ops">
        <text :class="['cop', profile?.workStatus === 'ONLINE' && 'on']" @click="clock('ONLINE')">上班</text>
        <text :class="['cop', profile?.workStatus === 'BUSY' && 'on']" @click="clock('BUSY')">忙碌</text>
        <text :class="['cop', profile?.workStatus === 'OFFLINE' && 'on']" @click="clock('OFFLINE')">下班</text>
      </view>
    </view>

    <!-- 资质 -->
    <view class="card">
      <view class="card-title">接单资质</view>
      <view class="kv">
        <text>人脸核身</text>
        <text :class="['tag', faceClass]">{{ faceLabel }}</text>
      </view>
      <view class="kv">
        <text>后台审核</text>
        <text :class="['tag', reviewClass]">{{ reviewLabel }}</text>
      </view>
      <view class="kv">
        <text>今日在班</text>
        <text>{{ profile?.onDutyToday ? '是' : '否（未排班或不在班）' }}</text>
      </view>
      <view class="kv">
        <text>进行中订单</text>
        <text>{{ profile?.activeOrderCount ?? 0 }} 单</text>
      </view>

      <block v-if="canSubmit">
        <view class="form">
          <view class="field">
            <text class="label">姓名</text>
            <input v-model="form.idName" class="input" placeholder="身份证姓名" />
          </view>
          <view class="field">
            <text class="label">身份证号</text>
            <input v-model="form.idNo" class="input" placeholder="身份证号" />
          </view>
          <view class="upload">
            <text class="label">身份证正面</text>
            <image v-if="form.idFrontFile" class="up-img" :src="fileUrl(form.idFrontFile)" mode="aspectFill" @click="upload('idFrontFile')" />
            <view v-else class="up-box" @click="upload('idFrontFile')"><text>＋</text></view>
          </view>
          <view class="upload">
            <text class="label">身份证反面</text>
            <image v-if="form.idBackFile" class="up-img" :src="fileUrl(form.idBackFile)" mode="aspectFill" @click="upload('idBackFile')" />
            <view v-else class="up-box" @click="upload('idBackFile')"><text>＋</text></view>
          </view>
          <view class="upload">
            <text class="label">驾照（选填）</text>
            <image v-if="form.licenseFile" class="up-img" :src="fileUrl(form.licenseFile)" mode="aspectFill" @click="upload('licenseFile')" />
            <view v-else class="up-box" @click="upload('licenseFile')"><text>＋</text></view>
          </view>
        </view>
        <text class="submit" @click="submit">提交资质</text>
      </block>
      <text v-else-if="reviewLabel === '已驳回'" class="rejected">驳回原因：{{ qual?.reviewRemark || '—' }}</text>
    </view>

    <WorkerTabBar current="mine" />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import WorkerTabBar from '@/components/WorkerTabBar.vue'
import {
  fetchWorkerProfile, setWorkerWorkStatus, fetchMyQualification, submitQualification,
  type WorkerProfileVO, type WorkerQualificationVO,
} from '@/api/worker'
import { uploadWorkerMedia, workerMediaUrl, MAX_UPLOAD_MB } from '@/api/media'
import { logoutWorker, restoreWorker, workerState } from '@/store/worker'

const profile = ref<WorkerProfileVO | null>(null)
const qual = ref<WorkerQualificationVO | null>(null)

const form = reactive({ idName: '', idNo: '', idFrontFile: '', idBackFile: '', licenseFile: '' })

const workLabel = computed(() => {
  const w = profile.value?.workStatus
  return w === 'ONLINE' ? '上班' : w === 'BUSY' ? '忙碌' : w === 'OFFLINE' ? '下班' : '—'
})
const faceLabel = computed(() => {
  const f = qual.value?.faceStatus
  return f === 'PASSED' ? '已通过' : f === 'FAILED' ? '未通过' : '待核身'
})
const reviewLabel = computed(() => {
  const r = qual.value?.reviewStatus
  return r === 'PASSED' ? '已通过' : r === 'REJECTED' ? '已驳回' : '待审核'
})
const faceClass = computed(() => faceLabel.value === '已通过' ? 'ok' : faceLabel.value === '未通过' ? 'no' : 'wait')
const reviewClass = computed(() => reviewLabel.value === '已通过' ? 'ok' : reviewLabel.value === '已驳回' ? 'no' : 'wait')

/** 未提交 / 被驳回 / 待审中 都允许提交或重新提交 */
const canSubmit = computed(() => {
  const r = qual.value?.reviewStatus
  return !qual.value?.submitted || r === 'REJECTED' || r === 'PENDING'
})

onShow(() => {
  if (!restoreWorker()) {
    uni.redirectTo({ url: '/pages/worker-login/worker-login' })
    return
  }
  load()
})

async function load(): Promise<void> {
  try {
    profile.value = await fetchWorkerProfile()
    qual.value = await fetchMyQualification()
    if (qual.value?.submitted && qual.value.idName) {
      form.idName = qual.value.idName
      form.idNo = qual.value.idNo ?? ''
      form.idFrontFile = qual.value.idFrontFile ?? ''
      form.idBackFile = qual.value.idBackFile ?? ''
      form.licenseFile = qual.value.licenseFile ?? ''
    }
  } catch {
    // 统一 toast
  }
}

async function clock(status: 'ONLINE' | 'BUSY' | 'OFFLINE'): Promise<void> {
  await guard(async () => {
    await setWorkerWorkStatus(status)
    uni.showToast({ title: '已更新', icon: 'none' })
    await load()
  })
}

async function upload(field: 'idFrontFile' | 'idBackFile' | 'licenseFile'): Promise<void> {
  await guard(async () => {
    const res = await new Promise<UniApp.ChooseMediaSuccessCallbackResult>((resolve, reject) => {
      uni.chooseMedia({ count: 1, mediaType: ['image'], sizeType: ['compressed'], success: resolve, fail: reject })
    }).catch(() => null)
    if (!res?.tempFiles?.length) return
    const f = res.tempFiles[0]
    if ((f.size ?? 0) > MAX_UPLOAD_MB * 1024 * 1024) {
      uni.showToast({ title: `图片不能超过 ${MAX_UPLOAD_MB}MB`, icon: 'none' })
      return
    }
    const media = await uploadWorkerMedia(f.tempFilePath, 'WORKER_QUAL')
    if (media.fileId) (form as Record<string, string>)[field] = media.fileId
  })
}

function fileUrl(fileId: string): string {
  return workerMediaUrl(fileId)
}

async function submit(): Promise<void> {
  if (!form.idName.trim() || !form.idNo.trim() || !form.idFrontFile || !form.idBackFile) {
    uni.showToast({ title: '请填写姓名、身份证号并上传正反面', icon: 'none' })
    return
  }
  await guard(async () => {
    await submitQualification({
      idName: form.idName.trim(),
      idNo: form.idNo.trim(),
      idFrontFile: form.idFrontFile,
      idBackFile: form.idBackFile,
      licenseFile: form.licenseFile || undefined,
    })
    uni.showToast({ title: '已提交，等待审核', icon: 'none' })
    await load()
  })
}

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

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx;
  margin-bottom: 20rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 18rpx;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status {
  font-size: 30rpx;
  font-weight: 600;
  color: #1a73e8;
}

.hint {
  font-size: 22rpx;
  color: #999;
}

.clock-ops {
  display: flex;
  margin-top: 18rpx;
}

.cop {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  margin-right: 12rpx;
  border-radius: 10rpx;
  background: #f0f2f5;
  color: #555;
  font-size: 28rpx;
}

.cop:last-child {
  margin-right: 0;
}

.cop.on {
  background: #1a73e8;
  color: #fff;
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  font-size: 28rpx;
  color: #555;
}

.tag {
  font-size: 26rpx;
}

.tag.ok {
  color: #18a058;
}

.tag.no {
  color: #d9534f;
}

.tag.wait {
  color: #e6a23c;
}

.form {
  margin-top: 12rpx;
}

.field {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.label {
  width: 170rpx;
  font-size: 26rpx;
  color: #666;
}

.input {
  flex: 1;
  border: 1rpx solid #eee;
  border-radius: 8rpx;
  padding: 14rpx;
  font-size: 26rpx;
}

.upload {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.up-box {
  width: 120rpx;
  height: 120rpx;
  border: 1rpx dashed #ccc;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 48rpx;
}

.up-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 10rpx;
  background: #f0f0f0;
}

.submit {
  display: block;
  text-align: center;
  background: #1a73e8;
  color: #fff;
  padding: 20rpx 0;
  border-radius: 10rpx;
  font-size: 30rpx;
  margin-top: 10rpx;
}

.rejected {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #d9534f;
}
</style>

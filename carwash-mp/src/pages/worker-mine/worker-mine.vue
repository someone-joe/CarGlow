<template>
  <view class="cg-page worker-mine">
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
/* 底部 WorkerTabBar 占位 */
.worker-mine {
  padding-bottom: calc(220rpx + env(safe-area-inset-bottom));
}

/* ---- 头部 ---- */
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.who {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.site {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.logout {
  min-height: 96rpx;
  padding: 0 8rpx;
  color: var(--cg-error);
  font-size: var(--cg-font-caption);
  line-height: 96rpx;
}

/* ---- 卡片 ---- */
.card {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.card-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

/* ---- 打卡 ---- */
.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status {
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-primary);
}

.hint {
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.clock-ops {
  display: flex;
  margin-top: 32rpx;
}

.cop {
  flex: 1;
  height: 88rpx;
  margin-right: 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
  line-height: 88rpx;
  text-align: center;
}

.cop:last-child {
  margin-right: 0;
}

.cop.on {
  background: var(--cg-primary);
  color: #ffffff;
}

/* ---- 资质键值行 ---- */
.kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 88rpx;
  padding: 16rpx 0;
  border-top: 1rpx solid var(--cg-line);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.tag {
  padding: 8rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.tag.ok {
  background: rgba(52, 199, 89, 0.12);
  color: var(--cg-success);
}

.tag.no {
  background: rgba(255, 59, 48, 0.10);
  color: var(--cg-error);
}

.tag.wait {
  background: rgba(255, 149, 0, 0.12);
  color: var(--cg-warning);
}

/* ---- 表单 ---- */
.form {
  margin-top: 32rpx;
}

.field {
  margin-top: 24rpx;
}

.label {
  display: block;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.input {
  box-sizing: border-box;
  width: 100%;
  margin-top: 16rpx;
  padding: 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.upload {
  margin-top: 32rpx;
}

.up-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200rpx;
  height: 200rpx;
  margin-top: 16rpx;
  background: var(--cg-fill);
  border: 1rpx dashed var(--cg-border);
  border-radius: var(--cg-radius-md);
  color: var(--cg-text-aux);
  font-size: var(--cg-font-subtitle);
}

.up-img {
  display: block;
  width: 200rpx;
  height: 200rpx;
  margin-top: 16rpx;
  border-radius: var(--cg-radius-md);
}

.submit {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 40rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.rejected {
  margin-top: 24rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-error);
  line-height: 1.6;
}
</style>

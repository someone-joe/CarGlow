<template>
  <view class="cg-page park-location">
    <!-- 新增 / 编辑表单 -->
    <view class="panel">
      <text class="panel-title">{{ editingId ? '编辑停车位置' : '添加停车位置' }}</text>

      <view class="picker-row" @click="pickCommunity">
        <text class="picker-label">所在小区</text>
        <text class="picker-value" :class="{ placeholder: !form.communityId }">
          {{ form.communityName || '请选择小区（必填）' }}
        </text>
        <text class="arrow">›</text>
      </view>

      <input v-model="form.building" class="input" placeholder="楼栋 / 单元号，如 3 栋 2 单元" />
      <input v-model="form.parkingNo" class="input" placeholder="车位号，如 B2 区 A 柱旁 / A 区 23 号" />
      <input v-model="form.detail" class="input" placeholder="具体位置补充（选填），便于师傅找车" />

      <view class="switch-row" @click="form.isDefault = form.isDefault === 'Y' ? 'N' : 'Y'">
        <text class="switch-label">设为默认位置</text>
        <view class="switch-box" :class="{ on: form.isDefault === 'Y' }">
          <view class="switch-dot" />
        </view>
      </view>

      <view class="btn-row">
        <view class="btn btn-primary" @click="submit">
          {{ submitting ? '保存中…' : editingId ? '保存' : '添加' }}
        </view>
        <view v-if="editingId" class="btn btn-plain" @click="resetForm">取消编辑</view>
      </view>
    </view>

    <!-- 位置列表 -->
    <view class="panel">
      <text class="panel-title">我的停车位置（{{ list.length }}）</text>
      <view v-if="loading" class="cg-empty">加载中…</view>
      <view v-else-if="!list.length" class="cg-empty">还没有停车位置，先添加一个吧</view>

      <view v-for="item in list" :key="item.locationId" class="loc">
        <view class="loc-top">
          <text class="loc-name">{{ item.communityName || '未命名小区' }}</text>
          <text v-if="item.isDefault === 'Y'" class="tag">默认</text>
        </view>
        <text class="loc-sub">
          {{ [item.building, item.parkingNo, item.detail].filter(Boolean).join(' · ') || '未填详细位置' }}
        </text>
        <view class="loc-actions">
          <text class="op" @click="edit(item)">编辑</text>
          <text class="op danger" @click="remove(item)">删除</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchCommunities, type CommunityVO } from '@/api/home'
import {
  createParkLocation,
  deleteParkLocation,
  fetchParkLocations,
  updateParkLocation,
  type ParkLocationRequest,
  type ParkLocationVO,
} from '@/api/park'

const list = ref<ParkLocationVO[]>([])
const communities = ref<CommunityVO[]>([])
const loading = ref(true)
const submitting = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = (): ParkLocationRequest => ({
  communityId: 0,
  communityName: '',
  building: '',
  parkingNo: '',
  detail: '',
  isDefault: 'N',
})
const form = ref<ParkLocationRequest>(emptyForm())

async function load(): Promise<void> {
  loading.value = true
  try {
    const [locs, comms] = await Promise.all([fetchParkLocations(), fetchCommunities()])
    list.value = locs || []
    communities.value = comms || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function pickCommunity(): void {
  if (!communities.value.length) {
    uni.showToast({ title: '暂无可选小区', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: communities.value.map((c) => c.name || `小区${c.communityId}`),
    success: (res) => {
      const c = communities.value[res.tapIndex]
      form.value.communityId = c.communityId as number
      form.value.communityName = c.name || ''
    },
  })
}

function edit(item: ParkLocationVO): void {
  editingId.value = item.locationId
  form.value = {
    communityId: item.communityId,
    communityName: item.communityName || '',
    building: item.building || '',
    parkingNo: item.parkingNo || '',
    detail: item.detail || '',
    isDefault: item.isDefault || 'N',
  }
}

function resetForm(): void {
  editingId.value = null
  form.value = emptyForm()
}

async function submit(): Promise<void> {
  if (!form.value.communityId) {
    uni.showToast({ title: '请选择所在小区', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateParkLocation(editingId.value, form.value)
      uni.showToast({ title: '已保存', icon: 'none' })
    } else {
      await createParkLocation(form.value)
      uni.showToast({ title: '已添加', icon: 'none' })
    }
    resetForm()
    load()
  } catch {
    // request 层已提示
  } finally {
    submitting.value = false
  }
}

function remove(item: ParkLocationVO): void {
  uni.showModal({
    title: '删除位置',
    content: `确定删除「${item.communityName || '该位置'}」？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteParkLocation(item.locationId)
        if (editingId.value === item.locationId) resetForm()
        load()
      } catch {
        // request 层已提示
      }
    },
  })
}

onShow(load)
</script>

<style>
/* ---- 表单面板 ---- */
.panel {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.picker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 96rpx;
}

.picker-label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.picker-value {
  flex: 1;
  margin-left: 24rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
  text-align: right;
}

.picker-value.placeholder {
  color: var(--cg-text-aux);
}

.arrow {
  color: var(--cg-text-aux);
  font-size: var(--cg-font-caption);
}

.input {
  box-sizing: border-box;
  width: 100%;
  margin-top: 24rpx;
  padding: 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

/* ---- iOS 开关 ---- */
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 96rpx;
  margin-top: 16rpx;
}

.switch-label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.switch-box {
  box-sizing: border-box;
  width: 96rpx;
  height: 56rpx;
  padding: 6rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  transition: background-color 0.2s ease;
}

.switch-box.on {
  background: var(--cg-primary);
}

.switch-dot {
  width: 44rpx;
  height: 44rpx;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
}

.switch-box.on .switch-dot {
  transform: translateX(40rpx);
}

/* ---- 按钮 ---- */
.btn-row {
  display: flex;
  margin-top: 40rpx;
}

.btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
}

.btn-primary {
  background: var(--cg-primary);
  color: #ffffff;
}

.btn-plain {
  margin-left: 24rpx;
  background: var(--cg-card);
  border: 1rpx solid var(--cg-border);
  color: var(--cg-title);
}

/* ---- 位置列表 ---- */
.loc {
  position: relative;
  padding: 32rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.loc:last-child {
  border-bottom: none;
}

.loc-top {
  display: flex;
  align-items: center;
}

.loc-name {
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.tag {
  margin-left: 24rpx;
  padding: 4rpx 20rpx;
  background: var(--cg-tint);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
}

.loc-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.loc-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
}

.op {
  min-height: 96rpx;
  margin-left: 32rpx;
  padding: 0 8rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-primary);
  line-height: 96rpx;
}

.op.danger {
  color: var(--cg-error);
}
</style>

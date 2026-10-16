<template>
  <view class="page">
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
      <view v-if="loading" class="tip">加载中…</view>
      <view v-else-if="!list.length" class="tip">还没有停车位置，先添加一个吧</view>

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
    items: communities.value.map((c) => c.name || `小区${c.communityId}`),
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
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.panel-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 16rpx;
}

.picker-row {
  display: flex;
  align-items: center;
  padding: 22rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.picker-label {
  font-size: 28rpx;
  color: #1b2b2a;
  width: 160rpx;
}

.picker-value {
  flex: 1;
  font-size: 28rpx;
  color: #1b2b2a;
  text-align: right;
}

.picker-value.placeholder {
  color: #b6c2c0;
}

.input {
  background: #f2f7f7;
  border-radius: 12rpx;
  padding: 22rpx 20rpx;
  font-size: 28rpx;
  margin-bottom: 16rpx;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
}

.switch-label {
  font-size: 28rpx;
  color: #1b2b2a;
}

.switch-box {
  width: 88rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background: #dfe6e5;
  padding: 6rpx;
  box-sizing: border-box;
}

.switch-box.on {
  background: #00aeb5;
}

.switch-dot {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.15s;
}

.switch-box.on .switch-dot {
  transform: translateX(40rpx);
}

.btn-row {
  display: flex;
  gap: 20rpx;
  margin-top: 12rpx;
}

.btn {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  border-radius: 40rpx;
  font-size: 29rpx;
  font-weight: 600;
}

.btn-primary {
  background: #14342f;
  color: #fff;
}

.btn-plain {
  background: #eef3f2;
  color: #6b7b79;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 50rpx 0;
}

.loc {
  padding: 22rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.loc:last-child {
  border-bottom: none;
}

.loc-top {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.loc-name {
  font-size: 29rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.tag {
  font-size: 20rpx;
  color: #00aeb5;
  background: #e6f6f6;
  border-radius: 20rpx;
  padding: 4rpx 14rpx;
}

.loc-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.loc-actions {
  display: flex;
  justify-content: flex-end;
  gap: 28rpx;
  margin-top: 12rpx;
}

.op {
  font-size: 26rpx;
  color: #00aeb5;
}

.op.danger {
  color: #ff5b4a;
}

.arrow {
  color: #b6c2c0;
  font-size: 32rpx;
}
</style>

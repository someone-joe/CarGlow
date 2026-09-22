<template>
  <view class="page">
    <!-- 新增 / 编辑表单 -->
    <view class="panel">
      <text class="panel-title">{{ editingId ? '编辑车辆' : '添加车辆' }}</text>

      <input class="input" v-model="form.plateNo" placeholder="车牌号（必填），支持新能源 8 位" />
      <input class="input" v-model="form.brand" placeholder="品牌型号，如 丰田 凯美瑞" />
      <input class="input" v-model="form.color" placeholder="颜色，如 白色" />
      <input class="input" v-model="form.parkingNo" placeholder="常停车位，如 A 区 23 号" />

      <view class="switch-row" @click="form.isNewEnergy = !form.isNewEnergy">
        <text class="switch-label">新能源车</text>
        <view class="switch-box" :class="{ on: form.isNewEnergy }">
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

    <!-- 车辆列表 -->
    <view class="panel">
      <text class="panel-title">我的车辆（{{ list.length }}）</text>
      <view v-if="loading" class="tip">加载中…</view>
      <view v-else-if="list.length === 0" class="tip">还没有车辆，先添加一辆吧</view>

      <view v-for="item in list" :key="item.vehicleId" class="vehicle">
        <view class="vehicle-top">
          <text class="plate">{{ item.plateNo }}</text>
          <text v-if="item.isDefault" class="tag">默认</text>
        </view>
        <text class="sub">{{ item.brand || '未填品牌' }}<text v-if="item.color"> · {{ item.color }}</text></text>
        <text v-if="item.address?.parkingNo" class="sub">车位 {{ item.address.parkingNo }}</text>
        <view class="edit-btn" @click="edit(item)">编辑</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { createVehicle, fetchVehicles, updateVehicle, type VehicleRequest, type VehicleVO } from '@/api/catalog'

const list = ref<VehicleVO[]>([])
const loading = ref(true)
const submitting = ref(false)
/** 有值 = 编辑模式；空 = 新增模式 */
const editingId = ref<number | undefined>(undefined)
const form = ref<VehicleRequest>({})

function loadList(): void {
  loading.value = true
  fetchVehicles()
    .then((data) => {
      list.value = data || []
    })
    .catch(() => {
      list.value = []
    })
    .finally(() => {
      loading.value = false
    })
}

function resetForm(): void {
  editingId.value = undefined
  form.value = {}
}

function edit(item: VehicleVO): void {
  editingId.value = item.vehicleId
  form.value = {
    plateNo: item.plateNo,
    brand: item.brand,
    color: item.color,
    isNewEnergy: item.isNewEnergy,
    communityId: item.address?.communityId,
    parkingNo: item.address?.parkingNo
  }
}

async function submit(): Promise<void> {
  const plateNo = (form.value.plateNo ?? '').trim()
  if (!plateNo) {
    uni.showToast({ title: '请填写车牌号', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const payload: VehicleRequest = { ...form.value, plateNo }
    if (editingId.value) {
      await updateVehicle(editingId.value, payload)
      uni.showToast({ title: '已保存', icon: 'none' })
    } else {
      await createVehicle(payload)
      uni.showToast({ title: '已添加', icon: 'none' })
    }
    resetForm()
    await loadList()
  } catch {
    // request 层已统一提示（如车牌必填、车辆不属于当前用户）
  } finally {
    submitting.value = false
  }
}

onShow(loadList)
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
  margin-bottom: 20rpx;
}

.input {
  background: #f6faf9;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #1b2b2a;
  margin-bottom: 16rpx;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0 24rpx;
}

.switch-label {
  font-size: 28rpx;
  color: #1b2b2a;
}

.switch-box {
  width: 88rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background: #d7e3e1;
  position: relative;
  transition: background 0.2s;
}

.switch-box.on {
  background: #00aeb5;
}

.switch-dot {
  position: absolute;
  top: 6rpx;
  left: 6rpx;
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: #fff;
  transition: left 0.2s;
}

.switch-box.on .switch-dot {
  left: 46rpx;
}

.btn-row {
  display: flex;
  gap: 16rpx;
}

.btn {
  flex: 1;
  text-align: center;
  padding: 26rpx 0;
  border-radius: 40rpx;
  font-size: 30rpx;
}

.btn-primary {
  background: #14342f;
  color: #fff;
  font-weight: 600;
}

.btn-plain {
  border: 2rpx solid #cfe0de;
  color: #6b7b79;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 40rpx 0;
}

/* ---- 车辆大卡 ---- */
.vehicle {
  position: relative;
  background: linear-gradient(135deg, #f3fbfb, #eaf5f4);
  border: 2rpx solid #eaf1f0;
  border-radius: 20rpx;
  padding: 30rpx 28rpx;
  margin-bottom: 20rpx;
}

.vehicle-top {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.plate {
  font-size: 40rpx;
  font-weight: 700;
  color: #1b2b2a;
  letter-spacing: 2rpx;
}

.tag {
  font-size: 21rpx;
  color: #00aeb5;
  background: #e6f7f7;
  border-radius: 18rpx;
  padding: 4rpx 14rpx;
}

.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  color: #6b7b79;
}

.edit-btn {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  font-size: 25rpx;
  color: #00aeb5;
  border: 2rpx solid #00aeb5;
  border-radius: 30rpx;
  padding: 8rpx 26rpx;
}
</style>

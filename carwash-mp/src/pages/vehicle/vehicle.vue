<template>
  <view class="page">
    <view class="panel">
      <text class="panel-title">{{ editingId ? '编辑车辆' : '新增车辆' }}</text>

      <input class="input" v-model="form.plateNo" placeholder="车牌号（必填），支持新能源 8 位" />
      <input class="input" v-model="form.brand" placeholder="品牌型号，如 丰田 凯美瑞" />
      <input class="input" v-model="form.color" placeholder="颜色，如 白色" />
      <input class="input" v-model="form.parkingNo" placeholder="常停车位，如 A 区 23 号" />

      <view class="switch-row" @click="form.isNewEnergy = !form.isNewEnergy">
        <text class="switch-label">新能源车</text>
        <text class="switch-value">{{ form.isNewEnergy ? '是' : '否' }}</text>
      </view>

      <view class="btn-row">
        <view class="btn btn-primary" @click="submit">{{ submitting ? '保存中…' : (editingId ? '保存' : '添加') }}</view>
        <view v-if="editingId" class="btn btn-plain" @click="resetForm">取消编辑</view>
      </view>
    </view>

    <view class="panel">
      <text class="panel-title">我的车辆（{{ list.length }}）</text>
      <view v-if="loading" class="tip">加载中…</view>
      <view v-else-if="list.length === 0" class="tip">还没有车辆，先添加一辆吧</view>

      <view v-for="item in list" :key="item.vehicleId" class="vehicle">
        <view class="vehicle-main">
          <text class="plate">{{ item.plateNo }}</text>
          <text class="sub">{{ item.brand || '未填品牌' }}<text v-if="item.color"> · {{ item.color }}</text></text>
          <text v-if="item.address?.parkingNo" class="sub">车位 {{ item.address.parkingNo }}</text>
        </view>
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
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.panel-title {
  display: block;
  font-size: 30rpx;
  color: #222;
  font-weight: 500;
  margin-bottom: 20rpx;
}

.input {
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 22rpx;
  font-size: 28rpx;
  margin-bottom: 16rpx;
}

.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 0;
}

.switch-label {
  font-size: 28rpx;
  color: #444;
}

.switch-value {
  font-size: 28rpx;
  color: #1a73e8;
}

.btn-row {
  display: flex;
  gap: 16rpx;
  margin-top: 12rpx;
}

.btn {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.btn-primary {
  background: #1a73e8;
  color: #fff;
}

.btn-plain {
  background: #fff;
  color: #666;
  border: 2rpx solid #ddd;
}

.vehicle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-top: 2rpx solid #f2f3f5;
}

.vehicle-main {
  flex: 1;
  min-width: 0;
}

.plate {
  display: block;
  font-size: 32rpx;
  color: #222;
  font-weight: 500;
}

.sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #999;
}

.edit-btn {
  color: #1a73e8;
  font-size: 28rpx;
  padding-left: 24rpx;
}

.tip {
  text-align: center;
  color: #999;
  font-size: 26rpx;
  padding: 30rpx 0;
}
</style>

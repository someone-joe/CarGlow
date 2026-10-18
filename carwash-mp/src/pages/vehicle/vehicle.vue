<template>
  <view class="page">
    <view class="add-bar" @click="startAdd">
      <text class="add-icon">＋</text>
      <text>新增车辆</text>
    </view>

    <!-- 新增 / 编辑表单：点击上方按钮后才展开（复用 VehicleForm） -->
    <VehicleForm v-if="showForm" :vehicle="editing ?? undefined" @saved="onSaved" @cancel="closeForm" />

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
        <view class="edit-btn" @click="startEdit(item)">编辑</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchVehicles, type VehicleVO } from '@/api/catalog'
import VehicleForm from '@/components/VehicleForm.vue'

const list = ref<VehicleVO[]>([])
const loading = ref(true)
/** 有值 = 编辑模式；空 = 新增模式；showForm 控制表单是否展开 */
const editing = ref<VehicleVO | undefined>(undefined)
const showForm = ref(false)

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

function startAdd(): void {
  editing.value = undefined
  showForm.value = true
}

function startEdit(item: VehicleVO): void {
  editing.value = item
  showForm.value = true
}

function closeForm(): void {
  showForm.value = false
  editing.value = undefined
}

function onSaved(): void {
  closeForm()
  loadList()
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

.add-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  background: #14342f;
  color: #fff;
  border-radius: 20rpx;
  padding: 30rpx 0;
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.add-icon {
  font-size: 38rpx;
  line-height: 1;
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  margin-top: 20rpx;
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

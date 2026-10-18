<template>
  <view class="page">
    <view class="head">选择车辆</view>

    <view class="panel">
      <text class="panel-title">我的车辆（{{ list.length }}）</text>
      <view v-if="loading" class="tip">加载中…</view>
      <view v-else-if="list.length === 0" class="tip">还没有车辆，先添加一辆吧</view>

      <view
        v-for="item in list"
        :key="item.vehicleId"
        class="vehicle"
        :class="{ active: item.vehicleId === selected }"
        @tap="choose(item)"
      >
        <view class="vehicle-top">
          <text class="plate">{{ item.plateNo }}</text>
          <text v-if="item.isDefault" class="tag">默认</text>
        </view>
        <text class="sub">{{ item.brand || '未填品牌' }}<text v-if="item.color"> · {{ item.color }}</text></text>
        <text v-if="item.address?.parkingNo" class="sub">车位 {{ item.address.parkingNo }}</text>
        <text v-if="item.vehicleId === selected" class="check">✓</text>
      </view>
    </view>

    <view v-if="showAdd" class="panel">
      <VehicleForm @saved="onAddSaved" @cancel="showAdd = false" />
    </view>

    <view class="actions">
      <view class="btn btn-primary" @tap="showAdd = !showAdd">{{ showAdd ? '收起新增' : '新增车辆' }}</view>
      <view class="btn btn-plain" @tap="goManage">管理车辆</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchVehicles, type VehicleVO } from '@/api/catalog'
import VehicleForm from '@/components/VehicleForm.vue'
import { pickVehicle } from '@/store/vehicleSelect'

const source = ref('')
const selected = ref<number | undefined>(undefined)
const list = ref<VehicleVO[]>([])
const loading = ref(true)
const showAdd = ref(false)

onLoad((q) => {
  source.value = (q?.source as string) || ''
  const s = q?.selected
  if (s) selected.value = Number(s)
})

/** 从管理页返回也要刷新列表 */
onShow(loadList)

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

function choose(item: VehicleVO): void {
  pickVehicle(item.vehicleId as number, source.value)
  uni.navigateBack()
}

/** 在选车页内直接新增：保存后直接回填到下单页 */
function onAddSaved(id: number): void {
  pickVehicle(id, source.value)
  uni.navigateBack()
}

function goManage(): void {
  uni.navigateTo({ url: '/pages/vehicle/vehicle' })
}
</script>

<style scoped>
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.head {
  font-size: 32rpx;
  font-weight: 700;
  color: #1b2b2a;
  padding: 8rpx 4rpx 20rpx;
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

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 40rpx 0;
}

.vehicle {
  position: relative;
  background: linear-gradient(135deg, #f3fbfb, #eaf5f4);
  border: 2rpx solid #eaf1f0;
  border-radius: 20rpx;
  padding: 30rpx 28rpx;
  margin-bottom: 20rpx;
}

.vehicle.active {
  border-color: #00aeb5;
  background: #f3fbfb;
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

.check {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  color: #00aeb5;
  font-size: 40rpx;
}

.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 8rpx;
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
</style>

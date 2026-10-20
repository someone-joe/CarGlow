<template>
  <view class="cg-page vehicle-select">
    <view class="head">选择车辆</view>

    <view class="panel">
      <text class="panel-title">我的车辆（{{ list.length }}）</text>
      <view v-if="loading" class="cg-empty">加载中…</view>
      <view v-else-if="list.length === 0" class="cg-empty">还没有车辆，先添加一辆吧</view>

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
.vehicle-select {
  padding-bottom: 220rpx;
}

/* ---- 页头 ---- */
.head {
  margin: 32rpx 8rpx 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

/* ---- 车辆列表面板 ---- */
.panel {
  padding: 8rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  padding: 32rpx 0 16rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

/* ---- 车辆行：选中 = 2px 蓝边 + 6% 淡蓝底 + 提升阴影 ---- */
.vehicle {
  position: relative;
  box-sizing: border-box;
  margin-bottom: 24rpx;
  padding: 32rpx;
  background: var(--cg-card);
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.vehicle.active {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
  box-shadow: var(--cg-shadow-raised);
}

.vehicle-top {
  display: flex;
  align-items: center;
}

.plate {
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
  letter-spacing: 2rpx;
}

.tag {
  margin-left: 24rpx;
  padding: 4rpx 20rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.check {
  position: absolute;
  top: 50%;
  right: 32rpx;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: var(--cg-primary);
  color: #ffffff;
  font-size: var(--cg-font-note);
}

/* ---- 底部操作 ---- */
.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  padding: 16rpx 40rpx calc(16rpx + env(safe-area-inset-bottom));
  background: var(--cg-glass-bg);
  -webkit-backdrop-filter: blur(var(--cg-glass-blur));
  backdrop-filter: blur(var(--cg-glass-blur));
  border-top: var(--cg-glass-border);
  box-shadow: 0 -4rpx 40rpx rgba(0, 0, 0, 0.06);
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
</style>

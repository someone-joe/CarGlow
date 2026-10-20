<template>
  <view class="cg-page vehicle">
    <view class="add-bar" @click="startAdd">
      <text class="add-icon">＋</text>
      <text>新增车辆</text>
    </view>

    <!-- 新增 / 编辑表单：点击上方按钮后才展开（复用 VehicleForm） -->
    <VehicleForm v-if="showForm" :vehicle="editing ?? undefined" @saved="onSaved" @cancel="closeForm" />

    <!-- 车辆列表 -->
    <view class="panel">
      <text class="panel-title">我的车辆（{{ list.length }}）</text>
      <view v-if="loading" class="cg-empty">加载中…</view>
      <view v-else-if="list.length === 0" class="cg-empty">还没有车辆，先添加一辆吧</view>

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
/* ---- 新增按钮 ---- */
.add-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 32rpx;
  background: var(--cg-card);
  border: 1rpx dashed var(--cg-border);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: var(--cg-primary);
}

.add-icon {
  margin-right: 16rpx;
  font-size: var(--cg-font-body);
  font-weight: 300;
}

/* ---- 车辆列表面板 ---- */
.panel {
  margin-top: 32rpx;
  padding: 8rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  padding: 32rpx 0 16rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

/* ---- 车辆行 ---- */
.vehicle {
  position: relative;
  padding: 32rpx 0;
  border-top: 1rpx solid var(--cg-line);
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
  background: var(--cg-tint);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
}

.sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.edit-btn {
  position: absolute;
  top: 32rpx;
  right: 0;
  min-height: 96rpx;
  padding: 0 8rpx;
  color: var(--cg-primary);
  font-size: var(--cg-font-caption);
  line-height: 96rpx;
}
</style>

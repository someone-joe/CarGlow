<template>
  <view class="vp">
    <block v-if="!vehicles || vehicles.length === 0">
      <view class="vp-add" @tap="goAdd">+ 新增车辆</view>
    </block>
    <block v-else>
      <view
        v-for="v in visible"
        :key="v.vehicleId"
        class="vp-item"
        :class="{ active: v.vehicleId === modelValue }"
        @tap="select(v.vehicleId)"
      >
        <view class="vp-plate">{{ v.plateNo }}</view>
        <view class="vp-sub">{{ subText(v) }}</view>
        <text v-if="v.vehicleId === modelValue" class="vp-check">✓</text>
      </view>
      <view v-if="vehicles.length > 2" class="vp-more" @tap="emit('navigate')">更多车辆 ›</view>
      <view class="vp-manage" @tap="emit('manage')">管理车辆 ›</view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VehicleVO } from '@/api/catalog'
import { VEHICLE_TYPE_OPTIONS } from '@/api/selfwash'

const props = defineProps<{
  vehicles?: VehicleVO[]
  modelValue?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number]
  /** 新增 / 更多：父页打开「选择车辆」页 */
  navigate: []
  /** 管理车辆：父页打开车辆管理页 */
  manage: []
}>()

const visible = computed(() => (props.vehicles || []).slice(0, 2))

const typeLabelMap = computed(() => {
  const m: Record<string, string> = {}
  for (const o of VEHICLE_TYPE_OPTIONS) m[o.value] = o.label
  return m
})

function subText(v: VehicleVO): string {
  return [v.brand, v.color, v.vehicleType ? typeLabelMap.value[v.vehicleType] : '']
    .filter(Boolean)
    .join(' · ')
}

function select(id?: number): void {
  if (id == null) return
  emit('update:modelValue', id)
}

function goAdd(): void {
  emit('navigate')
}
</script>

<style scoped>
.vp {
  width: 100%;
}

/* ---- 新增车辆 ---- */
.vp-add {
  border: 1rpx dashed var(--cg-border);
  border-radius: var(--cg-radius-md);
  padding: 32rpx 0;
  font-size: var(--cg-font-caption);
  color: var(--cg-primary);
  text-align: center;
}

/* ---- 车辆行：选中 = 2px 蓝边 + 6% 淡蓝底 + 提升阴影，不放大 ---- */
.vp-item {
  position: relative;
  box-sizing: border-box;
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  padding: 24rpx;
  margin-bottom: 16rpx;
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.vp-item.active {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
  box-shadow: var(--cg-shadow-raised);
}

.vp-plate {
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
  letter-spacing: 1rpx;
}

.vp-sub {
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.vp-check {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: var(--cg-primary);
  color: #ffffff;
  font-size: var(--cg-font-note);
}

.vp-more,
.vp-manage {
  padding: 16rpx 0;
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
  text-align: right;
}

.vp-more {
  margin-top: 16rpx;
}
</style>

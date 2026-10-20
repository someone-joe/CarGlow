<template>
  <view class="vp" :class="{ 'vp-plain': variant === 'plain' }">
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
  /** variant=plain 为新版视觉（下单页）；默认 legacy 保持旧版，未改版页面不受影响 */
  variant?: 'legacy' | 'plain'
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

.vp-add {
  border: 2rpx dashed #cfe0de;
  border-radius: 16rpx;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #00aeb5;
}

.vp-item {
  position: relative;
  display: flex;
  flex-direction: column;
  border: 2rpx solid #eaf1f0;
  border-radius: 16rpx;
  padding: 22rpx 24rpx;
  margin-bottom: 14rpx;
}

.vp-item.active {
  border-color: #00aeb5;
  background: #f3fbfb;
}

.vp-plate {
  font-size: 30rpx;
  font-weight: 700;
  color: #1b2b2a;
  letter-spacing: 1rpx;
}

.vp-sub {
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.vp-check {
  position: absolute;
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  color: #00aeb5;
  font-size: 32rpx;
}

.vp-more,
.vp-manage {
  text-align: right;
  font-size: 24rpx;
  color: #00aeb5;
  padding: 6rpx 0;
}

.vp-more {
  margin-top: 4rpx;
}

/* ---- plain 变体（Apple HIG 新版，仅下单页使用） ---- */
.vp-plain .vp-add {
  border: 1rpx dashed var(--cg-border);
  border-radius: var(--cg-radius-md);
  padding: 32rpx 0;
  font-size: var(--cg-font-caption);
  color: var(--cg-primary);
}

/* 复用全局单选卡片态：2px 蓝边 + 6% 淡蓝底 + 提升阴影，不放大 */
.vp-plain .vp-item {
  box-sizing: border-box;
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-md);
  padding: 24rpx;
  margin-bottom: 16rpx;
  transition: border-color 0.28s ease-out, background-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.vp-plain .vp-item.active {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
  box-shadow: var(--cg-shadow-raised);
}

.vp-plain .vp-plate {
  font-size: var(--cg-font-body);
  font-weight: 600;
  color: var(--cg-title);
  letter-spacing: 1rpx;
}

.vp-plain .vp-sub {
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.vp-plain .vp-check {
  right: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: var(--cg-primary);
  color: #ffffff;
  font-size: var(--cg-font-note);
  line-height: 36rpx;
  text-align: center;
}

.vp-plain .vp-more,
.vp-plain .vp-manage {
  padding: 16rpx 0;
  font-size: var(--cg-font-note);
  color: var(--cg-primary);
}
</style>

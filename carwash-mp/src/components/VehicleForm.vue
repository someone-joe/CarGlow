<template>
  <view class="vf">
    <text class="vf-title">{{ editingId != null ? '编辑车辆' : '添加车辆' }}</text>

    <PlateInput v-model="form.plateNo" />

    <view class="vtype-row">
      <text class="vtype-label">车辆类型</text>
      <view class="vtype-opts">
        <view
          v-for="o in vehicleTypeOptions"
          :key="o.value"
          class="vtype-opt"
          :class="{ on: form.vehicleType === o.value }"
          @tap="form.vehicleType = o.value"
        >{{ o.label }}</view>
      </view>
    </view>

    <input class="vf-input" v-model="form.brand" placeholder="品牌型号，如 丰田 凯美瑞" />
    <input class="vf-input" v-model="form.color" placeholder="颜色，如 白色" />
    <input class="vf-input" v-model="form.parkingNo" placeholder="常停车位，如 A 区 23 号" />

    <view class="switch-row" @click="form.isNewEnergy = !form.isNewEnergy">
      <text class="switch-label">新能源车</text>
      <view class="switch-box" :class="{ on: form.isNewEnergy }">
        <view class="switch-dot" />
      </view>
    </view>

    <view class="btn-row">
      <view class="btn btn-primary" @click="submit">
        {{ submitting ? '保存中…' : editingId != null ? '保存' : '添加' }}
      </view>
      <view v-if="editingId != null" class="btn btn-plain" @click="cancel">取消</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { createVehicle, updateVehicle, type VehicleRequest, type VehicleVO } from '@/api/catalog'
import { VEHICLE_TYPE_OPTIONS } from '@/api/selfwash'
import PlateInput from '@/components/PlateInput.vue'

const props = defineProps<{ vehicle?: VehicleVO }>()
const emit = defineEmits<{ saved: [id: number]; cancel: [] }>()

const vehicleTypeOptions = VEHICLE_TYPE_OPTIONS
const submitting = ref(false)
const editingId = ref<number | undefined>(props.vehicle?.vehicleId)
const form = ref<VehicleRequest>({
  plateNo: props.vehicle?.plateNo,
  brand: props.vehicle?.brand,
  color: props.vehicle?.color,
  vehicleType: props.vehicle?.vehicleType,
  isNewEnergy: props.vehicle?.isNewEnergy,
  communityId: props.vehicle?.address?.communityId,
  parkingNo: props.vehicle?.address?.parkingNo
})

// 车牌末位为新能源标识（D=纯电 / F=非纯电·插混）时，自动打开新能源开关（只开不关，避免覆盖已有值或手动选择）
watch(
  () => form.value.plateNo,
  (plate) => {
    const last = (plate ?? '').trim().slice(-1).toUpperCase()
    if (last === 'D' || last === 'F') form.value.isNewEnergy = true
  },
)

async function submit(): Promise<void> {
  const plateNo = (form.value.plateNo ?? '').trim()
  if (!plateNo) {
    uni.showToast({ title: '请填写车牌号', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const payload: VehicleRequest = { ...form.value, plateNo }
    let id: number
    if (editingId.value) {
      await updateVehicle(editingId.value, payload)
      id = editingId.value
    } else {
      id = await createVehicle(payload)
    }
    uni.showToast({ title: editingId.value ? '已保存' : '已添加', icon: 'none' })
    emit('saved', id)
  } catch {
    // request 层已统一提示（如车牌必填、车辆不属于当前用户）
  } finally {
    submitting.value = false
  }
}

function cancel(): void {
  emit('cancel')
}
</script>

<style scoped>
.vf {
  box-sizing: border-box;
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.vf-title {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

/* ---- 车辆类型 ---- */
.vtype-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 96rpx;
  margin-top: 24rpx;
}

.vtype-label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.vtype-opts {
  display: flex;
}

.vtype-opt {
  padding: 12rpx 32rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.vtype-opt + .vtype-opt {
  margin-left: 16rpx;
}

.vtype-opt.on {
  background: var(--cg-primary);
  color: #ffffff;
}

/* ---- 输入 ---- */
.vf-input {
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
</style>

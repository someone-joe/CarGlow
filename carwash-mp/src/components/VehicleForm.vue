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
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
}

.vf-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 20rpx;
}

.vtype-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.vtype-label {
  font-size: 28rpx;
  color: #1b2b2a;
  margin-right: 20rpx;
}

.vtype-opts {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.vtype-opt {
  font-size: 25rpx;
  color: #4d5e5c;
  background: #f2f7f7;
  border-radius: 24rpx;
  padding: 12rpx 28rpx;
}

.vtype-opt.on {
  color: #fff;
  background: #00aeb5;
}

.vf-input {
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
</style>

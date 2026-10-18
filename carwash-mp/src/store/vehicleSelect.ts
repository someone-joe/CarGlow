import { reactive } from 'vue'

/**
 * 选车回填：下单页通过 VehiclePicker 打开「选择车辆」页（pages/vehicle/select）后，
 * 在该页点选或新增车辆，把所选 vehicleId 暂存于此，下单页 onShow 读取并回填到本页 vehicleId。
 * 沿用 orderDraft 的模块级 reactive 单例写法，不引入 pinia。
 */
export const vehicleSelect = reactive<{ pendingVehicleId?: number; source?: string }>({})

export function pickVehicle(id: number, source: string): void {
  vehicleSelect.pendingVehicleId = id
  vehicleSelect.source = source
}

/** 仅当 source 匹配时取出并清空，避免错页误消费 */
export function consumePickedVehicle(source: string): number | undefined {
  if (vehicleSelect.source === source && vehicleSelect.pendingVehicleId != null) {
    const id = vehicleSelect.pendingVehicleId
    vehicleSelect.pendingVehicleId = undefined
    vehicleSelect.source = undefined
    return id
  }
  return undefined
}

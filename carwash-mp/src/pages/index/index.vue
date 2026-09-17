<template>
  <view class="page">
    <view class="hero">
      <text class="title">夜间代客洗车</text>
      <text class="subtitle">今晚下单，明早干净上路</text>
    </view>

    <view class="card">
      <text class="card-title">标准洗车（外观+内饰吸尘）</text>
      <text class="card-desc">¥39.00 · 约 45 分钟</text>
    </view>

    <view class="primary-btn" @click="submitOrder">{{ submitting ? '提交中…' : '立即下单' }}</view>

    <view class="entry" @click="goOrders">
      <text class="entry-title">我的订单</text>
      <text class="entry-desc">查看进行中 / 待评价 / 全部订单</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { createOrder } from '@/api/order'

/**
 * 临时下单入口：服务项/车辆/机柜的选择页尚未开发（对应接口 /api/v1/services、/api/v1/vehicles、/api/v1/cabinets 未实现），
 * 这里先用本地种子数据的 ID 打通下单链路，正式下单页上线后本常量必须删除。
 */
const DEMO_SERVICE_ID = 101
const DEMO_VEHICLE_ID = 201
const DEMO_CABINET_ID = 301

const submitting = ref(false)

/** 预约日期默认明天，格式与契约一致：yyyy-MM-dd */
function tomorrow(): string {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

async function submitOrder(): Promise<void> {
  if (submitting.value) {
    return
  }
  submitting.value = true
  try {
    const result = await createOrder({
      serviceId: DEMO_SERVICE_ID,
      vehicleId: DEMO_VEHICLE_ID,
      cabinetId: DEMO_CABINET_ID,
      appointDate: tomorrow(),
      appointTime: '19:00',
      pickupRequired: true,
      remark: '停在地库 A 区 23 号',
      agreed: true,
    })
    uni.showToast({ title: '下单成功，待支付', icon: 'none' })
    // 去订单列表看刚创建的订单（状态：待支付）
    uni.navigateTo({ url: `/pages/orders/orders?orderNo=${result.orderNo}` })
  } catch {
    // request 层已统一提示，这里只恢复按钮状态
  } finally {
    submitting.value = false
  }
}

function goOrders(): void {
  uni.navigateTo({ url: '/pages/orders/orders' })
}
</script>

<style>
.page {
  padding: 32rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.hero {
  padding: 32rpx 0 40rpx;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
  color: #222;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #888;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 32rpx;
}

.card-title {
  display: block;
  font-size: 32rpx;
  color: #222;
}

.card-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #e8700a;
}

.primary-btn {
  background: #1a73e8;
  color: #fff;
  text-align: center;
  padding: 28rpx 0;
  border-radius: 16rpx;
  font-size: 32rpx;
  margin-bottom: 32rpx;
}

.entry {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}

.entry-title {
  display: block;
  font-size: 32rpx;
  color: #222;
}

.entry-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #999;
}
</style>

<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 可投保产品 -->
      <view class="section">
        <text class="section-title">保障方案</text>
        <view v-if="!products.length" class="empty">暂无可投保的保障方案</view>
        <view v-for="p in products" :key="p.productId" class="card">
          <view class="card-main">
            <text class="card-name">{{ p.name }}</text>
            <text class="card-sub">{{ p.coverageDesc }}</text>
            <text class="card-price">保费 ¥{{ fen2yuan(p.priceAmount) }} / 年</text>
          </view>
          <view class="card-action">
            <view class="mini-btn" @click="onBuy(p)">投保</view>
          </view>
        </view>
      </view>

      <!-- 我的保单 -->
      <view class="section">
        <text class="section-title">我的保单</text>
        <view v-if="!policies.length" class="empty">还没有保单，选上面的方案投保吧</view>
        <view v-for="pol in policies" :key="pol.policyId" class="card column">
          <view class="card-row">
            <text class="card-name">{{ pol.name }}</text>
            <text class="status-text" :class="statusClass(pol.status)">{{ policyStatusLabel(pol.status) }}</text>
          </view>
          <text class="card-sub">保单号 {{ pol.policyNo }}</text>
          <text class="card-meta">车辆 {{ vehicleLabel(pol.vehicleId) }}</text>
          <text class="card-meta">保障期 {{ formatDate(pol.startTime) }} ~ {{ formatDate(pol.endTime) }}</text>
          <text class="card-meta">实付保费 ¥{{ fen2yuan(pol.paidAmount) }}</text>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  buyInsurance,
  fetchInsuranceProducts,
  fetchMyPolicies,
  type InsurancePolicyVO,
  type InsuranceProductVO,
} from '@/api/promo'
import { fetchVehicles } from '@/api/catalog'
import type { VehicleVO } from '@/api/catalog'

const loading = ref(true)
const products = ref<InsuranceProductVO[]>([])
const policies = ref<InsurancePolicyVO[]>([])
const vehicles = ref<VehicleVO[]>([])

/** 金额一律以「分」为单位，展示时才转元 */
function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function policyStatusLabel(status?: string): string {
  return status === 'ACTIVE' ? '保障中' : status === 'EXPIRED' ? '已过期' : status === 'CANCELED' ? '已退保' : '未知'
}

function statusClass(status?: string): string {
  return status === 'ACTIVE' ? 'st-active' : status === 'EXPIRED' ? 'st-expired' : 'st-canceled'
}

/** 根据保单里的 vehicleId 回显车牌 + 品牌，车辆列表已在本页加载 */
function vehicleLabel(vehicleId?: number): string {
  const v = vehicles.value.find((item) => item.vehicleId === vehicleId)
  if (!v) return '-'
  const plate = v.plateNo || '未上牌'
  return v.brand ? `${plate} · ${v.brand}` : plate
}

/** 时间戳转 yyyy-MM-dd（契约里时间均为毫秒时间戳） */
function formatDate(ts?: number): string {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = (n: number) => `${n}`.padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function loadAll(): void {
  loading.value = true
  Promise.all([fetchInsuranceProducts(), fetchMyPolicies(), fetchVehicles()])
    .then(([ps, pls, vs]) => {
      products.value = ps || []
      policies.value = pls || []
      vehicles.value = vs || []
    })
    .catch(() => {
      products.value = []
      policies.value = []
      vehicles.value = []
    })
    .finally(() => {
      loading.value = false
    })
}

/** 投保：先确保有车辆，再选车辆后下单 */
async function onBuy(p: InsuranceProductVO): Promise<void> {
  if (p.productId == null) return
  if (!vehicles.value.length) {
    uni.showToast({ title: '请先添加车辆', icon: 'none' })
    uni.navigateTo({ url: '/pages/vehicle/vehicle' })
    return
  }
  // 用动作面板选车辆（与首页 picker 风格一致，避免自造选择组件）
  const plateNos = vehicles.value.map((v) => v.plateNo || '未上牌车辆')
  uni.showActionSheet({
    itemList: plateNos,
    success: async (res) => {
      const vehicle = vehicles.value[res.tapIndex]
      if (!vehicle?.vehicleId) return
      try {
        await buyInsurance({ productId: p.productId as number, vehicleId: vehicle.vehicleId, orderNo: null })
        uni.showToast({ title: '投保成功，保障已生效', icon: 'none' })
        await loadAll()
      } catch {
        // request 层已统一提示
      }
    },
  })
}

// 投保后返回或切回本页时，保单列表要刷新
onShow(loadAll)
</script>

<style>
.page {
  padding: 24rpx 32rpx 60rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.section {
  margin-bottom: 32rpx;
}

.section-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.tip,
.empty {
  padding: 60rpx 0;
  text-align: center;
  color: #999;
  font-size: 28rpx;
}

.empty {
  background: #fff;
  border-radius: 16rpx;
}

.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.card.column {
  flex-direction: column;
  align-items: stretch;
}

.card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-name {
  display: block;
  font-size: 30rpx;
  color: #222;
}

.card-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #888;
}

.card-price {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #e8700a;
}

.card-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #999;
}

.card-action {
  margin-left: 16rpx;
}

.mini-btn {
  padding: 14rpx 32rpx;
  background: #1a73e8;
  color: #fff;
  border-radius: 30rpx;
  font-size: 26rpx;
}

.status-text {
  font-size: 26rpx;
}

.st-active {
  color: #1a73e8;
}

.st-expired {
  color: #999;
}

.st-canceled {
  color: #d93025;
}
</style>

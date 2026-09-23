<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 可投保产品 -->
      <view class="block-title">保障方案</view>
      <view v-if="!products.length" class="empty">暂无可投保的保障方案</view>
      <view v-for="p in products" :key="p.productId" class="product">
        <view class="product-main">
          <text class="product-name">{{ p.name }}</text>
          <text class="product-desc">{{ p.coverageDesc }}</text>
          <text class="product-price">保费 ¥{{ fen2yuan(p.priceAmount) }} / 年</text>
        </view>
        <view class="buy-btn" @click="onBuy(p)">投保</view>
      </view>

      <!-- 我的保单 -->
      <view class="block-title">我的保单</view>
      <view v-if="!policies.length" class="empty">还没有保单，选上面的方案投保吧</view>
      <view v-for="pol in policies" :key="pol.policyId" class="policy">
        <view class="policy-top">
          <text class="policy-name">{{ pol.name }}</text>
          <text class="badge" :class="statusClass(pol.status)">{{ policyStatusLabel(pol.status) }}</text>
        </view>
        <text class="policy-line">保单号 {{ pol.policyNo }}</text>
        <text class="policy-line">车辆 {{ vehicleLabel(pol.vehicleId) }}</text>
        <text class="policy-line">保障期 {{ formatDate(pol.startTime) }} ~ {{ formatDate(pol.endTime) }}</text>
        <text class="policy-line">实付保费 ¥{{ fen2yuan(pol.paidAmount) }}</text>
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
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 80rpx 0;
}

.block-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding: 12rpx 4rpx 18rpx;
}

.empty {
  text-align: center;
  color: #8a9a98;
  font-size: 25rpx;
  padding: 40rpx 0;
}

/* ---- 保障方案 ---- */
.product {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.product-main {
  flex: 1;
}

.product-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.product-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.product-price {
  display: block;
  margin-top: 10rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.buy-btn {
  background: #14342f;
  color: #fff;
  font-size: 26rpx;
  padding: 14rpx 34rpx;
  border-radius: 34rpx;
}

/* ---- 我的保单 ---- */
.policy {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.policy-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid #eaf1f0;
}

.policy-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.badge {
  font-size: 23rpx;
  border-radius: 20rpx;
  padding: 5rpx 18rpx;
}

.badge.st-active {
  color: #00aeb5;
  background: #e6f7f7;
}

.badge.st-expired {
  color: #8a9a98;
  background: #f2f7f7;
}

.badge.st-canceled {
  color: #ff5b4a;
  background: #ffeeeb;
}

.policy-line {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  color: #8a9a98;
}
</style>

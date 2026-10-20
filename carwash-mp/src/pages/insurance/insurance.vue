<template>
  <view class="cg-page insurance">
    <view v-if="loading" class="cg-empty">加载中…</view>

    <block v-else>
      <!-- 可投保产品 -->
      <text class="cg-section-title">保障方案</text>
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
      <text class="cg-section-title">我的保单</text>
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
/* ---- 产品卡 ---- */
.product {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.product-main {
  flex: 1;
  margin-right: 32rpx;
}

.product-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.product-desc {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
  line-height: 1.6;
}

.product-price {
  display: block;
  margin-top: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.buy-btn {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 176rpx;
  height: 104rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

/* ---- 保单卡 ---- */
.policy {
  margin-bottom: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.policy-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.policy-name {
  flex: 1;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.badge {
  flex: none;
  margin-left: 24rpx;
  padding: 8rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.badge.st-active {
  background: rgba(52, 199, 89, 0.12);
  color: var(--cg-success);
}

.badge.st-expired {
  background: rgba(134, 134, 139, 0.14);
  color: var(--cg-text-sub);
}

.badge.st-canceled {
  background: rgba(255, 59, 48, 0.10);
  color: var(--cg-error);
}

.policy-line {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}
</style>

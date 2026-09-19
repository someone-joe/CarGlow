<template>
  <view class="page">
    <SiteBar />

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <view v-if="!cabinets.length" class="empty">
        <text class="empty-title">附近暂无可用的钥匙柜</text>
        <text class="empty-sub">可联系客服申请在您的小区布柜</text>
      </view>

      <!-- 柜机列表：名称/空闲格口/在线状态/距离，全部后端下发；distance 未授权定位时为 null -->
      <view
        v-for="item in cabinets"
        :key="item.cabinetId"
        class="card"
        :class="{ disabled: item.full || !item.online, active: item.cabinetId === currentId }"
        @click="pick(item)"
      >
        <view class="card-main">
          <text class="name">{{ item.name }}</text>
          <text class="sub">
            空闲格口 {{ item.slotFree }} / {{ item.slotTotal }}
            <text v-if="item.distance != null"> · 距您约 {{ item.distance }} 米</text>
          </text>
          <text class="status" :class="{ off: !item.online, full: item.full }">
            {{ !item.online ? '离线' : item.full ? '已满 · 不可选' : '在线 · 可预约' }}
          </text>
        </view>
        <text v-if="item.cabinetId === currentId" class="check">✓</text>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import SiteBar from '@/components/SiteBar.vue'
import { fetchCabinets, type CabinetVO } from '@/api/catalog'
import { orderDraft, setOrderDraft } from '@/store/orderDraft'

const cabinets = ref<CabinetVO[]>([])
const currentId = ref<number | undefined>(orderDraft.cabinetId)
const loading = ref(true)

async function load(): Promise<void> {
  loading.value = true
  try {
    cabinets.value = (await fetchCabinets()) || []
    currentId.value = orderDraft.cabinetId ?? cabinets.value.find((c) => !c.full)?.cabinetId
  } catch {
    cabinets.value = []
  } finally {
    loading.value = false
  }
}

function pick(item: CabinetVO): void {
  if (item.full || !item.online) {
    uni.showToast({ title: item.full ? '该柜格口已满' : '该柜离线，暂不可用', icon: 'none' })
    return
  }
  setOrderDraft({ cabinetId: item.cabinetId })
  uni.navigateBack()
}

onShow(load)
</script>

<style>
.page {
  padding: 24rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #999;
  padding: 60rpx 0;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-title {
  font-size: 30rpx;
  color: #333;
}

.empty-sub {
  font-size: 26rpx;
  color: #999;
  margin-top: 12rpx;
}

.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.card.active {
  border-color: #1a73e8;
  background: #f2f7ff;
}

.card.disabled {
  opacity: 0.5;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.name {
  display: block;
  font-size: 30rpx;
  color: #222;
  font-weight: 500;
}

.sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #999;
}

.status {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #1a73e8;
}

.status.off {
  color: #999;
}

.status.full {
  color: #d93025;
}

.check {
  font-size: 40rpx;
  color: #1a73e8;
  margin-left: 16rpx;
}
</style>

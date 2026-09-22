<template>
  <view class="page">
    <SiteBar />

    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <view v-if="!cabinets.length" class="empty">
        <text class="empty-title">附近暂无可用的钥匙柜</text>
        <text class="empty-sub">可联系客服申请在您的小区布柜</text>
      </view>

      <!-- 柜机九宫格：名称/空闲格口/在线状态，全部后端下发 -->
      <view class="grid">
        <view
          v-for="item in cabinets"
          :key="item.cabinetId"
          class="slot"
          :class="{
            disabled: item.full || !item.online,
            active: item.cabinetId === currentId
          }"
          @click="pick(item)"
        >
          <text class="slot-name">{{ item.name }}</text>
          <text class="slot-free">空闲 {{ item.slotFree }}/{{ item.slotTotal }}</text>
          <text v-if="item.cabinetId === currentId" class="slot-check">✓</text>
        </view>
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

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-title {
  font-size: 30rpx;
  color: #1b2b2a;
}

.empty-sub {
  margin-top: 12rpx;
  font-size: 25rpx;
  color: #8a9a98;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
}

.slot {
  position: relative;
  width: calc((100% - 36rpx) / 3);
  background: #fff;
  border-radius: 18rpx;
  border: 2rpx solid #eaf1f0;
  padding: 26rpx 18rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.slot.active {
  border-color: #00aeb5;
  background: #f3fbfb;
}

.slot.disabled {
  opacity: 0.55;
}

.slot-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #1b2b2a;
  text-align: center;
}

.slot-free {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #8a9a98;
}

.slot-check {
  position: absolute;
  top: 8rpx;
  right: 12rpx;
  font-size: 24rpx;
  color: #00aeb5;
}
</style>

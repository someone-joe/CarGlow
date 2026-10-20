<template>
  <view class="cg-page cabinet">
    <SiteBar />

    <view v-if="loading" class="cg-empty">加载中…</view>

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
.empty-title {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-text-sub);
}

.empty-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

/* ---- 柜机网格：两列卡片 ---- */
.grid {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -8rpx;
}

.slot {
  position: relative;
  box-sizing: border-box;
  width: calc(50% - 16rpx);
  margin: 16rpx 8rpx 0;
  padding: 32rpx;
  background: var(--cg-card);
  border: 2rpx solid var(--cg-line);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
  transition: border-color 0.28s ease-out, box-shadow 0.28s ease-out;
}

.slot.active {
  border-color: var(--cg-primary);
  box-shadow: var(--cg-shadow-raised);
}

.slot.disabled {
  opacity: 0.55;
}

.slot-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
  line-height: 1.4;
}

.slot-free {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.slot-check {
  position: absolute;
  top: 32rpx;
  right: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: var(--cg-primary);
  color: #ffffff;
  font-size: var(--cg-font-note);
}
</style>

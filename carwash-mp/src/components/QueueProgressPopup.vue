<template>
  <view v-if="show">
    <view class="mask" @click="emit('close')" />
    <view class="dialog">
      <text class="dlg-title">排队进度</text>

      <view class="dlg-head">
        <text class="dlg-ws">{{ ws?.code || '' }}{{ ws?.name ? ` · ${ws.name}` : '' }}</text>
        <text class="dlg-ahead">前面有 {{ aheadCount }} 人</text>
      </view>

      <view class="dlg-list">
        <view v-if="!(ws?.queueItems || []).length" class="dlg-empty">暂无排队信息</view>
        <view v-for="(it, idx) in ws?.queueItems || []" :key="idx" class="dlg-row">
          <view class="dlg-main">
            <text class="dlg-plate">{{ it.plateNo }}</text>
            <text class="dlg-svc">{{ it.serviceName || '自助洗车' }}</text>
          </view>
          <view class="dlg-side">
            <text :class="['dlg-tag', it.status === 'SERVING' ? 'serving' : 'queuing']">
              {{ it.status === 'SERVING' ? '服务中' : '排队中' }}
            </text>
            <text v-if="it.estimatedMinutes != null" class="dlg-min">预计花费{{ it.estimatedMinutes }}分钟</text>
          </view>
        </view>
      </view>

      <view class="dlg-btn" @click="emit('close')">我知道了</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WorkstationVO } from '@/api/selfwash'

const props = defineProps<{ show: boolean; ws?: WorkstationVO }>()
const emit = defineEmits<{ close: [] }>()

// 与工位卡片口径一致：服务中 1 单 + 排队中单数
const aheadCount = computed(
  () => (props.ws?.status === 'SERVING' ? 1 : 0) + (props.ws?.queueCount ?? 0),
)
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9998;
}

.dialog {
  position: fixed;
  left: 70rpx;
  right: 70rpx;
  top: 50%;
  transform: translateY(-50%);
  z-index: 9999;
  background: #fff;
  border-radius: 24rpx;
  padding: 36rpx 32rpx;
}

.dlg-title {
  display: block;
  text-align: center;
  font-size: 32rpx;
  font-weight: 700;
  color: #1b2b2a;
  margin-bottom: 28rpx;
}

.dlg-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #eaf1f0;
}

.dlg-ws {
  font-size: 26rpx;
  color: #6b7b79;
}

.dlg-ahead {
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.dlg-list {
  padding: 8rpx 0 4rpx;
}

.dlg-empty {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 32rpx 0;
}

.dlg-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 0;
}

.dlg-main {
  display: flex;
  flex-direction: column;
}

.dlg-plate {
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.dlg-svc {
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.dlg-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
}

.dlg-tag {
  font-size: 21rpx;
  border-radius: 16rpx;
  padding: 4rpx 16rpx;
}

.dlg-tag.serving {
  color: #ff5b4a;
  background: #ffecea;
}

.dlg-tag.queuing {
  color: #3d7bff;
  background: #e8efff;
}

.dlg-min {
  font-size: 22rpx;
  color: #6b7b79;
}

.dlg-btn {
  margin-top: 28rpx;
  background: #14342f;
  color: #fff;
  text-align: center;
  border-radius: 40rpx;
  padding: 24rpx 0;
  font-size: 30rpx;
  font-weight: 600;
}
</style>

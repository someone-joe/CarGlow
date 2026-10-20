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
  background: rgba(0, 0, 0, 0.4);
  z-index: 100;
  animation: qp-fade 0.25s ease;
}

@keyframes qp-fade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.dialog {
  position: fixed;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 101;
  box-sizing: border-box;
  width: 560rpx;
  max-height: 70vh;
  padding: 48rpx 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-raised);
  animation: qp-rise 0.25s ease;
}

@keyframes qp-rise {
  from {
    transform: translate(-50%, -46%);
    opacity: 0.5;
  }
  to {
    transform: translate(-50%, -50%);
    opacity: 1;
  }
}

.dlg-title {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.dlg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
}

.dlg-ws {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.dlg-ahead {
  font-size: var(--cg-font-caption);
  color: var(--cg-primary);
}

.dlg-list {
  margin-top: 24rpx;
}

.dlg-empty {
  padding: 32rpx 0;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-aux);
  text-align: center;
}

.dlg-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.dlg-main {
  flex: 1;
}

.dlg-plate {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.dlg-svc {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.dlg-side {
  flex: none;
  text-align: right;
}

.dlg-tag {
  padding: 8rpx 24rpx;
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
}

.dlg-tag.serving {
  background: rgba(52, 199, 89, 0.12);
  color: var(--cg-success);
}

.dlg-tag.queuing {
  background: rgba(255, 149, 0, 0.12);
  color: var(--cg-warning);
}

.dlg-min {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

.dlg-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 32rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}
</style>

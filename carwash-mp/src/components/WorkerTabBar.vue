<template>
  <view class="wtab">
    <view :class="['wtab-item', current === 'tasks' && 'on']" @click="go('/pages/worker-tasks/worker-tasks')">
      <text class="wtab-ico">🧰</text>
      <text class="wtab-text">任务池</text>
    </view>
    <view :class="['wtab-item', current === 'station' && 'on']" @click="go('/pages/worker-station/worker-station')">
      <text class="wtab-ico">🚿</text>
      <text class="wtab-text">作业台</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{ current: 'tasks' | 'station' }>()

/**
 * 师傅端底部导航。
 * 为什么不用原生 tabBar：uni-app 的 tabBar 在 pages.json 里编译期固定，
 * 无法在运行时按角色（顾客 / 师傅）切换，且 C 端已有 4 个 tab 不能覆盖。
 * 故师傅端页面不走 tabBar，用本组件保持底部导航一致，切换用 redirectTo。
 */
function go(url: string): void {
  const target = url.includes('station') ? 'station' : 'tasks'
  if (target === props.current) {
    return
  }
  uni.redirectTo({ url })
}
</script>

<style>
.wtab {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  background: #fff;
  border-top: 1rpx solid #eee;
  padding: 14rpx 0;
  z-index: 10;
}

.wtab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.wtab-ico {
  font-size: 40rpx;
}

.wtab-text {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: #888;
}

.wtab-item.on .wtab-text {
  color: #1a73e8;
  font-weight: 600;
}
</style>

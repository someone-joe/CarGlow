<template>
  <view class="wtab">
    <view :class="['wtab-item', current === 'tasks' && 'on']" @click="go('/pages/worker-tasks/worker-tasks')">
      <view class="wtab-ico wtab-ico-tasks" />
      <text class="wtab-text">任务池</text>
    </view>
    <view :class="['wtab-item', current === 'station' && 'on']" @click="go('/pages/worker-station/worker-station')">
      <view class="wtab-ico wtab-ico-station" />
      <text class="wtab-text">作业台</text>
    </view>
    <view :class="['wtab-item', current === 'mine' && 'on']" @click="go('/pages/worker-mine/worker-mine')">
      <view class="wtab-ico wtab-ico-mine" />
      <text class="wtab-text">我的</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{ current: 'tasks' | 'station' | 'mine' }>()

/**
 * 师傅端底部导航。
 * 为什么不用原生 tabBar：uni-app 的 tabBar 在 pages.json 里编译期固定，
 * 无法在运行时按角色（顾客 / 师傅）切换，且 C 端已有 4 个 tab 不能覆盖。
 * 故师傅端页面不走 tabBar，用本组件保持底部导航一致，切换用 redirectTo。
 */
function go(url: string): void {
  const target = url.includes('station') ? 'station' : url.includes('mine') ? 'mine' : 'tasks'
  if (target === props.current) {
    return
  }
  uni.redirectTo({ url })
}
</script>

<style>
/* 师傅端底部导航：轻毛玻璃 */
.wtab {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  box-sizing: border-box;
  padding: 16rpx 40rpx calc(16rpx + env(safe-area-inset-bottom));
  background: var(--cg-glass-bg);
  -webkit-backdrop-filter: blur(var(--cg-glass-blur));
  backdrop-filter: blur(var(--cg-glass-blur));
  border-top: var(--cg-glass-border);
  box-shadow: 0 -4rpx 40rpx rgba(0, 0, 0, 0.06);
}

.wtab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8rpx 0;
}

/* 线性小图标（SF 风格），替代 emoji */
.wtab-ico {
  position: relative;
  width: 40rpx;
  height: 40rpx;
  border: 2rpx solid var(--cg-text-aux);
  border-radius: 10rpx;
}

.wtab-ico-tasks {
  border-radius: 10rpx;
}

.wtab-ico-tasks::after {
  content: '';
  position: absolute;
  left: 8rpx;
  top: 8rpx;
  width: 16rpx;
  height: 2rpx;
  background: var(--cg-text-aux);
  box-shadow: 0 8rpx 0 var(--cg-text-aux);
}

.wtab-ico-station {
  border: none;
  border-radius: 50%;
  background: var(--cg-text-aux);
}

.wtab-ico-station::after {
  content: '';
  position: absolute;
  left: 14rpx;
  top: 8rpx;
  width: 8rpx;
  height: 16rpx;
  border: 2rpx solid var(--cg-card);
  border-radius: var(--cg-radius-pill);
}

.wtab-ico-mine {
  border: none;
  border-radius: 50%;
  background: var(--cg-text-aux);
}

.wtab-ico-mine::after {
  content: '';
  position: absolute;
  left: 12rpx;
  top: 12rpx;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: var(--cg-card);
}

.wtab-text {
  margin-top: 8rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.wtab-item.on .wtab-ico {
  border-color: var(--cg-primary);
}

.wtab-item.on .wtab-ico-tasks::after,
.wtab-item.on .wtab-ico-station,
.wtab-item.on .wtab-ico-mine {
  background: var(--cg-primary);
}

.wtab-item.on .wtab-ico-station::after,
.wtab-item.on .wtab-ico-mine::after {
  border-color: var(--cg-card);
}

.wtab-item.on .wtab-text {
  color: var(--cg-primary);
}
</style>

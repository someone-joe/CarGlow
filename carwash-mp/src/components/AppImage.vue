<template>
  <!-- 加载成功：直接渲染 image，保持原标签的 class/size 语义 -->
  <image
    v-if="!failed"
    :src="src"
    :mode="mode"
    :class="imgClass"
    @error="failed = true"
    @load="failed = false"
  />
  <!-- 加载失败：降级为灰底占位，避免白图；尺寸沿用原 class -->
  <view v-else class="app-image-ph" :class="imgClass">
    <text class="app-image-ph__icon">🖼</text>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  src?: string
  /** 与原生 image 的 mode 一致：aspectFill / aspectFit / scaleToFill ... */
  mode?: 'aspectFill' | 'aspectFit' | 'scaleToFill' | 'widthFix' | string
  /** 透传原 image 的 class，保证占位态与原图尺寸/样式一致 */
  imgClass?: string | string[] | Record<string, boolean>
}>()

const failed = ref(!props.src)

watch(
  () => props.src,
  (v) => {
    failed.value = !v
  }
)
</script>

<style>
.app-image-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eceff3;
}
.app-image-ph__icon {
  font-size: 28rpx;
  opacity: 0.5;
}
</style>

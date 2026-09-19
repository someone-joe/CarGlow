<template>
  <view class="page">
    <view class="panel">
      <text class="panel-title">这次服务还满意吗？</text>

      <view class="stars">
        <text
          v-for="n in 5"
          :key="n"
          class="star"
          :class="{ 'star-on': n <= rating }"
          @click="rating = n"
        >★</text>
      </view>
      <text class="rating-text">{{ ratingText }}</text>

      <view class="tags">
        <text
          v-for="t in TAGS"
          :key="t"
          class="tag"
          :class="{ 'tag-on': tags.includes(t) }"
          @click="toggleTag(t)"
        >{{ t }}</text>
      </view>

      <textarea
        class="textarea"
        v-model="content"
        :placeholder="rating > 0 && rating < 5 ? '请说明原因，便于我们改进（必填）' : '说说你的体验（选填）'"
      />

      <view class="btn" @click="submit">{{ submitting ? '提交中…' : '提交评价' }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { submitReview } from '@/api/order'

/** 标签取值与契约 reviews.tags 示例一致，前端不新增取值 */
const TAGS = ['洗得干净', '时效快', '服务态度好', '取送准时']

const orderNo = ref('')
const rating = ref(0)
const tags = ref<string[]>([])
const content = ref('')
const submitting = ref(false)

const ratingText = computed(() => {
  return ['', '很不满意', '不满意', '一般', '满意', '非常满意'][rating.value] ?? ''
})

function toggleTag(tag: string): void {
  if (tags.value.includes(tag)) {
    tags.value = tags.value.filter((t) => t !== tag)
  } else {
    tags.value = [...tags.value, tag]
  }
}

async function submit(): Promise<void> {
  if (rating.value < 1) {
    uni.showToast({ title: '请先选择星级', icon: 'none' })
    return
  }
  // 与后端一致：4 星及以下必须填原因
  if (rating.value < 5 && !content.value.trim()) {
    uni.showToast({ title: '4 星及以下请说明原因', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await submitReview(orderNo.value, {
      rating: rating.value,
      tags: tags.value,
      content: content.value.trim() || undefined
    })
    uni.showToast({ title: '评价成功', icon: 'none' })
    uni.navigateBack()
  } catch {
    // request 层已统一提示（如已评价）
  } finally {
    submitting.value = false
  }
}

onLoad((options) => {
  orderNo.value = (options as { orderNo?: string }).orderNo ?? ''
})
</script>

<style>
.page {
  padding: 24rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.panel {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}

.panel-title {
  display: block;
  font-size: 34rpx;
  color: #222;
  font-weight: 500;
  text-align: center;
}

.stars {
  display: flex;
  justify-content: center;
  margin-top: 32rpx;
}

.star {
  font-size: 64rpx;
  color: #dcdfe6;
  padding: 0 12rpx;
}

.star-on {
  color: #f7ba2a;
}

.rating-text {
  display: block;
  text-align: center;
  font-size: 28rpx;
  color: #888;
  margin-top: 12rpx;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 40rpx;
}

.tag {
  padding: 14rpx 28rpx;
  border: 2rpx solid #ddd;
  border-radius: 999rpx;
  font-size: 26rpx;
  color: #666;
  margin: 0 16rpx 16rpx 0;
}

.tag-on {
  border-color: #1a73e8;
  color: #1a73e8;
  background: #f2f7ff;
}

.textarea {
  width: 100%;
  box-sizing: border-box;
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 22rpx;
  font-size: 28rpx;
  margin-top: 16rpx;
  height: 180rpx;
}

.btn {
  margin-top: 40rpx;
  background: #1a73e8;
  color: #fff;
  text-align: center;
  padding: 26rpx 0;
  border-radius: 16rpx;
  font-size: 32rpx;
}
</style>

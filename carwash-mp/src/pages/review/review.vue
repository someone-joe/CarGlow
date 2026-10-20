<template>
  <view class="cg-page review">
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

      <view class="btn" :class="{ disabled: submitting }" @click="submit">
        {{ submitting ? '提交中…' : '提交评价' }}
      </view>
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
.review {
  padding-top: 32rpx;
}

/* ---- 评价面板 ---- */
.panel {
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

/* ---- 星级 ---- */
.stars {
  display: flex;
  justify-content: center;
  margin-top: 32rpx;
}

.star {
  margin: 0 16rpx;
  font-size: 64rpx;
  color: var(--cg-line);
  line-height: 1;
}

.star-on {
  color: var(--cg-warning);
}

.rating-text {
  display: block;
  margin-top: 24rpx;
  text-align: center;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

/* ---- 标签 ---- */
.tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 32rpx;
}

.tag {
  margin: 0 16rpx 16rpx 0;
  padding: 16rpx 32rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.tag-on {
  background: var(--cg-tint);
  color: var(--cg-primary);
}

/* ---- 输入 ---- */
.textarea {
  box-sizing: border-box;
  width: 100%;
  margin-top: 16rpx;
  padding: 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

/* ---- 提交 ---- */
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 48rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.btn.disabled {
  opacity: 0.4;
}
</style>

<template>
  <view class="plate-input">
    <!-- 格子显示区：点击弹出键盘 -->
    <view class="cells" @tap="open">
      <view
        v-for="i in 7"
        :key="i"
        :class="['cell', activeIndex === i - 1 ? 'active' : '']"
      >{{ chars[i - 1] || '' }}</view>
      <!-- 新能源第 8 格：已填则显示字符，否则显示为提示格 -->
      <view
        v-if="chars[7]"
        :class="['cell', activeIndex === 7 ? 'active' : '']"
      >{{ chars[7] }}</view>
      <view
        v-else
        :class="['cell', 'new-energy', activeIndex === 7 ? 'active' : '', chars.length < 7 ? 'disabled' : '']"
      >新</view>
    </view>

    <!-- 底部滑出自定义键盘 -->
    <view v-if="showKeyboard" class="mask" @tap="close" />
    <view :class="['kb', showKeyboard ? 'show' : '']">
      <view class="kb-bar">
        <text class="kb-preview">{{ chars.join('') || '请输入车牌' }}</text>
        <text class="kb-done" @tap="close">完成</text>
      </view>
      <view class="kb-body">
        <!-- 省份键盘：仅在尚无字符时 -->
        <view v-if="chars.length < 1" class="province">
          <view v-for="(row, ri) in provinceRows" :key="ri" class="kb-row">
            <view v-for="(k, ki) in row" :key="ki" class="key" @tap="onKey(k)">{{ k }}</view>
          </view>
        </view>
        <!-- 数字 / 字母键盘 -->
        <view v-else class="alnum">
          <view class="kb-row">
            <view
              v-for="(k, ki) in '0123456789'.split('')"
              :key="ki"
              :class="['key', chars.length < 2 ? 'disabled' : '']"
              @tap="onKey(k)"
            >{{ k }}</view>
          </view>
          <view v-for="(row, ri) in letterRows" :key="'L' + ri" class="kb-row">
            <view
              v-for="(k, ki) in row"
              :key="ki"
              :class="['key', isSpecial(k) && chars.length < 2 ? 'disabled' : '']"
              @tap="onKey(k)"
            >{{ k }}</view>
          </view>
          <view class="kb-row">
            <view class="key delete" @tap="onDelete">⌫</view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

// 省份简称（SSOT 与车牌规则一致；首格限定为省份）
const PROVINCE = '京沪粤津冀晋蒙辽吉黑苏浙皖闽赣鲁豫鄂湘云贵川桂琼渝藏台陕甘青宁新'
const provinceRows = [
  '京沪粤津冀晋蒙辽吉黑',
  '苏浙皖闽赣鲁豫鄂湘',
  '云贵川桂琼渝藏台',
  '陕甘青宁新',
]
// 字母键盘：普通字母 + 港澳学警（仅第 3 位起可用）
const letterRows = ['ABCDEFGHJK', 'LMNPQRSTUV', 'WXYZ港澳学警']
const SPECIAL = ['港', '澳', '学', '警']

const chars = ref<string[]>((props.modelValue ?? '').slice(0, 8).split(''))
const showKeyboard = ref(false)

const activeIndex = computed(() => chars.value.length)

function isProvince(k: string): boolean {
  return PROVINCE.includes(k)
}

function isSpecial(k: string): boolean {
  return SPECIAL.includes(k)
}

function sync(): void {
  emit('update:modelValue', chars.value.join(''))
}

function open(): void {
  showKeyboard.value = true
}

function close(): void {
  showKeyboard.value = false
}

function onKey(k: string): void {
  if (chars.value.length >= 8) return
  const len = chars.value.length
  if (len === 0) {
    if (!isProvince(k)) return
  } else if (len === 1) {
    // 第 2 位必须是普通字母（数字与港澳学警均不允许）
    if (!/^[A-Z]$/.test(k)) return
  }
  chars.value = [...chars.value, k]
  sync()
}

function onDelete(): void {
  if (!chars.value.length) return
  chars.value = chars.value.slice(0, -1)
  sync()
}

watch(
  () => props.modelValue,
  (v) => {
    const next = (v ?? '').slice(0, 8).split('')
    if (next.join('') !== chars.value.join('')) chars.value = next
  },
)
</script>

<style scoped>
.plate-input {
  width: 100%;
}

/* ---- 格子显示区 ---- */
.cells {
  display: flex;
  gap: 8rpx;
  width: 100%;
}

.cell {
  flex: 1;
  height: 84rpx;
  min-width: 0;
  border: 2rpx solid #d7e3e1;
  border-radius: 12rpx;
  background: #f6faf9;
  text-align: center;
  line-height: 84rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: #1b2b2a;
}

.cell.active {
  border-color: #00aeb5;
  background: #f3fbfb;
}

.cell.new-energy {
  color: #9fb3b0;
  font-size: 26rpx;
  font-weight: 600;
}

.cell.new-energy.disabled {
  border-style: dashed;
  color: #c4d2cf;
}

/* ---- 底部键盘 ---- */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 9998;
}

.kb {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: #fff;
  border-radius: 30rpx 30rpx 0 0;
  border-top: 2rpx solid #d9d9d9;
  transform: translateY(100%);
  transition: transform 0.3s ease;
  padding-bottom: env(safe-area-inset-bottom);
}

.kb.show {
  transform: translateY(0);
}

.kb-bar {
  position: relative;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 2rpx solid #f0f4f3;
}

.kb-preview {
  font-size: 34rpx;
  font-weight: 700;
  color: #1b2b2a;
  letter-spacing: 2rpx;
}

.kb-done {
  position: absolute;
  right: 28rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 30rpx;
  color: #00aeb5;
}

.kb-body {
  padding: 16rpx 16rpx 24rpx;
}

.kb-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.key {
  flex: 1;
  min-width: 0;
  height: 92rpx;
  border: 2rpx solid #e2e8e7;
  border-radius: 12rpx;
  background: #fff;
  text-align: center;
  line-height: 92rpx;
  font-size: 34rpx;
  color: #1b2b2a;
}

.key.disabled {
  color: #c4d2cf;
  background: #f3f6f5;
  border-color: #eef2f1;
}

.key.delete {
  flex: 1.6;
  background: #eef2f1;
  font-size: 40rpx;
  color: #6b7b79;
}
</style>

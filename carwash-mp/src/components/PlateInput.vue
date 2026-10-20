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
  box-sizing: border-box;
  margin-top: 32rpx;
}

/* ---- 车牌格 ---- */
.cells {
  display: flex;
}

.cell {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 84rpx;
  height: 96rpx;
  margin-right: 8rpx;
  background: var(--cg-fill);
  border: 2rpx solid transparent;
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.cell.active {
  border-color: var(--cg-primary);
  background: var(--cg-tint);
}

.cell.new-energy {
  background: var(--cg-card);
  border: 1rpx dashed var(--cg-border);
  color: var(--cg-text-aux);
}

.cell.new-energy.disabled {
  opacity: 0.5;
}

/* ---- 自定义键盘 ---- */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 100;
  animation: pi-fade 0.25s ease;
}

@keyframes pi-fade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.kb {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 101;
  box-sizing: border-box;
  padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
  background: var(--cg-glass-bg);
  -webkit-backdrop-filter: blur(var(--cg-glass-blur));
  backdrop-filter: blur(var(--cg-glass-blur));
  border-top: var(--cg-glass-border);
  transform: translateY(100%);
  transition: transform 0.25s ease;
}

.kb.show {
  transform: translateY(0);
}

.kb-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
}

.kb-preview {
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
  letter-spacing: 4rpx;
}

.kb-done {
  min-height: 96rpx;
  padding: 0 16rpx;
  color: var(--cg-primary);
  font-size: var(--cg-font-caption);
  line-height: 96rpx;
}

.kb-body {
  padding-top: 16rpx;
}

.kb-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.key {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 108rpx;
  height: 96rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-md);
  box-shadow: var(--cg-shadow-card);
  font-size: var(--cg-font-body);
  color: var(--cg-title);
}

.key.disabled {
  background: var(--cg-fill);
  box-shadow: none;
  color: var(--cg-text-aux);
}

.key.delete {
  background: var(--cg-fill);
  box-shadow: none;
  color: var(--cg-text-sub);
}
</style>

<template>
  <!--
    首页 Banner：图片交叉淡化 + 文字整块从右往左划入；支持左右拖动（循环）；分段进度条。
    独立成组件的原因：拖动时每帧都在更新样式，若挂在首页上会带着整页一起 setData 重渲染，
    低端机上慢拖会明显卡顿；抽成组件后重渲染范围仅限 Banner 自身。
  -->
  <view
    class="home-banner"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <view
      v-for="(s, i) in slides"
      :key="s.bannerId ?? i"
      class="banner-slide"
      :class="{ active: i === bannerIndex, 'is-text': !s.imageUrl }"
      :style="slideStyle(i)"
    >
      <app-image v-if="s.imageUrl" class="banner-img" :src="s.imageUrl" mode="aspectFill" />
      <view v-else class="banner-fallback">
        <view class="banner-fallback-art">
          <view class="cg-art-car">
            <view class="art-cabin" />
            <view class="art-body" />
            <view class="art-wheel w-back" />
            <view class="art-wheel w-front" />
          </view>
          <view class="cg-art-drop banner-drop-1" />
          <view class="cg-art-drop banner-drop-2" />
        </view>
      </view>
      <view v-if="s.imageUrl" class="banner-scrim" />
      <view class="banner-text">
        <text class="banner-title">{{ slideTitle(s) }}</text>
        <text v-if="slideSub(s)" class="banner-sub">{{ slideSub(s) }}</text>
        <view v-if="hasCta(s)" class="banner-cta">点此了解详情 ›</view>
      </view>
    </view>
    <!-- 进度条：分段式，默认隐藏；手动拖动切换时淡入，静止后自动隐藏（苹果音量 HUD 模式） -->
    <view class="banner-progress" :class="{ show: progressVisible, 'is-text': currentIsText }">
      <view v-for="(s, i) in slides" :key="i" class="banner-seg">
        <view class="banner-seg-fill" :style="{ width: segFill(i) }" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

/** 后台 banner 契约结构；无图时 C 端渲染「标题+副标题」文字片 */
export interface Slide {
  bannerId?: number
  imageUrl?: string
  title?: string
  desc?: string
  linkType?: string
  linkValue?: string
}

const props = defineProps<{
  slides: Slide[]
  /** 页面是否可见：不可见时暂停轮播，避免后台空转与定时器泄漏 */
  active?: boolean
}>()

const emit = defineEmits<{
  /** 点击当前片（父页按 linkType 分流跳转） */
  tap: [slide: Slide]
}>()

/** 轮播间隔：与原 swiper autoplay 一致；纯展示节奏，不涉及业务阈值 */
const BANNER_INTERVAL_MS = 4000

/** 手动拖动提交阈值：位移超过容器宽度 25% 才切换，否则回弹（交互常量，非业务阈值） */
const BANNER_SWITCH_RATIO = 0.25

/** 进度条自动隐藏延时：类苹果音量 HUD，交互静止后淡出（交互常量） */
const BANNER_PROGRESS_HIDE_MS = 1600

const bannerIndex = ref(0)
let bannerTimer: number | undefined

/** Banner 通栏宽度（px）：负边距抵消页面左右边距，等于屏幕宽度 */
const bannerWidth = ref(375)

/** 拖动态：dx 为当前横向位移（px）；active 表示已判定为横向手势 */
const bannerDragDx = ref(0)
const bannerDragActive = ref(false)
let touchStartX = 0
let touchStartY = 0
let touchTracking = false
let touchHorizontal = false
let touchVertical = false

/** 进度条：默认隐藏，仅手动拖动时可见 */
const progressVisible = ref(false)
let progressTimer: number | undefined

/** 当前片是否为文字片（无图）：决定进度条配色，保证浅色/图片底上都可见 */
const currentIsText = computed(() => !props.slides[bannerIndex.value]?.imageUrl)

onMounted(() => {
  try {
    // Banner 通栏（负边距抵消页面边距），宽度即屏宽；拖动跟手比例按它换算
    bannerWidth.value = uni.getWindowInfo().windowWidth || 375
  } catch {
    bannerWidth.value = 375
  }
  if (props.active !== false) startBannerAuto()
})

onUnmounted(() => {
  stopBannerAuto()
  clearProgressTimer()
})

// 页面隐藏（跳转/切后台）时停轮播与进度条计时；回页恢复
watch(
  () => props.active,
  (active) => {
    if (active === false) {
      stopBannerAuto()
      clearProgressTimer()
      progressVisible.value = false
    } else {
      startBannerAuto()
    }
  }
)

// 数据刷新后重置到第一张，避免索引越界；列表长度变化时重启轮播
watch(
  () => props.slides,
  () => {
    bannerIndex.value = 0
    startBannerAuto()
  }
)

function startBannerAuto(): void {
  stopBannerAuto()
  if (props.slides.length <= 1) return
  bannerTimer = setInterval(() => {
    bannerIndex.value = (bannerIndex.value + 1) % props.slides.length
  }, BANNER_INTERVAL_MS)
}

function stopBannerAuto(): void {
  if (bannerTimer != null) {
    clearInterval(bannerTimer)
    bannerTimer = undefined
  }
}

/** 进度条淡入后延时自动隐藏（苹果音量 HUD：交互出现、静止消失） */
function scheduleProgressHide(): void {
  if (progressTimer != null) clearTimeout(progressTimer)
  progressTimer = setTimeout(() => {
    progressVisible.value = false
  }, BANNER_PROGRESS_HIDE_MS)
}

function clearProgressTimer(): void {
  if (progressTimer != null) {
    clearTimeout(progressTimer)
    progressTimer = undefined
  }
}

/**
 * 分段填充宽度：静止时当前段满、其余空；
 * 拖动时当前段随淡出收缩、目标段随淡入展开——与图片交叉淡化的状态严格对应。
 * 循环模式下向左拖目标为下一片、向右拖目标为上一片（末片→首片、首片→末片）。
 */
function segFill(j: number): string {
  const total = props.slides.length
  if (total <= 1) return '100%'
  const from = bannerIndex.value
  let t = 0
  if (bannerDragActive.value) {
    const p = bannerDragDx.value / bannerWidth.value
    if (p < 0) t = Math.min(1, Math.abs(p))
    else if (p > 0) t = Math.min(1, p)
  }
  if (t === 0) return j === from ? '100%' : '0%'
  const to = bannerDragDx.value < 0 ? (from + 1) % total : (from - 1 + total) % total
  if (j === from) return `${(1 - t) * 100}%`
  if (j === to) return `${t * 100}%`
  return '0%'
}

/** 文案兜底：后台未填标题时不出现空白片 */
function slideTitle(s: Slide): string {
  return s.title || 'CarGlow 夜间代客洗车'
}

function slideSub(s: Slide): string {
  return s.desc || ''
}

/** 有落地页的片才显示 CTA；ACTIVITY 暂无详情页，不诱导点击 */
function hasCta(s: Slide): boolean {
  return s.linkType === 'RECHARGE' || s.linkType === 'SERVICE'
}

/**
 * 片子样式：
 * - 静止态：opacity 与 transform 同步过渡（iOS 缓动曲线 0.8s，按反馈由 0.6s 调慢）。
 *   transform 必须一起过渡——否则拖动产生的位移会在松手瞬间弹回，是「切换生硬」的根因。
 * - 拖动态：不加任何过渡，样式即时生效，严格跟手不滞后。
 *   曾加的 12ms 微过渡在慢拖时会持续「追逐」上一帧目标，正是慢拖卡顿的元凶。
 * - 循环：目标片按取模计算，末片的下一张是首片、首片的上一张是末片，永不落空。
 */
function slideStyle(i: number): { opacity: string; transform: string; zIndex: number; transition: string } {
  const total = props.slides.length
  if (!bannerDragActive.value) {
    const isCurrent = i === bannerIndex.value
    return {
      opacity: isCurrent ? '1' : '0',
      transform: 'translateX(0)',
      zIndex: isCurrent ? 2 : 1,
      transition: 'opacity 0.8s cubic-bezier(0.32, 0.72, 0, 1), transform 0.8s cubic-bezier(0.32, 0.72, 0, 1)',
    }
  }
  const dx = bannerDragDx.value
  const p = dx / bannerWidth.value
  const isCurrent = i === bannerIndex.value
  const target = p > 0 ? (bannerIndex.value - 1 + total) % total : (bannerIndex.value + 1) % total
  const hasTarget = total > 1
  if (isCurrent) {
    // 循环下永远有邻片；仅剩 1 片时保留轻微橡皮筋阻尼
    const resist = hasTarget ? 0.3 : 0.15
    return {
      opacity: String(hasTarget ? Math.max(0, 1 - Math.abs(p)) : 1 - Math.abs(p) * 0.3),
      transform: `translateX(${dx * resist}px)`,
      zIndex: 2,
      transition: 'none',
    }
  }
  if (i === target) {
    return {
      opacity: String(Math.min(1, Math.abs(p))),
      transform: `translateX(${dx * 0.3}px)`,
      zIndex: 1,
      transition: 'none',
    }
  }
  return { opacity: '0', transform: 'translateX(0)', zIndex: 0, transition: 'none' }
}

interface TouchLike {
  clientX: number
  clientY: number
}

function onTouchStart(e: Event): void {
  const t = (e as unknown as { touches?: TouchLike[] }).touches?.[0]
  if (!t) return
  touchStartX = t.clientX
  touchStartY = t.clientY
  touchTracking = true
  touchHorizontal = false
  touchVertical = false
  bannerDragActive.value = false
  bannerDragDx.value = 0
  stopBannerAuto()
}

function onTouchMove(e: Event): void {
  if (!touchTracking) return
  const t = (e as unknown as { touches?: TouchLike[] }).touches?.[0]
  if (!t) return
  const dx = t.clientX - touchStartX
  const dy = t.clientY - touchStartY
  // 手势判定：横向意图明确才接管，否则交还页面纵向滚动，避免互相打断
  if (!touchHorizontal && !touchVertical) {
    if (Math.abs(dx) < 8 && Math.abs(dy) < 8) return
    if (Math.abs(dx) > Math.abs(dy)) {
      touchHorizontal = true
      progressVisible.value = true // 手动交互开始，进度条淡入
    } else {
      touchVertical = true
      touchTracking = false
      startBannerAuto()
      return
    }
  }
  if (!touchHorizontal) return
  bannerDragActive.value = true
  bannerDragDx.value = dx
}

function onTouchEnd(): void {
  const wasHorizontal = touchHorizontal
  const wasVertical = touchVertical
  const dx = bannerDragDx.value
  const wasDrag = bannerDragActive.value
  touchTracking = false
  touchHorizontal = false
  touchVertical = false
  bannerDragActive.value = false
  bannerDragDx.value = 0
  if (!wasDrag) {
    // 未形成横向拖动：竖向滚动已交还页面，不能再当点击；位移极小且非竖向手势才是点击当前片
    if (!wasHorizontal && !wasVertical && Math.abs(dx) < 10) {
      const current = props.slides[bannerIndex.value]
      if (current) emit('tap', current)
    }
    startBannerAuto()
    return
  }
  const ratio = Math.abs(dx) / bannerWidth.value
  if (ratio >= BANNER_SWITCH_RATIO) {
    // 循环切换：末片左滑回首片、首片右滑到末片
    const total = props.slides.length
    bannerIndex.value = (bannerIndex.value + (dx < 0 ? 1 : -1) + total) % total
  }
  scheduleProgressHide()
  startBannerAuto()
}
</script>

<style scoped>
.home-banner {
  position: relative;
  height: 520rpx;
  margin: 0 -40rpx 32rpx;
  overflow: hidden;
}

/*
 * 交叉淡化：所有片常驻 DOM，active 片置顶。
 * 切换时前一张 opacity 1→0、后一张 0→1 同时进行（同时长同缓动），
 * 因此过渡自然、无闪烁、无跳动；禁止改成位移/缩放类轮播动效。
 */
.banner-slide {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  z-index: 1;
}

.banner-slide.active {
  opacity: 1;
  z-index: 2;
}

.banner-img {
  width: 100%;
  height: 100%;
}

/* 无图文字片：低饱和浅色渐变 + 洗车插图，不使用营销大字 */
.banner-fallback {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(160deg, #e8f1fd 0%, #f5f5f7 62%);
}

.banner-fallback-art {
  position: absolute;
  right: 40rpx;
  top: 88rpx;
  width: 240rpx;
  height: 96rpx;
}

.banner-drop-1 {
  left: 8rpx;
  top: -32rpx;
}

.banner-drop-2 {
  left: 72rpx;
  top: -56rpx;
}

/* 图片片底部暗 scrim：仅为保证文字可读性的功能性渐变 */
.banner-scrim {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 300rpx;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.45) 100%);
}

.banner-text {
  position: absolute;
  left: 40rpx;
  right: 40rpx;
  bottom: 72rpx;
}

/*
 * 文字滚动入场：图片先交叉淡化出场，文字随后以恒定速度滚入。
 * - delay 0.3s：图片先动，文字后到，形成先后顺序而不是齐刷刷一起出；
 * - 0.7s + linear：速度恒定、可感知但不拖沓（原 0.9s 偏慢，按反馈调快）；
 * - fill-mode both：延时期间文字停在右侧画外，不会先闪现在终态再跳走。
 */
.banner-slide.active .banner-text {
  animation: banner-text-in 0.7s linear 0.3s both;
}

@keyframes banner-text-in {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.banner-title {
  display: block;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: #ffffff;
}

.banner-slide.is-text .banner-title {
  color: var(--cg-title);
}

.banner-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  color: rgba(255, 255, 255, 0.85);
}

.banner-slide.is-text .banner-sub {
  color: var(--cg-text-sub);
}

.banner-cta {
  display: inline-flex;
  align-items: center;
  height: 64rpx;
  margin-top: 32rpx;
  padding: 0 32rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: #ffffff;
}

/*
 * 进度条：分段式短横条（段与段之间有明显分割），左右居中，距 banner 底边 32rpx。
 * 默认隐藏；手动拖动切换时淡入，静止 1.6s 后自动隐藏（苹果音量 HUD 模式）。
 */
.banner-progress {
  position: absolute;
  left: 50%;
  bottom: 32rpx;
  transform: translateX(-50%);
  z-index: 3;
  display: flex;
  align-items: center;
  width: 240rpx;
  height: 8rpx;
  opacity: 0;
  transition: opacity 0.28s ease;
  pointer-events: none;
}

.banner-progress.show {
  opacity: 1;
}

/* 单段：左右各 4rpx 外边距形成 8rpx 分割间隙，段内填充溢出隐藏 */
.banner-seg {
  position: relative;
  flex: 1;
  height: 8rpx;
  margin: 0 4rpx;
  background: rgba(255, 255, 255, 0.35);
  border-radius: var(--cg-radius-pill);
  overflow: hidden;
}

.banner-seg-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: var(--cg-radius-pill);
  background: #ffffff;
}

/* 文字片（浅色底）上用主色填充 + 极淡深色轨道，保证可见且不抢视觉 */
.banner-progress.is-text .banner-seg {
  background: rgba(0, 0, 0, 0.10);
}

.banner-progress.is-text .banner-seg-fill {
  background: var(--cg-primary);
}
</style>

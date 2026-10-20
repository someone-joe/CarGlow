<template>
  <view class="cg-page customer-service">
    <view v-if="loading" class="cg-empty">加载中…</view>

    <block v-else>
      <!-- 专属管家（企微二维码由后台配置下发，未配置不显示） -->
      <view v-if="cfg.wecomQrcodeUrl" class="card">
        <text class="card-title">专属管家</text>
        <image class="qr" :src="cfg.wecomQrcodeUrl" mode="aspectFit" @longpress="saveQr" />
        <text class="qr-tip">长按二维码添加管家企业微信</text>
      </view>

      <!-- 常见问题：标签网格（来自后台「客服配置」，不再前端硬编码） -->
      <view class="card">
        <text class="card-title">您可能会遇到的问题</text>
        <view class="faq">
          <view v-for="(f, i) in faqs" :key="i" class="faq-item" @click="ask(f)">{{ f.question }}</view>
        </view>
        <view v-if="!faqs.length" class="muted">暂无常见问题</view>
      </view>

      <!-- 服务电话：平台 + 本站点（号码全部后台配置，前端不硬编码） -->
      <view class="card">
        <text class="card-title">服务电话</text>
        <view v-if="cfg.nightTip" class="night">{{ cfg.nightTip }}</view>
        <view v-for="(p, i) in phones" :key="i" class="phone" @click="call(p)">
          <view class="phone-main">
            <text class="phone-num">{{ p }}</text>
            <text class="phone-label">{{ i === 0 ? '平台客服' : '本站点专属' }}</text>
          </view>
          <text class="dial">拨打</text>
        </view>
        <view v-if="!phones.length" class="muted">暂无客服电话</view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchCustomerService, type CustomerServiceVO, type CsFaq } from '@/api/config'

/** 常见问题来自后台「客服配置」，前端不再硬编码（PRD：问题清单由运营维护） */
const faqs = ref<CsFaq[]>([])

const cfg = ref<CustomerServiceVO>({})
const loading = ref(true)
const phones = ref<string[]>([])

async function load(): Promise<void> {
  loading.value = true
  try {
    const c = await fetchCustomerService()
    cfg.value = c
    faqs.value = c.faqs && c.faqs.length ? c.faqs : []
    phones.value = [c.platformPhone, c.stationPhone].filter((p): p is string => !!p)
  } catch {
    cfg.value = {}
    faqs.value = []
  } finally {
    loading.value = false
  }
}

function ask(f: CsFaq): void {
  // 后台配了答案则直接展示；否则弹通用占位（后续可一键转工单，自动带类型与订单号）
  uni.showModal({
    title: f.question,
    content: f.answer && f.answer.trim() ? f.answer : '已记录您的问题，正在为您匹配解答；如需人工，将自动生成工单。',
    showCancel: false,
    confirmText: '知道了',
  })
}

function call(p: string): void {
  uni.makePhoneCall({ phoneNumber: p })
}

function saveQr(): void {
  if (!cfg.value.wecomQrcodeUrl) return
  uni.saveImageToPhotosAlbum({
    filePath: cfg.value.wecomQrcodeUrl,
    success: () => uni.showToast({ title: '已保存二维码', icon: 'none' }),
    fail: () => uni.showToast({ title: '保存失败，请截图', icon: 'none' }),
  })
}

onShow(load)
</script>

<style>
/* ---- 卡片 ---- */
.card {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.card-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.qr {
  display: block;
  width: 320rpx;
  height: 320rpx;
  margin: 0 auto;
}

.qr-tip {
  display: block;
  margin-top: 24rpx;
  text-align: center;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.muted {
  padding: 32rpx 0;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-aux);
}

/* ---- FAQ 网格 ---- */
.faq {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -8rpx;
}

.faq-item {
  box-sizing: border-box;
  width: calc(50% - 16rpx);
  margin: 8rpx;
  padding: 32rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
  line-height: 1.5;
}

/* ---- 电话 ---- */
.night {
  margin-bottom: 24rpx;
  padding: 16rpx 24rpx;
  background: rgba(255, 149, 0, 0.10);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-note);
  color: var(--cg-warning);
}

.phone {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 108rpx;
  padding: 24rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.phone-main {
  flex: 1;
}

.phone-num {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.phone-label {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.dial {
  flex: none;
  min-height: 96rpx;
  padding: 0 8rpx;
  color: var(--cg-primary);
  font-size: var(--cg-font-caption);
  line-height: 96rpx;
}
</style>

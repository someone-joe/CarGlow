<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <!-- 专属管家（企微二维码由后台配置下发，未配置不显示） -->
      <view v-if="cfg.wecomQrcodeUrl" class="card">
        <text class="card-title">专属管家</text>
        <image class="qr" :src="cfg.wecomQrcodeUrl" mode="aspectFit" @longpress="saveQr" />
        <text class="qr-tip">长按二维码添加管家企业微信</text>
      </view>

      <!-- 常见问题：标签网格 -->
      <view class="card">
        <text class="card-title">您可能会遇到的问题</text>
        <view class="faq">
          <view v-for="(f, i) in faqs" :key="i" class="faq-item" @click="ask(f)">{{ f }}</view>
        </view>
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
import { fetchCustomerService, type CustomerServiceVO } from '@/api/config'

/** 常见问题标签：PRD P16 固定清单，后端暂未提供接口，前端按契约常量维护 */
const faqs = [
  '无法打开柜门',
  '忘记存放钥匙',
  '想修改订单',
  '咨询排队情况',
  '是否要取送车',
  '普洗精洗区别',
  '未按服务时间',
  '取消预约',
  '其他小区服务',
  '车主未按时存车',
  '具体位置',
  '最近折扣',
  '其他需求',
]

const cfg = ref<CustomerServiceVO>({})
const loading = ref(true)
const phones = ref<string[]>([])

async function load(): Promise<void> {
  loading.value = true
  try {
    const c = await fetchCustomerService()
    cfg.value = c
    phones.value = [c.platformPhone, c.stationPhone].filter((p): p is string => !!p)
  } catch {
    cfg.value = {}
  } finally {
    loading.value = false
  }
}

function ask(tag: string): void {
  // V1.0：点击标签弹出对应解答占位，后续一键转工单（自动带类型与订单号）
  uni.showModal({
    title: tag,
    content: '已记录您的问题，正在为您匹配解答；如需人工，将自动生成工单。',
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
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 80rpx 0;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.card-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 20rpx;
}

.qr {
  width: 260rpx;
  height: 260rpx;
  margin: 0 auto;
  display: block;
}

.qr-tip {
  display: block;
  text-align: center;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

/* 问题标签网格 */
.faq {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.faq-item {
  padding: 14rpx 24rpx;
  border-radius: 32rpx;
  background: #f2f7f7;
  font-size: 25rpx;
  color: #4a5a58;
}

.night {
  font-size: 24rpx;
  color: #8a9a98;
  padding-bottom: 16rpx;
}

.phone {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
}

.phone:last-of-type {
  border-bottom: none;
}

.phone-num {
  font-size: 32rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.phone-label {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

.dial {
  background: #14342f;
  color: #fff;
  font-size: 25rpx;
  padding: 12rpx 32rpx;
  border-radius: 32rpx;
}

.muted {
  color: #8a9a98;
  font-size: 25rpx;
  padding: 16rpx 0;
}
</style>

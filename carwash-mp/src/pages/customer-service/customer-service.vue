<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else>
      <view v-if="cfg.wecomQrcodeUrl" class="card">
        <text class="card-title">专属管家</text>
        <image class="qr" :src="cfg.wecomQrcodeUrl" mode="aspectFit" @longpress="saveQr" />
        <text class="qr-tip">长按二维码添加洗悦管家企业微信</text>
      </view>

      <view class="card">
        <text class="card-title">常见问题</text>
        <view class="faq">
          <view v-for="(f, i) in faqs" :key="i" class="faq-item" @click="ask(f)">{{ f }}</view>
        </view>
      </view>

      <view class="card">
        <text class="card-title">服务电话</text>
        <view v-if="cfg.nightTip" class="night">{{ cfg.nightTip }}</view>
        <view class="phone" v-for="(p, i) in phones" :key="i" @click="call(p)">
          <text>{{ p }}</text><text class="dial">拨打 ›</text>
        </view>
        <view v-if="!phones.length" class="muted">暂无客服电话</view>
      </view>

      <view class="card">
        <text class="card-title">在线客服</text>
        <view class="online" @click="online">
          <text>微信客服会话（机器人优先 + 人工兜底）</text><text class="dial">进入 ›</text>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup lang="ts">
import { onShow, ref } from '@dcloudio/uni-app'
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

function online(): void {
  // 微信客服会话：需后端在下单配置中提供 corpid + 客服链接，V1.0 暂未接入
  uni.showToast({ title: '在线客服接入中（需后台配置微信客服）', icon: 'none' })
}

onShow(load)
</script>

<style>
.page {
  padding: 24rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tip {
  text-align: center;
  color: #999;
  padding: 80rpx 0;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 20rpx;
}

.qr {
  width: 320rpx;
  height: 320rpx;
  margin: 0 auto;
  display: block;
  background: #f2f2f2;
}

.qr-tip {
  display: block;
  text-align: center;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #888;
}

.faq {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.faq-item {
  background: #f2f7ff;
  color: #1a73e8;
  font-size: 24rpx;
  padding: 12rpx 20rpx;
  border-radius: 24rpx;
}

.night {
  font-size: 24rpx;
  color: #9a6b00;
  background: #fff7e6;
  border-radius: 8rpx;
  padding: 12rpx 16rpx;
  margin-bottom: 16rpx;
}

.phone,
.online {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  font-size: 28rpx;
  color: #222;
  border-top: 1rpx solid #f2f2f2;
}

.dial {
  color: #1a73e8;
  font-size: 28rpx;
}

.muted {
  font-size: 26rpx;
  color: #999;
  padding: 12rpx 0;
}
</style>

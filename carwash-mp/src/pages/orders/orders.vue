<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        :class="['tab', activeTab === t.key && 'tab-active']"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
    </view>

    <view v-if="loading" class="tip">加载中…</view>

    <view v-else-if="list.length === 0" class="empty">
      <image class="empty-img" src="/static/logo.png" mode="aspectFit" />
      <text class="empty-title">暂无订单</text>
      <text class="empty-sub">洗车订单会出现在这里</text>
    </view>

    <view v-for="item in list" :key="item.orderNo" class="card">
      <view class="card-row">
        <text class="service">{{ item.serviceName }}</text>
        <text class="status">{{ item.statusLabel }}</text>
      </view>
      <view class="card-row sub">
        <text>{{ item.plateNo || '未填车牌' }}</text>
        <text>¥{{ formatAmount(item.payAmount) }}</text>
      </view>

      <!-- 按钮由后端 mainAction 返回，前端不自行判断该显示什么按钮 -->
      <view v-if="item.mainAction?.enabled" class="action" @click.stop="onAction(item)">
        {{ item.mainAction.label }}
      </view>
    </view>

    <view v-if="!loading && list.length > 0 && list.length >= total" class="tip">没有更多了</view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchOrderList, payOrder, type OrderListItemVO, type OrderTab } from '@/api/order'

const tabs: { key: OrderTab; label: string }[] = [
  { key: 'ONGOING', label: '进行中' },
  { key: 'WAIT_REVIEW', label: '待评价' },
  { key: 'ALL', label: '全部' },
]

const activeTab = ref<OrderTab>('ONGOING')
const list = ref<OrderListItemVO[]>([])
const total = ref(0)
const loading = ref(false)

/** 金额：后端存分，只在展示层转元，任何计算都不用元 */
function formatAmount(amount?: number | null): string {
  return ((amount ?? 0) / 100).toFixed(2)
}

async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    const page = await fetchOrderList({ tab: activeTab.value, pageNum: 1, pageSize: 10 })
    list.value = page.list ?? []
    total.value = page.total ?? 0
  } catch {
    // request 层已统一提示，这里只保证页面不白屏
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 订单按钮点击：目前只实现 PAY，其余 action 随功能开工在此扩展 */
async function onAction(item: OrderListItemVO): Promise<void> {
  if (item.mainAction?.action !== 'PAY') {
    return
  }
  try {
    await payOrder(item.orderNo ?? '')
    uni.showToast({ title: '支付成功，等待存钥匙', icon: 'none' })
    await loadOrders()
  } catch {
    // request 层已统一提示（含 21002 状态不允许）
  }
}

function switchTab(key: OrderTab): void {
  if (activeTab.value === key) {
    return
  }
  activeTab.value = key
  loadOrders()
}

onShow(() => {
  loadOrders()
})
</script>

<style>
.page {
  padding: 24rpx;
  background: #f6f7f9;
  min-height: 100vh;
  box-sizing: border-box;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 24rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  font-size: 28rpx;
  color: #666;
  border-radius: 12rpx;
}

.tab-active {
  background: #1a73e8;
  color: #fff;
}

.tip {
  text-align: center;
  color: #999;
  font-size: 26rpx;
  padding: 40rpx 0;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-img {
  width: 160rpx;
  height: 160rpx;
  opacity: 0.4;
  margin-bottom: 32rpx;
}

.empty-title {
  font-size: 32rpx;
  color: #333;
}

.empty-sub {
  font-size: 26rpx;
  color: #999;
  margin-top: 12rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-row.sub {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #888;
}

.service {
  font-size: 32rpx;
  color: #222;
  font-weight: 500;
}

.status {
  font-size: 26rpx;
  color: #1a73e8;
}

.action {
  margin-top: 24rpx;
  background: #1a73e8;
  color: #fff;
  text-align: center;
  padding: 18rpx 0;
  border-radius: 12rpx;
  font-size: 28rpx;
}
</style>

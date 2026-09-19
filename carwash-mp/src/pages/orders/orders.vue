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

    <view v-for="item in list" :key="item.orderNo" class="card" @click="goDetail(item)">
      <view class="card-row">
        <text class="service">{{ item.serviceName }}</text>
        <text class="status">{{ item.statusLabel }}</text>
      </view>
      <view class="card-row sub">
        <text>{{ item.plateNo || '未填车牌' }}</text>
        <text>¥{{ formatAmount(item.payAmount) }}</text>
      </view>

      <!-- 按钮由后端 mainAction / subActions 返回，前端不自行判断该显示什么按钮 -->
      <view class="actions">
        <view v-if="item.mainAction?.enabled" class="action" @click.stop="onAction(item, item.mainAction)">
          {{ item.mainAction.label }}
        </view>
        <view
          v-for="sub in item.subActions ?? []"
          :key="sub.action"
          class="action action-plain"
          @click.stop="onAction(item, sub)"
        >
          {{ sub.label }}
        </view>
      </view>
    </view>

    <view v-if="!loading && list.length > 0 && list.length >= total" class="tip">没有更多了</view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { cancelOrder, fetchOrderList, type OrderAction, type OrderListItemVO, type OrderTab } from '@/api/order'
import { fetchCustomerService } from '@/api/config'

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

function goDetail(item: OrderListItemVO): void {
  uni.navigateTo({ url: `/pages/order-detail/order-detail?orderNo=${item.orderNo}` })
}

/** 订单按钮点击：action 由后端返回，前端按 action 分发；新增按钮时改这里 */
async function onAction(item: OrderListItemVO, action?: OrderAction): Promise<void> {
  const orderNo = item.orderNo ?? ''
  if (action?.action === 'PAY') {
    // P8 支付页：倒计时 + 费用明细 + 微信支付
    uni.navigateTo({ url: `/pages/pay/pay?orderNo=${orderNo}` })
    return
  }
  // 存钥匙 / 取钥匙 → P10 开箱页（后端按订单状态判定存还是取）
  if (action?.action === 'DEPOSIT_KEY' || action?.action === 'TAKE_KEY') {
    const mode = action.action === 'TAKE_KEY' ? 'take' : 'deposit'
    uni.navigateTo({ url: `/pages/open-box/open-box?orderNo=${orderNo}&mode=${mode}` })
    return
  }
  // 看进度 = 跳详情页（时间轴由后端流转日志生成，前端不另算进度）
  if (action?.action === 'VIEW_PROGRESS') {
    goDetail(item)
    return
  }

  // 再来一单：回到下单页重新选服务/车辆/机柜
  if (action?.action === 'REORDER') {
    uni.navigateTo({ url: '/pages/index/index' })
    return
  }

  // 联系客服：电话与夜间提示由后端配置下发，前端不硬编码号码
  if (action?.action === 'CONTACT_SERVICE') {
    try {
      const cfg = await fetchCustomerService()
      const phones = [cfg.platformPhone, cfg.stationPhone].filter((p): p is string => !!p)
      if (phones.length === 0) {
        uni.showModal({
          title: '联系客服',
          content: cfg.nightTip || '客服联系方式暂未配置',
          showCancel: false
        })
        return
      }
      uni.showActionSheet({
        itemList: phones.map((p) => '拨打 ' + p),
        success: (res) => {
          uni.makePhoneCall({ phoneNumber: phones[res.tapIndex] })
        }
      })
    } catch {
      // request 层已统一提示
    }
    return
  }

  // 去评价：星级 + 标签 + 原因（≤4 星必填原因），提交后状态机推进到已完成
  if (action?.action === 'REVIEW') {
    uni.navigateTo({ url: `/pages/review/review?orderNo=${orderNo}` })
    return
  }

  if (action?.action === 'CANCEL') {
    uni.showModal({
      title: '取消订单',
      content: '取消后不可恢复，已支付将全额退款',
      placeholderText: '请填写取消原因',
      editable: true,
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        const reason = (res.content ?? '').trim()
        if (!reason) {
          uni.showToast({ title: '请填写取消原因', icon: 'none' })
          return
        }
        try {
          await cancelOrder(orderNo, reason)
          uni.showToast({ title: '已取消', icon: 'none' })
          await loadOrders()
        } catch {
          // request 层已统一提示（含 21002「已开洗不可取消」、21004「原因必填」）
        }
      },
    })
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

.actions {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.action {
  flex: 1;
  background: #1a73e8;
  color: #fff;
  text-align: center;
  padding: 18rpx 0;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.action-plain {
  background: #fff;
  color: #666;
  border: 2rpx solid #ddd;
}
</style>

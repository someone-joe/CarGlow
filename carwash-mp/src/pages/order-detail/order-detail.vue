<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>

    <block v-else-if="detail">
      <!-- 状态头 -->
      <view class="status-bar">
        <view class="status-main">
          <text class="status">{{ detail.statusLabel }}</text>
          <text v-if="detail.statusDesc" class="status-desc">{{ detail.statusDesc }}</text>
        </view>
        <text v-if="detail.overdue" class="overdue">已超时</text>
      </view>

      <!-- 服务卡 -->
      <view class="panel">
        <view class="svc-row">
          <view class="svc-main">
            <text class="svc-name">{{ detail.serviceName }}</text>
            <text class="svc-sub">{{ detail.plateNo || '未填车牌' }}</text>
          </view>
          <text class="svc-price">¥{{ formatAmount(detail.payAmount) }}</text>
        </view>
      </view>

      <!-- 费用明细 -->
      <view class="panel">
        <view class="panel-title">费用明细</view>
        <view class="row">
          <text class="label">订单金额</text>
          <text class="value">¥{{ formatAmount(detail.originAmount) }}</text>
        </view>
        <!-- 没用到优惠就不显示这一行，避免出现 -¥0.00（bug092404） -->
        <view class="row" v-if="detail.discountAmount">
          <text class="label">优惠</text>
          <text class="value discount">-¥{{ formatAmount(detail.discountAmount) }}</text>
        </view>
        <view class="row total">
          <text class="label">{{ detail.status === 'WAIT_PAY' ? '应付' : '实付' }}</text>
          <text class="value price">¥{{ formatAmount(detail.payAmount) }}</text>
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="panel">
        <view class="panel-title">订单信息</view>
        <view class="row"><text class="label">订单号</text><text class="value">{{ detail.orderNo }}</text></view>
        <view v-if="detail.siteName" class="row">
          <text class="label">洗车站点</text><text class="value">{{ detail.siteName }}</text>
        </view>
        <view v-if="detail.cabinetName" class="row">
          <text class="label">钥匙柜</text><text class="value">{{ detail.cabinetName }}</text>
        </view>
        <view v-if="detail.promiseReturnTime" class="row">
          <text class="label">承诺还车</text><text class="value">{{ formatTime(detail.promiseReturnTime) }}</text>
        </view>
        <view v-if="detail.remark" class="row"><text class="label">备注</text><text class="value">{{ detail.remark }}</text></view>
      </view>

      <!-- 服务进度 -->
      <view class="panel">
        <view class="panel-title">服务进度</view>
        <view v-for="(node, index) in timeline" :key="node.node" class="node">
          <view class="node-line">
            <view :class="['dot', node.reached && 'dot-reached', node.current && 'dot-current']" />
            <view v-if="index < timeline.length - 1" :class="['line', node.reached && 'line-reached']" />
          </view>
          <view class="node-body">
            <text :class="['node-title', !node.reached && 'node-title-todo']">{{ node.title }}</text>
            <text class="node-time">{{ node.time ? formatTime(node.time) : '待完成' }}</text>
          </view>
        </view>
      </view>

      <!-- 底部操作 -->
      <view class="footer">
        <view v-if="detail.mainAction?.enabled" class="foot-btn primary" @click="onMainAction">
          {{ detail.mainAction.label }}
        </view>
        <view
          v-for="sub in detail.subActions ?? []"
          :key="sub.action"
          class="foot-btn"
          @click="onSubAction(sub)"
        >
          {{ sub.label }}
        </view>
      </view>
    </block>

    <view v-else class="tip">订单不存在或无权查看</view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { fetchOrderDetail, type OrderAction, type OrderDetailVO, type TimelineNodeVO } from '@/api/order'

const detail = ref<OrderDetailVO | null>(null)
const timeline = ref<TimelineNodeVO[]>([])
const loading = ref(true)

/** 金额：后端存分，只在展示层转元 */
function formatAmount(amount?: number | null): string {
  return ((amount ?? 0) / 100).toFixed(2)
}

function formatTime(ms: number): string {
  const d = new Date(ms)
  const pad = (n: number) => `${n}`.padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 主按钮：支付跳支付页；其余按 action 分发（新按钮在此扩展） */
function onMainAction(): void {
  const action = detail.value?.mainAction?.action
  const orderNo = detail.value?.orderNo ?? ''
  if (!action || !orderNo) return
  if (action === 'PAY') {
    uni.navigateTo({ url: `/pages/pay/pay?orderNo=${orderNo}` })
    return
  }
  if (action === 'DEPOSIT_KEY' || action === 'TAKE_KEY') {
    const mode = action === 'TAKE_KEY' ? 'take' : 'deposit'
    uni.navigateTo({ url: `/pages/open-box/open-box?orderNo=${orderNo}&mode=${mode}` })
    return
  }
  if (action === 'REVIEW') {
    uni.navigateTo({ url: `/pages/review/review?orderNo=${orderNo}` })
    return
  }
  if (action === 'REORDER') {
    uni.navigateTo({ url: '/pages/index/index' })
  }
}

function onSubAction(sub: OrderAction): void {
  const orderNo = detail.value?.orderNo ?? ''
  if (!orderNo) return
  if (sub.action === 'CANCEL') {
    uni.showModal({
      title: '取消订单',
      content: '取消后不可恢复，已支付将全额退款',
      placeholderText: '请填写取消原因',
      editable: true,
      success: async (res) => {
        if (!res.confirm) return
        const reason = (res.content ?? '').trim()
        if (!reason) {
          uni.showToast({ title: '请填写取消原因', icon: 'none' })
          return
        }
        const { cancelOrder } = await import('@/api/order')
        try {
          await cancelOrder(orderNo, reason)
          uni.showToast({ title: '已取消', icon: 'none' })
          loadDetail(orderNo)
        } catch {
          // request 层已统一提示
        }
      },
    })
    return
  }
  if (sub.action === 'CONTACT_SERVICE') {
    uni.switchTab({ url: '/pages/customer-service/customer-service' })
  }
}

/** 拉详情与进度；进入页面和取消后刷新都走这里（不能直接调用 onLoad，那是注册回调） */
function loadDetail(orderNo: string): void {
  loading.value = true
  fetchOrderDetail(orderNo)
    .then((data) => {
      detail.value = data
      timeline.value = data.timeline ?? []
    })
    .catch(() => {
      detail.value = null
    })
    .finally(() => {
      loading.value = false
    })
}

onLoad((options) => {
  const orderNo = (options as { orderNo?: string }).orderNo ?? ''
  if (!orderNo) {
    loading.value = false
    return
  }
  loadDetail(orderNo)
})
</script>

<style>
.page {
  padding: 24rpx 24rpx 180rpx;
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

.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #00aeb5, #0e8f94);
  border-radius: 20rpx;
  padding: 34rpx 30rpx;
  margin-bottom: 20rpx;
}

.status {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #fff;
}

.status-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  color: rgba(255, 255, 255, 0.85);
}

.overdue {
  font-size: 24rpx;
  color: #fff;
  background: #ff5b4a;
  border-radius: 20rpx;
  padding: 6rpx 18rpx;
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.panel-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 18rpx;
}

.svc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.svc-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1b2b2a;
}

.svc-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #8a9a98;
}

.svc-price {
  font-size: 40rpx;
  font-weight: 700;
  color: #ff5b4a;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 0;
}

.label {
  font-size: 26rpx;
  color: #8a9a98;
}

.value {
  font-size: 26rpx;
  color: #1b2b2a;
}

.discount {
  color: #ff5b4a;
}

.total {
  border-top: 2rpx solid #eaf1f0;
  margin-top: 10rpx;
  padding-top: 20rpx;
}

.price {
  font-size: 34rpx;
  font-weight: 700;
  color: #ff5b4a;
}

/* ---- 时间轴 ---- */
.node {
  display: flex;
  min-height: 96rpx;
}

.node-line {
  width: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #d7e3e1;
  margin-top: 8rpx;
}

.dot-reached {
  background: #00aeb5;
}

.dot-current {
  background: #14342f;
  box-shadow: 0 0 0 8rpx rgba(0, 174, 181, 0.18);
}

.line {
  flex: 1;
  width: 2rpx;
  background: #e6eeec;
}

.line-reached {
  background: #00aeb5;
}

.node-body {
  flex: 1;
  padding-bottom: 28rpx;
}

.node-title {
  font-size: 28rpx;
  color: #1b2b2a;
}

.node-title-todo {
  color: #b6c2c0;
}

.node-time {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #8a9a98;
}

/* ---- 底部操作 ---- */
.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  background: #fff;
  padding: 20rpx 24rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.foot-btn {
  flex: 1;
  text-align: center;
  padding: 26rpx 0;
  border-radius: 44rpx;
  font-size: 30rpx;
  color: #6b7b79;
  border: 2rpx solid #cfe0de;
}

.foot-btn.primary {
  background: #14342f;
  border-color: #14342f;
  color: #fff;
  font-weight: 600;
}
</style>

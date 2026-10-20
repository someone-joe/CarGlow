<template>
  <view class="cg-page order-detail">
    <view v-if="loading" class="cg-empty">加载中…</view>

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
        <!-- 派单信息（钥匙入柜后由自动派单/抢单/后台派单写入，客户要知道是谁接的单） -->
        <view v-if="detail.assignedAt" class="row">
          <text class="label">接单师傅</text>
          <text class="value">{{ detail.workerName || '服务师傅' }} · {{ formatTime(detail.assignedAt) }}</text>
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

    <view v-else class="cg-empty">订单不存在或无权查看</view>
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
/* 底部操作条占位：页面底部留白避开固定操作条 */
.order-detail {
  padding-bottom: 220rpx;
}

/* ---- 状态头：顶部重点，醒目 ---- */
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.status-main {
  flex: 1;
}

.status {
  display: block;
  font-size: var(--cg-font-title);
  font-weight: 600;
  color: var(--cg-title);
}

.status-desc {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.overdue {
  flex: none;
  margin-left: 24rpx;
  padding: 8rpx 24rpx;
  background: rgba(255, 59, 48, 0.10);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-error);
}

/* ---- 模块卡片 ---- */
.panel {
  margin-top: 32rpx;
  padding: 40rpx;
  background: var(--cg-card);
  border-radius: var(--cg-radius-lg);
  box-shadow: var(--cg-shadow-card);
}

.panel-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

.svc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.svc-name {
  display: block;
  font-size: var(--cg-font-body);
  font-weight: 500;
  color: var(--cg-title);
}

.svc-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.svc-price {
  margin-left: 32rpx;
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

/* ---- 明细行：靠留白区分，不用分割线 ---- */
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72rpx;
}

.label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.value {
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

.discount {
  color: var(--cg-error);
}

.total .label {
  color: var(--cg-title);
}

.total .price {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

/* ---- 时间轴 ---- */
.node {
  display: flex;
  align-items: stretch;
}

.node-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 40rpx;
}

.dot {
  flex: none;
  box-sizing: border-box;
  width: 20rpx;
  height: 20rpx;
  margin-top: 12rpx;
  border-radius: 50%;
  background: var(--cg-line);
}

.dot-reached {
  background: var(--cg-primary);
}

.dot-current {
  box-shadow: 0 0 0 6rpx rgba(0, 122, 255, 0.15);
}

.line {
  flex: 1;
  width: 2rpx;
  margin: 12rpx 0;
  background: var(--cg-line);
}

.line-reached {
  background: var(--cg-primary);
}

.node-body {
  flex: 1;
  padding: 0 0 32rpx 24rpx;
}

.node:last-child .node-body {
  padding-bottom: 0;
}

.node-title {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-title);
}

.node-title-todo {
  color: var(--cg-text-aux);
}

.node-time {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

/* ---- 底部操作 ---- */
.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  box-sizing: border-box;
  padding: 16rpx 40rpx calc(16rpx + env(safe-area-inset-bottom));
  background: var(--cg-glass-bg);
  -webkit-backdrop-filter: blur(var(--cg-glass-blur));
  backdrop-filter: blur(var(--cg-glass-blur));
  border-top: var(--cg-glass-border);
  box-shadow: 0 -4rpx 40rpx rgba(0, 0, 0, 0.06);
}

.foot-btn {
  min-height: 104rpx;
  margin-left: 24rpx;
  padding: 0 40rpx;
  background: var(--cg-card);
  border: 1rpx solid var(--cg-border);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: var(--cg-title);
  line-height: 104rpx;
  text-align: center;
}

.foot-btn.primary {
  background: var(--cg-primary);
  border-color: var(--cg-primary);
  color: #ffffff;
}
</style>

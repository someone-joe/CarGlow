<template>
  <view class="cg-page orders">
    <view class="cg-tabs">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="cg-tab"
        :class="{ 'is-active': activeTab === t.key }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </view>
    </view>

    <view v-if="loading" class="cg-empty">加载中…</view>

    <view v-else-if="list.length === 0" class="cg-empty">
      <text class="empty-title">暂无订单</text>
      <text class="empty-sub">洗车订单会出现在这里</text>
    </view>

    <view v-for="item in list" :key="item.orderNo" class="cg-card card" @click="goDetail(item)">
      <view class="card-top">
        <text class="svc">{{ item.serviceName }}</text>
        <text class="badge">{{ item.statusLabel }}</text>
      </view>

      <!-- 无感洗车订单 -->
      <template v-if="item.bizType === 'UNSENSITIVE'">
        <view class="meta">
          <text class="meta-line">订单号 {{ item.orderNo }}</text>
          <text class="meta-line">
            {{ item.serviceName }} · 约 {{ item.workMinutes || 40 }} 分钟
          </text>
          <text v-if="item.appointTime" class="meta-line">预计服务开始 {{ formatTime(item.appointTime) }}</text>
          <text v-if="item.communityName" class="meta-line">服务小区 {{ item.communityName }}</text>
          <text v-if="item.plateNo" class="meta-line">车牌 {{ item.plateNo }}</text>
          <!-- 派单信息：钥匙入柜后客户最关心「有没有人接单、谁接的」 -->
          <text v-if="item.assignedAt" class="meta-line meta-dispatch">
            已派单 · {{ item.workerName || '服务师傅' }} · {{ formatTime(item.assignedAt) }}
          </text>
        </view>

        <view class="card-bottom">
          <text class="amount">¥{{ formatAmount(item.payAmount) }}</text>
          <view class="ops">
            <view v-if="item.mainAction?.enabled" class="op op-primary" @click.stop="onAction(item, item.mainAction)">
              {{ item.mainAction.label }}
            </view>
            <view
              v-for="sub in item.subActions ?? []"
              :key="sub.action"
              class="op"
              @click.stop="onAction(item, sub)"
            >
              {{ sub.label }}
            </view>
          </view>
        </view>
      </template>

      <!-- 自助洗车订单：与无感融合同一张卡片，用户不需要感知两类区别 -->
      <template v-else>
        <view class="meta">
          <text class="meta-line">订单号 {{ item.orderNo }}</text>
          <text class="meta-line">
            {{ item.serviceMinutes || 30 }} 分钟 · {{ item.plateNo || '未填车牌' }}
          </text>
          <text v-if="item.status === 'SERVING'" class="meta-line meta-dispatch">
            剩余 {{ formatRemain(item.remainingSeconds) }}
          </text>
          <text v-else-if="item.status === 'QUEUING'" class="meta-line meta-dispatch">
            排队第 {{ item.queueNo }} 位
          </text>
        </view>

        <view class="card-bottom">
          <text class="amount">¥{{ formatAmount(item.payAmount) }}</text>
          <view class="ops">
            <view class="op" @click.stop="goDetail(item)">查看详情</view>
          </view>
        </view>
      </template>
    </view>

    <view v-if="loadingMore" class="cg-empty">加载中…</view>
    <view v-else-if="!loading && list.length > 0 && !hasMore" class="cg-empty">没有更多了</view>
  </view>
</template>

<script setup lang="ts">
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { cancelOrder, fetchOrderList, type OrderAction, type OrderListItemVO, type OrderTab } from '@/api/order'
import { fetchCustomerService } from '@/api/config'
import { fetchSelfwashOrders, type SelfwashOrderVO } from '@/api/selfwash'
import { requirePhone } from '@/store/auth'

/**
 * 融合后的订单列表项。
 *
 * <p>无感洗车与自助洗车在数据、状态机、模块上完全隔离，但用户只关心「我的订单」这一件事，
 * 不该被迫分成两个页面、两套心智。故在前端合并成一个列表，按 bizType 分派渲染与跳转。
 *
 * <p>为什么在后端不合并：wash-order 不能反向依赖 wash-selfwash（会成环），
 * 融合是纯展示层的事，放前端即零耦合。
 */
type MergedOrder =
  | (OrderListItemVO & { bizType: 'UNSENSITIVE' })
  | (SelfwashOrderVO & { bizType: 'SELFWASH' })

// 分组由后端唯一定义（契约 tab 枚举），前端只负责展示文案
const tabs: { key: OrderTab; label: string }[] = [
  { key: 'PENDING', label: '待处理' },
  { key: 'SERVING', label: '服务中' },
  { key: 'DONE', label: '已完成' },
]

/**
 * 融合 tab → 两边自己的 tab 取值。
 * 无感：契约 OrderTab（PENDING/SERVING/DONE，后端已做状态映射）
 * 自助：selfwash 的 tab（WAIT_PAY/WAIT_USE/ONGOING/DONE/CANCELED，单值，故用数组表达多查几次）
 */
const TAB_MAP: Record<OrderTab, { unsensitive: OrderTab; selfwash: string[] }> = {
  PENDING: { unsensitive: 'PENDING', selfwash: ['WAIT_PAY', 'WAIT_USE'] },
  SERVING: { unsensitive: 'SERVING', selfwash: ['ONGOING'] },
  DONE: { unsensitive: 'DONE', selfwash: ['DONE', 'CANCELED'] },
}

const activeTab = ref<OrderTab>('PENDING')
const list = ref<MergedOrder[]>([])
const loading = ref(false)
const pageNum = ref(1)
const loadingMore = ref(false)
/** 无感侧是否还有下一页（自助侧一次性全量返回，没有分页） */
const unsensitiveTotal = ref(0)
const unsensitiveLoaded = ref(0)

const hasMore = computed(() => unsensitiveLoaded.value < unsensitiveTotal.value)

/** 金额：后端存分，只在展示层转元，任何计算都不用元 */
function formatAmount(amount?: number | null): string {
  return ((amount ?? 0) / 100).toFixed(2)
}

/** 预约时间（毫秒）转「MM-DD HH:mm」 */
function formatTime(millis?: number | null): string {
  if (!millis) return ''
  const d = new Date(millis)
  const p = (n: number) => `${n}`.padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 自助剩余秒数 → mm:ss */
function formatRemain(sec?: number): string {
  const s = sec ?? 0
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

/**
 * 排序键：两类都统一用「下单时间（毫秒）」。
 * 自助的 createTime 是若依 Date 序列化出的字符串（yyyy-MM-dd HH:mm:ss），转成毫秒再比。
 */
function sortKey(item: MergedOrder): number {
  const t = item.createTime
  if (t == null) return 0
  if (typeof t === 'number') return t
  // 字符串形态：把 'yyyy-MM-dd HH:mm:ss' 当本地时间解析（后端时区 Asia/Shanghai）
  return new Date(String(t).replace(/-/g, '/')).getTime() || 0
}

/** 无感 → 融合项 */
function toMerged(o: OrderListItemVO): MergedOrder {
  return { ...o, bizType: 'UNSENSITIVE' }
}

/** 自助 → 融合项 */
function selfwashToMerged(o: SelfwashOrderVO): MergedOrder {
  return { ...o, bizType: 'SELFWASH' }
}

async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    const m = TAB_MAP[activeTab.value]
    const [page, ...selfPages] = await Promise.all([
      fetchOrderList({ tab: m.unsensitive, pageNum: 1, pageSize: 10 }),
      ...m.selfwash.map((t) => fetchSelfwashOrders(t)),
    ])
    const merged: MergedOrder[] = [
      ...(page.list ?? []).map(toMerged),
      ...selfPages.flat().map(selfwashToMerged),
    ]
    merged.sort((a, b) => sortKey(b) - sortKey(a))
    list.value = merged
    unsensitiveTotal.value = page.total ?? 0
    unsensitiveLoaded.value = (page.list ?? []).length
    pageNum.value = 1
  } catch {
    // request 层已统一提示，这里只保证页面不白屏
    list.value = []
    unsensitiveTotal.value = 0
    unsensitiveLoaded.value = 0
  } finally {
    loading.value = false
  }
}

/**
 * 触底加载下一页：只翻无感侧（自助一次性全量返回）。
 * 追加后重新排序，否则新一页的无感单会插在自助单后面，看起来像"加载失败了"。
 */
async function loadMore(): Promise<void> {
  if (loading.value || loadingMore.value || !hasMore.value) {
    return
  }
  loadingMore.value = true
  try {
    const next = pageNum.value + 1
    const m = TAB_MAP[activeTab.value]
    const page = await fetchOrderList({ tab: m.unsensitive, pageNum: next, pageSize: 10 })
    const fresh = (page.list ?? []).map(toMerged)
    const merged = [...list.value, ...fresh]
    merged.sort((a, b) => sortKey(b) - sortKey(a))
    list.value = merged
    unsensitiveTotal.value = page.total ?? 0
    unsensitiveLoaded.value += fresh.length
    pageNum.value = next
  } catch {
    // request 层已统一提示
  } finally {
    loadingMore.value = false
  }
}

function goDetail(item: MergedOrder): void {
  if (item.bizType === 'SELFWASH') {
    uni.navigateTo({ url: `/pages/selfwash-detail/selfwash-detail?orderNo=${item.orderNo}` })
    return
  }
  uni.navigateTo({ url: `/pages/order-detail/order-detail?orderNo=${item.orderNo}` })
}

/** 订单按钮点击：action 由后端返回，前端按 action 分发；新增按钮时改这里 */
async function onAction(item: MergedOrder, action?: OrderAction): Promise<void> {
  const orderNo = item.orderNo ?? ''
  if (action?.action === 'PAY') {
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
  // 订单属强用户相关信息：进入即过手机号授权门控——已绑直接加载，未绑弹授权、登后自动继续
  requirePhone(() => loadOrders())
})

// 触底加载下一页（bug 修复：之前固定只取第 1 页，第 11 条起的订单永远看不到）
onReachBottom(() => {
  loadMore()
})
</script>

<style>
/* tabBar 页：底部留白需避开原生 tabBar */
.orders {
  padding-bottom: calc(200rpx + env(safe-area-inset-bottom));
}

.empty-title {
  display: block;
  font-size: var(--cg-font-body);
  color: var(--cg-text-sub);
}

.empty-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-aux);
}

/* ---- 订单卡 ---- */
.card {
  margin-bottom: 32rpx;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.svc {
  flex: 1;
  font-size: var(--cg-font-subtitle);
  font-weight: 500;
  color: var(--cg-title);
}

/* 状态标签：待处理=橙、服务中=蓝、已完成=绿由后端 statusLabel 仅作文案，颜色按分组常量映射 */
.badge {
  flex: none;
  margin-left: 24rpx;
  padding: 8rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.meta {
  margin-top: 24rpx;
}

.meta-line {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.meta-dispatch {
  color: var(--cg-primary);
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 32rpx;
}

.amount {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.ops {
  display: flex;
  align-items: center;
}

.op {
  min-height: 72rpx;
  margin-left: 24rpx;
  padding: 0 32rpx;
  background: var(--cg-card);
  border: 1rpx solid var(--cg-border);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-title);
  line-height: 72rpx;
  text-align: center;
}

.op-primary {
  background: var(--cg-primary);
  border-color: var(--cg-primary);
  color: #ffffff;
}
</style>

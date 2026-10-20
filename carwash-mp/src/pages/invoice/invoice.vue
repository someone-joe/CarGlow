<template>
  <view class="cg-page invoice">
    <!-- 申请表单 -->
    <view class="panel">
      <text class="panel-title">申请开票</text>

      <view class="picker-row" @click="pickOrder">
        <text class="picker-label">选择订单</text>
        <text class="picker-value" :class="{ placeholder: !form.orderNo }">
          {{ orderText || '请选择已完成的订单' }}
        </text>
        <text class="arrow">›</text>
      </view>

      <view class="type-row">
        <text class="type-label">抬头类型</text>
        <view class="type-opts">
          <text class="type-opt" :class="{ on: form.titleType === 'PERSONAL' }" @click="form.titleType = 'PERSONAL'">个人</text>
          <text class="type-opt" :class="{ on: form.titleType === 'COMPANY' }" @click="form.titleType = 'COMPANY'">企业</text>
        </view>
      </view>

      <input v-model="form.title" class="input" placeholder="发票抬头（必填）" maxlength="64" />
      <input v-if="form.titleType === 'COMPANY'" v-model="form.taxNo" class="input" placeholder="纳税人识别号（企业必填）" maxlength="64" />
      <input v-model="form.email" class="input" placeholder="接收邮箱（必填）" maxlength="64" />

      <view class="submit" :class="{ disabled: !canSubmit }" @click="submit">
        {{ submitting ? '提交中…' : '提交申请' }}
      </view>
    </view>

    <!-- 我的申请 -->
    <view class="panel">
      <text class="panel-title">我的申请</text>
      <view v-if="loading" class="cg-empty">加载中…</view>
      <view v-else-if="!invoices.length" class="cg-empty">暂无开票申请</view>
      <view v-for="i in invoices" :key="i.invoiceId" class="inv">
        <view class="inv-top">
          <text class="inv-amount">¥{{ fen2yuan(i.amount) }}</text>
          <text class="inv-status" :class="statusClass(i.status)">{{ i.statusLabel }}</text>
        </view>
        <text class="inv-sub">{{ i.title }}<text v-if="i.titleType === 'COMPANY'">（企业）</text></text>
        <text class="inv-sub muted">单号 {{ i.orderNo }}<text v-if="i.invoiceNo"> · 票号 {{ i.invoiceNo }}</text></text>
        <text v-if="i.rejectReason" class="inv-sub danger">驳回原因：{{ i.rejectReason }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { applyInvoice, fetchInvoices, type InvoiceVO } from '@/api/invoice'
import { fetchOrderList } from '@/api/order'
import { fetchSelfwashOrders } from '@/api/selfwash'

const loading = ref(true)
const submitting = ref(false)
const invoices = ref<InvoiceVO[]>([])

/** 可开票订单：无感已完成单 + 自助已完成单 */
interface OrderOption {
  orderNo: string
  label: string
  amount: number
}
const orderOptions = ref<OrderOption[]>([])

const form = ref({
  orderNo: '',
  titleType: 'PERSONAL',
  title: '',
  taxNo: '',
  email: '',
})
const orderText = ref('')

const canSubmit = computed(
  () => !!form.value.orderNo && !!form.value.title.trim() && form.value.email.includes('@') && !submitting.value
)

function fen2yuan(fen?: number): string {
  return ((fen ?? 0) / 100).toFixed(2)
}

function statusClass(s: string): string {
  return s === 'ISSUED' ? 'done' : s === 'REJECTED' ? 'reject' : 'applying'
}

function pickOrder(): void {
  if (!orderOptions.value.length) {
    uni.showToast({ title: '暂无已完成订单', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: orderOptions.value.map((o) => `${o.label} · ¥${fen2yuan(o.amount)}`),
    success: (res) => {
      const o = orderOptions.value[res.tapIndex]
      form.value.orderNo = o.orderNo
      orderText.value = o.label
    },
  })
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [page, selfDone, mine] = await Promise.all([
      fetchOrderList({ tab: 'DONE' }),
      fetchSelfwashOrders('DONE'),
      fetchInvoices(),
    ])
    const opts: OrderOption[] = []
    for (const o of page.list || []) {
      if (o.orderNo) opts.push({ orderNo: o.orderNo, label: String(o.orderNo), amount: o.payAmount ?? 0 })
    }
    for (const s of selfDone || []) {
      opts.push({ orderNo: s.orderNo, label: String(s.orderNo), amount: s.payAmount ?? 0 })
    }
    orderOptions.value = opts
    invoices.value = mine || []
  } catch {
    orderOptions.value = []
    invoices.value = []
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  if (!canSubmit.value) return
  if (form.value.titleType === 'COMPANY' && !form.value.taxNo.trim()) {
    uni.showToast({ title: '企业开票请填写税号', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await applyInvoice({
      orderNo: form.value.orderNo,
      titleType: form.value.titleType,
      title: form.value.title.trim(),
      taxNo: form.value.titleType === 'COMPANY' ? form.value.taxNo.trim() : undefined,
      email: form.value.email.trim(),
    })
    uni.showToast({ title: '申请已提交', icon: 'none' })
    form.value = { orderNo: '', titleType: 'PERSONAL', title: '', taxNo: '', email: '' }
    orderText.value = ''
    load()
  } catch {
    // request 层已提示
  } finally {
    submitting.value = false
  }
}

onShow(load)
</script>

<style>
.invoice {
  padding-bottom: 96rpx;
}

/* ---- 面板 ---- */
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

/* ---- 选择行 ---- */
.picker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 96rpx;
}

.picker-label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.picker-value {
  flex: 1;
  margin-left: 24rpx;
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
  text-align: right;
}

.picker-value.placeholder {
  color: var(--cg-text-aux);
}

.arrow {
  color: var(--cg-text-aux);
  font-size: var(--cg-font-caption);
}

/* ---- 抬头类型 ---- */
.type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 96rpx;
  margin-top: 16rpx;
}

.type-label {
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.type-opts {
  display: flex;
}

.type-opt {
  padding: 12rpx 32rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-caption);
  color: var(--cg-text-sub);
}

.type-opt + .type-opt {
  margin-left: 16rpx;
}

.type-opt.on {
  background: var(--cg-primary);
  color: #ffffff;
}

/* ---- 输入 ---- */
.input {
  box-sizing: border-box;
  width: 100%;
  margin-top: 24rpx;
  padding: 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-caption);
  color: var(--cg-text);
}

/* ---- 提交 ---- */
.submit {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 104rpx;
  margin-top: 40rpx;
  background: var(--cg-primary);
  border-radius: var(--cg-radius-md);
  font-size: var(--cg-font-body);
  color: #ffffff;
}

.submit.disabled {
  opacity: 0.4;
}

/* ---- 申请列表 ---- */
.inv {
  padding: 32rpx 0;
  border-top: 1rpx solid var(--cg-line);
}

.inv:last-child {
  border-bottom: none;
}

.inv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.inv-amount {
  font-size: var(--cg-font-subtitle);
  font-weight: 600;
  color: var(--cg-title);
}

.inv-status {
  padding: 8rpx 24rpx;
  background: var(--cg-fill);
  border-radius: var(--cg-radius-pill);
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.inv-status.applying {
  background: var(--cg-tint);
  color: var(--cg-primary);
}

.inv-status.done {
  background: rgba(52, 199, 89, 0.12);
  color: var(--cg-success);
}

.inv-status.reject {
  background: rgba(255, 59, 48, 0.10);
  color: var(--cg-error);
}

.inv-sub {
  display: block;
  margin-top: 16rpx;
  font-size: var(--cg-font-note);
  color: var(--cg-text-sub);
}

.inv-sub.muted {
  color: var(--cg-text-aux);
}

.inv-sub.danger {
  color: var(--cg-error);
}
</style>

<template>
  <view class="page">
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
      <view v-if="loading" class="tip">加载中…</view>
      <view v-else-if="!invoices.length" class="tip">暂无开票申请</view>
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
.page {
  padding: 24rpx;
  background: #f2f7f7;
  min-height: 100vh;
  box-sizing: border-box;
}

.panel {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}

.panel-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1b2b2a;
  padding-bottom: 18rpx;
  border-bottom: 2rpx solid #eaf1f0;
  margin-bottom: 12rpx;
}

.picker-row {
  display: flex;
  align-items: center;
  padding: 22rpx 0;
}

.picker-label {
  width: 150rpx;
  font-size: 27rpx;
  color: #1b2b2a;
}

.picker-value {
  flex: 1;
  font-size: 26rpx;
  color: #1b2b2a;
  text-align: right;
}

.picker-value.placeholder {
  color: #b6c2c0;
}

.type-row {
  display: flex;
  align-items: center;
  padding: 12rpx 0 20rpx;
}

.type-label {
  width: 150rpx;
  font-size: 27rpx;
  color: #1b2b2a;
}

.type-opts {
  display: flex;
  gap: 16rpx;
}

.type-opt {
  padding: 10rpx 28rpx;
  border-radius: 30rpx;
  background: #f2f7f7;
  font-size: 26rpx;
  color: #6b7b79;
}

.type-opt.on {
  background: #00aeb5;
  color: #fff;
}

.input {
  background: #f2f7f7;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 27rpx;
  margin-bottom: 16rpx;
}

.submit {
  background: #14342f;
  color: #fff;
  text-align: center;
  padding: 24rpx 0;
  border-radius: 40rpx;
  font-size: 29rpx;
  font-weight: 600;
  margin-top: 8rpx;
}

.submit.disabled {
  background: #cfe0de;
}

.tip {
  text-align: center;
  color: #8a9a98;
  font-size: 26rpx;
  padding: 40rpx 0;
}

.inv {
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f2f7f7;
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
  font-size: 32rpx;
  font-weight: 700;
  color: #1b2b2a;
}

.inv-status {
  font-size: 24rpx;
  font-weight: 600;
}

.inv-status.applying {
  color: #ff8a3d;
}

.inv-status.done {
  color: #00aeb5;
}

.inv-status.reject {
  color: #ff5b4a;
}

.inv-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6b7b79;
}

.inv-sub.muted {
  color: #b6c2c0;
  font-size: 22rpx;
}

.inv-sub.danger {
  color: #ff5b4a;
}

.arrow {
  color: #b6c2c0;
  font-size: 32rpx;
}
</style>

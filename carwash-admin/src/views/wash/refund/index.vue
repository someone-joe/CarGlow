<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="78px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="SE 开头订单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />

    <el-alert type="info" :closable="false" show-icon class="mb10">
      <template #title>
        这里是判断<b>「钱到底退没退」</b>的地方：以本页 status 为准，订单状态 REFUNDING 只表示处理中。
        系统每 5 分钟自动重试一次（上限 5 次），FAILED 且超限的单需要在这里人工重试。
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="refundList">
      <el-table-column label="退款单号" prop="refundNo" width="180" fixed="left" show-overflow-tooltip />
      <el-table-column label="订单号" prop="orderNo" width="170" show-overflow-tooltip />
      <el-table-column label="退款金额" width="110">
        <template #default="{ row }">
          <span class="amount">{{ toYuan(row.amount) }} 元</span>
        </template>
      </el-table-column>
      <el-table-column label="原因" prop="reason" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reason || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.provider === 'wechat' ? 'success' : 'info'" effect="plain">
            {{ row.provider === 'wechat' ? '微信' : row.provider }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="渠道流水号" prop="providerTradeNo" width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.providerTradeNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="失败原因" prop="failReason" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.failReason || '-' }}</template>
      </el-table-column>
      <el-table-column label="重试次数" prop="retryCount" width="90" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Refresh" @click="handleRetry(row)" v-hasPermi="['wash:refund:edit']">
            重试
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="WashRefund">
import { listRefund, retryRefund } from '@/api/wash/refund'

const { proxy } = getCurrentInstance()

const refundList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const statusOptions = [
  { value: 'INIT', label: '待退款' },
  { value: 'SUCCESS', label: '已退款' },
  { value: 'FAILED', label: '失败待重试' }
]

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, orderNo: undefined, status: undefined }
})
const { queryParams } = toRefs(data)

function toYuan(fen) {
  if (fen === null || fen === undefined) return '-'
  return (fen / 100).toFixed(2)
}

function statusLabel(s) {
  const hit = statusOptions.find((x) => x.value === s)
  return hit ? hit.label : s || '-'
}

function statusType(s) {
  return { INIT: 'warning', SUCCESS: 'success', FAILED: 'danger' }[s] || 'info'
}

function formatTime(ms) {
  if (!ms) return '-'
  return proxy.parseTime(ms, '{y}-{m}-{d} {h}:{i}')
}

function getList() {
  loading.value = true
  listRefund(queryParams.value).then((res) => {
    refundList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

/** 人工重试：已成功的单后端会拒绝（重退等于重复打款），这里先挡一道给明确提示 */
function handleRetry(row) {
  if (row.status === 'SUCCESS') {
    proxy.$modal.msgError('该退款单已成功，不能重复退款')
    return
  }
  const tip = row.status === 'FAILED'
    ? `确认重试退款单「${row.refundNo}」？将再次调用渠道（当前渠道：${row.provider}），失败会累加重试次数。`
    : `确认立即退款「${row.refundNo}」？`
  proxy.$modal.confirm(tip).then(() => {
    return retryRefund(row.refundNo)
  }).then(() => {
    proxy.$modal.msgSuccess('已发起，结果以本页状态为准')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.mb10 {
  margin-bottom: 10px;
}
.amount {
  color: #f56c6c;
  font-weight: 600;
}
</style>

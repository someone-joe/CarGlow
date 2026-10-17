<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="78px">
      <el-form-item label="工单号" prop="afterSaleNo">
        <el-input v-model="queryParams.afterSaleNo" placeholder="AF 开头工单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />

    <el-table v-loading="loading" :data="saleList">
      <el-table-column label="工单号" prop="afterSaleNo" width="170" fixed="left" show-overflow-tooltip />
      <el-table-column label="订单号" prop="orderNo" width="170" show-overflow-tooltip />
      <el-table-column label="会员ID" prop="memberId" width="80" />
      <el-table-column label="服务项" prop="serviceName" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.serviceName || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'REFUND' ? 'danger' : row.type === 'REWASH' ? 'warning' : 'info'">
            {{ typeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请原因" prop="reason" min-width="180" show-overflow-tooltip />
      <el-table-column label="涉及金额" width="100">
        <template #default="{ row }">{{ toYuan(row.amount) }} 元</template>
      </el-table-column>
      <el-table-column label="订单状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.orderStatus || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="工单状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处置人" prop="handlerBy" width="90">
        <template #default="{ row }">{{ row.handlerBy || '-' }}</template>
      </el-table-column>
      <el-table-column label="处置说明" prop="handleRemark" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.handleRemark || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Finished" @click="handleHandle(row)" v-hasPermi="['wash:aftersale:handle']">
            {{ row.status === 'INIT' ? '处置' : '查看' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 处置 -->
    <el-dialog :title="dialogTitle" v-model="open" width="620px" append-to-body>
      <el-descriptions :column="2" border class="mb10">
        <el-descriptions-item label="工单号">{{ current.afterSaleNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeLabel(current.type) }}</el-descriptions-item>
        <el-descriptions-item label="涉及金额">{{ toYuan(current.amount) }} 元</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag size="small" type="info">{{ current.orderStatus || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工单状态">
          <el-tag size="small" :type="statusType(current.status)">{{ statusLabel(current.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请原因" :span="2">{{ current.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="current.handlerBy" label="处置人">{{ current.handlerBy }}</el-descriptions-item>
        <el-descriptions-item v-if="current.handleRemark" label="处置说明">{{ current.handleRemark }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="current.status === 'INIT'">
        <el-form ref="handleRef" :model="handleForm" :rules="handleRules" label-width="90px">
          <el-form-item label="处理结果" prop="accept">
            <el-switch v-model="handleForm.accept" active-text="同意" inactive-text="驳回" />
          </el-form-item>
          <el-form-item label="处置说明" prop="remark">
            <el-input v-model="handleForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="同意可写处理说明；驳回必须写明原因，客户要看到为什么没通过" />
          </el-form-item>
        </el-form>
        <el-alert v-if="handleForm.accept && current.type === 'REFUND'" type="warning" :closable="false" show-icon>
          <template #title>
            同意后将由支付域自动发起退款，本接口返回成功只表示「已受理」；
            <b>钱是否到账以「退款单」页的 status 为准</b>（SUCCESS 才是真退了）。
          </template>
        </el-alert>
        <el-alert v-else-if="handleForm.accept" type="info" :closable="false" show-icon>
          <template #title>该类售后（{{ typeLabel(current.type) }}）不自动退款，同意后转人工处理。</template>
        </el-alert>
      </template>
      <el-alert v-else type="info" :closable="false" show-icon>
        <template #title>该工单已处置，不能重复操作（避免两个运营同时点产生两种结果）。</template>
      </el-alert>

      <template #footer>
        <el-button v-if="current.status === 'INIT'" type="primary" @click="submitHandle">确 定</el-button>
        <el-button @click="open = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WashAfterSale">
import { listAfterSale, handleAfterSale } from '@/api/wash/aftersale'

const { proxy } = getCurrentInstance()

const saleList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const dialogTitle = ref('')

const typeOptions = [
  { value: 'REWASH', label: '重洗' },
  { value: 'REFUND', label: '退款' },
  { value: 'CLAIM', label: '理赔' }
]

const statusOptions = [
  { value: 'INIT', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'DONE', label: '已完成' },
  { value: 'REJECTED', label: '已驳回' }
]

const data = reactive({
  current: {},
  queryParams: { pageNum: 1, pageSize: 10, afterSaleNo: undefined, type: undefined, status: undefined },
  handleForm: { accept: true, remark: undefined },
  handleRules: {
    remark: [{
      validator: (rule, value, callback) => {
        if (!handleForm.value.accept && (!value || !value.trim())) {
          callback(new Error('驳回必须填写原因，客户要看到为什么没通过'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }]
  }
})
const { current, queryParams, handleForm, handleRules } = toRefs(data)

function toYuan(fen) {
  if (fen === null || fen === undefined) return '-'
  return (fen / 100).toFixed(2)
}

function typeLabel(t) {
  const hit = typeOptions.find((x) => x.value === t)
  return hit ? hit.label : t || '-'
}

function statusLabel(s) {
  const hit = statusOptions.find((x) => x.value === s)
  return hit ? hit.label : s || '-'
}

function statusType(s) {
  return { INIT: 'warning', PROCESSING: 'primary', DONE: 'success', REJECTED: 'info' }[s] || 'info'
}

function formatTime(ms) {
  if (!ms) return '-'
  return proxy.parseTime(ms, '{y}-{m}-{d} {h}:{i}')
}

function getList() {
  loading.value = true
  listAfterSale(queryParams.value).then((res) => {
    saleList.value = res.rows
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

function handleHandle(row) {
  current.value = row
  handleForm.value = { accept: true, remark: undefined }
  dialogTitle.value = row.status === 'INIT' ? '处置售后工单' : '售后工单详情'
  open.value = true
}

function submitHandle() {
  proxy.$refs.handleRef.validate((valid) => {
    if (!valid) return
    handleAfterSale(current.value.afterSaleNo, handleForm.value.accept, handleForm.value.remark || undefined).then(() => {
      proxy.$modal.msgSuccess('处置成功')
      open.value = false
      getList()
    })
  })
}

getList()
</script>

<style scoped>
.mb10 {
  margin-bottom: 10px;
}
</style>

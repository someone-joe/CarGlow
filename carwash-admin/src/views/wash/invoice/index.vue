<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in statusOpts" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单号">
        <el-input v-model="query.orderNo" placeholder="选填" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会员ID">
        <el-input v-model="query.memberId" placeholder="选填" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" border size="small">
      <el-table-column label="ID" prop="invoiceId" width="60" align="center" />
      <el-table-column label="会员ID" prop="memberId" width="90" />
      <el-table-column label="订单号" prop="orderNo" min-width="180" show-overflow-tooltip />
      <el-table-column label="金额(元)" width="100" align="right">
        <template #default="{ row }">{{ yuan(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="抬头" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.title }}<span v-if="row.titleType === 'COMPANY'" class="tag">企业</span>
        </template>
      </el-table-column>
      <el-table-column label="税号" prop="taxNo" min-width="130" show-overflow-tooltip />
      <el-table-column label="邮箱" prop="email" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发票号" prop="invoiceNo" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'APPLYING'">
            <el-button link type="primary" @click="doIssue(row)" v-hasPermi="['wash:invoice:audit']">开票</el-button>
            <el-button link type="danger" @click="doReject(row)" v-hasPermi="['wash:invoice:audit']">驳回</el-button>
          </template>
          <span v-else class="done">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="query.pageNum"
      v-model:limit="query.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="Invoice">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listInvoices, issueInvoice, rejectInvoice, invoiceStatusOptions } from '@/api/wash/invoice'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const statusOpts = ref([])

const query = reactive({ status: undefined, orderNo: undefined, memberId: undefined, pageNum: 1, pageSize: 10 })

function yuan(fen) {
  return ((fen || 0) / 100).toFixed(2)
}

function statusType(s) {
  const map = { APPLYING: 'warning', ISSUED: 'success', REJECTED: 'danger' }
  return map[s] ?? 'info'
}

function getList() {
  loading.value = true
  listInvoices({
    status: query.status, orderNo: query.orderNo,
    memberId: query.memberId ? Number(query.memberId) : undefined,
    pageNum: query.pageNum, pageSize: query.pageSize
  })
    .then((res) => {
      rows.value = res.data.rows || []
      total.value = res.data.total || 0
    })
    .finally(() => { loading.value = false })
}

function handleQuery() {
  query.pageNum = 1
  getList()
}

function resetQuery() {
  query.status = undefined
  query.orderNo = undefined
  query.memberId = undefined
  query.pageNum = 1
  getList()
}

function doIssue(row) {
  ElMessageBox.prompt('请输入发票号（选填）', '开票', { inputPlaceholder: '如 04412345' })
    .then(({ value }) => issueInvoice(row.invoiceId, value))
    .then(() => { ElMessage.success('已开票'); getList() })
    .catch(() => {})
}

function doReject(row) {
  ElMessageBox.prompt('请输入驳回原因', '驳回', { inputPlaceholder: '如 抬头信息有误' })
    .then(({ value }) => {
      if (!value) {
        ElMessage.warning('驳回原因必填')
        return Promise.reject(new Error('empty'))
      }
      return rejectInvoice(row.invoiceId, value)
    })
    .then(() => { ElMessage.success('已驳回'); getList() })
    .catch(() => {})
}

onMounted(() => {
  invoiceStatusOptions().then((res) => { statusOpts.value = res.data || [] }).catch(() => {})
  getList()
})
</script>

<style scoped>
.query-form {
  margin-bottom: 8px;
}
.tag {
  margin-left: 6px;
  font-size: 12px;
  color: #7b6cf6;
}
.done {
  color: #b6c2c0;
  font-size: 13px;
}
</style>

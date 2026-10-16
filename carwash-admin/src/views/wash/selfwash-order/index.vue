<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
          <el-option v-for="s in statusOpts" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="站点">
        <el-select v-model="query.siteId" placeholder="全部站点" clearable filterable style="width: 200px">
          <el-option v-for="s in siteOpts" :key="s.value" :label="s.label" :value="s.value" />
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
      <el-table-column label="订单号" prop="orderNo" min-width="190" show-overflow-tooltip />
      <el-table-column label="会员ID" prop="memberId" width="90" />
      <el-table-column label="工位" prop="workstationId" width="70" align="center" />
      <el-table-column label="服务" prop="serviceName" min-width="150" show-overflow-tooltip />
      <el-table-column label="实付(元)" width="100" align="right">
        <template #default="{ row }">{{ yuan(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusLabel || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排队" prop="queueNo" width="60" align="center" />
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 'SERVING'"
            link
            type="danger"
            @click="forceFinish(row)"
            v-hasPermi="['wash:selfwash:order:finish']"
          >强制结束</el-button>
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

    <el-dialog title="自助订单详情" v-model="detailOpen" width="520px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ detail.memberId }}</el-descriptions-item>
        <el-descriptions-item label="工位">{{ detail.workstationId }}</el-descriptions-item>
        <el-descriptions-item label="服务">{{ detail.serviceName }}（{{ detail.serviceMinutes }} 分钟）</el-descriptions-item>
        <el-descriptions-item label="实付">¥{{ yuan(detail.payAmount) }}（{{ detail.payChannel === 'BALANCE' ? '储值' : '微信' }}）</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusLabel }}</el-descriptions-item>
        <el-descriptions-item label="排队号">{{ detail.queueNo ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="加时">{{ detail.addedSeconds ? Math.floor(detail.addedSeconds / 60) + ' 分钟' : '无' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fmtTime(detail.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="SelfwashOrder">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSelfwashOrders, getSelfwashOrder, finishSelfwashOrder, selfwashStatusOptions } from '@/api/wash/selfwash-order'
import { siteOptions as fetchSiteOptions } from '@/api/wash/site'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const statusOpts = ref([])
const siteOpts = ref([])
const detailOpen = ref(false)
const detail = ref(null)

const query = reactive({ status: undefined, siteId: undefined, orderNo: undefined, memberId: undefined, pageNum: 1, pageSize: 10 })

function yuan(fen) {
  return ((fen || 0) / 100).toFixed(2)
}

function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

function statusType(s) {
  const map = { WAIT_PAY: 'warning', WAIT_USE: 'info', QUEUING: '', SERVING: 'success', PAUSED: 'danger', DONE: '', CANCELED: 'info' }
  return map[s] ?? 'info'
}

function getList() {
  loading.value = true
  listSelfwashOrders({
    status: query.status, siteId: query.siteId, orderNo: query.orderNo,
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
  query.siteId = undefined
  query.orderNo = undefined
  query.memberId = undefined
  query.pageNum = 1
  getList()
}

function openDetail(row) {
  getSelfwashOrder(row.orderNo).then((res) => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function forceFinish(row) {
  ElMessageBox.confirm(`确认强制结束订单「${row.orderNo}」？将断电并开始下一排队单。`, '强制结束', { type: 'warning' })
    .then(() => finishSelfwashOrder(row.orderNo))
    .then(() => { ElMessage.success('已强制结束'); getList() })
    .catch(() => {})
}

onMounted(() => {
  selfwashStatusOptions().then((res) => { statusOpts.value = res.data || [] }).catch(() => {})
  fetchSiteOptions().then((res) => { siteOpts.value = res.data || [] }).catch(() => {})
  getList()
})
</script>

<style scoped>
.query-form {
  margin-bottom: 8px;
}
</style>

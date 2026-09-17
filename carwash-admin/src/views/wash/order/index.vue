<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" label-width="90px">
      <el-form-item label="订单号">
        <el-input v-model="queryParams.orderNo" placeholder="支持模糊查询" clearable style="width: 220px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="订单状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="item in statusList" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="orderList" border>
      <el-table-column label="订单号" prop="orderNo" min-width="200" show-overflow-tooltip />
      <el-table-column label="服务项" prop="serviceName" min-width="180" show-overflow-tooltip />
      <el-table-column label="车牌" prop="plateNo" width="110" />
      <el-table-column label="状态" prop="statusLabel" width="140" />
      <el-table-column label="金额" width="100" align="right">
        <template #default="scope">¥{{ formatAmount(scope.row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="会员ID" prop="memberId" width="100" />
      <el-table-column label="站点ID" prop="siteId" width="100" />
      <el-table-column label="创建时间" prop="createTime" width="170" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Order">
import { listOrder, statusOptions } from '@/api/wash/order'

const orderList = ref([])
const statusList = ref([])
const loading = ref(true)
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  orderNo: undefined,
  status: undefined
})

/** 金额：后端存分，展示层转元 */
function formatAmount(amount) {
  return ((amount ?? 0) / 100).toFixed(2)
}

function getList() {
  loading.value = true
  listOrder(queryParams.value).then(res => {
    orderList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10, orderNo: undefined, status: undefined }
  getList()
}

statusOptions().then(res => {
  statusList.value = res.data
})

getList()
</script>

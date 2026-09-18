<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" label-width="80px">
      <el-form-item label="车牌">
        <el-input v-model="queryParams.plateNo" placeholder="支持模糊查询" clearable style="width: 180px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会员ID">
        <el-input v-model="queryParams.memberId" placeholder="按会员过滤" clearable style="width: 160px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="vehicleList" border>
      <el-table-column label="车辆ID" prop="vehicleId" width="100" />
      <el-table-column label="会员ID" prop="memberId" width="100" />
      <el-table-column label="车牌" prop="plateNo" width="130" />
      <el-table-column label="品牌" prop="brand" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.brand || '-' }}</template>
      </el-table-column>
      <el-table-column label="颜色" prop="color" width="100">
        <template #default="scope">{{ scope.row.color || '-' }}</template>
      </el-table-column>
      <el-table-column label="新能源" width="100">
        <template #default="scope">{{ scope.row.isNewEnergy === 'Y' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="车位" prop="parkingNo" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.parkingNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="viewOrders(scope.row.memberId)">他的订单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Vehicle">
import { listVehicle } from '@/api/wash/member'
import { useRouter } from 'vue-router'

const router = useRouter()
const vehicleList = ref([])
const loading = ref(true)
const total = ref(0)

const queryParams = ref({ pageNum: 1, pageSize: 10, plateNo: undefined, memberId: undefined })

function getList() {
  loading.value = true
  const params = {
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize,
    plateNo: queryParams.value.plateNo,
    // 空字符串会让后端把它当成有效过滤条件，这里显式转成 undefined
    memberId: queryParams.value.memberId === '' || queryParams.value.memberId === undefined
      ? undefined
      : Number(queryParams.value.memberId)
  }
  listVehicle(params).then(res => {
    vehicleList.value = res.rows
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
  queryParams.value = { pageNum: 1, pageSize: 10, plateNo: undefined, memberId: undefined }
  getList()
}

function viewOrders(memberId) {
  router.push({ path: '/wash/order', query: { memberId } })
}

getList()
</script>

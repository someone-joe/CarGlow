<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" label-width="80px">
      <el-form-item label="关键字">
        <el-input v-model="queryParams.keyword" placeholder="昵称 / 手机号" clearable style="width: 220px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" show-icon
              title="手机号与 openid 已脱敏展示（个保法最小化）。如需联系客户，请通过订单详情里的业务联系入口。" />

    <el-table v-loading="loading" :data="memberList" border style="margin-top: 12px">
      <el-table-column label="会员ID" prop="memberId" width="100" />
      <el-table-column label="昵称" prop="nickname" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="手机号" prop="phoneMasked" width="140" />
      <el-table-column label="openid" prop="openidMasked" min-width="180" show-overflow-tooltip />
      <el-table-column label="注册时间" prop="createTime" width="170" />
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

<script setup name="Member">
import { listMember } from '@/api/wash/member'
import { useRouter } from 'vue-router'

const router = useRouter()
const memberList = ref([])
const loading = ref(true)
const total = ref(0)

const queryParams = ref({ pageNum: 1, pageSize: 10, keyword: undefined })

function getList() {
  loading.value = true
  listMember(queryParams.value).then(res => {
    memberList.value = res.rows
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
  queryParams.value = { pageNum: 1, pageSize: 10, keyword: undefined }
  getList()
}

/** 跳到订单页并带上会员筛选：客服接到电话时的主要路径 */
function viewOrders(memberId) {
  router.push({ path: '/wash/order', query: { memberId } })
}

getList()
</script>

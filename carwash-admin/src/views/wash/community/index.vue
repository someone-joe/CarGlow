<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="小区名称" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="小区名称" clearable style="width:200px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所属站点" prop="siteId">
        <el-select v-model="queryParams.siteId" placeholder="全部" clearable style="width:160px">
          <el-option v-for="s in siteList" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:community:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="communityList">
      <el-table-column label="小区ID" prop="communityId" width="90" />
      <el-table-column label="小区名称" prop="communityName" />
      <el-table-column label="所属站点" width="140">
        <template #default="scope">{{ siteNameMap[scope.row.siteId] || '-' }}</template>
      </el-table-column>
      <el-table-column label="地库授权" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.accessAuth === 'Y' ? 'success' : 'danger'">
            {{ scope.row.accessAuth === 'Y' ? '已授权' : '未授权' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 'Y' ? 'success' : 'info'">
            {{ scope.row.enabled === 'Y' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['wash:community:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['wash:community:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="communityRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="小区名称" prop="communityName">
          <el-input v-model="form.communityName" placeholder="如：万科城" />
        </el-form-item>
        <el-form-item label="所属站点" prop="siteId">
          <el-select v-model="form.siteId" placeholder="请选择" style="width:100%">
            <el-option v-for="s in siteList" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="地库授权" prop="accessAuth">
          <el-radio-group v-model="form.accessAuth">
            <el-radio value="Y">已授权（可下单）</el-radio>
            <el-radio value="N">未授权</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio value="Y">启用</el-radio>
            <el-radio value="N">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WashCommunity">
import { listCommunity, getCommunity, addCommunity, updateCommunity, delCommunity } from '@/api/wash/community'
import { siteOptions } from '@/api/wash/site'

const { proxy } = getCurrentInstance()

const communityList = ref([])
const siteList = ref([])
const siteNameMap = ref({})
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: undefined,
    siteId: undefined
  },
  rules: {
    communityName: [{ required: true, message: '小区名称不能为空', trigger: 'blur' }],
    siteId: [{ required: true, message: '请选择所属站点', trigger: 'change' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

function loadSites() {
  siteOptions().then(res => {
    siteList.value = res.data || []
    const m = {}
    for (const s of (res.data || [])) m[s.value] = s.label
    siteNameMap.value = m
  })
}

function getList() {
  loading.value = true
  listCommunity(queryParams.value).then(res => {
    communityList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    communityId: undefined,
    communityName: undefined,
    siteId: undefined,
    accessAuth: 'Y',
    enabled: 'Y'
  }
  proxy.resetForm('communityRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增小区'
}

function handleUpdate(row) {
  reset()
  getCommunity(row.communityId).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改小区'
  })
}

function submitForm() {
  proxy.$refs.communityRef.validate(valid => {
    if (!valid) return
    const payload = { ...form.value }
    if (payload.communityId) {
      updateCommunity(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addCommunity(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除小区「${row.communityName}」？`).then(() => {
    return delCommunity(row.communityId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

loadSites()
getList()
</script>

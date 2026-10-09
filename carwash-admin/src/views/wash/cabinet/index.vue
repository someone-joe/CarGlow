<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="机柜名称" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="机柜名称" clearable style="width:200px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所在小区" prop="communityId">
        <el-select v-model="queryParams.communityId" placeholder="全部小区" clearable style="width:200px">
          <el-option v-for="c in communityOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:cabinet:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="cabinetList">
      <el-table-column label="机柜ID" prop="cabinetId" width="90" />
      <el-table-column label="机柜名称" prop="cabinetName" />
      <el-table-column label="所属站点" prop="siteName" />
      <el-table-column label="所在小区" prop="communityName" />
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
                     v-hasPermi="['wash:cabinet:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['wash:cabinet:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="cabinetRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="机柜名称" prop="cabinetName">
          <el-input v-model="form.cabinetName" placeholder="如：3栋负一层柜" />
        </el-form-item>
        <el-form-item label="所属站点" prop="siteId">
          <el-select v-model="form.siteId" placeholder="请选择站点" style="width:100%"
                     @change="onSiteChange">
            <el-option v-for="s in siteOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在小区" prop="communityId">
          <el-select v-model="form.communityId" placeholder="请选择小区" style="width:100%">
            <el-option v-for="c in communityOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态" prop="enabled">
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

<script setup name="WashCabinet">
import { listCabinet, getCabinet, addCabinet, updateCabinet, delCabinet, cabinetOptions as fetchCabinetOptions } from '@/api/wash/cabinet'
import { siteOptions as fetchSiteOptions } from '@/api/wash/site'
import { communityOptions as fetchCommunityOptions } from '@/api/wash/community'

const { proxy } = getCurrentInstance()

const cabinetList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const siteOptions = ref([])
const communityOptions = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: undefined,
    communityId: undefined
  },
  rules: {
    cabinetName: [{ required: true, message: '机柜名称不能为空', trigger: 'blur' }],
    siteId: [{ required: true, message: '所属站点不能为空', trigger: 'change' }],
    communityId: [{ required: true, message: '所在小区不能为空', trigger: 'change' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

function getList() {
  loading.value = true
  listCabinet(queryParams.value).then(res => {
    cabinetList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function loadSiteOptions() {
  fetchSiteOptions().then(res => { siteOptions.value = res.data })
}

function loadCommunityOptions(siteId) {
  fetchCommunityOptions(siteId).then(res => { communityOptions.value = res.data })
}

function onSiteChange(siteId) {
  form.value.communityId = undefined
  loadCommunityOptions(siteId)
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    cabinetId: undefined,
    cabinetName: undefined,
    siteId: undefined,
    communityId: undefined,
    enabled: 'Y'
  }
  proxy.resetForm('cabinetRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.communityId = undefined
  handleQuery()
}

function handleAdd() {
  reset()
  loadCommunityOptions(undefined)
  open.value = true
  title.value = '新增机柜'
}

function handleUpdate(row) {
  reset()
  getCabinet(row.cabinetId).then(res => {
    form.value = res.data
    loadCommunityOptions(form.value.siteId)
    open.value = true
    title.value = '修改机柜'
  })
}

function submitForm() {
  proxy.$refs.cabinetRef.validate(valid => {
    if (!valid) return
    const payload = { ...form.value }
    if (payload.cabinetId) {
      updateCabinet(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addCabinet(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除机柜「${row.cabinetName}」？删除前请先删除其下格口。`).then(() => {
    return delCabinet(row.cabinetId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

loadSiteOptions()
getList()
</script>

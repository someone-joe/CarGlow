<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="站点名称" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="站点名称" clearable style="width:200px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:site:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="siteList">
      <el-table-column label="站点ID" prop="siteId" width="90" />
      <el-table-column label="站点名称" prop="siteName" />
      <el-table-column label="日产能上限" width="110">
        <template #default="scope">{{ scope.row.dailyLimit === 0 ? '不限量' : scope.row.dailyLimit }}</template>
      </el-table-column>
      <el-table-column label="营业时段" width="160">
        <template #default="scope">{{ scope.row.businessStart }} - {{ scope.row.businessEnd }}</template>
      </el-table-column>
      <el-table-column label="最晚存钥匙" prop="depositDeadline" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.serviceStatus === 'OPEN' ? 'success' : 'info'">
            {{ scope.row.serviceStatus === 'OPEN' ? '营业中' : '已停业' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="停业公告" prop="closedNotice" show-overflow-tooltip />
      <el-table-column label="详情配置" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.detailTitle ? 'success' : 'info'">
            {{ scope.row.detailTitle ? '已配置' : '未配置' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['wash:site:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['wash:site:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="siteRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="站点名称" prop="siteName">
          <el-input v-model="form.siteName" placeholder="如：中央站A" />
        </el-form-item>
        <el-form-item label="日产能上限" prop="dailyLimit">
          <el-input v-model.number="form.dailyLimit" placeholder="0 表示不限量" />
        </el-form-item>
        <el-form-item label="营业时段起" prop="businessStart">
          <el-input v-model="form.businessStart" placeholder="如 19:00" />
        </el-form-item>
        <el-form-item label="营业时段止" prop="businessEnd">
          <el-input v-model="form.businessEnd" placeholder="如 23:00" />
        </el-form-item>
        <el-form-item label="最晚存钥匙" prop="depositDeadline">
          <el-input v-model="form.depositDeadline" placeholder="如 23:30" />
        </el-form-item>
        <el-form-item label="承诺还车" prop="promiseReturnTime">
          <el-input v-model="form.promiseReturnTime" placeholder="如 次日 07:00" />
        </el-form-item>
        <el-form-item label="营业状态" prop="serviceStatus">
          <el-radio-group v-model="form.serviceStatus">
            <el-radio value="OPEN">营业中</el-radio>
            <el-radio value="CLOSED">已停业</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="停业公告" prop="closedNotice">
          <el-input v-model="form.closedNotice" type="textarea" :rows="2" placeholder="停业时展示给C端" />
        </el-form-item>

        <el-divider content-position="left">站点详情（小程序服务页 Hero，未配置则回退默认）</el-divider>
        <el-form-item label="详情标题" prop="detailTitle">
          <el-input v-model="form.detailTitle" placeholder="如：科学洗车；留空回退站点名" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="详情副标题" prop="detailSubtitle">
          <el-input v-model="form.detailSubtitle" placeholder="如：中央站集中精洗 · 标准化 SOP" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="详情头图" prop="detailImageUrl">
          <el-input v-model="form.detailImageUrl" placeholder="头图 URL；留空则小程序渲染渐变占位图" maxlength="255" />
        </el-form-item>
        <el-form-item label="详情文案" prop="detailDesc">
          <el-input v-model="form.detailDesc" type="textarea" :rows="2" placeholder="站点补充说明（选填）" maxlength="512" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WashSite">
import { listSite, getSite, addSite, updateSite, delSite } from '@/api/wash/site'

const { proxy } = getCurrentInstance()

const siteList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: undefined
  },
  rules: {
    siteName: [{ required: true, message: '站点名称不能为空', trigger: 'blur' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

function getList() {
  loading.value = true
  listSite(queryParams.value).then(res => {
    siteList.value = res.rows
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
    siteId: undefined,
    siteName: undefined,
    dailyLimit: 0,
    businessStart: '19:00',
    businessEnd: '23:00',
    depositDeadline: undefined,
    promiseReturnTime: undefined,
    serviceStatus: 'OPEN',
    closedNotice: undefined,
    detailTitle: undefined,
    detailSubtitle: undefined,
    detailImageUrl: undefined,
    detailDesc: undefined
  }
  proxy.resetForm('siteRef')
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
  title.value = '新增站点'
}

function handleUpdate(row) {
  reset()
  getSite(row.siteId).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改站点'
  })
}

function submitForm() {
  proxy.$refs.siteRef.validate(valid => {
    if (!valid) return
    const payload = { ...form.value }
    if (payload.siteId) {
      updateSite(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addSite(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除站点「${row.siteName}」？`).then(() => {
    return delSite(row.siteId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>

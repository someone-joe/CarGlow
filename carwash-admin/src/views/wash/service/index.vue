<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="服务名称" prop="serviceName">
        <el-input v-model="queryParams.serviceName" placeholder="请输入服务名称" clearable style="width: 180px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所属分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="全部分类" clearable style="width: 160px">
          <el-option v-for="c in categoryList" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:service:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="serviceList">
      <el-table-column label="服务ID" prop="serviceId" width="90" />
      <el-table-column label="服务名称" prop="serviceName" min-width="160" show-overflow-tooltip />
      <el-table-column label="分类" width="120">
        <template #default="scope">{{ categoryLabel(scope.row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="价格" width="110">
        <template #default="scope">¥{{ fen2yuan(scope.row.priceAmount) }}</template>
      </el-table-column>
      <el-table-column label="工时(分钟)" prop="workMinutes" width="110" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 'Y' ? 'success' : 'info'">
            {{ scope.row.enabled === 'Y' ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['wash:service:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['wash:service:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 修改对话框 -->
    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="serviceRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="服务名称" prop="serviceName">
          <el-input v-model="form.serviceName" placeholder="如：标准洗车（外观+内饰吸尘）" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categoryList" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)" prop="priceYuan">
          <el-input-number v-model="form.priceYuan" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工时(分钟)" prop="workMinutes">
          <el-input-number v-model="form.workMinutes" :min="1" :step="5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上架状态" prop="enabled">
          <el-switch v-model="form.enabled" active-value="Y" inactive-value="N"
                     active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WashService">
import { listService, addService, updateService, delService, serviceCategoryOptions } from '@/api/wash/service'

const { proxy } = getCurrentInstance()

const serviceList = ref([])
const categoryList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    serviceName: undefined,
    categoryId: undefined
  },
  rules: {
    serviceName: [{ required: true, message: '服务名称不能为空', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
    priceYuan: [{ required: true, message: '价格不能为空', trigger: 'blur' }],
    workMinutes: [{ required: true, message: '预计工时不能为空', trigger: 'blur' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

/** 金额：后端存分，展示与输入都用元，只在提交/回显时换算 */
function fen2yuan(fen) {
  return ((fen || 0) / 100).toFixed(2)
}

function categoryLabel(categoryId) {
  const hit = categoryList.value.find((c) => c.value === categoryId)
  return hit ? hit.label : '-'
}

function getList() {
  loading.value = true
  listService(queryParams.value).then(res => {
    serviceList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function loadCategories() {
  serviceCategoryOptions().then(res => {
    categoryList.value = res.data || []
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    serviceId: undefined,
    serviceName: undefined,
    categoryId: undefined,
    priceYuan: 0,
    workMinutes: 40,
    enabled: 'Y'
  }
  proxy.resetForm('serviceRef')
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
  title.value = '新增服务项'
}

function handleUpdate(row) {
  reset()
  form.value = {
    serviceId: row.serviceId,
    serviceName: row.serviceName,
    categoryId: row.categoryId,
    priceYuan: Number(((row.priceAmount || 0) / 100).toFixed(2)),
    workMinutes: row.workMinutes,
    enabled: row.enabled
  }
  open.value = true
  title.value = '修改服务项'
}

function submitForm() {
  proxy.$refs.serviceRef.validate(valid => {
    if (!valid) return
    // 元 → 分：金额一律以「分」传给后端
    const payload = {
      serviceId: form.value.serviceId,
      serviceName: form.value.serviceName,
      categoryId: form.value.categoryId,
      priceAmount: Math.round((form.value.priceYuan || 0) * 100),
      workMinutes: form.value.workMinutes,
      enabled: form.value.enabled
    }
    if (payload.serviceId) {
      updateService(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addService(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除服务项「${row.serviceName}」？删除后 C 端不再展示（历史订单不受影响）`).then(() => {
    return delService(row.serviceId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

loadCategories()
getList()
</script>

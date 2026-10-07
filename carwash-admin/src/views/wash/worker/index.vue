<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="关键字" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="工号 / 姓名 / 手机号" clearable style="width: 200px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所属站点" prop="siteId">
        <el-select v-model="queryParams.siteId" placeholder="全部站点" clearable style="width: 160px">
          <el-option v-for="s in siteList" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option v-for="s in statusList" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:worker:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="workerList">
      <el-table-column label="工号" prop="workerNo" width="110" />
      <el-table-column label="姓名" prop="name" width="110" />
      <el-table-column label="手机号" prop="phone" width="140" />
      <el-table-column label="所属站点" width="140">
        <template #default="scope">{{ scope.row.siteName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <!-- 中文名由后端下发（statusLabel），前端不自己维护状态字典 -->
          <el-tag :type="scope.row.status === 'NORMAL' ? 'success' : 'danger'">
            {{ scope.row.statusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['wash:worker:edit']">修改</el-button>
          <el-button link type="warning" icon="Key" @click="handleResetPwd(scope.row)"
                     v-hasPermi="['wash:worker:reset']">重置密码</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['wash:worker:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 修改对话框 -->
    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="workerRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="工号" prop="workerNo">
          <!-- 工号是登录凭证，不允许改：改了等于给别人换账号 -->
          <el-input v-model="form.workerNo" placeholder="如：W002" :disabled="!!form.workerId" />
        </el-form-item>
        <el-form-item v-if="!form.workerId" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位，线下告知师傅" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="如：张师傅" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="所属站点" prop="siteId">
          <el-select v-model="form.siteId" placeholder="请选择站点" style="width: 100%">
            <el-option v-for="s in siteList" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="s in statusList" :key="s.value" :value="s.value">{{ s.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框：独立弹窗，避免与「修改资料」混在一起误触 -->
    <el-dialog title="重置密码" v-model="pwdOpen" width="420px" append-to-body>
      <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="工号">
          <el-input v-model="pwdForm.workerNo" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitPwd">确 定</el-button>
        <el-button @click="pwdOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WashWorker">
import {
  listWorker, addWorker, updateWorker, delWorker, resetWorkerPassword,
  workerStatusOptions, workerSiteOptions
} from '@/api/wash/worker'

const { proxy } = getCurrentInstance()

const workerList = ref([])
const statusList = ref([])
const siteList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const pwdOpen = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  pwdForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    keyword: undefined,
    siteId: undefined,
    status: undefined
  },
  rules: {
    workerNo: [{ required: true, message: '工号不能为空', trigger: 'blur' }],
    password: [
      { required: true, message: '初始密码不能为空', trigger: 'blur' },
      { min: 6, message: '密码至少 6 位', trigger: 'blur' }
    ],
    name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
    siteId: [{ required: true, message: '请选择所属站点', trigger: 'change' }]
  },
  pwdRules: {
    password: [
      { required: true, message: '新密码不能为空', trigger: 'blur' },
      { min: 6, message: '密码至少 6 位', trigger: 'blur' }
    ]
  }
})

const { form, pwdForm, queryParams, rules, pwdRules } = toRefs(data)

function getList() {
  loading.value = true
  listWorker(queryParams.value).then(res => {
    workerList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

/** 状态与站点下拉都由后端下发，前端不自带字典 */
function loadOptions() {
  workerStatusOptions().then(res => { statusList.value = res.data || [] })
  workerSiteOptions().then(res => { siteList.value = res.data || [] })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    workerId: undefined,
    workerNo: undefined,
    password: undefined,
    name: undefined,
    phone: undefined,
    siteId: undefined,
    status: 'NORMAL'
  }
  proxy.resetForm('workerRef')
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
  title.value = '新增师傅'
}

function handleUpdate(row) {
  reset()
  // 不回填 password：后端也不接受，改密码必须走重置密码
  form.value = {
    workerId: row.workerId,
    workerNo: row.workerNo,
    name: row.name,
    phone: row.phone,
    siteId: row.siteId,
    status: row.status
  }
  open.value = true
  title.value = '修改师傅'
}

function submitForm() {
  proxy.$refs.workerRef.validate(valid => {
    if (!valid) return
    const payload = {
      workerId: form.value.workerId,
      workerNo: form.value.workerNo,
      name: form.value.name,
      phone: form.value.phone,
      siteId: form.value.siteId,
      status: form.value.status
    }
    if (payload.workerId) {
      updateWorker(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      // 只有新增才带初始密码
      addWorker({ ...payload, password: form.value.password }).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleResetPwd(row) {
  pwdForm.value = { workerId: row.workerId, workerNo: row.workerNo, password: undefined }
  pwdOpen.value = true
  proxy.resetForm('pwdRef')
}

function submitPwd() {
  proxy.$refs.pwdRef.validate(valid => {
    if (!valid) return
    resetWorkerPassword(pwdForm.value.workerId, pwdForm.value.password).then(() => {
      proxy.$modal.msgSuccess('密码已重置，该师傅的旧登录态已失效')
      pwdOpen.value = false
    })
  })
}

function handleDelete(row) {
  const tip = row.status === 'NORMAL'
    ? `确认删除师傅「${row.name}」（${row.workerNo}）？该师傅将无法登录。`
    : `确认删除师傅「${row.name}」（${row.workerNo}）？`
  proxy.$modal.confirm(tip).then(() => {
    return delWorker(row.workerId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

loadOptions()
getList()
</script>

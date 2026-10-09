<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="所属机柜" prop="cabinetId">
        <el-select v-model="queryParams.cabinetId" placeholder="全部机柜" clearable style="width:200px"
                   @change="handleQuery">
          <el-option v-for="c in cabinetOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="格口状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width:160px"
                   @change="handleQuery">
          <el-option v-for="s in slotStatusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="info" plain icon="Document" @click="openLogDialog()">开箱记录</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:slot:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="slotList">
      <el-table-column label="格口ID" prop="slotId" width="90" />
      <el-table-column label="所属机柜" prop="cabinetName" />
      <el-table-column label="格口号" prop="slotNo" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前订单" prop="orderNo" width="140" show-overflow-tooltip />
      <el-table-column label="会员ID" prop="memberId" width="100" />
      <el-table-column label="创建时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['wash:slot:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['wash:slot:remove']">删除</el-button>
          <el-button link type="info" icon="Document" @click="openLogDialog(scope.row)"
                     v-hasPermi="['wash:slot:list']">开箱记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="slotRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属机柜" prop="cabinetId">
          <el-select v-model="form.cabinetId" placeholder="请选择机柜" style="width:100%">
            <el-option v-for="c in cabinetOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="格口号" prop="slotNo">
          <el-input v-model="form.slotNo" placeholder="如 A01（柜内唯一）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
            <el-option v-for="s in slotStatusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <el-dialog title="开箱留痕" v-model="logOpen" width="880px" append-to-body>
      <el-table v-loading="logLoading" :data="logList">
        <el-table-column label="动作" width="100">
          <template #default="scope">{{ actionLabel(scope.row.action) }}</template>
        </el-table-column>
        <el-table-column label="格口号" prop="slotNo" width="100" />
        <el-table-column label="订单号" prop="orderNo" width="150" show-overflow-tooltip />
        <el-table-column label="角色" width="100">
          <template #default="scope">{{ roleLabel(scope.row.role) }}</template>
        </el-table-column>
        <el-table-column label="来源端" width="130">
          <template #default="scope">{{ sourceLabel(scope.row.source) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.result === 'FAIL' ? 'danger' : 'success'">
              {{ resultLabel(scope.row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开箱码" prop="openCode" width="100" />
        <el-table-column label="时间" prop="createTime" width="160" />
      </el-table>
      <pagination v-show="logTotal > 0" :total="logTotal" v-model:page="logQuery.pageNum"
                  v-model:limit="logQuery.pageSize" @pagination="getLogList" />
    </el-dialog>
  </div>
</template>

<script setup name="WashSlot">
import { listSlot, getSlot, addSlot, updateSlot, delSlot, slotOptions, openLog } from '@/api/wash/slot'
import { cabinetOptions as fetchCabinetOptions } from '@/api/wash/cabinet'

const { proxy } = getCurrentInstance()

const slotStatusOptions = [
  { value: 'FREE', label: '空闲' },
  { value: 'RESERVED', label: '已预占' },
  { value: 'OCCUPIED', label: '占用中' },
  { value: 'DISABLED', label: '停用' }
]

const slotList = ref([])
const loading = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const cabinetOptions = ref([])

const logOpen = ref(false)
const logLoading = ref(false)
const logList = ref([])
const logTotal = ref(0)
const logQuery = reactive({ pageNum: 1, pageSize: 10, cabinetId: undefined, slotId: undefined })

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    cabinetId: undefined,
    status: undefined
  },
  rules: {
    cabinetId: [{ required: true, message: '所属机柜不能为空', trigger: 'change' }],
    slotNo: [{ required: true, message: '格口号不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
})

const { form, queryParams, rules } = toRefs(data)

function statusLabel(s) {
  return slotStatusOptions.find(x => x.value === s)?.label || s
}
function statusTagType(s) {
  return s === 'FREE' ? 'success' : s === 'RESERVED' ? 'warning' : s === 'OCCUPIED' ? 'danger' : 'info'
}
function actionLabel(a) {
  return { OPEN_CODE: '取码', OPEN: '开箱', DEPOSIT: '存入', TAKE: '取回', RELEASE: '释放' }[a] || a
}
function roleLabel(r) {
  return { CUSTOMER: '客户', WORKER: '工作人员', ADMIN: '管理员', SYSTEM: '系统' }[r] || r || '-'
}
function sourceLabel(s) {
  return { MP: '客户小程序', WORKER: '师傅小程序', ADMIN: '后台', DEVICE: '柜机' }[s] || s || '-'
}
function resultLabel(r) {
  return r === 'SUCCESS' ? '成功' : r === 'FAIL' ? '失败' : (r || '-')
}

function getList() {
  loading.value = true
  listSlot(queryParams.value).then(res => {
    slotList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function loadCabinetOptions() {
  fetchCabinetOptions().then(res => { cabinetOptions.value = res.data })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function reset() {
  form.value = {
    slotId: undefined,
    cabinetId: undefined,
    slotNo: undefined,
    status: 'FREE'
  }
  proxy.resetForm('slotRef')
}

function cancel() {
  open.value = false
  reset()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增格口'
}

function handleUpdate(row) {
  reset()
  getSlot(row.slotId).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改格口'
  })
}

function submitForm() {
  proxy.$refs.slotRef.validate(valid => {
    if (!valid) return
    const payload = { ...form.value }
    if (payload.slotId) {
      updateSlot(payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      })
    } else {
      addSlot(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除格口「${row.cabinetName} - ${row.slotNo}」？`).then(() => {
    return delSlot(row.slotId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function openLogDialog(row) {
  logQuery.pageNum = 1
  logQuery.slotId = row ? row.slotId : undefined
  logQuery.cabinetId = row ? row.cabinetId : queryParams.value.cabinetId
  logOpen.value = true
  getLogList()
}

function getLogList() {
  logLoading.value = true
  openLog(logQuery).then(res => {
    logList.value = res.rows
    logTotal.value = res.total
    logLoading.value = false
  })
}

loadCabinetOptions()
getList()
</script>

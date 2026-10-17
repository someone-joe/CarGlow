<template>
  <div class="app-container">
    <el-tabs v-model="topTab">
      <!-- ============ 师傅管理 ============ -->
      <el-tab-pane label="师傅管理" name="manage">
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
              <el-tag :type="scope.row.status === 'NORMAL' ? 'success' : 'danger'">
                {{ scope.row.statusLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="170" />
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                         v-hasPermi="['wash:worker:edit']">修改</el-button>
              <el-button link type="warning" icon="Key" @click="handleResetPwd(scope.row)"
                         v-hasPermi="['wash:worker:reset']">重置密码</el-button>
              <el-button link type="success" icon="Calendar" @click="openSchedule(scope.row)"
                         v-hasPermi="['wash:worker:schedule']">排班</el-button>
              <el-button link type="primary" icon="Promotion" @click="openDispatch(scope.row)"
                         v-hasPermi="['wash:order:dispatch']">派单</el-button>
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

        <!-- 重置密码对话框 -->
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

        <!-- 排班对话框 -->
        <el-dialog :title="`排班维护 - ${scheduleWorkerName}`" v-model="scheduleOpen" width="560px" append-to-body>
          <el-table :data="scheduleList">
            <el-table-column label="星期" width="100">
              <template #default="scope">{{ weekLabel(scope.row.weekDay) }}</template>
            </el-table-column>
            <el-table-column label="启用" width="80">
              <template #default="scope">
                <el-switch v-model="scope.row.enabled" />
              </template>
            </el-table-column>
            <el-table-column label="开始" width="170">
              <template #default="scope">
                <el-time-picker v-model="scope.row.start" value-format="HH:mm" format="HH:mm" placeholder="开始"
                                style="width: 150px" />
              </template>
            </el-table-column>
            <el-table-column label="结束" width="170">
              <template #default="scope">
                <el-time-picker v-model="scope.row.end" value-format="HH:mm" format="HH:mm" placeholder="结束"
                                style="width: 150px" />
              </template>
            </el-table-column>
          </el-table>
          <template #footer>
            <el-button type="primary" @click="submitSchedule">保 存</el-button>
            <el-button @click="scheduleOpen = false">取 消</el-button>
          </template>
        </el-dialog>

        <!-- 派单 / 改派对话框 -->
        <el-dialog :title="`派单 - ${dispatchWorkerName}`" v-model="dispatchOpen" width="760px" append-to-body>
          <el-tabs v-model="dispatchTab">
            <el-tab-pane label="派单（待派单池）" name="assign">
              <el-table :data="poolList" v-loading="poolLoading" max-height="360">
                <el-table-column label="单号" prop="orderNo" width="200" />
                <el-table-column label="服务" prop="serviceName" min-width="120" />
                <el-table-column label="车牌" prop="vehiclePlate" width="110" />
                <el-table-column label="状态" prop="status" width="100" />
                <el-table-column label="操作" width="100">
                  <template #default="scope">
                    <el-button link type="primary" @click="assign(scope.row.orderNo)">派给TA</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!poolLoading && !poolList.length" description="本站点暂无待派单订单" />
            </el-tab-pane>
            <el-tab-pane label="改派（本师傅订单）" name="reassign">
              <el-table :data="myTaskList" v-loading="taskLoading" max-height="360">
                <el-table-column label="单号" prop="orderNo" width="200" />
                <el-table-column label="服务" prop="serviceName" min-width="120" />
                <el-table-column label="车牌" prop="vehiclePlate" width="110" />
                <el-table-column label="状态" prop="status" width="100" />
                <el-table-column label="操作" width="100">
                  <template #default="scope">
                    <el-button link type="warning" @click="openReassign(scope.row)">改派</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!taskLoading && !myTaskList.length" description="该师傅暂无进行中订单" />
            </el-tab-pane>
          </el-tabs>
        </el-dialog>

        <!-- 改派：选新师傅 -->
        <el-dialog title="改派给其他师傅" v-model="reassignOpen" width="520px" append-to-body>
          <el-table :data="candidateList" v-loading="candLoading" max-height="320">
            <el-table-column label="师傅" prop="label" min-width="160" />
            <el-table-column label="进行中" prop="activeCount" width="90" />
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button link type="primary" @click="doReassign(scope.row.value)">指派</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!candLoading && !candidateList.length" description="本站点无其它合格师傅" />
        </el-dialog>
      </el-tab-pane>

      <!-- ============ 资质审核 ============ -->
      <el-tab-pane label="资质审核" name="audit">
        <el-form :model="auditQuery" :inline="true" label-width="80px">
          <el-form-item label="审核状态">
            <el-select v-model="auditQuery.reviewStatus" style="width: 160px" @change="getAuditList">
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="PASSED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-table v-loading="auditLoading" :data="auditList">
          <el-table-column label="师傅ID" prop="workerId" width="100" />
          <el-table-column label="姓名" prop="idName" width="110" />
          <el-table-column label="身份证号" prop="idNo" width="200" />
          <el-table-column label="人脸核身" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.faceStatus === 'PASSED' ? 'success' : scope.row.faceStatus === 'FAILED' ? 'danger' : 'info'">
                {{ faceText(scope.row.faceStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.reviewStatus === 'PASSED' ? 'success' : scope.row.reviewStatus === 'REJECTED' ? 'danger' : 'warning'">
                {{ reviewText(scope.row.reviewStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="viewQual(scope.row)">查看/核身</el-button>
              <el-button link type="success" icon="Check" @click="pass(scope.row)"
                         v-hasPermi="['wash:worker:audit']" :disabled="scope.row.reviewStatus === 'PASSED'">通过</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="auditTotal > 0" :total="auditTotal" v-model:page="auditQuery.pageNum"
                    v-model:limit="auditQuery.pageSize" @pagination="getAuditList" />

        <el-dialog title="资质详情 / 人工核身" v-model="qualOpen" width="560px" append-to-body>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="师傅ID">{{ currentQual.workerId }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ currentQual.idName }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ currentQual.idNo }}</el-descriptions-item>
            <el-descriptions-item label="人脸核身">{{ faceText(currentQual.faceStatus) }}</el-descriptions-item>
            <el-descriptions-item label="审核">{{ reviewText(currentQual.reviewStatus) }}</el-descriptions-item>
            <el-descriptions-item label="驳回原因">{{ currentQual.reviewRemark || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="imgs">
            <div v-if="currentQual.idFrontFile" class="img-wrap">
              <span>身份证正面</span>
              <img :src="qualImg(currentQual.idFrontFile)" class="q-img" />
            </div>
            <div v-if="currentQual.idBackFile" class="img-wrap">
              <span>身份证反面</span>
              <img :src="qualImg(currentQual.idBackFile)" class="q-img" />
            </div>
            <div v-if="currentQual.licenseFile" class="img-wrap">
              <span>驾照</span>
              <img :src="qualImg(currentQual.licenseFile)" class="q-img" />
            </div>
          </div>
          <el-divider />
          <div class="audit-actions" v-hasPermi="['wash:worker:audit']">
            <el-button type="success" @click="review(currentQual.workerId, 'PASSED')">审核通过</el-button>
            <el-button type="danger" @click="reject(currentQual.workerId)">驳回</el-button>
            <el-button type="primary" plain @click="facePass(currentQual.workerId)">登记人脸通过</el-button>
          </div>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="WashWorker">
import {
  listWorker, addWorker, updateWorker, delWorker, resetWorkerPassword,
  workerStatusOptions, workerSiteOptions,
  qualificationPage, reviewQualification, updateFace,
  getSchedule, saveSchedule, dispatchCandidates, dispatchOrder,
  getDispatchPool, getWorkerTasks,
} from '@/api/wash/worker'

const { proxy } = getCurrentInstance()

const topTab = ref('manage')
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

// ---------------- 排班 ----------------
const scheduleOpen = ref(false)
const scheduleWorkerId = ref(null)
const scheduleWorkerName = ref('')
const scheduleList = ref([])

function defaultSchedule() {
  const labels = [1, 2, 3, 4, 5, 6, 7]
  return labels.map(d => ({ weekDay: d, enabled: true, start: '09:00', end: '23:00' }))
}

function weekLabel(d) {
  return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][d] || d
}

function openSchedule(row) {
  scheduleWorkerId.value = row.workerId
  scheduleWorkerName.value = row.name
  scheduleOpen.value = true
  getSchedule(row.workerId).then(res => {
    const map = {}
    ;(res.data || []).forEach(s => {
      map[s.weekDay] = { weekDay: s.weekDay, enabled: s.enabled === 'Y', start: minToHHmm(s.startMinute), end: minToHHmm(s.endMinute) }
    })
    scheduleList.value = defaultSchedule().map(d => map[d.weekDay] || d)
  })
}

function minToHHmm(min) {
  if (min == null) return '09:00'
  const h = Math.floor(min / 60) % 24
  const m = min % 60
  return String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0')
}

function submitSchedule() {
  const rows = scheduleList.value
    .filter(r => r.enabled)
    .map(r => ({
      weekDay: r.weekDay,
      startMinute: hhmmToMin(r.start),
      endMinute: hhmmToMin(r.end),
      enabled: 'Y'
    }))
  saveSchedule(scheduleWorkerId.value, rows).then(() => {
    proxy.$modal.msgSuccess('排班已保存')
    scheduleOpen.value = false
  })
}

function hhmmToMin(v) {
  if (!v) return 0
  const [h, m] = v.split(':').map(Number)
  return h * 60 + m
}

// ---------------- 派单 / 改派 ----------------
const dispatchOpen = ref(false)
const dispatchTab = ref('assign')
const dispatchWorkerId = ref(null)
const dispatchWorkerName = ref('')
const dispatchWorkerSite = ref(null)
const poolList = ref([])
const poolLoading = ref(false)
const myTaskList = ref([])
const taskLoading = ref(false)
const reassignOpen = ref(false)
const reassignOrderNo = ref(null)
const candidateList = ref([])
const candLoading = ref(false)

function openDispatch(row) {
  dispatchWorkerId.value = row.workerId
  dispatchWorkerName.value = row.name
  dispatchWorkerSite.value = row.siteId
  dispatchOpen.value = true
  dispatchTab.value = 'assign'
  loadPool()
  loadTasks()
}

function loadTasks() {
  taskLoading.value = true
  getWorkerTasks(dispatchWorkerId.value).then(res => {
    myTaskList.value = res.data || []
    taskLoading.value = false
  }).catch(() => { taskLoading.value = false })
}

function loadPool() {
  poolLoading.value = true
  getDispatchPool(dispatchWorkerSite.value).then(res => {
    poolList.value = res.data || []
    poolLoading.value = false
  }).catch(() => { poolLoading.value = false })
}

function assign(orderNo) {
  dispatchOrder({ orderNo, workerId: dispatchWorkerId.value }).then(() => {
    proxy.$modal.msgSuccess('已派单')
    loadPool()
  })
}

function openReassign(row) {
  reassignOrderNo.value = row.orderNo
  reassignOpen.value = true
  candLoading.value = true
  dispatchCandidates(dispatchWorkerSite.value, null).then(res => {
    candidateList.value = (res.data || []).filter(c => c.value !== dispatchWorkerId.value)
    candLoading.value = false
  }).catch(() => { candLoading.value = false })
}

function doReassign(newWorkerId) {
  dispatchOrder({ orderNo: reassignOrderNo.value, workerId: newWorkerId }).then(() => {
    proxy.$modal.msgSuccess('已改派')
    reassignOpen.value = false
    dispatchOpen.value = false
  })
}

// ---------------- 资质审核 ----------------
const auditList = ref([])
const auditLoading = ref(false)
const auditTotal = ref(0)
const auditQuery = reactive({ reviewStatus: 'PENDING', pageNum: 1, pageSize: 10 })
const qualOpen = ref(false)
const currentQual = ref({})

function faceText(s) {
  return s === 'PASSED' ? '已通过' : s === 'FAILED' ? '未通过' : '待核身'
}
function reviewText(s) {
  return s === 'PASSED' ? '已通过' : s === 'REJECTED' ? '已驳回' : '待审核'
}

function getAuditList() {
  auditLoading.value = true
  qualificationPage(auditQuery).then(res => {
    auditList.value = res.rows
    auditTotal.value = res.total
    auditLoading.value = false
  })
}

function qualImg(fileId) {
  return `${import.meta.env.VITE_APP_BASE_API || ''}/api/v1/media/${fileId}/raw`
}

function viewQual(row) {
  currentQual.value = { ...row }
  qualOpen.value = true
}

function review(workerId, reviewStatus, reviewRemark) {
  reviewQualification({ workerId, reviewStatus, reviewRemark }).then(() => {
    proxy.$modal.msgSuccess('已审核')
    qualOpen.value = false
    getAuditList()
  })
}

function pass(row) {
  proxy.$modal.confirm(`确认通过师傅「${row.idName}」的资质？`).then(() => review(row.workerId, 'PASSED'))
}

function reject(workerId) {
  proxy.$modal.prompt('请填写驳回原因').then(({ value }) => {
    if (!value) {
      proxy.$modal.msgError('驳回必须填写原因')
      return
    }
    review(workerId, 'REJECTED', value)
  }).catch(() => {})
}

function facePass(workerId) {
  updateFace({ workerId, faceStatus: 'PASSED', faceProvider: 'MANUAL', faceRef: 'MANUAL-' + workerId }).then(() => {
    proxy.$modal.msgSuccess('已登记人脸通过')
    qualOpen.value = false
    getAuditList()
  })
}

loadOptions()
getList()
getAuditList()
</script>

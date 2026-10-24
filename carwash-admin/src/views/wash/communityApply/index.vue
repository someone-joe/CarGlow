<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="78px">
      <el-form-item label="小区名" prop="communityName">
        <el-input v-model="queryParams.communityName" placeholder="申请的小区名" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />

    <el-table v-loading="loading" :data="applyList">
      <el-table-column label="申请ID" prop="applyId" width="90" fixed="left" />
      <el-table-column label="小区名" prop="communityName" min-width="160" show-overflow-tooltip fixed="left" />
      <el-table-column label="联系电话" prop="contactPhone" width="130" />
      <el-table-column label="会员ID" prop="memberId" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="驳回原因" prop="rejectReason" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.rejectReason || '-' }}</template>
      </el-table-column>
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Finished" @click="handleAudit(row)" v-hasPermi="['wash:community-apply:approve', 'wash:community-apply:reject']">
            {{ row.status === 'PENDING' ? '审核' : '查看' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 审核 / 详情 -->
    <el-dialog :title="dialogTitle" v-model="open" width="620px" append-to-body>
      <el-descriptions :column="2" border class="mb10">
        <el-descriptions-item label="申请ID">{{ current.applyId }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ current.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="小区名">{{ current.communityName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ current.memberId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="statusType(current.status)">{{ statusLabel(current.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(current.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="current.rejectReason" label="驳回原因" :span="2">{{ current.rejectReason }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="current.status === 'PENDING'">
        <el-form ref="auditRef" :model="auditForm" :rules="auditRules" label-width="90px">
          <el-form-item label="审核结果" prop="pass">
            <el-switch v-model="auditForm.pass" active-text="通过" inactive-text="驳回" />
          </el-form-item>
          <template v-if="auditForm.pass">
            <el-form-item label="所属站点" prop="siteId">
              <el-select v-model="auditForm.siteId" placeholder="选择该小区归属的中央站" clearable style="width: 100%">
                <el-option v-for="s in siteOpts" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="小区名称" prop="communityName">
              <el-input v-model="auditForm.communityName" maxlength="50" show-word-limit placeholder="通过后据此新建小区" />
            </el-form-item>
          </template>
          <el-form-item v-else label="驳回原因" prop="reason">
            <el-input v-model="auditForm.reason" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="必须写明原因，客户要看到为什么没通过" />
          </el-form-item>
        </el-form>
        <el-alert v-if="auditForm.pass" type="info" :closable="false" show-icon>
          <template #title>通过后将在所选站点下新建小区，会员即可在该小区下单。</template>
        </el-alert>
      </template>
      <el-alert v-else type="info" :closable="false" show-icon>
        <template #title>该申请已审核，不能重复操作。</template>
      </el-alert>

      <template #footer>
        <el-button v-if="current.status === 'PENDING'" type="primary" @click="submitAudit">确 定</el-button>
        <el-button @click="open = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WashCommunityApply">
import { listCommunityApply, approveCommunityApply, rejectCommunityApply } from '@/api/wash/communityApply'
import { siteOptions } from '@/api/wash/site'

const { proxy } = getCurrentInstance()

const applyList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const dialogTitle = ref('')
const siteOpts = ref([])

const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }
]

const data = reactive({
  current: {},
  queryParams: { pageNum: 1, pageSize: 10, communityName: undefined, status: undefined },
  auditForm: { pass: true, siteId: undefined, communityName: undefined, reason: undefined },
  auditRules: {
    siteId: [{ required: true, message: '请选择小区所属站点', trigger: 'change' }],
    communityName: [{ required: true, message: '请输入小区名称', trigger: 'blur' }],
    reason: [{
      validator: (rule, value, callback) => {
        if (!data.auditForm.pass && (!value || !value.trim())) {
          callback(new Error('驳回必须填写原因，客户要看到为什么没通过'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }]
  }
})
const { current, queryParams, auditForm, auditRules } = toRefs(data)

function statusLabel(s) {
  const hit = statusOptions.find((x) => x.value === s)
  return hit ? hit.label : s || '-'
}

function statusType(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'info' }[s] || 'info'
}

function formatTime(ms) {
  if (!ms) return '-'
  return proxy.parseTime(ms, '{y}-{m}-{d} {h}:{i}')
}

function getList() {
  loading.value = true
  listCommunityApply(queryParams.value).then((res) => {
    applyList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAudit(row) {
  current.value = row
  auditForm.value = { pass: true, siteId: undefined, communityName: row.communityName, reason: undefined }
  dialogTitle.value = row.status === 'PENDING' ? '审核小区开通申请' : '小区开通申请详情'
  open.value = true
}

function submitAudit() {
  proxy.$refs.auditRef.validate((valid) => {
    if (!valid) return
    const id = current.value.applyId
    if (auditForm.value.pass) {
      approveCommunityApply(id, { siteId: auditForm.value.siteId, communityName: auditForm.value.communityName }).then(() => {
        proxy.$modal.msgSuccess('已通过，小区已创建')
        open.value = false
        getList()
      })
    } else {
      rejectCommunityApply(id, { reason: auditForm.value.reason }).then(() => {
        proxy.$modal.msgSuccess('已驳回')
        open.value = false
        getList()
      })
    }
  })
}

// 站点下拉：通过审核时选归属中央站
siteOptions().then((res) => { siteOpts.value = res.data || [] })

getList()
</script>

<style scoped>
.mb10 {
  margin-bottom: 10px;
}
</style>

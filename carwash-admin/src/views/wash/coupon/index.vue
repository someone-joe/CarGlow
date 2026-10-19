<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="券名称模糊查询" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="上架" value="Y" />
          <el-option label="下架" value="N" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wash:coupon:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Present" @click="handleBatchGrant" v-hasPermi="['wash:coupon:grant']">批量发放</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="templateList">
      <el-table-column label="ID" prop="couponTemplateId" width="70" />
      <el-table-column label="名称" prop="name" min-width="160" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'NEWBIE' ? 'warning' : row.type === 'INVITE' ? 'success' : ''">
            {{ typeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="门槛" width="100">
        <template #default="{ row }">
          <span v-if="!row.thresholdAmount">无门槛</span>
          <span v-else>满 {{ toYuan(row.thresholdAmount) }} 元</span>
        </template>
      </el-table-column>
      <el-table-column label="减免" width="90">
        <template #default="{ row }">{{ toYuan(row.discountAmount) }} 元</template>
      </el-table-column>
      <el-table-column label="总量" prop="total" width="70" />
      <el-table-column label="已领" prop="issued" width="70" />
      <el-table-column label="剩余" width="70">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.remain === 0 }">{{ row.remain }}</span>
        </template>
      </el-table-column>
      <el-table-column label="每人限领" prop="perLimit" width="90" />
      <el-table-column label="生效区间" min-width="230">
        <template #default="{ row }">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'Y' ? 'success' : 'info'" size="small">
            {{ row.status === 'Y' ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['wash:coupon:edit']">修改</el-button>
          <el-button link type="primary" icon="Switch" @click="handleToggleStatus(row)" v-hasPermi="['wash:coupon:edit']">
            {{ row.status === 'Y' ? '下架' : '上架' }}
          </el-button>
          <el-button link type="primary" icon="Present" @click="handleGrant(row)" v-hasPermi="['wash:coupon:grant']">发放</el-button>
          <el-button link type="primary" icon="Tickets" @click="handleRecords(row)">记录</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['wash:coupon:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 修改 -->
    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="templateRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" placeholder="如：新客立减10元" maxlength="64" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <div class="hint">新客券 / 邀请奖励券由业务规则自动发放，普通券才进领券中心</div>
        </el-form-item>
        <el-form-item label="使用门槛" prop="thresholdYuan">
          <el-input-number v-model="form.thresholdYuan" :min="0" :precision="2" :step="1" style="width: 100%" />
          <div class="hint">0 = 无门槛；减免不能大于门槛，否则平台倒贴</div>
        </el-form-item>
        <el-form-item label="减免金额" prop="discountYuan">
          <el-input-number v-model="form.discountYuan" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="!form.couponTemplateId" label="发行总量" prop="total">
          <el-input-number v-model="form.total" :min="1" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item v-else label="发行总量">
          <el-input-number :model-value="form.total" :disabled="true" style="width: 100%" />
          <div class="hint text-danger">已发行的模板不能改总量（会与真实发出的券对不上），要加量请发新模板</div>
        </el-form-item>
        <el-form-item label="已领/剩余">
          <span v-if="form.couponTemplateId">{{ form.issued ?? 0 }} / {{ form.remain ?? 0 }}</span>
          <span v-else class="hint">新增时自动为 0</span>
        </el-form-item>
        <el-form-item label="每人限领" prop="perLimit">
          <el-input-number v-model="form.perLimit" :min="1" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效区间" prop="timeRange">
          <el-date-picker v-model="form.timeRange" type="datetimerange" range-separator="~" start-placeholder="开始" end-placeholder="结束" value-format="x" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上架状态" prop="status">
          <el-switch v-model="form.status" active-value="Y" inactive-value="N" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 发放 -->
    <el-dialog title="发放优惠券" v-model="grantOpen" width="480px" append-to-body>
      <el-alert type="info" :closable="false" show-icon class="mb10">
        <template #title>
          发放同样受「每人限领」与库存约束，发不完会明确报错，不会超发。
        </template>
      </el-alert>
      <el-form ref="grantRef" :model="grantForm" :rules="grantRules" label-width="90px">
        <el-form-item label="券模板">
          <span>{{ grantForm.templateName }}（#{{ grantForm.couponTemplateId }}）</span>
        </el-form-item>
        <el-form-item label="会员ID" prop="memberId">
          <el-input v-model="grantForm.memberId" placeholder="会员ID，可在「会员管理」页查" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitGrant">发 放</el-button>
        <el-button @click="grantOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 领用 / 核销记录 -->
    <el-dialog title="领用 / 核销记录" v-model="recordOpen" width="900px" append-to-body>
      <el-form :inline="true" :model="recordQuery" class="mb10">
        <el-form-item label="状态">
          <el-select v-model="recordQuery.status" placeholder="全部" clearable style="width: 140px" @change="getRecords">
            <el-option label="未使用" value="UNUSED" />
            <el-option label="已核销" value="USED" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="recordQuery.phone" placeholder="按明文匹配，结果仍脱敏" clearable style="width: 220px" @keyup.enter="getRecords" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getRecords">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="recordLoading" :data="recordList">
        <el-table-column label="券ID" prop="couponUserId" width="80" />
        <el-table-column label="会员ID" prop="memberId" width="80" />
        <el-table-column label="手机号" prop="memberPhone" width="130">
          <template #default="{ row }">{{ row.memberPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'USED' ? 'success' : row.status === 'UNUSED' ? 'warning' : 'info'">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用订单" prop="orderNo" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="领取时间" width="170">
          <template #default="{ row }">{{ formatTime(row.obtainTime) }}</template>
        </el-table-column>
        <el-table-column label="核销时间" width="170">
          <template #default="{ row }">{{ formatTime(row.usedTime) }}</template>
        </el-table-column>
        <el-table-column label="过期时间" width="170">
          <template #default="{ row }">{{ formatTime(row.expireTime) }}</template>
        </el-table-column>
      </el-table>
      <pagination v-show="recordTotal > 0" :total="recordTotal" v-model:page="recordQuery.pageNum" v-model:limit="recordQuery.pageSize" @pagination="getRecords" />

      <el-divider content-position="left">批量发放批次（每条批次折叠为一行，点「查看」看完整发放结果）</el-divider>
      <el-table v-loading="batchLoading" :data="batchList">
        <el-table-column label="批次ID" prop="batchId" width="80" />
        <el-table-column label="券模板" prop="templateName" min-width="160" show-overflow-tooltip />
        <el-table-column label="每人" prop="perCount" width="70" />
        <el-table-column label="受众" min-width="220">
          <template #default="{ row }">
            <span>{{ audienceLabel(row.audienceType) }}</span>
            <template v-if="row.audienceType === 'COMMUNITY'"> · {{ row.communityName || row.communityId }}</template>
            <template v-else-if="row.audienceType === 'TIME'"> · {{ formatTime(row.timeFrom) }} ~ {{ formatTime(row.timeTo) }}</template>
          </template>
        </el-table-column>
        <el-table-column label="操作人" prop="operator" width="110" />
        <el-table-column label="成功" prop="successCount" width="70" />
        <el-table-column label="跳过" prop="skippedCount" width="70" />
        <el-table-column label="失败" prop="failedCount" width="70" />
        <el-table-column label="时间" width="170"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleBatchDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="batchTotal > 0" :total="batchTotal" v-model:page="batchQuery.pageNum" v-model:limit="batchQuery.pageSize" @pagination="getBatches" />
    </el-dialog>

    <!-- 批量发放 -->
    <el-dialog title="批量发放优惠券" v-model="batchGrantOpen" width="560px" append-to-body>
      <el-alert type="info" :closable="false" show-icon class="mb10">
        <template #title>逐人复用「每人限领」与库存约束，发不完会明确失败，不会超发；单人不成功只记结果，不影响他人。</template>
      </el-alert>
      <el-form ref="batchGrantRef" :model="batchGrantForm" :rules="batchGrantRules" label-width="90px">
        <el-form-item label="券模板" prop="templateId">
          <el-select v-model="batchGrantForm.templateId" filterable placeholder="选择要发放的券模板" style="width: 100%">
            <el-option v-for="t in templateOptions" :key="t.couponTemplateId" :label="t.name + '（满' + toYuan(t.thresholdAmount) + '减' + toYuan(t.discountAmount) + '）'" :value="t.couponTemplateId" />
          </el-select>
        </el-form-item>
        <el-form-item label="每人张数" prop="perCount">
          <el-input-number v-model="batchGrantForm.perCount" :min="1" :max="100" style="width: 100%" />
          <div class="hint">默认 1 张，可改；实际发出仍受每人限领与库存约束</div>
        </el-form-item>
        <el-form-item label="受众">
          <el-checkbox v-model="batchGrantForm.all">全量会员</el-checkbox>
        </el-form-item>
        <template v-if="!batchGrantForm.all">
          <el-form-item label="按小区">
            <el-select v-model="batchGrantForm.communityId" filterable clearable placeholder="按常用停车位置 ∪ 车辆常停小区筛选" style="width: 100%">
              <el-option v-for="c in communityOptionsList" :key="c.communityId" :label="c.communityName" :value="c.communityId" />
            </el-select>
          </el-form-item>
          <el-form-item label="注册时间">
            <el-date-picker v-model="batchGrantForm.timeRange" type="datetimerange" range-separator="~" start-placeholder="开始" end-placeholder="结束" value-format="x" style="width: 100%" />
          </el-form-item>
        </template>
      </el-form>
      <el-alert v-if="batchGrantResult" type="success" :closable="false" class="mb10">
        <template #title>
          发放完成：共 {{ batchGrantResult.totalCount }} 人，成功 {{ batchGrantResult.successCount }}、跳过(达限领) {{ batchGrantResult.skippedCount }}、失败 {{ batchGrantResult.failedCount }}。批次 #{{ batchGrantResult.batchId }}
        </template>
      </el-alert>
      <template #footer>
        <el-button type="primary" :disabled="!!batchGrantResult" @click="submitBatchGrant">发 放</el-button>
        <el-button @click="batchGrantOpen = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 批量发放明细 -->
    <el-dialog title="批量发放明细" v-model="batchDetailOpen" width="920px" append-to-body>
      <el-descriptions :column="3" border size="small" class="mb10" v-if="batchInfo">
        <el-descriptions-item label="批次ID">#{{ batchInfo.batchId }}</el-descriptions-item>
        <el-descriptions-item label="券模板">{{ batchInfo.templateName }}</el-descriptions-item>
        <el-descriptions-item label="每人张数">{{ batchInfo.perCount }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ batchInfo.operator }}</el-descriptions-item>
        <el-descriptions-item label="受众">
          {{ audienceLabel(batchInfo.audienceType) }}<template v-if="batchInfo.audienceType === 'COMMUNITY'"> · {{ batchInfo.communityName || batchInfo.communityId }}</template>
        </el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(batchInfo.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ batchInfo.successCount }}</el-descriptions-item>
        <el-descriptions-item label="跳过">{{ batchInfo.skippedCount }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ batchInfo.failedCount }}</el-descriptions-item>
      </el-descriptions>
      <el-form :inline="true" :model="batchDetailQuery" class="mb10">
        <el-form-item label="结果">
          <el-select v-model="batchDetailQuery.result" placeholder="全部" clearable style="width: 140px" @change="getBatchDetail()">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="跳过" value="SKIPPED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getBatchDetail()">搜索</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :disabled="batchInfo && batchInfo.failedCount === 0 && batchInfo.skippedCount === 0" @click="regrantAll">补发全部失败/跳过</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="batchDetailLoading" :data="batchDetailList">
        <el-table-column label="会员ID" prop="memberId" width="90" />
        <el-table-column label="手机号" prop="memberPhone" width="130"><template #default="{ row }">{{ row.memberPhone || '-' }}</template></el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : row.result === 'SKIPPED' ? 'warning' : 'danger'">{{ resultLabel(row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实际发出" prop="grantedCount" width="90" />
        <el-table-column label="原因" prop="reason" min-width="200" show-overflow-tooltip><template #default="{ row }">{{ row.reason || '-' }}</template></el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.result !== 'SUCCESS'" link type="primary" @click="regrantOne(row)">补发</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="batchDetailTotal > 0" :total="batchDetailTotal" v-model:page="batchDetailQuery.pageNum" v-model:limit="batchDetailQuery.pageSize" @pagination="getBatchDetail()" />
    </el-dialog>
  </div>
</template>

<script setup name="WashCoupon">
import { listCouponTemplate, addCouponTemplate, updateCouponTemplate, changeCouponStatus, delCouponTemplate, grantCoupon, listCouponRecord, batchGrantCoupon, listCouponBatch, getCouponBatch, regrantCouponBatch } from '@/api/wash/coupon'
import { listCommunity } from '@/api/wash/community'

const { proxy } = getCurrentInstance()

const templateList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const typeOptions = [
  { value: 'NORMAL', label: '普通券（进领券中心）' },
  { value: 'NEWBIE', label: '新客券' },
  { value: 'INVITE', label: '邀请奖励券' }
]

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, name: undefined, type: undefined, status: undefined },
  rules: {
    name: [{ required: true, message: '券名称不能为空', trigger: 'blur' }],
    type: [{ required: true, message: '请选择类型', trigger: 'change' }],
    perLimit: [{ required: true, message: '每人限领必填', trigger: 'blur' }],
    timeRange: [{ required: true, message: '请选择生效区间', trigger: 'change' }]
  }
})
const { form, queryParams, rules } = toRefs(data)

// ---------------- 发放 ----------------
const grantOpen = ref(false)
const grantForm = ref({})
const grantRules = {
  memberId: [{ required: true, message: '会员ID不能为空', trigger: 'blur' }]
}

// ---------------- 记录 ----------------
const recordOpen = ref(false)
const recordLoading = ref(false)
const recordList = ref([])
const recordTotal = ref(0)
const recordQuery = ref({ pageNum: 1, pageSize: 10, templateId: undefined, status: undefined, phone: undefined })

/** 分 → 元（展示用） */
function toYuan(fen) {
  if (fen === null || fen === undefined) return '-'
  return (fen / 100).toFixed(2)
}

/** 元 → 分（提交用）。用 Math.round 规避 0.07*100=7.000000000000001 这类浮点误差 */
function toFen(yuan) {
  if (yuan === null || yuan === undefined || yuan === '') return null
  return Math.round(Number(yuan) * 100)
}

function typeLabel(t) {
  const hit = typeOptions.find((x) => x.value === t)
  return hit ? hit.label.split('（')[0] : t || '-'
}

function statusLabel(s) {
  return { UNUSED: '未使用', USED: '已核销', EXPIRED: '已过期' }[s] || s || '-'
}

function formatTime(ms) {
  if (!ms) return '-'
  return proxy.parseTime(ms, '{y}-{m}-{d} {h}:{i}')
}

// ---------------- 批量发放 ----------------
const batchGrantOpen = ref(false)
const batchGrantForm = ref({ templateId: undefined, perCount: 1, all: true, communityId: undefined, timeRange: undefined })
const batchGrantRules = {
  templateId: [{ required: true, message: '请选择券模板', trigger: 'change' }]
}
const templateOptions = ref([])
const communityOptionsList = ref([])
const batchGrantResult = ref(null)

// 记录弹窗内的批量批次列表
const batchList = ref([])
const batchTotal = ref(0)
const batchLoading = ref(false)
const batchQuery = ref({ pageNum: 1, pageSize: 10, templateId: undefined })

// 批量明细弹窗
const batchDetailOpen = ref(false)
const batchInfo = ref(null)
const batchDetailList = ref([])
const batchDetailTotal = ref(0)
const batchDetailLoading = ref(false)
const batchDetailQuery = ref({ pageNum: 1, pageSize: 10, result: undefined })
let currentBatchId = null

function audienceLabel(t) {
  return { ALL: '全量会员', COMMUNITY: '按小区', TIME: '按注册时间' }[t] || t || '-'
}
function resultLabel(r) {
  return { SUCCESS: '成功', SKIPPED: '跳过', FAILED: '失败' }[r] || r || '-'
}

function handleBatchGrant() {
  batchGrantForm.value = { templateId: undefined, perCount: 1, all: true, communityId: undefined, timeRange: undefined }
  batchGrantResult.value = null
  loadTemplateOptions()
  loadCommunities()
  batchGrantOpen.value = true
}

function loadTemplateOptions() {
  listCouponTemplate({ pageNum: 1, pageSize: 100 }).then((res) => {
    templateOptions.value = res.rows || []
  })
}
function loadCommunities() {
  listCommunity({ pageNum: 1, pageSize: 200 }).then((res) => {
    communityOptionsList.value = res.rows || []
  })
}

function submitBatchGrant() {
  proxy.$refs.batchGrantRef.validate((valid) => {
    if (!valid) return
    const f = batchGrantForm.value
    if (!f.all && !f.communityId && !(f.timeRange && f.timeRange.length === 2)) {
      proxy.$modal.msgError('受众需选择：全量 / 小区 / 注册时间区间，至少其一')
      return
    }
    const payload = {
      templateId: f.templateId,
      perCount: f.perCount,
      audience: {
        all: f.all,
        communityId: f.all ? null : f.communityId,
        timeFrom: f.all || !f.timeRange ? null : f.timeRange[0],
        timeTo: f.all || !f.timeRange ? null : f.timeRange[1]
      }
    }
    batchGrantCoupon(payload).then((res) => {
      batchGrantResult.value = res
      proxy.$modal.msgSuccess('批量发放完成')
    })
  })
}

function getBatches() {
  batchLoading.value = true
  listCouponBatch(batchQuery.value).then((res) => {
    batchList.value = res.rows || []
    batchTotal.value = res.total || 0
    batchLoading.value = false
  }).catch(() => { batchLoading.value = false })
}

function handleBatchDetail(row) {
  currentBatchId = row.batchId
  batchDetailQuery.value = { pageNum: 1, pageSize: 10, result: undefined }
  batchDetailOpen.value = true
  getBatchDetail()
}

function getBatchDetail() {
  if (!currentBatchId) return
  batchDetailLoading.value = true
  getCouponBatch(currentBatchId, batchDetailQuery.value).then((res) => {
    batchInfo.value = res.batch || null
    batchDetailList.value = res.rows || []
    batchDetailTotal.value = res.total || 0
    batchDetailLoading.value = false
  }).catch(() => { batchDetailLoading.value = false })
}

function regrantOne(row) {
  proxy.$modal.confirm(`确认给会员 ${row.memberId} 重新发放？`).then(() => {
    return regrantCouponBatch(currentBatchId, { memberIds: [row.memberId] })
  }).then(() => {
    proxy.$modal.msgSuccess('已提交补发')
    getBatchDetail()
    getBatches()
  }).catch(() => {})
}

function regrantAll() {
  proxy.$modal.confirm('确认对全部「失败 / 跳过」会员重新发放？').then(() => {
    return getCouponBatch(currentBatchId, { result: 'FAILED', pageSize: 9999, pageNum: 1 }).then((res) => {
      const failed = res.rows || []
      return getCouponBatch(currentBatchId, { result: 'SKIPPED', pageSize: 9999, pageNum: 1 }).then((res2) => {
        const skipped = res2.rows || []
        const ids = [...failed, ...skipped].map((x) => x.memberId)
        if (!ids.length) { proxy.$modal.msgInfo('没有需要补发的会员'); return }
        return regrantCouponBatch(currentBatchId, { memberIds: ids })
      })
    })
  }).then(() => {
    proxy.$modal.msgSuccess('已提交补发')
    getBatchDetail()
    getBatches()
  }).catch(() => {})
}

function getList() {
  loading.value = true
  listCouponTemplate(queryParams.value).then((res) => {
    templateList.value = res.rows || []
    total.value = res.total || 0
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

function handleAdd() {
  form.value = {
    couponTemplateId: undefined,
    name: undefined,
    type: 'NORMAL',
    thresholdYuan: 0,
    discountYuan: undefined,
    total: undefined,
    perLimit: 1,
    status: 'Y',
    timeRange: undefined
  }
  open.value = true
  title.value = '新增优惠券'
}

function handleUpdate(row) {
  form.value = {
    couponTemplateId: row.couponTemplateId,
    name: row.name,
    type: row.type,
    thresholdYuan: (row.thresholdAmount || 0) / 100,
    discountYuan: (row.discountAmount || 0) / 100,
    total: row.total,
    issued: row.issued,
    remain: row.remain,
    perLimit: row.perLimit,
    status: row.status,
    timeRange: row.startTime && row.endTime ? [row.startTime, row.endTime] : undefined
  }
  open.value = true
  title.value = '修改优惠券'
}

function submitForm() {
  proxy.$refs.templateRef.validate((valid) => {
    if (!valid) return
    const range = form.value.timeRange
    const payload = {
      couponTemplateId: form.value.couponTemplateId,
      name: form.value.name,
      type: form.value.type,
      thresholdAmount: toFen(form.value.thresholdYuan),
      discountAmount: toFen(form.value.discountYuan),
      perLimit: form.value.perLimit,
      startTime: range ? range[0] : null,
      endTime: range ? range[1] : null
    }
    // 编辑接口**刻意不接受 total**（发行过的模板总量就是历史账），只有新增才带
    if (!payload.couponTemplateId) {
      payload.total = form.value.total
      payload.status = form.value.status
    }
    const action = payload.couponTemplateId ? updateCouponTemplate(payload) : addCouponTemplate(payload)
    action.then(() => {
      proxy.$modal.msgSuccess(payload.couponTemplateId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleToggleStatus(row) {
  const next = row.status === 'Y' ? 'N' : 'Y'
  proxy.$modal.confirm(`确认将「${row.name}」${next === 'Y' ? '上架' : '下架'}？`).then(() => {
    return changeCouponStatus(row.couponTemplateId, next)
  }).then(() => {
    proxy.$modal.msgSuccess('已更新')
    getList()
  }).catch(() => {})
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除优惠券「${row.name}」？已发放的券仍保留，不影响历史核销。`).then(() => {
    return delCouponTemplate(row.couponTemplateId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleGrant(row) {
  grantForm.value = { couponTemplateId: row.couponTemplateId, templateName: row.name, memberId: undefined }
  grantOpen.value = true
}

function submitGrant() {
  proxy.$refs.grantRef.validate((valid) => {
    if (!valid) return
    grantCoupon(grantForm.value.couponTemplateId, Number(grantForm.value.memberId)).then(() => {
      proxy.$modal.msgSuccess('发放成功')
      grantOpen.value = false
      getList()
    })
  })
}

function handleRecords(row) {
  recordQuery.value = { pageNum: 1, pageSize: 10, templateId: row.couponTemplateId, status: undefined, phone: undefined }
  batchQuery.value = { pageNum: 1, pageSize: 10, templateId: row.couponTemplateId }
  recordOpen.value = true
  getRecords()
  getBatches()
}

function getRecords() {
  recordLoading.value = true
  listCouponRecord(recordQuery.value).then((res) => {
    recordList.value = res.rows || []
    recordTotal.value = res.total || 0
    recordLoading.value = false
  }).catch(() => {
    recordLoading.value = false
  })
}

getList()
</script>

<style scoped>
.hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
.text-danger {
  color: #f56c6c;
}
.mb10 {
  margin-bottom: 10px;
}
</style>

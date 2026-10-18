<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- ============ 赠送规则 ============ -->
      <el-tab-pane label="充值赠送规则" name="rules">
        <div class="tip-line">
          充 X 送 Y，按顺序展示在小程序充值页。修改后整体覆盖生效（已生成的充值单为金额快照，不受影响）。
        </div>
        <el-table :data="rules" v-loading="rulesLoading" border size="small" style="margin-top: 12px">
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="充值金额（元）" min-width="180">
            <template #default="{ row }">
              <el-input-number v-model="row.rechargeYuan" :min="0" :precision="2" :step="10" :controls="false" />
            </template>
          </el-table-column>
          <el-table-column label="赠送金额（元）" min-width="180">
            <template #default="{ row }">
              <el-input-number v-model="row.giftYuan" :min="0" :precision="2" :step="5" :controls="false" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link icon="Delete" @click="removeRule($index)" />
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 12px">
          <el-button type="primary" plain icon="Plus" @click="addRule">新增一档</el-button>
          <el-button type="primary" @click="saveRules" v-hasPermi="['wash:wallet:edit']" :loading="saving">保存</el-button>
        </div>
      </el-tab-pane>

      <!-- ============ 充值流水 ============ -->
      <el-tab-pane label="充值流水" name="recharges">
        <el-form :inline="true" :model="query" class="query-form">
          <el-form-item label="会员ID">
            <el-input v-model="query.memberId" placeholder="选填" clearable style="width: 160px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
              <el-option label="待支付" value="INIT" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="recharges" v-loading="listLoading" border size="small">
          <el-table-column label="充值单号" prop="rechargeNo" min-width="180" show-overflow-tooltip />
          <el-table-column label="会员ID" prop="memberId" width="90" />
          <el-table-column label="实付（元）" width="120" align="right">
            <template #default="{ row }">{{ yuan(row.payAmount) }}</template>
          </el-table-column>
          <el-table-column label="赠送（元）" width="120" align="right">
            <template #default="{ row }">{{ yuan(row.giftAmount) }}</template>
          </el-table-column>
          <el-table-column label="渠道" prop="channel" width="90" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'INIT' ? 'warning' : 'danger'">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          @pagination="getRecharges"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="WalletConfig">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listGiftRules, listRecharges, saveGiftRules } from '@/api/wash/wallet'

const activeTab = ref('rules')

// ---- 赠送规则 ----
const rulesLoading = ref(false)
const saving = ref(false)
const rules = ref([])

function loadRules() {
  rulesLoading.value = true
  listGiftRules()
    .then((res) => {
      const list = res.data || []
      rules.value = list.map((r) => ({
        rechargeYuan: (r.rechargeFen || 0) / 100,
        giftYuan: (r.giftFen || 0) / 100
      }))
    })
    .finally(() => {
      rulesLoading.value = false
    })
}

function addRule() {
  rules.value.push({ rechargeYuan: undefined, giftYuan: undefined })
}

function removeRule(index) {
  rules.value.splice(index, 1)
}

function saveRules() {
  const rows = []
  for (const r of rules.value) {
    if (!r.rechargeYuan || r.rechargeYuan <= 0) {
      ElMessage.warning('充值金额需大于 0')
      return
    }
    rows.push({
      rechargeFen: Math.round(r.rechargeYuan * 100),
      giftFen: Math.round((r.giftYuan || 0) * 100)
    })
  }
  if (!rows.length) {
    ElMessage.warning('至少保留一档赠送规则')
    return
  }
  saving.value = true
  saveGiftRules(rows)
    .then(() => {
      ElMessage.success('保存成功')
      loadRules()
    })
    .finally(() => {
      saving.value = false
    })
}

// ---- 充值流水 ----
const listLoading = ref(false)
const recharges = ref([])
const total = ref(0)
const query = reactive({
  memberId: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

function getRecharges() {
  listLoading.value = true
  const params = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    memberId: query.memberId || undefined,
    status: query.status || undefined
  }
  listRecharges(params)
    .then((res) => {
      recharges.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      listLoading.value = false
    })
}

function handleQuery() {
  query.pageNum = 1
  getRecharges()
}

function resetQuery() {
  query.memberId = undefined
  query.status = undefined
  query.pageNum = 1
  getRecharges()
}

function yuan(fen) {
  return ((fen || 0) / 100).toFixed(2)
}

function statusLabel(s) {
  return { INIT: '待支付', SUCCESS: '成功', FAILED: '失败' }[s] || s
}

function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped>
.tip-line {
  color: #8a9a98;
  font-size: 13px;
  line-height: 20px;
}

.query-form {
  margin-bottom: 8px;
}
</style>

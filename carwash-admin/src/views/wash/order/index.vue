<template>
  <div class="app-container">
    <el-alert v-if="memberFilter" type="success" :closable="false" show-icon style="margin-bottom: 12px">
      正在查看会员 #{{ memberFilter }} 的订单
      <el-button link type="primary" style="margin-left: 8px" @click="clearMemberFilter">清除筛选</el-button>
    </el-alert>

    <el-form :model="queryParams" :inline="true" label-width="90px">
      <el-form-item label="订单号">
        <el-input v-model="queryParams.orderNo" placeholder="支持模糊查询" clearable style="width: 220px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="订单状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="item in statusList" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="orderList" border>
      <el-table-column label="订单号" prop="orderNo" min-width="200" show-overflow-tooltip />
      <el-table-column label="服务项" prop="serviceName" min-width="180" show-overflow-tooltip />
      <el-table-column label="车牌" prop="plateNo" width="110" />
      <el-table-column label="状态" prop="statusLabel" width="140" />
      <el-table-column label="金额" width="100" align="right">
        <template #default="scope">¥{{ formatAmount(scope.row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="会员ID" prop="memberId" width="100" />
      <el-table-column label="站点ID" prop="siteId" width="100" />
      <el-table-column label="创建时间" prop="createTime" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button v-hasPermi="['wash:order:query']" link type="primary" @click="openDetail(scope.row.orderNo)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 订单详情抽屉：时间轴直接读后端流转日志生成，前端不另算进度 -->
    <el-drawer v-model="drawer" :title="'订单详情 ' + (detail.orderNo || '')" size="45%">
      <el-descriptions :column="1" border v-if="detail.orderNo">
        <el-descriptions-item label="状态">{{ detail.statusLabel }}</el-descriptions-item>
        <el-descriptions-item label="服务项">{{ detail.serviceName }}</el-descriptions-item>
        <el-descriptions-item label="车牌">{{ detail.plateNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实付">¥{{ formatAmount(detail.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="timeline-title">服务进度</div>
      <el-timeline>
        <el-timeline-item v-for="node in detail.timeline || []" :key="node.node"
                          :type="node.current ? 'primary' : (node.reached ? 'success' : 'info')"
                          :hollow="!node.reached">
          {{ node.title }}
          <span class="node-time">{{ node.time ? formatTime(node.time) : '待完成' }}</span>
        </el-timeline-item>
      </el-timeline>

      <div class="timeline-title">过程影像</div>
      <div v-if="!(detail.medias || []).length" class="no-media">暂无影像</div>
      <div v-for="group in mediaGroups" :key="group.type" class="media-group">
        <div class="media-type">{{ group.label }}（{{ group.items.length }}）</div>
        <div class="media-list">
          <el-image
            v-for="m in group.items"
            :key="m.fileId"
            class="media-thumb"
            :src="mediaUrl(m.url)"
            :preview-src-list="group.items.map(i => mediaUrl(i.url))"
            fit="cover"
            preview-teleported
          />
        </div>
      </div>

      <div v-if="hasEditPermi" class="ops">
        <div class="timeline-title">人工干预（会留痕）</div>
        <el-select v-model="advanceForm.event" placeholder="选择要触发的事件" style="width: 240px">
          <el-option v-for="item in eventList" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="advanceForm.reason" placeholder="操作原因（必填）" style="width: 240px; margin-left: 8px" />
        <!-- 是否要二次确认由后端判定（confirmRequired），前端不维护高危事件名单 -->
        <el-checkbox v-if="confirmRequired" v-model="advanceForm.confirm" style="margin-left: 8px">
          确认执行（不可撤销）
        </el-checkbox>
        <el-button type="primary" style="margin-left: 8px" @click="submitAdvance">推进</el-button>
        <el-button type="danger" @click="submitCancel">取消订单</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="Order">
import { listOrder, statusOptions, getOrder, eventOptions, advanceOrder, cancelOrder } from '@/api/wash/order'
import useUserStore from '@/store/modules/user'
import { getToken } from '@/utils/auth'
import { useRoute } from 'vue-router'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const route = useRoute()

const orderList = ref([])
const statusList = ref([])
const loading = ref(true)
const total = ref(0)
const drawer = ref(false)

const detail = ref({})

const MEDIA_TYPE_LABELS = { PARK: '停车照', PICK: '取车照', WASHED: '洗后照', RETURN: '送回照', COMPARE: '对比图', VIDEO: '视频' }

// 影像按业务类型分组；url 是相对路径，el-image 带不了请求头，鉴权走 query token（后端已支持）
const mediaGroups = computed(() => {
  const groups = []
  const byType = {}
  for (const m of (detail.value.medias || [])) {
    const type = m.bizType || 'OTHER'
    if (!byType[type]) {
      byType[type] = { type, label: MEDIA_TYPE_LABELS[type] || type, items: [] }
      groups.push(byType[type])
    }
    byType[type].items.push(m)
  }
  return groups
})

function mediaUrl(url) {
  if (!url) return ''
  const full = url.startsWith('http') ? url : import.meta.env.VUE_APP_BASE_API + url
  const token = getToken()
  return token ? full + (full.includes('?') ? '&' : '?') + 'token=' + token : full
}
const eventList = ref([])
const advanceForm = ref({ event: undefined, reason: undefined, confirm: false })

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  orderNo: undefined,
  status: undefined,
  memberId: undefined
})

// 从会员/车辆页跳过来时带 memberId，客服不用再手输
const memberFilter = computed(() => queryParams.value.memberId)

function clearMemberFilter() {
  queryParams.value.memberId = undefined
  handleQuery()
}

// 按钮级权限：与后台菜单 perms（wash:order:edit）一致
const hasEditPermi = computed(() => userStore.permissions.includes('wash:order:edit'))

function formatAmount(amount) {
  return ((amount ?? 0) / 100).toFixed(2)
}

function formatTime(ms) {
  const d = new Date(ms)
  const pad = (n) => `${n}`.padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getList() {
  loading.value = true
  listOrder(queryParams.value).then(res => {
    orderList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10, orderNo: undefined, status: undefined }
  getList()
}

function openDetail(orderNo) {
  drawer.value = true
  detail.value = {}
  eventList.value = []
  getOrder(orderNo).then(res => {
    detail.value = res.data
  })
  if (hasEditPermi.value) {
    eventOptions(orderNo).then(res => {
      eventList.value = res.data
    })
  }
}

// 当前选中事件是否需要二次确认：取值来自后端 event-options 的 confirmRequired
const confirmRequired = computed(() => {
  const hit = eventList.value.find(item => item.value === advanceForm.value.event)
  return hit && String(hit.confirmRequired) === 'true'
})

// 换事件就重置确认，避免勾了一次后沿用
watch(() => advanceForm.value.event, () => {
  advanceForm.value.confirm = false
})

function submitAdvance() {
  if (!advanceForm.value.event) {
    proxy.$modal.msgWarning('请选择要触发的事件')
    return
  }
  if (!advanceForm.value.reason) {
    proxy.$modal.msgWarning('请填写操作原因')
    return
  }
  if (confirmRequired.value && !advanceForm.value.confirm) {
    proxy.$modal.msgWarning('该操作不可撤销，请勾选「确认执行」')
    return
  }
  advanceOrder(detail.value.orderNo, advanceForm.value.event, advanceForm.value.reason, advanceForm.value.confirm).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    advanceForm.value = { event: undefined, reason: undefined, confirm: false }
    openDetail(detail.value.orderNo)
    getList()
  })
}

function submitCancel() {
  proxy.$modal.prompt('请输入取消原因', '提示', {
    inputPattern: /\S/,
    inputErrorMessage: '取消原因不能为空'
  }).then(({ value }) => {
    cancelOrder(detail.value.orderNo, value).then(res => {
      proxy.$modal.msgSuccess(res.msg)
      openDetail(detail.value.orderNo)
      getList()
    })
  }).catch(() => {})
}

statusOptions().then(res => {
  statusList.value = res.data
})

// 支持从会员页带 memberId 直接进入
if (route.query.memberId) {
  queryParams.value.memberId = Number(route.query.memberId)
}

getList()
</script>

<style scoped>
.timeline-title {
  margin: 20px 0 12px;
  font-weight: 600;
}

.media-group {
  margin-top: 12px;
}

.media-type {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.media-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.media-thumb {
  width: 96px;
  height: 96px;
  border-radius: 4px;
  cursor: pointer;
}

.no-media {
  color: #909399;
  font-size: 13px;
}

.node-time {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.ops {
  margin-top: 20px;
}
</style>

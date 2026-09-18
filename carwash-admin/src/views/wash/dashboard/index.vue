<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card" v-loading="loading">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-hint">{{ card.hint }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="dist-card">
      <template #header>
        <div class="card-header">
          <span>订单状态分布</span>
          <el-button link type="primary" icon="Refresh" @click="getStats">刷新</el-button>
        </div>
      </template>

      <!-- 状态名称与标签全部由后端返回（来源是 OrderStatus 枚举），前端不维护状态名单 -->
      <div v-for="item in statusBreakdown" :key="item.status" class="dist-row">
        <span class="dist-label">{{ item.label }}</span>
        <el-progress class="dist-bar" :percentage="percent(item.count)" :stroke-width="16" :text-inside="true"
                     :format="() => item.count + ' 单'" />
      </div>
      <el-empty v-if="!statusBreakdown.length && !loading" description="暂无订单数据" />
    </el-card>
  </div>
</template>

<script setup name="Dashboard">
import { dashboardStats } from '@/api/wash/dashboard'

const loading = ref(true)
const stats = ref({})

const statusBreakdown = computed(() => stats.value.statusBreakdown || [])
const totalCount = computed(() => statusBreakdown.value.reduce((sum, item) => sum + item.count, 0))

// 四张卡片：口径说明与后端 WashOrderStatsService 保持一致
const cards = computed(() => [
  { key: 'today', label: '今日单量', value: stats.value.todayOrderCount ?? 0, hint: '按下单时间统计' },
  { key: 'washing', label: '在洗数', value: stats.value.washingCount ?? 0, hint: '运输去程 / 清洗 / 待质检' },
  { key: 'waitingKey', label: '待存钥匙', value: stats.value.waitingKeyCount ?? 0, hint: '等客户把钥匙放进柜子' },
  { key: 'abnormal', label: '异常数', value: stats.value.abnormalCount ?? 0, hint: '退款中，需人工跟进' }
])

function percent(count) {
  return totalCount.value > 0 ? Math.round((count / totalCount.value) * 100) : 0
}

function getStats() {
  loading.value = true
  dashboardStats().then(res => {
    stats.value = res.data || {}
  }).finally(() => {
    loading.value = false
  })
}

getStats()
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-value {
  margin-top: 8px;
  font-size: 32px;
  font-weight: 600;
  color: #303133;
}

.stat-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #a8abb2;
}

.dist-card {
  margin-top: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dist-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.dist-label {
  width: 160px;
  font-size: 14px;
  color: #606266;
}

.dist-bar {
  flex: 1;
}
</style>

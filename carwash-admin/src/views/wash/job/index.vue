<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" show-icon class="mb10">
      <template #title>
        手动触发 wash 定时任务（运维 / 补偿用）。任务为幂等安全，可重复执行；
        仅白名单内的任务可触发，结果以下方执行状态为准。
      </template>
    </el-alert>

    <el-row :gutter="16">
      <el-col v-for="j in jobList" :key="j.job" :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="job-card">
          <template #header>
            <div class="job-head">
              <span class="job-name">{{ j.name }}</span>
              <el-tag size="small" type="info">{{ j.job }}</el-tag>
            </div>
          </template>
          <p class="job-desc">{{ j.desc }}</p>
          <div class="job-foot">
            <el-button
              type="primary"
              size="small"
              icon="CaretRight"
              :loading="running[j.job]"
              @click="run(j)"
              v-hasPermi="['wash:job:trigger']"
            >立即执行</el-button>
            <span v-if="results[j.job]" :class="['job-result', results[j.job].ok ? 'ok' : 'fail']">
              {{ results[j.job].ok ? '✓ 已触发' : '✗ ' + results[j.job].msg }}
              <em>{{ formatTime(results[j.job].time) }}</em>
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="WashJob">
import { triggerJob } from '@/api/wash/job'

const { proxy } = getCurrentInstance()

// 白名单与后端 JobTriggerService 一致；描述供运维理解副作用，不参与逻辑
const jobList = [
  { job: 'pay-timeout', name: '支付超时取消', desc: '取消超时未支付的订单，释放预占格口与产能（与下单支付倒计时同生命周期）。' },
  { job: 'capacity-init', name: '每日产能初始化', desc: '为未来 N 天初始化各站点每日产能上限（SETNX，不覆盖已扣减的当日产能，避免凭空放量）。' },
  { job: 'refund-retry', name: '退款重试', desc: '重试失败/待重试的退款单（有重试上限），成功以退款单 status=SUCCESS 为准。' },
  { job: 'order-postpone', name: '订单顺延', desc: '顺延超时的钥匙存取 / 洗护订单到下一可用时段。' },
  { job: 'order-auto-finish', name: 'T+3 自动完成', desc: '将超时未评价的 WAIT_REVIEW 订单自动置为 FINISHED（规则表 2.1 / 规则 5）。' }
]

const running = ref({})
const results = ref({})

function formatTime(ms) {
  if (!ms) return ''
  return proxy.parseTime(ms, '{h}:{i}:{s}')
}

function run(j) {
  running.value[j.job] = true
  triggerJob({ job: j.job, params: {} }).then(() => {
    results.value[j.job] = { ok: true, msg: '已触发', time: Date.now() }
    proxy.$modal.msgSuccess(`已触发：${j.name}`)
  }).catch((e) => {
    results.value[j.job] = { ok: false, msg: (e && e.msg) || '执行失败', time: Date.now() }
  }).finally(() => {
    running.value[j.job] = false
  })
}
</script>

<style scoped>
.mb10 {
  margin-bottom: 10px;
}
.job-card {
  margin-bottom: 16px;
}
.job-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.job-name {
  font-weight: 600;
}
.job-desc {
  min-height: 60px;
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}
.job-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.job-result {
  font-size: 12px;
}
.job-result.ok {
  color: var(--el-color-success);
}
.job-result.fail {
  color: var(--el-color-danger);
}
.job-result em {
  margin-left: 6px;
  font-style: normal;
  opacity: 0.7;
}
</style>

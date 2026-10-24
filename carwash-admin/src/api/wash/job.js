import request from '@/utils/request'

// 后台运维工具：手动触发 wash 定时任务。接口前缀 /admin-api。
// 契约：openapi.yaml /admin-api/wash/job/trigger（tag admin-job）。
// 仅白名单内的 job 可触发（后端 JobTriggerService 维护），避免任意 bean 被反射调用造成越权执行。
// 可选 job：pay-timeout / capacity-init / refund-retry / order-postpone / order-auto-finish。

export function triggerJob(data) {
  return request({
    url: '/admin-api/wash/job/trigger',
    method: 'post',
    data
  })
}

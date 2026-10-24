import request from '@/utils/request'

// 后台小区开通申请审核。接口前缀 /admin-api。
// 契约：openapi.yaml /admin-api/wash/community-apply/**（tag admin-community-apply）。

export function listCommunityApply(query) {
  return request({
    url: '/admin-api/wash/community-apply/list',
    method: 'get',
    params: query
  })
}

export function getCommunityApply(applyId) {
  return request({
    url: '/admin-api/wash/community-apply/' + applyId,
    method: 'get'
  })
}

// 通过：需 siteId（归到哪个中央站）+ communityName（据此新建小区），返回新 communityId
export function approveCommunityApply(applyId, data) {
  return request({
    url: '/admin-api/wash/community-apply/' + applyId + '/approve',
    method: 'post',
    data
  })
}

// 驳回：需 reason（客户要看到为什么没通过）
export function rejectCommunityApply(applyId, data) {
  return request({
    url: '/admin-api/wash/community-apply/' + applyId + '/reject',
    method: 'post',
    data
  })
}

import request from '@/utils/request'

// 后台优惠券管理。接口前缀 /admin-api，与小程序端 /api/v1 分开。
// 金额口径：**后端一律存「分」**，表单里填「元」，换算在页面层做（与「服务项管理」一致）。

export function listCouponTemplate(query) {
  return request({
    url: '/admin-api/wash/coupon/template/list',
    method: 'get',
    params: query
  })
}

export function getCouponTemplate(templateId) {
  return request({
    url: '/admin-api/wash/coupon/template/' + templateId,
    method: 'get'
  })
}

export function addCouponTemplate(data) {
  return request({
    url: '/admin-api/wash/coupon/template',
    method: 'post',
    data
  })
}

export function updateCouponTemplate(data) {
  return request({
    url: '/admin-api/wash/coupon/template',
    method: 'put',
    data
  })
}

// 上下架单独一个接口：不跟改资料混在一起，免得改个名字顺手把券下架了
export function changeCouponStatus(templateId, status) {
  return request({
    url: '/admin-api/wash/coupon/template/' + templateId + '/status',
    method: 'post',
    data: { status }
  })
}

export function delCouponTemplate(templateId) {
  return request({
    url: '/admin-api/wash/coupon/template/' + templateId,
    method: 'delete'
  })
}

// 运营手工发放（补偿 / 线下活动）。每人限领同样生效，库存乐观扣减，不会超发
export function grantCoupon(templateId, memberId) {
  return request({
    url: '/admin-api/wash/coupon/grant',
    method: 'post',
    data: { templateId, memberId }
  })
}

// 领用 / 核销记录。会员手机号后端已脱敏，前端不要自己再处理
export function listCouponRecord(query) {
  return request({
    url: '/admin-api/wash/coupon/record/list',
    method: 'get',
    params: query
  })
}

/**
 * API-K 优惠券与保险（10 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》K 段；PROGRESS 21/22。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, typeIs } from '../lib/assert.js';
import { getSession, receiveAnyCoupon, cleanupCreated } from '../lib/factory.js';
import { SIDECAR_ENABLED, sql } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

test('API-K01 领券中心（库存与限领口径）', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/coupons/center', { token: s.token });
  ok(res, 'K01');
  typeIs(res.data, 'array', 'K01 列表类型');
  for (const t of res.data) {
    // 剩余库存 = 总量 - 已领（口径：后端算好下发，前端不自减）
    typeIs(t.remain, 'number', `K01 ${t.name} remain`);
    typeIs(t.perLimit, 'number', `K01 ${t.name} perLimit`);
    assert.ok(t.remain >= 0, `K01: ${t.name} remain 不应为负`);
  }
});

test('API-K02 领券-正向（领取后名下可见）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  // 找一张没领过的模板（claimed=0 且 remain>0），保证可领
  const center = (await api('GET', '/api/v1/coupons/center', { token: s.token })).data;
  const tpl = center.find((t) => t.remain > 0 && t.claimed === 0 && t.thresholdAmount <= 3900);
  if (!tpl) t.skip('没有未领过的可用模板');
  const res = await api('POST', `/api/v1/coupons/templates/${tpl.templateId}/receive`, { token: s.token });
  ok(res, 'K02 领取');
  // 副作用：名下出现 UNUSED 券
  const mine = (await api('GET', '/api/v1/coupons', { token: s.token })).data;
  assert.ok(mine.some((c) => c.templateId === tpl.templateId && c.status === 'UNUSED'),
    'K02: 领取后名下应有 UNUSED 券');
});

test('API-K03 领券-已领完/下架（C1001）', { todo: '需要「库存=0 或已下架」的模板；当前 seed 模板均有库存。造下架模板属后台写操作，留待 O 批（后台券管理）配合覆盖' }, () => {});

test('API-K04 领券-超限领（C1002）', async (t) => {
  const s = await getSession();
  const center = (await api('GET', '/api/v1/coupons/center', { token: s.token })).data;
  // 找 perLimit=1 且未领过的模板；领一次成功、第二次超限
  const tpl = center.find((t) => t.perLimit === 1 && t.claimed === 0 && t.remain > 0);
  if (!tpl) t.skip('没有 perLimit=1 且未领的模板');
  ok(await api('POST', `/api/v1/coupons/templates/${tpl.templateId}/receive`, { token: s.token }), 'K04 首次领取');
  bizCode(await api('POST', `/api/v1/coupons/templates/${tpl.templateId}/receive`, { token: s.token }), 31002, 'K04 超限领');
});

test('API-K05 我的券-EXPIRED 为计算值', { todo: '需要已过有效期的 UNUSED 券（库里存 UNUSED、展示 EXPIRED）；当前 seed 无过期券，无法构造' }, () => {});

test('API-K06 保险产品列表', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/insurances/products', { token: s.token });
  ok(res, 'K06');
  typeIs(res.data, 'array', 'K06 列表类型');
  if (res.data.length) {
    typeIs(res.data[0].priceAmount, 'number', 'K06 保费（分）');
  }
});

test('API-K07 投保-正向（本人车辆 → 保单 ACTIVE）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const res = await api('POST', '/api/v1/insurances', {
    token: s.token, body: { productId: 1, vehicleId: s.vehicleId },
  });
  ok(res, 'K07 投保');
  // 副作用：保单 ACTIVE、实付保费 990 分（表：wash_insurance_policy，PK policy_id，金额列 paid_amount）
  const policy = sql(`select status, paid_amount from wash_insurance_policy order by policy_id desc limit 1`).trim();
  assert.ok(policy.startsWith('ACTIVE'), `K07: 保单应 ACTIVE，实际 ${policy}`);
  assert.ok(policy.includes('990'), `K07: 保费应 990 分，实际 ${policy}`);
});

test('API-K08 投保-他人车辆', { todo: 'dev 单会员，无法构造他人车辆；待真实双账号后补' }, () => {});

test('API-K09 投保-模板不存在（C2001）', async () => {
  const s = await getSession();
  bizCode(await api('POST', '/api/v1/insurances', {
    token: s.token, body: { productId: 999999, vehicleId: s.vehicleId },
  }), 32001, 'K09');
});

test('API-K10 投保-车辆必填（C2002）', async () => {
  const s = await getSession();
  const res = await api('POST', '/api/v1/insurances', { token: s.token, body: { productId: 1 } });
  assert.notEqual(res.code, 0, `K10: 缺 vehicleId 应拒，实际 code=${res.code} msg=${res.msg}`);
  console.log(`  [K10] 实际 code=${res.code} msg=${res.msg}`);
});

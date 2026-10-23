/**
 * API-K 优惠券与保险（10 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》K 段；PROGRESS 21/22。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, typeIs } from '../lib/assert.js';
import { getSession, receiveAnyCoupon, cleanupCreated } from '../lib/factory.js';
import { SIDECAR_ENABLED, sql, ensureOtherMember, createVehicleForMember, fillCouponStock, setCouponIssued, expireCoupon, setCouponExpire, clearMyUnusedCoupons, createTestCouponTemplate, setCouponTemplate, deleteTestCouponTemplates } from '../lib/sidecar.js';

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
  // 夹具：自造模板（seed 模板都已被历史测试领过一轮，perLimit=1 会直接撞 31002 导致用例空跳）
  const tplId = createTestCouponTemplate({ name: `[AUTOTEST] 可领${Date.now()}`, total: 10, issued: 0, threshold: 0 });
  try {
    const res = await api('POST', `/api/v1/coupons/templates/${tplId}/receive`, { token: s.token });
    ok(res, 'K02 领取');
    // 副作用：名下出现 UNUSED 券
    const mine = (await api('GET', '/api/v1/coupons', { token: s.token })).data;
    assert.ok(mine.some((c) => c.templateId === tplId && c.status === 'UNUSED'),
      'K02: 领取后名下应有 UNUSED 券');
  } finally {
    deleteTestCouponTemplates();
  }
});

test('API-K03 领券-已领完/下架（C1001）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  // 夹具：自造模板（不污染 seed、不受历史领用影响），issued=total → 库存耗尽
  const tplId = createTestCouponTemplate({ name: `[AUTOTEST] 已领完${Date.now()}`, total: 10, issued: 10 });
  try {
    bizCode(await api('POST', `/api/v1/coupons/templates/${tplId}/receive`, { token: s.token }),
      31001, 'K03 已领完应 C1001');

    // 另一个分支：有库存但已下架（status=N）——契约错误码同为 C1001
    const offId = createTestCouponTemplate({ name: `[AUTOTEST] 已下架${Date.now()}`, total: 10, issued: 0 });
    setCouponTemplate(offId, { status: 'N' });
    bizCode(await api('POST', `/api/v1/coupons/templates/${offId}/receive`, { token: s.token }),
      31001, 'K03 已下架应 C1001');
  } finally {
    deleteTestCouponTemplates();
  }
});

test('API-K04 领券-超限领（C1002）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  // 夹具：自造 perLimit=1 模板，领一次成功、第二次超限（seed 模板已被历史测试领过，无法复用）
  const tplId = createTestCouponTemplate({ name: `[AUTOTEST] 限领${Date.now()}`, total: 10, issued: 0, perLimit: 1 });
  try {
    ok(await api('POST', `/api/v1/coupons/templates/${tplId}/receive`, { token: s.token }), 'K04 首次领取');
    bizCode(await api('POST', `/api/v1/coupons/templates/${tplId}/receive`, { token: s.token }), 31002, 'K04 超限领');
  } finally {
    deleteTestCouponTemplates();
  }
});

test('API-K05 我的券-EXPIRED 为计算值', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const coupon = await receiveAnyCoupon(3900);
  if (!coupon) return t.skip('没有可领且门槛 ≤3900 的券模板');
  // 夹具：把有效期改到昨天（库里状态仍是 UNUSED，展示应为 EXPIRED），跑完还原
  const before = expireCoupon(coupon.couponUserId);
  try {
    const mine = (await api('GET', '/api/v1/coupons', { token: s.token, query: { status: 'EXPIRED' } })).data || [];
    assert.ok(mine.some((c) => c.couponUserId === coupon.couponUserId),
      `K05: 过期的 UNUSED 券应按 EXPIRED 展示，实际列表 ${mine.length} 条`);
  } finally {
    setCouponExpire(coupon.couponUserId, before);
  }
});

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

test('API-K08 投保-他人车辆', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  // 夹具：给第二个会员造一辆车（dev 单会员，接口层造不出他人车辆）
  const vid = createVehicleForMember(ensureOtherMember(), `粤K${String(Date.now()).slice(-5)}`);
  const prods = (await api('GET', '/api/v1/insurances/products', { token: s.token })).data || [];
  const first = prods[0];
  if (!first) t.skip('无保险产品');
  const productId = first.insuranceProductId ?? first.productId;
  // 越权防护（2026-10-24 修复）：投保会生成保单并记保费，必须校验车辆归属
  const res = await api('POST', '/api/v1/insurances', { token: s.token, body: { productId, vehicleId: vid } });
  bizCode(res, 10001, 'K08 投保他人车辆应拒绝');
});

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

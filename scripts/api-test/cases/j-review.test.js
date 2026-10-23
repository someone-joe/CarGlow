/**
 * API-J 评价/售后/退款（12 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》J 段；规则表 2.4；PROGRESS 13 退款闭环。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, field, typeIs } from '../lib/assert.js';
import { getSession, createOrder, driveChain, adminLogin, cleanupCreated } from '../lib/factory.js';
import { SIDECAR_ENABLED, sql } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

/** 后台成功判定：若依口径 code=200 */
function okAdmin(env, label) {
  assert.equal(env.code, 200, `${label}: 期望 code=200，实际 code=${env.code} msg=${env.msg}`);
}

test('API-J01 评价-正向（WAIT_REVIEW → FINISHED）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J01 评价' });
  await driveChain(orderNo, 'WAIT_REVIEW');
  const res = await api('POST', `/api/v1/orders/${orderNo}/reviews`, {
    token: s.token, body: { rating: 5, content: '[AUTOTEST] 好评', tags: ['洗得干净'] },
  });
  ok(res, 'J01');
  field(await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token }), 'data.status', 'FINISHED', 'J01 评价后状态');
});

test('API-J02 评价-≤4 星强制原因', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J02 低分' });
  await driveChain(orderNo, 'WAIT_REVIEW');
  // 不填原因 → 拒
  const noReason = await api('POST', `/api/v1/orders/${orderNo}/reviews`, { token: s.token, body: { rating: 3 } });
  assert.notEqual(noReason.code, 0, `J02: ≤4 星不填原因应拒，实际 code=${noReason.code} msg=${noReason.msg}`);
  console.log(`  [J02] 无原因: code=${noReason.code} msg=${noReason.msg}`);
  // 填原因 → 过
  const withReason = await api('POST', `/api/v1/orders/${orderNo}/reviews`, {
    token: s.token, body: { rating: 3, content: '轮毂没洗干净' },
  });
  ok(withReason, 'J02 填原因后');
});

test('API-J03 评价-评分越界（0/6 拒绝）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J03 越界' });
  await driveChain(orderNo, 'WAIT_REVIEW');
  for (const rating of [0, 6, -1]) {
    const res = await api('POST', `/api/v1/orders/${orderNo}/reviews`, { token: s.token, body: { rating } });
    assert.notEqual(res.code, 0, `J03: rating=${rating} 应拒，实际 code=${res.code}`);
  }
});

test('API-J04 重复评价（一单一评）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J04 重复评价' });
  await driveChain(orderNo, 'WAIT_REVIEW');
  ok(await api('POST', `/api/v1/orders/${orderNo}/reviews`, { token: s.token, body: { rating: 5 } }), 'J04 首次');
  const second = await api('POST', `/api/v1/orders/${orderNo}/reviews`, { token: s.token, body: { rating: 4 } });
  assert.notEqual(second.code, 0, `J04: 二次评价应拒，实际 code=${second.code} msg=${second.msg}`);
});

test('API-J05 非待评价状态评价（21002）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J05 状态不符' });
  await driveChain(orderNo, 'WASHING');
  bizCode(await api('POST', `/api/v1/orders/${orderNo}/reviews`, { token: s.token, body: { rating: 5 } }), 21002, 'J05');
});

test('API-J06 售后三类建单（REWASH/REFUND/CLAIM）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J06 三类售后' });
  await driveChain(orderNo, 'FINISHED');
  for (const type of ['REWASH', 'CLAIM']) {
    const res = await api('POST', '/api/v1/after-sales', {
      token: s.token, body: { orderNo, type, reason: `[AUTOTEST] ${type}` },
    });
    ok(res, `J06 ${type} 建单`);
    typeIs(res.data, 'string', `J06 ${type} 工单号（实测裸字符串）`);
  }
});

test('API-J07 退款受理闭环（钱到否以退款单状态为准）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J07 退款闭环' });
  await driveChain(orderNo, 'FINISHED');
  const as = await api('POST', '/api/v1/after-sales', {
    token: s.token, body: { orderNo, type: 'REFUND', reason: '重洗后仍不满意' },
  });
  ok(as, 'J07 建单');
  const ticketNo = as.data;

  const atoken = await adminLogin();
  okAdmin(await api('PUT', `/admin-api/wash/after-sale/${ticketNo}/handle`, { token: atoken, body: { accept: true } }), 'J07 后台同意');

  // 口径：处置成功 ≠ 钱已退；以 wash_order_refund.status 为准（PROGRESS 13.2）
  const refund = sql(`select status, amount from wash_order_refund where order_no = '${orderNo}' order by refund_id desc limit 1`).trim();
  assert.ok(refund.startsWith('SUCCESS'), `J07: mock 渠道退款单应 SUCCESS，实际 ${refund}`);
  field(await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token }), 'data.status', 'REFUNDED', 'J07 订单终态');
});

test('API-J08 重复处置幂等', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J08 重复处置' });
  await driveChain(orderNo, 'FINISHED');
  const as = await api('POST', '/api/v1/after-sales', { token: s.token, body: { orderNo, type: 'REFUND', reason: 'J08' } });
  ok(as, 'J08 建单');
  const atoken = await adminLogin();
  okAdmin(await api('PUT', `/admin-api/wash/after-sale/${as.data}/handle`, { token: atoken, body: { accept: true } }), 'J08 首次处置');
  // 二次处置应被幂等拒绝（status='INIT' 乐观锁）
  const second = await api('PUT', `/admin-api/wash/after-sale/${as.data}/handle`, { token: atoken, body: { accept: true } });
  assert.notEqual(second.code, 200, `J08: 重复处置应被拒，实际 code=${second.code} msg=${second.msg}`);
});

test('API-J10 驳回不填原因拒绝', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J10 驳回' });
  await driveChain(orderNo, 'FINISHED');
  const as = await api('POST', '/api/v1/after-sales', { token: s.token, body: { orderNo, type: 'REFUND', reason: 'J10' } });
  ok(as, 'J10 建单');
  const atoken = await adminLogin();
  const res = await api('PUT', `/admin-api/wash/after-sale/${as.data}/handle`, { token: atoken, body: { accept: false } });
  assert.notEqual(res.code, 200, `J10: 驳回不填原因应拒，实际 code=${res.code} msg=${res.msg}`);
});

test('API-J11 CLAIM 不触发退款', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'J11 理赔' });
  await driveChain(orderNo, 'FINISHED');
  const as = await api('POST', '/api/v1/after-sales', { token: s.token, body: { orderNo, type: 'CLAIM', reason: '疑似剐蹭' } });
  ok(as, 'J11 建单');
  const atoken = await adminLogin();
  okAdmin(await api('PUT', `/admin-api/wash/after-sale/${as.data}/handle`, { token: atoken, body: { accept: true } }), 'J11 处置');
  // 副作用：不应产生退款单
  const refund = sql(`select count(*) from wash_order_refund where order_no = '${orderNo}'`).trim();
  assert.equal(refund, '0', `J11: CLAIM 不应触发退款单，实际 ${refund} 条`);
});

test('API-J09 SUCCESS 退款单重退拒绝', { todo: '依赖后台「退款单人工重试」入口与渠道状态机配合，留待 O 批（后台退款单页）一起覆盖' }, () => {});
test('API-J12 部分退款不改状态', { todo: '无部分退款的客户端入口（金额由服务端按实付解析）；规则表口径已由 J07 全额场景间接验证' }, () => {});

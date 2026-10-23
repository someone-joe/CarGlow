/**
 * API-L 自助洗车（12 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》L 段；小程序需求文档拆解 3.1。
 * 造数：工位取 seed（siteId=1，两个 IDLE 工位）；服务取自助洗车项。
 */
import { test, afterEach } from 'node:test';
import { randomUUID } from 'node:crypto';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, field, typeIs } from '../lib/assert.js';
import { getSession, createSelfwashOrder, getSelfwashOrder, selfwashPay, cleanupCreated, cleanupSelfwash, topUpExact } from '../lib/factory.js';
import { SIDECAR_ENABLED, sql } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());
afterEach(() => cleanupSelfwash());

test('API-L01 计价参数与工位列表', async () => {
  const s = await getSession();
  const pricing = await api('GET', '/api/v1/selfwash/pricing', { token: s.token, query: { siteId: 1 } });
  ok(pricing, 'L01 pricing');
  const stations = await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } });
  ok(stations, 'L01 workstations');
  typeIs(stations.data, 'array', 'L01 工位列表');
  assert.ok(stations.data.length > 0, 'L01: 应有工位');
  const idle = stations.data.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  assert.ok(idle, 'L01: 应有空闲可用工位');
});

test('API-L02 选位下单（快照价，建待支付单）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  assert.ok(idle, 'L02: 需要空闲工位');
  const { res, orderNo } = await createSelfwashOrder(idle.workstationId);
  ok(res, 'L02 下单');
  // 副作用：自助订单待支付 + 工位占用/排队
  const row = sql(`select status from wash_selfwash_order where order_no = '${orderNo}'`).trim();
  assert.equal(row, 'WAIT_PAY', `L02: 应为待支付，实际 ${row}`);
});

test('API-L03 余额支付 → 待使用', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  await topUpExact(10000); // sweep 后钱包为 0，先顶充再验余额支付
  const { orderNo } = await createSelfwashOrder(idle.workstationId);
  const pay = await selfwashPay(orderNo, 'balance');
  ok(pay, 'L03 余额支付');
  field(await getSelfwashOrder(orderNo), 'data.status', 'WAIT_USE', 'L03 支付后状态');
});

test('API-L04 取消-待支付直接取消', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  const { orderNo } = await createSelfwashOrder(idle.workstationId);
  const res = await api('POST', `/api/v1/selfwash/orders/${orderNo}/cancel`, { token: s.token, body: { reason: 'L04 取消' } });
  ok(res, 'L04');
  field(await getSelfwashOrder(orderNo), 'data.status', 'CANCELED', 'L04 取消后状态');
});

test('API-L05 取消-待使用退余额', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  await topUpExact(10000);
  const { orderNo } = await createSelfwashOrder(idle.workstationId);
  ok(await selfwashPay(orderNo, 'balance'), 'L05 支付');
  const before = (await api('GET', '/api/v1/wallet', { token: s.token })).data;
  ok(await api('POST', `/api/v1/selfwash/orders/${orderNo}/cancel`, { token: s.token, body: { reason: 'L05 取消' } }), 'L05 取消');
  const after = (await api('GET', '/api/v1/wallet', { token: s.token })).data;
  assert.ok(after.balance + after.giftBalance > before.balance + before.giftBalance,
    `L05: 待使用取消应退余额，${before.balance + before.giftBalance} → ${after.balance + after.giftBalance}`);
});

test('API-L06 取消-服务中拒绝', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  const { orderNo } = await createSelfwashOrder(idle.workstationId);
  ok(await selfwashPay(orderNo), 'L06 支付');
  ok(await api('POST', `/api/v1/selfwash/orders/${orderNo}/activate`, { token: s.token }), 'L06 激活');
  field(await getSelfwashOrder(orderNo), 'data.status', 'SERVING', 'L06 激活后状态');
  const res = await api('POST', `/api/v1/selfwash/orders/${orderNo}/cancel`, { token: s.token, body: { reason: 'L06 取消' } });
  assert.notEqual(res.code, 0, `L06: 服务中不可取消，实际 code=${res.code} msg=${res.msg}`);
});

test('API-L07 激活-空闲通电生成倒计时', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  const { orderNo } = await createSelfwashOrder(idle.workstationId);
  ok(await selfwashPay(orderNo), 'L07 支付');
  ok(await api('POST', `/api/v1/selfwash/orders/${orderNo}/activate`, { token: s.token }), 'L07 激活');
  const detail = await getSelfwashOrder(orderNo);
  field(detail, 'data.status', 'SERVING', 'L07 状态');
  // SERVING 详情带剩余秒数（>0）
  assert.ok(detail.data.remainingSeconds > 0, `L07: 应有剩余秒数，实际 ${detail.data.remainingSeconds}`);
});

test('API-L08 激活-前方未完结则排队', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  // 第一单：激活占住工位
  const first = await createSelfwashOrder(idle.workstationId);
  ok(await selfwashPay(first.orderNo), 'L08 首单支付');
  ok(await api('POST', `/api/v1/selfwash/orders/${first.orderNo}/activate`, { token: s.token }), 'L08 首单激活');
  // 第二单：同工位，激活应进排队而非直接服务
  const second = await createSelfwashOrder(idle.workstationId);
  ok(second.res, 'L08 次单下单');
  ok(await selfwashPay(second.orderNo), 'L08 次单支付');
  const act = await api('POST', `/api/v1/selfwash/orders/${second.orderNo}/activate`, { token: s.token });
  ok(act, 'L08 次单激活');
  const detail = await getSelfwashOrder(second.orderNo);
  assert.equal(detail.data.status, 'QUEUING', `L08: 前方未结转为排队，实际 ${detail.data.status}`);
});

test('API-L09 加时-整档计费不足一档按一档', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  const { orderNo } = await createSelfwashOrder(idle.workstationId);
  ok(await selfwashPay(orderNo), 'L09 支付');
  ok(await api('POST', `/api/v1/selfwash/orders/${orderNo}/activate`, { token: s.token }), 'L09 激活');
  const before = (await api('GET', `/api/v1/selfwash/orders/${orderNo}`, { token: s.token })).data;
  const res = await api('POST', `/api/v1/selfwash/orders/${orderNo}/add-time`, {
    token: s.token, body: { minutes: 30 }, headers: { 'Idempotency-Key': randomUUID() },
  });
  ok(res, 'L09 加时');
  const after = (await api('GET', `/api/v1/selfwash/orders/${orderNo}`, { token: s.token })).data;
  assert.ok(after.remainingSeconds > before.remainingSeconds,
    `L09: 加时后剩余秒数应增加，${before.remainingSeconds} → ${after.remainingSeconds}`);
});

test('API-L10 结束服务-断电完成', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const stations = (await api('GET', '/api/v1/selfwash/workstations', { token: s.token, query: { siteId: 1 } })).data;
  const idle = stations.find((w) => w.status === 'IDLE' && w.enabled === 'Y');
  if (!idle) t.skip('无空闲工位');
  const { orderNo } = await createSelfwashOrder(idle.workstationId);
  ok(await selfwashPay(orderNo), 'L10 支付');
  ok(await api('POST', `/api/v1/selfwash/orders/${orderNo}/activate`, { token: s.token }), 'L10 激活');
  ok(await api('POST', `/api/v1/selfwash/orders/${orderNo}/finish`, { token: s.token, headers: { 'Idempotency-Key': randomUUID() } }), 'L10 结束');
  // 自助订单终态枚举为 DONE（实测）
  field(await getSelfwashOrder(orderNo), 'data.status', 'DONE', 'L10 结束后状态');
});

test('API-L11 我的自助订单列表', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/selfwash/orders', { token: s.token });
  ok(res, 'L11');
  typeIs(res.data, 'array', 'L11 列表类型');
});

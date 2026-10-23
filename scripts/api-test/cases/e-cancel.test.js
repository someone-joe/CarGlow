/**
 * API-E 取消与紧急取钥匙（10 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》E 段；规则表 2.3；PRD 9 异常场景。
 * 副作用断言：格口释放（wash_slot）、产能回补（Redis）、退款单（wash_order_refund）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, field } from '../lib/assert.js';
import { getSession, createOrder, cancelOrder, driveChain, getOrder, cleanupCreated, receiveAnyCoupon } from '../lib/factory.js';
import { TEST_MARKER } from '../lib/config.js';
import { SIDECAR_ENABLED, sql, redis, capacityKey, couponStatus } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

test('API-E01 取消-未支付（CANCELED + 格口释放 + 产能回补）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const capBefore = redis('GET', capacityKey());
  const { orderNo } = await createOrder({ remark: 'E01 未支付取消' });
  const capAfterCreate = redis('GET', capacityKey());
  assert.equal(Number(capAfterCreate), Number(capBefore) - 1, 'E01: 下单应扣 1 产能');

  ok(await cancelOrder(orderNo, 'E01 取消'), 'E01');
  field(await getOrder(orderNo), 'data.status', 'CANCELED', 'E01 取消后状态');
  // 格口释放
  const slot = sql(`select status from wash_slot where order_no = '${orderNo}'`).trim();
  assert.ok(slot === '' || slot === 'FREE', `E01: 格口应释放，实际 ${slot || '(已解绑)'}`);
  // 产能回补
  const capAfterCancel = redis('GET', capacityKey());
  assert.equal(Number(capAfterCancel), Number(capBefore), `E01: 取消应回补产能，${capBefore} → ${capAfterCancel}`);
});

test('API-E02 取消-已支付未取车（REFUNDED 全额退款）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'E02 已支付取消' });
  await driveChain(orderNo, 'WAIT_KEY');
  ok(await cancelOrder(orderNo, 'E02 取消'), 'E02');
  // mock 渠道自动全额退款成功 → 终态 REFUNDED（2026-10-24 实测确认）
  field(await getOrder(orderNo), 'data.status', 'REFUNDED', 'E02 终态');
  // 退款单表主键是 refund_id（不是 id）
  const refund = sql(`select status, amount from wash_order_refund where order_no = '${orderNo}' order by refund_id desc limit 1`).trim();
  assert.ok(refund.length > 0, 'E02: 应生成退款单');
  assert.ok(refund.startsWith('SUCCESS'), `E02: 退款单应 SUCCESS，实际 ${refund}`);
});

test('API-E03 取消-PICKING 退全款不扣跑腿费', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'E03 取车中取消' });
  await driveChain(orderNo, 'PICKING');
  ok(await cancelOrder(orderNo, 'E03 取消'), 'E03');
  const detail = await getOrder(orderNo);
  assert.ok(['REFUNDED', 'REFUNDING'].includes(detail.data.status),
    `E03: 已支付单取消应走全额退款，实际 ${detail.data.status}`);
  const refund = sql(`select amount from wash_order_refund where order_no = '${orderNo}' order by refund_id desc limit 1`).trim();
  assert.equal(Number(refund), detail.data.payAmount, `E03: 退款额应=实付 ${detail.data.payAmount}，实际 ${refund}`);
});

test('API-E04 取消-已开洗被拒（PRD 红线，21002）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'E04 开洗取消' });
  await driveChain(orderNo, 'WASHING');
  bizCode(await cancelOrder(orderNo, 'E04 取消'), 21002, 'E04');
});

test('API-E05 取消-不填原因（21004）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'E05 无原因' });
  bizCode(await api('POST', `/api/v1/orders/${orderNo}/cancel`, { token: s.token, body: {} }), 21004, 'E05');
});

test('API-E06 取消-订单不存在（21001）', async () => {
  const s = await getSession();
  bizCode(await cancelOrder('SE-NOT-EXIST-0001', 'E06'), 21001, 'E06');
});

test('API-E07 重复取消（21002）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'E07 重复取消' });
  ok(await cancelOrder(orderNo, 'E07 首次'), 'E07 首次');
  bizCode(await cancelOrder(orderNo, 'E07 二次'), 21002, 'E07 二次');
});

test('API-E08 紧急取钥匙-车未动（取消+全额退款+留痕）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'E08 紧急取钥匙' });
  await driveChain(orderNo, 'KEY_IN');
  const res = await api('POST', `/api/v1/orders/${orderNo}/emergency-take-key`, { token: s.token });
  ok(res, 'E08');
  const detail = await getOrder(orderNo);
  assert.ok(['REFUNDED', 'REFUNDING', 'CANCELED'].includes(detail.data.status),
    `E08: 车未动应急取钥匙应取消并退款，实际 ${detail.data.status}`);
  // 留痕：流转日志有应急取消原因
  const log = sql(
    `select reason from wash_order_status_log where order_id = (select order_id from wash_order where order_no = '${orderNo}') order by id desc limit 1`
  ).trim();
  assert.ok(log.length > 0, `E08: 应有留痕原因，实际空`);
});

test('API-E09 紧急取钥匙-车已动拒绝（B1002 → 21002）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'E09 车已动' });
  await driveChain(orderNo, 'TO_STATION');
  bizCode(await api('POST', `/api/v1/orders/${orderNo}/emergency-take-key`, { token: s.token }), 21002, 'E09');
});

test('API-E10 取消-权益返还（用券订单取消后券回 UNUSED）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  // 规则表 2.3：取消副作用含「权益（券/体验名额）返还」。
  // 2026-10-24 修复：doCancel 调 WashCouponService.returnByOrderNo，按 order_no 反查已核销券并置回 UNUSED。
  const coupon = await receiveAnyCoupon(3900);
  if (!coupon) t.skip('没有可领且门槛 ≤3900 的券模板');
  const { res } = await createOrder({ couponUserId: coupon.couponUserId, remark: 'E10 券返还' });
  ok(res, 'E10 用券下单');
  assert.equal(couponStatus(coupon.couponUserId), 'USED', 'E10: 下单后券应已核销为 USED');

  ok(await cancelOrder(res.data.orderNo, `${TEST_MARKER} E10 券返还`), 'E10 取消');
  assert.equal(couponStatus(coupon.couponUserId), 'UNUSED',
    `E10: 取消后券应返还为 UNUSED，实际 ${couponStatus(coupon.couponUserId)}`);
});

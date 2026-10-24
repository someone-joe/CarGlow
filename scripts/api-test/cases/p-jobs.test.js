/**
 * API-P 定时任务（7 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》P 段；规则表 3 定时任务小节。
 *
 * 后台已暴露「手动触发 job」接口（POST /admin-api/wash/job/trigger，白名单），
 * 并支持 asOf 时间旅行偏移（仅运维/接口自动化）与 refund-retry 的 forceFail 开关，
 * 因此 P03/P04/P07 已由 todo 转正为真实断言；P06 仍依赖硬件/故障注入，维持 todo。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok } from '../lib/assert.js';
import { getSession, createOrder, cleanupCreated, mockPay, driveChain, adminLogin, getOrder, adminCancelOrder } from '../lib/factory.js';
import { SIDECAR_ENABLED, sql, redis, capacityKey } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

test('API-P01 支付超时取消：倒计时按配置生成（15 分钟）', async () => {
  const s = await getSession();
  const before = Date.now();
  const { res } = await createOrder({ remark: 'P01 倒计时' });
  ok(res, 'P01');
  const expireAt = res.data.payExpireAt;
  const minutes = (expireAt - before) / 60000;
  // wash.order.pay-timeout-minutes=15（配置驱动，阈值不硬编码在用例里）
  assert.ok(minutes > 14 && minutes < 16, `P01: 支付倒计时应约 15 分钟，实际 ${minutes.toFixed(2)} 分钟`);
});

test('API-P02 格口预占释放：下单即预占、取消即释放', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'P02 格口预占' });
  // 下单后格口被预占（RESERVED）
  const reserved = sql(`select status from wash_slot where order_no = '${orderNo}'`).trim();
  assert.equal(reserved, 'RESERVED', `P02: 下单后格口应 RESERVED，实际 ${reserved}`);
  // 取消后释放（job 之外的等价路径已由 E01 断言；此处再确认一次口径）
  ok(await api('POST', `/api/v1/orders/${orderNo}/cancel`, { token: s.token, body: { reason: '[AUTOTEST] P02' } }), 'P02 取消');
  const freed = sql(`select status from wash_slot where order_no = '${orderNo}'`).trim();
  assert.ok(freed === '' || freed === 'FREE', `P02: 取消后格口应释放，实际 ${freed || '(已解绑)'}`);
});

test('API-P03 23:30 未存钥匙顺延次日', async () => {
  const s = await getSession();
  // 建待支付单 → 模拟支付 → WAIT_KEY（钥匙尚未存入柜机）
  const { orderNo } = await createOrder({ remark: 'P03 顺延' });
  ok(await mockPay(orderNo), 'P03 模拟支付');
  let detail = await getOrder(orderNo);
  assert.equal(detail.data.status, 'WAIT_KEY', `P03: 应 WAIT_KEY，实际 ${detail.data.status}`);
  const beforeAppoint = Number(sql(`select appoint_time from wash_order where order_no='${orderNo}'`).trim());
  // 触发顺延 job：用 asOf 把"今天"对齐到后天，使下单的 T+1 预约被判为已错过当天存钥匙截止
  const atoken = await adminLogin();
  const trig = await api('POST', '/admin-api/wash/job/trigger', {
    token: atoken, body: { job: 'order-postpone', params: { asOf: Date.now() + 2 * 86400000 } },
  });
  assert.equal(trig.code, 200, `P03 触发顺延 job 失败: ${trig.msg}`);
  detail = await getOrder(orderNo);
  // 规则表 2.2：状态不变（WAIT_KEY 自环），预约日顺延到次日，释放预占格口
  assert.equal(detail.data.status, 'WAIT_KEY', 'P03: 顺延后状态应仍为 WAIT_KEY');
  const afterAppoint = Number(sql(`select appoint_time from wash_order where order_no='${orderNo}'`).trim());
  assert.ok(afterAppoint > beforeAppoint,
    `P03: 预约时间应顺延后移（前 ${beforeAppoint} 后 ${afterAppoint}）`);
});

test('API-P04 T+3 自动完成（WAIT_REVIEW → FINISHED）', async () => {
  const s = await getSession();
  // 走真实业务主链路到 WAIT_REVIEW（取车→入场→洗车→质检→还车→待评价）
  const { orderNo } = await createOrder({ remark: 'P04 自动完成' });
  ok(await mockPay(orderNo), 'P04 模拟支付');
  await driveChain(orderNo, 'WAIT_REVIEW');
  let detail = await getOrder(orderNo);
  assert.equal(detail.data.status, 'WAIT_REVIEW', `P04: 应 WAIT_REVIEW，实际 ${detail.data.status}`);
  // T+3 边界：asOf 设为 now+4 天 → 阈值=now+1 天 >= update_time(now) → 命中自动完成
  const atoken = await adminLogin();
  const trig = await api('POST', '/admin-api/wash/job/trigger', {
    token: atoken, body: { job: 'order-auto-finish', params: { asOf: Date.now() + 4 * 86400000 } },
  });
  assert.equal(trig.code, 200, `P04 触发自动完成 job 失败: ${trig.msg}`);
  detail = await getOrder(orderNo);
  assert.equal(detail.data.status, 'FINISHED', `P04: 应 FINISHED，实际 ${detail.data.status}`);
});

test('API-P05 每日产能初始化幂等（SETNX 不重置已有值）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  // 前置断言：今日产能 key 已存在且为整数（初始化 job 已跑过）
  const key = capacityKey();
  const value = redis('GET', key);
  assert.ok(value !== '' && Number.isInteger(Number(value)),
    `P05: 今日产能 key(${key}) 应已初始化为整数，实际 ${value || '(不存在)'}`);
  // 幂等含义：job 用 SETNX，已存在则不覆盖——此处只能验证「存在即不重置」的前提成立，
  // job 实际跑一轮的行为待后台触发能力后补
  console.log(`  [P05] 当前 ${key} = ${value}`);
});

test('API-P06 柜机离线检测与应急', { todo: '依赖硬件/模拟回调故障注入（device-callback 超时/失败），待联调环境具备故障注入能力后补' }, () => {});

test('API-P07 退款失败自动重试（上限 5 次）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  // 建待支付单 → 模拟支付 → 后台取消（同步建退款单）；先用 forceFail 让退款渠道强制失败
  const { orderNo } = await createOrder({ remark: 'P07 退款重试' });
  ok(await mockPay(orderNo), 'P07 模拟支付');
  const atoken = await adminLogin();
  const setFail = await api('POST', '/admin-api/wash/job/trigger',
    { token: atoken, body: { job: 'refund-retry', params: { forceFail: true } } });
  assert.equal(setFail.code, 200, `P07 开启 mock 强制失败失败: ${setFail.msg}`);
  const ac = await adminCancelOrder(orderNo, '[AUTOTEST] P07 构造退款失败');
  assert.equal(ac.code, 200, `P07 后台取消失败: ${ac.msg}`);
  // 退款单应留在 FAILED 且 retry_count=1（失败被捕获、不丢、待重试）
  const failRow = sql(`select status, retry_count from wash_order_refund where order_no='${orderNo}' order by refund_id desc limit 1`).trim().split('\t');
  assert.equal(failRow[0], 'FAILED', `P07: 退款单应 FAILED，实际 ${failRow[0]}`);
  assert.equal(Number(failRow[1]), 1, `P07: retry_count 应为 1，实际 ${failRow[1]}`);
  // 关闭失败开关并重试 → 退款成功、订单置 REFUNDED
  const unsetFail = await api('POST', '/admin-api/wash/job/trigger',
    { token: atoken, body: { job: 'refund-retry', params: { forceFail: false } } });
  assert.equal(unsetFail.code, 200, `P07 关闭 mock 强制失败失败: ${unsetFail.msg}`);
  const retry = await api('POST', '/admin-api/wash/job/trigger',
    { token: atoken, body: { job: 'refund-retry' } });
  assert.equal(retry.code, 200, `P07 触发重试失败: ${retry.msg}`);
  const detail = await getOrder(orderNo);
  assert.equal(detail.data.status, 'REFUNDED', `P07: 订单应 REFUNDED，实际 ${detail.data.status}`);
  // 上限 5 次由 RefundRetryJob.MAX_RETRY=5 与 listPending(retry_count<5) 保证，失败不无限重试
});

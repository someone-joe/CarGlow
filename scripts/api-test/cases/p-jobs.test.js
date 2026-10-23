/**
 * API-P 定时任务（7 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》P 段；规则表 3 定时任务小节。
 *
 * 现状：后台无「手动触发 job」接口（实测 /admin-api/monitor/job/* 未实现），
 * 因此本批只断言「job 的输入状态正确」（配置驱动、可预期），job 本身的流转标 todo——
 * 待后台提供 job 触发/时间旅行能力后转正。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok } from '../lib/assert.js';
import { getSession, createOrder, cleanupCreated } from '../lib/factory.js';
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

test('API-P03 23:30 未存钥匙顺延次日', { todo: '时间依赖：需把订单 appointTime 置为前一日且系统时间过 23:30，或后台提供 job 触发;待后台 job 触发能力后补' }, () => {});

test('API-P04 T+3 自动完成（WAIT_REVIEW → FINISHED）', { todo: '时间依赖：需 T+3 边界（差 1 天不流转、满 3 天才流转），待 job 触发/时间旅行能力后补' }, () => {});

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

test('API-P07 退款失败自动重试（上限 5 次）', { todo: '需构造渠道失败退款单（mock 渠道可强制失败开关），待后台提供失败注入/重试触发接口后补' }, () => {});

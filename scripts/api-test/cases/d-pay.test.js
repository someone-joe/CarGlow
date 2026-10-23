/**
 * API-D 支付（11 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》D 段；技术方案 3.4 回调幂等。
 * D10（回调丢失补偿，定时任务 5 分钟一轮）与 D11（mock 开关，配置级）不在快速回归内，标记 todo。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, field, typeIs, amountIs } from '../lib/assert.js';
import {
  getSession, createOrder, getOrder, mockPay, balancePay,
  getWallet, cleanupCreated, drainWalletBelow,
} from '../lib/factory.js';

// 每条用例后取消其创建的订单（失败也执行），避免格口被占导致后续用例 B2003 连坐
afterEach(() => cleanupCreated());

test('API-D01 mock-pay-正向（WAIT_PAY → WAIT_KEY）', async () => {
  const { orderNo } = await createOrder({ remark: 'D01 支付' });
  const pay = await mockPay(orderNo);
  ok(pay, 'D01');
  const detail = await getOrder(orderNo);
  field(detail, 'data.status', 'WAIT_KEY', 'D01 支付后状态');
});

test('API-D02 重复支付拒绝（21002）', async () => {
  const { orderNo } = await createOrder({ remark: 'D02 重复支付' });
  ok(await mockPay(orderNo), 'D02 首次');
  const again = await mockPay(orderNo);
  bizCode(again, 21002, 'D02 重复支付');
});

test('API-D03 支付-订单不存在（21001）', async () => {
  const res = await mockPay('SE-NOT-EXIST-0001');
  bizCode(res, 21001, 'D03');
});

test('API-D04 非待支付状态支付拒绝（KEY_IN 后再支付）', async () => {
  // 迷你链路：下单 → 支付 → 取开箱码 → 柜机回调存钥匙 → KEY_IN → 再支付应被拒
  const { orderNo } = await createOrder({ remark: 'D04 状态不符' });
  ok(await mockPay(orderNo), 'D04 支付');
  const oc = await api('POST', `/api/v1/orders/${orderNo}/open-code`, { token: (await getSession()).token });
  ok(oc, 'D04 取开箱码');
  const code = oc.data?.code ?? oc.data;
  assert.ok(code, `D04: 开箱码应有值，实际 ${JSON.stringify(oc.data)}`);
  const deposit = await api('POST', '/device-callback/v1/slot/deposit', { body: { orderNo, code } });
  console.log(`  [D04] 柜机回调: code=${deposit.code} msg=${deposit.msg}`);
  const detail = await getOrder(orderNo);
  field(detail, 'data.status', 'KEY_IN', 'D04 存钥匙后状态');
  const repay = await mockPay(orderNo);
  bizCode(repay, 21002, 'D04 非 WAIT_PAY 支付');
});

test('API-D05 余额支付-充足（先赠后本，副作用断言）', async () => {
  const s = await getSession();
  const before = await getWallet();
  // 用 ruleId=3 档位充值（充 50000 送 10000），确定性构造「赠金 ≥ 订单额」→ 走先赠后本
  const rc = await api('POST', '/api/v1/wallet/recharge', { token: s.token, body: { ruleId: 3 } });
  ok(rc, 'D05 档位充值');
  field(rc, 'data.payAmount', 50000, 'D05 充值额');
  field(rc, 'data.giftAmount', 10000, 'D05 赠送额');
  const credit = await api('POST', `/api/v1/wallet/recharge/${rc.data.rechargeNo}/mock-pay`, { token: s.token });
  ok(credit, 'D05 充值到账');

  const { orderNo } = await createOrder({ remark: 'D05 余额支付' });
  const pay = await balancePay(orderNo);
  ok(pay, 'D05');
  const detail = await getOrder(orderNo);
  field(detail, 'data.status', 'WAIT_KEY', 'D05 支付后状态');

  const after = await getWallet();
  // 先赠后本：赠金（before + 10000）≥ 订单额 3900，只扣赠金不动本金
  assert.equal(
    after.giftBalance,
    before.giftBalance + 10000 - 3900,
    `D05: 赠金应扣 3900，期望 ${before.giftBalance + 10000 - 3900}，实际 ${after.giftBalance}`
  );
  assert.equal(
    after.balance,
    before.balance + 50000,
    `D05: 本金不应被扣（仅追加档位充值 50000），期望 ${before.balance + 50000}，实际 ${after.balance}`
  );
});

test('API-D06 余额支付-不足拒绝（B3004）', async () => {
  const s = await getSession();
  // 取 200000 分的高价服务，构造「余额不足」（dev 钱包可能因历史退款很肥，先泄洪到不足）
  const pricey = s.services.find((x) => x.displayPrice === 200000) || s.services[s.services.length - 1];
  await drainWalletBelow(pricey.displayPrice, pricey.serviceId);
  const { orderNo } = await createOrder({ serviceId: pricey.serviceId, remark: 'D06 余额不足' });
  const pay = await balancePay(orderNo);
  bizCode(pay, 23004, 'D06 余额不足');
  const detail = await getOrder(orderNo);
  field(detail, 'data.status', 'WAIT_PAY', 'D06 拒绝后订单应仍待支付');
});

test('API-D07 预支付参数', async () => {
  const { orderNo } = await createOrder({ remark: 'D07 prepay' });
  const res = await api('POST', `/api/v1/payments/${orderNo}/prepay`, { token: (await getSession()).token });
  ok(res, 'D07');
  typeIs(res.data, 'object', 'D07 prepay 数据');
  console.log(`  [D07] prepay 字段: ${Object.keys(res.data || {}).join(',')}`);
});

test('API-D08 支付回调-垃圾数据不动订单', async () => {
  const { orderNo } = await createOrder({ remark: 'D08 垃圾回调' });
  // 缺订单号
  const noOrder = await api('POST', '/api/v1/payments/wechat/notify', { body: { foo: 'bar' } });
  assert.equal(noOrder.code, 'FAIL', `D08: 缺订单号应返回 FAIL，实际 ${JSON.stringify(noOrder)}`);
  // 假订单号（不能推进任何真实订单）
  const fake = await api('POST', '/api/v1/payments/wechat/notify', { body: { orderNo: 'SE-FAKE-0001' } });
  console.log(`  [D08] 假订单号回调: ${JSON.stringify(fake)}`);
  const detail = await getOrder(orderNo);
  field(detail, 'data.status', 'WAIT_PAY', 'D08 真实订单不应被垃圾回调推动');
});

test('API-D09 支付回调-重复通知幂等', async () => {
  const { orderNo } = await createOrder({ remark: 'D09 重复回调' });
  const first = await api('POST', '/api/v1/payments/wechat/notify', { body: { orderNo } });
  assert.equal(first.code, 'SUCCESS', `D09: 首次回调应 SUCCESS，实际 ${JSON.stringify(first)}`);
  let detail = await getOrder(orderNo);
  field(detail, 'data.status', 'WAIT_KEY', 'D09 首次回调后状态');

  const second = await api('POST', '/api/v1/payments/wechat/notify', { body: { orderNo } });
  assert.equal(second.code, 'SUCCESS', `D09: 重复回调也应 SUCCESS（不能报错，否则微信重试），实际 ${JSON.stringify(second)}`);
  assert.equal(second.message, '重复通知已忽略', `D09: 重复回调应被忽略，实际 message=${second.message}`);
  detail = await getOrder(orderNo);
  field(detail, 'data.status', 'WAIT_KEY', 'D09 重复回调后状态不应再变');
  // 注：mock 渠道下无支付流水表（实测无 wash_payment 表），入账次数以状态机口径为准；
  // 接真实微信渠道后补「trade_no 唯一键」副作用断言。
});

test('API-D10 回调丢失主动查单补偿', { todo: '定时任务 5 分钟一轮，跑完整套 job 用例时补（见 API-P 段）' }, () => {});
test('API-D11 mock 关闭时行为', { todo: '配置级（wash.pay.mock-enabled），上线前检查项，不在功能回归内' }, () => {});

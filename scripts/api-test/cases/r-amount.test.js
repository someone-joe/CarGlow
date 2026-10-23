/**
 * API-R.1 金额边界（15 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》R.1 段（边界值分析法）。
 *
 * 关键实现事实（已核对 WashWalletService.recharge）：
 * - 自定义金额（amountFen）≥100 分即接受，且【永不获赠】；赠品只随 ruleId 精确档位走；
 * - 0 < amountFen < 100 → A0001；amountFen ≤ 0 或不传 → A0001；
 * - 到账幂等：重复 mock-pay 同一充值单不重复入账。
 * 因此「恰好够 / 差 1 分」可用自定义金额精确构造（无赠品干扰）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, field, amountIs } from '../lib/assert.js';
import { getSession, createOrder, balancePay, getWallet, topUpExact, getOrder, cleanupCreated, drainWalletBelow } from '../lib/factory.js';

// 每条用例后取消其创建的订单（失败也执行）
afterEach(() => cleanupCreated());

/** 发起充值（不 mock 到账），返回信封。 */
async function recharge(body) {
  const s = await getSession();
  return api('POST', '/api/v1/wallet/recharge', { token: s.token, body });
}

test('API-R01 充值金额=0', async () => {
  bizCode(await recharge({ amountFen: 0 }), 10001, 'R01');
});

test('API-R02 充值金额=1 分（低于 ¥1 下限）', async () => {
  bizCode(await recharge({ amountFen: 1 }), 10001, 'R02');
});

test('API-R03 充值金额=99 分（差 1 分到下限）', async () => {
  bizCode(await recharge({ amountFen: 99 }), 10001, 'R03');
});

test('API-R04 充值金额=100 分（下限恰好，精确到账）', async () => {
  const before = await getWallet();
  const r = await recharge({ amountFen: 100 });
  ok(r, 'R04 建单');
  field(r, 'data.payAmount', 100, 'R04 充值额');
  field(r, 'data.giftAmount', 0, 'R04 自定义金额无赠品');
  const s = await getSession();
  const pay = await api('POST', `/api/v1/wallet/recharge/${r.data.rechargeNo}/mock-pay`, { token: s.token });
  ok(pay, 'R04 到账');
  const after = await getWallet();
  assert.equal(after.balance, before.balance + 100, `R04: 本金应 +100，${before.balance} → ${after.balance}`);
  assert.equal(after.giftBalance, before.giftBalance, 'R04: 赠金不应变');
});

test('API-R05 充值金额=负数', async () => {
  bizCode(await recharge({ amountFen: -100 }), 10001, 'R05');
});

test('API-R06 充值金额=超大值（>int32，不溢出不 500）', async () => {
  const r = await recharge({ amountFen: 2147483648 });
  ok(r, 'R06 大额应被正确接纳（long/decimal）');
  field(r, 'data.payAmount', 2147483648, 'R06 金额不应溢出');
  amountIs(r.data.payAmount, 'R06');
});

test('API-R07 充值金额=非数字', async () => {
  let rejected = false;
  let detail = '';
  try {
    const r = await recharge({ amountFen: 'abc' });
    rejected = r.code !== 0;
    detail = `code=${r.code} msg=${r.msg}`;
  } catch (e) {
    rejected = true; // 传输层/解析层拒绝（如 400）同样算拒绝
    detail = e.message.split('\n')[0];
  }
  assert.ok(rejected, `R07: 非数字金额应被拒绝，实际 ${detail}`);
  console.log(`  [R07] ${detail}`);
});

test('API-R08 元分混淆（传 200 不按 ¥200 档误判）', async () => {
  const r = await recharge({ amountFen: 200 });
  ok(r, 'R08');
  field(r, 'data.payAmount', 200, 'R08 按分入账');
  field(r, 'data.giftAmount', 0, 'R08 ¥2 不命中任何赠送档');
});

test('API-R09 赠送档位边界（ruleId 精确档）', async () => {
  const r1 = await recharge({ ruleId: 1 });
  ok(r1, 'R09 档位1');
  field(r1, 'data.payAmount', 20000, 'R09 档位1 充值额');
  field(r1, 'data.giftAmount', 2000, 'R09 档位1 赠送额');
  const r2 = await recharge({ ruleId: 2 });
  ok(r2, 'R09 档位2');
  field(r2, 'data.payAmount', 30000, 'R09 档位2 充值额');
  field(r2, 'data.giftAmount', 5000, 'R09 档位2 赠送额');
  // 不存在的档位
  bizCode(await recharge({ ruleId: 999999 }), 10001, 'R09 假档位');
});

// R11/R12 合并执行：两者是同一组钱包边界构造（恰好够 / 差 1 分）。
// 分开跑会因「R11 支付到 0 → afterEach 取消退款 200000」把 R12 的起始状态搞乱，故在一个用例内连续完成。
test('API-R11+R12 余额边界：恰好扣到 0 与差 1 分拒绝', async () => {
  const s = await getSession();
  const pricey = s.services.find((x) => x.displayPrice === 200000);
  assert.ok(pricey, 'R11: 未找到 200000 分的高价服务');
  const target = pricey.displayPrice;

  // ---- R11：补到恰好等于订单额，支付后可用余额应为 0 ----
  await drainWalletBelow(target, pricey.serviceId); // 钱包可能因历史退款过肥，先泄洪到可构造
  let w = await getWallet();
  let topUp = target - (w.balance + w.giftBalance);
  assert.ok(topUp >= 100, `R11: 需补差 ${topUp} 分（应 ≥100 才能构造）`);
  await topUpExact(topUp); // 自定义金额无赠品 → 精确到达 target

  const r11 = await createOrder({ serviceId: pricey.serviceId, remark: 'R11 恰好够' });
  ok(await balancePay(r11.orderNo), 'R11 恰好够应支付成功');
  field(await getOrder(r11.orderNo), 'data.status', 'WAIT_KEY', 'R11 支付后状态');
  w = await getWallet();
  assert.equal(w.balance + w.giftBalance, 0, `R11: 扣款后可用余额应为 0，实际 ${w.balance + w.giftBalance}`);

  // ---- R12：此时可用为 0，补到「差 1 分」，支付应被拒且不扣款 ----
  topUp = target - 1;
  assert.ok(topUp >= 100, `R12: 需补差 ${topUp} 分（应 ≥100 才能构造）`);
  await topUpExact(topUp);

  const r12 = await createOrder({ serviceId: pricey.serviceId, remark: 'R12 差1分' });
  bizCode(await balancePay(r12.orderNo), 23004, 'R12 差 1 分应拒绝');
  field(await getOrder(r12.orderNo), 'data.status', 'WAIT_PAY', 'R12 拒绝后订单应仍待支付');
  w = await getWallet();
  assert.equal(
    w.balance + w.giftBalance,
    target - 1,
    `R12: 拒绝时不应扣款，可用余额应仍为 ${target - 1}，实际 ${w.balance + w.giftBalance}`
  );
});

// 以下暂不在本批执行（依赖其他模块或无法在当前 dev 数据下构造）：
test('API-R10 0 元订单', { todo: '需要「满 N 减 N」券模板把订单抵到 0，当前 seed 无此模板；券模块扩充后补' }, () => {});
test('API-R13 退款金额=实付', { todo: '由 J 段（评价/售后/退款）系统性覆盖' }, () => {});
test('API-R14 超额退款请求拒绝', { todo: '退款金额由服务端按实付解析，无客户端入口；J 段以副作用断言覆盖' }, () => {});
test('API-R15 加时不足一档按一档', { todo: 'L 段（自助洗车）覆盖' }, () => {});

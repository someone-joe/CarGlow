/**
 * API-F 订单查询（8 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》F 段。
 * 造状态走 driveChain（真实接口），断言以契约为准。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, truthy } from '../lib/assert.js';
import { getSession, createOrder, cancelOrder, driveChain, getOrder, cleanupCreated } from '../lib/factory.js';
import { SIDECAR_ENABLED, ensureOtherMember, setOrderOwner } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

test('API-F01 订单列表-三 Tab 映射', async () => {
  const s = await getSession();
  // 造五种状态各一单（真实链路推进）；每步先确认下单成功，失败早暴露而非驱动时崩
  const waitPay = await createOrder({ remark: 'F01 待支付' });
  ok(waitPay.res, 'F01 造待支付单');
  const waitKey = await createOrder({ remark: 'F01 待存钥匙' });
  ok(waitKey.res, 'F01 造待存钥匙单');
  await driveChain(waitKey.orderNo, 'WAIT_KEY');
  const washing = await createOrder({ remark: 'F01 清洗中' });
  ok(washing.res, 'F01 造清洗中单');
  await driveChain(washing.orderNo, 'WASHING');
  const finished = await createOrder({ remark: 'F01 已完成' });
  ok(finished.res, 'F01 造已完成单');
  await driveChain(finished.orderNo, 'FINISHED');
  const canceled = await createOrder({ remark: 'F01 已取消' });
  ok(canceled.res, 'F01 造已取消单');
  await cancelOrder(canceled.orderNo, 'F01 取消造态');

  const listIn = async (tab) => {
    const res = await api('GET', '/api/v1/orders', { token: s.token, query: { tab } });
    ok(res, `F01 tab=${tab}`);
    return (res.data.list || []).map((o) => o.orderNo);
  };

  const pending = await listIn('PENDING');
  assert.ok(pending.includes(waitPay.orderNo), 'F01: 待支付单应在 PENDING');
  assert.ok(pending.includes(waitKey.orderNo), 'F01: 待存钥匙单应在 PENDING');

  const serving = await listIn('SERVING');
  assert.ok(serving.includes(washing.orderNo), 'F01: 清洗中单应在 SERVING');

  const done = await listIn('DONE');
  assert.ok(done.includes(finished.orderNo), 'F01: 已完成单应在 DONE');
  assert.ok(done.includes(canceled.orderNo), 'F01: 已取消单应在 DONE');
});

test('API-F02 旧 tab 值兼容（ONGOING/WAIT_REVIEW/ALL）', async () => {
  const s = await getSession();
  for (const tab of ['ONGOING', 'WAIT_REVIEW', 'ALL']) {
    const res = await api('GET', '/api/v1/orders', { token: s.token, query: { tab } });
    ok(res, `F02 tab=${tab}`);
  }
});

test('API-F03 非法 tab（10001）', async () => {
  const s = await getSession();
  bizCode(await api('GET', '/api/v1/orders', { token: s.token, query: { tab: 'XXX' } }), 10001, 'F03');
});

test('API-F04 订单详情-8 节点时间轴', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'F04 时间轴' });
  await driveChain(orderNo, 'FINISHED');
  const detail = await getOrder(orderNo);
  ok(detail, 'F04');
  const tl = detail.data.timeline;
  assert.ok(Array.isArray(tl) && tl.length === 8, `F04: 时间轴应为 8 节点，实际 ${tl?.length}`);
  // 主链路全走完：除「已下单」外 7 个业务节点应全部 reached（8 节点含 ORDERED）
  const reached = tl.filter((n) => n.reached).length;
  assert.ok(reached >= 8, `F04: 全链路后 8 节点应全 reached，实际 ${reached}`);
  // current 唯一且是最后一个 reached 节点（当前态）
  const currents = tl.filter((n) => n.current);
  assert.equal(currents.length, 1, `F04: current 节点应唯一，实际 ${currents.length}`);
  // 每个 reached 节点都带时间（毫秒）
  for (const n of tl.filter((x) => x.reached)) {
    assert.ok(Number.isInteger(n.time) && n.time > 1_600_000_000_000, `F04: 节点 ${n.node} 应有毫秒时间戳，实际 ${n.time}`);
  }
});

test('API-F05 详情字段完整（站点/机柜/承诺还车/师傅）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'F05 字段' });
  await driveChain(orderNo, 'TO_STATION');
  const detail = await getOrder(orderNo);
  ok(detail, 'F05');
  truthy(detail, 'data.siteName', 'F05 站点名');
  truthy(detail, 'data.cabinetName', 'F05 柜机名');
  truthy(detail, 'data.promiseReturnTime', 'F05 承诺还车时间');
  // 派单后应有师傅快照（T13：workerName 快照，C 端不 join 师傅表）
  truthy(detail, 'data.workerName', 'F05 接单师傅');
  // 金额口径：实付 = 原价 - 折扣（未用券时相等）
  assert.equal(detail.data.payAmount, detail.data.originAmount - detail.data.discountAmount, 'F05: 实付应 = 原价-折扣');
});

test('API-F06 详情-越权查他人订单', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'F06 越权' });
  // 夹具：dev 单会员（任何 code 登录都是 dev-openid），接口层造不出他人订单，
  // 用 sidecar 造第二个会员并把本单归属改过去（只改 member_id，不动状态）
  setOrderOwner(orderNo, ensureOtherMember());
  const res = await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token });
  assert.notEqual(res.code, 0, `F06: 不应能查他人订单，实际 code=${res.code}`);
  // 口径：不得泄漏「订单存在」以外的信息，实测返回「订单不存在」
  console.log(`  [F06] 实际 code=${res.code} msg=${res.msg}`);
});

test('API-F07 订单按钮（mainAction 按状态返回，前端不推断）', async () => {
  const s = await getSession();
  // WAIT_PAY → PAY；FINISHED → REORDER（2026-10-24 实测确认）
  const waitPay = await createOrder({ remark: 'F07 按钮' });
  ok(waitPay.res, 'F07 造单');
  const d1 = await getOrder(waitPay.orderNo);
  ok(d1, 'F07 待支付详情');
  assert.equal(d1.data.mainAction.action, 'PAY', `F07: WAIT_PAY 主按钮应为 PAY，实际 ${d1.data.mainAction?.action}`);

  const finished = await createOrder({ remark: 'F07 按钮2' });
  ok(finished.res, 'F07 造单2');
  await driveChain(finished.orderNo, 'FINISHED');
  const d2 = await getOrder(finished.orderNo);
  ok(d2, 'F07 已完成详情');
  assert.equal(d2.data.mainAction.action, 'REORDER', `F07: FINISHED 主按钮应为 REORDER，实际 ${d2.data.mainAction?.action}`);
  assert.equal(d2.data.mainAction.enabled, true, 'F07: 按钮应可点');
});

test('API-F08 分页参数', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/orders', { token: s.token, query: { tab: 'ALL', pageNum: 1, pageSize: 2 } });
  ok(res, 'F08');
  assert.equal(res.data.pageNum, 1, 'F08: pageNum 回显');
  assert.equal(res.data.pageSize, 2, 'F08: pageSize 回显');
  assert.ok((res.data.list || []).length <= 2, 'F08: 返回条数不应超过 pageSize');
  assert.ok(Number.isInteger(res.data.total), 'F08: total 应为整数');
});

/**
 * API-C 下单（14 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》C 段；技术方案 3.4 三个并发热点。
 * 造数全走接口；C04 操控 Redis 产能 key（withCapacityZero 自动恢复）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { api } from '../lib/http.js';
import { ok, bizCode, field, truthy, amountIs, tsIs } from '../lib/assert.js';
import {
  getSession, createOrder, getOrder, mockPay, freeCabinet,
  uploadPhoto, receiveAnyCoupon, cleanupCreated, markFinished,
} from '../lib/factory.js';
import { withCapacityZero, SIDECAR_ENABLED, mediaOrderNo, couponStatus, orderStatusInDb } from '../lib/sidecar.js';
import { todayStr } from '../lib/util.js';

// 每条用例后取消其创建的订单（失败也执行）：dev 仅数个空闲格口，不即时清理会 B2003 连坐
afterEach(() => cleanupCreated());

test('API-C01 下单-正向（含格口预占与产能扣减）', async () => {
  const s = await getSession();
  const cab = freeCabinet(s.cabinets);
  assert.ok(cab, 'C01: 没有有空闲格口的机柜，无法下单');
  const freeBefore = cab.slotFree;

  const { res, orderNo } = await createOrder();
  ok(res, 'C01');
  field(res, 'data.status', 'WAIT_PAY', 'C01 初始状态');
  amountIs(res.data.payAmount, 'C01 payAmount');
  field(res, 'data.couponDiscount', 0, 'C01 未用券抵扣应为 0');
  tsIs(res.data.payExpireAt, 'C01 支付截止时间');

  // 副作用：格口被预占（free -1）
  const after = (await api('GET', '/api/v1/cabinets', { token: s.token })).data.find((c) => c.cabinetId === cab.cabinetId);
  assert.equal(after.slotFree, freeBefore - 1, `C01: 格口应被预占，free ${freeBefore} → ${after.slotFree}`);
  // 副作用：DB 状态一致（只读核对）
  if (SIDECAR_ENABLED) assert.equal(orderStatusInDb(orderNo), 'WAIT_PAY', 'C01: DB 状态应为 WAIT_PAY');
});

test('API-C02 下单-同幂等键重复提交返回同一单', async () => {
  const key = randomUUID();
  const first = await createOrder({}, key);
  ok(first.res, 'C02 首次');
  const second = await createOrder({}, key);
  ok(second.res, 'C02 重复提交也应成功并返回首次结果');
  assert.equal(second.orderNo, first.orderNo, `C02: 同幂等键应返回同一订单，首次 ${first.orderNo}，二次 ${second.orderNo}`);
});

test('API-C03 下单-同内容不同幂等键生成新单', async () => {
  const a = await createOrder({ remark: 'C03-a' });
  const b = await createOrder({ remark: 'C03-a' });
  ok(a.res, 'C03 第一单');
  ok(b.res, 'C03 第二单');
  assert.notEqual(a.orderNo, b.orderNo, 'C03: 不同幂等键应生成不同订单');
});

test('API-C04 下单-产能为 0 拒绝（A0006）', async () => {
  const res = await withCapacityZero(() => createOrder({ remark: 'C04 超产能' }));
  bizCode(res.res, 10006, 'C04 产能为 0');
  assert.equal(res.orderNo, null, 'C04: 拒绝时不应产生订单');
});

test('API-C05 下单-无空闲格口拒绝（B2003）', async (t) => {
  const s = await getSession();
  // 重新拉取机柜：会话缓存的 slotFree 会因本文件前序用例下单而过期
  const cabinets = (await api('GET', '/api/v1/cabinets', { token: s.token })).data;
  const cab = freeCabinet(cabinets);
  assert.ok(cab, 'C05: 没有有空闲格口的机柜');
  if (cab.slotFree <= 0) t.skip('当前无空闲格口');

  // 占满该柜全部空闲格口
  const fillers = [];
  for (let i = 0; i < cab.slotFree; i++) {
    const r = await createOrder({ cabinetId: cab.cabinetId, remark: `C05 占位${i}` });
    ok(r.res, `C05 占位单 ${i + 1}/${cab.slotFree}`);
    fillers.push(r.orderNo);
  }
  // 第 N+1 单应被拒
  const overflow = await createOrder({ cabinetId: cab.cabinetId, remark: 'C05 溢出' });
  bizCode(overflow.res, 22003, 'C05 无空闲格口');
  assert.equal(overflow.orderNo, null, 'C05: 拒绝时不应产生订单');

  // 本用例即时清理占位单（afterEach 也会兜底，这里显式取消并标记，尽快释放格口）
  for (const orderNo of fillers) {
    const r = await api('POST', `/api/v1/orders/${orderNo}/cancel`, {
      token: s.token, body: { reason: '[AUTOTEST] C05 占位清理' },
    });
    if (r.code !== 0) console.error(`  [C05 清理] ${orderNo} code=${r.code} ${r.msg}`);
    markFinished(orderNo);
  }
});

test('API-C06 下单-缺必填参数逐项拒绝', async () => {
  const s = await getSession();
  const required = ['serviceId', 'vehicleId', 'cabinetId', 'appointDate', 'appointTime', 'agreed'];
  const actuals = [];
  for (const missing of required) {
    const body = {
      serviceId: s.service.serviceId,
      vehicleId: s.vehicleId,
      cabinetId: freeCabinet(s.cabinets)?.cabinetId,
      appointDate: todayStr(),
      appointTime: '19:00',
      agreed: true,
    };
    delete body[missing];
    const res = await api('POST', '/api/v1/orders', {
      token: s.token, body, headers: { 'Idempotency-Key': randomUUID() },
    });
    assert.notEqual(res.code, 0, `C06: 缺 ${missing} 不应下单成功，实际 code=${res.code}`);
    actuals.push(`${missing}→${res.code}`);
  }
  console.log('  [C06] 实际错误码:', actuals.join(' '));
});

test('API-C07 下单-未同意协议拒绝', async () => {
  const res = await createOrder({ agreed: false, remark: 'C07 未同意协议' });
  assert.notEqual(res.res.code, 0, `C07: agreed=false 不应下单成功，实际 code=${res.res.code} msg=${res.res.msg}`);
  console.log(`  [C07] 实际 code=${res.res.code} msg=${res.res.msg}`);
});

test('API-C08 下单-停车照超 6 张拒绝', async () => {
  const fileIds = [];
  for (let i = 0; i < 7; i++) fileIds.push(await uploadPhoto('PARK'));
  const res = await createOrder({ parkPhotoFileIds: fileIds, remark: 'C08 超量照片' });
  assert.notEqual(res.res.code, 0, `C08: 7 张停车照不应下单成功，实际 code=${res.res.code}`);
  console.log(`  [C08] 实际 code=${res.res.code} msg=${res.res.msg}`);
});

test('API-C09 下单-停车照归属回填（副作用断言）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const fileId = await uploadPhoto('PARK'); // 无订单号上传
  const { res, orderNo } = await createOrder({ parkPhotoFileIds: [fileId], remark: 'C09 照片绑定' });
  ok(res, 'C09');
  const bound = mediaOrderNo(fileId);
  assert.equal(bound, orderNo, `C09: 影像应回填本订单号，DB 中 order_no=${bound}，实际订单=${orderNo}`);
});

test('API-C10 下单-使用满减券抵扣', async (t) => {
  const s = await getSession();
  const coupon = await receiveAnyCoupon(3900);
  if (!coupon) t.skip('没有可领且门槛 ≤3900 的券模板');

  const { res } = await createOrder({ couponUserId: coupon.couponUserId, remark: 'C10 用券' });
  ok(res, 'C10');
  amountIs(res.data.couponDiscount, 'C10 抵扣额');
  assert.ok(res.data.couponDiscount > 0, `C10: 应发生抵扣，实际 couponDiscount=${res.data.couponDiscount}`);
  assert.equal(
    res.data.payAmount,
    3900 - res.data.couponDiscount,
    `C10: 实付应为 3900-抵扣=${3900 - res.data.couponDiscount}，实际 ${res.data.payAmount}`
  );
  if (SIDECAR_ENABLED) {
    assert.equal(couponStatus(coupon.couponUserId), 'USED', 'C10: 券应核销为 USED');
  }
});

test('API-C11 下单-优惠券不可用拒绝（C1003）', async () => {
  const res = await createOrder({ couponUserId: 999999999, remark: 'C11 假券' });
  bizCode(res.res, 31003, 'C11 优惠券不可用');
  assert.equal(res.orderNo, null, 'C11: 拒绝时不应产生订单（不占格口不扣产能）');
});

test('API-C12 最优券计算', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/coupons/best', { token: s.token, query: { amount: 3900 } });
  ok(res, 'C12');
  if (res.data) {
    // 实际返回结构：{ couponUserId, discountAmount }（已按 2026-10-24 实测校准）
    assert.ok(res.data.couponUserId, `C12: 有最优券时应带 couponUserId，实际 ${JSON.stringify(res.data)}`);
    assert.ok(res.data.discountAmount > 0, `C12: discountAmount 应 > 0，实际 ${res.data.discountAmount}`);
    console.log(`  [C12] 最优券: couponUserId=${res.data.couponUserId} discount=${res.data.discountAmount}`);
  } else {
    console.log('  [C12] 当前无可用券，返回空（符合预期分支）');
  }
});

test('API-C13 下单-预约过去时间拒绝', async () => {
  const res = await createOrder({ appointDate: '2020-01-01', remark: 'C13 过去时间' });
  assert.notEqual(res.res.code, 0, `C13: 过去预约时间不应成功，实际 code=${res.res.code}`);
  console.log(`  [C13] 实际 code=${res.res.code} msg=${res.res.msg}`);
});

test('API-C14 下单-站点/机柜信息一致（详情回查）', async () => {
  const s = await getSession();
  const cab = freeCabinet(s.cabinets);
  assert.ok(cab, 'C14: 没有有空闲格口的机柜');
  const { orderNo } = await createOrder({ cabinetId: cab.cabinetId, remark: 'C14 信息一致' });
  const detail = await getOrder(orderNo);
  ok(detail, 'C14 详情');
  truthy(detail, 'data.siteName', 'C14 站点名');
  assert.ok(
    !detail.data.cabinetName || detail.data.cabinetName === cab.name,
    `C14: 柜机名应一致，期望 ${cab.name}，实际 ${detail.data.cabinetName}`
  );
});

test('API-C15 支付前订单可取消（为 C05 占位单预演清理路径）', async () => {
  // 占位单由 after() 统一取消；这里只验证取消接口对 WAIT_PAY 可用（E 段会系统性覆盖）
  const { orderNo } = await createOrder({ remark: 'C15 取消路径' });
  const s = await getSession();
  const res = await api('POST', `/api/v1/orders/${orderNo}/cancel`, {
    token: s.token, body: { reason: '[AUTOTEST] C15 验证取消' },
  });
  ok(res, 'C15');
  markFinished(orderNo);
  const detail = await getOrder(orderNo);
  field(detail, 'data.status', 'CANCELED', 'C15 取消后状态');
});

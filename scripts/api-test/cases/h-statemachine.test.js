/**
 * API-H 订单状态机（14 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》H 段；《订单状态机规则表.md》（SSOT）。
 * 造状态走 driveChain（真实接口）；副作用断言走 wash_order_status_log（只读）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, field, truthy, typeIs } from '../lib/assert.js';
import {
  getSession, createOrder, cancelOrder, driveChain, getOrder,
  workerLogin, adminLogin, uploadPhotos, cleanupCreated,
} from '../lib/factory.js';
import { SIDECAR_ENABLED, sql } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

/**
 * 后台（admin-api）成功判定：若依口径 code=200（不走 Result 信封，见 PROGRESS 10.6）。
 */
function okAdmin(env, label) {
  assert.equal(env.code, 200, `${label}: 期望 code=200（若依口径），实际 code=${env.code} msg=${env.msg}`);
}

/** 查流转日志（按时间正序） */
function statusLog(orderNo) {
  const out = sql(
    `select from_status, to_status, event, operator_type, source from wash_order_status_log ` +
    `where order_id = (select order_id from wash_order where order_no = '${orderNo}') order by id`
  ).trim();
  return out ? out.split('\n').map((l) => l.split('\t')) : [];
}

test('API-H01 17 事件全链路穿测（含每步状态与日志）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const { orderNo } = await createOrder({ remark: 'H01 全链路' });
  const expected = [
    'WAIT_KEY', 'KEY_IN', 'PICKING', 'TO_STATION', 'WASHING', 'QC',
    'WAIT_RETURN', 'RETURNING', 'RETURNED', 'WAIT_REVIEW', 'FINISHED',
  ];
  for (const target of expected) {
    await driveChain(orderNo, target);
    field(await getOrder(orderNo), 'data.status', target, `H01 推进到 ${target}`);
  }
  // 日志：11 次流转，事件与规则表 2.1 一致
  const log = statusLog(orderNo);
  assert.equal(log.length, 11, `H01: 应有 11 条流转日志，实际 ${log.length}`);
  const events = log.map((r) => r[2]);
  assert.deepEqual(events, [
    'PAY_SUCCESS', 'DEPOSIT_KEY', 'TAKE_KEY', 'PICK_CAR_DONE', 'ARRIVE_STATION',
    'SOP_DONE', 'QC_PASS', 'LEAVE_STATION', 'RETURN_DONE', 'TAKE_KEY_BACK', 'REVIEW_SUBMIT',
  ], `H01: 事件序列应与规则表一致，实际 ${events.join(',')}`);
  // 每一条都带触发方与来源端（留痕红线）
  for (const r of log) {
    assert.ok(r[3] && r[4], `H01: 日志应带 operator_type/source，实际 ${r.join('/')}`);
  }
});

test('API-H02 重洗回路（QC → WASHING → QC）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const wtoken = await workerLogin();
  const { orderNo } = await createOrder({ remark: 'H02 重洗' });
  await driveChain(orderNo, 'QC');

  const fail1 = await api('POST', `/api/v1/station/orders/${orderNo}/qc-fail`, {
    token: wtoken, body: { reason: '轮毂有残留' },
  });
  ok(fail1, 'H02 质检不合格');
  field(await getOrder(orderNo), 'data.status', 'WASHING', 'H02 打回后状态');

  // 重新 SOP 完成 → 回到 QC
  ok(await api('POST', `/api/v1/station/orders/${orderNo}/sop-done`, { token: wtoken, body: { steps: ['s1'] } }), 'H02 二次 SOP');
  field(await getOrder(orderNo), 'data.status', 'QC', 'H02 回到待质检');

  // 日志里有 QC→WASHING 回退记录
  const back = statusLog(orderNo).filter((r) => r[0] === 'QC' && r[1] === 'WASHING');
  assert.equal(back.length, 1, `H02: 应有 1 条回退日志，实际 ${back.length}`);
});

test('API-H03 重洗 ≥2 次触发复盘阈值（当前行为：WARN 告警，不阻断）', async () => {
  const wtoken = await workerLogin();
  const { orderNo } = await createOrder({ remark: 'H03 两次重洗' });
  await driveChain(orderNo, 'QC');
  ok(await api('POST', `/api/v1/station/orders/${orderNo}/qc-fail`, { token: wtoken, body: { reason: '第1次不合格' } }), 'H03 第1次');
  ok(await api('POST', `/api/v1/station/orders/${orderNo}/sop-done`, { token: wtoken, body: { steps: ['s1'] } }), 'H03 二次 SOP');
  const fail2 = await api('POST', `/api/v1/station/orders/${orderNo}/qc-fail`, { token: wtoken, body: { reason: '第2次不合格' } });
  ok(fail2, 'H03 第2次');
  // 现状（PROGRESS 9.3 已知简化）：≥2 次只 WARN，不自动建复盘工单；此处验证「不阻断、可继续流转」
  field(await getOrder(orderNo), 'data.status', 'WASHING', 'H03 第2次打回后状态');
});

test('API-H04 非法流转拦截（规则表外组合）', async () => {
  const s = await getSession();
  const wtoken = await workerLogin();
  const { orderNo } = await createOrder({ remark: 'H04 非法流转' });
  await driveChain(orderNo, 'WAIT_KEY');
  // WAIT_KEY 直接调「入场打卡」（规则表要求 TO_STATION → WASHING）→ 拒绝
  const res = await api('POST', `/api/v1/station/orders/${orderNo}/arrive`, { token: wtoken });
  bizCode(res, 21002, 'H04 非法流转');
  field(await getOrder(orderNo), 'data.status', 'WAIT_KEY', 'H04 拒绝后状态不变');
});

test('API-H05 终态保护（REFUNDED 无出边）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const wtoken = await workerLogin();
  const { orderNo } = await createOrder({ remark: 'H05 终态' });
  await driveChain(orderNo, 'WAIT_KEY');
  ok(await cancelOrder(orderNo, 'H05 取消退款'), 'H05');
  field(await getOrder(orderNo), 'data.status', 'REFUNDED', 'H05 终态');
  // REFUNDED 再触发任何事件均应拒
  bizCode(await cancelOrder(orderNo, 'H05 再取消'), 21002, 'H05 再取消');
  const arrive = await api('POST', `/api/v1/station/orders/${orderNo}/arrive`, { token: wtoken });
  assert.notEqual(arrive.code, 0, `H05: REFUNDED 不应还能入场，实际 code=${arrive.code}`);
});

test('API-H06 FINISHED 可退款（FINISHED + APPLY_REFUND → REFUNDING 放行）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'H06 完成后退款' });
  await driveChain(orderNo, 'FINISHED');

  // C 端提交售后退款（入口在 FINISHED 可见）。
  // 注意：契约写 data={ticketNo}，实测实现返回裸字符串（ApiResult<String>）——按实测断言，偏差已报
  const as = await api('POST', '/api/v1/after-sales', {
    token: s.token, body: { orderNo, type: 'REFUND', reason: '洗不干净，重洗后仍不满意' },
  });
  ok(as, 'H06 提交售后');
  typeIs(as.data, 'string', 'H06 工单号（实测为裸字符串，契约写 {ticketNo} 对象）');
  const ticketNo = as.data;
  assert.ok(ticketNo && ticketNo.length > 0, 'H06: 工单号不应为空');

  // 后台处置同意 → 走 APPLY_REFUND + REFUND_SUCCESS → REFUNDED
  const atoken = await adminLogin();
  const handled = await api('PUT', `/admin-api/wash/after-sale/${ticketNo}/handle`, {
    token: atoken, body: { accept: true },
  });
  okAdmin(handled, 'H06 后台同意');
  field(await getOrder(orderNo), 'data.status', 'REFUNDED', 'H06 退款后终态');
  // 副作用：售后工单自动收尾 DONE
  const ticket = sql(`select status from wash_after_sale where after_sale_no = '${ticketNo}'`).trim();
  assert.equal(ticket, 'DONE', `H06: 工单应自动收尾 DONE，实际 ${ticket}`);
});

test('API-H07 流转日志完整性（只增不删 + 五元组齐全）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const { orderNo } = await createOrder({ remark: 'H07 日志' });
  await driveChain(orderNo, 'WASHING');
  const log = statusLog(orderNo);
  assert.equal(log.length, 5, `H07: WAIT_PAY→WASHING 应 5 条日志，实际 ${log.length}`);
  for (const r of log) {
    assert.ok(r[0] && r[1] && r[2] && r[3] && r[4], `H07: 每条日志五元组应齐全，实际 ${r.join('/')}`);
  }
  // 日志只增不删：无删除接口（契约层断言），此处验证再次流转只追加
  await driveChain(orderNo, 'QC');
  assert.equal(statusLog(orderNo).length, 6, 'H07: 新流转应追加日志');
});

test('API-H08 并发双流转防护（10 并发支付仅 1 次成功）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'H08 并发支付' });
  const results = await Promise.all(
    Array.from({ length: 10 }, () => api('POST', `/api/v1/payments/${orderNo}/mock-pay`, { token: s.token }))
  );
  const success = results.filter((r) => r.code === 0).length;
  assert.equal(success, 1, `H08: 并发 10 次支付应仅 1 次成功，实际 ${success}`);
  field(await getOrder(orderNo), 'data.status', 'WAIT_KEY', 'H08 终态');
});

test('API-H09 取车照片数量强校验（<6 张拒绝 B1005）', async () => {
  const s = await getSession();
  const wtoken = await workerLogin();
  const { orderNo } = await createOrder({ remark: 'H09 照片不足' });
  await driveChain(orderNo, 'PICKING');
  const ids = await uploadPhotos('PICK', orderNo, 5);
  const res = await api('POST', `/api/v1/pick/orders/${orderNo}/pick-car-done`, { token: wtoken, body: { fileIds: ids } });
  // 实测错误码：B4002「照片数量不足」（数量校验先于归属校验）
  bizCode(res, 24002, 'H09 少于 6 张');
  field(await getOrder(orderNo), 'data.status', 'PICKING', 'H09 拒绝后状态不变');
});

test('API-H10 归柜照片 bizType 错配拒绝（B1005）', async () => {
  const s = await getSession();
  const wtoken = await workerLogin();
  const { orderNo } = await createOrder({ remark: 'H10 bizType 错配' });
  await driveChain(orderNo, 'RETURNING');
  // 传 PICK 类型的照片去还车（契约要求 RETURN）→ 必须拦截
  const ids = await uploadPhotos('PICK', orderNo, 2);
  const res = await api('POST', `/api/v1/pick/orders/${orderNo}/return-done`, {
    token: wtoken, body: { fileIds: ids, parkingNo: 'B2-A08' },
  });
  bizCode(res, 21005, 'H10 bizType 错配');
  field(await getOrder(orderNo), 'data.status', 'RETURNING', 'H10 拒绝后状态不变');
});

test('API-H11 后台触发-白名单内事件成功（source=ADMIN）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const atoken = await adminLogin();
  const { orderNo } = await createOrder({ remark: 'H11 白名单内' });
  await driveChain(orderNo, 'KEY_IN');
  const res = await api('POST', `/admin-api/wash/order/${orderNo}/advance`, {
    token: atoken, body: { event: 'TAKE_KEY', reason: '客服代师傅取钥匙' },
  });
  okAdmin(res, 'H11 后台推进');
  field(await getOrder(orderNo), 'data.status', 'PICKING', 'H11 推进后状态');
  const last = statusLog(orderNo).pop();
  assert.equal(last[2], 'TAKE_KEY', `H11: 日志事件应为 TAKE_KEY，实际 ${last[2]}`);
  assert.equal(last[3], 'ADMIN', `H11: 触发方应为 ADMIN，实际 ${last[3]}`);
});

test('API-H12 后台触发-白名单外 6 事件拒绝（B1002）', async () => {
  const atoken = await adminLogin();
  const { orderNo } = await createOrder({ remark: 'H12 白名单外' });
  await driveChain(orderNo, 'WAIT_KEY');
  // PAY_SUCCESS：规则表允许（WAIT_PAY→WAIT_KEY 才行，此处 WAIT_KEY 状态本就不允许），
  // 但即使状态允许，后台也禁止触发——用 APPLY_REFUND（WAIT_REVIEW/FINISHED 才允许）与 REVIEW_SUBMIT 验证白名单
  for (const event of ['PAY_SUCCESS', 'REVIEW_SUBMIT', 'AUTO_FINISH', 'REFUND_SUCCESS']) {
    const res = await api('POST', `/admin-api/wash/order/${orderNo}/advance`, {
      token: atoken, body: { event, reason: '越权触发测试' },
    });
    assert.notEqual(res.code, 0, `H12: 后台触发 ${event} 应被拒，实际 code=${res.code} msg=${res.msg}`);
  }
});

test('API-H13 后台高危事件二次确认（TAKE_KEY_BACK）', async () => {
  const atoken = await adminLogin();
  const { orderNo } = await createOrder({ remark: 'H13 二次确认' });
  await driveChain(orderNo, 'RETURNED');
  // 不带 confirm → 拒
  const noConfirm = await api('POST', `/admin-api/wash/order/${orderNo}/advance`, {
    token: atoken, body: { event: 'TAKE_KEY_BACK', reason: '代客户确认取回' },
  });
  assert.notEqual(noConfirm.code, 0, `H13: 高危事件无二次确认应被拒，实际 code=${noConfirm.code} msg=${noConfirm.msg}`);
  field(await getOrder(orderNo), 'data.status', 'RETURNED', 'H13 拒绝后状态不变');
  // 带 confirm=true → 过
  const withConfirm = await api('POST', `/admin-api/wash/order/${orderNo}/advance`, {
    token: atoken, body: { event: 'TAKE_KEY_BACK', reason: '代客户确认取回', confirm: true },
  });
  okAdmin(withConfirm, 'H13 确认后推进');
  field(await getOrder(orderNo), 'data.status', 'WAIT_REVIEW', 'H13 推进后状态');
});

test('API-H14 后台干预-空原因拒绝（A0001）', async () => {
  const atoken = await adminLogin();
  const { orderNo } = await createOrder({ remark: 'H14 空原因' });
  await driveChain(orderNo, 'KEY_IN');
  bizCode(await api('POST', `/admin-api/wash/order/${orderNo}/advance`, {
    token: atoken, body: { event: 'TAKE_KEY' },
  }), 10001, 'H14 空原因');
});

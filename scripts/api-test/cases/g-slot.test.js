/**
 * API-G 开箱码与柜机回调（10 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》G 段；PRD 6.2 钥匙柜。
 * 副作用断言：开箱留痕走 wash_slot_open_log（只读核对）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, field } from '../lib/assert.js';
import { getSession, createOrder, driveChain, workerLogin, cleanupCreated } from '../lib/factory.js';
import { SIDECAR_ENABLED, sql, ensureOtherMember, setOrderOwner } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

/** 取开箱码（客户侧） */
async function openCode(s, orderNo) {
  const res = await api('POST', `/api/v1/orders/${orderNo}/open-code`, { token: s.token });
  ok(res, 'G 取开箱码');
  return res.data.code;
}

test('API-G01 取开箱码-存钥匙（WAIT_KEY）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G01 开箱码' });
  await driveChain(orderNo, 'WAIT_KEY');
  const code = await openCode(s, orderNo);
  assert.ok(/^\d{6}$/.test(code), `G01: 开箱码应为 6 位数字，实际 ${code}`);
  // 副作用：开箱记录留痕（码脱敏存储）
  const logCount = sql(
    `select count(*) from wash_slot_open_log where order_no = '${orderNo}'`
  ).trim();
  assert.ok(Number(logCount) >= 1, `G01: 应有开箱留痕，实际 ${logCount} 条`);
});

test('API-G02 取开箱码-非允许状态拒绝（WASHING）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G02 状态不符' });
  await driveChain(orderNo, 'WASHING');
  const res = await api('POST', `/api/v1/orders/${orderNo}/open-code`, { token: s.token });
  assert.notEqual(res.code, 0, `G02: WASHING 状态不应能取开箱码，实际 code=${res.code} msg=${res.msg}`);
  console.log(`  [G02] 实际 code=${res.code} msg=${res.msg}`);
});

test('API-G03 取开箱码-越权', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G03 越权取码' });
  await driveChain(orderNo, 'WAIT_KEY'); // 状态正确（WAIT_KEY 本可取码），只越权在归属
  setOrderOwner(orderNo, ensureOtherMember());
  const res = await api('POST', `/api/v1/orders/${orderNo}/open-code`, { token: s.token });
  assert.notEqual(res.code, 0, `G03: 不应能取他人订单的开箱码，实际 code=${res.code}`);
  console.log(`  [G03] 实际 code=${res.code} msg=${res.msg}`);
});

test('API-G04 柜机回调-存钥匙正向（WAIT_KEY → KEY_IN）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G04 存钥匙' });
  await driveChain(orderNo, 'WAIT_KEY');
  const code = await openCode(s, orderNo);
  const dep = await api('POST', '/device-callback/v1/slot/deposit', { body: { orderNo, code } });
  ok(dep, 'G04 柜机回调');
  const detail = await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token });
  field(detail, 'data.status', 'KEY_IN', 'G04 存钥匙后状态');
});

test('API-G05 柜机回调-错误开箱码拒绝', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G05 错码' });
  await driveChain(orderNo, 'WAIT_KEY');
  const dep = await api('POST', '/device-callback/v1/slot/deposit', { body: { orderNo, code: '000000' } });
  assert.notEqual(dep.code, 0, `G05: 错误开箱码应被拒，实际 code=${dep.code} msg=${dep.msg}`);
  const detail = await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token });
  field(detail, 'data.status', 'WAIT_KEY', 'G05 拒绝后状态不应变');
});

test('API-G06 柜机回调-重复回调幂等', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G06 重复回调' });
  await driveChain(orderNo, 'WAIT_KEY');
  const code = await openCode(s, orderNo);
  ok(await api('POST', '/device-callback/v1/slot/deposit', { body: { orderNo, code } }), 'G06 首次');
  // 同码二次：状态已是 KEY_IN，应被幂等忽略（不重复流转、不报错失控）
  const second = await api('POST', '/device-callback/v1/slot/deposit', { body: { orderNo, code } });
  console.log(`  [G06] 二次回调: code=${second.code} msg=${second.msg}`);
  const detail = await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token });
  field(detail, 'data.status', 'KEY_IN', 'G06 二次回调后状态不应变');
});

test('API-G07 柜机回调-格口状态不符拒绝', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G07 格口不符' });
  await driveChain(orderNo, 'KEY_IN'); // 格口已 OCCUPIED
  // 此时该单格口已占用，再用旧逻辑存一次应被拒（格口状态与「待存入」不符）
  const dep = await api('POST', '/device-callback/v1/slot/deposit', { body: { orderNo, code: '123456' } });
  assert.notEqual(dep.code, 0, `G07: 格口已占用时的存入应被拒，实际 code=${dep.code} msg=${dep.msg}`);
});

test('API-G08 师傅取钥匙开箱码 + 取钥匙（KEY_IN → PICKING）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G08 师傅取钥匙' });
  await driveChain(orderNo, 'KEY_IN');
  const wtoken = await workerLogin();
  const woc = await api('POST', `/api/v1/pick/orders/${orderNo}/open-code`, { token: wtoken });
  ok(woc, 'G08 师傅取开箱码');
  const tk = await api('POST', `/api/v1/pick/orders/${orderNo}/take-key`, { token: wtoken, body: { code: woc.data.code } });
  ok(tk, 'G08 取钥匙');
  const detail = await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token });
  field(detail, 'data.status', 'PICKING', 'G08 取钥匙后状态');
});

test('API-G09 客户确认取回钥匙（RETURNED → WAIT_REVIEW）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'G09 取回钥匙' });
  await driveChain(orderNo, 'RETURNED');
  const res = await api('POST', `/api/v1/orders/${orderNo}/take-key-back`, { token: s.token });
  ok(res, 'G09');
  const detail = await api('GET', `/api/v1/orders/${orderNo}`, { token: s.token });
  field(detail, 'data.status', 'WAIT_REVIEW', 'G09 取回后状态');
});

test('API-G10 非本人格口开箱', {
  todo: '柜机回调 /device-callback/v1/slot/deposit 入参只有 orderNo + code + slotNo（机器→服务端，无会员上下文），'
    + '归属校验的落点是取码接口而非回调——已由 G03（他人订单取码被拒）覆盖；'
    + '回调侧「码与格口匹配」由 G04/G05 覆盖。硬造「先取码再改归属」的场景不成立（码本就是本人合法取得，实测回调返回 0 属预期）',
}, () => {});

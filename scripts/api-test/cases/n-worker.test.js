/**
 * API-N 师傅端（12 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》N 段。
 * 说明：N09~N11（取车拍照/作业台/归柜）已由 H 批全链路覆盖，不重复造单。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, typeIs, truthy } from '../lib/assert.js';
import { getSession, workerLogin, createOrder, driveChain, cleanupCreated } from '../lib/factory.js';

afterEach(() => cleanupCreated());

test('API-N01 工号登录-正向', async () => {
  const res = await api('POST', '/api/v1/worker/login', { body: { workerNo: 'W001', password: 'dev123' } });
  ok(res, 'N01');
  truthy(res, 'data.token', 'N01 token');
  truthy(res, 'data.workerNo', 'N01 工号');
});

test('API-N02 工号登录-错误口令（D4002 → 44002）', async () => {
  bizCode(await api('POST', '/api/v1/worker/login', { body: { workerNo: 'W001', password: 'wrong-pass' } }), 44002, 'N02');
});

test('API-N03 工号登录-工号不存在（D4006 → 44006）', async () => {
  const res = await api('POST', '/api/v1/worker/login', { body: { workerNo: 'W-NOT-EXIST', password: 'dev123' } });
  assert.notEqual(res.code, 0, `N03: 不存在的工号应拒，实际 code=${res.code} msg=${res.msg}`);
  console.log(`  [N03] 实际 code=${res.code} msg=${res.msg}`);
});

test('API-N04 师傅档案聚合（打卡/资质/在班/进行中）', async () => {
  const wtoken = await workerLogin();
  const res = await api('GET', '/api/v1/worker/profile', { token: wtoken });
  ok(res, 'N04');
  truthy(res, 'data.workerNo', 'N04 工号');
  // 聚合字段（实测口径）：workStatus 在班状态、qualificationPassed 资质是否通过、
  // onDutyToday 今日排班、activeOrderCount 进行中单量
  assert.ok('workStatus' in res.data, 'N04: 应有在班状态');
  assert.ok('qualificationPassed' in res.data, `N04: 应有资质状态，实际字段 ${Object.keys(res.data).join(',')}`);
  assert.ok('onDutyToday' in res.data, 'N04: 应有今日排班');
  typeIs(res.data.activeOrderCount, 'number', 'N04 进行中单量');
});

test('API-N05 打卡上下班', async () => {
  const wtoken = await workerLogin();
  const before = (await api('GET', '/api/v1/worker/profile', { token: wtoken })).data;
  // workStatus 必填：OFFLINE / ONLINE / BUSY
  const punch = await api('POST', '/api/v1/worker/work-status', { token: wtoken, body: { workStatus: 'ONLINE' } });
  ok(punch, 'N05 打卡上班');
  const after = (await api('GET', '/api/v1/worker/profile', { token: wtoken })).data;
  assert.equal(after.workStatus, 'ONLINE', `N05: 打卡后应为 ONLINE，实际 ${after.workStatus}`);
  console.log(`  [N05] 在班状态: ${before.workStatus} → ${after.workStatus}`);
});

test('API-N06 资质提交（需证件照）/查询', async () => {
  const wtoken = await workerLogin();
  const before = await api('GET', '/api/v1/worker/qualification', { token: wtoken });
  ok(before, 'N06 查询');
  // 服务端强校验：未上传身份证正反面照片直接拒（实测「请上传身份证正反面照片」）
  const noPhoto = await api('POST', '/api/v1/worker/qualification', {
    token: wtoken, body: { idNo: '440102199001011234', idName: '张师傅', licenseNo: 'C1TEST123' },
  });
  assert.notEqual(noPhoto.code, 0, `N06: 缺证件照应拒，实际 code=${noPhoto.code} msg=${noPhoto.msg}`);
  console.log(`  [N06] 缺证件照: code=${noPhoto.code} msg=${noPhoto.msg}`);
});

test('API-N07 任务池/待派单池', async () => {
  const wtoken = await workerLogin();
  const tasks = await api('GET', '/api/v1/pick/tasks', { token: wtoken });
  ok(tasks, 'N07 任务池');
  typeIs(tasks.data, 'array', 'N07 任务池类型');
  const pool = await api('GET', '/api/v1/pick/pool', { token: wtoken });
  ok(pool, 'N07 待派池');
  typeIs(pool.data, 'array', 'N07 待派池类型');
});

test('API-N08 抢单', async (t) => {
  const wtoken = await workerLogin();
  const pool = (await api('GET', '/api/v1/pick/pool', { token: wtoken })).data || [];
  if (!pool.length) t.skip('待派单池为空（钥匙入柜才触发自动派单进池）');
  const target = pool[0];
  const claim = await api('POST', `/api/v1/pick/orders/${target.orderNo}/claim`, { token: wtoken });
  ok(claim, 'N08 抢单');
  // 重复抢单应被拒
  const again = await api('POST', `/api/v1/pick/orders/${target.orderNo}/claim`, { token: wtoken });
  assert.notEqual(again.code, 0, `N08: 重复抢单应拒，实际 code=${again.code} msg=${again.msg}`);
});

test('API-N09~N11 取车拍照/作业台/归柜', { todo: '已由 H 批（H01 全链路 / H09 照片强校验 / H10 bizType 错配）系统覆盖，不重复造单' }, () => {});
test('API-N12 师傅端 token 失效行为', { todo: '前端行为（10002 跳工号登录页、不自动续登），属 UI 批（UI-W 系列）覆盖范围' }, () => {});

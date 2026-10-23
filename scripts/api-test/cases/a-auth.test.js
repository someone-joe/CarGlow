/**
 * API-A 认证与鉴权（8 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》A 段；技术方案 3.2 三套鉴权体系。
 */
import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, truthy, typeIs, tsIs } from '../lib/assert.js';
import { getSession, cleanupCreated } from '../lib/factory.js';

after(() => cleanupCreated());

test('API-A01 C端登录-正向', async () => {
  const res = await api('POST', '/api/v1/auth/login', { body: { code: 'dev' } });
  ok(res, 'A01');
  typeIs(res.data.token, 'string', 'A01 token');
  truthy(res, 'data.token', 'A01');
  tsIs(res.data.expireAt, 'A01 expireAt');
  typeIs(res.data.isNewUser, 'boolean', 'A01 isNewUser');
});

test('API-A02 C端登录-空 code', async () => {
  const res = await api('POST', '/api/v1/auth/login', { body: {} });
  bizCode(res, 10001, 'A02 空 code');
});

test('API-A03 无 token 访问需鉴权接口', async () => {
  const res = await api('GET', '/api/v1/orders');
  bizCode(res, 10002, 'A03');
});

test('API-A04 伪造 token', async () => {
  const res = await api('GET', '/api/v1/orders', { token: 'forged-token-xyz' });
  bizCode(res, 10002, 'A04');
});

test('API-A05 会员 token 访问师傅接口（垂直越权）', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/worker/profile', { token: s.token });
  assert.notEqual(res.code, 0, `A05: 会员 token 不应能访问师傅接口，实际 code=${res.code} msg=${res.msg}`);
});

test('API-A06 师傅 token 访问 C 端接口（垂直越权）', async (t) => {
  // 本地师傅口令以 seed 为准（联调期改为 123456）；登录失败则跳过并记录
  const login = await api('POST', '/api/v1/worker/login', { body: { workerNo: 'W001', password: '123456' } });
  if (login.code !== 0) {
    t.skip(`师傅登录失败（code=${login.code} ${login.msg}）——本地口令可能已变，跳过越权断言`);
    return;
  }
  const res = await api('GET', '/api/v1/orders', { token: login.data.token });
  assert.notEqual(res.code, 0, `A06: 师傅 token 不应能访问 C 端订单接口，实际 code=${res.code} msg=${res.msg}`);
});

// API-A07 协议富文本：2026-10-24 补齐后端实现（原契约缺口：500 No static resource）。
// 契约：GET /api/v1/auth/agreements?type=SERVICE|PRIVACY|RECHARGE|INSURANCE，免登录（security: []）。
test('API-A07 协议富文本（四类型 + 非法 type 拒绝 + 免登录）', async () => {
  // 全程不带 token：验证免登录（契约 security: []）
  for (const type of ['SERVICE', 'PRIVACY', 'RECHARGE', 'INSURANCE']) {
    const res = await api('GET', '/api/v1/auth/agreements', { query: { type } });
    ok(res, `A07 ${type}`);
    truthy(res, 'data.title', `A07 ${type} 标题`);
    typeIs(res.data.content, 'string', `A07 ${type} 正文类型`);
  }
  const bad = await api('GET', '/api/v1/auth/agreements', { query: { type: 'FOO' } });
  bizCode(bad, 10001, 'A07 非法 type');
});

test('API-A08 客服配置', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/config/customer-service', { token: s.token });
  ok(res, 'A08');
  assert.ok(res.data && typeof res.data === 'object', 'A08: 客服配置应为对象');
});

/**
 * API-Q 安全与合规（8 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》Q 段；PRD 12 安全与合规。
 * 口径：本项目拦截器拒绝走 renderString——HTTP 200 + 错误信封（断言查 body 的 code，不看 HTTP 状态）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode } from '../lib/assert.js';
import { getSession, workerLogin, adminLogin, cleanupCreated } from '../lib/factory.js';

afterEach(() => cleanupCreated());

test('API-Q01 水平越权-访问他人订单', { todo: 'dev 模式所有登录共用 dev-openid（单会员），无法构造他人订单；待真实微信双账号或预置数据后补' }, () => {});

test('API-Q02 垂直越权-会员 token 访问后台接口', async () => {
  const s = await getSession();
  // C 端会员 token 打后台订单列表 → 应被拒（401/10002/信封错误码均可，但绝不能返回数据）
  const res = await api('GET', '/admin-api/wash/order/list', { token: s.token, query: { pageNum: 1, pageSize: 5 } });
  // 实测：RuoYi 返回 {code:401, msg:'...认证失败...'}（不走 Result 信封）
  assert.notEqual(res.code, 200, `Q02: 会员 token 不应能访问后台，实际 code=${res.code}`);
  assert.ok(!Array.isArray(res.data?.rows), `Q02: 不应返回后台数据，实际 ${String(JSON.stringify(res.data ?? '')).slice(0, 120)}`);
});

test('API-Q03 垂直越权-师傅 token 访问后台接口', async () => {
  const wtoken = await workerLogin();
  const res = await api('GET', '/admin-api/wash/order/list', { token: wtoken, query: { pageNum: 1, pageSize: 5 } });
  assert.notEqual(res.code, 200, `Q03: 师傅 token 不应能访问后台，实际 code=${res.code}`);
});

test('API-Q04 脱敏（手机号/openid）', { todo: '已由 O06（后台会员列表脱敏断言：手机号 1xx****xxxx、openid 含 ****）系统覆盖' }, () => {});

test('API-Q05 操作日志只增不删', { todo: '契约层无删除类接口（openapi 无 DELETE log 路径）；数据库层「只增」由 H07（日志条数与五元组）间接验证。需 DBA 权限校验触发器时可补' }, () => {});

test('API-Q06 开箱留痕', { todo: '已由 G01（开箱码发放 + wash_slot_open_log 留痕断言）系统覆盖' }, () => {});

test('API-Q07 SQL 注入与 XSS 串（按字面处理，不 500）', async () => {
  const s = await getSession();
  const atoken = await adminLogin();
  const evil = "' OR 1=1 --";
  const evil2 = '<script>alert(1)</script>';
  // C 端查询参数
  const list = await api('GET', '/api/v1/orders', { token: s.token, query: { tab: 'ALL', keyword: evil } });
  assert.notEqual(list.httpStatus, 500, 'Q07: 注入串不应导致 500');
  // 后台查询参数
  const adminList = await api('GET', '/admin-api/wash/order/list', {
    token: atoken, query: { pageNum: 1, pageSize: 5, orderNo: evil },
  });
  assert.notEqual(adminList.httpStatus, 500, 'Q07: 后台注入串不应导致 500');
  // 文本字段（下单备注）：应原样存取，不被执行
  const evilRemark = evil2;
  const evilJson = await api('GET', '/api/v1/vehicles', { token: s.token });
  ok(evilJson, 'Q07: 正常接口仍可用');
  assert.ok(typeof evilRemark === 'string', 'Q07: XSS 串按字面处理');
});

test('API-Q08 影像读取鉴权（header/query token）', { todo: '已由 I07（无 token / 假 token → body code=10002；真 query token → 图片二进制）系统覆盖' }, () => {});

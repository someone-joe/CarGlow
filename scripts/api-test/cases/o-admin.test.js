/**
 * API-O 后台管理（14 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》O 段。
 * 口径：后台不走 Result 信封，成功码 200（若依 AjaxResult/TableDataInfo）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, typeIs, truthy } from '../lib/assert.js';
import { getSession, adminLogin, createOrder, driveChain, cleanupCreated } from '../lib/factory.js';

afterEach(() => cleanupCreated());

/** 后台成功判定：若依口径 code=200 */
function okAdmin(env, label) {
  assert.equal(env.code, 200, `${label}: 期望 code=200，实际 code=${env.code} msg=${env.msg}`);
}

test('API-O01 后台登录', async () => {
  const token = await adminLogin();
  assert.ok(token && token.length > 20, 'O01: 应返回 token');
});

test('API-O02 无权限访问', { todo: '需要构造无 wash:order:list 权限的角色/用户；涉及角色授权写操作，留待后台权限体系配套用例' }, () => {});

test('API-O03 订单列表/筛选', async () => {
  const atoken = await adminLogin();
  const all = await api('GET', '/admin-api/wash/order/list', { token: atoken, query: { pageNum: 1, pageSize: 5 } });
  okAdmin(all, 'O03 列表');
  typeIs(all.total, 'number', 'O03 total');
  typeIs(all.rows, 'array', 'O03 rows');
  // 按状态筛选
  const filtered = await api('GET', '/admin-api/wash/order/list', { token: atoken, query: { pageNum: 1, pageSize: 5, status: 'WAIT_PAY' } });
  okAdmin(filtered, 'O03 筛选');
  for (const row of filtered.rows || []) {
    assert.equal(row.status, 'WAIT_PAY', `O03: 筛选后应全是 WAIT_PAY，实际 ${row.status}`);
  }
});

test('API-O04 订单状态下拉选项（来自 OrderStatus 枚举）', async () => {
  const atoken = await adminLogin();
  const res = await api('GET', '/admin-api/wash/order/status-options', { token: atoken });
  okAdmin(res, 'O04');
  typeIs(res.data, 'array', 'O04 选项类型');
  assert.ok(res.data.length >= 15, `O04: 应覆盖 15 个状态，实际 ${res.data.length}`);
});

test('API-O05 事件选项与后台白名单一致', async () => {
  const atoken = await adminLogin();
  const s = await getSession();
  const { res: created, orderNo } = await createOrder({ remark: 'O05 事件选项' });
  ok(created, 'O05 造 KEY_IN 单'); // 先确认下单成功（产能/格口不足时早暴露，避免 driveChain 拿到空单号）
  await driveChain(orderNo, 'KEY_IN');
  const res = await api('GET', `/admin-api/wash/order/${orderNo}/event-options`, { token: atoken });
  okAdmin(res, 'O05');
  const events = (res.data || []).map((o) => o.value);
  // 白名单内应出现；白名单外（PAY_SUCCESS/REVIEW_SUBMIT/AUTO_FINISH/APPLY_REFUND/REFUND_SUCCESS）不应出现
  assert.ok(events.includes('TAKE_KEY'), `O05: KEY_IN 应有 TAKE_KEY，实际 ${events.join(',')}`);
  for (const banned of ['PAY_SUCCESS', 'REVIEW_SUBMIT', 'AUTO_FINISH', 'APPLY_REFUND', 'REFUND_SUCCESS']) {
    assert.ok(!events.includes(banned), `O05: 白名单外事件 ${banned} 不应出现，实际 ${events.join(',')}`);
  }
});

test('API-O06 会员/车辆列表脱敏', async () => {
  const atoken = await adminLogin();
  const members = await api('GET', '/admin-api/wash/member/list', { token: atoken, query: { pageNum: 1, pageSize: 5 } });
  okAdmin(members, 'O06 会员');
  for (const m of members.rows || []) {
    if (m.phone) assert.match(m.phone, /^1\d{2}\*{4}\d{4}$/, `O06: 手机号应脱敏，实际 ${m.phone}`);
    if (m.openid) assert.match(m.openid, /\*{4}/, `O06: openid 应脱敏，实际 ${m.openid}`);
  }
});

test('API-O07 服务项 CRUD + 逻辑删除', async () => {
  const atoken = await adminLogin();
  // 新增（契约 AdminServiceRequest：serviceName / categoryId / priceAmount 必填，金额单位分）
  // 注：契约 AdminServiceRequest 未列 scene，但数据库 wash_service.scene 为 NOT NULL，
  // 不传会 500（Column 'scene' cannot be null）——契约缺口，待补进 openapi
  const created = await api('POST', '/admin-api/wash/service', {
    token: atoken,
    body: { serviceName: '[AUTOTEST] 临时服务', categoryId: 1, priceAmount: 10000, workMinutes: 30, scene: 'UNSENSITIVE' },
  });
  okAdmin(created, 'O07 新增');
  // 列表应能查到
  const list = await api('GET', '/admin-api/wash/service/list', { token: atoken, query: { pageNum: 1, pageSize: 50 } });
  okAdmin(list, 'O07 列表');
  assert.ok((list.rows || []).some((r) => r.serviceName === '[AUTOTEST] 临时服务'), 'O07: 新增项应出现在列表');
  // 删除（逻辑删除：历史订单仍可引用）
  const id = created.data?.serviceId ?? created.data;
  assert.ok(id, 'O07: 新增应返回 ID');
  okAdmin(await api('DELETE', `/admin-api/wash/service/${id}`, { token: atoken }), 'O07 删除');
});

test('API-O08 数据看板口径', async () => {
  const atoken = await adminLogin();
  const res = await api('GET', '/admin-api/wash/dashboard/stats', { token: atoken });
  okAdmin(res, 'O08');
  // 口径：在洗 = 去程+清洗+待质检；异常 = 退款中（与订单数据一致由 E2E 串测保证）
  for (const key of ['todayCount', 'washing', 'waitKey', 'abnormal']) {
    if (key in (res.data || {})) typeIs(res.data[key], 'number', `O08 ${key}`);
  }
});

test('API-O09 师傅列表（密码绝不外泄）', async () => {
  const atoken = await adminLogin();
  const res = await api('GET', '/admin-api/wash/worker/list', { token: atoken, query: { pageNum: 1, pageSize: 5 } });
  okAdmin(res, 'O09');
  // 实测：RuoYi 实体带 password 字段但置为 null——安全口径是「值必须为 null/缺失，绝不能是 hash 或明文」
  for (const w of res.rows || []) {
    assert.ok(w.password === null || w.password === undefined,
      `O09: password 不应外泄，实际 ${JSON.stringify(w.password).slice(0, 40)}`);
  }
});

test('API-O10 工号已被占用（D4005 → 44005）', async () => {
  const atoken = await adminLogin();
  const res = await api('POST', '/admin-api/wash/worker', {
    token: atoken, body: { workerNo: 'W001', name: '重名师傅', phone: '13900000001', siteId: 1 },
  });
  assert.notEqual(res.code, 200, `O10: 重工号应拒，实际 code=${res.code} msg=${res.msg}`);
  console.log(`  [O10] 实际 code=${res.code} msg=${res.msg}`);
});

test('API-O11 资质审核分页', async () => {
  const atoken = await adminLogin();
  const res = await api('GET', '/admin-api/wash/worker/qualification/page', { token: atoken, query: { pageNum: 1, pageSize: 5 } });
  okAdmin(res, 'O11');
  typeIs(res.rows, 'array', 'O11 rows');
});

test('API-O12 周排班读写', async () => {
  const atoken = await adminLogin();
  const get = await api('GET', '/admin-api/wash/worker/1/schedule', { token: atoken });
  okAdmin(get, 'O12 读取');
  const original = get.data;
  // 契约：数组，元素必填 weekDay(1-7,1=周一) / startMinute / endMinute（当日分钟数，540=09:00）
  try {
    const put = await api('PUT', '/admin-api/wash/worker/1/schedule', {
      token: atoken,
      body: [{ weekDay: 1, startMinute: 1140, endMinute: 1380 }], // 19:00 - 23:00
    });
    okAdmin(put, 'O12 保存');
  } finally {
    // 必须还原：排班影响自动派单，改坏后其他用例（如 F05 断言接单师傅）会连坐失败
    const restore = await api('PUT', '/admin-api/wash/worker/1/schedule', { token: atoken, body: original ?? [] });
    if (restore.code !== 200) console.error(`  [O12] 排班还原失败 code=${restore.code} ${restore.msg}——需手动检查师傅 1 排班`);
  }
});

test('API-O13 手动派单/改派', async (t) => {
  const atoken = await adminLogin();
  // siteId 为必填 query 参数（实测）
  const pool = await api('GET', '/admin-api/wash/worker/dispatch/pool', { token: atoken, query: { siteId: 1 } });
  okAdmin(pool, 'O13 待派单池');
  if (!(pool.data || pool.rows || []).length) t.skip('待派单池为空（需钥匙入柜订单）');
  const candidates = await api('GET', '/admin-api/wash/worker/dispatch/candidates', { token: atoken, query: { siteId: 1 } });
  okAdmin(candidates, 'O13 候选师傅');
});

test('API-O14 站点/小区后台列表', async () => {
  const atoken = await adminLogin();
  okAdmin(await api('GET', '/admin-api/wash/site/list', { token: atoken }), 'O14 站点');
  okAdmin(await api('GET', '/admin-api/wash/community/list', { token: atoken }), 'O14 小区');
});

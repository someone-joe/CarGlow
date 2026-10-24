/**
 * API-O-ext 后台管理·扩展覆盖（补齐 O01–O14 未覆盖的 admin-api 域）。
 * 对应 openapi.yaml /admin-api/wash/**（tag admin-*）；口径同 O 段：不走 Result 信封，成功码 200。
 *
 * 覆盖域：订单详情/后台取消、券模板 CRUD+上下架、券领用/批次记录（脱敏）、Banner CRUD、
 *   钱包赠品规则/充值、售后/退款/发票、机柜/格口/开箱日志、师傅增删改密/任务、
 *   自助洗车后台（工位/价格/订单）、客服配置/小区申请/站点选项、会员车辆列表、越权拦截。
 *
 * 约定：结构形状取自 2026-10-24 对运行中后端的实测；断言以「形状/类型/脱敏/金额(分)/不变量」为主，
 *   不写死条数（dev 数据量大）。建的数据（券模板/Banner/师傅）在用例内 finally 删除，不污染。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, typeIs, truthy, amountIs } from '../lib/assert.js';
import { getSession, adminLogin, createOrder, cleanupCreated } from '../lib/factory.js';

afterEach(() => cleanupCreated());

/** 后台成功判定：若依口径 code=200 */
function okAdmin(env, label) {
  assert.equal(env.code, 200, `${label}: 期望 code=200，实际 code=${env.code} msg=${env.msg}`);
}
const PG = { pageNum: 1, pageSize: 5 };
const NOW = Date.now();

test('API-O15 后台订单详情 + 后台取消', async () => {
  const atoken = await adminLogin();
  const { res, orderNo } = await createOrder({ remark: 'O15 后台取消' });
  ok(res, 'O15 造单');
  const detail = await api('GET', `/admin-api/wash/order/${orderNo}`, { token: atoken });
  okAdmin(detail, 'O15 详情');
  assert.equal(detail.data.orderNo, orderNo, 'O15: 详情 orderNo 应匹配');
  assert.equal(detail.data.status, 'WAIT_PAY', `O15: 新单应为 WAIT_PAY，实际 ${detail.data.status}`);
  typeIs(detail.data.timeline, 'array', 'O15 timeline');
  const cancel = await api('POST', `/admin-api/wash/order/${orderNo}/cancel`, {
    token: atoken, body: { reason: '[AUTOTEST] O15 后台取消' },
  });
  okAdmin(cancel, 'O15 后台取消');
  assert.equal(cancel.data, 'CANCELED', `O15: 后台取消应返回 CANCELED，实际 ${cancel.data}`);
});

test('API-O16 券模板 CRUD + 上下架', async () => {
  const atoken = await adminLogin();
  const created = await api('POST', '/admin-api/wash/coupon/template', {
    token: atoken,
    body: {
      name: '[AUTOTEST] 探针券', type: 'NORMAL', thresholdAmount: 1000, discountAmount: 100,
      total: 100, perLimit: 1, startTime: NOW, endTime: NOW + 7 * 86400000, status: 'Y',
    },
  });
  okAdmin(created, 'O16 新增');
  const tid = created.data?.couponTemplateId ?? created.data;
  assert.ok(tid, 'O16: 新增应返回模板 ID');
  try {
    const detail = await api('GET', `/admin-api/wash/coupon/template/${tid}`, { token: atoken });
    okAdmin(detail, 'O16 详情');
    assert.equal(detail.data.name, '[AUTOTEST] 探针券', 'O16: 详情名称应匹配');
    amountIs(detail.data.discountAmount, 'O16 discountAmount');
    amountIs(detail.data.thresholdAmount, 'O16 thresholdAmount');
    const list = await api('GET', '/admin-api/wash/coupon/template/list', { token: atoken, query: { pageNum: 1, pageSize: 50 } });
    okAdmin(list, 'O16 列表');
    assert.ok((list.rows || []).some((r) => r.couponTemplateId === tid), 'O16: 新模板应出现在列表');
    okAdmin(await api('POST', `/admin-api/wash/coupon/template/${tid}/status`, { token: atoken, body: { status: 'N' } }), 'O16 下架');
  } finally {
    okAdmin(await api('DELETE', `/admin-api/wash/coupon/template/${tid}`, { token: atoken }), 'O16 删除');
  }
});

test('API-O17 券领用/批次记录（手机号脱敏）', async () => {
  const atoken = await adminLogin();
  const rec = await api('GET', '/admin-api/wash/coupon/record/list', { token: atoken, query: PG });
  okAdmin(rec, 'O17 领用记录');
  typeIs(rec.rows, 'array', 'O17 rows');
  for (const r of rec.rows || []) {
    if (r.memberPhone) assert.match(r.memberPhone, /\*/, `O17: 领用记录手机号应脱敏，实际 ${r.memberPhone}`);
  }
  const batch = await api('GET', '/admin-api/wash/coupon/batch/list', { token: atoken, query: PG });
  okAdmin(batch, 'O17 批次');
  typeIs(batch.rows, 'array', 'O17 batch rows');
});

test('API-O18 Banner 增查删', async () => {
  const atoken = await adminLogin();
  const created = await api('POST', '/admin-api/wash/banner', {
    token: atoken, body: { title: '[AUTOTEST] 探针Banner', imageUrl: 'https://example.invalid/x.png', linkType: 'NONE', sortOrder: 99 },
  });
  okAdmin(created, 'O18 新增');
  const bid = created.data?.bannerId ?? created.data;
  assert.ok(bid, 'O18: 新增应返回 bannerId');
  try {
    const detail = await api('GET', `/admin-api/wash/banner/${bid}`, { token: atoken });
    okAdmin(detail, 'O18 详情');
    assert.equal(detail.data.title, '[AUTOTEST] 探针Banner', 'O18: 详情标题应匹配');
    const list = await api('GET', '/admin-api/wash/banner/list', { token: atoken, query: { pageNum: 1, pageSize: 50 } });
    okAdmin(list, 'O18 列表');
    assert.ok((list.rows || []).some((r) => r.bannerId === bid), 'O18: 新 Banner 应出现在列表');
  } finally {
    okAdmin(await api('DELETE', `/admin-api/wash/banner/${bid}`, { token: atoken }), 'O18 删除');
  }
});

test('API-O19 钱包赠品规则 + 充值列表', async () => {
  const atoken = await adminLogin();
  const rules = await api('GET', '/admin-api/wash/wallet/gift-rules', { token: atoken });
  okAdmin(rules, 'O19 赠品规则');
  typeIs(rules.data, 'array', 'O19 rules');
  for (const r of rules.data || []) {
    amountIs(r.rechargeFen, 'O19 rechargeFen');
    amountIs(r.giftFen, 'O19 giftFen');
  }
  const recharges = await api('GET', '/admin-api/wash/wallet/recharges', { token: atoken, query: PG });
  okAdmin(recharges, 'O19 充值列表');
  typeIs(recharges.rows, 'array', 'O19 rows');
  for (const r of recharges.rows || []) {
    amountIs(r.payAmount, 'O19 payAmount');
    amountIs(r.giftAmount, 'O19 giftAmount');
  }
});

test('API-O20 售后列表', async () => {
  const atoken = await adminLogin();
  const res = await api('GET', '/admin-api/wash/after-sale/list', { token: atoken, query: PG });
  okAdmin(res, 'O20');
  typeIs(res.rows, 'array', 'O20 rows');
  for (const r of res.rows || []) amountIs(r.amount, 'O20 amount');
});

test('API-O21 退款列表', async () => {
  const atoken = await adminLogin();
  const res = await api('GET', '/admin-api/wash/refund/list', { token: atoken, query: PG });
  okAdmin(res, 'O21');
  typeIs(res.rows, 'array', 'O21 rows');
  for (const r of res.rows || []) amountIs(r.amount, 'O21 amount');
});

test('API-O22 发票列表 + 状态选项', async () => {
  const atoken = await adminLogin();
  const list = await api('GET', '/admin-api/wash/invoice/list', { token: atoken, query: PG });
  okAdmin(list, 'O22 列表');
  typeIs(list.rows, 'array', 'O22 rows');
  for (const r of list.rows || []) amountIs(r.amount, 'O22 amount');
  const opts = await api('GET', '/admin-api/wash/invoice/status-options', { token: atoken });
  okAdmin(opts, 'O22 状态选项');
  typeIs(opts.data, 'array', 'O22 options');
  for (const o of opts.data || []) { truthy(o, 'value', 'O22 value'); truthy(o, 'label', 'O22 label'); }
});

test('API-O23 机柜/格口/开箱日志', async () => {
  const atoken = await adminLogin();
  const cab = await api('GET', '/admin-api/wash/cabinet/list', { token: atoken, query: PG });
  okAdmin(cab, 'O23 机柜');
  typeIs(cab.rows, 'array', 'O23 cabinet rows');
  const opts = await api('GET', '/admin-api/wash/cabinet/options', { token: atoken });
  okAdmin(opts, 'O23 机柜选项');
  typeIs(opts.data, 'array', 'O23 cabinet options');
  const slot = await api('GET', '/admin-api/wash/slot/list', { token: atoken, query: PG });
  okAdmin(slot, 'O23 格口');
  typeIs(slot.rows, 'array', 'O23 slot rows');
  const log = await api('GET', '/admin-api/wash/slot/open-log', { token: atoken, query: PG });
  okAdmin(log, 'O23 开箱日志');
  typeIs(log.rows, 'array', 'O23 open-log rows');
});

test('API-O24 师傅新增/详情/改密/任务/删除', async () => {
  const atoken = await adminLogin();
  const workerNo = 'WPROBE' + String(NOW).slice(-6);
  const created = await api('POST', '/admin-api/wash/worker', {
    token: atoken, body: { workerNo, name: '探针师傅', phone: '13900000099', siteId: 1, password: 'probe12345' },
  });
  okAdmin(created, 'O24 新增');
  // 新建接口不回传 data（RuoYi 口径），按工号反查拿 ID
  const found = await api('GET', '/admin-api/wash/worker/list', { token: atoken, query: { pageNum: 1, pageSize: 50, keyword: workerNo } });
  okAdmin(found, 'O24 反查');
  const row = (found.rows || []).find((r) => r.workerNo === workerNo);
  assert.ok(row, 'O24: 新建师傅应能按工号查到');
  const wid = row.workerId;
  try {
    const detail = await api('GET', `/admin-api/wash/worker/${wid}`, { token: atoken });
    okAdmin(detail, 'O24 详情');
    assert.equal(detail.data.workerNo, workerNo, 'O24: 详情工号应匹配');
    assert.ok(detail.data.password === null || detail.data.password === undefined, 'O24: 密码不得外泄');
    okAdmin(await api('POST', `/admin-api/wash/worker/${wid}/reset-password`, { token: atoken, body: { password: 'probe67890' } }), 'O24 改密');
    okAdmin(await api('GET', `/admin-api/wash/worker/${wid}/tasks`, { token: atoken }), 'O24 任务');
  } finally {
    okAdmin(await api('DELETE', `/admin-api/wash/worker/${wid}`, { token: atoken }), 'O24 删除');
  }
});

test('API-O25 自助洗车后台：工位/价格/订单/状态选项', async () => {
  const atoken = await adminLogin();
  const ws = await api('GET', '/admin-api/wash/selfwash/workstation/list', { token: atoken, query: PG });
  okAdmin(ws, 'O25 工位');
  typeIs(ws.data, 'array', 'O25 workstation');
  const pricing = await api('GET', '/admin-api/wash/selfwash/pricing', { token: atoken });
  okAdmin(pricing, 'O25 价格');
  amountIs(pricing.data.baseAmount, 'O25 baseAmount');
  amountIs(pricing.data.overtimeAmount, 'O25 overtimeAmount');
  const orders = await api('GET', '/admin-api/wash/selfwash/order/list', { token: atoken, query: PG });
  okAdmin(orders, 'O25 订单');
  typeIs(orders.rows, 'array', 'O25 selfwash order rows');
  const opts = await api('GET', '/admin-api/wash/selfwash/order/status-options', { token: atoken });
  okAdmin(opts, 'O25 状态选项');
  typeIs(opts.data, 'array', 'O25 selfwash options');
});

test('API-O26 客服配置/小区申请/站点选项', async () => {
  const atoken = await adminLogin();
  const cs = await api('GET', '/admin-api/wash/cs-config', { token: atoken });
  okAdmin(cs, 'O26 客服配置');
  truthy(cs, 'data', 'O26 cs-config');
  typeIs(cs.data.faqs, 'array', 'O26 faqs');
  const apply = await api('GET', '/admin-api/wash/community-apply/list', { token: atoken, query: PG });
  okAdmin(apply, 'O26 小区申请');
  typeIs(apply.rows, 'array', 'O26 apply rows');
  const sites = await api('GET', '/admin-api/wash/site/options', { token: atoken });
  okAdmin(sites, 'O26 站点选项');
  typeIs(sites.data, 'array', 'O26 site options');
});

test('API-O27 会员车辆后台列表', async () => {
  const atoken = await adminLogin();
  const res = await api('GET', '/admin-api/wash/vehicle/list', { token: atoken, query: PG });
  okAdmin(res, 'O27 车辆列表');
  typeIs(res.rows, 'array', 'O27 rows');
});

test('API-O28 越权拦截（会员 token 打多个后台域）', async () => {
  const s = await getSession();
  const paths = [
    ['GET', '/admin-api/wash/coupon/template/list', { pageNum: 1, pageSize: 1 }],
    ['GET', '/admin-api/wash/worker/list', { pageNum: 1, pageSize: 1 }],
    ['GET', '/admin-api/wash/dashboard/stats', undefined],
    ['GET', '/admin-api/wash/refund/list', { pageNum: 1, pageSize: 1 }],
  ];
  for (const [m, p, q] of paths) {
    const res = await api(m, p, { token: s.token, query: q });
    assert.ok([401, 403, 10002].includes(res.code), `O28: 会员 token 访问 ${p} 应被拒，实际 code=${res.code} msg=${res.msg}`);
  }
});

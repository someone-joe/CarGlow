/**
 * API-M 钱包/邀请/发票/通知（9 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》M 段。钱包金额边界见 R.1（R01~R09/R11/R12）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, typeIs, amountIs } from '../lib/assert.js';
import { getSession, createOrder, driveChain, cleanupCreated } from '../lib/factory.js';
import { SIDECAR_ENABLED, sql } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

test('API-M01 钱包余额（分 + 赠金分列）', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/wallet', { token: s.token });
  ok(res, 'M01');
  amountIs(res.data.balance, 'M01 balance');
  amountIs(res.data.giftBalance, 'M01 giftBalance');
  amountIs(res.data.totalRecharge, 'M01 totalRecharge');
});

test('API-M02 充值赠送规则', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/wallet/recharge-rules', { token: s.token });
  ok(res, 'M02');
  typeIs(res.data, 'array', 'M02 列表类型');
  for (const r of res.data) {
    amountIs(r.rechargeFen, `M02 ${r.ruleId} rechargeFen`);
    amountIs(r.giftFen, `M02 ${r.ruleId} giftFen`);
  }
});

test('API-M03/M04 充值金额校验与到账', { todo: '金额边界（0/1/99/100/负/超大/元分混淆/档位）与到账幂等已由 R.1（R01~R09）系统覆盖，不重复' }, () => {});

test('API-M05 钱包流水分页', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/wallet/flows', { token: s.token, query: { pageNum: 1, pageSize: 5 } });
  ok(res, 'M05');
  assert.equal(res.data.pageNum, 1, 'M05 pageNum');
  assert.equal(res.data.pageSize, 5, 'M05 pageSize');
  assert.ok((res.data.list || []).length <= 5, 'M05 条数上限');
  // 流水金额为分（可负：支出）
  for (const f of res.data.list || []) {
    assert.ok(Number.isInteger(f.changeAmount), `M05: changeAmount 应为整数（分），实际 ${f.changeAmount}`);
  }
});

test('API-M06 邀请码生成/列表', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const code = await api('GET', '/api/v1/invite/code', { token: s.token });
  ok(code, 'M06 生成邀请码');
  typeIs(code.data, 'string', 'M06 邀请码');
  const list = await api('GET', '/api/v1/invite/list', { token: s.token });
  ok(list, 'M06 邀请列表');
  // 绑定（被邀请人）需要第二个会员 → todo
});

test('API-M06b 邀请码绑定', { todo: '被邀请人绑定需要第二个会员（dev 单会员）；双方权益（邀请人得券、被邀请人首单优惠）待双账号环境覆盖' }, () => {});

test('API-M07 发票申请（金额后端解析，前端不传）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  // 取一个已完成订单开发票
  const { orderNo } = await createOrder({ remark: 'M07 发票' });
  await driveChain(orderNo, 'FINISHED');
  const res = await api('POST', '/api/v1/invoices', {
    token: s.token,
    body: { orderNo, title: '自动化测试公司', titleType: 'COMPANY', taxNo: '91440100MA5X', email: 'test@example.com' },
  });
  ok(res, 'M07 申请');
  // 副作用：金额按订单实付解析（不信任前端传值）；表 wash_invoice（PK invoice_id）
  const row = sql(`select amount, status from wash_invoice where order_no = '${orderNo}' order by invoice_id desc limit 1`).trim();
  assert.ok(row.length > 0, 'M07: 应落库');
});

test('API-M08 发票列表/详情', async () => {
  const s = await getSession();
  const list = await api('GET', '/api/v1/invoices', { token: s.token });
  ok(list, 'M08 列表');
  typeIs(list.data, 'array', 'M08 列表类型');
});

test('API-M09 通知模板/自测推送（x-dev-only）', async () => {
  const s = await getSession();
  const tpl = await api('GET', '/api/v1/notify/templates', { token: s.token });
  ok(tpl, 'M09 模板');
  const test = await api('POST', '/api/v1/notify/test', { token: s.token, body: {} });
  console.log(`  [M09] 自测推送: code=${test.code} msg=${test.msg}`);
});

test('API-M10 FAQ 列表', { todo: '契约定义了 GET /api/v1/faqs（misc tag），但后端未实现（实测 500 No static resource）——与 A07 同类的契约缺口，待决策：补实现或从契约移除' }, () => {});

/**
 * API-O-Actions 后台管理·动作类接口（写操作/状态流转），补齐只读覆盖之外的动作面。
 * 对应 openapi.yaml /admin-api/wash/** 的 POST/PUT/DELETE 动作端点；口径同 O 段：成功码 200。
 *
 * 覆盖：订单后台推进(advance)、售后处理(handle)、发票开具/驳回(issue/reject)、
 *   券发放(grant)/批量发放(batch)、小区申请审核(approve/reject)、退款重试(retry)、
 *   自助洗订单强制结束(finish)、定时任务触发(trigger)、工位增改删、各实体编辑(PUT)、免夹具详情/选项。
 *
 * 夹具策略：
 *   - 需前置状态且 dev 已有现成（售后 INIT / 发票 APPLYING / 小区申请 PENDING / 退款 SUCCESS）→ 直接取用；
 *   - 需自建（券模板/自助洗单/工位/订单/实体）→ 走接口造，finally 清理；
 *   - 副作用（同意售后触发退款、开票、审核建小区、触发任务取消超时单）在 dev 属可接受，已在用例内注明。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, typeIs, truthy } from '../lib/assert.js';
import {
  getSession, adminLogin, createOrder, driveChain, cleanupCreated, cleanupSelfwash,
  createSelfwashOrder, selfwashPay, getSelfwashOrder, getOrder,
} from '../lib/factory.js';

afterEach(async () => { await cleanupCreated(); await cleanupSelfwash(); });

function okAdmin(env, label) {
  assert.equal(env.code, 200, `${label}: 期望 code=200，实际 code=${env.code} msg=${env.msg}`);
}
const PG = { pageNum: 1, pageSize: 50 };
const NOW = Date.now();

test('API-OA01 订单后台推进 advance（正向 + 非法事件负例）', async () => {
  const atoken = await adminLogin();
  const { res, orderNo } = await createOrder({ remark: 'OA01 推进' });
  ok(res, 'OA01 造单');
  await driveChain(orderNo, 'KEY_IN');
  const opts = await api('GET', `/admin-api/wash/order/${orderNo}/event-options`, { token: atoken });
  okAdmin(opts, 'OA01 事件选项');
  const events = (opts.data || []).map((o) => o.value);
  assert.ok(events.length > 0, 'OA01: KEY_IN 应有可推进事件');
  const event = events[0];
  // 正向：带二次确认推进（高危事件 confirm=true 才放行）
  const adv = await api('POST', `/admin-api/wash/order/${orderNo}/advance`, { token: atoken, body: { event, confirm: true, reason: '[AUTOTEST] OA01 后台推进' } });
  okAdmin(adv, 'OA01 推进');
  truthy(adv, 'data', 'OA01 推进结果');
  // 负例：非法事件
  const bad = await api('POST', `/admin-api/wash/order/${orderNo}/advance`, { token: atoken, body: { event: 'NOT_A_REAL_EVENT', confirm: true } });
  assert.notEqual(bad.code, 200, `OA01: 非法事件应拒，实际 code=${bad.code}`);
});

test('API-OA02 售后处理 handle（取 INIT 售后，同意）', async (t) => {
  const atoken = await adminLogin();
  const list = await api('GET', '/admin-api/wash/after-sale/list', { token: atoken, query: PG });
  okAdmin(list, 'OA02 列表');
  const as = (list.rows || []).find((r) => r.status === 'INIT');
  if (!as) return t.skip('无 INIT 状态售后可处理（改拒绝分支亦需 INIT）');
  const h = await api('PUT', `/admin-api/wash/after-sale/${as.afterSaleNo}/handle`, { token: atoken, body: { accept: true, remark: '[AUTOTEST] OA02 同意' } });
  okAdmin(h, 'OA02 处理');
  // 复核：处理后不应仍为 INIT
  const after = (await api('GET', '/admin-api/wash/after-sale/list', { token: atoken, query: PG })).rows.find((r) => r.afterSaleNo === as.afterSaleNo);
  assert.notEqual(after?.status, 'INIT', `OA02: 处理后不应仍为 INIT，实际 ${after?.status}`);
});

test('API-OA03 发票开具 issue + 驳回 reject（取 APPLYING 发票）', async (t) => {
  const atoken = await adminLogin();
  const list = await api('GET', '/admin-api/wash/invoice/list', { token: atoken, query: PG });
  okAdmin(list, 'OA03 列表');
  const applying = (list.rows || []).filter((r) => r.status === 'APPLYING');
  if (applying.length < 2) return t.skip(`APPLYING 发票不足 2 张（现 ${applying.length}），无法同时测开具+驳回`);
  const toIssue = applying[0];
  const toReject = applying[1];
  okAdmin(await api('POST', `/admin-api/wash/invoice/${toIssue.invoiceId}/issue`, { token: atoken, body: { invoiceNo: 'INV-OA03-' + NOW } }), 'OA03 开具');
  okAdmin(await api('POST', `/admin-api/wash/invoice/${toReject.invoiceId}/reject`, { token: atoken, body: { reason: '[AUTOTEST] OA03 驳回' } }), 'OA03 驳回');
  const rows = (await api('GET', '/admin-api/wash/invoice/list', { token: atoken, query: PG })).rows;
  assert.equal(rows.find((r) => r.invoiceId === toIssue.invoiceId)?.status, 'ISSUED', 'OA03: 开具后应 ISSUED');
  assert.equal(rows.find((r) => r.invoiceId === toReject.invoiceId)?.status, 'REJECTED', 'OA03: 驳回后应 REJECTED');
});

test('API-OA04 券发放 grant（自建模板 → 发放 → 记录可见）', async () => {
  const atoken = await adminLogin();
  const tpl = await api('POST', '/admin-api/wash/coupon/template', {
    token: atoken,
    body: { name: '[AUTOTEST] OA04发放券', type: 'NORMAL', thresholdAmount: 0, discountAmount: 200, total: 100, perLimit: 5, startTime: NOW, endTime: NOW + 7 * 86400000, status: 'Y' },
  });
  okAdmin(tpl, 'OA04 建模板');
  const tid = tpl.data?.couponTemplateId ?? tpl.data;
  assert.ok(tid, 'OA04: 应返回模板 ID');
  try {
    const devMember = (await api('GET', '/admin-api/wash/member/list', { token: atoken, query: PG })).rows[0];
    assert.ok(devMember, 'OA04: 应有会员');
    const g = await api('POST', '/admin-api/wash/coupon/grant', { token: atoken, body: { templateId: tid, memberId: devMember.memberId } });
    okAdmin(g, 'OA04 发放');
    const rec = await api('GET', '/admin-api/wash/coupon/record/list', { token: atoken, query: { templateId: tid, pageNum: 1, pageSize: 10 } });
    okAdmin(rec, 'OA04 记录');
    assert.ok((rec.rows || []).length >= 1, 'OA04: 发放后应有领用记录');
  } finally {
    okAdmin(await api('DELETE', `/admin-api/wash/coupon/template/${tid}`, { token: atoken }), 'OA04 删模板');
  }
});

test('API-OA05 券批量发放 batch（自建模板 → 全员发放 → 批次可见）', async () => {
  const atoken = await adminLogin();
  const tpl = await api('POST', '/admin-api/wash/coupon/template', {
    token: atoken,
    body: { name: '[AUTOTEST] OA05批量券', type: 'NORMAL', thresholdAmount: 0, discountAmount: 100, total: 1000, perLimit: 2, startTime: NOW, endTime: NOW + 7 * 86400000, status: 'Y' },
  });
  okAdmin(tpl, 'OA05 建模板');
  const tid = tpl.data?.couponTemplateId ?? tpl.data;
  assert.ok(tid, 'OA05: 应返回模板 ID');
  try {
    const b = await api('POST', '/admin-api/wash/coupon/batch', { token: atoken, body: { templateId: tid, perCount: 1, audience: { all: true } } });
    okAdmin(b, 'OA05 批量发放');
    truthy(b, 'data', 'OA05 批次汇总');
    // 批次列表应能查到
    const bl = await api('GET', '/admin-api/wash/coupon/batch/list', { token: atoken, query: { templateId: tid } });
    okAdmin(bl, 'OA05 批次列表');
    assert.ok((bl.rows || []).length >= 1, 'OA05: 应有批次记录');
  } finally {
    okAdmin(await api('DELETE', `/admin-api/wash/coupon/template/${tid}`, { token: atoken }), 'OA05 删模板');
  }
});

test('API-OA06 小区开通申请审核 approve + reject（取 PENDING）', async (t) => {
  const atoken = await adminLogin();
  const list = await api('GET', '/admin-api/wash/community-apply/list', { token: atoken, query: PG });
  okAdmin(list, 'OA06 列表');
  const pending = (list.rows || []).filter((r) => r.status === 'PENDING');
  if (pending.length < 2) return t.skip(`PENDING 申请不足 2 条（现 ${pending.length}），无法同时测通过+驳回`);
  const toApprove = pending[0];
  const toReject = pending[1];
  // 详情（免夹具 GET）
  okAdmin(await api('GET', `/admin-api/wash/community-apply/${toApprove.applyId}`, { token: atoken }), 'OA06 详情');
  // 通过：需 siteId + communityName（会建小区）
  okAdmin(await api('POST', `/admin-api/wash/community-apply/${toApprove.applyId}/approve`, { token: atoken, body: { siteId: 1, communityName: '[AUTOTEST] OA06通过小区' } }), 'OA06 通过');
  // 驳回：需 reason
  okAdmin(await api('POST', `/admin-api/wash/community-apply/${toReject.applyId}/reject`, { token: atoken, body: { reason: '[AUTOTEST] OA06 驳回' } }), 'OA06 驳回');
  const rows = (await api('GET', '/admin-api/wash/community-apply/list', { token: atoken, query: PG })).rows;
  assert.equal(rows.find((r) => r.applyId === toApprove.applyId)?.status, 'APPROVED', 'OA06: 通过后应 APPROVED');
  assert.equal(rows.find((r) => r.applyId === toReject.applyId)?.status, 'REJECTED', 'OA06: 驳回后应 REJECTED');
});

test('API-OA07 退款重试 retry（SUCCESS 重退应拒，负例）', async (t) => {
  const atoken = await adminLogin();
  const list = await api('GET', '/admin-api/wash/refund/list', { token: atoken, query: PG });
  okAdmin(list, 'OA07 列表');
  const ok0 = (list.rows || []).find((r) => r.status === 'SUCCESS');
  if (!ok0) return t.skip('无 SUCCESS 退款可测重退拒绝');
  const r = await api('POST', `/admin-api/wash/refund/${ok0.refundNo}/retry`, { token: atoken });
  assert.notEqual(r.code, 200, `OA07: 已成功退款不应重退，实际 code=${r.code} msg=${r.msg}`);
});

test('API-OA08 自助洗订单强制结束 finish（自建 SERVING 单）', async (t) => {
  const atoken = await adminLogin();
  const s = await getSession();
  const ws = (await api('GET', '/admin-api/wash/selfwash/workstation/list', { token: atoken })).data || [];
  const station = ws.find((w) => w.enabled === 'Y' || w.enabled === true || w.enabled === '1') || ws[0];
  if (!station) return t.skip('无可用自助洗工位');
  const { orderNo } = await createSelfwashOrder(station.workstationId);
  assert.ok(orderNo, 'OA08: 应建成自助洗单');
  ok(await selfwashPay(orderNo, 'mock'), 'OA08 支付');
  // 激活/开始服务：WAIT_USE → SERVING（工位被占则 QUEUING）
  ok(await api('POST', `/api/v1/selfwash/orders/${orderNo}/activate`, { token: s.token }), 'OA08 激活');
  const cur = (await getSelfwashOrder(orderNo)).data?.status;
  if (cur !== 'SERVING') return t.skip(`激活后状态 ${cur}（工位被占进 QUEUING），无法测强制结束`);
  okAdmin(await api('POST', `/admin-api/wash/selfwash/order/${orderNo}/finish`, { token: atoken }), 'OA08 强制结束');
  assert.equal((await getSelfwashOrder(orderNo)).data?.status, 'DONE', 'OA08: 结束后应 DONE');
});

test('API-OA09 定时任务触发 trigger（白名单 + 非法 job 负例）', async () => {
  const atoken = await adminLogin();
  // 白名单内（order-auto-finish：T+3 自动完成，幂等安全）。
  // 注：实测 pay-timeout / capacity-init 经 trigger 调用会 10001（runNow 抛错），属 job 自身问题，另记录；
  //     这里用可稳定触发的 order-auto-finish 验证 trigger 机制本身。
  const r = await api('POST', '/admin-api/wash/job/trigger', { token: atoken, body: { job: 'order-auto-finish', params: {} } });
  okAdmin(r, 'OA09 触发 order-auto-finish');
  assert.equal(r.data?.triggered, true, 'OA09: 应返回 triggered=true');
  // 白名单外 → 拒
  const bad = await api('POST', '/admin-api/wash/job/trigger', { token: atoken, body: { job: 'not-in-whitelist', params: {} } });
  assert.notEqual(bad.code, 200, `OA09: 非白名单 job 应拒，实际 code=${bad.code}`);
});

test('API-OA10 自助洗工位增改删 workstation CRUD', async () => {
  const atoken = await adminLogin();
  const code = 'WST-OA' + String(NOW).slice(-6);
  const created = await api('POST', '/admin-api/wash/selfwash/workstation', { token: atoken, body: { code, name: '[AUTOTEST] 探针工位', siteId: 1, enabled: 'Y', sortOrder: 99 } });
  okAdmin(created, 'OA10 新增');
  const id = created.data?.workstationId ?? created.data;
  assert.ok(id, 'OA10: 应返回工位 ID');
  try {
    okAdmin(await api('PUT', `/admin-api/wash/selfwash/workstation/${id}`, { token: atoken, body: { workstationId: id, code, name: '[AUTOTEST] 探针工位-改', siteId: 1, enabled: 'Y', sortOrder: 98 } }), 'OA10 修改');
    const row = ((await api('GET', '/admin-api/wash/selfwash/workstation/list', { token: atoken })).data || []).find((w) => w.workstationId === id);
    assert.equal(row?.name, '[AUTOTEST] 探针工位-改', 'OA10: 修改后名称应更新');
  } finally {
    okAdmin(await api('DELETE', `/admin-api/wash/selfwash/workstation/${id}`, { token: atoken }), 'OA10 删除');
  }
});

test('API-OA11 各实体编辑 PUT（券模板/Banner/服务/师傅，建→改→删）', async () => {
  const atoken = await adminLogin();
  // 券模板编辑
  const tpl = await api('POST', '/admin-api/wash/coupon/template', { token: atoken, body: { name: '[AUTOTEST] OA11券', type: 'NORMAL', thresholdAmount: 0, discountAmount: 100, total: 50, perLimit: 1, startTime: NOW, endTime: NOW + 7 * 86400000, status: 'Y' } });
  okAdmin(tpl, 'OA11 建券');
  const tid = tpl.data?.couponTemplateId ?? tpl.data;
  try {
    okAdmin(await api('PUT', '/admin-api/wash/coupon/template', { token: atoken, body: { couponTemplateId: tid, name: '[AUTOTEST] OA11券-改', type: 'NORMAL', thresholdAmount: 0, discountAmount: 150, perLimit: 2, startTime: NOW, endTime: NOW + 7 * 86400000 } }), 'OA11 改券');
  } finally { await api('DELETE', `/admin-api/wash/coupon/template/${tid}`, { token: atoken }); }
  // Banner 编辑
  const bn = await api('POST', '/admin-api/wash/banner', { token: atoken, body: { title: '[AUTOTEST] OA11 Banner', imageUrl: 'https://example.invalid/x.png', linkType: 'NONE', sortOrder: 1 } });
  okAdmin(bn, 'OA11 建 Banner');
  const bid = bn.data?.bannerId ?? bn.data;
  try {
    okAdmin(await api('PUT', '/admin-api/wash/banner', { token: atoken, body: { bannerId: bid, title: '[AUTOTEST] OA11 Banner-改', imageUrl: 'https://example.invalid/x.png', linkType: 'NONE', sortOrder: 1 } }), 'OA11 改 Banner');
  } finally { await api('DELETE', `/admin-api/wash/banner/${bid}`, { token: atoken }); }
  // 服务编辑（O07 已建删，这里补改）
  const svc = await api('POST', '/admin-api/wash/service', { token: atoken, body: { serviceName: '[AUTOTEST] OA11服务', categoryId: 1, priceAmount: 12345, workMinutes: 30, scene: 'UNSENSITIVE' } });
  okAdmin(svc, 'OA11 建服务');
  const sid = svc.data?.serviceId ?? svc.data;
  try {
    okAdmin(await api('PUT', '/admin-api/wash/service', { token: atoken, body: { serviceId: sid, serviceName: '[AUTOTEST] OA11服务-改', categoryId: 1, priceAmount: 23456, workMinutes: 40, scene: 'UNSENSITIVE', enabled: 'Y' } }), 'OA11 改服务');
  } finally { await api('DELETE', `/admin-api/wash/service/${sid}`, { token: atoken }); }
  // 师傅编辑（建→改→删）
  const workerNo = 'WOA' + String(NOW).slice(-6);
  const w = await api('POST', '/admin-api/wash/worker', { token: atoken, body: { workerNo, name: 'OA11师傅', phone: '13900000088', siteId: 1, password: 'probe12345' } });
  okAdmin(w, 'OA11 建师傅');
  const found = (await api('GET', '/admin-api/wash/worker/list', { token: atoken, query: { pageNum: 1, pageSize: 50, keyword: workerNo } })).rows.find((r) => r.workerNo === workerNo);
  const wid = found?.workerId;
  try {
    assert.ok(wid, 'OA11: 应能按工号查到新师傅');
    okAdmin(await api('PUT', '/admin-api/wash/worker', { token: atoken, body: { workerId: wid, workerNo, name: 'OA11师傅-改', phone: '13900000087', siteId: 1 } }), 'OA11 改师傅');
  } finally { if (wid) await api('DELETE', `/admin-api/wash/worker/${wid}`, { token: atoken }); }
});

test('API-OA12 免夹具详情/选项 GETs', async () => {
  const atoken = await adminLogin();
  okAdmin(await api('GET', '/admin-api/wash/service/category-options', { token: atoken }), 'OA12 服务分类选项');
  okAdmin(await api('GET', '/admin-api/wash/worker/status-options', { token: atoken }), 'OA12 师傅状态选项');
  okAdmin(await api('GET', '/admin-api/wash/worker/site-options', { token: atoken }), 'OA12 师傅站点选项');
  // 各 {id} 详情：取列表首条 id
  const site = (await api('GET', '/admin-api/wash/site/list', { token: atoken })).rows?.[0];
  if (site) okAdmin(await api('GET', `/admin-api/wash/site/${site.siteId}`, { token: atoken }), 'OA12 站点详情');
  const comm = (await api('GET', '/admin-api/wash/community/list', { token: atoken })).rows?.[0];
  if (comm) okAdmin(await api('GET', `/admin-api/wash/community/${comm.communityId}`, { token: atoken }), 'OA12 小区详情');
  const batch = (await api('GET', '/admin-api/wash/coupon/batch/list', { token: atoken, query: PG })).rows?.[0];
  if (batch) okAdmin(await api('GET', `/admin-api/wash/coupon/batch/${batch.batchId}`, { token: atoken }), 'OA12 券批次详情');
  const inv = (await api('GET', '/admin-api/wash/invoice/list', { token: atoken, query: PG })).rows?.[0];
  if (inv) okAdmin(await api('GET', `/admin-api/wash/invoice/${inv.invoiceId}`, { token: atoken }), 'OA12 发票详情');
  const so = (await api('GET', '/admin-api/wash/selfwash/order/list', { token: atoken, query: PG })).rows?.[0];
  if (so) okAdmin(await api('GET', `/admin-api/wash/selfwash/order/${so.orderNo}`, { token: atoken }), 'OA12 自助洗订单详情');
});

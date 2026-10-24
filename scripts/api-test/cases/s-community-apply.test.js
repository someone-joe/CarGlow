/**
 * API-S 小区开通申请审核流（community-applies）。
 * 对应：openapi.yaml 的 POST /api/v1/community-applies 与 /admin-api/wash/community-apply/**。
 * 覆盖：C 端提交 → 后台列表可见 → 审核通过即建小区并启用（enabled=Y/accessAuth=Y）→ 拒绝路径。
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok } from '../lib/assert.js';
import { getSession, adminLogin } from '../lib/factory.js';

test('API-S01 小区开通申请：提交 → 审核通过（建小区并直接启用）', async () => {
  const s = await getSession();
  // C 端提交（member 上下文，后端取 MemberContext，不接收前端 memberId）
  const sub = await api('POST', '/api/v1/community-applies', {
    token: s.token, body: { communityName: '自动化测试小区', contactPhone: '13900000000' },
  });
  ok(sub, 'S01 提交申请');
  const applyId = sub.data;
  assert.ok(applyId > 0, `S01: 应返回申请ID，实际 ${applyId}`);

  const atoken = await adminLogin();
  // 后台列表应看到该待审申请
  const list = await api('GET', `/admin-api/wash/community-apply/list?status=PENDING`, { token: atoken });
  assert.equal(list.code, 200, `S01 列表失败: ${list.msg}`);
  const found = (list.rows || []).find((r) => r.applyId === applyId);
  assert.ok(found, 'S01: 列表中应能看到刚提交的申请');
  assert.equal(found.status, 'PENDING', `S01: 应为待审核，实际 ${found.status}`);
  assert.equal(found.statusLabel, '待审核', 'S01: 状态中文名应由后端给出');

  // 审核通过：建小区并直接启用（enabled=Y/accessAuth=Y）
  const ap = await api('POST', `/admin-api/wash/community-apply/${applyId}/approve`, {
    token: atoken, body: { siteId: 1 },
  });
  assert.equal(ap.code, 200, `S01 审核通过失败: ${ap.msg}`);
  assert.ok(ap.data > 0, `S01: 应返回新建小区ID，实际 ${ap.data}`);

  // 复核：申请已 APPROVED 且关联小区
  const detail = await api('GET', `/admin-api/wash/community-apply/${applyId}`, { token: atoken });
  assert.equal(detail.code, 200, `S01 详情失败: ${detail.msg}`);
  assert.equal(detail.data.status, 'APPROVED', `S01: 应为已通过，实际 ${detail.data.status}`);
  assert.equal(detail.data.communityId, ap.data, 'S01: 应回填新建小区ID');

  // 新建小区应可下单（enabled=Y/accessAuth=Y 已生效；C 端列表以布尔返回）
  const comms = await api('GET', '/api/v1/communities', { token: s.token });
  ok(comms, 'S01 小区列表');
  assert.ok(
    (comms.data || []).some((c) => c.communityId === ap.data && c.enabled === true && c.accessAuth === true),
    'S01: 新建小区应已启用且已授权',
  );
});

test('API-S02 小区开通申请：拒绝需填原因', async () => {
  const s = await getSession();
  const sub = await api('POST', '/api/v1/community-applies', {
    token: s.token, body: { communityName: '待拒绝小区', contactPhone: '13900000001' },
  });
  ok(sub, 'S02 提交申请');
  const applyId = sub.data;
  const atoken = await adminLogin();
  // 无原因拒绝应被拒
  const bad = await api('POST', `/admin-api/wash/community-apply/${applyId}/reject`, {
    token: atoken, body: {},
  });
  assert.notEqual(bad.code, 200, 'S02: 拒绝缺少原因应失败');
  // 带原因拒绝成功
  const okReject = await api('POST', `/admin-api/wash/community-apply/${applyId}/reject`, {
    token: atoken, body: { reason: '不在服务范围' },
  });
  assert.equal(okReject.code, 200, `S02 拒绝失败: ${okReject.msg}`);
  const detail = await api('GET', `/admin-api/wash/community-apply/${applyId}`, { token: atoken });
  assert.equal(detail.data.status, 'REJECTED', `S02: 应为已拒绝，实际 ${detail.data.status}`);
  assert.equal(detail.data.rejectReason, '不在服务范围', 'S02: 应回填拒绝原因');
});

/**
 * API-B 基础数据（14 条中的可执行子集）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》B 段。
 * 断言口径：以契约为准逐字段核对；契约与实现不一致的记为 todo 并说明，不写「放水」断言。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, truthy, typeIs, amountIs } from '../lib/assert.js';
import { getSession, cleanupCreated } from '../lib/factory.js';

afterEach(() => cleanupCreated());

test('API-B01 当前站点与小区', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/site/current', { token: s.token });
  ok(res, 'B01');
  truthy(res, 'data.displayName', 'B01 展示名（小区·站点）');
  truthy(res, 'data.communityName', 'B01 小区名');
  truthy(res, 'data.serviceStatus', 'B01 服务状态');
});

test('API-B02 小区列表', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/communities', { token: s.token });
  ok(res, 'B02');
  typeIs(res.data, 'array', 'B02 列表类型');
  if (res.data.length) {
    truthy(res, 'data.0.name', 'B02 首项小区名');
    truthy(res, 'data.0.communityId', 'B02 首项小区 ID');
  }
});

test('API-B03 小区开通申请', async () => {
  const s = await getSession();
  const res = await api('POST', '/api/v1/community-applies', {
    token: s.token,
    body: { communityName: `自动化测试小区${Date.now()}`, contactPhone: '13800000000' },
  });
  ok(res, 'B03 提交申请');
  assert.ok(typeof res.data === 'number' && res.data > 0, `B03: 应返回申请ID，实际 ${res.data}`);
});

test('API-B04 首页 Banner', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/banners', { token: s.token });
  ok(res, 'B04');
  typeIs(res.data, 'array', 'B04 列表类型');
});

test('API-B05 今晚剩余产能', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/capacity', { token: s.token });
  ok(res, 'B05');
  truthy(res, 'data.date', 'B05 日期');
  truthy(res, 'data.businessStart', 'B05 营业开始');
  typeIs(res.data.total, 'number', 'B05 total（站点日上限）');
  typeIs(res.data.remaining, 'number', 'B05 remaining（Redis 实时值）');
  // 口径与 WashCapacityService 一致：used = max(0, total - remaining)（扩容测试值可能使 remaining>total，used 不为负）
  assert.equal(res.data.used, Math.max(0, res.data.total - res.data.remaining),
    `B05: used 应 = max(0, total-remaining)，实际 used=${res.data.used} total=${res.data.total} remaining=${res.data.remaining}`);
  assert.ok(res.data.remaining >= 0 && res.data.total > 0, 'B05: 数值范围应合理');
});

test('API-B06 服务分类与服务列表', async () => {
  const s = await getSession();
  const cats = await api('GET', '/api/v1/service-categories', { token: s.token });
  ok(cats, 'B06 分类');
  typeIs(cats.data, 'array', 'B06 分类类型');
  const list = await api('GET', '/api/v1/services', { token: s.token });
  ok(list, 'B06 列表');
  typeIs(list.data, 'array', 'B06 列表类型');
  if (list.data.length) {
    typeIs(list.data[0].displayPrice, 'number', 'B06 价格类型（单位分）');
    typeIs(list.data[0].workMinutes, 'number', 'B06 工时类型（分钟）');
  }
});

test('API-B07 服务详情', async () => {
  const s = await getSession();
  const res = await api('GET', `/api/v1/services/${s.service.serviceId}`, { token: s.token });
  ok(res, 'B07');
  truthy(res, 'data.name', 'B07 服务名');
  typeIs(res.data.displayPrice, 'number', 'B07 价格（分）');
});

test('API-B08 服务详情-不存在（A0001）', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/services/999999', { token: s.token });
  bizCode(res, 10001, 'B08');
});

test('API-B09 机柜列表（full 与 slotFree 口径一致）', async () => {
  const s = await getSession();
  const res = await api('GET', '/api/v1/cabinets', { token: s.token });
  ok(res, 'B09');
  typeIs(res.data, 'array', 'B09 列表类型');
  for (const c of res.data) {
    typeIs(c.slotFree, 'number', `B09 ${c.name} slotFree`);
    typeIs(c.slotTotal, 'number', `B09 ${c.name} slotTotal`);
    // full 必须与 slotFree 口径一致（full ⇔ slotFree<=0），防止前端拿到矛盾数据
    assert.equal(c.full, c.slotFree <= 0, `B09: ${c.name} full=${c.full} 与 slotFree=${c.slotFree} 矛盾`);
  }
});

test('API-B10 车辆 CRUD 正向（增/改/查；DELETE 见 B10b）', async () => {
  const s = await getSession();
  const plate = `粤B${String(Date.now()).slice(-6)}`;
  const created = await api('POST', '/api/v1/vehicles', {
    token: s.token, body: { plateNo: plate, brand: '自动化', color: '白', vehicleType: 'SEDAN' },
  });
  ok(created, 'B10 新增');
  const vehicleId = created.data;

  const list = await api('GET', '/api/v1/vehicles', { token: s.token });
  ok(list, 'B10 列表');
  assert.ok(list.data.some((v) => v.vehicleId === vehicleId), 'B10: 新增的车应在列表中');

  const updated = await api('PUT', `/api/v1/vehicles/${vehicleId}`, { token: s.token, body: { color: '黑' } });
  ok(updated, 'B10 修改');
  const list2 = await api('GET', '/api/v1/vehicles', { token: s.token });
  assert.equal(list2.data.find((v) => v.vehicleId === vehicleId).color, '黑', 'B10: 修改应生效');
});

test('API-B10b 车辆-删除（DELETE）', async () => {
  const s = await getSession();
  const plate = `粤D${String(Date.now()).slice(-6)}`;
  const created = await api('POST', '/api/v1/vehicles', {
    token: s.token, body: { plateNo: plate, brand: '待删除', color: '白' },
  });
  ok(created, 'B10b 新增');
  const vehicleId = created.data;

  const del = await api('DELETE', `/api/v1/vehicles/${vehicleId}`, { token: s.token });
  ok(del, 'B10b 删除');

  // 副作用：逻辑删，列表不再出现（不能用物理删——历史订单仍引用 vehicle_id）
  const list = await api('GET', '/api/v1/vehicles', { token: s.token });
  ok(list, 'B10b 删除后列表');
  assert.ok(!(list.data || []).some((v) => v.vehicleId === vehicleId), 'B10b: 删除后不应再出现在列表');

  // 重复删除 / 删不存在的车：与 update 同口径报 A0001，不区分「不存在」与「不是你的」
  bizCode(await api('DELETE', `/api/v1/vehicles/${vehicleId}`, { token: s.token }), 10001, 'B10b 重复删除');
});

test('API-B11 车辆-越权修改（A0001 统一口径）', async () => {
  const s = await getSession();
  bizCode(await api('PUT', '/api/v1/vehicles/999999', { token: s.token, body: { color: '红' } }), 10001, 'B11 越权修改');
});

test('API-B12 车辆-车牌格式/重复', { todo: '非法车牌格式的校验规则契约未列明；重复车牌当前实现未禁止（多会员可同牌）。待产品明确后补' }, () => {});

test('API-B13 常用停车位置 CRUD + 越权', async () => {
  const s = await getSession();
  // 新增必填所在小区（实测：不传 communityId 报「请选择所在小区」）
  const communities = (await api('GET', '/api/v1/communities', { token: s.token })).data;
  const communityId = communities[0]?.communityId;
  assert.ok(communityId, 'B13: 应有可用小区');
  const created = await api('POST', '/api/v1/park-locations', {
    token: s.token, body: { name: '自动化测试车位', address: 'B2-A08', communityId },
  });
  ok(created, 'B13 新增');
  const id = created.data;

  const list = await api('GET', '/api/v1/park-locations', { token: s.token });
  ok(list, 'B13 列表');

  ok(await api('PUT', `/api/v1/park-locations/${id}`, { token: s.token, body: { name: '改过的车位' } }), 'B13 修改');
  ok(await api('DELETE', `/api/v1/park-locations/${id}`, { token: s.token }), 'B13 删除');
  bizCode(await api('DELETE', '/api/v1/park-locations/999999', { token: s.token }), 10001, 'B13 越权删除');
});

test('API-B14 免登录接口（selfwash/pricing）', async () => {
  // 契约 summary 标「免登录」（2026-10-24 决策：实现与 openapi 都对齐到免登录，
  // openapi 已补 security: []，MemberWebConfig 已放行）。价格是纯配置，不含用户数据。
  const res = await api('GET', '/api/v1/selfwash/pricing');
  ok(res, 'B14 免登录读取计价');
  amountIs(res.data.baseAmount, 'B14 baseAmount');
  typeIs(res.data.baseMinutes, 'number', 'B14 baseMinutes');
  amountIs(res.data.overtimeAmount, 'B14 overtimeAmount');
  typeIs(res.data.overtimeMinutes, 'number', 'B14 overtimeMinutes');
});

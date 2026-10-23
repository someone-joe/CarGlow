/**
 * API-I 影像（8 条）。
 * 对应：《自动化测试/接口自动化/API测试用例.md》I 段；PROGRESS E-2（fileId 归属校验）。
 */
import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api } from '../lib/http.js';
import { ok, bizCode, typeIs } from '../lib/assert.js';
import {
  getSession, createOrder, driveChain, workerLogin, uploadPhoto, uploadPhotos, cleanupCreated,
} from '../lib/factory.js';
import { SIDECAR_ENABLED, sql } from '../lib/sidecar.js';

afterEach(() => cleanupCreated());

test('API-I01 上传-正向（各 bizType 落库）', async (t) => {
  if (!SIDECAR_ENABLED) t.skip('CG_SKIP_SIDECAR=1');
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'I01 上传' });
  const fileId = await uploadPhoto('PARK', orderNo);
  assert.ok(fileId, 'I01: 应返回 fileId');
  // 副作用：DB 落库且归属本单
  const row = sql(`select biz_type, order_no from wash_media where file_id = '${fileId}'`).trim();
  assert.ok(row.length > 0, 'I01: 应落库');
  assert.ok(row.includes('PARK') && row.includes(orderNo), `I01: bizType/归属应正确，实际 ${row}`);
});

test('API-I02 上传-超限（>10MB 拒绝）', async () => {
  const s = await getSession();
  const big = Buffer.alloc(11 * 1024 * 1024, 1); // 11MB
  const form = new FormData();
  form.append('file', new Blob([big], { type: 'image/png' }), 'big.png');
  form.append('bizType', 'PARK');
  const res = await api('POST', '/api/v1/media/upload', { token: s.token, form });
  assert.notEqual(res.code, 0, `I02: 11MB 应被拒，实际 code=${res.code} msg=${res.msg}`);
  console.log(`  [I02] 实际 code=${res.code} msg=${res.msg}`);
});

test('API-I03 归属校验-他单影像不能推进本单（B1005）', async () => {
  const s = await getSession();
  const wtoken = await workerLogin();
  // 单 A 的 PICK 照片
  const a = await createOrder({ remark: 'I03 照片所属单A' });
  await driveChain(a.orderNo, 'PICKING');
  const idsOfA = await uploadPhotos('PICK', a.orderNo, 6);
  // 单 B（独立订单）到 PICKING，用 A 的照片推进 → 应被 B1005 拦
  const b = await createOrder({ remark: 'I03 被冒用单B' });
  await driveChain(b.orderNo, 'PICKING');
  const res = await api('POST', `/api/v1/pick/orders/${b.orderNo}/pick-car-done`, { token: wtoken, body: { fileIds: idsOfA } });
  bizCode(res, 21005, 'I03 他单影像');
});

test('API-I04 已绑定别单的停车照不能复用（B1005）', async () => {
  const s = await getSession();
  const a = await createOrder({ remark: 'I04 停车照所属单A' });
  const parkOfA = await uploadPhoto('PARK', a.orderNo); // 归属 A
  // 单 B 下单时带 A 的停车照 → 拒
  const b = await createOrder({ parkPhotoFileIds: [parkOfA], remark: 'I04 复用他单停车照' });
  bizCode(b.res, 21005, 'I04 复用停车照');
});

test('API-I05 读取原文件-正向（带 token）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'I05 读取' });
  const fileId = await uploadPhoto('PARK', orderNo);
  const res = await fetch(`http://127.0.0.1:8080/api/v1/media/${fileId}/raw`, {
    headers: { Authorization: `Bearer ${s.token}` },
  });
  assert.equal(res.status, 200, `I05: 应 200，实际 ${res.status}`);
  const buf = Buffer.from(await res.arrayBuffer());
  assert.ok(buf.length > 0, 'I05: 应有内容');
});

test('API-I06 读取-路径穿越拒绝', async () => {
  const s = await getSession();
  for (const evil of ['..%2F..%2Fetc%2Fpasswd', '....//....//etc/passwd']) {
    let rejected = false;
    let detail = '';
    try {
      const res = await api('GET', `/api/v1/media/${evil}/raw`, { token: s.token });
      rejected = res.code !== 0;
      detail = `code=${res.code}`;
    } catch (e) {
      // 传输层拒绝（如 Tomcat 对编码斜杠返回 400 HTML）同样算拒绝
      rejected = /HTTP 4\d\d/.test(e.message);
      detail = e.message.split('\n')[0];
    }
    assert.ok(rejected, `I06: ${evil} 应被拒，实际 ${detail}`);
  }
});

test('API-I07 读取-query token 鉴权（image 组件带不了 header）', async () => {
  const s = await getSession();
  const { orderNo } = await createOrder({ remark: 'I07 query token' });
  const fileId = await uploadPhoto('PARK', orderNo);
  // 注意：本项目拦截器拒绝走 renderString——HTTP 200 + 错误信封，断言必须查 body 的 code
  const noTok = await fetch(`http://127.0.0.1:8080/api/v1/media/${fileId}/raw`);
  const noTokBody = await noTok.json();
  assert.equal(noTokBody.code, 10002, `I07: 无 token 应拒绝（body code=10002），实际 ${JSON.stringify(noTokBody).slice(0, 120)}`);

  const fakeTok = await fetch(`http://127.0.0.1:8080/api/v1/media/${fileId}/raw?token=faketoken`);
  const fakeBody = await fakeTok.json();
  assert.equal(fakeBody.code, 10002, `I07: 假 token 应拒绝，实际 ${JSON.stringify(fakeBody).slice(0, 120)}`);

  // 带真 token（query）→ 放行，返回 PNG 二进制（魔数 89 50 4E 47）
  const withTok = await fetch(`http://127.0.0.1:8080/api/v1/media/${fileId}/raw?token=${encodeURIComponent(s.token)}`);
  const buf = Buffer.from(await withTok.arrayBuffer());
  assert.ok(buf.length > 0 && buf[0] === 0x89 && buf[1] === 0x50, 'I07: query token 应放行且返回图片二进制');
});

test('API-I08 师傅影像独立鉴权（worker 链）', async () => {
  const wtoken = await workerLogin();
  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    'base64'
  );
  const form = new FormData();
  form.append('file', new Blob([png], { type: 'image/png' }), 'qual.png');
  form.append('bizType', 'WORKER_QUAL');
  const up = await api('POST', '/api/v1/worker/media/upload', { token: wtoken, form });
  ok(up, 'I08 师傅上传');
  const fileId = up.data?.fileId ?? up.data;
  assert.ok(fileId, 'I08: 应返回 fileId');
  // 会员 token 读不到师傅影像（独立链）
  const s = await getSession();
  const asMember = await api('GET', `/api/v1/worker/media/${fileId}/raw`, { token: s.token });
  assert.notEqual(asMember.code, 0, `I08: 会员 token 不应能读师傅影像，实际 code=${asMember.code}`);
  // 师傅 token 可以
  const asWorker = await fetch(`http://127.0.0.1:8080/api/v1/worker/media/${fileId}/raw`, {
    headers: { Authorization: `Bearer ${wtoken}` },
  });
  assert.equal(asWorker.status, 200, `I08: 师傅 token 应放行，实际 ${asWorker.status}`);
});

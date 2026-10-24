#!/usr/bin/env node
/**
 * 造数 helper：把订单建在「dev-openid 会员」名下并推进到目标状态。
 *
 * 为什么能直接给 UI 用：
 *   后端 dev 模式（wash.wx.secret 为空）下 resolveOpenid 对任何 code 都返回固定 "dev-openid"，
 *   前端 ensureLogin（wx.login → /api/v1/auth/login）与 API 测试 factory.js（code:'dev'）落到【同一个会员】。
 *   因此本脚本造的订单，wechatide 模拟器登录后天然可见，UI 用例直接断言即可。
 *
 * 用法：
 *   node seed.mjs <STATE> [count]
 *   STATE ∈ 正常主链路节点（WAIT_PAY/WAIT_KEY/KEY_IN/PICKING/TO_STATION/WASHING/QC/
 *                        WAIT_RETURN/RETURNING/RETURNED/WAIT_REVIEW/FINISHED）
 *          + 别名 SERVING(=KEY_IN) + CANCELLED + REFUNDED
 *   输出：空格分隔的 orderNo（写入 stdout，便于 UI 用例 $(ui_seed ...) 捕获；错误走 stderr）
 *   副作用：每个 orderNo 追加到 .seeded（供 ui_reset 清理）
 */
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import fs from 'node:fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const LIB = resolve(__dirname, '../../../scripts/api-test/lib');
const f = await import(resolve(LIB, 'factory.js'));
const { api } = await import(resolve(LIB, 'http.js'));
const { createOrder, driveChain, getSession, cancelOrder, adminCancelOrder, adminLogin } = f;

const CHAIN = [
  'WAIT_PAY', 'WAIT_KEY', 'KEY_IN', 'PICKING', 'TO_STATION',
  'WASHING', 'QC', 'WAIT_RETURN', 'RETURNING', 'RETURNED', 'WAIT_REVIEW', 'FINISHED',
];
const ALIAS = { SERVING: 'KEY_IN' };

async function seedOne(target) {
  const state = ALIAS[target] || target;
  const { orderNo } = await createOrder({ remark: `UI造数 ${target}` });
  if (!orderNo) throw new Error('下单未返回 orderNo（产能/格口可能有问题）');

  if (CHAIN.includes(state)) {
    await driveChain(orderNo, state);
  } else if (state === 'CANCELLED') {
    const r = await cancelOrder(orderNo, 'UI造数取消');
    if (r.code !== 0 && r.code !== 200) throw new Error(`取消失败 code=${r.code} ${r.msg}`);
  } else if (state === 'REFUNDED') {
    await driveChain(orderNo, 'FINISHED');
    const s = await getSession();
    const as = await api('POST', '/api/v1/after-sales', {
      token: s.token, body: { orderNo, type: 'REFUND', reason: 'UI造数退款' },
    });
    if (as.code !== 0) throw new Error(`建售后失败 code=${as.code} ${as.msg}`);
    const atoken = await adminLogin();
    const h = await api('PUT', `/admin-api/wash/after-sale/${as.data}/handle`, {
      token: atoken, body: { accept: true },
    });
    if (h.code !== 200 && h.code !== 0) throw new Error(`后台同意售后失败 code=${h.code} ${h.msg}`);
  } else {
    throw new Error(`未知目标状态: ${target}（支持 ${[...CHAIN, ...Object.keys(ALIAS), 'CANCELLED', 'REFUNDED'].join('/')}）`);
  }
  return orderNo;
}

const target = process.argv[2] || 'WAIT_PAY';
const count = Math.max(1, parseInt(process.argv[3] || '1', 10));

const out = [];
for (let i = 0; i < count; i++) out.push(await seedOne(target));

const logPath = resolve(__dirname, '.seeded');
fs.appendFileSync(logPath, out.join('\n') + '\n');
console.log(out.join(' '));

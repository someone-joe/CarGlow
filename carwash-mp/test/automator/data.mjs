#!/usr/bin/env node
/**
 * 会员侧造数 helper（配合 UI 用例）：券 / 余额 / 车辆。
 *
 * 与 seed.mjs 一样建立在「dev-openid 同一会员」前提上（详见 seed.mjs 头部注释），
 * 走的都是业务接口，不直接改库。
 *
 * 用法：
 *   node data.mjs coupon                确保名下有一张 UNUSED 券，打印 couponUserId
 *   node data.mjs coupon-count          打印名下券总条数（用于领券后即时刷新的前后对比）
 *   node data.mjs wallet                打印可用余额（分，balance + giftBalance，与 pay.vue 口径一致）
 *   node data.mjs topup <fen>           精确充值（mock 到账），打印充值单号
 *   node data.mjs order-pay <orderNo>   打印订单应付金额（分）
 *   node data.mjs vehicles              打印车辆列表 JSON
 *   node data.mjs vehicles:clear        备份并删除所有车辆，打印删除条数
 *   node data.mjs vehicles:restore      从备份恢复车辆，打印恢复条数
 */
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import fs from 'node:fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const LIB = resolve(__dirname, '../../../scripts/api-test/lib');
const f = await import(resolve(LIB, 'factory.js'));
const { api } = await import(resolve(LIB, 'http.js'));

const BACKUP = resolve(__dirname, '.vehicles.bak.json');

/**
 * 直连登录取 token：刻意不用 factory.getSession()——后者在车辆列表为空时会自动建一辆车，
 * 会把「无车」场景（UI-C09）就地破坏。车辆相关子命令一律走这里。
 */
async function rawToken() {
  const login = await api('POST', '/api/v1/auth/login', { body: { code: 'dev' } });
  if (login.code !== 0) throw new Error(`登录失败 ${login.code} ${login.msg}`);
  return login.data.token;
}

async function coupon() {
  const got = await f.receiveAnyCoupon();
  if (!got?.couponUserId) {
    console.error('无可领模板且名下无 UNUSED 券');
    process.exit(1);
  }
  console.log(got.couponUserId);
}

/** 名下券条数：领券前后对比用（curl_count 变化反映「即时刷新」） */
async function couponCount() {
  const token = await rawToken();
  const mine = (await api('GET', '/api/v1/coupons', { token })).data || [];
  console.log(mine.length);
}

/** 可用余额（分）：与 pay.vue 的 walletAvailable 同口径（balance + giftBalance） */
async function wallet() {
  const w = await f.getWallet();
  console.log((w.balance ?? 0) + (w.giftBalance ?? 0));
}

/** 精确充值：mock 到账，用于把余额补到足够支付，避免 UI 落到「余额不足→跳充值」分支 */
async function topup(amountFen) {
  const r = await f.topUpExact(Number(amountFen));
  console.log(r.rechargeNo);
}

async function orderPay(orderNo) {
  const token = await rawToken();
  const r = await api('GET', `/api/v1/orders/${orderNo}`, { token });
  if (r.code !== 0) {
    console.error(`查订单失败 code=${r.code} ${r.msg}`);
    process.exit(1);
  }
  console.log(r.data.payAmount);
}

async function vehicles() {
  const token = await rawToken();
  const list = (await api('GET', '/api/v1/vehicles', { token })).data || [];
  console.log(JSON.stringify(list));
}

/** 清空车辆前先备份：UI-C09 测完必须能原样还原，避免污染其它用例 */
async function vehiclesClear() {
  const token = await rawToken();
  const list = (await api('GET', '/api/v1/vehicles', { token })).data || [];
  // 空列表会写出空备份，之后还原「什么都还原不回来」，必须拒绝
  if (!list.length) {
    console.error('会员名下已无车辆，拒绝清空（会写出空备份）');
    process.exit(1);
  }
  fs.writeFileSync(BACKUP, JSON.stringify(list, null, 2));
  for (const v of list) {
    await api('DELETE', `/api/v1/vehicles/${v.vehicleId}`, { token });
  }
  console.log(list.length);
}

async function vehiclesRestore() {
  if (!fs.existsSync(BACKUP)) {
    console.error('无备份文件，跳过恢复');
    process.exit(1);
  }
  const list = JSON.parse(fs.readFileSync(BACKUP, 'utf8'));
  const token = await rawToken();
  let n = 0;
  for (const v of list) {
    const body = {
      plateNo: v.plateNo,
      brand: v.brand,
      color: v.color,
      vehicleType: v.vehicleType || 'SEDAN',
    };
    if (v.isDefault != null) body.isDefault = v.isDefault;
    const r = await api('POST', '/api/v1/vehicles', { token, body });
    if (r.code === 0) n += 1;
  }
  fs.unlinkSync(BACKUP);
  console.log(n);
}

const cmd = process.argv[2] || '';
const arg = process.argv[3];
const table = {
  coupon,
  'coupon-count': couponCount,
  wallet,
  topup,
  'order-pay': orderPay,
  vehicles,
  'vehicles:clear': vehiclesClear,
  'vehicles:restore': vehiclesRestore,
};

const fn = table[cmd];
if (!fn) {
  console.error(`未知子命令: ${cmd}`);
  process.exit(2);
}
await fn(arg);

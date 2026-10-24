#!/usr/bin/env node
/**
 * 清理本 helper 造的订单：读取 .seeded，逐单取消（客户取消被拒则走后台取消）。
 * 终态单（FINISHED/CANCELLED/REFUNDED）取消会被状态机拒绝，属正常，忽略即可。
 * 用法：node cleanup-seed.mjs
 */
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import fs from 'node:fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const LIB = resolve(__dirname, '../../../scripts/api-test/lib');
const { cancelOrder, adminCancelOrder, getSession } = await import(resolve(LIB, 'factory.js'));

const logPath = resolve(__dirname, '.seeded');
if (!fs.existsSync(logPath)) {
  console.log('无 seed 记录，跳过');
  process.exit(0);
}
const nos = fs.readFileSync(logPath, 'utf8').split('\n').map((s) => s.trim()).filter(Boolean);
await getSession();
let cleaned = 0;
for (const no of nos) {
  try {
    let r = await cancelOrder(no, 'UI造数清理');
    if (r.code !== 0 && r.code !== 200) r = await adminCancelOrder(no, 'UI造数清理');
  } catch {
    /* 单子可能已不存在，忽略 */
  }
  cleaned += 1;
}
fs.writeFileSync(logPath, '');
console.log(`清理 ${cleaned} 个 seed 订单`);

/**
 * 测试窗口产能扩容：up = 保存原值并设为高限；down = 恢复原值。
 *
 * 为什么需要：dev 站点每日产能上限 20 单（业务规则），
 * 而接口套件一轮要建 30+ 单（含边界用例），不扩容会批量误报 A0006。
 * C04（产能为 0 拒绝）仍用 withCapacityZero 显式验证限量逻辑，不受扩容影响。
 *
 * 用法：node scripts/api-test/capacity.mjs up|down
 */
import { writeFileSync, readFileSync, existsSync, unlinkSync } from 'node:fs';
import { redis, capacityKey } from './lib/sidecar.js';

const BACKUP = '/tmp/cg-capacity-backup.txt';
const MISSING = '__MISSING__';
const mode = process.argv[2];
const key = capacityKey();

if (mode === 'up') {
  const before = redis('GET', key);
  writeFileSync(BACKUP, before === '' ? MISSING : before);
  const testLimit = process.env.CG_TEST_CAPACITY || '500';
  redis('SET', key, testLimit);
  console.log(`[capacity] ${key}: ${before === '' ? '(不存在)' : before} → ${testLimit}（测试窗口，跑完恢复）`);
} else if (mode === 'down') {
  if (!existsSync(BACKUP)) {
    console.log('[capacity] 无备份文件，跳过恢复');
    process.exit(0);
  }
  const before = readFileSync(BACKUP, 'utf8');
  if (before === MISSING) redis('DEL', key);
  else redis('SET', key, before);
  unlinkSync(BACKUP);
  console.log(`[capacity] ${key} 已恢复为 ${before === MISSING ? '(删除)' : before}`);
} else {
  console.error('用法: node capacity.mjs up|down');
  process.exit(1);
}

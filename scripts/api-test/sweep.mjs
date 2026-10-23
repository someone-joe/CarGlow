/**
 * 跑前清理：取消遗留的 [AUTOTEST] 待支付订单。
 * 识别走只读 SQL，取消走接口（释放格口 + 回补产能），不直接改库。
 */
import { existsSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { autotestWaitPayOrders, nonTerminalOrders, resetDevWallet, ensureTestSlots, listTestMedia, deleteTestMediaRows, SIDECAR_ENABLED } from './lib/sidecar.js';
import { getSession, cancelOrder, adminCancelOrder, adminLogin } from './lib/factory.js';
import { api } from './lib/http.js';

// 影像存储根目录（与 wash.media.store-path 对齐：./upload/media，相对后端启动目录 = 仓库根）
const MEDIA_ROOT = process.env.CG_MEDIA_ROOT || join(process.cwd(), 'upload', 'media');

if (!SIDECAR_ENABLED) {
  console.log('[sweep] CG_SKIP_SIDECAR=1，跳过清理');
  process.exit(0);
}

let orders = [];
try {
  orders = autotestWaitPayOrders();
} catch (e) {
  console.log('[sweep] 查询失败（docker 不可用？），跳过清理:', String(e.message).split('\n')[0]);
  process.exit(0);
}

if (orders.length) {
  await getSession();
  for (const orderNo of orders) {
    const r = await cancelOrder(orderNo, '[AUTOTEST] 跑前清理');
    console.log(`[sweep] ${orderNo} → ${r.code === 0 ? '已取消' : `code=${r.code} ${r.msg}`}`);
  }
} else {
  console.log('[sweep] 无遗留测试订单');
}

// 收尾残留的非终态订单（含上一轮客户取消不了的深度状态单）：释放格口、归还师傅接单额度
{
  try {
    const stuck = nonTerminalOrders();
    if (stuck.length) {
      for (const orderNo of stuck) {
        const r = await adminCancelOrder(orderNo, '[AUTOTEST] 跑前收尾');
        if (r.code !== 200 && r.code !== 0) console.log(`  [sweep] ${orderNo} 收尾失败 code=${r.code} ${r.msg}`);
      }
      console.log(`[sweep] 非终态残留订单已收尾：${stuck.length} 单`);
    } else {
      console.log('[sweep] 无非终态残留订单');
    }
  } catch (e) {
    console.log('[sweep] 订单收尾失败:', String(e.message).split('\n')[0]);
  }
}

// dev 钱包复位：保证钱包边界用例每轮可重复构造（详见 lib/sidecar.js 注释）
if (process.env.CG_RESET_WALLET !== '0') {
  try {
    resetDevWallet();
    console.log('[sweep] dev 钱包已复位为 0（测试基线；CG_RESET_WALLET=0 可关闭）');
  } catch (e) {
    console.log('[sweep] 钱包复位失败:', String(e.message).split('\n')[0]);
  }
}

// 清理测试影像残留（1x1 占位图）：防止 upload/media 下堆积上千个无效文件
if (process.env.CG_CLEAN_MEDIA !== '0') {
  try {
    const paths = listTestMedia();
    let removed = 0;
    for (const rel of paths) {
      const abs = join(MEDIA_ROOT, rel);
      if (existsSync(abs)) { rmSync(abs, { force: true }); removed++; }
    }
    // 先删文件再删行：文件删除若有遗漏，DB 行保留可下次再清，不留孤儿
    deleteTestMediaRows();
    console.log(`[sweep] 测试影像已清理：${removed}/${paths.length} 个文件（CG_CLEAN_MEDIA=0 可关闭）`);
  } catch (e) {
    console.log('[sweep] 影像清理失败:', String(e.message).split('\n')[0]);
  }
}

// 测试师傅在班供给：自动派单要求「在线 + 今日在班 + 资质通过」，
// dev 排班若是 19:00-23:00，跑测试的时间（如凌晨）不在班 → 派单无人 → 详情 workerName 为 null。
// 这里把 W001 排班设为全周全天，保证任何时刻跑用例都能派单成功。
if (process.env.CG_ENSURE_DUTY !== '0') {
  try {
    const atoken = await adminLogin();
    const body = [1, 2, 3, 4, 5, 6, 7].map((weekDay) => ({ weekDay, startMinute: 0, endMinute: 1439 }));
    const r = await api('PUT', '/admin-api/wash/worker/1/schedule', { token: atoken, body });
    console.log(`[sweep] 测试师傅 W001 排班已设为全周全天（CG_ENSURE_DUTY=0 可关闭）code=${r.code}`);
  } catch (e) {
    console.log('[sweep] 排班供给失败:', String(e.message).split('\n')[0]);
  }
}

// 测试格口补给：dev 种子仅 2 格，多状态并发用例放不下；幂等补到 6 格
if (process.env.CG_ENSURE_SLOTS !== '0') {
  try {
    ensureTestSlots();
    console.log('[sweep] 测试格口已补给（cabinet 304 → 6 格；CG_ENSURE_SLOTS=0 可关闭）');
  } catch (e) {
    console.log('[sweep] 格口补给失败:', String(e.message).split('\n')[0]);
  }
}

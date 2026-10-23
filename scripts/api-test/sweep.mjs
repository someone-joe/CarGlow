/**
 * 跑前清理：取消遗留的 [AUTOTEST] 待支付订单。
 * 识别走只读 SQL，取消走接口（释放格口 + 回补产能），不直接改库。
 */
import { autotestWaitPayOrders, resetDevWallet, SIDECAR_ENABLED } from './lib/sidecar.js';
import { getSession, cancelOrder } from './lib/factory.js';

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

// dev 钱包复位：保证钱包边界用例每轮可重复构造（详见 lib/sidecar.js 注释）
if (process.env.CG_RESET_WALLET !== '0') {
  try {
    resetDevWallet();
    console.log('[sweep] dev 钱包已复位为 0（测试基线；CG_RESET_WALLET=0 可关闭）');
  } catch (e) {
    console.log('[sweep] 钱包复位失败:', String(e.message).split('\n')[0]);
  }
}

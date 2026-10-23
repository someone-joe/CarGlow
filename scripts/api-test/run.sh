#!/usr/bin/env bash
# 接口自动化测试入口（零依赖：node:test + 内置 fetch）。
#
# 用法：
#   bash scripts/api-test/run.sh              # 清理 + 产能扩容 + 全量用例 + 恢复产能
#   bash scripts/api-test/run.sh --no-sweep   # 跳过清理与产能步骤，快速重跑
#   CG_API_BASE=http://host:port bash scripts/api-test/run.sh   # 指向其他环境
#
# 前置：后端已启动（默认 http://127.0.0.1:8080）；MySQL/Redis 容器在跑（清理与副作用断言用）。
set -u
cd "$(dirname "$0")/../.."

if [ "${1:-}" = "--no-sweep" ]; then
  echo "===== 快速模式：跳过清理与产能步骤 ====="
  node --test --test-concurrency=1 scripts/api-test/cases/
  exit $?
fi

echo "===== 1/4 跑前清理：遗留 [AUTOTEST] 待支付订单 ====="
node scripts/api-test/sweep.mjs || echo "（清理跳过，继续执行）"
echo
echo "===== 2/4 测试窗口产能扩容（跑完自动恢复） ====="
node scripts/api-test/capacity.mjs up || echo "（扩容跳过，继续执行）"
echo
echo "===== 3/4 执行用例（串行） ====="
node --test --test-concurrency=1 scripts/api-test/cases/
STATUS=$?
echo
echo "===== 4/4 恢复产能 ====="
node scripts/api-test/capacity.mjs down || true
echo
if [ $STATUS -eq 0 ]; then
  echo "===== 全部通过 ====="
else
  echo "===== 存在失败（见上方详情） ====="
fi
exit $STATUS

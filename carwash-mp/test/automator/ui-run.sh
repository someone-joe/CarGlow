#!/usr/bin/env bash
# UI 用例 runner：遍历 cases/ui-*.sh，汇总失败文件数，非零退出码（CI 门禁）。
# 用法：bash carwash-mp/test/automator/ui-run.sh
set -u
cd "$(dirname "$0")"
source common.sh

# 冷启动预热：wechatide 连接首跳偶发未就绪（元素查询 0），先暖一次 home 再正式跑
ui_navigate "/pages/home/home" switchTab
sleep "$CG_RENDER_WAIT"

failed=0; run=0
for f in cases/ui-*.sh; do
  [ -f "$f" ] || continue
  run=$((run + 1))
  echo "===== $f ====="
  # 每用例前重置导航栈到首页：清空 navigateTo 累积栈 + 稳定 wechatide「当前页」跟踪，
  # 避免上一用例遗留页面导致 querySelectorAll 命中旧页/返回 -1
  ui_navigate "/pages/home/home" switchTab
  sleep "$CG_RENDER_WAIT"
  if bash "$f"; then :; else failed=$((failed + 1)); fi
done
echo "===== UI RUN: $run 用例文件, $failed 失败 ====="
# 全局清理：取消本批造数订单，释放格口/产能，避免跨轮累积导致 B2003
ui_reset || true
[ "$failed" -eq 0 ]

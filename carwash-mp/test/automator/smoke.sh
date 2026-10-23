#!/usr/bin/env bash
# C 端冒烟测试（微信开发者工具官方 wechatide 自动化 CLI）。
#
# 验证目标：三个 tab 页能打开、关键结构渲染出来。纯 UI 冒烟，不写业务数据。
#
# 前置条件（一次性）：
#   1. 微信开发者工具「设置 → 安全 → 服务端口」已开启
#      （本机已通过配置文件开启：~/Library/Application Support/微信开发者工具/*/WeappLocalData/ls_*.json
#       的 security.enableServicePort=true；若失效，在 GUI 里手动开）
#   2. wechatide CLI 已安装（/usr/local/bin/wechatide）且已完成一次授权
#      （首次调用会弹窗，需在 IDE 里点同意；之后同 clientName 不再询问）
#   3. carwash-mp/src/manifest.json 的 mp-weixin.appid 为有效 appid
#      （当前为接口测试号 wx92e7a7507d5dbed5；touristappid/空 appid 会被 IDE 在线校验拒绝）
#   4. 项目窗口已打开：wechatide -c <clientName> open_project_window --project <dist 目录>
#
# 运行：bash carwash-mp/test/automator/smoke.sh
set -u

PROJECT="${CG_MP_PROJECT:-/Users/joe/CarGlow/carwash-mp/dist/dev/mp-weixin}"
CLIENT="${CG_WECHATIDE_CLIENT:-CodeBuddy}"
SHOT_DIR="$(cd "$(dirname "$0")" && pwd)/screenshots"
RENDER_WAIT="${CG_RENDER_WAIT:-2}"

mkdir -p "$SHOT_DIR"

# 每项: "页面名|路由|选择器1,选择器2,..."
CASES=(
  "首页|/pages/home/home|.home-nav-title,.home-entries,.home-flow-item"
  "订单列表|/pages/orders/orders|.cg-tabs,.cg-tab"
  "我的|/pages/mine/mine|.hero-name,.cg-list-row"
)

W="wechatide -c $CLIENT"
failed=0

query_count() {
  # 返回匹配元素个数；查询失败返回 -1
  local out
  out=$($W automation_page_action --project "$PROJECT" --action querySelectorAll --selector "$1" 2>/dev/null)
  if echo "$out" | grep -q '"success": true'; then
    echo "$out" | grep -c '"elementId"'
  else
    echo "-1"
  fi
}

echo "[smoke] project=$PROJECT client=$CLIENT"

for entry in "${CASES[@]}"; do
  IFS='|' read -r name url sels <<< "$entry"
  nav_out=$($W automation_navigate --project "$PROJECT" --action switchTab --url "$url" 2>/dev/null)
  if ! echo "$nav_out" | grep -q '"success": true'; then
    echo "FAIL $name 导航失败: $url"
    failed=$((failed + 1))
    continue
  fi
  sleep "$RENDER_WAIT"

  IFS=',' read -ra sel_arr <<< "$sels"
  page_failed=0
  for sel in "${sel_arr[@]}"; do
    n=$(query_count "$sel")
    if [ "$n" -gt 0 ] 2>/dev/null; then
      echo "PASS $name $sel ($n 个元素)"
    else
      echo "FAIL $name $sel (查询失败或未渲染)"
      page_failed=1
      failed=$((failed + 1))
    fi
  done

  shot="$SHOT_DIR/${url##*/}.png"
  if $W simulator_screenshot --project "$PROJECT" --path "$shot" >/dev/null 2>&1 && [ -s "$shot" ]; then
    echo "INFO $name 截图: $shot"
  else
    echo "WARN $name 截图失败（不影响断言结果）"
  fi

  [ "$page_failed" -eq 0 ] || echo "FAIL $name 存在未通过项"
done

if [ "$failed" -eq 0 ]; then
  echo "SMOKE PASS"
  exit 0
else
  echo "SMOKE FAIL（$failed 项未通过）"
  exit 1
fi

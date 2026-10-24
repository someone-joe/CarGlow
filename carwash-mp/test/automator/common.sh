#!/usr/bin/env bash
# UI 自动化共用 helper（微信开发者工具官方 wechatide automator）。
# 被 cases/*.sh 与 ui-run.sh source；不要直接执行。
[ -n "${_CG_UI_COMMON:-}" ] && return
_CG_UI_COMMON=1

CG_MP_PROJECT="${CG_MP_PROJECT:-/Users/joe/CarGlow/carwash-mp/dist/dev/mp-weixin}"
CG_WECHATIDE_CLIENT="${CG_WECHATIDE_CLIENT:-CodeBuddy}"
CG_RENDER_WAIT="${CG_RENDER_WAIT:-2}"
CG_API_BASE="${CG_API_BASE:-http://localhost:8080}"
_SHOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/screenshots"
mkdir -p "$_SHOT_DIR"
WCLI="wechatide -c $CG_WECHATIDE_CLIENT"

# 导航：$1=url $2=switchTab|navigateTo（默认 switchTab）
ui_navigate() {
  $WCLI automation_navigate --project "$CG_MP_PROJECT" --action "${2:-switchTab}" --url "$1" 2>/dev/null
}

# 查询元素个数：$1=selector；成功返回个数，失败（查询异常/未渲染）返回 -1
ui_query_count() {
  local out
  out=$($WCLI automation_page_action --project "$CG_MP_PROJECT" --action querySelectorAll --selector "$1" 2>/dev/null)
  if echo "$out" | grep -q '"success": true'; then
    echo "$out" | grep -c '"elementId"'
  else
    echo "-1"
  fi
}

# 断言元素存在：$1=用例名 $2=selector [$3=最少个数，默认 1]；返回 0/1
ui_assert() {
  local min="${3:-1}" n
  n=$(ui_query_count "$2")
  if [ "$n" -ge "$min" ] 2>/dev/null; then
    echo "PASS $1 $2 ($n)"
    return 0
  fi
  echo "FAIL $1 $2 (count=$n)"
  return 1
}

# 点击：$1=selector（只用 automation_element_action，不走坐标）
ui_tap() {
  $WCLI automation_element_action --project "$CG_MP_PROJECT" --action tap --selector "$1" 2>/dev/null
}

# 截图取证：$1=文件名（不含后缀）
ui_screenshot() {
  $WCLI simulator_screenshot --project "$CG_MP_PROJECT" --path "$_SHOT_DIR/$1.png" >/dev/null 2>&1 \
    && echo "INFO 截图: $_SHOT_DIR/$1.png" \
    || echo "WARN 截图失败 $1"
}

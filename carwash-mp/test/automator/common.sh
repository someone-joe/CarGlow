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

# 查询元素个数：$1=selector；成功返回个数，失败（查询异常/未渲染/会话抖动）返回 -1
# wechatide 在连续操作后偶发瞬时返回非 success（环境抖动），故失败重试 3 次吸收
ui_query_count() {
  local out i
  for i in 1 2 3; do
    out=$($WCLI automation_page_action --project "$CG_MP_PROJECT" --action querySelectorAll --selector "$1" 2>/dev/null)
    if echo "$out" | grep -q '"success": true'; then
      echo "$out" | grep -c '"elementId"'
      return 0
    fi
    sleep 1
  done
  echo "-1"
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

# 等待元素出现（wechatide 导航后"当前页"偶发滞后，需轮询而非一次性查询）。
#   $1=selector $2=超时秒(默认8) 出现返回0，超时返回1
# 用法：ui_wait_for ".cg-page.x" 8 || { echo "FAIL ..."; exit 1; }
ui_wait_for() {
  local sel="$1" t="${2:-8}" i n
  for i in $(seq 1 "$t"); do
    n=$(ui_query_count "$sel")
    if [ "$n" != "-1" ] && [ "$n" -ge 1 ] 2>/dev/null; then
      echo "INFO 等到 $sel (${i}s)"
      return 0
    fi
    sleep 1
  done
  echo "WARN 超时未等到 $sel (${t}s)"
  return 1
}

# 截图取证：$1=文件名（不含后缀）
ui_screenshot() {
  $WCLI simulator_screenshot --project "$CG_MP_PROJECT" --path "$_SHOT_DIR/$1.png" >/dev/null 2>&1 \
    && echo "INFO 截图: $_SHOT_DIR/$1.png" \
    || echo "WARN 截图失败 $1"
}

# 造数 helper（详见 seed.mjs 注释）：把订单建在 dev-openid 会员名下并推进到目标状态，
# 与 wechatide 模拟器登录的会员是同一个，UI 用例可直接看到。
#   $1=STATE(WAIT_PAY|WAIT_KEY|KEY_IN|PICKING|TO_STATION|WASHING|QC|WAIT_RETURN|
#            RETURNING|RETURNED|WAIT_REVIEW|FINISHED|SERVING|CANCELLED|REFUNDED) $2=count(默认1)
#   返回：空格分隔的 orderNo（stdout），可直接 $(ui_seed ...) 捕获
ui_seed() {
  node "$_AUTOMATOR/seed.mjs" "$1" "${2:-1}"
}
# 清理本 helper 造的订单（读 .seeded 逐单取消）
ui_reset() {
  node "$_AUTOMATOR/cleanup-seed.mjs"
}

# wechatide 的 SelectorQuery 不穿透自定义组件边界：组件内部节点（如 VehiclePicker 的 .vp-add）
# 用 querySelectorAll 一律返回 0。只能退而用 outerWxml 文本判定「有没有渲染」——
# 注意：只验存在性，组件内部节点不可点击、不可取文本。
# $1=页面根 selector $2=期望出现的字符串（class 名等）
ui_wxml_has() {
  $WCLI automation_element_action --project "$CG_MP_PROJECT" --action outerWxml --selector "$1" 2>/dev/null | grep -q "$2"
}

# 输入框写入 / 读值（wechatide：input 需 --value；value 返回 result 字段的裸字符串）
ui_input() {
  $WCLI automation_element_action --project "$CG_MP_PROJECT" --action input --selector "$1" --value "$2" >/dev/null 2>&1
}
ui_value() {
  $WCLI automation_element_action --project "$CG_MP_PROJECT" --action value --selector "$1" 2>/dev/null \
    | sed -n 's/.*"result": "\(.*\)"[,]*$/\1/p' | head -1
}

# 断言元素不存在：$1=用例名 $2=selector；为 0 则通过（查询失败 -1 视为失败）
ui_assert_absent() {
  local n
  n=$(ui_query_count "$2")
  if [ "$n" = "0" ]; then
    echo "PASS $1 已消失 $2"
    return 0
  fi
  echo "FAIL $1 应消失但仍在 $2 (count=$n)"
  return 1
}

# 会员侧造数 helper（详见 data.mjs 注释）：券/余额/车辆，同样落在 dev-openid 会员名下。
#   coupon          确保有 UNUSED 券，打印 couponUserId
#   wallet          可用余额（分）
#   order-pay <no>  订单应付金额（分）
#   vehicles[:clear|:restore]
ui_data() {
  node "$_AUTOMATOR/data.mjs" "$@"
}

# automator 目录绝对路径（供 seed 脚本定位，不依赖调用方 cwd）
_AUTOMATOR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

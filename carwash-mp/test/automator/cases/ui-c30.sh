#!/usr/bin/env bash
# UI-C30 10002 自动续登（C 端 P0，[造数] WAIT_PAY 订单）
# 场景：本地 token 失效 → 请求层收到 10002 → 清 token → ensureLogin 静默续登 → 重试原请求，
#       页面无感知恢复（不落「订单不存在或无权查看」空态）。
# 手段：wechatide automation_evaluate 把 storage 的 carglow_token 篡改为无效值，再进需鉴权的详情页。
# 依赖 D3 修复：ensureLogin 登录超时 + force 强制重登 + request.ts 静态引入 ensureLogin
#   （去掉脆弱的动态 import 分块竞争），续登落空时再强制重登一次兜底。
source "$(dirname "$0")/../common.sh"
NAME="UI-C30 10002自动续登"
fails=0

ui_reset
ORDERNO=$(ui_seed WAIT_PAY)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }

# 回首页清导航栈，再篡改 token（模拟本地 token 失效/过期）
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
$WCLI automation_evaluate --project "$CG_MP_PROJECT" --fn-source \
  'function(){ wx.setStorageSync("carglow_token","tampered-invalid-token"); return "set"; }' >/dev/null 2>&1
sleep 1

# 进入需鉴权的订单详情：首次带无效 token → 10002 → 静默续登 → 重试
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
ui_wait_for ".cg-page.order-detail" 12 || { echo "FAIL $NAME[详情页渲染]"; fails=$((fails + 1)); }

# 关键：不落空态（续登失败会落到「订单不存在或无权查看」空态）
n=$(ui_query_count ".cg-empty")
[ "$n" = "0" ] \
  && echo "PASS $NAME[静默续登恢复，未落空态]" \
  || { echo "FAIL $NAME[静默续登恢复] 落到空态 (empty=$n)"; fails=$((fails + 1)); }

# 佐证（信息级，不作门禁）：token 已被静默换回有效值（非篡改值、非空）
TOK=$($WCLI automation_evaluate --project "$CG_MP_PROJECT" --fn-source \
  'function(){ return wx.getStorageSync("carglow_token") }' 2>/dev/null \
  | python3 -c 'import sys,json
try:
    print(json.load(sys.stdin)["result"]["result"]["result"])
except Exception:
    print("")' 2>/dev/null)
if [ -n "$TOK" ] && [ "$TOK" != "tampered-invalid-token" ]; then
  echo "INFO $NAME[token已静默续登] (${TOK:0:8}…)"
else
  echo "INFO $NAME[token读取异常或未续登] (tok=$TOK) —— 以「未落空态」为准"
fi

ui_screenshot "c30-relogin"
exit $fails

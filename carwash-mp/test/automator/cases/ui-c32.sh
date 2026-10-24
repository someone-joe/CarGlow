#!/usr/bin/env bash
# UI-C32 杀进程恢复（C 端 P1，[造数] WAIT_PAY 订单）
# wechatide 的 simulator_refresh 等价于杀进程重进（重新加载小程序）：
# 预期 —— 登录态保留（重进后需鉴权的详情页仍能渲染）；未提交表单按实现清空（下单草稿是内存单例）
source "$(dirname "$0")/../common.sh"
NAME="UI-C32 杀进程恢复"
fails=0

ui_reset
ORDERNO=$(ui_seed WAIT_PAY)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }

# 造一个「未提交」的下单态：填了备注但没点确认下单
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页]"; fails=$((fails + 1)); }
ui_input ".order-park-input" "杀进程前填的备注"; sleep 1
BEFORE=$(ui_value ".order-park-input")
[ -n "$BEFORE" ] \
  && echo "PASS $NAME[填了未提交表单] ($BEFORE)" \
  || { echo "FAIL $NAME[填了未提交表单] (空)"; fails=$((fails + 1)); }

# 杀进程重进
$WCLI simulator_refresh --project "$CG_MP_PROJECT" >/dev/null 2>&1
sleep 10

# 登录态保留：重新进入后仍能看到需鉴权的订单详情（未落到空态）
ui_wait_for ".cg-page.home" 8 || true
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order-detail" 8 || { echo "FAIL $NAME[详情页]"; fails=$((fails + 1)); }
n=$(ui_query_count ".cg-empty")
[ "$n" = "0" ] \
  && echo "PASS $NAME[登录态保留]" \
  || { echo "FAIL $NAME[登录态保留] 详情页落空态 (empty=$n)"; fails=$((fails + 1)); }

# 未提交表单清空（orderDraft 为内存单例，不持久化，符合实现口径）
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页2]"; fails=$((fails + 1)); }
AFTER=$(ui_value ".order-park-input")
[ -z "$AFTER" ] \
  && echo "PASS $NAME[未提交表单已清空]" \
  || { echo "FAIL $NAME[未提交表单已清空] ($AFTER)"; fails=$((fails + 1)); }

ui_screenshot "c32-restart"
exit $fails

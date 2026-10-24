#!/usr/bin/env bash
# UI-C21 订单操作按钮矩阵（C 端 P0，[造数] 各状态各一）
# 按状态显示主按钮（前端不自行推断，以 mainAction 为准）：
# WAIT_PAY→去支付 WAIT_KEY→存钥匙 WAIT_REVIEW→评价 FINISHED→再来一单(实测后端 REORDER)
source "$(dirname "$0")/../common.sh"
NAME="UI-C21 订单操作按钮矩阵"
fails=0
ui_reset
P=$(ui_seed WAIT_PAY)
K=$(ui_seed WAIT_KEY)
R=$(ui_seed WAIT_REVIEW)
F=$(ui_seed FINISHED)
check() {
  local no="$1" want_primary="$2" label="$3" n
  ui_navigate "/pages/order-detail/order-detail?orderNo=$no" navigateTo
  sleep "$CG_RENDER_WAIT"
  if [ "$want_primary" = "1" ]; then
    ui_assert "$NAME[$label]主按钮" ".foot-btn.primary" 1 || fails=$((fails + 1))
  else
    n=$(ui_query_count ".foot-btn.primary")
    if [ "$n" = "0" ]; then echo "PASS $NAME[$label]无主按钮"; else echo "FAIL $NAME[$label]无主按钮 (n=$n)"; fails=$((fails + 1)); fi
    ui_assert "$NAME[$label]有子按钮" ".foot-btn" 1 || fails=$((fails + 1))
  fi
}
check "$P" 1 "WAIT_PAY"
check "$K" 1 "WAIT_KEY"
check "$R" 1 "WAIT_REVIEW"
check "$F" 1 "FINISHED"
ui_screenshot "c21-matrix"
exit $fails

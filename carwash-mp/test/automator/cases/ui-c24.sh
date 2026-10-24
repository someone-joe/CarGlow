#!/usr/bin/env bash
# UI-C24 取消订单入口（C 端 P0，[造数] WAIT_PAY）
# 待支付订单详情同时存在「去支付」主按钮与「取消」子按钮；已开洗不显示取消入口
source "$(dirname "$0")/../common.sh"
NAME="UI-C24 取消订单入口"
fails=0
ui_reset
ORDERNO=$(ui_seed WAIT_PAY)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[去支付]" ".foot-btn.primary" 1 || fails=$((fails + 1))
ui_assert "$NAME[取消子按钮]" ".foot-btn" 1 || fails=$((fails + 1))
ui_screenshot "c24-cancel"
exit $fails

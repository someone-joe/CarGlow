#!/usr/bin/env bash
# UI-C11 支付页倒计时（C 端 P0，[造数] WAIT_PAY）
# 直跳详情 → 去支付 → 支付页：倒计时/支付方式/支付条/费用明细均渲染
source "$(dirname "$0")/../common.sh"
NAME="UI-C11 支付页倒计时"
fails=0
ui_reset
ORDERNO=$(ui_seed WAIT_PAY)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[去支付]" ".foot-btn.primary" 1 || fails=$((fails + 1))
ui_tap ".foot-btn.primary"; sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[倒计时]" ".cd-time" 1 || fails=$((fails + 1))
ui_assert "$NAME[支付方式x2]" ".pt-row" 2 || fails=$((fails + 1))
ui_assert "$NAME[支付条]" ".pay-btn" 1 || fails=$((fails + 1))
ui_assert "$NAME[费用明细行]" ".row" 2 || fails=$((fails + 1))
ui_screenshot "c11-pay"
exit $fails

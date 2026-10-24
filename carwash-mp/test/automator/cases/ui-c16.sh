#!/usr/bin/env bash
# UI-C16 开箱取钥匙（C 端 P0，[造数] RETURNED）
# 直跳详情 → 取钥匙主按钮 → 开箱页(取件码/我已取出) → 取回后订单待评价(WAIT_REVIEW)
source "$(dirname "$0")/../common.sh"
NAME="UI-C16 开箱取钥匙"
fails=0
ui_reset
ORDERNO=$(ui_seed RETURNED)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[取钥匙入口]" ".foot-btn.primary" 1 || fails=$((fails + 1))
ui_tap ".foot-btn.primary"
ui_wait_for ".cg-page.open-box" 8 || { echo "FAIL $NAME[open-box]"; fails=$((fails + 1)); }
ui_wait_for ".code-value" 8 || { echo "FAIL $NAME[取件码]"; fails=$((fails + 1)); }
ui_wait_for ".taken-btn" 8 || { echo "FAIL $NAME[我已取出]"; fails=$((fails + 1)); }
[ "$fails" -gt 0 ] && exit $fails
ui_tap ".taken-btn"; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[→WAIT_REVIEW]" ".foot-btn.primary" 1 || fails=$((fails + 1))
ui_screenshot "c16-take"
exit $fails

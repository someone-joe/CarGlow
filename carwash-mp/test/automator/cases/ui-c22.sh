#!/usr/bin/env bash
# UI-C22 再来一单（C 端 P0，[造数] FINISHED）
# 直跳详情 → 点「再来一单」→ 带原服务/车辆信息回下单页(index)
source "$(dirname "$0")/../common.sh"
NAME="UI-C22 再来一单"
fails=0
ui_reset
ORDERNO=$(ui_seed FINISHED)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[再来一单入口]" ".foot-btn" 1 || fails=$((fails + 1))
ui_tap ".foot-btn"; sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[回下单页]" ".cg-page.order" 1 || fails=$((fails + 1))
ui_screenshot "c22-reorder"
exit $fails

#!/usr/bin/env bash
# UI-C25 紧急取钥匙入口（C 端 P0，[造数] KEY_IN）
# 已存钥匙(服务中)订单详情展示操作按钮（含紧急取钥匙子按钮）
source "$(dirname "$0")/../common.sh"
NAME="UI-C25 紧急取钥匙入口"
fails=0
ui_reset
ORDERNO=$(ui_seed KEY_IN)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[操作按钮]" ".foot-btn" 1 || fails=$((fails + 1))
ui_screenshot "c25-emergency"
exit $fails

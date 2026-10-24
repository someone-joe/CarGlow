#!/usr/bin/env bash
# UI-C17 订单列表三 Tab（C 端 P0）
source "$(dirname "$0")/../common.sh"
NAME="UI-C17 订单列表三 Tab"
fails=0
ui_navigate "/pages/orders/orders" switchTab
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME" ".cg-tabs" || fails=$((fails + 1))   # 三 Tab 容器
ui_assert "$NAME" ".cg-tab" 3 || fails=$((fails + 1)) # 待处理/服务中/已完成 三 Tab
ui_screenshot "c17-orders"
exit $fails

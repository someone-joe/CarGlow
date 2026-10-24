#!/usr/bin/env bash
# UI-C18 订单详情时间轴（C 端 P0，[造数] FINISHED 跑完主链路）
# 直跳详情：8 节点全亮、当前节点高亮
source "$(dirname "$0")/../common.sh"
NAME="UI-C18 订单详情时间轴"
fails=0
ui_reset
ORDERNO=$(ui_seed FINISHED)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[detail]" ".cg-page.order-detail" 1 || fails=$((fails + 1))
ui_assert "$NAME[8节点]" ".node" 8 || fails=$((fails + 1))
ui_assert "$NAME[已达成节点]" ".dot-reached" 1 || fails=$((fails + 1))
ui_assert "$NAME[当前节点高亮]" ".dot-current" 1 || fails=$((fails + 1))
ui_screenshot "c18-timeline"
exit $fails

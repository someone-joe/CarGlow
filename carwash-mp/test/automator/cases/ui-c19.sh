#!/usr/bin/env bash
# UI-C19 详情费用与订单信息（C 端 P0，[造数] FINISHED）
# 直跳详情：费用明细(原价/折扣/实付)、订单信息(接单师傅/柜机/承诺还车)、服务进度面板均渲染
source "$(dirname "$0")/../common.sh"
NAME="UI-C19 详情费用与订单信息"
fails=0
ui_reset
ORDERNO=$(ui_seed FINISHED)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[三面板]" ".panel-title" 3 || fails=$((fails + 1))
ui_assert "$NAME[明细行]" ".row" 3 || fails=$((fails + 1))
ui_assert "$NAME[进度节点]" ".node" 8 || fails=$((fails + 1))
ui_assert "$NAME[信息值]" ".value" 1 || fails=$((fails + 1))
ui_screenshot "c19-detail"
exit $fails

#!/usr/bin/env bash
# UI-C23 评价闭环（C 端 P0，[造数] WAIT_REVIEW）
# 直跳详情 → 去评价 → 5 星提交 → 订单推进 FINISHED（详情显示「再来一单」）
source "$(dirname "$0")/../common.sh"
NAME="UI-C23 评价闭环"
fails=0
ui_reset
ORDERNO=$(ui_seed WAIT_REVIEW)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[detail]" ".cg-page.order-detail" || fails=$((fails + 1))
ui_assert "$NAME[去评价]" ".foot-btn.primary" || fails=$((fails + 1))
ui_tap ".foot-btn.primary"                                  # → 评价页
ui_wait_for ".cg-page.review" 8 || { echo "FAIL $NAME[review]"; fails=$((fails + 1)); }
ui_wait_for ".stars" 8 || { echo "FAIL $NAME[stars]"; fails=$((fails + 1)); }
[ "$fails" -gt 0 ] && exit $fails
ui_tap ".stars .star-5"; sleep 1                              # 5 星（无需原因）
ui_tap ".btn"; sleep "$CG_RENDER_WAIT"                        # 提交评价
# 评价页 navigateBack 回到详情，但详情用 onLoad 不自动刷新；重新进入确认 FINISHED
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[FINISHED]" ".cg-page.order-detail" || fails=$((fails + 1))
ui_assert "$NAME[再来一单主按钮]" ".foot-btn.primary" || fails=$((fails + 1))
ui_screenshot "c23-review"
exit $fails

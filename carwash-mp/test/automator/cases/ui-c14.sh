#!/usr/bin/env bash
# UI-C14 支付成功 → 待存钥匙（C 端 P0，[造数] WAIT_PAY）
# 直跳订单详情 → 去支付 → 立即支付(mock) → 订单态 WAIT_KEY → 存钥匙指引页(open-box)
source "$(dirname "$0")/../common.sh"
NAME="UI-C14 支付成功→待存钥匙"
fails=0
ui_reset
ORDERNO=$(ui_seed WAIT_PAY)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[detail]" ".cg-page.order-detail" || fails=$((fails + 1))
ui_assert "$NAME[去支付]" ".foot-btn.primary" || fails=$((fails + 1))
ui_tap ".foot-btn.primary"; sleep "$CG_RENDER_WAIT"          # → 支付页
ui_assert "$NAME[pay]" ".pay-btn" || fails=$((fails + 1))
ui_tap ".pay-btn"; sleep 3                                   # mock 支付 → redirectTo 详情(WAIT_KEY)
ui_assert "$NAME[WAIT_KEY]" ".cg-page.order-detail" || fails=$((fails + 1))
ui_assert "$NAME[存钥匙]" ".foot-btn.primary" || fails=$((fails + 1))
ui_tap ".foot-btn.primary"; sleep "$CG_RENDER_WAIT"          # → 存钥匙指引页
ui_assert "$NAME[open-box]" ".cg-page.open-box" || fails=$((fails + 1))
ui_assert "$NAME[开箱码]" ".code-value" || fails=$((fails + 1))
ui_screenshot "c14-pay"
exit $fails

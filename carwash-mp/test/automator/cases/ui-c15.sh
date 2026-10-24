#!/usr/bin/env bash
# UI-C15 存钥匙指引页（C 端 P0，[造数] WAIT_KEY）
# 直跳详情 → 存钥匙主按钮 → 开箱页：开箱码 + 复制按钮渲染
source "$(dirname "$0")/../common.sh"
NAME="UI-C15 存钥匙指引页"
fails=0
ui_reset
ORDERNO=$(ui_seed WAIT_KEY)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }
ui_navigate "/pages/order-detail/order-detail?orderNo=$ORDERNO" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[存钥匙入口]" ".foot-btn.primary" 1 || fails=$((fails + 1))
ui_tap ".foot-btn.primary"; sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[open-box]" ".cg-page.open-box" 1 || fails=$((fails + 1))
ui_assert "$NAME[开箱码]" ".code-value" 1 || fails=$((fails + 1))
ui_assert "$NAME[复制开箱码]" ".copy" 1 || fails=$((fails + 1))
ui_screenshot "c15-deposit"
exit $fails

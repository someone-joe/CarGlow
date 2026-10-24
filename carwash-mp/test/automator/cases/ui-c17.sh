#!/usr/bin/env bash
# UI-C17 订单列表三 Tab（C 端 P0）+ 验证造数在 UI 真实可见
# 用 ui_seed 造 待处理/服务中/已完成 各一单，断言三 Tab 分组正确且卡片可见。
source "$(dirname "$0")/../common.sh"
NAME="UI-C17 订单列表三 Tab"
fails=0
ui_reset
ui_seed WAIT_PAY >/dev/null
ui_seed SERVING >/dev/null
ui_seed CANCELLED >/dev/null
ui_navigate "/pages/orders/orders" switchTab
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME" ".cg-tabs" || fails=$((fails + 1))
ui_assert "$NAME" ".cg-tab" 3 || fails=$((fails + 1))
# 待处理 Tab（默认）应见 seeded WAIT_PAY 卡
ui_assert "$NAME[PENDING]" ".card" 1 || fails=$((fails + 1))
# 切到 服务中 Tab（伪类引擎不可用，用 tab-KEY 锚点）
ui_tap ".tab-SERVING"; sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[SERVING]" ".card" 1 || fails=$((fails + 1))
# 切到 已完成 Tab
ui_tap ".tab-DONE"; sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[DONE]" ".card" 1 || fails=$((fails + 1))
ui_screenshot "c17-orders"
exit $fails

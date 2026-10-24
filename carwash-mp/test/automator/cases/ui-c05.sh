#!/usr/bin/env bash
# UI-C05 服务列表渲染（C 端 P0）
# 说明：重构后「洗车/美容双 Tab」改为按分类分区渲染，断言分区与服务卡即可。
source "$(dirname "$0")/../common.sh"
NAME="UI-C05 服务列表渲染"
fails=0
ui_navigate "/pages/services/services" switchTab
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME" ".cg-page.services" || fails=$((fails + 1))   # 页面根
ui_assert "$NAME" ".hero-title" || fails=$((fails + 1))         # 站点 Hero 标题
ui_assert "$NAME" ".section-head" || fails=$((fails + 1))        # 分类分区（双 Tab 重构为分区）
ui_assert "$NAME" ".cg-card.svc" 1 || fails=$((fails + 1))       # 服务卡
ui_assert "$NAME" ".svc-price-now" 1 || fails=$((fails + 1))     # 价格（分→元）
ui_assert "$NAME" ".svc-time-pill" 1 || fails=$((fails + 1))     # 工时 pill
ui_screenshot "c05-services"
exit $fails

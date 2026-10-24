#!/usr/bin/env bash
# UI-C06 服务详情渲染（C 端 P0）：经由服务列表「详情 ›」入口进入，避免硬编码 serviceId。
source "$(dirname "$0")/../common.sh"
NAME="UI-C06 服务详情渲染"
fails=0
ui_navigate "/pages/services/services" switchTab
sleep "$CG_RENDER_WAIT"
# 详情入口存在（无服务时列表为空，属造数问题，非本用例职责）
if [ "$(ui_query_count ".svc-detail-link")" -lt 1 ] 2>/dev/null; then
  echo "SKIP $NAME 无服务可进详情（需造数）"
  ui_screenshot "c06-nosvc"
  exit 0
fi
ui_tap ".svc-detail-link"
sleep 2
ui_assert "$NAME" ".cg-page.service-detail" || fails=$((fails + 1))  # 详情页根
ui_assert "$NAME" ".name" 1 || fails=$((fails + 1))                  # 服务名
ui_assert "$NAME" ".bottom-bar" 1 || fails=$((fails + 1))            # 底部下单条
ui_assert "$NAME" ".bottom-btn" 1 || fails=$((fails + 1))            # 立即下单按钮
ui_screenshot "c06-detail"
exit $fails

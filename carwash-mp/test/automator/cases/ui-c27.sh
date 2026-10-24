#!/usr/bin/env bash
# UI-C27 常用停车位置（C 端 P0）
source "$(dirname "$0")/../common.sh"
NAME="UI-C27 常用停车位置"
fails=0
ui_navigate "/pages/park-location/park-location" navigateTo
sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.park-location" 8 || { echo "FAIL $NAME 未进入停车位置页"; exit 1; }
ui_assert "$NAME" ".picker-row" 1 || fails=$((fails + 1))      # 所在小区选择
ui_assert "$NAME" ".input" 1 || fails=$((fails + 1))           # 楼栋/车位/补充输入
ui_assert "$NAME" ".btn-primary" 1 || fails=$((fails + 1))     # 添加按钮
ui_assert "$NAME" ".switch-row" 1 || fails=$((fails + 1))      # 默认位置开关
ui_screenshot "c27-park"
exit $fails

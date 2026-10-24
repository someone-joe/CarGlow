#!/usr/bin/env bash
# UI-C26 我的车辆管理（C 端 P0）
source "$(dirname "$0")/../common.sh"
NAME="UI-C26 我的车辆管理"
fails=0
ui_navigate "/pages/vehicle/vehicle" navigateTo
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME" ".cg-page.vehicle" || fails=$((fails + 1))
ui_assert "$NAME" ".add-bar" 1 || fails=$((fails + 1))        # 新增车辆入口
ui_assert "$NAME" ".panel-title" 1 || fails=$((fails + 1))     # 我的车辆（N）
ui_screenshot "c26-vehicle"
exit $fails

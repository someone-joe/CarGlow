#!/usr/bin/env bash
# UI-C02 首页渲染（C 端 P0）
source "$(dirname "$0")/../common.sh"
NAME="UI-C02 首页渲染"
fails=0
ui_navigate "/pages/home/home" switchTab
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME" ".home-nav-title" || fails=$((fails + 1))          # 站点抬头
ui_assert "$NAME" ".home-entries" || fails=$((fails + 1))            # 双服务入口
ui_assert "$NAME" ".home-flow-item" 3 || fails=$((fails + 1))        # 服务流程 3 项
ui_screenshot "c02-home"
exit $fails

#!/usr/bin/env bash
# UI-C28 我的（个人中心，C 端 P0）
source "$(dirname "$0")/../common.sh"
NAME="UI-C28 我的"
fails=0
ui_navigate "/pages/mine/mine" switchTab
sleep "$CG_RENDER_WAIT"
ui_assert "$NAME" ".hero-name" || fails=$((fails + 1))     # 头像区昵称
ui_assert "$NAME" ".cg-list-row" || fails=$((fails + 1))  # 功能列表行
ui_screenshot "c28-mine"
exit $fails

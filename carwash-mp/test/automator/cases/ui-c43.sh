#!/usr/bin/env bash
# UI-C43 tabBar 与返回栈（C 端 P1）
source "$(dirname "$0")/../common.sh"
NAME="UI-C43 tabBar 与返回栈"
fails=0
# 四 Tab 互切，每页稳定渲染
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[home]" ".home-nav-title" || fails=$((fails + 1))
ui_navigate "/pages/services/services" switchTab; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[services]" ".cg-page.services" || fails=$((fails + 1))
ui_navigate "/pages/orders/orders" switchTab; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[orders]" ".cg-tabs" || fails=$((fails + 1))
ui_navigate "/pages/mine/mine" switchTab; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[mine]" ".hero-name" || fails=$((fails + 1))
# 子页进入后返回：我的 → navigateTo 子页 → navigateBack 回我的（验证返回栈不乱）
ui_navigate "/pages/vehicle/vehicle" navigateTo; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[sub]" ".cg-page.vehicle" || fails=$((fails + 1))
ui_navigate "/pages/mine/mine" navigateBack; sleep 1; ui_assert "$NAME[back]" ".hero-name" || fails=$((fails + 1))
ui_screenshot "c43-tabs"
exit $fails

#!/usr/bin/env bash
# UI-C29 空状态全家桶（C 端 P1）：各数据页进入不白屏不报错。
# 注：真·空态文案断言需「新会员」造数，本用例仅做挂载级冒烟（页面组件成功渲染，非崩溃白屏）。
source "$(dirname "$0")/../common.sh"
NAME="UI-C29 空状态全家桶"
fails=0
ui_navigate "/pages/orders/orders" switchTab; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[orders]" ".cg-page" || fails=$((fails + 1))
ui_navigate "/pages/vehicle/vehicle" navigateTo; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[vehicle]" ".cg-page" || fails=$((fails + 1))
ui_navigate "/pages/coupons/coupons" navigateTo; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[coupons]" ".cg-page" || fails=$((fails + 1))
ui_navigate "/pages/cabinet/cabinet" navigateTo; sleep "$CG_RENDER_WAIT"; ui_assert "$NAME[cabinet]" ".cg-page" || fails=$((fails + 1))
ui_screenshot "c29-empty"
exit $fails

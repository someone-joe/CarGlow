#!/usr/bin/env bash
# UI-C01 冷启动静默登录（C 端 P0）
# 前置：清 token 后冷启动（本用例不造数，仅验证进入首页不白屏/不阻断）
source "$(dirname "$0")/../common.sh"
NAME="UI-C01 冷启动静默登录"
fails=0
ui_navigate "/pages/home/home" switchTab
sleep "$CG_RENDER_WAIT"
# 站点抬头渲染即证明静默登录成功进入首页（失败会有 toast 而非白屏）
ui_assert "$NAME" ".home-nav-title" || fails=$((fails + 1))
ui_screenshot "c01-home"
exit $fails

#!/usr/bin/env bash
# UI-C40 车损保障（保单）（C 端 P1）
# 入口可达曾是缺陷（PROGRESS 15.3），本例做回归：保障方案区 + 我的保单区必须都渲染
source "$(dirname "$0")/../common.sh"
NAME="UI-C40 车损保障"
fails=0

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/insurance/insurance" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.insurance" 8 || { echo "FAIL $NAME[保障页]"; fails=$((fails + 1)); }
ui_assert "$NAME[方案与保单两区]" ".cg-section-title" 2 || fails=$((fails + 1))

# 保障方案：有数据渲染卡片+投保按钮；无数据渲染空态（不许白屏）
p=$(ui_query_count ".product")
e=$(ui_query_count ".empty")
if [ "$p" -ge 1 ] 2>/dev/null; then
  echo "PASS $NAME[保障方案] (.product=$p)"
  ui_assert "$NAME[投保入口]" ".buy-btn" 1 || fails=$((fails + 1))
elif [ "$e" -ge 1 ] 2>/dev/null; then
  echo "PASS $NAME[保障方案空态] 文案存在"
else
  echo "FAIL $NAME[保障方案] 既无方案也无空态"
  fails=$((fails + 1))
fi

ui_screenshot "c40-insurance"
exit $fails

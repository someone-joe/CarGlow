#!/usr/bin/env bash
# UI-C10 下单-备注与条款（C 端 P0，不建单）
# 备注输入可用；未勾服务协议不让提交；勾选后可提交
source "$(dirname "$0")/../common.sh"
NAME="UI-C10 备注与条款"
fails=0

ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页]"; fails=$((fails + 1)); }
ui_assert "$NAME[备注输入]" ".order-park-input" 1 || fails=$((fails + 1))
ui_assert "$NAME[服务协议勾选]" ".order-agree" 1 || fails=$((fails + 1))

# 未勾条款 → 提交按钮禁用
ui_tap ".order-agree"; sleep 1                       # 先勾再取消勾，确保回到未勾不变体
ui_tap ".order-agree" ; sleep 1
n=$(ui_query_count ".cg-check.is-on")
[ "$n" = "0" ] && echo "PASS $NAME[未勾选态]" || { echo "FAIL $NAME[未勾选态] (n=$n)"; fails=$((fails + 1)); }
ui_assert "$NAME[未勾不可提交]" ".order-bar-btn.is-disabled" 1 || fails=$((fails + 1))

# 勾选后可提交
ui_tap ".order-agree"; sleep 1
ui_assert "$NAME[已勾选]" ".cg-check.is-on" 1 || fails=$((fails + 1))
ui_assert_absent "$NAME[勾选后可提交]" ".order-bar-btn.is-disabled" || fails=$((fails + 1))

# 备注可填写（写入后值能读回）
ui_input ".order-park-input" "地库 A 区 23 号"; sleep 1
v=$(ui_value ".order-park-input")
echo "$v" | grep -q "地库 A 区 23 号" \
  && echo "PASS $NAME[备注回填] $v" \
  || { echo "FAIL $NAME[备注回填] ($v)"; fails=$((fails + 1)); }

ui_screenshot "c10-remark"
exit $fails

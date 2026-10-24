#!/usr/bin/env bash
# UI-C39 售后与退款进度（C 端 P1，[造数] REFUNDED 订单）
# 售后工单 / 退款记录双 Tab；退款类展开可见退款单状态
source "$(dirname "$0")/../common.sh"
NAME="UI-C39 售后与退款进度"
fails=0

ui_reset
ORDERNO=$(ui_seed REFUNDED)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/aftersale/aftersale" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.aftersale" 8 || { echo "FAIL $NAME[售后页]"; fails=$((fails + 1)); }
ui_assert "$NAME[双Tab]" ".cg-tab" 2 || fails=$((fails + 1))

# 退款记录 Tab：seeded REFUNDED 订单应产生退款单
ui_tap ".tab-refund"; sleep "$CG_RENDER_WAIT"
n=$(ui_query_count ".card")
if [ "$n" -ge 1 ] 2>/dev/null; then
  echo "PASS $NAME[退款记录] (.card=$n)"
else
  echo "FAIL $NAME[退款记录] 无退款卡片 (应以 $ORDERNO 产生退款单)"
  fails=$((fails + 1))
fi
ui_assert "$NAME[退款单号/状态]" ".no" 1 || fails=$((fails + 1))
ui_assert "$NAME[退款状态徽标]" ".badge" 1 || fails=$((fails + 1))

# 售后工单 Tab：无工单时应给空态文案而非白屏
ui_tap ".tab-sale"; sleep "$CG_RENDER_WAIT"
s=$(ui_query_count ".card")
e=$(ui_query_count ".cg-empty")
if [ "$s" -ge 1 ] 2>/dev/null; then
  echo "PASS $NAME[售后工单] (.card=$s)"
elif [ "$e" -ge 1 ] 2>/dev/null; then
  echo "PASS $NAME[售后工单空态] 文案存在"
else
  echo "FAIL $NAME[售后工单] 既无卡片也无空态"
  fails=$((fails + 1))
fi

ui_screenshot "c39-aftersale"
exit $fails

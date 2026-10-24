#!/usr/bin/env bash
# UI-C37 储值充值（C 端 P1）
# 档位来自后端规则；自定义金额 <¥1 被拦（不产生充值单、余额不变）；有效充值后到账并返回
source "$(dirname "$0")/../common.sh"
NAME="UI-C37 储值充值"
fails=0

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/recharge/recharge" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.recharge" 8 || { echo "FAIL $NAME[充值页]"; fails=$((fails + 1)); }
ui_assert "$NAME[当前余额]" ".cur-value" 1 || fails=$((fails + 1))
ui_assert "$NAME[后端档位]" ".tier" 1 || fails=$((fails + 1))
ui_assert "$NAME[档位赠送]" ".tier-gift" 1 || fails=$((fails + 1))

# 选档位（选中态互斥）
ui_tap ".tier"; sleep 1
ui_assert "$NAME[选中档位]" ".tier.on" 1 || fails=$((fails + 1))

# 自定义 <¥1 应被拦：停留在充值页且余额不变
BEFORE=$(ui_data wallet 2>/dev/null)
ui_input ".custom-input" "0.5"; sleep 1
ui_tap ".submit"; sleep 3
ui_assert "$NAME[<¥1被拦仍在充值页]" ".cg-page.recharge" 1 || fails=$((fails + 1))
AFTER=$(ui_data wallet 2>/dev/null)
[ "$AFTER" = "$BEFORE" ] \
  && echo "PASS $NAME[<¥1未产生充值] (余额=$AFTER)" \
  || { echo "FAIL $NAME[<¥1未产生充值] ($BEFORE → $AFTER)"; fails=$((fails + 1)); }

# 有效自定义充值 ¥1：mock 到账后 navigateBack
ui_input ".custom-input" "1"; sleep 1
ui_tap ".submit"; sleep 4
ui_assert_absent "$NAME[充值成功后返回]" ".cg-page.recharge" || fails=$((fails + 1))
AFTER2=$(ui_data wallet 2>/dev/null)
[ "$AFTER2" -ge $((AFTER + 100)) ] 2>/dev/null \
  && echo "PASS $NAME[充值到账+1.00] ($AFTER → $AFTER2)" \
  || { echo "FAIL $NAME[充值到账+1.00] ($AFTER → $AFTER2)"; fails=$((fails + 1)); }

ui_screenshot "c37-recharge"
exit $fails

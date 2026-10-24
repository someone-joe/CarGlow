#!/usr/bin/env bash
# UI-C08 下单-机柜选择（C 端 P0，不建单）
# 进机柜页 → 九宫格展示空闲格口 → 选柜回下单页并回填柜名/空闲格口
source "$(dirname "$0")/../common.sh"
NAME="UI-C08 机柜选择"
fails=0

ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页]"; fails=$((fails + 1)); }

ui_navigate "/pages/cabinet/cabinet" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.cabinet" 8 || { echo "FAIL $NAME[机柜页]"; fails=$((fails + 1)); }
ui_assert "$NAME[柜卡片]" ".slot" 1 || fails=$((fails + 1))
ui_assert "$NAME[空闲格口数]" ".slot-free" 1 || fails=$((fails + 1))
# 满柜/离线柜置灰：是否存在取决于数据，只记录不断言
d=$(ui_query_count ".slot.disabled")
echo "INFO 置灰柜数量=$d"

# 选柜：必须点在可用柜上（满柜/离线禁选），pick 后 navigateBack
ui_tap ".slot.usable"; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[回下单页]"; fails=$((fails + 1)); }
ui_assert "$NAME[回填空闲格口]" ".order-row-hint" 1 || fails=$((fails + 1))
ui_assert_absent "$NAME[柜名已回填]" ".cg-row-value.is-placeholder" || fails=$((fails + 1))

ui_screenshot "c08-cabinet"
exit $fails

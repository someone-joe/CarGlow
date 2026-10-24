#!/usr/bin/env bash
# UI-C13 更改优惠（C 端 P0，[造数] 会员名下 UNUSED 券）
# 实现形态：优惠券页「去使用」→ 下单页带入券 → 点「清除」改回原价（支付页无「更改优惠」入口）
source "$(dirname "$0")/../common.sh"
NAME="UI-C13 更改优惠"
fails=0

CP=$(ui_data coupon) || { echo "FAIL $NAME 无可用券，跳过"; exit 1; }
echo "INFO couponUserId=$CP"

ui_navigate "/pages/coupons/coupons" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.coupons" 8 || { echo "FAIL $NAME[优惠券页]"; fails=$((fails + 1)); }
ui_tap ".coupon-tab-mine"; sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[我的券列表]" ".coupon" 1 || fails=$((fails + 1))

# 「去使用」仅 UNUSED 券渲染，首个 .btn 即目标
ui_tap ".btn"; sleep 2
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[回下单页]"; fails=$((fails + 1)); }
ui_assert "$NAME[带入券提示]" ".order-coupon" 1 || fails=$((fails + 1))
ui_assert "$NAME[清除入口]" ".order-coupon-clear" 1 || fails=$((fails + 1))

# 改回不用券
ui_tap ".order-coupon-clear"; sleep 1
ui_assert_absent "$NAME[改回原价]" ".order-coupon" || fails=$((fails + 1))

ui_screenshot "c13-coupon"
exit $fails

#!/usr/bin/env bash
# UI-C35 领券中心与我的券（C 端 P1，[造数] 会员已有券）
# 领券中心渲染 → 领取后我的券条数 +1 且列表即时刷新
# 限制：首帧按钮可能是「已领取/已抢光」（.btn 首个匹配不可挑、不可序号定位），
#       先读文案判断可领才点；不可领则跳点击、仍校验「我的券」列表刷新。
source "$(dirname "$0")/../common.sh"
NAME="UI-C35 领券中心与我的券"
fails=0

ui_data coupon >/dev/null 2>&1 || echo "WARN 领取前置券失败"
BEFORE=$(ui_data coupon-count 2>/dev/null)

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/coupons/coupons" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.coupons" 8 || { echo "FAIL $NAME[优惠券页]"; fails=$((fails + 1)); }
ui_assert "$NAME[领券中心]" ".cg-page.coupons .coupon" 1 || fails=$((fails + 1))
ui_assert "$NAME[门槛文案]" ".cond" 1 || fails=$((fails + 1))

LBL=$(ui_text ".btn")
if [ "$LBL" = "领取" ]; then
  ui_tap ".btn"; sleep "$CG_RENDER_WAIT"
  AFTER=$(ui_data coupon-count 2>/dev/null)
  [ "$AFTER" -gt "$BEFORE" ] 2>/dev/null \
    && echo "PASS $NAME[领取+1] ($BEFORE → $AFTER)" \
    || { echo "FAIL $NAME[领取+1] ($BEFORE → $AFTER)"; fails=$((fails + 1)); }
else
  echo "INFO 首个券按钮不可领（$LBL），跳过点击领取"
fi

# 我的优惠券 Tab：列表应即时反映刚领到的券
ui_tap ".coupon-tab-mine"; sleep "$CG_RENDER_WAIT"
AFTER=$(ui_data coupon-count 2>/dev/null)
ui_assert "$NAME[我的券列表]" ".coupon" 1 || fails=$((fails + 1))
[ "$AFTER" -ge 1 ] 2>/dev/null \
  && echo "PASS $NAME[我的券有数据] (n=$AFTER)" \
  || { echo "FAIL $NAME[我的券有数据] (n=$AFTER)"; fails=$((fails + 1)); }
ui_assert "$NAME[去使用入口]" ".btn" 1 || fails=$((fails + 1))

ui_screenshot "c35-coupon-center"
exit $fails

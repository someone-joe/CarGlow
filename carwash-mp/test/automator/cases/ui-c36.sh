#!/usr/bin/env bash
# UI-C36 券门槛提示（C 端 P1）
# 覆盖到的部分：每张券渲染门槛文案（无门槛减 X / 满 X 减 Y），券面 ADC 信息来自后端模板。
# 未覆盖（功能未实现，需反馈产品/前端）：不满门槛的券「不可选」或「提示还差多少」。
#   —— 当前下单页只展示已选券，选券在优惠券页「去使用」，无任何门槛校验/差额提示；
#      也未发现该逻辑的服务端兜底入口。见 UI测试用例.md UI-C36。
source "$(dirname "$0")/../common.sh"
NAME="UI-C36 券门槛提示"
fails=0

ui_data coupon >/dev/null 2>&1 || echo "WARN 领券失败"

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/coupons/coupons" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.coupons" 8 || { echo "FAIL $NAME[优惠券页]"; fails=$((fails + 1)); }

N=$(ui_query_count ".coupon")
C=$(ui_query_count ".cond")
if [ "$C" -ge "$N" ] 2>/dev/null; then
  echo "PASS $NAME[每张券都渲染门槛文案] (券=$N 门槛=$C)"
else
  echo "FAIL $NAME[每张券都渲染门槛文案] (券=$N 门槛=$C)"
  fails=$((fails + 1))
fi

# 门槛文案来自后端模板：无门槛 → 「无门槛减 X」；有门槛 → 「满 X 减 Y」
LBL=$(ui_text ".cond")
case "$LBL" in
  无门槛减*|满*减*) echo "PASS $NAME[门槛文案格式] $LBL" ;;
  *) echo "FAIL $NAME[门槛文案格式] ($LBL)"; fails=$((fails + 1)) ;;
esac

ui_screenshot "c36-coupon-threshold"
exit $fails

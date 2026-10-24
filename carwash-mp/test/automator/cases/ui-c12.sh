#!/usr/bin/env bash
# UI-C12 支付方式切换（C 端 P0，[造数] WAIT_PAY）
# 微信/余额两渠道互斥切换；余额应足够（不足会跳充值，属另一边界，故先补足）
source "$(dirname "$0")/../common.sh"
NAME="UI-C12 支付方式切换"
fails=0

ui_reset
ORDERNO=$(ui_seed WAIT_PAY)
[ -z "$ORDERNO" ] && { echo "FAIL $NAME 造数失败"; exit 1; }

W=$(ui_data wallet)
PAY=$(ui_data order-pay "$ORDERNO")
if [ "$W" -lt "$PAY" ] 2>/dev/null; then
  ui_data topup $((PAY - W + 100)) >/dev/null || echo "WARN 充值失败，余额仍不足"
  echo "INFO 补足余额：W=$W PAY=$PAY"
fi

ui_navigate "/pages/pay/pay?orderNo=$ORDERNO" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.pay" 8 || { echo "FAIL $NAME[支付页]"; fails=$((fails + 1)); }
ui_assert "$NAME[两种支付方式]" ".pt-row" 2 || fails=$((fails + 1))
ui_assert "$NAME[默认微信]" ".pt-row.on .pt-ico-wechat" 1 || fails=$((fails + 1))
ui_assert_absent "$NAME[余额充足无告警]" ".pt-row.insufficient" || fails=$((fails + 1))

# 切到余额：点钱包图标（冒泡到 .pt-row 的 pickBalance）
ui_tap ".pt-ico-wallet"; sleep 1
ui_assert "$NAME[切到余额]" ".pt-row.on .pt-ico-wallet" 1 || fails=$((fails + 1))
# 切回微信
ui_tap ".pt-ico-wechat"; sleep 1
ui_assert "$NAME[切回微信]" ".pt-row.on .pt-ico-wechat" 1 || fails=$((fails + 1))
ui_assert "$NAME[底部支付条]" ".pay-btn" 1 || fails=$((fails + 1))

ui_screenshot "c12-pay-channel"
exit $fails

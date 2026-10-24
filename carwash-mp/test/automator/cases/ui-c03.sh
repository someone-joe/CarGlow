#!/usr/bin/env bash
# UI-C03 首页入口跳转（C 端 P0，不造数）
# 依次点：无感洗车/自助洗车/储值充值/优惠券/钥匙柜/精选套餐 → 各自正确页（不白屏）
source "$(dirname "$0")/../common.sh"
NAME="UI-C03 首页入口跳转"
fails=0
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_assert "$NAME[无感→services]" ".cg-page" 1 || fails=$((fails + 1))
# wechatide 选择器引擎忽略 :nth-child/:nth-of-type（等于没写），一律用唯一类锚点
ui_tap ".cg-art-car"; sleep "$CG_RENDER_WAIT"                    # 无感洗车（入口1 专属插画）
ui_assert "$NAME[无感→services]" ".cg-page.services" 1 || fails=$((fails + 1))
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_tap ".home-entry-drop-2"; sleep "$CG_RENDER_WAIT"             # 自助洗车（入口2 专属插画）
ui_assert "$NAME[自助→self-wash]" ".cg-page.self-wash" 1 || fails=$((fails + 1))
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_tap ".home-promo-recharge"; sleep "$CG_RENDER_WAIT"           # 储值充值
ui_assert "$NAME[储值→recharge]" ".cg-page.recharge" 1 || fails=$((fails + 1))
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_tap ".home-promo-coupons"; sleep "$CG_RENDER_WAIT"            # 优惠券
ui_assert "$NAME[优惠券→coupons]" ".cg-page.coupons" 1 || fails=$((fails + 1))
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_tap ".home-cabinet"; sleep "$CG_RENDER_WAIT"                  # 附近钥匙柜
ui_assert "$NAME[钥匙柜→cabinet]" ".cg-page.cabinet" 1 || fails=$((fails + 1))
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_tap ".home-package"; sleep "$CG_RENDER_WAIT"                  # 精选套餐→下单页
ui_assert "$NAME[套餐→order]" ".cg-page.order" 1 || fails=$((fails + 1))
ui_screenshot "c03-home"
exit $fails

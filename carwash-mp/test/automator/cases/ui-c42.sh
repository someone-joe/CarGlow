#!/usr/bin/env bash
# UI-C42 图片加载失败占位（C 端 P1）
# 目标：网络图加载失败/缺失时显示占位，避免白块。实现：AppImage 组件 @error / 空 src → .app-image-ph 灰底占位。
#
# 可自动化边界（重要）：现有数据里所有 <app-image> 要么被 v-if 守卫（空 src 时整块不渲染，走页面自身占位），
#   要么 src 为真实可加载 URL；wechatide 无 evaluateJs 改运行期 src、SelectorQuery 不穿透组件，
#   无法注入「无效 URL」触发 @error。故「失败→占位」分支无法在本环境确定性复现。
# 本用例覆盖可确定验证的 happy-path：
#   ① 服务详情封面图经 AppImage 正常渲染（.cover 在 DOM 中）；
#   ② 正常加载时不出现占位（无 .app-image-ph）——不会误显占位、也不是白图。
# 「失败→占位」需断网/无效图 URL 触发，留人工或弱网回归（见 UI测试用例.md UI-C42）。
source "$(dirname "$0")/../common.sh"
NAME="UI-C42 图片占位(happy-path)"
fails=0

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/service-detail/service-detail?serviceId=101" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.service-detail" 8 || { echo "FAIL $NAME[详情页]"; fails=$((fails + 1)); }

# ① 封面图渲染（有封面用封面，无封面用 /static/logo.png，均正常加载）
if ui_wxml_has ".cg-page.service-detail" "cover"; then
  echo "PASS $NAME[封面图渲染] .cover"
else
  echo "FAIL $NAME[封面图渲染] .cover 未出现"; fails=$((fails + 1))
fi

# ② 正常加载不出现占位（负向：happy-path 无 .app-image-ph）
if ui_wxml_has ".cg-page.service-detail" "app-image-ph"; then
  echo "FAIL $NAME[不应出现占位] 但检测到 .app-image-ph"; fails=$((fails + 1))
else
  echo "PASS $NAME[正常加载无占位]"
fi

ui_screenshot "c42-image-placeholder"
exit $fails

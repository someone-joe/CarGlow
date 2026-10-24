#!/usr/bin/env bash
# UI-C09 下单-无车辆引导（C 端 P0，[造数] 清空会员车辆后校验，结束前必须还原）
#
# 唯一会改动共享会员数据的用例：删除前备份，trap EXIT 兜底还原。
# 已知限制：VehiclePicker 的空态按钮是自定义组件内部节点，wechatide 的 SelectorQuery
# 不穿透组件边界（querySelectorAll 恒为 0，也无法点击），因此「点新增→跳车辆选择页→
# 新增后回填」这段链路无法自动化，本用例只校验「无车时渲染出新增引导」这一半；
# 剩余部分仍需人工回归（见 UI测试用例.md UI-C09）。
source "$(dirname "$0")/../common.sh"
NAME="UI-C09 无车辆引导"
fails=0

restore() {
  ui_data vehicles:restore >/dev/null 2>&1 && echo "INFO 异常路径兜底还原完成" || echo "WARN 车辆还原失败"
}
trap restore EXIT

ui_data vehicles:clear >/dev/null || { echo "FAIL $NAME 清车失败"; exit 1; }

# 先回首页：switchTab 会清空 navigateTo 累积栈，避免微信 10 层页面栈上限导致导航静默失败
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页]"; fails=$((fails + 1)); }

# 无车 → VehiclePicker 渲染新增引导（wxml 兜底验证，无法点击）
if ui_wxml_has ".cg-page.order" "vp-add"; then
  echo "PASS $NAME[渲染新增引导] .vp-add"
else
  echo "FAIL $NAME[渲染新增引导] .vp-add 未渲染"
  fails=$((fails + 1))
fi
# 有车时应不具备该空态（负向对照在还原后做）
if ui_wxml_has ".cg-page.order" "vp-item"; then
  echo "FAIL $NAME[无车却仍有车辆卡] .vp-item"
  fails=$((fails + 1))
fi

# 即时还原，并校验；trap 只兜底异常路径，避免重复还原产生重复车辆
ui_data vehicles:restore >/dev/null || echo "WARN 车辆还原失败"
trap - EXIT
N=$(ui_data vehicles | python3 -c 'import sys,json;print(len(json.load(sys.stdin)))' 2>/dev/null)
[ "$N" -ge 1 ] 2>/dev/null \
  && echo "PASS $NAME[车辆已还原] (n=$N)" \
  || { echo "FAIL $NAME[车辆已还原] (n=$N)"; fails=$((fails + 1)); }

# 负向对照：有车时下单页应渲染车辆卡而非新增引导
ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页2]"; fails=$((fails + 1)); }
if ui_wxml_has ".cg-page.order" "vp-item"; then
  echo "PASS $NAME[有车时渲染车辆卡] .vp-item"
else
  echo "FAIL $NAME[有车时渲染车辆卡] .vp-item 未渲染"
  fails=$((fails + 1))
fi

ui_screenshot "c09-no-vehicle"
exit $fails

#!/usr/bin/env bash
# UI-C07 下单流程（C 端 P0，不建单，只验证表单步骤可填写 + 回看不丢）
# 实现版 STEP：1 服务时间 / 2 服务方式(取送+柜) / 3 服务 / 4 车辆与停车位置 / 5 拍照留言
# 注意：wechatide 选择器引擎忽略 :nth-child/:nth-of-type，同组多元素只能取首个匹配
source "$(dirname "$0")/../common.sh"
NAME="UI-C07 下单流程"
fails=0

ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页]"; fails=$((fails + 1)); }
ui_assert "$NAME[五步卡]" ".order-step" 5 || fails=$((fails + 1))
ui_assert "$NAME[底部结算栏]" ".cg-bottom-bar" 1 || fails=$((fails + 1))

# STEP1 时间浮层：首个 .cg-row.order-row 即「选择服务时间」行
ui_tap ".cg-row.order-row"; sleep 1
ui_wait_for ".cg-sheet" 8 || { echo "FAIL $NAME[时间浮层]"; fails=$((fails + 1)); }
ui_assert "$NAME[近7天日期]" ".order-day" 7 || fails=$((fails + 1))
ui_assert "$NAME[时段来自站点配置]" ".order-slot" 1 || fails=$((fails + 1))
ui_tap ".order-slot"; sleep 1
ui_tap ".order-sheet-close"; sleep 1
ui_assert_absent "$NAME[浮层已关]" ".cg-sheet" || fails=$((fails + 1))

# STEP3 选服务
ui_tap ".order-svc"; sleep 1
ui_assert "$NAME[服务选中态]" ".order-svc.active" 1 || fails=$((fails + 1))
ui_assert "$NAME[结算栏金额]" ".order-bar-price" 1 || fails=$((fails + 1))

# 未勾服务协议 → 不可提交（C10 条款前置也在此覆盖）
ui_assert "$NAME[未勾协议禁用]" ".order-bar-btn.is-disabled" 1 || fails=$((fails + 1))
ui_tap ".order-agree"; sleep 1
ui_assert "$NAME[勾选态]" ".cg-check.is-on" 1 || fails=$((fails + 1))
ui_assert_absent "$NAME[勾选后可提交]" ".order-bar-btn.is-disabled" || fails=$((fails + 1))

# 「已选项回看不丢」由 UI-C08（选柜 → navigateBack 回填）覆盖：index 非 tabBar 页，switchTab 会销毁实例

ui_screenshot "c07-order"
exit $fails

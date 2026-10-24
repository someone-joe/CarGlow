#!/usr/bin/env bash
# UI-C41 邀请好友（C 端 P1）
# 邀请码/绑定入口/邀请记录三块必须渲染；未绑 code 时绑定按钮禁用
source "$(dirname "$0")/../common.sh"
NAME="UI-C41 邀请好友"
fails=0

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/invite/invite" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.invite" 8 || { echo "FAIL $NAME[邀请页]"; fails=$((fails + 1)); }
ui_assert "$NAME[邀请码]" ".cc-code" 1 || fails=$((fails + 1))
ui_assert "$NAME[复制入口]" ".cc-copy" 1 || fails=$((fails + 1))
ui_assert "$NAME[填写好友码]" ".input" 1 || fails=$((fails + 1))
ui_assert "$NAME[绑定入口]" ".bind-btn" 1 || fails=$((fails + 1))

# 邀请码有值（不是「生成中…」）
CODE=$(ui_text ".cc-code")
[ -n "$CODE" ] && [ "$CODE" != "生成中…" ] \
  && echo "PASS $NAME[邀请码已生成] $CODE" \
  || { echo "FAIL $NAME[邀请码已生成] ($CODE)"; fails=$((fails + 1)); }

# 未填 code 时绑定按钮禁用
ui_assert "$NAME[未填禁用]" ".bind-btn.disabled" 1 || fails=$((fails + 1))
ui_input ".input" "CGTEST01"; sleep 1
ui_assert_absent "$NAME[填后可绑]" ".bind-btn.disabled" || fails=$((fails + 1))

# 邀请记录：有记录渲染列表，无记录渲染空态
n=$(ui_query_count ".inv")
e=$(ui_query_count ".cg-empty")
if [ "$n" -ge 1 ] 2>/dev/null; then
  echo "PASS $NAME[邀请记录] (.inv=$n)"
elif [ "$e" -ge 1 ] 2>/dev/null; then
  echo "PASS $NAME[邀请记录空态] 文案存在"
else
  echo "FAIL $NAME[邀请记录] 既无记录也无空态"
  fails=$((fails + 1))
fi

ui_screenshot "c41-invite"
exit $fails

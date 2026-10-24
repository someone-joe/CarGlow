#!/usr/bin/env bash
# UI-C33 多车辆与预选（C 端 P0，[造数] 造 2 辆车）
# 覆盖：下单页 STEP4 车辆选择渲染多辆车；无默认车时预选「列表首辆」。
#
# 边界（重要）：后端 wash_vehicle 无 is_default 列，isDefault 恒 false（见 VehicleVO /
#   WashVehicleService 注释「前端按列表首项预选即可，不要自己发明默认车规则」）。故「默认车预选」
#   当前无数据可验；index.vue 的 (find(isDefault) ?? [0]) 属前向兼容——无默认时等价于预选首辆，
#   本用例即验该回退行为 + 多车渲染。
#
# 手段：造 2 辆可辨车牌 → 进下单页 → 因 SelectorQuery 不穿透 VehiclePicker，改用 outerWxml：
#   ① 至少 2 个车辆卡（vp-plate）；② 恰 1 个选中项（vp-check ✓）；③ active 项车牌 == 接口列表首辆。
source "$(dirname "$0")/../common.sh"
NAME="UI-C33 多车辆与预选"
fails=0

# 造两辆可辨车（保证会员至少 2 辆），记 id 供清理
P1="粤Z$(date +%H%M%S)A"; P2="粤Z$(date +%H%M%S)B"
VID1=$(ui_data vehicles:add "$P1"); VID2=$(ui_data vehicles:add "$P2")
if [ -z "$VID1" ] || [ -z "$VID2" ]; then echo "FAIL $NAME 造车失败 (v1=$VID1 v2=$VID2)"; exit 1; fi
cleanup(){ ui_data vehicles:del "$VID1" >/dev/null 2>&1; ui_data vehicles:del "$VID2" >/dev/null 2>&1; }
trap cleanup EXIT

# 列表首辆（后端新车在前返回；预选应命中它）与车辆总数
PLATE0=$(ui_data vehicles 2>/dev/null | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d[0]["plateNo"] if d else "")' 2>/dev/null)
TOTAL=$(ui_data vehicles 2>/dev/null | python3 -c 'import sys,json;print(len(json.load(sys.stdin)))' 2>/dev/null)
echo "INFO 车辆总数=$TOTAL 列表首辆=$PLATE0"

ui_navigate "/pages/home/home" switchTab; sleep "$CG_RENDER_WAIT"
ui_navigate "/pages/index/index" navigateTo; sleep "$CG_RENDER_WAIT"
ui_wait_for ".cg-page.order" 8 || { echo "FAIL $NAME[下单页]"; fails=$((fails + 1)); }

WXML=$($WCLI automation_element_action --project "$CG_MP_PROJECT" --action outerWxml --selector ".cg-page.order" 2>/dev/null)

# ① 多辆车渲染：≥2 个车辆卡
NPLATE=$(printf '%s' "$WXML" | grep -o "vp-plate" | wc -l | tr -d ' ')
[ "${NPLATE:-0}" -ge 2 ] 2>/dev/null \
  && echo "PASS $NAME[渲染多辆车辆卡] (vp-plate=$NPLATE)" \
  || { echo "FAIL $NAME[渲染多辆车辆卡] (vp-plate=$NPLATE)"; fails=$((fails + 1)); }

# ② 有且仅有一个选中项（vp-check ✓ 仅出现在选中车辆上）
NCHK=$(printf '%s' "$WXML" | grep -o "vp-check" | wc -l | tr -d ' ')
[ "$NCHK" = "1" ] \
  && echo "PASS $NAME[恰一个预选项] (vp-check=$NCHK)" \
  || { echo "FAIL $NAME[恰一个预选项] (vp-check=$NCHK)"; fails=$((fails + 1)); }

# ③ 选中(active)项车牌 == 列表首辆（无默认车 → 预选首辆，即 D1 回退口径）
ACTIVE=$(printf '%s' "$WXML" | python3 -c 'import sys,re; w=sys.stdin.read(); m=re.search(r"vp-item[^\"]*active[^\"]*\"[^>]*>.*?vp-plate[^\"]*\">([^<]+)", w); print(m.group(1) if m else "")')
if [ -n "$ACTIVE" ] && [ "$ACTIVE" = "$PLATE0" ]; then
  echo "PASS $NAME[预选=列表首辆] ($ACTIVE)"
else
  echo "FAIL $NAME[预选=列表首辆] (active=$ACTIVE 期望=$PLATE0)"; fails=$((fails + 1))
fi

# 两辆造数车均在可视列表（slice(0,2) 取前两辆，二者为最新两辆）
if printf '%s' "$WXML" | grep -q "$P1" && printf '%s' "$WXML" | grep -q "$P2"; then
  echo "PASS $NAME[两辆造数车均在列表]"
else
  echo "WARN $NAME[造数车未都在可视列表]（会员已有更多车时 slice(0,2) 仅显示前两辆，非必败）"
fi

cleanup; trap - EXIT
ui_screenshot "c33-vehicles"
exit $fails

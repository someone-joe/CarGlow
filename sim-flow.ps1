# sim-flow.ps1 —— 订单全流程无硬件模拟（一键版）
# 用法： .\sim-flow.ps1 -OrderNo SE20260919141557296186 -AdminToken "<后台token>" [-Code 219437] [-SlotNo A02]
param(
  [Parameter(Mandatory=$true)] [string]$OrderNo,
  [Parameter(Mandatory=$true)] [string]$AdminToken,
  [string]$Code   = "219437",
  [string]$SlotNo = "A02",
  [string]$Base   = "http://localhost:8080"
)

$ProgressPreference = 'SilentlyContinue'
$ErrorActionPreference = 'Continue'

# 发 JSON（文件法，绕开 PowerShell 把原生 curl 的双引号吞掉的坑）
function PostJ($url, $obj, $token) {
  $j = $obj | ConvertTo-Json -Compress
  [System.IO.File]::WriteAllText('req.json', $j)
  $h = @('-s','-X','POST',$url,'-H','Content-Type: application/json','--data-binary','@req.json')
  if ($token) { $h += @('-H',"Authorization: Bearer $token") }
  & curl.exe @h
  echo ''
}

# 查当前状态，便于观察
function Status {
  docker exec carglow-mysql mysql -uroot -pcarglow_dev carwash -e "SELECT status FROM wash_order WHERE order_no='$OrderNo';" 2>&1 | findstr /V Warning
}

Write-Output "===== 0) 取会员 token（dev 模式任意 code） ====="
$mb = @{code="dev"} | ConvertTo-Json -Compress; [System.IO.File]::WriteAllText('m.json',$mb)
$memberToken = (curl.exe -s -X POST "$Base/api/v1/auth/login" -H "Content-Type: application/json" --data-binary "@m.json" | ConvertFrom-Json).data.token
Write-Output "memberToken=$memberToken"

Write-Output "===== 1) 模拟支付（仅 WAIT_PAY；已支付会报状态机拒绝，可忽略） ====="
curl.exe -s -X POST "$Base/api/v1/payments/$OrderNo/mock-pay"; echo ""

Write-Output "===== 2) 写开箱码 + 柜机存入钥匙（WAIT_KEY -> KEY_IN） ====="
docker exec carglow-redis redis-cli -a carglow_dev --no-auth-warning SET "slot:code:$OrderNo" "$Code" EX 600
PostJ "$Base/device-callback/v1/slot/deposit" @{orderNo=$OrderNo; code=$Code; slotNo=$SlotNo} $null
Status

Write-Output "===== 3) TAKE_KEY（KEY_IN -> PICKING） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="TAKE_KEY"; reason="模拟取钥匙"; confirm=$false} $AdminToken
Status

Write-Output "===== 4) PICK_CAR_DONE（PICKING -> TO_STATION） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="PICK_CAR_DONE"; reason="模拟取车拍照"; confirm=$false} $AdminToken
Status

Write-Output "===== 5) ARRIVE_STATION（TO_STATION -> WASHING） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="ARRIVE_STATION"; reason="模拟入场打卡"; confirm=$false} $AdminToken
Status

Write-Output "===== 6) SOP_DONE（WASHING -> QC） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="SOP_DONE"; reason="模拟SOP完成"; confirm=$false} $AdminToken
Status

Write-Output "===== 7) QC_PASS（QC -> WAIT_RETURN） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="QC_PASS"; reason="模拟质检通过"; confirm=$false} $AdminToken
Status

Write-Output "===== 8) LEAVE_STATION（WAIT_RETURN -> RETURNING） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="LEAVE_STATION"; reason="模拟驶离中央站"; confirm=$false} $AdminToken
Status

Write-Output "===== 9) RETURN_DONE（RETURNING -> RETURNED） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="RETURN_DONE"; reason="模拟还车归柜"; confirm=$false} $AdminToken
Status

Write-Output "===== 10) TAKE_KEY_BACK ⚠️高危（RETURNED -> WAIT_REVIEW，需 confirm=true） ====="
PostJ "$Base/admin-api/wash/order/$OrderNo/advance" @{event="TAKE_KEY_BACK"; reason="模拟客户取回钥匙"; confirm=$true} $AdminToken
Status

Write-Output "===== 11) REVIEW_SUBMIT（WAIT_REVIEW -> FINISHED） ====="
PostJ "$Base/api/v1/orders/$OrderNo/reviews" @{rating=5; tags=@("干净"); content="满意"} $memberToken
Status

Write-Output "===== 全流程结束 ====="

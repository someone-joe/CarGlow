# 接口自动化测试执行器（零依赖 Node）

> 用例设计在 `自动化测试/接口自动化/API测试用例.md`（226 条）；本目录是**执行层**，只实现当前已交付批次。

## 运行

```bash
bash scripts/api-test/run.sh              # 跑前清理 + 全量用例（推荐）
bash scripts/api-test/run.sh --no-sweep   # 跳过清理，快速重跑
```

环境变量：`CG_API_BASE`（默认 `http://127.0.0.1:8080`）、`CG_API_TIMEOUT_MS`（默认 10000）、
`CG_SKIP_SIDECAR=1`（跳过 docker 副作用断言与产能操控）。

## 前置条件

1. 后端已启动（`java -jar carwash-server/ruoyi-admin/target/ruoyi-admin.jar`，logback 绕法见 PROGRESS 11.4）；
2. MySQL/Redis 容器在跑（`docker compose -f docker/docker-compose.dev.yml up -d`）——清理、产能操控、副作用断言依赖；
3. dev 数据基线：站点 1、服务 101（3900 分）/102（200000 分）、测试号 appid 已配。

## 结构

```
scripts/api-test/
  run.sh                入口：清理 + node --test（串行）
  sweep.mjs             跑前清理：取消遗留 [AUTOTEST] 待支付订单（走接口）
  lib/
    config.js           BASE_URL / 超时 / 测试标记 / 站点 ID
    http.js             fetch 封装：信封解析、超时快失败、失败全量打印
    assert.js           五类显式断言（业务码/字段/类型/金额/时间戳）
    factory.js          数据工厂：登录、车辆、下单（幂等键）、支付、充值、领券、上传
    sidecar.js          docker exec 只读查询 + 产能操控（withCapacityZero 自动恢复）
  cases/
    a-auth.test.js      A 段 8 条
    c-order-create.test.js  C 段 15 条（C01~C15）
    d-pay.test.js       D 段 11 条（D10/D11 标记 todo）
    r-amount.test.js    R.1 金额边界 12 条（R10/R13~R15 标记 todo）
```

## 约定（对齐 README 7.2 稳定性措施）

1. **造数全走接口**：禁止 SQL 造单；sidecar 只做只读查询与产能 key 操控（用后自动恢复）；
2. **超时快失败**：单请求 10s 无响应即抛错并打印完整请求——曾遇服务连接层异常导致请求永久挂起（重启后端可恢复）；
3. **写操作不重试**：下单/支付/流转失败即记录，绝不静默重试（避免掩盖幂等缺陷）；
4. **清理**：每文件 `after()` 取消本文件创建的订单（释放格口+产能）；跑前 sweep 清理历史遗留；
5. **数据影响告知**：sweep 会把 dev 共享钱包复位为 0（`CG_RESET_WALLET=0` 可关）；R.1/D05 等钱包边界用例会改动其余额（顶充与退款），属预期；
6. **串行执行**：`--test-concurrency=1`，避免并发造数互相干扰（格口/产能计数类用例依赖确定性）；
7. **逐条清理**：`afterEach` 取消每条用例创建的订单（失败也执行）——dev 仅 2 个空闲格口，不即时清理会 B2003 连坐；
8. **A06 条件跳过**：师傅越权用例依赖 W001 口令为 123456；口令被改过时自动跳过并打印原因。

## 已知问题与修复记录

1. ~~【产品缺陷·产能泄漏】下单失败路径漏扣产能~~ **✅ 已修复（2026-10-24）**：
   `WashOrderCreateService.takeCapacity`（DECR）曾先于优惠券校验/格口预占执行，失败抛异常后名额不回补
   （实测：产能 20 → 假券失败 → 19）。修复：DECR 之后的失败路径 try/catch 回补（`giveBackCapacity`）。
   回归：API-C11 断言失败下单前后产能不变。
2. ~~【契约缺口】`GET /api/v1/auth/agreements` 后端未实现~~ **✅ 已补齐（2026-10-24）**：
   按契约实现（`WashAgreementService` + `WxAuthController`，免登录，type 四枚举），
   文案走 sys_config（`wash.agreement.*`，种子 `sql/wash_20261024_agreement_config.sql`，dev 占位内容上线前须替换）。
   回归：API-A07 四类型 + 非法 type 拒绝 + 免登录。
3. mock 渠道下无支付流水表（无 `wash_payment`），D09 入账次数以状态机口径断言；接真实微信渠道后补 trade_no 唯一键断言。
4. ~~【产品缺口】取消订单不返还优惠券~~ **✅ 已修复（2026-10-24）**：
   `doCancel` 调 `WashCouponService.returnByOrderNo(orderNo)`，按 `wash_coupon_user.order_no`
   反查已核销券置回 UNUSED（订单表未冗余券字段，故反查而不是从订单取）。
   返还异常只记 error 日志、不阻断取消主流程（券是附属权益，主流程是状态与退款）。→ E10 已转正。
5. ~~【实现缺口】车辆删除~~ **✅ 已修复（2026-10-24）**：`DELETE /api/v1/vehicles/{vehicleId}`
   已实现（逻辑删 `del_flag='2'`，历史订单仍引用 vehicle_id 故不能物理删）。→ B10b 已转正。
6. ~~【实现缺口】`GET /api/v1/faqs`~~ **✅ 已修复（2026-10-24）**：复用 `wash_cs_faq`
   （后台「客服配置」页维护），`question → label`；`ticketType` 库里无对应列，返回 null。→ M10 已转正。
7. ~~【契约不一致】`selfwash/pricing` 免登录~~ **✅ 已对齐（2026-10-24）**：决策取「免登录」——
   openapi 补 `security: []`（全局是 bearerAuth，不显式置空就会继承成需鉴权），
   `MemberWebConfig` 放行该路径。价格是纯配置，不含用户数据。→ B14 已转正。
8. **【契约缺口】`/admin-api/wash/service` 新增缺 `scene`**：契约 AdminServiceRequest 未列该字段，
   但 `wash_service.scene` 为 NOT NULL，按契约调用报 500。→ O07 用例内注明并补传 `scene`。
9. **【推进待定】后台无定时任务触发接口**（`/admin-api/monitor/job/*` 未实现），
   P 段 7 条中仅 P01/P02/P05 能断言前置状态，其余 4 条（顺延/T+3/离线/退款重试）挂起。
10. **【安全缺口·已修】投保未校验车辆归属（API-K08 发现）**：`WashInsuranceService.buy`
    此前只校验产品与车辆非空，拿他人的 `vehicleId` 投保会直接成功并生成保单（实测 code=0）。
    已补 `WashVehicleQueryService.requireOwned(memberId, vehicleId)`（跨域走 Service，不直接注入 Mapper）。
11. **【契约缺口·已修】我的券未按契约计算 EXPIRED（API-K05 发现）**：契约 `CouponUserVO.status`
    枚举含 EXPIRED 且 `/api/v1/coupons` 支持按 status 过滤，但实现直接返回库里原值，
    过期的 UNUSED 券仍显示 UNUSED、按 EXPIRED 过滤恒为空。已修：EXPIRED 为计算值
    （库里仍是 UNUSED，不改数据），过滤也按计算值走。
12. **【实现缺口】`POST /api/v1/community-applies` 未实现**（500，且无 `wash_community_apply` 表）。
    需新建表 + 后台审核流，属产品决策（不在本轮 4 个缺口范围）。→ B03 仍挂起。

## 已交付批次与通过标准

| 批次 | 用例数 | 状态 |
| --- | --- | --- |
| A 认证与鉴权 | 8 | ✅ 6 通过 + A07 挂起（契约缺口，已补齐接口）+ A06 条件跳过（师傅口令） |
| B 基础数据 | 14 | ✅ 13 通过（B10b 车辆删除、B14 pricing 免登录已转正）+ B03 挂起（接口未实现）+ B12 挂起（车牌规则待产品明确） |
| C 下单 | 15 | ✅ 15/15 通过（含产能泄漏回归 C11、格口 B2003、6 项缺参） |
| D 支付 | 11 | ✅ 9 通过 + D10/D11 挂起 |
| E 取消与应急 | 10 | ✅ 10/10 通过（E10 券返还已修复转正） |
| F 订单查询 | 8 | ✅ 8/8 通过（F06 越权用 sidecar 第二会员夹具转正） |
| G 开箱码与柜机回调 | 10 | ✅ 9 通过 + G10 挂起（回调无会员上下文，归属校验落点已由 G03 覆盖） |
| H 状态机 | 14 | ✅ 14/14 通过（全链路 11 步、重洗回路、白名单、并发支付、照片强校验） |
| I 影像 | 8 | ✅ 8/8 通过（含 bizType 错配、路径穿越、query token 鉴权） |
| J 评价/售后/退款 | 12 | ✅ 10 通过 + J09/J12 挂起 |
| K 优惠券与保险 | 10 | ✅ 10/10 通过（K03/K05/K08 已转正；K02/K04 改用自造模板夹具，不再空跳） |
| L 自助洗车 | 12 | ✅ 11 通过 |
| M 钱包/邀请/发票/通知 | 10 | ✅ 9 通过（M10 faqs、M06b 邀请绑定已转正）+ M03/M04 由 R.1 覆盖 |
| N 师傅端 | 12 | ✅ 9 通过 + N09~N12 挂起（已由 H 批覆盖 / 属 UI 批） |
| O 后台管理 | 14 | ✅ 12 通过 + O02 挂起（需无权限角色）+ O13 条件跳过（待派池空） |
| P 定时任务 | 7 | ✅ 2 通过 + 5 挂起（需 job 触发/时间旅行能力） |
| Q 安全与合规 | 8 | ✅ 4 通过（Q01 水平越权用第二会员夹具转正）+ Q04~Q06/Q08 已由其他批覆盖 |
| R.1 金额边界 | 15 | ✅ 11 通过（R11/R12 合并执行）+ R10/R13~R15 挂起或由 J 段覆盖 |

**2026-10-24 四轮连跑结果：195 用例 / 169 通过 / 0 失败 / 3 条件跳过 / 23 挂起（todo），四轮完全一致（可重复）。**

> 2026-10-24 第二轮推进：修复 4 个产品缺口（券返还 / 车辆删除 / faqs / pricing 免登录），
> 顺带修了执行中暴露的 2 个（投保越权、券 EXPIRED 计算），并把 11 条挂起用例转正
> （E10/B10b/B14/M10/K03/K05/K08/F06/Q01/G03/M06b），另把 K02/K04 从「空跳」改为真实执行。

通过标准：P0 100%、P1 ≥95%（对齐 PRD 12 验收）。失败用例须区分「产品缺陷」与「用例/环境问题」分别处置。

## 测试环境供给（run.sh 的 sweep 阶段自动完成）

每轮跑前执行，保证用例可重复执行；全部可用环境变量关闭：

| 步骤 | 作用 | 关闭开关 |
| --- | --- | --- |
| 收尾非终态残留订单 | 释放格口、归还师傅接单额度（否则 B2003 / 派单无人连坐） | — |
| 取消遗留待支付单 | 释放格口与产能 | `--no-sweep` |
| dev 钱包复位为 0 | 保证「余额恰好/差 1 分」可构造 | `CG_RESET_WALLET=0` |
| 清理测试影像（≤200B 占位图） | 防止 `upload/media` 堆积上千个 70 字节空图 | `CG_CLEAN_MEDIA=0` |
| 师傅 W001 排班设全周全天 | 保证任何时刻跑用例都能自动派单 | `CG_ENSURE_DUTY=0` |
| 格口补给到 6 个 | dev 种子仅 2 格，多状态并发用例放不下 | `CG_ENSURE_SLOTS=0` |
| 产能临时扩容到 500 | 站点日上限 20，一轮要建 30+ 单 | 自动，跑完恢复原值 |

> 环境供给只动 **dev 侧的容量与残留数据**，不动业务规则（状态机/价格/SLA）与产品代码。

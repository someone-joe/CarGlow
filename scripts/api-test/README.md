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

## 已交付批次与通过标准

| 批次 | 用例数 | 状态 |
| --- | --- | --- |
| A 认证与鉴权 | 8 | ✅ 6 执行通过 + A07 todo（契约缺口）+ A06 条件跳过（师傅口令非 123456 时） |
| C 下单 | 15 | ✅ 15/15 通过（C10 领券每轮可重复） |
| D 支付 | 11 | ✅ 9 执行通过 + D10/D11 todo |
| R.1 金额边界 | 15 | ✅ 11 执行通过（R11/R12 合并执行）+ R10/R13~R15 todo |
| B/E/F/G/H/I/J/K/L/M/N/O/P/Q | 其余 | 后续批次（用例已在设计文档中） |

**2026-10-24 两轮连跑结果：40 pass / 0 fail / 1 skip / 7 todo，两轮完全一致（可重复）。**

通过标准：P0 100%、P1 ≥95%（对齐 PRD 12 验收）。失败用例须区分「产品缺陷」与「用例/环境问题」分别处置。

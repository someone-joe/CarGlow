# 项目进度交接（换设备 / 新会话先看这里）

> 用途：聊天记录不会跨设备，但本文件会。新会话开始时，让 AI 先读本文件即可无损接上。
> 最后更新：2026-09-23

---

## 一、当前进度（一句话）

**四步走全部完成，第五步（师傅端）已完成并进入收尾**：C 端主链路（下单/支付/订单/评价/售后/影像）可用；**师傅端（取送+作业合并）11 个接口 + 前端已完成、全链路实测跑通**；运营后台可查订单与人工干预。当前重点：后台运营功能补全、商品（服务）后台可配置、C 端界面改版、补技术欠账。
- 后端已实测：`POST /api/v1/auth/login`（模拟登录，固定 dev-openid）✅、`GET /api/v1/orders`（空列表）✅、
  无 token 返回 10002 ✅、非法 tab 返回 10001 ✅、会员落库 ✅
- 新增业务模块：`wash-member`（登录/token/拦截器/独立安全链/异常处理）、`wash-order`（订单查询）
- 新表：`wash_member`、`wash_order`（SQL 见 `carwash-server/sql/wash_20260917_minimal.sql`）
环境卡点已全部解除：JDK 17 已装、Docker 已可用、MySQL 8 + Redis 7 容器已跑起来。
后端：若依官方 3.9.2（JDK 17 + Spring Boot 4.1）+ 我们的 `wash-common`（含状态机）编译通过，
`ruoyi-admin.jar` 启动成功并能连上 MySQL/Redis（`/captchaImage` 返回正常）。

```
[完成] 第 0 步 防漂移机制   → CODEBUDDY.md
[完成] 第 1 步 订单状态机   → 订单状态机规则表.md + 7 个 Java 文件
[完成] 第 2 步 API 契约     → openapi.yaml + 自动生成 TS 类型（已验证生效）
[待做] 第 3 步 工程骨架     → 后端 Maven 多模块 + uni-app 三端 + 跑通登录与空订单列表
```

---

## 二、新会话开场白（明天复制到新会话第一行）

```
我在做「夜间代客洗车平台」项目。请先按以下顺序读取，读完用 3 行话复述你理解的当前进度，
确认无误后我们再继续；不要直接开始写代码。

1. CODEBUDDY.md          （协作规则，必须遵守）
2. PROGRESS.md           （本文件，当前进度）
3. 订单状态机规则表.md    （订单 15 态与流转规则，SSOT）
4. openapi.yaml          （前后端接口契约，SSOT）

今天要做的事：搭工程骨架（后端 Maven 多模块 + uni-app 三端），跑通登录与空订单列表。
```

---

## 三、已产出的文件清单

| 文件 | 作用 | 状态 |
| --- | --- | --- |
| `CODEBUDDY.md` | AI 协作红线与自检清单，防代码漂移的总纲 | 完成 |
| `洗车项目技术方案.md` | 架构、选型、数据模型、接口规范、7 条 ADR | 完成（已修正 14 态 → 15 态） |
| `订单状态机规则表.md` | **订单 15 态 + 17 个流转事件的唯一定义** | 完成 |
| `openapi.yaml` | **前后端接口契约唯一真源**，C 端 23 个接口 | 完成 |
| `carwash-mp/src/api/schema.d.ts` | 由契约自动生成的前端 TS 类型（1369 行，已验证 15 个状态同步到位） | 已生成 |
| `carwash-server/ruoyi-wash/wash-common/.../statemachine/` | 7 个状态机 Java 文件（订单状态枚举、事件、流转规则表、闸机、日志实体） | 已写，**尚未编译**（等后端 `pom.xml`） |
| `docker/docker-compose.dev.yml` | 本地开发中间件编排（MySQL 8 + Redis 7） | 已启动，容器 healthy |
| `carwash-server/` 若依原生模块 | 若依 3.9.2 后端：admin / framework / system / quartz / generator / common + `sql/` | 已接入，**构建通过**（BUILD SUCCESS） |
| `carwash-server/ruoyi-wash/pom.xml`、`wash-common/pom.xml` | 业务父模块与公共模块，挂进若依根 pom | 已创建，编译通过 |
| `wash-common/src/test/.../OrderStateMachineTest.java` | 订单状态机 15 个单元测试（回归网第一道） | 已跑通，15/15 通过 |
| `carwash-mp/`（uni-app 模板） | 小程序工程：pages / static / manifest.json / pages.json / vite.config.ts | 已合并，**依赖未安装** |

Java 文件路径：
`carwash-server/ruoyi-wash/wash-common/src/main/java/com/ruoyi/wash/common/statemachine/`
`OrderStatus.java`、`OrderEvent.java`、`OrderOperatorType.java`、`OrderStatusTransitions.java`、`OrderStateMachine.java`、`OrderStatusLog.java`、`OrderStatusException.java`

---

## 四、已确认的决策（不要再重新讨论，除非明确要改）

| 类别 | 决策 |
| --- | --- |
| 业务 | `FINISHED`（已完成）之后**仍可退款** |
| 业务 | `PICKING`（已取钥匙、车未动）时客户取消，**退全款**，不扣跑腿费 |
| 技术 | 订单状态日志表**新增 `event` 字段**（PRD 原表无此字段） |
| 技术 | 金额一律**整数、单位分**；时间一律**毫秒时间戳** |
| 技术 | 分页参数 `pageNum` / `pageSize`，从 1 开始 |
| 技术 | 订单按钮（`OrderAction`）由后端返回，前端不得自行推断 |
| 技术 | 后端脚手架 = **若依官方 3.9.2**（JDK 17 + Spring Boot 4.1），源码进仓库（GPL-3.0，内部使用无碍） |
| 技术 | 构建用 **Docker 里的 Maven**（`maven:3.9-eclipse-temurin-17`），本机不装 Maven |
| 技术 | 本地中间件用 **Docker Compose**（MySQL 8.0.39 + Redis 7.2），不用直装绿色版 |
| 技术 | 流转拦截**以规则表为准**，不用 `isTerminal()` 一刀切（否则"已完成可退款"被误拦） |
| 技术 | 已开启 MyBatis 驼峰映射（若依默认注释掉了）：业务模块用注解式 Mapper，不开会全字段为 null |
| 技术 | 产能抢占用 **Lua 脚本**保证原子；直接 `DECR` 不存在的 key 会被 Redis 建成 -1，"未设置产能"会被误判成"已满" |
| 技术 | 新增错误码 A0006（当晚产能已满），契约 `openapi.yaml` 与 `ErrorCode` 已同步 |
| 技术 | **导入 SQL 必须带 `--default-character-set=utf8mb4`**：容器内 mysql 客户端默认 latin1（**实为 cp1252 超集**），
  中文会被双重编码——症状很阴险：`mysql` 命令行看着"正常"（字节原样吐出），API 返回却全是乱码 |
| 技术 | **已根治（2026-09-18）**：`carwash-server/sql/` 下所有 SQL 文件头部写入 `set names utf8mb4;`，
  防复发不再依赖"人记得加命令行参数"。`set names` 由服务器解释，能覆盖客户端默认字符集，Navicat 导入同样生效 |
| 技术 | 乱码排查先 `SELECT HEX(col)` 看字节再动手：只发生一次双重编码时 HEX 呈 `C3A5C28F…` 形态，
  可用 `CONVERT(BINARY(CONVERT(col USING latin1)) USING utf8mb4)` 逆推还原。**不要凭 Navicat 显示判断**，
  显示层与存储层可能不一致 |
| 技术 | 状态机不引入 Spring StateMachine，自研轻量流转表 |
| 技术 | 后端单体模块化（RuoYi 多模块），不上微服务 |
| 技术 | C 端用 uni-app（Vue3 + TS），三端复用 |

---

## 五、环境状态

| 工具 | 本机（2026-09-17） | 说明 |
| --- | --- | --- |
| Node.js | ✅ v20.20.2 / npm 10.8.2 | 已装 |
| Git | ✅ 2.54.0 | 已装 |
| JDK 17 | ✅ 17.0.12 | `C:\Program Files\Java\jdk-17` |
| Maven | ⚠️ 未单独装 | 用 IDEA 自带（3.9.16），不必单独装 |
| Docker | ✅ Desktop 29.8.0 | 已起 MySQL 8.0.39 + Redis 7.2 容器 |
| MySQL | ✅ 8.0.39 | `127.0.0.1:3306`，库 `carwash`，root / carglow_dev（2026-09-18 因双重编码重建，现已全量正常） |
| Redis | ✅ 7.2 | `127.0.0.1:6379`，密码 carglow_dev |
| IDEA | ❌ 缺失 | 建议装 Community 免费版（自带 Maven） |

常用命令（仓库根目录）：

```bash
docker compose -f docker/docker-compose.dev.yml up -d   # 启动
docker compose -f docker/docker-compose.dev.yml down    # 停止（数据在卷里，保留）
docker compose -f docker/docker-compose.dev.yml down -v # 连数据一起删，慎用
```

> 本机是 Windows 10 Pro，**非管理员**。曾尝试直装 MySQL/Redis 绿色版，因官方下载被挡而放弃，
> 最终改回 Docker 方案（用户决策：长期开发宁可用 Docker，避免多应用环境冲突）。

---

## 六、下一步待办（按顺序，全部未开始）

1. ~~**装环境**~~ ✅ 已完成：JDK 17.0.12 + Docker Desktop 29.8.0 + MySQL/Redis 容器
2. ~~**建后端骨架**~~ ✅ 已完成（2026-09-17）：若依 3.9.2 + JDK17 + Spring Boot 4.1，`wash-common` 编译通过，jar 启动成功
   - MySQL：`127.0.0.1:3306` 库 `carwash`，root / carglow_dev
   - Redis：`127.0.0.1:6379`，密码 carglow_dev
   - 启动：`java -jar carwash-server/ruoyi-admin/target/ruoyi-admin.jar`（端口 8080）
   - 打包（用 Docker 里的 Maven，本机不装）：
     `docker run --rm -v d:/CarGlow/carwash-server:/app -v carglow-m2:/root/.m2/repository -w /app maven:3.9-eclipse-temurin-17 mvn -B -DskipTests clean package`
3. **建前端骨架** ✅ 已完成（2026-09-17）：`npm run build:mp-weixin` 构建成功，产物 `dist/build/mp-weixin`
   - 编译命令：`cd carwash-mp && npm run build:mp-weixin`（开发热更新用 `npm run dev:mp-weixin`）
   - 微信开发者工具导入 `dist/build/mp-weixin`（或 dev 模式导入 `dist/dev/mp-weixin`）
   - `src/manifest.json` 里需填你自己的小程序 appid（测试号亦可）
   - 已完成：官方 `vite-ts` 模板合并进 `carwash-mp`（uni-app 3.0.0 + Vue 3.4 + Vite 5.2.8 + TS 4.9），`src/api/schema.d.ts` 已保留
   - 待执行：`cd carwash-mp && npm install --registry=https://registry.npmmirror.com && npm run dev:mp-weixin`
   - 产物目录 `dist/dev/mp-weixin`，用微信开发者工具导入；`src/manifest.json` 里需填你自己的小程序 appid
4. **建表**（MySQL/Redis 容器已起）：状态机日志表 DDL 见《订单状态机规则表.md》第四节
5. **跑通最小链路** ✅ 前后端均已完成（2026-09-17）
   - 后端：curl 实测通过（登录、鉴权拦截、空列表、错误码）
   - 前端：`src/utils/request.ts`（带 token、拆信封、统一报错、10002 自动续登重试一次）
     + `src/api/auth.ts`、`src/api/order.ts` + `src/pages/orders/orders.vue`（三 Tab + 空状态）
     + `App.vue` 启动静默登录；`npm run build:mp-weixin` 与 `vue-tsc --noEmit` 均通过
   - **联调前提**：微信开发者工具「详情 → 本地设置 → 不校验合法域名」必须勾选
     （后端是 http://localhost，非备案域名）；后端进程需在运行中
6. **补测试**：17 条合法流转全跑通 + 1 条非法流转被拦截（状态机单测已有 15 个，接入层测试待补）
7. **取消订单已完成**（2026-09-18）：`POST /api/v1/orders/{orderNo}/cancel`，走 `CANCEL` 事件 + 产能回补 + 已支付自动退款。
   实测四场景：未支付取消→`CANCELED`、未填原因→`21004`、已支付取消→`REFUNDING`（自动全额退款）、
   **已开洗客户取消→`21002` 拒绝**（PRD 红线在真实接口上生效）
8. **运营后台已打通**（2026-09-18）：
   - 后端：`GET /admin-api/wash/order/list`（权限 `wash:order:list`）+ `/status-options`（选项取自 OrderStatus 枚举，前端不写死状态名单）
   - 菜单：`sql/wash_menu.sql`（洗车业务 → 订单管理，perms = `wash:order:list`）
   - 前端：`carwash-admin`（RuoYi-Vue3），新增 `src/views/wash/order/index.vue` 与 `src/api/wash/order.js`；
     dev 端口改为 1024（80 端口非管理员起不来），浏览器打开 `http://localhost:1024`，admin / admin123
9. **后台详情 + 人工干预已完成**（2026-09-18）：
   - 接口：`GET /admin-api/wash/order/{orderNo}`（详情+时间轴，perm `wash:order:query`）、
     `GET /{orderNo}/event-options`（可触发事件，由规则表算出）、`POST /{orderNo}/advance`、`POST /{orderNo}/cancel`
     （后两个 perm `wash:order:edit`）；后台取消用 ADMIN 触发方，不受"客户只能取消车未动"限制
   - 前端：`views/wash/order/index.vue` 加详情抽屉（el-descriptions + el-timeline）+ 干预区
   - **全链路实测**（后台接口连推 10 个事件）：
     WAIT_KEY→KEY_IN→PICKING→TO_STATION→WASHING→QC→WAIT_RETURN→RETURNING→RETURNED→WAIT_REVIEW→**FINISHED**，
     时间轴 **8/8 节点全亮**；再对 FINISHED 发起取消 → 被状态机拒绝（终态保护生效）
10. **后台会员 / 车辆管理已完成**（2026-09-18）：
    - 接口：`GET /admin-api/wash/member/list`（perm `wash:member:list`，按昵称/手机号模糊查）、
      `GET /admin-api/wash/vehicle/list`（perm `wash:vehicle:list`，按车牌模糊查 / 按会员过滤）
    - **手机号与 openid 一律脱敏返回**（`138****1234`、`dev-****enid`），后台也不出明文（个保法最小化 P8）
    - 订单列表新增 `memberId` 过滤；会员/车辆页有「他的订单」按钮直接跳订单页并带上筛选
    - 前端：`views/wash/member/index.vue`、`views/wash/vehicle/index.vue`，菜单见 `sql/wash_menu.sql`（2010/2011/2012）
    - 实测：会员 1 条（脱敏正确）、车辆 1 条、按会员查订单 15 条、车牌模糊查命中 1 条
      ⚠️ 新菜单需**退出重新登录**才会出现（若依动态路由按角色权限缓存）
11. **格口预占 + 取送钥匙链路已完成**（2026-09-18）：
    - 新增 `wash-network` 模块：`wash_cabinet` / `wash_slot` / `wash_slot_open_log` 三张表（SQL `wash_20260919_slot.sql`），
      机柜/格口领域对象、`WashCabinetQueryService` / `WashCabinetMapper` / `WashSlotService` / `WashSlotMapper`
    - 下单即预占格口（`WashOrderCreateService` 调 `WashSlotService.reserve`，乐观更新防超卖，无空闲格口返回 **B2003**）；
      取消订单释放预占（`WashOrderStateService.doCancel` 调 `release`）
    - C 端取开箱码：`POST /api/v1/orders/{orderNo}/open-code`（仅 WAIT_KEY / RETURNED 可用，10 分钟有效，留痕脱敏）
    - 柜机回调：`POST /device-callback/v1/slot/deposit`（校验开箱码 → 格口占用 + `DEPOSIT_KEY` 事件 WAIT_KEY→KEY_IN）
    - 开箱操作全部写入 `wash_slot_open_log`（只增不删，含开箱码脱敏）
    - **契约对齐**：`openapi.yaml` 的 `open-code` 由 GET+action 改为 POST（后端按状态判定存/取，前端不传 action）
    - 实测全链路：下单(预占A02)→mock-pay(WAIT_KEY)→取码(967229)→柜机回调→**KEY_IN**，留痕正常
12. **后台数据看板已完成**（2026-09-19）：
    - 接口 `GET /admin-api/wash/dashboard/stats`（perm `wash:dashboard:list`）：今日单量 / 在洗数 / 待存钥匙 / 异常数 + 状态分布
    - 口径集中在 `WashOrderStatsService`（在洗 = 去程+清洗+待质检；异常 = 退款中），状态取值一律引 `OrderStatus` 枚举，前端不维护名单
    - 纯只读聚合，不新增表；菜单 `sql/wash_menu.sql`（2020 数据看板 / 2021 按钮权限），**导入后需重新登录才显示**
    - 前端 `carwash-admin/src/views/wash/dashboard/index.vue` + `src/api/wash/dashboard.js`
13. **C 端基础数据接口 + 正式下单页已完成**（2026-09-19）：
    - `GET /api/v1/services`（wash-goods）、`GET /api/v1/vehicles`（wash-member，只返回本人车辆）、
      `GET /api/v1/cabinets`（wash-network，带空闲格口数，`full` 由后端判定）—— 契约早已定义，本次补实现
    - 小程序首页 `pages/index/index.vue` 由"种子数据临时入口"改为**真实选择页**（服务/车辆/机柜/时间/备注），
      `DEMO_SERVICE_ID` 等三个常量已删除；新增 `src/api/catalog.ts`
    - 未实现（契约里有、表上没有）：服务分类 `categoryId`、`isDefault`、`distance`（定位未开工）→ 一律返回 null，前端不展示
    - 实测：三接口均返回数据 → 下单 `SE20260919004248124205`（3900 分 / WAIT_PAY）→ 格口自动占为 2/3 → 看板今日单量 1
    - **开发种子已补格口复位**：`wash_seed_dev.sql` 末尾释放 wash_slot，否则联调占满后一直报 B2003
14. **B1 支付超时取消 + B7 紧急取钥匙已完成**（2026-09-19）：
    - `OrderPayTimeoutJob` 每分钟扫 `pay_expire_at` 过期的 WAIT_PAY 单 → `systemCancel`（触发方 JOB），
      复用 `doCancel` 因此回补产能 + 释放格口。**不补会泄漏格口**：不付钱的单永久占柜，新单报 B2003
    - `emergencyTakeKey`：车未动 → 取消 + 全额退款 + 告警留痕；车已动 → 拒绝 B1002
    - 取消原因双写：`wash_order.cancel_reason` + 流转日志（之前只写日志，后台看不到原因）
15. **B4 车辆管理 + B6 订单按钮已完成**（2026-09-19）：
    - 车辆 `POST /vehicles`、`PUT /vehicles/{id}`（越权/不存在统一 A0001）；小程序新增「我的车辆」页
    - 按钮按状态返回：PAY / DEPOSIT_KEY / TAKE_KEY / VIEW_PROGRESS / REVIEW / REORDER / CANCEL / CONTACT_SERVICE；
      前端全部有落点，客服电话走 `GET /api/v1/config/customer-service`（配置化，不硬编码）
16. **B2 站点小区产能 + B8 分类默认车已完成**（2026-09-19）：
    - 建表 `wash_site` / `wash_community` / `wash_service_category`；`wash_service.category_id`、`wash_vehicle.is_default`
    - `/api/v1/site/current`、`/communities`、`/capacity`（产能按站点+日期读 Redis；最晚存钥匙与承诺还车取站点配置）
    - 订单详情补全 `siteName` / `cabinetName` / `promiseReturnTime`（预约日次日 7 点，小时走配置）
    - ⚠️ **产能 Redis key 目前无人初始化**：`capacity:{siteId}:{date}` 不存在时下单按"不限量"放行，
      接口则返回站点上限 —— 要准需补"每日产能初始化"任务
17. **B5 评价售后 + B3 影像已完成**（2026-09-19）：
    - 评价 `POST /orders/{orderNo}/reviews`：rating 1-5，≤4 星强制填原因并告警；一单一评；
      走状态机 `REVIEW_SUBMIT`（WAIT_REVIEW → FINISHED）。小程序新增评价页
    - 售后 `POST /after-sales`：REWASH / REFUND / CLAIM，生成 AF 工单号，只建单不直接改钱
    - 影像 `POST /media/upload` + `GET /media/{fileId}/raw`：本地磁盘（store-path 可配，阈值走配置），
      对外只暴露 fileId，读取做路径穿越校验；换对象存储只改 `WashMediaService.save/resolve`
18. **下一步建议**（仅剩未做的）：
    - **真实微信支付 + 退款**：替换 mock-pay。**当前挂起**——用户尚无微信支付普通商户号（需企业资质 + 备案域名）。
      接入时只填 `WechatPayProvider` + 给 `WxPaymentController.notify` 加验签解密，业务层零改动
    - **柜机硬件对接**：软件侧 `device-callback` 已就绪，缺硬件与协议（当前挂起）
    - **师傅端小程序（取送+作业合并）**：契约只覆盖 C 端，开工前必须先补 `openapi.yaml`（红线）。架构决策：原取送端/作业端合并为单一师傅端（PRD 第3章角色为 客户/工作人员/管理员 三级，取送与作业为子功能；前期2名合伙人自作业）
19. **每日产能初始化已完成**（2026-09-19）：
    - `CapacityInitJob` 每 30 分钟补今天起 7 天（可配 `wash.capacity.days-ahead`）的 `capacity:{siteId}:{date}` key，
      用 SETNX 幂等，不会把已扣减产能重置；`daily_limit=0` 的不限量站点不建 key
    - 重启实测：清掉 Redis 后重启后端，自动生成 2026-09-19 ~ 09-26 共 8 个 key，值 = 站点上限（20）
    - 第 16 条提到的"产能 key 无人初始化"已闭环
20. **后台干预收敛已完成**（2026-09-19）：
    - 白名单 + 原因必填 + 高危事件二次确认，全部在 `WashOrderStateService.adminFire` 与 `AdminOrderController` 落实；
      规则表已同步到《订单状态机规则表.md》3.1 节（SSOT）
    - 实测：event-options 仅返回白名单内事件；PAY_SUCCESS 被拒（B1002）；TAKE_KEY_BACK 未确认被拒、确认后通过；
      空原因被拒（A0001）；前端（carwash-admin）高危事件显示「确认执行」勾选框
    - 白名单（11 允许 / 6 禁止）：禁止 `PAY_SUCCESS`、`APPLY_REFUND`、`REFUND_SUCCESS`、`REVIEW_SUBMIT`、`AUTO_FINISH`、`CANCEL`
21. **促销模块 C 端（优惠券 / 保险）已完成**（2026-09-19）：
    - 后端 `wash-promo`：`GET /api/v1/coupons`（我的优惠券，按 UNUSED/USED/EXPIRED 带模板名）、
      `GET /api/v1/coupons/center`（领券中心，剩余库存=总量-已领）、`POST /api/v1/coupons/templates/{id}/receive`（领取，校验下架/限领/库存）、
      `GET /api/v1/coupons/best`（按订单金额选最优券）、`POST /api/v1/insurance/apply`（投保，模板 id=1 车损险，仅本人车辆）
    - 错误码 C1001（已领完/下架）、C1002（超限领）、C2001（保险模板不存在）、C2002（投保车辆必填），契约与 `ErrorCode` 已同步
    - 小程序新增 `pages/coupons/coupons.vue`（领券中心 + 我的券，UNUSED 显示「去使用」跳首页带 `couponUserId`）、
      `pages/insurance/insurance.vue`（投保 + 我的保单，按 `vehicleId` 回显车牌·品牌）；新增 `src/api/promo.ts`
    - bug 修复：新客券显示「无门槛减 X」、满减券「满 X 减 Y」；我的保单补车辆信息行
22. **下单用券（真正抵扣）已完成**（2026-09-19）：
    - 契约 `openapi.yaml`：`CreateOrderRequest` 增加 `couponUserId`、`CreateOrderVO` 增加 `couponDiscount`、错误码 **C1003**（优惠券不可用）
    - `wash-order` 新增对 `wash-promo` 的依赖；`WashOrderCreateService` 下单时调 `WashCouponService.applyToOrder`
      校验归属/状态/门槛，命中则 `payAmount -= discount` 并 `markUsed(orderNo)`，**同一事务，失败回滚**
    - `WashCouponUserMapper` 补 `selectById`；`WashCouponService.applyToOrder` 校验并标记 USED（乐观更新，重复核销安全）
    - 小程序 `pages/index/index.vue`：`onLoad` 读 `couponUserId` 显示横幅，提交订单带上 `couponUserId`
    - **实测**：领满减券(满3900减300)→ 下单 `WITH-COUPON` 返回 `payAmount=3600 couponDiscount=300`，券状态变 `USED`；无效券返回 C1003 不下单
    - 单测：`WashCouponServiceTest`(14) + 新增 `WashOrderCreateCouponTest`(3) 全绿

---

## 七、还没做的事（别以为做完了）

- **契约只覆盖 C 端**：取送端、作业端、柜机回调、后台管理四块接口未定，已列在 `openapi.yaml` 末尾的 `x-roadmap`，对应模块开工前必须先补契约
- **格口状态机、任务状态机未做**（PRD 4.4 / 4.5 已定义规则）
- ~~**状态机单元测试**~~ ✅ 已补 15 个用例（2026-09-17，全绿）
  - **测试抓到一个真 bug 并已修复**：`FINISHED` 被标记为终态，`fire()` / `canFire()` 用 `isTerminal()` 一刀切拦截，
    导致你确认过的「已完成仍可退款」在代码里走不通。已改为**以流转规则表为准**拦截，
    真正无出边的只有 `REFUNDED`（《订单状态机规则表.md》已同步说明）
- **下单已实现**（2026-09-17）：`POST /api/v1/orders`，含幂等键、协议/归属校验、产能原子抢占、价格快照、支付倒计时。
  实测：下单成功（`SE2026...`，3900 分，`WAIT_PAY`）、同幂等键返回同一单号、产能为 0 时返回 10006
- **正式下单页未做**：小程序首页的"立即下单"是**临时入口**（用种子数据 ID 101/201/301），
  服务项/车辆/机柜三个选择页与接口（`/api/v1/services`、`/api/v1/vehicles`、`/api/v1/cabinets`）都还没实现
- **格口预占未实现**：PRD 要求下单即预占格口，格口表与柜机模块未开工，代码里有 TODO 标注，不要当作已完成
- **状态机已接入业务**（2026-09-17）：模拟支付 → `PAY_SUCCESS` → `WAIT_PAY → WAIT_KEY`，
  走真正的闸机：Redis 锁（`lock:order:{orderNo}`）→ 读状态 → 校验 → CAS 乐观更新 → 写流转日志。
  实测：首次支付成功、重复支付 `21002`、订单不存在 `21001`、日志落库 `WAIT_PAY→WAIT_KEY/PAY_SUCCESS/CUSTOMER`
- **真实微信支付未接**：`mock-pay` 是 x-dev-only 接口，由 `wash.pay.mock-enabled` 控制（生产必须 false）。
  真支付接入只需在回调里调 `WashOrderStateService.paySuccess()`，状态机与日志零改动
- **退款只到 `REFUNDING`**：状态机已推进到"退款中"，但**没有真正调微信退款 API**，
  也没有退款单表/定时任务重试机制——生产前必须补齐，否则钱退不出去
- **格口始终未预占**：下单不预占、取消不释放，因柜机模块未开工（契约要求下单即预占）
- **后台"人工推进"是双刃剑**：现在客服可以一步把订单推到"已完成"（实测就是这么走完链路的）。
  上线前必须：① 限制哪些事件允许后台触发 ② 关键节点（如 FINISHED）要求二次确认 ③ 操作审计可查（日志已有）
- **订单按钮只实现了 PAY**：其余 action（存钥匙/看进度/评价/再来一单）随功能开工补在
  `WashOrderQueryService.resolveMainAction()`，前端不用动
- **订单详情已实现**（2026-09-18）：`GET /api/v1/orders/{orderNo}`，含 **C 端 8 节点时间轴**。
  时间轴**直接读 `wash_order_status_log`** 生成（规则表第七条，不另写一套进度）；
  实测：8 节点齐全、ORDERED 已到达并带时间、当前节点高亮、越权/不存在返回 `21001`
- **详情页部分字段仍为 null**：`siteName` / `cabinetName` / `promiseReturnTime` / 影像证据，
  对应网点表、站点表、影像模块未开工，前端目前不展示空值即可
- **后端错误码数字映射**已写进 `openapi.yaml`（A/B/C 段 → 1/2/3 前缀 + 4 位编号），改动需同步契约与 `ErrorCode.numeric()`
- **`ruoyi-wash` 还没被 `ruoyi-admin` 依赖**：业务模块目前只是"能编译"，尚未接进主工程，订单相关接口一个都还没有
- 若依配置文件改动仅两处：数据库连接串（+`allowPublicKeyRetrieval=true`）、Redis 口令；**未改任何若依 Java 代码**

---

## 八、跨设备同步操作

仓库已有 git remote（origin/main）。换设备前在本机执行：

```bash
git add .
git commit -m "chore: 完成状态机与API契约（防漂移第1-2步）"
git push
```

公司机器上：

```bash
git clone <仓库地址>      # 或 git pull
```

> 若公司机器与本机**不是**同一个仓库（例如用网盘/远程桌面），只要 `CODEBUDDY.md`、`PROGRESS.md`、`订单状态机规则表.md`、`openapi.yaml` 这几个文件在，新会话就能接上。

---

## 九、师傅端迭代（2026-09-20 ~ 2026-09-23）

> 本节记录师傅端（工作人员）从契约到联调的完整过程。**新会话开工前必读本节 + 第一节。**

### 9.1 已完成的里程碑

| 里程碑 | 内容 | commit |
| --- | --- | --- |
| M0 契约 | 补全师傅端 11 个接口（工号登录 + 取送 + 作业 + 3 个打卡）、D 段错误码、`WorkerLoginVO`/`PickTaskVO`/`StationQueueVO` | `57a464d`、`43ccac8`、`9af1632` |
| M1 师傅域 | 新建 `wash-worker` 模块：工号登录、`worker:token:` 独立安全链、`WorkerAuthInterceptor`、`WorkerContext`；C 端拦截器放行师傅端路径 | `9e3bae3` |
| M2 取送端 | 任务池、取车拍照、送回归柜；新增 `WashOrderStateService.workerFire` 作为统一状态机入口 | `6d58f16` |
| M3 作业端 | 工位看板、入场 / SOP / 质检通过 / 质检不合格 / 驶离、取钥匙打卡 | `2fc080a` |
| M4 前端 | 师傅端 API、登录态（模块级 reactive 单例，非 Pinia）、`WorkerTabBar`、登录页、任务池、作业台；「我的」页入口 A | `de22574` |
| M5 联调 | 师傅端全链路 7 步实测通过（KEY_IN → … → RETURNED）；一批联调问题修复 | 多个 fix commit |

### 9.2 关键决策（与第四节的决策同等效力，不要重新讨论）

- **单仓、按角色切换**：师傅端与 C 端同一小程序、同一构建产物。师傅走工号登录（种子 `W001 / dev123`），C 端走微信静默登录；两套 token 分开存，师傅端 `10002` 跳工号登录页（不自动续登）。
- **tabBar 方案**：C 端保留原生 tabBar 不动；师傅端页面不进 tabBar，用 `WorkerTabBar` 组件导航（`redirectTo`）。
- **状态流转**：师傅端一律走 `WashOrderStateService.workerFire`，禁止直接 update 状态；重洗次数用流转日志统计（不在订单表加列，避免两处漂移）。
- **影像读取**：`/api/v1/media/{fileId}/raw` 支持 query token（`image` / `el-image` 带不了请求头）；令牌优先级：会员 → 后台若依（仅 GET，用 request 包装器复用若依 `getLoginUser`，未改若依代码）。
- **客服电话**：后台参数（`wash.cs.platform-phone` 等）→ yml → 代码兜底 `10086`。
- **照片约束**：取车 ≥6 张（`wash.pick.photo-min`）、下单停放照 ≤6 张（`wash.order.park-photo-max`）、上传 ≤10MB（`wash.media.max-size-mb`）。

### 9.3 已知简化（刻意未做，不是遗漏）

- 工位（bay）表未建：看板 `bayNo` / `sopStep` / `startedAt` 为 null，bays 用「清洗中」订单顶上。
- SOP 15 步未落明细表：`steps` 汇总记入日志 reason。
- `qc-fail` 达阈值只 WARN 告警，未自动生成复盘工单。
- 照片 `fileId` 只校验数量，未校验真实性与归属。
- 师傅端影像上传走 C 端接口（依赖会员 token）；专用设备无会员登录态会失败。

### 9.4 待办欠账（下一步）

- `admin-api` 契约未入 `openapi.yaml`（`x-roadmap` 欠账，会随后台接口增多越滚越大）。
- 影像 `store-path` 默认相对路径（跟着进程工作目录走）——**生产必须配绝对路径**，否则老文件 404（已发生过）。
- 后台运营功能：商品（服务项）配置、师傅管理、站点/小区管理、优惠券管理。
- C 端界面改版（对标 `对标产品设计稿/`）。

### 9.5 测试数据红线

- 测试造数**必须走状态机**（用 `sim-flow.ps1`），直接 UPDATE 会让流转日志与状态错位（时间轴出现「已还车已到达、状态却是回程中」）。已发生过一次，用日志重放修复。
- 重启后端前先停进程：java 进程占用 `ruoyi-admin.jar` 会导致 `mvn clean package` 失败。
- 前端联调固定用 `dist/dev/mp-weixin`（dev watch 热更新）；正式发包前才 `build`。

---

## 十、C 端界面改版与商品配置（2026-09-23）

> 依据 `对标产品设计稿/`（22 张，竞品"洗悦 X-CLEAN"）改版 C 端，并让商品（服务项）后台可配置。

### 10.1 设计语言（新主题的基线）
- 主色 teal `#00aeb5`（主按钮/选中态/价格强调）、深墨 `#14342f`（次要按钮/确认）、页面底 `#f2f7f7`、价格红 `#ff5b4a`、圆角 20rpx 白卡片。
- 变量已写入 `carwash-mp/src/uni.scss`（`$cg-brand` 等）。

### 10.2 已改版页面
| 批次 | 页面 | commit |
| --- | --- | --- |
| 第一批 | 全局主题、新增首页 `home`（品牌头图/卖点/服务入口/本月爆品）、下单页 STEP 1–5 + **时间浮层**、订单列表 | `5635428` |
| 第二批 | 我的页、服务页（分类分组）、订单详情（状态头/费用明细/进度/底部操作）、服务详情 | `45eb11f` |
| 第三批 | 机柜页（九宫格）、客服页（问题标签网格 + 双电话 + 企微码） | `b991625` |
| 第四批 | 卡券页（券卡）、车辆页（车牌大卡） | `03ffa30` |
| 收尾 A-1 | 支付页（倒计时 + 底部支付条）、评价页、开箱页、保单页 | `dc0d212` |
| 收尾 A-2 | 服务详情页（封面 + 内容 + 样图 + 底部下单条） | `553cbef` |

**tabBar 变更**：首页指向新建的 `pages/home/home`（下单页 `index` 改为普通页面，恢复原生返回键）。

### 10.3 刻意隐藏的模块（设计稿有、我们无后端支撑）
余额、充值、充值记录、激活码、卡包、地图选站 —— **一律不做假数据**。

### 10.4 商品（服务项）后台配置（B）
- 后端：`wash-goods` 新增 `WashServiceAdminService` + `AdminServiceController`（`/admin-api/wash/service`，list/get/add/edit/remove + `category-options`）。
- Mapper 新增后台列表（含下架项）/ 新增 / 修改 / **逻辑删除**（历史订单引用服务名与价格快照，不能物理删）。
- 价格：后端存**分**，后台表单填元，提交时换算（与全项目金额规则一致）。
- 菜单：SQL `wash_menu.sql` 追加 2030–2034（`wash:service:list/add/edit/remove`），导入后需**退出重登**才显示。
- 前端：`carwash-admin/src/api/wash/service.js` + `views/wash/service/index.vue`。

### 10.5 改版引入并已修复的问题（引以为戒）
- `services.vue` / `service-detail.vue` 用 `switchTab` 跳下单页 → 下单页不再是 tabBar 页会失败，已改 `navigateTo`。
- 下单页 `onShow` 无条件重载 → 从相册返回会滚到顶部且重置已选，改为 `onLoad` 加载 + `onShow` 仅在从子页返回时刷新，且只补全未选项。
- 下单页缺返回键：`pages.json` 去掉 `navigationStyle: custom`。

### 10.6 剩余待办
- **后台「服务详情配置」**（用户明确要求）：详情页的 items/不包含项/注意事项/样图/封面等明细字段，需 `wash_service` 加列 + 后台表单。当前后端详情接口已通（`GET /api/v1/services/{serviceId}`）但明细返回空，前端有空态兜底。
- C 端订单 tab 改「待处理/服务中/已完成」需后端 `OrderTab` 新增取值（跨端小改）。
- 时间浮层时段目前固定 19:00–23:00（后端未下发站点营业时间，代码已标 TODO）。
- D（后台运营补全：师傅管理/站点小区/优惠券管理/工位与 SOP/复盘工单）与 E（`admin-api` 契约入 `openapi.yaml`、fileId 真实性校验、影像鉴权收敛）尚未开始。
- 设计稿图片被重命名为有语义文件名（22 张），**未提交**，待决定。

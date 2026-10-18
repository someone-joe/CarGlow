# 项目进度交接（换设备 / 新会话先看这里）

> 用途：聊天记录不会跨设备，但本文件会。新会话开始时，让 AI 先读本文件即可无损接上。
> 最后更新：2026-09-27（补录后台契约 + 新增第十一节「双机环境差异」）

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
- ~~**退款只到 `REFUNDING`**~~ ✅ 已补齐（2026-10-18）：`wash_order_refund` 表、`RefundRetryJob`
  （5 分钟一轮、上限 5 次）与 `PaymentProvider` 三件套早期就已存在，第七节与第十二节
  "无退款单表/无重试"的描述**已过时**。本次补的是**售后退款闭环**：工单处置（同意）→ 事件 →
  建退款单 → 调渠道 → 状态机 `APPLY_REFUND`+`REFUND_SUCCESS` → 订单 `REFUNDED`、工单自动收尾 `DONE`。
  实测全链路通过，详见第十三节。**唯一未接的是真实微信渠道**（仍占位，依赖商户号，挂起）。
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
| M6 T13 师傅域增强 | 自动派单（钥匙入柜触发 `OrderKeyInEvent` → 同小区优先+负载均衡；无候选进待派单池，在线师傅可抢单；后台可手动派单/改派）；师傅资质（身份证+驾照+人脸核身+审核，派单准入）；周排班（在班判定）；打卡上下班；师傅端「我的」页（打卡/资质）+ 任务池抢单；后台资质审核/排班/派单 UI | 见 T13 commit |

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
- 师傅端影像上传走 C 端接口（依赖会员 token）；专用设备无会员登录态会失败。→ **T13 已解决**：资质照片改走 `/api/v1/worker/media/upload` + `/api/v1/worker/media/{fileId}/raw`（独立 worker 鉴权，与会员/后台影像读取走不同拦截器）。

### 9.4 待办欠账（下一步）

- ~~`admin-api` 契约未入 `openapi.yaml`~~ ✅ 已完成（2026-09-27，E-1）：后台接口已全量入契约（tag `admin-*`）。
- 影像 `store-path` 默认相对路径（跟着进程工作目录走）——**生产必须配绝对路径**，否则老文件 404（已发生过）。
- 后台运营功能：商品（服务项）配置 ✅、师傅管理 ✅、站点/小区管理 ✅、优惠券管理 ✅（2026-10-18，见第十四节）。**后台运营补全基本收口**。
- C 端界面改版（对标 `对标产品设计稿/`）。
- **T13 后端已实测通过（2026-10-17）**：后台接口全通——资质分页（PENDING / PASSED 均可）、周排班读写、派单候选、待派单池、手动派单/改派（`worker_id` 由 1 改 2 已生效）、师傅进行中订单；师傅端登录 / 档案 / 打卡 / 资质提交与查询 / 待派单池 / 抢单均通过。
  - ⚠️ **后台登录路径是 `/login`，不是 `/admin-api/login`**：`/admin-api` 前缀写在各后台 Controller 的类级 `@RequestMapping` 上，而 `SecurityConfig` 只放行 `/login`。自测时 curl 打到 `/admin-api/login` 必然 401——**那不是基建缺陷**，曾据此误判过一次，别再踩。
- **T13 仅剩 1 项未验**：钥匙入柜（`DEPOSIT_KEY` → `KEY_IN`）触发的**自动**派单尚未在真实链路（下单 → mock-pay → 柜机回调 `device-callback/v1/slot/deposit`）跑通。已跑通的是共用同一套 `assignWorker` 写入路径的另外两条线：师傅抢单、后台手动派单/改派。
- **本地 dev 数据被本次联调改过**（换设备 / 新会话须知）：`W001` / `1111` 口令被改为 `123456`（原 `dev123`）、`community_id` 设为 1、`work_status` 被打卡过；`wash_worker_qualification` 里有一条 worker_id=1 的 PASSED 测试数据（`id_name` 曾因用 `docker exec mysql` 未带 `--default-character-set=utf8mb4` 造成中文双重编码，已按第九节红线修正）。

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

### 10.6 后续计划（E / D）与已完成项

**已完成（2026-09-24 夜间，bug 修复后继续推进）**
- ✅ **订单 tab 三分组**：契约 tab 枚举 `PENDING / SERVING / DONE`（待处理=待支付+待存钥匙；服务中=钥匙已入柜→还车；已完成=待评价/已完成/已取消/退款中/已退款）。后端 `WashOrderQueryService` 是映射唯一真源，**保留旧取值 ONGOING/WAIT_REVIEW/ALL 兼容老包**。实测：`PENDING=0 / SERVING=5 / DONE=37`。
- ✅ **时间浮层接站点营业时间**：`wash_site` 加 `business_start` / `business_end`（默认 19:00/23:00，SQL `wash_20260924_site_hours.sql`），经 `CapacityVO` 下发，前端 `timeSlots` 改 computed 按整点生成（异常值兜底 19:00-23:00），不再写死。

**E（正确性收口，下一步做）**
1. ✅ **已完成（2026-09-27，换到 Mac 后第一轮）**：`admin-api` 契约入 `openapi.yaml` —— 后台 4 个控制器共 13 路径 / 15 个操作全部落契约（tag `admin-*`），新增 14 个后台 schema。关键结论（别改）：
   - 后台返回体**不套 Result**，沿用若依 `AjaxResult` / `TableDataInfo`：成功码是 **200** 不是 0，分页是平铺的 `{ total, rows }` 不是 `data.list`。
     原因：RuoYi-Vue3 的请求拦截器按 `code === 200` 判成功，改成 0 会让全后台把成功响应当错误弹出来。
     已写进 `info.description` 与 `RuoYiResult` / `RuoYiTableDataInfo` 的说明里。
   - 每个操作都标了 `x-permission`（`wash:order:list` 等），必须与后端 `@PreAuthorize`、`wash_menu.sql` 三处一致。
   - 字典类接口（`status-options` / `event-options` / `category-options`）在契约里写明"来源唯一、前端不得自带名单"；
     `EventOption.confirmRequired` 由后端下发，哪些事件算高危不由前端判断。
   - 后台工单详情复用了 C 端同一个 `OrderDetailVO`（同一套 8 节点时间轴），避免两端字段漂移。
   - 后续已在 Mac 装好 Node 并完成类型重新生成（`src/api/schema.d.ts` 83KB → 113KB）。
2. ✅ **已完成（2026-09-27）**：`fileId` 真实性校验。原先只校验照片数量，传任意字符串都能推进状态。
   - **前置依赖先打通**：`uploadMedia` 本就支持 orderNo 但两处没传——`worker-station` 的洗后照（WASHED）、
     以及 `worker-tasks` 的 `choosePhotos()`。已全部补上。
   - **顺带修了一个真实的错配**：归柜照片 `choosePhotos()` 里硬编码 bizType=PICK，而契约 `return-done` 要求 RETURN。
     后端不校验时看不出来，**一加校验归柜就会全量失败**，属于必须提前修的雷。
   - 落点：`WashMediaService.requireBelongToOrder(fileIds, orderNo, bizType)` 是归属校验的唯一实现处，
     三个师傅端入口（pick-car-done / return-done / qc-pass）都调它，规则不散落。
   - **下单侧特殊处理**：停车照在下单前上传，此时没有订单号，只能按「本人 + PARK + 尚未归属」校验
     （`requireOwnParkPhotos`），**订单号生成后再 `bindToOrder` 回填**（SQL 带 `order_no is null`，已归别的单的抢不走）。
     此前下单完全不回填，影像与订单之间没有关联，事后查不到当时拍了什么。
   - 新增错误码 **B1005**（影像不存在或不属于当前订单），契约与 `ErrorCode` 已同步；新增 11 个单测全绿。
3. 影像鉴权收敛：前端 `photoUrl()` 用 query token（`?token=`，因 `image` 组件带不了 header，`MediaAuthInterceptor` 已支持）。待定：统一为 query token 唯一方式，还是限制有效期/加签。

**D（后台运营补全，最后做，量大）**
- ✅ **D-1 师傅管理已完成（2026-09-27）**：`wash_worker` CRUD + 菜单，契约 tag `admin-worker`（7 路径 9 操作）。
  - 接口：`/admin-api/wash/worker` 的 list / get / add / edit / remove + `reset-password` + 两个字典项
    （`status-options` / `site-options`）。权限 `wash:worker:list|add|edit|remove|reset`，菜单 SQL 2040–2045。
  - **新增枚举 `WorkerStatus`（NORMAL / DISABLED）作为 SSOT**，与 openapi 的 WorkerStatus 逐条一致。
    原先 NORMAL 是 `WashWorkerService` 里的私有字符串常量，后台同样要判断状态，再写一份就会漂移。
  - 新增错误码 **D4005（工号已被占用）/ D4006（师傅不存在）**。
  - 三个刻意的设计（别改）：
    1. **修改资料不接受 password** —— 改密码走独立的 `reset-password` 接口和 `wash:worker:reset` 权限，
       避免「运营改个手机号，师傅被登出」的误操作。
    2. **禁用 / 重置密码 / 删除都会吊销该师傅的登录态**。
       实现是扫 `worker:token:*` 比对 workerId：token 是 UUID，Redis 只有正向映射，
       规模极小，比改 TokenService 的签发逻辑划算，也没动公共组件。
    3. **工号不允许修改**（是登录凭证），要换就删除后新建。
  - 出参一律走 `AdminWorkerVO`（**不含 password**，Service 里查完即清空），
    绝不直接返回实体，避免将来给实体加字段时把密码带出去。
  - 为什么不用 WashWorker 实体当入参：实体只有登录字段（没 create_time / remark），
    补字段会把登录链路牵扯进来，故与 `AdminOrderVO` 一致——由 Mapper 直接查成 VO。
  - 验证：13 个单测全绿（`WashWorkerAdminServiceTest`），后台前端 `vite build` 无错。
  - ⚠️ 菜单 SQL 导入后**后台需退出重登**才显示（若依菜单缓存）。
- ✅ **站点 / 小区管理已完成（2026-10-08 核实）**：`AdminSiteController` + `WashSiteAdminService`（站点）、
  `AdminCommunityController` + `WashCommunityAdminService`（小区）均已落地，前端 `carwash-admin` 对应表单与列表页已存在。
  站点新增曾因 `deposit_deadline` 静态 insert 报 NOT NULL，已改为动态 SQL 修复（见 `WashSiteMapper`，commit ead7e0c）。
- ~~**优惠券管理**~~ ✅ 已完成（2026-10-18）：券模板 CRUD + 上下架 + 逻辑删除 + 运营发放 + 领用/核销记录，
  契约 tag `admin-coupon`，后端 `WashCouponAdminService` / `AdminCouponController`，详见第十四节。
- 工位（bay）与 SOP 15 步明细（当前无工位表，`station/queue` 的 bayNo/sopStep 为 null）
- 复盘工单（`qc-fail` 达阈值目前只 WARN，未自动建单）

### 10.6.1 F（后台服务详情配置，2026-10-08 已完成）

> 用户明确要求：服务详情的 items / 不包含项 / 注意事项 / 样图 / 封面可由运营后台配置，C 端详情接口返回。

- `wash_service` 新增 6 列：`detail_items` / `detail_not_included` / `detail_notices` / `detail_sample_images`（TEXT，存 JSON 数组）、
  `applicable_models` / `cover_url`（迁移 `sql/wash_20261008_service_detail.sql`，幂等，列已存在则跳过）。
- 新增 `JsonStringListTypeHandler`（`ruoyi-wash/wash-common`，复用 fastjson2）：数组字段在 DB 以 JSON 文本存取，实体用 `List<String>`，
  前后端 JSON 直接是数组，格式转换不散落。
- `WashService` 实体 + `WashServiceMapper`（全查询带别名映射 + `@ResultMap`、insert/update 带 typeHandler）；
  `WashServiceQueryService.detail()` 填充明细；C 端 `ServiceDetailVO` 补 `cover`。
- 契约 `openapi.yaml`：`ServiceDetailVO` / `AdminServiceVO` / `AdminServiceRequest` 补齐
  `items` / `notIncluded` / `notices` / `sampleImages` / `applicableModels` / `cover`（C 端 `cover` 为新增字段）。
- 后台前端 `carwash-admin/src/views/wash/service/index.vue` 新增明细表单（多行文本 ↔ 数组转换）。
- 小程序 `schema.d.ts` 已据 openapi 重新生成（仅新增字段，+30 行）。
- 验证：新增带明细的服务项，后台回查与 C 端 `GET /api/v1/services/{id}`（会员 token）均正确返回数组。
- ⚠️ **C 端小程序服务详情页尚未渲染这些字段**：数据已就绪，页面展示是独立前端任务（空态兜底仍在）。
  → **此条已过时（2026-10-18 核实）**：`pages/service-detail/service-detail.vue` 已渲染
  `coverUrl` / `items` / `notIncluded` / `notices` / `sampleImages`，无需再改。

### 10.7 原剩余待办（未开始）
- ~~**后台「服务详情配置」**（用户明确要求）~~ ✅ 已完成（2026-10-08）：`wash_service` 加 6 列 + 后台表单 + C 端详情接口已返回明细（见 10.6 已完成项 3）。C 端小程序页面渲染为独立前端任务。
- ~~C 端订单 tab 改「待处理/服务中/已完成」~~ ✅ 已完成（10.6 已完成项，`70c1dbd` + `74e589d`）
- ~~时间浮层时段固定 19:00–23:00~~ ✅ 已完成（站点营业时间经 `CapacityVO` 下发，`70c1dbd`）
- ~~设计稿图片重命名未提交~~ ✅ 已提交（`74de9be`）
- E 的 `admin-api` 契约 ✅ 已完成（见 10.6）；剩余 fileId 真实性校验、影像鉴权收敛未做
- D（师傅管理 ✅ / 站点小区 ✅ / 优惠券管理 / 工位与 SOP / 复盘工单）：部分完成
- ~~**后台「服务详情配置」~~ ✅ 已完成（2026-10-08，见 10.6 已完成项 3）

---

## 十一、开发环境：Windows 机 ↔ Mac 机（2026-09-27）

> Windows 是主力开发环境；Mac 目前**能跑中间件，但没有 Java / Maven / Node**。换机器前先看这一节，别浪费一轮去试命令。

| 工具 | Windows（`d:/CarGlow`） | Mac（`/Users/joe/CarGlow`） |
| --- | --- | --- |
| JDK 17 | ✅ | ✅ **2026-09-27 装好**（brew openjdk@17，17.0.20.1） |
| Maven | ✅ | ✅ **2026-09-27 装好**（3.9.16，走阿里云镜像） |
| Node | ✅ v20.2 | ✅ **2026-09-27 装好**（v26.10.0） |
| Docker 中间件 | ✅ Docker Desktop | ✅ **2026-09-27 装好并启动**（见 11.1） |

### 11.1 Mac 的 Docker 中间件（已就绪）

Mac 上没装 Docker Desktop，走的是 **colima**（VM 形态的容器引擎）+ brew 的 docker CLI。
容器里的 MySQL / Redis 与 Windows 完全相同：同一份 compose、同一套口令。

```bash
export PATH="/opt/homebrew/bin:$PATH"     # 该终端默认没带 brew 路径，别漏

colima start                              # 起容器引擎（已配置 4核/8G/60G，重开机后执行这条）
docker compose -f docker/docker-compose.dev.yml up -d   # 起 MySQL 8 + Redis 7
colima status                             # 查看状态
colima stop                               # 收工停引擎（数据保留在 named volume）
```

- 连接信息：`root / carglow_dev`，`127.0.0.1:3306`、`127.0.0.1:6379`，库 `carwash`（已自动创建）。
- **当前 Mac 的 MySQL 是空库**：`carwash-server/sql/` 下的脚本一份都还没导入，跑后端前必须先导。
- ⚠️ **别让它再去下载 colima 镜像**：本机直连 GitHub 只有 ~2.5KB/s（332MB 要下几十小时）。
  已用 `gh-proxy.com` 代理拉到本地 `~/Downloads/colima-ubuntu-24.04-docker-arm64.raw.gz`（317MB），
  将来重建环境用 `colima start --disk-image ~/Downloads/colima-ubuntu-24.04-docker-arm64.raw.gz` 复用。
- ⚠️ brew 装的 docker CLI **不含 compose**：已 `brew install docker-compose`，
  并在 `~/.docker/config.json` 配了 `cliPluginsExtraDirs: ["/opt/homebrew/lib/docker/cli-plugins"]`。
  没这一步就只有 `docker-compose`（连字符），用不了 `docker compose`（空格）——两者等价。

### 11.2 分工建议

> 2026-09-27 更新：JDK / Maven / Node 均已装好，Mac 现在**能编译、能跑测试、能起服务**，
> 不再受「只能改文档」的限制。下面两条是当时的判断，留作背景。

- **Mac 能做的**：纯文档与契约（`openapi.yaml`、各类 `.md`）、SQL 审阅、代码走查、git 操作。
- **后端编译与测试**（已验证可用）：

```bash
source ~/.zshrc          # 或新开终端；JAVA_HOME 只在 ~/.zshrc 里配了，没走 sudo 软链
cd carwash-server
mvn -B -pl ruoyi-wash/wash-order,ruoyi-wash/wash-worker -am test
```

  ⚠️ **必须带 `-am`**：只写 `-pl` 会因为 `wash-common`、`ruoyi-framework` 等模块没进本地仓库而
  报 `Could not find artifact`——那不是依赖下不到，是本仓库的兄弟模块没构建。
- **小程序类型检查**：`cd carwash-mp && npm run type-check`（依赖已装；`npm install` 走 npmmirror 镜像）。

### 11.3 契约改动后的必做动作

```bash
npx openapi-typescript openapi.yaml -o carwash-mp/src/api/schema.d.ts
```

不重新生成的话，小程序侧 TS 类型还是旧的，编译期发现不了字段漂移。

### 11.4 Mac 上起后端（2026-09-27 跑通）

数据库和中间件都好了，但**直接 `java -jar` 在 Mac 上会启动失败**（Windows 上不会），原因与绕法如下：

- `logback.xml` 里 `log.path` 硬编码 `/home/ruoyi/logs`。Windows 上会被解析成当前盘下的目录并自动创建；
  Mac 是 Unix，`/home` 在根分区，没权限创建 → Logback 报错 → Spring Boot 直接判定启动失败。
- `log.path` 是硬编码（没写成 `${LOG_PATH:-...}`），所以 `-Dlog.path=` 覆盖不了。
- **绕法（不改仓库文件，避免影响 Windows）**：复制一份改好路径的配置放到外部，启动时同时给两个参数：

```bash
mkdir -p ~/carwash-logs
sed 's|/home/ruoyi/logs|'"$HOME"'/carwash-logs|g' \
  carwash-server/ruoyi-admin/src/main/resources/logback.xml > /tmp/logback-mac.xml

cd /Users/joe/CarGlow
java -Dlogback.configurationFile=/tmp/logback-mac.xml \
     -jar carwash-server/ruoyi-admin/target/ruoyi-admin.jar \
     --logging.config=file:/tmp/logback-mac.xml
```

  两个参数缺一不可：logback 原生阶段和 Spring Boot 接管阶段各读一次，只给一个仍会失败。
- 打 jar：`cd carwash-server && mvn -B -DskipTests -pl ruoyi-admin -am package`（产物 `ruoyi-admin/target/ruoyi-admin.jar`）。
- 停进程：改代码重新打包前必须 `pkill -f ruoyi-admin.jar`，否则 jar 被占用（Windows 上踩过同一个坑）。

**已验证（2026-09-27）**：SQL 全量导入 51 张表 → 后端启动 → `GET /captchaImage` 正常返回、
师傅端 `POST /api/v1/worker/login`（W001 / dev123）返回 `code=0` 与 token。

导入 SQL 的顺序（有依赖，别乱）：

```bash
cd carwash-server/sql
for f in ry_20260417.sql quartz.sql wash_20260917_minimal.sql wash_20260918_order.sql \
         wash_20260919_pay.sql wash_20260919_promo.sql wash_20260919_refund.sql \
         wash_20260919_site_review_media.sql wash_20260919_slot.sql wash_20260920_worker.sql \
         wash_20260924_site_hours.sql wash_20261008_service_detail.sql wash_menu.sql wash_seed_dev.sql; do
  docker exec -i carglow-mysql mysql -uroot -pcarglow_dev --default-character-set=utf8mb4 carwash < "$f"
done
```

> ⚠️ 上面那条命令会在 stderr 打 `[Warning] Using a password on the command line interface can be insecure`，
> 这是 MySQL 的提示不是报错，别误判成导入失败。用 `show tables` 复核。
> 中文编码已用 hex 复核过（`标准洗车` = `E6A087E58786E6B497E8BDA6`，是真 UTF-8 不是双重编码）。

### 11.4.1 全套服务一键启动顺序（2026-10-07 补全）

之前只记了后端，漏了后台管理前端——它就是 `http://localhost:1024` 打不开的原因。
四个进程各自独立，按下面顺序起（重开机后从第一条开始；平时只重启挂掉的那个）：

```bash
# 0. 容器引擎（只有重启电脑后才需要；平时跳到 1）
colima start --disk-image ~/Downloads/colima-ubuntu-24.04-docker-arm64.raw.gz \
             --cpu 4 --memory 8 --disk 60 --arch aarch64
#    ⚠️ daemon 起得慢，start 完若 docker 命令报错，等几秒再试，别急着 stop 重建

# 1. 中间件
docker compose -f docker/docker-compose.dev.yml up -d     # MySQL 3306 / Redis 6379

# 2. 后端 API（8080）—— logback 路径坑见 11.4
sed 's|/home/ruoyi/logs|'"$HOME"'/carwash-logs|g' \
  carwash-server/ruoyi-admin/src/main/resources/logback.xml > /tmp/logback-mac.xml
java -Dlogback.configurationFile=/tmp/logback-mac.xml \
     -jar carwash-server/ruoyi-admin/target/ruoyi-admin.jar \
     --logging.config=file:/tmp/logback-mac.xml

# 3. 后台管理前端（1024，RuoYi-Vue3）—— 这是 localhost:1024 那个页面
cd carwash-admin && npm run dev        # /dev-api 代理到 8080；admin / admin123 登录

# 4. 小程序（可选，开发预览用）
cd carwash-mp && npm run dev:mp-weixin # 产物 dist/dev/mp-weixin，微信开发者工具导入
```

检查是否全活：`docker ps`（两个 healthy）、`curl -s localhost:8080/captchaImage`（200）、
浏览器开 `http://localhost:1024`（200）。

> 后台和小程序的 `npm run dev` 是 watch 长驻进程，关终端前先 `Ctrl+C`；
> 重打包后端前 `pkill -f ruoyi-admin.jar`，否则 jar 被占用。

### 11.6 后台前端去若依化（2026-10-07）

CarGlow 后台基于 RuoYi-Vue3，但侧边栏菜单由后端 `sys_menu` 驱动，默认会把一整套若依自带
模块（系统监控 / 系统工具 / 若依官网 + 系统管理全部子项）一起显示。本次按"只删明显无用的"原则清理：

**删除（2026-10-07）**
- 菜单（写进 `sql/wash_menu.sql` 顶部 delete，重导 SQL 即生效）：系统监控(2) 整目录、系统工具(3) 整目录、若依官网(4)，含 `sys_role_menu` 关联
- 前端视图：`views/monitor/` 下 online/job/druid/server/cache（留 operlog/logininfor，日志管理 108 用的是这俩）
- 前端视图：`views/tool/` 整目录（代码生成/表单构建/swagger）
- 前端 api：`api/monitor/` 的 cache/job/jobLog/online/server、`api/tool/` 整目录（留 operlog/logininfor）
- 首页样板页 `views/index.vue`（纯若依宣传页），根路径改重定向到本项目看板 `/wash/dashboard`
- navbar 若依品牌下拉：`components/RuoYi/Git`、`components/RuoYi/Doc` 及 Navbar 中的引用

**保留**（用户拍板：系统管理全留，权限根基）
- 用户/角色/菜单/部门/岗位/字典/参数/通知 + 日志管理(108)下的操作日志/登录日志

**验证**：`npx vite build` 通过（exit 0，无悬空 import）；DB 重导后监控/工具/官网菜单为空、日志管理仍在；`http://localhost:1024` 首页 200。

> ⚠️ 若重导 `ry_20260417.sql`（默认菜单源），被删的监控/工具/官网会回来——因为删除语句只在 `wash_menu.sql` 里。
> 正常走"导 ry 基础库 → 导 wash_menu.sql"的顺序即可保持干净。

### 11.5 跨设备同步

仓库 `origin = https://github.com/someone-joe/CarGlow`，走 git 即可。
离开 A 机前 `git push`，到 B 机后 `git pull --ff-only`（2026-09-27 已验证：22 个 commit 干净快进）。

---

## 十二、上线前提醒（Go-live checklist）

> 本地开发为便利临时放宽的项、以及已知的生产阻断项，上线/交付前逐条核对并改回。

- **后台登录验证码（本地已关，上线必须开）**：`sys_config.sys.account.captchaEnabled` 当前为 `false`（2026-10-08 本地开发关掉，方便登录）。
  注意 RuoYi 把 `sys_config` 缓存在 Redis（key `sys_config:sys.account.captchaEnabled`），改 DB 后需清该缓存键或重启后端才生效。上线前设为 `true`。
- **mock-pay 必须关**：`wash.pay.mock-enabled` 生产必须 `false`，否则钱不会真收到（见第七节）。
- **退款骨架已就绪，但真实微信渠道仍未接**：退款单 + 重试 + 售后闭环已落地（2026-10-18，见第十三节），
  但 `WechatPayProvider.refund` 仍是**会明确报错的占位实现**（不是假装成功）。接真实退款仍需
  商户号 + API 证书 + 备案回调域名，属外部依赖挂起。**接渠道前退款均为 mock，生产前必须替换，
  否则钱退不出去**。
- **影像存储路径必须绝对路径**：`wash.media.store-path` 默认相对路径（跟进程工作目录走），生产必须配绝对路径，否则老文件 404（已发生过，见第九节）。
- ~~**admin-api 契约补入 openapi.yaml**~~ ✅ 已完成（2026-09-27，E-1）：后台 4 个控制器共 13 路径 / 15 操作已全部落契约（tag `admin-*`），详见第十节 10.6。本条已从上线阻断项移除。

---

## 十三、退款落地（2026-10-18）

> 结论先行：**退款骨架早就存在，本次补的是「售后 → 退款」这一段闭环；真实微信渠道仍挂起。**

前情 `wash_order_refund` 表、`RefundRetryJob`（5 分钟重试、上限 5 次）、
`PaymentProvider` 三件套（mock 可用 / wechat 占位报错）早期均已落地，
第七节与第十二节"无退款单表 / 无重试"的描述**已过时**；真正的断点是售后——
`WashAfterSaleMapper` 原先只有一个 `insertAfterSale`，工单插进去就没人管了。

### 13.1 本次补的链路

```
C 端提交售后（REFUND）
  → 后台列表可见（带订单上下文：orderStatus / serviceName）
  → 后台处置 accept=true
  → wash-order 发 AfterSaleRefundEvent
  → wash-pay 监听：建退款单 + 调渠道
  → 成功：状态机 APPLY_REFUND → REFUND_SUCCESS，订单置 REFUNDED
  → wash-pay 发 RefundSucceededEvent
  → wash-order 监听：售后工单收尾 DONE
```

### 13.2 关键口径（别改）

- **"运营点了同意" ≠ "钱已退"**：钱是否到账以 `wash_order_refund.status` 为准（SUCCESS 才是真退了），
  订单状态 `REFUNDING` 只表示处理中。处置接口返回成功也只表示已受理。
- **退款成功后必须收尾，且绝不能回滚退款结果**：`WashPayService.advanceAfterRefund` 单独 try 兜住异常——
  钱在渠道已经扣了，若让事务回滚，退款单会退回 INIT 并被定时任务**再退一次**，
  那是真正的重复打款事故。宁可让订单状态暂时没跟上（运营在退款单页看得到 SUCCESS）。
- **依赖方向单向 `wash-pay → wash-order`**：售后→退款、退款到账→工单收尾，两个方向都用应用事件，
  与 T13 的 `OrderKeyInEvent` 同一套路。订单域永远不知道渠道长什么样。
- **幂等**：处置 SQL 带 `status = 'INIT'`（两个运营同时点只有一次生效）；SUCCESS 退款单拒绝重复退款。

### 13.3 实测（2026-10-18，mock 渠道）

会员提交 REFUND 工单（200000 分）→ 后台同意 → 退款单 SUCCESS → 订单 `FINISHED → REFUNDED`
→ 工单自动 `DONE`（处置人 admin、留痕时间齐全）。
另验：重复处置被拒、SUCCESS 重退被拒、驳回不填原因被拒、CLAIM 类不触发退款。

### 13.4 仍未做

- **真实微信退款 API**（`WechatPayProvider.refund` 占位，需商户号 + API 证书 + 备案域名）— **挂起**
- **部分退款**（规则表第七节决策 1：仅生成退款单、订单状态不变）— 当前无部分退款场景，未开工
- ~~**后台前端「售后工单处置」页与「退款单」页**~~ ✅ 已完成（2026-10-18）：
  `views/wash/aftersale/index.vue`（列表 + 处置弹窗，驳回强制填原因）、
  `views/wash/refund/index.vue`（以退款单状态为准 + 人工重试，SUCCESS 前端先挡一道）。
  菜单 2151 / 2152，SQL `wash_20261018_admin_pages_menu.sql`。
- ⚠️ **后台登录路径是 `/login` 不是 `/admin-api/login`**（`/admin-api` 写在各后台 Controller 类级
  `@RequestMapping` 上，`SecurityConfig` 只放行 `/login`）。自测 curl 别打错，曾据此误判为基建缺陷。

---

## 十四、后台优惠券管理（2026-10-18）

> C 端券能力（领券中心 / 领取 / 我的券 / 最优券 / 下单核销）早已存在，本次补的是**运营侧管理**。

### 14.1 接口（契约 tag `admin-coupon`，路径前缀 `/admin-api/wash/coupon`）

| 能力 | 方法 + 路径 | 权限 |
| --- | --- | --- |
| 模板分页 | GET `/template/list`（name/type/status） | `wash:coupon:list` |
| 模板详情 | GET `/template/{templateId}` | `wash:coupon:list` |
| 新增模板 | POST `/template` | `wash:coupon:add` |
| 修改资料 | PUT `/template` | `wash:coupon:edit` |
| 上/下架 | POST `/template/{templateId}/status` | `wash:coupon:edit` |
| 删除（逻辑） | DELETE `/template/{templateId}` | `wash:coupon:remove` |
| 发放 | POST `/grant` | `wash:coupon:grant` |
| 领用/核销记录 | GET `/record/list` | `wash:coupon:list` |

### 14.2 关键口径（别改）

- **编辑接口刻意不接受 `total` / `issued`**：模板一旦发行过，总量就是历史账，
  改了会让"已领 / 剩余"和真实发出的券对不上。**要加量请发新模板**。
  SQL 里干脆不出现这两列，是结构性保证而不是靠人记得。（新增时才校验 total。）
- **发放与 C 端领取共用同一套库存扣减**：`incrementIssued`（乐观更新 `issued < total`）+
  插入用户券，后台发放不是另一条路。否则迟早出现"后台发了 100 张但库存没扣"。
- **`perLimit` 对后台发放一样生效**：否则等于绕过限领规则，同一个号能领一叠。
- **上下架与改资料分成两个接口**：避免"改个名字顺手把券下架了"。
- **会员手机号在 SQL 层脱敏**（`138****1234`）：不把明文带进 Java 层，
  与后台订单 / 会员列表同一口径（个保法最小化）。查询按明文匹配、结果脱敏返回。
- **EXPIRED 是计算值不是库状态**：库里存 UNUSED，只是过了有效期，按 EXPIRED 展示。
- `remain` 由后端算好下发，前端不要自己减（运营刚改完 total、前端还没刷新时会错）。

### 14.3 实测（2026-10-18）

新增模板（id=4）→ 发放给会员 1 → 超限领被拒（每人限领 1）→ 领用记录返回
`couponTemplateId=4`、`memberPhone=138****5678`（脱敏生效）→ 改资料（total/issued 未被改动）→
下架 → 下架后发放被拒 → 倒贴校验被拒（减免 > 门槛）→ 逻辑删除 → 删后详情被拒。

### 14.4 仍未做
- 批量发放 / 按群发（当前只做单张发放：批量涉及库存与限领核算，容易出错，先不做）
- ~~后台前端「优惠券管理」页面~~ ✅ 已完成（2026-10-18）：`views/wash/coupon/index.vue`（模板表单 / 上下架 / 发放弹窗 / 核销记录抽屉），
  菜单 2150（`C`），SQL `wash_20261018_admin_pages_menu.sql`
- ⚠️ **新菜单导入后必须退出重新登录才显示**：若依侧边栏按角色权限缓存（`sys_menu` 变动不主动刷新），
  本地开发反复踩过。另外 `npm run dev` 若遇 1024 被占用会自动换端口（如 1025），代理仍指向 8080 不受影响。

---

## 十五、C 端补强（2026-10-18）

> 起因：核查「C 端还有什么没做」时，发现三处「有后端无前端 / 有前端无入口」的断点，
> 其中第一处是我前几轮自己留下的洞。

### 15.1 订单展示派单信息（T13 的 C 端闭环）

- `wash_order` 新增 `worker_name` 快照列（SQL `wash_20261018_order_worker_snapshot.sql`）：
  派单时把师傅姓名一起写进订单。
  **为什么用快照而不是 C 端去 join 师傅表**：wash-order 不能反向依赖 wash-worker（会成环），
  跨模块也不允许直接查对方的表。与 `service_name` 下单快照是同一个思路。
- 派单三处写回（自动派单 / 抢单 / 后台改派）都带上 workerName，
  `assignWorker` / `forceAssignWorker` 同步加参数（改派也会更新姓名）。
- `OrderListItemVO` / `OrderDetailVO` 增加 `workerId` / `workerName` / `assignedAt`，契约同步。
- 小程序：订单列表显示「已派单 · 张师傅 · 时间」（teal 高亮），订单详情「订单信息」里加「接单师傅」行。
- 实测：改派后 C 端详情与列表均正确返回 `workerName=张师傅` + `assignedAt`。
- ⚠️ 踩过的坑：XML 的 resultMap 加了映射不算完，**各 select 的列清单也得加 `worker_name`**，
  否则查出来是 null（第一次就栽在这，详情返回 workerName=null）。

### 15.2 售后 / 退款进度（补上我自己留的洞）

- 背景：上一轮做了 `GET /api/v1/after-sales`、`GET /api/v1/refunds` 的后端，
  但小程序**既没有页面也没有入口**——用户提交售后后石沉大海，后台能处置、用户侧毫无感知。
- 新增 `pages/aftersale/aftersale.vue`（tab：售后工单 / 退款记录）+ `api/aftersale.ts`，
  `mine.vue` 加「售后与退款进度」入口。
- 退款类工单展开时，直接把该订单的退款单状态带出来，客户不用自己猜钱到哪了。
- 新增 `AfterSaleStatus` / `RefundStatus` 枚举作为 SSOT，VO 增加 `statusLabel`
  ——**前端只展示不翻译**，不在前端维护第二份状态名单（与 OrderStatus 同一口径）。
- 实测：两个接口均返回正确 statusLabel（已驳回 / 已完成 / 已退款）。

### 15.3 投保入口（功能早已完整，只缺入口）

- `pages/insurance/insurance.vue` 早已完整实现（保障方案 / 选车辆 / 投保 / 我的保单），
  但全仓库搜不到任何跳转它的代码——**功能实际不可达**。`mine.vue` 加「车损保障」入口。
- 实测：投保成功（保单 `ACTIVE`，保障期一年，保费 990 分），我的保单可回查。
- ⚠️ **文档坑**：PROGRESS 里写的投保路径 `/api/v1/insurance/apply` 是过时的，
  契约 / 后端 / 前端三方实际一致为 `POST /api/v1/insurances`。curl 自测别照旧文档打（栽过一次）。

### 15.4 手机号授权补全（前后端原本都断）

- 背景：契约 `LoginRequest` 有 `phoneCode`，但 Controller 只传 `code`、Service 完全不处理；
  前端 `login(code)` 也不传、更没有 `getPhoneNumber` 按钮——**整条链路是断的**。
- 后端新增 `WxPhoneService`：`phoneCode → access_token → getuserphonenumber → 手机号`，
  与 `resolveOpenid` 同一套降级策略（未配 secret 直接返回 null，**绝不返回假手机号**）。
- `WashAuthService.login(code, phoneCode)` + `WashMemberMapper.bindPhone`
  （SQL 自带「手机号为空才更新」，并发授权只写成功一次；已有手机号不覆盖）。
- 前端：`login(code, phoneCode?)`；`mine.vue` 未绑定时显示原生 `getPhoneNumber` 按钮，
  授权回调把 code 交后端换号——**前端不碰手机号明文**（微信新版也不允许前端直接拿）。
- 实测：dev 模式带 phoneCode 登录**优雅降级不报错**，会员 `phone` 仍为 NULL。

### 15.5 微信支付 / 退款的完整度结论

**逻辑已完整，只差真实接入**（符合「有资质再实现，但要确保逻辑功能完整」）：

- 渠道抽象 `PaymentProvider` + 条件选择 `PayProviderConfig`（`wash.pay.provider`，默认 mock）
- mock 实现可用；wechat 是**会明确报错的占位**，绝不假装成功
- 回调入口 `notify` → `handlePaidNotify` → `paySuccess` 状态机推进完整
- 退款链路第十三节已闭环（退款单 + 重试 + 状态机 + 工单收尾）

真实接入要改的三处（代码注释均已标明）：① 引入 `wechatpay-java` SDK；
② 实现 `WechatPayProvider` 的 prepay 签名与 refund；③ 给 `notify` 加验签解密
（微信真实回调是 AES 加密 JSON，与现在的 `{orderNo}` 入参完全不同）。

⚠️ 上线阻断项不变：`wash.wx.appid/secret` 为空时所有用户共用 `dev-openid`，
**不配置的话线上数据全串**；`wash.pay.provider` 生产必须为 `wechat`。

---

## 十六、订单列表融合自助洗车（2026-10-18）

> 需求：自助洗车与无感洗车**逻辑隔离不变**，但订单要融成一个列表展示，用户不感知两类区别。

### 16.1 为什么放前端融合，而不是后端合并查询

`wash-selfwash` 不依赖 `wash-order`；若让订单域去聚合自助订单，就得让 `wash-order` 反向依赖
`wash-selfwash`（新增耦合，且订单域从此知道"自助洗车"的存在）。
融合纯是展示层的事，放前端即**零模块耦合**，隔离性完全保持。
类比：这与第十三节「事件解耦」是同一个思路——能不建立依赖就不建立。

### 16.2 做法

- `orders.vue` 的 PENDING/SERVING/DONE 三个 tab **同时拉两类订单**再合并，按下单时间倒序：
  - 无感：`GET /api/v1/orders?tab={PENDING|SERVING|DONE}`
  - 自助：`GET /api/v1/selfwash/orders?tab={WAIT_PAY,WAIT_USE|ONGOING|DONE,CANCELED}`（单值参数，多查几次）
- 融合项用 `bizType`（`UNSENSITIVE` / `SELFWASH`）分派：卡片渲染、点击跳各自详情页
  （`order-detail` vs `selfwash-detail`）。
- **排序键**：两类统一用「下单时间」。为此给 `OrderListItemVO` 增加了 `createTime`（毫秒）——
  mapper 本就查了 `create_time`，只是没往 VO 暴露。
  注意两边 createTime 形态不同：无感是毫秒数，自助是若依 Date 序列化的 `yyyy-MM-dd HH:mm:ss` 字符串，
  前端 `sortKey` 两种都兼容（字符串按本地时间解析，后端时区 Asia/Shanghai）。
- 无感订单保留原有操作按钮（PAY / DEPOSIT_KEY / TAKE_KEY / REVIEW / CANCEL…）；
  自助订单只显示「查看详情」，操作仍在其详情页内完成（与改版前 `selfwash-orders` 行为一致）。
- 翻页只翻无感侧（自助一次性全量返回），**追加后必须重新排序**，
  否则新一页的无感单会排在自助单后面，看起来像"加载失败"。

### 16.3 保留未动

- `pages/selfwash-orders/selfwash-orders.vue` 与「我的 → 我的自助洗车」入口保留：
  主动想看自助专项明细时仍可用，不影响主列表融合。
- 两类订单的表、状态机（`OrderStatus` 15 态 vs `SelfWashStatus` 7 态）、接口完全不变。

### 16.4 实测

无感列表返回 `createTime=1792243746000`（毫秒）；自助返回 `createTime=2026-10-17 23:24:25`（字符串），
两种形态均被 `sortKey` 正确处理。类型检查与 `build:mp-weixin` 通过。

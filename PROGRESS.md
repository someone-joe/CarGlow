# 项目进度交接（换设备 / 新会话先看这里）

> 用途：聊天记录不会跨设备，但本文件会。新会话开始时，让 AI 先读本文件即可无损接上。
> 最后更新：2026-09-17

---

## 一、当前进度（一句话）

**三步走全部完成**（状态机、契约、工程骨架）。**第四步进行中：登录 + 空订单列表最小链路——后端已完成并实测通过，前端待接。**
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
12. **下一步建议**（按价值排序）：
    - **真实微信支付 + 退款**：替换 mock-pay，并补真正的退款 API 与失败重试（上线前必须）
    - **存钥匙 + 柜机**：`DEPOSIT_KEY` 事件与格口预占，需柜机/格口模块开工（PRD 核心链路，依赖硬件协议）
    - **后台首页数据看板**：今日单量、在洗数、异常数（经营决策要看）

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

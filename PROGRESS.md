# 项目进度交接（换设备 / 新会话先看这里）

> 用途：聊天记录不会跨设备，但本文件会。新会话开始时，让 AI 先读本文件即可无损接上。
> 最后更新：2026-09-17

---

## 一、当前进度（一句话）

**三步走：前两步已完成**（订单状态机、API 契约），**第三步（工程骨架）后端已跑通，前端未开始**。
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
| MySQL | ✅ 8.0.39 | `127.0.0.1:3306`，库 `carwash`，root / carglow_dev |
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
3. **建前端骨架**（进行中）：uni-app 三端，引入已生成的 `schema.d.ts`
   - 已完成：官方 `vite-ts` 模板合并进 `carwash-mp`（uni-app 3.0.0 + Vue 3.4 + Vite 5.2.8 + TS 4.9），`src/api/schema.d.ts` 已保留
   - 待执行：`cd carwash-mp && npm install --registry=https://registry.npmmirror.com && npm run dev:mp-weixin`
   - 产物目录 `dist/dev/mp-weixin`，用微信开发者工具导入；`src/manifest.json` 里需填你自己的小程序 appid
4. **建表**（MySQL/Redis 容器已起）：状态机日志表 DDL 见《订单状态机规则表.md》第四节
5. **跑通最小链路**：登录 → 空订单列表（微信开发者工具里看到"暂无订单"）
6. **补测试**：17 条合法流转全跑通 + 1 条非法流转被拦截

---

## 七、还没做的事（别以为做完了）

- **契约只覆盖 C 端**：取送端、作业端、柜机回调、后台管理四块接口未定，已列在 `openapi.yaml` 末尾的 `x-roadmap`，对应模块开工前必须先补契约
- **格口状态机、任务状态机未做**（PRD 4.4 / 4.5 已定义规则）
- ~~**状态机单元测试**~~ ✅ 已补 15 个用例（2026-09-17，全绿）
  - **测试抓到一个真 bug 并已修复**：`FINISHED` 被标记为终态，`fire()` / `canFire()` 用 `isTerminal()` 一刀切拦截，
    导致你确认过的「已完成仍可退款」在代码里走不通。已改为**以流转规则表为准**拦截，
    真正无出边的只有 `REFUNDED`（《订单状态机规则表.md》已同步说明）
- **前端 uni-app 工程未开始**（`carwash-mp` 目前只有 `src/api/schema.d.ts`）
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

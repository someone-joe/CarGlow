# CarGlow（夜间代客洗车平台）

> 详细进度与交接看 [`PROGRESS.md`](./PROGRESS.md)；AI 协作红线看 [`CODEBUDDY.md`](./CODEBUDDY.md)。

## 目录地图（monorepo，三端独立）

| 目录 | 是什么 | 技术栈 | 端口 |
| --- | --- | --- | --- |
| `carwash-mp` | C 端 / 取送端 / 作业端小程序 | uni-app + Vue3 + TS | — |
| `carwash-admin` | 运营后台前端（**尚未建**） | RuoYi-Vue3 + Element Plus | 1024 |
| `carwash-server` | 后端（若依 3.9.2 + 业务模块 `ruoyi-wash`） | Spring Boot 4.1 + JDK 17 | 8080 |
| `docker` | 本地 MySQL 8 / Redis 7 | Docker Compose | 3306 / 6379 |
| `openapi.yaml` | **前后端接口契约（唯一真源）** | OpenAPI | — |

## 本机启动顺序

```bash
# 1. 起中间件（MySQL 8.0.39 + Redis 7.2，数据在 Docker 卷里，停止不丢）
docker compose -f docker/docker-compose.dev.yml up -d

# 2. 起后端（需先打包，见下）
java -jar carwash-server/ruoyi-admin/target/ruoyi-admin.jar

# 3. 起小程序（微信开发者工具导入 dist/build/mp-weixin）
cd carwash-mp && npm run dev:mp-weixin
```

## 常用命令

```bash
# 后端打包（用 Docker 里的 Maven，本机不装 Maven）
docker run --rm -v d:/CarGlow/carwash-server:/app -v carglow-m2:/root/.m2/repository \
  -w /app maven:3.9-eclipse-temurin-17 mvn -B -DskipTests clean package

# 后端单元测试
docker run --rm -v d:/CarGlow/carwash-server:/app -v carglow-m2:/root/.m2/repository \
  -w /app maven:3.9-eclipse-temurin-17 mvn -B -pl :wash-common -am test

# 小程序：安装依赖 / 构建
cd carwash-mp && npm install --registry=https://registry.npmmirror.com
npm run build:mp-weixin     # 构建产物 dist/build/mp-weixin

# 导入业务表 / 种子数据（必须带 --default-character-set=utf8mb4，
# 否则中文会被双重编码：库里看着"正常"，API 返回全是乱码）
docker cp carwash-server/sql/wash_seed_dev.sql carglow-mysql:/tmp/seed.sql
docker exec carglow-mysql sh -c "mysql -uroot -pcarglow_dev --default-character-set=utf8mb4 carwash < /tmp/seed.sql"
```

## 本地连接信息（仅开发用）

| 服务 | 地址 | 账号 |
| --- | --- | --- |
| MySQL | `127.0.0.1:3306`，库 `carwash` | root / carglow_dev |
| Redis | `127.0.0.1:6379` | 密码 carglow_dev |
| 后端 | `http://localhost:8080` | 若依默认 admin / admin123 |

## 一条最重要的纪律

**新增或修改接口，必须先改 `openapi.yaml`，再改代码。**
前端 TS 类型由契约生成，后端不得另立字段名，前端不得臆造字段。

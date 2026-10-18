#!/usr/bin/env bash
# CarGlow 本地全套开发服务一键启动（幂等，可重复执行）
#
# 启动内容:
#   1. Docker 运行时 (colima，Mac 上无 Docker Desktop 时用)
#   2. MySQL / Redis 容器 (carglow-mysql / carglow-redis)
#   3. 后端 ruoyi-admin.jar  (含服务详情 + 站点修复，端口 8080)
#   4. 后台前端 dev server   (carwash-admin，端口见启动日志，默认 1024)
#
# 用法:
#   bash scripts/start-dev.sh            # 已运行的组件自动跳过
#   bash scripts/start-dev.sh restart    # 强制重启后端 + 后台前端（吃最新代码）
#
# 说明:
#   - 后端日志走 /tmp/logback-mac.xml（写到 /tmp/ruoyi-logs/，避开 macOS 上
#     不存在的 /home/ruoyi/logs 导致启动崩溃）；该文件由本脚本按需生成。
#   - jar 不存在时会自动 mvn package 构建（首次较慢）。
#   - 小程序端(uni-app)不在此脚本内，需单独:
#       cd carwash-mp && npm run dev:mp-weixin
#     再用微信开发者工具打开 dist/dev/mp-weixin。

set -u

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
START_LOG=/tmp/carglow-start.log
: > "$START_LOG"

log() { echo "[$(date +%H:%M:%S)] $*"; }

RESTART=0
[ "${1:-}" = "restart" ] && RESTART=1

# ---------- 0) 可选：强制重启时先清掉旧进程 ----------
if [ "$RESTART" = 1 ]; then
  log "== restart 模式：先停止旧的后端/前端 =="
  lsof -nP -iTCP:8080 -sTCP:LISTEN -t 2>/dev/null | xargs -r kill -9
  lsof -nP -iTCP:1024 -sTCP:LISTEN -t 2>/dev/null | xargs -r kill -9
  # 兜底：按命令行特征杀 vite（仅本项目的 dev server）
  pkill -f "carwash-admin.*vite" 2>/dev/null
  sleep 2
fi

# ---------- 1) Docker 运行时 (colima) ----------
if ! docker info >/dev/null 2>&1; then
  log "colima 未运行，启动中..."
  colima start >>"$START_LOG" 2>&1
  for _ in $(seq 1 90); do docker info >/dev/null 2>&1 && break; sleep 1; done
fi
if docker info >/dev/null 2>&1; then
  log "✔ docker 就绪"
else
  log "✘ docker 仍不可用，请检查 colima / 手动 colima start"; exit 1
fi

# ---------- 2) MySQL / Redis 容器 ----------
for c in carglow-mysql carglow-redis; do
  if [ "$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null)" != "true" ]; then
    docker start "$c" >>"$START_LOG" 2>&1 && log "✔ 启动容器 $c" || log "✘ 容器 $c 启动失败"
  else
    log "✔ 容器 $c 已在运行"
  fi
done
for _ in $(seq 1 60); do
  docker exec carglow-mysql mysqladmin -uroot -pcarglow_dev ping >/dev/null 2>&1 && { log "✔ MySQL 就绪"; break; }
  sleep 1
done
docker exec carglow-redis redis-cli -a carglow_dev ping >/dev/null 2>&1 && log "✔ Redis 就绪"

# ---------- 3) 后端 logback 配置（macOS 友好）----------
LOGBACK=/tmp/logback-mac.xml
if [ ! -f "$LOGBACK" ]; then
  cat > "$LOGBACK" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATH" value="/tmp/ruoyi-logs"/>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern><charset>UTF-8</charset></encoder>
    </appender>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/sys-app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/sys-app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder><pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern><charset>UTF-8</charset></encoder>
    </appender>
    <root level="INFO"><appender-ref ref="CONSOLE"/><appender-ref ref="FILE"/></root>
    <logger name="com.ruoyi" level="DEBUG"/>
</configuration>
XML
  log "✔ 已生成 $LOGBACK"
fi

# ---------- 4) 后端 jar（不存在则构建）----------
JAR="$REPO_ROOT/carwash-server/ruoyi-admin/target/ruoyi-admin.jar"
if [ ! -f "$JAR" ] || [ "$RESTART" = "1" ]; then
  if [ "$RESTART" = "1" ]; then
    log "restart 模式：强制重新打包（吃最新代码），请稍候..."
  else
    log "jar 不存在，开始构建（首次较慢）..."
  fi
  ( cd "$REPO_ROOT/carwash-server" && mvn -B -pl ruoyi-admin -am -DskipTests package ) >>"$START_LOG" 2>&1 \
    && log "✔ 构建完成" || { log "✘ 构建失败，详见 $START_LOG"; exit 1; }
fi

BACK_CODE=$(curl -s -o /dev/null -w '%{http_code}' localhost:8080/captchaImage 2>/dev/null)
if [ "$BACK_CODE" = "200" ] && [ "$RESTART" != "1" ]; then
  log "✔ 后端已在 8080 运行"
else
  mkdir -p /tmp/ruoyi-logs
  rm -f /tmp/ruoyi-admin.log
  nohup java -Dlogback.configurationFile="$LOGBACK" -jar "$JAR" --logging.config="file:$LOGBACK" >/tmp/ruoyi-admin.log 2>&1 &
  log "启动后端 pid $!"
  for _ in $(seq 1 60); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' localhost:8080/captchaImage 2>/dev/null)" = "200" ] && { log "✔ 后端就绪"; break; }
    sleep 2
  done
fi

# ---------- 5) 后台前端 dev server ----------
FRONT_CODE=$(curl -s -o /dev/null -w '%{http_code}' localhost:1024/ 2>/dev/null)
if [ "$FRONT_CODE" = "200" ] && [ "$RESTART" != "1" ]; then
  log "✔ 后台前端已在 1024 运行"
else
  rm -f /tmp/admin-dev.log
  ( cd "$REPO_ROOT/carwash-admin" && nohup npm run dev >/tmp/admin-dev.log 2>&1 & )
  log "启动后台前端(dev)..."
  ADMIN_PORT=""
  for _ in $(seq 1 40); do
    ADMIN_PORT=$(grep -oE 'localhost:[0-9]+' /tmp/admin-dev.log 2>/dev/null | head -1 | cut -d: -f2)
    [ -n "$ADMIN_PORT" ] && [ "$(curl -s -o /dev/null -w '%{http_code}' localhost:$ADMIN_PORT/ 2>/dev/null)" = "200" ] && { log "✔ 后台前端就绪 (端口 $ADMIN_PORT)"; break; }
    sleep 2
  done
  [ -z "$ADMIN_PORT" ] && ADMIN_PORT=1024
fi

# ---------- 汇总 ----------
ADMIN_PORT=${ADMIN_PORT:-1024}
log "=============================================="
log "✔ 全部就绪"
log "   后台管理:  http://localhost:${ADMIN_PORT}"
log "   后端 API:  http://localhost:8080   (验证码本地已关, admin/admin123 直登)"
log "   小程序端:  cd carwash-mp && npm run dev:mp-weixin  -> 微信开发者工具打开 dist/dev/mp-weixin"
log "   启动明细:  $START_LOG"

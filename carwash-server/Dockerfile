# 后端运行时镜像：JDK17 + 预构建的 ruoyi-admin.jar
# 构建前需先 mvn package 生成 ruoyi-admin/target/ruoyi-admin.jar
# 用法（在 carwash-server/ 目录）：docker build -t carglow/server:<tag> .
FROM eclipse-temurin:17-jre

ENV TZ=Asia/Shanghai
WORKDIR /app

# 构建产物由 scripts/deploy.sh 先 mvn package 生成，再 COPY 进镜像
COPY ruoyi-admin/target/ruoyi-admin.jar app.jar

# 2C4G 与 mysql/redis 共存：堆上限 1G、G1GC、OOM 直接退出便于重启；容器内按 cgroup 感知内存上限
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError -Duser.timezone=GMT+8"

# 媒体与日志落到挂载卷，宿主持久化（容器重建不丢）
VOLUME ["/app/upload/media", "/app/logs"]

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

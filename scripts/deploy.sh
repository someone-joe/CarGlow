#!/usr/bin/env bash
# CarGlow 部署：构建一次镜像 → 上 test 验证 → 同 tag 上 prod（先备份库）。
# 在服务器上执行（例：ssh carglow-prod 'bash ~/CarGlow/scripts/deploy.sh test 1.0.0'）。
#   build   <tag>   mvn package + docker build 出 carglow/server:<tag>
#   test    <tag>   部署/更新 test 栈（项目 carglow-test）
#   prod    <tag>   备份生产库 + 部署/更新 prod 栈（项目 carglow-prod）
#   promote <tag>   build + 上 test；验证通过后再手动执行 prod <tag>
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
SERVER_DIR="$REPO/carwash-server"
DOCKER_DIR="$REPO/docker"
cmd="${1:-}"; tag="${2:-$(date +%Y%m%d-%H%M%S)}"

build_image() {
  echo ">> 构建 jar + 镜像 carglow/server:$tag"
  (cd "$SERVER_DIR" && mvn -B -pl ruoyi-admin -am -DskipTests package -q)
  docker build -t "carglow/server:$tag" "$SERVER_DIR"
}

deploy_env() { # $1 = test|prod
  local env="$1"
  IMAGE_TAG="$tag" docker compose -p "carglow-$env" --env-file "$DOCKER_DIR/.env.$env" -f "$DOCKER_DIR/docker-compose.yml" up -d
  echo ">> $env 栈已更新到 $tag："
  docker compose -p "carglow-$env" ps
}

backup_prod_db() {
  local out="/data/backups/carwash-prod-$(date +%Y%m%d-%H%M%S).sql"
  mkdir -p /data/backups
  # 容器名 = <项目名>_<服务名>_1，即 carglow-prod-mysql-1
  docker exec carglow-prod-mysql-1 sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" carwash' > "$out"
  echo ">> 生产库已备份：$out"
}

case "$cmd" in
  build)   build_image ;;
  test)    deploy_env test ;;
  prod)    backup_prod_db; deploy_env prod ;;
  promote) build_image; deploy_env test; echo ">> test 已上 $tag，验证通过后执行：$0 prod $tag" ;;
  *) echo "usage: $0 {build|test|prod|promote} [tag]"; exit 1 ;;
esac

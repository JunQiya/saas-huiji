#!/usr/bin/env bash
# =============================================================================
# 星河·会记 SaaS — 生产部署脚本(在服务器上以 root 或 sudo 执行一次)
#
# 用途: 1) 创建数据库与专用账号(最小权限)  2) 导入本地迁移数据(已脱敏)
#       3) 注入生产环境变量  4) 以后台方式启动后端 jar
#
# 前提:
#   - 已安装 MySQL 8.x 与 JDK 17
#   - 本目录下存在 huiji-db.sql(本地导出, 已排除 wx_account 密钥) 与 huiji-server-1.0.0.jar
#   - 先按需修改下方变量: DB_HOST(默认本机) / MYSQL_ROOT(本地 root 密码) / H5_DOMAIN
# =============================================================================
set -euo pipefail

APP_NAME="huiji-server"
APP_DIR="$(cd "$(dirname "$0")" && pwd)"

# ---- 配置区(按实际修改) ----
MYSQL_ROOT_PWD="${MYSQL_ROOT_PWD:-}"          # 服务器 MySQL root 密码(仅安装时用一次)
DB_HOST="127.0.0.1"
DB_PORT="3306"
DB_NAME="huiji"
DB_USER="huiji_app"                           # 专用业务账号, 不用 root
DB_PASS="$(openssl rand -base64 18 | tr -d '\n')"   # 若已有 DB_PASS 环境变量则沿用
JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')" # 若已有 JWT_SECRET 则沿用
H5_DOMAIN="${H5_DOMAIN:-http://h5.your-domain.com}"  # 改成真实 H5 域名
JAR="${APP_DIR}/${APP_NAME}-1.0.0.jar"

echo "==> [1/5] 校验依赖"
command -v mysql >/dev/null || { echo "缺少 mysql 客户端"; exit 1; }
command -v "$JAVA_HOME/bin/java" >/dev/null 2>&1 || command -v java >/dev/null || { echo "缺少 JDK17"; exit 1; }
[ -f "$JAR" ] || { echo "缺少 jar: $JAR"; exit 1; }
[ -f "${APP_DIR}/huiji-db.sql" ] || { echo "缺少数据文件 huiji-db.sql"; exit 1; }

echo "==> [2/5] 创建数据库与专用账号(最小权限)"
[ -n "$MYSQL_ROOT_PWD" ] || { echo "请先 export MYSQL_ROOT_PWD=<服务器MySQL root密码> 再运行"; exit 1; }
mysql -uroot -p"${MYSQL_ROOT_PWD}" -h"$DB_HOST" <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'${DB_HOST}' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'${DB_HOST}' IDENTIFIED BY '${DB_PASS}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP, TRIGGER ON \`${DB_NAME}\`.* TO '${DB_USER}'@'${DB_HOST}';
FLUSH PRIVILEGES;
SQL
echo "    已创建账号 ${DB_USER}@${DB_HOST}(权限仅限 ${DB_NAME} 库)"

echo "==> [3/5] 导入迁移数据"
mysql -uroot -p"${MYSQL_ROOT_PWD}" -h"$DB_HOST" "${DB_NAME}" < "${APP_DIR}/huiji-db.sql"
echo "    导入完成"

echo "==> [4/5] 写入环境变量文件(.env, chmod 600)"
cat > "${APP_DIR}/.env" <<ENV
DB_URL=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=true&requireSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
DB_USERNAME=${DB_USER}
DB_PASSWORD=${DB_PASS}
JWT_SECRET=${JWT_SECRET}
H5_DOMAIN=${H5_DOMAIN}
CORS_ORIGINS=
# 演示系统保持 true(未接短信网关也可登录, 前端自动填码); 真实生产改 false 并接短信
SMS_DEV_MODE=true
SMS_FIXED_CODE=
ENV
chmod 600 "${APP_DIR}/.env"
echo "    已写入 ${APP_DIR}/.env"

echo "==> [5/5] 启动后端(prod)"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
set -a; source "${APP_DIR}/.env"; set +a
nohup "${JAVA_BIN}" -jar "${JAR}" --spring.profiles.active=prod \
  --server.port=8081 > "${APP_DIR}/app.log" 2>&1 &
echo $! > "${APP_DIR}/app.pid"
echo "    已启动, PID=$(cat ${APP_DIR}/app.pid), 日志: ${APP_DIR}/app.log"

echo
echo "✅ 部署完成。健康检查: curl -s http://localhost:8081/api/h5/stores"
echo "⚠️  请立即: 1) 删除本脚本内的 MYSQL_ROOT_PWD 历史 2) 修改默认管理员密码(admin/123456) 3) 在后台重新配置微信支付/公众号凭据"

#!/usr/bin/env bash
set -euo pipefail

ENV_FILE=/opt/linkedknowledge/.env
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

if ! grep -q "^SPRING_DATASOURCE_URL=" "$ENV_FILE"; then
  {
    echo "SPRING_PROFILES_ACTIVE=prod"
    echo "SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/linkedknowledge?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
    echo "SPRING_DATASOURCE_USERNAME=lk"
    echo "SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD}"
    echo "SPRING_DATA_REDIS_HOST=127.0.0.1"
  } >> "$ENV_FILE"
fi
chmod 600 "$ENV_FILE"

cat >/etc/mysql/mysql.conf.d/zz-lowmem.cnf <<'EOF'
[mysqld]
innodb_buffer_pool_size=128M
performance_schema=OFF
bind-address=127.0.0.1
EOF

systemctl restart mysql
sleep 2

mysql --protocol=socket <<SQL
CREATE DATABASE IF NOT EXISTS linkedknowledge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lk'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
CREATE USER IF NOT EXISTS 'lk'@'127.0.0.1' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
ALTER USER 'lk'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
ALTER USER 'lk'@'127.0.0.1' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
GRANT ALL PRIVILEGES ON linkedknowledge.* TO 'lk'@'localhost';
GRANT ALL PRIVILEGES ON linkedknowledge.* TO 'lk'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

ln -sfn /etc/nginx/sites-available/linkedknowledge /etc/nginx/sites-enabled/linkedknowledge
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

systemctl daemon-reload
systemctl enable linkedknowledge
systemctl restart linkedknowledge
sleep 10
systemctl --no-pager --full status linkedknowledge || true
curl -sS -m 8 http://127.0.0.1:8080/api/health || true
echo
curl -sS -m 8 -o /dev/null -w "nginx:%{http_code}\n" http://127.0.0.1/ || true

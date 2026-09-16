#!/usr/bin/env bash
set -euo pipefail
sed -i 's/\r$//' /opt/linkedknowledge/.env
set -a
# shellcheck disable=SC1091
. /opt/linkedknowledge/.env
set +a
sed -i '/^SPRING_DATASOURCE_PASSWORD=/d' /opt/linkedknowledge/.env
printf 'SPRING_DATASOURCE_PASSWORD=%s\n' "$MYSQL_ROOT_PASSWORD" >> /opt/linkedknowledge/.env
sed -i 's/\r$//' /opt/linkedknowledge/.env
set -a
# shellcheck disable=SC1091
. /opt/linkedknowledge/.env
set +a
mysql --protocol=socket -e "DROP USER IF EXISTS 'lk'@'localhost'; DROP USER IF EXISTS 'lk'@'127.0.0.1';"
mysql --protocol=socket -e "CREATE USER 'lk'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';"
mysql --protocol=socket -e "CREATE USER 'lk'@'127.0.0.1' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';"
mysql --protocol=socket -e "GRANT ALL PRIVILEGES ON linkedknowledge.* TO 'lk'@'localhost'; GRANT ALL PRIVILEGES ON linkedknowledge.* TO 'lk'@'127.0.0.1'; FLUSH PRIVILEGES;"
mysql --protocol=socket --user=lk --password="${MYSQL_ROOT_PASSWORD}" -e "SELECT 1 AS ok;" linkedknowledge
systemctl restart linkedknowledge
for i in 1 2 3 4 5 6 7 8 9 10; do
  if curl -fsS -m 3 http://127.0.0.1:8080/api/health; then
    echo
    echo APP_UP
    exit 0
  fi
  sleep 3
done
echo APP_NOT_UP
journalctl -u linkedknowledge -n 20 --no-pager
exit 1

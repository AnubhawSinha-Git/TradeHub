#!/usr/bin/env bash
set -uo pipefail
exec > >(tee "$HOME/native_mysql_setup2.log") 2>&1

CONF=/etc/mysql/mysql.conf.d/mysqld.cnf
MARKER="# TRADEHUB RECOVERY ACTIVE"

echo "==> [1/6] Adding recovery options (skip-grant-tables + skip-networking)"
sudo grep -q "$MARKER" "$CONF" || \
  sudo bash -c "printf '\n%s\n[mysqld]\nskip-grant-tables\nskip-networking\n' '$MARKER' >> '$CONF'"
sudo tail -8 "$CONF"

echo "==> [2/6] Restarting MySQL into recovery mode"
sudo systemctl stop mysql 2>/dev/null || true
sleep 2
sudo pkill -9 -f '[m]ysqld' 2>/dev/null || true
sleep 1
sudo systemctl start mysql || { echo "FAILED to start:"; sudo tail -30 /var/log/mysql/error.log; exit 1; }
sleep 3

READY=0
for i in $(seq 1 20); do
  if mysql -u root -e "SELECT 1" >/dev/null 2>&1; then READY=1; break; fi
  sleep 1
done
if [ "$READY" -ne 1 ]; then
  echo "FAILED: cannot connect without password. Error log tail:"
  sudo tail -30 /var/log/mysql/error.log
  exit 1
fi
echo "    recovery mode active"

echo "==> [3/6] Setting root password='root' and creating DB + app user"
cat > "$HOME/mysql_init.sql" <<'SQL'
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';
CREATE DATABASE IF NOT EXISTS tradehub_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tradehub_user'@'localhost' IDENTIFIED BY 'tradehub_password';
CREATE USER IF NOT EXISTS 'tradehub_user'@'127.0.0.1' IDENTIFIED BY 'tradehub_password';
GRANT ALL PRIVILEGES ON tradehub_db.* TO 'tradehub_user'@'localhost';
GRANT ALL PRIVILEGES ON tradehub_db.* TO 'tradehub_user'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
if ! mysql -u root < "$HOME/mysql_init.sql" 2>&1; then
  echo "STEP 3 FAILED - see error above"
  exit 1
fi
echo "    done"

echo "==> [4/6] Removing recovery config"
sudo sed -i "/^${MARKER}$/,+3d" "$CONF"
sudo tail -5 "$CONF"

echo "==> [5/6] Restarting in normal mode"
sudo systemctl stop mysql 2>/dev/null || true
sleep 2
sudo systemctl start mysql || { echo "normal start failed:"; sudo tail -30 /var/log/mysql/error.log; exit 1; }
sleep 3
echo "    normal mode active (listening on 3306)"

echo "==> [6/6] Verifying app-user login and removing old Docker MySQL"
mysql -h 127.0.0.1 -P 3306 -u tradehub_user -ptradehub_password tradehub_db \
  -e "SELECT 'NATIVE MYSQL OK' AS status;" 2>&1

if sudo docker ps -a --format '{{.Names}}' | grep -q '^tradehub-mysql$'; then
  sudo docker stop tradehub-mysql >/dev/null
  sudo docker rm tradehub-mysql >/dev/null
  echo "==> Docker MySQL container removed"
fi

echo ""
echo "DONE - native MySQL ready on port 3306. Full log: ~/native_mysql_setup2.log"
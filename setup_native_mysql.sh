#!/usr/bin/env bash
set -uo pipefail
exec > >(tee "$HOME/native_mysql_setup.log") 2>&1

CONF=/etc/mysql/mysql.conf.d/mysqld.cnf
MARKER="# TRADEHUB RECOVERY"

echo "==> [1/7] Diagnostics: mysql-related systemd units"
sudo systemctl list-units --all 2>/dev/null | grep -iE "mysql|maria" || echo "    (none found)"

echo "==> [2/7] Stopping all mysql units and killing strays"
for u in mysql mysqld mariadb; do
  sudo systemctl stop "$u" 2>/dev/null || true
done
sleep 2
for i in 1 2 3 4 5 6 7 8 9 10; do
  PIDS=$(sudo pgrep -f mysqld 2>/dev/null | tr '\n' ' ')
  if [ -z "${PIDS// /}" ]; then
    break
  fi
  sudo kill -9 $PIDS 2>/dev/null || true
  sleep 2
  sudo pkill -9 -f '[m]ysqld' 2>/dev/null || true
  sleep 1
done
REMAINING=$(sudo pgrep -a -f mysqld 2>/dev/null || true)
if [ -n "$REMAINING" ]; then
  echo "WARNING - processes refuse to die (you may need to stop a systemd unit):"
  echo "$REMAINING"
  echo "Run: systemctl list-units | grep -i mysql"
fi
sudo rm -f /var/run/mysqld/mysqld.sock

echo "==> [3/7] Ensuring recovery mode config is present"
sudo mkdir -p /var/run/mysqld && sudo chown mysql:mysql /var/run/mysqld
sudo grep -q "$MARKER" "$CONF" || \
  sudo bash -c "printf '\n%s\n[mysqld]\nskip-grant-tables\nskip-networking\n' '$MARKER' >> '$CONF'"
sudo tail -6 "$CONF"

echo "==> [4/7] Starting MySQL (recovery mode - no password needed)"
sudo systemctl start mysql || { echo "FAILED to start:"; sudo tail -40 /var/log/mysql/error.log; exit 1; }
sleep 3
READY=0
for i in $(seq 1 20); do
  if mysql -u root -e "SELECT 1" >/dev/null 2>&1; then READY=1; break; fi
  sleep 1
done
if [ "$READY" -ne 1 ]; then
  echo "FAILED: cannot connect. Error log tail:"
  sudo tail -40 /var/log/mysql/error.log
  exit 1
fi
echo "    recovery mode active - connected without password"

echo "==> [5/7] Setting root password='root' and creating DB + app user"
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
  echo "STEP 5 FAILED - see error above"
  exit 1
fi
echo "    done - root password is 'root', tradehub_db + tradehub_user created"

echo "==> [6/7] Removing recovery config and restarting in normal mode"
sudo sed -i "/^${MARKER}$/,+3d" "$CONF"
sudo systemctl stop mysql 2>/dev/null || true
for i in 1 2 3; do sudo pkill -9 -f '[m]ysqld' 2>/dev/null || true; sleep 1; done
sudo systemctl start mysql || { echo "FAILED normal start:"; sudo tail -40 /var/log/mysql/error.log; exit 1; }
sleep 3
echo "    normal mode active (mysql listens on 3306)"

echo "==> [7/7] Verifying the app-user login and removing old Docker MySQL"
mysql -h 127.0.0.1 -P 3306 -u tradehub_user -ptradehub_password tradehub_db \
  -e "SELECT 'NATIVE MYSQL OK' AS status;" 2>&1

if sudo docker ps -a --format '{{.Names}}' | grep -q '^tradehub-mysql$'; then
  sudo docker stop tradehub-mysql >/dev/null
  sudo docker rm tradehub-mysql >/dev/null
  echo "==> Docker MySQL container removed"
fi

echo ""
echo "DONE - native MySQL ready on port 3306. Full log: ~/native_mysql_setup.log"
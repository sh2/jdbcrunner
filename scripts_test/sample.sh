#!/bin/bash -x

# ドキュメント記載用の実行例を出力する

export CLASSPATH=../jdbcrunner-1.3.1.jar

# default 10
TEST_WARMUP=10
# default 60
TEST_MEASUREMENT=60
# default 60
SYSBENCH_WARMUP=60
# default 180
SYSBENCH_MEASUREMENT=180
# default 60
TPCB_WARMUP=60
# default 180
TPCB_MEASUREMENT=180
# default 300
TPCC_WARMUP=300
# default 900
TPCC_MEASUREMENT=900

cd $(dirname $0)
podman stop mysql
podman rm mysql
podman run --detach --rm --publish=3306:3306 \
    --volume=$PWD/mysql.conf.d:/etc/mysql/conf.d \
    --env=MYSQL_ROOT_PASSWORD=rootpass \
    --name=mysql docker.io/mysql:latest

while true; do
    podman exec mysql mysql -u root -prootpass -e 'SELECT 1'
    if [ $? == 0 ]; then
        break
    fi
    echo 'waiting...'
    sleep 5
done

echo Chapter 2

cat <<_EOF_ | podman exec -i mysql mysql -v -u root -prootpass
CREATE DATABASE tutorial;
CREATE USER runner@'%' IDENTIFIED BY 'change_on_install';
GRANT ALL PRIVILEGES ON tutorial.* TO runner@'%';
_EOF_

cat <<_EOF_ | podman exec -i mysql mysql -v -t -u runner -pchange_on_install tutorial
CREATE TABLE sample (id INT PRIMARY KEY, data VARCHAR(10)) ENGINE = InnoDB;
INSERT INTO sample (id, data) VALUES (1, 'aaaaaaaaaa');
INSERT INTO sample (id, data) VALUES (2, 'bbbbbbbbbb');
INSERT INTO sample (id, data) VALUES (3, 'cccccccccc');
INSERT INTO sample (id, data) VALUES (4, 'dddddddddd');
INSERT INTO sample (id, data) VALUES (5, 'eeeeeeeeee');
SELECT * FROM sample ORDER BY id;
_EOF_

java JR

cat <<_EOF_ > test.js
function run() {
    var param = random(1, 5);
    query("SELECT data FROM sample WHERE id = \$int", param);
}
_EOF_

rm -rf logs_sample02
java JR test.js \
    -jdbcUrl jdbc:mysql://localhost/tutorial \
    -jdbcUser runner -jdbcPass change_on_install -logDir logs_sample02 \
    -warmupTime $TEST_WARMUP -measurementTime $TEST_MEASUREMENT
ls -l logs_sample02
cat logs_sample02/log_*_r.csv
cat logs_sample02/log_*_t.csv

echo Chapter 8

cat <<_EOF_ | podman exec -i mysql mysql -v -u root -prootpass
CREATE DATABASE sbtest;
CREATE USER sbtest@'%' IDENTIFIED BY 'sbtest';
GRANT ALL PRIVILEGES ON sbtest.* TO sbtest@'%';
_EOF_

rm -rf logs_sample08
java JR ../scripts/sysbench_load.js -logDir logs_sample08
java JR ../scripts/sysbench.js -logDir logs_sample08 \
    -warmupTime $SYSBENCH_WARMUP -measurementTime $SYSBENCH_MEASUREMENT

echo Chapter 9

cat <<_EOF_ | podman exec -i mysql mysql -v -u root -prootpass
CREATE DATABASE tpcb;
CREATE USER tpcb@'%' IDENTIFIED BY 'tpcb';
GRANT ALL PRIVILEGES ON tpcb.* TO tpcb@'%';
_EOF_

rm -rf logs_sample09
java JR ../scripts/tpcb_load.js -logDir logs_sample09
java JR ../scripts/tpcb.js -logDir logs_sample09 \
    -warmupTime $TPCB_WARMUP -measurementTime $TPCB_MEASUREMENT

echo Chapter 10

cat <<_EOF_ | podman exec -i mysql mysql -v -u root -prootpass
CREATE DATABASE tpcc;
CREATE USER tpcc@'%' IDENTIFIED BY 'tpcc';
GRANT ALL PRIVILEGES ON tpcc.* TO tpcc@'%';
_EOF_

rm -rf logs_sample10
java JR ../scripts/tpcc_load.js -logDir logs_sample10
java JR ../scripts/tpcc.js -logDir logs_sample10 \
    -warmupTime $TPCC_WARMUP -measurementTime $TPCC_MEASUREMENT

podman stop mysql

#!/bin/bash -x

export CLASSPATH=../jdbcrunner-1.3.1.jar

cd $(dirname $0)
podman stop --time 60 mysql
podman rm mysql
podman run --detach --rm --publish=3306:3306 \
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

cat <<_EOF_ | podman exec -i mysql mysql -u root -prootpass
CREATE DATABASE test;
CREATE USER test@'%' IDENTIFIED BY 'test';
GRANT ALL PRIVILEGES ON test.* TO test@'%';
CREATE DATABASE sbtest;
CREATE USER sbtest@'%' IDENTIFIED BY 'sbtest';
GRANT ALL PRIVILEGES ON sbtest.* TO sbtest@'%';
CREATE DATABASE tpcb;
CREATE USER tpcb@'%' IDENTIFIED BY 'tpcb';
GRANT ALL PRIVILEGES ON tpcb.* TO tpcb@'%';
CREATE DATABASE tpcc;
CREATE USER tpcc@'%' IDENTIFIED BY 'tpcc';
GRANT ALL PRIVILEGES ON tpcc.* TO tpcc@'%';
_EOF_

rm -rf logs_mysql
java JR ../scripts/sample01_ja.js \
    -jdbcUser test -jdbcPass test \
    -warmupTime 5 -measurementTime 10 -logDir logs_mysql
java JR ../scripts/sysbench_load.js \
    -logDir logs_mysql
java JR ../scripts/sysbench.js \
    -warmupTime 5 -measurementTime 10 -logDir logs_mysql
java JR ../scripts/tpcb_load.js \
    -param0 4 -logDir logs_mysql
java JR ../scripts/tpcb.js \
    -warmupTime 5 -measurementTime 10 -logDir logs_mysql
java JR ../scripts/tpcc_load.js \
    -param0 4 -logDir logs_mysql
java JR ../scripts/tpcc.js \
    -warmupTime 5 -measurementTime 10 -logDir logs_mysql

podman stop --time 60 mysql

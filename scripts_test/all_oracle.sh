#!/bin/bash -x

export CLASSPATH=../jdbcrunner-1.3.1.jar:../ojdbc11.jar

cd $(dirname $0)
podman stop --time 60 oracle
podman rm oracle
podman run --detach --rm --publish=1521:1521 \
    --env=ORACLE_PWD=rootpass --env=ORACLE_CHARACTERSET=AL32UTF8 \
    --name=oracle container-registry.oracle.com/database/express:latest

while true; do
    echo 'SELECT status FROM v$instance;' \
        | podman exec -i oracle sqlplus sys/rootpass@XEPDB1 AS SYSDBA \
        | grep OPEN
    if [ $? == 0 ]; then
        break
    fi
    echo 'waiting...'
    sleep 5
done

ORACLE_HOME=$(podman exec oracle bash -c 'echo $ORACLE_HOME')
podman cp oracle:$ORACLE_HOME/rdbms/admin/utlsampl.sql .
patch -u utlsampl.sql < utlsampl.patch
podman exec -i oracle sqlplus sys/rootpass@XEPDB1 AS SYSDBA < utlsampl.sql

cat <<_EOF_ | podman exec -i oracle sqlplus sys/rootpass@XEPDB1 AS SYSDBA
CREATE USER sbtest IDENTIFIED BY sbtest;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER, UNLIMITED TABLESPACE TO sbtest;
CREATE USER tpcb IDENTIFIED BY tpcb;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER, UNLIMITED TABLESPACE TO tpcb;
CREATE USER tpcc IDENTIFIED BY tpcc;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER, UNLIMITED TABLESPACE TO tpcc;
_EOF_

rm -rf logs_oracle
java -Doracle.net.disableOob=true JR ../scripts/sample02_ja.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 \
    -jdbcUser scott -jdbcPass tiger \
    -warmupTime 5 -measurementTime 10 -logDir logs_oracle
java -Doracle.net.disableOob=true JR ../scripts/sysbench_load.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 \
    -logDir logs_oracle
java -Doracle.net.disableOob=true JR ../scripts/sysbench.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 \
    -warmupTime 5 -measurementTime 10 -logDir logs_oracle
java -Doracle.net.disableOob=true JR ../scripts/tpcb_load.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 \
    -param0 4 -logDir logs_oracle
java -Doracle.net.disableOob=true JR ../scripts/tpcb.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 \
    -warmupTime 5 -measurementTime 10 -logDir logs_oracle
java -Doracle.net.disableOob=true JR ../scripts/tpcc_load.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 \
    -param0 4 -logDir logs_oracle
java -Doracle.net.disableOob=true JR ../scripts/tpcc.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 \
    -warmupTime 5 -measurementTime 10 -logDir logs_oracle

podman stop --time 60 oracle

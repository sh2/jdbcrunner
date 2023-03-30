#!/bin/bash -x

export CLASSPATH=jdbcrunner-1.3.jar:ojdbc11.jar:.

podman stop oracle
podman rm oracle
podman run --detach --rm --publish=1521:1521 --env=ORACLE_PWD=rootpass --env=ORACLE_CHARACTERSET=AL32UTF8 --name=oracle container-registry.oracle.com/database/express:latest

while true; do
    echo 'SELECT status FROM v$instance;' | podman exec -i oracle sqlplus sys/rootpass@XEPDB1 AS SYSDBA | grep OPEN
    if [ $? == 0 ]; then
        break
    fi
    echo 'waiting...'
    sleep 5
done

podman exec -i oracle sqlplus sys/rootpass@XEPDB1 AS SYSDBA < utlsampl_fix.sql

cat <<_EOF_ | podman exec -i oracle sqlplus sys/rootpass@XEPDB1 AS SYSDBA
CREATE USER sbtest IDENTIFIED BY sbtest;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER, UNLIMITED TABLESPACE TO sbtest;
CREATE USER tpcb IDENTIFIED BY tpcb;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER, UNLIMITED TABLESPACE TO tpcb;
CREATE USER tpcc IDENTIFIED BY tpcc;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER, UNLIMITED TABLESPACE TO tpcc;
_EOF_

java -Doracle.net.disableOob=true JR sample02_ja.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 -jdbcUser scott -jdbcPass tiger \
    -warmupTime 5 -measurementTime 10 -logDir logs
java -Doracle.net.disableOob=true JR sysbench_load.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1
java -Doracle.net.disableOob=true JR sysbench.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 -warmupTime 5 -measurementTime 10
java -Doracle.net.disableOob=true JR tpcb_load.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 -param0 4
java -Doracle.net.disableOob=true JR tpcb.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 -warmupTime 5 -measurementTime 10
java -Doracle.net.disableOob=true JR tpcc_load.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 -param0 4
java -Doracle.net.disableOob=true JR tpcc.js \
    -jdbcUrl jdbc:oracle:thin:@//localhost:1521/XEPDB1 -warmupTime 5 -measurementTime 10

podman stop oracle

#!/bin/bash -x

export CLASSPATH=jdbcrunner-1.3.jar:.

podman stop postgres
podman rm postgres
podman run --detach --rm --publish=5432:5432 --env=POSTGRES_PASSWORD=rootpass --name=postgres docker.io/postgres:latest

while true; do
    podman exec postgres psql -U postgres -c 'SELECT 1'
    if [ $? == 0 ]; then
        break
    fi
    echo 'waiting...'
    sleep 5
done

cat <<_EOF_ | podman exec -i postgres psql -U postgres
CREATE DATABASE sbtest TEMPLATE template0 ENCODING 'UTF-8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE USER sbtest PASSWORD 'sbtest';
CREATE DATABASE tpcb TEMPLATE template0 ENCODING 'UTF-8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE USER tpcb PASSWORD 'tpcb';
CREATE DATABASE tpcc TEMPLATE template0 ENCODING 'UTF-8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE USER tpcc PASSWORD 'tpcc';
_EOF_

podman exec -i postgres psql -U postgres sbtest -c 'CREATE SCHEMA AUTHORIZATION sbtest'
podman exec -i postgres psql -U postgres tpcb -c 'CREATE SCHEMA AUTHORIZATION tpcb'
podman exec -i postgres psql -U postgres tpcc -c 'CREATE SCHEMA AUTHORIZATION tpcc'

java JR sysbench_load.js -jdbcUrl jdbc:postgresql://localhost:5432/sbtest
java JR sysbench.js -jdbcUrl jdbc:postgresql://localhost:5432/sbtest -warmupTime 5 -measurementTime 10
java JR tpcb_load.js -jdbcUrl jdbc:postgresql://localhost:5432/tpcb -param0 4
java JR tpcb.js -jdbcUrl jdbc:postgresql://localhost:5432/tpcb -warmupTime 5 -measurementTime 10
java JR tpcc_load.js -jdbcUrl jdbc:postgresql://localhost:5432/tpcc -param0 4
java JR tpcc.js -jdbcUrl jdbc:postgresql://localhost:5432/tpcc -warmupTime 5 -measurementTime 10

podman stop postgres

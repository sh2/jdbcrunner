# JdbcRunner

JdbcRunner is a stress testing tool for various RDBMSs.
You can easily create a test scenario with JavaScript and run it in multi-threaded environments.
Additionaly, JdbcRunner bundles some test kits for Oracle Database, MySQL and PostgreSQL so you can use them for benchmarks.

- Tiny sysbench - Port of [sysbench](https://github.com/akopytov/sysbench) OLTP benchmark
- Tiny TPC-B - Simplified implementation of [TPC-B](http://www.tpc.org/tpcb/default.asp)
- Tiny TPC-C - Simplified implementation of [TPC-C](http://www.tpc.org/tpcc/default.asp)

Please see the documents "docs_ja/index.html".
Please see [the website](https://dbstudy.info/jdbcrunner/) for details and [GitHub](https://github.com/sh2/jdbcrunner) for the source code.

## Tutorial

Let's run the TPC-C benchmark on MySQL.
First, create a database and a user.

    shell> mysql -u root -p
    sql> CREATE DATABASE tpcc;
    sql> CREATE USER tpcc@'%' IDENTIFIED BY 'tpcc';
    sql> GRANT ALL PRIVILEGES ON tpcc.* TO tpcc@'%';

Next, load the initial data using the tpcc_load.js script.

    shell> export CLASSPATH=jdbcrunner-1.3.jar
    shell> java JR scripts/tpcc_load.js

    15:37:31 [INFO ] > JdbcRunner 1.3
    15:37:31 [INFO ] [Config]
    Program start time   : 20180815-153731
    Script filename      : scripts/tpcc_load.js
    JDBC driver          : -
    JDBC URL             : jdbc:mysql://localhost:3306/tpcc?useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true
    JDBC user            : tpcc
    Load mode            : true
    Number of agents     : 4
    Auto commit          : false
    Debug mode           : false
    Trace mode           : false
    Log directory        : logs
    Parameter 0          : 0
    Parameter 1          : 0
    Parameter 2          : 0
    Parameter 3          : 0
    Parameter 4          : 0
    Parameter 5          : 0
    Parameter 6          : 0
    Parameter 7          : 0
    Parameter 8          : 0
    Parameter 9          : 0
    15:37:32 [INFO ] Tiny TPC-C - data loader
    15:37:32 [INFO ] -param0  : Scale factor (default : 16)
    15:37:32 [INFO ] -nAgents : Parallel loading degree (default : 4)
    15:37:32 [INFO ] Scale factor            : 16
    15:37:32 [INFO ] Parallel loading degree : 4
    15:37:32 [INFO ] Dropping tables ...
    15:37:32 [INFO ] Creating tables ...
    15:37:33 [INFO ] Loading item ...
    15:37:34 [INFO ] item : 10000 / 100000
    15:37:34 [INFO ] item : 20000 / 100000
    15:37:35 [INFO ] item : 30000 / 100000
    ...
    15:43:40 [INFO ] [Agent 1] orders : 30000 / 30000
    15:43:40 [INFO ] [Agent 0] orders : 30000 / 30000
    15:43:40 [INFO ] [Agent 3] orders : 30000 / 30000
    15:43:40 [INFO ] Creating indexes ...
    15:43:46 [INFO ] Analyzing tables ...
    15:43:46 [INFO ] Completed.
    15:43:46 [INFO ] < JdbcRunner SUCCESS

Then run the benchmark with the tpcc.js script.

    shell> java JR scripts/tpcc.js

    15:45:31 [INFO ] > JdbcRunner 1.3
    15:45:31 [INFO ] [Config]
    Program start time   : 20180815-154531
    Script filename      : scripts/tpcc.js
    JDBC driver          : -
    JDBC URL             : jdbc:mysql://localhost:3306/tpcc?useSSL=false&allowPublicKeyRetrieval=true
    JDBC user            : tpcc
    Warmup time          : 300 sec
    Measurement time     : 900 sec
    Number of tx types   : 5
    Number of agents     : 16
    Connection pool size : 16
    Statement cache size : 40
    Auto commit          : false
    Sleep time           : 0,0,0,0,0 msec
    Throttle             : - tps (total)
    Debug mode           : false
    Trace mode           : false
    Log directory        : logs
    Parameter 0          : 0
    Parameter 1          : 0
    Parameter 2          : 0
    Parameter 3          : 0
    Parameter 4          : 0
    Parameter 5          : 0
    Parameter 6          : 0
    Parameter 7          : 0
    Parameter 8          : 0
    Parameter 9          : 0
    15:45:32 [INFO ] Tiny TPC-C
    15:45:33 [INFO ] Scale factor : 16
    15:45:33 [INFO ] tx0 : New-Order transaction
    15:45:33 [INFO ] tx1 : Payment transaction
    15:45:33 [INFO ] tx2 : Order-Status transaction
    15:45:33 [INFO ] tx3 : Delivery transaction
    15:45:33 [INFO ] tx4 : Stock-Level transaction
    15:45:34 [INFO ] [Warmup] -299 sec, 11,18,2,0,1 tps, (11,18,2,0,1 tx)
    15:45:35 [INFO ] [Warmup] -298 sec, 14,21,1,3,3 tps, (25,39,3,3,4 tx)
    15:45:36 [INFO ] [Warmup] -297 sec, 24,21,4,3,1 tps, (49,60,7,6,5 tx)
    ...
    16:05:31 [INFO ] [Progress] 898 sec, 36,41,5,7,5 tps, 41164,41156,4120,4116,4119 tx
    16:05:32 [INFO ] [Progress] 899 sec, 42,47,5,7,5 tps, 41206,41203,4125,4123,4124 tx
    16:05:33 [INFO ] [Progress] 900 sec, 42,58,3,4,5 tps, 41248,41261,4128,4127,4129 tx
    16:05:33 [INFO ] [Total tx count] 41248,41261,4128,4127,4129 tx
    16:05:33 [INFO ] [Throughput] 45.8,45.8,4.6,4.6,4.6 tps
    16:05:33 [INFO ] [Response time (minimum)] 23,20,5,167,9 msec
    16:05:33 [INFO ] [Response time (50%tile)] 212,63,26,429,36 msec
    16:05:33 [INFO ] [Response time (90%tile)] 329,107,43,609,92 msec
    16:05:33 [INFO ] [Response time (95%tile)] 369,140,51,684,114 msec
    16:05:33 [INFO ] [Response time (99%tile)] 461,268,73,843,158 msec
    16:05:33 [INFO ] [Response time (maximum)] 746,797,127,1237,411 msec
    16:05:33 [INFO ] < JdbcRunner SUCCESS

テストキット Tiny sysbench
==========================

sysbenchとは
------------

`sysbench <https://github.com/akopytov/sysbench>`_ はAlexey Kopytov氏によってメンテナンスされているオープンソースソフトウェアで、以下の6種類のテストを行うことができる総合的なベンチマークツールです。ライセンスはGPLv2です。

* a collection of OLTP-like database benchmarks
* a filesystem-level benchmark
* a simple CPU benchmark
* a memory access benchmark
* a thread-based scheduler benchmark
* a POSIX mutex benchmark

以下は `sysbench 0.4 <https://github.com/akopytov/sysbench/tree/0.4>`_ についての説明です。OLTPベンチマークで用いられるデータベースのER図を以下に示します。テーブルは一つだけで、ごく単純な作りとなっています。

.. image:: images/sysbench.png

OLTPベンチマークは以下の4種類のテストモードを備えています。

* simple : 主キーによる一意検索を行う
* complex : 主キーによる一意検索、範囲検索、集計処理など9種類のクエリを実行する
* nontrx : トランザクションを使わずに5種類のクエリを実行する
* sp : ユーザが用意したストアドプロシージャを実行する

complexモードで実行されるトランザクションの内容は以下のとおりです。主キーによる一意検索が10回、その他8種類のクエリは1回ずつ実行されます。この比率はカスタマイズ可能となっています。

.. code-block:: mysql

  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id = :1;
  SELECT c FROM sbtest WHERE id BETWEEN :1 AND :2;
  SELECT SUM(k) FROM sbtest WHERE id BETWEEN :1 AND :2;
  SELECT c FROM sbtest WHERE id BETWEEN :1 AND :2 ORDER BY c;
  SELECT DISTINCT c FROM sbtest WHERE id BETWEEN :1 AND :2 ORDER BY c;
  UPDATE sbtest SET k = k + 1 WHERE id = :1;
  UPDATE sbtest SET c = :1 WHERE id = :2;
  DELETE FROM sbtest WHERE id = :1;
  INSERT INTO sbtest (id, k, c, pad) VALUES (:1, :2, :3, :4);
  COMMIT;

sysbench 0.4のOLTPベンチマークはMySQLをターゲットとして開発されていますが、Oracle DatabaseとPostgreSQLにも対応しています。

Tiny sysbenchとは
-----------------

Tiny sysbenchは、sysbench 0.4のOLTPベンチマークのうちcomplexモードをJdbcRunner上に移植したものです。以下の二つのスクリプトから構成されています。

* scripts/sysbench_load.js : テストデータ生成用スクリプト
* scripts/sysbench.js : テスト用スクリプト

動作確認RDBMS
-------------

Tiny sysbenchは、以下のRDBMSで動作確認をしています。

* Oracle Database 21c
* MySQL 8.0
* PostgreSQL 15

テストの準備
------------

MySQLにおけるテストの準備手順を以下に示します。Oracle Database、PostgreSQLについてはscripts/sysbench_load.jsのコメントをご参照ください。

データベースの作成
^^^^^^^^^^^^^^^^^^

MySQLにrootユーザで接続し、sbtestデータベースを作成します。

.. code-block:: mysql

  shell> mysql -u root -p

  sql> CREATE DATABASE sbtest;
  Query OK, 1 row affected (0.00 sec)

ユーザの作成
^^^^^^^^^^^^

sbtestユーザを作成します。

.. code-block:: mysql

  sql> CREATE USER sbtest@'%' IDENTIFIED BY 'sbtest';
  Query OK, 0 rows affected (0.00 sec)

  sql> GRANT ALL PRIVILEGES ON sbtest.* TO sbtest@'%';
  Query OK, 0 rows affected (0.00 sec)

ネットワーク環境によっては、接続元ホストを制限したりsbtestをより安全なパスワードに変更することをおすすめします。

テストデータの生成
^^^^^^^^^^^^^^^^^^

scripts/sysbench_load.jsを用いてテストデータの生成を行います。このスクリプトは以下の処理を行っています。

* テーブルの削除
* テーブルの作成
* データロード
* インデックスの作成 (MySQLの主キーはデータロード前に作成)
* 統計情報の更新

.. code-block:: text

  shell> java JR ../scripts/sysbench_load.js -logDir logs_sample08
  13:12:27 [INFO ] > JdbcRunner 1.3.1
  13:12:27 [INFO ] [Config]
  Program start time   : 20230331-131227
  Script filename      : ../scripts/sysbench_load.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/sbtest?rewriteBatchedStatements=true
  JDBC user            : sbtest
  Load mode            : true
  Number of agents     : 1
  Auto commit          : false
  Debug mode           : false
  Trace mode           : false
  Log directory        : logs_sample08
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
  13:12:28 [INFO ] Tiny sysbench - data loader
  13:12:28 [INFO ] -param0 : Number of records (default : 10000)
  13:12:28 [INFO ] Number of records : 10000
  13:12:28 [INFO ] Dropping a table ...
  13:12:28 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'sbtest.sbtest'
  13:12:28 [INFO ] Creating a table ...
  13:12:28 [INFO ] Loading sbtest ...
  13:12:28 [INFO ] sbtest : 1000 / 10000
  13:12:28 [INFO ] sbtest : 2000 / 10000
  13:12:28 [INFO ] sbtest : 3000 / 10000
  13:12:28 [INFO ] sbtest : 4000 / 10000
  13:12:28 [INFO ] sbtest : 5000 / 10000
  13:12:28 [INFO ] sbtest : 6000 / 10000
  13:12:28 [INFO ] sbtest : 7000 / 10000
  13:12:28 [INFO ] sbtest : 8000 / 10000
  13:12:28 [INFO ] sbtest : 9000 / 10000
  13:12:28 [INFO ] sbtest : 10000 / 10000
  13:12:28 [INFO ] Creating an index ...
  13:12:28 [INFO ] Analyzing a table ...
  13:12:28 [INFO ] Completed.
  13:12:28 [INFO ] < JdbcRunner SUCCESS

「Unknown table 'sbtest'」という警告は、存在しないsbtestテーブルを削除しようとして出力されるものです。無視して構いません。

また、-param0を指定することによってsbtestテーブルにロードするレコード数を変更することが可能です。デフォルトは1万レコードとなっています。

.. code-block:: text

  shell> java JR ../scripts/sysbench_load.js -param0 50000

テストの実行
------------

scripts/sysbench.jsを用いてテストを実行します。以下の例ではlocalhostのRDBMSに対してテストを行っていますが、実際にはJdbcRunnerとRDBMSを異なるコンピューターに配置することをおすすめします。

.. code-block:: text

  shell> java JR ../scripts/sysbench.js -logDir logs_sample08 -warmupTime 60 -measurementTime 180
  13:12:29 [INFO ] > JdbcRunner 1.3.1
  13:12:29 [INFO ] [Config]
  Program start time   : 20230331-131229
  Script filename      : ../scripts/sysbench.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/sbtest
  JDBC user            : sbtest
  Warmup time          : 60 sec
  Measurement time     : 180 sec
  Number of tx types   : 1
  Number of agents     : 16
  Connection pool size : 16
  Statement cache size : 20
  Auto commit          : false
  Sleep time           : 0 msec
  Throttle             : - tps
  Debug mode           : false
  Trace mode           : false
  Log directory        : logs_sample08
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
  13:12:30 [INFO ] Tiny sysbench
  13:12:30 [INFO ] Number of records : 10000
  13:12:31 [INFO ] [Warmup] -59 sec, 99 tps, (99 tx)
  13:12:32 [INFO ] [Warmup] -58 sec, 152 tps, (251 tx)
  13:12:33 [INFO ] [Warmup] -57 sec, 180 tps, (431 tx)
  13:12:34 [INFO ] [Warmup] -56 sec, 207 tps, (638 tx)
  13:12:35 [INFO ] [Warmup] -55 sec, 231 tps, (869 tx)
  13:12:36 [INFO ] [Warmup] -54 sec, 273 tps, (1142 tx)
  13:12:37 [INFO ] [Warmup] -53 sec, 212 tps, (1354 tx)
  13:12:38 [INFO ] [Warmup] -52 sec, 220 tps, (1574 tx)
  13:12:39 [INFO ] [Warmup] -51 sec, 262 tps, (1836 tx)
  13:12:40 [INFO ] [Warmup] -50 sec, 258 tps, (2094 tx)
  13:12:41 [INFO ] [Warmup] -49 sec, 305 tps, (2399 tx)
  13:12:41 [WARN ] [Agent 4] Deadlock detected.
  13:12:42 [INFO ] [Warmup] -48 sec, 286 tps, (2685 tx)
  ...
  13:16:28 [INFO ] [Progress] 178 sec, 393 tps, 73397 tx
  13:16:29 [INFO ] [Progress] 179 sec, 405 tps, 73802 tx
  13:16:30 [INFO ] [Progress] 180 sec, 406 tps, 74208 tx
  13:16:30 [INFO ] [Total tx count] 74209 tx
  13:16:30 [INFO ] [Throughput] 412.3 tps
  13:16:30 [INFO ] [Response time (minimum)] 4 msec
  13:16:30 [INFO ] [Response time (50%tile)] 38 msec
  13:16:30 [INFO ] [Response time (90%tile)] 63 msec
  13:16:30 [INFO ] [Response time (95%tile)] 69 msec
  13:16:30 [INFO ] [Response time (99%tile)] 79 msec
  13:16:30 [INFO ] [Response time (maximum)] 141 msec
  13:16:30 [INFO ] < JdbcRunner SUCCESS

OLTPベンチマークのcomplexモードでは、デッドロックが発生することがあります。これはオリジナル版のsysbenchでも発生するものです。Tiny sysbenchはデッドロックが発生した場合、該当のトランザクションをロールバックして再度実行します。

テストのカスタマイズ
--------------------

Tiny sysbenchはスクリプトscripts/sysbench.jsの変数定義を修正することで、オリジナル版のsysbenchが持つ設定オプションをある程度再現することができます。変数はスクリプトのApplication settingsという箇所に定義されていますので、ここを修正してご利用ください。

.. code-block:: javascript

  // Application settings ----------------------------------------------

  var DIST_UNIFORM = 1;
  var DIST_GAUSSIAN = 2;
  var DIST_SPECIAL = 3;

  // Number of records in the test table
  var oltpTableSize;

  // Ratio of queries in a transaction
  var oltpPointSelects = 10;
  var oltpSimpleRanges = 1;
  var oltpSumRanges = 1;
  var oltpOrderRanges = 1;
  var oltpDistinctRanges = 1;
  var oltpIndexUpdates = 1;
  var oltpNonIndexUpdates = 1;

  // Read-only flag
  var oltpReadOnly = false;

  // Range size for range queries
  var oltpRangeSize = 100;

  // Parameters for random numbers distribution
  var oltpDistType = DIST_SPECIAL;
  var oltpDistIter = 12;
  var oltpDistPct = 1;
  var oltpDistRes = 75;

オリジナル版sysbenchとの対応表を以下に示します。

====================== =================== ====================================================================
sysbenchのオプション   sysbench.jsの変数   説明
====================== =================== ====================================================================
oltp-test-mode         (未対応)            テストモードを指定するオプションです
oltp-reconnect-mode    (未対応)            テスト中にデータベースに再接続する方式を指定するオプションです
oltp-sp-name           (未対応)            spモードで実行するストアドプロシージャを指定するオプションです
oltp-read-only         oltpReadOnly        SELECT文のみを実行するオプションです
oltp-skip-trx          (未対応)            BEGIN/COMMIT文をスキップするオプションです
oltp-range-size        oltpRangeSize       範囲検索クエリの検索範囲を指定するオプションです
oltp-point-selects     oltpPointSelects    一意検索クエリの回数を指定するオプションです
oltp-simple-ranges     oltpSimpleRanges    範囲検索クエリの回数を指定するオプションです
oltp-sum-ranges        oltpSumRanges       範囲検索して集計するクエリの回数を指定するオプションです
oltp-order-ranges      oltpOrderRanges     範囲検索してソートするクエリの回数を指定するオプションです
oltp-distinct-ranges   oltpDistinctRanges  範囲検索して重複を省くクエリの回数を指定するオプションです
oltp-index-updates     oltpIndexUpdates    インデックス付き列を更新するクエリの回数を指定するオプションです
oltp-non-index-updates oltpNonIndexUpdates インデックスなし列を更新するクエリの回数を指定するオプションです
oltp-nontrx-mode       (未対応)            nontrxモードで実行するクエリを指定するオプションです
oltp-auto-inc          (未対応)            ID列にAUTO_INCREMENTを用いるかどうかを指定するオプションです
oltp-connect-delay     (未対応)            データベースに接続した後のスリープ時間を指定するオプションです
oltp-user-delay-min    (未対応)            クエリごとのスリープ時間の最小値を指定するオプションです
oltp-user-delay-max    (未対応)            クエリごとのスリープ時間の最大値を指定するオプションです
oltp-table-name        (未対応)            テストに用いるテーブル名を指定するオプションです
oltp-table-size        (ローダで指定)      テストに用いるテーブルのレコード数を指定するオプションです
oltp-dist-type         oltpDistType        乱数生成方式を指定するオプションです
oltp-dist-iter         oltpDistIter        ガウス分布乱数を生成するための加算回数を指定するオプションです
oltp-dist-pct          oltpDistPct         特殊分布乱数において、均一分布乱数の生成範囲を指定するオプションです
oltp-dist-res          oltpDistRes         特殊分布乱数において、均一分布乱数の発生確率を指定するオプションです
====================== =================== ====================================================================

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

complexモードで実行されるトランザクションの内容は以下のとおりです。主キーによる一意検索が10回、その他8種類のクエリは1回ずつ実行されます。この比率はカスタマイズ可能となっています。 ::

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

MySQLにrootユーザで接続し、sbtestデータベースを作成します。 ::

  shell> mysql -u root -p
  
  sql> CREATE DATABASE sbtest;
  Query OK, 1 row affected (0.00 sec)

ユーザの作成
^^^^^^^^^^^^

sbtestユーザを作成します。 ::

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

::

  shell> java JR scripts/sysbench_load.js
  13:44:17 [INFO ] > JdbcRunner 1.3.1
  13:44:17 [INFO ] [Config]
  Program start time   : 20230328-134416
  Script filename      : scripts/sysbench_load.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/sbtest?useSSL=false&allowPublicKeyRetrieval=true&  rewriteBatchedStatements=true
  JDBC user            : sbtest
  Load mode            : true
  Number of agents     : 1
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
  13:44:17 [INFO ] Tiny sysbench - data loader
  13:44:17 [INFO ] -param0 : Number of records (default : 10000)
  13:44:17 [INFO ] Number of records : 10000
  13:44:17 [INFO ] Dropping a table ...
  13:44:17 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'sbtest.sbtest'
  13:44:17 [INFO ] Creating a table ...
  13:44:17 [INFO ] Loading sbtest ...
  13:44:17 [INFO ] sbtest : 1000 / 10000
  13:44:17 [INFO ] sbtest : 2000 / 10000
  13:44:17 [INFO ] sbtest : 3000 / 10000
  13:44:17 [INFO ] sbtest : 4000 / 10000
  13:44:17 [INFO ] sbtest : 5000 / 10000
  13:44:17 [INFO ] sbtest : 6000 / 10000
  13:44:17 [INFO ] sbtest : 7000 / 10000
  13:44:17 [INFO ] sbtest : 8000 / 10000
  13:44:17 [INFO ] sbtest : 9000 / 10000
  13:44:17 [INFO ] sbtest : 10000 / 10000
  13:44:17 [INFO ] Creating an index ...
  13:44:17 [INFO ] Analyzing a table ...
  13:44:17 [INFO ] Completed.
  13:44:17 [INFO ] < JdbcRunner SUCCESS

「Unknown table 'sbtest'」という警告は、存在しないsbtestテーブルを削除しようとして出力されるものです。無視して構いません。

また、-param0を指定することによってsbtestテーブルにロードするレコード数を変更することが可能です。デフォルトは1万レコードとなっています。 ::

  shell> java JR scripts/sysbench_load.js -param0 50000

テストの実行
------------

scripts/sysbench.jsを用いてテストを実行します。JdbcRunnerを動作させるマシンは、テスト対象のマシンとは別に用意することをおすすめします。 ::

  shell> java JR scripts/sysbench.js -jdbcUrl jdbc:mysql://localhost/sbtest
  13:46:44 [INFO ] > JdbcRunner 1.3.1
  13:46:44 [INFO ] [Config]
  Program start time   : 20230328-134644
  Script filename      : scripts/sysbench.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost/sbtest
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
  13:46:45 [INFO ] Tiny sysbench
  13:46:45 [INFO ] Number of records : 10000
  13:46:46 [INFO ] [Warmup] -59 sec, 117 tps, (117 tx)
  13:46:47 [INFO ] [Warmup] -58 sec, 166 tps, (283 tx)
  13:46:48 [INFO ] [Warmup] -57 sec, 210 tps, (493 tx)
  13:46:49 [INFO ] [Warmup] -56 sec, 258 tps, (751 tx)
  13:46:50 [INFO ] [Warmup] -55 sec, 257 tps, (1008 tx)
  13:46:51 [INFO ] [Warmup] -54 sec, 215 tps, (1223 tx)
  13:46:51 [WARN ] [Agent 0] Deadlock detected.
  13:46:52 [INFO ] [Warmup] -53 sec, 251 tps, (1474 tx)
  ...
  13:50:40 [INFO ] [Progress] 175 sec, 342 tps, 60670 tx
  13:50:41 [WARN ] [Agent 2] Deadlock detected.
  13:50:41 [INFO ] [Progress] 176 sec, 340 tps, 61010 tx
  13:50:42 [INFO ] [Progress] 177 sec, 342 tps, 61352 tx
  13:50:43 [INFO ] [Progress] 178 sec, 354 tps, 61706 tx
  13:50:44 [INFO ] [Progress] 179 sec, 339 tps, 62045 tx
  13:50:45 [INFO ] [Progress] 180 sec, 311 tps, 62356 tx
  13:50:45 [INFO ] [Total tx count] 62356 tx
  13:50:45 [INFO ] [Throughput] 346.4 tps
  13:50:45 [INFO ] [Response time (minimum)] 6 msec
  13:50:45 [INFO ] [Response time (50%tile)] 45 msec
  13:50:45 [INFO ] [Response time (90%tile)] 74 msec
  13:50:45 [INFO ] [Response time (95%tile)] 81 msec
  13:50:45 [INFO ] [Response time (99%tile)] 96 msec
  13:50:45 [INFO ] [Response time (maximum)] 194 msec
  13:50:45 [INFO ] < JdbcRunner SUCCESS

OLTPベンチマークのcomplexモードでは、デッドロックが発生することがあります。これはオリジナル版のsysbenchでも発生するものです。Tiny sysbenchはデッドロックが発生した場合、該当のトランザクションをロールバックして再度実行します。

テストのカスタマイズ
--------------------

Tiny sysbenchはスクリプトscripts/sysbench.jsの変数定義を修正することで、オリジナル版のsysbenchが持つ設定オプションをある程度再現することができます。変数はスクリプトのApplication settingsという箇所に定義されていますので、ここを修正してご利用ください。 ::

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

テストキット Tiny TPC-C
=======================

TPC-Cとは
---------

`TPC-C <http://www.tpc.org/tpcc/>`_ とは、 `TPC <http://www.tpc.org/>`_ によって策定されたベンチマーク仕様の一つです。卸売業における注文・支払いなどの業務をモデルにしたトランザクションを実行し、システムの性能を測定します。データベースのER図を以下に示します。

.. image:: images/tpc-c.png

* warehouse : 倉庫を表しています。このテーブルのレコード数がデータベース全体の規模を決めるスケールファクタになっています。
* district : 配送区域を表しています。倉庫あたり10の配送区域があります。
* customer : 顧客を表しています。配送区域あたり3,000の顧客がいます。
* history : 支払い履歴を表しています。初期値として顧客あたり1件の支払い履歴があり、支払いを行うと増加していきます。
* item : 商品を表しています。このテーブルのレコード数は10万で固定されています。
* stock : 在庫を表しています。倉庫あたり10万の在庫データを持っています。
* orders : 注文を表しています。初期値として顧客あたり1つの注文があり、注文を行うと増加していきます。
* order_line : 注文明細を表しています。注文あたり平均10件の注文明細が作られます。
* new_orders : 未配送の新規注文を表しています。初期値として30%の顧客が1件ずつ未配送の新規注文を抱えており、注文を行うと増加し、配送が行われると減少します。

TPC-Cでは5種類のトランザクションが定義されています。5種類のトランザクションの実行比率は10:10:1:1:1となっています。

* New-Order : 注文処理です。
* Payment : 支払い処理です
* Order-Status : 注文状況を確認する処理です。
* Delivery : 配送処理です。
* Stock-Level : 在庫状況を確認する処理です。

New-Orderトランザクションの内容を擬似コードで表すと、以下のようになります。 ::

  SELECT FROM warehouse JOIN customer;
  SELECT FROM district FOR UPDATE;
  UPDATE district;
  INSERT INTO orders;
  INSERT INTO new_orders;

  LOOP {
    SELECT FROM item;
    SELECT FROM stock FOR UPDATE;
    UPDATE stock;
    INSERT INTO order_line;
  }

  COMMIT;

同様に、Paymentトランザクションの内容を以下に示します。 ::

  SELECT FROM warehouse FOR UPDATE;
  UPDATE warehouse;
  SELECT FROM district FOR UPDATE;
  UPDATE district;
  SELECT FROM customer;
  SELECT FROM customer FOR UPDATE;
  UPDATE customer;
  INSERT INTO history;
  COMMIT;

Order-Statusトランザクションの内容を以下に示します。 ::

  SELECT FROM customer;
  SELECT FROM customer;
  SELECT FROM orders WHERE id = (SELECT MAX(id) FROM orders);
  SELECT FROM order_line;
  COMMIT;

Deliveryトランザクションの内容を以下に示します。 ::

  LOOP {
    SELECT FROM new_orders WHERE id = (SELECT MIN(id) FROM new_orders) FOR UPDATE;
    DELETE FROM new_orders;
    SELECT FROM orders FOR UPDATE;
    UPDATE orders;
    UPDATE order_line;
    SELECT FROM order_line;
    UPDATE customer;
  }

  COMMIT;

Stock-Levelトランザクションの内容を以下に示します。 ::

  SELECT FROM district JOIN order_line JOIN stock;
  COMMIT;

TPC-CのCRUD図を以下に示します。

============ ========= ======== ======== ======= ==== ===== ====== ========== ==========
Transaction  warehouse district customer history item stock orders new_orders order_line
============ ========= ======== ======== ======= ==== ===== ====== ========== ==========
New-Order    R         RU       R                R    RU    C      C          C
Payment      RU        RU       RU       C
Order-Status                    R                           R                 R
Delivery                        U                           RU     RD         RU
Stock-Level            R                              R                       R
============ ========= ======== ======== ======= ==== ===== ====== ========== ==========

Tiny TPC-Cとは
--------------

Tiny TPC-Cは、TPC-C Standard Specification 5.11の仕様を抜粋しJdbcRunnerのスクリプトとして実装したものです。仕様書のうち以下の章節を実装しています。

* 1 LOGICAL DATABASE DESIGN
* 2 TRANSACTION and TERMINAL PROFILES

  * 2.4 The New-Order Transaction (2.4.1.1、2.4.3を除く)
  * 2.5 The Payment Transaction (2.5.1.1、2.5.3を除く)
  * 2.6 The Order-Status Transaction (2.6.1.1、2.6.3を除く)
  * 2.7 The Delivery Transaction (2.7.1.1、2.7.2、2.7.3を除く)
  * 2.8 The Stock-Level Transaction (2.8.1、2.8.3を除く)

* 4 SCALING and DATABASE POPULATION

  * 4.3 Database Population

* 5 PERFORMANCE METRICS and RESPONSE TIME

  * 5.2 Pacing of Transactions by Emulated Users

    * 5.2.4 Regulation of Transaction Mix

それ以外の章節については実装されていないか、仕様を満たしていません。従ってTiny TPC-Cのテスト結果は正式なTPC-Cのスコアではありません。

Tiny TPC-Cは以下の二つのスクリプトから構成されています。

* scripts/tpcc_load.js : テストデータ生成用スクリプト
* scripts/tpcc.js : テスト用スクリプト

動作確認RDBMS
-------------

Tiny TPC-Cは、以下のRDBMSで動作確認をしています。

* Oracle Database 18c
* MySQL 8.0
* PostgreSQL 10

テストの準備
------------

MySQLにおけるテストの準備手順を以下に示します。Oracle Database、PostgreSQLについてはscripts/tpcc_load.jsのコメントをご参照ください。

データベースの作成
^^^^^^^^^^^^^^^^^^

MySQLにrootユーザで接続し、tpccデータベースを作成します。 ::

  shell> mysql -u root -p

  sql> CREATE DATABASE tpcc;
  Query OK, 1 row affected (0.00 sec)

ユーザの作成
^^^^^^^^^^^^

tpccユーザを作成します。 ::

  sql> CREATE USER tpcc@'%' IDENTIFIED BY 'tpcc';
  Query OK, 0 rows affected (0.00 sec)

  sql> GRANT ALL PRIVILEGES ON tpcc.* TO tpcc@'%';
  Query OK, 0 rows affected (0.00 sec)

ネットワーク環境によっては、接続元ホストを制限したりtpccをより安全なパスワードに変更したりすることをおすすめします。

テストデータの生成
^^^^^^^^^^^^^^^^^^

scripts/tpcc_load.jsを用いてテストデータの生成を行います。このスクリプトは以下の処理を行っています。

* テーブルの削除
* テーブルの作成
* データロード
* インデックスの作成 (MySQLの主キーはデータロード前に作成)
* 統計情報の更新

::

  shell> java JR scripts/tpcc_load.js
  15:53:05 [INFO ] > JdbcRunner 1.3
  15:53:05 [INFO ] [Config]
  Program start time   : 20180819-155305
  Script filename      : scripts/tpcc_load.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/tpcc?rewriteBatchedStatements=true
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
  15:53:06 [INFO ] Tiny TPC-C - data loader
  15:53:06 [INFO ] -param0  : Scale factor (default : 16)
  15:53:06 [INFO ] -nAgents : Parallel loading degree (default : 4)
  15:53:06 [INFO ] Scale factor            : 16
  15:53:06 [INFO ] Parallel loading degree : 4
  15:53:06 [INFO ] Dropping tables ...
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.order_line'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.new_orders'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.orders'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.stock'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.item'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.history'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.customer'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.district'
  15:53:06 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.warehouse'
  15:53:06 [INFO ] Creating tables ...
  15:53:06 [INFO ] Loading item ...
  15:53:07 [INFO ] item : 10000 / 100000
  15:53:08 [INFO ] item : 20000 / 100000
  15:53:09 [INFO ] item : 30000 / 100000
  15:53:09 [INFO ] item : 40000 / 100000
  15:53:10 [INFO ] item : 50000 / 100000
  15:53:10 [INFO ] item : 60000 / 100000
  15:53:10 [INFO ] item : 70000 / 100000
  15:53:11 [INFO ] item : 80000 / 100000
  15:53:11 [INFO ] item : 90000 / 100000
  15:53:12 [INFO ] item : 100000 / 100000
  15:53:12 [INFO ] Loading warehouse id 1 by agent 1 ...
  15:53:12 [INFO ] Loading warehouse id 2 by agent 2 ...
  15:53:12 [INFO ] Loading warehouse id 3 by agent 3 ...
  15:53:12 [INFO ] Loading warehouse id 4 by agent 0 ...
  ...
  15:59:17 [INFO ] [Agent 2] orders : 30000 / 30000
  15:59:18 [INFO ] [Agent 0] orders : 30000 / 30000
  15:59:19 [INFO ] [Agent 1] orders : 30000 / 30000
  15:59:19 [INFO ] [Agent 3] orders : 30000 / 30000
  15:59:19 [INFO ] Creating indexes ...
  15:59:24 [INFO ] Analyzing tables ...
  15:59:24 [INFO ] Completed.
  15:59:24 [INFO ] < JdbcRunner SUCCESS

「Unknown table 'order_line'」などの警告は、存在しないテーブルを削除しようとして出力されるものです。無視して構いません。

-param0を指定することによって、スケールファクタを変更することが可能です。スケールファクタ1あたりwarehouseテーブルのレコード数が1増加し、その他のテーブルについてもレコード数が以下のように増加します。デフォルトのスケールファクタは16です。

========== ======================
Table      Records
========== ======================
warehouse  sf x 1
district   sf x 10
customer   sf x 30,000
history    sf x 30,000
item       100,000
stock      sf x 100,000
orders     sf x 30,000
new_orders sf x 9,000
order_line sf x 300,000 (approx.)
========== ======================

-nAgentsを指定することによって、ロードの並列度を変更することが可能です。CPUコア数の多い環境では、並列度を上げることでロード時間を短縮することができます。デフォルトの並列度は4です。 ::

  shell> java JR scripts/tpcc_load.js -nAgents 8 -param0 100


テストの実行
------------

scripts/tpcc.jsを用いてテストを実行します。JdbcRunnerを動作させるマシンは、テスト対象のマシンとは別に用意することをおすすめします。 ::

  shell> java JR scripts/tpcc.js -jdbcUrl jdbc:mysql://server/tpcc
  16:05:22 [INFO ] > JdbcRunner 1.3
  16:05:22 [INFO ] [Config]
  Program start time   : 20180819-160522
  Script filename      : scripts/tpcc.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://server/tpcc
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
  16:05:23 [INFO ] Tiny TPC-C
  16:05:23 [INFO ] Scale factor : 16
  16:05:23 [INFO ] tx0 : New-Order transaction
  16:05:23 [INFO ] tx1 : Payment transaction
  16:05:23 [INFO ] tx2 : Order-Status transaction
  16:05:23 [INFO ] tx3 : Delivery transaction
  16:05:23 [INFO ] tx4 : Stock-Level transaction
  16:05:24 [INFO ] [Warmup] -299 sec, 18,34,2,0,3 tps, (18,34,2,0,3 tx)
  16:05:25 [INFO ] [Warmup] -298 sec, 42,27,3,2,5 tps, (60,61,5,2,8 tx)
  16:05:26 [INFO ] [Warmup] -297 sec, 40,33,5,6,5 tps, (100,94,10,8,13 tx)
  ...
  16:25:20 [INFO ] [Progress] 897 sec, 47,60,5,7,5 tps, 42576,42577,4259,4254,4257 tx
  16:25:21 [INFO ] [Progress] 898 sec, 50,47,2,7,3 tps, 42626,42624,4261,4261,4260 tx
  16:25:22 [INFO ] [Progress] 899 sec, 50,46,4,5,8 tps, 42676,42670,4265,4266,4268 tx
  16:25:23 [INFO ] [Progress] 900 sec, 51,52,7,5,3 tps, 42727,42722,4272,4271,4271 tx
  16:25:23 [INFO ] [Total tx count] 42727,42723,4272,4271,4271 tx
  16:25:23 [INFO ] [Throughput] 47.5,47.5,4.7,4.7,4.7 tps
  16:25:23 [INFO ] [Response time (minimum)] 9,6,2,79,3 msec
  16:25:23 [INFO ] [Response time (50%tile)] 212,52,12,465,48 msec
  16:25:23 [INFO ] [Response time (90%tile)] 347,100,42,662,117 msec
  16:25:23 [INFO ] [Response time (95%tile)] 386,131,51,730,137 msec
  16:25:23 [INFO ] [Response time (99%tile)] 476,252,72,903,180 msec
  16:25:23 [INFO ] [Response time (maximum)] 916,567,111,1507,421 msec
  16:25:23 [INFO ] < JdbcRunner SUCCESS

TPC-Cでは5種類のトランザクションが定義されており、結果は左からNew-Order、Payment、Order-Status、Delivery、Stock-Levelトランザクションのものとなっています。

TPC-CのスコアにはNew-Orderトランザクションの1分あたりの実行回数を用いることが多いです。上記の例では15分間で42,727txですから、スコアは2,848.5tpmとなります。

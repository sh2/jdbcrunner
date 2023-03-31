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

New-Orderトランザクションの内容を擬似コードで表すと、以下のようになります。

.. code-block:: plpgsql

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

同様に、Paymentトランザクションの内容を以下に示します。

.. code-block:: plpgsql

  SELECT FROM warehouse FOR UPDATE;
  UPDATE warehouse;
  SELECT FROM district FOR UPDATE;
  UPDATE district;
  SELECT FROM customer;
  SELECT FROM customer FOR UPDATE;
  UPDATE customer;
  INSERT INTO history;
  COMMIT;

Order-Statusトランザクションの内容を以下に示します。

.. code-block:: plpgsql

  SELECT FROM customer;
  SELECT FROM customer;
  SELECT FROM orders WHERE id = (SELECT MAX(id) FROM orders);
  SELECT FROM order_line;
  COMMIT;

Deliveryトランザクションの内容を以下に示します。

.. code-block:: plpgsql

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

Stock-Levelトランザクションの内容を以下に示します。

.. code-block:: plpgsql

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

* Oracle Database 21c
* MySQL 8.0
* PostgreSQL 15

テストの準備
------------

MySQLにおけるテストの準備手順を以下に示します。Oracle Database、PostgreSQLについてはscripts/tpcc_load.jsのコメントをご参照ください。

データベースの作成
^^^^^^^^^^^^^^^^^^

MySQLにrootユーザで接続し、tpccデータベースを作成します。

.. code-block:: mysql

  shell> mysql -u root -p

  sql> CREATE DATABASE tpcc;
  Query OK, 1 row affected (0.00 sec)

ユーザの作成
^^^^^^^^^^^^

tpccユーザを作成します。

.. code-block:: mysql

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

.. code-block:: text

  shell> java JR ../scripts/tpcc_load.js -logDir logs_sample10
  13:20:49 [INFO ] > JdbcRunner 1.3.1
  13:20:49 [INFO ] [Config]
  Program start time   : 20230331-132048
  Script filename      : ../scripts/tpcc_load.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/tpcc?rewriteBatchedStatements=true
  JDBC user            : tpcc
  Load mode            : true
  Number of agents     : 4
  Auto commit          : false
  Debug mode           : false
  Trace mode           : false
  Log directory        : logs_sample10
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
  13:20:49 [INFO ] Tiny TPC-C - data loader
  13:20:49 [INFO ] -param0  : Scale factor (default : 16)
  13:20:49 [INFO ] -nAgents : Parallel loading degree (default : 4)
  13:20:49 [INFO ] Scale factor            : 16
  13:20:49 [INFO ] Parallel loading degree : 4
  13:20:49 [INFO ] Dropping tables ...
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.order_line'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.new_orders'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.orders'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.stock'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.item'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.history'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.customer'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.district'
  13:20:49 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcc.warehouse'
  13:20:49 [INFO ] Creating tables ...
  13:20:49 [INFO ] Loading item ...
  13:20:50 [INFO ] item : 10000 / 100000
  13:20:50 [INFO ] item : 20000 / 100000
  13:20:51 [INFO ] item : 30000 / 100000
  13:20:51 [INFO ] item : 40000 / 100000
  13:20:51 [INFO ] item : 50000 / 100000
  13:20:51 [INFO ] item : 60000 / 100000
  13:20:51 [INFO ] item : 70000 / 100000
  13:20:52 [INFO ] item : 80000 / 100000
  13:20:52 [INFO ] item : 90000 / 100000
  13:20:52 [INFO ] item : 100000 / 100000
  13:20:52 [INFO ] Loading warehouse id 4 by agent 2 ...
  13:20:52 [INFO ] Loading warehouse id 2 by agent 3 ...
  13:20:52 [INFO ] Loading warehouse id 3 by agent 0 ...
  13:20:52 [INFO ] Loading warehouse id 1 by agent 1 ...
  ...
  13:23:11 [INFO ] [Agent 2] orders : 30000 / 30000
  13:23:15 [INFO ] [Agent 0] orders : 30000 / 30000
  13:23:16 [INFO ] [Agent 1] orders : 30000 / 30000
  13:23:16 [INFO ] [Agent 3] orders : 30000 / 30000
  13:23:16 [INFO ] Creating indexes ...
  13:23:21 [INFO ] Analyzing tables ...
  13:23:21 [INFO ] Completed.
  13:23:21 [INFO ] < JdbcRunner SUCCESS

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

-nAgentsを指定することによって、ロードの並列度を変更することが可能です。CPUコア数の多い環境では、並列度を上げることでロード時間を短縮することができます。デフォルトの並列度は4です。

.. code-block:: text

  shell> java JR ../scripts/tpcc_load.js -nAgents 8 -param0 100


テストの実行
------------

scripts/tpcc.jsを用いてテストを実行します。以下の例ではlocalhostのRDBMSに対してテストを行っていますが、実際にはJdbcRunnerとRDBMSを異なるコンピュータに配置することをおすすめします。

.. code-block:: text

  shell> java JR ../scripts/tpcc.js -logDir logs_sample10 -warmupTime 300 -measurementTime 900
  13:23:21 [INFO ] > JdbcRunner 1.3.1
  13:23:21 [INFO ] [Config]
  Program start time   : 20230331-132321
  Script filename      : ../scripts/tpcc.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/tpcc
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
  Log directory        : logs_sample10
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
  13:23:23 [INFO ] Tiny TPC-C
  13:23:23 [INFO ] Scale factor : 16
  13:23:23 [INFO ] tx0 : New-Order transaction
  13:23:23 [INFO ] tx1 : Payment transaction
  13:23:23 [INFO ] tx2 : Order-Status transaction
  13:23:23 [INFO ] tx3 : Delivery transaction
  13:23:23 [INFO ] tx4 : Stock-Level transaction
  13:23:24 [INFO ] [Warmup] -299 sec, 24,23,4,2,6 tps, (24,23,4,2,6 tx)
  13:23:25 [INFO ] [Warmup] -298 sec, 29,28,3,3,1 tps, (53,51,7,5,7 tx)
  13:23:26 [INFO ] [Warmup] -297 sec, 40,44,1,3,3 tps, (93,95,8,8,10 tx)
  ...
  13:43:21 [INFO ] [Progress] 898 sec, 125,119,12,14,12 tps, 106699,106695,10668,10674,10673 tx
  13:43:22 [INFO ] [Progress] 899 sec, 125,128,11,12,11 tps, 106824,106823,10679,10686,10684 tx
  13:43:23 [INFO ] [Progress] 900 sec, 119,114,16,14,10 tps, 106943,106937,10695,10700,10694 tx
  13:43:23 [INFO ] [Total tx count] 106943,106937,10695,10700,10694 tx
  13:43:23 [INFO ] [Throughput] 118.8,118.8,11.9,11.9,11.9 tps
  13:43:23 [INFO ] [Response time (minimum)] 3,2,0,19,9 msec
  13:43:23 [INFO ] [Response time (50%tile)] 70,17,6,156,116 msec
  13:43:23 [INFO ] [Response time (90%tile)] 157,45,23,287,235 msec
  13:43:23 [INFO ] [Response time (95%tile)] 182,58,29,321,261 msec
  13:43:23 [INFO ] [Response time (99%tile)] 228,104,43,387,301 msec
  13:43:23 [INFO ] [Response time (maximum)] 396,298,108,557,490 msec
  13:43:23 [INFO ] < JdbcRunner SUCCESS

TPC-Cでは5種類のトランザクションが定義されており、結果は左からNew-Order、Payment、Order-Status、Delivery、Stock-Levelトランザクションのものとなっています。

TPC-CのスコアにはNew-Orderトランザクションの1分あたりの実行回数を用いることが多いです。上記の例では15分間で106,943txですから、スコアは7,129.5tpmとなります。

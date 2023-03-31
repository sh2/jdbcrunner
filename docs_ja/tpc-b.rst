テストキット Tiny TPC-B
=======================

TPC-Bとは
---------

`TPC-B <http://www.tpc.org/tpcb/>`_ とは、 `TPC <http://www.tpc.org/>`_ によって策定されたベンチマーク仕様の一つです。銀行の窓口業務をモデルにしたトランザクションを実行し、システムの性能を測定します。データベースのER図を以下に示します。

.. image:: images/tpc-b.png

* branches : 銀行の支店を表しています。このテーブルのレコード数がデータベース全体の規模を決めるスケールファクタになっています。
* tellers : 銀行員を表しています。支店あたり10名の銀行員がいます。
* accounts : 銀行口座を表しています。支店あたり10万の口座があります。
* history : 取引履歴を表しています。

TPC-Bでは1種類のトランザクションが定義されています。これは以下のSQLを順番に発行するものです。

.. code-block:: mysql

  UPDATE accounts SET abalance = abalance + :1 WHERE aid = :2;
  SELECT abalance FROM accounts WHERE aid = :1;
  UPDATE tellers SET tbalance = abalance + :1 WHERE aid = :2;
  UPDATE branches SET bbalance = abalance + :1 WHERE aid = :2;
  INSERT INTO history (aid, aid, aid, delta) VALUES (:1, :2, :3, :4);
  COMMIT;

TPC-BのCRUD図を以下に示します。TPC-Bには更新の割合が非常に高いという特徴があります。

=========== ======== ======= ======== =======
Transaction branches tellers accounts history
=========== ======== ======= ======== =======
TPC-B       U        U       RU       C
=========== ======== ======= ======== =======

Tiny TPC-Bとは
--------------

Tiny TPC-Bは、TPC-B Standard Specification 2.0の仕様を抜粋しJdbcRunnerのスクリプトとして実装したものです。仕様書のうち以下の章節を実装しています。

* 1 Transaction Profile

  * 1.2 The Transaction Profile

* 3 Logical Database Design
* 4 Scaling Rules
* 5 Distribution, Partitioning, and Transaction Generation

それ以外の章節については実装されていないか、仕様を満たしていません。従ってTiny TPC-Bのテスト結果は正式なTPC-Bのスコアではありません。

Tiny TPC-Bは以下の二つのスクリプトから構成されています。

* scripts/tpcb_load.js : テストデータ生成用スクリプト
* scripts/tpcb.js : テスト用スクリプト

動作確認RDBMS
-------------

Tiny TPC-Bは、以下のRDBMSで動作確認をしています。

* Oracle Database 21c
* MySQL 8.0
* PostgreSQL 15

テストの準備
------------

MySQLにおけるテストの準備手順を以下に示します。Oracle Database、PostgreSQLについてはscripts/tpcb_load.jsのコメントをご参照ください。

データベースの作成
^^^^^^^^^^^^^^^^^^

MySQLにrootユーザで接続し、tpcbデータベースを作成します。

.. code-block:: mysql

  shell> mysql -u root -p

  sql> CREATE DATABASE tpcb;
  Query OK, 1 row affected (0.00 sec)

ユーザの作成
^^^^^^^^^^^^

tpcbユーザを作成します。

.. code-block:: mysql

  sql> CREATE USER tpcb@'%' IDENTIFIED BY 'tpcb';
  Query OK, 0 rows affected (0.00 sec)

  sql> GRANT ALL PRIVILEGES ON tpcb.* TO tpcb@'%';
  Query OK, 0 rows affected (0.00 sec)

ネットワーク環境によっては、接続元ホストを制限したりtpcbをより安全なパスワードに変更したりすることをおすすめします。

テストデータの生成
^^^^^^^^^^^^^^^^^^

scripts/tpcb_load.jsを用いてテストデータの生成を行います。このスクリプトは以下の処理を行っています。

* テーブルの削除
* テーブルの作成
* データロード
* インデックスの作成 (MySQLの主キーはデータロード前に作成)
* 統計情報の更新

.. code-block:: text

  shell> java JR ../scripts/tpcb_load.js -logDir logs_sample09
  13:16:30 [INFO ] > JdbcRunner 1.3.1
  13:16:30 [INFO ] [Config]
  Program start time   : 20230331-131630
  Script filename      : ../scripts/tpcb_load.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/tpcb?rewriteBatchedStatements=true
  JDBC user            : tpcb
  Load mode            : true
  Number of agents     : 4
  Auto commit          : false
  Debug mode           : false
  Trace mode           : false
  Log directory        : logs_sample09
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
  13:16:31 [INFO ] Tiny TPC-B - data loader
  13:16:31 [INFO ] -param0  : Scale factor (default : 16)
  13:16:31 [INFO ] -nAgents : Parallel loading degree (default : 4)
  13:16:31 [INFO ] Scale factor            : 16
  13:16:31 [INFO ] Parallel loading degree : 4
  13:16:31 [INFO ] Dropping tables ...
  13:16:31 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcb.history'
  13:16:31 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcb.accounts'
  13:16:31 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcb.tellers'
  13:16:31 [WARN ] JavaException: java.sql.SQLSyntaxErrorException: Unknown table 'tpcb.branches'
  13:16:31 [INFO ] Creating tables ...
  13:16:31 [INFO ] Loading branch id 1 by agent 3 ...
  13:16:31 [INFO ] Loading branch id 4 by agent 0 ...
  13:16:31 [INFO ] Loading branch id 3 by agent 2 ...
  13:16:31 [INFO ] Loading branch id 2 by agent 1 ...
  13:16:35 [INFO ] Loading branch id 5 by agent 3 ...
  13:16:36 [INFO ] Loading branch id 6 by agent 0 ...
  13:16:36 [INFO ] Loading branch id 7 by agent 1 ...
  13:16:36 [INFO ] Loading branch id 8 by agent 2 ...
  13:16:39 [INFO ] Loading branch id 9 by agent 3 ...
  13:16:40 [INFO ] Loading branch id 10 by agent 1 ...
  13:16:40 [INFO ] Loading branch id 11 by agent 0 ...
  13:16:40 [INFO ] Loading branch id 12 by agent 2 ...
  13:16:42 [INFO ] Loading branch id 13 by agent 3 ...
  13:16:43 [INFO ] Loading branch id 14 by agent 0 ...
  13:16:43 [INFO ] Loading branch id 15 by agent 2 ...
  13:16:43 [INFO ] Loading branch id 16 by agent 1 ...
  13:16:47 [INFO ] Analyzing tables ...
  13:16:47 [INFO ] Completed.
  13:16:47 [INFO ] < JdbcRunner SUCCESS

「Unknown table 'history'」などの警告は、存在しないテーブルを削除しようとして出力されるものです。無視して構いません。

-param0を指定することによって、スケールファクタを変更することが可能です。スケールファクタ1あたり、branchesテーブルが1レコード、tellersテーブルが10レコード、accountsテーブルが10万レコード増加します。デフォルトのスケールファクタは16です。

-nAgentsを指定することによって、ロードの並列度を変更することが可能です。CPUコア数の多い環境では、並列度を上げることでロード時間を短縮することができます。デフォルトの並列度は4です。

.. code-block:: text

  shell> java JR ../scripts/tpcb_load.js -nAgents 8 -param0 100

テストの実行
------------

scripts/tpcb.jsを用いてテストを実行します。以下の例ではlocalhostのRDBMSに対してテストを行っていますが、実際にはJdbcRunnerとRDBMSを異なるコンピュータに配置することをおすすめします。

.. code-block:: text

  + java JR ../scripts/tpcb.js -logDir logs_sample09 -warmupTime 60 -measurementTime 180
  13:16:47 [INFO ] > JdbcRunner 1.3.1
  13:16:47 [INFO ] [Config]
  Program start time   : 20230331-131647
  Script filename      : ../scripts/tpcb.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/tpcb
  JDBC user            : tpcb
  Warmup time          : 60 sec
  Measurement time     : 180 sec
  Number of tx types   : 1
  Number of agents     : 16
  Connection pool size : 16
  Statement cache size : 10
  Auto commit          : false
  Sleep time           : 0 msec
  Throttle             : - tps
  Debug mode           : false
  Trace mode           : false
  Log directory        : logs_sample09
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
  13:16:48 [INFO ] Tiny TPC-B
  13:16:48 [INFO ] Scale factor : 16
  13:16:48 [INFO ] Truncating history table...
  13:16:49 [INFO ] [Warmup] -59 sec, 371 tps, (371 tx)
  13:16:50 [INFO ] [Warmup] -58 sec, 649 tps, (1020 tx)
  13:16:51 [INFO ] [Warmup] -57 sec, 830 tps, (1850 tx)
  ...
  13:20:46 [INFO ] [Progress] 178 sec, 1013 tps, 178107 tx
  13:20:47 [INFO ] [Progress] 179 sec, 970 tps, 179077 tx
  13:20:48 [INFO ] [Progress] 180 sec, 984 tps, 180061 tx
  13:20:48 [INFO ] [Total tx count] 180061 tx
  13:20:48 [INFO ] [Throughput] 1000.3 tps
  13:20:48 [INFO ] [Response time (minimum)] 1 msec
  13:20:48 [INFO ] [Response time (50%tile)] 16 msec
  13:20:48 [INFO ] [Response time (90%tile)] 25 msec
  13:20:48 [INFO ] [Response time (95%tile)] 28 msec
  13:20:48 [INFO ] [Response time (99%tile)] 35 msec
  13:20:48 [INFO ] [Response time (maximum)] 86 msec
  13:20:48 [INFO ] < JdbcRunner SUCCESS

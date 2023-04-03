チュートリアル
==============

この章では、LinuxとMySQLを使用して簡単な負荷テストを行うまでの手順を説明します。

データベースの準備
------------------

MySQLサーバーにrootユーザーでログインして、tutorialデータベースとrunnerユーザーを作成します。
パスワードはお使いの環境に合わせて変更してください。

.. code-block:: mysql

  shell> mysql -u root -p
  sql> CREATE DATABASE tutorial;
  sql> CREATE USER runner@'%' IDENTIFIED BY 'change_on_install';
  sql> GRANT ALL PRIVILEGES ON tutorial.* TO runner@'%';

MySQLサーバーにrunnerユーザーでログインし直してsampleテーブルを作成し、テストデータをINSERTします。

.. code-block:: mysql

  shell> mysql -u runner -p tutorial
  sql> CREATE TABLE sample (id INT PRIMARY KEY, data VARCHAR(10)) ENGINE = InnoDB;
  sql> INSERT INTO sample (id, data) VALUES (1, 'aaaaaaaaaa');
  sql> INSERT INTO sample (id, data) VALUES (2, 'bbbbbbbbbb');
  sql> INSERT INTO sample (id, data) VALUES (3, 'cccccccccc');
  sql> INSERT INTO sample (id, data) VALUES (4, 'dddddddddd');
  sql> INSERT INTO sample (id, data) VALUES (5, 'eeeeeeeeee');

テーブルの中身は以下のようになります。

.. code-block:: mysql

  sql> SELECT * FROM sample ORDER BY id;
  +----+------------+
  | id | data       |
  +----+------------+
  |  1 | aaaaaaaaaa |
  |  2 | bbbbbbbbbb |
  |  3 | cccccccccc |
  |  4 | dddddddddd |
  |  5 | eeeeeeeeee |
  +----+------------+
  5 rows in set (0.00 sec)

ツールのセットアップ
--------------------

JdbcRunnerのJARファイルを任意のディレクトリーに配置し、環境変数CLASSPATHを設定します。

.. code-block:: text

  shell> export CLASSPATH=jdbcrunner-1.3.1.jar

ツールの起動クラスは、パッケージなしのJRです。
追加のオプションなしで実行すると、簡単な使い方が表示されます。

.. code-block:: text

  shell> java JR
  JdbcRunner 1.3.1
  スクリプトファイルが指定されていません

  usage: java JR <script> [options]
  -autoCommit <arg>     オートコミットモードを有効化または無効化します (デフォルト : true (有効))
  -connPoolSize <arg>   コネクションプールの物理接続数を指定します (デフォルト : nAgents)
  -debug                デバッグモードを有効にします (デフォルト : false)
  -jdbcDriver <arg>     JDBCドライバーのクラス名を指定します (デフォルト : (なし))
  -jdbcPass <arg>       データベースユーザーのパスワードを指定します (デフォルト : (なし))
  -jdbcUrl <arg>        JDBC接続URLを指定します (デフォルト : jdbc:mysql://localhost:3306/test)
  -jdbcUser <arg>       データベースのユーザー名を指定します (デフォルト : (なし))
  -logDir <arg>         ログの出力先ディレクトリーを指定します (デフォルト : .)
  -measurementTime <arg>測定時間[sec]を指定します (デフォルト : 60)
  -nAgents <arg>        エージェント数を指定します (デフォルト : 1)
  -param0 <arg>         変数param0に値を設定します
  -param1 <arg>         変数param1に値を設定します
  -param2 <arg>         変数param2に値を設定します
  -param3 <arg>         変数param3に値を設定します
  -param4 <arg>         変数param4に値を設定します
  -param5 <arg>         変数param5に値を設定します
  -param6 <arg>         変数param6に値を設定します
  -param7 <arg>         変数param7に値を設定します
  -param8 <arg>         変数param8に値を設定します
  -param9 <arg>         変数param9に値を設定します
  -scriptCharset <arg>  スクリプトの文字セットを指定します
  -sleepTime <arg>      トランザクションごとのスリープ時間[msec]を指定します (デフォルト : 0)
  -stmtCacheSize <arg>  コネクションあたりの文キャッシュ数を指定します (デフォルト : 10)
  -throttle <arg>       スループットの上限値[tps]を指定します (デフォルト : 0 (無制限))
  -trace                トレースモードを有効にします (デフォルト : false)
  -warmupTime <arg>     測定前にあらかじめ負荷をかけておく時間[sec]を指定します (デフォルト : 10)

スクリプトの作成
----------------

JdbcRunnerでは、負荷テストのシナリオをスクリプトで定義します。
以下のスクリプトをtest.jsというファイル名で作成します。

.. code-block:: javascript

  function run() {
      var param = random(1, 5);
      query("SELECT data FROM sample WHERE id = $int", param);
  }

このスクリプトは「1以上5以下の乱数を生成し、生成された値をint型としてクエリーのパラメーターにバインドして実行する」というファンクションを定義するものです。
JdbcRunnerはrun()ファンクションで定義された処理を指定された多重度で指定された時間だけ繰り返し実行し、スループットとレスポンスタイムを出力します。

負荷テストの実行
----------------

作成したスクリプトといくつかのオプションを指定して、負荷テストを開始します。

.. code-block:: text

  shell> java JR test.js -jdbcUrl jdbc:mysql://localhost/tutorial -jdbcUser runner -jdbcPass change_on_install -logDir logs_sample02
  13:11:17 [INFO ] > JdbcRunner 1.3.1
  13:11:17 [INFO ] [Config]
  Program start time   : 20230331-131116
  Script filename      : test.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost/tutorial
  JDBC user            : runner
  Warmup time          : 10 sec
  Measurement time     : 60 sec
  Number of tx types   : 1
  Number of agents     : 1
  Connection pool size : 1
  Statement cache size : 10
  Auto commit          : true
  Sleep time           : 0 msec
  Throttle             : - tps
  Debug mode           : false
  Trace mode           : false
  Log directory        : logs_sample02
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
  13:11:18 [INFO ] [Warmup] -9 sec, 1636 tps, (1636 tx)
  13:11:19 [INFO ] [Warmup] -8 sec, 2951 tps, (4587 tx)
  13:11:20 [INFO ] [Warmup] -7 sec, 3574 tps, (8161 tx)
  13:11:21 [INFO ] [Warmup] -6 sec, 3760 tps, (11921 tx)
  13:11:22 [INFO ] [Warmup] -5 sec, 3968 tps, (15889 tx)
  13:11:23 [INFO ] [Warmup] -4 sec, 3816 tps, (19705 tx)
  13:11:24 [INFO ] [Warmup] -3 sec, 3933 tps, (23638 tx)
  13:11:25 [INFO ] [Warmup] -2 sec, 3951 tps, (27589 tx)
  13:11:26 [INFO ] [Warmup] -1 sec, 4132 tps, (31721 tx)
  13:11:27 [INFO ] [Warmup] 0 sec, 3917 tps, (35638 tx)
  13:11:28 [INFO ] [Progress] 1 sec, 3928 tps, 3928 tx
  13:11:29 [INFO ] [Progress] 2 sec, 3982 tps, 7910 tx
  13:11:30 [INFO ] [Progress] 3 sec, 3931 tps, 11841 tx
  ...
  13:12:25 [INFO ] [Progress] 58 sec, 3959 tps, 231294 tx
  13:12:26 [INFO ] [Progress] 59 sec, 3988 tps, 235282 tx
  13:12:27 [INFO ] [Progress] 60 sec, 3923 tps, 239205 tx
  13:12:27 [INFO ] [Total tx count] 239205 tx
  13:12:27 [INFO ] [Throughput] 3986.8 tps
  13:12:27 [INFO ] [Response time (minimum)] 0 msec
  13:12:27 [INFO ] [Response time (50%tile)] 0 msec
  13:12:27 [INFO ] [Response time (90%tile)] 0 msec
  13:12:27 [INFO ] [Response time (95%tile)] 0 msec
  13:12:27 [INFO ] [Response time (99%tile)] 0 msec
  13:12:27 [INFO ] [Response time (maximum)] 13 msec
  13:12:27 [INFO ] < JdbcRunner SUCCESS

負荷テストを開始すると、標準出力に負荷テストの設定、進捗状況、測定結果が出力されます。
同様の内容はログファイルjdbcrunner.logにも出力されます。
負荷テストの設定のセクションからは、例えば以下のような情報が読み取れます。

* 測定する際、あらかじめ10秒間のウォームアップを行う(Warmup time)
* 60秒間測定する(Measurement time)
* 多重度は1(Number of agents)

進捗状況のセクションからは、毎秒4,000弱のトランザクションが実行されていることが読み取れます。
なお、ここで言うトランザクションとはスクリプトに定義されたrun()ファンクションを1回実行することを示しています。
必ずしもRDBMSにとってのトランザクション数と一致するわけではない点に注意してください。

測定結果のセクションには、合計のトランザクション数、スループット、レスポンスタイムが出力されます。
合計のトランザクション数からは、ウォームアップ時間に行われたトランザクションは除外されます。
レスポンスタイムはrun()ファンクションを1回実行するのにかかった時間のことで、最小値、50パーセンタイル値(中央値)、90パーセンタイル値、95パーセンタイル値、99パーセンタイル値、最大値の6種類が出力されます。
ここで、レスポンスタイムが0ミリ秒というのは正確には0ミリ秒以上1ミリ秒未満であることを示しています。

結果ファイルの確認
------------------

負荷テストが正常終了すると、ログファイルjdbcrunner.logの他に2つの結果ファイルが出力されます。

.. code-block:: text

  shell> ls -l
  -rw-rw-r-- 1 taira taira 6021  3月 31  2023 jdbcrunner.log
  -rw-rw-r-- 1 taira taira   91  3月 31  2023 log_20230331-131116_r.csv
  -rw-rw-r-- 1 taira taira  505  3月 31  2023 log_20230331-131116_t.csv

log_20230331-131116_r.csvと末尾に「_r」がついたCSVファイルは、レスポンスタイムの度数分布データです。
レスポンスタイムごとにトランザクション実行数が出力されます。

.. code-block:: text

  Response time[msec],Count
  0,238970
  1,95
  2,53
  3,53
  4,17
  5,7
  6,3
  8,1
  9,1
  10,2
  11,1
  12,1
  13,1

log_20230331-131116_t.csvと末尾に「_t」がついたCSVファイルは、スループットの時系列データです。

.. code-block:: text

  Elapsed time[sec],Throughput[tps]
  1,3927
  2,3982
  3,3931
  ...
  58,3959
  59,3988
  60,3923

注意点として、スループットの時系列データは標準出力に出力された進捗状況のデータと一致しないことがあります。
これは負荷テストの並列性を妨げないように、進捗状況の取得においては排他制御を行っていないためです。
CSVファイルの方が正確なデータとなっていますので、結果の分析にはCSVファイルのデータを使用してください。

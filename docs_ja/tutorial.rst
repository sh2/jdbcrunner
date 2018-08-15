チュートリアル
==============

この章では、LinuxとMySQLを使用して簡単な負荷テストを行うまでの手順を説明します。

データベースの準備
------------------

MySQLサーバにrootユーザでログインして、tutorialデータベースとrunnerユーザを作成します。パスワードはお使いの環境に合わせて変更してください。 ::

  shell> mysql -u root -p
  sql> CREATE DATABASE tutorial;
  sql> CREATE USER runner@'%' IDENTIFIED BY 'change_on_install';
  sql> GRANT ALL PRIVILEGES ON tutorial.* TO runner@'%';

MySQLサーバにrunnerユーザでログインし直してsampleテーブルを作成し、テストデータをINSERTしておきます。 ::

  shell> mysql -u runner -p tutorial
  sql> CREATE TABLE sample (id INT PRIMARY KEY, data VARCHAR(10)) ENGINE = InnoDB;
  sql> INSERT INTO sample (id, data) VALUES (1, 'aaaaaaaaaa');
  sql> INSERT INTO sample (id, data) VALUES (2, 'bbbbbbbbbb');
  sql> INSERT INTO sample (id, data) VALUES (3, 'cccccccccc');
  sql> INSERT INTO sample (id, data) VALUES (4, 'dddddddddd');
  sql> INSERT INTO sample (id, data) VALUES (5, 'eeeeeeeeee');

テーブルの中身は以下のようになります。 ::

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

JdbcRunnerのJARファイルを任意のディレクトリに配置し、環境変数CLASSPATHを設定します。 ::
  
  shell> export CLASSPATH=jdbcrunner-1.3.jar

ツールの起動クラスは、パッケージなしのJRです。追加のオプションなしで実行すると、簡単な使い方が表示されます。 ::

  shell> java JR
  JdbcRunner 1.3
  スクリプトファイルが指定されていません
  
  usage: java JR <script> [options]
  -autoCommit <arg>     オートコミットモードを有効化または無効化します (デフォルト : true (有効))
  -connPoolSize <arg>   コネクションプールの物理接続数を指定します (デフォルト : nAgents)
  -debug                デバッグモードを有効にします (デフォルト : false)
  -jdbcDriver <arg>     JDBCドライバのクラス名を指定します (デフォルト : (なし))
  -jdbcPass <arg>       データベースユーザのパスワードを指定します (デフォルト : (なし))
  -jdbcUrl <arg>        JDBC接続URLを指定します (デフォルト :
                        jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicK
                        eyRetrieval=true)
  -jdbcUser <arg>       データベースのユーザ名を指定します (デフォルト : (なし))
  -logDir <arg>         ログの出力先ディレクトリを指定します (デフォルト : .)
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

JdbcRunnerでは、負荷テストのシナリオをスクリプトで定義します。以下のスクリプトをtest.jsというファイル名で作成します。 ::

  function run() {
      var param = random(1, 5);
      query("SELECT data FROM sample WHERE id = $int", param);
  }

このスクリプトは「1以上5以下の乱数を生成し、生成された値をint型としてクエリのパラメータにバインドして実行する」というファンクションを定義するものです。JdbcRunnerはrun()ファンクションで定義された処理を指定された多重度で指定された時間だけ繰り返し実行し、スループットとレスポンスタイムを出力します。

負荷テストの実行
----------------

作成したスクリプトといくつかのオプションを指定して、負荷テストを開始します。 ::

  shell> java JR test.js -jdbcUrl jdbc:mysql://localhost/tutorial -jdbcUser runner -jdbcPass change_on_install
  22:33:59 [INFO ] > JdbcRunner 1.3
  22:33:59 [INFO ] [Config]
  Program start time   : 20180815-223358
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
  Log directory        : .
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
  22:34:01 [INFO ] [Warmup] -9 sec, 1195 tps, (1195 tx)
  22:34:02 [INFO ] [Warmup] -8 sec, 1929 tps, (3124 tx)
  22:34:03 [INFO ] [Warmup] -7 sec, 2166 tps, (5290 tx)
  22:34:04 [INFO ] [Warmup] -6 sec, 2056 tps, (7346 tx)
  22:34:05 [INFO ] [Warmup] -5 sec, 2389 tps, (9735 tx)
  22:34:06 [INFO ] [Warmup] -4 sec, 2358 tps, (12093 tx)
  22:34:07 [INFO ] [Warmup] -3 sec, 2286 tps, (14379 tx)
  22:34:08 [INFO ] [Warmup] -2 sec, 2221 tps, (16600 tx)
  22:34:09 [INFO ] [Warmup] -1 sec, 2065 tps, (18665 tx)
  22:34:10 [INFO ] [Warmup] 0 sec, 2355 tps, (21020 tx)
  22:34:11 [INFO ] [Progress] 1 sec, 2203 tps, 2203 tx
  22:34:12 [INFO ] [Progress] 2 sec, 2409 tps, 4612 tx
  22:34:13 [INFO ] [Progress] 3 sec, 1912 tps, 6524 tx
  ...
  22:35:08 [INFO ] [Progress] 58 sec, 2063 tps, 118784 tx
  22:35:09 [INFO ] [Progress] 59 sec, 1779 tps, 120563 tx
  22:35:10 [INFO ] [Progress] 60 sec, 2488 tps, 123051 tx
  22:35:10 [INFO ] [Total tx count] 123051 tx
  22:35:10 [INFO ] [Throughput] 2050.9 tps
  22:35:10 [INFO ] [Response time (minimum)] 0 msec
  22:35:10 [INFO ] [Response time (50%tile)] 0 msec
  22:35:10 [INFO ] [Response time (90%tile)] 0 msec
  22:35:10 [INFO ] [Response time (95%tile)] 0 msec
  22:35:10 [INFO ] [Response time (99%tile)] 0 msec
  22:35:10 [INFO ] [Response time (maximum)] 14 msec
  22:35:10 [INFO ] < JdbcRunner SUCCESS

負荷テストを開始すると、標準出力に負荷テストの設定、進捗状況、測定結果が出力されます。同様の内容はログファイルjdbcrunner.logにも出力されます。負荷テストの設定のセクションからは、例えば以下のような情報が読み取れます。

* 測定を行う際、あらかじめ10秒間のウォームアップを行う(Warmup time)
* 60秒間の測定を行う(Measurement time)
* 多重度は1(Number of agents)

進捗状況のセクションからは、毎秒およそ2,000トランザクションが実行されていることが読み取れます。なお、ここで言うトランザクションとはスクリプトに定義されたrun()ファンクションを1回実行することを示しています。必ずしもRDBMSにとってのトランザクション数と一致するわけではない点に注意してください。

測定結果のセクションには、合計のトランザクション数、スループット、レスポンスタイムが出力されます。合計のトランザクション数からは、ウォームアップ時間に行われたトランザクションは除外されます。レスポンスタイムはrun()ファンクションを1回実行するのにかかった時間のことで、最小値、50パーセンタイル値(中央値)、90パーセンタイル値、95パーセンタイル値、99パーセンタイル値、最大値の6種類が出力されます。ここで、レスポンスタイムが0ミリ秒というのは正確には0ミリ秒以上1ミリ秒未満であることを示しています。

結果ファイルの確認
------------------

負荷テストが正常終了すると、ログファイルjdbcrunner.logの他に2つの結果ファイルが出力されます。 ::

  shell> ls -l
  -rw-rw-r-- 1 taira taira    5979  8月 15 22:35 jdbcrunner.log
  -rw-rw-r-- 1 taira taira     108  8月 15 22:35 log_20180815-223358_r.csv
  -rw-rw-r-- 1 taira taira     505  8月 15 22:35 log_20180815-223358_t.csv

log_20180815-223358_r.csvと末尾に「_r」がついたCSVファイルは、レスポンスタイムの度数分布データです。レスポンスタイムごとにトランザクション実行数が出力されます。 ::

  Response time[msec],Count
  0,122416
  1,351
  2,28
  3,7
  4,20
  5,31
  6,41
  7,42
  8,24
  9,35
  10,23
  11,15
  12,14
  13,2
  14,2

log_20180815-223358_t.csvと末尾に「_t」がついたCSVファイルは、スループットの時系列データです。 ::

  Elapsed time[sec],Throughput[tps]
  1,2203
  2,2410
  3,1910
  ...
  58,2063
  59,1779
  60,2486

注意点として、スループットの時系列データは標準出力に出力された進捗状況のデータと一致しないことがあります。これは負荷テストの並列性を妨げないように、進捗状況の取得においては排他制御を行っていないためです。CSVファイルの方が正確なデータとなっていますので、結果の分析にはCSVファイルのデータを使用してください。

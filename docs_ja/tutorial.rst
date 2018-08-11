チュートリアル
==============

この章では、MySQLを使用して簡単な負荷テストを行うまでの手順を説明します。

データベースの準備
------------------

MySQLのtestデータベースにtutorialテーブルを作成し、テストデータをINSERTします。 ::

  > mysql test
  
  mysql> CREATE TABLE tutorial (id INT PRIMARY KEY, data VARCHAR(10)) ENGINE = InnoDB;
  mysql> INSERT INTO tutorial (id, data) VALUES (1, 'aaaaaaaaaa');
  mysql> INSERT INTO tutorial (id, data) VALUES (2, 'bbbbbbbbbb');
  mysql> INSERT INTO tutorial (id, data) VALUES (3, 'cccccccccc');
  mysql> INSERT INTO tutorial (id, data) VALUES (4, 'dddddddddd');
  mysql> INSERT INTO tutorial (id, data) VALUES (5, 'eeeeeeeeee');

テーブルの中身は以下のようになります。 ::

  mysql> SELECT * FROM tutorial ORDER BY id;
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

jdbcrunner-1.2.jarを任意のディレクトリに配置し、環境変数CLASSPATHを設定します。Windowsの場合はsetコマンドで環境変数を設定することができます。 ::

  > dir
  
   C:\jdbcrunner のディレクトリ
  
  2011/10/10  22:14    <DIR>          .
  2011/10/10  22:14    <DIR>          ..
  2011/10/10  21:48         3,276,889 jdbcrunner-1.2.jar
  
  > set CLASSPATH=jdbcrunner-1.2.jar

Linuxなどでbashを使用している場合は、exportコマンドで設定します。 ::
  
  $ export CLASSPATH=jdbcrunner-1.2.jar

ツールの起動クラスはパッケージなしのJRです。追加のオプションなしで実行すると、簡単な使い方が表示されます。 ::

  > java JR
  
  JdbcRunner 1.2
  スクリプトファイルが指定されていません
  
  usage: java JR <script> [options]
  -autoCommit <arg>     オートコミットモードを有効化または無効化します (デフォルト : true (有効))
  -connPoolSize <arg>   コネクションプールの物理接続数を指定します (デフォルト : nAgents)
  -debug                デバッグモードを有効にします (デフォルト : false)
  -jdbcDriver <arg>     JDBCドライバのクラス名を指定します (デフォルト : (なし))
  -jdbcPass <arg>       データベースユーザのパスワードを指定します (デフォルト : (なし))
  -jdbcUrl <arg>        JDBC接続URLを指定します (デフォルト : jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true)
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
      query("SELECT data FROM tutorial WHERE id = $int", param);
  }

このスクリプトは「1以上5以下の乱数を生成し、生成された値をint型としてクエリのパラメータにバインドして実行する」というファンクションを定義するものです。JdbcRunnerはrun()ファンクションで定義された処理を指定された多重度で指定された時間だけ繰り返し実行し、スループットとレスポンスタイムを出力します。

負荷テストの実行
----------------

作成したスクリプトをオプションに指定して実行すると、負荷テストが開始されます。 ::

  > java JR test.js
  
  22:40:58 [INFO ] > JdbcRunner 1.2
  22:40:58 [INFO ] [Config]
  Program start time   : 20111010-224058
  Script filename      : test.js
  JDBC driver          : -
  JDBC URL             : jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
  JDBC user            :
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
  22:40:59 [INFO ] [Warmup] -9 sec, 3038 tps, (3038 tx)
  22:41:00 [INFO ] [Warmup] -8 sec, 4887 tps, (7925 tx)
  22:41:01 [INFO ] [Warmup] -7 sec, 4858 tps, (12783 tx)
  22:41:02 [INFO ] [Warmup] -6 sec, 4920 tps, (17703 tx)
  22:41:03 [INFO ] [Warmup] -5 sec, 4932 tps, (22635 tx)
  22:41:04 [INFO ] [Warmup] -4 sec, 4842 tps, (27477 tx)
  22:41:05 [INFO ] [Warmup] -3 sec, 4854 tps, (32331 tx)
  22:41:06 [INFO ] [Warmup] -2 sec, 4799 tps, (37130 tx)
  22:41:07 [INFO ] [Warmup] -1 sec, 4789 tps, (41919 tx)
  22:41:08 [INFO ] [Warmup] 0 sec, 4776 tps, (46695 tx)
  22:41:09 [INFO ] [Progress] 1 sec, 4778 tps, 4778 tx
  22:41:10 [INFO ] [Progress] 2 sec, 4795 tps, 9573 tx
  22:41:11 [INFO ] [Progress] 3 sec, 4870 tps, 14443 tx
  22:41:12 [INFO ] [Progress] 4 sec, 4823 tps, 19266 tx
  22:41:13 [INFO ] [Progress] 5 sec, 4806 tps, 24072 tx
  ...
  22:42:04 [INFO ] [Progress] 56 sec, 4691 tps, 267178 tx
  22:42:05 [INFO ] [Progress] 57 sec, 4774 tps, 271952 tx
  22:42:06 [INFO ] [Progress] 58 sec, 4771 tps, 276723 tx
  22:42:07 [INFO ] [Progress] 59 sec, 4733 tps, 281456 tx
  22:42:08 [INFO ] [Progress] 60 sec, 4704 tps, 286160 tx
  22:42:08 [INFO ] [Total tx count] 286161 tx
  22:42:08 [INFO ] [Throughput] 4769.4 tps
  22:42:08 [INFO ] [Response time (minimum)] 0 msec
  22:42:08 [INFO ] [Response time (50%tile)] 0 msec
  22:42:08 [INFO ] [Response time (90%tile)] 0 msec
  22:42:08 [INFO ] [Response time (95%tile)] 0 msec
  22:42:08 [INFO ] [Response time (99%tile)] 0 msec
  22:42:08 [INFO ] [Response time (maximum)] 11 msec
  22:42:08 [INFO ] < JdbcRunner SUCCESS

負荷テストを開始すると、標準出力に負荷テストの設定、進捗状況、測定結果が出力されます。同様の内容はログファイルjdbcrunner.logにも出力されます。負荷テストの設定のセクションからは、例えば以下のような情報が読み取れます。

* 測定を行う際、あらかじめ10秒間のウォームアップを行う(Warmup time)
* 60秒間の測定を行う(Measurement time)
* 多重度は1(Number of agents)

進捗状況のセクションからは、毎秒およそ4,700トランザクションが実行されていることが読み取れます。ここで言うトランザクションとは、スクリプトに定義されたrun()ファンクションを1回実行することです。必ずしもRDBMSにとってのトランザクション数と一致するわけではない点に注意してください。

測定結果のセクションには、合計のトランザクション数、スループット、レスポンスタイムが出力されます。合計のトランザクション数には、ウォームアップ時間に行われたトランザクションは加算されません。レスポンスタイムはrun()ファンクションを1回実行するのにかかった時間のことで、最小値、50パーセンタイル値(中央値)、90パーセンタイル値、95パーセンタイル値、99パーセンタイル値、最大値の6種類が出力されます。また、レスポンスタイムが0ミリ秒というのは正確には0ミリ秒以上1ミリ秒未満であることを示しています。

結果ファイルの確認
------------------

負荷テストが正常終了すると、ログファイルjdbcrunner.logの他に2つの結果ファイルが出力されます。 ::

  > dir
  
   C:\jdbcrunner のディレクトリ
  
  2011/10/10  22:42    <DIR>          .
  2011/10/10  22:42    <DIR>          ..
  2011/10/10  21:48         3,276,889 jdbcrunner-1.2.jar
  2011/10/10  22:42             6,115 jdbcrunner.log
  2011/10/10  22:42                76 log_20111010-224058_r.csv
  2011/10/10  22:42               566 log_20111010-224058_t.csv
  2011/10/10  22:23               116 test.js

log_20111010-224058_r.csvと末尾に「_r」がついたCSVファイルは、レスポンスタイムの度数分布データです。レスポンスタイムごとにトランザクション実行数が出力されます。 ::

  Response time[msec],Count
  0,286042
  1,48
  2,8
  3,2
  4,34
  5,20
  6,6
  11,1

log_20111010-224058_t.csvと末尾に「_t」がついたCSVファイルは、スループットの時系列データです。 ::

  Elapsed time[sec],Throughput[tps]
  1,4771
  2,4798
  3,4870
  4,4820
  5,4807
  ...
  56,4692
  57,4774
  58,4770
  59,4738
  60,4704

スループットの時系列データは、標準出力に出力された進捗状況のデータと一致しないことがあります。これは負荷テストの並列性を妨げないように、進捗状況の取得において排他制御を行っていないためです。CSVファイルの方が正確なデータとなっていますので、レポートの作成などにはCSVファイルのデータを利用してください。

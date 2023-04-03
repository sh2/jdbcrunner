設定パラメータ
==============

この章では、JdbcRunnerの動作を規定する設定パラメータについて説明します。

パラメータの一覧
----------------

設定パラメータの一覧を以下に示します。
設定パラメータはスクリプトのグローバル変数として指定できるものと、コマンドラインオプションとして指定できるものがあります。

================ ========================== ======== ====================================
グローバル変数   コマンドラインオプション   タイプ   デフォルト値
================ ========================== ======== ====================================
(なし)           -scriptCharset             文字列   (なし)
jdbcDriver       -jdbcDriver                文字列   (なし)
jdbcUrl          -jdbcUrl                   文字列   ``jdbc:mysql://localhost:3306/test``
jdbcUser         -jdbcUser                  文字列   (なし)
jdbcPass         -jdbcPass                  文字列   (なし)
isLoad           (なし)                     真偽値   false
warmupTime       -warmupTime                整数     10
measurementTime  -measurementTime           整数     60
nTxTypes         (なし)                     整数     1
nAgents          -nAgents                   整数     1
connPoolSize     -connPoolSize              整数     (nAgentsと同じ数)
stmtCacheSize    -stmtCacheSize             整数     10
isAutoCommit     -autoCommit                真偽値   true
sleepTime        -sleepTime                 整数     0
throttle         -throttle                  整数     0
isDebug          -debug                     真偽値   false
isTrace          -trace                     真偽値   false
logDir           -logDir                    文字列   .
(なし)           -param0                    整数     0
(なし)           -param1                    整数     0
(なし)           -param2                    整数     0
(なし)           -param3                    整数     0
(なし)           -param4                    整数     0
(なし)           -param5                    整数     0
(なし)           -param6                    整数     0
(なし)           -param7                    整数     0
(なし)           -param8                    整数     0
(なし)           -param9                    整数     0
================ ========================== ======== ====================================

パラメータの設定方法
--------------------

JdbcRunnerのパラメータを設定するには、以下の2つの方法があります。

* スクリプト内にグローバル変数として宣言する
* コマンドラインオプションで指定する

以下はスクリプト内でグローバル変数を宣言する例です。

.. code-block:: javascript

  var jdbcUrl = "jdbc:mysql://dbserver01:3306/scott";
  var jdbcUser = "scott";
  var jdbcPass = "tiger";
  var warmupTime = 120;
  var measurementTime = 600;
  var nAgents = 20;
  var isAutoCommit = false;
  var isDebug = true;

同じ設定をコマンドラインオプションで行うと、次のようになります。
コマンドラインオプションを指定すると、グローバル変数による設定は上書きされます。

.. code-block:: text

  shell> java JR test.js -jdbcUrl jdbc:mysql://dbserver01:3306/scott
                         -jdbcUser scott
                         -jdbcPass tiger
                         -warmupTime 120
                         -measurementTime 600
                         -nAgents 20
                         -autoCommit false
                         -debug

パラメータの説明
----------------

この節では、それぞれのパラメータについて説明します。

スクリプトの文字セット
^^^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : (なし)
* コマンドラインオプション : -scriptCharset
* タイプ : 文字列
* デフォルト値 : (なし)

スクリプトの文字セットを指定するパラメータです。
JdbcRunnerはデフォルトでOSのロケールにあわせた文字セットが使われていると仮定します。

デフォルトとは異なる文字セットを指定したい場合、例えばLinuxで作成したスクリプトをWindowsの日本語環境で動かすといった場合は以下のようにします。

.. code-block:: text

  shell> java JR test.js -scriptCharset UTF-8

逆に、Windowsの日本語環境で作成したスクリプトをLinuxで動かす場合は以下のようにします。

.. code-block:: text

  shell> java JR test.js -scriptCharset Windows-31J

JDBCドライバ
^^^^^^^^^^^^

* グローバル変数 : jdbcDriver
* コマンドラインオプション : -jdbcDriver
* タイプ : 文字列
* デフォルト値 : (なし)

JDBCドライバのクラス名を指定するパラメータです。
JDBCドライバがJDBC 4.0以上に対応している場合はこのパラメータを指定する必要はありません。
JDBCドライバがJDBC 4.0以上に対応していない場合は、テスト対象のRDBMSにあわせて設定してください。

JDBC接続URL
^^^^^^^^^^^

* グローバル変数 : jdbcUrl
* コマンドラインオプション : -jdbcUrl
* タイプ : 文字列
* デフォルト値 : ``jdbc:mysql://localhost:3306/test``

JDBC接続URLを指定するパラメータです。
デフォルトはMySQLでローカルホストのtestデータベースに接続する設定になっています。
テスト対象のRDBMSにあわせて設定してください。

データベースのユーザ名
^^^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : jdbcUser
* コマンドラインオプション : -jdbcUser
* タイプ : 文字列
* デフォルト値 : (なし)

データベースへログインするユーザ名を指定するパラメータです。
テスト対象のRDBMSにあわせて設定してください。

データベースユーザのパスワード
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : jdbcPass
* コマンドラインオプション : -jdbcPass
* タイプ : 文字列
* デフォルト値 : (なし)

データベースへログインするユーザのパスワードを指定するパラメータです。
テスト対象のRDBMSにあわせて設定してください。

ロードモード
^^^^^^^^^^^^

* グローバル変数 : isLoad
* コマンドラインオプション : (なし)
* タイプ : 真偽値
* デフォルト値 : false

テストデータ生成を指示するパラメータです。
ロードモードを有効にすると、JdbcRunnerの動作が以下のように変わります。

* warmupTimeとmeasurementTimeの指定が無視され、すべてのエージェントがsetBreak()するまで処理が繰り返される
* 進捗状況と結果ファイルは出力されなくなる

ロードモードを利用するサンプルを示します。
このサンプルではtestテーブルに対し10レコードINSERTが行われます。

.. code-block:: javascript

  var isLoad = true;
  var scaleFactor = 10;
  var counter = 0;

  function run() {
      if (++counter <= scaleFactor) {
          execute("INSERT INTO test (id, data) VALUES ($int, $string)",
              counter, "ABCDEFGHIJKLMNOPQESTUVWXYZ");
      } else {
          setBreak();
      }
  }

ウォームアップ時間
^^^^^^^^^^^^^^^^^^

* グローバル変数 : warmupTime
* コマンドラインオプション : -warmupTime
* タイプ : 整数
* デフォルト値 : 10

測定開始後、トランザクションを集計から除外する時間を指定するパラメータです。
単位は秒です。

多くのRDBMSは起動直後、メモリ上のキャッシュにデータが溜まるまでは十分な性能が出ません。
ウォームアップ時間を適切に設定することで、序盤のデータを除外できます。

測定時間
^^^^^^^^

* グローバル変数 : measurementTime
* コマンドラインオプション : -measurementTime
* タイプ : 整数
* デフォルト値 : 60

run()ファンクションを繰り返し実行して測定する時間を指定するパラメータです。
単位は秒です。

このパラメータで指定する測定時間は、ウォームアップ時間を包含していません。
ツール全体の実行時間は、ウォームアップ時間と測定時間で指定した値の合計となります。

トランザクションの種類数
^^^^^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : nTxTypes
* コマンドラインオプション : (なし)
* タイプ : 整数
* デフォルト値 : 1

負荷シナリオで実行するトランザクションの種類数を指定するパラメータです。

JdbcRunnerでは一つのスクリプト内に複数種類のトランザクションを定義して実行し、それぞれのスループットとレスポンスタイムを分計できます。
その場合、あらかじめこのパラメータでトランザクションの種類数を設定しておく必要があります。

複数種類のトランザクションを実行する場合、事前にsetTxType()ファンクションを呼び出してトランザクション番号を指示します。
setTxType()の引数には0以上nTxTypes未満の値を指定できます。
以下に例を示します。

.. code-block:: javascript

  var nTxTypes = 2;

  function run() {
      var r = random(1, 100);

      if (r <= 60) {
          setTxType(0);
          orderFunc();
      } else {
          setTxType(1);
          paymentFunc();
      }
  }

この例では60%の確率で注文処理を行い、40%の確率で支払い処理を行います。
それぞれ処理の実行前にsetTxType()を呼び出し、注文処理に0番、支払い処理に1番のトランザクション番号を割り当てています。

エージェント数
^^^^^^^^^^^^^^

* グローバル変数 : nAgents
* コマンドラインオプション : -nAgents
* タイプ : 整数
* デフォルト値 : 1

負荷シナリオを実行する多重度を指定するパラメータです。
JdbcRunnerはエージェントの数だけスレッドを立ち上げ、負荷シナリオを並列に実行します。
このパラメータを増やすほどRDBMSにかける負荷が大きくなります。

コネクションプールサイズ
^^^^^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : connPoolSize
* コマンドラインオプション : -connPoolSize
* タイプ : 整数
* デフォルト値 : (nAgentsと同じ数)

コネクションプールに保持される、RDBMSへの物理的な接続数を指定するパラメータです。
デフォルトではエージェント数と同じだけの物理接続が確保されます。

このパラメータで設定された数の物理接続が、負荷テスト開始時に確保されます。
テスト中この数は上下しません。

文キャッシュサイズ
^^^^^^^^^^^^^^^^^^

* グローバル変数 : stmtCacheSize
* コマンドラインオプション : -stmtCacheSize
* タイプ : 整数
* デフォルト値 : 10

データベースへの接続ごとに、PreparedStatementを破棄せずにキャッシュする数を指定するパラメータです。

文キャッシュが有効な場合、PreparedStatement#close()は実際にはPreparedStatementオブジェクトを破棄せず、次回同じSQL文を実行するときのためにオブジェクトを保存しておくようになります。
こうすると次の実行においてConnection#prepareStatement()を省略できるため、性能が向上します。

負荷テストにおいては、負荷シナリオで実行されるSQL文の種類数より大きな数をこのパラメータに指定しておくと最も良い性能を得られます。
ただしRDBMS側で同時にオープンできるSQL文の数に制限がある場合は、その制限値を超えないように注意してください。

オートコミットモード
^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : isAutoCommit
* コマンドラインオプション : -autoCommit
* タイプ : 真偽値
* デフォルト値 : true

オートコミットモードの有効/無効を指定するパラメータです。

スリープ時間
^^^^^^^^^^^^

* グローバル変数 : sleepTime
* コマンドラインオプション : -sleepTime
* タイプ : 整数
* デフォルト値 : 0

run()ファンクションの実行後にスリープする時間を指定するパラメータです。
単位はミリ秒です。
デフォルトの0はスリープしないことを表しています。
スリープ時間を設定することで、RDBMSに与える負荷を調節できます。

トランザクションの種類数が2以上の場合は、それぞれのトランザクション種別に対して値を指定できます。
グローバル変数の場合は配列として宣言します。

.. code-block:: javascript

  var sleepTime = new Array(100, 200);

コマンドラインオプションの場合は、カンマ区切りで指定します。

.. code-block:: text

  shell> java JR test.js -sleepTime 100,200

ここでは単一の値も指定でき、その場合はすべてのトランザクション種別で同じスリープ時間となります。

スループットの上限値
^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : throttle
* コマンドラインオプション : -throttle
* タイプ : 整数
* デフォルト値 : 0

スループットの上限値を指定するパラメータです。
単位はトランザクション/秒です。
デフォルトは0ですが、これは0トランザクション/秒ではなく、この機能を使わないことを意味します。

スリープ時間と似たパラメータですが、このパラメータを指定するとスループットの上限値を超えないように時間を計算してスリープします。
これによってRDBMSに一定の負荷をかけ続けられます。

トランザクションの種類数が2以上の場合は、それぞれのトランザクション種別に対して値を指定できます。
グローバル変数の場合は配列として宣言します。

.. code-block:: javascript

  var throttle = new Array(100, 200);

コマンドラインオプションの場合は、カンマ区切りで指定します。

.. code-block:: text

  shell> java JR test.js -throttle 100,200

ここでは単一の値も指定でき、その場合はすべてのトランザクション種別を合計したスループットが上限値を超えないように、スリープを行います。

デバッグモード
^^^^^^^^^^^^^^

* グローバル変数 : isDebug
* コマンドラインオプション : -debug
* タイプ : 真偽値
* デフォルト値 : false

デバッグログの出力を指定するパラメータです。
デフォルトはfalseで、デバッグログを出力しません。

このパラメータを有効にすると、debug()ファンクションによりログが出力されるようになります。

.. code-block:: javascript

  debug("このメッセージは、isDebug == trueのときだけ出力されます");

コマンドラインオプションで指定する場合、-debug trueと引数をつける必要はありません。
-debugのみで有効化されます。

トレースモード
^^^^^^^^^^^^^^

* グローバル変数 : isTrace
* コマンドラインオプション : -trace
* タイプ : 真偽値
* デフォルト値 : false

デバッグログよりも詳細な、トレースログの出力を指定するパラメータです。
デフォルトはfalseで、トレースログを出力しません。

このパラメータを有効にすると、trace()ファンクションによりログが出力されるようになります。
また、トレースログを有効化した場合は自動的にデバッグログも有効化されます。

.. code-block:: javascript

  trace("このメッセージは、isTrace == trueのときだけ出力されます");

トレースログを有効化すると、ログエントリにログを出力したスレッド名とメソッド名が付加されるようになります。

.. code-block:: text

  2011-10-11 00:29:51 [receiver] [jdbcrunner.Manager$Receiver#run] [Progress] 59 sec, 5060 tps, 279128 tx
  2011-10-11 00:29:52 [receiver] [jdbcrunner.Manager$Receiver#run] [Progress] 60 sec, 5045 tps, 284173 tx
  2011-10-11 00:29:52 [main] [jdbcrunner.Manager$Receiver#stop] 割り込みが発生しました
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Total tx count] 284177 tx
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Throughput] 4736.3 tps
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Response time (minimum)] 0 msec
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Response time (50%tile)] 0 msec
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Response time (90%tile)] 0 msec
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Response time (95%tile)] 0 msec
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Response time (99%tile)] 0 msec
  2011-10-11 00:29:52 [main] [jdbcrunner.Result#printLine] [Response time (maximum)] 7 msec
  2011-10-11 00:29:52 [main] [JR#main] < JdbcRunner SUCCESS

コマンドラインオプションで指定する場合、-trace trueと引数をつける必要はありません。
-traceのみで有効化されます。

ログの出力先ディレクトリ
^^^^^^^^^^^^^^^^^^^^^^^^

* グローバル変数 : logDir
* コマンドラインオプション : -logDir
* タイプ : 文字列
* デフォルト値 : .

ログファイルと結果ファイルの出力先ディレクトリを指定するパラメータです。
デフォルトはカレントディレクトリです。

変数代入パラメータ
^^^^^^^^^^^^^^^^^^

* グローバル変数 : (なし)
* コマンドラインオプション : -param0 ～ -param9
* タイプ : 整数
* デフォルト値 : 0

コマンドラインオプションからスクリプトの変数に値を代入するパラメータです。
-param0を指定するとスクリプトのparam0に指定した値が代入されます。
代入できるのは整数のみで、デフォルトは0です。

例えば、以下のようなスクリプトを作成します。

.. code-block:: javascript

  function run() {
      var id = random(1, param0);
      query("SELECT ename FROM emp WHERE empno = $int", id);
  }

すると、次のようにコマンドラインオプションで-param0を指定することにより、複数のパターンで負荷テストを行えます。

.. code-block:: text

  shell> java JR test.js -param0 100

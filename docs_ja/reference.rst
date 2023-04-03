スクリプトAPIリファレンス
=========================

この章では、JdbcRunnerが提供する独自ファンクションの使い方を説明します。

SQL発行ファンクション
---------------------

SQL発行ファンクションの共通事項
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

独自記法のSQL文
~~~~~~~~~~~~~~~

SQL発行ファンクションはいずれも1番目の引数に独自記法のSQL文をとります。
このSQL文は基本的にPreparedStatementの記法に準じますが、一つ異なる点があります。
それはPreparedStatementでプレースホルダとして用いる「?」記号の代わりに、以下の独自プレースホルダを記述するということです。

* $int : 値を整数としてバインドする場合
* $long : 値を長整数としてバインドする場合
* $double : 値を倍精度の浮動小数点数としてバインドする場合
* $string : 値を文字列としてバインドする場合
* $timestamp : 値を日付・時刻(java.sql.Timestamp)としてバインドする場合

JDBCでクエリを発行する例を以下に示します。

.. code-block:: java

  pstmt = conn.prepareStatement("SELECT ename FROM emp WHERE empno = ?");
  pstmt.setInt(1, id);
  rs = pstmt.executeQuery();
  
  while (rs.next()) {
      count++;
  }
  
  rs.close();
  pstmt.close();

これと同じ処理を、JdbcRunnerでは以下のように書きます。

.. code-block:: javascript

  var count = query("SELECT ename FROM emp WHERE empno = $int", id);

本記法においては「$」は特殊記号として扱われます。
もしSQLに「$」という文字そのものを使いたい場合は、「$$」と書いてください。
次に示すのは、PostgreSQLにおいて「ドル引用符付け」という記法を用いる例です。

.. code-block:: javascript

  query("SELECT $$tag$$Dianne's horse$$tag$$");

NULL値をバインドする場合は、JavaScriptのnullを指定してください。

.. code-block:: javascript

  var value = null;
  execute("INSERT INTO test (c1) VALUES ($int)", value);

日付・時刻のバインド
~~~~~~~~~~~~~~~~~~~~

SQL文に日付・時刻をバインドする方法は複数用意されています。

#. JavaScriptのDateオブジェクトをバインドする
#. Javaのjava.util.Dateオブジェクトをバインドする
#. 数値をバインドする
#. 文字列をバインドする

以下はJavaScriptのDateオブジェクトをバインドする例です。

.. code-block:: javascript

  var d = new Date(2010, 0, 2, 3, 4, 5); // 2010年1月2日 3時4分5秒
  query("SELECT ename FROM emp WHERE hiredate < $timestamp", d);

Javaのjava.util.Dateオブジェクトをバインドする例です。

.. code-block:: javascript

  var d = new java.util.Date(1262369045000); // 2010年1月2日 3時4分5秒
  query("SELECT ename FROM emp WHERE hiredate < $timestamp", d);

数値をバインドする例です。
数値として、1970年1月1日 0時0分0秒 GMTからの経過ミリ秒を指定します。

.. code-block:: javascript

  var d = 1262369045000; // 2010年1月2日 3時4分5秒
  query("SELECT ename FROM emp WHERE hiredate < $timestamp", d);

文字列をバインドする例です。
文字列はJDBCタイムスタンプエスケープ形式(yyyy-mm-dd hh:mm:ss[.f...])で記述します。

.. code-block:: javascript

  var d = "2010-01-02 03:04:05"; // 2010年1月2日 3時4分5秒
  query("SELECT ename FROM emp WHERE hiredate < $timestamp", d);

query(sql, param, ...)
^^^^^^^^^^^^^^^^^^^^^^

* sql : 独自記法のSQL文
* param, ... : パラメータにバインドする値
* 戻り値 : 結果セットのレコード数

RDBMSに対してクエリを発行するファンクションです。
内部的にはPreparedStatement#executeQuery()のラッパになっています。

fetchAsArray(sql, param, ...)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* sql : 独自記法のSQL文
* param, ... : パラメータにバインドする値
* 戻り値 : 結果セット

RDBMSに対してクエリを発行するファンクションです。
内部的にはPreparedStatement#executeQuery()のラッパになっています。

query()では結果セットのレコード数しか得ることができませんが、fetchAsArray()では結果セットをJavaScriptの2次元配列として得ることができます。

.. code-block:: mysql

  sql> SELECT * FROM dept ORDER BY deptno;
  +--------+------------+----------+
  | deptno | dname      | loc      |
  +--------+------------+----------+
  |     10 | accounting | new york |
  |     20 | research   | dallas   |
  |     30 | sales      | chicago  |
  |     40 | operations | boston   |
  +--------+------------+----------+
  4 rows in set (0.00 sec)

以下は、このdeptテーブルからデータを取得するサンプルスクリプトです。

.. code-block:: javascript

  var rs = fetchAsArray("SELECT * FROM dept ORDER BY deptno");
  info("rows     : " + rs.length);
  info("columns  : " + rs[0].length);
  info("row1col1 : " + rs[0][0]);
  info("row2col3 : " + rs[1][2]);

この例では次のようなログが出力されます。

.. code-block:: text

  2011-10-11 01:06:52 [INFO ] rows     : 4
  2011-10-11 01:06:52 [INFO ] columns  : 3
  2011-10-11 01:06:52 [INFO ] row1col1 : 10
  2011-10-11 01:06:52 [INFO ] row2col3 : dallas

fetchAsArray()はクライアントの負荷が大きくなってしまうため、結果セットが必要ない場合はquery()を用いるようにしてください。

execute(sql, param, ...)
^^^^^^^^^^^^^^^^^^^^^^^^

* sql : 独自記法のSQL文
* param, ... : パラメータにバインドする値
* 戻り値 : 更新されたレコード数

RDBMSに対してDMLを発行するファンクションです。
内部的にはPreparedStatement#executeUpdate()のラッパになっています。

executeBatch(sql, paramArray, ...)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* sql : 独自記法のSQL文
* paramArray, ... : パラメータにバインドする配列
* 戻り値 : 更新されたレコード数の配列

RDBMSに対してJDBCバッチ更新をするファンクションです。内部的にはPreparedStatement#addBatch()、PreparedStatement#executeBatch()のラッパになっています。

paramArrayにはJavaScriptの配列を指定します。
パラメータが複数ある場合は、それらの要素数を揃えておく必要があります。

.. code-block:: javascript

  var c1Array = new Array(1, 2, 3);
  var c2Array = new Array("Apple", "Orange", "Banana");
  executeBatch("INSERT INTO test (c1, c2) VALUES ($int, $string)", c1Array, c2Array);

この例では、3つのレコードを一度にINSERTできます。

.. code-block:: mysql

  sql> SELECT * FROM test ORDER BY c1;
  +----+--------+
  | c1 | c2     |
  +----+--------+
  |  1 | Apple  |
  |  2 | Orange |
  |  3 | Banana |
  +----+--------+
  3 rows in set (0.00 sec)

データベース操作ファンクション
------------------------------

takeConnection()
^^^^^^^^^^^^^^^^

* 戻り値 : データベースへの接続

エージェントが現在使用している、データベースへの接続を返すファンクションです。
このファンクションは、JDBCの機能を直接呼び出す際に利用します。

オートコミットモードを切り替える例を以下に示します。

.. code-block:: javascript

  var conn = takeConnection();
  conn.setAutoCommit(true);

トランザクション分離レベルを設定する例を以下に示します。

.. code-block:: javascript

  var conn = takeConnection();
  conn.setTransactionIsolation(java.sql.Connection.TRANSACTION_SERIALIZABLE)

このファンクションは新たにコネクションプールからデータベースへの接続を払い出すのではなく、現在すでに使用している接続を返すという点に注意してください。

getDatabaseProductName()
^^^^^^^^^^^^^^^^^^^^^^^^

* 戻り値 : RDBMSの製品名

RDBMSの製品名を返すファンクションです。
内部的にはDatabaseMetaData#getDatabaseProductName()のラッパになっています。

getDatabaseMajorVersion()
^^^^^^^^^^^^^^^^^^^^^^^^^

* 戻り値 : RDBMSのメジャー・バージョン

RDBMSのメジャー・バージョンを返すファンクションです。
内部的にはDatabaseMetaData#getDatabaseMajorVersion()のラッパになっています。

getDatabaseMinorVersion()
^^^^^^^^^^^^^^^^^^^^^^^^^

* 戻り値 : RDBMSのマイナー・バージョン

RDBMSのマイナー・バージョンを返すファンクションです。
内部的にはDatabaseMetaData#getDatabaseMinorVersion()のラッパになっています。

commit()
^^^^^^^^

データベースへの変更を確定するファンクションです。
このメソッドを使う場合は、オートコミットモードが無効になっている必要があります。

rollback()
^^^^^^^^^^

データベースへの変更を取り消すファンクションです。
このメソッドを使う場合は、オートコミットモードが無効になっている必要があります。

エージェント制御ファンクション
-------------------------------

getId()
^^^^^^^

* 戻り値 : エージェントの番号

エージェントの番号を返すファンクションです。
エージェント数が10の場合、このファンクションは0以上9以下の値を返します。

setBreak()
^^^^^^^^^^

run()ファンクションの停止フラグを立てるファンクションです。
このファンクションを実行すると、run()ファンクションをそれ以上繰り返さなくなります。
ロードモードと組み合わせて、指定回数だけ処理を行わせる際に利用します。

.. code-block:: javascript

  var isLoad = true;
  var counter = 0;
  
  function run() {
      if (++counter <= 10) {
          execute("INSERT INTO test (id, data) VALUES ($int, $string)",
              counter, "ABCDEFGHIJKLMNOPQESTUVWXYZ");
      } else {
          setBreak();
      }
  }

setTxType(txType)
^^^^^^^^^^^^^^^^^

* txType : トランザクション種別

トランザクション種別を設定するファンクションです。
トランザクション種類数が5の場合、このファンクションには0以上4以下の値を設定できます。

トランザクション種類数を2以上に設定してこのファンクションを用いることで、複数の処理をミックスさせた負荷テストを行い、それぞれのスループットとレスポンスタイムを分計できます。

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

ユーティリティファンクション
----------------------------

getData(key)
^^^^^^^^^^^^

* key : 関連付けされたデータが返されるキー
* 戻り値 : 指定されたキーに関連付けされているデータ

エージェント間で共有しているデータを取得するファンクションです。
内部的にはjava.util.concurrent.ConcurrentHashMap#get()のラッパになっています。

putData(key, value)
^^^^^^^^^^^^^^^^^^^

* key : 指定されたデータが関連付けされるキー
* value : 指定されたキーに関連付けされるデータ

エージェント間で共有したいデータを登録するファンクションです。
内部的にはjava.util.concurrent.ConcurrentHashMap#put()のラッパになっています。

負荷テストの初期化処理でテーブルの主キー一覧を取得し、それを各エージェントに共有させる例を以下に示します。

.. code-block:: javascript

  var emp;
  
  function init() {
      if (getId() == 0) {
          putData("emp", fetchAsArray("SELECT empno FROM emp ORDER BY empno"));
      }
  }
  
  function run() {
      if (!emp) {
          emp = getData("emp");
      }
      
      var empno = emp[random(0, emp.length - 1)][0];
      query("SELECT ename FROM emp WHERE empno = $int", empno);
  }

random(min, max)
^^^^^^^^^^^^^^^^

* min : 乱数の最小値
* max : 乱数の最大値
* 戻り値 : min以上max以下のランダムな整数

mix以上max以下のランダムな整数を返すファンクションです。
maxを含みます。

randomString(length)
^^^^^^^^^^^^^^^^^^^^

* length : 文字列の長さ
* 戻り値 : 長さlengthのランダムな文字列

長さlengthのランダムな文字列を返すファンクションです。
初期状態では英小文字、英大文字、数字の62文字を使用してランダムな文字列を生成します。
setRandomStringElements()ファンクションによって、使用する文字群を変更できます。

setRandomStringElements(elements)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* elements : 使用する文字群

randomString()ファンクションで使用する文字群を指定するファンクションです。
使用したい文字を並べた文字列で指定します。

getScriptStackTrace(object)
^^^^^^^^^^^^^^^^^^^^^^^^^^^

* object : JavaScriptの例外オブジェクト
* 戻り値 : スタックトレース、引数がJavaScriptの例外オブジェクトでない場合は空文字列

try～catch文で受け取った例外オブジェクトを引数にして、スタックトレースを取得するファンクションです。
以下に例を示します。

.. code-block:: javascript

  try {
      ...
  } catch (e) {
      warn("[Agent " + getId() + "] " + e.javaException + getScriptStackTrace(e));
      rollback();
  }

こうすると、以下のように例外の発生箇所を特定できます。

.. code-block:: text

  2011-10-10 18:37:23 [WARN ] [Agent 6] org.postgresql.util.PSQLException: ERROR: deadlock detected
    詳細l: Process 8576 waits for ShareLock on transaction 219025; blocked by process 8583.
  Process 8583 waits for ShareLock on transaction 219016; blocked by process 8576.
    ヒント: See server log for query details.
    場所: SQL statement "SELECT 1 FROM ONLY "public"."warehouse" x WHERE "w_id" OPERATOR(pg_catalog.=) $1 FOR SHARE OF x"
      at helper.js:53 (execute)
      at tpcc.js:224 (newOrder)
      at tpcc.js:95 (run)

ログ出力ファンクション
----------------------

trace(message)
^^^^^^^^^^^^^^

* message : ログメッセージ

トレースログを出力するファンクションです。
このログはトレースモードが有効な場合のみ出力されます。

debug(message)
^^^^^^^^^^^^^^

* message : ログメッセージ

デバッグログを出力するファンクションです。
このログはデバッグモードが有効な場合のみ出力されます。

info(message)
^^^^^^^^^^^^^

* message : ログメッセージ

情報ログを出力するファンクションです。

warn(message)
^^^^^^^^^^^^^

* message : ログメッセージ

警告ログを出力するファンクションです。

error(message)
^^^^^^^^^^^^^^

* message : ログメッセージ

意図的にエラーを発生させるファンクションです。
また、エラーログを出力します。

このファンクションを呼び出すと、負荷テストが異常終了します。

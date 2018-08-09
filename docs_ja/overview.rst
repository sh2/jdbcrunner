JdbcRunnerの概要
================

この章では、JdbcRunnerの概要を説明します。

JdbcRunnerとは
--------------

JdbcRunnerとは、Oracle Database、MySQL、PostgreSQLなどRDBMSを対象とした負荷テストツールです。OLTPを想定した比較的短いトランザクションを定義して多重実行し、スループットとレスポンスタイムを測定することができます。

JdbcRunnerの特長
----------------

RDBMS非依存
^^^^^^^^^^^

JdbcRunnerはJavaで実装されているため、JDBCドライバが提供されているRDBMSであれば製品を問わず使用することができます。

自由度の高い負荷シナリオ
^^^^^^^^^^^^^^^^^^^^^^^^

JdbcRunnerでは負荷テストのシナリオをスクリプトで記述します。これによって入力データの生成や条件分岐を柔軟に行うことができ、より実際のシステムに近い状況をシミュレートすることができます。スクリプト言語にはJavaScriptを採用しています。

コネクションプールを利用
^^^^^^^^^^^^^^^^^^^^^^^^

コネクションプールとして、Apache Tomcatで用いられているApache Commons DBCPを使用しています。これによってより一般的なJava EEの構成に近い挙動を再現しています。

ただし、コネクションプールを利用していないシステムとは挙動が大きく異なることになります。

パラメータのバインド機構を利用
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

JdbcRunnerからSQLを発行する際は、必ずPreparedStatementを用いパラメータをバインドするように設計しています。他の負荷テストツール、特にMySQLをターゲットにしたものはパラメータのバインド機構を利用していないものが多いのですが、それらに比べより実際のシステムに近い性能の傾向を得ることができます。

ただし、パラメータのバインド機構を利用していないシステムとは性能の傾向が大きく異なることになります。

動作要件
--------

JdbcRunnerを動作させるには、以下の環境が必要です。

* Java SE 6
* 各RDBMS用のJDBCドライバ

MySQLとPostgreSQLについてはJDBCドライバを内蔵しているため、別途JDBCドライバを用意する必要はありません。

ライセンス
----------

JdbcRunner本体はNew BSD Licenseです。

JdbcRunnerでは以下のライブラリをそれぞれのライセンスに従って利用しています。JdbcRunnerを修正・再配布する場合は、これらのライセンスに対する考慮も必要となります。

* Apache Commons CLI (Apache License 2.0)
* Apache Commons DBCP (Apache License 2.0)
* Apache Commons Pool (Apache License 2.0)
* Apache log4j (Apache License 2.0)
* Mozilla Rhino (Mozilla Public License 1.1)
* MySQL Connector/J (GNU General Public License v2 with FOSS License Exception)
* PostgreSQL JDBC Driver (New BSD License)

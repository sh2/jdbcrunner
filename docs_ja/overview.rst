JdbcRunnerの概要
================

この章では、JdbcRunnerの概要を説明します。

JdbcRunnerとは
--------------

JdbcRunnerとは、Oracle Database、MySQL、PostgreSQLなどのRDBMSを対象とした負荷テストツールです。OLTPを想定した比較的短いトランザクションを定義して多重実行し、スループットとレスポンスタイムを測定することができます。

JdbcRunnerの特長
----------------

RDBMS非依存
^^^^^^^^^^^

JdbcRunnerはJavaで実装されているため、JDBCドライバが提供されているRDBMSであれば製品を問わず使用することができます。

自由度の高いテストシナリオ
^^^^^^^^^^^^^^^^^^^^^^^^^^

JdbcRunnerでは負荷テストのシナリオをスクリプトで記述します。これによって入力データの生成や条件分岐を柔軟に行うことができ、より実際のシステムに近い状況をシミュレートすることができます。スクリプト言語にはJavaScriptを採用しています。

コネクションプールを利用
^^^^^^^^^^^^^^^^^^^^^^^^

コネクションプールとして、Apache Tomcatで用いられているApache Commons DBCPを使用しています。これによって一般的なJakarta EEの構成に近い挙動を再現しています。

ただし、コネクションプールを利用していないシステムとは挙動が大きく異なることになります。

パラメータのバインド機構を利用
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

JdbcRunnerからSQLを発行する際は、必ずPreparedStatementを用いパラメータをバインドするように設計しています。他の負荷テストツール、特にMySQLをターゲットにしたものはパラメータのバインド機構を利用していないものが多いのですが、それらに比べより実際のシステムに近い性能特性を得ることができます。

ただし、パラメータのバインド機構を利用していないシステムとは性能の傾向が大きく異なることになります。

動作要件
--------

JdbcRunnerを動作させるには、以下のソフトウェアが必要です。

* Java 17の実行環境
* 各RDBMS用のJDBCドライバ

JavaはLinuxの場合ディストリビューションに同梱されています。Windowsの場合は `Adoptium <https://adoptium.net/>`_ などからダウンロードしてください。

JDBCドライバは多くの場合RDBMSの開発元が提供しています。なおJdbcRunnerはMySQLとPostgreSQLのJDBCドライバを内蔵しているため、MySQLとPostgreSQLについては別途JDBCドライバを用意する必要はありません。

インストール方法
----------------

インストーラはありません。アーカイブファイルを任意のディレクトリに展開してください。

アンインストール方法
--------------------

アンインストーラはありません。展開されたファイルを単純に削除してください。

ライセンス
----------

JdbcRunner本体は、MITライセンスです。

JdbcRunnerでは以下のライブラリをそれぞれのライセンスに従って利用しています。JdbcRunnerを修正して再頒布する場合は、これらのライセンスも遵守する必要があります。

* Apache Commons CLI (Apache License 2.0)
* Apache Commons DBCP (Apache License 2.0)
* Apache Commons Pool (Apache License 2.0)
* Apache log4j (Apache License 2.0)
* Mozilla Rhino (Mozilla Public License 2.0)
* MySQL Connector/J (GNU General Public License v2 with the Universal FOSS Exception, Version 1.0)
* PostgreSQL JDBC Driver (二条項BSDライセンス)

作者連絡先
----------

* 平塚 貞夫
* sh2@pop01.odn.ne.jp
* https://dbstudy.info

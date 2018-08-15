# 1.3 (20xx-xx-xx)

* ライセンスを三条項BSDライセンスから二条項BSDライセンスに変更しました。([Commit](https://github.com/sh2/jdbcrunner/commit/13cecc665127f9df1bfe2def853cf7b0879ee3a3))
* 動作環境を更新しました。
    - 動作要件をJava SE 6からJava SE 8に変更しました。([Commit](https://github.com/sh2/jdbcrunner/commit/5abfbfdafc16f36605479e93f2b0c558604c09c4))
    - Commons CLIを1.2から1.4に更新しました。([Commit](https://github.com/sh2/jdbcrunner/commit/2ef3c9f264a6b539889b32c4f705e85f80696b73))
    - Commons Poolを1.5.6から1.6に更新しました。([Commit](https://github.com/sh2/jdbcrunner/commit/03befde30ade3738ed3ccb8d3b7386febf7304d9))
    - Log4jを1.2.16から1.2.17に更新しました。([Commit](https://github.com/sh2/jdbcrunner/commit/0bc1f6d1e71d00e18594adff9ec63fb2e74cc907))
    - Rhinoを1.7R3から1.7.9に更新しました。([Commit](https://github.com/sh2/jdbcrunner/commit/f95f8959b53789853a820ca0c376bdf833adfd69))
* ヒストグラム作成シートをLibreOffice版からExcel版に変更しました。([Commit](https://github.com/sh2/jdbcrunner/commit/464506904a3ae02d640f1ff16a575410b2c26c88))
* getDatabaseMajorVersion()、getDatabaseMinorVersion()ファンクションを追加しました。([Commit](https://github.com/sh2/jdbcrunner/commit/c3e20610330038655a113ede4157447cce0e72ae))
* ランダム文字列を生成するためのrandomString()、setRandomStringElements()ファンクションを追加しました。([Commit](https://github.com/sh2/jdbcrunner/commit/b4dbdffaf6fb8eca8770cd990dd000210b3f99c7))
* TPC-CのデータロードをrandomString()ファンクションを用いて高速化しました。([Commit](https://github.com/sh2/jdbcrunner/commit/b4dbdffaf6fb8eca8770cd990dd000210b3f99c7))
* TPC-C MySQLでのデータロード手順を見直し、高速化しました。([Commit](https://github.com/sh2/jdbcrunner/commit/cf2d07cdc652fc6a1689f804e6c53dbb1926f244))([Commit](https://github.com/sh2/jdbcrunner/commit/4100086ca6ae9ae3d41bfa630ef1350a6882e08e))
* TPC-C PaymentトランザクションがPostgreSQLでデッドロックを起こしていたのを軽減しました。([Commit](https://github.com/sh2/jdbcrunner/commit/5668990d0813c3b2be84208a0cc5c50e7496274f))(by [myzkyy](https://github.com/myzkyy))
* TPC-C DeliveryトランザクションのSELECT FOR UPDATE文においてサブクエリを使わないように修正し、より多くのRDBMSで動作するようにしました。([Commit](https://github.com/sh2/jdbcrunner/commit/660226be7e7e2af4dd8d4a605fa9b0db242059b7))
* TPC-C customerテーブルのc_since列に、現在時刻ではなくデータロード開始時刻を入れるように修正しました。([Commit](https://github.com/sh2/jdbcrunner/commit/a48a95d87c008e1454ccb601278b0a6679e4d743))
* IBM Db2用のサンプルスクリプトを追加しました。([Commit](https://github.com/sh2/jdbcrunner/commit/a5076a694cc47a7ec8d5863f1b0ee07e50033cd9))(by [nakunaru](https://github.com/nakunaru))
* Oracle Databaseでテスト終了時にユーザ定義エラーが発生することがあったのを修正しました。([Commit](https://github.com/sh2/jdbcrunner/commit/fa4d5df39888e870367ba69546de9978748ee081))([Commit](https://github.com/sh2/jdbcrunner/commit/5b8714814c0e5501ad25dcccfb35915e94170e23))

# 1.2 (2011-10-11)

* The option -throttle checks busyness of transactions. When throughput is lower than the configured value, JdbcRunner gives up keeping throughput in total and tries to keep it from now on.
* The throughput is calculated down to the first decimal place.
* Tiny TPC-C was updated to version 1.1.
    - JdbcRunner executes transactions in more precise ratio.
    - An USE_NL hint was added to SQL S-01 for Oracle Database.
* A Calc file was added to draw response time histogram.
* Apache Commons Pool was updated to version 1.5.6.
* Apache log4j was updated to version 1.2.16.
* Mozilla Rhino was updated to version 1.7R3.
* MySQL Connector/J was updated to version 5.1.18.
* PostgreSQL JDBC Driver was updated to version 9.1 Build 901.

# 1.1 (2010-05-06)

* Long and double data types were supported for bind variables.
* Null value was supported for bind variables.
* A new function getScriptStackTrace() was added.
* A new test kit Tiny SysBench was added.
* Tiny TPC-B was updated to version 1.1.
    - Foreign key constraints were added.
* A new test kit Tiny TPC-C was added.
* Apache Commons DBCP was updated to version 1.4. DBCP 1.4 now supports JDBC 4.0.
* MySQL Connector/J was updated to version 5.1.12.
* The default value of jdbcDriver was changed to empty string. JDBC 4.0 drivers no longer need this parameter.

# 1.0 (2010-01-10)

* First release.

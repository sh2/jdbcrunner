# 1.3 (2018-xx-xx)

* ヒストグラム作成シートをLibreOffice版からExcel版に変更しました。(https://github.com/sh2/jdbcrunner/commit/464506904a3ae02d640f1ff16a575410b2c26c88)
* TPC-C MySQLでのデータロード手順を見直し、高速化しました。(https://github.com/sh2/jdbcrunner/commit/cf2d07cdc652fc6a1689f804e6c53dbb1926f244)(https://github.com/sh2/jdbcrunner/commit/4100086ca6ae9ae3d41bfa630ef1350a6882e08e)
* ランダム文字列を生成するrandomString()ファンクションを追加しました。(https://github.com/sh2/jdbcrunner/commit/b4dbdffaf6fb8eca8770cd990dd000210b3f99c7)
* TPC-CのデータロードをrandomString()ファンクションを用いて高速化しました。(https://github.com/sh2/jdbcrunner/commit/b4dbdffaf6fb8eca8770cd990dd000210b3f99c7)
* TPC-C DeliveryトランザクションのSELECT FOR UPDATE文においてサブクエリを使わないように修正し、より多くのRDBMSで動作するようにしました。(https://github.com/sh2/jdbcrunner/commit/660226be7e7e2af4dd8d4a605fa9b0db242059b7)
* IBM Db2用のサンプルスクリプトを追加しました。(https://github.com/sh2/jdbcrunner/commit/a5076a694cc47a7ec8d5863f1b0ee07e50033cd9)
* 動作環境の更新
    - 動作要件をJava SE 6からJava SE 8に変更しました。
    - Commons CLIを1.2から1.4に更新しました。
    - Commons Poolを1.5.6から1.6に更新しました。
    - Log4jを1.2.16から1.2.17に更新しました。
    - Rhinoを1.7R3から1.7.9に更新しました。
* 不具合の修正
    - Oracle Databaseでテスト終了時にユーザ定義エラーが発生することがあったのを修正しました。(https://github.com/sh2/jdbcrunner/commit/fa4d5df39888e870367ba69546de9978748ee081)(https://github.com/sh2/jdbcrunner/commit/5b8714814c0e5501ad25dcccfb35915e94170e23)
    - PostgreSQLでTPC-CのPaymentトランザクションがデッドロックを起こしていたのを軽減しました。(https://github.com/sh2/jdbcrunner/commit/5668990d0813c3b2be84208a0cc5c50e7496274f)

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

/*
 * サンプル 01
 * MySQLで基本的な処理を行うサンプルです。
 * 
 * このサンプルでは、以下のパラメータを取り扱っています。
 * jdbcDriver
 * jdbcUrl
 * jdbcUser
 * jdbcPass
 * 
 * このサンプルでは、以下の独自関数を取り扱っています。
 * getId()
 * execute(sql, parameter...)
 * random(min, max)
 * query(sql, parameter...)
 */

// JdbcRunner settings -----------------------------------------------

// JDBCドライバのクラス名を指定します。
// 起動時に-jdbcDriverオプションを指定すると上書きすることができます。
// JDBCドライバがJDBC 4.0以上に対応している場合は、指定不要です。
var jdbcDriver = "com.mysql.cj.jdbc.Driver";

// JDBC URLを指定します。
// 起動時に-jdbcUrlオプションを指定すると上書きすることができます。
// MySQLの場合、JDBC URLの書式は
// 「jdbc:mysql://<server_host>:<port_number>/<database_name>」です。
var jdbcUrl = "jdbc:mysql://localhost:3306/test";

// データベースのユーザ名を指定します。
// 起動時に-jdbcUserオプションを指定すると上書きすることができます。
var jdbcUser = "";

// データベースユーザのパスワードを指定します。
// 起動時に-jdbcPassオプションを指定すると上書きすることができます。
var jdbcPass = "";

// JdbcRunner functions ----------------------------------------------

// init()には測定開始前に1回だけ実行する処理を定義します。
function init() {
    
    // init()はすべてのエージェントが1回ずつ実行します。
    // そのため、テーブル作成など全体で一度しか行わない処理は
    // 1エージェントのみが実行するように制御する必要があります。
    // getId()はエージェントの番号を返す独自関数です。
    // エージェント数が10の場合、getId()は0～9の値を返します。
    if (getId() == 0) {
        
        // テーブルを削除します。
        execute("DROP TABLE IF EXISTS sample01");
        
        // テーブルを作成します。
        execute("CREATE TABLE sample01 ("
            + "id INT PRIMARY KEY, "
            + "data VARCHAR(100)) "
            + "ENGINE = MyISAM CHARACTER SET = utf8");
        
        // テーブルにテストデータを投入します。
        for (var id = 1; id <= 100; id++) {
            
            // execute(sql, parameter...)はINSERT、UPDATE、DELETE文などを実行する独自関数です。
            // 内部的にはPreparedStatement#executeUpdate()のラッパーになっています。
            // 一つ目の引数にSQLを指定し、二つ目以降の引数にはパラメータを指定します。
            // このとき、動的型付け言語のJavaScriptから静的型付け言語のJavaに変数を
            // 渡すため、PreparedStatementのプレースホルダ「?」の代わりに
            // 「$int、$long、$double、$string、$timestamp」を用いて型名を明示します。
            // 戻り値は更新されたレコード数です。
            execute("INSERT INTO sample01 (id, data) VALUES ($int, $string)",
                id, "abcdefghijklmnopqrstuvwxyz");
        }
    }
}

// run()には測定対象の処理を定義します。
// 測定中、run()は繰り返し実行されます。
function run() {
    
    // random(min, max)はmin以上max以下の乱数を返す独自関数です。
    // maxを含みます。
    var id = random(1, 100);
    
    // query(sql, parameter...)はSELECT文を実行する独自関数です。
    // 内部的にはPreparedStatement#executeQuery()のラッパーになっています。
    // 使い方はexecute(sql, parameter...)と同じです。
    // 戻り値は取得されたレコード数です。中身のデータは取り出せません。
    query("SELECT id, data FROM sample01 WHERE id = $int", id);
}

// fin()には測定終了後に1回だけ実行する処理を定義します。
function fin() {
    if (getId() == 0) {
        execute("DROP TABLE sample01");
    }
}

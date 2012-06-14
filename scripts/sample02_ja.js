/*
 * サンプル 02
 * OracleでempテーブルをSELECTするサンプルです。
 * 
 * このサンプルを動かすためには、あらかじめ
 * $ORACLE_HOME/rdbms/admin/utlsampl.sql
 * を実行してscottスキーマを作成しておく必要があります。
 * 
 * このサンプルでは、以下のパラメータを取り扱っています。
 * warmupTime
 * measurementTime
 * nAgents
 * 
 * このサンプルでは、以下の独自関数を取り扱っています。
 * fetchAsArray(sql, parameter...)
 * putData(key, value)
 * info(message)
 * getData(key)
 */

// JdbcRunner settings -----------------------------------------------

// Oracleの場合、JDBC URLの書式は
// 「jdbc:oracle:thin://@<server_name>:<port_number>/<service_name>」です。
var jdbcUrl = "jdbc:oracle:thin://@localhost:1521/ORCL";

var jdbcUser = "scott";
var jdbcPass = "tiger";

// 測定前にあらかじめ負荷をかけておく時間を秒単位で指定します。
// この例では、run()を繰り返し実行し始めてから10秒間は処理数をカウントしません。
var warmupTime = 10;

// 測定時間を秒単位で指定します。
// warmupTimeが過ぎたあと、このパラメータで指定した時間だけ
// run()が繰り返し実行され、処理数としてカウントされます。
// 負荷をかける時間の合計はwarmupTime＋measurementTimeとなります。
var measurementTime = 60;

// 起動するエージェントの数を指定します。
// 起動時に-nAgentsオプションを指定すると上書きすることができます。
// エージェント数はスレッド数とほぼ同義で、処理の多重度を表します。
var nAgents = 4;

// Application settings ----------------------------------------------

// スクリプト内でグローバル変数を定義して利用することができます。
// ただし、グローバル変数はエージェント内に閉じており、
// 他のエージェントとは共有していません。
var empnoArray;

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        
        // fetchAsArray(sql, parameter...)はSELECT文を実行する独自関数です。
        // fetchAsArray()はSELECT文の結果を二次元配列で返します。
        // この例の場合、temp[2][1]は3レコード目の2カラム目(ename列)を表します。
        // fetchAsArray()はquery()よりもクライアント側の負荷が高いため、
        // SELECT文の結果が必要な場合にのみ利用するようにしてください。
        var temp = fetchAsArray("SELECT empno, ename FROM emp ORDER BY empno");
        
        // putData(key, value)はエージェント間で共有するデータを登録する独自関数です。
        // 内部的にはConcurrentHashMap#put()のラッパーになっています。
        // このツールでは各エージェントは独立した変数スコープを持っているため、
        // たとえグローバル変数でも、代入した値を他のエージェントから見ることはできません。
        // (例えばempnoArray = tempとしても、他のエージェントには従業員データが見えません)
        // あるエージェントで取得したデータを他のエージェントに見せたい場合は、
        // このputData(key, value)とgetData(key)を使用してください。
        putData("Employees", temp);
        
        for (var i = 0; i < temp.length; i++) {
            
            // info(message)はログにmessageを出力する独自関数です。
            // 内部的にはlog4jのLogger#info()のラッパーになっています。
            // trace()、debug()、info()、warn()、error()の5つが利用可能です。
            // traceログは-traceオプションを指定した場合、
            // debugログは-traceまたは-debugオプションを指定した場合に出力されます。
            // errorを呼び出した場合、本ツールは即座に異常終了します。
            info("Employee : " + temp[i][0] + "," + temp[i][1]);
        }
    }
}

function run() {
    if (!empnoArray) {
        
        // getData(key)はエージェント間で共有したデータを取得する独自関数です。
        // 内部的にはConcurrentHashMap#get()のラッパーになっています。
        // 注意点として、この例のようにArrayを取り出す場合は特に問題ありませんが、
        // 数値を取り出す場合はNumber(getData("SomeNumber"))と型変換する必要があります。
        empnoArray = getData("Employees");
    }
    
    var empno = empnoArray[random(0, empnoArray.length - 1)][0];
    query("SELECT ename FROM emp WHERE empno = $int", empno);
}

// fin()は必要なければ書かなくても問題ありません。init()も省略できます。
// ほとんど意味はありませんが、run()を省略することも可能です。

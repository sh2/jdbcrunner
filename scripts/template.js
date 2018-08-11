/*
 * JdbcRunner script template
 */

// JdbcRunner settings -----------------------------------------------

// Oracle Database
// var jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/ORCL";

// MySQL
var jdbcUrl = "jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true";

// PostgreSQL
// var jdbcUrl = "jdbc:postgresql://localhost:5432/postgres";

var jdbcDriver = "";
var jdbcUser = "";
var jdbcPass = "";
var isLoad = false;
var warmupTime = 10;
var measurementTime = 60;
var nTxTypes = 1;
var nAgents = 1;
var connPoolSize = nAgents;
var stmtCacheSize = 10;
var isAutoCommit = true;
var sleepTime = 0;
var throttle = 0;
var isDebug = false;
var isTrace = false;
var logDir = ".";

// Application settings ----------------------------------------------

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        // This block is performed only by Agent 0.
    }
}

function run() {
}

function fin() {
    if (getId() == 0) {
        // This block is performed only by Agent 0.
    }
}

// Application functions ---------------------------------------------

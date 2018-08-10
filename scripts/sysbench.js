/*
 * Tiny SysBench
 * This script is based on SysBench 0.4.12.
 * http://sysbench.sourceforge.net/
 */

// JdbcRunner settings -----------------------------------------------

// Oracle Database
// var jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/SBTEST";

// MySQL
var jdbcUrl = "jdbc:mysql://localhost:3306/sbtest";

// PostgreSQL
// var jdbcUrl = "jdbc:postgresql://localhost:5432/sbtest";

var jdbcUser = "sbtest";
var jdbcPass = "sbtest";
var warmupTime = 60;
var measurementTime = 180;
var nAgents = 16;
var stmtCacheSize = 20;
var isAutoCommit = false;
var logDir = "logs";

// Application settings ----------------------------------------------

var DIST_UNIFORM = 1;
var DIST_GAUSSIAN = 2;
var DIST_SPECIAL = 3;

// Number of records in the test table
var oltpTableSize;

// Ratio of queries in a transaction
var oltpPointSelects = 10;
var oltpSimpleRanges = 1;
var oltpSumRanges = 1;
var oltpOrderRanges = 1;
var oltpDistinctRanges = 1;
var oltpIndexUpdates = 1;
var oltpNonIndexUpdates = 1;

// Read-only flag
var oltpReadOnly = false;

// Range size for range queries
var oltpRangeSize = 100;

// Parameters for random numbers distribution
var oltpDistType = DIST_SPECIAL;
var oltpDistIter = 12;
var oltpDistPct = 1;
var oltpDistRes = 75;

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        info("Tiny SysBench");
        
        putData("OltpTableSize", fetchAsArray("SELECT COUNT(*) FROM sbtest")[0][0]);
        info("Number of records : " + Number(getData("OltpTableSize")));
    }
}

function run() {
    if (!oltpTableSize) {
        oltpTableSize = Number(getData("OltpTableSize"));
    }
    
    oltpExecuteRequest();
}

// Application functions ---------------------------------------------

function oltpExecuteRequest() {
    var retry = false;
    
    do {
        retry = false;
        
        try {
            // Point selects
            for (var count = 0; count < oltpPointSelects; count++) {
                var id = getRandomId();
                
                query("SELECT c FROM sbtest WHERE id = $int", id);
            }
            
            // Simple ranges
            for (var count = 0; count < oltpSimpleRanges; count++) {
                var from = getRangeRandomId();
                var to = from + oltpRangeSize - 1;
                
                query("SELECT c FROM sbtest WHERE id BETWEEN $int AND $int", from, to);
            }
            
            // Sum ranges
            for (var count = 0; count < oltpSumRanges; count++) {
                var from = getRangeRandomId();
                var to = from + oltpRangeSize - 1;
                
                query("SELECT SUM(k) FROM sbtest WHERE id BETWEEN $int AND $int", from, to);
            }
            
            // Order ranges
            for (var count = 0; count < oltpOrderRanges; count++) {
                var from = getRangeRandomId();
                var to = from + oltpRangeSize - 1;
                
                query("SELECT c FROM sbtest WHERE id BETWEEN $int AND $int ORDER BY c", from, to);
            }
            
            // Distinct ranges
            for (var count = 0; count < oltpDistinctRanges; count++) {
                var from = getRangeRandomId();
                var to = from + oltpRangeSize - 1;
                
                query("SELECT DISTINCT c FROM sbtest WHERE id BETWEEN $int AND $int ORDER BY c",
                    from, to);
            }
            
            if (!oltpReadOnly) {
                // Index updates
                for (var count = 0; count < oltpIndexUpdates; count++) {
                    var id = getRandomId();
                    
                    execute("UPDATE sbtest SET k = k + 1 WHERE id = $int", id);
                }
                
                // Non index updates
                for (var count = 0; count < oltpNonIndexUpdates; count++) {
                    var c = getRandomString();
                    var id = getRandomId();
                    
                    execute("UPDATE sbtest SET c = $string WHERE id = $int", c, id);
                }
                
                // Delete and insert
                var deletedCount = 0;
                var id = getRandomId();
                
                do {
                    // PostgreSQL may fail to delete the row.
                    // We will retry it if such a situation occurs.
                    deletedCount = execute("DELETE FROM sbtest WHERE id = $int", id);
                } while (deletedCount == 0);
                
                execute("INSERT INTO sbtest (id, k, c, pad) VALUES ($int, 0, ' ', "
                    + "'aaaaaaaaaaffffffffffrrrrrrrrrreeeeeeeeeeyyyyyyyyyy')", id);
            }
            
            // Commit
            commit();
        } catch (e) {
            if (isDeadlock(e)) {
                warn("[Agent " + getId() + "] Deadlock detected.");
                rollback();
                retry = true;
            } else {
                error(e + getScriptStackTrace(e));
            }
        }
    } while (retry);
}

function getRandomString() {
    return "" + sbRnd() + "-" + sbRnd() + "-" + sbRnd() + "-" + sbRnd() + "-" + sbRnd()
        + "-" + sbRnd() + "-" + sbRnd() + "-" + sbRnd() + "-" + sbRnd() + "-" + sbRnd();
}

function getRangeRandomId() {
    var id = getRandomId();
    
    if (id + oltpRangeSize > oltpTableSize) {
        id = oltpTableSize - oltpRangeSize;
    }
    
    if (id < 1) {
        id = 1;
    }
    
    return id;
}

function getRandomId() {
    var r = 0;
    
    switch (oltpDistType) {
        case DIST_UNIFORM:
            r = rndFuncUniform();
            break;
        case DIST_GAUSSIAN:
            r = rndFuncGaussian();
            break;
        case DIST_SPECIAL:
            r = rndFuncSpecial();
            break;
        default:
            r = 0;
    }
    
    return r;
}

function rndFuncUniform() {
    return 1 + sbRnd() % oltpTableSize;
}

function rndFuncGaussian() {
    var sum = 0;
    
    for (var i = 0; i < oltpDistIter; i++) {
        sum += 1 + sbRnd() % oltpTableSize;
    }
    
    return Math.floor(sum / oltpDistIter);
}

function rndFuncSpecial() {
    var sum = 0;
    var d = 0;
    var res = 0;
    var rangeSize = 0;
    
    if (oltpTableSize == 0) {
        return 0;
    }
    
    rangeSize = oltpTableSize * Math.floor(100 / (100 - oltpDistRes));
    res = 1 + sbRnd() % rangeSize;
    
    if (res <= oltpTableSize) {
        for (var i = 0; i < oltpDistIter; i++) {
            sum += 1 + sbRnd() % oltpTableSize;
        }
        
        return Math.floor(sum / oltpDistIter);
    }
    
    d = Math.floor(oltpTableSize * oltpDistPct / 100);
    
    if (d < 1) {
        d = 1;
    }
    
    res %= d;
    res += Math.floor(oltpTableSize / 2) - Math.floor(oltpTableSize * oltpDistPct / 200);
    
    return res;
}

function sbRnd() {
    return random(0, 1073741822);
}

function isDeadlock(exception) {
    var javaException = exception.javaException;
    
    if (javaException instanceof java.sql.SQLException) {
        if (getDatabaseProductName() == "Oracle"
            && javaException.getErrorCode() == 60) {
            return true;
        } else if (getDatabaseProductName() == "MySQL"
            && javaException.getErrorCode() == 1213) {
            return true;
        } else if (getDatabaseProductName() == "PostgreSQL"
            && javaException.getSQLState() == "40P01") {
            return true;
        } else {
            return false;
        }
    } else {
        return false;
    }
}

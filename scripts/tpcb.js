/*
 * Tiny TPC-B
 * This script is based on TPC-B Standard Specification 2.0.
 * http://tpc.org/tpcb/
 */

// JdbcRunner settings -----------------------------------------------

// Oracle Database
// var jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/orcl.local";

// MySQL
var jdbcUrl = "jdbc:mysql://localhost:3306/tpcb";

// PostgreSQL
// var jdbcUrl = "jdbc:postgresql://localhost:5432/tpcb";

var jdbcUser = "tpcb";
var jdbcPass = "tpcb";
var warmupTime = 60;
var measurementTime = 180;
var nAgents = 16;
var isAutoCommit = false;
var logDir = "logs";

// Application settings ----------------------------------------------

var TID_SCALE = 10;
var AID_SCALE = 100000;
var FILLER = "aaaaaaaaaaaaaaaaaaaaaa";

var scale;

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        info("Tiny TPC-B");
        
        putData("ScaleFactor", fetchAsArray("SELECT COUNT(*) FROM branches")[0][0]);
        info("Scale factor : " + Number(getData("ScaleFactor")));
        
        info("Truncating history table...");
        execute("TRUNCATE TABLE history");
        commit();
    }
}

function run() {
    if (!scale) {
        scale = Number(getData("ScaleFactor"));
    }
    
    var tid = random(1, TID_SCALE * scale);
    var bid = Math.floor((tid - 1) / TID_SCALE) + 1;
    var aid = 0;
    
    if (scale == 1 || random(1, 100) <= 85) {
        aid = random(AID_SCALE * (bid - 1) + 1, AID_SCALE * bid);
    } else {
        aid = random(1, AID_SCALE * (scale - 1));
        if (aid > AID_SCALE * (bid - 1)) {
            aid += AID_SCALE;
        }
    }
    
    var delta = random(-999999, 999999);
    
    execute("UPDATE accounts SET abalance = abalance + $int WHERE aid = $int",
        delta, aid);
    
    query("SELECT abalance FROM accounts WHERE aid = $int", aid);
    
    execute("UPDATE tellers SET tbalance = tbalance + $int WHERE tid = $int",
        delta, tid);
    
    execute("UPDATE branches SET bbalance = bbalance + $int WHERE bid = $int",
        delta, bid);
    
    execute("INSERT INTO history (tid, bid, aid, delta, mtime, filler) "
        + "VALUES ($int, $int, $int, $int, $timestamp, $string)",
        tid, bid, aid, delta, new Date(), FILLER);
    
    commit();
}

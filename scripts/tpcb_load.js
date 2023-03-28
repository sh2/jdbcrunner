/*
 * Tiny TPC-B - data loader
 * This script is based on TPC-B Standard Specification 2.0.
 * http://tpc.org/tpcb/
 *
 * [Oracle Database]
 * shell> sqlplus "/ AS SYSDBA"
 * sql> CREATE USER tpcb IDENTIFIED BY tpcb;
 * sql> GRANT CREATE SESSION, CREATE TABLE, UNLIMITED TABLESPACE TO tpcb;
 *
 * [MySQL]
 * shell> mysql -u root -p
 * sql> CREATE DATABASE tpcb;
 * sql> CREATE USER tpcb@'%' IDENTIFIED BY 'tpcb';
 * sql> GRANT ALL PRIVILEGES ON tpcb.* TO tpcb@'%';
 *
 * [PostgreSQL]
 * shell> psql -U postgres
 * sql> CREATE DATABASE tpcb TEMPLATE template0 ENCODING 'UTF-8' LC_COLLATE 'C' LC_CTYPE 'C';
 * sql> CREATE USER tpcb PASSWORD 'tpcb';
 *
 * <postgresql.conf>
 * listen_addresses = '*'
 * port = 5432
 *
 * <pg_hba.conf>
 * host all all 0.0.0.0/0 md5
 */

// JdbcRunner settings -----------------------------------------------

// Oracle Database
// var jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/orcl.local";

// MySQL
var jdbcUrl = "jdbc:mysql://localhost:3306/tpcb?rewriteBatchedStatements=true";

// PostgreSQL
// var jdbcUrl = "jdbc:postgresql://localhost:5432/tpcb";

var jdbcUser = "tpcb";
var jdbcPass = "tpcb";
var isLoad = true;
var nAgents = 4;
var isAutoCommit = false;
var logDir = "logs";

// Application settings ----------------------------------------------

var BATCH_SIZE = 100;

var TID_SCALE = 10;
var AID_SCALE = 100000;

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        var scale = param0;
        var taskQueue = new java.util.concurrent.LinkedBlockingQueue();
        
        info("Tiny TPC-B - data loader");
        info("-param0  : Scale factor (default : 16)");
        info("-nAgents : Parallel loading degree (default : 4)");
        
        if (scale == 0) {
            scale = 16;
        }
        
        info("Scale factor            : " + scale);
        info("Parallel loading degree : " + nAgents);
        
        for (var branchId = 1; branchId <= scale; branchId++) {
            taskQueue.offer(branchId);
        }
        
        putData("TaskQueue", taskQueue);
        
        if (getDatabaseProductName() == "Oracle") {
            dropTable();
            createTableOracle();
        } else if (getDatabaseProductName() == "MySQL") {
            dropTable();
            createTableMySQL();
        } else if (getDatabaseProductName() == "PostgreSQL") {
            dropTable();
            createTablePostgreSQL();
        } else {
            error(getDatabaseProductName() + " is not supported yet.");
        }
        
        commit();
    }
}

function run() {
    var branchId = Number(getData("TaskQueue").poll());
    
    if (branchId != 0) {
        info("Loading branch id " + branchId + " by agent " + getId() + " ...");
        
        if (getDatabaseProductName() == "MySQL") {
            execute("SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0");
        }
        
        loadBranches(branchId);
        loadTellers(branchId);
        loadAccounts(branchId);
        commit();
        
        if (getDatabaseProductName() == "MySQL") {
            execute("SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS");
        }
    } else {
        setBreak();
    }
}

function fin() {
    if (getId() == 0) {
        if (getDatabaseProductName() == "Oracle") {
            createIndexOracle();
            createForeignKeyOracle();
            gatherStatsOracle();
        } else if (getDatabaseProductName() == "MySQL") {
            gatherStatsMySQL();
        } else if (getDatabaseProductName() == "PostgreSQL") {
            createIndexPostgreSQL();
            createForeignKeyPostgreSQL();
            gatherStatsPostgreSQL();
        } else {
            error(getDatabaseProductName() + " is not supported yet.");
        }
        
        commit();
        info("Completed.");
    }
}

// Application functions ---------------------------------------------

function dropTable() {
    info("Dropping tables ...");
    
    dropTableByName("history");
    dropTableByName("accounts");
    dropTableByName("tellers");
    dropTableByName("branches");
}

function dropTableByName(tableName) {
    try {
        execute("DROP TABLE " + tableName);
    } catch (e) {
        warn(e);
        rollback(); // PostgreSQL requires a rollback.
    }
}

function createTableOracle() {
    info("Creating tables ...");
    
    execute("CREATE TABLE branches ("
        + "bid NUMBER, "
        + "bbalance NUMBER, "
        + "filler CHAR(88))");
    
    execute("CREATE TABLE tellers ("
        + "tid NUMBER, "
        + "bid NUMBER, "
        + "tbalance NUMBER, "
        + "filler CHAR(84))");
    
    execute("CREATE TABLE accounts ("
        + "aid NUMBER, "
        + "bid NUMBER, "
        + "abalance NUMBER, "
        + "filler CHAR(84))");
    
    execute("CREATE TABLE history ("
        + "tid NUMBER, "
        + "bid NUMBER, "
        + "aid NUMBER, "
        + "delta NUMBER, "
        + "mtime TIMESTAMP, "
        + "filler CHAR(22))");
}

function createTableMySQL() {
    info("Creating tables ...");
    
    execute("CREATE TABLE branches ("
        + "bid INT PRIMARY KEY, "
        + "bbalance INT, "
        + "filler CHAR(88)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE tellers ("
        + "tid INT PRIMARY KEY, "
        + "bid INT, "
        + "tbalance INT, "
        + "filler CHAR(84), "
        + "CONSTRAINT tellers_fk1 "
            + "FOREIGN KEY (bid) REFERENCES branches (bid)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE accounts ("
        + "aid INT PRIMARY KEY, "
        + "bid INT, "
        + "abalance INT, "
        + "filler CHAR(84), "
        + "CONSTRAINT accounts_fk1 "
            + "FOREIGN KEY (bid) REFERENCES branches (bid)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE history ("
        + "id INT PRIMARY KEY AUTO_INCREMENT, " // A surrogate key for ascending inserts.
        + "tid INT, "
        + "bid INT, "
        + "aid INT, "
        + "delta INT, "
        + "mtime DATETIME, "
        + "filler CHAR(22), "
        + "CONSTRAINT history_fk1 "
            + "FOREIGN KEY (tid) REFERENCES tellers (tid), "
        + "CONSTRAINT history_fk2 "
            + "FOREIGN KEY (bid) REFERENCES branches (bid), "
        + "CONSTRAINT history_fk3 "
            + "FOREIGN KEY (aid) REFERENCES accounts (aid)) "
        + "ENGINE = InnoDB");
}

function createTablePostgreSQL() {
    info("Creating tables ...");
    
    execute("CREATE TABLE branches ("
        + "bid INTEGER, "
        + "bbalance INTEGER, "
        + "filler CHAR(88))");
    
    execute("CREATE TABLE tellers ("
        + "tid INTEGER, "
        + "bid INTEGER, "
        + "tbalance INTEGER, "
        + "filler CHAR(84))");
    
    execute("CREATE TABLE accounts ("
        + "aid INTEGER, "
        + "bid INTEGER, "
        + "abalance INTEGER, "
        + "filler CHAR(84))");
    
    execute("CREATE TABLE history ("
        + "tid INTEGER, "
        + "bid INTEGER, "
        + "aid INTEGER, "
        + "delta INTEGER, "
        + "mtime TIMESTAMP, "
        + "filler CHAR(22))");
}

function createIndexOracle() {
    info("Creating indexes ...");
    
    execute("ALTER TABLE branches ADD CONSTRAINT branches_pk PRIMARY KEY (bid)");
    execute("ALTER TABLE tellers ADD CONSTRAINT tellers_pk PRIMARY KEY (tid)");
    execute("ALTER TABLE accounts ADD CONSTRAINT accounts_pk PRIMARY KEY (aid)");
}

function createIndexPostgreSQL() {
    createIndexOracle();
}

function createForeignKeyOracle() {
    info("Creating foreign keys ...");
    
    execute("ALTER TABLE tellers ADD CONSTRAINT tellers_fk1 "
        + "FOREIGN KEY (bid) REFERENCES branches (bid)");
    
    execute("ALTER TABLE accounts ADD CONSTRAINT accounts_fk1 "
        + "FOREIGN KEY (bid) REFERENCES branches (bid)");
    
    execute("ALTER TABLE history ADD CONSTRAINT history_fk1 "
        + "FOREIGN KEY (tid) REFERENCES tellers (tid)");
    
    execute("ALTER TABLE history ADD CONSTRAINT history_fk2 "
        + "FOREIGN KEY (bid) REFERENCES branches (bid)");
    
    execute("ALTER TABLE history ADD CONSTRAINT history_fk3 "
        + "FOREIGN KEY (aid) REFERENCES accounts (aid)");
}

function createForeignKeyPostgreSQL() {
    createForeignKeyOracle()
}

function gatherStatsOracle() {
    info("Analyzing tables ...");
    
    execute("BEGIN DBMS_STATS.GATHER_SCHEMA_STATS(ownname => NULL); END;");
}

function gatherStatsMySQL() {
    info("Analyzing tables ...");
    
    query("ANALYZE TABLE branches");
    query("ANALYZE TABLE tellers");
    query("ANALYZE TABLE accounts");
    query("ANALYZE TABLE history");
}

function gatherStatsPostgreSQL() {
    info("Vacuuming and analyzing tables ...");
    
    takeConnection().setAutoCommit(true);
    execute("VACUUM ANALYZE branches");
    execute("VACUUM ANALYZE tellers");
    execute("VACUUM ANALYZE accounts");
    execute("VACUUM ANALYZE history");
    takeConnection().setAutoCommit(false);
}

function loadBranches(branchId) {
    execute("INSERT INTO branches (bid, bbalance, filler) "
        + "VALUES ($int, 0, "
        + "'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb')",
        branchId);
}

function loadTellers(branchId) {
    var tid = new Array(TID_SCALE);
    var bid = new Array(TID_SCALE);
    
    for (var tellerId = TID_SCALE * (branchId - 1) + 1;
        tellerId <= TID_SCALE * branchId; tellerId++) {
        
        var index = (tellerId - 1) % TID_SCALE;
        
        tid[index] = tellerId;
        bid[index] = branchId;
    }
    
    executeBatch("INSERT INTO tellers (tid, bid, tbalance, filler) "
        + "VALUES ($int, $int, 0, "
        + "'tttttttttttttttttttttttttttttttttttttttttt"
        + "tttttttttttttttttttttttttttttttttttttttttt')",
        tid, bid);
}

function loadAccounts(branchId) {
    var aid = new Array(BATCH_SIZE);
    var bid = new Array(BATCH_SIZE);
    
    for (var accountId = AID_SCALE * (branchId - 1) + 1;
        accountId <= AID_SCALE * branchId; accountId++) {
        
        var index = (accountId - 1) % BATCH_SIZE;
        
        aid[index] = accountId;
        bid[index] = branchId;
        
        if (accountId % BATCH_SIZE == 0) {
            executeBatch("INSERT INTO accounts (aid, bid, abalance, filler) "
                + "VALUES ($int, $int, 0, "
                + "'ccccccccccccccccccccccccccccccccccccccccc"
                + "ccccccccccccccccccccccccccccccccccccccccccc')",
                aid, bid);
        }
    }
}

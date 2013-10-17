/*
 * Tiny SysBench 1.0 - data loader
 * This script is based on SysBench 0.4.12.
 * http://sysbench.sourceforge.net/
 *
 * [Oracle Database]
 * shell> sqlplus "/ AS SYSDBA"
 * sql> CREATE USER sbtest IDENTIFIED BY sbtest;
 * sql> GRANT connect, resource TO sbtest;
 *
 * [MySQL]
 * shell> mysql -u root [-p]
 * sql> CREATE DATABASE sbtest;
 * sql> GRANT ALL PRIVILEGES ON sbtest.* TO sbtest@'%' IDENTIFIED BY 'sbtest';
 *
 * [PostgreSQL]
 * shell> psql -U postgres
 * sql> CREATE DATABASE sbtest;
 * sql> CREATE USER sbtest PASSWORD 'sbtest';
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
// var jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/SBTEST";

// MySQL
var jdbcUrl = "jdbc:mysql://localhost:3306/sbtest?rewriteBatchedStatements=true";

// PostgreSQL
// var jdbcUrl = "jdbc:postgresql://localhost:5432/sbtest";

var jdbcUser = "sbtest";
var jdbcPass = "sbtest";
var isLoad = true;
var isAutoCommit = false;
var logDir = "logs";

// Application settings ----------------------------------------------

var BATCH_SIZE = 100;
var COMMIT_SIZE = 1000;

var oltpTableSize;

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        info("Tiny SysBench 1.0 - data loader");
        info("-param0 : Number of records (default : 10000)");
        
        oltpTableSize = param0;
        
        if (oltpTableSize == 0) {
            oltpTableSize = 10000;
        }
        
        info("Number of records : " + oltpTableSize);
        
        if (getDatabaseProductName() == "Oracle") {
            dropTableOracle();
            createTableOracle();
        } else if (getDatabaseProductName() == "MySQL") {
            dropTableMySQL();
            createTableMySQL();
        } else if (getDatabaseProductName() == "PostgreSQL") {
            dropTablePostgreSQL();
            createTablePostgreSQL();
        } else {
            error(getDatabaseProductName() + " is not supported yet.");
        }
        
        commit();
    }
}

function run() {
    if (getId() == 0) {
        info("Loading sbtest ...");
        
        var k = new Array();
        var c = new Array();
        var pad = new Array();
        
        for (var count = 1; count <= oltpTableSize; count++) {
            k.push(0);
            c.push(" ");
            pad.push("qqqqqqqqqqwwwwwwwwwweeeeeeeeeerrrrrrrrrrtttttttttt");
            
            if (count % BATCH_SIZE == 0) {
                executeBatch("INSERT INTO sbtest (k, c, pad) VALUES ($int, $string, $string)",
                    k, c, pad);
                
                k.length = 0;
                c.length = 0;
                pad.length = 0;
                
                if (count % COMMIT_SIZE == 0) {
                    commit();
                    info("sbtest : " + count + " / " + oltpTableSize);
                }
            }
        }
        
        if (oltpTableSize % COMMIT_SIZE != 0) {
            if (oltpTableSize % BATCH_SIZE != 0) {
                executeBatch("INSERT INTO sbtest (k, c, pad) VALUES ($int, $string, $string)",
                    k, c, pad);
            }
            
            commit();
            info("sbtest : " + oltpTableSize + " / " + oltpTableSize);
        }
    }
    
    setBreak();
}

function fin() {
    if (getId() == 0) {
        if (getDatabaseProductName() == "Oracle") {
            createIndexOracle();
            gatherStatsOracle();
        } else if (getDatabaseProductName() == "MySQL") {
            // Do nothing.
        } else if (getDatabaseProductName() == "PostgreSQL") {
            createIndexPostgreSQL();
            gatherStatsPostgreSQL();
        } else {
            error(getDatabaseProductName() + " is not supported yet.");
        }
        
        commit();
        info("Completed.");
    }
}

// Application functions ---------------------------------------------

function dropTableOracle() {
    info("Dropping a table ...");
    
    try {
        execute("DROP TABLE sbtest");
        execute("DROP SEQUENCE sbtest_seq");
    } catch (e) {
        warn(e);
    }
}

function dropTableMySQL() {
    info("Dropping a table ...");
    
    try {
        execute("DROP TABLE sbtest");
    } catch (e) {
        warn(e);
        rollback(); // PostgreSQL requires a rollback.
    }
}

function dropTablePostgreSQL() {
    dropTableMySQL();
}

function createTableOracle() {
    info("Creating a table ...");
    
    execute("CREATE TABLE sbtest ("
        + "id NUMBER, "
        + "k NUMBER DEFAULT 0 NOT NULL, "
        + "c CHAR(120) DEFAULT '' NOT NULL, "
        + "pad CHAR(60) DEFAULT '' NOT NULL)");
    
    execute("CREATE SEQUENCE sbtest_seq");
    
    var statement;
    
    try {
        statement = takeConnection().createStatement();
        
        statement.executeUpdate("CREATE TRIGGER sbtest_trig "
            + "BEFORE INSERT ON sbtest FOR EACH ROW "
            + "BEGIN "
                + "IF :NEW.id IS NULL THEN "
                    + "SELECT sbtest_seq.NEXTVAL INTO :NEW.id FROM DUAL; "
                + "END IF; "
            + "END;");
    } finally {
        try {
            statement.close();
        } catch (e) {
            warn(e);
        }
    }
}

function createTableMySQL() {
    info("Creating a table ...");
    
    execute("CREATE TABLE sbtest ("
        + "id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT, "
        + "k INT UNSIGNED DEFAULT 0 NOT NULL, "
        + "c CHAR(120) DEFAULT '' NOT NULL, "
        + "pad CHAR(60) DEFAULT '' NOT NULL, "
        + "KEY k (k)) "
        + "ENGINE = InnoDB");
}

function createTablePostgreSQL() {
    info("Creating a table ...");
    
    execute("CREATE TABLE sbtest ("
        + "id SERIAL, "
        + "k INTEGER DEFAULT 0 NOT NULL, "
        + "c CHAR(120) DEFAULT '' NOT NULL, "
        + "pad CHAR(60) DEFAULT '' NOT NULL)");
}

function createIndexOracle() {
    info("Creating indexes ...");
    
    execute("ALTER TABLE sbtest ADD CONSTRAINT sbtest_pk PRIMARY KEY (id)");
    execute("CREATE INDEX sbtest_ix1 ON sbtest (k)");
}

function createIndexPostgreSQL() {
    createIndexOracle();
}

function gatherStatsOracle() {
    info("Analyzing a table ...");
    
    execute("BEGIN DBMS_STATS.GATHER_SCHEMA_STATS(ownname => NULL); END;");
}

function gatherStatsPostgreSQL() {
    info("Vacuuming and analyzing tables ...");
    
    takeConnection().setAutoCommit(true);
    execute("VACUUM ANALYZE sbtest");
    takeConnection().setAutoCommit(false);
}

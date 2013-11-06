/*
 * tpcc-mysql compatible data loader 1.1
 * This script is based on tpcc-mysql revision 42.
 * https://code.launchpad.net/~percona-dev/perconatools/tpcc-mysql
 *
 * shell> mysql -u root [-p]
 * sql> CREATE DATABASE tpcc;
 * sql> GRANT ALL PRIVILEGES ON tpcc.* TO tpcc@'%' IDENTIFIED BY 'tpcc';
 */

// JdbcRunner settings -----------------------------------------------

var jdbcUrl = "jdbc:mysql://localhost:3306/tpcc?rewriteBatchedStatements=true";
var jdbcUser = "tpcc";
var jdbcPass = "tpcc";
var isLoad = true;
var nAgents = 4;
var isAutoCommit = false;
var logDir = "logs";

// Application settings ----------------------------------------------

var BATCH_SIZE = 100;
var COMMIT_SIZE = 1000;
var PRINT_SIZE = 10000;

var C_255;
var C_1023;
var C_8191;

var SYLLABLE = [
    "BAR", "OUGHT", "ABLE", "PRI", "PRES",
    "ESE", "ANTI", "CALLY", "ATION", "EING"];

var beginTimestamp;

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        var scale = param0;
        var taskQueue = new java.util.concurrent.LinkedBlockingQueue();
        
        info("tpcc-mysql compatible data loader 1.0");
        info("-param0  : Scale factor (default : 16)");
        info("-nAgents : Parallel loading degree (default : 4)");
        
        if (scale == 0) {
            scale = 16;
        }
        
        info("Scale factor            : " + scale);
        info("Parallel loading degree : " + nAgents);
        
        for (var warehouseId = 1; warehouseId <= scale; warehouseId++) {
            taskQueue.offer(warehouseId);
        }
        
        putData("TaskQueue", taskQueue);
        putData("BeginTimestamp", new Date());
        putData("C_255", random(0, 255));
        putData("C_1023", random(0, 1023));
        putData("C_8191", random(0, 8191));
        dropTable();
        
        var version = 100 * getDatabaseMajorVersion() + getDatabaseMinorVersion();
        
        if (version >= 505) {
            // Delaying index creation
            createTable505();
        } else {
            createTable501();
        }
        
        loadItem();
    }
}

function run() {
    if (!beginTimestamp) {
        beginTimestamp = getData("BeginTimestamp");
        C_255 = Number(getData("C_255"));
        C_1023 = Number(getData("C_1023"));
        C_8191 = Number(getData("C_8191"));
    }
    
    var warehouseId = Number(getData("TaskQueue").poll());
    
    if (warehouseId != 0) {
        info("Loading warehouse id " + warehouseId + " by agent " + getId() + " ...");
        
        execute("SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0");
        loadWarehouse(warehouseId);
        loadDistrict(warehouseId);
        loadCustomer(warehouseId);
        loadStock(warehouseId);
        loadOrders(warehouseId);
        execute("SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS");
    } else {
        setBreak();
    }
}

function fin() {
    if (getId() == 0) {
        var version = 100 * getDatabaseMajorVersion() + getDatabaseMinorVersion();
        
        if (version >= 505) {
            // Using fast index creation
            createIndex505();
        }
        
        info("Completed.");
    }
}

// Application functions ---------------------------------------------

function dropTable() {
    info("Dropping tables ...");
    
    execute("DROP TABLE IF EXISTS order_line");
    execute("DROP TABLE IF EXISTS new_orders");
    execute("DROP TABLE IF EXISTS orders");
    execute("DROP TABLE IF EXISTS stock");
    execute("DROP TABLE IF EXISTS item");
    execute("DROP TABLE IF EXISTS history");
    execute("DROP TABLE IF EXISTS customer");
    execute("DROP TABLE IF EXISTS district");
    execute("DROP TABLE IF EXISTS warehouse");
}

function createTable505() {
    info("Creating tables ...");
    
    execute("CREATE TABLE warehouse ("
        + "w_id SMALLINT NOT NULL, "
        + "w_name VARCHAR(10), "
        + "w_street_1 VARCHAR(20), "
        + "w_street_2 VARCHAR(20), "
        + "w_city VARCHAR(20), "
        + "w_state CHAR(2), "
        + "w_zip CHAR(9), "
        + "w_tax DECIMAL(4, 2), "
        + "w_ytd DECIMAL(12, 2), "
        + "PRIMARY KEY (w_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE district ("
        + "d_id TINYINT NOT NULL, "
        + "d_w_id SMALLINT NOT NULL, "
        + "d_name VARCHAR(10), "
        + "d_street_1 VARCHAR(20), "
        + "d_street_2 VARCHAR(20), "
        + "d_city VARCHAR(20), "
        + "d_state CHAR(2), "
        + "d_zip CHAR(9), "
        + "d_tax DECIMAL(4, 2), "
        + "d_ytd DECIMAL(12, 2), "
        + "d_next_o_id INT, "
        + "PRIMARY KEY (d_w_id, d_id), "
        + "CONSTRAINT fkey_district_1 "
            + "FOREIGN KEY (d_w_id) "
            + "REFERENCES warehouse (w_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE customer ("
        + "c_id INT NOT NULL, "
        + "c_d_id TINYINT NOT NULL, "
        + "c_w_id SMALLINT NOT NULL, "
        + "c_first VARCHAR(16), "
        + "c_middle CHAR(2), "
        + "c_last VARCHAR(16), "
        + "c_street_1 VARCHAR(20), "
        + "c_street_2 VARCHAR(20), "
        + "c_city VARCHAR(20), "
        + "c_state CHAR(2), "
        + "c_zip CHAR(9), "
        + "c_phone CHAR(16), "
        + "c_since DATETIME, "
        + "c_credit CHAR(2), "
        + "c_credit_lim BIGINT, "
        + "c_discount DECIMAL(4, 2), "
        + "c_balance DECIMAL(12, 2), "
        + "c_ytd_payment DECIMAL(12, 2), "
        + "c_payment_cnt SMALLINT, "
        + "c_delivery_cnt SMALLINT, "
        + "c_data TEXT, "
        + "PRIMARY KEY (c_w_id, c_d_id, c_id), "
        + "CONSTRAINT fkey_customer_1 "
            + "FOREIGN KEY (c_w_id, c_d_id) "
            + "REFERENCES district (d_w_id, d_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE history ("
        + "h_c_id INT, "
        + "h_c_d_id TINYINT, "
        + "h_c_w_id SMALLINT, "
        + "h_d_id TINYINT, "
        + "h_w_id SMALLINT, "
        + "h_date DATETIME, "
        + "h_amount DECIMAL(6, 2), "
        + "h_data VARCHAR(24), "
        + "CONSTRAINT fkey_history_1 "
            + "FOREIGN KEY (h_c_w_id, h_c_d_id, h_c_id) "
            + "REFERENCES customer (c_w_id, c_d_id, c_id), "
        + "CONSTRAINT fkey_history_2 "
            + "FOREIGN KEY (h_w_id, h_d_id) "
            + "REFERENCES district (d_w_id, d_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE item ("
        + "i_id INT NOT NULL, "
        + "i_im_id INT, "
        + "i_name VARCHAR(24), "
        + "i_price DECIMAL(5, 2), "
        + "i_data VARCHAR(50), "
        + "PRIMARY KEY (i_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE stock ("
        + "s_i_id INT NOT NULL, "
        + "s_w_id SMALLINT NOT NULL, "
        + "s_quantity SMALLINT, "
        + "s_dist_01 CHAR(24), "
        + "s_dist_02 CHAR(24), "
        + "s_dist_03 CHAR(24), "
        + "s_dist_04 CHAR(24), "
        + "s_dist_05 CHAR(24), "
        + "s_dist_06 CHAR(24), "
        + "s_dist_07 CHAR(24), "
        + "s_dist_08 CHAR(24), "
        + "s_dist_09 CHAR(24), "
        + "s_dist_10 CHAR(24), "
        + "s_ytd DECIMAL(8, 0), "
        + "s_order_cnt SMALLINT, "
        + "s_remote_cnt SMALLINT, "
        + "s_data VARCHAR(50), "
        + "PRIMARY KEY (s_w_id, s_i_id), "
        + "CONSTRAINT fkey_stock_1 "
            + "FOREIGN KEY (s_w_id) "
            + "REFERENCES warehouse (w_id), "
        + "CONSTRAINT fkey_stock_2 "
            + "FOREIGN KEY (s_i_id) "
            + "REFERENCES item (i_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE orders ("
        + "o_id INT NOT NULL, "
        + "o_d_id TINYINT NOT NULL, "
        + "o_w_id SMALLINT NOT NULL, "
        + "o_c_id INT, "
        + "o_entry_d DATETIME, "
        + "o_carrier_id TINYINT, "
        + "o_ol_cnt TINYINT, "
        + "o_all_local TINYINT, "
        + "PRIMARY KEY (o_w_id, o_d_id, o_id), "
        + "CONSTRAINT fkey_orders_1 "
            + "FOREIGN KEY (o_w_id, o_d_id, o_c_id) "
            + "REFERENCES customer (c_w_id, c_d_id, c_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE new_orders ("
        + "no_o_id INT NOT NULL, "
        + "no_d_id TINYINT NOT NULL, "
        + "no_w_id SMALLINT NOT NULL, "
        + "PRIMARY KEY (no_w_id, no_d_id, no_o_id), "
        + "CONSTRAINT fkey_new_orders_1 "
            + "FOREIGN KEY (no_w_id, no_d_id, no_o_id) "
            + "REFERENCES orders (o_w_id, o_d_id, o_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE order_line ("
        + "ol_o_id INT NOT NULL, "
        + "ol_d_id TINYINT NOT NULL, "
        + "ol_w_id SMALLINT NOT NULL, "
        + "ol_number TINYINT NOT NULL, "
        + "ol_i_id INT, "
        + "ol_supply_w_id SMALLINT, "
        + "ol_delivery_d DATETIME, "
        + "ol_quantity TINYINT, "
        + "ol_amount DECIMAL(6, 2), "
        + "ol_dist_info CHAR(24), "
        + "PRIMARY KEY (ol_w_id, ol_d_id, ol_o_id, ol_number), "
        + "CONSTRAINT fkey_order_line_1 "
            + "FOREIGN KEY (ol_w_id, ol_d_id, ol_o_id) "
            + "REFERENCES orders (o_w_id, o_d_id, o_id), "
        + "CONSTRAINT fkey_order_line_2 "
            + "FOREIGN KEY (ol_supply_w_id, ol_i_id) "
            + "REFERENCES stock (s_w_id, s_i_id)) "
        + "ENGINE = InnoDB");
}

function createTable501() {
    info("Creating tables ...");
    
    execute("CREATE TABLE warehouse ("
        + "w_id SMALLINT NOT NULL, "
        + "w_name VARCHAR(10), "
        + "w_street_1 VARCHAR(20), "
        + "w_street_2 VARCHAR(20), "
        + "w_city VARCHAR(20), "
        + "w_state CHAR(2), "
        + "w_zip CHAR(9), "
        + "w_tax DECIMAL(4, 2), "
        + "w_ytd DECIMAL(12, 2), "
        + "PRIMARY KEY (w_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE district ("
        + "d_id TINYINT NOT NULL, "
        + "d_w_id SMALLINT NOT NULL, "
        + "d_name VARCHAR(10), "
        + "d_street_1 VARCHAR(20), "
        + "d_street_2 VARCHAR(20), "
        + "d_city VARCHAR(20), "
        + "d_state CHAR(2), "
        + "d_zip CHAR(9), "
        + "d_tax DECIMAL(4, 2), "
        + "d_ytd DECIMAL(12, 2), "
        + "d_next_o_id INT, "
        + "PRIMARY KEY (d_w_id, d_id), "
        + "CONSTRAINT fkey_district_1 "
            + "FOREIGN KEY (d_w_id) "
            + "REFERENCES warehouse (w_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE customer ("
        + "c_id INT NOT NULL, "
        + "c_d_id TINYINT NOT NULL, "
        + "c_w_id SMALLINT NOT NULL, "
        + "c_first VARCHAR(16), "
        + "c_middle CHAR(2), "
        + "c_last VARCHAR(16), "
        + "c_street_1 VARCHAR(20), "
        + "c_street_2 VARCHAR(20), "
        + "c_city VARCHAR(20), "
        + "c_state CHAR(2), "
        + "c_zip CHAR(9), "
        + "c_phone CHAR(16), "
        + "c_since DATETIME, "
        + "c_credit CHAR(2), "
        + "c_credit_lim BIGINT, "
        + "c_discount DECIMAL(4, 2), "
        + "c_balance DECIMAL(12, 2), "
        + "c_ytd_payment DECIMAL(12, 2), "
        + "c_payment_cnt SMALLINT, "
        + "c_delivery_cnt SMALLINT, "
        + "c_data TEXT, "
        + "PRIMARY KEY (c_w_id, c_d_id, c_id), "
        + "KEY idx_customer (c_w_id, c_d_id, c_last, c_first), "
        + "CONSTRAINT fkey_customer_1 "
            + "FOREIGN KEY (c_w_id, c_d_id) "
            + "REFERENCES district (d_w_id, d_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE history ("
        + "h_c_id INT, "
        + "h_c_d_id TINYINT, "
        + "h_c_w_id SMALLINT, "
        + "h_d_id TINYINT, "
        + "h_w_id SMALLINT, "
        + "h_date DATETIME, "
        + "h_amount DECIMAL(6, 2), "
        + "h_data VARCHAR(24), "
        + "CONSTRAINT fkey_history_1 "
            + "FOREIGN KEY (h_c_w_id, h_c_d_id, h_c_id) "
            + "REFERENCES customer (c_w_id, c_d_id, c_id), "
        + "CONSTRAINT fkey_history_2 "
            + "FOREIGN KEY (h_w_id, h_d_id) "
            + "REFERENCES district (d_w_id, d_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE item ("
        + "i_id INT NOT NULL, "
        + "i_im_id INT, "
        + "i_name VARCHAR(24), "
        + "i_price DECIMAL(5, 2), "
        + "i_data VARCHAR(50), "
        + "PRIMARY KEY (i_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE stock ("
        + "s_i_id INT NOT NULL, "
        + "s_w_id SMALLINT NOT NULL, "
        + "s_quantity SMALLINT, "
        + "s_dist_01 CHAR(24), "
        + "s_dist_02 CHAR(24), "
        + "s_dist_03 CHAR(24), "
        + "s_dist_04 CHAR(24), "
        + "s_dist_05 CHAR(24), "
        + "s_dist_06 CHAR(24), "
        + "s_dist_07 CHAR(24), "
        + "s_dist_08 CHAR(24), "
        + "s_dist_09 CHAR(24), "
        + "s_dist_10 CHAR(24), "
        + "s_ytd DECIMAL(8, 0), "
        + "s_order_cnt SMALLINT, "
        + "s_remote_cnt SMALLINT, "
        + "s_data VARCHAR(50), "
        + "PRIMARY KEY (s_w_id, s_i_id), "
        + "CONSTRAINT fkey_stock_1 "
            + "FOREIGN KEY (s_w_id) "
            + "REFERENCES warehouse (w_id), "
        + "CONSTRAINT fkey_stock_2 "
            + "FOREIGN KEY (s_i_id) "
            + "REFERENCES item (i_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE orders ("
        + "o_id INT NOT NULL, "
        + "o_d_id TINYINT NOT NULL, "
        + "o_w_id SMALLINT NOT NULL, "
        + "o_c_id INT, "
        + "o_entry_d DATETIME, "
        + "o_carrier_id TINYINT, "
        + "o_ol_cnt TINYINT, "
        + "o_all_local TINYINT, "
        + "PRIMARY KEY (o_w_id, o_d_id, o_id), "
        + "KEY idx_orders (o_w_id, o_d_id, o_c_id, o_id), "
        + "CONSTRAINT fkey_orders_1 "
            + "FOREIGN KEY (o_w_id, o_d_id, o_c_id) "
            + "REFERENCES customer (c_w_id, c_d_id, c_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE new_orders ("
        + "no_o_id INT NOT NULL, "
        + "no_d_id TINYINT NOT NULL, "
        + "no_w_id SMALLINT NOT NULL, "
        + "PRIMARY KEY (no_w_id, no_d_id, no_o_id), "
        + "CONSTRAINT fkey_new_orders_1 "
            + "FOREIGN KEY (no_w_id, no_d_id, no_o_id) "
            + "REFERENCES orders (o_w_id, o_d_id, o_id)) "
        + "ENGINE = InnoDB");
    
    execute("CREATE TABLE order_line ("
        + "ol_o_id INT NOT NULL, "
        + "ol_d_id TINYINT NOT NULL, "
        + "ol_w_id SMALLINT NOT NULL, "
        + "ol_number TINYINT NOT NULL, "
        + "ol_i_id INT, "
        + "ol_supply_w_id SMALLINT, "
        + "ol_delivery_d DATETIME, "
        + "ol_quantity TINYINT, "
        + "ol_amount DECIMAL(6, 2), "
        + "ol_dist_info CHAR(24), "
        + "PRIMARY KEY (ol_w_id, ol_d_id, ol_o_id, ol_number), "
        + "CONSTRAINT fkey_order_line_1 "
            + "FOREIGN KEY (ol_w_id, ol_d_id, ol_o_id) "
            + "REFERENCES orders (o_w_id, o_d_id, o_id), "
        + "CONSTRAINT fkey_order_line_2 "
            + "FOREIGN KEY (ol_supply_w_id, ol_i_id) "
            + "REFERENCES stock (s_w_id, s_i_id)) "
        + "ENGINE = InnoDB");
}

function createIndex505() {
    info("Creating indexes ...");
    
    execute("ALTER TABLE customer "
        + "ADD KEY idx_customer (c_w_id, c_d_id, c_last, c_first)");
    
    execute("ALTER TABLE orders "
        + "ADD KEY idx_orders (o_w_id, o_d_id, o_c_id, o_id)");
}

function loadItem() {
    info("Loading item ...");
    
    var i_id = new Array(BATCH_SIZE);
    var i_im_id = new Array(BATCH_SIZE);
    var i_name = new Array(BATCH_SIZE);
    var i_price = new Array(BATCH_SIZE);
    var i_data = new Array(BATCH_SIZE);
    
    for (var itemId = 1; itemId <= 100000; itemId++) {
        var index = (itemId - 1) % BATCH_SIZE;
        
        i_id[index] = itemId;
        i_im_id[index] = random(1, 10000);
        i_name[index] = randomString(random(14, 24));
        i_price[index] = random(100, 10000) / 100;
        i_data[index] = randomString(random(26, 50));
        
        if (random(1, 10) == 1) {
            var replace = random(0, i_data[index].length - 8);
            
            i_data[index] =
                i_data[index].substring(0, replace)
                + "original"
                + i_data[index].substring(replace + 8);
        }
        
        if (itemId % BATCH_SIZE == 0) {
            executeBatch("INSERT INTO item "
                + "(i_id, i_im_id, i_name, i_price, i_data) "
                + "VALUES ($int, $int, $string, $double, $string)",
                i_id, i_im_id, i_name, i_price, i_data);
            
            if (itemId % COMMIT_SIZE == 0) {
                commit();
                
                if (itemId % PRINT_SIZE == 0) {
                    info("item : " + itemId + " / 100000");
                }
            }
        }
    }
}

function loadWarehouse(warehouseId) {
    info("[Agent " + getId() + "] Loading warehouse ...");
    
    var w_name = randomString(random(6, 10));
    var w_street_1 = randomString(random(10, 20));
    var w_street_2 = randomString(random(10, 20));
    var w_city = randomString(random(10, 20));
    var w_state = randomString(2);
    var w_zip = String(random(1000000000, 1999999999)).substring(1);
    var w_tax = random(10, 20) / 100;
    var w_ytd = 3000000;
    
    execute("INSERT INTO warehouse "
        + "(w_id, w_name, w_street_1, w_street_2, w_city, "
        + "w_state, w_zip, w_tax, w_ytd) "
        + "VALUES ($int, $string, $string, $string, $string, "
        + "$string, $string, $double, $double)",
        warehouseId, w_name, w_street_1, w_street_2, w_city, w_state,
        w_zip, w_tax, w_ytd);
    
    commit();
}

function loadDistrict(warehouseId) {
    info("[Agent " + getId() + "] Loading district ...");
    
    var d_id = new Array(10);
    var d_w_id = new Array(10);
    var d_name = new Array(10);
    var d_street_1 = new Array(10);
    var d_street_2 = new Array(10);
    var d_city = new Array(10);
    var d_state = new Array(10);
    var d_zip = new Array(10);
    var d_tax = new Array(10);
    var d_ytd = new Array(10);
    var d_next_o_id = new Array(10);
    
    for (var districtId = 1; districtId <= 10; districtId++) {
        var index = districtId - 1;
        
        d_id[index] = districtId;
        d_w_id[index] = warehouseId;
        d_name[index] = randomString(random(6, 10));
        d_street_1[index] = randomString(random(10, 20));
        d_street_2[index] = randomString(random(10, 20));
        d_city[index] = randomString(random(10, 20));
        d_state[index] = randomString(2);
        d_zip[index] = String(random(1000000000, 1999999999)).substring(1);
        d_tax[index] = random(10, 20) / 100;
        d_ytd[index] = 30000;
        d_next_o_id[index] = 3001;
    }
    
    executeBatch("INSERT INTO district "
        + "(d_id, d_w_id, d_name, d_street_1, d_street_2, "
        + "d_city, d_state, d_zip, d_tax, d_ytd, d_next_o_id) "
        + "VALUES ($int, $int, $string, $string, $string, "
        + "$string, $string, $string, $double, $double, $int)",
        d_id, d_w_id, d_name, d_street_1, d_street_2,
        d_city, d_state, d_zip, d_tax, d_ytd, d_next_o_id);
    
    commit();
}

function loadCustomer(warehouseId) {
    info("[Agent " + getId() + "] Loading customer and history ...");
    
    // customer
    var c_id = new Array(BATCH_SIZE);
    var c_d_id = new Array(BATCH_SIZE);
    var c_w_id = new Array(BATCH_SIZE);
    var c_first = new Array(BATCH_SIZE);
    var c_middle = new Array(BATCH_SIZE);
    var c_last = new Array(BATCH_SIZE);
    var c_street_1 = new Array(BATCH_SIZE);
    var c_street_2 = new Array(BATCH_SIZE);
    var c_city = new Array(BATCH_SIZE);
    var c_state = new Array(BATCH_SIZE);
    var c_zip = new Array(BATCH_SIZE);
    var c_phone = new Array(BATCH_SIZE);
    var c_since = new Array(BATCH_SIZE);
    var c_credit = new Array(BATCH_SIZE);
    var c_credit_lim = new Array(BATCH_SIZE);
    var c_discount = new Array(BATCH_SIZE);
    var c_balance = new Array(BATCH_SIZE);
    var c_ytd_payment = new Array(BATCH_SIZE);
    var c_payment_cnt = new Array(BATCH_SIZE);
    var c_delivery_cnt = new Array(BATCH_SIZE);
    var c_data = new Array(BATCH_SIZE);
    
    // history
    var h_c_id = new Array(BATCH_SIZE);
    var h_c_d_id = new Array(BATCH_SIZE);
    var h_c_w_id = new Array(BATCH_SIZE);
    var h_d_id = new Array(BATCH_SIZE);
    var h_w_id = new Array(BATCH_SIZE);
    var h_date = new Array(BATCH_SIZE);
    var h_amount = new Array(BATCH_SIZE);
    var h_data = new Array(BATCH_SIZE);
    
    for (var districtId = 1; districtId <= 10; districtId++) {
        for (var customerId = 1; customerId <= 3000; customerId++) {
            var index = (customerId - 1) % BATCH_SIZE;
            
            // customer
            c_id[index] = customerId;
            c_d_id[index] = districtId;
            c_w_id[index] = warehouseId;
            c_first[index] = randomString(random(8, 16));
            c_middle[index] = "OE";
            
            if (customerId <= 1000) {
                c_last[index] = lastName(customerId - 1);
            } else {
                c_last[index] = lastName(nonUniformRandom(255, 0, 999));
            }
            
            c_street_1[index] = randomString(random(10, 20));
            c_street_2[index] = randomString(random(10, 20));
            c_city[index] = randomString(random(10, 20));
            c_state[index] = randomString(2);
            c_zip[index] = String(random(1000000000, 1999999999)).substring(1);
            c_phone[index] = String(random(10000000000000000, 19999999999999999)).substring(1);
            c_since[index] = beginTimestamp;
            
            if (random(1, 2) == 1) {
                c_credit[index] = "BC";
            } else {
                c_credit[index] = "GC";
            }
            
            c_credit_lim[index] = 50000;
            c_discount[index] = random(0, 50) / 100;
            c_balance[index] = -10;
            c_ytd_payment[index] = 10;
            c_payment_cnt[index] = 1;
            c_delivery_cnt[index] = 0;
            c_data[index] = randomString(random(300, 500));
            
            
            // history
            h_c_id[index] = customerId;
            h_c_d_id[index] = districtId;
            h_c_w_id[index] = warehouseId;
            h_d_id[index] = districtId;
            h_w_id[index] = warehouseId;
            h_date[index] = beginTimestamp;
            h_amount[index] = 10;
            h_data[index] = randomString(random(12, 24));
            
            if (customerId % BATCH_SIZE == 0) {
                executeBatch("INSERT INTO customer "
                    + "(c_id, c_d_id, c_w_id, c_first, c_middle, c_last, "
                    + "c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, "
                    + "c_since, c_credit, c_credit_lim, c_discount, c_balance, "
                    + "c_ytd_payment, c_payment_cnt, c_delivery_cnt, c_data) "
                    + "VALUES ($int, $int, $int, $string, $string, $string, "
                    + "$string, $string, $string, $string, $string, $string, "
                    + "$timestamp, $string, $double, $double, $double, "
                    + "$double, $int, $int, $string)",
                    c_id, c_d_id, c_w_id, c_first, c_middle, c_last,
                    c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, 
                    c_since, c_credit, c_credit_lim, c_discount, c_balance,
                    c_ytd_payment, c_payment_cnt, c_delivery_cnt, c_data);
                
                executeBatch("INSERT INTO history "
                    + "(h_c_id, h_c_d_id, h_c_w_id, h_d_id, "
                    + "h_w_id, h_date, h_amount, h_data) "
                    + "VALUES ($int, $int, $int, $int, "
                    + "$int, $timestamp, $double, $string)",
                    h_c_id, h_c_d_id, h_c_w_id, h_d_id,
                    h_w_id, h_date, h_amount, h_data);
                
                if (customerId % COMMIT_SIZE == 0) {
                    commit();
                    
                    if (((districtId - 1) * 3000 + customerId) % PRINT_SIZE == 0) {
                        info("[Agent " + getId() + "] customer : "
                            + ((districtId - 1) * 3000 + customerId) + " / 30000");
                    }
                }
            }
        }
    }
}

function loadStock(warehouseId) {
    info("[Agent " + getId() + "] Loading stock ...");
    
    var s_i_id = new Array(BATCH_SIZE);
    var s_w_id = new Array(BATCH_SIZE);
    var s_quantity = new Array(BATCH_SIZE);
    var s_dist_01 = new Array(BATCH_SIZE);
    var s_dist_02 = new Array(BATCH_SIZE);
    var s_dist_03 = new Array(BATCH_SIZE);
    var s_dist_04 = new Array(BATCH_SIZE);
    var s_dist_05 = new Array(BATCH_SIZE);
    var s_dist_06 = new Array(BATCH_SIZE);
    var s_dist_07 = new Array(BATCH_SIZE);
    var s_dist_08 = new Array(BATCH_SIZE);
    var s_dist_09 = new Array(BATCH_SIZE);
    var s_dist_10 = new Array(BATCH_SIZE);
    var s_ytd = new Array(BATCH_SIZE);
    var s_order_cnt = new Array(BATCH_SIZE);
    var s_remote_cnt = new Array(BATCH_SIZE);
    var s_data = new Array(BATCH_SIZE);
    
    for (var itemId = 1; itemId <= 100000; itemId++) {
        var index = (itemId - 1) % BATCH_SIZE;
        
        s_i_id[index] = itemId;
        s_w_id[index] = warehouseId;
        s_quantity[index] = random(10, 100);
        s_dist_01[index] = randomString(24);
        s_dist_02[index] = randomString(24);
        s_dist_03[index] = randomString(24);
        s_dist_04[index] = randomString(24);
        s_dist_05[index] = randomString(24);
        s_dist_06[index] = randomString(24);
        s_dist_07[index] = randomString(24);
        s_dist_08[index] = randomString(24);
        s_dist_09[index] = randomString(24);
        s_dist_10[index] = randomString(24);
        s_ytd[index] = 0;
        s_order_cnt[index] = 0;
        s_remote_cnt[index] = 0;
        s_data[index] = randomString(random(26, 50));
        
        if (random(1, 10) == 1) {
            var replace = random(0, s_data[index].length - 8);
            
            s_data[index] =
                s_data[index].substring(0, replace)
                + "original"
                + s_data[index].substring(replace + 8);
        }
        
        if (itemId % BATCH_SIZE == 0) {
            executeBatch("INSERT INTO stock "
                + "(s_i_id, s_w_id, s_quantity, s_dist_01, s_dist_02, s_dist_03, "
                + "s_dist_04, s_dist_05, s_dist_06, s_dist_07, s_dist_08, s_dist_09, "
                + "s_dist_10, s_ytd, s_order_cnt, s_remote_cnt, s_data) "
                + "VALUES ($int, $int, $int, $string, $string, $string, "
                + "$string, $string, $string, $string, $string, $string, "
                + "$string, $int, $int, $int, $string)",
                s_i_id, s_w_id, s_quantity, s_dist_01, s_dist_02, s_dist_03,
                s_dist_04, s_dist_05, s_dist_06, s_dist_07, s_dist_08, s_dist_09,
                s_dist_10, s_ytd, s_order_cnt, s_remote_cnt, s_data);
            
            if (itemId % COMMIT_SIZE == 0) {
                commit();
                
                if (itemId % PRINT_SIZE == 0) {
                    info("[Agent " + getId() + "] stock : " + itemId + " / 100000");
                }
            }
        }
    }
}

function loadOrders(warehouseId) {
    info("[Agent " + getId() + "] Loading orders, new_orders and order_line ...");
    
    // orders
    var o_id = new Array(BATCH_SIZE);
    var o_d_id = new Array(BATCH_SIZE);
    var o_w_id = new Array(BATCH_SIZE);
    var o_c_id = new Array(BATCH_SIZE);
    var o_entry_d = new Array(BATCH_SIZE);
    var o_carrier_id = new Array(BATCH_SIZE);
    var o_ol_cnt = new Array(BATCH_SIZE);
    var o_all_local = new Array(BATCH_SIZE);
    
    // new_orders
    var no_o_id = new Array();
    var no_d_id = new Array();
    var no_w_id = new Array();
    
    // order_line
    var ol_o_id = new Array();
    var ol_d_id = new Array();
    var ol_w_id = new Array();
    var ol_number = new Array();
    var ol_i_id = new Array();
    var ol_supply_w_id = new Array();
    var ol_delivery_d = new Array();
    var ol_quantity = new Array();
    var ol_amount = new Array();
    var ol_dist_info = new Array();
    
    for (var districtId = 1; districtId <= 10; districtId++) {
        var customerSequence = new Array(3000);
        var rand;
        var swap;
        
        for (var index = 0; index < 3000; index++) {
            customerSequence[index] = index + 1;
        }
        
        // Fisher-Yates algorithm
        for (var tail = 3000 - 1; tail > 0; tail--) {
            rand = random(0, tail);
            swap = customerSequence[tail];
            customerSequence[tail] = customerSequence[rand];
            customerSequence[rand] = swap;
        }
        
        for (var orderId = 1; orderId <= 3000; orderId++) {
            var index = (orderId - 1) % BATCH_SIZE;
            
            // orders
            o_id[index] = orderId;
            o_d_id[index] = districtId;
            o_w_id[index] = warehouseId;
            o_c_id[index] = customerSequence[orderId - 1];
            o_entry_d[index] = beginTimestamp;
            
            if (orderId < 2101) {
                o_carrier_id[index] = random(1, 10);
            } else {
                o_carrier_id[index] = null;
            }
            
            o_ol_cnt[index] = random(5, 15);
            o_all_local[index] = 1;
            
            // new_orders
            if (orderId >= 2101) {
                no_o_id.push(orderId);
                no_d_id.push(districtId);
                no_w_id.push(warehouseId);
            }
            
            // order_line
            for (var orderLineId = 1; orderLineId <= o_ol_cnt[index]; orderLineId++) {
                ol_o_id.push(orderId);
                ol_d_id.push(districtId);
                ol_w_id.push(warehouseId);
                ol_number.push(orderLineId);
                ol_i_id.push(random(1, 100000));
                ol_supply_w_id.push(warehouseId);
                
                if (orderId < 2101) {
                    ol_delivery_d.push(beginTimestamp);
                    ol_amount.push(0);
                } else {
                    ol_delivery_d.push(null);
                    ol_amount.push(random(10, 10000) / 100);
                }
                
                ol_quantity.push(5);
                ol_dist_info.push(randomString(24));
            }
            
            if (orderId % BATCH_SIZE == 0) {
                executeBatch("INSERT INTO orders "
                    + "(o_id, o_d_id, o_w_id, o_c_id, o_entry_d, "
                    + "o_carrier_id, o_ol_cnt, o_all_local) "
                    + "VALUES ($int, $int, $int, $int, $timestamp, "
                    + "$int, $int, $int)",
                    o_id, o_d_id, o_w_id, o_c_id, o_entry_d,
                    o_carrier_id, o_ol_cnt, o_all_local);
                
                executeBatch("INSERT INTO new_orders "
                    + "(no_o_id, no_d_id, no_w_id) "
                    + "VALUES ($int, $int, $int)",
                    no_o_id, no_d_id, no_w_id);
                
                no_o_id.length = 0;
                no_d_id.length = 0;
                no_w_id.length = 0;
                
                executeBatch("INSERT INTO order_line "
                    + "(ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, ol_supply_w_id, "
                    + "ol_delivery_d, ol_quantity, ol_amount, ol_dist_info) "
                    + "VALUES ($int, $int, $int, $int, $int, $int, "
                    + "$timestamp, $int, $double, $string)",
                    ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, ol_supply_w_id,
                    ol_delivery_d, ol_quantity, ol_amount, ol_dist_info);
                
                ol_o_id.length = 0;
                ol_d_id.length = 0;
                ol_w_id.length = 0;
                ol_number.length = 0;
                ol_i_id.length = 0;
                ol_supply_w_id.length = 0;
                ol_delivery_d.length = 0;
                ol_quantity.length = 0;
                ol_amount.length = 0;
                ol_dist_info.length = 0;
                
                if (orderId % COMMIT_SIZE == 0) {
                    commit();
                    
                    if (((districtId - 1) * 3000 + orderId) % PRINT_SIZE == 0) {
                        info("[Agent " + getId() + "] orders : "
                            + ((districtId - 1) * 3000 + orderId) + " / 30000");
                    }
                }
            }
        }
    }
}

function nonUniformRandom(a, x, y) {
    var c = 0;
    
    switch (a) {
        case 255:
            c = C_255;
            break;
        case 1023:
            c = C_1023;
            break;
        case 8191:
            c = C_8191;
            break;
        default:
            c = 0;
    }
    
    return (((random(0, a) | random(x, y)) + c) % (y - x + 1)) + x;
}

function lastName(seed) {
    return SYLLABLE[Math.floor(seed / 100)]
        + SYLLABLE[Math.floor(seed / 10) % 10]
        + SYLLABLE[seed % 10];
}

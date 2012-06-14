/*
 * Tiny TPC-C 1.1
 * This script is based on TPC-C Standard Specification 5.10.1.
 */

// JdbcRunner settings -----------------------------------------------

// Oracle Database
// var jdbcUrl = "jdbc:oracle:thin://@localhost:1521/TPCC";

// MySQL
var jdbcUrl = "jdbc:mysql://localhost:3306/tpcc";

// PostgreSQL
// var jdbcUrl = "jdbc:postgresql://localhost:5432/tpcc";

var jdbcUser = "tpcc";
var jdbcPass = "tpcc";
var warmupTime = 300;
var measurementTime = 900;
var nTxTypes = 5;
var nAgents = 16;
var stmtCacheSize = 40;
var isAutoCommit = false;
var logDir = "logs";

// Application settings ----------------------------------------------

var DEADLOCK_RETRY_LIMIT = 10;

// Using a constant value '100' for C-Run, and '0' for C-Load.
var C_255 = 100;
var C_1023 = 100;
var C_8191 = 100;

var SYLLABLE = [
    'BAR', 'OUGHT', 'ABLE', 'PRI', 'PRES',
    'ESE', 'ANTI', 'CALLY', 'ATION', 'EING'];

var scale;

// Transaction sequence
var txSequence = [
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    2, 3, 4];

txSequence.index = txSequence.length;

txSequence.next = function() {
    if (this.index == this.length) {
        // Shuffle
        var rand;
        var swap;
        
        // Fisher-Yates algorithm
        for (var tail = this.length - 1; tail > 0; tail--) {
            rand = random(0, tail);
            swap = this[tail];
            this[tail] = this[rand];
            this[rand] = swap;
        }
        
        this.index = 0;
    }
    
    return this[this.index++];
}

// JdbcRunner functions ----------------------------------------------

function init() {
    if (getId() == 0) {
        info("Tiny TPC-C 1.1");
        
        putData("ScaleFactor", fetchAsArray("SELECT COUNT(*) FROM warehouse")[0][0]);
        info("Scale factor : " + Number(getData("ScaleFactor")));
        
        info("tx0 : New-Order transaction");
        info("tx1 : Payment transaction");
        info("tx2 : Order-Status transaction");
        info("tx3 : Delivery transaction");
        info("tx4 : Stock-Level transaction");
    }
}

function run() {
    if (!scale) {
        scale = Number(getData("ScaleFactor"));
    }
    
    switch (txSequence.next()) {
        case 0:
            setTxType(0);
            newOrder();
            break;
        case 1:
            setTxType(1);
            payment();
            break;
        case 2:
            setTxType(2);
            orderStatus();
            break;
        case 3:
            setTxType(3);
            delivery();
            break;
        case 4:
            setTxType(4);
            stockLevel();
            break;
    }
}

// Application functions ---------------------------------------------

function newOrder() {
    var w_id = random(1, scale);
    var d_id = random(1, 10);
    var c_id = nonUniformRandom(1023, 1, 3000);
    var ol_cnt = random(5, 15);
    var all_local = 1;
    
    var i_id = new Array(ol_cnt);
    var supply_w_id = new Array(ol_cnt);
    var quantity = new Array(ol_cnt);
    var order = new Array(ol_cnt);
    
    for (var index = 0; index < ol_cnt; index++) {
        i_id[index] = nonUniformRandom(8191, 1, 100000);
        
        if ((scale > 1) && (random(1, 100) == 1)) {
            // A supplying warehouse number is selected as a remote warehouse 1% of the time.
            supply_w_id[index] = random(1, scale - 1);
            
            if (supply_w_id[index] >= w_id) {
                supply_w_id[index]++;
            }
            
            all_local = 0;
        } else {
            supply_w_id[index] = w_id;
        }
        
        quantity[index] = random(1, 10);
        order[index] = index;
    }
    
    // The items are sorted to avoid deadlock.
    order.sort(function(a, b) {
        return (supply_w_id[a] * 100000 + i_id[a]) - (supply_w_id[b] * 100000 + i_id[b]);
    });
    
    if (random(1, 100) == 1) {
        // A fixed 1% of the transactions are chosen to simulate user data entry errors.
        i_id[ol_cnt - 1] = 0;
    }
    
    for (var retry = 0; retry <= DEADLOCK_RETRY_LIMIT; retry++) {
        try {
            var rc01 = query("SELECT /* N-01 */ w.w_tax, c.c_discount, c.c_last, c.c_credit "
                           + "FROM warehouse w "
                           + "INNER JOIN customer c ON c.c_w_id = w.w_id "
                           + "WHERE w.w_id = $int AND c.c_d_id = $int AND c.c_id = $int",
                           w_id, d_id, c_id);
            
            var rs02 = fetchAsArray("SELECT /* N-02 */ d_tax, d_next_o_id "
                           + "FROM district "
                           + "WHERE d_w_id = $int AND d_id = $int "
                           + "FOR UPDATE",
                           w_id, d_id);
            
            var uc03 = execute("UPDATE /* N-03 */ district "
                           + "SET d_next_o_id = d_next_o_id + 1 "
                           + "WHERE d_w_id = $int AND d_id = $int",
                           w_id, d_id);
            
            var uc04 = execute("INSERT /* N-04 */ INTO orders "
                           + "(o_id, o_d_id, o_w_id, o_c_id, o_entry_d, "
                           + "o_carrier_id, o_ol_cnt, o_all_local) "
                           + "VALUES ($int, $int, $int, $int, $timestamp, "
                           + "NULL, $int, $int)",
                           rs02[0][1], d_id, w_id, c_id, new Date(),
                           ol_cnt, all_local);
            
            var uc05 = execute("INSERT /* N-05 */ INTO new_orders "
                           + "(no_o_id, no_d_id, no_w_id) "
                           + "VALUES ($int, $int, $int)",
                           rs02[0][1], d_id, w_id);
            
            for (var index = 0; index < ol_cnt; index++) {
                var rs06 = fetchAsArray("SELECT /* N-06 */ i_price, i_name, i_data "
                               + "FROM item "
                               + "WHERE i_id = $int",
                               i_id[order[index]]);
                
                if (rs06.length == 0) {
                    // An user data entry error occurred.
                    rollback();
                    return;
                }
                
                var rs07 = fetchAsArray("SELECT /* N-07 */ s_quantity, "
                               + "s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05, "
                               + "s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, s_data "
                               + "FROM stock "
                               + "WHERE s_i_id = $int AND s_w_id = $int "
                               + "FOR UPDATE",
                               i_id[order[index]], supply_w_id[order[index]]);
                
                var stock = Number(rs07[0][0]) - quantity[order[index]];
                
                if (stock < 10) {
                    // Notice: The sample code <A.1> is different from the specification <2.4.2.2>.
                    // The items will be filled up before running out of stock.
                    stock += 91;
                }
                
                var remote = (w_id == supply_w_id[order[index]]) ? 0 : 1;
                
                // Notice: The sample code <A.1> is different from the specification <2.4.2.2>.
                // The sample code only updates s_quantity.
                var uc08 = execute("UPDATE /* N-08 */ stock "
                               + "SET s_quantity = $int, s_ytd = s_ytd + $int, "
                               + "s_order_cnt = s_order_cnt + 1, "
                               + "s_remote_cnt = s_remote_cnt + $int "
                               + "WHERE s_i_id = $int AND s_w_id = $int",
                               stock, quantity[order[index]], remote,
                               i_id[order[index]], supply_w_id[order[index]]);
                
                // Notice: The sample code <A.1> is different from the specification <2.4.2.2>.
                // c_discount, w_tax, d_tax are used for calculating total-amount, not ol_amount.
                var uc09 = execute("INSERT /* N-09 */ INTO order_line "
                               + "(ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, "
                               + "ol_supply_w_id, ol_delivery_d, ol_quantity, "
                               + "ol_amount, ol_dist_info) "
                               + "VALUES ($int, $int, $int, $int, $int, "
                               + "$int, NULL, $int, "
                               + "$double, $string)",
                               rs02[0][1], d_id, w_id, index + 1, i_id[order[index]],
                               supply_w_id[order[index]], quantity[order[index]],
                               quantity[order[index]] * Number(rs06[0][0]), rs07[0][d_id]);
            }
            
            commit();
            return;
            
        } catch (e) {
            if (isDeadlock(e)) {
                warn("[Agent " + getId() + "] " + e.javaException + getScriptStackTrace(e));
                rollback();
            } else {
                error(e + getScriptStackTrace(e));
            }
        }
    }
    
    error("The deadlock retry limit is reached.");
}

function payment() {
    var w_id = random(1, scale);
    var d_id = random(1, 10);
    var c_w_id = 0;
    var c_d_id = 0;
    var byName = false;
    var c_last = "";
    var c_id = 0;
    var h_amount = random(100, 500000) / 100;
    
    if (random(1, 100) <= 85) {
        // The customer is paying through his/her own warehouse.
        c_w_id = w_id;
        c_d_id = d_id;
    } else {
        // The customer is paying through a warehouse and a district other than his/her own.
        if (scale > 1) {
            c_w_id = random(1, scale - 1);
            
            if (c_w_id >= w_id) {
                c_w_id++;
            }
        } else {
            c_w_id = 1;
        }
        
        c_d_id = random(1, 10);
    }
    
    if (random(1, 100) <= 60) {
        // The customer is using his/her last name
        // and is one of the possibly several customers with that last name.
        byName = true;
        c_last = lastName(nonUniformRandom(255, 0, 999));
    } else {
        // The customer is using his/her customer number.
        byName = false;
        c_id = nonUniformRandom(1023, 1, 3000);
    }
    
    for (var retry = 0; retry <= DEADLOCK_RETRY_LIMIT; retry++) {
        try {
            var rs01 = fetchAsArray("SELECT /* P-01 */ "
                           + "w_name, w_street_1, w_street_2, w_city, w_state, w_zip "
                           + "FROM warehouse "
                           + "WHERE w_id = $int "
                           + "FOR UPDATE",
                           w_id);
            
            var uc02 = execute("UPDATE /* P-02 */ warehouse "
                           + "SET w_ytd = w_ytd + $double "
                           + "WHERE w_id = $int",
                           h_amount,
                           w_id);
            
            var rs03 = fetchAsArray("SELECT /* P-03 */ "
                           + "d_name, d_street_1, d_street_2, d_city, d_state, d_zip "
                           + "FROM district "
                           + "WHERE d_w_id = $int AND d_id = $int "
                           + "FOR UPDATE",
                           w_id, d_id);
            
            var uc04 = execute("UPDATE /* P-04 */ district "
                           + "SET d_ytd = d_ytd + $double "
                           + "WHERE d_w_id = $int AND d_id = $int",
                           h_amount,
                           w_id, d_id);
            
            if (byName) {
                var rs05 = fetchAsArray("SELECT /* P-05 */ c_id "
                               + "FROM customer "
                               + "WHERE c_w_id = $int AND c_d_id = $int AND c_last = $string "
                               + "ORDER BY c_first",
                               c_w_id, c_d_id, c_last);
                
                if (rs05.length % 2 == 0) {
                    // Let n be the number of rows selected.
                    // The customer data is retrieved from the row at position n/2
                    // in the sorted set of selected rows from the CUSTOMER table.
                    c_id = Number(rs05[rs05.length / 2 - 1][0]);
                } else {
                    // The row position is rounded up to the next integer.
                    c_id = Number(rs05[(rs05.length + 1) / 2 - 1][0]);
                }
            }
            
            var rs06 = fetchAsArray("SELECT /* P-06 */ "
                           + "c_first, c_middle, c_last, c_street_1, c_street_2, "
                           + "c_city, c_state, c_zip, c_phone, c_since, c_credit, "
                           + "c_credit_lim, c_discount, c_balance, c_data "
                           + "FROM customer "
                           + "WHERE c_w_id = $int AND c_d_id = $int AND c_id = $int "
                           + "FOR UPDATE",
                           c_w_id, c_d_id, c_id);
            
            if (rs06[0][10] == "BC") {
                // If the value of C_CREDIT is equal to "BC",
                // the following history information: C_ID, C_D_ID, C_W_ID, D_ID, W_ID,
                // and H_AMOUNT, are inserted at the left of the C_DATA field by shifting
                // the existing content of C_DATA to the right by an equal number of bytes.
                
                // Notice: The sample code <A.2> is different from the specification <2.5.2.2>.
                // According to the specification, h_date is not used to build c_data.
                var c_data = ("| " + c_id + " " + c_d_id + " " + c_w_id + " " + d_id + " "
                                 + w_id + " " + h_amount + " " + rs06[0][14]).substring(0, 500);
                
                // Notice: The sample code <A.2> is different from the specification <2.5.2.2>.
                // The sample code only updates c_balance and c_data.
                var uc07 = execute("UPDATE /* P-07 */ customer "
                               + "SET c_balance = c_balance - $double, "
                               + "c_ytd_payment = c_ytd_payment + $double, "
                               + "c_payment_cnt = c_payment_cnt + 1, "
                               + "c_data = $string "
                               + "WHERE c_w_id = $int AND c_d_id = $int AND c_id = $int",
                               h_amount, h_amount, c_data,
                               c_w_id, c_d_id, c_id);
            } else {
                // Notice: The sample code <A.2> is different from the specification <2.5.2.2>.
                // The sample code only updates c_balance.
                var uc08 = execute("UPDATE /* P-08 */ customer "
                               + "SET c_balance = c_balance - $double, "
                               + "c_ytd_payment = c_ytd_payment + $double, "
                               + "c_payment_cnt = c_payment_cnt + 1 "
                               + "WHERE c_w_id = $int AND c_d_id = $int AND c_id = $int",
                               h_amount, h_amount,
                               c_w_id, c_d_id, c_id);
            }
            
            var h_data = rs01[0][0] + "    " + rs03[0][0];
            
            var uc09 = execute("INSERT /* P-09 */ INTO history "
                           + "(h_c_id, h_c_d_id, h_c_w_id, h_d_id, h_w_id, "
                           + "h_date, h_amount, h_data) "
                           + "VALUES ($int, $int, $int, $int, $int, "
                           + "$timestamp, $double, $string)",
                           c_id, c_d_id, c_w_id, d_id, w_id,
                           new Date(), h_amount, h_data);
            
            commit();
            return;
            
        } catch (e) {
            if (isDeadlock(e)) {
                warn("[Agent " + getId() + "] " + e.javaException + getScriptStackTrace(e));
                rollback();
            } else {
                error(e + getScriptStackTrace(e));
            }
        }
    }
    
    error("The deadlock retry limit is reached.");
}

function orderStatus() {
    var c_w_id = random(1, scale);
    var c_d_id = random(1, 10);
    var byName = false;
    var c_last = "";
    var c_id = 0;
    
    if (random(1, 100) <= 60) {
        // The customer is using his/her last name
        // and is one of the possibly several customers with that last name.
        byName = true;
        c_last = lastName(nonUniformRandom(255, 0, 999));
    } else {
        // The customer is using his/her customer number.
        byName = false;
        c_id = nonUniformRandom(1023, 1, 3000);
    }
    
    if (byName) {
        var rs01 = fetchAsArray("SELECT /* O-01 */ c_id "
                       + "FROM customer "
                       + "WHERE c_w_id = $int AND c_d_id = $int AND c_last = $string "
                       + "ORDER BY c_first",
                       c_w_id, c_d_id, c_last);
        
        if (rs01.length % 2 == 0) {
            // Let n be the number of rows selected.
            // The customer data is retrieved from the row at position n/2
            // in the sorted set of selected rows from the CUSTOMER table.
            c_id = Number(rs01[rs01.length / 2 - 1][0]);
        } else {
            // The row position is rounded up to the next integer.
            c_id = Number(rs01[(rs01.length + 1) / 2 - 1][0]);
        }
    }
    
    var rc02 = query("SELECT /* O-02 */ c_balance, c_first, c_middle, c_last "
                   + "FROM customer "
                   + "WHERE c_w_id = $int AND c_d_id = $int AND c_id = $int",
                   c_w_id, c_d_id, c_id);
    
    var rs03 = fetchAsArray("SELECT /* O-03 */ o1.o_id, o1.o_entry_d, o1.o_carrier_id "
                   + "FROM orders o1 "
                   + "WHERE o1.o_w_id = $int AND o1.o_d_id = $int "
                   + "AND o1.o_id = ("
                       + "SELECT MAX(o2.o_id) "
                       + "FROM orders o2 "
                       + "WHERE o2.o_w_id = $int AND o2.o_d_id = $int AND o2.o_c_id = $int"
                   + ")",
                   c_w_id, c_d_id, c_w_id, c_d_id, c_id);
    
    var rc04 = query("SELECT /* O-04 */ "
                   + "ol_i_id, ol_supply_w_id, ol_quantity, ol_amount, ol_delivery_d "
                   + "FROM order_line "
                   + "WHERE ol_w_id = $int AND ol_d_id = $int AND ol_o_id = $int",
                   c_w_id, c_d_id, rs03[0][0]);
    
    commit();
}

function delivery() {
    var w_id = random(1, scale);
    var o_carrier_id = random(1, 10);
    
    for (var retry = 0; retry <= DEADLOCK_RETRY_LIMIT; retry++) {
        try {
            for (var d_id = 1; d_id <= 10; d_id++) {
                var rs01 = fetchAsArray("SELECT /* D-01 */ n1.no_o_id "
                               + "FROM new_orders n1 "
                               + "WHERE n1.no_w_id = $int AND n1.no_d_id = $int "
                               + "AND n1.no_o_id = ("
                                   + "SELECT MIN(n2.no_o_id) "
                                   + "FROM new_orders n2 "
                                   + "WHERE n2.no_w_id = $int AND n2.no_d_id = $int"
                               + ") "
                               + "FOR UPDATE",
                               w_id, d_id, w_id, d_id);
                
                if (rs01.length == 0) {
                    // If no matching row is found,
                    // then the delivery of an order for this district is skipped.
                    continue;
                }
                
                var uc02 = execute("DELETE /* D-02 */ "
                               + "FROM new_orders "
                               + "WHERE no_w_id = $int AND no_d_id = $int AND no_o_id = $int",
                               w_id, d_id, rs01[0][0]);
                
                var rs03 = fetchAsArray("SELECT /* D-03 */ o_c_id "
                               + "FROM orders "
                               + "WHERE o_w_id = $int AND o_d_id = $int AND o_id = $int "
                               + "FOR UPDATE",
                               w_id, d_id, rs01[0][0]);
                
                var uc04 = execute("UPDATE /* D-04 */ orders "
                               + "SET o_carrier_id = $int "
                               + "WHERE o_w_id = $int AND o_d_id = $int AND o_id = $int",
                               o_carrier_id,
                               w_id, d_id, rs01[0][0]);
                
                var uc05 = execute("UPDATE /* D-05 */ order_line "
                               + "SET ol_delivery_d = $timestamp "
                               + "WHERE ol_w_id = $int AND ol_d_id = $int AND ol_o_id = $int",
                               new Date(), w_id, d_id, rs01[0][0]);
                
                var rs06 = fetchAsArray("SELECT /* D-06 */ SUM(ol_amount) "
                               + "FROM order_line "
                               + "WHERE ol_w_id = $int AND ol_d_id = $int AND ol_o_id = $int",
                               w_id, d_id, rs01[0][0]);
                
                // Notice: The sample code <A.4> is different from the specification <2.7.4.2>.
                // The sample code only updates c_balance.
                var uc07 = execute("UPDATE /* D-07 */ customer "
                               + "SET c_balance = c_balance + $double, "
                               + "c_delivery_cnt = c_delivery_cnt + 1 "
                               + "WHERE c_w_id = $int AND c_d_id = $int AND c_id = $int",
                               rs06[0][0], w_id, d_id, rs03[0][0]);
            }
            
            commit();
            return;
            
        } catch (e) {
            if (isDeadlock(e)) {
                warn("[Agent " + getId() + "] " + e.javaException + getScriptStackTrace(e));
                rollback();
            } else {
                error(e + getScriptStackTrace(e));
            }
        }
    }
    
    error("The deadlock retry limit is reached.");
}

function stockLevel() {
    var w_id = random(1, scale);
    var d_id = random(1, 10);
    var threshold = random(10, 20);
    
    var rc01 = query("SELECT /* S-01 */ /*+ USE_NL(ol s) */ COUNT(DISTINCT s.s_i_id) "
                   + "FROM district d "
                   + "INNER JOIN order_line ol ON ol.ol_w_id = d.d_w_id AND ol.ol_d_id = d.d_id "
                       + "AND ol.ol_o_id BETWEEN d.d_next_o_id - 20 AND d.d_next_o_id - 1 "
                   + "INNER JOIN stock s ON s.s_w_id = ol.ol_w_id AND s.s_i_id = ol.ol_i_id "
                   + "WHERE d.d_w_id = $int AND d.d_id = $int AND s.s_quantity < $int",
                   w_id, d_id, threshold);
    
    commit();
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

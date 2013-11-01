function getId() {
    return helper.getId();
}

function setBreak() {
    helper.setBreak();
}

function setTxType(txType) {
    helper.setTxType(txType);
}

function getData(key) {
    return helper.getData(key);
}

function putData(key, value) {
    helper.putData(key, value);
}

function random(min, max) {
    if (min > max) {
        throw "Error: min value is greater than max value at random(min, max).";
    }
    
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomString(length) {
    if (length < 0) {
        throw "Error: length is less than 0 at randomString(length).";
    }
    
    return String(helper.getRandomString(length));
}

function setRandomStringElements(elements) {
    helper.setRandomStringElements(elements);
}

function takeConnection() {
    return helper.takeConnection();
}

function getDatabaseProductName() {
    return helper.getDatabaseProductName();
}

function getDatabaseMajorVersion() {
    return helper.getDatabaseMajorVersion();
}

function getDatabaseMinorVersion() {
	return helper.getDatabaseMinorVersion();
}

function commit() {
    helper.commit();
}

function rollback() {
    helper.rollback();
}

function query() {
    return helper.query(arguments[0], Array.slice(arguments, 1));
}

function fetchAsArray() {
    return helper.fetchAsArray(arguments[0], Array.slice(arguments, 1));
}

function execute() {
    return helper.execute(arguments[0], Array.slice(arguments, 1));
}

function executeBatch() {
    return helper.executeBatch(arguments[0], Array.slice(arguments, 1));
}

function trace(message) {
    helper.trace(message);
}

function debug(message) {
    helper.debug(message);
}

function info(message) {
    helper.info(message);
}

function warn(message) {
    helper.warn(message);
}

function error(message) {
    helper.error(message);
}

function getScriptStackTrace(exception) {
    return helper.getScriptStackTrace(exception.rhinoException);
}

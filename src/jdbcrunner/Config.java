package jdbcrunner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;

/**
 * 負荷テストの設定を格納するクラスです。
 * <p>
 * このクラスのインスタンスは不変です。
 *
 * @author Sadao Hiratsuka
 */
public final class Config {
	/**
	 * ログのファイル名です。
	 */
	public static final String LOG4J_FILENAME = "jdbcrunner.log"; //$NON-NLS-1$

	/**
	 * ヘルパースクリプトのファイル名です。
	 */
	public static final String HELPER_FILENAME = "helper.js"; //$NON-NLS-1$

	// スクリプトから指定できるパラメータ

	/**
	 * JDBCドライバのクラス名を格納するスクリプトの変数名です。
	 */
	public static final String VAR_JDBC_DRIVER = "jdbcDriver"; //$NON-NLS-1$

	/**
	 * JDBC接続URLを格納するスクリプトの変数名です。
	 */
	public static final String VAR_JDBC_URL = "jdbcUrl"; //$NON-NLS-1$

	/**
	 * データベースのユーザ名を格納するスクリプトの変数名です。
	 */
	public static final String VAR_JDBC_USER = "jdbcUser"; //$NON-NLS-1$

	/**
	 * データベースユーザのパスワードを格納するスクリプトの変数名です。
	 */
	public static final String VAR_JDBC_PASS = "jdbcPass"; //$NON-NLS-1$

	/**
	 * ロードモードの有効/無効を格納するスクリプトの変数名です。
	 */
	public static final String VAR_LOAD = "isLoad"; //$NON-NLS-1$

	/**
	 * 測定前にあらかじめ負荷をかけておく時間を格納するスクリプトの変数名です。
	 */
	public static final String VAR_WARMUP_TIME = "warmupTime"; //$NON-NLS-1$

	/**
	 * 測定時間を格納するスクリプトの変数名です。
	 */
	public static final String VAR_MEASUREMENT_TIME = "measurementTime"; //$NON-NLS-1$

	/**
	 * トランザクションの種類数を格納するスクリプトの変数名です。
	 */
	public static final String VAR_N_TX_TYPES = "nTxTypes"; //$NON-NLS-1$

	/**
	 * エージェント数を格納するスクリプトの変数名です。
	 */
	public static final String VAR_N_AGENTS = "nAgents"; //$NON-NLS-1$

	/**
	 * コネクションプールの物理接続数を格納するスクリプトの変数名です。
	 */
	public static final String VAR_CONN_POOL_SIZE = "connPoolSize"; //$NON-NLS-1$

	/**
	 * コネクションあたりの文キャッシュ数を格納するスクリプトの変数名です。
	 */
	public static final String VAR_STMT_CACHE_SIZE = "stmtCacheSize"; //$NON-NLS-1$

	/**
	 * オートコミットモードの有効/無効を格納するスクリプトの変数名です。
	 */
	public static final String VAR_AUTO_COMMIT = "isAutoCommit"; //$NON-NLS-1$

	/**
	 * トランザクションごとのスリープ時間を格納するスクリプトの変数名です。
	 */
	public static final String VAR_SLEEP_TIME = "sleepTime"; //$NON-NLS-1$

	/**
	 * スループットの上限値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_THROTTLE = "throttle"; //$NON-NLS-1$

	/**
	 * デバッグモードの有効/無効を格納するスクリプトの変数名です。
	 */
	public static final String VAR_DEBUG = "isDebug"; //$NON-NLS-1$

	/**
	 * トレースモードの有効/無効を格納するスクリプトの変数名です。
	 */
	public static final String VAR_TRACE = "isTrace"; //$NON-NLS-1$

	/**
	 * ログの出力先ディレクトリを格納するスクリプトの変数名です。
	 */
	public static final String VAR_LOG_DIR = "logDir"; //$NON-NLS-1$

	/**
	 * -param0オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_0 = "param0"; //$NON-NLS-1$

	/**
	 * -param1オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_1 = "param1"; //$NON-NLS-1$

	/**
	 * -param2オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_2 = "param2"; //$NON-NLS-1$

	/**
	 * -param3オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_3 = "param3"; //$NON-NLS-1$

	/**
	 * -param4オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_4 = "param4"; //$NON-NLS-1$

	/**
	 * -param5オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_5 = "param5"; //$NON-NLS-1$

	/**
	 * -param6オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_6 = "param6"; //$NON-NLS-1$

	/**
	 * -param7オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_7 = "param7"; //$NON-NLS-1$

	/**
	 * -param8オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_8 = "param8"; //$NON-NLS-1$

	/**
	 * -param9オプションで指定された値を格納するスクリプトの変数名です。
	 */
	public static final String VAR_PARAM_9 = "param9"; //$NON-NLS-1$

	private static final String SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	// コマンドラインオプションから指定できるパラメータ
	private static final String OPT_SCRIPT_CHARSET = "scriptCharset"; //$NON-NLS-1$
	private static final String OPT_JDBC_DRIVER = "jdbcDriver"; //$NON-NLS-1$
	private static final String OPT_JDBC_URL = "jdbcUrl"; //$NON-NLS-1$
	private static final String OPT_JDBC_USER = "jdbcUser"; //$NON-NLS-1$
	private static final String OPT_JDBC_PASS = "jdbcPass"; //$NON-NLS-1$
	private static final String OPT_WARMUP_TIME = "warmupTime"; //$NON-NLS-1$
	private static final String OPT_MEASUREMENT_TIME = "measurementTime"; //$NON-NLS-1$
	private static final String OPT_N_AGENTS = "nAgents"; //$NON-NLS-1$
	private static final String OPT_CONN_POOL_SIZE = "connPoolSize"; //$NON-NLS-1$
	private static final String OPT_STMT_CACHE_SIZE = "stmtCacheSize"; //$NON-NLS-1$
	private static final String OPT_AUTO_COMMIT = "autoCommit"; //$NON-NLS-1$
	private static final String OPT_SLEEP_TIME = "sleepTime"; //$NON-NLS-1$
	private static final String OPT_THROTTLE = "throttle"; //$NON-NLS-1$
	private static final String OPT_DEBUG = "debug"; //$NON-NLS-1$
	private static final String OPT_TRACE = "trace"; //$NON-NLS-1$
	private static final String OPT_LOG_DIR = "logDir"; //$NON-NLS-1$
	private static final String OPT_PARAM_0 = "param0"; //$NON-NLS-1$
	private static final String OPT_PARAM_1 = "param1"; //$NON-NLS-1$
	private static final String OPT_PARAM_2 = "param2"; //$NON-NLS-1$
	private static final String OPT_PARAM_3 = "param3"; //$NON-NLS-1$
	private static final String OPT_PARAM_4 = "param4"; //$NON-NLS-1$
	private static final String OPT_PARAM_5 = "param5"; //$NON-NLS-1$
	private static final String OPT_PARAM_6 = "param6"; //$NON-NLS-1$
	private static final String OPT_PARAM_7 = "param7"; //$NON-NLS-1$
	private static final String OPT_PARAM_8 = "param8"; //$NON-NLS-1$
	private static final String OPT_PARAM_9 = "param9"; //$NON-NLS-1$

	// パラメータ
	private String scriptFilename;
	private String scriptCharset;
	private String jdbcDriver = ""; //$NON-NLS-1$
	private String jdbcUrl = "jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true"; //$NON-NLS-1$
	private String jdbcUser = ""; //$NON-NLS-1$
	private String jdbcPass = ""; //$NON-NLS-1$
	private boolean isLoad = false;
	private int warmupTime = 10;
	private int measurementTime = 60;
	private int nTxTypes = 1;
	private int nAgents = 1;
	private int connPoolSize = 1;
	private int stmtCacheSize = 10;
	private boolean isAutoCommit = true;
	private long[] sleepTimes = new long[] { 0L };
	private int[] throttles = new int[] { 0 };
	private boolean isDebug = false;
	private boolean isTrace = false;
	private String logDir = "."; //$NON-NLS-1$
	private int param0 = 0;
	private int param1 = 0;
	private int param2 = 0;
	private int param3 = 0;
	private int param4 = 0;
	private int param5 = 0;
	private int param6 = 0;
	private int param7 = 0;
	private int param8 = 0;
	private int param9 = 0;

	private String programStartTime;
	private String helperScript;
	private String scenarioScript;
	private boolean doThrottleByTotal = true;

	/**
	 * コマンドラインオプションを基に負荷テストの設定を構築します。
	 * <p>
	 * <ol>
	 * <li>1番目の要素からスクリプトのファイル名を取得します。
	 * <li>2番目以降の要素からオプションを構築します。
	 * <li>オプションから以下の設定を読み込みます。
	 * <ul>
	 * <li>スクリプトの文字セット
	 * <li>変数param0に設定する値
	 * <li>変数param1に設定する値
	 * <li>変数param2に設定する値
	 * <li>変数param3に設定する値
	 * <li>変数param4に設定する値
	 * <li>変数param5に設定する値
	 * <li>変数param6に設定する値
	 * <li>変数param7に設定する値
	 * <li>変数param8に設定する値
	 * <li>変数param9に設定する値
	 * </ul>
	 * <li>スクリプトを評価して以下の設定を読み込みます。
	 * <ul>
	 * <li>JDBCドライバのクラス名
	 * <li>JDBC接続URL
	 * <li>データベースのユーザ名
	 * <li>データベースユーザのパスワード
	 * <li>ロードモードの有効/無効フラグ
	 * <li>測定前にあらかじめ負荷をかけておく時間
	 * <li>測定時間
	 * <li>トランザクションの種類数
	 * <li>エージェント数
	 * <li>コネクションプールの物理接続数
	 * <li>コネクションあたりの文キャッシュ数
	 * <li>オートコミットモードの有効/無効フラグ
	 * <li>トランザクションごとのスリープ時間
	 * <li>スループットの上限値
	 * <li>デバッグモードの有効/無効フラグ
	 * <li>トレースモードの有効/無効フラグ
	 * <li>ログの出力先ディレクトリ
	 * </ul>
	 * <li>オプションから以下の設定を読み込みます。
	 * スクリプトに同じ設定が存在する場合はオプションの設定で上書きします。
	 * <ul>
	 * <li>JDBCドライバのクラス名
	 * <li>JDBC接続URL
	 * <li>データベースのユーザ名
	 * <li>データベースユーザのパスワード
	 * <li>測定前にあらかじめ負荷をかけておく時間
	 * <li>測定時間
	 * <li>エージェント数
	 * <li>コネクションプールの物理接続数
	 * <li>コネクションあたりの文キャッシュ数
	 * <li>オートコミットモードの有効/無効フラグ
	 * <li>トランザクションごとのスリープ時間
	 * <li>スループットの上限値
	 * <li>デバッグモードの有効/無効フラグ
	 * <li>トレースモードの有効/無効フラグ
	 * <li>ログの出力先ディレクトリ
	 * </ul>
	 * </ol>
	 *
	 * @param args
	 *            コマンドラインオプション
	 * @throws ApplicationException
	 *             オプションの指定に誤りがある場合、
	 *             スクリプトファイルを開けない場合、
	 *             スクリプトの文法に誤りがある場合、
	 *             スクリプトの設定に誤りがある場合
	 */
	public Config(String[] args) throws ApplicationException {
		this.programStartTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()); //$NON-NLS-1$

		// スクリプトのファイル名を取得する
		if (args.length < 1) {
			throw new ApplicationException(
					Resources.getString("Config.ILLEGAL_SCRIPT_NOT_SPECIFIED")); //$NON-NLS-1$
		}

		this.scriptFilename = args[0];

		// コマンドラインオプションを読み込む
		Options options = new Options();
		CommandLineParser parser = new BasicParser();
		CommandLine cl = null;
		Config.buildOptions(options);

		try {
			cl = parser.parse(options, Arrays.copyOfRange(args, 1, args.length), true);
		} catch (ParseException e) {
			throw new ApplicationException(Resources.getString("Config.PARSE_EXCEPTION"), e); //$NON-NLS-1$
		}

		if (cl.getArgList().size() > 0) {
			throw new ApplicationException(Resources.getString("Config.ILLEGAL_OPTION_1") //$NON-NLS-1$
					+ cl.getArgList().get(0) + Resources.getString("Config.ILLEGAL_OPTION_2")); //$NON-NLS-1$
		}

		// スクリプト構築前に設定する必要があるコマンドラインオプションを読み込む
		loadOptionsAhead(cl);

		// ヘルパースクリプトを読み込む
		this.helperScript = loadHelperScript(HELPER_FILENAME);

		// スクリプトを読み込む
		this.scenarioScript = loadScenarioScript(scriptFilename, scriptCharset);

		// スクリプトから設定を読み込む
		Script script = null;

		try {
			script = new Script(this, null, false);
			loadVariables(script);
		} finally {
			if (script != null) {
				script.close();
			}
		}

		// コマンドラインオプションを読み込んでスクリプトの設定を上書きする
		loadOptions(cl);
	}

	/**
	 * ヘルプメッセージを返します。
	 *
	 * @return ヘルプメッセージ
	 */
	public static String getHelpMessage() {
		Options options = new Options();
		HelpFormatter formatter = new HelpFormatter();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		Config.buildOptions(options);
		formatter.printHelp(pw, 80, "java JR <script> [options]", null, options, 0, 0, null); //$NON-NLS-1$
		pw.flush();
		pw.close();

		return sw.toString();
	}

	private static void buildOptions(Options options) {
		options.addOption(OPT_SCRIPT_CHARSET, true,
				Resources.getString("Config.USAGE_SCRIPT_CHARSET")); //$NON-NLS-1$

		options.addOption(OPT_JDBC_DRIVER, true, Resources.getString("Config.USAGE_JDBC_DRIVER")); //$NON-NLS-1$
		options.addOption(OPT_JDBC_URL, true, Resources.getString("Config.USAGE_JDBC_URL")); //$NON-NLS-1$
		options.addOption(OPT_JDBC_USER, true, Resources.getString("Config.USAGE_JDBC_USER")); //$NON-NLS-1$
		options.addOption(OPT_JDBC_PASS, true, Resources.getString("Config.USAGE_JDBC_PASS")); //$NON-NLS-1$
		options.addOption(OPT_WARMUP_TIME, true, Resources.getString("Config.USAGE_WARMUP_TIME")); //$NON-NLS-1$

		options.addOption(OPT_MEASUREMENT_TIME, true,
				Resources.getString("Config.USAGE_MEASUREMENT_TIME")); //$NON-NLS-1$

		options.addOption(OPT_N_AGENTS, true, Resources.getString("Config.USAGE_N_AGENTS")); //$NON-NLS-1$

		options.addOption(OPT_CONN_POOL_SIZE, true,
				Resources.getString("Config.USAGE_CONN_POOL_SIZE")); //$NON-NLS-1$

		options.addOption(OPT_STMT_CACHE_SIZE, true,
				Resources.getString("Config.USAGE_STMT_CACHE_SIZE")); //$NON-NLS-1$

		options.addOption(OPT_AUTO_COMMIT, true, Resources.getString("Config.USAGE_AUTO_COMMIT")); //$NON-NLS-1$
		options.addOption(OPT_SLEEP_TIME, true, Resources.getString("Config.USAGE_SLEEP_TIME")); //$NON-NLS-1$
		options.addOption(OPT_THROTTLE, true, Resources.getString("Config.USAGE_THROTTLE")); //$NON-NLS-1$
		options.addOption(OPT_DEBUG, false, Resources.getString("Config.USAGE_DEBUG")); //$NON-NLS-1$
		options.addOption(OPT_TRACE, false, Resources.getString("Config.USAGE_TRACE")); //$NON-NLS-1$
		options.addOption(OPT_LOG_DIR, true, Resources.getString("Config.USAGE_LOG_DIR")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_0, true, Resources.getString("Config.USAGE_PARAM_0")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_1, true, Resources.getString("Config.USAGE_PARAM_1")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_2, true, Resources.getString("Config.USAGE_PARAM_2")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_3, true, Resources.getString("Config.USAGE_PARAM_3")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_4, true, Resources.getString("Config.USAGE_PARAM_4")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_5, true, Resources.getString("Config.USAGE_PARAM_5")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_6, true, Resources.getString("Config.USAGE_PARAM_6")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_7, true, Resources.getString("Config.USAGE_PARAM_7")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_8, true, Resources.getString("Config.USAGE_PARAM_8")); //$NON-NLS-1$
		options.addOption(OPT_PARAM_9, true, Resources.getString("Config.USAGE_PARAM_9")); //$NON-NLS-1$
	}

	/**
	 * 設定の一覧を返します。
	 *
	 * @return 設定の一覧
	 */
	public String getConfigString() {
		StringBuilder configString = new StringBuilder();

		configString.append("[Config]" + SEPARATOR); //$NON-NLS-1$
		configString.append("Program start time   : " + programStartTime + SEPARATOR); //$NON-NLS-1$
		configString.append("Script filename      : " + scriptFilename + SEPARATOR); //$NON-NLS-1$
		configString.append("JDBC driver          : "); //$NON-NLS-1$

		if (jdbcDriver.equals("")) { //$NON-NLS-1$
			configString.append("-"); //$NON-NLS-1$
		} else {
			configString.append(jdbcDriver);
		}

		configString.append(SEPARATOR);
		configString.append("JDBC URL             : " + jdbcUrl + SEPARATOR); //$NON-NLS-1$
		configString.append("JDBC user            : " + jdbcUser + SEPARATOR); //$NON-NLS-1$

		if (isLoad) {
			configString.append("Load mode            : " + isLoad + SEPARATOR); //$NON-NLS-1$
			configString.append("Number of agents     : " + nAgents + SEPARATOR); //$NON-NLS-1$
			configString.append("Auto commit          : " + isAutoCommit + SEPARATOR); //$NON-NLS-1$
		} else {
			configString.append("Warmup time          : " + warmupTime //$NON-NLS-1$
					+ " sec" + SEPARATOR); //$NON-NLS-1$

			configString.append("Measurement time     : " + measurementTime //$NON-NLS-1$
					+ " sec" + SEPARATOR); //$NON-NLS-1$

			configString.append("Number of tx types   : " + nTxTypes + SEPARATOR); //$NON-NLS-1$
			configString.append("Number of agents     : " + nAgents + SEPARATOR); //$NON-NLS-1$
			configString.append("Connection pool size : " + connPoolSize + SEPARATOR); //$NON-NLS-1$
			configString.append("Statement cache size : " + stmtCacheSize + SEPARATOR); //$NON-NLS-1$
			configString.append("Auto commit          : " + isAutoCommit + SEPARATOR); //$NON-NLS-1$
			configString.append("Sleep time           : "); //$NON-NLS-1$

			for (int txType = 0; txType < nTxTypes; txType++) {
				configString.append(sleepTimes[txType]);
				configString.append(","); //$NON-NLS-1$
			}

			configString.deleteCharAt(configString.length() - 1);
			configString.append(" msec" + SEPARATOR); //$NON-NLS-1$
			configString.append("Throttle             : "); //$NON-NLS-1$

			for (int txType = 0; txType < nTxTypes; txType++) {
				int throttle = throttles[txType];

				if (throttle == 0) {
					configString.append("-,"); //$NON-NLS-1$
				} else {
					configString.append(throttle);
					configString.append(","); //$NON-NLS-1$
				}

				if (doThrottleByTotal()) {
					break;
				}
			}

			configString.deleteCharAt(configString.length() - 1);
			configString.append(" tps"); //$NON-NLS-1$

			if ((nTxTypes > 1) && doThrottleByTotal) {
				configString.append(" (total)"); //$NON-NLS-1$
			}

			configString.append(SEPARATOR);
		}

		configString.append("Debug mode           : " + isDebug + SEPARATOR); //$NON-NLS-1$
		configString.append("Trace mode           : " + isTrace + SEPARATOR); //$NON-NLS-1$
		configString.append("Log directory        : " + logDir + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 0          : " + param0 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 1          : " + param1 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 2          : " + param2 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 3          : " + param3 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 4          : " + param4 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 5          : " + param5 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 6          : " + param6 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 7          : " + param7 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 8          : " + param8 + SEPARATOR); //$NON-NLS-1$
		configString.append("Parameter 9          : " + param9); //$NON-NLS-1$

		return configString.toString();
	}

	/**
	 * プログラムの開始日時を返します。
	 *
	 * @return プログラムの開始日時
	 */
	public String getProgramStartTime() {
		return programStartTime;
	}

	/**
	 * ヘルパースクリプトを返します。
	 *
	 * @return ヘルパースクリプト
	 */
	public String getHelperScript() {
		return helperScript;
	}

	/**
	 * スクリプトのファイル名を返します。
	 *
	 * @return スクリプトのファイル名
	 */
	public String getScriptFilename() {
		return scriptFilename;
	}

	/**
	 * スクリプトの文字セットを返します。
	 *
	 * @return スクリプトの文字セット
	 */
	public String getScriptCharset() {
		return scriptCharset;
	}

	/**
	 * スクリプトを返します。
	 *
	 * @return スクリプト
	 */
	public String getScenarioScript() {
		return scenarioScript;
	}

	/**
	 * JDBCドライバのクラス名を返します。
	 *
	 * @return JDBCドライバのクラス名
	 */
	public String getJdbcDriver() {
		return jdbcDriver;
	}

	/**
	 * JDBC接続URLを返します。
	 *
	 * @return JDBC接続URL
	 */
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	/**
	 * データベースのユーザ名を返します。
	 *
	 * @return データベースのユーザ名
	 */
	public String getJdbcUser() {
		return jdbcUser;
	}

	/**
	 * データベースユーザのパスワードを返します。
	 *
	 * @return データベースユーザのパスワード
	 */
	public String getJdbcPass() {
		return jdbcPass;
	}

	/**
	 * ロードモードの有効/無効フラグを返します。
	 *
	 * @return ロードモードの有効/無効フラグ
	 */
	public boolean isLoad() {
		return isLoad;
	}

	/**
	 * 測定前にあらかじめ負荷をかけておく時間を返します。
	 *
	 * @return 測定前にあらかじめ負荷をかけておく時間
	 */
	public int getWarmupTime() {
		return warmupTime;
	}

	/**
	 * 測定時間を返します。
	 *
	 * @return 測定時間
	 */
	public int getMeasurementTime() {
		return measurementTime;
	}

	/**
	 * トランザクションの種類数を返します。
	 *
	 * @return トランザクションの種類数
	 */
	public int getNTxTypes() {
		return nTxTypes;
	}

	/**
	 * エージェント数を返します。
	 *
	 * @return エージェント数
	 */
	public int getNAgents() {
		return nAgents;
	}

	/**
	 * コネクションプールの物理接続数を返します。
	 *
	 * @return コネクションプールの物理接続数
	 */
	public int getConnPoolSize() {
		return connPoolSize;
	}

	/**
	 * コネクションあたりの文キャッシュ数を返します。
	 *
	 * @return コネクションあたりの文キャッシュ数
	 */
	public int getStmtCacheSize() {
		return stmtCacheSize;
	}

	/**
	 * オートコミットモードの有効/無効フラグを返します。
	 *
	 * @return オートコミットモードの有効/無効フラグ
	 */
	public boolean isAutoCommit() {
		return isAutoCommit;
	}

	/**
	 * トランザクションごとのスリープ時間を返します。
	 *
	 * @param txType
	 *            トランザクション種別
	 * @return トランザクションごとのスリープ時間
	 */
	public long getSleepTime(int txType) {
		return sleepTimes[txType];
	}

	/**
	 * スループットの上限値を返します。
	 *
	 * @param txType
	 *            トランザクション種別
	 * @return スループットの上限値
	 */
	public int getThrottle(int txType) {
		return throttles[txType];
	}

	/**
	 * スループット制限を合計スループットで行うかどうかのフラグを返します。
	 * <p>
	 * このフラグが{@code true}の場合、
	 * すべてのトランザクション種別のスループットを合計した値が
	 * 指定値になるようにスループットを制限します。このフラグが{@code false}
	 * の場合は、それぞれのトランザクション種別のスループットが指定値になるように
	 * スループットを制限します。
	 *
	 * @return スループット制限を合計スループットで行うかどうかのフラグ
	 */
	public boolean doThrottleByTotal() {
		return doThrottleByTotal;
	}

	/**
	 * デバッグモードの有効/無効フラグを返します。
	 *
	 * @return デバッグモードの有効/無効フラグ
	 */
	public boolean isDebug() {
		return isDebug;
	}

	/**
	 * トレースモードの有効/無効フラグを返します。
	 *
	 * @return トレースモードの有効/無効フラグ
	 */
	public boolean isTrace() {
		return isTrace;
	}

	/**
	 * ログの出力先ディレクトリを返します。
	 *
	 * @return ログの出力先ディレクトリ
	 */
	public String getLogDir() {
		return logDir;
	}

	/**
	 * -param0オプションで指定された値を返します。
	 *
	 * @return -param0オプションで指定された値
	 */
	public int getParam0() {
		return param0;
	}

	/**
	 * -param1オプションで指定された値を返します。
	 *
	 * @return -param1オプションで指定された値
	 */
	public int getParam1() {
		return param1;
	}

	/**
	 * -param2オプションで指定された値を返します。
	 *
	 * @return -param2オプションで指定された値
	 */
	public int getParam2() {
		return param2;
	}

	/**
	 * -param3オプションで指定された値を返します。
	 *
	 * @return -param3オプションで指定された値
	 */
	public int getParam3() {
		return param3;
	}

	/**
	 * -param4オプションで指定された値を返します。
	 *
	 * @return -param4オプションで指定された値
	 */
	public int getParam4() {
		return param4;
	}

	/**
	 * -param5オプションで指定された値を返します。
	 *
	 * @return -param5オプションで指定された値
	 */
	public int getParam5() {
		return param5;
	}

	/**
	 * -param6オプションで指定された値を返します。
	 *
	 * @return -param6オプションで指定された値
	 */
	public int getParam6() {
		return param6;
	}

	/**
	 * -param7オプションで指定された値を返します。
	 *
	 * @return -param7オプションで指定された値
	 */
	public int getParam7() {
		return param7;
	}

	/**
	 * -param8オプションで指定された値を返します。
	 *
	 * @return -param8オプションで指定された値
	 */
	public int getParam8() {
		return param8;
	}

	/**
	 * -param9オプションで指定された値を返します。
	 *
	 * @return -param9オプションで指定された値
	 */
	public int getParam9() {
		return param9;
	}

	private String loadHelperScript(String resourceName) throws ApplicationException {
		BufferedReader reader = null;
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourceName);

		if (is == null) {
			throw new ApplicationException(Resources.getString("Config.HELPER_SCRIPT_NOT_FOUND")); //$NON-NLS-1$
		}

		try {
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$
			return loadScript(reader);
		} catch (UnsupportedEncodingException e) {
			throw new ApplicationException(
					Resources.getString("Config.UNSUPPORTED_ENCODING_EXCEPTION"), e); //$NON-NLS-1$
		} catch (IOException e) {
			throw new ApplicationException(Resources.getString("Config.IO_EXCEPTION"), e); //$NON-NLS-1$
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// 何もしない
				}
			}
		}
	}

	private String loadScenarioScript(String fileName, String charset) throws ApplicationException {
		BufferedReader reader = null;

		try {
			if (charset == null) {
				reader = new BufferedReader(new FileReader(fileName));
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),
						charset));
			}

			return loadScript(reader);
		} catch (FileNotFoundException e) {
			throw new ApplicationException(
					Resources.getString("Config.FILE_NOT_FOUND_EXCEPTION"), e); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new ApplicationException(
					Resources.getString("Config.UNSUPPORTED_ENCODING_EXCEPTION"), e); //$NON-NLS-1$
		} catch (IOException e) {
			throw new ApplicationException(Resources.getString("Config.IO_EXCEPTION"), e); //$NON-NLS-1$
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// 何もしない
				}
			}
		}
	}

	private String loadScript(BufferedReader reader) throws IOException {
		StringBuilder buffer = new StringBuilder();
		String line = null;

		while ((line = reader.readLine()) != null) {
			buffer.append(line).append(SEPARATOR);
		}

		return buffer.toString();
	}

	private void loadOptionsAhead(CommandLine cl) throws ApplicationException {
		if (cl.hasOption(OPT_SCRIPT_CHARSET)) {
			this.scriptCharset = cl.getOptionValue(OPT_SCRIPT_CHARSET);
		}

		if (cl.hasOption(OPT_PARAM_0)) {
			try {
				this.param0 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_0));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_0"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_1)) {
			try {
				this.param1 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_1));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_1"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_2)) {
			try {
				this.param2 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_2));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_2"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_3)) {
			try {
				this.param3 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_3));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_3"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_4)) {
			try {
				this.param4 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_4));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_4"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_5)) {
			try {
				this.param5 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_5));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_5"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_6)) {
			try {
				this.param6 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_6));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_6"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_7)) {
			try {
				this.param7 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_7));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_7"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_8)) {
			try {
				this.param8 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_8));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_8"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_PARAM_9)) {
			try {
				this.param9 = Integer.parseInt(cl.getOptionValue(OPT_PARAM_9));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_PARAM_9"), e); //$NON-NLS-1$
			}
		}
	}

	private void loadVariables(Script script) throws ApplicationException {
		Object variable = null;

		variable = script.getVariable(VAR_JDBC_DRIVER);

		if (variable instanceof String) {
			this.jdbcDriver = (String) variable;
		}

		variable = script.getVariable(VAR_JDBC_URL);

		if (variable instanceof String) {
			this.jdbcUrl = (String) variable;
		}

		variable = script.getVariable(VAR_JDBC_USER);

		if (variable instanceof String) {
			this.jdbcUser = (String) variable;
		}

		variable = script.getVariable(VAR_JDBC_PASS);

		if (variable instanceof String) {
			this.jdbcPass = (String) variable;
		}

		variable = script.getVariable(VAR_LOAD);

		if (variable instanceof Boolean) {
			this.isLoad = ((Boolean) variable).booleanValue();
		}

		variable = script.getVariable(VAR_WARMUP_TIME);

		if (variable instanceof Number) {
			this.warmupTime = ((Number) variable).intValue();
		}

		variable = script.getVariable(VAR_MEASUREMENT_TIME);

		if (variable instanceof Number) {
			this.measurementTime = ((Number) variable).intValue();
		}

		variable = script.getVariable(VAR_N_TX_TYPES);

		if (variable instanceof Number) {
			this.nTxTypes = ((Number) variable).intValue();

			// sleepTimeとthrottleの配列サイズをnTxTypesに合わせる
			this.sleepTimes = new long[nTxTypes];
			this.throttles = new int[nTxTypes];
		}

		variable = script.getVariable(VAR_N_AGENTS);

		if (variable instanceof Number) {
			this.nAgents = ((Number) variable).intValue();

			// コネクションプールの接続数をエージェント数に合わせる
			this.connPoolSize = nAgents;
		}

		variable = script.getVariable(VAR_CONN_POOL_SIZE);

		if (variable instanceof Number) {
			this.connPoolSize = ((Number) variable).intValue();
		}

		variable = script.getVariable(VAR_STMT_CACHE_SIZE);

		if (variable instanceof Number) {
			this.stmtCacheSize = ((Number) variable).intValue();
		}

		variable = script.getVariable(VAR_AUTO_COMMIT);

		if (variable instanceof Boolean) {
			this.isAutoCommit = ((Boolean) variable).booleanValue();
		}

		variable = script.getVariable(VAR_SLEEP_TIME);

		if (variable instanceof Number) {
			// 書式1：var sleepTime = 1000;
			long value = ((Number) variable).longValue();

			for (int i = 0; i < nTxTypes; i++) {
				this.sleepTimes[i] = value;
			}
		} else if (variable instanceof NativeArray) {
			// 書式2：var sleepTime = new Array(1000,
			// 2000);
			Object[] array = (Object[]) Context.jsToJava(variable, Object[].class);

			if (array.length != nTxTypes) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_OF_SLEEPTIMES")); //$NON-NLS-1$
			}

			for (int i = 0; i < nTxTypes; i++) {
				if (array[i] instanceof Number) {
					this.sleepTimes[i] = ((Number) array[i]).longValue();
				} else {
					throw new ApplicationException(
							Resources.getString("Config.ILLEGAL_NUMBERS_SLEEP_TIME")); //$NON-NLS-1$
				}
			}
		}

		variable = script.getVariable(VAR_THROTTLE);

		if (variable instanceof Number) {
			// 書式1：var throttle = 100;
			int value = ((Number) variable).intValue();

			for (int i = 0; i < nTxTypes; i++) {
				this.throttles[i] = value;
			}

			this.doThrottleByTotal = true;
		} else if (variable instanceof NativeArray) {
			// 書式2：var throttle = new Array(100,
			// 200);
			Object[] array = (Object[]) Context.jsToJava(variable, Object[].class);

			if (array.length != nTxTypes) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_OF_THROTTLES")); //$NON-NLS-1$
			}

			for (int i = 0; i < nTxTypes; i++) {
				if (array[i] instanceof Number) {
					this.throttles[i] = ((Number) array[i]).intValue();
				} else {
					throw new ApplicationException(
							Resources.getString("Config.ILLEGAL_NUMBERS_THROTTLE")); //$NON-NLS-1$
				}
			}

			this.doThrottleByTotal = false;
		}

		variable = script.getVariable(VAR_DEBUG);

		if (variable instanceof Boolean) {
			this.isDebug = ((Boolean) variable).booleanValue();
		}

		variable = script.getVariable(VAR_TRACE);

		if (variable instanceof Boolean) {
			this.isTrace = ((Boolean) variable).booleanValue();

			if (isTrace) {
				this.isDebug = true;
			}
		}

		variable = script.getVariable(VAR_LOG_DIR);

		if (variable instanceof String) {
			this.logDir = (String) variable;
		}
	}

	private void loadOptions(CommandLine cl) throws ApplicationException {
		if (cl.hasOption(OPT_JDBC_DRIVER)) {
			this.jdbcDriver = cl.getOptionValue(OPT_JDBC_DRIVER);
		}

		if (cl.hasOption(OPT_JDBC_URL)) {
			this.jdbcUrl = cl.getOptionValue(OPT_JDBC_URL);
		}

		if (cl.hasOption(OPT_JDBC_USER)) {
			this.jdbcUser = cl.getOptionValue(OPT_JDBC_USER);
		}

		if (cl.hasOption(OPT_JDBC_PASS)) {
			this.jdbcPass = cl.getOptionValue(OPT_JDBC_PASS);
		}

		if (cl.hasOption(OPT_WARMUP_TIME)) {
			try {
				this.warmupTime = Integer.parseInt(cl.getOptionValue(OPT_WARMUP_TIME));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_WARMUP_TIME"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_MEASUREMENT_TIME)) {
			try {
				this.measurementTime = Integer.parseInt(cl.getOptionValue(OPT_MEASUREMENT_TIME));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_MEASUREMENT_TIME"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_N_AGENTS)) {
			try {
				this.nAgents = Integer.parseInt(cl.getOptionValue(OPT_N_AGENTS));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_N_AGENTS"), e); //$NON-NLS-1$
			}

			// コネクションプールの接続数をエージェント数に合わせる
			this.connPoolSize = nAgents;
		}

		if (cl.hasOption(OPT_CONN_POOL_SIZE)) {
			try {
				this.connPoolSize = Integer.parseInt(cl.getOptionValue(OPT_CONN_POOL_SIZE));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_CONN_POOL_SIZE"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_STMT_CACHE_SIZE)) {
			try {
				this.stmtCacheSize = Integer.parseInt(cl.getOptionValue(OPT_STMT_CACHE_SIZE));
			} catch (NumberFormatException e) {
				throw new ApplicationException(
						Resources.getString("Config.ILLEGAL_NUMBER_STMT_CACHE_SIZE"), e); //$NON-NLS-1$
			}
		}

		if (cl.hasOption(OPT_AUTO_COMMIT)) {
			this.isAutoCommit = Boolean.valueOf(cl.getOptionValue(OPT_AUTO_COMMIT));
		}

		if (cl.hasOption(OPT_SLEEP_TIME)) {
			try {
				// 書式1：-sleepTime 1000
				long value = Long.parseLong(cl.getOptionValue(OPT_SLEEP_TIME));

				for (int i = 0; i < nTxTypes; i++) {
					this.sleepTimes[i] = value;
				}
			} catch (NumberFormatException e) {
				// 書式2：-sleepTime 1000,2000
				String[] array = cl.getOptionValue(OPT_SLEEP_TIME).split(","); //$NON-NLS-1$

				if (array.length != nTxTypes) {
					throw new ApplicationException(
							Resources.getString("Config.ILLEGAL_NUMBER_OF_SLEEPTIMES")); //$NON-NLS-1$
				}

				for (int i = 0; i < nTxTypes; i++) {
					try {
						this.sleepTimes[i] = Long.parseLong(array[i]);
					} catch (NumberFormatException e2) {
						throw new ApplicationException(
								Resources.getString("Config.ILLEGAL_NUMBERS_SLEEP_TIME"), e2); //$NON-NLS-1$
					}
				}
			}
		}

		if (cl.hasOption(OPT_THROTTLE)) {
			try {
				// 書式1：-throttle 100
				int value = Integer.parseInt(cl.getOptionValue(OPT_THROTTLE));

				for (int i = 0; i < nTxTypes; i++) {
					throttles[i] = value;
				}

				this.doThrottleByTotal = true;
			} catch (NumberFormatException e) {
				// 書式2：-throttle 100,200
				String[] array = cl.getOptionValue(OPT_THROTTLE).split(","); //$NON-NLS-1$

				if (array.length != nTxTypes) {
					throw new ApplicationException(
							Resources.getString("Config.ILLEGAL_NUMBER_OF_THROTTLES")); //$NON-NLS-1$
				}

				for (int i = 0; i < nTxTypes; i++) {
					try {
						throttles[i] = Integer.parseInt(array[i]);
					} catch (NumberFormatException e2) {
						throw new ApplicationException(
								Resources.getString("Config.ILLEGAL_NUMBERS_THROTTLE"), e2); //$NON-NLS-1$
					}
				}

				this.doThrottleByTotal = false;
			}
		}

		if (cl.hasOption(OPT_DEBUG)) {
			this.isDebug = true;
		}

		if (cl.hasOption(OPT_TRACE)) {
			this.isTrace = true;
			this.isDebug = true;
		}

		if (cl.hasOption(OPT_LOG_DIR)) {
			this.logDir = cl.getOptionValue(OPT_LOG_DIR);
		}
	}
}

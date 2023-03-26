package jdbcrunner;

import java.sql.SQLException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

/**
 * スクリプトを構築して操作するクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Script {
	private static final int RHINO_OPTIMIZATION_LEVEL = 9;
	private static final String SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

	private final Context context;
	private final Scriptable scope;
	private final Function initFunction;
	private final Function runFunction;
	private final Function finFunction;

	/**
	 * 負荷テストの設定、データベースを操作する機能を提供するヘルパー、
	 * およびパラメータの上書き設定を指定してスクリプトを構築します。
	 * <p>
	 * <ol>
	 * <li>スクリプトのコンテキストとスコープを構築します。
	 * <li>スクリプトの以下の変数に値を設定します。
	 * <ul>
	 * <li>helper
	 * <li>param0
	 * <li>param1
	 * <li>param2
	 * <li>param3
	 * <li>param4
	 * <li>param5
	 * <li>param6
	 * <li>param7
	 * <li>param8
	 * <li>param9
	 * </ul>
	 * <li>ヘルパースクリプトとスクリプトを評価します。
	 * <li>{@code init()}、{@code run()}および
	 * {@code fin()}ファンクションをインスタンス変数に登録します。
	 * <li>パラメータの上書き設定が有効な場合、スクリプトの以下の変数に値を設定します。
	 * <ul>
	 * <li>jdbcDriver
	 * <li>jdbcUrl
	 * <li>jdbcUser
	 * <li>jdbcPass
	 * <li>isLoad
	 * <li>warmupTime
	 * <li>measurementTime
	 * <li>nTxTypes
	 * <li>nAgents
	 * <li>connPoolSize
	 * <li>stmtCacheSize
	 * <li>isAutoCommit
	 * <li>sleepTime
	 * <li>throttle
	 * <li>isDebug
	 * <li>isTrace
	 * <li>logDir
	 * </ul>
	 * </ol>
	 *
	 * @param config
	 *            負荷テストの設定
	 * @param helper
	 *            データベースを操作する機能を提供するヘルパー
	 * @param doOverride
	 *            パラメータの上書き設定
	 * @throws ApplicationException
	 *             スクリプトの文法に誤りがある場合
	 */
	public Script(Config config, Helper helper, boolean doOverride) throws ApplicationException {
		try {
			this.context = ContextFactory.getGlobal().enterContext();
			context.setOptimizationLevel(RHINO_OPTIMIZATION_LEVEL);
			this.scope = new ImporterTopLevel(context);

			scope.put("helper", scope, helper); //$NON-NLS-1$
			scope.put(Config.VAR_PARAM_0, scope, config.getParam0());
			scope.put(Config.VAR_PARAM_1, scope, config.getParam1());
			scope.put(Config.VAR_PARAM_2, scope, config.getParam2());
			scope.put(Config.VAR_PARAM_3, scope, config.getParam3());
			scope.put(Config.VAR_PARAM_4, scope, config.getParam4());
			scope.put(Config.VAR_PARAM_5, scope, config.getParam5());
			scope.put(Config.VAR_PARAM_6, scope, config.getParam6());
			scope.put(Config.VAR_PARAM_7, scope, config.getParam7());
			scope.put(Config.VAR_PARAM_8, scope, config.getParam8());
			scope.put(Config.VAR_PARAM_9, scope, config.getParam9());

			context.evaluateString(scope, config.getHelperScript(), Config.HELPER_FILENAME, 1, null);

			context.evaluateString(scope, config.getScenarioScript(), config.getScriptFilename(),
					1, null);

			Object variable = null;

			variable = scope.get("init", scope); //$NON-NLS-1$

			if (variable instanceof Function) {
				this.initFunction = (Function) variable;
			} else {
				this.initFunction = null;
			}

			variable = scope.get("run", scope); //$NON-NLS-1$

			if (variable instanceof Function) {
				this.runFunction = (Function) variable;
			} else {
				this.runFunction = null;
			}

			variable = scope.get("fin", scope); //$NON-NLS-1$

			if (variable instanceof Function) {
				this.finFunction = (Function) variable;
			} else {
				this.finFunction = null;
			}

			if (doOverride) {
				scope.put(Config.VAR_JDBC_DRIVER, scope, config.getJdbcDriver());
				scope.put(Config.VAR_JDBC_URL, scope, config.getJdbcUrl());
				scope.put(Config.VAR_JDBC_USER, scope, config.getJdbcUser());
				scope.put(Config.VAR_JDBC_PASS, scope, config.getJdbcPass());
				scope.put(Config.VAR_LOAD, scope, config.isLoad());
				scope.put(Config.VAR_WARMUP_TIME, scope, config.getWarmupTime());
				scope.put(Config.VAR_MEASUREMENT_TIME, scope, config.getMeasurementTime());
				scope.put(Config.VAR_N_TX_TYPES, scope, config.getNTxTypes());
				scope.put(Config.VAR_N_AGENTS, scope, config.getNAgents());
				scope.put(Config.VAR_CONN_POOL_SIZE, scope, config.getConnPoolSize());
				scope.put(Config.VAR_STMT_CACHE_SIZE, scope, config.getStmtCacheSize());
				scope.put(Config.VAR_AUTO_COMMIT, scope, config.isAutoCommit());

				if (config.getNTxTypes() == 1) {
					scope.put(Config.VAR_SLEEP_TIME, scope, config.getSleepTime(0));
					scope.put(Config.VAR_THROTTLE, scope, config.getThrottle(0));
				} else {
					NativeArray sleepTimeArray = (NativeArray) context.newArray(scope,
							config.getNTxTypes());

					NativeArray throttleArray = (NativeArray) context.newArray(scope,
							config.getNTxTypes());

					for (int txType = 0; txType < config.getNTxTypes(); txType++) {
						sleepTimeArray.put(txType, sleepTimeArray, config.getSleepTime(txType));
						throttleArray.put(txType, throttleArray, config.getThrottle(txType));
					}

					scope.put(Config.VAR_SLEEP_TIME, scope, sleepTimeArray);
					scope.put(Config.VAR_THROTTLE, scope, throttleArray);
				}

				scope.put(Config.VAR_DEBUG, scope, config.isDebug());
				scope.put(Config.VAR_TRACE, scope, config.isTrace());
				scope.put(Config.VAR_LOG_DIR, scope, config.getLogDir());
			}
		} catch (WrappedException e) {
			Throwable we = e.getWrappedException();

			throw new ApplicationException(Resources.getString("Script.WRAPPED_EXCEPTION") //$NON-NLS-1$
					+ SEPARATOR + we.toString() + SEPARATOR + e.getScriptStackTrace(), e);
		} catch (EvaluatorException e) {
			throw new ApplicationException(Resources.getString("Script.EVALUATOR_EXCEPTION") //$NON-NLS-1$
					+ SEPARATOR + e.getMessage() + SEPARATOR + e.lineSource(), e);
		} catch (RhinoException e) {
			throw new ApplicationException(Resources.getString("Script.RHINO_EXCEPTION") //$NON-NLS-1$
					+ SEPARATOR + e.getMessage() + SEPARATOR + e.getScriptStackTrace(), e);
		}
	}

	/**
	 * スクリプトから変数の値を取得して返します。
	 *
	 * @param name
	 *            変数の名前
	 * @return 変数の値
	 */
	public Object getVariable(String name) {
		return scope.get(name, scope);
	}

	/**
	 * スクリプトの{@code init()}ファンクションを実行します。
	 *
	 * @throws ApplicationException
	 *             ユーザ定義エラーが発生した場合、
	 *             データベースアクセス中に例外が発生した場合、
	 *             スクリプトの実行中に例外が発生した場合、
	 *             スクリプトの実行中にエラーが発生した場合
	 */
	public void callInit() throws ApplicationException {
		call(initFunction);
	}

	/**
	 * スクリプトの{@code run()}ファンクションを実行します。
	 *
	 * @throws ApplicationException
	 *             ユーザ定義エラーが発生した場合、
	 *             データベースアクセス中に例外が発生した場合、
	 *             スクリプトの実行中に例外が発生した場合、
	 *             スクリプトの実行中にエラーが発生した場合
	 */
	public void callRun() throws ApplicationException {
		call(runFunction);
	}

	/**
	 * スクリプトの{@code fin()}ファンクションを実行します。
	 *
	 * @throws ApplicationException
	 *             ユーザ定義エラーが発生した場合、
	 *             データベースアクセス中に例外が発生した場合、
	 *             スクリプトの実行中に例外が発生した場合、
	 *             スクリプトの実行中にエラーが発生した場合
	 */
	public void callFin() throws ApplicationException {
		call(finFunction);
	}

	/**
	 * スクリプトで用いる配列を作成して返します。
	 *
	 * @param length
	 *            配列の長さ
	 * @return 配列
	 */
	public NativeArray createArray(int length) {
		return (NativeArray) context.newArray(scope, length);
	}

	/**
	 * スクリプトのスタックトレースを返します。
	 *
	 * @param exception
	 *            例外オブジェクト
	 * @return スタックトレース
	 */
	public String getScriptStackTrace(RhinoException exception) {
		return SEPARATOR + exception.getScriptStackTrace();
	}

	/**
	 * スクリプトのスコープを返します。
	 *
	 * @return スクリプトのスコープ
	 */
	public Scriptable getScope() {
		return scope;
	}

	/**
	 * スクリプトのコンテキストを閉じます。
	 */
	public void close() {
		try {
			Context.exit();
		} catch (IllegalStateException e) {
			// 何もしない
		}
	}

	private void call(Function function) throws ApplicationException {
		if (function != null) {
			try {
				function.call(context, scope, scope, null);
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();

				if (we instanceof ApplicationException) {
					throw new ApplicationException(
							Resources.getString("Script.APPLICATION_EXCEPTION") //$NON-NLS-1$
									+ SEPARATOR + we.getMessage()
									+ SEPARATOR
									+ e.getScriptStackTrace(),
							we);
				} else if (we instanceof SQLException) {
					throw new ApplicationException(Resources.getString("Script.SQL_EXCEPTION") //$NON-NLS-1$
							+ SEPARATOR + we.toString() + SEPARATOR + e.getScriptStackTrace(), we);
				} else {
					throw new ApplicationException(Resources.getString("Script.WRAPPED_EXCEPTION") //$NON-NLS-1$
							+ SEPARATOR + we.toString() + SEPARATOR + e.getScriptStackTrace(), we);
				}
			} catch (RhinoException e) {
				throw new ApplicationException(Resources.getString("Script.RHINO_EXCEPTION") //$NON-NLS-1$
						+ SEPARATOR + e.getMessage() + SEPARATOR + e.getScriptStackTrace(), e);
			}
		}
	}
}

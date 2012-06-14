package jdbcrunner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.RhinoException;

/**
 * エージェントからスクリプトを操作、
 * およびスクリプトからデータベースを操作する機能を提供するヘルパークラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Helper {
	private static final Map<String, Object> sharedData = new ConcurrentHashMap<String, Object>();

	private final Config config;
	private final Agent agent;
	private final Script script;
	private final TemplateCache templateCache = new TemplateCache();

	private Connection connection;

	/**
	 * 負荷テストの設定、ヘルパーを管理するエージェントを指定してヘルパーを構築します。
	 *
	 * @param config
	 *            負荷テストの設定
	 * @param agent
	 *            ヘルパーを管理するエージェント
	 * @throws ApplicationException
	 *             スクリプトの文法に誤りがある場合
	 */
	public Helper(Config config, Agent agent) throws ApplicationException {
		this.config = config;
		this.agent = agent;
		this.script = new Script(config, this, true);
	}

	/**
	 * ヘルパー間で共有しているマップからデータを取得して返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @param key
	 *            関連付けされたデータが返されるキー
	 * @return 指定されたキーに関連付けされているデータ
	 */
	public static Object getData(String key) {
		return sharedData.get(key);
	}

	/**
	 * ヘルパー間で共有しているマップへデータを登録します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @param key
	 *            指定されたデータが関連付けされるキー
	 * @param value
	 *            指定されたキーに関連付けされるデータ
	 */
	public static void putData(String key, Object value) {
		sharedData.put(key, value);
	}

	/**
	 * {@code Helper}オブジェクトにデータベースへの接続を設定します。
	 *
	 * @param connection
	 *            データベースへの接続
	 */
	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	/**
	 * スクリプトの{@code init()}ファンクションを実行します。
	 *
	 * @throws ApplicationException
	 *             ユーザ定義エラーが発生した場合、
	 *             データベースアクセス中に例外が発生した場合、
	 *             スクリプトの実行中に例外が発生した場合、
	 *             スクリプトの実行中にエラーが発生した場合
	 * @see Script#callInit()
	 */
	public void callInit() throws ApplicationException {
		script.callInit();
	}

	/**
	 * スクリプトの{@code run()}ファンクションを実行します。
	 *
	 * @throws ApplicationException
	 *             ユーザ定義エラーが発生した場合、
	 *             データベースアクセス中に例外が発生した場合、
	 *             スクリプトの実行中に例外が発生した場合、
	 *             スクリプトの実行中にエラーが発生した場合
	 * @see Script#callRun()
	 */
	public void callRun() throws ApplicationException {
		script.callRun();
	}

	/**
	 * スクリプトの{@code fin()}ファンクションを実行します。
	 *
	 * @throws ApplicationException
	 *             ユーザ定義エラーが発生した場合、
	 *             データベースアクセス中に例外が発生した場合、
	 *             スクリプトの実行中に例外が発生した場合、
	 *             スクリプトの実行中にエラーが発生した場合
	 * @see Script#callFin()
	 */
	public void callFin() throws ApplicationException {
		script.callFin();
	}

	/**
	 * スクリプトのコンテキストを閉じます。
	 *
	 * @see Script#close()
	 */
	public void closeScript() {
		script.close();
	}

	/**
	 * エージェントのIDを返します。
	 *
	 * @return エージェントのID
	 * @see Agent#getId()
	 */
	public int getId() {
		return agent.getId();
	}

	/**
	 * {@code run()}ファンクションの停止フラグを立てます。
	 *
	 * @see Agent#setBreak()
	 */
	public void setBreak() {
		agent.setBreak();
	}

	/**
	 * トランザクション種別を設定します。
	 *
	 * @param txType
	 *            トランザクション種別の番号。これは0以上
	 *            {@code Config#getNTxTypes()}
	 *            未満である必要があります
	 * @see Agent#setTxType(int)
	 */
	public void setTxType(int txType) {
		agent.setTxType(txType);
	}

	/**
	 * データベースへの接続を返します。
	 * <p>
	 * このメソッドは新たにデータソースから接続を取得するのではなく、
	 * すでに取得済みの接続を呼び出し元に返すものです。
	 *
	 * @return データベースへの接続
	 */
	public Connection takeConnection() {
		return connection;
	}

	/**
	 * データベースの製品名を返します。
	 *
	 * @return データベースの製品名
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 */
	public String getDatabaseProductName() throws SQLException {
		return connection.getMetaData().getDatabaseProductName();
	}

	/**
	 * データベースへの変更を確定します。
	 *
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 * @see java.sql.Connection#commit()
	 */
	public void commit() throws SQLException {
		connection.commit();
	}

	/**
	 * データベースへの変更を取り消します。
	 *
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 * @see java.sql.Connection#rollback()
	 */
	public void rollback() throws SQLException {
		connection.rollback();
	}

	/**
	 * クエリを実行し、検索されたレコード数を返します。
	 * <p>
	 * <ol>
	 * <li>独自記法のSQL文をキーとして、
	 * テンプレートのキャッシュからテンプレートを取得します。
	 * テンプレートが存在しない場合は、新たに作成してキャッシュに登録します。
	 * <li>テンプレートからSQL文を取得し、
	 * {@code PreparedStatement}オブジェクトを構築します。
	 * <li>{@code PreparedStatement}
	 * オブジェクトにパラメータをバインドします。
	 * <li>クエリを実行し、検索されたレコード数を返します。
	 * </ol>
	 *
	 * @param statement
	 *            独自記法のSQL文
	 * @param parameters
	 *            SQLにバインドするパラメータの配列
	 * @return クエリによって検索されたレコード数
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 */
	public int query(String statement, Object[] parameters) throws SQLException {
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int rowCount = 0;

		try {
			Template template = getTemplate(statement);
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			template.setParameters(preparedStatement, parameters);
			resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				rowCount++;
			}
		} finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}

			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}
		}

		return rowCount;
	}

	/**
	 * クエリを実行し、 検索されたレコードをスクリプトにおける二次元配列として返します。
	 * <p>
	 * <ol>
	 * <li>独自記法のSQL文をキーとして、
	 * テンプレートのキャッシュからテンプレートを取得します。
	 * テンプレートが存在しない場合は、新たに作成してキャッシュに登録します。
	 * <li>テンプレートからSQL文を取得し、
	 * {@code PreparedStatement}オブジェクトを構築します。
	 * <li>{@code PreparedStatement}
	 * オブジェクトにパラメータをバインドします。
	 * <li>クエリを実行し、
	 * 検索されたレコードをスクリプトにおける二次元配列として返します。
	 * </ol>
	 *
	 * @param statement
	 *            独自記法のSQL文
	 * @param parameters
	 *            SQLにバインドするパラメータの配列
	 * @return クエリによって検索されたレコード
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 */
	public NativeArray fetchAsArray(String statement, Object[] parameters) throws SQLException {
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		NativeArray rowArray = script.createArray(0);

		try {
			Template template = getTemplate(statement);
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			template.setParameters(preparedStatement, parameters);
			resultSet = preparedStatement.executeQuery();
			int rowCount = 0;
			int columnCount = resultSet.getMetaData().getColumnCount();

			while (resultSet.next()) {
				NativeArray columnArray = script.createArray(columnCount);

				for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
					columnArray.put(columnIndex - 1, columnArray, resultSet.getObject(columnIndex));
				}
				rowArray.put(rowCount++, rowArray, columnArray);
			}
		} finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}

			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}
		}

		return rowArray;
	}

	/**
	 * DMLを実行し、更新されたレコード数を返します。
	 * <p>
	 * <ol>
	 * <li>独自記法のSQL文をキーとして、
	 * テンプレートのキャッシュからテンプレートを取得します。
	 * テンプレートが存在しない場合は、新たに作成してキャッシュに登録します。
	 * <li>テンプレートからSQL文を取得し、
	 * {@code PreparedStatement}オブジェクトを構築します。
	 * <li>{@code PreparedStatement}
	 * オブジェクトにパラメータをバインドします。
	 * <li>DMLを実行し、更新されたレコード数を返します。
	 * </ol>
	 *
	 * @param statement
	 *            独自記法のSQL文
	 * @param parameters
	 *            SQLにバインドするパラメータの配列
	 * @return クエリによって更新されたレコード数
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 */
	public int execute(String statement, Object[] parameters) throws SQLException {
		PreparedStatement preparedStatement = null;
		int count = 0;

		try {
			Template template = getTemplate(statement);
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			template.setParameters(preparedStatement, parameters);
			count = preparedStatement.executeUpdate();
		} finally {
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}
		}

		return count;
	}

	/**
	 * JDBCバッチ更新を行い、
	 * 更新されたレコード数をスクリプトにおける配列として返します。
	 * <p>
	 * <ol>
	 * <li>独自記法のSQL文をキーとして、
	 * テンプレートのキャッシュからテンプレートを取得します。
	 * テンプレートが存在しない場合は、新たに作成してキャッシュに登録します。
	 * <li>テンプレートからSQL文を取得し、
	 * {@code PreparedStatement}オブジェクトを構築します。
	 * <li>{@code PreparedStatement}
	 * オブジェクトにパラメータをバインドし、バッチに追加します。
	 * <li>JDBCバッチ更新を行い、
	 * 更新されたレコード数をスクリプトにおける配列として返します。
	 * </ol>
	 *
	 * @param statement
	 *            独自記法のSQL文
	 * @param arrayParameters
	 *            SQLにバインドするパラメータ配列の配列
	 * @return クエリによって更新されたレコード数
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 * @throws IllegalArgumentException
	 *             パラメータ配列の配列長が一致しない場合、
	 *             パラメータ配列として配列でないものが指定されている場合
	 */
	public NativeArray executeBatch(String statement, Object[] arrayParameters) throws SQLException {
		PreparedStatement preparedStatement = null;
		int batchSize = 0;

		for (Object arrayParameter : arrayParameters) {
			if (arrayParameter instanceof NativeArray) {
				int arrayLength = (int) ((NativeArray) arrayParameter).getLength();

				if (batchSize == 0) {
					batchSize = arrayLength;
				} else if (batchSize != arrayLength) {
					throw new IllegalArgumentException(
							Resources.getString("Helper.ILLEGAL_ARRAY_LENGTH") //$NON-NLS-1$
									+ arrayLength);
				}
			} else {
				throw new IllegalArgumentException(Resources.getString("Helper.ILLEGAL_NOT_ARRAY") //$NON-NLS-1$
						+ arrayParameter.getClass().getName());
			}
		}

		NativeArray countArray = script.createArray(batchSize);

		try {
			Template template = getTemplate(statement);
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());

			for (int rowIndex = 0; rowIndex < batchSize; rowIndex++) {
				Object[] parameters = new Object[arrayParameters.length];
				preparedStatement.clearParameters();

				for (int columnIndex = 0; columnIndex < arrayParameters.length; columnIndex++) {
					parameters[columnIndex] = ((NativeArray) arrayParameters[columnIndex]).get(
							rowIndex, script.getScope());
				}

				template.setParameters(preparedStatement, parameters);
				preparedStatement.addBatch();
			}

			int[] counts = preparedStatement.executeBatch();

			for (int rowIndex = 0; rowIndex < counts.length; rowIndex++) {
				countArray.put(rowIndex, countArray, counts[rowIndex]);
			}
		} finally {
			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}
		}

		return countArray;
	}

	/**
	 * トレースログを出力します。
	 * <p>
	 * トレースログは、トレースモードが有効な場合のみ出力されます。
	 *
	 * @param message
	 *            ログメッセージ
	 */
	public void trace(String message) {
		if (config.isTrace()) {
			agent.putMessage(new Message(Message.Level.TRACE, message));
		}
	}

	/**
	 * デバッグログを出力します。
	 * <p>
	 * デバッグログは、デバッグモードが有効な場合のみ出力されます。
	 *
	 * @param message
	 *            ログメッセージ
	 */
	public void debug(String message) {
		if (config.isDebug()) {
			agent.putMessage(new Message(Message.Level.DEBUG, message));
		}
	}

	/**
	 * 情報ログを出力します。
	 *
	 * @param message
	 *            ログメッセージ
	 */
	public void info(String message) {
		agent.putMessage(new Message(Message.Level.INFO, message));
	}

	/**
	 * 警告ログを出力します。
	 *
	 * @param message
	 *            ログメッセージ
	 */
	public void warn(String message) {
		agent.putMessage(new Message(Message.Level.WARN, message));
	}

	/**
	 * ログメッセージを指定して例外を発生させます。
	 * <p>
	 * このメソッドは例外を意図的に発生させます。発生した例外は
	 * {@code Agent#run()}内でキャッチされ、マネージャに通知されます。
	 *
	 * @param message
	 *            ログメッセージ
	 * @throws ApplicationException
	 *             このメソッドを実行した際に必ず発生する例外
	 */
	public void error(String message) throws ApplicationException {
		throw new ApplicationException(message);
	}

	/**
	 * スクリプトのスタックトレースを返します。
	 *
	 * @param object
	 *            例外オブジェクト
	 * @return スタックトレース、引数が例外オブジェクトでない場合は空文字列
	 * @see Script#getScriptStackTrace(RhinoException)
	 */
	public String getScriptStackTrace(Object object) {
		if (object instanceof RhinoException) {
			return script.getScriptStackTrace((RhinoException) object);
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private Template getTemplate(String statement) {
		Template template = templateCache.get(statement);

		if (template == null) {
			template = new Template(statement);
			templateCache.put(statement, template);
		}

		return template;
	}
}

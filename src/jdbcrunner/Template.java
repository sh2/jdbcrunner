package jdbcrunner;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;

/**
 * 独自記法のSQL文から、SQL文とパラメータのデータ型を抽出して保持するクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Template {
	/**
	 * パラメータのデータ型を表す列挙型クラスです。
	 *
	 * @author Sadao Hiratsuka
	 */
	public static enum DataType {
		/**
		 * {@code int}を表す列挙子です。
		 */
		INT,

		/**
		 * {@code long}を表す列挙子です。
		 */
		LONG,

		/**
		 * {@code double}を表す列挙子です。
		 */
		DOUBLE,

		/**
		 * {@code String}を表す列挙子です。
		 */
		STRING,

		/**
		 * {@code Timestamp}を表す列挙子です。
		 */
		TIMESTAMP
	}

	private static final Pattern pattern = Pattern
			.compile("\\$(\\$|int|long|double|string|timestamp)"); //$NON-NLS-1$

	private final String preparableStatement;
	private final List<DataType> dataTypeList = new ArrayList<DataType>();

	/**
	 * 独自記法のSQL文を指定して{@code Template}を構築します。
	 * <p>
	 * <ol>
	 * <li>独自記法のSQL文に「$」で始まる文字列があるかどうかを調べます。
	 * <li>文字列「$$」があった場合、「$」に置換します。
	 * <li>文字列「$int」「$long」「$double」「$string」
	 * 「$timestamp」があった場合、パラメータのデータ型リストに{@code int}、{@code long}、{@code double}、{@code String}、{@code Timestamp} を登録し、文字列を「?」に置換します。
	 * <li>それ以外の「$」で始まる文字列がある場合は
	 * {@code IllegalArgumentException}をスローします。
	 * <li>置換された文字列をSQL文としてインスタンス変数に登録します。
	 * </ol>
	 *
	 * @param statement
	 *            独自記法のSQL文
	 * @throws IllegalArgumentException
	 *             独自記法のSQL文に、「$」で始まるが「$$」
	 *             あるいはパラメータとして認識されない文字列が含まれている場合
	 */
	public Template(String statement) {
		Matcher matcher = pattern.matcher(statement);
		StringBuffer replacedStatement = new StringBuffer();
		int checkBegin = 0;
		int checkEnd = 0;

		while (matcher.find()) {
			String parameter = matcher.group();
			checkEnd = matcher.start();
			checkSequence(statement, checkBegin, checkEnd);
			checkBegin = matcher.end();

			if (parameter.charAt(1) == '$') {
				matcher.appendReplacement(replacedStatement, "\\$"); //$NON-NLS-1$
			} else {
				dataTypeList.add(DataType.valueOf(parameter.substring(1)
						.toUpperCase(Locale.ENGLISH)));

				matcher.appendReplacement(replacedStatement, "?"); //$NON-NLS-1$
			}
		}

		checkSequence(statement, checkBegin, statement.length());
		matcher.appendTail(replacedStatement);
		this.preparableStatement = replacedStatement.toString();
	}

	/**
	 * SQL文を返します。
	 *
	 * @return SQL文
	 */
	public String getPreparableStatement() {
		return preparableStatement;
	}

	/**
	 * パラメータのデータ型リストを返します。
	 *
	 * @return パラメータのデータ型リスト
	 */
	public List<DataType> getDataTypeList() {
		return dataTypeList;
	}

	/**
	 * {@code PreparedStatement}
	 * オブジェクトにパラメータを設定します。
	 * <p>
	 * <ol>
	 * <li>
	 * パラメータ配列の配列長が、データ型のリスト長と等しいことを確認します。異なる場合は
	 * {@code IllegalArgumentException}をスローします。
	 * <li>
	 * それぞれのパラメータを、リストから得たデータ型へ変換し
	 * {@code PreparedStatement}オブジェクトに設定します。
	 * 対象のデータ型への変換が行えない場合は
	 * {@code IllegalArgumentException}をスローします。
	 * </ol>
	 *
	 * @param preparedStatement
	 *            パラメータを設定する
	 *            {@code PreparedStatement}オブジェクト
	 * @param parameters
	 *            パラメータの配列
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 * @throws IllegalArgumentException
	 *             パラメータの配列長がデータ型のリスト長と異なる場合、
	 *             パラメータを対象のデータ型へ変換できない場合
	 */
	public void setParameters(PreparedStatement preparedStatement, Object[] parameters)
			throws SQLException {

		if (dataTypeList.size() != parameters.length) {
			throw new IllegalArgumentException(
					Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") //$NON-NLS-1$
							+ parameters.length
							+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") //$NON-NLS-1$
							+ dataTypeList.size());
		}

		for (int parameterIndex = 1; parameterIndex <= dataTypeList.size(); parameterIndex++) {
			Object parameter = parameters[parameterIndex - 1];

			switch (dataTypeList.get(parameterIndex - 1)) {
			case INT:
				if (parameter instanceof Number) {
					preparedStatement.setInt(parameterIndex, ((Number) parameter).intValue());
				} else if (parameter instanceof String) {
					try {
						preparedStatement.setInt(parameterIndex,
								Integer.parseInt((String) parameter));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								Resources.getString("Template.ILLEGAL_STRING_FOR_INT_1") //$NON-NLS-1$
										+ parameterIndex
										+ Resources.getString("Template.ILLEGAL_STRING_FOR_INT_2") //$NON-NLS-1$
										+ (String) parameter, e);
					}
				} else if (parameter == null) {
					preparedStatement.setNull(parameterIndex, java.sql.Types.INTEGER);
				} else {
					throw new IllegalArgumentException(
							Resources.getString("Template.ILLEGAL_NUMBER_1") //$NON-NLS-1$
									+ parameterIndex
									+ Resources.getString("Template.ILLEGAL_NUMBER_2") //$NON-NLS-1$
									+ parameter.getClass().getName());
				}
				break;
			case LONG:
				if (parameter instanceof Number) {
					preparedStatement.setLong(parameterIndex, ((Number) parameter).longValue());
				} else if (parameter instanceof String) {
					try {
						preparedStatement.setLong(parameterIndex,
								Long.parseLong((String) parameter));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								Resources.getString("Template.ILLEGAL_STRING_FOR_LONG_1") //$NON-NLS-1$
										+ parameterIndex
										+ Resources.getString("Template.ILLEGAL_STRING_FOR_LONG_2") //$NON-NLS-1$
										+ (String) parameter, e);
					}
				} else if (parameter == null) {
					preparedStatement.setNull(parameterIndex, java.sql.Types.BIGINT);
				} else {
					throw new IllegalArgumentException(
							Resources.getString("Template.ILLEGAL_NUMBER_1") //$NON-NLS-1$
									+ parameterIndex
									+ Resources.getString("Template.ILLEGAL_NUMBER_2") //$NON-NLS-1$
									+ parameter.getClass().getName());
				}
				break;
			case DOUBLE:
				if (parameter instanceof Number) {
					preparedStatement.setDouble(parameterIndex, ((Number) parameter).doubleValue());
				} else if (parameter instanceof String) {
					try {
						preparedStatement.setDouble(parameterIndex,
								Double.parseDouble((String) parameter));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								Resources.getString("Template.ILLEGAL_STRING_FOR_DOUBLE_1") //$NON-NLS-1$
										+ parameterIndex
										+ Resources.getString("Template.ILLEGAL_STRING_FOR_DOUBLE_2") //$NON-NLS-1$
										+ (String) parameter, e);
					}
				} else if (parameter == null) {
					preparedStatement.setNull(parameterIndex, java.sql.Types.DOUBLE);
				} else {
					throw new IllegalArgumentException(
							Resources.getString("Template.ILLEGAL_NUMBER_1") //$NON-NLS-1$
									+ parameterIndex
									+ Resources.getString("Template.ILLEGAL_NUMBER_2") //$NON-NLS-1$
									+ parameter.getClass().getName());
				}
				break;
			case STRING:
				if (parameter instanceof String) {
					preparedStatement.setString(parameterIndex, (String) parameter);
				} else if (parameter instanceof ConsString) {
					// Rhino 1.7R4 uses ConsString for string concatenation
					preparedStatement.setString(parameterIndex, parameter.toString());
				} else if (parameter == null) {
					preparedStatement.setNull(parameterIndex, java.sql.Types.CHAR);
				} else {
					throw new IllegalArgumentException(
							Resources.getString("Template.ILLEGAL_STRING_1") //$NON-NLS-1$
									+ parameterIndex
									+ Resources.getString("Template.ILLEGAL_STRING_2") //$NON-NLS-1$
									+ parameter.getClass().getName());
				}
				break;
			case TIMESTAMP:
				if ((parameter != null)
						&& (parameter.getClass().getName()
								.equals("org.mozilla.javascript.NativeDate"))) { //$NON-NLS-1$

					Date date = (Date) Context.jsToJava(parameter, Date.class);
					Timestamp timestamp = new Timestamp(date.getTime());
					preparedStatement.setTimestamp(parameterIndex, timestamp);
				} else if (parameter instanceof Date) {
					Timestamp timestamp = new Timestamp(((Date) parameter).getTime());
					preparedStatement.setTimestamp(parameterIndex, timestamp);
				} else if (parameter instanceof Number) {
					Timestamp timestamp = new Timestamp(((Number) parameter).longValue());
					preparedStatement.setTimestamp(parameterIndex, timestamp);
				} else if (parameter instanceof String) {
					Timestamp timestamp = Timestamp.valueOf((String) parameter);
					preparedStatement.setTimestamp(parameterIndex, timestamp);
				} else if (parameter == null) {
					preparedStatement.setNull(parameterIndex, java.sql.Types.TIMESTAMP);
				} else {
					throw new IllegalArgumentException(
							Resources.getString("Template.ILLEGAL_TIMESTAMP_1") //$NON-NLS-1$
									+ parameterIndex
									+ Resources.getString("Template.ILLEGAL_TIMESTAMP_2") //$NON-NLS-1$
									+ parameter.getClass().getName());
				}
			}
		}
	}

	private void checkSequence(String statement, int beginIndex, int endIndex) {
		String sequence = statement.substring(beginIndex, endIndex);
		int index = sequence.indexOf("$"); //$NON-NLS-1$

		if (index > -1) {
			throw new IllegalArgumentException(
					Resources.getString("Template.ILLEGAL_PARAMETER_NAME") //$NON-NLS-1$
							+ sequence.substring(index));
		}
	}
}

package jdbcrunner;

/**
 * エージェントからマネージャ、マネージャからマネージャへ通知する情報を格納するクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Message {
	/**
	 * メッセージの重要度を表す列挙型クラスです。
	 *
	 * @author Sadao Hiratsuka
	 */
	public static enum Level {
		/**
		 * トレースレベル、{@code DEBUG}
		 * よりも詳細な情報であることを表す列挙子です。
		 */
		TRACE,

		/**
		 * デバッグレベル、詳細な情報であることを表す列挙子です。
		 */
		DEBUG,

		/**
		 * 情報レベル、通常の情報であることを表す列挙子です。
		 */
		INFO,

		/**
		 * 警告レベル、潜在的に異常な状態であることを表す列挙子です。
		 */
		WARN,

		/**
		 * エラーレベル、異常な状態であることを表す列挙子です。
		 */
		ERROR
	}

	private final Level level;
	private final String message;
	private final Throwable throwable;

	/**
	 * メッセージのレベル、メッセージの内容、通知する例外を指定してメッセージを構築します。
	 *
	 * @param level
	 *            メッセージのレベル
	 * @param message
	 *            メッセージの内容
	 * @param throwable
	 *            通知する例外
	 */
	public Message(Level level, String message, Throwable throwable) {
		this.level = level;
		this.message = message;
		this.throwable = throwable;
	}

	/**
	 * メッセージのレベル、メッセージの内容を指定してメッセージを構築します。
	 *
	 * @param level
	 *            メッセージのレベル
	 * @param message
	 *            メッセージの内容
	 */
	public Message(Level level, String message) {
		this(level, message, null);
	}

	/**
	 * メッセージのレベルを返します。
	 *
	 * @return メッセージのレベル
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * メッセージの内容を返します。
	 *
	 * @return メッセージの内容
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * 通知する例外を返します。
	 *
	 * @return 通知する例外
	 */
	public Throwable getThrowable() {
		return throwable;
	}
}

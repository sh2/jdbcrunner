package jdbcrunner;

/**
 * アプリケーションで異常が発生したときにスローされる例外です。
 *
 * @author Sadao Hiratsuka
 */
public class ApplicationException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * 例外を説明するメッセージを指定して
	 * {@code ApplicationException}を構築します。
	 *
	 * @param message
	 *            例外を説明するメッセージ
	 */
	public ApplicationException(String message) {
		super(message);
	}

	/**
	 * 基となる原因を指定して{@code ApplicationException}
	 * を構築します。
	 *
	 * @param cause
	 *            この{@code ApplicationException}
	 *            の基となる原因
	 */
	public ApplicationException(Throwable cause) {
		super(cause);
	}

	/**
	 * 例外を説明するメッセージおよび基となる原因を指定して
	 * {@code ApplicationException}を構築します。
	 *
	 * @param message
	 *            例外を説明するメッセージ
	 * @param cause
	 *            この{@code ApplicationException}
	 *            の基となる原因
	 */
	public ApplicationException(String message, Throwable cause) {
		super(message, cause);
	}
}

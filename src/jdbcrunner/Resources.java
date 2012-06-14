package jdbcrunner;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * メッセージの国際化を行うクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Resources {
	private static final String BUNDLE_NAME = "jdbcrunner.resources"; //$NON-NLS-1$
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Resources() {
		// 何もしない
	}

	/**
	 * ロケールに応じたメッセージを返します。
	 *
	 * @param key
	 *            メッセージのキー
	 * @return ロケールに応じたメッセージ。
	 *         メッセージが見つからない場合はキーの前後を「!」で囲んだ文字列
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}

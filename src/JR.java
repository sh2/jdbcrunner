import java.io.File;
import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.mozilla.javascript.RhinoException;

import jdbcrunner.ApplicationException;
import jdbcrunner.Config;
import jdbcrunner.Manager;
import jdbcrunner.Resources;

// TODO 中断機能を作る
// TODO tpcc-mysql互換ローダを作る
// TODO Tiny TPC-C DeliveryトランザクションのSELECT FOR UPDATE文が空振りしないようにする

/**
 * アプリケーションの起動用クラスです。
 *
 * @author Sadao Hiratsuka
 */
public class JR {
	private static final Logger log = Logger.getLogger(JR.class);

	private JR() {
		// 何もしない
	}

	/**
	 * アプリケーションの起動用メソッドです。
	 * <p>
	 * <ol>
	 * <li>コマンドラインオプションを基に負荷テストの設定を構築します。
	 * <li>ログ出力の設定を行います。
	 * <li>負荷テストを管理するマネージャを構築します。
	 * <li>負荷テストを行います。
	 * <li>負荷テストの結果を返り値として、プログラムを終了します。
	 * </ol>
	 * <p>
	 * 途中で例外が発生した場合は、例外の内容をログに出力してプログラムを異常終了します。
	 *
	 * @param args
	 *            コマンドラインオプション
	 */
	public static void main(String[] args) {
		int status = Manager.RETURN_FAILURE;
		Config config = null;

		try {
			config = new Config(args);
			setLogger(config);

			log.info("> " + Manager.APPLICATION_NAME //$NON-NLS-1$
					+ " " + Manager.VERSION); //$NON-NLS-1$
			status = new Manager(config).measure();

			if (status == Manager.RETURN_SUCCESS) {
				log.info("< " + Manager.APPLICATION_NAME //$NON-NLS-1$
						+ " SUCCESS"); //$NON-NLS-1$
			} else {
				log.info("< " + Manager.APPLICATION_NAME //$NON-NLS-1$
						+ " ERROR"); //$NON-NLS-1$
			}

		} catch (ApplicationException e) {
			// configを作れなかったとき
			status = Manager.RETURN_FAILURE;
			if (e.getCause() instanceof RhinoException) {
				printError(e, false);
			} else {
				printError(e, true);
			}

		} catch (IOException e) {
			// log4jのログファイルを開けなかったとき
			status = Manager.RETURN_FAILURE;
			printError(new ApplicationException(Resources.getString("JR.IO_EXCEPTION"), e), true); //$NON-NLS-1$

		} catch (Exception e) {
			status = Manager.RETURN_FAILURE;
			e.printStackTrace();
		}

		System.exit(status);
	}

	private static void setLogger(Config config) throws IOException {
		Layout consoleLayout = new PatternLayout("%d{HH:mm:ss} [%-5p] %m%n"); //$NON-NLS-1$
		Layout fileLayout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} [%-5p] %m%n"); //$NON-NLS-1$

		if (config.isTrace()) {
			Logger.getRootLogger().setLevel(Level.TRACE);
			consoleLayout = new PatternLayout("%d{HH:mm:ss} [%-5p] [%t] [%C#%M] %m%n"); //$NON-NLS-1$
			fileLayout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} [%t] [%C#%M] %m%n"); //$NON-NLS-1$
		} else if (config.isDebug()) {
			Logger.getRootLogger().setLevel(Level.DEBUG);
		} else {
			Logger.getRootLogger().setLevel(Level.INFO);
		}

		Appender consoleAppender = new ConsoleAppender(consoleLayout, "System.out"); //$NON-NLS-1$
		consoleAppender.setName("Console"); //$NON-NLS-1$
		Logger.getRootLogger().addAppender(consoleAppender);

		Appender fileAppender = new FileAppender(fileLayout, config.getLogDir() + File.separator
				+ Config.LOG4J_FILENAME);
		fileAppender.setName("File"); //$NON-NLS-1$
		Logger.getRootLogger().addAppender(fileAppender);
	}

	private static void printError(Throwable e, boolean isRecursive) {
		System.err.println(Manager.APPLICATION_NAME + " " + Manager.VERSION); //$NON-NLS-1$
		System.err.println(e.getMessage());

		if (isRecursive) {
			Throwable cause = e.getCause();
			while (cause != null) {
				System.err.println(cause.getMessage());
				cause = cause.getCause();
			}
		}

		System.err.println();
		System.err.print(Config.getHelpMessage());
	}
}

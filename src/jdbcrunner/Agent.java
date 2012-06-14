package jdbcrunner;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 負荷シナリオを実行するクラスです。
 * <p>
 * このクラスのインスタンスは、スレッドを1つ起動して負荷シナリオを実行します。
 *
 * @author Sadao Hiratsuka
 */
public class Agent implements Runnable {
	private final int id;
	private final Manager manager;
	private final Config config;
	private final Throttle throttle;
	private final Record record;

	private volatile Thread thread;
	private boolean doBreak;
	private int txType = 0;

	/**
	 * エージェントのID、負荷テストを管理するマネージャ、
	 * 負荷テストの設定を指定してエージェントを構築します。
	 *
	 * @param id
	 *            エージェントのID
	 * @param manager
	 *            負荷テストを管理するマネージャ
	 * @param config
	 *            負荷テストの設定
	 * @param throttle
	 *            スループット制御オブジェクト
	 */
	public Agent(int id, Manager manager, Config config, Throttle throttle) {
		this.id = id;
		this.manager = manager;
		this.config = config;
		this.throttle = throttle;
		this.record = new Record(config);
	}

	/**
	 * スレッドを起動して、負荷シナリオを開始します。
	 */
	public void start() {
		if (thread == null) {
			this.thread = new Thread(this, "agent" + Integer.toString(id)); //$NON-NLS-1$
			thread.start();
		}
	}

	/**
	 * 負荷シナリオを実行します。
	 * <p>
	 * <ol>
	 * <li>{@code init()}ファンクションを1回実行します。
	 * <li>初期化処理の完了ラッチを解除します。
	 * <li>測定の開始ラッチが解除されるのを待ちます。
	 * <li>測定開始時刻をインスタンス変数に登録します。
	 * <li>以下の条件を満たす間{@code run()}
	 * ファンクションを繰り返し実行します。
	 * <ul>
	 * <li>負荷テストが正常に行われている
	 * <li>負荷テストの進捗状況が{@code RUNNING}である
	 * <li>スクリプトの停止フラグが有効になっていない
	 * </ul>
	 * <li>測定の完了ラッチを解除します。
	 * <li>終了処理の開始ラッチが解除されるのを待ちます。
	 * <li>{@code fin()}ファンクションを1回実行します。
	 * <li>終了処理の完了ラッチを解除します。
	 * </ol>
	 * <p>
	 * 途中で例外が発生した場合は、以降の処理は中止してマネージャに例外を通知します。
	 */
	@Override
	public void run() {
		Helper helper = null;

		try {
			helper = new Helper(config, this);

			// 初期化処理を行う
			callInit(helper);
			manager.getInitEndLatch().countDown();

			// 測定を行う
			while (manager.getRunStartLatch().getCount() > 0) {
				try {
					manager.getRunStartLatch().await();
				} catch (InterruptedException e) {
					putMessage(new Message(Message.Level.TRACE, Resources
							.getString("Agent.INTERRUPTED_EXCEPTION"))); //$NON-NLS-1$
				}
			}

			record.setStartTime(manager.getStartTime());

			while (manager.getCondition() && (manager.getStatus() == Manager.Status.RUNNING)
					&& !doBreak) {

				long txBeginTime = 0L;
				long txEndTime = 0L;

				txBeginTime = System.nanoTime();
				callRun(helper);
				txEndTime = System.nanoTime();

				manager.incrementTxCount(txType);
				record.add(txType, txBeginTime, txEndTime);
				sleep();
				throttle(txEndTime);
			}

			manager.getRunEndLatch().countDown();

			// 終了処理を行う
			while (manager.getFinStartLatch().getCount() > 0) {
				try {
					manager.getFinStartLatch().await();
				} catch (InterruptedException e) {
					putMessage(new Message(Message.Level.TRACE, Resources
							.getString("Agent.INTERRUPTED_EXCEPTION"))); //$NON-NLS-1$
				}
			}

			if (manager.getCondition()) {
				callFin(helper);
			}

			manager.getFinEndLatch().countDown();

		} catch (ApplicationException e) {
			putMessage(new Message(Message.Level.ERROR,
					Resources.getString("Agent.EXCEPTION_1") + id //$NON-NLS-1$
							+ Resources.getString("Agent.EXCEPTION_2"), e)); //$NON-NLS-1$

		} catch (Exception e) {
			putMessage(new Message(Message.Level.ERROR,
					Resources.getString("Agent.EXCEPTION_1") + id //$NON-NLS-1$
							+ Resources.getString("Agent.EXCEPTION_2"), e)); //$NON-NLS-1$

		} finally {
			if (helper != null) {
				helper.closeScript();
			}
		}
	}

	/**
	 * エージェントのIDを返します。
	 *
	 * @return エージェントのID
	 */
	public int getId() {
		return id;
	}

	/**
	 * エージェントが実行したトランザクションの記録を返します。
	 *
	 * @return トランザクションの記録
	 */
	public Record getRecord() {
		return record;
	}

	/**
	 * エージェントが起動したスレッドを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return スレッド。スレッドがまだ起動されていないときは{@code null}
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * 測定の停止フラグを立てます。
	 */
	public void setBreak() {
		this.doBreak = true;
	}

	/**
	 * トランザクション種別を設定します。
	 *
	 * @param txType
	 *            トランザクション種別の番号。これは0以上{@code
	 *            Config#getNTxTypes()}
	 *            未満である必要があります
	 * @throws IllegalArgumentException
	 *             {@code txType}が範囲外の場合
	 */
	public void setTxType(int txType) {
		if ((txType >= 0) && (txType < config.getNTxTypes())) {
			this.txType = txType;
		} else {
			throw new IllegalArgumentException(Resources.getString("Agent.TXTYPE_OUT_OF_RANGE") //$NON-NLS-1$
					+ txType);
		}
	}

	/**
	 * マネージャにメッセージを通知します。
	 *
	 * @param message
	 *            メッセージ
	 * @see Manager#putMessage(Message)
	 */
	public void putMessage(Message message) {
		manager.putMessage(message);
	}

	private void callInit(Helper helper) throws ApplicationException {
		Connection connection = null;

		try {
			connection = manager.getConnection();
			helper.setConnection(connection);
			helper.callInit();

		} catch (SQLException e) {
			throw new ApplicationException(Resources.getString("Agent.SQL_EXCEPTION"), e); //$NON-NLS-1$

		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}
		}
	}

	private void callRun(Helper helper) throws ApplicationException {
		Connection connection = null;

		try {
			connection = manager.getConnection();
			helper.setConnection(connection);
			helper.callRun();

		} catch (SQLException e) {
			throw new ApplicationException(Resources.getString("Agent.SQL_EXCEPTION"), e); //$NON-NLS-1$

		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}
		}
	}

	private void callFin(Helper helper) throws ApplicationException {
		Connection connection = null;

		try {
			connection = manager.getConnection();
			helper.setConnection(connection);
			helper.callFin();

		} catch (SQLException e) {
			throw new ApplicationException(Resources.getString("Agent.SQL_EXCEPTION"), e); //$NON-NLS-1$

		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// 何もしない
				}
			}
		}
	}

	private void sleep() {
		if ((config.getSleepTime(txType) > 0L) && manager.getCondition()
				&& (manager.getStatus() == Manager.Status.RUNNING)) {
			try {
				Thread.sleep(config.getSleepTime(txType));
			} catch (InterruptedException e) {
				putMessage(new Message(Message.Level.TRACE, Resources
						.getString("Agent.INTERRUPTED_EXCEPTION"))); //$NON-NLS-1$
			}
		}
	}

	private void throttle(long txEndTime) {
		if ((config.getThrottle(txType) > 0) && manager.getCondition()
				&& (manager.getStatus() == Manager.Status.RUNNING)) {
			try {
				Thread.sleep(throttle.getSleepTime(txType, txEndTime));
			} catch (InterruptedException e) {
				putMessage(new Message(Message.Level.TRACE, Resources
						.getString("Agent.INTERRUPTED_EXCEPTION"))); //$NON-NLS-1$
			}

		}
	}
}

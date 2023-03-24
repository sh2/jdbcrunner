package jdbcrunner;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;

/**
 * 負荷テストを管理するクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Manager {
	/**
	 * 負荷テストの進捗状況を表す列挙型クラスです。
	 *
	 * @author Sadao Hiratsuka
	 */
	public static enum Status {
		/**
		 * 初期化処理を行っていることを表す列挙子です。
		 */
		INITIALIZING,

		/**
		 * 測定を行っていることを表す列挙子です。
		 */
		RUNNING,

		/**
		 * 終了処理を行っていることを表す列挙子です。
		 */
		FINALIZING
	}

	/**
	 * このアプリケーションの名称です。
	 */
	public static final String APPLICATION_NAME = "JdbcRunner"; //$NON-NLS-1$

	/**
	 * このアプリケーションのバージョン番号です。
	 */
	public static final String VERSION = Resources.getString("Manager.VERSION"); //$NON-NLS-1$

	/**
	 * 負荷テストが成功したことを表す定数です。
	 */
	public static final int RETURN_SUCCESS = 0;

	/**
	 * 負荷テストが失敗したことを表す定数です。
	 */
	public static final int RETURN_FAILURE = 1;

	private static final int DATASOURCE_NO_LIMIT = -1;
	private static final Logger log = Logger.getLogger(Manager.class);

	private final Config config;
	private final CountDownLatch initEndLatch;
	private final CountDownLatch runStartLatch = new CountDownLatch(1);
	private final CountDownLatch progressEndLatch = new CountDownLatch(1);
	private final CountDownLatch runEndLatch;
	private final CountDownLatch finStartLatch = new CountDownLatch(1);
	private final CountDownLatch finEndLatch;
	private final AtomicIntegerArray txCount;
	private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();

	private volatile BasicDataSource dataSource;
	private volatile Thread thread;
	private volatile Status status;
	private volatile boolean condition = true;
	private volatile long startTime;

	/**
	 * 負荷テストの設定を指定してマネージャを構築します。
	 *
	 * @param config
	 *            負荷テストの設定
	 */
	public Manager(Config config) {
		this.config = config;
		this.initEndLatch = new CountDownLatch(config.getNAgents());
		this.runEndLatch = new CountDownLatch(config.getNAgents());
		this.finEndLatch = new CountDownLatch(config.getNAgents());
		this.txCount = new AtomicIntegerArray(config.getNTxTypes());
	}

	/**
	 * 負荷テストを実行します。
	 * <p>
	 * <ol>
	 * <li>進捗状況を{@code INITIALIZING}へ更新します。
	 * <li>このメソッドを呼び出したスレッドをインスタンス変数に登録します。
	 * <li>負荷テストの設定をログに出力します。
	 * <li>メッセージの取り出しを開始します。
	 * <li>データソースを構築します。
	 * <li>スループット制御オブジェクトを構築します。
	 * <li>エージェント群を構築して初期化処理を開始させます。
	 * <li>初期化処理の完了ラッチが解除されるのを待ちます。
	 * <li>測定開始時刻として現在時刻をインスタンス変数に登録します。
	 * <li>スループットの算出基準時刻として測定開始時刻を設定します。
	 * <li>進捗状況を{@code RUNNING}へ更新します。
	 * <li>進捗の監視を開始します。
	 * <li>測定の開始ラッチを解除します。
	 * <li>進捗監視の完了ラッチが解除されるのを待ちます。
	 * <li>進捗状況を{@code FINALIZING}へ更新します。
	 * <li>進捗の監視を終了します。
	 * <li>測定の完了ラッチが解除されるのを待ちます。
	 * <li>終了処理の開始ラッチを解除します。
	 * <li>終了処理の完了ラッチが解除されるのを待ちます。
	 * <li>すべてのエージェントのスレッドが終了するのを待ちます。
	 * <li>メッセージの取り出しを終了します。
	 * <li>データソースを閉じます。
	 * <li>測定結果を出力します。
	 * <li>負荷テストが成功したかどうかを{@code RETURN_SUCCESS}
	 * または{@code RETURN_FAILURE}で呼び出し元に返します。
	 * </ol>
	 * <p>
	 * 負荷テストの設定でロードモードが有効になっている場合、
	 * 12～17番の代わりに以下の処理を行います。
	 * <ol>
	 * <li>測定の開始ラッチを解除します。
	 * <li>測定の完了ラッチが解除されるのを待ちます。
	 * <li>進捗状況を{@code FINALIZING}へ更新します。
	 * </ol>
	 *
	 * @return 負荷テストが成功したかどうかを表す整数
	 */
	public int measure() {
		// 初期化処理を行う
		this.status = Status.INITIALIZING;
		this.thread = Thread.currentThread();
		log.info(config.getConfigString());

		Receiver receiver = new Receiver();
		receiver.start();

		this.dataSource = createDataSource();
		Throttle throttle = new Throttle(config);
		List<Agent> agentList = createAgentList(throttle);
		startAgents(agentList);
		waitInit();

		// 測定を行う
		if (config.isLoad()) {
			this.startTime = System.nanoTime();
			throttle.setBaseTime(startTime);
			this.status = Status.RUNNING;
			runStartLatch.countDown();
			waitRun();

			// 終了処理を行う
			this.status = Status.FINALIZING;
		} else {
			ScheduledExecutorService progress = createProgressExecutor();
			this.startTime = System.nanoTime();
			throttle.setBaseTime(startTime);
			this.status = Status.RUNNING;
			progress.scheduleAtFixedRate(new Progress(), 1, 1, TimeUnit.SECONDS);
			runStartLatch.countDown();
			waitProgress();

			// 終了処理を行う
			this.status = Status.FINALIZING;
			progress.shutdownNow();
			waitRun();
		}

		finStartLatch.countDown();
		waitFin();
		waitShutdown(agentList);
		receiver.stop();
		closeDataSource();

		if (!config.isLoad()) {
			printResult(agentList);
		}

		if (condition) {
			return RETURN_SUCCESS;
		} else {
			return RETURN_FAILURE;
		}
	}

	/**
	 * {@code measure()}メソッドを呼んだスレッドを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return スレッド。{@code measure()}
	 *         メソッドがまだ呼ばれていない場合は{@code null}
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * データベースへの接続を返します。
	 * <p>
	 * このメソッドはスレッドセーフです。スレッドセーフであることは
	 * {@code DataSource}の実装によって担保されます。
	 *
	 * @return データベースへの接続
	 * @throws SQLException
	 *             データベースアクセス中に例外が発生した場合
	 */
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	/**
	 * 初期化処理の完了ラッチを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 初期化処理の完了ラッチ
	 */
	public CountDownLatch getInitEndLatch() {
		return initEndLatch;
	}

	/**
	 * 測定の開始ラッチを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 測定の開始ラッチ
	 */
	public CountDownLatch getRunStartLatch() {
		return runStartLatch;
	}

	/**
	 * 測定の完了ラッチを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 測定の完了ラッチ
	 */
	public CountDownLatch getRunEndLatch() {
		return runEndLatch;
	}

	/**
	 * 終了処理の開始ラッチを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 終了処理の開始ラッチ
	 */
	public CountDownLatch getFinStartLatch() {
		return finStartLatch;
	}

	/**
	 * 終了処理の完了ラッチを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 終了処理の完了ラッチ
	 */
	public CountDownLatch getFinEndLatch() {
		return finEndLatch;
	}

	/**
	 * 負荷テストが正常に行われているかどうかを返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 負荷テストが正常に行われているかどうか
	 */
	public boolean getCondition() {
		return condition;
	}

	/**
	 * 負荷テストの進捗状況を返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 負荷テストの進捗状況
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * 測定開始時刻を返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @return 測定開始時刻
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * トランザクションの実行回数を1増やします。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @param txType
	 *            トランザクション種別
	 */
	public void incrementTxCount(int txType) {
		txCount.incrementAndGet(txType);
	}

	/**
	 * トランザクションの実行回数を返します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @param txType
	 *            トランザクション種別
	 * @return トランザクションの実行回数
	 */
	public int getTxCount(int txType) {
		return txCount.get(txType);
	}

	/**
	 * メッセージキューにメッセージを登録します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @param message
	 *            メッセージ
	 */
	public void putMessage(Message message) {
		try {
			messageQueue.put(message);
		} catch (InterruptedException e) {
			log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
		}
	}

	private BasicDataSource createDataSource() {
		BasicDataSource dataSource = new BasicDataSource();

		dataSource.setDriverClassName(config.getJdbcDriver());
		dataSource.setUrl(config.getJdbcUrl());
		dataSource.setUsername(config.getJdbcUser());
		dataSource.setPassword(config.getJdbcPass());
		dataSource.setDefaultAutoCommit(config.isAutoCommit());
		dataSource.setInitialSize(config.getConnPoolSize());
		dataSource.setMaxTotal(config.getConnPoolSize());

		// MaxIdleはデフォルト値が8。無制限に変更する
		dataSource.setMaxIdle(DATASOURCE_NO_LIMIT);

		if (config.getStmtCacheSize() > 0) {
			dataSource.setPoolPreparedStatements(true);
			dataSource.setMaxOpenPreparedStatements(config.getStmtCacheSize());
		} else {
			dataSource.setPoolPreparedStatements(false);
		}

		return dataSource;
	}

	private List<Agent> createAgentList(Throttle throttle) {
		List<Agent> agentList = new ArrayList<Agent>();

		for (int id = 0; id < config.getNAgents(); id++) {
			agentList.add(new Agent(id, this, config, throttle));
		}

		return agentList;
	}

	private void startAgents(List<Agent> agentList) {
		for (Agent agent : agentList) {
			agent.start();
		}
	}

	private void waitInit() {
		if (condition) {
			try {
				initEndLatch.await();
			} catch (InterruptedException e) {
				log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
			}
		}
	}

	private ScheduledExecutorService createProgressExecutor() {
		return Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable runnable) {
				return new Thread(runnable, "progress"); //$NON-NLS-1$
			}
		});
	}

	private void waitProgress() {
		if (condition) {
			try {
				progressEndLatch.await();
			} catch (InterruptedException e) {
				log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
			}
		}
	}

	private void waitRun() {
		if (condition) {
			try {
				runEndLatch.await();
			} catch (InterruptedException e) {
				log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
			}
		}
	}

	private void waitFin() {
		if (condition) {
			try {
				finEndLatch.await();
			} catch (InterruptedException e) {
				log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
			}
		}
	}

	private void waitShutdown(List<Agent> agentList) {
		for (Agent agent : agentList) {
			do {
				try {
					agent.getThread().join();
				} catch (InterruptedException e) {
					log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
				}
			} while (agent.getThread().isAlive());
		}
	}

	private void closeDataSource() {
		try {
			dataSource.close();
		} catch (SQLException e) {
			// 何もしない
		}
	}

	private void printResult(List<Agent> agentList) {
		if (condition) {
			Result result = new Result(config);

			for (Agent agent : agentList) {
				result.addRecord(agent.getRecord());
			}

			result.printSummary();

			try {
				result.writeThroughputLog();
				result.writeResponseLog();
			} catch (IOException e) {
				log.error(Resources.getString("Manager.IO_EXCEPTION"), e); //$NON-NLS-1$
				this.condition = false;
			}
		}
	}

	/**
	 * メッセージキューに登録されたメッセージを取り出してログに出力するクラスです。
	 *
	 * @author Sadao Hiratsuka
	 */
	private class Receiver implements Runnable {
		private static final long POLL_TIMEOUT = 1000L;

		private Thread thread;
		private volatile boolean doReceive = true;

		/**
		 * スレッドを起動して、メッセージの取り出しとログ出力を開始します。
		 */
		public void start() {
			if (thread == null) {
				this.thread = new Thread(this, "receiver"); //$NON-NLS-1$
				thread.start();
			}
		}

		/**
		 * メッセージの取り出しとログ出力を行います。
		 * <p>
		 * このメソッドは、以下の処理を繰り返し行います。
		 * <ol>
		 * <li>
		 * 負荷テストが正常に行われているか、受信フラグが有効かどうかを確認します。
		 * どちらかの条件を満たしていない場合はスレッドを終了します。
		 * <li>メッセージキューからメッセージを取り出し、ログに出力します。
		 * <li>メッセージのレベルが
		 * {@code Message.Level.ERROR}
		 * だった場合は、受信フラグと
		 * 負荷テストの正常フラグを無効にしてマネージャに割り込みをかけます。
		 * </ol>
		 * 途中で{@code InterruptedException}
		 * 以外の例外が発生した場合は繰り返し処理を終了し、
		 * 受信フラグと負荷テストの正常フラグを無効にしてマネージャに割り込みをかけます。
		 */
		@Override
		public void run() {
			try {
				while (condition && doReceive) {
					try {
						Message message = messageQueue.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);

						if (message != null) {
							switch (message.getLevel()) {
							case TRACE:
								log.trace(message.getMessage());
								break;
							case DEBUG:
								log.debug(message.getMessage());
								break;
							case INFO:
								log.info(message.getMessage());
								break;
							case WARN:
								log.warn(message.getMessage());
								break;
							case ERROR:
								log.error(message.getMessage(), message.getThrowable());
								this.doReceive = false;
								Manager.this.condition = false;
								Manager.this.getThread().interrupt();
								break;
							default:
								break;
							}
						}
					} catch (InterruptedException e) {
						log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
					}
				}
			} catch (Exception e) {
				log.error(Resources.getString("Manager.RECEIVER_EXCEPTION"), e); //$NON-NLS-1$
				this.doReceive = false;
				Manager.this.condition = false;
				Manager.this.getThread().interrupt();
			}
		}

		/**
		 * メッセージを取り出すスレッドを停止し、
		 * 残りのメッセージを取り出してログに出力します。
		 */
		public void stop() {
			this.doReceive = false;

			do {
				thread.interrupt();

				try {
					thread.join();
				} catch (InterruptedException e) {
					log.trace(Resources.getString("Manager.INTERRUPTED_EXCEPTION")); //$NON-NLS-1$
				}
			} while (thread.isAlive());

			// キューに残ったメッセージを処理する
			while (condition) {
				Message message = messageQueue.poll();

				if (message == null) {
					break;
				} else {
					switch (message.getLevel()) {
					case TRACE:
						log.trace(message.getMessage());
						break;
					case DEBUG:
						log.debug(message.getMessage());
						break;
					case INFO:
						log.info(message.getMessage());
						break;
					case WARN:
						log.warn(message.getMessage());
						break;
					case ERROR:
						// LEVEL_ERRORが発生した場合、Agentによってラッチが解除されないため、
						// Receiverスレッドがメッセージを受け取るまでwaitFin()が終了しない。
						// そのためここには来ない
						log.error(message.getMessage(), message.getThrowable());
						Manager.this.condition = false;
						break;
					default:
						break;
					}
				}
			}
		}
	}

	/**
	 * 負荷テストの進捗状況を監視し、進捗状況をログに出力するクラスです。
	 *
	 * @author Sadao Hiratsuka
	 */
	private class Progress implements Runnable {
		private boolean doPrint = true;
		private int elapsedTime = 0 - config.getWarmupTime();
		private int[] referenceCount = new int[config.getNTxTypes()];
		private int[] prevCount = new int[config.getNTxTypes()];

		/**
		 * 負荷テストの進捗状況を監視し、進捗状況をログに出力します。
		 * <p>
		 * <ol>
		 * <li>経過時間を1増やします。
		 * <li>トランザクションの実行回数を取得します。
		 * <li>経過時間が0の場合、
		 * 現在までのトランザクションの実行回数を基準値として保存します。
		 * <li>
		 * 負荷テストが正常に行われているか、表示フラグが有効かどうかを確認します。
		 * いずれの条件も満たしている場合、スループットとトランザクションの実行回数を
		 * 整形してメッセージキューに登録します。
		 * <li>表示フラグが有効でかつ経過時間が測定時間以上になった場合、
		 * 表示フラグを無効にして進捗監視の完了ラッチを解除します。
		 * </ol>
		 * 途中で例外が発生した場合は、
		 * 表示フラグと負荷テストの正常フラグを無効にしてマネージャに割り込みをかけます。
		 */
		@Override
		public void run() {
			try {
				this.elapsedTime++;
				int[] txCount = new int[config.getNTxTypes()];

				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					txCount[txType] = Manager.this.getTxCount(txType);
				}

				if (elapsedTime == 0) {
					for (int txType = 0; txType < config.getNTxTypes(); txType++) {
						this.referenceCount[txType] = txCount[txType];
					}
				}

				if (condition && doPrint) {
					print(txCount);
					checkClock();
				}

				if (doPrint && (elapsedTime >= config.getMeasurementTime())) {
					this.doPrint = false;
					progressEndLatch.countDown();
				}
			} catch (Exception e) {
				log.error(Resources.getString("Manager.PROGRESS_EXCEPTION"), e); //$NON-NLS-1$
				this.doPrint = false;
				Manager.this.condition = false;
				Manager.this.getThread().interrupt();
			}
		}

		private void print(int[] txCount) {
			int[] throughput = new int[config.getNTxTypes()];

			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				throughput[txType] = txCount[txType] - prevCount[txType];
				this.prevCount[txType] = txCount[txType];
			}

			StringBuilder message = new StringBuilder();

			if (elapsedTime <= 0) {
				message.append("[Warmup] "); //$NON-NLS-1$
			} else {
				message.append("[Progress] "); //$NON-NLS-1$
			}

			message.append(elapsedTime);
			message.append(" sec, "); //$NON-NLS-1$

			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				message.append(throughput[txType]);
				message.append(","); //$NON-NLS-1$
			}

			message.deleteCharAt(message.length() - 1);
			message.append(" tps, "); //$NON-NLS-1$

			if (elapsedTime <= 0) {
				message.append("("); //$NON-NLS-1$

				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					message.append(txCount[txType]);
					message.append(","); //$NON-NLS-1$
				}

				message.deleteCharAt(message.length() - 1);
				message.append(" tx)"); //$NON-NLS-1$
			} else {
				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					message.append(txCount[txType] - referenceCount[txType]);
					message.append(","); //$NON-NLS-1$
				}

				message.deleteCharAt(message.length() - 1);
				message.append(" tx"); //$NON-NLS-1$
			}

			putMessage(new Message(Message.Level.INFO, message.toString()));
		}

		private void checkClock() {
			int actualTime = (int) ((System.nanoTime() - startTime) / 1000000000L)
					- config.getWarmupTime();

			if (elapsedTime < actualTime) {
				putMessage(new Message(Message.Level.WARN,
						Resources.getString("Manager.PROGRESS_DELAY") //$NON-NLS-1$
								+ actualTime + "sec")); //$NON-NLS-1$
			}
		}
	}
}

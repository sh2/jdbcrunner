package jdbcrunner;

/**
 * スループットの上限値を制御するクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Throttle {
	private static final long BUSY_THRESHOLD = 1000000000L;

	private final Config config;
	private final int[] txCount;
	private final boolean[] wasBusy;
	private final long[] baseTime;
	private final long[] busySince;

	/**
	 * 負荷テストの設定を指定してスループット制御オブジェクトを構築します。
	 *
	 * @param config
	 *            負荷テストの設定
	 */
	public Throttle(Config config) {
		this.config = config;

		if (config.doThrottleByTotal()) {
			this.txCount = new int[1];
			this.wasBusy = new boolean[1];
			this.baseTime = new long[1];
			this.busySince = new long[1];
		} else {
			this.txCount = new int[config.getNTxTypes()];
			this.wasBusy = new boolean[config.getNTxTypes()];
			this.baseTime = new long[config.getNTxTypes()];
			this.busySince = new long[config.getNTxTypes()];
		}
	}

	/**
	 * スループットを算出する基準となる時刻を設定します。
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @param time
	 *            スループットを算出する基準となる時刻
	 */
	public synchronized void setBaseTime(long time) {
		for (int index = 0; index < baseTime.length; index++) {
			baseTime[index] = time;
		}
	}

	/**
	 * スループット上限値を維持するための、スリープ時間を求めます。
	 * <p>
	 * <ol>
	 * <li>これまで実行したトランザクション回数から、
	 * 指定したスループットで本来経過しているべき時間を求め
	 * {@code expectedTime}とします。
	 * <li>実際に経過した時間{@code actualTime}を求め、
	 * {@code expectedTime - actualTime}
	 * をスリープ時間とします。
	 * <li>スリープ時間が負数の場合は、0とします。
	 * <li>
	 * スリープ時間が0、つまりトランザクションが追いついていない状態が一定時間続いていた場合
	 * 、スループットを算出する基準となる時刻およびトランザクションの実行回数をリセットします
	 * 。
	 * </ol>
	 * <p>
	 * このメソッドはスレッドセーフです。
	 *
	 * @param txType
	 *            トランザクション種別
	 * @param txEndTime
	 *            トランザクション終了時刻
	 * @return スリープ時間
	 */
	public synchronized long getSleepTime(int txType, long txEndTime) {
		int txIndex = config.doThrottleByTotal() ? 0 : txType;

		long expectedTime = (long) (1000.0 * ++txCount[txIndex] / config.getThrottle(txIndex));
		long actualTime = (txEndTime - baseTime[txIndex]) / 1000000L;
		long sleepTime = actualTime < expectedTime ? expectedTime - actualTime : 0L;

		if (sleepTime == 0L) {
			// トランザクションが追いついていない状態が続いているかどうか調べる
			if (wasBusy[txIndex]) {
				if (busySince[txIndex] + BUSY_THRESHOLD < txEndTime) {
					// スループットを算出する基準値をリセットする
					txCount[txIndex] = 0;
					baseTime[txIndex] = txEndTime;
					wasBusy[txIndex] = false;
				}
			} else {
				busySince[txIndex] = txEndTime;
				wasBusy[txIndex] = true;
			}
		} else {
			wasBusy[txIndex] = false;
		}

		return sleepTime;
	}
}

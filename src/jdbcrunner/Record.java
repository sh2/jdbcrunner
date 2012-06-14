package jdbcrunner;

import java.util.HashMap;
import java.util.Map;

/**
 * トランザクションの実行回数とレスポンスタイムを格納するクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Record {
	/**
	 * レスポンスタイムを配列に記録するしきい値です。このしきい値以上のレスポンスタイムは
	 * {@code Map}に記録されます。
	 */
	public static final int RESPONSE_THRESHOLD = 1000;

	private final Config config;
	private final int[][] txData;
	private final int[][] responseData;
	private final Map<Integer, int[]> slowResponseMap = new HashMap<Integer, int[]>();

	private long startTime;

	/**
	 * 負荷テストの設定を指定して{@code Record}を構築します。
	 *
	 * @param config
	 */
	public Record(Config config) {
		this.config = config;
		this.txData = new int[config.getMeasurementTime()][config.getNTxTypes()];
		this.responseData = new int[RESPONSE_THRESHOLD][config.getNTxTypes()];
	}

	/**
	 * トランザクションの実行回数を返します。
	 * <p>
	 * 1つ目の添え字は経過時間のインデックスを表します。
	 * 0は測定開始後0秒以上1秒未満の間に完了したトランザクションのことを示します。
	 * 2つ目の添え字はトランザクション種別を表します。
	 *
	 * @return トランザクションの実行回数
	 */
	public int[][] getTxData() {
		int[][] returnTxData = new int[config.getMeasurementTime()][config.getNTxTypes()];

		for (int elapsedIndex = 0; elapsedIndex < config.getMeasurementTime(); elapsedIndex++) {
			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				returnTxData[elapsedIndex][txType] = txData[elapsedIndex][txType];
			}
		}

		return returnTxData;
	}

	/**
	 * レスポンスタイムが{@code RESPONSE_THRESHOLD}
	 * 未満のものについて、トランザクションのレスポンスタイムを返します。
	 * <p>
	 * 1つ目の添え字はレスポンスタイムを表します。
	 * 0はレスポンスタイムが0ミリ秒以上1ミリ秒未満であることを示します。
	 * 2つ目の添え字はトランザクション種別を表します。
	 *
	 * @return トランザクションのレスポンスタイム
	 */
	public int[][] getResponseData() {
		int[][] returnResponseData = new int[RESPONSE_THRESHOLD][config.getNTxTypes()];

		for (int responseTime = 0; responseTime < Record.RESPONSE_THRESHOLD; responseTime++) {
			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				returnResponseData[responseTime][txType] = responseData[responseTime][txType];
			}
		}
		return returnResponseData;
	}

	/**
	 * レスポンスタイムが{@code RESPONSE_THRESHOLD}
	 * 以上のものについて、トランザクションのレスポンスタイムを返します。
	 * <p>
	 * {@code Map}のキーはレスポンスタイムを表します。
	 * 0はレスポンスタイムが0ミリ秒以上1ミリ秒未満であることを示します。
	 * {@code Map}の値における配列の添え字は、トランザクション種別を表します。
	 *
	 * @return トランザクションのレスポンスタイム
	 */
	public Map<Integer, int[]> getSlowResponseMap() {
		return slowResponseMap;
	}

	/**
	 * 測定開始時刻を登録します。
	 *
	 * @param startTime
	 *            測定開始時刻
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * トランザクションを記録します。
	 * <p>
	 * <ol>
	 * <li>経過時間から配列のインデックスを求めます。
	 * <li>配列のインデックスが範囲外の場合は何もせずに終了します。
	 * <li>トランザクションの実行回数を1増やします。
	 * <li>レスポンスタイムを求めます。
	 * <li>レスポンスタイムが{@code RESPONSE_THRESHOLD}
	 * 未満の場合は、レスポンスタイムを配列に記録します。
	 * <li>レスポンスタイムが{@code RESPONSE_THRESHOLD}
	 * 以上の場合は、レスポンスタイムを{@code Map}に記録します。
	 * </ol>
	 *
	 * @param txType
	 *            トランザクション種別
	 * @param txBeginTime
	 *            トランザクション開始時刻
	 * @param txEndTime
	 *            トランザクション終了時刻
	 */
	public void add(int txType, long txBeginTime, long txEndTime) {
		int elapsedIndex = (int) ((txEndTime - startTime) / 1000000000L) - config.getWarmupTime();

		if ((elapsedIndex >= 0) && (elapsedIndex < config.getMeasurementTime())) {
			txData[elapsedIndex][txType]++;

			int responseTime = (int) ((txEndTime - txBeginTime) / 1000000L);

			if (responseTime >= 0) {
				if (responseTime < RESPONSE_THRESHOLD) {
					responseData[responseTime][txType]++;

				} else if (slowResponseMap.containsKey(responseTime)) {
					slowResponseMap.get(responseTime)[txType]++;

				} else {
					int[] txSubCount = new int[config.getNTxTypes()];
					txSubCount[txType] = 1;
					slowResponseMap.put(responseTime, txSubCount);
				}
			}
		}
	}
}

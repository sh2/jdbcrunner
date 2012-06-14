package jdbcrunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * 負荷テストの測定結果を収集して出力するクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class Result {
	private static final Logger log = Logger.getLogger(Result.class);

	private final Config config;
	private final int[][] txData;
	private final int[][] responseData;
	private final SortedMap<Integer, int[]> slowResponseMap = new TreeMap<Integer, int[]>();
	private final int[] totalTxCount;
	private final int[] totalResponseCount;

	/**
	 * 負荷テストの設定を指定して{@code Result}を構築します。
	 *
	 * @param config
	 *            負荷テストの設定
	 */
	public Result(Config config) {
		this.config = config;
		this.txData = new int[config.getMeasurementTime()][config.getNTxTypes()];
		this.responseData = new int[Record.RESPONSE_THRESHOLD][config.getNTxTypes()];
		this.totalTxCount = new int[config.getNTxTypes()];
		this.totalResponseCount = new int[config.getNTxTypes()];
	}

	/**
	 * {@code Record}に格納されたトランザクションの実行回数と
	 * レスポンスタイムのデータを、{@code Result}に加えます。
	 *
	 * @param record
	 *            加える{@code Record}
	 */
	public void addRecord(Record record) {
		addTxData(record.getTxData());
		addResponseData(record.getResponseData());
		addSlowResponseMap(record.getSlowResponseMap());
	}

	/**
	 * 測定結果のサマリをログに出力します。
	 * <p>
	 * 以下のデータをトランザクション種別ごとに出力します。
	 * <ol>
	 * <li>トランザクションの実行回数
	 * <li>スループット
	 * <li>レスポンスタイムの最小値
	 * <li>レスポンスタイムの50パーセンタイル値(中央値)
	 * <li>レスポンスタイムの90パーセンタイル値
	 * <li>レスポンスタイムの95パーセンタイル値
	 * <li>レスポンスタイムの99パーセンタイル値
	 * <li>レスポンスタイムの最大値
	 * </ol>
	 */
	public void printSummary() {
		printLine("[Total tx count]", //$NON-NLS-1$
				"tx", totalTxCount); //$NON-NLS-1$
		printLine("[Throughput]", //$NON-NLS-1$
				"tps", getThroughput()); //$NON-NLS-1$
		printLine("[Response time (minimum)]", //$NON-NLS-1$
				"msec", getResponsePercentile(0)); //$NON-NLS-1$
		printLine("[Response time (50%tile)]", //$NON-NLS-1$
				"msec", getResponsePercentile(50)); //$NON-NLS-1$
		printLine("[Response time (90%tile)]", //$NON-NLS-1$
				"msec", getResponsePercentile(90)); //$NON-NLS-1$
		printLine("[Response time (95%tile)]", //$NON-NLS-1$
				"msec", getResponsePercentile(95)); //$NON-NLS-1$
		printLine("[Response time (99%tile)]", //$NON-NLS-1$
				"msec", getResponsePercentile(99)); //$NON-NLS-1$
		printLine("[Response time (maximum)]", //$NON-NLS-1$
				"msec", getResponsePercentile(100)); //$NON-NLS-1$
	}

	/**
	 * スループットのデータをCSV形式でファイルに出力します。
	 * <p>
	 * <ol>
	 * <li>「{@literal
	 * <ログの出力先ディレクトリ>/log_<プログラムの開始日時>_t.log}」
	 * という形式で、出力ファイル名を求めます。
	 * <li>
	 * トランザクションの種類数が1の場合、以下のフォーマットでデータを出力します。
	 *
	 * <pre>
	 * Elapsed time[sec],Throughput[tps]
	 * 1,7879
	 * 2,7902
	 * 3,7914
	 * </pre>
	 *
	 * トランザクションの種類数が2以上の場合は、以下のフォーマットでデータを出力します。
	 *
	 * <pre>
	 * Elapsed time[sec],Throughput(tx0)[tps],Throughput(tx1)[tps]
	 * 1,2339,5256
	 * 2,1116,6758
	 * 3,1907,5926
	 * </pre>
	 *
	 * </ol>
	 *
	 * @throws IOException
	 *             ファイルを開けなかった場合、ファイルに書き込めなかった場合
	 */
	public void writeThroughputLog() throws IOException {
		FileWriter writer = null;
		BufferedWriter buffer = null;
		StringBuilder line = new StringBuilder();

		try {
			writer = new FileWriter(config.getLogDir() + File.separator + "log_" //$NON-NLS-1$
					+ config.getProgramStartTime() + "_t.csv"); //$NON-NLS-1$
			buffer = new BufferedWriter(writer);

			line.append("Elapsed time[sec]"); //$NON-NLS-1$

			if (config.getNTxTypes() == 1) {
				line.append(",Throughput[tps]"); //$NON-NLS-1$
			} else {
				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					line.append(",Throughput(tx"); //$NON-NLS-1$
					line.append(txType);
					line.append(")[tps]"); //$NON-NLS-1$
				}
			}

			buffer.write(line.toString());
			buffer.newLine();

			for (int elapsedIndex = 0; elapsedIndex < config.getMeasurementTime(); elapsedIndex++) {
				line.setLength(0);
				line.append(elapsedIndex + 1);

				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					line.append(","); //$NON-NLS-1$
					line.append(txData[elapsedIndex][txType]);
				}

				buffer.write(line.toString());
				buffer.newLine();
			}

		} finally {
			if (buffer != null) {
				try {
					buffer.close();
				} catch (IOException e) {
					// 何もしない
				}
			}

			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// 何もしない
				}
			}
		}
	}

	/**
	 * レスポンスタイムのデータをCSV形式でファイルに出力します。
	 * <p>
	 * <ol>
	 * <li>「{@literal
	 * <ログの出力先ディレクトリ>/log_<プログラムの開始日時>_r.log}」
	 * という形式で、出力ファイル名を求めます。
	 * <li>
	 * トランザクションの種類数が1の場合、以下のフォーマットでデータを出力します。
	 *
	 * <pre>
	 * Response time[msec],Count
	 * 0,152402
	 * 1,1930
	 * 2,760
	 * </pre>
	 *
	 * トランザクションの種類数が2以上の場合は、以下のフォーマットでデータを出力します。
	 *
	 * <pre>
	 * Response time[msec],Count(tx0),Count(tx1)
	 * 0,8725,29067
	 * 1,104,270
	 * 2,59,118
	 * </pre>
	 *
	 * あるレスポンスタイムについてトランザクション実行回数がすべて0回の場合、
	 * その行は出力されません。
	 *
	 * </ol>
	 *
	 * @throws IOException
	 *             ファイルを開けなかった場合、ファイルに書き込めなかった場合
	 */
	public void writeResponseLog() throws IOException {
		FileWriter writer = null;
		BufferedWriter buffer = null;
		StringBuilder line = new StringBuilder();

		try {
			writer = new FileWriter(config.getLogDir() + File.separator + "log_" //$NON-NLS-1$
					+ config.getProgramStartTime() + "_r.csv"); //$NON-NLS-1$
			buffer = new BufferedWriter(writer);

			line.append("Response time[msec]"); //$NON-NLS-1$

			if (config.getNTxTypes() == 1) {
				line.append(",Count"); //$NON-NLS-1$
			} else {
				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					line.append(",Count(tx"); //$NON-NLS-1$
					line.append(txType);
					line.append(")"); //$NON-NLS-1$
				}
			}

			buffer.write(line.toString());
			buffer.newLine();

			for (int responseTime = 0; responseTime < Record.RESPONSE_THRESHOLD; responseTime++) {
				boolean doWrite = false;

				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					if (responseData[responseTime][txType] > 0) {
						doWrite = true;
						break;
					}
				}

				if (doWrite) {
					line.setLength(0);
					line.append(responseTime);

					for (int txType = 0; txType < config.getNTxTypes(); txType++) {
						line.append(","); //$NON-NLS-1$
						line.append(responseData[responseTime][txType]);
					}

					buffer.write(line.toString());
					buffer.newLine();
				}
			}

			for (int responseTime : slowResponseMap.keySet()) {
				line.setLength(0);
				line.append(responseTime);

				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					line.append(","); //$NON-NLS-1$
					line.append(slowResponseMap.get(responseTime)[txType]);
				}

				buffer.write(line.toString());
				buffer.newLine();
			}

		} finally {
			if (buffer != null) {
				try {
					buffer.close();
				} catch (IOException e) {
					// 何もしない
				}
			}

			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// 何もしない
				}
			}
		}
	}

	private void addTxData(int[][] txEntryData) {
		for (int elapsedIndex = 0; elapsedIndex < config.getMeasurementTime(); elapsedIndex++) {
			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				this.txData[elapsedIndex][txType] += txEntryData[elapsedIndex][txType];
				this.totalTxCount[txType] += txEntryData[elapsedIndex][txType];
			}
		}
	}

	private void addResponseData(int[][] responseEntryData) {
		for (int responseTime = 0; responseTime < Record.RESPONSE_THRESHOLD; responseTime++) {
			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				this.responseData[responseTime][txType] += responseEntryData[responseTime][txType];
				this.totalResponseCount[txType] += responseEntryData[responseTime][txType];
			}
		}
	}

	private void addSlowResponseMap(Map<Integer, int[]> slowResponseEntryMap) {
		for (int responseTime : slowResponseEntryMap.keySet()) {
			int[] responseEntryCount = slowResponseEntryMap.get(responseTime);

			if (slowResponseMap.containsKey(responseTime)) {
				int[] responseCount = slowResponseMap.get(responseTime);
				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					responseCount[txType] += responseEntryCount[txType];
				}
			} else {
				int[] responseCount = new int[config.getNTxTypes()];
				for (int txType = 0; txType < config.getNTxTypes(); txType++) {
					responseCount[txType] = responseEntryCount[txType];
				}
				slowResponseMap.put(responseTime, responseCount);
			}

			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				this.totalResponseCount[txType] += responseEntryCount[txType];
			}
		}
	}

	private void printLine(String header, String footer, int[] data) {
		StringBuilder message = new StringBuilder();

		message.append(header);
		message.append(" "); //$NON-NLS-1$

		for (int element : data) {
			message.append(element);
			message.append(","); //$NON-NLS-1$
		}

		message.deleteCharAt(message.length() - 1);
		message.append(" "); //$NON-NLS-1$
		message.append(footer);

		log.info(message.toString());
	}

	private void printLine(String header, String footer, double[] data) {
		StringBuilder message = new StringBuilder();
		Formatter formatter = new Formatter(message);

		message.append(header);
		message.append(" "); //$NON-NLS-1$

		for (double element : data) {
			formatter.format("%.1f,", element); //$NON-NLS-1$
		}

		message.deleteCharAt(message.length() - 1);
		message.append(" "); //$NON-NLS-1$
		message.append(footer);

		log.info(message.toString());
	}

	private double[] getThroughput() {
		double[] throughput = new double[config.getNTxTypes()];

		if (config.getMeasurementTime() > 0) {
			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				throughput[txType] = (double) totalTxCount[txType] / config.getMeasurementTime();
			}
		}

		return throughput;
	}

	private int[] getResponsePercentile(int percentile) {
		int[] targetCount = new int[config.getNTxTypes()];
		int[] responseCount = new int[config.getNTxTypes()];
		int[] responsePercentile = new int[config.getNTxTypes()];
		boolean[] isComplete = new boolean[config.getNTxTypes()];

		for (int txType = 0; txType < config.getNTxTypes(); txType++) {
			if (percentile == 0) {
				// 0のみ特別扱いしてレスポンスタイムの最小値を返す
				targetCount[txType] = 1;
			} else {
				targetCount[txType] = (int) ((long) totalResponseCount[txType] * percentile / 100);
				if ((long) totalResponseCount[txType] * percentile % 100 != 0L) {
					// 切り上げ
					targetCount[txType]++;
				}
			}
		}

		for (int responseTime = 0; responseTime < Record.RESPONSE_THRESHOLD; responseTime++) {
			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				responseCount[txType] += responseData[responseTime][txType];
				if (!isComplete[txType] && (responseCount[txType] >= targetCount[txType])) {
					responsePercentile[txType] = responseTime;
					isComplete[txType] = true;
				}
			}
		}

		for (int responseTime : slowResponseMap.keySet()) {
			for (int txType = 0; txType < config.getNTxTypes(); txType++) {
				responseCount[txType] += slowResponseMap.get(responseTime)[txType];
				if (!isComplete[txType] && (responseCount[txType] >= targetCount[txType])) {
					responsePercentile[txType] = responseTime;
					isComplete[txType] = true;
				}
			}
		}

		return responsePercentile;
	}
}

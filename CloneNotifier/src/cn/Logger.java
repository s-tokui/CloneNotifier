package cn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

/**
 * 実行時ログの出力クラス.
 * 
 * @author y-yuuki
 */
public class Logger {

	/** ログファイルに出力を行う{@link PrintWriter}クラス. */
	private static PrintWriter logger;

	/** 現在時刻を示す{@link Calendar}クラス. */
	private static Calendar now = Calendar.getInstance();

	/** 現在時刻(時). */
	private static String hour;

	/** 現在時刻(分). */
	private static String minute;

	/** 現在時刻(秒). */
	private static String second;

	/**
	 * 情報レベル. デバッグ用.
	 * 
	 * @author m-sano
	 */
	public static final int DEBUG = -1;

	/**
	 * 情報レベル. 誤りとは無関係な情報.
	 * 
	 * @author m-sano
	 */
	public static final int INFO = 0;

	/**
	 * 警告レベル. 実行は継続できるが, 実行結果に異常が出る可能性がある場合.
	 * 
	 * @author m-sano
	 */
	public static final int WARNING = 1;

	/**
	 * エラーレベル. 実行が続行不可能な場合.
	 * 
	 * @author m-sano
	 */
	public static final int ERROR = 2;

	/**
	 * 致命的レベル. 実行可能状態の有無に関わらず, プログラム上に欠陥がある場合.
	 * 
	 * @author m-sano
	 */
	public static final int FATAL = 3;

	/**
	 * システムレベル. 必ず表示するシステムメッセージ.
	 * 
	 * @author m-sano
	 */
	public static final int SYSTEM = 4;

	/**
	 * 出力を行うログレベル.
	 * 
	 * @author m-sano
	 */
	private static int level = Logger.WARNING;

	/**
	 * ロガーを初期化し, ログファイルへの書き込みを開始する.
	 * 
	 * @throws IOException
	 *             ログファイル出力に失敗した場合
	 */
	public static void init() throws IOException {
		logger = new PrintWriter(new BufferedWriter(new FileWriter(new File(Def.LOG_FILE), true)));
		String year = Integer.toString(now.get(Calendar.YEAR));
		String month = Integer.toString(now.get(Calendar.MONTH) + 1);

		// 月を2桁にする
		if (month.length() == 1) {
			month = "0" + month;
		}

		// 日を2桁にする
		String day = Integer.toString(now.get(Calendar.DATE));
		if (day.length() == 1) {
			day = "0" + day;
		}
		Logger.writeln("######  " + year + " / " + month + " / " + day + "  #####");
	}

	/**
	 * ログファイルに書き込みを行う. 書き込み時間が自動的に付加される.
	 * 
	 * @param msg
	 *            メッセージ.
	 * @author y-yuuki
	 * @author m-sano ログレベル実装に伴い, アクセス修飾子を<code>private</code>に変更.
	 */
	private static void writeln(String msg) {
		updateTime();
		logger.println(msg + " ( " + hour + ":" + minute + ":" + second + " )");
		logger.flush();
	}

	/**
	 * ログファイルに書き込みを行う. 書き込み時間が自動的に付加される.
	 * 
	 * @param msg
	 *            メッセージ.
	 * @param level
	 *            ログレベル.
	 * @author m-sano ログレベル実装に伴い, アクセス修飾子を<code>private</code>に変更.
	 */
	public static void writeln(String msg, int level) {
		if (level >= Logger.level) {
			String prefix = getLevelPrefix(level);
			Logger.writeln(prefix + msg);
		}
	}

	/**
	 * ログファイルへの書き込みを実施後, システムを終了する.
	 * 
	 * @param msg
	 *            終了メッセージ.
	 * @param level
	 *            ログレベル.
	 * @author y-yuuki
	 * @author m-sano ログレベルの指定を追加.
	 */
	public static void finish(String msg, int level) {
		Logger.writeln(msg, level);
		logger.close();
		System.exit(0); // TODO 異常終了の場合と返り値を区別する必要がある
	}

	/**
	 * 現在時刻を更新する.
	 */
	private static void updateTime() {
		now = Calendar.getInstance();
		hour = Integer.toString(now.get(Calendar.HOUR_OF_DAY));

		// 時間を2桁にする
		if (hour.length() == 1) {
			hour = "0" + hour;
		}

		minute = Integer.toString(now.get(Calendar.MINUTE));

		// 分を2桁にする
		if (minute.length() == 1) {
			minute = "0" + minute;
		}

		second = Integer.toString(now.get(Calendar.SECOND));

		// 秒を2桁にする
		if (second.length() == 1) {
			second = "0" + second;
		}
	}

	/**
	 * <p>
	 * コンソール (<code>System.err</code>) 上にメッセージを出力する. 指定レベルに応じた接頭辞を付与する.
	 * </p>
	 * 
	 * @param mes
	 *            メッセージ.
	 * @param loglevel
	 *            ログレベル.
	 * @author m-sano
	 */
	public static void printlnConsole(String mes, int loglevel) {
		if (loglevel >= Logger.level) {
			String prefix = getLevelPrefix(loglevel);
			System.err.println(prefix + mes);
		}
	}

	/**
	 * ログレベルに応じた接頭辞を取得する.
	 * 
	 * @param level
	 *            ログレベル.
	 * @return ログレベルを意味する接頭辞. 不明なレベルの場合は空文字列.
	 * @author m-sano
	 */
	private static String getLevelPrefix(int level) {
		String prefix = "";
		switch (level) {
		case INFO:
			prefix = "INFO: ";
			break;

		case WARNING:
			prefix = "WARNING: ";
			break;

		case ERROR:
			prefix = "ERROR: ";
			break;

		case FATAL:
			prefix = "FATAL: ";
			break;

		case SYSTEM:
		case DEBUG:
			prefix = "";
			break;

		default:
			printlnConsole("Specified unknown log level. [getLevelPrefix]", FATAL);
			break;
		}
		return prefix;
	}

	/* エラー時 */
	public static void writeError(IOException e) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		e.printStackTrace(printWriter);
		writeln(writer.toString(), Logger.ERROR);
		printWriter.close();
	}

	public static void writeError(Exception e) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		e.printStackTrace(printWriter);
		writeln(writer.toString(), Logger.ERROR);
		printWriter.close();
	}
}

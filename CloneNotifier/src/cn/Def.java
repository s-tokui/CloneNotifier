package cn;

/**
 * <p>
 * ファイルパスや実行ファイル名などのデータクラス
 * </p>
 *
 * @author y-yuuki
 *
 */

public class Def {
	/** analyze.jar へのパス */
	public static String NOTIFIER_PATH = null;

	/**
	 * CCFinderX
	 */
	public static final String CCFX_TOOLNAME = "CCFinderX";

	/**
	 * CCfinderXへのパス
	 */
	public static final String CCFX_PATH = "ccfx/bin/ccfx.exe";

	/**
	 * CloneDetector
	 */
	public static final String CD_TOOLNAME = "CloneDetector";

	/**
	 * CloneDetector (関数クローン検出) へのパス
	 */
	public static final String CLDT_DIR = "clonedetector";

	/**
	 * CloneDetector のファイル名
	 */
	public static final String CLDT_FILENAME = "clonedetector_ver1.0.jar";

	/**
	 * CCVolti
	 */
	public static final String CCV_TOOLNAME = "CCVolti";

	/**
	 * CCVolti へのパス
	 */
	public static final String CCV_DIR = "ccvolti";

	/**
	 * CCVolti (Windows ver.)のファイル名
	 */
	public static final String CCV_FILENAME = "CCVolti.jar";

	/**
	 * SourcererCC
	 */
	public static final String SCC_TOOLNAME = "SourcererCC";

	/**
	 * SourcererCC へのパス
	 */
	public static final String SCC_DIR = "sourcerercc";

	/**
	 * SourcererCC tokenizer のファイル名
	 */
	// public static final String SCC_TOKENIZER_NAME = "parser/java/tascc.jar";
	// public static final String SCC_TOKENIZER_NAME =
	// "parser/java/tascc_181115.jar";
	// public static final String SCC_TOKENIZER_NAME =
	// "parser/java/tascc_181122.jar";
	public static final String SCC_TOKENIZER_NAME = "parser/java/tascc_190225.jar";

	/**
	 * SourcererCC SearchManager のファイル名
	 */
	public static final String SCC_FILENAME = "dist/indexbased.SearchManager.jar";

	/**
	 * 新バージョンの検出結果の出力ファイル名 (.txt)
	 */
	public static final String RESULT_TXT = "result.txt";

	/**
	 * 新バージョンの検出結果の出力ファイル名 (.csv)
	 */
	public static final String RESULT_CSV = "result.csv";

	/**
	 * 旧バージョンの検出結果の出力ファイル名 (.txt)
	 */
	public static final String RESULT_TXT_OLD = "result-old.txt";

	/**
	 * 旧バージョンの検出結果の出力ファイル名 (.csv)
	 */
	public static final String RESULT_CSV_OLD = "result-old.csv";

	/** 新バージョンのクローンメトリクス出力ファイル名 (CCFinderX) */
	public static final String METRICS_TXT = "metrics.txt";

	/** 旧バージョンのクローンメトリクス出力ファイル名 (CCFinderX) */
	public static final String METRICS_TXT_OLD = "metrics-old.txt";

	/** テキスト類似度を計算するためのtempファイル */
	public static final String TEXTSIM_TXT = "textsim.txt";

	/** diff.exe (Windows ver.) へのパス */
	public static final String DIFF_PATH  = "lib/diff.exe";

	/** imageディレクトリへのパス */
	public static final String RESOURCES = "res";

	/** 出力ログファイル名 */
	public static final String LOG_FILE = "ccm.log";

	/**
	 * 標準作業用ディレクトリ名
	 */
	public static final String DEFAULT_WORK_DIR = "file";

	/**
	 * ？？？？？？？？？？？？？
	 */
	public static final String OLD_PROJ_DIR = "old";

	/**
	 * 指定ツールに対して言語名が有効か調べる.
	 * 
	 * @author m-sano
	 * @param lang
	 * @param tool
	 * @return 有効ならtrue
	 */
	public static boolean isValidLang(String lang, String tool) {
		if (tool.equals(Def.CD_TOOLNAME)) {
			if (lang.equals("c") || lang.equals("java")) {
				return true;
			}
		} else if (tool.equals(Def.CCFX_TOOLNAME)) {
			if (lang.equals("cpp") || lang.equals("java") || lang.equals("cobol") || lang.equals("csharp")
					|| lang.equals("visualbasic") || lang.equals("plaintext")) {
				return true;
			}
		} else if (tool.equals(Def.CCV_TOOLNAME)) {
			if (lang.equals("c") || lang.equals("java") || lang.equals("csharp")) {
				return true;
			}
		} else if (tool.equals(Def.SCC_TOOLNAME)) {
			if (lang.equals("cpp") || lang.equals("java") || lang.equals("csharp") || lang.equals("js")
					|| lang.equals("python")) {
				return true;
			}
		}
		Logger.writeln("Invalid programming language '" + lang + "' for " + tool, Logger.ERROR);
		return false;
	}
}
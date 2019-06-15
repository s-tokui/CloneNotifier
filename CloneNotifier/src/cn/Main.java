package cn;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import cn.analyze.AnalyzeManager;
import cn.data.Project;
import cn.generate.OutputGenerator;

/**
 * CloneNotifierメインクラス.
 *
 * @version 3.0.180115
 * @author y-yuuki
 * @author s-tokui
 */

public class Main {
	/**
	 * メインメソッド.
	 *
	 * @param args
	 *            <code><i>SettingFileName</i> [ <i>SettingFileName</i>... ]</code>
	 */
	public static void main(String[] args) {

		// ログファイル初期化
		try {
			Logger.init();
		} catch (IOException e) {
			Logger.printlnConsole("Can't generate log file.", Logger.ERROR);
			System.exit(1);
		}

		// CloneNotifierのパス設定
		Path path = null;
		try {
			path = Paths.get(Main.class.getClassLoader().getResource("").toURI());
		} catch (URISyntaxException e) {
			Logger.writeError(e);
			System.exit(1);
		}
		if (path.endsWith("bin"))
			path = path.getParent();
		Def.NOTIFIER_PATH = path.toString();

		// 引数には複数の設定ファイルを指定できる
		int argnum = 1;
		for (String arg : args) {
			Logger.writeln("--- Analyze Project" + Integer.toString(argnum) + "---", Logger.SYSTEM);
			Logger.printlnConsole("--- Analyze Project" + Integer.toString(argnum++) + "---", Logger.SYSTEM);

			Project project = new Project();

			// 設定ファイル読込み
			if (SettingFileLoader.loadSettingFile(arg, project)) {
				Logger.writeln("<Success> Load setting file.", Logger.SYSTEM);

				// 中間ファイルディレクトリ生成
				if (!(new File(project.getWorkDir())).exists()) {
					Logger.writeln("Create directory 'file'.", Logger.INFO);
					(new File(project.getWorkDir())).mkdirs();
				}

				boolean okFlg = true; // プロジェクトのチェックアウトに失敗したら false になる

				// 自動チェックアウトの実行
				if (project.isCheckout()) {
					if (!(okFlg = VCSController.checkoutProject(project))) {
						Logger.writeln("Can't checkout project.", Logger.ERROR);
						okFlg = false;
					} else {
						Logger.writeln("<Success> Checkout new version of " + project.getName() + ".", Logger.SYSTEM);
					}
				}

				// チェックアウトに成功した
				if (okFlg) {

					// コードクローン情報の取得
					if (AnalyzeManager.getCloneInf(project)) {
						Logger.writeln("<Success> Extract code clone information.", Logger.SYSTEM);
						Logger.printlnConsole("Analyze code clones.", Logger.SYSTEM);

						// コードクローンの分類，分析
						if (AnalyzeManager.analyzeClone(project)) {
							Logger.writeln("<Success> Categorize code clones.", Logger.SYSTEM);

							// 分類結果の出力
							OutputGenerator generator = new OutputGenerator(project);

							// TEXT
							if (project.isGenerateText()) {
								generator.generateTextFile();
							}

							// CSV
							if (project.isGenerateCSV()) {
								generator.generateCSVFile();
							}

							// HTML
							if (project.isGenerateHtml()) {
								generator.generateHTMLFile();
							}

							// JSON
							if (project.isGenerateJson()) {
								generator.generateJsonFile();
							}
						}
					} else {
						Logger.writeln("Can't extract code clone information.", Logger.ERROR);
					}
				}
			} else {
				Logger.writeln("Can't load setting file.", Logger.ERROR);
			}
			// delete temp files under "workdir/clone/"
			try {
				deleteTempfiles(new File(Paths.get(project.getWorkDir(), "clone").toString()));
			} catch (Exception e) {
				Logger.printlnConsole("Can't delete temp files under \"workdir/clone/\".", Logger.ERROR);
			}
		}
		Logger.finish("End.", Logger.SYSTEM);
	}

	private static void deleteTempfiles(File file) throws Exception {
		// 存在しない場合は処理終了
		if (!file.exists()) {
			return;
		}
		// 対象がディレクトリの場合は再帰処理
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				deleteTempfiles(child);
			}
		}
		// 対象がファイルもしくは配下が空のディレクトリの場合は削除する
		file.delete();
	}
}

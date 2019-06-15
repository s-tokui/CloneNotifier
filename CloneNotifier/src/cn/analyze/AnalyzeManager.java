package cn.analyze;

import cn.Def;
import cn.Logger;
import cn.data.Project;

/**
 * <p>ソースコード分析クラス</p>
 * @author y-yuuki
 */
public class AnalyzeManager {

	/**
	 * <p>外部ツールを利用して，クローン情報を取得する</p>
	 * @param project Projectオブジェクト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public static boolean getCloneInf(Project project) {

		// 各ツールの実行
		if (project.getTool().equals(Def.CCFX_TOOLNAME)) {
			CCFXController controller = new CCFXController(project);

			// CCFinderXの実行
			if (!controller.execute()) {
				Logger.writeln("Can't execute CCFinder.", Logger.ERROR);
				return false;
			}

			// 検出結果の読込み
			if (!controller.readCloneDataFile()) {
				Logger.writeln("Can't read clone data file.", Logger.ERROR);
				return false;
			}

			try {
				// 前処理ファイルの読込み
				if (!controller.readPrepFile()) {
					Logger.writeln("Can't read preprocess files.", Logger.ERROR);
					return false;
				}
			} catch (Exception e) {
				Logger.writeln("<Exception> Can't read preprocess files.", Logger.ERROR);
				return false;
			}

			// メトリクスの読込み
			if (!controller.getCloneMetrics()) {
				Logger.writeln("Can't read clone metrics file.", Logger.ERROR);
				return false;
			}
		} else if (project.getTool().equals(Def.CD_TOOLNAME)) {
			CloneDetectorController controller = new CloneDetectorController(project);

			// CloneDetector の実行
			if (!controller.execute()) {
				Logger.writeln("Can't execute clonedetector.", Logger.ERROR);
				return false;
			}

			// 検出結果の読込み
			if (!controller.readCloneDataFile()) {
				Logger.writeln("Can't read clone data file.", Logger.ERROR);
				return false;
			}

		} else if (project.getTool().equals(Def.CCV_TOOLNAME)) {
			CCVoltiController controller = new CCVoltiController(project);

			// CCVolti の実行
			if (!controller.execute()) {
				Logger.writeln("Can't execute CCVolti.", Logger.ERROR);
				return false;
			}

			// 検出結果の読込み
			if (!controller.readCloneDataFile()) {
				Logger.writeln("Can't read clone data file.", Logger.ERROR);
				return false;
			}

		} else if (project.getTool().equals(Def.SCC_TOOLNAME)) {
			SourcererccController controller = new SourcererccController(project);

			// SourcererCC の実行
			if (!controller.execute()) {
				Logger.writeln("Can't execute SourcererCC.", Logger.ERROR);
				return false;
			}

			// 検出結果の読込み
			if (!controller.readCloneDataFile()) {
				Logger.writeln("Can't read clone data file.", Logger.ERROR);
				return false;
			}

		} else {
			Logger.writeln("Can't execute unknown tool '" + project.getTool() + "'.", Logger.FATAL);
			return false;
		}
		return true;
	}

	/**
	 * <p>クローンセット/クローン変更情報を分類する</p>
	 * @param project プロジェクトオブジェクト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public static boolean analyzeClone(Project project) {

		// ファイルのdiffを取得
		if (!DiffDetector.getDiff(project.getFileList())) {
			Logger.writeln("Can't get diff of source code.", Logger.ERROR);
			return false;
		}

		// クローンの分類，コード位置の重複に基づいた親子クローン取得
		new CloneCategorizer().categorizeClone(project.getFileList());
		Logger.writeln("<Success> Categorized clone.", Logger.INFO);

		// テキスト類似度に基づいた子クローン取得
/*		if (CloneSetCategorizer.searchSimCloneSet(project)) {
			Logger.writeln("<Success> Searched similar cloneset.", Logger.INFO);
		} else {
			Logger.writeln("Can't search similar cloneset because CCFinderX can't work.", Logger.ERROR);
		}*/
		// 子クローン情報に基づいてクローンセット再構成
		if (project.getTool().equals(Def.CCV_TOOLNAME) || project.getTool().equals(Def.CCFX_TOOLNAME)
				|| project.getTool().equals(Def.SCC_TOOLNAME)) {
			project.setCloneSetList(
					CloneSetCategorizer.getCloneGenealogy(project.getCloneSetList(), project.getFileList()));
			Logger.writeln("<Success> Tracked cloneset.", Logger.INFO);
		}

		// クローンセットの分類
		CloneSetCategorizer.categorizeCloneSet(project.getCloneSetList(), project.getTool());
		Logger.writeln("<Success> Categorized cloneset.", Logger.INFO);

		// gumtreediffに基づいた危険度の計算
/*		if (project.getLang().equals("java") || project.getLang().equals("c") || project.getLang().equals("cpp")
				|| project.getLang().equals("csharp")) {
			new CalculateCloneSetRisk(project).calculateCloneRisk(project.getCloneSetList());
			Logger.writeln("<Success> Calculate cloneset risk.", Logger.INFO);
		} else {
			Logger.writeln("Can't calculate cloneset risk because of language.", Logger.ERROR);
		}
*/
		return true;
	}

}

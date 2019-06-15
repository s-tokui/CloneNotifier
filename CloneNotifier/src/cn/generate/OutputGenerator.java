package cn.generate;

import java.io.File;
import java.util.Calendar;

import cn.Def;
import cn.Logger;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;
import cn.data.SourceFile;

/**
 * <p>分類結果出力クラス</p>
 * @author y-yuuki</p>
 */
public class OutputGenerator {

	/** プロジェクト情報. ファイル総数 */
	private int fileNum = 0;

	/** プロジェクト情報. 追加ファイル数 */
	private int addedFileNum = 0;

	/** プロジェクト情報. 削除ファイル数 */
	private int deletedFileNum = 0;

	/** プロジェクト情報. クローンを含むファイル数 */
	private int cloneFileNum = 0;

	/** クローンセット情報. クローンセット総数 */
	private int cloneSetNum = 0;

	/** クローン情報. 現状維持クローン数 */
	private int stableCloneNum = 0;

	/** クローン情報. 追加クローン数 */
	private int addedCloneNum = 0;

	/** クローン情報. 除去クローン数 */
	private int deletedCloneNum = 0;

	/** クローン情報. 編集かつ除去クローン数 */
	private int modified_deletedCloneNum = 0;

	/** クローン情報. 修正クローン数 */
	private int modifiedCloneNum = 0;

	/** クローン情報. 移動クローン数 */
	private int movedCloneNum = 0;

	/** クローン情報. 総クローン数 */
	private int cloneNum = 0;

	/** クローンセット情報. 現状維持クローンセット数 */
	private int stableCloneSetNum = 0;

	/** クローンセット情報. 新規クローンセット数 */
	private int newCloneSetNum = 0;

	/** クローンセット情報. 除去クローンセット数 */
	private int deletedCloneSetNum = 0;

	/** クローンセット情報. 変更クローンセット数 */
	private int changedCloneSetNum = 0;

	/** 出力対象のプロジェクト */
	private Project project = null;

	/**
	 * <p>コンストラクタ</p>
	 * @param project Projectオブジェクト
	 * @author y-yuuki
	 * @author m-sano
	 * @author s-tokui
	 */
	public OutputGenerator(Project project) {

		// project の日付設定
		String year;
		String month;
		String day;
		Calendar now = Calendar.getInstance();
		year = Integer.toString(now.get(Calendar.YEAR));
		// 月は2桁にする
		month = Integer.toString(now.get(Calendar.MONTH) + 1);
		if (month.length() == 1) {
			month = "0" + month;
		}
		// 日も2桁にする
		day = Integer.toString(now.get(Calendar.DATE));
		if (day.length() == 1) {
			day = "0" + day;
		}
		project.setDate(year + month + day);

		// 危険度ランキングでソート
		project.sortCloneSetListbyRisk();

		// 分類別にソート
		// LEN の情報を持つのは CCFinderX のみ
		if (project.getTool().equals(Def.CCFX_TOOLNAME)) {
			project.sortCloneSetListbyLEN();
		}
		project.sortCloneSetListbyCategory();

		// 出力ID付加
		int cloneSetId = 0;
		for (CloneSet cloneSet : project.getCloneSetList()) {
			cloneSet.setOutputId(cloneSetId++);

			int cloneId = 0;
			for (Clone clone : cloneSet.getNewCloneList()) {
				clone.setOutputId(cloneId++);
			}

			for (Clone clone : cloneSet.getOldCloneList()) {
				if (clone.getCategory() == Clone.DELETED || clone.getCategory() == Clone.DELETE_MODIFIED) {
					clone.setOutputId(cloneId++);
				}
			}
		}

		// ファイル数の計算
		for (SourceFile file : project.getFileList()) {
			switch (file.getState()) {
			case SourceFile.ADDED:
				addedFileNum++;
				fileNum++;
				break;

			case SourceFile.DELETED:
				deletedFileNum++;
				break;

			case SourceFile.NORMAL:
				fileNum++;
			}

			if (!file.getNewCloneList().isEmpty()) {
				cloneFileNum++;
			}
		}

		// クローン/クローンセット数の計算
		for (CloneSet cloneSet : project.getCloneSetList()) {
			switch (cloneSet.getCategory()) {

			case CloneSet.NULL:
				stableCloneSetNum++;
				cloneSetNum++;
				break;

			case CloneSet.CHANGED:
				changedCloneSetNum++;
				cloneSetNum++;
				break;

			case CloneSet.STABLE:
				stableCloneSetNum++;
				cloneSetNum++;
				break;

			case CloneSet.NEW:
				newCloneSetNum++;
				cloneSetNum++;
				break;

			case CloneSet.DELETED:
				deletedCloneSetNum++;
				break;
			}

			for (Clone clone : cloneSet.getNewCloneList()) {
				cloneNum++;

				switch (clone.getCategory()) {
				case Clone.STABLE:
					stableCloneNum++;
					break;

				case Clone.MODIFIED:
					modifiedCloneNum++;
					break;

				case Clone.MOVED:
					movedCloneNum++;
					break;

				case Clone.ADDED:
					addedCloneNum++;
					break;
				}
			}

			for (Clone clone : cloneSet.getOldCloneList()) {
				switch (clone.getCategory()) {

				case Clone.DELETED:
					deletedCloneNum++;
					break;

				case Clone.DELETE_MODIFIED:
					modified_deletedCloneNum++;
					break;
				}
			}
		}

		this.project = project;
	}

	/**
	 * <p>テキストファイル生成</p>
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateTextFile() {

		// テキストファイル名を設定
		project.setGenerateTextFileName(project.getName() + project.getDate() + ".txt");

		// 出力先ディレクトリの生成
		File file = new File(project.getGenerateTextDir());
		if (!file.exists()) {
			file.mkdirs();
		}

		TextFileGenerator generater_text = new TextFileGenerator(this, project);

		if (!generater_text.generateTextFile()) {
			Logger.writeln("Can't generate text file.", Logger.ERROR);
			return false;
		}
		if (!generater_text.generateMailText()) {
			Logger.writeln("Can't generate mail text file.", Logger.ERROR);
			return false;
		}
		Logger.writeln("<Success> Generate text file.", Logger.SYSTEM);

		return true;
	}

	/**
	 * <p>CSVファイル生成</p>
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateCSVFile() {
		project.setGenerateCSVFileName(project.getName() + project.getDate() + ".csv");

		File file = new File(project.getGenerateCSVDir());
		if (!file.exists()) {
			file.mkdirs();
		}

		CSVGenerator generater_csv = new CSVGenerator(project);

		if (!generater_csv.generateCSVFile()) {
			Logger.writeln("Can't generate CSV file.", Logger.ERROR);
			return false;
		}
		Logger.writeln("<Success> Generate CSV file.", Logger.SYSTEM);
		return true;
	}

	/**
	 * <p>HTMLファイル生成</p>
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateHTMLFile() {
		File file = new File(project.getGenerateHTMLDir());
		if (!file.exists()) {
			file.mkdirs();
		}

		HTMLFileGenerater generater_html = new HTMLFileGenerater(this, project);

		if (!generater_html.generateHTMLFile()) {
			Logger.writeln("Can't generate HTML files.", Logger.ERROR);
			return false;
		}
		Logger.writeln("<Success> Generate HTML files.", Logger.SYSTEM);
		return true;
	}

	/**
	 * <p>JSONファイル生成</p>
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateJsonFile() {
		File file = new File(project.getWorkDir());
		if (!file.exists()) {
			file.mkdirs();
		}

		JSONFileGenerater generater_json = new JSONFileGenerater(this, project);

		if (!generater_json.generateJSONFile()) {
			Logger.writeln("Can't generate JSON files.", Logger.ERROR);
			return false;
		}
		Logger.writeln("<Success> Generate JSON files.", Logger.SYSTEM);
		return true;

	}

	/**
	 * <p>総ファイル数の取得<p>
	 * @return
	 */
	public int getFileNum() {
		return fileNum;
	}

	/**
	 * <p>追加ファイル数の取得<p>
	 * @return
	 */
	public int getAddedFileNum() {
		return addedFileNum;
	}

	/**
	 * <p>削除ファイル数の取得<p>
	 * @return
	 */
	public int getDeletedFileNum() {
		return deletedFileNum;
	}

	/**
	 * <p>クローンファイル数の取得</p>
	 * @return
	 */
	public int getCloneFileNum() {
		return cloneFileNum;
	}

	/**
	 * <p>Stable Clone 数の取得</p>
	 * @return
	 */
	public int getStableCloneNum() {
		return stableCloneNum;
	}

	/**
	 * <p>Added Clone 数の取得</p>
	 * @return
	 */
	public int getAddedCloneNum() {
		return addedCloneNum;
	}

	/**
	 * <p>Deleted Clone 数の取得</p>
	 * @return
	 */
	public int getDeletedCloneNum() {
		return deletedCloneNum;
	}

	/**
	 * <p>Modified Clone 数の取得</p>
	 * @return
	 */
	public int getModifiedCloneNum() {
		return modifiedCloneNum;
	}

	/**
	 * <p>Moved Clone 数の取得</p>
	 * @return
	 */
	public int getMovedCloneNum() {
		return movedCloneNum;
	}

	/**
	 * <p>Moved Clone 数の取得</p>
	 * @return
	 */
	public int getDeleteModifiedCloneNum() {
		return modified_deletedCloneNum;
	}

	/**
	 * <p>総クローン数の取得</p>
	 * @return
	 */
	public int getCloneNum() {
		return cloneNum;
	}

	/**
	 * <p>Stable CloneSet 数の取得</p>
	 * @return
	 */
	public int getStableCloneSetNum() {
		return stableCloneSetNum;
	}

	/**
	 * <p>New CloneSet 数の取得</p>
	 * @return
	 */
	public int getNewCloneSetNum() {
		return newCloneSetNum;
	}

	/**
	 * <p>Deleted CloneSet 数の取得</p>
	 * @return
	 */
	public int getDeletedCloneSetNum() {
		return deletedCloneSetNum;
	}

	/**
	 * <p>Changed CloneSet 数の取得</p>
	 * @return
	 */
	public int getChangedCloneSetNum() {
		return changedCloneSetNum;
	}

	/**
	 * <p>総クローンセット数の取得</p>
	 * @return
	 */
	public int getCloneSetNum() {
		return cloneSetNum;
	}

}

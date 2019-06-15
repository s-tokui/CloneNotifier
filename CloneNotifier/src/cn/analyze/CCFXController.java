package cn.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import cn.Def;
import cn.Logger;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;
import cn.data.SourceFile;

/**
 * <p>
 * CCFinderXのコントローラクラス
 * </p>
 *
 * @author y-yuuki
 */
public class CCFXController {

	/** 検出対象プロジェクト */
	private Project project = null;
	private String workDir;
	private String CCFX_PATH;

	/**
	 * <p>
	 * コンストラクタ
	 * </p>
	 *
	 * @param project
	 *            Project オブジェクト
	 */
	public CCFXController(Project project) {
		this.project = project;
		this.workDir = project.getWorkDir();
		this.CCFX_PATH = Paths.get(Def.NOTIFIER_PATH, Def.CCFX_PATH).toString();
	}

	/**
	 * <p>
	 * CCFinderXを実行
	 * </p>
	 *
	 * @param String
	 *            oldFilePath
	 * @param String
	 *            newFilePath
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean executeForTextSimilarity(String oldFilePath, String newFilePath) {

		// CCFinderX実行用コマンド
		String[] cmdArray1 = { CCFX_PATH, "d", "plaintext", "-b", Integer.toString(30), "-dn", oldFilePath, "-is",
				"-dn", newFilePath };

		// CCFinderXテキスト表示用コマンド
		String[] cmdArray2 = { CCFX_PATH, "p", "a.ccfxd", "-o", Def.TEXTSIM_TXT };

		try {
			new ProcessBuilder(cmdArray1).directory(new File(workDir)).start().waitFor();
			new ProcessBuilder(cmdArray2).directory(new File(workDir)).start().waitFor();
		} catch (InterruptedException e) {
			Logger.writeError(e);
			return false;
		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		}

		if (!(new File(workDir, "a.ccfxd").exists())) {
			return false;
		}

		return true;
	}

	/**
	 * <p>
	 * CCFinderXを実行
	 * </p>
	 *
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean execute() {

		// CCFinderX実行用コマンド New Path
		String[] cmdArrayNewExecute = { CCFX_PATH, "d", project.getLang(), "-b", Integer.toString(project.getTokenTh()),
				"-dn", project.getNewDir(), "-o", Paths.get(workDir, "new.ccfxd").toString() };

		// CCFinderXテキスト表示用コマンド New Path
		String[] cmdArrayNewResult = { CCFX_PATH, "p", "new.ccfxd", "-o", Def.RESULT_TXT };

		// CCFinderXメトリクス計測用コマンド New Path
		String[] cmdArrayNewMetrics = { CCFX_PATH, "m", "new.ccfxd", "-c", "-o", Def.METRICS_TXT };

		// 新バージョンのクローン出力
		try {
			Logger.writeln("Extract new code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract new code clone.", Logger.SYSTEM);
			new ProcessBuilder(cmdArrayNewExecute).directory(new File(workDir)).start().waitFor();
			new ProcessBuilder(cmdArrayNewResult).directory(new File(workDir)).start().waitFor();
			new ProcessBuilder(cmdArrayNewMetrics).directory(new File(workDir)).start().waitFor();
		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		} catch (InterruptedException e) {
			Logger.writeError(e);
			return false;
		}

		if (!(new File(workDir, "new.ccfxd").exists())) {
			return false;
		}

		// CCFinderX実行用コマンド Old Path
		String[] cmdArrayOldExecute = { CCFX_PATH, "d", project.getLang(), "-b", Integer.toString(project.getTokenTh()),
				"-dn", project.getOldDir(), "-o", Paths.get(workDir, "old.ccfxd").toString() };

		// CCFinderXテキスト表示用コマンド Old Path
		String[] cmdArrayOldResult = { CCFX_PATH, "p", "old.ccfxd", "-o", Def.RESULT_TXT_OLD };

		// CCFinderXメトリクス計測用コマンド Old Path
		String[] cmdArrayOldMetrics = { CCFX_PATH, "m", "old.ccfxd", "-c", "-o", Def.METRICS_TXT_OLD };

		// 旧バージョンのクローン出力
		try {
			Logger.writeln("Extract old code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract old code clone.", Logger.SYSTEM);
			new ProcessBuilder(cmdArrayOldExecute).directory(new File(workDir)).start().waitFor();
			new ProcessBuilder(cmdArrayOldResult).directory(new File(workDir)).start().waitFor();
			new ProcessBuilder(cmdArrayOldMetrics).directory(new File(workDir)).start().waitFor();
		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		} catch (InterruptedException e) {
			Logger.writeError(e);
			return false;
		}

		if (!(new File(workDir, "old.ccfxd").exists())) {
			return false;
		}

		return true;
	}

	/**
	 * <p>
	 * CCFinderXによって出力されたクローンデータファイルの読込み
	 * </p>
	 *
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean readCloneDataFile() {
		BufferedReader readFile = null;
		ArrayList<SourceFile> fileList = new ArrayList<SourceFile>();
		ArrayList<CloneSet> cloneSetList = new ArrayList<CloneSet>();
		ArrayList<String> newFileList = new ArrayList<String>();
		ArrayList<String> oldFileList = new ArrayList<String>();

		Logger.writeln("Read clone data file.", Logger.SYSTEM);
		Path resultTxt = Paths.get(workDir, Def.RESULT_TXT);
		Path resultTxtOld = Paths.get(workDir, Def.RESULT_TXT_OLD);

		try {

			// ソースファイルの取得
			Logger.writeln("Get fileList.", Logger.INFO);
			newFileList = analyzeResultFileFileList(resultTxt.toString(), true);
			oldFileList = analyzeResultFileFileList(resultTxtOld.toString(), false);

			Iterator<String> it = newFileList.iterator();
			int fileId = 0;
			while (it.hasNext()) {
				String fileName = it.next();
				SourceFile file = new SourceFile();
				file.setName(fileName);
				file.setNewPath(project.getNewDir() + "\\" + fileName);
				file.setOldPath(project.getOldDir() + "\\" + fileName);
				file.setId(fileId++);

				// 旧ファイルリストに含まれないファイルは新規追加分
				int index = oldFileList.indexOf(fileName);
				if (index > -1) {
					file.setState(SourceFile.NORMAL);
					oldFileList.remove(fileName);
				} else {
					file.setState(SourceFile.ADDED);
				}
				fileList.add(file);
			}

			Logger.writeln("\t Analyze delete files.", Logger.DEBUG);

			// 残った旧ファイルは変更後に消えたもの
			it = oldFileList.iterator();
			while (it.hasNext()) {
				String fileName = it.next();
				SourceFile file = new SourceFile();
				file.setName(fileName);
				file.setOldPath(project.getOldDir() + "\\" + fileName);
				file.setId(fileId++);
				file.setState(SourceFile.DELETED);
				fileList.add(file);
			}
			Logger.writeln("<Success> Get source file status.", Logger.INFO);

			Logger.writeln("Analyze new clone result file.", Logger.INFO);
			analyzeResultFileCloneSetList(resultTxt.toString(), cloneSetList, fileList, true);
			Logger.writeln("Analyze old clone result file.", Logger.INFO);
			analyzeResultFileCloneSetList(resultTxtOld.toString(), cloneSetList, fileList, false);

		} catch (FileNotFoundException e) {
			Logger.writeln("Can't found read clone file.", Logger.ERROR);
			return false;
		} catch (IOException e) {
			Logger.writeln("<IOException> Can't read clone file.", Logger.ERROR);
			return false;
		} finally {
			if (readFile != null) {
				try {
					readFile.close();
				} catch (IOException e) {
					Logger.writeln("<IOException> Can't close buffer.", Logger.ERROR);
				}
			}
		}

		Logger.writeln("Check error detection.", Logger.INFO);

		// 誤検出の除去
		@SuppressWarnings("unchecked")
		ArrayList<CloneSet> tmpCloneSetList = (ArrayList<CloneSet>) cloneSetList.clone();
		for (CloneSet cloneSet : tmpCloneSetList) {

			// クローン数が 1
			if (cloneSet.getNewCloneList().size() == 1) {
				cloneSet.getNewCloneList().remove(0);
			}

			if (cloneSet.getOldCloneList().size() == 1) {
				cloneSet.getOldCloneList().remove(0);
			}

			// 新旧クローンが共に無し
			if (cloneSet.getNewCloneList().isEmpty() && cloneSet.getOldCloneList().isEmpty()) {
				cloneSetList.remove(cloneSet);
			}

			// オーバーラップの除去
			if (project.isOlFilter()
					&& (isOverlapping(cloneSet.getNewCloneList()) || isOverlapping(cloneSet.getOldCloneList()))) {
				cloneSetList.remove(cloneSet);
			}
		}

		// ソースファイルにクローンの追加
		for (CloneSet cloneSet : cloneSetList) {
			for (Clone clone : cloneSet.getNewCloneList()) {
				clone.getFile().getNewCloneList().add(clone);
			}
			for (Clone clone : cloneSet.getOldCloneList()) {
				clone.getFile().getOldCloneList().add(clone);
			}
		}

		// クローンリストの整列
		for (SourceFile file : fileList) {
			file.sortCloneListbyToken();
		}

		project.getCloneSetList().addAll(cloneSetList);
		project.getFileList().addAll(fileList);

		return true;
	}

	/**
	 * 出力ファイルの解析(fileList)
	 *
	 * @param result
	 *            出力ファイルのパス
	 * @param cloneSetList
	 *            解析結果を蓄えるリスト
	 * @param isNew
	 *            新プロジェクトに関してなら ture
	 * @return FileName Stringのファイル名
	 */
	private ArrayList<String> analyzeResultFileFileList(String result, boolean isNew) throws IOException {

		BufferedReader readFile = new BufferedReader(new FileReader(result));
		ArrayList<String> FileNameList = new ArrayList<String>();
		String line;
		String fileName = null;

		while (!(line = readFile.readLine()).equals("source_files {"))
			;

		// ファイルリスト取得
		while (!(line = readFile.readLine()).equals("}")) {
			String[] str = line.split("\t");
			if (isNew) {
				fileName = str[1].substring(project.getNewDir().length() + 1);
			} else {
				fileName = str[1].substring(project.getOldDir().length() + 1);
			}
			FileNameList.add(fileName);
		}
		readFile.close();

		return FileNameList;
	}

	/**
	 * 出力ファイルの解析(CloneSet)
	 *
	 * @param result
	 *            出力ファイルのパス
	 * @param fileList
	 *            クローンを含む SourceFile の探索用リスト
	 * @param isNew
	 *            新プロジェクトに関してなら ture
	 */
	private void analyzeResultFileCloneSetList(String result, ArrayList<CloneSet> cloneSetList,
			ArrayList<SourceFile> fileList, boolean isNew) throws IOException {
		BufferedReader readFile = new BufferedReader(new FileReader(result));

		ArrayList<String> FileNameList = new ArrayList<String>();
		String line;
		String fileName = null;

		// ファイルリスト取得
		while (!(line = readFile.readLine()).equals("source_files {"))
			;
		while (!(line = readFile.readLine()).equals("}")) {
			String[] str = line.split("\t");
			if (isNew) {
				fileName = str[1].substring(project.getNewDir().length() + 1);
			} else {
				fileName = str[1].substring(project.getOldDir().length() + 1);
			}
			FileNameList.add(fileName);
		}

		int cloneId = 0;
		CloneSet cloneSet = null;
		Clone clone = null;
		final Pattern pattern = Pattern.compile("[\t|.|-]");

		// クローンセット情報取得
		while (!(line = readFile.readLine()).equals("clone_pairs {"))
			;
		while (!(line = readFile.readLine()).equals("}")) {
			String[] str = pattern.split(line);
			cloneSet = null;

			// CloneSetオブジェクトの用意
			if (isNew) {
				for (CloneSet pCloneSet : cloneSetList) {
					if (pCloneSet.getId() == (Integer.valueOf(str[0]))) {
						cloneSet = pCloneSet;
					}
				}
			} else {
				for (CloneSet pCloneSet : cloneSetList) {
					if (pCloneSet.getOldId() == (Integer.valueOf(str[0]))) {
						cloneSet = pCloneSet;
					}
				}
			}

			// リスト中に無ければ新しく作成
			if (cloneSet == null) {
				cloneSet = new CloneSet();
				if (isNew) {
					cloneSet.setId(Integer.valueOf(str[0]));
				} else {
					cloneSet.setOldId(Integer.valueOf(str[0]));
				}
				cloneSetList.add(cloneSet);
			}

			// クローン1
			clone = new Clone();
			clone.setCloneSet(cloneSet);
			clone.setStartToken(Integer.valueOf(str[2]));
			clone.setEndToken(Integer.valueOf(str[3]));
			clone.setId(cloneId++);
			clone.setFile(Project.getFileObj(fileList, FileNameList.get(Integer.valueOf(str[1]) - 1)));
			// 既にクローンがリスト中に存在する場合は中止し, IDを巻き戻す
			if (isNew) {
				if (!addClone(cloneSet.getNewCloneList(), clone)) {
					cloneId--;
				}
			} else {
				if (!addClone(cloneSet.getOldCloneList(), clone)) {
					cloneId--;
				}
			}

			// クローン2
			clone = new Clone();
			clone.setCloneSet(cloneSet);
			clone.setStartToken(Integer.valueOf(str[5]));
			clone.setEndToken(Integer.valueOf(str[6]));
			clone.setId(cloneId++);
			clone.setFile(Project.getFileObj(fileList, FileNameList.get(Integer.valueOf(str[4]) - 1)));
			// 既にクローンがリスト中に存在する場合は中止し, IDを巻き戻す
			if (isNew) {
				if (!addClone(cloneSet.getNewCloneList(), clone)) {
					cloneId--;
				}
			} else {
				if (!addClone(cloneSet.getOldCloneList(), clone)) {
					cloneId--;
				}
			}
		}
		readFile.close();
	}

	/**
	 * <p>
	 * クローンリストにクローンを追加
	 * </p>
	 *
	 * @param cloneList
	 *            クローンリスト
	 * @param clone
	 *            追加クローン
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean addClone(ArrayList<Clone> cloneList, Clone clone) {
		for (Clone pClone : cloneList) {
			if (clone.equalsForCCFX(pClone)) {
				return false;
			}
		}
		cloneList.add(clone);
		return true;
	}

	/**
	 * <p>
	 * オーバラッピングの判定
	 * </p>
	 *
	 * @param cloneList
	 *            クローンリスト
	 * @return
	 *         <ul>
	 *         <li>オーバラップしている場合 - true</li>
	 *         <li>オーバラップしていない場合 - false</li>
	 *         </ul>
	 */
	private boolean isOverlapping(ArrayList<Clone> cloneList) {
		Clone clone1, clone2;
		for (int i = 0; cloneList.size() > i; i++) {
			for (int j = i + 1; cloneList.size() > j; j++) {

				// 親ファイルが同じ
				if ((clone1 = cloneList.get(i)).getFile().getId() == (clone2 = cloneList.get(j)).getFile().getId()) {

					// オーバーラップ判定
					if ((clone1.getStartToken() < clone2.getStartToken()
							&& clone1.getEndToken() > clone2.getStartToken())
							|| (clone2.getStartToken() < clone1.getStartToken()
									&& clone2.getEndToken() > clone1.getStartToken())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * <p>
	 * CCFinderXのプリプロセスファイルの読込み
	 * </p>
	 *
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean readPrepFile() {
		BufferedReader readFile = null;

		for (SourceFile file : project.getFileList()) {

			// 新バージョンにおけるクローンの処理
			if (file.getState() != SourceFile.DELETED) {

				// Prepファイル名の取得
				String prepFile = null;
				String prepDir = Paths.get(project.getNewDir(), ".ccfxprepdir").toString();

				// 前処理ディレクトリが存在しない
				if (!(new File(prepDir)).exists()) {
					System.err.println(prepDir + "is not exit.");
					return false;
				}

				// 当該の前処理ファイルの絶対パスを取得
				ArrayList<File> tmpList = new ArrayList<File>();
				tmpList.addAll(getPrepList(new File(prepDir)));
				File[] prepList = (File[]) tmpList.toArray(new File[tmpList.size()]);

				for (int i = 0; i < prepList.length; i++) {
					String prepPath = prepList[i].getPath().substring((project.getNewDir() + ".ccfxprepdir").length() + 2);
					if (prepPath.startsWith(file.getNewPath().substring(project.getNewDir().length() + 1))) {
						prepFile = prepList[i].getAbsolutePath();
						break;
					}
				}

				// 前処理ファイルが存在しない
				if (prepFile == null)
					continue;

				// 行番号・列番号の読込
				ArrayList<Integer> lines = new ArrayList<Integer>();
				ArrayList<Integer> columns = new ArrayList<Integer>();

				try {
					readFile = new BufferedReader(new InputStreamReader(new FileInputStream(prepFile)));
					String line;

					// 各トークンの行・列番号を読み込み
					while ((line = readFile.readLine()) != null) {
						String[] str = Pattern.compile("[\t|.]").split(line);
						lines.add(Integer.decode("0x" + str[0]).intValue());
						columns.add(Integer.decode("0x" + str[1]).intValue());
					}

					// 各クローンの開始行・列, 終了行・列を更新
					for (Clone clone : file.getNewCloneList()) {
						clone.setStartLine(lines.get(clone.getStartToken()));
						clone.setStartColumn(columns.get(clone.getStartToken()));
						clone.setEndLine(lines.get(clone.getEndToken()));
						clone.setEndColumn(columns.get(clone.getEndToken()));
					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
					Logger.writeError(e);
					return false;
				} catch (IOException e) {
					e.printStackTrace();
					Logger.writeError(e);
					return false;
				} finally {
					if (readFile != null) {
						try {
							readFile.close();
						} catch (IOException e) {
							e.printStackTrace();
							Logger.writeError(e);
						}
					}
				}
			}

			// 旧バージョンにおけるクローンの処理
			if (file.getState() != SourceFile.ADDED) {

				// Prepファイル名の取得
				String prepFile = null;
				String prepDir = Paths.get(project.getOldDir(), ".ccfxprepdir").toString();

				// 前処理ディレクトリが存在しない
				if (!(new File(prepDir)).exists()) {
					System.err.println(prepDir + "is not exit.");
					return false;
				}

				// 当該の前処理ファイルの絶対パスを取得
				ArrayList<File> tmpList = new ArrayList<File>();
				tmpList.addAll(getPrepList(new File(prepDir)));
				File[] prepList = (File[]) tmpList.toArray(new File[tmpList.size()]);

				for (int i = 0; i < prepList.length; i++) {
					String prepPath = prepList[i].getPath().substring((project.getOldDir() + ".ccfxprepdir").length() + 2);
					if (prepPath.startsWith(file.getOldPath().substring(project.getOldDir().length() + 1))) {
						prepFile = prepList[i].getAbsolutePath();
						break;
					}
				}

				// 前処理ファイルが存在しない
				if (prepFile == null)
					continue;

				// 行番号・列番号の読込
				ArrayList<Integer> lines = new ArrayList<Integer>();
				ArrayList<Integer> columns = new ArrayList<Integer>();

				try {
					readFile = new BufferedReader(new InputStreamReader(new FileInputStream(prepFile)));
					String line;

					// 各トークンの行・列番号を読み込み
					while ((line = readFile.readLine()) != null) {
						String[] str = Pattern.compile("[\t|.]").split(line);
						lines.add(Integer.decode("0x" + str[0]).intValue());
						columns.add(Integer.decode("0x" + str[1]).intValue());
					}

					// 各クローンの開始行・列, 終了行・列を更新
					for (Clone clone : file.getOldCloneList()) {
						clone.setStartLine(lines.get(clone.getStartToken()));
						clone.setStartColumn(columns.get(clone.getStartToken()));
						clone.setEndLine(lines.get(clone.getEndToken()));
						clone.setEndColumn(columns.get(clone.getEndToken()));
					}

				} catch (FileNotFoundException e) {
					Logger.writeError(e);
					return false;
				} catch (IOException e) {
					Logger.writeError(e);
					return false;
				} finally {
					if (readFile != null) {
						try {
							readFile.close();
						} catch (IOException e) {
							Logger.writeError(e);
						}
					}
				}
			}
		}
		return true;
	}

	private ArrayList<File> getPrepList(File file) {
		ArrayList<File> prepList = new ArrayList<File>();
		File[] tmpList = file.listFiles();
		for (int i = 0; i < tmpList.length; i++) {
			File childFile = tmpList[i];
			if (childFile.isFile()) {
				prepList.add(childFile);
			}
			if (childFile.isDirectory()) {
				if (childFile.listFiles().length > 0) {
					prepList.addAll(getPrepList(childFile));
				}
			}
		}
		return prepList;
	}

	/**
	 * <p>
	 * CCFinderXのメトリクス抽出
	 * </p>
	 *
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean getCloneMetrics() {
		BufferedReader readFile = null;
		try {

			// 新バージョンのクローンセットのメトリクス読み込み
			readFile = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(workDir, Def.METRICS_TXT))));
			String line = null;

			// ヘッダ読み飛ばし
			readFile.readLine();
			// 各クローンセットのメトリクスを設定
			while ((line = readFile.readLine()) != null) {
				String[] str = line.split("\t");
				for (CloneSet cloneSet : project.getCloneSetList()) {
					if (str.length == 10 && cloneSet.getId() == Integer.valueOf(str[0])) {
						cloneSet.setLEN(Integer.valueOf(str[1]));
						cloneSet.setPOP(Integer.valueOf(str[2]));
						cloneSet.setNIF(Integer.valueOf(str[3]));
						cloneSet.setRAD(Integer.valueOf(str[4]));
						cloneSet.setRNR(Double.valueOf(str[5]));
						cloneSet.setTKS(Integer.valueOf(str[6]));
						cloneSet.setLOOP(Integer.valueOf(str[7]));
						cloneSet.setCOND(Integer.valueOf(str[8]));
						cloneSet.setMcCabe(Integer.valueOf(str[9]));
					}
				}
			}

			// 旧バージョンのクローンセットのメトリクス読み込み
			readFile = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(workDir, Def.METRICS_TXT_OLD))));
			line = null;

			// ヘッダ読み飛ばし
			readFile.readLine();
			// 各クローンセットのメトリクスを設定
			while ((line = readFile.readLine()) != null) {
				String[] str = line.split("\t");
				for (CloneSet cloneSet : project.getCloneSetList()) {
					if (str.length == 10 && cloneSet.getOldId() == Integer.valueOf(str[0])) {
						cloneSet.setLEN(Integer.valueOf(str[1]));
						cloneSet.setPOP(Integer.valueOf(str[2]));
						cloneSet.setNIF(Integer.valueOf(str[3]));
						cloneSet.setRAD(Integer.valueOf(str[4]));
						cloneSet.setRNR(Double.valueOf(str[5]));
						cloneSet.setTKS(Integer.valueOf(str[6]));
						cloneSet.setLOOP(Integer.valueOf(str[7]));
						cloneSet.setCOND(Integer.valueOf(str[8]));
						cloneSet.setMcCabe(Integer.valueOf(str[9]));
					}
				}
			}
		} catch (FileNotFoundException e) {
			Logger.writeError(e);
			return false;
		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		} finally {
			if (readFile != null) {
				try {
					readFile.close();
				} catch (IOException e) {
					Logger.writeError(e);
				}
			}
		}
		return true;
	}

}

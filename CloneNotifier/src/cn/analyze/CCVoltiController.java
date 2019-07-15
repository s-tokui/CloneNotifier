package cn.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import cn.Def;
import cn.Logger;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;
import cn.data.SourceFile;

/**
 * <p>
 * CCVolti のコントローラクラス
 * </p>
 * 
 * @author s-tokui
 */
public class CCVoltiController {

	/** 検出対象のプロジェクト */
	private Project project = null;
	private String workDir = null;

	/**
		 * <p>コンストラクタ</p>
		 * @param project Project オブジェクト
		 */
	public CCVoltiController(Project project) {
			this.project = project;
			this.workDir = project.getWorkDir();
		}

	/**
	 * <p>
	 * CCVolti を実行
	 * </p>
	 * 
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean execute() {

		// 実行ファイルのあるディレクトリを
		// カレントディレクトリにしないと正常に動作しない
		String ccvTool = Paths.get(Def.NOTIFIER_PATH, Def.CCV_DIR, Def.CCV_FILENAME).toString();
		// Block Clone Detector 新バージョン実行用コマンド
		String[] cmdArray1 = { "java", "-jar", ccvTool, "-d", project.getNewDir(), "-l", project.getLang(), "-on",
				Paths.get(workDir, Def.RESULT_TXT).toString(), "--sim", String.valueOf(0.9), "--size",
				String.valueOf(project.getTokenTh()), "--sizeb", String.valueOf(project.getTokenTh()) };

		// Block Clone Detector 旧バージョン実行用コマンド
		String[] cmdArray2 = { "java", "-jar", ccvTool, "-d", project.getOldDir(), "-l", project.getLang(), "-on",
				Paths.get(workDir, Def.RESULT_TXT_OLD).toString(), "--sim", String.valueOf(0.9), "--size",
				String.valueOf(project.getTokenTh()), "--sizeb", String.valueOf(project.getTokenTh()) };
		ProcessBuilder pb1 = new ProcessBuilder(cmdArray1);
		pb1.directory(new File(workDir));
		ProcessBuilder pb2 = new ProcessBuilder(cmdArray2);
		pb2.directory(new File(workDir));

		try {
			// 新バージョン実行
			pb1.redirectErrorStream(true);
			Process pr1 = pb1.start();
			Logger.writeln("Extract new code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract new code clone.", Logger.SYSTEM);
			BufferedReader reader = new BufferedReader(new InputStreamReader(pr1.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				Logger.writeln(line, Logger.SYSTEM);
			}
			reader.close();

			// 旧バージョン実行
			pb2.redirectErrorStream(true);
			Process pr2 = pb2.start();
			Logger.writeln("Extract old code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract old code clone.", Logger.SYSTEM);
			reader = new BufferedReader(new InputStreamReader(pr2.getInputStream()));
			while ((line = reader.readLine()) != null) {
				Logger.writeln(line, Logger.SYSTEM);
			}
			reader.close();

		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		}

		// 実行失敗の判定
		File nf = new File(workDir, Def.RESULT_TXT);
		File of = new File(workDir, Def.RESULT_TXT_OLD);

		// ファイルが存在しない
		if (!nf.exists() || !of.exists()) {
			Logger.writeln("Can't find result file.", Logger.ERROR);
			return false;
		}

		// ファイルが空
		if (nf.length() == 0 || of.length() == 0) {
			Logger.writeln("Result file is empty.", Logger.ERROR);
			return false;
		}
		return true;
	}

	/**
	 * <p>
	 * CCVoltiによって出力されたクローンデータファイルの読込み
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
			Logger.writeln("Get fileList.", Logger.INFO);
			newFileList = analyzeResultFileFileList(resultTxt.toString(), true);
			oldFileList = analyzeResultFileFileList(resultTxtOld.toString(), false);

			// ソースファイルの取得
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
			file.sortCloneListbyMethod();
		}

		project.getCloneSetList().addAll(cloneSetList);
		project.getFileList().addAll(fileList);

		Logger.writeln("<Success> Read clone data file.", Logger.INFO);

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
			if (isNew) {
				fileName = line.substring(project.getNewDir().length() + 1);
			} else {
				fileName = line.substring(project.getOldDir().length() + 1);
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
		int cloneId = 0;
		CloneSet cloneSet = null;
		Clone clone = null;
		String line;

		while (!(line = readFile.readLine()).equals("clone_sets {"))
			;

		while (!(line = readFile.readLine()).startsWith("}")) {
			if (line.startsWith("cloneset:")) {
				int id = Integer.valueOf(line.substring("cloneset:".length()));
				cloneSet = new CloneSet();
				cloneSetList.add(cloneSet);
				if (isNew) {
					cloneSet.setId(id);
				} else {
					cloneSet.setOldId(id);
				}
			}

			if (line.startsWith("\t ")) {
				clone = new Clone();
				clone.setId(cloneId++);

				// フォーマット: <\t methodname - 予約語 @ path\...\xxx.c ( startline:00 endline:00
				// size:00 )\n>
				// [0]="\t", [1]=メソッド名, [2]="@", [3]=ファイルパス, [4]="("
				// [5]="startline:00", [6]="endline:00", [7]="size:00", [8]=")"
				String[] str = line.split(" ");

				clone.setMethodName(str[1]);
				// method名をまとめる
				for (;;) {
					if (str[2].equals("-")) {
						str[1] += " - " + str[3];
						for (int i = 2; i < str.length - 2; i++) {
							str[i] = str[i + 2];
						}
					} else {
						break;
					}
				}

				for (;;) {
					if (str[4].equals("(")) {
						break;
					} else {
						str[3] += " " + str[4];
						for (int i = 4; i < str.length - 1; i++) {
							str[i] = str[i + 1];
						}
					}
				}

				// ファイル名取得
				Path path = new File(str[3]).toPath();
				String abstPath = path.toAbsolutePath().toString();
				String relPath;
				if (isNew) {
					relPath = abstPath.substring(new File(project.getNewDir()).getAbsolutePath().length() + 1);
				} else {
					relPath = abstPath.substring(new File(project.getOldDir()).getAbsolutePath().length() + 1);
				}

				SourceFile sfile = Project.getFileObj(fileList, relPath);

				Logger.writeln("SourceFile is " + (sfile != null ? sfile.getName() : "null"), Logger.DEBUG);
				Logger.writeln("relPath is " + relPath, Logger.DEBUG);

				clone.setFile(sfile);

				String[] tmpstart = str[5].split(":", 0);
				int startline = Integer.parseInt(tmpstart[1]);
				String[] tmpend = str[6].split(":", 0);
				int endline = Integer.parseInt(tmpend[1]);
				clone.setStartLine(startline);
				clone.setStartColumn(0);
				clone.setEndLine(endline);
				clone.setEndColumn(0);

				clone.setCloneSet(cloneSet);

				if (isNew) {
					cloneSet.getNewCloneList().add(clone);
				} else {
					cloneSet.getOldCloneList().add(clone);
				}
			}
		}
		readFile.close();
	}

}

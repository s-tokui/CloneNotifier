package cn.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import cn.Def;
import cn.Logger;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Method;
import cn.data.Project;
import cn.data.SourceFile;
import extractor.CMethodExtractor;
import extractor.JavaMethodExtractor;
import visitor.CFileVistor;
import visitor.JavaFileVistor;

/**
 * <p>Clone Detector のコントローラクラス</p>
 * @author m-sano
 */
public class CloneDetectorController {

	/** 検出対象のプロジェクト */
	private Project project = null;
	private String workDir = null;

	/**
	 * <p>コンストラクタ</p>
	 * @param project Project オブジェクト
	 */
	public CloneDetectorController(Project project) {
		this.project = project;
		this.workDir = project.getWorkDir();
	}

	/**
	 * <p>Clone Detector を実行</p>
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean execute() {

		//実行ファイルのあるディレクトリを
		//カレントディレクトリにしないと正常に動作しない
		String cdTool = Paths.get(Def.NOTIFIER_PATH, Def.CLDT_DIR, Def.CLDT_FILENAME).toString();

		// Clone Detector 実行用コマンド (新バージョン)
		String[] cmdArray1= {
			"java", "-jar",
			cdTool, "-d", project.getNewDir(),
			"-l", project.getLang(),
			"-oc", Paths.get(workDir, Def.RESULT_CSV).toString(),
			"-ot", Paths.get(workDir, Def.RESULT_TXT).toString()
		};

		// Clone Detector 実行用コマンド (旧バージョン)
		String[] cmdArray2= {
				"java", "-jar",
				cdTool, "-d", project.getOldDir(),
				"-l", project.getLang(),
				"-oc", Paths.get(workDir, Def.RESULT_CSV_OLD).toString(),
				"-ot", Paths.get(workDir, Def.RESULT_TXT_OLD).toString()
			};
		ProcessBuilder pb1 = new ProcessBuilder(cmdArray1);
		pb1.directory(new File(Paths.get(Def.NOTIFIER_PATH, Def.CLDT_DIR).toString()));
		ProcessBuilder pb2 = new ProcessBuilder(cmdArray2);
		pb2.directory(new File(Paths.get(Def.NOTIFIER_PATH, Def.CLDT_DIR).toString()));

		// clonedetectorがdataset.txt等の場所を指定できない
		// これらは全プロセス共有になるため, 並列実行不可能

		// TODO もしかしたら pb.redirectErrorStream(true) 入れないとまずいかもしれない
		// XXX 確かどこかでwaitFor() なくても行けるって見たからこうしてるんだと思うが, 一応あった方がいいかも.
		try {
			pb1.redirectErrorStream(true);
			Process pr1 = pb1.start();
			Logger.writeln("Extract new code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract new code clone.", Logger.SYSTEM);
			BufferedReader reader = new BufferedReader(new InputStreamReader(pr1.getInputStream()));
			String line;
			while((line = reader.readLine())!=null) {
				Logger.writeln(line, Logger.SYSTEM);
			}
			reader.close();

			pb2.redirectErrorStream(true);
			Process pr2 = pb2.start();
			Logger.writeln("Extract old code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract old code clone.", Logger.SYSTEM);
			reader = new BufferedReader(new InputStreamReader(pr2.getInputStream()));
			while((line = reader.readLine())!=null) {
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
		if(!nf.exists() || !of.exists()) {
			Logger.writeln("Can't find result file.", Logger.ERROR);
			return false;
		}

		// ファイルが空
		if(nf.length() == 0 || of.length() == 0) {
			Logger.writeln("Result file is empty.", Logger.ERROR);
			return false;
		}
		return true;
	}

	/**
	 * <p>Clone Detectorによって出力されたクローンデータファイルの読込み</p>
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
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
			// ファイルリストを再帰的に取得
			if(project.getLang().equals("java")) {
				newFileList = getFileList(project.getNewDir(), true);
				oldFileList = getFileList(project.getOldDir(), true);
			} else if (project.getLang().equals("c")) {
				newFileList = getFileList(project.getNewDir(), false);
				oldFileList = getFileList(project.getOldDir(), false);
			} else {
				Logger.writeln("Invalid programming langage.", Logger.FATAL);
				return false;
			}

			Logger.writeln("Get source file status.", Logger.INFO);
			Logger.writeln("\t Analyze add & stay files.", Logger.DEBUG);

			// ソースファイルの取得
			Iterator<String> it = newFileList.iterator();
			int fileId = 0;
			while(it.hasNext()) {
				String fileName = it.next();
				SourceFile file = new SourceFile();
				file.setName(fileName);
				file.setNewPath(project.getNewDir() + "\\" + fileName);
				file.setOldPath(project.getOldDir() + "\\" + fileName);
				file.setId(fileId++);

				// 旧ファイルリストに含まれないファイルは新規追加分
				int index = oldFileList.indexOf(fileName);
				if(index > -1) {
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
			while(it.hasNext()) {
				String fileName = it.next();
				SourceFile file = new SourceFile();
				file.setName(fileName);
				file.setOldPath(project.getOldDir() + "\\" + fileName);
				file.setId(fileId++);
				file.setState(SourceFile.DELETED);
				fileList.add(file);
			}

			Logger.writeln("<Success> Get source file status.", Logger.INFO);

			if(project.getLang().equals("java")) {
				Logger.writeln("Analyze new clone result file.", Logger.INFO);
				analyzeResultFileForJava(resultTxt.toString(), cloneSetList, fileList, true);
				Logger.writeln("Analyze old clone result file.", Logger.INFO);
				analyzeResultFileForJava(resultTxtOld.toString(), cloneSetList, fileList, false);
			} else {
				Logger.writeln("Analyze new clone result file.", Logger.INFO);
				analyzeResultFileForC(resultTxt.toString(), cloneSetList, fileList, true);
				Logger.writeln("Analyze old clone result file.", Logger.INFO);
				analyzeResultFileForC(resultTxtOld.toString(), cloneSetList, fileList, false);
			}
	    } catch (FileNotFoundException e) {
	    	Logger.writeln("Can't found read clone file.", Logger.ERROR);
			return false;
		} catch (IOException e) {
			Logger.writeln("<IOException> Can't read clone file.", Logger.ERROR);
			return false;
		} finally {
			if(readFile != null) {
				try {
					readFile.close();
				}catch (IOException e) {
					Logger.writeln("<IOException> Can't close buffer.", Logger.ERROR);
				}
			}
		}

		Logger.writeln("Check error detection.", Logger.INFO);

		// 誤検出の除去
		// XXX CloneDetectorなら不要な可能性
		@SuppressWarnings("unchecked")
		ArrayList<CloneSet> tmpCloneSetList = (ArrayList<CloneSet>)cloneSetList.clone();
		for(CloneSet cloneSet : tmpCloneSetList) {
			if(cloneSet.getNewCloneList().size() == 1) {
				cloneSet.getNewCloneList().remove(0);
			}

			if(cloneSet.getOldCloneList().size() == 1) {
				cloneSet.getOldCloneList().remove(0);
			}

			if(cloneSet.getNewCloneList().isEmpty() && cloneSet.getOldCloneList().isEmpty()) {
				cloneSetList.remove(cloneSet);
			}
		}

		// ソースファイルにクローンの追加
		for(CloneSet cloneSet : cloneSetList) {
			for(Clone clone : cloneSet.getNewCloneList()) {
				clone.getFile().getNewCloneList().add(clone);
			}

			for(Clone clone : cloneSet.getOldCloneList()) {
				clone.getFile().getOldCloneList().add(clone);
			}
		}

		// クローンリストの整列
		for(SourceFile file : fileList) {
			file.sortCloneListbyMethod();
		}

		project.getCloneSetList().addAll(cloneSetList);
		project.getFileList().addAll(fileList);

		Logger.writeln("<Success> Read clone data file.", Logger.INFO);

		return true;
	}

	/**
	 * 出力ファイルの解析 (Java用)
	 * @param result 出力ファイルのパス
	 * @param cloneSetList 解析結果を蓄えるリスト
	 * @param fileList クローンを含む SourceFile の探索用リスト
	 * @param isNew 新プロジェクトに関してなら ture
	 * @return 成功:true, 失敗:false
	 */
	private void analyzeResultFileForJava(String result, ArrayList<CloneSet> cloneSetList, ArrayList<SourceFile> fileList, boolean isNew) throws IOException {
		BufferedReader readFile = new BufferedReader(new FileReader(result));
		int cloneId = 0;
		CloneSet cloneSet = null;
		CloneSet tmpCloneSet = null;
		Clone clone = null;
		String methodName = null;
		String modifiedMethodName = null;
		String line;

		while((line = readFile.readLine()) != null) {

			// クローンセット部
			// clone == null はコード中の文字列に誤爆するのを防ぐため
			if(line.startsWith("cloneset:") && clone == null) {
				int id = Integer.valueOf(line.substring("cloneset:".length()));

				// CloneSetオブジェクトの用意
				cloneSet = null;
				tmpCloneSet = new CloneSet();
				tmpCloneSet.setId(id);
			}

			// クローンを持つファイル名
			if(line.startsWith("filename : ") && clone == null) {
				clone = new Clone();
				clone.setId(cloneId++);

				// 最後に付いている半角スペースの排除
				Path path = new File(line.substring("filename : ".length(), line.length()-1)).toPath();
				String abstPath = path.toAbsolutePath().toString();
				String relPath;

				if(isNew) {
					relPath = abstPath.substring(new File(project.getNewDir()).getAbsolutePath().length()+1);
				} else {
					relPath = abstPath.substring(new File(project.getOldDir()).getAbsolutePath().length()+1);
				}
				SourceFile sfile = Project.getFileObj(fileList, relPath);

				Logger.writeln("Start extracting method '" + relPath + "'.", Logger.DEBUG);
				Logger.writeln("SourceFile is " + (sfile != null ? sfile.getName() : "null"), Logger.DEBUG);
				Logger.writeln("NewFlag is " + isNew, Logger.DEBUG);

				// ファイル内のメソッド抽出
				extractMethodForJava(sfile, abstPath, isNew);

				clone.setFile(sfile);
			}

			// クローンとなっているメソッド名の取得
			if(line.startsWith("methodname : ") && clone != null && methodName == null) {
				String[] mname = line.substring("methodname : ".length(), line.indexOf("(")-1).split("\\.");

				methodName = mname[mname.length-1];
				modifiedMethodName = line.substring("methodname : ".length(), line.indexOf("(")-1);
			}

			// メソッド宣言部の探索
			if(methodName != null && line.contains(methodName + "(") && line.endsWith("{")) {
				String paramStart = methodName + "(";
				String paramPart = line.substring(line.indexOf(paramStart) + paramStart.length(), line.lastIndexOf(")"));

				clone.setMethodName(modifiedMethodName + "(" + paramPart + ")");

				Method parentMd = clone.getFile().getMethod(modifiedMethodName, methodName, paramPart, isNew, true);

				Logger.writeln("<CloneDetectorController.analyzeResultFileForJava> success: get Method.", Logger.DEBUG);

				if(parentMd == null) {
					Logger.writeln("Can't find method belonging clone.\n\tMethod name <" + clone.getMethodName() + ">.", Logger.FATAL);

					if(isNew) {
						ArrayList<Method> mdlist = isNew ? clone.getFile().getNewMethodList() : clone.getFile().getOldMethodList();
						Iterator<Method> itmd = mdlist.iterator();
						while(itmd.hasNext()) {
							Method md = itmd.next();
							Logger.writeln(md.getModifiedName() + md.getParams(), Logger.SYSTEM);
						}
					}
				} else {
					clone.setStartLine(parentMd.getStartLine());
					clone.setStartColumn(parentMd.getStartColumn());
					clone.setEndLine(parentMd.getEndLine());
					clone.setEndColumn(parentMd.getEndColumn());
				}

				Logger.writeln("<CloneDetectorController.analyzeResultFileForJava> clone.getMethodName() is " + clone.getMethodName(), Logger.DEBUG);
				Logger.writeln("<CloneDetectorController.analyzeResultFileForJava> parentMd.getName() is " + parentMd.getName(), Logger.DEBUG);

				// 同一クローンセットの探索は必要情報の都合上, ここに挿入する
				// リストから同一のクローンセットを探す
				if(cloneSet == null) {
					for(CloneSet pCloneSet : cloneSetList) {
						if(pCloneSet.equalsForCloneDetector(clone.getMethodName(), clone.getFile().getName())) {
							cloneSet = pCloneSet;
						}
					}

					if(cloneSet == null) {
						cloneSet = new CloneSet();
						cloneSetList.add(cloneSet);
					}

					// クローンセット情報の移動
					if(isNew) {
						cloneSet.setId(tmpCloneSet.getId());
					} else {
						cloneSet.setOldId(tmpCloneSet.getId());
					}
				}

				clone.setCloneSet(cloneSet);

				// 既に登録済みのクローンなら ID を巻き戻す
				if(isNew) {
					Logger.writeln("<CloneDetectorController.analyzeResultFileForJava> clone.getFile().getName() is " + clone.getFile().getName(), Logger.DEBUG);
					Logger.writeln("<CloneDetectorController.analyzeResultFileForJava> clone.getMethodName() is " + clone.getMethodName(), Logger.DEBUG);

					if(!addClone(cloneSet.getNewCloneList(), clone)) {
						cloneId--;
					}
				} else {
					if(!addClone(cloneSet.getOldCloneList(), clone)) {
						cloneId--;
					}
				}

				//clone.getFile().clearMethodList();

				clone = null;
				methodName = null;
			}
		}
		readFile.close();
	}

	/**
	 * 出力ファイルの解析 (C用)
	 * @param result 出力ファイルのパス
	 * @param cloneSetList 解析結果を蓄えるリスト
	 * @param fileList クローンを含む SourceFile の探索用リスト
	 * @param isNew 新プロジェクトに関してなら ture
	 * @return 成功:true, 失敗:false
	 */
	private void analyzeResultFileForC(String result, ArrayList<CloneSet> cloneSetList, ArrayList<SourceFile> fileList, boolean isNew) throws IOException {
		BufferedReader readFile = new BufferedReader(new FileReader(result));
		int cloneId = 0;
		CloneSet cloneSet = null;
		CloneSet tmpCloneSet = null;
		Clone clone = null;
		String line;

		while((line = readFile.readLine()) != null) {

			// クローン情報の先頭部
			if(line.startsWith("=================================================")) {
				cloneSet = null;
				tmpCloneSet = null;
				clone = null;
			}

			// クローンセットID
			if(line.startsWith("cloneset:") && tmpCloneSet == null) {

				//System.err.println(line);

				int id = Integer.valueOf(line.substring("cloneset:".length()));

				// CloneSetオブジェクトの用意
				tmpCloneSet = new CloneSet();
				tmpCloneSet.setId(id);
			}

			if(line.startsWith("\t ") && tmpCloneSet != null) {
				clone = new Clone();
				clone.setId(cloneId++);

				// フォーマット: <\t methodname @ path\...\xxx.c ( size:00 )\n>
				// [0]="\t", [1]=メソッド名, [2]="@", [3]=ファイルパス, [4]="("
				// [5]="size:00", [6]=")"
				String[] str = line.split(" ");

				// ファイル名取得
				Path path = new File(str[3]).toPath();
				String abstPath = path.toAbsolutePath().toString();
				String relPath;

				if(isNew) {
					relPath = abstPath.substring(new File(project.getNewDir()).getAbsolutePath().length()+1);
				} else {
					relPath = abstPath.substring(new File(project.getOldDir()).getAbsolutePath().length()+1);
				}

				SourceFile sfile = Project.getFileObj(fileList, relPath);

				Logger.writeln("SourceFile is " + (sfile != null ? sfile.getName() : "null"), Logger.DEBUG);
				Logger.writeln("relPath is " + relPath, Logger.DEBUG);

				extractMethodForC(sfile, abstPath, isNew);

				clone.setFile(sfile);
				clone.setMethodName(str[1]);

				Method parentMd = clone.getFile().getMethod(path.getFileName().toString(), str[1], null, isNew, false);
				if(parentMd == null) {
					Logger.writeln("Can't find method belonging clone.", Logger.FATAL);
					Logger.writeln("Method name is " + clone.getMethodName(), Logger.DEBUG);
					Logger.writeln("path.getFileName().toString() is " + path.getFileName().toString(), Logger.DEBUG);
					Logger.writeln("str[1] is " + str[1], Logger.DEBUG);
				} else {
					clone.setStartLine(parentMd.getStartLine());
					clone.setStartColumn(parentMd.getStartColumn());
					clone.setEndLine(parentMd.getEndLine());
					clone.setEndColumn(parentMd.getEndColumn());
				}

				// 同一クローンセットの探索は必要情報の都合上, ここに挿入する
				// リストから同一のクローンセットを探す
				if(cloneSet == null) {
					for(CloneSet pCloneSet : cloneSetList) {
						if(pCloneSet.equalsForCloneDetector(clone.getMethodName(), clone.getFile().getName())) {
							cloneSet = pCloneSet;
						}
					}

					if(cloneSet == null) {
						cloneSet = new CloneSet();
						cloneSetList.add(cloneSet);
					}

					// クローンセット情報の移動
					if(isNew) {
						cloneSet.setId(tmpCloneSet.getId());
					} else {
						cloneSet.setOldId(tmpCloneSet.getId());
					}
				}

				clone.setCloneSet(cloneSet);

				// 既に登録済みのクローンなら ID を巻き戻す
				if(isNew) {
					if(!addClone(cloneSet.getNewCloneList(), clone)) {
						cloneId--;
					}
				} else {
					if(!addClone(cloneSet.getOldCloneList(), clone)) {
						cloneId--;
					}
				}
			}
		}
		readFile.close();
	}

	/**
	 * 指定ディレクトリをルートとして,
	 * 再帰的に検索したファイルリストを返す
	 * @param root
	 * @param javaMode trueなら".java", それ以外なら".c", ".h"
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> getFileList(String root, boolean javaMode) throws IOException {
		File rootFile = new File(root);

		// java モード
		if(javaMode) {
			JavaFileVistor jfv = new JavaFileVistor(rootFile.getAbsolutePath());
			Files.walkFileTree(rootFile.toPath(), jfv);

			return jfv.getFileList();
		} else {
			CFileVistor cfv = new CFileVistor(rootFile.getAbsolutePath());
			Files.walkFileTree(rootFile.toPath(), cfv);

			return cfv.getFileList();
		}
	}

	/**
	 * <p>クローンリストにクローンを追加</p>
	 * @param cloneList クローンリスト
	 * @param clone 追加クローン
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean addClone(ArrayList<Clone> cloneList, Clone clone) {
		for(Clone pClone : cloneList) {
			if(clone.equalsForCloneDetector(pClone)) {
				return false;
			}
		}
		cloneList.add(clone);
		return true;
	}



	/**
	 * ソースファイル中のメソッドを抽出する.
	 * @param sfile 記録するSourceFile
	 * @param abstPath 対象ファイルの絶対パス
	 * @param isNew 新旧ファイルのどちらか
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void extractMethodForJava(SourceFile sfile, String abstPath, boolean isNew) throws FileNotFoundException, IOException {

		Logger.writeln("<extractMethodForJava> SouceFile is " + (sfile != null ? sfile.getName() : "null"), Logger.DEBUG);

		// 既に抽出済みなら終了
		if(isNew) {
			if(sfile.getNewMethodList().size() > 0) {
				return;
			}
		} else {
			if(sfile.getOldMethodList().size() > 0) {
				return;
			}
		}

		Logger.writeln("Start extract method for " + (sfile != null ? sfile.getName() : "null"), Logger.DEBUG);

		JavaMethodExtractor jme = new JavaMethodExtractor(abstPath);

		if(isNew) {
			sfile.setNewMethodList(jme.extractMethod());
		} else {
			sfile.setOldMethodList(jme.extractMethod());
		}
	}



	/**
	 * ソースファイル中のメソッドを抽出する.
	 * @param sfile 記録するSourceFile
	 * @param abstPath 対象ファイルの絶対パス
	 * @param isNew 新旧ファイルのどちらか
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void extractMethodForC(SourceFile sfile, String abstPath, boolean isNew) throws FileNotFoundException, IOException {

		Logger.writeln("<extractMethodForC> SouceFile is " + (sfile != null ? sfile.getName() : "null"), Logger.DEBUG);

		// 既に抽出済みなら終了
		if(isNew) {
			if(sfile.getNewMethodList().size() > 0) {
				return;
			}
		} else {
			if(sfile.getOldMethodList().size() > 0) {
				return;
			}
		}

		Logger.writeln("Start extract method for " + (sfile != null ? sfile.getName() : "null"), Logger.DEBUG);

		CMethodExtractor cme = new CMethodExtractor(abstPath);

		if(isNew) {
			sfile.setNewMethodList(cme.extractMethod());
		} else {
			sfile.setOldMethodList(cme.extractMethod());
		}
	}
}

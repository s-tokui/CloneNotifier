package cn.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.clique.DegeneracyBronKerboschCliqueFinder;
import org.jgrapht.alg.interfaces.MaximalCliqueEnumerationAlgorithm;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import cn.Def;
import cn.Logger;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;
import cn.data.SourceFile;

/**
 * <p>
 * BlockClone Detector のコントローラクラス
 * </p>
 *
 * @author s-tokui
 */
public class SourcererccController {

	/** 検出対象のプロジェクト */
	private Project project = null;

	/**
	 * <p>
	 * コンストラクタ
	 * </p>
	 *
	 * @param project Project オブジェクト
	 */
	public SourcererccController(Project project) {
		this.project = project;
	}

	/**
	 * <p>
	 * SourcererCC を実行
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
		final String sccTokenizer = Paths.get(Def.NOTIFIER_PATH, Def.SCC_DIR, Def.SCC_TOKENIZER_NAME).toString();
		final String sccTool = Paths.get(Def.NOTIFIER_PATH, Def.SCC_DIR, Def.SCC_FILENAME).toString();
		final String workDir = project.getWorkDir();
		final int threshold = 8;
		try {
			Files.createDirectories(Paths.get(workDir, "input", "dataset"));
			Files.createDirectories(Paths.get(workDir, "input", "bookkeeping"));
		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		}
		final String tokensFile = Paths.get(workDir, "input", "dataset", "tokens.file").toString();
		final String headersFile = Paths.get(workDir, "input", "bookkeeping", "headers.file").toString();

		// SourcererCC Tokenizer 実行用コマンド
		String[] cmdArray1 = { "java", "-jar", sccTokenizer, project.getNewDir(), tokensFile, headersFile,
				project.getSCCGranularity(),
				project.getLang(), String.valueOf(project.getTokenTh()), "0", "0", "0" };

		// SourcererCC Tokenizer 実行用コマンド
		String[] cmdArray2 = { "java", "-jar", sccTokenizer, project.getOldDir(), tokensFile, headersFile,
				project.getSCCGranularity(),
				project.getLang(), String.valueOf(project.getTokenTh()), "0", "0", "0" };

		// SourcererCC index 実行用コマンド
		String[] sccIndex = { "java", "-jar", sccTool, "index", Integer.toString(threshold) };

		// SourcererCC search 実行用コマンド
		String[] sccSearch = { "java", "-jar", sccTool, "search", Integer.toString(threshold) };

		try {
			Path source = Paths.get(Def.NOTIFIER_PATH, Def.SCC_DIR, "sourcerer-cc.properties");
			Path targetDir = Paths.get(workDir);
			Files.copy(source, targetDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Logger.writeError(e);
			System.err.println("Can't copy sourcerer-cc.propeties.");
			System.err.println(Paths.get(Def.NOTIFIER_PATH, Def.SCC_DIR, "sourcerer-cc.properties"));
			System.err.println(workDir);
			return false;
		}

		// これらは全プロセス共有になるため, 並列実行不可能
		// XXX 確かどこかでwaitFor() なくても行けるって見たからこうしてるんだと思うが, 一応あった方がいいかも.
		try {
			Logger.writeln("Extract new code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract new code clone.", Logger.SYSTEM);

			executeCmd(cmdArray1, workDir);
			executeCmd(sccIndex, workDir);
			executeCmd(sccSearch, workDir);
			Path clonesIndex = Paths.get(workDir,
					"output" + Integer.toString(threshold) + ".0/tokensclones_index_WITH_FILTER.txt");
			generateResultTxt(Paths.get(headersFile), clonesIndex, Paths.get(workDir, Def.RESULT_TXT));
		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		}

		try {
			Logger.writeln("Extract old code clone.", Logger.SYSTEM);
			Logger.printlnConsole("Extract old code clone.", Logger.SYSTEM);

			executeCmd(cmdArray2, workDir);
			executeCmd(sccIndex, workDir);
			executeCmd(sccSearch, workDir);

			Path clonesIndex = Paths.get(workDir,
					"output" + Integer.toString(threshold) + ".0/tokensclones_index_WITH_FILTER.txt");
			generateResultTxt(Paths.get(headersFile), clonesIndex, Paths.get(workDir, Def.RESULT_TXT_OLD));
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

	private static void executeCmd(String[] command, String directory) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(new File(directory));
		pb.redirectErrorStream(true);
		Process pr = pb.start();

		InputStreamReader in = new InputStreamReader(pr.getInputStream());
		try (BufferedReader reader = new BufferedReader(in)) {
			String line;
			while ((line = reader.readLine()) != null) {
				Logger.writeln(line, Logger.SYSTEM);
			}
		} catch (IOException e) {
			Logger.writeError(e);
			throw e;
		}
	}

	/**
	 * <p>
	 * CloneNotifierより読み込み可能なResultファイルを作成
	 * </p>
	 */
	private void generateResultTxt(Path headers, Path clonesIndex, Path resultTxt) {
		Map<Integer, CodeUnit> codeUnitMap = new HashMap<>();
		Set<String> fileSet = new LinkedHashSet<>();

		try {
			Files.lines(headers).forEach(line -> {
				String[] col = line.split(",");
				CodeUnit codeUnit = new CodeUnit(col[1], Integer.parseInt(col[2]), Integer.parseInt(col[3]));
				codeUnitMap.put(Integer.parseInt(col[0]), codeUnit);
				fileSet.add(col[1]);
			});
		} catch (IOException e) {
			Logger.writeError(e);
		}

		List<Pair<Integer, Integer>> clonePairList = filteringClonePair(readClonesIndexFile(clonesIndex), codeUnitMap);
		List<Set<Integer>> cloneSetList = getCloneSetList(clonePairList);

		try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(resultTxt))) {
			out.println("source_files {");
			fileSet.forEach(file -> out.println(file));
			out.println("}");

			out.println("clone_sets {");
			int index = 0;
			for (Set<Integer> cloneSet : cloneSetList) {
				out.println("=================================================");
				out.println("cloneset:" + index++);
				cloneSet.forEach(cloneId -> out.println(codeUnitMap.get(cloneId).toString()));
			}
			out.println("}");
		} catch (IOException e) {
			Logger.writeError(e);
		}
	}

	/**
	 * <p>
	 * クローンインデックスファイルよりクローンペアを作成
	 * </p>
	 */
	private List<Pair<Integer, Integer>> readClonesIndexFile(Path clonesIndex) {
		List<Pair<Integer, Integer>> clonePairList = new ArrayList<>();
		try {
			Files.lines(clonesIndex).forEach(line -> {
				String[] col = line.split(",");
				int a = Integer.parseInt(col[0]);
				int b = Integer.parseInt(col[1]);
				clonePairList.add(new Pair<>(a, b));
			});
		} catch (IOException e) {
			Logger.writeError(e);
		}

		return clonePairList;
	}

	/**
	 * <p>
	 * クローンペアをフィルタリング
	 * </p>
	 */
	private static List<Pair<Integer, Integer>> filteringClonePair(final List<Pair<Integer, Integer>> clonePairList,
			final Map<Integer, CodeUnit> codeUnitMap) {
		List<Pair<Integer, Integer>> filteredClonePairList = new ArrayList<>();

		for (Pair<Integer, Integer> pair : clonePairList) {
			CodeUnit a = codeUnitMap.get(pair.getFirst());
			CodeUnit b = codeUnitMap.get(pair.getSecond());
			if (isPairWithDescendants(a, b)) {
				continue;
			}
			if (isPairWithDescendants(b, a)) {
				continue;
			}
			if (isDupulicatePair(clonePairList, codeUnitMap, a, b)) {
				continue;
			}
			filteredClonePairList.add(pair);
		}
		return filteredClonePairList;
	}

	/**
	 * <p>
	 * コード片が親子関係にあるか判定
	 * </p>
	 */
	private static boolean isPairWithDescendants(final CodeUnit parent, final CodeUnit child) {
		if (parent.equals(child))
			return true;
		if (!parent.fileName.equals(child.fileName))
			return false;
		if (parent.startLine <= child.startLine && child.endLine <= parent.endLine)
			return true;
		return false;
	}

	/**
	 * <p>
	 * 各コード片を包含するクローンペアが存在するか判定
	 * </p>
	 */
	private static boolean isDupulicatePair(final List<Pair<Integer, Integer>> clonePairList,
			final Map<Integer, CodeUnit> codeUnitMap, final CodeUnit a, final CodeUnit b) {
		for (Pair<Integer, Integer> pair : clonePairList) {
			final CodeUnit pairA = codeUnitMap.get(pair.getFirst());
			final CodeUnit pairB = codeUnitMap.get(pair.getSecond());
			if (isPairWithDescendants(pairA, a) && isPairWithDescendants(pairB, b))
				if (!pairA.equals(a) || !pairB.equals(b))
					return true;
			if (isPairWithDescendants(pairA, b) && isPairWithDescendants(pairB, a))
				if (!pairA.equals(b) || !pairB.equals(a))
					return true;
		}
		return false;
	}

	/**
	 * <p>
	 * クローンペアリストよりクローンセットを作成
	 * </p>
	 */
	private List<Set<Integer>> getCloneSetList(List<Pair<Integer, Integer>> clonePairList) {
		Graph<Integer, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
		clonePairList.forEach(pair -> {
			graph.addVertex(pair.getFirst());
			graph.addVertex(pair.getSecond());
			graph.addEdge(pair.getFirst(), pair.getSecond());
		});

		MaximalCliqueEnumerationAlgorithm<Integer, DefaultEdge> cliqueFinder;
		// cliqueFinder = new BronKerboschCliqueFinder<>(graph);
		// cliqueFinder = new PivotBronKerboschCliqueFinder<>(graph);
		cliqueFinder = new DegeneracyBronKerboschCliqueFinder<>(graph);

		List<Set<Integer>> cloneSetList = new ArrayList<>();
		cliqueFinder.forEach(set -> cloneSetList.add(new TreeSet<>(set)));

		return cloneSetList;
	}

	/**
	 * <p>
	 * BlockClone Detectorによって出力されたクローンデータファイルの読込み
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
		final String resultTxt = Paths.get(project.getWorkDir(), Def.RESULT_TXT).toString();
		final String resultTxtOld = Paths.get(project.getWorkDir(), Def.RESULT_TXT_OLD).toString();

		Logger.writeln("Read clone data file.", Logger.SYSTEM);

		try {
			Logger.writeln("Get fileList.", Logger.INFO);
			newFileList = analyzeResultFileFileList(resultTxt, true);
			oldFileList = analyzeResultFileFileList(resultTxtOld, false);

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
			analyzeResultFileCloneSetList(resultTxt, cloneSetList, fileList, true);
			Logger.writeln("Analyze old clone result file.", Logger.INFO);
			analyzeResultFileCloneSetList(resultTxtOld, cloneSetList, fileList, false);

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
			file.sortCloneListbyLine();
		}

		project.getCloneSetList().addAll(cloneSetList);
		project.getFileList().addAll(fileList);

		Logger.writeln("<Success> Read clone data file.", Logger.INFO);

		return true;
	}

	/**
	 * 出力ファイルの解析(fileList)
	 *
	 * @param result       出力ファイルのパス
	 * @param cloneSetList 解析結果を蓄えるリスト
	 * @param isNew        新プロジェクトに関してなら ture
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
	 * @param result   出力ファイルのパス
	 * @param fileList クローンを含む SourceFile の探索用リスト
	 * @param isNew    新プロジェクトに関してなら ture
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

	class CodeUnit {
		public final String fileName;
		public final int startLine;
		public final int endLine;

		public CodeUnit(String fileName, int startLine, int endLine) {
			this.fileName = fileName;
			this.startLine = startLine;
			this.endLine = endLine;
		}

		@Override
		public String toString() {
			return new StringBuilder("\t null @ ").append(fileName).append(" ( startline:").append(startLine)
					.append(" endline:").append(endLine).append(" token:null )").toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
			result = prime * result + startLine;
			result = prime * result + endLine;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CodeUnit other = (CodeUnit) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (fileName == null) {
				if (other.fileName != null)
					return false;
			} else if (!fileName.equals(other.fileName))
				return false;
			if (startLine != other.startLine)
				return false;
			if (endLine != other.endLine)
				return false;
			return true;
		}

		private SourcererccController getOuterType() {
			return SourcererccController.this;
		}

	}
}

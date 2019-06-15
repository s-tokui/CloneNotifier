package cn.analyze;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cn.Def;
import cn.Logger;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;
import cn.data.SourceFile;

/**
 * <p>
 * クローンセットの変更履歴分類クラス
 * </p>
 * 
 * @author y-yuuki
 */

public class CloneSetCategorizer {

	/**
	 * テキスト類似度による親子クローンの判定
	 * 
	 * @param cloneSetList
	 *            クローンセットリスト TODO 要テスト
	 * @throws IOException
	 */
	public static boolean searchSimCloneSet(Project project) {

		Logger.writeln("writeclone Start.", Logger.INFO);
		ArrayList<CloneSet> newCloneSetList = new ArrayList<CloneSet>();
		ArrayList<CloneSet> oldCloneSetList = new ArrayList<CloneSet>();
		Clone clone = null;
		int id = -1;
		String filePath = null;
		String outputFilePath = null;
		Path dir = Paths.get(project.getWorkDir(), "clone");
		Path newdir = Paths.get(project.getWorkDir(), "clone/new");
		Path olddir = Paths.get(project.getWorkDir(), "clone/old");
		try {
			Files.createDirectories(dir);
			Files.createDirectories(newdir);
			Files.createDirectories(olddir);
		} catch (IOException e) {
			Logger.writeError(e);
			return false;
		}

		for (CloneSet cloneSet : project.getCloneSetList()) {
			if (cloneSet.getId() != CloneSet.NULL) {
				newCloneSetList.add(cloneSet);
				id = cloneSet.getId();
				clone = cloneSet.getNewCloneList().get(0);
				filePath = clone.getFile().getNewPath();
				outputFilePath = newdir.resolve(id + ".txt").toString();
				writeClone(clone, filePath, outputFilePath);
				continue;
			}
			if (cloneSet.getOldId() != CloneSet.NULL) {
				oldCloneSetList.add(cloneSet);
				id = cloneSet.getOldId();
				clone = cloneSet.getOldCloneList().get(0);
				filePath = clone.getFile().getOldPath();
				outputFilePath = olddir.resolve(id + ".txt").toString();
				writeClone(clone, filePath, outputFilePath);
				continue;
			}
		}

		if (!new CCFXController(project).executeForTextSimilarity(olddir.toString(), newdir.toString()))
			return false;

		// 出力結果を読み込んでTextSim計算
		cloneSetTextSim(project, oldCloneSetList, newCloneSetList);

		return true;
	}

	/**
	 * クローンのコード片書き出し
	 * 
	 * @param Clone
	 *            出力クローン
	 * @param String
	 *            filePath
	 * @param String
	 *            outputFilePath
	 */
	private static void writeClone(Clone clone, String filePath, String outputFilePath) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFilePath)));

			String line;
			int lineNum = 0;

			// ソースコード出力
			while ((line = reader.readLine()) != null) {
				lineNum++;
				if (lineNum > clone.getEndLine()) {
					writer.println();
					break;
				}
				if (lineNum < clone.getStartLine())
					continue;
				writer.printf("%s\r\n", line);
			}

			writer.close();
			if (reader != null) {
				reader.close();
			}

		} catch (IOException e) {
			Logger.writeError(e);
		}
	}

	/**
	 * 出力結果の読み取りと解析
	 * 
	 * @param oldCloneSetList
	 * @param newCloneSetList
	 * 
	 */
	private static void cloneSetTextSim(Project project, ArrayList<CloneSet> oldCloneSetList,
			ArrayList<CloneSet> newCloneSetList) {
		int oldCloneListSize = oldCloneSetList.size();
		int newCloneListSize = newCloneSetList.size();

		BufferedReader readFile = null;
		String line = null;
		int[] oldCloneSetIdList = new int[oldCloneListSize + 1];
		int[] newCloneSetIdList = new int[newCloneListSize + 1];
		int[] numOldCloneTokens = new int[oldCloneListSize + 1];
		int[] numNewCloneTokens = new int[newCloneListSize + 1];
		int[][] numDuplicateTokens = new int[oldCloneListSize + 1][newCloneListSize + 1];
		for (int i = 0; i < oldCloneListSize + 1; i++) {
			for (int j = 0; j < newCloneListSize + 1; j++) {
				numDuplicateTokens[i][j] = 0;
			}
		}
		try {
			String textSimTxt = Paths.get(project.getWorkDir(), Def.TEXTSIM_TXT).toString();
			readFile = new BufferedReader(new InputStreamReader(new FileInputStream(textSimTxt)));
			while (!(line = readFile.readLine()).equals("source_files {"))
				;

			// CCfinderが指定したファイル番号とトークン数を配列に読みこみ
			// oldCloneIdListは1からスタート
			// file番号がoldlCloneSetListSizeを超えたらそれとの差をnewCloneSetNumberListにすると、newの方も1からスタート
			int fileId = 0;
			int cloneSetId = 0;
			while (!(line = readFile.readLine()).equals("}")) {
				String str[] = line.split("\t");
				fileId = Integer.valueOf(str[0]);
				str[1] = Paths.get(str[1]).getFileName().toString();
				str[1] = str[1].substring(0, str[1].lastIndexOf('.'));
				cloneSetId = Integer.valueOf(str[1]);
				if (fileId <= oldCloneListSize) {
					oldCloneSetIdList[fileId] = cloneSetId;
					numOldCloneTokens[fileId] = Integer.valueOf(str[2]);
				} else {
					newCloneSetIdList[fileId - oldCloneListSize] = cloneSetId;
					numNewCloneTokens[fileId - oldCloneListSize] = Integer.valueOf(str[2]);
				}
			}

			while (!readFile.readLine().equals("clone_pairs {"))
				;

			// 重複トークン数をカウントしていく(フィルターを掛けつつカウントを重複しないように)
			int fileOldId = 0;
			int fileNewId = 0;
			final Pattern pattern = Pattern.compile("[\t|.|-]");
			while (!(line = readFile.readLine()).equals("}")) {
				String[] str = pattern.split(line);
				fileOldId = Integer.valueOf(str[1]);
				fileNewId = Integer.valueOf(str[4]);
				if ((fileOldId > oldCloneListSize) || (fileNewId <= oldCloneListSize))
					continue;
				int duplication = Integer.valueOf(str[3]) - Integer.valueOf(str[2]) + 1;
				numDuplicateTokens[fileOldId][fileNewId - oldCloneListSize] += duplication;
			}
			readFile.close();

		} catch (IOException e) {
			Logger.writeError(e);
			return;
		}

		// それぞれのtextsimを計算し0.3以上なら子クローンセットに追加
		double textSim = 0;
		final double textSimTH = 0.3;
		// int counta = 0;
		// int countb = 0;
		// int countc = 0;
		CloneSet oldCloneSet = null;
		CloneSet newCloneSet = null;
		for (int i = 1; i < oldCloneListSize + 1; i++) {
			for (int j = 1; j < newCloneListSize + 1; j++) {
				if (numDuplicateTokens[i][j] == 0)
					continue;
				textSim = (2.0 * (double) numDuplicateTokens[i][j])
						/ (double) (numOldCloneTokens[i] + numNewCloneTokens[j]);
				oldCloneSet = null;
				newCloneSet = null;
				for (CloneSet cloneSet : newCloneSetList) {
					if (cloneSet.getId() == newCloneSetIdList[j]) {
						newCloneSet = cloneSet;
						break;
					}
				}
				for (CloneSet cloneSet : oldCloneSetList) {
					if (cloneSet.getOldId() == oldCloneSetIdList[i]) {
						oldCloneSet = cloneSet;
						break;
					}
				}
				if (oldCloneSet == null || newCloneSet == null) {
					System.err.println("can't found oldCloneSet or newCloneSet");
					continue;
				}
				if (textSim >= textSimTH) {
					// counta++;
					if (!oldCloneSet.getChildCloneSetList().contains(newCloneSet)) {
						// countb++;
						oldCloneSet.getChildCloneSetList().add(newCloneSet);
						newCloneSet.getParentCloneSetList().add(oldCloneSet);
					}
				} else if (textSim < textSimTH) {
					if (oldCloneSet.getChildCloneSetList().contains(newCloneSet)) {
						oldCloneSet.getChildCloneSetList().remove(newCloneSet);
						// countc++;
					}
				}
			}
		}

		/*System.out.println(
				"textsim >= 0.3  -->  " + counta + "\nnew track : " + countb + "\ntextsim<0.3 and overlapping : "
						+ countc + "\nrate (countb/counta) : " + (double) ((double) countb / (double) counta));*/

	}

	/**
	 * クローンセットの追跡(cloneSetList)
	 * 
	 * @param cloneSetList
	 *            クローンセットリスト
	 */
	public static ArrayList<CloneSet> getCloneGenealogy(ArrayList<CloneSet> cloneSetList,
			ArrayList<SourceFile> fileList) {
		// 新しいクローンセットリストを作る
		ArrayList<CloneSet> newCloneSetList = new ArrayList<CloneSet>();
		createNewCloneSetList(cloneSetList, newCloneSetList);

		// コード片の分類を再設定
		classifyCloneSet(newCloneSetList);

		// ソースファイルにクローンの追加
		for (SourceFile file : fileList) {
			file.initCloneList();
		}
		for (CloneSet cloneSet : newCloneSetList) {
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

		return newCloneSetList;
	}

	private static void classifyCloneSet(ArrayList<CloneSet> newCloneSetList) {
		int cloneId = 0;
		for (CloneSet cloneSet : newCloneSetList) {
			cloneId = 0;
			// New CloneSet
			if (cloneSet.getOldCloneList().isEmpty()) {
				for (Clone cloneA : cloneSet.getNewCloneList()) {
					cloneA.setId(cloneId++);
					cloneA.setParentClone(null);
				}
				continue;
			}

			// Delete CloneSet
			if (cloneSet.getNewCloneList().isEmpty()) {
				for (Clone cloneA : cloneSet.getOldCloneList()) {
					cloneA.setId(cloneId++);
					cloneA.setChildClone(null);
				}
				continue;
			}

			// Stable or Modified CloneSet
			for (Clone cloneB : cloneSet.getOldCloneList()) {
				cloneB.setChildClone(null);
			}
			for (Clone cloneA : cloneSet.getNewCloneList()) {
				cloneA.setId(cloneId++);
				if (cloneA.getParentClone() == null) {
					continue;
				}
				// if cloneA = Stabled or Modified
				boolean notHaveParentClone = true;
				for (Clone cloneB : cloneSet.getOldCloneList()) {
					if (cloneA.getParentClone().equalsByLine(cloneB)) {
						notHaveParentClone = false;
						cloneA.setParentClone(cloneB);
						cloneB.setChildClone(cloneA);
						cloneB.setCategory(cloneA.getCategory());
						break;
					}
				}
				if (notHaveParentClone) {
					cloneA.setParentClone(null);
					if (cloneA.getCategory() == Clone.STABLE) {
						cloneA.setCategory(Clone.MOVED);
					} else {
						cloneA.setCategory(Clone.ADDED);
					}
				}
			}
			for (Clone cloneB : cloneSet.getOldCloneList()) {
				cloneB.setId(cloneId++);
				if (cloneB.getChildClone() == null) {
					continue;
				}
				if (!cloneSet.getNewCloneList().contains(cloneB.getChildClone())) {
					cloneB.setChildClone(null);
					switch (cloneB.getCategory()) {
					case Clone.STABLE:
						cloneB.setCategory(Clone.DELETED);
						break;

					case Clone.MODIFIED:
						cloneB.setCategory(Clone.DELETE_MODIFIED);
						break;

					default:
						break;
					}
				}
			}
		}
	}

	private static void createNewCloneSetList(ArrayList<CloneSet> cloneSetList, ArrayList<CloneSet> newCloneSetList) {
		int id = -1;
		for (CloneSet cloneSet : cloneSetList) {
			if (cloneSet.getOldId() != CloneSet.NULL) {
				if (cloneSet.getChildCloneSetList().isEmpty()) {
					// 子クローンセットがない場合、そのクローンセット情報を新しいクローンセットリストに追加
					addCloneSet(newCloneSetList, id++, cloneSet, cloneSet.getNewCloneList(),
							cloneSet.getOldCloneList());
					continue;
				} else if (!cloneSet.getChildCloneSetList().isEmpty()) {
					// 子クローンセットがある時、親子クローンセットをまとめて新しいリストに追加
					ArrayList<Integer> realChildCloneSetList = new ArrayList<Integer>();
					double simrate = 0.0;
					double maxrate = -1.0;
					// 最も繋がりのあるクローンセットのみを追跡
					for (int i = 0; i < cloneSet.getChildCloneSetList().size(); i++) {
						simrate = simratecalcurate(cloneSet, i);
						if (maxrate == simrate) {
							realChildCloneSetList.add(i);
						} else if (maxrate < simrate) {
							maxrate = simrate;
							realChildCloneSetList.clear();
							realChildCloneSetList.add(i);
						}
					}
					for (int i : realChildCloneSetList) {
						addCloneSet(newCloneSetList, id++, cloneSet,
								cloneSet.getChildCloneSetList().get(i).getNewCloneList(),
							cloneSet.getOldCloneList());
					}
					continue;
				}
			} else if (cloneSet.getId() != CloneSet.NULL) {
				if (cloneSet.getParentCloneSetList().isEmpty()) {
					// 親クローンセットがない場合、そのクローンセット情報を新しいクローンセットリストに追加
					addCloneSet(newCloneSetList, id++, cloneSet, cloneSet.getNewCloneList(),
							cloneSet.getOldCloneList());
					continue;
				}
			}
		}
	}

	private static void addCloneSet(ArrayList<CloneSet> newCloneSetList, int id, CloneSet cloneSet,
			ArrayList<Clone> nCloneSetList, ArrayList<Clone> oCloneSetList) {
		CloneSet newCloneSet = new CloneSet();
		newCloneSet.setId(id);
		newCloneSetList.add(newCloneSet);
		newCloneSet.copyMetrics(cloneSet);
		if (!nCloneSetList.isEmpty()) {
			for (Clone nClone : nCloneSetList) {
				Clone newClone = new Clone(nClone);
				newClone.setCloneSet(newCloneSet);
				newCloneSet.getNewCloneList().add(newClone);
			}
		}
		if (!oCloneSetList.isEmpty()) {
			for (Clone oClone : oCloneSetList) {
				Clone newClone = new Clone(oClone);
				newClone.setCloneSet(newCloneSet);
				newCloneSet.getOldCloneList().add(newClone);
			}
		}
	}

	private static double simratecalcurate(CloneSet cloneSet, int i) {
		CloneSet childCloneSet = cloneSet.getChildCloneSetList().get(i);
		int haveParentCount = 0;
		for (Clone cloneA : childCloneSet.getNewCloneList()) {
			if (cloneA.getParentClone() == null)
				continue;
			for (Clone cloneB : cloneSet.getOldCloneList()) {
				if (cloneB.equalsByLine(cloneA.getParentClone())) {
					haveParentCount++;
					break;
				}
			}
		}
		int haveChildCount = 0;
		for (Clone cloneB : cloneSet.getOldCloneList()) {
			if (cloneB.getChildClone() == null)
				continue;
			for (Clone cloneA : childCloneSet.getNewCloneList()) {
				if (cloneA.equalsByLine(cloneB.getChildClone())) {
					haveChildCount++;
					break;
				}
			}
		}
		double simrate = ((double) haveParentCount / (double) childCloneSet.getNewCloneList().size()
				+ (double) haveChildCount / (double) cloneSet.getOldCloneList().size()) / 2.0;
		return simrate;
	}

	/**
	 * <p>
	 * クローンセットの分類
	 * </p>
	 * CloneDetector選択時は, メトリクス関連の処理を行わないように.
	 * 
	 * @author m-sano
	 * @param fileList
	 *            クローンセットリスト
	 */
	public static void categorizeCloneSet(ArrayList<CloneSet> cloneSetList, String toolName) {

		for (CloneSet cloneSet : cloneSetList) {
			if (cloneSet.getNewCloneList().isEmpty()) {
				// 新バージョンで消えたら削除済み
				cloneSet.setCategory(CloneSet.DELETED);

			} else if (cloneSet.getOldCloneList().isEmpty()) {
				// 旧バージョンに無いなら新規追加
				cloneSet.setCategory(CloneSet.NEW);
			} else {
				boolean changedFlg = false;

				// メトリクスは CCFinderX のみ
				if (toolName.equals(Def.CCFX_TOOLNAME)) {
					cloneSet.setPOP(cloneSet.getPOP() / 2);
				}

				// クローン変更の有無を確認
				for (Clone clone : cloneSet.getNewCloneList()) {
					if (clone.getCategory() != Clone.STABLE) {
						changedFlg = true;
						break;
					}
				}

				for (Clone clone : cloneSet.getOldCloneList()) {
					if (clone.getCategory() != Clone.STABLE) {
						changedFlg = true;
						break;
					}
				}

				// 変更があれば CHANGED
				if (changedFlg) {
					cloneSet.setCategory(CloneSet.CHANGED);
					subdivisionChangedCloneSet(cloneSet);
				} else {
					cloneSet.setCategory(CloneSet.STABLE);
				}
			}
		}
	}

	/**
	 * <p>
	 * Changedクローンセットの細分類
	 * </p>
	 * 
	 * @author s-tokui
	 * @param CloneSet
	 */
	// TODO 再確認
	public static void subdivisionChangedCloneSet(CloneSet cloneSet) {
		boolean shiftFlag = false;
		boolean consistentFlag = true;
		boolean stableFlag = false;
		boolean modifierFlag = false;
		boolean inConsistentFlag = false;
		for (Clone clone : cloneSet.getOldCloneList()) {
			switch (clone.getCategory()) {

			case Clone.DELETED:
				consistentFlag = false;
				cloneSet.setDivCategory(CloneSet.SUBTRACT);
				break;

			case Clone.DELETE_MODIFIED:
				consistentFlag = false;
				inConsistentFlag = true;
				cloneSet.setDivCategory(CloneSet.SUBTRACT);
				break;

			default:
				break;
			}
		}

		for (Clone clone : cloneSet.getNewCloneList()) {
			switch (clone.getCategory()) {
			case Clone.STABLE:
				stableFlag = true;
				break;

			case Clone.MOVED:
				shiftFlag = true;
				break;

			case Clone.ADDED:
				cloneSet.setDivCategory(CloneSet.ADD);
				break;

			case Clone.MODIFIED:
				modifierFlag = true;
				break;

			default:
				break;
			}
		}

		if (shiftFlag)
			cloneSet.setDivCategory(CloneSet.SHIFT);
		if (consistentFlag && !stableFlag && modifierFlag)
			cloneSet.setDivCategory(CloneSet.CONSISTENT);
		if (inConsistentFlag || (stableFlag && modifierFlag))
			cloneSet.setDivCategory(CloneSet.INCONSISTENT);
	}
}

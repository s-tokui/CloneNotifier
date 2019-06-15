package cn.analyze;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.srcml.SrcmlCppTreeGenerator;
import com.github.gumtreediff.gen.srcml.SrcmlCsTreeGenerator;
import com.github.gumtreediff.gen.srcml.SrcmlJavaTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;

import cn.Logger;
import cn.data.ActionPair;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;

/**
 * <p>クローンセットの危険度計算</p>
 * @author s-tokui
 * @param fileList クローンセットリスト
 * @tool gumtreediff
 */
public class CalculateCloneSetRisk {
	private Project project;
	private String oldCloneTxtPath;
	private String newCloneTxtPath;

	public CalculateCloneSetRisk(Project project) {
		this.project = project;
		this.newCloneTxtPath = Paths.get(project.getWorkDir(), "newclone.txt").toString();
		this.oldCloneTxtPath = Paths.get(project.getWorkDir(), "oldclone.txt").toString();
	}

	public void calculateCloneRisk(ArrayList<CloneSet> cloneSetList) {
		Run.initGenerators();
		for (CloneSet cloneSet : cloneSetList) {

			// クローンセット分類がChange以外は危険度0とする
			if (!(cloneSet.getCategory() == CloneSet.CHANGED)) {
				cloneSet.setRisk(0);
				continue;
			}

			// クローンに子クローンが存在するものだけ編集距離を算出
			// クローン分類がAdd,Deleteは編集操作列はなしとし、Stable,Modifier,MoveのときASTと編集操作列を各クローンに保存
			for (Clone cloneA : cloneSet.getOldCloneList()) {
				if (cloneA.getCategory() == Clone.MODIFIED || cloneA.getCategory() == Clone.MOVED
						|| cloneA.getCategory() == Clone.STABLE) {
					calculateEditDistance(cloneA);
				}
			}

			// 距離の定義から考える
			// クローンセットの各レーベンシュタイン距離を計算
			List<Double> levenshteinDistanceList = new ArrayList<Double>();

			int count = 0;
			for (Clone cloneA : cloneSet.getOldCloneList()) {
				for (Clone cloneB : cloneSet.getOldCloneList()) {
					if (cloneA.getId() < cloneB.getId()) {
						count++;
						if (cloneA.getTreeContext() != null && cloneB.getTreeContext() != null) {
							levenshteinDistanceList.add(calculateLevenshteinDistance(cloneA, cloneB));
						}
					}
				}
			}

			// レーベンシュタイン距離の分散の値を、CloneSetにリスクを追加
			cloneSet.setRisk(calculateVariance(levenshteinDistanceList, count));
		}
	}

	/** 
	 * <p>クローン片のASTの取得と 編集操作列の取得</p>
	 * @param Clone 前バージョンのクローンセットのクローン片(Stable, Move, Modifier)
	 */
	private void calculateEditDistance(Clone cloneA) {
		writeClone(cloneA, cloneA.getId());
		// String absolutePath = "file/oldclone.txt";
		// String absolutePath2 = "file/newclone.txt";
		try {
			TreeContext src = generateITree(oldCloneTxtPath);
			cloneA.setTreeContext(src);
			TreeContext dst = generateITree(newCloneTxtPath);
			cloneA.getChildClone().setTreeContext(dst);
			if (cloneA.getCategory() == Clone.MODIFIED || cloneA.getCategory() == Clone.MOVED) {
				Matcher m = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
				m.match();
				ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), m.getMappings());
				g.generate();
				List<Action> actions = g.getActions();
				MappingStore map = m.getMappings();
				boolean Flag = true;

				for (Action action : actions) {
					ActionPair pair = new ActionPair(action.getNode(), action.getName());
					if (pair.actionType == "INS") {
						pair.tree = map.getSrc(map.firstMappedDstParent(action.getNode()));
					}
					Flag = true;
					for (ActionPair p : cloneA.getEditOperationList()) {
						if (p.tree.equals(pair.tree) && p.actionType == pair.actionType) {
							Flag = false;
						}
					}
					if (Flag) {
						cloneA.addEditOperationList(pair);
					}
				}
			}
		} catch (Exception e) {
			Logger.writeError(e);
		}
	}

	/**
	 * <p>gumtreediffで使用する ソースコード読み込み</p>
	 * @param filename コード片が含まれるファイル名
	 */
	private TreeContext generateITree(String filename) {
		try {
			String lang = project.getLang();
			TreeContext iTree = null;

			if (lang.equals("java")) {
				iTree = new SrcmlJavaTreeGenerator().generateFromFile(filename);
			} else if (lang.equals("c") || lang.equals("cpp")) {
				iTree = new SrcmlCppTreeGenerator().generateFromFile(filename);
			} else if (lang.equals("csharp")) {
				iTree = new SrcmlCsTreeGenerator().generateFromFile(filename);
			}

			return iTree;
		} catch (NullPointerException | IOException e) {
			Logger.writeError(e);
			return null;
		}
	}

	/**
	 * <p>gumtreediffで使用する ソースコードファイルの作成</p>
	 * @param Clone 前バージョンのコード片
	 */
	private void writeClone(Clone cloneA, int id) {
		try {
			//前バージョンtxt出力
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(cloneA.getFile().getOldPath())));
			PrintWriter writer = new PrintWriter(
					new BufferedWriter(new FileWriter(new File(oldCloneTxtPath))));

			String line;
			int lineNum = 0;

			// ソースコード出力
			while ((line = reader.readLine()) != null) {
				lineNum++;
				if (lineNum > cloneA.getEndLine())
					break;
				if (lineNum < cloneA.getStartLine())
					continue;
				if (lineNum == cloneA.getStartLine()) {
					if (line.indexOf("(") == -1) {
						line = "void main()" + line;
					} else {
						line = "void main" + line;
					}
				}
				writer.printf("%s\r\n", line);
			}

			writer.close();
			if (reader != null) {
				reader.close();
			}
			if (!(cloneA.getChildClone() == null)) {
				//後バージョンtxt出力
				Clone cloneB = cloneA.getChildClone();
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(cloneB.getFile().getNewPath())));
				writer = new PrintWriter(
						new BufferedWriter(new FileWriter(new File(newCloneTxtPath))));

				lineNum = 0;

				// ソースコード出力
				while ((line = reader.readLine()) != null) {
					lineNum++;
					if (lineNum > cloneB.getEndLine())
						break;
					if (lineNum < cloneB.getStartLine())
						continue;
					if (lineNum == cloneB.getStartLine()) {
						if (line.indexOf("(") == -1) {
							line = "void main()" + line;
						} else {
							line = "void main" + line;
						}
					}

					writer.printf("%s\r\n", line);
				}

				writer.close();
				if (reader != null) {
					reader.close();
				}
			} else {
				Logger.writeln("cloneA has no ChildClone because cloneA belong to " + cloneA.getCategoryString(),
						Logger.ERROR);
			}
		} catch (IOException e) {
			Logger.writeError(e);
		}
	}

	/** 
	 * <p>レーベンシュタイン距離の計算</p>
	 * @param cloneA 前バージョンのコード片
	 * @param cloneB 前バージョンのコード片
	 */
	private static Double calculateLevenshteinDistance(Clone cloneA, Clone cloneB) {
		double levenshteinDistance = 0;

		TreeContext src = cloneA.getTreeContext();
		TreeContext dst = cloneB.getTreeContext();
		Matcher m = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
		m.match();

		List<ActionPair> actionsA = cloneA.getEditOperationList();
		List<ActionPair> actionsB = cloneB.getEditOperationList();
		MappingStore map = m.getMappings();

		int size = actionsA.size();

		for (int i = size - 1; i >= 0; i--) {
			if (!map.hasSrc(actionsA.get(i).tree)) {
				actionsA.remove(i);
			}
		}
		size = actionsB.size();
		for (int i = size - 1; i >= 0; i--) {
			if (!map.hasDst(actionsB.get(i).tree)) {
				actionsB.remove(i);
			}
		}
		levenshteinDistance = calculateDiffAction(actionsA, actionsB, map);
		return Double.valueOf(levenshteinDistance);
	}

	/**
	 * <p>actionListの差分を計算(一致ノードの片方だけ編集されている，あるいは編集内容が異なる)<p>
	 * @param List<ActionPair> cloneA.getEditOperationList()
	 * @param List<ActionPair> cloneB.getEditOperationList()
	 * @return double levendistance
	 * */
	private static double calculateDiffAction(List<ActionPair> actionsA, List<ActionPair> actionsB, MappingStore map) {
		int diffDistance = 0;
		int count = 0;
		for (ActionPair pair : actionsA) {
			for (ActionPair pairClone : actionsB) {
				if (map.has(pair.tree, pairClone.tree) && pair.actionType == pairClone.actionType) {
					count++;
				}
			}
		}
		diffDistance = actionsA.size() + actionsB.size() - 2 * count;
		return diffDistance;
	}

	/**
	 * <p>レーベンシュタイン距離の分散を計算</p>
	 * @param levenshteinDistanceList cloneset内のすべての組み合わせのレーベンシュタイン距離のリスト
	 */
	private static double calculateVariance(List<Double> levenshteinDistanceList, int size) {
		double variance = 0;
		double average = 0;
		for (Double distance : levenshteinDistanceList) {
			double x = distance.doubleValue();
			variance += x * x;
			average += x;
		}
		average /= size;

		for (Double distance : levenshteinDistanceList) {
			double x = distance.doubleValue();
			variance += (x - average) * (x - average);
		}
		variance /= size;
		return variance;
	}
}

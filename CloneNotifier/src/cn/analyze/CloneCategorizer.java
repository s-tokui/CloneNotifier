package cn.analyze;

import java.util.ArrayList;

import cn.data.Clone;
import cn.data.SourceFile;

/**
 * <p>
 * コードクローンの変更履歴分類クラス
 * </p>
 * 
 * @author y-yuuki
 */
public class CloneCategorizer {

	/**
	 * <p>
	 * コードクローンの分類
	 * </p>
	 * 
	 * @param fileList
	 *            ソースファイルリスト
	 */
	public void categorizeClone(ArrayList<SourceFile> fileList) {

		for (SourceFile file : fileList) {
			if (file.getState() == SourceFile.NORMAL) {
				categorizeStableModified(file);
			}

			// 実行時点で, 新旧両方に存在するクローンは分類されているはず
			categorizeAddedDeleted(file);

		}
	}

	/**
	 * <p>
	 * Stable/Modifiedクローンの分類
	 * </p>
	 * 
	 * @param file
	 *            クローン分類を行うソースファイル
	 */
	private void categorizeStableModified(SourceFile file) {

		for (Clone cloneA : file.getNewCloneList()) {

			int addedLineStart = 0;
			int addedLineEnd = 0;

			// コードクローンの開始行/終了行までの追加行の計算
			for (int line : file.getAddedCodeList()) {
				if (line < cloneA.getStartLine()) {
					addedLineStart++;
					addedLineEnd++;
				} else if (line <= cloneA.getEndLine()) {
					addedLineEnd++;
				} else {
					break;
				}
			}

			for (Clone cloneB : file.getOldCloneList()) {

				int deletedLineStart = 0;
				int deletedLineEnd = 0;
				// コードクローンの開始行/終了行までの削除行の計算
				for (int line : file.getDeletedCodeList()) {
					if (line < cloneB.getStartLine()) {
						deletedLineStart++;
						deletedLineEnd++;
					} else if (line <= cloneB.getEndLine()) {
						deletedLineEnd++;
					} else {
						break;
					}
				}

				// 親子関係が存在するか判定
				// 行の重複が30%以上のとき追跡
				int startLineA = cloneA.getStartLine() - addedLineStart;
				int endLineA = cloneA.getEndLine() - addedLineEnd;
				int startLineB = cloneB.getStartLine() - deletedLineStart;
				int endLineB = cloneB.getEndLine() - deletedLineEnd;
				double sim = calcurateLocationSimilarity(cloneA, cloneB, startLineA, endLineA, startLineB, endLineB);
				if (sim >= 0.3) {
					if (cloneA.getParentClone() != null) {
						if (sim > cloneA.getLocationSimilarity()) {
							cloneA.setParentClone(cloneB);
							cloneA.setLocationSimilarity(sim);
							if (addedLineStart == addedLineEnd && deletedLineStart == deletedLineEnd) {
								cloneA.setCategory(Clone.STABLE);
							} else {
								cloneA.setCategory(Clone.MODIFIED);
							}
						}
					}else {
						cloneA.setParentClone(cloneB);
						cloneA.setLocationSimilarity(sim);
						if (addedLineStart == addedLineEnd && deletedLineStart == deletedLineEnd) {
							cloneA.setCategory(Clone.STABLE);
						} else {
							cloneA.setCategory(Clone.MODIFIED);
						}
					}
					if (cloneB.getChildClone() != null) {
						if (sim > cloneB.getLocationSimilarity()) {
							cloneB.setChildClone(cloneA);
							cloneB.setLocationSimilarity(sim);
							if (addedLineStart == addedLineEnd && deletedLineStart == deletedLineEnd) {
								cloneB.setCategory(Clone.STABLE);
							} else {
								cloneB.setCategory(Clone.MODIFIED);
							}
						}
					}else {
						cloneB.setChildClone(cloneA);
						cloneB.setLocationSimilarity(sim);
						if (addedLineStart == addedLineEnd && deletedLineStart == deletedLineEnd) {
							cloneB.setCategory(Clone.STABLE);
						} else {
							cloneB.setCategory(Clone.MODIFIED);
						}
					}
					

					if (!cloneB.getCloneSet().getChildCloneSetList().contains(cloneA.getCloneSet())) {
						cloneB.getCloneSet().getChildCloneSetList().add(cloneA.getCloneSet());
					}
					if (!cloneA.getCloneSet().getParentCloneSetList().contains(cloneB.getCloneSet())) {
						cloneA.getCloneSet().getParentCloneSetList().add(cloneB.getCloneSet());
					}
				}
			}
		}
	}

	private double calcurateLocationSimilarity(Clone cloneA, Clone cloneB, int startLineA, int endLineA, int startLineB,
			int endLineB) {
		double sim = 0.0;
		int lineA = cloneA.getEndLine() - cloneA.getStartLine();
		int lineB = cloneB.getEndLine() - cloneB.getStartLine();

		if (startLineA >= endLineB) {
			return 0;
		}
		if (startLineB >= endLineA) {
			return 0;
		}
		if (startLineA <= startLineB) {
			if (endLineA >= endLineB) {
				sim = 2.0 * (double) (endLineB - startLineB) / (double) (lineA + lineB);
			} else if (endLineA < endLineB) {
				sim = 2.0 * (double) (endLineA - startLineB) / (double) (lineA + lineB);
			}
		}
		if (startLineA > startLineB) {
			if (endLineA <= endLineB) {
				sim = 2.0 * (double) (endLineA - startLineA) / (double) (lineA + lineB);
			} else if (endLineA < endLineB) {
				sim = 2.0 * (double) (endLineB - startLineA) / (double) (lineA + lineB);
			}
		}
		return sim;
	}

	/**
	 * <p>
	 * Added/Deletedクローンの分類
	 * </p>
	 * 
	 * @param file
	 *            クローン分類を行うソースファイル
	 */
	private void categorizeAddedDeleted(SourceFile file) {

		// Addedクローンの分類
		for (Clone clone : file.getNewCloneList()) {
			if (clone.getCategory() == Clone.NULL) {
				clone.setCategory(Clone.ADDED);
			}
		}

		// Deletedクローンの分類
		for (Clone clone : file.getOldCloneList()) {
			if (clone.getCategory() == Clone.NULL) {
				clone.setCategory(Clone.DELETED);
			}
		}
	}
}

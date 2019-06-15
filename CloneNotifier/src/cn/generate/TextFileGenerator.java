package cn.generate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import cn.Def;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;
import cn.data.SourceFile;

/**
 * <p>テキストファイル出力クラス</p>
 * @author y-yuuki
 */
public class TextFileGenerator {
	private OutputGenerator generator = null;
	private Project project = null;

	/** テキスト出力用のライター */
	private static PrintWriter writer = null;

	/** クローン部の何行前から何行後まで出力するか */
	private static int EXTRA_LINE = 3;

	private final static int ADD = 0;
	private final static int DELETE = 1;
	private final static int NORMAL = 2;

	public TextFileGenerator(OutputGenerator outputGenerator, Project project) {
		this.generator = outputGenerator;
		this.project = project;
	}

	/**
	 * <p>テキストファイル生成</p>
	 * @param generator OutputGeneratorオブジェクト
	 * @param project Projectオブジェクト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateTextFile() {

		switch (project.getTool()) {
		case Def.CD_TOOLNAME:
			EXTRA_LINE = 0;
			break;

		default:
			EXTRA_LINE = 3;
		}

		try {
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(project.getGenerateTextDir() + "//" + project.getGenerateTextFileName()),
					"UTF-8")));
			// プロジェクトタイトル
			writer.println("###################################################################");
			writer.println("\tプロジェクト名: " + project.getName());
			writer.println("\t言語： " + project.getLang());
			writer.println("###################################################################");

			writer.println();

			writer.println("■ファイル情報 ");
			writer.printf("\t-総ファイル数：%d\r\n", generator.getFileNum());
			writer.printf("\t-追加ファイル数：%d\r\n", generator.getAddedFileNum());
			writer.printf("\t-削除ファイル数：%d\r\n", generator.getDeletedFileNum());
			writer.printf("\t-クローンを含むファイル数：%d\r\n", generator.getCloneFileNum());
			writer.println();

			writer.println("■クローンセット分類数");
			writer.printf("\t-STABLE Clone Set：%d\r\n", generator.getStableCloneSetNum());
			writer.printf("\t-CHANGED Clone Set：%d\r\n", generator.getChangedCloneSetNum());
			writer.printf("\t-NEW Clone Set：%d\r\n", generator.getNewCloneSetNum());
			writer.printf("\t-DELETED Clone Set：%d\r\n", generator.getDeletedCloneSetNum());
			writer.println();

			writer.println("■クローン分類");
			writer.printf("\t-STABLE Clone：%d\r\n", generator.getStableCloneNum());
			writer.printf("\t-MODIFIED Clone:%d\r\n", generator.getModifiedCloneNum());
			writer.printf("\t-MOVED Clone：%d\r\n", generator.getMovedCloneNum());
			writer.printf("\t-ADDED Clone：%d\r\n", generator.getAddedCloneNum());
			writer.printf("\t-DELETED Clone：%d\r\n", generator.getDeletedCloneNum());
			writer.printf("\t-MODIFIEDandDELETED Clone：%d\r\n", generator.getDeleteModifiedCloneNum());
			writer.println();

			ArrayList<CloneSet> cloneSetList = project.getCloneSetList();

			int state = 0;
			for (int i = 0; i < cloneSetList.size(); i++) {
				CloneSet cloneSet = cloneSetList.get(i);
				int cat = cloneSet.getCategory();
				while (state < cat) {
					state++;
					outputTitle(state);
					if (state < cat) {
						outputNotAbleDetect(state);
					}
				}
				outputCloneSet(cloneSet);
			}

			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}

	/**
	 * <p>クローンセットの分類のタイトル</p>
	 * @param int category
	 */
	private void outputTitle(int cat) {
		writer.println("###################################################################");
		switch (cat) {
		case CloneSet.CHANGED:
			writer.println("    Changed Clone Set 一覧");
			break;

		case CloneSet.STABLE:
			writer.println("    Stable Clone Set 一覧");
			break;

		case CloneSet.NEW:
			writer.println("    NEW Clone Set 一覧");
			break;

		case CloneSet.DELETED:
			writer.println("    Deleted Clone Set 一覧");
			break;
		}
		writer.println("###################################################################");
	}

	/**
	 * <p>クローンセットのがなかった場合の出力</p>
	 * @param int category
	 */
	private void outputNotAbleDetect(int cat) {
		switch (cat) {
		case CloneSet.CHANGED:
			writer.println("Changed Clone Set は検出されませんでした");
			break;

		case CloneSet.STABLE:
			writer.println("Stable Clone Set は検出されませんでした");
			break;

		case CloneSet.NEW:
			writer.println("New Clone Set は検出されませんでした");
			break;

		case CloneSet.DELETED:
			writer.println("Deleted Clone Set は検出されませんでした");
			break;
		}
		writer.println();
	}

	/**
	 * <p>クローンセット情報の出力</p>
	 * @param cloneSet CloneSetオブジェクト
	 * @throws IOException
	 */
	private void outputCloneSet(CloneSet cloneSet) throws IOException {

		// ヘッダ部出力
		writer.printf("*************************************************************\r\n");
		writer.printf("  @CloneSet%d\r\n", cloneSet.getOutputId());
		writer.printf("  @Risk=%f\r\n", cloneSet.getRisk());
		writer.printf("*************************************************************\r\n");

		// クローンリストの出力
		outputCloneList(cloneSet.getNewCloneList(), cloneSet.getOldCloneList());

		// クローンコードの出力
		outputCodeClones(cloneSet.getNewCloneList(), cloneSet.getOldCloneList());

		writer.println();
	}

	/**
	 * <p>クローンリストの出力</p>
	 * @param newCloneList - 新クローンリスト
	 * @param oldCloneList - 旧クローンリスト
	 */
	private void outputCloneList(ArrayList<Clone> newCloneList, ArrayList<Clone> oldCloneList)
			throws IOException {
		for (Clone clone : newCloneList) {
			outputCloneInformation(clone);
		}

		// 削除/移動クローンが存在するなら, それも出力
		boolean state = true;
		for (Clone clone : oldCloneList) {
			if (clone.getCategory() == Clone.DELETE_MODIFIED || clone.getCategory() == Clone.DELETED) {
				if (state) {
					writer.println("----Previous Version----");
					state = false;
				}
				outputCloneInformation(clone);
			}
		}
	}

	/**
	 * <p>クローン情報の出力</p>
	 * @param Clone クローン
	 */
	private void outputCloneInformation(Clone clone) throws IOException {
		writer.printf("@%d.%d", clone.getCloneSet().getOutputId(), clone.getOutputId());
		writer.printf("\t%s\t%s\t%d.%d-%d.%d\r\n",
				clone.getCategoryString(),
				clone.getFile().getName(),
				clone.getStartLine(),
				clone.getStartColumn(),
				clone.getEndLine(),
				clone.getEndColumn());
	}

	/**
	 * <p>クローンコードの出力</p>
	 * @param newCloneList - 新クローンリスト
	 * @param oldCloneList - 旧クローンリスト
	 */
	private void outputCodeClones(ArrayList<Clone> newCloneList, ArrayList<Clone> oldCloneList)
			throws IOException {
		boolean stableFlg = true;
		for (Clone clone : newCloneList) {
			if ((clone.getCategory() == Clone.STABLE) && stableFlg) {
				writer.println("Stable Clone のサンプル");
				stableFlg = false;
			}
			outputCloneCode(clone);
		}

		for (Clone clone : oldCloneList) {
			if (clone.getCategory() == Clone.DELETE_MODIFIED || clone.getCategory() == Clone.DELETED) {
				outputCloneCode(clone);
			}
		}
	}

	/**
	 * <p>コード片の出力</p>
	 * @param clone - 出力クローン
	 * @throws IOException
	 */
	private void outputCloneCode(Clone clone) throws IOException {
		writer.println("-------------------------------------------------");
		writer.printf("@%d.%d\r\n", clone.getCloneSet().getOutputId(), clone.getOutputId());
		writer.printf("%s\r\n", clone.getFile().getName());

		BufferedReader reader;
		int lineNum = 0;
		String line;
		// 追加ファイルの場合
		switch (clone.getFile().getState()) {
		case SourceFile.ADDED:
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(clone.getFile().getNewPath())));
			while ((line = reader.readLine()) != null) {
				lineNum++;
				writeCode(clone, lineNum, line, ADD);
			}
			reader.close();
			break;

		// 削除ファイル
		case SourceFile.DELETED:
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(clone.getFile().getOldPath())));
			while ((line = reader.readLine()) != null) {
				lineNum++;
				writeCode(clone, lineNum, line, DELETE);
			}
			reader.close();
			break;

		// 存続ファイル
		case SourceFile.NORMAL:
			int addCodeId = 0, deleteCodeId = 0;
			int lineNumA = 0, lineNumB = 0;
			String lineA, lineB;

			BufferedReader readerA = new BufferedReader(
					new InputStreamReader(new FileInputStream(clone.getFile().getNewPath())));
			BufferedReader readerB = new BufferedReader(
					new InputStreamReader(new FileInputStream(clone.getFile().getOldPath())));

			int outputStart = clone.getStartLine() - EXTRA_LINE;
			int outputEnd = clone.getEndLine() + EXTRA_LINE;
			boolean cloneFlg = false;

			// DeletedCloneかDeleteModifiedCloneの場合
			if ((clone.getCategory() == Clone.DELETED) || (clone.getCategory() == Clone.DELETE_MODIFIED)) {

				while (true) {
					// 新旧ファイルの1行読み込み
					if ((lineA = readerA.readLine()) != null)
						lineNumA++;
					if ((lineB = readerB.readLine()) != null)
						lineNumB++;

					// 両方終了した
					if (lineA == null && lineB == null)
						break;

					// 追加行の場合
					while (addCodeId < clone.getFile().getAddedCodeList().size()
							&& lineNumA == clone.getFile().getAddedCodeList().get(addCodeId)) {
						addCodeId++;
						if (cloneFlg)
							writeCode(clone, lineNumA, lineA, ADD);
						if ((lineA = readerA.readLine()) != null)
							lineNumA++;
					}

					// 削除行の場合
					while (deleteCodeId < clone.getFile().getDeletedCodeList().size()
							&& lineNumB == clone.getFile().getDeletedCodeList().get(deleteCodeId)) {
						deleteCodeId++;
						if (cloneFlg = (isLineInClone(lineNumB, outputStart, outputEnd) && lineB != null))
							break;

						if (cloneFlg)
							writeCode(clone, lineNumB, lineB, DELETE);

						if ((lineB = readerB.readLine()) != null)
							lineNumB++;
					}

					// 通常コードの場合
					if (cloneFlg = (isLineInClone(lineNumB, outputStart, outputEnd) && lineB != null))
						break;

					if (cloneFlg)
						writeCode(clone, lineNumB, lineB, NORMAL);
				}
			} else if (!(clone.getCategory() == Clone.DELETED) || (clone.getCategory() == Clone.DELETE_MODIFIED)) {
				// DeletedCloneでもDeleteModifiedCloneでもない場合
				while (true) {

					// 新旧ファイルの1行読み込み
					if ((lineA = readerA.readLine()) != null) {
						lineNumA++;
					}
					if ((lineB = readerB.readLine()) != null) {
						lineNumB++;
					}

					// 両方終了した
					if (lineA == null && lineB == null) {
						break;
					}

					// 追加行の場合
					while (addCodeId < clone.getFile().getAddedCodeList().size()
							&& lineNumA == clone.getFile().getAddedCodeList().get(addCodeId)) {
						addCodeId++;
						if (cloneFlg = (isLineInClone(lineNumA, outputStart, outputEnd) && lineA != null))
							break;

						if (cloneFlg) {
							writeCode(clone, lineNumA, lineA, ADD);
						}

						if ((lineA = readerA.readLine()) != null) {
							lineNumA++;
						}
					}

					// 削除行の場合
					while (deleteCodeId < clone.getFile().getDeletedCodeList().size()
							&& lineNumB == clone.getFile().getDeletedCodeList().get(deleteCodeId)) {
						deleteCodeId++;
						if (cloneFlg) {
							writeCode(clone, lineNumB, lineB, DELETE);
						}

						if ((lineB = readerB.readLine()) != null) {
							lineNumB++;
						}
					}

					// 通常コードの場合
					if (cloneFlg = (isLineInClone(lineNumA, outputStart, outputEnd) && lineA != null))
						break;

					if (cloneFlg) {
						writeCode(clone, lineNumA, lineA, NORMAL);
					}
				}

				readerA.close();
				readerB.close();
			}
		}
	}

	/**
	 * <p>クローン書き出し</p>
	 * @param Clone clone
	 * @param int lineNum
	 * @param String line 読み込み中の行
	 * @param int 追加行か削除行か変更なしか
	 */
	private void writeCode(Clone clone, int lineNum, String line, int state) throws IOException {
		if (clone.getStartLine() == lineNum) {
			writer.println("    <START " + clone.getCategoryString() + "Clone>");
		}

		if (lineNum >= clone.getStartLine() - EXTRA_LINE && lineNum <= clone.getEndLine() + EXTRA_LINE) {
			writer.printf("%-4d", lineNum);

			switch (state) {
			case ADD:
				writer.println("+ " + line);
				break;

			case DELETE:
				writer.println("- " + line);
				break;

			case NORMAL:
				writer.println("  " + line);
				break;
			}
		}

		if (clone.getEndLine() == lineNum) {
			writer.println("    <END>");
		}
	}

	/**
	 * <p>クローン部を読み込み中かどうか判定</p>
	 * @param int lineNum
	 * @param int outputStart
	 * @param int outputEnd
	 * @return boolean クローン部読み込み中
	 */
	private boolean isLineInClone(int lineNum, int outputStart, int outputEnd) {
		if (lineNum >= outputStart && lineNum <= outputEnd)
			return true;
		return false;
	}

	/**
	 * <p>メール本文に記述する内容の生成</p>
	 */
	public boolean generateMailText() {
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(project.getWorkDir() + "//mailtext.txt")));

			// プロジェクトタイトル
			writer.println("---");
			writer.println("プロジェクト: " + project.getName());
			writer.println("言語： " + project.getLang());
			writer.println("---");

			writer.println();

			writer.println("■ファイル情報 ");
			writer.printf("  -総ファイル数：%d\r\n", generator.getFileNum());
			writer.printf("  -追加ファイル数：%d\r\n", generator.getAddedFileNum());
			writer.printf("  -削除ファイル数：%d\r\n", generator.getDeletedFileNum());
			writer.printf("  -クローンを含むファイル数：%d\r\n", generator.getCloneFileNum());
			writer.println();

			writer.println("■クローンセット分類数");
			writer.printf("  -STABLE Clone Set：%d\r\n", generator.getStableCloneSetNum());
			writer.printf("  -CHANGED Clone Set：%d\r\n", generator.getChangedCloneSetNum());
			writer.printf("  -NEW Clone Set：%d\r\n", generator.getNewCloneSetNum());
			writer.printf("  -DELETED Clone Set：%d\r\n", generator.getDeletedCloneSetNum());
			writer.println();

			writer.println("■クローン分類");
			writer.printf("  -STABLE Clone：%d\r\n", generator.getStableCloneNum());
			writer.printf("  -MODIFIED Clone:%d\r\n", generator.getModifiedCloneNum());
			writer.printf("  -MOVED Clone：%d\r\n", generator.getMovedCloneNum());
			writer.printf("  -ADDED Clone：%d\r\n", generator.getAddedCloneNum());
			writer.printf("  -DELETED Clone：%d\r\n", generator.getDeletedCloneNum());
			writer.printf("  -MODIFIEDandDELETED Clone：%d\r\n", generator.getDeleteModifiedCloneNum());
			writer.println();

			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}
}

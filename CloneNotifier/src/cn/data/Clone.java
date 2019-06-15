package cn.data;

import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.tree.TreeContext;

/**
 * <p>クローンデータクラス</p>
 * @author y-yuuki
 */
public class Clone {

	/** クローンID */
	private int id = NULL;
	private int outputId = NULL;

	/** 開始行番号 */
	private int startLine = NULL;

	/** 開始列番号 */
	private int startColumn = NULL;

	/** クローンの開始トークン番号 */
	private int startToken = NULL;

	/** 終了行番号 */
	private int endLine = NULL;

	/** 終了列番号 */
	private int endColumn = NULL;

	/** クローンの終了トークン番号 */
	private int endToken = NULL;
	private Clone parentClone = null;
	private Clone childClone = null;
	private int category = NULL;

	/** このクローンが属するクローンセット */
	private CloneSet cloneSet = null;

	/** このクローンを含んでいるファイル */
	private SourceFile file = null;

	/**
	 * <p>
	 * 編集距離
	 * gumtree用
	 * <p>
	 * @author s-tokui
	 * */
	private List<ActionPair> editOperationList = new ArrayList<ActionPair>();
	private TreeContext srcTree = null;

	private double localitySimilarity = 0.0;

	/**
	 * <p>
	 * メソッドクローンのメソッド名と引数.
	 * CloneDetector用.
	 * </p>
	 * Class.Subclass.MethodName(Type param,Type param)
	 * @author m-sano
	 */
	private String methodName = null;

	/** <p>初期状態</p> */
	public final static int NULL = -1;

	/** <p>Stable Clone</p> */
	public final static int STABLE = 0;

	/** <p>Modified Clone</p> */
	public final static int MODIFIED = 1;

	/** <p>Moved Clone</p> */
	public final static int MOVED = 2;

	/** <p>Added Clone</p> */
	public final static int ADDED = 3;

	/** <p>Deleted Clone</p> */
	public final static int DELETED = 4;

	/** <p>Modified and Deleted Clone</p> */
	public final static int DELETE_MODIFIED = 5;

	// コンストラクタ
	public Clone() {
	}

	// 新しいクローンオブジェクト作成のためのコンストラクタ
	public Clone(Clone clone) {
		this.category = clone.getCategory();
		this.file = clone.getFile();
		this.methodName = clone.getMethodName();
		this.startLine = clone.getStartLine();
		this.endLine = clone.getEndLine();
		this.startColumn = clone.getStartColumn();
		this.endColumn = clone.getEndColumn();
		this.startToken = clone.getStartToken();
		this.endToken = clone.getEndToken();
		this.childClone = clone.getChildClone();
		this.parentClone = clone.getParentClone();
	}

	/**
	 * <p>クローンIDの取得</p>
	 * @return クローンID
	 */
	public int getId() {
		return id;
	}

	/**
	 * <p>クローンセットIDの取得</p>
	 * @param id クローンID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/** <p>クローン開始行の取得</p>
	 * @return 開始行
	 */
	public int getStartLine() {
		return startLine;
	}

	/**
	 * <p>クローン開始行の設定</p>
	 * @param startLine 開始行
	 */
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}

	/**
	 * <p>クローン開始列の取得</p>
	 * @return 開始列
	 */
	public int getStartColumn() {
		return startColumn;
	}

	/**
	 * <p>クローン開始列の設定</p>
	 * @param startColumn 開始列
	 */
	public void setStartColumn(int startColumn) {
		this.startColumn = startColumn;
	}

	/**
	 * <p>クローン開始トークンの取得</p>
	 * @return 開始トークン
	 */
	public int getStartToken() {
		return startToken;
	}

	/**
	 * <p>クローン開始トークの設定</p>
	 * @param startToken 開始トークン
	 */
	public void setStartToken(int startToken) {
		this.startToken = startToken;
	}

	/**
	 * <p>クローン終了行の取得</p>
	 * @return 終了行
	 */
	public int getEndLine() {
		return endLine;
	}

	/**
	 * <p>クローン終了行の設定</p>
	 * @param endLine 終了行
	 */
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}

	/**
	 * <p>クローン終了列の取得</p>
	 * @return 終了列
	 */
	public int getEndColumn() {
		return endColumn;
	}

	/**
	 * <p>クローン終了列の設定</p>
	 * @param endColumn 終了列
	 */
	public void setEndColumn(int endColumn) {
		this.endColumn = endColumn;
	}

	/**
	 * <p>クローン終了トークンの取得</p>
	 * @return 終了トークン
	 */
	public int getEndToken() {
		return endToken;
	}

	/**
	 * <p>クローン終了トークンの設定</p>
	 * @param endToken
	 */
	public void setEndToken(int endToken) {
		this.endToken = endToken;
	}

	/**
	 * <p>親クローンの取得</p>
	 * @return 親クローンオブジェクト
	 */
	public Clone getParentClone() {
		return parentClone;
	}

	/**
	 * <p>親クローンの設定</p>
	 * @param parentClone 親クローンオブジェクト
	 */
	public void setParentClone(Clone parentClone) {
		this.parentClone = parentClone;
	}

	/**
	 * <p>子クローンの取得</p>
	 * @return 子クローンオブジェクト
	 */
	public Clone getChildClone() {
		return childClone;
	}

	/**
	 * <p>子クローンの設定</p>
	 * @param childClone 子クローンオブジェクト
	 */
	public void setChildClone(Clone childClone) {
		this.childClone = childClone;
	}

	/**
	 * <p>クローン分類の取得</p>
	 * @return 分類情報
	 */
	public int getCategory() {
		return category;
	}

	/**
	 *  <p>クローン分類の取得</p>
	 * @return 分類情報
	 */
	public String getCategoryString() {
		String str = null;
		switch (category) {
		case ADDED:
			str = "ADDED";
			break;
		case DELETED:
			str = "DELETED";
			break;
		case MODIFIED:
			str = "MODIFIED";
			break;
		case MOVED:
			str = "MOVED";
			break;
		case STABLE:
			str = "STABLE";
			break;
		case DELETE_MODIFIED:
			str = "DELETE_MODIFIED";
		}
		return str;
	}

	/**
	 * <p>クローン分類の設定</p>
	 * @param category 分類情報
	 */
	public void setCategory(int category) {
		this.category = category;
	}

	/**
	 * <p>クローンが属するクローンセットの取得</p>
	 * @return CloneSetオブジェクト
	 */
	public CloneSet getCloneSet() {
		return cloneSet;
	}

	/**
	 * <p>クローンが属するクローンセットの設定</p>
	 * @param cloneSet CloneSetオブジェクト
	 */
	public void setCloneSet(CloneSet cloneSet) {
		this.cloneSet = cloneSet;
	}

	/**
	 * <p>クローンが属するソースファイルの取得</p>
	 * @return SourceFileオブジェクト
	 */
	public SourceFile getFile() {
		return file;
	}

	/**
	 * <p>クローンが属するソースファイルの設定</p>
	 * @param file SourceFileオブジェクト
	 */
	public void setFile(SourceFile file) {
		this.file = file;
	}

	/**
	 * <p>出力用IDの取得</p>
	 * @return 出力用ID
	 */
	public int getOutputId() {
		return outputId;
	}

	/**
	 * <p>出力用IDの設定</p>
	 * @param outputId 出力用ID
	 */
	public void setOutputId(int outputId) {
		this.outputId = outputId;
	}

	/**
	 * <p>オブジェクトの等価性の判定</p>
	 */
	public boolean equalsForCCFX(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Clone other = (Clone) obj;
		if (endToken != other.endToken)
			return false;
		if (file != other.file)
			return false;
		if (startToken != other.startToken)
			return false;

		if (methodName != null && !methodName.equals(other.getMethodName())) {
			return false;
		}
		return true;
	}

	/**
	 * <p>オブジェクトの等価性の判定</p>
	 */
	public boolean equalsForCloneDetector(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Clone other = (Clone) obj;
		if (endLine != other.endLine)
			return false;
		if (file != other.file)
			return false;
		if (startLine != other.startLine)
			return false;

		if (methodName != null && !methodName.equals(other.getMethodName())) {
			return false;
		}
		return true;
	}

	/**
	 * <p>
	 * 行単位によるオブジェクトの等価性の判定
	 * </p>
	 */
	public boolean equalsByLine(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Clone other = (Clone) obj;
		if (file != other.file)
			return false;
		if (this.startLine == other.startLine
				&& this.endLine == other.endLine)
			return true;
		return false;
	}

	/**
	 * メソッド名の取得
	 * @author m-sano
	 * @return 修飾子付メソッド名(+引数)
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * メソッド名の設定
	 * @author m-sano
	 * @param methodName 修飾子付メソッド名(+引数)
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * 編集距離の取得
	 * @author s-tokui
	 * @return このコード片の後バージョンへの編集操作列 List<ActionPair>
	 */
	public List<ActionPair> getEditOperationList() {
		return editOperationList;
	}

	/**
	 * 編集距離の設定
	 * @author s-tokui
	 * @param このコード片の後バージョンへの編集操作 Pair
	 */
	public void addEditOperationList(ActionPair pair) {
		this.editOperationList.add(pair);
	}

	/**
	 * gumtreediffが生成したASTの取得
	 * @author s-tokui
	 * @return このコード片のAST TreeContext
	 */
	public void setTreeContext(TreeContext src) {
		this.srcTree = src;
	}

	/**
	 * 編集距離の設定
	 * @author s-tokui
	 * @param このコード片のAST TreeContext
	 */
	public TreeContext getTreeContext() {
		return srcTree;
	}

	/**
	 * 親子クローン同士位置類似度
	 * @author s-tokui
	 * @return double localitySimilarity
	 */
	public double getLocationSimilarity() {
		return localitySimilarity;
	}

	/**
	 * 親子クローン同士位置類似度の設定
	 * @author s-tokui
	 * @param double
	 *            localitySimilarity
	 */
	public void setLocationSimilarity(double sim) {
		this.localitySimilarity = sim;
	}
}

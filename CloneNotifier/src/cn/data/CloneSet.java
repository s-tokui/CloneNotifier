package cn.data;

import java.util.ArrayList;

/**
 * <p>クローンセットデータクラス</p>
 * @author y-yuuki
 */
public class CloneSet {

	/** 新プロジェクトにおけるクローンリスト */
	private ArrayList<Clone> newCloneList = new ArrayList<Clone>();

	/** 旧プロジェクトにおけるクローンリスト */
	private ArrayList<Clone> oldCloneList = new ArrayList<Clone>();

	/** クローンセットの追跡における子クローンセットリスト */
	private ArrayList<CloneSet> childCloneSetList = new ArrayList<>();

	/** クローンセットの追跡における親クローンセットリスト */
	private ArrayList<CloneSet> parentCloneSetList = new ArrayList<>();

	/** 出力用ID */
	private int outputId;

	/** クローンセットID. CloneDetectorの時は新プロジェクトにおけるクローンセットID. */
	private int id = NULL;

	/**
	 * 旧プロジェクトにおけるクローンセットID.
	 * CloneDetector用.
	 * @author m-sano
	 */
	private int oldId = NULL;

	private int category = NULL;
	private int divcategory = NULL;

	//メトリクス
	private int LEN = NULL;
	private int POP = NULL;
	private int NIF = NULL;
	private int RAD = NULL;
	private double RNR = NULL;
	private int TKS = NULL;
	private int LOOP = NULL;
	private int COND = NULL;
	private int McCabe = NULL;

	/**
	 * <p>編集距離から算出した危険度</p>
	 * @author s-tokui
	 */
	private double RISK = 0;

	/** <p>初期状態</p> */
	public final static int NULL = -1;


	/** <p>Changed Clone Set</p> */
	public final static int CHANGED = 1;

	/** <p>New Clone</p> */
	public final static int NEW = 2;

	/** <p>Deleted Clone</p> */
	public final static int DELETED = 3;

	/** <p>Stable Clone Set</p> */
	public final static int STABLE = 4;

	/** <p>Division Category of Changed Clone Set</p> */
	/// XXX: SHIFTは使用不可
	public final static int SHIFT = 5;
	public final static int ADD = 6;
	public final static int SUBTRACT = 7;
	public final static int CONSISTENT = 8;
	public final static int INCONSISTENT = 50;

	/** <p>Labels of Division Category of Changed Clone Set</p> */
	private boolean addLabel = false;
	private boolean subtractLabel = false;
	private boolean shiftLabel = false;
	private boolean inconsistentLabel = false;
	private boolean consistentLabel = false;

	/**
	 * クローンセットが同一かどうか調べる.
	 * CloneDetector用.
	 * @param mName 比較するクローンセットの先頭クローンのメソッド名
	 * @param fName 比較するクローンセットの先頭クローンのファイル名
	 * @return
	 */
	public boolean equalsForCloneDetector(String mName, String fName) {
		if (!this.getMethodNameForCloneDetector().equals(mName)) {
			return false;
		}

		if (!this.getFileNameForCloneDetector().equals(fName)) {
			return false;
		}
		return true;
	}

	/**
	 * クローンセットが同一かどうか調べる.
	 * CloneDetector用.
	 * @return
	 */
	public boolean equalsForCloneDetector(CloneSet other) {
		System.err.println(this.getMethodNameForCloneDetector() + " : " + other.getMethodNameForCloneDetector());
		if (!this.getMethodNameForCloneDetector().equals(other.getMethodNameForCloneDetector())) {
			return false;
		}

		if (!this.getFileNameForCloneDetector().equals(other.getFileNameForCloneDetector())) {
			return false;
		}
		return true;
	}

	/**
	 * このクローンセットの先頭に来るクローンのメソッド名を返す.
	 * @return
	 */
	private String getMethodNameForCloneDetector() {
		if (newCloneList.size() > 0) {
			return newCloneList.get(0).getMethodName();
		} else if (oldCloneList.size() > 0) {
			return oldCloneList.get(0).getMethodName();
		}
		return null;
	}

	/**
	 * このクローンセットの先頭に来るクローンを含むファイル名を返す.
	 * @return
	 */
	private String getFileNameForCloneDetector() {
		if (newCloneList.size() > 0) {
			return newCloneList.get(0).getFile().getName();
		} else if (oldCloneList.size() > 0) {
			return oldCloneList.get(0).getFile().getName();
		}
		return null;
	}

	/**
	 * <p>新バージョンのクローンリストの取得</p>
	 * @return クローンリスト
	 */
	public ArrayList<Clone> getNewCloneList() {
		return newCloneList;
	}

	/**
	 * <p>旧バージョンのクローンリストの取得</p>
	 * @return クローンリスト
	 */
	public ArrayList<Clone> getOldCloneList() {
		return oldCloneList;
	}

	/**
	 * <p>子クローンセットリストの取得</p>
	 * @return クローンセットリスト
	 */
	public ArrayList<CloneSet> getChildCloneSetList() {
		return childCloneSetList;
	}

	/**
	 * <p>親クローンセットリストの取得</p>
	 * @return クローンセットリスト
	 */
	public ArrayList<CloneSet> getParentCloneSetList() {
		return parentCloneSetList;
	}

	/**
	 * <p>クローンセットIDの取得</p>
	 * @return クローンセットID
	 */
	public int getId() {
		return id;
	}

	/**
	 * <p>クローンセットIDの設定</p>
	 * @param id　クローンセットID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * <p>クローンセット分類の設定</p>
	 * @param category 分類情報
	 */
	public void setCategory(int category) {
		this.category = category;
	}

	/**
	 * <p>クローンセット分類の取得</p>
	 * @return 分類情報
	 */
	public int getCategory() {
		return category;
	}

	/**
	 * <p>Changedクローンセットの細分類</p>
	 * @param divcategory ADD | SUBTRACT | SHIFT | INCONSISTENT | CONSISTENT
	 */
	public void setDivCategory(int divcategory) {
		switch (divcategory) {
		case ADD:
			if (!addLabel) {
				this.divcategory += divcategory;
			}
			addLabel = true;
			break;

		case SUBTRACT:
			if (!subtractLabel) {
				this.divcategory += divcategory;
			}
			subtractLabel = true;
			break;

		case SHIFT:
			if (!shiftLabel) {
				this.divcategory += divcategory;
			}
			shiftLabel = true;
			break;

		case INCONSISTENT:
			if (!inconsistentLabel) {
				this.divcategory += divcategory;
			}
			inconsistentLabel = true;
			break;

		case CONSISTENT:
			if (!consistentLabel) {
				this.divcategory += divcategory;
			}
			consistentLabel = true;
			break;
		}
	}

	/**
	 * <p>Changedクローンセットの細分類</p>
	 * @return int divcategory
	 */
	public int getDivCategory() {
		return divcategory;
	}

	/**
	 * <p>Changedクローンセットの細分類 addLabel 取得</p>
	 * @return boolean addLabel
	 */
	public boolean getAddLabel() {
		return addLabel;
	}

	/**
	 * <p>Changedクローンセットの細分類 subtractLabel 取得</p>
	 * @return boolean subtractLabel
	 */
	public boolean getSubtractLabel() {
		return subtractLabel;
	}

	/**
	 * <p>Changedクローンセットの細分類 shiftLabel 取得</p>
	 * @return boolean shiftLabel
	 */
	public boolean getShiftLabel() {
		return shiftLabel;
	}

	/**
	 * <p>Changedクローンセットの細分類 inconsistentLabel 取得</p>
	 * @return boolean inconsistentLabel
	 */
	public boolean getInconsistentLabel() {
		return inconsistentLabel;
	}

	/**
	 * <p>Changedクローンセットの細分類 consistentLabel 取得</p>
	 * @return boolean consistentLabel
	 */
	public boolean getConsistentLabel() {
		return consistentLabel;
	}

	/**
	 *  <p>クローン分類の取得</p>
	 * @return 分類情報
	 */
	public String getCategoryString() {
		String str = null;
		switch (category) {
		case NEW:
			str = "NEW";
			break;

		case DELETED:
			str = "DELETED";
			break;

		case CHANGED:
			str = "CHANGED";
			if (addLabel)
				str = str + "(ADD)";
			if (subtractLabel)
				str = str + "(SUBTRACT)";
			if (shiftLabel)
				str = str + "(SHIFT)";
			if (consistentLabel)
				str = str + "(CONSISTRNT)";
			if (inconsistentLabel)
				str = str + "(INCONSISTENT)";
			break;

		case STABLE:
			str = "STABLE";
		}
		return str;
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
	 * <p>LENメトリクス値の取得</p>
	 * @return LENメトリクス値
	 */
	public int getLEN() {
		return LEN;
	}

	/**
	 * <p>LENメトリクス値の設定</p>
	 * @param len LENメトリクス値
	 */
	public void setLEN(int LEN) {
		this.LEN = LEN;
	}

	/**
	 * <p>POPメトリクス値の取得</p>
	 * @return POPメトリクス値
	 */
	public int getPOP() {
		return POP;
	}

	/**
	 * <p>POPメトリクス値の設定</p>
	 * @param pOP POPメトリクス値
	 */
	public void setPOP(int pOP) {
		POP = pOP;
	}

	/**
	 * <p>NIFメトリクス値の取得</p>
	 * @return NIFメトリクス値
	 */
	public int getNIF() {
		return NIF;
	}

	/**
	 * <p>NIFメトリクス値の設定</p>
	 * @param nIF NIFメトリクス値
	 */
	public void setNIF(int nIF) {
		NIF = nIF;
	}

	/**
	 * <p>RADメトリクス値の取得</p>
	 * @return RADメトリクス値
	 */
	public int getRAD() {
		return RAD;
	}

	/**
	 * <p>RADメトリクス値の設定</p>
	 * @param rAD RADメトリクス値
	 */
	public void setRAD(int rAD) {
		RAD = rAD;
	}

	/**
	 * <p>RNRメトリクス値の取得</p>
	 * @return RNRメトリクス値
	 */
	public double getRNR() {
		return RNR;
	}

	/**
	 * <p>RNRメトリクス値の設定</p>
	 * @param rNR RNRメトリクス値
	 */
	public void setRNR(double rNR) {
		RNR = rNR;
	}

	/**
	 * <p>TKSメトリクス値の取得</p>
	 * @return TKSメトリクス値
	 */
	public int getTKS() {
		return TKS;
	}

	/**
	 * <p>TKSメトリクス値の設定</p>
	 * @param tKS TKSメトリクス値
	 */
	public void setTKS(int tKS) {
		TKS = tKS;
	}

	/**
	 * <p>LOOPメトリクス値の取得</p>
	 * @return LOOPメトリクス値
	 */
	public int getLOOP() {
		return LOOP;
	}

	/**
	 * <p>LOOP値の設定</p>
	 * @param lOOP LOOPメトリクス値
	 */
	public void setLOOP(int lOOP) {
		LOOP = lOOP;
	}

	/**
	 * <p>CONDメトリクス値の取得</p>
	 * @return CONDメトリクス値
	 */
	public int getCOND() {
		return COND;
	}

	/**
	 * <p>CONDメトリクス値の設定</p>
	 * @param cOND CONDメトリクス値
	 */
	public void setCOND(int cOND) {
		COND = cOND;
	}

	/**
	 * <p>McCabeメトリクス値の取得</p>
	 * @return McCabeメトリクス値
	 */
	public int getMcCabe() {
		return McCabe;
	}

	/**
	 * <p>McCabeメトリクス値の設定</p>
	 * @param mcCabe McCabeメトリクス値
	 */
	public void setMcCabe(int mcCabe) {
		McCabe = mcCabe;
	}

	/**
	 * 旧プロジェクトのクローンセットID.
	 * CloneDetector用
	 * @author m-sano
	 * @return
	 */
	public int getOldId() {
		return oldId;
	}

	/**
	 * 旧プロジェクトのクローンセットIDの設定.
	 * CloneDetector用
	 * @author m-sano
	 * @param oldId
	 */
	public void setOldId(int oldId) {
		this.oldId = oldId;
	}

	/**
	 * <p>RISKメトリクス値の取得</p>
	 * @author s-tokui
	 * @return RISKメトリクス値
	 */
	public double getRisk() {
		return RISK;
	}

	/**
	 * <p>RISKメトリクス値の設定</p>
	 * @author s-tokui
	 * @param RISKメトリクス値
	 */
	public void setRisk(double risk) {
		RISK = risk;
	}

	/**
	 * <p>
	 * メトリクスをコピー
	 * </p>
	 * 
	 * @author s-tokui
	 * @param cloneSet
	 */
	public void copyMetrics(CloneSet cloneSet) {
		this.LEN = cloneSet.getLEN();
		this.POP = cloneSet.getPOP();
		this.NIF = cloneSet.getNIF();
		this.RAD = cloneSet.getRAD();
		this.RNR = cloneSet.getRNR();
		this.TKS = cloneSet.getTKS();
		this.LOOP = cloneSet.getLOOP();
		this.COND = cloneSet.getCOND();
		this.McCabe = cloneSet.getMcCabe();
	}

}

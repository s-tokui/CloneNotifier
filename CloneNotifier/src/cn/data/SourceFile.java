package cn.data;

import java.util.ArrayList;
import java.util.Iterator;

import cn.Logger;



/**
 * ソースファイルデータクラス
 * @author y-yuuki
 */
public class SourceFile {

	/** ファイルID */
	private int id = NULL;

	/** ファイル名 */
	private String name = null;

	/** 最新プロジェクトにおけるパス */
	private String newPath = null;

	/** 旧プロジェクトにおけるパス */
	private String oldPath = null;

	/** ファイルの状態 */
	private int state = NULL;

	/** 新バージョンにおける, このファイルに属するクローン */
	private ArrayList<Clone> newCloneList = new ArrayList<Clone>();

	/** 旧バージョンにおける, このファイルに属するクローン */
	private ArrayList<Clone> oldCloneList = new ArrayList<Clone>();

	/** 新バージョンで追加されたコード行 */
	private ArrayList<Integer> addedCodeList = new ArrayList<Integer>();

	/** 新バージョンで削除されたコード行 */
	private ArrayList<Integer> deletedCodeList = new ArrayList<Integer>();

	/**
	 * 旧バージョンのメソッドリスト
	 * @author m-sano
	 */
	private ArrayList<Method> oldMethodList = new ArrayList<Method>();

	/**
	 * 新バージョンのメソッドリスト
	 * @author m-sano
	 */
	private ArrayList<Method> newMethodList = new ArrayList<Method>();

	/**
	 * <p>ソースファイルIDの取得</p>
	 * @return ソースファイルID
	 */
	public int getId() {
		return id;
	}

	/**
	 * <p>ソースファイルIDの設定</p>
	 * @param name ソースファイルID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * <p>ソースファイル名の取得</p>
	 * @return ソースファイル名
	 */
	public String getName() {
		return name;
	}

	/**
	 * <p>ソースファイル名の設定</p>
	 * @param name ソースファイル名
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * <p>新バージョンソースファイルパスの取得</p>
	 * @return　ファイルパス
	 */
	public String getNewPath() {
		return newPath;
	}

	/**
	 * <p>新バージョンソースファイルパスの設定</p>
	 * @param newPath ファイルパス
	 */
	public void setNewPath(String newPath) {
		this.newPath = newPath;
	}

	/**
	 * <p>旧バージョンソースファイルパスの取得</p>
	 * @return　ファイルパス
	 */
	public String getOldPath() {
		return oldPath;
	}

	/**
	 * <p>旧バージョンソースファイルパスの設定</p>
	 * @param oldPath ファイルパス
	 */
	public void setOldPath(String oldPath) {
		this.oldPath = oldPath;
	}

	/**
	 * <p>新バージョンソースファイルに含まれるクローンリストの取得</p>
	 * @return クローンリスト
	 */
	public ArrayList<Clone> getNewCloneList() {
		return newCloneList;
	}

	/**
	 * <p>新バージョンソースファイルに含まれるクローンリストの設定</p>
	 * @param newCloneList クローンリスト
	 */
	public void setNewCloneList(ArrayList<Clone> newCloneList) {
		this.newCloneList = newCloneList;
	}

	/**
	 * <p>旧バージョンソースファイルに含まれるクローンリストの取得</p>
	 * @return　クローンリスト
	 */
	public ArrayList<Clone> getOldCloneList() {
		return oldCloneList;
	}

	/**
	 * <p>旧バージョンソースファイルに含まれるクローンリストの取得</p>
	 * @return　クローンリスト
	 */
	public void initCloneList() {
		newCloneList = new ArrayList<Clone>();
		oldCloneList = new ArrayList<Clone>();
	}

	/**
	 * <p>旧バージョンソースファイルに含まれるクローンリストの設定</p>
	 * @param oldCloneList クローンリスト
	 */
	public void setOldCloneList(ArrayList<Clone> oldCloneList) {
		this.oldCloneList = oldCloneList;
	}

	/**
	 * <p>追加コードリストの取得</p>
	 * @return コードリスト
	 */
	public ArrayList<Integer> getAddedCodeList() {
		return addedCodeList;
	}

	/**
	 * <p>削除コードリストの取得</p>
	 * @return コードリスト
	 */
	public ArrayList<Integer> getDeletedCodeList() {
		return deletedCodeList;
	}

	/** <p>初期状態</p> */
	public final static int NULL = -1;

	/** <p>存続ファイル</p> */
	public final static int NORMAL = 0;

	/** <p>追加ファイル</p> */
	public final static int ADDED = 1;

	/** <p>削除ファイル</p> */
	public final static int DELETED = 2;

	/**
	 * <p>ソースファイル状態の取得</p>
	 * @return 状態
	 */
	public int getState() {
		return state;
	}

	/**
	 * <p>ソースファイル状態の設定</p>
	 * @param state 状態
	 */
	public void setState(int state) {
		this.state = state;
	}

	/**
	 * <p>クローンリストを行番号順で整列</p>
	 */
	public void sortCloneListbyLine() {
		sortCloneListbyLine(newCloneList);
		sortCloneListbyLine(oldCloneList);
	}

	/**
	 * <p>クローンリストを行番号順で整列</p>
	 * @param cloneList クローンリスト
	 */
	private void sortCloneListbyLine(ArrayList<Clone> cloneList) {
		for(int i = 0; i < cloneList.size(); i++) {
			for (int j = i; j > 0 && cloneList.get(j).getStartLine() < cloneList.get(j - 1).getStartLine(); j--) {
				Clone tmp = cloneList.get(j);
				cloneList.set(j,cloneList.get(j-1));
				cloneList.set(j-1,tmp);
			}
		}
	}
	
	/**
	 * <p>クローンリストをトークン順で整列</p>
	 */
	public void sortCloneListbyToken() {
		sortCloneListbyToken(newCloneList);
		sortCloneListbyToken(oldCloneList);
	}

	/**
	 * <p>クローンリストをトークン順で整列</p>
	 * @param cloneList クローンリスト
	 */
	private void sortCloneListbyToken(ArrayList<Clone> cloneList) {
		for(int i = 0; i < cloneList.size(); i++) {
			for(int j = i; j > 0 && cloneList.get(j).getStartToken() < cloneList.get(j-1).getStartToken(); j--) {
				Clone tmp = cloneList.get(j);
				cloneList.set(j,cloneList.get(j-1));
				cloneList.set(j-1,tmp);
			}
		}
	}

	/**
	 * <p>クローンリストをメソッド名順で整列</p>
	 * @author m-sano
	 */
	public void sortCloneListbyMethod() {
		sortCloneListbyMethod(newCloneList);
		sortCloneListbyMethod(oldCloneList);
	}

	/**
	 * <p>クローンリストをメソッド名順で整列</p>
	 * @author m-sano
	 * @param cloneList クローンリスト
	 */
	private void sortCloneListbyMethod(ArrayList<Clone> cloneList) {
		for(int i = 0; i < cloneList.size(); i++) {
			for(int j = i; j > 0 && cloneList.get(j).getMethodName().compareTo(cloneList.get(j-1).getMethodName()) < 0; j--) {
				Clone tmp = cloneList.get(j);
				cloneList.set(j, cloneList.get(j-1));
				cloneList.set(j-1, tmp);
			}
		}
	}

	/**
	 * 旧バージョンのメソッドリストを取得する
	 * @return 旧バージョンのメソッドリスト
	 */
	public ArrayList<Method> getOldMethodList() {
		return oldMethodList;
	}

	/**
	 * 旧バージョンのメソッドリストを設定する
	 * @param oldMethodList 旧バージョンのメソッドリスト
	 */
	public void setOldMethodList(ArrayList<Method> oldMethodList) {
		this.oldMethodList = oldMethodList;
	}

	/**
	 * 新バージョンのメソッドリストを取得する
	 * @return 新バージョンのメソッドリスト
	 */
	public ArrayList<Method> getNewMethodList() {
		return newMethodList;
	}

	/**
	 * 新バージョンのメソッドリストを設定する
	 * @param newMethodList 新バージョンのメソッドリスト
	 */
	public void setNewMethodList(ArrayList<Method> newMethodList) {
		this.newMethodList = newMethodList;
	}

	/**
	 * このファイル中のメソッドを取得する.
	 * Class.Subclass.Method(Type param,Type param)
	 * @param modifiedName 修飾子 + メソッド名(Cはファイル名)
	 * @param methodName メソッド名
	 * @param params 引数部. ()不要 (Cは未使用)
	 * @param isNew trueなら新バージョンのメソッド, falseなら旧バージョンのメソッドを探索する
	 * @param forJava java用ならtrue
	 * @return フルネームに対応するMethod
	 * @author m-sano
	 */
	public Method getMethod(String modifiedName, String methodName, String params, boolean isNew, boolean forJava) {
		Iterator<Method> it;

		Logger.writeln("<SourceFile.getMethod> " + name, Logger.DEBUG);

		if(isNew) {
			it = newMethodList.iterator();
		} else {
			it = oldMethodList.iterator();
		}

		Logger.writeln("<SourceFile.getMethod> search - " + modifiedName + params, Logger.DEBUG);

		while(it.hasNext()) {
			Method md = it.next();
			Logger.writeln("<SourceFile.getMethod> equal check - " + md.getModifiedName() + md.getParams(), Logger.DEBUG);
			if(md.equalsMethod(modifiedName, methodName, params, forJava)) {
				Logger.writeln("<SourceFile.getMethod> return", Logger.DEBUG);
				return md;
			}
			Logger.writeln("<SourceFile.getMethod> next loop", Logger.DEBUG);
		}
		Logger.writeln("<SourceFile.getMethod> return null", Logger.DEBUG);
		return null;
	}



	/**
	 * メソッドリストをクリアする.
	 * ヒープ領域不足の一時しのぎ.
	 */
	/*
	@Deprecated
	public void clearMethodList() {
		newMethodList = new ArrayList<Method>();
		oldMethodList = new ArrayList<Method>();
	}
	*/
}

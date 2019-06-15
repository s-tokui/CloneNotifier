package cn.data;

import cn.Logger;

/**
 * メソッドデータ.
 * CloneDetector用.
 * @author m-sano
 */
public class Method {

	/** メソッド名 */
	private String name = null;

	/** 修飾子付メソッド名 */
	private String modifiedName = null;

	/** 引数 (Type param,Type param) の形式. */
	private String params = null;

	/** 開始行番号. static や private トークンのある位置 */
	private int startLine = -1;

	/** 開始列番号. static や private トークンのある位置 */
	private int startColumn = -1;

	/** 終了行番号. } の位置 */
	private int endLine = -1;

	/** 終了列番号. } の位置 */
	private int endColumn = -1;

	/**
	 * 引数シグネチャを取得する
	 * @return 引数
	 */
	public String getParams() {
		return params;
	}

	/**
	 * 引数シグネチャを設定する
	 * @param params 引数
	 */
	public void setParams(String params) {
		this.params = params;
	}

	/**
	 * 修飾付きメソッド名を取得する
	 * @return 修飾付きメソッド名
	 */
	public String getModifiedName() {
		return modifiedName;
	}

	/**
	 * 修飾付きメソッド名を設定する
	 * @param modifiedName 修飾付きメソッド名
	 */
	public void setModifiedName(String modifiedName) {
		this.modifiedName = modifiedName;
	}

	/**
	 * メソッド名を取得する
	 * @return メソッド名
	 */
	public String getName() {
		return name;
	}

	/**
	 * メソッド名を設定する
	 * @param name メソッド名
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * メソッドの開始行を取得する
	 * @return 開始行番号
	 */
	public int getStartLine() {
		return startLine;
	}

	/**
	 * メソッドの開始行を設定する
	 * @param startLine 開始行番号
	 */
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}

	/**
	 * メソッドの開始列を取得する
	 * @return 開始列番号
	 */
	public int getStartColumn() {
		return startColumn;
	}

	/**
	 * メソッドの開始列を設定する
	 * @param startColumn 開始列番号
	 */
	public void setStartColumn(int startColumn) {
		this.startColumn = startColumn;
	}

	/**
	 * メソッドの終了行を取得する
	 * @return 終了行番号
	 */
	public int getEndLine() {
		return endLine;
	}

	/**
	 * メソッドの終了行を設定する
	 * @param endLine 終了行番号
	 */
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}

	/**
	 * メソッドの終了列を取得する
	 * @return 終了列番号
	 */
	public int getEndColumn() {
		return endColumn;
	}

	/**
	 * メソッドの終了列を設定する
	 * @param endColumn 終了列番号
	 */
	public void setEndColumn(int endColumn) {
		this.endColumn = endColumn;
	}

	/**
	 * 仮引数名等を取り除いた引数を返す.
	 * Eclipse上で同じシグネチャと見なされるようにする.
	 * 型指定 <T> も取り除かれる
	 *
	 * TODO 関数ポインタの引数がさらに関数ポインタになる場合, 引数整形部を分離して再帰構造にしないと駄目かもしれない
	 * @oaram forJava trueならJava用
	 * @return
	 */
	public String getParamTypeOnly(boolean forJava) {
		String paramLine = getParams();

		// () の除去
		paramLine = paramLine.substring(1, paramLine.length()-1);

		Logger.writeln("<Method.getParamTypeOnly> " + paramLine, Logger.DEBUG);

		return "(" + arrangeParameter(paramLine, forJava) + ")";
	}



	/**
	 * コードから抽出したままの引数部を整形する.
	 * @param params 左右端の () を含まない引数部
	 * @param forJava
	 * @return
	 */
	public static String arrangeParameter(String paramLine, boolean forJava) {
		String tempLine = paramLine;

		Logger.writeln("<Method.arrangeParameter> <T> exclude", Logger.DEBUG);

		// <T>の除去(Java)
		if(forJava) {
			while(tempLine.indexOf("<") > -1) {
				int start = tempLine.indexOf("<");
				int end = tempLine.indexOf(">");
				tempLine = tempLine.substring(0, start) + tempLine.substring(end+1, tempLine.length());
			}
		}

		Logger.writeln("<Method.arrangeParameter> " + tempLine, Logger.DEBUG);

		String[] params = tempLine.split(",");
		String newParam = "";
		for(int i = 0; i < params.length; i++) {
			newParam = newParam + arrangeOneParam(params[i], forJava);

			if(i+1 < params.length) {
				newParam = newParam + ",";
			}
		}

		Logger.writeln("<Method.arrangeParameter> return", Logger.DEBUG);

		return newParam;
	}


	/**
	 * 1つの引数部を整形する.
	 * すなわち, 型名等の最低限の情報以外を削る.
	 * @param param
	 * @return
	 */
	private static String arrangeOneParam(String param, boolean forJava) {
		String newParam = "";

		// 関数ポインタ対策
		if(!forJava && param.contains("(")) {
			String[] funcp = param.split("\\(");
			newParam = newParam + funcp[0] + "(*)";

			String fpprm = funcp[2].substring(0, funcp[2].length()-1);	// ")" を除去
			String[] fpParams = fpprm.split(",");
			newParam = newParam + "(";

			for(int i = 0; i < fpParams.length; i++) {
				newParam = newParam + arrangeOneParam(fpParams[i], false);

				if(i+1 < fpParams.length) {
					newParam = newParam + ",";
				}
			}
			return newParam + ")";
		}

		String[] temp;
		if(forJava) {
			temp = param.split(" ");
		} else {
			temp = param.split("[ *]");
		}

		int length = temp.length-1;

		// void用
		if(!forJava && length == 0) {
			length = 1;
		}

		// 接尾辞タイプの配列, かつ, [] と変数名の間にスペースがある場合の対策
		if(temp[temp.length-1].equals("[]")) {
			--length;
		}

		// 配列 [] の間にスペースがあった場合等の対策
		for(int k = 0; k < length; k++) {
			if(!forJava) {
				if(!temp[k].equals("const")) {
					newParam = newParam + temp[k];
				}
			} else {
				if(!temp[k].equals("final")) {
					newParam = newParam + temp[k];
				}
			}
		}

		// ポインタ対策
		if(!forJava && param.contains("*")) {
			newParam = newParam + "*";
		}

		// 配列接尾辞タイプ対策
		if(temp[temp.length-1].endsWith("[]")) {
			newParam = newParam + "[]";
		}

		return newParam;
	}

	/**
	 * 修飾子と引数付きのメソッド名が一致するか比較する
	 * Class.Subclass.Method(Type param,Type param)
	 * @param modifiedName 修飾子 + メソッド名(Cはファイル名)
	 * @param methodName メソッド名
	 * @param params 引数部. ()不要 (Cは未使用)
	 * @param forJava trueならJava用, それ以外はc用
	 * @return 一致したらtrue, それ以外ならfalse
	 */
	public boolean equalsMethod(String modifiedName, String methodName, String params, boolean forJava) {

		// Cはオーバーロードが無いので引数判定は不要.
		if(!forJava) {
			if(!getModifiedName().equals(modifiedName)) {
				return false;
			}

			if(!getName().equals(methodName)) {
				return false;
			}
		} else {

			//System.err.println(modifiedName + " " + methodName + " " + arrangeParameter(params, forJava));
			//System.err.println(getModifiedName() + " " + getName() + " " + getParamTypeOnly(forJava));

			Logger.writeln("<Method.equalsMethod> modified name for java", Logger.DEBUG);

			if(!getModifiedName().equals(modifiedName)) {
				return false;
			}

			Logger.writeln("<Method.equalsMethod> method name for java", Logger.DEBUG);

			if(!getName().equals(methodName)) {
				return false;
			}

			Logger.writeln("<Method.equalsMethod> params for java", Logger.DEBUG);

			// paramsの整形がいる
			if(!getParamTypeOnly(forJava).equals("(" + arrangeParameter(params, forJava) + ")")) {
				return false;
			}

			Logger.writeln("<Method.equalsMethod> params for java END", Logger.DEBUG);
		}
		return true;
	}
}

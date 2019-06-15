package extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.Logger;
import cn.data.Method;
import extractor.data.Token;
import extractor.data.TokenType;



/**
 * Javaで記述されたソースコードの字句解析を行う.
 * <p>
 * メソッド宣言の特定が目的のため,
 * 字句は "名前", "数値", "その他記号" 程度にしか分けてない.
 * @author m-sano
 */
public class JavaMethodExtractor {

	/**
	 * 識別子名.
	 * @see {@link StreamTokenizer#TT_WORD}
	 */
	public static final int TT_WORD = StreamTokenizer.TT_WORD;

	/**
	 * 数値.
	 * @see {@link StreamTokenizer#TT_NUMBER}
	 */
	public static final int TT_NUMBER = StreamTokenizer.TT_NUMBER;

	/**
	 * End of File.
	 * @see {@link StreamTokenizer#TT_EOF}
	 */
	public static final int TT_EOF = StreamTokenizer.TT_EOF;

	/**
	 * 改行文字. '\n' と同じ.
	 * @see {@link StreamTokenizer#TT_EOL}
	 */
	public static final int TT_LF = StreamTokenizer.TT_EOL;

	/** ドット. */
	public static final int TT_DOT = '.';

	/** 復帰文字. */
	public static final int TT_CR = '\r';

	/** タブ文字. */
	public static final int TT_TAB = '\t';

	/** 半角スペース. */
	public static final int TT_SPACE = ' ';

	/** 予約語リスト. Java8版. */
	public static final ArrayList<String> keywords = new ArrayList<String>(Arrays.asList(
		"abstract",	"assert",
		"boolean",	"break",
		"byte",		"case",
		"catch",	"char",
		"class",	"const",
		"continue",	"default",
		"do",		"double",
		"else",		"enum",
		"extends",	"final",
		"finally",	"float",
		"for",		"if",
		"goto",		"implements",
		"import",	"instanceof",
		"int",		"interface",
		"long",		"native",
		"new",		"package",
		"private",	"protected",
		"public",	"return",
		"short",	"static",
		"strictfp",	"super",
		"switch",	"synchronized",
		"this",		"throw",
		"throws",	"transient",
		"try",		"void",
		"volatile",	"while"
	));

	/** 対象ソースコード. */
	private File targetFile;

	/**
	 * 字句解析器を生成する.
	 * @param path 対象ソースコードのパス.
	 */
	public JavaMethodExtractor(String path) {
		this.setTargetFile(path);
	}

	/**
	 * 現在の対象ソースコードを取得する.
	 * @return 対象ソースコード.
	 */
	public File getTargetFile() {
		return this.targetFile;
	}

	/**
	 * 解析対象のソースコードを指定する.
	 * @param target 解析対象のソースコード.
	 */
	public void setTargetFile(File target) {
		this.targetFile = target;
	}

	/**
	 * 解析対象のソースコードを指定する.
	 * @param targetPath 解析対象のソースコードのパス.
	 */
	public void setTargetFile(String targetPath) {
		this.setTargetFile(new File(targetPath));
	}



	/**
	 * メソッドの抽出を行う
	 * @return メソッドリスト
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public ArrayList<Method> extractMethod() throws FileNotFoundException, IOException {
		ArrayList<Method> methodList = new ArrayList<Method>();

		LinkedList<Token> tkList = this.tokenizeToToken();
		Iterator<Token> it = tkList.iterator();

		ArrayList<Token> sentence = new ArrayList<Token>();
		Method md = null;
		ArrayList<String> modifier = new ArrayList<String>();
		ArrayList<Integer> classStartBlacket = new ArrayList<Integer>();

		int blacketCnt = -1;
		int mdStartBlacket = -1;

		boolean isStaticInit = false;
		boolean enumMode = false;
		boolean inEnum = false;
		boolean instanceMethod = false;
		boolean isClassInit = false;

		while(it.hasNext()) {
			Token tk = it.next();

			// 修飾子生成用にクラス名を保存
			if(tk.getToken().equals("class") || tk.getToken().equals("interface")) {
				if(it.hasNext()) {
					tk = it.next();
					if(tk.getType() == TokenType.IDENTIFIER) {
						modifier.add(tk.getToken());
						classStartBlacket.add(blacketCnt+1);
						isClassInit = true;
					}
				}
			}

			// クラス/インタフェース名が取得されるまで以降の処理は実施しない
			if(!isClassInit) {
				continue;
			}

			// メソッド宣言の開始部かどうかを判定
			if(tk.getToken().equals("{")) {
				int index = -1;

				// 既にメソッド宣言内ならスルー
				if(md == null && !isStaticInit) {
					// 文中の "(" を探し, その直前が識別子であるかを見る
					Iterator<Token> itStInd = sentence.iterator();
					int k = 0;
					boolean annotation = false;

					while(itStInd.hasNext()) {
						String parentk = itStInd.next().getToken();

						// enum型の宣言だった場合
						// TODO enum内の複数メソッド宣言の区別が困難なため, エラー回避だけに留めている.
						// おそらく, clonedetectorの出力を読み, 検索でEnum列挙しないの宣言に引っ掛かった場合は,
						// メソッド内がタイプ1クローンとして一致するかどうかチェックという流れになる.
						// しかし, Enum内の異なる列挙子間でタイプ1クローンがあるとどうしようもない.
						if(parentk.equals("enum")) {
							enumMode = true;
							break;
						}

						// インスタンス生成のコンストラクタに続けて, 中身を記述している場合
						if(parentk.equals("new")) {
							instanceMethod = true;
							break;
						}

						// enum列挙子のコンストラクタ
						if(enumMode && !inEnum) {
							inEnum = true;
							break;
						}

						// アノテーションが存在する
						if(parentk.equals("@")) {
							k++;

							// アノテーション名
							parentk = itStInd.next().getToken();
							k++;

							parentk = itStInd.next().getToken();
							annotation = true;
						}

						if(parentk.equals("(")) {
							if(annotation) {

								// ")" までスキップ
								while(!parentk.equals(")") && itStInd.hasNext()) {
									k++;
									parentk = itStInd.next().getToken();
								}
								annotation = false;

								// アノテーション引数内の変なところに "{" があったらこの回は廃棄
								if(!itStInd.hasNext()) {
									break;
								}
							} else {
								index = k;
								break;
							}
						}

						// ここに到達しているなら, 必ずアノテーション終了のはず
						annotation = false;
						k++;
					}

					if(index != -1) {

						//System.err.println(tk.getToken());	// テスト用

						if(index > 0 && sentence.get(index-1).getType() == TokenType.IDENTIFIER) {
							// メソッド宣言開始部だった場合
							md = new Method();
							md.setName(sentence.get(index-1).getToken());
							md.setStartLine(sentence.get(0).getLine());
							md.setStartColumn(sentence.get(0).getColumn());

							// 修飾子の処理
							String mod = null;
							Iterator<String> itMod = modifier.iterator();
							while(itMod.hasNext()) {
								if(mod == null) {
									mod = itMod.next();
								} else {
									mod = mod.concat("." + itMod.next());
								}
							}
							md.setModifiedName(mod + "." + md.getName());

							// 引数の整形
							int i = index+1;
							String paramStr = "(";


							// テスト用
							/*
							Iterator<Token> testit= sentence.iterator();
							while(testit.hasNext()) {
								System.err.print(testit.next().getToken()+ " ");
							}
							System.err.println();
							*/
							// テスト用ここまで

							Logger.writeln("<JavaMethodExtractor> targetFile is " + targetFile.toString(), Logger.DEBUG);

							
							while(!sentence.get(i).getToken().equals(")")) {
								Token paramTk = sentence.get(i);

								// 初回は詰める
								if(i == index+1) {
									paramStr = paramStr.concat(paramTk.getToken());
								} else {

									// <TYPE> は詰める
									if(paramTk.getToken().equals("<")) {
										paramStr = paramStr.concat("<");
										while(!(paramTk = sentence.get(++i)).getToken().equals(">")) {
											paramStr = paramStr.concat(paramTk.getToken());
										}
										paramStr = paramStr.concat(">");

									// 可変長引数 ... と引数名の間をあける
									} else if(paramStr.endsWith("...")) {
										paramStr = paramStr.concat(" " + paramTk.getToken());

									// 名前系は間をあける
									} else if((paramTk.getType() == TokenType.IDENTIFIER || paramTk.getType() == TokenType.RESERVED)
											&& (sentence.get(i-1).getType() == TokenType.IDENTIFIER || sentence.get(i-1).getType() == TokenType.RESERVED
											|| sentence.get(i-1).getToken().equals(">") || sentence.get(i-1).getToken().equals("]"))) {
										paramStr = paramStr.concat(" " + paramTk.getToken());
									} else {
										paramStr = paramStr.concat(paramTk.getToken());
									}
								}
								i++;
							}
							md.setParams(paramStr + ")");

							mdStartBlacket = blacketCnt+1;

							Logger.writeln("<JavaMethodExtractor> Extract " + md.getModifiedName() + md.getParams(), Logger.DEBUG);
						}
					}

				// スタティックイニシャライザ宣言部
				} if(sentence.size() == 1 && sentence.get(0).getToken().equals("static")) {
					isStaticInit = true;
					mdStartBlacket = blacketCnt+1;
				}
				sentence.clear();
				blacketCnt++;

			// メソッド宣言部の終了処理
			} else if(tk.getToken().equals("}")) {

				if(blacketCnt == mdStartBlacket) {
					// メソッド宣言内, かつ, 宣言終了の "}"
					if(md != null) {
						md.setEndLine(tk.getLine());
						md.setEndColumn(tk.getColumn());
						methodList.add(md);

						md = null;
					}

					// staticイニシャライザの終了
					if(isStaticInit) {
						isStaticInit = false;
					}

					mdStartBlacket = -1;
				} else {
					if(instanceMethod) {
						instanceMethod = false;
					}

					if(inEnum) {
						inEnum = false;
					} else if(enumMode) {
						enumMode = false;
					}
				}

				// サブクラスの脱出
				if(classStartBlacket.get(classStartBlacket.size()-1) == blacketCnt) {
					classStartBlacket.remove(classStartBlacket.size()-1);
					modifier.remove(modifier.size()-1);
				}

				sentence.clear();
				blacketCnt--;
			} else if(tk.getToken().equals(";")) {
				sentence.clear();
			} else {
				sentence.add(tk);
			}
		}

		return methodList;
	}



	/**
	 * 現在設定されているソースコードの字句解析を行う.
	 * リスト要素を {@link Token}に置き換えたもの.
	 * @return 解析結果(トークンリスト).
	 * @throws FileNotFoundException 対象ファイルが存在しない.
	 * @throws IOException 入出力エラー.
	 */
	private LinkedList<Token> tokenizeToToken() throws FileNotFoundException, IOException {
		InputStreamReader reader = new InputStreamReader(new FileInputStream(this.targetFile),"JISAutoDetect");

		StreamTokenizer tokenizer = new StreamTokenizer(reader);

		this.setupTokenizer(tokenizer);

		LinkedList<Token> tokenList = new LinkedList<Token>();
		int token;

		int line = 1;
		int column = 1;

		int startLine = -1;
		int startColumn = -1;

		String tkName = null;
		TokenType tkType = TokenType.UNKNOWN;

		while((token = tokenizer.nextToken()) != TT_EOF) {
			switch(token) {
			case TT_WORD:
				tkName = tokenizer.sval;
				if(keywords.contains(tkName)) {
					tkType = TokenType.RESERVED;
				} else if(this.isNumber(tkName)) {
					tkType = TokenType.NUMBER;
				} else {
					tkType = TokenType.IDENTIFIER;
				}
				startLine = line;
				startColumn = column;
				column += tkName.length();
				break;

			case TT_SPACE:
			case TT_TAB:
				column++;
				break;

			case TT_LF:
				line++;
				column = 1;
				break;

			case TT_CR:
				break;

			case '/':	// コメントの処理
				int next = tokenizer.nextToken();
				tokenizer.ordinaryChar('\'');
				tokenizer.ordinaryChar('"');
				column++;
				if(next == '/') {
					// 行末までスキップ
					while((token = tokenizer.nextToken()) != TT_LF && token != TT_EOF);
					line++;
					column = 1;

				} else if(next == '*') {
					// '*/' までスキップ
					while((token = tokenizer.nextToken()) != TT_EOF) {

						if(token == TT_WORD) {
							column += tokenizer.sval.length();
						} else {
							column++;
						}

						if(token == '*') {
							int ntk = tokenizer.nextToken();
							if(ntk == '/') {
								column++;
								break;
							}
							tokenizer.pushBack();
						}

						if(token == TT_LF) {
							line++;
							column = 1;
						}
					}
				} else {
					tokenizer.pushBack();
				}
				tokenizer.quoteChar('\'');
				tokenizer.quoteChar('"');
				break;

			default:
				tkName = String.valueOf(Character.toChars(token));
				tkType = TokenType.SYMBOL;
				startLine = line;
				startColumn = column;
				column++;
			}

			if(tkName != null) {
				Token tk = new Token(tkName, tkType);
				tk.setPositon(startLine, startColumn);
				tokenList.add(tk);

				tkName = null;
			}
		}

		reader.close();

		return tokenList;
	}



	/**
	 * 与えられたトークンが数値トークンかどうか調べる.
	 * 数値トークンは, 先頭が整数であるような TT_WORD とする.
	 * <p>
	 * {@link StreamTokenizer}の数値トークンとは意味が異なる.
	 * @param token 調査するトークン.
	 * @return 数値トークンなら{@code true}, それ以外なら{@false}.
	 */
	private boolean isNumber(String token) {
		String regex = "^[0-9]";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(token);
		return matcher.find();
	}


	/**
	 * 引数の{@link StreamTokenizer}の設定をする.
	 * @param tokenizer {@link StreamTokenizer}.
	 */
	private void setupTokenizer(StreamTokenizer tokenizer) {
		tokenizer.resetSyntax();
		tokenizer.wordChars('0', '9');
		tokenizer.wordChars('a', 'z');
		tokenizer.wordChars('A', 'Z');
		tokenizer.wordChars('_', '_');

		tokenizer.quoteChar('\'');
		tokenizer.quoteChar('"');

		tokenizer.eolIsSignificant(true);	// 改行文字をトークンとして扱う
	}
}

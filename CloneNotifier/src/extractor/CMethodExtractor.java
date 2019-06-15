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
 * Cで記述されたソースコードの字句解析を行う.
 * <p>
 * メソッド宣言の特定が目的のため,
 * 字句は "名前", "数値", "その他記号" 程度にしか分けてない.
 * @author m-sano
 */
public class CMethodExtractor {
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

	/** 予約語C89, C99, C11 の 全44個. */
	public static final ArrayList<String> keywords = new ArrayList<String>(Arrays.asList(
		"_Bool",			"char",				"short",		"int",
		"long",				"signed",			"unsigned",		"float",
		"double",			"_Complex",			"_Imaginary",	"struct",
		"union",			"enum",				"volatile",		"const",
		"restrict",			"auto",				"extern",		"static",
		"register",			"typedef",			"void",			"if",
		"else",				"switch",			"case",			"default",
		"for",				"while",			"do",			"goto",
		"continue",			"break",			"return",		"_Atomic",
		"_Thread_local",	"_Alignas",			"alignof",		"inline",
		"sizeof",			"_Static_assert",	"_Generic",		"_Noreturn"
	));

	/** 対象ソースコード. */
	private File targetFile;

	/**
	 * 字句解析器を生成する.
	 * @param path 対象ソースコードのパス.
	 */
	public CMethodExtractor(String path) {
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

		int blacketCnt = -1;
		int mdStartBlacket = -1;

		int macroLine = -1;

		while(it.hasNext()) {
			Token tk = it.next();

			Logger.writeln("<extractMethod> token " + tk.getToken(), Logger.DEBUG);

			// マクロのスキップ
			if(macroLine > -1) {
				if(macroLine < tk.getLine()) {

					// マクロ続行
					if(sentence.size() > 0 && sentence.get(sentence.size()-1).getToken().equals("\\")) {
						macroLine++;
						sentence.add(tk);
						continue;
					} else {
						macroLine = -1;
						sentence.clear();
						sentence.add(tk);
					}
				} else {
					sentence.add(tk);
					continue;
				}
			}

			// メソッド宣言の開始部かどうかを判定
			if(tk.getToken().equals("{")) {
				int index = -1;

				// 既にメソッド宣言内ならスルー
				if(md == null) {
					// 文中の "(" を探し, その直前が識別子であるかを見る
					Iterator<Token> itStInd = sentence.iterator();
					int k = 0;
					while(itStInd.hasNext()) {
						if(itStInd.next().getToken().equals("(")) {
							index = k;
							break;
						}
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

							// Cはクラスの概念が無いのでファイル名で代用
							md.setModifiedName(targetFile.getName());

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

							Logger.writeln("<CMethodExtractor> targetFile is " + targetFile.toString(), Logger.DEBUG);


							// 最後の ")" を捜索 (関数ポインタ対策)
							int lastParenthIndex = 0;
							for(int l = sentence.size()-1; l >= 0; l--) {
								if(sentence.get(l).getToken().equals(")")) {
									lastParenthIndex = l;
									break;
								}
							}

							while(i < lastParenthIndex) {
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

							Logger.writeln("<CMethodExtractor> Extract " + md.getModifiedName() + md.getParams(), Logger.DEBUG);
						}
					}
				}
				sentence.clear();
				blacketCnt++;

			// メソッド宣言部の終了処理
			} else if(tk.getToken().equals("}")) {

				// テスト用

				/*
				sentence.add(tk);
				Iterator<Token> testit= sentence.iterator();
				while(testit.hasNext()) {
					Token x = testit.next();
					System.err.print(x.getToken()+ " ");
					System.err.print(x.getLine()+ " ");
					System.err.print(blacketCnt+ " == ");
					System.err.print(mdStartBlacket+ " ");
				}
				System.err.println();
				*/

				// テスト用ここまで

				if(blacketCnt == mdStartBlacket) {
					// メソッド宣言内, かつ, 宣言終了の "}"
					if(md != null) {
						md.setEndLine(tk.getLine());
						md.setEndColumn(tk.getColumn());
						methodList.add(md);

						md = null;
					}

					mdStartBlacket = -1;
				}

				sentence.clear();
				blacketCnt--;
			} else if(tk.getToken().equals(";")) {
				sentence.clear();
			} else if(tk.getToken().equals("#")) {
				sentence.clear();
				macroLine = tk.getLine();
				sentence.add(tk);
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

				//System.err.println(tkName + ": " + line);
				break;

			case TT_SPACE:
			case TT_TAB:
				//System.err.println("タブ: " + line);
				column++;
				break;

			case TT_LF:
				line++;
				column = 1;
				//System.err.println("改行: " + line);
				break;

			case TT_CR:
				break;

			case '/':	// コメントの処理
				int next = tokenizer.nextToken();

				// 文字定数にされないようにする
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
					//System.err.println("/*: " + line);
					while((token = tokenizer.nextToken()) != TT_EOF) {
						//System.err.println(token + ": " + line);
						//System.err.println(tokenizer.toString());

						if(token == TT_WORD) {
							column += tokenizer.sval.length();
							//System.err.println(tokenizer.sval + ": " + line);
						} else {
							//System.err.println(String.valueOf(Character.toChars(token)) + ": " + line);
							column++;
						}

						if(token == '*') {
							int ntk = tokenizer.nextToken();
							//System.err.println(String.valueOf(Character.toChars(token)) + ": " + line);
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
					//System.err.println("*/: " + line);
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
				//System.err.println(tkName + ": " + startLine);
				column++;
				break;
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

package extractor.data;


/**
 * トークンを表すデータ構造.
 * @author m-sano
 */
public class Token {

	/** コード中での表記. */
	private String token;

	/** トークンの種別. */
	private TokenType type;

	/**
	 * トークンがある行番号.
	 */
	private int line;

	/**
	 * トークンの最初の文字がある列番号.
	 */
	private int column;

	public Token(String code, TokenType type) {
		this.setToken(code);
		this.setType(type);
	}

	public String getToken() {
		return token;
	}

	public TokenType getType() {
		return type;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setType(TokenType type) {
		this.type = type;
	}


	/**
	 * @return
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * @param line
	 */
	public void setLine(int line) {
		this.line = line;
	}

	/**
	 * @param column
	 */
	public void setColumn(int column) {
		this.column = column;
	}

	/**
	 * @param line
	 * @param column
	 */
	public void setPositon(int line, int column) {
		this.setLine(line);
		this.setColumn(column);
	}

	/**
	 * コード中での表記とトークン種別が一致する場合に true を返す.
	 */
	@Override
	public boolean equals(Object obj) {
		Token t = (Token) obj;

		if(this.getToken().equals(t.getToken()) && this.getType() == t.getType()) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return (this.getToken() + ": " + this.getType().name());
	}
}

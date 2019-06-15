package cn.data;

/**
 * <p>メールアカウントクラス</p>
 * @author y-yuuki
 */
public class Account {

	/** アカウント名 */
	private String name;

	/** パスワード */
	private String pass;

	/**
	 * <p>アカウント名の取得</p>
	 * @return アカウント名
	 */
	public String getName() {
		return name;
	}

	/**
	 * <p>アカウント名の設定</p>
	 * @param name アカウント名
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * <p>パスワードの取得</p>
	 * @return パスワード
	 */
	public String getPass() {
		return pass;
	}

	/**
	 * <p>パスワードの設定</p>
	 * @param pass パスワード
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

}

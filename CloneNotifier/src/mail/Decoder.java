package mail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import cn.data.Account;

/**
 * <p>
 * 暗号復号クラス
 * </p>
 *
 * @author y-yuuki
 */
public class Decoder {

	/**
	 * <p>
	 * アカウント情報のデコード
	 * </p>
	 *
	 * @param keyfile
	 *            キー保存ファイル
	 * @param accountfile
	 *            アカウント保存ファイル
	 * @return アカウント情報
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IOException
	 */
	public Account decode(String keyfile, String accountfile) throws InvalidKeySpecException, NoSuchAlgorithmException,
			IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException, IOException {

		// 秘密鍵の復元
		PrivateKey privateKey = null;
		BufferedReader reader = new BufferedReader(new FileReader(new File(keyfile)));
		String[] tmpKey = reader.readLine().split(",");
		byte[] binary = new byte[tmpKey.length];
		for (int i = 0; i < tmpKey.length; i++) {
			binary[i] = Byte.valueOf(tmpKey[i]);
		}
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(binary);
		privateKey = keyFactory.generatePrivate(keySpec);

		// 復号化
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] name = null;
		byte[] pass = null;
		reader = new BufferedReader(new FileReader(new File(accountfile)));
		String[] tmpName = reader.readLine().split(",");
		String[] tmpPass = reader.readLine().split(",");
		name = new byte[tmpName.length];
		pass = new byte[tmpPass.length];
		for (int i = 0; i < tmpName.length; i++) {
			name[i] = Byte.valueOf(tmpName[i]);
		}
		for (int i = 0; i < tmpPass.length; i++) {
			pass[i] = Byte.valueOf(tmpPass[i]);
		}

		String strName = new String(cipher.doFinal(name));
		String strPass = new String(cipher.doFinal(pass));

		Account account = new Account();
		account.setName(strName);
		account.setPass(strPass);

		return account;
	}
}

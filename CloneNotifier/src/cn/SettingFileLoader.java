package cn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import cn.data.Project;

/**
 * <p>設定ファイル読み込みクラス</p>
 * @author y-yuuki
 */
public class SettingFileLoader {

	/**
	 * <p>設定ファイルの読み込み</p>
	 * @author y-yuuki
	 * @author m-sano
	 * @param settingFile 設定ファイル名
	 * @param project Projectオブジェクト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public static boolean loadSettingFile(String settingFile, Project project) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(settingFile));
			String line;
			while((line = reader.readLine()) != null) {

				// 先頭 "%" はコメント行として扱えるようにしている模様
				if(line.length() > 0 && line.charAt(0) != '%') {

					//プロジェクト名取得
					if(line.contains("PROJECT_NAME:")) {
						project.setName(removeSpace(line.replace("PROJECT_NAME:","")));
					}

					// ツール設定
					if(line.contains("TOOL:")) {
						project.setTool(removeSpace(line.replaceAll("TOOL:", "")));
					}

					// 言語設定
					if(line.contains("LANGUAGE:")) {
						project.setLang(removeSpace(line.replace("LANGUAGE:","")));
					}

					// 作業ディレクトリ設定
					if (line.contains("WORK_DIR:")) {
						project.setWorkDir(removeSpace(line.replace("WORK_DIR:", "")));
					}

					// チェックアウト設定
					if(line.contains("CHECKOUT:")) {
						if(removeSpace(line.replace("CHECKOUT:","")).equals("AUTO")) {
							project.setCheckout(true);
						} else if(removeSpace(line.replace("CHECKOUT:","")).equals("MANUAL")) {
							project.setCheckout(false);
						}
					}

					// チェックアウト用コマンド取得 (チェックアウト有効時)
					if(project.isCheckout() && line.contains("CHECKOUT_CMD:")) {
					    project.setCheckoutCmd(line.replace("CHECKOUT_CMD:",""));
					    project.setOldDir(Def.OLD_PROJ_DIR + "\\" + project.getName());
					}

					// チェックアウト先ディレクトリ (チェックアウト有効時)
					if(project.isCheckout() && line.contains("CHECKOUT_DIR:")) {
					    project.setNewDir(removeSpace(line.replace("CHECKOUT_DIR:","")));
					}

					// 最新バージョンチェックアウトディレクトリ設定 (チェックアウト無効時)
					if(!project.isCheckout() && line.contains("NEW_VERSION:")) {
						project.setNewDir(removeSpace(line.replace("NEW_VERSION:","")));
						if(!(new File(project.getNewDir())).exists()) {
							Logger.writeln("Can't found new version directory.", Logger.ERROR);
							return false;
						}
						// if(!(new File(project.getNewDir()+"\\src")).exists()) {
						// Logger.writeln("Can't found 'src' directory.", Logger.ERROR);
						// return false;
						// }
					}

					// 旧バージョンチェックアウトディレクトリ設定 (チェックアウト無効時)
					if(!project.isCheckout() && line.contains("OLD_VERSION:")){
						project.setOldDir(removeSpace(line.replace("OLD_VERSION:","")));
						if(!(new File(project.getOldDir())).exists()) {
							Logger.writeln("Can't found old version directory.", Logger.ERROR);
							return false;
						}
						// if(!(new File(project.getOldDir()+"\\src")).exists()) {
						// Logger.writeln("Can't found 'src' directory.", Logger.ERROR);
						// return false;
						// }
					}

					// トークン閾値
					if(line.contains("TOKEN:")) {
						project.setTokenTh(Integer.parseInt(removeSpace(line.replace("TOKEN:",""))));
					}

					// OVERLAPPING 閾値 (CCFinderX のみ)
					if(line.contains("OVERLAPPING:")) {
						if(removeSpace(line.replace("OVERLAPPING:","")).equals("true")) {
							project.setOlFilter(true);
						} else {
							project.setOlFilter(false);
						}
					}

					// テキストファイル出力用ディレクトリ設定
					if(line.contains("TEXT_DIR:")){
						project.setGenerateTextDir(removeSpace(line.replace("TEXT_DIR:","")));
						project.setGenerateText(true);
					}

					// HTMLファイル出力用ディレクトリ設定
					if(line.contains("HTML_DIR:")){
						project.setGenerateHTMLDir(removeSpace(line.replace("HTML_DIR:","")));
						project.setGenerateHtml(true);
					}

					// JSONファイル出力用ディレクトリ設定
					if (line.contains("JSON_DIR:")) {
						project.setGenerateJSONDir(removeSpace(line.replace("JSON_DIR:", "")));
						project.setGenerateJson(true);
					}

					// CSVファイル出力用ディレクトリ設定
					if(line.contains("CSV_DIR:")){
						project.setGenerateCSVDir(removeSpace(line.replace("CSV_DIR:","")));
						project.setGenerateCSV(true);
					}

					// アカウントファイル
					if(line.contains("ACCOUNT_FILE:")) {
						project.setAccountFile(removeSpace(line.replace("ACCOUNT_FILE:","")));
					}

					// 鍵ファイル
					if(line.contains("KEY_FILE:")) {
						project.setKeyFile(removeSpace(line.replace("KEY_FILE:","")));
					}

					// ホスト
					if(line.contains("HOST:")) {
						project.setHost(removeSpace(line.replace("HOST:","")));
					}

					// ポート
					if(line.contains("PORT:")) {
						project.setPort(Integer.valueOf(removeSpace(line.replace("PORT:",""))));
					}

					// 送り元
					if(line.contains("FROM:")) {
						project.setFrom(removeSpace(line.replace("FROM:","")));
					}

					// 送り先
					if(line.contains("TO1:")) {
						project.getToList().add(line.replace("TO1:",""));
					}

					// 送り先
					if(line.contains("TO2:")) {
						project.getToList().add(line.replace("TO2:",""));
					}

					// 送り先
					if(line.contains("TO3:")) {
						project.getToList().add(line.replace("TO3:",""));
					}

					// SSL
					if(line.contains("SSL:")){
						if(removeSpace(line.replace("SSL:","")).equals("SSL/TLS")) {
							project.setSsl(1);
						} else if(removeSpace(line.replace("SSL:","")).equals("STARTTLS")) {
							project.setSsl(2);
						}
					}
				}
			}
		} catch(Exception e) {
			return false;
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Logger.writeError(e);
				}
			}
		}
		// 作業ディレクトリ指定がない場合はデフォルトディレクトリに設定
		if (project.getWorkDir() == null)
			project.setWorkDir(Paths.get(Def.NOTIFIER_PATH, Def.DEFAULT_WORK_DIR).toString());

		// 言語とツール名の対応チェック
		if (!Def.isValidLang(project.getLang(), project.getTool())) {
			return false;
		}

		return true;
	}

	/**
	 * <p>スペース除去<p>
	 * @param str 文字列
	 * @return スペース除去後の文字列
	 */
	private static String removeSpace(String str) {
		while(str.startsWith(" ") || str.startsWith("\t")) {
			str = str.substring(1);
		}
		return str;
	}

}

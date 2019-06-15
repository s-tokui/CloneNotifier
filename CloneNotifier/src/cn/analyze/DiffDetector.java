package cn.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;

import cn.Def;
import cn.data.SourceFile;

/**
 * <p>Diff検出クラス</p>
 * @author y-yuuki
 */
public class DiffDetector {

	/**
	 * <p>ソースファイル間のdiffの取得</p>
	 * @param fileList 新旧バージョン間でdiffを実施するファイルのリスト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public static boolean  getDiff(ArrayList<SourceFile> fileList) {
		for(SourceFile file: fileList) {

			// ファイルが存続 かつ クローンが消滅していない場合
			if(file.getState() == SourceFile.NORMAL && !(file.getNewCloneList().isEmpty() && !file.getOldCloneList().isEmpty())) {
				if(!executeDiff(file)) {
					return false;
				}
			}
		}
		return true;
	}



	/**
	 * <p>diffの実行</p>
	 * @param file 新旧バージョン間のdiffを取るファイル
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private static boolean executeDiff(SourceFile file) {
		try{
			
			ProcessBuilder pb;
			if (File.separatorChar == '\\') {
				String[] cmdArray = { Paths.get(Def.NOTIFIER_PATH, Def.DIFF_PATH).toString(), file.getOldPath(),
						file.getNewPath() };
				pb = new ProcessBuilder(cmdArray);
			} else {
				String[] cmdArray = { "diff", file.getOldPath(), file.getNewPath() };
				pb = new ProcessBuilder(cmdArray);
			}
			Process p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;

			while((line = reader.readLine()) != null) {
				if(Character.isDigit(line.charAt(0))) {

					// 追加コード
					if(line.contains("a")) {
						int startLine, endLine;
						String[] str1 = line.split("a");
						String[] str2 = str1[1].split(",");

						// 1行だけ追加された場合
						if(str2.length == 1) {
							startLine = Integer.valueOf(str2[0]);
							endLine = Integer.valueOf(str2[0]);
						} else {
							startLine = Integer.valueOf(str2[0]);
							endLine = Integer.valueOf(str2[1]);
						}

						for (int i = startLine; i <= endLine; i++) {
							file.getAddedCodeList().add(i);
						}

					// 削除コード
					} else if (line.contains("d")) {
						int startLine, endLine;
						String[] str1 = line.split("d");
						String[] str2 = str1[0].split(",");

						// 1行だけ削除された場合
						if (str2.length == 1) {
							startLine = Integer.valueOf(str2[0]);
							endLine = Integer.valueOf(str2[0]);
						} else {
							startLine = Integer.valueOf(str2[0]);
							endLine = Integer.valueOf(str2[1]);
						}

						for (int i = startLine; i <= endLine; i++) {
							file.getDeletedCodeList().add(i);
						}

		    		// 変更コード
					// 変更部分全体が削除, 追加されたとみなす
		    		} else if(line.contains("c")) {
		    			int startLine, endLine;
		    			String[] str1 = line.split("c");
		    			String[] str2 = str1[0].split(",");
		    			String[] str3 = str1[1].split(",");

		    			if(str2.length == 1) {
		    				startLine = Integer.valueOf(str2[0]);
		    				endLine = Integer.valueOf(str2[0]);
		    			} else {
		    				startLine = Integer.valueOf(str2[0]);
		    				endLine = Integer.valueOf(str2[1]);
		    			}

		    			for(int i = startLine; i <= endLine; i++) {
		    				file.getDeletedCodeList().add(i);
		    			}

		    			if(str3.length == 1) {
		    				startLine = Integer.valueOf(str3[0]);
		    				endLine = Integer.valueOf(str3[0]);
		    			} else {
		    				startLine = Integer.valueOf(str3[0]);
		    				endLine = Integer.valueOf(str3[1]);
		    			}

		    			for(int i = startLine; i <= endLine; i++) {
		    				file.getAddedCodeList().add(i);
		    			}
		    		}
		    	}
		     }
		} catch (IOException e) {
			return false;
		}
		return true;
	}
}

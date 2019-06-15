package cn;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;

import cn.data.Project;

/**
 * <p>VCS管理クラス</p>
 * @author y-yuuki
 */
public class VCSController {

	/**
	 * <p>プロジェクトのチェックアウト<p>
	 * @param project Projectオブジェクト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public static boolean checkoutProject(Project project) {

		if(!deleteDir(project.getOldDir())) {
			Logger.writeln("Can't delete old directory.", Logger.ERROR);
		}

		if(!changeDir(project.getNewDir(), project.getOldDir())) {
			Logger.writeln("Can't move old directory.", Logger.ERROR);
		}

		// TODO CloneDetector の場合, 元々 ".ccfxprepdir" が存在しないにも関わらずエラーログを出している
		if(!deleteDir(project.getOldDir() + "\\.ccfxprepdir")){
			Logger.writeln("Can't delete '.ccfxprepdir' directory.", Logger.ERROR);
		}

		String[] cmdarray = project.getCheckoutCmd().split(" ");
		ProcessBuilder pb = new ProcessBuilder(cmdarray);
		Process p;
		try {
			p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			Logger.writeln("Start checkout command.", Logger.SYSTEM);

			String line;
			while((line = reader.readLine())!=null) {
				Logger.writeln("\t" + line, Logger.SYSTEM);
			}

			reader.close();
		}catch (IOException e1){
			return false;
		}

		if(!((new File(project.getNewDir())).exists())){
			(new File(project.getNewDir())).mkdirs();
			return false;
		}

		if(!((new File(project.getOldDir())).exists())){
			(new File(project.getOldDir())).mkdirs();
			return false;
		}
		return true;
	}

	/**
	 * ディレクトリ名の変更
	 * @author y-yuuki
	 * @author m-sano
	 */
	private static boolean changeDir(String dirA, String dirB) {
		File fileA = new File(dirA);
		File fileB = new File(dirB);

		if(fileA.exists()) {
			return fileA.renameTo(fileB);
		}
		return true;
	}

	/**
	 * ディレクトリの削除
	 */
	public static boolean deleteDir(String dir) {
		File file = new File(dir);
		if(file.exists()) {
			if(file.isDirectory()) {
				File[] f = file.listFiles();
				for(int i = 0; i < f.length; i++) {
					deleteDir(f[i].toString());
				}
			}
		}

		boolean result = false;
		try {
			file.setWritable(true);
			result = Files.deleteIfExists(file.toPath());
		} catch (DirectoryNotEmptyException e) {
			Logger.writeln("<DirectoryNotEmptyException> Can't delete " + file.toString(), Logger.FATAL);
			result = false;
		} catch (IOException e) {
			Logger.writeln("<IOException> Can't delete " + file.toString(), Logger.FATAL);
			Logger.writeln("\tWritable: " + file.canWrite(), Logger.DEBUG);
			Logger.writeln("\tReadable: " + file.canRead(), Logger.DEBUG);
		}

		return result;
	}
}

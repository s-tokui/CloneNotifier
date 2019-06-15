package visitor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * .cs ファイル探索用のビジター
 * @author s-tokui
 */
public class CSFileVistor extends SimpleFileVisitor<Path> {
	
	/** 検出されたファイルのリスト */
	private ArrayList<String> list = new ArrayList<String>();

	/** 探索するディレクトリの絶対パス */
	private String rootPath;

	/**
	 * .cs ファイル探索用のビジター
	 * @param root 探索するディレクトリの絶対パス
	 */
	public CSFileVistor(String root) {
		rootPath = root;
	}

	/**
	 * 検出されたファイルリストを取得
	 * @return 検出されたファイルのリスト
	 */
	public ArrayList<String> getFileList() {
		return list;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		String fileName = file.getFileName().toString();

		// 拡張子 .c と .h のファイルをリストに追加
		if(fileName.endsWith(".cs")) {
			String abstPath = file.toAbsolutePath().toString();
			String relPath = abstPath.substring(rootPath.length()+1);
			list.add(relPath);
		}

		return super.visitFile(file, attrs);
	}

}

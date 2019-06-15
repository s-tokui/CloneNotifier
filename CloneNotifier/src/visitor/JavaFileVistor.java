package visitor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * .java ファイル探索用のビジター
 * @author m-sano
 */
public class JavaFileVistor extends SimpleFileVisitor<Path> {

	/** 検出されたファイルのリスト */
	private ArrayList<String> list = new ArrayList<String>();

	/** 探索を行うディレクトリの絶対パス */
	private String rootPath;

	/**
	 * .java ファイル探索用のビジター
	 * @param root 探索を行うディレクトリの絶対パス
	 */
	public JavaFileVistor(String root) {
		rootPath = root;
	}

	/**
	 * 検出されたファイルのリストを取得する
	 * @return 検出されたファイルのリスト
	 */
	public ArrayList<String> getFileList() {
		return list;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

		// 拡張子が .java ならリストに追加
		if(file.getFileName().toString().endsWith(".java")) {
			String abstPath = file.toAbsolutePath().toString();
			String relPath = abstPath.substring(rootPath.length()+1);
			list.add(relPath);
		}

		return super.visitFile(file, attrs);
	}

}

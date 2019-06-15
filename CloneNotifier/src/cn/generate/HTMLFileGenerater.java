package cn.generate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import cn.Def;
import cn.Logger;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;
import cn.data.SourceFile;

/**
 * <p>
 * HTML出力クラス
 * </p>
 * 
 * @author y-yuuki
 */
public class HTMLFileGenerater {

	private OutputGenerator g = null;
	private Project project = null;

	/** Javascriptファイル名 */
	private static final String SCRIPT = "script.js";

	/** Javascriptディレクトリ名 */
	private final static String JS = "js";

	/** CSSディレクトリ名 */
	private final static String CSS = "css";

	/** 画像ディレクトリ名 */
	private final static String IMAGES = "images";

	/** インデックスページのhtmlファイル名 */
	private final static String INDEX_PAGE = "index.html";

	/** クローンセットリストページのhtmlファイル名 */
	private final static String CLONESETLIST_PAGE = "cloneset.html";

	/** パッケージ(ディレクトリ)リストページのhtmlファイル名 */
	private final static String PACKAGELIST_PAGE = "packagelist.html";
	private final static int STACK = 100;

	public HTMLFileGenerater(OutputGenerator g, Project project) {
		this.g = g;
		this.project = project;
	}

	/**
	 * <p>
	 * HTMLファイル生成
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param project
	 *            Projectオブジェクト
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateHTMLFile() {

		// リソースファイルのコピー
		if (!copyResourceFiles(project.getGenerateHTMLDir())) {
			return false;
		}

		// プロジェクトディレクトリの生成
		File dir = new File(project.getGenerateHTMLDir() + "\\" + project.getDate());
		dir.mkdirs();

		// プロジェクトホームファイルの出力
		if (!generateProjectPage(g, dir.getAbsolutePath(), project)) {
			return false;
		}

		// クローンセットファイルの出力
		if (!generateCloneSetListPage(g, dir.toString(), project)) {
			return false;
		}

		// パッケージ一覧ファイルの出力
		if (!generatePackageListPage(g, dir.getAbsolutePath(), project)) {
			return false;
		}

		// ソースファイルのindex.htmlの出力
		if (!generateIndexEachPackege(g, dir.getAbsolutePath(), project)) {
			return false;
		}

		// ソースファイル一覧ファイルの出力

		if (!generateFileListPage(g, new File(dir.getAbsolutePath()), 1, dir.getAbsolutePath(), project)) {
			return false;
		}
		if (!generateDateListPage(project)) {
			return false;
		}
		return true;
	}

	/**
	 * <p>
	 * 画像ファイルのコピー処理
	 * </p>
	 * 
	 * @param generateHTMLDir
	 *            出力ディレクトリ
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean copyImageFiles(String generateHTMLDir) {
		File dir = new File(generateHTMLDir + "\\" + Def.RESOURCES);

		if (dir.exists()) {
			return true;
		}

		dir.mkdirs();
		String[] images = { "asc.gif", "desc.gif", "sort.gif" };
		for (String image : images) {
			try {
				FileInputStream src = new FileInputStream(Def.RESOURCES + "/" + image);
				FileOutputStream dest = new FileOutputStream(dir.toString() + "/" + image);
				FileChannel srcChannel = src.getChannel();
				FileChannel destChannel = dest.getChannel();
				try {
					srcChannel.transferTo(0, srcChannel.size(), destChannel);
				} catch (IOException e) {
					return false;
				} finally {
					try {
						srcChannel.close();
						destChannel.close();
						src.close();
						dest.close();
					} catch (IOException e) {
						return false;
					}
				}
			} catch (FileNotFoundException e) {
				Logger.writeln("Can't find image files.", Logger.ERROR);
				return false;
			}
		}
		return true;
	}

	/**
	 * <p>
	 * 画像ファイルのコピー処理
	 * </p>
	 * 
	 * @param generateHTMLDir
	 *            出力ディレクトリ
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean copyResourceFiles(String generateHTMLDir) {
		File srcDir = new File(Def.RESOURCES + "/DataTables-1.10.16/");
		File destDir = new File(generateHTMLDir);

		IOFileFilter filter = FileFilterUtils.nameFileFilter("LICENSE");
		try {
			FileUtils.copyDirectory(srcDir, destDir, FileFilterUtils.notFileFilter(filter));
		} catch (IOException e1) {
			Logger.writeln("Can't find image files.", Logger.ERROR);
			return false;
		}
		return true;
	}

	/**
	 * <p>
	 * JavaScriptの生成
	 * </p>
	 * 
	 * @param dir
	 *            出力ディレクトリ
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateScript(String dir) {
		try {
			if (!new File(dir + "\\" + SCRIPT).exists()) {
				PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dir + "\\" + SCRIPT)));
				writer.println("var table=function(){");
				writer.println("\tfunction sorter(n){");
				writer.println("\t\tthis.n=n; this.t; this.b; this.r; this.d; this.p; this.w; this.a=[]; this.l=0");
				writer.println("\t}");
				writer.println("\tsorter.prototype.init=function(t,f){");
				writer.println("\t\tthis.t=document.getElementById(t);");
				writer.println("\t\tthis.b=this.t.getElementsByTagName('tbody')[0];");
				writer.println("\t\tthis.r=this.b.rows; var l=this.r.length;");
				writer.println("\t\tfor(var i=0;i<l;i++){");
				writer.println("\t\t\tif(i==0){");
				writer.println("\t\t\t\tvar c=this.r[i].cells; this.w=c.length;");
				writer.println("\t\t\t\tfor(var x=0;x<this.w;x++){");
				writer.println("\t\t\t\t\tif(c[x].className!='nosort'){");
				writer.println("\t\t\t\t\t\tc[x].className='head';");
				writer.println("\t\t\t\t\t\tc[x].onclick=new Function(this.n+'.work(this.cellIndex)')");
				writer.println("\t\t\t\t\t}");
				writer.println("\t\t\t\t}");
				writer.println("\t\t\t}else{");
				writer.println("\t\t\t\tthis.a[i-1]={}; this.l++;");
				writer.println("\t\t\t}");
				writer.println("\t\t}");
				writer.println("\t\tif(f!=null){");
				writer.println("\t\t\tvar a=new Function(this.n+'.work('+f+')'); a()");
				writer.println("\t\t}");
				writer.println("\t}");
				writer.println("\tsorter.prototype.work=function(y){");
				writer.println("\t\tthis.b=this.t.getElementsByTagName('tbody')[0]; this.r=this.b.rows;");
				writer.println("\t\tvar x=this.r[0].cells[y],i;");
				writer.println("\t\tfor(i=0;i<this.l;i++){");
				writer.println("\t\t\tthis.a[i].o=i+1; var v=this.r[i+1].cells[y].firstChild;");
				writer.println("\t\t\tthis.a[i].value=(v!=null)?v.nodeValue:''");
				writer.println("\t\t}");
				writer.println("\t\tfor(i=0;i<this.w;i++){");
				writer.println("\t\t\tvar c=this.r[0].cells[i];");
				writer.println("\t\t\tif(c.className!='nosort'){c.className='head'}");
				writer.println("\t\t}");
				writer.println("\t\tif(this.p==y){");
				writer.println("\t\t\tthis.a.reverse(); x.className=(this.d)?'asc':'desc';");
				writer.println("\t\t\tthis.d=(this.d)?false:true");
				writer.println("\t\t}else{");
				writer.println("\t\t\tthis.p=y; this.a.sort(compare); x.className='asc'; this.d=false");
				writer.println("\t\t}");
				writer.println("\t\tvar n=document.createElement('tbody');");
				writer.println("\t\tn.appendChild(this.r[0]);");
				writer.println("\t\tfor(i=0;i<this.l;i++){");
				writer.println("\t\t\tvar r=this.r[this.a[i].o-1].cloneNode(true);");
				writer.println("\t\t\tn.appendChild(r); r.className=(i%2==0)?'even':'odd'");
				writer.println("\t\t}");
				writer.println("\t\tthis.t.replaceChild(n,this.b)");
				writer.println("\t}");
				writer.println("\tfunction compare(f,c){");
				writer.println("\t\tf=f.value,c=c.value;");
				writer.println(
						"\t\tvar i=parseFloat(f.replace(/(\\$|\\,)/g,'')),n=parseFloat(c.replace(/(\\$|\\,)/g,''));");
				writer.println("\t\tif(!isNaN(i)&&!isNaN(n)){f=i,c=n}");
				writer.println("\t\treturn (f>c?1:(f<c?-1:0))");
				writer.println("\t}");
				writer.println("\treturn{sorter:sorter}");
				writer.println("}();");
				writer.close();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * <p>
	 * プロジェクトページ生成
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param dir
	 *            出力ディレクトリ
	 * @param project
	 *            Projectオブジェクト
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateProjectPage(OutputGenerator g, String dir, Project project) {
		try {
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dir + "\\" + INDEX_PAGE)));

			// ヘッダ部出力
			outputHtmlHead(writer, project.getName());

			// タイトル出力
			writer.printf("<a href=\"../%s\">%s</a>\r\n", INDEX_PAGE, project.getDate());
			writer.printf("<h1> プロジェクト名： %s </h1>\r\n", project.getName());
			writer.printf("<hr>\r\n");
			writer.printf("<center>\r\n");

			writer.printf("<h2><a href=\"%s\">クローンセット一覧</a></h2>\r\n", CLONESETLIST_PAGE);
			writer.printf("<hr>\r\n");
			writer.printf("<h2><a href=\"%s\">ディレクトリ（パッケージ）一覧</a></h2>\r\n", PACKAGELIST_PAGE);
			writer.printf("<hr>\r\n");

			// プロジェクト情報の出力
			writer.printf("<table border=\"1\">\r\n");
			writer.printf("<tr><th bgcolor=\"lightgrey\" colspan=\"2\">プロジェクト情報</th></tr>\r\n");

			// ファイル情報出力
			writer.printf("<tr bgcolor=\"#D1D168\"><th colspan=\"2\">ソースファイル情報</th></tr>\r\n");
			writer.printf("<tr><th width=\"400\">総ファイル数</th><td width=\"200\">%d</td></tr>\r\n", g.getFileNum());
			writer.printf("<tr><th width=\"400\">追加ファイル数</th><td width=\"200\">%d</td></tr>\r\n", g.getAddedFileNum());
			writer.printf("<tr><th width=\"400\">削除ファイル数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getDeletedFileNum());
			writer.printf("<tr><th width=\"400\">クローンを含むファイル数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getCloneFileNum());

			// クローンセット情報一覧
			writer.printf("<tr bgcolor=\"#D1D168\"><th colspan=\"2\">クローンセット分類情報</th></tr>\r\n");
			writer.printf("<tr><th width=\"400\">総クローンセット数</th><td width=\"200\">%d</td></tr>\r\n", g.getCloneSetNum());
			writer.printf(
					"<tr><th width=\"400\"><a href=\"%s#stable\">STABLEクローンセット数</a></th><td width=\"200\">%d</td></tr>\r\n",
					CLONESETLIST_PAGE, g.getStableCloneSetNum());
			writer.printf(
					"<tr><th width=\"400\"><a href=\"%s#changed\">CHANGEDクローンセット数</a></th><td width=\"200\">%d</td></tr>\r\n",
					CLONESETLIST_PAGE, g.getChangedCloneSetNum());
			writer.printf(
					"<tr><th width=\"400\"><a href=\"%s#new\">NEWクローンセット数</a></th><td width=\"200\">%d</td></tr>\r\n",
					CLONESETLIST_PAGE, g.getNewCloneSetNum());
			writer.printf(
					"<tr><th width=\"400\"><a href=\"%s#deleted\">DELETEDクローンセット数</a></th><td width=\"200\">%d</td></tr>\r\n",
					CLONESETLIST_PAGE, g.getDeletedCloneSetNum());

			// クローン情報一覧
			writer.printf("<tr bgcolor=\"D1D168\"><th colspan=\"2\">コードクローン分類情報</th></tr>\r\n");
			writer.printf("<tr><th width=\"400\">総コードクローン数</th><td width=\"200\">%d</td></tr>\r\n", g.getCloneNum());
			writer.printf("<tr><th width=\"400\">STABLEクローン数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getStableCloneNum());
			writer.printf("<tr><th width=\"400\">MODIFIEDクローン数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getModifiedCloneNum());
			writer.printf("<tr><th width=\"400\">MOVEDクローン数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getMovedCloneNum());
			writer.printf("<tr><th width=\"400\">ADDEDクローン数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getAddedCloneNum());
			writer.printf("<tr><th width=\"400\">DELETEDクローン数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getDeletedCloneNum());
			writer.printf("<tr><th width=\"400\">DELETEMODIFIEDクローン数</th><td width=\"200\">%d</td></tr>\r\n",
					g.getDeleteModifiedCloneNum());

			writer.printf("</table>\n");
			writer.printf("</body>\n");
			writer.printf("</html>\n");

			writer.flush();
			writer.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	/**
	 * </p>
	 * クローンセット一覧ページ生成
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param dir
	 *            出力ディレクトリ
	 * @param project
	 *            Projectオブジェクト
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateCloneSetListPage(OutputGenerator g, String dir, Project project) {
		try {
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dir + "\\" + CLONESETLIST_PAGE)));

			// ヘッダ部出力
			// outputHtmlHead(writer, project.getName()+"-クローンセット一覧");
			writer.print("<html>\r\n");
			writer.print("<head>\r\n" + "<title>test-クローンセット一覧</title>\r\n"
					+ "<link rel=\"stylesheet\" href=\"../css/datatables.min.css\" type=\"text/css\" media=\"screen\" />\r\n"
					+ "<script type=\"text/javascript\" src=\"../js/jquery-3.2.1.slim.min.js\"></script> \r\n"
					+ "<script type=\"text/javascript\" src=\"../js/datatables.min.js\"></script> \r\n"
					+ "<script type=\"text/javascript\" src=\"../js/script.js\"></script> \r\n" + "</head>\r\n");

			// タイトル出力
			writer.printf("<body>\r\n");
			writer.printf("<a href=\"../%s\">%s</a>\r\n", INDEX_PAGE, project.getDate());
			writer.printf("<h1>プロジェクト名：<a href=\"%s\">%s</a></h1>\r\n", INDEX_PAGE, project.getName());
			writer.printf("<h2>クローンセットリスト</h2>\r\n");
			writer.printf("<center>\r\n");

			// クローンセットが全く無い場合
			if (project.getCloneSetList().isEmpty()) {
				writer.printf("<hr>\r\n");
				writer.printf("クローンセットは存在しません\r\n ");
			} else {
				writer.printf("<hr>\r\n");
				writer.printf("<p>\r\n" + "<div align=\"right\">\r\n"
						+ "<input type=\"checkbox\" id=\"isDisplayRisk\">Risk 0 を表示しない\r\n" + "</div>\r\n"
						+ "</p>\r\n");

				// クローンセット一覧
				writer.printf("<table border=\"1\" class=\"display compact\" id=\"myTable\" width=\"1000px\">\r\n");
				writer.printf("<thead>\r\n");
				writer.printf("<tr>\r\n");
				writer.printf("<th bgcolor=\"gainsboro\" class=\"nosort\">ID</th>\r\n");
				writer.printf("<th >分類</th>\r\n");
				writer.printf("<th >RISK</th>\r\n");

				if (project.getTool().equals(Def.CCFX_TOOLNAME)) {
					writer.printf("<th >LEN</th>\r\n");
					writer.printf("<th>POP</th>\r\n");
					writer.printf("<th>NIF</th>\r\n");
					writer.printf("<th>RAD</th>\r\n");
					writer.printf("<th>RNR</th>\r\n");
					writer.printf("<th>TKS</th>\r\n");
					writer.printf("<th>LOOP</th>\r\n");
					writer.printf("<th>COND</th>\r\n");
					writer.printf("<th>McCabe</th>\r\n");
				}
				writer.printf("</tr>\r\n");
				writer.printf("</thead>\r\n");
				writer.printf("<tbody>\r\n");

				for (CloneSet cloneSet : project.getCloneSetList()) {
					writer.printf("<tr bgcolor=\"%s\">\r\n", getCloneSetColor(cloneSet));
					writer.printf("<td><a href=\"cloneset.html#cloneset%d\">%d</a></td>\r\n", cloneSet.getOutputId(),
							cloneSet.getOutputId());
					writer.printf("<td>%s</td>\r\n", cloneSet.getCategoryString());
					writer.printf("<td>%f</td>\r\n", cloneSet.getRisk());

					if (project.getTool().equals(Def.CCFX_TOOLNAME)) {
						writer.printf("<td>%d</td>\r\n", cloneSet.getLEN());
						writer.printf("<td>%d</td>\r\n", cloneSet.getPOP());
						writer.printf("<td>%d</td>\r\n", cloneSet.getNIF());
						writer.printf("<td>%d</td>\r\n", cloneSet.getRAD());
						writer.printf("<td>%f</td>\r\n", cloneSet.getRNR());
						writer.printf("<td>%d</td>\r\n", cloneSet.getTKS());
						writer.printf("<td>%d</td>\r\n", cloneSet.getLOOP());
						writer.printf("<td>%d</td>\r\n", cloneSet.getCOND());
						writer.printf("<td>%d</td>\r\n", cloneSet.getMcCabe());
					}
					writer.printf("</tr>\r\n");
				}
				writer.printf("<tbody>\r\n");
				writer.printf("</table>\r\n");

				// 新規クローンセット一覧
				writer.printf("<hr>\r\n");
				writer.printf("<h2 id=\"new\">New Clone Set</h2>\r\n");
				if (g.getNewCloneSetNum() == 0) {
					writer.printf("New Clone Set は存在しません\r\n ");
				} else {
					for (CloneSet cloneSet : project.getCloneSetList()) {
						if (cloneSet.getCategory() == CloneSet.NEW) {
							outputCloneSet(writer, project, cloneSet);
						}
					}
				}

				// 変更クローンセット一覧
				writer.printf("<hr>\r\n");
				writer.printf("<h2 id=\"changed\">Changed Clone Set</h2>\r\n");
				if (g.getChangedCloneSetNum() == 0) {
					writer.printf("Changed Clone Set は存在しません\r\n ");
				} else {
					for (CloneSet cloneSet : project.getCloneSetList()) {
						if (cloneSet.getCategory() == CloneSet.CHANGED) {
							outputCloneSet(writer, project, cloneSet);
						}
					}
				}

				// 除去クローンセット一覧
				writer.printf("<hr>\r\n");
				writer.printf("<h2 id=\"deleted\">Deleted Clone Set</h2>\r\n");
				if (g.getDeletedCloneSetNum() == 0) {
					writer.printf("Deleted Clone Set  は存在しません\r\n ");
				} else {
					for (CloneSet cloneSet : project.getCloneSetList()) {
						if (cloneSet.getCategory() == CloneSet.DELETED) {
							outputCloneSet(writer, project, cloneSet);
						}
					}
				}

				// 現状維持クローンセット一覧
				writer.printf("<hr>\r\n");
				writer.printf("<h2 id=\"stable\">Stable Clone Set</h2>\r\n");
				if (g.getStableCloneSetNum() == 0) {
					writer.printf("Stable Clone Setは存在しません\r\n ");
				} else {
					for (CloneSet cloneSet : project.getCloneSetList()) {
						if (cloneSet.getCategory() == CloneSet.STABLE) {
							outputCloneSet(writer, project, cloneSet);
						}
					}
				}
			}

			writer.printf("</body>\r\n");
			writer.printf("</html>\r\n");

			writer.flush();
			writer.close();

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * <p>
	 * クローンセットのコードクローン一覧出力
	 * </p>
	 * 
	 * @param writer
	 *            PrintWriterオブジェクト
	 * @param project
	 *            Projectオブジェクト
	 * @param cloneSet
	 *            CloneSetオブジェクト
	 */
	private void outputCloneSet(PrintWriter writer, Project project, CloneSet cloneSet) {

		// クローン一覧タグの表示
		writer.printf("<hr>\r\n");
		writer.printf("<table id=\"cloneset%d\" border=\"1\">\r\n", cloneSet.getOutputId());
		writer.printf("<tr>\r\n");
		writer.printf("<th bgcolor=\"%s\" colspan=\"3\">クローンセットID:%d</th>\r\n", getCloneSetColor(cloneSet),
				cloneSet.getOutputId());
		writer.printf("<th bgcolor=\"%s\" colspan=\"1\">危険度:%f</th>\r\n", getCloneSetColor(cloneSet),
				cloneSet.getRisk());
		writer.printf("</tr>\r\n");
		writer.printf("<tr bgcolor=\"lightgrey\">\r\n");
		writer.printf("<th width=\"50\">ID</th>\r\n");
		writer.printf("<th width=\"100\">分類</th>\r\n");
		writer.printf("<th width=\"720\">ファイル名</th>\r\n");
		writer.printf("<th width=\"120\">位置</th>\r\n");

		if (project.getTool().equals(Def.CD_TOOLNAME)) {
			writer.printf("<th width=\"200\">メソッド名</th>\r\n");
		}

		writer.printf("</tr>\r\n");

		// クローン一覧出力
		for (Clone clone : cloneSet.getNewCloneList()) {
			writer.printf("<tr bgcolor=\"%s\">\r\n", getCloneColor(clone));
			writer.printf("<td><a href=\"%s#clone%d.%d\">%d.%d</a></td>\r\n",
					clone.getFile().getName().replace("\\", "/") + ".html", cloneSet.getOutputId(), clone.getOutputId(),
					cloneSet.getOutputId(), clone.getOutputId());
			writer.printf("<td>%s</td>\r\n", clone.getCategoryString());
			writer.printf("<td>%s</td>\r\n", clone.getFile().getName());
			writer.printf("<td>%d.%d-%d.%d</td>\n", clone.getStartLine(), clone.getStartColumn(), clone.getEndLine(),
					clone.getEndColumn());

			if (project.getTool().equals(Def.CD_TOOLNAME)) {
				writer.printf("<td>%s</td>", clone.getMethodName());
			}

			writer.printf("</tr>\n");
		}

		// 旧バージョンのクローン
		for (Clone clone : cloneSet.getOldCloneList()) {
			if (clone.getCategory() == Clone.DELETED || clone.getCategory() == Clone.DELETE_MODIFIED) {
				if (clone.getChildClone() != null)
					continue;
				writer.printf("<tr><th colspan=\"4\">前バージョンのコードクローン</th></tr>\r\n");
				break;
			}
		}

		// 元クローン出力
		for (Clone clone : cloneSet.getOldCloneList()) {
			if (clone.getCategory() == Clone.DELETED || clone.getCategory() == Clone.DELETE_MODIFIED) {
				if (clone.getChildClone() != null)
					continue;
				writer.printf("<tr bgcolor=\"%s\">\r\n", getCloneColor(clone));
				writer.printf("<td><a href=\"%s#clone%d.%d\">%d.%d</a></td>\r\n",
						clone.getFile().getName().replace("\\", "/") + ".html", cloneSet.getOutputId(),
						clone.getOutputId(), cloneSet.getOutputId(), clone.getOutputId());
				writer.printf("<td>%s</td>\r\n", clone.getCategoryString());
				writer.printf("<td>%s</td>\r\n", clone.getFile().getName());
				writer.printf("<td>%d.%d-%d.%d</td>\n", clone.getStartLine(), clone.getStartColumn(),
						clone.getEndLine(), clone.getEndColumn());

				if (project.getTool().equals(Def.CD_TOOLNAME)) {
					writer.printf("<td>%s</td>", clone.getMethodName());
				}

				writer.printf("</tr>\n");
			}
		}
		writer.printf("</table>\n");
	}

	/**
	 * <p>
	 * パッケージリストの生成
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param dir
	 *            出力ディレクトリ
	 * @param project
	 *            Projectオブジェクト
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generatePackageListPage(OutputGenerator g, String dir, Project project) {
		try {
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dir + "/" + PACKAGELIST_PAGE)));

			// ヘッダ部出力
			outputHtmlHead(writer, project.getName() + "-ディレクトリ（パッケージ）一覧");

			// タイトル出力
			writer.printf("<a href=\"../%s\">%s</a>\r\n", INDEX_PAGE, project.getDate());
			writer.printf("<h1>プロジェクト名：<a href=\"%s\">%s</a></h1>\r\n", INDEX_PAGE, project.getName());
			writer.printf("<h2>ディレクトリ（パッケージ）一覧</h2>\r\n");
			writer.printf("<hr>\r\n");
			writer.printf("<center>\r\n");

			// パッケージ一覧
			writer.printf("<table border=\"1\" width=\"500\">\n");
			writer.printf("<tr><th bgcolor=\"greenyellow\">ディレクトリ（パッケージ）一覧</th></tr>\n");

			ArrayList<String> packageList = new ArrayList<String>();
			boolean flag = false;
			for (SourceFile file : project.getFileList()) {
				if (file.getNewCloneList().isEmpty() && file.getOldCloneList().isEmpty())
					continue;
				flag = true;

				String fileName;
				if (file.getState() != SourceFile.DELETED)
					fileName = (new File(file.getNewPath())).getName();
				else
					fileName = (new File(file.getOldPath())).getName();

				// ディレクトリ生成
				String packageDirPath = dir + "\\" + file.getName();
				packageDirPath = packageDirPath.replace("\\" + fileName, "");
				File packageDir = new File(packageDirPath);
				if (!packageList.contains(packageDirPath.toString())) {
					packageList.add(packageDirPath.toString());
					packageDir.mkdirs();
					writer.printf("<tr><td><a href=\"%s\">%s</a></td></tr>\r\n",
							packageDirPath.replace(dir, "").replace("\\", "/").substring(1) + "/" + INDEX_PAGE,
							packageDirPath.replace(dir, "").substring(1));
				}

			}

			// ファイルが存在しない場合
			if (!flag) {
				writer.printf("<tr><td>クローンを含むディレクトリ（パッケージは存在しません）</td></tr>\r\n");
			}

			writer.printf("</table>\r\n");
			writer.printf("</body>\r\n");
			writer.printf("</html>\r\n");

			writer.flush();
			writer.close();

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private boolean generateIndexEachPackege(OutputGenerator g, String dir, Project project) {
		for (SourceFile file : project.getFileList()) {
			if (file.getNewCloneList().isEmpty() && file.getOldCloneList().isEmpty())
				continue;
			String fileName;
			if (file.getState() != SourceFile.DELETED)
				fileName = (new File(file.getNewPath())).getName();
			else
				fileName = (new File(file.getOldPath())).getName();
			String packageDirPath = dir + "\\" + file.getName();
			packageDirPath = packageDirPath.replace("\\" + fileName, "");
			// ソースコードの生成
			if (file.getState() == SourceFile.ADDED) {
				generateAddedSourceFile(g, file, packageDirPath + "\\" + fileName + ".html", project.getName());
			} else if (file.getState() == SourceFile.NORMAL) {
				generateNormalSourceFile(g, file, packageDirPath + "\\" + fileName + ".html", project.getName());
			} else if (file.getState() == SourceFile.DELETED) {
				generateDeletedSourceFile(g, file, packageDirPath + "\\" + fileName + ".html", project.getName());
			}
		}
		return true;
	}

	/**
	 * <p>
	 * 存続ソースファイルの生成
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param file
	 *            出力対象ソースファイル
	 * @param fileName
	 *            出力ファイル名
	 * @param projectName
	 *            プロジェクト名
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateNormalSourceFile(OutputGenerator g, SourceFile file, String fileName,
			String projectName) {

		BufferedReader readerA = null, readerB = null;
		try {
			readerA = new BufferedReader(new InputStreamReader(new FileInputStream(file.getNewPath())));
			readerB = new BufferedReader(new InputStreamReader(new FileInputStream(file.getOldPath())));
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			String lineA, lineB;
			int countClone = 0;
			int lineNumA = 0, lineNumB = 0;
			int addCodeId = 0, deleteCodeId = 0;

			// ヘッダ部出力
			String cloneSetFile = outputHtmlHead(g, writer, file, projectName);

			// ソースコード出力
			writer.printf("<table cellpadding=\"0\">\r\n");
			writer.printf("<tr><td width=\"50\"></td><td width=\"50\"></td><td></td></tr>\r\n");
			while (true) {
				if ((lineA = readerA.readLine()) != null) {
					lineNumA++;
				}
				if ((lineB = readerB.readLine()) != null) {
					lineNumB++;
				}

				// 追加コードの場合
				while (addCodeId < file.getAddedCodeList().size()
						&& lineNumA == file.getAddedCodeList().get(addCodeId)) {
					addCodeId++;
					countClone = countClone
							+ writeCloneStartSign(writer, file.getNewCloneList(), lineNumA, true, cloneSetFile);
					writeCodeLine(writer, Integer.toString(lineNumA), "+", lineA, countClone);
					countClone = countClone - writeCloneEndSign(writer, file.getNewCloneList(), lineNumA, true);
					if ((lineA = readerA.readLine()) != null) {
						lineNumA++;
					}
				}

				// 削除コードの場合
				while (deleteCodeId < file.getDeletedCodeList().size()
						&& lineNumB == file.getDeletedCodeList().get(deleteCodeId)) {
					deleteCodeId++;
					countClone = countClone
							+ writeCloneStartSign(writer, file.getOldCloneList(), lineNumB, false, cloneSetFile);
					writeCodeLine(writer, "", "-", lineB, countClone);
					countClone = countClone - writeCloneEndSign(writer, file.getOldCloneList(), lineNumB, false);
					if ((lineB = readerB.readLine()) != null) {
						lineNumB++;
					}
				}

				if (lineA == null && lineB == null) {
					break;
				} else {
					countClone = countClone
							+ writeCloneStartSign(writer, file.getNewCloneList(), lineNumA, true, cloneSetFile);
					countClone = countClone
							+ writeCloneStartSign(writer, file.getOldCloneList(), lineNumB, false, cloneSetFile);
					writeCodeLine(writer, Integer.toString(lineNumA), "", lineA, countClone);
					countClone = countClone - writeCloneEndSign(writer, file.getNewCloneList(), lineNumA, true);
					countClone = countClone - writeCloneEndSign(writer, file.getOldCloneList(), lineNumB, false);
				}
			}

			writer.printf("</table>\n");
			writer.printf("</body>\n");
			writer.printf("</html>\n");

			writer.flush();
			writer.close();

			if (readerA != null) {
				readerA.close();
			}
			if (readerB != null) {
				readerB.close();
			}

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * <p>
	 * 追加ソースファイルの生成
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param file
	 *            出力対象ソースファイル
	 * @param fileName
	 *            出力ファイル名
	 * @param projectName
	 *            プロジェクト名
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateAddedSourceFile(OutputGenerator g, SourceFile file, String fileName,
			String projectName) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getNewPath())));
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			int countClone = 0;
			String line;
			int lineNum = 0;

			// ヘッダ部出力
			String cloneSetFile = outputHtmlHead(g, writer, file, projectName);

			// ソースコード出力
			writer.printf("<table cellpadding=\"0\">\r\n");
			writer.printf("<tr><td width=\"50\"></td><td width=\"50\"></td><td></td></tr>\r\n");
			while ((line = reader.readLine()) != null) {
				lineNum++;
				countClone = writeCloneStartSign(writer, file.getNewCloneList(), lineNum, true, cloneSetFile)
						+ countClone;
				writeCodeLine(writer, Integer.toString(lineNum), "+", line, countClone);
				countClone = countClone - writeCloneEndSign(writer, file.getNewCloneList(), lineNum, true);
			}
			writer.printf("</table>\r\n");
			writer.printf("</body>\r\n");
			writer.printf("</html>\r\n");

			writer.flush();
			writer.close();
			if (reader != null) {
				reader.close();
			}

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * <p>
	 * 削除ソースファイルの生成
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param file
	 *            出力対象ソースファイル
	 * @param fileName
	 *            出力ファイル名
	 * @param projectName
	 *            プロジェクト名
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateDeletedSourceFile(OutputGenerator g, SourceFile file, String fileName,
			String projectName) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getOldPath())));
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			String line;
			int lineNum = 0;
			int countClone = 0;

			// ヘッダ部出力
			String cloneSetFile = outputHtmlHead(g, writer, file, projectName);

			// ソースコード出力
			writer.printf("<table cellpadding=\"0\">\r\n");
			writer.printf("<tr><td width=\"50\"></td><td width=\"50\"></td><td></td></tr>\r\n");
			while ((line = reader.readLine()) != null) {
				lineNum++;

				// ソースコード１行出力
				countClone = countClone
						+ writeCloneStartSign(writer, file.getOldCloneList(), lineNum, false, cloneSetFile);
				writeCodeLine(writer, Integer.toString(lineNum), "-", line, countClone);
				countClone = countClone - writeCloneEndSign(writer, file.getOldCloneList(), lineNum, false);
			}
			writer.printf("</table>\r\n");
			writer.printf("</body>\r\n");
			writer.printf("</html>\r\n");

			writer.flush();
			writer.close();
			if (reader != null) {
				reader.close();
			}

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * <p>
	 * クローン開始サインの出力
	 * </p>
	 * 
	 * @param writer
	 *            書き込み用のライター
	 * @param cloneList
	 *            クローン一覧
	 * @param line
	 *            チェックする行番号
	 * @param isNew
	 *            新バージョンのクローンならtrue
	 * @param cloneSetFile
	 *            クローンセット一覧ファイル
	 * @return lineから開始するクローンの総数
	 */
	private int writeCloneStartSign(PrintWriter writer, ArrayList<Clone> cloneList, int line, boolean isNew,
			String cloneSetFile) {
		int count = 0;
		for (Clone clone : cloneList) {
			if (clone.getOutputId() == Clone.NULL)
				continue;
			if (clone.getStartLine() != line)
				continue;
			if (!isNew) {
				if (clone.getChildClone() != null)
					continue;
			}
			writer.printf("<tr id=\"clone%d.%d\" ><th></th><th></th>", clone.getCloneSet().getOutputId(),
					clone.getOutputId());
			writer.printf("<th align=\"left\">[START CLONE:<a href=\"%s#cloneset%d\">%d.%d(%sクローン)</a>]</th></tr>",
					cloneSetFile, clone.getCloneSet().getOutputId(), clone.getCloneSet().getOutputId(),
					clone.getOutputId(), clone.getCategoryString());
			writer.println();
			count++;
		}
		return count;
	}

	/**
	 * <p>
	 * クローン終了サインの出力
	 * </p>
	 * 
	 * @param writer
	 *            書き込み用のライター
	 * @param cloneList
	 *            クローン一覧
	 * @param line
	 *            チェックする行番号
	 * @param isNew
	 *            新バージョンのクローンならtrue
	 * @return lineで終了するクローンの総数
	 */
	private int writeCloneEndSign(PrintWriter writer, ArrayList<Clone> cloneList, int line, boolean isNew) {
		int count = 0;
		for (Clone clone : cloneList) {
			if (clone.getOutputId() == Clone.NULL)
				continue;
			if (clone.getEndLine() != line)
				continue;
			if (!isNew) {
				if (clone.getChildClone() != null)
					continue;
			}
			writer.printf("<tr id=\"clone%d.%d\" ><th></th><th></th>", clone.getCloneSet().getOutputId(),
					clone.getOutputId());
			writer.printf("<th  align=\"left\">[END ID:%d.%d]</th></tr>", clone.getCloneSet().getOutputId(),
					clone.getOutputId());
			writer.println();
			count++;
		}
		return count;
	}

	/**
	 * <p>
	 * ソースコード1行出力
	 * </p>
	 * 
	 * @param writer
	 *            書き込み用ライター
	 * @param lineNum
	 *            行番号
	 * @param state
	 *            該当コード行の状態
	 * @param line
	 *            書き込むコード
	 * @param count
	 *            その行を含むクローンの総数
	 */
	private void writeCodeLine(PrintWriter writer, String lineNum, String state, String line, int count) {
		writer.printf("<tr>\r\n");
		writer.printf("<th align=\"left\">%s</th>\r\n", lineNum);
		writer.printf("<th align=\"left\">%s</th>\r\n", state);
		if (count > 0 && state.equals("-")) {
			writer.printf("<td bgcolor=\"F2D500\"><xmp>%s</xmp></td>\r\n", line);
		} else if (count > 0 && state.equals("+")) {
			writer.printf("<td bgcolor=\"yellow\"><xmp>%s</xmp></td>\r\n", line);
		} else if (count == 0 && state.equals("-")) {
			writer.printf("<td bgcolor=\"tan\"><xmp>%s</xmp></td>\r\n", line);
		} else {
			writer.printf("<td><xmp>%s</xmp></td>\r\n", line);
		}
		writer.println("</tr>");
	}

	/**
	 * <p>
	 * Indexページの生成
	 * </p>
	 * 
	 * @param project
	 *            Projectオブジェクト
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateDateListPage(Project project) {

		// index.htmlの削除
		File index = new File(project.getGenerateHTMLDir() + "\\" + INDEX_PAGE);
		if (index.exists()) {
			index.delete();
		}

		// indexファイル生成
		try {
			PrintWriter writer = new PrintWriter(
					new BufferedWriter(new FileWriter(project.getGenerateHTMLDir() + "\\" + INDEX_PAGE)));
			File[] dateList = (new File(project.getGenerateHTMLDir())).listFiles();

			// タイトル出力
			outputHtmlHead(writer, "コードクローン変更管理システム");
			writer.println("<h1>コードクローン変更管理システム</h1>");
			writer.println("<h1>分析プロジェクト：" + project.getName() + "</h1>");
			writer.println("<hr>");
			writer.println("<center>");

			// 分析日一覧
			writer.println("<table border=\"1\">");
			writer.println("<tr bgcolor=\"greenyellow\"><th>分析日一覧</th></tr>");
			for (int i = 0; i < dateList.length; i++) {
				String date = dateList[i].getName();
				if (dateList[i].isDirectory() && i < STACK && date.length() == 8) {
					writer.printf("<tr><td><a href = \"%s\">%c%c%c%c年%c%c月%c%c日</a></td></tr>\n",
							date + "/" + INDEX_PAGE, date.charAt(0), date.charAt(1), date.charAt(2), date.charAt(3),
							date.charAt(4), date.charAt(5), date.charAt(6), date.charAt(7));
				} else {
					if (!dateList[i].getName().equals(JS) && !dateList[i].getName().equals(CSS)
							&& !dateList[i].getName().equals(IMAGES)) {
						deleteDir(dateList[i].toString());
					}
				}
			}
			writer.println("</table>");
			writer.println("</html>");

			writer.flush();
			writer.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * <p>
	 * ソースファイルページ一覧
	 * </p>
	 * 
	 * @param g
	 *            OutputGeneratorオブジェクト
	 * @param dir
	 *            出力ディレクトリ
	 * @param node
	 *            ノード
	 * @param indexDir
	 *            index.htmlの相対位置
	 * @param project
	 *            Projectオブジェクト
	 * @return
	 *         <ul>
	 *         <li>成功の場合 - true</li>
	 *         <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	private boolean generateFileListPage(OutputGenerator g, File dir, int node, String indexDir,
			Project project) {

		File[] files = dir.listFiles();

		// ソースファイルが存在するか判定
		boolean flag = false;
		for (File file : files) {
			if (file.getName().equals(dir.getName() + "/" + INDEX_PAGE))
				continue;
			if (file.getName().equals(dir.getName() + "/" + CLONESETLIST_PAGE))
				continue;
			if (file.getName().equals(dir.getName() + "/" + PACKAGELIST_PAGE))
				continue;
			if (file.isFile()) {
				flag = true;
			} else if (file.isDirectory()) {
				generateFileListPage(g, file, node + 1, indexDir, project);
			}
		}
		if (dir.getPath().equals(project.getGenerateHTMLDir() + "\\" + project.getDate()))
			return true;

		if (flag) {
			try {
				PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dir + "\\" + INDEX_PAGE)));
				// ヘッダ部出力
				outputHtmlHead(writer, project.getName() + "-" + dir.toString().replace(indexDir, "") + "-ソースファイル一覧");

				// タイトルの出力
				String projectFile = INDEX_PAGE;
				String projectListFile = "../" + INDEX_PAGE;
				for (int i = 0; i < node; i++) {
					projectFile = "../" + projectFile;
					projectListFile = "../" + projectListFile;
				}

				writer.printf("<a href=\"%s\">%s</a>\r\n", projectListFile, project.getDate());
				writer.printf("<h1>プロジェクト名：<a href=\"%s\">%s</a></h1>\r\n", projectFile, project.getName());
				writer.printf("<h2>ディレクトリ（パッケージ）名：%s</h2>\r\n", dir.toString().replace(indexDir, "").substring(1));
				writer.printf("<hr>\r\n");
				writer.printf("<center>\r\n");

				// ソースファイルの出力
				writer.printf("<table width=\"650\" border=\"1\">\r\n");
				writer.printf("<tr><th bgcolor=\"greenyellow\" colspan=\"3\">クローンを含むソースファイル一覧</th></tr>\r\n");
				writer.printf("<tr bgcolor=\"lightgrey\">\r\n");
				writer.printf("<th width=\"400\">ファイル名</th>\r\n");
				writer.printf("<th width=\"100\">クローン数</th>\r\n");
				writer.printf("<th width=\"150\">備考</th>\r\n");
				writer.printf("</tr>\r\n");

				for (int i = 0; i < files.length; i++) {
					if (files[i].isFile() && !files[i].getName().equals(INDEX_PAGE)) {
						SourceFile file = null;
						for (SourceFile tmpFile : project.getFileList()) {
							if (tmpFile.getName().equals(
									files[i].toString().replace(indexDir, "").replace(".html", "").substring(1))) {
								file = tmpFile;
								break;
							}
						}
						if (file != null) {
							writer.printf("<tr bgcolor=\"%s\">\r\n", getSourceFileColor(file));
							writer.printf("<td><a href=\"%s\">%s</a></td>\r\n", files[i].getName(),
									files[i].getName().replace(".html", ""));
							writer.printf("<td>%d</td>\r\n", file.getNewCloneList().size());
							writer.printf("<td>%s</td>\r\n", getFileState(file));
							writer.println("</tr>\n");
						}
					}
				}

				writer.printf("</table>\n");
				writer.printf("</body>\n");
				writer.printf("</html>\n");

				writer.flush();
				writer.close();

			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * <p>
	 * ディレクトリ削除
	 * </p>
	 * 
	 * @param dir
	 *            削除するディレクトリ
	 */
	private void deleteDir(String dir) {
		File file = new File(dir);
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] f = file.listFiles();
				for (int i = 0; i < f.length; i++) {
					deleteDir(f[i].toString());
				}
			}
			file.delete();
		}
	}

	/**
	 * <p>
	 * クローンセットカラーの取得
	 * </p>
	 * 
	 * @param cloneSet
	 *            クローンセット
	 * @return クローンセットカラー
	 */
	private String getCloneSetColor(CloneSet cloneSet) {
		switch (cloneSet.getCategory()) {
		case CloneSet.NEW:
			return "orange";

		case CloneSet.CHANGED:
			return "greenyellow";

		case CloneSet.DELETED:
			return "tan";

		default:
			return "white";
		}
	}

	/**
	 * <p>
	 * クローンカラーの取得
	 * </p>
	 * 
	 * @param clone
	 *            クローン
	 * @return クローンカラー
	 */
	private String getCloneColor(Clone clone) {
		switch (clone.getCategory()) {
		case Clone.ADDED:
			return "#FFCCE5";

		case Clone.DELETED:
			return "#DBC7AC";

		case Clone.MOVED:
			return "#EFEFA7";

		case Clone.MODIFIED:
			return "#B2D6FF";

		case Clone.DELETE_MODIFIED:
			return "#DBC7AC";

		default:
			return "white";
		}
	}

	/**
	 * <p>
	 * ソースファイルカラーの取得
	 * </p>
	 * 
	 * @param file
	 *            ソースファイル
	 * @return ソースファイルカラー
	 */
	private String getSourceFileColor(SourceFile file) {
		switch (file.getState()) {
		case SourceFile.ADDED:
			return "orange";

		case SourceFile.DELETED:
			return "tan";

		default:
			return "white";
		}
	}

	/**
	 * ソースファイル情報の取得
	 * 
	 * @param file
	 *            ソースファイル
	 * @return - ソースファイル情報
	 */
	private String getFileState(SourceFile file) {
		switch (file.getState()) {
		case SourceFile.ADDED:
			return "追加ファイル";

		case SourceFile.DELETED:
			return "削除ファイル";

		default:
			return " ";
		}
	}

	/**
	 * <p>
	 * HTMLヘッダ部出力
	 * </p>
	 * 
	 * @param writer
	 * @param title
	 */
	private void outputHtmlHead(PrintWriter writer, String title) {
		writer.println("<html>");
		writer.println("<head>");
		writer.println("\t<title>" + title + "</title>");
		writer.printf("\t<style type=\"text/css\">\r\n");
		writer.printf(
				"\t\t.sortable .head {background:gainsboro url(../image/sort.gif) 6px  center no-repeat; cursor:pointer; padding-left:18px}\r\n");
		writer.printf(
				"\t\t.sortable .desc {background:darkgray url(../image/desc.gif) 6px   center no-repeat; cursor:pointer; padding-left:18px}\r\n");
		writer.printf(
				"\t\t.sortable .asc {background:darkgray  url(../image/asc.gif) 6px  center no-repeat; cursor:pointer; padding-left:18px}\r\n");
		writer.printf("\t\t.sortable .head:hover, .sortable .desc:hover, .sortable .asc:hover {color:white}\r\n");
		writer.printf("\t</style>\r\n");
		writer.println("<script type=\"text/javascript\" src=\"../script.js\"></script>");
		writer.println("</head>");
		writer.println("<body>");
	}

	/**
	 * <p>
	 * HTMLヘッダ部出力(ソースファイルの場合)
	 * </p>
	 * 
	 * @param writer
	 * @param file
	 * @return クローンセット一覧HTMLファイル名
	 */
	private String outputHtmlHead(OutputGenerator g, PrintWriter writer, SourceFile file, String projectName) {
		writer.printf("<html>\r\n");
		writer.printf("<head>\n");
		writer.printf("\t<title>%s-%s</title>\r\n", projectName, file.getName());
		writer.printf("\t<style type=\"text/css\">\r\n");
		writer.println("\t\t td,th {font-size:12px}");
		writer.printf("\t\t td xmp {margin:0}\r\n");
		writer.printf("\t</style>\r\n");
		writer.printf("</head>\r\n");
		writer.printf("<body>\r\n");

		String projectListFile = "../" + INDEX_PAGE;
		String projectFile = INDEX_PAGE;
		String cloneSetFile = CLONESETLIST_PAGE;

		// 階層の計算
		String[] tmp = file.getName().split("\\\\");
		for (int i = 0; i < tmp.length - 1; i++) {
			projectFile = "../" + projectFile;
			cloneSetFile = "../" + cloneSetFile;
			projectListFile = "../" + projectListFile;
		}

		// タイトル出力
		writer.printf("<a href=\"%s\">%s</a>\r\n", projectListFile, project.getDate());
		writer.printf("<h1>プロジェクト名：<a href=\"%s\">%s</a></h1>\r\n", projectFile, projectName);
		writer.printf("<h2>ソースファイル：%s</h2>\r\n", file.getName());
		writer.printf("<h4><a href=\"%s\">ソースファイル一覧へ戻る</a></h4>\r\n", INDEX_PAGE);
		writer.printf("<hr>\r\n");

		return cloneSetFile;
	}
}

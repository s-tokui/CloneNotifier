package cn.generate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cn.Def;
import cn.data.Clone;
import cn.data.CloneSet;
import cn.data.Project;

/**
 * <p>CSVファイル生成</p>
 * @param generator OutputGeneratorオブジェクト
 * @param project Projectオブジェクト
 * @return <ul>
 *           <li>成功の場合 - true</li>
 *           <li>失敗の場合 - false</li>
 *         </ul>
 * @author y-yuuki
 */
public class CSVGenerator {

	private Project project = null;
	private static PrintWriter writer = null;

	public CSVGenerator(Project project) {
		this.project = project;
		}

	/**
	 * <p>CSVファイル生成</p>
	 * @param generator OutputGeneratorオブジェクト
	 * @param project Projectオブジェクト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateCSVFile() {
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(project.getGenerateCSVDir()+"//"+ project.getGenerateCSVFileName())));

			writer.print("\"CloneSet ID\",");
			writer.print("\"CATEGORY\",");
			writer.print("\"RISK\",");

			if(project.getTool().equals(Def.CCFX_TOOLNAME)) {
				writer.print("\"LEN\",");
				writer.print("\"POP\",");
				writer.print("\"NIF\",");
				writer.print("\"RAD\",");
				writer.print("\"RNR\",");
				writer.print("\"TKS\",");
				writer.print("\"LOOP\",");
				writer.print("\"COND\",");
				writer.print("\"McCabe\",");
			}
			writer.println();

			for(CloneSet cloneSet: project.getCloneSetList()) {
				writer.print("\"" + cloneSet.getOutputId() + "\",");
				writer.print("\"" + cloneSet.getCategoryString() + "\",");
				writer.print("\"" + cloneSet.getRisk() + "\",");

				if(project.getTool().equals(Def.CCFX_TOOLNAME)) {
					writer.print("\"" + cloneSet.getLEN() + "\",");
					writer.print("\"" + cloneSet.getPOP() + "\",");
					writer.print("\"" + cloneSet.getNIF() + "\",");
					writer.print("\"" + cloneSet.getRAD() + "\",");
					writer.print("\"" + cloneSet.getRNR() + "\",");
					writer.print("\"" + cloneSet.getTKS() + "\",");
					writer.print("\"" + cloneSet.getLOOP() + "\",");
					writer.print("\"" + cloneSet.getCOND() + "\",");
					writer.print("\"" + cloneSet.getMcCabe() + "\"");
				}
				writer.print("\r\n");
			}

			writer.print("--------\r\n");

			writer.print("\"CloneSet ID\",");
			writer.print("\"Clone ID\",");
			writer.print("\"CATEGORY\",");
			writer.print("\"VERSION\",");
			writer.print("\"FILE\",");
			writer.print("\"STARTLINE\",");
			writer.print("\"STARTCOLUMN\",");
			writer.print("\"ENDLINE\",");
			writer.print("\"ENDCOLUMN\",");
			writer.print("\"OLDFILE\",");
			writer.print("\"STARTLINE\",");
			writer.print("\"STARTCOLUMN\",");
			writer.print("\"ENDLINE\",");
			writer.print("\"ENDCOLUMN\"");
			writer.print("\r\n");

			for (CloneSet cloneSet : project.getCloneSetList()) {
				for (Clone clone : cloneSet.getNewCloneList()) {
					writer.print("\"" + cloneSet.getOutputId() + "\",");
					writer.print("\"" + clone.getOutputId() + "\",");
					writer.print("\"" + clone.getCategoryString() + "\",");
					writer.print("\"NEW\",");
					writer.print("\"" + clone.getFile().getName() + "\",");
					writer.printf("%d,", clone.getStartLine());
					writer.printf("%d,", clone.getStartColumn());
					writer.printf("%d,", clone.getEndLine());
					writer.printf("%d", clone.getEndColumn());
					if (clone.getParentClone() != null) {
						clone = clone.getParentClone();
						writer.print(",");
						writer.print("\"" + clone.getFile().getName() + "\",");
						writer.printf("%d,", clone.getStartLine());
						writer.printf("%d,", clone.getStartColumn());
						writer.printf("%d,", clone.getEndLine());
						writer.printf("%d", clone.getEndColumn());
					}
					writer.print("\r\n");
				}

				for (Clone clone : cloneSet.getOldCloneList()) {
					if (clone.getOutputId() == Clone.NULL)
						continue;
					writer.print("\"" + cloneSet.getOutputId() + "\",");
					writer.print("\"" + clone.getOutputId() + "\",");
					writer.print("\"" + clone.getCategoryString() + "\",");
					writer.print("\"OLD\",");
					writer.print(",,,,,");
					writer.print("\"" + clone.getFile().getName() + "\",");
					writer.printf("%d,", clone.getStartLine());
					writer.printf("%d,", clone.getStartColumn());
					writer.printf("%d,", clone.getEndLine());
					writer.printf("%d", clone.getEndColumn());
					writer.print("\r\n");
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}

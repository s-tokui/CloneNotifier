package cn.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import cn.Logger;
import cn.data.Project;

public class JSONFileGenerater {

	private Project project = null;
	private OutputGenerator outputGenerator = null;
	private static PrintWriter writer = null;

	JSONFileGenerater(OutputGenerator outputGenerator, Project project) {
		this.outputGenerator = outputGenerator;
		this.project = project;
	}

	/**
	 * <p>JSONファイル生成</p>
	 * @param generator OutputGeneratorオブジェクト
	 * @param project Projectオブジェクト
	 * @return <ul>
	 *           <li>成功の場合 - true</li>
	 *           <li>失敗の場合 - false</li>
	 *         </ul>
	 */
	public boolean generateJSONFile() {
		try {
			String filename = project.getWorkDir() + "\\" + project.getName() + ".json";

			File file = new File(filename);

			ProjectJson projson = null;
			projson = new ProjectJson();
			projson.setProjectJson(outputGenerator, project);
			ObjectMapper mapper = new ObjectMapper();

			if (file.exists()) {
				JsonNode node = mapper.readTree(file);
				Iterator<JsonNode> itURL = node.get("repositoryURL").elements();
				while (itURL.hasNext()) {
					node = itURL.next();
					DataJson dataJson = new DataJson(node);
					projson.addDataJson(dataJson);
				}
				projson.repositoryURL = node.get("repositoryURL").asText();
				Iterator<JsonNode> itCommitId = node.get("data").elements();
				JsonNode dataNode;
				while (itCommitId.hasNext()) {
					dataNode = itCommitId.next();
					DataJson dataJson = new DataJson(dataNode);
					projson.addDataJson(dataJson);
				}
			}

			writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			writer.println(mapper.writeValueAsString(projson));
			writer.flush();
			writer.close();

		} catch (IOException e) {
			Logger.writeln("Can't Json file.", Logger.ERROR);
		}
		return true;
	}
}

// TODO JsonデータのrepositoryURLを正しく
class ProjectJson {
	public String repositoryURL;
	public ArrayList<DataJson> data;

	ProjectJson() {
		data = new ArrayList<DataJson>();
	}

	public void setProjectJson(OutputGenerator outputGenerator, Project project) {
		this.repositoryURL = project.getNewDir();
		DataJson dataJson = new DataJson(outputGenerator, project);
		this.data.add(dataJson);
	}

	public void addDataJson(DataJson dataJson) {
		data.add(dataJson);
	}
}

// TODO JsonデータのcommitIdを正しく
class DataJson {
	public String commit_id;
	public int stable_cloneset;
	public int changed_cloneset;
	public int deleted_cloneset;
	public int new_cloneset;

	DataJson(OutputGenerator outputGenerator, Project project) {
		this.commit_id = project.getDate();
		this.stable_cloneset = outputGenerator.getStableCloneSetNum();
		this.changed_cloneset = outputGenerator.getChangedCloneSetNum();
		this.deleted_cloneset = outputGenerator.getDeletedCloneSetNum();
		this.new_cloneset = outputGenerator.getNewCloneSetNum();
    }

	DataJson(JsonNode jsonNode) {
		this.commit_id = jsonNode.get("commit_id").asText();
		this.stable_cloneset = jsonNode.get("stable_cloneset").asInt();
		this.changed_cloneset = jsonNode.get("changed_cloneset").asInt();
		this.deleted_cloneset = jsonNode.get("deleted_cloneset").asInt();
		this.new_cloneset = jsonNode.get("new_cloneset").asInt();
	}
}

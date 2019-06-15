package cn.data;

import com.github.gumtreediff.tree.ITree;

public class ActionPair {

	public ITree tree;
	public String actionType;

	
	public ActionPair(ITree tree, String actionType){
		this.tree = tree;
		this.actionType = actionType;
	}
}
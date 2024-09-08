package simlive.model;

import java.util.ArrayList;

import simlive.SimLive;

public class SubTree implements DeepEqualsInterface {
	
	public int nrVertices;
	public int nrFacets;
	public ArrayList<SubTree> subTrees;

	public SubTree() {
		subTrees = new ArrayList<SubTree>();
	}
	
	public SubTree clone() {
		SubTree subTree = new SubTree();
		subTree.nrVertices = nrVertices;
		subTree.nrFacets = nrFacets;
		for (int i = 0; i < subTrees.size(); i++) {
			subTree.subTrees.add(subTrees.get(i).clone());
		}
		return subTree;
	}

	public boolean deepEquals(Object obj) {
		SubTree subTree = (SubTree) obj;
		if (nrVertices != subTree.nrVertices) return false;
		if (nrFacets != subTree.nrFacets) return false;
		if (!SimLive.deepEquals(subTrees, subTree.subTrees)) return false;
		return true;
	}

}

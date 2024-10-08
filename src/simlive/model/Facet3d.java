package simlive.model;

import java.util.Arrays;

public class Facet3d implements DeepEqualsInterface {
	
	private int id;
	private int[] indices = new int[3];
	private int colorID;

	public Facet3d(int[] indices, int colorID) {
		this.indices = indices;
		this.colorID = colorID;
	}

	public int getColorID() {
		return colorID;
	}

	public int[] getIndices() {
		return indices;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}

	public Facet3d clone() {
		Facet3d facet = new Facet3d(this.indices, this.colorID);
		facet.id = this.id;
		return facet;
	}
	
	public boolean deepEquals(Object obj) {
		Facet3d facet = (Facet3d) obj;
		if (this.id != facet.id) return false;
		if (!Arrays.equals(this.indices, facet.indices)) return false;
		if (this.colorID != facet.colorID) return false;
		return true;
	}

}

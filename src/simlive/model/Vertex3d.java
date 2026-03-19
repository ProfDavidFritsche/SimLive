package simlive.model;

import java.util.Arrays;

public class Vertex3d implements DeepEqualsInterface {
	
	private int id;
	private int elementID;
	private double t;
	private double[] r = new double[2];
	
	private double[] coords = new double[3];

	public Vertex3d(double[] coords) {
		this.coords = coords;
		elementID = -1;
	}

	public double[] getCoords() {
		return coords;
	}

	public void setCoords(double[] coords) {
		this.coords = coords;
	}

	public int getElementID() {
		return elementID;
	}

	public void setElementID(int elementID) {
		this.elementID = elementID;
	}

	public double getT() {
		return t;
	}

	public void setT(double t) {
		this.t = t;
	}

	public double[] getR() {
		return r;
	}

	public void setR(double[] r) {
		this.r = r;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}

	public Vertex3d clone() {
		Vertex3d vertex = new Vertex3d(this.coords);
		vertex.id = this.id;
		vertex.elementID = this.elementID;
		vertex.t = this.t;
		vertex.r[0] = this.r[0];
		vertex.r[1] = this.r[1];
		return vertex;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Vertex3d vertex = (Vertex3d) obj;
		if (this.id != vertex.id) return Result.RECALC;
		if (!Arrays.equals(this.coords, vertex.coords)) return Result.RECALC;
		//do not check following because it might detect a wrong model change
		/*if (this.elementID != vertex.elementID) return false;
		if (this.t != vertex.t) return false;
		if (this.r[0] != vertex.r[0]) return false;
		if (this.r[1] != vertex.r[1]) return false;*/
		return result;
	}

}

package simlive.model;

import java.util.Arrays;

import simlive.SimLive;

public class Node implements DeepEqualsInterface {
	
	private double[] coords = new double[3];
	private boolean isRotationalDOF;
	private int id;
	
	public Node(double x, double y, double z) {
		coords[0] = x;
		coords[1] = y;
		coords[2] = z;
		isRotationalDOF = false;
		id = SimLive.model.getNodes().size();
	}
	
	public Node clone() {
		Node node = new Node(coords[0], coords[1], coords[2]);
		node.isRotationalDOF = this.isRotationalDOF;
		node.id = this.id;
		return node;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Node node = (Node) obj;
		if (!Arrays.equals(this.coords, node.coords)) return Result.RECALC;
		if (this.isRotationalDOF != node.isRotationalDOF) return Result.RECALC;
		if (this.id != node.id) return Result.RECALC;
		return result;
	}

	public double[] getCoords() {
		return this.coords;
	}
	
	public void setCoords(double[] coords) {
		this.coords = coords;
	}
	
	public double getXCoord() {
		return coords[0];
	}
	
	public void setXCoord(double x) {
		this.coords[0] = x;
	}

	public double getYCoord() {
		return coords[1];
	}
	
	public void setYCoord(double y) {
		this.coords[1] = y;
	}
	
	public double getZCoord() {
		return coords[2];
	}
	
	public void setZCoord(double z) {
		this.coords[2] = z;
	}
	
	public boolean isRotationalDOF() {
		return isRotationalDOF;
	}

	public void setRotationalDOF(boolean isRotationalDOF) {
		this.isRotationalDOF = isRotationalDOF;
	}

	public int getID() {
		return id;
	}
	
	public void update() {
		/* ID */
		id = SimLive.model.getNodes().indexOf(this);
		
		/* rotationalDOF */
		isRotationalDOF = false;
	}
}

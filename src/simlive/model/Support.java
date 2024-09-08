package simlive.model;

import java.util.ArrayList;
import java.util.Arrays;

import simlive.SimLive;

public class Support implements DeepEqualsInterface {

	private ArrayList<Node> nodes;
	private double[] axis;
	double angle;
	private boolean[] isFixedDisp;
	private boolean[] isFixedRot;
	public String name;
	public boolean isShifted; /* only for display */
	
	public Support () {
		nodes = new ArrayList<Node>();
		axis = new double[3];
		angle = 0.0;
		isFixedDisp = new boolean[3];
		isFixedRot = new boolean[3];
		name = SimLive.model.getDefaultName("Support");
	}
	
	public Support clone(Model model) {
		Support support = new Support();
		for (int i = 0; i < this.nodes.size(); i++) {
			int id = this.nodes.get(i).getID();
			support.nodes.add(model.getNodes().get(id));
		}
		support.axis = Arrays.copyOf(this.axis, 3);
		support.angle = this.angle;
		support.isFixedDisp = Arrays.copyOf(this.isFixedDisp, 3);
		support.isFixedRot = Arrays.copyOf(this.isFixedRot, 3);
		support.name = this.name;
		support.isShifted = this.isShifted;
		return support;
	}
	
	public boolean deepEquals(Object obj) {
		Support support = (Support) obj;
		if (!SimLive.deepEquals(this.nodes, support.nodes)) return false;
		if (!Arrays.equals(this.axis, support.axis)) return false;
		if (this.angle != support.angle) return false;
		if (!Arrays.equals(this.isFixedDisp, support.isFixedDisp)) return false;
		if (!Arrays.equals(this.isFixedRot, support.isFixedRot)) return false;
		if (this.name != support.name) return false;
		return true;
	}
	
	public ArrayList<Node> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<Node> nodes) {
		this.nodes = nodes;
	}

	public double[] getAxis() {
		return axis;
	}

	public void setAxis(double axis, int i) {
		this.axis[i] = axis;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public boolean[] isFixedDisp() {
		return isFixedDisp;
	}

	public void setFixedDisp(boolean isFixedDisp, int i) {
		this.isFixedDisp[i] = isFixedDisp;
	}

	public boolean[] isFixedRot() {
		return isFixedRot;
	}

	public void setFixedRot(boolean isFixedRot, int i) {
		this.isFixedRot[i] = isFixedRot;
	}
	
	public void update() {
		for (int n = nodes.size()-1; n > -1; n--) {
			if (!SimLive.model.getNodes().contains(nodes.get(n))) {
				nodes.remove(n);
			}
		}
	}
}

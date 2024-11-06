package simlive.model;

import java.util.ArrayList;
import java.util.Arrays;

import simlive.SimLive;

public class Load extends AbstractLoad implements DeepEqualsInterface {
	
	public enum Type {FORCE, DISPLACEMENT}
	private ArrayList<Node> nodes;
	private double[] axis;
	private double angle;
	private Type type;
	private double[] force;
	private double[] moment;
	private boolean[] isDisp;
	private boolean[] isRotation;
	private double[] disp;
	private double[] rotation;
	private TimeTable timeTable;
	
	public Load () {
		nodes = new ArrayList<Node>();
		axis = new double[3];
		angle = 0.0;
		type = Type.FORCE;
		isDisp = new boolean[3];
		isRotation = new boolean[3];
		force = new double[3];
		moment = new double[3];
		disp = new double[3];
		rotation = new double[3];
		timeTable = new TimeTable();
		this.name = SimLive.model.getDefaultName("Load");
	}
	
	public Load clone(Model model) {
		Load load = new Load();
		for (int i = 0; i < this.nodes.size(); i++) {
			int id = this.nodes.get(i).getID();
			load.nodes.add(model.getNodes().get(id));
		}
		load.axis = Arrays.copyOf(this.axis, 3);
		load.angle = this.angle;
		load.type = this.type;
		load.force = Arrays.copyOf(this.force, 3);
		load.moment = Arrays.copyOf(this.moment, 3);
		load.isDisp = Arrays.copyOf(this.isDisp, 3);
		load.isRotation = Arrays.copyOf(this.isRotation, 3);
		load.disp = Arrays.copyOf(this.disp, 3);
		load.rotation = Arrays.copyOf(this.rotation, 3);
		if (this.referenceNode != null) {
			load.referenceNode = model.getNodes().get(this.referenceNode.getID());
		}
		load.timeTable = this.timeTable.clone();
		load.name = this.name;
		load.isShifted = this.isShifted;
		return load;
	}
	
	public boolean deepEquals(Object obj) {
		Load load = (Load) obj;
		if (!SimLive.deepEquals(this.nodes, load.nodes)) return false;
		if (!Arrays.equals(this.axis, load.axis)) return false;
		if (this.angle != load.angle) return false;
		if (this.type != load.type) return false;
		if (!Arrays.equals(this.force, load.force)) return false;
		if (!Arrays.equals(this.moment, load.moment)) return false;
		if (!Arrays.equals(this.isDisp, load.isDisp)) return false;
		if (!Arrays.equals(this.isRotation, load.isRotation)) return false;
		if (!Arrays.equals(this.disp, load.disp)) return false;
		if (!Arrays.equals(this.rotation, load.rotation)) return false;
		if (this.referenceNode != null && load.referenceNode != null &&
				!this.referenceNode.deepEquals(load.referenceNode)) return false;
		if (!this.timeTable.deepEquals(load.timeTable)) return false;
		if (this.name != load.name) return false;
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

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public double[] getForce() {
		return force;
	}

	public double[] getForce(double time) {
		double factor = timeTable.getFactorAtTime(time);
		return new double[]{force[0]*factor, force[1]*factor, force[2]*factor};
	}

	public void setForce(double force, int i) {
		this.force[i] = force;
	}
	
	public double[] getMoment() {
		return moment;
	}

	public double[] getMoment(double time) {
		double factor = timeTable.getFactorAtTime(time);
		return new double[]{moment[0]*factor, moment[1]*factor, moment[2]*factor};
	}

	public void setMoment(double moment, int i) {
		this.moment[i] = moment;
	}

	public boolean[] isDisp() {
		return isDisp;
	}
	
	public void setDisp(boolean isDisp, int i) {
		this.isDisp[i] = isDisp;
	}

	public boolean[] isRotation() {
		return isRotation;
	}
	
	public void setRotation(boolean isRotation, int i) {
		this.isRotation[i] = isRotation;
	}

	public double[] getDisp() {
		return disp;
	}

	public double[] getDisp(double time) {
		double factor = timeTable.getFactorAtTime(time);
		return new double[]{disp[0]*factor, disp[1]*factor, disp[2]*factor};
	}

	public void setDisp(double disp, int i) {
		this.disp[i] = disp;
	}

	public double[] getRotation() {
		return rotation;
	}

	public double[] getRotation(double time) {
		double factor = timeTable.getFactorAtTime(time);
		return new double[]{rotation[0]*factor, rotation[1]*factor, rotation[2]*factor};
	}

	public void setRotation(double rotation, int i) {
		this.rotation[i] = rotation;
	}
	
	public TimeTable getTimeTable() {
		return timeTable;
	}

	public void setTimeTable(TimeTable timeTable) {
		this.timeTable = timeTable;
	}

	@Override
	public LoadType getLoadType() {
		return LoadType.LOAD;
	}

	public void update() {
		for (int n = nodes.size()-1; n > -1; n--) {
			if (!SimLive.model.getNodes().contains(nodes.get(n))) {
				nodes.remove(n);
			}
		}
		if (!SimLive.model.getNodes().contains(referenceNode)) {
			referenceNode = null;
		}
	}
}

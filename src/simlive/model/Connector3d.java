package simlive.model;

import java.util.ArrayList;

import simlive.SimLive;

public class Connector3d extends AbstractConnector implements DeepEqualsInterface {
	
	private ArrayList<Part3d> parts3d = new ArrayList<Part3d>();
	private ArrayList<Set> sets = new ArrayList<Set>();
	
	public Connector3d() {
		this.name = SimLive.model.getDefaultName("3D-Connector");
	}
	
	public Connector3d(ArrayList<Part3d> parts3d, ArrayList<Set> lineSets) {
		this.parts3d = parts3d;
		this.sets = lineSets;
		this.name = SimLive.model.getDefaultName("3D-Connector");
	}

	public Connector3d clone(Model model) {
		Connector3d connector3d = new Connector3d();
		for (int i = 0; i < this.parts3d.size(); i++) {
			int id = this.parts3d.get(i).getID();
			connector3d.parts3d.add(model.getParts3d().get(id));
		}
		for (int i = 0; i < this.sets.size(); i++) {
			int id = this.sets.get(i).getID();
			connector3d.sets.add(model.getSets().get(id));
		}
		connector3d.name = this.name;
		return connector3d;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Connector3d connector3d = (Connector3d) obj;
		result = SimLive.deepEquals(parts3d, connector3d.parts3d, result);
		result = SimLive.deepEquals(sets, connector3d.sets, result);
		if (this.name != connector3d.name && result != Result.RECALC) result = Result.CHANGE;
		return result;
	}

	public void setParts3d(ArrayList<Part3d> parts3d) {
		this.parts3d = parts3d;
	}

	public void setParts(ArrayList<Set> sets) {
		this.sets = sets;
	}

	public ArrayList<Part3d> getParts3d() {
		return parts3d;
	}

	public ArrayList<Set> getParts() {
		return sets;
	}

	@Override
	public ConnectorType getConnectorType() {
		return ConnectorType.CONNECTOR_3D;
	}
	
	public void update() {
		if (SimLive.mode != SimLive.Mode.RESULTS) {
			if (!SimLive.model.getParts3d().containsAll(parts3d) || !SimLive.model.getSets().containsAll(sets)) {
				SimLive.model.getConnectors3d().remove(this);
			}
		}
	}

}

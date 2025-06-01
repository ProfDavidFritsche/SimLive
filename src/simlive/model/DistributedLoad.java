package simlive.model;

import java.util.ArrayList;
import java.util.Arrays;

import simlive.SimLive;

public class DistributedLoad extends AbstractLoad implements DeepEqualsInterface {
	
	private double[] startValues;
	private double[] endValues;
	private double[] axis;
	private double angle;
	private boolean isLocalSysAligned;
	private ArrayList<Set> elementSets;
	private TimeTable timeTable;
	
	public DistributedLoad() {
		elementSets = new ArrayList<Set>();
		startValues = new double[3];
		endValues = new double[3];
		axis = new double[3];
		angle = 0.0;
		isLocalSysAligned = false;
		timeTable = new TimeTable();
		this.name = SimLive.model.getDefaultName("Distributed Load");
	}
	
	public DistributedLoad clone(Model model) {
		DistributedLoad distributedLoad = new DistributedLoad();
		for (int i = 0; i < this.elementSets.size(); i++) {
			distributedLoad.elementSets.add(model.getSetByElementsRecursive(model.getSets(),
					this.elementSets.get(i).getElements()));
		}
		distributedLoad.startValues = Arrays.copyOf(this.startValues, 3);
		distributedLoad.endValues = Arrays.copyOf(this.endValues, 3);
		distributedLoad.axis = Arrays.copyOf(this.axis, 3);
		distributedLoad.angle = this.angle;
		distributedLoad.isLocalSysAligned = this.isLocalSysAligned;
		if (this.referenceNode != null) {
			distributedLoad.referenceNode = model.getNodes().get(this.referenceNode.getID());
		}
		distributedLoad.timeTable = this.timeTable.clone();
		distributedLoad.name = this.name;
		distributedLoad.isShifted = this.isShifted;
		return distributedLoad;
	}
	
	public boolean deepEquals(Object obj) {
		DistributedLoad distributedLoad = (DistributedLoad) obj;
		if (!SimLive.deepEquals(elementSets, distributedLoad.elementSets)) return false;
		if (!Arrays.equals(this.startValues, distributedLoad.startValues)) return false;
		if (!Arrays.equals(this.endValues, distributedLoad.endValues)) return false;
		if (!Arrays.equals(this.axis, distributedLoad.axis)) return false;
		if (this.angle != distributedLoad.angle) return false;
		if (this.isLocalSysAligned != distributedLoad.isLocalSysAligned) return false;
		if (this.referenceNode != null && distributedLoad.referenceNode != null &&
			!this.referenceNode.deepEquals(distributedLoad.referenceNode)) return false;
		if (!this.timeTable.deepEquals(distributedLoad.timeTable)) return false;
		if (this.name != distributedLoad.name) return false;
		return true;
	}
	
	public double getStartValue(int comp) {
		return startValues[comp];
	}
	
	public double getStartValue(int comp, double time) {
		return startValues[comp]*timeTable.getFactorAtTime(time);
	}

	public void setStartValue(int comp, double value) {
		this.startValues[comp] = value;
	}
	
	public double getEndValue(int comp) {
		return endValues[comp];
	}
	
	public double getEndValue(int comp, double time) {
		return endValues[comp]*timeTable.getFactorAtTime(time);
	}

	public void setEndValue(int comp, double value) {
		this.endValues[comp] = value;
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

	public boolean isLocalSysAligned() {
		return isLocalSysAligned;
	}

	public void setLocalSysAligned(boolean isLocalSysAligned) {
		this.isLocalSysAligned = isLocalSysAligned;
	}

	public ArrayList<Set> getElementSets() {
		return elementSets;
	}

	public void setElementSets(ArrayList<Set> sets) {
		this.elementSets.clear();
		for (int s = 0; s < sets.size(); s++) {
			this.elementSets.addAll(getBeamSetsRecursive(sets.get(s)));
		}
		for (int s = 0; s < this.elementSets.size(); s++) {
			if (this.elementSets.get(s).getElements().size() == 1) {
				SimLive.model.refineSet(this.elementSets.get(s), SimLive.CONTINUOUS_LOAD_REFINE_LEVEL);
			}
		}
		SimLive.view.redraw();
	}
	
	private ArrayList<Set> getBeamSetsRecursive(Set set) {
		ArrayList<Set> beamSets = new ArrayList<Set>();
		if (set.getSets().isEmpty()) {
			if (SimLive.model.doElementsContainOnlyType(set.getElements(), Element.Type.BEAM)) {
				beamSets.add(set);
			}
		}
		else {
			for (int s = 0; s < set.getSets().size(); s++) {
				beamSets.addAll(getBeamSetsRecursive(set.getSets().get(s)));
			}
		}
		return beamSets;
	}

	public void unrefine() {
		ArrayList<double[]> coords = SimLive.model.getLabelCoords();
		
		for (int s = elementSets.size()-1; s > -1; s--) {
			Set set = elementSets.get(s);
			/* is set referenced by another distributed load? */
			boolean isReferenced = false;
			for (int d = 0; d < SimLive.model.getDistributedLoads().size(); d++) {
				DistributedLoad load = SimLive.model.getDistributedLoads().get(d);
				if (load != this) {
					for (int s1 = 0; s1 < load.getElementSets().size(); s1++) {
						if (load.getElementSets().get(s1) == set) {
							isReferenced = true;
						}
					}
				}
			}
			if (!isReferenced) {
				/* unrefine set */
				Element element = set.getElements().get(0);
				ArrayList<Element> removeElements = set.getElements();
				removeElements.remove(element);
				SimLive.model.getElements().removeAll(removeElements);
				int[] elementNodes = new int[2];
				elementNodes[0] = element.getElementNodes()[0];
				elementNodes[1] = set.getElements().get(set.getElements().size()-1).getElementNodes()[1];
				element.setElementNodes(elementNodes);
				set.getElements().clear();
				set.getElements().add(element);
			}
			elementSets.remove(set);
		}
		
		SimLive.model.mapLabelsToNewMesh(coords);
	}
	
	public TimeTable getTimeTable() {
		return timeTable;
	}
	
	public void setTimeTable(TimeTable timeTable) {
		this.timeTable = timeTable;
	}

	@Override
	public LoadType getLoadType() {
		return LoadType.DISTRIBUTED_LOAD;
	}

	public void update() {
		for (int s = elementSets.size()-1; s > -1; s--) {
			if (!SimLive.model.getElements().containsAll(elementSets.get(s).getElements())) {
				elementSets.remove(s);
			}
		}
		
		for (int s = 0; s < elementSets.size(); s++) {
			Set set = elementSets.get(s);
			Node node0 = set.getNodes().get(0);
			Node node1 = set.getNodes().get(1);
			double[] startCoord = node0.getCoords();
			double[] endCoord = node1.getCoords();
			double[] diff = new double[3];
			diff[0] = endCoord[0]-startCoord[0];
			diff[1] = endCoord[1]-startCoord[1];
			diff[2] = endCoord[2]-startCoord[2];
			double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
			diff[0] /= length;
			diff[1] /= length;
			diff[2] /= length;
			double elemLength = length/(double) (set.getElements().size());
			for (int i = 1; i < set.getElements().size(); i++) {
				int node0ID = set.getElements().get(i).getElementNodes()[0];
				SimLive.model.getNodes().get(node0ID).setXCoord(startCoord[0]+diff[0]*i*elemLength);
				SimLive.model.getNodes().get(node0ID).setYCoord(startCoord[1]+diff[1]*i*elemLength);
				SimLive.model.getNodes().get(node0ID).setZCoord(startCoord[2]+diff[2]*i*elemLength);
			}
		}
		
		if (!SimLive.model.getNodes().contains(referenceNode)) {
			referenceNode = null;
		}
	}
}

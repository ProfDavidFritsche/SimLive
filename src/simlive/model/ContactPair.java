package simlive.model;

import java.util.ArrayList;

import simlive.SimLive;

public class ContactPair implements DeepEqualsInterface {
	
	private ArrayList<Node> slaveNodes = new ArrayList<Node>();
	private ArrayList<Set> masterSets = new ArrayList<Set>();
	private boolean switchContactSide = false;
	private boolean isMaxPenetration = false;
	private double maxPenetration = 0.0;
	private double frictionCoefficient = 0.0;
	private double forwardTol = 0.0;
	private boolean noSeparation = false;
	public enum Type {DEFORMABLE_DEFORMABLE, RIGID_DEFORMABLE}
	private Type type = Type.DEFORMABLE_DEFORMABLE;
	private ArrayList<Element> rigidElements = new ArrayList<Element>();
	private ArrayList<Node> rigidNodes = new ArrayList<Node>();
	public String name;
	
	public ContactPair() {
		this.name = SimLive.model.getDefaultName("Contact");
	}
	
	private void setRigidDeformable() {
		rigidElements.clear();
		rigidNodes.clear();
		for (int s = 0; s < masterSets.size(); s++) {
			Set set = masterSets.get(s);
			for (int elem = 0; elem < set.getElements().size(); elem++) {
				Element element = set.getElements().get(elem);
				int[] elementNodes = element.getElementNodes();
				for (int n = 0; n < elementNodes.length; n++) {
					Node node = SimLive.model.getNodes().get(elementNodes[n]);
					if (!rigidNodes.contains(node)) {
						rigidNodes.add(node);							
						elementNodes[n] = rigidNodes.size()-1;
					}
					else {
						elementNodes[n] = rigidNodes.indexOf(node);
					}
				}
				element.setElementNodes(elementNodes);
				rigidElements.add(element);
			}
			SimLive.model.getSets().remove(set);
			SimLive.model.getElements().removeAll(set.getElements());
		}
	}
	
	private void setDeformableDeformable() {
		int nNodes = SimLive.model.getNodes().size();
		for (int s = 0; s < masterSets.size(); s++) {
			Set set = masterSets.get(s);
			SimLive.model.getSets().add(set);
			for (int elem = 0; elem < set.getElements().size(); elem++) {
				Element element = set.getElements().get(elem);
				int[] elementNodes = element.getElementNodes();
				for (int n = 0; n < elementNodes.length; n++) {
					elementNodes[n] += nNodes;
				}
				element.setElementNodes(elementNodes);
				SimLive.model.getElements().add(element);
			}
		}
		SimLive.model.getNodes().addAll(rigidNodes);
		for (int n = 0; n < rigidNodes.size(); n++) {
			rigidNodes.get(n).update();
		}
		rigidElements.clear();
		rigidNodes.clear();
	}
	
	public ArrayList<Node> getRigidNodes() {
		return rigidNodes;
	}
	
	public ArrayList<Element> getRigidElements() {
		return rigidElements;
	}
	
	public void setType(Type type, boolean update) {
		this.type = type;
		if (update) {
			updateType();
		}
	}

	public Type getType() {
		return type;
	}

	public double getFrictionCoefficient() {
		return frictionCoefficient;
	}

	public void setFrictionCoefficient(double frictionCoefficient) {
		this.frictionCoefficient = frictionCoefficient;
	}

	public double getForwardTol() {
		return forwardTol;
	}

	public boolean isNoSeparation() {
		return noSeparation;
	}

	public void setNoSeparation(boolean noSeparation) {
		this.noSeparation = noSeparation;
	}

	public boolean isMaxPenetration() {
		return isMaxPenetration;
	}

	public void setMaxPenetration(boolean isMaxPenetration) {
		this.isMaxPenetration = isMaxPenetration;
	}

	public double getMaxPenetration() {
		return maxPenetration;
	}

	public void setMaxPenetration(double maxPenetration) {
		this.maxPenetration = maxPenetration;
	}

	public boolean isSwitchContactSide() {
		return switchContactSide;
	}

	public void setSwitchContactSide(boolean switchContactSide) {
		this.switchContactSide = switchContactSide;
	}

	public void setSlave(ArrayList<Node> slaveNodes) {
		this.slaveNodes.clear();
		this.slaveNodes.addAll(slaveNodes);
	}
	
	public void setMaster(ArrayList<Set> masterSets) {
		if (this.type == Type.RIGID_DEFORMABLE) {
			this.setDeformableDeformable();
		}
		this.masterSets.clear();
		this.masterSets.addAll(masterSets);
		if (this.type == Type.RIGID_DEFORMABLE) {
			this.setRigidDeformable();
		}
	}
	
	public ArrayList<Node> getSlaveNodes() {
		return slaveNodes;
	}

	public ArrayList<Set> getMasterSets() {
		return masterSets;
	}
	
	public ContactPair clone(Model model) {
		ContactPair contactPair = new ContactPair();
		for (int i = 0; i < this.slaveNodes.size(); i++) {
			int id = this.slaveNodes.get(i).getID();
			contactPair.slaveNodes.add(model.getNodes().get(id));
		}
		for (int i = 0; i < this.rigidElements.size(); i++) {
			contactPair.rigidElements.add(this.rigidElements.get(i).clone(SimLive.model));
		}
		for (int i = 0; i < this.rigidNodes.size(); i++) {
			contactPair.rigidNodes.add(this.rigidNodes.get(i).clone());
		}
		if (this.type == Type.DEFORMABLE_DEFORMABLE) {
			for (int i = 0; i < this.masterSets.size(); i++) {
				int id = this.masterSets.get(i).getID();
				contactPair.masterSets.add(model.getSets().get(id));
			}
		}
		if (this.type == Type.RIGID_DEFORMABLE) {
			for (int i = 0; i < this.masterSets.size(); i++) {
				contactPair.masterSets.add(this.masterSets.get(i).clone(model));
				contactPair.masterSets.get(i).getElements().clear();
				for (int e = 0; e < this.masterSets.get(i).getElements().size(); e++) {
					int index = this.rigidElements.indexOf(this.masterSets.get(i).getElements().get(e));
					Element element = contactPair.rigidElements.get(index);
					contactPair.masterSets.get(i).getElements().add(element);
				}
			}
		}
		contactPair.switchContactSide = this.switchContactSide;
		contactPair.isMaxPenetration = this.isMaxPenetration;
		contactPair.maxPenetration = this.maxPenetration;
		contactPair.frictionCoefficient = this.frictionCoefficient;
		contactPair.forwardTol = this.forwardTol;
		contactPair.noSeparation = this.noSeparation;
		contactPair.type = this.type;
		contactPair.name = this.name;
		return contactPair;
	}
	
	public boolean deepEquals(Object obj) {
		ContactPair contactPair = (ContactPair) obj;
		if (!SimLive.deepEquals(this.slaveNodes, contactPair.slaveNodes)) return false;
		if (type == Type.DEFORMABLE_DEFORMABLE) {
			if (!SimLive.deepEquals(this.masterSets, contactPair.masterSets)) return false;
		}
		if (!SimLive.deepEquals(this.rigidElements, contactPair.rigidElements)) return false;
		if (!SimLive.deepEquals(this.rigidNodes, contactPair.rigidNodes)) return false;
		if (this.switchContactSide != contactPair.switchContactSide) return false;
		if (this.isMaxPenetration != contactPair.isMaxPenetration) return false;
		if (this.maxPenetration != contactPair.maxPenetration) return false;
		if (this.frictionCoefficient != contactPair.frictionCoefficient) return false;
		if (this.forwardTol != contactPair.forwardTol) return false;
		if (this.noSeparation != contactPair.noSeparation) return false;
		if (this.type != contactPair.type) return false;
		if (this.name != contactPair.name) return false;
		return true;
	}
	
	private void updateForwardTol() {
		ArrayList<Node> nodes = type == Type.DEFORMABLE_DEFORMABLE ? SimLive.model.getNodes() : rigidNodes;
		double sumEdge = 0.0;
		int count = 0;
		for (int s = 0; s < masterSets.size(); s++) {
			for (int e = 0; e < masterSets.get(s).getElements().size(); e++) {
				Element element = masterSets.get(s).getElements().get(e);
				int[] elemNodes = element.getElementNodes();
				for (int i = 0; i < elemNodes.length; i++) {
					double[] coords0 = nodes.get(elemNodes[i]).getCoords();
					double[] coords1 = nodes.get(elemNodes[(i+1)%elemNodes.length]).getCoords();
					sumEdge += Math.sqrt((coords1[0]-coords0[0])*(coords1[0]-coords0[0])+
										 (coords1[1]-coords0[1])*(coords1[1]-coords0[1])+
										 (coords1[2]-coords0[2])*(coords1[2]-coords0[2]));
					count++;
				}
			}
		}
		if (count > 0) forwardTol = sumEdge/count*SimLive.ZERO_TOL;
	}
	
	private void updateType() {
		if (type == Type.DEFORMABLE_DEFORMABLE) {
			if (!rigidElements.isEmpty() && !rigidNodes.isEmpty()) {
				setDeformableDeformable();
			}
		}
		if (type == Type.RIGID_DEFORMABLE) {
			if (rigidElements.isEmpty() && rigidNodes.isEmpty()) {
				setRigidDeformable();
			}
		}
	}
	
	public void update() {
		updateType();
		slaveNodes.retainAll(SimLive.model.getNodes());
		if (type == Type.DEFORMABLE_DEFORMABLE) {
			masterSets.retainAll(SimLive.model.getSets());
		}
		updateForwardTol();
	}

}

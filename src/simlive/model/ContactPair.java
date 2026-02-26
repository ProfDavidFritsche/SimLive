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
	private ArrayList<Element> outline = new ArrayList<Element>();
	private ArrayList<Node> outlineNodes = new ArrayList<Node>();
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
		generateOutline();
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
		for (int i = 0; i < this.outline.size(); i++) {
			contactPair.outline.add(this.outline.get(i).clone(SimLive.model));
		}
		for (int i = 0; i < this.outlineNodes.size(); i++) {
			contactPair.outlineNodes.add(this.outlineNodes.get(i).clone());
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
	
	public Result deepEquals(Object obj, Result result) {
		ContactPair contactPair = (ContactPair) obj;
		result = SimLive.deepEquals(this.slaveNodes, contactPair.slaveNodes, result);
		if (type == Type.DEFORMABLE_DEFORMABLE) {
			result = SimLive.deepEquals(this.masterSets, contactPair.masterSets, result);
		}
		result = SimLive.deepEquals(this.rigidElements, contactPair.rigidElements, result);
		result = SimLive.deepEquals(this.rigidNodes, contactPair.rigidNodes, result);
		result = SimLive.deepEquals(this.outline, contactPair.outline, result);
		result = SimLive.deepEquals(this.outlineNodes, contactPair.outlineNodes, result);
		if (this.switchContactSide != contactPair.switchContactSide) return Result.RECALC;
		if (this.isMaxPenetration != contactPair.isMaxPenetration) return Result.RECALC;
		if (this.maxPenetration != contactPair.maxPenetration) return Result.RECALC;
		if (this.frictionCoefficient != contactPair.frictionCoefficient) return Result.RECALC;
		if (this.forwardTol != contactPair.forwardTol) return Result.RECALC;
		if (this.noSeparation != contactPair.noSeparation) return Result.RECALC;
		if (this.type != contactPair.type) return Result.RECALC;
		if (this.name != contactPair.name && result != Result.RECALC) result = Result.CHANGE;
		return result;
	}
	
	private int[][] getOutlineEdge(ArrayList<Element> elements) {
		int maxNodeNr = 0;
		for (int e = 0; e < elements.size(); e++) {
			Element masterElement = elements.get(e);
			int[] element_nodes = masterElement.getElementNodes();
			for (int i = 0; i < element_nodes.length; i++) {
				if (element_nodes[i] > maxNodeNr) maxNodeNr = element_nodes[i];
			}
		}
		int[][] outlineEdge = new int[maxNodeNr+1][0];
		
		for (int e = 0; e < elements.size(); e++) {
			Element masterElement = elements.get(e);
			int[] element_nodes = masterElement.getElementNodes();
			for (int i = 0; i < element_nodes.length; i++) {
				int n0 = element_nodes[i];
				int n1 = element_nodes[(i+1)%element_nodes.length];
				outlineEdge[n0] = SimLive.add(outlineEdge[n0], n1);
				if (SimLive.contains(outlineEdge[n1], n0)) {
					outlineEdge[n0] = SimLive.remove(outlineEdge[n0], n1);
					outlineEdge[n1] = SimLive.remove(outlineEdge[n1], n0);
				}
			}
		}
		return outlineEdge;
	}
	
	private void generateOutline() {
		outline.clear();
		outlineNodes.clear();
		ArrayList<Element> elements = new ArrayList<Element>();
		if (type == ContactPair.Type.DEFORMABLE_DEFORMABLE) {
			for (int s = 0; s < masterSets.size(); s++) {
				elements.addAll(masterSets.get(s).getElements());
			}
		}
		else {
			elements.addAll(rigidElements);
		}
		int[][] outlineEdge = getOutlineEdge(elements);
		
		for (int e = 0; e < elements.size(); e++) {
			int[] elemNodes = elements.get(e).getElementNodes();
			for (int i = 0; i < elemNodes.length; i++) {
				if (SimLive.contains(outlineEdge[elemNodes[i]], elemNodes[(i+1)%elemNodes.length])) {
					Node node0 = null;
					Node node1 = null;
					if (type == ContactPair.Type.DEFORMABLE_DEFORMABLE) {
						node0 = SimLive.model.getNodes().get(elemNodes[i]);
						node1 = SimLive.model.getNodes().get(elemNodes[(i+1)%elemNodes.length]);
					}
					else {
						node0 = rigidNodes.get(elemNodes[i]);
						node1 = rigidNodes.get(elemNodes[(i+1)%elemNodes.length]);
					}
					int[] elemNodes0 = new int[2];
					elemNodes0[0] = outlineNodes.indexOf(node0);
					if (elemNodes0[0] == -1) {
						outlineNodes.add(node0);
						elemNodes0[0] = outlineNodes.size()-1;
					}
					elemNodes0[1] = outlineNodes.indexOf(node1);
					if (elemNodes0[1] == -1) {
						outlineNodes.add(node1);
						elemNodes0[1] = outlineNodes.size()-1;
					}
					Rod rod = new Rod(elemNodes0);
					rod.setQ0(new double[]{0, 0, 1});
					outline.add(rod);
				}
			}
		}
	}
	
	public boolean hasStoredEdges() {
		for (int e = 0; e < outline.size(); e++) {
			if (((Rod) outline.get(e)).isStoredEdge()) return true;
		}
		return false;
	}

	public ArrayList<Element> getOutline() {
		return outline;
	}
	
	public ArrayList<Node> getOutlineNodes() {
		return outlineNodes;
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
	
	private boolean checkOutline() {
		ArrayList<Element> elements = new ArrayList<Element>();
		for (int s = 0; s < masterSets.size(); s++) {
			elements.addAll(masterSets.get(s).getElements());
		}
		int[][] outlineEdge = getOutlineEdge(elements);
		int nrFound = 0;
		for (int e = 0; e < elements.size(); e++) {
			int[] elemNodes = elements.get(e).getElementNodes();
			for (int i = 0; i < elemNodes.length; i++) {
				if (SimLive.contains(outlineEdge[elemNodes[i]], elemNodes[(i+1)%elemNodes.length])) {
					Node n0 = SimLive.model.getNodes().get(elemNodes[i]);
					Node n1 = SimLive.model.getNodes().get(elemNodes[(i+1)%elemNodes.length]);
					int e0;
					for (e0 = 0; e0 < outline.size(); e0++) {
						Node node0 = outlineNodes.get(outline.get(e0).getElementNodes()[0]);
						Node node1 = outlineNodes.get(outline.get(e0).getElementNodes()[1]);
						if (node0.getXCoord() == n0.getXCoord() &&
							node0.getYCoord() == n0.getYCoord() &&
							node1.getXCoord() == n1.getXCoord() &&
							node1.getYCoord() == n1.getYCoord()) {
							
							nrFound++;
							break;
						}
					}
					if (e0 == outline.size()) return false;
				}
			}
		}
		return nrFound == outline.size();
	}
	
	public void update() {
		updateType();
		slaveNodes.retainAll(SimLive.model.getNodes());
		if (type == Type.DEFORMABLE_DEFORMABLE) {
			masterSets.retainAll(SimLive.model.getSets());
			
			/* generate if outline does not match master sets */
			if (!checkOutline()) generateOutline();
		}
		updateForwardTol();
	}

}
package simlive.solution;

import java.util.ArrayList;
import java.util.stream.Stream;

import Jama.Matrix;
import simlive.SimLive;
import simlive.model.Beam;
import simlive.model.ContactPair;
import simlive.model.Element;
import simlive.model.LineElement;
import simlive.model.Node;
import simlive.model.PlaneElement;
import simlive.model.Rod;
import simlive.model.SectionShape;
import simlive.model.ContactPair.Type;

public class Contact {
	
	private Element masterElement;
	private double[] shapeFunctionValues;
	private double penetration;
	private double[] norm;
	private double frictionCoefficient;
	private boolean isSticking;
	private static ArrayList<Node> slaveNodes;
	private static int[][] slaveNodeElements;
	private boolean isDeformableDeformable;

	public Contact(Element masterElement, double penetration, double[] norm, double frictionCoefficient, double[] shapeFunctionValues, boolean isDeformableDeformable) {
		this.masterElement = masterElement;
		this.penetration = penetration;
		this.norm = norm;
		this.frictionCoefficient = frictionCoefficient;
		this.isSticking = true;
		this.shapeFunctionValues = shapeFunctionValues;
		this.isDeformableDeformable = isDeformableDeformable;
	}

	public Element getMasterElement() {
		return masterElement;
	}

	public double getPenetration() {
		return penetration;
	}

	public double[] getNorm() {
		return norm;
	}
	
	public double getFrictionCoefficient() {
		return frictionCoefficient;
	}

	public boolean isSticking() {
		return isSticking;
	}

	public void setSticking(boolean isSticking) {
		this.isSticking = isSticking;
	}

	public double[] getShapeFunctionValues() {
		return shapeFunctionValues;
	}
	
	public boolean isDeformableDeformable() {
		return isDeformableDeformable;
	}
	
	public static void generateSlaveNodeList(ArrayList<ContactPair> contactPairs, ArrayList<Element> elements, int nrNodes) {
		slaveNodes = new ArrayList<Node>();
		slaveNodeElements = new int[nrNodes][0];
		for (int c = 0; c < contactPairs.size(); c++) {
			for (int n = 0; n < contactPairs.get(c).getSlaveNodes().size(); n++) {
				if (!slaveNodes.contains(contactPairs.get(c).getSlaveNodes().get(n))) {
					int id = contactPairs.get(c).getSlaveNodes().get(n).getID();
					slaveNodes.add(contactPairs.get(c).getSlaveNodes().get(n));
					for (int e = 0; e < elements.size(); e++) {
						int[] elemNodes = elements.get(e).getElementNodes();
						for (int i = 0; i < elemNodes.length; i++) {
							if (elemNodes[i] == id) {
								slaveNodeElements[id] = SimLive.add(slaveNodeElements[id], e);
							}
						}
					}
				}
			}
		}
	}
	
	private static double[] getMasterNodeCoords(ContactPair contactPair, int k, int[] masterNodeIDs, Solution solution, Matrix u_global) {
		double[] masterNodeCoords = new double[3];
		if (contactPair.getType() == Type.DEFORMABLE_DEFORMABLE) {
			Node masterNode = solution.getRefModel().getNodes().get(masterNodeIDs[k]);
			int dofMasterNode = solution.getDofOfNodeID(masterNodeIDs[k]);
			masterNodeCoords[0] = masterNode.getXCoord() + u_global.get(dofMasterNode, 0);
			masterNodeCoords[1] = masterNode.getYCoord() + u_global.get(dofMasterNode+1, 0);
			masterNodeCoords[2] = masterNode.getZCoord() + u_global.get(dofMasterNode+2, 0);
		}
		if (contactPair.getType() == Type.RIGID_DEFORMABLE) {
			Node masterNode = contactPair.getRigidNodes().get(masterNodeIDs[k]);
			masterNodeCoords[0] = masterNode.getXCoord();
			masterNodeCoords[1] = masterNode.getYCoord();
			masterNodeCoords[2] = masterNode.getZCoord();
		}
		return masterNodeCoords;
	}
	
	public static void search(Contact[] contacts, ArrayList<ContactPair> contactPairs,
			Solution solution, Matrix u_global, Matrix u_global0, Matrix C_global) {
		
		/* do search for all slave nodes */
		Stream<Node> stream = slaveNodes.parallelStream();
		stream.forEach(slaveNode -> {
		
			double[] coords = new double[3];
			int slaveNodeID = slaveNode.getID();
			int dofNode = solution.getDofOfNodeID(slaveNodeID);
			coords[0] = slaveNode.getXCoord() + u_global.get(dofNode, 0);
			coords[1] = slaveNode.getYCoord() + u_global.get(dofNode+1, 0);
			coords[2] = slaveNode.getZCoord() + u_global.get(dofNode+2, 0);
					
			/* search master element with max penetration, store in masterElement0 */
			Element masterElement0 = null;
			double maxPenetration = -Double.MAX_VALUE;
			double frictionCoefficient = 0.0;
			double forwardTol = 0.0;
			boolean noSeparation = false;
			boolean isDeformableDeformable = false;
			double[] masterNormals0 = new double[2];
			
			for (int c = 0; c < contactPairs.size(); c++) if (contactPairs.get(c).getSlaveNodes().contains(slaveNode)) {
				ContactPair contactPair = contactPairs.get(c);
				
				ArrayList<Element> masterElements = new ArrayList<Element>();
				for (int s = 0; s < contactPair.getMasterSets().size(); s++) {
					masterElements.addAll(contactPair.getMasterSets().get(s).getElements());
				}
				
				/* store data used for search */
				int arraySize = solution.getRefModel().getNodes().size();
				if (contactPair.getType() == Type.RIGID_DEFORMABLE) {
					arraySize = contactPair.getRigidNodes().size();
				}
				double[][] masterNodeCoords = new double[arraySize][3];
				double[][] masterNodeNormals = new double[arraySize][3];
				double[][] masterNormals = new double[masterElements.size()][3];
				
				for (int e = 0; e < masterElements.size(); e++) {
					Element masterElement = masterElements.get(e);
					int[] masterElemNodes = masterElement.getElementNodes();
					for (int k = 0; k < masterElemNodes.length; k++) {
						masterNodeCoords[masterElemNodes[k]] = getMasterNodeCoords(contactPair, k, masterElemNodes, solution, u_global);
					}
					int k0 = masterElemNodes[0];
					int k1 = masterElemNodes[1];
					int k2 = masterElemNodes[2];
					int k3 = masterElemNodes[masterElemNodes.length-1];
					double[] a = new double[3];
					a[0] = masterNodeCoords[k2][0] - masterNodeCoords[k0][0];
					a[1] = masterNodeCoords[k2][1] - masterNodeCoords[k0][1];
					a[2] = masterNodeCoords[k2][2] - masterNodeCoords[k0][2];
					double[] b = new double[3];
					b[0] = masterNodeCoords[k3][0] - masterNodeCoords[k1][0];
					b[1] = masterNodeCoords[k3][1] - masterNodeCoords[k1][1];
					b[2] = masterNodeCoords[k3][2] - masterNodeCoords[k1][2];
					masterNormals[e][0] = a[1]*b[2]-a[2]*b[1];
					masterNormals[e][1] = a[2]*b[0]-a[0]*b[2];
					masterNormals[e][2] = a[0]*b[1]-a[1]*b[0];
					double length = Math.sqrt(masterNormals[e][0]*masterNormals[e][0]+masterNormals[e][1]*masterNormals[e][1]+masterNormals[e][2]*masterNormals[e][2]);
					double masterSign = contactPair.isSwitchContactSide() ? 1.0 : -1.0;
					masterNormals[e][0] /= masterSign*length;
					masterNormals[e][1] /= masterSign*length;
					masterNormals[e][2] /= masterSign*length;
					for (int k = 0; k < masterElemNodes.length; k++) {
						masterNodeNormals[masterElemNodes[k]][0] += masterSign*masterNormals[e][0];
						masterNodeNormals[masterElemNodes[k]][1] += masterSign*masterNormals[e][1];
						masterNodeNormals[masterElemNodes[k]][2] += masterSign*masterNormals[e][2];
					}
				}
				
				for (int e = 0; e < masterElements.size(); e++) {
					Element masterElement = masterElements.get(e);
					
					boolean inside = true;
					int[] masterElemNodes = masterElement.getElementNodes();
					for (int k = 0; k < masterElemNodes.length; k++) {
						int k0 = masterElemNodes[k];
						int k1 = masterElemNodes[(k+1)%masterElemNodes.length];
						double[] a = new double[3];
						a[0] = masterNodeCoords[k1][0] - masterNodeCoords[k0][0];
						a[1] = masterNodeCoords[k1][1] - masterNodeCoords[k0][1];
						a[2] = masterNodeCoords[k1][2] - masterNodeCoords[k0][2];
						double[] masterEdgeNormal0 = new double[3];
						masterEdgeNormal0[0] = a[1]*masterNodeNormals[k0][2]-a[2]*masterNodeNormals[k0][1];
						masterEdgeNormal0[1] = a[2]*masterNodeNormals[k0][0]-a[0]*masterNodeNormals[k0][2];
						masterEdgeNormal0[2] = a[0]*masterNodeNormals[k0][1]-a[1]*masterNodeNormals[k0][0];
						double length = Math.sqrt(masterEdgeNormal0[0]*masterEdgeNormal0[0]+masterEdgeNormal0[1]*masterEdgeNormal0[1]+masterEdgeNormal0[2]*masterEdgeNormal0[2]);
						masterEdgeNormal0[0] /= length;
						masterEdgeNormal0[1] /= length;
						masterEdgeNormal0[2] /= length;						
						double[] masterEdgeNormal1 = new double[3];
						masterEdgeNormal1[0] = a[1]*masterNodeNormals[k1][2]-a[2]*masterNodeNormals[k1][1];
						masterEdgeNormal1[1] = a[2]*masterNodeNormals[k1][0]-a[0]*masterNodeNormals[k1][2];
						masterEdgeNormal1[2] = a[0]*masterNodeNormals[k1][1]-a[1]*masterNodeNormals[k1][0];						
						length = Math.sqrt(masterEdgeNormal1[0]*masterEdgeNormal1[0]+masterEdgeNormal1[1]*masterEdgeNormal1[1]+masterEdgeNormal1[2]*masterEdgeNormal1[2]);
						masterEdgeNormal1[0] /= length;
						masterEdgeNormal1[1] /= length;
						masterEdgeNormal1[2] /= length;						
						double[] diff = new double[3];
						diff[0] = coords[0] - masterNodeCoords[k0][0];
						diff[1] = coords[1] - masterNodeCoords[k0][1];
						diff[2] = coords[2] - masterNodeCoords[k0][2];
						length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
						diff[0] /= length;
						diff[1] /= length;
						diff[2] /= length;						
						
						if (masterEdgeNormal0[0]*diff[0]+masterEdgeNormal0[1]*diff[1]+masterEdgeNormal0[2]*diff[2] > SimLive.ZERO_TOL &&
							masterEdgeNormal1[0]*diff[0]+masterEdgeNormal1[1]*diff[1]+masterEdgeNormal1[2]*diff[2] > SimLive.ZERO_TOL) {
							inside = false;
							break;
						}						
					}
					
					if (inside) {
						
						double[] diff = new double[3];
						diff[0] = coords[0] - masterNodeCoords[masterElemNodes[0]][0];
						diff[1] = coords[1] - masterNodeCoords[masterElemNodes[0]][1];
						diff[2] = coords[2] - masterNodeCoords[masterElemNodes[0]][2];
						
						double penetration = diff[0]*masterNormals[e][0]+diff[1]*masterNormals[e][1]+diff[2]*masterNormals[e][2];
						
						if (contactPair.getType() == Type.DEFORMABLE_DEFORMABLE) {
							double halfThickness = ((PlaneElement) masterElement).getThickness()/2.0;
							penetration += halfThickness;
						}
						double slaveNodeHalfThickness = 0;
						ArrayList<Node> nodes = solution.getRefModel().getNodes();
						double[] planeNormal = new double[3];
						for (int e1 = 0; e1 < slaveNodeElements[slaveNodeID].length; e1++) {
							Element element = solution.getRefModel().getElements().get(slaveNodeElements[slaveNodeID][e1]);
							Matrix u_elem0 = element.globalToLocalVector(u_global0);
							if (element.isPlaneElement()) {
								double[] normal = ((PlaneElement) element).getRr(nodes, u_elem0).getMatrix(0, 2, 2, 2).getColumnPackedCopy();
								planeNormal[0] += normal[0];
								planeNormal[1] += normal[1];
								planeNormal[2] += normal[2];
							}
							if ((element.getType() == Element.Type.BEAM || element.getType() == Element.Type.ROD) &&
									solution.getRefModel().getSections().get(((LineElement) element).getSectionID()).getSectionShape().getType() !=
									SectionShape.Type.DIRECT_INPUT) {
								double length = ((LineElement) element).getCurrentLength(nodes, u_elem0);
								Matrix r1 = ((LineElement) element).getr1(nodes, u_elem0, length);
								Matrix Rr = null;
								if (element.getType() == Element.Type.BEAM) {
									Rr = ((Beam) element).getRr(u_elem0, r1);
								}
								if (element.getType() == Element.Type.ROD) {
									Rr = ((Rod) element).getVectorTransformation(r1.getColumnPackedCopy());
								}
								if (Rr != null) {
									SectionShape sectionShape = solution.getRefModel().getSections().get(((LineElement) element).getSectionID()).getSectionShape();
									double w, h;
									if (sectionShape.getType() == SectionShape.Type.CIRCLE || sectionShape.getType() == SectionShape.Type.HOLLOW_CIRCLE) {
										w = h = sectionShape.getDiameter();
									}
									else {
										w = sectionShape.getWidth();
										h = sectionShape.getHeight();
									}
									double[] normal = Rr.getMatrix(0, 2, 1, 1).getColumnPackedCopy();
									double scal = normal[0]*masterNormals[e][0]+normal[1]*masterNormals[e][1]+normal[2]*masterNormals[e][2];
									slaveNodeHalfThickness = Math.max(slaveNodeHalfThickness, w/2.0*Math.abs(scal));
									normal = Rr.getMatrix(0, 2, 2, 2).getColumnPackedCopy();
									scal = normal[0]*masterNormals[e][0]+normal[1]*masterNormals[e][1]+normal[2]*masterNormals[e][2];
									slaveNodeHalfThickness = Math.max(slaveNodeHalfThickness, h/2.0*Math.abs(scal));
								}
							}
						}
						double length = Math.sqrt(planeNormal[0]*planeNormal[0]+planeNormal[1]*planeNormal[1]+planeNormal[2]*planeNormal[2]);
						if (length > 0) {
							planeNormal[0] /= length;
							planeNormal[1] /= length;
							planeNormal[2] /= length;
							double scal = planeNormal[0]*masterNormals[e][0]+planeNormal[1]*masterNormals[e][1]+planeNormal[2]*masterNormals[e][2];
							for (int e1 = 0; e1 < slaveNodeElements[slaveNodeID].length; e1++) {
								Element element = solution.getRefModel().getElements().get(slaveNodeElements[slaveNodeID][e1]);
								if (element.isPlaneElement()) {
									slaveNodeHalfThickness = Math.max(slaveNodeHalfThickness, ((PlaneElement) element).getThickness()/2.0*Math.abs(scal));
								}
							}
						}
						penetration += slaveNodeHalfThickness;
								
						if (penetration > maxPenetration &&
								(!contactPair.isMaxPenetration() || penetration < contactPair.getMaxPenetration())) {
							maxPenetration = penetration;
							masterElement0 = masterElement;
							frictionCoefficient = contactPair.getFrictionCoefficient();
							forwardTol = contactPair.getForwardTol();
							noSeparation = contactPair.isNoSeparation();
							isDeformableDeformable = contactPair.getType() == Type.DEFORMABLE_DEFORMABLE;
							masterNormals0 = masterNormals[e];
						}
					}
				}
			}
				
			/* check contact with masterElement0 */
			if (masterElement0 != null) {
				
				double contactForce = 0.0;
				if (C_global != null) {
					contactForce = C_global.get(dofNode, 0)*masterNormals0[0]+C_global.get(dofNode+1, 0)*masterNormals0[1]+C_global.get(dofNode+2, 0)*masterNormals0[2];
				}
				
				if ((contacts[slaveNodeID] == null && maxPenetration > -forwardTol) ||
					(contacts[slaveNodeID] != null && ((maxPenetration > -forwardTol && contactForce <= 0.0 && C_global != null) ||
					 noSeparation))) {
					
					double[] shapeFunctionValues = null;
					if (isDeformableDeformable) {
						if (contacts[slaveNodeID] != null && contacts[slaveNodeID].isSticking && contacts[slaveNodeID].getShapeFunctionValues() != null) {
							shapeFunctionValues = contacts[slaveNodeID].getShapeFunctionValues();
						}
						else {
							double[] r = ((PlaneElement) masterElement0).getLocalFromGlobalCoordinates(coords);
							shapeFunctionValues = ((PlaneElement) masterElement0).getShapeFunctionValues(r[0], r[1]);
						}
					}
					contacts[slaveNodeID] = new Contact(masterElement0, maxPenetration, masterNormals0, frictionCoefficient, shapeFunctionValues, isDeformableDeformable);
				}
				else {
					contacts[slaveNodeID] = null;
				}
			}
			else {
				contacts[slaveNodeID] = null;
			}
		});
	}
	
}

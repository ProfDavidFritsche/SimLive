package simlive.model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TreeItem;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.misc.GeomUtility;
import simlive.misc.Settings;
import simlive.view.Label;
import simlive.view.View;

public class Model implements DeepEqualsInterface {

	public Settings settings;
	private ArrayList<Node> nodes;
	private ArrayList<Element> elements;
	private ArrayList<Material> materials;
	private ArrayList<Section> sections;
	private ArrayList<Step> steps;
	private ArrayList<Set> sets;
	private ArrayList<Part3d> parts3d;
	private ArrayList<Part3dColor> part3dColors;
	private ArrayList<Support> supports;
	private ArrayList<Connector> connectors;
	private ArrayList<Connector3d> connectors3d;
	private ArrayList<ContactPair> contactPairs;
	private ArrayList<Load> loads;
	private ArrayList<DistributedLoad> distributedLoads;
	public static boolean twoDimensional = false;
	public static int maxUsedNodeID = -1;
	
	public Model() {
		settings = new Settings();
		nodes = new ArrayList<Node>();
		elements = new ArrayList<Element>();
		materials = new ArrayList<Material>();
		sections = new ArrayList<Section>();
		steps = new ArrayList<Step>();
		sets = new ArrayList<Set>();
		parts3d = new ArrayList<Part3d>();
		part3dColors = new ArrayList<Part3dColor>();
		supports = new ArrayList<Support>();
		connectors = new ArrayList<Connector>();
		connectors3d = new ArrayList<Connector3d>();
		contactPairs = new ArrayList<ContactPair>();
		loads = new ArrayList<Load>();
		distributedLoads = new ArrayList<DistributedLoad>();
	}
	
	public String getDefaultName(String defaultBaseName) {
		int nr = 1;
		switch (defaultBaseName) {
			case "Material":
				for (int i = 0; i < materials.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, materials.get(i).name, nr);
				}
				break;
			case "Step":
				for (int i = 0; i < steps.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, steps.get(i).name, nr);
				}
				break;
			case "Support":
				for (int i = 0; i < supports.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, supports.get(i).name, nr);
				}
				break;
			case "Connector":
				for (int i = 0; i < connectors.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, connectors.get(i).name, nr);
				}
				break;
			case "3D-Connector":
				for (int i = 0; i < connectors3d.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, connectors3d.get(i).name, nr);
				}
				break;
			case "Contact":
				for (int i = 0; i < contactPairs.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, contactPairs.get(i).name, nr);
				}
				break;
			case "Load":
				for (int i = 0; i < loads.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, loads.get(i).name, nr);
				}
				break;
			case "Distributed Load":
				for (int i = 0; i < distributedLoads.size(); i++) {
					nr = getDefaultNameNr(defaultBaseName, distributedLoads.get(i).name, nr);
				}
				break;
			default:
				return null;
		}
		
		return defaultBaseName+" "+Integer.toString(nr);
	}
	
	private int getDefaultNameNr(String defaultBaseName, String name, int nr) {
		if (name.startsWith(defaultBaseName+" ")) {
			String str = name.replace(defaultBaseName+" ", "");
			try {
				int newNr = Integer.parseInt(str)+1;
				if (newNr > nr) {
					return newNr;
				}
			}
			catch (NumberFormatException e) {}
		}
		return nr;
	}
	
	public ArrayList<Node> getNodes() {
		return nodes;
	}

	public ArrayList<Element> getElements() {
		return elements;
	}

	public ArrayList<Material> getMaterials() {
		return materials;
	}

	public ArrayList<Section> getSections() {
		return sections;
	}
	
	public ArrayList<Step> getSteps() {
		return steps;
	}
	
	public ArrayList<Set> getSets() {
		return sets;
	}
	
	public ArrayList<Part3d> getParts3d() {
		return parts3d;
	}
	
	public ArrayList<Part3dColor> getPart3dColors() {
		return part3dColors;
	}
	
	public ArrayList<Support> getSupports() {
		return supports;
	}
	
	public ArrayList<Connector> getConnectors() {
		return connectors;
	}
	
	public ArrayList<Connector3d> getConnectors3d() {
		return connectors3d;
	}
	
	public ArrayList<ContactPair> getContactPairs() {
		return contactPairs;
	}
	
	public ArrayList<Load> getLoads() {
		return loads;
	}
	
	public ArrayList<DistributedLoad> getDistributedLoads() {
		return distributedLoads;
	}
	
	public void clearAll() {
		settings = new Settings();
		nodes.clear();
		elements.clear();
		materials.clear();
		sections.clear();
		steps.clear();
		sets.clear();
		parts3d.clear();
		part3dColors.clear();
		supports.clear();
		connectors.clear();
		connectors3d.clear();
		contactPairs.clear();
		loads.clear();
		distributedLoads.clear();
	}
	
	public Model clone() {
		Model model = new Model();
		model.settings = this.settings.clone();
		for (int i = 0; i < materials.size(); i++) 	 	  model.materials.add(materials.get(i).clone());
		for (int i = 0; i < sections.size(); i++) 	 	  model.sections.add(sections.get(i).clone());
		for (int i = 0; i < nodes.size(); i++) 		 	  model.nodes.add(nodes.get(i).clone());
		for (int i = 0; i < elements.size(); i++) 	 	  model.elements.add(elements.get(i).clone(model));
		for (int i = 0; i < steps.size(); i++) 	 	  	  model.steps.add(steps.get(i).clone());
		for (int i = 0; i < sets.size(); i++)       	  model.sets.add(sets.get(i).clone(model));
		for (int i = 0; i < parts3d.size(); i++)          model.parts3d.add(parts3d.get(i).clone());
		for (int i = 0; i < part3dColors.size(); i++)     model.part3dColors.add(part3dColors.get(i).clone());
		for (int i = 0; i < supports.size(); i++)  	      model.supports.add(supports.get(i).clone(model));
		for (int i = 0; i < connectors.size(); i++)  	  model.connectors.add(connectors.get(i).clone(model));
		for (int i = 0; i < connectors3d.size(); i++)     model.connectors3d.add(connectors3d.get(i).clone(model));
		for (int i = 0; i < contactPairs.size(); i++)  	  model.contactPairs.add(contactPairs.get(i).clone(model));
		for (int i = 0; i < loads.size(); i++) 			  model.loads.add(loads.get(i).clone(model));
		for (int i = 0; i < distributedLoads.size(); i++) model.distributedLoads.add(distributedLoads.get(i).clone(model));
		return model;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Model model = (Model) obj;
		result = model.settings.deepEquals(this.settings, result);
		result = SimLive.deepEquals(nodes, model.getNodes(), result);
		result = SimLive.deepEquals(elements, model.getElements(), result);
		result = SimLive.deepEquals(materials, model.getMaterials(), result);
		result = SimLive.deepEquals(sections, model.getSections(), result);
		result = SimLive.deepEquals(steps, model.getSteps(), result);
		result = SimLive.deepEquals(sets, model.getSets(), result);
		result = SimLive.deepEquals(parts3d, model.getParts3d(), result);
		result = SimLive.deepEquals(part3dColors, model.getPart3dColors(), result);
		result = SimLive.deepEquals(supports, model.getSupports(), result);
		result = SimLive.deepEquals(connectors, model.getConnectors(), result);
		result = SimLive.deepEquals(connectors3d, model.getConnectors3d(), result);
		result = SimLive.deepEquals(contactPairs, model.getContactPairs(), result);
		result = SimLive.deepEquals(loads, model.getLoads(), result);
		result = SimLive.deepEquals(distributedLoads, model.getDistributedLoads(), result);
		return result;
	}
	
	public void reorderNodes() {
		Matrix adjacencyMatrix = getAdjacencyMatrix();
		ArrayList<Node> R = new ArrayList<Node>();
		ArrayList<Node> Q = new ArrayList<Node>();
		ArrayList<Node> nodesCpy = new ArrayList<Node>();
		nodesCpy.addAll(nodes);
		while(!nodesCpy.isEmpty()) {
			Node P = sortNodes(nodesCpy, adjacencyMatrix).get(0);
			R.add(P);			
			Q.addAll(getAdjacentNodesSorted(P, adjacencyMatrix));
			while(!Q.isEmpty()) {
				Node C = Q.remove(0);
				if (!R.contains(C)) {
					R.add(C);
					Q.addAll(getAdjacentNodesSorted(C, adjacencyMatrix));
				}
			}
			nodesCpy.removeAll(R);
		}
		
		for (int e = 0; e < elements.size(); e++) {
			Element element = elements.get(e);
			int[] elementNodes = element.getElementNodes();
			int[] elementNodesNew = new int[elementNodes.length];
			for (int i = 0; i < elementNodes.length; i++) {
				elementNodesNew[i] = R.indexOf(nodes.get(elementNodes[i]));
			}
			element.setElementNodes(elementNodesNew);
		}
		nodes.clear();
		nodes.addAll(R);
		updateModel();
	}
	
	private Matrix getAdjacencyMatrix() {
		Matrix adjacencyMatrix = new Matrix(nodes.size(), nodes.size());
		for (int e = 0; e < elements.size(); e++) {
			int[] elementNodes = elements.get(e).getElementNodes();
			for (int i = 0; i < elementNodes.length; i++) {
				for (int j = 0; j < elementNodes.length; j++) if (i != j) {
					adjacencyMatrix.set(elementNodes[i], elementNodes[j], 1);
				}
			}
		}
		/* write number of adjacent nodes to diagonal */
		for (int c = 0; c < adjacencyMatrix.getColumnDimension(); c++) {
			int nrAdjacentNodes = 0;
			for (int r = 0; r < adjacencyMatrix.getRowDimension(); r++) {
				if (adjacencyMatrix.get(r, c) == 1) {
					nrAdjacentNodes++;
				}
			}
			adjacencyMatrix.set(c, c, nrAdjacentNodes);
		}
		return adjacencyMatrix;
	}
	
	private ArrayList<Node> getAdjacentNodesSorted(Node node, Matrix adjacencyMatrix) {
		int nodeID = node.getID();
		ArrayList<Node> adjacentNodes = new ArrayList<Node>();
		for (int n = 0; n < nodes.size(); n++) {
			if (n != nodeID && adjacencyMatrix.get(n, nodeID) == 1) {
				adjacentNodes.add(nodes.get(n));
			}
		}
		return sortNodes(adjacentNodes, adjacencyMatrix);
	}
	
	private ArrayList<Node> sortNodes(ArrayList<Node> nodes, Matrix adjacencyMatrix) {
		ArrayList<Node> nodesUnsorted = new ArrayList<Node>();
		nodesUnsorted.addAll(nodes);
		ArrayList<Node> nodesSorted = new ArrayList<Node>();
		int nrAdjacentNodes = 0;
		while (!nodesUnsorted.isEmpty()) {
			boolean found = false;
			for (int n = 0; n < nodesUnsorted.size(); n++) {
				int nodeID = nodesUnsorted.get(n).getID();
				if (adjacencyMatrix.get(nodeID, nodeID) == nrAdjacentNodes) {
					nodesSorted.add(nodesUnsorted.get(n));
					nodesUnsorted.remove(n);
					found = true;
					break;
				}
			}
			if (!found) {
				nrAdjacentNodes++;
			}
		}
		return nodesSorted;
	}
	
	public boolean isElementValid(int[] nodeIDs) {
		for (int i = 0; i < nodeIDs.length-1; i++) {
			for (int j = i+1; j < nodeIDs.length; j++) {
				if (areNodesCoincident(nodeIDs[i], nodeIDs[j])) {
					return false;
				}
			}
		}
		return !isElementAlreadyExisting(nodeIDs);
	}
	
	private boolean isElementAlreadyExisting(int[] nodeIDs) {
		for (int elem = 0; elem < elements.size(); elem++) {
			int[] elemNodes = elements.get(elem).getElementNodes();
			int nodesFound = 0;
			
			if (elemNodes.length == nodeIDs.length) {
				for (int i = 0; i < nodeIDs.length; i++) {
					for (int j = 0; j < elemNodes.length; j++) {
						if (nodeIDs[i] == elemNodes[j]) {
							nodesFound++;
						}
					}
				}
				if (nodesFound == nodeIDs.length) return true;
			}
		}
		return false;
	}
	
	private boolean areNodesCoincident(int node0ID, int node1ID) {
		double[] coords0 = View.getCoordsWithScaledDisp(node0ID);
		double[] coords1 = View.getCoordsWithScaledDisp(node1ID);
		if (coords0[0] == coords1[0] && coords0[1] == coords1[1] && coords0[2] == coords1[2]) {
			return true;
		}
		return false;
	}
	
	public boolean doSetsContainOnlyType(ArrayList<Set> sets, Element.Type type) {
		for (int s = 0; s < sets.size(); s++) {
			if (!doElementsContainOnlyType(sets.get(s).getElements(), type)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean doSetsContainOnlyPlaneElements(ArrayList<Set> sets) {
		for (int s = 0; s < sets.size(); s++) {
			for (int elem = 0; elem < sets.get(s).getElements().size(); elem++) {
				if (!sets.get(s).getElements().get(elem).isPlaneElement()) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean doElementsContainOnlyType(ArrayList<Element> elementSet, Element.Type type) {
		for (int elem = 0; elem < elementSet.size(); elem++) {
			if (elementSet.get(elem).getType() != type) {
				return false;
			}
		}
		return true;
	}
	
	public boolean doElementsContainType(ArrayList<Element> elementSet, Element.Type type) {
		for (int elem = 0; elem < elementSet.size(); elem++) {
			if (elementSet.get(elem).getType() == type) {
				return true;
			}
		}
		return false;
	}
	
	public boolean doElementsContainOnlyLineElements(ArrayList<Element> elementSet) {
		for (int elem = 0; elem < elementSet.size(); elem++) {
			if (!elementSet.get(elem).isLineElement()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean havePointMassesSameMass(ArrayList<Element> elementSet) {
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (((PointMass) elementSet.get(elem)).getMass() !=
					((PointMass) elementSet.get(0)).getMass()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean haveSpringsSameSpringStiffness(ArrayList<Element> elementSet) {
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (((Spring) elementSet.get(elem)).getStiffness() !=
					((Spring) elementSet.get(0)).getStiffness()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean haveElementsSameMaterial(ArrayList<Element> elementSet) {
		if (materials.isEmpty()) return false;
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (elementSet.get(elem).getMaterial() != elementSet.get(0).getMaterial()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean haveElementsSameStiffnessDamping(ArrayList<Element> elementSet) {
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (elementSet.get(elem).getStiffnessDamping() != elementSet.get(0).getStiffnessDamping()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean haveElementsSameMassDamping(ArrayList<Element> elementSet) {
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (elementSet.get(elem).getMassDamping() != elementSet.get(0).getMassDamping()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean haveLineElementsSameSection(ArrayList<Element> elementSet) {
		if (sections.isEmpty()) return false;
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (((LineElement) elementSet.get(elem)).getSection() !=
					((LineElement) elementSet.get(0)).getSection()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean haveLineElementsSameQ0(ArrayList<Element> elementSet, int i) {
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (((LineElement) elementSet.get(elem)).getQ0()[i] !=
					((LineElement) elementSet.get(0)).getQ0()[i]) {
				return false;
			}
		}
		return true;
	}
	
	public boolean havePlaneElementsSameThickness(ArrayList<Element> elementSet) {
		for (int elem = 1; elem < elementSet.size(); elem++) {
			if (((PlaneElement) elementSet.get(elem)).getThickness() !=
					((PlaneElement) elementSet.get(0)).getThickness()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean doStepsContainType(Step.Type type) {
		for (int s = 0; s < steps.size(); s++) {
			if (steps.get(s).type == type) return true;
		}
		return false;
	}
	
	public boolean isConnectorAtNode(Node node) {
		double[] coords = node.getCoords();
		for (int c = 0; c < connectors.size(); c++) {
			if (connectors.get(c).isCoordsSet()) {
				double[] connectorCoords = connectors.get(c).getCoordinates();
				if (Math.sqrt((coords[0]-connectorCoords[0])*(coords[0]-connectorCoords[0])+
					(coords[1]-connectorCoords[1])*(coords[1]-connectorCoords[1])+
					(coords[2]-connectorCoords[2])*(coords[2]-connectorCoords[2])) < SimLive.ZERO_TOL) {
					return true;
				}
			}
		}
		return false;
	}
	
	private double[] getCenter(ArrayList<Node> nodeSet) {
		double[] center = new double[3];
		for (int n = 0; n < nodeSet.size(); n++) {
			center[0] += nodeSet.get(n).getXCoord();
			center[1] += nodeSet.get(n).getYCoord();
			center[2] += nodeSet.get(n).getZCoord();
		}
		center[0] /= (double) nodeSet.size();
		center[1] /= (double) nodeSet.size();
		center[2] /= (double) nodeSet.size();
		return center;
	}
	
	public void rotateNodes(ArrayList<Node> nodeSet, double angle, double[] point, double[] axis) {
		Matrix p = new Matrix(point, 3);
		Matrix rot = GeomUtility.getRotationMatrix(angle, axis);
		for (int n = 0; n < nodeSet.size(); n++) {
			double[] coords = nodeSet.get(n).getCoords();
			Matrix coordOld = new Matrix(coords, 3);
			coordOld.minusEquals(p);
			Matrix coordNew = rot.times(coordOld);
			coordNew.plusEquals(p);
			nodeSet.get(n).setCoords(coordNew.getColumnPackedCopy());
		}
	}
	
	public void deleteUnusedNodes() {
		boolean[] usedNodes = new boolean[nodes.size()];
		for (int elem = 0; elem < elements.size(); elem++) {
			int[] elemNodes = elements.get(elem).getElementNodes();
			for (int n = 0; n < elemNodes.length; n++) {
				usedNodes[elemNodes[n]] = true;
			}
		}
		for (int n = nodes.size()-1; n > -1; n--) {
			if (!usedNodes[n]) {
				for (int elem = 0; elem < elements.size(); elem++) {
					elements.get(elem).adaptNodeIDs(n);
				}
				nodes.remove(n);
			}
		}
		maxUsedNodeID = nodes.size()-1;
	}
	
	public double getTotalDuration() {
		double totalDuration = 0.0;
		for (int s = 0; s < steps.size(); s++) {
			totalDuration += steps.get(s).duration;
		}
		return totalDuration;
	}
	
	public void updateModel() {
		if (SimLive.mode != Mode.RESULTS) {
			deleteUnusedNodes();
			updateAllContacts();
			updateAllNodes();
			//updateAllPlaneElementOrientations();
			updateAllElements();
			updateAllSets();
			updateAllParts3d();
			updateAllSupports();
			updateAllLoads();
			updateAllDistributedLoads();
			updateAllConnectors();
			updateAllConnectors3d();
			updateAllOutlines();
			updateAllLabels();
			SimLive.view.cleanViewData();
			
			storeModelHistory();
			
			SimLive.shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					if (!SimLive.modelTree.isDisposed()) {
						updateModelTree();
					}
				}
			});
		}
	}
	
	private void updateModelTree() {
		//Parts
		boolean removed = false;
		TreeItem items = SimLive.modelTree.getItem(0);
		if (items.getItemCount() != sets.size()+parts3d.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < sets.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			Set set = sets.get(i);
			String str = "";
			if (set.getElements().size() > 1) {
				str = "Part (" + set.getElements().size() + " Elements)";
			}
			else {
				Element element = set.getElements().get(0);
				int elemID = element.getID();
				str = element.getTypeString() + " " + (elemID+1);
			}
			item.setText(str);
			item.setData(set);
		}
		for (int i = 0; i < parts3d.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(sets.size()+i);
			String str = "3D-Part ("+parts3d.get(i).getNrFacets()+" Facets)";
			item.setText(str);
			item.setData(parts3d.get(i));
		}
		//Supports
		removed = false;
		items = SimLive.modelTree.getItem(1);
		if (items.getItemCount() != supports.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < supports.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			item.setText(supports.get(i).name);
			item.setData(supports.get(i));
		}
		//Loads
		removed = false;
		items = SimLive.modelTree.getItem(2);
		if (items.getItemCount() != loads.size()+distributedLoads.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < loads.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			item.setText(loads.get(i).name);
			item.setData(loads.get(i));
		}
		for (int i = 0; i < distributedLoads.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(loads.size()+i);
			item.setText(distributedLoads.get(i).name);
			item.setData(distributedLoads.get(i));
		}
		//Connectors
		removed = false;
		items = SimLive.modelTree.getItem(3);
		if (items.getItemCount() != connectors.size()+connectors3d.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < connectors.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			item.setText(connectors.get(i).name);
			item.setData(connectors.get(i));
		}
		for (int i = 0; i < connectors3d.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(connectors.size()+i);
			item.setText(connectors3d.get(i).name);
			item.setData(connectors3d.get(i));
		}
		//Contacts
		removed = false;
		items = SimLive.modelTree.getItem(4);
		if (items.getItemCount() != contactPairs.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < contactPairs.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			item.setText(contactPairs.get(i).name);
			item.setData(contactPairs.get(i));
		}
		//Materials
		removed = false;
		items = SimLive.modelTree.getItem(5);
		if (items.getItemCount() != materials.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < materials.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			item.setText(materials.get(i).name);
			item.setData(materials.get(i));
		}
		//Sections
		removed = false;
		items = SimLive.modelTree.getItem(6);
		if (items.getItemCount() != sections.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < sections.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			item.setText(sections.get(i).getName());
			item.setData(sections.get(i));
		}
		//Steps
		removed = false;
		items = SimLive.modelTree.getItem(7);
		if (items.getItemCount() != steps.size()) {
			items.removeAll();
			removed = true;
		}
		for (int i = 0; i < steps.size(); i++) {
			TreeItem item = removed ? new TreeItem(items, SWT.NONE) : items.getItem(i);
			item.setText(steps.get(i).name);
			item.setData(steps.get(i));
		}
	}
	
	private Model reduceModel(Model model) {
		if (SimLive.modelPos > -1) {
			Model oldModel = expandModel(SimLive.modelPos-1);
			if (oldModel.settings.deepEquals(model.settings, Result.EQUAL) == Result.EQUAL) model.settings = null;
			if (SimLive.deepEquals(oldModel.nodes, model.nodes, Result.EQUAL) == Result.EQUAL) model.nodes = null;
			if (SimLive.deepEquals(oldModel.elements, model.elements, Result.EQUAL) == Result.EQUAL) model.elements = null;
			if (SimLive.deepEquals(oldModel.materials, model.materials, Result.EQUAL) == Result.EQUAL) model.materials = null;
			if (SimLive.deepEquals(oldModel.sections, model.sections, Result.EQUAL) == Result.EQUAL) model.sections = null;
			if (SimLive.deepEquals(oldModel.steps, model.steps, Result.EQUAL) == Result.EQUAL) model.steps = null;
			if (SimLive.deepEquals(oldModel.sets, model.sets, Result.EQUAL) == Result.EQUAL) model.sets = null;
			if (SimLive.deepEquals(oldModel.parts3d, model.parts3d, Result.EQUAL) == Result.EQUAL) model.parts3d = null;
			if (SimLive.deepEquals(oldModel.part3dColors, model.part3dColors, Result.EQUAL) == Result.EQUAL) model.part3dColors = null;
			if (SimLive.deepEquals(oldModel.supports, model.supports, Result.EQUAL) == Result.EQUAL) model.supports = null;
			if (SimLive.deepEquals(oldModel.connectors, model.connectors, Result.EQUAL) == Result.EQUAL) model.connectors = null;
			if (SimLive.deepEquals(oldModel.connectors3d, model.connectors3d, Result.EQUAL) == Result.EQUAL) model.connectors3d = null;
			if (SimLive.deepEquals(oldModel.contactPairs, model.contactPairs, Result.EQUAL) == Result.EQUAL) model.contactPairs = null;
			if (SimLive.deepEquals(oldModel.loads, model.loads, Result.EQUAL) == Result.EQUAL) model.loads = null;
			if (SimLive.deepEquals(oldModel.distributedLoads, model.distributedLoads, Result.EQUAL) == Result.EQUAL) model.distributedLoads = null;
		}
		return model;
	}
	
	public Model expandModel(int modelPos) {
		Model model = SimLive.modelHistory.get(0).clone();
		for (int i = 1; i <= modelPos; i++) {
			Model modify = SimLive.modelHistory.get(i);
			if (modify.settings != null) model.settings = modify.settings;
			if (modify.nodes != null) model.nodes = modify.nodes;
			if (modify.elements != null) model.elements = modify.elements;
			if (modify.materials != null) model.materials = modify.materials;
			if (modify.sections != null) model.sections = modify.sections;
			if (modify.steps != null) model.steps = modify.steps;
			if (modify.sets != null) model.sets = modify.sets;
			if (modify.parts3d != null) model.parts3d = modify.parts3d;
			if (modify.part3dColors != null) model.part3dColors = modify.part3dColors;
			if (modify.supports != null) model.supports = modify.supports;
			if (modify.connectors != null) model.connectors = modify.connectors;
			if (modify.connectors3d != null) model.connectors3d = modify.connectors3d;
			if (modify.contactPairs != null) model.contactPairs = modify.contactPairs;
			if (modify.loads != null) model.loads = modify.loads;
			if (modify.distributedLoads != null) model.distributedLoads = modify.distributedLoads;
		}
		return model;
	}
	
	private void storeModelHistory() {
		if (SimLive.mode != Mode.NONE && (SimLive.modelHistory.isEmpty() ||
				expandModel(SimLive.modelPos).deepEquals(this, Result.EQUAL) != Result.EQUAL)) {
			for (int i = SimLive.modelHistory.size()-1; i > SimLive.modelPos; i--) {
				SimLive.modelHistory.remove(i);
			}
			SimLive.modelHistory.add(reduceModel(this.clone()));
			//System.out.println("Stored. modelPos="+Sim2d.modelPos);
			if (SimLive.modelHistory.size() > SimLive.MODEL_HISTORY_MAX) {
				SimLive.modelHistory.set(1, expandModel(1));
				SimLive.modelHistory.remove(0);
			}
			else {
				SimLive.modelPos++;
			}
		}
	}
	
	public void finalUpdateModel() {
		updateModel();
		mapVerticesToElements();
	}
	
	private void mapVerticesToElements() {
		for (int c = 0; c < connectors3d.size(); c++) {
			ArrayList<Part3d> parts3d = connectors3d.get(c).getParts3d();			
			ArrayList<Set> sets = connectors3d.get(c).getParts();			
			for (int p = 0; p < parts3d.size(); p++) {
				Vertex3d[] vertices = parts3d.get(p).getVertices();
				Stream<Vertex3d> stream = Arrays.stream(vertices).parallel();
				stream.forEach(vertex -> {
					double minDist = Double.MAX_VALUE;
					Matrix coords = new Matrix(vertex.getCoords(), 3);
					int elementID = -1;
					for (int s = 0; s < sets.size(); s++) {
						for (int elem = 0; elem < sets.get(s).getElements().size(); elem++) {
							Element element = sets.get(s).getElements().get(elem);
							int[] elemNodes = element.getElementNodes();
							if (element.isLineElement()) {
								LineElement lineElement = (LineElement) element;
								Matrix coords0 = new Matrix(nodes.get(elemNodes[0]).getCoords(), 3);
								Matrix coords1 = new Matrix(nodes.get(elemNodes[1]).getCoords(), 3);
								Matrix dir = coords1.minus(coords0);
								dir.timesEquals(1.0/dir.normF());
								Matrix diff0 = coords.minus(coords0);
								Matrix diff1 = coords.minus(coords1);
								if (diff0.dotProduct(dir) > 0.0 && diff1.dotProduct(dir) < 0.0) {
									double dist = (diff0.minus(dir.times(diff0.dotProduct(dir)))).normF();
									if (dist < minDist) {
										minDist = dist;
										double t = diff0.dotProduct(dir)/lineElement.getLength();
										vertex.setElementID(lineElement.getID());
										vertex.setT(t);
									}
								}									
								for (int i = 0; i < elemNodes.length; i++) {
									double dist = Math.min(diff0.normF(), diff1.normF());
									if (dist < minDist) {
										minDist = dist;
										double t = diff0.dotProduct(dir)/lineElement.getLength();
										vertex.setElementID(lineElement.getID());
										vertex.setT(t);
									}
								}
							}
							if (element.isPlaneElement()) {
								PlaneElement planeElement = (PlaneElement) element;
								Matrix R0T = planeElement.getR0().transpose();
								float[][] localNodeCoords = new float[elemNodes.length][2];
								for (int i = 0; i < elemNodes.length; i++) {
									Matrix temp = R0T.times(new Matrix(nodes.get(elemNodes[i]).getCoords(), 3));
									localNodeCoords[i][0] = (float) temp.get(0, 0);
									localNodeCoords[i][1] = (float) temp.get(1, 0);
								}
								double[] point = R0T.times(coords).getColumnPackedCopy();
								if (GeomUtility.isPointInConvexPolygon(localNodeCoords, new float[]{(float) point[0], (float) point[1]})) {
									Matrix diff = coords.minus(new Matrix(nodes.get(elemNodes[0]).getCoords(), 3));
									Matrix normal = planeElement.getR0().getMatrix(0, 2, 2, 2);
									double dist = Math.abs(diff.dotProduct(normal));
									if (dist < minDist) {
										minDist = dist;
										elementID = planeElement.getID();
										continue;
									}
								}
								for (int i = 0; i < elemNodes.length; i++) {
									Matrix c0 = new Matrix(nodes.get(elemNodes[i]).getCoords(), 3);
									Matrix c1 = new Matrix(nodes.get(elemNodes[(i+1)%elemNodes.length]).getCoords(), 3);
									Matrix a = c1.minus(c0);
									Matrix d0 = coords.minus(c0);
									Matrix d1 = coords.minus(c1);
									if (d0.dotProduct(a) > 0 && d1.dotProduct(a) < 0) {
										a.timesEquals(1.0/a.normF());
										double dist = d0.minus(a.times(d0.dotProduct(a))).normF();
										if (dist < minDist) {
											minDist = dist;
											elementID = planeElement.getID();
											continue;
										}
									}
								}
								for (int i = 0; i < elemNodes.length; i++) {
									Matrix c0 = new Matrix(nodes.get(elemNodes[i]).getCoords(), 3);
									Matrix diff = coords.minus(c0);
									double dist = diff.normF();
									if (dist < minDist) {
										minDist = dist;
										elementID = planeElement.getID();
									}
								}
							}
							if (element.getType() == Element.Type.POINT_MASS) {
								Matrix coords0 = new Matrix(nodes.get(elemNodes[0]).getCoords(), 3);
								double dist = (coords.minus(coords0)).normF();
								if (dist < minDist) {
									minDist = dist;
									vertex.setElementID(element.getID());
								}
							}
						}
					}
					if (elementID != -1) {
						vertex.setElementID(elementID);
						PlaneElement planeElement = (PlaneElement) SimLive.model.getElements().get(elementID);
						double[] r = planeElement.getLocalFromGlobalCoordinates(coords.getColumnPackedCopy());
						vertex.setR(r);
					}
				});
			}
		}
	}
	
	/*private void updateAllPlaneElementOrientations() {
		// all elements connected by an edge are orientated like the first one
		ArrayList<PlaneElement> planeElements = new ArrayList<PlaneElement>();
		for (int e = 0; e < elements.size(); e++) {
			if (elements.get(e).isPlaneElement()) {
				planeElements.add((PlaneElement) elements.get(e));
			}
		}
		ArrayList<PlaneElement> flippedElements = new ArrayList<PlaneElement>();
		PlaneElement flipElement = findFlipElement(planeElements, flippedElements);
		while (flipElement != null) {
			if (!flippedElements.contains(flipElement)) {
				int[] nodeIDs = new int[flipElement.getElementNodes().length];
				for (int k = 0; k < nodeIDs.length; k++) {
					nodeIDs[k] = flipElement.getElementNodes()[nodeIDs.length-k-1];
				}
				flipElement.setElementNodes(nodeIDs);
				flippedElements.add(flipElement);
			}
			flipElement = findFlipElement(planeElements, flippedElements);
		}
	}*/
	
	/*private PlaneElement findFlipElement(ArrayList<PlaneElement> planeElements,
			ArrayList<PlaneElement> flippedElements) {
		int[][][] neighbors = new int[nodes.size()][nodes.size()][];
		for (int e = 0; e < planeElements.size(); e++) {
			PlaneElement planeElement = planeElements.get(e);
			int[] elemNodes = planeElement.getElementNodes();
			for (int i = 0; i < elemNodes.length; i++) {
				int i0 = elemNodes[i];
				int i1 = elemNodes[(i+1)%elemNodes.length];
				if (neighbors[i0][i1] != null) {
					if (flippedElements.contains(planeElement)) {
						return planeElements.get(neighbors[i0][i1][0]);
					}
					else {
						return planeElement;
					}
				}
				neighbors[i0][i1] = new int[1];
				neighbors[i0][i1][0] = planeElement.getID();
			}
		}
		return null;
	}*/
	
	private void updateAllNodes() {
		for (int n = 0; n < nodes.size(); n++) {
			nodes.get(n).update();
		}
	}

	public void updateAllElements() {
		for (int e = 0; e < elements.size(); e++) {
			elements.get(e).update();
		}
	}

	private void updateAllSets() {
		for (int s = 0; s < sets.size(); s++) {
			sets.get(s).update();
		}
	}
	
	private void updateAllParts3d() {
		for (int s = 0; s < parts3d.size(); s++) {
			parts3d.get(s).update();
		}
	}
	
	private void updateAllSupports() {
		for (int s = 0; s < supports.size(); s++) {
			supports.get(s).update();
		}
	}
	
	private void updateAllLoads() {
		for (int l = 0; l < loads.size(); l++) {
			loads.get(l).update();
		}
	}
	
	public void updateAllDistributedLoads() {
		for (int d = 0; d < distributedLoads.size(); d++) {
			distributedLoads.get(d).update();
		}
	}
	
	private void updateAllConnectors() {
		for (int c = connectors.size()-1; c > -1; c--) {
			connectors.get(c).update();
		}
	}
	
	private void updateAllConnectors3d() {
		for (int c = connectors3d.size()-1; c > -1; c--) {
			connectors3d.get(c).update();
		}		
		for (int s = 0; s < parts3d.size(); s++) {
			boolean isPartUsed = false;
			for (int c = 0; c < connectors3d.size(); c++) {
				if (connectors3d.get(c).getParts3d().contains(parts3d.get(s))) {
					isPartUsed = true;
				}
			}
			if (!isPartUsed) {
				for (int v = 0; v < parts3d.get(s).getNrVertices(); v++) {
					parts3d.get(s).getVertex(v).setElementID(-1);
				}
			}
		}
	}
	
	private void updateAllContacts() {
		for (int c = 0; c < contactPairs.size(); c++) {
			contactPairs.get(c).update();
		}
	}
	
	public void updateAllOutlines() {
		Double[][] edgeThickness = new Double[nodes.size()][nodes.size()];
		SimLive.view.outlineEdge = new int[nodes.size()][0];
		SimLive.view.innerEdge = new int[nodes.size()][0];
		SimLive.view.smoothEdge = new int[nodes.size()][0];
		Matrix[] r1 = new Matrix[nodes.size()];
		Matrix[][] edgeNorm = new Matrix[nodes.size()][nodes.size()];
		boolean[] angled = new boolean[nodes.size()];
		Section[] section = new Section[nodes.size()];
		Element.Type[] type = new Element.Type[nodes.size()];
		for (int e = 0; e < elements.size(); e++) {
			int[] element_nodes = elements.get(e).getElementNodes();
			if (elements.get(e).isLineElement()) {
				int n0 = element_nodes[0];
				int n1 = element_nodes[1];
				Matrix r1Loc = ((LineElement) elements.get(e)).getR0().getMatrix(0, 2, 0, 0);
				if (!angled[n0] && r1[n0] != null) {
					angled[n0] = r1[n0].dotProduct(r1Loc) < 1.0-SimLive.ZERO_TOL;
				}
				if (!angled[n1] && r1[n1] != null) {
					angled[n1] = r1[n1].dotProduct(r1Loc) < 1.0-SimLive.ZERO_TOL;
				}
				if (SimLive.contains(SimLive.view.outlineEdge[n1], n1) && !angled[n1] && elements.get(e).getType() == type[n1] &&
						((LineElement) elements.get(e)).getSection() == section[n1]) {
					SimLive.view.outlineEdge[n1] = SimLive.remove(SimLive.view.outlineEdge[n1], n1);
				}
				else {
					SimLive.view.outlineEdge[n1] = SimLive.add(SimLive.view.outlineEdge[n1], n1);
				}
				if (SimLive.contains(SimLive.view.outlineEdge[n0], n0) && !angled[n0] && elements.get(e).getType() == type[n0] &&
						((LineElement) elements.get(e)).getSection() == section[n0]) {
					SimLive.view.outlineEdge[n0] = SimLive.remove(SimLive.view.outlineEdge[n0], n0);
				}
				else {
					SimLive.view.outlineEdge[n0] = SimLive.add(SimLive.view.outlineEdge[n0], n0);
				}
				r1[n0] = r1[n1] = r1Loc;
				type[n0] = type[n1] = elements.get(e).getType();
				section[n0] = section[n1] = ((LineElement) elements.get(e)).getSection();
			}
			if (elements.get(e).isPlaneElement()) {
				double thickness = ((PlaneElement) elements.get(e)).getThickness();
				for (int i = 0; i < element_nodes.length; i++) {
					int n0 = element_nodes[i];
					int n1 = element_nodes[(i+1)%element_nodes.length];
					Matrix normLoc = ((PlaneElement) elements.get(e)).getR0().getMatrix(0, 2, 2, 2);
					edgeNorm[n0][n1] = normLoc;
					edgeThickness[n0][n1] = thickness;
					SimLive.view.outlineEdge[n0] = SimLive.add(SimLive.view.outlineEdge[n0], n1);
					if (SimLive.contains(SimLive.view.outlineEdge[n1], n0)) {
						SimLive.view.outlineEdge[n0] = SimLive.remove(SimLive.view.outlineEdge[n0], n1);
						SimLive.view.outlineEdge[n1] = SimLive.remove(SimLive.view.outlineEdge[n1], n0);
						if (edgeThickness[n0][n1].doubleValue() != edgeThickness[n1][n0].doubleValue()) {
							SimLive.view.innerEdge[n0] = SimLive.add(SimLive.view.innerEdge[n0], n1);
							SimLive.view.innerEdge[n1] = SimLive.add(SimLive.view.innerEdge[n1], n0);
						}
						if (edgeNorm[n0][n1].dotProduct(edgeNorm[n1][n0]) < SimLive.COS_ANGLE_INNER_EDGE) {
							SimLive.view.innerEdge[n0] = SimLive.add(SimLive.view.innerEdge[n0], n1);
							SimLive.view.innerEdge[n1] = SimLive.add(SimLive.view.innerEdge[n1], n0);
						}
						else if (edgeNorm[n0][n1].dotProduct(edgeNorm[n1][n0]) < 1.0-SimLive.ZERO_TOL) {
							SimLive.view.smoothEdge[n0] = SimLive.add(SimLive.view.smoothEdge[n0], n1);
							SimLive.view.smoothEdge[n1] = SimLive.add(SimLive.view.smoothEdge[n1], n0);
						}
					}
				}
			}
		}
		
		SimLive.view.isOutlineNode = new boolean[nodes.size()];
		for (int e = 0; e < elements.size(); e++) {
			int[] element_nodes = elements.get(e).getElementNodes();
			if (elements.get(e).isPlaneElement()) {
				for (int i = 0; i < element_nodes.length; i++) {				
					int n0 = element_nodes[i];
					int n1 = element_nodes[(i+1)%element_nodes.length];					
					if (SimLive.contains(SimLive.view.outlineEdge[n0], n1) ||
							SimLive.contains(SimLive.view.innerEdge[n0], n1)) {
						SimLive.view.isOutlineNode[n0] = true;
						SimLive.view.isOutlineNode[n1] = true;
					}
				}
			}
			else {
				for (int n = 0; n < element_nodes.length; n++) {
					SimLive.view.isOutlineNode[element_nodes[n]] = true;
				}
			}
		}
		
		SimLive.view.isCornerNode = new boolean[nodes.size()];
		ArrayList<int[]> edges = new ArrayList<int[]>();
		for (int e = 0; e < elements.size(); e++) {
			int[] element_nodes = elements.get(e).getElementNodes();
			if (elements.get(e).isPlaneElement()) {
				for (int i = 0; i < element_nodes.length; i++) {
					int n0 = element_nodes[i];
					int n1 = element_nodes[(i+1)%element_nodes.length];
					if (SimLive.contains(SimLive.view.outlineEdge[n0], n1) ||
							SimLive.contains(SimLive.view.innerEdge[n0], n1)) {
						edges.add(new int[]{n0, n1});
					}
				}
			}
		}
		for (int e0 = 0; e0 < edges.size(); e0++) {
			for (int e1 = 0; e1 < edges.size(); e1++) {
				int n0 = edges.get(e0)[0];
				int n1 = edges.get(e0)[1];
				int m0 = edges.get(e1)[0];
				int m1 = edges.get(e1)[1];
				if ((n0 == m1 || n1 == m0) && !(n0 == m1 && n1 == m0)) {
					double[] diff = new double[3];
					diff[0] = nodes.get(n1).getXCoord()-nodes.get(n0).getXCoord();
					diff[1] = nodes.get(n1).getYCoord()-nodes.get(n0).getYCoord();
					diff[2] = nodes.get(n1).getZCoord()-nodes.get(n0).getZCoord();
					double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
					diff[0] /= length;
					diff[1] /= length;
					diff[2] /= length;
					double[] diff1 = new double[3];
					diff1[0] = nodes.get(m1).getXCoord()-nodes.get(m0).getXCoord();
					diff1[1] = nodes.get(m1).getYCoord()-nodes.get(m0).getYCoord();
					diff1[2] = nodes.get(m1).getZCoord()-nodes.get(m0).getZCoord();
					length = Math.sqrt(diff1[0]*diff1[0]+diff1[1]*diff1[1]+diff1[2]*diff1[2]);
					diff1[0] /= length;
					diff1[1] /= length;
					diff1[2] /= length;
					double scal = diff[0]*diff1[0]+diff[1]*diff1[1]+diff[2]*diff1[2];
					if (scal < SimLive.COS_ANGLE_INNER_EDGE) {
						if (n0 == m1) {
							SimLive.view.isCornerNode[n0] = true;
						}
						if (n1 == m0) {
							SimLive.view.isCornerNode[n1] = true;
						}
					}
				}
			}
		}
	}
	
	private void updateAllLabels() {
		for (int l = SimLive.view.labels.size()-1; l > -1; l--) {
			if (!elements.contains(SimLive.view.labels.get(l).getElement())) {
				SimLive.view.labels.remove(l);
			}
		}
		if (SimLive.view.focusPoint != null) {
			if ((SimLive.view.focusPoint.getElement() != null &&
				 !elements.contains(SimLive.view.focusPoint.getElement())) ||
				(SimLive.view.focusPoint.getPart3d() != null &&
				 !parts3d.contains(SimLive.view.focusPoint.getPart3d()))) {
				
				SimLive.view.focusPoint = null;
			}
		}
	}
	
	/*public ArrayList<Node> getReferenceNodesFromNodeSet(ArrayList<Node> nodeSet) {
		for (int n = 0; n < nodeSet.size(); n++) {
			ArrayList<Set> sets = getSetsByNode(nodeSet.get(n));
			for (int s = 0; s < sets.size(); s++) {
				for (int e = 0; e < sets.get(s).getElements().size(); e++) {
					int[] elemNodes = sets.get(s).getElements().get(e).getElementNodes();
					for (int i = 0; i < elemNodes.length; i++) {
						if (nodes.get(elemNodes[i]) == nodeSet.get(n)) {
							for (int n1 = n+1; n1 < nodeSet.size(); n1++) {
								for (int i1 = 0; i1 < elemNodes.length; i1++) {
									if (nodes.get(elemNodes[i1]) == nodeSet.get(n1)) {
										ArrayList<Node> referenceNodes = new ArrayList<Node>();
										referenceNodes.add(nodeSet.get(n));
										referenceNodes.add(nodeSet.get(n1));
										return referenceNodes;
									}
								}
							}
						}
					}
				}
			}
			for (int d = 0; d < distributedLoads.size(); d++) {
				DistributedLoad distributedLoad = distributedLoads.get(d);
				for (int s = 0; s < distributedLoad.getElementSets().size(); s++) {
					Set set = distributedLoad.getElementSets().get(s);
					if (set.getNodes().contains(nodeSet.get(n))) {
						for (int n1 = n+1; n1 < nodeSet.size(); n1++) {
							if (set.getNodes().contains(nodeSet.get(n1))) {
								ArrayList<Node> referenceNodes = new ArrayList<Node>();
								referenceNodes.add(nodeSet.get(n));
								referenceNodes.add(nodeSet.get(n1));
								return referenceNodes;
							}
						}
					}
				}
			}
		}
		return null;
	}*/
	
	public void mergeCoincidentNodes(ArrayList<Element> elementSet) {
		double tolerance = Settings.meshSize/1000.0;
		IntStream.range(0, elementSet.size()).parallel().forEach(e0 -> {
			int[] nodes0 = elementSet.get(e0).getElementNodes();
			for (int i = 0; i < nodes0.length; i++) {
				double[] p0 = nodes.get(nodes0[i]).getCoords();
				for (int e1 = e0+1; e1 < elementSet.size(); e1++) {
					int[] nodes1 = elementSet.get(e1).getElementNodes();
					for (int j = 0; j < nodes1.length; j++) {
						double[] p1 = nodes.get(nodes1[j]).getCoords();
						if (Math.abs(p0[0] - p1[0]) < tolerance &&
							Math.abs(p0[1] - p1[1]) < tolerance &&
							Math.abs(p0[2] - p1[2]) < tolerance) {
							nodes1[j] = nodes0[i];
							elementSet.get(e1).setElementNodes(nodes1);
						}
					}
				}
			}
		});
	}
	
	private void refineSubSets(Set set) {
		if (!set.getSets().isEmpty()) {
			ArrayList<Element> elements = new ArrayList<Element>();
			for (int s = 0; s < set.getSets().size(); s++) {
				refineSubSets(set.getSets().get(s));
				elements.addAll(set.getSets().get(s).getElements());
			}
			set.getElements().clear();
			set.getElements().addAll(elements);
		}
		else {
			for (int e = set.getElements().size()-1; e >-1 ; e--) {			
				Element element = set.getElements().get(e);
				ArrayList<Element> newElements = element.refine(nodes, elements);
				if (newElements != null) {
					set.getElements().addAll(e+1, newElements);
				}
			}
			if (set.getType() == Set.Type.BASIC && !isSetPartOfDistributedLoad(set)) {
				set.setType(Set.Type.COMPOSITE);
			}
			
			mergeCoincidentNodes(set.getElements());
			SimLive.mode = Mode.NONE;
			updateModel();
			SimLive.mode = Mode.PARTS;
			
			if (set.getType() == Set.Type.CIRCULAR) {
				removeChordErrorOfRefinedSet(set);
				if (set.getSpurGearValues() != null) {
					removeFlankErrorOfRefinedSpurGear(set);
				}
			}
		}
	}
	
	public void refineSet(Set set, int levels) {
		
		ArrayList<double[]> coords = getLabelCoords();
		
		for (int l = 0; l < levels; l++) {
			refineSubSets(set);
		}
		
		mergeCoincidentNodes(set.getElements());
		/*Mode mode = SimLive.mode;
		SimLive.mode = Mode.NONE;
		updateModel();
		SimLive.mode = mode;*/
		
		mapLabelsToNewMesh(coords);
	}
	
	public void relaxMesh(ArrayList<Set> sets) {
		
		ArrayList<double[]> coords = getLabelCoords();
		
		Matrix adjacencyMatrix = getAdjacencyMatrix();
		int[][] neighbors = new int[nodes.size()][];
		
		for (int n = 0; n < nodes.size(); n++) {
			if (!SimLive.view.isCornerNode[n]) {
				neighbors[n] = new int[(int) adjacencyMatrix.get(n, n)];
				int count = 0;
				for (int i = 0; i < adjacencyMatrix.getColumnDimension(); i++) {
					if (i != n && adjacencyMatrix.get(n, i) == 1.0) {
						neighbors[n][count++] = i;
					}
				}
			}
		}
		
		for (int e = 0; e < elements.size(); e++) {
			if (!elements.get(e).isPlaneElement()) {
				int[] elemNodes = elements.get(e).getElementNodes();
				for (int i = 0; i < elemNodes.length; i++) {
					neighbors[elemNodes[i]] = null;
				}
			}
		}
		
		for (int s = 0; s < this.sets.size(); s++) {
			if (!sets.contains(this.sets.get(s))) {
				for (int n = 0; n < this.sets.get(s).getNodes().size(); n++) {
					neighbors[this.sets.get(s).getNodes().get(n).getID()] = null;
				}
			}
		}
		
		for (int iter = 0; iter < 100; iter++) {
			for (int s = 0; s < sets.size(); s++) {
				for (int n0 = 0; n0 < sets.get(s).getNodes().size(); n0++) {
					int n = sets.get(s).getNodes().get(n0).getID();
					if (neighbors[n] != null) {
						double x = 0;
						double y = 0;
						double z = 0;
						for (int i = 0; i < neighbors[n].length; i++) {
							x += nodes.get(neighbors[n][i]).getXCoord();
							y += nodes.get(neighbors[n][i]).getYCoord();
							z += nodes.get(neighbors[n][i]).getZCoord();
						}
						int i1 = -1;
						int i2 = -1;
						for (int i = 0; i < neighbors[n].length; i++) {
							if (SimLive.contains(SimLive.view.outlineEdge[n], neighbors[n][i]) ||
								SimLive.contains(SimLive.view.outlineEdge[neighbors[n][i]], n) ||
								SimLive.contains(SimLive.view.innerEdge[n], neighbors[n][i]) ||
								SimLive.contains(SimLive.view.innerEdge[neighbors[n][i]], n) ||
								SimLive.contains(SimLive.view.smoothEdge[n], neighbors[n][i]) ||
								SimLive.contains(SimLive.view.smoothEdge[neighbors[n][i]], n)) {
								if (i1 == -1) {
									i1 = neighbors[n][i];
								}
								if (i2 == -1 && neighbors[n][i] != i1) {
									i2 = neighbors[n][i];
								}
							}
						}
						
						if (i1 != -1 && i2 != -1) {
							double[] d = new double[3];
							d[0] = nodes.get(i1).getXCoord()-nodes.get(i2).getXCoord();
							d[1] = nodes.get(i1).getYCoord()-nodes.get(i2).getYCoord();
							d[2] = nodes.get(i1).getZCoord()-nodes.get(i2).getZCoord();
							double length = Math.sqrt(d[0]*d[0]+d[1]*d[1]+d[2]*d[2]);
							d[0] /= length;
							d[1] /= length;
							d[2] /= length;
							double[] d0 = new double[3];
							d0[0] = nodes.get(n).getXCoord()-nodes.get(i2).getXCoord();
							d0[1] = nodes.get(n).getYCoord()-nodes.get(i2).getYCoord();
							d0[2] = nodes.get(n).getZCoord()-nodes.get(i2).getZCoord();
							double scal = d0[0]*d[0]+d0[1]*d[1]+d0[2]*d[2];
							double[] vec = new double[3];
							vec[0] = d0[0]-scal*d[0];
							vec[1] = d0[1]-scal*d[1];
							vec[2] = d0[2]-scal*d[2];
							double[] dCur = new double[3];
							dCur[0] = (nodes.get(i1).getXCoord()+nodes.get(i2).getXCoord())/2.0+vec[0];
							dCur[1] = (nodes.get(i1).getYCoord()+nodes.get(i2).getYCoord())/2.0+vec[1];
							dCur[2] = (nodes.get(i1).getZCoord()+nodes.get(i2).getZCoord())/2.0+vec[2];
							nodes.get(n).setCoords(dCur);
						}
						else {
							nodes.get(n).setXCoord(x/neighbors[n].length);
							nodes.get(n).setYCoord(y/neighbors[n].length);
							nodes.get(n).setZCoord(z/neighbors[n].length);
						}
					}
				}
			}
		}
		
		mapLabelsToNewMesh(coords);
	}
	
	public ArrayList<double[]> getLabelCoords() {
		ArrayList<double[]> coords = new ArrayList<double[]>();
		for (int l = 0; l < SimLive.view.labels.size(); l++) {
			Label label = SimLive.view.labels.get(l);
			coords.add(label.getCoordinatesWithDeformation());
		}
		return coords;
	}
	
	public void mapLabelsToNewMesh(ArrayList<double[]> coords) {
		final double zero = -SimLive.ZERO_TOL;
		final double one = 1+SimLive.ZERO_TOL;
		/*bounding spheres of plane elements*/
		double[][] boundingSpheres = new double[elements.size()][];
		for (int e = 0; e < elements.size(); e++) {
			Element element = elements.get(e);
			if (element.isPlaneElement()) {
				boundingSpheres[e] = new double[4];
				int[] elementNodes = element.getElementNodes();
				for (int i = 0; i < elementNodes.length; i++) {
					double[] node = nodes.get(elementNodes[i]).getCoords();
					boundingSpheres[e][0] += node[0];
					boundingSpheres[e][1] += node[1];
					boundingSpheres[e][2] += node[2];
				}
				boundingSpheres[e][0] /= elementNodes.length;
				boundingSpheres[e][1] /= elementNodes.length;
				boundingSpheres[e][2] /= elementNodes.length;
				double halfThickness = ((PlaneElement) element).getThickness()/2;
				for (int i = 0; i < elementNodes.length; i++) {
					double[] node = nodes.get(elementNodes[i]).getCoords();
					double[] diff = new double[3];
					diff[0] = node[0]-boundingSpheres[e][0];
					diff[1] = node[1]-boundingSpheres[e][1];
					diff[2] = node[2]-boundingSpheres[e][2];
					double radiusSqr = diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]+halfThickness*halfThickness;
					if (radiusSqr > boundingSpheres[e][3]) {
						boundingSpheres[e][3] = radiusSqr;
					}
				}
			}
		}
		for (int l = 0; l < SimLive.view.labels.size(); l++) {
			Label label = SimLive.view.labels.get(l);
			Label newLabel = null;
			double minD = Double.MAX_VALUE;
			for (int e = 0; e < elements.size(); e++) {
				Element element = elements.get(e);
				if (label.getElement().isLineElement() && element.isLineElement()) {
					double x = ((LineElement) element).getLocalFromGlobalCoordinates(coords.get(l));
					if (x >= zero && x <= one) {
						
						double[] p = ((LineElement) element).getGlobalFromLocalCoordinates(x);
						double d = Math.sqrt((coords.get(l)[0]-p[0])*(coords.get(l)[0]-p[0])+
								(coords.get(l)[1]-p[1])*(coords.get(l)[1]-p[1])+
								(coords.get(l)[2]-p[2])*(coords.get(l)[2]-p[2]));
						if (d < minD) {
							minD = d;
							newLabel = new Label((LineElement) element, x);
						}
					}
				}
				if (label.getElement().isPlaneElement() && element.isPlaneElement()) {
					/*check against bounding sphere*/
					double[] diff = new double[3];
					diff[0] = coords.get(l)[0]-boundingSpheres[e][0];
					diff[1] = coords.get(l)[1]-boundingSpheres[e][1];
					diff[2] = coords.get(l)[2]-boundingSpheres[e][2];
					if (diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2] > boundingSpheres[e][3]) continue;
					
					double[] r = ((PlaneElement) element).getLocalFromGlobalCoordinates(coords.get(l));
					if ((element.getType() == Element.Type.QUAD &&
							r[0] >= -one && r[0] <= one && r[1] >= -one && r[1] <= one) ||
						(element.getType() == Element.Type.TRI &&
							r[0] >= zero && r[1] >= zero && r[0]+r[1] <= one)) {
						
						double[] p = ((PlaneElement) element).getGlobalFromLocalCoordinates(r[0], r[1]);
						double t = ((PlaneElement) element).getThickness()*label.getShift();
						p[0] += ((PlaneElement) element).getR0().get(0, 2)*t;
						p[1] += ((PlaneElement) element).getR0().get(1, 2)*t;
						p[2] += ((PlaneElement) element).getR0().get(2, 2)*t;
						double d = Math.sqrt((coords.get(l)[0]-p[0])*(coords.get(l)[0]-p[0])+
								(coords.get(l)[1]-p[1])*(coords.get(l)[1]-p[1])+
								(coords.get(l)[2]-p[2])*(coords.get(l)[2]-p[2]));
						if (d < minD) {
							minD = d;
							newLabel = new Label((PlaneElement) element, coords.get(l));
						}
					}
				}
			}
			if (newLabel != null) {
				newLabel.addToMove(label.getMove());
				newLabel.setOnRightHandSide(label.isOnRightHandSide());
				float[][] polygon = label.getPolygon();
				double[] screenCoords = View.modelToScreenCoordinates(coords.get(l));
				newLabel.updateLabel(screenCoords, Math.abs(polygon[2][0]-polygon[0][0]), (polygon[3][1]-polygon[2][1])/2);
				SimLive.view.labels.set(l, newLabel);
			}
		}
	}
	
	private void removeChordErrorOfEdge(Node n0, Node n1, double[] center) {
		double[] r0 = new double[3];
		r0[0] = n0.getXCoord()-center[0];
		r0[1] = n0.getYCoord()-center[1];
		r0[2] = n0.getZCoord()-center[2];
		double r0Length = Math.sqrt(r0[0]*r0[0]+r0[1]*r0[1]+r0[2]*r0[2]);
		double[] r1 = new double[3];
		r1[0] = n1.getXCoord()-center[0];
		r1[1] = n1.getYCoord()-center[1];
		r1[2] = n1.getZCoord()-center[2];
		double r1Length = Math.sqrt(r1[0]*r1[0]+r1[1]*r1[1]+r1[2]*r1[2]);
		if (r0Length > SimLive.ZERO_TOL && r1Length > SimLive.ZERO_TOL) {
			double[] coordsNew = new double[3];
			if (r0Length < r1Length) {
				coordsNew[0] = center[0] + r0[0]*r1Length/r0Length;
				coordsNew[1] = center[1] + r0[1]*r1Length/r0Length;
				coordsNew[2] = center[2] + r0[2]*r1Length/r0Length;
				n0.setCoords(coordsNew);
			}
			else {
				coordsNew[0] = center[0] + r1[0]*r0Length/r1Length;
				coordsNew[1] = center[1] + r1[1]*r0Length/r1Length;
				coordsNew[2] = center[2] + r1[2]*r0Length/r1Length;
				n1.setCoords(coordsNew);
			}
		}
	}
	
	private void removeChordErrorOfRefinedSet(Set set) {
		double[] center = getCenter(set.getNodes());
		for (int e = 0; e < set.getElements().size(); e++) {
			int[] elemNodes = set.getElements().get(e).elementNodes;
			if (elemNodes.length == 3) {
				removeChordErrorOfEdge(nodes.get(elemNodes[1]), nodes.get(elemNodes[2]), center);
			}
			if (elemNodes.length == 4) {
				removeChordErrorOfEdge(nodes.get(elemNodes[0]), nodes.get(elemNodes[1]), center);
				removeChordErrorOfEdge(nodes.get(elemNodes[2]), nodes.get(elemNodes[3]), center);
			}
		}
	}
	
	private void removeFlankErrorOfNode(double tipAngle, double rootAngle, double r, double m, double alpha,
			ArrayList<Element> elements, Node node, double[] center, double[] axis, boolean isInternal, boolean leftFlank) {
		
		double[] coords = node.getCoords();
		double minDist = Double.MAX_VALUE;
		double[] rNode = new double[3];
		rNode[0] = coords[0]-center[0];
		rNode[1] = coords[1]-center[1];
		rNode[2] = coords[2]-center[2];
		double rNodeLength = Math.sqrt(rNode[0]*rNode[0]+rNode[1]*rNode[1]+rNode[2]*rNode[2]);
		
		double phi = 0.0;
		for (int e = 0; e < elements.size(); e++) {
			Element element = elements.get(e);
			int[] elemNodes = element.getElementNodes();
			if (elemNodes.length == 4) {
				double[] coords0 = null;
				if (isInternal) {
					if (SimLive.view.isOutlineNode[elemNodes[0]] && SimLive.view.isOutlineNode[elemNodes[1]] &&
						SimLive.view.isOutlineNode[elemNodes[2]]) {
						coords0 = nodes.get(elemNodes[1]).getCoords();
					}
					if (SimLive.view.isOutlineNode[elemNodes[0]] && SimLive.view.isOutlineNode[elemNodes[1]] &&
						SimLive.view.isOutlineNode[elemNodes[3]]) {
						coords0 = nodes.get(elemNodes[0]).getCoords();
					}
				}
				else {
					if (SimLive.view.isOutlineNode[elemNodes[0]] && SimLive.view.isOutlineNode[elemNodes[2]] &&
						SimLive.view.isOutlineNode[elemNodes[3]]) {
						coords0 = nodes.get(elemNodes[3]).getCoords();
					}
					if (SimLive.view.isOutlineNode[elemNodes[1]] && SimLive.view.isOutlineNode[elemNodes[2]] &&
						SimLive.view.isOutlineNode[elemNodes[3]]) {
						coords0 = nodes.get(elemNodes[2]).getCoords();
					}
				}
				if (coords0 != null) {
					double dist = Math.sqrt((coords0[0]-coords[0])*(coords0[0]-coords[0])+
							(coords0[1]-coords[1])*(coords0[1]-coords[1])+
							(coords0[2]-coords[2])*(coords0[2]-coords[2]));
					if (dist < minDist) {
						minDist = dist;
						double[] ri = new double[3];
						ri[0] = coords0[0]-center[0];
						ri[1] = coords0[1]-center[1];
						ri[2] = coords0[2]-center[2];
						double riLength = Math.sqrt(ri[0]*ri[0]+ri[1]*ri[1]+ri[2]*ri[2]);
						phi = Math.acos(Math.min(rNode[0]/rNodeLength*ri[0]/riLength+rNode[1]/rNodeLength*ri[1]/riLength+rNode[2]/rNodeLength*ri[2]/riLength, 1.0));
					}
				}
			}
		}
		double s = this.toothThicknessArcLengthAtRadius(rNodeLength, r, m, alpha);
		
		double rot = 0.0;
		if (isInternal) {
			rot = s*0.5/rNodeLength+phi-rootAngle*0.5;
		}
		else {
			rot = s*0.5/rNodeLength-phi-tipAngle*0.5;
		}
		if (!leftFlank) rot = -rot;
		
		double[][] R = GeomUtility.getRotationMatrix(rot, axis).getArray();
		double[] coordsNew = new double[3];
		coordsNew[0] = R[0][0]*rNode[0]+R[0][1]*rNode[1]+R[0][2]*rNode[2]+center[0];
		coordsNew[1] = R[1][0]*rNode[0]+R[1][1]*rNode[1]+R[1][2]*rNode[2]+center[1];
		coordsNew[2] = R[2][0]*rNode[0]+R[2][1]*rNode[1]+R[2][2]*rNode[2]+center[2];
		node.setCoords(coordsNew);
	}
	
	private void removeFlankErrorOfRefinedSpurGear(Set set) {
		ArrayList<Element> elements = set.getElements();
		double[] center = getCenter(set.getNodes());
		Matrix c0 = new Matrix(nodes.get(elements.get(0).elementNodes[0]).getCoords(), 3);
		Matrix c1 = new Matrix(nodes.get(elements.get(0).elementNodes[1]).getCoords(), 3);
		Matrix c2 = new Matrix(nodes.get(elements.get(0).elementNodes[2]).getCoords(), 3);
		double[] axis = (c2.minus(c1)).crossProduct(c0.minus(c1)).getColumnPackedCopy();
		double r = set.getSpurGearValues().getPitchRadius();
		double m = set.getSpurGearValues().getModule();
		double alpha = set.getSpurGearValues().getPressureAngle();
		boolean isInternal = set.getSpurGearValues().isInternal();
		
		double ra = r+m;
		double rf = r-m;
		double tipAngle = toothThicknessArcLengthAtRadius(ra, r, m, alpha)/ra;
		double rootAngle = toothThicknessArcLengthAtRadius(rf, r, m, alpha)/rf;		
		
		for (int e = 0; e < elements.size(); e++) {
			int[] elemNodes = elements.get(e).elementNodes;
			if (elemNodes.length == 4) {
				if (SimLive.contains(SimLive.view.outlineEdge[elemNodes[3]], elemNodes[0])) {
					removeFlankErrorOfNode(tipAngle, rootAngle, r, m, alpha, elements, nodes.get(elemNodes[3]), center, axis, isInternal, !isInternal);
					removeFlankErrorOfNode(tipAngle, rootAngle, r, m, alpha, elements, nodes.get(elemNodes[0]), center, axis, isInternal, !isInternal);
				}
				if (SimLive.contains(SimLive.view.outlineEdge[elemNodes[1]], elemNodes[2])) {
					removeFlankErrorOfNode(tipAngle, rootAngle, r, m, alpha, elements, nodes.get(elemNodes[1]), center, axis, isInternal, isInternal);
					removeFlankErrorOfNode(tipAngle, rootAngle, r, m, alpha, elements, nodes.get(elemNodes[2]), center, axis, isInternal, isInternal);
				}
			}
		}
	}
	
	public void addCircleFromTrisAndQuads(double[] center, double radius) {
		if (radius < SimLive.ZERO_TOL) {
			return;
		}
		
		ArrayList<Element> elementSet = new ArrayList<Element>();
		nodes.add(new Node(center[0], center[1], 0));
		int[] element_node = new int[3];
		element_node[0] = nodes.size()-1;
		double[] coord = new double[2];
		coord[0] = center[0]+radius;
		coord[1] = center[1];
		nodes.add(new Node(coord[0], coord[1], 0));
		for (int i = 0; i < 8; i++) {
			double angle = i*Math.PI/4.0;
			coord[0] = center[0]+radius/2.0*Math.cos(angle);
			coord[1] = center[1]+radius/2.0*Math.sin(angle);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[1] = nodes.size()-1;
			angle = (i+1)*Math.PI/4.0;
			coord[0] = center[0]+radius/2.0*Math.cos(angle);
			coord[1] = center[1]+radius/2.0*Math.sin(angle);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[2] = nodes.size()-1;
			elementSet.add(new Tri(element_node.clone()));
		}
		element_node = new int[4];
		for (int i = 0; i < 8; i++) {
			double angle = (i+1)*Math.PI/4.0;
			coord[0] = center[0]+radius/2.0*Math.cos(angle);
			coord[1] = center[1]+radius/2.0*Math.sin(angle);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[0] = nodes.size()-1;
			angle = i*Math.PI/4.0;
			coord[0] = center[0]+radius/2.0*Math.cos(angle);
			coord[1] = center[1]+radius/2.0*Math.sin(angle);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[1] = nodes.size()-1;
			coord[0] = center[0]+radius*Math.cos(angle);
			coord[1] = center[1]+radius*Math.sin(angle);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[2] = nodes.size()-1;
			angle = (i+1)*Math.PI/4.0;
			coord[0] = center[0]+radius*Math.cos(angle);
			coord[1] = center[1]+radius*Math.sin(angle);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[3] = nodes.size()-1;
			elementSet.add(new Quad(element_node.clone()));
		}
		elements.addAll(elementSet);
		mergeCoincidentNodes(elementSet);
		sets.add(new Set(elementSet, Set.Type.CIRCULAR));
		updateModel();
	}
	
	public void addRectangleFromQuads(double[] edge, double width, double height) {
		if (Math.abs(width) < SimLive.ZERO_TOL || Math.abs(height) < SimLive.ZERO_TOL) {
			return;
		}
		
		ArrayList<Element> elementSet = new ArrayList<Element>();
		if (width < 0.0) {
			edge[0] += width;
			width = -width;
		}
		if (height < 0.0) {
			edge[1] += height;
			height = -height;
		}
		int[] element_node = new int[4];
		if (width > height) {
			int nr = (int) Math.round(width/height);
			for (int i = 0; i < nr; i++) {
				nodes.add(new Node(edge[0]+i*width/nr, edge[1], 0));
				element_node[0] = nodes.size()-1;
				nodes.add(new Node(edge[0]+(i+1)*width/nr, edge[1], 0));
				element_node[1] = nodes.size()-1;
				nodes.add(new Node(edge[0]+(i+1)*width/nr, edge[1]+height, 0));
				element_node[2] = nodes.size()-1;
				nodes.add(new Node(edge[0]+i*width/nr, edge[1]+height, 0));
				element_node[3] = nodes.size()-1;
				elementSet.add(new Quad(element_node.clone()));
			}
		}
		else {
			int nr = (int) Math.round(height/width);
			for (int i = 0; i < nr; i++) {
				nodes.add(new Node(edge[0]+width, edge[1]+i*height/nr, 0));
				element_node[0] = nodes.size()-1;
				nodes.add(new Node(edge[0]+width, edge[1]+(i+1)*height/nr, 0));
				element_node[1] = nodes.size()-1;
				nodes.add(new Node(edge[0], edge[1]+(i+1)*height/nr, 0));
				element_node[2] = nodes.size()-1;
				nodes.add(new Node(edge[0], edge[1]+i*height/nr, 0));
				element_node[3] = nodes.size()-1;
				elementSet.add(new Quad(element_node.clone()));
			}
		}
		elements.addAll(elementSet);
		mergeCoincidentNodes(elementSet);
		sets.add(new Set(elementSet, Set.Type.COMPOSITE));
		updateModel();
	}
	
	public void addTriangleFromQuads(double[] edge, double base, double height, double offset) {
		if (Math.abs(base) < SimLive.ZERO_TOL || Math.abs(height) < SimLive.ZERO_TOL) {
			return;
		}
		
		ArrayList<Element> elementSet = new ArrayList<Element>();
		int[] element_node = new int[4];
		nodes.add(new Node(edge[0], edge[1], 0));
		nodes.add(new Node(edge[0]+base/2.0, edge[1], 0));
		nodes.add(new Node(edge[0]+base, edge[1], 0));
		nodes.add(new Node(edge[0]+base*2.0/3.0-offset/3.0, edge[1]+height/3.0, 0));
		nodes.add(new Node(edge[0]+base/2.0-offset/2.0, edge[1]+height/2.0, 0));
		nodes.add(new Node(edge[0]+base-offset/2.0, edge[1]+height/2.0, 0));
		nodes.add(new Node(edge[0]+base-offset, edge[1]+height, 0));
		
		if (base*height > 0.0) {
			element_node[0] = nodes.size()-7;
			element_node[1] = nodes.size()-6;
			element_node[2] = nodes.size()-4;
			element_node[3] = nodes.size()-3;
			elementSet.add(new Quad(element_node.clone()));
			
			element_node[0] = nodes.size()-6;
			element_node[1] = nodes.size()-5;
			element_node[2] = nodes.size()-2;
			element_node[3] = nodes.size()-4;
			elementSet.add(new Quad(element_node.clone()));
			
			element_node[0] = nodes.size()-4;
			element_node[1] = nodes.size()-2;
			element_node[2] = nodes.size()-1;
			element_node[3] = nodes.size()-3;
			elementSet.add(new Quad(element_node.clone()));
		}
		else {
			element_node[0] = nodes.size()-6;
			element_node[1] = nodes.size()-7;
			element_node[2] = nodes.size()-3;
			element_node[3] = nodes.size()-4;
			elementSet.add(new Quad(element_node.clone()));
			
			element_node[0] = nodes.size()-5;
			element_node[1] = nodes.size()-6;
			element_node[2] = nodes.size()-4;
			element_node[3] = nodes.size()-2;
			elementSet.add(new Quad(element_node.clone()));
			
			element_node[0] = nodes.size()-4;
			element_node[1] = nodes.size()-3;
			element_node[2] = nodes.size()-1;
			element_node[3] = nodes.size()-2;
			elementSet.add(new Quad(element_node.clone()));
		}
		
		elements.addAll(elementSet);
		mergeCoincidentNodes(elementSet);
		sets.add(new Set(elementSet, Set.Type.COMPOSITE));
		updateModel();
	}
	
	public void addExternalSpurGearFromTrisAndQuads(double[] center, int z, double alpha, double m,
			double r) {
		
		double ra = r+m;
		double rf = r-m;
		double s = m*Math.PI/2.0;
		double sa = toothThicknessArcLengthAtRadius(ra, r, m, alpha);
		double sf = toothThicknessArcLengthAtRadius(rf, r, m, alpha);
		double theta = sf/rf;		
		
		ArrayList<Element> elementSet = new ArrayList<Element>();
		
		for (int t = 0; t < z; t++) {
			double phi = t*Math.PI*2.0/z;
			int[] element_node = new int[3];
			nodes.add(new Node(center[0], center[1], 0));
			element_node[0] = nodes.size()-1;
			double[] coord = rotatedCoords(rf, phi, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[1] = nodes.size()-1;
			coord = rotatedCoords(rf, phi+theta, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[2] = nodes.size()-1;
			elementSet.add(new Tri(element_node.clone()));
			coord = rotatedCoords(rf, phi+Math.PI*2.0/z, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[0] = nodes.size()-4;
			element_node[1] = nodes.size()-2;
			element_node[2] = nodes.size()-1;
			elementSet.add(new Tri(element_node.clone()));
			
			coord = rotatedCoords(r, phi+(theta-s/r)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			coord = rotatedCoords(r, phi+(theta+s/r)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node = new int[4];
			element_node[0] = nodes.size()-4;
			element_node[1] = nodes.size()-5;
			element_node[2] = nodes.size()-2;
			element_node[3] = nodes.size()-1;
			elementSet.add(new Quad(element_node.clone()));
			
			coord = rotatedCoords(ra, phi+(theta-sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			coord = rotatedCoords(ra, phi+(theta+sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node = new int[4];
			element_node[0] = nodes.size()-3;
			element_node[1] = nodes.size()-4;
			element_node[2] = nodes.size()-2;
			element_node[3] = nodes.size()-1;
			elementSet.add(new Quad(element_node.clone()));
			
		}
		elements.addAll(elementSet);
		mergeCoincidentNodes(elementSet);
		Set set = new Set(elementSet, Set.Type.CIRCULAR);
		set.setSpurGearValues(new SpurGearValues(alpha, m, r, false));
		sets.add(set);
		updateModel();
	}
	
	public void addInternalSpurGearFromQuads(double[] center, int z, double alpha, double m,
			double r, double rim) {
		
		double ra = r+m;
		double rf = r-m;
		double s = m*Math.PI/2.0;
		double sa = toothThicknessArcLengthAtRadius(ra, r, m, alpha);
		double sf = toothThicknessArcLengthAtRadius(rf, r, m, alpha);
		double theta = sf/rf;
		
		ArrayList<Element> elementSet = new ArrayList<Element>();
		
		for (int t = 0; t < z; t++) {
			double phi = t*Math.PI*2.0/z;
			int[] element_node = new int[4];
			double[] coord = rotatedCoords(rf, phi+Math.PI*2.0/z, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[0] = nodes.size()-1;
			coord = rotatedCoords(rf, phi+theta, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[1] = nodes.size()-1;
			coord = rotatedCoords(r, phi+(theta+s/r)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[2] = nodes.size()-1;
			coord = rotatedCoords(r, phi+Math.PI*2.0/z+(theta-s/r)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[3] = nodes.size()-1;
			elementSet.add(new Quad(element_node.clone()));
			coord = rotatedCoords(ra, phi+(theta+sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			coord = rotatedCoords(ra, phi+Math.PI*2.0/z+(theta-sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[0] = nodes.size()-3;
			element_node[1] = nodes.size()-4;
			element_node[2] = nodes.size()-2;
			element_node[3] = nodes.size()-1;
			elementSet.add(new Quad(element_node.clone()));			
			coord = rotatedCoords(ra, phi+(theta-sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			coord = rotatedCoords(ra+rim, phi+(theta-sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			coord = rotatedCoords(ra+rim, phi+(theta+sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[0] = nodes.size()-5;
			element_node[1] = nodes.size()-3;
			element_node[2] = nodes.size()-2;
			element_node[3] = nodes.size()-1;
			elementSet.add(new Quad(element_node.clone()));
			coord = rotatedCoords(ra+rim, phi+Math.PI*2.0/z+(theta-sa/ra)*0.5, center);
			nodes.add(new Node(coord[0], coord[1], 0));
			element_node[0] = nodes.size()-5;
			element_node[1] = nodes.size()-6;
			element_node[2] = nodes.size()-2;
			element_node[3] = nodes.size()-1;
			elementSet.add(new Quad(element_node.clone()));
		}
		elements.addAll(elementSet);
		mergeCoincidentNodes(elementSet);
		Set set = new Set(elementSet, Set.Type.CIRCULAR);
		set.setSpurGearValues(new SpurGearValues(alpha, m, r, true));
		sets.add(set);
		updateModel();
	}

	private double inv(double alpha) {
		return Math.tan(alpha)-alpha;
	}
	
	public double toothThicknessArcLengthAtRadius(double radius, double pitchRadius, double module,
			double pressureAngle) {
		double baseRadius = pitchRadius*Math.cos(pressureAngle);
		if (radius < baseRadius) {
			return 2.0*radius*(module*Math.PI*0.25/pitchRadius+inv(pressureAngle)-
					inv(Math.acos(Math.min(pitchRadius/baseRadius*Math.cos(pressureAngle), 1.0))));
		}
		else {
			return 2.0*radius*(module*Math.PI*0.25/pitchRadius+inv(pressureAngle)-
					inv(Math.acos(Math.min(pitchRadius/radius*Math.cos(pressureAngle), 1.0))));
		}
	}
	
	public double[] rotatedCoords(double r, double phi, double[] center) {
		double[] rotatedCoords = new double[2];
		rotatedCoords[0] = -r*Math.sin(phi)+center[0];
		rotatedCoords[1] = r*Math.cos(phi)+center[1];
		return rotatedCoords;
	}
	
	public Set getSetByElement(Element element) {
		for (int s = 0; s < sets.size(); s++) {
			if (sets.get(s).getElements().contains(element)) {
				return sets.get(s);
			}
		}
		return null;
	}
	
	public Set getSetByElementsRecursive(ArrayList<Set> sets, ArrayList<Element> elements) {
		for (int s = 0; s < sets.size(); s++) {
			if (SimLive.deepEquals(sets.get(s).getElements(), elements, Result.EQUAL) == Result.EQUAL) {
				return sets.get(s);
			}
			Set subSet = getSetByElementsRecursive(sets.get(s).getSets(), elements);
			if (subSet != null) {
				return subSet;
			}
		}
		return null;
	}
	
	public ArrayList<Set> getSetsByNode(Node node) {
		ArrayList<Set> setsByNode = new ArrayList<Set>();
		for (int s = 0; s < sets.size(); s++) {
			if (sets.get(s).getNodes().contains(node)) {
				setsByNode.add(sets.get(s));
			}
		}
		return setsByNode;
	}
	
	private boolean doElementsShareNode(Element elem0, Element elem1) {
		int[] elem0_nodes = elem0.getElementNodes();
		int[] elem1_nodes = elem1.getElementNodes();
		for (int n = 0; n < elem0_nodes.length; n++) {
			for (int m = 0; m < elem1_nodes.length; m++) {
				if (elem0_nodes[n] == elem1_nodes[m]) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isSetPartOfDistributedLoad(Set set) {
		for (int d = 0; d < distributedLoads.size(); d++) {
			DistributedLoad load = distributedLoads.get(d);
			if (load.getElementSets().contains(set)) {
				return true;
			}
		}
		return false;
	}
	
	private void recursiveFindConnectedBasicSets(Set startSet, ArrayList<Set> connectedBasicSets,
			ArrayList<Set> searchSets) {
		for (int s = 0; s < searchSets.size(); s++) {
			Set set = searchSets.get(s);
			if (startSet != set && !connectedBasicSets.contains(set) &&
				set.getType() == Set.Type.BASIC &&
				doElementsShareNode(startSet.getElements().get(0),
					set.getElements().get(0))) {
				connectedBasicSets.add(set);
				recursiveFindConnectedBasicSets(set, connectedBasicSets, searchSets);
			}
		}
	}
	
	public ArrayList<Set> getConnectedBasicSets(Set basicSet, ArrayList<Set> sets) {
		ArrayList<Set> connectedBasicSets = new ArrayList<Set>();
		connectedBasicSets.add(basicSet);
		recursiveFindConnectedBasicSets(basicSet, connectedBasicSets, sets);
		return connectedBasicSets;
	}
	
}

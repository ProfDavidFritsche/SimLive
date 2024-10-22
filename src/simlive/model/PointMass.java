package simlive.model;

import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import simlive.SimLive;
import simlive.misc.Units;
import simlive.solution.Solution;

public class PointMass extends Element {
	
	protected double mass;

	public PointMass() {
		super();
	}
	
	public PointMass(int[] element_node) {
		super();
		this.elementNodes = element_node;
		PointMass pointMass = new PointMass();
		pointMass.setMass(0.001);
		Units.convertUnitsOfElement(Units.UnitSystem.t_mm_s_N, SimLive.settings.unitSystem, pointMass);
		this.mass = pointMass.getMass();
	}
	
	public double getMass() {
		return mass;
	}

	public void setMass(double mass) {
		this.mass = mass;
	}

	@Override
	public Element clone(Model model) {
		PointMass pointMass = new PointMass();
		pointMass.elementNodes = this.elementNodes.clone();
		pointMass.mass = this.mass;
		pointMass.id = this.id;
		pointMass.stiffnessDamping = this.stiffnessDamping;
		pointMass.massDamping = this.massDamping;
		return pointMass;
	}

	@Override
	public boolean deepEquals(Object obj) {
		if (!(obj instanceof PointMass)) return false;
		PointMass element = (PointMass) obj;
		if (this.getType() != element.getType()) return false;
		if (!Arrays.equals(this.elementNodes, element.elementNodes)) return false;
		if (this.mass != element.mass) return false;
		if (this.id != element.id) return false;
		if (this.stiffnessDamping != element.stiffnessDamping) return false;
		if (this.massDamping != element.massDamping) return false;
		return true;
	}

	@Override
	public boolean isLineElement() {
		return false;
	}
	
	@Override
	public boolean isPlaneElement() {
		return false;
	}

	@Override
	public void adaptNodeIDs(int deleteNodeID) {
		if (elementNodes[0] >= deleteNodeID) elementNodes[0]--;
	}

	@Override
	public Type getType() {
		return Element.Type.POINT_MASS;
	}

	@Override
	public String getTypeString() {
		return "Point Mass";
	}

	@Override
	public Matrix getElementStiffness(ArrayList<Node> nodes) {
		return new Matrix(3, 3);
	}

	@Override
	public Matrix getElementStiffnessNL(ArrayList<Node> nodes, Matrix u_global) {
		return new Matrix(3, 3);
	}

	@Override
	public Matrix getElementForce(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {
		return new Matrix(3, 1);
	}

	@Override
	public Matrix getElementForceNL(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {
		return new Matrix(3, 1);
	}

	@Override
	protected Matrix getMelem(ArrayList<Node> nodes) {
		Matrix M_elem = new Matrix(3, 3);
		M_elem.set(0, 0, mass);
		M_elem.set(1, 1, mass);
		M_elem.set(2, 2, mass);
		return M_elem;
	}

	@Override
	public void setIndexIncidence(Solution solution, ArrayList<Node> nodes) {
		
		I = new Matrix(3, 1);
		
		for (int n = 0; n < nodes.size(); n++) {
			int dof = solution.getDofOfNodeID(n);
			if (n == elementNodes[0]) {
				I.set(0, 0, dof);
				I.set(1, 0, dof+1);
				I.set(2, 0, dof+2);
			}
		}
	}

	@Override
	public String[] getLocalDofNames() {
		String[] dofNames = new String[3];
		dofNames[0] = "u"+(1);
		dofNames[1] = "v"+(2);
		dofNames[2] = "w"+(3);
		return dofNames;
	}

	@Override
	public ArrayList<Element> refine(ArrayList<Node> nodes, ArrayList<Element> elements) {
		// PointMass cannot be refined
		return null;
	}

	@Override
	public void update() {
		updateIDAndMaterial();
	}

}

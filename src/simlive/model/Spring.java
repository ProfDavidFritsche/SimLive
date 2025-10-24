package simlive.model;
import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import simlive.SimLive;
import simlive.misc.Units;
import simlive.solution.Increment;
import simlive.solution.Solution;

public class Spring extends LineElement {
	
	private double stiffness;
	
	private Spring() {
		super();
	}
	
	public Spring(int[] element_node) {
		super(element_node);
		Spring spring = new Spring();
		spring.setStiffness(10.0);
		Units.convertUnitsOfElement(Units.UnitSystem.t_mm_s_N, SimLive.settings.unitSystem, spring);
		this.stiffness = spring.getStiffness();
	}
	
	public void setStiffness(double stiffness) {
		this.stiffness = stiffness;
	}

	public double getStiffness() {
		return this.stiffness;
	}
	
	@Override
	public boolean isSectionValid(ArrayList<Section> sections) {
		return true;
	}

	@Override
	public Type getType() {
		return Element.Type.SPRING;
	}

	@Override
	public String getTypeString() {
		return "Spring";
	}

	@Override
	public Matrix getElementStiffness(ArrayList<Node> nodes) {
		
		if (Tt_K_elem_T == null) {
			Matrix K_elem = new Matrix(2, 2);
			
			K_elem.set(0, 0, 1.0);
			K_elem.set(0, 1, -1.0);
			K_elem.set(1, 0, -1.0);
			K_elem.set(1, 1, 1.0);
			
			K_elem = K_elem.times(stiffness);
			
			Matrix T = getTransformation();
			
			Tt_K_elem_T = T.transpose().times(K_elem).times(T);
		}
		return Tt_K_elem_T;
	}

	@Override
	public Matrix getElementStiffnessNL(ArrayList<Node> nodes, Matrix u_global) {
		
		//Large displacement, large strain formulation
		//Co-rotational formulation
		Matrix u_elem = globalToLocalVector(u_global);
		
		double l = getCurrentLength(nodes, u_elem);
		double l0 = getLength();
		Matrix T = getCurrentTransformation(nodes, u_elem);
		
		Matrix K_elem = new Matrix(2, 2);
		
		K_elem.set(0, 0, 1.0);
		K_elem.set(0, 1, -1.0);
		K_elem.set(1, 0, -1.0);
		K_elem.set(1, 1, 1.0);
		
		K_elem = K_elem.times(stiffness);
		
		double N = (l-l0)*stiffness;
		
		Matrix K_geo = new Matrix(2, 2);
		
		K_geo.set(0, 0, 1.0);
		K_geo.set(0, 1, -1.0);
		K_geo.set(1, 0, -1.0);
		K_geo.set(1, 1, 1.0);
		
		K_geo = K_geo.times(N/l0);
		
		return T.transpose().times(K_elem.plus(K_geo)).times(T);
	}

	@Override
	public Matrix getElementForce(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {

		Matrix K_elem = getElementStiffness(nodes);
		
		Matrix u_elem = globalToLocalVector(u_global);
		
		Matrix f_int = K_elem.times(u_elem);
		
		if (localSys) {
			Matrix T = getTransformation();
			f_int = T.times(f_int);
		}
		
		return f_int;
	}

	@Override
	public Matrix getElementForceNL(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {
		
		//Large displacement, large strain formulation
		//Co-rotational formulation
		Matrix u_elem = globalToLocalVector(u_global);
		
		double l = getCurrentLength(nodes, u_elem);
		double l0 = getLength();
		
		Matrix f_int = new Matrix(2, 1);
		f_int.set(0, 0, -(l-l0)*stiffness);
		f_int.set(1, 0, (l-l0)*stiffness);
		
		if (!localSys) {
			Matrix T = getCurrentTransformation(nodes, u_elem);
			f_int = T.transpose().times(f_int);
		}
		
		return f_int;
	}

	@Override
	protected Matrix getMelem(ArrayList<Node> nodes) {
		return new Matrix(6, 6);
	}

	private Matrix getTransformation() {
		
		double length = getLength();
		double[] r1 = new double[3];
		r1[0] = getDeltaX()/length;
		r1[1] = getDeltaY()/length;
		r1[2] = getDeltaZ()/length;
		
		Matrix T = new Matrix(2, 6);
		T.set(0, 0, r1[0]);
		T.set(0, 1, r1[1]);
		T.set(0, 2, r1[2]);
		T.set(1, 3, r1[0]);
		T.set(1, 4, r1[1]);
		T.set(1, 5, r1[2]);
		
		return T;
	}
	
	private Matrix getCurrentTransformation(ArrayList<Node> nodes, Matrix u_elem) {
		
		double length = getCurrentLength(nodes, u_elem);
		double[] r1 = new double[3];
		r1[0] = getCurrentDeltaX(nodes, u_elem)/length;
		r1[1] = getCurrentDeltaY(nodes, u_elem)/length;
		r1[2] = getCurrentDeltaZ(nodes, u_elem)/length;
		
		Matrix T = new Matrix(2, 6);
		T.set(0, 0, r1[0]);
		T.set(0, 1, r1[1]);
		T.set(0, 2, r1[2]);
		T.set(1, 3, r1[0]);
		T.set(1, 4, r1[1]);
		T.set(1, 5, r1[2]);
		
		return T;
	}
	
	@Override
	public void setIndexIncidence(Solution solution, ArrayList<Node> nodes) {

		I = new Matrix(6, 1);
		
		for (int n = 0; n < nodes.size(); n++) {
			int dof = solution.getDofOfNodeID(n);
			if (n == elementNodes[0]) {
				I.set(0, 0, dof);
				I.set(1, 0, dof+1);
				I.set(2, 0, dof+2);
			}
			if (n == elementNodes[1]) {
				I.set(3, 0, dof);
				I.set(4, 0, dof+1);
				I.set(5, 0, dof+2);
			}
		}
	}
	
	@Override
	public ArrayList<Element> refine(ArrayList<Node> nodes, ArrayList<Element> elements) {
		ArrayList<Element> newElements = new ArrayList<Element>();
		double x, y, z;
		x = 0.5 * (nodes.get(elementNodes[0]).getXCoord() + nodes.get(elementNodes[1]).getXCoord());
		y = 0.5 * (nodes.get(elementNodes[0]).getYCoord() + nodes.get(elementNodes[1]).getYCoord());
		z = 0.5 * (nodes.get(elementNodes[0]).getZCoord() + nodes.get(elementNodes[1]).getZCoord());
		nodes.add(new Node(x, y, z));
		int[][] element_node = {{elementNodes[0], nodes.size()-1}, {nodes.size()-1, elementNodes[1]}};
		this.setElementNodes(element_node[0]);
		Spring newElement = new Spring();
		newElement.setElementNodes(element_node[1]);
		newElement.setStiffness(this.getStiffness());
		newElement.setQ0(q0.clone());
		newElement.setStiffnessDamping(this.getStiffnessDamping());
		newElement.setMassDamping(this.getMassDamping());
		elements.add(elements.indexOf(this)+1, newElement);
		newElements.add(newElement);
		return newElements;
	}

	@Override
	public String[] getLocalDofNames() {
		String[] dofNames = new String[6];
		for (int counter = 0, n = 0; n < 2; n++) {
			dofNames[counter] = "u"+(n+1);
			counter++;
			dofNames[counter] = "v"+(n+1);
			counter++;
			dofNames[counter] = "w"+(n+1);
			counter++;
		}
		return dofNames;
	}

	@Override
	public Element clone(Model model) {
		Spring spring = new Spring();
		spring.elementNodes = this.elementNodes.clone();
		spring.stiffness = this.stiffness;
		spring.R0 = this.R0.copy();
		spring.q0 = this.q0.clone();
		spring.id = this.id;
		spring.stiffnessDamping = this.stiffnessDamping;
		spring.massDamping = this.massDamping;
		return spring;
	}

	@Override
	public Result deepEquals(Object obj, Result result) {
		if (!(obj instanceof Spring)) return Result.RECALC;
		Spring element = (Spring) obj;
		if (this.getType() != element.getType()) return Result.RECALC;
		if (!Arrays.equals(this.elementNodes, element.elementNodes)) return Result.RECALC;
		if (this.stiffness != element.stiffness) return Result.RECALC;
		if (!Arrays.equals(this.R0.getRowPackedCopy(), element.R0.getRowPackedCopy())) return Result.RECALC;
		if (!Arrays.equals(this.q0, element.q0)) return Result.RECALC;
		if (this.id != element.id) return Result.RECALC;
		if (this.stiffnessDamping != element.stiffnessDamping) return Result.RECALC;
		if (this.massDamping != element.massDamping) return Result.RECALC;
		return result;
	}

	@Override
	public double[] getShapeFunctionValues(double t) {
		double[] h = new double[2];
		h[0] = 1.0-t;
		h[1] = t;
		return h;
	}

	@Override
	public double[] interpolateNodeKinematicValues(double t, Increment increment, int val) {
		double[][] xyzVal = new double[2][3];
		switch (val) {
			case 0:	for (int i = 0; i < 2; i++) {
						xyzVal[i] = increment.getDisplacement(elementNodes[i]);
					}
					break;
			case 1:	for (int i = 0; i < 2; i++) {
						xyzVal[i] = increment.getAcceleration(elementNodes[i]);
					}
					break;
			case 2:	for (int i = 0; i < 2; i++) {
						xyzVal[i] = increment.getVelocity(elementNodes[i]);
					}
					break;
		}
		double[] values = new double[3];
		values[0] = xyzVal[0][0]+(xyzVal[1][0]-xyzVal[0][0])*t;
		values[1] = xyzVal[0][1]+(xyzVal[1][1]-xyzVal[0][1])*t;
		values[2] = xyzVal[0][2]+(xyzVal[1][2]-xyzVal[0][2])*t;
		return values;
	}

}

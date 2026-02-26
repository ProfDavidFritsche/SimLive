package simlive.model;
import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import simlive.solution.Increment;
import simlive.solution.Solution;

public class Rod extends LineElement {
	
	private boolean storedEdge = false;
	
	public boolean isStoredEdge() {
		return storedEdge;
	}

	public void setStoredEdge(boolean storedEdge) {
		this.storedEdge = storedEdge;
	}

	private Rod() {
		super();
	}
	
	public Rod(int[] element_node) {
		super(element_node);
	}
	
	@Override
	public boolean isSectionValid(ArrayList<Section> sections) {
		return sections.contains(section);
	}

	@Override
	public Type getType() {
		return Element.Type.ROD;
	}

	@Override
	public String getTypeString() {
		return "Rod";
	}

	@Override
	public Matrix getElementStiffness(ArrayList<Node> nodes) {
		
		if (Tt_K_elem_T == null) {
			Matrix K_elem = new Matrix(2, 2);
			
			double E, A, l;
			E = material.getYoungsModulus();
			A = section.getArea();
			l = getLength();
			
			K_elem.set(0, 0, 1.0);
			K_elem.set(0, 1, -1.0);
			K_elem.set(1, 0, -1.0);
			K_elem.set(1, 1, 1.0);
			
			K_elem = K_elem.times(E*A/l);
			
			Matrix T = getTransformation();
			
			Tt_K_elem_T = T.transpose().times(K_elem).times(T);
		}
		return Tt_K_elem_T;
	}

	@Override
	public Matrix getElementStiffnessNL(ArrayList<Node> nodes, Matrix u_global) {
		
		//Large displacement, large strain formulation
		//Co-rotational formulation
		double E, A, l0;
		E = material.getYoungsModulus();
		A = section.getArea();
		l0 = getLength();
		Matrix u_elem = globalToLocalVector(u_global);
		
		double l = getCurrentLength(nodes, u_elem);
		Matrix T = getCurrentTransformation(nodes, u_elem);
		
		Matrix K_elem = new Matrix(2, 2);
		
		K_elem.set(0, 0, 1.0);
		K_elem.set(0, 1, -1.0);
		K_elem.set(1, 0, -1.0);
		K_elem.set(1, 1, 1.0);
		
		K_elem = K_elem.times(E*A/l0);
		
		double N = (l-l0)*E*A/l0;
		
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
		double E, A, l0;
		E = material.getYoungsModulus();
		A = section.getArea();
		l0 = getLength();
		Matrix u_elem = globalToLocalVector(u_global);
		
		double l = getCurrentLength(nodes, u_elem);
		
		Matrix f_int = new Matrix(2, 1);
		f_int.set(0, 0, -(l-l0)*E*A/l0);
		f_int.set(1, 0, (l-l0)*E*A/l0);
		
		if (!localSys) {
			Matrix T = getCurrentTransformation(nodes, u_elem);
			f_int = T.transpose().times(f_int);
		}
		
		return f_int;
	}

	@Override
	protected Matrix getMelem(ArrayList<Node> nodes) {
		
		double rho, A, l;
		rho = material.getDensity();
		A = section.getArea();
		l = getLength();
		
		Matrix M_elem = null;
		
		/*{
			// lumped mass matrix 
			M_elem = Matrix.identity(4, 4);
			M_elem = M_elem.times(rho*A*l/2.0);
		}*/
		
		{
			/* consistent mass matrix */
			M_elem = new Matrix(6, 6);
			
			M_elem.set(0, 0, 2.0);
			M_elem.set(1, 1, 2.0);
			M_elem.set(2, 2, 2.0);
			M_elem.set(3, 3, 2.0);
			M_elem.set(4, 4, 2.0);
			M_elem.set(5, 5, 2.0);
			M_elem.set(0, 3, 1.0);
			M_elem.set(1, 4, 1.0);
			M_elem.set(2, 5, 1.0);
			M_elem.set(3, 0, 1.0);
			M_elem.set(4, 1, 1.0);
			M_elem.set(5, 2, 1.0);
			
			M_elem = M_elem.times(rho*A*l/6.0);
		}
		
		/* no need to transform */
		return M_elem;
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
		Rod newElement = new Rod(element_node[1]);
		newElement.setMaterial(this.getMaterial());
		newElement.setSection(this.getSection());
		newElement.setQ0(q0.clone());
		newElement.setStiffnessDamping(this.getStiffnessDamping());
		newElement.setMassDamping(this.getMassDamping());
		elements.add(this.getID()+1, newElement);
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
		Rod rod = new Rod();
		rod.storedEdge = this.storedEdge;
		rod.elementNodes = this.elementNodes.clone();
		rod.material = findMaterial(model.getMaterials(), material);
		rod.section = findSection(model.getSections(), section);
		rod.R0 = this.R0.copy();
		rod.q0 = this.q0.clone();
		rod.id = this.id;
		rod.stiffnessDamping = this.stiffnessDamping;
		rod.massDamping = this.massDamping;
		return rod;
	}

	@Override
	public Result deepEquals(Object obj, Result result) {
		if (!(obj instanceof Rod)) return Result.RECALC;
		Rod element = (Rod) obj;
		if (this.storedEdge != element.storedEdge) return Result.RECALC;
		if (this.getType() != element.getType()) return Result.RECALC;
		if (!Arrays.equals(this.elementNodes, element.elementNodes)) return Result.RECALC;
		if (this.material != null && element.material != null)
			result = this.material.deepEquals(element.material, result);
		if (this.section != null && element.section != null)
			result = this.section.deepEquals(element.section, result);
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

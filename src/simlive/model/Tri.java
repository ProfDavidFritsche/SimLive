package simlive.model;
import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import simlive.SimLive;
import simlive.misc.Units;
import simlive.solution.Solution;

public class Tri extends PlaneElement {
	
	public Tri() {
		super();
		this.elementNodes = new int[3];
	}
	
	public Tri(int[] element_node) {
		super();
		this.elementNodes = element_node;
		Tri tri = new Tri();
		tri.setThickness(1.0);
		Units.convertUnitsOfElement(Units.UnitSystem.t_mm_s_N, SimLive.settings.unitSystem, tri);
		this.thickness = tri.getThickness();
		//this.state = PlaneElement.State.PLANE_STRESS;
		ArrayList<Node> nodes = SimLive.model.getNodes();
		nodes.get(element_node[0]).setRotationalDOF(true);
		nodes.get(element_node[1]).setRotationalDOF(true);
		nodes.get(element_node[2]).setRotationalDOF(true);
	}
	
	@Override
	public double[] getLocalCoords(int localNodeID) {
		double[] localCoords = new double[2];
		switch (localNodeID) {
			case 0:	localCoords[0] = 0.0;
					localCoords[1] = 0.0;
					break;
			case 1:	localCoords[0] = 1.0;
					localCoords[1] = 0.0;
					break;
			case 2:	localCoords[0] = 0.0;
					localCoords[1] = 1.0;
					break;
		}
		return localCoords;
	}
	
	@Override
	public Type getType() {
		return Element.Type.TRI;
	}

	@Override
	public String getTypeString() {
		return "Tri";
	}
	
	@Override
	protected Matrix getJacobian(double[] x, double[] y, double r, double s) {
		Matrix J = new Matrix(2, 2);
		J.set(0, 0, x[1]-x[0]);
		J.set(0, 1, y[1]-y[0]);
		J.set(1, 0, x[2]-x[0]);
		J.set(1, 1, y[2]-y[0]);
		return J;
	}
	
	private Matrix getShapeFunctionDerivativesMembrane(Matrix Jinv) {
		Matrix N = new Matrix(2, 3);
		N.set(0, 0, -Jinv.get(0, 0)-Jinv.get(0, 1));
		N.set(0, 1,  Jinv.get(0, 0));
		N.set(0, 2,  Jinv.get(0, 1));
		N.set(1, 0, -Jinv.get(1, 0)-Jinv.get(1, 1));
		N.set(1, 1,  Jinv.get(1, 0));
		N.set(1, 2,  Jinv.get(1, 1));
		return N;
	}
	
	@Override
	protected Matrix getBMatrixPlate(double[][] x, Matrix Jinv, double r, double s) {
		
		double[][] f = getFactors(x);
		double P4 = f[0][0];
		double P5 = f[0][1];
		double P6 = f[0][2];
		double q4 = f[1][0];
		double q5 = f[1][1];
		double q6 = f[1][2];
		double r4 = f[2][0];
		double r5 = f[2][1];
		double r6 = f[2][2];
		double t4 = f[3][0];
		double t5 = f[3][1];
		double t6 = f[3][2];
		
		Matrix hxr = new Matrix(1, 9);		
		hxr.set(0, 0, P4*(1.0-2.0*r)+(P6-P4)*s);
		hxr.set(0, 1, q4*(1.0-2.0*r)-(q4+q6)*s);
		hxr.set(0, 2, -4.0+6.0*(r+s)+r4*(1.0-2.0*r)-(r4+r6)*s);		
		hxr.set(0, 3, -P4*(1.0-2.0*r)+(P4+P5)*s);
		hxr.set(0, 4, q4*(1.0-2.0*r)-(q4-q5)*s);
		hxr.set(0, 5, -2.0+6.0*r+r4*(1.0-2.0*r)+(r5-r4)*s);		
		hxr.set(0, 6, -(P5+P6)*s);
		hxr.set(0, 7, (q5-q6)*s);
		hxr.set(0, 8, -(r6-r5)*s);
		Matrix hxs = new Matrix(1, 9);		
		hxs.set(0, 0, -P6*(1.0-2.0*s)-(P4-P6)*r);
		hxs.set(0, 1, q6*(1.0-2.0*s)-(q4+q6)*r);
		hxs.set(0, 2, -4.0+6.0*(r+s)+r6*(1.0-2.0*s)-(r4+r6)*r);		
		hxs.set(0, 3, (P4+P5)*r);
		hxs.set(0, 4, (q5-q4)*r);
		hxs.set(0, 5, -(r4-r5)*r);
		hxs.set(0, 6, P6*(1.0-2.0*s)-(P5+P6)*r);
		hxs.set(0, 7, q6*(1.0-2.0*s)+(q5-q6)*r);
		hxs.set(0, 8, -2.0+6.0*s+r6*(1.0-2.0*s)+(r5-r6)*r);		
		Matrix hyr = new Matrix(1, 9);		
		hyr.set(0, 0, t4*(1.0-2.0*r)+(t6-t4)*s);
		hyr.set(0, 1, 1.0+r4*(1.0-2.0*r)-(r4+r6)*s);
		hyr.set(0, 2, -q4*(1.0-2.0*r)+(q4+q6)*s);		
		hyr.set(0, 3, -t4*(1.0-2.0*r)+(t4+t5)*s);
		hyr.set(0, 4, -1.0+r4*(1.0-2.0*r)+(r5-r4)*s);
		hyr.set(0, 5, -q4*(1.0-2.0*r)-(q5-q4)*s);		
		hyr.set(0, 6, -(t6+t5)*s);
		hyr.set(0, 7, (r5-r6)*s);
		hyr.set(0, 8, -(q5-q6)*s);
		Matrix hys = new Matrix(1, 9);		
		hys.set(0, 0, -t6*(1.0-2.0*s)-(t4-t6)*r);
		hys.set(0, 1, 1.0+r6*(1.0-2.0*s)-(r4+r6)*r);
		hys.set(0, 2, -q6*(1.0-2.0*s)+(q4+q6)*r);		
		hys.set(0, 3, (t4+t5)*r);
		hys.set(0, 4, (r5-r4)*r);
		hys.set(0, 5, -(q5-q4)*r);
		hys.set(0, 6, t6*(1.0-2.0*s)-(t5+t6)*r);
		hys.set(0, 7, -1.0+r6*(1.0-2.0*s)+(r5-r6)*r);
		hys.set(0, 8, -q6*(1.0-2.0*s)-(q5-q6)*r);
		
		Matrix B = new Matrix(3, 9);
		B.setMatrix(0, 0, 0, 8, hxr.times(Jinv.get(0, 0)).plus(hxs.times(Jinv.get(0, 1))));
		B.setMatrix(1, 1, 0, 8, hyr.times(Jinv.get(1, 0)).plus(hys.times(Jinv.get(1, 1))));
		B.setMatrix(2, 2, 0, 8, hyr.times(Jinv.get(0, 0)).plus(hys.times(Jinv.get(0, 1))).
				plus(hxr.times(Jinv.get(1, 0)).plus(hxs.times(Jinv.get(1, 1)))));
		return B;
	}
	
	@Override
	protected Matrix getBMatrixMembrane(Matrix Jinv, double r, double s) {
		Matrix N = getShapeFunctionDerivativesMembrane(Jinv);
		Matrix B = new Matrix(3, 6);
		B.set(0, 0, N.get(0, 0));
		B.set(0, 2, N.get(0, 1));
		B.set(0, 4, N.get(0, 2));
		B.set(1, 1, N.get(1, 0));
		B.set(1, 3, N.get(1, 1));
		B.set(1, 5, N.get(1, 2));
		B.set(2, 0, N.get(1, 0));
		B.set(2, 1, N.get(0, 0));
		B.set(2, 2, N.get(1, 1));
		B.set(2, 3, N.get(0, 1));
		B.set(2, 4, N.get(1, 2));
		B.set(2, 5, N.get(0, 2));
		return B;
	}
	
	private double[][] getFactors(double[][] x) {
		double[][] f = new double[4][3];
		for (int i = 0; i < 3; i++) {
			double xij = x[0][i]-x[0][(i+1)%3];
			double yij = x[1][i]-x[1][(i+1)%3];
			double lij = xij*xij+yij*yij;
			f[0][i] = -6.0*xij/lij;
			f[1][i] = 3.0*xij*yij/lij;
			f[2][i] = 3.0*yij*yij/lij;
			f[3][i] = -6.0*yij/lij;
		}
		return f;
	}
	
	@Override
	protected Matrix getKelem(ArrayList<Node> nodes) {
		Matrix K_elem_plate = new Matrix(9, 9);
		Matrix K_elem_membrane = new Matrix(6, 6);
		
		double t = getThickness();		
		Matrix C = getMaterialMatrix();
		Matrix Cp = C.times(t*t/12.0);
		
		double[][] x = new double[2][3];
		for (int i = 0; i < 3; i++) {
			Matrix global = new Matrix(nodes.get(elementNodes[i]).getCoords(), 3);
			Matrix local = R0.transpose().times(global);
			x[0][i] = local.get(0, 0);
			x[1][i] = local.get(1, 0);
		}
		
		Matrix J = getJacobian(x[0], x[1], 0, 0);
		double Jdet = J.det();
		Matrix Jinv = J.inverse();
		Matrix Bm = getBMatrixMembrane(Jinv, 0, 0);
		K_elem_membrane.plusEquals(Bm.transpose().times(C).times(Bm).times(t*Jdet/2.0));
		
		double[][] ip = new double[][]{{0.5, 0}, {0.5, 0.5}, {0, 0.5}};
		for (int i = 0; i < 3; i ++) {
			double r = ip[i][0];
			double s = ip[i][1];
			Matrix Bp = getBMatrixPlate(x, Jinv, r, s);
			K_elem_plate.plusEquals(Bp.transpose().times(Cp).times(Bp).times(t*Jdet/6.0));
		}
				
		Matrix K_elem = new Matrix(18, 18);
		for (int n = 0; n < 3; n++) {
			for (int m = 0; m < 3; m++) {
				K_elem.setMatrix(n*6, n*6+1, m*6, m*6+1, K_elem_membrane.getMatrix(n*2, n*2+1, m*2, m*2+1));
				K_elem.setMatrix(n*6+2, n*6+4, m*6+2, m*6+4, K_elem_plate.getMatrix(n*3, n*3+2, m*3, m*3+2));
			}
		}
		
		for (int n = 0; n < 3; n++) {
			double maxDiag = Math.max(K_elem.get(n*6+3, n*6+3), K_elem.get(n*6+4, n*6+4));
			K_elem.set(n*6+5, n*6+5, 10e-3*maxDiag);
		}
		
		return K_elem;
	}
	
	private Matrix getFLocal(ArrayList<Node> nodes, Matrix u_elem, Matrix RrT) {
		
		Matrix temp = new Matrix(18, 1);
		Matrix r0 = new Matrix(nodes.get(elementNodes[0]).getCoords(), 3);
		Matrix r0u = r0.plus(u_elem.getMatrix(0, 2, 0, 0));
		for (int n = 0; n < 3; n++) {
			Matrix rn = new Matrix(nodes.get(elementNodes[n]).getCoords(), 3);
			Matrix rnu = rn.plus(u_elem.getMatrix(n*6, n*6+2, 0, 0));
			Matrix uloc = RrT.times(rnu.minus(r0u)).minus(R0.transpose().times(rn.minus(r0)));
			temp.setMatrix(n*6, n*6+2, 0, 0, uloc);
			Matrix Rg = Beam.rotationMatrixFromAngles(u_elem.getMatrix(n*6+3, n*6+5, 0, 0));
			Matrix Rloc = RrT.times(Rg).times(R0);
			double[] rloc = Beam.anglesFromRotationMatrix(Rloc);
			temp.set(n*6+3, 0, rloc[0]);
			temp.set(n*6+4, 0, rloc[1]);
			temp.set(n*6+5, 0, rloc[2]);
		}
		
		return K_elem.times(temp);
	}
	
	@Override
	public Matrix getElementStiffness(ArrayList<Node> nodes) {
		
		if (Tt_K_elem_T == null) {
			Matrix T = getTransformation();
			
			Tt_K_elem_T = T.transpose().times(K_elem).times(T);
		}
		return Tt_K_elem_T;
	}
	
	private Matrix getTransformation() {
		Matrix R0T = R0.transpose();
		Matrix T = new Matrix(18, 18);
		T.setMatrix(0, 2, 0, 2, R0T);
		T.setMatrix(3, 5, 3, 5, R0T);
		T.setMatrix(6, 8, 6, 8, R0T);
		T.setMatrix(9, 11, 9, 11, R0T);
		T.setMatrix(12, 14, 12, 14, R0T);
		T.setMatrix(15, 17, 15, 17, R0T);
		return T;
	}
	
	private Matrix getE(Matrix Rr) {
		Matrix E = new Matrix(18, 18);
		E.setMatrix(0, 2, 0, 2, Rr);
		E.setMatrix(3, 5, 3, 5, Rr);
		E.setMatrix(6, 8, 6, 8, Rr);
		E.setMatrix(9, 11, 9, 11, Rr);
		E.setMatrix(12, 14, 12, 14, Rr);
		E.setMatrix(15, 17, 15, 17, Rr);
		return E;
	}
	
	private Matrix getP(ArrayList<Node> nodes, Matrix u_elem, Matrix RrT) {
		Matrix r0 = new Matrix(nodes.get(elementNodes[0]).getCoords(), 3);
		r0.plusEquals(u_elem.getMatrix(0, 2, 0, 0));
		Matrix r1 = new Matrix(nodes.get(elementNodes[1]).getCoords(), 3);
		r1.plusEquals(u_elem.getMatrix(6, 8, 0, 0));
		Matrix r2 = new Matrix(nodes.get(elementNodes[2]).getCoords(), 3);
		r2.plusEquals(u_elem.getMatrix(12, 14, 0, 0));
		Matrix I3 = Matrix.identity(3, 3);
		Matrix A = new Matrix(18, 3);
		//A.setMatrix(0, 2, 0, 2, Beam.getSkewSymmetricMatrix(RrT.times(r0.minus(r0))));
		A.setMatrix(3, 5, 0, 2, I3);
		A.setMatrix(6, 8, 0, 2, Beam.getSkewSymmetricMatrix(RrT.times(r0.minus(r1))));
		A.setMatrix(9, 11, 0, 2, I3);
		A.setMatrix(12, 14, 0, 2, Beam.getSkewSymmetricMatrix(RrT.times(r0.minus(r2))));
		A.setMatrix(15, 17, 0, 2, I3);
		
		Matrix G = getG(nodes, u_elem, RrT);
		//G.transpose().times(A).print(10, 10); //has to be I3
		return Matrix.identity(18, 18).minus(A.times(G.transpose()));
	}
	
	private Matrix getG(ArrayList<Node> nodes, Matrix u_elem, Matrix RrT) {
		Matrix r0 = new Matrix(nodes.get(elementNodes[0]).getCoords(), 3);
		r0.plusEquals(u_elem.getMatrix(0, 2, 0, 0));
		Matrix r1 = new Matrix(nodes.get(elementNodes[1]).getCoords(), 3);
		r1.plusEquals(u_elem.getMatrix(6, 8, 0, 0));
		Matrix r2 = new Matrix(nodes.get(elementNodes[2]).getCoords(), 3);
		r2.plusEquals(u_elem.getMatrix(12, 14, 0, 0));
		Matrix x1 = RrT.times(r1.minus(r0));
		Matrix x2 = RrT.times(r2.minus(r0));
		Matrix G = new Matrix(18, 3);
		G.set(1, 2, -1.0/x1.get(0, 0));
		G.set(2, 0, (x2.get(0, 0)-x1.get(0, 0))/(x2.get(1, 0)*x1.get(0, 0)));
		G.set(2, 1, 1.0/x1.get(0, 0));
		G.set(7, 2, 1.0/x1.get(0, 0));
		G.set(8, 0, -x2.get(0, 0)/(x2.get(1, 0)*x1.get(0, 0)));
		G.set(8, 1, -1.0/x1.get(0, 0));
		G.set(14, 0, 1.0/x2.get(1, 0));
		return G;
	}
	
	@Override
	public Matrix getElementStiffnessNL(ArrayList<Node> nodes, Matrix u_global) {
		
		Matrix u_elem = globalToLocalVector(u_global);
		Matrix Rr = getRr(nodes, u_elem);
		Matrix RrT = Rr.transpose();
		Matrix E = getE(Rr);
		Matrix P = getP(nodes, u_elem, RrT);
		Matrix f_int = /*P.transpose().times(*/getFLocal(nodes, u_elem, RrT)/*)*/;
		Matrix F1 = new Matrix(18, 3);
		Matrix F2 = new Matrix(18, 3);
		for (int i = 0; i < 3; i++) {
			Matrix niT = Beam.getSkewSymmetricMatrix(f_int.getMatrix(i*6, i*6+2, 0, 0));
			F1.setMatrix(i*6, i*6+2, 0, 2, niT);
			F2.setMatrix(i*6, i*6+2, 0, 2, niT);
			Matrix miT = Beam.getSkewSymmetricMatrix(f_int.getMatrix(i*6+3, i*6+5, 0, 0));
			F2.setMatrix(i*6+3, i*6+5, 0, 2, miT);
		}
		
		Matrix G = getG(nodes, u_elem, RrT);
		
		return E.times(P.transpose().times(K_elem).times(P).
				minus(G.times(F1.transpose()).times(P)).
				minus(F2.times(G.transpose()))).times(E.transpose());
	}

	@Override
	public Matrix getElementForce(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {
		
		Matrix K_elem = getElementStiffness(nodes);
		
		Matrix u_elem = globalToLocalVector(u_global);
		
		Matrix f_int = K_elem.times(u_elem);
		
		return f_int;
	}

	@Override
	public Matrix getElementForceNL(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {
		
		Matrix u_elem = globalToLocalVector(u_global);
		Matrix Rr = getRr(nodes, u_elem);
		Matrix RrT = Rr.transpose();
		
		Matrix f_int = getFLocal(nodes, u_elem, RrT);
		
		Matrix E = getE(Rr);
		Matrix P = getP(nodes, u_elem, RrT);
		
		return E.times(P.transpose().times(f_int));
	}

	@Override
	protected Matrix getMelem(ArrayList<Node> nodes) {

		double rho, t;
		rho = material.getDensity();
		t = getThickness();
		
		double[] x = new double[3];
		double[] y = new double[3];
		for (int i = 0; i < 3; i++) {
			Matrix global = new Matrix(nodes.get(elementNodes[i]).getCoords(), 3);
			Matrix local = R0.transpose().times(global);
			x[i] = local.get(0, 0);
			y[i] = local.get(1, 0);
		}
		
		Matrix J = getJacobian(x, y, 0, 0);
		double Jdet = J.det()/6.0;
		
		Matrix M_elem = new Matrix(18, 18);
		
		{
			// lumped mass matrix
			for (int n = 0; n < 3; n++) {
				double mass = rho*Jdet*t;
				M_elem.set(n*6, n*6, mass);
				M_elem.set(n*6+1, n*6+1, mass);
				M_elem.set(n*6+2, n*6+2, mass);
				M_elem.set(n*6+3, n*6+3, mass*Jdet/100.0);
				M_elem.set(n*6+4, n*6+4, mass*Jdet/100.0);
				M_elem.set(n*6+5, n*6+5, mass*Jdet/100.0);
			}
		}
		
		/* no need to transform */
		return M_elem;
	}


	@Override
	public void setIndexIncidence(Solution solution, ArrayList<Node> nodes) {

		I = new Matrix(18, 1);
		
		for (int n = 0; n < nodes.size(); n++) {
			int dof = solution.getDofOfNodeID(n);
			if (n == elementNodes[0]) {
				I.set(0, 0, dof);
				I.set(1, 0, dof+1);
				I.set(2, 0, dof+2);
				I.set(3, 0, dof+3);
				I.set(4, 0, dof+4);
				I.set(5, 0, dof+5);
			}
			if (n == elementNodes[1]) {
				I.set(6, 0, dof);
				I.set(7, 0, dof+1);
				I.set(8, 0, dof+2);
				I.set(9, 0, dof+3);
				I.set(10, 0, dof+4);
				I.set(11, 0, dof+5);
			}
			if (n == elementNodes[2]) {
				I.set(12, 0, dof);
				I.set(13, 0, dof+1);
				I.set(14, 0, dof+2);
				I.set(15, 0, dof+3);
				I.set(16, 0, dof+4);
				I.set(17, 0, dof+5);
			}
		}
	}
	
	@Override
	public ArrayList<Element> refine(ArrayList<Node> nodes, ArrayList<Element> elements) {
		ArrayList<Element> newElements = new ArrayList<Element>();
		double x, y, z;
		int[] edgeNodes = new int[3];
		for (int i = 0; i < 3; i++) {
			x = 0.5 * (nodes.get(elementNodes[i]).getXCoord() + nodes.get(elementNodes[(i+1)%3]).getXCoord());
			y = 0.5 * (nodes.get(elementNodes[i]).getYCoord() + nodes.get(elementNodes[(i+1)%3]).getYCoord());
			z = 0.5 * (nodes.get(elementNodes[i]).getZCoord() + nodes.get(elementNodes[(i+1)%3]).getZCoord());
			nodes.add(new Node(x, y, z));
			edgeNodes[i] = nodes.size()-1;
		}
		for (int i = 0; i < 3; i++) {
			int[] newElementNodes = new int[3];
			newElementNodes[i] = elementNodes[i];
			newElementNodes[(i+1)%3] = edgeNodes[i];
			newElementNodes[(i+2)%3] = edgeNodes[(i+2)%3];
			Tri newElement = new Tri(newElementNodes);
			newElement.setMaterial(this.getMaterial());
			//newElement.setState(this.getState());
			newElement.setThickness(this.getThickness());
			newElement.setStiffnessDamping(this.getStiffnessDamping());
			newElement.setMassDamping(this.getMassDamping());
			elements.add(elements.indexOf(this)+1, newElement);
			newElements.add(newElement);
		}
		elementNodes[0] = edgeNodes[1];
		elementNodes[1] = edgeNodes[2];
		elementNodes[2] = edgeNodes[0];
		
		return newElements;
	}
	
	@Override
	public String[] getLocalDofNames() {
		String[] dofNames = new String[18];
		for (int counter = 0, n = 0; n < 3; n++) {
			dofNames[counter] = "u"+(n+1);
			counter++;
			dofNames[counter] = "v"+(n+1);
			counter++;
			dofNames[counter] = "w"+(n+1);
			counter++;
			dofNames[counter] = "\u03b8x"+(n+1);
			counter++;
			dofNames[counter] = "\u03b8y"+(n+1);
			counter++;
			dofNames[counter] = "\u03b8z"+(n+1);
			counter++;
		}
		return dofNames;
	}
	
	public Element clone(Model model) {
		Tri tri = new Tri();
		tri.elementNodes = this.elementNodes.clone();
		tri.material = findMaterial(model.getMaterials(), material);
		tri.thickness = this.thickness;
		//tri.state = this.state;
		tri.R0 = this.R0.copy();
		tri.id = this.id;
		tri.stiffnessDamping = this.stiffnessDamping;
		tri.massDamping = this.massDamping;
		return tri;
	}

	@Override
	public boolean deepEquals(Object obj) {
		if (!(obj instanceof Tri)) return false;
		Tri element = (Tri) obj;
		if (this.getType() != element.getType()) return false;
		if (!Arrays.equals(this.elementNodes, element.elementNodes)) return false;
		if (this.material != null && element.material != null)
			if (!this.material.deepEquals(element.material)) return false;
		if (this.thickness != element.thickness) return false;
		//if (this.state != element.state) return false;
		if (!Arrays.equals(this.R0.getRowPackedCopy(), element.R0.getRowPackedCopy())) return false;
		if (this.id != element.id) return false;
		if (this.stiffnessDamping != element.stiffnessDamping) return false;
		if (this.massDamping != element.massDamping) return false;
		return true;
	}

	public double[] getShapeFunctionValues(double r, double s) {
		//only membrane part
		double[] h = new double[3];
		h[0] = 1.0-r-s;
		h[1] = r;
		h[2] = s;
		return h;
	}

}

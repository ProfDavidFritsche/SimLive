package simlive.model;
import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import simlive.SimLive;
import simlive.misc.Units;
import simlive.solution.Solution;

public class Quad extends PlaneElement {
	
	private Quad() {
		super();
	}
	
	public Quad(int[] element_node) {
		super();
		this.elementNodes = element_node;
		Quad quad = new Quad();
		quad.setThickness(1.0);
		Units.convertUnitsOfElement(Units.UnitSystem.t_mm_s_N, SimLive.settings.unitSystem, quad);
		this.thickness = quad.getThickness();
		//this.state = PlaneElement.State.PLANE_STRESS;
		ArrayList<Node> nodes = SimLive.model.getNodes();
		nodes.get(element_node[0]).setRotationalDOF(true);
		nodes.get(element_node[1]).setRotationalDOF(true);
		nodes.get(element_node[2]).setRotationalDOF(true);
		nodes.get(element_node[3]).setRotationalDOF(true);
	}
	
	@Override
	public double[] getLocalCoords(int localNodeID) {
		double[] localCoords = new double[2];
		switch (localNodeID) {
			case 0:	localCoords[0] = -1.0;
					localCoords[1] = -1.0;
					break;
			case 1:	localCoords[0] = 1.0;
					localCoords[1] = -1.0;
					break;
			case 2:	localCoords[0] = 1.0;
					localCoords[1] = 1.0;
					break;
			case 3:	localCoords[0] = -1.0;
					localCoords[1] = 1.0;
					break;
		}
		return localCoords;
	}
	
	@Override
	public Type getType() {
		return Element.Type.QUAD;
	}

	@Override
	public String getTypeString() {
		return "Quad";
	}
	
	@Override
	protected Matrix getJacobian(double[] x, double[] y, double r, double s) {
		Matrix J = new Matrix(2, 2);
		J.set(0, 0, x[1]-x[0]+x[2]-x[3]+s*(x[0]-x[1]+x[2]-x[3]));
		J.set(0, 1, y[1]-y[0]+y[2]-y[3]+s*(y[0]-y[1]+y[2]-y[3]));
		J.set(1, 0, x[2]-x[1]+x[3]-x[0]+r*(x[0]-x[1]+x[2]-x[3]));
		J.set(1, 1, y[2]-y[1]+y[3]-y[0]+r*(y[0]-y[1]+y[2]-y[3]));
		return J.times(0.25);
	}
	
	private Matrix getShapeFunctionDerivativesPlate(double r, double s) {
		Matrix N = new Matrix(2, 8);
		N.set(0, 0, 0.25*(2.0*r+s)*(1.0-s));
		N.set(0, 1, 0.25*(2.0*r-s)*(1.0-s));
		N.set(0, 2, 0.25*(2.0*r+s)*(1.0+s));
		N.set(0, 3, 0.25*(2.0*r-s)*(1.0+s));
		N.set(0, 4, -r*(1.0-s));
		N.set(0, 5, 0.5*(1.0-s*s));
		N.set(0, 6, -r*(1.0+s));
		N.set(0, 7, -0.5*(1.0-s*s));
		N.set(1, 0, 0.25*(2.0*s+r)*(1.0-r));
		N.set(1, 1, 0.25*(2.0*s-r)*(1.0+r));
		N.set(1, 2, 0.25*(2.0*s+r)*(1.0+r));
		N.set(1, 3, 0.25*(2.0*s-r)*(1.0-r));
		N.set(1, 4, -0.5*(1.0-r*r));
		N.set(1, 5, -s*(1.0+r));
		N.set(1, 6, 0.5*(1.0-r*r));
		N.set(1, 7, -s*(1.0-r));
		return N;
	}
	
	private Matrix getShapeFunctionDerivativesMembrane(Matrix Jinv, double r, double s) {
		Matrix N = new Matrix(2, 4);
		N.set(0, 0, -0.25*(1-s)*Jinv.get(0, 0)-0.25*(1-r)*Jinv.get(0, 1));
		N.set(0, 1,  0.25*(1-s)*Jinv.get(0, 0)-0.25*(1+r)*Jinv.get(0, 1));
		N.set(0, 2,  0.25*(1+s)*Jinv.get(0, 0)+0.25*(1+r)*Jinv.get(0, 1));
		N.set(0, 3, -0.25*(1+s)*Jinv.get(0, 0)+0.25*(1-r)*Jinv.get(0, 1));
		N.set(1, 0, -0.25*(1-s)*Jinv.get(1, 0)-0.25*(1-r)*Jinv.get(1, 1));
		N.set(1, 1,  0.25*(1-s)*Jinv.get(1, 0)-0.25*(1+r)*Jinv.get(1, 1));
		N.set(1, 2,  0.25*(1+s)*Jinv.get(1, 0)+0.25*(1+r)*Jinv.get(1, 1));
		N.set(1, 3, -0.25*(1+s)*Jinv.get(1, 0)+0.25*(1-r)*Jinv.get(1, 1));
		return N;
	}
	
	@Override
	protected Matrix getBMatrixPlate(double[][] x, Matrix Jinv, double r, double s) {
		Matrix N = getShapeFunctionDerivativesPlate(r, s);
		double[][] f = getFactors(x);
		Matrix hxr = new Matrix(1, 12);		
		hxr.set(0, 0,     3.0/2.0*(f[0][0]*N.get(0, 4)-f[0][3]*N.get(0, 7)));
		hxr.set(0, 1,              f[1][0]*N.get(0, 4)+f[1][3]*N.get(0, 7));
		hxr.set(0, 2,  N.get(0, 0)-f[2][0]*N.get(0, 4)-f[2][3]*N.get(0, 7));		
		hxr.set(0, 3,     3.0/2.0*(f[0][1]*N.get(0, 5)-f[0][0]*N.get(0, 4)));
		hxr.set(0, 4,              f[1][1]*N.get(0, 5)+f[1][0]*N.get(0, 4));
		hxr.set(0, 5,  N.get(0, 1)-f[2][1]*N.get(0, 5)-f[2][0]*N.get(0, 4));		
		hxr.set(0, 6,     3.0/2.0*(f[0][2]*N.get(0, 6)-f[0][1]*N.get(0, 5)));
		hxr.set(0, 7,              f[1][2]*N.get(0, 6)+f[1][1]*N.get(0, 5));
		hxr.set(0, 8,  N.get(0, 2)-f[2][2]*N.get(0, 6)-f[2][1]*N.get(0, 5));		
		hxr.set(0, 9,     3.0/2.0*(f[0][3]*N.get(0, 7)-f[0][2]*N.get(0, 6)));
		hxr.set(0, 10,             f[1][3]*N.get(0, 7)+f[1][2]*N.get(0, 6));
		hxr.set(0, 11, N.get(0, 3)-f[2][3]*N.get(0, 7)-f[2][2]*N.get(0, 6));
		Matrix hxs = new Matrix(1, 12);		
		hxs.set(0, 0,     3.0/2.0*(f[0][0]*N.get(1, 4)-f[0][3]*N.get(1, 7)));
		hxs.set(0, 1,              f[1][0]*N.get(1, 4)+f[1][3]*N.get(1, 7));
		hxs.set(0, 2,  N.get(1, 0)-f[2][0]*N.get(1, 4)-f[2][3]*N.get(1, 7));		
		hxs.set(0, 3,     3.0/2.0*(f[0][1]*N.get(1, 5)-f[0][0]*N.get(1, 4)));
		hxs.set(0, 4,              f[1][1]*N.get(1, 5)+f[1][0]*N.get(1, 4));
		hxs.set(0, 5,  N.get(1, 1)-f[2][1]*N.get(1, 5)-f[2][0]*N.get(1, 4));		
		hxs.set(0, 6,     3.0/2.0*(f[0][2]*N.get(1, 6)-f[0][1]*N.get(1, 5)));
		hxs.set(0, 7,              f[1][2]*N.get(1, 6)+f[1][1]*N.get(1, 5));
		hxs.set(0, 8,  N.get(1, 2)-f[2][2]*N.get(1, 6)-f[2][1]*N.get(1, 5));		
		hxs.set(0, 9,     3.0/2.0*(f[0][3]*N.get(1, 7)-f[0][2]*N.get(1, 6)));
		hxs.set(0, 10,             f[1][3]*N.get(1, 7)+f[1][2]*N.get(1, 6));
		hxs.set(0, 11, N.get(1, 3)-f[2][3]*N.get(1, 7)-f[2][2]*N.get(1, 6));		
		Matrix hyr = new Matrix(1, 12);		
		hyr.set(0, 0,     3.0/2.0*(f[3][0]*N.get(0, 4)-f[3][3]*N.get(0, 7)));
		hyr.set(0, 1, -N.get(0, 0)+f[4][0]*N.get(0, 4)+f[4][3]*N.get(0, 7));		
		hyr.set(0, 2,             -f[1][0]*N.get(0, 4)-f[1][3]*N.get(0, 7));
		hyr.set(0, 3,     3.0/2.0*(f[3][1]*N.get(0, 5)-f[3][0]*N.get(0, 4)));
		hyr.set(0, 4, -N.get(0, 1)+f[4][1]*N.get(0, 5)+f[4][0]*N.get(0, 4));		
		hyr.set(0, 5,             -f[1][1]*N.get(0, 5)-f[1][0]*N.get(0, 4));
		hyr.set(0, 6,     3.0/2.0*(f[3][2]*N.get(0, 6)-f[3][1]*N.get(0, 5)));
		hyr.set(0, 7, -N.get(0, 2)+f[4][2]*N.get(0, 6)+f[4][1]*N.get(0, 5));		
		hyr.set(0, 8,             -f[1][2]*N.get(0, 6)-f[1][1]*N.get(0, 5));
		hyr.set(0, 9,     3.0/2.0*(f[3][3]*N.get(0, 7)-f[3][2]*N.get(0, 6)));
		hyr.set(0, 10,-N.get(0, 3)+f[4][3]*N.get(0, 7)+f[4][2]*N.get(0, 6));		
		hyr.set(0, 11,            -f[1][3]*N.get(0, 7)-f[1][2]*N.get(0, 6));
		Matrix hys = new Matrix(1, 12);		
		hys.set(0, 0,     3.0/2.0*(f[3][0]*N.get(1, 4)-f[3][3]*N.get(1, 7)));
		hys.set(0, 1, -N.get(1, 0)+f[4][0]*N.get(1, 4)+f[4][3]*N.get(1, 7));		
		hys.set(0, 2,             -f[1][0]*N.get(1, 4)-f[1][3]*N.get(1, 7));
		hys.set(0, 3,     3.0/2.0*(f[3][1]*N.get(1, 5)-f[3][0]*N.get(1, 4)));
		hys.set(0, 4, -N.get(1, 1)+f[4][1]*N.get(1, 5)+f[4][0]*N.get(1, 4));		
		hys.set(0, 5,             -f[1][1]*N.get(1, 5)-f[1][0]*N.get(1, 4));
		hys.set(0, 6,     3.0/2.0*(f[3][2]*N.get(1, 6)-f[3][1]*N.get(1, 5)));
		hys.set(0, 7, -N.get(1, 2)+f[4][2]*N.get(1, 6)+f[4][1]*N.get(1, 5));		
		hys.set(0, 8,             -f[1][2]*N.get(1, 6)-f[1][1]*N.get(1, 5));
		hys.set(0, 9,     3.0/2.0*(f[3][3]*N.get(1, 7)-f[3][2]*N.get(1, 6)));
		hys.set(0, 10,-N.get(1, 3)+f[4][3]*N.get(1, 7)+f[4][2]*N.get(1, 6));		
		hys.set(0, 11,            -f[1][3]*N.get(1, 7)-f[1][2]*N.get(1, 6));
		
		Matrix B = new Matrix(3, 12);
		B.setMatrix(0, 0, 0, 11, hxr.times(Jinv.get(0, 0)).plus(hxs.times(Jinv.get(0, 1))));
		B.setMatrix(1, 1, 0, 11, hyr.times(Jinv.get(1, 0)).plus(hys.times(Jinv.get(1, 1))));
		B.setMatrix(2, 2, 0, 11, hyr.times(Jinv.get(0, 0)).plus(hys.times(Jinv.get(0, 1))).
				plus(hxr.times(Jinv.get(1, 0)).plus(hxs.times(Jinv.get(1, 1)))));
		return B;
	}
	
	@Override
	protected Matrix getBMatrixMembrane(Matrix Jinv, double r, double s) {
		Matrix N = getShapeFunctionDerivativesMembrane(Jinv, r, s);
		Matrix B = new Matrix(3, 8);
		B.set(0, 0, N.get(0, 0));
		B.set(0, 2, N.get(0, 1));
		B.set(0, 4, N.get(0, 2));
		B.set(0, 6, N.get(0, 3));
		B.set(1, 1, N.get(1, 0));
		B.set(1, 3, N.get(1, 1));
		B.set(1, 5, N.get(1, 2));
		B.set(1, 7, N.get(1, 3));
		B.set(2, 0, N.get(1, 0));
		B.set(2, 1, N.get(0, 0));
		B.set(2, 2, N.get(1, 1));
		B.set(2, 3, N.get(0, 1));
		B.set(2, 4, N.get(1, 2));
		B.set(2, 5, N.get(0, 2));
		B.set(2, 6, N.get(1, 3));
		B.set(2, 7, N.get(0, 3));
		return B;
	}
	
	/*public Matrix getLocalRotations(double r, double s, Matrix u_elem) {
		Matrix N = new Matrix(getShapeFunctionValues(r, s), 1);
		double[][] x = new double[2][4];
		for (int i = 0; i < 4; i++) {
			Matrix global = new Matrix(Sim2d.model.getNodes().get(elementNodes[i]).getCoords(), 3);
			Matrix local = lambda.transpose().times(global);
			x[0][i] = local.get(0, 0);
			x[1][i] = local.get(1, 0);
		}
		double[][] f = getFactors(x);
		Matrix Hx = new Matrix(1, 12);		
		Hx.set(0, 0,     3.0/2.0*(f[0][0]*N.get(0, 4)-f[0][3]*N.get(0, 7)));
		Hx.set(0, 1,              f[1][0]*N.get(0, 4)+f[1][3]*N.get(0, 7));
		Hx.set(0, 2,  N.get(0, 0)-f[2][0]*N.get(0, 4)-f[2][3]*N.get(0, 7));		
		Hx.set(0, 3,     3.0/2.0*(f[0][1]*N.get(0, 5)-f[0][0]*N.get(0, 4)));
		Hx.set(0, 4,              f[1][1]*N.get(0, 5)+f[1][0]*N.get(0, 4));
		Hx.set(0, 5,  N.get(0, 1)-f[2][1]*N.get(0, 5)-f[2][0]*N.get(0, 4));		
		Hx.set(0, 6,     3.0/2.0*(f[0][2]*N.get(0, 6)-f[0][1]*N.get(0, 5)));
		Hx.set(0, 7,              f[1][2]*N.get(0, 6)+f[1][1]*N.get(0, 5));
		Hx.set(0, 8,  N.get(0, 2)-f[2][2]*N.get(0, 6)-f[2][1]*N.get(0, 5));		
		Hx.set(0, 9,     3.0/2.0*(f[0][3]*N.get(0, 7)-f[0][2]*N.get(0, 6)));
		Hx.set(0, 10,             f[1][3]*N.get(0, 7)+f[1][2]*N.get(0, 6));
		Hx.set(0, 11, N.get(0, 3)-f[2][3]*N.get(0, 7)-f[2][2]*N.get(0, 6));
		Matrix Hy = new Matrix(1, 12);		
		Hy.set(0, 0,     3.0/2.0*(f[3][0]*N.get(0, 4)-f[3][3]*N.get(0, 7)));
		Hy.set(0, 1, -N.get(0, 0)+f[4][0]*N.get(0, 4)+f[4][3]*N.get(0, 7));		
		Hy.set(0, 2,             -f[1][0]*N.get(0, 4)-f[1][3]*N.get(0, 7));
		Hy.set(0, 3,     3.0/2.0*(f[3][1]*N.get(0, 5)-f[3][0]*N.get(0, 4)));
		Hy.set(0, 4, -N.get(0, 1)+f[4][1]*N.get(0, 5)+f[4][0]*N.get(0, 4));		
		Hy.set(0, 5,             -f[1][1]*N.get(0, 5)-f[1][0]*N.get(0, 4));
		Hy.set(0, 6,     3.0/2.0*(f[3][2]*N.get(0, 6)-f[3][1]*N.get(0, 5)));
		Hy.set(0, 7, -N.get(0, 2)+f[4][2]*N.get(0, 6)+f[4][1]*N.get(0, 5));		
		Hy.set(0, 8,             -f[1][2]*N.get(0, 6)-f[1][1]*N.get(0, 5));
		Hy.set(0, 9,     3.0/2.0*(f[3][3]*N.get(0, 7)-f[3][2]*N.get(0, 6)));
		Hy.set(0, 10,-N.get(0, 3)+f[4][3]*N.get(0, 7)+f[4][2]*N.get(0, 6));		
		Hy.set(0, 11,            -f[1][3]*N.get(0, 7)-f[1][2]*N.get(0, 6));
		Matrix u_elem_red = new Matrix(12, 1);
		u_elem_red.setMatrix(0, 2, 0, 0, u_elem.getMatrix(2, 4, 0, 0));
		u_elem_red.setMatrix(3, 5, 0, 0, u_elem.getMatrix(8, 10, 0, 0));
		u_elem_red.setMatrix(6, 8, 0, 0, u_elem.getMatrix(14, 16, 0, 0));
		u_elem_red.setMatrix(9, 11, 0, 0, u_elem.getMatrix(20, 22, 0, 0));
		double betaX = Hx.times(u_elem_red).get(0, 0);
		double betaY = Hy.times(u_elem_red).get(0, 0);
		Matrix beta = new Matrix(new double[]{betaX, betaY, 1}, 3);
		return beta.times(1.0/beta.normF());
	}*/
	
	private double[][] getFactors(double[][] x) {
		double[][] f = new double[5][4];
		for (int i = 0; i < 4; i++) {
			double xij = x[0][i]-x[0][(i+1)%4];
			double yij = x[1][i]-x[1][(i+1)%4];
			double lij = xij*xij+yij*yij;
			f[0][i] = -xij/lij;
			f[1][i] = 3.0/4.0*xij*yij/lij;
			f[2][i] = (0.25*xij*xij-0.5*yij*yij)/lij;
			f[3][i] = -yij/lij;
			f[4][i] = (0.25*yij*yij-0.5*xij*xij)/lij;
		}
		return f;
	}
	
	@Override
	protected Matrix getKelem(ArrayList<Material> materials, ArrayList<Section> sections,
			  ArrayList<Node> nodes) {
		Matrix K_elem_plate = new Matrix(12, 12);
		Matrix K_elem_membrane = new Matrix(8, 8);
		
		double t = getThickness();		
		Matrix C = getMaterialMatrix(materials);
		Matrix Cp = C.times(t*t/12.0);
		
		double[][] x = new double[2][4];
		for (int i = 0; i < 4; i++) {
			Matrix global = new Matrix(nodes.get(elementNodes[i]).getCoords(), 3);
			Matrix local = R0.transpose().times(global);
			x[0][i] = local.get(0, 0);
			x[1][i] = local.get(1, 0);
		}
		
		for (double r = -Math.sqrt(1.0/3.0); r < 1.0; r+=2.0*Math.sqrt(1.0/3.0)) {
			for (double s = -Math.sqrt(1.0/3.0); s < 1.0; s+=2.0*Math.sqrt(1.0/3.0)) {
				Matrix J = getJacobian(x[0], x[1], r, s);
				double Jdet = J.det();
				Matrix Jinv = J.inverse();
				Matrix Bp = getBMatrixPlate(x, Jinv, r, s);
				K_elem_plate.plusEquals(Bp.transpose().times(Cp).times(Bp).times(t*Jdet));
				Matrix Bm = getBMatrixMembrane(Jinv, r, s);
				K_elem_membrane.plusEquals(Bm.transpose().times(C).times(Bm).times(t*Jdet));
			}
		}
		
		Matrix K_elem = new Matrix(24, 24);
		for (int n = 0; n < 4; n++) {
			for (int m = 0; m < 4; m++) {
				K_elem.setMatrix(n*6, n*6+1, m*6, m*6+1, K_elem_membrane.getMatrix(n*2, n*2+1, m*2, m*2+1));
				K_elem.setMatrix(n*6+2, n*6+4, m*6+2, m*6+4, K_elem_plate.getMatrix(n*3, n*3+2, m*3, m*3+2));
			}
		}
		
		for (int n = 0; n < 4; n++) {
			double maxDiag = Math.max(K_elem.get(n*6+3, n*6+3), K_elem.get(n*6+4, n*6+4));
			K_elem.set(n*6+5, n*6+5, 10e-3*maxDiag);
		}
		
		return K_elem;
	}
	
	private Matrix getFLocal(ArrayList<Node> nodes, Matrix u_elem, Matrix RrT) {
		
		Matrix temp = new Matrix(24, 1);
		Matrix r0 = new Matrix(nodes.get(elementNodes[0]).getCoords(), 3);
		Matrix r0u = r0.plus(u_elem.getMatrix(0, 2, 0, 0));
		for (int n = 0; n < 4; n++) {
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
	public Matrix getElementStiffness(ArrayList<Material> materials, ArrayList<Section> sections,
									  ArrayList<Node> nodes) {
		
		Matrix T = getTransformation();
		
		return T.transpose().times(K_elem).times(T);
	}
	
	private Matrix getTransformation() {
		Matrix R0T = R0.transpose();
		Matrix T = new Matrix(24, 24);
		T.setMatrix(0, 2, 0, 2, R0T);
		T.setMatrix(3, 5, 3, 5, R0T);
		T.setMatrix(6, 8, 6, 8, R0T);
		T.setMatrix(9, 11, 9, 11, R0T);
		T.setMatrix(12, 14, 12, 14, R0T);
		T.setMatrix(15, 17, 15, 17, R0T);
		T.setMatrix(18, 20, 18, 20, R0T);
		T.setMatrix(21, 23, 21, 23, R0T);
		return T;
	}
	
	private Matrix getE(Matrix Rr) {
		Matrix E = new Matrix(24, 24);
		E.setMatrix(0, 2, 0, 2, Rr);
		E.setMatrix(3, 5, 3, 5, Rr);
		E.setMatrix(6, 8, 6, 8, Rr);
		E.setMatrix(9, 11, 9, 11, Rr);
		E.setMatrix(12, 14, 12, 14, Rr);
		E.setMatrix(15, 17, 15, 17, Rr);
		E.setMatrix(18, 20, 18, 20, Rr);
		E.setMatrix(21, 23, 21, 23, Rr);		
		return E;
	}
	
	private Matrix getP(ArrayList<Node> nodes, Matrix u_elem, Matrix RrT) {
		Matrix r0 = new Matrix(nodes.get(elementNodes[0]).getCoords(), 3);
		r0.plusEquals(u_elem.getMatrix(0, 2, 0, 0));
		Matrix r1 = new Matrix(nodes.get(elementNodes[1]).getCoords(), 3);
		r1.plusEquals(u_elem.getMatrix(6, 8, 0, 0));
		Matrix r2 = new Matrix(nodes.get(elementNodes[2]).getCoords(), 3);
		r2.plusEquals(u_elem.getMatrix(12, 14, 0, 0));
		Matrix r3 = new Matrix(nodes.get(elementNodes[3]).getCoords(), 3);
		r3.plusEquals(u_elem.getMatrix(18, 20, 0, 0));
		Matrix I3 = Matrix.identity(3, 3);
		Matrix A = new Matrix(24, 3);
		//A.setMatrix(0, 2, 0, 2, Beam.getSkewSymmetricMatrix(RrT.times(r0.minus(r0))));
		A.setMatrix(3, 5, 0, 2, I3);
		A.setMatrix(6, 8, 0, 2, Beam.getSkewSymmetricMatrix(RrT.times(r0.minus(r1))));
		A.setMatrix(9, 11, 0, 2, I3);
		A.setMatrix(12, 14, 0, 2, Beam.getSkewSymmetricMatrix(RrT.times(r0.minus(r2))));
		A.setMatrix(15, 17, 0, 2, I3);
		A.setMatrix(18, 20, 0, 2, Beam.getSkewSymmetricMatrix(RrT.times(r0.minus(r3))));
		A.setMatrix(21, 23, 0, 2, I3);
		
		Matrix G = getG(nodes, u_elem, RrT);
		//G.transpose().times(A).print(10, 10); //has to be I3
		return Matrix.identity(24, 24).minus(A.times(G.transpose()));
	}
	
	private Matrix getG(ArrayList<Node> nodes, Matrix u_elem, Matrix RrT) {
		Matrix r0 = new Matrix(nodes.get(elementNodes[0]).getCoords(), 3);
		r0.plusEquals(u_elem.getMatrix(0, 2, 0, 0));
		Matrix r1 = new Matrix(nodes.get(elementNodes[1]).getCoords(), 3);
		r1.plusEquals(u_elem.getMatrix(6, 8, 0, 0));
		Matrix r2 = new Matrix(nodes.get(elementNodes[3]).getCoords(), 3);
		r2.plusEquals(u_elem.getMatrix(18, 20, 0, 0));
		Matrix x1 = RrT.times(r1.minus(r0));
		Matrix x2 = RrT.times(r2.minus(r0));
		Matrix G = new Matrix(24, 3);
		G.set(1, 2, -1.0/x1.get(0, 0));
		G.set(2, 0, (x2.get(0, 0)-x1.get(0, 0))/(x2.get(1, 0)*x1.get(0, 0)));
		G.set(2, 1, 1.0/x1.get(0, 0));
		G.set(7, 2, 1.0/x1.get(0, 0));
		G.set(8, 0, -x2.get(0, 0)/(x2.get(1, 0)*x1.get(0, 0)));
		G.set(8, 1, -1.0/x1.get(0, 0));
		G.set(20, 0, 1.0/x2.get(1, 0));
		return G;
	}
	
	@Override
	public Matrix getElementStiffnessNL(ArrayList<Material> materials, ArrayList<Section> sections,
			ArrayList<Node> nodes, Matrix u_global) {
		
		Matrix u_elem = globalToLocalVector(u_global);
		Matrix Rr = getRr(nodes, u_elem);
		Matrix RrT = Rr.transpose();
		Matrix E = getE(Rr);
		Matrix P = getP(nodes, u_elem, RrT);
		Matrix f_int = /*P.transpose().times(*/getFLocal(nodes, u_elem, RrT)/*)*/;
		Matrix F1 = new Matrix(24, 3);
		Matrix F2 = new Matrix(24, 3);
		for (int i = 0; i < 4; i++) {
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
	public Matrix getElementForce(ArrayList<Material> materials, ArrayList<Section> sections, ArrayList<Node> nodes,
			Matrix u_global, boolean localSys) {
		
		Matrix K_elem = getElementStiffness(materials, sections, nodes);
		
		Matrix u_elem = globalToLocalVector(u_global);
		
		Matrix f_int = K_elem.times(u_elem);
		
		return f_int;
	}

	@Override
	public Matrix getElementForceNL(ArrayList<Material> materials, ArrayList<Section> sections, ArrayList<Node> nodes,
			Matrix u_global, boolean localSys) {
		
		Matrix u_elem = globalToLocalVector(u_global);
		Matrix Rr = getRr(nodes, u_elem);
		Matrix RrT = Rr.transpose();
		
		Matrix f_int = getFLocal(nodes, u_elem, RrT);
		
		Matrix E = getE(Rr);
		Matrix P = getP(nodes, u_elem, RrT);
		
		return E.times(P.transpose().times(f_int));
	}

	@Override
	protected Matrix getMelem(ArrayList<Material> materials, ArrayList<Section> sections,
			  ArrayList<Node> nodes) {

		double rho, t;
		rho = materials.get(material_id).getDensity();
		t = getThickness();
		
		double[] x = new double[4];
		double[] y = new double[4];
		for (int i = 0; i < 4; i++) {
			Matrix global = new Matrix(nodes.get(elementNodes[i]).getCoords(), 3);
			Matrix local = R0.transpose().times(global);
			x[i] = local.get(0, 0);
			y[i] = local.get(1, 0);
		}
		
		Matrix M_elem = new Matrix(24, 24);
		
		{
			// lumped mass matrix
			int n = 0;
			for (double r = -Math.sqrt(1.0/3.0); r < 1.0; r+=2.0*Math.sqrt(1.0/3.0)) {
				for (double s = -Math.sqrt(1.0/3.0); s < 1.0; s+=2.0*Math.sqrt(1.0/3.0)) {
					double Jdet = getJacobian(x, y, r, s).det();
					double mass = rho*Jdet*t;
					M_elem.set(n*6, n*6, mass);
					M_elem.set(n*6+1, n*6+1, mass);
					M_elem.set(n*6+2, n*6+2, mass);
					M_elem.set(n*6+3, n*6+3, mass*Jdet/100.0);
					M_elem.set(n*6+4, n*6+4, mass*Jdet/100.0);
					M_elem.set(n*6+5, n*6+5, mass*Jdet/100.0);
					n++;
				}
			}
		}
		
		/* no need to transform */
		return M_elem;
	}


	@Override
	public void setIndexIncidence(Solution solution, ArrayList<Node> nodes) {

		I = new Matrix(24, 1);
		
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
			if (n == elementNodes[3]) {
				I.set(18, 0, dof);
				I.set(19, 0, dof+1);
				I.set(20, 0, dof+2);
				I.set(21, 0, dof+3);
				I.set(22, 0, dof+4);
				I.set(23, 0, dof+5);
			}
		}
	}
	
	@Override
	public ArrayList<Element> refine(ArrayList<Node> nodes, ArrayList<Element> elements) {
		ArrayList<Element> newElements = new ArrayList<Element>();
		double x, y, z;
		int[] edgeNodes = new int[4];
		for (int i = 0; i < 4; i++) {
			x = 0.5 * (nodes.get(elementNodes[i]).getXCoord() + nodes.get(elementNodes[(i+1)%4]).getXCoord());
			y = 0.5 * (nodes.get(elementNodes[i]).getYCoord() + nodes.get(elementNodes[(i+1)%4]).getYCoord());			
			z = 0.5 * (nodes.get(elementNodes[i]).getZCoord() + nodes.get(elementNodes[(i+1)%4]).getZCoord());
			nodes.add(new Node(x, y, z));
			edgeNodes[i] = nodes.size()-1;
		}
		nodes.add(new Node(0.25*(nodes.get(elementNodes[0]).getXCoord()+nodes.get(elementNodes[1]).getXCoord()+
								 nodes.get(elementNodes[2]).getXCoord()+nodes.get(elementNodes[3]).getXCoord()),
						   0.25*(nodes.get(elementNodes[0]).getYCoord()+nodes.get(elementNodes[1]).getYCoord()+
								 nodes.get(elementNodes[2]).getYCoord()+nodes.get(elementNodes[3]).getYCoord()),
						   0.25*(nodes.get(elementNodes[0]).getZCoord()+nodes.get(elementNodes[1]).getZCoord()+
								 nodes.get(elementNodes[2]).getZCoord()+nodes.get(elementNodes[3]).getZCoord())));
		int middleNode = nodes.size()-1;
		for (int i = 3; i > -1; i--) {
			int[] newElementNodes = new int[4];
			newElementNodes[i] = elementNodes[i];
			newElementNodes[(i+1)%4] = edgeNodes[i];
			newElementNodes[(i+2)%4] = middleNode;
			newElementNodes[(i+3)%4] = edgeNodes[(i+3)%4];			
			if (i == 0) {
				this.setElementNodes(newElementNodes);
			}
			else {
				Quad newElement = new Quad(newElementNodes);
				newElement.setMaterialID(this.getMaterialID());
				//newElement.setState(this.getState());
				newElement.setThickness(this.getThickness());
				newElement.setStiffnessDamping(this.getStiffnessDamping());
				newElement.setMassDamping(this.getMassDamping());
				elements.add(elements.indexOf(this)+1, newElement);
				newElements.add(newElement);
			}
		}
		return newElements;
	}
	
	@Override
	public String[] getLocalDofNames() {
		String[] dofNames = new String[24];
		for (int counter = 0, n = 0; n < 4; n++) {
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
	
	public Element clone() {
		Quad quad = new Quad();
		quad.elementNodes = this.elementNodes.clone();
		quad.material_id = this.material_id;
		quad.thickness = this.thickness;
		//quad.state = this.state;
		quad.R0 = this.R0.copy();
		quad.id = this.id;
		quad.stiffnessDamping = this.stiffnessDamping;
		quad.massDamping = this.massDamping;
		return quad;
	}

	@Override
	public boolean deepEquals(Object obj) {
		if (!(obj instanceof Quad)) return false;
		Quad element = (Quad) obj;
		if (this.getType() != element.getType()) return false;
		if (!Arrays.equals(this.elementNodes, element.elementNodes)) return false;
		if (this.material_id != element.material_id) return false;
		if (this.thickness != element.thickness) return false;
		//if (this.state != element.state) return false;
		if (!Arrays.equals(this.R0.getRowPackedCopy(), element.R0.getRowPackedCopy())) return false;
		if (this.id != element.id) return false;
		if (this.stiffnessDamping != element.stiffnessDamping) return false;
		if (this.massDamping != element.massDamping) return false;
		return true;
	}

	@Override
	public double[] getShapeFunctionValues(double r, double s) {
		//only membrane part
		double[] h = new double[4];
		h[0] = 0.25*(1.0-r)*(1.0-s);
		h[1] = 0.25*(1.0+r)*(1.0-s);
		h[2] = 0.25*(1.0+r)*(1.0+s);
		h[3] = 0.25*(1.0-r)*(1.0+s);
		return h;
	}

}

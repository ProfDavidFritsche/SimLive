package simlive.model;
import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import simlive.SimLive;
import simlive.solution.Increment;
import simlive.solution.Solution;

public class Beam extends LineElement {

	private Beam() {
		super();
	}
	
	public Beam(int[] element_node) {
		super(element_node);
		ArrayList<Node> nodes = SimLive.model.getNodes();
		nodes.get(element_node[0]).setRotationalDOF(true);
		nodes.get(element_node[1]).setRotationalDOF(true);
	}
	
	@Override
	public boolean isSectionValid(ArrayList<Section> sections) {
		return sections.contains(section);
	}

	@Override
	public Type getType() {
		return Element.Type.BEAM;
	}

	@Override
	public String getTypeString() {
		return "Beam";
	}
	
	public Matrix getRr(Matrix u_elem, Matrix r1) {
		Matrix R1g = rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0));
		Matrix R2g = rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0));
		Matrix q1 = R1g.times(R0).times(new Matrix(new double[]{0, 1, 0}, 3));
		Matrix q2 = R2g.times(R0).times(new Matrix(new double[]{0, 1, 0}, 3));
		Matrix q = (q1.plus(q2)).times(0.5);
		Matrix r3 = r1.crossProduct(q);
		r3 = r3.times(1.0/r3.normF());
		Matrix r2 = r3.crossProduct(r1);
		Matrix Rr = new Matrix(3, 3);
		Rr.setMatrix(0, 2, 0, 0, r1);
		Rr.setMatrix(0, 2, 1, 1, r2);
		Rr.setMatrix(0, 2, 2, 2, r3);
		
		return Rr;
	}
	
	private Matrix getG(Matrix u_elem, Matrix Rr, double length) {
		Matrix R1g = rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0));
		Matrix R2g = rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0));
		Matrix q1 = R1g.times(R0).times(new Matrix(new double[]{0, 1, 0}, 3));
		Matrix q2 = R2g.times(R0).times(new Matrix(new double[]{0, 1, 0}, 3));
		Matrix q = (q1.plus(q2)).times(0.5);
		Matrix RrTq = Rr.transpose().times(q);
		//Matrix RrTq1 = Rr.transpose().times(q1);
		//Matrix RrTq2 = Rr.transpose().times(q2);
		
		double eta = RrTq.get(0, 0)/RrTq.get(1, 0);
		//double eta11 = RrTq1.get(0, 0)/RrTq.get(1, 0);
		//double eta12 = RrTq1.get(1, 0)/RrTq.get(1, 0);
		//double eta21 = RrTq2.get(0, 0)/RrTq.get(1, 0);
		//double eta22 = RrTq2.get(1, 0)/RrTq.get(1, 0);
		
		/*Matrix G = new Matrix(12, 3);
		G.set(2, 0, eta/length);
		G.set(3, 0, eta12/2.0);
		G.set(4, 0, -eta11/2.0);
		G.set(8, 0, -eta/length);
		G.set(9, 0, eta22/2.0);		
		G.set(10, 0, -eta21/2.0);
		G.set(2, 1, 1.0/length);
		G.set(8, 1, -1.0/length);
		G.set(1, 2, -1.0/length);
		G.set(7, 2, 1.0/length);*/
		
		Matrix G = new Matrix(12, 3);
		G.set(2, 0, eta/length);
		G.set(3, 0, 1.0/2.0);
		G.set(4, 0, -eta/2.0);
		G.set(8, 0, -eta/length);
		G.set(9, 0, 1.0/2.0);		
		G.set(10, 0, -eta/2.0);
		G.set(2, 1, 1.0/length);
		G.set(8, 1, -1.0/length);
		G.set(1, 2, -1.0/length);
		G.set(7, 2, 1.0/length);
		
		return G;
	}
	
	private Matrix getP(Matrix G) {
		Matrix P = new Matrix(6, 12);
		P.set(0, 3, 1);
		P.set(1, 4, 1);
		P.set(2, 5, 1);
		P.set(3, 9, 1);
		P.set(4, 10, 1);
		P.set(5, 11, 1);
		
		Matrix temp = new Matrix(6, 12);
		temp.setMatrix(0, 2, 0, 11, G.transpose());
		temp.setMatrix(3, 5, 0, 11, G.transpose());
		
		return P.minus(temp);
	}
	
	private Matrix getE(Matrix Rr) {
		Matrix E = new Matrix(12, 12);
		E.setMatrix(0, 2, 0, 2, Rr);
		E.setMatrix(3, 5, 3, 5, Rr);
		E.setMatrix(6, 8, 6, 8, Rr);
		E.setMatrix(9, 11, 9, 11, Rr);
		
		return E;
	}
	
	private Matrix getr(Matrix r1) {
		Matrix r = new Matrix(1, 12);
		r.setMatrix(0, 0, 0, 2, r1.transpose().times(-1.0));
		r.setMatrix(0, 0, 6, 8, r1.transpose());
		
		return r;
	}
	
	private Matrix getD(Matrix r1, double length) {
		Matrix D3 = (Matrix.identity(3, 3).minus(r1.times(r1.transpose()))).times(1.0/length);
		Matrix D = new Matrix(12, 12);
		D.setMatrix(0, 2, 0, 2, D3);
		D.setMatrix(0, 2, 6, 8, D3.times(-1.0));
		D.setMatrix(6, 8, 0, 2, D3.times(-1.0));
		D.setMatrix(6, 8, 6, 8, D3);
		
		return D;
	}
	
	public static Matrix getTInv(Matrix Psi) {
		double psi = Psi.normF();
		if (psi > 0.0) {
			double f1 = (psi/2.0)/Math.tan(psi/2.0);
			Matrix u = Psi.times(1.0/psi);
			return Matrix.identity(3, 3).times(f1).plus(u.times(u.transpose()).times(1.0-f1)).minus(getSkewSymmetricMatrix(Psi).times(0.5));
		}
		else {
			return Matrix.identity(3, 3);
		}
	}
	
	public static Matrix getTDot(Matrix Psi, Matrix PsiDot) {
		double psi = Psi.normF();
		if (psi > 0.0) {
			double f1 = Math.cos(psi)/psi;
			double f2 = Math.sin(psi)/(psi*psi);
			double f3 = Math.sin(psi/2.0)/(psi/2.0);
			double a1 = f1-f2;
			double a2 = 1.0/psi-f2;
			double a3 = 3.0*f2-f1-2.0/psi;
			double a4 = 1.0/psi*f3*f3-f2;
			double a5 = -0.5*f3*f3;
			Matrix e = Psi.times(1.0/psi);
			double dotProduct = PsiDot.dotProduct(e);
			return Matrix.identity(3, 3).times(dotProduct*a1).plus(
					PsiDot.times(e.transpose()).plus(e.times(PsiDot.transpose())).times(a2)).plus(
					e.times(e.transpose()).times(dotProduct*a3)).plus(
					getSkewSymmetricMatrix(Psi).times(dotProduct*a4)).plus(
					getSkewSymmetricMatrix(PsiDot).times(a5));
		}
		else {
			return getSkewSymmetricMatrix(PsiDot).times(-0.5);
		}
	}
	
	private Matrix getBMatrix(Matrix u_elem, Matrix Rr, Matrix r1, double length) {
		Matrix G = getG(u_elem, Rr, length);		
		Matrix P = getP(G);
		Matrix E = getE(Rr);
		Matrix r = getr(r1);
		
		Matrix B = new Matrix(7, 12);
		B.setMatrix(0, 0, 0, 11, r);
		B.setMatrix(1, 6, 0, 11, P.times(E.transpose()));
		
		return B;
	}
	
	@Override
	public Matrix getElementStiffness(ArrayList<Node> nodes) {
		
		Matrix K_elem = new Matrix(12, 12);
		
		double E, A, Iz, Iy, G, It, l;
		E = material.getYoungsModulus();
		A = section.getArea();
		Iy = section.getIy();
		Iz = section.getIz();
		It = section.getIt();
		G = E/(2.0*(1.0+material.getPoissonsRatio()));
		l = getLength();
		
		K_elem.set(0, 0, E*A/l);
		K_elem.set(0, 6, -E*A/l);
		K_elem.set(1, 1, 12*E*Iz/(l*l*l));
		K_elem.set(1, 5, 6*E*Iz/(l*l));
		K_elem.set(1, 7, -12*E*Iz/(l*l*l));
		K_elem.set(1, 11, 6*E*Iz/(l*l));		
		K_elem.set(2, 2, 12*E*Iy/(l*l*l));
		K_elem.set(2, 4, -6*E*Iy/(l*l));
		K_elem.set(2, 8, -12*E*Iy/(l*l*l));
		K_elem.set(2, 10, -6*E*Iy/(l*l));
		K_elem.set(3, 3, G*It/l);
		K_elem.set(3, 9, -G*It/l);
		K_elem.set(4, 2, -6*E*Iy/(l*l));
		K_elem.set(4, 4, 4*E*Iy/l);
		K_elem.set(4, 8, 6*E*Iy/(l*l));
		K_elem.set(4, 10, 2*E*Iy/l);
		K_elem.set(5, 1, 6*E*Iz/(l*l));
		K_elem.set(5, 5, 4*E*Iz/l);
		K_elem.set(5, 7, -6*E*Iz/(l*l));
		K_elem.set(5, 11, 2*E*Iz/l);
		K_elem.set(6, 0, -E*A/l);
		K_elem.set(6, 6, E*A/l);
		K_elem.set(7, 1, -12*E*Iz/(l*l*l));
		K_elem.set(7, 5, -6*E*Iz/(l*l));
		K_elem.set(7, 7, 12*E*Iz/(l*l*l));
		K_elem.set(7, 11, -6*E*Iz/(l*l));
		K_elem.set(8, 2, -12*E*Iy/(l*l*l));
		K_elem.set(8, 4, 6*E*Iy/(l*l));
		K_elem.set(8, 8, 12*E*Iy/(l*l*l));
		K_elem.set(8, 10, 6*E*Iy/(l*l));
		K_elem.set(9, 3, -G*It/l);
		K_elem.set(9, 9, G*It/l);		
		K_elem.set(10, 2, -6*E*Iy/(l*l));
		K_elem.set(10, 4, 2*E*Iy/l);
		K_elem.set(10, 8, 6*E*Iy/(l*l));
		K_elem.set(10, 10, 4*E*Iy/l);
		K_elem.set(11, 1, 6*E*Iz/(l*l));
		K_elem.set(11, 5, 2*E*Iz/l);
		K_elem.set(11, 7, -6*E*Iz/(l*l));
		K_elem.set(11, 11, 4*E*Iz/l);
		
		Matrix T = getTransformation();
		
		return T.transpose().times(K_elem).times(T);
	}

	@Override
	public Matrix getElementStiffnessNL(ArrayList<Node> nodes, Matrix u_global) {
		
		//Large displacement, small strain formulation
		//Co-rotational formulation, euler-bernoulli beam
		Matrix u_elem = globalToLocalVector(u_global);
		
		double length = getCurrentLength(nodes, u_elem);
		double length0 = getLength();
		Matrix r1 = getr1(nodes, u_elem, length);
		Matrix Rr = getRr(u_elem, r1);
		
		Matrix B = getBMatrix(u_elem, Rr, r1, length);
		
		Matrix R_local = Rr.transpose().times(rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0))).times(R0);
		double[] angles0 = anglesFromRotationMatrix(R_local);
		R_local = Rr.transpose().times(rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0))).times(R0);
		double[] angles1 = anglesFromRotationMatrix(R_local);
		
		Matrix K_elem = getKelem(length-length0, angles0, angles1, length0);
		
		Matrix f_local = getFLocal(length-length0, angles0, angles1, length0);
		
		Matrix K_geo = getKgeo(u_elem, f_local, Rr, r1, length);
		
		return B.transpose().times(K_elem).times(B).plus(K_geo);
	}
	
	private Matrix getKelem(double u, double[] a0, double[] a1, double L0) {
		double E, A, Iy, Iz, Io, G, It;
		E = material.getYoungsModulus();
		A = section.getArea();
		Iy = section.getIy();
		Iz = section.getIz();
		Io = Iy+Iz;
		It = section.getIt();
		G = E/(2.0*(1.0+material.getPoissonsRatio()));
		//Rechteck: Irr = 1/80*h^5*w + 1/72*h^3*w^3 + 1/80*h*w^5
		//Kreis: Irr = d^6*pi/192
		
		double delta_a = a0[0] - a1[0];
		
		Matrix K_elem = new Matrix(7, 7);
		K_elem.set(0, 0, E*A/L0);
		K_elem.set(1, 1, 1.0/30.0*(((2.0*a0[1]*a0[1] - a0[1]*a1[1] + 2.0*a1[1]*a1[1] + 2.0*a0[2]*a0[2] - a0[2]*a1[2] + 2.0*a1[2]*a1[2])*Io*L0*L0 + 30.0*Io*L0*u)*E + 30.0*G*It*L0*L0)/(L0*L0*L0));
		K_elem.set(1, 2, 1.0/30.0*E*Io*delta_a*(4.0*a0[1] - a1[1])/L0);
		K_elem.set(1, 3, 1.0/30.0*E*Io*delta_a*(4.0*a0[2] - a1[2])/L0);
		K_elem.set(1, 4, -K_elem.get(1, 1));
		K_elem.set(1, 5, -1.0/30.0*E*Io*delta_a*(a0[1] - 4.0*a1[1])/L0);
		K_elem.set(1, 6, -1.0/30.0*E*Io*delta_a*(a0[2] - 4.0*a1[2])/L0);
		
		K_elem.set(2, 1, K_elem.get(1, 2));
		K_elem.set(2, 2, 2.0/225.0*E*(A*(a0[2]*a0[2] - 1.0/2.0*a0[2]*a1[2] + a1[2]*a1[2] + 3.0*a0[1]*a0[1] - 3.0/2.0*a0[1]*a1[1] + 9.0/8.0*a1[1]*a1[1])*L0*L0 + 15.0*u*L0*A + 15.0/2.0*Io*delta_a*delta_a + 450.0*Iy)/L0);
		K_elem.set(2, 3, 1.0/900.0*E*A*L0*(4.0*a0[1] - a1[1])*(4.0*a0[2] - a1[2]));
		K_elem.set(2, 4, -1.0/15.0*(2.0*a0[1] - 1.0/2.0*a1[1])*E*delta_a*Io/L0);
		K_elem.set(2, 5, -1.0/450.0*E*(A*(a0[2]*a0[2] - 1.0/2.0*a0[2]*a1[2] + a1[2]*a1[2] + 3.0*a0[1]*a0[1] - 9.0*a0[1]*a1[1] + 3.0*a1[1]*a1[1])*L0*L0 + 15.0*u*L0*A + 15.0/2.0*Io*delta_a*delta_a - 900.0*Iy)/L0);
		K_elem.set(2, 6, -1.0/225.0*E*A*L0*(a0[2] - 4.0*a1[2])*(a0[1] - 1.0/4.0*a1[1]));
		
		K_elem.set(3, 1, K_elem.get(1, 3));
		K_elem.set(3, 2, K_elem.get(2, 3));
		K_elem.set(3, 3, 2.0/75.0*E*(A*(a0[2]*a0[2] - 1.0/2.0*a0[2]*a1[2] + 3.0/8.0*a1[2]*a1[2] + 1.0/3.0*a0[1]*a0[1] - 1.0/6.0*a0[1]*a1[1] + 1.0/3.0*a1[1]*a1[1])*L0*L0 + 5.0*u*L0*A + 5.0/2.0*Io*delta_a*delta_a + 150.0*Iz)/L0);
		K_elem.set(3, 4, -1.0/15.0*(2.0*a0[2] - 1.0/2.0*a1[2])*E*delta_a*Io/L0);
		K_elem.set(3, 5, -1.0/225.0*(a0[2] - 1.0/4.0*a1[2])*E*(a0[1] - 4.0*a1[1])*A*L0);
		K_elem.set(3, 6, -1.0/150.0*E*(A*(a0[2]*a0[2] - 3.0*a0[2]*a1[2] + a1[2]*a1[2] + 1.0/3.0*a0[1]*a0[1] - 1.0/6.0*a0[1]*a1[1] + 1.0/3.0*a1[1]*a1[1])*L0*L0 + 5.0*u*L0*A + 5.0/2.0*Io*delta_a*delta_a - 300.0*Iz)/L0);
		
		K_elem.set(4, 1, K_elem.get(1, 4));
		K_elem.set(4, 2, K_elem.get(2, 4));
		K_elem.set(4, 3, K_elem.get(3, 4));
		K_elem.set(4, 4, K_elem.get(1, 1));
		K_elem.set(4, 5, -K_elem.get(1, 5));
		K_elem.set(4, 6, -K_elem.get(1, 6));
		
		K_elem.set(5, 1, K_elem.get(1, 5));
		K_elem.set(5, 2, K_elem.get(2, 5));
		K_elem.set(5, 3, K_elem.get(3, 5));
		K_elem.set(5, 4, K_elem.get(4, 5));
		K_elem.set(5, 5, 2.0/225.0*E*(A*(a0[2]*a0[2] - 1.0/2.0*a0[2]*a1[2] + a1[2]*a1[2] + 9.0/8.0*a0[1]*a0[1] - 3.0/2.0*a0[1]*a1[1] + 3.0*a1[1]*a1[1])*L0*L0 + 15.0*u*L0*A + 15.0/2.0*Io*delta_a*delta_a + 450.0*Iy)/L0);
		K_elem.set(5, 6, 1.0/900.0*E*A*L0*(a0[1] - 4.0*a1[1])*(a0[2] - 4.0*a1[2]));
		
		K_elem.set(6, 1, K_elem.get(1, 6));
		K_elem.set(6, 2, K_elem.get(2, 6));
		K_elem.set(6, 3, K_elem.get(3, 6));
		K_elem.set(6, 4, K_elem.get(4, 6));
		K_elem.set(6, 5, K_elem.get(5, 6));
		K_elem.set(6, 6, 1.0/100.0*(A*(a0[2]*a0[2] - 4.0/3.0*a0[2]*a1[2] + 8.0/3.0*a1[2]*a1[2] + 8.0/9.0*a0[1]*a0[1] - 4.0/9.0*a0[1]*a1[1] + 8.0/9.0*a1[1]*a1[1])*L0*L0 + 40.0/3.0*u*L0*A + 20.0/3.0*Io*delta_a*delta_a + 400.0*Iz)*E/L0);
		
		return K_elem;
	}
	
	private Matrix getKgeo(Matrix u_elem, Matrix f_local, Matrix Rr, Matrix r1, double length) {
		Matrix D = getD(r1, length);
		Matrix G = getG(u_elem, Rr, length);
		Matrix E = getE(Rr);
		Matrix r = getr(r1);
		Matrix P = getP(G);
		
		Matrix R1g = rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0));
		Matrix R2g = rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0));
		Matrix q1 = R1g.times(R0).times(new Matrix(new double[]{0, 1, 0}, 3));
		Matrix q2 = R2g.times(R0).times(new Matrix(new double[]{0, 1, 0}, 3));
		Matrix q = (q1.plus(q2)).times(0.5);
		Matrix RrTq = Rr.transpose().times(q);
		double eta = RrTq.get(0, 0)/RrTq.get(1, 0);
		
		Matrix a = new Matrix(3, 1);
		a.set(1, 0, eta/length*(f_local.get(1, 0)+f_local.get(4, 0))-1.0/length*(f_local.get(2, 0)+f_local.get(5, 0)));
		a.set(2, 0, 1.0/length*(f_local.get(3, 0)+f_local.get(6, 0)));		
		
		Matrix m = new Matrix(6, 1);
		m.setMatrix(0, 5, 0, 0, f_local.getMatrix(1, 6, 0, 0));
		Matrix temp = P.transpose().times(m);
		Matrix Q = new Matrix(12, 3);
		Q.setMatrix(0, 2, 0, 2, getSkewSymmetricMatrix(temp.getMatrix(0, 2, 0, 0)));
		Q.setMatrix(3, 5, 0, 2, getSkewSymmetricMatrix(temp.getMatrix(3, 5, 0, 0)));
		Q.setMatrix(6, 8, 0, 2, getSkewSymmetricMatrix(temp.getMatrix(6, 8, 0, 0)));
		Q.setMatrix(9, 11, 0, 2, getSkewSymmetricMatrix(temp.getMatrix(9, 11, 0, 0)));
				
		return D.times(f_local.get(0, 0)).minus(E.times(Q).times(G.transpose()).times(E.transpose())).plus(E.times(G).times(a).times(r));		
	}
	
	static public Matrix getSkewSymmetricMatrix(Matrix a) {
		Matrix A = new Matrix(3, 3);
		A.set(0, 1, -a.get(2, 0));
		A.set(0, 2, a.get(1, 0));
		A.set(1, 0, a.get(2, 0));
		A.set(1, 2, -a.get(0, 0));
		A.set(2, 0, -a.get(1, 0));
		A.set(2, 1, a.get(0, 0));
		return A;
	}

	@Override
	public Matrix getElementForce(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {
				
		Matrix K_elem = getElementStiffness(nodes);
		
		Matrix u_elem = globalToLocalVector(u_global);
		
		Matrix f_int = K_elem.times(u_elem);
		
		if (localSys) {
			Matrix T = getTransformation();
			f_int = T.times(f_int);
			double L0 = getLength();
			f_int.set(1, 0, -(f_int.get(11, 0)+f_int.get(5, 0))/L0);
			f_int.set(2, 0, -(f_int.get(10, 0)+f_int.get(4, 0))/L0);
			f_int.set(7, 0, -f_int.get(1, 0));
			f_int.set(8, 0, -f_int.get(2, 0));
		}
		
		return f_int;
	}
	
	static public Matrix rotationMatrixFromAngles(Matrix rot) {
		Matrix Psi = getSkewSymmetricMatrix(rot);
		double psi = rot.normF();
		double f1 = 0.0, f2 = 0.0;
		if (psi > SimLive.ZERO_TOL) {
			f1 = Math.sin(psi)/psi;
			f2 = (1.0-Math.cos(psi))/(psi*psi);
		}
		return Matrix.identity(3, 3).plus(Psi.times(f1)).plus(Psi.times(Psi).times(f2));
	}
	
	/*static public Matrix rotationMatrixFromAngles3(Matrix rot) {
		Matrix Psi = getSkewSymmetricMatrix(rot);
		double psi = rot.normF();
		double f1 = 0.0, f2 = 0.0;
		if (psi > 0.0) {
			f1 = Math.sin(psi)/psi;
			f2 = Math.sin(psi/2.0)/(psi/2.0);
			//System.out.println(f1+" "+f2);
		}
		return Matrix.identity(3, 3).plus(Psi.times(f1)).plus(Psi.times(Psi).times(0.5*f2*f2));
	}
	
	static public Matrix rotationMatrixFromAngles2(Matrix rot) {
		Matrix Psi = getSkewSymmetricMatrix(rot);
		return Matrix.identity(3, 3).plus(Psi).plus(Psi.times(Psi).times(0.5));
	}*/
	
	public static double[] anglesFromRotationMatrix(Matrix R) {
		try {
			Matrix I = Matrix.identity(3, 3);
			Matrix gx = (R.minus(I)).times((R.plus(I)).inverse());
			Matrix g = new Matrix(3, 1);
			g.set(0, 0, gx.get(2, 1));
			g.set(1, 0, gx.get(0, 2));
			g.set(2, 0, gx.get(1, 0));
			double norm = g.normF();
			if (norm > 0) {
				g.timesEquals(2.0*Math.atan(norm)/norm);
			}
			return g.getColumnPackedCopy();
		}
		catch (RuntimeException e) {
			return anglesFromRotationMatrixFallback(R);
		}
	}
	
	private static double[] anglesFromRotationMatrixFallback(Matrix R) {
		final double tol = SimLive.ZERO_TOL;
		Matrix A = (R.minus(R.transpose())).times(0.5);
		Matrix rho = new Matrix(3, 1);
		rho.set(0, 0, A.get(2, 1));
		rho.set(1, 0, A.get(0, 2));
		rho.set(2, 0, A.get(1, 0));
		double s = rho.normF();
		double c = (R.trace()-1.0)/2.0;
		if (s < tol) {
			if (c > 0.0) {
				return new double[3];
			}
			else {
				for (int i = 0; i < 3; i++) {
					Matrix v = R.plus(Matrix.identity(3, 3)).getMatrix(0, 2, i, i);
					if (v.normF() > tol) {
						Matrix r = v.times(Math.PI/v.normF());
						if ((Math.abs(r.get(0, 0)) < tol && Math.abs(r.get(1, 0)) < tol && r.get(2, 0) < 0.0) ||
							(Math.abs(r.get(0, 0)) < tol && r.get(1, 0) < 0.0) || r.get(0, 0) < 0.0) {
							r = r.times(-1.0);
						}
						return r.getColumnPackedCopy();
					}
				}
			}
		}
		double theta = Math.atan2(s, c);
		Matrix r = rho.times(theta/s);
		return r.getColumnPackedCopy();
	}
	
	@Override
	public Matrix getElementForceNL(ArrayList<Node> nodes, Matrix u_global, boolean localSys) {
		
		//Large displacement, small strain formulation
		//Co-rotational formulation, euler-bernoulli beam
		Matrix u_elem = globalToLocalVector(u_global);
		
		double length = getCurrentLength(nodes, u_elem);
		double length0 = getLength();
		Matrix r1 = getr1(nodes, u_elem, length);
		Matrix Rr = getRr(u_elem, r1);
		
		Matrix R_local = Rr.transpose().times(rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0))).times(R0);
		double[] angles0 = anglesFromRotationMatrix(R_local);
		R_local = Rr.transpose().times(rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0))).times(R0);
		double[] angles1 = anglesFromRotationMatrix(R_local);
				
		Matrix f_local = getFLocal(length-length0, angles0, angles1, length0);
		
		Matrix B = getBMatrix(u_elem, Rr, r1, length);
		
		if (localSys) {
			Matrix temp = new Matrix(12, 1);
			temp.set(0, 0, -f_local.get(0, 0));
			temp.set(1, 0, -(f_local.get(6, 0)+f_local.get(3, 0))/length);
			temp.set(2, 0, -(f_local.get(5, 0)+f_local.get(2, 0))/length);
			temp.set(3, 0, f_local.get(1, 0));
			temp.set(4, 0, f_local.get(2, 0));
			temp.set(5, 0, f_local.get(3, 0));
			temp.set(6, 0, -temp.get(0, 0));
			temp.set(7, 0, -temp.get(1, 0));
			temp.set(8, 0, -temp.get(2, 0));
			temp.set(9, 0, f_local.get(4, 0));
			temp.set(10, 0, f_local.get(5, 0));
			temp.set(11, 0, f_local.get(6, 0));
			return temp;
		}
		
		return B.transpose().times(f_local);
	}
	
	private Matrix getFLocal(double u, double[] a0, double[] a1, double L0) {
		double E, A, Iy, Iz, Io, G, It;
		E = material.getYoungsModulus();
		A = section.getArea();
		Iy = section.getIy();
		Iz = section.getIz();
		Io = Iy+Iz;
		It = section.getIt();
		G = E/(2.0*(1.0+material.getPoissonsRatio()));
		//Rechteck: Irr = 1/80*h^5*w + 1/72*h^3*w^3 + 1/80*h*w^5
		//Kreis: Irr = d^6*pi/192
		
		double delta_a = a0[0] - a1[0];
		double a_h = a0[1]*a0[1] + a1[1]*a1[1] - 1.0/2.0*a0[1]*a1[1] + a0[2]*a0[2] + a1[2]*a1[2] - 1.0/2.0*a0[2]*a1[2];
		
		Matrix f_local = new Matrix(7, 1);
		f_local.set(0, 0, E*A*u/L0);
		f_local.set(1, 0, 1.0/15.0*delta_a*((a_h*Io*L0*L0 + 15.0*Io*L0*u)*E + 15.0*G*It*L0*L0)/(L0*L0*L0));
		f_local.set(2, 0, 1.0/225.0*(2.0*a_h*A*(a0[1] - 1.0/4.0*a1[1])*L0*L0 + 30.0*A*(a0[1] - 1.0/4.0*a1[1])*u*L0 + (15.0*Io*delta_a*delta_a + 900.0*Iy)*a0[1] - 15.0/4.0*(Io*delta_a*delta_a - 120.0*Iy)*a1[1])*E/L0);
		f_local.set(3, 0, 1.0/225.0*(2.0*a_h*A*(a0[2] - 1.0/4.0*a1[2])*L0*L0 + 30.0*A*(a0[2] - 1.0/4.0*a1[2])*u*L0 + (15.0*Io*delta_a*delta_a + 900.0*Iz)*a0[2] - 15.0/4.0*(Io*delta_a*delta_a - 120.0*Iz)*a1[2])*E/L0);
		f_local.set(4, 0, -f_local.get(1, 0));
		f_local.set(5, 0, -1.0/450.0*(a_h*A*(a0[1] - 4.0*a1[1])*L0*L0 + 15.0*A*u*(a0[1] - 4.0*a1[1])*L0 + (15.0/2.0*Io*delta_a*delta_a - 900.0*Iy)*a0[1] - 30.0*(Io*delta_a*delta_a + 60.0*Iy)*a1[1])*E/L0);
		f_local.set(6, 0, -1.0/450.0*(a_h*A*(a0[2] - 4.0*a1[2])*L0*L0 + 15.0*A*u*(a0[2] - 4.0*a1[2])*L0 + (15.0/2.0*Io*delta_a*delta_a - 900.0*Iz)*a0[2] - 30.0*(Io*delta_a*delta_a + 60.0*Iz)*a1[2])*E/L0);
		
		return f_local;
	}

	@Override
	protected Matrix getMelem(ArrayList<Node> nodes) {

		double rho, A, l;
		rho = material.getDensity();
		A = section.getArea();
		l = getLength();
		
		Matrix M_elem = null;
		
		{
			/* lumped mass matrix */
			M_elem = new Matrix(12, 12);
			
			M_elem.set(0, 0, 1.0/2.0);
			M_elem.set(1, 1, 1.0/2.0);
			M_elem.set(2, 2, 1.0/2.0);
			M_elem.set(3, 3, l*l/100.0);
			M_elem.set(4, 4, l*l/100.0);
			M_elem.set(5, 5, l*l/100.0);
			M_elem.set(6, 6, 1.0/2.0);
			M_elem.set(7, 7, 1.0/2.0);
			M_elem.set(8, 8, 1.0/2.0);
			M_elem.set(9, 9, l*l/100.0);
			M_elem.set(10, 10, l*l/100.0);
			M_elem.set(11, 11, l*l/100.0);
			
			M_elem = M_elem.times(rho*A*l);
		}
		
		/* no need to transform */
		return M_elem;
	}
	
	private Matrix getTransformation() {
		
		Matrix R = getR0().transpose();
		
		Matrix T = new Matrix(12, 12);
		T.setMatrix(0, 2, 0, 2, R);
		T.setMatrix(3, 5, 3, 5, R);
		T.setMatrix(6, 8, 6, 8, R);
		T.setMatrix(9, 11, 9, 11, R);
		
		return T;
	}
	
	@Override
	public void setIndexIncidence(Solution solution, ArrayList<Node> nodes) {

		I = new Matrix(12, 1);
		
		for (int n = 0; n < nodes.size(); n++) {
			int dof = solution.getDofOfNodeID(n);
			if (n == this.elementNodes[0]) {
				I.set(0, 0, dof);
				I.set(1, 0, dof+1);
				I.set(2, 0, dof+2);
				I.set(3, 0, dof+3);
				I.set(4, 0, dof+4);
				I.set(5, 0, dof+5);
			}
			if (n == this.elementNodes[1]) {
				I.set(6, 0, dof);
				I.set(7, 0, dof+1);
				I.set(8, 0, dof+2);
				I.set(9, 0, dof+3);
				I.set(10, 0, dof+4);
				I.set(11, 0, dof+5);
			}
		}
	}
	
	@Override
	public double[] getShapeFunctionValues(double t) {
		double[] h = new double[6];
		double l = getLength();
		h[0] = 1.0-t;
		h[1] = 2.0*t*t*t-3.0*t*t+1.0;
		h[2] = l*(t*t*t-2.0*t*t+t);
		h[3] = t;
		h[4] = -2.0*t*t*t+3.0*t*t;
		h[5] = l*(t*t*t-t*t);
		return h;
	}
	
	@Override
	public double[] interpolateNodeKinematicValues(double t, Increment increment, int val) {
		double[][] xyzVal = new double[2][3];
		double[][] phiVal = new double[2][3];
		Matrix Rr = increment.getRrBeam(id);
		
		switch (val) {
			case 0:	xyzVal[0] = increment.getDisplacement(elementNodes[0]);
					xyzVal[1] = increment.getDisplacement(elementNodes[1]);
					phiVal = increment.getAnglesBeam(id);
					break;
			case 1:	xyzVal[0] = increment.getAcceleration(elementNodes[0]);
					xyzVal[1] = increment.getAcceleration(elementNodes[1]);
					phiVal = increment.getAngularAccBeam(id);
					break;
			case 2:	xyzVal[0] = increment.getVelocity(elementNodes[0]);
					xyzVal[1] = increment.getVelocity(elementNodes[1]);
					phiVal = increment.getAngularVelBeam(id);
					break;
		}
		double[] values = new double[3];
		values[0] = xyzVal[0][0]+(xyzVal[1][0]-xyzVal[0][0])*t;
		values[1] = xyzVal[0][1]+(xyzVal[1][1]-xyzVal[0][1])*t;
		values[2] = xyzVal[0][2]+(xyzVal[1][2]-xyzVal[0][2])*t;
		double[] l = getBendingDispInCoRotatedFrame(t, phiVal);
		Matrix loc = new Matrix(new double[]{0, l[0], l[1]}, 3);
		double[] bend = Rr.times(loc).getColumnPackedCopy();
		values[0] += bend[0];
		values[1] += bend[1];
		values[2] += bend[2];		
		return values;
	}
	
	public double[][] getAngularValuesInCoRotatedFrame(Increment increment, Matrix Rr, int val /* 0 - angle, 1 - angularVel, 2 - angularAcc */) {
		double[][] angles = new double[2][3];
		if (increment != null) {
			Element refModelElem = increment.getSolution().getRefModel().getElements().get(id);
			Matrix x_global = null;
			switch (val) {
				case 0: x_global = increment.get_u_global();
						break;
				case 1: x_global = increment.get_v_global();
						break;
				case 2: x_global = increment.get_a_global();
						break;
			}
			Matrix x_elem = refModelElem.globalToLocalVector(x_global);
			Matrix a0 = x_elem.getMatrix(3, 5, 0, 0);
			Matrix a1 = x_elem.getMatrix(9, 11, 0, 0);
			if (val == 0) {
				Matrix R_local = Rr.transpose().times(rotationMatrixFromAngles(a0)).times(R0);
				angles[0] = anglesFromRotationMatrix(R_local);
				R_local = Rr.transpose().times(rotationMatrixFromAngles(a1)).times(R0);
				angles[1] = anglesFromRotationMatrix(R_local);
			}
			else {
				angles[0] = a0.getColumnPackedCopy();
				angles[1] = a1.getColumnPackedCopy();
			}
		}
		return angles;
	}
	
	public double[] getBendingDispInCoRotatedFrame(double t, double[][] angles) {
		double length = getLength();
		double f2 = t*(1.0-t)*(1.0-t)*length;
		double f4 = t*t*(t-1.0)*length;
		double[] values = new double[2];
		values[0] = f2*angles[0][2]+f4*angles[1][2];
		values[1] = -f2*angles[0][1]-f4*angles[1][1];
		return values;
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
		Beam newElement = new Beam(element_node[1]);
		newElement.setMaterial(this.getMaterial());
		newElement.setSection(this.getSection());
		newElement.setQ0(q0.clone());
		newElement.setStiffnessDamping(this.getStiffnessDamping());
		newElement.setMassDamping(this.getMassDamping());
		elements.add(elements.indexOf(this)+1, newElement);
		newElements.add(newElement);
		return newElements;
	}
	
	@Override
	public String[] getLocalDofNames() {
		String[] dofNames = new String[12];
		for (int counter = 0, n = 0; n < 2; n++) {
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
		Beam beam = new Beam();
		beam.elementNodes = this.elementNodes.clone();
		if (isMaterialValid(SimLive.model.getMaterials()))
			beam.material = model.getMaterials().get(SimLive.model.getMaterials().indexOf(this.material));
		if (isSectionValid(SimLive.model.getSections()))
			beam.section = model.getSections().get(SimLive.model.getSections().indexOf(this.section));
		beam.R0 = this.R0.copy();
		beam.q0 = this.q0.clone();
		beam.id = this.id;
		beam.stiffnessDamping = this.stiffnessDamping;
		beam.massDamping = this.massDamping;
		return beam;
	}

	@Override
	public boolean deepEquals(Object obj) {
		if (!(obj instanceof Beam)) return false;
		Beam element = (Beam) obj;
		if (this.getType() != element.getType()) return false;
		if (!Arrays.equals(this.elementNodes, element.elementNodes)) return false;
		if (this.material != null && element.material != null)
			if (!this.material.deepEquals(element.material)) return false;
		if (this.section != null && element.section != null)
			if (!this.section.deepEquals(element.section)) return false;
		if (!Arrays.equals(this.R0.getRowPackedCopy(), element.R0.getRowPackedCopy())) return false;
		if (!Arrays.equals(this.q0, element.q0)) return false;
		if (this.id != element.id) return false;
		if (this.stiffnessDamping != element.stiffnessDamping) return false;
		if (this.massDamping != element.massDamping) return false;
		return true;
	}

}

package simlive.model;

import java.util.ArrayList;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.misc.GeomUtility;
import simlive.postprocessing.Post.Layer;
import simlive.view.View;

public abstract class PlaneElement extends Element {

	//public enum State {PLANE_STRESS, PLANE_STRAIN}
	protected double thickness;
	//protected State state;
	protected Matrix R0;
	protected Matrix K_elem; //solution speed-up

	public PlaneElement() {
		super();
	}

	public void setThickness(double thickness) {
		this.thickness = thickness;
	}
	
	public double getThickness() {
		return thickness;
	}
	
	/*public void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return state;
	}*/
	
	public Matrix getR0() {
		return R0;
	}
	
	public Matrix getRr(ArrayList<Node> nodes, Matrix u_elem) {
		double[][] x = new double[3][];
		x[0] = nodes.get(elementNodes[0]).getCoords().clone();
		x[0][0] += u_elem.get(0, 0);
		x[0][1] += u_elem.get(1, 0);
		x[0][2] += u_elem.get(2, 0);
		x[1] = nodes.get(elementNodes[1]).getCoords().clone();
		x[1][0] += u_elem.get(6, 0);
		x[1][1] += u_elem.get(7, 0);
		x[1][2] += u_elem.get(8, 0);
		x[2] = nodes.get(elementNodes[elementNodes.length-1]).getCoords().clone();
		x[2][0] += u_elem.get((elementNodes.length-1)*6, 0);
		x[2][1] += u_elem.get((elementNodes.length-1)*6+1, 0);
		x[2][2] += u_elem.get((elementNodes.length-1)*6+2, 0);
		Matrix vx = new Matrix(3, 1);
		vx.set(0, 0, x[1][0]-x[0][0]);
		vx.set(1, 0, x[1][1]-x[0][1]);
		vx.set(2, 0, x[1][2]-x[0][2]);
		Matrix vy = new Matrix(3, 1);
		vy.set(0, 0, x[2][0]-x[0][0]);
		vy.set(1, 0, x[2][1]-x[0][1]);
		vy.set(2, 0, x[2][2]-x[0][2]);
		Matrix vz = vx.crossProduct(vy);
		vy = vz.crossProduct(vx);
		Matrix Rr = new Matrix(3, 3);
		Rr.setMatrix(0, 2, 0, 0, vx.times(1.0/vx.normF()));
		Rr.setMatrix(0, 2, 1, 1, vy.times(1.0/vy.normF()));
		Rr.setMatrix(0, 2, 2, 2, vz.times(1.0/vz.normF()));
		return Rr;
	}
	
	private void initR0(ArrayList<Node> nodes) {
		double[][] x = new double[3][];
		x[0] = nodes.get(elementNodes[0]).getCoords();
		x[1] = nodes.get(elementNodes[1]).getCoords();
		x[2] = nodes.get(elementNodes[elementNodes.length-1]).getCoords();
		Matrix vx = new Matrix(3, 1);
		vx.set(0, 0, x[1][0]-x[0][0]);
		vx.set(1, 0, x[1][1]-x[0][1]);
		vx.set(2, 0, x[1][2]-x[0][2]);
		Matrix vy = new Matrix(3, 1);
		vy.set(0, 0, x[2][0]-x[0][0]);
		vy.set(1, 0, x[2][1]-x[0][1]);
		vy.set(2, 0, x[2][2]-x[0][2]);
		Matrix vz = vx.crossProduct(vy);
		vy = vz.crossProduct(vx);
		R0 = new Matrix(3, 3);
		R0.setMatrix(0, 2, 0, 0, vx.times(1.0/vx.normF()));
		R0.setMatrix(0, 2, 1, 1, vy.times(1.0/vy.normF()));
		R0.setMatrix(0, 2, 2, 2, vz.times(1.0/vz.normF()));
	}
	
	protected abstract Matrix getKelem(ArrayList<Material> materials, ArrayList<Section> sections, ArrayList<Node> nodes);
	
	public void initKelem(ArrayList<Material> materials, ArrayList<Section> sections, ArrayList<Node> nodes) {
		K_elem = getKelem(materials, sections, nodes);
	}

	@Override
	public void update() {
		updateIDAndMaterial();
		initR0(SimLive.model.getNodes());
	}
	
	@Override
	public void adaptNodeIDs(int deleteNodeID) {
		for (int i = 0; i < elementNodes.length; i++) {
			if (elementNodes[i] >= deleteNodeID) elementNodes[i]--;
		}
	}
	
	@Override
	public boolean isLineElement() {
		return false;
	}
	
	@Override
	public boolean isPlaneElement() {
		return true;
	}
	
	public abstract double[] getLocalCoords(int localNodeID);
		
	public abstract double[] getShapeFunctionValues(double r, double s);
	
	protected abstract Matrix getJacobian(double[] px, double[] py, double r, double s);
	
	protected abstract Matrix getBMatrixPlate(double[][] x, Matrix Jinv, double r, double s);
	
	protected abstract Matrix getBMatrixMembrane(Matrix Jinv, double r, double s);
	
	protected Matrix getMaterialMatrix(ArrayList<Material> materials) {
		Matrix C = new Matrix(3, 3);
		
		/*if (state == PlaneElement.State.PLANE_STRESS)*/ {
			double E = materials.get(material_id).getYoungsModulus();
			double mu = materials.get(material_id).getPoissonsRatio();
			
			C.set(0, 0, 1.0);
			C.set(0, 1, mu);
			C.set(1, 0, mu);
			C.set(1, 1, 1.0);
			C.set(2, 2, 0.5*(1.0-mu));
			C.timesEquals(E/(1.0-mu*mu));
		}
		/*else {
			double[] lameConstants = materials.get(material_id).getLameConstants();
			
			C.set(0, 0, lameConstants[0]+2.0*lameConstants[1]);
			C.set(0, 1, lameConstants[0]);
			C.set(1, 0, lameConstants[0]);
			C.set(1, 1, lameConstants[0]+2.0*lameConstants[1]);
			C.set(2, 2, lameConstants[1]);
		}*/
		
		return C;
	}
	
	public Matrix getStrain(ArrayList<Node> nodes, Matrix u_global,
			double r, double s) {
		double[][] x = new double[2][elementNodes.length];
		Matrix R0T = R0.transpose();
		for (int i = 0; i < elementNodes.length; i++) {
			Matrix global = new Matrix(nodes.get(elementNodes[i]).getCoords(), 3);
			Matrix local = R0T.times(global);
			x[0][i] = local.get(0, 0);
			x[1][i] = local.get(1, 0);
		}
		Matrix J = getJacobian(x[0], x[1], r, s);
		Matrix Jinv = J.inverse();
		
		Matrix u_elem = globalToLocalVector(u_global);
		Matrix RrT = R0T;
		if (SimLive.settings.isLargeDisplacement) {
			RrT = getRr(nodes, u_elem).transpose();				
		}
		Matrix u_membrane = new Matrix(elementNodes.length*2, 1);
		Matrix u_plate = new Matrix(elementNodes.length*3, 1);
		Matrix r0 = new Matrix(nodes.get(elementNodes[0]).getCoords(), 3);
		Matrix r0u = r0.plus(u_elem.getMatrix(0, 2, 0, 0));
		for (int n = 0; n < elementNodes.length; n++) {
			Matrix rn = new Matrix(nodes.get(elementNodes[n]).getCoords(), 3);
			Matrix rnu = rn.plus(u_elem.getMatrix(n*6, n*6+2, 0, 0));
			Matrix uloc = RrT.times(rnu.minus(r0u)).minus(R0T.times(rn.minus(r0)));
			u_membrane.set(n*2, 0, uloc.get(0, 0));
			u_membrane.set(n*2+1, 0, uloc.get(1, 0));
			u_plate.set(n*3, 0, uloc.get(2, 0));
			Matrix Rg = Beam.rotationMatrixFromAngles(u_elem.getMatrix(n*6+3, n*6+5, 0, 0));
			Matrix Rloc = RrT.times(Rg).times(R0);
			double[] rloc = Beam.anglesFromRotationMatrix(Rloc);
			u_plate.set(n*3+1, 0, rloc[0]);
			u_plate.set(n*3+2, 0, rloc[1]);
		}
		
		Matrix Bm = getBMatrixMembrane(Jinv, r, s);
		Matrix Bp = getBMatrixPlate(x, Jinv, r, s);
		int sign = SimLive.post.getLayer() == Layer.TOP ? 1 : -1;
		
		return Bm.times(u_membrane).plus(Bp.times(u_plate).times(sign*thickness/2.0));
	}
	
	public Matrix getStress(ArrayList<Material> materials,
			ArrayList<Node> nodes, Matrix u_global, double r, double s) {
		Matrix C = getMaterialMatrix(materials);		
		return C.times(getStrain(nodes, u_global, r, s));
	}
	
	public double getThickening(ArrayList<Material> materials, ArrayList<Node> nodes,
			Matrix u_global, double r, double s) {
		/*if (state == PlaneElement.State.PLANE_STRESS)*/ {
			double t = getThickness();
			double E, mu;
			E = materials.get(material_id).getYoungsModulus();
			mu = materials.get(material_id).getPoissonsRatio();
			
			Matrix stress = getStress(materials, nodes, u_global, r, s);
			return -t*mu/E*(stress.get(0, 0)+stress.get(1, 0));
		}
		/*else {
			return 0.0;
		}*/
	}
	
	public double interpolateNodeValues(double[] shapeFunctionValues, double[] v) {
		double value = shapeFunctionValues[0]*v[0]+shapeFunctionValues[1]*v[1]+shapeFunctionValues[2]*v[2];
		if (elementNodes.length > 3) {
			value += shapeFunctionValues[3]*v[3];
		}
		return value;
	}
	
	public int interpolateNodeValues(double[] shapeFunctionValues, int[] v) {
		double value = shapeFunctionValues[0]*v[0]+shapeFunctionValues[1]*v[1]+shapeFunctionValues[2]*v[2];
		if (elementNodes.length > 3) {
			value += shapeFunctionValues[3]*v[3];
		}
		return (int) value;
	}
	
	public double[] getGlobalFromLocalCoordinates(double r, double s) {
		double[] px = new double[elementNodes.length];
		double[] py = new double[elementNodes.length];
		Matrix Rr = null;
		try {
			Rr = new Matrix(View.Rr[id]);
		}
		catch (Exception e) {
			Rr = R0;
		}
		Matrix c0 = new Matrix(View.getCoordsWithScaledDisp(elementNodes[0]), 3);
		for (int n = 1; n < elementNodes.length; n++) {
			Matrix c = new Matrix(View.getCoordsWithScaledDisp(elementNodes[n]), 3);
			c = Rr.transpose().times(c.minus(c0));
			px[n] = c.get(0, 0);
			py[n] = c.get(1, 0);
		}
		double[] shapeFunctionValues = getShapeFunctionValues(r, s);
		double[] localCoords = new double[3];
		localCoords[0] = interpolateNodeValues(shapeFunctionValues, px);
		localCoords[1] = interpolateNodeValues(shapeFunctionValues, py);
	    return c0.plus(Rr.times(new Matrix(localCoords, 3))).getColumnPackedCopy();
	}
	
	public double[] getLocalFromGlobalCoordinates(double[] p) {
		double[] px = new double[elementNodes.length];
		double[] py = new double[elementNodes.length];
		Matrix Rr = null;
		try {
			Rr = new Matrix(View.Rr[id]);
		}
		catch (Exception e) {
			Rr = R0;
		}
		Matrix c0 = new Matrix(View.getCoordsWithScaledDisp(elementNodes[0]), 3);
		for (int n = 1; n < elementNodes.length; n++) {
			Matrix c = new Matrix(View.getCoordsWithScaledDisp(elementNodes[n]), 3);
			c = Rr.transpose().times(c.minus(c0));
			px[n] = c.get(0, 0);
			py[n] = c.get(1, 0);
		}
		p = Rr.transpose().times(new Matrix(p, 3).minus(c0)).getColumnPackedCopy();

		return getLocalFromGlobalCoordinates(p, px, py);
	}
	
	public double[] getLocalFromGlobalCoordinates(double[] p, double[] px, double[] py) {
		double r = 0.0, s = 0.0;
		double x, y;
		for (int i = 0; i < 100; i++) {
			double[] shapeFunctionValues = getShapeFunctionValues(r, s);
			x = interpolateNodeValues(shapeFunctionValues, px);
			y = interpolateNodeValues(shapeFunctionValues, py);
			Matrix J = getJacobian(px, py, r, s);
		    Matrix rhs = new Matrix(2,1);
		    rhs.set(0,0, p[0]-x);
		    rhs.set(1,0, p[1]-y);
		    if (rhs.normF() == 0.0) break;
		    Matrix delta = J.transpose().inverse().times(rhs);
			r += delta.get(0,0);
			s += delta.get(1,0);
		}
		return new double[] {r, s};
	}
	
	public double[] getDispAtLocalCoordinates(double r, double s) {
		double[] disp = new double[3];
		if (SimLive.mode == Mode.RESULTS) {
			double[] dx = new double[elementNodes.length];
			double[] dy = new double[elementNodes.length];
			double[] dz = new double[elementNodes.length];
			for (int n = 0; n < elementNodes.length; n++) {
				double[] d = SimLive.post.getPostIncrement().getDisplacement(elementNodes[n]);
				dx[n] = d[0];
				dy[n] = d[1];
				dz[n] = d[2];
			}
			double[] shapeFunctionValues = getShapeFunctionValues(r, s);
		    disp[0] = interpolateNodeValues(shapeFunctionValues, dx)*SimLive.post.getScaling();
		    disp[1] = interpolateNodeValues(shapeFunctionValues, dy)*SimLive.post.getScaling();
		    disp[2] = interpolateNodeValues(shapeFunctionValues, dz)*SimLive.post.getScaling();
		}
	    return disp;
	}

	public double[] getCoordsInElement(double[] modelCoords2d) {
		double[][] coords = new double[elementNodes.length][];
		double[][] coordsTop = new double[elementNodes.length][];
		double[][] coordsBottom = new double[elementNodes.length][];
		double[] norm = new double[3];
		try {
			norm[0] = View.Rr[id][0][2];
			norm[1] = View.Rr[id][1][2];
			norm[2] = View.Rr[id][2][2];
			for (int n = 0; n < elementNodes.length; n++) {
				double[] nodeNormal = View.nodeNormals[elementNodes[n]];
				coords[n] = View.getCoordsWithScaledDisp(elementNodes[n]);
				coordsTop[n] = new double[]{coords[n][0]+nodeNormal[0]*thickness/2.0, coords[n][1]+nodeNormal[1]*thickness/2.0, coords[n][2]+nodeNormal[2]*thickness/2.0};
				coordsBottom[n] = new double[]{coords[n][0]-nodeNormal[0]*thickness/2.0, coords[n][1]-nodeNormal[1]*thickness/2.0, coords[n][2]-nodeNormal[2]*thickness/2.0};
			}
		}
		catch (Exception e) {
			return new double[3];
		}
		double[] dir = View.getViewDirection(modelCoords2d);
		if (dir[0]*norm[0]+dir[1]*norm[1]+dir[2]*norm[2] > 0.0) {
			double[] intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coordsTop[0], coordsTop[1], coordsTop[2]);
			if (GeomUtility.isPointInTriangle3d(coordsTop[0], coordsTop[1], coordsTop[2], intersect)) {
				return intersect;
			}
			if (elementNodes.length > 3) {
				intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coordsTop[0], coordsTop[2], coordsTop[3]);
				if (GeomUtility.isPointInTriangle3d(coordsTop[0], coordsTop[2], coordsTop[3], intersect)) {
					return intersect;
				}
			}
		}
		else {
			double[] intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coordsBottom[0], coordsBottom[1], coordsBottom[2]);
			if (GeomUtility.isPointInTriangle3d(coordsBottom[0], coordsBottom[1], coordsBottom[2], intersect)) {
				return intersect;
			}
			if (elementNodes.length > 3) {
				intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coordsBottom[0], coordsBottom[2], coordsBottom[3]);
				if (GeomUtility.isPointInTriangle3d(coordsBottom[0], coordsBottom[2], coordsBottom[3], intersect)) {
					return intersect;
				}
			}
		}
		for (int i = 0; i < elementNodes.length; i++) {
			int i1 = (i+1)%elementNodes.length;
			double[] diff = new double[3];
			diff[0] = coords[i1][0] - coords[i][0];
			diff[1] = coords[i1][1] - coords[i][1];
			diff[2] = coords[i1][2] - coords[i][2];
			double[] edgeNormal = new double[3];
			edgeNormal[0] = diff[1]*norm[2]-diff[2]*norm[1];
			edgeNormal[1] = diff[2]*norm[0]-diff[0]*norm[2];
			edgeNormal[2] = diff[0]*norm[1]-diff[1]*norm[0];
			if ((dir[0]*edgeNormal[0]+dir[1]*edgeNormal[1]+dir[2]*edgeNormal[2]) > 0.0) {
				double[] intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coordsBottom[i], coordsBottom[i1], coordsTop[i1]);
				if (GeomUtility.isPointInTriangle3d(coordsBottom[i], coordsBottom[i1], coordsTop[i1], intersect)) {
					return intersect;
				}
				intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coordsBottom[i], coordsTop[i1], coordsTop[i]);
				if (GeomUtility.isPointInTriangle3d(coordsBottom[i], coordsTop[i1], coordsTop[i], intersect)) {
					return intersect;
				}
			}
		}
		return null;
	}
	
	public double[] getCoordsInRigidElement(double[] modelCoords2d, ArrayList<Node> nodes) {
		double[][] coords = new double[elementNodes.length][];
		for (int n = 0; n < elementNodes.length; n++) {
			coords[n] = nodes.get(elementNodes[n]).getCoords();
		}
		double[] dir = View.getViewDirection(modelCoords2d);
		if (Math.abs(dir[0]*R0.get(0, 2)+dir[1]*R0.get(1, 2)+dir[2]*R0.get(2, 2)) > SimLive.ZERO_TOL) {
			double[] intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coords[0], coords[1], coords[2]);
			if (GeomUtility.isPointInTriangle3d(coords[0], coords[1], coords[2], intersect)) {
				return intersect;
			}
			if (elementNodes.length > 3) {
				intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, coords[0], coords[2], coords[3]);
				if (GeomUtility.isPointInTriangle3d(coords[0], coords[2], coords[3], intersect)) {
					return intersect;
				}
			}
		}
		return null;
	}
	
	public boolean isPointInElement(double[] point) {
		double[][] coords = new double[elementNodes.length][];
		for (int n = 0; n < elementNodes.length; n++) {
			coords[n] = View.getCoordsWithScaledDisp(elementNodes[n]);
		}
		
		return GeomUtility.isPointInTriangle(coords[0], coords[1], coords[2], point, null) ||
			(elementNodes.length > 3 && GeomUtility.isPointInTriangle(coords[0], coords[2], coords[3], point, null));
	}

}

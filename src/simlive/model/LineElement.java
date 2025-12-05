package simlive.model;
import java.util.ArrayList;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.misc.GeomUtility;
import simlive.misc.Settings;
import simlive.solution.Increment;
import simlive.view.View;

public abstract class LineElement extends Element {
	
	protected Section section;
	protected Matrix R0 = Matrix.identity(3, 3);
	protected double[] q0 = new double[]{0, 1, 0};
	
	protected LineElement() {
		super();
	}
	
	public LineElement(int[] elementNodes) {
		super();
		this.elementNodes = elementNodes;
	}
	
	@Override
	public boolean isLineElement() {
		return true;
	}
	
	@Override
	public boolean isPlaneElement() {
		return false;
	}
		
	@Override
	public void adaptNodeIDs(int deleteNodeID) {
		if (elementNodes[0] >= deleteNodeID) elementNodes[0]--;
		if (elementNodes[1] >= deleteNodeID) elementNodes[1]--;
	}

	public void setSection(Section section) {
		this.section = section;
	}
	
	public Section getSection() {
		return section;
	}
	
	public abstract boolean isSectionValid(ArrayList<Section> sections);
	
	public Section findSection(ArrayList<Section> sections, Section section) {
		for (int i = 0; i < sections.size(); i++) {
			if (sections.get(i).deepEquals(section, Result.EQUAL) == Result.EQUAL) return sections.get(i);
		}
		return null;
	}
	
	public double[] getQ0() {
		return q0;
	}

	public void setQ0(double[] q0) {
		this.q0 = q0;
		update();
	}

	public double getDeltaX() {
		ArrayList<Node> nodes = SimLive.model.getNodes();
		return nodes.get(elementNodes[1]).getXCoord()-nodes.get(elementNodes[0]).getXCoord();
	}
	
	public double getDeltaY() {
		ArrayList<Node> nodes = SimLive.model.getNodes();
		return nodes.get(elementNodes[1]).getYCoord()-nodes.get(elementNodes[0]).getYCoord();
	}
	
	public double getDeltaZ() {
		ArrayList<Node> nodes = SimLive.model.getNodes();
		return nodes.get(elementNodes[1]).getZCoord()-nodes.get(elementNodes[0]).getZCoord();
	}
	
	public double getLength() {
		double dx = getDeltaX();
		double dy = getDeltaY();
		double dz = getDeltaZ();
		return Math.sqrt(dx*dx+dy*dy+dz*dz);
	}
	
	public double getCurrentDeltaX(ArrayList<Node> nodes, Matrix u_elem) {
		if (u_elem.getRowDimension() > 6) {
			return (nodes.get(elementNodes[1]).getXCoord()+u_elem.get(6, 0))-
				   (nodes.get(elementNodes[0]).getXCoord()+u_elem.get(0, 0));
		}
		else {
			return (nodes.get(elementNodes[1]).getXCoord()+u_elem.get(3, 0))-
				   (nodes.get(elementNodes[0]).getXCoord()+u_elem.get(0, 0));
		}
	}
	
	public double getCurrentDeltaY(ArrayList<Node> nodes, Matrix u_elem) {
		if (u_elem.getRowDimension() > 6) {
			return (nodes.get(elementNodes[1]).getYCoord()+u_elem.get(7, 0))-
				   (nodes.get(elementNodes[0]).getYCoord()+u_elem.get(1, 0));
		}
		else {
			return (nodes.get(elementNodes[1]).getYCoord()+u_elem.get(4, 0))-
				   (nodes.get(elementNodes[0]).getYCoord()+u_elem.get(1, 0));
		}
	}
	
	public double getCurrentDeltaZ(ArrayList<Node> nodes, Matrix u_elem) {
		if (u_elem.getRowDimension() > 6) {
			return (nodes.get(elementNodes[1]).getZCoord()+u_elem.get(8, 0))-
				   (nodes.get(elementNodes[0]).getZCoord()+u_elem.get(2, 0));
		}
		else {
			return (nodes.get(elementNodes[1]).getZCoord()+u_elem.get(5, 0))-
				   (nodes.get(elementNodes[0]).getZCoord()+u_elem.get(2, 0));
		}
	}
	
	public double getCurrentLength(ArrayList<Node> nodes, Matrix u_elem) {
		double dx = getCurrentDeltaX(nodes, u_elem);
		double dy = getCurrentDeltaY(nodes, u_elem);
		double dz = getCurrentDeltaZ(nodes, u_elem);
		return Math.sqrt(dx*dx+dy*dy+dz*dz);
	}
	
	public Matrix getr1(ArrayList<Node> nodes, Matrix u_elem, double length) {
		double[] r1 = new double[3];
		r1[0] = getCurrentDeltaX(nodes, u_elem)/length;
		r1[1] = getCurrentDeltaY(nodes, u_elem)/length;
		r1[2] = getCurrentDeltaZ(nodes, u_elem)/length;
		
		return new Matrix(r1, 3);
	}
	
	public Matrix getVectorTransformation(double[] r1) {
		
		Matrix r1_vec = new Matrix(r1, 3);
		Matrix q0_vec = new Matrix(q0, 3);
		Matrix r3_vec = r1_vec.crossProduct(q0_vec);
		double length = r3_vec.normF();
		if (length < SimLive.ZERO_TOL) {
			q0_vec = new Matrix(new double[]{0.0, 1.0, 0.0}, 3);
			r3_vec = r1_vec.crossProduct(q0_vec);
			length = r3_vec.normF();
			if (length < SimLive.ZERO_TOL) {
				q0_vec = new Matrix(new double[]{1.0, 0.0, 0.0}, 3);
				r3_vec = r1_vec.crossProduct(q0_vec);
				length = r3_vec.normF();
			}
		}
		if (SimLive.mode != Mode.RESULTS) {
			q0 = q0_vec.getColumnPackedCopy();
		}
		r3_vec.timesEquals(1.0/length);
		Matrix r2_vec = r3_vec.crossProduct(r1_vec);
		Matrix R = new Matrix(3, 3);
		R.setMatrix(0, 2, 0, 0, r1_vec);
		R.setMatrix(0, 2, 1, 1, r2_vec);
		R.setMatrix(0, 2, 2, 2, r3_vec);
		return R;
	}
	
	public abstract double[] getShapeFunctionValues(double t);
	
	public abstract double[] interpolateNodeKinematicValues(double t, Increment increment,
			int val /*0-displacement, 1-acceleration 2-velocity*/);
	
	public double getLocalFromGlobalCoordinates(double[] p) {
		double[] q0 = View.getCoordsWithScaledDisp(elementNodes[0]);
		double[] q1 = View.getCoordsWithScaledDisp(elementNodes[1]);
		double[] diff = new double[3];
		diff[0] = q1[0]-q0[0];
		diff[1] = q1[1]-q0[1];
		diff[2] = q1[2]-q0[2];
		double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
		diff[0] /= length;
		diff[1] /= length;
		diff[2] /= length;
		return ((p[0]-q0[0])*diff[0]+(p[1]-q0[1])*diff[1]+(p[2]-q0[2])*diff[2])/length;
	}
	
	public double[] getGlobalFromLocalCoordinates(double t) {
		double[] dispB = new double[3];
		if (SimLive.mode == Mode.RESULTS && getType() == Type.BEAM) {
			Matrix Rr = new Matrix(View.Rr[id]);
			double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(id);
			double[] dispBLoc = ((Beam) this).getBendingDispInCoRotatedFrame(t, angles);
			dispB[0] = (Rr.get(0, 1)*dispBLoc[0]+Rr.get(0, 2)*dispBLoc[1])*SimLive.post.getScaling();
			dispB[1] = (Rr.get(1, 1)*dispBLoc[0]+Rr.get(1, 2)*dispBLoc[1])*SimLive.post.getScaling();
			dispB[2] = (Rr.get(2, 1)*dispBLoc[0]+Rr.get(2, 2)*dispBLoc[1])*SimLive.post.getScaling();
		}
		double[] q0 = View.getCoordsWithScaledDisp(elementNodes[0]);
		double[] q1 = View.getCoordsWithScaledDisp(elementNodes[1]);
		double[] coords = new double[3];
		coords[0] = q0[0]+t*(q1[0]-q0[0])+dispB[0];
		coords[1] = q0[1]+t*(q1[1]-q0[1])+dispB[1];
		coords[2] = q0[2]+t*(q1[2]-q0[2])+dispB[2];
		return coords;
	}
	
	public double[] getDispAtLocalCoordinates(double t) {
		double[] disp = new double[3];
		if (SimLive.mode == Mode.RESULTS) {
			disp = getKinematicValuesAtLocalCoordinates(t, SimLive.post.getPostIncrementID(), 0);
			disp[0] *= SimLive.post.getScaling();
			disp[1] *= SimLive.post.getScaling();
			disp[2] *= SimLive.post.getScaling();
		}
		return disp;
	}
	
	public double[] getKinematicValuesAtLocalCoordinates(double t, int inc,
			int val /*0-displacement, 1-acceleration 2-velocity*/) {
		double[] values = new double[3];
		if (SimLive.mode == Mode.RESULTS) {
			values = interpolateNodeKinematicValues(t, SimLive.post.getSolution().getIncrement(inc), val);
		}
		return values;
	}
	
	private double[] getIntersection(double[] modelCoords2d, double[] dir, double[] A, double[] B, double[] C) {
		double[] norm = new double[3];
		norm[0] = (B[1]-A[1])*(C[2]-A[2])-(B[2]-A[2])*(C[1]-A[1]);
		norm[1] = (B[2]-A[2])*(C[0]-A[0])-(B[0]-A[0])*(C[2]-A[2]);
		norm[2] = (B[0]-A[0])*(C[1]-A[1])-(B[1]-A[1])*(C[0]-A[0]);
		double[] intersect = GeomUtility.getIntersectionLinePlane(modelCoords2d, dir, A, norm);
		if (GeomUtility.isPointInTriangle3d(A, B, C, intersect)) {
			return intersect;
		}
		return null;
	}
	
	public double[] getCoordsInElement(double[] modelCoords2d) {
		if (this.getType() != Element.Type.SPRING &&
				Settings.isShowSections && isSectionValid(SimLive.model.getSections()) &&
				section.getSectionShape().getType() != SectionShape.Type.DIRECT_INPUT) {
			double[] coords0 = View.getCoordsWithScaledDisp(elementNodes[0]);
			double[] coords1 = View.getCoordsWithScaledDisp(elementNodes[1]);
			double[] diff = new double[3];
			diff[0] = coords1[0]-coords0[0];
			diff[1] = coords1[1]-coords0[1];
			diff[2] = coords1[2]-coords0[2];
			double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
			Matrix Rr = null;
			try {
				Rr = new Matrix(View.Rr[id]);
			}
			catch (Exception e) {
				return new double[3];
			}
	    	
			double[][] P = section.getSectionPoints();
				
			double t = 0.0, y = 0.0, z = 0.0;
			int lineDivisions = SimLive.view.getLineDivisions(this);
			double[][][] p = new double[lineDivisions+1][P.length][];
			double deltaL = length/(double) lineDivisions;
			for (int i = 0; i < lineDivisions+1; i++) {
				t = i/(double) lineDivisions;
				if (getType() == Element.Type.BEAM && SimLive.mode == Mode.RESULTS) {
					Beam beam = (Beam) this;
		    		double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(beam.getID());
					double[] disp = beam.getBendingDispInCoRotatedFrame(t, angles);
	    			double deltaY = disp[0]*SimLive.post.getScaling()-y;
	    			double deltaZ = disp[1]*SimLive.post.getScaling()-z;
	    			double[] axis = new double[3];
	    			axis[1] = -deltaZ;
	    			axis[2] = deltaY;
	    			double sectionLength = Math.sqrt(deltaL*deltaL+deltaY*deltaY+deltaZ*deltaZ);
	    			double angle = Math.acos(deltaL/sectionLength);
	    			Matrix R1 = GeomUtility.getRotationMatrix(angle, axis);
	    			Matrix Rx = GeomUtility.getRotationMatrixX(2*angles[1][0]*t*SimLive.post.getScaling());
	    			for (int k = 0; k < P.length; k++) {
						p[i][k] = Rr.times(R1.times(Rx.times(new Matrix(P[k], 3))).plus(new Matrix(new double[]{t*length, y, z}, 3))).plus(
								new Matrix(new double[]{coords0[0], coords0[1], coords0[2]}, 3)).getColumnPackedCopy();
	    			}
	    			y = disp[0]*SimLive.post.getScaling();
					z = disp[1]*SimLive.post.getScaling();
				}
				else {
					for (int k = 0; k < P.length; k++) {
						p[i][k] = Rr.times(new Matrix(P[k], 3).plus(new Matrix(new double[]{t*length, 0, 0}, 3))).plus(
								new Matrix(new double[]{coords0[0], coords0[1], coords0[2]}, 3)).getColumnPackedCopy();
					}
				}
			}
			
			double[] dir = View.getViewDirection(modelCoords2d);
			for (int i = 0; i < lineDivisions; i++) {
				for (int k = 0; k < P.length; k++) {
					double[] intersect = getIntersection(modelCoords2d, dir, p[i][k], p[i+1][k], p[i][(k+1)%P.length]);
					if (intersect != null) return intersect;
					intersect = getIntersection(modelCoords2d, dir, p[i][(k+1)%P.length], p[i+1][k], p[i+1][(k+1)%P.length]);
					if (intersect != null) return intersect;
				}
				if (i == 0) {
					for (int k = 0; k < P.length; k++) {
						double[] intersect = getIntersection(modelCoords2d, dir, p[i][0], p[i][(k+1)%P.length], p[i][k]);
						if (intersect != null) return intersect;
					}
				}
				if (i == lineDivisions-1) {
					for (int k = 0; k < P.length; k++) {
						double[] intersect = getIntersection(modelCoords2d, dir, p[i+1][0], p[i+1][k], p[i+1][(k+1)%P.length]);
						if (intersect != null) return intersect;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public void update() {
		updateIDAndMaterial();
		
		/* assign section */
		if (!isSectionValid(SimLive.model.getSections())) {
			if (!SimLive.model.getSections().isEmpty()) {
				section = SimLive.model.getSections().get(0);
			}
		}
		
		/* geometric data */
		double length = getLength();
		double[] r10 = new double[3];
		r10[0] = getDeltaX()/length;
		r10[1] = getDeltaY()/length;
		r10[2] = getDeltaZ()/length;
		
		R0 = getVectorTransformation(r10);
	}
	
	public Matrix getR0() {
		return R0;
	}

}

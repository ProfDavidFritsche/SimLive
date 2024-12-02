package simlive.misc;

import Jama.Matrix;
import simlive.SimLive;

public abstract class GeomUtility {
	
	private final static double ZERO = 1E-25;

	public static Matrix getRotationMatrixX(double angle) {
		Matrix rot = new Matrix(3, 3);
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);
		rot.set(0, 0, 1);
		rot.set(1, 1, cosAngle);
		rot.set(1, 2, -sinAngle);
		rot.set(2, 1, sinAngle);
		rot.set(2, 2, cosAngle);
		return rot;
	}
	
	public static Matrix getRotationMatrixY(double angle) {
		Matrix rot = new Matrix(3, 3);
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);
		rot.set(0, 0, cosAngle);
		rot.set(0, 2, sinAngle);
		rot.set(1, 1, 1);
		rot.set(2, 0, -sinAngle);
		rot.set(2, 2, cosAngle);
		return rot;
	}
	
	public static Matrix getRotationMatrixZ(double angle) {
		Matrix rot = new Matrix(3, 3);
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);
		rot.set(0, 0, cosAngle);
		rot.set(0, 1, -sinAngle);
		rot.set(1, 0, sinAngle);
		rot.set(1, 1, cosAngle);
		rot.set(2, 2, 1);
		return rot;
	}
	
	public static Matrix getRotationMatrix(double angle, double[] axis) {
		double length = Math.sqrt(axis[0]*axis[0]+axis[1]*axis[1]+axis[2]*axis[2]);
		if (length > SimLive.ZERO_TOL) {
			axis[0] /= length;
			axis[1] /= length;
			axis[2] /= length;
			Matrix rot = new Matrix(3, 3);
			double cosAngle = Math.cos(angle);
			double sinAngle = Math.sin(angle);
			rot.set(0, 0, axis[0]*axis[0]*(1-cosAngle)+cosAngle);
			rot.set(0, 1, axis[0]*axis[1]*(1-cosAngle)-axis[2]*sinAngle);
			rot.set(0, 2, axis[0]*axis[2]*(1-cosAngle)+axis[1]*sinAngle);
			rot.set(1, 0, axis[0]*axis[1]*(1-cosAngle)+axis[2]*sinAngle);
			rot.set(1, 1, axis[1]*axis[1]*(1-cosAngle)+cosAngle);
			rot.set(1, 2, axis[1]*axis[2]*(1-cosAngle)-axis[0]*sinAngle);
			rot.set(2, 0, axis[0]*axis[2]*(1-cosAngle)-axis[1]*sinAngle);
			rot.set(2, 1, axis[1]*axis[2]*(1-cosAngle)+axis[0]*sinAngle);
			rot.set(2, 2, axis[2]*axis[2]*(1-cosAngle)+cosAngle);
			return rot;
		}
		else {
			return Matrix.identity(3, 3);
		}
	}
	
	public static double[] intersect(double[] p0, double[] p1, double[] q0, double[] q1) {
		double[] intersectionPoint = new double[2];
		double[] params = getIntersectionParams(p0[0], p0[1], p1[0], p1[1], q0[0], q0[1], q1[0], q1[1]);
		if (Math.abs(params[0]) < ZERO) {
			intersectionPoint[0] = (p0[0]+q0[0])*0.5;
			intersectionPoint[1] = (p0[1]+q0[1])*0.5;
		}
		else {
			intersectionPoint[0] = p0[0]+(p1[0]-p0[0])*params[1];
			intersectionPoint[1] = p0[1]+(p1[1]-p0[1])*params[1];
		}
		return intersectionPoint;
	}

	public static boolean doLineSegmentsIntersect(double[] p0, double[] p1, double[] q0, double[] q1) {
		double[] params = getIntersectionParams(p0[0], p0[1], p1[0], p1[1], q0[0], q0[1], q1[0], q1[1]);
		if (Math.abs(params[0]) < ZERO || params[1] < 0.0 || params[1] > 1.0 ||
				params[2] < 0.0 || params[2] > 1.0) return false;
		return true;
	}
	
	private static double[] getIntersectionParams(double x1, double y1, double x2, double y2,
			double x3, double y3, double x4, double y4) {
		double[] params = new double[3];
		/* denominator */
		params[0] = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
		/* ua */
		params[1] = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / params[0];
		/* ub */
		params[2] = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / params[0];
		return params;
	}
	
	public static double distancePointToLineSegment(double[] point, double[] p0, double[] p1) {
		double[] point_p0 = new double[3];
		point_p0[0] = p0[0]-point[0];
		point_p0[1] = p0[1]-point[1];
		point_p0[2] = p0[2]-point[2];
		double[] point_p1 = new double[3];
		point_p1[0] = p1[0]-point[0];
		point_p1[1] = p1[1]-point[1];
		point_p1[2] = p1[2]-point[2];
		double dist = Math.min(Math.sqrt(point_p0[0]*point_p0[0]+point_p0[1]*point_p0[1]+point_p0[2]*point_p0[2]),
							   Math.sqrt(point_p1[0]*point_p1[0]+point_p1[1]*point_p1[1]+point_p1[2]*point_p1[2]));
		double[] lineVec = new double[3];
		lineVec[0] = p1[0]-p0[0];
		lineVec[1] = p1[1]-p0[1];
		lineVec[2] = p1[2]-p0[2];
		if (point_p0[0]*lineVec[0]+point_p0[1]*lineVec[1]+point_p0[2]*lineVec[2] < 0.0 &&
			point_p1[0]*lineVec[0]+point_p1[1]*lineVec[1]+point_p1[2]*lineVec[2] > 0.0) {
			dist = normalDistancePointToLineSegment(point, p0, p1);
		}
		return dist;
	}
	
	public static double normalDistancePointToLineSegment(double[] point, double[] p0, double[] p1) {
		double[] lineVec = new double[3];
		lineVec[0] = p1[0]-p0[0];
		lineVec[1] = p1[1]-p0[1];
		lineVec[2] = p1[2]-p0[2];
		double length = Math.sqrt(lineVec[0]*lineVec[0]+lineVec[1]*lineVec[1]+lineVec[2]*lineVec[2]);
		lineVec[0] /= length;
		lineVec[1] /= length;
		lineVec[2] /= length;
		double scal = (point[0]-p0[0])*lineVec[0]+(point[1]-p0[1])*lineVec[1]+(point[2]-p0[2])*lineVec[2];
		double[] diff = new double[3];
		diff[0] = point[0]-p0[0]-scal*lineVec[0];
		diff[1] = point[1]-p0[1]-scal*lineVec[1];
		diff[2] = point[2]-p0[2]-scal*lineVec[2];
		return Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
	}
	
	public static double anglePointToLineSegmentNormal(double[] point, double[] p0, double[] p1) {
		double[] point_p0 = new double[3];
		point_p0[0] = p0[0]-point[0];
		point_p0[1] = p0[1]-point[1];
		point_p0[2] = p0[2]-point[2];
		double[] point_p1 = new double[3];
		point_p1[0] = p1[0]-point[0];
		point_p1[1] = p1[1]-point[1];
		point_p1[2] = p1[2]-point[2];
		double[] lineVec = new double[3];
		lineVec[0] = p1[0]-p0[0];
		lineVec[1] = p1[1]-p0[1];
		lineVec[2] = p1[2]-p0[2];
		double length = Math.sqrt(lineVec[0]*lineVec[0]+lineVec[1]*lineVec[1]+lineVec[2]*lineVec[2]);
		lineVec[0] /= length;
		lineVec[1] /= length;
		lineVec[2] /= length;
		if (point_p0[0]*lineVec[0]+point_p0[1]*lineVec[1]+point_p0[2]*lineVec[2] > 0.0) {
			length = Math.sqrt(point_p0[0]*point_p0[0]+point_p0[1]*point_p0[1]+point_p0[2]*point_p0[2]);
			return Math.acos((lineVec[0]*point_p0[0]+lineVec[1]*point_p0[1]+lineVec[2]*point_p0[2])/(-length))-Math.PI/2.0;
		}
		else if (point_p1[0]*lineVec[0]+point_p1[1]*lineVec[1]+point_p1[2]*lineVec[2] < 0.0) {
			length = Math.sqrt(point_p1[0]*point_p1[0]+point_p1[1]*point_p1[1]+point_p1[2]*point_p1[2]);
			return Math.acos((lineVec[0]*point_p1[0]+lineVec[1]*point_p1[1]+lineVec[2]*point_p1[2])/length)-Math.PI/2.0;
		}
		else {
			return 0.0;
		}
	}
	
	public static double getAreaOfPolygon(double[][] points) {
		double area = points[points.length-1][0]*points[0][1]-
				      points[points.length-1][1]*points[0][0];			
		for (int i = 0; i < points.length-1; i++) {
			area += points[i][0]*points[i+1][1]-
					points[i][1]*points[i+1][0];
		}
		return Math.abs(area)/2.0;
	}
	
	public static boolean isPointInTriangle(double[] p1, double[] p2, double[] p3, double[] pointIn, double[] pointOut)
	{
		double tol = SimLive.ZERO_TOL;
		double denominator = ((p2[1]-p3[1])*(p1[0]-p3[0])+(p3[0]-p2[0])*(p1[1]-p3[1]));
	 	double a = ((p2[1]-p3[1])*(pointIn[0]-p3[0])+(p3[0]-p2[0])*(pointIn[1]-p3[1]))/denominator;
	 	double b = ((p3[1]-p1[1])*(pointIn[0]-p3[0])+(p1[0]-p3[0])*(pointIn[1]-p3[1]))/denominator;
	 	double c = 1.0-a-b;
	 	
	 	if (pointOut != null) {
	 		pointOut[0] = pointIn[0];
	 		pointOut[1] = pointIn[1];
	 		pointOut[2] = a*p1[2]+b*p2[2]+c*p3[2];
	 	}
	 
	 	if (-tol <= a && a <= 1.0+tol && -tol <= b && b <= 1.0+tol && -tol <= c && c <= 1.0+tol) {
	 		return true;
	 	}
	 	return false;
	}
	
	public static boolean isPointInTriangle3d(double[] p1, double[] p2, double[] p3, double[] pointIn)
	{
		Matrix[] p = new Matrix[3];
		p[0] = new Matrix(p1, 3);
		p[1] = new Matrix(p2, 3);
		p[2] = new Matrix(p3, 3);
		Matrix q = new Matrix(pointIn, 3);
		Matrix nOld = null;
		for (int i = 0; i < 3; i++) {
			Matrix n = (p[(i+1)%3].minus(p[i])).crossProduct(q.minus(p[i]));
			if ((nOld != null && nOld.dotProduct(n) <= 0) || !Double.isFinite(n.get(0, 0))) {
				return false;
			}
			nOld = n;
		}
		return true;
	}
	
	public static boolean isPointInConvexPolygon(float[][] p, float[] pointIn)
	{
		if (p == null || pointIn == null) return false;
		for (int i = 0; i < p.length; i++) {
			float[] p0 = p[i];
			float[] p1 = p[(i+1)%p.length];
			float cp = (p1[0]-p0[0])*(pointIn[1]-p0[1])-(p1[1]-p0[1])*(pointIn[0]-p0[0]);
			if (cp < 0.0) return false;
		}
		return true;
	}
	
	public static double[] getIntersectionLinePlane(double[] linePoint, double[] lineDir,
			double[] A, double[] normal)
	{
		Matrix a = new Matrix(A, 3);
		Matrix lp = new Matrix(linePoint, 3);
		Matrix ld = new Matrix(lineDir, 3);
		Matrix n = new Matrix(normal, 3);
		double det = -ld.dotProduct(n);
		if (Math.abs(det) > SimLive.ZERO_TOL) {
			Matrix ao = lp.minus(a);
			double t = ao.dotProduct(n)/det;
			return lp.plus(ld.times(t)).getColumnPackedCopy();
		}
		else {
			return linePoint;
		}
	}
	
	public static double[] getIntersectionLinePlane(double[] linePoint, double[] lineDir,
			double[] A, double[] B, double[] C)
	{
		Matrix a = new Matrix(A, 3);
		Matrix b = new Matrix(B, 3);
		Matrix c = new Matrix(C, 3);
		Matrix e1 = b.minus(a);
		Matrix e2 = c.minus(a);
		Matrix n = e1.crossProduct(e2);
		return getIntersectionLinePlane(linePoint, lineDir, A, n.getColumnPackedCopy());
	}
	
	public static double[][] getIntersectionLineCylinder(double[] linePoint, double[] lineDir,
			double[] cylinderPoint, double[] cylinderDir, double cylinderRadius, double cylinderLength)
	{	
		Matrix R = getRotationMatrix(Math.acos(cylinderDir[2]), new double[]{cylinderDir[1], -cylinderDir[0], 0});
		Matrix lDir = new Matrix(lineDir, 3);
		double[] dir = R.times(lDir).getColumnPackedCopy();
		double denominator = dir[0]*dir[0]+dir[1]*dir[1];
		if (Math.abs(denominator) < SimLive.ZERO_TOL) return null;
		Matrix lPoint = new Matrix(linePoint, 3);
		double[] lp = R.times(lPoint.minus(new Matrix(cylinderPoint, 3))).getColumnPackedCopy();
		double p_half = (lp[0]*dir[0]+lp[1]*dir[1])/denominator;
		double q = (lp[0]*lp[0]+lp[1]*lp[1]-cylinderRadius*cylinderRadius)/denominator;
		if (p_half*p_half-q < 0) return null;
		double sqrt = Math.sqrt(p_half*p_half-q);
		double t1 = -p_half+sqrt;
		double t2 = -p_half-sqrt;
		if ((lp[2]+dir[2]*t1 < 0 && lp[2]+dir[2]*t2 < 0) ||
				(lp[2]+dir[2]*t1 > cylinderLength && lp[2]+dir[2]*t2 > cylinderLength)) return null;
		double[][] intersect = new double[2][];
		intersect[0] = lPoint.plus(lDir.times(t1)).getColumnPackedCopy();
		intersect[1] = lPoint.plus(lDir.times(t2)).getColumnPackedCopy();
		return intersect;
	}
}

package simlive.view;

import simlive.SimLive;
import simlive.misc.GeomUtility;
import simlive.misc.Units;

public class Measurement {

	public enum Type {DISTANCE, ANGLE}
	
	private Type type;
	private double[] startPoint;
	private double[] endPoint;
	private double[] midPoint;
	private double angle;
	private String[] label;
	private boolean isFinalized;
	private double[] move = new double[3];
	private float[][] polygon;
	
	public Measurement(Type type, double[] startPoint) {
		this.type = type;
		this.startPoint = startPoint;
		this.midPoint = startPoint;
	}
	
	public void toFront() {
		SimLive.view.measurements.remove(this);
		SimLive.view.measurements.add(this);		
	}
	
	public float[][] getPolygon() {
		return polygon;
	}

	public void setPolygon(float[] screenCoords, float halfWidth, float halfHeight) {
		polygon = new float[4][2];
		polygon[0][0] = screenCoords[0]-halfWidth;
		polygon[0][1] = screenCoords[1]-halfHeight;
		polygon[1][0] = screenCoords[0]+halfWidth;
		polygon[1][1] = screenCoords[1]-halfHeight;
		polygon[2][0] = screenCoords[0]+halfWidth;
		polygon[2][1] = screenCoords[1]+halfHeight;
		polygon[3][0] = screenCoords[0]-halfWidth;
		polygon[3][1] = screenCoords[1]+halfHeight;
	}
	
	public boolean isPointInside(int[] point) {
		if (isFinalized) {
			return GeomUtility.isPointInConvexPolygon(polygon, new float[]{point[0], point[1]});
		}
		return false;
	}
	
	public double[] getStartPoint() {
		return startPoint;
	}
	
	public double[] getMidPoint() {
		return midPoint;
	}
	
	public void setMidPoint(double[] midPoint) {
		this.midPoint = midPoint;
	}

	public double[] getEndPoint() {
		return endPoint;
	}
	
	public void setEndPoint(double[] endPoint, boolean showComponents) {
		this.endPoint = endPoint;
		
		if (type == Type.DISTANCE) {
			double[] dir = new double[3];
			dir[0] = endPoint[0] - startPoint[0];
			dir[1] = endPoint[1] - startPoint[1];
			dir[2] = endPoint[2] - startPoint[2];
			
			double length = Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
			
			if (showComponents) {
				label = new String[3];
				label[0] = "x: " + SimLive.double2String(dir[0]) + " "+Units.getLengthUnit();
				label[1] = "y: " + SimLive.double2String(dir[1]) + " "+Units.getLengthUnit();
				label[2] = "z: " + SimLive.double2String(dir[2]) + " "+Units.getLengthUnit();
			}
			else {
				label = new String[1];
				label[0] = SimLive.double2String(length) + " "+Units.getLengthUnit();
			}
		}
		if (type == Type.ANGLE) {
			double[] mp_sp = new double[3];
			mp_sp[0] = startPoint[0]-midPoint[0];
			mp_sp[1] = startPoint[1]-midPoint[1];
			mp_sp[2] = startPoint[2]-midPoint[2];
			double length1 = Math.sqrt(mp_sp[0]*mp_sp[0]+mp_sp[1]*mp_sp[1]+mp_sp[2]*mp_sp[2]);
			mp_sp[0] /= length1;
			mp_sp[1] /= length1;
			mp_sp[2] /= length1;
			double[] mp_ep = new double[3];
			mp_ep[0] = endPoint[0]-midPoint[0];
			mp_ep[1] = endPoint[1]-midPoint[1];
			mp_ep[2] = endPoint[2]-midPoint[2];
			double length2 = Math.sqrt(mp_ep[0]*mp_ep[0]+mp_ep[1]*mp_ep[1]+mp_ep[2]*mp_ep[2]);
			mp_ep[0] /= length2;
			mp_ep[1] /= length2;
			mp_ep[2] /= length2;
			double scal = mp_sp[0]*mp_ep[0]+mp_sp[1]*mp_ep[1]+mp_sp[2]*mp_ep[2];
			angle = Math.acos(Math.min(scal, 1.0));
			if (length1 < SimLive.ZERO_TOL || length2 < SimLive.ZERO_TOL) {
				angle = 0.0;
			}
			
			label = new String[1];
			label[0] = SimLive.double2String(Math.abs(angle)*180.0/Math.PI) + "\u00B0";
		}
	}

	public boolean isFinalized() {
		return isFinalized;
	}

	public void setFinalized(boolean isFinalized) {
		this.isFinalized = isFinalized;
	}

	public Type getType() {
		return type;
	}
	
	public double getAngle() {
		return angle;
	}

	public String[] getLabel() {
		return label;
	}

	public double[] getMove() {
		return move;
	}

	public void setMove(double[] move) {
		this.move = move;
	}

}

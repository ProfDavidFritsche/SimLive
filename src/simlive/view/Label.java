package simlive.view;
import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.misc.GeomUtility;
import simlive.misc.Snap;
import simlive.misc.Units;
import simlive.model.Element;
import simlive.model.Facet3d;
import simlive.model.LineElement;
import simlive.model.Material;
import simlive.model.Node;
import simlive.model.Part3d;
import simlive.model.PlaneElement;
import simlive.model.PointMass;
import simlive.model.Section;
import simlive.model.Set;
import simlive.model.Spring;
import simlive.model.Tri;
import simlive.postprocessing.ScalarPlot;

public class Label {

	private Element element;
	private Part3d part3d; /* Part3d */
	private Facet3d facet3d; /* Part3d */
	private double[] r; /* PlaneElement */
	private double shift; /* PlaneElement */
	private double t; /* LineElement */
	private String[] text = new String[3];
	private int[] move;
	private boolean isOnRightHandSide;
	private float[][] polygon;
	
	public Label(Part3d part3d, Facet3d facet3d, double[] p) {
		this.part3d = part3d;
		this.facet3d = facet3d;
		double[] px = new double[3];
		double[] py = new double[3];
		for (int i = 0; i < 3; i++) {
			double[] coords = View.getCoordsWithScaledDisp(part3d.getVertex(facet3d.getIndices()[i]));
			double[] screenCoords = View.modelToScreenCoordinates(coords);
			px[i] = screenCoords[0];
			py[i] = screenCoords[1];
		}
		Tri tri = new Tri();
		r = tri.getLocalFromGlobalCoordinates(p, px, py);
		this.move = new int[2];
		this.isOnRightHandSide = true;
		updateText();
	}
	
	public Label(PlaneElement element, double[] p) {
		this.element = element;
		this.r = element.getLocalFromGlobalCoordinates(p);
		double[] coords = element.getGlobalFromLocalCoordinates(r[0], r[1]);
		Matrix Rr = null;
		try {
			Rr = new Matrix(View.Rr[element.getID()]);
		}
		catch (Exception e) {
			Rr = element.getR0();
		}
		this.shift = (new Matrix(p, 3).minus(new Matrix(coords, 3)).dotProduct(Rr.getMatrix(0, 2, 2, 2)))/
				element.getThickness();
		this.move = new int[2];
		this.isOnRightHandSide = true;
		updateText();
	}
	
	public Label(LineElement element, double t) {
		this.element = element;
		this.t = t;
		this.move = new int[2];
		this.isOnRightHandSide = true;
		updateText();
	}
	
	public Label(PointMass element) {
		this.element = element;
		this.move = new int[2];
		this.isOnRightHandSide = true;
		updateText();
	}
	
	public void updateLabel(double[] screenCoords, float maxWidth, float halfHeight) {
		setRightHandSide(maxWidth, halfHeight);
		setPolygon(screenCoords, maxWidth, halfHeight);
	}
	
	public void updateText() {
		if (element != null) {
			if (SimLive.mode != Mode.RESULTS || SimLive.post.getScalarPlot() == null) {
				if (element.getType() == Element.Type.SPRING) {
					setText(element.getTypeString(), "Stiffness="+
							SimLive.double2String(((Spring) element).getStiffness())+" "+
							Units.getForceUnit()+"/"+Units.getLengthUnit(), null);
				}
				else if (element.getType() == Element.Type.POINT_MASS) {
					setText(element.getTypeString(), "Mass="+
							SimLive.double2String(((PointMass) element).getMass())+" "+
							Units.getMassUnit(), null);
				}
				else {
					String firstRow = "No material";
					String secondRow = "No section";
					String thirdRow = null;
					if (element.isMaterialIDValid(SimLive.model.getMaterials())) {
						Material material = SimLive.model.getMaterials().get(element.getMaterialID());
						firstRow = material.name;
					}
					if (element.isPlaneElement()) {
						/*if (((PlaneElement) element).getState() == PlaneElement.State.PLANE_STRESS) {
							secondRow = "Plane Stress";
						}
						else {
							secondRow = "Plane Strain";
						}*/
						double thickness = ((PlaneElement) element).getThickness();
						secondRow = "Thickness="+SimLive.double2String(thickness)+" "+Units.getLengthUnit();
					}
					if (element.isLineElement() &&
						((LineElement) element).isSectionIDValid(SimLive.model.getSections())) {
						Section section = SimLive.model.getSections().get(((LineElement) element).getSectionID());
						secondRow = section.getName();
					}
						
					setText(firstRow, secondRow, thirdRow);
				}
			}
			else {
				if (this == SimLive.post.getMinLabel() || this == SimLive.post.getMaxLabel()) {
					if (this == SimLive.post.getMinLabel()) {
						setText("Min:", getValueWithUnit(), null);
					}
					if (this == SimLive.post.getMaxLabel()) {
						setText("Max:", getValueWithUnit(), null);
					}
				}
				else {
					if (!SimLive.post.getScalarPlot().hasValue(element, SimLive.post.getPostIncrement())) {			
						setText("No Value", null, null);
					}
					else {
						setText(getValueWithUnit(), null, null);
					}
				}
			}
		}
	}
	
	private String getValueWithUnit() {
		int inc = SimLive.post.getPostIncrementID();
		ScalarPlot scalarPlot = SimLive.post.getScalarPlot();
		double value = getValue(inc);
		if (scalarPlot.getUnit().equals("")) {
			return new String(SimLive.double2String(value));
		}
		else {
			return new String(SimLive.double2String(value)+" "+scalarPlot.getUnit());
		}
	}
	
	public double getValue(int inc) {
		ScalarPlot scalarPlot = SimLive.post.getScalarPlot();
		double value = 0.0;
		if (scalarPlot.hasValue(element, SimLive.post.getSolution().getIncrement(inc))) {
			if (element.isPlaneElement()) {
				int[] elemNodes = element.getElementNodes();			
				double[] nodalValues = new double[elemNodes.length];
			    for (int i = 0; i < elemNodes.length; i++) {
			    	nodalValues[i] = scalarPlot.getValueAtNode(elemNodes[i], inc);
			    }
				double[] shapeFunctionValues = ((PlaneElement) element).getShapeFunctionValues(r[0], r[1]);
				value = ((PlaneElement) element).interpolateNodeValues(shapeFunctionValues, nodalValues);
			}
			if (element.isLineElement()) {
				LineElement lineElement = (LineElement) element;
				value = scalarPlot.getValueForLineElement(lineElement, t, inc);
			}
			if (element.getType() == Element.Type.POINT_MASS) {
				value = scalarPlot.getValueAtNode(element.getElementNodes()[0], inc);
			}
		}
		return value;
	}

	public double getTValue() {
		return t;
	}
	
	public void addToMove(int[] deltaMove) {
		this.move[0] += deltaMove[0];
		this.move[1] += deltaMove[1];
		if (polygon != null) {
			for (int i = 0; i < 5; i++) {
				polygon[i][0] += deltaMove[0];
				polygon[i][1] += deltaMove[1];
			}
		}
	}
	
	public void finalizeMove() {
		float h = Math.abs(polygon[3][1]-polygon[2][1]);
		if (Math.abs(this.move[1]) < h/2) {
			if ((this.isOnRightHandSide && this.move[0] < -Math.abs(this.move[1])) ||
				(!this.isOnRightHandSide && this.move[0] > Math.abs(this.move[1]))) {
				if (polygon != null) {
					for (int i = 0; i < 5; i++) {
						polygon[i][0] -= this.move[0];
						polygon[i][1] -= this.move[1];
					}
				}
				this.move[0] = this.move[1] = 0;
			}
		}
	}
	
	public int[] getMove() {
		return move;
	}

	public double[] getR() {
		return r;
	}
	
	public double getShift() {
		return shift;
	}
	
	public String[] getRows() {
		return text;
	}
	
	public Element getElement() {
		return element;
	}
	
	public Part3d getPart3d() {
		return part3d;
	}

	public Facet3d getFacet3d() {
		return facet3d;
	}

	public double[] getCoordinatesWithDeformation() {
		double[] coords = new double[3];
		if (element != null) {
			if (element.isPlaneElement()) {
				coords = ((PlaneElement) element).getGlobalFromLocalCoordinates(r[0], r[1]);
				if (SimLive.post == null || (this != SimLive.post.getMinLabel() && this != SimLive.post.getMaxLabel())) {
					double[][] Rr = null;
					try {
						Rr = View.Rr[element.getID()];
					}
					catch (Exception e) {
						Rr = ((PlaneElement) element).getR0().getArray();
					}
					double thickness = ((PlaneElement) element).getThickness();
					coords[0] += Rr[0][2]*shift*thickness;
					coords[1] += Rr[1][2]*shift*thickness;
					coords[2] += Rr[2][2]*shift*thickness;
				}
			}
			if (element.isLineElement()) {
				coords = ((LineElement) element).getGlobalFromLocalCoordinates(t);
			}
			if (element.getType() == Element.Type.POINT_MASS) {
				coords = View.getCoordsWithScaledDisp(element.getElementNodes()[0]);
			}
		}
		else {
			double[] px = new double[3];
			double[] py = new double[3];
			double[] pz = new double[3];
			for (int i = 0; i < 3; i++) {
				double[] vertexCoords = View.getCoordsWithScaledDisp(part3d.getVertex(facet3d.getIndices()[i]));
				px[i] = vertexCoords[0];
				py[i] = vertexCoords[1];
				pz[i] = vertexCoords[2];
			}
			Tri tri = new Tri();
			double[] shapeFunctionValues = tri.getShapeFunctionValues(r[0], r[1]);
		    coords[0] = tri.interpolateNodeValues(shapeFunctionValues, px);
		    coords[1] = tri.interpolateNodeValues(shapeFunctionValues, py);
		    coords[2] = tri.interpolateNodeValues(shapeFunctionValues, pz);
		}
		return coords;
	}

	public boolean isOnRightHandSide() {
		return isOnRightHandSide;
	}

	public void setOnRightHandSide(boolean isOnRightHandSide) {
		this.isOnRightHandSide = isOnRightHandSide;
	}
	
	public static Label getNewLabelForPart3d(int[] point, Part3d part3d, Facet3d facet3d) {		
		return new Label(part3d, facet3d, new double[]{point[0], point[1]});
	}
	
	public static Label getNewLabelForElement(Element element) {		
		if (element.getType() == Element.Type.POINT_MASS) {
			return new Label((PointMass) element);
		}
		if (element.isLineElement()) {
			double t = ((LineElement) element).getLocalFromGlobalCoordinates(Snap.coords3d);
			return new Label((LineElement) element, t);
		}
		if (element.isPlaneElement()) {
			return new Label((PlaneElement) element, Snap.coords3d);
		}
		return null;
	}
	
	public static  Label getNewLabelForNode(Node node) {		
		Set set = Snap.set;
		int nodeID = node.getID();
		for (int e = 0; e < set.getElements().size(); e++) {
			Element element = set.getElements().get(e);
			int[] element_nodes = element.getElementNodes();
			for (int i = 0; i < element_nodes.length; i++) {
				if (element_nodes[i] == nodeID) {
					return getNewLabelForElement(element);
				}
			}
		}
		return null;
	}
	
	public void toStatusBar() {
		if (text[2] != null) {
			SimLive.statusBar.setText(text[0]+" | "+text[1]+" | "+text[2]);
		}
		else if (text[1] != null) {
			SimLive.statusBar.setText(text[0]+" | "+text[1]);
		}
		else {
			SimLive.statusBar.setText(text[0]);
		}
	}

	public void toFront() {
		SimLive.view.labels.remove(this);
		SimLive.view.labels.add(this);		
	}
	
	public float[][] getPolygon() {
		return polygon;
	}

	private void setPolygon(double[] screenCoords, float maxWidth, float halfHeight) {
		polygon = new float[5][2];
		polygon[0][0] = (float) (screenCoords[0]+move[0]);
		polygon[0][1] = (float) (screenCoords[1]+move[1]);
		if (!isOnRightHandSide) {
			maxWidth = -maxWidth;
			halfHeight = -halfHeight;
		}
		polygon[1][0] = polygon[0][0]+halfHeight;
		polygon[1][1] = polygon[0][1]-halfHeight;
		polygon[2][0] = polygon[1][0]+maxWidth;
		polygon[2][1] = polygon[1][1];
		polygon[3][0] = polygon[2][0];
		polygon[3][1] = polygon[0][1]+halfHeight;
		polygon[4][0] = polygon[1][0];
		polygon[4][1] = polygon[3][1];
	}
	
	private void setRightHandSide(float maxWidth, float halfHeight) {
		if (isOnRightHandSide) {
			if (move[0] < -(maxWidth+halfHeight)/2) {
				move[0] += maxWidth+halfHeight;
				isOnRightHandSide = false;
			}
		}
		else {
			if (move[0] > (maxWidth+halfHeight)/2) {
				move[0] -= maxWidth+halfHeight;
				isOnRightHandSide = true;
			}
		}
	}
	
	public boolean isPointInside(int[] point) {
		return GeomUtility.isPointInConvexPolygon(polygon, new float[]{point[0], point[1]});
	}
	
	public void setText(String text0, String text1, String text2) {
		text[0] = text0;
		text[1] = text1;
		text[2] = text2;
	}

}

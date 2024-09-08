package simlive.model;

import simlive.SimLive;
import simlive.misc.Search;

public class Connector extends AbstractConnector implements DeepEqualsInterface {

	public enum Type {SPHERICAL, FIXED, REVOLUTE}
	private Type type;
	private double[] coordinates = new double[3];
	private boolean isCoordsSet = false;
	private Set set0;
	private Set set1;
	private Element element0;
	private Element element1;
	private double[] r0 = new double[2];
	private double[] r1 = new double[2];
	private double t0, t1;
	
	public Connector() {
		this.type = Type.SPHERICAL;
		this.name = SimLive.model.getDefaultName("Connector");
	}

	public Connector(double[] coordinates, Set set0, Set set1) {
		this.set0 = set0;
		this.set1 = set1;
		this.coordinates[0] = coordinates[0];
		this.coordinates[1] = coordinates[1];
		this.coordinates[2] = coordinates[2];
		this.isCoordsSet = true;
		this.type = Type.SPHERICAL;
		this.name = SimLive.model.getDefaultName("Connector");
	}
	
	public Connector clone(Model model) {
		Connector connector = new Connector();
		connector.coordinates = this.coordinates.clone();
		connector.isCoordsSet = this.isCoordsSet;
		if (this.set0 != null) connector.set0 = model.getSets().get(this.set0.getID());
		if (this.set1 != null) connector.set1 = model.getSets().get(this.set1.getID());
		if (this.element0 != null) connector.element0 = model.getElements().get(this.element0.getID());
		if (this.element1 != null) connector.element1 = model.getElements().get(this.element1.getID());
		connector.r0 = this.r0.clone();
		connector.r1 = this.r1.clone();
		connector.t0 = this.t0;
		connector.t1 = this.t1;
		connector.type = this.type;
		connector.name = this.name;
		return connector;
	}
	
	public boolean deepEquals(Object obj) {
		Connector connector = (Connector) obj;
		//do not check coordinates, because not necessary and possibility of wrong false
		//if (!Arrays.equals(this.coordinates, connector.coordinates)) return false;
		if (this.set0 != null) if (!this.set0.deepEquals(connector.set0)) return false;
		if (this.set1 != null) if (!this.set1.deepEquals(connector.set1)) return false;
		if (this.element0 != null) if (!this.element0.deepEquals(connector.element0)) return false;
		if (this.element1 != null) if (!this.element1.deepEquals(connector.element1)) return false;
		//check with tolerance
		if (Math.abs(this.r0[0]-connector.r0[0]) > SimLive.ZERO_TOL) return false;
		if (Math.abs(this.r0[1]-connector.r0[1]) > SimLive.ZERO_TOL) return false;
		if (Math.abs(this.r1[0]-connector.r1[0]) > SimLive.ZERO_TOL) return false;
		if (Math.abs(this.r1[1]-connector.r1[1]) > SimLive.ZERO_TOL) return false;
		if (Math.abs(this.t0-connector.t0) > SimLive.ZERO_TOL) return false;
		if (Math.abs(this.t1-connector.t1) > SimLive.ZERO_TOL) return false;
		if (this.type != connector.type) return false;
		if (this.name != connector.name) return false;
		return true;
	}
	
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public double[] getCoordinates() {
		return this.coordinates.clone();
	}
	
	public void setCoordinates(double[] coordinates, boolean newSearch) {
		if (set0 != null && set1 != null) {
			this.coordinates[0] = coordinates[0];
			this.coordinates[1] = coordinates[1];
			this.coordinates[2] = coordinates[2];
			this.isCoordsSet = true;
			if (newSearch) {
				newSearch();
			}
		}
	}
	
	public boolean isCoordsSet() {
		return isCoordsSet;
	}

	public void setCoordsSets(boolean isCoordsSets) {
		this.isCoordsSet = isCoordsSets;
	}

	public double[] getDisp(boolean forElement0) {
		if (forElement0) {
			if (element0.isPlaneElement()) {
				return ((PlaneElement) element0).getDispAtLocalCoordinates(r0[0], r0[1]);
			}
			else {
				return ((LineElement) element0).getDispAtLocalCoordinates(t0);
			}			
		}
		else {
			if (element1.isPlaneElement()) {
				return ((PlaneElement) element1).getDispAtLocalCoordinates(r1[0], r1[1]);
			}
			else {
				return ((LineElement) element1).getDispAtLocalCoordinates(t1);
			}			
		}
	}
	
	private double[] getGlobalFromLocalCoordinates(boolean forElement0) {
		if (forElement0) {
			if (element0.isPlaneElement()) {
				return ((PlaneElement) element0).getGlobalFromLocalCoordinates(r0[0], r0[1]);
			}
			else {
				return ((LineElement) element0).getGlobalFromLocalCoordinates(t0);
			}			
		}
		else {
			if (element1.isPlaneElement()) {
				return ((PlaneElement) element1).getGlobalFromLocalCoordinates(r1[0], r1[1]);
			}
			else {
				return ((LineElement) element1).getGlobalFromLocalCoordinates(t1);
			}			
		}
	}
	
	private void newSearch() {
		if (set0 != null && set1 != null) {
			Element[] temp = Search.getElementsAtConnector(coordinates, set0, set1);
			element0 = temp[0];
			element1 = temp[1];
			if (this.type == Type.REVOLUTE &&
				(element0.getType() != Element.Type.BEAM || element1.getType() != Element.Type.BEAM)) {
				this.type = Type.SPHERICAL;
			}
			if (this.type == Type.FIXED &&
				(element0.getType() == Element.Type.ROD || element0.getType() == Element.Type.SPRING ||
				 element1.getType() == Element.Type.ROD || element1.getType() == Element.Type.SPRING)) {
				this.type = Type.SPHERICAL;
			}
			updateLocalCoordinates();
		}
	}
	
	public boolean updateCoordinates() {
		if (element0 != null && element1 != null &&
			SimLive.model.getElements().contains(element0) && SimLive.model.getElements().contains(element1)) {
			double[] coords0 = getGlobalFromLocalCoordinates(true);
			double[] coords1 = getGlobalFromLocalCoordinates(false);
			if (Math.sqrt((coords0[0]-coords1[0])*(coords0[0]-coords1[0])+(coords0[1]-coords1[1])*(coords0[1]-coords1[1])+
					(coords0[2]-coords1[2])*(coords0[2]-coords1[2])) < SimLive.ZERO_TOL) {
				coordinates[0] = coords0[0];
				coordinates[1] = coords0[1];
				coordinates[2] = coords0[2];
				return true;
			}
		}
		return false;
	}
	
	public void update() {
		if (isCoordsSet && SimLive.mode != SimLive.Mode.RESULTS) {
			if (set0 != null && set1 != null) {
				if (!SimLive.model.getSets().contains(set0) || !SimLive.model.getSets().contains(set1)) {
					SimLive.model.getConnectors().remove(this);
				}
				else {
					if (!updateCoordinates()) {
						newSearch();
					}
				}
			}
		}
	}
	
	public Set getSet0() {
		return set0;
	}

	public Set getSet1() {
		return set1;
	}

	public void setSets(Set set0, Set set1) {
		this.set0 = set0;
		this.set1 = set1;
		newSearch();
	}

	public Element getElement0() {
		return element0;
	}

	public Element getElement1() {
		return element1;
	}

	public double[] getR0() {
		return r0;
	}

	public double[] getR1() {
		return r1;
	}

	public double getT0() {
		return t0;
	}

	public double getT1() {
		return t1;
	}
	
	private void updateLocalCoordinates() {
		if (element0.isPlaneElement()) {
			r0 = ((PlaneElement) element0).getLocalFromGlobalCoordinates(coordinates);
		}
		else {
			t0 = ((LineElement) element0).getLocalFromGlobalCoordinates(coordinates);
		}		
		if (element1.isPlaneElement()) {
			r1 = ((PlaneElement) element1).getLocalFromGlobalCoordinates(coordinates);
		}
		else {
			t1 = ((LineElement) element1).getLocalFromGlobalCoordinates(coordinates);
		}
	}

	@Override
	public ConnectorType getConnectorType() {
		return ConnectorType.CONNECTOR;
	}

}

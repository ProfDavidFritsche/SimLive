package simlive.model;
import java.util.ArrayList;
import Jama.Matrix;
import simlive.SimLive;
import simlive.solution.Solution;

public abstract class Element implements DeepEqualsInterface {

	public enum Type {ROD, SPRING, BEAM, TRI, QUAD, RECTANGULAR, CIRCULAR, TRIANGULAR, SPUR_GEAR, POINT_MASS}
	protected int[] elementNodes;
	protected int material_id;
	protected Matrix I;
	protected int id;
	protected double stiffnessDamping;
	protected double massDamping;
	public Matrix M_elem; //solution speed-up
	
	public Element() {
		material_id = -1;
		id = SimLive.model.getElements().size();
	}
	
	public int[] getElementNodes() {
		return elementNodes;
	}
	
	public void setElementNodes(int[] elementNodes) {
		this.elementNodes = elementNodes;
	}

	public abstract Element clone();
	
	public abstract boolean deepEquals(Object obj);
	
	public abstract boolean isLineElement();
	
	public abstract boolean isPlaneElement();
	
	public abstract void adaptNodeIDs(int deleteNodeID);
	
	public void setMaterialID(int mat_id) {
		material_id = mat_id;
	}
	
	public int getMaterialID() {
		return material_id;
	}

	public boolean isMaterialIDValid(ArrayList<Material> materials) {
		if (material_id < 0 || material_id > materials.size() - 1) return false;
		return true;
	}
	
	public abstract Element.Type getType();
	
	public abstract String getTypeString();
	
	public abstract Matrix getElementStiffness(ArrayList<Material> materials, ArrayList<Section> sections,
			   						           ArrayList<Node> nodes);
	
	public abstract Matrix getElementStiffnessNL(ArrayList<Material> materials, ArrayList<Section> sections,
	           									 ArrayList<Node> nodes, Matrix u_global);
	
	public abstract Matrix getElementForce(ArrayList<Material> materials, ArrayList<Section> sections,
		     					   ArrayList<Node> nodes, Matrix u_global, boolean localSys);

	public abstract Matrix getElementForceNL(ArrayList<Material> materials, ArrayList<Section> sections,
            					     ArrayList<Node> nodes, Matrix u_global, boolean localSys);
	
	public abstract void setIndexIncidence(Solution solution, ArrayList<Node> nodes);
	
	public abstract String[] getLocalDofNames();
	
	public abstract ArrayList<Element> refine(ArrayList<Node> nodes, ArrayList<Element> elements);
	
	public Matrix addLocalToGlobalMatrix(Matrix localMatrix, Matrix globalMatrix) {
		for (int r = 0; r < localMatrix.getRowDimension(); r++) {
			for (int c = 0; c < localMatrix.getColumnDimension(); c++) {
				int r_global = (int) I.get(r, 0);
				int c_global = 0;
				if (localMatrix.getColumnDimension() > 1) {
					c_global = (int) I.get(c, 0);
				}
				globalMatrix.set(r_global, c_global,
						globalMatrix.get(r_global, c_global) + localMatrix.get(r, c));
			}
		}
		return globalMatrix;
	}
	
	public Matrix globalToLocalVector(Matrix globalVector) {
		Matrix localVector = new Matrix(I.getRowDimension(), 1);
		for (int r = 0; r < I.getRowDimension(); r++) {
			int r_global = (int) I.get(r, 0);
			localVector.set(r, 0, globalVector.get(r_global, 0));
		}
		return localVector;
	}
	
	public int getID() {
		return id;
	}
	
	public double getStiffnessDamping() {
		return stiffnessDamping;
	}

	public void setStiffnessDamping(double stiffnessDamping) {
		this.stiffnessDamping = stiffnessDamping;
	}
	
	public double getMassDamping() {
		return massDamping;
	}

	public void setMassDamping(double massDamping) {
		this.massDamping = massDamping;
	}
	
	protected abstract Matrix getMelem(ArrayList<Material> materials, ArrayList<Section> sections, ArrayList<Node> nodes);
	
	public void initMelem(ArrayList<Material> materials, ArrayList<Section> sections, ArrayList<Node> nodes) {
		M_elem = getMelem(materials, sections, nodes);
	}

	public abstract void update();
	
	protected void updateIDAndMaterial() {
		/* ID */
		id = SimLive.model.getElements().indexOf(this);
		
		/* assign material */
		if (!isMaterialIDValid(SimLive.model.getMaterials())) {
			if (SimLive.model.getMaterials().size() > 0) {							
				material_id = 0;
			}
		}
		
		/* rotationalDOF */
		if (getType() == Element.Type.BEAM || isPlaneElement()) {
			for (int i = 0; i < elementNodes.length; i++) {
				SimLive.model.getNodes().get(elementNodes[i]).setRotationalDOF(true);
			}
		}
	}
}

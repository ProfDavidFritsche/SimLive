package simlive.model;

public abstract class AbstractLoad {
	public enum LoadType {LOAD, DISTRIBUTED_LOAD}	
	public abstract LoadType getLoadType();
	public boolean isShifted; /* only for display */
	public String name;
	protected Node referenceNode;
	
	public Node getReferenceNode() {
		return referenceNode;
	}
	
	public void setReferenceNode(Node referenceNode) {
		this.referenceNode = referenceNode;
	}
}

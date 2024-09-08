package simlive.model;

public abstract class AbstractConnector {	
	public enum ConnectorType {CONNECTOR, CONNECTOR_3D}		
	public abstract ConnectorType getConnectorType();
	public String name;
}

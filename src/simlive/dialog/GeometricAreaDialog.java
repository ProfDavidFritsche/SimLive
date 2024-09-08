package simlive.dialog;

import org.eclipse.swt.widgets.Composite;

import simlive.model.Node;

public abstract class GeometricAreaDialog extends Composite {

	public GeometricAreaDialog(Composite parent, int style) {
		super(parent, style);
	}
	
	public abstract void updateDialog(Node edgePoint);
}

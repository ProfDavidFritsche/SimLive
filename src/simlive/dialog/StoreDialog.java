package simlive.dialog;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import simlive.SimLive;

public abstract class StoreDialog extends Composite {

	public StoreDialog(Composite arg0, int arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	protected Label[] lblOK = new Label[2];
	
	public void setOKLabel(int index, boolean done) {
		lblOK[index].setImage(SimLive.resize(SimLive.OK_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		lblOK[index].setVisible(done);
		lblOK[index].getParent().layout();
	}

}

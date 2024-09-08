package simlive.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import simlive.SimLive;
import simlive.model.Connector3d;

import org.eclipse.wb.swt.SWTResourceManager;

public class Connector3dDialog extends StoreDialog {
	
	public Connector3dDialog(Composite parent, int style, final Connector3d connector3d) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText(connector3d.name);
			
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout1 = new GridLayout(4, false);
		SimLive.formatGridLayoutForComposite(gridLayout1);
		composite.setLayout(gridLayout1);
		
		Label lblIcon = new Label(composite, SWT.NONE);
		lblIcon.setImage(SimLive.resize(SimLive.INFO_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		
		Label lblStore = new Label(composite, SWT.NONE);
		lblStore.setFont(SimLive.FONT_BOLD);
		lblStore.setText("Store");
		
		Label lbl3dParts = new Label(composite, SWT.NONE);
		lbl3dParts.setFont(SimLive.FONT_BOLD);
		lbl3dParts.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		lbl3dParts.setText("3D-Parts");
		
		lblOK[0] = new Label(composite, SWT.NONE);
		lblOK[0].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(0, !connector3d.getParts3d().isEmpty());
		
		Composite composite1 = new Composite(this, SWT.NONE);
		composite1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout2 = new GridLayout(4, false);
		SimLive.formatGridLayoutForComposite(gridLayout2);
		composite1.setLayout(gridLayout2);
		
		Label lblIcon_1 = new Label(composite1, SWT.NONE);
		lblIcon_1.setImage(SimLive.resize(SimLive.INFO_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		
		Label lblStore_1 = new Label(composite1, SWT.NONE);
		lblStore_1.setFont(SimLive.FONT_BOLD);
		lblStore_1.setText("Store");
		
		Label lblParts = new Label(composite1, SWT.NONE);
		lblParts.setFont(SimLive.FONT_BOLD);
		lblParts.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		lblParts.setText("Parts");
		
		lblOK[1] = new Label(composite1, SWT.NONE);
		lblOK[1].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(1, !connector3d.getParts().isEmpty());
	}

}

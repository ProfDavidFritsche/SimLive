package simlive.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.ContactPair;
import simlive.model.ContactPair.Type;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;

public class ContactDialog extends StoreDialog {
	private Text text;
	private Text text_1;
	
	public ContactDialog(Composite parent, int style, final ContactPair contactPair) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText(contactPair.name);
			
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
		
		Label lblSlaveNodes = new Label(composite, SWT.NONE);
		lblSlaveNodes.setFont(SimLive.FONT_BOLD);
		lblSlaveNodes.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		lblSlaveNodes.setText("Slave Nodes");
		
		lblOK[0] = new Label(composite, SWT.NONE);
		lblOK[0].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(0, !contactPair.getSlaveNodes().isEmpty());
		
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
		
		Label lblMasterParts = new Label(composite1, SWT.NONE);
		lblMasterParts.setFont(SimLive.FONT_BOLD);
		lblMasterParts.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		lblMasterParts.setText("Master Parts");
		
		lblOK[1] = new Label(composite1, SWT.NONE);
		lblOK[1].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(1, !contactPair.getMasterSets().isEmpty());
		
		Label lblProperties = new Label(this, SWT.NONE);
		lblProperties.setText("Properties");
		lblProperties.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblProperties.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblProperties.setFont(SimLive.FONT_BOLD);
		
		Label lblType = new Label(this, SWT.NONE);
		lblType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblType.setText("Type:");
		
		Combo combo = new Combo(this, SWT.READ_ONLY);
		combo.setItems(new String[] {"Deformable-Deformable", "Rigid-Deformable"});
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				contactPair.setType(Type.values()[combo.getSelectionIndex()], true);
				SimLive.model.updateModel();				
				SimLive.view.redraw();
			}
		});
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		combo.select(contactPair.getType().ordinal());
		new Label(this, SWT.NONE);
		
		final Button btnSwitchContactSide = new Button(this, SWT.CHECK);
		btnSwitchContactSide.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				contactPair.setSwitchContactSide(btnSwitchContactSide.getSelection());
				SimLive.view.redraw();
			}
		});
		btnSwitchContactSide.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnSwitchContactSide.setText("Switch Contact Side");
		btnSwitchContactSide.setSelection(contactPair.isSwitchContactSide());
		new Label(this, SWT.NONE);
		
		final Button btnNoSeparation = new Button(this, SWT.CHECK);
		btnNoSeparation.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				contactPair.setNoSeparation(btnNoSeparation.getSelection());
			}
		});
		btnNoSeparation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnNoSeparation.setText("No Separation");
		btnNoSeparation.setSelection(contactPair.isNoSeparation());
		new Label(this, SWT.NONE);
		
		final Button btnSetMaximumPenetration = new Button(this, SWT.CHECK);
		btnSetMaximumPenetration.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				contactPair.setMaxPenetration(btnSetMaximumPenetration.getSelection());
				text.setEnabled(contactPair.isMaxPenetration());
				if (contactPair.isMaxPenetration()) {
					text.setText(SimLive.double2String(contactPair.getMaxPenetration()));
				}
				else {
					text.setText("");
				}
			}
		});
		btnSetMaximumPenetration.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnSetMaximumPenetration.setText("Set Maximum Penetration");
		btnSetMaximumPenetration.setSelection(contactPair.isMaxPenetration());
		
		Label lblValue = new Label(this, SWT.NONE);
		lblValue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblValue.setText("Value:");
		
		text = new Text(this, SWT.BORDER);
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				text.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				contactPair.setMaxPenetration(SimLive.getInputDouble(text));
			}
		});
		if (contactPair.isMaxPenetration()) {
			text.setText(SimLive.double2String(contactPair.getMaxPenetration()));
		}
		text.setEnabled(contactPair.isMaxPenetration());
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setText(Units.getLengthUnit());
		
		Label lblFriction = new Label(this, SWT.NONE);
		lblFriction.setText("Friction");
		lblFriction.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblFriction.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblFriction.setFont(SimLive.FONT_BOLD);
		
		Label lblCoefficient = new Label(this, SWT.NONE);
		lblCoefficient.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblCoefficient.setText("Coefficient:");
		
		text_1 = new Text(this, SWT.BORDER);
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				contactPair.setFrictionCoefficient(SimLive.getInputDouble(text_1));
			}
		});
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_1.setText(SimLive.double2String(contactPair.getFrictionCoefficient()));
		new Label(this, SWT.NONE);
	}

}

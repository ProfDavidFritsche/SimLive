package simlive.dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.DistributedLoad;
import simlive.model.Model;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class DistributedLoadDialog extends StoreDialog {
	private Text text_5;
	private Text text_6;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public DistributedLoadDialog(Composite parent, int style, final DistributedLoad load) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
			
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText(load.name);
			
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		SimLive.formatGridLayoutForComposite(gl_composite);
		composite.setLayout(gl_composite);
		
		Label lblIcon = new Label(composite, SWT.NONE);
		lblIcon.setImage(SimLive.resize(SimLive.INFO_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		
		Label lblStoreBeams = new Label(composite, SWT.NONE);
		lblStoreBeams.setFont(SimLive.FONT_BOLD);
		lblStoreBeams.setText("Store Beams");
		
		lblOK[0] = new Label(composite, SWT.NONE);
		lblOK[0].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(0, !load.getElementSets().isEmpty());
		
		Label lblProperties = new Label(this, SWT.NONE);
		lblProperties.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblProperties.setFont(SimLive.FONT_BOLD);
		lblProperties.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblProperties.setText("Properties");
		new Label(this, SWT.NONE);
		
		Label lblStart = new Label(this, SWT.NONE);
		lblStart.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblStart.setText("Start:");
		
		Label lblEnd = new Label(this, SWT.NONE);
		lblEnd.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblEnd.setText("End:");
		new Label(this, SWT.NONE);
		
		Label lblxValue = new Label(this, SWT.NONE);
		lblxValue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblxValue.setText("x-Value:");
		
		final Text text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setStartValue(0, SimLive.getInputDouble(text));
				SimLive.view.redraw();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text.setText(SimLive.double2String(load.getStartValue(0)));
		
		final Text text_2 = new Text(this, SWT.BORDER);
		text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_2.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_2.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setEndValue(0, SimLive.getInputDouble(text_2));
				SimLive.view.redraw();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_2.setText(SimLive.double2String(load.getEndValue(0)));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getForceUnit()+"/"+Units.getLengthUnit());
		
		Label lblyValue = new Label(this, SWT.NONE);
		lblyValue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblyValue.setText("y-Value:");
		
		final Text text_1 = new Text(this, SWT.BORDER);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setStartValue(1, SimLive.getInputDouble(text_1));
				SimLive.view.redraw();
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_1.setText(SimLive.double2String(load.getStartValue(1)));
		
		final Text text_3 = new Text(this, SWT.BORDER);
		text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_3.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_3.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setEndValue(1, SimLive.getInputDouble(text_3));
				SimLive.view.redraw();
			}
		});
		text_3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_3.setText(SimLive.double2String(load.getEndValue(1)));
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getForceUnit()+"/"+Units.getLengthUnit());
		
		Label lblZvalue = new Label(this, SWT.NONE);
		lblZvalue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblZvalue.setText("z-Value:");
		
		text_5 = new Text(this, SWT.BORDER);
		text_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_5.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_5.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setStartValue(2, SimLive.getInputDouble(text_5));
				SimLive.view.redraw();
			}
		});
		text_5.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_5.setText(SimLive.double2String(load.getStartValue(2)));
		text_5.setEnabled(!Model.twoDimensional);
		
		text_6 = new Text(this, SWT.BORDER);
		text_6.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_6.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_6.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_6.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setEndValue(2, SimLive.getInputDouble(text_6));
				SimLive.view.redraw();
			}
		});
		text_6.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_6.setText(SimLive.double2String(load.getEndValue(2)));
		text_6.setEnabled(!Model.twoDimensional);
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText(Units.getForceUnit()+"/"+Units.getLengthUnit());
		
		Label lblCoordinateSystem = new Label(this, SWT.NONE);
		lblCoordinateSystem.setText("Coordinate System");
		lblCoordinateSystem.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblCoordinateSystem.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblCoordinateSystem.setFont(SimLive.FONT_BOLD);
		
		/*Composite composite_2 = new Composite(this, SWT.NONE);
		composite_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		GridLayout gl_composite_1 = new GridLayout(2, false);
		Sim2d.formatGridLayoutForComposite(gl_composite_1);
		composite_2.setLayout(gl_composite_1);
		
		Label lblIcon_1 = new Label(composite_2, SWT.NONE);
		lblIcon_1.setImage(Sim2d.resize(getDisplay().getSystemImage(SWT.ICON_INFORMATION), Sim2d.ICON_HEIGHT_FACTORS[2]));
		
		Label lblSelectReferenceNode = new Label(composite_2, SWT.NONE);
		lblSelectReferenceNode.setFont(Sim2d.FONT_BOLD);
		lblSelectReferenceNode.setText("Select Reference Node");
		
		Button btnStore_1 = new Button(this, SWT.NONE);
		btnStore_1.setToolTipText("Store Selected Node");
		btnStore_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				load.setReferenceNode(Sim2d.view.getSelectedNodes().get(0));
				Sim2d.view.deselectAll();
				Sim2d.view.redraw();
			}
		});
		btnStore_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnStore_1.setText("Store");
		btnStore_1.setEnabled(false);*/
		
		Label lblAxis = new Label(this, SWT.NONE);
		lblAxis.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAxis.setText("Axis:");
		
		Text text1 = new Text(this, SWT.BORDER);
		text1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setAxis(SimLive.getInputDouble(text1), 0);
				SimLive.view.redraw();
			}
		});
		text1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text1.setText(SimLive.double2String(load.getAxis()[0]));
		text1.setEnabled(!Model.twoDimensional);
		
		Text text_11 = new Text(this, SWT.BORDER);
		text_11.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_11.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_11.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_11.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setAxis(SimLive.getInputDouble(text_11), 1);
				SimLive.view.redraw();
			}
		});
		text_11.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_11.setText(SimLive.double2String(load.getAxis()[1]));
		text_11.setEnabled(!Model.twoDimensional);
		
		Text text_21 = new Text(this, SWT.BORDER);
		text_21.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_21.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_21.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_21.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setAxis(SimLive.getInputDouble(text_21), 2);
				SimLive.view.redraw();
			}
		});
		text_21.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		if (Model.twoDimensional) {
			load.setAxis(1, 2);
		}
		text_21.setText(SimLive.double2String(load.getAxis()[2]));
		text_21.setEnabled(!Model.twoDimensional);
		
		Label lblAngle = new Label(this, SWT.NONE);
		lblAngle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAngle.setText("Angle:");
		
		final Text text_31 = new Text(this, SWT.BORDER);
		text_31.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_31.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_31.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_31.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setAngle(SimLive.getInputDouble(text_31, -360, 360));
				SimLive.view.redraw();
			}
		});
		text_31.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_31.setText(SimLive.double2String(load.getAngle()));
		
		Label lblUnit1 = new Label(this, SWT.NONE);
		lblUnit1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit1.setText("\u00B0");
		
		new Label(this, SWT.NONE);
		
		final Button btnAlignWithBeam = new Button(this, SWT.CHECK);
		btnAlignWithBeam.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				load.setLocalSysAligned(btnAlignWithBeam.getSelection());
				text1.setEnabled(!btnAlignWithBeam.getSelection() && !Model.twoDimensional);
		 		text_11.setEnabled(!btnAlignWithBeam.getSelection() && !Model.twoDimensional);
		 		text_21.setEnabled(!btnAlignWithBeam.getSelection() && !Model.twoDimensional);
		 		text_31.setEnabled(!btnAlignWithBeam.getSelection());
				if (btnAlignWithBeam.getSelection()) {
					text1.setText("");
					text_11.setText("");
					text_21.setText("");
					text_31.setText("");
				}
				else {
					text1.setText(SimLive.double2String(load.getAxis()[0]));
					text_11.setText(SimLive.double2String(load.getAxis()[1]));
					text_21.setText(SimLive.double2String(load.getAxis()[2]));
					text_31.setText(SimLive.double2String(load.getAngle()));
				}
				SimLive.view.redraw();
			}
		});
 		btnAlignWithBeam.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
 		btnAlignWithBeam.setText("Align with Beam");
 		btnAlignWithBeam.setSelection(load.isLocalSysAligned());
 		
 		text1.setEnabled(!btnAlignWithBeam.getSelection() && !Model.twoDimensional);
 		text_11.setEnabled(!btnAlignWithBeam.getSelection() && !Model.twoDimensional);
 		text_21.setEnabled(!btnAlignWithBeam.getSelection() && !Model.twoDimensional);
 		text_31.setEnabled(!btnAlignWithBeam.getSelection());
		if (btnAlignWithBeam.getSelection()) {
			text1.setText("");
			text_11.setText("");
			text_21.setText("");
			text_31.setText("");
		}
		else {
			text1.setText(SimLive.double2String(load.getAxis()[0]));
			text_11.setText(SimLive.double2String(load.getAxis()[1]));
			text_21.setText(SimLive.double2String(load.getAxis()[2]));
			text_31.setText(SimLive.double2String(load.getAngle()));
		}
		
		TimeDependencyDialog timeDependencyDialog = new TimeDependencyDialog(this, SWT.NONE, load.getTimeTable());
		timeDependencyDialog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

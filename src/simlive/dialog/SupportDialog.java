package simlive.dialog;
import org.eclipse.swt.SWT;
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
import simlive.model.Model;
import simlive.model.Support;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class SupportDialog extends StoreDialog {
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public SupportDialog(final Composite parent, int style, final Support support) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText(support.name);
		
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		SimLive.formatGridLayoutForComposite(gl_composite);
		composite.setLayout(gl_composite);
		
		Label lblIcon = new Label(composite, SWT.NONE);
		lblIcon.setImage(SimLive.resize(SimLive.INFO_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		
		Label lblStoreNodes = new Label(composite, SWT.NONE);
		lblStoreNodes.setFont(SimLive.FONT_BOLD);
		lblStoreNodes.setText("Store Nodes");
		
		lblOK[0] = new Label(composite, SWT.NONE);
		lblOK[0].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(0, !support.getNodes().isEmpty());
		
		Label lblProperties = new Label(this, SWT.NONE);
		lblProperties.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblProperties.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblProperties.setFont(SimLive.FONT_BOLD);
		lblProperties.setText("Properties");
		new Label(this, SWT.NONE);
		
		final Button btnFixedXDisp = new Button(this, SWT.CHECK);
		btnFixedXDisp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnFixedXDisp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				support.setFixedDisp(btnFixedXDisp.getSelection(), 0);
				SimLive.view.redraw();
			}
		});
		btnFixedXDisp.setText("Fixed x-Displacement");
		btnFixedXDisp.setSelection(support.isFixedDisp()[0]);
		new Label(this, SWT.NONE);
		
		final Button btnFixedYDisp = new Button(this, SWT.CHECK);
		btnFixedYDisp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnFixedYDisp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				support.setFixedDisp(btnFixedYDisp.getSelection(), 1);
				SimLive.view.redraw();
			}
		});
		btnFixedYDisp.setText("Fixed y-Displacement");
		btnFixedYDisp.setSelection(support.isFixedDisp()[1]);
		new Label(this, SWT.NONE);
		
		Button btnFixedZDisp = new Button(this, SWT.CHECK);
		btnFixedZDisp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnFixedZDisp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				support.setFixedDisp(btnFixedZDisp.getSelection(), 2);
				SimLive.view.redraw();
			}
		});
		btnFixedZDisp.setText("Fixed z-Displacement");
		btnFixedZDisp.setSelection(support.isFixedDisp()[2]);
		if (Model.twoDimensional) {
			btnFixedZDisp.setSelection(true);
			btnFixedZDisp.setEnabled(false);
		}
		new Label(this, SWT.NONE);
		
		final Button btnFixedXRot = new Button(this, SWT.CHECK);
		btnFixedXRot.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnFixedXRot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				support.setFixedRot(btnFixedXRot.getSelection(), 0);
				SimLive.view.redraw();
			}
		});
		btnFixedXRot.setText("Fixed x-Rotation");
		btnFixedXRot.setSelection(support.isFixedRot()[0]);
		if (Model.twoDimensional) {
			btnFixedXRot.setSelection(true);
			btnFixedXRot.setEnabled(false);
		}
		new Label(this, SWT.NONE);
		
		Button btnFixedYRot = new Button(this, SWT.CHECK);
		btnFixedYRot.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnFixedYRot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				support.setFixedRot(btnFixedYRot.getSelection(), 1);
				SimLive.view.redraw();
			}
		});
		btnFixedYRot.setText("Fixed y-Rotation");
		btnFixedYRot.setSelection(support.isFixedRot()[1]);
		if (Model.twoDimensional) {
			btnFixedYRot.setSelection(true);
			btnFixedYRot.setEnabled(false);
		}
		new Label(this, SWT.NONE);
		
		Button btnFixedZRot = new Button(this, SWT.CHECK);
		btnFixedZRot.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnFixedZRot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				support.setFixedRot(btnFixedZRot.getSelection(), 2);
				SimLive.view.redraw();
			}
		});
		btnFixedZRot.setText("Fixed z-Rotation");
		btnFixedZRot.setSelection(support.isFixedRot()[2]);
		
		Label lblCoordinateSystem = new Label(this, SWT.NONE);
		lblCoordinateSystem.setText("Coordinate System");
		lblCoordinateSystem.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblCoordinateSystem.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblCoordinateSystem.setFont(SimLive.FONT_BOLD);
		
		Label lblAxis = new Label(this, SWT.NONE);
		lblAxis.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblAxis.setText("Axis:");
		
		Text text = new Text(this, SWT.BORDER);
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
				support.setAxis(SimLive.getInputDouble(text), 0);
				SimLive.view.redraw();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text.setText(SimLive.double2String(support.getAxis()[0]));
		text.setEnabled(!Model.twoDimensional);
		
		Text text_1 = new Text(this, SWT.BORDER);
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
				support.setAxis(SimLive.getInputDouble(text_1), 1);
				SimLive.view.redraw();
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_1.setText(SimLive.double2String(support.getAxis()[1]));
		text_1.setEnabled(!Model.twoDimensional);
		
		Text text_2 = new Text(this, SWT.BORDER);
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
				support.setAxis(SimLive.getInputDouble(text_2), 2);
				SimLive.view.redraw();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		if (Model.twoDimensional) {
			support.setAxis(1, 2);
		}
		text_2.setText(SimLive.double2String(support.getAxis()[2]));
		text_2.setEnabled(!Model.twoDimensional);
		
		Label lblAngle = new Label(this, SWT.NONE);
		lblAngle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAngle.setText("Angle:");
		
		final Text text_3 = new Text(this, SWT.BORDER);
		text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_3.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_3.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				support.setAngle(SimLive.getInputDouble(text_3, -360.0, 360.0));
				SimLive.view.redraw();
			}
		});
		text_3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_3.setText(SimLive.double2String(support.getAngle()));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText("\u00B0");
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}

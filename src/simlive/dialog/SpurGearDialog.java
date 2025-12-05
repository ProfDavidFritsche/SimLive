package simlive.dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import simlive.SimLive;
import simlive.misc.Settings;
import simlive.misc.Units;
import simlive.model.Node;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Combo;

public class SpurGearDialog extends GeometricAreaDialog {
	
	private Node center;
	private double radius;
	private Combo combo;
	private Spinner spinner;
	private Text text;
	private Text text_1;
	private Text text_2;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public SpurGearDialog(Composite parent, int style, final Node center) {
		super(parent, style);
		this.center = center;
		this.radius = 0.0;
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblSpurGear = new Label(this, SWT.NONE);
		lblSpurGear.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblSpurGear.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblSpurGear.setFont(SimLive.FONT_BOLD);
		lblSpurGear.setText("Spur Gear");
		
		Label lblType = new Label(this, SWT.NONE);
		lblType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblType.setText("Type:");
		
		combo = new Combo(this, SWT.READ_ONLY);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				SimLive.view.redraw();
			}
		});
		combo.setItems(new String[] {"External", "Internal"});
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		combo.select(0);
		
		Label lblTeeth = new Label(this, SWT.NONE);
		lblTeeth.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblTeeth.setText("Teeth:");
		
		spinner = new Spinner(this, SWT.BORDER);
		spinner.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				spinner.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		spinner.setMinimum(4);
		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				double d = spinner.getSelection()*SimLive.string2Double(text_1.getText());
				text_2.setText(SimLive.double2String(d));
				SimLive.view.redraw();
			}
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				spinner.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		new Label(this, SWT.NONE);
		
		Label lblPressureAngle = new Label(this, SWT.NONE);
		lblPressureAngle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblPressureAngle.setText("Pressure \u03b1:");
		
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
				Settings.pressureAngle = SimLive.getInputDouble(text, 12.0, 35.0);
				SimLive.view.redraw();
			}
		});
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text.setText(SimLive.double2String(Settings.pressureAngle));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setText("°");
		
		Label lblModule = new Label(this, SWT.NONE);
		lblModule.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblModule.setText("Module:");
		
		text_1 = new Text(this, SWT.BORDER);
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				int z = (int) (SimLive.string2Double(text_2.getText())/SimLive.string2Double(text_1.getText()));
				spinner.setSelection(z);
				Settings.module = SimLive.string2Double(text_1.getText());
				SimLive.view.redraw();
			}
		});
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_1.setText(SimLive.double2String(Settings.module));
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setText(Units.getLengthUnit());
		
		Label lblPitchDiameter = new Label(this, SWT.NONE);
		lblPitchDiameter.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblPitchDiameter.setText("Pitch \u00d8:");
		
		text_2 = new Text(this, SWT.BORDER);
		text_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				int z = (int) (SimLive.string2Double(text_2.getText())/SimLive.string2Double(text_1.getText()));
				spinner.setSelection(z);
				SimLive.view.redraw();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_2.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_2.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_2.setText("0");
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setText(Units.getLengthUnit());
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		
		Button btnOk = new Button(this, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				double[] values = getValues();
				if (values[4] == 0.0) {
					SimLive.model.addExternalSpurGearFromTrisAndQuads(center.getCoords(),
							(int) values[0], values[1], values[2], values[3]);
				}
				else {
					SimLive.model.addInternalSpurGearFromQuads(center.getCoords(),
							(int) values[0], values[1], values[2], values[3], values[2]*2.0);
				}
				SimLive.view.deselectAllAndDisposeDialogs();
			}
		});
		btnOk.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnOk.setText("OK");
		
		Button btnCancel = new Button(this, SWT.NONE);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SimLive.view.deselectAllAndDisposeDialogs();
			}
		});
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnCancel.setText("Cancel");
		
		updateDialog(center);
		
		parent.layout();
	}
	
	public void updateDialog(Node radiusPoint) {
		double[] diff = new double[2];
		diff[0] = radiusPoint.getXCoord()-center.getXCoord();
		diff[1] = radiusPoint.getYCoord()-center.getYCoord();
		radius = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]);
		
		text_2.setText(SimLive.double2String(2.0*radius));
		
		int z = (int) (SimLive.string2Double(text_2.getText())/SimLive.string2Double(text_1.getText()));
		spinner.setSelection(z);
		text_2.setText(SimLive.double2String(spinner.getSelection()*SimLive.string2Double(text_1.getText())));
	}
	
	public double[] getValues() {
		double z = spinner.getSelection();
		double alpha = SimLive.getInputDouble(text)*Math.PI/180.0;
		double m = SimLive.getInputDouble(text_1);
		double r = SimLive.getInputDouble(text_2)/2.0;
		double selection = combo.getSelectionIndex();
		return new double[]{z, alpha, m, r, selection};
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
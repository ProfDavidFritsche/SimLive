package simlive.dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.Material;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class MaterialDialog extends Composite {
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public MaterialDialog(final Composite parent, int style, final Material material) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		GridLayout gl_composite = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gl_composite);
		this.setLayout(gl_composite);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText(material.name);
		
		Label lblDensity = new Label(this, SWT.NONE);
		lblDensity.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblDensity.setText("Density:");
		
		final Text text_1 = new Text(this, SWT.BORDER);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				double value = SimLive.getInputDouble(text_1);
				material.setDensity(value);
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.setText(SimLive.double2String(material.getDensity()));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getMassUnit()+"/"+Units.getLengthUnit()+"\u00B3");
		
		Label lblYoungs = new Label(this, SWT.NONE);
		lblYoungs.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblYoungs.setText("Young's:");
		
		final Text text_2 = new Text(this, SWT.BORDER);
		text_2.setToolTipText("Young's Modulus");
		text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_2.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_2.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				double value = SimLive.getInputDouble(text_2);
				material.setYoungsModulus(value);
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_2.setText(SimLive.double2String(material.getYoungsModulus()));
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getForceUnit()+"/"+Units.getLengthUnit()+"\u00B2");
		
		Label lblPoissons = new Label(this, SWT.NONE);
		lblPoissons.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblPoissons.setText("Poisson's:");
		
		final Text text_3 = new Text(this, SWT.BORDER);
		text_3.setToolTipText("Poisson's Ratio");
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
				double value = SimLive.getInputDouble(text_3, 0, 0.5);
				material.setPoissonsRatio(value);
			}
		});
		text_3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_3.setText(SimLive.double2String(material.getPoissonsRatio()));
		new Label(this, SWT.NONE);
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

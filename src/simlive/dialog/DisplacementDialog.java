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
import simlive.model.Load;
import simlive.model.Model;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;

public class DisplacementDialog extends Composite {
	
	private Text text, text_1, text_2, text_3, text_4, text_5;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public DisplacementDialog(Composite parent, int style, final Load load) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		final Button btnXDisp = new Button(this, SWT.CHECK|SWT.RIGHT_TO_LEFT);
		btnXDisp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		btnXDisp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				load.setDisp(btnXDisp.getSelection(), 0);
				text.setEnabled(load.isDisp()[0]);
				if (load.isDisp()[0]) {
					text.setText(SimLive.double2String(load.getDisp()[0]));
				}
				else {
					text.setText("");
				}
				SimLive.view.redraw();
			}
		});
		btnXDisp.setText(":.x-Disp");
		btnXDisp.setSelection(load.isDisp()[0]);
			
		text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setDisp(SimLive.getInputDouble(text), 0);
				SimLive.view.redraw();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text.setEnabled(load.isDisp()[0]);
		if (load.isDisp()[0]) {
			text.setText(SimLive.double2String(load.getDisp()[0]));
		}
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getLengthUnit());
		
		final Button btnYDisp = new Button(this, SWT.CHECK|SWT.RIGHT_TO_LEFT);
		btnYDisp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		btnYDisp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				load.setDisp(btnYDisp.getSelection(), 1);
				text_1.setEnabled(load.isDisp()[1]);
				if (load.isDisp()[1]) {
					text_1.setText(SimLive.double2String(load.getDisp()[1]));
				}
				else {
					text_1.setText("");
				}
				SimLive.view.redraw();
			}
		});
		btnYDisp.setText(":.y-Disp");
		btnYDisp.setSelection(load.isDisp()[1]);
		
		text_1 = new Text(this, SWT.BORDER);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setDisp(SimLive.getInputDouble(text_1), 1);
				SimLive.view.redraw();
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_1.setEnabled(load.isDisp()[1]);
		if (load.isDisp()[1]) {
			text_1.setText(SimLive.double2String(load.getDisp()[1]));
		}
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getLengthUnit());
		
		final Button btnZDisp = new Button(this, SWT.CHECK|SWT.RIGHT_TO_LEFT);
		btnZDisp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnZDisp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				load.setDisp(btnZDisp.getSelection(), 2);
				text_2.setEnabled(load.isDisp()[2]);
				if (load.isDisp()[2]) {
					text_2.setText(SimLive.double2String(load.getDisp()[2]));
				}
				else {
					text_2.setText("");
				}
				SimLive.view.redraw();
			}
		});
		btnZDisp.setText(":.z-Disp");
		btnZDisp.setSelection(load.isDisp()[2]);
		btnZDisp.setEnabled(!Model.twoDimensional);
		
		text_2 = new Text(this, SWT.BORDER);
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
				load.setDisp(SimLive.getInputDouble(text_2), 2);
				SimLive.view.redraw();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_2.setEnabled(load.isDisp()[2]);
		if (load.isDisp()[2]) {
			text_2.setText(SimLive.double2String(load.getDisp()[2]));
		}
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText(Units.getLengthUnit());
		
		final Button btnXrot = new Button(this, SWT.CHECK|SWT.RIGHT_TO_LEFT);
		btnXrot.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		btnXrot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				load.setRotation(btnXrot.getSelection(), 0);
				text_3.setEnabled(load.isRotation()[0]);
				if (load.isRotation()[0]) {
					text_3.setText(SimLive.double2String(load.getRotation()[0]));
				}
				else {
					text_3.setText("");
				}
				SimLive.view.redraw();
			}
		});
		btnXrot.setText(":.x-Rot");
		btnXrot.setSelection(load.isRotation()[0]);
		btnXrot.setEnabled(!Model.twoDimensional);
		
		text_3 = new Text(this, SWT.BORDER);
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
				load.setRotation(SimLive.getInputDouble(text_3), 0);
				SimLive.view.redraw();
			}
		});
		text_3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_3.setEnabled(load.isRotation()[0]);
		if (load.isRotation()[0]) {
			text_3.setText(SimLive.double2String(load.getRotation()[0]));
		}
		
		Label lblUnit_3 = new Label(this, SWT.NONE);
		lblUnit_3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_3.setText("\u00B0");
		
		Button btnYrot = new Button(this, SWT.CHECK|SWT.RIGHT_TO_LEFT);
		btnYrot.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnYrot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				load.setRotation(btnYrot.getSelection(), 1);
				text_4.setEnabled(load.isRotation()[1]);
				if (load.isRotation()[1]) {
					text_4.setText(SimLive.double2String(load.getRotation()[1]));
				}
				else {
					text_4.setText("");
				}
				SimLive.view.redraw();
			}
		});
		btnYrot.setText(":.y-Rot");
		btnYrot.setSelection(load.isRotation()[1]);
		btnYrot.setEnabled(!Model.twoDimensional);
		
		text_4 = new Text(this, SWT.BORDER);
		text_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_4.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_4.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setRotation(SimLive.getInputDouble(text_4), 1);
				SimLive.view.redraw();
			}
		});
		text_4.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_4.setEnabled(load.isRotation()[1]);
		if (load.isRotation()[1]) {
			text_4.setText(SimLive.double2String(load.getRotation()[1]));
		}
		
		Label lblUnit_4 = new Label(this, SWT.NONE);
		lblUnit_4.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_4.setText("\u00B0");		
		
		Button btnZrot = new Button(this, SWT.CHECK|SWT.RIGHT_TO_LEFT);
		btnZrot.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnZrot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				load.setRotation(btnZrot.getSelection(), 2);
				text_5.setEnabled(load.isRotation()[2]);
				if (load.isRotation()[2]) {
					text_5.setText(SimLive.double2String(load.getRotation()[2]));
				}
				else {
					text_5.setText("");
				}
				SimLive.view.redraw();
			}
		});
		btnZrot.setText(":.z-Rot");
		btnZrot.setSelection(load.isRotation()[2]);
		
		text_5 = new Text(this, SWT.BORDER);
		text_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_5.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_5.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setRotation(SimLive.getInputDouble(text_5), 2);
				SimLive.view.redraw();
			}
		});
		text_5.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_5.setEnabled(load.isRotation()[2]);
		if (load.isRotation()[2]) {
			text_5.setText(SimLive.double2String(load.getRotation()[2]));
		}
		
		Label lblUnit_5 = new Label(this, SWT.NONE);
		lblUnit_5.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_5.setText("\u00B0");
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

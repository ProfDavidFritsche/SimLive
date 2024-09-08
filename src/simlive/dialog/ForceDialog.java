package simlive.dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import simlive.model.Load;
import simlive.model.Model;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;

public class ForceDialog extends Composite {
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public ForceDialog(Composite parent, int style, final Load load) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblXForce = new Label(this, SWT.NONE);
		lblXForce.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblXForce.setText("x-Force:");
		 
		final Text text = new Text(this, SWT.BORDER);
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
				load.setForce(SimLive.getInputDouble(text), 0);
				SimLive.view.redraw();
		 	}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
		 		if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
		 	}
		});
		text.setText(SimLive.double2String(load.getForce()[0]));
				 
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getForceUnit());
		 
		Label lblYForce = new Label(this, SWT.NONE);
		lblYForce.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblYForce.setText("y-Force:");
		 
		final Text text_1 = new Text(this, SWT.BORDER);
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
				load.setForce(SimLive.getInputDouble(text_1), 1);
				SimLive.view.redraw();
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_1.setText(SimLive.double2String(load.getForce()[1]));
		 
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getForceUnit());
		
		Label lblZforce = new Label(this, SWT.NONE);
		lblZforce.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblZforce.setText("z-Force:");
		
		Text text_2 = new Text(this, SWT.BORDER);
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
				load.setForce(SimLive.getInputDouble(text_2), 2);
				SimLive.view.redraw();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_2.setText(SimLive.double2String(load.getForce()[2]));
		text_2.setEnabled(!Model.twoDimensional);
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText(Units.getForceUnit());		
		
		Label lblXmoment = new Label(this, SWT.NONE);
		lblXmoment.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblXmoment.setText("x-Moment:");
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
				load.setMoment(SimLive.getInputDouble(text_3), 0);
				SimLive.view.redraw();
			}
		});
		text_3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_3.setText(SimLive.double2String(load.getMoment()[0]));
		text_3.setEnabled(!Model.twoDimensional);
		 
		Label lblUnit_3 = new Label(this, SWT.NONE);
		lblUnit_3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
	 	lblUnit_3.setText(Units.getForceUnit()+Units.getLengthUnit());
	 	
	 	Label lblYmoment = new Label(this, SWT.NONE);
	 	lblYmoment.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
	 	lblYmoment.setText("y-Moment:");
	 	
	 	Text text_4 = new Text(this, SWT.BORDER);
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
				load.setMoment(SimLive.getInputDouble(text_4), 1);
				SimLive.view.redraw();
			}
		});
	 	text_4.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
	 	text_4.setText(SimLive.double2String(load.getMoment()[1]));
	 	text_4.setEnabled(!Model.twoDimensional);
		
	 	Label lblUnit_4 = new Label(this, SWT.NONE);
		lblUnit_4.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
	 	lblUnit_4.setText(Units.getForceUnit()+Units.getLengthUnit());
	 	
	 	Label lblZmoment = new Label(this, SWT.NONE);
	 	lblZmoment.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
	 	lblZmoment.setText("z-Moment:");
	 	
	 	Text text_5 = new Text(this, SWT.BORDER);
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
				load.setMoment(SimLive.getInputDouble(text_5), 2);
				SimLive.view.redraw();
			}
		});
	 	text_5.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
	 	text_5.setText(SimLive.double2String(load.getMoment()[2]));
	 	
	 	Label lblUnit_5 = new Label(this, SWT.NONE);
		lblUnit_5.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
	 	lblUnit_5.setText(Units.getForceUnit()+Units.getLengthUnit());
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

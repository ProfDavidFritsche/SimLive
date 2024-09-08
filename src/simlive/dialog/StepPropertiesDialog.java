package simlive.dialog;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.Model;
import simlive.model.Step;
import simlive.model.Step.GRAVITY;

import org.eclipse.swt.widgets.Combo;

public class StepPropertiesDialog extends Composite {

	private Text text;
	
	public StepPropertiesDialog(final Composite parent, int style, final Step step) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		if (step.type != Step.Type.MODAL_ANALYSIS) {
			Label lblDuration = new Label(this, SWT.NONE);
			lblDuration.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblDuration.setText("Duration:");
			
			final Text text = new Text(this, SWT.BORDER);
			text.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
				}
			});
			text.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					text.notifyListeners(SWT.DefaultSelection, new Event());
				}
			});
			text.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					double value = SimLive.getInputDouble(text);
					step.duration = value;
				}
			});
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			text.setText(SimLive.double2String(step.duration));
			
			Label lblUnit = new Label(this, SWT.NONE);
			lblUnit.setText(Units.getTimeUnit());
		}
		
		Label lblIncrements = new Label(this, SWT.NONE);
		lblIncrements.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblIncrements.setText("Increments:");
		
		final Spinner spinner = new Spinner(this, SWT.BORDER);
		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = spinner.getSelection();
				step.nIncrements = value;
			}
		});
		spinner.setMaximum(10000);
		spinner.setMinimum(1);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		spinner.setSelection(step.nIncrements);
		new Label(this, SWT.NONE);
		
		if (step.type == Step.Type.MECHANICAL_STATIC) {
			Label lblMaxIterations = new Label(this, SWT.NONE);
			lblMaxIterations.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
			lblMaxIterations.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			lblMaxIterations.setFont(SimLive.FONT_BOLD);
			lblMaxIterations.setText("Nonlinear Iterations");
			
			Label lblMaximum = new Label(this, SWT.NONE);
			lblMaximum.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblMaximum.setText("Maximum:");
			
			final Spinner spinner_1 = new Spinner(this, SWT.BORDER);
			spinner_1.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int value = spinner_1.getSelection();
					step.maxIterations = value;
				}
			});
			spinner_1.setMaximum(100);
			spinner_1.setMinimum(2);
			spinner_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			spinner_1.setSelection(step.maxIterations);
			new Label(this, SWT.NONE);
		}
		
		if (step.type != Step.Type.MODAL_ANALYSIS) {
			Label lblGravity = new Label(this, SWT.NONE);
			lblGravity.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
			lblGravity.setFont(SimLive.FONT_BOLD);
			lblGravity.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			lblGravity.setText("Gravity");
			
			Label lblType = new Label(this, SWT.NONE);
			lblType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblType.setText("Type:");
			
			Combo combo = new Combo(this, SWT.READ_ONLY);
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					step.gravity = Step.GRAVITY.values()[combo.getSelectionIndex()];
					text.setEnabled(step.gravity != GRAVITY.NO_GRAVITY);
					if (step.gravity != GRAVITY.NO_GRAVITY) {
						text.setText(SimLive.double2String(step.gValue));
					}
					else {
						text.setText("");
					}
					SimLive.view.redraw();
				}
			});
			String[] str = new String[] {"No Gravity", "Gravity x-Direction", "Gravity y-Direction", "Gravity z-Direction"};
			if (Model.twoDimensional) {
				str = Arrays.copyOf(str, str.length-1);
			}
			combo.setItems(str);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			combo.select(step.gravity.ordinal());
			
			Label lblgValue = new Label(this, SWT.NONE);
			lblgValue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblgValue.setText("g-Value:");
			
			text = new Text(this, SWT.BORDER);
			text.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					step.gValue = SimLive.string2Double(text.getText());
					SimLive.view.redraw();
				}
			});
			text.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent arg0) {
					text.notifyListeners(SWT.DefaultSelection, new Event());
				}
			});
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			text.setEnabled(step.gravity != GRAVITY.NO_GRAVITY);
			if (step.gravity != GRAVITY.NO_GRAVITY) {
				text.setText(SimLive.double2String(step.gValue));
			}
			
			Label lblUnit = new Label(this, SWT.NONE);
			lblUnit.setText(Units.getLengthUnit()+"/"+Units.getTimeUnit()+"\u00B2");
		}
	}

}

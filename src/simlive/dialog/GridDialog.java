package simlive.dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import simlive.SimLive;
import simlive.misc.Settings;
import simlive.misc.Units;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class GridDialog extends Composite {
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public GridDialog(final Composite parent, int style, final Settings settings) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText("Grid");
		
		Label lblMeshSize = new Label(this, SWT.NONE);
		lblMeshSize.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblMeshSize.setText("Mesh Size:");
		
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
				double value = SimLive.getInputDouble(text, SimLive.ZERO_TOL, Double.MAX_VALUE);
				SimLive.view.adaptZoomToNewGridSize(settings.meshSize, value);
				settings.meshSize = value;
				SimLive.view.redraw();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text.setText(SimLive.double2String(settings.meshSize));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getLengthUnit());
		
		Label lblCount = new Label(this, SWT.NONE);
		lblCount.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblCount.setText("Count:");
		
		final Spinner spinner = new Spinner(this, SWT.BORDER);
		spinner.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				spinner.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		spinner.addListener(SWT.Verify, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if (arg0.keyCode == 0) {
					spinner.notifyListeners(SWT.DefaultSelection, new Event());
				}
			}
		});
		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				int value = spinner.getSelection();
				if (value%2 != 0) {
					value -= 1;
					spinner.setSelection(value);
				}
				settings.meshCount = value;
				SimLive.view.redraw();
			}
		});
		spinner.setSelection(settings.meshCount);
		spinner.setMaximum(100);
		spinner.setMinimum(2);
		spinner.setIncrement(2);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

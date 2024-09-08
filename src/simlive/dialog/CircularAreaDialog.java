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
import simlive.misc.Units;
import simlive.model.Node;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.wb.swt.SWTResourceManager;

public class CircularAreaDialog extends GeometricAreaDialog {
	
	private Node center;
	private double radius;
	private Text text;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public CircularAreaDialog(Composite parent, int style, final Node center) {
		super(parent, style);
		this.center = center;
		this.radius = 0.0;
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblCircularArea = new Label(this, SWT.NONE);
		lblCircularArea.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblCircularArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblCircularArea.setFont(SimLive.FONT_BOLD);
		lblCircularArea.setText("Circular Area");
		
		Label lblRadius = new Label(this, SWT.NONE);
		lblRadius.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblRadius.setText("Radius:");
		
		text = new Text(this, SWT.BORDER);
		text.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setCircle();
			}
		});
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
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text.setText("0");
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setText(Units.getLengthUnit());
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		
		Button btnOk = new Button(this, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SimLive.model.addCircleFromTrisAndQuads(center.getCoords(), radius);
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
		
		parent.layout();
	}
	
	private void setCircle() {
		radius = SimLive.getInputDouble(text);
		double[] pos = new double[3];
		pos[0] = center.getXCoord()+radius;
		pos[1] = center.getYCoord();
		SimLive.view.setMousePos(pos);
		SimLive.view.redraw();
	}
	
	public void updateDialog(Node radiusPoint) {
		double[] diff = new double[2];
		diff[0] = radiusPoint.getXCoord()-center.getXCoord();
		diff[1] = radiusPoint.getYCoord()-center.getYCoord();
		radius = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]);
		
		text.setText(SimLive.double2String(radius));
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
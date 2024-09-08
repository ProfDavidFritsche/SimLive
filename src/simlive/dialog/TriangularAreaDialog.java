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

public class TriangularAreaDialog extends GeometricAreaDialog {
	
	private Node edge;
	private double base;
	private double height;
	private double angle;
	private Text text;
	private Text text_1;
	private Text text_2;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public TriangularAreaDialog(Composite parent, int style, final Node edge) {
		super(parent, style);
		this.edge = edge;
		this.base = 0.0;
		this.height = 0.0;
		this.angle = 90.0;
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTriangularArea = new Label(this, SWT.NONE);
		lblTriangularArea.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTriangularArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTriangularArea.setFont(SimLive.FONT_BOLD);
		lblTriangularArea.setText("Triangular Area");
		
		Label lblBase = new Label(this, SWT.NONE);
		lblBase.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblBase.setText("Base:");
		
		text = new Text(this, SWT.BORDER);
		text.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setTriangle();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
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
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getLengthUnit());
		
		Label lblHeight = new Label(this, SWT.NONE);
		lblHeight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblHeight.setText("Height:");
		
		text_1 = new Text(this, SWT.BORDER);
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setTriangle();
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_1.setText("0");
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getLengthUnit());
		
		Label lblAngle = new Label(this, SWT.NONE);
		lblAngle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAngle.setText("Angle:");
		
		text_2 = new Text(this, SWT.BORDER);
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_2.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				text_2.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				setTriangle();
			}
		});
		text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_2.setText("90");
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText("°");
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		
		Button btnOk = new Button(this, SWT.NONE);
		btnOk.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SimLive.model.addTriangleFromQuads(edge.getCoords(), base, height, getOffset());
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
	
	private void setTriangle() {
		base = SimLive.getInputDouble(text);
		height = SimLive.getInputDouble(text_1);
		angle = SimLive.getInputDouble(text_2, 0.0, 180.0);
		double[] pos = new double[3];
		pos[0] = edge.getXCoord()+base;
		pos[1] = edge.getYCoord()+height;
		SimLive.view.setMousePos(pos);
		SimLive.view.redraw();
	}
	
	public void updateDialog(Node edgePoint) {
		base = edgePoint.getXCoord()-edge.getXCoord();
		height = edgePoint.getYCoord()-edge.getYCoord();
		
		text.setText(SimLive.double2String(base));
		text_1.setText(SimLive.double2String(height));
	}
	
	public double getOffset() {
		return Math.signum(base)*Math.abs(height)/Math.tan(angle*Math.PI/180.0);
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
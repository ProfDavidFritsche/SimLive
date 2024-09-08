package simlive.dialog;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import simlive.SimLive;
import simlive.misc.Settings;
import simlive.misc.Units;
import simlive.model.Part3d;
import simlive.model.Vertex3d;

import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class Part3dDialog extends Composite {
	private ArrayList<Part3d> parts3d;
	private ArrayList<Vertex3d> vertices0;
	private ArrayList<Vertex3d> vertices0rot;
	private Text text_2;
	private Text text_3;
	private Text text_4;
	private Text text_5;
	private Text text_6;
	private Text text_7;
	private Text text_8;
	private Text text_9;
	private Text text_10;
	private Text text_11;
	private double[] move = new double[3];
	private double rotAngle;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public Part3dDialog(Composite parent, int style, final ArrayList<Part3d> parts3d, final Settings settings) {
		super(parent, style);
		if (SimLive.dialogArea != null) SimLive.dialogArea.dispose();
		this.parts3d = new ArrayList<Part3d>();
		this.parts3d.addAll(parts3d);
		this.vertices0 = new ArrayList<Vertex3d>();
		this.vertices0rot = new ArrayList<Vertex3d>();
		for (int s = 0; s < parts3d.size(); s++) {
			for (int n = 0; n < parts3d.get(s).getNrVertices(); n++) {
				double[] coords = parts3d.get(s).getVertex(n).getCoords();
				this.vertices0.add(new Vertex3d(coords));
				this.vertices0rot.add(new Vertex3d(coords));
			}
		}
		
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		int nrFacets = 0;
		for (int s = 0; s < parts3d.size(); s++) {
			nrFacets += parts3d.get(s).getNrFacets();
		}
		if (parts3d.size() > 1) {
			lblTitle.setText(parts3d.size() + " 3D-Parts ("+nrFacets+" Facets)");
		}
		else {
			lblTitle.setText("3D-Part ("+nrFacets+" Facets)");
		}
		
		Label lblRotate = new Label(this, SWT.NONE);
		lblRotate.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblRotate.setFont(SimLive.FONT_BOLD);
		lblRotate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblRotate.setText("Rotate");
		
		Label lblPoint = new Label(this, SWT.NONE);
		lblPoint.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblPoint.setText("Point:");
		
		text_5 = new Text(this, SWT.BORDER);
		text_5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_5.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_5.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_5.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_5.setText("0");
		
		text_6 = new Text(this, SWT.BORDER);
		text_6.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_6.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_6.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_6.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_6.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_6.setText("0");
		
		text_7 = new Text(this, SWT.BORDER);
		text_7.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_7.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_7.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_7.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_7.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_7.setText("0");
		
		Label lblAxis = new Label(this, SWT.NONE);
		lblAxis.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAxis.setText("Axis:");
		
		text_8 = new Text(this, SWT.BORDER);
		text_8.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_8.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_8.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_8.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_8.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_8.setText("0");
		
		text_9 = new Text(this, SWT.BORDER);
		text_9.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_9.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_9.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_9.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_9.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_9.setText("0");
		
		text_10 = new Text(this, SWT.BORDER);
		text_10.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_10.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_10.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_10.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_10.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_10.setText("0");
		
		Label lblAngle = new Label(this, SWT.NONE);
		lblAngle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAngle.setText("Angle:");
		
		text_11 = new Text(this, SWT.BORDER);
		text_11.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_11.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_11.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_11.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_11.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Label lblUnit_3 = new Label(this, SWT.NONE);
		lblUnit_3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_3.setText("\u00B0");
		
		Label lblMove = new Label(this, SWT.NONE);
		lblMove.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblMove.setFont(SimLive.FONT_BOLD);
		lblMove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblMove.setText("Move");
		
		Label lblDeltax = new Label(this, SWT.NONE);
		lblDeltax.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblDeltax.setText("\u0394x:");
		
		text_2 = new Text(this, SWT.BORDER);
		text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_2.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_2.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getLengthUnit());
		
		Label lblDeltay = new Label(this, SWT.NONE);
		lblDeltay.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblDeltay.setText("\u0394y:");
		
		text_3 = new Text(this, SWT.BORDER);
		text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_3.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_3.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getLengthUnit());
		
		Label lblDeltaz = new Label(this, SWT.NONE);
		lblDeltaz.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblDeltaz.setText("\u0394z:");
		
		text_4 = new Text(this, SWT.BORDER);
		text_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				setElements();
			}
		});
		text_4.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_4.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_4.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText(Units.getLengthUnit());
		
		updateDialog(new double[3]);
		
		parent.layout();
	}
	
	public void updateDialog(double[] deltaMove) {
		this.move[0] += deltaMove[0];
		this.move[1] += deltaMove[1];
		this.move[2] += deltaMove[2];
		
		int startIndex = 0;
		for (int s = 0; s < parts3d.size(); s++) {
			for (int n = 0; n < parts3d.get(s).getNrVertices(); n++) {
				double[] coords = vertices0rot.get(startIndex+n).getCoords().clone();
				coords[0] += move[0];
				coords[1] += move[1];
				coords[2] += move[2];
				parts3d.get(s).getVertex(n).setCoords(coords);
			}
			startIndex += parts3d.get(s).getNrVertices();			
		}
		
		text_2.setText(SimLive.double2String(move[0]));
		text_3.setText(SimLive.double2String(move[1]));
		text_4.setText(SimLive.double2String(move[2]));
		text_11.setText(SimLive.double2String(rotAngle));
	}
	
	private void setElements() {
		move[0] = SimLive.getInputDouble(text_2);
		move[1] = SimLive.getInputDouble(text_3);
		move[2] = SimLive.getInputDouble(text_4);
		rotAngle = SimLive.getInputDouble(text_11, -360, 360);
		double[] point = new double[3];
		point[0] = SimLive.getInputDouble(text_5);
		point[1] = SimLive.getInputDouble(text_6);
		point[2] = SimLive.getInputDouble(text_7);
		double[] axis = new double[3];
		axis[0] = SimLive.getInputDouble(text_8);
		axis[1] = SimLive.getInputDouble(text_9);
		axis[2] = SimLive.getInputDouble(text_10);
		
		this.vertices0rot.clear();
		int startIndex = 0;
		for (int s = 0; s < parts3d.size(); s++) {
			for (int n = 0; n < parts3d.get(s).getNrVertices(); n++) {
				parts3d.get(s).getVertex(n).setCoords(vertices0.get(startIndex+n).getCoords());
			}
			parts3d.get(s).rotate(rotAngle*Math.PI/180.0, point, axis);
			for (int n = 0; n < parts3d.get(s).getNrVertices(); n++) {
				double[] coords = parts3d.get(s).getVertex(n).getCoords();
				this.vertices0rot.add(new Vertex3d(coords));
			}
			startIndex += parts3d.get(s).getNrVertices();			
		}
		
		updateDialog(new double[3]);
		SimLive.model.updateModel();
		SimLive.view.redraw();
	}
	
	public boolean atLeastOnePart3dIsUngroupable(ArrayList<Part3d> parts3d) {
		for (int s = 0; s < parts3d.size(); s++) {
			if (!parts3d.get(s).getSubTree().subTrees.isEmpty()) return true;
		}
		return false;
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

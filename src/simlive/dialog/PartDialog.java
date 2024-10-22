package simlive.dialog;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;

import simlive.SimLive;
import simlive.misc.GeomUtility;
import simlive.misc.Settings;
import simlive.misc.Units;
import simlive.model.Connector;
import simlive.model.Element;
import simlive.model.LineElement;
import simlive.model.Model;
import simlive.model.Node;
import simlive.model.PlaneElement;
import simlive.model.PointMass;
import simlive.model.Set;
import simlive.model.Spring;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.wb.swt.SWTResourceManager;

import Jama.Matrix;

import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;

public class PartDialog extends Composite {
	private ArrayList<Set> remainingSets = new ArrayList<Set>();
	private ArrayList<Element> elementSet = new ArrayList<Element>();
	private ArrayList<Node> nodes0;
	private ArrayList<Node> nodes0rot;
	private ArrayList<Connector> connectors0;
	private ArrayList<Connector> connectors0rot;
	private Matrix[] q0Array;
	private Combo combo, combo_2;
	private Text text;
	private Text text_1;
	private Text text_4;
	private Text text_5;
	private Text text_6;
	private Text text_7;
	private Text text_8;
	private Text text_9;
	private Text text_10;
	private Text text_11;
	private Text text_12;
	private Text text_13;
	private Text text_14;
	private Text text_15;
	private Text text_16;
	private Text text_17;
	private Text text_18;
	private double[] move = new double[3];
	private double rotAngle;
	private double[] axis = new double[3];
	private Timer timer = null;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public PartDialog(Composite parent, int style, final ArrayList<Set> sets, final Settings settings) {
		super(parent, style);
		if (SimLive.dialogArea != null) SimLive.dialogArea.dispose();
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				if (timer != null) {
					timer.cancel();
					setElements();
				}
			}
		});
		for (int s = 0; s < sets.size(); s++) {
			elementSet.addAll(sets.get(s).getElements());
		}
		this.remainingSets.addAll(SimLive.model.getSets());
		this.remainingSets.removeAll(sets);
		this.nodes0 = new ArrayList<Node>();
		this.nodes0rot = new ArrayList<Node>();
		for (int s = 0; s < sets.size(); s++) {
			for (int n = 0; n < sets.get(s).getNodes().size(); n++) {
				this.nodes0.add(sets.get(s).getNodes().get(n).clone());
				this.nodes0rot.add(sets.get(s).getNodes().get(n).clone());
			}
		}
		this.connectors0 = new ArrayList<Connector>();
		this.connectors0rot = new ArrayList<Connector>();
		for (int c = 0; c < SimLive.model.getConnectors().size(); c++) {
			this.connectors0.add(SimLive.model.getConnectors().get(c).clone(SimLive.model));
			this.connectors0rot.add(SimLive.model.getConnectors().get(c).clone(SimLive.model));
		}
		q0Array = new Matrix[elementSet.size()];
		for (int elem = 0; elem < elementSet.size(); elem++) {
			if (elementSet.get(elem).isLineElement()) {
				double[] q0 = ((LineElement) elementSet.get(elem)).getQ0();
				q0Array[elem] = new Matrix(q0, 3);
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
		if (elementSet.size() > 1) {
			int nrParts = sets.size();
			if (nrParts > 1) {
				lblTitle.setText(nrParts + " Parts (" + elementSet.size() + " Elements)");
			}
			else {
				lblTitle.setText("Part (" + elementSet.size() + " Elements)");
			}
		}
		else {
			lblTitle.setText(elementSet.get(0).getTypeString() + " " + (elementSet.get(0).getID()+1));
			
		}
		
		if (SimLive.model.doElementsContainOnlyType(elementSet, Element.Type.SPRING)) {
			Label lblStiffness = new Label(this, SWT.NONE);
			lblStiffness.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblStiffness.setText("Stiffness:");
			
			text_1 = new Text(this, SWT.BORDER);
			text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			text_1.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					setElements();
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
			if (SimLive.model.haveSpringsSameSpringStiffness(elementSet)) {
				text_1.setText(SimLive.double2String(((Spring) elementSet.get(0)).getStiffness()));
			}
			
			Label lblUnit = new Label(this, SWT.NONE);
			lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			lblUnit.setText(Units.getForceUnit()+"/"+Units.getLengthUnit());
		}
		
		if (SimLive.model.doElementsContainOnlyType(elementSet, Element.Type.POINT_MASS)) {
			Label lblMass = new Label(this, SWT.NONE);
			lblMass.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblMass.setText("Mass:");
			
			text_1 = new Text(this, SWT.BORDER);
			text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			text_1.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					setElements();
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
			if (SimLive.model.havePointMassesSameMass(elementSet)) {
				text_1.setText(SimLive.double2String(((PointMass) elementSet.get(0)).getMass()));
			}
			
			Label lblUnit = new Label(this, SWT.NONE);
			lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			lblUnit.setText(Units.getMassUnit());
		}
		
		if (!SimLive.model.doElementsContainType(elementSet, Element.Type.SPRING) &&
			!SimLive.model.doElementsContainType(elementSet, Element.Type.POINT_MASS)) {
			/*Label lblMaterial = new Label(this, SWT.NONE);
			lblMaterial.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblMaterial.setText("Material:");
			
			combo = new Combo(this, SWT.READ_ONLY);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setElements();
				}
			});
			
			for (int m = 0; m < SimLive.model.getMaterials().size(); m++) {
				combo.add(SimLive.model.getMaterials().get(m).name);
			}
			if (SimLive.model.haveElementsSameMaterial(elementSet)) {
				combo.select(combo.indexOf(SimLive.model.getMaterials().get(elementSet.get(0).getMaterialID()).name));
			}*/
			
			if (!SimLive.model.doElementsContainType(elementSet, Element.Type.ROD) &&
				!SimLive.model.doElementsContainType(elementSet, Element.Type.BEAM)) {
				/*Label lblState = new Label(this, SWT.NONE);
				lblState.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
				lblState.setText("State:");
				
				combo_1 = new Combo(this, SWT.READ_ONLY);
				combo_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
				combo_1.add("Plane Stress");
				combo_1.add("Plane Strain");
				combo_1.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						setElements();
					}
				});
				if (Sim2d.model.havePlaneElementsSameState(elementSet)) {
					combo_1.select(((PlaneElement) elementSet.get(0)).getState().ordinal());
				}*/
				
				Label lblThickness = new Label(this, SWT.NONE);
				lblThickness.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
				lblThickness.setText("Thickness:");
				
				text = new Text(this, SWT.BORDER);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				text.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						setElements();
						SimLive.model.updateAllOutlines();
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
				if (SimLive.model.havePlaneElementsSameThickness(elementSet)) {
					text.setText(SimLive.double2String(((PlaneElement) elementSet.get(0)).getThickness()));
				}
				
				Label lblUnit = new Label(this, SWT.NONE);
				lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
				lblUnit.setText(Units.getLengthUnit());
			}
			
			/*if (!SimLive.model.doElementsContainType(elementSet, Element.Type.QUAD) &&
				!SimLive.model.doElementsContainType(elementSet, Element.Type.TRI)) {
				Label lblSection = new Label(this, SWT.NONE);
				lblSection.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
				lblSection.setText("Section:");
				
				combo_2 = new Combo(this, SWT.READ_ONLY);
				combo_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
				combo_2.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						setElements();
					}
				});
				
				for (int s = 0; s < SimLive.model.getSections().size(); s++) {
					combo_2.add(SimLive.model.getSections().get(s).getName());
				}
				if (SimLive.model.haveLineElementsSameSection(elementSet)) {
					combo_2.select(combo_2.indexOf(SimLive.model.getSections().get(((LineElement) elementSet.get(0)).getSectionID()).getName()));
				}
			}*/
		}
		
		Label lblTotals = new Label(this, SWT.NONE);
		lblTotals.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTotals.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTotals.setFont(SimLive.FONT_BOLD);
		lblTotals.setText("Totals");
		
		Label lblVolume = new Label(this, SWT.NONE);
		lblVolume.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblVolume.setText("Volume:");
		text_14 = new Text(this, SWT.BORDER);
		text_14.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_14.setEditable(false);
		Label lblUnit_6 = new Label(this, SWT.NONE);
		lblUnit_6.setText(Units.getLengthUnit()+"\u00b3");
		
		Label lblMass = new Label(this, SWT.NONE);
		lblMass.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblMass.setText("Mass:");
		text_15 = new Text(this, SWT.BORDER);
		text_15.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_15.setEditable(false);
		Label lblUnit_7 = new Label(this, SWT.NONE);
		lblUnit_7.setText(Units.getMassUnit());		
		
		Label lblDamping = new Label(this, SWT.NONE);
		lblDamping.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblDamping.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblDamping.setFont(SimLive.FONT_BOLD);
		lblDamping.setText("Damping");
		
		if (!SimLive.model.doElementsContainType(elementSet, Element.Type.POINT_MASS)) {
			Label lblToStiffness = new Label(this, SWT.NONE);
			lblToStiffness.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblToStiffness.setText("To Stiffness:");
			
			Text text_2 = new Text(this, SWT.BORDER);
			text_2.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					double value = SimLive.getInputDouble(text_2);
					for (int e = 0; e < elementSet.size(); e++) {
						elementSet.get(e).setStiffnessDamping(value);
					}
				}
			});
			text_2.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent arg0) {
					text_2.notifyListeners(SWT.DefaultSelection, new Event());
				}
			});
			text_2.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
				}
			});
			text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			if (SimLive.model.haveElementsSameStiffnessDamping(elementSet)) {
				text_2.setText(SimLive.double2String(elementSet.get(0).getStiffnessDamping()));
			}
			Label lblUnit_1 = new Label(this, SWT.NONE);
			lblUnit_1.setText(Units.getTimeUnit());
		}
		
		if (!SimLive.model.doElementsContainType(elementSet, Element.Type.SPRING)) {
			Label lblToMass = new Label(this, SWT.NONE);
			lblToMass.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblToMass.setText("To Mass:");
			
			final Text text_3 = new Text(this, SWT.BORDER);
			text_3.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					double value = SimLive.getInputDouble(text_3);
					for (int e = 0; e < elementSet.size(); e++) {
						elementSet.get(e).setMassDamping(value);
					}
				}
			});
			text_3.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent arg0) {
					text_3.notifyListeners(SWT.DefaultSelection, new Event());
				}
			});
			text_3.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
				}
			});
			text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			if (SimLive.model.haveElementsSameMassDamping(elementSet)) {
				text_3.setText(SimLive.double2String(elementSet.get(0).getMassDamping()));
			}
			Label lblUnit_2 = new Label(this, SWT.NONE);
			lblUnit_2.setText("1/"+Units.getTimeUnit());
		}
		
		Label lblOrientate = new Label(this, SWT.NONE);
		lblOrientate.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblOrientate.setFont(SimLive.FONT_BOLD);
		lblOrientate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblOrientate.setText("Orientate");
		
		if (SimLive.model.doElementsContainOnlyLineElements(elementSet)) {
			
			Label lbl2ndAxis = new Label(this, SWT.NONE);
			lbl2ndAxis.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lbl2ndAxis.setText("2nd Axis:");
			
			text_16 = new Text(this, SWT.BORDER);
			text_16.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					setTimer();
				}
			});
			text_16.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text_16.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					setTimer();
				}
				@Override
				public void focusGained(FocusEvent arg0) {
					if (timer != null) timer.cancel();
				}
			});
			text_16.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			
			text_17 = new Text(this, SWT.BORDER);
			text_17.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					setTimer();
				}
			});
			text_17.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text_17.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					setTimer();
				}
				@Override
				public void focusGained(FocusEvent arg0) {
					if (timer != null) timer.cancel();
				}
			});
			text_17.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			
			text_18 = new Text(this, SWT.BORDER);
			text_18.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					setTimer();
				}
			});
			text_18.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text_18.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					setTimer();
				}
				@Override
				public void focusGained(FocusEvent arg0) {
					if (timer != null) timer.cancel();
				}
			});
			text_18.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		}
		
		Label lblRotate = new Label(this, SWT.NONE);
		lblRotate.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblRotate.setFont(SimLive.FONT_BOLD);
		lblRotate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblRotate.setText("Rotate");
		
		{
			Label lblPoint = new Label(this, SWT.NONE);
			lblPoint.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblPoint.setText("Point:");
			
			text_7 = new Text(this, SWT.BORDER);
			text_7.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
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
			text_7.setText("0");
			
			text_8 = new Text(this, SWT.BORDER);
			text_8.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
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
			text_8.setText("0");
			
			text_9 = new Text(this, SWT.BORDER);
			text_9.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
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
			text_9.setText("0");
			text_9.setEnabled(!Model.twoDimensional);
			
			Label lblAxis = new Label(this, SWT.NONE);
			lblAxis.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblAxis.setText("Axis:");
			
			text_10 = new Text(this, SWT.BORDER);
			text_10.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
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
			text_10.setText("0");
			text_10.setEnabled(!Model.twoDimensional);
			
			text_11 = new Text(this, SWT.BORDER);
			text_11.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
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
			text_11.setText("0");
			text_11.setEnabled(!Model.twoDimensional);
			
			text_12 = new Text(this, SWT.BORDER);
			text_12.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			text_12.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					setElements();
				}
			});
			text_12.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text_12.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					text_12.notifyListeners(SWT.DefaultSelection, new Event());
				}
			});
			String str = Model.twoDimensional ? "1" : "0";
			text_12.setText(str);
			text_12.setEnabled(!Model.twoDimensional);
						
			Label lblAngle = new Label(this, SWT.NONE);
			lblAngle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblAngle.setText("Angle:");
			
			text_13 = new Text(this, SWT.BORDER);
			text_13.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			text_13.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					setElements();
				}
			});
			text_13.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text_13.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					text_13.notifyListeners(SWT.DefaultSelection, new Event());
				}
			});
			
			Label lblUnit_5 = new Label(this, SWT.NONE);
			lblUnit_5.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			lblUnit_5.setText("\u00B0");
		}
		
		Label lblMove = new Label(this, SWT.NONE);
		lblMove.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblMove.setFont(SimLive.FONT_BOLD);
		lblMove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblMove.setText("Move");
		
		{
			Label lblDeltax = new Label(this, SWT.NONE);
			lblDeltax.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblDeltax.setText("\u0394x:");
			
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
			
			Label lblUnit_3 = new Label(this, SWT.NONE);
			lblUnit_3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			lblUnit_3.setText(Units.getLengthUnit());
			
			Label lblDeltay = new Label(this, SWT.NONE);
			lblDeltay.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblDeltay.setText("\u0394y:");
			
			text_5 = new Text(this, SWT.BORDER);
			text_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
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
			
			Label lblUnit_4 = new Label(this, SWT.NONE);
			lblUnit_4.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			lblUnit_4.setText(Units.getLengthUnit());
			
			Label lblDeltaz = new Label(this, SWT.NONE);
			lblDeltaz.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblDeltaz.setText("\u0394z:");
			
			text_6 = new Text(this, SWT.BORDER);
			text_6.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
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
			text_6.setEnabled(!Model.twoDimensional);
			
			Label lblUnit_8 = new Label(this, SWT.NONE);
			lblUnit_8.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			lblUnit_8.setText(Units.getLengthUnit());
		}
		
		updateDialog(new double[3]);
		
		parent.layout();
	}
	
	public void updateDialog(double[] deltaMove) {
		this.move[0] += deltaMove[0];
		this.move[1] += deltaMove[1];
		this.move[2] += deltaMove[2];
		
		for (int n = 0; n < nodes0rot.size(); n++) {
			double[] coords = nodes0rot.get(n).getCoords().clone();
			coords[0] += move[0];
			coords[1] += move[1];
			coords[2] += move[2];
			Node node = SimLive.model.getNodes().get(nodes0rot.get(n).getID());
			node.setCoords(coords);
		}
		
		for (int c = 0; c < connectors0rot.size(); c++) {
			if (elementSet.contains(connectors0rot.get(c).getElement0()) &&
				elementSet.contains(connectors0rot.get(c).getElement1())) {
				double[] coords = connectors0rot.get(c).getCoordinates();
				coords[0] += move[0];
				coords[1] += move[1];
				coords[2] += move[2];
				SimLive.model.getConnectors().get(c).setCoordinates(coords, false);
			}
		}
		
		Matrix R = GeomUtility.getRotationMatrix(rotAngle*Math.PI/180.0, axis);
		for (int elem = 0; elem < elementSet.size(); elem++) {
			if (elementSet.get(elem).isLineElement()) {
				((LineElement) elementSet.get(elem)).setQ0(R.times(q0Array[elem]).getColumnPackedCopy());
			}
		}
		
		if (text_16 != null) {
			text_16.setEnabled(R.trace() == 3);
			if (SimLive.model.haveLineElementsSameQ0(elementSet, 0)) {
				text_16.setText(SimLive.double2String(((LineElement) elementSet.get(0)).getQ0()[0]));
			}
			else {
				text_16.setText("");
			}
		}
		if (text_17 != null) {
			text_17.setEnabled(R.trace() == 3);
			if (SimLive.model.haveLineElementsSameQ0(elementSet, 1)) {
				text_17.setText(SimLive.double2String(((LineElement) elementSet.get(0)).getQ0()[1]));
			}
			else {
				text_17.setText("");
			}
		}
		if (text_18 != null) {
			text_18.setEnabled(R.trace() == 3);
			if (SimLive.model.haveLineElementsSameQ0(elementSet, 2)) {
				text_18.setText(SimLive.double2String(((LineElement) elementSet.get(0)).getQ0()[2]));
			}
			else {
				text_18.setText("");
			}
		}
		
		{
			text_4.setText(SimLive.double2String(move[0]));
			text_5.setText(SimLive.double2String(move[1]));
			text_6.setText(SimLive.double2String(move[2]));
			text_13.setText(SimLive.double2String(rotAngle));
		}
			
		SimLive.model.updateAllDistributedLoads();
		SimLive.model.updateAllElements();
		
		double[] volumeAndMass = getTotalVolumeAndMass();
		text_14.setText(SimLive.double2String(volumeAndMass[0]));		
		text_15.setText(SimLive.double2String(volumeAndMass[1]));		
	}
	
	private void setElements() {
		for (int elem = 0; elem < elementSet.size(); elem++) {
			
			if (elementSet.get(elem).isLineElement() &&
					text_16 != null && text_17 != null && text_18 != null &&
					text_16.isEnabled() && text_17.isEnabled() && text_18.isEnabled()) {
				double[] q0 = ((LineElement) elementSet.get(elem)).getQ0();
				if (text_16.getText() != "") q0[0] = SimLive.getInputDouble(text_16);
				if (text_17.getText() != "") q0[1] = SimLive.getInputDouble(text_17);
				if (text_18.getText() != "") q0[2] = SimLive.getInputDouble(text_18);
				((LineElement) elementSet.get(elem)).setQ0(q0);
				q0Array[elem] = new Matrix(q0, 3);
			}
			
			if (elementSet.get(elem).getType() == Element.Type.SPRING) {
				if (text_1 != null && text_1.getText() != "") {
					((Spring) elementSet.get(elem)).setStiffness(SimLive.getInputDouble(text_1));
				}
			}
			else if (elementSet.get(elem).getType() == Element.Type.POINT_MASS) {
				if (text_1 != null && text_1.getText() != "") {
					((PointMass) elementSet.get(elem)).setMass(SimLive.getInputDouble(text_1));
				}
			}
			else {
				if (combo != null && combo.getSelectionIndex() != -1) {
					elementSet.get(elem).setMaterial(SimLive.model.getMaterials().get(combo.getSelectionIndex()));
				}
				if (elementSet.get(elem).getType() == Element.Type.ROD ||
					elementSet.get(elem).getType() == Element.Type.BEAM) {
					if (combo_2 != null && combo_2.getSelectionIndex() != -1) {
						((LineElement) elementSet.get(elem)).setSection(SimLive.model.getSections().get(combo_2.getSelectionIndex()));
					}
				}
				if (elementSet.get(elem).isPlaneElement()) {
					/*if (combo_1 != null && combo_1.getSelectionIndex() != -1) {
						((PlaneElement) elementSet.get(elem)).setState(PlaneElement.State.values()[combo_1.getSelectionIndex()]);
					}*/
					if (text != null && text.getText() != "") {
						((PlaneElement) elementSet.get(elem)).setThickness(SimLive.getInputDouble(text));
					}
				}
			}
		}
		
		move[0] = SimLive.getInputDouble(text_4);
		move[1] = SimLive.getInputDouble(text_5);
		move[2] = SimLive.getInputDouble(text_6);
		rotAngle = SimLive.getInputDouble(text_13, -360, 360);
		double[] point = new double[3];
		point[0] = SimLive.getInputDouble(text_7);
		point[1] = SimLive.getInputDouble(text_8);
		point[2] = SimLive.getInputDouble(text_9);
		axis[0] = SimLive.getInputDouble(text_10);
		axis[1] = SimLive.getInputDouble(text_11);
		axis[2] = SimLive.getInputDouble(text_12);
		
		for (int n = 0; n < nodes0rot.size(); n++) {
			nodes0rot.get(n).setCoords(nodes0.get(n).getCoords().clone());
		}
		SimLive.model.rotateNodes(nodes0rot, rotAngle*Math.PI/180.0, point, axis);
		
		for (int c = 0; c < connectors0.size(); c++) {
			if (elementSet.contains(connectors0.get(c).getElement0()) &&
				elementSet.contains(connectors0.get(c).getElement1())) {
				double[] coords = connectors0.get(c).getCoordinates();
				ArrayList<Node> connectorCoords = new ArrayList<Node>();
				connectorCoords.add(new Node(coords[0], coords[1], coords[2]));
				SimLive.model.rotateNodes(connectorCoords, rotAngle*Math.PI/180.0, point, axis);
				connectors0rot.get(c).setCoordinates(connectorCoords.get(0).getCoords(), false);
			}
		}
		
		updateDialog(new double[3]);
		SimLive.view.redraw();
	}
	
	public boolean isRelaxable(ArrayList<Set> sets) {
		for (int s = 0; s < sets.size(); s++) {
			for (int elem = 0; elem < sets.get(s).getElements().size(); elem++) {
				if (sets.get(s).getElements().get(elem).isPlaneElement()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isSplittable(ArrayList<Set> sets) {
		int nrLocalNodes = 0;
		ArrayList<Node> globalNodes = new ArrayList<Node>();
		for (int s = 0; s < sets.size(); s++) {
			Set set = sets.get(s);
			for (int n = 0; n < set.getNodes().size(); n++) {
				if (!globalNodes.contains(set.getNodes().get(n))) {
					globalNodes.add(set.getNodes().get(n));
				}
			}
			if (set.getType() == Set.Type.BASIC && set.getElements().size() > 1) {
				nrLocalNodes += 2;
			}
			else {
				for (int e = 0; e < set.getElements().size(); e++) {
					nrLocalNodes += set.getElements().get(e).getElementNodes().length;
				}
			}
		}
		return nrLocalNodes > globalNodes.size();
	}
	
	public boolean isMergeable() {
		double tolerance = SimLive.settings.meshSize/1000.0;
		for (int n0 = 0; n0 < nodes0.size(); n0++) {
			double[] p0 = nodes0.get(n0).getCoords();
			for (int n1 = n0+1; n1 < nodes0.size(); n1++) {
				double[] p1 = nodes0.get(n1).getCoords();
				if (Math.abs(p0[0] - p1[0]) < tolerance &&
					Math.abs(p0[1] - p1[1]) < tolerance &&
					Math.abs(p0[2] - p1[2]) < tolerance &&
					nodes0.get(n0).getID() != nodes0.get(n1).getID()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean allSetsAreOfTypeBasic(ArrayList<Set> sets) {
		for (int s = 0; s < sets.size(); s++) {
			if (sets.get(s).getType() != Set.Type.BASIC) return false;
		}
		return true;
	}
	
	private double[] getTotalVolumeAndMass() {
		double[] volumeAndMass = new double[2];
		for (int e = 0; e < elementSet.size(); e++) {
			if (elementSet.get(e).isPlaneElement()) {
				Matrix R0T = ((PlaneElement) elementSet.get(e)).getR0().transpose();
				int[] elementNodes = elementSet.get(e).getElementNodes();
				double[][] points = new double[elementNodes.length][];
				for (int n = 0; n < elementNodes.length; n++) {
					points[n] = R0T.times(new Matrix(SimLive.model.getNodes().get(elementNodes[n]).getCoords(), 3)).getColumnPackedCopy();
				}
				double volume = GeomUtility.getAreaOfPolygon(points)*((PlaneElement) elementSet.get(e)).getThickness();
				volumeAndMass[0] += volume;
				double density = 0.0;
				if (!SimLive.model.getMaterials().isEmpty()) {
					density = elementSet.get(e).getMaterial().getDensity();
				}
				volumeAndMass[1] += density*volume;
			}
			if (elementSet.get(e).isLineElement() && elementSet.get(e).getType() != Element.Type.SPRING) {
				double area = 0.0;
				if (!SimLive.model.getSections().isEmpty()) {
					area = ((LineElement) elementSet.get(e)).getSection().getArea();
				}
				double volume = area*((LineElement) elementSet.get(e)).getLength();
				volumeAndMass[0] += volume;
				double density = 0.0;
				if (!SimLive.model.getMaterials().isEmpty()) {
					density = elementSet.get(e).getMaterial().getDensity();
				}
				volumeAndMass[1] += density*volume;
			}
			if (elementSet.get(e).getType() == Element.Type.POINT_MASS) {
				volumeAndMass[1] += ((PointMass) elementSet.get(e)).getMass();
			}
		}
		return volumeAndMass;
	}
	
	private void setTimer() {
		timer = new Timer();
		TimerTask task = new TimerTask() {
			public void run() {
				SimLive.shell.getDisplay().syncExec(new Runnable() {
					public void run() {
						if (!PartDialog.this.isDisposed()) setElements();
					}
				});
			}
		};
		timer.schedule(task, 10);
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

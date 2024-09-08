package simlive.dialog;

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
import org.eclipse.swt.widgets.Text;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.Connector;
import simlive.model.Element;
import simlive.model.Model;

import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Combo;

public class ConnectorDialog extends StoreDialog {
	private Text text, text_1, text_2;
	private Combo combo;
	
	public ConnectorDialog(Composite parent, int style, final Connector connector) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText(connector.name);
			
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gl_composite = new GridLayout(6, false);
		SimLive.formatGridLayoutForComposite(gl_composite);
		composite.setLayout(gl_composite);
		
		Label lblIcon = new Label(composite, SWT.NONE);
		lblIcon.setImage(SimLive.resize(SimLive.INFO_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		
		Label lblStore = new Label(composite, SWT.NONE);
		lblStore.setFont(SimLive.FONT_BOLD);
		lblStore.setText("Store");
		
		Label lblFirstPart = new Label(composite, SWT.NONE);
		lblFirstPart.setFont(SimLive.FONT_BOLD);
		lblFirstPart.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		lblFirstPart.setText("First Part");
		
		Label lblAnd = new Label(composite, SWT.NONE);
		lblAnd.setFont(SimLive.FONT_BOLD);
		lblAnd.setText("And");
		
		Label lblSecondPart = new Label(composite, SWT.NONE);
		lblSecondPart.setFont(SimLive.FONT_BOLD);
		lblSecondPart.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		lblSecondPart.setText("Second Part");
		
		lblOK[0] = new Label(composite, SWT.NONE);
		lblOK[0].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(0, connector.getSet0() != null && connector.getSet1() != null);
		
		Composite composite_1 = new Composite(this, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gl_composite_1 = new GridLayout(3, false);
		SimLive.formatGridLayoutForComposite(gl_composite_1);
		composite_1.setLayout(gl_composite_1);
		
		Label lblIcon_1 = new Label(composite_1, SWT.NONE);
		lblIcon_1.setImage(SimLive.resize(SimLive.INFO_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		
		Label lblClickPosition = new Label(composite_1, SWT.NONE);
		lblClickPosition.setFont(SimLive.FONT_BOLD);
		lblClickPosition.setText("Click Position");
		
		lblOK[1] = new Label(composite_1, SWT.NONE);
		lblOK[1].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(1, connector.isCoordsSet());
		
		Label lblProperties = new Label(this, SWT.NONE);
		lblProperties.setText("Properties");
		lblProperties.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblProperties.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblProperties.setFont(SimLive.FONT_BOLD);
		
		Label lblXcoordinate = new Label(this, SWT.NONE);
		lblXcoordinate.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblXcoordinate.setText("x-Coord:");
		
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
				double[] newCoords = connector.getCoordinates();
				newCoords[0] = SimLive.getInputDouble(text);
				connector.setCoordinates(newCoords, true);
				SimLive.view.redraw();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getLengthUnit());
		
		Label lblYcoordinate = new Label(this, SWT.NONE);
		lblYcoordinate.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblYcoordinate.setText("y-Coord:");
		
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
				double[] newCoords = connector.getCoordinates();
				newCoords[1] = SimLive.getInputDouble(text_1);
				connector.setCoordinates(newCoords, true);
				SimLive.view.redraw();
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getLengthUnit());
		
		Label lblZcoordinate = new Label(this, SWT.NONE);
		lblZcoordinate.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblZcoordinate.setText("z-Coord:");
		
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
				double[] newCoords = connector.getCoordinates();
				newCoords[2] = SimLive.getInputDouble(text_2);
				connector.setCoordinates(newCoords, true);
				SimLive.view.redraw();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText(Units.getLengthUnit());
		
		Label lblType = new Label(this, SWT.NONE);
		lblType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblType.setText("Type:");
		
		combo = new Combo(this, SWT.READ_ONLY);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				connector.setType(Connector.Type.values()[combo.getSelectionIndex()]);
				SimLive.view.redraw();
			}
		});
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		coordsSet(connector);
	}
	
	private void setCombo(Connector connector) {
		if (connector.getElement0() != null && connector.getElement0().getType() == Element.Type.BEAM &&
			connector.getElement1() != null && connector.getElement1().getType() == Element.Type.BEAM) {
			combo.setItems(new String[] {"Spherical", "Fixed", "Revolute"});
		}
		else if (connector.getElement0() != null &&
				 connector.getElement0().getType() != Element.Type.ROD &&
				 connector.getElement0().getType() != Element.Type.SPRING &&
				 connector.getElement1() != null &&
				 connector.getElement1().getType() != Element.Type.ROD &&
				 connector.getElement1().getType() != Element.Type.SPRING) {
			combo.setItems(new String[] {"Spherical", "Fixed"});
		}
		else {
			combo.setItems(new String[] {"Spherical"});
		}
		combo.select(connector.getType().ordinal());
	}
	
	public void coordsSet(Connector connector) {
		if (connector.isCoordsSet()) {
			text.setText(SimLive.double2String(connector.getCoordinates()[0]));
			text_1.setText(SimLive.double2String(connector.getCoordinates()[1]));
			text_2.setText(SimLive.double2String(connector.getCoordinates()[2]));
		}
		else {
			text.setText("");
			text_1.setText("");
			text_2.setText("");
		}
		text.setEnabled(connector.isCoordsSet());
		text_1.setEnabled(connector.isCoordsSet());
		text_2.setEnabled(connector.isCoordsSet() && !Model.twoDimensional);
		
		setCombo(connector);
		
		setOKLabel(1, connector.isCoordsSet());
	}

}

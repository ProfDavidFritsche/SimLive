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
import simlive.model.Load;
import simlive.model.Model;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Combo;

public class LoadDialog extends StoreDialog {
	
	private Composite composite;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public LoadDialog(final Composite parent, int style, final Load load) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText(load.name);
			
		Composite composite_1 = new Composite(this, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gl_composite = new GridLayout(3, false);
		SimLive.formatGridLayoutForComposite(gl_composite);
		composite_1.setLayout(gl_composite);
		
		Label lblIcon = new Label(composite_1, SWT.NONE);
		lblIcon.setImage(SimLive.resize(SimLive.INFO_ICON, SimLive.ICON_HEIGHT_FACTORS[2]));
		
		Label lblStoreNodes = new Label(composite_1, SWT.NONE);
		lblStoreNodes.setFont(SimLive.FONT_BOLD);
		lblStoreNodes.setText("Store Nodes");
		
		lblOK[0] = new Label(composite_1, SWT.NONE);
		lblOK[0].setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		setOKLabel(0, !load.getNodes().isEmpty());
		
		Label lblProperties = new Label(this, SWT.NONE);
		lblProperties.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblProperties.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblProperties.setFont(SimLive.FONT_BOLD);
		lblProperties.setText("Properties");
			
		Label lblType = new Label(this, SWT.NONE);
		lblType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblType.setText("Type:");
		
		final Combo combo = new Combo(this, SWT.READ_ONLY);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {				
				load.setType(Load.Type.values()[combo.getSelectionIndex()]);
				Composite p = composite.getParent();
				composite.dispose();
				if (combo.getSelectionIndex() == 0) {
					composite = new ForceDialog(p, SWT.NONE, load);
				}
				else {
					composite = new DisplacementDialog(p, SWT.NONE, load);
				}
				composite.moveBelow(combo);
				p.layout();
				SimLive.view.redraw();
			}
		});
		combo.setItems(new String[] {"Force/Moment", "Displacement/Rotation"});
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		if (load.getType() == Load.Type.FORCE) {
			combo.select(Load.Type.FORCE.ordinal());
			composite = new ForceDialog(this, SWT.NONE, load);
		}
		else {
			combo.select(Load.Type.DISPLACEMENT.ordinal());
			composite = new DisplacementDialog(this, SWT.NONE, load);
		}
		
		Label lblCoordinateSystem = new Label(this, SWT.NONE);
		lblCoordinateSystem.setText("Coordinate System");
		lblCoordinateSystem.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblCoordinateSystem.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblCoordinateSystem.setFont(SimLive.FONT_BOLD);
		
		/*Composite composite_2 = new Composite(this, SWT.NONE);
		composite_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		GridLayout gl_composite_1 = new GridLayout(2, false);
		Sim2d.formatGridLayoutForComposite(gl_composite_1);
		composite_2.setLayout(gl_composite_1);
		
		Label lblIcon_1 = new Label(composite_2, SWT.NONE);
		lblIcon_1.setImage(Sim2d.resize(SimLive.INFO_ICON, Sim2d.ICON_HEIGHT_FACTORS[2]));
		
		Label lblSelectReferenceNode = new Label(composite_2, SWT.NONE);
		lblSelectReferenceNode.setFont(Sim2d.FONT_BOLD);
		lblSelectReferenceNode.setText("Select Reference Node");
		
		Button btnStore_1 = new Button(this, SWT.NONE);
		btnStore_1.setToolTipText("Store Selected Node");
		btnStore_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				load.setReferenceNode(Sim2d.view.getSelectedNodes().get(0));
				Sim2d.view.deselectAll();
				Sim2d.view.redraw();
			}
		});
		btnStore_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnStore_1.setText("Store");
		btnStore_1.setEnabled(false);*/
		
		Label lblAxis = new Label(this, SWT.NONE);
		lblAxis.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAxis.setText("Axis:");
		
		Text text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setAxis(SimLive.getInputDouble(text), 0);
				SimLive.view.redraw();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text.setText(SimLive.double2String(load.getAxis()[0]));
		text.setEnabled(!Model.twoDimensional);
		
		Text text_1 = new Text(this, SWT.BORDER);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setAxis(SimLive.getInputDouble(text_1), 1);
				SimLive.view.redraw();
			}
		});
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_1.setText(SimLive.double2String(load.getAxis()[1]));
		text_1.setEnabled(!Model.twoDimensional);
		
		Text text_2 = new Text(this, SWT.BORDER);
		text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text_2.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_2.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				load.setAxis(SimLive.getInputDouble(text_2), 2);
				SimLive.view.redraw();
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		if (Model.twoDimensional) {
			load.setAxis(1, 2);
		}
		text_2.setText(SimLive.double2String(load.getAxis()[2]));
		text_2.setEnabled(!Model.twoDimensional);
		
		Label lblAngle = new Label(this, SWT.NONE);
		lblAngle.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblAngle.setText("Angle:");
		
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
				load.setAngle(SimLive.getInputDouble(text_3, -360, 360));
				SimLive.view.redraw();
			}
		});
		text_3.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_3.setText(SimLive.double2String(load.getAngle()));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText("\u00B0");
		
		TimeDependencyDialog timeDependencyDialog = new TimeDependencyDialog(this, SWT.NONE, load.getTimeTable());
		timeDependencyDialog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}

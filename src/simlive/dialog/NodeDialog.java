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
import simlive.model.Model;
import simlive.model.Node;
import simlive.model.Set;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class NodeDialog extends Composite {
	
	private Node node;
	private Set setByNode;
	private Text text, text_1, text_2;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public NodeDialog(Composite parent, int style, final Node node) {
		super(parent, style);
		this.node = node;
		this.setByNode = SimLive.model.getSetsByNode(node).get(0);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setText("Node "+(node.getID()+1));
		
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
				setNodes(node);
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
				setNodes(node);
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
				setNodes(node);
			}
		});
		text_2.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
			}
		});
		text_2.setEnabled(!Model.twoDimensional);
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText(Units.getLengthUnit());
		
		text.setText(SimLive.double2String(node.getXCoord()));
		text_1.setText(SimLive.double2String(node.getYCoord()));
		text_2.setText(SimLive.double2String(node.getZCoord()));
		
		updateDialog(new double[3]);
		
		parent.layout();
	}
	
	public void updateDialog(double[] deltaMove) {
		if (setByNode.getType() != Set.Type.BASIC) {
			for (int n = 0; n < setByNode.getNodes().size(); n++) {
				setByNode.getNodes().get(n).setXCoord(setByNode.getNodes().get(n).getXCoord()+deltaMove[0]);
				setByNode.getNodes().get(n).setYCoord(setByNode.getNodes().get(n).getYCoord()+deltaMove[1]);
				setByNode.getNodes().get(n).setZCoord(setByNode.getNodes().get(n).getZCoord()+deltaMove[2]);
			}
			//splitSets(deltaMove[2]);
		}
		else {
			node.setXCoord(node.getXCoord()+deltaMove[0]);
			node.setYCoord(node.getYCoord()+deltaMove[1]);
			node.setZCoord(node.getZCoord()+deltaMove[2]);
		}
		
		for (int c = 0; c < SimLive.model.getConnectors().size(); c++) {
			SimLive.model.getConnectors().get(c).updateCoordinates();
		}
		
		text.setText(SimLive.double2String(node.getXCoord()));
		text_1.setText(SimLive.double2String(node.getYCoord()));
		text_2.setText(SimLive.double2String(node.getZCoord()));
		
		SimLive.model.updateAllDistributedLoads();
		SimLive.model.updateAllElements();
	}
	
	private void setNodes(final Node node) {
		double[] coords = node.getCoords();
		double[] deltaMove = new double[3];
		deltaMove[0] = SimLive.getInputDouble(text)-coords[0];
		deltaMove[1] = SimLive.getInputDouble(text_1)-coords[1];
		deltaMove[2] = SimLive.getInputDouble(text_2)-coords[2];
		updateDialog(deltaMove);
		SimLive.view.redraw();
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
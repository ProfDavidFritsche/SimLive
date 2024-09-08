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
import org.eclipse.swt.widgets.TreeItem;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.Section;

public class SectionRectangleDialog extends Composite {
	
	private Text text_2, text_3, text_4, text_5;

	public SectionRectangleDialog(final Composite parent, int style,
			final Section section, final TreeItem treeItem) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblWidth = new Label(this, SWT.NONE);
		lblWidth.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblWidth.setText("Width:");
		
		final Text text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				double value = SimLive.getInputDouble(text);
				section.getSectionShape().setWidth(value);
				text_2.setText(SimLive.double2String(section.getArea()));
				text_3.setText(SimLive.double2String(section.getIy()));
				text_4.setText(SimLive.double2String(section.getIz()));
				text_5.setText(SimLive.double2String(section.getIt()));
				((SectionDialog) parent).updateTreeAndCanvas(treeItem, section);
			}
		});
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text.setText(SimLive.double2String(section.getSectionShape().getWidth()));
		
		Label lblUnit = new Label(this, SWT.NONE);
		lblUnit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit.setText(Units.getLengthUnit());
		
		Label lblHeight = new Label(this, SWT.NONE);
		lblHeight.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblHeight.setText("Height:");
		
		final Text text_1 = new Text(this, SWT.BORDER);
		text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_1.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				double value = SimLive.getInputDouble(text_1);
				section.getSectionShape().setHeight(value);
				text_2.setText(SimLive.double2String(section.getArea()));
				text_3.setText(SimLive.double2String(section.getIy()));
				text_4.setText(SimLive.double2String(section.getIz()));
				text_5.setText(SimLive.double2String(section.getIt()));
				((SectionDialog) parent).updateTreeAndCanvas(treeItem, section);
			}
		});
		text_1.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text_1.notifyListeners(SWT.DefaultSelection, new Event());
			}
		});
		text_1.setText(SimLive.double2String(section.getSectionShape().getHeight()));
		
		Label lblUnit_1 = new Label(this, SWT.NONE);
		lblUnit_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_1.setText(Units.getLengthUnit());
		
		Label lblArea = new Label(this, SWT.NONE);
		lblArea.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblArea.setText("Area:");
		
		text_2 = new Text(this, SWT.BORDER);
		text_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_2.setEditable(false);
		text_2.setText(SimLive.double2String(section.getArea()));
		
		Label lblUnit_2 = new Label(this, SWT.NONE);
		lblUnit_2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_2.setText(Units.getLengthUnit()+"\u00B2");
		
		Label lblIy = new Label(this, SWT.NONE);
		lblIy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblIy.setText("Iy:");
		
		text_3 = new Text(this, SWT.BORDER);
		text_3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_3.setEditable(false);
		text_3.setText(SimLive.double2String(section.getIy()));
		
		Label lblUnit_3 = new Label(this, SWT.NONE);
		lblUnit_3.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_3.setText(Units.getLengthUnit()+"\u2074");
		
		Label lblIz = new Label(this, SWT.NONE);
		lblIz.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblIz.setText("Iz:");
		
		text_4 = new Text(this, SWT.BORDER);
		text_4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_4.setEditable(false);
		text_4.setText(SimLive.double2String(section.getIz()));
		
		Label lblUnit_4 = new Label(this, SWT.NONE);
		lblUnit_4.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_4.setText(Units.getLengthUnit()+"\u2074");
		
		Label lblIt = new Label(this, SWT.NONE);
		lblIt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblIt.setText("It:");
		
		text_5 = new Text(this, SWT.BORDER);
		text_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text_5.setEditable(false);
		text_5.setText(SimLive.double2String(section.getIt()));
		
		Label lblUnit_5 = new Label(this, SWT.NONE);
		lblUnit_5.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		lblUnit_5.setText(Units.getLengthUnit()+"\u2074");
	}

}

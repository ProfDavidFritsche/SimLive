package simlive.dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;

import simlive.SimLive;
import simlive.model.Section;
import simlive.model.SectionShape;

import org.eclipse.wb.swt.SWTResourceManager;

public class SectionDialog extends Composite {
	
	private Composite composite;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public SectionDialog(final Composite parent, int style,
            				final Section section, final TreeItem treeItem) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		GridLayout gl_composite = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gl_composite);
		this.setLayout(gl_composite);
		
		Label lblTitle = new Label(this, SWT.NONE);
		lblTitle.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTitle.setFont(SimLive.FONT_BOLD);
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTitle.setText(section.getName());
		
		Label lblShape = new Label(this, SWT.NONE);
		lblShape.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblShape.setText("Shape:");
		
		final Combo combo = new Combo(this, SWT.READ_ONLY);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		combo.setItems(new String[] {"Rectangle", "Hollow Rectangle", "Circle", "Hollow Circle", "Direct Input"});
		combo.select(section.getSectionShape().getType().ordinal());
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				section.getSectionShape().setType(SectionShape.Type.values()[combo.getSelectionIndex()]);
				composite.dispose();
				composite = getSectionComposite(section.getSectionShape(), SectionDialog.this, section, treeItem);
				parent.layout();
				layout();
				updateTreeAndCanvas(treeItem, section);
			}
		});
		
		composite = getSectionComposite(section.getSectionShape(), this, section, treeItem);
	}
	
	private Composite getSectionComposite(SectionShape sectionShape, final Composite parent,
			final Section section, final TreeItem treeItem) {
		switch (sectionShape.getType()) {
			case RECTANGLE:
				return new SectionRectangleDialog(parent, SWT.NONE, section, treeItem);
			case HOLLOW_RECTANGLE:
				return new SectionHollowRectangleDialog(parent, SWT.NONE, section, treeItem);
			case CIRCLE:
				return new SectionCircleDialog(parent, SWT.NONE, section, treeItem);
			case HOLLOW_CIRCLE:
				return new SectionHollowCircleDialog(parent, SWT.NONE, section, treeItem);
			case DIRECT_INPUT:
				return new SectionDirectInputDialog(parent, SWT.NONE, section, treeItem);
		}
		return null;
	}
	
	public void updateTreeAndCanvas(final TreeItem treeItem, final Section section) {
		if (treeItem != null && section != null) {
			treeItem.setText(section.getName());
			//Text text = (Text) table.getChildren()[0];
			//text.setText(section.getName());
		}
		((Label) getChildren()[0]).setText(section.getName());
		SimLive.view.redraw();
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

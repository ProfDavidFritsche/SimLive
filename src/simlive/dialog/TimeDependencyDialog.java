package simlive.dialog;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.SWTResourceManager;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.TimeTable;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;

public class TimeDependencyDialog extends Composite {
	
	private Table table;
	private Composite composite;
	private Button btnRow_1;
	private Label label;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public TimeDependencyDialog(final Composite parent, int style, final TimeTable timeTable) {
		super(parent, SWT.NONE);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblTimeDependency = new Label(this, SWT.NONE);
		lblTimeDependency.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTimeDependency.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTimeDependency.setFont(SimLive.FONT_BOLD);
		lblTimeDependency.setText("Time Dependency");

		label = new Label(this, SWT.NONE);
		composite = getTable(timeTable);
		
		new Label(this, SWT.NONE);
		
		Button btnImport = new Button(this, SWT.NONE);
		btnImport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				FileDialog fileDialog = new FileDialog(SimLive.shell, SWT.OPEN);
				String[] filter = new String[1];
				filter[0] = "*.csv";
				fileDialog.setFilterExtensions(filter);						
				if (fileDialog.open() != null) {
					TimeTable importTimeTable = importTimeTable(fileDialog.getFilterPath()+
							System.getProperty("file.separator")+fileDialog.getFileName());
					if (importTimeTable != null) {
						timeTable.setNumberOfRows(importTimeTable.getNumberOfRows());
						for (int i = 0; i < importTimeTable.getNumberOfRows(); i++) {
							timeTable.setTime(importTimeTable.getTime(i), i);
							timeTable.setFactor(importTimeTable.getFactor(i), i);
						}
						composite.dispose();
						composite = getTable(timeTable);
						composite.moveBelow(label);
						composite.getParent().layout();
						btnRow_1.setEnabled(timeTable.getNumberOfRows() > 1);
					}
					else {
						SimLive.messageBox(true, "Error reading from file.");
					}
				}
			}
		});
		btnImport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnImport.setText("Import...");
		
		Button btnRow = new Button(this, SWT.NONE);
		btnRow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = timeTable.getNumberOfRows()+1;
				timeTable.setNumberOfRows(value);
				composite.dispose();
				composite = getTable(timeTable);
				composite.moveBelow(label);
				composite.getParent().layout();
				btnRow_1.setEnabled(timeTable.getNumberOfRows() > 1);
				table.setSelection(table.getItemCount()-1);
				table.showSelection();
			}
		});
		btnRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnRow.setText("Row +");
		
		btnRow_1 = new Button(this, SWT.NONE);
		btnRow_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = timeTable.getNumberOfRows()-1;
				timeTable.setNumberOfRows(value);
				composite.dispose();
				composite = getTable(timeTable);
				composite.moveBelow(label);
				composite.getParent().layout();
				btnRow_1.setEnabled(timeTable.getNumberOfRows() > 1);
				table.setSelection(table.getItemCount()-1);
				table.showSelection();
			}
		});
		btnRow_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnRow_1.setEnabled(timeTable.getNumberOfRows() > 1);
		btnRow_1.setText("Row -");
	}
	
	private TimeTable importTimeTable(String fileName) {
		BufferedReader br = null;
        String csvSplitBy = ";";
        String line = "";
        ArrayList<String[]> records = new ArrayList<String[]>();
        
        try {

            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {

            	// use semicolon as separator
            	String[] strings = line.split(csvSplitBy);
                
            	records.add(strings);
            }
            br.close();
            
            TimeTable timeTable = new TimeTable();
            timeTable.setNumberOfRows(records.size());
        	for (int i = 0; i < records.size(); i++) {
            	timeTable.setTime(SimLive.string2Double(records.get(i)[0]), i);
            	timeTable.setFactor(SimLive.string2Double(records.get(i)[1]), i);
            }
            return timeTable;

        }
        catch (Exception e) {
        }
        
        return null;
	}
	
	private Composite getTable(final TimeTable timeTable) {
		int rows = timeTable.getNumberOfRows();
		
		Composite composite = new Composite(this, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		GridLayout gl_composite = new GridLayout(1, false);
		SimLive.formatGridLayoutForComposite(gl_composite);
		composite.setLayout(gl_composite);
		
		table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.VIRTUAL | SWT.NO_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tblclmnTime = new TableColumn(table, SWT.NONE);
		tblclmnTime.setResizable(false);
		tblclmnTime.setText("Time ["+Units.getTimeUnit()+"]");
		
		TableColumn tblclmnFactor = new TableColumn(table, SWT.NONE);
		tblclmnFactor.setResizable(false);
		tblclmnFactor.setText("Load Factor");
		
		for (int i = 0; i < rows; i++) {
			TableItem tableItem = new TableItem(table, SWT.NONE);
			String[] strArray = new String[] {"", ""};
			if (i < timeTable.getNumberOfRows()) {
				strArray[0] = SimLive.double2String(timeTable.getTime(i));
				strArray[1] = SimLive.double2String(timeTable.getFactor(i));
			}
			tableItem.setText(strArray);
		}
		
		final TableEditor editor = new TableEditor(table);
	    editor.horizontalAlignment = SWT.LEFT;
	    editor.grabHorizontal = true;

	    table.addMouseListener(new MouseAdapter() {
	    	public void mouseDown(MouseEvent event) {
		        Control old = editor.getEditor();
		        if (old != null) {
		        	old.dispose();
		        }
	
		        Point pt = new Point(event.x, event.y);
	
		        final TableItem item = table.getItem(pt);
		        if (item == null) {
		        	return;
		        }
		        
		        int column = -1;
		        for (int i = 0, n = table.getColumnCount(); i < n; i++) {
		        	Rectangle rect = item.getBounds(i);
		        	if (rect.contains(pt)) {
		        		column = i;
		        		break;
		        	}
		        }
	
		        final Text text = new Text(table, SWT.NONE);
		        text.setForeground(item.getForeground());
	
		        text.setText(item.getText(column));
		        text.setForeground(item.getForeground());
		        text.selectAll();
		        text.setFocus();
	
		        editor.minimumWidth = text.getBounds().width;
	
		        editor.setEditor(text, item, column);
	
		        final int col = column;
		        text.addModifyListener(new ModifyListener() {
		        	public void modifyText(ModifyEvent event) {
		        		double value = SimLive.string2Double(text.getText());	        			
		        		/*if (col == 1) {
			        		if (value > 1.0) {
		        				value = 1.0;
		        			}
			        		if (value < -1.0) {
		        				value = -1.0;
		        			}
		        		}*/
		        		item.setText(col, SimLive.double2String(value));		        		
	        			if (col == 0) {
	        				timeTable.setTime(value, table.getSelectionIndex());
	        			}
	        			if (col == 1) {
	        				timeTable.setFactor(value, table.getSelectionIndex());
	        			}		        		
		        	}
		        });
		        text.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent arg0) {
						if (!SimLive.isInputValid(arg0, col == 1)) arg0.doit = false;
					}
				});
		    }
	    });
	    table.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				tblclmnTime.setWidth(table.getClientArea().width/2);
				tblclmnFactor.setWidth(table.getClientArea().width/2);
			}
		});
	    
	    return composite;
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

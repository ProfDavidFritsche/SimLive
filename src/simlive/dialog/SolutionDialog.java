package simlive.dialog;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import simlive.SimLive;
import simlive.misc.Settings;
import simlive.model.Model;
import simlive.solution.ConstraintMethod;
import simlive.solution.Increment;
import simlive.solution.Solution;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.events.VerifyEvent;

public class SolutionDialog extends Composite {

	private Combo combo;
	private ProgressBar progressBar;
	private StyledText styledText;
	private int logIndex, progressBarInc, progressBarMax;
	private final int PROGRESS_BAR = 1000;
	private Composite resultComposite;
	private Solution solution = null;
	private Composite composite;
	public static Thread thread;
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public SolutionDialog(final Composite parent, int style, final Model model,
			final Settings settings) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblConstraints = new Label(this, SWT.NONE);
		lblConstraints.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblConstraints.setFont(SimLive.FONT_BOLD);
		lblConstraints.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblConstraints.setText("Constraints");
		
		Label lblMethod = new Label(this, SWT.NONE);
		lblMethod.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblMethod.setText("Method:");
		
		combo = new Combo(this, SWT.READ_ONLY);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.constraintType = ConstraintMethod.Type.values()[combo.getSelectionIndex()];
				composite.dispose();
				composite = getComposite(SolutionDialog.this, model, settings);
				composite.moveBelow(combo);
				parent.layout();
				layout();
			}
		});
		combo.setItems(new String[] {"Lagrange Multipliers", "Penalty Method"});
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		combo.select(settings.constraintType.ordinal());
		
		composite = getComposite(this, model, settings);
		
		Label lblSolverLog = new Label(this, SWT.NONE);
		lblSolverLog.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblSolverLog.setFont(SimLive.FONT_BOLD);
		lblSolverLog.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblSolverLog.setText("Solver Log");
		
		styledText = new StyledText(this, SWT.V_SCROLL | SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		
		fillLogComplete();
	}
	
	private Composite getComposite(final Composite parent, final Model model, final Settings settings) {		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		composite.setLayout(gridLayout);
		
		if (settings.constraintType == ConstraintMethod.Type.PENALTY_METHOD) {
			Label lblFactor = new Label(composite, SWT.NONE);
			lblFactor.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblFactor.setText("Factor:");
			
			final Text text_1 = new Text(composite, SWT.BORDER);
			text_1.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {
					double value = SimLive.getInputDouble(text_1);
					settings.penaltyFactor = value;
				}
			});
			text_1.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent arg0) {
					text_1.notifyListeners(SWT.DefaultSelection, new Event());
				}
			});
			text_1.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
				}
			});
			text_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			text_1.setText(SimLive.double2String(settings.penaltyFactor));
			new Label(composite, SWT.NONE);
		}
		
		Label lblSettings = new Label(composite, SWT.NONE);
		lblSettings.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblSettings.setFont(SimLive.FONT_BOLD);
		lblSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblSettings.setText("Settings");
		new Label(composite, SWT.NONE);
		
		final Button btnReorderNodes = new Button(composite, SWT.CHECK);
		btnReorderNodes.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnReorderNodes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				settings.isReorderNodes = btnReorderNodes.getSelection();
			}
		});
		btnReorderNodes.setText("Reorder Nodes");
		btnReorderNodes.setSelection(settings.isReorderNodes);
		new Label(composite, SWT.NONE);
		
		final Button btnLargeDisplacement = new Button(composite, SWT.CHECK);
		btnLargeDisplacement.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnLargeDisplacement.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				settings.isLargeDisplacement = btnLargeDisplacement.getSelection();
			}
		});
		btnLargeDisplacement.setText("Large Displacement");
		btnLargeDisplacement.setSelection(settings.isLargeDisplacement);
		new Label(composite, SWT.NONE);
		
		final Button btnWriteMatrixView = new Button(composite, SWT.CHECK);
		btnWriteMatrixView.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		btnWriteMatrixView.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.isWriteMatrixView = btnWriteMatrixView.getSelection();
			}
		});
		btnWriteMatrixView.setText("Write Matrix View");
		btnWriteMatrixView.setSelection(settings.isWriteMatrixView);
		
		Label lblSolution = new Label(composite, SWT.NONE);
		lblSolution.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblSolution.setFont(SimLive.FONT_BOLD);
		lblSolution.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblSolution.setText("Solution");
		
		progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		progressBar.setMaximum(PROGRESS_BAR);
		
		resultComposite = new Composite(composite, SWT.NONE);
		resultComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		GridLayout gridLayout1 = new GridLayout(2, false);
		SimLive.formatGridLayoutForComposite(gridLayout1);
		resultComposite.setLayout(gridLayout1);
		
		new Label(resultComposite, SWT.NONE);
		
		Label lblResult = new Label(resultComposite, SWT.NONE);
		lblResult.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lblResult.setFont(SimLive.FONT_BOLD);
		
		SimLive.setResultLabel(resultComposite, false, false, false);
		
		final Button btnSolve = new Button(composite, SWT.NONE);
		btnSolve.setToolTipText("Calculate Solution");
		btnSolve.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				styledText.setText("");
				logIndex = 0;
				
				thread = new Thread(new Runnable() {			
					public void run() {
						try {
							SimLive.freezeGUI(true);
							
							Solution.resetLog();

							if (settings.isReorderNodes) {
								SimLive.shell.getDisplay().asyncExec(new Runnable() {
									public void run() {
										SimLive.setResultLabel(resultComposite, false, true, false);
									}
								});
								model.reorderNodes();
							}
							
							SimLive.shell.getDisplay().asyncExec(new Runnable() {
								public void run() {
									SimLive.setResultLabel(resultComposite, true, false, false);
								}
							});
							model.finalUpdateModel();
							
							SimLive.shell.getDisplay().asyncExec(new Runnable() {
								public void run() {
									SimLive.setResultLabel(resultComposite, false, false, true);
								}
							});
							
							solution = new Solution(model, settings);
							
							if (solution.checkModel()) {
								solution.calculate(SolutionDialog.this);
								if (solution.getNumberOfIncrements() > 0) {
									SimLive.initPost(solution);
								}
							}
							
							SimLive.shell.getDisplay().asyncExec(new Runnable() {
								public void run() {
									progressBar.setSelection(0);
									SimLive.setResultLabel(resultComposite, false, false, false);
									fillLogComplete();
								}
							});
							
							SimLive.freezeGUI(false);
						} catch (Exception e) {}
					}
				});
				thread.start();
			}
		});
		btnSolve.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnSolve.setText("Solve");
		
		return composite;
	}
	
	public boolean resultsAvailable() {
		if (solution != null) {
			ArrayList<Increment> increments = new ArrayList<Increment>();
			for (int inc = 0; inc < solution.getNumberOfIncrements(); inc++) {
				if (solution.getIncrement(inc) != null && solution.getIncrement(inc).get_u_global() != null) {
					increments.add(solution.getIncrement(inc));
				}
			}
			if (increments.size() > 0) {
				solution.setIncrements(increments.toArray(new Increment[increments.size()]));
				Solution.errors.add("Stopped by user. " + " Results are available.");
				SimLive.initPost(solution);
				return true;
			}
		}
		return false;
	}
	
	/*public void setToNoSolution() {
		styledText.setText("");
		Sim2d.setResultLabel(null, lblResult, false, false, 32);
	}*/
	
	public void fillLogComplete() {			
		SimLive.shell.getDisplay().asyncExec(new Runnable() {
			public void run() {		
				String str = "";
				for (int i = 0; i < Solution.log.size(); i++) {
					str += Solution.log.get(i)+"\n";
				}
				for (int i = 0; i < Solution.errors.size(); i++) {
					str += "ERROR: "+Solution.errors.get(i)+"\n";
				}
				for (int i = 0; i < Solution.warnings.size(); i++) {
					str += "WARNING: "+Solution.warnings.get(i)+"\n";
				}
				styledText.setText(str);
				styledText.setTopIndex(styledText.getLineCount()-1);
			}
		});
	}
	
	public void updateLog() {
		SimLive.shell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				final int logSize = Solution.log.size();
				if (!styledText.isDisposed() && logSize > logIndex) {
					String text = styledText.getText();
					for (int i = logIndex; i < logSize; i++) {
						text += Solution.log.get(i)+"\n";
					}
					logIndex = logSize;
					if (styledText.getTopIndex() > 0) {
						int index = 0;
						for (int i = 0; i < styledText.getTopIndex(); i++) {
							index += styledText.getLine(i).length()+1;
						}
						text = text.substring(index);
					}
					styledText.setText(text);
					styledText.setTopIndex(styledText.getLineCount()-1);
				}
			}
		});
	}
	
	public void initProgressBar(final int nInc) {
		progressBarInc = 0;
		progressBarMax = nInc;
	}
	
	public void incrementProgressBar() {
		progressBarInc++;
		SimLive.shell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				double fraction = progressBarInc/(double) progressBarMax;
				int inc = (int) Math.floor(fraction*PROGRESS_BAR);
				if (!progressBar.isDisposed() && inc > progressBar.getSelection()) {
					progressBar.setSelection(inc);
				}
			}
		});
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}

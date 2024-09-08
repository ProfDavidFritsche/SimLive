package simlive.dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import simlive.SimLive;
import simlive.misc.Settings;
import simlive.misc.Units;
import simlive.model.Element;
import simlive.model.Step;
import simlive.postprocessing.Post;
import simlive.postprocessing.ScalarPlot;
import simlive.postprocessing.TensorPlot;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.wb.swt.SWTResourceManager;

import Jama.Matrix;

public class ResultsDialog extends Composite {
	
	private Composite dialog;
	private Slider slider;
	private Text text;
	private Combo combo, combo_1;
	private Button btnShowMinmax;
	private Button btnPlay;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public ResultsDialog(final Composite parent, int style, final Post post, Settings settings) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		Label lblDeformation = new Label(this, SWT.NONE);
		lblDeformation.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblDeformation.setFont(SimLive.FONT_BOLD);
		lblDeformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblDeformation.setText("Deformation");
		
		Label lblScaling = new Label(this, SWT.NONE);
		lblScaling.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblScaling.setText("Scaling:");
		
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
				double value = SimLive.getInputDouble(text);
				post.setScaling(value);
				SimLive.view.redraw();
			}
		});
		text.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent arg0) {
				if (!SimLive.isInputValid(arg0, false)) arg0.doit = false;
			}
		});
		text.setEnabled(!post.isAutoScaling());
		text.setText(SimLive.double2String(post.getScaling()));
		
		final Button btnAuto = new Button(this, SWT.CHECK);
		btnAuto.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		btnAuto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnAuto.getSelection()) {
					post.setAutoScaling(true);
					text.setEnabled(false);
					text.setText(SimLive.double2String(post.getScaling()));
					SimLive.view.redraw();
				}
				else {
					post.setAutoScaling(false);
					text.setEnabled(true);
				}
			}
		});
		btnAuto.setSelection(post.isAutoScaling());
		btnAuto.setText("Auto");
		
		Label lblTimeIncrement = new Label(this, SWT.NONE);
		lblTimeIncrement.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblTimeIncrement.setFont(SimLive.FONT_BOLD);
		lblTimeIncrement.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblTimeIncrement.setText("Time Increment");
		
		slider = new Slider(this, SWT.NONE);
		slider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		slider.setMaximum(post.getSolution().getNumberOfIncrements()+slider.getThumb()); 
		slider.setMinimum(0);
		setSliderValue(post.getPostIncrementID());
		slider.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = slider.getSelection();
				post.setPostIncrementID(value);
				slider.setToolTipText(SimLive.double2String(value)+"/"+
						SimLive.post.getSolution().getNumberOfIncrements());
				SimLive.post.updateMinMaxLabels();
				SimLive.view.redraw();
				SimLive.updateMatrixView();
			}
		});
		
		Composite composite = new Composite(this, SWT.NONE);
		GridLayout gridLayout1 = new GridLayout(6, true);
		SimLive.formatGridLayoutForComposite(gridLayout1);
		composite.setLayout(gridLayout1);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Button btnStart = new Button(composite, SWT.NONE);
		btnStart.setToolTipText("Start");
		btnStart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (SimLive.view.isAnimationRunning()) {
					SimLive.view.stopAnimation();
					btnPlay.setImage(SimLive.PLAY_ICON);
					btnPlay.setToolTipText("Play");
				}
				post.setPostIncrementID(0);
				setSliderValue(SimLive.post.getPostIncrementID());
				SimLive.post.updateMinMaxLabels();
				SimLive.view.redraw();
				SimLive.updateMatrixView();
			}
		});
		btnStart.setImage(SimLive.START_ICON);
		btnStart.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Button btnPrevious = new Button(composite, SWT.NONE);
		btnPrevious.setToolTipText("Previous Step");
		btnPrevious.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (SimLive.view.isAnimationRunning()) {
					SimLive.view.stopAnimation();
					btnPlay.setImage(SimLive.PLAY_ICON);
					btnPlay.setToolTipText("Play");
				}
				post.setPostIncrementIDtoStartOfStep();
				setSliderValue(SimLive.post.getPostIncrementID());
				SimLive.post.updateMinMaxLabels();
				SimLive.view.redraw();
				SimLive.updateMatrixView();
			}
		});
		btnPrevious.setImage(SimLive.PREVIOUS_ICON);
		btnPrevious.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnPlay = new Button(composite, SWT.NONE);
		btnPlay.setToolTipText("Play");
		btnPlay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (SimLive.view.isAnimationRunning()) {
					SimLive.view.stopAnimation();
					btnPlay.setImage(SimLive.PLAY_ICON);
					btnPlay.setToolTipText("Play");
				}
				else {
					SimLive.view.startAnimation();
					btnPlay.setImage(SimLive.PAUSE_ICON);
					btnPlay.setToolTipText("Pause");
				}
			}
		});
		btnPlay.setImage(SimLive.PLAY_ICON);
		btnPlay.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Button btnNext = new Button(composite, SWT.NONE);
		btnNext.setToolTipText("Next Step");
		btnNext.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (SimLive.view.isAnimationRunning()) {
					SimLive.view.stopAnimation();
					btnPlay.setImage(SimLive.PLAY_ICON);
					btnPlay.setToolTipText("Play");
				}
				post.setPostIncrementIDtoEndOfStep();
				setSliderValue(SimLive.post.getPostIncrementID());
				SimLive.post.updateMinMaxLabels();
				SimLive.view.redraw();
				SimLive.updateMatrixView();
			}
		});
		btnNext.setImage(SimLive.NEXT_ICON);
		btnNext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Button btnEnd = new Button(composite, SWT.NONE);
		btnEnd.setToolTipText("End");
		btnEnd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (SimLive.view.isAnimationRunning()) {
					SimLive.view.stopAnimation();
					btnPlay.setImage(SimLive.PLAY_ICON);
					btnPlay.setToolTipText("Play");
				}
				post.setPostIncrementID(post.getSolution().getNumberOfIncrements());
				setSliderValue(SimLive.post.getPostIncrementID());
				SimLive.post.updateMinMaxLabels();
				SimLive.view.redraw();
				SimLive.updateMatrixView();
			}
		});
		btnEnd.setImage(SimLive.END_ICON);
		btnEnd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		final Button btnReverse = new Button(this, SWT.CHECK);
		btnReverse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				post.setReverseAnimation(btnReverse.getSelection());
			}
		});
		btnReverse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnReverse.setText("Reverse");
		btnReverse.setSelection(post.isReverseAnimation());
		
		Label lblSpeed = new Label(this, SWT.NONE);
		lblSpeed.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblSpeed.setText("Speed:");
		
		final Slider slider_1 = new Slider(this, SWT.NONE);
		slider_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				double value = sliderToValue(slider_1.getSelection());
				post.setAnimationSpeed(value);
				if (SimLive.view.isAnimationRunning()) {
					SimLive.view.stopAnimation();
					SimLive.view.startAnimation();
				}
				slider_1.setToolTipText(SimLive.double2String(value*100.0)+"%");
			}
		});
		slider_1.setThumb(10);
		slider_1.setPageIncrement(10);
		slider_1.setMaximum(100+slider_1.getThumb());
		slider_1.setMinimum(0);
		slider_1.setSelection((int) valueToSlider(post.getAnimationSpeed()));
		slider_1.setToolTipText(SimLive.double2String(sliderToValue(slider_1.getSelection())*100.0)+"%");
		slider_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		if (SimLive.model.getSteps().get(post.getPostIncrement().getStepNr()).type == Step.Type.MODAL_ANALYSIS) {
			Label lblEigenModes = new Label(this, SWT.NONE);
			lblEigenModes.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
			lblEigenModes.setFont(SimLive.FONT_BOLD);
			lblEigenModes.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			lblEigenModes.setText("Eigen Modes");
			
			Label lblFrequency = new Label(this, SWT.NONE);
			lblFrequency.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			lblFrequency.setText("Frequency:");
			
			combo = new Combo(this, SWT.READ_ONLY);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			
			for (int i = 0; i < post.getSolution().getD().getRowDimension(); i++) {
				double f = post.getSolution().getD().get(i, 0)/(2.0*Math.PI);
				combo.add(SimLive.double2String(f) + " " + Units.getFrequencyUnit());
			}
			combo.select(post.getEigenMode());
		
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					final int index = combo.getSelectionIndex();
					
					SimLive.freezeGUI(true);
					
					post.getSolution().calculateIncrementsForEigenmode(index);
					post.setEigenMode(index);
					if (post.isAutoScaling()) {
						post.setAutoScaling(true);
						text.setText(SimLive.double2String(post.getScaling()));
					}
					combo_1.notifyListeners(SWT.Selection, new Event());
					SimLive.view.redraw();
					
					SimLive.freezeGUI(false);
				}
			});
		}
	
		Label lblResultVariables = new Label(this, SWT.NONE);
		lblResultVariables.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		lblResultVariables.setFont(SimLive.FONT_BOLD);
		lblResultVariables.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		lblResultVariables.setText("Result Variables");
		
		Label lblVariable = new Label(this, SWT.NONE);
		lblVariable.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		lblVariable.setText("Variable:");
		
		combo_1 = new Combo(this, SWT.READ_ONLY);
		combo_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		for (int i = 0; i < 13; i++) {
			combo_1.add(ScalarPlot.types[i]);
		}
		
		boolean truss = SimLive.model.doElementsContainType(SimLive.model.getElements(), Element.Type.ROD);
		boolean spring = SimLive.model.doElementsContainType(SimLive.model.getElements(), Element.Type.SPRING);
		boolean beam = SimLive.model.doElementsContainType(SimLive.model.getElements(), Element.Type.BEAM);
		boolean tri = SimLive.model.doElementsContainType(SimLive.model.getElements(), Element.Type.TRI);
		boolean quad = SimLive.model.doElementsContainType(SimLive.model.getElements(), Element.Type.QUAD);
		if (spring) {
			combo_1.add(ScalarPlot.types[13]);
		}
		if (truss || spring || beam) {
			combo_1.add(ScalarPlot.types[14]);
		}
		if (beam) {
			combo_1.add(ScalarPlot.types[15]);
			combo_1.add(ScalarPlot.types[16]);
			combo_1.add(ScalarPlot.types[17]);
			combo_1.add(ScalarPlot.types[18]);
			combo_1.add(ScalarPlot.types[19]);
		}
		if (tri || quad) {
			for (int i = 20; i < 29; i++) {
				combo_1.add(ScalarPlot.types[i]);
			}
		}

		if (post.getScalarPlot() == null) {
			combo_1.select(0);
		}
		else {
			for (int i = 0; i < combo_1.getItemCount(); i++) {
				if (combo_1.getItem(i).equals(post.getScalarPlot().getType())) {
					combo_1.select(i);
				}
			}
		}
		
		combo_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String selection = combo_1.getItem(combo_1.getSelectionIndex());
				
				SimLive.freezeGUI(true);
				
				new Thread(new Runnable() {			
					private ScalarPlot scalarPlot;
					private TensorPlot tensorPlot;
					
					public void run() {
						
						if (selection.equals(ScalarPlot.types[0])) {
						}
						if (selection.equals(ScalarPlot.types[1])) {
							scalarPlot = new ScalarPlot(post.calculateAcceleration(0),
									ScalarPlot.types[1]);
						}
						if (selection.equals(ScalarPlot.types[2])) {
							scalarPlot = new ScalarPlot(post.calculateAcceleration(1),
									ScalarPlot.types[2]);
						}
						if (selection.equals(ScalarPlot.types[3])) {
							scalarPlot = new ScalarPlot(post.calculateAcceleration(2),
									ScalarPlot.types[3]);
						}
						if (selection.equals(ScalarPlot.types[4])) {
							scalarPlot = new ScalarPlot(post.calculateAcceleration(3),
									ScalarPlot.types[4]);
						}
						if (selection.equals(ScalarPlot.types[5])) {
							scalarPlot = new ScalarPlot(post.calculateVelocity(0),
									ScalarPlot.types[5]);
						}
						if (selection.equals(ScalarPlot.types[6])) {
							scalarPlot = new ScalarPlot(post.calculateVelocity(1),
									ScalarPlot.types[6]);
						}
						if (selection.equals(ScalarPlot.types[7])) {
							scalarPlot = new ScalarPlot(post.calculateVelocity(2),
									ScalarPlot.types[7]);
						}
						if (selection.equals(ScalarPlot.types[8])) {
							scalarPlot = new ScalarPlot(post.calculateVelocity(3),
									ScalarPlot.types[8]);
						}
						if (selection.equals(ScalarPlot.types[9])) {
							scalarPlot = new ScalarPlot(post.calculateDisplacement(0),
									ScalarPlot.types[9]);
						}
						if (selection.equals(ScalarPlot.types[10])) {
							scalarPlot = new ScalarPlot(post.calculateDisplacement(1),
									ScalarPlot.types[10]);
						}
						if (selection.equals(ScalarPlot.types[11])) {
							scalarPlot = new ScalarPlot(post.calculateDisplacement(2),
									ScalarPlot.types[11]);
						}
						if (selection.equals(ScalarPlot.types[12])) {
							scalarPlot = new ScalarPlot(post.calculateDisplacement(3),
									ScalarPlot.types[12]);
						}
						if (selection.equals(ScalarPlot.types[13])) {
							scalarPlot = new ScalarPlot(post.calculateSpringDeflection(),
									ScalarPlot.types[13]);
						}
						if (selection.equals(ScalarPlot.types[14])) {
							scalarPlot = new ScalarPlot(post.calculateElementForce(0),
									ScalarPlot.types[14]);
						}
						if (selection.equals(ScalarPlot.types[15])) {
							scalarPlot = new ScalarPlot(post.calculateElementForce(1),
									ScalarPlot.types[15]);
						}
						if (selection.equals(ScalarPlot.types[16])) {
							scalarPlot = new ScalarPlot(post.calculateElementForce(2),
									ScalarPlot.types[16]);
						}
						if (selection.equals(ScalarPlot.types[17])) {
							scalarPlot = new ScalarPlot(post.calculateElementForce(3),
									ScalarPlot.types[17]);
						}
						if (selection.equals(ScalarPlot.types[18])) {
							scalarPlot = new ScalarPlot(post.calculateElementForce(4),
									ScalarPlot.types[18]);
						}
						if (selection.equals(ScalarPlot.types[19])) {
							scalarPlot = new ScalarPlot(post.calculateElementForce(5),
									ScalarPlot.types[19]);
						}					
						if (selection.equals(ScalarPlot.types[20])) {
							Matrix[][][] strain = post.calculateStrain();
							scalarPlot = new ScalarPlot(post.calculatePrincipalStrain(true, strain),
									ScalarPlot.types[20]);
							tensorPlot = new TensorPlot(
									post.calculatePrincipalStrainVectors(true, strain), null);
						}
						if (selection.equals(ScalarPlot.types[21])) {
							Matrix[][][] strain = post.calculateStrain();
							scalarPlot = new ScalarPlot(post.calculatePrincipalStrain(false, strain),
									ScalarPlot.types[21]);
							tensorPlot = new TensorPlot(
									null, post.calculatePrincipalStrainVectors(false, strain));
						}
						if (selection.equals(ScalarPlot.types[22])) {
							Matrix[][][] strain = post.calculateStrain();
							scalarPlot = new ScalarPlot(post.calculateEquivalentStrain(strain),
									ScalarPlot.types[22]);
							tensorPlot = new TensorPlot(
									post.calculatePrincipalStrainVectors(true, strain),
									post.calculatePrincipalStrainVectors(false, strain));
						}
						if (selection.equals(ScalarPlot.types[23])) {
							Matrix[][][] stress = post.calculateStress();
							scalarPlot = new ScalarPlot(post.calculatePrincipalStress(true, stress),
									ScalarPlot.types[23]);
							tensorPlot = new TensorPlot(
									post.calculatePrincipalStressVectors(true, stress), null);
						}
						if (selection.equals(ScalarPlot.types[24])) {
							Matrix[][][] stress = post.calculateStress();
							scalarPlot = new ScalarPlot(post.calculatePrincipalStress(false, stress),
									ScalarPlot.types[24]);
							tensorPlot = new TensorPlot(
									null, post.calculatePrincipalStressVectors(false, stress));
						}
						if (selection.equals(ScalarPlot.types[25])) {
							Matrix[][][] stress = post.calculateStress();
							scalarPlot = new ScalarPlot(post.calculateEquivalentStress(stress, 0),
									ScalarPlot.types[25]);
							tensorPlot = new TensorPlot(
									post.calculatePrincipalStressVectors(true, stress),
									post.calculatePrincipalStressVectors(false, stress));
						}
						if (selection.equals(ScalarPlot.types[26])) {
							Matrix[][][] stress = post.calculateStress();
							scalarPlot = new ScalarPlot(post.calculateEquivalentStress(stress, 1),
									ScalarPlot.types[26]);
							tensorPlot = new TensorPlot(
									post.calculatePrincipalStressVectors(true, stress),
									post.calculatePrincipalStressVectors(false, stress));
						}
						if (selection.equals(ScalarPlot.types[27])) {
							Matrix[][][] stress = post.calculateStress();
							scalarPlot = new ScalarPlot(post.calculateEquivalentStress(stress, 2),
									ScalarPlot.types[27]);
							tensorPlot = new TensorPlot(
									post.calculatePrincipalStressVectors(true, stress),
									post.calculatePrincipalStressVectors(false, stress));
						}
						if (selection.equals(ScalarPlot.types[28])) {
							scalarPlot = new ScalarPlot(post.calculateThickening(),
									ScalarPlot.types[28]);
						}
						
						SimLive.shell.getDisplay().syncExec(new Runnable() {
							public void run() {
								post.setScalarPlot(scalarPlot);
								post.setTensorPlot(tensorPlot);
								
								btnShowMinmax.setEnabled(post.getScalarPlot() != null);
								post.updateMinMaxLabels();
								SimLive.freezeGUI(false);
								SimLive.view.redraw();
								SimLive.diagramArea.setDisplay(scalarPlot);
								if (dialog != null) {
									dialog.dispose();
									parent.layout();									
								}
								if (post.getScalarPlot() != null) {
									dialog = new PlotSettingsDialog(ResultsDialog.this, SWT.NONE, post);
									parent.layout();
								}
							}
						});
					}
				}).start();
			}
		});
		new Label(this, SWT.NONE);
		
		btnShowMinmax = new Button(this, SWT.CHECK);
		btnShowMinmax.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnShowMinmax.getSelection()) {
					SimLive.post.addMinMaxLabels();
				}
				else {
					SimLive.post.removeMinMaxLabels();
				}
				post.setShowMinMax(btnShowMinmax.getSelection());
				SimLive.view.redraw();
			}
		});
		btnShowMinmax.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		btnShowMinmax.setText("Show Min/Max");
		btnShowMinmax.setSelection(post.isShowMinMax());
		btnShowMinmax.setEnabled(post.getScalarPlot() != null);
		
		if (post.getScalarPlot() != null) {
			dialog = new PlotSettingsDialog(this, SWT.NONE, post);
			parent.layout();
		}
	}
	
	public void setSliderValue(int value) {
		slider.setSelection(value);
		slider.setToolTipText(SimLive.double2String(value)+"/"+
				SimLive.post.getSolution().getNumberOfIncrements());
	}
	
	private double sliderToValue(double value) {
		return Math.pow(2.0, (value-50.0)/10.0);
	}
	
	private double valueToSlider(double value) {
		return Math.log(value)/Math.log(2.0)*10.0+50.0;
	}
	
	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}

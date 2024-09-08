package simlive.dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import simlive.SimLive;
import simlive.postprocessing.Post;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class PlotSettingsDialog extends Composite {
	private Text text_1, text_2;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public PlotSettingsDialog(Composite parent, int style, final Post post) {
		super(parent, style);
		this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		GridLayout gridLayout = new GridLayout(4, true);
		SimLive.formatGridLayoutForComposite(gridLayout);
		setLayout(gridLayout);
		
		{
			Label lblColorPlot = new Label(this, SWT.NONE);
			lblColorPlot.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
			lblColorPlot.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 4, 1));
			lblColorPlot.setFont(SimLive.FONT_BOLD);
			lblColorPlot.setText("Color Plot");
			
			Label lblColors = new Label(this, SWT.NONE);
			lblColors.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblColors.setText("Colors:");
			
			final Slider slider = new Slider(this, SWT.NONE);
			slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int value = slider.getSelection();
					post.setNumberOfColors(value);
					post.getScalarPlot().setPalette(value);
					slider.setToolTipText(SimLive.double2String(value));
					SimLive.view.redraw();
				}
			});
			slider.setThumb(1);
			slider.setPageIncrement(1);
			slider.setMaximum(32+slider.getThumb());
			slider.setMinimum(2);
			slider.setSelection(post.getNumberOfColors());
			slider.setToolTipText(SimLive.double2String(slider.getSelection()));
			slider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			
			Label lblMaximum = new Label(this, SWT.NONE);
			lblMaximum.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblMaximum.setText("Maximum:");
			
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
					double value = SimLive.getInputDouble(text_1);
					post.setMaxValue(value);
					SimLive.view.redraw();
				}
			});
			text_1.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text_1.setEnabled(!post.isAutoMax());
			text_1.setText(SimLive.double2String(post.getMaxValue()));
			
			final Button btnAuto = new Button(this, SWT.CHECK);
			btnAuto.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			btnAuto.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (btnAuto.getSelection()) {
						post.setAutoMax(true);
						text_1.setEnabled(false);
						text_1.setText(SimLive.double2String(post.getMaxValue()));
						SimLive.view.redraw();
					}
					else {
						post.setAutoMax(false);
						text_1.setEnabled(true);
					}
				}
			});
			btnAuto.setSelection(post.isAutoMax());
			btnAuto.setText("Auto");
			
			Label lblMinimum = new Label(this, SWT.NONE);
			lblMinimum.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblMinimum.setText("Minimum:");
			
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
					double value = SimLive.getInputDouble(text_2);
					post.setMinValue(value);
					SimLive.view.redraw();
				}
			});
			text_2.addVerifyListener(new VerifyListener() {
				public void verifyText(VerifyEvent arg0) {
					if (!SimLive.isInputValid(arg0, true)) arg0.doit = false;
				}
			});
			text_2.setEnabled(!post.isAutoMin());
			text_2.setText(SimLive.double2String(post.getMinValue()));
			
			final Button btnAuto_1 = new Button(this, SWT.CHECK);
			btnAuto_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			btnAuto_1.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (btnAuto_1.getSelection()) {
						post.setAutoMin(true);
						text_2.setEnabled(false);
						text_2.setText(SimLive.double2String(post.getMinValue()));
						SimLive.view.redraw();
					}
					else {
						post.setAutoMin(false);
						text_2.setEnabled(true);
					}
				}
			});
			btnAuto_1.setSelection(post.isAutoMin());
			btnAuto_1.setText("Auto");
		}
		
		if (post.getScalarPlot().isCurvePlot()) {
			
			Label lblCurvePlot = new Label(this, SWT.NONE);
			lblCurvePlot.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
			lblCurvePlot.setFont(SimLive.FONT_BOLD);
			lblCurvePlot.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			lblCurvePlot.setText("Curve Plot");
			
			Label lblSize = new Label(this, SWT.NONE);
			lblSize.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblSize.setText("Size:");
			
			final Slider slider_2 = new Slider(this, SWT.NONE);
			slider_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			slider_2.addSelectionListener(new SelectionAdapter() {
			 	@Override
			 	public void widgetSelected(SelectionEvent e) {
			 		int value = slider_2.getSelection()-1000;
					SimLive.post.setCurvePlotScaleFactor(value/100.0);
					slider_2.setToolTipText(SimLive.double2String(value)+"%");
					SimLive.view.redraw();
			 	}
			});
			slider_2.setThumb(100);
			slider_2.setPageIncrement(100);
			slider_2.setMaximum(2000+slider_2.getThumb());
			slider_2.setMinimum(0);
			slider_2.setSelection((int) (SimLive.post.getCurvePlotScaleFactor()*100.0)+1000);
			slider_2.setToolTipText(SimLive.double2String(slider_2.getSelection()-1000)+"%");
			new Label(this, SWT.NONE);
			
			final Button btnSwitchOrientation = new Button(this, SWT.CHECK);
			btnSwitchOrientation.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					SimLive.post.setCurvePlotSwitchOrientation(btnSwitchOrientation.getSelection());
					SimLive.view.redraw();
				}
			});
			btnSwitchOrientation.setSelection(SimLive.post.isCurvePlotSwitchOrientation());
			btnSwitchOrientation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			btnSwitchOrientation.setText("Switch Orientation");
		}
		
		if (post.getTensorPlot() != null) {
			Composite composite = new Composite(this, SWT.NONE);
			GridLayout gl_composite = new GridLayout(4, true);
			SimLive.formatGridLayoutForComposite(gl_composite);
			composite.setLayout(gl_composite);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
			
			Label lblPrincipalVectors = new Label(composite, SWT.NONE);
			lblPrincipalVectors.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
			lblPrincipalVectors.setFont(SimLive.FONT_BOLD);
			lblPrincipalVectors.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
			lblPrincipalVectors.setText("Principal Vectors");
			
			Label lblLayer = new Label(composite, SWT.NONE);
			lblLayer.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblLayer.setText("Layer:");
			
			Combo combo = new Combo(composite, SWT.READ_ONLY);
			combo.setItems(new String[] {"Top", "Bottom"});
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					post.setLayer(Post.Layer.values()[combo.getSelectionIndex()]);
					parent.getChildren()[12].notifyListeners(SWT.Selection, new Event());
				}
			});
			combo.select(post.getLayer().ordinal());
			
			Label lblSize = new Label(composite, SWT.NONE);
			lblSize.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
			lblSize.setText("Size:");
			
			final Slider slider_1 = new Slider(composite, SWT.NONE);
			slider_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			slider_1.addSelectionListener(new SelectionAdapter() {
			 	@Override
			 	public void widgetSelected(SelectionEvent e) {
			 		double value = sliderToValue(slider_1.getSelection());
					post.setPrincipalVectorScaling(value);
					slider_1.setToolTipText(SimLive.double2String(value*100.0)+"%");
					SimLive.view.redraw();
			 	}
			});
			slider_1.setThumb(10);
			slider_1.setPageIncrement(10);
			slider_1.setMaximum(100+slider_1.getThumb());
			slider_1.setMinimum(0);
			slider_1.setSelection((int) valueToSlider(post.getPrincipalVectorScaling()));
			slider_1.setToolTipText(SimLive.double2String(sliderToValue(slider_1.getSelection())*100.0)+"%");
		}

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

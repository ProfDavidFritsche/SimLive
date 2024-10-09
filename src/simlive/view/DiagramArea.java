package simlive.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.SimLive.Select;
import simlive.dialog.ResultsDialog;
import simlive.misc.GeomUtility;
import simlive.model.Step;
import simlive.postprocessing.ScalarPlot;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

public class DiagramArea extends Canvas {

	private int[] mousePos = new int[2];
	private int mouseButton = 0;
	private boolean isMouseDragged = false;
	public static double[] origin = new double[2];
	public static double zoom;
	public static double scaleFactor;
	public enum Display {NONE, HISTORY}
	private Display display;
	
	public DiagramArea(Composite parent, int style) {
		super(parent, style);
		SimLive.addFocusListener(this, (CTabFolder) parent);
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				/* empty keyListener required to get focus on canvas if clicked */
				SimLive.view.timeControlByKeys(e);
				simlive.SimLive.view.redraw();
			}
		});
		reset();
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				/*fix required to open SimLive in WindowBuilder editor*/
				if (SimLive.model == null) return;
				
				fitToView();
			}
		});
		/*addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				setFocus();
			}
			@Override
			public void mouseExit(MouseEvent e) {
				SimLive.shell.setFocus();
			}
		});*/
		addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent arg0) {
				setCursor(null);
				int[] mousePosDelta = new int[2];
				mousePosDelta[0] = arg0.x - mousePos[0];
				mousePosDelta[1] = arg0.y - mousePos[1];
				mousePos[0] = arg0.x;
				mousePos[1] = arg0.y;
				
				if (mouseButton == 3) {
					isMouseDragged = true;
					
					origin[0] += mousePosDelta[0];
					origin[1] += mousePosDelta[1];
					redraw();
				}
				
				if (display == Display.HISTORY) {
					int nIncs = SimLive.post.getSolution().getNumberOfIncrements()-
							SimLive.post.getSolution().getRefModel().getSteps().size()+1;
					double maxValue = SimLive.post.getMaxValue();
					double minValue = SimLive.post.getMinValue();		
					double scaleX = SimLive.FIT_TO_VIEW*getSize().x/(double) nIncs;
					double scaleY = SimLive.FIT_TO_VIEW*getSize().y/(maxValue-minValue);			
					double shiftX = (1.0-SimLive.FIT_TO_VIEW)/2.0*getSize().x;
					double shiftY = (SimLive.FIT_TO_VIEW-1.0)/2.0*getSize().y;
					int currentInc = SimLive.post.getPostIncrementID()-SimLive.post.getPostIncrement().getStepNr();
					double[] coords = new double[2];
					coords[0] = shiftX+currentInc*scaleX;
					coords[1] = shiftY;
					double[] p0 = toScreenCoords(coords);
					coords[1] = shiftY-(maxValue-minValue)*scaleY;
					double[] p1 = toScreenCoords(coords);
					int[] mousePosOld = new int[2];
					mousePosOld[0] = mousePos[0]-mousePosDelta[0];
					mousePosOld[1] = mousePos[1]-mousePosDelta[1];
					if ((Math.abs(p0[0] - mousePos[0]) < SimLive.SNAP_TOL && mousePos[1] > p0[1] && mousePos[1] < p1[1]) ||
						(Math.abs(p0[0] - mousePosOld[0]) < SimLive.SNAP_TOL && mousePosOld[1] > p0[1] && mousePosOld[1] < p1[1]) ||
						(mouseButton == 1 && isMouseDragged)) {
						setCursor(new Cursor(getDisplay(), SWT.CURSOR_SIZEW));
						if (mouseButton == 1) {
							isMouseDragged = true;
							coords[0] = shiftX;
							coords[1] = 0;
							p0 = toScreenCoords(coords);
							coords[0] = shiftX+nIncs*scaleX;
							coords[1] = 0;
							p1 = toScreenCoords(coords);
							double currentIncDouble = (mousePos[0]-p0[0])/(p1[0]-p0[0])*nIncs+SimLive.post.getPostIncrement().getStepNr();
							if (currentIncDouble > SimLive.post.getPostIncrementID()) {
								currentInc = (int) Math.floor(currentIncDouble);
							}
							else {
								currentInc = (int) Math.ceil(currentIncDouble);
							}
							currentInc = Math.min(currentInc, SimLive.post.getSolution().getNumberOfIncrements());
							currentInc = Math.max(currentInc, 0);
							SimLive.post.setPostIncrementID(currentInc);
							((ResultsDialog) SimLive.dialogArea).setSliderValue(currentInc);
							SimLive.post.updateMinMaxLabels();
							SimLive.view.redraw();
							SimLive.updateMatrixView();
						}
					}					
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				setCursor(null);
				mouseButton = 0;
				
				if (isMouseDragged) {
					isMouseDragged = false;
				}
				else {
					if (e.button == 2) /* middle click */ {
						fitToView();
						redraw();
					}
				}
			}
			@Override
			public void mouseDown(MouseEvent e) {
				mouseButton = e.button;
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent arg0) {
				if (arg0.count > 0)  /*zoom in*/  {
					zoomIn(arg0.x, arg0.y);
				}
				else  /*zoom out*/  {
					zoomOut(arg0.x, arg0.y);
				}	
			}
		});
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent arg0) {				
				// Render image
				Image image = renderImage();				
					
				// Draw the offscreen buffer to the screen
				arg0.gc.drawImage(image, 0, 0);
	
			    // Clean up
			    image.dispose();
			}
		});
	}
	
	private Image renderImage() {
		// Create the image to fill the canvas
		Image image = new Image(getParent().getDisplay(), getBounds());
		
		// Set up the offscreen gc
		GC gcImage = new GC(image);
		
		/* gradient background */
		gcImage.setForeground(SimLive.floatToColor(SimLive.COLOR_LIGHT_BLUE));
		gcImage.setBackground(SimLive.floatToColor(SimLive.COLOR_WHITE));
		gcImage.fillGradientRectangle(0, 0, getSize().x, getSize().y, true);
		
		if (SimLive.mode == Mode.RESULTS) {
			if (display != Display.NONE) {
				renderHistoryImage(gcImage);
			}
		}
		
		// Clean up
        gcImage.dispose();
        
		return image;
	}
	
	private void renderHistoryImage(GC gcImage) {
			
		int nIncs = SimLive.post.getSolution().getNumberOfIncrements()-
				SimLive.post.getSolution().getRefModel().getSteps().size()+1;
		double maxValue = SimLive.post.getMaxValue();
		double minValue = SimLive.post.getMinValue();
		double scaleX = SimLive.FIT_TO_VIEW*getSize().x/(double) nIncs;
		double scaleY = SimLive.FIT_TO_VIEW*getSize().y/(maxValue-minValue);			
		double shiftX = (1.0-SimLive.FIT_TO_VIEW)/2.0*getSize().x;
		double shiftY = (SimLive.FIT_TO_VIEW-1.0)/2.0*getSize().y;			
		
		gcImage.setLineDash(new int[] {1, 1});
		gcImage.setForeground(SimLive.floatToColor(SimLive.COLOR_DARK_GRAY));
		double[] maxLine0 = new double[2];
		maxLine0[0] = shiftX;
		maxLine0[1] = shiftY;
		double[] maxLine1 = new double[2];
		maxLine1[0] = shiftX+nIncs*scaleX;
		maxLine1[1] = shiftY;
		double[] minLine0 = new double[2];
		minLine0[0] = shiftX;
		minLine0[1] = shiftY-(maxValue-minValue)*scaleY;
		double[] minLine1 = new double[2];
		minLine1[0] = shiftX+nIncs*scaleX;
		minLine1[1] = shiftY-(maxValue-minValue)*scaleY;
		
		double[] p0 = toScreenCoords(maxLine0);
		double[] p1 = toScreenCoords(minLine1);			
		Path path = new Path(gcImage.getDevice());
		path.addRectangle((float) p0[0], (float) p0[1], (float) (p1[0]-p0[0]), (float) (p1[1]-p0[1]));
		double[] coords = new double[2];
		if (maxValue > 0.0 && minValue < 0.0) {
			/* zero line */
			coords[0] = shiftX;
			coords[1] = shiftY-maxValue*scaleY;
			p0 = toScreenCoords(coords);
			coords[0] = shiftX+nIncs*scaleX;
			p1 = toScreenCoords(coords);
			path.moveTo((float) p0[0], (float) p0[1]);
			path.lineTo((float) p1[0], (float) p1[1]);
		}
		for (int inc = 0, s = 1; s < SimLive.post.getSolution().getRefModel().getSteps().size(); s++) {
			inc += SimLive.post.getSolution().getRefModel().getSteps().get(s-1).nIncrements;
			coords[0] = shiftX+inc*scaleX;
			coords[1] = shiftY;
			p0 = toScreenCoords(coords);
			coords[1] = shiftY-(maxValue-minValue)*scaleY;
			p1 = toScreenCoords(coords);
			path.moveTo((float) p0[0], (float) p0[1]);
			path.lineTo((float) p1[0], (float) p1[1]);
		}
		gcImage.drawPath(path);
		path.dispose();
		gcImage.setLineStyle(SWT.LINE_SOLID);
		
		gcImage.setLineWidth(2);
		gcImage.setForeground(SimLive.floatToColor(SimLive.COLOR_BLUE));
		path = new Path(gcImage.getDevice());
		int currentInc = SimLive.post.getPostIncrementID()-SimLive.post.getPostIncrement().getStepNr();
		coords[0] = shiftX+currentInc*scaleX;
		coords[1] = shiftY;
		p0 = toScreenCoords(coords);
		coords[1] = shiftY-(maxValue-minValue)*scaleY;
		p1 = toScreenCoords(coords);
		path.moveTo((float) p0[0], (float) p0[1]);
		path.lineTo((float) p1[0], (float) p1[1]);
		gcImage.drawPath(path);
		path.dispose();
		gcImage.setLineWidth(1);
		
		if (SimLive.select == Select.LABEL && SimLive.view.labelAtMousePos != null && SimLive.view.labelAtMousePos.getElement() != null) {
			gcImage.setForeground(SimLive.floatToColor(SimLive.COLOR_DARK_GRAY));
			drawHistoryCurve(gcImage, SimLive.view.labelAtMousePos, maxValue, shiftX, shiftY, scaleX, scaleY,
					p0[1], p1[1]);
		}
		
		for (int l = 0; l < SimLive.view.labels.size(); l++) {
			Label label = SimLive.view.labels.get(l);
			if (SimLive.view.selectedLabel == label) {
				gcImage.setForeground(SimLive.floatToColor(SimLive.COLOR_SELECTION));
			}
			else {
				gcImage.setForeground(SimLive.floatToColor(SimLive.COLOR_BLACK));
			}
			
			drawHistoryCurve(gcImage, label, maxValue, shiftX, shiftY, scaleX, scaleY,
					p0[1], p1[1]);
		}
		
		/* set bold font */
		gcImage.setFont(SimLive.FONT_BOLD);
		
		final int gap = (int) (SimLive.fontHeight/2.0);
		gcImage.setForeground(SimLive.floatToColor(SimLive.COLOR_BLACK));
		String string = SimLive.post.getScalarPlot().getType();
		gcImage.drawText(string, gap, gap, true);
		
		string = "Max: " + SimLive.double2String(maxValue)+
				" "+SimLive.post.getScalarPlot().getUnit();
		Point pt = gcImage.stringExtent(string);
		final int textHeight = gcImage.getFontMetrics().getHeight();
		final int descent = gcImage.getFontMetrics().getDescent();
		gcImage.drawText(string, getSize().x-gap-pt.x, gap, true);
		
		string = "Min: " + SimLive.double2String(minValue)+
				" "+SimLive.post.getScalarPlot().getUnit();
		pt = gcImage.stringExtent(string);
		gcImage.drawText(string, getSize().x-gap-pt.x,
				getSize().y-(int) (3*SimLive.fontHeight/2.0)-textHeight+descent, true);
		
		/* reset font */
		gcImage.setFont(getDisplay().getSystemFont());
	}
	
	private void drawHistoryCurve(GC gcImage, Label label, double maxValue,
			double shiftX, double shiftY, double scaleX, double scaleY, double minP, double maxP) {
		Path path = new Path(gcImage.getDevice());
		double[] coords0 = new double[2];
		double[] coords1 = new double[2];
		for (int startInc = 1, s = 0; s < SimLive.post.getSolution().getRefModel().getSteps().size(); s++) {
			Step step = SimLive.post.getSolution().getRefModel().getSteps().get(s);
			for (int i = startInc; i < startInc+step.nIncrements; i++) {
				if (i < SimLive.post.getSolution().getNumberOfIncrements()+1 &&
					SimLive.post.getScalarPlot().hasValue(label.getElement(), SimLive.post.getSolution().getIncrement(i))) {
					double oldValue = label.getValue(i-1);
					double value = label.getValue(i);
					coords0[0] = shiftX+(i-1-s)*scaleX;
					coords0[1] = shiftY+(oldValue-maxValue)*scaleY;
					coords1[0] = shiftX+(i-s)*scaleX;
					coords1[1] = shiftY+(value-maxValue)*scaleY;
					
					double[] p0 = toScreenCoords(coords0);
					double[] p1 = toScreenCoords(coords1);
					
					if (i == startInc && s > 0) {
						float[] pOld = new float[2];
						path.getCurrentPoint(pOld);
						pOld[1] = (float) Math.max(Math.min(pOld[1], maxP), minP);
						path.moveTo(pOld[0], pOld[1]);
						p0[1] = (float) Math.max(Math.min(p0[1], maxP), minP);
						path.lineTo((float) p0[0], (float) p0[1]);
					}
					if (!(p0[1] < minP && p1[1] < minP) && !(p0[1] > maxP && p1[1] > maxP)) {						
						if (p0[1] > maxP) {
							p0 = GeomUtility.intersect(new double[]{0, maxP}, new double[]{1, maxP}, p0, p1);
						}
						if (p0[1] < minP) {
							p0 = GeomUtility.intersect(new double[]{0, minP}, new double[]{1, minP}, p0, p1);
						}
						if (p1[1] > maxP) {
							p1 = GeomUtility.intersect(new double[]{0, maxP}, new double[]{1, maxP}, p0, p1);
						}
						if (p1[1] < minP) {
							p1 = GeomUtility.intersect(new double[]{0, minP}, new double[]{1, minP}, p0, p1);
						}
						
						//fix: some line segments are not drawn when zoomed
						p0[0] = Math.round(p0[0]);
						p0[1] = Math.round(p0[1]);
						p1[0] = Math.round(p1[0]);
						p1[1] = Math.round(p1[1]);
						
						path.moveTo((float) p0[0], (float) p0[1]);
						path.lineTo((float) p1[0], (float) p1[1]);
					}
					else {
						path.moveTo((float) p1[0], (float) p1[1]);
					}
				}
			}
			startInc += step.nIncrements+1;
		}
		gcImage.drawPath(path);
		path.dispose();
	}
	
	private void zoomIn(double x, double y) {
		origin[0] -= (x - origin[0]) * (SimLive.ZOOM_FACTOR - 1.0);
		origin[1] -= (y - origin[1]) * (SimLive.ZOOM_FACTOR - 1.0);
		zoom *= SimLive.ZOOM_FACTOR;
		redraw();
	}
	
	private void zoomOut(double x, double y) {
		origin[0] -= (x - origin[0]) * (1.0/SimLive.ZOOM_FACTOR - 1.0);
		origin[1] -= (y - origin[1]) * (1.0/SimLive.ZOOM_FACTOR - 1.0);
		zoom /= SimLive.ZOOM_FACTOR;
		redraw();
	}
	
	public void setDisplay(ScalarPlot scalarPlot) {
		Display displayOld = display;
		if (scalarPlot == null) display = Display.NONE;
		else display = Display.HISTORY;
		if (displayOld != display) fitToView();
	}
	
	public void reset() {
		display = Display.NONE;
		zoom = 1.0;
		scaleFactor = 1.0;
	}
	
	private void fitToView() {		
		zoom = 1.0;
		origin[0] = 0.0;
		origin[1] = 0.0;
	}
	
	private double[] toScreenCoords(double[] coords) {
		double[] screenCoords = new double[2];
		screenCoords[0] = origin[0]+coords[0]*zoom;
		screenCoords[1] = origin[1]-coords[1]*zoom;
		return screenCoords;
	}

}

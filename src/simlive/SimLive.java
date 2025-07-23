package simlive;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;

import simlive.dialog.Connector3dDialog;
import simlive.dialog.ConnectorDialog;
import simlive.dialog.ContactDialog;
import simlive.dialog.DistributedLoadDialog;
import simlive.dialog.GridDialog;
import simlive.dialog.LoadDialog;
import simlive.dialog.MaterialDialog;
import simlive.dialog.NodeDialog;
import simlive.dialog.Part3dDialog;
import simlive.dialog.PartDialog;
import simlive.dialog.ResultsDialog;
import simlive.dialog.SectionDialog;
import simlive.dialog.SolutionDialog;
import simlive.dialog.StepDialog;
import simlive.dialog.SupportDialog;
import simlive.misc.Settings;
import simlive.misc.Units;
import simlive.misc.XML;
import simlive.model.AbstractConnector;
import simlive.model.AbstractLoad;
import simlive.model.DeepEqualsInterface;
import simlive.model.DistributedLoad;
import simlive.model.Facet3d;
import simlive.model.Material;
import simlive.model.Model;
import simlive.model.Part3d;
import simlive.model.Part3d.Render;
import simlive.model.Part3dColor;
import simlive.model.Section;
import simlive.model.Set;
import simlive.model.Step;
import simlive.model.SubTree;
import simlive.model.Support;
import simlive.model.Vertex3d;
import simlive.model.Connector;
import simlive.model.Connector3d;
import simlive.model.ContactPair;
import simlive.model.Load;
import simlive.postprocessing.Post;
import simlive.solution.Solution;
import simlive.view.DiagramArea;
import simlive.view.View;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Mtl;
import de.javagl.obj.MtlReader;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjGroup;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.RowLayout;

public class SimLive {
	/* TODO: Implementing new model features:
	 * Write/read XML -> simlive.misc.XML
	 * Convert units -> simlive.misc.Units
	 * checkModelChange -> clone(), equals()
	 * updateModel -> simlive.model.Model
	 */
	
	public static Shell shell;
	private static boolean shellClosed = false;
	
	public static Settings settings = null;
	public static Model model = null;
	public static ArrayList<Model> modelHistory;
	public static final int MODEL_HISTORY_MAX = 50;
	public static int modelPos;
	public static Post post = null;
	
	public static final String APPLICATION_NAME = "SimLive";
	public static final String VERSION_NAME = "V1.0";
	public static final int OUTPUT_DIGITS = 8;
	
	public static Font FONT_BOLD;
	
	public static Image OK_ICON;
	public static Image INFO_ICON;
	public static Image WARNING_ICON;
	public static Image ERROR_ICON;
	public static Image START_ICON;
	public static Image PREVIOUS_ICON;
	public static Image PLAY_ICON;
	public static Image PAUSE_ICON;
	public static Image NEXT_ICON;
	public static Image END_ICON;	
	
	public static float fontHeight;
	public static final double[] ICON_HEIGHT_FACTORS = {0.8, 1.1, 2.2};
	public static final float[] COLOR_LIGHT_BLUE = new float[]{0.59f, 0.59f, 1f};
	public static final float[] COLOR_HIGHLIGHT = new float[]{0.94f, 0.94f, 0.94f};
	public static final float[] COLOR_SELECTION = new float[]{1f, 1f, 0f};
	public static final float[] COLOR_DARK_SELECTION = new float[]{0.75f, 0.75f, 0f};
	public static final float[] COLOR_BLACK = new float[]{0f, 0f, 0f};
	public static final float[] COLOR_WHITE = new float[]{1f, 1f, 1f};
	public static final float[] COLOR_RED = new float[]{1f, 0f, 0f};
	public static final float[] COLOR_GREEN = new float[]{0f, 1f, 0f};
	public static final float[] COLOR_BLUE = new float[]{0f, 0f, 1f};
	public static final float[] COLOR_DARK_GRAY = new float[]{0.5f, 0.5f, 0.5f};
	public static final float[] COLOR_DARK_RED = new float[]{0.5f, 0f, 0f};
	public static final float[] COLOR_DARK_GREEN = new float[]{0f, 0.5f, 0f};
	public static final float[] COLOR_DARK_BLUE = new float[]{0f, 0f, 0.5f};
	public static final float[] COLOR_TRANSPARENT = new float[]{1f, 1f, 1f, 0.4f};
	public static final double ZERO_TOL = 1E-8;
	public static final double UNIT_SIZE = 1.0;
	public static final double NODE_RADIUS = 15.0*UNIT_SIZE;
	public static final double SNAP_TOL = 3.0*UNIT_SIZE;
	public static final double LINE_ELEMENT_RADIUS = 8.0*UNIT_SIZE;
	public static final double COORDINATE_SYSTEM_SIZE = 200.0*UNIT_SIZE;
	public static final double COORDINATE_SYSTEM_BOX_FRACTION = 1.0/6.0;
	public static final double ARROW_SIZE = 150.0*UNIT_SIZE;
	public static final double ORIENTATION_SIZE = 0.75*ARROW_SIZE;
	public static final double ARROW_HEAD_FRACTION = 1.0/4.0;
	public static final double ARROW_RADIUS_FRACTION = 1.0/30.0;
	public static final double FIT_TO_VIEW = 0.85;
	public static final double ZOOM_FACTOR = 1.1;
	public static final int ARROW_SLICES = 10;
	public static final int LINE_SLICES = 8;
	public static final int SPHERE_SLICES = 10;
	public static final int SPHERE_STACKS = 10;
	public static final double LABEL_BASE_POINT_SIZE = 3.0*UNIT_SIZE;
	public static final double LABEL_GAP = 3.0*UNIT_SIZE;
	public static final double SPRING_RADIUS = 15.0*UNIT_SIZE;
	public static final double POINT_MASS_RADIUS = 20.0*UNIT_SIZE;
	public static final double LINE_DIVISIONS_SIZE = 20.0*UNIT_SIZE;
	public static final int LINE_DIVISIONS_MAX = 30;
	public static final int CYLINDRIC_SECTION_SLICES_MAX = 32;
	public static final double TEXT_GAP = 10.0*UNIT_SIZE;
	public static final double CURVE_PLOT_SIZE = 200.0*UNIT_SIZE;
	public static final double ANGLE_LEG_SIZE = 50.0*UNIT_SIZE;
	public static final double COS_ANGLE_INNER_EDGE = 0.7;
	
	public static final int UNIT_SIZE1 = 10;
	public static final int GROUP_FRAME_GAP = 2*UNIT_SIZE1;
	public static final int SPRING_WIDTH = 3*UNIT_SIZE1/2;
	public static final int LINE_WIDTH = 2*UNIT_SIZE1/3;
	public static final int ARROWHEAD_SIZE = 3*UNIT_SIZE1/2;
	public static final double ARROWHEAD_ANGLE = 15*Math.PI/180.0;
	public static final int ORIENTATION_ARROW_SIZE = 4*UNIT_SIZE1;
	public static final double ROUND_ARROW_ANGLE = 235*Math.PI/180.0;	
	public static final int ROUND_ARROW_RADIUS = 3*UNIT_SIZE1;	
	public static final int CONTINUOUS_LOAD_REFINE_LEVEL = 4;
	
	public enum Mode {NONE, PARTS, SUPPORTS, LOADS, CONNECTORS, CONTACTS,
						MATERIALS, SECTIONS, STEPS, GRID, SOLUTION, RESULTS}
	public static Mode mode = Mode.NONE;
	
	public enum BoxSelect {NODES, PARTS, PARTS_3D}
	public static BoxSelect boxSelect;
	public enum Select {DEFAULT, DISTANCE, ANGLE, LABEL}
	public static Select select;
	
	public static Label xCoord;
	public static Label yCoord;
	public static Label zCoord;
	public static Label statusBar;
	private static Label displayPartsIcon;
	private static Label displayParts;
	private static Label selection;
	private static Label resultIcon;
	private static Label result;
	public static Composite compositeLeft;
	public static Composite compositeStatusBar;
	public static Composite dialogArea;
	
	public static GLContext glcontext;
	public static View view;
	public static DiagramArea diagramArea;
	
	CTabFolder tabFolderModel;
	public static Tree modelTree;
	private ToolItem tltmUndo;
	private ToolItem tltmRedo;
	private ToolItem tltmMeasureDistance;
	private ToolItem tltmMeasureAngle;
	private ToolItem tltmCreateLabels;
	private ToolItem tltmOrientations;
	private ToolItem tltmNodes;
	private ToolItem tltmEdges;
	private ToolItem tltmSections;
	private ToolItem tltmGrid;
	
	private static Tree tree;
	private static Table table;
	
	private static SashForm sashForm, sashForm_1, sashForm_2, sashFormMatrixView;
	private static int[] sashForm_Weights;
	private static int sashForm_SashWidth;
	private static int[] sashForm_1_Weights;
	private static int sashForm_1_SashWidth;
	private static int sashFormMatrixView_sashWidth;
	private CTabItem tbtmMatrixView;
	private CTabItem tbtmDiagram;
	
	private Thread checkModel;
	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SimLive window = new SimLive();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		FontData fontData = display.getSystemFont().getFontData()[0];
		fontData.setStyle(SWT.BOLD);
		FONT_BOLD = new Font(display, fontData);
		fontHeight = fontData.getHeight()*Toolkit.getDefaultToolkit().getScreenResolution() / 72f;
		OK_ICON = new Image(display, SimLive.class.getResourceAsStream("ok-32.png"));
		INFO_ICON = new Image(display, SimLive.class.getResourceAsStream("info-40.png"));
		WARNING_ICON = new Image(display, SimLive.class.getResourceAsStream("warning-40.png"));
		ERROR_ICON = new Image(display, SimLive.class.getResourceAsStream("error-40.png"));
		START_ICON = new Image(display, SimLive.class.getResourceAsStream("start-36.png"));
		PREVIOUS_ICON = new Image(display, SimLive.class.getResourceAsStream("previous-36.png"));
		PLAY_ICON = new Image(display, SimLive.class.getResourceAsStream("play-36.png"));
		PAUSE_ICON = new Image(display, SimLive.class.getResourceAsStream("pause-36.png"));
		NEXT_ICON = new Image(display, SimLive.class.getResourceAsStream("next-36.png"));
		END_ICON = new Image(display, SimLive.class.getResourceAsStream("end-36.png"));
		START_ICON = resize(START_ICON, ICON_HEIGHT_FACTORS[0]);
		PREVIOUS_ICON = resize(PREVIOUS_ICON, ICON_HEIGHT_FACTORS[0]);
		PLAY_ICON = resize(PLAY_ICON, ICON_HEIGHT_FACTORS[0]);
		PAUSE_ICON = resize(PAUSE_ICON, ICON_HEIGHT_FACTORS[0]);
		NEXT_ICON = resize(NEXT_ICON, ICON_HEIGHT_FACTORS[0]);
		END_ICON = resize(END_ICON, ICON_HEIGHT_FACTORS[0]);
		createContents();
		shell.open();
		shell.layout();
		newFile(null);
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {				
				display.sleep();
			}
			regularChecks();
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				shellClosed = true;
			}
		});
		shell.setMinimumSize(new Point(1024, 768));
		shell.setMaximized(true);
		shell.setText(APPLICATION_NAME+" "+VERSION_NAME);
		shell.setLayout(new GridLayout(2, false));
		
		shell.getDisplay().addFilter(SWT.KeyDown, new Listener() {
            @SuppressWarnings("deprecation")
			public void handleEvent(Event arg0) {
            	if(arg0.keyCode == SWT.DEL && modelTree.isFocusControl()) {
            		ArrayList<Object> objects = getModelTreeSelection();
            		switch (mode) {
            			case PARTS:
            				if (!view.getSelectedSets().isEmpty()) view.deleteSelectedSets();
            				if (!view.getSelectedParts3d().isEmpty()) view.deleteSelectedParts3d();
            				break;
            			case SUPPORTS:
            				model.getSupports().removeAll(objects);
            				break;
            			case LOADS:
            				model.getLoads().removeAll(objects);
            				model.getDistributedLoads().removeAll(objects);
            				break;
            			case CONNECTORS:
            				model.getConnectors().removeAll(objects);
            				model.getConnectors3d().removeAll(objects);
            				break;
            			case CONTACTS:
            				model.getContactPairs().removeAll(objects);
            				break;
            			case MATERIALS:
            				model.getMaterials().removeAll(objects);
            				break;
            			case SECTIONS:
            				model.getSections().removeAll(objects);
            				break;
            			case STEPS:
            				model.getSteps().removeAll(objects);
            				break;
            			default:
            				break;
            		}
            		if (mode != Mode.PARTS && !objects.isEmpty()) {
            			int index = getModelTreeSelectionIndex();
                		doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
            		}
            		model.updateModel();
            		view.redraw();
            	}
            	if(arg0.keyCode == SWT.CONTROL) {
            		view.isControlKeyPressed = true;
					view.measuring(false);
					view.redraw();
            	}
            	if(arg0.keyCode == SWT.ESC && SolutionDialog.thread != null && SolutionDialog.thread.isAlive()) {
            		if (!((SolutionDialog) SimLive.dialogArea).resultsAvailable()) {
            			resetPost();
                		Solution.resetLog();
            		}
            		SolutionDialog.thread.stop();
            		freezeGUI(false);
            		Composite parent = dialogArea.getParent();
            		disposeDialogAreas();
            		dialogArea = new SolutionDialog(parent, SWT.NONE, model, settings);
					parent.layout();
            		
            		try {
						SimLive.messageBox(true, "Solution stopped by user.");
					}
            		catch (Exception e) {}
            	}
			}
        });
		shell.getDisplay().addFilter(SWT.KeyUp, new Listener() {
            public void handleEvent(Event arg0) {
            	if(arg0.keyCode == SWT.CONTROL) {
            		view.isControlKeyPressed = false;
					view.measuring(false);
					view.redraw();
            	}
			}
        });
		
		final Composite compositeToolBar = new Composite(shell, SWT.NONE);
		compositeToolBar.setLayout(new GridLayout(4, false));
		compositeToolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Group grpProject = new Group(compositeToolBar, SWT.NONE);
		grpProject.setText("Project");
		RowLayout rl_grpProject = new RowLayout(SWT.HORIZONTAL);
		grpProject.setLayout(rl_grpProject);
		
		ToolBar toolBar = new ToolBar(grpProject, SWT.FLAT | SWT.RIGHT);
		
		ToolItem tltmFile = new ToolItem(toolBar, SWT.DROP_DOWN);
		tltmFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Menu menu = new Menu(compositeToolBar);
				MenuItem menuItem_new = new MenuItem(menu, SWT.NONE);
				menuItem_new.setText("New");
				menuItem_new.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						newFile(null);
						Model.twoDimensional = false;
					}
				});			
				MenuItem menuItem_new2dmodel = new MenuItem(menu, SWT.NONE);
				menuItem_new2dmodel.setText("New 2D-Model");
				menuItem_new2dmodel.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						newFile(null);
						Model.twoDimensional = true;
					}
				});
				new MenuItem(menu, SWT.SEPARATOR);
				MenuItem menuItem_open = new MenuItem(menu, SWT.NONE);
				menuItem_open.setText("Open...");
				menuItem_open.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
						String[] filter = new String[1];
						filter[0] = "*.sim";
						fileDialog.setFilterExtensions(filter);						
						if (fileDialog.open() != null) {
							Model oldModel = model;
							Settings oldSettings = settings;
							Mode oldMode = mode;
							mode = Mode.NONE;
							freezeGUI(true);
							if (newFile(fileDialog.getFilterPath()+
									System.getProperty("file.separator")+fileDialog.getFileName())) {
								
								view.fitToView();
							}
							else {
								model = oldModel;
						        settings = oldSettings;
						        mode = oldMode;
						        
								MessageBox messageBox = new MessageBox(shell, SWT.ERROR);
								messageBox.setText("Error");
						        messageBox.setMessage("Error reading from file.");
						        messageBox.open();
							}
							freezeGUI(false);
						}
					}
				});
				MenuItem menuItem_save = new MenuItem(menu, SWT.NONE);
				menuItem_save.setText("Save");
				menuItem_save.setEnabled(XML.getFilePath() != null);
				menuItem_save.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						File f = new File(XML.getFilePath());
						boolean overwrite = true;
						if (f.exists() && !f.isDirectory()) {
							 MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION|SWT.OK|SWT.CANCEL);
							 messageBox.setMessage("Overwrite existing file?");
							 overwrite = messageBox.open() == SWT.OK;
						}
						if (overwrite) {
							freezeGUI(true);
							XML.writeFile(XML.getFilePath(), model, settings);
							freezeGUI(false);
						}
					}
				});
				MenuItem menuItem_saveAs = new MenuItem(menu, SWT.NONE);
				menuItem_saveAs.setText("Save As...");
				menuItem_saveAs.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
						String[] filter = new String[1];
						filter[0] = "*.sim";
						fileDialog.setFilterExtensions(filter);
						if (fileDialog.open() != null) {
							freezeGUI(true);
							XML.writeFile(fileDialog.getFilterPath()+
									System.getProperty("file.separator")+
									fileDialog.getFileName(), model, settings);
							XML.setFilePath(fileDialog.getFilterPath()+
									System.getProperty("file.separator")+
									fileDialog.getFileName());
							shell.setText(XML.getFilePath()+" - "+APPLICATION_NAME+" "+VERSION_NAME);
							freezeGUI(false);
						}
					}
				});
				new MenuItem(menu, SWT.SEPARATOR);
				MenuItem menuItem_importControl = new MenuItem(menu, SWT.NONE);
				menuItem_importControl.setText("Import Control...");
				menuItem_importControl.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
						String[] filter = new String[1];
						filter[0] = "*.control";
						fileDialog.setFilterExtensions(filter);
						if (fileDialog.open() != null) {
							Model oldModel = model.clone();
							if (!XML.importControlToModel(fileDialog.getFilterPath(), 
									System.getProperty("file.separator")+fileDialog.getFileName())) {
								model = oldModel;
						        
								MessageBox messageBox = new MessageBox(shell, SWT.ERROR);
								messageBox.setText("Error");
						        messageBox.setMessage("Import failed.");
						        messageBox.open();
							}
							else {
								MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION);
								messageBox.setText("Info");
						        messageBox.setMessage("Import successful. Script file written.");
						        messageBox.open();
						        resetToPartsMode();
							}
						}
					}
				});
				new MenuItem(menu, SWT.SEPARATOR);
				MenuItem menuItem_exportMatrixView = new MenuItem(menu, SWT.NONE);
				menuItem_exportMatrixView.setText("Export Matrix View...");
				menuItem_exportMatrixView.setEnabled(tree.getItemCount() > 0);
				menuItem_exportMatrixView.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						
					}
				});
				MenuItem menuItem_saveScreenshot = new MenuItem(menu, SWT.NONE);
				menuItem_saveScreenshot.setText("Save Screenshot...");
				menuItem_saveScreenshot.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
						String[] filter = new String[1];
						filter[0] = "*.png";
						fileDialog.setFilterExtensions(filter);
						view.redraw();
						if (fileDialog.open() != null) {
							GL2 gl2 = glcontext.getGL().getGL2();
							int[] viewport = View.getViewport();
							FloatBuffer buffer = FloatBuffer.allocate(3*viewport[2]*viewport[3]);
							gl2.glReadPixels(viewport[0], viewport[1], viewport[2], viewport[3], GL2.GL_BGR, GL2.GL_FLOAT, buffer);
							/* flip array */
							byte[] array = new byte[3*viewport[2]*viewport[3]];
							for (int r = 0; r < viewport[3]; r++) {
								/* convert floats to bytes */
								byte[] byteRow = new byte[3*viewport[2]];
								for (int c = 0; c < 3*viewport[2]; c++) {
									float value = buffer.get(r*3*viewport[2]+c);
									byteRow[c] = (byte) Math.floor(value >= 1.0 ? 255 : value * 256.0);
								}
								System.arraycopy(byteRow, 0, array, (viewport[3]-1-r)*3*viewport[2], 3*viewport[2]);
							}
							PaletteData palette = new PaletteData(0xFF , 0xFF00 , 0xFF0000);
							ImageData sourceData = new ImageData(viewport[2], viewport[3], 24, palette, 1, array);
							ImageLoader loader = new ImageLoader();
							loader.data = new ImageData[] {sourceData};
							loader.save(fileDialog.getFilterPath()+
									System.getProperty("file.separator")+
									fileDialog.getFileName(), SWT.IMAGE_PNG);
						}
					}
				});
				new MenuItem(menu, SWT.SEPARATOR);			
				MenuItem menuItem_exit = new MenuItem(menu, SWT.NONE);
				menuItem_exit.setText("Exit");
				menuItem_exit.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						shell.dispose();
					}
				});
				menu.setVisible(true);
			}
		});
		tltmFile.setText("File");
		
		ToolItem tltmView = new ToolItem(toolBar, SWT.DROP_DOWN);
		tltmView.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Menu menu = new Menu(shell);
				MenuItem menuItem_ortho = new MenuItem(menu, SWT.RADIO);
				menuItem_ortho.setText("Orthographic");
				menuItem_ortho.setSelection(!View.perspective);	
				menuItem_ortho.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						View.perspective = false;
						view.redraw();
					}
				});
				MenuItem menuItem_perspective = new MenuItem(menu, SWT.RADIO);
				menuItem_perspective.setText("Perspective");
				menuItem_perspective.setSelection(View.perspective);	
				menuItem_perspective.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!View.perspective) {
							view.initPerspective();
						}
						View.perspective = true;
						view.redraw();
					}
				});
				
				new MenuItem(menu, SWT.SEPARATOR);
				
				MenuItem menuItem_zoomIn = new MenuItem(menu, SWT.NONE);
				menuItem_zoomIn.setText("Zoom In");
				menuItem_zoomIn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						view.zoomIn();
						view.redraw();
					}
				});
				MenuItem menuItem_zoomOut = new MenuItem(menu, SWT.NONE);
				menuItem_zoomOut.setText("Zoom Out");
				menuItem_zoomOut.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						view.zoomOut();
						view.redraw();
					}
				});
				MenuItem menuItem_fitToView = new MenuItem(menu, SWT.NONE);
				menuItem_fitToView.setText("Fit To View");
				menuItem_fitToView.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						view.fitToView();
						view.redraw();
					}
				});
				
				new MenuItem(menu, SWT.SEPARATOR);
				
				final MenuItem menuItem_rotateWithFocus = new MenuItem(menu, SWT.CHECK);
				menuItem_rotateWithFocus.setText("Rotate With Focus");
				menuItem_rotateWithFocus.setEnabled(view.focusPoint != null && view.getFocusPointOrientation() != null);
				menuItem_rotateWithFocus.setSelection(view.focusPointOrientation != null);
				menuItem_rotateWithFocus.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (menuItem_rotateWithFocus.getSelection()) {
							view.focusPointOrientation = view.getFocusPointOrientation();
						}
						else {
							view.focusPointOrientation = null;
						}
					}
				});
				
				final MenuItem menuItem_setFocus = new MenuItem(menu, SWT.CHECK);
				menuItem_setFocus.setText("Set Focus");
				menuItem_setFocus.setSelection(view.focusPoint != null || view.setFocusPoint);
				menuItem_setFocus.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						view.focusPoint = null;
						view.setFocusPoint = menuItem_setFocus.getSelection();
						view.redraw();
					}
				});
				menu.setVisible(true);
			}
		});
		tltmView.setText("View");
		
		ToolItem tltmUnits = new ToolItem(toolBar, SWT.DROP_DOWN);
		tltmUnits.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Menu menu = new Menu(shell);
				final MenuItem menuItem_t_mm_s_N = new MenuItem(menu, SWT.RADIO);
				menuItem_t_mm_s_N.setText("t, mm, s, N");
				menuItem_t_mm_s_N.setSelection(settings.unitSystem == Units.UnitSystem.t_mm_s_N);				
				menuItem_t_mm_s_N.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Units.convertUnitsOfModel(settings.unitSystem, Units.UnitSystem.t_mm_s_N);
						settings.unitSystem = Units.UnitSystem.t_mm_s_N;
						resetState();
						resetToPartsMode();
						view.redraw();
					}
				});
				final MenuItem menuItem_t_m_s_kN = new MenuItem(menu, SWT.RADIO);
				menuItem_t_m_s_kN.setText("t, m, s, kN");
				menuItem_t_m_s_kN.setSelection(settings.unitSystem == Units.UnitSystem.t_m_s_kN);
				menuItem_t_m_s_kN.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Units.convertUnitsOfModel(settings.unitSystem, Units.UnitSystem.t_m_s_kN);
						settings.unitSystem = Units.UnitSystem.t_m_s_kN;
						resetState();
						resetToPartsMode();
						view.redraw();
					}
				});
				final MenuItem menuItem_kg_m_s_N = new MenuItem(menu, SWT.RADIO);
				menuItem_kg_m_s_N.setText("kg, m, s, N");
				menuItem_kg_m_s_N.setSelection(settings.unitSystem == Units.UnitSystem.kg_m_s_N);
				menuItem_kg_m_s_N.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Units.convertUnitsOfModel(settings.unitSystem, Units.UnitSystem.kg_m_s_N);
						settings.unitSystem = Units.UnitSystem.kg_m_s_N;
						resetState();
						resetToPartsMode();
						view.redraw();
					}
				});
				menu.setVisible(true);
			}
		});
		tltmUnits.setText("Units");
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		tltmUndo = new ToolItem(toolBar, SWT.NONE);
		tltmUndo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				undoRedo(true);
			}
		});
		tltmUndo.setText("Undo");
		
		tltmRedo = new ToolItem(toolBar, SWT.NONE);
		tltmRedo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				undoRedo(false);
			}
		});
		tltmRedo.setText("Redo");
		
		new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem tltmInfo = new ToolItem(toolBar, SWT.NONE);
		tltmInfo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SimLive.messageBox(false, APPLICATION_NAME + " " + VERSION_NAME + "\n" +
				                      "Prof. Dr. David Fritsche\n" +
		        		              "Faculty of Mechanical and Systems Engineering\n" +
		        		              "Esslingen University of Applied Sciences\n" +
		        		              "Germany\n");
			}
		});
		tltmInfo.setText("Info");
		
		Group grpAnnotations = new Group(compositeToolBar, SWT.NONE);
		grpAnnotations.setText("Annotations");
		RowLayout rl_grpAnnotations = new RowLayout(SWT.HORIZONTAL);
		grpAnnotations.setLayout(rl_grpAnnotations);
		
		ToolBar toolBar_2 = new ToolBar(grpAnnotations, SWT.FLAT | SWT.RIGHT);
		
		tltmMeasureDistance = new ToolItem(toolBar_2, SWT.CHECK);
		tltmMeasureDistance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				select = select == Select.DISTANCE ? Select.DEFAULT : Select.DISTANCE;
				//SimLive.view.deselectAllAndDisposeDialogs();
				SimLive.view.removeUnfinalizedMeasurement();
				setSelectionLabel();
			}
		});
		tltmMeasureDistance.setText("Measure Distance");
		
		tltmMeasureAngle = new ToolItem(toolBar_2, SWT.CHECK);
		tltmMeasureAngle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				select = select == Select.ANGLE ? Select.DEFAULT : Select.ANGLE;
				//SimLive.view.deselectAllAndDisposeDialogs();
				SimLive.view.removeUnfinalizedMeasurement();
				setSelectionLabel();
			}
		});
		tltmMeasureAngle.setText("Measure Angle");
		
		tltmCreateLabels = new ToolItem(toolBar_2, SWT.CHECK);
		tltmCreateLabels.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				select = select == Select.LABEL ? Select.DEFAULT : Select.LABEL;
				//SimLive.view.deselectAllAndDisposeDialogs();
				SimLive.view.removeUnfinalizedMeasurement();
				setSelectionLabel();
			}
		});
		tltmCreateLabels.setText("Create Labels");
		
		new ToolItem(toolBar_2, SWT.SEPARATOR);
		
		ToolItem tltmAnnotations = new ToolItem(toolBar_2, SWT.DROP_DOWN);
		tltmAnnotations.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Menu menu = new Menu(shell);
				final MenuItem menuItem_deleteAllMeasurements = new MenuItem(menu, SWT.NONE);
				menuItem_deleteAllMeasurements.setText("Delete All Measurements");
				menuItem_deleteAllMeasurements.setEnabled(!view.measurements.isEmpty());
				menuItem_deleteAllMeasurements.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						view.deleteAllMeasurements();
					}
				});
				final MenuItem menuItem_deleteAllLabels = new MenuItem(menu, SWT.NONE);
				menuItem_deleteAllLabels.setText("Delete All Labels");
				menuItem_deleteAllLabels.setEnabled(!(view.labels.isEmpty() ||
						(SimLive.post != null && view.labels.size() == 2 &&
						 (view.labels.get(1) == SimLive.post.getMinLabel() ||
						  view.labels.get(1) == SimLive.post.getMaxLabel()))));
				menuItem_deleteAllLabels.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						view.deleteAllLabels();
					}
				});
				menu.setVisible(true);
			}
		});
		tltmAnnotations.setText("Annotations");
		
		Group grpDisplayOptions = new Group(compositeToolBar, SWT.NONE);
		grpDisplayOptions.setText("Display Options");
		grpDisplayOptions.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		ToolBar toolBar_3 = new ToolBar(grpDisplayOptions, SWT.FLAT | SWT.RIGHT);
		
		tltmOrientations = new ToolItem(toolBar_3, SWT.CHECK);
		tltmOrientations.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.isShowOrientations = tltmOrientations.getSelection();
				view.redraw();
			}
		});
		tltmOrientations.setText("Orientations");
		
		tltmNodes = new ToolItem(toolBar_3, SWT.CHECK);
		tltmNodes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.isShowNodes = tltmNodes.getSelection();
				view.redraw();
			}
		});
		tltmNodes.setText("Nodes");
		
		tltmEdges = new ToolItem(toolBar_3, SWT.CHECK);
		tltmEdges.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.isShowEdges = tltmEdges.getSelection();
				view.redraw();
			}
		});
		tltmEdges.setText("Edges");
		
		tltmSections = new ToolItem(toolBar_3, SWT.CHECK);
		tltmSections.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.isShowSections = tltmSections.getSelection();
				view.redraw();
			}
		});
		tltmSections.setText("Sections");
		
		tltmGrid = new ToolItem(toolBar_3, SWT.CHECK);
		tltmGrid.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				settings.isShowGrid = tltmGrid.getSelection();
				view.redraw();
			}
		});
		tltmGrid.setText("Grid");
		
		new ToolItem(toolBar_3, SWT.SEPARATOR);
		
		ToolItem tltmDisplayOptions = new ToolItem(toolBar_3, SWT.DROP_DOWN);
		tltmDisplayOptions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Menu menu = new Menu(shell);
				
				MenuItem menuItem_objects = new MenuItem(menu, SWT.CASCADE);
				menuItem_objects.setText("Objects");
				Menu menu_objects = new Menu(menuItem_objects);
				final MenuItem menuItem_orientations = new MenuItem(menu_objects, SWT.CHECK);
				menuItem_orientations.setText("Orientations");
				menuItem_orientations.setSelection(settings.isShowOrientations);
				menuItem_orientations.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowOrientations = menuItem_orientations.getSelection();
						view.redraw();
					}
				});
				final MenuItem menuItem_nodes = new MenuItem(menu_objects, SWT.CHECK);
				menuItem_nodes.setText("Nodes");
				menuItem_nodes.setSelection(settings.isShowNodes);				
				menuItem_nodes.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowNodes = ((MenuItem) e.getSource()).getSelection();
						view.redraw();
					}
				});
				final MenuItem menuItem_edges = new MenuItem(menu_objects, SWT.CHECK);
				menuItem_edges.setText("Edges");
				menuItem_edges.setSelection(settings.isShowEdges);
				menuItem_edges.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowEdges = ((MenuItem) e.getSource()).getSelection();
						view.redraw();
					}
				});
				final MenuItem menuItem_sections = new MenuItem(menu_objects, SWT.CHECK);
				menuItem_sections.setText("Sections");
				menuItem_sections.setSelection(settings.isShowSections);
				menuItem_sections.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowSections = menuItem_sections.getSelection();
						view.redraw();
					}
				});
				final MenuItem menuItem_supports = new MenuItem(menu_objects, SWT.CHECK);
				menuItem_supports.setText("Supports");
				menuItem_supports.setSelection(settings.isShowSupports);
				menuItem_supports.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowSupports = ((MenuItem) e.getSource()).getSelection();
						view.redraw();
					}
				});
				final MenuItem menuItem_loads = new MenuItem(menu_objects, SWT.CHECK);
				menuItem_loads.setText("Loads");
				menuItem_loads.setSelection(settings.isShowLoads);
				menuItem_loads.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowLoads = ((MenuItem) e.getSource()).getSelection();
						view.redraw();
					}
				});
				/*final MenuItem menuItem_reactions = new MenuItem(menu_objects, SWT.CHECK);
				menuItem_reactions.setText("Reactions");
				menuItem_reactions.setEnabled(mode == Mode.RESULTS);
				menuItem_reactions.setSelection(settings.isShowReactions);
				menuItem_reactions.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowReactions = ((MenuItem) e.getSource()).getSelection();
						view.redraw();
					}
				});*/
				menuItem_objects.setMenu(menu_objects);
				
				MenuItem menuItem_coordSys = new MenuItem(menu, SWT.CASCADE);
				menuItem_coordSys.setText("Coordinate System");				
				Menu menu_coordSys = new Menu(menu);
				final MenuItem menuItem_grid = new MenuItem(menu_coordSys, SWT.CHECK);
				menuItem_grid.setText("Grid");
				menuItem_grid.setSelection(settings.isShowGrid);
				menuItem_grid.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowGrid = menuItem_grid.getSelection();
						view.redraw();
					}
				});
				final MenuItem menuItem_axes = new MenuItem(menu_coordSys, SWT.CHECK);
				menuItem_axes.setText("Axes");
				menuItem_axes.setSelection(settings.isShowAxes);
				menuItem_axes.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowAxes = menuItem_axes.getSelection();
						view.redraw();
					}
				});
				final MenuItem menuItem_scale = new MenuItem(menu_coordSys, SWT.CHECK);
				menuItem_scale.setText("Scale");
				menuItem_scale.setSelection(settings.isShowScale);
				menuItem_scale.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						settings.isShowScale = menuItem_scale.getSelection();
						view.redraw();
					}
				});
				menuItem_coordSys.setMenu(menu_coordSys);
				
				new MenuItem(menu, SWT.SEPARATOR);
				
				final MenuItem menuItem_reset = new MenuItem(menu, SWT.NONE);
				menuItem_reset.setText("Reset To Defaults");
				menuItem_reset.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						for (int s = 0; s < model.getSets().size(); s++) {
							model.getSets().get(s).view = Set.View.DEFAULT;
						}
						for (int p = 0; p < model.getParts3d().size(); p++) {
							model.getParts3d().get(p).render = Render.FILL;
							model.getParts3d().get(p).doubleSided = false;
						}
						for (int s = 0; s < model.getSupports().size(); s++) {
							model.getSupports().get(s).isShifted = false;
						}
						for (int l = 0; l < model.getLoads().size(); l++) {
							model.getLoads().get(l).isShifted = false;
						}
						for (int l = 0; l < model.getDistributedLoads().size(); l++) {
							model.getDistributedLoads().get(l).isShifted = false;
						}
						settings.resetDisplayOptions();
						view.redraw();
					}
				});
				
				/*new MenuItem(menu, SWT.SEPARATOR);
				
				final MenuItem menuItem_shiftVectors = new MenuItem(menu, SWT.NONE);
				menuItem_shiftVectors.setText("Shift Selected Support/Load");
				menuItem_shiftVectors.setEnabled(
						(SimLive.mode == SimLive.Mode.LOADS && ((LoadsDialog) SimLive.dialogArea).getSelection() != null) ||
						(SimLive.mode == SimLive.Mode.SUPPORTS && ((SupportsDialog) SimLive.dialogArea).getSelection() != null));
				menuItem_shiftVectors.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (SimLive.mode == SimLive.Mode.LOADS) {
							AbstractLoad abstractLoad = ((LoadsDialog) SimLive.dialogArea).getSelection();
							abstractLoad.isShifted = !abstractLoad.isShifted;
						}
						if (SimLive.mode == SimLive.Mode.SUPPORTS) {
							Support support = ((SupportsDialog) SimLive.dialogArea).getSelection();
							support.isShifted = !support.isShifted;
						}
						view.redraw();
					}
				});
				
				new MenuItem(menu, SWT.SEPARATOR);
				
				final MenuItem menuItem_hideSelectedParts = new MenuItem(menu, SWT.NONE);
				menuItem_hideSelectedParts.setText("Hide Selected Parts");
				menuItem_hideSelectedParts.setEnabled(!view.doSetsOnlyContain(view.getSelectedSets(), Set.View.HIDDEN));
				menuItem_hideSelectedParts.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						for (int s = 0; s < SimLive.view.getSelectedSets().size(); s++) {
							Set set = SimLive.view.getSelectedSets().get(s);
							set.view = Set.View.HIDDEN;
						}
						view.deselectAllAndDisposeDialogs();
						setDisplayPartsLabel();
						view.redraw();
					}
				});
				
				final MenuItem menuItem_pinSelectedParts = new MenuItem(menu, SWT.NONE);
				menuItem_pinSelectedParts.setText("Pin Selected Parts");
				menuItem_pinSelectedParts.setEnabled(!view.doSetsOnlyContain(view.getSelectedSets(), Set.View.PINNED));
				menuItem_pinSelectedParts.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						for (int s = 0; s < SimLive.view.getSelectedSets().size(); s++) {
							Set set = SimLive.view.getSelectedSets().get(s);
							set.view = Set.View.PINNED;
						}
						view.deselectAllAndDisposeDialogs();
						setDisplayPartsLabel();
						view.redraw();
					}
				});
				
				final MenuItem menuItem_restoreDefault = new MenuItem(menu, SWT.NONE);
				menuItem_restoreDefault.setText("Restore Default");
				menuItem_restoreDefault.setEnabled(!view.doSetsOnlyContain(model.getSets(), Set.View.DEFAULT));
				menuItem_restoreDefault.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						for (int s = 0; s < model.getSets().size(); s++) {
							Set set = model.getSets().get(s);
							set.view = Set.View.DEFAULT;
						}
						for (int c = 0; c < model.getContactPairs().size(); c++) {
							if (model.getContactPairs().get(c).getType() == ContactPair.Type.RIGID_DEFORMABLE) {
								for (int s = 0; s < model.getContactPairs().get(c).getMasterSets().size(); s++) {
									Set set = model.getContactPairs().get(c).getMasterSets().get(s);
									set.view = Set.View.DEFAULT;
								}
							}
						}
						view.deselectAllAndDisposeDialogs();
						setDisplayPartsLabel();
						view.redraw();
					}
				});*/
				
				menu.setVisible(true);
			}
		});
		tltmDisplayOptions.setText("Display Options");
		
		ToolItem tltmLayout = new ToolItem(toolBar_3, SWT.DROP_DOWN);
		tltmLayout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Menu menu = new Menu(shell);
				final MenuItem menuItem_showDiagramView = new MenuItem(menu, SWT.CHECK);
				menuItem_showDiagramView.setText("Show Diagram View");
				menuItem_showDiagramView.setSelection(!tbtmDiagram.isDisposed());
				menuItem_showDiagramView.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (menuItem_showDiagramView.getSelection()) {
							initArea(false);
						}
						else {
							tbtmDiagram.dispose();
						}
					}
				});
				final MenuItem menuItem_showMatrixView = new MenuItem(menu, SWT.CHECK);
				menuItem_showMatrixView.setText("Show Matrix View");
				menuItem_showMatrixView.setSelection(!tbtmMatrixView.isDisposed());
				menuItem_showMatrixView.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (menuItem_showMatrixView.getSelection()) {
							initArea(true);
						}
						else {
							tbtmMatrixView.dispose();
						}
					}
				});
				
				new MenuItem(menu, SWT.SEPARATOR);
				
				final MenuItem menuItem_horizontal = new MenuItem(menu, SWT.CHECK);
				menuItem_horizontal.setText("Switch Matrix View Horizontal/Vertical");
				menuItem_horizontal.setSelection(sashForm.getOrientation() == SWT.HORIZONTAL);
				menuItem_horizontal.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (sashForm.getOrientation() == SWT.HORIZONTAL) {
							sashForm.setOrientation(SWT.VERTICAL);
						}
						else {
							sashForm.setOrientation(SWT.HORIZONTAL);
						}
					}
				});
				final MenuItem menuItem_vertical = new MenuItem(menu, SWT.CHECK);
				menuItem_vertical.setText("Switch Diagram View Horizontal/Vertical");
				menuItem_vertical.setSelection(sashForm_1.getOrientation() == SWT.VERTICAL);
				menuItem_vertical.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (sashForm_1.getOrientation() == SWT.VERTICAL) {
							sashForm_1.setOrientation(SWT.HORIZONTAL);
						}
						else {
							sashForm_1.setOrientation(SWT.VERTICAL);
						}
					}
				});
				
				new MenuItem(menu, SWT.SEPARATOR);
				
				final MenuItem menuItem_reset = new MenuItem(menu, SWT.NONE);
				menuItem_reset.setText("Reset To Defaults");
				menuItem_reset.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						sashForm.setOrientation(SWT.VERTICAL);
						sashForm_Weights = new int[]{4, 1};
						sashForm.setWeights(sashForm_Weights);
						sashForm_1.setOrientation(SWT.HORIZONTAL);
						sashForm_1_Weights = new int[]{3, 1};
						sashForm_1.setWeights(sashForm_1_Weights);
						if (!menuItem_showDiagramView.getSelection()) {
							initArea(false);
						}
						if (!menuItem_showMatrixView.getSelection()) {
							initArea(true);
						}
						if (tree.getItemCount() > 0) {
							setSashFormMatrixView(tree.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
						}
						sashForm_2.setWeights(new int[] {1, 3});
					}
				});
				menu.setVisible(true);
			}
		});
		tltmLayout.setText("Layout");
		
		tabFolderModel = new CTabFolder(shell, SWT.BORDER | SWT.SINGLE);
		CTabItem tabItem = new CTabItem(tabFolderModel, SWT.NONE);
		tabItem.setText("Model");
		
		tabFolderModel.setSelection(tabItem);
		
		CTabItem tbtmNewItem = new CTabItem(tabFolderModel, SWT.NONE);
		tbtmNewItem.setText("Solution");
		
		Composite composite_1 = new Composite(tabFolderModel, SWT.NONE);
		tbtmNewItem.setControl(composite_1);
		composite_1.setLayout(new GridLayout(1, false));
		
		CTabItem tbtmNewItem_1 = new CTabItem(tabFolderModel, SWT.NONE);
		tbtmNewItem_1.setText("Results");
		
		Composite composite_2 = new Composite(tabFolderModel, SWT.NONE);
		tbtmNewItem_1.setControl(composite_2);
		composite_2.setLayout(new GridLayout(1, false));
		
		tabFolderModel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				for (int i = 0; i < tabFolderModel.getItemCount(); i++) {
					tabFolderModel.getItem(i).setFont(shell.getDisplay().getSystemFont());
				}
				int index = tabFolderModel.getSelectionIndex();
				tabFolderModel.getItem(index).setFont(FONT_BOLD);
				switch(index) {
					case 0:
						modelTree.notifyListeners(SWT.Selection, new Event());
						break;
						
					case 1:
						mode = Mode.SOLUTION;
						resetState();
						dialogArea = new SolutionDialog(composite_1, SWT.NONE, model, settings);
						composite_1.layout();
						break;
					
					case 2:
						checkModelChange();
						if (SimLive.post != null) {
							mode = Mode.RESULTS;
							resetState();
							dialogArea = new ResultsDialog(composite_2, SWT.NONE, post, settings);
							composite_2.layout();					
							SimLive.post.updateMinMaxLabels();
							updateMatrixView();
							((CTabFolder) view.getParent()).getItem(0).setText("Results View");
						}
						else {
							SimLive.messageBox(true, "No results available.\n"
					        		+ "Solve model first.");
					        resetToPartsMode();
						}
						break;
				}
			}
		});
		tabFolderModel.setSingle(false);
		GridData gd_tabFolderStructure = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
		gd_tabFolderStructure.widthHint = 400;
		tabFolderModel.setLayoutData(gd_tabFolderStructure);
		tabFolderModel.setSimple(false);
		tabFolderModel.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		sashForm_2 = new SashForm(tabFolderModel, SWT.VERTICAL);
		tabItem.setControl(sashForm_2);
		
		modelTree = new Tree(sashForm_2, SWT.MULTI | SWT.BORDER);
		final TreeEditor editor = new TreeEditor(modelTree);
		modelTree.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				final TreeItem item = modelTree.getItem(new Point(event.x, event.y));
				if (item != null && item.getParentItem() != null && item.getParentItem() != modelTree.getItem(0) &&
						item.getParentItem() != modelTree.getItem(6)) {
					final Composite composite = new Composite(modelTree, SWT.NONE);
					final Text text = new Text(composite, SWT.NONE);
					composite.addListener(SWT.Resize, new Listener() {
						public void handleEvent(Event e) {
							Rectangle rect = composite.getClientArea();
							text.setBounds(rect.x, rect.y, rect.width, rect.height);
						}
					});
					text.addDisposeListener(new DisposeListener() {
						@Override
						public void widgetDisposed(DisposeEvent arg0) {
							switch (mode) {
					        	case SUPPORTS:
					        		((Support) item.getData()).name = item.getText();
					        		break;
					        	case LOADS:
					        		((AbstractLoad) item.getData()).name = item.getText();
					        		break;
					        	case CONNECTORS:
					        		((AbstractConnector) item.getData()).name = item.getText();
					        		break;
					        	case CONTACTS:
					        		((ContactPair) item.getData()).name = item.getText();
					        		break;
					        	case MATERIALS:
					        		((Material) item.getData()).name = item.getText();
					        		break;
					        	case STEPS:
					        		((Step) item.getData()).name = item.getText();
					        		break;
					        	default:
					        		break;
					        }
							modelTree.notifyListeners(SWT.Selection, new Event());
					        view.redraw();
						}						
					});
					Listener textListener = new Listener() {
						public void handleEvent(final Event e) {
							switch (e.type) {
								case SWT.FocusOut:
									item.setText(text.getText());
									composite.dispose();
									break;
								case SWT.Verify:
					                String newText = text.getText();
					                String leftText = newText.substring(0, e.start);
					                String rightText = newText.substring(e.end, newText.length());
					                GC gc = new GC(text);
					                Point size = gc.textExtent(leftText + e.text + rightText);
					                gc.dispose();
					                size = text.computeSize(size.x, SWT.DEFAULT);
					                editor.horizontalAlignment = SWT.LEFT;
					                Rectangle itemRect = item.getBounds(),
					                rect = modelTree.getClientArea();
					                editor.minimumWidth = Math.max(size.x, itemRect.width);
					                int left = itemRect.x,
					                right = rect.x + rect.width;
					                editor.minimumWidth = Math.min(editor.minimumWidth, right - left);
					                editor.minimumHeight = size.y;
					                editor.layout();
					                break;
								case SWT.Traverse:
									switch (e.detail) {
										case SWT.TRAVERSE_RETURN:
											item.setText(text.getText());
										// FALL THROUGH
										case SWT.TRAVERSE_ESCAPE:
											composite.dispose();
											e.doit = false;
									}
									break;
							}
						}
					};
					text.addListener(SWT.FocusOut, textListener);
			        text.addListener(SWT.Traverse, textListener);
			        text.addListener(SWT.Verify, textListener);
			        editor.setEditor(composite, item);
			        text.setText(item.getText());
			        text.selectAll();
			        text.setFocus();
				}
			}
	    });
		modelTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selection = modelTree.getSelection();
				boolean doIt = selection.length > 0;
				for (int i = 1; i < selection.length; i++) {
					if (selection[i].getParentItem() == null ||
						selection[i].getParentItem() !=
						selection[0].getParentItem()) doIt = false;
				}
				if (doIt && selection[0].getParentItem() == modelTree.getItem(0)) {
					if (modelTree.getItem(0).indexOf(selection[0]) < model.getSets().size() &&
						modelTree.getItem(0).indexOf(selection[selection.length-1]) > model.getSets().size()-1) doIt = false;
				}
				if (doIt) {					
					int index = modelTree.indexOf(selection[0]);
					if (selection[0].getParentItem() != null) {
						index = modelTree.indexOf(selection[0].getParentItem());
					}
					ArrayList<Object> objects = getModelTreeSelection();
					switch (index) {
						case 0:
							boolean wasAnotherTab = mode == Mode.SOLUTION || mode == Mode.RESULTS;
							mode = Mode.PARTS;
							resetState();
							if (wasAnotherTab) {
								synchronizeModelTreeWithViewSelection();
								objects = getModelTreeSelection();
							}
							else if (objects.isEmpty()) {
								synchronizeModelTreeWithViewSelection();
							}
							if (!objects.isEmpty() && objects.get(0) instanceof Set) {
								ArrayList<Set> selectedSets = new ArrayList<Set>();
								for (int i = 0; i < objects.size(); i++) {
									selectedSets.add((Set) objects.get(i));
								}
								view.setSelectedSets(selectedSets);
								view.setSelectedParts3d(new ArrayList<Part3d>());
							}
							if (!objects.isEmpty() && objects.get(0) instanceof Part3d) {
								ArrayList<Part3d> selectedParts3d = new ArrayList<Part3d>();
								for (int i = 0; i < objects.size(); i++) {
									selectedParts3d.add((Part3d) objects.get(i));
								}
								view.setSelectedSets(new ArrayList<Set>());
								view.setSelectedParts3d(selectedParts3d);
							}
							if (view.getSelectedNodes().size() == 1) {
								SimLive.dialogArea = new NodeDialog(SimLive.compositeLeft, SWT.NONE, view.getSelectedNodes().get(0));
							}
							if (!view.getSelectedSets().isEmpty()) {
								SimLive.dialogArea = new PartDialog(SimLive.compositeLeft, SWT.NONE, view.getSelectedSets(), SimLive.settings);
							}
							if (!view.getSelectedParts3d().isEmpty()) {
								SimLive.dialogArea = new Part3dDialog(SimLive.compositeLeft, SWT.NONE, view.getSelectedParts3d(), SimLive.settings);
							}
							break;
						
						case 1:	
							mode = Mode.SUPPORTS;
							resetState();
							if (objects.size() == 1) {
								dialogArea = new SupportDialog(compositeLeft, SWT.NONE,
										(Support) objects.get(0));
								compositeLeft.layout();
							}
							break;
						
						case 2:	
							mode = Mode.LOADS;
							resetState();
							if (objects.size() == 1 && objects.get(0) instanceof Load) {
								dialogArea = new LoadDialog(compositeLeft, SWT.NONE,
										(Load) objects.get(0));
								compositeLeft.layout();
							}
							if (objects.size() == 1 && objects.get(0) instanceof DistributedLoad) {
								dialogArea = new DistributedLoadDialog(compositeLeft, SWT.NONE,
										(DistributedLoad) objects.get(0));
								compositeLeft.layout();
							}
							break;
						
						case 3:	
							mode = Mode.CONNECTORS;
							resetState();
							if (objects.size() == 1 && objects.get(0) instanceof Connector) {
								dialogArea = new ConnectorDialog(compositeLeft, SWT.NONE,
										(Connector) objects.get(0));
								compositeLeft.layout();
							}
							if (objects.size() == 1 && objects.get(0) instanceof Connector3d) {
								dialogArea = new Connector3dDialog(compositeLeft, SWT.NONE,
										(Connector3d) objects.get(0));
								compositeLeft.layout();
							}
							break;
						
						case 4:	
							mode = Mode.CONTACTS;
							resetState();
							if (objects.size() == 1) {
								dialogArea = new ContactDialog(compositeLeft, SWT.NONE,
										(ContactPair) objects.get(0));
								compositeLeft.layout();
							}
							break;
						
						case 5:	
							mode = Mode.MATERIALS;
							resetState();
							if (objects.size() == 1) {
								dialogArea = new MaterialDialog(compositeLeft, SWT.NONE,
										(Material) objects.get(0));
								compositeLeft.layout();
							}
							break;
						
						case 6:	
							mode = Mode.SECTIONS;
							resetState();
							if (objects.size() == 1) {
								dialogArea = new SectionDialog(compositeLeft, SWT.NONE,
										(Section) objects.get(0), selection[0]);
								compositeLeft.layout();
							}
							break;
						
						case 7:
							mode = Mode.STEPS;
							resetState();
							if (objects.size() == 1) {
								dialogArea = new StepDialog(compositeLeft, SWT.NONE,
										(Step) objects.get(0));
								compositeLeft.layout();
							}
							break;
						
						case 8:
							mode = Mode.GRID;
							resetState();
							dialogArea = new GridDialog(compositeLeft, SWT.NONE, settings);
							compositeLeft.layout();
							break;
					}
				}
				else {
					modelTree.deselectAll();
					Point point = Display.getCurrent().getFocusControl().toControl(Display.getCurrent().getCursorLocation());
					TreeItem item = modelTree.getItem(point);
					if (item != null) {
						modelTree.setSelection(item);
						modelTree.notifyListeners(SWT.Selection, new Event());
					}
				}
			}
		});
		
		modelTree.addListener(SWT.Expand, new Listener() {
	        public void handleEvent(Event event) {
	        	if (event.item == modelTree.getItem(0)) {
	        		modelTree.getItem(0).setExpanded(true);
		        	synchronizeModelTreeWithViewSelection();
	        	}
	        }
		});
		
		String[] str = {"Parts", "Supports", "Loads", "Connectors", "Contacts", "Materials", "Sections",
				"Steps", "Grid"};
		for (int i = 0; i < str.length; i++) {
			TreeItem item = new TreeItem(modelTree, SWT.NONE);
			item.setText(str[i]);
		}
		
		final Menu rightClickMenu = new Menu(modelTree);
	    modelTree.setMenu(rightClickMenu);	   
	    rightClickMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuHidden(MenuEvent arg0) {
				view.redraw();
			}
			@Override
			public void menuShown(MenuEvent arg0) {
			}
		});
		modelTree.addListener(SWT.MouseDown, new Listener() {
	        public void handleEvent(Event event) {
	        	if (event.button != 3) return;
	        	modelTree.notifyListeners(SWT.Selection, new Event());
	        	MenuItem[] items = rightClickMenu.getItems();
	            for (int i = 0; i < items.length; i++) {
	                items[i].dispose();
	            }
	            ArrayList<Object> objects = getModelTreeSelection();
	            switch (mode) {
		        	case PARTS:
		        		view.popupMenu(rightClickMenu);
		        		break;
			        
		        	case SUPPORTS:
		        		MenuItem newSupport = new MenuItem(rightClickMenu, SWT.NONE);
		        		newSupport.setText("New Support");
		        		newSupport.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getSupports().add(new Support());
								doModelTreeNewSelection(model.getSupports().size()-1);
							}
						});
		        		if (!objects.isEmpty()) {
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem shift = new MenuItem(rightClickMenu, SWT.NONE);
		        			shift.setText("Shift Vectors");
		        			shift.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									for (int i = 0; i < objects.size(); i++) {
										((Support) objects.get(i)).isShifted = !((Support) objects.get(i)).isShifted;
									}
								}
							});
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem delete = new MenuItem(rightClickMenu, SWT.NONE);
			        		delete.setText("Delete");
			        		delete.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									int index = getModelTreeSelectionIndex();
									model.getSupports().removeAll(objects);
									doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
								}
							});
		        		}
		        		break;
		        	
		        	case LOADS:
		        		MenuItem newLoad = new MenuItem(rightClickMenu, SWT.NONE);
		        		newLoad.setText("New Load");
		        		newLoad.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getLoads().add(new Load());
								doModelTreeNewSelection(model.getLoads().size()-1);
							}
						});
		        		MenuItem newDistributedLoad = new MenuItem(rightClickMenu, SWT.NONE);
		        		newDistributedLoad.setText("New Distributed Load");
		        		newDistributedLoad.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getDistributedLoads().add(new DistributedLoad());
								doModelTreeNewSelection(model.getLoads().size()+model.getDistributedLoads().size()-1);
							}
						});
		        		if (!objects.isEmpty()) {
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem shift = new MenuItem(rightClickMenu, SWT.NONE);
		        			shift.setText("Shift Vectors");
		        			shift.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									for (int i = 0; i < objects.size(); i++) {
										((AbstractLoad) objects.get(i)).isShifted = !((AbstractLoad) objects.get(i)).isShifted;
									}
								}
							});
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem delete = new MenuItem(rightClickMenu, SWT.NONE);
			        		delete.setText("Delete");
			        		delete.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									int index = getModelTreeSelectionIndex();
									model.getLoads().removeAll(objects);
									for (int i = 0; i < objects.size(); i++) {
										if (((AbstractLoad) objects.get(i)).getLoadType() == AbstractLoad.LoadType.DISTRIBUTED_LOAD) {
											((DistributedLoad) objects.get(i)).unrefine();
										}
									}
									model.getDistributedLoads().removeAll(objects);
									doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
								}
							});
		        		}
		        		break;
		        	
		        	case CONNECTORS:
		        		MenuItem newConnector = new MenuItem(rightClickMenu, SWT.NONE);
		        		newConnector.setText("New Connector");
		        		newConnector.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getConnectors().add(new Connector());
								doModelTreeNewSelection(model.getConnectors().size()-1);
							}
						});
		        		MenuItem newConnector3d = new MenuItem(rightClickMenu, SWT.NONE);
		        		newConnector3d.setText("New 3D-Connector");
		        		newConnector3d.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getConnectors3d().add(new Connector3d());
								doModelTreeNewSelection(model.getConnectors().size()+model.getConnectors3d().size()-1);
							}
						});
		        		if (!objects.isEmpty()) {
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem delete = new MenuItem(rightClickMenu, SWT.NONE);
			        		delete.setText("Delete");
			        		delete.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									int index = getModelTreeSelectionIndex();
									model.getConnectors().removeAll(objects);
									model.getConnectors3d().removeAll(objects);
									doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
								}
							});
		        		}
		        		break;
		        	
		        	case CONTACTS:
		        		MenuItem newContact = new MenuItem(rightClickMenu, SWT.NONE);
		        		newContact.setText("New Contact");
		        		newContact.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getContactPairs().add(new ContactPair());
								doModelTreeNewSelection(model.getContactPairs().size()-1);
							}
						});
		        		if (!objects.isEmpty()) {
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem delete = new MenuItem(rightClickMenu, SWT.NONE);
			        		delete.setText("Delete");
			        		delete.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									int index = getModelTreeSelectionIndex();
									model.getContactPairs().removeAll(objects);
									doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
								}
							});
		        		}
		        		break;
		        	
		        	case MATERIALS:
		        		MenuItem newMaterial = new MenuItem(rightClickMenu, SWT.NONE);
		        		newMaterial.setText("New Material");
		        		newMaterial.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getMaterials().add(new Material(false));
								doModelTreeNewSelection(model.getMaterials().size()-1);
							}
						});
		        		if (!objects.isEmpty()) {
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem delete = new MenuItem(rightClickMenu, SWT.NONE);
			        		delete.setText("Delete");
			        		delete.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									int index = getModelTreeSelectionIndex();
									model.getMaterials().removeAll(objects);
									doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
								}
							});
		        		}
		        		break;
		        	
		        	case SECTIONS:
		        		MenuItem newSection = new MenuItem(rightClickMenu, SWT.NONE);
		        		newSection.setText("New Section");
		        		newSection.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getSections().add(new Section());
								doModelTreeNewSelection(model.getSections().size()-1);
							}
						});
		        		if (!objects.isEmpty()) {
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem delete = new MenuItem(rightClickMenu, SWT.NONE);
			        		delete.setText("Delete");
			        		delete.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									int index = getModelTreeSelectionIndex();
									model.getSections().removeAll(objects);
									doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
								}
							});
		        		}
		        		break;
		        	
		        	case STEPS:
		        		MenuItem newStep = new MenuItem(rightClickMenu, SWT.NONE);
		        		newStep.setText("New Step");
		        		newStep.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								model.getSteps().add(new Step());
								doModelTreeNewSelection(model.getSteps().size()-1);
							}
						});
		        		if (!objects.isEmpty()) {
		        			new MenuItem(rightClickMenu, SWT.SEPARATOR);
		        			MenuItem delete = new MenuItem(rightClickMenu, SWT.NONE);
			        		delete.setText("Delete");
			        		delete.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									int index = getModelTreeSelectionIndex();
									model.getSteps().removeAll(objects);
									doModelTreeNewSelection(getModelTreeSelectionIndexAfterDelete(index));
								}
							});
		        		}
		        		break;
		        	
		        	default:
		        		break;
	        	}
	        }
	    });
		
		compositeLeft = new Composite(sashForm_2, SWT.BORDER);
		compositeLeft.setLayout(new GridLayout());
		sashForm_2.setWeights(new int[] {1, 3});
		
		sashForm = new SashForm(shell, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		sashForm_1 = new SashForm(sashForm, SWT.NONE);
		
		CTabFolder tabFolderView = new CTabFolder(sashForm_1, SWT.BORDER | SWT.SINGLE);
		tabFolderView.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent arg0) {
				view.setFocus();
			}
		});
		tabFolderView.setSimple(false);
		tabFolderView.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		
		CTabItem tbtmView = new CTabItem(tabFolderView, SWT.NONE);
		
		tabFolderView.setSelection(tbtmView);
		
		GLData gldata = new GLData();
        gldata.doubleBuffer = true;
        // need SWT.NO_BACKGROUND to prevent SWT from clearing the window
        // at the wrong times (we use glClear for this instead)
        view = new View( tabFolderView, SWT.NO_BACKGROUND, gldata );
        view.setCurrent();
        GLProfile glprofile = GLProfile.getDefault();
        glcontext = GLDrawableFactory.getFactory( glprofile ).createExternalGLContext();
        
		tbtmView.setControl(view);
		
		CTabFolder tabFolderDiagram = new CTabFolder(sashForm_1, SWT.BORDER | SWT.CLOSE | SWT.SINGLE);
		tabFolderDiagram.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent arg0) {
				diagramArea.setFocus();
			}
		});
		tabFolderDiagram.setSimple(false);
		tabFolderDiagram.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));		
		diagramArea = new DiagramArea(tabFolderDiagram, SWT.NO_BACKGROUND);
		tbtmDiagram = new CTabItem(tabFolderDiagram, SWT.NONE);
		tbtmDiagram.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				hideArea(false);
			}
		});
		tbtmDiagram.setText("Diagram View");
		tbtmDiagram.setControl(diagramArea);
		tabFolderDiagram.setSelection(tbtmDiagram);
		
		CTabFolder tabFolderMatrixView = new CTabFolder(sashForm, SWT.BORDER | SWT.CLOSE | SWT.SINGLE);
		tabFolderMatrixView.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent arg0) {
				sashFormMatrixView.setFocus();
			}
		});
		tabFolderMatrixView.setSimple(false);
		tabFolderMatrixView.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));		
		
		sashFormMatrixView = new SashForm(tabFolderMatrixView, SWT.NONE);
		sashFormMatrixView.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				if (tree.getItemCount() == 0) {
					sashFormMatrixView.setWeights(new int[] {1, 0});
					if (sashFormMatrixView_sashWidth == 0) {
						sashFormMatrixView_sashWidth = sashFormMatrixView.getSashWidth();
					}
					sashFormMatrixView.setSashWidth(0);
				}
			}
		});
		
		tbtmMatrixView = new CTabItem(tabFolderMatrixView, SWT.NONE);
		tbtmMatrixView.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				hideArea(true);
			}
		});
		tbtmMatrixView.setText("Matrix View");
		tbtmMatrixView.setControl(sashFormMatrixView);
		tabFolderMatrixView.setSelection(tbtmMatrixView);
		
		tree = new Tree(sashFormMatrixView, SWT.BORDER);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				updateMatrixView();
			}
		});
		addFocusListener(tree, tabFolderMatrixView);
		/*tree.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				tree.setFocus();
			}
			@Override
			public void mouseExit(MouseEvent e) {
				shell.setFocus();
			}
		});*/
		
		table = new Table(sashFormMatrixView, SWT.BORDER);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setLinesVisible(true);
		addFocusListener(table, tabFolderMatrixView);
		/*table.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				table.setFocus();
			}
			@Override
			public void mouseExit(MouseEvent e) {
				shell.setFocus();
			}
		});*/
		//new TableCursor(table, SWT.NONE);
		
		sashForm_1.setWeights(new int[] {3, 1});
		sashForm.setWeights(new int[] {4, 1});
		
		compositeStatusBar = new Composite(shell, SWT.NONE);
		GridLayout gl_compositeStatusBar = new GridLayout(19, false);
		SimLive.formatGridLayoutForComposite(gl_compositeStatusBar);
		compositeStatusBar.setLayout(gl_compositeStatusBar);
		compositeStatusBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1));
		int widthHint = -1;
		{
			GC gc = new GC(shell);
			char[] c = new char[OUTPUT_DIGITS];
			Arrays.fill(c, '0');
			widthHint = gc.stringExtent("-,E-000 mm"+new String(c)).x;
			gc.dispose();
		}	
		Label x = new Label(compositeStatusBar, SWT.NONE);
		x.setText("x:");
		
		xCoord = new Label(compositeStatusBar, SWT.NONE);
		GridData gd_xCoord = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_xCoord.widthHint = widthHint;
		xCoord.setLayoutData(gd_xCoord);
		
		Label separator = new Label(compositeStatusBar, SWT.SEPARATOR);
		GridData gd_separator = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_separator.heightHint = (int) fontHeight;
		separator.setLayoutData(gd_separator);
		
		Label y = new Label(compositeStatusBar, SWT.NONE);
		y.setText("y:");
		
		yCoord = new Label(compositeStatusBar, SWT.NONE);
		GridData gd_yCoord = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_yCoord.widthHint = widthHint;
		yCoord.setLayoutData(gd_yCoord);
		
		Label separator_1 = new Label(compositeStatusBar, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd_separator_1 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_separator_1.heightHint = (int) fontHeight;
		separator_1.setLayoutData(gd_separator_1);
		
		Label z = new Label(compositeStatusBar, SWT.NONE);
		z.setText("z:");
		
		zCoord = new Label(compositeStatusBar, SWT.NONE);
		GridData gd_zCoord = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_zCoord.widthHint = widthHint;
		zCoord.setLayoutData(gd_zCoord);
		
		Label separator_2 = new Label(compositeStatusBar, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd_separator_2 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_separator_2.heightHint = (int) fontHeight;
		separator_2.setLayoutData(gd_separator_2);
		
		statusBar = new Label(compositeStatusBar, SWT.NONE);
		statusBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label separator_3 = new Label(compositeStatusBar, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd_separator_3 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_separator_3.heightHint = (int) fontHeight;
		separator_3.setLayoutData(gd_separator_3);
		
		Label selectionIcon = new Label(compositeStatusBar, SWT.NONE);
		Image img = INFO_ICON;
		selectionIcon.setImage(resize(img, ICON_HEIGHT_FACTORS[1]));
		
		selection = new Label(compositeStatusBar, SWT.NONE);
		
		Label separator_4 = new Label(compositeStatusBar, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd_separator_4 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_separator_4.heightHint = (int) fontHeight;
		separator_4.setLayoutData(gd_separator_4);
		
		displayPartsIcon = new Label(compositeStatusBar, SWT.NONE);
		
		displayParts = new Label(compositeStatusBar, SWT.NONE);
		
		Label separator_5 = new Label(compositeStatusBar, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd_separator_5 = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_separator_5.heightHint = (int) fontHeight;
		separator_5.setLayoutData(gd_separator_5);
		
		resultIcon = new Label(compositeStatusBar, SWT.NONE);
		
		result = new Label(compositeStatusBar, SWT.NONE);
	}
	
	public static void addFocusListener(Composite composite, CTabFolder tabFolder) {
		composite.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent arg0) {
				tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
				tabFolder.setFont(SimLive.FONT_BOLD);
			}
			@Override
			public void focusLost(FocusEvent arg0) {
				tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				tabFolder.setFont(SimLive.shell.getDisplay().getSystemFont());
			}
		});
	}
	
	private int getModelTreeSelectionIndex() {
		TreeItem[] selection = modelTree.getSelection();
		TreeItem parent = selection[0].getParentItem();
		for (int i = 0; i < parent.getItemCount(); i++) {
			if (parent.getItem(i) == selection[0]) return i;
		}
		return -1;
	}
	
	private int getModelTreeSelectionIndexAfterDelete(int index) {
		int mode = SimLive.mode.ordinal()-1;
		if (index > 0) index--;
		if (index > modelTree.getItems()[mode].getItemCount()-1)
			index = modelTree.getItems()[mode].getItemCount()-1;
		return index;
	}
	
	private static void doModelTreeNewSelection(int index) {
		if (mode != Mode.NONE) {
			model.updateModel();
			int mode = SimLive.mode.ordinal()-1;
			if (modelTree.getItems()[mode].getItemCount() > 0) {
				modelTree.setSelection(modelTree.getItems()[mode].getItem(index));
			}
			else {
				modelTree.setSelection(modelTree.getItem(mode));
			}
			modelTree.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	public static ArrayList<Object> getModelTreeSelection() {
		ArrayList<Object> objects = new ArrayList<Object>();
		TreeItem[] selection = modelTree.getSelection();
		for (int i = 0; i < selection.length; i++) {
			if (selection[i].getData() != null) {
				objects.add(selection[i].getData());
			}
		}
		return objects;
	}
	
	public static void synchronizeModelTreeWithViewSelection() {
		if (mode == Mode.PARTS && modelTree.getItem(0).getExpanded()) {
			ArrayList<Set> selectedSets = view.getSelectedSets();
			ArrayList<Part3d> selectedParts3d = view.getSelectedParts3d();
			TreeItem[] items = modelTree.getItem(0).getItems();
			if (!selectedSets.isEmpty()) {
				TreeItem[] selection = new TreeItem[selectedSets.size()];
				for (int s = 0; s < selectedSets.size(); s++) {
					selection[s] = items[selectedSets.get(s).getID()];
				}
				modelTree.setSelection(selection);
			}
			else if (!selectedParts3d.isEmpty()) {
				TreeItem[] selection = new TreeItem[selectedParts3d.size()];
				for (int s = 0; s < selectedParts3d.size(); s++) {
					selection[s] = items[model.getSets().size()+selectedParts3d.get(s).getID()];
				}
				modelTree.setSelection(selection);
			}
			else {
				modelTree.setSelection(modelTree.getItem(0));
			}
		}
	}
	
	public static void importPart3d() {
		FileDialog fileDialog = new FileDialog(SimLive.shell, SWT.OPEN);
		String[] filter = new String[1];
		filter[0] = "*.obj";
		fileDialog.setFilterExtensions(filter);
		if (fileDialog.open() != null) {
			try {
				SimLive.freezeGUI(true);
				InputStream objInputStream = new FileInputStream(fileDialog.getFilterPath()+
						System.getProperty("file.separator")+fileDialog.getFileName());
				
				Obj obj = ObjReader.read(objInputStream);
				obj = ObjUtils.triangulate(obj);
				List<Mtl> mtls = null;
				int offset = SimLive.model.getPart3dColors().size();
				try {
					InputStream mtlInputStream = new FileInputStream(fileDialog.getFilterPath()+
							System.getProperty("file.separator")+obj.getMtlFileNames().get(0));
					mtls = MtlReader.read(mtlInputStream);
					
					for (int i = 0; i < mtls.size(); i++) {
						Mtl mtl = mtls.get(i);
						FloatTuple kd = mtl.getKd();
						FloatTuple ks = mtl.getKs();
						Part3dColor part3dColor = new Part3dColor();
						part3dColor.setKd(kd.getX(), kd.getY(), kd.getZ());
						part3dColor.setKs(ks.getX(), ks.getY(), ks.getZ());
						part3dColor.setShininess(mtl.getNs());
						SimLive.model.getPart3dColors().add(part3dColor);
					}
				}
				catch (Exception e) {
					SimLive.messageBox(false, "No colors found.");
				}
				
				for (int colorIndex = 0, g = 0; g < obj.getNumGroups(); g++) {
					ObjGroup group = obj.getGroup(g);
					Obj groupObj = ObjUtils.groupToObj(obj, group, null);
					
					Part3d obj3d = new Part3d(groupObj.getNumVertices(), groupObj.getNumFaces());
					SubTree subTree = new SubTree();
					subTree.nrVertices = groupObj.getNumVertices();
					subTree.nrFacets = groupObj.getNumFaces();
					obj3d.setSubTree(subTree);
					for (int v = 0; v < groupObj.getNumVertices(); v++) {
						double[] vertex = new double[3];
						vertex[0] = groupObj.getVertex(v).getX();
						vertex[1] = groupObj.getVertex(v).getY();
						vertex[2] = groupObj.getVertex(v).getZ();
						obj3d.setVertex(new Vertex3d(vertex), v);
					}
					if (groupObj.getNumMaterialGroups() > 0) {
						String materialName = groupObj.getMaterialGroup(0).getName();
						colorIndex = Part3dColor.getIndexOfColor(materialName, mtls, offset);								
					}
					for (int f = 0; f < groupObj.getNumFaces(); f++) {
						ObjFace face = groupObj.getFace(f);
						int[] indices = new int[3];
						indices[0] = face.getVertexIndex(0);
						indices[1] = face.getVertexIndex(1);
						indices[2] = face.getVertexIndex(2);
						obj3d.setFacet(new Facet3d(indices, colorIndex), f);
					}
					
					SimLive.model.getParts3d().add(obj3d);
				}
				SimLive.model.updateModel();
				SimLive.view.deselectAllAndDisposeDialogs();
				SimLive.view.fitToView();
				SimLive.freezeGUI(false);
			}
			catch (Exception e) {
				SimLive.messageBox(true, "Error reading from file.");
			}
		}
	}
	
	private void initArea(boolean matrixArea) {
		CTabItem tabItem = null;
		if (matrixArea) {
			CTabFolder tabFolderMatrixView = (CTabFolder) sashForm.getChildren()[1];
			tbtmMatrixView = new CTabItem(tabFolderMatrixView, SWT.NONE);
			tbtmMatrixView.setText("Matrix View");
			tbtmMatrixView.setControl(sashFormMatrixView);
			tabFolderMatrixView.setSelection(tbtmMatrixView);
			tabItem = tbtmMatrixView;
		}
		else {
			CTabFolder tabFolderDiagram = (CTabFolder) sashForm_1.getChildren()[1];
			tbtmDiagram = new CTabItem(tabFolderDiagram, SWT.NONE);
			tbtmDiagram.setText("Diagram View");
			tbtmDiagram.setControl(diagramArea);
			tabFolderDiagram.setSelection(tbtmDiagram);
			tabItem = tbtmDiagram;
		}
		showArea(matrixArea);
		tabItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				hideArea(matrixArea);
			}
		});
	}
	
	private static void hideArea(boolean matrixArea) {
		if (!shellClosed) {
			if (matrixArea) {
				sashForm_Weights = sashForm.getWeights();
				sashForm_SashWidth = sashForm.getSashWidth();
				sashForm.setWeights(new int[] {1, 0});
				sashForm.setSashWidth(0);
			}
			else {
				sashForm_1_Weights = sashForm_1.getWeights();
				sashForm_1_SashWidth = sashForm_1.getSashWidth();
				sashForm_1.setWeights(new int[] {1, 0});
				sashForm_1.setSashWidth(0);
			}
		}
	}
	
	private static void showArea(boolean matrixArea) {
		if (matrixArea) {
			sashForm.setWeights(sashForm_Weights);
			sashForm.setSashWidth(sashForm_SashWidth);
		}
		else {
			sashForm_1.setWeights(sashForm_1_Weights);
			sashForm_1.setSashWidth(sashForm_1_SashWidth);
		}
	}
	
	private void resetState() {
		if (view.isAnimationRunning()) {
			view.stopAnimation();
		}
		if (post != null) {
			post.removeMinMaxLabels();
			diagramArea.setDisplay(SimLive.post.getScalarPlot());			
		}
		disposeDialogAreas();
		//view.deselectAll();
		statusBar.setText("");
		setDisplayPartsLabel();
		setSelectionLabel();
		view.setCursor(null);
		View.Rr = null;
		SimLive.model.updateModel();
		((CTabFolder) view.getParent()).getItem(0).setText("Model View");
		SimLive.view.lockSelectParts3d = false;
		view.redraw();
	}
	
	private void resetToPartsMode() {
		modelTree.setSelection(modelTree.getItem(0));
		modelTree.notifyListeners(SWT.Selection, new Event());
		tabFolderModel.setSelection(0);
		tabFolderModel.notifyListeners(SWT.Selection, new Event());
		for (int i = 0; i < modelTree.getItemCount(); i++) {
			modelTree.getItems()[i].setExpanded(false);
		}
	}
	
	public static void setDisplayPartsLabel() {
		Image img = null;
		String text = null;
		if (view.doSetsOnlyContain(model.getSets(), Set.View.DEFAULT)) {
			text = "Default Display Of Parts";
			img = OK_ICON;
		}
		else {
			text = "Parts Are Hidden Or Pinned";
			img = WARNING_ICON;
		}
		displayPartsIcon.setImage(resize(img, ICON_HEIGHT_FACTORS[1]));
		displayParts.setText(text);
		compositeStatusBar.layout();
	}
	
	private void setSelectionLabel() {
		String string = null;
		switch (select) {
			case DISTANCE: 	string = "Measure Distance";
							break;
			case ANGLE: 	string = "Measure Angle";
							break;
			case LABEL: 	string = "Create Labels";
							break;
			default: 		string = "Default Selection";
		}
		selection.setText(string);
		compositeStatusBar.layout();
	}
	
	public static boolean isInputValid(VerifyEvent e, boolean negativeInputAllowed) {
		char sep=((DecimalFormat) DecimalFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
		String oldInput = ((Text) e.getSource()).getText();
		String input = oldInput.substring(0, e.start) + e.text + oldInput.substring(e.end);
		if (!negativeInputAllowed && !input.isEmpty()) {
			if (input.charAt(0) == '-') return false;
		}
		for (int index = 0; index < input.length(); index++) {
	        if (!Character.isDigit(input.charAt(index)) && !(input.charAt(index) == sep ||
	        	input.charAt(index) == '-' || input.charAt(index) == 'e' ||
	        	input.charAt(index) == 'E')) return false;
	    }
		return true;
	}
	
	public static String double2String(double value) {
		if (!Double.isFinite(value)) {
			return "NaN";
		}
		BigDecimal bigDecimal = new BigDecimal(value);
		int exp = bigDecimal.precision()-bigDecimal.scale()-1;
		DecimalFormat decimalFormat = null;
		if (Math.abs(exp) > SimLive.OUTPUT_DIGITS-1) {
			String format = "#.";
			for (int i = 0; i < SimLive.OUTPUT_DIGITS-1; i++) {
				format += "#";
			}
			format += "E0";
			decimalFormat = new DecimalFormat(format);
		}
		else {
			decimalFormat = (DecimalFormat) NumberFormat.getInstance();
			int limit = SimLive.OUTPUT_DIGITS-1;
			if (exp > 0) {
				limit -= exp;
			}
			decimalFormat.setMaximumFractionDigits(limit);
		}
		return decimalFormat.format(value).replace(Character.toString(decimalFormat.getDecimalFormatSymbols().getGroupingSeparator()),"");
	}
	
	public static double string2Double(String string) {
		try {
			return NumberFormat.getInstance().parse(string.replace('e', 'E')).doubleValue();
		}
		catch (ParseException e) {
			return 0;
		}
	}
	
	public static String getInputString(Text text) {
		text.selectAll();
		return text.getText();
	}
	
	public static double getInputDouble(Text text) {
		return getInputDouble(text, -Double.MAX_VALUE, Double.MAX_VALUE);
	}
	
	public static double getInputDouble(Text text, double minimum, double maximum) {
		double value = SimLive.string2Double(text.getText());
		if (value < minimum) {
			value = minimum;
		}
		if (value > maximum) {
			value = maximum;
		}
		text.setText(SimLive.double2String(value));
		text.selectAll();
		return value;
	}
	
	public static void disposeDialogAreas() {
		if (dialogArea != null) {
			dialogArea.dispose();
		}
	}

	public static void formatGridLayoutForComposite(GridLayout gridLayout) {
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
	}

	public static void updateMatrixView() {
		if (settings.isWriteMatrixView) {			
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (post != null) {
						post.getPostIncrement().updateTree(tree, post.getPostIncrementID());
						post.getPostIncrement().updateTable(table, tree);
					}
				}
			});
		}
	}
	
	private static void initMatrixView() {
		if (settings.isWriteMatrixView) {			
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (post != null) {
						post.getPostIncrement().initTree(tree, post.getPostIncrementID());
						post.getPostIncrement().initTable(table);
						int leftWidth = tree.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
						if (sashFormMatrixView_sashWidth != 0) {
							sashFormMatrixView.setSashWidth(sashFormMatrixView_sashWidth);
						}
						setSashFormMatrixView(leftWidth);
					}
				}
			});
		}
	}
	
	public static void initPost(Solution solution) {
		SimLive.shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				tree.removeAll();
			}
		});
		post = new Post(solution);
		post.setAutoScaling(true);
		initMatrixView();
		view.drawingApplicationInitLines();
	}
	
	private static void resetPost() {
		post = null;
		diagramArea.reset();
		tree.removeAll();
		sashFormMatrixView.setWeights(new int[] {1, 0});
		while (table.getColumnCount() > 0) {
		    table.getColumns()[0].dispose();
		}
		table.removeAll();
		
		//setResultLabel(null, false, false, false);
		/*lastSolution = null;
		setResultLabel(null, result, false, false, 16);
		if (dialogArea instanceof SolutionDialog) {
			((SolutionDialog) dialogArea).setToNoSolution();
		}*/
	}
	
	private static void setSashFormMatrixView(int leftWidth) {
		int width = sashFormMatrixView.getClientArea().width;
		int[] weights = new int[2];
		weights[0] = leftWidth;
	    weights[1] = Math.max(width - weights[0], 0);
	    sashFormMatrixView.setWeights(weights);
	}
	
	public static Image resize(Image image, double iconHeightFactor) {
		int height = (int) Math.round(iconHeightFactor*fontHeight);
		double scale = height/(double) image.getBounds().height;
		int width = (int) Math.round(image.getBounds().width*scale);		
		Image scaled = new Image(Display.getDefault(), width, height);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.drawImage(image, 0, 0,image.getBounds().width, image.getBounds().height, 0, 0, width, height);
		ImageData imageData = scaled.getImageData();
		imageData.transparentPixel = imageData.palette.getPixel(gc.getBackground().getRGB());
		gc.dispose();		
		//image.dispose(); /* destroy original */
		scaled.dispose();
		return new Image(Display.getDefault(), imageData);
	}
	
	public static void setResultLabel(Composite resultComposite, boolean isUpdateModel, boolean isReorderNodes, boolean isCalculating) {
		Image img = null;
		String text = null;
		if (!Solution.errors.isEmpty()) {
			text = Solution.results[0];
			img = ERROR_ICON;
		}
		else if (!Solution.warnings.isEmpty()) {
			text = Solution.results[1];
			img = WARNING_ICON;
		}
		else if (!Solution.log.isEmpty()) {
			text = Solution.results[2];
			img = OK_ICON;
		}
		else if (isUpdateModel) {
			text = Solution.results[3];
			img = INFO_ICON;
		}
		else if (isReorderNodes) {
			text = Solution.results[4];
			img = INFO_ICON;
		}
		else if (isCalculating) {
			text = Solution.results[5];
			img = INFO_ICON;
		}
		else {
			text = Solution.results[6];
			img = INFO_ICON;
		}
		result.setText(text);
		resultIcon.setImage(resize(img, ICON_HEIGHT_FACTORS[1]));
		compositeStatusBar.layout();
		if (resultComposite != null) {
			((Label) resultComposite.getChildren()[0]).setImage(resize(img, ICON_HEIGHT_FACTORS[2]));
			((Label) resultComposite.getChildren()[1]).setText(text);
		}
	}
	
	public static Color floatToColor(float[] color) {
		return new Color(shell.getDisplay(), (int) (color[0]*255f), (int) (color[1]*255f), (int) (color[2]*255f));
	}
	
	public static void messageBox(boolean error, String text) {
		MessageBox messageBox = null;
		if (error) {
			messageBox = new MessageBox(shell, SWT.ERROR);
			messageBox.setText("Error");
		}
		else {
			messageBox = new MessageBox(shell, SWT.ICON_INFORMATION);
			messageBox.setText("Info");
		}
        messageBox.setMessage(text);
        messageBox.open();
	}
	
	public static void freezeGUI(final boolean freeze) {
		SimLive.shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				Control[] children = shell.getChildren();
				for(Control control : children) {
					control.setEnabled(!freeze);
				}
				if (freeze) {
					shell.setCursor(new Cursor(SimLive.shell.getDisplay(), SWT.CURSOR_WAIT));
				}
				else {
					shell.setCursor(null);					
				}
			}
		});
	}
	
	/*public static void recursiveSetEnabled(Control ctrl, boolean enabled) {
		if (ctrl instanceof Composite) {
			Composite comp = (Composite) ctrl;
			for (Control c : comp.getChildren()) {
				recursiveSetEnabled(c, enabled);
			}
			comp.setEnabled(enabled);
		}
		else {
			ctrl.setEnabled(enabled);
		}
	}*/
	
	public static boolean contains(int[] array, int value) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == value) return true;
		}
		return false;
	}
	
	public static int[] add(int[] array, int value) {
		array = Arrays.copyOf(array, array.length+1);
		array[array.length-1] = value;
		return array;
	}
	
	public static int[] remove(int[] array, int value) {
		boolean shift = false;
		for (int i = 0; i < array.length; i++) {
			if (shift) {
				array[i-1] = array[i];	
			}
			if (array[i] == value) {
				shift = true;
			}
		}
		return Arrays.copyOf(array, array.length-1);
	}
	
	public static boolean deepEquals(ArrayList<?> list1, ArrayList<?> list2) {
		if (list1.size() != list2.size()) return false;
		for (int i = 0; i < list1.size(); i++) {
			if (!list1.get(i).getClass().equals(list2.get(i).getClass())) return false;
			DeepEqualsInterface o1 = (DeepEqualsInterface) list1.get(i);
			DeepEqualsInterface o2 = (DeepEqualsInterface) list2.get(i);
			if (!o1.deepEquals(o2)) return false;
		}
		return true;
	}
	
	private void checkModelChange() {
		if (mode != Mode.RESULTS) {
			if (post != null) {
				if (!post.getSolution().getRefModel().deepEquals(model) ||
					!post.getSolution().getRefSettings().deepEquals(settings)) {
					SimLive.shell.getDisplay().syncExec(new Runnable() {
						public void run() {
							resetPost();
						}
					});
				}
			}
		}
	}
	
	private void regularChecks() {
		if (mode != Mode.RESULTS && !shell.isDisposed() && shell.getChildren()[0].isEnabled()) {
			if (post != null && (checkModel == null || !checkModel.isAlive())) {
				checkModel = new Thread(new Runnable() {
					public void run() {
						checkModelChange();
					}
				});
				checkModel.start();
			}
		}
		
		if (!tltmUndo.isDisposed() && !tltmRedo.isDisposed()) {
			tltmUndo.setEnabled(modelPos > 0 && mode != Mode.RESULTS);
			tltmRedo.setEnabled(modelPos < modelHistory.size()-1 && mode != Mode.RESULTS);
		}
		if (!tltmOrientations.isDisposed() && !tltmSections.isDisposed() && !tltmNodes.isDisposed() &&
				!tltmEdges.isDisposed() && !tltmGrid.isDisposed()) {
			tltmOrientations.setSelection(settings.isShowOrientations);
			tltmSections.setSelection(settings.isShowSections);
			tltmNodes.setSelection(settings.isShowNodes);
			tltmEdges.setSelection(settings.isShowEdges);
			tltmGrid.setSelection(settings.isShowGrid);
		}
		if (!tltmMeasureDistance.isDisposed() &&
				!tltmMeasureAngle.isDisposed() && !tltmCreateLabels.isDisposed()) {
			tltmMeasureDistance.setSelection(select == Select.DISTANCE);
			tltmMeasureAngle.setSelection(select == Select.ANGLE);
			tltmCreateLabels.setSelection(select == Select.LABEL);
		}
	}
	
	private void undoRedo(boolean undo) {
		view.deselectAllAndDisposeDialogs();
		model.updateModel();
		if (undo) {
			modelPos--;
		}
		else {
			modelPos++;
		}
		model = model.expandModel(modelPos).clone();
		//resetState();
		resetToPartsMode();
		view.redraw();
	}
	
	private boolean newFile(String fileName) {
		
		if (fileName == null) {
			modelHistory = new ArrayList<Model>();
			modelPos = -1;
			
			resetPost();
			Solution.resetLog();
			
			model = new Model();
			settings = new Settings();
			view.initializeZoomAndOrigin(settings.meshSize);
			
			view.initViewForNewFile();
			
			/* default materials */
			model.getMaterials().addAll(Material.getDefaultMaterials());
			
			/* default sections */
			model.getSections().add(Section.getDefaultSection());
			
			/* default step */
			model.getSteps().add(new Step());

			/* default part3dColor */
			model.getPart3dColors().add(Part3dColor.getDefaultColor());
			
			XML.setFilePath(null);
			shell.setText(APPLICATION_NAME+" "+VERSION_NAME);
		}
		else {
			if (!XML.readFileAndGenerateModel(fileName) ||
			    !XML.readFileAndGenerateSettings(fileName)) {
				return false;
			}
			
			modelHistory = new ArrayList<Model>();
			modelPos = -1;
			
			resetPost();
			Solution.resetLog();
			
			view.initViewForNewFile();
			
			XML.readFileAndGenerateSolution(fileName);
			XML.setFilePath(fileName);
			shell.setText(XML.getFilePath()+" - "+APPLICATION_NAME+" "+VERSION_NAME);
		}
		
		boxSelect = BoxSelect.NODES;
		select = Select.DEFAULT;
		resetState();
		resetToPartsMode();
		setResultLabel(null, false, false, false);
		
		return true;
	}
}
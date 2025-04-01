package simlive.view;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import org.eclipse.swt.opengl.GLCanvas;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.SimLive.BoxSelect;
import simlive.SimLive.Select;
import simlive.dialog.CircularAreaDialog;
import simlive.dialog.ConnectorDialog;
import simlive.dialog.GeometricAreaDialog;
import simlive.dialog.NodeDialog;
import simlive.dialog.Part3dDialog;
import simlive.dialog.PartDialog;
import simlive.dialog.RectangularAreaDialog;
import simlive.dialog.ResultsDialog;
import simlive.dialog.SpurGearDialog;
import simlive.dialog.StoreDialog;
import simlive.dialog.TriangularAreaDialog;
import simlive.misc.GeomUtility;
import simlive.misc.Search;
import simlive.misc.Snap;
import simlive.misc.Units;
import simlive.model.AbstractConnector;
import simlive.model.AbstractLoad;
import simlive.model.Beam;
import simlive.model.Connector;
import simlive.model.Connector3d;
import simlive.model.ContactPair;
import simlive.model.DistributedLoad;
import simlive.model.Element;
import simlive.model.Facet3d;
import simlive.model.LineElement;
import simlive.model.Load;
import simlive.model.Material;
import simlive.model.Model;
import simlive.model.Node;
import simlive.model.Part3d;
import simlive.model.Part3d.Render;
import simlive.model.PlaneElement;
import simlive.model.PointMass;
import simlive.model.Quad;
import simlive.model.Set;
import simlive.model.Spring;
import simlive.model.Step;
import simlive.model.SubTree;
import simlive.model.Support;
import simlive.model.Tri;
import simlive.model.Rod;
import simlive.model.Section;
import simlive.model.SectionShape;
import simlive.model.Vertex3d;
import simlive.model.AbstractConnector.ConnectorType;
import simlive.model.AbstractLoad.LoadType;
import simlive.model.ContactPair.Type;
import simlive.model.Step.GRAVITY;
import simlive.postprocessing.ScalarPlot;
import simlive.postprocessing.Post.Layer;

import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

public class View extends GLCanvas {

	public static boolean perspective = false;
	private double fovy = 0;
	private static double[] cameraRefPos = {0, 0, 0};
	public static Matrix R0 = Matrix.identity(3, 3);
	private static Matrix R = Matrix.identity(3, 3);
	private boolean rotAroundAxis = false;
	private double[] rotNorm = null;
	private static int[] mouseDown = new int[2];
	private static double[] move = new double[2];
	public static double zoom = 1;
	private static double zoom0 = 1;
	public ArrayList<Label> labels = new ArrayList<Label>();
	public ArrayList<Measurement> measurements = new ArrayList<Measurement>();
	private boolean isMouseDown = false;
	private boolean isMouseDragged = false;
	private Node moveNode = null;
	private double[] movePoint = new double[3];
	private int mouseButton = 0;
	private int[] mousePos = new int[2];
	private boolean robotMove = false;
	private ArrayList<Part3d> selectedParts3d = new ArrayList<Part3d>();
	private ArrayList<Set> selectedSets = new ArrayList<Set>();
	private ArrayList<Node> selectedNodes = new ArrayList<Node>();
	public Label selectedLabel = null;
	public Label focusPoint = null;
	public boolean setFocusPoint = false;
	private double[] rotPoint = new double[3];
	public Matrix focusPointOrientation = null;
	public Measurement selectedMeasurement = null;
	private Rectangle selectionBox = null;
	public Label labelAtMousePos = null;
	public boolean lockSelectParts3d = false;
	
	public boolean isControlKeyPressed;
	private TimerTask animation = null;
	public boolean backwards = false;
	
	public int[][] outlineEdge;
	public int[][] innerEdge;
	public int[][] smoothEdge;
	public boolean[] isOutlineNode;
	public boolean[] isCornerNode;
	
	public static double[][][] Rr = null;
	public static double[] deltaL = null;
	public static double[][] nodeNormals = null;
	private static double[][][] outlineNormals = null;
	private static Matrix[][] outlineNormals0 = null;
	public static double[][][] pVertices = null;
	public static double[][][] pPart3dBox = null;
	
	private static double[][][] pElementScreenPolys = null;
	
	private TextRenderer renderer;
	private TextRenderer rendererBold;
	private float descent;
	
	public enum Side {NONE, X, MINUS_X, Y, MINUS_Y, Z, MINUS_Z}
	public static Side side = Side.NONE;
	
	private static FloatBuffer imgBuffer;
	
	private int shaderProgram = -1;
	
	//drawing application
	public int nodeID;
	public double zDisp, zCoord;
	private double[][] lines;
	
	public View(Composite parent, int style, GLData gldata) {
		super(parent, style, gldata);
		
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				initFOV();
			}
		});
		SimLive.addFocusListener(this, (CTabFolder) parent);
		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(MouseEvent e) {
				//SimLive.shell.setFocus();
				labelAtMousePos = null;
				SimLive.statusBar.setText("");
				SimLive.xCoord.setText("");
				SimLive.yCoord.setText("");
				SimLive.zCoord.setText("");
				redraw();
			}
			@Override
			public void mouseEnter(MouseEvent e) {
				//setFocus();
				setCursor(new Cursor(getDisplay(), SWT.CURSOR_CROSS));
				mousePos[0] = e.x;
				mousePos[1] = e.y;
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(MouseEvent arg0) {
				if (arg0.count > 0) /* zoom in */ {
					zoomIn();
				}
				else /* zoom out */ {
					zoomOut();
				}
				redraw();
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				/* empty keyListener required to get focus on canvas if clicked */
				timeControlByKeys(e);
				if (e.keyCode == SWT.DEL) {
					deleteSelectedLabel();
					deleteSelectedMeasurement();
					
					if (SimLive.mode == Mode.PARTS) {
						if (!selectedSets.isEmpty()) {
							deleteSelectedSets();
						}
						if (!selectedParts3d.isEmpty()) {
							deleteSelectedParts3d();
						}
						SimLive.model.updateModel();
					}
				}
				if (e.keyCode == SWT.ESC) {
					deselectAllAndDisposeDialogs();
				}
				redraw();
			}
		});
		addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent arg0) {
				if (robotMove) {
					robotMove = false;
					return;
				}
				/* drag threshold */
				if (!isMouseDragged && mouseButton > 0 &&
						Math.abs(arg0.x-mouseDown[0]) < SimLive.SNAP_TOL &&
						Math.abs(arg0.y-mouseDown[1]) < SimLive.SNAP_TOL) return;
				
				isMouseDragged = mouseButton > 0;
				
				int[] mousePosOld = new int[2];
				mousePosOld[0] = mousePos[0];
				mousePosOld[1] = mousePos[1];
				
				mousePos[0] = arg0.x;
				mousePos[1] = arg0.y;
				
				int[] mousePosDelta = new int[2];
				mousePosDelta[0] = mousePos[0]-mousePosOld[0];
				mousePosDelta[1] = mousePos[1]-mousePosOld[1];
				
				if (!isMouseDragged && animation == null) {
					selectedLabel = Search.getLabelAtPoint(mousePos);
					selectedMeasurement = selectedLabel == null ? Search.getMeasurementAtPoint(mousePos) : null; 
				}
				
				if (mouseButton == 0) {
					Snap.initData(mousePos, true, true);
				}
					
				if (rotAroundAxis) /*rotate around axis*/ {
					Snap.initData(mousePos, false, false);
					int xMove = arg0.x-mouseDown[0];
					int yMove = -(arg0.y-mouseDown[1]);
					R = rotateAroundAxis(xMove, yMove);
					redraw();
					return;
				}
				
				if (mouseButton == 2)  /*rotate view*/  {
					Snap.initData(mousePos, false, false);
				}				
				if (mouseButton == 3)  /*move view*/  {
					Snap.initData(mousePos, false, false);
					move[0] += mousePosDelta[0];
					move[1] += mousePosDelta[1];
				}
				if (mouseButton == 1 && selectionBox == null) /* move label */ {
					if (selectedLabel != null) {
						Snap.initData(mousePos, false, false);
						selectedLabel.toFront();
						selectedLabel.addToMove(mousePosDelta);
					}
				}
				if (mouseButton == 1 && selectionBox == null) /* move measurement */ {
					if (selectedMeasurement != null) {
						Snap.initData(mousePos, false, false);
						selectedMeasurement.toFront();
						double[] c0 = screenToModelCoordinates(mousePosOld[0], mousePosOld[1]);
						double[] c1 = screenToModelCoordinates(mousePos[0], mousePos[1]);
						double[] viewDirC0 = getViewDirection(c0);
						double[] viewDirC1 = getViewDirection(c1);
						Matrix moveOld = new Matrix(selectedMeasurement.getMove(), 3);
						if (selectedMeasurement.getType() == Measurement.Type.DISTANCE) {
							double[] start = selectedMeasurement.getStartPoint();
							double[] end = selectedMeasurement.getEndPoint();
							Matrix dir = new Matrix(new double[]{end[0]-start[0], end[1]-start[1], end[2]-start[2]}, 3);
							dir = dir.times(1.0/dir.normF());
							if (moveOld.normF() < SimLive.ZERO_TOL) {
								Matrix z = new Matrix(new double[]{0, 0, 1}, 3);
								moveOld = dir.crossProduct(R0.times(z));
							}
							Matrix moveDir = moveOld.times(1.0/moveOld.normF());
							
							if (isControlKeyPressed) {
								Matrix mid = (new Matrix(start, 3).plus(new Matrix(end, 3))).times(0.5);
								double[] p0 = mid.plus(moveOld).getColumnPackedCopy();
								double[] p1 = mid.plus(moveOld.crossProduct(dir)).getColumnPackedCopy();
								double[] p = GeomUtility.getIntersectionLinePlane(c1,
										viewDirC1, mid.getColumnPackedCopy(), p0, p1);
								if (p != c1) {
									Matrix move = new Matrix(p, 3).minus(mid);
									selectedMeasurement.setMove(move.times(moveOld.normF()/move.normF()).getColumnPackedCopy());
								}
							}
							else {
								double[] p2 = new Matrix(start, 3).plus(moveOld).getColumnPackedCopy();
								double[] c2 = GeomUtility.getIntersectionLinePlane(c0, viewDirC0, start, end, p2);
								double[] c3 = GeomUtility.getIntersectionLinePlane(c1, viewDirC1, start, end, p2);
								double move = moveDir.dotProduct(new Matrix(c3, 3).minus(new Matrix(c2, 3)));
								selectedMeasurement.setMove(moveOld.plus(moveDir.times(move)).getColumnPackedCopy());
							}
						}
						if (selectedMeasurement.getType() == Measurement.Type.ANGLE) {
							double[] start = selectedMeasurement.getStartPoint();
							double[] mid = selectedMeasurement.getMidPoint();
							double[] end = selectedMeasurement.getEndPoint();
							Matrix dir0 = new Matrix(new double[]{start[0]-mid[0], start[1]-mid[1], start[2]-mid[2]}, 3);
							Matrix dir1 = new Matrix(new double[]{end[0]-mid[0], end[1]-mid[1], end[2]-mid[2]}, 3);
							Matrix dir = dir0.times(1.0/dir0.normF()).plus(dir1.times(1.0/dir1.normF()));
							if (dir.normF() > SimLive.ZERO_TOL) {
								dir = dir.times(1.0/dir.normF());
								double[] c2 = GeomUtility.getIntersectionLinePlane(c0, viewDirC0, start, mid, end);
								double[] c3 = GeomUtility.getIntersectionLinePlane(c1, viewDirC1, start, mid, end);
								double move = dir.dotProduct(new Matrix(c3, 3).minus(new Matrix(c2, 3)));
								selectedMeasurement.setMove(moveOld.plus(dir.times(move)).getColumnPackedCopy());
							}
						}
					}
				}
				
				if (mouseButton == 1 && selectedMeasurement == null && selectedLabel == null) {
					
					if (selectedNodes.size() == 1 && selectedNodes.get(0) == moveNode &&
							SimLive.mode == Mode.PARTS && selectionBox == null &&
							!isControlKeyPressed)  /*move node*/ {
						
						Snap.initData(mousePos, true, true);
						
						double[] coords = null;
						if (Snap.node != null) {
							coords = Snap.node.getCoords();
						}
						else {
							coords = Snap.coords2d;
						}
						double[] deltaCoords = new double[3];
						deltaCoords[0] = coords[0]-selectedNodes.get(0).getXCoord();
						deltaCoords[1] = coords[1]-selectedNodes.get(0).getYCoord();
						deltaCoords[2] = coords[2]-selectedNodes.get(0).getZCoord();
						((NodeDialog) SimLive.dialogArea).updateDialog(deltaCoords);				
					}
					else if (!selectedParts3d.isEmpty() && selectedParts3d.contains(Snap.part3d) &&
							SimLive.mode == Mode.PARTS && selectionBox == null &&
							!isControlKeyPressed /*&& Math.abs(R0.get(2, 2)) == 1.0*/) /* move parts3d */ {
						Snap.initData(mousePos, false, false);
						
						double[] deltaCoords = new double[3];
						double[] coords = screenToModelCoordinates(mousePosOld[0], mousePosOld[1]);
						deltaCoords[0] = Snap.coords2d[0]-coords[0];
						deltaCoords[1] = Snap.coords2d[1]-coords[1];
						if (perspective) {
							double d1 = Math.sqrt((cameraRefPos[0]-coords[0])*(cameraRefPos[0]-coords[0])+
									(cameraRefPos[1]-coords[1])*(cameraRefPos[1]-coords[1])+
									(cameraRefPos[2]-coords[2])*(cameraRefPos[2]-coords[2]));
							double d2 = Math.sqrt((movePoint[0]-coords[0])*(movePoint[0]-coords[0])+
									(movePoint[1]-coords[1])*(movePoint[1]-coords[1])+
									(movePoint[2]-coords[2])*(movePoint[2]-coords[2]));
							deltaCoords[0] *= (d1-d2)/d1;
							deltaCoords[1] *= (d1-d2)/d1;
							movePoint[0] += deltaCoords[0];
							movePoint[1] += deltaCoords[1];
						}
						((Part3dDialog) SimLive.dialogArea).updateDialog(deltaCoords);
					}
					else if (!selectedSets.isEmpty() && (selectedSets.contains(Snap.set) ||
							!Collections.disjoint(SimLive.model.getSetsByNode(moveNode), selectedSets)) &&
							SimLive.mode == Mode.PARTS && selectionBox == null &&
							!isControlKeyPressed)  /*move elements*/ {
						
						double[] deltaCoords = new double[3];
						
						if (moveNode == null) {
							Snap.initData(mousePos, false, false);
							
							double[] coords = screenToModelCoordinates(mousePosOld[0], mousePosOld[1]);
							deltaCoords[0] = Snap.coords2d[0]-coords[0];
							deltaCoords[1] = Snap.coords2d[1]-coords[1];
							if (perspective) {
								double d1 = Math.sqrt((cameraRefPos[0]-coords[0])*(cameraRefPos[0]-coords[0])+
										(cameraRefPos[1]-coords[1])*(cameraRefPos[1]-coords[1])+
										(cameraRefPos[2]-coords[2])*(cameraRefPos[2]-coords[2]));
								double d2 = Math.sqrt((movePoint[0]-coords[0])*(movePoint[0]-coords[0])+
										(movePoint[1]-coords[1])*(movePoint[1]-coords[1])+
										(movePoint[2]-coords[2])*(movePoint[2]-coords[2]));
								deltaCoords[0] *= (d1-d2)/d1;
								deltaCoords[1] *= (d1-d2)/d1;
								movePoint[0] += deltaCoords[0];
								movePoint[1] += deltaCoords[1];
							}
						}
						else {
							Snap.initData(mousePos, true, true);
							
							double[] coords = null;
							if (Snap.node != null) {
								coords = Snap.node.getCoords();
							}
							else {
								coords = Snap.coords2d;
							}
							deltaCoords[0] = coords[0]-moveNode.getXCoord();
							deltaCoords[1] = coords[1]-moveNode.getYCoord();
							deltaCoords[2] = coords[2]-moveNode.getZCoord();
						}
						
						((PartDialog) SimLive.dialogArea).updateDialog(deltaCoords);
					}
					else if (mouseButton == 1 && selectedLabel == null)  /*selection box*/  {
						
						Snap.initData(mousePos, false, false);
						
						if (selectionBox == null) {
							if (!isControlKeyPressed) {
								deselectAll();
							}
						}
						
						selectionBox = new Rectangle(mousePos[0], mousePos[1],
								mouseDown[0]-mousePos[0], mouseDown[1]-mousePos[1]);
						
						if (animation == null) {
							if (SimLive.boxSelect == SimLive.BoxSelect.NODES) {
								selectNodesInBox();
							}
							if (SimLive.boxSelect == SimLive.BoxSelect.PARTS) {
								selectSetsInBox();
							}
							if (SimLive.boxSelect == SimLive.BoxSelect.PARTS_3D) {
								selectParts3dInBox();
							}
						}
					}
					
				}
				
				if (mouseButton == 0 && SimLive.mode == Mode.PARTS &&
					SimLive.dialogArea instanceof GeometricAreaDialog && !SimLive.dialogArea.isDisposed()) {
					Node node = Snap.node != null ? Snap.node : new Node(Snap.coords2d[0], Snap.coords2d[1], 0);
					((GeometricAreaDialog) SimLive.dialogArea).updateDialog(node);
				}
				
				/*if (mouseButton == 1 && selectionBox == null) {
					
					if (selectedMeasurement != null &&
						Search.getMeasurementAtPoint(mousePosOld) == selectedMeasurement)  move measurement  {
						Snap.initData(mousePos, getNewPointInit(), false, false);
						
						selectedMeasurement.toFront();			
						int[] spmOld = selectedMeasurement.getMovedPoint(toScreenCoords(selectedMeasurement.getStartPoint()));
						selectedMeasurement.addToMove(mousePosDelta);
						int[] spm = selectedMeasurement.getMovedPoint(toScreenCoords(selectedMeasurement.getStartPoint()));
						View.this.mouseMove(mousePosOld[0]+spm[0]-spmOld[0]-mousePos[0],
								mousePosOld[1]+spm[1]-spmOld[1]-mousePos[1]);
					}
					else {
						selectedMeasurement = null;
					}
				}

				if (mouseButton == 1 && selectionBox == null) {
					
					if (selectedLabel != null &&
						Search.getLabelAtPoint(mousePosOld) == selectedLabel)  move label  {
						Snap.initData(mousePos, getNewPointInit(), false, false);
						
						selectedLabel.toFront();
						selectedLabel.addToMove(mousePosDelta);
					}
					else {
						selectedLabel = null;
					}
				}
					
				if (mouseButton == 1 && (Sim2d.select == Sim2d.Select.NODES || Sim2d.select == Sim2d.Select.PARTS ||
						Sim2d.select == Sim2d.Select.PARTS_3D) &&
						selectedMeasurement == null && selectedLabel == null) {
					
					if (selectedNodes.size() == 1 && selectedNodes.get(0) == moveNode &&
							Sim2d.mode == Mode.PARTS && selectionBox == null &&
							!isControlKeyPressed)  move node  {
						Snap.initData(mousePos, getNewPointInit(), true, true);
						
						double[] coords = null;
						if (Snap.node != null) {
							coords = Snap.node.getCoords();
						}
						else {
							coords = Snap.coords;
						}
						double[] deltaCoords = new double[2];
						deltaCoords[0] = coords[0]-selectedNodes.get(0).getXCoord();
						deltaCoords[1] = coords[1]-selectedNodes.get(0).getYCoord();
						((NodeDialog) ((PartsDialog) Sim2d.dialogArea).getDialog()).updateDialog(deltaCoords);				
					}
					else if (!selectedParts3d.isEmpty() && selectedParts3d.contains(Snap.part3d) &&
							Sim2d.mode == Mode.PARTS && selectionBox == null &&
							!isControlKeyPressed)  move parts3d  {
						Snap.initData(mousePos, getNewPointInit(), false, false);
						
						double[] deltaCoords = new double[2];
						deltaCoords[0] = deltaCoords[1] = Double.MAX_VALUE;
						for (int s = 0; s < selectedParts3d.size(); s++) {
							for (int n = 0; n < selectedParts3d.get(s).getNrVertices(); n++) {
								Vertex3d vertex = selectedParts3d.get(s).getVertex(n);
								int[] mousePosNode = toScreenCoords(vertex.getCoords());
								mousePosNode[0] += mousePosDelta[0];
								mousePosNode[1] += mousePosDelta[1];
								
								if (mousePosNode[0] < 0 || mousePosNode[0] > getSize().x ||
									mousePosNode[1] < 0 || mousePosNode[1] > getSize().y) {
									continue;
								}
									
								Node node = Search.getNodeAtGridLine(fromScreenCoords(mousePosNode),
										Sim2d.SNAP_TOL/zoom);
								if (node != null) {
									double diff = node.getCoords()[0]-vertex.getCoords()[0];
									if (Math.abs(diff) < Math.abs(deltaCoords[0])) {
										deltaCoords[0] = diff;
									}
									diff = node.getCoords()[1]-vertex.getCoords()[1];
									if (Math.abs(diff) < Math.abs(deltaCoords[1])) {
										deltaCoords[1] = diff;
									}
								}
							}
						}
						
						if (deltaCoords[0] != Double.MAX_VALUE || deltaCoords[1] != Double.MAX_VALUE) {
							int[] move = new int[2];
							move[0] = (int) Math.round(deltaCoords[0]*zoom);
							move[1] = (int) Math.round(-deltaCoords[1]*zoom);
							View.this.mouseMove(move[0]-mousePosDelta[0], move[1]-mousePosDelta[1]);
						}
						else {
							deltaCoords[0] = mousePosDelta[0]/zoom;
							deltaCoords[1] = -mousePosDelta[1]/zoom;
						}
						((Part3dDialog) ((PartsDialog) Sim2d.dialogArea).getDialog()).updateDialog(deltaCoords);
					}
					else if (!selectedSets.isEmpty() && selectedSets.contains(Sim2d.model.getSetByElement(Snap.element)) &&
							Sim2d.mode == Mode.PARTS && selectionBox == null &&
							!isControlKeyPressed)  move elements  {
						Snap.initData(mousePos, getNewPointInit(), false, false);
						
						double[] deltaCoords = new double[2];
						deltaCoords[0] = deltaCoords[1] = Double.MAX_VALUE;
						for (int s = 0; s < selectedSets.size(); s++) {
							for (int n = 0; n < selectedSets.get(s).getNodes().size(); n++) {
								Node elementNode = selectedSets.get(s).getNodes().get(n);
								int[] mousePosNode = toScreenCoords(elementNode.getCoords());
								mousePosNode[0] += mousePosDelta[0];
								mousePosNode[1] += mousePosDelta[1];
								
								if (mousePosNode[0] < 0 || mousePosNode[0] > getSize().x ||
									mousePosNode[1] < 0 || mousePosNode[1] > getSize().y) {
									continue;
								}
									
								Node node = Search.getNodeAtGridLine(fromScreenCoords(mousePosNode),
										Sim2d.SNAP_TOL/zoom);
								if (node != null) {
									double diff = node.getCoords()[0]-elementNode.getCoords()[0];
									if (Math.abs(diff) < Math.abs(deltaCoords[0])) {
										deltaCoords[0] = diff;
									}
									diff = node.getCoords()[1]-elementNode.getCoords()[1];
									if (Math.abs(diff) < Math.abs(deltaCoords[1])) {
										deltaCoords[1] = diff;
									}
								}
							}
						}
						
						if (deltaCoords[0] != Double.MAX_VALUE || deltaCoords[1] != Double.MAX_VALUE) {
							int[] move = new int[2];
							move[0] = (int) Math.round(deltaCoords[0]*zoom);
							move[1] = (int) Math.round(-deltaCoords[1]*zoom);
							View.this.mouseMove(move[0]-mousePosDelta[0], move[1]-mousePosDelta[1]);
						}
						else {
							deltaCoords[0] = mousePosDelta[0]/zoom;
							deltaCoords[1] = -mousePosDelta[1]/zoom;
						}
						((PartDialog) ((PartsDialog) Sim2d.dialogArea).getDialog()).updateDialog(deltaCoords);
					}
					else if (Sim2d.mode == Mode.CONNECTORS && selectionBox == null &&
							((ConnectorsDialog) Sim2d.dialogArea).getSelection() == Snap.connector &&
							!isControlKeyPressed)  move connector  {
						Snap.initData(mousePos, getNewPointInit(), true, true);
						
						Connector connector = ((ConnectorsDialog) Sim2d.dialogArea).getSelection();
						connector.setCoordinates(Snap.coords);
						((ConnectorsDialog) Sim2d.dialogArea).updateDialog();
					}
					else  selection box  {
						
						Snap.initData(mousePos, getNewPointInit(), false, false);
						
						if (selectionBox == null) {
							if (!isControlKeyPressed) {
								deselectAll();
							}
							selectionBox = new Rectangle(mousePos[0], mousePos[1],
									mousePosDelta[0], mousePosDelta[1]);
						}
						else {
							selectionBox.width += mousePosDelta[0];
							selectionBox.height += mousePosDelta[1];
						}
						
						if (Sim2d.select == Sim2d.Select.NODES) {
							selectNodesInBox();
						}
						if (Sim2d.select == Sim2d.Select.PARTS) {
							selectSetsInBox();
						}
						if (Sim2d.select == Sim2d.Select.PARTS_3D) {
							selectParts3dInBox();
						}
					}
				}
				
				if (mouseButton == 2)  move view  {
					Snap.initData(mousePos, getNewPointInit(), false, false);
					setCursor(new Cursor(getDisplay(), SWT.CURSOR_SIZEALL));
					origin[0] += mousePosDelta[0];
					origin[1] += mousePosDelta[1];
				}
				
				if (mouseButton == 0 && (Sim2d.select == Sim2d.Select.NODES || Sim2d.select == Sim2d.Select.PARTS ||
					Sim2d.select == Sim2d.Select.PARTS_3D)) {
					Snap.initData(mousePos, getNewPointInit(), true, true);
					
					if (Sim2d.mode == Mode.PARTS && ((PartsDialog) Sim2d.dialogArea).getDialog() instanceof GeometricAreaDialog) {
						((GeometricAreaDialog) ((PartsDialog) Sim2d.dialogArea).getDialog()).updateDialog(
							new Node(Snap.coords[0], Snap.coords[1]));
					}
				}
				
				if (mouseButton == 0 && (Sim2d.select == Sim2d.Select.DISTANCE || Sim2d.select == Sim2d.Select.ANGLE)) {
					Snap.initData(mousePos, getNewPointInit(), true, true);
					
					measuring(false);
				}
				
				if (mouseButton == 0 && Sim2d.select == Sim2d.Select.LABEL) {
					Snap.initData(mousePos, getNewPointInit(), true, true);
				}*/
				
				if (!isMouseDragged) {
					setSideOfCoordinateSystem(mousePos[0], mousePos[1]);
				}
				if (animation == null) {
					Snap.snapAndSetText(mousePos, isMouseDragged, moveNode);
					redraw();
				}
			}
		});
		
		final Menu popup = new Menu(SimLive.shell, SWT.POP_UP);
        setMenu(popup);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				popup.setVisible(false);
				if (!isMouseDown) return;
				isMouseDown = false;
				mouseButton = 0;
				
				rotAroundAxis = false;
				rotNorm = null;
				moveNode = null;
				
				R0 = R0.times(R);
				R = Matrix.identity(3, 3);
				
				if (selectedLabel != null) {
					selectedLabel.finalizeMove();
				}
				
				if (setFocusPoint && e.button == 1 && !isMouseDragged) {
					if (Snap.node != null) {
						focusPoint = Label.getNewLabelForNode(Snap.node);
					}
					else if (Snap.element != null) {
						focusPoint = Label.getNewLabelForElement(Snap.element);
					}
					else if (Snap.part3d != null) {
						focusPoint = Label.getNewLabelForPart3d(mousePos, Snap.part3d, Search.getFacet3d());
					}
					if (focusPoint != null && focusPointOrientation != null) {
						focusPointOrientation = getFocusPointOrientation();
					}
					setFocusPoint = false;
				}
				else {
					if (isMouseDragged) {
						if (selectionBox != null) {
							if (SimLive.mode == Mode.PARTS) {
								SimLive.disposeDialogAreas();							
								if (selectedNodes.size() == 1) {
									SimLive.dialogArea = new NodeDialog(SimLive.compositeLeft, SWT.NONE, selectedNodes.get(0));
								}
								if (!selectedSets.isEmpty()) {
									SimLive.dialogArea = new PartDialog(SimLive.compositeLeft, SWT.NONE, selectedSets, SimLive.settings);
								}
								if (!selectedParts3d.isEmpty()) {
									SimLive.dialogArea = new Part3dDialog(SimLive.compositeLeft, SWT.NONE, selectedParts3d, SimLive.settings);
								}
							}
							else if (SimLive.mode == Mode.SUPPORTS) {
							
							}
							else if (SimLive.mode == Mode.LOADS) {
							
							}
							else if (SimLive.mode == Mode.CONNECTORS) {
							
							}
							else if (SimLive.mode == Mode.CONTACTS) {
							
							}
						}
						selectionBox = null;
						isMouseDragged = false;
					}
					else {
					
						if (e.button == 2)  /* middle click */ {
							fitToView();
						}
						
						if (e.button == 3)  /* right click */ {
							popupMenu(popup);
						}
						
						boolean labelAdded = false;
						
						if (e.button == 1) {
							/* orientation */
							if (side != Side.NONE) {
								R0 = getViewMatrix();
								R = Matrix.identity(3, 3);
							}
							else if (animation == null) {
								/* select label */
								if (selectedLabel != null) {
									selectedLabel.toFront();
								}
								
								else if (selectedMeasurement == null && SimLive.select == SimLive.Select.LABEL) {
									/* add label */
									if (labelAtMousePos != null && labelAtMousePos.getElement() != null) {
										labels.add(labelAtMousePos);
										labelAdded = true;
									}
								}
								
								if (selectedLabel == null) {
									/* select measurement */
									if (selectedMeasurement != null) {
										selectedMeasurement.toFront();
									}
									
									else if (SimLive.select == SimLive.Select.DISTANCE || SimLive.select == SimLive.Select.ANGLE) {
										/* start measuring */
										measuring(true);
									}
								}
							}
						}
						
						if (e.button == 1 && !labelAdded && SimLive.select != Select.DISTANCE && SimLive.select != Select.ANGLE &&
								selectedLabel == null && selectedMeasurement == null &&
								side == Side.NONE) {
							
							{
								if (selectedSets.isEmpty() && selectedParts3d.isEmpty() &&
									Snap.node != null) {
									if (!selectNodes()) {
										//deselectAllAndDisposeDialogs();
										//return;
									}
								}
								else if (selectedParts3d.isEmpty() && selectedNodes.isEmpty() &&
										Snap.element != null) {
									if (!selectSets()) {
										deselectAllAndDisposeDialogs();
										return;
									}
								}
								else if (selectedSets.isEmpty() && selectedNodes.isEmpty() &&
										Snap.part3d != null) {
									if (!selectParts3d()) {
										deselectAllAndDisposeDialogs();
										return;
									}
								}
							}
							
							if (!isControlKeyPressed) {
								if ((SimLive.mode != Mode.PARTS && Snap.node == null && !selectedNodes.isEmpty()) ||
										(Snap.element == null && !selectedSets.isEmpty()) ||
										(Snap.part3d == null && !selectedParts3d.isEmpty())) {
									deselectAllAndDisposeDialogs();
									return;
								}
							}
							
							if (SimLive.mode == Mode.PARTS) {
								
								if (SimLive.model.getNodes().contains(Snap.node) || !selectedSets.isEmpty() || !selectedParts3d.isEmpty()) {
									if (selectedNodes.size() == 1 && selectedNodes.get(0).getID() <= Model.maxUsedNodeID) {
										SimLive.disposeDialogAreas();							
										SimLive.dialogArea = new NodeDialog(SimLive.compositeLeft, SWT.NONE, selectedNodes.get(0));
									}
									if (!selectedSets.isEmpty()) {
										SimLive.disposeDialogAreas();							
										SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
											SWT.NONE, selectedSets, SimLive.settings);
									}
									if (!selectedParts3d.isEmpty()) {
										SimLive.disposeDialogAreas();							
										SimLive.dialogArea = new Part3dDialog(SimLive.compositeLeft,
											SWT.NONE, selectedParts3d, SimLive.settings);
									}
								}
								else {
									//double[] coords = screenToModelCoordinates(mousePos[0], mousePos[1]);
									Node newNode = new Node(Snap.coords2d[0], Snap.coords2d[1], 0);
									selectedSets.clear();
									selectedParts3d.clear();
									SimLive.model.getNodes().add(newNode);
									selectedNodes.add(newNode);
									
									if (selectedNodes.size() == 1) {
										SimLive.disposeDialogAreas();
				
										if (SimLive.settings.newPartType == Element.Type.CIRCULAR) {
											/* add new node as center of circle */
											SimLive.dialogArea = new CircularAreaDialog(SimLive.compositeLeft, SWT.NONE, newNode);
										}
										else if (SimLive.settings.newPartType == Element.Type.SPUR_GEAR) {
											/* add new node as center of spur gear */
											SimLive.dialogArea = new SpurGearDialog(SimLive.compositeLeft, SWT.NONE, newNode);
										}
										else if (SimLive.settings.newPartType == Element.Type.TRIANGULAR) {
											/* add new node as edge of triangle */
											SimLive.dialogArea = new TriangularAreaDialog(SimLive.compositeLeft, SWT.NONE, newNode);
										}
										else if (SimLive.settings.newPartType == Element.Type.RECTANGULAR) {
											/* add new node as edge of rectangle */
											SimLive.dialogArea = new RectangularAreaDialog(SimLive.compositeLeft, SWT.NONE, newNode);
										}
									}
								}
								
								if (SimLive.settings.newPartType == Element.Type.CIRCULAR &&
									selectedNodes.size() == 2) /* add new circular area */ {					
									if (selectedNodes.get(0).getID() > Model.maxUsedNodeID) {
										double[] center = selectedNodes.get(0).getCoords();
										double[] mpos = selectedNodes.get(1).getCoords();
										double radius = Math.sqrt((mpos[0]-center[0])*(mpos[0]-center[0])+
												(mpos[1]-center[1])*(mpos[1]-center[1]));
										SimLive.model.addCircleFromTrisAndQuads(center, radius);
										deselectAllAndDisposeDialogs();
									}
									else {
										deselectAllAndDisposeDialogs();
									}
								}
								
								if (SimLive.settings.newPartType == Element.Type.RECTANGULAR &&
									selectedNodes.size() == 2) /* add new rectangular area */ {					
									if (selectedNodes.get(0).getID() > Model.maxUsedNodeID) {
										double[] edge = selectedNodes.get(0).getCoords();
										double[] mpos = selectedNodes.get(1).getCoords();
										double width = mpos[0]-edge[0];
										double height = mpos[1]-edge[1];
										SimLive.model.addRectangleFromQuads(edge, width, height);
										deselectAllAndDisposeDialogs();
									}
									else {
										deselectAllAndDisposeDialogs();
									}
								}
								
								if (SimLive.settings.newPartType == Element.Type.TRIANGULAR &&
									 selectedNodes.size() == 2) /* add new triangular area */ {					
									if (selectedNodes.get(0).getID() > Model.maxUsedNodeID) {
										double[] edge = selectedNodes.get(0).getCoords();
										double[] mpos = selectedNodes.get(1).getCoords();
										double base = mpos[0]-edge[0];
										double height = mpos[1]-edge[1];
										double offset = ((TriangularAreaDialog) SimLive.dialogArea).getOffset();
										SimLive.model.addTriangleFromQuads(edge, base, height, offset);
										deselectAllAndDisposeDialogs();
									}
									else {
										deselectAllAndDisposeDialogs();
									}
								}
								
								if (SimLive.settings.newPartType == Element.Type.SPUR_GEAR &&
									 selectedNodes.size() == 2) /* add new spur gear */ {					
									if (selectedNodes.get(0).getID() > Model.maxUsedNodeID) {
										double[] center = selectedNodes.get(0).getCoords();
										double[] values = ((SpurGearDialog) SimLive.dialogArea).getValues();
										if (values[4] == 0.0) {
											SimLive.model.addExternalSpurGearFromTrisAndQuads(center,
													(int) values[0], values[1], values[2], values[3]);
										}
										else {
											SimLive.model.addInternalSpurGearFromQuads(center,
													(int) values[0], values[1], values[2], values[3], values[2]*2.0);
										}
										deselectAllAndDisposeDialogs();
									}
									else {
										deselectAllAndDisposeDialogs();
									}
								}
								
								if (SimLive.settings.newPartType == Element.Type.POINT_MASS) {
									if (selectedNodes.size() == 0 && Snap.node != null) /* add new element */ {
										
										int[] element_node = new int[]{Snap.node.getID()};
										
										Element newElement = null;
										if (SimLive.settings.newPartType == Element.Type.POINT_MASS) {
											newElement = new PointMass(element_node);
										}
										SimLive.model.getSets().add(new Set(newElement, Set.Type.BASIC));
										SimLive.model.getElements().add(newElement);
										SimLive.model.updateModel();
										deselectAll();
										SimLive.disposeDialogAreas();
									}
									else if (selectedNodes.size() > 1 && Snap.node == null) {
										deselectAll();
										SimLive.disposeDialogAreas();
									}
								}
								
								if (SimLive.settings.newPartType == Element.Type.ROD ||
									SimLive.settings.newPartType == Element.Type.SPRING ||
									SimLive.settings.newPartType == Element.Type.BEAM) {
									
									if (selectedNodes.size() == 2) /* add new element */ {					
										
										int[] element_node = new int[2];
										element_node[0] = selectedNodes.get(0).getID();
										element_node[1] = selectedNodes.get(1).getID();
										
										if (!SimLive.model.isElementValid(element_node)) {
											deselectAll();
											SimLive.disposeDialogAreas();
											if (Snap.node != null && selectNodes()) {
												SimLive.dialogArea = new NodeDialog(SimLive.compositeLeft, SWT.NONE, Snap.node);
											}
										}
										else {
											Element newElement = null;
											if (SimLive.settings.newPartType == Element.Type.ROD) {
												newElement = new Rod(element_node);
											}
											if (SimLive.settings.newPartType == Element.Type.SPRING) {
												newElement = new Spring(element_node);
											}
											if (SimLive.settings.newPartType == Element.Type.BEAM) {
												newElement = new Beam(element_node);
											}
											SimLive.model.getSets().add(new Set(newElement, Set.Type.BASIC));
											SimLive.model.getElements().add(newElement);
											SimLive.model.updateModel();
											deselectAll();
											SimLive.disposeDialogAreas();
										}
									}
								}
								
								if (SimLive.settings.newPartType == Element.Type.TRI) {
									if (selectedNodes.size() == 3) /* add new element */ {					
											
										int[] element_node = new int[3];
										element_node[0] = selectedNodes.get(0).getID();
										element_node[1] = selectedNodes.get(1).getID();
										element_node[2] = selectedNodes.get(2).getID();
										
										if (!SimLive.model.isElementValid(element_node)) {
											deselectAll();
											SimLive.disposeDialogAreas();
											if (Snap.node != null && selectNodes()) {
												SimLive.dialogArea = new NodeDialog(SimLive.compositeLeft, SWT.NONE, Snap.node);
											}
										}
										else {
											Element newElement = new Tri(element_node);
											SimLive.model.getSets().add(new Set(newElement, Set.Type.BASIC));
											SimLive.model.getElements().add(newElement);
											SimLive.model.updateModel();
											deselectAll();
											SimLive.disposeDialogAreas();
										}
									}
								}
									
								if (SimLive.settings.newPartType == Element.Type.QUAD) {
									if (selectedNodes.size() == 4) /* add new element */ {					
											
										int[] element_node = new int[4];
										element_node[0] = selectedNodes.get(0).getID();
										element_node[1] = selectedNodes.get(1).getID();
										element_node[2] = selectedNodes.get(2).getID();
										element_node[3] = selectedNodes.get(3).getID();
										
										if (!SimLive.model.isElementValid(element_node)) {
											deselectAll();
											SimLive.disposeDialogAreas();
											if (Snap.node != null && selectNodes()) {
												SimLive.dialogArea = new NodeDialog(SimLive.compositeLeft, SWT.NONE, Snap.node);
											}
										}
										else {
											Element newElement = new Quad(element_node);
											SimLive.model.getSets().add(new Set(newElement, Set.Type.BASIC));
											SimLive.model.getElements().add(newElement);
											SimLive.model.updateModel();
											deselectAll();
											SimLive.disposeDialogAreas();
										}
									}
								}
								

								if (selectedNodes.size() > 1) {
									SimLive.disposeDialogAreas();
								}
							}
								
							if (SimLive.mode == Mode.SUPPORTS) {					
								
							}
								
							if (SimLive.mode == Mode.LOADS) {					
								
							}
							
							if (SimLive.mode == Mode.CONNECTORS) {					
								
								ArrayList<Object> objects = SimLive.getModelTreeSelection();
								if (!objects.isEmpty()) {
									AbstractConnector connector = (AbstractConnector) objects.get(0);
									if (connector.getConnectorType() == ConnectorType.CONNECTOR) {
										if (!((Connector) connector).isCoordsSet() &&
											((Connector) connector).getSet0() != null && ((Connector) connector).getSet1() != null) {
											if (Snap.coords3d != null) {
												((Connector) connector).setCoordinates(Snap.coords3d, true);
											}
											else {
												((Connector) connector).setCoordinates(Snap.coords2d, true);
											}
											deselectAll();
											((ConnectorDialog) SimLive.dialogArea).coordsSet((Connector) connector);
										}
									}
								}
							}
							
							if (SimLive.mode == Mode.CONTACTS) {					
								
							}
						}
					}
				}
				
				SimLive.synchronizeModelTreeWithViewSelection();
				
				if (animation == null) {
					Snap.initData(mousePos, true, true);
					Snap.snapAndSetText(mousePos, isMouseDragged, moveNode);				
					redraw();
				}
			}
			@Override
			public void mouseDown(MouseEvent e) {
				isMouseDown = true;
				mouseButton = e.button;
				
				rotAroundAxis = side != Side.NONE;
				
				if (Snap.node != null && Snap.node.getID() <= Model.maxUsedNodeID) {
					moveNode = Snap.node;
				}
				movePoint = Snap.coords3d;
				
				mouseDown[0] = mousePos[0];
				mouseDown[1] = mousePos[1];
				
				if (!selectedNodes.isEmpty()) {
					SimLive.boxSelect = BoxSelect.NODES;
				}
				else if (!selectedParts3d.isEmpty()) {
					SimLive.boxSelect = BoxSelect.PARTS_3D;
				}
				else {
					SimLive.boxSelect = BoxSelect.PARTS;
				}
			}
		});
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent arg0) {
				/*fix required to open Sim2d in WindowBuilder editor*/
				if (SimLive.model == null) return;
				
				Rectangle rectangle = getClientArea();
                setCurrent();
                SimLive.glcontext.makeCurrent();
                render(SimLive.glcontext.getGL().getGL2(), rectangle.width, rectangle.height);
                swapBuffers();
                SimLive.glcontext.release();
		        
		        SimLive.diagramArea.redraw();
		        SimLive.diagramArea.update();
			}
		});
	}
	
	public void timeControlByKeys(KeyEvent e) {
		if (SimLive.mode == Mode.RESULTS) {
			int old = SimLive.post.getPostIncrementID();
			if (e.keyCode == SWT.ARROW_LEFT) {
				SimLive.post.setPostIncrementID(Math.max(old-1, 0));
			}
			if (e.keyCode == SWT.ARROW_RIGHT) {
				SimLive.post.setPostIncrementID(Math.min(old+1,
						SimLive.post.getSolution().getNumberOfIncrements()));
			}
			if (e.keyCode == SWT.ARROW_UP) {
				SimLive.post.setPostIncrementIDtoStartOfStep();
				if (SimLive.post.getPostIncrementID() == old) {
					SimLive.post.setPostIncrementID(Math.max(old-1, 0));
					SimLive.post.setPostIncrementIDtoStartOfStep();
				}
			}
			if (e.keyCode == SWT.ARROW_DOWN) {
				SimLive.post.setPostIncrementIDtoEndOfStep();
				if (SimLive.post.getPostIncrementID() == old) {
					SimLive.post.setPostIncrementID(Math.min(old+1,
							SimLive.post.getSolution().getNumberOfIncrements()));
					SimLive.post.setPostIncrementIDtoEndOfStep();
				}
			}
			if (e.keyCode == SWT.PAGE_UP) {
				SimLive.post.setPostIncrementID(0);
			}
			if (e.keyCode == SWT.PAGE_DOWN) {
				SimLive.post.setPostIncrementID(SimLive.post.getSolution().getNumberOfIncrements());
			}
			((ResultsDialog) SimLive.dialogArea).setSliderValue(SimLive.post.getPostIncrementID());
		}
	}
	
	private Matrix getViewMatrix() {
		Matrix coordAxis = null;
		switch (side) {
			case X: 		coordAxis = R0.getMatrix(0, 0, 0, 2).transpose();
							break;
			case MINUS_X: 	coordAxis = R0.getMatrix(0, 0, 0, 2).transpose().times(-1);
							break;
			case Y: 		coordAxis = R0.getMatrix(1, 1, 0, 2).transpose();
							break;
			case MINUS_Y: 	coordAxis = R0.getMatrix(1, 1, 0, 2).transpose().times(-1);
							break;
			case Z: 		coordAxis = R0.getMatrix(2, 2, 0, 2).transpose();
							break;
			case MINUS_Z: 	coordAxis = R0.getMatrix(2, 2, 0, 2).transpose().times(-1);
							break;
			default:
		}
		Matrix zDir = new Matrix(new double[]{0, 0, 1}, 3);
		Matrix v = coordAxis.crossProduct(zDir);
		double alpha = -Math.acos(coordAxis.dotProduct(zDir));
		Matrix RR = R0.times(GeomUtility.getRotationMatrix(alpha, v.getColumnPackedCopy()));
		double beta = 0;
		if (side == Side.X || side == Side.MINUS_X) {
			beta = Math.atan2(RR.get(1, 1), RR.get(1, 0));
		}
		else {
			beta = Math.atan2(RR.get(0, 1), RR.get(0, 0));
		}
		if (beta < 0.0) beta = 2.0*Math.PI + beta;
		beta = beta%(Math.PI/2.0);
		if (beta > Math.PI/4.0) beta -= Math.PI/2.0;
		return RR.times(GeomUtility.getRotationMatrixZ(beta));
	}
	
	private MenuItem getStoreMenuItem(Menu popup) {
		new MenuItem(popup, SWT.SEPARATOR);
		MenuItem store = new MenuItem(popup, SWT.NONE);
		store.setText("Store Selection");
		return store;
	}
	
	private void storeMenuItemSelected(int index, boolean ok) {
		((StoreDialog) SimLive.dialogArea).setOKLabel(index, ok);
		SimLive.view.deselectAll();
	}
	
	public void popupMenu(Menu popup) {
		popup.setVisible(true);
		MenuItem[] items = popup.getItems();
        for (int i = 0; i < items.length; i++) {
            items[i].dispose();
        }
        MenuItem deselectAll = new MenuItem(popup, SWT.NONE);
		deselectAll.setText("Deselect All");
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deselectAllAndDisposeDialogs();
			}
		});
		if ((!selectedSets.isEmpty() || !selectedParts3d.isEmpty()) && selectedLabel == null && selectedMeasurement == null) {
			new MenuItem(popup, SWT.SEPARATOR);
			MenuItem visibility = new MenuItem(popup, SWT.CASCADE);
			visibility.setText("Visibility");
			final Menu menu1 = new Menu(visibility);
			if (!selectedSets.isEmpty()) {
				String[] str = {"Default", "Hidden", "Pinned"};
	            for (int i = 0; i < str.length; i++) {
	            	final int j = i;
	            	MenuItem newItem1 = new MenuItem(menu1, SWT.CHECK);
		            newItem1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							for (int s = 0; s < selectedSets.size(); s++) {
								selectedSets.get(s).view = Set.View.values()[j];
							}
							deselectAllAndDisposeDialogs();
							SimLive.setDisplayPartsLabel();
						}
					});
		            for (int s = 0; s < selectedSets.size(); s++) {
						if (selectedSets.get(s).view == Set.View.values()[j]) {
							newItem1.setSelection(true);
						}
		            }
		            newItem1.setText(str[j]);
	            }
			}
        	if (!selectedParts3d.isEmpty()) {
        		String[] str = {"Fill", "Fill And Wireframe", "Wireframe"};
        		for (int i = 0; i < str.length; i++) {
        			final int j = i;
        			MenuItem newItem1 = new MenuItem(menu1, SWT.CHECK);
        			newItem1.setText(str[i]);
	        		newItem1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							for (int p = 0; p < selectedParts3d.size(); p++) {
								selectedParts3d.get(p).render = Part3d.Render.values()[j];
							}
							deselectAllAndDisposeDialogs();
						}
					});
	        		for (int p = 0; p < selectedParts3d.size(); p++) {
						if (selectedParts3d.get(p).render == Part3d.Render.values()[j]) {
							newItem1.setSelection(true);
						}
					}
        		}
        		new MenuItem(menu1, SWT.SEPARATOR);
        		String[] str1 = {"Single Sided", "Double Sided"};
        		for (int i = 0; i < 2; i++) {
        			final boolean doubleSided = i > 0;
        			MenuItem newItem1 = new MenuItem(menu1, SWT.CHECK);
        			newItem1.setText(str1[i]);
	        		for (int p = 0; p < selectedParts3d.size(); p++) {
						if (selectedParts3d.get(p).doubleSided == doubleSided) {
							newItem1.setSelection(true);
						}
					}
	        		newItem1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							for (int p = 0; p < selectedParts3d.size(); p++) {
								selectedParts3d.get(p).doubleSided = doubleSided;
							}
							deselectAllAndDisposeDialogs();
						}
					});
        		}
            }
            visibility.setMenu(menu1);
		}
        ArrayList<Object> objects = SimLive.getModelTreeSelection();
        if (objects.size() == 1 && selectedLabel == null && selectedMeasurement == null) {
			if (objects.get(0) instanceof Support && !selectedNodes.isEmpty()) {
				Support support = (Support) objects.get(0);
				getStoreMenuItem(popup).addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						support.setNodes(getSelectedNodes());
						storeMenuItemSelected(0, !support.getNodes().isEmpty());
					}
				});
			}
			if (objects.get(0) instanceof Load && !selectedNodes.isEmpty()) {
				Load load = (Load) objects.get(0);
				if (load.getType() == Load.Type.DISPLACEMENT && selectedNodes.size() == 1) {
					new MenuItem(popup, SWT.SEPARATOR);
					MenuItem setReference = new MenuItem(popup, SWT.NONE);
					setReference.setText("Reference Node");
					setReference.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							load.setReferenceNode(selectedNodes.get(0));
							storeMenuItemSelected(0, !load.getNodes().isEmpty());
						}
					});
				}
				getStoreMenuItem(popup).addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						load.setNodes(getSelectedNodes());
						storeMenuItemSelected(0, !load.getNodes().isEmpty());
					}
				});
			}
			if (objects.get(0) instanceof DistributedLoad && !selectedSets.isEmpty() &&
					SimLive.model.doSetsContainOnlyType(selectedSets, Element.Type.BEAM)) {
				DistributedLoad distributedLoad = (DistributedLoad) objects.get(0);
				getStoreMenuItem(popup).addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						ArrayList<Set> sets = getSelectedSets();
						ungroupSetsRecursive(sets);
						distributedLoad.unrefine();
						distributedLoad.setElementSets(sets);
						storeMenuItemSelected(0, !distributedLoad.getElementSets().isEmpty());
					}
				});
			}
			if (objects.get(0) instanceof Connector && selectedSets.size() == 2) {
				Connector connector = (Connector) objects.get(0);
				getStoreMenuItem(popup).addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						connector.setSets(selectedSets.get(0), selectedSets.get(1));
						connector.setCoordsSets(false);
						((ConnectorDialog) SimLive.dialogArea).coordsSet(connector);
						storeMenuItemSelected(0, connector.getSet0() != null && connector.getSet1() != null);
					}
				});
			}
			if (objects.get(0) instanceof Connector3d &&
					(!selectedSets.isEmpty() || !selectedParts3d.isEmpty())) {
				Connector3d connector3d = (Connector3d) objects.get(0);
				getStoreMenuItem(popup).addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!selectedSets.isEmpty()) connector3d.setParts(getSelectedSets());
						if (!selectedParts3d.isEmpty()) connector3d.setParts3d(getSelectedParts3d());
						storeMenuItemSelected(0, !connector3d.getParts3d().isEmpty());
						storeMenuItemSelected(1, !connector3d.getParts().isEmpty());
					}
				});
			}
			if (objects.get(0) instanceof ContactPair &&
					(!selectedNodes.isEmpty() || (!selectedSets.isEmpty() &&
					SimLive.model.doSetsContainOnlyPlaneElements(selectedSets)))) {
				ContactPair contactPair = (ContactPair) objects.get(0);
				getStoreMenuItem(popup).addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!selectedNodes.isEmpty()) contactPair.setSlave(getSelectedNodes());
						if (!selectedSets.isEmpty()) {
							contactPair.setMaster(getSelectedSets());
						}
						storeMenuItemSelected(0, !contactPair.getSlaveNodes().isEmpty());
						storeMenuItemSelected(1, !contactPair.getMasterSets().isEmpty());
					}
				});
			}
		}
		if (SimLive.mode == Mode.PARTS) {
        	new MenuItem(popup, SWT.SEPARATOR);
    		MenuItem createNewParts = new MenuItem(popup, SWT.CASCADE);
            createNewParts.setText("Create New Parts");
            final Menu menu1 = new Menu(createNewParts);
            String[] str = {"Rod", "Spring", "Beam", "Tri", "Quad", "Rectangular Area", "Circular Area",
    				"Triangular Area", "Spur Gear", "Point Mass"};
            for (int i = 0; i < str.length; i++) {
            	final int j = i;
            	MenuItem newItem1 = new MenuItem(menu1, SWT.RADIO);
	            newItem1.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						SimLive.settings.newPartType = Element.Type.values()[j];
						if (selectedNodes.size() == 1 && selectedNodes.get(0).getID() > Model.maxUsedNodeID) {
							SimLive.disposeDialogAreas();
							if (SimLive.settings.newPartType == Element.Type.CIRCULAR) {
								SimLive.dialogArea = new CircularAreaDialog(SimLive.compositeLeft, SWT.NONE, selectedNodes.get(0));
							}
							else if (SimLive.settings.newPartType == Element.Type.SPUR_GEAR) {
								SimLive.dialogArea = new SpurGearDialog(SimLive.compositeLeft, SWT.NONE, selectedNodes.get(0));
							}
							else if (SimLive.settings.newPartType == Element.Type.TRIANGULAR) {
								SimLive.dialogArea = new TriangularAreaDialog(SimLive.compositeLeft, SWT.NONE, selectedNodes.get(0));
							}
							else if (SimLive.settings.newPartType == Element.Type.RECTANGULAR) {
								SimLive.dialogArea = new RectangularAreaDialog(SimLive.compositeLeft, SWT.NONE, selectedNodes.get(0));
							}
						}
					}
				});
	            newItem1.setSelection(SimLive.settings.newPartType == Element.Type.values()[j]);
	            newItem1.setText(str[j]);				            
            }
            createNewParts.setMenu(menu1);
            MenuItem importPart3d = new MenuItem(popup, SWT.NONE);
    		importPart3d.setText("Import 3D-Part...");
    		importPart3d.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					SimLive.importPart3d();
				}
			});
    		MenuItem lockPart3dSelection = new MenuItem(popup, SWT.CHECK);
    		lockPart3dSelection.setText("Lock 3D-Part Selection");
    		lockPart3dSelection.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					lockSelectParts3d = !lockSelectParts3d;
					if (!selectedParts3d.isEmpty()) {
						deselectAllAndDisposeDialogs();
					}
				}
			});
    		lockPart3dSelection.setSelection(lockSelectParts3d);
    		lockPart3dSelection.setEnabled(!SimLive.model.getParts3d().isEmpty());
    		
    		if ((!selectedSets.isEmpty() || !selectedParts3d.isEmpty()) && selectedLabel == null && selectedMeasurement == null) {
    			if (!selectedSets.isEmpty()) {
        			ArrayList<Element> elementSet = new ArrayList<Element>();
        			for (int s = 0; s < selectedSets.size(); s++) {
        				elementSet.addAll(selectedSets.get(s).getElements());
        			}
        			if (!SimLive.model.doElementsContainType(elementSet, Element.Type.SPRING) &&
        				!SimLive.model.doElementsContainType(elementSet, Element.Type.POINT_MASS)) {
    	    			new MenuItem(popup, SWT.SEPARATOR);
    	        		MenuItem material = new MenuItem(popup, SWT.CASCADE);
    	        		material.setText("Material");
    	                final Menu menu2 = new Menu(material);
    	                for (int i = 0; i < SimLive.model.getMaterials().size(); i++) {
    	                	final Material mat = SimLive.model.getMaterials().get(i);
    	                	MenuItem newItem1 = new MenuItem(menu2, SWT.CHECK);
    	                	newItem1.addSelectionListener(new SelectionAdapter() {
    							@Override
    							public void widgetSelected(SelectionEvent e) {
    								for (int e1 = 0; e1 < elementSet.size(); e1++) {
    									elementSet.get(e1).setMaterial(mat);
    								}
    								((PartDialog) SimLive.dialogArea).updateDialog(new double[3]);
    							}
    						});
    	                	for (int e1 = 0; e1 < elementSet.size(); e1++) {
    	                		if (elementSet.get(e1).getMaterial() == mat) {
    								newItem1.setSelection(true);
    							}
    	                	}
    			            newItem1.setText(mat.name);
    	                }
    	                material.setMenu(menu2);
    	                
        				if (!SimLive.model.doElementsContainType(elementSet, Element.Type.QUAD) &&
        					!SimLive.model.doElementsContainType(elementSet, Element.Type.TRI)) {
    		        		MenuItem section = new MenuItem(popup, SWT.CASCADE);
    		        		section.setText("Section");
    		                final Menu menu3 = new Menu(section);
    		                for (int i = 0; i < SimLive.model.getSections().size(); i++) {
    		                	final Section sec = SimLive.model.getSections().get(i);
    		                	MenuItem newItem1 = new MenuItem(menu3, SWT.CHECK);
    		                	newItem1.addSelectionListener(new SelectionAdapter() {
    								@Override
    								public void widgetSelected(SelectionEvent e) {
    									for (int e1 = 0; e1 < elementSet.size(); e1++) {
    										((LineElement) elementSet.get(e1)).setSection(sec);
    									}
    									((PartDialog) SimLive.dialogArea).updateDialog(new double[3]);
    								}
    							});
    		                	for (int e1 = 0; e1 < elementSet.size(); e1++) {
    		                		if (((LineElement) elementSet.get(e1)).getSection() == sec) {
    									newItem1.setSelection(true);
    								}
    		                	}
    				            newItem1.setText(sec.getName());
    		                }
    		                section.setMenu(menu3);
    	    			}
        			}
        		}
    			new MenuItem(popup, SWT.SEPARATOR);
        		MenuItem copy = new MenuItem(popup, SWT.NONE);
        		copy.setText("Copy");
        		copy.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!selectedSets.isEmpty()) copySelectedSets();
						if (!selectedParts3d.isEmpty()) copySelectedParts3d();
					}
				});
        		MenuItem flip = new MenuItem(popup, SWT.NONE);
        		flip.setText("Flip");
        		flip.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!selectedSets.isEmpty()) flipSelectedSets();
						if (!selectedParts3d.isEmpty()) flipSelectedParts3d();
					}
				});
        		MenuItem group = new MenuItem(popup, SWT.NONE);
        		group.setText("Group");
        		group.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!selectedSets.isEmpty()) groupSelectedSets();
						if (!selectedParts3d.isEmpty()) groupSelectedParts3d();
					}
				});
        		group.setEnabled(selectedSets.size() > 1 || selectedParts3d.size() > 1);
        		MenuItem ungroup = new MenuItem(popup, SWT.NONE);
        		ungroup.setText("Ungroup");
        		ungroup.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!selectedSets.isEmpty()) ungroupSelectedSets();
						if (!selectedParts3d.isEmpty()) ungroupSelectedParts3d();
					}
				});
        		ungroup.setEnabled((!selectedSets.isEmpty() &&
        				!((PartDialog) SimLive.dialogArea).allSetsAreOfTypeBasic(selectedSets)) ||
        				(!selectedParts3d.isEmpty() &&
		        		((Part3dDialog) SimLive.dialogArea).atLeastOnePart3dIsUngroupable(selectedParts3d)));
        		if (!selectedSets.isEmpty()) {
	        		new MenuItem(popup, SWT.SEPARATOR);
	        		MenuItem merge = new MenuItem(popup, SWT.NONE);
	        		merge.setText("Merge");
	        		merge.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							mergeSelectedSets();
						}
					});
	        		merge.setEnabled(((PartDialog) SimLive.dialogArea).isMergeable());
	        		MenuItem split = new MenuItem(popup, SWT.NONE);
	        		split.setText("Split");
	        		split.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							splitSelectedSets();
						}
					});
	        		split.setEnabled(((PartDialog) SimLive.dialogArea).isSplittable(selectedSets));
	        		MenuItem refine = new MenuItem(popup, SWT.NONE);
	        		refine.setText("Refine");
	        		refine.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							refineSelectedSets(1);
						}
					});
	        		MenuItem relax = new MenuItem(popup, SWT.NONE);
	        		relax.setText("Relax");
	        		relax.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							relaxSelectedSets();
						}
					});
	        		relax.setEnabled(((PartDialog) SimLive.dialogArea).isRelaxable(selectedSets));				        		
        		}
        		new MenuItem(popup, SWT.SEPARATOR);
        		MenuItem delete = new MenuItem(popup, SWT.NONE);
        		delete.setText("Delete");
        		delete.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (!selectedSets.isEmpty()) deleteSelectedSets();
						if (!selectedParts3d.isEmpty()) deleteSelectedParts3d();
					}
				});
    		}
		}
		if (selectedMeasurement != null || (selectedLabel != null && (SimLive.post == null ||
				(selectedLabel != SimLive.post.getMinLabel() && selectedLabel != SimLive.post.getMaxLabel())))) {
			new MenuItem(popup, SWT.SEPARATOR);
    		MenuItem delete = new MenuItem(popup, SWT.NONE);
    		if (selectedMeasurement != null) delete.setText("Delete Measurement");
    		if (selectedLabel != null) delete.setText("Delete Label");
    		delete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (selectedMeasurement != null) deleteSelectedMeasurement();
					if (selectedLabel != null) deleteSelectedLabel();
				}
			});
		}
		items = popup.getItems();
        for (int i = 0; i < items.length; i++) {
            items[i].addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					SimLive.model.updateModel();
					SimLive.synchronizeModelTreeWithViewSelection();
				}
            });
        }
	}
	
	private Matrix rotateAroundAxis(int xMove, int yMove) {
		if (rotNorm == null) {
			int[] viewport = View.getViewport();
			final double dist = 1.2*SimLive.COORDINATE_SYSTEM_SIZE;
			double[] center = new double[]{dist/2, viewport[3]-dist/2};
			double[] modelCoords = screenToModelCoordinates(center[0], center[1]);
			if (side == Side.X || side == Side.MINUS_X) {
				modelCoords[0] += SimLive.settings.meshSize;
			}
			if (side == Side.Y || side == Side.MINUS_Y) {
				modelCoords[1] += SimLive.settings.meshSize;
			}
			if (side == Side.Z || side == Side.MINUS_Z) {
				modelCoords[2] += SimLive.settings.meshSize;
			}
			double[] screenAxis = modelToScreenCoordinates(modelCoords);
			screenAxis[0] -= center[0];
			screenAxis[1] -= center[1];
			rotNorm = new double[2];
			rotNorm[0] = screenAxis[1];
			rotNorm[1] = screenAxis[0];
			double length = Math.sqrt(rotNorm[0]*rotNorm[0] + rotNorm[1]*rotNorm[1]);
			if (length < 1e-3) {
				rotNorm[0] = 0.0;
				rotNorm[1] = -1.0;
			}
			else {
				rotNorm[0] /= length;
				rotNorm[1] /= length;
			}
		}
		
		double scal = rotNorm[0]*xMove+rotNorm[1]*yMove;
		
		if (side == Side.X || side == Side.MINUS_X) {
			return GeomUtility.getRotationMatrix(scal*1e-2, new double[]{R0.get(0, 0), R0.get(0, 1), R0.get(0, 2)});
		}
		if (side == Side.Y || side == Side.MINUS_Y) {
			return GeomUtility.getRotationMatrix(scal*1e-2, new double[]{R0.get(1, 0), R0.get(1, 1), R0.get(1, 2)});
		}
		if (side == Side.Z || side == Side.MINUS_Z) {
			return GeomUtility.getRotationMatrix(scal*1e-2, new double[]{R0.get(2, 0), R0.get(2, 1), R0.get(2, 2)});
		}
		return null;
	}
	
	public void zoomIn() {
		if (perspective) {
			double dist = Math.sqrt((cameraRefPos[0]-rotPoint[0])*(cameraRefPos[0]-rotPoint[0])+
    				(cameraRefPos[1]-rotPoint[1])*(cameraRefPos[1]-rotPoint[1])+
    				(cameraRefPos[2]-rotPoint[2])*(cameraRefPos[2]-rotPoint[2]));
			double nearClip = SimLive.settings.meshSize/100.0;
			Matrix dir = R0.getMatrix(0, 2, 2, 2);
			if (dist - (SimLive.ZOOM_FACTOR - 1.0)*dist > nearClip) {
				cameraRefPos[0] = cameraRefPos[0] - (SimLive.ZOOM_FACTOR - 1.0)*dir.get(0, 0)*dist;
				cameraRefPos[1] = cameraRefPos[1] - (SimLive.ZOOM_FACTOR - 1.0)*dir.get(1, 0)*dist;
				cameraRefPos[2] = cameraRefPos[2] - (SimLive.ZOOM_FACTOR - 1.0)*dir.get(2, 0)*dist;
			}
			else {
				//stop camera at nearClip
				cameraRefPos[0] = rotPoint[0] + dir.get(0, 0)*nearClip;
				cameraRefPos[1] = rotPoint[1] + dir.get(1, 0)*nearClip;
				cameraRefPos[2] = rotPoint[2] + dir.get(2, 0)*nearClip;
			}
		}
		else {
			zoom *= SimLive.ZOOM_FACTOR;
		}
	}
	
	public void zoomOut() {
		if (perspective) {
			double dist = Math.sqrt((cameraRefPos[0]-rotPoint[0])*(cameraRefPos[0]-rotPoint[0])+
    				(cameraRefPos[1]-rotPoint[1])*(cameraRefPos[1]-rotPoint[1])+
    				(cameraRefPos[2]-rotPoint[2])*(cameraRefPos[2]-rotPoint[2]));
			Matrix dir = R0.getMatrix(0, 2, 2, 2);
			cameraRefPos[0] = cameraRefPos[0] + (SimLive.ZOOM_FACTOR - 1.0)*dir.get(0, 0)*dist;
			cameraRefPos[1] = cameraRefPos[1] + (SimLive.ZOOM_FACTOR - 1.0)*dir.get(1, 0)*dist;
			cameraRefPos[2] = cameraRefPos[2] + (SimLive.ZOOM_FACTOR - 1.0)*dir.get(2, 0)*dist;
		}
		else {
			zoom /= SimLive.ZOOM_FACTOR;
		}
	}
	
	private void getXYScalingAndCenter(double[] xyScaling, double[] center) {
		ArrayList<Node> nodes = SimLive.model.getNodes();
		ArrayList<Part3d> parts3d = SimLive.model.getParts3d();
		ArrayList<ContactPair> contactPairs = SimLive.model.getContactPairs();
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE,
			   minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE,
			   minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (int n = 0; n < nodes.size(); n++) {
			double[] coords = getCoordsWithScaledDisp(n);
			if (coords[0] < minX) minX = coords[0];
			if (coords[1] < minY) minY = coords[1];
			if (coords[2] < minZ) minZ = coords[2];
			if (coords[0] > maxX) maxX = coords[0];
			if (coords[1] > maxY) maxY = coords[1];
			if (coords[2] > maxZ) maxZ = coords[2];
		}
		for (int obj = 0; obj < parts3d.size(); obj++) {
			Part3d part3d = parts3d.get(obj);
			double[][] vertexCoords = part3d.getVertexCoords();
			for (int v = 0; v < part3d.getNrVertices(); v++) {
				if (vertexCoords[v][0] < minX) minX = vertexCoords[v][0];
				if (vertexCoords[v][1] < minY) minY = vertexCoords[v][1];
				if (vertexCoords[v][2] < minZ) minZ = vertexCoords[v][2];
				if (vertexCoords[v][0] > maxX) maxX = vertexCoords[v][0];
				if (vertexCoords[v][1] > maxY) maxY = vertexCoords[v][1];
				if (vertexCoords[v][2] > maxZ) maxZ = vertexCoords[v][2];
			}
		}
		for (int c = 0; c < contactPairs.size(); c++) {
			if (contactPairs.get(c).getType() == Type.RIGID_DEFORMABLE) {
				for (int n = 0; n < contactPairs.get(c).getRigidNodes().size(); n++) {
					double[] coords = contactPairs.get(c).getRigidNodes().get(n).getCoords();
					if (coords[0] < minX) minX = coords[0];
					if (coords[1] < minY) minY = coords[1];
					if (coords[2] < minZ) minZ = coords[2];
					if (coords[0] > maxX) maxX = coords[0];
					if (coords[1] > maxY) maxY = coords[1];
					if (coords[2] > maxZ) maxZ = coords[2];
				}
			}
		}
		center[0] = minX+(maxX-minX)/2;
		center[1] = minY+(maxY-minY)/2;
		center[2] = minZ+(maxZ-minZ)/2;
		if (!Double.isFinite(center[0])) center[0] = 0;
		if (!Double.isFinite(center[1])) center[1] = 0;
		if (!Double.isFinite(center[2])) center[2] = 0;
		double[][] bBox = new double[8][];
		bBox[0] = new double[]{minX, minY, minZ};
		bBox[1] = new double[]{minX, minY, maxZ};
		bBox[2] = new double[]{minX, maxY, maxZ};
		bBox[3] = new double[]{minX, maxY, minZ};
		bBox[4] = new double[]{maxX, minY, minZ};
		bBox[5] = new double[]{maxX, minY, maxZ};
		bBox[6] = new double[]{maxX, maxY, maxZ};
		bBox[7] = new double[]{maxX, maxY, minZ};
		
		double min_x = Double.MAX_VALUE, max_x = -Double.MAX_VALUE,
			   min_y = Double.MAX_VALUE, max_y = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double[] screen = modelToScreenCoordinates(bBox[i]);
			if (screen[0] < min_x) min_x = screen[0];
			if (screen[1] < min_y) min_y = screen[1];
			if (screen[0] > max_x) max_x = screen[0];
			if (screen[1] > max_y) max_y = screen[1];
		}
		/* fit to canvas size */
		if (focusPoint != null) {
			double[] coords = focusPoint.getCoordinatesWithDeformation();
			double[] point = modelToScreenCoordinates(coords);
			xyScaling[0] = getSize().x*SimLive.FIT_TO_VIEW/(2.0*Math.max(max_x-point[0], point[0]-min_x));
			xyScaling[1] = getSize().y*SimLive.FIT_TO_VIEW/(2.0*Math.max(max_y-point[1], point[1]-min_y));
		}
		else {
			xyScaling[0] = getSize().x*SimLive.FIT_TO_VIEW/(max_x-min_x);
			xyScaling[1] = getSize().y*SimLive.FIT_TO_VIEW/(max_y-min_y);
		}
	}
	
	public void fitToView() {
		if (perspective) {
			int[] viewport = getViewport();
			float aspectRatio = (float) viewport[3] / (float) viewport[2];
			GL2 gl2 = SimLive.glcontext.getGL().getGL2();
			gl2.glMatrixMode(GL2.GL_PROJECTION);
			gl2.glLoadIdentity();
			double farClip = SimLive.settings.meshSize*100.0;
			gl2.glOrtho(-1.0/zoom, 1.0/zoom, -aspectRatio/zoom, aspectRatio/zoom, -farClip, farClip); //TODO
			gl2.glMatrixMode(GL2.GL_MODELVIEW);
			fitToViewOrtho();
			initPerspective();
		}
		else {
			fitToViewOrtho();
		}
		zoom0 = zoom;
	}
	
	private void fitToViewOrtho() {
		double[] xyScaling = new double[2];
		double[] center = new double[3];
		getXYScalingAndCenter(xyScaling, center);
		if (focusPoint != null) {
			center = focusPoint.getCoordinatesWithDeformation();
		}
		double deltaZoom = Math.min(xyScaling[0], xyScaling[1]);
		if (Double.isFinite(deltaZoom) && deltaZoom > SimLive.ZERO_TOL) {
			zoom *= deltaZoom;
			
			cameraRefPos = center.clone();
			rotPoint = center.clone();
		}
	}
	
	public void initializeZoomAndOrigin(double gridSize) {
		zoom *= 2.0 / (5.0*gridSize*zoom);
		zoom0 = zoom;
		cameraRefPos = new double[]{0, 0, 0};
	}
	
	public void adaptZoomToNewGridSize(double gridSizeOld, double gridSizeNew) {
		zoom *= gridSizeOld/gridSizeNew;
	}
	
	public void convertUnitsOfViewData(double lengthFactor) {
		zoom /= lengthFactor;
		zoom0 /= lengthFactor;
		cameraRefPos[0] *= lengthFactor;
		cameraRefPos[1] *= lengthFactor;
		cameraRefPos[2] *= lengthFactor;
		rotPoint[0] *= lengthFactor;
		rotPoint[1] *= lengthFactor;
		rotPoint[2] *= lengthFactor;
	}
	
	public void measuring(boolean mouseUp) {
		Measurement measurement = null;
		if (!measurements.isEmpty() &&
				!measurements.get(measurements.size()-1).isFinalized()) {
			measurement = measurements.get(measurements.size()-1);
		}
		
		Snap.initData(mousePos, true, true);
		Snap.snapAndSetText(mousePos, isMouseDragged, moveNode);
		double[] coords = Snap.coords3d != null ? Snap.coords3d : Snap.coords2d;
	
		if (measurement == null) {
			if (mouseUp) {
				Measurement.Type type = null;
				if (SimLive.select == SimLive.Select.DISTANCE) {
					type = Measurement.Type.DISTANCE;
				}
				if (SimLive.select == SimLive.Select.ANGLE) {
					type = Measurement.Type.ANGLE;
				}
				measurement = new Measurement(type, coords);
				measurements.add(measurement);
			}
		}
		else if (!measurement.isFinalized()) {
			if (SimLive.select == SimLive.Select.DISTANCE) {
				measurement.setEndPoint(coords.clone(), isControlKeyPressed);
				measurement.setFinalized(mouseUp);
			}
			if (SimLive.select == SimLive.Select.ANGLE) {
				measurement.setFinalized(measurement.getEndPoint() != null && mouseUp);
				if (measurement.getEndPoint() != null || mouseUp) {
					measurement.setEndPoint(coords.clone(), false);
				}
				else {
					measurement.setMidPoint(coords.clone());
				}
			}
		}
	}
	
	public void removeUnfinalizedMeasurement() {
		if (!measurements.isEmpty()) {
			if (!measurements.get(measurements.size()-1).isFinalized()) {
				measurements.remove(measurements.size()-1);
			}
			redraw();
		}
	}
	
	public boolean doSetsOnlyContain(ArrayList<Set> sets, Set.View view) {
		for (int s = 0; s < sets.size(); s++) {
			if (sets.get(s).view != view) {
				return false;
			}
		}
		return true;
	}
	
	private boolean selectParts3d() {
		Part3d part3d = Snap.part3d;
		if (part3d != null) {
			selectedNodes.clear();
			selectedSets.clear();
			if (isControlKeyPressed) {
				if (selectedParts3d.contains(part3d)) {
					selectedParts3d.remove(part3d);
				}
				else {
					selectedParts3d.add(part3d);
				}
			}
			else {
				if (selectedParts3d.size() == 1 && selectedParts3d.contains(part3d)) {
					selectedParts3d.clear();
				}
				else {
					selectedParts3d.clear();
					selectedParts3d.add(part3d);
				}
			}
		}
		return !selectedParts3d.isEmpty();
	}
	
	private boolean selectSets() {
		Set set = Snap.set;
		if (set != null) {
			selectedNodes.clear();
			selectedParts3d.clear();
			if (isControlKeyPressed) {
				if (selectedSets.contains(set)) {
					selectedSets.remove(set);
				}
				else {
					selectedSets.add(set);
				}
			}
			else {
				if (selectedSets.size() == 1 && selectedSets.contains(set)) {
					selectedSets.clear();
				}
				else {
					selectedSets.clear();
					selectedSets.add(set);
				}
			}
		}
		return !selectedSets.isEmpty();
	}
	
	private boolean selectNodes() {
		if (Snap.node != null) {
			selectedSets.clear();
			selectedParts3d.clear();
			if (isControlKeyPressed || (SimLive.mode == Mode.PARTS &&
					(SimLive.settings.newPartType == Element.Type.ROD ||
					 SimLive.settings.newPartType == Element.Type.SPRING ||
					 SimLive.settings.newPartType == Element.Type.BEAM ||
					 SimLive.settings.newPartType == Element.Type.TRI ||
					 SimLive.settings.newPartType == Element.Type.QUAD ||
					 SimLive.dialogArea instanceof GeometricAreaDialog))) {
				if (selectedNodes.contains(Snap.node)) {
					selectedNodes.remove(Snap.node);
				}
				else {
					selectedNodes.add(Snap.node);
				}
			}
			else {
				if (selectedNodes.size() == 1 && selectedNodes.contains(Snap.node)) {
					selectedNodes.clear();
				}
				else {
					selectedNodes.clear();
					selectedNodes.add(Snap.node);
				}
			}
		}
		return !selectedNodes.isEmpty();
	}
	
	private void selectParts3dInBox() {
		ArrayList<Part3d> parts3d = SimLive.model.getParts3d();
		double[][] bCoords = new double[4][2];
		bCoords[0][0] = selectionBox.x;
		bCoords[0][1] = selectionBox.y;
		bCoords[1][0] = selectionBox.x+selectionBox.width;
		bCoords[1][1] = selectionBox.y;
		bCoords[2][0] = selectionBox.x+selectionBox.width;
		bCoords[2][1] = selectionBox.y+selectionBox.height;
		bCoords[3][0] = selectionBox.x;
		bCoords[3][1] = selectionBox.y+selectionBox.height;
		for (int s = 0; s < parts3d.size(); s++) {
			Part3d part3d = parts3d.get(s);
			if (!selectedParts3d.contains(part3d)) {
				Stream<Facet3d> stream = Arrays.stream(part3d.getFacets()).parallel();
				if (stream.anyMatch(facet -> isPart3dInBox(part3d, facet, bCoords))) {
					selectedParts3d.add(part3d);
				}
			}
		}
	}
	
	private boolean isPart3dInBox(Part3d part3d, Facet3d facet, double[][] boxCoords) {
		double[][] p = new double[3][3];
		for (int i = 0; i < 3; i++) {
			p[i] = pVertices[part3d.getID()][facet.getIndices()[i]];
		}
		for (int i = 0; i < 3; i++) {
			if (selectionBoxContains(boxCoords, p[i])) {
				return true;
			}
		}
		if (GeomUtility.isPointInTriangle(p[0], p[1], p[2], boxCoords[2], null)) {
			return true;
		}
		else {
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 3; j++) {
					if (GeomUtility.doLineSegmentsIntersect(
							boxCoords[i], boxCoords[(i+1)%4],
							p[j], p[(j+1)%3])) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static boolean isPointInElementBBox(int elementID, double pointX, double pointY) {
		double maxX = -Double.MAX_VALUE;
		double minX = Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		try {
			for (int p = 0; p < pElementScreenPolys[elementID].length; p++) {
				if (pElementScreenPolys[elementID][p][0] > maxX) maxX = pElementScreenPolys[elementID][p][0];
				if (pElementScreenPolys[elementID][p][0] < minX) minX = pElementScreenPolys[elementID][p][0];
				if (pElementScreenPolys[elementID][p][1] > maxY) maxY = pElementScreenPolys[elementID][p][1];
				if (pElementScreenPolys[elementID][p][1] < minY) minY = pElementScreenPolys[elementID][p][1];
			}
		}
		catch (Exception e) {
		}
		return pointX < maxX && pointX > minX && pointY < maxY && pointY > minY;
	}
	
	private void selectSetsInBox() {
		ArrayList<Set> sets = SimLive.model.getSets();
		double[][] bCoords = new double[4][2];
		bCoords[0][0] = selectionBox.x;
		bCoords[0][1] = selectionBox.y;
		bCoords[1][0] = selectionBox.x+selectionBox.width;
		bCoords[1][1] = selectionBox.y;
		bCoords[2][0] = selectionBox.x+selectionBox.width;
		bCoords[2][1] = selectionBox.y+selectionBox.height;
		bCoords[3][0] = selectionBox.x;
		bCoords[3][1] = selectionBox.y+selectionBox.height;
		Stream<Set> stream = sets.stream();
		stream.forEach(set -> {
			if (isSetInBox(set, bCoords)) {
				selectedSets.add(set);
				selectedNodes.clear();
			}
		});
	}
	
	private boolean isSetInBox(Set set, double[][] bCoords) {
		if (!selectedSets.contains(set)) {
			for (int elem = 0; elem < set.getElements().size(); elem++) {
				Element element = set.getElements().get(elem);
				int[] elemNodes = element.getElementNodes();
				if (element.getType() == Element.Type.POINT_MASS) {
					Node node = SimLive.model.getNodes().get(elemNodes[0]);
					double[] coords = getCoordsWithScaledDisp(node.getID());
					double factor = getSizeFactorPerspective(coords);
					double[] p = modelToScreenCoordinates(coords);
					if (isCircleInBox(bCoords, p, SimLive.POINT_MASS_RADIUS/factor)) {
						return true;
					}
				}
				else {
					double[][] p = pElementScreenPolys[element.getID()];
					if (p == null) return false;
					for (int n = 0; n < p.length; n++) {
						if (selectionBoxContains(bCoords, p[n])) {
							return true;
						}
					}
					for (int i = 0; i < 4; i++) {
						for (int j = 0; j < p.length-1; j++) {
							if (GeomUtility.doLineSegmentsIntersect(
									bCoords[i], bCoords[(i+1)%4],
									p[j], p[j+1])) {
								return true;
							}
						}
					}
					if (isPointInElementBBox(element.getID(), bCoords[2][0], bCoords[2][1])) {
						double[] modelCoords = View.screenToModelCoordinates(bCoords[2][0], bCoords[2][1]);
						if (element.isPlaneElement()) {
							if (((PlaneElement) element).getCoordsInElement(modelCoords) != null) {
								return true;
							}
						}
						if (element.isLineElement()) {
							if (((LineElement) element).getCoordsInElement(modelCoords) != null) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean isCircleInBox(double[][] bCoords, double[] p, double r) {
		if (selectionBoxContains(bCoords, p)) {
			return true;
		}
		for (int i = 0; i < 4; i++) {
			double[] diff0 = new double[2];
			diff0[0] = p[0]-bCoords[i][0];
			diff0[1] = p[1]-bCoords[i][1];
			if (diff0[0]*diff0[0]+diff0[1]*diff0[1] < r*r) {
				return true;
			}
			double[] diff1 = new double[2];
			diff1[0] = p[0]-bCoords[(i+1)%4][0];
			diff1[1] = p[1]-bCoords[(i+1)%4][1];
			double[] diff = new double[2];
			diff[0] = bCoords[(i+1)%4][0]-bCoords[i][0];
			diff[1] = bCoords[(i+1)%4][1]-bCoords[i][1];
			if (diff[0]*diff0[0]+diff[1]*diff0[1] > 0.0 && diff[0]*diff1[0]+diff[1]*diff1[1] < 0.0) {
				double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]);
				double scal = Math.abs(diff0[1]*diff[0]-diff0[0]*diff[1])/length;
				if (scal < r) {
					return true;
				}
			}
		}
		return false;
	}

	private void selectNodesInBox() {
		ArrayList<Set> sets = SimLive.model.getSets();
		double[][] bCoords = new double[4][2];
		bCoords[0][0] = selectionBox.x;
		bCoords[0][1] = selectionBox.y;
		bCoords[1][0] = selectionBox.x+selectionBox.width;
		bCoords[1][1] = selectionBox.y;
		bCoords[2][0] = selectionBox.x+selectionBox.width;
		bCoords[2][1] = selectionBox.y+selectionBox.height;
		bCoords[3][0] = selectionBox.x;
		bCoords[3][1] = selectionBox.y+selectionBox.height;
		Stream<Set> stream = sets.stream();
		stream.forEach(set -> {
			if (set.view != Set.View.HIDDEN) {
				for (int n = 0; n < set.getNodes().size(); n++) {
					Node node = set.getNodes().get(n);
					if (!selectedNodes.contains(node)) {
						double[] coords = getCoordsWithScaledDisp(node.getID());
						double factor = getSizeFactorPerspective(coords);
						double[] p = modelToScreenCoordinates(coords);
						if (isCircleInBox(bCoords, p, SimLive.NODE_RADIUS/2.0/factor)) {
							selectedNodes.add(node);
							selectedSets.clear();
						}
					}
				}
			}
		});
	}
	
	private boolean selectionBoxContains(double[][] boxCoords, double[] point) {
		double minX = Math.min(boxCoords[0][0], boxCoords[1][0]);
		double maxX = Math.max(boxCoords[0][0], boxCoords[1][0]);
		double minY = Math.min(boxCoords[0][1], boxCoords[2][1]);
		double maxY = Math.max(boxCoords[0][1], boxCoords[2][1]);
		if (point[0] < minX || point[0] > maxX || point[1] < minY || point[1] > maxY) {
			return false;
		}
		return true;
	}
	
	public void cleanViewData() {
		for (int n = selectedNodes.size()-1; n > -1; n--) {
			if (selectedNodes.get(n).getID() > SimLive.model.getNodes().size()-1) {
				selectedNodes.remove(n);
			}
		}
		for (int s = selectedSets.size()-1; s > -1; s--) {
			if (selectedSets.get(s).getID() > SimLive.model.getSets().size()-1) {
				selectedSets.remove(s);
			}
		}
		for (int p = selectedParts3d.size()-1; p > -1; p--) {
			if (selectedParts3d.get(p).getID() > SimLive.model.getParts3d().size()-1) {
				selectedParts3d.remove(p);
			}
		}
	}
	
	public ArrayList<Node> getSelectedNodes() {
		ArrayList<Node> selectedNodes = new ArrayList<Node>();
		selectedNodes.addAll(this.selectedNodes);
		return selectedNodes;
	}
	
	public ArrayList<Set> getSelectedSets() {
		ArrayList<Set> selectedSets = new ArrayList<Set>();
		selectedSets.addAll(this.selectedSets);
		return selectedSets;
	}
	
	public void setSelectedSets(ArrayList<Set> selectedSets) {
		this.selectedSets = selectedSets;
	}
	
	public ArrayList<Part3d> getSelectedParts3d() {
		ArrayList<Part3d> selectedParts3d = new ArrayList<Part3d>();
		selectedParts3d.addAll(this.selectedParts3d);
		return selectedParts3d;
	}
	
	public void setSelectedParts3d(ArrayList<Part3d> selectedParts3d) {
		this.selectedParts3d = selectedParts3d;
	}
	
	public void deselectAll() {
		selectedParts3d.clear();
		selectedSets.clear();
		selectedNodes.clear();
		selectedMeasurement = null;
		selectedLabel = null;
		//Sim2d.model.updateModel();
		selectionBox = null;
		removeUnfinalizedMeasurement();
		SimLive.synchronizeModelTreeWithViewSelection();
	}
	
	public void deselectAllAndDisposeDialogs() {
		deselectAll();
		redraw();
		if (SimLive.mode != Mode.SOLUTION && SimLive.mode != Mode.RESULTS &&
				SimLive.getModelTreeSelection().isEmpty()) {
			SimLive.disposeDialogAreas();
		}
	}
	
	public void deleteSelectedSets() {
		for (int s = 0; s < selectedSets.size(); s++) {
			Set set = selectedSets.get(s);
			SimLive.model.getSets().remove(set);
			SimLive.model.getElements().removeAll(set.getElements());
		}
		SimLive.setDisplayPartsLabel();
		SimLive.disposeDialogAreas();
		deselectAll();
	}
	
	public void deleteSelectedLabel() {
		if (SimLive.post == null || (selectedLabel != SimLive.post.getMinLabel() && selectedLabel != SimLive.post.getMaxLabel())) {
			labels.remove(selectedLabel);
			selectedLabel = null;
		}
	}
	
	public void deleteSelectedMeasurement() {
		measurements.remove(selectedMeasurement);
		selectedMeasurement = null;
	}
	
	public void deleteAllLabels() {
		labels.clear();
		selectedLabel = null;
		if (SimLive.post != null) SimLive.post.updateMinMaxLabels();
		redraw();
	}
	
	public void deleteAllMeasurements() {
		measurements.clear();
		selectedMeasurement = null;
		redraw();
	}
	
	private void reorderSetsByID(ArrayList<Set> sets) {
		ArrayList<Set> setsReorderedByID = new ArrayList<Set>();
		setsReorderedByID.addAll(sets);
		for (int i = 0; i < setsReorderedByID.size(); i++) {
			int ID = Integer.MAX_VALUE;
			for (int s = 0; s < sets.size(); s++) {
				if (sets.get(s).getID() < ID) {
					ID = sets.get(s).getID();
					setsReorderedByID.set(i, sets.get(s));
				}
			}
			sets.remove(setsReorderedByID.get(i));
		}
		sets.addAll(setsReorderedByID);
	}
	
	public void copySelectedSets() {
		/* copy selected sets - only parts are copied */
		ArrayList<Set> newSets = new ArrayList<Set>();
		int nNodes = SimLive.model.getNodes().size();
		int[] indices = new int[nNodes];
		
		/* copy nodes */
		{
			ArrayList<Node> newNodes = new ArrayList<Node>();
			for (int s = 0; s < selectedSets.size(); s++) {
				Set set = selectedSets.get(s);
				for (int n = 0; n < set.getNodes().size(); n++) {
					if (!newNodes.contains(set.getNodes().get(n))) {
						newNodes.add(set.getNodes().get(n));
					}
				}
			}
			
			for (int n = 0; n < newNodes.size(); n++) {
				indices[newNodes.get(n).getID()] = n + nNodes;
			}
			
			for (int n = 0; n < newNodes.size(); n++) {
				double[] coords = newNodes.get(n).getCoords();
				newNodes.set(n, new Node(coords[0], coords[1], coords[2]));
			}		
			
			SimLive.model.getNodes().addAll(newNodes);
		}
		
		/* copy sets and elements */
		{
			reorderSetsByID(selectedSets);
			for (int s = 0; s < selectedSets.size(); s++) {
				Set newSet = selectedSets.get(s).clone(SimLive.model);
				copySubSets(newSet, indices);
				newSets.add(newSet);
				SimLive.model.getSets().add(newSet);
			}			
		}
		
		SimLive.model.updateModel();
		deselectAll();
		selectedSets.addAll(newSets);
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
		double arrowSize = 0.5*SimLive.ARROW_SIZE/getViewport()[3]/zoom;
		((PartDialog) SimLive.dialogArea).updateDialog(new double[]{arrowSize, -arrowSize, 0});
	}
	
	private void copySubSets(Set set, int[] indices) {
		if (!set.getSets().isEmpty()) {
			ArrayList<Element> elements = new ArrayList<Element>();
			for (int s = 0; s < set.getSets().size(); s++) {
				copySubSets(set.getSets().get(s), indices);
				elements.addAll(set.getSets().get(s).getElements());
			}
			set.getElements().clear();
			set.getElements().addAll(elements);
		}
		else {
			ArrayList<Element> newElements = new ArrayList<Element>();
			
			if (set.getType() == Set.Type.BASIC && set.getElements().size() > 1) {
				/* set used by distributed load - copy as single beam */
				Element element = set.getElements().get(0);
				Element newElement = element.clone(SimLive.model);
				int[] elementNodes = new int[element.getElementNodes().length];
				elementNodes[0] = indices[element.getElementNodes()[0]];
				elementNodes[1] = indices[set.getElements().get(set.getElements().size()-1).getElementNodes()[1]];
				newElement.setElementNodes(elementNodes);
				newElements.add(newElement);
			}
			else {
				for (int e = 0; e < set.getElements().size(); e++) {
					Element element = set.getElements().get(e);
					Element newElement = element.clone(SimLive.model);
					int[] elementNodes = new int[element.getElementNodes().length];
					for (int i = 0; i < element.getElementNodes().length; i++) {
						elementNodes[i] = indices[element.getElementNodes()[i]];
					}
					newElement.setElementNodes(elementNodes);
					newElements.add(newElement);
				}
			}
			
			SimLive.model.getElements().addAll(newElements);
			set.getElements().clear();
			set.getElements().addAll(newElements);
		}
	}
	
	public void flipSelectedSets() {
		flipSets(selectedSets);
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
	}

	private void flipSets(ArrayList<Set> sets) {
		for (int s = 0; s < sets.size(); s++) {
			for (int e = 0; e < sets.get(s).getElements().size(); e++) {
				Element element = sets.get(s).getElements().get(e);
				int[] elemNodes = element.getElementNodes();
				int[] newElemNodes = new int[elemNodes.length];
				for (int i = 0; i < elemNodes.length; i++) {
					newElemNodes[i] = elemNodes[elemNodes.length-1-i];
				}
				element.setElementNodes(newElemNodes);
			}
			// distributed load
			if (sets.get(s).getType() == Set.Type.BASIC && sets.get(s).getElements().size() > 1) {
				ArrayList<Element> elements = sets.get(s).getElements();
				for (int e = 0; e < elements.size(); e++) {
					elements.add(e, elements.get(elements.size()-1));
					elements.remove(elements.size()-1);
				}
				sets.get(s).update();
			}
		}
	}

	public void ungroupSelectedSets() {
		ungroupSets(selectedSets);
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
	}
	
	private void ungroupSets(ArrayList<Set> sets) {
		for (int s = sets.size()-1; s > -1; s--) {
			Set set = sets.get(s);
			if (!set.getSets().isEmpty()) {
				for (int s1 = 0; s1 < set.getSets().size(); s1++) {
					Set newSet = set.getSets().get(s1);
					newSet.view = set.view;
					SimLive.model.getSets().add(newSet);
					sets.add(newSet);
				}
				SimLive.model.getSets().remove(set);
				sets.remove(set);
			}
			else if (set.getType() != Set.Type.BASIC) {
				for (int e = 0; e < set.getElements().size(); e++) {
					Set newSet = new Set(set.getElements().get(e), Set.Type.BASIC);
					newSet.view = set.view;
					SimLive.model.getSets().add(newSet);
					sets.add(newSet);
				}
				SimLive.model.getSets().remove(set);
				sets.remove(set);
			}
		}
	}
	
	private void ungroupSetsRecursive(ArrayList<Set> sets) {
		for (int s = sets.size()-1; s > -1; s--) {
			Set set = sets.get(s);
			if (!set.getSets().isEmpty()) {
				for (int s1 = 0; s1 < set.getSets().size(); s1++) {
					Set newSet = set.getSets().get(s1);
					newSet.view = set.view;
					SimLive.model.getSets().add(newSet);
					sets.add(newSet);
				}
				SimLive.model.getSets().remove(set);
				sets.remove(set);
				ungroupSetsRecursive(sets);
			}
			else if (set.getType() != Set.Type.BASIC) {
				for (int e = 0; e < set.getElements().size(); e++) {
					Set newSet = new Set(set.getElements().get(e), Set.Type.BASIC);
					newSet.view = set.view;
					SimLive.model.getSets().add(newSet);
					sets.add(newSet);
				}
				SimLive.model.getSets().remove(set);
				sets.remove(set);
			}
		}
	}
	
	public void groupSelectedSets() {
		groupSets(selectedSets);
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
	}
	
	private void groupSets(ArrayList<Set> sets) {
		ArrayList<Element> elements = new ArrayList<Element>();
		for (int s = 0; s < sets.size(); s++) {
			Set set = sets.get(s);
			elements.addAll(set.getElements());
		}
		reorderSetsByID(sets);
		Set newSet = new Set(elements, sets, Set.Type.COMPOSITE);
		newSet.view = sets.get(0).view;
		SimLive.model.getSets().removeAll(sets);
		SimLive.model.getSets().add(newSet);
		sets.clear();
		sets.add(newSet);
	}
	
	public void relaxSelectedSets() {
		SimLive.model.relaxMesh(selectedSets);
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
	}
	
	public void splitSelectedSets() {
		splitSets(selectedSets);
		SimLive.model.updateModel();
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
	}
	
	private void splitSets(ArrayList<Set> sets) {
		for (int s = 0; s < sets.size(); s++) {
			Set set = sets.get(s);
			if (!(set.getType() == Set.Type.BASIC && set.getElements().size() > 1)) {
				for (int e = 0; e < set.getElements().size(); e++) {
					int[] elemNodes = set.getElements().get(e).getElementNodes();
					for (int n = 0; n < elemNodes.length; n++) {
						double[] coords = SimLive.model.getNodes().get(elemNodes[n]).getCoords();
						Node node = new Node(coords[0], coords[1], coords[2]);
						SimLive.model.getNodes().add(node);
						elemNodes[n] = SimLive.model.getNodes().size()-1;
					}
					set.getElements().get(e).setElementNodes(elemNodes);
				}
			}
		}
	}
	
	public void mergeSelectedSets() {
		mergeSets(selectedSets);
		SimLive.model.updateModel();
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
	}
	
	private void mergeSets(ArrayList<Set> sets) {
		ArrayList<Element> elements = new ArrayList<Element>();
		for (int s = 0; s < sets.size(); s++) {
			Set set = sets.get(s);
			elements.addAll(set.getElements());
		}
		SimLive.model.mergeCoincidentNodes(elements);
	}
	
	public void refineSelectedSets(int levels) {
		for (int s = 0; s < selectedSets.size(); s++) {
			Set set = selectedSets.get(s);
			SimLive.model.refineSet(set, levels);
		}
		SimLive.dialogArea = new PartDialog(SimLive.compositeLeft,
				SWT.NONE, selectedSets, SimLive.settings);
	}
	
	public void deleteSelectedParts3d() {
		for (int s = 0; s < selectedParts3d.size(); s++) {
			Part3d part3d = selectedParts3d.get(s);
			SimLive.model.getParts3d().remove(part3d);
		}
		SimLive.disposeDialogAreas();
		deselectAll();
	}
	
	public void copySelectedParts3d() {
		for (int s = selectedParts3d.size()-1; s > -1 ; s--) {
			Part3d newPart3d = selectedParts3d.get(s).clone();
			for (int n = 0; n < newPart3d.getNrVertices(); n++) {
				double[] coords = newPart3d.getVertex(n).getCoords();
				newPart3d.setVertex(new Vertex3d(coords), n);
			}
			SimLive.model.getParts3d().add(newPart3d);
			selectedParts3d.remove(s);
			selectedParts3d.add(newPart3d);
		}
		
		SimLive.dialogArea = new Part3dDialog(SimLive.compositeLeft,
				SWT.NONE, selectedParts3d, SimLive.settings);
		double arrowSize = 0.5*SimLive.ARROW_SIZE/getViewport()[3]/zoom;
		((Part3dDialog) SimLive.dialogArea).updateDialog(new double[]{arrowSize, -arrowSize, 0});
	}
	
	public void flipSelectedParts3d() {
		for (int s = 0; s < selectedParts3d.size(); s++) {
			Part3d part3d = selectedParts3d.get(s);
			part3d.flip();
		}
		
		SimLive.dialogArea = new Part3dDialog(SimLive.compositeLeft,
				SWT.NONE, selectedParts3d, SimLive.settings);
	}
	
	public void ungroupSelectedParts3d() {
		ungroupParts3d(selectedParts3d);
		SimLive.dialogArea = new Part3dDialog(SimLive.compositeLeft,
				SWT.NONE, selectedParts3d, SimLive.settings);
	}
	
	private void ungroupParts3d(ArrayList<Part3d> parts3d) {
		for (int s = parts3d.size()-1; s > -1; s--) {
			Part3d part3d = parts3d.get(s);
			SubTree subTree = part3d.getSubTree();
			
			int nrVertices = 0, vertexCount = 0, facetCount = 0;
			if (!subTree.subTrees.isEmpty()) {
				for (int s1 = 0; s1 < subTree.subTrees.size(); s1++) {
					Part3d newPart3d = new Part3d(subTree.subTrees.get(s1).nrVertices,
							subTree.subTrees.get(s1).nrFacets);
					newPart3d.render = part3d.render;
					newPart3d.setSubTree(subTree.subTrees.get(s1));
					for (int v = 0; v < newPart3d.getNrVertices(); v++) {
						newPart3d.setVertex(part3d.getVertex(vertexCount++), v);
					}
					for (int f = 0; f < newPart3d.getNrFacets(); f++) {
						Facet3d facet = part3d.getFacet(facetCount++);
						int[] indices = facet.getIndices().clone();
						indices[0] -= nrVertices;
						indices[1] -= nrVertices;
						indices[2] -= nrVertices;
						newPart3d.setFacet(new Facet3d(indices, facet.getColorID()), f);
					}
					nrVertices += newPart3d.getNrVertices();
					SimLive.model.getParts3d().add(newPart3d);
					parts3d.add(newPart3d);
				}
				SimLive.model.getParts3d().remove(part3d);
				parts3d.remove(part3d);
			}			
		}
	}
	
	public void groupSelectedParts3d() {
		groupParts3d(selectedParts3d);
		SimLive.dialogArea = new Part3dDialog(SimLive.compositeLeft,
				SWT.NONE, selectedParts3d, SimLive.settings);
	}
	
	private void groupParts3d(ArrayList<Part3d> parts3d) {
		int nrVertices = 0, nrFacets = 0;
		SubTree subTree = new SubTree();
		for (int s = 0; s < parts3d.size(); s++) {
			nrVertices += parts3d.get(s).getNrVertices();
			nrFacets += parts3d.get(s).getNrFacets();
			subTree.subTrees.add(parts3d.get(s).getSubTree());
		}
		Part3d newPart3d = new Part3d(nrVertices, nrFacets);
		newPart3d.render = parts3d.get(0).render;
		subTree.nrVertices = nrVertices;
		subTree.nrFacets = nrFacets;
		nrVertices = 0;
		int vIndex = 0, fIndex = 0;
		for (int s = 0; s < parts3d.size(); s++) {
			for (int v = 0; v < parts3d.get(s).getNrVertices(); v++) {
				newPart3d.setVertex(parts3d.get(s).getVertex(v).clone(), vIndex++);
			}
			for (int f = 0; f < parts3d.get(s).getNrFacets(); f++) {
				int[] indices = parts3d.get(s).getFacet(f).getIndices().clone();
				indices[0] += nrVertices;
				indices[1] += nrVertices;
				indices[2] += nrVertices;
				newPart3d.setFacet(new Facet3d(indices, parts3d.get(s).getFacet(f).getColorID()), fIndex++);
			}
			nrVertices += parts3d.get(s).getNrVertices();
		}
		newPart3d.setSubTree(subTree);
		SimLive.model.getParts3d().removeAll(parts3d);
		SimLive.model.getParts3d().add(newPart3d);
		parts3d.clear();
		parts3d.add(newPart3d);
	}
	
	public void startAnimation() {
		Timer timer = new Timer();
		animation = new TimerTask() {			
			double incrementID;
			public void run() {
				if (!SimLive.shell.isDisposed()) {
					SimLive.shell.getDisplay().syncExec(new Runnable() {
						public void run() {
							if (isAnimationRunning()) {
								if ((int) incrementID != SimLive.post.getPostIncrementID()) {
									incrementID = SimLive.post.getPostIncrementID();
								}
								if (SimLive.post.isReverseAnimation()) {
									if (SimLive.post.getPostIncrementID() == SimLive.post.getSolution().getNumberOfIncrements()) {
										backwards = true;
									}
									if (SimLive.post.getPostIncrementID() == 0) {
										backwards = false;
									}
								}
								if (backwards) {
									incrementID -= SimLive.post.getAnimationSpeed();
									SimLive.post.previousPostIncrementID(incrementID);
								}
								else {
									incrementID += SimLive.post.getAnimationSpeed();
									SimLive.post.nextPostIncrementID(incrementID);
								}
								((ResultsDialog) SimLive.dialogArea).setSliderValue(SimLive.post.getPostIncrementID());
								SimLive.post.updateMinMaxLabels();
								redraw();
								SimLive.updateMatrixView();
							}
						}
					});
				}
			}
		};
		timer.schedule(animation, 0, 1);
	}
	
	public void stopAnimation() {
		animation.cancel();
		animation = null;
	}
	
	public boolean isAnimationRunning() {
		return animation != null;
	}

	public void setMousePos(double[] pos) {
		double[] screen = modelToScreenCoordinates(pos);
		mousePos = new int[]{(int) Math.round(screen[0]), (int) Math.round(screen[1])};
	}

	public void mouseMove(int deltaX, int deltaY) {
		if (!(deltaX == 0 && deltaY == 0) && Math.abs(deltaX) <= SimLive.SNAP_TOL && Math.abs(deltaY) <= SimLive.SNAP_TOL) {
			try {
				mousePos[0] += deltaX;
				mousePos[1] += deltaY;
				Robot r = new Robot();
				Point p = toDisplay(mousePos[0], mousePos[1]);
				int count = 0;
				while (MouseInfo.getPointerInfo().getLocation().x != p.x ||
						MouseInfo.getPointerInfo().getLocation().y != p.y) {
					robotMove = true;
					r.mouseMove(p.x, p.y);
					if (count > 10) break;
					count++;
				}
			}
			catch (AWTException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static double[] getCoordsWithScaledDisp(int nodeID) {
		double[] coords = SimLive.model.getNodes().get(nodeID).getCoords().clone();
		if (SimLive.mode == Mode.RESULTS) {
			double[] disp = SimLive.post.getPostIncrement().getDisplacement(nodeID);
			double scaling = SimLive.post.getScaling();
			coords[0] += disp[0]*scaling;
			coords[1] += disp[1]*scaling;
			coords[2] += disp[2]*scaling;
		}
		return coords;
	}
	
	public static Matrix getRotation(Vertex3d vertex) {
		if (SimLive.mode == Mode.RESULTS && vertex.getElementID() > -1) {
			return SimLive.post.getPostIncrement().getRotation(vertex.getElementID(),
						vertex.getT(), vertex.getR());
		}
		return Matrix.identity(3, 3);
	}
	
	public static double[] getCoordsWithScaledDisp(Vertex3d vertex, Matrix Rotation) {
		double[] coords = vertex.getCoords().clone();
		if (SimLive.mode == Mode.RESULTS && vertex.getElementID() > -1) {
			if (Rotation == null) {
				Rotation = getRotation(vertex);
			}
			double[] disp = SimLive.post.getPostIncrement().getDisplacement(coords, vertex.getElementID(),
					vertex.getT(), vertex.getR(), Rotation);
			coords[0] += disp[0];
			coords[1] += disp[1];
			coords[2] += disp[2];
		}
		return coords;
	}
	
	public Matrix getFocusPointOrientation() {
		if (focusPoint.getElement() != null) {
			double[][] Rr = View.Rr[focusPoint.getElement().getID()];
			return Rr != null ? new Matrix(Rr) : null;
		}
		else {
			int[] indices = focusPoint.getFacet3d().getIndices();
			double[][] coords = new double[3][];
			coords[0] = getCoordsWithScaledDisp(focusPoint.getPart3d().getVertex(indices[0]), null);
			coords[1] = getCoordsWithScaledDisp(focusPoint.getPart3d().getVertex(indices[1]), null);
			coords[2] = getCoordsWithScaledDisp(focusPoint.getPart3d().getVertex(indices[2]), null);
			Matrix diff0 = new Matrix(coords[1], 3).minus(new Matrix(coords[0], 3));
			Matrix diff1 = new Matrix(coords[2], 3).minus(new Matrix(coords[0], 3));
			Matrix norm = diff0.crossProduct(diff1);
			diff1 = norm.crossProduct(diff0);
			Matrix Rr = new Matrix(3, 3);
			Rr.setMatrix(0, 2, 0, 0, diff0.times(1.0/diff0.normF()));
			Rr.setMatrix(0, 2, 1, 1, diff1.times(1.0/diff1.normF()));
			Rr.setMatrix(0, 2, 2, 2, norm.times(1.0/norm.normF()));
			return Rr;
		}
	}
	
	private void switchSingleDoubleSided(GL2 gl2, boolean doubleSided) {
		if (doubleSided) {
			/* double sided lighting without face culling */
			gl2.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
			gl2.glDisable(GL2.GL_CULL_FACE);
			gl2.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
		}
		else {
			/* single sided lighting with face culling */
			gl2.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_FALSE);
			gl2.glEnable(GL2.GL_CULL_FACE);
			gl2.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
		}
	}
	
	private void initLights(GL2 gl2) {
		gl2.glEnable(GL2.GL_LIGHTING);
		gl2.glEnable(GL2.GL_LIGHT0);
		gl2.glEnable(GL2.GL_NORMALIZE);
	}
	
	private int initProgram(GL2 gl2) {
        ShaderCode vertexShader = ShaderCode.create(gl2, GL2.GL_VERTEX_SHADER, this.getClass(), null, null, "shader", "vert", null, true);
        ShaderCode fragmentShader = ShaderCode.create(gl2, GL2.GL_FRAGMENT_SHADER, this.getClass(), null, null, "shader", "frag", null, true);
        ShaderProgram program = new ShaderProgram();
        program.add(vertexShader);
        program.add(fragmentShader);
        program.link(gl2, System.out);
        return program.program();
    }
	
	private void storeMeshDataElements(double scaling) {
		Rr = new double[SimLive.model.getElements().size()][][];
		deltaL = new double[SimLive.model.getElements().size()];
		if (SimLive.mode == Mode.RESULTS) {
			Matrix u_global = SimLive.post.getPostIncrement().get_u_global();
			for (int e = 0; e < SimLive.model.getElements().size(); e++) {
				if (SimLive.model.getElements().get(e).isLineElement()) {
					LineElement lineElement = (LineElement) SimLive.post.getSolution().getRefModel().getElements().get(e);
					Matrix u_elem = lineElement.globalToLocalVector(u_global).times(scaling);
					double currentLength = lineElement.getCurrentLength(SimLive.post.getSolution().getRefModel().getNodes(), u_elem);
					Matrix r1 = lineElement.getr1(SimLive.post.getSolution().getRefModel().getNodes(), u_elem, currentLength);
					if (lineElement.getType() == Element.Type.BEAM) {
						Matrix Rr_matrix = ((Beam) lineElement).getRr(u_elem, r1);
						double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(lineElement.getID());
						Rr_matrix = Rr_matrix.times(GeomUtility.getRotationMatrixX(angles[0][0]*scaling));
						Rr[e] = Rr_matrix.getArray();
					}
					else {
						Matrix Rr_matrix = lineElement.getVectorTransformation(r1.getColumnPackedCopy());
						Rr[e] = Rr_matrix.getArray();
					}
					deltaL[e] = currentLength-lineElement.getLength();
				}
				if (SimLive.model.getElements().get(e).isPlaneElement()) {
					PlaneElement planeElement = (PlaneElement) SimLive.post.getSolution().getRefModel().getElements().get(e);
					Matrix u_elem = planeElement.globalToLocalVector(u_global).times(scaling);
					Matrix Rr_matrix = planeElement.getRr(SimLive.model.getNodes(), u_elem);
					Rr[e] = Rr_matrix.getArray();
				}
			}
		}
		else {
			for (int e = 0; e < SimLive.model.getElements().size(); e++) {
				if (SimLive.model.getElements().get(e).isLineElement()) {
					LineElement lineElement = (LineElement) SimLive.model.getElements().get(e);
					Rr[e] = lineElement.getR0().getArray();
					deltaL[e] = 0.0;
				}
				if (SimLive.model.getElements().get(e).isPlaneElement()) {
					PlaneElement planeElement = (PlaneElement) SimLive.model.getElements().get(e);
					Rr[e] = planeElement.getR0().getArray();
				}
			}
		}
		
		outlineNormals0 = new Matrix[SimLive.model.getNodes().size()][0];
		for (int e = 0; e < SimLive.model.getElements().size(); e++) {
			Element element = SimLive.model.getElements().get(e);
			if (element.isPlaneElement()) {
				int[] elemNodes = element.getElementNodes();
				Matrix norm0 = ((PlaneElement) element).getR0().getMatrix(0, 2, 2, 2);
				for (int i = 0; i < elemNodes.length; i++) {
					if (isOutlineNode[elemNodes[i]]) {
						int j;
						for (j = 0; j < outlineNormals0[elemNodes[i]].length; j++) {
							if (outlineNormals0[elemNodes[i]][j].dotProduct(norm0) > 1.0-SimLive.ZERO_TOL) {
								break;
							}
						}
						if (j == outlineNormals0[elemNodes[i]].length) {
							outlineNormals0[elemNodes[i]] = Arrays.copyOf(outlineNormals0[elemNodes[i]], outlineNormals0[elemNodes[i]].length+1);
							outlineNormals0[elemNodes[i]][outlineNormals0[elemNodes[i]].length-1] = norm0;
						}
					}
				}
			}
		}
		
		outlineNormals = new double[SimLive.model.getNodes().size()][][];
		for (int i = 0; i < SimLive.model.getNodes().size(); i++) {
			outlineNormals[i] = new double[outlineNormals0[i].length][];
		}
		nodeNormals = new double[SimLive.model.getNodes().size()][];
		for (int e = 0; e < SimLive.model.getElements().size(); e++) {
			Element element = SimLive.model.getElements().get(e);
			if (element.isPlaneElement()) {
				int[] elemNodes = element.getElementNodes();
				Matrix norm0 = ((PlaneElement) element).getR0().getMatrix(0, 2, 2, 2);
				Matrix[] c = new Matrix[elemNodes.length];
				for (int i = 0; i < elemNodes.length; i++) {
					c[i] = new Matrix(getCoordsWithScaledDisp(elemNodes[i]), 3);
				}
				Matrix norm = null;
				if (elemNodes.length > 3) {
					norm = (c[2].minus(c[0])).crossProduct(c[3].minus(c[1]));
					norm = norm.times(1.0/norm.normF());
				}
				else {
					norm = (c[2].minus(c[1])).crossProduct(c[0].minus(c[1]));
					norm = norm.times(1.0/norm.normF());
				}
				for (int i = 0; i < elemNodes.length; i++) {
					if (nodeNormals[elemNodes[i]] == null) {
						nodeNormals[elemNodes[i]] = norm.getColumnPackedCopy();
					}
					else {
						nodeNormals[elemNodes[i]][0] += norm.get(0, 0);
						nodeNormals[elemNodes[i]][1] += norm.get(1, 0);
						nodeNormals[elemNodes[i]][2] += norm.get(2, 0);
					}
					if (isOutlineNode[elemNodes[i]]) {
						for (int j = 0; j < outlineNormals0[elemNodes[i]].length; j++) {
							if (outlineNormals0[elemNodes[i]][j].dotProduct(norm0) > SimLive.COS_ANGLE_INNER_EDGE) {
								if (outlineNormals[elemNodes[i]][j] == null) {
									outlineNormals[elemNodes[i]][j] = norm.getColumnPackedCopy();
								}
								else {
									outlineNormals[elemNodes[i]][j][0] += norm.get(0, 0);
									outlineNormals[elemNodes[i]][j][1] += norm.get(1, 0);
									outlineNormals[elemNodes[i]][j][2] += norm.get(2, 0);
								}
							}
						}
					}
				}
			}
		}
		for (int i = 0; i < SimLive.model.getNodes().size(); i++) {
			if (nodeNormals[i] != null) {
				double length = Math.sqrt(nodeNormals[i][0]*nodeNormals[i][0]+nodeNormals[i][1]*nodeNormals[i][1]+
						nodeNormals[i][2]*nodeNormals[i][2]);
				if (length > 0) {
					nodeNormals[i][0] /= length;
					nodeNormals[i][1] /= length;
					nodeNormals[i][2] /= length;
				}
			}
			for (int j = 0; j < outlineNormals[i].length; j++) if (outlineNormals[i][j] != null) {
				double length = Math.sqrt(outlineNormals[i][j][0]*outlineNormals[i][j][0]+outlineNormals[i][j][1]*outlineNormals[i][j][1]+
						outlineNormals[i][j][2]*outlineNormals[i][j][2]);
				if (length > 0) {
					outlineNormals[i][j][0] /= length;
					outlineNormals[i][j][1] /= length;
					outlineNormals[i][j][2] /= length;
				}
			}
		}
	}
	
	private void storeMeshDataParts3d() {
		for (int p = 0; p < SimLive.model.getParts3d().size(); p++) {
			Part3d part3d = SimLive.model.getParts3d().get(p);
			if (part3d.getNrVertices() > 0 && part3d.getVertex(0).getElementID() > -1) {
				Matrix[] R = new Matrix[part3d.getNrVertices()];
		    	double[][] vertexCoords = new double[part3d.getNrVertices()][];
		    	Stream<Vertex3d> stream = Stream.of(part3d.getVertices()).parallel();
				stream.forEach(vertex -> {
					R[vertex.getID()] = getRotation(vertex);
					vertexCoords[vertex.getID()] = getCoordsWithScaledDisp(vertex, R[vertex.getID()]);
				});
				part3d.setVertexCoords(vertexCoords);
				double[][] normals = new double[part3d.getNrFacets()*3][];
				double[][] normals0 = part3d.getNormals0();
				Stream<Facet3d> stream1 = Stream.of(part3d.getFacets()).parallel();
				stream1.forEach(facet -> {
					int[] indices = facet.getIndices();
					for (int i = 0; i < indices.length; i++) {
						normals[facet.getID()*3+i] = R[indices[i]].times(new Matrix(normals0[facet.getID()*3+i], 3)).getColumnPackedCopy();
					}
				});
				part3d.setNormals(normals);
			}
		}
    	
		if (animation == null && !isMouseDragged) {
			pVertices = new double[SimLive.model.getParts3d().size()][][];
			pPart3dBox = new double[SimLive.model.getParts3d().size()][8][];
			for (int p = 0; p < SimLive.model.getParts3d().size(); p++) {
				double[] min = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
				double[] max = new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
				Part3d part3d = SimLive.model.getParts3d().get(p);
				pVertices[p] = new double[part3d.getNrVertices()][];
				double[][] vertexCoords = part3d.getVertexCoords();
				for (int v = 0; v < part3d.getNrVertices(); v++) {
					double[] coords = vertexCoords[v];
					for (int i = 0; i < 3; i++) {
						if (coords[i] < min[i]) min[i] = coords[i];
						if (coords[i] > max[i]) max[i] = coords[i];
					}
					pVertices[p][v] = modelToScreenCoordinates(coords);
				}
				pPart3dBox[p][0] = modelToScreenCoordinates(min);
				pPart3dBox[p][1] = modelToScreenCoordinates(new double[]{max[0], min[1], min[2]});
				pPart3dBox[p][2] = modelToScreenCoordinates(new double[]{max[0], max[1], min[2]});
				pPart3dBox[p][3] = modelToScreenCoordinates(new double[]{min[0], max[1], min[2]});
				pPart3dBox[p][4] = modelToScreenCoordinates(new double[]{min[0], min[1], max[2]});
				pPart3dBox[p][5] = modelToScreenCoordinates(new double[]{max[0], min[1], max[2]});
				pPart3dBox[p][6] = modelToScreenCoordinates(max);
				pPart3dBox[p][7] = modelToScreenCoordinates(new double[]{min[0], max[1], max[2]});
			}
		}
	}
	
	public void drawingApplicationInitLines() {
		if (nodeID > -1) {
			lines = new double[SimLive.post.getSolution().getNumberOfIncrements()][];
			Node node = SimLive.model.getNodes().get(nodeID);
			int dof = SimLive.post.getSolution().getDofOfNodeID(node.getID());
			for (int i = 1; i < SimLive.post.getSolution().getNumberOfIncrements(); i++) {
				Matrix u0 = SimLive.post.getSolution().getIncrement(i).get_u_global();
				Matrix u1 = SimLive.post.getSolution().getIncrement(i-1).get_u_global();
				if (u0.get(dof+2, 0) < zDisp && u1.get(dof+2, 0) < zDisp) {
					lines[i] = new double[4];
					lines[i][0] = node.getXCoord()+u0.get(dof, 0);
					lines[i][1] = node.getYCoord()+u0.get(dof+1, 0);
					lines[i][2] = node.getXCoord()+u1.get(dof, 0);
					lines[i][3] = node.getYCoord()+u1.get(dof+1, 0);
				}
			}
		}
	}
	
	private void render(GL2 gl2, float width, float height) {
		
		double scaling = 0.0;
		if (SimLive.mode == Mode.RESULTS) {
			scaling = SimLive.post.getScaling();
		}
		//before setTransformation
		storeMeshDataElements(scaling);
		
		if (focusPoint != null && focusPointOrientation != null) {
			Matrix Rr = getFocusPointOrientation();
			Matrix deltaRFocus = Rr.times(focusPointOrientation.transpose());
			if (deltaRFocus.trace() < 3.0-SimLive.ZERO_TOL) {
				R0 = deltaRFocus.times(R0);
				focusPointOrientation = Rr;
			}
		}
		
		if (perspective) {
			double distToRotPoint = Math.sqrt((cameraRefPos[0]-rotPoint[0])*(cameraRefPos[0]-rotPoint[0])+
					(cameraRefPos[1]-rotPoint[1])*(cameraRefPos[1]-rotPoint[1])+
					(cameraRefPos[2]-rotPoint[2])*(cameraRefPos[2]-rotPoint[2]));
			zoom = height/width/distToRotPoint/Math.tan(fovy/2);
		}
		
		double time = -1;
		int stepNr = -1;
		Step step = null;
		if (SimLive.mode == Mode.RESULTS) {
			time = SimLive.post.getPostTime();
			stepNr = SimLive.post.getPostIncrement().getStepNr();
			step = SimLive.model.getSteps().get(stepNr);
		}
		
		GLU glu = new GLU();
		GLUquadric outside = glu.gluNewQuadric();
		GLUquadric inside = glu.gluNewQuadric();
		glu.gluQuadricOrientation(inside, GLU.GLU_INSIDE);		
		initView(gl2, width, height);
		if (SimLive.settings.isShowAxes) {
			renderToImageBuffer(gl2, glu, width, height, outside, inside);
		}
		drawBackground(gl2);
		initLights(gl2);
		setTransformation(gl2, glu, width, height, zoom);
		gl2.glEnable(GL2.GL_DEPTH_TEST);
		gl2.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
		gl2.glPolygonOffset(1.0f, 1.0f);
		
		if (shaderProgram == -1) {
			shaderProgram = initProgram(gl2);
		}
		
		//after setTransformation
		storeMeshDataParts3d();
		
		ArrayList<Object> objects = SimLive.getModelTreeSelection();
		
		ArrayList<Part3d> connectParts3d = new ArrayList<Part3d>();
		if (SimLive.mode == SimLive.Mode.CONNECTORS) {
			for (int i = 0; i < objects.size(); i++) {
				if (((AbstractConnector) objects.get(i)).getConnectorType() == ConnectorType.CONNECTOR_3D) {
					connectParts3d.addAll(((Connector3d) objects.get(i)).getParts3d());
				}
			}
		}
		
		for (int p = 0; p < SimLive.model.getParts3d().size(); p++) {
	    	Part3d part3d = SimLive.model.getParts3d().get(p);
	    	switchSingleDoubleSided(gl2, part3d.doubleSided);
			renderPart3d(gl2, part3d, scaling, selectedParts3d.contains(part3d), connectParts3d.contains(part3d));
	    }
		gl2.glEnable(GL2.GL_LIGHTING);
		switchSingleDoubleSided(gl2, false);
		
		ArrayList<Set> connectSets0 = new ArrayList<Set>();
		ArrayList<Set> connectSets1 = new ArrayList<Set>();		
		if (SimLive.mode == SimLive.Mode.CONNECTORS) {
			for (int i = 0; i < objects.size(); i++) {
				AbstractConnector connect = (AbstractConnector) objects.get(i);
				if (connect.getConnectorType() == ConnectorType.CONNECTOR &&
						((Connector) connect).getSet0() != null && ((Connector) connect).getSet1() != null) {
					connectSets0.add(((Connector) connect).getSet0());
					connectSets1.add(((Connector) connect).getSet1());
				}
				if (connect.getConnectorType() == ConnectorType.CONNECTOR_3D) {
					connectSets1.addAll(((Connector3d) connect).getParts());
				}
			}
		}
		if (SimLive.mode == SimLive.Mode.CONTACTS) {
			for (int i = 0; i < objects.size(); i++) {
				connectSets1.addAll(((ContactPair) objects.get(i)).getMasterSets());
			}
		}
	    
		float[] projection = new float[16];
	    gl2.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projection, 0);
	    double lineElementRadius = SimLive.LINE_ELEMENT_RADIUS/width/zoom;
	    
	    //fill pElementScreenPolys for hidden sets
	    pElementScreenPolys = new double[SimLive.model.getElements().size()][][];
		if (animation == null) {
			//render offscreen
			int[] params = new int[1];
			gl2.glGenFramebuffers(1, params, 0);
			gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, params[0]);
  			for (int s = 0; s < SimLive.model.getSets().size(); s++) {
  		    	Set set = SimLive.model.getSets().get(s);
  		    	if (set != null && set.view == Set.View.HIDDEN) {
  			    	for (int e = 0; e < set.getElements().size(); e++) {
  						Element element = set.getElements().get(e);
  						if (element.isPlaneElement()) {
  							renderPlaneElement(gl2, element, projection, null, new float[3], false);
  						}
  						else {
  							renderLineElementAndPointMass(gl2, element, connectSets0, connectSets1,
  									width, glu, outside, inside, lineElementRadius, scaling, zoom, new float[3]);
  						}
  			    	}
  		    	}
  		    }
  			gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
  		}
	  	
	    ArrayList<Set> sets0 = new ArrayList<Set>();
		sets0.addAll(SimLive.model.getSets());
		sets0.removeAll(selectedSets);
		
		//drawing application
		if (nodeID > -1 && lines != null && SimLive.mode == Mode.RESULTS) {
			gl2.glDisable(GL2.GL_LIGHTING);
			gl2.glLineWidth(3);
    		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
    		gl2.glBegin(GL2.GL_LINES);
    		int imax = Math.min(SimLive.post.getPostIncrementID()+1, lines.length);
			for (int i = 1; i < imax; i++) {
				if (lines[i] != null) {
					gl2.glVertex3d(lines[i][0], lines[i][1], zCoord);
					gl2.glVertex3d(lines[i][2], lines[i][3], zCoord);
				}
			}
			gl2.glEnd();
			gl2.glLineWidth(1);
    		gl2.glEnable(GL2.GL_LIGHTING);
		}
		
		/* sets */
	    renderSets(gl2, glu, connectSets0, connectSets1, projection, width, inside, outside,
	    		lineElementRadius, scaling, Set.View.DEFAULT, zoom, sets0, objects);
		
	    if (SimLive.mode == SimLive.Mode.CONTACTS) {
			connectSets1.clear();
		}
	    
	    /* rigid contacts */
	    switchSingleDoubleSided(gl2, true);
	    for (int c = 0; c < SimLive.model.getContactPairs().size(); c++) {
	    	double arrowSize = SimLive.ORIENTATION_SIZE/width/zoom;
			ContactPair contactPair = SimLive.model.getContactPairs().get(c);
			if (contactPair.getType() == Type.RIGID_DEFORMABLE) {
				gl2.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 0);
				float[] color = SimLive.COLOR_WHITE;
				if (SimLive.mode == SimLive.Mode.CONTACTS && objects.contains(contactPair)) {
		    		color = SimLive.COLOR_BLUE;
				}
				float[] modelview = new float[16];
			    gl2.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
		    	renderRigidPlaneElements(gl2, contactPair, projection, modelview, color, arrowSize, glu, inside, outside, objects);
			}
	    }
	    switchSingleDoubleSided(gl2, false);
	    		
		/*for (int c = 0; c < SimLive.model.getContactPairs().size(); c++) {
			ContactPair contactPair = SimLive.model.getContactPairs().get(c);
			if (contactPair.getType() == Type.RIGID_DEFORMABLE) {
				
				gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_WHITE, 0);
	    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);
	    		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
	    		gl2.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
	    		gl2.glPolygonOffset(1.0f, 1.0f);
	    		for (int e = 0; e < contactPair.getMasterElements().size(); e++) {
					int[] elementNodes = contactPair.getMasterElements().get(e).getElementNodes();
					int masterEdge = contactPair.getMasterEdges().get(e);
					int nodeID = elementNodes[masterEdge];
					double[] p0 = contactPair.getRigidNodes().get(nodeID).getCoords();
					nodeID = elementNodes[(masterEdge+1)%elementNodes.length];
					double[] p1 = contactPair.getRigidNodes().get(nodeID).getCoords();							
					double angle = Math.atan2(p1[1]-p0[1], p1[0]-p0[0])*180.0/Math.PI;
					
					gl2.glPushMatrix();
					gl2.glTranslated(p0[0], p0[1], p0[2]);
					gl2.glRotated(90, 0, 1, 0);
	    			gl2.glRotated(angle, -1, 0, 0);
	    			double length = Math.sqrt((p1[0]-p0[0])*(p1[0]-p0[0])+(p1[1]-p0[1])*(p1[1]-p0[1])+(p1[2]-p0[2])*(p1[2]-p0[2]));
	    			double thickness = lineElementRadius*2.0;
	    			if (contactPair.getMasterElements().get(e).isPlaneElement()) {
	    				thickness = ((PlaneElement) contactPair.getMasterElements().get(e)).getThickness();
	    			}
    				gl2.glBegin(GL2.GL_QUADS);
    				gl2.glNormal3d(0, 1, 0);
    				gl2.glVertex3d(thickness/2.0, 0, 0);
    				gl2.glVertex3d(-thickness/2.0, 0, 0.0);
    				gl2.glVertex3d(-thickness/2.0, 0, length);
    				gl2.glVertex3d(thickness/2.0, 0, length);
    				gl2.glNormal3d(0, -1, 0);
    				gl2.glVertex3d(-thickness/2.0, 0, 0);
    				gl2.glVertex3d(thickness/2.0, 0, 0.0);
    				gl2.glVertex3d(thickness/2.0, 0, length);
    				gl2.glVertex3d(-thickness/2.0, 0, length);
    				gl2.glEnd();
    				gl2.glDisable(GL2.GL_LIGHTING);
    				gl2.glBegin(GL2.GL_LINES);
    				gl2.glVertex3d(-thickness/2.0, 0, 0.0);
    				gl2.glVertex3d(-thickness/2.0, 0, length);
    				gl2.glVertex3d(thickness/2.0, 0, 0);
    				gl2.glVertex3d(thickness/2.0, 0, length);
    				gl2.glEnd();
    				gl2.glEnable(GL2.GL_LIGHTING);
	    			gl2.glPopMatrix();
				}
	    		gl2.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
			}
		}*/
		
		/* label lines in curve plots */
		/*if (SimLive.mode == Mode.RESULTS && SimLive.post.getScalarPlot() != null && SimLive.post.getScalarPlot().isCurvePlot()) {
			gl2.glDisable(GL2.GL_LIGHTING);
			gl2.glDisable(GL2.GL_DEPTH_TEST);
			for (int l = 0; l < labels.size(); l++) {
				Set set = SimLive.model.getSetByElement(labels.get(l).getElement());
				if (set.view != Set.View.HIDDEN) {				
					renderCurvePlotLine(gl2, labels.get(l), width, scaling);
				}
		    }
			if (labelAtMousePos != null && labelAtMousePos.getElement() != null) {
				renderCurvePlotLine(gl2, labelAtMousePos, width, scaling);
			}
			gl2.glEnable(GL2.GL_LIGHTING);
			gl2.glEnable(GL2.GL_DEPTH_TEST);
		}*/
		
		if (SimLive.settings.isShowGrid) {
    		gl2.glDisable(GL2.GL_LIGHTING);
    		drawGrid(gl2, width, height);
			gl2.glEnable(GL2.GL_LIGHTING);
		}
		
    	gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		/* sets */
	    renderSets(gl2, glu, connectSets0, connectSets1, projection, width, inside, outside,
	    		lineElementRadius, scaling, Set.View.PINNED, zoom, sets0, objects);
	    
	    /* sets */
	    renderSets(gl2, glu, connectSets0, connectSets1, projection, width, inside, outside,
	    		lineElementRadius, scaling, Set.View.PINNED, zoom, selectedSets, objects);
	    
	    gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
    	/* connectors */
		for (int c = 0; c < SimLive.model.getConnectors().size(); c++) {
			Connector connector = SimLive.model.getConnectors().get(c);
			if (connector.isCoordsSet()) {
				drawConnector(gl2, glu, connector, lineElementRadius, inside, outside, objects);
			}
		}
		
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
	    /* nodes */
		if (SimLive.settings.isShowNodes) {
			double nodeRadius = SimLive.NODE_RADIUS/width/zoom;
			for (int s = 0; s < SimLive.model.getSets().size(); s++) {
		    	Set set = SimLive.model.getSets().get(s);
		    	for (int n = 0; n < set.getNodes().size(); n++) {
			    	if (set.view != Set.View.HIDDEN || selectedSets.contains(set)) {
			    		double[] coords = getCoordsWithScaledDisp(set.getNodes().get(n).getID());
						boolean selected = selectedNodes.contains(set.getNodes().get(n));
				    	if (!selected && !SimLive.model.isConnectorAtNode(set.getNodes().get(n))) {
					    	gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_WHITE, 0);
				    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);	        	
				    		
				    		gl2.glPushMatrix();
				    		gl2.glTranslated(coords[0], coords[1], coords[2]);
					    	glu.gluSphere(outside, nodeRadius, SimLive.SPHERE_SLICES, SimLive.SPHERE_STACKS);
					    	gl2.glPopMatrix();
		    			}
		    		}
		    	}
			}
		}
		
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		/* orientations */
		if (SimLive.settings.isShowOrientations) {
			double arrowSize = SimLive.ORIENTATION_SIZE/width/zoom;
			for (int s = 0; s < SimLive.model.getSets().size(); s++) {
		    	Set set = SimLive.model.getSets().get(s);
		    	if (set.view != Set.View.HIDDEN || selectedSets.contains(set)) {
			    	gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);
			    	for (int e = 0; e < set.getElements().size(); e++) {
						Element element = set.getElements().get(e);
			    		if (element.isLineElement()) {
							int[] elemNodes = element.getElementNodes();
							double[] coords0 = getCoordsWithScaledDisp(elemNodes[0]);
							double[] coords1 = getCoordsWithScaledDisp(elemNodes[1]);
							gl2.glPushMatrix();
							gl2.glTranslated(0.5*(coords0[0]+coords1[0]), 0.5*(coords0[1]+coords1[1]), 0.5*(coords0[2]+coords1[2]));
							double[] R = getArrayFromRotationMatrix(new Matrix(View.Rr[element.getID()]), true);
							gl2.glMultMatrixd(R, 0);
					    	if (element.getType() == Element.Type.BEAM && SimLive.mode == Mode.RESULTS) {
					    		Beam beam = (Beam) element;
					    		double[] disp = beam.getBendingDispInCoRotatedFrame(0.5, SimLive.post.getPostIncrement().getAnglesBeam(beam.getID()));
					    		gl2.glTranslated(0, disp[0]*scaling, disp[1]*scaling);
					    		double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(element.getID());
					    		gl2.glRotated((angles[1][0]-angles[0][0])*0.5*scaling*180.0/Math.PI, 1, 0, 0);
					    	}
							
							{
					    		gl2.glPushMatrix();
					    		gl2.glRotatef(90, 0, 1, 0);
					    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_RED, 0);
						    	drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
										(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
										SimLive.ARROW_HEAD_FRACTION*arrowSize, false, outside, inside);
					    		gl2.glPopMatrix();
					    		
					    		gl2.glPushMatrix();
					    		gl2.glRotatef(-90, 1, 0, 0);
					    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_GREEN, 0);
						    	drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
										(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
										SimLive.ARROW_HEAD_FRACTION*arrowSize, false, outside, inside);
					    		gl2.glPopMatrix();
					    		
					    		gl2.glPushMatrix();
					    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_BLUE, 0);
						    	drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
										(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
										SimLive.ARROW_HEAD_FRACTION*arrowSize, false, outside, inside);
					    		gl2.glPopMatrix();
					    	}
					    	gl2.glPopMatrix();
						}
			    		/*if (element.isPlaneElement()) {
							double[] center = null;
							if (element.getType() == Element.Type.TRI) {
								center = ((PlaneElement) element).getGlobalFromLocalCoordinates(1.0/3.0, 1.0/3.0);
							}
			    			if (element.getType() == Element.Type.QUAD) {
								center = ((PlaneElement) element).getGlobalFromLocalCoordinates(0.0, 0.0);
							}
			    			gl2.glPushMatrix();
							gl2.glTranslated(center[0], center[1], center[2]);
							Matrix R = new Matrix(Rr[element.getID()]);
					    	Matrix RR = Matrix.identity(4, 4);
					    	RR.setMatrix(0, 2, 0, 2, R.transpose());
							gl2.glMultMatrixd(RR.getRowPackedCopy(), 0);
							{
					    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, Sim2d.COLOR_BLUE, 0);
						    	drawArrow(gl2, glu, Sim2d.ARROW_RADIUS_FRACTION*arrowSize,
										(1f-Sim2d.ARROW_HEAD_FRACTION)*arrowSize,
										Sim2d.ARROW_HEAD_FRACTION*arrowSize, false, outside, inside);
					    	}
					    	gl2.glPopMatrix();
						}*/
			    	}
		    	}
			}
		}
		
		/* external reactions */
		double arrowSize = SimLive.ARROW_SIZE/width/zoom;
		double nodeRadius = SimLive.NODE_RADIUS/width/zoom;
		/* supports */
    	for (int s = 0; s < SimLive.model.getSupports().size(); s++) {
			Support support = SimLive.model.getSupports().get(s);
			renderSupport(gl2, support, arrowSize, glu, outside, inside, objects);
		}
		
    	/*if (Sim2d.mode == Mode.RESULTS && Sim2d.settings.isShowReactions) {
			for (int s = 0; s < Sim2d.model.getSupports().size(); s++) {
				Support support = Sim2d.model.getSupports().get(s);
				renderReactions(gl2, glu, support.getNodes(), support.getAngle(), 0.0,
						support.isFixedXDisp(), support.isFixedYDisp(), arrowSize, nodeRadius, outside, inside);
			}
			
			for (int l = 0; l < Sim2d.model.getLoads().size(); l++) {
				Load load = Sim2d.model.getLoads().get(l);
				if (load.getType() == Load.Type.DISPLACEMENT) {
					double phi = Sim2d.post.getPostIncrement().getRotationOfReference(load.getReferenceNodes(), Sim2d.post.getPostIncrement().get_u_global(), Sim2d.post.getScaling());
					renderReactions(gl2, glu, load.getNodes(), load.getAngle(), phi,
							load.isXDisp(), load.isYDisp(), arrowSize, nodeRadius, outside, inside);
				}
			}
		}*/
		
		/* loads */
		for (int l = 0; l < SimLive.model.getLoads().size(); l++) {
			Load load = SimLive.model.getLoads().get(l);
			renderLoad(gl2, glu, load, time, scaling, nodeRadius, arrowSize, outside, inside, objects);
		}
		/* distributed loads */
		for (int l = 0; l < SimLive.model.getDistributedLoads().size(); l++) {
			DistributedLoad distributedLoad = SimLive.model.getDistributedLoads().get(l);
			renderDistributedLoad(gl2, glu, distributedLoad, time, scaling, nodeRadius, arrowSize, outside, inside, objects);
		}
		
		gl2.glDisable(GL2.GL_LIGHTING);
		
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		/* reference systems */
		if (SimLive.mode == Mode.LOADS) {
			for (int i = 0; i < objects.size(); i++) {
				Node referenceNode = null;
				AbstractLoad abstractLoad = (AbstractLoad) objects.get(i);
				if (abstractLoad != null) {
					if (abstractLoad.getLoadType() == LoadType.LOAD &&
							((Load) abstractLoad).getType() == Load.Type.DISPLACEMENT) {
						referenceNode = ((Load) abstractLoad).getReferenceNode();
					}
				}
				if (referenceNode != null) {
					double[] t = getModelViewMatrix();
					double[] coords = referenceNode.getCoords();
					double[] screen = modelToScreenCoordinates(coords);
			    	gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
			    	
				    gl2.glPushMatrix();
			    	gl2.glTranslated(coords[0], coords[1], coords[2]);
			    	glu.gluSphere(outside, nodeRadius, SimLive.SPHERE_SLICES, SimLive.SPHERE_STACKS);
			    	
			    	String text = "Reference Node";
					double halfWidth = renderer.getBounds(text).getWidth()/2.0;
			    	renderer.beginRendering((int) width, (int) height);
			    	drawText(renderer, 0, 0, SimLive.COLOR_BLACK, t, text, screen, (int) height, halfWidth, false);
			    	drawText(renderer, -1, 1, SimLive.COLOR_SELECTION, t, text, screen, (int) height, halfWidth, false);
			    	renderer.endRendering();
					
			    	gl2.glPopMatrix();
				}
			}
		}
		
		/* selected supports */
		if (SimLive.mode == SimLive.Mode.SUPPORTS) {
			for (int s = 0; s < objects.size(); s++) {			
				Support support = (Support) objects.get(s);
				if (support != null) {
					renderSupport(gl2, support, arrowSize, glu, outside, inside, objects);
				}
			}
		}
		/* selected loads and distributed loads */
		if (SimLive.mode == SimLive.Mode.LOADS) {
			for (int s = 0; s < objects.size(); s++) {			
				AbstractLoad abstractLoad = (AbstractLoad) objects.get(s);
				if (abstractLoad != null && abstractLoad.getLoadType() == AbstractLoad.LoadType.LOAD) {
					Load load = (Load) abstractLoad;
					renderLoad(gl2, glu, load, time, scaling, nodeRadius, arrowSize, outside, inside, objects);
				}
				if (abstractLoad != null && abstractLoad.getLoadType() == AbstractLoad.LoadType.DISTRIBUTED_LOAD) {
					DistributedLoad distributedLoad = (DistributedLoad) abstractLoad;
					renderDistributedLoad(gl2, glu, distributedLoad, time, scaling, nodeRadius, arrowSize, outside, inside, objects);
				}
			}
		}
		/* contact */
		if (SimLive.mode == SimLive.Mode.CONTACTS) {
			for (int i = 0; i < objects.size(); i++) {
				ContactPair contactPair = (ContactPair) objects.get(i);
				if (contactPair != null) {
					/* slave nodes */
					for (int n = 0; n < contactPair.getSlaveNodes().size(); n++) {
						double[] coords = getCoordsWithScaledDisp(contactPair.getSlaveNodes().get(n).getID());
						gl2.glColor3fv(SimLive.COLOR_RED, 0);
				    	
					    gl2.glPushMatrix();
				    	gl2.glTranslated(coords[0], coords[1], coords[2]);
				    	glu.gluSphere(outside, nodeRadius, SimLive.SPHERE_SLICES, SimLive.SPHERE_STACKS);
				    	gl2.glPopMatrix();
					}
				}
			}
		}
		
		/* new tri or quad element */
		if (SimLive.mode == Mode.PARTS) {
			if ((selectedNodes.size() == 2 && SimLive.settings.newPartType == Element.Type.TRI) ||
				(selectedNodes.size() == 3 && SimLive.settings.newPartType == Element.Type.QUAD)) {
				Node newNode = null;
				if (Snap.node != null) {
					newNode = Snap.node;
				}
				else if (Snap.coords2d != null) {
					newNode = new Node(Snap.coords2d[0], Snap.coords2d[1], 0);
					SimLive.model.getNodes().add(newNode);
				}
				
				if (!selectedNodes.contains(newNode)) {
					gl2.glDisable(GL2.GL_CULL_FACE);
					gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
					gl2.glEnable(GL2.GL_BLEND);
					
					gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
					gl2.glBegin(GL2.GL_LINE_STRIP);
					for (int n = 0; n < selectedNodes.size(); n++) {
						gl2.glVertex3dv(selectedNodes.get(n).getCoords(), 0);
					}
					gl2.glVertex3dv(newNode.getCoords(), 0);
					gl2.glVertex3dv(selectedNodes.get(0).getCoords(), 0);
			    	gl2.glEnd();
			    	gl2.glColor4fv(SimLive.COLOR_TRANSPARENT, 0);
			    	gl2.glBegin(GL2.GL_TRIANGLE_FAN);
			    	gl2.glVertex3dv(newNode.getCoords(), 0);
					for (int n = 0; n < selectedNodes.size(); n++) {
						gl2.glVertex3dv(selectedNodes.get(n).getCoords(), 0);
					}
					gl2.glEnd();
					
					gl2.glDisable(GL2.GL_BLEND);
					gl2.glEnable(GL2.GL_CULL_FACE);
				}
				
				if (Snap.node != newNode) SimLive.model.getNodes().remove(newNode);
			}
		}
		
		/* new line element or point mass */
		if (SimLive.mode == Mode.PARTS && selectionBox == null) {
			Node newNode = null;
			if (Snap.node != null) {
				newNode = Snap.node;
			}
			else if (Snap.coords2d != null) {
				newNode = new Node(Snap.coords2d[0], Snap.coords2d[1], 0);
				SimLive.model.getNodes().add(newNode);
			}
			
			Element element = null;
			if (selectedNodes.size() == 1) {
				if (SimLive.settings.newPartType == Element.Type.ROD)
					element = new Rod(new int[]{selectedNodes.get(0).getID(), newNode.getID()});
				if (SimLive.settings.newPartType == Element.Type.BEAM)
					element = new Beam(new int[]{selectedNodes.get(0).getID(), newNode.getID()});
				if (SimLive.settings.newPartType == Element.Type.SPRING)
					element = new Spring(new int[]{selectedNodes.get(0).getID(), newNode.getID()});
				if (SimLive.settings.newPartType == Element.Type.POINT_MASS &&
						selectedNodes.get(0) == newNode)
					element = new PointMass(new int[]{newNode.getID()});
			}
			
			if (element != null) {
				if (element.isLineElement() && !SimLive.model.getSections().isEmpty()) {
					((LineElement) element).setSection(SimLive.model.getSections().get(0));
				}
				gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
				gl2.glEnable(GL2.GL_BLEND);
				renderLineElementAndPointMass(gl2, element, connectSets0, connectSets1, width, glu, outside, inside,
		    			lineElementRadius, scaling, zoom, SimLive.COLOR_TRANSPARENT);
				gl2.glDisable(GL2.GL_BLEND);
			}
			
			if (Snap.node != newNode) SimLive.model.getNodes().remove(newNode);
		}
		
		/* geometric area */
		if (SimLive.mode == Mode.PARTS && selectionBox == null) {
			if (selectedNodes.size() == 1 && selectedNodes.get(0).getID() > Model.maxUsedNodeID) {
				double[] coords = selectedNodes.get(0).getCoords();
				double[] p = screenToModelCoordinates(mousePos[0], mousePos[1]);
				
				gl2.glDisable(GL2.GL_CULL_FACE);
				gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
				gl2.glEnable(GL2.GL_BLEND);
				
				if (SimLive.settings.newPartType == Element.Type.CIRCULAR) {
					double radius = Math.sqrt((p[0]-coords[0])*(p[0]-coords[0])+(p[1]-coords[1])*(p[1]-coords[1]));
					gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
					gl2.glPushMatrix();
					gl2.glTranslated(coords[0], coords[1], 0);
		    		drawArc(gl2, radius, 0.0, 2.0*Math.PI);
					gl2.glColor4fv(SimLive.COLOR_TRANSPARENT, 0);
					glu.gluDisk(outside, 0, radius, 50, 1);
		    		gl2.glPopMatrix();
				}
				if (SimLive.settings.newPartType == Element.Type.SPUR_GEAR) {
					double[] values = ((SpurGearDialog) SimLive.dialogArea).getValues();
					drawSpurGear(gl2, glu, outside, coords, values[0], values[1], values[2], values[3], values[4] == 1.0);
				}
				if (SimLive.settings.newPartType == Element.Type.RECTANGULAR) {
					gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
					gl2.glBegin(GL2.GL_LINE_STRIP);
			    	gl2.glVertex2d(coords[0], coords[1]);
			    	gl2.glVertex2d(p[0], coords[1]);
			    	gl2.glVertex2d(p[0], p[1]);
			    	gl2.glVertex2d(coords[0], p[1]);
			    	gl2.glVertex2d(coords[0], coords[1]);
			    	gl2.glEnd();
			    	gl2.glColor4fv(SimLive.COLOR_TRANSPARENT, 0);
		    		gl2.glBegin(GL2.GL_QUADS);
					gl2.glVertex2d(coords[0], coords[1]);
			    	gl2.glVertex2d(p[0], coords[1]);
			    	gl2.glVertex2d(p[0], p[1]);
			    	gl2.glVertex2d(coords[0], p[1]);
					gl2.glEnd();
				}
				if (SimLive.settings.newPartType == Element.Type.TRIANGULAR) {
					double offset = ((TriangularAreaDialog) SimLive.dialogArea).getOffset();
					gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
					gl2.glBegin(GL2.GL_LINE_STRIP);
			    	gl2.glVertex2d(coords[0], coords[1]);
			    	gl2.glVertex2d(p[0], coords[1]);
			    	gl2.glVertex2d(p[0]-offset, p[1]);
			    	gl2.glVertex2d(coords[0], coords[1]);
			    	gl2.glEnd();
			    	gl2.glColor4fv(SimLive.COLOR_TRANSPARENT, 0);
		    		gl2.glBegin(GL2.GL_TRIANGLES);
					gl2.glVertex2d(coords[0], coords[1]);
					gl2.glVertex2d(p[0], coords[1]);
			    	gl2.glVertex2d(p[0]-offset, p[1]);
			    	gl2.glEnd();
				}
				
				gl2.glDisable(GL2.GL_BLEND);
				gl2.glEnable(GL2.GL_CULL_FACE);
			}
		}
		
		/* selected nodes */
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		gl2.glDisable(GL2.GL_LIGHTING);
		{
			for (int n = 0; n < selectedNodes.size(); n++) {
				double[] coords = getCoordsWithScaledDisp(selectedNodes.get(n).getID());
				gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    	
			    gl2.glPushMatrix();
		    	gl2.glTranslated(coords[0], coords[1], coords[2]);
		    	glu.gluSphere(outside, nodeRadius, SimLive.SPHERE_SLICES, SimLive.SPHERE_STACKS);
		    	gl2.glPopMatrix();
			}
		}
		
		/* store screen coordinates of labels */
	    double[][] screenCoordsLabels = new double[labels.size()][];
	    for (int l = 0; l < labels.size(); l++) {
    		Label label = labels.get(l);
	    	double[] coords = label.getCoordinatesWithDeformation();
    		if (label.getElement().isLineElement() && SimLive.mode == Mode.RESULTS &&
	    		SimLive.post.getScalarPlot() != null && SimLive.post.getScalarPlot().isCurvePlot()) {
	    		double[] shift = getLabelBasePointShiftForCurvePlot((LineElement) label.getElement(), width, label, scaling, SimLive.post.getScalarPlot());
	    		coords[0] += shift[0];
	    		coords[1] += shift[1];
	    		coords[2] += shift[2];
	    	}
    		screenCoordsLabels[l] = modelToScreenCoordinates(coords);
	    }
    	
    	/* store screen coordinates of measurements */
    	double[][][] screenCoordsMeasurements = new double[measurements.size()][][];
    	for (int m = 0; m < measurements.size(); m++) {
    		Measurement measurement = measurements.get(m);
    		if (measurement.getType() == Measurement.Type.DISTANCE) {
	    		double[] start = measurement.getStartPoint();
	    		double[] end = measurement.getEndPoint();
	    		if (!measurement.isFinalized()) {
	    			end = Snap.coords3d != null ? Snap.coords3d : Snap.coords2d;
	    			measurement.setEndPoint(end, isControlKeyPressed);
	    		}
	    		double[] start1 = start.clone();
	    		double[] end1 = end.clone();
	    		double[] move = measurement.getMove();
	    		start1[0] += move[0];
	    		start1[1] += move[1];
	    		start1[2] += move[2];
		    	end1[0] += move[0];
		    	end1[1] += move[1];
		    	end1[2] += move[2];
		    	double[] mid1 = new double[3];
		    	mid1[0] = (start1[0]+end1[0])/2;
		    	mid1[1] = (start1[1]+end1[1])/2;
		    	mid1[2] = (start1[2]+end1[2])/2;
		    	screenCoordsMeasurements[m] = new double[5][];
	    		screenCoordsMeasurements[m][0] = modelToScreenCoordinates(start1);
	    		screenCoordsMeasurements[m][1] = modelToScreenCoordinates(end1);
	    		screenCoordsMeasurements[m][2] = modelToScreenCoordinates(start);
	    		screenCoordsMeasurements[m][3] = modelToScreenCoordinates(end);
	    		screenCoordsMeasurements[m][4] = modelToScreenCoordinates(mid1);
    		}
    		if (measurement.getType() == Measurement.Type.ANGLE) {
	    		double[] start = measurement.getStartPoint();
	    		double[] mid = measurement.getMidPoint();
	    		double[] end = measurement.getEndPoint();
	    		if (!measurement.isFinalized()) {
	    			if (end == null) {
	    				mid = Snap.coords3d != null ? Snap.coords3d : Snap.coords2d;
		    			measurement.setMidPoint(mid);
		    			end = mid;
	    			}
	    			else {
		    			end = Snap.coords3d != null ? Snap.coords3d : Snap.coords2d;
		    			measurement.setEndPoint(end, false);
	    			}
	    		}
	    		double[] start1 = start.clone();
	    		double[] mid1 = mid.clone();
	    		double[] end1 = end.clone();
	    		double[] move = measurement.getMove();
	    		start1[0] += move[0];
	    		start1[1] += move[1];
	    		start1[2] += move[2];
	    		mid1[0] += move[0];
	    		mid1[1] += move[1];
	    		mid1[2] += move[2];
		    	end1[0] += move[0];
		    	end1[1] += move[1];
		    	end1[2] += move[2];
		    	screenCoordsMeasurements[m] = new double[3][];
	    		screenCoordsMeasurements[m][0] = modelToScreenCoordinates(start1);
	    		screenCoordsMeasurements[m][1] = modelToScreenCoordinates(mid1);
	    		screenCoordsMeasurements[m][2] = modelToScreenCoordinates(end1);
	    		
	    		/* draw angle arc */
	    		if (measurement.getEndPoint() != null) {
	    			Matrix d0 = new Matrix(new double[]{start[0]-mid[0], start[1]-mid[1], start[2]-mid[2]}, 3);
	    			d0 = d0.times(2.0*SimLive.ANGLE_LEG_SIZE/width/zoom/d0.normF());
	    			Matrix d1 = new Matrix(new double[]{end[0]-mid[0], end[1]-mid[1], end[2]-mid[2]}, 3);
	    			Matrix norm = d0.crossProduct(d1);
	    			int nrSlices = (int) (measurement.getAngle()/(Math.PI/16))+1;
	    			Matrix R = GeomUtility.getRotationMatrix(measurement.getAngle()/nrSlices, norm.getColumnPackedCopy());
	    			gl2.glPushMatrix();
	    			gl2.glTranslated(mid1[0], mid1[1], mid1[2]);
	    			gl2.glDisable(GL2.GL_LIGHTING);
	    			if (measurement == selectedMeasurement) {
	    				gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
	    			}
	    			else {
	    				gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
	    			}
	        		gl2.glBegin(GL2.GL_LINE_STRIP);
	        		gl2.glVertex3d(0, 0, 0);
	    			gl2.glVertex3dv(d0.getColumnPackedCopy(), 0);
	    			for (int i = 0; i < nrSlices; i++) {
	    				d0 = R.times(d0);
	    				gl2.glVertex3dv(d0.getColumnPackedCopy(), 0);
	    			}
	    			gl2.glVertex3d(0, 0, 0);
	    			gl2.glEnd();
	    			gl2.glEnable(GL2.GL_LIGHTING);
	    			gl2.glPopMatrix();
	    		}
    		}
    	}
    	
    	gl2.glEnable(GL2.GL_LIGHTING);
    	
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		if (SimLive.settings.isShowAxes) {
			drawCoordinateSystem(gl2, glu, width, height, outside, inside, false);
		}
    	
    	/*start of 2d rendering*/
	    gl2.glMatrixMode(GL2.GL_PROJECTION);
	    gl2.glPushMatrix();
	    gl2.glLoadIdentity();
	    gl2.glOrtho(0.0, getSize().x, getSize().y, 0.0, -1.0, 10.0);
	    gl2.glMatrixMode(GL2.GL_MODELVIEW);
	    gl2.glPushMatrix();
	    gl2.glLoadIdentity();
	    gl2.glDisable(GL2.GL_LIGHTING);
	    
	    /* scale */
	    if (SimLive.settings.isShowScale)
		{
	    	double w = SimLive.settings.meshSize*zoom*width/2.0;
	    	double h = SimLive.fontHeight/2.0;
	    	double x1 = width/2-w/2.0;
			double y1 = height-h;
			
	    	gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
	    	gl2.glBegin(GL2.GL_QUADS);
	    	gl2.glVertex2d(x1, y1);
	    	gl2.glVertex2d(x1+w/2.0, y1);
	    	gl2.glVertex2d(x1+w/2.0, y1-h);
	    	gl2.glVertex2d(x1, y1-h);
	    	gl2.glEnd();
	    	gl2.glBegin(GL2.GL_LINE_STRIP);
	    	gl2.glVertex2d(x1, y1);
	    	gl2.glVertex2d(x1+w, y1);
	    	gl2.glVertex2d(x1+w, y1-h);
	    	gl2.glVertex2d(x1, y1-h);
	    	gl2.glVertex2d(x1, y1);
	    	gl2.glEnd();
	    	
	    	String string = SimLive.double2String(SimLive.settings.meshSize)+" "+Units.getLengthUnit();
	    	rendererBold.beginRendering((int) width, (int) height);
	    	rendererBold.setColor(0.0f, 0.0f, 0.0f, 1.0f);
		    double wStr = rendererBold.getBounds(string).getWidth();
		    rendererBold.draw(string, (int) Math.round(width/2-wStr/2), (int) (3*h));
		    rendererBold.endRendering();
		}
	    
	    /* post info */
	    if (SimLive.mode == Mode.RESULTS)
		{
	    	final double gap = SimLive.fontHeight/2.0;
	    	
	    	rendererBold.beginRendering((int) width, (int) height);
	    	rendererBold.setColor(0.0f, 0.0f, 0.0f, 1.0f);
		    
			String string = Step.typeStrings[step.type.ordinal()] + " - \"" + step.name + "\"";
			rendererBold.draw(string, (int) gap, (int) (height-SimLive.fontHeight-gap));
			
			string = "";
			if (step.type == Step.Type.MODAL_ANALYSIS) {
				int i = SimLive.post.getEigenMode();
				double f = SimLive.post.getSolution().getD().get(i, 0)/(2.0*Math.PI);
				string = SimLive.double2String(f) + " " + Units.getFrequencyUnit() + " - ";
			}
			if (SimLive.post.getScalarPlot() == null) {
				string += ScalarPlot.types[0];
			}
			else {
				string += SimLive.post.getScalarPlot().getType();
			}
			rendererBold.draw(string, (int) gap, (int) (height-2*SimLive.fontHeight-gap));
			
			if (step.gravity != GRAVITY.NO_GRAVITY && step.type != Step.Type.MODAL_ANALYSIS) {
				string = "Gravity ";
				string += "xyz".charAt(step.gravity.ordinal()-1);
				string += "-Direction: g=" + SimLive.double2String(step.gValue)+" "+Units.getLengthUnit()+"/"+Units.getTimeUnit()+"\u00B2";
			}
			else {
				string = "No Gravity";
			}
			rendererBold.draw(string, (int) gap, (int) (height-3*SimLive.fontHeight-gap));
			
			string = "Deformation Scaling: " + SimLive.double2String(SimLive.post.getScaling());
			rendererBold.draw(string, (int) gap, (int) (height-4*SimLive.fontHeight-gap));
			
			string = "Increment: " + SimLive.double2String(SimLive.post.getPostIncrementID()) +
					"/" + SimLive.double2String(SimLive.post.getSolution().getNumberOfIncrements());
			rendererBold.draw(string, (int) gap, (int) (height-5*SimLive.fontHeight-gap));
			
			string = "Time: " + SimLive.double2String(time) + " " + Units.getTimeUnit();
			rendererBold.draw(string, (int) gap, (int) (height-6*SimLive.fontHeight-gap));
			
			rendererBold.endRendering();
		}
	    
	    /* color map */
	    if (SimLive.mode == Mode.RESULTS && SimLive.post.getScalarPlot() != null)
		{
	    	final double gap = SimLive.fontHeight/2.0;
	    	rendererBold.beginRendering((int) width, (int) height);
	    	rendererBold.setColor(0.0f, 0.0f, 0.0f, 1.0f);
	    	String string = "Max: " + SimLive.double2String(SimLive.post.getMaxValue())+
					" "+SimLive.post.getScalarPlot().getUnit();
			double wStr = rendererBold.getBounds(string).getWidth();
			rendererBold.draw(string, (int) (width-gap-wStr), (int) (height-SimLive.fontHeight-gap));
			
			string = "Min: " + SimLive.double2String(SimLive.post.getMinValue())+
					" "+SimLive.post.getScalarPlot().getUnit();
			wStr = rendererBold.getBounds(string).getWidth();
			rendererBold.draw(string, (int) (width-gap-wStr), (int) (3*SimLive.fontHeight/2.0));
			
			rendererBold.endRendering();
	    	
	    	int numberOfColors = SimLive.post.getNumberOfColors();
			double x1 = width-2*gap;
			double y1 = height-4*gap-SimLive.fontHeight;
			double h = (height-6*gap-2*SimLive.fontHeight-descent)/(double)numberOfColors;
			float[] palette = SimLive.post.getScalarPlot().getPalette();
			for (int i = 0; i < numberOfColors; i++) {
				gl2.glColor3f(palette[3*i], palette[3*i+1], palette[3*i+2]);
				gl2.glBegin(GL2.GL_QUADS);
		    	gl2.glVertex3d(x1, y1-h*i, 0);
		    	gl2.glVertex3d(x1+gap, y1-h*i, 0);
		    	gl2.glVertex3d(x1+gap, y1-h*(i+1), 0);
		    	gl2.glVertex3d(x1, y1-h*(i+1), 0);
		    	gl2.glEnd();
		    	gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
				gl2.glBegin(GL2.GL_LINE_STRIP);
		    	gl2.glVertex3d(x1, y1-h*i, 1);
		    	gl2.glVertex3d(x1+gap, y1-h*i, 1);
		    	gl2.glVertex3d(x1+gap, y1-h*(i+1), 1);
		    	gl2.glVertex3d(x1, y1-h*(i+1), 1);
		    	gl2.glVertex3d(x1, y1-h*i, 1);
		    	gl2.glEnd();
			}
		}
	    
	    gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
	    
	    /* measurements */
	    for (int m = 0; m < measurements.size(); m++) {
			Measurement measurement = measurements.get(m);
			if (measurement.getType() == Measurement.Type.DISTANCE) {
				String[] stringArray = measurement.getLabel();
				float halfWidth = 0;
				for (int r = 0; r < stringArray.length; r++) {
					halfWidth = (float) Math.max(halfWidth, renderer.getBounds(stringArray[r]).getWidth()/2.0);
				}
				halfWidth += SimLive.LABEL_GAP;
				float halfHeight = SimLive.fontHeight*stringArray.length/2f+descent/2f;
				measurement.setPolygon(new float[]{(float) (screenCoordsMeasurements[m][4][0]),
						(float) (screenCoordsMeasurements[m][4][1])}, halfWidth, halfHeight);
		    	float[][] polygon = measurement.getPolygon();
		    	float depth = (m+1)/(float) measurements.size();
		    	if (selectedMeasurement == measurement) {
		    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    	}
		    	else {
		    		gl2.glColor3fv(SimLive.COLOR_WHITE, 0);
		    	}
		    	gl2.glBegin(GL2.GL_TRIANGLE_STRIP);
				gl2.glVertex3f(polygon[1][0], polygon[1][1], depth);
				gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
				gl2.glVertex3f(polygon[2][0], polygon[2][1], depth);
				gl2.glVertex3f(polygon[3][0], polygon[3][1], depth);
				gl2.glEnd();
				gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
				gl2.glBegin(GL2.GL_LINE_STRIP);
				gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
				gl2.glVertex3f(polygon[1][0], polygon[1][1], depth);
				gl2.glVertex3f(polygon[2][0], polygon[2][1], depth);
				gl2.glVertex3f(polygon[3][0], polygon[3][1], depth);
				gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
				gl2.glEnd();
				if (selectedMeasurement == measurement) {
		    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    	}
				gl2.glBegin(GL2.GL_LINE_STRIP);
				gl2.glVertex3d(screenCoordsMeasurements[m][2][0], screenCoordsMeasurements[m][2][1], 0);
				gl2.glVertex3d(screenCoordsMeasurements[m][0][0], screenCoordsMeasurements[m][0][1], 0);
				gl2.glVertex3d(screenCoordsMeasurements[m][1][0], screenCoordsMeasurements[m][1][1], 0);
				gl2.glVertex3d(screenCoordsMeasurements[m][3][0], screenCoordsMeasurements[m][3][1], 0);
				gl2.glEnd();
				gl2.glPushMatrix();
				gl2.glTranslated(screenCoordsMeasurements[m][0][0], screenCoordsMeasurements[m][0][1], 0);
				glu.gluDisk(inside, 0, SimLive.LABEL_BASE_POINT_SIZE, 8, 1);
				gl2.glPopMatrix();
				gl2.glPushMatrix();
				gl2.glTranslated(screenCoordsMeasurements[m][1][0], screenCoordsMeasurements[m][1][1], 0);
				glu.gluDisk(inside, 0, SimLive.LABEL_BASE_POINT_SIZE, 8, 1);
				gl2.glPopMatrix();
				
				renderer.beginRendering((int) width, (int) height);
			    renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
			    for (int r = 0; r < stringArray.length; r++) {
			    	float wRow = (float) (renderer.getBounds(stringArray[r]).getWidth()/2.0+SimLive.LABEL_GAP);
			    	renderer.draw(stringArray[r], (int) Math.round(polygon[0][0]+SimLive.LABEL_GAP+halfWidth-wRow),
			    			Math.round(height-polygon[0][1]-(r+1)*SimLive.fontHeight));
			    }
			    renderer.endRendering();
			}
			if (measurement.getType() == Measurement.Type.ANGLE) {
				String string = null;
				float[][] polygon = null;
				if (measurement.getEndPoint() != null) {
					string = measurement.getLabel()[0];
					float halfWidth = (float) (renderer.getBounds(string).getWidth()/2.0);
					halfWidth += SimLive.LABEL_GAP;
					float halfHeight = SimLive.fontHeight/2f+descent/2f;
					measurement.setPolygon(new float[]{(float) screenCoordsMeasurements[m][1][0],
							(float) screenCoordsMeasurements[m][1][1]}, halfWidth, halfHeight);
			    	polygon = measurement.getPolygon();
			    	float depth = (m+1)/(float) measurements.size();
			    	if (selectedMeasurement == measurement) {
			    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
			    	}
			    	else {
			    		gl2.glColor3fv(SimLive.COLOR_WHITE, 0);
			    	}
			    	gl2.glBegin(GL2.GL_TRIANGLE_STRIP);
					gl2.glVertex3f(polygon[1][0], polygon[1][1], depth);
					gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
					gl2.glVertex3f(polygon[2][0], polygon[2][1], depth);
					gl2.glVertex3f(polygon[3][0], polygon[3][1], depth);
					gl2.glEnd();
					gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
					gl2.glBegin(GL2.GL_LINE_STRIP);
					gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
					gl2.glVertex3f(polygon[1][0], polygon[1][1], depth);
					gl2.glVertex3f(polygon[2][0], polygon[2][1], depth);
					gl2.glVertex3f(polygon[3][0], polygon[3][1], depth);
					gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
					gl2.glEnd();
				}
				if (selectedMeasurement == measurement) {
		    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    	}
				gl2.glBegin(GL2.GL_LINE_STRIP);
				gl2.glVertex3d(screenCoordsMeasurements[m][0][0], screenCoordsMeasurements[m][0][1], 0);
				gl2.glVertex3d(screenCoordsMeasurements[m][1][0], screenCoordsMeasurements[m][1][1], 0);
				gl2.glVertex3d(screenCoordsMeasurements[m][2][0], screenCoordsMeasurements[m][2][1], 0);
				gl2.glEnd();
				gl2.glPushMatrix();
				gl2.glTranslated(screenCoordsMeasurements[m][0][0], screenCoordsMeasurements[m][0][1], 0);
				glu.gluDisk(inside, 0, SimLive.LABEL_BASE_POINT_SIZE, 8, 1);
				gl2.glPopMatrix();
				gl2.glPushMatrix();
				gl2.glTranslated(screenCoordsMeasurements[m][1][0], screenCoordsMeasurements[m][1][1], 0);
				glu.gluDisk(inside, 0, SimLive.LABEL_BASE_POINT_SIZE, 8, 1);
				gl2.glPopMatrix();
				gl2.glPushMatrix();
				gl2.glTranslated(screenCoordsMeasurements[m][2][0], screenCoordsMeasurements[m][2][1], 0);
				glu.gluDisk(inside, 0, SimLive.LABEL_BASE_POINT_SIZE, 8, 1);
				gl2.glPopMatrix();
				
				if (measurement.getEndPoint() != null) {
					renderer.beginRendering((int) width, (int) height);
				    renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
				    renderer.draw(string, (int) Math.round(polygon[0][0]+SimLive.LABEL_GAP),
			    			Math.round(height-polygon[0][1]-SimLive.fontHeight));
				    renderer.endRendering();
				}
			}
	    }
		
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		
		/* labels */
	    for (int l = 0; l < labels.size(); l++) {
			labels.get(l).updateText();
	    	String[] string = labels.get(l).getRows();
	    	float maxWidth = 0, rows = 0;
	    	for (int r = 0; r < string.length; r++) if (string[r] != null) {
	    		maxWidth = (float) Math.max(maxWidth, renderer.getBounds(string[r]).getWidth());
	    		rows++;
	    	}
	    	maxWidth += 2*SimLive.LABEL_GAP;
	    	float halfHeight = SimLive.fontHeight*rows/2f+descent/2f;
	    	labels.get(l).updateLabel(screenCoordsLabels[l], maxWidth, halfHeight);
	    	float[][] polygon = labels.get(l).getPolygon();
	    	float depth = (l+1)/(float) labels.size();
	    	if (selectedLabel == labels.get(l)) {
	    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
	    	}
	    	else {
	    		gl2.glColor3fv(SimLive.COLOR_WHITE, 0);
	    	}
	    	gl2.glBegin(GL2.GL_TRIANGLE_STRIP);
			gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
			gl2.glVertex3f(polygon[4][0], polygon[4][1], depth);
			gl2.glVertex3f(polygon[1][0], polygon[1][1], depth);
			gl2.glVertex3f(polygon[3][0], polygon[3][1], depth);
			gl2.glVertex3f(polygon[2][0], polygon[2][1], depth);
			gl2.glEnd();
			gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			gl2.glBegin(GL2.GL_LINE_STRIP);
			gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
			gl2.glVertex3f(polygon[1][0], polygon[1][1], depth);
			gl2.glVertex3f(polygon[2][0], polygon[2][1], depth);
			gl2.glVertex3f(polygon[3][0], polygon[3][1], depth);
			gl2.glVertex3f(polygon[4][0], polygon[4][1], depth);
			gl2.glVertex3f(polygon[0][0], polygon[0][1], depth);
			gl2.glEnd();
			if (selectedLabel == labels.get(l)) {
	    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
	    	}
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(polygon[0][0], polygon[0][1], 0);
			gl2.glVertex3d(screenCoordsLabels[l][0], screenCoordsLabels[l][1], 0);
			gl2.glEnd();
			gl2.glPushMatrix();
			gl2.glTranslated(screenCoordsLabels[l][0], screenCoordsLabels[l][1], 0);
			glu.gluDisk(inside, 0, SimLive.LABEL_BASE_POINT_SIZE, 8, 1);
			gl2.glPopMatrix();
			
			float[] textPos;
			if (!labels.get(l).isOnRightHandSide()) {
				textPos = polygon[3];
			}
			else {
				textPos = polygon[1];
			}
			renderer.beginRendering((int) width, (int) height);
		    renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
		    for (int r = 0; r < string.length; r++) if (string[r] != null) {
		    	float wRow = (float) renderer.getBounds(string[r]).getWidth();
		    	renderer.draw(string[r], Math.round(textPos[0]+(maxWidth-wRow)/2),
		    			Math.round(height-textPos[1]-(r+1)*SimLive.fontHeight));
		    }
		    renderer.endRendering();
	    }
		
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		
		/* labelAtMousePos */
		if (labelAtMousePos != null)
		{
			renderer.beginRendering((int) width, (int) height);
		    String[] string = labelAtMousePos.getRows();
		    float yShift = SimLive.fontHeight;
		    for (int r = string.length-1; r > -1; r--) if (string[r] != null) {
		    	float wRow = (float) renderer.getBounds(string[r]).getWidth();
		    	int x = Math.round(mousePos[0]+(-wRow)/2);
		    	int y = Math.round(height-mousePos[1]+yShift);
		    	yShift += SimLive.fontHeight;
		    	renderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
		    	renderer.draw(string[r], x+1, y-1);
		    	renderer.setColor(SimLive.COLOR_HIGHLIGHT[0], SimLive.COLOR_HIGHLIGHT[1], SimLive.COLOR_HIGHLIGHT[2], 1.0f);
		    	renderer.draw(string[r], x, y);
		    }
		    renderer.endRendering();
		}
		
		/* focus point */
		if (focusPoint != null || (isMouseDragged && mouseButton > 1) || rotAroundAxis)
		{
			double size = SimLive.UNIT_SIZE*20;
			gl2.glLineWidth(5);
			gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3d(width/2-size, height/2, 0);
			gl2.glVertex3d(width/2-size/4, height/2, 0);
			gl2.glVertex3d(width/2+size, height/2, 0);
			gl2.glVertex3d(width/2+size/4, height/2, 0);
			gl2.glVertex3d(width/2, height/2-size, 0);
			gl2.glVertex3d(width/2, height/2-size/4, 0);
			gl2.glVertex3d(width/2, height/2+size, 0);
			gl2.glVertex3d(width/2, height/2+size/4, 0);
			gl2.glEnd();
			gl2.glLineWidth(3);
		    gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3d(width/2-size+1, height/2, 1);
			gl2.glVertex3d(width/2-size/4-1, height/2, 1);
			gl2.glVertex3d(width/2+size-1, height/2, 1);
			gl2.glVertex3d(width/2+size/4+1, height/2, 1);
			gl2.glVertex3d(width/2, height/2-size+1, 1);
			gl2.glVertex3d(width/2, height/2-size/4-1, 1);
			gl2.glVertex3d(width/2, height/2+size-1, 1);
			gl2.glVertex3d(width/2, height/2+size/4+1, 1);
			gl2.glEnd();
			gl2.glLineWidth(1);
		}
		
	    /* selection box */
		if (selectionBox != null)
		{
			gl2.glEnable(GL2.GL_LINE_STIPPLE);
			gl2.glLineStipple(3, (short) 0x7777);
			gl2.glLineWidth(3);
			gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			gl2.glBegin(GL2.GL_LINE_STRIP);
			gl2.glVertex3d(selectionBox.x, selectionBox.y, 0);
			gl2.glVertex3d(selectionBox.x+selectionBox.width, selectionBox.y, 0);
			gl2.glVertex3d(selectionBox.x+selectionBox.width, selectionBox.y+selectionBox.height, 0);
			gl2.glVertex3d(selectionBox.x, selectionBox.y+selectionBox.height, 0);
			gl2.glVertex3d(selectionBox.x, selectionBox.y, 0);
		    gl2.glEnd();
		    gl2.glDisable(GL2.GL_LINE_STIPPLE);
		    gl2.glLineWidth(1);
		    gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    gl2.glBegin(GL2.GL_LINE_STRIP);
			gl2.glVertex3d(selectionBox.x, selectionBox.y, 1);
			gl2.glVertex3d(selectionBox.x+selectionBox.width, selectionBox.y, 1);
			gl2.glVertex3d(selectionBox.x+selectionBox.width, selectionBox.y+selectionBox.height, 1);
			gl2.glVertex3d(selectionBox.x, selectionBox.y+selectionBox.height, 1);
			gl2.glVertex3d(selectionBox.x, selectionBox.y, 1);
		    gl2.glEnd();
		}
		gl2.glEnable(GL2.GL_LIGHTING);
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPopMatrix();
		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPopMatrix();
		
		gl2.glFlush();
	}
	
	private void renderSets(GL2 gl2, GLU glu, ArrayList<Set> connectSets0, ArrayList<Set> connectSets1,
			float[] projection, float width, GLUquadric inside, GLUquadric outside,
			double lineElementRadius, double scaling, Set.View view, double zoom,
			ArrayList<Set> sets, ArrayList<Object> objects) {
		
		double arrowSize = SimLive.ORIENTATION_SIZE/width/zoom;
		
		/* sets */
	    gl2.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 0);
	    boolean selected = sets == selectedSets;
    	for (int s = 0; s < sets.size(); s++) {
	    	Set set = sets.get(s);
	    	if (set != null && (set.view == view || selected) ||
	    			connectSets0.contains(set) || connectSets1.contains(set)) {
	    		float[] modelview = new float[16];
			    gl2.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
		    	{
			    	float[] uniColor = getUniColorOrSetMaterial(gl2, set, connectSets0, connectSets1);
					for (int e = 0; e < set.getElements().size(); e++) {
						Element element = set.getElements().get(e);
						if (element.isPlaneElement()) {
							renderPlaneElement(gl2, element, projection, modelview, uniColor, selected);
							if (SimLive.settings.isShowOrientations) {
								renderPlaneElementOrientation(gl2, glu, element, arrowSize, uniColor, inside, outside, null);
							}
						}
						else {
							renderLineElementAndPointMass(gl2, element, connectSets0, connectSets1, width, glu, outside, inside,
					    			lineElementRadius, scaling, zoom, uniColor);
						}
			    	}
		    	}
		    	
		    	/* principal vectors */
				if (SimLive.mode == Mode.RESULTS && SimLive.post.getScalarPlot() != null) {
					gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_WHITE, 0);
					gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);
					renderPrincipalVectors(gl2, glu, outside, inside, width, view, zoom, set);
				}
				
		    	/* curve plots */
				if (SimLive.mode == Mode.RESULTS && SimLive.post.getScalarPlot() != null && SimLive.post.getScalarPlot().isCurvePlot()) {
					for (int e = 0; e < set.getElements().size(); e++) {
						Element element = set.getElements().get(e);
						if (element.isLineElement() && SimLive.post.getScalarPlot().hasValue(element, SimLive.post.getPostIncrement())) {
							renderCurvePlot(gl2, (LineElement) element, width, scaling, SimLive.post.getScalarPlot(), lineElementRadius);
						}
			    	}
				}
	    	}
		}
	}
	
	private void renderPart3d(GL2 gl2, Part3d part3d, double scaling, boolean selected, boolean connected) {
		
    	double[][] vertexCoords = part3d.getVertexCoords();
    	double[][] normals = part3d.getNormals();
    	
    	/*if (render != Render.FILL || selected || connected)*/ {
    		double[] viewDir = new double[3];
    		if (!perspective) {
    			viewDir = R0.times(R).getMatrix(0, 2, 2, 2).getColumnPackedCopy();
    		}
    		gl2.glDisable(GL2.GL_LIGHTING);
    		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
	    	for (int f = 0; f < part3d.getNrFacets(); f++) {
	    		int[] facet = part3d.getFacet(f).getIndices();
	    		if (perspective) {
	    			viewDir[0] = cameraRefPos[0]-vertexCoords[facet[0]][0];
	    			viewDir[1] = cameraRefPos[1]-vertexCoords[facet[0]][1];
	    			viewDir[2] = cameraRefPos[2]-vertexCoords[facet[0]][2];
	    		}
	    		for (int i = 0; i < 3; i++) {
	    			if (part3d.render != Render.FILL || part3d.isEdge(f, i, viewDir)) {
	    				int j = (i+1)%3;
		    			gl2.glBegin(GL2.GL_LINES);
				    	gl2.glVertex3d(vertexCoords[facet[i]][0], vertexCoords[facet[i]][1], vertexCoords[facet[i]][2]);
			    		gl2.glVertex3d(vertexCoords[facet[j]][0], vertexCoords[facet[j]][1], vertexCoords[facet[j]][2]);
			    		gl2.glEnd();
	    			}
	    		}
	    	}
    	}
    	
    	if (selected) {
    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
    	}
    	else if (connected) {
    		gl2.glColor3fv(SimLive.COLOR_RED, 0);
    	}
    	else {
    		gl2.glEnable(GL2.GL_LIGHTING);
    	}
	    
    	if (selected || connected || part3d.render != Render.WIREFRAME) {
	    	int colorID = -1;
			gl2.glBegin(GL2.GL_TRIANGLES);
			for (int f = 0; f < part3d.getNrFacets(); f++) {
	    		if (!selected && !connected && colorID != part3d.getFacet(f).getColorID()) {
	    			colorID = part3d.getFacet(f).getColorID();
	    			gl2.glEnd();
	    			gl2.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE,
		    				SimLive.model.getPart3dColors().get(colorID).getKd(), 0);
		    		gl2.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR,
		    				SimLive.model.getPart3dColors().get(colorID).getKs(), 0);
		    		gl2.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS,
		    				SimLive.model.getPart3dColors().get(colorID).getShininess());
		    		gl2.glBegin(GL2.GL_TRIANGLES);
	    		}
	    		int[] indices = part3d.getFacet(f).getIndices();
	    		for (int i = 0; i < 3; i++) {
	    			gl2.glNormal3dv(normals[f*3+i], 0);
		    		gl2.glVertex3dv(vertexCoords[indices[i]], 0);
				}
	    	}
			gl2.glEnd();
    	}
	}
	
	private void renderPlaneElement(GL2 gl2, Element element, float[] projection, float[] modelview, float[] uniColor, boolean selected) {
		int[] elemNodes = element.getElementNodes();
		double[][] coords = new double[elemNodes.length][];
		for (int i = 0; i < elemNodes.length; i++) {
			coords[i] = getCoordsWithScaledDisp(elemNodes[i]);
		}
		Matrix norm0 = ((PlaneElement) element).getR0().getMatrix(0, 2, 2, 2);
		double[] norm = new double[3];
		double[][] Rr = View.Rr[element.getID()];
		norm[0] = Rr[0][2];
		norm[1] = Rr[1][2];
		norm[2] = Rr[2][2];
		double[][] top = new double[elemNodes.length][3];
		double[][] bottom = new double[elemNodes.length][3];
		double halfThickness = ((PlaneElement) element).getThickness()/2.0;
		for (int i = 0; i < elemNodes.length; i++) {
			double[] nodeNorm = null;
			if (nodeNormals[elemNodes[i]] != null) {
				double scal = norm[0]*nodeNormals[elemNodes[i]][0]+norm[1]*nodeNormals[elemNodes[i]][1]+norm[2]*nodeNormals[elemNodes[i]][2];
				if (scal != 0) {
					nodeNorm = nodeNormals[elemNodes[i]].clone();
					if (scal < 0) {
						nodeNorm[0] = -nodeNorm[0];
						nodeNorm[1] = -nodeNorm[1];
						nodeNorm[2] = -nodeNorm[2];
					}
				}
			}
			if (nodeNorm == null) {
				nodeNorm = norm.clone();
			}
			top[i][0] = nodeNorm[0]*halfThickness;
			top[i][1] = nodeNorm[1]*halfThickness;
			top[i][2] = nodeNorm[2]*halfThickness;
			bottom[i][0] = nodeNorm[0]*(-halfThickness);
			bottom[i][1] = nodeNorm[1]*(-halfThickness);
			bottom[i][2] = nodeNorm[2]*(-halfThickness);
		}
		double[][] edgeNormal = new double[elemNodes.length][3];
		for (int i = 0; i < elemNodes.length; i++) {
    		int j = (i+1)%elemNodes.length;
    		double[] diff = new double[3];
			diff[0] = coords[j][0] - coords[i][0];
			diff[1] = coords[j][1] - coords[i][1];
			diff[2] = coords[j][2] - coords[i][2];
			edgeNormal[i][0] = diff[1]*top[i][2]-diff[2]*top[i][1];
			edgeNormal[i][1] = diff[2]*top[i][0]-diff[0]*top[i][2];
			edgeNormal[i][2] = diff[0]*top[i][1]-diff[1]*top[i][0];
		}
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
    	for (int i = 0; i < elemNodes.length; i++) {
    		int j = (i+1)%elemNodes.length;
			boolean outline = SimLive.contains(outlineEdge[elemNodes[i]], elemNodes[j]) ||
					SimLive.contains(innerEdge[elemNodes[i]], elemNodes[j]);
    		double[] p0 = new double[]{coords[i][0]+top[i][0], coords[i][1]+top[i][1], coords[i][2]+top[i][2]};
    		double[] p1 = new double[]{coords[j][0]+top[j][0], coords[j][1]+top[j][1], coords[j][2]+top[j][2]};
    		double[] p2 = new double[]{coords[i][0]+bottom[i][0], coords[i][1]+bottom[i][1], coords[i][2]+bottom[i][2]};
    		double[] p3 = new double[]{coords[j][0]+bottom[j][0], coords[j][1]+bottom[j][1], coords[j][2]+bottom[j][2]};
			
			if (SimLive.settings.isShowEdges || outline) {
				gl2.glBegin(GL2.GL_LINES);
				gl2.glVertex3dv(p0, 0);
				gl2.glVertex3dv(p1, 0);
				gl2.glVertex3dv(p2, 0);
				gl2.glVertex3dv(p3, 0);
				if (SimLive.settings.isShowEdges || isCornerNode[elemNodes[i]]) {
					gl2.glVertex3dv(p0, 0);
					gl2.glVertex3dv(p2, 0);
				}
				/*if (Sim2d.settings.isShowEdges || isCornerNode[elemNodes[(i+1)%elemNodes.length]]) {
					gl2.glVertex3d(coords[(i+1)%elemNodes.length][0]+bottom[0], coords[(i+1)%elemNodes.length][1]+bottom[1], coords[(i+1)%elemNodes.length][2]+bottom[2]);
					gl2.glVertex3d(coords[(i+1)%elemNodes.length][0]+bottom[0], coords[(i+1)%elemNodes.length][1]+bottom[1], coords[(i+1)%elemNodes.length][2]+bottom[2]);
				}*/
				gl2.glEnd();				
			}
    		
    		if (animation == null) {
    			if (pElementScreenPolys[element.getID()] == null) {
    				pElementScreenPolys[element.getID()] = new double[elemNodes.length*4][];
    			}
    			pElementScreenPolys[element.getID()][i*4] = modelToScreenCoordinates(p0);
    			pElementScreenPolys[element.getID()][i*4+1] = modelToScreenCoordinates(p1);
    			pElementScreenPolys[element.getID()][i*4+2] = modelToScreenCoordinates(p3);
    			pElementScreenPolys[element.getID()][i*4+3] = modelToScreenCoordinates(p2);
    		}
    	}
    	if (uniColor != null) {
			gl2.glColor3fv(uniColor, 0);
		}
		else {
			gl2.glEnable(GL2.GL_LIGHTING);
		}
		if (SimLive.mode == Mode.RESULTS && SimLive.post.getScalarPlot() != null &&
				SimLive.post.getScalarPlot().hasValue(element, SimLive.post.getPostIncrement()) && uniColor == null) {
			
			DoubleBuffer vertexBuffer = null;
			IntBuffer indexBufferQuad = null;
			IntBuffer indexBufferTri = null;
			DoubleBuffer valueBuffer = null;
			int length = elemNodes.length*2;
			if (element.getType() == Element.Type.QUAD) {
				vertexBuffer = Buffers.newDirectDoubleBuffer((length+2)*3);
				valueBuffer = Buffers.newDirectDoubleBuffer(length+2);
			}
			if (element.getType() == Element.Type.TRI) {
				vertexBuffer = Buffers.newDirectDoubleBuffer(length*3);
				valueBuffer = Buffers.newDirectDoubleBuffer(length);
			}
			if (element.getType() == Element.Type.QUAD) {
				double[] center = new double[3];
				center[0] = (coords[0][0]+coords[1][0]+coords[2][0]+coords[3][0])/4.0;
				center[1] = (coords[0][1]+coords[1][1]+coords[2][1]+coords[3][1])/4.0;
				center[2] = (coords[0][2]+coords[1][2]+coords[2][2]+coords[3][2])/4.0;
				vertexBuffer.put(center[0]+norm[0]*halfThickness);
				vertexBuffer.put(center[1]+norm[1]*halfThickness);
				vertexBuffer.put(center[2]+norm[2]*halfThickness);
				vertexBuffer.put(center[0]-norm[0]*halfThickness);
				vertexBuffer.put(center[1]-norm[1]*halfThickness);
				vertexBuffer.put(center[2]-norm[2]*halfThickness);
				double value = 0.0;
				for (int i = 0; i < 4; i++) {
					value += SimLive.post.getScalarPlot().getValueAtNode(elemNodes[i], SimLive.post.getPostIncrementID());
				}
				valueBuffer.put(value/4.0);
				valueBuffer.put(value/4.0);
			}
			for (int i = 0; i < elemNodes.length; i++) {
				vertexBuffer.put(coords[i][0]+top[i][0]);
				vertexBuffer.put(coords[i][1]+top[i][1]);
				vertexBuffer.put(coords[i][2]+top[i][2]);
				valueBuffer.put(SimLive.post.getScalarPlot().getValueAtNode(elemNodes[i], SimLive.post.getPostIncrementID()));
			}
			for (int i = 0; i < elemNodes.length; i++) {
				vertexBuffer.put(coords[i][0]+bottom[i][0]);
				vertexBuffer.put(coords[i][1]+bottom[i][1]);
				vertexBuffer.put(coords[i][2]+bottom[i][2]);
				valueBuffer.put(SimLive.post.getScalarPlot().getValueAtNode(elemNodes[i], SimLive.post.getPostIncrementID()));
			}
			if (element.getType() == Element.Type.QUAD) {
				/* front and back with four triangles each */
				indexBufferTri = Buffers.newDirectIntBuffer(elemNodes.length*6);
				indexBufferTri.put(new int[]{0, 2, 3, 0, 3, 4, 0, 4, 5, 0, 5, 2, 1, 7, 6, 1, 8, 7, 1, 9, 8, 1, 6, 9});
				/* edges with one quad each */
				indexBufferQuad = Buffers.newDirectIntBuffer(elemNodes.length*4);
				indexBufferQuad.put(new int[]{2, 6, 7, 3, 7, 8, 4, 3, 8, 9, 5, 4, 2, 5, 9, 6});
			}
			if (element.getType() == Element.Type.TRI) {
				/* front and back with one triangle each */
				indexBufferTri = Buffers.newDirectIntBuffer(elemNodes.length*2);
				indexBufferTri.put(new int[]{0, 1, 2, 4, 3, 5});
				/* edges with one quad each */
				indexBufferQuad = Buffers.newDirectIntBuffer(elemNodes.length*4);
				indexBufferQuad.put(new int[]{0, 3, 4, 1, 0, 2, 5, 3, 1, 4, 5, 2});
			}
			
			FloatBuffer palette = FloatBuffer.wrap(SimLive.post.getScalarPlot().getPalette());
			
			gl2.glUseProgram(shaderProgram);
			
			int positionAttribute = gl2.glGetAttribLocation(shaderProgram, "inPosition");
			int valueAttribute = gl2.glGetAttribLocation(shaderProgram, "inValue");
			
			int minLoc = gl2.glGetUniformLocation(shaderProgram, "min");
			int maxLoc = gl2.glGetUniformLocation(shaderProgram, "max");
			int nrColorsLoc = gl2.glGetUniformLocation(shaderProgram, "nrColors");
			int paletteLoc = gl2.glGetUniformLocation(shaderProgram, "palette");
			gl2.glUniform1f(minLoc, (float) SimLive.post.getMinValue());
			gl2.glUniform1f(maxLoc, (float) SimLive.post.getMaxValue());
			gl2.glUniform1i(nrColorsLoc, SimLive.post.getNumberOfColors());
			gl2.glUniform3fv(paletteLoc, SimLive.post.getNumberOfColors(), palette);
			gl2.glEnableVertexAttribArray(positionAttribute);
			gl2.glEnableVertexAttribArray(valueAttribute);
			gl2.glVertexAttribPointer(positionAttribute, 3, GL2.GL_DOUBLE, false, 0, vertexBuffer.rewind());
			gl2.glVertexAttribPointer(valueAttribute, 1, GL2.GL_DOUBLE, false, 0, valueBuffer.rewind());
			int projectionLoc = gl2.glGetUniformLocation(shaderProgram, "projection");
		    int modelviewLoc = gl2.glGetUniformLocation(shaderProgram, "modelview");
		    gl2.glUniformMatrix4fv(projectionLoc, 1, false, projection, 0);
		    gl2.glUniformMatrix4fv(modelviewLoc, 1, false, modelview, 0);
		    
		    if (element.getType() == Element.Type.QUAD) {
		    	gl2.glDrawElements(GL2.GL_TRIANGLES, elemNodes.length*6, GL2.GL_UNSIGNED_INT, indexBufferTri.rewind());
		    	gl2.glDrawElements(GL2.GL_QUADS, elemNodes.length*4, GL2.GL_UNSIGNED_INT, indexBufferQuad.rewind());
		    }
		    if (element.getType() == Element.Type.TRI) {
		    	gl2.glDrawElements(GL2.GL_TRIANGLES, elemNodes.length*2, GL2.GL_UNSIGNED_INT, indexBufferTri.rewind());
		    	gl2.glDrawElements(GL2.GL_QUADS, elemNodes.length*4, GL2.GL_UNSIGNED_INT, indexBufferQuad.rewind());
		    }
		    
			gl2.glDisableVertexAttribArray(positionAttribute);
			gl2.glDisableVertexAttribArray(valueAttribute);
			gl2.glUseProgram(0);
		}
		else {
			if (element.getType() == Element.Type.QUAD) {
				gl2.glBegin(GL2.GL_TRIANGLES);
				int[] index = {0, 1, 3, 1, 2, 3};
				for (int j = 0; j < index.length; j++) {
					int i = index[j];
					if (isOutlineNode[elemNodes[i]]) {
						for (int k = 0; k < outlineNormals0[elemNodes[i]].length; k++) {
							if (outlineNormals0[elemNodes[i]][k].dotProduct(norm0) > SimLive.COS_ANGLE_INNER_EDGE) {
								gl2.glNormal3d(outlineNormals[elemNodes[i]][k][0], outlineNormals[elemNodes[i]][k][1], outlineNormals[elemNodes[i]][k][2]);
							}
						}
					}
					else {
						gl2.glNormal3d(top[i][0], top[i][1], top[i][2]);
					}
					gl2.glVertex3d(coords[i][0]+top[i][0], coords[i][1]+top[i][1], coords[i][2]+top[i][2]);
				}
				for (int j = index.length-1; j > -1; j--) {
					int i = index[j];
					if (isOutlineNode[elemNodes[i]]) {
						for (int k = 0; k < outlineNormals0[elemNodes[i]].length; k++) {
							if (outlineNormals0[elemNodes[i]][k].dotProduct(norm0) > SimLive.COS_ANGLE_INNER_EDGE) {
								gl2.glNormal3d(-outlineNormals[elemNodes[i]][k][0], -outlineNormals[elemNodes[i]][k][1], -outlineNormals[elemNodes[i]][k][2]);
							}
						}
					}
					else {
						gl2.glNormal3d(bottom[i][0], bottom[i][1], bottom[i][2]);
					}
					gl2.glVertex3d(coords[i][0]+bottom[i][0], coords[i][1]+bottom[i][1], coords[i][2]+bottom[i][2]);
				}
				gl2.glEnd();
			}
			if (element.getType() == Element.Type.TRI) {
				gl2.glBegin(GL2.GL_TRIANGLES);
				for (int i = 0; i < elemNodes.length; i++) {
					if (isOutlineNode[elemNodes[i]]) {
						for (int k = 0; k < outlineNormals0[elemNodes[i]].length; k++) {
							if (outlineNormals0[elemNodes[i]][k].dotProduct(norm0) > SimLive.COS_ANGLE_INNER_EDGE) {
								gl2.glNormal3d(outlineNormals[elemNodes[i]][k][0], outlineNormals[elemNodes[i]][k][1], outlineNormals[elemNodes[i]][k][2]);
							}
						}
					}
					else {
						gl2.glNormal3d(top[i][0], top[i][1], top[i][2]);
					}
					gl2.glVertex3d(coords[i][0]+top[i][0], coords[i][1]+top[i][1], coords[i][2]+top[i][2]);
				}
				for (int i = elemNodes.length-1; i > -1; i--) {
					if (isOutlineNode[elemNodes[i]]) {
						for (int k = 0; k < outlineNormals0[elemNodes[i]].length; k++) {
							if (outlineNormals0[elemNodes[i]][k].dotProduct(norm0) > SimLive.COS_ANGLE_INNER_EDGE) {
								gl2.glNormal3d(-outlineNormals[elemNodes[i]][k][0], -outlineNormals[elemNodes[i]][k][1], -outlineNormals[elemNodes[i]][k][2]);
							}
						}
					}
					else {
						gl2.glNormal3d(bottom[i][0], bottom[i][1], bottom[i][2]);
					}
					gl2.glVertex3d(coords[i][0]+bottom[i][0], coords[i][1]+bottom[i][1], coords[i][2]+bottom[i][2]);
				}
				gl2.glEnd();
			}
			gl2.glBegin(GL2.GL_QUADS);
			for (int i = 0; i < elemNodes.length; i++) {
				int j = (i+1)%elemNodes.length;
				if (SimLive.contains(outlineEdge[elemNodes[i]], elemNodes[j]) ||
						SimLive.contains(innerEdge[elemNodes[i]], elemNodes[j]) || selected) {
					gl2.glNormal3d(edgeNormal[i][0], edgeNormal[i][1], edgeNormal[i][2]);
					gl2.glVertex3d(coords[i][0]+top[i][0], coords[i][1]+top[i][1], coords[i][2]+top[i][2]);
					gl2.glVertex3d(coords[i][0]+bottom[i][0], coords[i][1]+bottom[i][1], coords[i][2]+bottom[i][2]);
					gl2.glVertex3d(coords[j][0]+bottom[j][0], coords[j][1]+bottom[j][1], coords[j][2]+bottom[j][2]);
					gl2.glVertex3d(coords[j][0]+top[j][0], coords[j][1]+top[j][1], coords[j][2]+top[j][2]);					
				}
			}
			gl2.glEnd();
			
			/*{
				//normals debug output
				gl2.glDisable(GL2.GL_LIGHTING);
				gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
				double d = 10.0;
		    	for (int i = 0; i < elemNodes.length; i++) {
		    		gl2.glBegin(GL2.GL_LINES);
					gl2.glVertex3d(coords[i][0], coords[i][1], coords[i][2]);
		    		if (isOutlineNode[elemNodes[i]]) {
						for (int k = 0; k < outlineNormals0[elemNodes[i]].length; k++) {
							if (outlineNormals0[elemNodes[i]][k].dotProduct(norm0) > SimLive.COS_ANGLE_INNER_EDGE) {
								gl2.glVertex3d(coords[i][0]+d*outlineNormals[elemNodes[i]][k][0],
										coords[i][1]+d*outlineNormals[elemNodes[i]][k][1],
										coords[i][2]+d*outlineNormals[elemNodes[i]][k][2]);
							}
						}
					}
					else {
						gl2.glVertex3d(coords[i][0]+d*top[i][0], coords[i][1]+d*top[i][1], coords[i][2]+d*top[i][2]);
					}
		    		gl2.glEnd();
		    	}
		    	gl2.glEnable(GL2.GL_LIGHTING);
			}*/
		}
		gl2.glEnable(GL2.GL_LIGHTING);
	}
	
	private void renderRigidPlaneElements(GL2 gl2, ContactPair contactPair, float[] projection, float[] modelview, float[] uniColor,
			double arrowSize, GLU glu, GLUquadric inside, GLUquadric outside, ArrayList<Object> objects) {
		for (int s = 0; s < contactPair.getMasterSets().size(); s++) {			
			Set set = contactPair.getMasterSets().get(s);
			if (set.view != Set.View.HIDDEN || (SimLive.mode == SimLive.Mode.CONTACTS &&
		    		!objects.isEmpty() && objects.get(0) == contactPair)) {
				for (int e = 0; e < set.getElements().size(); e++) {
					Element element = set.getElements().get(e);
					int[] elemNodes = element.getElementNodes();
					double[][] coords = new double[elemNodes.length][];
					for (int i = 0; i < elemNodes.length; i++) {
						coords[i] = contactPair.getRigidNodes().get(elemNodes[i]).getCoords();
					}			
					gl2.glDisable(GL2.GL_LIGHTING);
					gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			    	for (int i = 0; i < elemNodes.length; i++) {
			    		int j = (i+1)%elemNodes.length;
						gl2.glBegin(GL2.GL_LINES);
						gl2.glVertex3d(coords[i][0], coords[i][1], coords[i][2]);
						gl2.glVertex3d(coords[j][0], coords[j][1], coords[j][2]);
						gl2.glEnd();
			    	}
			    	gl2.glColor3fv(uniColor, 0);
					
			    	{
						if (element.getType() == Element.Type.QUAD) {
							gl2.glBegin(GL2.GL_TRIANGLES);
							int[] index = {0, 1, 3, 1, 2, 3};
							for (int j = 0; j < index.length; j++) {
								int i = index[j];
								gl2.glVertex3d(coords[i][0], coords[i][1], coords[i][2]);
							}
							gl2.glEnd();
						}
						if (element.getType() == Element.Type.TRI) {
							gl2.glBegin(GL2.GL_TRIANGLES);
							for (int i = 0; i < elemNodes.length; i++) {
								gl2.glVertex3d(coords[i][0], coords[i][1], coords[i][2]);
							}
							gl2.glEnd();
						}
					}
					gl2.glEnable(GL2.GL_LIGHTING);
					
					if (SimLive.settings.isShowOrientations) {
						renderPlaneElementOrientation(gl2, glu, element, arrowSize, uniColor, inside, outside, contactPair);
					}
				}
			}
		}
	}
	
	private void renderPlaneElementOrientation(GL2 gl2, GLU glu, Element element, double arrowSize, float[] uniColor,
			GLUquadric inside, GLUquadric outside, ContactPair contactPair) {
		double halfThickness = contactPair != null ? 0 : ((PlaneElement) element).getThickness()/2.0;
		double[] center = new double[3];
		int[] elemNodes = element.getElementNodes();
		double[][] coords = new double[elemNodes.length][];
		for (int i = 0; i < elemNodes.length; i++) {
			coords[i] = contactPair != null ?
					contactPair.getRigidNodes().get(elemNodes[i]).getCoords() :
					View.getCoordsWithScaledDisp(elemNodes[i]);
			center[0] += coords[i][0];
			center[1] += coords[i][1];
			center[2] += coords[i][2];
		}
		center[0] /= elemNodes.length;
		center[1] /= elemNodes.length;
		center[2] /= elemNodes.length;
		Matrix R = contactPair != null ? ((PlaneElement) element).getR0() : new Matrix(Rr[element.getID()]);
		if (elemNodes.length > 3) {
			double[] n = new double[]{R.get(0, 2), R.get(1, 2), R.get(2, 2)};
			center = GeomUtility.getIntersectionLinePlane(center, n, coords[0], coords[1], coords[3]);
			Matrix Rt = R.transpose();
			double[] c0 = Rt.times(new Matrix(coords[0], 3)).getColumnPackedCopy();
			double[] c1 = Rt.times(new Matrix(coords[1], 3)).getColumnPackedCopy();
			double[] c2 = Rt.times(new Matrix(coords[3], 3)).getColumnPackedCopy();
			double[] c = Rt.times(new Matrix(center, 3)).getColumnPackedCopy();
			if (!GeomUtility.isPointInTriangle(c0, c1, c2, c, null)) {
				center = GeomUtility.getIntersectionLinePlane(center, n, coords[1], coords[2], coords[3]);
			}
		}
		gl2.glPushMatrix();
		gl2.glTranslated(center[0]+R.get(0, 2)*halfThickness, center[1]+R.get(1, 2)*halfThickness,
				center[2]+R.get(2, 2)*halfThickness);
		double[] RR = getArrayFromRotationMatrix(R, true);
    	gl2.glMultMatrixd(RR, 0);
		{
			gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_BLUE, 0);
	    	gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);
			gl2.glEnable(GL2.GL_LIGHTING);
    		
    		//drawArrow without bottom disk
    		double r = SimLive.ARROW_RADIUS_FRACTION*arrowSize;
    		double l1 = (1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize;
    		double l2 = SimLive.ARROW_HEAD_FRACTION*arrowSize;
    		glu.gluCylinder(outside, r, r, l1, SimLive.ARROW_SLICES, 1);
    		gl2.glTranslated(0, 0, l1);
    		glu.gluDisk(inside, 0, 3*r, SimLive.ARROW_SLICES, 1);
    		glu.gluCylinder(outside, 3*r, 0, l2, SimLive.ARROW_SLICES, 1);
    		
    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_WHITE, 0);
    	}
    	gl2.glPopMatrix();
	}
	
	private void drawSphereOutline(GL2 gl2, double radius, double[] point) {
		gl2.glPushMatrix();
		double[] dir = getViewDirection(point);
		gl2.glRotated(Math.acos(dir[2])*180/Math.PI, -dir[1], dir[0], 0);
		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
		gl2.glBegin(GL2.GL_LINE_STRIP);
		for (int i = 0; i <= 20; i++) {
			double phi = i*2.0*Math.PI/20;
			double x = radius*Math.cos(phi);
			double y = radius*Math.sin(phi);			
			gl2.glVertex3d(x, y, 0);
		}
		gl2.glEnd();
		gl2.glPopMatrix();
	}
	
	private void drawDiskOutline(GL2 gl2, double radius) {
		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
		gl2.glBegin(GL2.GL_LINE_STRIP);
		for (int i = 0; i < SimLive.LINE_SLICES+1; i++) {
			double phi = i*2.0*Math.PI/SimLive.LINE_SLICES;
			double x = radius*Math.cos(phi);
			double y = radius*Math.sin(phi);			
			gl2.glVertex3d(x, y, 0);
		}
		gl2.glEnd();
	}
	
	private void drawCylinderOutline(GL2 gl2, double radius, double length) {
		double[] t = View.getModelViewMatrix();
		double normF = Math.sqrt(t[0]*t[0]+t[1]*t[1]);
		double[] projY = new double[]{t[1]/normF, -t[0]/normF};
		double[] crossP = new double[]{-t[6]*projY[1], t[6]*projY[0], t[4]*projY[1]-t[5]*projY[0]};
		double sign = Math.signum(crossP[0]*t[0]+crossP[1]*t[1]+crossP[2]*t[2]);
		double phi0 = sign*Math.acos(t[4]*projY[0]+t[5]*projY[1]);
		int k = (int) Math.round(phi0/(2*Math.PI/SimLive.LINE_SLICES));
		double phi = k*2*Math.PI/SimLive.LINE_SLICES;
		double yComp = radius*Math.cos(phi);
		double zComp = radius*Math.sin(phi);
		gl2.glBegin(GL2.GL_LINES);
		gl2.glVertex3d(0, yComp, zComp);
		gl2.glVertex3d(length, yComp, zComp);
		gl2.glVertex3d(0, -yComp, -zComp);
		gl2.glVertex3d(length, -yComp, -zComp);
		gl2.glEnd();
	}
	
	private void drawLineElementCylindricOutline(GL2 gl2, double[][] p0, double[][] p1, boolean hollow, boolean start, boolean end) {
		if (start) {
			gl2.glBegin(GL2.GL_LINE_STRIP);
			int kMax = hollow ? p0.length/2 : p0.length;
			for (int k = 0; k < kMax; k++) {
				gl2.glVertex3dv(p0[k], 0);
			}
			gl2.glEnd();
			if (hollow) {
				gl2.glBegin(GL2.GL_LINE_STRIP);
				for (int k = p0.length/2; k < p0.length; k++) {
					gl2.glVertex3dv(p0[k], 0);
				}
				gl2.glEnd();
			}
		}
		if (end) {
			gl2.glBegin(GL2.GL_LINE_STRIP);
			int kMax = hollow ? p1.length/2 : p1.length;
			for (int k = 0; k < kMax; k++) {
				gl2.glVertex3dv(p1[k], 0);
			}
			gl2.glEnd();
			if (hollow) {
				gl2.glBegin(GL2.GL_LINE_STRIP);
				for (int k = p1.length/2; k < p1.length; k++) {
					gl2.glVertex3dv(p1[k], 0);
				}
				gl2.glEnd();
			}
		}
		//draw cylinder outline by rendering backface polygons with lines
		gl2.glDisable(GL2.GL_CULL_FACE);
		gl2.glPolygonMode(GL2.GL_BACK, GL2.GL_LINE);
		gl2.glBegin(GL2.GL_QUADS);
		int kMax = hollow ? p0.length/2-1 : p0.length-1;
		for (int k = 0; k < kMax; k++) {
			gl2.glVertex3dv(p0[k], 0);
			gl2.glVertex3dv(p0[k+1], 0);
			gl2.glVertex3dv(p1[k+1], 0);
			gl2.glVertex3dv(p1[k], 0);
		}
		gl2.glEnd();
		gl2.glEnable(GL2.GL_CULL_FACE);
		gl2.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
	}
	
	private void drawLineElementSectionOutline(GL2 gl2, LineElement element, double[][] sectionPoints0,
			double[][] sectionPoints1, double length, boolean start, boolean end) {
		gl2.glBegin(GL2.GL_LINES);
		for (int j = 0; j < sectionPoints0.length; j+=2) {
			gl2.glVertex3dv(sectionPoints0[j], 0);
			gl2.glVertex3dv(sectionPoints1[j], 0);
		}
		gl2.glEnd();
		if (start) {
			gl2.glBegin(GL2.GL_LINES);
			for (int j = 0; j < sectionPoints0.length; j+=2) {
				gl2.glVertex3dv(sectionPoints0[j], 0);
				gl2.glVertex3dv(sectionPoints0[j+1], 0);
			}
			gl2.glEnd();
		}
		if (end) {
			gl2.glBegin(GL2.GL_LINES);
			for (int j = 0; j < sectionPoints1.length; j+=2) {
				gl2.glVertex3dv(sectionPoints1[j], 0);
				gl2.glVertex3dv(sectionPoints1[j+1], 0);
			}
			gl2.glEnd();
		}
	}
	
	private float[] getUniColorOrSetMaterial(GL2 gl2, Set set, ArrayList<Set> connectSets0, ArrayList<Set> connectSets1) {
		boolean selected = selectedSets.contains(set);
    	boolean connectRed = connectSets0.contains(set);
		boolean connectBlue = connectSets1.contains(set);
    	if (selected) {
    		return SimLive.COLOR_SELECTION;
    	}
    	else if (connectRed) {
    		return SimLive.COLOR_RED;
    	}
    	else if (connectBlue) {
    		return SimLive.COLOR_BLUE;
    	}
    	else {
    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_WHITE, 0);
    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);	        	
    		return null;
    	}
	}
	
	private void renderLineElementAndPointMass(GL2 gl2, Element element, ArrayList<Set> connectSets0, ArrayList<Set> connectSets1,
			float width, GLU glu, GLUquadric outside, GLUquadric inside, double lineElementRadius, double scaling, double zoom, float[] uniColor) {
		ScalarPlot scalarPlot = null;
		if (SimLive.mode == Mode.RESULTS && SimLive.post.getScalarPlot() != null &&
			SimLive.post.getScalarPlot().hasValue(element, SimLive.post.getPostIncrement())) {
			scalarPlot = SimLive.post.getScalarPlot();
		}
		if (element.getType() == Element.Type.POINT_MASS) {
			boolean existing = element.getID() < SimLive.model.getElements().size();
			int[] elemNodes = element.getElementNodes();
			double[] coords = getCoordsWithScaledDisp(elemNodes[0]);
			gl2.glPushMatrix();
			gl2.glTranslated(coords[0], coords[1], coords[2]);
			if (scalarPlot != null || uniColor != null) {
				gl2.glDisable(GL2.GL_LIGHTING);
				drawSphereOutline(gl2, SimLive.POINT_MASS_RADIUS*2.0/width/zoom, coords);
				if (uniColor != null) {
					if (existing) gl2.glColor3fv(uniColor, 0);
		    		else gl2.glColor4fv(uniColor, 0);
				}
				else {
					double value = scalarPlot.getValueAtNode(elemNodes[0], SimLive.post.getPostIncrementID());
					gl2.glColor3fv(scalarPlot.getColor(value, SimLive.post.getNumberOfColors()), 0);												
				}
			}
			glu.gluSphere(outside, SimLive.POINT_MASS_RADIUS*2.0/width/zoom, 2*SimLive.SPHERE_SLICES, SimLive.SPHERE_STACKS);
			gl2.glPopMatrix();
		}
		if (element.isLineElement()) {
			if (element.getType() == Element.Type.SPRING) {
				renderSpring(gl2, (LineElement) element, width, glu, outside, inside, lineElementRadius,
						scalarPlot, uniColor, zoom);
			}
			else {
				renderLineElementWithSection(gl2, (LineElement) element, scaling, scalarPlot, uniColor);
			}
		}
		gl2.glEnable(GL2.GL_LIGHTING);
	}
	
	private boolean isLineElementVisibleOnScreen(LineElement element, double[] coords0, double[] coords1, double radius, double scaling) {
		//check if sphere with radius is visible
		double[] coords = new double[3];
		coords[0] = coords0[0]+(coords1[0]-coords0[0])/2;
		coords[1] = coords0[1]+(coords1[1]-coords0[1])/2;
		coords[2] = coords0[2]+(coords1[2]-coords0[2])/2;
		double[] screen = modelToScreenCoordinates(coords);
		if (element.getType() == Element.Type.BEAM && SimLive.mode == Mode.RESULTS) {
			double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(element.getID());
			double length0 = element.getLength();
			//max from shape function is length0*4.0/27.0
			double dispY0 = angles[0][1]*length0*4.0/27.0*scaling;
			double dispZ0 = angles[0][2]*length0*4.0/27.0*scaling;
			double dispY1 = angles[1][1]*length0*4.0/27.0*scaling;
			double dispZ1 = angles[1][2]*length0*4.0/27.0*scaling;
			radius += Math.sqrt(dispY0*dispY0+dispZ0*dispZ0)+Math.sqrt(dispY1*dispY1+dispZ1*dispZ1);
		}
		int[] viewport = getViewport();
		double factor = Math.abs(getSizeFactorPerspective(coords));
		radius *= zoom*viewport[2]/2/factor;
		if (screen[0] < -radius || screen[0] > viewport[2]+radius ||
			screen[1] < -radius || screen[1] > viewport[3]+radius) {
			return false;
		}
		return true;
	}
	
	private void renderLineElementWithSection(GL2 gl2, LineElement element, double scaling, ScalarPlot scalarPlot, float[] uniColor) {
		boolean existing = element.getID() < SimLive.model.getElements().size();
		int[] elemNodes = element.getElementNodes();
		double[] coords0 = getCoordsWithScaledDisp(elemNodes[0]);
		double[] coords1 = getCoordsWithScaledDisp(elemNodes[1]);
		double[] diff = new double[3];
		diff[0] = coords1[0]-coords0[0];
		diff[1] = coords1[1]-coords0[1];
		diff[2] = coords1[2]-coords0[2];
		double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
		if (length < SimLive.ZERO_TOL) return;
		Section section = element.isSectionValid(SimLive.model.getSections()) ?
				element.getSection() :
				new Section(new SectionShape(SectionShape.Type.DIRECT_INPUT));
		double[][] P = section.getSectionPoints();
		if (!isLineElementVisibleOnScreen(element, coords0, coords1,
				Math.sqrt(P[0][1]*P[0][1]+P[0][2]*P[0][2])+length/2.0, scaling)) return;
		gl2.glPushMatrix();
		gl2.glTranslated(coords0[0], coords0[1], coords0[2]);
		Matrix Rr = null;
		if (existing) {
			Rr = new Matrix(View.Rr[element.getID()]);
		}
		else {
			Rr = element.getVectorTransformation(new double[]{diff[0]/length, diff[1]/length, diff[2]/length});
		}
    	gl2.glMultMatrixd(getArrayFromRotationMatrix(Rr, true), 0);
    	
    	double t = 0.0, y = 0.0, z = 0.0;
		int lineDivisions = getLineDivisions(element);
		if (!existing) {
			lineDivisions = 1;
		}
		boolean shading = uniColor == null && scalarPlot == null;
		double[][] N = shading ? section.getSectionNormals() : null;
		for (int k = 0; k < P.length; k++) {
			P[k] = Arrays.copyOf(P[k], 4);
			P[k][3] = 1;
		}
		double[][][] p = new double[lineDivisions+1][P.length][];
		double[][][] n = shading ? new double[lineDivisions+1][N.length][] : null;
		double[] startN = null;
		double[] endN = null;
		double deltaL = length/(double) lineDivisions;
		SectionShape.Type sectionShapeType = section.getSectionShape().getType();
		boolean cylindric = !SimLive.settings.isShowSections || sectionShapeType == SectionShape.Type.CIRCLE ||
				sectionShapeType == SectionShape.Type.HOLLOW_CIRCLE ||sectionShapeType == SectionShape.Type.DIRECT_INPUT;
		boolean hollow = SimLive.settings.isShowSections && sectionShapeType == SectionShape.Type.HOLLOW_CIRCLE ||
				sectionShapeType == SectionShape.Type.HOLLOW_RECTANGLE;
		for (int i = 0; i < lineDivisions+1; i++) {
			gl2.glPushMatrix();
			gl2.glTranslated(t*length, y, z);
			t = (i+1)/(double) lineDivisions;
			boolean start, end;
			if (existing) {
				start = i == 0 && (SimLive.contains(outlineEdge[elemNodes[0]], elemNodes[0]) || SimLive.settings.isShowEdges);
				end = i == lineDivisions && (SimLive.contains(outlineEdge[elemNodes[1]], elemNodes[1]) || SimLive.settings.isShowEdges);
			}
			else {
				start = i == 0;
				end = i == lineDivisions;
			}
	    	if (element.getType() == Element.Type.BEAM && SimLive.mode == Mode.RESULTS) {
				Beam beam = (Beam) element;
	    		double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(beam.getID());
				double[] disp = beam.getBendingDispInCoRotatedFrame(t, angles);
    			double deltaY = disp[0]*scaling-y;
    			double deltaZ = disp[1]*scaling-z;
    			double[] axis = new double[3];
    			axis[1] = -deltaZ;
    			axis[2] = deltaY;
    			double sectionLength = Math.sqrt(deltaL*deltaL+deltaY*deltaY+deltaZ*deltaZ);
    			double angle = Math.acos(deltaL/sectionLength);
    			double[] R1 = getArrayFromRotationMatrix(GeomUtility.getRotationMatrix(angle, axis), true);
    			gl2.glMultMatrixd(R1, 0);
    			gl2.glRotated((angles[1][0]-angles[0][0])*t*scaling*180.0/Math.PI, 1, 0, 0);
    			Matrix R = new Matrix(getModelViewMatrix(), 4);
    			Matrix RR = shading ? R.getMatrix(0, 2, 0, 2) : null;
    			for (int k = 0; k < P.length; k++) {
    				p[i][k] = R.times(new Matrix(P[k], 4)).getColumnPackedCopy();
    				if (shading) n[i][k] = RR.times(new Matrix(N[k], 3)).getColumnPackedCopy();
    			}
    			if (start && shading) startN = R.getMatrix(0, 2, 0, 0).times(-1).getColumnPackedCopy();
    			if (end && shading) endN = R.getMatrix(0, 2, 0, 0).getColumnPackedCopy();
    			y = disp[0]*scaling;
				z = disp[1]*scaling;
			}
			else {
				Matrix R = new Matrix(getModelViewMatrix(), 4);
				Matrix RR = shading ? R.getMatrix(0, 2, 0, 2) : null;
    			for (int k = 0; k < P.length; k++) {
    				p[i][k] = R.times(new Matrix(P[k], 4)).getColumnPackedCopy();
    				if (shading) n[i][k] = RR.times(new Matrix(N[k], 3)).getColumnPackedCopy();
    			}
    			if (start && shading) startN = R.getMatrix(0, 2, 0, 0).times(-1).getColumnPackedCopy();
    			if (end && shading) endN = R.getMatrix(0, 2, 0, 0).getColumnPackedCopy();
			}
			gl2.glPopMatrix();    			
		}
		
		gl2.glLoadIdentity();
		for (int i = 0; i < lineDivisions; i++) {
			t = i/(double) lineDivisions;
			boolean start, end;
			if (existing) {
				start = i == 0 && (SimLive.contains(outlineEdge[elemNodes[0]], elemNodes[0]) || SimLive.settings.isShowEdges);
				end = i == lineDivisions-1 && (SimLive.contains(outlineEdge[elemNodes[1]], elemNodes[1]) || SimLive.settings.isShowEdges);
			}
			else {
				start = i == 0;
				end = i == lineDivisions-1;
			}
	    	if (uniColor != null) {
	    		gl2.glDisable(GL2.GL_LIGHTING);
	    		if (existing) gl2.glColor3fv(uniColor, 0);
	    		else gl2.glColor4fv(uniColor, 0);
			}
	    	else if (scalarPlot != null) {
				gl2.glDisable(GL2.GL_LIGHTING);
				double value = scalarPlot.getValueForLineElement(element, t, SimLive.post.getPostIncrementID());
				gl2.glColor3fv(scalarPlot.getColor(value, SimLive.post.getNumberOfColors()), 0);
			}
			if (start) {
				if (shading) gl2.glNormal3dv(startN, 0);
    			if (hollow) {
    				gl2.glBegin(GL2.GL_QUAD_STRIP);
	    			for (int k = 0; k < P.length/2; k++) {
	    				gl2.glVertex3dv(p[i][k], 0);
	    				gl2.glVertex3dv(p[i][P.length-1-k], 0);
	    			}
	        		gl2.glEnd();
    			}
    			else {
					gl2.glBegin(GL2.GL_TRIANGLE_FAN);
	    			gl2.glVertex3dv(p[i][0], 0);
    				for (int k = P.length-1; k > -1; k--) {
	    				gl2.glVertex3dv(p[i][k], 0);
	    			}
	    			gl2.glEnd();
    			}
    		}
			gl2.glBegin(GL2.GL_QUADS);
    		for (int k = 0; k < P.length-1; k++) {
    			if (shading) gl2.glNormal3dv(n[i][k], 0);
    			gl2.glVertex3dv(p[i][k], 0);
    			if (shading) gl2.glNormal3dv(n[i][k+1], 0);
    			gl2.glVertex3dv(p[i][k+1], 0);
    			if (shading) gl2.glNormal3dv(n[i+1][k+1], 0);
    			gl2.glVertex3dv(p[i+1][k+1], 0);
    			if (shading) gl2.glNormal3dv(n[i+1][k], 0);
    			gl2.glVertex3dv(p[i+1][k], 0);
    		}
    		gl2.glEnd();
    		if (end) {
    			if (shading) gl2.glNormal3dv(endN, 0);
    			if (hollow) {
    				gl2.glBegin(GL2.GL_QUAD_STRIP);
    				for (int k = 0; k < P.length/2; k++) {
	    				gl2.glVertex3dv(p[i+1][P.length-1-k], 0);
	    				gl2.glVertex3dv(p[i+1][k], 0);
	    			}
	        		gl2.glEnd();
    			}
    			else {
	    			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
	    			gl2.glVertex3dv(p[i+1][0], 0);
	    			for (int k = 0; k < P.length-1; k++) {
		    			gl2.glVertex3dv(p[i+1][k], 0);
		    		}
	    			gl2.glEnd();
    			}
    		}
    		
    		gl2.glDisable(GL2.GL_LIGHTING);
    		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
    		if (!cylindric) {
    			drawLineElementSectionOutline(gl2, element, p[i], p[i+1], deltaL, start, end);
    		}
    		else {
    			drawLineElementCylindricOutline(gl2, p[i], p[i+1], hollow, start, end);
    		}
    		gl2.glEnable(GL2.GL_LIGHTING);
    		
    		if (existing && animation == null) {
    			if (pElementScreenPolys[element.getID()] == null) {
    				pElementScreenPolys[element.getID()] = new double[lineDivisions*p[i].length*3][];
    			}
    			for (int k = 0; k < p[i].length; k++) {
    				pElementScreenPolys[element.getID()][i*p[i].length*3+k*3] = modelToScreenCoordinates(p[i][k]);
    				pElementScreenPolys[element.getID()][i*p[i].length*3+k*3+1] = modelToScreenCoordinates(p[i][(k+1)%p[i].length]);
    				pElementScreenPolys[element.getID()][i*p[i].length*3+k*3+2] = modelToScreenCoordinates(p[i+1][(k+1)%p[i].length]);
    			}
    		}
		}
    	gl2.glPopMatrix();
	}
	
	private void renderSpring(GL2 gl2, LineElement element, float width, GLU glu, GLUquadric outside,
			GLUquadric inside, double lineElementRadius, ScalarPlot scalarPlot, float[] uniColor, double zoom) {
		boolean existing = element.getID() < SimLive.model.getElements().size();
		int[] elemNodes = element.getElementNodes();
		double[] coords0 = getCoordsWithScaledDisp(elemNodes[0]);
		double[] coords1 = getCoordsWithScaledDisp(elemNodes[1]);
		double[] diff = new double[3];
		diff[0] = coords1[0]-coords0[0];
		diff[1] = coords1[1]-coords0[1];
		diff[2] = coords1[2]-coords0[2];
		double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
		if (length < SimLive.ZERO_TOL) return;
		if (!isLineElementVisibleOnScreen(element, coords0, coords1,
				SimLive.SPRING_RADIUS*2.0/width/zoom+length/2.0, 0.0)) return;
		gl2.glPushMatrix();
		gl2.glTranslated(coords0[0], coords0[1], coords0[2]);
		Matrix Rr = null;
		if (existing) {
			Rr = new Matrix(View.Rr[element.getID()]);
		}
		else {
			Rr = element.getVectorTransformation(new double[]{diff[0]/length, diff[1]/length, diff[2]/length});
		}
    	gl2.glMultMatrixd(getArrayFromRotationMatrix(Rr, true), 0);
    	
		double r = SimLive.SPRING_RADIUS*2.0/width/zoom;
		double h = r;
		double length0 = element.getLength();
		double nCoils = length0/h;
		h = length / nCoils;
		double deltaCoil = 1.0/8.0;
		double chordLength = 2.0*Math.sin(Math.PI*deltaCoil)*r;
		double slope = Math.atan(h*deltaCoil/chordLength);
		double sliceLength = chordLength/Math.cos(slope);
		if (!existing && getModelViewMatrix()[2] > 0) {
			gl2.glTranslated(length, 0, 0);
			gl2.glRotated(180, 0, 1, 0);
		}
		for (double t = 0; t < nCoils; t+=deltaCoil) {
			double x = h*t;
			double y = r*Math.cos(-Math.PI*deltaCoil);
			double z = r*Math.sin(-Math.PI*deltaCoil);
			gl2.glPushMatrix();
			double angle = 2.0*Math.PI*(t+deltaCoil/2.0);
			gl2.glRotated(angle*180.0/Math.PI, 1, 0, 0);
			gl2.glTranslated(x, y, z);
			gl2.glRotated(slope*180.0/Math.PI, 0, 1, 0);
			
			gl2.glPushMatrix();
			gl2.glRotated(-90, 0, 1, 0);
			gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			gl2.glDisable(GL2.GL_LIGHTING);
			drawCylinderOutline(gl2, lineElementRadius/2.0, sliceLength);
			gl2.glEnable(GL2.GL_LIGHTING);
			gl2.glPopMatrix();
			
			if (uniColor != null) {
	    		gl2.glDisable(GL2.GL_LIGHTING);
	    		if (existing) gl2.glColor3fv(uniColor, 0);
	    		else gl2.glColor4fv(uniColor, 0);
			}
	    	else if (scalarPlot != null) {
				gl2.glDisable(GL2.GL_LIGHTING);
				double value = scalarPlot.getValueForLineElement(element, t, SimLive.post.getPostIncrementID());
				gl2.glColor3fv(scalarPlot.getColor(value, SimLive.post.getNumberOfColors()), 0);
			}
			
			glu.gluCylinder(outside, lineElementRadius/2.0, lineElementRadius/2.0, sliceLength, SimLive.LINE_SLICES, 1);
			gl2.glPopMatrix();
		}
		
		if (existing && animation == null) {
			if (pElementScreenPolys[element.getID()] == null) {
				pElementScreenPolys[element.getID()] = new double[SimLive.LINE_SLICES*4][];
			}
			double radius = 2*SimLive.SPRING_RADIUS/width/zoom;
			for (int k = 0; k < SimLive.LINE_SLICES; k++) {
				double phi0 = k*2*Math.PI/SimLive.LINE_SLICES;
				double phi1 = (k+1)*2*Math.PI/SimLive.LINE_SLICES;
				pElementScreenPolys[element.getID()][k*4] =
						modelToScreenCoordinates(new double[]{0, Math.cos(phi0)*radius, Math.sin(phi0)*radius});
				pElementScreenPolys[element.getID()][k*4+1] =
						modelToScreenCoordinates(new double[]{0, Math.cos(phi1)*radius, Math.sin(phi1)*radius});
				pElementScreenPolys[element.getID()][k*4+2] =
						modelToScreenCoordinates(new double[]{length, Math.cos(phi1)*radius, Math.sin(phi1)*radius});
				pElementScreenPolys[element.getID()][k*4+3] =
						modelToScreenCoordinates(new double[]{length, Math.cos(phi0)*radius, Math.sin(phi0)*radius});
			}
		}
    	gl2.glPopMatrix();
	}
	
	private void renderCurvePlot(GL2 gl2, LineElement element, float width, double scaling, ScalarPlot scalarPlot, double lineElementRadius) {
		
		final double tol = 1.1;
		
		double valueScale = SimLive.CURVE_PLOT_SIZE/Math.max(Math.abs(scalarPlot.getGlobalMaxValue()),
				Math.abs(scalarPlot.getGlobalMinValue()))/width/zoom0*SimLive.post.getCurvePlotScaleFactor();
		
		int inc = SimLive.post.getPostIncrementID();
		double value0 = scalarPlot.getValueForLineElement(element, 0, inc)*valueScale;
		double value1 = scalarPlot.getValueForLineElement(element, 1, inc)*valueScale;
		
		if (value0 != 0.0 || value1 != 0.0) {
			gl2.glDisable(GL2.GL_LIGHTING);
			gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			switchSingleDoubleSided(gl2, true);
			
			int[] elemNodes = element.getElementNodes();
			double[] coords0 = getCoordsWithScaledDisp(elemNodes[0]);
			double[] coords1 = getCoordsWithScaledDisp(elemNodes[1]);
			double[] diff = new double[3];
			diff[0] = coords1[0]-coords0[0];
			diff[1] = coords1[1]-coords0[1];
			diff[2] = coords1[2]-coords0[2];
			double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
			gl2.glPushMatrix();
			gl2.glTranslated(coords0[0], coords0[1], coords0[2]);
			double[] R = getArrayFromRotationMatrix(new Matrix(View.Rr[element.getID()]), true);
			gl2.glMultMatrixd(R, 0);
			
			double t = 0.0, y = 0.0, z = 0.0;
			int lineDivisions = getLineDivisions(element);
			double deltaL = length/(double) lineDivisions;
			double[][] p0 = new double[lineDivisions+1][3];
			double[][] p1 = new double[lineDivisions+1][3];
			double[] v = new double[lineDivisions+1];
			for (int i = 1; i < lineDivisions+2; i++) {
				p0[i-1][0] = t*length;
				p0[i-1][1] = y;
				p0[i-1][2] = z;
				t = i/(double) lineDivisions;
				
				double[] disp = new double[2];
				Matrix RR = Matrix.identity(3, 3);
				if (element.getType() == Element.Type.BEAM) {
					Beam beam = (Beam) element;
					
					double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(beam.getID());
					disp = beam.getBendingDispInCoRotatedFrame(t, angles);
	    			double deltaY = disp[0]*scaling-y;
	    			double deltaZ = disp[1]*scaling-z;
	    			double[] axis = new double[3];
	    			axis[1] = -deltaZ;
	    			axis[2] = deltaY;
	    			double angle = Math.atan(Math.sqrt(deltaY*deltaY+deltaZ*deltaZ)/deltaL);
	    			Matrix R1 = GeomUtility.getRotationMatrix(angle, axis);
	    			Matrix R2 = GeomUtility.getRotationMatrixX((t-1.0/(double) lineDivisions)*(angles[1][0]-angles[0][0])*scaling);
					RR = R1.times(R2);
				}
				
				if (SimLive.post.isCurvePlotSwitchOrientation()) {
					RR = RR.times(GeomUtility.getRotationMatrixX(Math.PI/2.0));
				}
				
				v[i-1] = value0+(value1-value0)*(t-1/(double) lineDivisions);
				
				p1[i-1][0] = p0[i-1][0]+RR.get(0, 1)*v[i-1];
				p1[i-1][1] = p0[i-1][1]+RR.get(1, 1)*v[i-1];
				p1[i-1][2] = p0[i-1][2]+RR.get(2, 1)*v[i-1];
				
				if (element.getType() == Element.Type.BEAM) {
	    			y = disp[0]*scaling;
					z = disp[1]*scaling;
				}
			}
			
			gl2.glBegin(GL2.GL_LINE_STRIP);
	    	gl2.glVertex3dv(p0[0], 0);
	    	gl2.glVertex3dv(p1[0], 0);
	    	for (int i = 1; i < lineDivisions+1; i++) {
				gl2.glVertex3dv(p1[i], 0);
				gl2.glVertex3dv(p0[i], 0);
				gl2.glVertex3dv(p1[i], 0);
			}
			gl2.glVertex3dv(p0[lineDivisions], 0);
			gl2.glEnd();
			gl2.glBegin(GL2.GL_LINE_STRIP);
			for (int i = 0; i < lineDivisions+1; i++) {
				gl2.glVertex3dv(p0[i], 0);
			}
			gl2.glEnd();
			
			gl2.glColor3fv(SimLive.COLOR_WHITE, 0);
			for (int i = 0; i < lineDivisions; i++) {
				Matrix d0 = new Matrix(new double[]{p0[i][0]-p1[i][0], p0[i][1]-p1[i][1], p0[i][2]-p1[i][2]}, 3);
				Matrix d1 = new Matrix(new double[]{p1[i+1][0]-p1[i][0], p1[i+1][1]-p1[i][1], p1[i+1][2]-p1[i][2]}, 3);
				gl2.glNormal3dv(d1.crossProduct(d0).getColumnPackedCopy(), 0);
				if (v[i]*v[i+1] > 0) {
					gl2.glBegin(GL2.GL_QUADS);
					gl2.glVertex3dv(p0[i], 0);
					gl2.glVertex3dv(p1[i], 0);
					gl2.glVertex3dv(p1[i+1], 0);
					gl2.glVertex3dv(p0[i+1], 0);
					gl2.glEnd();
				}
				else {
					double l0 = d0.normF();
					d0.timesEquals(1.0/l0);
					double l1 = d1.normF();
					d1.timesEquals(1.0/l1);
					double cosAngle = d0.dotProduct(d1);
					double factor = Math.min((l0+lineElementRadius*tol)/cosAngle, l1);
					double[] p = new Matrix(p1[i], 3).plus(d1.times(factor)).getColumnPackedCopy();
					gl2.glBegin(GL2.GL_TRIANGLES);
					gl2.glVertex3dv(p0[i], 0);
					gl2.glVertex3dv(p1[i], 0);
					gl2.glVertex3dv(p, 0);
					gl2.glVertex3dv(p, 0);
					gl2.glVertex3dv(p0[i+1], 0);
					gl2.glVertex3dv(p1[i+1], 0);
					gl2.glEnd();
				}
			}
			gl2.glEnable(GL2.GL_LIGHTING);
			switchSingleDoubleSided(gl2, false);
			
			gl2.glPopMatrix();
		}
	}
	
	private double[] getLabelBasePointShiftForCurvePlot(LineElement element, float width, Label label, double scaling, ScalarPlot scalarPlot) {
		double[] shift = new double[3];
		
		double maxGlobal = Math.max(Math.abs(scalarPlot.getGlobalMaxValue()), Math.abs(scalarPlot.getGlobalMinValue()));
		double valueScale = maxGlobal > 0.0 ? SimLive.CURVE_PLOT_SIZE/maxGlobal/width/zoom0*SimLive.post.getCurvePlotScaleFactor() : 0.0;
		
		int inc = SimLive.post.getPostIncrementID();
		double t = label.getTValue();
		double value = SimLive.post.getScalarPlot().getValueForLineElement((LineElement) element, t, inc)*valueScale;
		
		Matrix Rr = new Matrix(View.Rr[element.getID()]);
		
		if (element.getType() == Element.Type.BEAM) {
			int[] elemNodes = element.getElementNodes();
			double[] coords0 = getCoordsWithScaledDisp(elemNodes[0]);
			double[] coords1 = getCoordsWithScaledDisp(elemNodes[1]);
			double[] diff = new double[3];
			diff[0] = coords1[0]-coords0[0];
			diff[1] = coords1[1]-coords0[1];
			diff[2] = coords1[2]-coords0[2];
			double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
			int lineDivisions = getLineDivisions(element);
			double deltaL = length/(double) lineDivisions;
			Beam beam = (Beam) element;
			double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(beam.getID());
			double[] disp = beam.getBendingDispInCoRotatedFrame(t, angles);
			double y = disp[0]*scaling;
			double z = disp[1]*scaling;
			disp = beam.getBendingDispInCoRotatedFrame(t+1.0/(double) lineDivisions, SimLive.post.getPostIncrement().getAnglesBeam(beam.getID()));
			double deltaY = disp[0]*scaling-y;
			double deltaZ = disp[1]*scaling-z;
			double[] axis = new double[3];
			axis[1] = -deltaZ;
			axis[2] = deltaY;
			double angle = Math.atan(Math.sqrt(deltaY*deltaY+deltaZ*deltaZ)/deltaL);
			Matrix R1 = GeomUtility.getRotationMatrix(angle, axis);
			Matrix R2 = GeomUtility.getRotationMatrixX(t*(angles[1][0]-angles[0][0])*scaling);
			Rr = Rr.times(R1).times(R2);
		}
		
		if (SimLive.post.isCurvePlotSwitchOrientation()) {
			Rr = Rr.times(GeomUtility.getRotationMatrixX(Math.PI/2.0));
		}
		shift[0] = Rr.get(0, 1)*value;
	    shift[1] = Rr.get(1, 1)*value;
	    shift[2] = Rr.get(2, 1)*value;
		
		return shift;
	}
	
	private void renderToImageBuffer(GL2 gl2, GLU glu, float width, float height,
			GLUquadric outside, GLUquadric inside) {
		gl2.glEnable(GL2.GL_DEPTH_TEST);
		Side temp = side;
		side = Side.NONE;
		drawCoordinateSystem(gl2, glu, width, height, outside, inside, true);
		side = temp;
		int length = (int) SimLive.COORDINATE_SYSTEM_SIZE*2;
		imgBuffer = FloatBuffer.allocate(length*length*3);
		gl2.glReadPixels(0, 0, length, length, GL2.GL_RGB, GL2.GL_FLOAT, imgBuffer);
		gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);		
	}
	
	public void initViewForNewFile() {
		Rr = null;
		perspective = false;
		R0 = Matrix.identity(3, 3);
		rotPoint = new double[3];
		int[] viewport = getViewport();
		GL2 gl2 = SimLive.glcontext.getGL().getGL2();
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glLoadIdentity();
		setProjection(gl2, viewport[2], viewport[3]);
		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glLoadIdentity();
		
		deleteAllLabels();
		deleteAllMeasurements();
		setFocusPoint = false;
		focusPoint = null;
		focusPointOrientation = null;
		nodeID = -1;
		zCoord = zDisp = 0.0;
		lines = null;
		deselectAllAndDisposeDialogs();
	}
	
	private void initView(GL2 gl2, float width, float height) {
		imgBuffer = null;
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl2.glLoadIdentity();        
		setProjection(gl2, width, height);
	    gl2.glViewport(0, 0, (int) width, (int) height);
	    gl2.glMatrixMode(GL2.GL_MODELVIEW);
	    
	    FontData fontData = Display.getDefault().getSystemFont().getFontData()[0];
	    java.awt.Font font = new java.awt.Font(fontData.getName(), java.awt.Font.PLAIN, Math.round(SimLive.fontHeight));
		renderer = new TextRenderer(font, true, false);
		LineMetrics lineMetrics = renderer.getFont().getLineMetrics("", new FontRenderContext(renderer.getFont().getTransform(), true, false));
    	descent = lineMetrics.getDescent();
    	font = new java.awt.Font(fontData.getName(), java.awt.Font.BOLD, Math.round(SimLive.fontHeight));
		rendererBold = new TextRenderer(font, true, false);
	}
	
	private void initFOV() {
		double width = getBounds().width;
		double height = getBounds().height;
		if (width > height) {
			fovy = Math.atan(height/width);
		}
		else {
			fovy = Math.PI/4.0;
		}
	}
	
	public void initPerspective() {
		int[] viewport = getViewport();
		float aspectRatio = (float) viewport[3] / (float) viewport[2];
		Matrix dir = R0.getMatrix(0, 2, 2, 2);
		if (dir.get(2, 0) < 0) {
			dir.times(-1);
		}
		double dist = aspectRatio/zoom/Math.tan(fovy/2);
		cameraRefPos[0] = rotPoint[0] + dir.get(0, 0)*dist;
		cameraRefPos[1] = rotPoint[1] + dir.get(1, 0)*dist;
		cameraRefPos[2] = rotPoint[2] + dir.get(2, 0)*dist;
	}
	
	private void setProjection(GL2 gl2, float width, float height) {
		float aspectRatio = height / width;
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		double nearClip = SimLive.settings.meshSize/100.0;
		double farClip = SimLive.settings.meshSize*100.0;
		if (perspective) {
			GLU glu = new GLU();
			glu.gluPerspective(fovy*180/Math.PI, width/height, nearClip, farClip);
		}
		else {
			gl2.glOrtho(-1.0/zoom, 1.0/zoom, -aspectRatio/zoom, aspectRatio/zoom, -farClip, farClip); //TODO
		}
	}
	
	private void drawBackground(GL2 gl2) {
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glMatrixMode(GL2.GL_PROJECTION);
	    gl2.glPushMatrix();
	    gl2.glLoadIdentity();
	    gl2.glMatrixMode(GL2.GL_MODELVIEW);
	    gl2.glPushMatrix();
	    gl2.glLoadIdentity();
	    gl2.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
		gl2.glBegin(GL2.GL_QUADS);
		gl2.glColor3fv(SimLive.COLOR_WHITE, 0);    	
	    gl2.glVertex2d(-1, -1);
	    gl2.glVertex2d(1, -1);
	    gl2.glColor3fv(SimLive.COLOR_LIGHT_BLUE, 0);    	
	    gl2.glVertex2d(1, 1);
	    gl2.glVertex2d(-1, 1);
	    gl2.glEnd();
	    gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPopMatrix();
		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPopMatrix();
		gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);		
	}
	
	private void drawGrid(GL2 gl2, float width, float height) {
		gl2.glLineStipple(1, (short) 0xAAAA);
		gl2.glEnable(GL2.GL_LINE_STIPPLE);
		gl2.glBegin(GL2.GL_LINES);
		gl2.glColor3fv(SimLive.COLOR_DARK_GRAY, 0);
		/*double w = 2.0/zoom;
		int start = (int) (cameraRefPos[0]/zoom/Sim2d.settings.gridSize);
	    for (double i = start*Sim2d.settings.gridSize-w/2.0; i <= start*Sim2d.settings.gridSize+w/2.0+Sim2d.settings.gridSize; i+=Sim2d.settings.gridSize) {
			double x1 = i;
			double y1 = -1000;
			double x2 = x1;
			double y2 = 1000;				
			gl2.glVertex3d(x1, y1, 0);
		    gl2.glVertex3d(x2, y2, 0);
		}*/		
		double nSteps = SimLive.settings.meshCount;
		double xMin = -nSteps/2.0*SimLive.settings.meshSize;
		double xMax = nSteps/2.0*SimLive.settings.meshSize;
		double yMin = -nSteps/2.0*SimLive.settings.meshSize;
		double yMax = nSteps/2.0*SimLive.settings.meshSize;
		for (double x = xMin; x <= xMax; x+=(xMax-xMin)/nSteps) {
			gl2.glVertex3d(x, yMin, 0);
		    gl2.glVertex3d(x, yMax, 0);
		}
		for (double y = yMin; y <= yMax; y+=(yMax-yMin)/nSteps) {
			gl2.glVertex3d(xMin, y, 0);
		    gl2.glVertex3d(xMax, y, 0);
		}
		gl2.glEnd();
		gl2.glDisable(GL2.GL_LINE_STIPPLE);		
	}
	
	private double[] mapTrackBallCoordinates(double x, double y, double r) {
		/* holroyd's trackball */
		double[] coords = new double[3];
		coords[0] = x;
		coords[1] = y;
		double l = Math.sqrt(x*x+y*y);
		if (l > r/Math.sqrt(2.0)) {
			coords[2] = r*r/(2.0*l);
		}
		else {
			coords[2] = Math.sqrt(r*r-(x*x+y*y));
		}
		return coords;
	}
	
	private void setTransformation(GL2 gl2, GLU glu, float width, float height, double zoom) {	
		gl2.glLoadIdentity();
		
		if (mouseButton == 2 && isMouseDragged) {
			double xa = mouseDown[0]-width/2.0;
			double ya = height/2.0-mouseDown[1];
			double xc = mousePos[0]-width/2.0;
			double yc = height/2.0-mousePos[1];
			double r = Math.min(width, height)*3.0/8.0;
			
			Matrix Pa = new Matrix(mapTrackBallCoordinates(xa, ya, r), 3);
			Matrix Pc = new Matrix(mapTrackBallCoordinates(xc, yc, r), 3);
			Matrix u = Pa.crossProduct(Pc);
			double theta = Math.atan2(u.normF(), Pa.dotProduct(Pc));
			
			R = GeomUtility.getRotationMatrix(-theta, u.getColumnPackedCopy());
		}
		
		Matrix RR = R0.times(R);
		Matrix dir = RR.getMatrix(0, 2, 2, 2);
		Matrix right = RR.getMatrix(0, 2, 0, 0);
		Matrix up = RR.getMatrix(0, 2, 1, 1);
		
		double distToRotPoint = Math.sqrt((cameraRefPos[0]-rotPoint[0])*(cameraRefPos[0]-rotPoint[0])+
				(cameraRefPos[1]-rotPoint[1])*(cameraRefPos[1]-rotPoint[1])+
				(cameraRefPos[2]-rotPoint[2])*(cameraRefPos[2]-rotPoint[2]));
		
		if (focusPoint != null) {
			rotPoint = focusPoint.getCoordinatesWithDeformation();
		}
		else {
			double[] delta = new double[2];
			delta[0] = -2.0*move[0]/width/zoom;
			delta[1] = 2.0*move[1]/width/zoom;
			move[0] = move[1] = 0.0;		
			
			rotPoint[0] += delta[0]*right.get(0, 0)+delta[1]*up.get(0, 0);
			rotPoint[1] += delta[0]*right.get(1, 0)+delta[1]*up.get(1, 0);
			rotPoint[2] += delta[0]*right.get(2, 0)+delta[1]*up.get(2, 0);
		}
		
		cameraRefPos[0] = rotPoint[0]+dir.get(0, 0)*distToRotPoint;
		cameraRefPos[1] = rotPoint[1]+dir.get(1, 0)*distToRotPoint;
		cameraRefPos[2] = rotPoint[2]+dir.get(2, 0)*distToRotPoint;
		
		glu.gluLookAt(cameraRefPos[0], cameraRefPos[1], cameraRefPos[2],
				cameraRefPos[0]-dir.get(0, 0), cameraRefPos[1]-dir.get(1, 0), cameraRefPos[2]-dir.get(2, 0),
				up.get(0, 0), up.get(1, 0), up.get(2, 0));
		
		//gl2.glScaled(zoom, zoom, zoom);
	}
	
	private double[] getArrayFromRotationMatrix(Matrix R, boolean columnPacked) {
		double[] array = new double[16];
		if (columnPacked) {
			for (int i = 0; i < 3; i++) {
				array[i] = R.get(i, 0);
				array[4+i] = R.get(i, 1);
				array[8+i] = R.get(i, 2);
			}
		}
		else {
			for (int i = 0; i < 3; i++) {
				array[i] = R.get(0, i);
				array[4+i] = R.get(1, i);
				array[8+i] = R.get(2, i);
			}
		}
		array[15] = 1.0;
		return array;
	}
	
	private void drawCoordinateSystem(GL2 gl2, GLU glu, double width, double height,
			GLUquadric outside, GLUquadric inside, boolean imageBuffer) {
		gl2.glPushMatrix();
		gl2.glLoadIdentity();
		final double dist = 1.2*SimLive.COORDINATE_SYSTEM_SIZE;
		double aspectRatio = height / width;
		double scale = 0;
		if (perspective) {
			double nearClip = SimLive.settings.meshSize/100.0;
			double depth = 2.0*nearClip; //larger than distance to nearClip
			double top = depth*Math.tan(fovy/2);
			double right = top/aspectRatio;
			gl2.glTranslated(-right+dist/width*right, -top+dist/height*top, -depth);
			scale = SimLive.COORDINATE_SYSTEM_SIZE/width*right;
		}
		else {
			gl2.glTranslated((-1.0+dist/width)/zoom, (-1.0+dist/height)*aspectRatio/zoom, 0.0);
			scale = SimLive.COORDINATE_SYSTEM_SIZE/width/zoom;		    
		}
		gl2.glMultMatrixd(getArrayFromRotationMatrix(R0.times(R), false), 0);
		
	    gl2.glPushMatrix();
		gl2.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 0);
		gl2.glRotated(90, 0, 1, 0);
		drawAxis(gl2, glu, scale, SimLive.COLOR_RED, side == Side.X || side == Side.MINUS_X, outside, inside, imageBuffer);
		gl2.glPopMatrix();
		gl2.glPushMatrix();
		gl2.glRotated(-90, 1, 0, 0);
		drawAxis(gl2, glu, scale, SimLive.COLOR_GREEN, side == Side.Y || side == Side.MINUS_Y, outside, inside, imageBuffer);
		gl2.glPopMatrix();
		drawAxis(gl2, glu, scale, SimLive.COLOR_BLUE, side == Side.Z || side == Side.MINUS_Z, outside, inside, imageBuffer);
		if (!imageBuffer) {
			gl2.glPushMatrix();
			gl2.glRotated(90, 0, 1, 0);
			drawTextToCoordinateSystem(scale, SimLive.COLOR_DARK_RED, side == Side.X || side == Side.MINUS_X, "x");
			gl2.glPopMatrix();
			gl2.glPushMatrix();
			gl2.glRotated(-90, 1, 0, 0);
			drawTextToCoordinateSystem(scale, SimLive.COLOR_DARK_GREEN, side == Side.Y || side == Side.MINUS_Y, "y");
			gl2.glPopMatrix();
			drawTextToCoordinateSystem(scale, SimLive.COLOR_DARK_BLUE, side == Side.Z || side == Side.MINUS_Z, "z");
		}
		gl2.glPopMatrix();
	}
	
	private void drawAxis(GL2 gl2, GLU glu, double scale, float[] color, boolean selected,
			GLUquadric outside, GLUquadric inside, boolean imageBuffer) {
		if (imageBuffer) {
			gl2.glDisable(GL2.GL_LIGHTING);
			gl2.glColor3fv(color, 0);
		}
		else {
			if (selected) {
				gl2.glDisable(GL2.GL_LIGHTING);
				gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
			}
			else {
	    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, color, 0);
	    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);
	    	}			
		}
		gl2.glBegin(GL2.GL_QUADS);
		gl2.glNormal3d(0, 0, 1);
		double size = SimLive.COORDINATE_SYSTEM_BOX_FRACTION*scale;
		gl2.glVertex3d(-size, -size, size);
		gl2.glVertex3d(size, -size, size);
		gl2.glVertex3d(size, size, size);
		gl2.glVertex3d(-size, size, size);
		gl2.glNormal3d(0, 0, -1);
		gl2.glVertex3d(-size, -size, -size);
		gl2.glVertex3d(-size, size, -size);
		gl2.glVertex3d(size, size, -size);
		gl2.glVertex3d(size, -size, -size);
		gl2.glEnd();
		
		if (!imageBuffer && selected) {
			gl2.glEnable(GL2.GL_LIGHTING);
			gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_SELECTION, 0);
			gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);
		}
		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*scale,
				(1f-SimLive.ARROW_HEAD_FRACTION)*scale,
				SimLive.ARROW_HEAD_FRACTION*scale, false, outside, inside);
	}
	
	private void renderSupport(GL2 gl2, Support support, double arrowSize,
			GLU glu, GLUquadric outside, GLUquadric inside, ArrayList<Object> objects) {
		boolean selected = SimLive.mode == SimLive.Mode.SUPPORTS && objects.contains(support);
		if (SimLive.settings.isShowSupports || selected) {
	    	if (selected) {
	    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
	    	}
	    	else {
	    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_BLUE, 0);
	    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);	        	
	    	}
	    	
	    	Matrix R = GeomUtility.getRotationMatrix(-support.getAngle()*Math.PI/180.0, support.getAxis().clone());
	    	double[] RR = getArrayFromRotationMatrix(R, false);
	    	boolean shift = support.isShifted;
	    	for (int n = 0; n < support.getNodes().size(); n++) {
	    		ArrayList<Set> sets = SimLive.model.getSetsByNode(support.getNodes().get(n));
	    		if (!doSetsOnlyContain(sets, Set.View.HIDDEN) || !Collections.disjoint(sets, selectedSets) || selected) {
	    			
	    			Matrix force = null;
	    			Matrix moment = null;
	    			if (SimLive.mode == Mode.RESULTS) {
		    			double[] comp = SimLive.post.getPostIncrement().getReactions(support.getNodes().get(n).getID());
		    			force = R.times(new Matrix(Arrays.copyOfRange(comp, 0, 3), 3));
		    			if (comp.length > 3) {
		    				moment = R.times(new Matrix(Arrays.copyOfRange(comp, 3, 6), 3));
		    			}
	    			}
	    			
	    			gl2.glPushMatrix();
	    			double[] coords = getCoordsWithScaledDisp(support.getNodes().get(n).getID());
			    	gl2.glTranslated(coords[0], coords[1], coords[2]);
			    	gl2.glMultMatrixd(RR, 0);
			    	if (support.isFixedDisp()[0]) {
			    		gl2.glPushMatrix();
			    		gl2.glRotated(90, 0, 1, 0);
			    		if (shift) {
			    			gl2.glTranslated(0, 0, -arrowSize);
			    		}
			    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
								(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
								SimLive.ARROW_HEAD_FRACTION*arrowSize,
								force != null && force.get(0, 0) < -SimLive.ZERO_TOL,
					    		outside, inside);
			    		if (force != null && Math.abs(force.get(0, 0)) > SimLive.ZERO_TOL) {
			    			drawTextToArrow(arrowSize, force.get(0, 0), Units.getForceUnit(), SimLive.COLOR_BLUE, false, shift);
			    		}
				    	gl2.glPopMatrix();
			    	}
			    	if (support.isFixedDisp()[1]) {
			    		gl2.glPushMatrix();
				    	gl2.glRotated(-90, 1, 0, 0);
				    	if (shift) {
				    		gl2.glTranslated(0, 0, -arrowSize);
			    		}
			    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
								(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
								SimLive.ARROW_HEAD_FRACTION*arrowSize,
								force != null && force.get(1, 0) < -SimLive.ZERO_TOL,
					    		outside, inside);
			    		if (force != null && Math.abs(force.get(1, 0)) > SimLive.ZERO_TOL) {
			    			drawTextToArrow(arrowSize, force.get(1, 0), Units.getForceUnit(), SimLive.COLOR_BLUE, false, shift);
			    		}
			    		gl2.glPopMatrix();
			    	}
			    	if (support.isFixedDisp()[2]) {
			    		gl2.glPushMatrix();
				    	if (shift) {
				    		gl2.glTranslated(0, 0, -arrowSize);
			    		}
			    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
								(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
								SimLive.ARROW_HEAD_FRACTION*arrowSize,
								force != null && force.get(2, 0) < -SimLive.ZERO_TOL,
					    		outside, inside);
			    		if (force != null && Math.abs(force.get(2, 0)) > SimLive.ZERO_TOL) {
			    			drawTextToArrow(arrowSize, force.get(2, 0), Units.getForceUnit(), SimLive.COLOR_BLUE, false, shift);
			    		}
			    		gl2.glPopMatrix();
			    	}
			    	if (support.isFixedRot()[0]) {
			    		gl2.glPushMatrix();
			    		gl2.glRotated(90, 0, 1, 0);
			    		if (!shift) {
			    			gl2.glTranslated(0, 0, -arrowSize);
			    		}
			    		drawDoubleArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
								(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
								SimLive.ARROW_HEAD_FRACTION*arrowSize,
								moment != null && moment.get(0, 0) < -SimLive.ZERO_TOL,
					    		outside, inside);
			    		if (moment != null && Math.abs(moment.get(0, 0)) > SimLive.ZERO_TOL) {
			    			drawTextToArrow(arrowSize, moment.get(0, 0), Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_BLUE, false, !shift);
			    		}
				    	gl2.glPopMatrix();
			    	}
			    	if (support.isFixedRot()[1]) {
			    		gl2.glPushMatrix();
			    		gl2.glRotated(-90, 1, 0, 0);
			    		if (!shift) {
			    			gl2.glTranslated(0, 0, -arrowSize);
			    		}
			    		drawDoubleArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
								(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
								SimLive.ARROW_HEAD_FRACTION*arrowSize,
								moment != null && moment.get(1, 0) < -SimLive.ZERO_TOL,
					    		outside, inside);
			    		if (moment != null && Math.abs(moment.get(1, 0)) > SimLive.ZERO_TOL) {
			    			drawTextToArrow(arrowSize, moment.get(1, 0), Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_BLUE, false, !shift);
			    		}
			    		gl2.glPopMatrix();
			    	}
			    	if (support.isFixedRot()[2]) {
			    		gl2.glPushMatrix();
			    		if (Model.twoDimensional) {
			    			gl2.glRotated(-90, 1, 0, 0);
			    			double radius = (1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize/2;
			    			drawRoundArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
									radius,
									SimLive.ARROW_HEAD_FRACTION*arrowSize,
									moment != null && moment.get(2, 0) < -SimLive.ZERO_TOL,
						    		outside, inside);
				    		if (moment != null && Math.abs(moment.get(2, 0)) > SimLive.ZERO_TOL) {
				    			drawTextToArrow(radius, moment.get(2, 0), Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_BLUE, false, false);
				    		}
			    		}
			    		else {
				    		if (!shift) {
				    			gl2.glTranslated(0, 0, -arrowSize);
				    		}
				    		drawDoubleArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
									(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
									SimLive.ARROW_HEAD_FRACTION*arrowSize,
									moment != null && moment.get(2, 0) < -SimLive.ZERO_TOL,
						    		outside, inside);
				    		if (moment != null && Math.abs(moment.get(2, 0)) > SimLive.ZERO_TOL) {
				    			drawTextToArrow(arrowSize, moment.get(2, 0), Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_BLUE, false, !shift);
				    		}
			    		}
			    		gl2.glPopMatrix();
			    	}
			    	gl2.glPopMatrix();
	    		}
			}
		}
	}
	
	private void drawConnector(GL2 gl2, GLU glu, Connector connector, double lineElementRadius, 
			GLUquadric inside, GLUquadric outside, ArrayList<Object> objects) {
		boolean selected = SimLive.mode == SimLive.Mode.CONNECTORS && objects.contains(connector);
		if ((connector.getSet0().view != Set.View.HIDDEN || connector.getSet1().view != Set.View.HIDDEN) ||
			selected || selectedSets.contains(connector.getSet0()) || selectedSets.contains(connector.getSet1())) {
			double[] coords = connector.getCoordinates();
			double[] disp0 = connector.getDisp(true);
			coords[0] += disp0[0];
			coords[1] += disp0[1];
			coords[2] += disp0[2];
			
			gl2.glPushMatrix();
			gl2.glTranslated(coords[0], coords[1], coords[2]);
			
			gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			gl2.glDisable(GL2.GL_LIGHTING);
			
			if (connector.getType() == Connector.Type.FIXED) {
				Matrix R = null;
				if (connector.getElement0().isPlaneElement()) {
					R = ((PlaneElement) connector.getElement0()).getR0();
				}
				else {
					R = ((LineElement) connector.getElement0()).getR0();
				}
				if (SimLive.mode == Mode.RESULTS) {
					Matrix Rg = null;
					if (connector.getElement0().isPlaneElement()) {
						PlaneElement element = (PlaneElement) SimLive.post.getSolution().getRefModel().getElements().get(connector.getElement0().getID());
						double[] shapeFunctionValues0 = element.getShapeFunctionValues(connector.getR0()[0], connector.getR0()[1]);
						Matrix u_elem = element.globalToLocalVector(SimLive.post.getPostIncrement().get_u_global()).times(SimLive.post.getScaling());
						double rx = 0, ry = 0, rz = 0;
						if (element.getType() == Element.Type.QUAD) {
							rx = element.interpolateNodeValues(shapeFunctionValues0,
									new double[]{u_elem.get(3, 0), u_elem.get(9, 0), u_elem.get(15, 0), u_elem.get(21, 0)});
							ry = element.interpolateNodeValues(shapeFunctionValues0,
									new double[]{u_elem.get(4, 0), u_elem.get(10, 0), u_elem.get(16, 0), u_elem.get(22, 0)});
							rz = element.interpolateNodeValues(shapeFunctionValues0,
									new double[]{u_elem.get(5, 0), u_elem.get(11, 0), u_elem.get(17, 0), u_elem.get(23, 0)});
						}
						if (element.getType() == Element.Type.TRI) {
							rx = element.interpolateNodeValues(shapeFunctionValues0,
									new double[]{u_elem.get(3, 0), u_elem.get(9, 0), u_elem.get(15, 0)});
							ry = element.interpolateNodeValues(shapeFunctionValues0,
									new double[]{u_elem.get(4, 0), u_elem.get(10, 0), u_elem.get(16, 0)});
							rz = element.interpolateNodeValues(shapeFunctionValues0,
									new double[]{u_elem.get(5, 0), u_elem.get(11, 0), u_elem.get(17, 0)});
						}
						Rg = Beam.rotationMatrixFromAngles(new Matrix(new double[]{rx, ry, rz}, 3));
					}
					else {
						Beam beam = (Beam) SimLive.post.getSolution().getRefModel().getElements().get(connector.getElement0().getID());
						double[] shapeFunctionValues0 = beam.getShapeFunctionValues(connector.getT0());
						Matrix u_elem = beam.globalToLocalVector(SimLive.post.getPostIncrement().get_u_global()).times(SimLive.post.getScaling());
						Rg = Beam.rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0).times(shapeFunctionValues0[0]).plus(
								u_elem.getMatrix(9, 11, 0, 0).times(shapeFunctionValues0[3])));
					}
					R = Rg.times(R);
				}
		    	gl2.glMultMatrixd(getArrayFromRotationMatrix(R, true), 0);
				double size = Math.sqrt(2)*2.5*lineElementRadius;
				gl2.glBegin(GL2.GL_LINES);
				gl2.glVertex3d(0, 0, size);
				gl2.glVertex3d(0, size, 0);
				gl2.glVertex3d(size, 0, 0);
				gl2.glVertex3d(0, size, 0);
				gl2.glVertex3d(0, 0, -size);
				gl2.glVertex3d(0, size, 0);
				gl2.glVertex3d(-size, 0, 0);
				gl2.glVertex3d(0, size, 0);
				gl2.glVertex3d(0, 0, size);
				gl2.glVertex3d(0, -size, 0);
				gl2.glVertex3d(size, 0, 0);
				gl2.glVertex3d(0, -size, 0);
				gl2.glVertex3d(0, 0, -size);
				gl2.glVertex3d(0, -size, 0);
				gl2.glVertex3d(-size, 0, 0);
				gl2.glVertex3d(0, -size, 0);
				gl2.glEnd();
				gl2.glBegin(GL2.GL_LINE_STRIP);
				gl2.glVertex3d(0, 0, size);
				gl2.glVertex3d(size, 0, 0);
				gl2.glVertex3d(0, 0, -size);
				gl2.glVertex3d(-size, 0, 0);
				gl2.glVertex3d(0, 0, size);
				gl2.glEnd();
			}
			if (connector.getType() == Connector.Type.SPHERICAL) {
				drawSphereOutline(gl2, 2.5*lineElementRadius, coords);
			}
			if (connector.getType() == Connector.Type.REVOLUTE) {
				Matrix R = ((LineElement) connector.getElement0()).getR0();
				if (SimLive.mode == Mode.RESULTS) {
					Beam beam = (Beam) SimLive.post.getSolution().getRefModel().getElements().get(connector.getElement0().getID());
					double[] shapeFunctionValues0 = beam.getShapeFunctionValues(connector.getT0());
					Matrix u_elem = beam.globalToLocalVector(SimLive.post.getPostIncrement().get_u_global()).times(SimLive.post.getScaling());
					Matrix Rg = Beam.rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0).times(shapeFunctionValues0[0]).plus(
							u_elem.getMatrix(9, 11, 0, 0).times(shapeFunctionValues0[3])));
					R = Rg.times(R);
				}
		    	gl2.glMultMatrixd(getArrayFromRotationMatrix(R, true), 0);
				gl2.glTranslated(0, 0, -2.5*lineElementRadius);
				gl2.glPushMatrix();
				gl2.glRotated(-90, 0, 1, 0);
				gl2.glLineWidth(2);
				drawCylinderOutline(gl2, 2.5*lineElementRadius, 5*lineElementRadius);
				gl2.glPopMatrix();
				drawDiskOutline(gl2, 2.5*lineElementRadius);
				gl2.glTranslated(0, 0, 5*lineElementRadius);
				drawDiskOutline(gl2, 2.5*lineElementRadius);
				gl2.glLineWidth(1);
			}
			
			if (selected) {
				gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
				gl2.glDisable(GL2.GL_LIGHTING);
			}
			else {
				gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_DARK_GRAY, 0);
				gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);
				gl2.glEnable(GL2.GL_LIGHTING);
			}
			
			if (connector.getType() == Connector.Type.FIXED) {
				double size = Math.sqrt(2)*2.5*lineElementRadius;
				for (int i = 0; i < 2; i++) {
					gl2.glBegin(GL2.GL_TRIANGLES);
					gl2.glNormal3d(1, 1, 1);
					gl2.glVertex3d(size, 0, 0);
					gl2.glVertex3d(0, size, 0);
					gl2.glVertex3d(0, 0, size);
					gl2.glNormal3d(1, 1, -1);
					gl2.glVertex3d(0, 0, -size);
					gl2.glVertex3d(0, size, 0);
					gl2.glVertex3d(size, 0, 0);
					gl2.glNormal3d(-1, 1, -1);
					gl2.glVertex3d(-size, 0, 0);
					gl2.glVertex3d(0, size, 0);
					gl2.glVertex3d(0, 0, -size);
					gl2.glNormal3d(-1, 1, 1);
					gl2.glVertex3d(0, 0, size);
					gl2.glVertex3d(0, size, 0);
					gl2.glVertex3d(-size, 0, 0);
					gl2.glEnd();
					gl2.glRotated(180, 1, 0, 0);
				}
			}
			if (connector.getType() == Connector.Type.SPHERICAL) {
				glu.gluSphere(outside, 2.5*lineElementRadius, 2*SimLive.SPHERE_SLICES, SimLive.SPHERE_STACKS);
			}
			if (connector.getType() == Connector.Type.REVOLUTE) {
				gl2.glTranslated(0, 0, -5*lineElementRadius);
				glu.gluCylinder(outside, 2.5*lineElementRadius, 2.5*lineElementRadius, 5*lineElementRadius, SimLive.LINE_SLICES, 1);
				glu.gluDisk(inside, 0, 2.5*lineElementRadius, SimLive.LINE_SLICES, 1);
				gl2.glTranslated(0, 0, 5*lineElementRadius);
				glu.gluDisk(outside, 0, 2.5*lineElementRadius, SimLive.LINE_SLICES, 1);
			}
			gl2.glPopMatrix();			
			gl2.glEnable(GL2.GL_LIGHTING);
		}
	}
	
	private void renderLoad(GL2 gl2, GLU glu, Load load, double time, double scaling, double nodeRadius,
			double arrowSize, GLUquadric outside, GLUquadric inside, ArrayList<Object> objects) {
		if (load.getTimeTable().isFactorDefinedAtTime(time)) {
			boolean selected = SimLive.mode == SimLive.Mode.LOADS && objects.contains(load);
			if (SimLive.settings.isShowLoads || selected) {
		    	if (selected) {
		    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    	}
		    	else {
		    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_RED, 0);
		    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);	        	
		    	}
		    	Matrix R = GeomUtility.getRotationMatrix(-load.getAngle()*Math.PI/180.0, load.getAxis().clone());
		    	if (SimLive.mode == Mode.RESULTS && load.getType() == Load.Type.DISPLACEMENT &&
		    			load.getReferenceNode() != null && load.getReferenceNode().isRotationalDOF()) {
		    		Matrix u_global = SimLive.post.getPostIncrement().get_u_global();
		    		int id = SimLive.post.getSolution().getDofOfNodeID(load.getReferenceNode().getID());
		    		Matrix rot = u_global.getMatrix(id+3, id+5, 0, 0);
		    		double factor = 0;
		    		if (SimLive.settings.isLargeDisplacement) {
		    			factor = scaling;
		    		}
		    		else {
		    			factor = rot.normF();
		    			if (factor > 0) {
			    			factor = Math.atan(scaling*factor)/factor;
			    		}
		    		}
		    		R = R.times(Beam.rotationMatrixFromAngles(rot.times(factor)).transpose());
		    	}
		    	double[] RR = getArrayFromRotationMatrix(R, false);
		    	boolean shift = load.isShifted;
		    	for (int n = 0; n < load.getNodes().size(); n++) {
		    		ArrayList<Set> sets = SimLive.model.getSetsByNode(load.getNodes().get(n));
		    		if (!doSetsOnlyContain(sets, Set.View.HIDDEN) || !Collections.disjoint(sets, selectedSets) || selected) {
		    			gl2.glPushMatrix();
		    			double[] coords = getCoordsWithScaledDisp(load.getNodes().get(n).getID());
				    	gl2.glTranslated(coords[0], coords[1], coords[2]);
				    	gl2.glMultMatrixd(RR, 0);
				    	if ((load.getType() == Load.Type.FORCE && load.getForce(time)[0] != 0.0) ||
				    		(load.getType() == Load.Type.DISPLACEMENT && load.isDisp()[0])) {
				    		gl2.glPushMatrix();
				    		gl2.glRotated(90, 0, 1, 0);
				    		if (shift) {
				    			gl2.glTranslated(0, 0, -arrowSize);
				    		}
				    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
									(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
									SimLive.ARROW_HEAD_FRACTION*arrowSize,
									(load.getType() == Load.Type.FORCE && load.getForce(time)[0] < 0.0) ||
						    		(load.getType() == Load.Type.DISPLACEMENT && load.getDisp(time)[0] < 0.0),
						    		outside, inside);
				    		if (load.getType() == Load.Type.FORCE) {
					    		drawTextToArrow(arrowSize, load.getForce(time)[0], Units.getForceUnit(), SimLive.COLOR_RED, selected, shift);
					    	}
					    	if (load.getType() == Load.Type.DISPLACEMENT) {
					    		drawTextToArrow(arrowSize, load.getDisp(time)[0], Units.getLengthUnit(), SimLive.COLOR_RED, selected, shift);
					    	}
					    	gl2.glPopMatrix();
				    	}
				    	if ((load.getType() == Load.Type.FORCE && load.getForce(time)[1] != 0.0) ||
					    	(load.getType() == Load.Type.DISPLACEMENT && load.isDisp()[1])) {
				    		gl2.glPushMatrix();
					    	gl2.glRotated(-90, 1, 0, 0);
					    	if (shift) {
				    			gl2.glTranslated(0, 0, -arrowSize);
				    		}
				    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
									(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
									SimLive.ARROW_HEAD_FRACTION*arrowSize,
									(load.getType() == Load.Type.FORCE && load.getForce(time)[1] < 0.0) ||
						    		(load.getType() == Load.Type.DISPLACEMENT && load.getDisp(time)[1] < 0.0),
						    		outside, inside);
				    		if (load.getType() == Load.Type.FORCE) {
					    		drawTextToArrow(arrowSize, load.getForce(time)[1], Units.getForceUnit(), SimLive.COLOR_RED, selected, shift);
					    	}
					    	if (load.getType() == Load.Type.DISPLACEMENT) {
					    		drawTextToArrow(arrowSize, load.getDisp(time)[1], Units.getLengthUnit(), SimLive.COLOR_RED, selected, shift);
					    	}
					    	gl2.glPopMatrix();
				    	}
				    	if ((load.getType() == Load.Type.FORCE && load.getForce(time)[2] != 0.0) ||
					    	(load.getType() == Load.Type.DISPLACEMENT && load.isDisp()[2])) {
				    		gl2.glPushMatrix();
					    	if (shift) {
				    			gl2.glTranslated(0, 0, -arrowSize);
				    		}
				    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
									(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
									SimLive.ARROW_HEAD_FRACTION*arrowSize,
									(load.getType() == Load.Type.FORCE && load.getForce(time)[2] < 0.0) ||
						    		(load.getType() == Load.Type.DISPLACEMENT && load.getDisp(time)[2] < 0.0),
						    		outside, inside);
				    		if (load.getType() == Load.Type.FORCE) {
					    		drawTextToArrow(arrowSize, load.getForce(time)[2], Units.getForceUnit(), SimLive.COLOR_RED, selected, shift);
					    	}
					    	if (load.getType() == Load.Type.DISPLACEMENT) {
					    		drawTextToArrow(arrowSize, load.getDisp(time)[2], Units.getLengthUnit(), SimLive.COLOR_RED, selected, shift);
					    	}
					    	gl2.glPopMatrix();
				    	}
				    	if ((load.getType() == Load.Type.FORCE && load.getMoment(time)[0] != 0.0) ||
							(load.getType() == Load.Type.DISPLACEMENT && load.isRotation()[0])) {
				    		gl2.glPushMatrix();
				    		gl2.glRotated(90, 0, 1, 0);
				    		if (!shift) {
				    			gl2.glTranslated(0, 0, -arrowSize);
				    		}
				    		drawDoubleArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
									(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
									SimLive.ARROW_HEAD_FRACTION*arrowSize,
									(load.getType() == Load.Type.FORCE && load.getMoment(time)[0] < 0.0) ||
						    		(load.getType() == Load.Type.DISPLACEMENT && load.getRotation(time)[0] < 0.0),
						    		outside, inside);			    		
				    		if (load.getType() == Load.Type.FORCE) {
					    		drawTextToArrow(arrowSize, load.getMoment(time)[0], Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_RED, selected, !shift);
					    	}
					    	if (load.getType() == Load.Type.DISPLACEMENT) {
					    		drawTextToArrow(arrowSize, load.getRotation(time)[0], "\u00B0", SimLive.COLOR_RED, selected, !shift);
					    	}
				    		gl2.glPopMatrix();
				    	}
				    	if ((load.getType() == Load.Type.FORCE && load.getMoment(time)[1] != 0.0) ||
							(load.getType() == Load.Type.DISPLACEMENT && load.isRotation()[1])) {
				    		gl2.glPushMatrix();
				    		gl2.glRotated(-90, 1, 0, 0);
				    		if (!shift) {
				    			gl2.glTranslated(0, 0, -arrowSize);
				    		}
				    		drawDoubleArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
									(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
									SimLive.ARROW_HEAD_FRACTION*arrowSize,
									(load.getType() == Load.Type.FORCE && load.getMoment(time)[1] < 0.0) ||
						    		(load.getType() == Load.Type.DISPLACEMENT && load.getRotation(time)[1] < 0.0),
						    		outside, inside);			    		
				    		if (load.getType() == Load.Type.FORCE) {
					    		drawTextToArrow(arrowSize, load.getMoment(time)[1], Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_RED, selected, !shift);
					    	}
					    	if (load.getType() == Load.Type.DISPLACEMENT) {
					    		drawTextToArrow(arrowSize, load.getRotation(time)[1], "\u00B0", SimLive.COLOR_RED, selected, !shift);
					    	}
				    		gl2.glPopMatrix();
				    	}
				    	if ((load.getType() == Load.Type.FORCE && load.getMoment(time)[2] != 0.0) ||
							(load.getType() == Load.Type.DISPLACEMENT && load.isRotation()[2])) {
				    		gl2.glPushMatrix();
				    		if (Model.twoDimensional) {
				    			gl2.glRotated(-90, 1, 0, 0);
				    			double radius = (1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize/2;
				    			drawRoundArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
										radius,
										SimLive.ARROW_HEAD_FRACTION*arrowSize,
										(load.getType() == Load.Type.FORCE && load.getMoment(time)[2] < 0.0) ||
							    		(load.getType() == Load.Type.DISPLACEMENT && load.getRotation(time)[2] < 0.0),
							    		outside, inside);
					    		if (load.getType() == Load.Type.FORCE) {
					    			drawTextToArrow(radius, load.getMoment(time)[2], Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_RED, selected, false);
						    	}
						    	if (load.getType() == Load.Type.DISPLACEMENT) {
						    		drawTextToArrow(radius, load.getRotation(time)[2], "\u00B0", SimLive.COLOR_RED, selected, false);
						    	}
				    		}
				    		else {
					    		if (!shift) {
					    			gl2.glTranslated(0, 0, -arrowSize);
					    		}
					    		drawDoubleArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
										(1f-SimLive.ARROW_HEAD_FRACTION)*arrowSize,
										SimLive.ARROW_HEAD_FRACTION*arrowSize,
										(load.getType() == Load.Type.FORCE && load.getMoment(time)[2] < 0.0) ||
							    		(load.getType() == Load.Type.DISPLACEMENT && load.getRotation(time)[2] < 0.0),
							    		outside, inside);
					    		if (load.getType() == Load.Type.FORCE) {
					    			drawTextToArrow(arrowSize, load.getMoment(time)[2], Units.getForceUnit()+Units.getLengthUnit(), SimLive.COLOR_RED, selected, !shift);
						    	}
						    	if (load.getType() == Load.Type.DISPLACEMENT) {
						    		drawTextToArrow(arrowSize, load.getRotation(time)[2], "\u00B0", SimLive.COLOR_RED, selected, !shift);
						    	}
				    		}
				    		gl2.glPopMatrix();
				    	}
				    	gl2.glPopMatrix();
		    		}
				}
			}
		}
	}
	
	private void renderDistributedLoad(GL2 gl2, GLU glu, DistributedLoad distributedLoad, double time, double scaling, double nodeRadius,
			double arrowSize, GLUquadric outside, GLUquadric inside, ArrayList<Object> objects) {
		if (distributedLoad.getTimeTable().isFactorDefinedAtTime(time)) {
			boolean selected = SimLive.mode == SimLive.Mode.LOADS && objects.contains(distributedLoad);
			if (SimLive.settings.isShowLoads || selected) {
		    	if (selected) {
		    		gl2.glColor3fv(SimLive.COLOR_SELECTION, 0);
		    	}
		    	else {
		    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, SimLive.COLOR_RED, 0);
		    		gl2.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, SimLive.COLOR_BLACK, 0);	        	
		    	}
		    	/*double phi = 0.0;
		    	if (Sim2d.mode == Mode.RESULTS && distributedLoad.getReferenceNodes() != null) {
		    		Matrix u_global = Sim2d.post.getPostIncrement().get_u_global();
		    		phi = Sim2d.post.getPostIncrement().getRotationOfReference(distributedLoad.getReferenceNodes(), u_global, scaling);
		    	}*/
		    	double xDirStartValue = distributedLoad.getStartValue(0, time);
				double xDirEndValue = distributedLoad.getEndValue(0, time);
				double yDirStartValue = distributedLoad.getStartValue(1, time);
				double yDirEndValue = distributedLoad.getEndValue(1, time);
				double zDirStartValue = distributedLoad.getStartValue(2, time);
				double zDirEndValue = distributedLoad.getEndValue(2, time);
				double scale = 2.0/3.0/Math.sqrt(Math.max(xDirStartValue*xDirStartValue+
						yDirStartValue*yDirStartValue+zDirStartValue*zDirStartValue,
						xDirEndValue*xDirEndValue+yDirEndValue*yDirEndValue+zDirEndValue*zDirEndValue));
				boolean shift = distributedLoad.isShifted;
		    	for (int s = 0; s < distributedLoad.getElementSets().size(); s++) {
		    		Set set = distributedLoad.getElementSets().get(s);
		    		if (set.view != Set.View.HIDDEN || selectedSets.contains(set) || selected) {
		    			gl2.glPushMatrix();
						
						Matrix R = GeomUtility.getRotationMatrix(-distributedLoad.getAngle()*Math.PI/180.0, distributedLoad.getAxis().clone());
				    	if (distributedLoad.isLocalSysAligned()) {
							Beam beam = (Beam) set.getElements().get(0);
							R = beam.getR0().transpose();
						}
						double[] RR = getArrayFromRotationMatrix(R, false);
				    	//gl2.glMultMatrixd(RR.getRowPackedCopy(), 0);
				    	
				    	//boolean flip = false;
		    			for (int e = 0; e < set.getElements().size()+1; e++) {
		    				int nodeID = -1;
		    				if (e < set.getElements().size()) {
		    					int[] elementNodes = set.getElements().get(e).getElementNodes();
		    					nodeID = elementNodes[0];
		    				}
		    				else {
		    					int[] elementNodes = set.getElements().get(e-1).getElementNodes();
		    					nodeID = elementNodes[1];
		    				}
		    				double[] coords = getCoordsWithScaledDisp(nodeID);
					    	gl2.glPushMatrix();
					    	gl2.glTranslated(coords[0], coords[1], coords[2]);
					    	gl2.glMultMatrixd(RR, 0);
					    	
					    	double[] comp = new double[3];
							double pos = e/(double) (set.getElements().size());
							comp[0] = xDirStartValue+pos*(xDirEndValue-xDirStartValue);
							comp[1] = yDirStartValue+pos*(yDirEndValue-yDirStartValue);
							comp[2] = zDirStartValue+pos*(zDirEndValue-zDirStartValue);
							double value = Math.sqrt(comp[0]*comp[0]+comp[1]*comp[1]+comp[2]*comp[2]);
							if (Math.abs(comp[2]+value) < SimLive.ZERO_TOL) {
								gl2.glRotated(180.0, 1, 0, 0);
							}
							else {
								gl2.glRotated(-Math.acos(comp[2]/value)*180.0/Math.PI, comp[1], -comp[0], 0);
							}
							//double angle = Math.atan2(comp[1], comp[0]);
							
							/*if (e == 0) {
								flip = angle < 0;
							}*/
							
							if (value > SimLive.ZERO_TOL) {
								if (e == 0 || e == set.getElements().size()) {
									gl2.glPushMatrix();
						    		//gl2.glRotated(90, 0, 1, 0);
						    		/*if (Sim2d.settings.isShowNodes) {
						    			gl2.glTranslated(0, 0, nodeRadius);
						    		}*/
						    		/*if (flip) {
						    			gl2.glRotated(180, 1, 0, 0);
						    		}*/
						    		if (shift) {
						    			gl2.glTranslated(0, 0, -arrowSize*value*scale);
						    		}
						    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
											arrowSize*value*scale-SimLive.ARROW_HEAD_FRACTION*arrowSize,
											SimLive.ARROW_HEAD_FRACTION*arrowSize,
											false /*flip*/,
								    		outside, inside);
							    	drawTextToArrow(arrowSize*value*scale, value, Units.getForceUnit()+"/"+Units.getLengthUnit(), SimLive.COLOR_RED, selected, shift);
							    	gl2.glPopMatrix();
								}
								else {
									gl2.glPushMatrix();
						    		//gl2.glRotated(90, 0, 1, 0);
						    		/*if (Sim2d.settings.isShowNodes) {
						    			gl2.glTranslated(0, 0, nodeRadius);
						    		}*/
						    		/*if (flip) {
						    			gl2.glRotated(180, 1, 0, 0);
						    		}*/
						    		if (shift) {
						    			gl2.glTranslated(0, 0, -arrowSize*value*scale);
						    		}
						    		drawArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize,
											arrowSize*value*scale-SimLive.ARROW_HEAD_FRACTION*arrowSize,
											SimLive.ARROW_HEAD_FRACTION*arrowSize,
											false /*flip*/,
								    		outside, inside);
							    	gl2.glPopMatrix();
								}
							}
					    	gl2.glPopMatrix();
			    		}
		    			gl2.glPopMatrix();
		    		}
				}
			}
		}
	}
	
	private void renderPrincipalVectors(GL2 gl2, GLU glu, GLUquadric outside, GLUquadric inside, float width, Set.View view, double zoom, Set set) {
		double arrowSize = 0.75*SimLive.ARROW_SIZE/width/zoom;
		double arrowLength = 0.75*SimLive.ARROW_SIZE/width/zoom0;
		double max = Math.max(Math.abs(SimLive.post.getMaxValue()), Math.abs(SimLive.post.getMinValue()));
		int inc = SimLive.post.getPostIncrementID();
		int sign = SimLive.post.getLayer() == Layer.TOP ? 1 : -1;
		String type = SimLive.post.getScalarPlot().getType();
		boolean both = type == ScalarPlot.types[22] || type == ScalarPlot.types[25] ||
				type == ScalarPlot.types[26] || type == ScalarPlot.types[27];
		boolean major = type == ScalarPlot.types[20] || type == ScalarPlot.types[23] || both;
		boolean minor = type == ScalarPlot.types[21] || type == ScalarPlot.types[24] || both;
		
		for (int e = 0; e < set.getElements().size(); e++) {
			Element element = set.getElements().get(e);
			if (element.getType() == Element.Type.TRI) {
				if (major) {
					renderPrincipalVectorsForTri(gl2, glu, (Tri) element, inc, true, max, arrowSize, arrowLength, outside, inside, sign);
				}
				if (minor) {
					renderPrincipalVectorsForTri(gl2, glu, (Tri) element, inc, false, max, arrowSize, arrowLength, outside, inside, sign);
				}
			}
			if (element.getType() == Element.Type.QUAD) {
				if (major) {
					renderPrincipalVectorsForQuad(gl2, glu, (Quad) element, inc, true, max, arrowSize, arrowLength, outside, inside, sign);
				}
				if (minor) {
					renderPrincipalVectorsForQuad(gl2, glu, (Quad) element, inc, false, max, arrowSize, arrowLength, outside, inside, sign);
				}
			}
		}
	}
	
	private void renderPrincipalVectorsForTri(GL2 gl2, GLU glu, Tri tri, int inc, boolean major,
			double max, double arrowSize, double arrowLength, GLUquadric outside, GLUquadric inside, int sign) {
		int elementID = tri.getID();
		Matrix R = new Matrix(Rr[elementID]);		
		double[] RR = getArrayFromRotationMatrix(R, true);
    	
    	double[] coords = tri.getGlobalFromLocalCoordinates(1.0/3.0, 1.0/3.0);
		
		double[] v = null;
		if (major) {
			v = SimLive.post.getTensorPlot().getMajorVectorAtGaussPoint(inc, elementID, 0);
		}
		else {
			v = SimLive.post.getTensorPlot().getMinorVectorAtGaussPoint(inc, elementID, 0);
		}
		
		double length = v[0];
		double angle = v[1];
		
		boolean flip = false;
		if (length < 0) {
			length = -length;
			flip = true;
		}
		
		if (length > max) {
			length = max;
		}
		if (max > SimLive.ZERO_TOL) {
			length *= arrowLength/max*SimLive.post.getPrincipalVectorScaling();
		}
		else {
			length = 0.0;
		}
		
		gl2.glPushMatrix();
		double offset = sign*(tri.getThickness()/2.0+3*SimLive.ARROW_RADIUS_FRACTION*arrowSize);
    	gl2.glTranslated(coords[0]+R.get(0, 2)*offset, coords[1]+R.get(1, 2)*offset, coords[2]+R.get(2, 2)*offset);
    	gl2.glMultMatrixd(RR, 0);
    	gl2.glRotatef(90, 0, 1, 0);
    	gl2.glRotated(-angle*180.0/Math.PI, 1, 0, 0);
    	
    	drawPrincipalArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize, 0.5*length-SimLive.ARROW_HEAD_FRACTION*arrowSize, SimLive.ARROW_HEAD_FRACTION*arrowSize, flip, outside, inside);
		gl2.glRotatef(180, 1, 0, 0);
		drawPrincipalArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize, 0.5*length-SimLive.ARROW_HEAD_FRACTION*arrowSize, SimLive.ARROW_HEAD_FRACTION*arrowSize, flip, outside, inside);
		gl2.glPopMatrix();
	}
	
	private void renderPrincipalVectorsForQuad(GL2 gl2, GLU glu, Quad quad, int inc, boolean major,
			double max, double arrowSize, double arrowLength, GLUquadric outside, GLUquadric inside, int sign) {
		double[][] nodeCoords = new double[4][];
    	for (int i = 0; i < 4; i++) {
    		nodeCoords[i] = getCoordsWithScaledDisp(quad.getElementNodes()[i]);
    	}
		Matrix diag0 = new Matrix(new double[]{nodeCoords[2][0]-nodeCoords[0][0],
				nodeCoords[2][1]-nodeCoords[0][1], nodeCoords[2][2]-nodeCoords[0][2]}, 3);
    	Matrix diag1 = new Matrix(new double[]{nodeCoords[3][0]-nodeCoords[1][0],
    			nodeCoords[3][1]-nodeCoords[1][1], nodeCoords[3][2]-nodeCoords[1][2]}, 3);
    	Matrix norm = diag0.crossProduct(diag1);
    	norm.timesEquals(1.0/norm.normF());
		int elementID = quad.getID();
		Matrix R = new Matrix(3, 3);
		Matrix yDir = norm.crossProduct(new Matrix(Rr[elementID]).getMatrix(0, 2, 0, 0));
		Matrix xDir = yDir.crossProduct(norm);
		R.setMatrix(0, 2, 0, 0, xDir);
		R.setMatrix(0, 2, 1, 1, yDir);
		R.setMatrix(0, 2, 2, 2, norm);
		double[] RR = getArrayFromRotationMatrix(R, true);
    	double[] center = new double[3];
    	center[0] = (nodeCoords[0][0]+nodeCoords[1][0]+nodeCoords[2][0]+nodeCoords[3][0])/4.0;
    	center[1] = (nodeCoords[0][1]+nodeCoords[1][1]+nodeCoords[2][1]+nodeCoords[3][1])/4.0;
    	center[2] = (nodeCoords[0][2]+nodeCoords[1][2]+nodeCoords[2][2]+nodeCoords[3][2])/4.0;    	
    	for (int gaussPoint = 0; gaussPoint < 4; gaussPoint++) {
			double[] coords = new double[3];
			coords[0] = center[0]+(nodeCoords[gaussPoint][0]-center[0])/Math.sqrt(3);
			coords[1] = center[1]+(nodeCoords[gaussPoint][1]-center[1])/Math.sqrt(3);
			coords[2] = center[2]+(nodeCoords[gaussPoint][2]-center[2])/Math.sqrt(3);
			
			double[] v = null;
			if (major) {
				v = SimLive.post.getTensorPlot().getMajorVectorAtGaussPoint(inc, elementID, gaussPoint);
			}
			else {
				v = SimLive.post.getTensorPlot().getMinorVectorAtGaussPoint(inc, elementID, gaussPoint);
			}
			
			double length = v[0];
			double angle = v[1];
			
			boolean flip = false;
			if (length < 0) {
				length = -length;
				flip = true;
			}
			
			if (length > max) {
				length = max;
			}
			if (max > SimLive.ZERO_TOL) {
				length *= arrowLength/max*SimLive.post.getPrincipalVectorScaling();
			}
			else {
				length = 0.0;
			}
			
			gl2.glPushMatrix();
			double offset = sign*(quad.getThickness()/2.0+3*SimLive.ARROW_RADIUS_FRACTION*arrowSize);
	    	gl2.glTranslated(coords[0]+R.get(0, 2)*offset, coords[1]+R.get(1, 2)*offset, coords[2]+R.get(2, 2)*offset);
	    	gl2.glMultMatrixd(RR, 0);
	    	gl2.glRotatef(90, 0, 1, 0);
	    	gl2.glRotated(-angle*180.0/Math.PI, 1, 0, 0);
	    	
	    	drawPrincipalArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize, 0.5*length-SimLive.ARROW_HEAD_FRACTION*arrowSize, SimLive.ARROW_HEAD_FRACTION*arrowSize, flip, outside, inside);
			gl2.glRotatef(180, 1, 0, 0);
			drawPrincipalArrow(gl2, glu, SimLive.ARROW_RADIUS_FRACTION*arrowSize, 0.5*length-SimLive.ARROW_HEAD_FRACTION*arrowSize, SimLive.ARROW_HEAD_FRACTION*arrowSize, flip, outside, inside);
			gl2.glPopMatrix();
		}
	}
	
	private void drawPrincipalArrow(GL2 gl2, GLU glu, double r, double l1, double l2, boolean flip,
			GLUquadric outside, GLUquadric inside) {
		if (l1 < 0) {
			r *= (l1+l2)/l2;
			l2 = l1+l2;
			l1 = 0.0;
		}
		gl2.glPushMatrix();
		glu.gluDisk(inside, 0, r, SimLive.ARROW_SLICES, 1);
		if (flip) {
			glu.gluCylinder(outside, r, r, l1+l2, SimLive.ARROW_SLICES, 1);
			gl2.glRotated(180, 1, 0, 0);
			gl2.glTranslated(0, 0, -l1-l2);
		}
		else {
			glu.gluCylinder(outside, r, r, l1, SimLive.ARROW_SLICES, 1);
			gl2.glTranslated(0, 0, l1);
		}
		glu.gluDisk(inside, 0, 3*r, SimLive.ARROW_SLICES, 1);
		glu.gluCylinder(outside, 3*r, 0, l2, SimLive.ARROW_SLICES, 1);
		gl2.glPopMatrix();
	}
	
	private void drawArrow(GL2 gl2, GLU glu, double r, double l1, double l2, boolean flip,
			GLUquadric outside, GLUquadric inside) {
		if (l1 < 0) {
			r *= (l1+l2)/l2;
			l2 = l1+l2;
			l1 = 0.0;
		}
		gl2.glPushMatrix();
		if (flip) {
			gl2.glTranslated(0, 0, l1+l2);
			gl2.glRotated(180, 1, 0, 0);
		}
		glu.gluDisk(inside, 0, r, SimLive.ARROW_SLICES, 1);
		glu.gluCylinder(outside, r, r, l1, SimLive.ARROW_SLICES, 1);
		gl2.glTranslated(0, 0, l1);
		glu.gluDisk(inside, 0, 3*r, SimLive.ARROW_SLICES, 1);
		glu.gluCylinder(outside, 3*r, 0, l2, SimLive.ARROW_SLICES, 1);
		gl2.glPopMatrix();
	}
	
	private void drawDoubleArrow(GL2 gl2, GLU glu, double r, double l1, double l2, boolean flip,
			GLUquadric outside, GLUquadric inside) {
		/*if (l1 < 0) {
			r *= (l1+l2)/l2;
			l2 = l1+l2;
			l1 = 0.0;
		}*/
		gl2.glPushMatrix();
		if (flip) {
			gl2.glTranslated(0, 0, l1+l2);
			gl2.glRotated(180, 1, 0, 0);
		}
		glu.gluDisk(inside, 0, r, SimLive.ARROW_SLICES, 1);
		glu.gluCylinder(outside, r, r, l1-l2, SimLive.ARROW_SLICES, 1);
		gl2.glTranslated(0, 0, l1-l2);
		glu.gluDisk(inside, 0, 3*r, SimLive.ARROW_SLICES, 1);
		glu.gluCylinder(outside, 3*r, 0, l2, SimLive.ARROW_SLICES, 1);
		gl2.glTranslated(0, 0, l2);
		glu.gluDisk(inside, 0, 3*r, SimLive.ARROW_SLICES, 1);
		glu.gluCylinder(outside, 3*r, 0, l2, SimLive.ARROW_SLICES, 1);
		gl2.glPopMatrix();
	}
	
	private void drawRoundArrow(GL2 gl2, GLU glu, double r, double radius, double arrowHeadLength,
			boolean flip, GLUquadric outside, GLUquadric inside) {
		gl2.glPushMatrix();
		final int nSegments = 20;
		double deltaPhi = 1.25*Math.PI/nSegments;
		double sliceLength = 2.0*radius*Math.sin(deltaPhi/2.0);
		if (flip) {
			gl2.glRotated(90, 0, 0, 1);
		}
		else {
			gl2.glRotated(-90, 0, 0, 1);
		}
		for (int i = 0; i < nSegments+1; i++) {
			gl2.glPushMatrix();
			double phi = i*deltaPhi;
			gl2.glTranslated(0, (radius-r)*Math.cos(phi), (radius-r)*Math.sin(phi));
			gl2.glRotated((phi+deltaPhi/2.0)*180.0/Math.PI, 1, 0, 0);
			if (i == 0) {
				glu.gluDisk(inside, 0, r, SimLive.ARROW_SLICES, 1);
			}
			if (i == nSegments) {
				glu.gluDisk(inside, 0, 3*r, SimLive.ARROW_SLICES, 1);
				glu.gluCylinder(outside, 3*r, 0, arrowHeadLength, SimLive.ARROW_SLICES, 1);
			}
			else {
				glu.gluCylinder(outside, r, r, sliceLength, SimLive.ARROW_SLICES, 1);				
			}
			gl2.glPopMatrix();
		}
		gl2.glPopMatrix();
	}
	
	private void drawTextToCoordinateSystem(double arrowSize, float[] color, boolean selected, String dir) {
		double[] t = getModelViewMatrix();
		int[] viewport = getViewport();
		double[] screen = null;
		screen = modelToScreenCoordinates(new double[]{0, 0, arrowSize});
		rendererBold.beginRendering(viewport[2], viewport[3]);
		double halfWidth = rendererBold.getBounds(dir).getWidth()/2.0;
		if (selected) {
			drawText(rendererBold, 0, 0, SimLive.COLOR_DARK_SELECTION, t, dir, screen, viewport[3], halfWidth, false);
		}
		else {
			drawText(rendererBold, 0, 0, color, t, dir, screen, viewport[3], halfWidth, false);
		}
		rendererBold.endRendering();
	}
	
	private void drawTextToArrow(double arrowSize, double value, String unit, float[] color, boolean selected, boolean shift) {
		double[] t = getModelViewMatrix();
		int[] viewport = getViewport();
		double[] screen = null;
		if (shift) {
			screen = modelToScreenCoordinates(new double[3]);
		}
		else {
			screen = modelToScreenCoordinates(new double[]{0, 0, arrowSize});
		}
		if (screen[2] < 1) {
			renderer.beginRendering(viewport[2], viewport[3]);
			String string = SimLive.double2String(Math.abs(value))+" "+unit;
		    double halfWidth = renderer.getBounds(string).getWidth()/2.0;
		    if (selected) {
				drawText(renderer, 0, 0, SimLive.COLOR_BLACK, t, string, screen, viewport[3], halfWidth, shift);
				drawText(renderer, -1, 1, SimLive.COLOR_SELECTION, t, string, screen, viewport[3], halfWidth, shift);
			}
			else {
				drawText(renderer, 0, 0, color, t, string, screen, viewport[3], halfWidth, shift);
			}
		    renderer.endRendering();
		}
	}
	
	private void drawText(TextRenderer renderer, int i, int j, float[] color, double[] t, String string, double[] screen, int height, double halfWidth, boolean shift) {
		renderer.setColor(color[0], color[1], color[2], 1.0f);
		if ((shift && t[9] > 0) || (!shift && t[9] < 0)) {
	    	renderer.draw(string, (int) (screen[0]-halfWidth+i), (int) (height-screen[1]-SimLive.fontHeight+j));
	    }
	    else {
	    	renderer.draw(string, (int) (screen[0]-halfWidth+i), (int) (height-screen[1]+descent+j));
	    }
	}

	private void drawArc(GL2 gl2, double radius, double startAngle, double angle) {
		gl2.glBegin(GL2.GL_LINE_STRIP);
		for (double phi = startAngle; phi < startAngle+angle; phi+=Math.PI/50.0) {
			gl2.glVertex2d(radius*Math.cos(phi), radius*Math.sin(phi));
		}
		gl2.glVertex2d(radius*Math.cos(startAngle+angle), radius*Math.sin(startAngle+angle));
    	gl2.glEnd();
	}
	
	private void drawSlice(GL2 gl2, double startX, double startY, double radius, double startAngle, double angle) {
		gl2.glBegin(GL2.GL_TRIANGLE_FAN);
		gl2.glVertex2d(startX, startY);
		for (double phi = startAngle; phi < startAngle+angle; phi+=Math.PI/50.0) {
			gl2.glVertex2d(radius*Math.cos(phi), radius*Math.sin(phi));
		}
		gl2.glVertex2d(radius*Math.cos(startAngle+angle), radius*Math.sin(startAngle+angle));
    	gl2.glEnd();
	}
	
	private void drawSpurGear(GL2 gl2, GLU glu, GLUquadric outside, double[] center, double z, double alpha, double m, double r, boolean isInternal) {
		
		double ra = r+m;
		double rf = r-m;
		double tipAngle = SimLive.model.toothThicknessArcLengthAtRadius(ra, r, m, alpha)/ra;
		double rootAngle = SimLive.model.toothThicknessArcLengthAtRadius(rf, r, m, alpha)/rf;
		
		gl2.glPushMatrix();
		gl2.glTranslated(center[0], center[1], 0);
		for (int t = 0; t < z; t++) {
			gl2.glRotated(360.0/z, 0, 0, 1);
			
			gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
			gl2.glBegin(GL2.GL_LINE_STRIP);
			for (int i = 0; i < 10; i++) {
				double ri = rf+i*(ra-rf)/9.0;
				double sit = SimLive.model.toothThicknessArcLengthAtRadius(ri, r, m, alpha);
				double[] coord = SimLive.model.rotatedCoords(ri, (rootAngle-sit/ri)*0.5, new double[2]);
				gl2.glVertex2d(coord[0], coord[1]);
			}
			gl2.glEnd();
			
			drawArc(gl2, ra, Math.PI/2.0+(rootAngle-tipAngle)*0.5, tipAngle);
			
			gl2.glBegin(GL2.GL_LINE_STRIP);
			for (int i = 0; i < 10; i++) {
				double ri = ra-i*(ra-rf)/9.0;
				double sit = SimLive.model.toothThicknessArcLengthAtRadius(ri, r, m, alpha);
				double[] coord = SimLive.model.rotatedCoords(ri, (rootAngle+sit/ri)*0.5, new double[2]);
				gl2.glVertex2d(coord[0], coord[1]);
			}
			gl2.glEnd();
			
			drawArc(gl2, rf, Math.PI/2.0+rootAngle, 2.0*Math.PI/z-rootAngle);
			
			gl2.glColor4fv(SimLive.COLOR_TRANSPARENT, 0);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
			if (isInternal) {
				gl2.glVertex2d(0, ra+2.0*m);
			}
			else {
				gl2.glVertex2d(0, 0);
			}
			for (int i = 0; i < 10; i++) {
				double ri = rf+i*(ra-rf)/9.0;
				double sit = SimLive.model.toothThicknessArcLengthAtRadius(ri, r, m, alpha);
				double[] coord = SimLive.model.rotatedCoords(ri, (rootAngle-sit/ri)*0.5, new double[2]);
				gl2.glVertex2d(coord[0], coord[1]);
			}
			gl2.glEnd();
			
			if (isInternal) {
				double[] coord = SimLive.model.rotatedCoords(ra, (rootAngle-tipAngle)*0.5, new double[2]);
				drawSlice(gl2, coord[0], coord[1], ra+2.0*m, Math.PI/2.0, (rootAngle-tipAngle)*0.5);
				glu.gluPartialDisk(outside, ra, ra+2.0*m, 10, 1, -(rootAngle-tipAngle)*0.5*180/Math.PI, -tipAngle*180/Math.PI);
				coord = SimLive.model.rotatedCoords(ra, (rootAngle+tipAngle)*0.5, new double[2]);
				drawSlice(gl2, coord[0], coord[1], ra+2.0*m, Math.PI/2.0+(rootAngle+tipAngle)*0.5, (rootAngle-tipAngle)*0.5);
			}
			else {
				drawSlice(gl2, 0, 0, ra, Math.PI/2.0+(rootAngle-tipAngle)*0.5, tipAngle);
			}
			
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
			if (isInternal) {
				double[] coord = SimLive.model.rotatedCoords(ra+2.0*m, rootAngle, new double[2]);
				gl2.glVertex2d(coord[0], coord[1]);
			}
			else {
				gl2.glVertex2d(0, 0);
			}
			for (int i = 0; i < 10; i++) {
				double ri = ra-i*(ra-rf)/9.0;
				double sit = SimLive.model.toothThicknessArcLengthAtRadius(ri, r, m, alpha);
				double[] coord = SimLive.model.rotatedCoords(ri, (rootAngle+sit/ri)*0.5, new double[2]);
				gl2.glVertex2d(coord[0], coord[1]);
			}
			gl2.glEnd();
			
			if (isInternal) {
				glu.gluPartialDisk(outside, rf, ra+2.0*m, 10, 1, -rootAngle*180/Math.PI, -(2*Math.PI/z-rootAngle)*180/Math.PI);
			}
			else {
				drawSlice(gl2, 0, 0, rf, Math.PI/2.0+rootAngle, 2.0*Math.PI/z-rootAngle);
			}
		}
		
		gl2.glColor3fv(SimLive.COLOR_BLACK, 0);
		gl2.glLineWidth(2);
		drawArc(gl2, r, 0.0, 2.0*Math.PI);
		gl2.glLineWidth(1);
		if (isInternal) {
			drawArc(gl2, ra+2.0*m, 0.0, 2.0*Math.PI);
		}
		gl2.glPopMatrix();
	}
	
	private static double[] getModelViewMatrix() {
		GL2 gl2 = SimLive.glcontext.getGL().getGL2();
		double[] modelview = new double[16];
	    gl2.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
	    return modelview;
	}
	
	public static int[] getViewport() {
		GL2 gl2 = SimLive.glcontext.getGL().getGL2();
		int[] viewport = new int[4];
	    gl2.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
		return viewport;
	}
	
	public static double[] modelToScreenCoordinates(double[] modelCoords) {
		double[] screenCoords = new double[3];
		GL2 gl2 = SimLive.glcontext.getGL().getGL2();
		double[] modelview = new double[16];
	    gl2.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
	    double[] projection = new double[16];
	    gl2.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);
	    int[] viewport = new int[4];
	    gl2.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
	    GLU glu = new GLU();
		glu.gluProject(modelCoords[0], modelCoords[1], modelCoords[2],
				modelview, 0, projection, 0, viewport, 0, screenCoords, 0);
		screenCoords[1] = viewport[3]-screenCoords[1];
		return screenCoords;
	}
	
	public static double[] getViewDirection(double[] modelCoords) {
		if (perspective) {
			double[] dir = new double[3];
			dir[0] = cameraRefPos[0]-modelCoords[0];
			dir[1] = cameraRefPos[1]-modelCoords[1];
			dir[2] = cameraRefPos[2]-modelCoords[2];
			double length = Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
			dir[0] /= length;
			dir[1] /= length;
			dir[2] /= length;
			return dir;
		}
		else {
			return R0.times(R).getMatrix(0, 2, 2, 2).getColumnPackedCopy();
		}
	}
	
	public double getSizeFactorPerspective(double[] modelCoords) {
		if (perspective) {
			double[] dir = new double[3];
			dir[0] = modelCoords[0]-cameraRefPos[0];
			dir[1] = modelCoords[1]-cameraRefPos[1];
			dir[2] = modelCoords[2]-cameraRefPos[2];
			double[] dir1 = new double[3];
			dir1[0] = rotPoint[0]-cameraRefPos[0];
			dir1[1] = rotPoint[1]-cameraRefPos[1];
			dir1[2] = rotPoint[2]-cameraRefPos[2];
			return (dir1[0]*dir[0]+dir1[1]*dir[1]+dir1[2]*dir[2])/
					(dir1[0]*dir1[0]+dir1[1]*dir1[1]+dir1[2]*dir1[2]);
		}
		else {
			return 1.0;
		}
	}
	
	public int getLineDivisions(LineElement element) {
		int[] viewport = getViewport();
		double screenLength = element.getLength()*viewport[2]*zoom;
		return Math.min((int) (screenLength/SimLive.LINE_DIVISIONS_SIZE)+1, SimLive.LINE_DIVISIONS_MAX);
	}
	
	public int getCylindricSectionSlices(double radius) {
		return Math.min(2*(int) (radius*zoom*SimLive.CYLINDRIC_SECTION_SLICES_MAX/2+4), SimLive.CYLINDRIC_SECTION_SLICES_MAX);
	}
	
	public static double[] screenToModelCoordinates(double screenX, double screenY) {
		double[] modelCoords = new double[3];
		GL2 gl2 = SimLive.glcontext.getGL().getGL2();
		double[] modelview = new double[16];
	    gl2.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
	    double[] projection = new double[16];
	    gl2.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);
	    int[] viewport = new int[4];
	    gl2.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
	    GLU glu = new GLU();
		glu.gluUnProject(screenX, viewport[3]-screenY, 0.5,
				modelview, 0, projection, 0, viewport, 0, modelCoords, 0);
		double[] dir = getViewDirection(modelCoords);
		return GeomUtility.getIntersectionLinePlane(modelCoords, dir,
				new double[3], new double[]{1, 0, 0}, new double[]{0, 1, 0});
	}
	
	private void setSideOfCoordinateSystem(int screenX, int screenY) {		
		int[] viewport = getViewport();
		screenY = viewport[3] - screenY;
	    side = Side.NONE;
    	if (imgBuffer != null && selectedLabel == null && selectedMeasurement == null) {
			int length = (int) SimLive.COORDINATE_SYSTEM_SIZE*2;
	    	if (screenX > -1 && screenX < length && screenY > -1 && screenY < length) {
		    	float r = imgBuffer.get(screenX*3+screenY*3*length);
		    	float g = imgBuffer.get(screenX*3+screenY*3*length+1);
		    	float b = imgBuffer.get(screenX*3+screenY*3*length+2);
		    	float max = Math.max(r, Math.max(g, b));
		    	if (max > 0) {
		    		Matrix viewDir = new Matrix(new double[]{0, 0, -1}, 3);
		    		if (perspective) {
		    			final double dist = 1.2*SimLive.COORDINATE_SYSTEM_SIZE;
		    			double z = viewport[3]/2.0/Math.tan(fovy/2.0);
		    			viewDir = new Matrix(new double[]{-viewport[2]/2.0+dist, -viewport[3]/2.0+dist, -z}, 3);
		    	    }
		    	    if (r == max) {
		    	    	if (viewDir.dotProduct(R0.getMatrix(0, 0, 0, 2).transpose()) > SimLive.ZERO_TOL) {
			    			side = Side.MINUS_X;
			    		}
			    		else {
			    			side = Side.X;
			    		}
			    	}
			    	if (g == max) {
			    		if (viewDir.dotProduct(R0.getMatrix(1, 1, 0, 2).transpose()) > SimLive.ZERO_TOL) {
			    			side = Side.MINUS_Y;
			    		}
			    		else {
			    			side = Side.Y;
			    		}
			    	}
			    	if (b == max) {
			    		if (viewDir.dotProduct(R0.getMatrix(2, 2, 0, 2).transpose()) > SimLive.ZERO_TOL) {
			    			side = Side.MINUS_Z;
			    		}
			    		else {
			    			side = Side.Z;
			    		}
			    	}
		    	}
	    	}
	    }
	}

}

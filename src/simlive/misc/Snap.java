package simlive.misc;

import java.util.ArrayList;

import simlive.SimLive;
import simlive.model.ContactPair;
import simlive.model.Element;
import simlive.model.Node;
import simlive.model.Part3d;
import simlive.model.Set;
import simlive.view.Label;
import simlive.view.View;
import simlive.view.View.Side;

public abstract class Snap {

	public static double[] coords2d;
	public static double[] coords3d;
	public static Node node = null;
	public static Element element = null;
	public static ContactPair contactPair = null;
	public static Set set = null;
	public static Part3d part3d = null;
	private static boolean snapToGrid = false;
	private static boolean snapToObject = false;
	
	public static void snapAndSetText(int[] mousePos, boolean isMouseDragged, Node moveNode) {
		
		if (snapToObject) {		
			/* search */		
			node = Search.getNodeAtPoint(mousePos, isMouseDragged, moveNode);
			int[] projectedPoint = new int[2];
			if (node == null && !isMouseDragged) {
				element = Search.getElementAtPoint(true, mousePos, projectedPoint);
				contactPair = Search.getContactPairAtPoint(mousePos);
				if (!SimLive.view.lockSelectParts3d) {
					part3d = Search.getPart3dAtPoint(mousePos);
				}
				else {
					part3d = null;
				}
				element = Search.getSelectedOrPinnedElementAtPoint(mousePos, projectedPoint);
			}
			
			/* set */
			if (!isMouseDragged) {
				set = SimLive.model.getSetByElement(element);
				ArrayList<Set> setsByNode = SimLive.model.getSetsByNode(node);
				if (!setsByNode.isEmpty() && !setsByNode.contains(set)) {
					set = setsByNode.get(0);
				}
			}
			
			/* snap */
			if (node != null) {
				snapToNode(node, mousePos);
			}
			else if (element != null && !isMouseDragged) {
				snapToElement(element, mousePos, projectedPoint);
			}
			else if (Snap.snapToGrid) {
				snapToGrid(mousePos);
			}
			
			/* text to labelAtMousePos and statusBar */
			if (!isMouseDragged && View.side == Side.NONE &&
					SimLive.view.selectedLabel == null && SimLive.view.selectedMeasurement == null) {
				String[] strArray = new String[2];
				if (node != null && set != null) {
					if (element != null) {
						SimLive.view.labelAtMousePos = Label.getNewLabelForElement(element);
					}
					else {
						SimLive.view.labelAtMousePos = Label.getNewLabelForNode(node);
					}
					if (SimLive.select != SimLive.Select.LABEL) {
						strArray[0] = getTextForNode(node);
						SimLive.view.labelAtMousePos.setText(strArray[0], strArray[1], null);
					}
				}
				else if (element != null) {
					SimLive.view.labelAtMousePos = Label.getNewLabelForElement(element);
					if (SimLive.select != SimLive.Select.LABEL) {
						strArray = getTextForElement(element);
						SimLive.view.labelAtMousePos.setText(strArray[0], strArray[1], null);
					}
				}
				else if (contactPair != null) {
					SimLive.view.labelAtMousePos = new Label(null);
					strArray = getTextForContactPair(contactPair);
					SimLive.view.labelAtMousePos.setText(strArray[0], strArray[1], null);
				}
				else if (part3d != null) {
					SimLive.view.labelAtMousePos = new Label(null);
					strArray = getTextForPart3d(part3d);
					SimLive.view.labelAtMousePos.setText(strArray[0], strArray[1], null);
				}
				
				if (SimLive.view.labelAtMousePos != null) {
					SimLive.view.labelAtMousePos.toStatusBar();
				}
			}
		}
		else if (Snap.snapToGrid) {
			snapToGrid(mousePos);
		}
		
		if (coords3d != null && !isMouseDragged) {
			coordsToStatusBar(coords3d);
		}
		else {
			coordsToStatusBar(coords2d);
		}
	}
	
	public static void resetData() {
		Snap.node = null;
		Snap.element = null;
		Snap.contactPair = null;
		Snap.set = null;
		Snap.part3d = null;		
	}
	
	public static void initData(int[] mousePos, boolean snapToGrid, boolean snapToObject) {
		SimLive.view.labelAtMousePos = null;
		Snap.snapToGrid = snapToGrid;
		Snap.snapToObject = snapToObject;
		coords2d = View.screenToModelCoordinates(mousePos[0], mousePos[1]);
		coords2d[2] = 0;
		coords3d = null;
		SimLive.statusBar.setText("");
		if (Snap.snapToGrid) {
			Search.getCoordsAtGridLine(mousePos, SimLive.SNAP_TOL);
		}
	}
	
	private static void snapToGrid(int[] point) {
		if (coords2d != null) {
			double[] coords2dOriginal = View.screenToModelCoordinates(point[0], point[1]);
			if ((coords2d[0]-coords2dOriginal[0])*(coords2d[0]-coords2dOriginal[0])+
				(coords2d[1]-coords2dOriginal[1])*(coords2d[1]-coords2dOriginal[1]) > 0) {
				/* snap to grid */
				double[] p = View.modelToScreenCoordinates(coords2d);
				SimLive.view.mouseMove((int) Math.round(p[0]-point[0]), (int) Math.round(p[1]-point[1]));			
				//coords = node.getCoords();
				//coordsToStatusBar(coords);
			}
		}
	}
	
	private static void snapToElement(Element element, int[] point, int[] projectedPoint) {
		if (element != null) {
			if (element.isLineElement()) {
				/* snap to found LineElement */
				SimLive.view.mouseMove(projectedPoint[0]-point[0], projectedPoint[1]-point[1]);
				//coords = projectedPoint;
				//coordsToStatusBar(coords);
			}
		}
	}

	private static void snapToNode(Node node, int[] point) {
		if (node != null) {
			/* snap to node */
			double[] coords = View.getCoordsWithScaledDisp(node.getID());
			double[] p = View.modelToScreenCoordinates(coords);
			SimLive.view.mouseMove((int) p[0]-point[0], (int) p[1]-point[1]);
			//coords = View.getCoordsWithScaledDisp(node.getID());
			//coordsToStatusBar(modelCoords);
		}
	}
	
	private static String[] getTextForElement(Element element) {
		String[] strArray = new String[2];
		if (set != null) {
			if (set.getElements().size() > 1) {
				strArray[0] = "Part";
				strArray[1] = "(" + set.getElements().size() + " Elements)";
			}
			else {
				int elemID = element.getID();
				strArray[0] = element.getTypeString() + " " + (elemID+1);
			}
		}
		else {
			strArray[0] = "Edge";
		}
		return strArray;
	}
	
	private static String[] getTextForPart3d(Part3d part3d) {
		String[] strArray = new String[2];
		strArray[0] = "3D-Part";
		strArray[1] = "("+part3d.getNrFacets()+" Facets)";
		return strArray;
	}
	
	private static String[] getTextForContactPair(ContactPair contactPair) {
		String[] strArray = new String[2];
		strArray[0] = "Rigid Contact";
		strArray[1] = "("+contactPair.getRigidElements().size()+" Facets)";
		return strArray;
	}

	private static String getTextForNode(Node node) {
		int n = node.getID();
		return "Node " + (n+1);
	}

	private static void coordsToStatusBar(double[] coords) {
		if (coords != null) {
			SimLive.xCoord.setText(new String(SimLive.double2String(coords[0]) + " " +
					Units.getLengthUnit()));
			SimLive.yCoord.setText(new String(SimLive.double2String(coords[1]) + " " +
					Units.getLengthUnit()));
			SimLive.zCoord.setText(new String(SimLive.double2String(coords[2]) + " " +
					Units.getLengthUnit()));
		}
		else {
			SimLive.xCoord.setText("");
			SimLive.yCoord.setText("");
			SimLive.zCoord.setText("");
		}
	}

	
}

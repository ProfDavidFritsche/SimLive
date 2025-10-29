package simlive.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.model.Beam;
import simlive.model.ContactPair;
import simlive.model.ContactPair.Type;
import simlive.model.Element;
import simlive.model.Facet3d;
import simlive.model.LineElement;
import simlive.model.Model;
import simlive.model.Node;
import simlive.model.Part3d;
import simlive.model.PlaneElement;
import simlive.model.Set;
import simlive.view.Label;
import simlive.view.Measurement;
import simlive.view.View;

public abstract class Search {
	
	private static Node node;
	private static Element element;
	private static ContactPair contactPair;
	private static Part3d part3d;
	private static Facet3d facet3d;
	private final static double minSqrDistNode = SimLive.NODE_RADIUS/2.0 * SimLive.NODE_RADIUS/2.0;
	private final static double minSqrDistLine = SimLive.LINE_ELEMENT_RADIUS/2.0 * SimLive.LINE_ELEMENT_RADIUS/2.0;
	private final static double minSqrDistPointMass = SimLive.POINT_MASS_RADIUS * SimLive.POINT_MASS_RADIUS;
	private static double zCoord;
	
	public static Node getNodeAtPoint(int[] point, boolean isMouseDragged, Node moveNode) {
		Search.node = null;
		Search.zCoord = Double.MAX_VALUE;
		// sets: 1st sets by selected node, 2nd selected sets, 3rd remaining sets
		// isMouseDragged: only remaining sets
		ArrayList<Set> sets = new ArrayList<Set>();
		if (!isMouseDragged && !SimLive.view.getSelectedNodes().isEmpty()) {
			sets.addAll(SimLive.model.getSetsByNode(SimLive.view.getSelectedNodes().get(0)));
		}
		sets.addAll(SimLive.model.getSets());
		for (int s = sets.size()-1; s > -1; s--) {
			if (SimLive.view.getSelectedSets().contains(sets.get(s))) {
				if (isMouseDragged) {
					sets.remove(s);
				}
				else {
					sets.add(0, sets.get(s));
					sets.remove(s+1);
				}
			}
			else if (sets.get(s).view == Set.View.HIDDEN) {
				sets.remove(s);
			}
		}
		Set set = new Set(Set.Type.BASIC);
		set.view = Set.View.HIDDEN;
		sets.add(0, set);
		for (int s = 0; s < sets.size(); s++) {
			nodeAtPointInSet(point, sets.get(s), isMouseDragged, moveNode);
		}
		//nodeAtPointInSet(point, Sim2d.view.getSelectedNodesAsSet());
		
		//unused nodes
		if (SimLive.model.settings.newPartType != Element.Type.SPUR_GEAR) {
			for (int n = Model.maxUsedNodeID+1; n < SimLive.model.getNodes().size(); n++) {
				Node node = SimLive.model.getNodes().get(n);
				nodeAtPoint(point, node, isMouseDragged, moveNode);
			}
		}
		
		return Search.node;
	}
	
	public static void getCoordsAtGridLine(int[] point, final double snapTol) {
		double gridSize = SimLive.model.settings.meshSize;
		long i = Math.round(Snap.coords2d[0]/gridSize);
		long j = Math.round(Snap.coords2d[1]/gridSize);
		double[] coordsGridPoint = new double[2];
		coordsGridPoint[0] = i*gridSize;
		coordsGridPoint[1] = j*gridSize;
		if (SimLive.model.settings.isShowGrid && Math.abs(View.R0.get(2, 2)) > SimLive.ZERO_TOL &&
				i <= SimLive.model.settings.meshCount/2 && i >= -SimLive.model.settings.meshCount/2 &&
				j <= SimLive.model.settings.meshCount/2 && j >= -SimLive.model.settings.meshCount/2) {
			
			double[] grid = View.modelToScreenCoordinates(new double[]{
					coordsGridPoint[0], coordsGridPoint[1], 0});
			double[] diff = new double[2];
			diff[0] = point[0]-grid[0];
			diff[1] = point[1]-grid[1];
			double length = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]);
			if (length < snapTol) {
				Snap.coords2d[0] = coordsGridPoint[0];
				Snap.coords2d[1] = coordsGridPoint[1];
				return;
			}
			
			{
				double[] gridY = View.modelToScreenCoordinates(new double[]{
						coordsGridPoint[0], (j+1)*gridSize, 0});
				double[] norm = new double[2];
				norm[0] = grid[1]-gridY[1];
				norm[1] = gridY[0]-grid[0];
				length = Math.sqrt(norm[0]*norm[0]+norm[1]*norm[1]);
				norm[0] /= length;
				norm[1] /= length;
				double scal = diff[0]*norm[0]+diff[1]*norm[1];
				if (Math.abs(scal) < snapTol) {
					Snap.coords2d[0] = coordsGridPoint[0];
					return;
				}
			}
			
			{
				double[] gridX = View.modelToScreenCoordinates(new double[]{
						(i+1)*gridSize, coordsGridPoint[1], 0});
				double[] norm = new double[2];
				norm[0] = grid[1]-gridX[1];
				norm[1] = gridX[0]-grid[0];
				length = Math.sqrt(norm[0]*norm[0]+norm[1]*norm[1]);
				norm[0] /= length;
				norm[1] /= length;
				double scal = diff[0]*norm[0]+diff[1]*norm[1];
				if (Math.abs(scal) < snapTol) {
					Snap.coords2d[1] = coordsGridPoint[1];
					return;
				}
			}
		}
	}
	
	public static Element getElementAtPoint(boolean skipSelectedAndPinned, int[] point, int[] projectedPoint) {
		Search.element = null;
		Search.zCoord = Double.MAX_VALUE;
		int[] viewport = View.getViewport();
		double[] q = View.screenToModelCoordinates(point[0], point[1]);
		Matrix q0 = new Matrix(new double[]{q[0], q[1], q[2]}, 3);
		Matrix lookAt = new Matrix(View.getViewDirection(q), 3);
		ArrayList<Set> sets = new ArrayList<Set>();
		sets.addAll(SimLive.model.getSets());
		if (skipSelectedAndPinned) {
			sets.removeAll(SimLive.view.getSelectedSets());
			Predicate<Set> pr = (Set set)->(set.view == Set.View.PINNED);
			sets.removeIf(pr);
		}
		for (int s = 0; s < sets.size(); s++) {
			if (sets.get(s).view != Set.View.HIDDEN) {
				elementAtPointInSet(point, sets.get(s), projectedPoint, viewport, q0, lookAt);
			}
		}
		
		return Search.element;
	}
	
	public static Element getSelectedOrPinnedElementAtPoint(int[] point, int[] projectedPoint) {
		Search.zCoord = Double.MAX_VALUE;
		int[] viewport = View.getViewport();
		double[] q = View.screenToModelCoordinates(point[0], point[1]);
		Matrix q0 = new Matrix(new double[]{q[0], q[1], q[2]}, 3);
		Matrix lookAt = new Matrix(View.getViewDirection(q), 3);
		ArrayList<Set> sets = new ArrayList<Set>();
		sets.addAll(SimLive.view.getSelectedSets());
		for (int s = 0; s < SimLive.model.getSets().size(); s++) {
			if (SimLive.model.getSets().get(s).view == Set.View.PINNED &&
					!SimLive.view.getSelectedSets().contains(SimLive.model.getSets().get(s))) {
				sets.add(SimLive.model.getSets().get(s));
			}
		}
		for (int s = 0; s < sets.size(); s++) {
			elementAtPointInSet(point, sets.get(s), projectedPoint, viewport, q0, lookAt);
		}
		
		return Search.element;
	}
	
	public static ContactPair getContactPairAtPoint(int[] point) {
		Search.contactPair = null;
		for (int c = 0; c < SimLive.model.getContactPairs().size(); c++) {
			if (SimLive.model.getContactPairs().get(c).getType() == Type.RIGID_DEFORMABLE) {
				ContactPair contactPair = SimLive.model.getContactPairs().get(c);
				ArrayList<Node> nodes = contactPair.getRigidNodes();
				for (int s = 0; s < contactPair.getMasterSets().size(); s++) {
					if (contactPair.getMasterSets().get(s).view != Set.View.HIDDEN) {
						rigidElementAtPointInSet(point, contactPair, contactPair.getMasterSets().get(s), nodes);
					}
				}
			}
		}
		
		return Search.contactPair;
	}
	
	public static Part3d getPart3dAtPoint(int[] point) {
		Search.part3d = null;
		Search.facet3d = null;
		//Search.zCoord = Double.MAX_VALUE;
		ArrayList<Part3d> parts3d = new ArrayList<Part3d>();
		parts3d.addAll(SimLive.model.getParts3d());
		parts3d.removeAll(SimLive.view.getSelectedParts3d());
		parts3d.addAll(0, SimLive.view.getSelectedParts3d());
		double[] screenCoords = new double[2];
	    screenCoords[0] = point[0];
	    screenCoords[1] = point[1];
	    double[] linePoint = View.screenToModelCoordinates(screenCoords[0], screenCoords[1]);
		double[] lineDir = View.getViewDirection(linePoint);
		for (int s = 0; s < parts3d.size(); s++) {
			if (s == SimLive.view.getSelectedParts3d().size() && Search.part3d != null) break;
			Part3d part3d = parts3d.get(s);
			if (isPointInPart3dBoundingBox(part3d, screenCoords)) {
				Stream<Facet3d> stream = Arrays.stream(part3d.getFacets()).parallel();
				stream.forEach(facet -> {
					double[][] p = new double[3][3];
					for (int i = 0; i < 3; i++) {
						if (View.pVertices != null && View.pVertices.length > part3d.getID() &&
								View.pVertices[part3d.getID()].length > facet.getIndices()[i]) {
							p[i] = View.pVertices[part3d.getID()][facet.getIndices()[i]];
						}
					}
					double[] pointOut = new double[3];
					if (p[0][2] < 1 && p[1][2] < 1 && p[2][2] < 1 &&
						GeomUtility.isPointInTriangle(p[0], p[1], p[2], screenCoords, pointOut)) {
						synchronized(Search.class) {
							if (pointOut[2] < Search.zCoord) {
								Search.zCoord = pointOut[2];
								Search.part3d = part3d;
								Search.facet3d = facet;
								Search.element = null;
								Snap.element = null;
								Snap.contactPair = null;
								double[][] coords = new double[3][3];
								for (int i = 0; i < 3; i++) {
									coords[i] = part3d.getVertexCoords()[facet.getIndices()[i]];
								}
								Snap.coords3d = GeomUtility.getIntersectionLinePlane(linePoint, lineDir, coords[0], coords[1], coords[2]);
							}
						}
					}
				});
			}
		}
		
		return Search.part3d;
	}
	
	public static Facet3d getFacet3d() {
		return Search.facet3d;
	}
	
	public static Element[] getElementsAtConnector(double[] point, Set set0, Set set1) {
		Element element0 = null, element1 = null;
		double minDist0 = Double.MAX_VALUE;
		double minDist1 = Double.MAX_VALUE;
		double minAngle0 = Double.MAX_VALUE;
		double minAngle1 = Double.MAX_VALUE;
		for (int e0 = 0; e0 < set0.getElements().size(); e0++) {
			Element elem0 = set0.getElements().get(e0);
			int[] elem0_nodes = elem0.getElementNodes();
			
			if (elem0.isPlaneElement() && ((PlaneElement) elem0).isPointInElement(point)) {
				element0 = elem0;
				minDist0 = 0.0;
				minAngle0 = 0.0;
			}
			
			for (int i = 0; i < elem0_nodes.length; i++) {
				double dist = GeomUtility.distancePointToLineSegment(point,
						SimLive.model.getNodes().get(elem0_nodes[i]).getCoords(),
						SimLive.model.getNodes().get(elem0_nodes[(i+1)%elem0_nodes.length]).getCoords());
				
				if (dist < minDist0) {
					minDist0 = dist;
					element0 = elem0;
				}
				else if (dist == minDist0) {
					double angle = GeomUtility.anglePointToLineSegmentNormal(point,
							SimLive.model.getNodes().get(elem0_nodes[i]).getCoords(),
							SimLive.model.getNodes().get(elem0_nodes[(i+1)%elem0_nodes.length]).getCoords());
					if (angle < minAngle0) {
						minAngle0 = angle;
						element0 = elem0;
					}
				}
			}
					
			for (int e1 = 0; e1 < set1.getElements().size(); e1++) {
				Element elem1 = set1.getElements().get(e1);						
				int[] elem1_nodes = elem1.getElementNodes();
				
				if (elem1.isPlaneElement() && ((PlaneElement) elem1).isPointInElement(point)) {
					element1 = elem1;
					minDist1 = 0.0;
					minAngle1 = 0.0;
				}
				
				for (int i = 0; i < elem1_nodes.length; i++) {
					double dist = GeomUtility.distancePointToLineSegment(point,
							SimLive.model.getNodes().get(elem1_nodes[i]).getCoords(),
							SimLive.model.getNodes().get(elem1_nodes[(i+1)%elem1_nodes.length]).getCoords());
					
					if (dist < minDist1) {
						minDist1 = dist;
						element1 = elem1;
					}
					else if (dist == minDist1) {
						double angle = GeomUtility.anglePointToLineSegmentNormal(point,
								SimLive.model.getNodes().get(elem1_nodes[i]).getCoords(),
								SimLive.model.getNodes().get(elem1_nodes[(i+1)%elem1_nodes.length]).getCoords());
						if (angle < minAngle1) {
							minAngle1 = angle;
							element1 = elem1;
						}
					}
				}
			}
		}
		if (element0 != null && element1 != null) {
			return new Element[]{element0, element1};
		}
		
		return null;
	}
	
	public static Label getLabelAtPoint(int[] point) {
		for (int l = SimLive.view.labels.size()-1; l > -1; l--) {
			if (SimLive.view.labels.get(l).isPointInside(point)) {
				return SimLive.view.labels.get(l);
			}
		}
		return null;
	}

	public static Measurement getMeasurementAtPoint(int[] point) {
		for (int m = SimLive.view.measurements.size()-1; m > -1; m--) {
			if (SimLive.view.measurements.get(m).isPointInside(point)) {
				return SimLive.view.measurements.get(m);
			}
		}
		return null;
	}
	
	private static boolean isPointInPart3dBoundingBox(Part3d part3d, double[] point) {
		if (View.pPart3dBox.length > part3d.getID()) {
			double[][] p = View.pPart3dBox[part3d.getID()];
			if (View.R0.get(2, 2) > 0) {
				if (GeomUtility.isPointInTriangle(p[5], p[6], p[7], point, null)) return true;
				if (GeomUtility.isPointInTriangle(p[4], p[5], p[7], point, null)) return true;
			}
			else {
				if (GeomUtility.isPointInTriangle(p[0], p[1], p[2], point, null)) return true;
				if (GeomUtility.isPointInTriangle(p[0], p[2], p[3], point, null)) return true;
			}
			if (View.R0.get(0, 2) > 0) {
				if (GeomUtility.isPointInTriangle(p[1], p[2], p[6], point, null)) return true;
				if (GeomUtility.isPointInTriangle(p[1], p[6], p[5], point, null)) return true;
			}
			else {
				if (GeomUtility.isPointInTriangle(p[0], p[3], p[7], point, null)) return true;
				if (GeomUtility.isPointInTriangle(p[0], p[7], p[4], point, null)) return true;
			}
			if (View.R0.get(1, 2) > 0) {
				if (GeomUtility.isPointInTriangle(p[2], p[3], p[7], point, null)) return true;
				if (GeomUtility.isPointInTriangle(p[2], p[7], p[6], point, null)) return true;
			}
			else {
				if (GeomUtility.isPointInTriangle(p[0], p[4], p[5], point, null)) return true;
				if (GeomUtility.isPointInTriangle(p[0], p[5], p[1], point, null)) return true;
			}		
		}
		return false;
	}

	private static void nodeAtPointInSet(int[] point, Set set, boolean isMouseDragged, Node moveNode) {
		ArrayList<Node> nodeSet = set.getNodes();
		for (int n = 0; n < nodeSet.size(); n++) {
			nodeAtPoint(point, nodeSet.get(n), isMouseDragged, moveNode);
		}
	}
	
	private static void nodeAtPoint(int[] point, Node node, boolean isMouseDragged, Node moveNode) {
		double[] modelCoords = View.getCoordsWithScaledDisp(node.getID());
		double[] coords = View.modelToScreenCoordinates(modelCoords);
		
		double sqrDist = (point[0]-coords[0])*(point[0]-coords[0]) +
		                 (point[1]-coords[1])*(point[1]-coords[1]);
		double factor = SimLive.view.getSizeFactorPerspective(modelCoords);
		sqrDist *= factor*factor;
		
		if (sqrDist < Search.minSqrDistNode && (!isMouseDragged || moveNode != node)) {
			double zCoord = coords[2];
			if (zCoord < Search.zCoord) {
				Search.zCoord = zCoord;
				Search.node = node;
				Snap.coords3d = modelCoords.clone();
			}
		}
	}
	
	private static void elementAtPointInSet(int[] point, Set set, int[] projectedPoint, int[] viewport,
			Matrix q0, Matrix lookAt) {
		ArrayList<Element> elementSet = set.getElements();
		double[] modelCoords = q0.getColumnPackedCopy();
		for (int e = 0; e < elementSet.size(); e++) {
			Element elem = elementSet.get(e);
			if (elem.getType() == Element.Type.POINT_MASS) {
				int[] elemNodes = elem.getElementNodes();
				Node node = SimLive.model.getNodes().get(elemNodes[0]);
				double[] coords0 = View.getCoordsWithScaledDisp(node.getID());
				double[] p = View.modelToScreenCoordinates(coords0);				
				double sqrDist = (p[0]-point[0])*(p[0]-point[0])+(p[1]-point[1])*(p[1]-point[1]);
				double factor = SimLive.view.getSizeFactorPerspective(coords0);
				sqrDist *= factor*factor;
				if (sqrDist < Search.minSqrDistPointMass) {
					double[] diff = new double[3];
					diff[0] = coords0[0]-modelCoords[0];
					diff[1] = coords0[1]-modelCoords[1];
					diff[2] = coords0[2]-modelCoords[2];
					double scal = diff[0]*lookAt.get(0, 0)+diff[1]*lookAt.get(1, 0)+diff[2]*lookAt.get(2, 0);
					diff[0] -= scal*lookAt.get(0, 0);
					diff[1] -= scal*lookAt.get(1, 0);
					diff[2] -= scal*lookAt.get(2, 0);
					double dist = Math.sqrt(diff[0]*diff[0]+diff[1]*diff[1]+diff[2]*diff[2]);
					double[] coords = new double[3];
					coords[0] = coords0[0]+dist*lookAt.get(0, 0);
					coords[1] = coords0[1]+dist*lookAt.get(1, 0);
					coords[2] = coords0[2]+dist*lookAt.get(2, 0);
					double zCoord = View.modelToScreenCoordinates(coords)[2];
					if (zCoord < Search.zCoord) {
						Search.zCoord = zCoord;
						Search.element = elem;
						Snap.coords3d = coords0;
					}
				}
			}
			if (elem.isLineElement() && View.isPointInElementBBox(elem.getID(), point[0], point[1])) {
				int[] elemNodes = elem.getElementNodes();
				Node node0 = SimLive.model.getNodes().get(elemNodes[0]);
				Node node1 = SimLive.model.getNodes().get(elemNodes[1]);
				double[] c0 = View.getCoordsWithScaledDisp(node0.getID());
				double[] c1 = View.getCoordsWithScaledDisp(node1.getID());
				Matrix p0 = new Matrix(c0, 3);
				Matrix p1 = new Matrix(c1, 3);
				Matrix dir0 = p1.minus(p0);
				double length0 = dir0.normF();
				dir0.timesEquals(1.0/length0);
				Matrix d = null;
				if (elem.getType() == Element.Type.BEAM && SimLive.mode == Mode.RESULTS) {
					Beam beam = (Beam) elem;
					Matrix d0 = p0;
					int lineDivisions = SimLive.view.getLineDivisions(beam);
		    		for (int i = 1; i < lineDivisions+1; i++) {
		    			Matrix Rr = new Matrix(View.Rr[beam.getID()]);
		    			double[][] angles = SimLive.post.getPostIncrement().getAnglesBeam(beam.getID());
		    			double x1 = i/(double) lineDivisions;	
	    				double[] disp = beam.getBendingDispInCoRotatedFrame(x1, angles);
						disp[0] *= SimLive.post.getScaling();
						disp[1] *= SimLive.post.getScaling();						
	    				Matrix d1 = Rr.times(new Matrix(new double[]{x1*length0, disp[0], disp[1]}, 3)).plus(p0);		    			
	    				Matrix dir1 = d1.minus(d0);
						double sectionLength = dir1.normF();
						dir1.timesEquals(1.0/sectionLength);
						Matrix n = dir1.crossProduct(lookAt);
						Matrix n2 = lookAt.crossProduct(n);
						double x = (q0.minus(d0)).dotProduct(n2)/dir1.dotProduct(n2);
						if (x > 0.0 && x < sectionLength) {
							d = d0.plus(dir1.times(x));
						}
						d0 = d1;
					}
				}
				else {
					Matrix n = dir0.crossProduct(lookAt);
					Matrix n2 = lookAt.crossProduct(n);
					double x = (q0.minus(p0)).dotProduct(n2)/dir0.dotProduct(n2);
					if (x > 0.0 && x < length0) {
						d = p0.plus(dir0.times(x));
					}
				}
				
				double sqrDist = Double.MAX_VALUE;
				double[] p = null;
				if (d != null) {
					p = View.modelToScreenCoordinates(d.getColumnPackedCopy());
					
					sqrDist = (p[0]-point[0])*(p[0]-point[0])+(p[1]-point[1])*(p[1]-point[1]);
					double factor = SimLive.view.getSizeFactorPerspective(d.getColumnPackedCopy());
					sqrDist *= factor*factor;
				}
				if (sqrDist > Search.minSqrDistLine) {
					d = null;
					double[] intersection = null;
					if (elem.getType() == Element.Type.SPRING) {
						double r = 2*SimLive.SPRING_RADIUS/viewport[2]/View.zoom;
						double[][] intersect = GeomUtility.getIntersectionLineCylinder(modelCoords,
								lookAt.getColumnPackedCopy(), c0, dir0.getColumnPackedCopy(), r, length0);
						if (intersect != null) {
							double zCoord0 = View.modelToScreenCoordinates(intersect[0])[2];
							double zCoord1 = View.modelToScreenCoordinates(intersect[1])[2];
							intersection = zCoord0 < zCoord1 ? intersect[0] : intersect[1];
						}
					}
					else {
						intersection = ((LineElement) elem).getCoordsInElement(modelCoords);
					}
					if (intersection != null) {
						double t = ((LineElement) elem).getLocalFromGlobalCoordinates(intersection);
						t = Math.min(Math.max(0, t), 1);
						d = new Matrix(((LineElement) elem).getGlobalFromLocalCoordinates(t), 3);
						p = View.modelToScreenCoordinates(d.getColumnPackedCopy());
					}
				}
				
				if (d != null) {
					
					double zCoord = p[2];
					if (zCoord < Search.zCoord) {
						Search.zCoord = zCoord;
						
						if (projectedPoint != null) {
							projectedPoint[0] = (int) Math.round(p[0]);
							projectedPoint[1] = (int) Math.round(p[1]);
						}
						Search.element = elem;
						Snap.coords3d = d.getColumnPackedCopy();
					}
				}
			}
			if (elem.isPlaneElement() && View.isPointInElementBBox(elem.getID(), point[0], point[1])) {
				double[] coords = ((PlaneElement) elem).getCoordsInElement(modelCoords);
				if (coords != null) {
					double zCoord = View.modelToScreenCoordinates(coords)[2];
					if (zCoord < Search.zCoord) {
						Search.zCoord = zCoord;
						Search.element = elem;
						Snap.coords3d = coords;
					}
				}
			}
		}
	}
	
	private static void rigidElementAtPointInSet(int[] point, ContactPair contactPair, Set set, ArrayList<Node> nodes) {
		ArrayList<Element> elementSet = set.getElements();
		double[] modelCoords = View.screenToModelCoordinates(point[0], point[1]);
		for (int e = 0; e < elementSet.size(); e++) {
			Element elem = elementSet.get(e);
			if (elem.isPlaneElement()) {
				double[] coords = ((PlaneElement) elem).getCoordsInRigidElement(modelCoords, nodes);
				if (coords != null) {
					double zCoord = View.modelToScreenCoordinates(coords)[2];
					if (zCoord < Search.zCoord) {
						Search.zCoord = zCoord;
						Search.contactPair = contactPair;
						Search.element = null;
						Snap.element = null;
						Snap.coords3d = coords;
					}
				}
			}
		}
	}

}

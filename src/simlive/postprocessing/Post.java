package simlive.postprocessing;
import java.util.ArrayList;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.model.Beam;
import simlive.model.DistributedLoad;
import simlive.model.Element;
import simlive.model.LineElement;
import simlive.model.Node;
import simlive.model.PlaneElement;
import simlive.model.PointMass;
import simlive.model.Set;
import simlive.model.Spring;
import simlive.model.Step;
import simlive.solution.Increment;
import simlive.solution.Solution;
import simlive.view.Label;
import simlive.view.View;

public class Post {

	private Solution solution;
	private int eigenMode;
	private ScalarPlot scalarPlot;
	private TensorPlot tensorPlot;
	private boolean isAutoMin, isAutoMax;
	private boolean isShowMinMax;
	private double minValue, maxValue;
	private int numberOfColors;
	private boolean isAutoScaling;
	private double scaling;
	private double principalVectorScaling;
	private int postIncrementID;
	private boolean isReverseAnimation;
	private double animationSpeed;
	private Label minLabel, maxLabel;
	private double curvePlotScaleFactor;
	private boolean curvePlotSwitchOrientation;
	public enum Layer {TOP, BOTTOM};
	private Layer layer;
	
	public Post(Solution solution) {
		if (solution.getRefModel().getSteps().get(0).type == Step.Type.MODAL_ANALYSIS) {
			solution.calculateIncrementsForEigenmode(0);
		}
		this.solution = solution;
		this.eigenMode = 0;
		this.isAutoMin = true;
		this.isAutoMax = true;
		this.isShowMinMax = false;
		this.isAutoScaling = true;
		this.scaling = 1.0;
		this.principalVectorScaling = 1.0;
		this.numberOfColors = 32;
		this.scalarPlot = null;
		this.tensorPlot = null;
		this.postIncrementID = solution.getNumberOfIncrements();
		this.setReverseAnimation(false);
		this.animationSpeed = 1;
		this.minLabel = null;
		this.maxLabel = null;
		this.curvePlotScaleFactor = 1.0;
		this.curvePlotSwitchOrientation = false;
		this.layer = Layer.TOP;
	}

	public Layer getLayer() {
		return layer;
	}

	public void setLayer(Layer layer) {
		this.layer = layer;
	}

	public Solution getSolution() {
		return solution;
	}
	
	public int getEigenMode() {
		return eigenMode;
	}
	
	public ScalarPlot getScalarPlot() {
		return scalarPlot;
	}
	
	public TensorPlot getTensorPlot() {
		return tensorPlot;
	}
	
	public void setEigenMode(int eigenMode) {
		this.eigenMode = eigenMode;
	}

	public void setScalarPlot(ScalarPlot scalarPlot) {
		this.scalarPlot = scalarPlot;
		if (scalarPlot != null) {
			if (isAutoMin()) this.minValue = scalarPlot.getGlobalMinValue();
			if (isAutoMax()) this.maxValue = scalarPlot.getGlobalMaxValue();
		}
	}
	
	public void setTensorPlot(TensorPlot tensorPlot) {
		this.tensorPlot = tensorPlot;
	}

	public boolean isAutoMin() {
		return isAutoMin;
	}

	public void setAutoMin(boolean isAutoMin) {
		this.isAutoMin = isAutoMin;
		if (isAutoMin) {
			this.minValue = this.scalarPlot.getGlobalMinValue();
		}
	}

	public boolean isAutoMax() {
		return isAutoMax;
	}

	public void setAutoMax(boolean isAutoMax) {
		this.isAutoMax = isAutoMax;
		if (isAutoMax) {
			this.maxValue = this.scalarPlot.getGlobalMaxValue();
		}
	}

	public boolean isShowMinMax() {
		return isShowMinMax;
	}

	public void setShowMinMax(boolean isShowMinMax) {
		this.isShowMinMax = isShowMinMax;
	}

	public boolean isReverseAnimation() {
		return isReverseAnimation;
	}

	public void setReverseAnimation(boolean isReverseAnimation) {
		this.isReverseAnimation = isReverseAnimation;
		SimLive.view.backwards = false;
	}

	public double getAnimationSpeed() {
		return animationSpeed;
	}

	public void setAnimationSpeed(double animationSpeed) {
		this.animationSpeed = animationSpeed;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public int getNumberOfColors() {
		return numberOfColors;
	}
	
	public void setNumberOfColors(int numberOfColors) {
		this.numberOfColors = numberOfColors;
	}

	public boolean isAutoScaling() {
		return isAutoScaling;
	}

	public void setAutoScaling(boolean isAutoScaling) {
		this.isAutoScaling = isAutoScaling;
		this.scaling = getAutoScaling();
	}

	public double getScaling() {
		return scaling;
	}

	public void setScaling(double scaling) {
		this.scaling = scaling;
	}
	
	public double getPrincipalVectorScaling() {
		return principalVectorScaling;
	}

	public void setPrincipalVectorScaling(double principalVectorScaling) {
		this.principalVectorScaling = principalVectorScaling;
	}

	public double getPostTime() {
		return solution.getIncrement(postIncrementID).getTime();
	}
	
	public int getPostIncrementID() {
		return postIncrementID;
	}
	
	public void setPostIncrementIDtoStartOfStep() {
		int stepNr = getPostIncrement().getStepNr();
		int id = 0;
		for (int s = 0; s < stepNr; s++) {
			int delta = SimLive.model.getSteps().get(s).nIncrements+1;
			id += delta;
			if (postIncrementID == id) {
				id -= delta;
			}
		}
		postIncrementID = id;
	}
	
	public void setPostIncrementIDtoEndOfStep() {
		int stepNr = getPostIncrement().getStepNr();
		int id = 0;
		for (int s = 0; s < stepNr+1; s++) {
			int delta = SimLive.model.getSteps().get(s).nIncrements+1;
			id += delta;
			if (id > solution.getNumberOfIncrements()) {
				id = solution.getNumberOfIncrements();
			}
		}
		postIncrementID = id;
	}

	public void setPostIncrementID(int postIncrementID) {
		this.postIncrementID = postIncrementID;
	}

	public void nextPostIncrementID(double incrementID) {
		if (incrementID-animationSpeed == solution.getNumberOfIncrements()) {
			postIncrementID = 0;
		}
		else {
			postIncrementID = Math.min((int) incrementID, solution.getNumberOfIncrements());
		}
	}
	
	public void previousPostIncrementID(double incrementID) {
		postIncrementID = Math.max((int) incrementID, 0);
	}
	
	public Increment getPostIncrement() {
		return solution.getIncrement(postIncrementID);
	}

	public void addMinMaxLabels() {
		ScalarPlot scalarPlot = getScalarPlot();
		ArrayList<Element> elements = SimLive.model.getElements();
		double minValue = Double.MAX_VALUE;
		double maxValue = -Double.MAX_VALUE;
		for (int elem = 0; elem < elements.size(); elem++) {
			Element element = elements.get(elem);
			if (scalarPlot.hasValue(element, getPostIncrement())) {
				if (element.isPlaneElement()) {
					int[] elemNodes = element.getElementNodes();
					for (int n = 0; n < elemNodes.length; n++) {
						double value = scalarPlot.getValueAtNode(elemNodes[n], postIncrementID);
						if (value < minValue) {
							minValue = value;
							minLabel = new Label((PlaneElement) element,
									View.getCoordsWithScaledDisp(elemNodes[n]));
						}
						if (value > maxValue) {
							maxValue = value;
							maxLabel = new Label((PlaneElement) element,
									View.getCoordsWithScaledDisp(elemNodes[n]));
						}
					}
				}
				if (element.isLineElement()) {
					boolean constValue = true;
					double value0 = scalarPlot.getValueForLineElement
							((LineElement) element, 0.0, postIncrementID);
					double t = 0.0;
					for (int i = 0; i <= SimLive.LINE_DIVISIONS_MAX; i++) {
						double value = scalarPlot.getValueForLineElement
								((LineElement) element, t, postIncrementID);
						if (value != value0) {
							constValue = false;
							break;
						}
						t += 1.0/SimLive.LINE_DIVISIONS_MAX;
					}
					t = 0.0;
					for (int i = 0; i <= SimLive.LINE_DIVISIONS_MAX; i++) {
						double value = scalarPlot.getValueForLineElement
								((LineElement) element, t, postIncrementID);
						if (value < minValue) {
							minValue = value;
							if (constValue) {
								minLabel = new Label((LineElement) element, 0.5);
							}
							else {
								minLabel = new Label((LineElement) element, t);
							}
						}
						if (value > maxValue) {
							maxValue = value;
							if (constValue) {
								maxLabel = new Label((LineElement) element, 0.5);
							}
							else {
								maxLabel = new Label((LineElement) element, t);
							}
						}
						t += 1.0/SimLive.LINE_DIVISIONS_MAX;
					}
				}
				if (element.getType() == Element.Type.POINT_MASS) {
					int[] elemNodes = element.getElementNodes();
					double value = scalarPlot.getValueAtNode(elemNodes[0], postIncrementID);
					if (value < minValue) {
						minValue = value;
						minLabel = new Label((PointMass) element);
					}
					if (value > maxValue) {
						maxValue = value;
						maxLabel = new Label((PointMass) element);
					}
				}
			}
		}
		SimLive.view.labels.add(0, minLabel);
		SimLive.view.labels.add(1, maxLabel);
	}
	
	public void removeMinMaxLabels() {
		SimLive.view.labels.remove(minLabel);
		SimLive.view.labels.remove(maxLabel);
	}
	
	public void updateMinMaxLabels() {		
		if (SimLive.mode == Mode.RESULTS && isShowMinMax()) {
			if (getScalarPlot() == null) {
				removeMinMaxLabels();
			}
			else {
				int[] minLabelMove = minLabel.getMove();
				int[] maxLabelMove = maxLabel.getMove();
				boolean minLabelSide = minLabel.isOnRightHandSide();
				boolean maxLabelSide = maxLabel.isOnRightHandSide();
				boolean selectedLabelIsMinLabel = SimLive.view.selectedLabel == minLabel;
				boolean selectedLabelIsMaxLabel = SimLive.view.selectedLabel == maxLabel;
				
				removeMinMaxLabels();
				addMinMaxLabels();
				
				if (selectedLabelIsMinLabel) SimLive.view.selectedLabel = minLabel;
				if (selectedLabelIsMaxLabel) SimLive.view.selectedLabel = maxLabel;
				minLabel.addToMove(minLabelMove);
				maxLabel.addToMove(maxLabelMove);
				minLabel.setOnRightHandSide(minLabelSide);
				maxLabel.setOnRightHandSide(maxLabelSide);
			}
		}
	}

	public Label getMinLabel() {
		return minLabel;
	}

	public Label getMaxLabel() {
		return maxLabel;
	}
	
	private double getAutoScaling() {
		if (solution.getRefModel().settings.isLargeDisplacement) {
			return 1.0;
		}
		
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		double maxDisp = 0;
		for (int inc = 0; inc < solution.getNumberOfIncrements()+1; inc++) {
			for (int i = 0; i < nodes.size(); i++) {
				double[] d = solution.getIncrement(inc).getDisplacement(i);
				double disp = Math.sqrt(d[0]*d[0] + d[1]*d[1] + d[2]*d[2]);
				if (disp > maxDisp) {
					maxDisp = disp;
				}
			}
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).getType() == Element.Type.BEAM) {
					Beam beam = (Beam) elements.get(elem);
					
					double t = 0.0;
					for (int i = 0; i < SimLive.LINE_DIVISIONS_MAX; i++) {
						t += 1.0/SimLive.LINE_DIVISIONS_MAX;
						double[] disp = beam.interpolateNodeKinematicValues(t, solution.getIncrement(inc), 0);
						
						double dispLength = Math.sqrt(disp[0]*disp[0] + disp[1]*disp[1] + disp[2]*disp[2]);
						if (dispLength > maxDisp) {
							maxDisp = dispLength;
						}
					}
				}
			}
		}
		
		double scaling = 0.0;
		if (maxDisp > SimLive.ZERO_TOL) {	
			double minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
			for (int i = 0; i < nodes.size(); i++) {
				if (nodes.get(i).getXCoord() < minX) minX = nodes.get(i).getXCoord();
				if (nodes.get(i).getXCoord() > maxX) maxX = nodes.get(i).getXCoord();
				if (nodes.get(i).getYCoord() < minY) minY = nodes.get(i).getYCoord();
				if (nodes.get(i).getYCoord() > maxY) maxY = nodes.get(i).getYCoord();
				if (nodes.get(i).getZCoord() < minZ) minZ = nodes.get(i).getZCoord();
				if (nodes.get(i).getZCoord() > maxZ) maxZ = nodes.get(i).getZCoord();
			}
			scaling = Math.sqrt((maxX-minX)*(maxX-minX)+(maxY-minY)*(maxY-minY)+(maxZ-minZ)*(maxZ-minZ))/(10.0*maxDisp);
		}
		
		double maxRot = 0.0;
		for (int inc = 0; inc < solution.getNumberOfIncrements()+1; inc++) {
			Matrix u_global = solution.getIncrement(inc).get_u_global();
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).getType() == Element.Type.BEAM) {
					Beam beam = (Beam) elements.get(elem);
					for (int i = 0; i < beam.getElementNodes().length; i++) {
						int dof = solution.getDofOfNodeID(beam.getElementNodes()[i]);
						double rot = u_global.getMatrix(dof+3, dof+5, 0, 0).normF();
						if (rot > maxRot) {
							maxRot = rot;
						}
					}
				}
			}
		}
		if (maxRot > SimLive.ZERO_TOL) {
			if (scaling == 0.0 || scaling > Math.PI/4.0/Math.atan(maxRot)) {
				scaling = Math.PI/4.0/Math.atan(maxRot);
			}
		}
		
		return scaling;
	}
	
	public double getCurvePlotScaleFactor() {
		return curvePlotScaleFactor;
	}

	public void setCurvePlotScaleFactor(double curvePlotScaleFactor) {
		this.curvePlotScaleFactor = curvePlotScaleFactor;
	}

	public boolean isCurvePlotSwitchOrientation() {
		return curvePlotSwitchOrientation;
	}

	public void setCurvePlotSwitchOrientation(boolean curvePlotSwitchOrientation) {
		this.curvePlotSwitchOrientation = curvePlotSwitchOrientation;
	}

	public double[][] calculateDisplacement(int component /* 0: total, 1: x, 2: y, 3: z */) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		double[][] disp = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			disp[i] = new double[nodes.size()];
			
			for (int node = 0; node < nodes.size(); node++) {
				double[] d = solution.getIncrement(i).getDisplacement(node);
				if (component == 0) {
					disp[i][node] = Math.sqrt(d[0]*d[0] + d[1]*d[1] + d[2]*d[2]);
				}
				else {
					disp[i][node] = d[component-1];
				}
			}
		}
		return disp;
	}
	
	public double[][] calculateAcceleration(int component /* 0: total, 1: x, 2: y, 3: z */) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		double[][] acc = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			acc[i] = new double[nodes.size()];
				
			for (int node = 0; node < nodes.size(); node++) {
				double[] a = solution.getIncrement(i).getAcceleration(node);
				if (component == 0) {
					acc[i][node] = Math.sqrt(a[0]*a[0] + a[1]*a[1] + a[2]*a[2]);
				}
				else {
					acc[i][node] = a[component-1];
				}
			}
		}
		return acc;
	}
	
	public double[][] calculateVelocity(int component /* 0: total, 1: x, 2: y, 3: z */) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		double[][] vel = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			vel[i] = new double[nodes.size()];
				
			for (int node = 0; node < nodes.size(); node++) {
				double[] v = solution.getIncrement(i).getVelocity(node);
				if (component == 0) {
					vel[i][node] = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
				}
				else {
					vel[i][node] = v[component-1];
				}
			}
		}
		return vel;
	}
	
	public Matrix[][][] calculateStrain() {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		Matrix[][][] strain = new Matrix[solution.getNumberOfIncrements()+1][elements.size()][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			Matrix u_global = solution.getIncrement(i).get_u_global();
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					PlaneElement planeElement = (PlaneElement) elements.get(elem);
					int[] elemNodes = elements.get(elem).getElementNodes();
					strain[i][elem] = new Matrix[elemNodes.length];
					
					for (int n = 0; n < elemNodes.length; n++) {
						double[] localCoords = planeElement.getLocalCoords(n);
						strain[i][elem][n] = planeElement.getStrain(
									nodes, u_global, localCoords[0], localCoords[1]);
					}
				}
			}
		}
		return strain;
	}
	
	public double[][] calculatePrincipalStrain(boolean major, Matrix[][][] strain) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][] principalStrain = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			principalStrain[i] = new double[nodes.size()];
			int[] count = new int[nodes.size()];
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					int[] elemNodes = elements.get(elem).getElementNodes();
					
					for (int n = 0; n < elemNodes.length; n++) {
						Matrix elemStrain = strain[i][elem][n];
						
						if (major) {
							principalStrain[i][elemNodes[n]] += 0.5*(elemStrain.get(0, 0)+elemStrain.get(1, 0))+
									Math.sqrt(0.25*(elemStrain.get(0, 0)-elemStrain.get(1, 0))*(elemStrain.get(0, 0)-
									elemStrain.get(1, 0))+0.25*elemStrain.get(2, 0)*elemStrain.get(2, 0));
						}
						else {
							principalStrain[i][elemNodes[n]] += 0.5*(elemStrain.get(0, 0)+elemStrain.get(1, 0))-
									Math.sqrt(0.25*(elemStrain.get(0, 0)-elemStrain.get(1, 0))*(elemStrain.get(0, 0)-
									elemStrain.get(1, 0))+0.25*elemStrain.get(2, 0)*elemStrain.get(2, 0));
						}
						
						count[elemNodes[n]]++;
					}
				}
			}
			
			for (int n = 0; n < nodes.size(); n++) {
				if (count[n] > 0) {
					principalStrain[i][n] /= count[n];
				}
			}
		}
		return principalStrain;
	}
	
	public double[][][][] calculatePrincipalStrainVectors(boolean major, Matrix[][][] strain) {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][][][] vectors = new double[solution.getNumberOfIncrements()+1][][][];
		
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			vectors[i] = new double[elements.size()][4][2];
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					
					int nrGaussPoints = elements.get(elem).getType() == Element.Type.TRI ? 1 : 4;
					for (int gaussPoint = 0; gaussPoint < nrGaussPoints; gaussPoint++) {
						Matrix elemStrain = strain[i][elem][gaussPoint];
					
						double principalStr = 0;
						if (major) {
							principalStr = 0.5*(elemStrain.get(0, 0)+elemStrain.get(1, 0))+
									Math.sqrt(0.25*(elemStrain.get(0, 0)-elemStrain.get(1, 0))*(elemStrain.get(0, 0)-
									elemStrain.get(1, 0))+0.25*elemStrain.get(2, 0)*elemStrain.get(2, 0));
						}
						else {
							principalStr = 0.5*(elemStrain.get(0, 0)+elemStrain.get(1, 0))-
									Math.sqrt(0.25*(elemStrain.get(0, 0)-elemStrain.get(1, 0))*(elemStrain.get(0, 0)-
									elemStrain.get(1, 0))+0.25*elemStrain.get(2, 0)*elemStrain.get(2, 0));
						}
						double angle = 0.5*Math.atan2(elemStrain.get(2, 0), elemStrain.get(0, 0)-elemStrain.get(1, 0));
						if (!major) {
							angle += Math.PI/2.0;
						}
						vectors[i][elem][gaussPoint][0] = principalStr;
						vectors[i][elem][gaussPoint][1] = angle;
					}
				}
			}
		}
		return vectors;
	}

	public double[][] calculateEquivalentStrain(Matrix[][][] strain) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][] equivalentStrain = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			equivalentStrain[i] = new double[nodes.size()];
			int[] count = new int[nodes.size()];
			Matrix u_global = solution.getIncrement(i).get_u_global();
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					PlaneElement planeElement = (PlaneElement) elements.get(elem);
					int[] elemNodes = elements.get(elem).getElementNodes();
					
					for (int n = 0; n < elemNodes.length; n++) {
						double[] localCoords = planeElement.getLocalCoords(n);
						Matrix elemStrain = strain[i][elem][n];
						double thicknessStrain = planeElement.getThickening(
								nodes, u_global, localCoords[0], localCoords[1])/
								planeElement.getThickness();
						
						equivalentStrain[i][elemNodes[n]] += 1.0/(Math.sqrt(2)*(1+planeElement.getMaterial().getPoissonsRatio()))*
								Math.sqrt((elemStrain.get(0, 0)-elemStrain.get(1, 0))*(elemStrain.get(0, 0)-elemStrain.get(1, 0))+
										(elemStrain.get(1, 0)-thicknessStrain)*(elemStrain.get(1, 0)-thicknessStrain)+
										(thicknessStrain-elemStrain.get(0, 0))*(thicknessStrain-elemStrain.get(0, 0))+
										3.0/2.0*elemStrain.get(2, 0)*elemStrain.get(2, 0));
						
						count[elemNodes[n]]++;
					}
				}
			}
			
			for (int n = 0; n < nodes.size(); n++) {
				if (count[n] > 0) {
					equivalentStrain[i][n] /= count[n];
				}
			}
		}
		return equivalentStrain;
	}
	
	public Matrix[][][] calculateStress() {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		Matrix[][][] stress = new Matrix[solution.getNumberOfIncrements()+1][elements.size()][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			Matrix u_global = solution.getIncrement(i).get_u_global();
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					PlaneElement planeElement = (PlaneElement) elements.get(elem);
					int[] elemNodes = elements.get(elem).getElementNodes();
					stress[i][elem] = new Matrix[elemNodes.length];
					
					for (int n = 0; n < elemNodes.length; n++) {
						double[] localCoords = planeElement.getLocalCoords(n);
						stress[i][elem][n] = planeElement.getStress(
									nodes, u_global, localCoords[0], localCoords[1]);
					}
				}
			}
		}
		return stress;
	}
	
	public double[][] calculatePrincipalStress(boolean major, Matrix[][][] stress) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][] principalStress = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			principalStress[i] = new double[nodes.size()];
			int[] count = new int[nodes.size()];
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					int[] elemNodes = elements.get(elem).getElementNodes();
					
					for (int n = 0; n < elemNodes.length; n++) {
						Matrix elemStress = stress[i][elem][n];
						
						if (major) {
							principalStress[i][elemNodes[n]] += 0.5*(elemStress.get(0, 0)+elemStress.get(1, 0))+
									Math.sqrt(0.25*(elemStress.get(0, 0)-elemStress.get(1, 0))*(elemStress.get(0, 0)-
									elemStress.get(1, 0))+elemStress.get(2, 0)*elemStress.get(2, 0));
						}
						else {
							principalStress[i][elemNodes[n]] += 0.5*(elemStress.get(0, 0)+elemStress.get(1, 0))-
									Math.sqrt(0.25*(elemStress.get(0, 0)-elemStress.get(1, 0))*(elemStress.get(0, 0)-
									elemStress.get(1, 0))+elemStress.get(2, 0)*elemStress.get(2, 0));
						}
						
						count[elemNodes[n]]++;
					}
				}
			}
			
			for (int n = 0; n < nodes.size(); n++) {
				if (count[n] > 0) {
					principalStress[i][n] /= count[n];
				}
			}
		}
		return principalStress;
	}
	
	public double[][][][] calculatePrincipalStressVectors(boolean major, Matrix[][][] stress) {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][][][] vectors = new double[solution.getNumberOfIncrements()+1][][][];
		
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			vectors[i] = new double[elements.size()][4][2];
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					
					int nrGaussPoints = elements.get(elem).getType() == Element.Type.TRI ? 1 : 4;
					for (int gaussPoint = 0; gaussPoint < nrGaussPoints; gaussPoint++) {
						Matrix elemStress = stress[i][elem][gaussPoint];
					
						double principalStr = 0;
						if (major) {
							principalStr = 0.5*(elemStress.get(0, 0)+elemStress.get(1, 0))+
									Math.sqrt(0.25*(elemStress.get(0, 0)-elemStress.get(1, 0))*(elemStress.get(0, 0)-
									elemStress.get(1, 0))+elemStress.get(2, 0)*elemStress.get(2, 0));
						}
						else {
							principalStr = 0.5*(elemStress.get(0, 0)+elemStress.get(1, 0))-
									Math.sqrt(0.25*(elemStress.get(0, 0)-elemStress.get(1, 0))*(elemStress.get(0, 0)-
									elemStress.get(1, 0))+elemStress.get(2, 0)*elemStress.get(2, 0));
						}
						double angle = 0.5*Math.atan2(2.0*elemStress.get(2, 0), elemStress.get(0, 0)-elemStress.get(1, 0));
						if (!major) {
							angle += Math.PI/2.0;
						}
						vectors[i][elem][gaussPoint][0] = principalStr;
						vectors[i][elem][gaussPoint][1] = angle;
					}
				}
			}
		}
		return vectors;
	}
	
	public double[][] calculateEquivalentStress(Matrix[][][] stress, int type /* 0-Mises, 1-Tresca, 2-Rankine */) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][] eqvStress = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			eqvStress[i] = new double[nodes.size()];
			int[] count = new int[nodes.size()];
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					int[] elemNodes = elements.get(elem).getElementNodes();
					
					for (int n = 0; n < elemNodes.length; n++) {
						Matrix elemStress = stress[i][elem][n];
						
						double temp1 = 0.5*(elemStress.get(0, 0)+elemStress.get(1, 0));
						double temp2 = Math.sqrt(0.25*(elemStress.get(0, 0)-elemStress.get(1, 0))*(elemStress.get(0, 0)-
								elemStress.get(1, 0))+elemStress.get(2, 0)*elemStress.get(2, 0));
						double principalStr1 = temp1+temp2;
						double principalStr2 = temp1-temp2;
						
						switch (type) {
							case 0: eqvStress[i][elemNodes[n]] += Math.sqrt(principalStr1*principalStr1-
									principalStr1*principalStr2+principalStr2*principalStr2);
									break;
							case 1: eqvStress[i][elemNodes[n]] += Math.max(Math.abs(principalStr1-principalStr2),
									Math.max(Math.abs(principalStr1), Math.abs(principalStr2)));
									break;
							case 2: eqvStress[i][elemNodes[n]] += principalStr1;
									break;	
						}
						
						count[elemNodes[n]]++;
					}
				}
			}
			
			for (int n = 0; n < nodes.size(); n++) {
				if (count[n] > 0) {
					eqvStress[i][n] /= count[n];
				}
			}
		}
		return eqvStress;
	}
	
	public double[][] calculateThickening() {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][] thickening = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			thickening[i] = new double[nodes.size()];
			int[] count = new int[nodes.size()];
			Matrix u_global = solution.getIncrement(i).get_u_global();
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isPlaneElement()) {
					PlaneElement planeElement = (PlaneElement) elements.get(elem);
					int[] elemNodes = planeElement.getElementNodes();
					
					for (int n = 0; n < elemNodes.length; n++) {
						double[] localCoords = planeElement.getLocalCoords(n);
						thickening[i][elemNodes[n]] += planeElement.getThickening(
									nodes, u_global, localCoords[0], localCoords[1]);
						count[elemNodes[n]]++;
					}
				}
			}
			
			for (int n = 0; n < nodes.size(); n++) {
				if (count[n] > 0) thickening[i][n] /= count[n];
			}
		}
		return thickening;
	}
	
	public double[][] calculateElementForce(int component /* 0: normal, 1: shear y, 2: shear z, 3: torsion, 4: bending y, 5: bending z */) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		ArrayList<DistributedLoad> distributedLoads = solution.getRefModel().getDistributedLoads();
		double[][] forces = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			forces[i] = new double[elements.size()*2];
			Matrix u_global = solution.getIncrement(i).get_u_global();
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isLineElement()) {
					Matrix elementForces = null;
					if (solution.getRefModel().settings.isLargeDisplacement) {
						elementForces = elements.get(elem).getElementForceNL(nodes, u_global, true);
					}
					else {
						elementForces = elements.get(elem).getElementForce(nodes, u_global, true);
					}
					if (elements.get(elem).getType() == Element.Type.ROD ||
						elements.get(elem).getType() == Element.Type.SPRING) {
						if (component == 0) {
							forces[i][elem*2] = -elementForces.get(0, 0);
							forces[i][elem*2+1] = elementForces.get(1, 0);
						}
					}
					if (elements.get(elem).getType() == Element.Type.BEAM) {
						switch (component) {
							case 0: forces[i][elem*2] = -elementForces.get(0,0);
									forces[i][elem*2+1] = elementForces.get(6,0);
									break;
							case 1: forces[i][elem*2] = -elementForces.get(1,0);
									forces[i][elem*2+1] = elementForces.get(7,0);
									break;
							case 2: forces[i][elem*2] = -elementForces.get(2,0);
									forces[i][elem*2+1] = elementForces.get(8,0);
									break;
							case 3: forces[i][elem*2] = -elementForces.get(3,0);
									forces[i][elem*2+1] = elementForces.get(9,0);
									break;
							case 4: forces[i][elem*2] = -elementForces.get(4,0);
									forces[i][elem*2+1] = elementForces.get(10,0);
									break;
							case 5: forces[i][elem*2] = -elementForces.get(5,0);
									forces[i][elem*2+1] = elementForces.get(11,0);
									break;
						}
					}
				}
			}
			// interpolation for N, Qy, Qz, Mx at distributed loads
			if (component < 4) {
				for (int d = 0; d < distributedLoads.size(); d++) {
					for (int s = 0; s < distributedLoads.get(d).getElementSets().size(); s++) {
						Set set = distributedLoads.get(d).getElementSets().get(s);
						double[] forces1 = new double[set.getElements().size()*2];
						for (int elem = 1; elem < set.getElements().size()-1; elem++) {
							int id0 = set.getElements().get(elem-1).getID();
							int id1 = set.getElements().get(elem).getID();
							int id2 = set.getElements().get(elem+1).getID();
							forces1[elem*2] = (forces[i][id0*2]+forces[i][id1*2])/2.0;
							forces1[elem*2+1] = (forces[i][id1*2]+forces[i][id2*2])/2.0;
						}
						int id0 = set.getElements().get(0).getID();
						forces1[0] = 2.0*forces[i][id0*2]-forces1[2];
						forces1[1] = forces1[2];
						int id1 = set.getElements().get(set.getElements().size()-1).getID();
						forces1[forces1.length-1] = 2.0*forces[i][id1*2+1]-forces1[forces1.length-3];
						forces1[forces1.length-2] = forces1[forces1.length-3];
						
						for (int k = 0; k < forces1.length; k++) {
							forces[i][k+id0*2] = forces1[k];
						}
					}
				}
			}
		}
		return forces;
	}
	
	public double[][] calculateSpringDeflection() {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		double[][] deflections = new double[solution.getNumberOfIncrements()+1][];
		for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
			deflections[i] = new double[elements.size()*2];
			Matrix u_global = solution.getIncrement(i).get_u_global();
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).getType() == Element.Type.SPRING) {
					
					double length = ((LineElement) elements.get(elem)).getLength();
					Matrix u_elem = elements.get(elem).globalToLocalVector(u_global);
					
					if (solution.getRefModel().settings.isLargeDisplacement) {
						double lengthNew = ((Spring) elements.get(elem)).getCurrentLength(solution.getRefModel().getNodes(), u_elem);
						deflections[i][elem*2] = lengthNew-length;
						deflections[i][elem*2+1] = lengthNew-length;		
					}
					else {
						double dx = ((Spring) elements.get(elem)).getCurrentDeltaX(solution.getRefModel().getNodes(), u_elem);
						double dy = ((Spring) elements.get(elem)).getCurrentDeltaY(solution.getRefModel().getNodes(), u_elem);
						double[] diff = new double[2];
						diff[0] = ((LineElement) elements.get(elem)).getDeltaX()/length;
						diff[1] = ((LineElement) elements.get(elem)).getDeltaY()/length;
						double lengthNewPrj = dx*diff[0]+dy*diff[1];
						deflections[i][elem*2] = lengthNewPrj-length;
						deflections[i][elem*2+1] = lengthNewPrj-length;
					}
				}
			}
		}
		return deflections;
	}

}
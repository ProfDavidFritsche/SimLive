package simlive.solution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import simlive.SimLive;
import simlive.dialog.SolutionDialog;
import simlive.model.Beam;
import simlive.model.Connector;
import simlive.model.ContactPair;
import simlive.model.DistributedLoad;
import simlive.model.Element;
import simlive.model.LineElement;
import simlive.model.Load;
import simlive.model.Material;
import simlive.model.Model;
import simlive.model.Node;
import simlive.model.PlaneElement;
import simlive.model.Section;
import simlive.model.Spring;
import simlive.model.Step;
import simlive.model.Step.Type;
import simlive.model.Support;
import simlive.model.TimeTable;

public class Solution {
	private Increment[] increments;
	private Matrix D, V;
	private ConstraintMethod constraintMethod;
	private int nDofs;
	public static ArrayList<String> log = new ArrayList<String>();
	public static ArrayList<String> errors = new ArrayList<String>();
	public static ArrayList<String> warnings = new ArrayList<String>();
	protected Model refModel;
	private int[] dofOfNodeID;
	public static String[] results = {"Solution Stopped with Errors",
			"Solution Finished with Warnings", "Solution Finished Successfully",
			"Update Model...", "Reorder Nodes...", "Calculating...", "No Solution"};
	private boolean isWriteMatrixView;
	private int nIncrements;
	private Contact[] contacts; //parallelStream
	private int nSuppressedDofs = 0; //solve2d
	private boolean[] suppressedDof = null; //solve2d
	
	
	public Solution(Model model) {
		this.refModel = model.clone();
		switch (refModel.settings.constraintType) {
			case LAGRANGE_MULTIPLIERS:
				this.constraintMethod = new LagrangeMultipliers();
				break;
			case PENALTY_METHOD:
				this.constraintMethod = new PenaltyMethod(refModel.settings.penaltyFactor);
				break;
			default:
				break;
		}
		this.isWriteMatrixView = refModel.settings.isWriteMatrixView;
		
		for (int s = 0; s < refModel.getSteps().size(); s++) {
			Step step = refModel.getSteps().get(s);
			this.nIncrements += step.nIncrements+1;
		}
		this.increments = new Increment[this.nIncrements];
		this.nIncrements--;
		
		this.initialize();
	}
	
	public static void resetLog() {
		log.clear();
		errors.clear();
		warnings.clear();
	}
	
	private void initialize() {
		ArrayList<Node> nodes = refModel.getNodes();
		ArrayList<Element> elements = refModel.getElements();
		
		/*set nDofs*/
		nDofs = 0;
		for (int n = 0; n < nodes.size(); n++) {
			if (nodes.get(n).isRotationalDOF()) nDofs += 6;
			else								nDofs += 3;
		}
		
		/*set dofOfNodeID*/
		dofOfNodeID = new int[nodes.size()];				
		int current_dof = 0;
		for (int n = 0; n < nodes.size(); n++) {
			dofOfNodeID[n] = current_dof;
			current_dof+=3;
			if (nodes.get(n).isRotationalDOF()) {
				current_dof+=3;				
			}
		}
		
		/*setIndexIncidence*/
		for (int elem = 0; elem < elements.size(); elem++) {
			elements.get(elem).setIndexIncidence(this, nodes);
		}
		
		/*generateSlaveNodeList*/
		Contact.generateSlaveNodeList(refModel.getContactPairs(), refModel.getElements(), refModel.getNodes().size());
		
		if (Model.twoDimensional) {
			/*generateEdgeList*/
			Contact.generateEdgeList(refModel.getContactPairs());
			
			setSuppressedDofs2d();
		}
	}
	
	public boolean checkModel() {
		ArrayList<Node> nodes = refModel.getNodes();
		ArrayList<Element> elements = refModel.getElements();
		ArrayList<Support> supports = refModel.getSupports();
		ArrayList<Connector> connectors = refModel.getConnectors();
		ArrayList<ContactPair> contactPairs = refModel.getContactPairs();
		ArrayList<Material> materials = refModel.getMaterials();
		ArrayList<Section> sections = refModel.getSections();
		ArrayList<Step> steps = refModel.getSteps();
		ArrayList<Load> loads = refModel.getLoads();
		ArrayList<DistributedLoad> distributedLoads = refModel.getDistributedLoads();
		
		if (nodes.size() == 0) {
			errors.add("No node found.");
		}
		
		if (materials.size() == 0) {
			errors.add("No material defined.");
		}
		
		for (int i = 0; i < Element.Type.values().length; i++) {
			Element.Type type = Element.Type.values()[i];
			for (int elem = 0; elem < elements.size(); elem++) {
				if (elements.get(elem).isLineElement()) {
					LineElement element = (LineElement) elements.get(elem);
					if (element.getType() == type) {
						if (!element.isSectionValid(sections)) {
							errors.add("No section for elements of type \""+element.getTypeString()+"\" defined.");
						}
						break;
					}
				}
			}
		}
		
		/*for (int elem = 0; elem < elements.size(); elem++) {
			if (elements.get(elem).isLineElement()) {
				LineElement element = (LineElement) elements.get(elem);
				double[] q0 = element.getQ0();
				Matrix R0 = element.getR0();
				double[] r3 = new double[3];
				r3[0] = q0[1]*R0.get(2, 0)-q0[2]*R0.get(1, 0);
				r3[1] = q0[2]*R0.get(0, 0)-q0[0]*R0.get(2, 0);
				r3[2] = q0[0]*R0.get(1, 0)-q0[1]*R0.get(0, 0);
				double length = Math.sqrt(r3[0]*r3[0]+r3[1]*r3[1]+r3[2]*r3[2]);
				if (length < Sim2d.ZERO_TOL) {
					errors.add("Element \""+element.getTypeString()+" "+Integer.toString(elem+1)+"\" has an invalid orientation.");
				}
			}
		}*/
		
		if (steps.size() == 0) {
			errors.add("No step defined.");
		}		
		else {
			for (int s = 0; s < steps.size(); s++) {
				if (steps.get(s).type == Step.Type.MODAL_ANALYSIS) {
					if (steps.size() > 1) {
						errors.add("Modal analysis only possible as single step.");
					}
					if (refModel.settings.isLargeDisplacement) {
						errors.add("Modal analysis with large displacement is not possible.");
					}
					break;
				}
			}
		}
		
		for (int elem = 0; elem < elements.size(); elem++) {
			if (elements.get(elem).getType() == Element.Type.SPRING) {
				Spring spring = (Spring) elements.get(elem);
				if (spring.getStiffness() <= 0.0) {
					errors.add("Element \""+spring.getTypeString()+" "+Integer.toString(elem+1)+"\" has no stiffness.");
				}
			}
			if (elements.get(elem).isPlaneElement()) {
				PlaneElement element = (PlaneElement) elements.get(elem);
				if (element.getThickness() <= 0.0) {
					errors.add("Element \""+element.getTypeString()+" "+Integer.toString(elem+1)+"\" has no thickness.");
				}
			}
		}
		
		if (isWriteMatrixView) {
			int max_nDofs = 1000;
			if (nDofs > max_nDofs) {
				errors.add("Write matrix view for large systems (nDof>"+Integer.toString(max_nDofs)+") is not possible.");
			}
		}
		
		for (int n = 0; n < nodes.size(); n++) {
			if (!nodes.get(n).isRotationalDOF()) {
				for (int s = 0; s < supports.size(); s++) {
					if (supports.get(s).isFixedRot()[0] || supports.get(s).isFixedRot()[1] || supports.get(s).isFixedRot()[2]) {
						if (supports.get(s).getNodes().contains(nodes.get(n))) {
							warnings.add("Fixed rotation is ignored for node "+Integer.toString(n+1)+".");
						}
					}
				}
				for (int l = 0; l < loads.size(); l++) {
					if (loads.get(l).getType() == Load.Type.FORCE && (loads.get(l).getMoment()[0] != 0.0 ||
							loads.get(l).getMoment()[1] != 0.0 || loads.get(l).getMoment()[2] != 0.0)) {
						if (loads.get(l).getNodes().contains(nodes.get(n))) {
							warnings.add("Moment is ignored for node "+Integer.toString(n+1)+".");
						}
					}
					if (loads.get(l).getType() == Load.Type.DISPLACEMENT && (loads.get(l).isRotation()[0] ||
							loads.get(l).isRotation()[1] || loads.get(l).isRotation()[2])) {
						if (loads.get(l).getNodes().contains(nodes.get(n))) {
							warnings.add("Rotation is ignored for node "+Integer.toString(n+1)+".");
						}
					}
				}
			}
		}
		
		for (int c = 0; c < connectors.size(); c++) {
			if (connectors.get(c).getSet0() == null || connectors.get(c).getSet1() == null ||
				!connectors.get(c).isCoordsSet()) {
				errors.add("Connector \""+connectors.get(c).name+"\" is not completely defined.");
			}
			else {
				Element[] elem = new Element[2];
				elem[0] = connectors.get(c).getElement0();
				elem[1] = connectors.get(c).getElement1();
				double[] coords = connectors.get(c).getCoordinates();
				for (int i = 0; i < elem.length; i++) {
					if (elem[i].isLineElement()) {
						int[] elementNodes = elem[i].getElementNodes();
						int n = 0;
						for (n = 0; n < elementNodes.length; n++) {
							if (Math.abs(nodes.get(elementNodes[n]).getXCoord()-coords[0]) < SimLive.ZERO_TOL &&
								Math.abs(nodes.get(elementNodes[n]).getYCoord()-coords[1]) < SimLive.ZERO_TOL &&
								Math.abs(nodes.get(elementNodes[n]).getZCoord()-coords[2]) < SimLive.ZERO_TOL) {
								break;
							}
						}
						if (n == elementNodes.length) {
							warnings.add("Connector \""+connectors.get(c).name+"\" is not positioned at node.");
							break;
						}
					}
				}
			}
		}
		
		for (int c = 0; c < contactPairs.size(); c++) {
			if (contactPairs.get(c).getSlaveNodes().isEmpty() ||
				contactPairs.get(c).getMasterSets().isEmpty() ||
				(Model.twoDimensional && !contactPairs.get(c).hasStoredEdges())) {
				errors.add("Contact \""+contactPairs.get(c).name+"\" is not completely defined.");
			}
		}
		
		for (int l = 0; l < loads.size(); l++) {
			TimeTable timeTable = loads.get(l).getTimeTable();
			for (int i = 1; i < timeTable.getNumberOfRows(); i++) {
				if (timeTable.getTime(i) <= timeTable.getTime(i-1)) {
					errors.add("Load \""+loads.get(l).name+"\" has time dependency in wrong order.");
					break;
				}				
			}
		}
		
		for (int d = 0; d < distributedLoads.size(); d++) {
			TimeTable timeTable = distributedLoads.get(d).getTimeTable();
			for (int i = 1; i < timeTable.getNumberOfRows(); i++) {
				if (timeTable.getTime(i) <= timeTable.getTime(i-1)) {
					errors.add("Distributed Load \""+distributedLoads.get(d).name+"\" has time dependency in wrong order.");
					break;
				}				
			}
		}
		
		return errors.isEmpty();
	}
	
	public int getDofOfNodeID(int nodeID) {
		return dofOfNodeID[nodeID];
	}
	
	public void calculate(SolutionDialog dialog) {
		
		dialog.initProgressBar(refModel.getSteps().get(0).type == Type.MODAL_ANALYSIS ? 5*(nDofs-nSuppressedDofs) : nIncrements + 1);
		
		int startInc = 0;
		double startTime = 0.0;
		double minElemLength = Double.MAX_VALUE;
		
		contacts = new Contact[refModel.getNodes().size()];
		
		for (int e = 0; e < refModel.getElements().size(); e++) {
			refModel.getElements().get(e).initMelem(refModel.getNodes());
			if (refModel.getElements().get(e).isPlaneElement()) {
				((PlaneElement) refModel.getElements().get(e)).initKelem(refModel.getNodes());
			}
			int[] elemNodes = refModel.getElements().get(e).getElementNodes();
			for (int i = 1; i < elemNodes.length; i++) {
				double[] coords0 = refModel.getNodes().get(elemNodes[i-1]).getCoords();
				double[] coords1 = refModel.getNodes().get(elemNodes[i]).getCoords();
				minElemLength = Math.min(minElemLength, (coords1[0]-coords0[0])*(coords1[0]-coords0[0])+
						(coords1[1]-coords0[1])*(coords1[1]-coords0[1])+
						(coords1[2]-coords0[2])*(coords1[2]-coords0[2]));
			}
		}
		minElemLength = Math.sqrt(minElemLength);
		
		for (int s = 0; s < refModel.getSteps().size(); s++) {
			Step step = refModel.getSteps().get(s);
			
			log.add("STEP: \""+step.name+"\"");
			dialog.updateLog();
			
			switch (step.type) {
			
				case MECHANICAL_STATIC:	if (!staticSolution(dialog, step, startInc, startTime, minElemLength)) {
											return;
										}
										break;
										
				case EXPLICIT_DYNAMIC:	if (!dynamicSolution(dialog, step, startInc, startTime)) {
											return;
										}
										break;
														
				case MODAL_ANALYSIS:	if (!modalAnalysis(dialog)) {
											return;
										}
										break;
										
				default:				break;										
			}
			
			startInc += step.nIncrements+1;
			startTime += step.duration;
		}
	}
	
	private boolean staticSolution(SolutionDialog dialog, Step step, int startInc, double startTime, double minElemLength) {
		
		final double CONV_RATIO = 0.01;
		final double DIV_RATIO = 1E20;
		
		int stepNr = refModel.getSteps().indexOf(step);
		
		int maxIterations = 1;
		if (!refModel.getContactPairs().isEmpty() ||
				refModel.settings.isLargeDisplacement) maxIterations = step.maxIterations;
		
		final double timeStep = step.duration/step.nIncrements;
		
		Matrix u_global = new Matrix(nDofs, 1);
		if (startInc > 0) {
			u_global = increments[startInc-1].get_u_global().copy();
		}
		Matrix delta_u_constr = null;
		Matrix K_global = null;
		Matrix G = null;
		Matrix g = null;
		Matrix K_constr = null;
		Matrix delta_f_constr = null;
		double norm0 = 0.0;
		
		log.add("STATIC SOLUTION");
		dialog.updateLog();
		log.add("Time step = "+SimLive.double2String(timeStep));
		dialog.updateLog();
		
		Matrix f_gravity = new Increment(this, 0.0, stepNr).getGravityForce(nDofs, step, null);
		Matrix G_support = new Increment(this, 0.0, stepNr).getSupportPartOfGMatrix(nDofs);
		
		for (int i = startInc; i < startInc+step.nIncrements+1; i++) {
			
			double time = (i-startInc)*timeStep+startTime;
			
			log.add("Increment "+i+": Time = "+SimLive.double2String(time));
			dialog.updateLog();
			
			increments[i] = new Increment(this, time, stepNr);
			Matrix u_global0 = null;
			if (i > 0) {
				u_global0 = increments[i-1].get_u_global();
			}
			else {
				u_global0 = new Matrix(nDofs, 1);
			}
			Matrix C_reaction_global = new Matrix(nDofs, 1);
			Matrix C_coupling_global = new Matrix(nDofs, 1);
			Matrix f_int = increments[i].assembleForceParallel(nDofs, u_global);
			Matrix f_ext = null;
			
			for (int iter = 0; iter < maxIterations; iter++) {
				
				/* contact search */
				Contact.search(contacts, refModel.getContactPairs(), this, u_global, u_global0, C_coupling_global);
				
				Matrix f_fric = increments[i].getFrictionForce(nDofs, contacts, C_coupling_global, null, null, 0.0);
				f_ext = increments[i].getExternalForce(nDofs, step, f_gravity, u_global).plus(f_fric);
				
				K_global = increments[i].assembleStiffnessParallel(nDofs, u_global);
				Matrix G_connect = increments[i].getConnectorPartOfGMatrix(nDofs, u_global);
				Matrix G_contact = increments[i].getContactPartOfGMatrix(nDofs, contacts, u_global);
				Matrix G_load = increments[i].getLoadPartOfGMatrix(nDofs, u_global);
				G = increments[i].getAssembledGMatrix(nDofs, G_support, G_connect, G_contact, G_load);
				Matrix g_connect = increments[i].getConnectorPartOfgMatrix(nDofs, u_global);
				Matrix g_contact = increments[i].getContactPartOfgMatrix(nDofs, contacts);
				Matrix g_load = increments[i].getLoadPartOfgMatrix(nDofs, u_global);
				g = increments[i].getAssembledgMatrix(G, g_connect, g_contact, g_load);
				K_constr = constraintMethod.getConstrainedMatrix(K_global, G);
				delta_f_constr = constraintMethod.getConstrainedRHS(f_ext.minus(f_int), C_reaction_global.plus(C_coupling_global), K_global, G, g);
				
				try {
					if (Model.twoDimensional) {
						K_constr = getMatrix2d(K_constr);
						delta_f_constr = getVector2d(delta_f_constr, false);
						delta_u_constr = getVector2d(K_constr.solve(delta_f_constr), true);
					}
					else {
						delta_u_constr = K_constr.solve(delta_f_constr);
					}
				}
				catch (RuntimeException e) {
					return solutionError(i, e.getMessage());
				}
			
				Matrix delta_u_global = constraintMethod.getFullSolution(delta_u_constr, G);
				u_global = increments[i].updateSolution(u_global, delta_u_global);
				f_int = increments[i].assembleForceParallel(nDofs, u_global);
				delta_f_constr = constraintMethod.getConstrainedRHS(f_ext.minus(f_int), C_reaction_global.plus(C_coupling_global), K_global, G, g);
				if (Model.twoDimensional) {
					delta_f_constr = getVector2d(delta_f_constr, false);
				}
				
				Matrix G_onlyCoupling = increments[i].getAssembledGMatrix(nDofs, G_support.getEmptyCopy(), G_connect, G_contact, G_load.getEmptyCopy());
				C_coupling_global = constraintMethod.getConstraintForce(C_coupling_global, delta_u_constr, G_onlyCoupling, g, K_global);
				
				Matrix G_onlyReaction = increments[i].getAssembledGMatrix(nDofs, G_support, G_connect.getEmptyCopy(), G_contact.getEmptyCopy(), G_load);
				C_reaction_global = constraintMethod.getConstraintForce(C_reaction_global, delta_u_constr, G_onlyReaction, g, K_global);
				
				if (iter == 0) {
					norm0 = delta_u_global.normF();
				}
				else {
					double norm = delta_u_global.normF();
					double ratio = norm/norm0;
					if (norm < SimLive.ZERO_TOL*minElemLength) ratio = 0.0;
					
					log.add("     Iteration "+(iter+1)+": Ratio = "+SimLive.double2String(ratio));
					dialog.updateLog();
					
					if (ratio < CONV_RATIO) {
						log.add("     Convergence after "+(iter+1)+" iterations.");
						dialog.updateLog();
						break;
					}
					if (ratio > DIV_RATIO) {
						return divergenceError(i);
					}
				}
			}
			
			Matrix v_global = null;
			Matrix a_global = null;
			if (i > 0) {
				if (i == startInc) {
					v_global = increments[i-1].get_v_global();
					a_global = increments[i-1].get_a_global();
				}
				else {
					v_global = u_global.minus(u_global0).times(1.0/timeStep);
					a_global = v_global.minus(increments[i-1].get_v_global()).times(1.0/timeStep);
				}
			}
			else {
				v_global = u_global.times(1.0/timeStep);
				a_global = v_global.times(1.0/timeStep);
			}
			
			increments[i].setResults(u_global, v_global, a_global, C_reaction_global);
				
			if (isWriteMatrixView) {
				Matrix u_global_old = u_global.minus(constraintMethod.getFullSolution(delta_u_constr, G));
				Matrix[] K_elem = increments[i].getElementStiffnessArray(u_global_old);
				increments[i].setResultsForMatrixView(K_elem, null, null, K_global, f_ext, f_int,
						G, K_constr, null, delta_f_constr);
			}
			
			dialog.incrementProgressBar();
		}
		
		return true;
	}
	
	private boolean dynamicSolution(SolutionDialog dialog, Step step, int startInc, double startTime) {
		
		int stepNr = refModel.getSteps().indexOf(step);
		
		Matrix u_global = new Matrix(nDofs, 1);
		Matrix v_global = new Matrix(nDofs, 1);
		Matrix C_coupling_global = new Matrix(nDofs, 1);
		if (startInc > 0) {
			u_global = increments[startInc-1].get_u_global().copy();
			v_global = increments[startInc-1].get_v_global().copy();				
		}
		Matrix u_global_old = u_global.copy();
		
		int postInc = startInc;
		int nInc = new Increment(this, 0.0, stepNr).getNumberOfDynamicTimeSteps(nDofs, u_global, step);
		double timeStep = step.duration/nInc;
		
		log.add("DYNAMIC SOLUTION");
		dialog.updateLog();
		log.add("Time step = "+SimLive.double2String(timeStep));
		dialog.updateLog();
		
		Matrix M_global = new Increment(this, 0.0, stepNr).assembleMassSequential(nDofs);
		Matrix f_gravity = new Increment(this, 0.0, stepNr).getGravityForce(nDofs, step, M_global);
		Matrix G_support = new Increment(this, 0.0, stepNr).getSupportPartOfGMatrix(nDofs);
		ArrayList<Element> dStiffElems = new ArrayList<Element>();
		ArrayList<Element> dMassElems = new ArrayList<Element>();
		for (int elem = 0; elem < refModel.getElements().size(); elem++) {
			if (refModel.getElements().get(elem).getStiffnessDamping() > 0) {
				dStiffElems.add(refModel.getElements().get(elem));
			}
			if (refModel.getElements().get(elem).getMassDamping() > 0) {
				dMassElems.add(refModel.getElements().get(elem));
			}
		}
		
		for (int i = 0; i < nInc+1; i++) {
			
			double time = startTime+i*timeStep;
			
			Increment increment = new Increment(this, time, stepNr);
			
			Matrix u_global_with_delta = u_global.times(2.0).minus(u_global_old);
			
			/* contact search */
			Contact.search(contacts, refModel.getContactPairs(), this, u_global_with_delta, u_global_with_delta, C_coupling_global);
			
			Matrix f_fric = increment.getFrictionForce(nDofs, contacts, C_coupling_global, v_global, M_global, timeStep);
			Matrix f_ext = increment.getExternalForce(nDofs, step, f_gravity, u_global).plus(f_fric);
			Matrix G_connect = increment.getConnectorPartOfGMatrix(nDofs, u_global);
			Matrix G_contact = increment.getContactPartOfGMatrix(nDofs, contacts, u_global);
			Matrix G_load = increment.getLoadPartOfGMatrix(nDofs, u_global);
			Matrix G = increment.getAssembledGMatrix(nDofs, G_support, G_connect, G_contact, G_load);
			Matrix g_connect = increment.getConnectorPartOfgMatrix(nDofs, u_global_with_delta);
			Matrix g_contact = increment.getContactPartOfgMatrix(nDofs, contacts);
			Matrix g_load = increment.getLoadPartOfgMatrix(nDofs, u_global_with_delta);
			Matrix g = increment.getAssembledgMatrix(G, g_connect, g_contact, g_load).times(1.0/(timeStep*timeStep));
			Matrix M_constr = constraintMethod.getConstrainedMatrix(M_global, G);
			Matrix f_int = increment.assembleForceParallel(nDofs, u_global);
			f_int = increment.addDampingForce(nDofs, f_int, u_global, v_global, dStiffElems, dMassElems);
			Matrix delta_f_constr = constraintMethod.getConstrainedRHS(f_ext.minus(f_int), new Matrix(nDofs, 1), M_global, G, g);
			
			Matrix a_constr = null;
			try {
				if (Model.twoDimensional) {
					M_constr = getMatrix2d(M_constr);
					delta_f_constr = getVector2d(delta_f_constr, false);
					a_constr = getVector2d(M_constr.solve(delta_f_constr), true);
				}
				else {
					a_constr = M_constr.solve(delta_f_constr);
				}
				
				// modification of a_constr for finite rotations
				if (refModel.settings.isLargeDisplacement) {
					for (int n = 0; n < refModel.getNodes().size(); n++) if (refModel.getNodes().get(n).isRotationalDOF()) {
						int dof = dofOfNodeID[n];
						Matrix PsiDotDot = a_constr.getMatrix(dof+3, dof+5, 0, 0);
						Matrix PsiDot = v_global.getMatrix(dof+3, dof+5, 0, 0);
						Matrix Psi = u_global.getMatrix(dof+3, dof+5, 0, 0);
						Matrix TInv = Beam.getTInv(Psi);
						Matrix TDot = Beam.getTDot(Psi, PsiDot);
						a_constr.setMatrix(dof+3, dof+5, 0, 0, TInv.times(PsiDotDot.minus(TDot.times(PsiDot))));
					}
				}
			}
			catch (RuntimeException e) {
				return solutionError(postInc, e.getMessage());
			}
			
			Matrix a_global = constraintMethod.getFullSolution(a_constr, G);
			
			if (i == 0) {
				/* get u_global_old */
				u_global_old = u_global.minus(v_global.times(timeStep)).plus(a_global.times(timeStep*timeStep/2.0));
			}
			
			Matrix u_global_new = a_global.times(timeStep*timeStep).plus(u_global.times(2.0)).minus(u_global_old);
			v_global = u_global_new.minus(u_global_old).times(1.0/(2.0*timeStep));
			
			Matrix G_onlyCoupling = increment.getAssembledGMatrix(nDofs, G_support.getEmptyCopy(), G_connect, G_contact, G_load.getEmptyCopy());
			C_coupling_global = constraintMethod.getConstraintForce(new Matrix(nDofs, 1), a_constr, G_onlyCoupling, g, M_global);
			
			/* post increment */
			if (i%(nInc/step.nIncrements) == 0) {
				
				log.add("Increment "+postInc+": Time = "+SimLive.double2String(time));
				dialog.updateLog();
				
				Matrix G_onlyReaction = increment.getAssembledGMatrix(nDofs, G_support, G_connect.getEmptyCopy(), G_contact.getEmptyCopy(), G_load);
				Matrix C_reaction_global = constraintMethod.getConstraintForce(new Matrix(nDofs, 1), a_constr, G_onlyReaction, g, M_global);
				
				increment.setResults(u_global.copy(), v_global.copy(), a_global, C_reaction_global);
				
				if (isWriteMatrixView) {
					Matrix[] K_elem = increment.getElementStiffnessArray(u_global);
					Matrix[] M_elem = increment.getElementMassArray();
					Matrix K_global = increment.assembleStiffnessParallel(nDofs, u_global);
					increment.setResultsForMatrixView(K_elem, M_elem, M_global, K_global, f_ext, f_int,
							G, null, M_constr, delta_f_constr);
				}
				
				increments[postInc] = increment;
				
				postInc++;
				
				dialog.incrementProgressBar();
			}
			
			u_global_old = u_global;
			u_global = u_global_new;
		}
		
		return true;
	}
	
	private boolean modalAnalysis(SolutionDialog dialog) {
		
		log.add("MODAL ANALYSIS\nCalculate Matrix...");
		dialog.updateLog();
		
		Increment increment = new Increment(this, 0.0, 0);
		Matrix K_global = increment.assembleStiffnessParallel(nDofs, null);
		Matrix M_global = increment.assembleMassSequential(nDofs);
		Matrix G_support = increment.getSupportPartOfGMatrix(nDofs);
		Matrix G_connect = increment.getConnectorPartOfGMatrix(nDofs, new Matrix(nDofs, 1));
		Matrix G_contact = new Matrix(0, nDofs);
		Matrix G_load = new Matrix(0, nDofs);
		Matrix G = increment.getAssembledGMatrix(nDofs, G_support, G_connect, G_contact, G_load);
		Matrix M_constr = constraintMethod.getConstrainedMatrix(M_global, G);
		Matrix K_constr = constraintMethod.getConstrainedMatrix(K_global, G);
		
		try {
			if (Model.twoDimensional) {
				M_constr = getMatrix2d(M_constr);
				K_constr = getMatrix2d(K_constr);
			}
			Matrix A = M_constr.inverse().times(K_constr);
			
			log.add("Calculate Eigenmodes...");
			dialog.updateLog();
			
			EigenvalueDecomposition eig = A.eig(true);
			Matrix Dmatrix = eig.getD();
			D = new Matrix(Dmatrix.getRowDimension(), 1);
			for (int i = 0; i < Dmatrix.getRowDimension(); i++) {
				D.set(i, 0, Dmatrix.get(i, i));
			}
			V = eig.getV();
		}
		catch (RuntimeException e) {
			return solutionError(0, e.getMessage());
		}
		
		log.add("Sort Eigenmodes...");
		dialog.updateLog();
		
		/* get full solution for eigenvectors and normalize */
		final Matrix Vnew = new Matrix(nDofs, V.getColumnDimension());
		IntStream.range(0, V.getColumnDimension()).parallel().forEach(i -> {
			Matrix eigenVector = V.getMatrix(0, V.getRowDimension()-1, i, i);
			if (Model.twoDimensional) {
				eigenVector = getVector2d(eigenVector, true);
			}
			eigenVector = constraintMethod.getFullSolution(eigenVector, G);
			double norm = eigenVector.normF();
			Vnew.setMatrix(0, nDofs-1, i, i, eigenVector.times(1.0/norm));
		});
		V = Vnew;
		
		/* sort eigenmodes after violation of constraint conditions */
		double[] norm = new double[V.getColumnDimension()];
		IntStream.range(0, V.getColumnDimension()).parallel().forEach(i -> {
			Matrix eigenVector = V.getMatrix(0, V.getRowDimension()-1, i, i);
			norm[i] = G.times(eigenVector).normF();
		});
	    int[] indices = IntStream.range(0, norm.length)
                .boxed().sorted((i, j) -> Double.compare(norm[i], norm[j]) )
                .mapToInt(ele -> ele).toArray();
		Matrix Dsort = new Matrix(D.getRowDimension(), 1);
		Matrix Vsort = new Matrix(nDofs, V.getColumnDimension());
		IntStream.range(0, indices.length).parallel().forEach(i -> {
			Dsort.set(i, 0, D.get(indices[i], 0));
			Vsort.setMatrix(0, V.getRowDimension()-1, i, i,
					V.getMatrix(0, V.getRowDimension()-1, indices[i], indices[i]));
		});
		D = Dsort;
		V = Vsort;
		
		/* remove eigenmodes caused by artificial stiffness of plane elements */
		boolean[] isBeamNode = new boolean[refModel.getNodes().size()];
		for (int e = 0; e < refModel.getElements().size(); e++) {
			Element element = refModel.getElements().get(e);
			if (element.getType() == Element.Type.BEAM) {
				int[] elementNodes = element.getElementNodes();
				isBeamNode[elementNodes[0]] = true;
				isBeamNode[elementNodes[1]] = true;
			}
		}
		boolean[] remove = new boolean[V.getColumnDimension()];
		IntStream.range(0, V.getColumnDimension()).parallel().forEach(i -> {
			Matrix eigenVector = V.getMatrix(0, V.getRowDimension()-1, i, i);
			boolean noDisp = true;
			boolean maxRotIsPlaneElement = false;
			double maxRot = 0;
			for (int n = 0; n < refModel.getNodes().size(); n++) {
				int dof = this.getDofOfNodeID(n);
				double disp = Math.sqrt(eigenVector.get(dof, 0)*eigenVector.get(dof, 0)+
						eigenVector.get(dof+1, 0)*eigenVector.get(dof+1, 0)+
						eigenVector.get(dof+2, 0)*eigenVector.get(dof+2, 0));
				if (disp > SimLive.ZERO_TOL) {
					noDisp = false;
					break;
				}
				if (refModel.getNodes().get(n).isRotationalDOF()) {
					double rot = Math.sqrt(eigenVector.get(dof+3, 0)*eigenVector.get(dof+3, 0)+
							eigenVector.get(dof+4, 0)*eigenVector.get(dof+4, 0)+
							eigenVector.get(dof+5, 0)*eigenVector.get(dof+5, 0));
					if (rot > maxRot) {
						maxRot = rot;
						maxRotIsPlaneElement = !isBeamNode[n];
					}
				}
			}
			if (noDisp && maxRotIsPlaneElement) {
				remove[i] = true;
			}
		});
		int[] ind = new int[V.getColumnDimension()];
		int count = 0;
		for (int i = 0; i < V.getColumnDimension(); i++) if (!remove[i]) {
			ind[count++] = i;
		}
		Matrix Dmod = new Matrix(count, 1);
		Matrix Vmod = new Matrix(V.getRowDimension(), count);
		IntStream.range(0, count).parallel().forEach(i -> {
			Dmod.set(i, 0, D.get(ind[i], 0));
			Vmod.setMatrix(0, V.getRowDimension()-1, i, i, V.getMatrix(0, V.getRowDimension()-1, ind[i], ind[i]));
		});
		D = Dmod;
		V = Vmod;
		
		/* remove eigenmodes that violate constraint conditions most excessive */
		D = constraintMethod.removeInvalidEigenvalues(D, G);
		V = constraintMethod.removeInvalidEigenvectors(V, G);
		
		if (D.getRowDimension() == 0) {
			return noEigenmodeError();
		}
		
		/* check for remaining negative eigenvalues */
		for (int i = 0; i < D.getRowDimension(); i++) {
			if (D.get(i, 0) < 0.0) {
				return negativeEigenvaluesError();
			}
		}
		
		/* sort eigenmodes */
		ArrayList<Double[]> frequencies = new ArrayList<Double[]>();
		for (int n = 0; n < D.getRowDimension(); n++) {
			frequencies.add(new Double[]{D.get(n, 0), (double) n});
		}
		Matrix Vtemp = new Matrix(V.getRowDimension(), V.getColumnDimension());;
		for (int n = 0; n < D.getRowDimension(); n++) {
			double lowestFrequency = Double.MAX_VALUE;
			int column = -1;
			int index = -1;
			for (int m = 0; m < frequencies.size(); m++) {
				if (frequencies.get(m)[0] < lowestFrequency) {
					lowestFrequency = frequencies.get(m)[0];
					double c = frequencies.get(m)[1];
					column = (int) c;
					index = m;
				}
			}
			D.set(n, 0, lowestFrequency);
			Vtemp.setMatrix(0, Vtemp.getRowDimension()-1, n, n, V.getMatrix(0, V.getRowDimension()-1, column, column));
			frequencies.remove(index);
		}
		V = Vtemp;
		
		/* calculate angular frequencies from eigenvalues */
		for (int i = 0; i < D.getRowDimension(); i++) {
			D.set(i, 0, Math.sqrt(D.get(i, 0)));
		}
		
		log.add(D.getRowDimension()+" Eigenmodes calculated.");
		dialog.updateLog();
		
		return true;
	}
	
	private boolean divergenceError(int postInc) {
		return solutionError(postInc, "Solution is divergent.");
	}
	
	private boolean negativeEigenvaluesError() {
		return solutionError(0, "Matrix has negative eigenvalues.");
	}
	
	private boolean noEigenmodeError() {
		return solutionError(0, "No eigenmode was calculated.");
	}
	
	private boolean solutionError(int postInc, String text) {
		if (postInc > 1) {
			nIncrements = postInc - 1;
			increments = Arrays.copyOf(increments, postInc);
			errors.add(text + " Results are available.");
		}
		else {
			nIncrements = 0;
			increments = null;
			errors.add(text);
		}
		return false;
	}
	
	public void calculateIncrementsForEigenmode(int eigenmode) {
		
		double omega = D.get(eigenmode, 0);
		Matrix eigenVector = V.getMatrix(0, V.getRowDimension()-1, eigenmode, eigenmode);
		
		this.increments = new Increment[nIncrements + 1];
		final double timeStep = 2.0*Math.PI/omega/nIncrements;
		
		for (int i = 0; i < nIncrements+1; i++) {
			
			increments[i] = new Increment(this, i*timeStep, 0);
			Matrix u_global = eigenVector.times(Math.sin(omega*i*timeStep));
			Matrix v_global = eigenVector.times(omega*Math.cos(omega*i*timeStep));
			Matrix a_global = eigenVector.times(-omega*omega*Math.sin(omega*i*timeStep));
			Matrix r_global = new Matrix(nDofs, 1);
			increments[i].setResults(u_global, v_global, a_global, r_global);
			
			if (isWriteMatrixView) {
				Matrix K_global = increments[i].assembleStiffnessParallel(nDofs, u_global);
				Matrix M_global = increments[i].assembleMassSequential(nDofs);
				Matrix G_support = increments[i].getSupportPartOfGMatrix(nDofs);
				Matrix G_connect = increments[i].getConnectorPartOfGMatrix(nDofs, u_global);
				Matrix G_contact = new Matrix(0, nDofs);
				Matrix G_load = new Matrix(0, nDofs);
				Matrix G = increments[i].getAssembledGMatrix(nDofs, G_support, G_connect, G_contact, G_load);
				Matrix M_constr = constraintMethod.getConstrainedMatrix(M_global, G);
				Matrix K_constr = constraintMethod.getConstrainedMatrix(K_global, G);
				if (Model.twoDimensional) {
					M_constr = getMatrix2d(M_constr);
					K_constr = getMatrix2d(K_constr);
				}
				Matrix[] K_elem = increments[i].getElementStiffnessArray(u_global);
				Matrix[] M_elem = increments[i].getElementMassArray();
				
				increments[i].setResultsForMatrixView(K_elem, M_elem, M_global, K_global, null, null, G,
						K_constr, M_constr, null);
			}
		}
	}
	
	public Matrix getD() {
		return D;
	}

	public void setD(Matrix d) {
		D = d;
	}

	public Matrix getV() {
		return V;
	}

	public void setV(Matrix v) {
		V = v;
	}

	public Model getRefModel() {
		return refModel;
	}
	
	public int getNumberOfDofs() {
		return nDofs;
	}
	
	public int getNumberOfIncrements() {
		return nIncrements;
	}

	public Increment getIncrement(int incrementID) {
		return increments[incrementID];
	}
	
	public void setIncrements(Increment[] increments) {
		this.increments = increments;
		nIncrements = increments.length-1;
	}

	public ConstraintMethod getConstraintMethod() {
		return constraintMethod;
	}

	private void setSuppressedDofs2d() {
		nSuppressedDofs = 0;
		suppressedDof = new boolean[nDofs];
		for (int n = 0; n < refModel.getNodes().size(); n++) {
			int dof = dofOfNodeID[n];
			suppressedDof[dof+2] = true;
			nSuppressedDofs++;
			if (refModel.getNodes().get(n).isRotationalDOF()) {
				suppressedDof[dof+3] = true;
				suppressedDof[dof+4] = true;
				nSuppressedDofs +=2;
			}
		}
	}
	
	private Matrix getMatrix2d(Matrix matrix) {
		int nDofs = matrix.getRowDimension();
		Matrix matrixConstr = new Matrix(nDofs-nSuppressedDofs, nDofs-nSuppressedDofs);
		for (int r = 0, i = 0; i < nDofs; i++) {
			if (i >= suppressedDof.length || !suppressedDof[i]) {
				for (int c = 0, j = 0; j < nDofs; j++) {
					if (j >= suppressedDof.length || !suppressedDof[j]) {
						matrixConstr.set(r, c, matrix.get(i, j));
						c++;
					}
				}
				r++;
			}
		}
		return matrixConstr;
	}
	
	private Matrix getVector2d(Matrix vector, boolean inverse) {
		int nDofs = vector.getRowDimension();
		Matrix vectorConstr = null;
		if (inverse) {
			nDofs += nSuppressedDofs;
			vectorConstr = new Matrix(nDofs, 1);
		}
		else {
			vectorConstr = new Matrix(nDofs-nSuppressedDofs, 1);
		}
		for (int r = 0, i = 0; i < nDofs; i++) {
			if (i >= suppressedDof.length || !suppressedDof[i]) {
				if (inverse) vectorConstr.set(i, 0, vector.get(r, 0));
				else 		 vectorConstr.set(r, 0, vector.get(i, 0));
				r++;
			}
		}
		return vectorConstr;
	}
	
}

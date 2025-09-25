package simlive.solution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wb.swt.SWTResourceManager;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import simlive.SimLive;
import simlive.misc.GeomUtility;
import simlive.model.Beam;
import simlive.model.Connector;
import simlive.model.DistributedLoad;
import simlive.model.Element;
import simlive.model.LineElement;
import simlive.model.Load;
import simlive.model.Model;
import simlive.model.Node;
import simlive.model.PlaneElement;
import simlive.model.Set;
import simlive.model.Step;
import simlive.model.Support;
import simlive.model.Step.GRAVITY;
import simlive.view.View;

public class Increment {

	protected Solution solution;
	private Matrix[] M_elem, K_elem;
	private Matrix M_global, K_global, f_ext, f_int, u_global, v_global, a_global,
			G, r_global, K_constr, M_constr, delta_f_constr;
	private Matrix[] Rr_beam;
	private double[][][] angles, angularVel, angularAcc;
	private double time;
	private int stepNr;
	
	public Increment(Solution solution, double time, int stepNr) {
		this.solution = solution;
		this.time = time;
		this.stepNr = stepNr;
	}
	
	public Matrix assembleMassSequential(int nDofs) {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		Matrix M_global = new Matrix(nDofs, nDofs);
		
		for (int elem = 0; elem < elements.size(); elem++) {
			Matrix M_elem = elements.get(elem).M_elem;
			M_global = elements.get(elem).addLocalToGlobalMatrix(M_elem, M_global);
		}
		
		return M_global;
	}
	
	public Matrix assembleStiffnessParallel(int nDofs, Matrix u_global) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		Matrix[] K_elem = new Matrix[elements.size()];
		IntStream.range(0, elements.size()).parallel().forEach(e -> {
			if (solution.getRefSettings().isLargeDisplacement) {
				K_elem[e] = elements.get(e).getElementStiffnessNL(nodes, u_global);
			}
			else {
				K_elem[e] = elements.get(e).getElementStiffness(nodes);
			}
	    });
		Matrix result = new Matrix(nDofs, nDofs);
		for (int e = 0; e < elements.size(); e++) {
			result = elements.get(e).addLocalToGlobalMatrix(K_elem[e], result);
		}
		
		return result;
	}
	
	public Matrix[] getElementMassArray() {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		Matrix[] M_elem = new Matrix[elements.size()];
		
		for (int elem = 0; elem < elements.size(); elem++) {
			M_elem[elem] = elements.get(elem).M_elem;
		}
		
		return M_elem;
	}
	
	public Matrix[] getElementStiffnessArray(Matrix u_global) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		Matrix[] K_elem = new Matrix[elements.size()];
		
		for (int elem = 0; elem < elements.size(); elem++) {
			if (solution.getRefSettings().isLargeDisplacement) {
				K_elem[elem] = elements.get(elem).getElementStiffnessNL(nodes, u_global);
			}
			else {
				K_elem[elem] = elements.get(elem).getElementStiffness(nodes);
			}
		}
		
		return K_elem;
	}
	
	public Matrix assembleForceParallel(int nDofs, Matrix u_global) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		Matrix[] f_elem = new Matrix[elements.size()];
		IntStream.range(0, elements.size()).parallel().forEach(e -> {
			if (solution.getRefSettings().isLargeDisplacement) {
				f_elem[e] = elements.get(e).getElementForceNL(nodes, u_global, false);
			}
			else {
				f_elem[e] = elements.get(e).getElementForce(nodes, u_global, false);
			}
	    });
		Matrix result = new Matrix(nDofs, 1);
		for (int e = 0; e < elements.size(); e++) {
			result = elements.get(e).addLocalToGlobalMatrix(f_elem[e], result);
		}
		
		return result;
	}
	
	public Matrix getDMassMatrix(int nDofs) {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		Matrix D_mass = new Matrix(nDofs, nDofs);
		
		for (int elem = 0; elem < elements.size(); elem++) {
			Matrix M_elem = elements.get(elem).M_elem;
			D_mass = elements.get(elem).addLocalToGlobalMatrix(M_elem.times(elements.get(elem).getMassDamping()), D_mass);
		}
		
		return D_mass;
	}
	
	public Matrix getDStiffMatrix(int nDofs, Matrix u_global, ArrayList<Element> dStiffElems) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		
		Matrix[] K_elem = new Matrix[dStiffElems.size()];
		IntStream.range(0, dStiffElems.size()).parallel().forEach(e -> {
			if (solution.getRefSettings().isLargeDisplacement) {
				K_elem[e] = dStiffElems.get(e).getElementStiffnessNL(nodes, u_global);
			}
			else {
				K_elem[e] = dStiffElems.get(e).getElementStiffness(nodes);
			}
	    });
		Matrix result = new Matrix(nDofs, nDofs);
		for (int e = 0; e < dStiffElems.size(); e++) {
			Element element = dStiffElems.get(e);
			result = element.addLocalToGlobalMatrix(K_elem[e].times(element.getStiffnessDamping()), result);
		}
		
		return result;
	}
	
	public Matrix getFrictionForce(int nDofs, Contact[] contacts, Matrix C_global, Matrix v_global, Matrix M_global, double timeStep) {
		Matrix f_fric = new Matrix(nDofs, 1);
		boolean calledFromStatic = v_global == null && M_global == null && timeStep == 0.0;
		boolean calledFromDynamic = v_global != null && M_global != null && timeStep > 0.0;
		for (int c = 0; c < contacts.length; c++) {
			if (contacts[c] != null) {
				Element masterElement = contacts[c].getMasterElement();
				double[] shapeFunctionValues = contacts[c].getShapeFunctionValues();
				
				int[] element_nodes = masterElement.getElementNodes();
				int dof_n = solution.getDofOfNodeID(c);
				double[] norm = contacts[c].getNorm();
				
				double scal = C_global.get(dof_n, 0)*norm[0]+C_global.get(dof_n+1, 0)*norm[1]+C_global.get(dof_n+2, 0)*norm[2];
				double normalForce = Math.abs(scal);
				double fricForce = contacts[c].getFrictionCoefficient()*normalForce;
				
				double[] fricDir = new double[3];
				
				double[] tForce = new double[3];
				tForce[0] = C_global.get(dof_n, 0)-scal*norm[0];
				tForce[1] = C_global.get(dof_n+1, 0)-scal*norm[1];
				tForce[2] = C_global.get(dof_n+2, 0)-scal*norm[2];
				double tangentialForce = Math.sqrt(tForce[0]*tForce[0]+tForce[1]*tForce[1]+tForce[2]*tForce[2]);
				
				if (calledFromStatic) {
					
					if (normalForce > SimLive.ZERO_TOL && tangentialForce > fricForce-SimLive.ZERO_TOL) {
						contacts[c].setSticking(false);
						
						if (tangentialForce > SimLive.ZERO_TOL) {
							fricDir[0] = tForce[0]/tangentialForce;
							fricDir[1] = tForce[1]/tangentialForce;
							fricDir[2] = tForce[2]/tangentialForce;
						}
						
						C_global.set(dof_n, 0, C_global.get(dof_n, 0)-tForce[0]);
						C_global.set(dof_n+1, 0, C_global.get(dof_n+1, 0)-tForce[1]);
						C_global.set(dof_n+2, 0, C_global.get(dof_n+2, 0)-tForce[2]);
						if (contacts[c].isDeformableDeformable()) {
							for (int i = 0; i < element_nodes.length; i++) {
								int dof = solution.getDofOfNodeID(element_nodes[i]);
								C_global.set(dof, 0, C_global.get(dof, 0)+shapeFunctionValues[i]*tForce[0]);
								C_global.set(dof+1, 0, C_global.get(dof+1, 0)+shapeFunctionValues[i]*tForce[1]);
								C_global.set(dof+2, 0, C_global.get(dof+2, 0)+shapeFunctionValues[i]*tForce[2]);
							}
						}
					}
				}
				
				if (calledFromDynamic) {
					
					double[] v_tang = new double[3];
					if (tangentialForce < SimLive.ZERO_TOL) {
						scal = v_global.get(dof_n, 0)*norm[0]+v_global.get(dof_n+1, 0)*norm[1]+v_global.get(dof_n+2, 0)*norm[2];
						v_tang[0] = v_global.get(dof_n, 0)-scal*norm[0];
						v_tang[1] = v_global.get(dof_n+1, 0)-scal*norm[1];
						v_tang[2] = v_global.get(dof_n+2, 0)-scal*norm[2];
						double[] v_tang_master = new double[3];
						if (contacts[c].isDeformableDeformable()) {
							for (int i = 0; i < element_nodes.length; i++) {
								int dof = solution.getDofOfNodeID(element_nodes[i]);
								scal = v_global.get(dof_n, 0)*norm[0]+v_global.get(dof_n+1, 0)*norm[1]+v_global.get(dof_n+2, 0)*norm[2];
								v_tang_master[0] += shapeFunctionValues[i]*(v_global.get(dof, 0)-scal*norm[0]);
								v_tang_master[1] += shapeFunctionValues[i]*(v_global.get(dof+1, 0)-scal*norm[1]);
								v_tang_master[2] += shapeFunctionValues[i]*(v_global.get(dof+2, 0)-scal*norm[2]);
							}
						}
						v_tang[0] -= v_tang_master[0];
						v_tang[1] -= v_tang_master[1];
						v_tang[2] -= v_tang_master[2];
					}
					double v_tangential = Math.sqrt(v_tang[0]*v_tang[0]+v_tang[1]*v_tang[1]+v_tang[2]*v_tang[2]);
										
					// from slip
					if (v_tangential > SimLive.ZERO_TOL) {
						double stickingForce = M_global.get(dof_n, dof_n)*v_tangential/timeStep;
						if (fricForce > stickingForce) {
							fricForce = stickingForce;
						}
						contacts[c].setSticking(false);
						
						fricDir[0] = -v_tang[0]/v_tangential;
						fricDir[1] = -v_tang[1]/v_tangential;
						fricDir[2] = -v_tang[2]/v_tangential;
					}
					
					// from stick
					else {
						if (tangentialForce > fricForce-SimLive.ZERO_TOL) {
							contacts[c].setSticking(false);
							
							if (tangentialForce > SimLive.ZERO_TOL) {
								fricDir[0] = tForce[0]/tangentialForce;
								fricDir[1] = tForce[1]/tangentialForce;
								fricDir[2] = tForce[2]/tangentialForce;
							}
						}
					}
				}
				
				if (!contacts[c].isSticking()) {				
					if (contacts[c].isDeformableDeformable()) {
						for (int n = 0; n < element_nodes.length; n++) {
							int dof = solution.getDofOfNodeID(element_nodes[n]);
							f_fric.set(dof, 0, f_fric.get(dof, 0)-shapeFunctionValues[n]*fricDir[0]*fricForce);
							f_fric.set(dof+1, 0, f_fric.get(dof+1, 0)-shapeFunctionValues[n]*fricDir[1]*fricForce);
							f_fric.set(dof+2, 0, f_fric.get(dof+2, 0)-shapeFunctionValues[n]*fricDir[2]*fricForce);
						}
					}
					f_fric.set(dof_n, 0, f_fric.get(dof_n, 0)+fricDir[0]*fricForce);
					f_fric.set(dof_n+1, 0, f_fric.get(dof_n+1, 0)+fricDir[1]*fricForce);
					f_fric.set(dof_n+2, 0, f_fric.get(dof_n+2, 0)+fricDir[2]*fricForce);
				}
			}
		}
		return f_fric;
	}

	public Matrix getConnectorPartOfGMatrix(int nDofs, Matrix u_global) {
		Matrix G = new Matrix(0, nDofs);
		ArrayList<Connector> connectors = solution.getRefModel().getConnectors();
		
		/* add coupling for connectors */
		for (int c = 0; c < connectors.size(); c++) {
			Element e0 = connectors.get(c).getElement0();
			Element e1 = connectors.get(c).getElement1();
			double[] shapeFunctionValues0 = null;
			double[] shapeFunctionValues1 = null;
			if (e0.isPlaneElement()) {
				double[] r = connectors.get(c).getR0();
				shapeFunctionValues0 = ((PlaneElement) e0).getShapeFunctionValues(r[0], r[1]);
			}
			else {
				double t = connectors.get(c).getT0();
				shapeFunctionValues0 = ((LineElement) e0).getShapeFunctionValues(t);
			}
			if (e1.isPlaneElement()) {
				double[] r = connectors.get(c).getR1();
				shapeFunctionValues1 = ((PlaneElement) e1).getShapeFunctionValues(r[0], r[1]);
			}
			else {
				double t = connectors.get(c).getT1();
				shapeFunctionValues1 = ((LineElement) e1).getShapeFunctionValues(t);
			}
			int[] element_nodes0 = e0.getElementNodes();
			int[] element_nodes1 = e1.getElementNodes();
			int[] dof_e0 = new int[element_nodes0.length];
			int[] dof_e1 = new int[element_nodes1.length];
			for (int i = 0; i < element_nodes0.length; i++) {
				dof_e0[i] = solution.getDofOfNodeID(element_nodes0[i]);
			}
			for (int i = 0; i < element_nodes1.length; i++) {
				dof_e1[i] = solution.getDofOfNodeID(element_nodes1[i]);
			}
			Matrix G_row0 = new Matrix(1, nDofs);
			Matrix G_row1 = new Matrix(1, nDofs);
			Matrix G_row2 = new Matrix(1, nDofs);
			Matrix G_row3 = new Matrix(1, nDofs);
			Matrix G_row4 = new Matrix(1, nDofs);
			Matrix G_row5 = new Matrix(1, nDofs);
			
			Matrix axis = null;
			if (connectors.get(c).getType() == Connector.Type.REVOLUTE) {
				axis = ((Beam) e0).getR0().getMatrix(0, 2, 2, 2);
			}
			
			{
				Connector connector = connectors.get(c);
				
				if (e0.isPlaneElement()) {
					for (int n = 0; n < element_nodes0.length; n++) {
						G_row0.set(0, dof_e0[n], shapeFunctionValues0[n]);
						G_row1.set(0, dof_e0[n]+1, shapeFunctionValues0[n]);
						G_row2.set(0, dof_e0[n]+2, shapeFunctionValues0[n]);
					}
				}
				if (e1.isPlaneElement()) {
					for (int n = 0; n < element_nodes1.length; n++) {
						G_row0.set(0, dof_e1[n], -shapeFunctionValues1[n]);
						G_row1.set(0, dof_e1[n]+1, -shapeFunctionValues1[n]);
						G_row2.set(0, dof_e1[n]+2, -shapeFunctionValues1[n]);
					}
				}
				if (e0.getType() == Element.Type.BEAM) {
					G_row0.set(0, dof_e0[0], shapeFunctionValues0[0]);
					G_row0.set(0, dof_e0[1], shapeFunctionValues0[3]);
					G_row1.set(0, dof_e0[0]+1, shapeFunctionValues0[0]);
					G_row1.set(0, dof_e0[1]+1, shapeFunctionValues0[3]);
					G_row2.set(0, dof_e0[0]+2, shapeFunctionValues0[0]);
					G_row2.set(0, dof_e0[1]+2, shapeFunctionValues0[3]);
					
					if (connector.getType() == Connector.Type.REVOLUTE) {
						Matrix u_elem = ((Beam) e0).globalToLocalVector(u_global);
						Matrix R1g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0));
						Matrix R2g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0));
						Matrix a0 = R1g.times(axis).times(shapeFunctionValues0[0]).plus(R2g.times(axis).times(shapeFunctionValues0[3]));
						a0.timesEquals(1.0/a0.normF());
						
						u_elem = ((Beam) e1).globalToLocalVector(u_global);
						R1g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0));
						R2g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0));
						Matrix a1 = R1g.times(axis).times(shapeFunctionValues1[0]).plus(R2g.times(axis).times(shapeFunctionValues1[3]));
						a1.timesEquals(1.0/a1.normF());
						double length = ((Beam) e1).getCurrentLength(solution.refModel.getNodes(), u_elem);
						Matrix r1 = ((Beam) e1).getr1(solution.refModel.getNodes(), u_elem, length);
						Matrix c1 = a1.crossProduct(r1);
						Matrix b1 = c1.crossProduct(a1);
						
						Matrix b0 = b1.crossProduct(a0);
						Matrix c0 = c1.crossProduct(a0);
						
						G_row3.set(0, dof_e0[0]+3, b0.get(0, 0)*shapeFunctionValues0[0]);
						G_row3.set(0, dof_e0[0]+4, b0.get(1, 0)*shapeFunctionValues0[0]);
						G_row3.set(0, dof_e0[0]+5, b0.get(2, 0)*shapeFunctionValues0[0]);
						G_row3.set(0, dof_e0[1]+3, b0.get(0, 0)*shapeFunctionValues0[3]);
						G_row3.set(0, dof_e0[1]+4, b0.get(1, 0)*shapeFunctionValues0[3]);
						G_row3.set(0, dof_e0[1]+5, b0.get(2, 0)*shapeFunctionValues0[3]);
						G_row3.set(0, dof_e1[0]+3, -b0.get(0, 0)*shapeFunctionValues1[0]);
						G_row3.set(0, dof_e1[0]+4, -b0.get(1, 0)*shapeFunctionValues1[0]);
						G_row3.set(0, dof_e1[0]+5, -b0.get(2, 0)*shapeFunctionValues1[0]);
						G_row3.set(0, dof_e1[1]+3, -b0.get(0, 0)*shapeFunctionValues1[3]);
						G_row3.set(0, dof_e1[1]+4, -b0.get(1, 0)*shapeFunctionValues1[3]);
						G_row3.set(0, dof_e1[1]+5, -b0.get(2, 0)*shapeFunctionValues1[3]);
						
						G_row4.set(0, dof_e0[0]+3, c0.get(0, 0)*shapeFunctionValues0[0]);
						G_row4.set(0, dof_e0[0]+4, c0.get(1, 0)*shapeFunctionValues0[0]);
						G_row4.set(0, dof_e0[0]+5, c0.get(2, 0)*shapeFunctionValues0[0]);
						G_row4.set(0, dof_e0[1]+3, c0.get(0, 0)*shapeFunctionValues0[3]);
						G_row4.set(0, dof_e0[1]+4, c0.get(1, 0)*shapeFunctionValues0[3]);
						G_row4.set(0, dof_e0[1]+5, c0.get(2, 0)*shapeFunctionValues0[3]);
						G_row4.set(0, dof_e1[0]+3, -c0.get(0, 0)*shapeFunctionValues1[0]);
						G_row4.set(0, dof_e1[0]+4, -c0.get(1, 0)*shapeFunctionValues1[0]);
						G_row4.set(0, dof_e1[0]+5, -c0.get(2, 0)*shapeFunctionValues1[0]);
						G_row4.set(0, dof_e1[1]+3, -c0.get(0, 0)*shapeFunctionValues1[3]);
						G_row4.set(0, dof_e1[1]+4, -c0.get(1, 0)*shapeFunctionValues1[3]);
						G_row4.set(0, dof_e1[1]+5, -c0.get(2, 0)*shapeFunctionValues1[3]);					
					}
				}
				if (e1.getType() == Element.Type.BEAM) {
					G_row0.set(0, dof_e1[0], -shapeFunctionValues1[0]);
					G_row0.set(0, dof_e1[1], -shapeFunctionValues1[3]);
					G_row1.set(0, dof_e1[0]+1, -shapeFunctionValues1[0]);
					G_row1.set(0, dof_e1[1]+1, -shapeFunctionValues1[3]);
					G_row2.set(0, dof_e1[0]+2, -shapeFunctionValues1[0]);
					G_row2.set(0, dof_e1[1]+2, -shapeFunctionValues1[3]);
				}
				
				if (e0.getType() == Element.Type.ROD ||
					e0.getType() == Element.Type.SPRING) {
					G_row0.set(0, dof_e0[0], shapeFunctionValues0[0]);
					G_row0.set(0, dof_e0[1], shapeFunctionValues0[1]);
					G_row1.set(0, dof_e0[0]+1, shapeFunctionValues0[0]);
					G_row1.set(0, dof_e0[1]+1, shapeFunctionValues0[1]);
					G_row2.set(0, dof_e0[0]+2, shapeFunctionValues0[0]);
					G_row2.set(0, dof_e0[1]+2, shapeFunctionValues0[1]);
				}
				if (e1.getType() == Element.Type.ROD ||
					e1.getType() == Element.Type.SPRING) {
					G_row0.set(0, dof_e1[0], -shapeFunctionValues1[0]);
					G_row0.set(0, dof_e1[1], -shapeFunctionValues1[1]);
					G_row1.set(0, dof_e1[0]+1, -shapeFunctionValues1[0]);
					G_row1.set(0, dof_e1[1]+1, -shapeFunctionValues1[1]);
					G_row2.set(0, dof_e1[0]+2, -shapeFunctionValues1[0]);
					G_row2.set(0, dof_e1[1]+2, -shapeFunctionValues1[1]);
				}
				if (connector.getType() == Connector.Type.FIXED) {
					if (e0.isPlaneElement()) {
						for (int n = 0; n < element_nodes0.length; n++) {
							G_row3.set(0, dof_e0[n]+3, shapeFunctionValues0[n]);
							G_row4.set(0, dof_e0[n]+4, shapeFunctionValues0[n]);
							G_row5.set(0, dof_e0[n]+5, shapeFunctionValues0[n]);
						}
					}
					if (e1.isPlaneElement()) {
						for (int n = 0; n < element_nodes1.length; n++) {
							G_row3.set(0, dof_e1[n]+3, -shapeFunctionValues1[n]);
							G_row4.set(0, dof_e1[n]+4, -shapeFunctionValues1[n]);
							G_row5.set(0, dof_e1[n]+5, -shapeFunctionValues1[n]);
						}
					}
					if (e0.getType() == Element.Type.BEAM) {
						G_row3.set(0, dof_e0[0]+3, shapeFunctionValues0[0]);
						G_row3.set(0, dof_e0[1]+3, shapeFunctionValues0[3]);
						G_row4.set(0, dof_e0[0]+4, shapeFunctionValues0[0]);
						G_row4.set(0, dof_e0[1]+4, shapeFunctionValues0[3]);
						G_row5.set(0, dof_e0[0]+5, shapeFunctionValues0[0]);
						G_row5.set(0, dof_e0[1]+5, shapeFunctionValues0[3]);
					}
					if (e1.getType() == Element.Type.BEAM) {
						G_row3.set(0, dof_e1[0]+3, -shapeFunctionValues1[0]);
						G_row3.set(0, dof_e1[1]+3, -shapeFunctionValues1[3]);
						G_row4.set(0, dof_e1[0]+4, -shapeFunctionValues1[0]);
						G_row4.set(0, dof_e1[1]+4, -shapeFunctionValues1[3]);
						G_row5.set(0, dof_e1[0]+5, -shapeFunctionValues1[0]);
						G_row5.set(0, dof_e1[1]+5, -shapeFunctionValues1[3]);
					}
				}
			}
			if (G_row0.normF() != 0.0) {
				G = addRowToMatrix(G, G_row0);
			}
			if (G_row1.normF() != 0.0) {
				G = addRowToMatrix(G, G_row1);
			}
			if (!Model.twoDimensional) {
				if (G_row2.normF() != 0.0) {
					G = addRowToMatrix(G, G_row2);
				}
				if (G_row3.normF() != 0.0) {
					G = addRowToMatrix(G, G_row3);
				}
				if (G_row4.normF() != 0.0) {
					G = addRowToMatrix(G, G_row4);
				}
			}
			if (G_row5.normF() != 0.0) {
				G = addRowToMatrix(G, G_row5);
			}
		}
		return G;
	}
	
	public Matrix getConnectorPartOfgMatrix(int nDofs, Matrix u_global) {
		Matrix g = new Matrix(0, 1);
		ArrayList<Connector> connectors = solution.getRefModel().getConnectors();
		
		/* add coupling for connectors */
		for (int c = 0; c < connectors.size(); c++) {
			g = addRowToMatrix(g, new Matrix(1, 1));
			g = addRowToMatrix(g, new Matrix(1, 1));
			if (!Model.twoDimensional) {
				g = addRowToMatrix(g, new Matrix(1, 1));
			}
			
			if (connectors.get(c).getType() == Connector.Type.REVOLUTE) {
				Element e0 = connectors.get(c).getElement0();
				Element e1 = connectors.get(c).getElement1();
				double[] shapeFunctionValues0 = null;
				double[] shapeFunctionValues1 = null;
				if (e0.isLineElement()) {
					double t = connectors.get(c).getT0();
					shapeFunctionValues0 = ((LineElement) e0).getShapeFunctionValues(t);
				}
				if (e1.isLineElement()) {
					double t = connectors.get(c).getT1();
					shapeFunctionValues1 = ((LineElement) e1).getShapeFunctionValues(t);
				}
				
				Matrix axis = ((Beam) e0).getR0().getMatrix(0, 2, 2, 2);
				
				Matrix u_elem = ((Beam) e0).globalToLocalVector(u_global);
				Matrix R1g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0));
				Matrix R2g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0));
				Matrix a0 = R1g.times(axis).times(shapeFunctionValues0[0]).plus(R2g.times(axis).times(shapeFunctionValues0[3]));
				a0.timesEquals(1.0/a0.normF());
				
				u_elem = ((Beam) e1).globalToLocalVector(u_global);
				R1g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(3, 5, 0, 0));
				R2g = Beam.rotationMatrixFromAngles(u_elem.getMatrix(9, 11, 0, 0));
				Matrix a1 = R1g.times(axis).times(shapeFunctionValues1[0]).plus(R2g.times(axis).times(shapeFunctionValues1[3]));
				a1.timesEquals(1.0/a1.normF());
				double length = ((Beam) e1).getCurrentLength(solution.refModel.getNodes(), u_elem);
				Matrix r1 = ((Beam) e1).getr1(solution.refModel.getNodes(), u_elem, length);
				Matrix c1 = a1.crossProduct(r1);
				Matrix b1 = c1.crossProduct(a1);
				
				if (!Model.twoDimensional) {
					g = addRowToMatrix(g, new Matrix(new double[]{a0.dotProduct(b1)}, 1));
					g = addRowToMatrix(g, new Matrix(new double[]{a0.dotProduct(c1)}, 1));
				}
			}
			
			if (connectors.get(c).getType() == Connector.Type.FIXED) {
				if (!Model.twoDimensional) {
					g = addRowToMatrix(g, new Matrix(1, 1));
					g = addRowToMatrix(g, new Matrix(1, 1));
				}
				g = addRowToMatrix(g, new Matrix(1, 1));
			}
		}
		return g;
	}
	
	public Matrix getContactPartOfGMatrix(int nDofs, Contact[] contacts, Matrix u_global) {
		Matrix G = new Matrix(0, nDofs);
		
		/* add coupling for contacts */
		for (int c = 0; c < contacts.length; c++) {
			if (contacts[c] != null) {
				Element masterElement = contacts[c].getMasterElement();
				double[] shapeFunctionValues = contacts[c].getShapeFunctionValues();
				
				Matrix G_row = new Matrix(1, nDofs);
				int[] element_nodes = masterElement.getElementNodes();
				
				double[] norm = contacts[c].getNorm();
				if (contacts[c].isDeformableDeformable()) {
					for (int i = 0; i < element_nodes.length; i++) {
						int dof = solution.getDofOfNodeID(element_nodes[i]);
						G_row.set(0, dof, shapeFunctionValues[i]*norm[0]);
						G_row.set(0, dof+1, shapeFunctionValues[i]*norm[1]);
						G_row.set(0, dof+2, shapeFunctionValues[i]*norm[2]);
					}
				}
				int dof = solution.getDofOfNodeID(c);
				G_row.set(0, dof, -norm[0]);
				G_row.set(0, dof+1, -norm[1]);
				G_row.set(0, dof+2, -norm[2]);
				
				G = addRowToMatrix(G, G_row);
				
				if (contacts[c].isSticking()) {
					Matrix G_row0 = new Matrix(1, nDofs);
					Matrix G_row1 = new Matrix(1, nDofs);
					
					Matrix norm0 = new Matrix(norm, 3);
					Matrix dir0 = new Matrix(new double[]{1, 0, 0}, 3).crossProduct(norm0);
					if (dir0.normF() < SimLive.ZERO_TOL) {
						dir0 = new Matrix(new double[]{0, 1, 0}, 3).crossProduct(norm0);
					}
					Matrix dir1 = norm0.crossProduct(dir0);
					
					int refDof = solution.getDofOfNodeID(c);
					
					if (contacts[c].isDeformableDeformable()) {	
						for (int i = 0; i < element_nodes.length; i++) {
							dof = solution.getDofOfNodeID(element_nodes[i]);
							G_row0.set(0, dof, shapeFunctionValues[i]*dir0.get(0, 0));
							G_row0.set(0, dof+1, shapeFunctionValues[i]*dir0.get(1, 0));
							G_row0.set(0, dof+2, shapeFunctionValues[i]*dir0.get(2, 0));
							G_row1.set(0, dof, shapeFunctionValues[i]*dir1.get(0, 0));
							G_row1.set(0, dof+1, shapeFunctionValues[i]*dir1.get(1, 0));
							G_row1.set(0, dof+2, shapeFunctionValues[i]*dir1.get(2, 0));
						}
					}
					G_row0.set(0, refDof, -dir0.get(0, 0));
					G_row0.set(0, refDof+1, -dir0.get(1, 0));
					G_row0.set(0, refDof+2, -dir0.get(2, 0));
					G_row1.set(0, refDof, -dir1.get(0, 0));
					G_row1.set(0, refDof+1, -dir1.get(1, 0));
					G_row1.set(0, refDof+2, -dir1.get(2, 0));
					
					if (!Model.twoDimensional) G = addRowToMatrix(G, G_row0);
					G = addRowToMatrix(G, G_row1);
				}
			}
		}
		return G;
	}
	
	public Matrix getSupportPartOfGMatrix(int nDofs) {
		Matrix G = new Matrix(0, nDofs);
		ArrayList<Support> supports = solution.getRefModel().getSupports();
		
		for (int s = 0; s < supports.size(); s++) {
			Support support = supports.get(s);
			Matrix rot = GeomUtility.getRotationMatrix(-support.getAngle()*Math.PI/180.0, support.getAxis().clone());
			for (int n = 0; n < support.getNodes().size(); n++) {
				Node node = support.getNodes().get(n);
				int dof = solution.getDofOfNodeID(node.getID());
				if (support.isFixedDisp()[0]) {
					Matrix G_row = new Matrix(1, nDofs);
					G_row.set(0, dof, rot.get(0, 0));
					G_row.set(0, dof+1, rot.get(0, 1));
					G_row.set(0, dof+2, rot.get(0, 2));
					G = addRowToMatrix(G, G_row);
				}
				if (support.isFixedDisp()[1]) {
					Matrix G_row = new Matrix(1, nDofs);
					G_row.set(0, dof, rot.get(1, 0));
					G_row.set(0, dof+1, rot.get(1, 1));
					G_row.set(0, dof+2, rot.get(1, 2));
					G = addRowToMatrix(G, G_row);
				}
				if (support.isFixedDisp()[2]) {
					Matrix G_row = new Matrix(1, nDofs);
					G_row.set(0, dof, rot.get(2, 0));
					G_row.set(0, dof+1, rot.get(2, 1));
					G_row.set(0, dof+2, rot.get(2, 2));
					G = addRowToMatrix(G, G_row);
				}
				if (support.isFixedRot()[0] && node.isRotationalDOF()) {
					Matrix G_row = new Matrix(1, nDofs);
					G_row.set(0, dof+3, rot.get(0, 0));
					G_row.set(0, dof+4, rot.get(0, 1));
					G_row.set(0, dof+5, rot.get(0, 2));
					G = addRowToMatrix(G, G_row);
				}
				if (support.isFixedRot()[1] && node.isRotationalDOF()) {
					Matrix G_row = new Matrix(1, nDofs);
					G_row.set(0, dof+3, rot.get(1, 0));
					G_row.set(0, dof+4, rot.get(1, 1));
					G_row.set(0, dof+5, rot.get(1, 2));
					G = addRowToMatrix(G, G_row);
				}
				if (support.isFixedRot()[2] && node.isRotationalDOF()) {
					Matrix G_row = new Matrix(1, nDofs);
					G_row.set(0, dof+3, rot.get(2, 0));
					G_row.set(0, dof+4, rot.get(2, 1));
					G_row.set(0, dof+5, rot.get(2, 2));
					G = addRowToMatrix(G, G_row);
				}
			}
		}
		return G;
	}
	
	/*public double getRotationOfReference(ArrayList<Node> referenceNodes, Matrix u_global, double scale) {
		if (referenceNodes.size() > 1) {
			int dof0 = solution.getDofOfNodeID(referenceNodes.get(0).getID());
			int dof1 = solution.getDofOfNodeID(referenceNodes.get(1).getID());
			double[] diff0 = new double[2];
			diff0[0] = referenceNodes.get(1).getXCoord()-referenceNodes.get(0).getXCoord();
			diff0[1] = referenceNodes.get(1).getYCoord()-referenceNodes.get(0).getYCoord();
			double length = Math.sqrt(diff0[0]*diff0[0]+diff0[1]*diff0[1]);
			diff0[0] /= length;
			diff0[1] /= length;				
			double[] diff1 = new double[2];
			diff1[0] = referenceNodes.get(1).getXCoord()+u_global.get(dof1, 0)*scale-referenceNodes.get(0).getXCoord()-u_global.get(dof0, 0)*scale;
			diff1[1] = referenceNodes.get(1).getYCoord()+u_global.get(dof1+1, 0)*scale-referenceNodes.get(0).getYCoord()-u_global.get(dof0+1, 0)*scale;
			length = Math.sqrt(diff1[0]*diff1[0]+diff1[1]*diff1[1]);
			diff1[0] /= length;
			diff1[1] /= length;
			if (diff0[0]*diff1[1]-diff0[1]*diff1[0] > 0.0) {
				return Math.acos(diff0[0]*diff1[0]+diff0[1]*diff1[1]);
			}
			else {
				return -Math.acos(diff0[0]*diff1[0]+diff0[1]*diff1[1]);
			}
		}
		return 0.0;
	}
	
	private double getMove(Node node, Node refNode, Matrix u_global,
			double phi, double cosAngle, double sinAngle, int dof, boolean xPrimeDir) {
		if (refNode != null) {
			int dof0 = solution.getDofOfNodeID(refNode.getID());
			double[] diff = new double[2];
			diff[0] = node.getXCoord()-refNode.getXCoord();
			diff[1] = node.getYCoord()-refNode.getYCoord();
			double cosPhi = Math.cos(phi);
			double sinPhi = Math.sin(phi);			
			double[] uDiff = new double[2];
			uDiff[0] = cosPhi*diff[0]-sinPhi*diff[1]-diff[0];
			uDiff[1] = sinPhi*diff[0]+cosPhi*diff[1]-diff[1];
			double[] uNode = new double[2];
			uNode[0] = u_global.get(dof, 0)-u_global.get(dof0, 0)-uDiff[0];
			uNode[1] = u_global.get(dof+1, 0)-u_global.get(dof0+1, 0)-uDiff[1];
			if (xPrimeDir) {
				return uNode[0]*cosAngle+uNode[1]*sinAngle;
			}
			else {
				return -uNode[0]*sinAngle+uNode[1]*cosAngle;
			}
		}
		if (xPrimeDir) {
			return u_global.get(dof, 0)*cosAngle+u_global.get(dof+1, 0)*sinAngle;
		}
		else {
			return -u_global.get(dof, 0)*sinAngle+u_global.get(dof+1, 0)*cosAngle;
		}
	}*/
	
	public Matrix getLoadPartOfGMatrix(int nDofs, Matrix u_global) {
		Matrix G = new Matrix(0, nDofs);
		ArrayList<Load> loads = solution.getRefModel().getLoads();
		
		for (int l = 0; l < loads.size(); l++) {
			Load load = loads.get(l);
			
			if (load.getType() == Load.Type.DISPLACEMENT &&
				load.getTimeTable().isFactorDefinedAtTime(time)) {
				Matrix TT = GeomUtility.getRotationMatrix(-load.getAngle()*Math.PI/180.0, load.getAxis().clone());
				Matrix TTRgT = TT.copy();
				int refDof = -1;
				Node refNode = null;
				if (load.referenceNode != null) {
					refNode = load.referenceNode;
					refDof = solution.getDofOfNodeID(refNode.getID());
					if (refNode.isRotationalDOF()) {
						Matrix Rg = Beam.rotationMatrixFromAngles(u_global.getMatrix(refDof+3, refDof+5, 0, 0));
						TTRgT = TT.times(Rg.transpose());
					}
				}
				
				for (int n = 0; n < load.getNodes().size(); n++) {
					Node node = load.getNodes().get(n);
					int dof = solution.getDofOfNodeID(node.getID());
					
					if (refNode != null) {
						Matrix r1 = null;
						if (refNode.isRotationalDOF()) {
							r1 = new Matrix(node.getCoords(), 3).plus(u_global.getMatrix(dof, dof+2, 0, 0)).minus(
									new Matrix(refNode.getCoords(), 3).plus(u_global.getMatrix(refDof, refDof+2, 0, 0)));
							r1 = TTRgT.times(r1);
						}
						if (load.isDisp()[0]) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof, TTRgT.get(0, 0));
							G_row.set(0, dof+1, TTRgT.get(0, 1));
							G_row.set(0, dof+2, TTRgT.get(0, 2));
							G_row.set(0, refDof, -TTRgT.get(0, 0));
							G_row.set(0, refDof+1, -TTRgT.get(0, 1));
							G_row.set(0, refDof+2, -TTRgT.get(0, 2));
							if (refNode.isRotationalDOF()) {
								G_row.set(0, refDof+4, -r1.get(2, 0));
								G_row.set(0, refDof+5, r1.get(1, 0));
							}
							G = addRowToMatrix(G, G_row);
						}
						if (load.isDisp()[1]) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof, TTRgT.get(1, 0));
							G_row.set(0, dof+1, TTRgT.get(1, 1));
							G_row.set(0, dof+2, TTRgT.get(1, 2));
							G_row.set(0, refDof, -TTRgT.get(1, 0));
							G_row.set(0, refDof+1, -TTRgT.get(1, 1));
							G_row.set(0, refDof+2, -TTRgT.get(1, 2));
							if (refNode.isRotationalDOF()) {
								G_row.set(0, refDof+3, r1.get(2, 0));
								G_row.set(0, refDof+5, -r1.get(0, 0));
							}
							G = addRowToMatrix(G, G_row);
						}
						if (load.isDisp()[2]) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof, TTRgT.get(2, 0));
							G_row.set(0, dof+1, TTRgT.get(2, 1));
							G_row.set(0, dof+2, TTRgT.get(2, 2));
							G_row.set(0, refDof, -TTRgT.get(2, 0));
							G_row.set(0, refDof+1, -TTRgT.get(2, 1));
							G_row.set(0, refDof+2, -TTRgT.get(2, 2));
							if (refNode.isRotationalDOF()) {
								G_row.set(0, refDof+3, -r1.get(1, 0));
								G_row.set(0, refDof+4, r1.get(0, 0));
							}
							G = addRowToMatrix(G, G_row);
						}
						
						if (node.isRotationalDOF()) {
							if (load.isRotation()[0]) {
								Matrix G_row = new Matrix(1, nDofs);
								G_row.set(0, dof+3, TT.get(0, 0));
								G_row.set(0, dof+4, TT.get(0, 1));
								G_row.set(0, dof+5, TT.get(0, 2));
								if (refNode.isRotationalDOF()) {
									G_row.set(0, refDof+3, -TT.get(0, 0));
									G_row.set(0, refDof+4, -TT.get(0, 1));
									G_row.set(0, refDof+5, -TT.get(0, 2));
								}
								G = addRowToMatrix(G, G_row);
							}
							if (load.isRotation()[1]) {
								Matrix G_row = new Matrix(1, nDofs);
								G_row.set(0, dof+3, TT.get(1, 0));
								G_row.set(0, dof+4, TT.get(1, 1));
								G_row.set(0, dof+5, TT.get(1, 2));
								if (refNode.isRotationalDOF()) {
									G_row.set(0, refDof+3, -TT.get(1, 0));
									G_row.set(0, refDof+4, -TT.get(1, 1));
									G_row.set(0, refDof+5, -TT.get(1, 2));
								}
								G = addRowToMatrix(G, G_row);
							}
							if (load.isRotation()[2]) {
								Matrix G_row = new Matrix(1, nDofs);
								G_row.set(0, dof+3, TT.get(2, 0));
								G_row.set(0, dof+4, TT.get(2, 1));
								G_row.set(0, dof+5, TT.get(2, 2));
								if (refNode.isRotationalDOF()) {
									G_row.set(0, refDof+3, -TT.get(2, 0));
									G_row.set(0, refDof+4, -TT.get(2, 1));
									G_row.set(0, refDof+5, -TT.get(2, 2));
								}
								G = addRowToMatrix(G, G_row);
							}
						}
					}
					
					else {
						if (load.isDisp()[0]) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof, TT.get(0, 0));
							G_row.set(0, dof+1, TT.get(0, 1));
							G_row.set(0, dof+2, TT.get(0, 2));
							G = addRowToMatrix(G, G_row);						
						}
						if (load.isDisp()[1]) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof, TT.get(1, 0));
							G_row.set(0, dof+1, TT.get(1, 1));
							G_row.set(0, dof+2, TT.get(1, 2));
							G = addRowToMatrix(G, G_row);
						}
						if (load.isDisp()[2]) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof, TT.get(2, 0));
							G_row.set(0, dof+1, TT.get(2, 1));
							G_row.set(0, dof+2, TT.get(2, 2));
							G = addRowToMatrix(G, G_row);
						}
						if (load.isRotation()[0] && node.isRotationalDOF()) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof+3, TT.get(0, 0));
							G_row.set(0, dof+4, TT.get(0, 1));
							G_row.set(0, dof+5, TT.get(0, 2));
							G = addRowToMatrix(G, G_row);
						}
						if (load.isRotation()[1] && node.isRotationalDOF()) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof+3, TT.get(1, 0));
							G_row.set(0, dof+4, TT.get(1, 1));
							G_row.set(0, dof+5, TT.get(1, 2));
							G = addRowToMatrix(G, G_row);
						}
						if (load.isRotation()[2] && node.isRotationalDOF()) {
							Matrix G_row = new Matrix(1, nDofs);
							G_row.set(0, dof+3, TT.get(2, 0));
							G_row.set(0, dof+4, TT.get(2, 1));
							G_row.set(0, dof+5, TT.get(2, 2));
							G = addRowToMatrix(G, G_row);
						}
					}
				}
			}
		}
		return G;
	}
	
	public Matrix getLoadPartOfgMatrix(int nDofs, Matrix u_global) {
		final double tol_factor = 0.999;
		Matrix g = new Matrix(0, 1);
		ArrayList<Load> loads = solution.getRefModel().getLoads();
		
		for (int l = 0; l < loads.size(); l++) {
			Load load = loads.get(l);
			
			if (load.getType() == Load.Type.DISPLACEMENT &&
				load.getTimeTable().isFactorDefinedAtTime(time)) {
				Matrix TT = GeomUtility.getRotationMatrix(-load.getAngle()*Math.PI/180.0, load.getAxis().clone());
				
				Node refNode = null;
				int refDof = -1;
				Matrix Rg = null;
				if (load.referenceNode != null) {
					refNode = load.referenceNode;
					refDof = solution.getDofOfNodeID(refNode.getID());
					if (refNode.isRotationalDOF()) {
						Rg = Beam.rotationMatrixFromAngles(u_global.getMatrix(refDof+3, refDof+5, 0, 0));
					}
				}
				
				for (int n = 0; n < load.getNodes().size(); n++) {
					Node node = load.getNodes().get(n);
					int dof = solution.getDofOfNodeID(node.getID());
					
					if (refNode != null) {
						Matrix move = null;
						if (refNode.isRotationalDOF()) {
							move = new Matrix(node.getCoords(), 3).plus(u_global.getMatrix(dof, dof+2, 0, 0)).minus(
									new Matrix(refNode.getCoords(), 3).plus(u_global.getMatrix(refDof, refDof+2, 0, 0)));
							Matrix move0 = new Matrix(node.getCoords(), 3).minus(new Matrix(refNode.getCoords(), 3));
							move = TT.times(Rg.transpose().times(move).minus(move0));
						}
						else {
							move = TT.times((u_global.getMatrix(dof, dof+2, 0, 0)).minus(u_global.getMatrix(refDof, refDof+2, 0, 0)));
						}
										
						if (load.isDisp()[0]) {
							double xValue = load.getDisp(time)[0]-move.get(0, 0);
							Matrix g_row = new Matrix(1, 1);
							g_row.set(0, 0, xValue);
							g = addRowToMatrix(g, g_row);
						}
						if (load.isDisp()[1]) {
							double yValue = load.getDisp(time)[1]-move.get(1, 0);
							Matrix g_row = new Matrix(1, 1);
							g_row.set(0, 0, yValue);
							g = addRowToMatrix(g, g_row);
						}
						if (load.isDisp()[2]) {
							double zValue = load.getDisp(time)[2]-move.get(2, 0);
							Matrix g_row = new Matrix(1, 1);
							g_row.set(0, 0, zValue);
							g = addRowToMatrix(g, g_row);
						}
						if (node.isRotationalDOF()) {
							Matrix deltaAngles = new Matrix(3, 1);
							Matrix rotate = u_global.getMatrix(dof+3, dof+5, 0, 0);
							Matrix rotation = new Matrix(3, 1);
							rotation.set(0, 0, load.getRotation(time)[0]*Math.PI/180.0);
							rotation.set(1, 0, load.getRotation(time)[1]*Math.PI/180.0);
							rotation.set(2, 0, load.getRotation(time)[2]*Math.PI/180.0);
							Matrix R = Beam.rotationMatrixFromAngles(rotation);
							Matrix rotateR = Beam.rotationMatrixFromAngles(TT.times(rotate));
							if (refNode.isRotationalDOF()) {
								Matrix refRotate = u_global.getMatrix(refDof+3, refDof+5, 0, 0);
								Matrix refR = Beam.rotationMatrixFromAngles(TT.times(refRotate));
								deltaAngles = new Matrix(Beam.anglesFromRotationMatrix(refR.times(R).times(rotateR.transpose())), 3);
								deltaAngles.timesEquals(tol_factor);
							}
							else {
								deltaAngles = new Matrix(Beam.anglesFromRotationMatrix(R.times(rotateR.transpose())), 3);
								deltaAngles.timesEquals(tol_factor);
							}
							
							if (load.isRotation()[0]) {
								Matrix g_row = new Matrix(1, 1);
								g_row.set(0, 0, deltaAngles.get(0, 0));
								g = addRowToMatrix(g, g_row);
							}
							if (load.isRotation()[1]) {
								Matrix g_row = new Matrix(1, 1);
								g_row.set(0, 0, deltaAngles.get(1, 0));
								g = addRowToMatrix(g, g_row);
							}
							if (load.isRotation()[2]) {
								Matrix g_row = new Matrix(1, 1);
								g_row.set(0, 0, deltaAngles.get(2, 0));
								g = addRowToMatrix(g, g_row);
							}
						}
					}
					
					else {
						Matrix move = TT.times(u_global.getMatrix(dof, dof+2, 0, 0));
						
						if (load.isDisp()[0]) {
							double xValue = load.getDisp(time)[0]-move.get(0, 0);
							Matrix g_row = new Matrix(1, 1);
							g_row.set(0, 0, xValue);
							g = addRowToMatrix(g, g_row);
						}
						if (load.isDisp()[1]) {
							double yValue = load.getDisp(time)[1]-move.get(1, 0);
							Matrix g_row = new Matrix(1, 1);
							g_row.set(0, 0, yValue);
							g = addRowToMatrix(g, g_row);
						}
						if (load.isDisp()[2]) {
							double zValue = load.getDisp(time)[2]-move.get(2, 0);
							Matrix g_row = new Matrix(1, 1);
							g_row.set(0, 0, zValue);
							g = addRowToMatrix(g, g_row);
						}
						
						if (node.isRotationalDOF()) {
							Matrix rotate = u_global.getMatrix(dof+3, dof+5, 0, 0);
							Matrix rotation = new Matrix(3, 1);
							rotation.set(0, 0, load.getRotation(time)[0]*Math.PI/180.0);
							rotation.set(1, 0, load.getRotation(time)[1]*Math.PI/180.0);
							rotation.set(2, 0, load.getRotation(time)[2]*Math.PI/180.0);
							Matrix R = Beam.rotationMatrixFromAngles(rotation);
							Matrix rotateR = Beam.rotationMatrixFromAngles(TT.times(rotate));
							Matrix deltaAngles = new Matrix(Beam.anglesFromRotationMatrix(R.times(rotateR.transpose())), 3);
							deltaAngles.timesEquals(tol_factor);
							
							if (load.isRotation()[0]) {
								Matrix g_row = new Matrix(1, 1);
								g_row.set(0, 0, deltaAngles.get(0, 0));
								g = addRowToMatrix(g, g_row);
							}
							if (load.isRotation()[1]) {
								Matrix g_row = new Matrix(1, 1);
								g_row.set(0, 0, deltaAngles.get(1, 0));
								g = addRowToMatrix(g, g_row);
							}
							if (load.isRotation()[2]) {
								Matrix g_row = new Matrix(1, 1);
								g_row.set(0, 0, deltaAngles.get(2, 0));
								g = addRowToMatrix(g, g_row);
							}
						}
					}
				}
			}
		}
		return g;
		
	}
	
	public Matrix getContactPartOfgMatrix(int nDofs, Contact[] contacts) {
		Matrix g = new Matrix(0, 1);
		
		for (int c = 0; c < contacts.length; c++) {
			if (contacts[c] != null) {
				Matrix g_row = new Matrix(1, 1);
				double penetration = contacts[c].getPenetration();
				g_row.set(0, 0, penetration);
				g = addRowToMatrix(g, g_row);
				if (contacts[c].isSticking()) {
					g_row = new Matrix(1, 1);
					if (!Model.twoDimensional) g = addRowToMatrix(g, g_row);
					g = addRowToMatrix(g, g_row);
				}
			}
		}
		return g;
	}
	
	public Matrix getAssembledGMatrix(int nDofs, Matrix G_support, Matrix G_connect, Matrix G_contact, Matrix G_load) {
		Matrix G = new Matrix(G_support.getRowDimension() + G_connect.getRowDimension() +
				G_contact.getRowDimension() + G_load.getRowDimension(), nDofs);
		G.setMatrix(0, G_support.getRowDimension()-1, 0, nDofs-1, G_support);
		G.setMatrix(G_support.getRowDimension(),
				G_support.getRowDimension()+G_connect.getRowDimension()-1,
				0, nDofs-1, G_connect);
		G.setMatrix(G_support.getRowDimension()+G_connect.getRowDimension(),
				G_support.getRowDimension()+G_connect.getRowDimension()+G_contact.getRowDimension()-1,
				0, nDofs-1, G_contact);
		G.setMatrix(G_support.getRowDimension()+G_connect.getRowDimension()+G_contact.getRowDimension(),
				G_support.getRowDimension()+G_connect.getRowDimension()+G_contact.getRowDimension()+G_load.getRowDimension()-1,
				0, nDofs-1, G_load);
		return G;
	}
	
	public Matrix getAssembledgMatrix(Matrix G, Matrix g_connect, Matrix g_contact, Matrix g_load) {
		Matrix g = new Matrix(G.getRowDimension(), 1);
		if (g.getRowDimension() >= g_load.getRowDimension()+g_contact.getRowDimension()+g_connect.getRowDimension()) {
			g.setMatrix(g.getRowDimension()-g_load.getRowDimension()-g_contact.getRowDimension()-g_connect.getRowDimension(),
					g.getRowDimension()-1-g_load.getRowDimension()-g_contact.getRowDimension(), 0, 0, g_connect);
			g.setMatrix(g.getRowDimension()-g_load.getRowDimension()-g_contact.getRowDimension(),
					g.getRowDimension()-1-g_load.getRowDimension(), 0, 0, g_contact);
			g.setMatrix(g.getRowDimension()-g_load.getRowDimension(),
					g.getRowDimension()-1, 0, 0, g_load);
		}
		return g;
	}
	
	public Matrix getGravityForce(int nDofs, Step step, Matrix M_global) {
		Matrix f_gravity = new Matrix(nDofs, 1);
		if (step.gravity != GRAVITY.NO_GRAVITY) {
			if (M_global == null) {
				M_global = assembleMassSequential(nDofs);
			}
			int shift = step.gravity.ordinal()-1;
			for (int n = 0; n < solution.getRefModel().getNodes().size(); n++) {
				Node node = solution.getRefModel().getNodes().get(n);
				int dof = solution.getDofOfNodeID(node.getID());
				double mass = 0;
				for (int n1 = 0; n1 < solution.getRefModel().getNodes().size(); n1++) {
					Node node1 = solution.getRefModel().getNodes().get(n1);
					int dof1 = solution.getDofOfNodeID(node1.getID());
					mass += M_global.get(dof1+shift, dof+shift);
				}
				f_gravity.set(dof+shift, 0, mass*step.gValue);
			}
		}
		return f_gravity;
	}
	
	public Matrix getExternalForce(int nDofs, Step step, Matrix f_gravity, Matrix u_global) {
		Matrix f_ext = f_gravity.copy();
		ArrayList<Load> loads = solution.getRefModel().getLoads();
		ArrayList<DistributedLoad> distributedLoads = solution.getRefModel().getDistributedLoads();
		
		for (int l = 0; l < loads.size(); l++) {
			Load load = loads.get(l);
			if (load.getType() == Load.Type.FORCE &&
				load.getTimeTable().isFactorDefinedAtTime(time)) {
				Matrix rot = GeomUtility.getRotationMatrix(load.getAngle()*Math.PI/180.0, load.getAxis().clone());
				
				if (load.referenceNode != null && load.referenceNode.isRotationalDOF()) {
					int dof = solution.getDofOfNodeID(load.referenceNode.getID());
					Matrix R1g = Beam.rotationMatrixFromAngles(u_global.getMatrix(dof+3, dof+5, 0, 0));
					rot = rot.times(R1g);
				}
				
				for (int n = 0; n < load.getNodes().size(); n++) {
					Matrix force = new Matrix(load.getForce(time), 3);
					force = rot.times(force);
					Matrix moment = new Matrix(load.getMoment(time), 3);
					moment = rot.times(moment);
					Node node = load.getNodes().get(n);
					int dof = solution.getDofOfNodeID(node.getID());
					
					f_ext.set(dof, 0, f_ext.get(dof, 0) + force.get(0, 0));
					f_ext.set(dof+1, 0, f_ext.get(dof+1, 0) + force.get(1, 0));
					f_ext.set(dof+2, 0, f_ext.get(dof+2, 0) + force.get(2, 0));
					if (node.isRotationalDOF()) {						
						f_ext.set(dof+3, 0, f_ext.get(dof+3, 0) + moment.get(0, 0));
						f_ext.set(dof+4, 0, f_ext.get(dof+4, 0) + moment.get(1, 0));
						f_ext.set(dof+5, 0, f_ext.get(dof+5, 0) + moment.get(2, 0));
					}
				}
			}
		}
		
		for (int d = 0; d < distributedLoads.size(); d++) {
			DistributedLoad load = distributedLoads.get(d);
			if (load.getTimeTable().isFactorDefinedAtTime(time)) {
				double xDirStartValue = load.getStartValue(0, time);
				double xDirEndValue = load.getEndValue(0, time);
				double yDirStartValue = load.getStartValue(1, time);
				double yDirEndValue = load.getEndValue(1, time);		
				double zDirStartValue = load.getStartValue(2, time);
				double zDirEndValue = load.getEndValue(2, time);		
				//double phi = getRotationOfReference(load.getReferenceNodes(), u_global, 1.0);
				
				for (int s = 0; s < load.getElementSets().size(); s++) {
					Set set = load.getElementSets().get(s);
					//double cosAngle = 0.0, sinAngle = 0.0;
					Beam beam = (Beam) set.getElements().get(0);
					double length = beam.getLength();
					Matrix R = GeomUtility.getRotationMatrix(load.getAngle()*Math.PI/180.0, load.getAxis().clone());					
					if (load.isLocalSysAligned()) {
						R = beam.getR0();
					}
					if (load.referenceNode != null && load.referenceNode.isRotationalDOF()) {
						int dof = solution.getDofOfNodeID(load.referenceNode.getID());
						Matrix R1g = Beam.rotationMatrixFromAngles(u_global.getMatrix(dof+3, dof+5, 0, 0));
						R = R.times(R1g);
					}
					
					for (int i = 0; i < set.getElements().size(); i++) {
						beam = (Beam) set.getElements().get(i);
						
						double[] comp0 = new double[3];
						double pos = i/(double) (set.getElements().size());
						comp0[0] = xDirStartValue+pos*(xDirEndValue-xDirStartValue);
						comp0[1] = yDirStartValue+pos*(yDirEndValue-yDirStartValue);
						comp0[2] = zDirStartValue+pos*(zDirEndValue-zDirStartValue);
						double[] comp1 = new double[3];
						pos = (i+1)/(double) (set.getElements().size());
						comp1[0] = xDirStartValue+pos*(xDirEndValue-xDirStartValue);
						comp1[1] = yDirStartValue+pos*(yDirEndValue-yDirStartValue);
						comp1[2] = zDirStartValue+pos*(zDirEndValue-zDirStartValue);
						comp0 = R.times(new Matrix(comp0, 3)).getColumnPackedCopy();
						comp1 = R.times(new Matrix(comp1, 3)).getColumnPackedCopy();
						double[][] force = new double[2][3];
						force[0][0] = comp0[0]*length/2.0;
						force[0][1] = comp0[1]*length/2.0;
						force[0][2] = comp0[2]*length/2.0;
						force[1][0] = comp1[0]*length/2.0;
						force[1][1] = comp1[1]*length/2.0;
						force[1][2] = comp1[2]*length/2.0;
						
						int[] elemNodes = beam.getElementNodes();
						int dof = solution.getDofOfNodeID(elemNodes[0]);
						f_ext.set(dof, 0, f_ext.get(dof, 0) + force[0][0]);
						f_ext.set(dof+1, 0, f_ext.get(dof+1, 0) + force[0][1]);
						f_ext.set(dof+2, 0, f_ext.get(dof+2, 0) + force[0][2]);
						dof = solution.getDofOfNodeID(elemNodes[1]);
						f_ext.set(dof, 0, f_ext.get(dof, 0) + force[1][0]);
						f_ext.set(dof+1, 0, f_ext.get(dof+1, 0) + force[1][1]);
						f_ext.set(dof+2, 0, f_ext.get(dof+2, 0) + force[1][2]);
					}
				}
			}
		}
		
		return f_ext;
	}
	
	public Matrix updateSolution(Matrix u_global, Matrix delta_u_global) {
		Matrix u_global_new = u_global.plus(delta_u_global);
		//intrinsic update of rotations
		if (solution.getRefSettings().isLargeDisplacement) {
			ArrayList<Node> nodes = solution.getRefModel().getNodes();
			for (int n = 0; n < nodes.size(); n++) {
				if (nodes.get(n).isRotationalDOF()) {
					int dof = solution.getDofOfNodeID(n);
					Matrix rotVecOld = u_global.getMatrix(dof+3, dof+5, 0, 0);
					Matrix delta_rotVec = delta_u_global.getMatrix(dof+3, dof+5, 0, 0);
					Matrix ROld = Beam.rotationMatrixFromAngles(rotVecOld);
					Matrix deltaR = Beam.rotationMatrixFromAngles(delta_rotVec);
					Matrix rotVecNew = new Matrix(Beam.anglesFromRotationMatrix(deltaR.times(ROld)), 3);
					u_global_new.setMatrix(dof+3, dof+5, 0, 0, rotVecNew);
				}
			}
		}
		return u_global_new;
	}
	
	public void setResults(Matrix u_global, Matrix v_global, Matrix a_global, Matrix r_global) {
		this.u_global = u_global;
		this.v_global = v_global;
		this.a_global = a_global;
		this.r_global = r_global;
		this.Rr_beam = new Matrix[solution.getRefModel().getElements().size()];
		this.angles = new double[solution.getRefModel().getElements().size()][][];
		this.angularVel = new double[solution.getRefModel().getElements().size()][][];
		this.angularAcc = new double[solution.getRefModel().getElements().size()][][];
		for (int e = 0; e < solution.getRefModel().getElements().size(); e++) {
			if (solution.getRefModel().getElements().get(e).getType() == Element.Type.BEAM) {
				Beam beam = (Beam) solution.getRefModel().getElements().get(e);
				Matrix u_elem = beam.globalToLocalVector(u_global);
				double length = beam.getCurrentLength(SimLive.model.getNodes(), u_elem);
				Matrix r1 = beam.getr1(SimLive.model.getNodes(), u_elem, length);
				this.Rr_beam[e] = beam.getRr(u_elem, r1);
				this.angles[e] = beam.getAngularValuesInCoRotatedFrame(this, this.Rr_beam[e], 0);
				this.angularVel[e] = beam.getAngularValuesInCoRotatedFrame(this, this.Rr_beam[e], 1);
				this.angularAcc[e] = beam.getAngularValuesInCoRotatedFrame(this, this.Rr_beam[e], 2);
			}
		}
	}
	
	public void setResultsForMatrixView(Matrix[] K_elem, Matrix[] M_elem,
			Matrix M_global, Matrix K_global, Matrix f_ext, Matrix f_int,
			Matrix G, Matrix K_constr, Matrix M_constr, Matrix delta_f_constr) {
		this.M_elem = M_elem;
		this.K_elem = K_elem;
		this.M_global = M_global;
		this.K_global = K_global;
		this.f_ext = f_ext;
		this.f_int = f_int;
		this.G = G;
		this.K_constr = K_constr;
		this.M_constr = M_constr;
		this.delta_f_constr = delta_f_constr;
	}
	
	public int getNumberOfDynamicTimeSteps(int nDofs, Matrix u_global, Step step) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		double minCritTimeStep = Double.MAX_VALUE;
		for (int elem = 0; elem < elements.size(); elem++) {
			if (elements.get(elem).getType() != Element.Type.SPRING) {
				Matrix M_elem = elements.get(elem).M_elem;
				Matrix K_elem = null;
				if (solution.getRefSettings().isLargeDisplacement) {
					K_elem = elements.get(elem).getElementStiffnessNL(nodes, u_global);
				}
				else {
					K_elem = elements.get(elem).getElementStiffness(nodes);
				}
			
				try {
					EigenvalueDecomposition eig = M_elem.inverse().times(K_elem).eig(false);
					double[] eigenValues = eig.getRealEigenvalues();		
					Arrays.sort(eigenValues);
					double omega = Math.sqrt(eigenValues[eigenValues.length-1]);
					if (elements.get(elem).getStiffnessDamping() > SimLive.string2Double(SimLive.double2String(2.0/omega))) {
						Solution.warnings.add("Stiffness damping > "+SimLive.double2String(2.0/omega)+" for element \""+
								elements.get(elem).getTypeString()+" "+Integer.toString(elem+1)+"\".");
					}
					double xi = elements.get(elem).getMassDamping()/(2.0*omega)+elements.get(elem).getStiffnessDamping()*omega/2.0;
					double critTimeStep = 2.0/omega*(Math.sqrt(xi*xi+1.0)-xi);
					if (critTimeStep < minCritTimeStep) {
						minCritTimeStep = critTimeStep;
					}
				}
				catch (RuntimeException e) {
					return -1;
				}
			}
		}
		
		double deltaPostTime = step.duration/step.nIncrements;
		int stepsPerPostInc = (int) (deltaPostTime/minCritTimeStep) + 1;
		return stepsPerPostInc*step.nIncrements;
	}
	
	public void initTree(Tree tree, int incrementID) {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		
		TreeItem root, item, subItem, subSubItem;
		
		root = new TreeItem(tree, SWT.NONE);
		updateTree(tree, incrementID);
		
		item = new TreeItem(root, SWT.NONE);
		item.setText("Element Matrices");
		for (int elem = 0; elem < elements.size(); elem++) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("Element "+(elem+1));
			subSubItem = new TreeItem(subItem, SWT.NONE);
			subSubItem.setText("K_elem");
			subSubItem.setData(new int[] {0, elem, 0});
			subSubItem = new TreeItem(subItem, SWT.NONE);
			subSubItem.setText("K_elem \u2192 K_global");
			subSubItem.setData(new int[] {0, elem, 1});
			if (solution.refModel.doStepsContainType(Step.Type.EXPLICIT_DYNAMIC) ||
				solution.refModel.doStepsContainType(Step.Type.MODAL_ANALYSIS)) {
				subSubItem = new TreeItem(subItem, SWT.NONE);
				subSubItem.setText("M_elem");
				subSubItem.setData(new int[] {0, elem, 2});
				subSubItem = new TreeItem(subItem, SWT.NONE);
				subSubItem.setText("M_elem \u2192 M_global");
				subSubItem.setData(new int[] {0, elem, 3});
			}
		}
		
		item = new TreeItem(root, SWT.NONE);
		item.setText("Global System");
		subItem = new TreeItem(item, SWT.NONE);
		subItem.setText("K_global");
		subItem.setData(new int[] {1, 0, 0});
		if (solution.refModel.doStepsContainType(Step.Type.EXPLICIT_DYNAMIC) ||
			solution.refModel.doStepsContainType(Step.Type.MODAL_ANALYSIS)) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("M_global");
			subItem.setData(new int[] {1, 0, 1});
		}
		if (solution.refModel.doStepsContainType(Step.Type.MECHANICAL_STATIC) ||
			solution.refModel.doStepsContainType(Step.Type.EXPLICIT_DYNAMIC)) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("f_ext");
			subItem.setData(new int[] {1, 0, 2});
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("f_int");
			subItem.setData(new int[] {1, 0, 3});
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("\u0394f");
			subItem.setData(new int[] {1, 0, 4});
		}
		
		item = new TreeItem(root, SWT.NONE);
		item.setText("Constrained System");
		if (solution.refModel.doStepsContainType(Step.Type.MECHANICAL_STATIC) ||
			solution.refModel.doStepsContainType(Step.Type.MODAL_ANALYSIS)) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("K_constr");
			subItem.setData(new int[] {2, 0, 0});
		}
		if (solution.refModel.doStepsContainType(Step.Type.EXPLICIT_DYNAMIC) ||
			solution.refModel.doStepsContainType(Step.Type.MODAL_ANALYSIS)) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("M_constr");
			subItem.setData(new int[] {2, 0, 1});
		}
		if (solution.refModel.doStepsContainType(Step.Type.MECHANICAL_STATIC) ||
			solution.refModel.doStepsContainType(Step.Type.EXPLICIT_DYNAMIC)) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("\u0394f_constr");
			subItem.setData(new int[] {2, 0, 2});
		}
		
		item = new TreeItem(root, SWT.NONE);
		item.setText("Solution");
		if (solution.refModel.doStepsContainType(Step.Type.EXPLICIT_DYNAMIC) ||
			solution.refModel.doStepsContainType(Step.Type.MODAL_ANALYSIS)) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("a_global");
			subItem.setData(new int[] {3, 0, 0});
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("v_global");
			subItem.setData(new int[] {3, 0, 1});
		}
		subItem = new TreeItem(item, SWT.NONE);
		subItem.setText("u_global");
		subItem.setData(new int[] {3, 0, 2});
		if (solution.refModel.doStepsContainType(Step.Type.MECHANICAL_STATIC) ||
			solution.refModel.doStepsContainType(Step.Type.EXPLICIT_DYNAMIC)) {
			subItem = new TreeItem(item, SWT.NONE);
			subItem.setText("r_global");
			subItem.setData(new int[] {3, 0, 3});
		}
	}
	
	public void updateTree(Tree tree, int incrementID) {
		TreeItem root = tree.getItem(0);
		root.setText("Time Increment "+incrementID+"/"+
				solution.getNumberOfIncrements());
	}
	
	public void updateTable(Table table, Tree tree) {		
		table.setRedraw(false);
		String[][] dofNames = new String[1][];
		Matrix matrix = getMatrixFromTreeSelection(tree, dofNames);
		if (matrix != null) {
			if (table.getItemCount() < matrix.getRowDimension()+1 || table.getColumnCount() < matrix.getColumnDimension()+2) {
				for (int r = table.getItemCount(); r < matrix.getRowDimension()+1; r++) {
					new TableItem(table, SWT.NONE);
				}
				for (int c = table.getColumnCount(); c < matrix.getColumnDimension()+2; c++) {
					new TableColumn(table, SWT.NONE);
					table.getColumn(c).setAlignment(SWT.RIGHT);
				}
				table.getColumn(0).setWidth(0);
			}
			else {
				table.clearAll();
			}
				
			TableItem item = table.getItem(0);
			String[] str = new String[matrix.getColumnDimension()+2];
			for (int c = 0; c < matrix.getColumnDimension(); c++) {
				if (matrix.getColumnDimension() > 1) {
					str[c+2] = dofNames[0][c];
				}
				item.setBackground(c+2, SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
			}
			item.setText(str);
			
			for (int r = 0; r < matrix.getRowDimension(); r++) {
				item = table.getItem(r+1);
				str = new String[matrix.getColumnDimension()+2];
				str[1] = dofNames[0][r];
				item.setBackground(1, SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
				for (int c = 0; c < matrix.getColumnDimension(); c++) {
					double value = matrix.get(r, c);
					if (SimLive.toggleNonZeroEntries) {
						if (value != 0.0) {
							item.setBackground(c+2, SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION));
						}
					}
					else {
						str[c+2] = value == 0.0 ? "0" : SimLive.double2String(value);
					}
				}
				item.setText(str);
			}
			
			packTable(table);
		}
		table.setRedraw(true);
	}
	
	public void packTable(Table table) {
		table.getColumn(1).pack();
		int widthHint = -1;
		{
			GC gc = new GC(table);
			char[] chars = new char[SimLive.OUTPUT_DIGITS];
			Arrays.fill(chars, '0');
			widthHint = gc.stringExtent("-,E-00XX"+new String(chars)).x;
			gc.dispose();
		}	
		for (int c = 2; c < table.getColumnCount(); c++) {
			table.getColumn(c).setWidth(widthHint);
		}
	}

	private Matrix getMatrixFromTreeSelection(Tree tree, String[][] dofNames) {
		ArrayList<Element> elements = solution.getRefModel().getElements();
		int nDofs = solution.getNumberOfDofs();
		String[] globalDofNames = solution.getConstraintMethod().getGlobalDofNames(new Matrix(0, nDofs), solution.getRefModel());
		String[] constrDofNames = solution.getConstraintMethod().getGlobalDofNames(G, solution.getRefModel());
		
		if (tree.getSelectionCount() > 0 && tree.getSelection()[0].getItemCount() == 0) {
			TreeItem selectedItem = tree.getSelection()[0]; 
			int[] data = (int[]) selectedItem.getData();
			switch (data[0]) {
				
				case 0: int elem = data[1];
						
						switch (data[2]) {
							case 0: if (K_elem == null) return null;
									dofNames[0] = elements.get(elem).getLocalDofNames();
									return K_elem[elem];
							case 1: if (K_elem == null) return null;
									dofNames[0] = globalDofNames;
									return elements.get(elem).addLocalToGlobalMatrix(K_elem[elem],
									new Matrix(nDofs, nDofs));
							case 2: if (M_elem == null) return null;
									dofNames[0] = elements.get(elem).getLocalDofNames();
									return M_elem[elem];
							case 3: if (M_elem == null) return null;
									dofNames[0] = globalDofNames;
									return elements.get(elem).addLocalToGlobalMatrix(M_elem[elem],
									new Matrix(nDofs, nDofs));
						}
				
				case 1: dofNames[0] = globalDofNames;						
						switch (data[2]) {
							case 0: return K_global;
							case 1: return M_global;
							case 2: return f_ext;
							case 3: return f_int;
							case 4: return f_ext.minus(f_int);
						}
				
				case 2: dofNames[0] = constrDofNames;						
						switch (data[2]) {
							case 0: return K_constr;
							case 1: return M_constr;
							case 2: return delta_f_constr;
						}
				
				case 3: dofNames[0] = globalDofNames;
						switch (data[2]) {
							case 0: return a_global;
							case 1: return v_global;
							case 2: return u_global;
							case 3: return r_global;
						}
			}
		}
		return null;
	}
	
	public double getTime() {
		return time;
	}
	
	public int getStepNr() {
		return stepNr;
	}
	
	public Matrix[] get_M_elem() {
		return M_elem;
	}

	public Matrix[] get_K_elem() {
		return K_elem;
	}

	public Matrix get_M_global() {
		return M_global;
	}

	public Matrix get_K_global() {
		return K_global;
	}

	public Matrix get_f_ext() {
		return f_ext;
	}

	public Matrix get_f_int() {
		return f_int;
	}

	public Matrix get_u_global() {
		return u_global;
	}

	public Matrix get_v_global() {
		return v_global;
	}

	public Matrix get_a_global() {
		return a_global;
	}

	public Matrix get_G() {
		return G;
	}

	public Matrix get_r_global() {
		return r_global;
	}

	public Matrix get_K_constr() {
		return K_constr;
	}

	public Matrix get_M_constr() {
		return M_constr;
	}

	public Matrix get_delta_f_constr() {
		return delta_f_constr;
	}

	public double[] getDisplacement(int nodeID) {
		int dof = solution.getDofOfNodeID(nodeID);
		return new double[]{u_global.get(dof, 0), u_global.get(dof+1, 0), u_global.get(dof+2, 0)};
	}
	
	public double[] getDisplacement(double[] vertexCoords, int elementID, double t, double[] r, Matrix R) {
		if (elementID >= 0) {
			Element element = solution.getRefModel().getElements().get(elementID);
			int[] elemNodes = element.getElementNodes();
			if (element.isLineElement()) {
				LineElement lineElement = (LineElement) element;
				double[] p = SimLive.model.getNodes().get(elemNodes[0]).getCoords();
				Matrix d = new Matrix(new double[]{vertexCoords[0]-p[0], vertexCoords[1]-p[1],
						vertexCoords[2]-p[2]}, 3);
				Matrix R0 = lineElement.getR0();
				Matrix R0x = R0.getMatrix(0, 2, 0, 0);
				double scal = d.dotProduct(R0x);
				d.minusEquals(R0x.times(scal));
				double[] disp1 = R.times(d).minus(d).getColumnPackedCopy();
				double disp[] = lineElement.getDispAtLocalCoordinates(t);
				return new double[]{disp[0]+disp1[0], disp[1]+disp1[1], disp[2]+disp1[2]};
			}
			if (element.isPlaneElement()) {
				PlaneElement planeElement = (PlaneElement) element;
				double[] p = SimLive.model.getNodes().get(elemNodes[0]).getCoords();
				Matrix d = new Matrix(new double[]{vertexCoords[0]-p[0], vertexCoords[1]-p[1],
						vertexCoords[2]-p[2]}, 3);
				Matrix R0 = planeElement.getR0();
				Matrix R0z = R0.getMatrix(0, 2, 2, 2);
				double scal = d.dotProduct(R0z);
				d = R0z.times(scal);
				Matrix d1 = R.times(d);
				double[] disp1 = d1.minus(d).getColumnPackedCopy();
				double disp[] = planeElement.getDispAtLocalCoordinates(r[0], r[1]);
				return new double[]{disp[0]+disp1[0], disp[1]+disp1[1], disp[2]+disp1[2]};
			}
			if (element.getType() == Element.Type.POINT_MASS) {
				double scaling = SimLive.post.getScaling();
				int dof = solution.getDofOfNodeID(elemNodes[0]);
				return new double[]{u_global.get(dof, 0)*scaling,
						u_global.get(dof+1, 0)*scaling,
						u_global.get(dof+2, 0)*scaling};
			}
		}
		return new double[3];
	}
	
	public Matrix getRotation(int elementID, double t, double[] r) {
		if (elementID >= 0) {
			Element element = solution.getRefModel().getElements().get(elementID);
			if (element.isLineElement()) {
				LineElement lineElement = (LineElement) element;
				int elemID = lineElement.getID();
				Matrix R0 = lineElement.getR0();
				Matrix Rr = new Matrix(View.Rr[elemID]);
				if (lineElement.getType() == Element.Type.BEAM) {
					double scaling = SimLive.post.getScaling();
					double[][] angles = this.angles[elemID];
					double f2d = (t-1.0)*(3.0*t-1.0);
					double f5d = t*(3.0*t-2.0);
					double[] rot = new double[3];
					rot[0] = (angles[1][0]-angles[0][0])*t*scaling;
					rot[1] = Math.atan((f2d*angles[0][1]+f5d*angles[1][1])*scaling);
					rot[2] = Math.atan((f2d*angles[0][2]+f5d*angles[1][2])*scaling);
					Matrix R = Beam.rotationMatrixFromAngles(new Matrix(rot, 3));
					Rr = Rr.times(R);
				}
				return Rr.times(R0.transpose());
			}
			if (element.isPlaneElement()) {
				PlaneElement planeElement = (PlaneElement) element;
				double[] rotation = new double[3];
				double[] shapeFunctionValues = planeElement.getShapeFunctionValues(r[0], r[1]);
				double[][] rot = new double[3][planeElement.getElementNodes().length];
				double scaling = SimLive.post.getScaling();
				for (int j = 0; j < planeElement.getElementNodes().length; j++) {
					int nodeID = planeElement.getElementNodes()[j];
					int dof = solution.getDofOfNodeID(nodeID);
					Matrix nodeRot = u_global.getMatrix(dof+3, dof+5, 0, 0).times(scaling);
					for (int i = 0; i < 3; i++) {
						rot[i][j] = solution.getRefSettings().isLargeDisplacement ? nodeRot.get(i, 0) : Math.atan(nodeRot.get(i, 0));
					}
				}
				for (int i = 0; i < 3; i++) {
					rotation[i] = planeElement.interpolateNodeValues(shapeFunctionValues, rot[i]);
				}
				return Beam.rotationMatrixFromAngles(new Matrix(rotation, 3));
			}
		}
		return Matrix.identity(3, 3);
	}
	
	/*public double[] getPhiRotation(int nodeID) {
		int dof = solution.getDofOfNodeID(nodeID);
		return new double[]{u_global.get(dof+3, 0), u_global.get(dof+4, 0), u_global.get(dof+5, 0)};
	}*/
	
	public double[] getVelocity(int nodeID) {
		int dof = solution.getDofOfNodeID(nodeID);
		return new double[]{v_global.get(dof, 0), v_global.get(dof+1, 0), v_global.get(dof+2, 0)};
	}
	
	/*public double[] getPhiVelocity(int nodeID) {
		int dof = solution.getDofOfNodeID(nodeID);
		return new double[]{v_global.get(dof+3, 0), v_global.get(dof+4, 0), v_global.get(dof+5, 0)};
	}*/
	
	public double[] getAcceleration(int nodeID) {
		int dof = solution.getDofOfNodeID(nodeID);
		return new double[]{a_global.get(dof, 0), a_global.get(dof+1, 0), a_global.get(dof+2, 0)};
	}
	
	/*public double[] getPhiAcceleration(int nodeID) {
		int dof = solution.getDofOfNodeID(nodeID);
		return new double[]{a_global.get(dof+3, 0), a_global.get(dof+4, 0), a_global.get(dof+5, 0)};
	}*/
	
	public double[] getReactions(int nodeID) {
		ArrayList<Node> nodes = solution.getRefModel().getNodes();
		double[] reactions = null;
		int dof = solution.getDofOfNodeID(nodeID);
		if (nodes.get(nodeID).isRotationalDOF()) {
			reactions = new double[6];
			reactions[0] = r_global.get(dof, 0);
			reactions[1] = r_global.get(dof+1, 0);
			reactions[2] = r_global.get(dof+2, 0);
			reactions[3] = r_global.get(dof+3, 0);
			reactions[4] = r_global.get(dof+4, 0);
			reactions[5] = r_global.get(dof+5, 0);
		}
		else {
			reactions = new double[3];
			reactions[0] = r_global.get(dof, 0);
			reactions[1] = r_global.get(dof+1, 0);
			reactions[2] = r_global.get(dof+2, 0);
		}
		return reactions;
	}
	
	public Matrix getRrBeam(int e) {
		return Rr_beam[e];
	}
	
	public double[][] getAnglesBeam(int e) {
		return angles[e];
	}
	
	public double[][] getAngularVelBeam(int e) {
		return angularVel[e];
	}
	
	public double[][] getAngularAccBeam(int e) {
		return angularAcc[e];
	}
	
	private Matrix addRowToMatrix(Matrix matrix, Matrix row) {
		Matrix newMatrix = new Matrix(matrix.getRowDimension()+1, matrix.getColumnDimension());
		newMatrix.setMatrix(0, matrix.getRowDimension()-1, 0, matrix.getColumnDimension()-1, matrix);
		newMatrix.setMatrix(matrix.getRowDimension(), matrix.getRowDimension(), 0, matrix.getColumnDimension()-1, row);
		return newMatrix;
	}
	
	public Solution getSolution() {
		return solution;
	}
}

package simlive.solution;

import Jama.Matrix;
import simlive.model.Model;

public abstract class ConstraintMethod {
	
	public enum Type {LAGRANGE_MULTIPLIERS, PENALTY_METHOD}

	public abstract Matrix getConstrainedMatrix(Matrix matrix, Matrix G);
	
	public abstract Matrix getConstrainedRHS(Matrix rhs, Matrix C_global, Matrix matrix, Matrix G, Matrix g);
	
	public abstract Matrix getFullSolution(Matrix solutionConstr, Matrix G);
	
	public abstract Matrix removeInvalidEigenvalues(Matrix D, Matrix G);
	
	public abstract Matrix removeInvalidEigenvectors(Matrix V, Matrix G);
	
	public abstract Matrix getConstraintForce(Matrix C_global, Matrix solutionConstr, Matrix G, Matrix g, Matrix matrix);
	
	public abstract String[] getGlobalDofNames(Matrix G, Model refModel);

}

package simlive.solution;

import java.util.ArrayList;

import Jama.Matrix;
import simlive.model.Model;
import simlive.model.Node;

public class PenaltyMethod extends ConstraintMethod {
	
	private double PENALTY_FACTOR;
	
	public PenaltyMethod(double penaltyFactor) {
		PENALTY_FACTOR = penaltyFactor;
	}
	
	private double getPenaltyValue(Matrix matrix) {
		int nDofs = matrix.getRowDimension();
		double maxDiag = 0;
		for (int i = 0; i < nDofs; i++) {
			if (Math.abs(matrix.get(i,i)) > maxDiag) maxDiag = Math.abs(matrix.get(i,i));				
		}
		return maxDiag * PENALTY_FACTOR;		
	}
	
	@Override
	public Matrix getConstrainedMatrix(Matrix matrix, Matrix G) {
		return matrix.plus(G.transposeTimesItself().times(getPenaltyValue(matrix)));
	}

	@Override
	public Matrix getConstrainedRHS(Matrix rhs, Matrix C_global, Matrix matrix, Matrix G, Matrix g) {
		double penalty = getPenaltyValue(matrix);
		return rhs.plus(G.transpose().times(g.times(penalty)));
	}

	@Override
	public Matrix getFullSolution(Matrix solutionConstr, Matrix G) {
		return solutionConstr;
	}
	
	@Override
	public Matrix removeInvalidEigenvalues(Matrix D, Matrix G) {
		int nConstraints = G.getRowDimension();
		return D.getMatrix(0, D.getRowDimension()-1-nConstraints, 0, 0);
	}

	@Override
	public Matrix removeInvalidEigenvectors(Matrix V, Matrix G) {
		int nConstraints = G.getRowDimension();
		return V.getMatrix(0, V.getRowDimension()-1, 0, V.getColumnDimension()-1-nConstraints);
	}

	@Override
	public Matrix getConstraintForce(Matrix C_global, Matrix solutionConstr, Matrix G, Matrix g, Matrix matrix) {
		double penalty = getPenaltyValue(matrix);
		return G.transpose().times(g.minus(G.times(solutionConstr)).times(penalty));
	}

	@Override
	public String[] getGlobalDofNames(Matrix G, Model refModel) {
		ArrayList<Node> nodes = refModel.getNodes();
		
		ArrayList<String> dofNames = new ArrayList<String>();
		for (int n = 0; n < nodes.size(); n++) {
			dofNames.add("u"+(n+1));
			dofNames.add("v"+(n+1));
			if (!Model.twoDimensional || G.getRowDimension() == 0) {
				dofNames.add("w"+(n+1));
			}
			if (nodes.get(n).isRotationalDOF()) {
				if (!Model.twoDimensional || G.getRowDimension() == 0) {
					dofNames.add("\u03b8x"+(n+1));
					dofNames.add("\u03b8y"+(n+1));
				}
				dofNames.add("\u03b8z"+(n+1));
			}
		}
		return dofNames.toArray(new String[0]);
	}

}

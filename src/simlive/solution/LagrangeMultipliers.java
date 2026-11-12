package simlive.solution;

import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import simlive.model.Model;
import simlive.model.Node;

public class LagrangeMultipliers extends ConstraintMethod {

	@Override
	public Matrix getConstrainedMatrix(Matrix matrix, boolean keep, Matrix G) {
		int nDofs = G.getColumnDimension();
		int nLambdas = G.getRowDimension();
		Matrix X = keep ? matrix.copy() : matrix;
		double[][] array = X.getArray();
		for (int r = 0; r < nDofs; r++) {
			array[r] = Arrays.copyOf(array[r], nDofs+nLambdas);
		}
		array = Arrays.copyOf(array, nDofs+nLambdas);
		for (int r = nDofs; r < nDofs+nLambdas; r++) {
			array[r] = new double[nDofs+nLambdas];
		}
		X = new Matrix(array);
		X.setMatrix(nDofs, 0, G);
		X.setMatrix(0, nDofs, G.transpose());
		return X;
	}

	@Override
	public Matrix getConstrainedRHS(Matrix rhs, Matrix C_global, Matrix G, Matrix g) {
		int nDofs = rhs.getRowDimension();
		int nLambdas = g.getRowDimension();
		Matrix rhsConstr = new Matrix(nDofs+nLambdas, 1);
		rhsConstr.setMatrix(0, 0, rhs.plus(C_global));
		rhsConstr.setMatrix(nDofs, 0, g);
		return rhsConstr;
	}

	@Override
	public Matrix getFullSolution(Matrix solutionConstr, Matrix G) {
		int nDofs = G.getColumnDimension();
		return solutionConstr.getMatrix(0, nDofs-1, 0, 0);
	}

	@Override
	public Matrix removeInvalidEigenvalues(Matrix D, Matrix G) {
		int nConstraints = 2*G.getRowDimension();
		return D.getMatrix(0, D.getRowDimension()-1-nConstraints, 0, 0);
	}

	@Override
	public Matrix removeInvalidEigenvectors(Matrix V, Matrix G) {
		int nConstraints = 2*G.getRowDimension();
		return V.getMatrix(0, V.getRowDimension()-1, 0, V.getColumnDimension()-1-nConstraints);
	}

	@Override
	public Matrix getConstraintForce(Matrix C_global, Matrix solutionConstr, Matrix G, Matrix g) {
		int nDofs = G.getColumnDimension();
		int nLambdas = G.getRowDimension();
		Matrix lambda = solutionConstr.getMatrix(nDofs, nDofs+nLambdas-1, 0, 0);
		Matrix delta_C_global = G.transpose().times(lambda);
		C_global = C_global.minus(delta_C_global);
		/* contact lost during iteration */
		for (int i = 0; i < nDofs; i++) {
			if (G.getMatrix(0, nLambdas-1, i, i).norm1() == 0.0) {
				C_global.set(i, 0, 0.0);
			}
		}
		return C_global;
	}

	@Override
	public String[] getGlobalDofNames(Matrix G, Model refModel) {
		ArrayList<Node> nodes = refModel.getNodes();
		int nLambdas = G.getRowDimension();
		
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
		for (int lambda = 0; lambda < nLambdas; lambda++) {
			dofNames.add("\u03bb"+(lambda+1));
		}
		return dofNames.toArray(new String[0]);
	}

}

package Jama;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StreamTokenizer;
import Jama.util.*;

/**
   Jama = Java Matrix class.
<P>
   The Java Matrix Class provides the fundamental operations of numerical
   linear algebra.  Various constructors create Matrices from two dimensional
   arrays of double precision floating point numbers.  Various "gets" and
   "sets" provide access to submatrices and matrix elements.  Several methods 
   implement basic matrix arithmetic, including matrix addition and
   multiplication, matrix norms, and element-by-element array operations.
   Methods for reading and printing matrices are also included.  All the
   operations in this version of the Matrix Class involve real matrices.
   Complex matrices may be handled in a future version.
<P>
   Five fundamental matrix decompositions, which consist of pairs or triples
   of matrices, permutation vectors, and the like, produce results in five
   decomposition classes.  These decompositions are accessed by the Matrix
   class to compute solutions of simultaneous linear equations, determinants,
   inverses and other matrix functions.  The five decompositions are:
<P><UL>
   <LI>Cholesky Decomposition of symmetric, positive definite matrices.
   <LI>LU Decomposition of rectangular matrices.
   <LI>QR Decomposition of rectangular matrices.
   <LI>Singular Value Decomposition of rectangular matrices.
   <LI>Eigenvalue Decomposition of both symmetric and nonsymmetric square matrices.
</UL>
<DL>
<DT><B>Example of use:</B></DT>
<P>
<DD>Solve a linear system A x = b and compute the residual norm, ||b - A x||.
<P><PRE>
      double[][] vals = {{1.,2.,3},{4.,5.,6.},{7.,8.,10.}};
      Matrix A = new Matrix(vals);
      Matrix b = Matrix.random(3,1);
      Matrix x = A.solve(b);
      Matrix r = A.times(x).minus(b);
      double rnorm = r.normInf();
</PRE></DD>
</DL>

@author The MathWorks, Inc. and the National Institute of Standards and Technology.
@version 5 August 1998
*/

public class Matrix implements Cloneable, java.io.Serializable {

/* ------------------------
   Class variables
 * ------------------------ */

   /** Array for internal storage of elements.
   @serial internal array storage.
   */
   private double[][] A;

   /** Row and column dimensions.
   @serial row dimension.
   @serial column dimension.
   */
   private int m, n;

/* ------------------------
   Constructors
 * ------------------------ */

   /** Construct an m-by-n matrix of zeros. 
   @param m    Number of rows.
   @param n    Number of colums.
   */

   public Matrix (int m, int n) {
      this.m = m;
      this.n = n;
      A = new double[m][n];
   }

   /** Construct an m-by-n constant matrix.
   @param m    Number of rows.
   @param n    Number of colums.
   @param s    Fill the matrix with this scalar value.
   */

   public Matrix (int m, int n, double s) {
      this.m = m;
      this.n = n;
      A = new double[m][n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = s;
         }
      }
   }

   /** Construct a matrix from a 2-D array.
   @param A    Two-dimensional array of doubles.
   @exception  IllegalArgumentException All rows must have the same length
   @see        #constructWithCopy
   */

   public Matrix (double[][] A) {
      m = A.length;
      n = A[0].length;
      for (int i = 0; i < m; i++) {
         if (A[i].length != n) {
            throw new IllegalArgumentException("All rows must have the same length.");
         }
      }
      this.A = A;
   }

   /** Construct a matrix quickly without checking arguments.
   @param A    Two-dimensional array of doubles.
   @param m    Number of rows.
   @param n    Number of colums.
   */

   public Matrix (double[][] A, int m, int n) {
      this.A = A;
      this.m = m;
      this.n = n;
   }

   /** Construct a matrix from a one-dimensional packed array
   @param vals One-dimensional array of doubles, packed by columns (ala Fortran).
   @param m    Number of rows.
   @exception  IllegalArgumentException Array length must be a multiple of m.
   */

   public Matrix (double vals[], int m) {
      this.m = m;
      n = (m != 0 ? vals.length/m : 0);
      if (m*n != vals.length) {
         throw new IllegalArgumentException("Array length must be a multiple of m.");
      }
      A = new double[m][n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = vals[i+j*m];
         }
      }
   }

/* ------------------------
   Public Methods
 * ------------------------ */

   /** Construct a matrix from a copy of a 2-D array.
   @param A    Two-dimensional array of doubles.
   @exception  IllegalArgumentException All rows must have the same length
   */

   public static Matrix constructWithCopy(double[][] A) {
      int m = A.length;
      int n = A[0].length;
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         if (A[i].length != n) {
            throw new IllegalArgumentException
               ("All rows must have the same length.");
         }
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j];
         }
      }
      return X;
   }

   /** Make a deep copy of a matrix
   */

   public Matrix copy () {
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j];
         }
      }
      return X;
   }

   /** Clone the Matrix object.
   */

   public Object clone () {
      return this.copy();
   }

   public Matrix getEmptyCopy() {
      return new Matrix(m, n);
   }

   /** Access the internal two-dimensional array.
   @return     Pointer to the two-dimensional array of matrix elements.
   */

   public double[][] getArray () {
      return A;
   }

   /** Copy the internal two-dimensional array.
   @return     Two-dimensional array copy of matrix elements.
   */

   public double[][] getArrayCopy () {
      double[][] C = new double[m][n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j];
         }
      }
      return C;
   }

   /** Make a one-dimensional column packed copy of the internal array.
   @return     Matrix elements packed in a one-dimensional array by columns.
   */

   public double[] getColumnPackedCopy () {
      double[] vals = new double[m*n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            vals[i+j*m] = A[i][j];
         }
      }
      return vals;
   }

   /** Make a one-dimensional row packed copy of the internal array.
   @return     Matrix elements packed in a one-dimensional array by rows.
   */

   public double[] getRowPackedCopy () {
      double[] vals = new double[m*n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            vals[i*n+j] = A[i][j];
         }
      }
      return vals;
   }

   /** Get row dimension.
   @return     m, the number of rows.
   */

   public int getRowDimension () {
      return m;
   }

   /** Get column dimension.
   @return     n, the number of columns.
   */

   public int getColumnDimension () {
      return n;
   }

   /** Get a single element.
   @param i    Row index.
   @param j    Column index.
   @return     A(i,j)
   @exception  ArrayIndexOutOfBoundsException
   */

   public double get (int i, int j) {
      return A[i][j];
   }

   /** Get a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param j0   Initial column index
   @param j1   Final column index
   @return     A(i0:i1,j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public Matrix getMatrix (int i0, int i1, int j0, int j1) {
      Matrix X = new Matrix(i1-i0+1,j1-j0+1);
      double[][] B = X.getArray();
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
               B[i-i0][j-j0] = A[i][j];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Get a submatrix.
   @param r    Array of row indices.
   @param c    Array of column indices.
   @return     A(r(:),c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public Matrix getMatrix (int[] r, int[] c) {
      Matrix X = new Matrix(r.length,c.length);
      double[][] B = X.getArray();
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = 0; j < c.length; j++) {
               B[i][j] = A[r[i]][c[j]];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Get a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param c    Array of column indices.
   @return     A(i0:i1,c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public Matrix getMatrix (int i0, int i1, int[] c) {
      Matrix X = new Matrix(i1-i0+1,c.length);
      double[][] B = X.getArray();
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = 0; j < c.length; j++) {
               B[i-i0][j] = A[i][c[j]];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Get a submatrix.
   @param r    Array of row indices.
   @param j0   Initial column index
   @param j1   Final column index
   @return     A(r(:),j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public Matrix getMatrix (int[] r, int j0, int j1) {
      Matrix X = new Matrix(r.length,j1-j0+1);
      double[][] B = X.getArray();
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = j0; j <= j1; j++) {
               B[i][j-j0] = A[r[i]][j];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Set a single element.
   @param i    Row index.
   @param j    Column index.
   @param s    A(i,j).
   @exception  ArrayIndexOutOfBoundsException
   */

   public void set (int i, int j, double s) {
      A[i][j] = s;
   }

   /** Set a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param j0   Initial column index
   @param j1   Final column index
   @param X    A(i0:i1,j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int i0, int i1, int j0, int j1, Matrix X) {
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
               A[i][j] = X.get(i-i0,j-j0);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Set a submatrix.
   @param r    Array of row indices.
   @param c    Array of column indices.
   @param X    A(r(:),c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int[] r, int[] c, Matrix X) {
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = 0; j < c.length; j++) {
               A[r[i]][c[j]] = X.get(i,j);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Set a submatrix.
   @param r    Array of row indices.
   @param j0   Initial column index
   @param j1   Final column index
   @param X    A(r(:),j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int[] r, int j0, int j1, Matrix X) {
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = j0; j <= j1; j++) {
               A[r[i]][j] = X.get(i,j-j0);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Set a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param c    Array of column indices.
   @param X    A(i0:i1,c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int i0, int i1, int[] c, Matrix X) {
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = 0; j < c.length; j++) {
               A[i][c[j]] = X.get(i-i0,j);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Matrix transpose.
   @return    A'
   */

   public Matrix transpose () {
      Matrix X = new Matrix(n,m);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[j][i] = A[i][j];
         }
      }
      return X;
   }

   /** One norm
   @return    maximum column sum.
   */

   public double norm1 () {
      double f = 0;
      for (int j = 0; j < n; j++) {
         double s = 0;
         for (int i = 0; i < m; i++) {
            s += Math.abs(A[i][j]);
         }
         f = Math.max(f,s);
      }
      return f;
   }

   /** Two norm
   @return    maximum singular value.
   */

   public double norm2 () {
      return (new SingularValueDecomposition(this).norm2());
   }

   /** Infinity norm
   @return    maximum row sum.
   */

   public double normInf () {
      double f = 0;
      for (int i = 0; i < m; i++) {
         double s = 0;
         for (int j = 0; j < n; j++) {
            s += Math.abs(A[i][j]);
         }
         f = Math.max(f,s);
      }
      return f;
   }

   /** Frobenius norm
   @return    sqrt of sum of squares of all elements.
   */

   public double normF () {
      double f = 0;
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            f = Maths.hypot(f,A[i][j]);
         }
      }
      return f;
   }

   /**  Unary minus
   @return    -A
   */

   public Matrix uminus () {
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = -A[i][j];
         }
      }
      return X;
   }

   /** C = A + B
   @param B    another matrix
   @return     A + B
   */

   public Matrix plus (Matrix B) {
      checkMatrixDimensions(B);
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j] + B.A[i][j];
         }
      }
      return X;
   }

   /** A = A + B
   @param B    another matrix
   @return     A + B
   */

   public Matrix plusEquals (Matrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j] + B.A[i][j];
         }
      }
      return this;
   }

   /** C = A - B
   @param B    another matrix
   @return     A - B
   */

   public Matrix minus (Matrix B) {
      checkMatrixDimensions(B);
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j] - B.A[i][j];
         }
      }
      return X;
   }

   /** A = A - B
   @param B    another matrix
   @return     A - B
   */

   public Matrix minusEquals (Matrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j] - B.A[i][j];
         }
      }
      return this;
   }

   /** Element-by-element multiplication, C = A.*B
   @param B    another matrix
   @return     A.*B
   */

   public Matrix arrayTimes (Matrix B) {
      checkMatrixDimensions(B);
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j] * B.A[i][j];
         }
      }
      return X;
   }

   /** Element-by-element multiplication in place, A = A.*B
   @param B    another matrix
   @return     A.*B
   */

   public Matrix arrayTimesEquals (Matrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j] * B.A[i][j];
         }
      }
      return this;
   }

   /** Element-by-element right division, C = A./B
   @param B    another matrix
   @return     A./B
   */

   public Matrix arrayRightDivide (Matrix B) {
      checkMatrixDimensions(B);
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j] / B.A[i][j];
         }
      }
      return X;
   }

   /** Element-by-element right division in place, A = A./B
   @param B    another matrix
   @return     A./B
   */

   public Matrix arrayRightDivideEquals (Matrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j] / B.A[i][j];
         }
      }
      return this;
   }

   /** Element-by-element left division, C = A.\B
   @param B    another matrix
   @return     A.\B
   */

   public Matrix arrayLeftDivide (Matrix B) {
      checkMatrixDimensions(B);
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = B.A[i][j] / A[i][j];
         }
      }
      return X;
   }

   /** Element-by-element left division in place, A = A.\B
   @param B    another matrix
   @return     A.\B
   */

   public Matrix arrayLeftDivideEquals (Matrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = B.A[i][j] / A[i][j];
         }
      }
      return this;
   }

   /** Multiply a matrix by a scalar, C = s*A
   @param s    scalar
   @return     s*A
   */

   public Matrix times (double s) {
      Matrix X = new Matrix(m,n);
      double[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = s*A[i][j];
         }
      }
      return X;
   }

   /** Multiply a matrix by a scalar in place, A = s*A
   @param s    scalar
   @return     replace A by s*A
   */

   public Matrix timesEquals (double s) {
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = s*A[i][j];
         }
      }
      return this;
   }

   /** Linear algebraic matrix multiplication, A * B
   @param B    another matrix
   @return     Matrix product, A * B
   @exception  IllegalArgumentException Matrix inner dimensions must agree.
   */

   public Matrix times (Matrix B) {
      if (B.m != n) {
         throw new IllegalArgumentException("Matrix inner dimensions must agree.");
      }
      Matrix X = new Matrix(m,B.n);
      double[][] C = X.getArray();
      int[] startA = new int[m];
      int[] endA = new int[m];
      int[] startB = new int[B.n];
      int[] endB = new int[B.n];
      for (int j = 0; j < m; j++) {
         while (startA[j] < n && A[j][startA[j]] == 0.0) {
            startA[j]++;
         }
         if (startA[j] < n) {
            endA[j] = n-1;
            while (endA[j] > startA[j] && A[j][endA[j]] == 0.0) {
               endA[j]--;
            }
         }
      }
      for (int j = 0; j < B.n; j++) {
         while (startB[j] < n && B.A[startB[j]][j] == 0.0) {
            startB[j]++;
         }
         if (startB[j] < n) {
            endB[j] = n-1;
            while (endB[j] > startB[j] && B.A[endB[j]][j] == 0.0) {
               endB[j]--;
            }
         }
      }
      for (int j = 0; j < B.n; j++) if (startB[j] < n) {
         for (int i = 0; i < m; i++) if (startA[i] < n) {
            double s = 0;
            int start = Math.max(startA[i], startB[j]);
            int end = Math.min(endA[i], endB[j]);
            for (int k = start; k <= end; k++) {
               s += A[i][k]*B.A[k][j];
            }
            C[i][j] = s;
         }
      }
      return X;
   }

   /** Linear algebraic matrix multiplication, A' * A
   @return     Matrix product, A' * A
   */

   public Matrix transposeTimesItself () {
      Matrix X = new Matrix(n,n);
      int[] NumberNonZeroIndices = new int[n];
      int[][] NonZeroIndices = new int[n][m];
      for (int c = 0; c < n; c++) {
         for (int r = 0; r < m; r++) {
            if (A[r][c] != 0.0) {
               NonZeroIndices[c][NumberNonZeroIndices[c]++] = r;
            }
         }
      }
      for (int c = 0; c < n; c++) if (NumberNonZeroIndices[c] > 0) {
         for (int r = 0; r < c+1; r++) if (NumberNonZeroIndices[r] > 0) {
            double s = 0;
            for (int index = 0; index < NumberNonZeroIndices[c]; index++) {
               int r1 = NonZeroIndices[c][index];
               s += A[r1][r]*A[r1][c];
            }
            X.A[r][c] = s;
            if (r != c) X.A[c][r] = s;
         }
      }
      return X;
   }

   /** LU Decomposition
   @return     LUDecomposition
   @see LUDecomposition
   */

   public LUDecomposition lu () {
      return new LUDecomposition(this);
   }

   /** QR Decomposition
   @return     QRDecomposition
   @see QRDecomposition
   */

   public QRDecomposition qr () {
      return new QRDecomposition(this);
   }

   /** Cholesky Decomposition
   @return     CholeskyDecomposition
   @see CholeskyDecomposition
   */

   public CholeskyDecomposition chol () {
      return new CholeskyDecomposition(this);
   }

   /** Singular Value Decomposition
   @return     SingularValueDecomposition
   @see SingularValueDecomposition
   */

   public SingularValueDecomposition svd () {
      return new SingularValueDecomposition(this);
   }

   /** Eigenvalue Decomposition
   @return     EigenvalueDecomposition
   @see EigenvalueDecomposition
   */

   public EigenvalueDecomposition eig (boolean modalAnalysis) {
      return new EigenvalueDecomposition(this, modalAnalysis);
   }

   /** Solve A*X = B
   @param B    right hand side
   @return     solution if A is square, least squares solution otherwise
   */

   public Matrix solve (Matrix B) {
      return (m == n ? (new LUDecomposition(this)).solve(B) :
                       (new QRDecomposition(this)).solve(B));
   }

   /** Solve X*A = B, which is also A'*X' = B'
   @param B    right hand side
   @return     solution if A is square, least squares solution otherwise.
   */

   public Matrix solveTranspose (Matrix B) {
      return transpose().solve(B.transpose());
   }

   /** Matrix inverse or pseudoinverse
   @return     inverse(A) if A is square, pseudoinverse otherwise.
   */

   public Matrix inverse () {
      if (m == 2 && n == 2) {
         double factor = 1.0/(A[0][0]*A[1][1]-A[0][1]*A[1][0]);
         double[][] array = {{A[1][1], -A[0][1]}, {-A[1][0], A[0][0]}};
         return new Matrix(array).times(factor);
      }
      else if (m == 3 && n == 3) {
         double factor = 1.0/(A[0][0]*A[1][1]*A[2][2]+A[0][1]*A[1][2]*A[2][0]+A[0][2]*A[1][0]*A[2][1]-
                              A[0][1]*A[1][0]*A[2][2]-A[0][2]*A[1][1]*A[2][0]-A[0][0]*A[1][2]*A[2][1]);
         double[][] array = {{ A[1][1]*A[2][2]-A[1][2]*A[2][1], -A[0][1]*A[2][2]+A[0][2]*A[2][1],  A[0][1]*A[1][2]-A[0][2]*A[1][1]},
                             {-A[1][0]*A[2][2]+A[1][2]*A[2][0],  A[0][0]*A[2][2]-A[0][2]*A[2][0], -A[0][0]*A[1][2]+A[0][2]*A[1][0]},
                             { A[1][0]*A[2][1]-A[1][1]*A[2][0], -A[0][0]*A[2][1]+A[0][1]*A[2][0],  A[0][0]*A[1][1]-A[0][1]*A[1][0]}};
         return new Matrix(array).times(factor);
      }
      else {
         return solve(identity(m,m));
      }
   }

   /** Matrix determinant
   @return     determinant
   */

   public double det () {
      return new LUDecomposition(this).det();
   }
   
   /** 3x1 vector cross product
   @param B    another 3x1 vector
   @return     cross product, A x B
   */

   public Matrix crossProduct (Matrix B) {
      Matrix X = new Matrix(3, 1);
      X.set(0, 0, A[1][0]*B.get(2, 0)-A[2][0]*B.get(1, 0));
      X.set(1, 0, A[2][0]*B.get(0, 0)-A[0][0]*B.get(2, 0));
      X.set(2, 0, A[0][0]*B.get(1, 0)-A[1][0]*B.get(0, 0));
      return X;
   }
   
   /** nx1 vector dot product
   @param B    another nx1 vector
   @return     dot product, A' * B
   */

   public double dotProduct (Matrix B) {
      return A[0][0]*B.A[0][0]+A[1][0]*B.A[1][0]+A[2][0]*B.A[2][0];
      //return transpose().times(B).get(0, 0);
   }

   /** Matrix rank
   @return     effective numerical rank, obtained from SVD.
   */

   public int rank () {
      return new SingularValueDecomposition(this).rank();
   }

   /** Matrix condition (2 norm)
   @return     ratio of largest to smallest singular value.
   */

   public double cond () {
      return new SingularValueDecomposition(this).cond();
   }

   /** Matrix trace.
   @return     sum of the diagonal elements.
   */

   public double trace () {
      double t = 0;
      for (int i = 0; i < Math.min(m,n); i++) {
         t += A[i][i];
      }
      return t;
   }

   /** Generate matrix with random elements
   @param m    Number of rows.
   @param n    Number of colums.
   @return     An m-by-n matrix with uniformly distributed random elements.
   */

   public static Matrix random (int m, int n) {
      Matrix A = new Matrix(m,n);
      double[][] X = A.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            X[i][j] = Math.random();
         }
      }
      return A;
   }

   /** Generate identity matrix
   @param m    Number of rows.
   @param n    Number of colums.
   @return     An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */

   public static Matrix identity (int m, int n) {
      Matrix A = new Matrix(m,n);
      double[][] X = A.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            X[i][j] = (i == j ? 1.0 : 0.0);
         }
      }
      return A;
   }


   /** Print the matrix to stdout.   Line the elements up in columns
     * with a Fortran-like 'Fw.d' style format.
   @param w    Column width.
   @param d    Number of digits after the decimal.
   */

   public void print (int w, int d) {
      print(new PrintWriter(System.out,true),w,d); }

   /** Print the matrix to the output stream.   Line the elements up in
     * columns with a Fortran-like 'Fw.d' style format.
   @param output Output stream.
   @param w      Column width.
   @param d      Number of digits after the decimal.
   */

   public void print (PrintWriter output, int w, int d) {
      DecimalFormat format = new DecimalFormat();
      format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
      format.setMinimumIntegerDigits(1);
      format.setMaximumFractionDigits(d);
      format.setMinimumFractionDigits(d);
      format.setGroupingUsed(false);
      print(output,format,w+2);
   }

   /** Print the matrix to stdout.  Line the elements up in columns.
     * Use the format object, and right justify within columns of width
     * characters.
     * Note that is the matrix is to be read back in, you probably will want
     * to use a NumberFormat that is set to US Locale.
   @param format A  Formatting object for individual elements.
   @param width     Field width for each column.
   @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

   public void print (NumberFormat format, int width) {
      print(new PrintWriter(System.out,true),format,width); }

   // DecimalFormat is a little disappointing coming from Fortran or C's printf.
   // Since it doesn't pad on the left, the elements will come out different
   // widths.  Consequently, we'll pass the desired column width in as an
   // argument and do the extra padding ourselves.

   /** Print the matrix to the output stream.  Line the elements up in columns.
     * Use the format object, and right justify within columns of width
     * characters.
     * Note that is the matrix is to be read back in, you probably will want
     * to use a NumberFormat that is set to US Locale.
   @param output the output stream.
   @param format A formatting object to format the matrix elements 
   @param width  Column width.
   @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

   public void print (PrintWriter output, NumberFormat format, int width) {
      output.println();  // start on new line.
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            String s = format.format(A[i][j]); // format the number
            int padding = Math.max(1,width-s.length()); // At _least_ 1 space
            for (int k = 0; k < padding; k++)
               output.print(' ');
            output.print(s);
         }
         output.println();
      }
      output.println();   // end with blank line.
   }

   /** Read a matrix from a stream.  The format is the same the print method,
     * so printed matrices can be read back in (provided they were printed using
     * US Locale).  Elements are separated by
     * whitespace, all the elements for each row appear on a single line,
     * the last row is followed by a blank line.
   @param input the input stream.
   */

   public static Matrix read (BufferedReader input) throws java.io.IOException {
      StreamTokenizer tokenizer= new StreamTokenizer(input);

      // Although StreamTokenizer will parse numbers, it doesn't recognize
      // scientific notation (E or D); however, Double.valueOf does.
      // The strategy here is to disable StreamTokenizer's number parsing.
      // We'll only get whitespace delimited words, EOL's and EOF's.
      // These words should all be numbers, for Double.valueOf to parse.

      tokenizer.resetSyntax();
      tokenizer.wordChars(0,255);
      tokenizer.whitespaceChars(0, ' ');
      tokenizer.eolIsSignificant(true);
      java.util.Vector<Double> vD = new java.util.Vector<Double>();

      // Ignore initial empty lines
      while (tokenizer.nextToken() == StreamTokenizer.TT_EOL);
      if (tokenizer.ttype == StreamTokenizer.TT_EOF)
	throw new java.io.IOException("Unexpected EOF on matrix read.");
      do {
         vD.addElement(Double.valueOf(tokenizer.sval)); // Read & store 1st row.
      } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);

      int n = vD.size();  // Now we've got the number of columns!
      double row[] = new double[n];
      for (int j=0; j<n; j++)  // extract the elements of the 1st row.
         row[j]=vD.elementAt(j).doubleValue();
      java.util.Vector<double[]> v = new java.util.Vector<double[]>();
      v.addElement(row);  // Start storing rows instead of columns.
      while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
         // While non-empty lines
         v.addElement(row = new double[n]);
         int j = 0;
         do {
            if (j >= n) throw new java.io.IOException
               ("Row " + v.size() + " is too long.");
            row[j++] = Double.valueOf(tokenizer.sval).doubleValue();
         } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);
         if (j < n) throw new java.io.IOException
            ("Row " + v.size() + " is too short.");
      }
      int m = v.size();  // Now we've got the number of rows.
      double[][] A = new double[m][];
      v.copyInto(A);  // copy the rows out of the vector
      return new Matrix(A);
   }


/* ------------------------
   Private Methods
 * ------------------------ */

   /** Check if size(A) == size(B) **/

   private void checkMatrixDimensions (Matrix B) {
      if (B.m != m || B.n != n) {
         throw new IllegalArgumentException("Matrix dimensions must agree.");
      }
   }

  private static final long serialVersionUID = 1;
}

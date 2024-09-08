package simlive.postprocessing;

public class TensorPlot {

	/* majorVectors: majorVector per gauss point, per quad element, per increment */
	/* minorVectors: minorVector per gauss point, per quad element, per increment */
	private double[][][][] majorVectors;
	private double[][][][] minorVectors;
	
	public TensorPlot(double[][][][] majorVectors, double[][][][] minorVectors) {
		this.majorVectors = majorVectors;
		this.minorVectors = minorVectors;
	}
	
	public double[] getMajorVectorAtGaussPoint(int inc, int elementID, int gaussPoint) {
		return majorVectors[inc][elementID][gaussPoint];
	}
	
	public double[] getMinorVectorAtGaussPoint(int inc, int elementID, int gaussPoint) {
		return minorVectors[inc][elementID][gaussPoint];
	}

}

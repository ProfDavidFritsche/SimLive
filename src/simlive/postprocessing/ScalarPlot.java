package simlive.postprocessing;
import java.util.ArrayList;

import simlive.SimLive;
import simlive.misc.Units;
import simlive.model.Element;
import simlive.model.LineElement;
import simlive.solution.Increment;

public class ScalarPlot {

	private float[][] palette;
	/* scalarValues: value per global node, except for normal force, shear force,
	 * bending moment: value per local node (two values per LineElement) */
	private double[][] scalarValues;
	private double globalMinValue, globalMaxValue;
	public static String[] types = {"No Variable", 
			"Total Acceleration", "Acceleration x", "Acceleration y", "Acceleration z", 
			"Total Velocity", "Velocity x", "Velocity y", "Velocity z", 
			"Total Displacement", "Displacement x", "Displacement y", "Displacement z", "Spring Deflection",
			"Normal Force", "Shear Force y", "Shear Force z", "Torsional Moment", "Bending Moment y", "Bending Moment z",
			"Major Principal Strain", "Minor Principal Strain", "Equivalent Strain",
			"Major Principal Stress", "Minor Principal Stress", "Mises Stress", "Tresca Stress", "Rankine Stress", "Thickening"};
	private String type;
	
	public ScalarPlot(double[][] scalarValues, String type) {
		this.scalarValues = scalarValues;
		this.type = type;
		setPalette(SimLive.post.getNumberOfColors());
		
		calculateGlobalMinValue();
		calculateGlobalMaxValue();
	}
	
	private void calculateGlobalMinValue() {
		globalMinValue = Double.MAX_VALUE;
		ArrayList<Element> elements = SimLive.model.getElements();
		for (int inc = 0; inc < SimLive.post.getSolution().getNumberOfIncrements()+1; inc++) {
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (hasValue(elements.get(elem), SimLive.post.getSolution().getIncrement(inc))) {
					if (elements.get(elem).isLineElement()) {
						LineElement element = (LineElement) elements.get(elem);
						double t = 0.0;
						for (int i = 0; i <= SimLive.LINE_DIVISIONS_MAX; i++) {
							double value = getValueForLineElement(element, t, inc);
							if (value < globalMinValue) {
								globalMinValue = value;
							}
							t += 1.0/SimLive.LINE_DIVISIONS_MAX;
						}
					}
					else {
						int[] elemNodes = elements.get(elem).getElementNodes();
						for (int n = 0; n < elemNodes.length; n++) {
							if (scalarValues[inc][elemNodes[n]] < globalMinValue) globalMinValue = scalarValues[inc][elemNodes[n]];
						}
					}
				}
			}
		}
	}

	private void calculateGlobalMaxValue() {
		globalMaxValue = -Double.MAX_VALUE;
		ArrayList<Element> elements = SimLive.model.getElements();
		for (int inc = 0; inc < SimLive.post.getSolution().getNumberOfIncrements()+1; inc++) {
			
			for (int elem = 0; elem < elements.size(); elem++) {
				if (hasValue(elements.get(elem), SimLive.post.getSolution().getIncrement(inc))) {
					if (elements.get(elem).isLineElement()) {
						LineElement element = (LineElement) elements.get(elem);
						double t = 0.0;
						for (int i = 0; i <= SimLive.LINE_DIVISIONS_MAX; i++) {
							double value = getValueForLineElement(element, t, inc);
							if (value > globalMaxValue) {
								globalMaxValue = value;
							}
							t += 1.0/SimLive.LINE_DIVISIONS_MAX;
						}
					}
					else {
						int[] elemNodes = elements.get(elem).getElementNodes();
						for (int n = 0; n < elemNodes.length; n++) {
							if (scalarValues[inc][elemNodes[n]] > globalMaxValue) globalMaxValue = scalarValues[inc][elemNodes[n]];
						}
					}
				}
			}
		}
	}
	
	public void setPalette(int numberOfColors /* >=2, <=32 */) {
		float currentValue = 0;
		palette = new float[numberOfColors][];
		palette[0] = new float[]{0,0,1};
		palette[numberOfColors-1] = new float[]{1,0,0};
		for (int i = 1; i < numberOfColors-1; i++) {
			currentValue += 4f/(numberOfColors-1);
			int section = (int) currentValue;
			if (section == 0) {
				palette[i] = new float[]{0,currentValue,1};
			}
			if (section == 1) {
				palette[i] = new float[]{0,1,2-currentValue};
			}
			if (section == 2) {
				palette[i] = new float[]{currentValue-2,1,0};
			}
			if (section == 3) {
				palette[i] = new float[]{1,4-currentValue,0};
			}
		}
	}
	
	public float[] getPalette() {
		float[] p = new float[palette.length*3];
		for (int i = 0; i < palette.length; i++) {
			p[i*3] = palette[i][0];
			p[i*3+1] = palette[i][1];
			p[i*3+2] = palette[i][2];
		}
		return p;
	}

	public float[] getColor(double value, int numberOfColors) {
		int index = 0;
		if (SimLive.post.getMaxValue() - SimLive.post.getMinValue() > SimLive.ZERO_TOL) {
			index = (int) ((value - SimLive.post.getMinValue()) * numberOfColors /
					(SimLive.post.getMaxValue() - SimLive.post.getMinValue()));
			if (index < 0) return palette[0];
			if (index > palette.length-1) return palette[palette.length-1];
		}
		return palette[index];
	}

	public double getGlobalMinValue() {
		return globalMinValue;
	}

	public double getGlobalMaxValue() {
		return globalMaxValue;
	}

	public double getValueAtNode(int nodeID, int inc) {
		return scalarValues[inc][nodeID];
	}

	public String getUnit() {
		String unit = null;
		if (type.equals(types[1])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit()+"\u00B2";
		if (type.equals(types[2])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit()+"\u00B2";
		if (type.equals(types[3])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit()+"\u00B2";
		if (type.equals(types[4])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit()+"\u00B2";
		if (type.equals(types[5])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit();
		if (type.equals(types[6])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit();
		if (type.equals(types[7])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit();
		if (type.equals(types[8])) unit = Units.getLengthUnit()+"/"+Units.getTimeUnit();
		if (type.equals(types[9])) unit = Units.getLengthUnit();
		if (type.equals(types[10])) unit = Units.getLengthUnit();
		if (type.equals(types[11])) unit = Units.getLengthUnit();
		if (type.equals(types[12])) unit = Units.getLengthUnit();
		if (type.equals(types[13])) unit = Units.getLengthUnit();
		if (type.equals(types[14])) unit = Units.getForceUnit();
		if (type.equals(types[15])) unit = Units.getForceUnit();
		if (type.equals(types[16])) unit = Units.getForceUnit();
		if (type.equals(types[17])) unit = Units.getForceUnit()+Units.getLengthUnit();
		if (type.equals(types[18])) unit = Units.getForceUnit()+Units.getLengthUnit();
		if (type.equals(types[19])) unit = Units.getForceUnit()+Units.getLengthUnit();
		if (type.equals(types[20])) unit = "";
		if (type.equals(types[21])) unit = "";
		if (type.equals(types[22])) unit = "";
		if (type.equals(types[23])) unit = Units.getForceUnit()+"/"+Units.getLengthUnit()+"\u00B2";
		if (type.equals(types[24])) unit = Units.getForceUnit()+"/"+Units.getLengthUnit()+"\u00B2";
		if (type.equals(types[25])) unit = Units.getForceUnit()+"/"+Units.getLengthUnit()+"\u00B2";
		if (type.equals(types[26])) unit = Units.getForceUnit()+"/"+Units.getLengthUnit()+"\u00B2";
		if (type.equals(types[27])) unit = Units.getForceUnit()+"/"+Units.getLengthUnit()+"\u00B2";
		if (type.equals(types[28])) unit = Units.getLengthUnit();
		return unit;
	}

	public String getType() {
		return type;
	}
	
	public double getValueForLineElement(LineElement element, double t, int inc) {
		if (type.equals(ScalarPlot.types[13]) ||
			type.equals(ScalarPlot.types[14]) ||
			type.equals(ScalarPlot.types[15]) ||
			type.equals(ScalarPlot.types[16]) ||
			type.equals(ScalarPlot.types[17]) ||
			type.equals(ScalarPlot.types[18]) ||
			type.equals(ScalarPlot.types[19])) {
			int elementID = element.getID();
			double value0 = this.getValueAtNode(elementID*2, inc);
			double value1 = this.getValueAtNode(elementID*2+1, inc);
			return (value1-value0)*t+value0;
		}
		else {
			double[] val = null;
			if (type.equals(ScalarPlot.types[1]) ||
				type.equals(ScalarPlot.types[2]) ||
				type.equals(ScalarPlot.types[3]) ||
				type.equals(ScalarPlot.types[4])) {
				val = element.getKinematicValuesAtLocalCoordinates(t, inc, 1 /*acc*/);
			}
			if (type.equals(ScalarPlot.types[5]) ||
				type.equals(ScalarPlot.types[6]) ||
				type.equals(ScalarPlot.types[7]) ||
				type.equals(ScalarPlot.types[8])) {
				val = element.getKinematicValuesAtLocalCoordinates(t, inc, 2 /*vel*/);
			}
			if (type.equals(ScalarPlot.types[9]) ||
				type.equals(ScalarPlot.types[10]) ||
				type.equals(ScalarPlot.types[11]) ||
				type.equals(ScalarPlot.types[12])) {
				val = element.getKinematicValuesAtLocalCoordinates(t, inc, 0 /*disp*/);
			}
			if (type.equals(ScalarPlot.types[1]) ||
				type.equals(ScalarPlot.types[5]) ||
				type.equals(ScalarPlot.types[9])) {
				return Math.sqrt(val[0]*val[0]+val[1]*val[1]+val[2]*val[2]);
			}
			if (type.equals(ScalarPlot.types[2]) ||
				type.equals(ScalarPlot.types[6]) ||
				type.equals(ScalarPlot.types[10])) {
				return val[0];
			}
			if (type.equals(ScalarPlot.types[3]) ||
				type.equals(ScalarPlot.types[7]) ||
				type.equals(ScalarPlot.types[11])) {
				return val[1];
			}
			if (type.equals(ScalarPlot.types[4]) ||
				type.equals(ScalarPlot.types[8]) ||
				type.equals(ScalarPlot.types[12])) {
				return val[2];
			}
		}
		return 0;
	}
	
	public boolean hasValue(Element element, Increment inc) {
		for (int i = 0; i < 13; i++) {
			if (type.equals(ScalarPlot.types[i])) return true;
		}
		if (element.isPlaneElement()) {
			for (int i = 20; i < 29; i++) {
				if (type.equals(ScalarPlot.types[i])) return true;
			}
		}
		if (element.getType() == Element.Type.ROD &&
			type.equals(ScalarPlot.types[14])) {
			return true;
		}
		if (element.getType() == Element.Type.SPRING &&
			(type.equals(ScalarPlot.types[13]) ||
			 type.equals(ScalarPlot.types[14]))) {
			return true;
		}
		if (element.getType() == Element.Type.BEAM &&
			(type.equals(ScalarPlot.types[14]) ||
			 type.equals(ScalarPlot.types[15]) ||
			 type.equals(ScalarPlot.types[16]) ||
			 type.equals(ScalarPlot.types[17]) ||
			 type.equals(ScalarPlot.types[18]) ||
			 type.equals(ScalarPlot.types[19]))) {
			return true;
		}
		return false;
	}
	
	public boolean isCurvePlot() {
		if (type.equals(ScalarPlot.types[14]) ||
			type.equals(ScalarPlot.types[15]) ||
			type.equals(ScalarPlot.types[16]) ||
			type.equals(ScalarPlot.types[17]) ||
			type.equals(ScalarPlot.types[18]) ||
			type.equals(ScalarPlot.types[19])) {
			return true;
		}
		return false;
	}

}

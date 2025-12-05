package simlive.misc;

import simlive.model.DeepEqualsInterface;
import simlive.model.Element;
import simlive.solution.ConstraintMethod;

public class Settings implements DeepEqualsInterface {

	public static Element.Type newPartType;
	public static double module;
	public static double pressureAngle;
	public ConstraintMethod.Type constraintType;
	public double penaltyFactor;
	public boolean isReorderNodes;
	public boolean isLargeDisplacement;
	public boolean isWriteMatrixView;
	public static double meshSize;
	public static int meshCount;
	/*public boolean isShiftForceVectors;*/
	public Units.UnitSystem unitSystem;
	
	public static boolean isShowAxes, isShowGrid, isShowScale;
	public static boolean isShowOrientations, isShowNodes, isShowEdges, isShowSections, isShowSupports, isShowLoads/*, isShowReactions*/;
	
	public Settings() {
		constraintType = ConstraintMethod.Type.LAGRANGE_MULTIPLIERS;
		penaltyFactor = 1E8;
		isReorderNodes = true;
		isLargeDisplacement = false;
		isWriteMatrixView = false;
		/*isShiftForceVectors = false;*/
		unitSystem = Units.UnitSystem.t_mm_s_N;
	}
	
	public static void resetDisplayOptions() {
		isShowAxes = isShowGrid = isShowScale = true;
		isShowOrientations = isShowNodes = isShowEdges = isShowSections = isShowSupports = isShowLoads = true;
	}
	
	public static void resetAllStaticOptions() {
		resetDisplayOptions();
		newPartType = Element.Type.ROD;
		module = 10.0;
		pressureAngle = 20.0;
		meshSize = 100;
		meshCount = 20;		
	}
	
	public Settings clone() {
		Settings settings = new Settings();
		//settings.newPartType = this.newPartType;
		//settings.module = this.module;
		//settings.pressureAngle = this.pressureAngle;
		settings.constraintType = this.constraintType;
		settings.penaltyFactor = this.penaltyFactor;
		settings.isLargeDisplacement = this.isLargeDisplacement;
		settings.isReorderNodes = this.isReorderNodes;
		settings.isWriteMatrixView = this.isWriteMatrixView;
		//settings.meshSize = this.meshSize;
		//settings.meshCount = this.meshCount;
		/*settings.isShiftForceVectors = this.isShiftForceVectors;*/
		settings.unitSystem = this.unitSystem;
		
		/*settings.isShowAxes = this.isShowAxes;
		settings.isShowGrid = this.isShowGrid;
		settings.isShowScale = this.isShowScale;
		settings.isShowOrientations = this.isShowOrientations;
		settings.isShowNodes = this.isShowNodes;
		settings.isShowEdges = this.isShowEdges;
		settings.isShowSections = this.isShowSections;
		settings.isShowSupports = this.isShowSupports;
		settings.isShowLoads = this.isShowLoads;*/
		/*settings.isShowReactions = this.isShowReactions;*/
		
		return settings;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Settings settings = (Settings) obj;
		if (constraintType != settings.constraintType) return Result.RECALC;
		if (penaltyFactor != settings.penaltyFactor) return Result.RECALC;
		if (isReorderNodes != settings.isReorderNodes) return Result.RECALC;
		if (isLargeDisplacement != settings.isLargeDisplacement) return Result.RECALC;
		if (isWriteMatrixView != settings.isWriteMatrixView) return Result.RECALC;
		if (unitSystem != settings.unitSystem) return Result.RECALC;
		/*if (result != Result.RECALC) {
			if (newPartType != settings.newPartType) result = Result.CHANGE;
			if (module != settings.module) result = Result.CHANGE;
			if (pressureAngle != settings.pressureAngle) result = Result.CHANGE;
			if (meshSize != settings.meshSize) result = Result.CHANGE;
			if (meshCount != settings.meshCount) result = Result.CHANGE;
			if (isShowAxes != settings.isShowAxes) result = Result.CHANGE;
			if (isShowGrid != settings.isShowGrid) result = Result.CHANGE;
			if (isShowScale != settings.isShowScale) result = Result.CHANGE;
			if (isShowOrientations != settings.isShowOrientations) result = Result.CHANGE;
			if (isShowNodes != settings.isShowNodes) result = Result.CHANGE;
			if (isShowEdges != settings.isShowEdges) result = Result.CHANGE;
			if (isShowSections != settings.isShowSections) result = Result.CHANGE;
			if (isShowSupports != settings.isShowSupports) result = Result.CHANGE;
			if (isShowLoads != settings.isShowLoads) result = Result.CHANGE;
		}*/
		return result;
	}

}

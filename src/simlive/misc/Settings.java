package simlive.misc;

import simlive.model.DeepEqualsInterface;
import simlive.model.Element;
import simlive.solution.ConstraintMethod;

public class Settings implements DeepEqualsInterface {

	public Element.Type newPartType;
	public double module;
	public double pressureAngle;
	public ConstraintMethod.Type constraintType;
	public double penaltyFactor;
	public boolean isReorderNodes;
	public boolean isLargeDisplacement;
	public boolean isWriteMatrixView;
	public double meshSize;
	public int meshCount;
	/*public boolean isShiftForceVectors;*/
	public Units.UnitSystem unitSystem;
	
	public boolean isShowAxes, isShowGrid, isShowScale;
	public boolean isShowOrientations, isShowNodes, isShowEdges, isShowSections, isShowSupports, isShowLoads/*, isShowReactions*/;
	
	public Settings() {
		newPartType = Element.Type.ROD;
		module = 10.0;
		pressureAngle = 20.0;
		constraintType = ConstraintMethod.Type.LAGRANGE_MULTIPLIERS;
		penaltyFactor = 1E8;
		isReorderNodes = true;
		isLargeDisplacement = false;
		isWriteMatrixView = false;
		/*isShiftForceVectors = false;*/
		meshSize = 100;
		meshCount = 20;
		unitSystem = Units.UnitSystem.t_mm_s_N;
		
		resetDisplayOptions();
	}
	
	public void resetDisplayOptions() {
		isShowAxes = isShowGrid = isShowScale = true;
		isShowOrientations = isShowNodes = isShowEdges = isShowSections = isShowSupports = isShowLoads = true;
	}
	
	public Settings clone() {
		Settings settings = new Settings();
		settings.newPartType = this.newPartType;
		settings.module = this.module;
		settings.pressureAngle = this.pressureAngle;
		settings.constraintType = this.constraintType;
		settings.penaltyFactor = this.penaltyFactor;
		settings.isLargeDisplacement = this.isLargeDisplacement;
		settings.isReorderNodes = this.isReorderNodes;
		settings.isWriteMatrixView = this.isWriteMatrixView;
		settings.meshSize = this.meshSize;
		settings.meshCount = this.meshCount;
		/*settings.isShiftForceVectors = this.isShiftForceVectors;*/
		settings.unitSystem = this.unitSystem;
		
		settings.isShowAxes = this.isShowAxes;
		settings.isShowGrid = this.isShowGrid;
		settings.isShowScale = this.isShowScale;
		settings.isShowOrientations = this.isShowOrientations;
		settings.isShowNodes = this.isShowNodes;
		settings.isShowEdges = this.isShowEdges;
		settings.isShowSections = this.isShowSections;
		settings.isShowSupports = this.isShowSupports;
		settings.isShowLoads = this.isShowLoads;
		/*settings.isShowReactions = this.isShowReactions;*/
		
		return settings;
	}
	
	public boolean deepEquals(Object obj) {
		Settings settings = (Settings) obj;
		/*here are only settings listed that invalidate the result if changed*/
		if (constraintType != settings.constraintType) return false;
		if (penaltyFactor != settings.penaltyFactor) return false;
		if (isReorderNodes != settings.isReorderNodes) return false;
		if (isLargeDisplacement != settings.isLargeDisplacement) return false;
		if (isWriteMatrixView != settings.isWriteMatrixView) return false;
		if (unitSystem != settings.unitSystem) return false;
		return true;
	}

}

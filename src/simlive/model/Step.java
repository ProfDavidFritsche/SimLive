package simlive.model;

import simlive.SimLive;
import simlive.misc.Units;

public class Step implements DeepEqualsInterface {

	public enum Type {MECHANICAL_STATIC, EXPLICIT_DYNAMIC, MODAL_ANALYSIS}
	public static String[] typeStrings = {"Mechanical Static", "Explicit Dynamic", "Modal Analysis"};
	public Type type;
	public double duration;
	public int nIncrements;
	public int maxIterations;
	public enum GRAVITY {NO_GRAVITY, X_DIR, Y_DIR, Z_DIR}
	public GRAVITY gravity;
	public double gValue;
	public String name;
	
	public Step() {
		type = Type.MECHANICAL_STATIC;
		duration = 1.0;
		nIncrements = 20;
		maxIterations = 100;
		gravity = GRAVITY.NO_GRAVITY;
		gValue = -9.81;
		Units.convertUnitsOfStep(Units.UnitSystem.kg_m_s_N, SimLive.model.settings.unitSystem, this);
		this.name = SimLive.model.getDefaultName("Step");
	}

	public Step clone() {
		Step step = new Step();
		step.type = this.type;
		step.duration = this.duration;
		step.nIncrements = this.nIncrements;
		step.maxIterations = this.maxIterations;
		step.gravity = this.gravity;
		step.gValue = this.gValue;
		step.name = this.name;
		return step;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Step step = (Step) obj;
		if (this.type != step.type) return Result.RECALC;
		if (this.duration != step.duration) return Result.RECALC;
		if (this.nIncrements != step.nIncrements) return Result.RECALC;
		if (this.maxIterations != step.maxIterations) return Result.RECALC;
		if (this.gravity != step.gravity) return Result.RECALC;
		if (this.gValue != step.gValue) return Result.RECALC;
		if (this.name != step.name && result != Result.RECALC) result = Result.CHANGE;
		return result;
	}

}

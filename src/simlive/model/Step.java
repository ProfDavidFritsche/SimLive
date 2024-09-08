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
		Units.convertUnitsOfStep(Units.UnitSystem.kg_m_s_N, SimLive.settings.unitSystem, this);
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
	
	public boolean deepEquals(Object obj) {
		Step step = (Step) obj;
		if (this.type != step.type) return false;
		if (this.duration != step.duration) return false;
		if (this.nIncrements != step.nIncrements) return false;
		if (this.maxIterations != step.maxIterations) return false;
		if (this.gravity != step.gravity) return false;
		if (this.gValue != step.gValue) return false;
		if (this.name != step.name) return false;
		return true;
	}

}

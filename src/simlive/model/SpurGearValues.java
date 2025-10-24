package simlive.model;

public class SpurGearValues implements DeepEqualsInterface {
	
	private double pressureAngle;
	private double module;
	private double pitchRadius;
	private boolean isInternal;

	public SpurGearValues(double pressureAngle, double module, double pitchRadius, boolean isInternal) {
		this.pressureAngle = pressureAngle;
		this.module = module;
		this.pitchRadius = pitchRadius;
		this.isInternal = isInternal;
	}
	
	public SpurGearValues clone(Model model) {
		SpurGearValues spurGearValues = new SpurGearValues(this.pressureAngle, this.module,
				this.pitchRadius, this.isInternal);
		return spurGearValues;
	}

	public Result deepEquals(Object obj, Result result) {
		SpurGearValues spurGearValues = (SpurGearValues) obj;
		if (this.pressureAngle != spurGearValues.pressureAngle) return Result.RECALC;
		if (this.module != spurGearValues.module) return Result.RECALC;
		if (this.pitchRadius != spurGearValues.pitchRadius) return Result.RECALC;
		if (this.isInternal != spurGearValues.isInternal) return Result.RECALC;
		return result;
	}

	public double getPressureAngle() {
		return pressureAngle;
	}

	public double getModule() {
		return module;
	}

	public double getPitchRadius() {
		return pitchRadius;
	}

	public boolean isInternal() {
		return isInternal;
	}

}

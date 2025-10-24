package simlive.model;

public class SectionShape implements DeepEqualsInterface {
	
	public enum Type {RECTANGLE, HOLLOW_RECTANGLE, CIRCLE, HOLLOW_CIRCLE, DIRECT_INPUT}
	
	private Type type;
	private double width;
	private double height;
	private double diameter;
	private double thickness;	

	public SectionShape(Type type) {
		this.type = type;
	}
	
	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getDiameter() {
		return diameter;
	}

	public void setDiameter(double diameter) {
		this.diameter = diameter;
	}

	public double getThickness() {
		return thickness;
	}

	public void setThickness(double thickness) {
		this.thickness = thickness;
	}

	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public double getArea() {
		if (type == Type.RECTANGLE) {
			return width * height;
		}
		if (type == Type.HOLLOW_RECTANGLE) {
			double innerWidth = width - 2.0 * thickness;
			double innerHeight = height - 2.0 * thickness;
			return width * height - innerWidth * innerHeight;
		}
		if (type == Type.CIRCLE) {
			return Math.PI * diameter * diameter / 4.0;
		}
		if (type == Type.HOLLOW_CIRCLE) {
			double innerDiameter = diameter - 2.0 * thickness;
			return Math.PI * (diameter * diameter - innerDiameter * innerDiameter) / 4.0;
		}
		return 0.0;
	}
	
	private double getIy(double width, double height) {
		if (type == Type.RECTANGLE) {
			return width * Math.pow(height, 3) / 12.0;
		}
		if (type == Type.HOLLOW_RECTANGLE) {
			double innerWidth = width - 2.0 * thickness;
			double innerHeight = height - 2.0 * thickness;
			return (width * Math.pow(height, 3) - innerWidth * Math.pow(innerHeight, 3)) / 12.0;
		}
		if (type == Type.CIRCLE) {
			return Math.PI * Math.pow(diameter, 4) / 64.0;
		}
		if (type == Type.HOLLOW_CIRCLE) {
			double innerDiameter = diameter - 2.0 * thickness;
			return Math.PI * (Math.pow(diameter, 4) - Math.pow(innerDiameter, 4)) / 64.0;
		}
		return 0.0;
	}
	
	public double getIy() {
		return getIy(width, height);
	}
	
	public double getIz() {
		return getIy(height, width);
	}
	
	public double getIt() {
		if (type == Type.RECTANGLE) {
			double a = Math.max(width, height)/2.0;
			double b = Math.min(width, height)/2.0;
			return a*b*b*b*(16.0/3.0-3.36*b/a*(1.0-b*b*b*b/(12.0*a*a*a*a)));
		}
		if (type == Type.HOLLOW_RECTANGLE) {
			double mediumWidth = width - thickness;
			double mediumHeight = height - thickness;
			double Am = mediumWidth * mediumHeight;
			return 4.0 * Am * Am / (2.0 * mediumWidth / thickness + 2.0 * mediumHeight / thickness);
		}
		if (type == Type.CIRCLE) {
			return Math.PI * Math.pow(diameter, 4) / 32.0;
		}
		if (type == Type.HOLLOW_CIRCLE) {
			double innerDiameter = diameter - 2.0 * thickness;
			return Math.PI * (Math.pow(diameter, 4) - Math.pow(innerDiameter, 4)) / 32.0;
		}
		return 0.0;
	}
	
	public SectionShape clone() {
		SectionShape sectionShape = new SectionShape(this.getType());
		sectionShape.width = this.width;
		sectionShape.height = this.height;
		sectionShape.diameter = this.diameter;
		sectionShape.thickness = this.thickness;
		return sectionShape;
	}
	
	public Result deepEquals(Object obj, Result result) {
		SectionShape sectionShape = (SectionShape) obj;
		if (this.type != sectionShape.type) return Result.RECALC;
		if (this.width != sectionShape.width) return Result.RECALC;
		if (this.height != sectionShape.height) return Result.RECALC;
		if (this.diameter != sectionShape.diameter) return Result.RECALC;
		if (this.thickness != sectionShape.thickness) return Result.RECALC;
		return result;
	}

}

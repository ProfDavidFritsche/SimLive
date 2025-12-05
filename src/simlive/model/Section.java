package simlive.model;

import simlive.SimLive;
import simlive.misc.Settings;
import simlive.misc.Units;
import simlive.model.SectionShape.Type;
import simlive.view.View;

public class Section implements DeepEqualsInterface {
	
	private double Area;
	private double Iy, Iz, It;
	private SectionShape sectionShape;
	
	public Section () {
		Section section = getDefaultSection();
		Units.convertUnitsOfSection(Units.UnitSystem.t_mm_s_N, SimLive.model.settings.unitSystem, section);
		this.sectionShape = section.getSectionShape();
		this.Area = section.getSectionShape().getArea();
		this.Iy = section.getSectionShape().getIy();
		this.Iz = section.getSectionShape().getIz();
		this.It = section.getSectionShape().getIt();
	}
	
	public Section (SectionShape sectionShape) {
		this.sectionShape = sectionShape;
	}
	
	public static Section getDefaultSection () {
		SectionShape rectangular = new SectionShape(SectionShape.Type.RECTANGLE);
		rectangular.setWidth(10.0);
		rectangular.setHeight(10.0);
		rectangular.setDiameter(10.0);
		rectangular.setThickness(1.0);
		
		Section defaultSection = new Section(rectangular);
		defaultSection.Area = rectangular.getArea();
		defaultSection.Iy = rectangular.getIy();
		defaultSection.Iz = rectangular.getIz();
		defaultSection.It = rectangular.getIt();
			
		return defaultSection;
	}
	
	public void setArea (double Area) {
		this.Area = Area;		
	}
	
	public double getArea () {
		updateData();
		return Area;
	}
	
	public void setIy (double Iy) {
		this.Iy = Iy;		
	}
	
	public double getIy () {
		updateData();
		return Iy;
	}
	
	public void setIz (double Iz) {
		this.Iz = Iz;		
	}
	
	public double getIz () {
		updateData();
		return Iz;
	}
	
	public void setIt (double It) {
		this.It = It;		
	}
	
	public double getIt () {
		updateData();
		return It;
	}
	
	private void updateData () {
		if (sectionShape.getType() != SectionShape.Type.DIRECT_INPUT) {
			Area = sectionShape.getArea();
			Iy = sectionShape.getIy();
			Iz = sectionShape.getIz();
			It = sectionShape.getIt();
		}
	}
	
	public SectionShape getSectionShape() {
		return sectionShape;
	}
	
	public double[][] getSectionPoints() {
		double[][] p = null;
		if (!Settings.isShowSections || sectionShape.getType() == Type.CIRCLE ||
				sectionShape.getType() == Type.HOLLOW_CIRCLE || sectionShape.getType() == Type.DIRECT_INPUT) {
			int[] viewport = View.getViewport();
			double r = !Settings.isShowSections || sectionShape.getType() == Type.DIRECT_INPUT ?
					SimLive.LINE_ELEMENT_RADIUS/viewport[2]/View.zoom : sectionShape.getDiameter()/2;
			int slices = SimLive.view.getCylindricSectionSlices(r);
			p = (Settings.isShowSections && sectionShape.getType() == Type.HOLLOW_CIRCLE) ? new double[slices*2+2][] :
				new double[slices+1][];
			for (int k = 0; k < slices; k++) {
				double phi = k*2*Math.PI/slices;
				p[k] = new double[]{0, r*Math.cos(phi), r*Math.sin(phi)};
			}
			p[slices] = p[0].clone();
			if (Settings.isShowSections && sectionShape.getType() == Type.HOLLOW_CIRCLE) {
				r -= sectionShape.getThickness();
				for (int k = slices+1; k < slices*2+1; k++) {
					double phi = (k-1)*2*Math.PI/slices;
					p[k] = new double[]{0, r*Math.cos(phi), -r*Math.sin(phi)};
				}
				p[slices*2+1] = p[slices+1].clone();
			}
		}
		else if (sectionShape.getType() == Type.RECTANGLE || sectionShape.getType() == Type.HOLLOW_RECTANGLE) {
			double w = sectionShape.getWidth()/2;
			double h = sectionShape.getHeight()/2;
			p = sectionShape.getType() == Type.HOLLOW_RECTANGLE ? new double[16][] : new double[8][];
			p[0] = new double[]{0, w, -h};
			p[1] = new double[]{0, w, h};
			p[2] = new double[]{0, w, h};
			p[3] = new double[]{0, -w, h};
			p[4] = new double[]{0, -w, h};
			p[5] = new double[]{0, -w, -h};
			p[6] = new double[]{0, -w, -h};
			p[7] = new double[]{0, w, -h};
			if (sectionShape.getType() == Type.HOLLOW_RECTANGLE) {
				w -= sectionShape.getThickness();
				h -= sectionShape.getThickness();
				p[8] = new double[]{0, w, -h};
				p[9] = new double[]{0, -w, -h};
				p[10] = new double[]{0, -w, -h};
				p[11] = new double[]{0, -w, h};
				p[12] = new double[]{0, -w, h};
				p[13] = new double[]{0, w, h};
				p[14] = new double[]{0, w, h};
				p[15] = new double[]{0, w, -h};
			}
		}
		return p;
	}
	
	public double[][] getSectionNormals() {
		double[][] n = null;
		if (!Settings.isShowSections || sectionShape.getType() == Type.CIRCLE ||
				sectionShape.getType() == Type.HOLLOW_CIRCLE || sectionShape.getType() == Type.DIRECT_INPUT) {
			int[] viewport = View.getViewport();
			double r = !Settings.isShowSections || sectionShape.getType() == Type.DIRECT_INPUT ?
					SimLive.LINE_ELEMENT_RADIUS/viewport[2]/View.zoom : sectionShape.getDiameter()/2;
			int slices = SimLive.view.getCylindricSectionSlices(r);
			n = (Settings.isShowSections && sectionShape.getType() == Type.HOLLOW_CIRCLE) ? new double[slices*2+2][] :
				new double[slices+1][];
			for (int k = 0; k < slices; k++) {
				double phi = k*2*Math.PI/slices;
				n[k] = new double[]{0, Math.cos(phi), Math.sin(phi)};
			}
			n[slices] = n[0].clone();
			if (Settings.isShowSections && sectionShape.getType() == Type.HOLLOW_CIRCLE) {
				for (int k = slices+1; k < slices*2+1; k++) {
					double phi = k*2*Math.PI/slices;
					n[k] = new double[]{0, -Math.cos(phi), Math.sin(phi)};
				}
				n[slices*2+1] = n[slices+1].clone();
			}
		}
		else if (sectionShape.getType() == Type.RECTANGLE || sectionShape.getType() == Type.HOLLOW_RECTANGLE) {
			n = sectionShape.getType() == Type.HOLLOW_RECTANGLE ? new double[16][] : new double[8][];
			n[0] = new double[]{0, 1, 0};
			n[1] = new double[]{0, 1, 0};
			n[2] = new double[]{0, 0, 1};
			n[3] = new double[]{0, 0, 1};
			n[4] = new double[]{0, -1, 0};
			n[5] = new double[]{0, -1, 0};
			n[6] = new double[]{0, 0, -1};
			n[7] = new double[]{0, 0, -1};
			if (sectionShape.getType() == Type.HOLLOW_RECTANGLE) {
				n[8] = new double[]{0, 0, 1};
				n[9] = new double[]{0, 0, 1};
				n[10] = new double[]{0, 1, 0};
				n[11] = new double[]{0, 1, 0};
				n[12] = new double[]{0, 0, -1};
				n[13] = new double[]{0, 0, -1};
				n[14] = new double[]{0, -1, 0};
				n[15] = new double[]{0, -1, 0};
			}
		}
		return n;
	}

	public String getName() {
		if (sectionShape.getType() == SectionShape.Type.RECTANGLE) {
			return "Rectangle " + SimLive.double2String(sectionShape.getWidth()) +
					"x" + SimLive.double2String(sectionShape.getHeight()) +
					" " + Units.getLengthUnit();
		}
		if (sectionShape.getType() == SectionShape.Type.HOLLOW_RECTANGLE) {
			return "Hollow Rectangle " + SimLive.double2String(sectionShape.getWidth()) +
					"x" + SimLive.double2String(sectionShape.getHeight()) +
					" t=" + SimLive.double2String(sectionShape.getThickness()) +
					" " + Units.getLengthUnit();
		}
		if (sectionShape.getType() == SectionShape.Type.CIRCLE) {
			return "Circle \u00d8" + SimLive.double2String(sectionShape.getDiameter()) +
					" " + Units.getLengthUnit();
		}
		if (sectionShape.getType() == SectionShape.Type.HOLLOW_CIRCLE) {
			return "Hollow Circle \u00d8" + SimLive.double2String(sectionShape.getDiameter()) +
					" t=" + SimLive.double2String(sectionShape.getThickness()) +
					" " + Units.getLengthUnit();
		}
		if (sectionShape.getType() == SectionShape.Type.DIRECT_INPUT) {
			return "Iy=" + SimLive.double2String(Iy) +
					" Iz=" + SimLive.double2String(Iz) +
					" It=" + SimLive.double2String(It) +
					" " + Units.getLengthUnit()+"\u2074";
		}
		return null;
	}

	public Section clone() {
		Section section = new Section(this.sectionShape.clone());
		section.Area = this.Area;
		section.Iy = this.Iy;
		section.Iz = this.Iz;
		section.It = this.It;
		return section;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Section section = (Section) obj;
		result = this.sectionShape.deepEquals(section.sectionShape, result);
		if (this.Area != section.Area) return Result.RECALC;
		if (this.Iy != section.Iy) return Result.RECALC;
		if (this.Iz != section.Iz) return Result.RECALC;
		if (this.It != section.It) return Result.RECALC;
		return result;
	}
}
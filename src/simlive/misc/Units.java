package simlive.misc;

import java.util.ArrayList;

import simlive.SimLive;
import simlive.model.Connector;
import simlive.model.ContactPair;
import simlive.model.DistributedLoad;
import simlive.model.Element;
import simlive.model.Load;
import simlive.model.Material;
import simlive.model.Node;
import simlive.model.Part3d;
import simlive.model.PlaneElement;
import simlive.model.PointMass;
import simlive.model.Section;
import simlive.model.SectionShape;
import simlive.model.Spring;
import simlive.model.Step;
import simlive.view.Measurement;
import simlive.view.Measurement.Type;

public class Units {
	
	private static double massFactor;
	private static double lengthFactor;
	private static double forceFactor;
	
	public static enum UnitSystem {t_mm_s_N, t_m_s_kN, kg_m_s_N}
	
	public static String getMassUnit() {
		switch (SimLive.model.settings.unitSystem) {
			case t_mm_s_N: return "t";
			case t_m_s_kN: return "t";
			case kg_m_s_N: return "kg";
		}
		return null;
	}
	
	public static String getLengthUnit() {
		switch (SimLive.model.settings.unitSystem) {
			case t_mm_s_N: return "mm";
			case t_m_s_kN: return "m";
			case kg_m_s_N: return "m";
		}
		return null;
	}
	
	public static String getTimeUnit() {
		switch (SimLive.model.settings.unitSystem) {
			case t_mm_s_N: return "s";
			case t_m_s_kN: return "s";
			case kg_m_s_N: return "s";
		}
		return null;
	}
	
	public static String getFrequencyUnit() {
		switch (SimLive.model.settings.unitSystem) {
			case t_mm_s_N: return "Hz";
			case t_m_s_kN: return "Hz";
			case kg_m_s_N: return "Hz";
		}
		return null;
	}
	
	public static String getForceUnit() {
		switch (SimLive.model.settings.unitSystem) {
			case t_mm_s_N: return "N";
			case t_m_s_kN: return "kN";
			case kg_m_s_N: return "N";
		}
		return null;
	}
	
	private static void setConversionFactors(UnitSystem oldUnits, UnitSystem newUnits) {
		massFactor = 1.0;
		lengthFactor = 1.0;
		forceFactor = 1.0;
		
		if (oldUnits == UnitSystem.t_mm_s_N && newUnits == UnitSystem.t_m_s_kN) {
			lengthFactor = 0.001;
			forceFactor = 0.001;
		}
		if (oldUnits == UnitSystem.t_m_s_kN && newUnits == UnitSystem.t_mm_s_N) {
			lengthFactor = 1000.0;
			forceFactor = 1000.0;
		}
		if (oldUnits == UnitSystem.t_mm_s_N && newUnits == UnitSystem.kg_m_s_N) {
			massFactor = 1000.0;
			lengthFactor = 0.001;
		}
		if (oldUnits == UnitSystem.kg_m_s_N && newUnits == UnitSystem.t_mm_s_N) {
			massFactor = 0.001;
			lengthFactor = 1000.0;
		}
		if (oldUnits == UnitSystem.t_m_s_kN && newUnits == UnitSystem.kg_m_s_N) {
			massFactor = 1000.0;
			forceFactor = 1000.0;
		}
		if (oldUnits == UnitSystem.kg_m_s_N && newUnits == UnitSystem.t_m_s_kN) {
			massFactor = 0.001;
			forceFactor = 0.001;
		}
	}
	
	public static void convertUnitsOfStep(UnitSystem oldUnits, UnitSystem newUnits, Step step) {
		setConversionFactors(oldUnits, newUnits);
		
		step.gValue *= lengthFactor;
	}
	
	public static void convertUnitsOfMaterial(UnitSystem oldUnits, UnitSystem newUnits,
			Material material) {
		setConversionFactors(oldUnits, newUnits);
		
		material.setDensity(material.getDensity()*massFactor/(lengthFactor*lengthFactor*lengthFactor));
		material.setYoungsModulus(material.getYoungsModulus()*forceFactor/(lengthFactor*lengthFactor));
	}
	
	public static void convertUnitsOfSection(UnitSystem oldUnits, UnitSystem newUnits,
			Section section) {
		setConversionFactors(oldUnits, newUnits);
		
		SectionShape shape = section.getSectionShape();
		shape.setDiameter(shape.getDiameter()*lengthFactor);
		shape.setHeight(shape.getHeight()*lengthFactor);
		shape.setThickness(shape.getThickness()*lengthFactor);
		shape.setWidth(shape.getWidth()*lengthFactor);
		if (shape.getType() == SectionShape.Type.DIRECT_INPUT) {
			section.setArea(section.getArea()*lengthFactor*lengthFactor);
			section.setIy(section.getIy()*lengthFactor*lengthFactor*lengthFactor*lengthFactor);
			section.setIz(section.getIz()*lengthFactor*lengthFactor*lengthFactor*lengthFactor);
			section.setIt(section.getIt()*lengthFactor*lengthFactor*lengthFactor*lengthFactor);
		}
		else {
			section.setArea(shape.getArea());
			section.setIy(shape.getIy());
			section.setIz(shape.getIz());
			section.setIt(shape.getIt());
		}
	}
	
	public static void convertUnitsOfElement(UnitSystem oldUnits, UnitSystem newUnits,
			Element element) {
		setConversionFactors(oldUnits, newUnits);
		
		if (element.getType() == Element.Type.POINT_MASS) {
			PointMass pointMass = (PointMass) element;
			pointMass.setMass(pointMass.getMass()*massFactor);
		}
		if (element.getType() == Element.Type.SPRING) {
			Spring spring = (Spring) element;
			spring.setStiffness(spring.getStiffness()*forceFactor/lengthFactor);
		}
		if (element.isPlaneElement()) {
			PlaneElement planeElement = (PlaneElement) element;
			planeElement.setThickness(planeElement.getThickness()*lengthFactor);
		}
	}
	
	public static void convertUnitsOfModel(UnitSystem oldUnits, UnitSystem newUnits) {
		setConversionFactors(oldUnits, newUnits);
		
		ArrayList<Node> nodes = SimLive.model.getNodes();
		ArrayList<Element> elements = SimLive.model.getElements();
		ArrayList<Part3d> parts3d = SimLive.model.getParts3d();
		ArrayList<Load> loads = SimLive.model.getLoads();
		ArrayList<DistributedLoad> distributedLoads = SimLive.model.getDistributedLoads();
		ArrayList<Connector> connectors = SimLive.model.getConnectors();
		ArrayList<ContactPair> contactPairs = SimLive.model.getContactPairs();
		ArrayList<Material> materials = SimLive.model.getMaterials();
		ArrayList<Section> sections = SimLive.model.getSections();
		ArrayList<Step> steps = SimLive.model.getSteps();
		
		for (int m = 0; m < SimLive.view.measurements.size(); m++) {
			Measurement measurement = SimLive.view.measurements.get(m);
			double[] startPoint = measurement.getStartPoint();
			startPoint[0] *= lengthFactor;
			startPoint[1] *= lengthFactor;
			startPoint[2] *= lengthFactor;
			if (measurement.getType() == Type.ANGLE) {
				double[] midPoint = measurement.getMidPoint();
				midPoint[0] *= lengthFactor;
				midPoint[1] *= lengthFactor;
				midPoint[2] *= lengthFactor;
			}
			double[] endPoint = measurement.getEndPoint();
			endPoint[0] *= lengthFactor;
			endPoint[1] *= lengthFactor;
			endPoint[2] *= lengthFactor;
			double[] move = measurement.getMove();
			move[0] *= lengthFactor;
			move[1] *= lengthFactor;
			move[2] *= lengthFactor;
			
			/* temporary change SimLive.settings.unitSystem */
			UnitSystem temp = SimLive.model.settings.unitSystem;
			SimLive.model.settings.unitSystem = newUnits;
			
			measurement.setEndPoint(endPoint, measurement.getLabel().length > 1);
			
			/* reset SimLive.settings.unitSystem */
			SimLive.model.settings.unitSystem = temp;
		}
		
		for (int n = 0; n < nodes.size(); n++) {
			Node node = nodes.get(n);
			node.setXCoord(node.getXCoord()*lengthFactor);
			node.setYCoord(node.getYCoord()*lengthFactor);
			node.setZCoord(node.getZCoord()*lengthFactor);
		}
		
		for (int p = 0; p < parts3d.size(); p++) {
			for (int v = 0; v < parts3d.get(p).getNrVertices(); v++) {
				double[] coords = parts3d.get(p).getVertex(v).getCoords();
				coords[0] *= lengthFactor;
				coords[1] *= lengthFactor;
				coords[2] *= lengthFactor;
				parts3d.get(p).getVertex(v).setCoords(coords);
			}
		}
		
		for (int e = 0; e < elements.size(); e++) {
			convertUnitsOfElement(oldUnits, newUnits, elements.get(e));
		}
		
		for (int l = 0; l < loads.size(); l++) {
			Load load = loads.get(l);
			if (load.getType() == Load.Type.FORCE) {
				load.setForce(load.getForce()[0]*forceFactor, 0);
				load.setForce(load.getForce()[1]*forceFactor, 1);
				load.setForce(load.getForce()[2]*forceFactor, 2);
				load.setMoment(load.getMoment()[0]*forceFactor*lengthFactor, 0);
				load.setMoment(load.getMoment()[1]*forceFactor*lengthFactor, 1);
				load.setMoment(load.getMoment()[2]*forceFactor*lengthFactor, 2);
			}
			if (load.getType() == Load.Type.DISPLACEMENT) {
				load.setDisp(load.getDisp()[0]*lengthFactor, 0);
				load.setDisp(load.getDisp()[1]*lengthFactor, 1);
				load.setDisp(load.getDisp()[2]*lengthFactor, 2);
			}
		}
		
		for (int d = 0; d < distributedLoads.size(); d++) {
			DistributedLoad load = distributedLoads.get(d);
			for (int i = 0; i < 3; i++) {
				load.setStartValue(i, load.getStartValue(i)*forceFactor/lengthFactor);
				load.setEndValue(i, load.getEndValue(i)*forceFactor/lengthFactor);
			}
		}
		
		for (int c = 0; c < connectors.size(); c++) {
			Connector connector = connectors.get(c);
			double[] coords = connector.getCoordinates();
			coords[0] *= lengthFactor;
			coords[1] *= lengthFactor;
			coords[2] *= lengthFactor;
			connector.setCoordinates(coords, true);
		}
		
		for (int c = 0; c < contactPairs.size(); c++) {
			ContactPair contactPair = contactPairs.get(c);
			double maxPenetration = contactPair.getMaxPenetration();
			maxPenetration *= lengthFactor;
			contactPair.setMaxPenetration(maxPenetration);
			if (contactPair.getType() == ContactPair.Type.RIGID_DEFORMABLE) {
				for (int n = 0; n < contactPair.getRigidNodes().size(); n++) {
					Node node = contactPair.getRigidNodes().get(n);
					node.setXCoord(node.getXCoord()*lengthFactor);
					node.setYCoord(node.getYCoord()*lengthFactor);
					node.setZCoord(node.getZCoord()*lengthFactor);
				}
				for (int e = 0; e < contactPair.getRigidElements().size(); e++) {
					PlaneElement planeElement = (PlaneElement) contactPair.getRigidElements().get(e);
					planeElement.setThickness(planeElement.getThickness()*lengthFactor);
				}
			}
		}
		
		for (int m = 0; m < materials.size(); m++) {
			convertUnitsOfMaterial(oldUnits, newUnits, materials.get(m));
		}
		
		for (int s = 0; s < sections.size(); s++) {
			convertUnitsOfSection(oldUnits, newUnits, sections.get(s));
		}
		
		for (int s = 0; s < steps.size(); s++) {
			convertUnitsOfStep(oldUnits, newUnits, steps.get(s));
		}
		
		SimLive.model.settings.module *= lengthFactor;
		SimLive.model.settings.meshSize *= lengthFactor;
		SimLive.view.convertUnitsOfViewData(lengthFactor);
	}

}

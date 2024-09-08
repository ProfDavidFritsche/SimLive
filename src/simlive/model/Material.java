package simlive.model;

import java.util.ArrayList;
import java.util.Arrays;

import simlive.SimLive;
import simlive.misc.Units;

public class Material implements DeepEqualsInterface {

	private double density;
	private double youngsModulus;
	private double poissonsRatio;
	private double[] lameConstants;
	public String name;
	
	public Material (boolean empty) {
		if (!empty) {
			Material material = getDefaultMaterials().get(0);
			Units.convertUnitsOfMaterial(Units.UnitSystem.t_mm_s_N, SimLive.settings.unitSystem, material);
			this.setDensity(material.getDensity());
			this.setYoungsModulus(material.getYoungsModulus());
			this.setPoissonsRatio(material.getPoissonsRatio());
			this.name = SimLive.model.getDefaultName("Material");
		}
	}
	
	public static ArrayList<Material> getDefaultMaterials () {
		ArrayList<Material> materials = new ArrayList<Material>();
		
		Material steel = new Material(true);
		steel.name = "Steel";
		steel.setDensity(0.00000000785);
		steel.setYoungsModulus(200000.0);
		steel.setPoissonsRatio(0.3);
		materials.add(steel);
		
		Material castIron = new Material(true);
		castIron.name = "Cast Iron";
		castIron.setDensity(0.0000000072);
		castIron.setYoungsModulus(100000.0);
		castIron.setPoissonsRatio(0.25);
		materials.add(castIron);
		
		Material aluminium = new Material(true);
		aluminium.name = "Aluminium";
		aluminium.setDensity(0.0000000027);
		aluminium.setYoungsModulus(70000.0);
		aluminium.setPoissonsRatio(0.35);
		materials.add(aluminium);
		
		Material polymer = new Material(true);
		polymer.name = "Elastic Polymer";
		polymer.setDensity(0.0000000013);
		polymer.setYoungsModulus(3000.0);
		polymer.setPoissonsRatio(0.4);
		materials.add(polymer);
		
		return materials;
	}
	
	public double getDensity() {
		return density;
	}

	public void setDensity(double density) {
		this.density = density;
	}

	public void setYoungsModulus (double youngsModulus) {
		this.youngsModulus = youngsModulus;	
		this.updateLameConstants();
	}
	
	public double getYoungsModulus () {
		return youngsModulus;		
	}
	
	public void setPoissonsRatio (double poissonsRatio) {
		this.poissonsRatio = poissonsRatio;
		this.updateLameConstants();
	}
	
	public double getPoissonsRatio () {
		return poissonsRatio;		
	}

	public double[] getLameConstants() {
		return lameConstants;
	}

	private void updateLameConstants() {
		lameConstants = new double[2];
		lameConstants[0] = poissonsRatio/(1.0-2.0*poissonsRatio)*youngsModulus/(1.0+poissonsRatio);
		lameConstants[1] = youngsModulus/(2.0+2.0*poissonsRatio);
	}

	public Material clone() {
		Material material = new Material(true);
		material.name = this.name;
		material.density = this.density;
		material.youngsModulus = this.youngsModulus;
		material.poissonsRatio = this.poissonsRatio;
		material.lameConstants = this.lameConstants;
		return material;
	}
	
	public boolean deepEquals(Object obj) {
		Material material = (Material) obj;
		if (this.name != material.name) return false;
		if (this.density != material.density) return false;
		if (this.youngsModulus != material.youngsModulus) return false;
		if (this.poissonsRatio != material.poissonsRatio) return false;
		if (!Arrays.equals(this.lameConstants, material.lameConstants)) return false;
		return true;
	}
}

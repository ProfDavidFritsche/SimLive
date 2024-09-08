package simlive.model;

import java.util.Arrays;
import java.util.List;

import de.javagl.obj.Mtl;

public class Part3dColor implements DeepEqualsInterface {
	
	private float[] kd;
	private float[] ks;
	private float shininess;	
	
	public Part3dColor() {
	}

	public float[] getKd() {
		return kd;
	}

	public void setKd(float r, float g, float b) {
		this.kd = new float[]{r, g, b};
	}

	public float[] getKs() {
		return ks;
	}

	public void setKs(float r, float g, float b) {
		this.ks = new float[]{r, g, b};
	}

	public float getShininess() {
		return shininess;
	}

	public void setShininess(float shininess) {
		this.shininess = shininess;
	}

	public Part3dColor clone() {
		Part3dColor part3dColor = new Part3dColor();
		part3dColor.kd = this.kd.clone();
		part3dColor.ks = this.ks.clone();
		part3dColor.shininess = this.shininess;
		return part3dColor;
	}

	public boolean deepEquals(Object obj) {
		Part3dColor part3dColor = (Part3dColor) obj;
		if (!Arrays.equals(this.kd, part3dColor.kd)) return false;
		if (!Arrays.equals(this.ks, part3dColor.ks)) return false;
		if (this.shininess != part3dColor.shininess) return false;
		return true;
	}
	
	static public Part3dColor getDefaultColor() {
		Part3dColor part3dColor = new Part3dColor();
		part3dColor.kd = new float[]{0.5f, 0.5f, 0.5f};
		part3dColor.ks = new float[]{0.5f, 0.5f, 0.5f};
		part3dColor.shininess = 100f;
		return part3dColor;
	}
	
	static public int getIndexOfColor(String name, List<Mtl> mtls, int offset) {
		if (mtls != null) {
			for (int i = 0; i < mtls.size(); i++) {
				if (mtls.get(i).getName().equals(name)) {
					return i+offset;
				}
			}
		}
		return 0;
	}

}

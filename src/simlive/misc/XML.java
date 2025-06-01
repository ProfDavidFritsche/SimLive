package simlive.misc;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.model.Beam;
import simlive.model.Connector;
import simlive.model.Connector3d;
import simlive.model.ContactPair;
import simlive.model.DistributedLoad;
import simlive.model.Element;
import simlive.model.Facet3d;
import simlive.model.LineElement;
import simlive.model.Load;
import simlive.model.Material;
import simlive.model.Model;
import simlive.model.Node;
import simlive.model.Part3d;
import simlive.model.Part3dColor;
import simlive.model.PlaneElement;
import simlive.model.PointMass;
import simlive.model.Quad;
import simlive.model.Section;
import simlive.model.SectionShape;
import simlive.model.Set;
import simlive.model.Spring;
import simlive.model.SpurGearValues;
import simlive.model.Step;
import simlive.model.SubTree;
import simlive.model.Support;
import simlive.model.TimeTable;
import simlive.model.Tri;
import simlive.model.Rod;
import simlive.model.Vertex3d;
import simlive.model.ContactPair.Type;
import simlive.solution.ConstraintMethod;
import simlive.solution.Increment;
import simlive.solution.Solution;

public class XML {
	
	private static String filePath = null;

	private static Load getLoad(String name) {
		for (int l = 0; l < SimLive.model.getLoads().size(); l++) {
			if (SimLive.model.getLoads().get(l).name.equals(name)) {
				return SimLive.model.getLoads().get(l);
			}
		}
		System.out.println("No load \""+name+"\" found.");
		return null;
	}
	
	private static double[] inverseKinematics(double x, double y, double z, double l1, double l2,
			double shift, boolean isM1) {
		if (isM1) {
			double c = Math.sqrt(x*x+y*y);
			double d = (l1*l1+c*c-l2*l2)/(2*c);
			double alpha = Math.atan2(y, x);
			return new double[]{z, Math.acos(d/l1)+alpha, -Math.acos((c-d)/l2)+alpha};
		}
		else {
			double r = Math.sqrt(x*x+y*y)-shift;
			double c = Math.sqrt(r*r+z*z);
			double d = (l1*l1+c*c-l2*l2)/(2*c);
			double alpha = Math.atan(z/r);
			return new double[]{Math.atan2(y, x), Math.acos(d/l1)+alpha, Math.acos((c-d)/l2)-alpha};
		}
	}
	
	private static double[] forwardKinematics(double phi0, double phi1, double phi2, double l1, double l2,
			double shift, boolean isM1) {
		if (isM1) {
			double x = l1*Math.cos(phi1)+l2*Math.cos(phi2);
			double y = l1*Math.sin(phi1)+l2*Math.sin(phi2);
			return new double[]{x, y, phi0};
		}
		else {
			double r = l1*Math.cos(phi1)+l2*Math.cos(phi2)+shift;
			double z = l1*Math.sin(phi1)-l2*Math.sin(phi2);
			return new double[]{r*Math.cos(phi0), r*Math.sin(phi0), z};
		}
	}
	
	public static boolean importControlToModel(String pathName, String fileName) {
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(pathName+fileName);
		
		try {
			String fileNameScript = pathName.concat(System.getProperty("file.separator")+"dobot.script");
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileNameScript));
			SimLive.model.getSteps().clear();
			
			Document document = (Document) builder.build(xmlFile);
			org.jdom.Element XMLroot = document.getRootElement();
			org.jdom.Element XMLcontrol = XMLroot.getChild("control");
			org.jdom.Element XMLconfig = XMLroot.getChild("config");
			Object[] list = XMLcontrol.getChildren().toArray();
			
			for (int i = 0; i < list.length; i++) {
				org.jdom.Element XMLcommand = (org.jdom.Element) list[i];
				String name = XMLcommand.getAttributeValue("name");
				Step step = new Step();
				step.name = name;
				step.nIncrements = getIntegerAttribute(XMLcommand, "nInc");
				step.gravity = Step.GRAVITY.valueOf(getStringAttribute(XMLconfig, "gravity"));
				step.gValue = getDoubleAttribute(XMLconfig, "gValue");
				SimLive.model.getSteps().add(step);
			}
			
			Load xDisp = getLoad(XMLconfig.getAttributeValue("x"));
			Load yDisp = getLoad(XMLconfig.getAttributeValue("y"));
			Load zDisp = getLoad(XMLconfig.getAttributeValue("z"));
			Load rotation = getLoad(XMLconfig.getAttributeValue("r"));
			
			xDisp.setTimeTable(new TimeTable(new double[1], new double[1]));
			yDisp.setTimeTable(new TimeTable(new double[1], new double[1]));
			zDisp.setTimeTable(new TimeTable(new double[1], new double[1]));
			rotation.setTimeTable(new TimeTable(new double[1], new double[1]));
			
			Load switchOrient = null;
			String curOrient = null;
			double l1, l2, shift = 0, zShift = 0;
			Node baseNode = SimLive.model.getNodes().get(getIntegerAttribute(XMLconfig, "baseNodeID"));
			boolean isM1 = XMLconfig.getAttributeValue("switchOrient") != null;
			double[] j1 = null, j2 = null, j3 = null, j4 = null;
			for (int c = 0; c < SimLive.model.getConnectors().size(); c++) {
				Connector connector = SimLive.model.getConnectors().get(c);
				if (XMLconfig.getAttributeValue("j1").equals(connector.name)) j1 = connector.getCoordinates();
				if (XMLconfig.getAttributeValue("j2").equals(connector.name)) j2 = connector.getCoordinates();
				if (XMLconfig.getAttributeValue("j3").equals(connector.name)) j3 = connector.getCoordinates();
				if (!isM1) {
					if (XMLconfig.getAttributeValue("j4").equals(connector.name)) j4 = connector.getCoordinates();
				}
			}
			l1 = Math.sqrt((j2[0]-j1[0])*(j2[0]-j1[0])+(j2[1]-j1[1])*(j2[1]-j1[1])+(j2[2]-j1[2])*(j2[2]-j1[2]));
			l2 = Math.sqrt((j3[0]-j2[0])*(j3[0]-j2[0])+(j3[1]-j2[1])*(j3[1]-j2[1])+(j3[2]-j2[2])*(j3[2]-j2[2]));
			if (isM1) {
				switchOrient = getLoad(XMLconfig.getAttributeValue("switchOrient"));
				switchOrient.setTimeTable(new TimeTable(new double[1], new double[1]));
				Matrix d1 = new Matrix(j2, 3).minus(new Matrix(j1, 3));
				Matrix d2 = new Matrix(j3, 3).minus(new Matrix(j2, 3));
				curOrient = d1.crossProduct(d2).get(2, 0) < 0.0 ? "right" : "left";
			}
			else {
				shift = Math.sqrt((j1[0]-baseNode.getXCoord())*(j1[0]-baseNode.getXCoord())+
						(j1[1]-baseNode.getYCoord())*(j1[1]-baseNode.getYCoord()))+
						Math.sqrt((j4[0]-j3[0])*(j4[0]-j3[0])+(j4[1]-j3[1])*(j4[1]-j3[1]));
				zShift = j1[2]-baseNode.getZCoord()+j4[2]-j3[2];
			}
			
			//drawing application
			if (XMLconfig.getAttributeValue("nodeID") != null &&
				XMLconfig.getAttributeValue("zCoord") != null &&
				XMLconfig.getAttributeValue("zDisp") != null) {
				
				SimLive.view.nodeID = getIntegerAttribute(XMLconfig, "nodeID");
				SimLive.view.zCoord = getDoubleAttribute(XMLconfig, "zCoord");
				SimLive.view.zDisp = getDoubleAttribute(XMLconfig, "zDisp");
			}
			
			Matrix p = new Matrix(xDisp.getNodes().get(0).getCoords(), 3).minus(new Matrix(baseNode.getCoords(), 3));
			p = GeomUtility.getRotationMatrixZ(-xDisp.getAngle()*Math.PI/180.0).times(p);
			double[] iniPos = new double[4];
			iniPos[0] = p.get(0, 0);
			iniPos[1] = p.get(1, 0);
			iniPos[2] = p.get(2, 0);
			double[] curPos = iniPos.clone();
			if (isM1) {
				writer.write("#Dobot M1 Script\n");
				writer.write("dType.SetPTPCmdEx(api, 1, "+iniPos[0]+", "+iniPos[1]+", "+iniPos[2]+", "+iniPos[3]+", 1)\n");
				if (curOrient.equals("left")) {
					writer.write("dType.SetArmOrientationEx(api, 0, 1)\n");
				}
				else {
					writer.write("dType.SetArmOrientationEx(api, 1, 1)\n");
				}
			}
			else {
				writer.write("--Dobot MG400 Script\n");
				writer.write("\tlocal Option = {CP = 1, SpeedJ = 50, AccJ = 20}\n");
				writer.write("\tlocal P = {coordinate = {"+iniPos[0]+", "+iniPos[1]+", "+iniPos[2]+", "+iniPos[3]+"}, tool = 0, user = 0}\n");
				writer.write("MovJ(P, Option)\n");
			}
			
			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLcommand = (org.jdom.Element) list[i];
				switch (XMLcommand.getName()) {
					case "movl":
					{
						double x = Double.parseDouble(XMLcommand.getAttributeValue("x"));
						double y = Double.parseDouble(XMLcommand.getAttributeValue("y"));
						double z = Double.parseDouble(XMLcommand.getAttributeValue("z"));
						double r = Double.parseDouble(XMLcommand.getAttributeValue("r"));
						TimeTable timeTable = xDisp.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(x-iniPos[0], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						timeTable = yDisp.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(y-iniPos[1], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						timeTable = zDisp.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(z-iniPos[2], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						timeTable = rotation.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(r-iniPos[3], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						curPos = new double[]{x, y, z, r};
						if (isM1) {
							writer.write("dType.SetPTPCmdEx(api, 2, "+x+", "+y+", "+z+", "+r+", 1)\n");
						}
						else {
							writer.write("\tlocal Option = {CP = 1, SpeedL = 50, AccL = 20}\n");
							writer.write("\tlocal P = {coordinate = {"+x+", "+y+", "+z+", "+r+"}, tool = 0, user = 0}\n");
							writer.write("MovL(P, Option)\n");
						}
						break;
					}
					case "movj":
					{
						int nInc = SimLive.model.getSteps().get(i).nIncrements;
						double x = Double.parseDouble(XMLcommand.getAttributeValue("x"));
						double y = Double.parseDouble(XMLcommand.getAttributeValue("y"));
						double z = Double.parseDouble(XMLcommand.getAttributeValue("z"));
						double r = Double.parseDouble(XMLcommand.getAttributeValue("r"));
						Matrix startAngles = new Matrix(inverseKinematics(curPos[0], curPos[1], curPos[2]-zShift, l1, l2, shift, isM1), 3);
						Matrix endAngles = new Matrix(inverseKinematics(x, y, z-zShift, l1, l2, shift, isM1), 3);
						TimeTable timeTableX = xDisp.getTimeTable();
						TimeTable timeTableY = yDisp.getTimeTable();
						TimeTable timeTableZ = zDisp.getTimeTable();
						timeTableX.setNumberOfRows(timeTableX.getNumberOfRows()+nInc);
						timeTableY.setNumberOfRows(timeTableY.getNumberOfRows()+nInc);
						timeTableZ.setNumberOfRows(timeTableZ.getNumberOfRows()+nInc);
						for (int n = 0; n <= nInc; n++) {
							Matrix angles = startAngles.plus(endAngles.minus(startAngles).times((double) n/nInc));
							double[] pos = forwardKinematics(angles.get(0, 0), angles.get(1, 0), angles.get(2, 0), l1, l2, shift, isM1);
							timeTableX.setFactor(pos[0]-iniPos[0], timeTableX.getNumberOfRows()-nInc+n-1);
							timeTableX.setTime(i+n/(double) nInc, timeTableX.getNumberOfRows()-nInc+n-1);
							timeTableY.setFactor(pos[1]-iniPos[1], timeTableY.getNumberOfRows()-nInc+n-1);
							timeTableY.setTime(i+n/(double) nInc, timeTableY.getNumberOfRows()-nInc+n-1);
							timeTableZ.setFactor(pos[2]+zShift-iniPos[2], timeTableZ.getNumberOfRows()-nInc+n-1);
							timeTableZ.setTime(i+n/(double) nInc, timeTableZ.getNumberOfRows()-nInc+n-1);
						}
						TimeTable timeTableR = rotation.getTimeTable();
						timeTableR.setNumberOfRows(timeTableR.getNumberOfRows()+1);
						timeTableR.setFactor(r-iniPos[3], timeTableR.getNumberOfRows()-1);
						timeTableR.setTime(i+1, timeTableR.getNumberOfRows()-1);
						curPos = new double[]{x, y, z, r};
						if (isM1) {
							writer.write("dType.SetPTPCmdEx(api, 1, "+x+", "+y+", "+z+", "+r+", 1)\n");
						}
						else {
							writer.write("\tlocal Option = {CP = 1, SpeedJ = 50, AccJ = 20}\n");
							writer.write("\tlocal P = {coordinate = {"+x+", "+y+", "+z+", "+r+"}, tool = 0, user = 0}\n");
							writer.write("MovJ(P, Option)\n");
						}
						break;
					}
					case "arc":
					{
						int nInc = SimLive.model.getSteps().get(i).nIncrements;
						double x1 = Double.parseDouble(XMLcommand.getAttributeValue("x1"));
						double y1 = Double.parseDouble(XMLcommand.getAttributeValue("y1"));
						double z1 = Double.parseDouble(XMLcommand.getAttributeValue("z1"));
						double r1 = Double.parseDouble(XMLcommand.getAttributeValue("r1"));
						double x2 = Double.parseDouble(XMLcommand.getAttributeValue("x2"));
						double y2 = Double.parseDouble(XMLcommand.getAttributeValue("y2"));
						double z2 = Double.parseDouble(XMLcommand.getAttributeValue("z2"));
						double r2 = Double.parseDouble(XMLcommand.getAttributeValue("r2"));
						double[] diff = new double[2];
						diff[0] = x2-curPos[0];
						diff[1] = y2-curPos[1];
						double[] p0 = new double[2];
						p0[0] = curPos[0]+diff[0]/2.0;
						p0[1] = curPos[1]+diff[1]/2.0;
						double[] p1 = new double[2];
						p1[0] = p0[0]-diff[1];
						p1[1] = p0[1]+diff[0];
						diff[0] = x2-x1;
						diff[1] = y2-y1;
						double[] q0 = new double[2];
						q0[0] = x1+diff[0]/2.0;
						q0[1] = y1+diff[1]/2.0;
						double[] q1 = new double[2];
						q1[0] = q0[0]-diff[1];
						q1[1] = q0[1]+diff[0];
						double[] m = GeomUtility.intersect(p0, p1, q0, q1);
						double[] diff1 = new double[2];
						diff1[0] = curPos[0]-m[0];
						diff1[1] = curPos[1]-m[1];
						double[] diff2 = new double[2];
						diff2[0] = x1-m[0];
						diff2[1] = y1-m[1];
						double[] diff3 = new double[2];
						diff3[0] = x2-m[0];
						diff3[1] = y2-m[1];
						double r = Math.sqrt(diff1[0]*diff1[0]+diff1[1]*diff1[1]);
						double sign = 0.0;
						if (Math.abs(diff1[0]*diff2[1]-diff1[1]*diff2[0]) > 0.0) {
							sign = Math.signum(diff1[0]*diff2[1]-diff1[1]*diff2[0]);
						}
						else {
							sign = Math.signum(diff2[0]*diff3[1]-diff2[1]*diff3[0]);
						}
						double angle = Math.acos((diff1[0]*diff3[0]+diff1[1]*diff3[1])/(r*r));
						if (sign*(diff1[0]*diff3[1]-diff1[1]*diff3[0]) < 0.0) {
							angle = 2.0*Math.PI-angle;
						}
						angle = sign*angle/nInc;
						TimeTable timeTable = xDisp.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+nInc);
						for (int n = 0; n <= nInc; n++) {
							double x = Math.cos(angle*n)*(curPos[0]-m[0])-Math.sin(angle*n)*(curPos[1]-m[1])+m[0];
							timeTable.setFactor(x-iniPos[0], timeTable.getNumberOfRows()-nInc+n-1);
							timeTable.setTime(i+n/(double) nInc, timeTable.getNumberOfRows()-nInc+n-1);
						}
						timeTable = yDisp.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+nInc);
						for (int n = 0; n <= nInc; n++) {
							double y = Math.sin(angle*n)*(curPos[0]-m[0])+Math.cos(angle*n)*(curPos[1]-m[1])+m[1];
							timeTable.setFactor(y-iniPos[1], timeTable.getNumberOfRows()-nInc+n-1);
							timeTable.setTime(i+n/(double) nInc, timeTable.getNumberOfRows()-nInc+n-1);
						}
						timeTable = zDisp.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(z2-iniPos[2], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						timeTable = rotation.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(r2-iniPos[3], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						curPos = new double[]{x2, y2, z2, r2};
						if (isM1) {
							writer.write("dType.SetARCCmdEx(api, ["+x1+", "+y1+", "+z1+", "+r1+"], ["+x2+", "+y2+", "+z2+", "+r2+"])\n");
						}
						else {
							writer.write("\tlocal Option = {CP = 1, SpeedL = 50, AccL = 20}\n");
							writer.write("\tlocal P1 = {coordinate = {"+x1+", "+y1+", "+z1+", "+r1+"}, tool = 0, user = 0}\n");
							writer.write("\tlocal P2 = {coordinate = {"+x2+", "+y2+", "+z2+", "+r2+"}, tool = 0, user = 0}\n");
							writer.write("Arc(P1, P2, Option)\n");
						}
						break;
					}
					case "switchOrient":
					{
						double switchMoment = getDoubleAttribute(XMLconfig, "switchMoment");
						double length = Math.sqrt(curPos[0]*curPos[0]+curPos[1]*curPos[1]);
						for (int n = 1; n <= 2; n++) {
							if (n > 1) {
								length = l1+l2;
							}
							TimeTable timeTable = xDisp.getTimeTable();
							timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
							timeTable.setFactor(curPos[0]*(l1+l2)/length-iniPos[0], timeTable.getNumberOfRows()-1);
							timeTable.setTime(i+n/2.0, timeTable.getNumberOfRows()-1);
							timeTable = yDisp.getTimeTable();
							timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
							timeTable.setFactor(curPos[1]*(l1+l2)/length-iniPos[1], timeTable.getNumberOfRows()-1);
							timeTable.setTime(i+n/2.0, timeTable.getNumberOfRows()-1);
						}
						TimeTable timeTable = zDisp.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(curPos[2]-iniPos[2], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						timeTable = rotation.getTimeTable();
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(curPos[3]-iniPos[3], timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1, timeTable.getNumberOfRows()-1);
						timeTable = switchOrient.getTimeTable();
						if (timeTable.getTime(timeTable.getNumberOfRows()-1) < i) {
							timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
							timeTable.setFactor(0, timeTable.getNumberOfRows()-1);
							timeTable.setTime(i, timeTable.getNumberOfRows()-1);
						}
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						if (curOrient.equals("left")) {
							curOrient = "right";
							timeTable.setFactor(-switchMoment, timeTable.getNumberOfRows()-1);
							writer.write("dType.SetArmOrientationEx(api, 1, 1)\n");
						}
						else {
							curOrient = "left";
							timeTable.setFactor(switchMoment, timeTable.getNumberOfRows()-1);
							writer.write("dType.SetArmOrientationEx(api, 0, 1)\n");
						}
						timeTable.setTime(i+0.5, timeTable.getNumberOfRows()-1);
						timeTable.setNumberOfRows(timeTable.getNumberOfRows()+1);
						timeTable.setFactor(0, timeTable.getNumberOfRows()-1);
						timeTable.setTime(i+1.0, timeTable.getNumberOfRows()-1);
					}
				}
			}
			writer.close();
			
			return true;
		}
		catch (NullPointerException npex) {
			System.out.println(npex.getMessage());
		}
		catch (NumberFormatException nfex) {
			System.out.println(nfex.getMessage());
		}
		catch (IOException io) {
			System.out.println(io.getMessage());
		}
		catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
		catch (Exception e) {
		}
		return false;
	}
	
	public static boolean readFileAndGenerateModel(String fileName) {
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(fileName);
		
		try {

			SimLive.model = new Model();
			
			Document document = (Document) builder.build(xmlFile);
			org.jdom.Element XMLroot = document.getRootElement();
			org.jdom.Element XMLmodel = XMLroot.getChild("model");
			Model.twoDimensional = getBooleanAttribute(XMLmodel, "twoDimensional");
			
			/* materials */
			org.jdom.Element XMLmaterials = XMLmodel.getChild("materials");
			Object[] list = XMLmaterials.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLmaterial = (org.jdom.Element) list[i];
				Material material = new Material(true);
				material.name = getStringAttribute(XMLmaterial, "name");
				material.setDensity(getDoubleAttribute(XMLmaterial, "density"));
				material.setYoungsModulus(getDoubleAttribute(XMLmaterial, "youngsModulus"));
				material.setPoissonsRatio(getDoubleAttribute(XMLmaterial, "poissonsRatio"));
				SimLive.model.getMaterials().add(material);
			}
			
			/* sections */
			org.jdom.Element XMLsections = XMLmodel.getChild("sections");
			list = XMLsections.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLsection = (org.jdom.Element) list[i];
				org.jdom.Element XMLshape = XMLsection.getChild("shape");
				SectionShape sectionShape = new SectionShape(SectionShape.Type.valueOf(getStringAttribute(XMLshape, "type")));
				sectionShape.setWidth(getDoubleAttribute(XMLshape, "width"));
				sectionShape.setHeight(getDoubleAttribute(XMLshape, "height"));
				sectionShape.setDiameter(getDoubleAttribute(XMLshape, "diameter"));
				sectionShape.setThickness(getDoubleAttribute(XMLshape, "thickness"));
				Section section = new Section(sectionShape);
				section.setArea(getDoubleAttribute(XMLsection, "area"));
				section.setIy(getDoubleAttribute(XMLsection, "Iy"));
				section.setIz(getDoubleAttribute(XMLsection, "Iz"));
				section.setIt(getDoubleAttribute(XMLsection, "It"));
				SimLive.model.getSections().add(section);
			}
			
			/* nodes */
			org.jdom.Element XMLnodes = XMLmodel.getChild("nodes");
			list = XMLnodes.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLnode = (org.jdom.Element) list[i];
				Node node = new Node(getDoubleAttribute(XMLnode, "xCoord"),
						getDoubleAttribute(XMLnode, "yCoord"), getDoubleAttribute(XMLnode, "zCoord"));
				SimLive.model.getNodes().add(node);
			}
			
			/* elements */
			org.jdom.Element XMLelements = XMLmodel.getChild("elements");
			list = XMLelements.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLelement = (org.jdom.Element) list[i];
				Element element = null;
				if (getStringAttribute(XMLelement, "type").equals(Element.Type.POINT_MASS.toString())) {
					int[] nodes = new int[1];
					nodes[0] = getIntegerAttribute(XMLelement, "node0");
					element = new PointMass(nodes);
					((PointMass) element).setMass(getDoubleAttribute(XMLelement, "mass"));
				}
				if (getStringAttribute(XMLelement, "type").equals(Element.Type.ROD.toString())) {
					int[] nodes = new int[2];
					nodes[0] = getIntegerAttribute(XMLelement, "node0");
					nodes[1] = getIntegerAttribute(XMLelement, "node1");
					element = new Rod(nodes);
					element.setMaterial(SimLive.model.getMaterials().get(getIntegerAttribute(XMLelement, "materialID")));
					((LineElement) element).setSection(SimLive.model.getSections().get(getIntegerAttribute(XMLelement, "sectionID")));
					org.jdom.Element XMLsecondAxis = XMLelement.getChild("secondAxis");
					((LineElement) element).setQ0(new double[]{getDoubleAttribute(XMLsecondAxis, "x"),
							getDoubleAttribute(XMLsecondAxis, "y"), getDoubleAttribute(XMLsecondAxis, "z")});
				}
				if (getStringAttribute(XMLelement, "type").equals(Element.Type.SPRING.toString())) {
					int[] nodes = new int[2];
					nodes[0] = getIntegerAttribute(XMLelement, "node0");
					nodes[1] = getIntegerAttribute(XMLelement, "node1");
					element = new Spring(nodes);
					((Spring) element).setStiffness(getDoubleAttribute(XMLelement, "stiffness"));
					org.jdom.Element XMLsecondAxis = XMLelement.getChild("secondAxis");
					((LineElement) element).setQ0(new double[]{getDoubleAttribute(XMLsecondAxis, "x"),
							getDoubleAttribute(XMLsecondAxis, "y"), getDoubleAttribute(XMLsecondAxis, "z")});
				}
				if (getStringAttribute(XMLelement, "type").equals(Element.Type.BEAM.toString())) {
					int[] nodes = new int[2];
					nodes[0] = getIntegerAttribute(XMLelement, "node0");
					nodes[1] = getIntegerAttribute(XMLelement, "node1");
					element = new Beam(nodes);
					element.setMaterial(SimLive.model.getMaterials().get(getIntegerAttribute(XMLelement, "materialID")));
					((LineElement) element).setSection(SimLive.model.getSections().get(getIntegerAttribute(XMLelement, "sectionID")));
					org.jdom.Element XMLsecondAxis = XMLelement.getChild("secondAxis");
					((LineElement) element).setQ0(new double[]{getDoubleAttribute(XMLsecondAxis, "x"),
							getDoubleAttribute(XMLsecondAxis, "y"), getDoubleAttribute(XMLsecondAxis, "z")});
				}
				if (getStringAttribute(XMLelement, "type").equals(Element.Type.TRI.toString())) {
					int[] nodes = new int[3];
					nodes[0] = getIntegerAttribute(XMLelement, "node0");
					nodes[1] = getIntegerAttribute(XMLelement, "node1");
					nodes[2] = getIntegerAttribute(XMLelement, "node2");
					element = new Tri(nodes);
				}
				if (getStringAttribute(XMLelement, "type").equals(Element.Type.QUAD.toString())) {
					int[] nodes = new int[4];
					nodes[0] = getIntegerAttribute(XMLelement, "node0");
					nodes[1] = getIntegerAttribute(XMLelement, "node1");
					nodes[2] = getIntegerAttribute(XMLelement, "node2");
					nodes[3] = getIntegerAttribute(XMLelement, "node3");
					element = new Quad(nodes);
				}
				if (element.getType() == Element.Type.TRI || element.getType() == Element.Type.QUAD) {
					element.setMaterial(SimLive.model.getMaterials().get(getIntegerAttribute(XMLelement, "materialID")));
					/*if (getStringAttribute(XMLelement, "state").equals(PlaneElement.State.PLANE_STRESS.toString())) {
						((PlaneElement) element).setState(PlaneElement.State.PLANE_STRESS);
					}
					else {
						((PlaneElement) element).setState(PlaneElement.State.PLANE_STRAIN);
					}*/
					((PlaneElement) element).setThickness(getDoubleAttribute(XMLelement, "thickness"));
				}
				element.setStiffnessDamping(getDoubleAttribute(XMLelement, "stiffnessDamping"));
				element.setMassDamping(getDoubleAttribute(XMLelement, "massDamping"));
				SimLive.model.getElements().add(element);
			}
			
			/* sets */
			org.jdom.Element XMLsets = XMLmodel.getChild("sets");
			list = XMLsets.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLset = (org.jdom.Element) list[i];
				Set.Type type = Set.Type.valueOf(getStringAttribute(XMLset, "type"));
				Set set = new Set(type);
				set.view = Set.View.valueOf(getStringAttribute(XMLset, "view"));
				getSubSets(XMLset, set);
				SimLive.model.getSets().add(set);
				set.update();
			}
			
			/* parts3d */
			org.jdom.Element XMLparts3d = XMLmodel.getChild("parts3d");
			list = XMLparts3d.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {
				org.jdom.Element XMLpart3d = (org.jdom.Element) list[i];
				
				org.jdom.Element XMLsubTree = XMLpart3d.getChild("subTree");
				SubTree subTree = new SubTree();
				getSubTree(XMLsubTree, subTree);
				
				Part3d part3d = new Part3d(subTree.nrVertices, subTree.nrFacets);
				part3d.render = Part3d.Render.valueOf(getStringAttribute(XMLpart3d, "render"));
				part3d.doubleSided = getBooleanAttribute(XMLpart3d, "doubleSided");
				part3d.setSubTree(subTree);
				
				org.jdom.Element XMLvertices = XMLpart3d.getChild("vertices");
				Object[] listVertices = XMLvertices.getChildren().toArray();

				for (int n = 0; n < listVertices.length; n++) {
					org.jdom.Element XMLvertex = (org.jdom.Element) listVertices[n];
					double[] coords = new double[3];
					coords[0] = getDoubleAttribute(XMLvertex, "x");
					coords[1] = getDoubleAttribute(XMLvertex, "y");
					coords[2] = getDoubleAttribute(XMLvertex, "z");
					part3d.setVertex(new Vertex3d(coords), n);
				}
				
				org.jdom.Element XMLfacets = XMLpart3d.getChild("facets");
				Object[] listFacets = XMLfacets.getChildren().toArray();

				for (int f = 0; f < listFacets.length; f++) {
					org.jdom.Element XMLfacet = (org.jdom.Element) listFacets[f];
					int[] indices = new int[3];
					indices[0] = getIntegerAttribute(XMLfacet, "ID0");
					indices[1] = getIntegerAttribute(XMLfacet, "ID1");
					indices[2] = getIntegerAttribute(XMLfacet, "ID2");
					int colorID = getIntegerAttribute(XMLfacet, "colorID");
					part3d.setFacet(new Facet3d(indices, colorID), f);
				}
				
				SimLive.model.getParts3d().add(part3d);				
			}
			
			/* part3dColors */
			org.jdom.Element XMLpart3dColors = XMLmodel.getChild("part3dColors");
			list = XMLpart3dColors.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLpart3dColor = (org.jdom.Element) list[i];
				Part3dColor part3dColor = new Part3dColor();
				org.jdom.Element XMLKd = XMLpart3dColor.getChild("Kd");
				part3dColor.setKd((float) getDoubleAttribute(XMLKd, "red"),
						(float) getDoubleAttribute(XMLKd, "green"),
						(float) getDoubleAttribute(XMLKd, "blue"));
				org.jdom.Element XMLKs = XMLpart3dColor.getChild("Ks");
				part3dColor.setKs((float) getDoubleAttribute(XMLKs, "red"),
						(float) getDoubleAttribute(XMLKs, "green"),
						(float) getDoubleAttribute(XMLKs, "blue"));
				part3dColor.setShininess((float) getDoubleAttribute(XMLpart3dColor, "shininess"));
				SimLive.model.getPart3dColors().add(part3dColor);
			}
			
			/* supports */
			org.jdom.Element XMLsupports = XMLmodel.getChild("supports");
			list = XMLsupports.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLsupport = (org.jdom.Element) list[i];
				org.jdom.Element XMLsupportNodes = XMLsupport.getChild("nodes");
				Support support = new Support();
				support.name = getStringAttribute(XMLsupport, "name");
				support.isShifted = getBooleanAttribute(XMLsupport, "isShifted");
				support.setNodes(getNodeSet(XMLsupportNodes));
				
				support.setAngle(getDoubleAttribute(XMLsupport, "angle"));
				org.jdom.Element XMLaxis = XMLsupport.getChild("axis");
				support.setAxis(getDoubleAttribute(XMLaxis, "x"), 0);
				support.setAxis(getDoubleAttribute(XMLaxis, "y"), 1);
				support.setAxis(getDoubleAttribute(XMLaxis, "z"), 2);
				org.jdom.Element XMLisFixedDisp = XMLsupport.getChild("isFixedDisp");
				support.setFixedDisp(getBooleanAttribute(XMLisFixedDisp, "x"), 0);
				support.setFixedDisp(getBooleanAttribute(XMLisFixedDisp, "y"), 1);
				support.setFixedDisp(getBooleanAttribute(XMLisFixedDisp, "z"), 2);
				org.jdom.Element XMLisFixedRot = XMLsupport.getChild("isFixedRot");
				support.setFixedRot(getBooleanAttribute(XMLisFixedRot, "x"), 0);
				support.setFixedRot(getBooleanAttribute(XMLisFixedRot, "y"), 1);
				support.setFixedRot(getBooleanAttribute(XMLisFixedRot, "z"), 2);
				
				SimLive.model.getSupports().add(support);
			}
			
			/* loads */
			org.jdom.Element XMLloads = XMLmodel.getChild("loads");
			list = XMLloads.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLload = (org.jdom.Element) list[i];
				org.jdom.Element XMLloadNodes = XMLload.getChild("nodes");
				Load load = new Load();
				load.name = getStringAttribute(XMLload, "name");
				load.isShifted = getBooleanAttribute(XMLload, "isShifted");
				load.setNodes(getNodeSet(XMLloadNodes));
				
				Load.Type type = Load.Type.valueOf(getStringAttribute(XMLload, "type"));
				load.setType(type);
				
				load.setAngle(getDoubleAttribute(XMLload, "angle"));
				org.jdom.Element XMLaxis = XMLload.getChild("axis");
				load.setAxis(getDoubleAttribute(XMLaxis, "x"), 0);
				load.setAxis(getDoubleAttribute(XMLaxis, "y"), 1);
				load.setAxis(getDoubleAttribute(XMLaxis, "z"), 2);
				org.jdom.Element XMLforce = XMLload.getChild("force");
				load.setForce(getDoubleAttribute(XMLforce, "x"), 0);
				load.setForce(getDoubleAttribute(XMLforce, "y"), 1);
				load.setForce(getDoubleAttribute(XMLforce, "z"), 2);
				org.jdom.Element XMLmoment = XMLload.getChild("moment");
				load.setMoment(getDoubleAttribute(XMLmoment, "x"), 0);
				load.setMoment(getDoubleAttribute(XMLmoment, "y"), 1);
				load.setMoment(getDoubleAttribute(XMLmoment, "z"), 2);
				org.jdom.Element XMLisDisp = XMLload.getChild("isDisp");
				load.setDisp(getBooleanAttribute(XMLisDisp, "x"), 0);
				load.setDisp(getBooleanAttribute(XMLisDisp, "y"), 1);
				load.setDisp(getBooleanAttribute(XMLisDisp, "z"), 2);
				org.jdom.Element XMLdisp = XMLload.getChild("disp");
				load.setDisp(getDoubleAttribute(XMLdisp, "x"), 0);
				load.setDisp(getDoubleAttribute(XMLdisp, "y"), 1);
				load.setDisp(getDoubleAttribute(XMLdisp, "z"), 2);
				org.jdom.Element XMLisRotation = XMLload.getChild("isRotation");
				load.setRotation(getBooleanAttribute(XMLisRotation, "x"), 0);
				load.setRotation(getBooleanAttribute(XMLisRotation, "y"), 1);
				load.setRotation(getBooleanAttribute(XMLisRotation, "z"), 2);
				org.jdom.Element XMLrotation = XMLload.getChild("rotation");
				load.setRotation(getDoubleAttribute(XMLrotation, "x"), 0);
				load.setRotation(getDoubleAttribute(XMLrotation, "y"), 1);
				load.setRotation(getDoubleAttribute(XMLrotation, "z"), 2);
				
				if (XMLload.getAttribute("referenceNode") != null) {
					load.referenceNode = SimLive.model.getNodes().get(getIntegerAttribute(XMLload, "referenceNode"));
				}
				
				load.setTimeTable(getTimeTable(XMLload.getChild("timeTable")));
				
				SimLive.model.getLoads().add(load);
			}
			
			/* distributed loads */
			org.jdom.Element XMLdistributedLoads = XMLmodel.getChild("distributedLoads");
			list = XMLdistributedLoads.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLdistributedLoad = (org.jdom.Element) list[i];
				ArrayList<Set> sets = new ArrayList<Set>();
				org.jdom.Element XMLdistributedLoadSets = XMLdistributedLoad.getChild("sets");
				Object[] listSets = XMLdistributedLoadSets.getChildren().toArray();
				
				for (int j = 0; j < listSets.length; j++) {
					org.jdom.Element XMLdistributedLoadSet = (org.jdom.Element) listSets[j];
					Set.Type type = Set.Type.valueOf(getStringAttribute(XMLdistributedLoadSet, "type"));
					Set set = new Set(type);
					getSubSets(XMLdistributedLoadSet, set);
					sets.add(SimLive.model.getSetByElementsRecursive(SimLive.model.getSets(), set.getElements()));
				}				
				
				double angle = getDoubleAttribute(XMLdistributedLoad, "angle");
				org.jdom.Element XMLaxis = XMLdistributedLoad.getChild("axis");
				double axisX = getDoubleAttribute(XMLaxis, "x");
				double axisY = getDoubleAttribute(XMLaxis, "y");
				double axisZ = getDoubleAttribute(XMLaxis, "z");
				boolean isLocalSysAligned = getBooleanAttribute(XMLdistributedLoad, "isLocalSysAligned");
				double xDirStartValue = getDoubleAttribute(XMLdistributedLoad, "xDirStartValue");
				double xDirEndValue = getDoubleAttribute(XMLdistributedLoad, "xDirEndValue");
				double yDirStartValue = getDoubleAttribute(XMLdistributedLoad, "yDirStartValue");
				double yDirEndValue = getDoubleAttribute(XMLdistributedLoad, "yDirEndValue");
				double zDirStartValue = getDoubleAttribute(XMLdistributedLoad, "zDirStartValue");
				double zDirEndValue = getDoubleAttribute(XMLdistributedLoad, "zDirEndValue");
				
				DistributedLoad load = new DistributedLoad();
				load.name = getStringAttribute(XMLdistributedLoad, "name");
				load.isShifted = getBooleanAttribute(XMLdistributedLoad, "isShifted");
				load.setElementSets(sets);
				load.setAngle(angle);
				load.setAxis(axisX, 0);
				load.setAxis(axisY, 1);
				load.setAxis(axisZ, 2);
				load.setLocalSysAligned(isLocalSysAligned);
				load.setStartValue(0, xDirStartValue);
				load.setEndValue(0, xDirEndValue);
				load.setStartValue(1, yDirStartValue);
				load.setEndValue(1, yDirEndValue);
				load.setStartValue(2, zDirStartValue);
				load.setEndValue(2, zDirEndValue);
				
				if (XMLdistributedLoad.getAttribute("referenceNode") != null) {
					load.referenceNode = SimLive.model.getNodes().get(getIntegerAttribute(XMLdistributedLoad, "referenceNode"));
				}
				
				load.setTimeTable(getTimeTable(XMLdistributedLoad.getChild("timeTable")));
				
				SimLive.model.getDistributedLoads().add(load);
			}
			
			/* connectors */
			org.jdom.Element XMLconnectors = XMLmodel.getChild("connectors");
			list = XMLconnectors.getChildren().toArray();
			
			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLconnector = (org.jdom.Element) list[i];
				double[] coords = new double[3];
				coords[0] = getDoubleAttribute(XMLconnector, "xCoord");
				coords[1] = getDoubleAttribute(XMLconnector, "yCoord");
				coords[2] = getDoubleAttribute(XMLconnector, "zCoord");
				Set set0 = SimLive.model.getSets().get(getIntegerAttribute(XMLconnector, "set0ID"));
				Set set1 = SimLive.model.getSets().get(getIntegerAttribute(XMLconnector, "set1ID"));
				Connector connector = new Connector(coords, set0, set1);
				connector.name = getStringAttribute(XMLconnector, "name");
				Connector.Type type = Connector.Type.valueOf(getStringAttribute(XMLconnector, "type"));
				connector.setType(type);
				
				SimLive.model.getConnectors().add(connector);
			}
			
			/* connectors3d */
			org.jdom.Element XMLConnectors3d = XMLmodel.getChild("connectors3d");
			list = XMLConnectors3d.getChildren().toArray();
			
			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLConnector3d = (org.jdom.Element) list[i];
				
				ArrayList<Part3d> parts3d = new ArrayList<Part3d>();
				XMLparts3d = XMLConnector3d.getChild("parts3d");
				Object[] listParts3d = XMLparts3d.getChildren().toArray();
				
				for (int j = 0; j < listParts3d.length; j++) {
					org.jdom.Element XMLpart3d = (org.jdom.Element) listParts3d[j];
					parts3d.add(SimLive.model.getParts3d().get(getIntegerAttribute(XMLpart3d, "ID")));
				}
				
				ArrayList<Set> sets = new ArrayList<Set>();
				XMLsets = XMLConnector3d.getChild("sets");
				Object[] listSets = XMLsets.getChildren().toArray();
				
				for (int j = 0; j < listSets.length; j++) {
					org.jdom.Element XMLset = (org.jdom.Element) listSets[j];
					sets.add(SimLive.model.getSets().get(getIntegerAttribute(XMLset, "ID")));
				}
				
				Connector3d connector3d = new Connector3d(parts3d, sets);
				connector3d.name = getStringAttribute(XMLConnector3d, "name");
				SimLive.model.getConnectors3d().add(connector3d);
			}

			/* contact pairs */
			org.jdom.Element XMLcontactPairs = XMLmodel.getChild("contactPairs");
			list = XMLcontactPairs.getChildren().toArray();
			
			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLcontactPair = (org.jdom.Element) list[i];
				ContactPair contactPair = new ContactPair();
				contactPair.name = getStringAttribute(XMLcontactPair, "name");
				contactPair.setSwitchContactSide(getBooleanAttribute(XMLcontactPair, "switchContactSide"));
				contactPair.setMaxPenetration(getBooleanAttribute(XMLcontactPair, "isMaxPenetration"));
				contactPair.setMaxPenetration(getDoubleAttribute(XMLcontactPair, "maxPenetration"));
				contactPair.setFrictionCoefficient(getDoubleAttribute(XMLcontactPair, "frictionCoefficient"));
				contactPair.setNoSeparation(getBooleanAttribute(XMLcontactPair, "noSeparation"));
				org.jdom.Element XMLslaveNodes = XMLcontactPair.getChild("slaveNodes");
				contactPair.setSlave(getNodeSet(XMLslaveNodes));
				ArrayList<Set> sets = new ArrayList<Set>();
				org.jdom.Element XMLmasterSets = XMLcontactPair.getChild("masterSets");
				Object[] listSets = XMLmasterSets.getChildren().toArray();
				for (int j = 0; j < listSets.length; j++) {
					org.jdom.Element XMLset = (org.jdom.Element) listSets[j];
					Set set = SimLive.model.getSets().get(getIntegerAttribute(XMLset, "ID"));
					for (int e = 0; e < set.getElements().size(); e++) {
						set.getElements().get(e).update();
					}
					sets.add(set);
				}
				contactPair.setMaster(sets);
				contactPair.setType(ContactPair.Type.valueOf(getStringAttribute(XMLcontactPair, "type")), false);
				SimLive.model.getContactPairs().add(contactPair);
			}

			/* steps */
			org.jdom.Element XMLsteps = XMLmodel.getChild("steps");
			list = XMLsteps.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLstep = (org.jdom.Element) list[i];
				Step step = new Step();
				step.name = getStringAttribute(XMLstep, "name");
				step.type = Step.Type.valueOf(getStringAttribute(XMLstep, "type"));
				step.duration = getDoubleAttribute(XMLstep, "duration");
				step.nIncrements = getIntegerAttribute(XMLstep, "nIncrements");
				step.maxIterations = getIntegerAttribute(XMLstep, "maxIterations");
				step.gravity = Step.GRAVITY.valueOf(getStringAttribute(XMLstep, "gravity"));
				step.gValue = getDoubleAttribute(XMLstep, "gValue");				
				SimLive.model.getSteps().add(step);
			}
			
			SimLive.model.updateModel();
			
			return true;

		}
		catch (NullPointerException npex) {
			System.out.println(npex.getMessage());
		}
		catch (NumberFormatException nfex) {
			System.out.println(nfex.getMessage());
		}
		catch (IOException io) {
			System.out.println(io.getMessage());
		}
		catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
		return false;
	}
	
	public static boolean readFileAndGenerateSettings(String fileName) {
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(fileName);

		try {

			SimLive.settings = new Settings();
			
			Document document = (Document) builder.build(xmlFile);
			org.jdom.Element XMLroot = document.getRootElement();
			org.jdom.Element XMLsettings = XMLroot.getChild("settings");
			SimLive.settings.newPartType = Element.Type.valueOf(getStringAttribute(XMLsettings, "newPartType"));
			SimLive.settings.module = getDoubleAttribute(XMLsettings, "module");
			SimLive.settings.pressureAngle = getDoubleAttribute(XMLsettings, "pressureAngle");
			//Sim2d.settings.isShiftForceVectors = getBooleanAttribute(XMLsettings, "isShiftForceVectors");
			SimLive.settings.constraintType = ConstraintMethod.Type.valueOf(getStringAttribute(XMLsettings, "constraintType"));
			SimLive.settings.penaltyFactor = getDoubleAttribute(XMLsettings, "penaltyFactor");
			SimLive.settings.isReorderNodes = getBooleanAttribute(XMLsettings, "isReorderNodes");
			SimLive.settings.isLargeDisplacement = getBooleanAttribute(XMLsettings, "isLargeDisplacement");
			SimLive.settings.isWriteMatrixView = getBooleanAttribute(XMLsettings, "isWriteMatrixView");
			SimLive.settings.unitSystem = Units.UnitSystem.valueOf(getStringAttribute(XMLsettings, "unitSystem"));
			SimLive.settings.meshSize = getDoubleAttribute(XMLsettings, "meshSize");
			SimLive.settings.meshCount = getIntegerAttribute(XMLsettings, "meshCount");
			
			SimLive.settings.isShowAxes = getBooleanAttribute(XMLsettings, "isShowAxes");
			SimLive.settings.isShowGrid = getBooleanAttribute(XMLsettings, "isShowGrid");
			SimLive.settings.isShowScale = getBooleanAttribute(XMLsettings, "isShowScale");
			SimLive.settings.isShowOrientations = getBooleanAttribute(XMLsettings, "isShowOrientations");
			SimLive.settings.isShowNodes = getBooleanAttribute(XMLsettings, "isShowNodes");
			SimLive.settings.isShowEdges = getBooleanAttribute(XMLsettings, "isShowEdges");
			SimLive.settings.isShowSections = getBooleanAttribute(XMLsettings, "isShowSections");
			SimLive.settings.isShowSupports = getBooleanAttribute(XMLsettings, "isShowSupports");
			SimLive.settings.isShowLoads = getBooleanAttribute(XMLsettings, "isShowLoads");
			//Sim2d.settings.isShowReactions = getBooleanAttribute(XMLsettings, "isShowReactions");
			
			return true;

		}
		catch (NullPointerException npex) {
			System.out.println(npex.getMessage());
		}
		catch (NumberFormatException nfex) {
			System.out.println(nfex.getMessage());
		}
		catch (IOException io) {
			System.out.println(io.getMessage());
		}
		catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
		return false;
	}
	
	public static boolean readFileAndGenerateSolution(String fileName) {
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(fileName);

		try {

			SimLive.model.finalUpdateModel();
			Solution solution = new Solution(SimLive.model, SimLive.settings);
			
			Document document = (Document) builder.build(xmlFile);
			org.jdom.Element XMLroot = document.getRootElement();
			org.jdom.Element XMLsolution = XMLroot.getChild("solution");
			org.jdom.Element XMLincrements = XMLsolution.getChild("increments");
			Object[] list = XMLincrements.getChildren().toArray();
			
			Increment[] increments = new Increment[list.length];
			
			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLincrement = (org.jdom.Element) list[i];
				double time = getDoubleAttribute(XMLincrement, "time");
				int stepNr = getIntegerAttribute(XMLincrement, "stepNr");
				increments[i] = new Increment(solution, time, stepNr);
				
				Matrix[] M_elem = XMLToMatrixArray(XMLincrement.getChild("M_elem"));
				Matrix[] K_elem = XMLToMatrixArray(XMLincrement.getChild("K_elem"));
				Matrix M_global = XMLToMatrix(XMLincrement.getChild("M_global"));
				Matrix K_global = XMLToMatrix(XMLincrement.getChild("K_global"));
				Matrix f_ext = XMLToMatrix(XMLincrement.getChild("f_ext"));
				Matrix f_int = XMLToMatrix(XMLincrement.getChild("f_int"));
				Matrix u_global = XMLToMatrix(XMLincrement.getChild("u_global"));
				Matrix v_global = XMLToMatrix(XMLincrement.getChild("v_global"));
				Matrix a_global = XMLToMatrix(XMLincrement.getChild("a_global"));
				Matrix G = XMLToMatrix(XMLincrement.getChild("G"));
				Matrix r_global = XMLToMatrix(XMLincrement.getChild("r_global"));
				Matrix K_constr = XMLToMatrix(XMLincrement.getChild("K_constr"));
				Matrix M_constr = XMLToMatrix(XMLincrement.getChild("M_constr"));
				Matrix delta_f_constr = XMLToMatrix(XMLincrement.getChild("delta_f_constr"));
				
				increments[i].setResults(u_global, v_global, a_global, r_global);
				increments[i].setResultsForMatrixView(K_elem, M_elem, M_global, K_global, f_ext, f_int, G, K_constr, M_constr, delta_f_constr);
			}
			
			solution.setIncrements(increments);
			
			solution.setD(XMLToMatrix(XMLsolution.getChild("D")));
			solution.setV(XMLToMatrix(XMLsolution.getChild("V")));
			
			Solution.log = XMLToStringList(XMLsolution.getChild("log"));
			Solution.warnings = XMLToStringList(XMLsolution.getChild("warnings"));
			Solution.errors = XMLToStringList(XMLsolution.getChild("errors"));
			SimLive.initPost(solution);
			
			return true;

		}
		catch (NullPointerException npex) {
			System.out.println(npex.getMessage());
		}
		catch (NumberFormatException nfex) {
			System.out.println(nfex.getMessage());
		}
		catch (IOException io) {
			System.out.println(io.getMessage());
		}
		catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
		return false;
	}
	
	public static void writeFile(String fileName, Model model, Settings settings) {
		try {
			ArrayList<Node> nodes = model.getNodes();
			ArrayList<Element> elements = model.getElements();
			ArrayList<Set> sets = model.getSets();
			ArrayList<Part3d> parts3d = model.getParts3d();
			ArrayList<Part3dColor> part3dColors = model.getPart3dColors();
			ArrayList<Support> supports = model.getSupports();
			ArrayList<Load> loads = model.getLoads();
			ArrayList<DistributedLoad> distributedLoads = model.getDistributedLoads();
			ArrayList<Connector> connectors = model.getConnectors();
			ArrayList<Connector3d> connector3ds = model.getConnectors3d();
			ArrayList<ContactPair> contactPairs = model.getContactPairs();
			ArrayList<Material> materials = model.getMaterials();
			ArrayList<Section> sections = model.getSections();
			ArrayList<Step> steps = model.getSteps();
			
			/* conversion for file writing */
			convertRigidDeformableToDeformableDeformable();
			
			org.jdom.Element XMLroot = new org.jdom.Element("root");
			setStringAttribute(XMLroot, "application", SimLive.APPLICATION_NAME);
			setStringAttribute(XMLroot, "version", SimLive.VERSION_NAME);
			Document doc = new Document(XMLroot);
			doc.setRootElement(XMLroot);
			
			org.jdom.Element XMLmodel = new org.jdom.Element("model");
			setBooleanAttribute(XMLmodel, "twoDimensional", Model.twoDimensional);

			/* nodes */
			org.jdom.Element XMLnodes = new org.jdom.Element("nodes");
			for (int i = 0; i < nodes.size(); i++) {
				Node node = nodes.get(i);
				org.jdom.Element XMLnode = new org.jdom.Element("node");
				setDoubleAttribute(XMLnode, "xCoord", node.getXCoord());
				setDoubleAttribute(XMLnode, "yCoord", node.getYCoord());
				setDoubleAttribute(XMLnode, "zCoord", node.getZCoord());
				
				XMLnodes.addContent(XMLnode);
			}
			XMLmodel.addContent(XMLnodes);
			
			/* elements */
			org.jdom.Element XMLelements = new org.jdom.Element("elements");
			for (int i = 0; i < elements.size(); i++) {
				Element element = elements.get(i);
				org.jdom.Element XMLelement = new org.jdom.Element("element");
				setStringAttribute(XMLelement, "type", element.getType().toString());
				setDoubleAttribute(XMLelement, "stiffnessDamping", element.getStiffnessDamping());
				setDoubleAttribute(XMLelement, "massDamping", element.getMassDamping());
				int[] elemNodes = element.getElementNodes();
				if (element.getType() == Element.Type.POINT_MASS) {
					setIntegerAttribute(XMLelement, "node0", elemNodes[0]);
					setDoubleAttribute(XMLelement, "mass", ((PointMass) element).getMass());
				}
				if (element.isLineElement()) {
					setIntegerAttribute(XMLelement, "node0", elemNodes[0]);
					setIntegerAttribute(XMLelement, "node1", elemNodes[1]);
					if (element.getType() == Element.Type.SPRING) {
						setDoubleAttribute(XMLelement, "stiffness", ((Spring) element).getStiffness());
					}
					else {
						setIntegerAttribute(XMLelement, "materialID", materials.indexOf(element.getMaterial()));
						setIntegerAttribute(XMLelement, "sectionID", sections.indexOf(((LineElement) element).getSection()));
					}
					org.jdom.Element XMLsecondAxis = new org.jdom.Element("secondAxis");
					setDoubleAttribute(XMLsecondAxis, "x", ((LineElement) element).getQ0()[0]);
					setDoubleAttribute(XMLsecondAxis, "y", ((LineElement) element).getQ0()[1]);
					setDoubleAttribute(XMLsecondAxis, "z", ((LineElement) element).getQ0()[2]);
					XMLelement.addContent(XMLsecondAxis);
				}
				if (element.getType() == Element.Type.TRI) {
					setIntegerAttribute(XMLelement, "node0", elemNodes[0]);
					setIntegerAttribute(XMLelement, "node1", elemNodes[1]);
					setIntegerAttribute(XMLelement, "node2", elemNodes[2]);
					setIntegerAttribute(XMLelement, "materialID", materials.indexOf(element.getMaterial()));
					//setStringAttribute(XMLelement, "state", ((Tri) element).getState().toString());
					setDoubleAttribute(XMLelement, "thickness", ((Tri) element).getThickness());
				}
				if (element.getType() == Element.Type.QUAD) {
					setIntegerAttribute(XMLelement, "node0", elemNodes[0]);
					setIntegerAttribute(XMLelement, "node1", elemNodes[1]);
					setIntegerAttribute(XMLelement, "node2", elemNodes[2]);
					setIntegerAttribute(XMLelement, "node3", elemNodes[3]);
					setIntegerAttribute(XMLelement, "materialID", materials.indexOf(element.getMaterial()));
					//setStringAttribute(XMLelement, "state", ((Quad) element).getState().toString());
					setDoubleAttribute(XMLelement, "thickness", ((Quad) element).getThickness());
				}
				XMLelements.addContent(XMLelement);
			}
			XMLmodel.addContent(XMLelements);
			
			/* sets */
			org.jdom.Element XMLsets = new org.jdom.Element("sets");
			for (int s = 0; s < sets.size(); s++) {
				org.jdom.Element XMLset = new org.jdom.Element("set");
				setSubSets(XMLset, sets.get(s));
				
				XMLsets.addContent(XMLset);
			}
			XMLmodel.addContent(XMLsets);
			
			/* parts3d */
			org.jdom.Element XMLparts3d = new org.jdom.Element("parts3d");
			for (int s = 0; s < parts3d.size(); s++) {
				Part3d part3d = parts3d.get(s);
				org.jdom.Element XMLpart3d = new org.jdom.Element("part3d");
				setStringAttribute(XMLpart3d, "render", part3d.render.toString());
				setBooleanAttribute(XMLpart3d, "doubleSided", part3d.doubleSided);
				org.jdom.Element XMLvertices = new org.jdom.Element("vertices");
				for (int n = 0; n < part3d.getNrVertices(); n++) {
					org.jdom.Element XMLvertex = new org.jdom.Element("vertex");
					setDoubleAttribute(XMLvertex, "x", part3d.getVertex(n).getCoords()[0]);
					setDoubleAttribute(XMLvertex, "y", part3d.getVertex(n).getCoords()[1]);
					setDoubleAttribute(XMLvertex, "z", part3d.getVertex(n).getCoords()[2]);
					XMLvertices.addContent(XMLvertex);
				}
				XMLpart3d.addContent(XMLvertices);
				org.jdom.Element XMLfacets = new org.jdom.Element("facets");
				for (int f = 0; f < part3d.getNrFacets(); f++) {
					org.jdom.Element XMLfacet = new org.jdom.Element("facet");
					setIntegerAttribute(XMLfacet, "ID0", part3d.getFacet(f).getIndices()[0]);
					setIntegerAttribute(XMLfacet, "ID1", part3d.getFacet(f).getIndices()[1]);
					setIntegerAttribute(XMLfacet, "ID2", part3d.getFacet(f).getIndices()[2]);
					setIntegerAttribute(XMLfacet, "colorID", part3d.getFacet(f).getColorID());
					XMLfacets.addContent(XMLfacet);
				}
				XMLpart3d.addContent(XMLfacets);
				
				org.jdom.Element XMLsubTree = new org.jdom.Element("subTree");
				setSubTree(XMLsubTree, part3d.getSubTree());
				XMLpart3d.addContent(XMLsubTree);
				
				XMLparts3d.addContent(XMLpart3d);
			}
			XMLmodel.addContent(XMLparts3d);
			
			/* part3dColors */
			org.jdom.Element XMLpart3dColors = new org.jdom.Element("part3dColors");
			for (int c = 0; c < part3dColors.size(); c++) {
				Part3dColor part3dColor = part3dColors.get(c);
				org.jdom.Element XMLpart3dColor = new org.jdom.Element("part3dColor");
				org.jdom.Element XMLKd = new org.jdom.Element("Kd");
				setDoubleAttribute(XMLKd, "red", part3dColor.getKd()[0]);
				setDoubleAttribute(XMLKd, "green", part3dColor.getKd()[1]);
				setDoubleAttribute(XMLKd, "blue", part3dColor.getKd()[2]);
				XMLpart3dColor.addContent(XMLKd);
				org.jdom.Element XMLKs = new org.jdom.Element("Ks");
				setDoubleAttribute(XMLKs, "red", part3dColor.getKs()[0]);
				setDoubleAttribute(XMLKs, "green", part3dColor.getKs()[1]);
				setDoubleAttribute(XMLKs, "blue", part3dColor.getKs()[2]);
				XMLpart3dColor.addContent(XMLKs);
				setDoubleAttribute(XMLpart3dColor, "shininess", part3dColor.getShininess());
				XMLpart3dColors.addContent(XMLpart3dColor);
			}
			XMLmodel.addContent(XMLpart3dColors);
			
			/* supports */
			org.jdom.Element XMLsupports = new org.jdom.Element("supports");
			for (int s = 0; s < supports.size(); s++) {
				Support support = supports.get(s);
				org.jdom.Element XMLsupport = new org.jdom.Element("support");
				setStringAttribute(XMLsupport, "name", support.name);
				setBooleanAttribute(XMLsupport, "isShifted", support.isShifted);
				setDoubleAttribute(XMLsupport, "angle", support.getAngle());
				org.jdom.Element XMLaxis = new org.jdom.Element("axis");
				setDoubleAttribute(XMLaxis, "x", support.getAxis()[0]);
				setDoubleAttribute(XMLaxis, "y", support.getAxis()[1]);
				setDoubleAttribute(XMLaxis, "z", support.getAxis()[2]);
				XMLsupport.addContent(XMLaxis);
				org.jdom.Element XMLisFixedDisp = new org.jdom.Element("isFixedDisp");
				setBooleanAttribute(XMLisFixedDisp, "x", support.isFixedDisp()[0]);
				setBooleanAttribute(XMLisFixedDisp, "y", support.isFixedDisp()[1]);
				setBooleanAttribute(XMLisFixedDisp, "z", support.isFixedDisp()[2]);
				XMLsupport.addContent(XMLisFixedDisp);
				org.jdom.Element XMLisFixedRot = new org.jdom.Element("isFixedRot");
				setBooleanAttribute(XMLisFixedRot, "x", support.isFixedRot()[0]);
				setBooleanAttribute(XMLisFixedRot, "y", support.isFixedRot()[1]);
				setBooleanAttribute(XMLisFixedRot, "z", support.isFixedRot()[2]);
				XMLsupport.addContent(XMLisFixedRot);
				
				org.jdom.Element XMLsupportNodes = new org.jdom.Element("nodes");
				setNodeSet(XMLsupportNodes, support.getNodes());
				XMLsupport.addContent(XMLsupportNodes);
				XMLsupports.addContent(XMLsupport);
			}
			XMLmodel.addContent(XMLsupports);
			
			/* loads */
			org.jdom.Element XMLloads = new org.jdom.Element("loads");
			for (int l = 0; l < loads.size(); l++) {
				Load load = loads.get(l);
				org.jdom.Element XMLload = new org.jdom.Element("load");
				setStringAttribute(XMLload, "name", load.name);
				setBooleanAttribute(XMLload, "isShifted", load.isShifted);
				setStringAttribute(XMLload, "type", load.getType().toString());
				setDoubleAttribute(XMLload, "angle", load.getAngle());
				org.jdom.Element XMLaxis = new org.jdom.Element("axis");
				setDoubleAttribute(XMLaxis, "x", load.getAxis()[0]);
				setDoubleAttribute(XMLaxis, "y", load.getAxis()[1]);
				setDoubleAttribute(XMLaxis, "z", load.getAxis()[2]);
				XMLload.addContent(XMLaxis);
				org.jdom.Element XMLforce = new org.jdom.Element("force");
				setDoubleAttribute(XMLforce, "x", load.getForce()[0]);
				setDoubleAttribute(XMLforce, "y", load.getForce()[1]);
				setDoubleAttribute(XMLforce, "z", load.getForce()[2]);
				XMLload.addContent(XMLforce);
				org.jdom.Element XMLmoment = new org.jdom.Element("moment");
				setDoubleAttribute(XMLmoment, "x", load.getMoment()[0]);
				setDoubleAttribute(XMLmoment, "y", load.getMoment()[1]);
				setDoubleAttribute(XMLmoment, "z", load.getMoment()[2]);
				XMLload.addContent(XMLmoment);
				org.jdom.Element XMLisDisp = new org.jdom.Element("isDisp");
				setBooleanAttribute(XMLisDisp, "x", load.isDisp()[0]);
				setBooleanAttribute(XMLisDisp, "y", load.isDisp()[1]);
				setBooleanAttribute(XMLisDisp, "z", load.isDisp()[2]);
				XMLload.addContent(XMLisDisp);
				org.jdom.Element XMLdisp = new org.jdom.Element("disp");
				setDoubleAttribute(XMLdisp, "x", load.getDisp()[0]);
				setDoubleAttribute(XMLdisp, "y", load.getDisp()[1]);
				setDoubleAttribute(XMLdisp, "z", load.getDisp()[2]);
				XMLload.addContent(XMLdisp);
				org.jdom.Element XMLisRotation = new org.jdom.Element("isRotation");
				setBooleanAttribute(XMLisRotation, "x", load.isRotation()[0]);
				setBooleanAttribute(XMLisRotation, "y", load.isRotation()[1]);
				setBooleanAttribute(XMLisRotation, "z", load.isRotation()[2]);
				XMLload.addContent(XMLisRotation);
				org.jdom.Element XMLrotation = new org.jdom.Element("rotation");
				setDoubleAttribute(XMLrotation, "x", load.getRotation()[0]);
				setDoubleAttribute(XMLrotation, "y", load.getRotation()[1]);
				setDoubleAttribute(XMLrotation, "z", load.getRotation()[2]);
				XMLload.addContent(XMLrotation);
				setTimeTable(XMLload, load.getTimeTable());				
				
				if (load.referenceNode != null) {
					setIntegerAttribute(XMLload, "referenceNode", load.referenceNode.getID());
				}
				
				org.jdom.Element XMLloadNodes = new org.jdom.Element("nodes");
				setNodeSet(XMLloadNodes, load.getNodes());
				XMLload.addContent(XMLloadNodes);
				XMLloads.addContent(XMLload);
			}
			XMLmodel.addContent(XMLloads);
			
			/* distributed loads */
			org.jdom.Element XMLdistributedLoads = new org.jdom.Element("distributedLoads");
			for (int d = 0; d < distributedLoads.size(); d++) {
				DistributedLoad distributedLoad = distributedLoads.get(d);
				org.jdom.Element XMLdistributedLoad = new org.jdom.Element("distributedLoad");
				setStringAttribute(XMLdistributedLoad, "name", distributedLoad.name);
				setBooleanAttribute(XMLdistributedLoad, "isShifted", distributedLoad.isShifted);
				setDoubleAttribute(XMLdistributedLoad, "angle",
						distributedLoad.getAngle());
				org.jdom.Element XMLaxis = new org.jdom.Element("axis");
				setDoubleAttribute(XMLaxis, "x", distributedLoad.getAxis()[0]);
				setDoubleAttribute(XMLaxis, "y", distributedLoad.getAxis()[1]);
				setDoubleAttribute(XMLaxis, "z", distributedLoad.getAxis()[2]);
				XMLdistributedLoad.addContent(XMLaxis);
				setBooleanAttribute(XMLdistributedLoad, "isLocalSysAligned",
						distributedLoad.isLocalSysAligned());
				setDoubleAttribute(XMLdistributedLoad, "xDirStartValue",
						distributedLoad.getStartValue(0));
				setDoubleAttribute(XMLdistributedLoad, "xDirEndValue",
						distributedLoad.getEndValue(0));
				setDoubleAttribute(XMLdistributedLoad, "yDirStartValue",
						distributedLoad.getStartValue(1));
				setDoubleAttribute(XMLdistributedLoad, "yDirEndValue",
						distributedLoad.getEndValue(1));
				setDoubleAttribute(XMLdistributedLoad, "zDirStartValue",
						distributedLoad.getStartValue(2));
				setDoubleAttribute(XMLdistributedLoad, "zDirEndValue",
						distributedLoad.getEndValue(2));
				
				if (distributedLoad.referenceNode != null) {
					setIntegerAttribute(XMLdistributedLoad, "referenceNode", distributedLoad.referenceNode.getID());
				}
								
				org.jdom.Element XMLdistributedLoadSets = new org.jdom.Element("sets");
				for (int s = 0; s < distributedLoad.getElementSets().size(); s++) {
					Set set = distributedLoad.getElementSets().get(s);
					org.jdom.Element XMLdistributedLoadSet = new org.jdom.Element("set");
					setSubSets(XMLdistributedLoadSet, set);
					XMLdistributedLoadSets.addContent(XMLdistributedLoadSet);					
				}
				XMLdistributedLoad.addContent(XMLdistributedLoadSets);
				
				setTimeTable(XMLdistributedLoad, distributedLoad.getTimeTable());
				
				XMLdistributedLoads.addContent(XMLdistributedLoad);
			}
			XMLmodel.addContent(XMLdistributedLoads);
			
			/* connectors */
			org.jdom.Element XMLconnectors = new org.jdom.Element("connectors");
			for (int c = 0; c < connectors.size(); c++) {
				Connector connector = connectors.get(c);
				org.jdom.Element XMLconnector = new org.jdom.Element("connector");
				if (connector.isCoordsSet() && connector.getSet0() != null && connector.getSet1() != null) {
					setStringAttribute(XMLconnector, "name", connector.name);
					setDoubleAttribute(XMLconnector, "xCoord", connector.getCoordinates()[0]);
					setDoubleAttribute(XMLconnector, "yCoord", connector.getCoordinates()[1]);
					setDoubleAttribute(XMLconnector, "zCoord", connector.getCoordinates()[2]);
					setIntegerAttribute(XMLconnector, "set0ID", connector.getSet0().getID());
					setIntegerAttribute(XMLconnector, "set1ID", connector.getSet1().getID());
					setStringAttribute(XMLconnector, "type", connector.getType().toString());
					
					XMLconnectors.addContent(XMLconnector);
				}
			}
			XMLmodel.addContent(XMLconnectors);
			
			/* connectors3d */
			org.jdom.Element XMLConnectors3d = new org.jdom.Element("connectors3d");
			for (int c = 0; c < connector3ds.size(); c++) {
				Connector3d connector3d = connector3ds.get(c);
				org.jdom.Element XMLConnector3d = new org.jdom.Element("connector3d");
				setStringAttribute(XMLConnector3d, "name", connector3d.name);
				
				XMLparts3d = new org.jdom.Element("parts3d");
				for (int s = 0; s < connector3d.getParts3d().size(); s++) {
					Part3d part3d = connector3d.getParts3d().get(s);
					org.jdom.Element XMLpart3d = new org.jdom.Element("part3d");
					setIntegerAttribute(XMLpart3d, "ID", part3d.getID());					
					XMLparts3d.addContent(XMLpart3d);					
				}
				XMLsets = new org.jdom.Element("sets");
				for (int s = 0; s < connector3d.getParts().size(); s++) {
					Set set = connector3d.getParts().get(s);
					org.jdom.Element XMLset = new org.jdom.Element("set");
					setIntegerAttribute(XMLset, "ID", set.getID());					
					XMLsets.addContent(XMLset);					
				}
				
				XMLConnector3d.addContent(XMLparts3d);
				XMLConnector3d.addContent(XMLsets);
				XMLConnectors3d.addContent(XMLConnector3d);
			}
			XMLmodel.addContent(XMLConnectors3d);
			
			/* contact pairs */
			org.jdom.Element XMLcontactPairs = new org.jdom.Element("contactPairs");
			for (int c = 0; c < contactPairs.size(); c++) {
				ContactPair contactPair = contactPairs.get(c);
				org.jdom.Element XMLcontactPair = new org.jdom.Element("contactPair");
				setStringAttribute(XMLcontactPair, "name", contactPair.name);
				setBooleanAttribute(XMLcontactPair, "switchContactSide", contactPair.isSwitchContactSide());
				setBooleanAttribute(XMLcontactPair, "isMaxPenetration", contactPair.isMaxPenetration());
				setDoubleAttribute(XMLcontactPair, "maxPenetration", contactPair.getMaxPenetration());
				setDoubleAttribute(XMLcontactPair, "frictionCoefficient", contactPair.getFrictionCoefficient());
				setBooleanAttribute(XMLcontactPair, "noSeparation", contactPair.isNoSeparation());
				setStringAttribute(XMLcontactPair, "type", contactPair.getType().toString());
				org.jdom.Element XMLslaveNodes = new org.jdom.Element("slaveNodes");
				setNodeSet(XMLslaveNodes, contactPair.getSlaveNodes());
				XMLcontactPair.addContent(XMLslaveNodes);
				org.jdom.Element XMLmasterSets = new org.jdom.Element("masterSets");
				for (int s = 0; s < contactPair.getMasterSets().size(); s++) {
					Set set = contactPair.getMasterSets().get(s);
					org.jdom.Element XMLset = new org.jdom.Element("set");
					setIntegerAttribute(XMLset, "ID", set.getID());
					XMLmasterSets.addContent(XMLset);
				}
				XMLcontactPair.addContent(XMLmasterSets);
				XMLcontactPairs.addContent(XMLcontactPair);
			}
			XMLmodel.addContent(XMLcontactPairs);

			/* materials */
			org.jdom.Element XMLmaterials = new org.jdom.Element("materials");
			for (int i = 0; i < materials.size(); i++) {
				Material material = materials.get(i);
				org.jdom.Element XMLmaterial = new org.jdom.Element("material");
				setStringAttribute(XMLmaterial, "name", material.name);
				setDoubleAttribute(XMLmaterial, "density", material.getDensity());
				setDoubleAttribute(XMLmaterial, "youngsModulus", material.getYoungsModulus());
				setDoubleAttribute(XMLmaterial, "poissonsRatio", material.getPoissonsRatio());
				XMLmaterials.addContent(XMLmaterial);
			}			
			XMLmodel.addContent(XMLmaterials);
			
			/* sections */
			org.jdom.Element XMLsections = new org.jdom.Element("sections");
			for (int i = 0; i < sections.size(); i++) {
				Section section = sections.get(i);
				org.jdom.Element XMLsection = new org.jdom.Element("section");
				setDoubleAttribute(XMLsection, "area", section.getArea());
				setDoubleAttribute(XMLsection, "Iy", section.getIy());
				setDoubleAttribute(XMLsection, "Iz", section.getIz());
				setDoubleAttribute(XMLsection, "It", section.getIt());
				org.jdom.Element XMLshape = new org.jdom.Element("shape");
				setStringAttribute(XMLshape, "type", section.getSectionShape().getType().toString());
				setDoubleAttribute(XMLshape, "width", section.getSectionShape().getWidth());
				setDoubleAttribute(XMLshape, "height", section.getSectionShape().getHeight());
				setDoubleAttribute(XMLshape, "diameter", section.getSectionShape().getDiameter());
				setDoubleAttribute(XMLshape, "thickness", section.getSectionShape().getThickness());
				XMLsection.addContent(XMLshape);
				XMLsections.addContent(XMLsection);
			}		
			XMLmodel.addContent(XMLsections);
			
			/* steps */
			org.jdom.Element XMLsteps = new org.jdom.Element("steps");
			for (int i = 0; i < steps.size(); i++) {
				Step step = steps.get(i);
				org.jdom.Element XMLstep = new org.jdom.Element("step");
				setStringAttribute(XMLstep, "name", step.name);
				setStringAttribute(XMLstep, "type", step.type.toString());
				setDoubleAttribute(XMLstep, "duration", step.duration);
				setIntegerAttribute(XMLstep, "nIncrements", step.nIncrements);
				setIntegerAttribute(XMLstep, "maxIterations", step.maxIterations);
				setStringAttribute(XMLstep, "gravity", step.gravity.toString());
				setDoubleAttribute(XMLstep, "gValue", step.gValue);				
				XMLsteps.addContent(XMLstep);
			}
			XMLmodel.addContent(XMLsteps);
			
			doc.getRootElement().addContent(XMLmodel);
			
			/* solution */
			if (SimLive.post != null) {
				org.jdom.Element XMLsolution = new org.jdom.Element("solution");
				
				Solution solution = SimLive.post.getSolution();
				org.jdom.Element XMLincrements = new org.jdom.Element("increments");
				for (int i = 0; i < solution.getNumberOfIncrements()+1; i++) {
					org.jdom.Element XMLincrement = new org.jdom.Element("increment");
					setDoubleAttribute(XMLincrement, "time", solution.getIncrement(i).getTime());
					setIntegerAttribute(XMLincrement, "stepNr", solution.getIncrement(i).getStepNr());
					
					matrixArrayToXML(solution.getIncrement(i).get_M_elem(), new org.jdom.Element("M_elem"), XMLincrement);
					matrixArrayToXML(solution.getIncrement(i).get_K_elem(), new org.jdom.Element("K_elem"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_M_global(), new org.jdom.Element("M_global"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_K_global(), new org.jdom.Element("K_global"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_f_ext(), new org.jdom.Element("f_ext"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_f_int(), new org.jdom.Element("f_int"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_u_global(), new org.jdom.Element("u_global"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_v_global(), new org.jdom.Element("v_global"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_a_global(), new org.jdom.Element("a_global"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_G(), new org.jdom.Element("G"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_r_global(), new org.jdom.Element("r_global"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_K_constr(), new org.jdom.Element("K_constr"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_M_constr(), new org.jdom.Element("M_constr"), XMLincrement);
					matrixToXML(solution.getIncrement(i).get_delta_f_constr(), new org.jdom.Element("delta_f_constr"), XMLincrement);
					
					XMLincrements.addContent(XMLincrement);
				}
				XMLsolution.addContent(XMLincrements);
				
				matrixToXML(solution.getD(), new org.jdom.Element("D"), XMLsolution);
				matrixToXML(solution.getV(), new org.jdom.Element("V"), XMLsolution);
				
				stringListToXML(Solution.log, new org.jdom.Element("log"), XMLsolution);
				stringListToXML(Solution.errors, new org.jdom.Element("errors"), XMLsolution);
				stringListToXML(Solution.warnings, new org.jdom.Element("warnings"), XMLsolution);
				
				doc.getRootElement().addContent(XMLsolution);
			}
			
			/* settings */
			org.jdom.Element XMLsettings = new org.jdom.Element("settings");
			
			setStringAttribute(XMLsettings, "newPartType", settings.newPartType.toString());
			setDoubleAttribute(XMLsettings, "module", settings.module);
			setDoubleAttribute(XMLsettings, "pressureAngle", settings.pressureAngle);
			//setBooleanAttribute(XMLsettings, "isShiftForceVectors", settings.isShiftForceVectors);
			setStringAttribute(XMLsettings, "constraintType", settings.constraintType.toString());
			setDoubleAttribute(XMLsettings, "penaltyFactor", settings.penaltyFactor);
			setBooleanAttribute(XMLsettings, "isReorderNodes", settings.isReorderNodes);
			setBooleanAttribute(XMLsettings, "isLargeDisplacement", settings.isLargeDisplacement);
			setBooleanAttribute(XMLsettings, "isWriteMatrixView", settings.isWriteMatrixView);
			setStringAttribute(XMLsettings, "unitSystem", settings.unitSystem.toString());
			setDoubleAttribute(XMLsettings, "meshSize", settings.meshSize);
			setIntegerAttribute(XMLsettings, "meshCount", settings.meshCount);
			
			setBooleanAttribute(XMLsettings, "isShowAxes", settings.isShowAxes);
			setBooleanAttribute(XMLsettings, "isShowGrid", settings.isShowGrid);
			setBooleanAttribute(XMLsettings, "isShowScale", settings.isShowScale);
			setBooleanAttribute(XMLsettings, "isShowOrientations", settings.isShowOrientations);
			setBooleanAttribute(XMLsettings, "isShowNodes", settings.isShowNodes);
			setBooleanAttribute(XMLsettings, "isShowEdges", settings.isShowEdges);
			setBooleanAttribute(XMLsettings, "isShowSections", settings.isShowSections);
			setBooleanAttribute(XMLsettings, "isShowSupports", settings.isShowSupports);
			setBooleanAttribute(XMLsettings, "isShowLoads", settings.isShowLoads);
			//setBooleanAttribute(XMLsettings, "isShowReactions", settings.isShowReactions);
			
			doc.getRootElement().addContent(XMLsettings);
			
			// new XMLOutputter().output(doc, System.out);
			XMLOutputter xmlOutput = new XMLOutputter();

			// display nice nice
			Format format = Format.getPrettyFormat();
			format.setEncoding("ISO-8859-1");
			xmlOutput.setFormat(format);
			xmlOutput.output(doc, new FileWriter(fileName));
			
			/* this will undo the conversion for file writing */
			Mode mode = SimLive.mode;
			SimLive.mode = Mode.NONE;
			model.updateModel();
			model.deleteUnusedNodes();
			SimLive.mode = mode;
			
		}
		catch (IOException io) {
			System.out.println(io.getMessage());
		}
	}
	
	private static void convertRigidDeformableToDeformableDeformable() {
		ArrayList<ContactPair> rigidContactPairs = new ArrayList<ContactPair>();
		for (int c = 0; c < SimLive.model.getContactPairs().size(); c++) {
			if (SimLive.model.getContactPairs().get(c).getType() == Type.RIGID_DEFORMABLE) {
				rigidContactPairs.add(SimLive.model.getContactPairs().get(c));
			}
		}
		for (int c = 0; c < rigidContactPairs.size(); c++) {
			rigidContactPairs.get(c).setType(Type.DEFORMABLE_DEFORMABLE, true);
		}
		Mode mode = SimLive.mode;
		SimLive.mode = Mode.NONE;
		SimLive.model.updateModel();
		SimLive.mode = mode;
		for (int c = 0; c < rigidContactPairs.size(); c++) {
			rigidContactPairs.get(c).setType(Type.RIGID_DEFORMABLE, false);
		}
	}
	
	private static void setNodeSet(org.jdom.Element element, ArrayList<Node> nodeSet) {
		for (int n = 0; n < nodeSet.size(); n++) {
			org.jdom.Element XMLnode = new org.jdom.Element("node");
			setIntegerAttribute(XMLnode, "ID", nodeSet.get(n).getID());
			element.addContent(XMLnode);
		}
	}
	
	private static ArrayList<Node> getNodeSet(org.jdom.Element element) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		Object[] listNodes = element.getChildren().toArray();
		
		for (int j = 0; j < listNodes.length; j++) {
			org.jdom.Element XMLnode = (org.jdom.Element) listNodes[j];
			int nodeID = getIntegerAttribute(XMLnode, "ID");
			nodes.add(SimLive.model.getNodes().get(nodeID));
		}
		
		return nodes;
	}
	
	private static void setSubSets(org.jdom.Element XMLset, Set set) {
		setStringAttribute(XMLset, "type", set.getType().toString());
		setStringAttribute(XMLset, "view", set.view.toString());
		if (!set.getSets().isEmpty()) {
			org.jdom.Element XMLsets = new org.jdom.Element("sets");
			for (int s = 0; s < set.getSets().size(); s++) {
				org.jdom.Element XMLsubSet = new org.jdom.Element("set");
				setSubSets(XMLsubSet, set.getSets().get(s));
				XMLsets.addContent(XMLsubSet);
			}
			XMLset.addContent(XMLsets);
		}
		else {
			if (set.getSpurGearValues() != null) {
				org.jdom.Element XMLspurGearValues = new org.jdom.Element("spurGearValues");
				setDoubleAttribute(XMLspurGearValues, "pressureAngle", set.getSpurGearValues().getPressureAngle());
				setDoubleAttribute(XMLspurGearValues, "module", set.getSpurGearValues().getModule());
				setDoubleAttribute(XMLspurGearValues, "pitchRadius", set.getSpurGearValues().getPitchRadius());
				setBooleanAttribute(XMLspurGearValues, "isInternal", set.getSpurGearValues().isInternal());
				XMLset.addContent(XMLspurGearValues);
			}		
			org.jdom.Element XMLsetElements = new org.jdom.Element("elements");
			for (int e = 0; e < set.getElements().size(); e++) {
				org.jdom.Element XMLsetElement = new org.jdom.Element("element");
				setIntegerAttribute(XMLsetElement, "ID", set.getElements().get(e).getID());
				XMLsetElements.addContent(XMLsetElement);
			}
			XMLset.addContent(XMLsetElements);
		}
	}
	
	private static void getSubSets(org.jdom.Element XMLset, Set set) {
		ArrayList<Element> elements = new ArrayList<Element>();
		ArrayList<Set> sets = set.getSets();
		org.jdom.Element XMLsets = XMLset.getChild("sets");
		if (XMLsets != null) {
			Object[] list = XMLsets.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				org.jdom.Element XMLsubSet = (org.jdom.Element) list[i];
				Set.Type type = Set.Type.valueOf(getStringAttribute(XMLsubSet, "type"));
				Set subSet = new Set(type);
				subSet.view = Set.View.valueOf(getStringAttribute(XMLsubSet, "view"));
				getSubSets(XMLsubSet, subSet);
				sets.add(subSet);
				set.getElements().addAll(subSet.getElements());
			}
		}
		else {
			try {
				org.jdom.Element XMLspurGearValues = XMLset.getChild("spurGearValues");
				double pressureAngle = getDoubleAttribute(XMLspurGearValues, "pressureAngle");
				double module = getDoubleAttribute(XMLspurGearValues, "module");
				double pitchRadius = getDoubleAttribute(XMLspurGearValues, "pitchRadius");
				boolean isInternal = getBooleanAttribute(XMLspurGearValues, "isInternal");
				set.setSpurGearValues(new SpurGearValues(pressureAngle, module, pitchRadius, isInternal));
			}
			catch (Exception e) {}
			
			org.jdom.Element XMLsetElements = XMLset.getChild("elements");
			Object[] list = XMLsetElements.getChildren().toArray();
			
			for (int j = 0; j < list.length; j++) {

				org.jdom.Element XMLsetElement = (org.jdom.Element) list[j];
				Element element = SimLive.model.getElements().get(getIntegerAttribute(XMLsetElement, "ID"));
				elements.add(element);
			}
			
			Set.Type type = Set.Type.valueOf(getStringAttribute(XMLset, "type"));
			set.setType(type);
			set.getElements().addAll(elements);
		}
	}
	
	private static void setSubTree(org.jdom.Element XMLsubTree, SubTree subTree) {
		setIntegerAttribute(XMLsubTree, "nrVertices", subTree.nrVertices);
		setIntegerAttribute(XMLsubTree, "nrFacets", subTree.nrFacets);
		for (int s = 0; s < subTree.subTrees.size(); s++) {
			org.jdom.Element XMLsubSubTree = new org.jdom.Element("subTree");
			setSubTree(XMLsubSubTree, subTree.subTrees.get(s));
			XMLsubTree.addContent(XMLsubSubTree);
		}	
	}
	
	private static void getSubTree(org.jdom.Element XMLsubTree, SubTree subTree) {
		subTree.nrVertices = getIntegerAttribute(XMLsubTree, "nrVertices");
		subTree.nrFacets = getIntegerAttribute(XMLsubTree, "nrFacets");
		org.jdom.Element XMLsubSubTree = XMLsubTree.getChild("subTree");
		if (XMLsubSubTree != null) {
			Object[] list = XMLsubTree.getChildren().toArray();

			for (int i = 0; i < list.length; i++) {

				SubTree subSubTree = new SubTree();
				getSubTree((org.jdom.Element) list[i], subSubTree);
				subTree.subTrees.add(subSubTree);
			}
		}
	}
	
	private static void setTimeTable(org.jdom.Element element, TimeTable timeTable) {
		org.jdom.Element XMLtimeTable = new org.jdom.Element("timeTable");
		for (int n = 0; n < timeTable.getNumberOfRows(); n++) {
			org.jdom.Element XMLrow = new org.jdom.Element("row");
			setDoubleAttribute(XMLrow, "time", timeTable.getTime(n));
			setDoubleAttribute(XMLrow, "factor", timeTable.getFactor(n));
			XMLtimeTable.addContent(XMLrow);
		}
		element.addContent(XMLtimeTable);
	}
	
	private static TimeTable getTimeTable(org.jdom.Element XMLtimeTable) {
		Object[] list = XMLtimeTable.getChildren().toArray();
		
		double[] time = new double[list.length];
		double[] factor = new double[list.length];

		for (int i = 0; i < list.length; i++) {
			org.jdom.Element XMLrow = (org.jdom.Element) list[i];
			time[i] = getDoubleAttribute(XMLrow, "time");
			factor[i] = getDoubleAttribute(XMLrow, "factor");
		}
		
		return new TimeTable(time, factor);
	}
	
	private static void setDoubleAttribute(org.jdom.Element element,
			String attributeName, double value) {
		element.setAttribute(new Attribute(attributeName, Double.toString(value)));
	}
	
	private static double getDoubleAttribute(org.jdom.Element element, String attributeName) 
			throws NullPointerException, NumberFormatException {
		if (element.getAttributeValue(attributeName) == null) throw new NullPointerException();
		return Double.parseDouble(element.getAttributeValue(attributeName));
	}
	
	private static void setBooleanAttribute(org.jdom.Element element,
			String attributeName, boolean value) {
		element.setAttribute(new Attribute(attributeName, Boolean.toString(value)));
	}
	
	private static boolean getBooleanAttribute(org.jdom.Element element, String attributeName)
			throws NullPointerException {
		if (element.getAttributeValue(attributeName) == null) throw new NullPointerException();
		return Boolean.parseBoolean(element.getAttributeValue(attributeName));
	}
	
	private static void setIntegerAttribute(org.jdom.Element element,
			String attributeName, int value) {
		element.setAttribute(new Attribute(attributeName, Integer.toString(value)));
	}
	
	private static int getIntegerAttribute(org.jdom.Element element, String attributeName)
			throws NullPointerException, NumberFormatException {
		if (element.getAttributeValue(attributeName) == null) throw new NullPointerException();
		return Integer.parseInt(element.getAttributeValue(attributeName));
	}
	
	private static void setStringAttribute(org.jdom.Element element,
			String attributeName, String value) {
		element.setAttribute(new Attribute(attributeName, value));
	}
	
	private static String getStringAttribute(org.jdom.Element element, String attributeName)
			throws NullPointerException {
		if (element.getAttributeValue(attributeName) == null) throw new NullPointerException();
		return element.getAttributeValue(attributeName);
	}
	
	private static void matrixArrayToXML(Matrix[] matrixArray, org.jdom.Element element, org.jdom.Element parent) {
		if (matrixArray != null) {
			for (int i = 0; i < matrixArray.length; i++) {
				matrixToXML(matrixArray[i], new org.jdom.Element("entry"), element);
			}
			parent.addContent(element);
		}
	}
	
	private static Matrix[] XMLToMatrixArray(org.jdom.Element element) {
		if (element != null) {
			Object[] list = element.getChildren().toArray();
			Matrix[] matrixArray = new Matrix[list.length];
			for (int i = 0; i < list.length; i++) {
				matrixArray[i] = XMLToMatrix((org.jdom.Element) list[i]);
			}
			return matrixArray;
		}
		return null;
	}
	
	private static void matrixToXML(Matrix matrix, org.jdom.Element element, org.jdom.Element parent) {
		if (matrix != null) {
			setIntegerAttribute(element, "rows", matrix.getRowDimension());
			setIntegerAttribute(element, "columns", matrix.getColumnDimension());
			for (int c = 0; c < matrix.getColumnDimension(); c++) {
				String string = new String();
				for (int r = 0; r < matrix.getRowDimension(); r++) {
					string += Double.toString(matrix.get(r, c)) + " ";
				}
				org.jdom.Element column = new org.jdom.Element("column");
				setStringAttribute(column, "values", string);
				element.addContent(column);
			}
			parent.addContent(element);
		}
	}
	
	private static Matrix XMLToMatrix(org.jdom.Element element) {
		if (element != null) {
			int rows = getIntegerAttribute(element, "rows");
			int columns = getIntegerAttribute(element, "columns");
			Matrix matrix = new Matrix(rows, columns);
			Object[] list = element.getChildren().toArray();
			for (int c = 0; c < list.length; c++) {
				org.jdom.Element column = (org.jdom.Element) list[c];
				String string = getStringAttribute(column, "values");
				String[] splitString = string.split(" ");
				for (int r = 0; r < rows; r++) {
					matrix.set(r, c, Double.parseDouble(splitString[r]));
				}
			}
			return matrix;
		}
		return null;
	}

	private static void stringListToXML(ArrayList<String> stringList, org.jdom.Element element, org.jdom.Element parent) {
		for (int i = 0; i < stringList.size(); i++) {
			org.jdom.Element entry = new org.jdom.Element("entry");
			setStringAttribute(entry, "string", stringList.get(i));
			element.addContent(entry);
		}
		parent.addContent(element);
	}
	
	private static ArrayList<String> XMLToStringList(org.jdom.Element element) {
		ArrayList<String> stringList = new ArrayList<String>();
		Object[] list = element.getChildren().toArray();
		
		for (int i = 0; i < list.length; i++) {

			org.jdom.Element entry = (org.jdom.Element) list[i];
			
			stringList.add(getStringAttribute(entry, "string"));
		}
		return stringList;
	}
	
	public static String getFilePath() {
		return filePath;
	}

	public static void setFilePath(String filePath) {
		XML.filePath = filePath;
	}

}

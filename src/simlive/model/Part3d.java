package simlive.model;

import java.util.stream.Stream;

import Jama.Matrix;
import simlive.SimLive;
import simlive.SimLive.Mode;
import simlive.misc.GeomUtility;

public class Part3d implements DeepEqualsInterface {
	
	private Vertex3d[] vertices;
	private Facet3d[] facets;
	private SubTree subTree;
	private int id;
	private int[][] connect;
	private int[][] neighbourFacets;
	public enum Render {FILL, FILL_AND_WIREFRAME, WIREFRAME};
	public Render render = Render.FILL;
	public boolean doubleSided = false;
	private double[][] normals0;
	//subject to change in increments, do not check
	private double[][] normals;
	private double[][] vertexCoords;
	
	public Part3d(int nrVertices, int nrFacets) {
		vertices = new Vertex3d[nrVertices];
		facets = new Facet3d[nrFacets];
	}

	public Part3d clone() {
		Part3d part3d = new Part3d(vertices.length, facets.length);
		for (int i = 0; i < vertices.length; i++) {
			part3d.vertices[i] = vertices[i].clone();
		}
		for (int i = 0; i < facets.length; i++) {
			part3d.facets[i] = facets[i].clone();
		}
		part3d.subTree = subTree.clone();
		part3d.id = id;
		//do copy to have it in model history
		part3d.render = render;
		part3d.doubleSided = doubleSided;		
		//do not copy following to reduce data in model history
		/*if (connect != null) {
			part3d.connect = connect.clone();
		}
		if (neighbourFacets != null) {
			part3d.neighbourFacets = neighbourFacets.clone();
		}
		if (normals != null) {
			part3d.normals = normals.clone();
		}
		if (facetNormals != null) {
			part3d.facetNormals = facetNormals.clone();
		}
		if (vertexCoords != null) {
			part3d.vertexCoords = vertexCoords.clone();
		}*/
		return part3d;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Part3d part3d = (Part3d) obj;
		for (int i = 0; i < vertices.length; i++) {
			result = this.vertices[i].deepEquals(part3d.vertices[i], result);
		}
		for (int i = 0; i < facets.length; i++) {
			result = this.facets[i].deepEquals(part3d.facets[i], result);
		}
		result = this.subTree.deepEquals(part3d.subTree, result);
		if (this.id != part3d.id) return Result.RECALC;
		//do check to have it in model history
		if (this.render != part3d.render && result != Result.RECALC) result = Result.CHANGE;
		if (this.doubleSided != part3d.doubleSided && result != Result.RECALC) result = Result.CHANGE;
		//do not check following because it was not copied
		/*for (int i = 0; i < connect.length; i++) {
			if (!Arrays.equals(this.connect[i], part3d.connect[i])) return false;
		}
		for (int i = 0; i < neighbourFacets.length; i++) {
			if (!Arrays.equals(this.neighbourFacets[i], part3d.neighbourFacets[i])) return false;
		}*/
		//do not check following because it might detect a wrong model change
		/*for (int i = 0; i < normals.length; i++) {
			if (!Arrays.equals(this.normals[i], part3d.normals[i])) return false;
		}*/
		return result;
	}

	public SubTree getSubTree() {
		return subTree;
	}

	public void setSubTree(SubTree subTree) {
		this.subTree = subTree;
	}
	
	public double[][] getVertexCoords() {
		if (SimLive.mode == Mode.RESULTS) {
			return this.vertexCoords;
		}
		else {
			double[][] vertexCoords = new double[vertices.length][];
    		for (int v = 0; v < vertices.length; v++) {
        		vertexCoords[v] = vertices[v].getCoords();
    		}
    		return vertexCoords;
		}
	}

	public void setVertexCoords(double[][] vertexCoords) {
		this.vertexCoords = vertexCoords;
	}

	public Vertex3d[] getVertices() {
		return vertices;
	}
	
	public Facet3d[] getFacets() {
		return facets;
	}

	public Vertex3d getVertex(int i) {
		return vertices[i];
	}

	public void setVertex(Vertex3d vertex, int i) {
		vertex.setID(i);
		this.vertices[i] = vertex;
	}

	public Facet3d getFacet(int i) {
		return facets[i];
	}

	public void setFacet(Facet3d facet, int i) {
		facet.setID(i);
		this.facets[i] = facet;
	}
	
	public int getNrVertices() {
		return vertices.length;
	}
	
	public int getNrFacets() {
		return facets.length;
	}
	
	private double[] calculateFacetNormal(int i) {
		int[] indices = facets[i].getIndices();
		double[] diff0 = new double[3];
		diff0[0] = vertexCoords[indices[1]][0]-vertexCoords[indices[0]][0];
		diff0[1] = vertexCoords[indices[1]][1]-vertexCoords[indices[0]][1];
		diff0[2] = vertexCoords[indices[1]][2]-vertexCoords[indices[0]][2];
		double[] diff1 = new double[3];
		diff1[0] = vertexCoords[indices[2]][0]-vertexCoords[indices[0]][0];
		diff1[1] = vertexCoords[indices[2]][1]-vertexCoords[indices[0]][1];
		diff1[2] = vertexCoords[indices[2]][2]-vertexCoords[indices[0]][2];
		double[] facetNormal = new double[3];
		facetNormal[0] = diff0[1]*diff1[2]-diff0[2]*diff1[1];
		facetNormal[1] = diff0[2]*diff1[0]-diff0[0]*diff1[2];
		facetNormal[2] = diff0[0]*diff1[1]-diff0[1]*diff1[0];
		return facetNormal;
	}
	
	private double dotProductNormalized(double[] vec0, double[] vec1) {
		double l0_sqr = vec0[0]*vec0[0]+vec0[1]*vec0[1]+vec0[2]*vec0[2];
		double l1_sqr = vec1[0]*vec1[0]+vec1[1]*vec1[1]+vec1[2]*vec1[2];
		return (vec0[0]*vec1[0]+vec0[1]*vec1[1]+vec0[2]*vec1[2])/Math.sqrt(l0_sqr*l1_sqr);
	}
	
	private void initTopology() {
		connect = new int[vertices.length][0];
    	for (int f = 0; f < facets.length; f++) {
    		int[] indices = facets[f].getIndices();
    		for (int i = 0; i < 3; i++) {
    			connect[indices[i]] = SimLive.add(connect[indices[i]], f);
    			connect[indices[i]] = SimLive.add(connect[indices[i]], i);
    		}
    	}
    	
    	neighbourFacets = new int[facets.length][3];
    	for (int f = 0; f < facets.length; f++) {
    		int[] indices = facets[f].getIndices();
			for (int i = 0; i < 3; i++) {
				int j = (i+1)%3;
				neighbourFacets[f][i] = -1;
	    		for (int k = 0; k < connect[indices[i]].length/2; k++) {
					int f0 = connect[indices[i]][k*2];
					if (f != f0) {
						int[] indices0 = facets[f0].getIndices();
						if (SimLive.contains(indices0, indices[i]) && SimLive.contains(indices0, indices[j])) {
							neighbourFacets[f][i] = f0;
						}
					}
	    		}
			}
    	}
	}
	
	private void initNormals() {
		normals0 = new double[getNrFacets()*3][3];
		double[][] facetNormals = new double[getNrFacets()][];
		Stream<Facet3d> stream = Stream.of(facets).parallel();
		stream.forEach(facet -> {
			facetNormals[facet.getID()] = calculateFacetNormal(facet.getID());
		});
		stream = Stream.of(facets).parallel();
		stream.forEach(facet -> {
    		int[] indices = facet.getIndices();
    		double[] facetNormal0 = facetNormals[facet.getID()];
	    	for (int i = 0; i < 3; i++) {
		    	for (int j = 0; j < connect[indices[i]].length/2; j++) {
		    		int f1 = connect[indices[i]][j*2];
		    		double[] facetNormal1 = facetNormals[f1];
		    		if (dotProductNormalized(facetNormal0, facetNormal1) > 0.85) {
		    			int k = connect[indices[i]][j*2+1];
		    			synchronized(this){
			    			normals0[f1*3+k][0] += facetNormal0[0];
			    			normals0[f1*3+k][1] += facetNormal0[1];
			    			normals0[f1*3+k][2] += facetNormal0[2];
		    			}
		    		}
		    	}
    		}
    	});
		normals = normals0;
	}
	
	public void setNormals(double[][] normals) {
		this.normals = normals;
	}
	
	public double[][] getNormals0() {
		return normals0;
	}

	public double[][] getNormals() {
		return normals;
	}
	
	public void flip() {
		for (int f = 0; f < facets.length; f++) {
			int[] indices = facets[f].getIndices();
			facets[f] = new Facet3d(new int[]{indices[2], indices[1], indices[0]}, facets[f].getColorID());
			facets[f].setID(f);
		}
		connect = null;
		neighbourFacets = null;
	}
	
	public boolean isEdge(int facet, int i, double[] viewDir) {
		int f = neighbourFacets[facet][i];
		if (f == -1) return true;
		double[] facetNormal = calculateFacetNormal(facet);
		double scal = facetNormal[0]*viewDir[0]+facetNormal[1]*viewDir[1]+facetNormal[2]*viewDir[2];
		if (scal > 0) return false;
		double[] facetNormal0 = calculateFacetNormal(f);
		double scal0 = facetNormal0[0]*viewDir[0]+facetNormal0[1]*viewDir[1]+facetNormal0[2]*viewDir[2];
		if (scal0 > 0) return true;
		return false;
	}

	public void rotate(double angle, double[] rotPoint, double[] axis) {
		Matrix rot = GeomUtility.getRotationMatrix(angle, axis);
		Matrix p = new Matrix(rotPoint, 3);
		for (int v = 0; v < vertices.length; v++) {
			double[] oldCoords = vertices[v].getCoords();
			Matrix v0 = new Matrix(new double[]{oldCoords[0], oldCoords[1], oldCoords[2]}, 3).minus(p);
			double[] newCoords = rot.times(v0).plus(p).getColumnPackedCopy();
			vertices[v].setCoords(newCoords);
		}
	}
	
	public int getID() {
		return id;
	}
	
	public void update() {
		/* ID */
		id = SimLive.model.getParts3d().indexOf(this);
		
		if (connect == null && neighbourFacets == null) {
			initTopology();
		}
		
		double[][] vertexCoordsNew = new double[getNrVertices()][];
    	for (int v = 0; v < getNrVertices(); v++) {
    		vertexCoordsNew[v] = getVertex(v).getCoords();
		}
    	vertexCoords = vertexCoordsNew;
    	initNormals();
		
		/*for (int f = 0; f < facets.length; f++) {
			double[] v0 = getVertex(facets[f].getIndices()[0]).getCoords();
			double[] v1 = getVertex(facets[f].getIndices()[1]).getCoords();
			double[] v2 = getVertex(facets[f].getIndices()[2]).getCoords();
			
			double[] diff0 = new double[3];
			diff0[0] = v1[0]-v0[0];
			diff0[1] = v1[1]-v0[1];
			diff0[2] = v1[2]-v0[2];
			double[] diff1 = new double[3];
			diff1[0] = v2[0]-v0[0];
			diff1[1] = v2[1]-v0[1];
			diff1[2] = v2[2]-v0[2];
			double[] normal = new double[3];
			normal[0] = diff0[1]*diff1[2]-diff0[2]*diff1[1];
			normal[1] = diff0[2]*diff1[0]-diff0[0]*diff1[2];
			normal[2] = diff0[0]*diff1[1]-diff0[1]*diff1[0];
			double length = Math.sqrt(normal[0]*normal[0]+normal[1]*normal[1]+normal[2]*normal[2]);
			if (length > 0.0) {
				double normalZ = normal[2]/length;
				facets[f].setNormalZ(normalZ);
			}
		}*/
		
		/*int[][] outlineEdge = new int[vertices.length][0];
		for (int f = 0; f < facets.length; f++) {
			if (facets[f].getNormalZ() > 0.01) {
				for (int i = 0; i < 3; i++) {
					int n0 = facets[f].getIndices()[i];
					int n1 = facets[f].getIndices()[(i+1)%3];
					outlineEdge[n0] = SimLive.add(outlineEdge[n0], n1);
					if (SimLive.contains(outlineEdge[n1], n0)) {
						outlineEdge[n1] = SimLive.remove(outlineEdge[n1], n0);
						outlineEdge[n0] = SimLive.remove(outlineEdge[n0], n1);
					}
				}
			}
		}*/
		/*for (int f = 0; f < facets.length; f++) {
			if (facets[f].getNormalZ() > 0.0) {
				for (int i = 0; i < 3; i++) {
					distToEdge[f*3+i] = Double.MAX_VALUE;
					int n0 = facets[f].getIndices()[(i+1)%3];
					int n1 = facets[f].getIndices()[(i+2)%3];
					if (Sim2d.contains(outlineEdge[n0], n1)) {
						double[] v0 = getVertex(facets[f].getIndices()[i]).getCoords();
						double[] v1 = getVertex(n0).getCoords();
						double[] v2 = getVertex(n1).getCoords();					
						distToEdge[f*3+i] = distToEdge(v0, v1, v2);
					}
				}
			}
		}*/
	}

}

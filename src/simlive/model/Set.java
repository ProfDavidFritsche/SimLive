package simlive.model;

import java.util.ArrayList;

import simlive.SimLive;

public class Set implements DeepEqualsInterface {

	/* BASIC: single truss, spring, beam, quad element or distributed load
	 * COMPOSITE: merged together
	 * CIRCULAR: to identify circular geometry
	 * RECTANGULAR: to identify rectangular geometry
	 */
	public enum Type {BASIC, COMPOSITE, CIRCULAR};
	private Type type;
	private ArrayList<Element> elements;
	private ArrayList<Node> nodes;
	private ArrayList<Set> sets;
	private int id;
	private SpurGearValues spurGearValues;
	public enum View {DEFAULT, HIDDEN, PINNED};
	public View view = View.DEFAULT;
	
	public Set(Type type) {
		this.elements = new ArrayList<Element>();
		this.nodes = new ArrayList<Node>();
		this.sets = new ArrayList<Set>();
		this.type = type;
	}
	
	public Set(Element element, Type type) {
		this.elements = new ArrayList<Element>();
		this.nodes = new ArrayList<Node>();
		this.sets = new ArrayList<Set>();
		this.elements.add(element);
		this.type = type;
		update();
	}
	
	public Set(ArrayList<Element> elements, Type type) {
		this.elements = new ArrayList<Element>();
		this.nodes = new ArrayList<Node>();
		this.sets = new ArrayList<Set>();
		this.elements.addAll(elements);
		this.type = type;
		update();
	}
	
	public Set(ArrayList<Element> elements, ArrayList<Set> sets, Type type) {
		this.elements = new ArrayList<Element>();
		this.nodes = new ArrayList<Node>();
		this.sets = new ArrayList<Set>();
		this.sets.addAll(sets);
		this.elements.addAll(elements);
		this.type = type;
		update();
	}
	
	public Set clone(Model model) {
		Set set = new Set(this.type);
		for (int i = 0; i < this.elements.size(); i++) {
			int id = this.elements.get(i).getID();
			if (id < model.getElements().size()) {
				set.elements.add(model.getElements().get(id));
			}
		}
		for (int i = 0; i < this.nodes.size(); i++) {
			int id = this.nodes.get(i).getID();
			if (id < model.getNodes().size()) {
				set.nodes.add(model.getNodes().get(id));
			}
		}
		for (int i = 0; i < this.sets.size(); i++) {
			set.sets.add(this.sets.get(i).clone(model));
		}
		set.id = this.id;
		if (this.spurGearValues != null) {
			set.spurGearValues = this.spurGearValues.clone(model);
		}
		set.view = this.view;
		return set;
	}
	
	public Result deepEquals(Object obj, Result result) {
		Set set = (Set) obj;
		result = SimLive.deepEquals(elements, set.elements, result);
		result = SimLive.deepEquals(nodes, set.nodes, result);
		result = SimLive.deepEquals(sets, set.sets, result);
		if (this.type != set.type) return Result.RECALC;
		if (this.id != set.id) return Result.RECALC;
		if (this.spurGearValues != null && set.spurGearValues != null) {
			result = this.spurGearValues.deepEquals(set.spurGearValues, result);
		}
		if (this.view != set.view && result != Result.RECALC) result = Result.CHANGE;
		return result;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public ArrayList<Set> getSets() {
		return sets;
	}
	
	public ArrayList<Element> getElements() {
		return elements;
	}
	
	public ArrayList<Node> getNodes() {
		return nodes;
	}
	
	public ArrayList<Element> update() {
		if (!sets.isEmpty()) {
			elements.clear();
		}
		for (int s = 0; s < sets.size(); s++) {
			elements.addAll(sets.get(s).update());
		}
		
		/* ID */
		id = SimLive.model.getSets().indexOf(this);
		
		/* nodes */
		this.nodes.clear();
		for (int e = 0; e < elements.size(); e++) {
			int[] element_nodes = elements.get(e).getElementNodes();
			for (int i = 0; i < element_nodes.length; i++) {
				if (!this.nodes.contains(SimLive.model.getNodes().get(element_nodes[i]))) {
					this.nodes.add(SimLive.model.getNodes().get(element_nodes[i]));
				}
			}
		}
		for (int d = 0; d < SimLive.model.getDistributedLoads().size(); d++) {
			DistributedLoad load = SimLive.model.getDistributedLoads().get(d);			
			for (int s = 0; s < load.getElementSets().size(); s++) {
				ArrayList<Element> elements = load.getElementSets().get(s).getElements();
				if (this.elements.containsAll(elements)) {
					for (int e = 1; e < elements.size()-1; e++) {
						int[] element_nodes = elements.get(e).getElementNodes();
						this.nodes.remove(SimLive.model.getNodes().get(element_nodes[0]));
						this.nodes.remove(SimLive.model.getNodes().get(element_nodes[1]));
					}
				}
			}
		}
		
		return elements;
	}
	
	public int getID() {
		return id;
	}
	
	public void setSpurGearValues(SpurGearValues spurGearValues) {
		this.spurGearValues = spurGearValues;
	}
	
	public SpurGearValues getSpurGearValues() {
		return this.spurGearValues;
	}

}

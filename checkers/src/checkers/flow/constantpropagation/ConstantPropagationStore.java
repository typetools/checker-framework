package checkers.flow.constantpropagation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import checkers.flow.analysis.Store;
import checkers.flow.cfg.node.Node;

public class ConstantPropagationStore implements Store<Constant> {

	/** Information about variables gathered so far. */
	Map<String, Constant> contents;
	
	/** Information about nodes. */
	Map<Node, Constant> nodeContents;
	
	public ConstantPropagationStore() {
		contents = new HashMap<>();
		nodeContents = new HashMap<>();
	}

	protected ConstantPropagationStore(Map<String, Constant> contents) {
		this.contents = contents;
		nodeContents = new HashMap<>();
	}
	
	public void setNodeInformation(Node n, Constant val) {
		nodeContents.put(n, val);
	}
	
	public Constant getNodeInformation(Node n) {
		return nodeContents.get(n);
	}
	
	public void addInformation(String var, Constant val) {
		Constant value;
		if (contents.containsKey(var)) {
			value = val.leastUpperBound(contents.get(var));
		} else {
			value = val;
		}
		contents.put(var, value);
	}
	
	public void setInformation(String var, Constant val) {
		contents.put(var, val);
	}

	@Override
	public ConstantPropagationStore copy() {
		return new ConstantPropagationStore(new HashMap<>(contents));
	}

	@Override
	public ConstantPropagationStore leastUpperBound(Store<Constant> o) {
		assert o instanceof ConstantPropagationStore;
		ConstantPropagationStore other = (ConstantPropagationStore) o;
		Map<String, Constant> newContents = new HashMap<>();

		// go through all of the information of the other class
		for (Entry<String, Constant> e : other.contents.entrySet()) {
			String var = e.getKey();
			Constant otherVal = e.getValue();
			if (contents.containsKey(var)) {
				// merge if both contain information about a variable
				newContents.put(var,
						otherVal.leastUpperBound(contents.get(var)));
			} else {
				// add new information
				newContents.put(var, otherVal);
			}
		}

		for (Entry<String, Constant> e : contents.entrySet()) {
			String var = e.getKey();
			Constant thisVal = e.getValue();
			if (!other.contents.containsKey(var)) {
				// add new information
				newContents.put(var, thisVal);
			}
		}

		return new ConstantPropagationStore(newContents);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof ConstantPropagationStore))
			return false;
		ConstantPropagationStore other = (ConstantPropagationStore) o;
		// go through all of the information of the other object
		for (Entry<String, Constant> e : other.contents.entrySet()) {
			String var = e.getKey();
			Constant otherVal = e.getValue();
			if (otherVal.isBottom())
				continue; // no information
			if (contents.containsKey(var)) {
				if (!otherVal.equals(contents.get(var))) {
					return false;
				}
			} else {
				return false;
			}
		}
		// go through all of the information of the this object
		for (Entry<String, Constant> e : contents.entrySet()) {
			String var = e.getKey();
			Constant thisVal = e.getValue();
			if (thisVal.isBottom())
				continue; // no information
			if (other.contents.containsKey(var)) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int s = 0;
		for (Entry<String, Constant> e : contents.entrySet()) {
			if (!e.getValue().isBottom()) {
				s += e.hashCode();
			}
		}
		return s;
	}
	
	@Override
	public String toString() {
		return contents.toString();
	}

}

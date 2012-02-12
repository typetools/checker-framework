package checkers.flow.analysis.checkers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.Element;

import checkers.flow.analysis.Store;
import checkers.flow.cfg.node.LocalVariableNode;

/**
 * A store for the checker framework analysis tracks the annotations of memory
 * locations such as local variables and fields.
 * 
 * TODO: Extend {@link CFStore} to track class member fields in the same way as
 * variables.
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 */
public class CFStore implements Store<CFStore> {

	/**
	 * The analysis class this store belongs to.
	 */
	protected final CFAnalysis analysis;

	/**
	 * Information collected about local variables, which are identified by the
	 * corresponding element.
	 */
	protected Map<Element, CFValue> localVariableValues;

	public CFStore(CFAnalysis analysis) {
		this.analysis = analysis;
		localVariableValues = new HashMap<>();
	}

	/** Copy constructor. */
	protected CFStore(CFAnalysis analysis, CFStore other) {
		this.analysis = analysis;
		localVariableValues = new HashMap<>(other.localVariableValues);
	}

	/**
	 * @return Current abstract value of a local variable, or {@code null} if no
	 *         information is available.
	 */
	public/* @Nullable */CFValue getValue(LocalVariableNode n) {
		Element el = n.getElement();
		return localVariableValues.get(el);
	}

	/**
	 * Set the abstract value of a local variable in the store. Overwrites any
	 * value that might have been available previously.
	 */
	public void setValue(LocalVariableNode n, CFValue val) {
		assert val != null;
		localVariableValues.put(n.getElement(), val);
	}

	/**
	 * Merge in an abstract value of a local variable in the store by taking the
	 * least upper bound of the previous value and {@code val}. Previous
	 * information needs to be available.
	 */
	public void mergeValue(LocalVariableNode n, CFValue val) {
		Element el = n.getElement();
		assert localVariableValues.containsKey(el);
		CFValue newVal = val.leastUpperBound(localVariableValues.get(el));
		localVariableValues.put(el, newVal);
	}

	@Override
	public CFStore copy() {
		return new CFStore(analysis, this);
	}

	@Override
	public CFStore leastUpperBound(CFStore other) {
		CFStore newStore = new CFStore(analysis);

		for (Entry<Element, CFValue> e : other.localVariableValues.entrySet()) {
			// local variables that are only part of one store, but not the
			// other are discarded. They are assumed to not be in scope any
			// more.
			Element el = e.getKey();
			if (localVariableValues.containsKey(el)) {
				CFValue otherVal = e.getValue();
				CFValue thisVal = localVariableValues.get(el);
				CFValue mergedVal = thisVal.leastUpperBound(otherVal);
				newStore.localVariableValues.put(el, mergedVal);
			}
		}

		return newStore;
	}

	/**
	 * Returns true iff this {@link CFStore} contains a superset of the map
	 * entries of the argument {@link CFStore}. Note that we test the entry keys
	 * and values by Java equality, not by any subtype relationship. This method
	 * is used primarily to simplify the equals predicate.
	 */
	protected boolean supersetOf(CFStore other) {
		for (Entry<Element, CFValue> e : other.localVariableValues.entrySet()) {
			Element key = e.getKey();
			if (!localVariableValues.containsKey(key)
					|| !localVariableValues.get(key).equals(e.getValue())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof CFStore) {
			CFStore other = (CFStore) o;
			return this.supersetOf(other) && other.supersetOf(this);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("CFStore (\\n");
		for (Map.Entry<Element, CFValue> entry : localVariableValues.entrySet()) {
			result.append("  " + entry.getKey() + " > " + entry.getValue()
					+ "\\n");
		}
		result.append(")");
		return result.toString();
	}
}
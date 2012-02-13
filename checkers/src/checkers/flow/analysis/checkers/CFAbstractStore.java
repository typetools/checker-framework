package checkers.flow.analysis.checkers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.Element;

import checkers.flow.analysis.Store;
import checkers.flow.cfg.node.ExplicitThisNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.util.HashCodeUtils;

import com.sun.org.apache.bcel.internal.classfile.Unknown;

/**
 * A store for the checker framework analysis tracks the annotations of memory
 * locations such as local variables and fields.
 * 
 * TODO: Extend {@link CFAbstractStore} to track class member fields in the same
 * way as variables.
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 */
public abstract class CFAbstractStore<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>>
		implements Store<S> {

	public static abstract class Receiver {
		public boolean containsUnknown() {
			return contains(new Unkown());
		}

		/**
		 * Does {@code other} appear anywhere in this receiver?
		 */
		public boolean contains(Receiver other) {
			return this.equals(other);
		}
	}

	public static class FieldAccess extends Receiver {
		protected Receiver receiver;
		protected Element field;

		public Receiver getReceiver() {
			return receiver;
		}

		public Element getField() {
			return field;
		}

		public FieldAccess(Receiver receiver, Element field) {
			this.receiver = receiver;
			this.field = field;
		}

		@Override
		public boolean contains(Receiver other) {
			return super.contains(other) || receiver.contains(other);
		}
	}

	public static class ThisReference extends Receiver {
		@Override
		public boolean equals(Object obj) {
			return obj != null && obj instanceof ThisReference;
		}

		@Override
		public int hashCode() {
			return HashCodeUtils.hash(0);
		}
	}

	public static class Unkown extends Receiver {
		@Override
		public boolean equals(Object obj) {
			return obj != null && obj instanceof Unkown;
		}

		@Override
		public int hashCode() {
			return HashCodeUtils.hash(1);
		}
	}

	public static class LocalVariable extends Receiver {
		protected Element element;

		public LocalVariable(Element element) {
			this.element = element;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof LocalVariable)) {
				return false;
			}
			LocalVariable other = (LocalVariable) obj;
			return other.element.equals(element);
		}

		@Override
		public int hashCode() {
			return HashCodeUtils.hash(element);
		}
	}

	// TODO: add pure method calls later
	public static class PureMethodCall extends Receiver {
	}

	/**
	 * The analysis class this store belongs to.
	 */
	protected final CFAbstractAnalysis<V, S, ?> analysis;

	/**
	 * Information collected about local variables, which are identified by the
	 * corresponding element.
	 */
	protected final Map<Element, V> localVariableValues;

	/**
	 * Information collected about fields, using the interal representation
	 * {@link FieldAccess}.
	 */
	protected Map<FieldAccess, V> fieldValues;

	public CFAbstractStore(CFAbstractAnalysis<V, S, ?> analysis) {
		this.analysis = analysis;
		localVariableValues = new HashMap<>();
		fieldValues = new HashMap<>();
	}

	/** Copy constructor. */
	protected CFAbstractStore(CFAbstractStore<V, S> other) {
		this.analysis = other.analysis;
		localVariableValues = new HashMap<>(other.localVariableValues);
		fieldValues = new HashMap<>(other.fieldValues);
	}

	/* --------------------------------------------------------- */
	/* Handling of fields */
	/* --------------------------------------------------------- */

	/**
	 * @return The internal representation (as {@link FieldAccess}) of a
	 *         {@link FieldAccessNode}. Can contain {@link Unknown} as receiver.
	 */
	protected FieldAccess internalReprOf(FieldAccessNode node) {
		Receiver receiver;
		Node receiverNode = node.getReceiver();
		if (receiverNode instanceof FieldAccessNode) {
			receiver = internalReprOf((FieldAccessNode) receiverNode);
		} else if (receiverNode instanceof ImplicitThisLiteralNode
				|| receiverNode instanceof ExplicitThisNode) {
			receiver = new ThisReference();
		} else if (receiverNode instanceof LocalVariableNode) {
			LocalVariableNode lv = (LocalVariableNode) receiverNode;
			receiver = new LocalVariable(lv.getElement());
		} else {
			receiver = new Unkown();
		}
		return new FieldAccess(receiver, null);
	}

	/**
	 * @return Current abstract value of a field access, or {@code null} if no
	 *         information is available.
	 */
	public/* @Nullable */V getValue(FieldAccessNode n) {
		FieldAccess fieldAccess = internalReprOf(n);
		return fieldValues.get(fieldAccess);
	}

	/**
	 * Update the information in the store by considering a field assignment
	 * with target {@code n}, where the right hand side has the abstract value
	 * {@code val}.
	 */
	public void updateForAssignemnt(FieldAccessNode n, V val) {
		assert val != null;
		FieldAccess fieldAccess = internalReprOf(n);
		removeConflicting(fieldAccess, val);
		if (!fieldAccess.containsUnknown()) {
			fieldValues.put(fieldAccess, val);
		}
	}

	/**
	 * Remove any information in {@code fieldValues} that might not be true any
	 * more after the fact that {@code fieldAccess} has abstract value
	 * {@code val} will be added. This includes the following steps:
	 * <ol>
	 * <li value="1">Update the abstract value of other field accesses where the
	 * field is equal, and the receiver might alias the receiver of
	 * {@code fieldAccess}. This update will raise the abstract value for such
	 * field accesses to at least {@code val} (or the old value, if that was
	 * less precise).</li>
	 * <li value="2">Remove any abstract values for field accesses where
	 * {@code fieldAccess} appear anywhere in the receiver.</li>
	 * </ol>
	 */
	protected void removeConflicting(FieldAccess fieldAccess, V val) {
		Map<FieldAccess, V> newFieldValues = new HashMap<>();
		for (Entry<FieldAccess, V> e : fieldValues.entrySet()) {
			FieldAccess otherFieldAccess = e.getKey();
			V otherVal = e.getValue();
			// case 2:
			if (otherFieldAccess.contains(fieldAccess)) {
				continue; // remove information completely
			}
			// case 1:
			if (fieldAccess.getField().equals(otherFieldAccess.getField())) {
				if (canAlias(fieldAccess.getReceiver(),
						otherFieldAccess.getReceiver())) {
					newFieldValues.put(otherFieldAccess,
							val.leastUpperBound(otherVal));
				}
			}
		}
		fieldValues = newFieldValues;
	}

	protected boolean canAlias(Receiver a, Receiver b) {
		// TODO more accurate (at least include type information)
		return true;
	}

	/* --------------------------------------------------------- */
	/* Handling of local variables */
	/* --------------------------------------------------------- */

	/**
	 * @return Current abstract value of a local variable, or {@code null} if no
	 *         information is available.
	 */
	public/* @Nullable */V getValue(LocalVariableNode n) {
		Element el = n.getElement();
		return localVariableValues.get(el);
	}

	/**
	 * Set the abstract value of a local variable in the store. Overwrites any
	 * value that might have been available previously.
	 */
	public void setValue(LocalVariableNode n, V val) {
		assert val != null;
		localVariableValues.put(n.getElement(), val);
	}

	/* --------------------------------------------------------- */
	/* Helper and miscellaneous methods */
	/* --------------------------------------------------------- */

	@SuppressWarnings("unchecked")
	@Override
	public S copy() {
		return analysis.createCopiedStore((S) this);
	}

	@Override
	public S leastUpperBound(S other) {
		S newStore = analysis.createEmptyStore();

		for (Entry<Element, V> e : other.localVariableValues.entrySet()) {
			// local variables that are only part of one store, but not the
			// other are discarded. They are assumed to not be in scope any
			// more.
			Element el = e.getKey();
			if (localVariableValues.containsKey(el)) {
				V otherVal = e.getValue();
				V thisVal = localVariableValues.get(el);
				V mergedVal = thisVal.leastUpperBound(otherVal);
				newStore.localVariableValues.put(el, mergedVal);
			}
		}

		return newStore;
	}

	/**
	 * Returns true iff this {@link CFAbstractStore} contains a superset of the
	 * map entries of the argument {@link CFAbstractStore}. Note that we test
	 * the entry keys and values by Java equality, not by any subtype
	 * relationship. This method is used primarily to simplify the equals
	 * predicate.
	 */
	protected boolean supersetOf(CFAbstractStore<V, S> other) {
		for (Entry<Element, V> e : other.localVariableValues.entrySet()) {
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
		if (o != null && o instanceof CFAbstractStore) {
			@SuppressWarnings("unchecked")
			CFAbstractStore<V, S> other = (CFAbstractStore<V, S>) o;
			return this.supersetOf(other) && other.supersetOf(this);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("CFStore (\\n");
		for (Map.Entry<Element, V> entry : localVariableValues.entrySet()) {
			result.append("  " + entry.getKey() + " > " + entry.getValue()
					+ "\\n");
		}
		result.append(")");
		return result.toString();
	}
}
package checkers.flow.analysis.checkers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import checkers.flow.analysis.Store;
import checkers.flow.cfg.node.ClassNameNode;
import checkers.flow.cfg.node.ExplicitThisNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.util.HashCodeUtils;

/**
 * A store for the checker framework analysis tracks the annotations of memory
 * locations such as local variables and fields.
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 */
// TODO: this class should be split into parts that are reusable generally, and
// parts specific to the checker framework
public abstract class CFAbstractStore<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>>
        implements Store<S> {

    public static abstract class Receiver {
        protected final TypeMirror type;

        public Receiver(TypeMirror type) {
            this.type = type;
        }

        public TypeMirror getType() {
            return type;
        }

        public abstract boolean containsUnknown();

        /**
         * @return True if and only if the two receiver are syntactically
         *         identical.
         */
        public boolean syntacticEquals(Receiver other) {
            return other == this;
        }

        /**
         * @return True if and only if this receiver contains a receiver that is
         *         syntactically equal to {@code other}.
         */
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other);
        }

        /**
         * Returns true if and only if {@code other} appear anywhere in this
         * receiver or an expression appears in this receiver such that
         * {@code other} might alias this expression.
         * 
         * <p>
         * 
         * Informal examples include:
         * 
         * <pre>
         *   "a".containsAliasOf("a") == true
         *   "x.f".containsAliasOf("x.f") == true
         *   "x.f".containsAliasOf("y.g") == false
         *   "x.f".containsAliasOf("a") == true // unless information about "x != a" is available
         *   "?".containsAliasOf("a") == true // ? is Unknown, and a can be anything
         * </pre>
         */
        public boolean containsAliasOf(CFAbstractStore<?, ?> store,
                Receiver other) {
            return this.equals(other) || store.canAlias(this, other);
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

        public FieldAccess(Receiver receiver, FieldAccessNode node) {
            super(node.getType());
            this.receiver = receiver;
            this.field = node.getElement();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof FieldAccess)) {
                return false;
            }
            FieldAccess fa = (FieldAccess) obj;
            return fa.getField().equals(getField())
                    && fa.getReceiver().equals(getReceiver());
        }
        
        @Override
        public int hashCode() {
            return HashCodeUtils.hash(getField(), getReceiver());
        }

        @Override
        public boolean containsAliasOf(CFAbstractStore<?, ?> store,
                Receiver other) {
            return super.containsAliasOf(store, other)
                    || receiver.containsAliasOf(store, other);
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other)
                    || receiver.containsSyntacticEqualReceiver(other);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof FieldAccess)) {
                return false;
            }
            FieldAccess fa = (FieldAccess) other;
            return super.syntacticEquals(other)
                    || fa.getField().equals(getField())
                    && fa.getReceiver().syntacticEquals(getReceiver());
        }

        @Override
        public String toString() {
            return receiver + "." + field;
        }

        @Override
        public boolean containsUnknown() {
            return receiver.containsUnknown();
        }
    }

    public static class ThisReference extends Receiver {
        public ThisReference(TypeMirror type) {
            super(type);
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof ThisReference;
        }

        @Override
        public int hashCode() {
            return HashCodeUtils.hash(0);
        }

        @Override
        public String toString() {
            return "this";
        }

        @Override
        public boolean containsUnknown() {
            return false;
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return other instanceof ThisReference;
        }
    }

    /**
     * A ClassName represents the occurrence of a class as part of a static
     * field access or method invocation.
     */
    public static class ClassName extends Receiver {
        protected Element element;

        public ClassName(TypeMirror type, Element element) {
            super(type);
            this.element = element;
        }

        public Element getElement() {
            return element;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null && !(obj instanceof ClassName)) {
                return false;
            }
            ClassName other = (ClassName) obj;
            return getElement().equals(other.getElement());
        }

        @Override
        public int hashCode() {
            return HashCodeUtils.hash(getElement());
        }

        @Override
        public String toString() {
            return getElement().getSimpleName().toString();
        }

        @Override
        public boolean containsUnknown() {
            return false;
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return this.equals(other);
        }
    }

    public static class Unknown extends Receiver {
        public Unknown(TypeMirror type) {
            super(type);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            return "?";
        }

        @Override
        public boolean containsAliasOf(CFAbstractStore<?, ?> store,
                Receiver other) {
            return true;
        }

        @Override
        public boolean containsUnknown() {
            return true;
        }

    }

    public static class LocalVariable extends Receiver {
        protected Element element;

        public LocalVariable(LocalVariableNode localVar) {
            super(localVar.getType());
            this.element = localVar.getElement();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof LocalVariable)) {
                return false;
            }
            LocalVariable other = (LocalVariable) obj;
            return other.element.equals(element);
        }

        public Element getElement() {
            return element;
        }

        @Override
        public int hashCode() {
            return HashCodeUtils.hash(element);
        }

        @Override
        public String toString() {
            return element.toString();
        }

        @Override
        public boolean containsUnknown() {
            return false;
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof LocalVariable)) {
                return false;
            }
            LocalVariable l = (LocalVariable) other;
            return l.getElement().equals(getElement());
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other);
        }
    }

    // TODO: add pure method calls later
    public static class PureMethodCall extends Receiver {

        public PureMethodCall(TypeMirror type) {
            super(type);
        }

        @Override
        public boolean containsUnknown() {
            return false; // TODO: correct implementation
        }
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

    /* --------------------------------------------------------- */
    /* Initialization */
    /* --------------------------------------------------------- */

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

    /**
     * Set the abstract value of a method parameter (only adds the information
     * to the store, does not remove any other knowledge).
     */
    public void initializeMethodParameter(LocalVariableNode p, V value) {
        localVariableValues.put(p.getElement(), value);
    }

    /* --------------------------------------------------------- */
    /* Handling of fields */
    /* --------------------------------------------------------- */

    // TODO: add MethodCallNode as parameter and check for pure-ity
    public void updateForMethodCall() {
        fieldValues = new HashMap<>();
    }

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
            receiver = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof LocalVariableNode) {
            LocalVariableNode lv = (LocalVariableNode) receiverNode;
            receiver = new LocalVariable(lv);
        } else if (receiverNode instanceof ClassNameNode) {
            ClassNameNode cn = (ClassNameNode) receiverNode;
            receiver = new ClassName(cn.getType(), cn.getElement());
        } else {
            receiver = new Unknown(receiverNode.getType());
        }
        return new FieldAccess(receiver, node);
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
     * 
     * @param val
     *            The abstract value of the value assigned to {@code n} (or
     *            {@code null} if the abstract value is not known).
     */
    public void updateForAssignment(FieldAccessNode n, /* @Nullable */V val) {
        assert val != null;
        FieldAccess fieldAccess = internalReprOf(n);
        removeConflicting(fieldAccess, val);
        if (!fieldAccess.containsUnknown() && val != null) {
            fieldValues.put(fieldAccess, val);
        }
    }

    /**
     * Update the information in the store by considering an assignment with
     * target {@code n}, where the target is neither a local variable nor a
     * field access. This includes the following steps:
     * 
     * <ol>
     * <li value="1">Remove any abstract values for field accesses <em>b.g</em>
     * where {@code n} might alias any expression in the receiver <em>b</em>.</li>
     * </ol>
     */
    public void updateForUnknownAssignment(Node n) {
        Unknown unknown = new Unknown(n.getType());
        Map<FieldAccess, V> newFieldValues = new HashMap<>();
        for (Entry<FieldAccess, V> e : fieldValues.entrySet()) {
            FieldAccess otherFieldAccess = e.getKey();
            V otherVal = e.getValue();
            // case 1:
            if (otherFieldAccess.getReceiver().containsAliasOf(this, unknown)) {
                continue; // remove information completely
            }
            newFieldValues.put(otherFieldAccess, otherVal);
        }
        fieldValues = newFieldValues;
    }

    /**
     * Remove any information in {@code fieldValues} that might not be true any
     * more after {@code fieldAccess} has been assigned a new value (with the
     * abstract value {@code val}). This includes the following steps (assume
     * that {@code fieldAccess} is of the form <em>a.f</em> for some <em>a</em>.
     * 
     * <ol>
     * <li value="1">Update the abstract value of other field accesses
     * <em>b.g</em> where the field is equal (that is, <em>f=g</em>), and the
     * receiver <em>b</em> might alias the receiver of {@code fieldAccess},
     * <em>a</em>. This update will raise the abstract value for such field
     * accesses to at least {@code val} (or the old value, if that was less
     * precise).</li>
     * <li value="2">Remove any abstract values for field accesses <em>b.g</em>
     * where {@code fieldAccess} is the same (i.e., <em>a=b</em> and
     * <em>f=g</em>), or where {@code fieldAccess} might alias any expression in
     * the receiver <em>b</em>.</li>
     * </ol>
     * 
     * @param val
     *            The abstract value of the value assigned to {@code n} (or
     *            {@code null} if the abstract value is not known).
     */
    protected void removeConflicting(FieldAccess fieldAccess, /* @Nullable */
            V val) {
        Map<FieldAccess, V> newFieldValues = new HashMap<>();
        for (Entry<FieldAccess, V> e : fieldValues.entrySet()) {
            FieldAccess otherFieldAccess = e.getKey();
            V otherVal = e.getValue();
            // case 2:
            if (otherFieldAccess.getReceiver().containsAliasOf(this,
                    fieldAccess)
                    || otherFieldAccess.equals(fieldAccess)) {
                continue; // remove information completely
            }
            // case 1:
            if (fieldAccess.getField().equals(otherFieldAccess.getField())) {
                if (canAlias(fieldAccess.getReceiver(),
                        otherFieldAccess.getReceiver())) {
                    if (val != null) {
                        newFieldValues.put(otherFieldAccess,
                                val.leastUpperBound(otherVal));
                    } else {
                        // remove information completely
                    }
                    continue;
                }
            }
            // information is save to be carried over
            newFieldValues.put(otherFieldAccess, otherVal);
        }
        fieldValues = newFieldValues;
    }

    /**
     * Remove any information in {@code fieldValues} that might not be true any
     * more after {@code localVar} has been assigned a new value. This includes
     * the following steps:
     * 
     * <ol>
     * <li value="1">Remove any abstract values for field accesses <em>b.g</em>
     * where {@code localVar} might alias any expression in the receiver
     * <em>b</em>.</li>
     * </ol>
     */
    protected void removeConflicting(LocalVariableNode localVar) {
        Map<FieldAccess, V> newFieldValues = new HashMap<>();
        LocalVariable var = new LocalVariable(localVar);
        for (Entry<FieldAccess, V> e : fieldValues.entrySet()) {
            FieldAccess otherFieldAccess = e.getKey();
            // case 1:
            if (otherFieldAccess.containsSyntacticEqualReceiver(var)) {
                continue;
            }
            newFieldValues.put(otherFieldAccess, e.getValue());
        }
        fieldValues = newFieldValues;
    }

    /**
     * Can the objects {@code a} and {@code b} be aliases? Returns a
     * conservative answer (i.e., returns {@code true} if not enough information
     * is available to determine aliasing).
     */
    protected boolean canAlias(Receiver a, Receiver b) {
        TypeMirror tb = b.getType();
        TypeMirror ta = a.getType();
        Types types = analysis.getTypes();
        return types.isSubtype(ta, tb) || types.isSubtype(tb, ta);
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
     * 
     * @param val
     *            The abstract value of the value assigned to {@code n} (or
     *            {@code null} if the abstract value is not known).
     */
    public void updateForAssignment(LocalVariableNode n, /* @Nullable */V val) {
        removeConflicting(n);
        if (val != null) {
            localVariableValues.put(n.getElement(), val);
        }
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
            // other are discarded, as one of store implicitly contains 'top'
            // for that variable.
            Element el = e.getKey();
            if (localVariableValues.containsKey(el)) {
                V otherVal = e.getValue();
                V thisVal = localVariableValues.get(el);
                V mergedVal = thisVal.leastUpperBound(otherVal);
                newStore.localVariableValues.put(el, mergedVal);
            }
        }
        for (Entry<FieldAccess, V> e : other.fieldValues.entrySet()) {
            // information about fields that are only part of one store, but not
            // the other are discarded, as one store implicitly contains 'top'
            // for that field.
            FieldAccess el = e.getKey();
            if (fieldValues.containsKey(el)) {
                V otherVal = e.getValue();
                V thisVal = fieldValues.get(el);
                V mergedVal = thisVal.leastUpperBound(otherVal);
                newStore.fieldValues.put(el, mergedVal);
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
        for (Entry<FieldAccess, V> e : other.fieldValues.entrySet()) {
            FieldAccess key = e.getKey();
            if (!fieldValues.containsKey(key)
                    || !fieldValues.get(key).equals(e.getValue())) {
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
        for (Entry<Element, V> entry : localVariableValues.entrySet()) {
            result.append("  " + entry.getKey() + " > " + entry.getValue()
                    + "\\n");
        }
        for (Entry<FieldAccess, V> entry : fieldValues.entrySet()) {
            result.append("  " + entry.getKey() + " > " + entry.getValue()
                    + "\\n");
        }
        result.append(")");
        return result.toString();
    }
}
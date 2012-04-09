package checkers.flow.analysis.checkers;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.sun.tools.javac.code.Type;

import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.Store;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.util.ValueParseUtil;
import checkers.quals.AssertAfter;
import checkers.quals.Pure;
import checkers.util.AnnotationUtils;
import checkers.util.TreeUtils;
import checkers.util.test.Odd;

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
     * {@link FlowExpressions.FieldAccess}.
     */
    protected Map<FlowExpressions.FieldAccess, V> fieldValues;

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

    /**
     * Remove any information that might not be valid any more after a method
     * call, and add information guaranteed by the method.
     */
    public void updateForMethodCall(MethodInvocationNode n) {
        ExecutableElement method = TreeUtils.elementFromUse(n.getTree());

        // remove information if necessary
        boolean isPure = analysis.factory.getDeclAnnotation(method, Pure.class) != null;
        if (!isPure) {
            fieldValues = new HashMap<>();
        }

        // add new information based on postcondition
        AnnotationMirror assertAfter = analysis.factory.getDeclAnnotation(
                method, AssertAfter.class);
        if (assertAfter != null) {
            List<String> expressions = AnnotationUtils.elementValueStringArray(
                    assertAfter, "expression");
            String annotation = AnnotationUtils.elementValueClassName(
                    assertAfter, "annotation");
            for (String e : expressions) {
                Node receiver = n.getTarget().getReceiver();
                FlowExpressions.Receiver r = ValueParseUtil.parse(e, receiver,
                        FlowExpressions.internalReprOf(receiver));
                if (r != null) {
                    insertValue(r,
                            analysis.factory.annotationFromName(annotation));
                }
            }
        }
    }

    /**
     * Add the annotation {@code a} for the expression {@code r} (correctly
     * deciding where to store the information depending on the type of the
     * expression {@code r}).
     */
    protected void insertValue(FlowExpressions.Receiver r, AnnotationMirror a) {
        V value = analysis.createAbstractValue(Collections.singleton(a));
        if (r instanceof FlowExpressions.LocalVariable) {
            Element localVar = ((FlowExpressions.LocalVariable) r).getElement();
            if (localVariableValues.containsKey(localVar)) {
                V mergedValue = localVariableValues.get(localVar)
                        .leastUpperBound(value);
                localVariableValues.put(localVar, mergedValue);
            } else {
                localVariableValues.put(localVar, value);
            }
        } else if (r instanceof FlowExpressions.FieldAccess) {
            FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) r;
            if (fieldValues.containsKey(fieldAcc)) {
                V mergedValue = fieldValues.get(fieldAcc)
                        .leastUpperBound(value);
                fieldValues.put(fieldAcc, mergedValue);
            } else {
                fieldValues.put(fieldAcc, value);
            }
        } else {
            assert false;
        }
    }

    /**
     * @return Current abstract value of a field access, or {@code null} if no
     *         information is available.
     */
    public/* @Nullable */V getValue(FieldAccessNode n) {
        FlowExpressions.FieldAccess fieldAccess = FlowExpressions
                .internalReprOfFieldAccess(n);
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
        FlowExpressions.FieldAccess fieldAccess = FlowExpressions
                .internalReprOfFieldAccess(n);
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
        FlowExpressions.Unknown unknown = new FlowExpressions.Unknown(
                n.getType());
        Map<FlowExpressions.FieldAccess, V> newFieldValues = new HashMap<>();
        for (Entry<FlowExpressions.FieldAccess, V> e : fieldValues.entrySet()) {
            FlowExpressions.FieldAccess otherFieldAccess = e.getKey();
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
    protected void removeConflicting(FlowExpressions.FieldAccess fieldAccess, /*
                                                                               * @
                                                                               * Nullable
                                                                               */
            V val) {
        Map<FlowExpressions.FieldAccess, V> newFieldValues = new HashMap<>();
        for (Entry<FlowExpressions.FieldAccess, V> e : fieldValues.entrySet()) {
            FlowExpressions.FieldAccess otherFieldAccess = e.getKey();
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
        Map<FlowExpressions.FieldAccess, V> newFieldValues = new HashMap<>();
        FlowExpressions.LocalVariable var = new FlowExpressions.LocalVariable(
                localVar);
        for (Entry<FlowExpressions.FieldAccess, V> e : fieldValues.entrySet()) {
            FlowExpressions.FieldAccess otherFieldAccess = e.getKey();
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
    public boolean canAlias(FlowExpressions.Receiver a,
            FlowExpressions.Receiver b) {
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
        for (Entry<FlowExpressions.FieldAccess, V> e : other.fieldValues
                .entrySet()) {
            // information about fields that are only part of one store, but not
            // the other are discarded, as one store implicitly contains 'top'
            // for that field.
            FlowExpressions.FieldAccess el = e.getKey();
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
        for (Entry<FlowExpressions.FieldAccess, V> e : other.fieldValues
                .entrySet()) {
            FlowExpressions.FieldAccess key = e.getKey();
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
        return toDOToutput().replace("\\n", "\n");
    }

    /**
     * @return DOT representation of the store (may contain control characters
     *         such as "\n").
     */
    public String toDOToutput() {
        StringBuilder result = new StringBuilder("CFStore (\\n");
        for (Entry<Element, V> entry : localVariableValues.entrySet()) {
            result.append("  " + entry.getKey() + " > " + entry.getValue()
                    + "\\n");
        }
        for (Entry<FlowExpressions.FieldAccess, V> entry : fieldValues
                .entrySet()) {
            result.append("  " + entry.getKey() + " > " + entry.getValue()
                    + "\\n");
        }
        result.append(")");
        return result.toString();
    }
}
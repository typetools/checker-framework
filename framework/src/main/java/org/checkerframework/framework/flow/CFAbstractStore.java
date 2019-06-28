package org.checkerframework.framework.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.ClassName;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.StringCFGVisualizer;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ThisLiteralNode;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;

/**
 * A store for the checker framework analysis tracks the annotations of memory locations such as
 * local variables and fields.
 *
 * <p>When adding a new field to track values for a code construct (similar to {@code
 * localVariableValues} and {@code thisValue}), it is important to review all constructors and
 * methods in this class for locations where the new field must be handled (such as the copy
 * constructor and {@code clearValue}), as well as all constructors/methods in subclasses of {code
 * CFAbstractStore}. Note that this includes not only overridden methods in the subclasses, but new
 * methods in the subclasses as well. Also check if
 * BaseTypeVisitor#getFlowExpressionContextFromNode(Node) needs to be updated. Failing to do so may
 * result in silent failures that are time consuming to debug.
 */
// TODO: this class should be split into parts that are reusable generally, and
// parts specific to the checker framework
public abstract class CFAbstractStore<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>>
        implements Store<S> {

    /** The analysis class this store belongs to. */
    protected final CFAbstractAnalysis<V, S, ?> analysis;

    /** Information collected about local variables (including method arguments). */
    protected final Map<FlowExpressions.LocalVariable, V> localVariableValues;

    /** Information collected about the current object. */
    protected V thisValue;

    /**
     * Information collected about fields, using the internal representation {@link FieldAccess}.
     */
    protected Map<FlowExpressions.FieldAccess, V> fieldValues;

    /**
     * Information collected about arrays, using the internal representation {@link ArrayAccess}.
     */
    protected Map<FlowExpressions.ArrayAccess, V> arrayValues;

    /**
     * Information collected about method calls, using the internal representation {@link
     * MethodCall}.
     */
    protected Map<FlowExpressions.MethodCall, V> methodValues;

    /**
     * Information collected about <i>classname</i>.class values, using the internal representation
     * {@link ClassName}.
     */
    protected Map<FlowExpressions.ClassName, V> classValues;

    /**
     * Should the analysis use sequential Java semantics (i.e., assume that only one thread is
     * running at all times)?
     */
    protected final boolean sequentialSemantics;

    /* --------------------------------------------------------- */
    /* Initialization */
    /* --------------------------------------------------------- */

    public CFAbstractStore(CFAbstractAnalysis<V, S, ?> analysis, boolean sequentialSemantics) {
        this.analysis = analysis;
        localVariableValues = new HashMap<>();
        thisValue = null;
        fieldValues = new HashMap<>();
        methodValues = new HashMap<>();
        arrayValues = new HashMap<>();
        classValues = new HashMap<>();
        this.sequentialSemantics = sequentialSemantics;
    }

    /** Copy constructor. */
    protected CFAbstractStore(CFAbstractStore<V, S> other) {
        this.analysis = other.analysis;
        localVariableValues = new HashMap<>(other.localVariableValues);
        thisValue = other.thisValue;
        fieldValues = new HashMap<>(other.fieldValues);
        methodValues = new HashMap<>(other.methodValues);
        arrayValues = new HashMap<>(other.arrayValues);
        classValues = new HashMap<>(other.classValues);
        sequentialSemantics = other.sequentialSemantics;
    }

    /**
     * Set the abstract value of a method parameter (only adds the information to the store, does
     * not remove any other knowledge). Any previous information is erased; this method should only
     * be used to initialize the abstract value.
     */
    public void initializeMethodParameter(LocalVariableNode p, @Nullable V value) {
        if (value != null) {
            localVariableValues.put(new FlowExpressions.LocalVariable(p.getElement()), value);
        }
    }

    /**
     * Set the value of the current object. Any previous information is erased; this method should
     * only be used to initialize the value.
     */
    public void initializeThisValue(AnnotationMirror a, TypeMirror underlyingType) {
        if (a != null) {
            thisValue = analysis.createSingleAnnotationValue(a, underlyingType);
        }
    }

    /*
     * Indicates whether the given method is side-effect-free as far as the
     * current store is concerned.
     * In some cases, a store for a checker allows for other mechanisms to specify
     * whether a method is side-effect-free. For example, unannotated methods may
     * be considered side-effect-free by default.
     *
     * @param atypeFactory     the type factory used to retrieve annotations on the method element
     * @param method           the method element
     *
     * @return whether the method is side-effect-free
     */
    protected boolean isSideEffectFree(
            AnnotatedTypeFactory atypeFactory, ExecutableElement method) {
        return PurityUtils.isSideEffectFree(atypeFactory, method);
    }

    /* --------------------------------------------------------- */
    /* Handling of fields */
    /* --------------------------------------------------------- */

    /**
     * Remove any information that might not be valid any more after a method call, and add
     * information guaranteed by the method.
     *
     * <ol>
     *   <li>If the method is side-effect-free (as indicated by {@link
     *       org.checkerframework.dataflow.qual.SideEffectFree} or {@link
     *       org.checkerframework.dataflow.qual.Pure}), then no information needs to be removed.
     *   <li>Otherwise, all information about field accesses {@code a.f} needs to be removed, except
     *       if the method {@code n} cannot modify {@code a.f} (e.g., if {@code a} is a local
     *       variable or {@code this}, and {@code f} is final).
     *   <li>Furthermore, if the field has a monotonic annotation, then its information can also be
     *       kept.
     * </ol>
     *
     * Furthermore, if the method is deterministic, we store its result {@code val} in the store.
     */
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory atypeFactory, V val) {
        ExecutableElement method = n.getTarget().getMethod();

        // case 1: remove information if necessary
        if (!(analysis.checker.hasOption("assumeSideEffectFree")
                || isSideEffectFree(atypeFactory, method))) {
            // update field values
            Map<FlowExpressions.FieldAccess, V> newFieldValues = new HashMap<>();
            for (Entry<FlowExpressions.FieldAccess, V> e : fieldValues.entrySet()) {
                FlowExpressions.FieldAccess fieldAccess = e.getKey();
                V otherVal = e.getValue();

                // case 3:
                List<Pair<AnnotationMirror, AnnotationMirror>> fieldAnnotations =
                        atypeFactory.getAnnotationWithMetaAnnotation(
                                fieldAccess.getField(), MonotonicQualifier.class);
                V newOtherVal = null;
                for (Pair<AnnotationMirror, AnnotationMirror> fieldAnnotation : fieldAnnotations) {
                    AnnotationMirror monotonicAnnotation = fieldAnnotation.second;
                    Name annotation =
                            AnnotationUtils.getElementValueClassName(
                                    monotonicAnnotation, "value", false);
                    AnnotationMirror target =
                            AnnotationBuilder.fromName(atypeFactory.getElementUtils(), annotation);
                    // Make sure the 'target' annotation is present.
                    if (AnnotationUtils.containsSame(otherVal.getAnnotations(), target)) {
                        newOtherVal =
                                analysis.createSingleAnnotationValue(
                                                target, otherVal.getUnderlyingType())
                                        .mostSpecific(newOtherVal, null);
                    }
                }
                if (newOtherVal != null) {
                    // keep information for all hierarchies where we had a
                    // monotone annotation.
                    newFieldValues.put(fieldAccess, newOtherVal);
                    continue;
                }

                // case 2:
                if (!fieldAccess.isUnassignableByOtherCode()) {
                    continue; // remove information completely
                }

                // keep information
                newFieldValues.put(fieldAccess, otherVal);
            }
            fieldValues = newFieldValues;

            // update method values
            methodValues.entrySet().removeIf(e -> !e.getKey().isUnmodifiableByOtherCode());

            arrayValues.clear();
        }

        // store information about method call if possible
        Receiver methodCall = FlowExpressions.internalReprOf(analysis.getTypeFactory(), n);
        replaceValue(methodCall, val);
    }

    /**
     * Add the annotation {@code a} for the expression {@code r} (correctly deciding where to store
     * the information depending on the type of the expression {@code r}).
     *
     * <p>This method does not take care of removing other information that might be influenced by
     * changes to certain parts of the state.
     *
     * <p>If there is already a value {@code v} present for {@code r}, then the stronger of the new
     * and old value are taken (according to the lattice). Note that this happens per hierarchy, and
     * if the store already contains information about a hierarchy other than {@code a}s hierarchy,
     * that information is preserved.
     */
    public void insertValue(FlowExpressions.Receiver r, AnnotationMirror a) {
        insertValue(r, analysis.createSingleAnnotationValue(a, r.getType()));
    }

    /** Returns true if the receiver {@code r} can be stored in this store. */
    public static boolean canInsertReceiver(Receiver r) {
        if (r instanceof FlowExpressions.FieldAccess
                || r instanceof FlowExpressions.ThisReference
                || r instanceof FlowExpressions.LocalVariable
                || r instanceof FlowExpressions.MethodCall
                || r instanceof FlowExpressions.ArrayAccess
                || r instanceof FlowExpressions.ClassName) {
            return !r.containsUnknown();
        }
        return false;
    }

    /**
     * Add the abstract value {@code value} for the expression {@code r} (correctly deciding where
     * to store the information depending on the type of the expression {@code r}).
     *
     * <p>This method does not take care of removing other information that might be influenced by
     * changes to certain parts of the state.
     *
     * <p>If there is already a value {@code v} present for {@code r}, then the stronger of the new
     * and old value are taken (according to the lattice). Note that this happens per hierarchy, and
     * if the store already contains information about a hierarchy for which {@code value} does not
     * contain information, then that information is preserved.
     */
    public void insertValue(FlowExpressions.Receiver r, @Nullable V value) {
        if (value == null) {
            // No need to insert a null abstract value because it represents
            // top and top is also the default value.
            return;
        }
        if (r.containsUnknown()) {
            // Expressions containing unknown expressions are not stored.
            return;
        }
        if (r instanceof FlowExpressions.LocalVariable) {
            FlowExpressions.LocalVariable localVar = (FlowExpressions.LocalVariable) r;
            V oldValue = localVariableValues.get(localVar);
            V newValue = value.mostSpecific(oldValue, null);
            if (newValue != null) {
                localVariableValues.put(localVar, newValue);
            }
        } else if (r instanceof FlowExpressions.FieldAccess) {
            FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) r;
            // Only store information about final fields (where the receiver is
            // also fixed) if concurrent semantics are enabled.
            boolean isMonotonic = isMonotonicUpdate(fieldAcc, value);
            if (sequentialSemantics || isMonotonic || fieldAcc.isUnassignableByOtherCode()) {
                V oldValue = fieldValues.get(fieldAcc);
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    fieldValues.put(fieldAcc, newValue);
                }
            }
        } else if (r instanceof FlowExpressions.MethodCall) {
            FlowExpressions.MethodCall method = (FlowExpressions.MethodCall) r;
            // Don't store any information if concurrent semantics are enabled.
            if (sequentialSemantics) {
                V oldValue = methodValues.get(method);
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    methodValues.put(method, newValue);
                }
            }
        } else if (r instanceof FlowExpressions.ArrayAccess) {
            FlowExpressions.ArrayAccess arrayAccess = (ArrayAccess) r;
            if (sequentialSemantics) {
                V oldValue = arrayValues.get(arrayAccess);
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    arrayValues.put(arrayAccess, newValue);
                }
            }
        } else if (r instanceof FlowExpressions.ThisReference) {
            FlowExpressions.ThisReference thisRef = (FlowExpressions.ThisReference) r;
            if (sequentialSemantics || thisRef.isUnassignableByOtherCode()) {
                V oldValue = thisValue;
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    thisValue = newValue;
                }
            }
        } else if (r instanceof FlowExpressions.ClassName) {
            FlowExpressions.ClassName className = (FlowExpressions.ClassName) r;
            if (sequentialSemantics || className.isUnassignableByOtherCode()) {
                V oldValue = classValues.get(className);
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    classValues.put(className, newValue);
                }
            }
        } else {
            // No other types of expressions need to be stored.
        }
    }

    /**
     * @return true if fieldAcc is an update of a monotonic qualifier to its target qualifier.
     *     (e.g. @MonotonicNonNull to @NonNull). Always returns false if sequentialSemantics is
     *     true.
     */
    protected boolean isMonotonicUpdate(FieldAccess fieldAcc, V value) {
        boolean isMonotonic = false;
        // TODO: This check for !sequentialSemantics is an optimization that breaks the contract of
        // the method, since the method name and documentation say nothing about sequential
        // semantics.  This check should be performed by callers of this method when needed.
        // TODO: Update the javadoc of this method when the above to-do item is addressed.
        if (!sequentialSemantics) { // only compute if necessary
            AnnotatedTypeFactory atypeFactory = this.analysis.atypeFactory;
            List<Pair<AnnotationMirror, AnnotationMirror>> fieldAnnotations =
                    atypeFactory.getAnnotationWithMetaAnnotation(
                            fieldAcc.getField(), MonotonicQualifier.class);
            for (Pair<AnnotationMirror, AnnotationMirror> fieldAnnotation : fieldAnnotations) {
                AnnotationMirror monotonicAnnotation = fieldAnnotation.second;
                Name annotation =
                        AnnotationUtils.getElementValueClassName(
                                monotonicAnnotation, "value", false);
                AnnotationMirror target =
                        AnnotationBuilder.fromName(atypeFactory.getElementUtils(), annotation);
                // Make sure the 'target' annotation is present.
                if (AnnotationUtils.containsSame(value.getAnnotations(), target)) {
                    isMonotonic = true;
                    break;
                }
            }
        }
        return isMonotonic;
    }

    public void insertThisValue(AnnotationMirror a, TypeMirror underlyingType) {
        if (a == null) {
            return;
        }

        V value = analysis.createSingleAnnotationValue(a, underlyingType);

        V oldValue = thisValue;
        V newValue = value.mostSpecific(oldValue, null);
        if (newValue != null) {
            thisValue = newValue;
        }
    }

    /**
     * Completely replaces the abstract value {@code value} for the expression {@code r} (correctly
     * deciding where to store the information depending on the type of the expression {@code r}).
     * Any previous information is discarded.
     *
     * <p>This method does not take care of removing other information that might be influenced by
     * changes to certain parts of the state.
     */
    public void replaceValue(FlowExpressions.Receiver r, @Nullable V value) {
        clearValue(r);
        insertValue(r, value);
    }

    /**
     * Remove any knowledge about the expression {@code r} (correctly deciding where to remove the
     * information depending on the type of the expression {@code r}).
     */
    public void clearValue(FlowExpressions.Receiver r) {
        if (r.containsUnknown()) {
            // Expressions containing unknown expressions are not stored.
            return;
        }
        if (r instanceof FlowExpressions.LocalVariable) {
            FlowExpressions.LocalVariable localVar = (FlowExpressions.LocalVariable) r;
            localVariableValues.remove(localVar);
        } else if (r instanceof FlowExpressions.FieldAccess) {
            FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) r;
            fieldValues.remove(fieldAcc);
        } else if (r instanceof FlowExpressions.MethodCall) {
            MethodCall method = (MethodCall) r;
            methodValues.remove(method);
        } else if (r instanceof FlowExpressions.ArrayAccess) {
            ArrayAccess a = (ArrayAccess) r;
            arrayValues.remove(a);
        } else if (r instanceof FlowExpressions.ClassName) {
            FlowExpressions.ClassName c = (FlowExpressions.ClassName) r;
            classValues.remove(c);
        } else { // thisValue ...
            // No other types of expressions are stored.
        }
    }

    /**
     * @return current abstract value of a flow expression, or {@code null} if no information is
     *     available.
     */
    public @Nullable V getValue(FlowExpressions.Receiver expr) {
        if (expr instanceof FlowExpressions.LocalVariable) {
            FlowExpressions.LocalVariable localVar = (FlowExpressions.LocalVariable) expr;
            return localVariableValues.get(localVar);
        } else if (expr instanceof FlowExpressions.ThisReference) {
            return thisValue;
        } else if (expr instanceof FlowExpressions.FieldAccess) {
            FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) expr;
            return fieldValues.get(fieldAcc);
        } else if (expr instanceof FlowExpressions.MethodCall) {
            FlowExpressions.MethodCall method = (FlowExpressions.MethodCall) expr;
            return methodValues.get(method);
        } else if (expr instanceof FlowExpressions.ArrayAccess) {
            FlowExpressions.ArrayAccess a = (FlowExpressions.ArrayAccess) expr;
            return arrayValues.get(a);
        } else if (expr instanceof FlowExpressions.ClassName) {
            FlowExpressions.ClassName c = (FlowExpressions.ClassName) expr;
            return classValues.get(c);
        } else {
            throw new BugInCF("Unexpected FlowExpression: " + expr + " (" + expr.getClass() + ")");
        }
    }

    /**
     * @return current abstract value of a field access, or {@code null} if no information is
     *     available.
     */
    public @Nullable V getValue(FieldAccessNode n) {
        FlowExpressions.FieldAccess fieldAccess =
                FlowExpressions.internalReprOfFieldAccess(analysis.getTypeFactory(), n);
        return fieldValues.get(fieldAccess);
    }

    /**
     * @return current abstract value of a method call, or {@code null} if no information is
     *     available.
     */
    public @Nullable V getValue(MethodInvocationNode n) {
        Receiver method = FlowExpressions.internalReprOf(analysis.getTypeFactory(), n, true);
        if (method == null) {
            return null;
        }
        return methodValues.get(method);
    }

    /**
     * @return current abstract value of a field access, or {@code null} if no information is
     *     available.
     */
    public @Nullable V getValue(ArrayAccessNode n) {
        FlowExpressions.ArrayAccess arrayAccess =
                FlowExpressions.internalReprOfArrayAccess(analysis.getTypeFactory(), n);
        return arrayValues.get(arrayAccess);
    }

    /** Update the information in the store by considering an assignment with target {@code n}. */
    public void updateForAssignment(Node n, @Nullable V val) {
        Receiver receiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), n);
        if (receiver instanceof ArrayAccess) {
            updateForArrayAssignment((ArrayAccess) receiver, val);
        } else if (receiver instanceof FieldAccess) {
            updateForFieldAccessAssignment((FieldAccess) receiver, val);
        } else if (receiver instanceof LocalVariable) {
            updateForLocalVariableAssignment((LocalVariable) receiver, val);
        } else {
            throw new BugInCF("Unexpected receiver of class " + receiver.getClass());
        }
    }

    /**
     * Update the information in the store by considering a field assignment with target {@code n},
     * where the right hand side has the abstract value {@code val}.
     *
     * @param val the abstract value of the value assigned to {@code n} (or {@code null} if the
     *     abstract value is not known).
     */
    protected void updateForFieldAccessAssignment(FieldAccess fieldAccess, @Nullable V val) {
        removeConflicting(fieldAccess, val);
        if (!fieldAccess.containsUnknown() && val != null) {
            // Only store information about final fields (where the receiver is
            // also fixed) if concurrent semantics are enabled.
            boolean isMonotonic = isMonotonicUpdate(fieldAccess, val);
            if (sequentialSemantics || isMonotonic || fieldAccess.isUnassignableByOtherCode()) {
                fieldValues.put(fieldAccess, val);
            }
        }
    }

    /**
     * Update the information in the store by considering an assignment with target {@code n}, where
     * the target is an array access.
     *
     * <p>See {@link #removeConflicting(FlowExpressions.ArrayAccess,CFAbstractValue)}, as it is
     * called first by this method.
     */
    protected void updateForArrayAssignment(ArrayAccess arrayAccess, @Nullable V val) {
        removeConflicting(arrayAccess, val);
        if (!arrayAccess.containsUnknown() && val != null) {
            // Only store information about final fields (where the receiver is
            // also fixed) if concurrent semantics are enabled.
            if (sequentialSemantics) {
                arrayValues.put(arrayAccess, val);
            }
        }
    }

    /**
     * Set the abstract value of a local variable in the store. Overwrites any value that might have
     * been available previously.
     *
     * @param val the abstract value of the value assigned to {@code n} (or {@code null} if the
     *     abstract value is not known).
     */
    protected void updateForLocalVariableAssignment(LocalVariable receiver, @Nullable V val) {
        removeConflicting(receiver);
        if (val != null) {
            localVariableValues.put(receiver, val);
        }
    }

    /**
     * Remove any information in this store that might not be true any more after {@code
     * fieldAccess} has been assigned a new value (with the abstract value {@code val}). This
     * includes the following steps (assume that {@code fieldAccess} is of the form <em>a.f</em> for
     * some <em>a</em>.
     *
     * <ol>
     *   <li value="1">Update the abstract value of other field accesses <em>b.g</em> where the
     *       field is equal (that is, <em>f=g</em>), and the receiver <em>b</em> might alias the
     *       receiver of {@code fieldAccess}, <em>a</em>. This update will raise the abstract value
     *       for such field accesses to at least {@code val} (or the old value, if that was less
     *       precise). However, this is only necessary if the field <em>g</em> is not final.
     *   <li value="2">Remove any abstract values for field accesses <em>b.g</em> where {@code
     *       fieldAccess} might alias any expression in the receiver <em>b</em>.
     *   <li value="3">Remove any information about method calls.
     *   <li value="4">Remove any abstract values an array access <em>b[i]</em> where {@code
     *       fieldAccess} might alias any expression in the receiver <em>a</em> or index <em>i</em>.
     * </ol>
     *
     * @param val the abstract value of the value assigned to {@code n} (or {@code null} if the
     *     abstract value is not known).
     */
    protected void removeConflicting(FlowExpressions.FieldAccess fieldAccess, @Nullable V val) {
        Map<FlowExpressions.FieldAccess, V> newFieldValues = new HashMap<>();
        for (Entry<FlowExpressions.FieldAccess, V> e : fieldValues.entrySet()) {
            FlowExpressions.FieldAccess otherFieldAccess = e.getKey();
            V otherVal = e.getValue();
            // case 2:
            if (otherFieldAccess.getReceiver().containsModifiableAliasOf(this, fieldAccess)) {
                continue; // remove information completely
            }
            // case 1:
            if (fieldAccess.getField().equals(otherFieldAccess.getField())) {
                if (canAlias(fieldAccess.getReceiver(), otherFieldAccess.getReceiver())) {
                    if (!otherFieldAccess.isFinal()) {
                        if (val != null) {
                            V newVal = val.leastUpperBound(otherVal);
                            newFieldValues.put(otherFieldAccess, newVal);
                        } else {
                            // remove information completely
                        }
                        continue;
                    }
                }
            }
            // information is save to be carried over
            newFieldValues.put(otherFieldAccess, otherVal);
        }
        fieldValues = newFieldValues;

        Map<FlowExpressions.ArrayAccess, V> newArrayValues = new HashMap<>();
        for (Entry<ArrayAccess, V> e : arrayValues.entrySet()) {
            FlowExpressions.ArrayAccess otherArrayAccess = e.getKey();
            V otherVal = e.getValue();
            if (otherArrayAccess.containsModifiableAliasOf(this, fieldAccess)) {
                // remove information completely
                continue;
            }
            newArrayValues.put(otherArrayAccess, otherVal);
        }
        arrayValues = newArrayValues;

        // case 3:
        methodValues = new HashMap<>();
    }

    /**
     * Remove any information in the store that might not be true any more after {@code arrayAccess}
     * has been assigned a new value (with the abstract value {@code val}). This includes the
     * following steps (assume that {@code arrayAccess} is of the form <em>a[i]</em> for some
     * <em>a</em>.
     *
     * <ol>
     *   <li value="1">Remove any abstract value for other array access <em>b[j]</em> where
     *       <em>a</em> and <em>b</em> can be aliases, or where either <em>b</em> or <em>j</em>
     *       contains a modifiable alias of <em>a[i]</em>.
     *   <li value="2">Remove any abstract values for field accesses <em>b.g</em> where
     *       <em>a[i]</em> might alias any expression in the receiver <em>b</em> and there is an
     *       array expression somewhere in the receiver.
     *   <li value="3">Remove any information about method calls.
     * </ol>
     *
     * @param val the abstract value of the value assigned to {@code n} (or {@code null} if the
     *     abstract value is not known).
     */
    protected void removeConflicting(FlowExpressions.ArrayAccess arrayAccess, @Nullable V val) {
        Map<FlowExpressions.ArrayAccess, V> newArrayValues = new HashMap<>();
        for (Entry<FlowExpressions.ArrayAccess, V> e : arrayValues.entrySet()) {
            FlowExpressions.ArrayAccess otherArrayAccess = e.getKey();
            V otherVal = e.getValue();
            // case 1:
            if (otherArrayAccess.containsModifiableAliasOf(this, arrayAccess)) {
                continue; // remove information completely
            }
            if (canAlias(arrayAccess.getReceiver(), otherArrayAccess.getReceiver())) {
                // TODO: one could be less strict here, and only raise the abstract
                // value for all array expressions with potentially aliasing receivers.
                continue; // remove information completely
            }
            // information is save to be carried over
            newArrayValues.put(otherArrayAccess, otherVal);
        }
        arrayValues = newArrayValues;

        // case 2:
        Map<FlowExpressions.FieldAccess, V> newFieldValues = new HashMap<>();
        for (Entry<FieldAccess, V> e : fieldValues.entrySet()) {
            FlowExpressions.FieldAccess otherFieldAccess = e.getKey();
            V otherVal = e.getValue();
            Receiver receiver = otherFieldAccess.getReceiver();
            if (receiver.containsModifiableAliasOf(this, arrayAccess)
                    && receiver.containsOfClass(ArrayAccess.class)) {
                // remove information completely
                continue;
            }
            newFieldValues.put(otherFieldAccess, otherVal);
        }
        fieldValues = newFieldValues;

        // case 3:
        methodValues = new HashMap<>();
    }

    /**
     * Remove any information in this store that might not be true any more after {@code localVar}
     * has been assigned a new value. This includes the following steps:
     *
     * <ol>
     *   <li value="1">Remove any abstract values for field accesses <em>b.g</em> where {@code
     *       localVar} might alias any expression in the receiver <em>b</em>.
     *   <li value="2">Remove any abstract values for array accesses <em>a[i]</em> where {@code
     *       localVar} might alias the receiver <em>a</em>.
     *   <li value="3">Remove any information about method calls where the receiver or any of the
     *       parameters contains {@code localVar}.
     * </ol>
     */
    protected void removeConflicting(LocalVariable var) {
        Map<FlowExpressions.FieldAccess, V> newFieldValues = new HashMap<>();
        for (Entry<FlowExpressions.FieldAccess, V> e : fieldValues.entrySet()) {
            FlowExpressions.FieldAccess otherFieldAccess = e.getKey();
            // case 1:
            if (otherFieldAccess.containsSyntacticEqualReceiver(var)) {
                continue;
            }
            newFieldValues.put(otherFieldAccess, e.getValue());
        }
        fieldValues = newFieldValues;

        Map<FlowExpressions.ArrayAccess, V> newArrayValues = new HashMap<>();
        for (Entry<FlowExpressions.ArrayAccess, V> e : arrayValues.entrySet()) {
            FlowExpressions.ArrayAccess otherArrayAccess = e.getKey();
            // case 2:
            if (otherArrayAccess.containsSyntacticEqualReceiver(var)) {
                continue;
            }
            newArrayValues.put(otherArrayAccess, e.getValue());
        }
        arrayValues = newArrayValues;

        Map<FlowExpressions.MethodCall, V> newMethodValues = new HashMap<>();
        for (Entry<FlowExpressions.MethodCall, V> e : methodValues.entrySet()) {
            FlowExpressions.MethodCall otherMethodAccess = e.getKey();
            // case 3:
            if (otherMethodAccess.containsSyntacticEqualReceiver(var)
                    || otherMethodAccess.containsSyntacticEqualParameter(var)) {
                continue;
            }
            newMethodValues.put(otherMethodAccess, e.getValue());
        }
        methodValues = newMethodValues;
    }

    /**
     * Can the objects {@code a} and {@code b} be aliases? Returns a conservative answer (i.e.,
     * returns {@code true} if not enough information is available to determine aliasing).
     */
    @Override
    public boolean canAlias(FlowExpressions.Receiver a, FlowExpressions.Receiver b) {
        TypeMirror tb = b.getType();
        TypeMirror ta = a.getType();
        Types types = analysis.getTypes();
        return types.isSubtype(ta, tb) || types.isSubtype(tb, ta);
    }

    /* --------------------------------------------------------- */
    /* Handling of local variables */
    /* --------------------------------------------------------- */

    /**
     * @return current abstract value of a local variable, or {@code null} if no information is
     *     available.
     */
    public @Nullable V getValue(LocalVariableNode n) {
        Element el = n.getElement();
        return localVariableValues.get(new FlowExpressions.LocalVariable(el));
    }

    /* --------------------------------------------------------- */
    /* Handling of the current object */
    /* --------------------------------------------------------- */

    /**
     * @return current abstract value of the current object, or {@code null} if no information is
     *     available.
     */
    public @Nullable V getValue(ThisLiteralNode n) {
        return thisValue;
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
        return upperBound(other, false);
    }

    @Override
    public S widenedUpperBound(S previous) {
        return upperBound(previous, true);
    }

    private S upperBound(S other, boolean shouldWiden) {
        S newStore = analysis.createEmptyStore(sequentialSemantics);

        for (Entry<FlowExpressions.LocalVariable, V> e : other.localVariableValues.entrySet()) {
            // local variables that are only part of one store, but not the
            // other are discarded, as one of store implicitly contains 'top'
            // for that variable.
            FlowExpressions.LocalVariable localVar = e.getKey();
            if (localVariableValues.containsKey(localVar)) {
                V otherVal = e.getValue();
                V thisVal = localVariableValues.get(localVar);
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);

                if (mergedVal != null) {
                    newStore.localVariableValues.put(localVar, mergedVal);
                }
            }
        }

        // information about the current object
        {
            V otherVal = other.thisValue;
            V myVal = thisValue;
            V mergedVal = myVal == null ? null : upperBoundOfValues(otherVal, myVal, shouldWiden);
            if (mergedVal != null) {
                newStore.thisValue = mergedVal;
            }
        }

        for (Entry<FlowExpressions.FieldAccess, V> e : other.fieldValues.entrySet()) {
            // information about fields that are only part of one store, but not
            // the other are discarded, as one store implicitly contains 'top'
            // for that field.
            FlowExpressions.FieldAccess el = e.getKey();
            if (fieldValues.containsKey(el)) {
                V otherVal = e.getValue();
                V thisVal = fieldValues.get(el);
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);
                if (mergedVal != null) {
                    newStore.fieldValues.put(el, mergedVal);
                }
            }
        }
        for (Entry<FlowExpressions.ArrayAccess, V> e : other.arrayValues.entrySet()) {
            // information about arrays that are only part of one store, but not
            // the other are discarded, as one store implicitly contains 'top'
            // for that array access.
            FlowExpressions.ArrayAccess el = e.getKey();
            if (arrayValues.containsKey(el)) {
                V otherVal = e.getValue();
                V thisVal = arrayValues.get(el);
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);
                if (mergedVal != null) {
                    newStore.arrayValues.put(el, mergedVal);
                }
            }
        }
        for (Entry<MethodCall, V> e : other.methodValues.entrySet()) {
            // information about methods that are only part of one store, but
            // not the other are discarded, as one store implicitly contains
            // 'top' for that field.
            FlowExpressions.MethodCall el = e.getKey();
            if (methodValues.containsKey(el)) {
                V otherVal = e.getValue();
                V thisVal = methodValues.get(el);
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);
                if (mergedVal != null) {
                    newStore.methodValues.put(el, mergedVal);
                }
            }
        }
        for (Entry<FlowExpressions.ClassName, V> e : other.classValues.entrySet()) {
            FlowExpressions.ClassName el = e.getKey();
            if (classValues.containsKey(el)) {
                V otherVal = e.getValue();
                V thisVal = classValues.get(el);
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);
                if (mergedVal != null) {
                    newStore.classValues.put(el, mergedVal);
                }
            }
        }
        return newStore;
    }

    private V upperBoundOfValues(V otherVal, V thisVal, boolean shouldWiden) {
        return shouldWiden ? thisVal.widenUpperBound(otherVal) : thisVal.leastUpperBound(otherVal);
    }

    /**
     * Returns true iff this {@link CFAbstractStore} contains a superset of the map entries of the
     * argument {@link CFAbstractStore}. Note that we test the entry keys and values by Java
     * equality, not by any subtype relationship. This method is used primarily to simplify the
     * equals predicate.
     */
    protected boolean supersetOf(CFAbstractStore<V, S> other) {
        for (Entry<FlowExpressions.LocalVariable, V> e : other.localVariableValues.entrySet()) {
            FlowExpressions.LocalVariable key = e.getKey();
            if (!localVariableValues.containsKey(key)
                    || !localVariableValues.get(key).equals(e.getValue())) {
                return false;
            }
        }
        for (Entry<FlowExpressions.FieldAccess, V> e : other.fieldValues.entrySet()) {
            FlowExpressions.FieldAccess key = e.getKey();
            if (!fieldValues.containsKey(key) || !fieldValues.get(key).equals(e.getValue())) {
                return false;
            }
        }
        for (Entry<FlowExpressions.ArrayAccess, V> e : other.arrayValues.entrySet()) {
            FlowExpressions.ArrayAccess key = e.getKey();
            if (!arrayValues.containsKey(key) || !arrayValues.get(key).equals(e.getValue())) {
                return false;
            }
        }
        for (Entry<MethodCall, V> e : other.methodValues.entrySet()) {
            FlowExpressions.MethodCall key = e.getKey();
            if (!methodValues.containsKey(key) || !methodValues.get(key).equals(e.getValue())) {
                return false;
            }
        }
        for (Entry<FlowExpressions.ClassName, V> e : other.classValues.entrySet()) {
            FlowExpressions.ClassName key = e.getKey();
            if (!classValues.containsKey(key) || !classValues.get(key).equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CFAbstractStore) {
            @SuppressWarnings("unchecked")
            CFAbstractStore<V, S> other = (CFAbstractStore<V, S>) o;
            return this.supersetOf(other) && other.supersetOf(this);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // What is a good hash code to use?
        return System.identityHashCode(this);
    }

    @SideEffectFree
    @Override
    public String toString() {
        return visualize(new StringCFGVisualizer<>());
    }

    @Override
    public String visualize(CFGVisualizer<?, S, ?> viz) {
        /* This cast is guaranteed to be safe, as long as the CFGVisualizer is created by
         * CFGVisualizer<Value, Store, TransferFunction> createCFGVisualizer() of GenericAnnotatedTypeFactory */
        @SuppressWarnings("unchecked")
        CFGVisualizer<V, S, ?> castedViz = (CFGVisualizer<V, S, ?>) viz;
        StringBuilder sbVisualize = new StringBuilder();
        sbVisualize.append(castedViz.visualizeStoreHeader(this.getClass().getCanonicalName()));
        sbVisualize.append(internalVisualize(castedViz));
        sbVisualize.append(castedViz.visualizeStoreFooter());
        return sbVisualize.toString();
    }

    /**
     * Adds a representation of the internal information of this Store to visualizer {@code viz}.
     *
     * @return a representation of the internal information of this {@link Store}
     */
    protected String internalVisualize(CFGVisualizer<V, S, ?> viz) {
        StringBuilder res = new StringBuilder();
        for (Entry<FlowExpressions.LocalVariable, V> entry : localVariableValues.entrySet()) {
            res.append(viz.visualizeStoreLocalVar(entry.getKey(), entry.getValue()));
        }
        if (thisValue != null) {
            res.append(viz.visualizeStoreThisVal(thisValue));
        }
        for (Entry<FlowExpressions.FieldAccess, V> entry : fieldValues.entrySet()) {
            res.append(viz.visualizeStoreFieldVals(entry.getKey(), entry.getValue()));
        }
        for (Entry<FlowExpressions.ArrayAccess, V> entry : arrayValues.entrySet()) {
            res.append(viz.visualizeStoreArrayVal(entry.getKey(), entry.getValue()));
        }
        for (Entry<MethodCall, V> entry : methodValues.entrySet()) {
            res.append(viz.visualizeStoreMethodVals(entry.getKey(), entry.getValue()));
        }
        for (Entry<FlowExpressions.ClassName, V> entry : classValues.entrySet()) {
            res.append(viz.visualizeStoreClassVals(entry.getKey(), entry.getValue()));
        }
        return res.toString();
    }
}

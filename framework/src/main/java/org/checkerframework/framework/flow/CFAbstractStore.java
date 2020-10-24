package org.checkerframework.framework.flow;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ThisLiteralNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.cfg.visualize.StringCFGVisualizer;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.FlowExpressions;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.Receiver;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.plumelib.util.UniqueId;

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
        implements Store<S>, UniqueId {

    /** The analysis class this store belongs to. */
    protected final CFAbstractAnalysis<V, S, ?> analysis;

    /** Information collected about local variables (including method arguments). */
    protected final Map<LocalVariable, V> localVariableValues;

    /** Information collected about the current object. */
    protected V thisValue;

    /**
     * Information collected about fields, using the internal representation {@link FieldAccess}.
     */
    protected Map<FieldAccess, V> fieldValues;

    /**
     * Information collected about arrays, using the internal representation {@link ArrayAccess}.
     */
    protected Map<ArrayAccess, V> arrayValues;

    /**
     * Information collected about method calls, using the internal representation {@link
     * MethodCall}.
     */
    protected Map<MethodCall, V> methodValues;

    /**
     * Information collected about <i>classname</i>.class values, using the internal representation
     * {@link ClassName}.
     */
    protected Map<ClassName, V> classValues;

    /**
     * Should the analysis use sequential Java semantics (i.e., assume that only one thread is
     * running at all times)?
     */
    protected final boolean sequentialSemantics;

    /** The unique ID for the next-created object. */
    static final AtomicLong nextUid = new AtomicLong(0);
    /** The unique ID of this object. */
    final transient long uid = nextUid.getAndIncrement();

    @Override
    public long getUid() {
        return uid;
    }

    /* --------------------------------------------------------- */
    /* Initialization */
    /* --------------------------------------------------------- */

    protected CFAbstractStore(CFAbstractAnalysis<V, S, ?> analysis, boolean sequentialSemantics) {
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
            localVariableValues.put(new LocalVariable(p.getElement()), value);
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
                || analysis.checker.hasOption("assumePure")
                || isSideEffectFree(atypeFactory, method))) {
            // update field values
            Map<FieldAccess, V> newFieldValues = new HashMap<>();
            for (Map.Entry<FieldAccess, V> e : fieldValues.entrySet()) {
                FieldAccess fieldAccess = e.getKey();
                V otherVal = e.getValue();

                // case 3:
                if (!((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory)
                        .getSupportedMonotonicTypeQualifiers()
                        .isEmpty()) {
                    List<Pair<AnnotationMirror, AnnotationMirror>> fieldAnnotations =
                            atypeFactory.getAnnotationWithMetaAnnotation(
                                    fieldAccess.getField(), MonotonicQualifier.class);
                    V newOtherVal = null;
                    for (Pair<AnnotationMirror, AnnotationMirror> fieldAnnotation :
                            fieldAnnotations) {
                        AnnotationMirror monotonicAnnotation = fieldAnnotation.second;
                        Name annotation =
                                AnnotationUtils.getElementValueClassName(
                                        monotonicAnnotation, "value", false);
                        AnnotationMirror target =
                                AnnotationBuilder.fromName(
                                        atypeFactory.getElementUtils(), annotation);
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
    public void insertValue(Receiver r, AnnotationMirror a) {
        insertValue(r, analysis.createSingleAnnotationValue(a, r.getType()));
    }

    /**
     * Add the annotation {@code newAnno} for the expression {@code r} (correctly deciding where to
     * store the information depending on the type of the expression {@code r}).
     *
     * <p>This method does not take care of removing other information that might be influenced by
     * changes to certain parts of the state.
     *
     * <p>If there is already a value {@code v} present for {@code r}, then the greatest lower bound
     * of the new and old value is inserted into the store unless it's bottom. Some checkers do not
     * override {@link QualifierHierarchy#greatestLowerBound(AnnotationMirror, AnnotationMirror)}
     * and the default implementation will return the bottom qualifier incorrectly. So this method
     * conservatively does not insert the glb if it is bottom.
     *
     * <p>Note that this happens per hierarchy, and if the store already contains information about
     * a hierarchy other than {@code newAnno}'s hierarchy, that information is preserved.
     */
    public void insertOrRefine(Receiver r, AnnotationMirror newAnno) {
        if (!canInsertReceiver(r)) {
            return;
        }
        V oldValue = getValue(r);
        if (oldValue == null) {
            insertValue(r, analysis.createSingleAnnotationValue(newAnno, r.getType()));
            return;
        }
        QualifierHierarchy qualifierHierarchy = analysis.getTypeFactory().getQualifierHierarchy();
        AnnotationMirror top = qualifierHierarchy.getTopAnnotation(newAnno);
        AnnotationMirror oldAnno =
                qualifierHierarchy.findAnnotationInHierarchy(oldValue.annotations, top);
        if (oldAnno == null) {
            insertValue(r, analysis.createSingleAnnotationValue(newAnno, r.getType()));
            return;
        }

        AnnotationMirror glb = qualifierHierarchy.greatestLowerBound(newAnno, oldAnno);
        if (AnnotationUtils.areSame(qualifierHierarchy.getBottomAnnotation(top), glb)) {
            glb = newAnno;
        }

        insertValue(r, analysis.createSingleAnnotationValue(glb, r.getType()));
    }

    /** Returns true if the receiver {@code r} can be stored in this store. */
    public static boolean canInsertReceiver(Receiver r) {
        if (r instanceof FieldAccess
                || r instanceof ThisReference
                || r instanceof LocalVariable
                || r instanceof MethodCall
                || r instanceof ArrayAccess
                || r instanceof ClassName) {
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
    public void insertValue(Receiver r, @Nullable V value) {
        if (value == null) {
            // No need to insert a null abstract value because it represents
            // top and top is also the default value.
            return;
        }
        if (r.containsUnknown()) {
            // Expressions containing unknown expressions are not stored.
            return;
        }
        if (r instanceof LocalVariable) {
            LocalVariable localVar = (LocalVariable) r;
            V oldValue = localVariableValues.get(localVar);
            V newValue = value.mostSpecific(oldValue, null);
            if (newValue != null) {
                localVariableValues.put(localVar, newValue);
            }
        } else if (r instanceof FieldAccess) {
            FieldAccess fieldAcc = (FieldAccess) r;
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
        } else if (r instanceof MethodCall) {
            MethodCall method = (MethodCall) r;
            // Don't store any information if concurrent semantics are enabled.
            if (sequentialSemantics) {
                V oldValue = methodValues.get(method);
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    methodValues.put(method, newValue);
                }
            }
        } else if (r instanceof ArrayAccess) {
            ArrayAccess arrayAccess = (ArrayAccess) r;
            if (sequentialSemantics) {
                V oldValue = arrayValues.get(arrayAccess);
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    arrayValues.put(arrayAccess, newValue);
                }
            }
        } else if (r instanceof ThisReference) {
            ThisReference thisRef = (ThisReference) r;
            if (sequentialSemantics || thisRef.isUnassignableByOtherCode()) {
                V oldValue = thisValue;
                V newValue = value.mostSpecific(oldValue, null);
                if (newValue != null) {
                    thisValue = newValue;
                }
            }
        } else if (r instanceof ClassName) {
            ClassName className = (ClassName) r;
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
     * Return true if fieldAcc is an update of a monotonic qualifier to its target qualifier.
     * (e.g. @MonotonicNonNull to @NonNull). Always returns false if {@code sequentialSemantics} is
     * true.
     *
     * @return true if fieldAcc is an update of a monotonic qualifier to its target qualifier.
     *     (e.g. @MonotonicNonNull to @NonNull)
     */
    protected boolean isMonotonicUpdate(FieldAccess fieldAcc, V value) {
        if (analysis.atypeFactory.getSupportedMonotonicTypeQualifiers().isEmpty()) {
            return false;
        }
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
    public void replaceValue(Receiver r, @Nullable V value) {
        clearValue(r);
        insertValue(r, value);
    }

    /**
     * Remove any knowledge about the expression {@code r} (correctly deciding where to remove the
     * information depending on the type of the expression {@code r}).
     */
    public void clearValue(Receiver r) {
        if (r.containsUnknown()) {
            // Expressions containing unknown expressions are not stored.
            return;
        }
        if (r instanceof LocalVariable) {
            LocalVariable localVar = (LocalVariable) r;
            localVariableValues.remove(localVar);
        } else if (r instanceof FieldAccess) {
            FieldAccess fieldAcc = (FieldAccess) r;
            fieldValues.remove(fieldAcc);
        } else if (r instanceof MethodCall) {
            MethodCall method = (MethodCall) r;
            methodValues.remove(method);
        } else if (r instanceof ArrayAccess) {
            ArrayAccess a = (ArrayAccess) r;
            arrayValues.remove(a);
        } else if (r instanceof ClassName) {
            ClassName c = (ClassName) r;
            classValues.remove(c);
        } else { // thisValue ...
            // No other types of expressions are stored.
        }
    }

    /**
     * Returns current abstract value of a flow expression, or {@code null} if no information is
     * available.
     *
     * @return current abstract value of a flow expression, or {@code null} if no information is
     *     available
     */
    public @Nullable V getValue(Receiver expr) {
        if (expr instanceof LocalVariable) {
            LocalVariable localVar = (LocalVariable) expr;
            return localVariableValues.get(localVar);
        } else if (expr instanceof ThisReference) {
            return thisValue;
        } else if (expr instanceof FieldAccess) {
            FieldAccess fieldAcc = (FieldAccess) expr;
            return fieldValues.get(fieldAcc);
        } else if (expr instanceof MethodCall) {
            MethodCall method = (MethodCall) expr;
            return methodValues.get(method);
        } else if (expr instanceof ArrayAccess) {
            ArrayAccess a = (ArrayAccess) expr;
            return arrayValues.get(a);
        } else if (expr instanceof ClassName) {
            ClassName c = (ClassName) expr;
            return classValues.get(c);
        } else {
            throw new BugInCF("Unexpected FlowExpression: " + expr + " (" + expr.getClass() + ")");
        }
    }

    /**
     * Returns current abstract value of a field access, or {@code null} if no information is
     * available.
     *
     * @return current abstract value of a field access, or {@code null} if no information is
     *     available
     */
    public @Nullable V getValue(FieldAccessNode n) {
        FieldAccess fieldAccess =
                FlowExpressions.internalReprOfFieldAccess(analysis.getTypeFactory(), n);
        return fieldValues.get(fieldAccess);
    }

    /**
     * Returns current abstract value of a method call, or {@code null} if no information is
     * available.
     *
     * @return current abstract value of a method call, or {@code null} if no information is
     *     available
     */
    public @Nullable V getValue(MethodInvocationNode n) {
        Receiver method = FlowExpressions.internalReprOf(analysis.getTypeFactory(), n, true);
        if (method == null) {
            return null;
        }
        return methodValues.get(method);
    }

    /**
     * Returns current abstract value of a field access, or {@code null} if no information is
     * available.
     *
     * @return current abstract value of a field access, or {@code null} if no information is
     *     available
     */
    public @Nullable V getValue(ArrayAccessNode n) {
        ArrayAccess arrayAccess =
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
     * <p>See {@link #removeConflicting(ArrayAccess,CFAbstractValue)}, as it is called first by this
     * method.
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
    protected void removeConflicting(FieldAccess fieldAccess, @Nullable V val) {
        final Iterator<Map.Entry<FieldAccess, V>> fieldValuesIterator =
                fieldValues.entrySet().iterator();
        while (fieldValuesIterator.hasNext()) {
            Map.Entry<FieldAccess, V> entry = fieldValuesIterator.next();
            FieldAccess otherFieldAccess = entry.getKey();
            V otherVal = entry.getValue();
            // case 2:
            if (otherFieldAccess.getReceiver().containsModifiableAliasOf(this, fieldAccess)) {
                fieldValuesIterator.remove(); // remove information completely
            }
            // case 1:
            else if (fieldAccess.getField().equals(otherFieldAccess.getField())) {
                if (canAlias(fieldAccess.getReceiver(), otherFieldAccess.getReceiver())) {
                    if (!otherFieldAccess.isFinal()) {
                        if (val != null) {
                            V newVal = val.leastUpperBound(otherVal);
                            entry.setValue(newVal);
                        } else {
                            // remove information completely
                            fieldValuesIterator.remove();
                        }
                    }
                }
            }
        }

        final Iterator<Map.Entry<ArrayAccess, V>> arrayValuesIterator =
                arrayValues.entrySet().iterator();
        while (arrayValuesIterator.hasNext()) {
            Map.Entry<ArrayAccess, V> entry = arrayValuesIterator.next();
            ArrayAccess otherArrayAccess = entry.getKey();
            if (otherArrayAccess.containsModifiableAliasOf(this, fieldAccess)) {
                // remove information completely
                arrayValuesIterator.remove();
            }
        }

        // case 3:
        methodValues.clear();
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
    protected void removeConflicting(ArrayAccess arrayAccess, @Nullable V val) {
        final Iterator<Map.Entry<ArrayAccess, V>> arrayValuesIterator =
                arrayValues.entrySet().iterator();
        while (arrayValuesIterator.hasNext()) {
            Map.Entry<ArrayAccess, V> entry = arrayValuesIterator.next();
            ArrayAccess otherArrayAccess = entry.getKey();
            // case 1:
            if (otherArrayAccess.containsModifiableAliasOf(this, arrayAccess)) {
                arrayValuesIterator.remove(); // remove information completely
            } else if (canAlias(arrayAccess.getReceiver(), otherArrayAccess.getReceiver())) {
                // TODO: one could be less strict here, and only raise the abstract
                // value for all array expressions with potentially aliasing receivers.
                arrayValuesIterator.remove(); // remove information completely
            }
        }

        // case 2:
        final Iterator<Map.Entry<FieldAccess, V>> fieldValuesIterator =
                fieldValues.entrySet().iterator();
        while (fieldValuesIterator.hasNext()) {
            Map.Entry<FieldAccess, V> entry = fieldValuesIterator.next();
            FieldAccess otherFieldAccess = entry.getKey();
            Receiver receiver = otherFieldAccess.getReceiver();
            if (receiver.containsModifiableAliasOf(this, arrayAccess)
                    && receiver.containsOfClass(ArrayAccess.class)) {
                // remove information completely
                fieldValuesIterator.remove();
            }
        }

        // case 3:
        methodValues.clear();
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
        final Iterator<Map.Entry<FieldAccess, V>> fieldValuesIterator =
                fieldValues.entrySet().iterator();
        while (fieldValuesIterator.hasNext()) {
            Map.Entry<FieldAccess, V> entry = fieldValuesIterator.next();
            FieldAccess otherFieldAccess = entry.getKey();
            // case 1:
            if (otherFieldAccess.containsSyntacticEqualReceiver(var)) {
                fieldValuesIterator.remove();
            }
        }

        final Iterator<Map.Entry<ArrayAccess, V>> arrayValuesIterator =
                arrayValues.entrySet().iterator();
        while (arrayValuesIterator.hasNext()) {
            Map.Entry<ArrayAccess, V> entry = arrayValuesIterator.next();
            ArrayAccess otherArrayAccess = entry.getKey();
            // case 2:
            if (otherArrayAccess.containsSyntacticEqualReceiver(var)) {
                arrayValuesIterator.remove();
            }
        }

        final Iterator<Map.Entry<MethodCall, V>> methodValuesIterator =
                methodValues.entrySet().iterator();
        while (methodValuesIterator.hasNext()) {
            Map.Entry<MethodCall, V> entry = methodValuesIterator.next();
            MethodCall otherMethodAccess = entry.getKey();
            // case 3:
            if (otherMethodAccess.containsSyntacticEqualReceiver(var)
                    || otherMethodAccess.containsSyntacticEqualParameter(var)) {
                methodValuesIterator.remove();
            }
        }
    }

    /**
     * Can the objects {@code a} and {@code b} be aliases? Returns a conservative answer (i.e.,
     * returns {@code true} if not enough information is available to determine aliasing).
     */
    @Override
    public boolean canAlias(Receiver a, Receiver b) {
        TypeMirror tb = b.getType();
        TypeMirror ta = a.getType();
        Types types = analysis.getTypes();
        return types.isSubtype(ta, tb) || types.isSubtype(tb, ta);
    }

    /* --------------------------------------------------------- */
    /* Handling of local variables */
    /* --------------------------------------------------------- */

    /**
     * Returns current abstract value of a local variable, or {@code null} if no information is
     * available.
     *
     * @return current abstract value of a local variable, or {@code null} if no information is
     *     available
     */
    public @Nullable V getValue(LocalVariableNode n) {
        Element el = n.getElement();
        return localVariableValues.get(new LocalVariable(el));
    }

    /* --------------------------------------------------------- */
    /* Handling of the current object */
    /* --------------------------------------------------------- */

    /**
     * Returns current abstract value of the current object, or {@code null} if no information is
     * available.
     *
     * @return current abstract value of the current object, or {@code null} if no information is
     *     available
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

        for (Map.Entry<LocalVariable, V> e : other.localVariableValues.entrySet()) {
            // local variables that are only part of one store, but not the
            // other are discarded, as one of store implicitly contains 'top'
            // for that variable.
            LocalVariable localVar = e.getKey();
            V thisVal = localVariableValues.get(localVar);
            if (thisVal != null) {
                V otherVal = e.getValue();
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

        for (Map.Entry<FieldAccess, V> e : other.fieldValues.entrySet()) {
            // information about fields that are only part of one store, but not
            // the other are discarded, as one store implicitly contains 'top'
            // for that field.
            FieldAccess el = e.getKey();
            V thisVal = fieldValues.get(el);
            if (thisVal != null) {
                V otherVal = e.getValue();
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);
                if (mergedVal != null) {
                    newStore.fieldValues.put(el, mergedVal);
                }
            }
        }
        for (Map.Entry<ArrayAccess, V> e : other.arrayValues.entrySet()) {
            // information about arrays that are only part of one store, but not
            // the other are discarded, as one store implicitly contains 'top'
            // for that array access.
            ArrayAccess el = e.getKey();
            V thisVal = arrayValues.get(el);
            if (thisVal != null) {
                V otherVal = e.getValue();
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);
                if (mergedVal != null) {
                    newStore.arrayValues.put(el, mergedVal);
                }
            }
        }
        for (Map.Entry<MethodCall, V> e : other.methodValues.entrySet()) {
            // information about methods that are only part of one store, but
            // not the other are discarded, as one store implicitly contains
            // 'top' for that field.
            MethodCall el = e.getKey();
            V thisVal = methodValues.get(el);
            if (thisVal != null) {
                V otherVal = e.getValue();
                V mergedVal = upperBoundOfValues(otherVal, thisVal, shouldWiden);
                if (mergedVal != null) {
                    newStore.methodValues.put(el, mergedVal);
                }
            }
        }
        for (Map.Entry<ClassName, V> e : other.classValues.entrySet()) {
            ClassName el = e.getKey();
            V thisVal = classValues.get(el);
            if (thisVal != null) {
                V otherVal = e.getValue();
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
        for (Map.Entry<LocalVariable, V> e : other.localVariableValues.entrySet()) {
            LocalVariable key = e.getKey();
            V value = localVariableValues.get(key);
            if (value == null || !value.equals(e.getValue())) {
                return false;
            }
        }
        for (Map.Entry<FieldAccess, V> e : other.fieldValues.entrySet()) {
            FieldAccess key = e.getKey();
            V value = fieldValues.get(key);
            if (value == null || !value.equals(e.getValue())) {
                return false;
            }
        }
        for (Map.Entry<ArrayAccess, V> e : other.arrayValues.entrySet()) {
            ArrayAccess key = e.getKey();
            V value = arrayValues.get(key);
            if (value == null || !value.equals(e.getValue())) {
                return false;
            }
        }
        for (Map.Entry<MethodCall, V> e : other.methodValues.entrySet()) {
            MethodCall key = e.getKey();
            V value = methodValues.get(key);
            if (value == null || !value.equals(e.getValue())) {
                return false;
            }
        }
        for (Map.Entry<ClassName, V> e : other.classValues.entrySet()) {
            ClassName key = e.getKey();
            V value = classValues.get(key);
            if (value == null || !value.equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
        return 22;
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
        String internal = internalVisualize(castedViz);
        if (internal.trim().isEmpty()) {
            return this.getClassAndUid() + "()";
        } else {
            return this.getClassAndUid() + "(" + viz.getSeparator() + internal + ")";
        }
    }

    /**
     * Adds a representation of the internal information of this Store to visualizer {@code viz}.
     *
     * @param viz the visualizer
     * @return a representation of the internal information of this {@link Store}
     */
    protected String internalVisualize(CFGVisualizer<V, S, ?> viz) {
        StringJoiner res = new StringJoiner(viz.getSeparator());
        for (Map.Entry<LocalVariable, V> entry : localVariableValues.entrySet()) {
            res.add(viz.visualizeStoreLocalVar(entry.getKey(), entry.getValue()));
        }
        if (thisValue != null) {
            res.add(viz.visualizeStoreThisVal(thisValue));
        }
        for (Map.Entry<FieldAccess, V> entry : fieldValues.entrySet()) {
            res.add(viz.visualizeStoreFieldVal(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<ArrayAccess, V> entry : arrayValues.entrySet()) {
            res.add(viz.visualizeStoreArrayVal(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<MethodCall, V> entry : methodValues.entrySet()) {
            res.add(viz.visualizeStoreMethodVals(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<ClassName, V> entry : classValues.entrySet()) {
            res.add(viz.visualizeStoreClassVals(entry.getKey(), entry.getValue()));
        }
        return res.toString();
    }
}

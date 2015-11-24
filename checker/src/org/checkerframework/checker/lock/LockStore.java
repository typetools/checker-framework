package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;

import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

/*
 * The Lock Store behaves like CFAbstractStore but requires the ability
 * to insert exact annotations. This is because we want to be able to
 * insert @LockPossiblyHeld to replace @LockHeld, which normally is
 * not possible in CFAbstractStore since @LockHeld is more specific.
 */
public class LockStore extends CFAbstractStore<CFValue, LockStore> {

    /** If true, indicates that the store refers to a point in the code
      * inside a constructor or initializer. This is useful because
      * constructors and initializers are special with regard to
      * the set of locks that is considered to be held. For example,
      * 'this' is considered to be held inside a constructor.
      */ 
    protected boolean inConstructorOrInitializer = false;

    protected final AnnotationMirror LOCKHELD = AnnotationUtils.fromClass(analysis.getTypeFactory().getElementUtils(), LockHeld.class);
    protected final AnnotationMirror LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(analysis.getTypeFactory().getElementUtils(), LockPossiblyHeld.class);

    public LockStore(LockAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    /** Copy constructor. */
    public LockStore(LockAnalysis analysis,
            CFAbstractStore<CFValue, LockStore> other) {
        super(other);
        inConstructorOrInitializer = ((LockStore)other).inConstructorOrInitializer;
    }

    @Override
    public LockStore leastUpperBound(LockStore other) {
        LockStore newStore = super.leastUpperBound(other);

        // Least upper bound of a boolean
        newStore.inConstructorOrInitializer = this.inConstructorOrInitializer && other.inConstructorOrInitializer;

        return newStore;
    }

    /*
     * Insert an annotation exactly, without regard to whether an annotation was already present.
     * This is only done for @LockPossiblyHeld. This is not sound for other type qualifiers.
     */
    public void insertLockPossiblyHeld(FlowExpressions.Receiver r) {
        CFValue value = analysis.createSingleAnnotationValue(LOCKPOSSIBLYHELD, r.getType());

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
            Element localVar = ((FlowExpressions.LocalVariable) r).getElement();
            localVariableValues.put(localVar, value);
        } else if (r instanceof FlowExpressions.FieldAccess) {
            FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) r;
            // Only store information about final fields (where the receiver is
            // also fixed) if concurrent semantics are enabled.

            // No need to handle monotonic updates as there are no monotonic qualifiers
            // in the @LockPossiblyHeld hierarchy.
            if (sequentialSemantics || fieldAcc.isUnmodifiableByOtherCode()) {
                fieldValues.put(fieldAcc, value);
            }
        } else if (r instanceof FlowExpressions.PureMethodCall) {
            FlowExpressions.PureMethodCall method = (FlowExpressions.PureMethodCall) r;
            // Don't store any information if concurrent semantics are enabled.
            if (sequentialSemantics) {
                methodValues.put(method, value);
            }
        } else if (r instanceof FlowExpressions.ArrayAccess) {
            FlowExpressions.ArrayAccess arrayAccess = (ArrayAccess) r;
            if (sequentialSemantics) {
                arrayValues.put(arrayAccess, value);
            }
        } else if (r instanceof FlowExpressions.ThisReference) {
            FlowExpressions.ThisReference thisRef = (FlowExpressions.ThisReference) r;
            // Only store information about final fields (where the receiver is
            // also fixed) if concurrent semantics are enabled.
            if (sequentialSemantics || thisRef.isUnmodifiableByOtherCode()) {
                thisValue = value;
            }
        } else if (r instanceof FlowExpressions.ClassName) {
            FlowExpressions.ClassName className = (FlowExpressions.ClassName) r;
            if (sequentialSemantics || className.isUnmodifiableByOtherCode()) {
                classValues.put(className, value);
            }
        } else {
            // No other types of expressions need to be stored.
        }
    }

    public void setInConstructorOrInitializer() {
        inConstructorOrInitializer = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public /*@Nullable*/ CFValue getValue(FlowExpressions.Receiver expr) {

        if (inConstructorOrInitializer) {
            if (expr instanceof FlowExpressions.ThisReference) {
                initializeThisValue(LOCKHELD, expr.getType());
            } else if (expr instanceof FlowExpressions.FieldAccess) {
                FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) expr;
                if (!fieldAcc.isStatic() && // Static fields are not automatically considered synchronized within a constructor or initializer
                    fieldAcc.getReceiver() instanceof FlowExpressions.ThisReference) {
                    insertValue(fieldAcc.getReceiver(), LOCKHELD);
                }
            }
        }

        return super.getValue(expr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalDotOutput(StringBuilder result) {
        result.append("  inConstructorOrInitializer = " + inConstructorOrInitializer
                + "\\n");
        super.internalDotOutput(result);
    }

    @Override
    protected boolean isSideEffectFree(AnnotatedTypeFactory atypeFactory,
            ExecutableElement method) {
        return ((LockAnnotatedTypeFactory) atypeFactory).methodSideEffectAnnotation(method, false) == SideEffectAnnotation.RELEASESNOLOCKS ||
               super.isSideEffectFree(atypeFactory, method);
    }

    // Same as super method, except for localVariableValues.clear();
    @Override
    public void updateForMethodCall(MethodInvocationNode n,
            AnnotatedTypeFactory atypeFactory, CFValue val) {
        ExecutableElement method = n.getTarget().getMethod();

        // case 1: remove information if necessary
        if (!(((LockChecker) analysis.getTypeFactory().getContext()).hasOption("assumeSideEffectFree")
              || isSideEffectFree(atypeFactory, method))) {
            // update field values
            Map<FlowExpressions.FieldAccess, CFValue> newFieldValues = new HashMap<>();
            for (Entry<FlowExpressions.FieldAccess, CFValue> e : fieldValues.entrySet()) {
                FlowExpressions.FieldAccess fieldAccess = e.getKey();
                CFValue otherVal = e.getValue();

                // case 3:
                List<Pair<AnnotationMirror, AnnotationMirror>> fieldAnnotations = atypeFactory
                        .getAnnotationWithMetaAnnotation(
                                fieldAccess.getField(),
                                MonotonicQualifier.class);
                CFValue newOtherVal = null;
                for (Pair<AnnotationMirror, AnnotationMirror> fieldAnnotation : fieldAnnotations) {
                    AnnotationMirror monotonicAnnotation = fieldAnnotation.second;
                    Name annotation = AnnotationUtils.getElementValueClassName(
                            monotonicAnnotation, "value", false);
                    AnnotationMirror target = AnnotationUtils.fromName(
                            atypeFactory.getElementUtils(), annotation);
                    AnnotationMirror anno = otherVal.getType()
                            .getAnnotationInHierarchy(target);
                    // Make sure the 'target' annotation is present.
                    if (anno != null && AnnotationUtils.areSame(anno, target)) {
                        newOtherVal = analysis.createSingleAnnotationValue(
                                target, otherVal.getType().getUnderlyingType())
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
                if (!fieldAccess.isUnmodifiableByOtherCode()) {
                    continue; // remove information completely
                }

                // keep information
                newFieldValues.put(fieldAccess, otherVal);
            }
            fieldValues = newFieldValues;
            
            localVariableValues.clear();

            // update method values
            methodValues.clear();

            arrayValues.clear();
        }

        // store information about method call if possible
        Receiver methodCall = FlowExpressions.internalReprOf(
                analysis.getTypeFactory(), n);
        replaceValue(methodCall, val);
    }
}

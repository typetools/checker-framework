package org.checkerframework.checker.lock;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;

/*
 * The Lock Store behaves like CFAbstractStore but requires the ability
 * to insert exact annotations. This is because we want to be able to
 * insert @LockPossiblyHeld to replace @LockHeld, which normally is
 * not possible in CFAbstractStore since @LockHeld is more specific.
 */
public class LockStore extends CFAbstractStore<CFValue, LockStore> {

    protected boolean inConstructorOrInitializer = false;

    protected final AnnotationMirror LOCKHELD = AnnotationUtils.fromClass(analysis.getTypeFactory().getElementUtils(), LockHeld.class);

    public LockStore(CFAbstractAnalysis<CFValue, LockStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    /** Copy constructor. */
    public LockStore(CFAbstractAnalysis<CFValue, LockStore, ?> analysis,
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
     */
    public void insertExactValue(FlowExpressions.Receiver r, AnnotationMirror a) {
        insertExactValue(r, analysis.createSingleAnnotationValue(a, r.getType()));
    }

    /*
     * Insert an annotation exactly, without regard to whether an annotation was already present.
     */
    public void insertExactValue(FlowExpressions.Receiver r, CFValue value) {
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
                return thisValue;
            } else if (expr instanceof FlowExpressions.FieldAccess) {
                FlowExpressions.FieldAccess fieldAcc = (FlowExpressions.FieldAccess) expr;
                if (!fieldAcc.isStatic() && // Static fields are not automatically considered synchronized within a constructor or initializer
                    fieldAcc.getReceiver() instanceof FlowExpressions.ThisReference) {
                    insertValue(fieldAcc, LOCKHELD);
                    return fieldValues.get(fieldAcc);
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
}
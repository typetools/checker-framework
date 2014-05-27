package org.checkerframework.checker.lock;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;

/*
 * The Lock Store behaves like CFAbstractStore but requires the ability
 * to insert exact annotations. This is because we want to be able to
 * insert @LockPossiblyHeld to replace @LockHeld, which normally is
 * not possible in CFAbstractStore since @LockHeld is more specific.
 */
public class LockStore extends CFAbstractStore<CFValue, LockStore> {

    public LockStore(CFAbstractAnalysis<CFValue, LockStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public LockStore(CFAbstractAnalysis<CFValue, LockStore, ?> analysis,
            CFAbstractStore<CFValue, LockStore> other) {
        super(other);
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
}
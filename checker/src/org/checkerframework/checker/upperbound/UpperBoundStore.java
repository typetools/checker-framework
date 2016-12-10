package org.checkerframework.checker.upperbound;

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

public class UpperBoundStore extends CFAbstractStore<UpperBoundValue, UpperBoundStore> {

    protected UpperBoundStore(UpperBoundStore other) {
        super(other);
    }

    public UpperBoundStore(
            CFAbstractAnalysis<UpperBoundValue, UpperBoundStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    // If we remove from a list it reduces the minlen of anything that could be an alias of the list by 1.
    // if we clear a list anything that could be an alias of this list goes to UpperBound(0).
    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory atypeFactory, UpperBoundValue val) {
        Receiver caller = FlowExpressions.internalReprOf(atypeFactory, n.getTarget().getReceiver());
        String methodName = n.getTarget().getMethod().toString();
        boolean remove = methodName.startsWith("remove(");
        boolean clear = methodName.startsWith("clear(");
        Map<Receiver, UpperBoundValue> replace = new HashMap<Receiver, UpperBoundValue>();
        if (clear) {
            for (FlowExpressions.LocalVariable rec : localVariableValues.keySet()) {
                if (caller.containsModifiableAliasOf(this, rec)) {
                    applyTransfer(rec, replace, true, atypeFactory);
                }
            }
            for (FieldAccess rec : fieldValues.keySet()) {
                if (caller.containsModifiableAliasOf(this, rec)) {
                    applyTransfer(rec, replace, true, atypeFactory);
                }
            }
        }
        if (remove) {
            for (FlowExpressions.LocalVariable rec : localVariableValues.keySet()) {
                if (caller.containsModifiableAliasOf(this, rec)) {
                    applyTransfer(rec, replace, false, atypeFactory);
                }
            }
            for (FieldAccess rec : fieldValues.keySet()) {
                if (caller.containsModifiableAliasOf(this, rec)) {
                    applyTransfer(rec, replace, false, atypeFactory);
                }
            }
        }
        for (Receiver rec : replace.keySet()) {
            replaceValue(rec, replace.get(rec));
        }

        super.updateForMethodCall(n, atypeFactory, val);
    }

    private void applyTransfer(
            Receiver rec,
            Map<Receiver, UpperBoundValue> replace,
            boolean isClear,
            AnnotatedTypeFactory atypeFactory) {}

    @Override
    public String toString() {
        String res = "";
        for (LocalVariable k : this.localVariableValues.keySet()) {
            UpperBoundValue anno = localVariableValues.get(k);
            res += k.toString() + ": " + anno.toString();
            res += "\n";
        }
        return res;
    }
}

package org.checkerframework.checker.minlen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;

public class MinLenStore extends CFAbstractStore<MinLenValue, MinLenStore> {

    protected MinLenStore(MinLenStore other) {
        super(other);
    }

    public MinLenStore(
            CFAbstractAnalysis<MinLenValue, MinLenStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    // If we remove from a list it reduces the minlen of anything that could be an alias of the list by 1.
    // if we clear a list anything that could be an alias of this list goes to MinLen(0).
    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory atypeFactory, MinLenValue val) {
        Receiver caller = FlowExpressions.internalReprOf(atypeFactory, n.getTarget().getReceiver());
        String methodName = n.getTarget().getMethod().toString();
        boolean remove = methodName.startsWith("remove(");
        boolean clear = methodName.startsWith("clear(");
        Map<Receiver, MinLenValue> replace = new HashMap<Receiver, MinLenValue>();
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
            Map<Receiver, MinLenValue> replace,
            boolean isClear,
            AnnotatedTypeFactory atypeFactory) {
        MinLenAnnotatedTypeFactory factory = (MinLenAnnotatedTypeFactory) atypeFactory;
        MinLenValue value = this.getValue(rec);
        Set<AnnotationMirror> atm = value.getAnnotations();
        if (AnnotationUtils.containsSameByClass(atm, MinLen.class)) {
            if (isClear) {
                MinLenValue val =
                        analysis.createSingleAnnotationValue(
                                factory.createMinLen(0), rec.getType());
                replace.put(rec, val);
            } else {
                int length =
                        MinLenAnnotatedTypeFactory.getMinLenValue(
                                AnnotationUtils.getAnnotationByClass(atm, MinLen.class));
                MinLenValue val =
                        analysis.createSingleAnnotationValue(
                                factory.createMinLen(Math.max(length - 1, 0)), rec.getType());
                replace.put(rec, val);
            }
        }
    }

    @Override
    public String toString() {
        String res = "";
        for (LocalVariable k : this.localVariableValues.keySet()) {
            MinLenValue anno = localVariableValues.get(k);
            res += k.toString() + ": " + anno.toString();
            res += "\n";
        }
        return res;
    }
}

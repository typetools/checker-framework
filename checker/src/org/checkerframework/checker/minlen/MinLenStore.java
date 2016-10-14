package org.checkerframework.checker.minlen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.ArrayAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class MinLenStore extends CFAbstractStore<MinLenValue, MinLenStore> {

    protected MinLenStore(MinLenStore other) {
        super(other);
    }

    public MinLenStore(
            CFAbstractAnalysis<MinLenValue, MinLenStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    // changes values similar to ++ if we remove from a list
    // if we clear a list anything NonNegatvie goes to NonNeg else becomes unknown
    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory atypeFactory, MinLenValue val) {

        String methodName = n.getTarget().getMethod().toString();
        boolean remove = methodName.startsWith("remove(");
        boolean clear = methodName.startsWith("clear(");
        Map<Receiver, MinLenValue> replace = new HashMap<Receiver, MinLenValue>();
        if (clear) {
            for (FlowExpressions.LocalVariable rec : localVariableValues.keySet()) {
                applyTransfer(rec, replace, true, atypeFactory);
            }
            for (FieldAccess rec : fieldValues.keySet()) {
                applyTransfer(rec, replace, true, atypeFactory);
            }
        }
        if (remove) {
            for (FlowExpressions.LocalVariable rec : localVariableValues.keySet()) {
                applyTransfer(rec, replace, false, atypeFactory);
            }
            for (FieldAccess rec : fieldValues.keySet()) {
                applyTransfer(rec, replace, false, atypeFactory);
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
        AnnotatedTypeMirror atm = value.getType();
        if (atm.hasAnnotation(MinLen.class)) {
            if (isClear) {
                MinLenValue val =
                        analysis.createSingleAnnotationValue(
                                factory.createMinLen(0), rec.getType());
                replace.put(rec, val);
            } else {
                int length =
                        MinLenAnnotatedTypeFactory.getMinLenValue(atm.getAnnotation(MinLen.class));
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

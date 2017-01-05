package org.checkerframework.checker.minlen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.minlen.qual.MinLen;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;

public class MinLenStore extends CFAbstractStore<CFValue, MinLenStore> {

    protected MinLenStore(MinLenStore other) {
        super(other);
    }

    public MinLenStore(
            CFAbstractAnalysis<CFValue, MinLenStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    @Override
    public void updateForMethodCall(
            MethodInvocationNode n, AnnotatedTypeFactory atypeFactory, CFValue val) {
        super.updateForMethodCall(n, atypeFactory, val);
        MinLenAnnotatedTypeFactory factory = (MinLenAnnotatedTypeFactory) atypeFactory;

        // Calling certain methods on a list object side-effects the list.  If the list is a local
        // variable or a final or effectively final field, then dataflow does not update the store
        // to reflect these side-effects.  This method correctly updates refined types for those
        // variables including their aliases.;
        boolean remove = factory.isListRemove(n.getTarget().getMethod());
        boolean clear = factory.isListClear(n.getTarget().getMethod());
        if (!(remove || clear)) {
            return;
        }

        Receiver caller = FlowExpressions.internalReprOf(atypeFactory, n.getTarget().getReceiver());
        List<Entry<? extends Receiver, CFValue>> localAndFields = new ArrayList<>();
        localAndFields.addAll(localVariableValues.entrySet());
        localAndFields.addAll(fieldValues.entrySet());

        for (Map.Entry<? extends Receiver, CFValue> entry : localAndFields) {
            Receiver rec = entry.getKey();
            CFValue value = entry.getValue();
            if (caller.containsModifiableAliasOf(this, rec)
                    && AnnotationUtils.containsSameByClass(value.getAnnotations(), MinLen.class)) {
                AnnotationMirror newMinLen;
                if (clear) {
                    newMinLen = factory.MIN_LEN_0;
                } else { // if (remove)
                    AnnotationMirror minLen =
                            AnnotationUtils.getAnnotationByClass(
                                    value.getAnnotations(), MinLen.class);
                    int length = MinLenAnnotatedTypeFactory.getMinLenValue(minLen);
                    newMinLen = factory.createMinLen(Math.max(length - 1, 0));
                }
                CFValue newValue = analysis.createSingleAnnotationValue(newMinLen, rec.getType());
                replaceValue(rec, newValue);
            }
        }
    }

    @Override
    public String toString() {
        String res = "";
        for (LocalVariable k : this.localVariableValues.keySet()) {
            CFValue anno = localVariableValues.get(k);
            res += k.toString() + ": " + anno.toString();
            res += "\n";
        }
        return res;
    }
}

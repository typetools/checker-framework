package org.checkerframework.checker.minlen;

import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

public class MinLenStore extends CFAbstractStore<MinLenValue, MinLenStore> {

    protected MinLenStore(MinLenStore other) {
        super(other);
    }

    public MinLenStore(
            CFAbstractAnalysis<MinLenValue, MinLenStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
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

package org.checkerframework.dataflow.cfg;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;

/** Generate a string representation of a Store. */
public class StoreToString<A extends AbstractValue<A>, S extends Store<S>>
        extends DOTCFGVisualizer<A, S, TransferFunction<A, S>> {

    private static final String PREFIX = "org.checkerframework.framework.flow.CFStore (\\n";
    private static final String SUFFIX = "\\n)";
    private static final String lineSep = System.lineSeparator();

    public StoreToString() {
        this.sbStore = new StringBuilder();
    }

    public String getStoreString() {
        StringBuilder result = sbStore;

        if (result.indexOf(PREFIX) == 0) {
            result.delete(0, PREFIX.length());
        }
        int suffixStart = result.length() - SUFFIX.length();
        if (result.substring(result.length() - SUFFIX.length()).equals(SUFFIX)) {
            result.delete(suffixStart, result.length());
        }

        int newlinePos;
        while ((newlinePos = result.indexOf("\\n")) != -1) {
            result.replace(newlinePos, newlinePos + 2, lineSep);
        }

        return result.toString();
    }
}

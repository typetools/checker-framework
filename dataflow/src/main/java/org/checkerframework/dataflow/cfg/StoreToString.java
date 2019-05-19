package org.checkerframework.dataflow.cfg;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;

/** Generate a string representation of a Store. */
public class StoreToString<A extends AbstractValue<A>, S extends Store<S>>
        extends DOTCFGVisualizer<A, S, TransferFunction<A, S>> {

    /** A string to remove from the front of DOTCFGVisualizer's output. */
    private static final String PREFIX = "org.checkerframework.framework.flow.CFStore (\\n";
    /** A string to remove from the end of DOTCFGVisualizer's output. */
    private static final String SUFFIX = "\\n)";
    /** The system-specific line separator. */
    private static final String lineSep = System.lineSeparator();

    /** Creates a new Store printer. */
    public StoreToString() {
        this.sbStore = new StringBuilder();
    }

    /**
     * Gets the string representation of the most recently-visualized store.
     *
     * @return the string representation of the most recently-visualized store
     */
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

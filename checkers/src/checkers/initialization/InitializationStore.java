package checkers.initialization;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;

import checkers.flow.analysis.FlowExpressions.FieldAccess;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFValue;

/**
 * A store that extends {@code CFAbstractStore} and additionally tracks which
 * fields of the 'self' reference have been initialized.
 * 
 * @author Stefan Heule
 * @see InitializationTransfer
 */
public class InitializationStore extends CFAbstractStore<CFValue, InitializationStore> {

    /** The list of fields that are initialized. */
    protected final Set<Element> initializedFields;

    public InitializationStore(
            CFAbstractAnalysis<CFValue, InitializationStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        initializedFields = new HashSet<>();
    }

    /** A copy constructor. */
    public InitializationStore(InitializationStore other) {
        super(other);
        initializedFields = new HashSet<>(other.initializedFields);
    }

    /**
     * Mark the field identified by the element {@code f} as initialized.
     */
    public void addInitializedField(Element f) {
        initializedFields.add(f);
    }

    /**
     * Is the field identified by the element {@code f} initialized?
     */
    public boolean isFieldInitialized(Element f) {
        return initializedFields.contains(f);
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<CFValue, InitializationStore> o) {
        if (!(o instanceof InitializationStore)) {
            return false;
        }
        InitializationStore other = (InitializationStore) o;
        for (Element field : other.initializedFields) {
            if (!initializedFields.contains(field)) {
                return false;
            }
        }
        return super.supersetOf(other);
    }

    @Override
    public InitializationStore leastUpperBound(InitializationStore other) {
        InitializationStore result = super.leastUpperBound(other);

        // Set intersection for initializedFields.
        result.initializedFields.addAll(other.initializedFields);
        result.initializedFields.retainAll(initializedFields);

        return result;
    }

    @Override
    protected void internalDotOutput(StringBuilder result) {
        super.internalDotOutput(result);
        result.append("  initialized fields = " + initializedFields + "\\n");
    }
    
    public Map<FieldAccess, CFValue> getFieldValues() {
        return fieldValues;
    }

    public CFAbstractAnalysis<CFValue, InitializationStore, ?> getAnalysis() {
        return analysis;
    }
}

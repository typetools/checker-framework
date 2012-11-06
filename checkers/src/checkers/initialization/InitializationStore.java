package checkers.initialization;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import javacutils.AnnotationUtils;

import dataflow.analysis.FlowExpressions;
import dataflow.analysis.FlowExpressions.FieldAccess;
import dataflow.analysis.FlowExpressions.ThisReference;
import dataflow.cfg.node.MethodInvocationNode;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.types.AnnotatedTypeFactory;

/**
 * A store that extends {@code CFAbstractStore} and additionally tracks which
 * fields of the 'self' reference have been initialized.
 *
 * @author Stefan Heule
 * @see InitializationTransfer
 */
public class InitializationStore extends
        CFAbstractStore<CFValue, InitializationStore> {

    /** The list of fields that are initialized. */
    protected final Set<Element> initializedFields;

    public InitializationStore(
            CFAbstractAnalysis<CFValue, InitializationStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        initializedFields = new HashSet<>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Additionally, the {@link InitializationStore} keeps all field values for
     * fields that have the 'invariant' annotation.
     */
    @Override
    public void updateForMethodCall(MethodInvocationNode n,
            AnnotatedTypeFactory atypeFactory, CFValue val) {
        InitializationChecker checker = (InitializationChecker) analysis
                .getFactory().getChecker();
        AnnotationMirror fieldInvariantAnnotation = checker
                .getFieldInvariantAnnotation();

        // Are there fields that have the 'invariant' annotations and are in the
        // store?
        Set<FlowExpressions.FieldAccess> invariantFields = new HashSet<>();
        for (Entry<FlowExpressions.FieldAccess, CFValue> e : fieldValues
                .entrySet()) {
            FlowExpressions.FieldAccess fieldAccess = e.getKey();
            Set<AnnotationMirror> declaredAnnos = atypeFactory
                    .getAnnotatedType(fieldAccess.getField()).getAnnotations();
            if (AnnotationUtils.containsSame(declaredAnnos,
                    fieldInvariantAnnotation)) {
                invariantFields.add(fieldAccess);
            }
        }

        super.updateForMethodCall(n, atypeFactory, val);

        // Add invariant annotation again.
        for (FieldAccess invariantField : invariantFields) {
            insertValue(invariantField, fieldInvariantAnnotation);
        }
    }

    /** A copy constructor. */
    public InitializationStore(InitializationStore other) {
        super(other);
        initializedFields = new HashSet<>(other.initializedFields);
    }

    /**
     * Mark the field identified by the element {@code field} as initialized (if
     * it belongs to the current class, or is static (in which case there is no
     * aliasing issue and we can just add all static fields).
     */
    public void addInitializedField(FieldAccess field) {
        boolean fieldOnThisReference = field.getReceiver() instanceof ThisReference;
        boolean staticField = field.isStatic();
        if (fieldOnThisReference || staticField) {
            initializedFields.add(field.getField());
        }
    }

    /**
     * Mark the field identified by the element {@code f} as initialized (the
     * caller needs to ensure that the field belongs to the current class, or is
     * a static field).
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

package checkers.flow.analysis.checkers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.analysis.Analysis;
import checkers.flow.analysis.checkers.CFAbstractValue.InferredAnnotation;
import checkers.flow.cfg.CFGDOTVisualizer;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.Pair;

/**
 * {@link CFAbstractAnalysis} is an extensible dataflow analysis for the Checker
 * Framework that tracks the annotations using a flow-sensitive analysis. It
 * uses an {@link AnnotatedTypeFactory} to provide checker-specific logic how to
 * combine types (e.g., what is the type of a string concatenation, given the
 * types of the two operands) and as an abstraction function (e.g., determine
 * the annotations on literals).
 *
 * <p>
 * The purpose of this class is twofold: Firstly, it serves as factory for
 * abstract values, stores and the transfer function. Furthermore, it makes it
 * easy for the transfer function and the stores to access the
 * {@link AnnoatedTypeFactory}, the qualifier hierarchy, etc.
 *
 * @author Charlie Garrett
 * @author Stefan Heule
 *
 */
public abstract class CFAbstractAnalysis<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>, T extends CFAbstractTransfer<V, S, T>>
        extends Analysis<V, S, T> {
    /**
     * The qualifier hierarchy for which to track annotations.
     */
    protected final QualifierHierarchy qualifierHierarchy;

    /**
     * The 'top' annotations in all hierarchies. This is used in the abstract
     * values to determine the index at which to store various annotations.
     */
    protected final AnnotationMirror[] tops;

    /**
     * A type factory that can provide static type annotations for AST Trees.
     */
    protected final AbstractBasicAnnotatedTypeFactory<? extends BaseTypeChecker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> atypeFactory;

    /**
     * A checker used to do error reporting.
     */
    protected final BaseTypeChecker checker;

    protected List<Pair<VariableElement, V>> fieldValues;

    public <Checker extends BaseTypeChecker> CFAbstractAnalysis(
            AbstractBasicAnnotatedTypeFactory<Checker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            ProcessingEnvironment env, Checker checker) {
        super(env);
        qualifierHierarchy = factory.getQualifierHierarchy();
        this.atypeFactory = factory;
        this.checker = checker;
        transferFunction = createTransferFunction();
        Set<AnnotationMirror> topAnnotations = factory.getQualifierHierarchy()
                .getTopAnnotations();
        tops = new ArrayList<>(topAnnotations)
                .toArray(new AnnotationMirror[topAnnotations.size()]);

        fieldValues = Collections.emptyList();
    }

    public <Checker extends BaseTypeChecker> CFAbstractAnalysis(
            AbstractBasicAnnotatedTypeFactory<Checker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            ProcessingEnvironment env, Checker checker,
            List<Pair<VariableElement, V>> fieldValues) {
        this(factory, env, checker);
        this.fieldValues = fieldValues;
    }

    public List<Pair<VariableElement, V>> getFieldValues() {
        return fieldValues;
    }

    /**
     * @return The transfer function to be used by the analysis.
     */
    @SuppressWarnings("unchecked")
    public T createTransferFunction() {
        @SuppressWarnings("rawtypes")
        AbstractBasicAnnotatedTypeFactory f = atypeFactory;
        return (T) f.createFlowTransferFunction(this);
    }

    /**
     * @return An empty store of the appropriate type.
     */
    public abstract S createEmptyStore(boolean sequentialSemantics);

    /**
     * @return An identical copy of the store {@code s}.
     */
    public abstract S createCopiedStore(S s);

    /**
     * @return An abstract value containing the valid subset of annotations of
     *         {@code annotations}.
     */
    public abstract/* @Nullable */V createAbstractValue(
            Set<AnnotationMirror> annotationSet);

    /**
     * Creates an abstract value given.
     */
    public abstract V createAbstractValue(
            InferredAnnotation[] resultAnnotations);

    public QualifierHierarchy getTypeHierarchy() {
        return qualifierHierarchy;
    }

    public AbstractBasicAnnotatedTypeFactory<? extends BaseTypeChecker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> getFactory() {
        return atypeFactory;
    }

    /**
     * Print a DOT graph of the CFG and analysis info for inspection.
     */
    public void outputToDotFile(String outputFile) {
        String s = CFGDOTVisualizer.visualize(cfg.getEntryBlock(), this);

        try {
            FileWriter fstream = new FileWriter(outputFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

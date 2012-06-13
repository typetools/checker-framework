package checkers.flow.analysis.checkers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.analysis.Analysis;
import checkers.flow.cfg.CFGDOTVisualizer;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;

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
     * A type factory that can provide static type annotations for AST Trees.
     */
    protected final AbstractBasicAnnotatedTypeFactory<? extends BaseTypeChecker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory;

    /**
     * A checker used to do error reporting.
     */
    protected final BaseTypeChecker checker;

    /**
     * The set of annotations that the flow analysis should track (must be a
     * subset of the legal annotations of the qualifier hierarchy).
     */
    protected final Set<AnnotationMirror> supportedAnnotations;

    public <Checker extends BaseTypeChecker> CFAbstractAnalysis(
            AbstractBasicAnnotatedTypeFactory<Checker, V, S, T, ? extends CFAbstractAnalysis<V, S, T>> factory,
            ProcessingEnvironment env, Checker checker) {
        super(env);
        qualifierHierarchy = factory.getQualifierHierarchy();
        this.factory = factory;
        transferFunction = createTransferFunction();
        this.checker = checker;

        // Build the set of supported annotations.
        supportedAnnotations = AnnotationUtils.createAnnotationSet();
        Set<Class<? extends Annotation>> noFlowInferenceAnnotations = factory
                .noFlowInferenceAnnotations();
        for (AnnotationMirror a : qualifierHierarchy.getAnnotations()) {
            boolean add = true;
            for (Class<? extends Annotation> c : noFlowInferenceAnnotations) {
                if (AnnotationUtils.areSameByClass(a, c)) {
                    add = false;
                }
            }
            if (add) {
                supportedAnnotations.add(a);
            }
        }
    }

    /**
     * @return The transfer function to be used by the analysis.
     */
    protected abstract T createTransferFunction();

    /**
     * @return An empty store of the appropriate type.
     */
    protected abstract S createEmptyStore();

    /**
     * @return An identical copy of the store {@code s}.
     */
    protected abstract S createCopiedStore(S s);

    /**
     * @return An abstract value containing the valid subset of annotations of
     *         {@code annotations} (or {@code null} if that set is empty).
     */
    protected abstract/* @Nullable */V createAbstractValue(
            Set<AnnotationMirror> annotations);

    public QualifierHierarchy getTypeHierarchy() {
        return qualifierHierarchy;
    }

    public AnnotatedTypeFactory getFactory() {
        return factory;
    }

    public Set<AnnotationMirror> getSupportedAnnotations() {
        return supportedAnnotations;
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

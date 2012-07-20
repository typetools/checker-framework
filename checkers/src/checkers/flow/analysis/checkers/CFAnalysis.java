package checkers.flow.analysis.checkers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.analysis.checkers.CFAbstractValue.InferredAnnotation;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.Pair;

/**
 * The default dataflow analysis used in the Checker Framework.
 * 
 * @author Stefan Heule
 * 
 */
public class CFAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, CFTransfer> {

    public <Checker extends BaseTypeChecker> CFAnalysis(
            AbstractBasicAnnotatedTypeFactory<Checker, CFValue, CFStore, CFTransfer, CFAnalysis> factory,
            ProcessingEnvironment env, Checker checker) {
        super(factory, env, checker);
    }

    public <Checker extends BaseTypeChecker> CFAnalysis(
            AbstractBasicAnnotatedTypeFactory<Checker, CFValue, CFStore, CFTransfer, CFAnalysis> factory,
            ProcessingEnvironment env, Checker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
    }

    @Override
    protected CFTransfer createTransferFunction() {
        return new CFTransfer(this);
    }

    @Override
    protected CFStore createEmptyStore(boolean sequentialSemantics) {
        return new CFStore(this, sequentialSemantics);
    }

    @Override
    protected CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    protected/* @Nullable */CFValue createAbstractValue(
            Set<AnnotationMirror> annotations) {
        return defaultCreateAbstractValue(annotations, supportedAnnotations,
                this);
    }

    @Override
    protected CFValue createAbstractValue(InferredAnnotation[] annotations) {
        return defaultCreateAbstractValue(annotations, this);
    }

    /**
     * Creates a {@link CFValue} from a given array of
     * {@link InferredAnnotation}s. This allows the creation of {@link CFValue}
     * objects where annotations are only available for some of the hierarchies.
     */
    public static CFValue defaultCreateAbstractValue(
            InferredAnnotation[] annotations,
            CFAbstractAnalysis<CFValue, ?, ?> analysis) {
        return new CFValue(analysis, annotations);
    }

    /**
     * Only uses the legal annotations in {@code annotations} and
     * {@code effectiveAnnotations}, and creates a {@link CFValue}. It is
     * assumed that information for all hierarchies is available. If for a given
     * hierarchy, the set does not contain an annotation, then it is assumed
     * that "no annotation" is the correct information for that hierarchy.
     */
    public static CFValue defaultCreateAbstractValue(
            Set<AnnotationMirror> annotations,
            Set<AnnotationMirror> legalAnnotations,
            CFAbstractAnalysis<CFValue, ?, ?> analysis) {
        Set<AnnotationMirror> as = new HashSet<>();
        for (AnnotationMirror a : annotations) {
            if (AnnotationUtils.containsSameIgnoringValues(legalAnnotations, a)) {
                as.add(a);
            }
        }
        return new CFValue(analysis, as);
    }
}

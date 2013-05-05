package checkers.flow;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import javacutils.Pair;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;

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
    public CFStore createEmptyStore(boolean sequentialSemantics) {
        return new CFStore(this, sequentialSemantics);
    }

    @Override
    public CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
        return defaultCreateAbstractValue(this, type);
    }
}

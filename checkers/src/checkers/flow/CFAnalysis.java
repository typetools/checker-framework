package checkers.flow;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;

import javacutils.Pair;

import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * The default dataflow analysis used in the Checker Framework.
 *
 * @author Stefan Heule
 *
 */
public class CFAnalysis extends CFAbstractAnalysis<CFValue, CFStore, CFTransfer> {

    public CFAnalysis(BaseTypeChecker checker,
            AbstractBasicAnnotatedTypeFactory<CFValue, CFStore, CFTransfer, CFAnalysis> factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
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

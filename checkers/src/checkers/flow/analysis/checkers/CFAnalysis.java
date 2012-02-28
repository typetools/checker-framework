package checkers.flow.analysis.checkers;

import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotatedTypeFactory;

/**
 * The default dataflow analysis used in the Checker Framework.
 * 
 * @author Stefan Heule
 * 
 */
public class CFAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, CFTransfer> {

    public CFAnalysis(AnnotatedTypeFactory factory, ProcessingEnvironment env) {
        super(factory, env);
    }

    @Override
    protected CFTransfer createTransferFunction() {
        return new CFTransfer(this);
    }

    @Override
    protected CFStore createEmptyStore() {
        return new CFStore(this);
    }

    @Override
    protected CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    protected CFValue createAbstractValue(Set<AnnotationMirror> annotations) {
        return new CFValue(this, annotations);
    }

}

package checkers.types;

import java.util.List;

import javax.lang.model.element.VariableElement;

import javacutils.Pair;

import dataflow.analysis.checkers.CFAnalysis;
import dataflow.analysis.checkers.CFStore;
import dataflow.analysis.checkers.CFTransfer;
import dataflow.analysis.checkers.CFValue;

import checkers.basetype.BaseTypeChecker;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A factory that extends {@link AbstractBasicAnnotatedTypeFactory} to use the
 * default flow-sensitive analysis as provided by {@link CFAnalysis}.
 * 
 * @author Stefan Heule
 */
public class BasicAnnotatedTypeFactory<Checker extends BaseTypeChecker>
        extends
        AbstractBasicAnnotatedTypeFactory<Checker, CFValue, CFStore, CFTransfer, CFAnalysis> {

    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root,
            boolean useFlow) {
        super(checker, root, useFlow);
    }

    public BasicAnnotatedTypeFactory(Checker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    protected CFAnalysis createFlowAnalysis(Checker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new CFAnalysis(this, processingEnv, checker, fieldValues);
    }
}

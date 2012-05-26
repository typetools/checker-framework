package checkers.types;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.analysis.checkers.CFAnalysis;
import checkers.flow.analysis.checkers.CFStore;
import checkers.flow.analysis.checkers.CFTransfer;
import checkers.flow.analysis.checkers.CFValue;

import com.sun.source.tree.CompilationUnitTree;

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
    protected CFAnalysis createFlowAnalysis(Checker checker) {
        return new CFAnalysis(this, getEnv(), checker);
    }
}

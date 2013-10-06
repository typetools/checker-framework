package checkers.types;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.CFAnalysis;
import checkers.flow.CFStore;
import checkers.flow.CFTransfer;
import checkers.flow.CFValue;

import javacutils.Pair;

import java.util.List;

import javax.lang.model.element.VariableElement;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A factory that extends {@link AbstractBasicAnnotatedTypeFactory} to use the
 * default flow-sensitive analysis as provided by {@link CFAnalysis}.
 *
 * @author Stefan Heule
 */
public class SubtypingAnnotatedTypeFactory<Checker extends BaseTypeChecker<?>>
    extends AbstractBasicAnnotatedTypeFactory<Checker, CFValue, CFStore, CFTransfer, CFAnalysis> {

    public SubtypingAnnotatedTypeFactory(Checker checker, CompilationUnitTree root,
            boolean useFlow) {
        super(checker, root, useFlow);
        // Every subclass must call postInit!
        if (this.getClass().equals(SubtypingAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    public SubtypingAnnotatedTypeFactory(Checker checker, CompilationUnitTree root) {
        this(checker, root, FLOW_BY_DEFAULT);
    }

    @Override
    protected CFAnalysis createFlowAnalysis(Checker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        return new CFAnalysis(this, processingEnv, checker, fieldValues);
    }
}

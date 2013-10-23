package checkers.basetype;

import checkers.flow.CFAnalysis;
import checkers.flow.CFStore;
import checkers.flow.CFTransfer;
import checkers.flow.CFValue;
import checkers.types.AbstractBasicAnnotatedTypeFactory;

import javacutils.Pair;

import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * A factory that extends {@link AbstractBasicAnnotatedTypeFactory} to use the
 * default flow-sensitive analysis as provided by {@link CFAnalysis}.
 *
 * @author Stefan Heule
 */
public class BaseAnnotatedTypeFactory
    extends AbstractBasicAnnotatedTypeFactory<CFValue, CFStore, CFTransfer, CFAnalysis> {

    public BaseAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker, useFlow);

        // Every subclass must call postInit!
        if (this.getClass().equals(BaseAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    public BaseAnnotatedTypeFactory(BaseTypeChecker checker) {
        this(checker, FLOW_BY_DEFAULT);
    }

    @Override
    protected CFAnalysis createFlowAnalysis(List<Pair<VariableElement, CFValue>> fieldValues) {
        return new CFAnalysis(checker, this, fieldValues);
    }
}

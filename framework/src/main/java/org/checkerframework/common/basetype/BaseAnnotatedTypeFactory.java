package org.checkerframework.common.basetype;

import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.Pair;

/**
 * A factory that extends {@link GenericAnnotatedTypeFactory} to use the default flow-sensitive
 * analysis as provided by {@link CFAnalysis}.
 */
public class BaseAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<CFValue, CFStore, CFTransfer, CFAnalysis> {

    public BaseAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker, useFlow);

        // Every subclass must call postInit!
        if (this.getClass() == BaseAnnotatedTypeFactory.class) {
            this.postInit();
        }
    }

    public BaseAnnotatedTypeFactory(BaseTypeChecker checker) {
        this(checker, flowByDefault);
    }

    @Override
    protected CFAnalysis createFlowAnalysis(List<Pair<VariableElement, CFValue>> fieldValues) {
        return new CFAnalysis(checker, this, fieldValues);
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        CFTransfer ret = new BaseTypeTransfer(analysis);
        return ret;
    }

    /** Checks the flow of data with regard to the Base Type checker. */
    private class BaseTypeTransfer extends CFTransfer {

        /** The type factory obtained from control flow analysis */
        private AnnotatedTypeFactory factory;

        /**
         * Constructor function and obtaining the type factory from the control flow abstract
         * analysis
         *
         * @param analysis the control flow analysis whose annotated type factory is required
         */
        public BaseTypeTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
            super(analysis);
            factory = analysis.getTypeFactory();
        }

        @Override
        public TransferResult<CFValue, CFStore> visitInstanceOf(
                InstanceOfNode node, TransferInput<CFValue, CFStore> in) {
            TransferResult<CFValue, CFStore> result = super.visitInstanceOf(node, in);
            if (node.getTree().getType().getKind() == Tree.Kind.ANNOTATED_TYPE) {
                AnnotatedTypeMirror type = factory.getAnnotatedType(node.getTree().getType());
                AnnotatedTypeMirror exp = factory.getAnnotatedType(node.getTree().getExpression());
                if (factory.getTypeHierarchy().isSubtype(type, exp)
                        && !type.getAnnotations().equals(exp.getAnnotations())) {
                    FlowExpressions.Receiver receiver =
                            FlowExpressions.internalReprOf(
                                    analysis.getTypeFactory(), node.getTree().getExpression());
                    for (AnnotationMirror anno : type.getAnnotations()) {
                        in.getRegularStore().insertOrRefine(receiver, anno);
                    }
                    return new RegularTransferResult<>(
                            result.getResultValue(), in.getRegularStore());
                }
            }
            return result;
        }
    }
}

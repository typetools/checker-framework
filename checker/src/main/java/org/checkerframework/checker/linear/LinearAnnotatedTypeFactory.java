package org.checkerframework.checker.linear;

import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.linear.qual.Linear;
import org.checkerframework.checker.linear.qual.Normal;
import org.checkerframework.checker.linear.qual.Unusable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * Marks a {@link Linear} variable as {@link Unusable} once the variable is the receiver of a method
 * call
 */
public class LinearAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The @{@link Linear} annotation. */
    final AnnotationMirror LINEAR;
    /** The @{@link Unusable} annotation. */
    final AnnotationMirror UNUSABLE;
    /** The @{@link Normal} annotation. */
    final AnnotationMirror NORMAL;

    /**
     * Constructor function and building LINEAR, UNUSABLE and NORMAL annotation mirrors from
     * classes.
     *
     * @param checker the associated {@link LinearChecker}
     */
    @SuppressWarnings("method.invocation.invalid")
    public LinearAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        LINEAR = AnnotationBuilder.fromClass(elements, Linear.class);
        UNUSABLE = AnnotationBuilder.fromClass(elements, Unusable.class);
        NORMAL = AnnotationBuilder.fromClass(elements, Normal.class);

        this.postInit();
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new LinearFlow(analysis);
    }

    /**
     * Checks the flow of data with regard to the linear checker. Changes annotations of receiver on
     * method invocation.
     */
    private class LinearFlow extends CFTransfer {
        /** The type factory obtained from control flow analysis */
        private final AnnotatedTypeFactory factory;

        /**
         * Constructor function and obtaining the type factory from the control flow abstract
         * analysis
         *
         * @param analysis the control flow analysis whose annotated type factory is required
         */
        LinearFlow(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
            super(analysis);
            factory = analysis.getTypeFactory();
        }

        @Override
        public TransferResult<CFValue, CFStore> visitMethodInvocation(
                MethodInvocationNode node, TransferInput<CFValue, CFStore> in) {
            TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);
            @Nullable Tree receiverTree = node.getTarget().getReceiver().getTree();
            if (receiverTree != null) {
                AnnotatedTypeMirror type = factory.getAnnotatedType(receiverTree);
                if (type.hasAnnotation(Linear.class)) {
                    FlowExpressions.Receiver receiver =
                            FlowExpressions.internalReprOf(
                                    analysis.getTypeFactory(), node.getTarget().getReceiver());
                    in.getRegularStore().insertOrRefine(receiver, UNUSABLE);
                    return new RegularTransferResult<>(
                            analysis.createSingleAnnotationValue(
                                    NORMAL, result.getResultValue().getUnderlyingType()),
                            in.getRegularStore());
                }
            }
            return result;
        }
    }
}

package org.checkerframework.checker.signature;

import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.signature.qual.CanonicalNameOrEmpty;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The transfer function for the Signature Checker. */
public class SignatureTransfer extends CFTransfer {

    /** The annotated type factory for this transfer function. */
    private SignatureAnnotatedTypeFactory aTypeFactory;

    /**
     * Create a new SignatureTransfer.
     *
     * @param analysis the analysis
     */
    public SignatureTransfer(CFAnalysis analysis) {
        super(analysis);
        aTypeFactory = (SignatureAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> superResult = super.visitMethodInvocation(n, in);

        MethodAccessNode target = n.getTarget();
        ExecutableElement method = target.getMethod();
        Node receiver = target.getReceiver();
        if (TypesUtils.isString(receiver.getType())
                && ElementUtils.matchesElement(method, "isEmpty")) {

            AnnotatedTypeMirror receiverAtm = aTypeFactory.getAnnotatedType(receiver.getTree());
            if (receiverAtm.hasAnnotation(CanonicalNameOrEmpty.class)) {

                CFStore thenStore = superResult.getRegularStore();
                CFStore elseStore = thenStore.copy();
                ConditionalTransferResult<CFValue, CFStore> result =
                        new ConditionalTransferResult<>(
                                superResult.getResultValue(), thenStore, elseStore);
                // The refined expression is the receiver of the method call.
                JavaExpression refinedExpr =
                        JavaExpression.fromNode(
                                aTypeFactory.getChecker().getAnnotationProvider(), receiver);

                elseStore.insertValue(refinedExpr, aTypeFactory.CANONICAL_NAME);
                return result;
            }
        }
        return superResult;
    }
}

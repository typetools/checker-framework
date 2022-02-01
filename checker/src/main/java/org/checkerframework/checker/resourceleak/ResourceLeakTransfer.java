package org.checkerframework.checker.resourceleak;

import org.checkerframework.checker.calledmethods.CalledMethodsTransfer;
import org.checkerframework.checker.mustcall.CreatesMustCallForElementSupplier;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.SwitchExpressionNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TypesUtils;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

/** The transfer function for the resource-leak extension to the called-methods type system. */
public class ResourceLeakTransfer extends CalledMethodsTransfer {

    /**
     * Shadowed because we must dispatch to the Resource Leak Checker's version of
     * getTypefactoryOfSubchecker to get the correct MustCallAnnotatedTypeFactory.
     */
    private final ResourceLeakAnnotatedTypeFactory rlTypeFactory;

    /**
     * Create a new resource leak transfer function.
     *
     * @param analysis the analysis. Its type factory must be a {@link
     *     ResourceLeakAnnotatedTypeFactory}.
     */
    public ResourceLeakTransfer(final ResourceLeakAnalysis analysis) {
        super(analysis);
        this.rlTypeFactory = (ResourceLeakAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    @Override
    public TransferResult<CFValue, CFStore> visitTernaryExpression(
            TernaryExpressionNode node, TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitTernaryExpression(node, input);
        if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
            // Add the synthetic variable created during CFG construction to the temporary
            // variable map (rather than creating a redundant temp var)
            rlTypeFactory.addTempVar(node.getTernaryExpressionVar(), node.getTree());
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSwitchExpressionNode(
            SwitchExpressionNode node, TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitSwitchExpressionNode(node, input);
        if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
            // Add the synthetic variable created during CFG construction to the temporary
            // variable map (rather than creating a redundant temp var)
            rlTypeFactory.addTempVar(node.getSwitchExpressionVar(), node.getTree());
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {

        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);

        handleCreatesMustCallFor(node, result);
        updateStoreWithTempVar(result, node);

        // If there is a temporary variable for the receiver, update its type.
        Node receiver = node.getTarget().getReceiver();
        MustCallAnnotatedTypeFactory mcAtf =
                rlTypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
        Node accumulationTarget = mcAtf.getTempVar(receiver);
        if (accumulationTarget != null) {
            String methodName = node.getTarget().getMethod().getSimpleName().toString();
            methodName =
                    rlTypeFactory.adjustMethodNameUsingValueChecker(methodName, node.getTree());
            accumulate(accumulationTarget, result, methodName);
        }

        return result;
    }

    /**
     * Clears the called-methods store of all information about the target if an @CreatesMustCallFor
     * method is invoked and the type factory can create obligations. Otherwise, does nothing.
     *
     * @param n a method invocation
     * @param result the transfer result whose stores should be cleared of information
     */
    private void handleCreatesMustCallFor(
            MethodInvocationNode n, TransferResult<CFValue, CFStore> result) {
        if (!rlTypeFactory.canCreateObligations()) {
            return;
        }

        List<JavaExpression> targetExprs =
                CreatesMustCallForElementSupplier.getCreatesMustCallForExpressions(
                        n, rlTypeFactory, rlTypeFactory);
        AnnotationMirror defaultType = rlTypeFactory.top;
        for (JavaExpression targetExpr : targetExprs) {
            CFValue defaultTypeValue =
                    analysis.createSingleAnnotationValue(defaultType, targetExpr.getType());
            if (result.containsTwoStores()) {
                result.getThenStore().replaceValue(targetExpr, defaultTypeValue);
                result.getElseStore().replaceValue(targetExpr, defaultTypeValue);
            } else {
                result.getRegularStore().replaceValue(targetExpr, defaultTypeValue);
            }
        }
    }

    @Override
    public TransferResult<CFValue, CFStore> visitObjectCreation(
            ObjectCreationNode node, TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitObjectCreation(node, input);
        updateStoreWithTempVar(result, node);
        return result;
    }

    /**
     * This method either creates or looks up the temp var t for node, and then updates the store to
     * give t the same type as node. Temporary variables are supported for expressions throughout
     * this checker (and the Must Call Checker) to enable refinement of their types. See the
     * documentation of {@link MustCallConsistencyAnalyzer} for more details.
     *
     * @param node the node to be assigned to a temporary variable
     * @param result the transfer result containing the store to be modified
     */
    public void updateStoreWithTempVar(TransferResult<CFValue, CFStore> result, Node node) {
        // Must-call obligations on primitives are not supported.
        if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
            MustCallAnnotatedTypeFactory mcAtf =
                    rlTypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
            LocalVariableNode temp = mcAtf.getTempVar(node);
            if (temp != null) {
                rlTypeFactory.addTempVar(temp, node.getTree());
                JavaExpression localExp = JavaExpression.fromNode(temp);
                AnnotationMirror anm =
                        rlTypeFactory
                                .getAnnotatedType(node.getTree())
                                .getAnnotationInHierarchy(rlTypeFactory.top);
                insertIntoStores(result, localExp, anm == null ? rlTypeFactory.top : anm);
            }
        }
    }
}

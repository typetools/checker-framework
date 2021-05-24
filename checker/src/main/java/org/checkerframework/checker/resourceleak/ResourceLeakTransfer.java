package org.checkerframework.checker.resourceleak;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsTransfer;
import org.checkerframework.checker.mustcall.CreatesObligationElementSupplier;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TypesUtils;

/** The transfer function for the resource-leak extension to the called-methods type system. */
public class ResourceLeakTransfer extends CalledMethodsTransfer {

  /**
   * Shadowed because we MUST dispatch to the RLC's version of getTypefactoryOfSubchecker to get the
   * correct MCATF.
   */
  private final ResourceLeakAnnotatedTypeFactory rlTypeFactory;

  /**
   * Create a new RL transfer function.
   *
   * @param analysis the analysis. Note that the associated type factory must be a Resource Leak
   *     type factory.
   */
  public ResourceLeakTransfer(final CFAnalysis analysis) {
    super(analysis);
    this.rlTypeFactory = (ResourceLeakAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitTernaryExpression(node, input);
    updateStoreWithTempVar(result, node);
    return result;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {

    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);

    handleCreatesObligation(node, result);
    updateStoreWithTempVar(result, node);

    Node receiver = node.getTarget().getReceiver();
    // If there is a temporary variable for the receiver, update its type.
    MustCallAnnotatedTypeFactory mcAtf =
        rlTypeFactory.getTypeFactoryOfSubchecker(MustCallChecker.class);
    Node accumulationTarget = mcAtf.getTempVar(receiver);

    if (accumulationTarget != null) {
      String methodName = node.getTarget().getMethod().getSimpleName().toString();
      methodName = rlTypeFactory.adjustMethodNameUsingValueChecker(methodName, node.getTree());
      accumulate(accumulationTarget, result, methodName);
    }

    return result;
  }

  /**
   * Clears the called-methods store of all information about the target if an @CreatesObligation
   * method is invoked and the type factory can create obligations. Othewise, does nothing.
   *
   * @param n a method invocation
   * @param result the transfer result whose stores should be cleared of information
   */
  private void handleCreatesObligation(
      MethodInvocationNode n, TransferResult<CFValue, CFStore> result) {
    if (!rlTypeFactory.canCreateObligations()) {
      return;
    }

    List<JavaExpression> targetExprs =
        CreatesObligationElementSupplier.getCreatesObligationExpressions(
            n, rlTypeFactory, rlTypeFactory);
    for (JavaExpression targetExpr : targetExprs) {
      AnnotationMirror defaultType = rlTypeFactory.top;
      if (result.containsTwoStores()) {
        CFStore thenStore = result.getThenStore();
        thenStore.clearValue(targetExpr);
        thenStore.insertValue(targetExpr, defaultType);
        CFStore elseStore = result.getElseStore();
        elseStore.clearValue(targetExpr);
        elseStore.insertValue(targetExpr, defaultType);
      } else {
        CFStore store = result.getRegularStore();
        store.clearValue(targetExpr);
        store.insertValue(targetExpr, defaultType);
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
   * give t the same type as node
   *
   * @param node the node to be assigned to a temporal variable
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

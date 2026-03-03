package org.checkerframework.checker.rlccalledmethods;

import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.calledmethods.CalledMethodsTransfer;
import org.checkerframework.checker.mustcall.CreatesMustCallForToJavaExpression;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.SwitchExpressionNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The transfer function for the resource-leak extension to the called-methods type system. */
public class RLCCalledMethodsTransfer extends CalledMethodsTransfer {

  /**
   * Shadowed because we must dispatch to the Resource Leak Checker's version of
   * getTypefactoryOfSubchecker to get the correct MustCallAnnotatedTypeFactory.
   */
  private final RLCCalledMethodsAnnotatedTypeFactory rlTypeFactory;

  /**
   * Create a new resource leak transfer function.
   *
   * @param analysis the analysis. Its type factory must be a {@link
   *     RLCCalledMethodsAnnotatedTypeFactory}.
   */
  public RLCCalledMethodsTransfer(RLCCalledMethodsAnalysis analysis) {
    super(analysis);
    this.rlTypeFactory = (RLCCalledMethodsAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<AccumulationValue, AccumulationStore> input) {
    TransferResult<AccumulationValue, AccumulationStore> result =
        super.visitTernaryExpression(node, input);
    if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
      // Add the synthetic variable created during CFG construction to the temporary
      // variable map (rather than creating a redundant temp var)
      rlTypeFactory.addTempVar(node.getTernaryExpressionVar(), node.getTree());
    }
    return result;
  }

  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitSwitchExpressionNode(
      SwitchExpressionNode node, TransferInput<AccumulationValue, AccumulationStore> input) {
    TransferResult<AccumulationValue, AccumulationStore> result =
        super.visitSwitchExpressionNode(node, input);
    if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
      // Add the synthetic variable created during CFG construction to the temporary
      // variable map (rather than creating a redundant temp var)
      rlTypeFactory.addTempVar(node.getSwitchExpressionVar(), node.getTree());
    }
    return result;
  }

  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<AccumulationValue, AccumulationStore> input) {

    TransferResult<AccumulationValue, AccumulationStore> result =
        super.visitMethodInvocation(node, input);

    handleCreatesMustCallFor(node, result);
    updateStoreWithTempVar(result, node);

    // If there is a temporary variable for the receiver, update its type.
    Node receiver = node.getTarget().getReceiver();
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
   * Clears the called-methods store of all information about the target if an @CreatesMustCallFor
   * method is invoked and the type factory can create obligations. Otherwise, does nothing.
   *
   * @param n a method invocation
   * @param result the transfer result whose stores should be cleared of information
   */
  private void handleCreatesMustCallFor(
      MethodInvocationNode n, TransferResult<AccumulationValue, AccumulationStore> result) {
    if (!rlTypeFactory.canCreateObligations()) {
      return;
    }

    List<JavaExpression> targetExprs =
        CreatesMustCallForToJavaExpression.getCreatesMustCallForExpressionsAtInvocation(
            n, rlTypeFactory, rlTypeFactory);
    AnnotationMirror defaultType = rlTypeFactory.top;
    for (JavaExpression targetExpr : targetExprs) {
      AccumulationValue defaultTypeValue =
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
  public TransferResult<AccumulationValue, AccumulationStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<AccumulationValue, AccumulationStore> input) {
    TransferResult<AccumulationValue, AccumulationStore> result =
        super.visitObjectCreation(node, input);
    updateStoreWithTempVar(result, node);
    return result;
  }

  /**
   * This method either creates or looks up the temp var t for node, and then updates the store to
   * give t the same type as node. Temporary variables are supported for expressions throughout this
   * checker (and the Must Call Checker) to enable refinement of their types. See the documentation
   * of {@link MustCallConsistencyAnalyzer} for more details.
   *
   * @param node the node to be assigned to a temporary variable
   * @param result the transfer result containing the store to be modified
   */
  public void updateStoreWithTempVar(
      TransferResult<AccumulationValue, AccumulationStore> result, Node node) {
    // If the node is a void method invocation then do not create temp vars for it.
    if (node instanceof MethodInvocationNode) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) node.getTree();
      ExecutableElement executableElement = TreeUtils.elementFromUse(methodInvocationTree);
      if (ElementUtils.getType(executableElement).getKind() == TypeKind.VOID) {
        return;
      }
    }
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
                .getPrimaryAnnotationInHierarchy(rlTypeFactory.top);
        if (anm == null) {
          anm = rlTypeFactory.top;
        }
        if (result.containsTwoStores()) {
          result.getThenStore().insertValue(localExp, anm);
          result.getElseStore().insertValue(localExp, anm);
        } else {
          result.getRegularStore().insertValue(localExp, anm);
        }
      }
    }
  }
}

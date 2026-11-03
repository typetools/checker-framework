package org.checkerframework.checker.rlccalledmethods;

import com.sun.source.tree.MethodInvocationTree;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsTransfer;
import org.checkerframework.checker.collectionownership.DisposalLoopInfo;
import org.checkerframework.checker.mustcall.CreatesMustCallForToJavaExpression;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.SwitchExpressionNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.expression.IteratedCollectionElement;
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
  public void accumulate(
      Node node, TransferResult<AccumulationValue, AccumulationStore> result, String... values) {
    super.accumulate(node, result, values);
    updateStoreForIteratedCollectionElement(Arrays.asList(values), result, node);
  }

  /**
   * Returns the initial store, plus an {@link IteratedCollectionElement} entry for the element
   * iterated over by each {@linkplain DisposalLoopInfo potential disposal loop} in the method, such
   * as {@code r} in {@code for (Resource r : list)} or {@code list.get(i)} in an indexed loop.
   *
   * <p>These entries determine which iterated elements this analysis tracks: {@link #accumulate}
   * adds called methods to an iterated element only if the store has an entry for it. After the
   * analysis, {@link MustCallConsistencyAnalyzer#analyzeDisposalLoop} reads the entries to find the
   * methods that a loop body calls on every iterated element.
   *
   * @param underlyingAST the code being analyzed
   * @param parameters the parameters of the method being analyzed
   * @return the initial store
   */
  @Override
  public AccumulationStore initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    AccumulationStore store = super.initialStore(underlyingAST, parameters);
    for (DisposalLoopInfo disposalLoopInfo :
        ResourceLeakUtils.getCollectionOwnershipAnnotatedTypeFactory(rlTypeFactory)
            .getDisposalLoopInfos(underlyingAST)) {
      IteratedCollectionElement collectionElementJE =
          new IteratedCollectionElement(
              disposalLoopInfo.iteratedElementNode(), disposalLoopInfo.iteratedElementTree());
      store.insertValue(collectionElementJE, rlTypeFactory.top);
    }
    return store;
  }

  /**
   * If {@code node} is the element iterated over by a {@linkplain DisposalLoopInfo potential
   * disposal loop}, adds the called methods to its {@link IteratedCollectionElement}.
   *
   * @param valuesAsList the newly called methods
   * @param result the transfer result
   * @param node a cfg node
   */
  private void updateStoreForIteratedCollectionElement(
      List<String> valuesAsList,
      TransferResult<AccumulationValue, AccumulationStore> result,
      Node node) {
    IteratedCollectionElement collectionElement =
        result.getRegularStore().getIteratedCollectionElement(node, node.getTree());
    if (collectionElement == null) {
      return;
    }
    if (result.containsTwoStores()) {
      updateValueAndInsertIntoStore(result.getThenStore(), collectionElement, valuesAsList);
      updateValueAndInsertIntoStore(result.getElseStore(), collectionElement, valuesAsList);
    } else {
      updateValueAndInsertIntoStore(result.getRegularStore(), collectionElement, valuesAsList);
    }
    accumulateInExceptionalStores(collectionElement, result, valuesAsList);
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
   * Clears the called-methods stores, including the exceptional stores, of all information about
   * the target if an @CreatesMustCallFor method is invoked and the type factory can create
   * obligations. Otherwise, does nothing.
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
      // The invoked method might have created the obligation before throwing an exception.
      Map<TypeMirror, AccumulationStore> exceptionalStores = result.getExceptionalStores();
      if (exceptionalStores != null) {
        for (AccumulationStore exceptionalStore : exceptionalStores.values()) {
          exceptionalStore.replaceValue(targetExpr, defaultTypeValue);
        }
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
        insertIntoStores(result, localExp, anm);
      }
    }
  }
}

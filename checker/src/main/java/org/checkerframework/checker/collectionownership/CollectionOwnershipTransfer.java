package org.checkerframework.checker.collectionownership;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Transfer function for the collection ownership type system. Its primary purpose is to create
 * temporary variables for expressions (which allow those expressions to have refined information in
 * the store, which the consistency checker can use).
 */
public class CollectionOwnershipTransfer extends CFTransfer {

  /** The type factory. */
  private final CollectionOwnershipAnnotatedTypeFactory atypeFactory;

  /** The MustCall type factory to manage temp vars. */
  private final MustCallAnnotatedTypeFactory mcAtf;

  /**
   * Create a CollectionOwnershipTransfer.
   *
   * @param analysis the analysis
   */
  public CollectionOwnershipTransfer(CFAnalysis analysis) {
    super(analysis);
    atypeFactory = (CollectionOwnershipAnnotatedTypeFactory) analysis.getTypeFactory();
    mcAtf = ResourceLeakUtils.getMustCallAnnotatedTypeFactory(atypeFactory);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

    updateStoreWithTempVar(result, n);
    // if (!noCreatesMustCallFor) {
    //   List<JavaExpression> targetExprs =
    //       CreatesMustCallForToJavaExpression.getCreatesMustCallForExpressionsAtInvocation(
    //           n, atypeFactory, atypeFactory);
    //   for (JavaExpression targetExpr : targetExprs) {
    //     AnnotationMirror defaultType =
    //         atypeFactory
    //             .getAnnotatedType(TypesUtils.getTypeElement(targetExpr.getType()))
    //             .getPrimaryAnnotationInHierarchy(atypeFactory.TOP);

    //     if (result.containsTwoStores()) {
    //       CFStore thenStore = result.getThenStore();
    //       lubWithStoreValue(thenStore, targetExpr, defaultType);

    //       CFStore elseStore = result.getElseStore();
    //       lubWithStoreValue(elseStore, targetExpr, defaultType);
    //     } else {
    //       CFStore store = result.getRegularStore();
    //       lubWithStoreValue(store, targetExpr, defaultType);
    //     }
    //   }
    // }
    return result;
  }

  // /**
  //  * Computes the LUB of the current value in the store for expr, if it exists, and defaultType.
  //  * Inserts that LUB into the store as the new value for expr.
  //  *
  //  * @param store a CFStore
  //  * @param expr an expression that might be in the store
  //  * @param defaultType the default type of the expression's static type
  //  */
  // private void lubWithStoreValue(CFStore store, JavaExpression expr, AnnotationMirror
  // defaultType) {
  //   CFValue value = store.getValue(expr);
  //   CFValue defaultTypeAsCFValue =
  //       analysis.createSingleAnnotationValue(defaultType, expr.getType());
  //   CFValue newValue = defaultTypeAsCFValue.leastUpperBound(value);
  //   store.replaceValue(expr, newValue);
  // }

  @Override
  public TransferResult<CFValue, CFStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> result = super.visitObjectCreation(node, input);
    updateStoreWithTempVar(result, node);
    return result;
  }

  // TODO sck: I think that I can use the temp var management from MC checker and don't
  // need to replicate that in the COatf. If that turns out to not work, uncomment this
  // @Override
  // public TransferResult<CFValue, CFStore> visitSwitchExpressionNode(
  //     SwitchExpressionNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> result = super.visitSwitchExpressionNode(node, input);
  //   if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
  //     // Add the synthetic variable created during CFG construction to the temporary
  //     // variable map (rather than creating a redundant temp var)
  //     atypeFactory.tempVars.put(node.getTree(), node.getSwitchExpressionVar());
  //   }
  //   return result;
  // }

  /**
   * This method either creates or looks up the temp var t for node, and then updates the store to
   * give t the same type as {@code node}.
   *
   * @param node the node to be assigned to a temporary variable
   * @param result the transfer result containing the store to be modified
   */
  public void updateStoreWithTempVar(TransferResult<CFValue, CFStore> result, Node node) {
    // Must-call obligations on primitives are not supported.
    if (!TypesUtils.isPrimitiveOrBoxed(node.getType())) {
      LocalVariableNode temp = mcAtf.getTempVar(node);
      if (temp != null) {
        // atypeFactory.addTempVar(temp, node.getTree());
        JavaExpression localExp = JavaExpression.fromNode(temp);
        AnnotationMirror anm =
            atypeFactory
                .getAnnotatedType(node.getTree())
                .getPrimaryAnnotationInHierarchy(atypeFactory.TOP);
        insertIntoStores(result, localExp, anm == null ? atypeFactory.TOP : anm);
        // if (anm == null) {
        //   anm = atypeFactory.TOP;
        // }
        // if (result.containsTwoStores()) {
        //   result.getThenStore().insertValue(localExp, anm);
        //   result.getElseStore().insertValue(localExp, anm);
        // } else {
        //   result.getRegularStore().insertValue(localExp, anm);
        // }
      }
    }
  }
}

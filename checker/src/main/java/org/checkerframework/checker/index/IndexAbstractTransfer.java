package org.checkerframework.checker.index;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/**
 * This class provides methods shared by the Index Checker's internal checkers in their transfer
 * functions. In particular, it provides a common framework for visiting comparison operators.
 */
@SuppressWarnings("ArgumentSelectionDefectChecker") // TODO: apply suggested error-prone fixes
public abstract class IndexAbstractTransfer extends CFTransfer {

  protected IndexAbstractTransfer(CFAnalysis analysis) {
    super(analysis);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitGreaterThan(
      GreaterThanNode node, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitGreaterThan(node, in);

    IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);
    if (rfi.leftAnno == null || rfi.rightAnno == null) {
      return result;
    }
    // Refine the then branch.
    refineGT(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.thenStore, in);

    // Refine the else branch, which is the inverse of the then branch.
    refineGTE(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, rfi.elseStore, in);

    return rfi.newResult;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
      GreaterThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitGreaterThanOrEqual(node, in);

    IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);
    if (rfi.leftAnno == null || rfi.rightAnno == null) {
      return result;
    }

    // Refine the then branch.
    refineGTE(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.thenStore, in);

    // Refine the else branch.
    refineGT(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, rfi.elseStore, in);

    return rfi.newResult;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
      LessThanOrEqualNode node, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitLessThanOrEqual(node, in);

    IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);
    if (rfi.leftAnno == null || rfi.rightAnno == null) {
      return result;
    }

    // Refine the then branch. A <= is just a flipped >=.
    refineGTE(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, rfi.thenStore, in);

    // Refine the else branch.
    refineGT(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.elseStore, in);
    return rfi.newResult;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitLessThan(node, in);

    IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, node);
    if (rfi.leftAnno == null || rfi.rightAnno == null) {
      return result;
    }

    // Refine the then branch. A < is just a flipped >.
    refineGT(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, rfi.thenStore, in);

    // Refine the else branch.
    refineGTE(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.elseStore, in);
    return rfi.newResult;
  }

  /**
   * Inserts the given annotation into the store for each expression that has the value of the given
   * node when the comparison that contains the node completes (see {@link #nodesWithValueOf}).
   *
   * @param store the store to modify
   * @param node a node in an operand of a comparison
   * @param anno the annotation to insert for each expression that has the value of {@code node}
   */
  protected void insertValueForComparedNode(CFStore store, Node node, AnnotationMirror anno) {
    for (Node internal : nodesWithValueOf(node)) {
      store.insertValue(JavaExpression.fromNode(internal), anno);
    }
  }

  /**
   * Refines the given store, assuming that the value of {@code left} is greater than the value of
   * {@code right}.
   *
   * @param left the larger operand of a comparison
   * @param leftAnno the annotation of {@code left}
   * @param right the smaller operand of the comparison
   * @param rightAnno the annotation of {@code right}
   * @param store the store to refine
   * @param in the input to the transfer function for the comparison
   */
  protected abstract void refineGT(
      Node left,
      AnnotationMirror leftAnno,
      Node right,
      AnnotationMirror rightAnno,
      CFStore store,
      TransferInput<CFValue, CFStore> in);

  /**
   * Refines the given store, assuming that the value of {@code left} is greater than or equal to
   * the value of {@code right}.
   *
   * @param left the larger-or-equal operand of a comparison
   * @param leftAnno the annotation of {@code left}
   * @param right the smaller-or-equal operand of the comparison
   * @param rightAnno the annotation of {@code right}
   * @param store the store to refine
   * @param in the input to the transfer function for the comparison
   */
  protected abstract void refineGTE(
      Node left,
      AnnotationMirror leftAnno,
      Node right,
      AnnotationMirror rightAnno,
      CFStore store,
      TransferInput<CFValue, CFStore> in);
}

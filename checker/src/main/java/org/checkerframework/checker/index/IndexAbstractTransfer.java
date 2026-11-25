package org.checkerframework.checker.index;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TypesUtils;

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

    // Refine the else branch, which is the inverse of the then branch, unless an operand might be
    // NaN.
    if (!isFloatingPointComparison(rfi)) {
      refineGTE(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, rfi.elseStore, in);
    }

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

    // Refine the else branch, unless an operand might be NaN.
    if (!isFloatingPointComparison(rfi)) {
      refineGT(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, rfi.elseStore, in);
    }

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

    // Refine the else branch, unless an operand might be NaN.
    if (!isFloatingPointComparison(rfi)) {
      refineGT(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.elseStore, in);
    }
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

    // Refine the else branch, unless an operand might be NaN.
    if (!isFloatingPointComparison(rfi)) {
      refineGTE(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, rfi.elseStore, in);
    }
    return rfi.newResult;
  }

  /**
   * Returns true if the given comparison is a floating-point comparison: that is, if an operand has
   * a floating-point type, primitive or boxed. An operand of such a comparison might be NaN. Every
   * comparison with NaN is false, so the else branch of such a comparison does not imply the
   * inverse comparison: if {@code d} is NaN, then both {@code i > d} and {@code i <= d} are false.
   *
   * @param rfi refinement information for a numerical comparison
   * @return true if the comparison is a floating-point comparison
   */
  private static boolean isFloatingPointComparison(IndexRefinementInfo rfi) {
    return TypesUtils.isFloatingPoint(rfi.left.getType())
        || TypesUtils.isFloatingPoint(rfi.right.getType());
  }

  /**
   * Refines the given store, given that {@code left > right} holds in it.
   *
   * @param left the greater operand of a comparison
   * @param leftAnno the annotation on the type of {@code left}
   * @param right the lesser operand of the comparison
   * @param rightAnno the annotation on the type of {@code right}
   * @param store the store to refine, in which {@code left > right} holds
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
   * Refines the given store, given that {@code left >= right} holds in it.
   *
   * @param left the greater-or-equal operand of a comparison
   * @param leftAnno the annotation on the type of {@code left}
   * @param right the lesser-or-equal operand of the comparison
   * @param rightAnno the annotation on the type of {@code right}
   * @param store the store to refine, in which {@code left >= right} holds
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

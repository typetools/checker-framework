package org.checkerframework.checker.index;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TypeSystemError;

/**
 * This struct contains all of the information that the refinement functions need. It's called by
 * each node function (i.e. greater than node, less than node, etc.) and then the results are passed
 * to the refinement function in whatever order is appropriate for that node. Its constructor
 * contains all of its logic.
 */
public class IndexRefinementInfo {

  /** The left operand. */
  public final Node left;

  /** The right operand. */
  public final Node right;

  /**
   * Annotation for left expressions. Might be null if dataflow doesn't have a value for the
   * expression.
   */
  public final @Nullable AnnotationMirror leftAnno;

  /**
   * Annotation for right expressions. Might be null if dataflow doesn't have a value for the
   * expression.
   */
  public final @Nullable AnnotationMirror rightAnno;

  /** The then store. */
  public final CFStore thenStore;

  /** The else store. */
  public final CFStore elseStore;

  /** The new result, after refinement. */
  public final ConditionalTransferResult<CFValue, CFStore> newResult;

  /**
   * Creates a new IndexRefinementInfo.
   *
   * @param left the left operand
   * @param right the right operand
   * @param result the new result, after refinement
   * @param analysis the CFAbstractAnalysis
   */
  public IndexRefinementInfo(
      TransferResult<CFValue, CFStore> result,
      CFAbstractAnalysis<?, ?, ?> analysis,
      Node right,
      Node left) {
    this.right = right;
    this.left = left;

    thenStore = result.getThenStore();
    elseStore = result.getElseStore();

    if (analysis.getValue(right) == null || analysis.getValue(left) == null) {
      leftAnno = null;
      rightAnno = null;
      newResult = new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
    } else {
      QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
      rightAnno = getAnno(analysis.getValue(right).getAnnotations(), hierarchy);
      leftAnno = getAnno(analysis.getValue(left).getAnnotations(), hierarchy);
      newResult = new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
    }
  }

  public IndexRefinementInfo(
      TransferResult<CFValue, CFStore> result,
      CFAbstractAnalysis<?, ?, ?> analysis,
      BinaryOperationNode node) {
    this(result, analysis, node.getRightOperand(), node.getLeftOperand());
  }

  /**
   * Returns the annotation (from the given set) in the given hierarchy.
   *
   * @param set a set of annotations
   * @param hierarchy a qualifier hierarchy
   * @return the annotation (from {@code set}) in the given hierarchy
   */
  private static AnnotationMirror getAnno(AnnotationMirrorSet set, QualifierHierarchy hierarchy) {
    AnnotationMirrorSet tops = hierarchy.getTopAnnotations();
    if (tops.size() != 1) {
      throw new TypeSystemError(
          "%s: Found %d tops, but expected one.%nFound: %s",
          IndexRefinementInfo.class, tops.size(), tops);
    }
    return hierarchy.findAnnotationInHierarchy(set, tops.iterator().next());
  }
}

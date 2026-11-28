package org.checkerframework.checker.confidential;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

/** The transfer class for the Confidential Checker. */
public class ConfidentialTransfer extends CFTransfer {

  /** The Confidential type factory. */
  protected final ConfidentialAnnotatedTypeFactory atypeFactory;

  /** The Confidential qualifier hierarchy. */
  protected final QualifierHierarchy qualHierarchy;

  /**
   * Create a new ConfidentialTransfer.
   *
   * @param analysis the corresponding analysis
   */
  public ConfidentialTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
    atypeFactory = (ConfidentialAnnotatedTypeFactory) analysis.getTypeFactory();
    qualHierarchy = atypeFactory.getQualifierHierarchy();
  }

  /**
   * Enforces Confidential String concatenation rules:
   *
   * <ul>
   *   <li>(Confidential + NonConfidential) returns Confidential (commutatively);
   *   <li>(Confidential + Confidential) returns Confidential;
   *   <li>(NonConfidential + NonConfidential) returns NonConfidential;
   *   <li>UnknownConfidential dominates other types in concatenation;
   *   <li>Non-bottom types dominate BottomConfidential in concatenation.
   * </ul>
   */
  @Override
  public TransferResult<CFValue, CFStore> visitStringConcatenate(
      StringConcatenateNode n, TransferInput<CFValue, CFStore> p) {
    TransferResult<CFValue, CFStore> result = super.visitStringConcatenate(n, p);
    return stringConcatenation(n.getLeftOperand(), n.getRightOperand(), p, result);
  }

  /**
   * Determines the type of a string concatenation.
   *
   * @param leftOperand the left operand to be concatenated
   * @param rightOperand the right operand to be concatenated
   * @param p the input abstract values
   * @param result the result abstract values
   * @return the resulting type of the string concatenation operation
   */
  public TransferResult<CFValue, CFStore> stringConcatenation(
      Node leftOperand,
      Node rightOperand,
      TransferInput<CFValue, CFStore> p,
      TransferResult<CFValue, CFStore> result) {
    AnnotationMirror resultAnno =
        createAnnotationForStringConcatenation(leftOperand, rightOperand, p);
    if (resultAnno == null) {
      // The Confidential qualifier of an operand is unknown, so leave the result of the
      // superclass's transfer function unchanged.
      return result;
    }
    return recreateTransferResult(resultAnno, result);
  }

  /**
   * Creates an annotation for a result of string concatenation.
   *
   * @param leftOperand the left operand to be concatenated
   * @param rightOperand the right operand to be concatenated
   * @param p the input abstract values
   * @return the resulting AnnotationMirror of the string concatenation operation, or null if either
   *     operand has no abstract value or has no annotation in the Confidential hierarchy
   */
  private @Nullable AnnotationMirror createAnnotationForStringConcatenation(
      Node leftOperand, Node rightOperand, TransferInput<CFValue, CFStore> p) {
    AnnotationMirror leftAnno = getValueAnnotation(p.getValueOfSubNode(leftOperand));
    if (leftAnno == null) {
      return null;
    }
    AnnotationMirror rightAnno = getValueAnnotation(p.getValueOfSubNode(rightOperand));
    if (rightAnno == null) {
      return null;
    }

    if (AnnotationUtils.areSame(leftAnno, atypeFactory.CONFIDENTIAL)
        || AnnotationUtils.areSame(rightAnno, atypeFactory.CONFIDENTIAL)) {
      return atypeFactory.CONFIDENTIAL;
    }

    return qualHierarchy.leastUpperBoundShallow(
        leftAnno, leftOperand.getType(), rightAnno, rightOperand.getType());
  }

  /**
   * Returns the annotation in the Confidential type hierarchy for the given value.
   *
   * @param cfValue the value, or null if the value is unknown
   * @return the value's AnnotationMirror from the Confidential hierarchy, or null if {@code
   *     cfValue} is null or has no annotation in the Confidential hierarchy
   */
  private @Nullable AnnotationMirror getValueAnnotation(@Nullable CFValue cfValue) {
    if (cfValue == null) {
      return null;
    }
    return qualHierarchy.findAnnotationInHierarchy(
        cfValue.getAnnotations(), atypeFactory.UNKNOWN_CONFIDENTIAL);
  }
}

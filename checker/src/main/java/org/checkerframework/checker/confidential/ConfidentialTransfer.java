package org.checkerframework.checker.confidential;

import javax.lang.model.element.AnnotationMirror;
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

  public TransferResult<CFValue, CFStore> stringConcatenation(
      Node leftOperand,
      Node rightOperand,
      TransferInput<CFValue, CFStore> p,
      TransferResult<CFValue, CFStore> result) {
    AnnotationMirror resultAnno =
        createAnnotationForStringConcatenation(leftOperand, rightOperand, p);
    return recreateTransferResult(resultAnno, result);
  }

  /** Creates an annotation for a result of string concatenation. */
  private AnnotationMirror createAnnotationForStringConcatenation(
      Node leftOperand, Node rightOperand, TransferInput<CFValue, CFStore> p) {
    CFValue leftValue = p.getValueOfSubNode(leftOperand);
    AnnotationMirror leftAnno = getValueAnnotation(leftValue);
    if (leftAnno == null) {
      return null;
    }
    String leftAnnoName = AnnotationUtils.annotationName(leftAnno);
    CFValue rightValue = p.getValueOfSubNode(rightOperand);
    AnnotationMirror rightAnno = getValueAnnotation(rightValue);
    if (rightAnno == null) {
      return null;
    }
    String rightAnnoName = AnnotationUtils.annotationName(rightAnno);

    if (leftAnnoName.equals(ConfidentialAnnotatedTypeFactory.UNKNOWN_CONFIDENTIAL_NAME)
        || rightAnnoName.equals(ConfidentialAnnotatedTypeFactory.UNKNOWN_CONFIDENTIAL_NAME)) {
      return atypeFactory.UNKNOWN_CONFIDENTIAL;
    }

    if (leftAnnoName.equals(ConfidentialAnnotatedTypeFactory.BOTTOM_CONFIDENTIAL_NAME)) {
      return rightAnno;
    } else if (rightAnnoName.equals(ConfidentialAnnotatedTypeFactory.BOTTOM_CONFIDENTIAL_NAME)) {
      return leftAnno;
    }

    if (leftAnnoName.equals(ConfidentialAnnotatedTypeFactory.CONFIDENTIAL_NAME)
        || rightAnnoName.equals(ConfidentialAnnotatedTypeFactory.CONFIDENTIAL_NAME)) {
      return atypeFactory.CONFIDENTIAL;
    }

    return atypeFactory.NONCONFIDENTIAL;
  }

  private AnnotationMirror getValueAnnotation(CFValue cfValue) {
    return qualHierarchy.findAnnotationInHierarchy(
        cfValue.getAnnotations(), atypeFactory.UNKNOWN_CONFIDENTIAL);
  }
}

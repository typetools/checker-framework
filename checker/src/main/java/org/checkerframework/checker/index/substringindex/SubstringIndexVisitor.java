package org.checkerframework.checker.index.substringindex;

import com.sun.source.tree.ExpressionTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.index.IndexChecker;
import org.checkerframework.checker.index.qual.SubstringIndexFor;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.checker.index.upperbound.UBQualifier;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/** The visitor for the Substring Index Checker. */
public class SubstringIndexVisitor extends BaseTypeVisitor<SubstringIndexAnnotatedTypeFactory> {

  /**
   * Creates a SubstringIndexVisitor.
   *
   * @param checker the checker that created this visitor
   */
  public SubstringIndexVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Also permits the reassignment if, for each sequence {@code b} in the primary annotation of
   * {@code requiredType}, the primary annotation of {@code currentType} has the same offsets for
   * {@code b} or for a sequence that has the same length as {@code b} before the reassignment. For
   * example, if {@code i} is {@code @SubstringIndexFor(value = "a", offset = "-1")} and {@code b}
   * is {@code @SameLen("a")}, then after {@code a = b}, {@code i} still has that type. This
   * relaxation applies only to the primary annotation; the rest of the type must satisfy the
   * default check.
   */
  @Override
  protected boolean isValidAfterReassignment(
      VariableElement variable,
      AnnotatedTypeMirror currentType,
      AnnotatedTypeMirror requiredType,
      ExpressionTree reassignmentTree) {
    if (super.isValidAfterReassignment(variable, currentType, requiredType, reassignmentTree)) {
      return true;
    }
    AnnotationMirror currentAnno =
        currentType.getPrimaryAnnotationInHierarchy(atypeFactory.UNKNOWN);
    AnnotationMirror requiredAnno =
        requiredType.getPrimaryAnnotationInHierarchy(atypeFactory.UNKNOWN);
    if (currentAnno == null
        || requiredAnno == null
        || !atypeFactory.areSameByClass(currentAnno, SubstringIndexFor.class)
        || !atypeFactory.areSameByClass(requiredAnno, SubstringIndexFor.class)) {
      return false;
    }
    IndexChecker indexChecker = (IndexChecker) checker.getUltimateParentChecker();
    UBQualifier currentQual = UBQualifier.createUBQualifier(currentAnno, indexChecker);
    UBQualifier requiredQual = UBQualifier.createUBQualifier(requiredAnno, indexChecker);
    if (!(currentQual instanceof LessThanLengthOf currentLtl)
        || !(requiredQual instanceof LessThanLengthOf requiredLtl)) {
      return false;
    }
    SameLenAnnotatedTypeFactory sameLenFactory =
        atypeFactory.getTypeFactoryOfSubchecker(SameLenChecker.class);
    if (!currentLtl.impliesUpToSameLen(
        requiredLtl,
        sequence ->
            sameLenFactory.getSameLensFromString(sequence, reassignmentTree, getCurrentPath()))) {
      return false;
    }
    // The primary annotation is valid.  Check the rest of the type without relaxation.
    AnnotatedTypeMirror relaxedCurrentType = currentType.deepCopy();
    relaxedCurrentType.replaceAnnotation(requiredAnno);
    return super.isValidAfterReassignment(
        variable, relaxedCurrentType, requiredType, reassignmentTree);
  }
}

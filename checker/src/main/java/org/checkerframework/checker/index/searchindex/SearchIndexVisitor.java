package org.checkerframework.checker.index.searchindex;

import com.sun.source.tree.ExpressionTree;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.index.qual.NegativeIndexFor;
import org.checkerframework.checker.index.qual.SearchIndexFor;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.samelen.SameLenChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/** The visitor for the Search Index Checker. */
public class SearchIndexVisitor extends BaseTypeVisitor<SearchIndexAnnotatedTypeFactory> {

  /**
   * Creates a SearchIndexVisitor.
   *
   * @param checker the checker that created this visitor
   */
  public SearchIndexVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Also permits the reassignment if each sequence {@code b} in the primary annotation of {@code
   * requiredType} is in the primary annotation of {@code currentType} or has the same length as one
   * of its sequences before the reassignment. For example, if {@code i} is
   * {@code @SearchIndexFor("a")} and {@code b} is {@code @SameLen("a")}, then after {@code a = b},
   * {@code i} is still {@code @SearchIndexFor("a")}. This relaxation applies only to the primary
   * annotation; the rest of the type must satisfy the default check.
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
    if (currentAnno == null || requiredAnno == null) {
      return false;
    }
    boolean currentIsNegative = atypeFactory.areSameByClass(currentAnno, NegativeIndexFor.class);
    boolean requiredIsNegative = atypeFactory.areSameByClass(requiredAnno, NegativeIndexFor.class);
    // A @NegativeIndexFor is also a @SearchIndexFor, but not vice versa.
    if (!(currentIsNegative || atypeFactory.areSameByClass(currentAnno, SearchIndexFor.class))
        || !(requiredIsNegative || atypeFactory.areSameByClass(requiredAnno, SearchIndexFor.class))
        || (requiredIsNegative && !currentIsNegative)) {
      return false;
    }
    SameLenAnnotatedTypeFactory sameLenFactory =
        atypeFactory.getTypeFactoryOfSubchecker(SameLenChecker.class);
    List<String> currentSequences = atypeFactory.getValueElement(currentAnno);
    for (String required : atypeFactory.getValueElement(requiredAnno)) {
      if (currentSequences.contains(required)) {
        continue;
      }
      List<String> sameLens =
          sameLenFactory.getSameLensFromString(required, reassignmentTree, getCurrentPath());
      if (Collections.disjoint(sameLens, currentSequences)) {
        return false;
      }
    }
    // The primary annotation is valid.  Check the rest of the type without relaxation.
    AnnotatedTypeMirror relaxedCurrentType = currentType.deepCopy();
    relaxedCurrentType.replaceAnnotation(requiredAnno);
    return super.isValidAfterReassignment(
        variable, relaxedCurrentType, requiredType, reassignmentTree);
  }
}

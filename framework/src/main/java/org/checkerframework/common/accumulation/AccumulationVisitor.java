package org.checkerframework.common.accumulation;

import com.sun.source.tree.AnnotationTree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for an accumulation checker. Issues predicate errors if the user writes an invalid
 * predicate.
 */
public class AccumulationVisitor extends BaseTypeVisitor<AccumulationAnnotatedTypeFactory> {

  /**
   * Constructor matching super.
   *
   * @param checker the checker
   */
  public AccumulationVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /** Checks each predicate annotation to make sure the predicate is well-formed. */
  @Override
  public Void visitAnnotation(final AnnotationTree node, final Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(node);
    if (atypeFactory.isPredicate(anno)) {
      String errorMessage = atypeFactory.isValidPredicate(anno);
      if (errorMessage != null) {
        checker.reportError(node, "predicate", errorMessage);
      }
    }
    return super.visitAnnotation(node, p);
  }
}

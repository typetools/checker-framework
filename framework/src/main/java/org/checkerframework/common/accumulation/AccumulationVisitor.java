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
   * Creates an AccumulationVisitor.
   *
   * @param checker the checker
   */
  public AccumulationVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /** Checks each predicate annotation to make sure the predicate is well-formed. */
  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(tree);
    if (atypeFactory.isPredicate(anno)) {
      String errorMessage = atypeFactory.isValidPredicate(anno);
      if (errorMessage != null) {
        checker.reportError(tree, "predicate", errorMessage);
      }
    }
    return super.visitAnnotation(tree, p);
  }
}

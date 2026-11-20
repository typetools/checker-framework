package org.checkerframework.checker.nullness;

import com.sun.source.tree.ClassTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * The visitor for the Path Shape Checker. It skips classes that do not use paths.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
public class PathShapeVisitor extends BaseTypeVisitor<PathShapeAnnotatedTypeFactory> {

  /**
   * Creates a new PathShapeVisitor.
   *
   * @param checker the checker that uses this visitor
   */
  public PathShapeVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Returns true if the given class is a top-level class that does not use paths. In such a class,
   * the Path Shape Checker never issues a warning and never refines a type, so checking the class
   * would only waste time. A nested class is checked if its top-level class is.
   *
   * @param tree a class
   * @return true if the Path Shape Checker should skip {@code tree}
   */
  @Override
  protected boolean shouldSkipClass(ClassTree tree) {
    return root.getTypeDecls().contains(tree) && !atypeFactory.usesPaths(tree);
  }
}

package org.checkerframework.checker.index.growonly;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * The visitor for the Grow-only Checker.
 *
 * <p>Issues an error if a method that may shrink a collection (like a method annotated as
 * {@code @Shrinkable}) is called on a reference that is annotated as {@code @GrowOnly}.
 */
public class GrowOnlyVisitor extends BaseTypeVisitor<GrowOnlyAnnotatedTypeFactory> {

  /**
   * Creates a new GrowOnlyVisitor.
   *
   * @param checker the checker that created this visitor
   */
  public GrowOnlyVisitor(BaseTypeChecker checker) {
    super(checker);
  }
}

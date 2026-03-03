package org.checkerframework.checker.sqlquotes;

import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/** Visitor for the {@link SqlQuotesChecker}. */
public class SqlQuotesVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /**
   * Creates a {@link SqlQuotesVisitor}.
   *
   * @param checker the checker that uses this visitor
   */
  public SqlQuotesVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * This override does not check that the constructor result is top. Checking that the super() or
   * this() call is a subtype of the constructor result is sufficient.
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}
}

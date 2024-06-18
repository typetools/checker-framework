package org.checkerframework.checker.sqltainting;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.sqltainting.SqlTaintingAnnotatedTypeFactory;
import org.checkerframework.checker.sqltainting.qual.SqlUntainted;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** Annotated type factory for the SQL Tainting Checker. */
public class SqlTaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link SqlUntainted} annotation mirror. */
  private final AnnotationMirror SQLUNTAINTED;

  /** A singleton set containing the {@code @}{@link SqlUntainted} annotation mirror. */
  private final AnnotationMirrorSet setOfSqlUntainted;

  /**
   * Creates a {@link SqlTaintingAnnotatedTypeFactory}.
   *
   * @param checker the tainting checker
   */
  public SqlTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.SQLUNTAINTED = AnnotationBuilder.fromClass(getElementUtils(), SqlUntainted.class);
    this.setOfSqlUntainted = AnnotationMirrorSet.singleton(SQLUNTAINTED);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfSqlUntainted;
  }
}

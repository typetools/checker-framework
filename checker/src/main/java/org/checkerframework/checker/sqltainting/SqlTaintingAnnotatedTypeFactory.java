package org.checkerframework.checker.sqltainting;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.sqltainting.qual.SqlSafe;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** Annotated type factory for the SQL Tainting Checker. */
public class SqlTaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link SqlSafe} annotation mirror. */
  private final AnnotationMirror SQLSAFE;

  /** A singleton set containing the {@code @}{@link SqlSafe} annotation mirror. */
  private final AnnotationMirrorSet setOfSqlSafe;

  /**
   * Creates a {@link SqlTaintingAnnotatedTypeFactory}.
   *
   * @param checker the tainting checker
   */
  public SqlTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.SQLSAFE = AnnotationBuilder.fromClass(getElementUtils(), SqlSafe.class);
    this.setOfSqlSafe = AnnotationMirrorSet.singleton(SQLSAFE);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfSqlSafe;
  }
}

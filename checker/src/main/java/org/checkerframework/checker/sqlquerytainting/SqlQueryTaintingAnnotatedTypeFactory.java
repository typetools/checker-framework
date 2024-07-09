package org.checkerframework.checker.sqlquerytainting;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** Annotated type factory for the SQL Query Tainting Checker. */
public class SqlQueryTaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link SqlSanitized} annotation mirror. */
  private final AnnotationMirror SQLSANITIZED;

  /** A singleton set containing the {@code @}{@link SqlSanitized} annotation mirror. */
  private final AnnotationMirrorSet setOfSqlSanitized;

  /**
   * Creates a {@link SqlQueryTaintingAnnotatedTypeFactory}.
   *
   * @param checker the SQL tainting checker
   */
  public SqlQueryTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.SQLSANITIZED = AnnotationBuilder.fromClass(getElementUtils(), SqlSanitized.class);
    this.setOfSqlSanitized = AnnotationMirrorSet.singleton(SQLSANITIZED);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfSqlSanitized;
  }
}

package org.checkerframework.checker.sqlquerytainting;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.sqlquerytainting.qual.SqlSanitizedUser;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** Annotated type factory for the SQL Query Tainting Checker. */
public class SqlQueryTaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link SqlSanitizedUser} annotation mirror. */
  private final AnnotationMirror SQLSANITIZEDUSER;

  /** A singleton set containing the {@code @}{@link SqlSanitizedUser} annotation mirror. */
  private final AnnotationMirrorSet setOfSqlSanitizedUser;

  /**
   * Creates a {@link SqlQueryTaintingAnnotatedTypeFactory}.
   *
   * @param checker the SQL tainting checker
   */
  public SqlQueryTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.SQLSANITIZEDUSER = AnnotationBuilder.fromClass(getElementUtils(), SqlSanitizedUser.class);
    this.setOfSqlSanitizedUser = AnnotationMirrorSet.singleton(SQLSANITIZEDUSER);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfSqlSanitizedUser;
  }
}

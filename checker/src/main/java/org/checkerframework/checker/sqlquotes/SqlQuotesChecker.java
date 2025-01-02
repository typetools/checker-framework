package org.checkerframework.checker.sqlquotes;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker plug-in for the SQL Quotes type system. It finds (and verifies the absence of)
 * some SQL injection bugs.
 *
 * <p>It verifies that each string used as a SQL query has matching open and close quotes.
 */
@StubFiles({"BCryptPasswordEncoder.astub"})
@RelevantJavaTypes(CharSequence.class)
public class SqlQuotesChecker extends BaseTypeChecker {
  /** Creates a SqlQuotesChecker. */
  public SqlQuotesChecker() {}
}

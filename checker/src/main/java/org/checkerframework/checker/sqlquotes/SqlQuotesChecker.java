package org.checkerframework.checker.sqlquotes;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker plug-in for the SQL Quotes type system. It finds (and verifies the absence of) SQL
 * injection bugs.
 *
 * <p>It verifies that only SQL-safe embedded query values are trusted and that user input is
 * sanitized before use.
 */
@StubFiles({"BCryptPasswordEncoder.astub", "Statement.astub"})
public class SqlQuotesChecker extends BaseTypeChecker {
  /** Creates a SqlQuotesChecker. */
  public SqlQuotesChecker() {}
}

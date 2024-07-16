package org.checkerframework.checker.sqlquerytainting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker plug-in for the SQL Tainting type system qualifier that finds (and verifies the
 * absence of) SQL injection bugs in embedded query values.
 *
 * <p>It verifies that only SQL-safe embedded query values are trusted and that user input is
 * sanitized before use.
 */
@SuppressWarningsPrefix({"sqlquerysanitized", "sqlquerytainting"})
@StubFiles({"BCryptPasswordEncoder.astub", "Statement.astub"})
public class SqlQueryTaintingChecker extends BaseTypeChecker {}

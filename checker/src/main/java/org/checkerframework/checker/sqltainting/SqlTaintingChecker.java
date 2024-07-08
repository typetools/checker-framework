package org.checkerframework.checker.sqltainting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker plug-in for the SQL Tainting type system qualifier that prevents SQL injection
 * attacks.
 *
 * <p>It verifies that only verified values are trusted and that user-input is sanitized before use.
 *
 * @checker_framework.manual #sqltainting-checker SQL Tainting Checker
 */
@SuppressWarningsPrefix({"sqlsanitized", "sqltainting"})
public class SqlTaintingChecker extends BaseTypeChecker {}

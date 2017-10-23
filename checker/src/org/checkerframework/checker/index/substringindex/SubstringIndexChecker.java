package org.checkerframework.checker.index.substringindex;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * The Substring Index Checker is an internal checker that assists the Index Checker in typing the
 * results of calls to the JDK's {@link java.lang.String#indexOf(String) String.indexOf} and {@link
 * java.lang.String#lastIndexOf(String) String.lastIndexOf} routines.
 *
 * @checker_framework.manual #index-substringindex Index Checker
 */
@SuppressWarningsKeys({"index", "substringindex"})
public class SubstringIndexChecker extends BaseTypeChecker {}

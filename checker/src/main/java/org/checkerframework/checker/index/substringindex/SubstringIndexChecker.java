package org.checkerframework.checker.index.substringindex;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * The Substring Index Checker is an internal checker that assists the Index Checker in typing the
 * results of calls to the JDK's {@link java.lang.String#indexOf(String) String.indexOf} and {@link
 * java.lang.String#lastIndexOf(String) String.lastIndexOf} routines.
 *
 * @checker_framework.manual #index-substringindex Index Checker
 */
@SuppressWarningsPrefix({"index", "substringindex"})
@RelevantJavaTypes({CharSequence.class, Object[].class})
public class SubstringIndexChecker extends BaseTypeChecker {}

package org.checkerframework.checker.regex;

import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker plug-in for the {@link Regex} qualifier that finds syntactically invalid regular
 * expressions.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@StubFiles("apache-xerces.astub")
@RelevantJavaTypes(CharSequence.class)
public class RegexChecker extends BaseTypeChecker {}

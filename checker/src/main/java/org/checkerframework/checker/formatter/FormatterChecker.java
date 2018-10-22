package org.checkerframework.checker.formatter;

import org.checkerframework.checker.formatter.qual.Format;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/**
 * A type-checker plug-in for the {@link Format} qualifier that finds syntactically invalid
 * formatter calls.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 */
@RelevantJavaTypes(CharSequence.class)
public class FormatterChecker extends BaseTypeChecker {}
